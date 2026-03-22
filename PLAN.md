# UTEM Core — Multi-User Authentication and Project Data Isolation

**Target version**: v0.9.0
**Status**: Complete ✅

---

## Overview

Adds JWT-based authentication, role-based access control (SUPER_ADMIN / MEMBER), and project-scoped data isolation to UTEM Core. The reporter API key flow is unchanged. All changes are gated behind `utem.security.enabled=true` — existing solo deployments continue to work without any configuration changes.

---

## Architecture

### Auth Flow
1. `POST /utem/auth/login` → returns JWT (24h default expiry)
2. JWT stored in `localStorage` as `utem_token`
3. All API requests send `Authorization: Bearer <token>`
4. `JwtAuthFilter` validates token, populates `UserContextHolder` (ThreadLocal)
5. `ApiKeyAuthFilter` handles reporter writes via `X-API-Key` (unchanged)
6. `utem.security.enabled=false` → both filters are no-ops

### Roles
| Role | Permissions |
|---|---|
| `SUPER_ADMIN` | Full access to all data, all users, all projects |
| `MEMBER` | Read access limited to assigned projects; no user management |

### Data Isolation
- All run list queries accept `List<String> allowedProjectIds`
  - `null` = SUPER_ADMIN (no filter applied)
  - empty list = no access (returns empty)
  - non-empty list = filter to those projects only
- Single-run access validated against user's project membership
- JPQL guard: `(:projectIds IS NULL OR r.projectId IN :projectIds)`

### Project Member Roles
| Role | Permissions |
|---|---|
| `ADMIN` | Manage project members, regenerate API key |
| `VIEWER` | Read access to project data only |

---

## Phase 1: Core Auth Infrastructure (Backend)

**Goal**: JWT login endpoint works, tokens issued and validated, filter chain updated. All existing behavior unchanged with `utem.security.enabled=false`.

### Step 1.1 — Maven dependencies (`pom.xml`)
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
    <!-- No version — managed by Spring Boot BOM -->
