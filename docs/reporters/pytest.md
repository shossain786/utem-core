# pytest Reporter

The UTEM pytest reporter streams test results to the UTEM server in real time. Zero external dependencies — uses only the Python standard library. Auto-discovered by pytest with no configuration needed.

## Installation

```bash
pip install utem-pytest-reporter
```

## Usage

Once installed the plugin is auto-discovered — no changes to `conftest.py` or `pytest.ini` needed.

```bash
UTEM_SERVER_URL=http://localhost:8080/utem pytest
```

Or via CLI option:

```bash
pytest --utem-url=http://localhost:8080/utem
```

## Configuration

### utem.config.json

Create `utem.config.json` in your project root:

```json
{
  "serverUrl": "http://myserver:8080/utem",
  "runName": "My Test Suite",
  "runLabel": "regression",
  "jobName": "nightly",
  "disabled": false
}
```

### Environment Variables

```bash
UTEM_SERVER_URL=http://myserver:8080/utem \
UTEM_RUN_NAME="My Suite" \
UTEM_RUN_LABEL=regression \
pytest
```

### All options

| Env Variable | CLI | JSON key | Description | Default |
|---|---|---|---|---|
| `UTEM_SERVER_URL` | `--utem-url` | `serverUrl` | UTEM server URL | `http://localhost:8080/utem` |
| `UTEM_RUN_NAME` | — | `runName` | Run name | Session name |
| `UTEM_RUN_LABEL` | — | `runLabel` | Tag (e.g. `regression`) | — |
| `UTEM_JOB_NAME` | — | `jobName` | CI job name | — |
| `UTEM_DISABLED` | — | `disabled` | Set `true` to disable | `false` |

**Priority:** CLI → Environment variable → `utem.config.json` → default

## Screenshot Support (Selenium)

Register your WebDriver in a fixture:

```python
import pytest
import utem_pytest_reporter

@pytest.fixture(autouse=True)
def utem_driver(driver):
    utem_pytest_reporter.register_driver(driver)
    yield
    utem_pytest_reporter.unregister_driver()
```

Screenshots are automatically captured on failure and attached in the dashboard.

## CI Integration

### GitHub Actions

```yaml
- name: Run pytest
  run: pytest
  env:
    UTEM_SERVER_URL: http://your-utem-server:8080/utem
    UTEM_API_KEY: ${{ secrets.UTEM_API_KEY }}
    UTEM_RUN_NAME: "pytest - ${{ github.ref_name }}"
    UTEM_RUN_LABEL: regression
```

## Disabling the Reporter

```bash
UTEM_DISABLED=true pytest
```

## What Gets Reported

| pytest event | UTEM event |
|---|---|
| Session start | `TEST_RUN_STARTED` |
| First test in a file | `TEST_SUITE_STARTED` |
| Each test start | `TEST_CASE_STARTED` |
| Test pass | `TEST_PASSED` + `TEST_CASE_FINISHED` |
| Test failure | `TEST_FAILED` + `TEST_CASE_FINISHED` |
| Test skip | `TEST_SKIPPED` + `TEST_CASE_FINISHED` |
| Session end | `TEST_SUITE_FINISHED` × N + `TEST_RUN_FINISHED` |
| Failure screenshot | `ATTACHMENT` + file upload |

## Requirements

- Python 3.8+
- pytest 7+
