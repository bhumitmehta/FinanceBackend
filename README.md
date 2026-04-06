# Finance Backend

A production-ready RESTful API for managing personal and organizational financial records, built with Spring Boot 3, Spring Security (JWT), JPA/Hibernate, and PostgreSQL.

---

## Table of Contents

1. [Setup](#setup)
2. [API Reference](#api-reference)
3. [Architecture & Design Patterns](#architecture--design-patterns)
4. [Assumptions & Tradeoffs](#assumptions--tradeoffs)

---

## Setup

### Prerequisites

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 17+ | Runtime |
| Maven | 3.9+ (or use `./mvnw`) | Build tool |
| Docker | 24+ | PostgreSQL container |

### 1 — Start the database

```bash
docker run -d \
  --name financedb \
  -e POSTGRES_DB=financedb \
  -e POSTGRES_USER=finance \
  -e POSTGRES_PASSWORD=finance123 \
  -p 5432:5432 \
  postgres:16-alpine
```

### 2 — Run the application

```bash
# Clone / navigate to project root
cd "Finance Backend"

# Build and start (skip tests for a fast first boot)
./mvnw spring-boot:run

# Or build a fat-jar first
./mvnw clean package -DskipTests
java -jar target/FinanceBackend-0.0.1-SNAPSHOT.jar
```

The server starts on **http://localhost:8080**.

### 3 — Open Swagger UI

Navigate to **http://localhost:8080/swagger-ui.html** (or `/swagger-ui/index.html`).

Click **Authorize** (top-right padlock), paste a JWT token from the login step, then explore every endpoint interactively.

### 4 — Default seeded credentials

`DataSeeder` runs automatically on the **first** boot when the database is empty and creates:

| Role | Email | Password |
|------|-------|----------|
| ADMIN | `admin@finance.local` | `Admin1234!` |
| ANALYST | `analyst@finance.local` | `Analyst1234!` |
| VIEWER | `viewer@finance.local` | `Viewer1234!` |

30 sample financial records across multiple categories (Salary, Freelance, Rent, Groceries, etc.) are also seeded.

**Get a token:**
```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@finance.local","password":"Admin1234!"}' \
  | jq .token
```

### 5 — Import the Postman collection

The file `postman/collection.json` contains a ready-to-use Postman collection with example requests and expected responses for every endpoint.

1. Open Postman → **Import** → select `postman/collection.json`.
2. Run `Login — admin` to auto-populate the `adminToken` variable.
3. All other requests use `Authorization: Bearer {{adminToken}}` automatically.

---

### 6 — End-to-end RBAC scenarios

These scenarios verify that permission management works at runtime with zero code changes.

**Scenario 1 — Runtime role creation**

```bash
# 1. Create a custom permission
POST /api/permissions
{ "name": "invoices:approve", "resource": "invoices", "action": "approve", "description": "Approve invoices" }

# 2. Create a role with that permission
POST /api/roles
{ "name": "approver", "description": "Invoice approvers", "permissionIds": ["<permission-id>"] }

# 3. Assign the new role to a user
POST /api/users/<user-id>/roles/<role-id>

# 4. Login as that user — JWT now grants PERM_invoices:approve
# Add @PreAuthorize("hasAuthority('PERM_invoices:approve')") to any endpoint
# → user can access it immediately, no redeploy
```

**Scenario 2 — Permission upgrade (no code change)**

```bash
# VIEWER user cannot POST /api/records — 403 Forbidden

# Add records:write permission to the viewer role at runtime:
POST /api/roles/<viewer-role-id>/permissions/<records-write-permission-id>

# Login again as viewer user — new token now carries PERM_records:write
# → POST /api/records now returns 201 Created
# → No code was changed. No redeploy happened.
```

**Scenario 3 — Multi-role user**

```bash
# Assign both viewer and analyst roles to one user
POST /api/users/<user-id>/roles/<viewer-role-id>
POST /api/users/<user-id>/roles/<analyst-role-id>

# Get effective permissions — returns deduplicated union
GET /api/users/<user-id>/permissions
# → [records:read, dashboard:view, records:history, dashboard:export]
# records:read and dashboard:view appear once despite being in both roles
```

**Scenario 4 — Revoke access**

```bash
# User currently has analyst role → can access GET /api/dashboard/... (dashboard:export)

# Revoke analyst role
DELETE /api/users/<user-id>/roles/<analyst-role-id>

# User's next request (with the same token) to GET /api/dashboard/summary
# → 403 Forbidden immediately — no code change, no redeploy
# Note: existing JWT is still valid for non-protected endpoints;
#       re-login required to get a token with updated authorities
```

### Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | `veryLongRandomSecretKeyThatIsHardToGuess123456789` | HMAC-SHA signing key — **override in production** |
| `JWT_EXPIRATION` | `3600000` (1 h) | Token lifetime in milliseconds |

---

## API Reference

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| POST | `/api/auth/register` | Public | Register — returns JWT |
| POST | `/api/auth/login` | Public | Login — returns JWT |
| GET | `/api/records` | Any auth | List records (paginated, filterable) |
| POST | `/api/records` | `records:write` | Create record |
| GET | `/api/records/{id}` | Any auth | Get record by ID |
| PUT | `/api/records/{id}` | `records:write` | Update record |
| DELETE | `/api/records/{id}` | `records:delete` | Soft-delete record |
| GET | `/api/records/{id}/history` | `records:history` | Envers revision history for a record |
| GET | `/api/dashboard/summary` | `dashboard:view` | Full summary (parallel fetch) |
| GET | `/api/dashboard/by-category` | `dashboard:view` | Category totals |
| GET | `/api/dashboard/trends` | `dashboard:view` | Monthly income/expense trends |
| GET | `/api/dashboard/recent` | `dashboard:view` | Recent activity |
| GET | `/api/users` | `users:manage` | List all users |
| POST | `/api/users` | `users:manage` | Create user |
| POST | `/api/users/{id}/roles/{roleId}` | `users:manage` | Assign role to user |
| DELETE | `/api/users/{id}/roles/{roleId}` | `users:manage` | Revoke role from user |
| GET | `/api/users/{id}/permissions` | `users:manage` | Effective permission list (flattened union) |
| PATCH | `/api/users/{id}/status` | `users:manage` | Toggle user status |
| GET | `/api/users/{id}/role-history` | `users:manage` | Role assignment/revocation audit log |
| GET | `/api/permissions` | `users:manage` | List all permissions |
| GET | `/api/permissions/{id}` | `users:manage` | Get permission by ID |
| POST | `/api/permissions` | `users:manage` | Create permission |
| DELETE | `/api/permissions/{id}` | `users:manage` | Delete permission (guard: not in use) |
| GET | `/api/roles` | `users:manage` | List all roles with permissions |
| GET | `/api/roles/{id}` | `users:manage` | Get role with permissions |
| POST | `/api/roles` | `users:manage` | Create role with permission set |
| POST | `/api/roles/{id}/permissions/{permId}` | `users:manage` | Add permission to role at runtime |
| DELETE | `/api/roles/{id}/permissions/{permId}` | `users:manage` | Remove permission from role |
| DELETE | `/api/roles/{id}` | `users:manage` | Delete role (guard: not in use) |

### Pagination

`GET /api/records` accepts standard Spring `Pageable` query params:

```
?page=0&size=20&sort=date,desc&type=EXPENSE&category=Rent&startDate=2026-01-01&endDate=2026-04-05
```

Response shape:
```json
{
  "content": [ { "id": "...", "amount": 1200.00, "type": "EXPENSE", ... } ],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

### Error response shape

All errors return a consistent JSON body:

```json
{
  "error": "Not Found",
  "message": "Record not found: 550e8400-...",
  "statusCode": 404
}
```

Validation errors additionally include a `fields` map:

```json
{
  "error": "Validation failed",
  "message": "One or more fields have invalid values",
  "statusCode": 400,
  "fields": {
    "amount": "Amount must be positive",
    "type": "Type is required"
  }
}
```

---

## Architecture & Design Patterns

### 1 — DB-driven RBAC (Level 2 — Runtime Permission Management)

**RBAC maturity ladder:**

| Level | Description | Redeploy needed? |
|-------|-------------|------------------|
| 0 | Hardcoded `if (user.isAdmin())` checks in code | Yes — every change |
| 1 | Role enum (`ADMIN`, `ANALYST`, `VIEWER`) stored in DB, checked in code via `hasRole()` | Yes — adding a role requires code |
| **2** | **Permission strings in DB, roles are collections of permissions, runtime API to manage both** | **No — add/remove permissions and roles via API** |
| 3 | Attribute-based access control (ABAC) — conditions on resource fields (`owner == currentUser`) | No |
| 4 | Policy engine (OPA, Casbin) — external policy files, hot-reload | No |
| 5 | Zero-trust — every request carries a signed capability token | No |

**This implementation is Level 2.** Roles and permissions live in the `roles`, `permissions`, and `role_permissions` tables. An admin can:
- Create a new permission (`POST /api/permissions`)
- Create a new role with that permission (`POST /api/roles`)
- Assign the role to a user (`POST /api/users/{id}/roles/{roleId}`)
- The user's next login will receive the updated authorities — **zero code change, zero redeploy.**

**How authority resolution works:**
1. `CustomUserDetailsService.loadUserByUsername()` calls `findWithRolesAndPermissionsByEmail()` — an `@EntityGraph` query that joins `users → user_roles → roles → role_permissions → permissions` in **one SQL statement**.
2. `CustomUserDetails.getAuthorities()` iterates every role the user holds, flattens all their permissions into a `Set<GrantedAuthority>`, and prefixes each name with `PERM_` (e.g. `PERM_records:write`).
3. Spring Security's `@PreAuthorize("hasAuthority('PERM_records:write')")` checks this set — no custom voter needed.

---

### 2 — Composite (Role = collection of Permissions, User = collection of Roles)

**Where:** `model/Role`, `model/User`, `model/UserRole`, `service/UserService.getUserPermissions()`

**Why:** A user's effective access is the *union* of all permissions across all roles they hold. Rather than duplicating permission logic per role, the system treats each level as a composite:
- A `Permission` is a leaf node (single capability string).
- A `Role` is a composite of `Permission` objects.
- A `User` is a composite of `Role` objects.
- `getUserPermissions()` is a single traversal that flattens the whole tree.

**How:**
```
User
 └── UserRole(s) [user_roles join table]
      └── Role [roles table]
           └── Permission(s) [role_permissions join table]
```
`UserService.getUserPermissions(UUID userId)` does:
```java
user.getUserRoles().stream()
    .flatMap(ur -> ur.getRole().getPermissions().stream())
    .distinct()   // deduplicates permissions shared across roles
    .map(permissionService::toResponse)
    .toList();
```
This means a user holding both `viewer` and `analyst` roles gets the union `{records:read, dashboard:view, records:history, dashboard:export}` without any duplication — even if `records:read` and `dashboard:view` are in both roles.

---

### 3 — Repository + JPA Specification (Dynamic query building)

**Where:** `repository/RecordRepository`, `repository/spec/RecordSpecification`

**Why:** `GET /api/records` supports four independent optional filters. Building queries with `if (param != null) query.where(...)` is fragile. `Specification<T>` composes predicates as objects via `Specification.where(...).and(...)`.

**How:** `RecordSpecification` exposes static factory methods (`byType`, `byCategory`, `byDateRange`, `excludeDeleted`). `RecordService.getAll()` chains them and delegates to `RecordRepository.findAll(spec, pageable)` — a Spring Data JPA `JpaSpecificationExecutor` method.

---

### 4 — Builder (Entity & DTO construction)

**Where:** Every `@Entity` (`User`, `FinancialRecord`) and all DTOs are annotated with Lombok `@Builder`.

**Why:** Constructors with many optional fields are error-prone. Builder pattern makes intent explicit, prevents argument-order bugs, and supports optional fields cleanly (e.g. `notes`, `role` in `RegisterRequest`).

**How:** Lombok generates the builder at compile time. `DataSeeder` and service classes use fluent `.builder()...build()` calls.

---

### 5 — DTO + MapStruct (Data transfer & mapping)

**Where:** `dto/request/`, `dto/response/`, `mapper/RecordMapper`, `mapper/UserMapper`

**Why:** Exposing entities directly couples the API contract to the persistence model. DTOs decouple them, allowing fields to be added/removed from the DB without changing the API and vice versa. MapStruct generates the boilerplate mapping code at compile time — zero reflection, zero runtime overhead.

**How:** `RecordMapper` and `UserMapper` are `@Mapper(componentModel = "spring")` interfaces. Spring injects them as beans. `RecordService` calls `recordMapper.toResponse(entity)` to produce API output.

---

### 6 — Facade (Dashboard aggregation)

**Where:** `service/DashboardService`

**Why:** The dashboard endpoint aggregates data from multiple repository calls (totals, category breakdown, monthly trends, recent activity). Exposing those four calls directly to the controller would scatter logic and make parallel fetching impossible.

**How:** `DashboardService.getFullSummary()` fires four `CompletableFuture.supplyAsync()` tasks concurrently, joins them with `CompletableFuture.allOf()`, then assembles the unified `DashboardSummaryResponse`. The controller calls exactly one method.

---

### 7 — Chain of Responsibility (Global error handling)

**Where:** `exception/GlobalExceptionHandler`, `exception/AppException`

**Why:** Many exception types need different HTTP status codes and response shapes. Scattering `try/catch` in controllers duplicates code. A `@RestControllerAdvice` acts as a chain: Spring dispatches to the most specific `@ExceptionHandler`, falling back to the generic `Exception` handler.

**How:**
- `AppException` — business rule violations thrown by services (4xx); carries an `HttpStatus`.
- `MethodArgumentNotValidException` → 400 with per-field messages.
- `AccessDeniedException` → 403.
- `BadCredentialsException` / `UsernameNotFoundException` → 401.
- `Exception` → 500 (stack trace logged, safe message returned).

All handlers produce `{ error, message, statusCode }`.

---

### 8 — Observer / Application Events (Audit log)

**Where:** `event/RecordCreatedEvent`, `event/RecordDeletedEvent`, `event/RoleAssignedEvent`, `event/RoleRevokedEvent`, `event/RoleChangedEvent` (legacy), `event/AuditListener`

**Why:** Audit logging is a cross-cutting concern. Embedding log calls in service classes couples them to the logging/persistence subsystem and makes unit testing harder. Spring's `ApplicationEventPublisher` decouples the publisher from the listener — multiple listeners can react to the same event without the publisher knowing.

**How:**
- `RecordService` publishes `RecordCreatedEvent` / `RecordDeletedEvent` after persisting changes → `AuditListener` writes structured log lines.
- `UserService.assignRole()` publishes `RoleAssignedEvent` → `AuditListener.onRoleAssigned()` persists a `user_role_history` row with `previousRole = null`.
- `UserService.revokeRole()` publishes `RoleRevokedEvent` → `AuditListener.onRoleRevoked()` persists a `user_role_history` row with `newRole = null`.
- The `GET /api/users/{id}/role-history` endpoint surfaces the full log sorted by `changedAt` descending.
- Adding a webhook or email notification requires **zero changes** to `UserService`.

---

### 9 — Hibernate Envers (Automatic record versioning)

**Where:** `model/FinancialRecord` (`@Audited`), `service/RecordService.getRecordHistory()`, `controller/RecordController` (`GET /{id}/history`)

**Why:** Financial records must be immutable from an audit perspective — every create, update, and soft-delete should be traceable. Implementing this manually would mean writing snapshot logic in every service method. Envers integrates with Hibernate's flush cycle and captures snapshots automatically.

**How:** Adding `@Audited` to `FinancialRecord` is the only change needed on the entity. Hibernate Envers creates two tables on startup:
- `revinfo` — one row per revision: `rev` (integer PK) and `revtstmp` (epoch milliseconds).
- `financial_records_aud` — one row per (record, revision) pair containing a snapshot of every audited column plus `revtype` (0 = INSERT, 1 = UPDATE, 2 = DELETE).

The `user` association is annotated `@NotAudited` to avoid deep-auditing the `User` entity while still tracking FK-level changes.

`RecordService.getRecordHistory()` queries via `AuditReader` (injected `EntityManager`), maps each `(snapshot, DefaultRevisionEntity, RevisionType)` triple to a `RecordHistoryResponse`, and returns all revisions sorted by revision number descending.

**Response shape:**
```json
[
  {
    "revision": 4,
    "timestamp": "2026-04-05T10:30:00",
    "changeType": "DEL",
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 1750.00,
    "type": "INCOME",
    "category": "Freelance",
    "date": "2026-04-02",
    "notes": "Updated amount after invoice correction",
    "deletedAt": "2026-04-05T10:30:00"
  },
  {
    "revision": 3,
    "timestamp": "2026-04-05T10:25:00",
    "changeType": "MOD",
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 1750.00,
    "type": "INCOME",
    "category": "Freelance",
    "date": "2026-04-02",
    "notes": "Updated amount after invoice correction",
    "deletedAt": null
  },
  {
    "revision": 1,
    "timestamp": "2026-04-05T10:15:00",
    "changeType": "ADD",
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "amount": 1500.00,
    "type": "INCOME",
    "category": "Freelance",
    "date": "2026-04-01",
    "notes": "API testing payment",
    "deletedAt": null
  }
]
```

---

## Assumptions & Tradeoffs

### Single role per user ~~(superseded)~~
~~Each `User` carries exactly one `Role` enum value.~~ **This is no longer the case.** Users now hold a `Set<UserRole>` — a many-to-many join to the `roles` table. Multiple roles can be assigned at runtime via `POST /api/users/{id}/roles/{roleId}`. The Strategy pattern classes (`AdminStrategy`, `AnalystStrategy`, etc.) have been removed; authority checks are handled by Spring Security's `hasAuthority()` against permission strings loaded from the DB.

### Monetary amounts in `BigDecimal`
All `amount` fields use `BigDecimal(precision=19, scale=4)` mapped to PostgreSQL `NUMERIC(19,4)`. `double`/`float` are avoided because floating-point arithmetic produces rounding errors that accumulate in financial summaries.

### Soft delete only
Records are never physically removed. `FinancialRecord.deletedAt` is set to the deletion timestamp; all queries apply `RecordSpecification.excludeDeleted()`. This preserves audit history and makes accidental-deletion recovery trivial. There is no `DELETE FROM` issued at the DB level.

### UTC everywhere
The JVM timezone is set to UTC (`hibernate.jdbc.time_zone=UTC`). All `LocalDate` / `LocalDateTime` values are stored and returned as UTC. The client is responsible for displaying them in the user's local timezone. This avoids off-by-one-day bugs caused by DST transitions in server-side date arithmetic.

### Audit persistence model (two-tier)
There are two complementary audit mechanisms:

1. **Hibernate Envers** (`financial_records_aud`, `revinfo`) — database-level snapshots of every `FinancialRecord` mutation captured automatically by Hibernate. Zero application code required beyond `@Audited`.
2. **Observer-based role history** (`user_role_history`) — `UserService.assignRole()` publishes a `RoleChangedEvent`; `AuditListener.onRoleChanged()` persists a `UserRoleHistory` row with the previous and new role, who made the change, and an optional free-text reason.

Record-create and record-delete events (`RecordCreatedEvent`, `RecordDeletedEvent`) still write to SLF4J only. In production these log lines could be shipped to an ELK stack. The observer pattern means adding a persistence listener later requires zero changes to `RecordService`.

### DDL set to `validate`
After the schema stabilised, `spring.jpa.hibernate.ddl-auto` was changed from `create` / `update` to `validate`. Hibernate verifies that database columns match entity mappings on startup and refuses to boot if they diverge. Schema migrations in production should be managed with Flyway or Liquibase.
