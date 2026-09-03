package com.incidentmanager.agent.runbook;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Runbook {
    private String incidentType;
    private String description;
    private MatchRules match = new MatchRules();
    private Map<String, ContainsRule> severityRules = new LinkedHashMap<>();
    private List<RunbookStep> steps = new ArrayList<>();
    private Escalation escalation = new Escalation();

    public String getIncidentType() {
        return incidentType;
    }

    public void setIncidentType(String incidentType) {
        this.incidentType = incidentType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MatchRules getMatch() {
        return match;
    }

    public void setMatch(MatchRules match) {
        this.match = match;
    }

    public Map<String, ContainsRule> getSeverityRules() {
        return severityRules;
    }

    public void setSeverityRules(Map<String, ContainsRule> severityRules) {
        this.severityRules = severityRules;
    }

    public List<RunbookStep> getSteps() {
        return steps;
    }

    public void setSteps(List<RunbookStep> steps) {
        this.steps = steps;
    }

    public Escalation getEscalation() {
        return escalation;
    }

    public void setEscalation(Escalation escalation) {
        this.escalation = escalation;
    }
}
