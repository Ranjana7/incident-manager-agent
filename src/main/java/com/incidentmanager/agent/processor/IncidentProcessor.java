package com.incidentmanager.agent.processor;

import com.incidentmanager.agent.audit.IncidentRecord;
import com.incidentmanager.agent.audit.IncidentRecordRepository;
import com.incidentmanager.agent.audit.IncidentStatus;
import com.incidentmanager.agent.audit.ProcessedMessage;
import com.incidentmanager.agent.audit.ProcessedMessageRepository;
import com.incidentmanager.agent.mailbox.MailboxMessage;
import com.incidentmanager.agent.runbook.Runbook;
import com.incidentmanager.agent.runbook.RunbookService;
import com.incidentmanager.agent.teams.TeamsAlertClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class IncidentProcessor {

    private static final Logger log = LoggerFactory.getLogger(IncidentProcessor.class);

    private final RunbookService runbookService;
    private final RunbookExecutor runbookExecutor;
    private final TeamsAlertClient teamsAlertClient;
    private final ProcessedMessageRepository processedMessageRepository;
    private final IncidentRecordRepository incidentRecordRepository;

    public IncidentProcessor(RunbookService runbookService,
                             RunbookExecutor runbookExecutor,
                             TeamsAlertClient teamsAlertClient,
                             ProcessedMessageRepository processedMessageRepository,
                             IncidentRecordRepository incidentRecordRepository) {
        this.runbookService = runbookService;
        this.runbookExecutor = runbookExecutor;
        this.teamsAlertClient = teamsAlertClient;
        this.processedMessageRepository = processedMessageRepository;
        this.incidentRecordRepository = incidentRecordRepository;
    }

    @Transactional
    public void process(MailboxMessage message) {
        if (processedMessageRepository.existsByProviderMessageId(message.providerMessageId())) {
            return;
        }

        Optional<Runbook> match = runbookService.findMatchingRunbook(message);
        if (match.isEmpty()) {
            recordUnmatchedIncident(message);
            return;
        }

        Runbook runbook = match.get();
        String severity = runbookService.determineSeverity(runbook, message);
        RunbookExecutionResult result = runbookExecutor.execute(runbook, message, severity);
        IncidentStatus status = result.resolved()
                ? IncidentStatus.RESOLVED
                : result.escalated() ? IncidentStatus.ESCALATED : IncidentStatus.UNRESOLVED;

        String report = buildReport(runbook, severity, status, result);
        OffsetDateTime now = OffsetDateTime.now();
        incidentRecordRepository.save(new IncidentRecord(
                message.providerMessageId(),
                runbook.getIncidentType(),
                severity,
                status,
                message.sender(),
                message.subject(),
                message.receivedAt(),
                now,
                report
        ));
        processedMessageRepository.save(new ProcessedMessage(message.providerMessageId(), now));
        log.info("Processed incident {} as {} / {} / {}.", message.providerMessageId(), runbook.getIncidentType(), severity, status);
    }

    private void recordUnmatchedIncident(MailboxMessage message) {
        boolean alertSent = teamsAlertClient.sendAlert(
                "Unmatched security email requires triage",
                "No local runbook matched this mailbox item."
                        + "\n\nSender: " + message.sender()
                        + "\nSubject: " + message.subject()
                        + "\nReceived: " + message.receivedAt()
        );
        OffsetDateTime now = OffsetDateTime.now();
        incidentRecordRepository.save(new IncidentRecord(
                message.providerMessageId(),
                "unmatched",
                "medium",
                alertSent ? IncidentStatus.ESCALATED : IncidentStatus.UNRESOLVED,
                message.sender(),
                message.subject(),
                message.receivedAt(),
                now,
                "No runbook matched. Human review required. Teams alert sent=" + alertSent
        ));
        processedMessageRepository.save(new ProcessedMessage(message.providerMessageId(), now));
    }

    private static String buildReport(Runbook runbook, String severity, IncidentStatus status, RunbookExecutionResult result) {
        StringBuilder report = new StringBuilder();
        report.append("Incident type: ").append(runbook.getIncidentType()).append('\n');
        report.append("Severity: ").append(severity).append('\n');
        report.append("Status: ").append(status).append('\n');
        for (ActionResult action : result.actions()) {
            report.append("- ")
                    .append(action.stepName())
                    .append(" [")
                    .append(action.action())
                    .append("] success=")
                    .append(action.success())
                    .append(": ")
                    .append(action.detail())
                    .append('\n');
        }
        return report.toString();
    }
}
