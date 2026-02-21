# UTEM Adapter SDK

This document describes the UTEM event protocol so you can build a reporter adapter for any test framework.

UTEM uses a simple REST API: your adapter calls two HTTP endpoints with structured JSON as tests run. The server processes events in real time and updates the dashboard.

---

## Quick Start

1. Start the UTEM server (`mvn spring-boot:run` or your deployment)
2. Add your adapter to the test project (see framework-specific sections below)
3. Set the server URL: `-Dutem.server.url=http://localhost:8080/utem` (or `UTEM_SERVER_URL` env var)
4. Run your tests — events appear in the dashboard automatically

---

## HTTP API

### Single Event
```
POST /utem/events
Content-Type: application/json
```

### Batch Events (preferred for performance)
```
POST /utem/events/batch
Content-Type: application/json
```
Send up to 500 events per batch as a JSON array.

### Response Codes
| Code | Meaning |
|------|---------|
| `201 Created` | Event(s) accepted |
| `400 Bad Request` | Validation failure (missing required field) |
| `409 Conflict` | `eventId` already exists — treat as success (idempotent) |

---

## Event Structure

Every event has this top-level shape:

```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440000",
  "runId":   "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "eventType": "TEST_CASE_STARTED",
  "parentId": "a1b2c3d4-...",
  "timestamp": "2026-02-21T10:30:00.000Z",
  "payload": "{\"name\":\"Login Test\"}"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `eventId` | UUID string | Yes | Unique per event. Use a fresh UUID. Duplicate `eventId`s are silently ignored (idempotent). |
| `runId` | UUID string | Yes | Same value for every event in one test run. Generate once at run start. |
| `eventType` | string enum | Yes | See table below. |
| `parentId` | UUID string | No | The `eventId` of the parent event (e.g. the suite this test belongs to). Null for top-level events. |
| `timestamp` | ISO-8601 string | Yes | When this event occurred (`Instant.now().toString()` in Java). |
| `payload` | JSON string | Yes | A **JSON-encoded string** (escaped) containing event-specific fields. See below. |

> **Important:** `payload` is a JSON *string*, not a nested object. Serialize your payload object to a JSON string and embed it as a string value.

---

## Event Types and Payload Fields

### `TEST_RUN_STARTED`
Signal the beginning of a test run.

```json
{
  "eventId": "<uuid>",
  "runId": "<run-uuid>",
  "eventType": "TEST_RUN_STARTED",
  "parentId": null,
  "timestamp": "...",
  "payload": "{\"name\":\"My Project Tests\",\"totalTests\":42}"
}
```

Payload fields:
| Field | Type | Required |
|-------|------|----------|
| `name` | string | Yes |
| `totalTests` | int | No |
| `metadata` | string | No |

---

### `TEST_RUN_FINISHED`
Signal the end of a test run with final counts.

Payload fields:
| Field | Type | Required |
|-------|------|----------|
| `runStatus` | `PASSED` \| `FAILED` | Yes |
| `totalTests` | int | No |
| `passedTests` | int | No |
| `failedTests` | int | No |
| `skippedTests` | int | No |

---

### `TEST_SUITE_STARTED`
A suite or class containing multiple tests.

Payload fields:
| Field | Type | Required |
|-------|------|----------|
| `name` | string | Yes |
| `metadata` | string | No |

---

### `TEST_SUITE_FINISHED`
Payload fields:
| Field | Type | Required |
|-------|------|----------|
| `nodeStatus` | `PASSED` \| `FAILED` \| `SKIPPED` | Yes |
| `duration` | long (ms) | No |

---

### `TEST_CASE_STARTED`
A single test case (scenario, test method).

Payload fields:
| Field | Type | Required |
|-------|------|----------|
| `name` | string | Yes |
| `flaky` | boolean | No |
| `retryCount` | int | No |

---

### `TEST_CASE_FINISHED`
Payload fields:
| Field | Type | Required |
|-------|------|----------|
| `nodeStatus` | `PASSED` \| `FAILED` \| `SKIPPED` | Yes |
| `duration` | long (ms) | No |

---

### `TEST_PASSED`
Payload fields:
| Field | Type | Required |
|-------|------|----------|
| `duration` | long (ms) | No |

---

### `TEST_FAILED`
Payload fields:
| Field | Type | Required |
|-------|------|----------|
| `duration` | long (ms) | No |
| `errorMessage` | string | No |
| `stackTrace` | string | No |

---

### `TEST_SKIPPED`
Payload fields:
| Field | Type | Required |
|-------|------|----------|
| `errorMessage` | string | No (use for skip reason) |

---

### `TEST_STEP`
An individual step within a test case (Cucumber steps, etc.).

Payload fields:
| Field | Type | Required |
|-------|------|----------|
| `name` | string | Yes |
| `stepOrder` | int | No |
| `stepStatus` | `PASSED` \| `FAILED` \| `SKIPPED` | Yes |
| `duration` | long (ms) | No |
| `errorMessage` | string | No |
| `stackTrace` | string | No |

---

### `ATTACHMENT`
Link a file to a test (screenshot, log, video).

Payload fields:
| Field | Type | Required |
|-------|------|----------|
| `name` | string | Yes |
| `attachmentType` | `SCREENSHOT` \| `FILE` \| `VIDEO` \| `LOG` | Yes |
| `mimeType` | string | No |
| `fileSize` | long | No |
| `isFailureScreenshot` | boolean | No |

After sending the ATTACHMENT event, upload the file:
```
POST /utem/attachments/{attachmentId}/upload
Content-Type: multipart/form-data
```

---

## Hierarchy and Linking

Events form a tree via `parentId`. A typical run looks like:

```
TEST_RUN_STARTED          (eventId=A, parentId=null)
  TEST_SUITE_STARTED      (eventId=B, parentId=A)   ← runId matches
    TEST_CASE_STARTED     (eventId=C, parentId=B)
      TEST_STEP           (eventId=D, parentId=C)
    TEST_FAILED           (eventId=E, parentId=C)
    TEST_CASE_FINISHED    (eventId=F, parentId=C)
  TEST_SUITE_FINISHED     (eventId=G, parentId=B)