</dependency>
```
> **Gotcha**: Do NOT pin `spring-security-crypto` version — Spring Boot BOM manages it.
> **Gotcha**: jjwt 0.12.x uses `verifyWith()` not `setSigningKey()`, and `parseSignedClaims()` not `parseClaimsJws()`.

### Step 1.2 — `config/JwtProperties.java`
`@ConfigurationProperties(prefix = "utem.jwt")` record with `secret` and `expiryHours`.
Register in `UtemCoreApplication.java` via `@EnableConfigurationProperties`.

Add to `application.properties`:
```properties
utem.jwt.secret=dGhpcy1pcy1hLXNlY3VyZS1zZWNyZXQta2V5LWZvci11dGVtLWNvcmU=
utem.jwt.expiry-hours=24
utem.admin.username=admin
utem.admin.password=admin
```
> **Gotcha**: JWT secret must decode to >= 32 bytes (256 bits). The default above is 32 bytes.

### Step 1.3 — `entity/User.java`
Fields: `id` (UUID), `username` (unique, 50), `email` (unique, 100), `passwordHash`, `role` (SUPER_ADMIN | MEMBER), `active` (default true), `createdAt`.
> **Gotcha**: Use `@Table(name = "utem_user")` — `user` is a reserved word in SQLite.

### Step 1.4 — `entity/ProjectMember.java` + `entity/ProjectMemberId.java`
`ProjectMember`: `@EmbeddedId ProjectMemberId id`, `MemberRole role` (ADMIN | VIEWER), `createdAt`.
`ProjectMemberId`: `@Embeddable`, fields `userId` + `projectId`, implements `Serializable`.
> **Gotcha**: `@Data` on embeddable generates correct `equals`/`hashCode` — do not add `@EqualsAndHashCode(callSuper=false)`.

### Step 1.5 — `repository/UserRepository.java` + `repository/ProjectMemberRepository.java`
Key methods:
- `UserRepository`: `findByUsername`, `existsByUsername`, `existsByEmail`
- `ProjectMemberRepository`: `findProjectIdsByUserId` (`@Query` returning `List<String>`), `findByIdProjectId`, `deleteByIdUserIdAndIdProjectId`

### Step 1.6 — `config/SecurityBeansConfig.java`
`@Bean BCryptPasswordEncoder passwordEncoder()` — separate class to avoid circular dependency.

### Step 1.7 — `service/JwtService.java`
- Build `SecretKey` from: `Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()))`
- `generateToken(User, List<String> projectIds)` — claims: `sub`=userId, `username`, `role`, `projectIds`
- `validateToken(String token)` — returns `Claims`; throws `ExpiredJwtException` / `JwtException`

### Step 1.8 — `security/AuthenticatedUser.java`
Record (not JPA entity — avoids lazy-loading in filters):
```java
public record AuthenticatedUser(String userId, String username, User.Role role, List<String> projectIds) {
    public boolean isSuperAdmin() { return role == User.Role.SUPER_ADMIN; }
    public boolean canAccessProject(String projectId) {
        return isSuperAdmin() || projectIds.contains(projectId);
    }
}
```

### Step 1.9 — `security/UserContextHolder.java`
Same pattern as `ProjectContextHolder` — `ThreadLocal<AuthenticatedUser>` with `set`, `get`, `clear`.

### Step 1.10 — `exception/UnauthorizedException.java` + `exception/ForbiddenException.java`
Both extend `RuntimeException`. Add handlers to `GlobalExceptionHandler`:
- `UnauthorizedException` → HTTP 401
- `ForbiddenException` → HTTP 403

### Step 1.11 — `service/AdminInitService.java`
`implements ApplicationRunner` — on startup, if `securityEnabled=true` and no users exist, creates SUPER_ADMIN from `utem.admin.username` + `utem.admin.password`.

### Step 1.12 — `service/AuthService.java`
- `login(username, password)` → validates credentials, generates JWT, returns `LoginResponse`
- `changePassword(userId, currentPassword, newPassword)` → BCrypt verify + re-hash

### Step 1.13 — `controller/AuthController.java`
- `POST /utem/auth/login` — open, no auth needed
- `GET /utem/auth/me` — reads `UserContextHolder`
- `POST /utem/auth/change-password` — requires valid JWT

### Step 1.14 — `security/JwtAuthFilter.java` (`@Order(2)`)
1. Skip if `UserContextHolder` already set (API key reporter flow)
2. Extract `Authorization: Bearer <token>` header
3. Validate via `JwtService`; on `ExpiredJwtException` → 401 `{"error":"TokenExpired"}`; on other `JwtException` → 401 `{"error":"InvalidToken"}`
4. Set `UserContextHolder` from claims
5. `finally`: `UserContextHolder.clear()`

### Step 1.15 — Update `security/ApiKeyAuthFilter.java` (`@Order(1)`)
- Add `@Order(1)` annotation
- Change open-request logic: only `/utem/auth/**` is always open (remove blanket GET bypass)
- If `X-API-Key` present → resolve project, set `ProjectContextHolder`, allow through
- If neither API key nor JWT → return 401 (when `securityEnabled=true`)

### DTOs for Phase 1
- `dto/LoginRequest.java` — `record(@NotBlank String username, @NotBlank String password)`
- `dto/LoginResponse.java` — `record(String token, String userId, String username, User.Role role, List<String> projectIds)`
- `dto/UserDTO.java` — `record(String id, String username, User.Role role, List<String> projectIds)`
- `dto/ChangePasswordRequest.java` — `record(@NotBlank String currentPassword, @NotBlank String newPassword)`

---

## Phase 2: Data Isolation (Backend)

**Goal**: All run queries filtered by user's project membership. SUPER_ADMIN sees everything. Security bypass when `securityEnabled=false`.

### Step 2.1 — Add project-scoped queries to `TestRunRepository`
Add ~10 `@Query` variants with `(:projectIds IS NULL OR r.projectId IN :projectIds)` guard.
> **Gotcha**: Guard against empty `IN` list at service level — Hibernate behavior with empty list is undefined. Return `Page.empty()` before hitting DB.

### Step 2.2 — `service/RunQueryService.java`
Thin wrapper around `TestRunRepository`. Contract:
- `allowedProjectIds = null` → SUPER_ADMIN, no filter
- `allowedProjectIds = []` → no access, return empty immediately
- `allowedProjectIds = [...]` → filter to those projects

### Step 2.3 — Update `service/RunHistoryService.java`
All 14 list methods get `allowedProjectIds` parameter. Delegates to `RunQueryService` for list operations.

### Step 2.4 — Update `controller/RunHistoryController.java`
Add `resolveProjectIds()` helper:
```java
private List<String> resolveProjectIds() {
    if (!securityEnabled) return null;
    AuthenticatedUser user = UserContextHolder.get();
    if (user == null) throw new UnauthorizedException("Authentication required");
    return user.isSuperAdmin() ? null : user.projectIds();
}
```

### Step 2.5 — Update analytics services
`TrendAnalysisService`, `FailureInsightsService`, `PerformanceAnalysisService`, `FlakinessDetectionService` — each gets `allowedProjectIds` parameter on methods that query run lists. Their controllers call `resolveProjectIds()` and pass it in.

### Step 2.6 — Single-run access guard
In `RunHistoryService.getRunById()` (and `getRunDetail()`): after loading the run, verify `user.canAccessProject(run.getProjectId())` — throw `ForbiddenException` if not.

---

## Phase 3: User and Member Management (Backend)

**Goal**: SUPER_ADMIN manages users. Project ADMINs manage members.

### Step 3.1 — `service/UserService.java`
- `getAllUsers()`, `createUser(username, email, password, role)`, `deactivateUser(id)`, `reactivateUser(id)`, `resetPassword(id, newPassword)`

### Step 3.2 — `controller/UserController.java`
All endpoints under `/utem/users`, all guarded by `requireSuperAdmin()`:
- `GET /utem/users`
- `POST /utem/users`
- `DELETE /utem/users/{userId}`
- `POST /utem/users/{userId}/reset-password`

### Step 3.3 — `dto/ProjectMemberDTO.java`
`record(String userId, String username, String email, ProjectMember.MemberRole role, Instant createdAt)`

### Step 3.4 — Update `service/ProjectService.java`
Add: `getProjectMembers(projectId)`, `addMember(projectId, userId, role)`, `removeMember(projectId, userId)`.
Update `getAllProjects()`: MEMBER users see only their own projects; SUPER_ADMIN sees all.

### Step 3.5 — Update `controller/ProjectController.java`
Add member endpoints (Project ADMIN or SUPER_ADMIN):
- `GET /utem/projects/{id}/members`
- `POST /utem/projects/{id}/members`
- `DELETE /utem/projects/{id}/members/{userId}`

Lock down existing endpoints with auth guards when `securityEnabled=true`.

---

## Phase 4: Frontend Auth Integration

**Goal**: Login page, JWT in localStorage, protected routes, user management UI.

### Step 4.1 — `src/contexts/AuthContext.tsx`
- `AuthProvider` reads token + user from `localStorage` on init
- `login(username, password)` → POST to `/utem/auth/login`, store token + user
- `logout()` → clear localStorage + call `queryClient.clear()` to flush TanStack Query cache
- `useEffect` on mount: if token exists, call `GET /utem/auth/me`; if 401 → auto-logout
> **Gotcha**: `queryClient.clear()` on logout prevents stale data from showing after re-login.
> **Gotcha**: `AuthProvider` is above `BrowserRouter` — cannot use `useNavigate` inside it.

### Step 4.2 — Update `src/api/client.ts`
Add request interceptor: attach `Authorization: Bearer <token>` from `localStorage`.
Update response interceptor: on 401, clear `localStorage` and `window.location.href = '/login'` (with guard to not redirect when already on `/login`).
> **Gotcha**: `window.location.href` causes full reload — acceptable for security redirects.

### Step 4.3 — `src/pages/LoginPage.tsx`
Username + password form. On success, navigate to `/`. On failure, show error message. Auto-redirects to `/` if already authenticated.

### Step 4.4 — `src/components/ProtectedRoute.tsx`
Renders `<Outlet />` if authenticated; redirects to `/login` with `state.from` otherwise.

### Step 4.5 — Update `src/App.tsx`
New route structure:
```
<AuthProvider>
  <WebSocketProvider>
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            ... all existing routes ...
            <Route path="/users" element={<UsersPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  </WebSocketProvider>
</AuthProvider>
```
> **Gotcha**: `/login` must be a sibling of `ProtectedRoute`, NOT nested inside it.

### Step 4.6 — Update `src/components/layout/Sidebar.tsx`
- Add `{ to: '/users', label: 'Users', icon: '👥' }` nav item — only rendered when `isSuperAdmin`
- Replace version footer with user info: username, role, Logout button

### Step 4.7 — New hooks in `src/hooks/useApi.ts`
- `useUsers()`, `useCreateUser()`, `useDeactivateUser()`, `useResetPassword()`
- `useProjectMembers(projectId)`, `useAddProjectMember()`, `useRemoveProjectMember()`

### Step 4.8 — `src/pages/UsersPage.tsx`
Table of all users (SUPER_ADMIN only). Create user form. Deactivate/reactivate buttons. Reset password.

### Step 4.9 — Update `src/pages/ProjectsPage.tsx`
Add collapsible "Members" section per project card. Shows member list with role. Add/remove member controls.

### Step 4.10 — Update `src/api/types.ts`
Add `UserDTO` and `ProjectMember` interfaces.

---

## New Files Summary

### Backend (19 new files)
| File | Purpose |
|---|---|
| `entity/User.java` | User account entity (table: `utem_user`) |
| `entity/ProjectMember.java` | Project-user membership with role |
| `entity/ProjectMemberId.java` | Composite PK embeddable |
| `repository/UserRepository.java` | User data access |
| `repository/ProjectMemberRepository.java` | Membership data access |
| `config/JwtProperties.java` | JWT config properties |
| `config/SecurityBeansConfig.java` | BCryptPasswordEncoder bean |
| `dto/LoginRequest.java` | Login form record |
| `dto/LoginResponse.java` | Login response record |
| `dto/UserDTO.java` | User info record (no passwordHash) |
| `dto/ChangePasswordRequest.java` | Change password record |
| `dto/ProjectMemberDTO.java` | Member info with username/email |
| `service/JwtService.java` | JWT generate/validate (jjwt 0.12.x) |
| `service/AuthService.java` | Login + password change |
| `service/UserService.java` | User CRUD (SUPER_ADMIN only) |
| `service/AdminInitService.java` | Auto-creates first SUPER_ADMIN on startup |
| `service/RunQueryService.java` | Project-filtered run query wrapper |
| `security/JwtAuthFilter.java` | Bearer token validation filter (@Order 2) |
| `security/UserContextHolder.java` | ThreadLocal<AuthenticatedUser> |
| `security/AuthenticatedUser.java` | Record with role/project access helpers |
| `controller/AuthController.java` | /utem/auth/* endpoints |
| `controller/UserController.java` | /utem/users/* (SUPER_ADMIN only) |
| `exception/UnauthorizedException.java` | → HTTP 401 |
| `exception/ForbiddenException.java` | → HTTP 403 |

### Frontend (4 new files)
| File | Purpose |
|---|---|
| `src/contexts/AuthContext.tsx` | AuthProvider + useAuth hook |
| `src/pages/LoginPage.tsx` | Login form |
| `src/components/ProtectedRoute.tsx` | Auth guard component |
| `src/pages/UsersPage.tsx` | User management (SUPER_ADMIN) |

## Modified Files Summary

### Backend
- `pom.xml` — add jjwt + spring-security-crypto
- `application.properties` — add `utem.jwt.*`, `utem.admin.*`
- `UtemCoreApplication.java` — register `JwtProperties`
- `security/ApiKeyAuthFilter.java` — @Order(1), exempt `/utem/auth/`, remove GET bypass
- `exception/GlobalExceptionHandler.java` — add 401/403 handlers
- `controller/RunHistoryController.java` — `resolveProjectIds()` on all list endpoints
- `controller/ProjectController.java` — auth guards, member management endpoints
- `service/RunHistoryService.java` — all list methods accept `allowedProjectIds`
- `service/ProjectService.java` — member methods, filter `getAllProjects` by membership
- `service/TrendAnalysisService.java` — accept `allowedProjectIds`
- `service/FailureInsightsService.java` — accept `allowedProjectIds`
- `service/PerformanceAnalysisService.java` — accept `allowedProjectIds`
- `service/FlakinessDetectionService.java` — accept `allowedProjectIds`
- `repository/TestRunRepository.java` — add ~10 project-filtered query methods

### Frontend
- `src/api/client.ts` — request interceptor + 401 redirect
- `src/api/types.ts` — add `UserDTO`, `ProjectMember` interfaces
- `src/hooks/useApi.ts` — add user and member hooks
- `src/App.tsx` — `AuthProvider` wrap, `ProtectedRoute`, `/login` + `/users` routes
- `src/components/layout/Sidebar.tsx` — user info footer, conditional Users nav
- `src/pages/ProjectsPage.tsx` — member management section

---

## Key Constraints / Gotchas

1. **jjwt 0.12.x**: `verifyWith()` not `setSigningKey()`; `parseSignedClaims()` not `parseClaimsJws()`
2. **JWT secret**: must decode to >= 32 bytes (256 bits minimum for HMAC-SHA256)
3. **SQLite reserved word**: entity table must be `utem_user`, not `user`
4. **Filter ordering**: `@Order(1)` on `ApiKeyAuthFilter`, `@Order(2)` on `JwtAuthFilter`
5. **Empty IN clause**: guard at service level — return empty before hitting DB
6. **ThreadLocal cleanup**: both `UserContextHolder.clear()` and `ProjectContextHolder.clear()` in `finally` blocks
7. **BCryptPasswordEncoder**: declare as `@Bean` in separate config class, not `new` in service
8. **`utem.security.enabled=false`**: all filters are no-ops; all data visible (backwards compatible)
9. **401 redirect loop**: 401 interceptor must NOT redirect when already on `/login`
10. **queryClient.clear() on logout**: prevents stale data from prior session appearing after re-login
11. **Token expiry on reload**: `AuthProvider` calls `/utem/auth/me` on mount; if 401 → auto-logout
12. **WebSocket stays unauthenticated**: STOMP `/ws` endpoint remains open at transport level for MVP
13. **`useNavigate` not available in `AuthContext`**: use `window.location.href` for security redirects
14. **`compareRuns` access**: both run IDs must be accessible to the user — `getRunById()` access check covers this automatically

---

## Implementation Order

```
Phase 1 — Core Auth (Backend)
  [x] 1.1  pom.xml dependencies
  [x] 1.2  JwtProperties + application.properties
  [x] 1.3  User entity
  [x] 1.4  ProjectMember entity + ProjectMemberId
  [x] 1.5  UserRepository + ProjectMemberRepository
  [x] 1.6  SecurityBeansConfig (BCryptPasswordEncoder)
  [x] 1.7  JwtService
  [x] 1.8  AuthenticatedUser record
  [x] 1.9  UserContextHolder
  [x] 1.10 UnauthorizedException + ForbiddenException
  [x] 1.11 GlobalExceptionHandler — 401/403 handlers
  [x] 1.12 AdminInitService
  [x] 1.13 AuthService
  [x] 1.14 AuthController + DTOs
  [x] 1.15 JwtAuthFilter (@Order 2)
  [x] 1.16 Update ApiKeyAuthFilter (@Order 1)
  ✓  TEST: 267 tests pass. Login endpoint works. Existing behavior unchanged with security disabled.

Phase 2 — Data Isolation (Backend)
  [x] 2.1  TestRunRepository — project-filtered queries
  [x] 2.2  RunQueryService
  [x] 2.3  RunHistoryService — allowedProjectIds overloads
  [x] 2.4  RunHistoryController — resolveProjectIds() helper
  [ ] 2.5  TrendAnalysisService, FailureInsightsService, PerformanceAnalysisService, FlakinessDetectionService
  [ ] 2.6  Single-run access guard in getRunById/getRunDetail
  ✓  TEST: Run list queries project-filtered for MEMBER users. SUPER_ADMIN sees all.

Phase 3 — User & Member Management (Backend)
  [x] 3.1  UserService
  [x] 3.2  UserController
  [x] 3.3  ProjectMemberDTO
  [x] 3.4  ProjectService — member methods + getAllProjects filter
  [x] 3.5  ProjectController — member endpoints + auth guards
  ✓  TEST: SUPER_ADMIN creates users and assigns to projects. Project ADMIN manages members.

Phase 4 — Frontend
  [x] 4.1  AuthContext.tsx
  [x] 4.2  client.ts — interceptors
  [x] 4.3  LoginPage.tsx
  [x] 4.4  ProtectedRoute.tsx
  [x] 4.5  App.tsx — route restructure
  [x] 4.6  Sidebar.tsx — user info + logout
  [x] 4.7  useApi.ts — user/member hooks
  [x] 4.8  UsersPage.tsx
  [ ] 4.9  ProjectsPage.tsx — member section
  [x] 4.10 types.ts — UserDTO + ProjectMember
  ✓  TEST: Frontend builds. Login form, ProtectedRoute, Users page, Sidebar user info all complete.
```
