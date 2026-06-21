# AGENTS.md — Minecloud

## Architecture overview

Monorepo managed by **pnpm workspaces** (no Turborepo configured yet, despite the plan).
Two halves connected by a Spring Cloud Gateway:

- **Frontend**: `apps/web/` — React 19 + Vite 8 + TypeScript 6 (boilerplate only, not integrated yet)
- **Backend**: `apps/server/` — Spring Boot 3.5.14 + Maven multi-module (Java 21)

The Gateway is **not** a separate top-level app. It lives inside the Maven reactor at `apps/server/gateway/` and routes to the two backend services.

```
Browser → :8080 (Gateway) → :8081 (auth) or :8082 (storage)
```

## Maven module tree (under apps/server/)

```
server (root POM)
├── api/          — shared DTOs (auth/dto/, storage/dto/)
├── common/       — JWT, R<> wrapper, BusinessException, CORS, GlobalExceptionHandler
├── gateway/      — Spring Cloud Gateway (port 8080, reactive)
└── services/     — aggregator POM
    ├── auth/     — login, register, JWT, RBAC (port 8081)
    └── storage/  — file CRUD, chunked upload, directory tree (port 8082)
```

## Current status vs. future plan

**What exists and works:**
- Auth: register (sends verification email), login, JWT access+refresh, email verification, forgot-password / reset-password with email, RBAC entities (sys_user, sys_role, sys_permission, join tables), permission-check API endpoint for service-to-service calls
- Storage: upload, chunked upload + MD5 merge, download, delete (soft-delete with recursive child cleanup), mkdir, browse/path-resolution; directory tree via `file_node.parent_id`
- `StorageBackend` interface with a working `LocalStorageBackend`; `S3StorageBackend` and `MountStorageBackend` are stubs (registered as beans so `StorageRouter` can discover them)
- `StorageRouter` selects backends per-file based on `FileNode.storageType` with log warnings on fallback
- Service-to-service permission checks: storage calls auth via Feign (`AuthClient`) instead of raw SQL joins

**Not yet implemented:**
- share, recycle bin UI/API, search, admin UI
- Redis, MinIO (docker-compose has only PostgreSQL)
- OCR, RAG, Agent services (entirely not started)
- Tests, CI/CD, Dockerfiles

## Essential commands

All commands run from repo root unless noted.

### Start PostgreSQL (required before running backend)

```bash
pnpm --filter @minecloud/docker docker:up
```

### Build the entire backend

```bash
# Use the Maven wrapper directly (the package.json scripts hardcode mvnw.cmd — Windows-only)
./apps/server/mvnw clean compile -f apps/server/pom.xml
```

To build a single module (faster for verification):

```bash
./apps/server/mvnw compile -pl services/auth -am -f apps/server/pom.xml
```

### Run individual services

Each service is a `@SpringBootApplication`. Run from the server directory:

```bash
./apps/server/mvnw spring-boot:run -pl gateway -f apps/server/pom.xml
./apps/server/mvnw spring-boot:run -pl services/auth -f apps/server/pom.xml
./apps/server/mvnw spring-boot:run -pl services/storage -f apps/server/pom.xml
```

Or from within each module directory:

```bash
cd apps/server/gateway && ../../mvnw spring-boot:run
```

### Frontend

```bash
pnpm --filter @minecloud/web dev     # Vite dev server
pnpm --filter @minecloud/web lint    # ESLint
pnpm --filter @minecloud/web build   # tsc + vite build
```

## Gotchas and quirks

### Storage path is Windows-hardcoded
`apps/server/services/storage/src/main/resources/application.yml` has `minecloud.storage.upload-path: D:/minecloud/data/`. On Linux/macOS this will fail. Change it to `/opt/minecloud/data` or a local path before running.

### Maven wrapper package.json scripts are Windows-only
`apps/server/package.json` scripts all use `mvnw.cmd`. On Linux/macOS use `./apps/server/mvnw` directly instead.

### Logical delete field name is inconsistent
- Auth entities: field name `deleted` (`logic-delete-field: deleted`)
- Storage entity (`FileNode`): field name `isDeleted` (`logic-delete-field: isDeleted`)

When adding new entities, match the module's convention. Do NOT unify them unless refactoring both modules.

### JWT secret is hardcoded and duplicated
The same JWT secret appears in both `gateway/src/main/resources/application.yml` and `services/auth/src/main/resources/application.yml`. Changes must be kept in sync.

### User context is passed via request attribute
The gateway filter (`JwtAuthGatewayFilter`) parses the JWT and sets `X-User-Id` and `X-Username` headers on the downstream request. Storage's `UserContextFilter` reads these headers and converts them to request attributes (`currentUserId`, `currentUsername`). Storage controllers read `currentUserId` via `request.getAttribute("currentUserId")`. No Spring Security context is propagated to downstream services. New controllers must follow this pattern.

### Service-to-service permission checks use Feign
Storage checks permissions by calling auth via Feign (`AuthClient` at `org.feiesos.storage.client`), not by querying auth tables directly. The auth module exposes `GET /api/v1/auth/permission/check?userId=X&permissionCode=Y`. The Feign client URL is configured via `minecloud.auth.url` (default `http://localhost:8081`). When adding new permission checks in storage, use `AuthzService.checkPermission()` — never query sys_role/sys_permission tables from storage.

