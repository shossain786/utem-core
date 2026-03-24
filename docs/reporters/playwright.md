# Playwright Reporter

The UTEM Playwright reporter streams test results to the UTEM server in real time, including automatic screenshot forwarding on failure. Zero external dependencies — uses only Node.js built-ins.

## Installation

```bash
npm install --save-dev utem-reporter-playwright
```

## Configuration

Add to `playwright.config.ts`:

```typescript
import { defineConfig } from '@playwright/test';

export default defineConfig({
  reporter: [
    ['utem-reporter-playwright'],
    ['html'],  // keep existing reporters alongside
  ],
});
```

### Environment Variables

```bash
UTEM_SERVER_URL=http://myserver:8080/utem \
UTEM_RUN_NAME="Checkout E2E Suite" \
UTEM_RUN_LABEL="regression" \
npx playwright test
```

### utem.config.json

Create `utem.config.json` in your project root:

```json
{
  "serverUrl": "http://myserver:8080/utem",
  "runName": "Checkout E2E Suite",
  "runLabel": "regression",
  "jobName": "nightly",
  "disabled": false
}
```

### All options

| Env Variable | JSON key | Description | Default |
|---|---|---|---|
| `UTEM_SERVER_URL` | `serverUrl` | UTEM server URL | `http://localhost:8080/utem` |
| `UTEM_RUN_NAME` | `runName` | Run name | `Playwright Test Run` |
| `UTEM_RUN_LABEL` | `runLabel` | Tag (e.g. `regression`) | — |
| `UTEM_JOB_NAME` | `jobName` | CI job name | — |
| `UTEM_DISABLED` | `disabled` | Set `true` to disable | `false` |

**Priority:** Environment variable → `utem.config.json` → default

## Screenshot Support

Screenshots captured by Playwright are automatically forwarded to UTEM and attached to the failing test.

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

## CI Integration

### GitHub Actions

```yaml
- name: Run Playwright tests
  run: npx playwright test
  env:
    UTEM_SERVER_URL: http://your-utem-server:8080/utem
    UTEM_API_KEY: ${{ secrets.UTEM_API_KEY }}
    UTEM_RUN_NAME: "E2E - ${{ github.ref_name }}"
    UTEM_RUN_LABEL: regression
```

## Disabling the Reporter

```bash
UTEM_DISABLED=true npx playwright test
```

## What Gets Reported

| Playwright event | UTEM event |
|---|---|
| `onBegin` | `TEST_RUN_STARTED` |
| `onTestBegin` (first in file) | `TEST_SUITE_STARTED` |
| `onTestBegin` | `TEST_CASE_STARTED` |
| Test passed | `TEST_PASSED` + `TEST_CASE_FINISHED` |
| Test failed / timed out | `TEST_FAILED` + `TEST_CASE_FINISHED` |
| Test skipped | `TEST_SKIPPED` + `TEST_CASE_FINISHED` |
| `onEnd` (per suite) | `TEST_SUITE_FINISHED` |
| `onEnd` | `TEST_RUN_FINISHED` |
| Screenshot attachment | `ATTACHMENT` + file upload |

## Requirements

- Node.js ≥ 18
- Playwright ≥ 1.35.0
