# UTEM Reporter for pytest

Streams test results to [UTEM Core](../README.md) in real time as tests run.

## Installation

```bash
pip install -e .
```

Or install from the built wheel:
```bash
pip install utem-reporter-0.1.0-py3-none-any.whl
```

## Usage

Once installed, the plugin is auto-discovered by pytest. No configuration in `conftest.py` or `pytest.ini` is needed.

```bash
# Point to your UTEM Core server
UTEM_SERVER_URL=http://localhost:8080/utem pytest

# Or use the command-line option
pytest --utem-url=http://localhost:8080/utem
```

## Configuration

| Method | Example |
|--------|---------|
| Environment variable | `UTEM_SERVER_URL=http://host:8080/utem` |
| Command-line option | `pytest --utem-url=http://host:8080/utem` |
| Default | `http://localhost:8080/utem` |

## Selenium Screenshot Support (Optional)

Register your WebDriver in a fixture to get automatic screenshots on failure:

```python
import pytest
import utem_reporter

@pytest.fixture(autouse=True)
def utem_driver(driver):
    utem_reporter.register_driver(driver)
    yield
    utem_reporter.unregister_driver()
```

## What Gets Reported

| pytest event | UTEM event |
|-------------|------------|
| Session start | `TEST_RUN_STARTED` |
| First test in a file | `TEST_SUITE_STARTED` |
| Each test start | `TEST_CASE_STARTED` |
| Test pass | `TEST_PASSED` + `TEST_CASE_FINISHED` |
| Test failure | `TEST_FAILED` + `TEST_CASE_FINISHED` |
| Test skip | `TEST_SKIPPED` + `TEST_CASE_FINISHED` |
| Session end | `TEST_SUITE_FINISHED` × N + `TEST_RUN_FINISHED` |
