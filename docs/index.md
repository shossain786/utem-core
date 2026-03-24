---
layout: home

hero:
  name: "UTEM"
  text: "Universal Test Execution Monitor"
  tagline: Real-time test reporting dashboard for JUnit 5, Cucumber, TestNG, Playwright & pytest. Self-hosted.
  image:
    src: /hero-image.svg
    alt: UTEM Dashboard
  actions:
    - theme: brand
      text: Get Started
      link: /guide/getting-started
    - theme: alt
      text: View on GitHub
      link: https://github.com/shossain786/utem-core

features:
  - icon: ⚡
    title: Real-time Dashboard
    details: Live test results streamed via WebSocket. Watch tests pass and fail as they run.
  - icon: 🔌
    title: Zero Config Reporters
    details: Drop-in reporters for JUnit 5, TestNG, Cucumber, Jest, Cypress, Playwright, pytest, and Robot Framework. One dependency, no code changes required.
  - icon: 📊
    title: Analytics & Trends
    details: Pass rate trends, flakiness detection, performance analysis, and failure clustering.
  - icon: 🔐
    title: Multi-user Auth
    details: JWT-based login with role-based access. Project isolation — members only see their runs.
  - icon: 🐳
    title: Docker Ready
    details: Single container, SQLite persistence via volume. Run anywhere in minutes.
  - icon: 📤
    title: Export & Compare
    details: Export runs as JSON, CSV or JUnit XML. Side-by-side run comparison with regression detection.
---

<!-- ── Stats bar ─────────────────────────────────────────────────── -->
<div class="stats-bar">
  <div class="stat-item">
    <span class="stat-number">8</span>
    <span class="stat-label">Reporters</span>
  </div>
  <div class="stat-divider"></div>
  <div class="stat-item">
    <span class="stat-number">5</span>
    <span class="stat-label">Languages</span>
  </div>
  <div class="stat-divider"></div>
  <div class="stat-item">
    <span class="stat-number">Real-time</span>
    <span class="stat-label">WebSocket</span>
  </div>
  <div class="stat-divider"></div>
  <div class="stat-item">
    <span class="stat-number">Self-hosted</span>
    <span class="stat-label">Your data, your server</span>
  </div>
  <div class="stat-divider"></div>
  <div class="stat-item">
    <span class="stat-number">MIT</span>
    <span class="stat-label">Open Source</span>
  </div>
</div>

<!-- ── Works with ────────────────────────────────────────────────── -->
<div class="works-with">
  <p class="works-with-label">Works with</p>
  <div class="framework-logos">
    <a href="/utem-core/reporters/junit5" class="framework-badge java">JUnit 5</a>
    <a href="/utem-core/reporters/testng" class="framework-badge java">TestNG</a>
    <a href="/utem-core/reporters/cucumber" class="framework-badge java">Cucumber</a>
    <a href="/utem-core/reporters/jest" class="framework-badge js">Jest</a>
    <a href="/utem-core/reporters/cypress" class="framework-badge js">Cypress</a>
    <a href="/utem-core/reporters/playwright" class="framework-badge js">Playwright</a>
    <a href="/utem-core/reporters/pytest" class="framework-badge python">pytest</a>
    <a href="/utem-core/reporters/robot" class="framework-badge python">Robot Framework</a>
  </div>
</div>

<!-- ── How it works ──────────────────────────────────────────────── -->
<div class="how-it-works">
  <h2 class="section-title">How it works</h2>
  <p class="section-subtitle">Up and running in under 5 minutes</p>
  <div class="steps">
    <div class="step">
      <div class="step-number">1</div>
      <div class="step-content">
        <h3>Start the server</h3>
        <p>Run the JAR or Docker container. The dashboard is immediately available at <code>localhost:8080</code>.</p>
        <div class="step-code">java -jar utem-core-0.9.1.jar</div>
      </div>
    </div>
    <div class="step-arrow">→</div>
    <div class="step">
      <div class="step-number">2</div>
      <div class="step-content">
        <h3>Add the reporter</h3>
        <p>One dependency in your test project. Configure the server URL and API key — no code changes needed.</p>
        <div class="step-code">utem.server.url=http://localhost:8080</div>
      </div>
    </div>
    <div class="step-arrow">→</div>
    <div class="step">
      <div class="step-number">3</div>
      <div class="step-content">
        <h3>Run your tests</h3>
        <p>Results appear in the dashboard live. Trends, analytics, and flakiness detection update automatically.</p>
        <div class="step-code">mvn test / npx playwright test</div>
      </div>
    </div>
  </div>
</div>

<!-- ── FAQ ───────────────────────────────────────────────────────── -->
<div class="faq-section">

## Frequently Asked Questions

<details class="faq-item">
<summary>Do I need a database to run UTEM?</summary>

No. UTEM uses embedded **SQLite** — no database server required. Data is stored in a single file (`utem.db`) in the current directory. For Docker, mount a volume at `/app/data` to persist it across restarts.

</details>

<details class="faq-item">
<summary>Is authentication required?</summary>

No. Authentication is **opt-in** and disabled by default (`utem.security.enabled=false`). This makes it easy to get started in a single-user or internal network setup. Enable it when you need multi-user access or want to restrict who can view test results.

</details>

<details class="faq-item">
<summary>Which test frameworks are supported?</summary>

UTEM has first-class reporters for:
- **[JUnit 5](/reporters/junit5)** — Java, zero dependencies, Maven/Gradle
- **[Cucumber](/reporters/cucumber)** — JVM (Java/Kotlin), step-level reporting + screenshots
- **[Jest](/reporters/jest)** — JavaScript/TypeScript, zero dependencies, Node.js ≥ 18
- **[Cypress](/reporters/cypress)** — E2E testing, spec-level reporting, Node.js ≥ 18
- **[Playwright](/reporters/playwright)** — E2E testing, screenshot forwarding, Node.js ≥ 18
- **[pytest](/reporters/pytest)** — Python, auto-discovered, Python 3.8+
- **[Robot Framework](/reporters/robot)** — Python, listener v3, RF 4+

Support for NUnit, xUnit, and Vitest is on the roadmap.

</details>

<details class="faq-item">
<summary>Can I use UTEM in CI/CD pipelines?</summary>

Yes. The reporter sends events over HTTP — just set `UTEM_SERVER_URL` and `UTEM_API_KEY` as environment variables in your CI pipeline. Works with GitHub Actions, Jenkins, GitLab CI, and any other CI system.

</details>

<details class="faq-item">
<summary>How do screenshots work?</summary>

Register your Selenium `WebDriver` with `WebDriverRegistry.register(driver)` in your test setup. UTEM automatically captures a screenshot on test failure and attaches it to the run in the dashboard. No extra code needed in your test methods.

</details>

<details class="faq-item">
<summary>Can multiple teams share one UTEM server?</summary>

Yes. Create a **Project** for each team — each gets its own API key. Enable authentication, create user accounts, and assign users to projects. Members only see their team's runs and analytics.

</details>

<details class="faq-item">
<summary>How long is test data retained?</summary>

By default, runs older than **30 days** are automatically deleted at 2am daily. You can configure this with `utem.retention.retention-days` and `utem.retention.cron-expression`, or disable it entirely with `utem.retention.enabled=false`.

</details>

<details class="faq-item">
<summary>Is UTEM free and open source?</summary>

Yes. UTEM is released under the **MIT License** — free to use, modify, and self-host. Source code is available on [GitHub](https://github.com/shossain786/utem-core).

</details>

</div>
