package com.incidentmanager.agent.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "incident_records")
public class IncidentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String providerMessageId;

    @Column(nullable = false)
    private String incidentType;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IncidentStatus status;

    private String sender;
    private String subject;
    private OffsetDateTime receivedAt;
    private OffsetDateTime processedAt;

    @Lob
    private String report;

    protected IncidentRecord() {
    }

    public IncidentRecord(String providerMessageId, String incidentType, String severity, IncidentStatus status,
                          String sender, String subject, OffsetDateTime receivedAt, OffsetDateTime processedAt,
                          String report) {
        this.providerMessageId = providerMessageId;
        this.incidentType = incidentType;
        this.severity = severity;
        this.status = status;
        this.sender = sender;
        this.subject = subject;
        this.receivedAt = receivedAt;
        this.processedAt = processedAt;
        this.report = report;
    }

    public Long getId() {
        return id;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public String getIncidentType() {
        return incidentType;
    }

    public String getSeverity() {
        return severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public String getSender() {
        return sender;
    }

    public String getSubject() {
        return subject;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public String getReport() {
        return report;
    }
}
