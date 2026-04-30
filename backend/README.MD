# Geodata Backend

Spring Boot 3.5 microservices backend for the Geodata map-borrowing platform.

## Architecture

Three microservices, one Postgres database (schema-per-service):

| Service            | Port  | Schema   | Purpose                                                 |
|--------------------|-------|----------|---------------------------------------------------------|
| `identity-service` | 8010  | `auth`   | Auth, JWT issuance, user/role management                |
| `map-service`      | 8020  | `map`    | Map catalog, availability state, search                 |
| `borrow-service`   | 8030  | `borrow` | Borrow request lifecycle (request → approve → return)   |

Inter-service calls are synchronous REST via `RestTemplate` with JWT pass-through.

The frontend (React) and mobile app (Flutter) live in sibling directories
(`../frontend/`, `../mobile/`). They consume the REST APIs documented via
Swagger / springdoc-openapi.

## Repository layout

```
backend/
├── identity-service/      # Spring Boot module
├── map-service/           # Spring Boot module
├── borrow-service/        # Spring Boot module
├── database-scripts/      # SQL utilities
├── pgadmin4/              # pgAdmin pre-configured server JSON
├── docker-compose.yml     # Local dev infra (Postgres, pgAdmin, Mailpit, SonarQube)
├── pom.xml                # Parent POM (multi-module)
├── SETUP_PLAN.md          # Hardening roadmap (5 phases)
└── README.MD              # This file
```

## Running locally

### Prerequisites

- Java 21 (target after Phase 2; current source is Java 17)
- Maven 3.9+
- Docker + Docker Compose
- A Supabase Postgres URL **or** the local Postgres container (see below)

### Start the local infrastructure

```bash
cd backend
docker-compose up -d
```

This starts:
- Postgres on `localhost:5432` (user: `postgres`, password: `admin`)
- pgAdmin on `http://localhost:4040` (login: `admin@gmail.com` / `admin`)
- Mailpit on `http://localhost:8025`
- SonarQube on `http://localhost:9000`

### Run a service

Each service expects environment variables for DB credentials and JWT secret.

**Quick setup:**

```bash
cp .env.example .env.local
# Edit .env.local with your values (or use defaults for local dev)
source .env.local
```

Then start a service:

```bash
mvn -pl identity-service spring-boot:run
mvn -pl map-service spring-boot:run
mvn -pl borrow-service spring-boot:run
```

**Note:** Each service auto-selects its schema (`auth` for identity, `map` for map-service, `borrow` for borrow-service) based on the `SUPABASE_JDBC_URL` and internal configuration.

### Build everything

```bash
mvn clean install
```

## Testing

```bash
mvn test                                  # unit tests across all modules
mvn -pl identity-service test             # unit tests for one service
mvn verify                                # unit + integration + JaCoCo aggregate
```

## API documentation

Once a service is running, Swagger UI is available at:
`http://localhost:<port>/swagger-ui/index.html`
OpenAPI JSON: `http://localhost:<port>/v3/api-docs`

## Health & observability

- `GET /actuator/health` — liveness/readiness probe
- `GET /actuator/info` — build info

## Deployment

Kubernetes manifests live in `../k8s/`:

```bash
kubectl apply -f ../k8s/
```

The manifests deploy each service, a Postgres instance, and an nginx ingress
that routes:

- `/api/auth`, `/api/users`, `/api/admin`, `/api/home` → identity-service
- `/api/maps` → map-service
- `/api/borrows` → borrow-service

Container images are published to GitHub Container Registry under
`ghcr.io/mariusss11/{service}:{tag}` (CI pipeline planned for Phase 5).

## Project status

This backend is in active hardening. See `SETUP_PLAN.md` for the 5-phase
roadmap and current progress. TL;DR:

1. Cleanup (remove dead services & infra)
2. Per-service standards baseline (profiles, Flyway, exception handling, MDC, Actuator)
3. Security pass (RS256 JWT + refresh, rate limiting, security headers)
4. Test coverage (unit, repo, controller, full borrow-flow integration)
5. CI/CD (GitHub Actions, Dependabot, SonarCloud)
