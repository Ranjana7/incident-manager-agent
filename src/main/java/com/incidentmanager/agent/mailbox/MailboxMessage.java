package com.incidentmanager.agent.mailbox;

import java.time.OffsetDateTime;

public record MailboxMessage(
        String providerMessageId,
        String subject,
        String sender,
        String body,
        OffsetDateTime receivedAt
) {
}
