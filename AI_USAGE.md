# AI_USAGE.md — AI Methodology Log

## 1. Overview

This document describes how AI tools were used during the development of this project, with a strong emphasis on intentional usage, critical evaluation, and full human ownership of technical decisions.

The guiding principle throughout the project was:

> AI assists execution — it does not drive architecture or decision-making.

All major technical choices were made upfront, before using any AI tool, to ensure consistency, maintainability, and control over the system design.

---

## 2. Architecture & Stack Decisions (Human-Owned)

All architectural decisions were made independently to avoid bias and over-reliance on AI-generated suggestions.

### Backend
- **Java 21 + Spring Boot 3**

Chosen because:
- Strong prior experience ensured efficient and reliable development
- Mature ecosystem with well-established enterprise patterns
- Built-in support for security, dependency injection, and data management

### Frontend
- **Angular 17** (standalone components, signals)

Chosen because:
- Opinionated framework enforcing structure and best practices
- Suitable for scalable, maintainable applications
- Strong alignment with enterprise architectures

Although prior experience with Angular was limited, this choice was intentional:
- It allowed adoption of a structured framework
- AI was used to accelerate learning while outputs were critically reviewed

### Authentication & Authorization
- **OAuth 2.0 / OIDC + Keycloak**

Chosen because:
- Industry-standard identity and access management
- Seamless integration with Spring Security's OAuth2 Resource Server
- JWT-based authentication + PKCE code flow
- Container-friendly architecture

### Database
- **PostgreSQL**

Chosen because:
- Strong compatibility with Keycloak (shared schema `keycloak`)
- ACID compliance and reliability
- Advanced features (indexing, JSON support, extensibility)
- Production-grade maturity

### Infrastructure
- **Docker / Docker Compose**

Used to:
- Ensure environment consistency across machines
- Orchestrate all services with health-check dependencies (postgres → keycloak → backend → frontend)
- Keep secrets out of source control via `.env` files

---

## 3. AI Tools Used

| Tool              | Primary Use                                                           |
|-------------------|-----------------------------------------------------------------------|
| **Claude AI**     | Scaffolding, code generation, debugging, test generation, code review |
| **Google Stitch** | UI/UX design exploration and layout ideation                          |

---

## 4. What AI Was Used For

| Task                   | Detail                                                                                                                                                          |
|------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Full-stack scaffolding | Backend layers (controller / service / repository / dto / mapper), Angular 17 standalone components, Docker Compose multi-service setup                        |
| CRUD acceleration      | Boat entity lifecycle — paginated list with search/status/type filters, create, update, delete; `ProblemDetail` error responses (RFC 7807)                      |
| Auth debugging         | OAuth2/OIDC silent-refresh race conditions, `AuthGuard` redirect loops, `authInterceptor` 401/403 error handling                                                |
| Unit test generation   | Spring Boot `@Nested` / AAA service tests, `@WebMvcTest` controller slice tests, `@DataJpaRepository` repo tests, Angular `HttpTestingController` service specs |
| Audit system           | `@Auditable` AOP annotation, `AuditAspect` with `@Around` advice, async `AuditLogService` with `REQUIRES_NEW` propagation, JSONB metadata column               |
| Code review            | Angular RxJS patterns, `takeUntil` memory-leak checks, WCAG 2.2 AA accessibility pass (aria-labels, roles, color contrast)                                     |
| Documentation          | Springdoc / OpenAPI `@Tag`, `@Operation`, `@ApiResponse` annotations; structured JSON log format with MDC `user` field                                         |

**AI was NOT used for:**
- Architecture design and technology selection (see §2)
- Security decisions (CORS strict-origin policy, stateless JWT config, `@PreAuthorize` for audit endpoints)
- Business rules (LIKE-injection escaping, `AuditAspect` threading model, audit record immutability)
- Final validation of all generated tests and code

---

## 5. Representative Prompts

### Prompt 1 — Full Application Generation (Claude AI)

