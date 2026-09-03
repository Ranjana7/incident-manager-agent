package com.incidentmanager.agent.web;

import java.util.List;
import java.util.Map;

public record IncidentListResponse(
        List<IncidentDto> incidents,
        long total,
        Map<String, Long> bySeverity,
        Map<String, Long> byIncidentType,
        Map<String, Long> byStatus
) {
}
