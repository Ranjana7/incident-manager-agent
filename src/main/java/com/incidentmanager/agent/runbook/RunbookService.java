package com.incidentmanager.agent.runbook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.incidentmanager.agent.config.AgentProperties;
import com.incidentmanager.agent.mailbox.MailboxMessage;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class RunbookService {

    private static final Logger log = LoggerFactory.getLogger(RunbookService.class);

    private final AgentProperties properties;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private List<Runbook> runbooks = List.of();

    public RunbookService(AgentProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void loadRunbooks() {
        Path directory = resolveRunbookDirectory();
        List<Runbook> loaded = new ArrayList<>();
        if (containsRunbookYaml(directory)) {
            loadFileSystemRunbooks(directory, loaded);
        } else {
            loadClasspathRunbooks(loaded);
        }

        if (loaded.isEmpty()) {
            throw new IllegalStateException("No runbook YAML files were found in configured folders or embedded defaults.");
        }

        this.runbooks = List.copyOf(loaded);
        log.info("Loaded {} runbooks.", runbooks.size());
    }

    private void loadFileSystemRunbooks(Path directory, List<Runbook> loaded) {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(path -> path.getFileName().toString().endsWith(".yml") || path.getFileName().toString().endsWith(".yaml"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(path -> loaded.add(readRunbook(path)));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read runbooks from " + directory.toAbsolutePath(), ex);
        }
        log.info("Loaded runbooks from filesystem directory {}.", directory.toAbsolutePath());
    }

    private void loadClasspathRunbooks(List<Runbook> loaded) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources("classpath*:/runbooks/**/*.yml");
            for (Resource resource : resources) {
                loaded.add(readRunbook(resource));
            }
            log.info("Loaded embedded default runbooks from classpath.");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read embedded default runbooks.", ex);
        }
    }

    private Runbook readRunbook(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            Runbook runbook = yamlMapper.readValue(inputStream, Runbook.class);
            if (runbook.getIncidentType() == null || runbook.getIncidentType().isBlank()) {
                throw new IllegalStateException("Runbook is missing incidentType: " + resource.getDescription());
            }
            return runbook;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse runbook " + resource.getDescription(), ex);
        }
    }

    private Path resolveRunbookDirectory() {
        List<Path> candidates = List.of(
                Path.of(properties.getRunbookDirectory()),
                applicationDirectory().resolve(properties.getRunbookDirectory()),
                applicationDirectory().resolve("runbooks"),
                Path.of("app", properties.getRunbookDirectory()),
                Path.of("app", "runbooks"),
                Path.of(System.getProperty("user.home"), "IncidentManagerAgent", "runbooks")
        );
        return candidates.stream()
                .filter(RunbookService::containsRunbookYaml)
                .findFirst()
                .orElse(candidates.getFirst());
    }

    private static Path applicationDirectory() {
        try {
            Path location = Path.of(RunbookService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return Files.isRegularFile(location) ? location.getParent() : location;
        } catch (URISyntaxException | RuntimeException ex) {
            return Path.of(".");
        }
    }

    private static boolean containsRunbookYaml(Path directory) {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.anyMatch(path -> {
                String fileName = path.getFileName().toString();
                return Files.isRegularFile(path) && (fileName.endsWith(".yml") || fileName.endsWith(".yaml"));
            });
        } catch (IOException ex) {
            return false;
        }
    }

    public List<Runbook> allRunbooks() {
        return runbooks;
    }

    public Optional<Runbook> findMatchingRunbook(MailboxMessage message) {
        return runbooks.stream()
                .filter(runbook -> matches(runbook, message))
                .findFirst();
    }

    public String determineSeverity(Runbook runbook, MailboxMessage message) {
        String haystack = normalized(message.subject() + "\n" + message.body());
        for (String severity : List.of("critical", "high", "medium", "low")) {
            ContainsRule rule = runbook.getSeverityRules().get(severity);
            if (rule != null && containsAny(haystack, rule.getContainsAny())) {
                return severity;
            }
        }
        return "low";
    }

    private Runbook readRunbook(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            Runbook runbook = yamlMapper.readValue(inputStream, Runbook.class);
            if (runbook.getIncidentType() == null || runbook.getIncidentType().isBlank()) {
                throw new IllegalStateException("Runbook is missing incidentType: " + path.toAbsolutePath());
            }
            return runbook;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse runbook " + path.toAbsolutePath(), ex);
        }
    }

    private static boolean matches(Runbook runbook, MailboxMessage message) {
        String subject = normalized(message.subject());
        String body = normalized(message.body());
        MatchRules rules = runbook.getMatch();
        return containsAny(subject, rules.getSubjectContains()) || containsAny(body, rules.getBodyContains());
    }

    private static boolean containsAny(String haystack, List<String> needles) {
        if (needles == null) {
            return false;
        }
        return needles.stream()
                .filter(needle -> needle != null && !needle.isBlank())
                .map(RunbookService::normalized)
                .anyMatch(haystack::contains);
    }

    private static String normalized(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
