# Cucumber Reporter

UTEM provides a Cucumber plugin that reports scenario results in real time, including step-level details and screenshots.

## Installation

Same dependency as JUnit 5:

```xml
<dependency>
  <groupId>com.utem</groupId>
  <artifactId>utem-reporter-junit5</artifactId>
  <version>0.9.1</version>
  <scope>test</scope>
</dependency>
```

## Configuration

### Register the Plugin

Add to your `@CucumberOptions`:

```java
@RunWith(Cucumber.class)
@CucumberOptions(
    features = "src/test/resources/features",
    glue = "com.example.steps",
    plugin = {
        "pretty",
        "com.utem.reporter.junit5.UtemCucumberPlugin"
    }
)
public class TestRunner { }
```

### utem.properties

Create `src/test/resources/utem.properties`:

```properties
utem.server.url=http://localhost:8080
utem.api.key=utem_your_api_key_here
utem.run.name=Cucumber Regression
utem.run.label=regression
```

## Screenshot Capture

For Selenium screenshot capture in Cucumber, use `WebDriverRegistry` in your hooks:

```java
import com.utem.reporter.junit5.WebDriverRegistry;
import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;

public class Hooks {

    private WebDriver driver;

    @Before
    public void setUp() {
        driver = new ChromeDriver();
        WebDriverRegistry.register(driver);
    }

    @After
    public void tearDown(Scenario scenario) {
        // Embed screenshot in Cucumber HTML report
        if (scenario.isFailed()) {
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            scenario.attach(screenshot, "image/png", "screenshot");
        }
        WebDriverRegistry.clear();
        driver.quit();
    }
}
```

::: tip
UTEM captures screenshots automatically on step failure before `@After` hooks run. The `scenario.attach()` call is only needed if you also want screenshots embedded in the Cucumber HTML report.
:::

## What Gets Reported

- Each **feature file** → one test suite in UTEM
- Each **scenario** → one test case
- Each **step** → one test node with pass/fail/pending status
- **Screenshots** → attached to the failed step
- **Error messages** → stack traces captured on failure
