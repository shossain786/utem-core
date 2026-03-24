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
    details: Drop-in reporter for JUnit 5 and Cucumber. One dependency, no code changes required.
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

Currently UTEM has first-class reporters for **JUnit 5** and **Cucumber** (JVM). The reporter is zero-dependency and works with any build tool (Maven, Gradle). Support for TestNG, Playwright, and pytest is on the roadmap.

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
