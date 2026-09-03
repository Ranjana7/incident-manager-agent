package com.incidentmanager.agent.mailbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(prefix = "agent.mailbox", name = "mode", havingValue = "sample", matchIfMissing = true)
public class SampleMailboxService implements MailboxService {

    @Override
    public List<MailboxMessage> fetchUnreadIncidentEmails() {
        return List.of(
                new MailboxMessage(
                        "sample-phishing-001",
                        "Provider alert: credential theft phishing campaign",
                        "alerts@example-security-provider.test",
                        "A malicious link was detected for customer Contoso. No active compromise confirmed.",
                        OffsetDateTime.now().minusMinutes(15)
                ),
                new MailboxMessage(
                        "sample-malware-001",
                        "Endpoint alert: malware detected and quarantined",
                        "soc@example-edr-provider.test",
                        "Malware detected on one host. Quarantine succeeded. Human validation is recommended.",
                        OffsetDateTime.now().minusMinutes(7)
                )
        );
    }
}
