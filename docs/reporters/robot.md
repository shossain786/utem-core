# Robot Framework Reporter

The UTEM Robot Framework listener streams test results to the UTEM dashboard in real time as tests run. Zero external dependencies — uses only the Python standard library. Compatible with Robot Framework 4 and 5.

## Installation

```bash
pip install utem-robot-reporter
```

## Usage

```bash
robot --listener utem_robot_reporter.UtemListener tests/
```

With server URL and API key as listener arguments:

```bash
robot --listener "utem_robot_reporter.UtemListener:http://myserver:8080/utem:my-api-key" tests/
```

## Configuration

### Environment Variables

```bash
UTEM_SERVER_URL=http://myserver:8080/utem \
UTEM_API_KEY=utem_your_api_key_here \
UTEM_RUN_NAME="My Test Suite" \
UTEM_RUN_LABEL=regression \
robot --listener utem_robot_reporter.UtemListener tests/
```

### utem.config.json

Create `utem.config.json` in your project root:

```json
{
  "serverUrl": "http://myserver:8080/utem",
  "apiKey": "utem_your_api_key_here",
  "runName": "My Test Suite",
  "runLabel": "regression",
  "jobName": "nightly",
  "disabled": false
}
```

### All options

| Env Variable | Listener Arg | JSON key | Description | Default |
|---|---|---|---|---|
| `UTEM_SERVER_URL` | 1st positional | `serverUrl` | UTEM server URL | `http://localhost:8080/utem` |
| `UTEM_API_KEY` | 2nd positional | `apiKey` | Project API key | — |
| `UTEM_RUN_NAME` | — | `runName` | Run name | Suite name |
| `UTEM_RUN_LABEL` | — | `runLabel` | Tag (e.g. `regression`) | — |
| `UTEM_JOB_NAME` | — | `jobName` | CI job name | — |
| `UTEM_DISABLED` | — | `disabled` | Set `true` to disable | `false` |

**Priority:** Listener args → Environment variables → `utem.config.json` → defaults

## Screenshot Support (Selenium)

Register your WebDriver after opening the browser:

```robotframework
*** Settings ***
Library    SeleniumLibrary
Library    utem_robot_reporter

Suite Setup    Open Browser And Register

*** Keywords ***
Open Browser And Register
    Open Browser    https://example.com    chrome
    Register Driver    ${BROWSER}
```

Or from a Python custom library:

```python
import utem_robot_reporter

utem_robot_reporter.register_driver(driver)
```

Screenshots are automatically captured on test failure and attached in the UTEM dashboard.

## CI Integration

### GitHub Actions

```yaml
- name: Run Robot Framework tests
  run: robot --listener utem_robot_reporter.UtemListener tests/
  env:
    UTEM_SERVER_URL: http://your-utem-server:8080/utem
    UTEM_API_KEY: ${{ secrets.UTEM_API_KEY }}
    UTEM_RUN_NAME: "Robot - ${{ github.ref_name }}"
    UTEM_RUN_LABEL: regression
```

### Jenkins

```groovy
stage('Test') {
    environment {
        UTEM_SERVER_URL = 'http://your-utem-server:8080/utem'
        UTEM_API_KEY    = credentials('utem-api-key')
        UTEM_RUN_NAME   = "Robot - ${env.BRANCH_NAME}"
    }
    steps {
        sh 'robot --listener utem_robot_reporter.UtemListener tests/'
    }
}
```

## Disabling the Reporter

```bash
UTEM_DISABLED=true robot tests/
```

## What Gets Reported

| Robot Framework event | UTEM event |
|---|---|
| Root suite start | `TEST_RUN_STARTED` + `TEST_SUITE_STARTED` |
| Nested suite start | `TEST_SUITE_STARTED` |
| Test start | `TEST_CASE_STARTED` |
| Test PASS | `TEST_PASSED` + `TEST_CASE_FINISHED` |
| Test FAIL | `TEST_FAILED` + `TEST_CASE_FINISHED` |
| Test SKIP / NOT RUN | `TEST_SKIPPED` + `TEST_CASE_FINISHED` |
| Suite end | `TEST_SUITE_FINISHED` |
| Root suite end | `TEST_RUN_FINISHED` |
| Failure screenshot | `ATTACHMENT` + file upload |

## Requirements

- Python 3.8+
- Robot Framework 4+
