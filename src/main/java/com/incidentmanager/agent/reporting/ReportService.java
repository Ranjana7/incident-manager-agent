package com.incidentmanager.agent.reporting;

import com.incidentmanager.agent.audit.IncidentRecord;
import com.incidentmanager.agent.audit.IncidentRecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final IncidentRecordRepository incidentRecordRepository;

    public ReportService(IncidentRecordRepository incidentRecordRepository) {
        this.incidentRecordRepository = incidentRecordRepository;
    }

    public IncidentSummary summarize(LocalDate date) {
        ZoneId zone = ZoneId.systemDefault();
        OffsetDateTime start = date.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = date.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        List<IncidentRecord> records = incidentRecordRepository.findByProcessedAtBetween(start, end);
        return new IncidentSummary(
                date,
                records.size(),
                countBy(records, IncidentRecord::getSeverity),
                countBy(records, IncidentRecord::getIncidentType),
                countBy(records, record -> record.getStatus().name())
        );
    }

    private static Map<String, Long> countBy(List<IncidentRecord> records, Function<IncidentRecord, String> classifier) {
        return records.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.counting()));
    }
}
