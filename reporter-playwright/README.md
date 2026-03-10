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

All configuration is done via environment variables — no code changes required.

| Environment Variable | Description | Default |
|----------------------|-------------|---------|
| `UTEM_SERVER_URL` | UTEM Core server URL | `http://localhost:8080/utem` |
| `UTEM_RUN_NAME` | Custom name for the test run | `Playwright Test Run` |
| `UTEM_RUN_LABEL` | Label to tag the run (e.g. `regression`, `smoke`) | _(none)_ |
| `UTEM_JOB_NAME` | CI job name (e.g. Jenkins build name) | _(none)_ |

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
- UTEM Core server running (see [UTEM Core setup](https://github.com/shossain786/utem-core))
