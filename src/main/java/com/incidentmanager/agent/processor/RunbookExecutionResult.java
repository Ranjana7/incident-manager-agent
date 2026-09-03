package com.incidentmanager.agent.processor;

import java.util.List;

public record RunbookExecutionResult(boolean resolved, boolean escalated, List<ActionResult> actions) {
}
