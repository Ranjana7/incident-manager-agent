package com.incidentmanager.agent.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "processed_messages")
public class ProcessedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String providerMessageId;

    @Column(nullable = false)
    private OffsetDateTime processedAt;

    protected ProcessedMessage() {
    }

    public ProcessedMessage(String providerMessageId, OffsetDateTime processedAt) {
        this.providerMessageId = providerMessageId;
        this.processedAt = processedAt;
    }

    public Long getId() {
        return id;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
