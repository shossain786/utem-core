# Jest Reporter

The UTEM Jest reporter is a zero-dependency custom reporter that streams test results to the UTEM server in real time as tests run.

## Installation

```bash
npm install --save-dev utem-jest-reporter
```

Or link locally during development:
```bash
# In reporter-jest directory
npm link
# In your project
npm link utem-jest-reporter
```

## Configuration

### jest.config.js

```javascript
module.exports = {
  reporters: [
    "default",
    ["utem-jest-reporter", { serverUrl: "http://localhost:8080/utem" }]
  ]
};
```

### jest.config.ts

```typescript
export default {
  reporters: [
    "default",
    ["utem-jest-reporter", { serverUrl: "http://localhost:8080/utem" }],
  ],
};
```

### Configuration options

| Method | Example |
|---|---|
| Reporter option | `{ serverUrl: "http://host:8080/utem" }` |
| Environment variable | `UTEM_SERVER_URL=http://host:8080/utem` |
| Default | `http://localhost:8080/utem` |

### With API key (when auth is enabled)

```javascript
module.exports = {
  reporters: [
    "default",
    ["utem-jest-reporter", {
      serverUrl: "http://localhost:8080/utem",
      apiKey: process.env.UTEM_API_KEY
    }]
  ]
};
```

## CI Integration

### GitHub Actions

```yaml
- name: Run tests
  run: npx jest
  env:
    UTEM_SERVER_URL: http://your-utem-server:8080/utem
    UTEM_API_KEY: ${{ secrets.UTEM_API_KEY }}
```

### Jenkins / GitLab CI

```bash
UTEM_SERVER_URL=http://utem:8080/utem UTEM_API_KEY=utem_xxx npx jest
```

## What Gets Reported

| Jest event | UTEM event |
|---|---|
| `onRunStart` | `TEST_RUN_STARTED` |
| `onTestFileStart` | `TEST_SUITE_STARTED` (per file) |
| `onTestCaseResult` passed | `TEST_PASSED` + `TEST_CASE_FINISHED` |
| `onTestCaseResult` failed | `TEST_FAILED` + `TEST_CASE_FINISHED` |
| `onTestCaseResult` skipped/pending/todo | `TEST_SKIPPED` + `TEST_CASE_FINISHED` |
| `onTestFileResult` | `TEST_SUITE_FINISHED` |
| `onRunComplete` | `TEST_RUN_FINISHED` |

On failure, the assertion message and full stack trace are captured and shown in the dashboard.

## Requirements

- Node.js ≥ 18
- Jest ≥ 27
