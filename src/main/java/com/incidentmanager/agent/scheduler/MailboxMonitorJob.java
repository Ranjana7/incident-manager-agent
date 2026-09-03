package com.incidentmanager.agent.scheduler;

import com.incidentmanager.agent.mailbox.MailboxService;
import com.incidentmanager.agent.processor.IncidentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MailboxMonitorJob {

    private static final Logger log = LoggerFactory.getLogger(MailboxMonitorJob.class);

    private final MailboxService mailboxService;
    private final IncidentProcessor incidentProcessor;

    public MailboxMonitorJob(MailboxService mailboxService, IncidentProcessor incidentProcessor) {
        this.mailboxService = mailboxService;
        this.incidentProcessor = incidentProcessor;
    }

    @Scheduled(fixedDelayString = "${agent.mailbox.poll-interval-ms:60000}", initialDelay = 5000)
    public void pollMailbox() {
        try {
            mailboxService.fetchUnreadIncidentEmails().forEach(incidentProcessor::process);
        } catch (RuntimeException ex) {
            log.error("Mailbox polling failed.", ex);
        }
    }
}
