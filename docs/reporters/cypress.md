# Cypress Reporter

The UTEM Cypress reporter is a zero-dependency plugin that streams test results to the UTEM server in real time as specs run.

## Installation

```bash
npm install --save-dev utem-cypress-reporter
```

## Configuration

### cypress.config.js

```javascript
const { defineConfig } = require('cypress');
const { registerUtemPlugin } = require('utem-cypress-reporter');

module.exports = defineConfig({
  e2e: {
    setupNodeEvents(on, config) {
      registerUtemPlugin(on, config, {
        serverUrl: 'http://localhost:8080/utem',
        apiKey: process.env.UTEM_API_KEY,
      });
    }
  }
});
```

### cypress.config.ts

```typescript
import { defineConfig } from 'cypress';
import { registerUtemPlugin } from 'utem-cypress-reporter';

export default defineConfig({
  e2e: {
    setupNodeEvents(on, config) {
      registerUtemPlugin(on, config, {
        serverUrl: 'http://localhost:8080/utem',
        apiKey: process.env.UTEM_API_KEY,
      });
    }
  }
});
```

### Configuration options

| Method | Example |
|---|---|
| Plugin option | `{ serverUrl: 'http://host:8080/utem' }` |
| Environment variable | `UTEM_SERVER_URL=http://host:8080/utem` |
| Default | `http://localhost:8080/utem` |

## CI Integration

### GitHub Actions

```yaml
- name: Run Cypress tests
  run: npx cypress run
  env:
    UTEM_SERVER_URL: http://your-utem-server:8080/utem
    UTEM_API_KEY: ${{ secrets.UTEM_API_KEY }}
```

### Jenkins / GitLab CI

```bash
UTEM_SERVER_URL=http://utem:8080/utem UTEM_API_KEY=utem_xxx npx cypress run
```

## Custom Run Name

Set via environment variable:

```bash
UTEM_RUN_NAME="Nightly E2E" npx cypress run
```

## What Gets Reported

| Cypress event | UTEM event |
|---|---|
| `before:run` | `TEST_RUN_STARTED` |
| `before:spec` | `TEST_SUITE_STARTED` (per spec file) |
| `after:spec` test passed | `TEST_PASSED` + `TEST_CASE_FINISHED` |
| `after:spec` test failed | `TEST_FAILED` + `TEST_CASE_FINISHED` |
| `after:spec` test pending | `TEST_SKIPPED` + `TEST_CASE_FINISHED` |
| `after:spec` | `TEST_SUITE_FINISHED` |
| `after:run` | `TEST_RUN_FINISHED` |

On failure, the assertion error message and stack trace are captured and shown in the dashboard.

## Requirements

- Node.js ≥ 18
- Cypress ≥ 10
