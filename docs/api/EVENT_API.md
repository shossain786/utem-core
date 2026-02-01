# Event Ingestion API

Base URL: `/utem/events`

## Overview

The Event Ingestion API allows test frameworks and adapters to send test execution events to UTEM Core. Events are stored in the `event_log` table and later processed to build the test hierarchy.

---

## Event Types

| Event Type | Description |
|------------|-------------|
| `TEST_RUN_STARTED` | Test run has started |
| `TEST_RUN_FINISHED` | Test run has completed |
| `TEST_SUITE_STARTED` | Test suite has started |
| `TEST_SUITE_FINISHED` | Test suite has completed |
| `TEST_CASE_STARTED` | Individual test case has started |
| `TEST_CASE_FINISHED` | Individual test case has completed |
| `TEST_STEP` | A step within a test case |
| `TEST_PASSED` | Test has passed |
| `TEST_FAILED` | Test has failed |
| `TEST_SKIPPED` | Test was skipped |
| `ATTACHMENT` | File attachment (screenshot, log, etc.) |

---

## Endpoints

### 1. Ingest Single Event

Ingests a single test event.

**Request**

```
POST /utem/events
Content-Type: application/json
```

**Request Body**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `eventId` | string | Yes | Unique identifier for this event (UUID recommended) |
| `runId` | string | Yes | Identifier for the test run |
| `eventType` | string | Yes | One of the event types listed above |
| `parentId` | string | No | ID of the parent event (for hierarchy) |
| `timestamp` | string (ISO-8601) | Yes | When the event occurred |
| `payload` | string (JSON) | Yes | Event-specific data as JSON string |

**Example Request**

```json
{
  "eventId": "evt-001-abc",
  "runId": "run-2024-001",
  "eventType": "TEST_RUN_STARTED",
  "parentId": null,
  "timestamp": "2024-01-15T10:30:00Z",
  "payload": "{\"name\": \"Regression Suite\", \"environment\": \"staging\", \"tags\": [\"smoke\", \"regression\"]}"
}
```

**Response**

| Status | Description |
|--------|-------------|
| `201 Created` | Event successfully ingested |
| `400 Bad Request` | Invalid request body or missing required fields |
| `409 Conflict` | Event with this `eventId` already exists |

**Example Response (201)**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "eventId": "evt-001-abc",
  "runId": "run-2024-001",
  "eventType": "TEST_RUN_STARTED",
  "parentId": null,
  "timestamp": "2024-01-15T10:30:00Z",
  "payload": "{\"name\": \"Regression Suite\", \"environment\": \"staging\", \"tags\": [\"smoke\", \"regression\"]}",
  "receivedAt": "2024-01-15T10:30:01.123Z"
}
```

---

### 2. Ingest Batch Events

Ingests multiple events in a single request. Duplicate events (by `eventId`) are automatically skipped.

**Request**

```
POST /utem/events/batch
Content-Type: application/json
```

**Request Body**

Array of event objects (same structure as single event).

**Example Request**

```json
[
  {
    "eventId": "evt-001",
    "runId": "run-2024-001",
    "eventType": "TEST_RUN_STARTED",
    "parentId": null,
    "timestamp": "2024-01-15T10:30:00Z",
    "payload": "{\"name\": \"Regression Suite\"}"
  },
  {
    "eventId": "evt-002",
    "runId": "run-2024-001",
    "eventType": "TEST_SUITE_STARTED",
    "parentId": "evt-001",
    "timestamp": "2024-01-15T10:30:01Z",
    "payload": "{\"name\": \"Login Tests\"}"
  },
  {
    "eventId": "evt-003",
    "runId": "run-2024-001",
    "eventType": "TEST_CASE_STARTED",
    "parentId": "evt-002",
    "timestamp": "2024-01-15T10:30:02Z",
    "payload": "{\"name\": \"Valid Login Test\"}"
  }
]
```

**Response**

| Status | Description |
|--------|-------------|
| `201 Created` | Events successfully ingested (returns only newly created events) |
| `400 Bad Request` | Invalid request body |

**Example Response (201)**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "eventId": "evt-001",
    "runId": "run-2024-001",
    "eventType": "TEST_RUN_STARTED",
    "parentId": null,
    "timestamp": "2024-01-15T10:30:00Z",
    "payload": "{\"name\": \"Regression Suite\"}",
    "receivedAt": "2024-01-15T10:30:05.123Z"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "eventId": "evt-002",
    "runId": "run-2024-001",
    "eventType": "TEST_SUITE_STARTED",
    "parentId": "evt-001",
    "timestamp": "2024-01-15T10:30:01Z",
    "payload": "{\"name\": \"Login Tests\"}",
    "receivedAt": "2024-01-15T10:30:05.124Z"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440003",
    "eventId": "evt-003",
    "runId": "run-2024-001",
    "eventType": "TEST_CASE_STARTED",
    "parentId": "evt-002",
    "timestamp": "2024-01-15T10:30:02Z",
    "payload": "{\"name\": \"Valid Login Test\"}",
    "receivedAt": "2024-01-15T10:30:05.125Z"
  }
]
```

