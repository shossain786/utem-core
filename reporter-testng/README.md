# UTEM Reporter for TestNG

Streams TestNG test results to [UTEM Core](https://github.com/shossain786/utem-core) in real time as tests run.
Zero runtime dependencies — uses only `java.net.http.HttpClient` (JDK 11+).

## Installation

Add to your `pom.xml`:

```xml
<dependency>
  <groupId>io.github.shossain786</groupId>
  <artifactId>utem-reporter-testng</artifactId>
  <version>0.1.1</version>
  <scope>test</scope>
</dependency>
```

## Usage

The listener is auto-discovered via the Java SPI mechanism when the JAR is on your classpath — no configuration needed.

To explicitly register it in `testng.xml`:

```xml
<suite name="My Suite">
  <listeners>
    <listener class-name="com.utem.reporter.testng.UtemTestNGListener"/>
  </listeners>
  <test name="My Tests">
    <classes>
      <class name="com.example.MyTest"/>
    </classes>
  </test>
</suite>
```

## Configuration

### System Properties / Environment Variables

Pass on the command line or set as environment variables:

| System Property | Environment Variable | Description | Default |
|-----------------|----------------------|-------------|---------|
| `utem.server.url` | `UTEM_SERVER_URL` | UTEM Core server URL | `http://localhost:8080/utem` |
| `utem.run.name` | `UTEM_RUN_NAME` | Custom name for the test run | Suite name |
| `utem.run.label` | `UTEM_RUN_LABEL` | Label to tag the run (e.g. `regression`, `smoke`) | _(none)_ |
| `utem.job.name` | `UTEM_JOB_NAME` | CI job name (e.g. Jenkins build name) | _(none)_ |
| `utem.disabled` | `UTEM_DISABLED` | Set to `true` to disable the reporter entirely | `false` |

**Example — Maven Surefire:**
```bash
mvn test \
  -Dutem.server.url=http://myserver:8080/utem \
  -Dutem.run.name="Regression Suite" \
  -Dutem.run.label=regression
```

### Config File (`utem.properties`)

For settings that apply whenever you run tests (including directly from your IDE), create `src/test/resources/utem.properties`:

```properties
utem.server.url=http://myserver:8080/utem
utem.run.name=My Test Suite
utem.run.label=regression
utem.job.name=nightly
utem.disabled=false
```

| Property | Description | Default |
|----------|-------------|---------|
| `utem.server.url` | UTEM Core server URL | `http://localhost:8080/utem` |
| `utem.run.name` | Custom name for the test run | Suite name |
| `utem.run.label` | Label to tag the run | _(none)_ |
| `utem.job.name` | CI job name | _(none)_ |
| `utem.disabled` | Set to `true` to disable the reporter entirely | `false` |

**Priority order:** System property (`-D`) → Environment variable → `utem.properties` file → built-in default.

## Disabling the Reporter

Sometimes you want to run tests without sending results to UTEM (e.g. during local development or when running individual test methods directly from the IDE).

**Option 1 — system property (one-off):**
```bash
mvn test -Dutem.disabled=true
```

**Option 2 — environment variable:**
```bash
UTEM_DISABLED=true mvn test
```

**Option 3 — `utem.properties` file (always disabled until you change it back):**

Create `src/test/resources/utem.properties`:
```properties
utem.disabled=true
```

When disabled, the reporter prints a single log line and sends no HTTP requests to the UTEM server. This is ideal for running tests directly from your IDE without having a UTEM server running.

## Screenshot Support (Selenium)

Screenshots can be captured on test failure using the `WebDriverRegistry`:

```java
// In your @BeforeMethod, register the WebDriver
WebDriverRegistry.set(driver);

// In your @AfterMethod, clear it
WebDriverRegistry.clear();
```

Screenshots are automatically attached to the failing test in the UTEM dashboard.

## What Gets Reported

| TestNG callback | UTEM event |
|-----------------|------------|
| `onStart(ISuite)` | `TEST_RUN_STARTED` |
| `onStart(ITestContext)` | `TEST_SUITE_STARTED` |
| `onTestStart` | `TEST_CASE_STARTED` |
| `onTestSuccess` | `TEST_PASSED` + `TEST_CASE_FINISHED` |
| `onTestFailure` | `TEST_FAILED` + `TEST_CASE_FINISHED` |
| `onTestSkipped` | `TEST_SKIPPED` + `TEST_CASE_FINISHED` |
| `onFinish(ITestContext)` | `TEST_SUITE_FINISHED` |
| `onFinish(ISuite)` | `TEST_RUN_FINISHED` |

## Requirements

- Java 17+
- TestNG 7.5+
- UTEM Core server running

**Quickstart with Docker:**
```bash
docker run -d -p 8080:8080 --name utem-core sddmhossain/utem-core
```

See [UTEM Core](https://github.com/shossain786/utem-core) for full setup options.
