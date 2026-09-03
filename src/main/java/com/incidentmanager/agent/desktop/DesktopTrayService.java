package com.incidentmanager.agent.desktop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.net.URI;

@Component
public class DesktopTrayService {

    private static final Logger log = LoggerFactory.getLogger(DesktopTrayService.class);

    @EventListener(ApplicationReadyEvent.class)
    public void openDashboard() {
        if (GraphicsEnvironment.isHeadless() || !Desktop.isDesktopSupported()) {
            log.info("Desktop integration is not available. Open http://localhost:8080 manually.");
            return;
        }

        try {
            Desktop.getDesktop().browse(URI.create("http://localhost:8080"));
        } catch (Exception ex) {
            log.warn("Could not open desktop browser. Open http://localhost:8080 manually.", ex);
        }
    }
}