---

### 3. Get Event by ID

Retrieves a single event by its internal ID.

**Request**

```
GET /utem/events/{id}
```

**Path Parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | string | Internal UUID of the event |

**Response**

| Status | Description |
|--------|-------------|
| `200 OK` | Event found |
| `404 Not Found` | Event not found |

**Example Response (200)**

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "eventId": "evt-001-abc",
  "runId": "run-2024-001",
  "eventType": "TEST_RUN_STARTED",
  "parentId": null,
  "timestamp": "2024-01-15T10:30:00Z",
  "payload": "{\"name\": \"Regression Suite\"}",
  "receivedAt": "2024-01-15T10:30:01.123Z"
}
```

---

### 4. Get Events by Run ID

Retrieves all events for a specific test run, ordered by timestamp.

**Request**

```
GET /utem/events?runId={runId}
```

**Query Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `runId` | string | Yes | The test run identifier |

**Response**

| Status | Description |
|--------|-------------|
| `200 OK` | List of events (may be empty) |

**Example Response (200)**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440001",
    "eventId": "evt-001",
    "runId": "run-2024-001",
    "eventType": "TEST_RUN_STARTED",
    "parentId": null,
    "timestamp": "2024-01-15T10:30:00Z",
    "payload": "{\"name\": \"Regression Suite\"}",
    "receivedAt": "2024-01-15T10:30:01.123Z"
  },
  {
    "id": "550e8400-e29b-41d4-a716-446655440002",
    "eventId": "evt-002",
    "runId": "run-2024-001",
    "eventType": "TEST_SUITE_STARTED",
    "parentId": "evt-001",
    "timestamp": "2024-01-15T10:30:01Z",
    "payload": "{\"name\": \"Login Tests\"}",
    "receivedAt": "2024-01-15T10:30:02.123Z"
  }
]
```

---

### 5. Get Events by Type

Retrieves events filtered by event type, optionally filtered by run ID.

**Request**

```
GET /utem/events/type/{eventType}
GET /utem/events/type/{eventType}?runId={runId}
```

**Path Parameters**

| Parameter | Type | Description |
|-----------|------|-------------|
| `eventType` | string | One of the event types listed above |

**Query Parameters**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `runId` | string | No | Filter by test run identifier |

**Response**

| Status | Description |
|--------|-------------|
| `200 OK` | List of events (may be empty) |

**Example Request**

```
GET /utem/events/type/TEST_FAILED?runId=run-2024-001
```

