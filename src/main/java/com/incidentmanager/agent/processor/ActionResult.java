package com.incidentmanager.agent.processor;

public record ActionResult(String stepName, String action, boolean success, String detail) {
}