```
You are a senior Java/Spring Boot engineer and software architect.
Your task is to generate a complete, production-ready backend for a Boat Management Application.
Produce real, executable code only — no pseudo-code, no placeholders, no skipped sections.

---
GOAL
Build a Spring Boot 3 backend with REST API, JWT authentication via Keycloak, PostgreSQL persistence, and full Docker support.

---
TECHNICAL STACK (MANDATORY)
* Java 21 / Spring Boot 3 / Spring Security (OAuth2 Resource Server, JWT via Keycloak)
* Spring Data JPA + Hibernate / PostgreSQL
* Springdoc OpenAPI (Swagger UI at /swagger-ui.html) / Jakarta Validation
* Docker + Docker Compose

---
REQUIREMENTS
1. Architecture: clean layered — controller / service / repository / dto / mapper
2. Entity Boat: id (Long or UUID), name (required, not blank), description (String), createdAt (Instant or LocalDateTime)
3. API: GET /api/boats (pagination + search), GET /api/boats/{id}, POST /api/boats, PUT /api/boats/{id}, DELETE /api/boats/{id}
4. Features: Pagination (Spring Pageable), Search by name and description (partial match)
5. Validation: Jakarta (@NotBlank, etc.) — return 400 / 404 / 401 / 403
6. Security: OAuth2 Resource Server, JWT from Keycloak, ALL endpoints secured
7. Documentation: Swagger UI at /swagger-ui.html
8. Extras (MANDATORY): DTO pattern (no entity exposure), @ControllerAdvice, Logging

---
NON-FUNCTIONAL REQUIREMENTS
* Clean and readable code / Proper separation of concerns / Scalable architecture
* Production-ready structure / No hardcoded secrets / Use .env files

---
IMPORTANT CONSTRAINTS
* DO NOT skip any section / DO NOT provide pseudo-code
* Provide REAL, executable code / Use best practices / Keep explanations concise but clear

---
BONUS (if possible)
* Add OpenAPI annotations / Add Testcontainers for tests / Add environment-based config (dev/prod)

---
EXPECTED OUTPUT
1. Complete folder structure
2. Backend implementation
3. API examples (curl or Postman)
4. Step-by-step run instructions

---
EXECUTION PLAN — Follow this order strictly:
1. Architecture overview 
2. Backend implementation 
3. Docker setup  
4. Run instructions

Now build the complete backend application.
```

---

### Prompt 2 — UI/UX Design (Stitch)

```
I am building a web application for managing boats. The frontend is developed using Angular.
---
User Flow : 
    * When the user opens the application, they are redirected to a login page.
    * After successful authentication, the user is redirected to the boat list page.
    * The authenticated user can:
    * View a paginated list of boats
    * Search and filter boats by name or description
    * Click on a boat to view its details
    * Create a new boat
    * Update an existing boat
    * Delete a boat (with a confirmation dialog)
    * Before deleting a boat, a confirmation modal must be displayed to prevent accidental deletion.
---
Accessibility Requirement:
    The entire application must comply with WCAG 2.2 AA accessibility standards, ensuring it is usable by people with disabilities (including keyboard navigation support, proper semantic structure, sufficient color contrast, screen reader compatibility, and accessible form controls).
```

---

### Prompt 3 — Debugging Authentication Flow (Claude AI)

```
You are a senior Angular and Spring Boot security engineer with deep expertise
in OAuth2/OIDC flows, browser security policies, and Angular routing.

---
CONTEXT

Stack:
- Frontend: Angular 17 (standalone components), angular-oauth2-oidc library
- Backend: Spring Boot 3, Spring Security OAuth2 Resource Server
- Auth server: Keycloak (latest)

Application flow:
1. User authenticates via Keycloak (OIDC Authorization Code Flow)
2. Angular stores the JWT and uses an HTTP interceptor to attach it to requests
3. Routes are protected by a custom AuthGuard using canActivate

---
PROBLEM

After successfully creating an entity (POST → 201), navigating to the sibling
Details route (/boats/:id) causes one of two failure modes:

**Failure A** — Infinite loading:
- The Details page shows a spinner indefinitely
- No network errors in DevTools
- No console errors from Angular

**Failure B** — Unexpected redirect to login:
- The app redirects the user to Keycloak login unexpectedly
- The user is already authenticated (valid JWT in storage)
- This only happens when navigating between sibling routes, not on page load

---
SPECIFIC QUESTIONS

Answer each question separately and in order:
1. What are the known reasons why silent refresh fails?
2. How does angular-oauth2-oidc handle token expiry during in-app navigation?
   Where exactly can the AuthGuard trigger a redirect loop when the token
   is technically valid but hasValidAccessToken() returns false?
3. What is the correct guard logic to:
   - Check token validity without triggering unnecessary redirects
   - Handle the async nature of token refresh before allowing navigation
   - Avoid race conditions between the guard and the OIDC service initialization

---
CONSTRAINTS
- Do not suggest replacing angular-oauth2-oidc with another library
- Do not suggest disabling the AuthGuard
- Solutions must be compatible with Angular 17 standalone components
- Assume Keycloak uses Authorization Code Flow + PKCE (no implicit flow)

---
EXPECTED OUTPUT
For each question above provide:
1. Root cause explanation (2–4 sentences)
2. Corrected code snippet (TypeScript, production-ready)
3. One-line summary of the fix

End with a unified AuthGuard implementation that addresses all issues.
```

