# UTEM Core — Backlog

## Reporter Gaps
- [x] pytest reporter (`utem-pytest-reporter`) — PyPI ✅ v0.1.0
- [x] TestNG reporter (`utem-reporter-testng`) — Maven Central ✅ v0.1.1
- [x] Playwright reporter (`utem-reporter-playwright`) — npm ✅ v0.1.5
- [x] Jest reporter (`utem-jest-reporter`) — npm ✅ v0.1.0
- [x] Cypress reporter (`utem-cypress-reporter`) — npm ✅ v0.1.0
- [x] Robot Framework listener (`utem-robot-reporter`) — PyPI ✅ v0.1.0

## Notifications & Alerts
- [x] Slack notifications — incoming webhook, Block Kit format ✅ v0.2.0
- [x] Teams notifications — incoming webhook, MessageCard format ✅ v0.2.0
- [x] Email notifications — HTML email via SMTP ✅ v0.2.0
- [x] Webhooks — generic HTTP POST JSON (Jenkins, custom CI) ✅ v0.2.0
- [x] Dashboard UI to manage channels (add/edit/delete/test/toggle) ✅ v0.2.0
- [x] Notify on flakiness threshold breached — alert all channels when tests exceed configured flakiness % ✅ v0.8.0

## Intelligence
- [x] Failure clustering — group similar stack traces, hotspot detection ✅ v0.2.0
- [x] AI failure analysis — rule-based diagnosis engine (30+ patterns: NPE, assertions, Selenium, networking, Cucumber etc.), diagnosis panel in StepDetailPanel ✅ v0.5.0
- [x] Flakiness scoring — per-test pass rate, flakiness badge, FlakinessPage ✅ v0.2.0

## CI/CD Integration
- [x] GitHub Actions / Jenkins plugin — post run summary as a PR comment with pass/fail badge ✅ v0.6.0
- [x] Quality gates — fail the build if flakiness score > threshold or new failures vs baseline ✅ v0.6.0

## User Experience
- [x] Run tagging / custom labels UI — inline edit in Runs list and Run detail header ✅ v0.3.0
- [x] Pinned runs — bookmark important runs (release candidates, baselines) ✅ v0.4.0
- [ ] Dark mode

## Multi-user / Teams
- [x] Basic auth / API key — X-API-Key header on write endpoints, enable via `utem.security.enabled=true` ✅ v0.8.0
- [x] Projects — namespace runs by project, per-project API keys, Projects page in dashboard ✅ v0.8.0
- [x] JWT authentication — login endpoint, Bearer token, 24h expiry, localStorage ✅ v0.9.0
- [x] Role-based access — SUPER_ADMIN (full access) / MEMBER (project-scoped) ✅ v0.9.0
- [x] User management — create/deactivate/reactivate users, reset passwords (SUPER_ADMIN) ✅ v0.9.0
- [x] Project membership — add/remove members with ADMIN/VIEWER roles ✅ v0.9.0
- [x] Frontend auth — LoginPage, ProtectedRoute, AuthContext, Users page, Sidebar user info ✅ v0.9.0
