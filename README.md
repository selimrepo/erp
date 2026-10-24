# Erplancers ERP

An open-source, modular ERP application built with Spring Boot, Spring Data JPA, Hibernate, and a React web UI. The project combines operational CRUD workflows with reporting, OLAP/BI, business rules, and an optional embedded analytics job engine.

> **Project status:** actively developed. Treat the repository and its CI as the source of truth for current implementation status. Review security, persistence, and deployment configuration before exposing an instance to real users or production data.

## Contents

- [Highlights](#highlights)
- [Architecture](#architecture)
- [Technology stack](#technology-stack)
- [Run locally](#run-locally)
- [Run with Docker Compose](#run-with-docker-compose)
- [Web application](#web-application)
- [REST API](#rest-api)
- [Embedded analytics jobs](#embedded-analytics-jobs)
- [Tests and CI](#tests-and-ci)
- [Configuration and security notes](#configuration-and-security-notes)
- [Contributing](#contributing)

## Highlights

- **Modular ERP domain:** accounting, finance, sales, procurement, inventory, CRM, projects, support, communication, content, tax, surveys, and platform/common services.
- **Dynamic resource API:** registered entities can be discovered and accessed through a shared CRUD API with resource- and action-level policy checks.
- **Accounting and reporting:** account and journal-entry models plus fiscal-period, ledger, trial-balance, cost-center, and profit-center reporting services.
- **Business intelligence:** configurable dimension/measure queries, aggregate reports, entity profiling, advanced analysis, and OLAP cube/drill-down endpoints.
- **Business rules:** schema discovery and rule evaluation/execution endpoints for supported registered resources.
- **Analytics jobs:** an optional embedded Spark-based engine, job worker, metadata/job persistence, and Kafka outbox notifications. The engine is disabled by default until its dataset catalogue and database grants are configured.
- **Web UI:** React, TypeScript, Vite, React Router, TanStack Query, and PrimeReact components for login/registration, resources, dashboards, reports, BI, OLAP, analytics, and business rules.
- **Operational foundations:** Spring Security, OpenAPI/Swagger UI, Actuator health checks, PostgreSQL support, Docker images, and Docker Compose.

## Architecture

The application is currently organized as a **single Spring Boot backend** rather than independently deployed domain microservices.

- `src/main/java/com/erplancers/erp` — backend application and domain packages.
- `frontend/` — React/TypeScript single-page application and Nginx configuration.
- `deploy/` — example configuration for the optional embedded analytics engine.
- `src/main/resources/db/migration/` — versioned SQL migrations.
- `.github/workflows/` — GitHub Actions build and test workflows.

The backend exposes JSON APIs under `/api`. The frontend sends requests to the same origin; the production Nginx configuration proxies `/api/` to the backend container.

## Technology stack

| Area | Technology |
| --- | --- |
| Backend | Java 21 runtime/build image, Spring Boot 4, Spring Security |
| Persistence | Spring Data JPA, Hibernate ORM 7, H2 for the default local profile/configuration, PostgreSQL for deployment |
| Analytics | Apache Spark SQL and MLlib; Kafka clients and Spring Kafka |
| Jobs and integration | Spring Batch, Quartz, JobRunr, Apache Camel |
| API documentation | springdoc OpenAPI / Swagger UI |
| Frontend | React 19, TypeScript, Vite, React Router, TanStack Query, PrimeReact |
| Deployment | Docker, Docker Compose, Nginx |
| Build and tests | Gradle Wrapper, JUnit Platform, Vitest |

Version details are maintained in `build.gradle`, `settings.gradle`, and `frontend/package.json`.

## Run locally

### Prerequisites

- JDK 21
- Node.js and npm compatible with the versions used in CI
- Git

### Start the backend

From the repository root:

```bash
./gradlew bootRun
```

On Windows, use:

```powershell
.\gradlew.bat bootRun
```

The default `application.properties` enables the H2 console and configures Hibernate schema generation for local development. Do not use the development schema settings as a production migration strategy.

Useful URLs when the application is running:

- API documentation: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI document: `http://localhost:8080/v3/api-docs`
- Health endpoint: `http://localhost:8080/actuator/health`
- H2 console (development only): `http://localhost:8080/h2-console`

Endpoint availability can depend on the active Spring profile and configuration.

### Start the frontend

In another terminal:

```bash
cd frontend
npm ci
npm run dev
```

Vite serves the development UI. Check `frontend/vite.config.ts` for its development proxy and server settings. For a production-style static build:

```bash
npm test
npm run build
```

Run the commands from the `frontend/` directory.

## Run with Docker Compose

The Compose stack defines PostgreSQL, Kafka, the backend, and the Nginx-served frontend.

1. Create a local environment file and replace the example secrets:

   ```bash
   cp .env.example .env
   ```

2. Set strong, unique values for `POSTGRES_PASSWORD` and `ERP_REGISTRATION_ADMIN_TOKEN` in `.env`. Do not commit `.env`.
3. Build and start the stack:

   ```bash
   docker compose up --build -d
   ```

4. Follow service logs and check container health:

   ```bash
   docker compose ps
   docker compose logs -f backend
   ```

5. Open the UI at `http://localhost:8088` by default. Override `ERP_HTTP_PORT` in `.env` to change the host port.

The Compose configuration persists PostgreSQL data, Kafka data, and analytics artifacts in named volumes. Stop the stack with `docker compose down`; this preserves named volumes. To delete persisted data as well, use `docker compose down --volumes` only when you intentionally want to destroy it.

The Compose file publishes Kafka on `localhost:9092` by default for host tools; containers communicate with the broker at `kafka:29092`.

## Web application

The frontend includes these main routes:

| Route | Purpose |
| --- | --- |
| `/login` | Sign in |
| `/register` | User registration |
| `/register-admin` | Initial/admin registration flow |
| `/` | Dashboard |
| `/resources` | Browse registered resources |
| `/resources/:slug` | Browse a resource |
| `/resources/:slug/new` | Create a resource |
| `/resources/:slug/:id/edit` | Edit a resource |
| `/bi/:slug` | BI query for a resource |
| `/reports` | Reports |
| `/analytics` | Analytics tools and jobs |
| `/olap` | OLAP exploration |
| `/business-rules` | Business-rule workflows |

Routes that display business data require authentication. Whether an authenticated user can access a resource or perform an action is controlled by backend authorization policies.

## REST API

The API is documented at Swagger UI when the backend is running. Main route groups include:

| Route | Purpose |
| --- | --- |
| `/api/resources` | Discover resources and perform generic CRUD operations |
| `/api/reports` | Resource and module reports |
| `/api/analytics` | Dashboard, profiles, aggregates, advanced analysis, and analytics jobs |
| `/api/bi` | Dimension/measure queries, BI analysis, OLAP catalog, cubes, and drill-down |
| `/api/business-rules` | Business-rule schema, evaluation, and execution |
| `/api/auth` | Registration endpoints |
| `/actuator/health` | Application health |

Typical generic resource operations use `/api/resources/{slug}`: list with pagination, fetch an item, create, update, and delete. Use the OpenAPI schema and the resource metadata endpoint to discover the fields accepted for a specific entity. Do not assume every domain entity is exposed under a particular slug without checking the runtime resource catalogue.

The API uses HTTP Basic authentication for protected endpoints. The React UI stores the current credentials in its authentication state and sends an Authorization header with API requests. Use HTTPS in any non-local environment.

## Embedded analytics jobs

The embedded Spark engine is **disabled by default** (`ANALYTICS_ENGINE_ENABLED=false`). Do not enable it until you have reviewed the dataset mappings and database permissions.

To configure it:

1. Copy the example file:
   ```bash
   cp deploy/analytics-datasets.properties.example deploy/analytics-datasets.properties
   ```
2. Replace every placeholder with valid registered JPA entity names and JPA attribute paths for the deployed model.
3. Review the dataset scope relation and tenant/row-level access grants. Never allow a request to choose arbitrary entities or attributes.
4. Set `ANALYTICS_ENGINE_ENABLED=true` in `.env` and recreate the backend.

The backend mounts `deploy/` read-only at `/run/config`. Keep deployment-specific `analytics-datasets.properties` out of version control. The current JPA extraction path is paged and capped at one million rows per job; large datasets should use bounded date windows or approved aggregate/projection entities. More details are in [deploy/README.md](deploy/README.md).

Kafka is used for queued-job notifications via a durable outbox; the job store/database remains the source of truth. A running Kafka broker alone does not configure or enable the analytics engine.

## Tests and CI

Run the backend test suite from the repository root:

```bash
./gradlew test
```

Run frontend unit tests and type-check/build:

```bash
cd frontend
npm ci
npm test
npm run build
```

The GitHub Actions workflows compile and test the backend and run the frontend checks. A local pass is useful but does not replace checking the CI status for the exact commit being reviewed.

## Configuration and security notes

- **Secrets:** keep `.env`, production passwords, admin bootstrap tokens, and deployment-specific analytics mappings out of Git.
- **Admin bootstrap:** protect the admin registration token and rotate it after the initial setup according to your deployment process.
- **Database:** use PostgreSQL for deployment, configure backups and least-privilege database users, and establish a deliberate schema migration/rollback strategy before production use.
- **Analytics permissions:** configure dataset mappings and row/tenant scope explicitly before enabling Spark jobs.
- **Transport:** terminate TLS at a trusted reverse proxy or load balancer for non-local deployments.
- **Access control:** API policy checks are not a substitute for verifying row-level and tenant isolation in every service and nested relationship. Review authorization behavior and test it with multiple users/tenants before storing sensitive data.
- **Development defaults:** H2 console and Hibernate `ddl-auto` settings are intended for local development, not production.
- **Observability:** Actuator health and Prometheus registry support are dependencies; configure and secure any additional operational endpoints deliberately.

## Contributing

1. Create a focused branch for your change.
2. Add or update backend and/or frontend tests for behavior changes.
3. Run the relevant Gradle and frontend checks.
4. Open a pull request with a summary, test evidence, and any configuration or migration notes.

Please report reproducible defects with the endpoint or UI route, expected behavior, actual result, and relevant sanitized logs. Never include credentials, tokens, or production records in issues.