---

### Prompt 4 — Unit Test Generation (Claude AI)

```
You are an expert in unit testing and backend development.
Your task is to generate comprehensive, robust, and maintainable unit tests.

Constraints and expectations:
- Use the appropriate testing framework for Java 21 and Spring Boot 3
- Cover the following cases:
  - normal behavior
  - edge cases
  - error cases (exceptions, invalid inputs)
- Mock external dependencies (database, APIs, services)
- Follow best practices (AAA: Arrange, Act, Assert)
- Use clear and descriptive test names
- Structure the tests in a readable and modular way
- Add comments when necessary to explain complex cases

Expected output format:
1. Brief explanation of the testing strategy
2. Complete unit test code ready to run
3. Suggestions to improve code testability if relevant
```

---

### Prompt 5 — Frontend Code Review (Claude AI)

```
You are an expert Angular developer and software architect. Please perform a comprehensive review of the frontend code.

---
CONSTRAINTS
 - Scope the review to Angular 17 standalone components only
 - Do not suggest migrating to a different framework or library
 - Do not refactor working logic — flag issues only, with targeted fixes

---
EXPECTATIONS
 - Highlight deprecated practices, inefficient logic, or code smells.
 - Ensure compliance with the latest Angular style guide (e.g., standalone components, signals, proper RxJS patterns).
 - Spot potential memory leaks (missing unsubscriptions) and security vulnerabilities.
 - For every issue found, provide a clear explanation and a code snippet showing the improved version.

---
EXPECTED OUTPUT
 - Suggest a testing plan (Unit tests with Jasmine/Jest or E2E with Playwright/Cypress) for the most critical parts of the reviewed code and provide example test cases.
```

---

## 6. Validation & Critical Review

AI outputs were never used as-is. Each generated artifact was reviewed, corrected, or partially rewritten before commit.

### Backend

| Area                        | What was changed                                                                                                                                                                                                                                                        |
|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SecurityConfig`            | AI generated a wildcard CORS config (`*`). Replaced with strict origin parsing from `cors.allowed-origins`; added a startup guard that throws `IllegalStateException` if a wildcard is detected — because `allowCredentials=true` + `*` is rejected by browsers anyway. |
| `AuditAspect`               | AI placed HTTP context extraction inside the async thread. Fixed: all `AuditContext` reads (username, IP, user-agent) now happen synchronously on the HTTP worker thread _before_ the async handoff, since `SecurityContextHolder` is not propagated by default.         |
| `BoatService.findAll`       | AI left raw `%` and `_` characters passable to the LIKE clause. Added `escapeLike()` to prevent LIKE injection; the JPQL query uses `ESCAPE '\\'` to honour the escapes.                                                                                                |
| `AuditAspect.sanitise()`    | AI did not sanitise exception messages before DB storage. Added newline stripping and 1000-character truncation to prevent log injection.                                                                                                                                |
| `GlobalExceptionHandler`    | AI double-logged `BoatNotFoundException` — once in the aspect (FAILURE record) and once here. Fixed: handler only audits `AccessDeniedException`, which Spring Security throws _before_ the service layer and therefore before `AuditAspect` can intercept it.          |
| `AsyncConfig`               | AI generated a default Spring async config with no queue bound. Replaced with a dedicated `auditExecutor` thread pool (core=2, max=4, queue=500) and `CallerRunsPolicy` to degrade gracefully under load rather than silently dropping audit records.                   |
| `AuditLog` entity           | AI added `@Setter` on the audit entity. Removed: audit rows must be insert-only and never mutated. Retained only `@Getter`, `@Builder`, `@NoArgsConstructor`/`@AllArgsConstructor`.                                                                                     |

### Unit Tests

| Area                           | What was changed                                                                                                                                                                                                                                              |
|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `BoatIntegrationTest`          | AI generated tests without Docker availability checks. Introduced `@EnabledIfDockerAvailable` custom condition to skip tests gracefully in CI without Docker. Added `@AfterAll` to explicitly close the Testcontainers `PostgreSQLContainer`.                 |
| `BoatServiceTest`              | AI mocked all dependencies but missed edge cases on LIKE-escaped search. Added specific tests for `%`, `_`, and `\` in search terms to verify `escapeLike()` correctness.                                                                                     |
| Auth guard / interceptor specs | AI tests used `done()` callbacks (deprecated in Jasmine 4). Rewritten with `firstValueFrom` + `async/await`.                                                                                                                                                  |
| `AuditAspectTest`              | AI-generated test verified the happy path only. Added failure-path tests to assert that a `FAILURE` record is persisted even when the service throws, and that the exception is re-thrown unchanged so the HTTP response is not swallowed.                    |

### Frontend

| Area                     | What was changed                                                                                                                                                                                                       |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AuthService.initAuth()` | AI used `tryLogin()` unconditionally. Changed to branch on `responseType === 'code'` and call `tryLoginCodeFlow()` for PKCE, with a type-safe cast since the method is not in the public interface.                    |
| `authInterceptor`        | AI generated a generic 401 handler that redirected to `/login`. Fixed to call `authService.login()` (which preserves the return URL in `sessionStorage`) and route 403 responses to `/boats` instead of the login page.|
| `ThemeService`           | AI did not guard `localStorage`/`window` access for SSR compatibility. Added `typeof localStorage !== 'undefined'` and `typeof window !== 'undefined'` guards.                                                         |
| Dark mode                | AI suggested CSS variable swapping without respecting `prefers-color-scheme`. `ThemeService` now initialises from OS preference when no stored value exists.                                                            |
| WCAG 2.2 AA              | AI-generated templates lacked `aria-label` on icon-only buttons and missing `role` on interactive elements. All corrected manually after accessibility audit.                                                           |

