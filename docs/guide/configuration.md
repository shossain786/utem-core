# Configuration

UTEM is configured via `application.properties`. Place this file in the same directory as the JAR to override defaults.

## Security

```properties
# Enable JWT authentication (default: false)
utem.security.enabled=true

# Initial super-admin credentials (used only on first startup)
utem.admin.username=admin
utem.admin.password=changeme

# JWT secret — must decode to >= 32 bytes. Change in production!
utem.jwt.secret=dGhpcy1pcy1hLXNlY3VyZS1zZWNyZXQta2V5LWZvci11dGVtLWNvcmU=
utem.jwt.expiry-hours=24
```

## Database

```properties
# SQLite database file path
spring.datasource.url=jdbc:sqlite:utem.db

# Docker: use /app/data/utem.db for persistent volume
spring.datasource.url=jdbc:sqlite:/app/data/utem.db
```

## Storage

```properties
# Directory for screenshot attachments
utem.storage.base-path=./utem-attachments
utem.storage.max-file-size-mb=50
utem.storage.organize-by-run-id=true
```

## Data Retention

```properties
# Automatically delete runs older than retention-days
utem.retention.enabled=true
utem.retention.retention-days=30

# Cron: runs at 2am daily by default
utem.retention.cron-expression=0 0 2 * * *
```

## Stale Run Recovery

```properties
# Mark RUNNING runs as ABORTED if stuck longer than threshold
utem.recovery.enabled=true
utem.recovery.stale-threshold-minutes=60
utem.recovery.check-interval-ms=300000
```

## Notifications

```properties
# Microsoft Teams
utem.notification.teams.enabled=true
utem.notification.teams.webhook-url=https://...

# Jenkins Webhook
utem.notification.jenkins.enabled=true
utem.notification.jenkins.url=https://...

# Email
utem.notification.email.enabled=true
utem.notification.email.to=team@example.com
utem.notification.email.from=utem@example.com

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=sender@gmail.com
spring.mail.password=app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

## Environment Variables

All properties can be overridden via environment variables by replacing `.` with `_` and uppercasing:

| Property | Environment Variable |
|---|---|
| `utem.security.enabled` | `UTEM_SECURITY_ENABLED` |
| `utem.admin.password` | `UTEM_ADMIN_PASSWORD` |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` |
