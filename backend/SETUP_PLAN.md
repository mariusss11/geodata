# Geodata Backend — Hardening Roadmap

5-phase plan to bring the 3-service backend (`identity-service`, `map-service`,
`borrow-service`) up to production-quality standards. Each phase leaves the
system working — we ship phase by phase, not big-bang.

The original full plan is at
`~/.claude/plans/replicated-sleeping-cupcake.md`. This file tracks live status
and acts as the source of truth for what's done, in progress, or pending.

---

## Phase 1 — Cleanup &nbsp;&nbsp;`[done]`

**Goal:** A working 3-service system with no references to deleted services
or Kafka. Documentation matches reality.

- [x] Delete `backend/review-service/`
- [x] Delete `k8s/review-service.yaml`, `k8s/kafka.yaml`, `k8s/zookeeper.yaml`,
      `k8s/item-service.yaml`, `k8s/client-service.yaml`
- [x] Remove `review-service` from `backend/pom.xml` modules
- [x] Strip Kafka, Zookeeper, kafka-ui from `backend/docker-compose.yml`
- [x] Strip dead services + Kafka from root `docker-compose.yml`;
      add `map-service` entry
- [x] Update `k8s/identity-service.yaml` (drop `CLIENT_SERVICE`, replace
      `ITEM_SERVICE` → `MAP_SERVICE`, drop Kafka env)
- [x] Update `k8s/borrow-service.yaml` (drop client/item refs, drop Kafka,
      add `MAP_SERVICE`)
- [x] Add new `k8s/map-service.yaml`
- [x] Update `k8s/ingress.yaml` (drop `/api/items`, `/api/client`, `/api/review`;
      add `/api/maps`, `/api/users`, `/api/admin`, `/api/home`)
- [x] Rewrite `backend/.claude/CLAUDE.md` to reflect 3-service reality
- [x] Write a real `backend/README.MD`
- [x] Create this `SETUP_PLAN.md`
- [x] Verify: `mvn clean compile` passes for all 3 services
- [x] Verify: both docker-compose files validate (`docker compose config`)
- [x] Verify: no production refs to dropped services or Kafka remain
      (only intentional history notes in `CLAUDE.md` / `SETUP_PLAN.md`)

---

## Phase 2 — Per-service standards baseline &nbsp;&nbsp;`[done]`

**Goal:** Each service follows the same patterns. Dev/prod separated via
Spring profiles. Schemas managed by Flyway (no more `ddl-auto: update`).

Applied per service in this order: **identity → map → borrow**.

- [x] **2.1 Spring profiles**
  - `application.yml` (shared base, env-var driven)
  - `application-dev.yml` (localhost URLs, DEBUG logging, permissive CORS)
  - `application-prod.yml` (env-only, INFO logging, JSON logs, strict CORS)
  - `application-test.yml` (Testcontainers placeholder)
- [x] **2.2 Flyway migrations**
  - Added `flyway-core` + `flyway-database-postgresql` deps
  - Created `src/main/resources/db/migration/V1__init.sql` per service
  - Set `ddl-auto: validate` in all profiles
- [x] **2.3 Global exception handler**
  - `@RestControllerAdvice` returning a structured `ErrorResponse` record
  - No stack traces leak to clients
  - Explicit handlers for each domain exception
- [x] **2.4 Structured logging + MDC**
  - `logback-spring.xml` per service (plain in dev, JSON via Logstash in prod)
  - `MDCFilter` generating/propagating `X-Request-ID`
  - `RestTemplate` interceptor forwards the header on inter-service calls
- [x] **2.5 Dependency alignment**
  - `jjwt` → `0.12.6` on all 3 services (via parent dependencyManagement)
  - `springdoc-openapi-starter-webmvc-ui` → `2.8.5` on all 3 services
  - Java source → `21`, Spring Boot → `3.5.1`
  - Shared versions moved to parent `pom.xml` `<properties>` + `<dependencyManagement>`
  - Parent pom now inherits from `spring-boot-starter-parent` (services inherit from parent)
- [x] **2.6 Dockerfile hardening**
  - Multi-stage: `maven:3.9-eclipse-temurin-21` builder → `eclipse-temurin:21-jre-alpine` runtime
  - Non-root user (`USER 1000:1000`)
  - `HEALTHCHECK` hitting `/actuator/health`
  - `ENV SPRING_PROFILES_ACTIVE=prod` default
  - Build context is `backend/` root (enables parent pom access)
- [x] **2.7 Actuator**
  - `spring-boot-starter-actuator` dep on all 3 services
  - Expose `health`, `info` always; `prometheus` in prod only

**Phase-2 verification:**
- [x] `mvn clean compile` passes for all 3 services — BUILD SUCCESS

**Remaining manual verification (needs running DB):**
- [ ] `SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run` starts cleanly,
  `/actuator/health` returns `UP`
- [ ] `SPRING_PROFILES_ACTIVE=prod` without env vars → fail-fast (good)
- [ ] `curl localhost:<port>/v3/api-docs` returns OpenAPI JSON
- [ ] Flyway runs cleanly against a fresh schema

---

## Phase 3 — Security pass &nbsp;&nbsp;`[pending]`

**Goal:** Real production security. No more 10-day HMAC tokens or wildcard CORS.

- [ ] **3.1 JWT redesign — RS256 + refresh tokens**
  - identity holds private key; map/borrow hold only public key
  - access token 15 min; refresh token 7 days, rotated on use, stored & revocable in DB
  - new `POST /api/auth/refresh` endpoint
- [ ] **3.2 CORS profiles** — dev permissive, prod from `${CORS_ALLOWED_ORIGINS}` (no default)
- [ ] **3.3 Rate limiting** (Bucket4j) on `/login`, `/register`, borrow create
- [ ] **3.4 Security headers** — HSTS (prod), nosniff, frame-deny, CSP, no-referrer
- [ ] **3.5 Shared `geodata-security-commons` module** — extract `JwtUtils`,
      `JwtAuthFilter`, `MDCFilter`, `GlobalExceptionHandler`, `ErrorResponse`,
      `PagedResponse` from each service into one module
- [ ] **3.6 Secrets handling** — `.env.example`, gitignored `.env`,
      audit `git log -p` for any historic secret commits
- [ ] **3.7 Input validation audit** — `@Valid` on every `@RequestBody`,
      Bean Validation on all DTO fields

---

## Phase 4 — Test coverage &nbsp;&nbsp;`[pending]`

**Goal:** Confidence in changes. Borrow flow covered end-to-end.

- [ ] **4.1 Unit tests (Mockito)** — 70%+ line coverage on service layers
- [ ] **4.2 Repository tests (`@DataJpaTest` + Testcontainers Postgres)** — validates Flyway too
- [ ] **4.3 Controller tests (`@WebMvcTest`)** — auth rules + status codes
- [ ] **4.4 Borrow-flow integration test** — register → login → create map → borrow → approve → return
- [ ] **4.5 JaCoCo aggregate** — fail builds <70% line coverage

---

## Phase 5 — CI/CD &nbsp;&nbsp;`[pending]`

**Goal:** Automated test/scan/build/publish pipeline.

- [ ] **5.1 GitHub Actions**
  - `backend-ci.yml`: PR + push-to-main → matrix test per service, integration test, SonarCloud, OWASP dep-check
  - `backend-publish.yml`: push-to-main + version tags → build & push images to GHCR
- [ ] **5.2 Dependabot** — weekly Maven, grouped minor/patch
- [ ] **5.3 SonarCloud** — quality gate on new code
- [ ] **5.4 Semantic versioning** — git tags drive image tags
