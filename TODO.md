# UTEM Core — Backlog

## Reporter Gaps
- [x] pytest reporter (`utem-pytest-reporter`) — PyPI ✅ v0.1.0
- [x] TestNG reporter (`utem-reporter-testng`) — Maven Central ✅ v0.1.1
- [x] Playwright reporter (`utem-reporter-playwright`) — npm ✅ v0.1.5
- [ ] Jest reporter (`utem-jest-reporter`) — npm
- [ ] Cypress reporter
- [ ] Robot Framework listener

## Notifications & Alerts
- [x] Slack notifications — incoming webhook, Block Kit format ✅ v0.2.0
- [x] Teams notifications — incoming webhook, MessageCard format ✅ v0.2.0
- [x] Email notifications — HTML email via SMTP ✅ v0.2.0
- [x] Webhooks — generic HTTP POST JSON (Jenkins, custom CI) ✅ v0.2.0
- [x] Dashboard UI to manage channels (add/edit/delete/test/toggle) ✅ v0.2.0
- [ ] Notify on flakiness threshold breached

## Intelligence
- [ ] Failure clustering — group similar stack traces ("15 tests failed for the same root cause")
- [ ] AI failure analysis — send failure + stack trace to Claude API, get plain-English diagnosis and suggested fix
- [ ] Flakiness scoring — per-test pass rate over last N runs with a flakiness badge

## CI/CD Integration
- [ ] GitHub Actions / Jenkins plugin — post run summary as a PR comment with pass/fail badge
- [ ] Quality gates — fail the build if flakiness score > threshold or new failures vs baseline

## User Experience
- [ ] Pinned runs — bookmark important runs (release candidates, baselines)
- [ ] Run tagging / custom labels UI — set labels directly in the dashboard, not just via CLI
- [ ] Dark mode

## Multi-user / Teams
- [ ] Basic auth / API key — protect the server from unauthorized writes
- [ ] Projects — namespace runs by project so one UTEM server can serve multiple teams
