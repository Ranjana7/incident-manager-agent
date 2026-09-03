package com.incidentmanager.agent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "agent.runbook-directory=runbooks",
        "agent.mailbox.mode=sample",
        "spring.datasource.url=jdbc:sqlite:file:memdb1?mode=memory&cache=shared"
})
class IncidentManagerAgentApplicationTests {

    @Test
    void contextLoads() {
    }
}
