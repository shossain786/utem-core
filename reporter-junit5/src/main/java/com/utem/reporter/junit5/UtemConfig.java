package com.utem.reporter.junit5;

/**
 * Resolves UTEM server URL from system property, environment variable, or default.
 */
public final class UtemConfig {

    private static final String PROP_KEY = "utem.server.url";
    private static final String ENV_KEY = "UTEM_SERVER_URL";
    private static final String DEFAULT_URL = "http://localhost:8080/utem";

    private final String serverUrl;

    public UtemConfig() {
        String url = System.getProperty(PROP_KEY);
        if (url == null || url.isBlank()) {
            url = System.getenv(ENV_KEY);
        }
        if (url == null || url.isBlank()) {
            url = DEFAULT_URL;
        }
        this.serverUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String getEventsUrl() {
        return serverUrl + "/events";
    }

    public String getBatchEventsUrl() {
        return serverUrl + "/events/batch";
    }

    public String getAttachmentUploadUrl(String attachmentId) {
        return serverUrl + "/attachments/" + attachmentId + "/upload";
    }
}
