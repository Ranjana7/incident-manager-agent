package com.incidentmanager.agent.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface IncidentRecordRepository extends JpaRepository<IncidentRecord, Long> {
    List<IncidentRecord> findByProcessedAtBetween(OffsetDateTime startInclusive, OffsetDateTime endExclusive);
}
