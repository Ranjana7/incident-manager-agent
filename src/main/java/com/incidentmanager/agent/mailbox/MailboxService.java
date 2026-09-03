package com.incidentmanager.agent.mailbox;

import java.util.List;

public interface MailboxService {
    List<MailboxMessage> fetchUnreadIncidentEmails();
}
