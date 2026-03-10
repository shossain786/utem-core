
# UTEM Core  
**UTEM — A Universal Language for Test Truth**

## Overview

UTEM Core is a real-time, self-hosted test reporting engine designed to ingest test execution events from any programming language or framework, reconstruct execution hierarchy, and provide live insights with historical tracking.

UTEM aims to solve the complexity, rigidity, and fragmentation in existing reporting tools by offering a universal, event-driven model that enables consistent reporting across Java, Python, JavaScript, and other ecosystems.

This project focuses on simplicity, observability, and extensibility while ensuring minimal integration effort for automation teams.

---

## Vision

UTEM establishes a standardized event model that allows test frameworks to communicate execution data in real time. By doing so, UTEM becomes a framework-agnostic reporting protocol rather than a tool tied to specific technologies.

The platform enables:

- Real-time execution monitoring
- Structured test hierarchy visualization
- Historical tracking and comparison
- Flakiness detection
- Failure analysis with attachments
- Self-hosted deployment for data control

---

## Key Features

### Real-Time Reporting
UTEM receives test events as they occur and updates execution status immediately. Users can observe progress, detect failures early, and respond faster.

### Framework and Language Neutral
Adapters can be built for any framework capable of emitting JSON events. UTEM does not enforce dependency on specific tools.

### Hierarchical Test Visualization
Supports full test structures such as:

- Suite
- Feature or Class
- Scenario or Test
- Steps

Hierarchy is reconstructed dynamically from incoming events.

### Step-Level Visibility
Each executed step is captured, timestamped, and displayed to provide clear execution traceability.

### Screenshot and Attachment Support
Users can attach screenshots or files at any step. Failure screenshots are automatically associated with failed nodes.

### Flakiness Detection
Tracks retries and failure patterns to help teams identify unstable tests.

### Execution History
Maintains a complete history of suite runs, enabling trend analysis and comparison.

### Resume Interrupted Runs
If execution is terminated, UTEM preserves the last known state and allows reruns for incomplete tests.

---

## Technology Stack

### Backend
- Java
- Spring Boot
- Spring Data JPA
- SQLite

### Frontend
- React
- Vite
- WebSockets for live updates

### Deployment
- Docker
- Standalone JAR

---

## Architecture

UTEM follows an event-driven architecture.

Test frameworks emit UTEM-compliant JSON events to the backend. The backend processes events, reconstructs hierarchy, stores state, and pushes live updates to the frontend.

High-Level Flow:

1. Test framework emits events
2. UTEM Core receives events via HTTP
3. Events are stored and processed
4. Execution state is updated
5. Frontend receives real-time updates
6. Historical data is retained for analysis

---

## Supported Frameworks

| Framework | Package | Registry |
|-----------|---------|----------|
| JUnit 5 + Cucumber | `io.github.shossain786:utem-reporter-junit5` | Maven Central |
| TestNG | `io.github.shossain786:utem-reporter-testng` | Maven Central |
| Playwright (JS/TS) | `utem-reporter-playwright` | npm |
| Jest | `utem-jest-reporter` | npm |
| pytest | `utem-pytest-reporter` | PyPI |

---

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven
- Node.js (for frontend)
- Docker (optional)

---

## Installation

### Option 1 — Docker (recommended, no build required)

Pull and run the pre-built image from Docker Hub:

```bash
docker pull sddmhossain/utem-core
docker run -d -p 8080:8080 --name utem-core sddmhossain/utem-core
```

Or with a persistent database volume so data survives container restarts:

```bash
docker run -d \
  -p 8080:8080 \
  -v utem-data:/app/data \
  --name utem-core \
  sddmhossain/utem-core
```

Dashboard will be available at **http://localhost:8080**

> If port 8080 is in use (e.g. Jenkins), map to a different host port:
> ```bash
> docker run -d -p 9090:8080 --name utem-core sddmhossain/utem-core
> ```
> Then access at **http://localhost:9090**

---

### Option 2 — Build from Source

```bash
git clone https://github.com/shossain786/utem-core.git
cd utem-core
mvn clean package -DskipTests
java -jar target/utem-core-*.jar
```

The server will start on port 8080.

---

## Configuration

Edit `application.yml` to configure:

- Database location
- Server port
- Logging level
- WebSocket settings

Default configuration uses SQLite for simplicity.

---

## API Overview

### Event Ingestion Endpoint

POST /utem/events

This endpoint accepts UTEM event payloads.

---

## UTEM Event Model

Each event must include:

- eventId
- runId
- timestamp
- eventType
- parentId (optional)
- metadata
- status (if applicable)
- duration (if applicable)

Example Event Types:

- TestRunStarted
- TestSuiteStarted
- TestCaseStarted
- TestStep
- Attachment
- TestPassed
- TestFailed
- TestRunFinished

---

## Data Storage

UTEM uses SQLite with the following logical structure:

- test_run
- test_node
- test_step
- attachments
- event_log

Event logs remain immutable. Execution state is derived from stored events.

---

## MVP Scope

The initial version includes:

- Real-time event ingestion
- Hierarchy reconstruction
- Live execution updates
- Screenshot support
- Run history storage
- Flakiness marking
- Search and filtering

---

## Out of Scope for MVP

- Cloud hosting
- Role-based access control
- AI-driven analytics
- Test management features
- Multi-tenant architecture

---

## Roadmap

Phase 1:
- Event ingestion
- Real-time visualization
- Execution history

Phase 2:
- Advanced insights
- Failure clustering
- Performance analysis

Phase 3:
- Adapter ecosystem
- Plugin support
- Enterprise integrations

---

## Contribution Guidelines

Contributions are welcome. Areas of interest include:

- Framework adapters
- UI improvements
- Performance optimizations
- Documentation

All contributions should follow project coding standards and include tests.

---

## License

This project will be released under an open-source license to encourage community adoption and collaboration.

---

## Design Philosophy

UTEM is built on three principles:

1. Simplicity over complexity
2. Observability over aesthetics
3. Universality over framework lock-in

---

## Support

Documentation and community support will expand as the project evolves.

---

## Closing Statement

UTEM is not merely a reporting tool. It is an evolving protocol designed to unify how test execution data is captured, observed, and understood. By standardizing event communication, UTEM enables teams to focus less on tooling friction and more on delivering reliable software.
