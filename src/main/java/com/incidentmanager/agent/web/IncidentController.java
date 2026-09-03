package com.incidentmanager.agent.web;

import com.incidentmanager.agent.audit.IncidentRecord;
import com.incidentmanager.agent.audit.IncidentRecordRepository;
import com.incidentmanager.agent.audit.IncidentStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class IncidentController {

    private final IncidentRecordRepository incidentRecordRepository;

    public IncidentController(IncidentRecordRepository incidentRecordRepository) {
        this.incidentRecordRepository = incidentRecordRepository;
    }

    @GetMapping("/api/incidents")
    public IncidentListResponse incidents(
            @RequestParam(defaultValue = "today") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) String incidentType,
            @RequestParam(required = false) String search
    ) {
        DateWindow window = DateWindow.from(range, startDate, endDate);
        List<IncidentRecord> filtered = incidentRecordRepository.findByProcessedAtBetween(window.start(), window.end())
                .stream()
                .filter(record -> equalsIgnoreCaseOrEmpty(severity, record.getSeverity()))
                .filter(record -> status == null || status == record.getStatus())
                .filter(record -> equalsIgnoreCaseOrEmpty(incidentType, record.getIncidentType()))
                .filter(record -> matchesSearch(record, search))
                .sorted(Comparator.comparing(IncidentRecord::getProcessedAt).reversed())
                .toList();

        List<IncidentDto> incidents = filtered.stream().map(IncidentDto::from).toList();
        return new IncidentListResponse(
                incidents,
                incidents.size(),
                countBy(filtered, IncidentRecord::getSeverity),
                countBy(filtered, IncidentRecord::getIncidentType),
                countBy(filtered, record -> record.getStatus().name())
        );
    }

    private static boolean equalsIgnoreCaseOrEmpty(String requested, String actual) {
        return requested == null || requested.isBlank() || requested.equalsIgnoreCase(actual);
    }

    private static boolean matchesSearch(IncidentRecord record, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String needle = search.toLowerCase(Locale.ROOT);
        String haystack = String.join("\n",
                nullToEmpty(record.getSubject()),
                nullToEmpty(record.getSender()),
                nullToEmpty(record.getIncidentType()),
                nullToEmpty(record.getReport())
        ).toLowerCase(Locale.ROOT);
        return haystack.contains(needle);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Map<String, Long> countBy(List<IncidentRecord> records, Function<IncidentRecord, String> classifier) {
        return records.stream().collect(Collectors.groupingBy(classifier, Collectors.counting()));
    }

    private record DateWindow(OffsetDateTime start, OffsetDateTime end) {
        static DateWindow from(String range, LocalDate startDate, LocalDate endDate) {
            ZoneId zone = ZoneId.systemDefault();
            LocalDate today = LocalDate.now(zone);
            LocalDate start;
            LocalDate endExclusive;
            switch (range == null ? "today" : range.toLowerCase(Locale.ROOT)) {
                case "last-week" -> {
                    start = today.minusDays(7);
                    endExclusive = today.plusDays(1);
                }
                case "last-month" -> {
                    start = today.minusMonths(1);
                    endExclusive = today.plusDays(1);
                }
                case "custom" -> {
                    start = startDate == null ? today : startDate;
                    endExclusive = endDate == null ? start.plusDays(1) : endDate.plusDays(1);
                }
                case "all" -> {
                    start = LocalDate.of(1970, 1, 1);
                    endExclusive = today.plusDays(1);
                }
                default -> {
                    start = today;
                    endExclusive = today.plusDays(1);
                }
            }
            return new DateWindow(start.atStartOfDay(zone).toOffsetDateTime(), endExclusive.atStartOfDay(zone).toOffsetDateTime());
        }
    }
}
