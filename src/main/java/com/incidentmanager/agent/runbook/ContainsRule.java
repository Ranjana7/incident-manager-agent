package com.incidentmanager.agent.runbook;

import java.util.ArrayList;
import java.util.List;

public class ContainsRule {
    private List<String> containsAny = new ArrayList<>();

    public List<String> getContainsAny() {
        return containsAny;
    }

    public void setContainsAny(List<String> containsAny) {
        this.containsAny = containsAny;
    }
}