---

## 7. What Was NOT Delegated to AI — and Why

| Decision                                                        | Reason not delegated                                                                                                                                                                                                                                                    |
|-----------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Stack selection** (Java 21, Angular 17, Keycloak, PostgreSQL) | Requires weighing personal experience, long-term support, ecosystem maturity, and project constraints. AI would optimise for popularity, not fit.                                                                                                                           |
| **CORS security policy**                                        | Security misconfiguration is the most common class of production vulnerabilities. A wildcard `*` with credentials is silently dangerous. This required understanding the exact browser security model — not just following a pattern.                                   |
| **AOP threading model**                                         | The decision to extract `SecurityContext` synchronously _before_ the async handoff is a concurrency correctness requirement, not a convention. Getting this wrong produces a subtle data-race that passes all tests but fails intermittently in production under load.  |
| **LIKE injection escaping**                                     | Not a well-known attack vector for AI-generated code. The fix required recognising that `%` and `_` in user input change query semantics — a domain reasoning problem, not a code generation one.                                                                       |
| **Audit endpoint access control**                               | `@PreAuthorize("hasRole('ADMIN')")` placement and scope (which operations require admin vs. any authenticated user) is a business rule that cannot be delegated without a full understanding of the authorization model.                                                |
| **Audit record immutability**                                   | AI placed `@Setter` on `AuditLog`. The decision to make audit rows insert-only (no `@Setter`, no update timestamp, no JPA `merge`) is a data-integrity contract. Delegating it risks silent violations if AI later adds convenience setters during iterative prompts.  |
| **`AsyncConfig` thread-pool sizing + rejection policy**         | `CallerRunsPolicy` was a deliberate choice: it degrades gracefully (the HTTP thread runs the audit write inline) rather than dropping records silently. AI cannot reason about the operational trade-off between latency, backpressure, and audit completeness.        |
| **Docker Compose health-check chain**                           | The dependency order (postgres → keycloak → backend → frontend) and the specific `healthcheck` commands for each service require operational knowledge of each container's readiness semantics. AI consistently produced incomplete or incorrect health-check configs.  |

---

## 8. Key Takeaways

- **Prompt quality determines output quality.** Structured, constraint-heavy prompts (stack, forbidden approaches, expected output format) produced directly usable scaffolding. Vague prompts produced generic boilerplate requiring full rewrites.
- **Security and concurrency require human review.** AI consistently produced plausible but subtly incorrect code in these two areas. Every security config and every async boundary was verified or rewritten.
- **AI is not a substitute for domain reasoning.** Business rules (audit scope, LIKE escaping, auth guard behavior) and operational decisions (Docker health-check chains) were out of scope for delegation.
- **AI accelerated learning, not just execution.** For Angular 17 (limited prior experience), AI-generated examples served as a learning scaffold — but were always cross-referenced with official documentation before adoption.

---

## 9. Conclusion

Human-defined architecture combined with AI-assisted execution ensured high productivity without sacrificing correctness or security. The discipline of treating AI output as a first draft — not a final answer — was the key practice that made this approach work at production quality.
