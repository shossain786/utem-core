package com.utem.reporter.testng;

/**
 * Resolves UTEM server URL from system property, environment variable, or default.
 */
public final class UtemConfig {

    private static final String PROP_KEY       = "utem.server.url";
    private static final String ENV_KEY        = "UTEM_SERVER_URL";
    private static final String DEFAULT_URL    = "http://localhost:8080/utem";
    private static final String LABEL_PROP_KEY = "utem.run.label";
    private static final String LABEL_ENV_KEY  = "UTEM_RUN_LABEL";
    private static final String JOB_PROP_KEY   = "utem.job.name";
    private static final String JOB_ENV_KEY    = "UTEM_JOB_NAME";
    private static final String NAME_PROP_KEY  = "utem.run.name";
    private static final String NAME_ENV_KEY   = "UTEM_RUN_NAME";

    private final String serverUrl;
    private final String runLabel;
    private final String jobName;
    private final String runName;

    public UtemConfig() {
        String url = System.getProperty(PROP_KEY);
        if (url == null || url.isBlank()) url = System.getenv(ENV_KEY);
        if (url == null || url.isBlank()) url = DEFAULT_URL;
        this.serverUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

        String label = System.getProperty(LABEL_PROP_KEY);
        if (label == null || label.isBlank()) label = System.getenv(LABEL_ENV_KEY);
        this.runLabel = (label != null && !label.isBlank()) ? label.trim() : null;

        String job = System.getProperty(JOB_PROP_KEY);
        if (job == null || job.isBlank()) job = System.getenv(JOB_ENV_KEY);
        this.jobName = (job != null && !job.isBlank()) ? job.trim() : null;

        String name = System.getProperty(NAME_PROP_KEY);
        if (name == null || name.isBlank()) name = System.getenv(NAME_ENV_KEY);
        this.runName = (name != null && !name.isBlank()) ? name.trim() : null;
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

    /** Returns the run label, or null if not configured. */
    public String getRunLabel() { return runLabel; }

    /** Returns the job name, or null if not configured. */
    public String getJobName() { return jobName; }

    /**
     * Returns the custom run name if set via {@code -Dutem.run.name} or {@code UTEM_RUN_NAME},
     * otherwise returns the provided default name.
     */
    public String getRunName(String defaultName) {
        return runName != null ? runName : defaultName;
    }
}
