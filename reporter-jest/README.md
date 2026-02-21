# UTEM Reporter for Jest

Streams test results to [UTEM Core](../README.md) in real time as tests run.
Zero external dependencies — uses only Node.js built-ins.

## Installation

```bash
npm install --save-dev utem-jest-reporter
```

Or link locally from this directory:
```bash
npm link
# In your project:
npm link utem-jest-reporter
```

## Usage

Add to `jest.config.js`:

```javascript
module.exports = {
  reporters: [
    "default",
    ["utem-jest-reporter", { serverUrl: "http://localhost:8080/utem" }]
  ]
};
```

Or in `jest.config.ts`:
```typescript
export default {
  reporters: [
    "default",
    ["utem-jest-reporter", { serverUrl: "http://localhost:8080/utem" }],
  ],
};
```

## Configuration

| Method | Example |
|--------|---------|
| Reporter option | `{ serverUrl: "http://host:8080/utem" }` |
| Environment variable | `UTEM_SERVER_URL=http://host:8080/utem` |
| Default | `http://localhost:8080/utem` |

## What Gets Reported

| Jest event | UTEM event |
|-----------|------------|
| `onRunStart` | `TEST_RUN_STARTED` |
| `onTestFileStart` | `TEST_SUITE_STARTED` |
| `onTestCaseResult` (passed) | `TEST_PASSED` + `TEST_CASE_FINISHED` |
| `onTestCaseResult` (failed) | `TEST_FAILED` + `TEST_CASE_FINISHED` |
| `onTestCaseResult` (skipped/pending) | `TEST_SKIPPED` + `TEST_CASE_FINISHED` |
| `onTestFileResult` | `TEST_SUITE_FINISHED` |
| `onRunComplete` | `TEST_RUN_FINISHED` |

## Requirements

- Node.js ≥ 18
- Jest ≥ 27
