package com.incidentmanager.agent.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.incidentmanager.agent.config.AgentProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class SettingsController {

    private final AgentProperties properties;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public SettingsController(AgentProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/api/settings")
    public AgentSettingsDto getSettings() {
        return fromProperties(false);
    }

    @PostMapping("/api/settings")
    public AgentSettingsDto saveSettings(@RequestBody AgentSettingsDto settings) {
        writeLocalSettings(settings);
        return new AgentSettingsDto(
                settings.mailboxMode(),
                settings.pollIntervalMs(),
                settings.tenantId(),
                settings.clientId(),
                maskSecret(settings.clientSecret()),
                settings.mailboxAddress(),
                settings.teamsWebhookUrl(),
                settings.runbookDirectory(),
                true
        );
    }

    private AgentSettingsDto fromProperties(boolean restartRequired) {
        AgentProperties.Graph graph = properties.getMailbox().getGraph();
        return new AgentSettingsDto(
                properties.getMailbox().getMode(),
                properties.getMailbox().getPollIntervalMs(),
                graph.getTenantId(),
                graph.getClientId(),
                maskSecret(graph.getClientSecret()),
                graph.getMailboxAddress(),
                properties.getTeams().getWebhookUrl(),
                properties.getRunbookDirectory(),
                restartRequired
        );
    }

    private void writeLocalSettings(AgentSettingsDto settings) {
        Path configPath = Path.of(System.getProperty("user.home"), "IncidentManagerAgent", "config", "application-local.yml");
        try {
            Files.createDirectories(configPath.getParent());
            yamlMapper.writeValue(configPath.toFile(), buildConfig(settings, properties.getMailbox().getGraph().getClientSecret()));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write local settings to " + configPath.toAbsolutePath(), ex);
        }
    }

    private static Map<String, Object> buildConfig(AgentSettingsDto settings, String existingClientSecret) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("server", Map.of("port", 8080));
        root.put("spring", Map.of(
                "datasource", Map.of(
                        "url", "jdbc:sqlite:${user.home}/IncidentManagerAgent/incident-agent.db",
                        "driver-class-name", "org.sqlite.JDBC"
                ),
                "jpa", Map.of(
                        "database-platform", "org.hibernate.community.dialect.SQLiteDialect",
                        "hibernate", Map.of("ddl-auto", "update"),
                        "open-in-view", false
                ),
                "main", Map.of("banner-mode", "console")
        ));
        root.put("agent", Map.of(
                "runbook-directory", blankDefault(settings.runbookDirectory(), "runbooks"),
                "mailbox", Map.of(
                        "mode", blankDefault(settings.mailboxMode(), "sample"),
                        "poll-interval-ms", settings.pollIntervalMs() < 5000 ? 60000 : settings.pollIntervalMs(),
                        "graph", Map.of(
                                "tenant-id", blankDefault(settings.tenantId(), ""),
                                "client-id", blankDefault(settings.clientId(), ""),
                                "client-secret", maskedOrValue(settings.clientSecret(), existingClientSecret),
                                "mailbox-address", blankDefault(settings.mailboxAddress(), "")
                        )
                ),
                "teams", Map.of("webhook-url", blankDefault(settings.teamsWebhookUrl(), "")),
                "reporting", Map.of("daily-summary-hour", 8)
        ));
        return root;
    }

    private static String blankDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String maskedOrValue(String value, String existingValue) {
        if (value == null) {
            return "";
        }
        if (value.equals("********")) {
            return blankDefault(existingValue, "");
        }
        return value;
    }

    private static String maskSecret(String value) {
        return value == null || value.isBlank() ? "" : "********";
    }
}
