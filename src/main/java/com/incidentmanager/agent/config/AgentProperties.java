package com.incidentmanager.agent.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private String runbookDirectory = "runbooks";
    @Valid
    private Mailbox mailbox = new Mailbox();
    @Valid
    private Teams teams = new Teams();
    @Valid
    private Reporting reporting = new Reporting();

    public String getRunbookDirectory() {
        return runbookDirectory;
    }

    public void setRunbookDirectory(String runbookDirectory) {
        this.runbookDirectory = runbookDirectory;
    }

    public Mailbox getMailbox() {
        return mailbox;
    }

    public void setMailbox(Mailbox mailbox) {
        this.mailbox = mailbox;
    }

    public Teams getTeams() {
        return teams;
    }

    public void setTeams(Teams teams) {
        this.teams = teams;
    }

    public Reporting getReporting() {
        return reporting;
    }

    public void setReporting(Reporting reporting) {
        this.reporting = reporting;
    }

    public static class Mailbox {
        private String mode = "sample";
        @Min(5000)
        private long pollIntervalMs = 60000;
        @Valid
        private Graph graph = new Graph();

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }

        public Graph getGraph() {
            return graph;
        }

        public void setGraph(Graph graph) {
            this.graph = graph;
        }
    }

    public static class Graph {
        private String tenantId = "";
        private String clientId = "";
        private String clientSecret = "";
        private String mailboxAddress = "";

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getMailboxAddress() {
            return mailboxAddress;
        }

        public void setMailboxAddress(String mailboxAddress) {
            this.mailboxAddress = mailboxAddress;
        }
    }

    public static class Teams {
        private String webhookUrl = "";

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }
    }

    public static class Reporting {
        private int dailySummaryHour = 8;

        public int getDailySummaryHour() {
            return dailySummaryHour;
        }

        public void setDailySummaryHour(int dailySummaryHour) {
            this.dailySummaryHour = dailySummaryHour;
        }
    }
}
