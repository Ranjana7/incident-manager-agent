package com.incidentmanager.agent.mailbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.incidentmanager.agent.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "agent.mailbox", name = "mode", havingValue = "graph")
public class GraphMailboxService implements MailboxService {

    private static final Logger log = LoggerFactory.getLogger(GraphMailboxService.class);

    private final AgentProperties.Graph graph;
    private final WebClient webClient;

    public GraphMailboxService(AgentProperties properties, WebClient.Builder webClientBuilder) {
        this.graph = properties.getMailbox().getGraph();
        this.webClient = webClientBuilder.build();
    }

    @Override
    public List<MailboxMessage> fetchUnreadIncidentEmails() {
        if (isBlank(graph.getTenantId()) || isBlank(graph.getClientId()) || isBlank(graph.getClientSecret()) || isBlank(graph.getMailboxAddress())) {
            throw new IllegalStateException("Graph mailbox mode requires tenant-id, client-id, client-secret, and mailbox-address.");
        }

        String token = fetchAccessToken();
        String mailbox = UriUtils.encodePathSegment(graph.getMailboxAddress(), StandardCharsets.UTF_8);
        String url = "https://graph.microsoft.com/v1.0/users/" + mailbox
                + "/mailFolders/inbox/messages?$top=25&$filter=isRead eq false"
                + "&$select=id,subject,from,receivedDateTime,bodyPreview,body";

        JsonNode response = webClient.get()
                .uri(url)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        List<MailboxMessage> messages = new ArrayList<>();
        if (response == null || !response.has("value")) {
            return messages;
        }

        for (JsonNode node : response.get("value")) {
            String id = text(node, "id");
            String subject = text(node, "subject");
            String sender = node.path("from").path("emailAddress").path("address").asText("");
            String body = node.path("body").path("content").asText(node.path("bodyPreview").asText(""));
            OffsetDateTime receivedAt = OffsetDateTime.parse(text(node, "receivedDateTime"));
            messages.add(new MailboxMessage(id, subject, sender, body, receivedAt));
        }

        log.info("Fetched {} unread mailbox messages from Graph.", messages.size());
        return messages;
    }

    private String fetchAccessToken() {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", graph.getClientId());
        form.add("client_secret", graph.getClientSecret());
        form.add("scope", "https://graph.microsoft.com/.default");
        form.add("grant_type", "client_credentials");

        JsonNode token = webClient.post()
                .uri("https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token", graph.getTenantId())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (token == null || !token.hasNonNull("access_token")) {
            throw new IllegalStateException("Microsoft Graph token response did not contain an access_token.");
        }
        return token.get("access_token").asText();
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).asText("");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
