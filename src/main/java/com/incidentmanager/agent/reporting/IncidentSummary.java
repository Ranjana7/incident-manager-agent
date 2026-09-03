package com.incidentmanager.agent.reporting;

import java.time.LocalDate;
import java.util.Map;

public record IncidentSummary(
        LocalDate date,
        long total,
        Map<String, Long> bySeverity,
        Map<String, Long> byIncidentType,
        Map<String, Long> byStatus
) {
}
