# TestNG Reporter

The UTEM TestNG reporter streams test results to the UTEM server in real time. Zero runtime dependencies — uses only `java.net.http.HttpClient` (JDK 11+).

## Installation

Add to `pom.xml`:

```xml
<dependency>
  <groupId>io.github.shossain786</groupId>
  <artifactId>utem-reporter-testng</artifactId>
  <version>0.1.1</version>
  <scope>test</scope>
</dependency>
```

The listener is **auto-discovered** via the Java SPI mechanism — no code changes needed.

## Explicit Registration

To register explicitly in `testng.xml`:

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

### utem.properties

Create `src/test/resources/utem.properties`:

```properties
utem.server.url=http://localhost:8080/utem
utem.run.name=My Test Suite
utem.run.label=regression
utem.job.name=nightly
```

### System Properties (Maven)

```bash
mvn test \
  -Dutem.server.url=http://myserver:8080/utem \
  -Dutem.run.name="Regression Suite" \
  -Dutem.run.label=regression
```

### Environment Variables

```bash
UTEM_SERVER_URL=http://myserver:8080/utem \
UTEM_RUN_NAME="Regression Suite" \
mvn test
```

### All options

| Property | Env Variable | Description | Default |
|---|---|---|---|
| `utem.server.url` | `UTEM_SERVER_URL` | UTEM server URL | `http://localhost:8080/utem` |
| `utem.run.name` | `UTEM_RUN_NAME` | Run name | Suite name |
| `utem.run.label` | `UTEM_RUN_LABEL` | Tag (e.g. `regression`) | — |
| `utem.job.name` | `UTEM_JOB_NAME` | CI job name | — |
| `utem.disabled` | `UTEM_DISABLED` | Set `true` to disable | `false` |

**Priority:** System property → Environment variable → `utem.properties` → default

## Disabling the Reporter

```bash
# One-off
mvn test -Dutem.disabled=true

# Always disabled
UTEM_DISABLED=true mvn test
```

## Screenshot Support (Selenium)

```java
@BeforeMethod
public void setUp() {
    driver = new ChromeDriver();
    WebDriverRegistry.set(driver);
}

@AfterMethod
public void tearDown() {
    WebDriverRegistry.clear();
    driver.quit();
}
```

Screenshots are automatically captured on failure and attached in the dashboard.

## What Gets Reported

| TestNG callback | UTEM event |
|---|---|
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
