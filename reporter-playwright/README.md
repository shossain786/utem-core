# UTEM Reporter for Playwright

Streams Playwright test results to [UTEM Core](https://github.com/shossain786/utem-core) in real time as tests run.
Zero external dependencies — uses only Node.js built-ins.

## Installation

```bash
npm install --save-dev utem-reporter-playwright
```

## Usage

Add to `playwright.config.ts`:

```typescript
import { defineConfig } from '@playwright/test';

export default defineConfig({
  reporter: [
    ['utem-reporter-playwright'],
    ['html'],   // keep your existing reporters alongside
  ],
});
```

## Configuration

### Environment Variables

| Environment Variable | Description | Default |
|----------------------|-------------|---------|
| `UTEM_SERVER_URL` | UTEM Core server URL | `http://localhost:8080/utem` |
| `UTEM_RUN_NAME` | Custom name for the test run | `Playwright Test Run` |
| `UTEM_RUN_LABEL` | Label to tag the run (e.g. `regression`, `smoke`) | _(none)_ |
| `UTEM_JOB_NAME` | CI job name (e.g. Jenkins build name) | _(none)_ |
| `UTEM_DISABLED` | Set to `true` to disable the reporter entirely | `false` |

**Example — running with custom name:**
```bash
UTEM_SERVER_URL=http://myserver:8080/utem \
UTEM_RUN_NAME="Checkout E2E Suite" \
UTEM_RUN_LABEL="regression" \
npx playwright test
```

**Example — in `playwright.config.ts` using process.env:**
```typescript
process.env.UTEM_SERVER_URL = 'http://myserver:8080/utem';
process.env.UTEM_RUN_NAME  = 'Checkout E2E Suite';
```

### Config File (`utem.config.json`)

For settings that apply whenever you run tests (including directly from your IDE), create a `utem.config.json` file in your project root:

```json
{
  "serverUrl": "http://myserver:8080/utem",
  "runName": "Checkout E2E Suite",
  "runLabel": "regression",
  "jobName": "nightly",
  "disabled": false
}
```

| Key | Description | Default |
|-----|-------------|---------|
| `serverUrl` | UTEM Core server URL | `http://localhost:8080/utem` |
| `runName` | Custom name for the test run | `Playwright Test Run` |
| `runLabel` | Label to tag the run | _(none)_ |
| `jobName` | CI job name | _(none)_ |
| `disabled` | Set to `true` to disable the reporter entirely | `false` |

**Priority order:** Environment variable → `utem.config.json` → built-in default.

## Disabling the Reporter

Sometimes you want to run tests without sending results to UTEM (e.g. during local development or when running individual tests from the IDE).

**Option 1 — environment variable (one-off):**
```bash
UTEM_DISABLED=true npx playwright test
```

**Option 2 — `utem.config.json` (always disabled until you change it back):**

Create `utem.config.json` in your project root:
```json
{
  "disabled": true
}
```

When disabled, the reporter prints a single log line and sends no HTTP requests to the UTEM server.

## What Gets Reported

| Playwright event | UTEM event |
|-----------------|------------|
| `onBegin` | `TEST_RUN_STARTED` |
| `onTestBegin` (first test in file/describe) | `TEST_SUITE_STARTED` |
| `onTestBegin` | `TEST_CASE_STARTED` |
| Test passed | `TEST_PASSED` + `TEST_CASE_FINISHED` |
| Test failed / timed out | `TEST_FAILED` + `TEST_CASE_FINISHED` |
| Test skipped | `TEST_SKIPPED` + `TEST_CASE_FINISHED` |
| `onEnd` (per suite) | `TEST_SUITE_FINISHED` |
| `onEnd` | `TEST_RUN_FINISHED` |
| Screenshot attachment | `ATTACHMENT` + file upload |

## Screenshot Support

Screenshots captured by Playwright (via `screenshot: 'on'` or `screenshot: 'only-on-failure'` in your config) are automatically forwarded to UTEM Core and attached to the corresponding test case.

```typescript
// playwright.config.ts
export default defineConfig({
  use: {
    screenshot: 'only-on-failure',
  },
  reporter: [
    ['utem-reporter-playwright'],
  ],
});
```

## Requirements

- Node.js ≥ 18
- Playwright ≥ 1.35.0
- UTEM Core server running

**Quickstart with Docker:**
```bash
docker run -d -p 8080:8080 --name utem-core sddmhossain/utem-core
```

See [UTEM Core](https://github.com/shossain786/utem-core) for full setup options.
