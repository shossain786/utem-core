package com.utem.reporter.junit5;

import java.time.Instant;

/**
 * Builds EventRequest JSON strings matching the UTEM Core API contract.
 * Uses no external JSON library — constructs JSON via string operations.
 */
public final class EventBuilder {

    public String buildRunStarted(String eventId, String runId, String name, String label, String jobName) {
        StringBuilder payload = new StringBuilder("{\"name\":\"").append(escape(name)).append("\"");
        if (label != null && !label.isBlank()) {
            payload.append(",\"label\":\"").append(escape(label)).append("\"");
        }
        if (jobName != null && !jobName.isBlank()) {
            payload.append(",\"jobName\":\"").append(escape(jobName)).append("\"");
        }
        payload.append("}");
        return buildEvent(eventId, runId, "TEST_RUN_STARTED", null, payload.toString());
    }

    public String buildRunFinished(String eventId, String runId, String parentId,
                                   int total, int passed, int failed, int skipped) {
        String status = failed > 0 ? "FAILED" : "PASSED";
        String payload = "{\"totalTests\":" + total
                + ",\"passedTests\":" + passed
                + ",\"failedTests\":" + failed
                + ",\"skippedTests\":" + skipped
                + ",\"runStatus\":\"" + status + "\"}";
        return buildEvent(eventId, runId, "TEST_RUN_FINISHED", parentId, payload);
    }

    public String buildSuiteStarted(String eventId, String runId, String parentId, String name) {
        String payload = "{\"name\":\"" + escape(name) + "\"}";
        return buildEvent(eventId, runId, "TEST_SUITE_STARTED", parentId, payload);
    }

    public String buildSuiteFinished(String eventId, String runId, String parentId,
                                     String nodeStatus, Long duration) {
        StringBuilder payload = new StringBuilder("{\"nodeStatus\":\"").append(nodeStatus).append("\"");
        if (duration != null) {
            payload.append(",\"duration\":").append(duration);
        }
        payload.append("}");
        return buildEvent(eventId, runId, "TEST_SUITE_FINISHED", parentId, payload.toString());
    }

    public String buildCaseStarted(String eventId, String runId, String parentId, String name) {
        String payload = "{\"name\":\"" + escape(name) + "\"}";
        return buildEvent(eventId, runId, "TEST_CASE_STARTED", parentId, payload);
    }

    public String buildCaseFinished(String eventId, String runId, String parentId,
                                    String nodeStatus, Long duration) {
        StringBuilder payload = new StringBuilder("{\"nodeStatus\":\"").append(nodeStatus).append("\"");
        if (duration != null) {
            payload.append(",\"duration\":").append(duration);
        }
        payload.append("}");
        return buildEvent(eventId, runId, "TEST_CASE_FINISHED", parentId, payload.toString());
    }

    public String buildTestPassed(String eventId, String runId, String parentId, Long duration) {
        String payload = duration != null ? "{\"duration\":" + duration + "}" : "{}";
        return buildEvent(eventId, runId, "TEST_PASSED", parentId, payload);
    }

    public String buildTestFailed(String eventId, String runId, String parentId,
                                  Long duration, String errorMessage, String stackTrace) {
        StringBuilder payload = new StringBuilder("{");
        boolean hasField = false;
        if (duration != null) {
            payload.append("\"duration\":").append(duration);
            hasField = true;
        }
        if (errorMessage != null) {
            if (hasField) payload.append(",");
            payload.append("\"errorMessage\":\"").append(escape(errorMessage)).append("\"");
            hasField = true;
        }
        if (stackTrace != null) {
            if (hasField) payload.append(",");
            payload.append("\"stackTrace\":\"").append(escape(stackTrace)).append("\"");
        }
        payload.append("}");
        return buildEvent(eventId, runId, "TEST_FAILED", parentId, payload.toString());
    }

    public String buildTestSkipped(String eventId, String runId, String parentId, String reason) {
        String payload = reason != null
                ? "{\"errorMessage\":\"" + escape(reason) + "\"}"
                : "{}";
        return buildEvent(eventId, runId, "TEST_SKIPPED", parentId, payload);
    }

    public String buildTestStep(String eventId, String runId, String parentId,
                               String name, String stepStatus, int stepOrder,
                               Long duration, String errorMessage, String stackTrace) {
        StringBuilder payload = new StringBuilder("{");
        payload.append("\"name\":\"").append(escape(name)).append("\"");
        payload.append(",\"stepStatus\":\"").append(stepStatus).append("\"");
        payload.append(",\"stepOrder\":").append(stepOrder);
        if (duration != null) payload.append(",\"duration\":").append(duration);
        if (errorMessage != null) {
            payload.append(",\"errorMessage\":\"").append(escape(errorMessage)).append("\"");
        }
        if (stackTrace != null) {
            payload.append(",\"stackTrace\":\"").append(escape(stackTrace)).append("\"");
        }
        payload.append("}");
        return buildEvent(eventId, runId, "TEST_STEP", parentId, payload.toString());
    }

    public String buildAttachment(String eventId, String runId, String parentId,
                                  String name, String mimeType, long fileSize,
                                  boolean isFailureScreenshot) {
        String payload = "{\"name\":\"" + escape(name) + "\""
                + ",\"attachmentType\":\"SCREENSHOT\""
                + ",\"mimeType\":\"" + escape(mimeType) + "\""
                + ",\"fileSize\":" + fileSize
                + ",\"isFailureScreenshot\":" + isFailureScreenshot + "}";
        return buildEvent(eventId, runId, "ATTACHMENT", parentId, payload);
    }

    // ── Internal ────────────────────────────────────────────────────

    private String buildEvent(String eventId, String runId, String eventType,
                              String parentId, String payload) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"eventId\":\"").append(eventId).append("\"");
        sb.append(",\"runId\":\"").append(runId).append("\"");
        sb.append(",\"eventType\":\"").append(eventType).append("\"");
        if (parentId != null) {
            sb.append(",\"parentId\":\"").append(parentId).append("\"");
        } else {
            sb.append(",\"parentId\":null");
        }
        sb.append(",\"timestamp\":\"").append(Instant.now().toString()).append("\"");
        sb.append(",\"payload\":\"").append(escape(payload)).append("\"");
        sb.append("}");
        return sb.toString();
    }

    static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
