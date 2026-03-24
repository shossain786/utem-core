# Authentication

UTEM supports multi-user authentication with role-based project isolation. It is **opt-in** — disabled by default for single-user setups.

## Enabling Auth

Set in `application.properties`:

```properties
utem.security.enabled=true
utem.admin.username=admin
utem.admin.password=changeme
```

Restart the server. On first startup, the `admin` super-admin account is created automatically.

Login at `http://localhost:8080/login`.

## Roles

| Role | Description |
|---|---|
| `SUPER_ADMIN` | Full access — manage users, projects, all runs |
| `MEMBER` | Access only to assigned projects |

## User Management

As SUPER_ADMIN, go to **Users** to:

- Create new users (username, email, password, role)
- Deactivate / reactivate accounts
- Reset passwords

## Project Isolation

Each **Project** has an API key used by reporters. Members can only see runs from their assigned projects.

1. Go to **Projects** → create a project
2. Click **Members** on the project card
3. Add a user with role `VIEWER` or `ADMIN`

The user now sees only that project's runs, trends, and analytics.

## API Keys vs JWT

| | API Key | JWT |
|---|---|---|
| Used by | Reporters (CI pipeline) | Dashboard users (browser) |
| Format | `X-API-Key: utem_...` header | `Authorization: Bearer ...` header |
| Scope | Single project | User's assigned projects |

Reporters always use the API key — they are not affected by enabling auth.

## Changing Password

Logged-in users can change their password via the user menu in the sidebar.

## Security Notes

- Change the default `admin` password immediately in production
- Set a strong `utem.jwt.secret` (base64-encoded, at least 32 bytes)
- HTTPS is recommended for production deployments (use a reverse proxy like nginx)
