package com.incidentmanager.agent.web;

import com.incidentmanager.agent.config.AgentProperties;
import com.incidentmanager.agent.runbook.RunbookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
public class StatusController {

    private final AgentProperties properties;
    private final RunbookService runbookService;

    public StatusController(AgentProperties properties, RunbookService runbookService) {
        this.properties = properties;
        this.runbookService = runbookService;
    }

    @GetMapping("/api/status")
    public Map<String, Object> status() {
        return Map.of(
                "status", "running",
                "time", OffsetDateTime.now().toString(),
                "mailboxMode", properties.getMailbox().getMode(),
                "pollIntervalMs", properties.getMailbox().getPollIntervalMs(),
                "runbookDirectory", properties.getRunbookDirectory(),
                "runbooksLoaded", runbookService.allRunbooks().size(),
                "teamsWebhookConfigured", properties.getTeams().getWebhookUrl() != null && !properties.getTeams().getWebhookUrl().isBlank()
        );
    }
}
