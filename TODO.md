# UTEM Core — Backlog

## Reporter Gaps
- [ ] Jest reporter (`utem-jest-reporter`) — npm
- [ ] Cypress reporter
- [ ] Robot Framework listener

## Notifications & Alerts
- [ ] Slack / Teams / Email — alert on run completion, new failures, or flakiness threshold breached
- [ ] Webhooks — POST to any URL when a run finishes (CI/CD integration)

## Intelligence
- [ ] Failure clustering — group similar stack traces ("15 tests failed for the same root cause")
- [ ] AI failure analysis — send failure + stack trace to Claude API, get plain-English diagnosis and suggested fix
- [ ] Flakiness scoring — per-test pass rate over last N runs with a flakiness badge

## CI/CD Integration
- [ ] GitHub Actions / Jenkins plugin — post run summary as a PR comment with pass/fail badge
- [ ] Quality gates — fail the build if flakiness score > threshold or new failures vs baseline

## User Experience
- [ ] Test search — search by test name across all historical runs
- [ ] Pinned runs — bookmark important runs (release candidates, baselines)
- [ ] Run tagging / custom labels UI — set labels directly in the dashboard, not just via CLI
- [ ] Dark mode

## Multi-user / Teams
- [ ] Basic auth / API key — protect the server from unauthorized writes
- [ ] Projects — namespace runs by project so one UTEM server can serve multiple teams
