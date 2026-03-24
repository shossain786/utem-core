# Getting Started

UTEM (Universal Test Execution Monitor) is a self-hosted, real-time test reporting dashboard. It receives test events from your CI pipeline and displays live results, trends, and analytics in a web UI.

## How It Works

```
Your tests  →  UTEM Reporter  →  UTEM Server  →  Dashboard
(JUnit/Cucumber)   (sends HTTP events)   (stores + streams)   (React UI)
```

1. **UTEM Server** runs as a Spring Boot app (JAR or Docker)
2. **UTEM Reporter** is a test listener added to your project
3. Reporter sends events (test started, passed, failed) to the server via HTTP
4. Dashboard shows live results via WebSocket

## Quick Start

### 1. Start the server

```bash
java -jar utem-core-0.9.1.jar
```

Open [http://localhost:8080](http://localhost:8080) — the dashboard is ready.

### 2. Create a project

Go to **Projects** → **+ New Project**. Copy the generated API key.

### 3. Add the reporter

Add to your `pom.xml`:

```xml
<dependency>
  <groupId>com.utem</groupId>
  <artifactId>utem-reporter-junit5</artifactId>
  <version>0.9.1</version>
  <scope>test</scope>
</dependency>
```

Configure in `src/test/resources/utem.properties`:

```properties
utem.server.url=http://localhost:8080
utem.api.key=utem_your_api_key_here
utem.run.name=My Test Suite
```

### 4. Run your tests

```bash
mvn test
```

Your test run appears live in the dashboard.

## Next Steps

- [Installation options](./installation) — JAR, Docker, start.bat
- [Configuration reference](./configuration) — all properties
- [Enable authentication](./auth) — multi-user setup
- [JUnit 5 reporter](../reporters/junit5) — detailed setup
- [Cucumber reporter](../reporters/cucumber) — Cucumber integration
- [Jest reporter](../reporters/jest) — JavaScript/TypeScript projects
- [Cypress reporter](../reporters/cypress) — E2E testing