**Example Response (200)**

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440010",
    "eventId": "evt-010",
    "runId": "run-2024-001",
    "eventType": "TEST_FAILED",
    "parentId": "evt-005",
    "timestamp": "2024-01-15T10:35:00Z",
    "payload": "{\"name\": \"Invalid Login Test\", \"error\": \"Expected 401, got 500\"}",
    "receivedAt": "2024-01-15T10:35:01.123Z"
  }
]
```

---

## Payload Examples

### TEST_RUN_STARTED

```json
{
  "name": "Regression Suite",
  "environment": "staging",
  "tags": ["smoke", "regression"],
  "metadata": {
    "browser": "chrome",
    "os": "linux",
    "buildNumber": "1234"
  }
}
```

### TEST_SUITE_STARTED

```json
{
  "name": "Login Tests",
  "description": "Tests for user authentication",
  "tags": ["auth", "critical"]
}
```

### TEST_CASE_STARTED

```json
{
  "name": "Valid Login Test",
  "description": "Test login with valid credentials",
  "tags": ["smoke"],
  "parameters": {
    "username": "testuser",
    "password": "***"
  }
}
```

### TEST_STEP

```json
{
  "name": "Enter username",
  "action": "sendKeys",
  "target": "#username",
  "value": "testuser",
  "order": 1
}
```

### TEST_PASSED

```json
{
  "name": "Valid Login Test",
  "duration": 1500,
  "assertions": 3
}
```

### TEST_FAILED

```json
{
  "name": "Invalid Login Test",
  "duration": 2000,
  "error": "AssertionError: Expected status 401, got 500",
  "stackTrace": "at LoginTest.testInvalidLogin(LoginTest.java:45)\n...",
  "screenshots": ["screenshot-001.png"]
}
```

### TEST_SKIPPED

```json
{
  "name": "Feature X Test",
  "reason": "Feature not enabled in staging environment"
}
```

### ATTACHMENT

```json
{
  "name": "failure-screenshot.png",
  "type": "image/png",
  "size": 102400,
  "path": "/attachments/run-2024-001/screenshot-001.png"
}
```

---

## Error Responses

### 400 Bad Request

```json
{
  "error": "Validation failed",
  "details": [
    "eventId: must not be blank",
    "timestamp: must not be null"
  ]
}
```

### 409 Conflict

```json
{
  "error": "Event with ID 'evt-001' already exists"
}
```

### 404 Not Found

```json
{
  "error": "Event not found with id: 550e8400-e29b-41d4-a716-446655440000"
}
```

---

## Usage Examples

### cURL

**Ingest single event:**

```bash
curl -X POST http://localhost:8080/utem/events \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-001",
    "runId": "run-2024-001",
    "eventType": "TEST_RUN_STARTED",
    "timestamp": "2024-01-15T10:30:00Z",
    "payload": "{\"name\": \"My Test Run\"}"
  }'
```

**Get events by run ID:**

```bash
curl http://localhost:8080/utem/events?runId=run-2024-001
```

**Get failed tests:**

```bash
curl http://localhost:8080/utem/events/type/TEST_FAILED?runId=run-2024-001
```

### Java (RestTemplate)

```java
RestTemplate restTemplate = new RestTemplate();

// Ingest event
EventRequest request = new EventRequest(
    UUID.randomUUID().toString(),
    "run-2024-001",
    EventLog.EventType.TEST_RUN_STARTED,
    null,
    Instant.now(),
    "{\"name\": \"My Test Run\"}"
);

ResponseEntity<EventResponse> response = restTemplate.postForEntity(
    "http://localhost:8080/utem/events",
    request,
    EventResponse.class
);
```

### JavaScript (fetch)

```javascript
// Ingest event
const response = await fetch('http://localhost:8080/utem/events', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    eventId: crypto.randomUUID(),
    runId: 'run-2024-001',
    eventType: 'TEST_RUN_STARTED',
    timestamp: new Date().toISOString(),
    payload: JSON.stringify({ name: 'My Test Run' })
  })
});

const event = await response.json();
```

---

## Notes

1. **Idempotency**: The `eventId` field ensures idempotent event ingestion. Duplicate events are rejected with 409 Conflict (single) or silently skipped (batch).

2. **Ordering**: Events are stored with both `timestamp` (when the event occurred) and `receivedAt` (when UTEM received it). Query results are ordered by `timestamp`.

3. **Payload Format**: The `payload` field must be a valid JSON string. It allows flexible event-specific data without schema changes.

4. **Parent-Child Relationships**: Use `parentId` to establish hierarchy:
   - `TEST_SUITE_STARTED` → parent is `TEST_RUN_STARTED` eventId
   - `TEST_CASE_STARTED` → parent is `TEST_SUITE_STARTED` eventId
   - `TEST_STEP` → parent is `TEST_CASE_STARTED` eventId
