package com.incidentmanager.agent.runbook;

public class Escalation {
    private String title = "Incident requires review";
    private String message = "The desktop agent could not resolve this incident automatically.";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
