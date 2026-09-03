package com.incidentmanager.agent.runbook;

import java.util.ArrayList;
import java.util.List;

public class MatchRules {
    private List<String> subjectContains = new ArrayList<>();
    private List<String> bodyContains = new ArrayList<>();

    public List<String> getSubjectContains() {
        return subjectContains;
    }

    public void setSubjectContains(List<String> subjectContains) {
        this.subjectContains = subjectContains;
    }

    public List<String> getBodyContains() {
        return bodyContains;
    }

    public void setBodyContains(List<String> bodyContains) {
        this.bodyContains = bodyContains;
    }
}
