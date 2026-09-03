package com.incidentmanager.agent;

import com.incidentmanager.agent.config.AgentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AgentProperties.class)
public class IncidentManagerAgentApplication {

    public static void main(String[] args) {
        createUserRuntimeDirectories();
        SpringApplication.run(IncidentManagerAgentApplication.class, args);
    }

    private static void createUserRuntimeDirectories() {
        Path root = Path.of(System.getProperty("user.home"), "IncidentManagerAgent");
        try {
            Files.createDirectories(root);
            Files.createDirectories(root.resolve("config"));
            Files.createDirectories(root.resolve("runbooks"));
            Files.createDirectories(root.resolve("logs"));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create Incident Manager runtime directories under " + root, ex);
        }
    }
}
