package com.incidentmanager.agent.processor;

import com.incidentmanager.agent.mailbox.MailboxMessage;
import com.incidentmanager.agent.runbook.Runbook;
import com.incidentmanager.agent.runbook.RunbookStep;
import com.incidentmanager.agent.teams.TeamsAlertClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RunbookExecutor {

    private final TeamsAlertClient teamsAlertClient;

    public RunbookExecutor(TeamsAlertClient teamsAlertClient) {
        this.teamsAlertClient = teamsAlertClient;
    }

    public RunbookExecutionResult execute(Runbook runbook, MailboxMessage message, String severity) {
        List<ActionResult> results = new ArrayList<>();
        boolean resolved = false;
        boolean escalated = false;

        for (RunbookStep step : runbook.getSteps()) {
            if (!shouldRun(step, resolved)) {
                continue;
            }

            String action = normalize(step.getAction());
            switch (action) {
                case "parse_email" -> results.add(new ActionResult(step.getName(), action, true, parseDetail(message)));
                case "add_note" -> results.add(new ActionResult(step.getName(), action, true, step.getParameters().getOrDefault("note", "")));
                case "mark_resolved" -> {
                    resolved = true;
                    results.add(new ActionResult(step.getName(), action, true, "Runbook marked the incident as resolved."));
                }
                case "teams_alert" -> {
                    boolean sent = teamsAlertClient.sendAlert(
                            runbook.getEscalation().getTitle(),
                            buildAlertMessage(runbook, message, severity)
                    );
                    escalated = true;
                    results.add(new ActionResult(step.getName(), action, sent, sent ? "Teams alert sent." : "Teams webhook not configured."));
                }
                default -> results.add(new ActionResult(step.getName(), action, false, "Unsupported action. Human review required."));
            }
        }

        return new RunbookExecutionResult(resolved, escalated, List.copyOf(results));
    }

    private static boolean shouldRun(RunbookStep step, boolean resolved) {
        String condition = normalize(step.getCondition());
        return switch (condition) {
            case "always" -> true;
            case "unresolved" -> !resolved;
            case "resolved" -> resolved;
            default -> false;
        };
    }

    private static String parseDetail(MailboxMessage message) {
        return "sender=" + message.sender()
                + ", subject=" + message.subject()
                + ", receivedAt=" + message.receivedAt();
    }

    private static String buildAlertMessage(Runbook runbook, MailboxMessage message, String severity) {
        return runbook.getEscalation().getMessage()
                + "\n\nSeverity: " + severity
                + "\nIncident type: " + runbook.getIncidentType()
                + "\nSender: " + message.sender()
                + "\nSubject: " + message.subject()
                + "\nReceived: " + message.receivedAt();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
