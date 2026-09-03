package com.incidentmanager.agent.web;

import com.incidentmanager.agent.audit.IncidentRecord;
import com.incidentmanager.agent.audit.IncidentStatus;

import java.time.OffsetDateTime;

public record IncidentDto(
        Long id,
        String providerMessageId,
        String incidentType,
        String severity,
        IncidentStatus status,
        String sender,
        String subject,
        OffsetDateTime receivedAt,
        OffsetDateTime processedAt,
        String report
) {
    public static IncidentDto from(IncidentRecord record) {
        return new IncidentDto(
                record.getId(),
                record.getProviderMessageId(),
                record.getIncidentType(),
                record.getSeverity(),
                record.getStatus(),
                record.getSender(),
                record.getSubject(),
                record.getReceivedAt(),
                record.getProcessedAt(),
                record.getReport()
        );
    }
}