TEST_RUN_FINISHED         (eventId=H, parentId=A)
```

Rules:
- `runId` is the same on **all** events in the run
- `parentId` on `TEST_SUITE_*` = the `eventId` of `TEST_RUN_STARTED`
- `parentId` on `TEST_CASE_*` = the `eventId` of the containing `TEST_SUITE_STARTED`
- `parentId` on `TEST_STEP` = the `eventId` of the containing `TEST_CASE_STARTED`
- `parentId` on `TEST_PASSED/FAILED/SKIPPED` = the `eventId` of the `TEST_CASE_STARTED`

---

## Complete Example: 2-Test Run

```json
[
  {
    "eventId": "run-001",
    "runId":   "run-001",
    "eventType": "TEST_RUN_STARTED",
    "parentId": null,
    "timestamp": "2026-02-21T10:00:00.000Z",
    "payload": "{\"name\":\"Login Suite\",\"totalTests\":2}"
  },
  {
    "eventId": "suite-001",
    "runId":   "run-001",
    "eventType": "TEST_SUITE_STARTED",
    "parentId": "run-001",
    "timestamp": "2026-02-21T10:00:00.100Z",
    "payload": "{\"name\":\"LoginTest\"}"
  },
  {
    "eventId": "case-001",
    "runId":   "run-001",
    "eventType": "TEST_CASE_STARTED",
    "parentId": "suite-001",
    "timestamp": "2026-02-21T10:00:00.200Z",
    "payload": "{\"name\":\"testLoginSuccess\"}"
  },
  {
    "eventId": "pass-001",
    "runId":   "run-001",
    "eventType": "TEST_PASSED",
    "parentId": "case-001",
    "timestamp": "2026-02-21T10:00:01.000Z",
    "payload": "{\"duration\":800}"
  },
  {
    "eventId": "casefin-001",
    "runId":   "run-001",
    "eventType": "TEST_CASE_FINISHED",
    "parentId": "case-001",
    "timestamp": "2026-02-21T10:00:01.010Z",
    "payload": "{\"nodeStatus\":\"PASSED\",\"duration\":800}"
  },
  {
    "eventId": "case-002",
    "runId":   "run-001",
    "eventType": "TEST_CASE_STARTED",
    "parentId": "suite-001",
    "timestamp": "2026-02-21T10:00:01.100Z",
    "payload": "{\"name\":\"testLoginFail\"}"
  },
  {
    "eventId": "fail-001",
    "runId":   "run-001",
    "eventType": "TEST_FAILED",
    "parentId": "case-002",
    "timestamp": "2026-02-21T10:00:02.500Z",
    "payload": "{\"duration\":1400,\"errorMessage\":\"Expected 200 but got 401\",\"stackTrace\":\"at LoginTest.testLoginFail(LoginTest.java:42)\"}"
  },
  {
    "eventId": "casefin-002",
    "runId":   "run-001",
    "eventType": "TEST_CASE_FINISHED",
    "parentId": "case-002",
    "timestamp": "2026-02-21T10:00:02.510Z",
    "payload": "{\"nodeStatus\":\"FAILED\",\"duration\":1400}"
  },
  {
    "eventId": "suitefin-001",
    "runId":   "run-001",
    "eventType": "TEST_SUITE_FINISHED",
    "parentId": "suite-001",
    "timestamp": "2026-02-21T10:00:02.600Z",
    "payload": "{\"nodeStatus\":\"FAILED\",\"duration\":2500}"
  },
  {
    "eventId": "runfin-001",
    "runId":   "run-001",
    "eventType": "TEST_RUN_FINISHED",
    "parentId": "run-001",
    "timestamp": "2026-02-21T10:00:02.700Z",
    "payload": "{\"runStatus\":\"FAILED\",\"totalTests\":2,\"passedTests\":1,\"failedTests\":1,\"skippedTests\":0}"
  }
]
```

POST this array to `POST /utem/events/batch`.

---

## Configuration

| Method | Example |
|--------|---------|
| System property | `-Dutem.server.url=http://localhost:8080/utem` |
| Environment variable | `UTEM_SERVER_URL=http://localhost:8080/utem` |
| Default | `http://localhost:8080/utem` |

