# JUnit 5 Reporter

The UTEM JUnit 5 reporter is a zero-dependency test listener that sends results to the UTEM server in real time.

## Installation

Add to `pom.xml`:

```xml
<dependency>
  <groupId>com.utem</groupId>
  <artifactId>utem-reporter-junit5</artifactId>
  <version>0.9.2</version>
  <scope>test</scope>
</dependency>
```

Register the reporter in `src/test/resources/junit-platform.properties`:

```properties
junit.platform.reporting.open.xml.enabled=false
junit.jupiter.extensions.autodetection.enabled=true
```

Or register explicitly in your test class:

```java
@ExtendWith(UtemReporter.class)
public class MyTest { }
```

## Configuration

Create `src/test/resources/utem.properties`:

```properties
# Required
utem.server.url=http://localhost:8080
utem.api.key=utem_your_api_key_here

# Optional
utem.run.name=My Test Suite
utem.run.label=regression
utem.run.environment=staging
```

## Screenshot Capture (Selenium)

Register your WebDriver with `WebDriverRegistry` to capture screenshots on failure:

```java
import com.utem.reporter.junit5.WebDriverRegistry;

@BeforeEach
void setUp() {
    driver = new ChromeDriver();
    WebDriverRegistry.register(driver);
}

@AfterEach
void tearDown() {
    WebDriverRegistry.clear();
    driver.quit();
}
```

Screenshots are automatically captured on test failure and attached to the run in UTEM.

## CI Integration

### GitHub Actions

```yaml
- name: Run tests
  run: mvn test
  env:
    UTEM_SERVER_URL: http://your-utem-server:8080
    UTEM_API_KEY: ${{ secrets.UTEM_API_KEY }}
```

Override properties via environment variables:

| Property | Environment Variable |
|---|---|
| `utem.server.url` | `UTEM_SERVER_URL` |
| `utem.api.key` | `UTEM_API_KEY` |
| `utem.run.name` | `UTEM_RUN_NAME` |
| `utem.run.label` | `UTEM_RUN_LABEL` |
