package com.incidentmanager.agent.teams;

import com.incidentmanager.agent.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class TeamsAlertClient {

    private static final Logger log = LoggerFactory.getLogger(TeamsAlertClient.class);

    private final AgentProperties properties;
    private final WebClient webClient;

    public TeamsAlertClient(AgentProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder.build();
    }

    public boolean sendAlert(String title, String message) {
        String webhookUrl = properties.getTeams().getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Teams webhook is not configured. Alert not sent. Title: {}", title);
            return false;
        }

        webClient.post()
                .uri(webhookUrl)
                .bodyValue(Map.of("text", "**" + title + "**\n\n" + message))
                .retrieve()
                .toBodilessEntity()
                .block();
        return true;
    }
}
