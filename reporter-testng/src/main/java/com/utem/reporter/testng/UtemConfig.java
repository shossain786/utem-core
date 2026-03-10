package com.utem.reporter.testng;

import java.io.InputStream;
import java.util.Properties;

/**
 * Resolves UTEM configuration from (in priority order):
 * 1. System property  (-Dutem.*)
 * 2. Environment variable (UTEM_*)
 * 3. utem.properties file on the test classpath (src/test/resources/utem.properties)
 * 4. Built-in default
 */
public final class UtemConfig {

    private static final String DISABLED_PROP_KEY = "utem.disabled";
    private static final String DISABLED_ENV_KEY  = "UTEM_DISABLED";
    private static final String PROP_KEY          = "utem.server.url";
    private static final String ENV_KEY           = "UTEM_SERVER_URL";
    private static final String DEFAULT_URL       = "http://localhost:8080/utem";
    private static final String LABEL_PROP_KEY    = "utem.run.label";
    private static final String LABEL_ENV_KEY     = "UTEM_RUN_LABEL";
    private static final String JOB_PROP_KEY      = "utem.job.name";
    private static final String JOB_ENV_KEY       = "UTEM_JOB_NAME";
    private static final String NAME_PROP_KEY     = "utem.run.name";
    private static final String NAME_ENV_KEY      = "UTEM_RUN_NAME";

    private final Properties fileProps;
    private final String serverUrl;
    private final String runLabel;
    private final String jobName;
    private final String runName;

    public UtemConfig() {
        this.fileProps = loadFileProps();

        String url = resolve(PROP_KEY, ENV_KEY, DEFAULT_URL);
        this.serverUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;

        String label = resolve(LABEL_PROP_KEY, LABEL_ENV_KEY, null);
        this.runLabel = (label != null && !label.isBlank()) ? label.trim() : null;

        String job = resolve(JOB_PROP_KEY, JOB_ENV_KEY, null);
        this.jobName = (job != null && !job.isBlank()) ? job.trim() : null;

        String name = resolve(NAME_PROP_KEY, NAME_ENV_KEY, null);
        this.runName = (name != null && !name.isBlank()) ? name.trim() : null;
    }

    public String getEventsUrl()                       { return serverUrl + "/events"; }
    public String getBatchEventsUrl()                  { return serverUrl + "/events/batch"; }
    public String getAttachmentUploadUrl(String id)    { return serverUrl + "/attachments/" + id + "/upload"; }
    public String getRunLabel()                        { return runLabel; }
    public String getJobName()                         { return jobName; }
    public String getRunName(String defaultName)       { return runName != null ? runName : defaultName; }

    /**
     * Returns true if disabled via system property, env var, or utem.properties.
     * Set {@code utem.disabled=true} in {@code src/test/resources/utem.properties}
     * to disable without any command-line args (e.g. when running from IDE).
     */
    public boolean isDisabled() {
        String v = resolve(DISABLED_PROP_KEY, DISABLED_ENV_KEY, "false");
        return "true".equalsIgnoreCase(v);
    }

    // ── Internal ──────────────────────────────────────────────────────

    private String resolve(String propKey, String envKey, String defaultValue) {
        String v = System.getProperty(propKey);
        if (v != null && !v.isBlank()) return v.trim();
        v = System.getenv(envKey);
        if (v != null && !v.isBlank()) return v.trim();
        v = fileProps.getProperty(propKey);
        if (v != null && !v.isBlank()) return v.trim();
        return defaultValue;
    }

    private static Properties loadFileProps() {
        Properties p = new Properties();
        try (InputStream is = UtemConfig.class.getClassLoader()
                .getResourceAsStream("utem.properties")) {
            if (is != null) p.load(is);
        } catch (Exception ignored) {}
        return p;
    }
}
