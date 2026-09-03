package com.incidentmanager.agent.runbook;

import java.util.LinkedHashMap;
import java.util.Map;

public class RunbookStep {
    private String name;
    private String action;
    private String condition = "always";
    private Map<String, String> parameters = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
