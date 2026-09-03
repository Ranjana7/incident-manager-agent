package com.incidentmanager.agent.web;

public record AgentSettingsDto(
        String mailboxMode,
        long pollIntervalMs,
        String tenantId,
        String clientId,
        String clientSecret,
        String mailboxAddress,
        String teamsWebhookUrl,
        String runbookDirectory,
        boolean restartRequired
) {
}
