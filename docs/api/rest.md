# REST API

All endpoints are prefixed with `/utem`.

## Authentication

When `utem.security.enabled=true`, requests must include either:

- **JWT** (dashboard): `Authorization: Bearer <token>`
- **API Key** (reporters): `X-API-Key: utem_...`

## Auth Endpoints

### POST /utem/auth/login

```json
// Request
{ "username": "admin", "password": "changeme" }

// Response
{
  "token": "eyJ...",
  "userId": "uuid",
  "username": "admin",
  "role": "SUPER_ADMIN",
  "projectIds": null
}
```

### GET /utem/auth/me

Returns the currently authenticated user.

### POST /utem/auth/change-password

```json
{ "currentPassword": "old", "newPassword": "new" }
```

## Runs

### GET /utem/runs

Returns paginated list of active (non-archived) runs.

Query params: `page`, `size`, `status`, `name`, `label`

### GET /utem/runs/{runId}

Returns run summary.

### GET /utem/runs/{runId}/detail

Returns full run hierarchy (suites → test cases → steps).

### POST /utem/runs/{runId}/archive

Archive a run.

### GET /utem/runs/archived

Returns paginated archived runs.

## Projects

### GET /utem/projects

Returns all projects. Members see only their assigned projects.

### POST /utem/projects *(SUPER_ADMIN)*

```json
{ "name": "My Project", "description": "Optional" }
```

### POST /utem/projects/{id}/regenerate-key *(SUPER_ADMIN)*

Regenerates the API key.

### DELETE /utem/projects/{id} *(SUPER_ADMIN)*

Deactivates the project.

### GET /utem/projects/{id}/members *(SUPER_ADMIN)*

### POST /utem/projects/{id}/members *(SUPER_ADMIN)*

```json
{ "userId": "uuid", "role": "VIEWER" }
```

### DELETE /utem/projects/{id}/members/{userId} *(SUPER_ADMIN)*

## Users

### GET /utem/users *(SUPER_ADMIN)*

### POST /utem/users *(SUPER_ADMIN)*

```json
{ "username": "bob", "email": "bob@example.com", "password": "pass", "role": "MEMBER" }
```

### DELETE /utem/users/{userId} *(SUPER_ADMIN)*

Deactivates the user.

### POST /utem/users/{userId}/reactivate *(SUPER_ADMIN)*

### POST /utem/users/{userId}/reset-password *(SUPER_ADMIN)*

```json
{ "newPassword": "newpass" }
```

## Export

### GET /utem/export/{runId}/json

### GET /utem/export/{runId}/csv

### GET /utem/export/{runId}/junit-xml

## Analytics

### GET /utem/trends/pass-rate?limit=20

### GET /utem/trends/duration?limit=20

### GET /utem/trends/test-count?limit=20

### GET /utem/trends/flakiness?limit=20

### GET /utem/insights/summary

### GET /utem/performance/report

### GET /utem/flakiness/report

### GET /utem/failures/hotspots

### GET /utem/failures/clusters