### Chunk uploads are user-isolated
Chunks are stored under `{chunk.temp-dir}/{userId}/{md5}/{index}`. The temp dir is configurable via `minecloud.storage.chunk.temp-dir` (default `temp`). Both `uploadChunk` and `mergeChunks` require authentication and are scoped to the authenticated user.

### DB credentials are hardcoded
All `application.yml` files use `postgres/postgres` for the `minecloud` database. Match this in local development.

### Only PostgreSQL is containerized
Despite the tech stack listing Redis and MinIO, `infra/docker/docker-compose.yml` only defines PostgreSQL 16. If you need Redis or MinIO, add them there (but they're not required by current code).

### Email configuration required for auth features
Registration, email verification, and password reset require an SMTP server. Configure via environment variables: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`. Without SMTP config, emails are skipped and tokens are logged at `WARN` level. For local development, use [Mailpit](https://github.com/axllent/mailpit) or similar.

### No Turborepo
The AGENTS.md plan mentions Turborepo but `turbo.json` does not exist. The monorepo uses plain pnpm workspaces. Any cross-package orchestration must use pnpm filters (`--filter`).

### Lombok annotation processor requires explicit Maven config
The root `pom.xml` has an explicit `maven-compiler-plugin` configuration with `annotationProcessorPaths` pointing to Lombok. Without this, Lombok annotations (`@Data`, `@Builder`, `@Slf4j`, etc.) are not processed and the build fails. If you create a new Maven module, ensure it inherits from the root POM or replicates this config.

## Coding conventions

### Package structure
- `org.feiesos.{module}.controller` — REST controllers (thin, delegate to facade/service)
- `org.feiesos.{module}.service` — service interfaces
- `org.feiesos.{module}.service.impl` — implementations
- `org.feiesos.{module}.entity` — MyBatis Plus entities (use `@TableName`, `@TableLogic`)
- `org.feiesos.{module}.mapper` — MyBatis Plus mappers (extend `BaseMapper<T>`)
- `org.feiesos.{module}.backend` — storage backend implementations

### Shared code goes in `api/` or `common/`
- DTOs that cross module boundaries → `api/` module (e.g. `org.feiesos.api.auth.dto.*`)
- Infrastructure (JWT, exception handler, result wrapper) → `common/` module
- Never duplicate a DTO or utility between auth and storage

### Response format
All controllers return `R<T>` (from `common`). Use `R.ok(data)`, `R.ok()`, `R.fail(msg)`, or `R.fail(code, msg)`. The timestamp field is set automatically.

### Storage abstraction (critical)
- **Interface**: `StorageBackend` (NOT "StorageProvider" as the old AGENTS.md called it)
- Business code must go through `StorageFacade`, which routes through `StorageRouter` → `StorageBackend`
- Never call `java.nio.file` or MinIO SDK directly from a controller, service, or facade
- Adding a new storage backend: implement `StorageBackend`, register in `StorageRouter`, add value to `StorageType` enum

### Permission checks
Use `AuthzService.checkPermission(userId, "permission:string")`. Current permissions used:
- `file:read`, `file:write`, `file:delete`

### Constructor injection
Use constructor injection (not `@Autowired` fields). The existing code uses it consistently.

### No tests exist yet
There are zero tests in the repository. When adding tests, use the standard Spring Boot test stack. Run with:

```bash
./apps/server/mvnw test -pl services/auth -f apps/server/pom.xml
```

## Key files to know

| File | Why it matters |
|---|---|
| `apps/server/pom.xml` | Root Maven POM, defines all modules and dependency versions |
| `apps/server/gateway/src/main/resources/application.yml` | Gateway routes + JWT secret |
| `apps/server/services/auth/src/main/resources/application.yml` | Auth DB config + JWT + MyBatis Plus logical delete |
| `apps/server/services/storage/src/main/resources/application.yml` | Storage config + upload path + chunk temp dir |
| `apps/server/services/storage/.../backend/StorageBackend.java` | Storage abstraction interface |
| `apps/server/services/storage/.../backend/StorageFacade.java` | Main orchestration — all file ops go through here |
| `apps/server/services/storage/.../backend/StorageRouter.java` | Backend routing with log warnings on fallback |
| `apps/server/services/storage/.../client/AuthClient.java` | Feign client for auth permission checks |
| `apps/server/services/storage/.../dto/FileItemResponse.java` | DTO for API responses (decouples entity from API) |
| `apps/server/common/src/main/java/org/feiesos/common/result/R.java` | Unified API response wrapper |
| `apps/server/common/src/main/java/org/feiesos/common/security/JwtTokenProvider.java` | JWT create/parse/validate |
| `apps/server/gateway/.../filter/JwtAuthGatewayFilter.java` | Gateway JWT filter — sets `X-User-Id` header |
| `apps/server/services/storage/.../config/UserContextFilter.java` | Converts headers to request attributes |
| `infra/docker/docker-compose.yml` | PostgreSQL container |

## Design constraints (keep these in mind)

- **DDD-lite**: modules map to domains (auth, storage, share, recycle, search). Keep logic in the right module.
- **Interface-first**: always define a service interface before the implementation.
- **AI isolation**: agent/OCR/RAG are independent services. The Spring Boot server only handles users, permissions, files, and API routing. Never embed LLM/OCR/Embedding calls in core modules.
- **Module boundaries**: before adding code, ask: which module does this belong to? Should it be an interface? Does it affect the storage abstraction? Does it need RBAC?
- **Avoid**: god classes, giant services, util class proliferation, magic strings, hardcoded values (except in application.yml config).
