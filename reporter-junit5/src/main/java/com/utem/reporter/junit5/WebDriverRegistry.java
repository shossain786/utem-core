package com.utem.reporter.junit5;

/**
 * Thread-local registry for Selenium WebDriver instances.
 * <p>
 * Test code should call {@link #register(Object)} in {@code @BeforeEach}
 * and {@link #unregister()} in {@code @AfterEach} to enable automatic
 * screenshot capture on test failure.
 * <p>
 * The parameter type is {@code Object} to avoid a hard dependency on Selenium.
 * The reporter checks the classpath at runtime before casting.
 */
public final class WebDriverRegistry {

    private static final ThreadLocal<Object> DRIVER = new ThreadLocal<>();

    /**
     * Register a WebDriver instance for the current thread.
     *
     * @param driver a Selenium WebDriver instance
     */
    public static void register(Object driver) {
        DRIVER.set(driver);
    }

    /**
     * Unregister the WebDriver for the current thread.
     * Call this in {@code @AfterEach} before quitting the driver.
     */
    public static void unregister() {
        DRIVER.remove();
    }

    /**
     * Get the registered WebDriver for the current thread, or null.
     */
    static Object get() {
        return DRIVER.get();
    }
}
