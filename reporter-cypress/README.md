# UTEM Reporter for Cypress

Streams test results to [UTEM Core](../README.md) in real time as specs run.
Zero external dependencies — uses only Node.js built-ins.

## Installation

```bash
npm install --save-dev utem-cypress-reporter
```

## Usage

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

## Configuration

| Method | Example |
|---|---|
| Plugin option | `{ serverUrl: 'http://host:8080/utem' }` |
| Environment variable | `UTEM_SERVER_URL=http://host:8080/utem` |
| Default | `http://localhost:8080/utem` |

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

## Requirements

- Node.js ≥ 18
- Cypress ≥ 10
