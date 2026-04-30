# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Instructions for Claude Code — Backend

You are helping Marius build and improve the **Spring Boot microservices backend**
for the Geodata project. This file contains standing instructions. Read it before
every session.

## Project at a glance

- **Goal:** A robust, secure map-borrowing platform backend.
- **Stack:** Spring Boot 3.5 microservices + Postgres + Docker Compose
- **Services (3):** `identity-service` (8010), `map-service` (8020), `borrow-service` (8030)
- **Data layer:** Postgres (Supabase in dev, in-cluster in k8s). Schema-per-service (`auth`, `map`, `borrow`).
- **Schema migrations:** Flyway (per service)
- **Deployment:** Docker Compose locally, Kubernetes manifests in `k8s/`
- **Frontend:** separate React app (don't touch it)
- **Mobile:** separate Flutter app that calls the REST APIs

> **Note on history:** there used to be `client-service`, `item-service`,
> `chat-service`, and `review-service`. They were collapsed into the current
> 3 services (commit `ff54977`) and the unused ones removed. Do not reintroduce
> them — the architecture is intentionally narrow.

## Marius's background

- Strong with Spring Boot, Java, microservices patterns
- Experience with QueryDSL filtering, JWT auth, Spring Security,
  Supabase Storage, Swagger/OpenAPI, Docker, GitHub Actions, Render deployment
- Comfortable with unit testing (Mockito), debugging circular dependencies,
  port-blocking issues
- **Key point:** he knows what he's doing — don't over-explain Spring basics.
  Focus on clean architecture, security, and solving actual problems.

## How to work with Marius

- **One service at a time.** Don't refactor all 3 services in one PR.
  Pick one, improve it, test it, then move to the next.
- **Explain architectural trade-offs.** When there are multiple ways to solve
  something (e.g., RS256 vs HMAC JWT, RestTemplate vs Feign), outline the
  pros/cons and let Marius pick.
- **Test before pushing.** Write unit tests (Mockito), repo tests
  (Testcontainers), and run integration tests against `docker-compose up`.
- **When adding features, think about the mobile client.** The Flutter app will
  call these endpoints. Keep REST contracts clean and documented (Swagger).
- **Show actual commands.** Don't just describe what you would do — run it,
  show the output, commit the changes.

## Hard rules

1. **Never commit secrets.** No real DB passwords, JWT secrets, API keys in
   `application.yml` or any tracked file. Use environment variables.
2. **Don't break Docker Compose.** If you add a new service or change ports,
   update both `backend/docker-compose.yml` and the root
   `/home/simba/DEV/geodata/docker-compose.yml`, and test locally.
3. **Maintain backward compatibility** with the React frontend and mobile app
   unless Marius explicitly says to break it. REST endpoints are contracts.
4. **PostGIS is available but optional.** If you want to add spatial queries
   (e.g., "maps near location"), use Postgres functions, not client-side
   filtering. But don't force it if simple lat/lng columns work.
5. **No new async messaging unless asked.** Kafka/Zookeeper were removed
   (dead infrastructure). If a real async need appears, discuss before adding.
6. **Logging over print statements.** Use SLF4J. Add contextual info
   (request IDs via MDC, user IDs, timestamps).
7. **Don't remove or rename existing endpoints** without coordinating with
   Marius first — they're consumed by the React frontend and mobile app.
8. **Schema changes go through Flyway**, not `ddl-auto: update`. Every schema
   change is a new versioned migration file.

## Common commands

```bash
# Build all services from root
mvn clean compile

# Run tests for all services
mvn test

# Run a single service (from its directory)
cd identity-service && mvn spring-boot:run

# Run with dev profile
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run

# Start infrastructure (Postgres, pgAdmin, Mailpit, SonarQube)
cd backend && docker compose up -d

# Check health after startup
curl localhost:8010/actuator/health
curl localhost:8020/actuator/health
curl localhost:8030/actuator/health

# Run tests for a single service
cd identity-service && mvn test

# Run a single test class
cd identity-service && mvn test -Dtest=UserServiceTest

# SonarQube analysis
mvn verify sonar:sonar -Dsonar.token=<token>
```

## Architecture overview

### Multi-module Maven project
`pom.xml` at the root is the parent POM. All services inherit Spring Boot 3.5.1, Java 21, and shared dependency versions (`jjwt 0.12.6`, `springdoc 2.8.5`, `logstash-logback 7.4`). JaCoCo aggregate coverage report runs at parent level.

### Service internals (same pattern across all 3)
Each service follows: `controller → service → repository → JPA entity`. DTOs are used for REST contracts (never expose entities directly). Bean Validation + Swagger annotations on all DTOs.

Package root: `com.geodata.<layer>` (not nested by service name since each is its own module).

### JWT auth flow
- **identity-service** issues HMAC-SHA256 JWT tokens (`secretsJwtString` env var, same secret shared across all services).
- **map-service** and **borrow-service** validate tokens via their own `JwtAuthFilter` (duplicated code — Phase 3 roadmap extracts this into `geodata-security-commons`).
- Tokens are passed through on inter-service calls via `RestTemplate` with `Authorization: Bearer <token>` header.

### Inter-service communication
Synchronous REST via `RestTemplate`. URLs are constructed with `HTTP + ${services.<name>}` — defaults to `localhost:<port>` if env var not set. Pattern used in `BorrowsService` for calling identity-service and map-service.

### Borrow saga pattern
`BorrowsService.borrowMap()` implements a manual compensating transaction:
1. Fetch user from identity-service
2. Fetch map from map-service, check availability
3. Lock map (`setAvailableToBorrowed`)
4. Save borrow record
On any failure after step 3: compensation step reverts map status via `setBorrowedToAvailable`.

### Security configuration (per service)
- **identity-service:** full Spring Security with `UserDetailsService`, BCrypt, session-less. Routes: `/api/auth/**` public, `/api/admin/**` requires `ADMIN`, `/api/home` authenticated.
- **map-service / borrow-service:** stateless JWT validation only, no `UserDetailsService`. `UserDetailsServiceAutoConfiguration` is excluded.

### Roles
`USER`, `ADMIN`, `LIBRARIAN`, `MANAGER`. Stored as strings in the `users` table.

### Database schemas
All 3 services share one Postgres instance (dev). Each service owns its own tables:
- `identity-service`: `users` (user_id, username, name, password, role, is_enabled, created_at, updated_at)
- `map-service`: `maps` (map_id, name, year, is_enabled, availability_status, created_at, updated_at)
- `borrow-service`: `borrows` (id, user_id, map_id, borrow_date, return_date, status)

Flyway migrations in `src/main/resources/db/migration/`. Current baseline: `V1__init.sql` per service. `ddl-auto: validate` — Flyway must run before the app starts.

### Pagination
`PagedResponse<T>` is a generic wrapper used in map-service and borrow-service for paginated list endpoints.

### MDC / request tracing
Each service has its own `MDCFilter` that generates/propagates `X-Request-ID`. RestTemplate interceptor (in `AppConfig`) forwards the header on outgoing calls.

## Service-by-service notes

### identity-service (port 8010)
- Handles user registration, login, JWT token issuance
- Schema: `auth`. Stores users.
- Roles: `USER`, `ADMIN`, `LIBRARIAN`, `MANAGER`
- Endpoints: `/api/auth/{register,login}`, `/api/users/**`, `/api/admin/**`, `/api/home/**`
- **Key:** if you change the JWT signing algorithm or key, coordinate with
  the other services — they validate tokens issued here.

### map-service (port 8020)
- The catalog. CRUD for maps (the "books" being borrowed).
- Schema: `map`. Fields: name, year, availability state, enabled flag, timestamps.
- Availability states: `AVAILABLE`, `BORROWED`. Transitions are driven
  by borrow-service via REST callbacks.
- Endpoints: `/api/maps/**`, `/api/maps/manager/**`
- Key internal endpoints called by borrow-service: `/api/maps/setAvailableToBorrowed`, `/api/maps/setBorrowedToAvailable`, `/api/maps/batch` (POST, accepts list of IDs)

### borrow-service (port 8030)
- The core business logic: borrow request → return → transfer
- Schema: `borrow`
- Talks to identity-service (user info via `/api/home`, `/api/users/{userId}`) and map-service (availability) via `RestTemplate` with JWT pass-through
- Endpoints: `/api/borrows/**` (user actions), `/api/borrows/librarian/**` (librarian actions)
- Borrow statuses: `BORROWED`, `RETURNED`, `TRANSFERRED`

## Testing strategy

- **Unit tests (Mockito):** for service layer logic. Aim for 70%+ coverage on business logic.
- **Repository tests (`@DataJpaTest` + Testcontainers Postgres):** verify custom queries against a real Postgres. Tests run Flyway migrations against the container — this also validates the migrations themselves.
- **Controller tests (`@WebMvcTest`):** verify route mappings, status codes, authorization rules.
- **Integration test for the borrow flow:** one `@SpringBootTest` with Testcontainers spinning up all 3 services + Postgres, exercising the full register → login → create map → borrow → approve → return flow.
- **Manual:** `docker-compose up`, test with Postman or `curl`.

## Deployment notes

- **Local:** `cd backend && docker-compose up -d` brings up Postgres + pgAdmin + Mailpit + SonarQube. Run each service via `mvn spring-boot:run` or your IDE.
- **Prod:** `kubectl apply -f k8s/`. Manifests in `k8s/` deploy 3 services + Postgres + ingress.
- **Mobile testing:** services run in Docker on your laptop. Flutter Android emulator hits them at `10.0.2.2:<port>` (Android's alias for host `localhost`).

## Required environment variables

All 3 services need:
- `SUPABASE_JDBC_URL` — JDBC connection string
- `SUPABASE_DATABASE_USER`
- `SUPABASE_DATABASE_PASSWORD`
- `JWT_SECRET` — shared HMAC secret for JWT signing/validation

borrow-service also needs:
- `IDENTITY_SERVICE` (default: `localhost:8010`)
- `MAP_SERVICE` (default: `localhost:8020`)

identity-service also needs:
- `BORROW_SERVICE` (default: `localhost:8030`)

map-service also needs:
- `BORROW_SERVICE` (default: `localhost:8030`)

## Code style

- **Java 21**, Lombok for boilerplate (`@RequiredArgsConstructor`, `@Slf4j`, `@Builder`).
- **Package structure:** `com.geodata.<layer>` (controller, service, repository, model, dto, security, exceptions, filter, config, utils).
- **DTOs for REST contracts:** don't expose JPA entities directly.
- **Logging:** include context. Prefer `log.info("User {} borrowed map {}", userId, mapId)`.

## Roadmap

The hardening roadmap lives in `backend/SETUP_PLAN.md` (5 phases).
- **Phase 1** [done]: cleanup — remove dead services, fix docker-compose, update docs
- **Phase 2** [in progress]: per-service baseline — Spring profiles, Flyway, exception handler, structured logging, Dockerfile hardening, Actuator
- **Phase 3** [pending]: security — RS256 JWT, refresh tokens, rate limiting, security headers, `geodata-security-commons` shared module
- **Phase 4** [pending]: test coverage — 70%+ line coverage, Testcontainers, borrow flow integration test
- **Phase 5** [pending]: CI/CD — GitHub Actions, Dependabot, SonarCloud, semantic versioning

## Definition of done for any backend task

- Code compiles (`mvn clean compile`).
- Unit + repo tests pass (`mvn test`).
- Service starts cleanly with `SPRING_PROFILES_ACTIVE=dev`.
- `/actuator/health` returns `UP`.
- REST endpoints work (tested with Postman, curl, or Swagger UI).
- No secrets in git diff.
- Swagger documentation is accurate.

## When unsure

Ask Marius. Don't guess about:

- Whether a feature belongs in an existing service or needs a new one
- What the mobile app actually needs (ask, don't assume)
- Deployment strategy (local Docker vs production cloud)
- Whether to break backward compatibility with React frontend
- JWT/security policy decisions (token expiry, signing algorithm, etc.)

## Starting point

If this is the first session improving the backend:

1. Read `backend/SETUP_PLAN.md` — note current phase and next steps
2. Tell Marius which service you're focusing on
3. Describe what you want to improve (feature, fix, refactor, tests)
4. Review the existing code in that service
5. Make one focused change, test it, commit it
6. Stop and wait for feedback