---

## Reliability Guidelines

1. **Generate unique UUIDs** for every `eventId`. Use `UUID.randomUUID()` in Java or `crypto.randomUUID()` in JS.
2. **Batch events** — the `/events/batch` endpoint is more efficient. Collect events in a background queue and flush every 200ms.
3. **Retry on failure** — retry up to 3 times with exponential backoff (100ms → 500ms → 2000ms). HTTP 409 is a success (already received).
4. **Never block test execution** — send events on a background daemon thread. If the queue is full, drop and log.
5. **Flush at test run end** — wait up to 30 seconds for the queue to drain before exiting.
6. **Escape payload strings** — `"` → `\"`, `\` → `\\`, newlines → `\n`. Control chars → `\uXXXX`.

---

## Available Adapters

| Framework | Module | Discovery |
|-----------|--------|-----------|
| JUnit 5 | `reporter-junit5` | SPI auto-discovery (`META-INF/services`) |
| Cucumber (Java) | `reporter-junit5` | `@CucumberOptions(plugin = {"com.utem.reporter.junit5.UtemCucumberPlugin"})` |
| TestNG | `reporter-testng` | SPI auto-discovery or `testng.xml` listeners |

### JUnit 5
```xml
<dependency>
  <groupId>com.utem</groupId>
  <artifactId>reporter-junit5</artifactId>
  <version>0.1.0</version>
  <scope>test</scope>
</dependency>
```
No further configuration needed — auto-discovered via JUnit Platform SPI.

### TestNG
```xml
<dependency>
  <groupId>com.utem</groupId>
  <artifactId>reporter-testng</artifactId>
  <version>0.1.0</version>
  <scope>test</scope>
</dependency>
```
Either auto-discovered via SPI or add to `testng.xml`:
```xml
<listeners>
  <listener class-name="com.utem.reporter.testng.UtemTestNGListener"/>
</listeners>
```

### Selenium Screenshots (optional)
Register your WebDriver before tests:
```java
// JUnit 5 / TestNG
WebDriverRegistry.set(driver);  // at test start
WebDriverRegistry.clear();      // at test end
```
Failed tests will automatically capture and upload a screenshot.
