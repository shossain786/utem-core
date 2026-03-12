# UTEM Core — Universal Test Execution Monitor

Real-time, self-hosted test reporting dashboard. Stream live test results from any framework, view failure screenshots, analyze trends, compare runs, and export reports.

## Quick Start

```bash
docker run -d \
  --name utem-core \
  -p 9090:8080 \
  -v utem-data:/app/data \
  sddmhossain/utem-core:latest
```

Open the dashboard at **http://localhost:9090**

## Using Docker Compose

```yaml
services:
  utem-core:
    image: sddmhossain/utem-core:latest
    container_name: utem-core
    ports:
      - "9090:8080"
    volumes:
      - utem-data:/app/data
    environment:
      - SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/utem.db
    restart: unless-stopped

volumes:
  utem-data:
    driver: local
```

```bash
docker compose up -d
```

## Configuration

| Environment Variable | Default | Description |
|----------------------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:sqlite:/app/data/utem.db` | SQLite database path |
| `SERVER_PORT` | `8080` | Internal port (map via `-p`) |

## Supported Frameworks

| Framework | Package | Registry |
|-----------|---------|----------|
| JUnit 5 + Cucumber | `io.github.shossain786:utem-reporter-junit5` | Maven Central |
| TestNG | `io.github.shossain786:utem-reporter-testng` | Maven Central |
| Playwright (JS/TS) | `utem-reporter-playwright` | npm |
| pytest | `utem-pytest-reporter` | PyPI |
| Jest | `utem-jest-reporter` | npm *(coming soon)* |

## Connect Your Tests

**JUnit 5 / Cucumber (Maven)**
```xml
<dependency>
    <groupId>io.github.shossain786</groupId>
    <artifactId>utem-reporter-junit5</artifactId>
    <version>0.1.2</version>
    <scope>test</scope>
</dependency>
```
```bash
mvn test -Dutem.server.url=http://localhost:9090/utem
```

**TestNG (Maven)**
```xml
<dependency>
    <groupId>io.github.shossain786</groupId>
    <artifactId>utem-reporter-testng</artifactId>
    <version>0.1.1</version>
    <scope>test</scope>
</dependency>
```

**Playwright**
```bash
npm install --save-dev utem-reporter-playwright
UTEM_SERVER_URL=http://localhost:9090/utem npx playwright test
```

**pytest**
```bash
pip install utem-pytest-reporter
UTEM_SERVER_URL=http://localhost:9090/utem pytest
```

## Features

- Live test run progress via WebSocket
- JUnit 5, Cucumber, TestNG, Playwright, pytest support
- Failure screenshot capture (Selenium / Playwright)
- Run comparison and trend analysis
- Flakiness detection
- Run labels with inline dashboard editing
- Slack, Teams, Email and Webhook notifications
- Export as JSON, CSV, JUnit XML
- Job grouping and archive

## Source & Docs

[github.com/shossain786/utem-core](https://github.com/shossain786/utem-core)
