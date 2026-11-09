# Erplancers ERP

**Erplancers ERP** is an open-source, modular enterprise resource planning application built around a single Spring Boot backend and a React/TypeScript web client. It combines operational business workflows with finance and accounting, sales and procurement, inventory, CRM, configurable tax policies, reporting, and JPA-based business intelligence and analytics.

> **Development status:** actively developed. This repository is an evolving ERP platform, not a claim of jurisdiction-certified accounting or tax compliance. Review the current implementation, CI results, migrations, authorization rules, and deployment configuration before using real business data.

## Contents

- [Project overview](#project-overview)
- [Business capabilities](#business-capabilities)
- [Architecture](#architecture)
- [Technology stack](#technology-stack)
- [Quick start](#quick-start)
- [Docker Compose](#docker-compose)
- [Web application](#web-application)
- [REST API](#rest-api)
- [Tax policy engine](#tax-policy-engine)
- [Transactional workflows and accounting](#transactional-workflows-and-accounting)
- [Business intelligence and analytics](#business-intelligence-and-analytics)
- [Configuration](#configuration)
- [Testing and CI](#testing-and-ci)
- [Security and production readiness](#security-and-production-readiness)
- [Repository layout](#repository-layout)
- [Contributing](#contributing)

## Project overview

Erplancers ERP is designed as an extensible business application rather than a collection of unrelated CRUD screens:

- **Integrated business domains:** accounting, finance, sales, procurement, inventory, CRM, projects, support, communication, content, tax, surveys, and shared platform services.
- **Transactional business commands:** backend workflows coordinate document state, stock movements, invoice creation, payments, credit notes, and journal posting.
- **Configurable tax treatment:** date-effective, company-scoped or global policies; ordered tax components; exemption evidence; withholding; reverse charge; and compound calculations.
- **Accounting controls:** fiscal-year and fiscal-period validation, posting-account eligibility, balanced journal checks, and source-document posting flows.
- **Metadata-driven access:** registered entities can be discovered through a generic resource API with resource/action authorization checks.
- **Reporting and BI:** operational reports, dimension/measure queries, entity profiling, advanced analysis, OLAP cubes, and drill-down.
- **JPA-based analytics:** database-side aggregation, advanced statistical analysis, OLAP exploration, and drill-down without a separate distributed analytics runtime.
- **Modern web client:** React, TypeScript, Vite, TanStack Query, React Router, and PrimeReact.

## Business capabilities

Exact operations and exposed resources evolve with the codebase. The OpenAPI document is the authoritative runtime API catalogue.

| Area | Main responsibility |
| --- | --- |
| Sales | Quotes, orders, order lines, stock reservation, shipment, cancellation, and sales invoices |
| Procurement | Purchase orders, purchase details, partial/full receipt, supplier invoices, and stock receipt |
| Inventory | Product/warehouse balances, stock movements, adjustments, and warehouse transfers |
| Finance | Invoices, payments, credit notes, bank-statement import, and reconciliation |
| Accounting | Chart of accounts, journal entries, fiscal years/periods, posting controls, and financial reporting |
| Tax | Tax policies, tax calculations, exemptions/evidence, component-level tax snapshots, withholding, and reverse charge |
| CRM | Contacts, leads/opportunities, and sales quote conversion |
| Projects and support | Project/task and support-case lifecycle workflows |
| BI and analytics | Database-backed aggregates, advanced analysis, OLAP cubes, drill-down, saved reports, scheduled execution, and business rules |
| Platform | Authentication, authorization, metadata-driven resources, API documentation, and operational health |

## Architecture

The backend is intentionally deployed as a **single Spring Boot application**. Domain packages provide separation of concerns without requiring a business transaction to cross independently deployed microservices.

- Backend domain packages own business rules, authorization, persistence, and transaction boundaries.
- Spring Data JPA and Hibernate provide persistence.
- Flyway SQL migrations are under **src/main/resources/db/migration/**.
- The React application is developed in **frontend/**. The root Dockerfile builds it and copies its static output into the Spring Boot application.
- Docker Compose currently defines PostgreSQL, the backend, and the frontend web container.
- The current Compose file does **not** start Kafka. Provision and configure a broker separately if a deployment enables broker-backed integrations.
- BI and analytics use the ERP's JPA persistence layer and database queries; no Spark cluster, external analytics worker, or dataset catalogue is required.

### Main source directories

| Path | Contents |
| --- | --- |
| src/main/java/com/erplancers/erp/ | Backend application and domain packages |
| src/main/resources/db/migration/ | Flyway schema migrations |
| src/main/resources/ | Spring configuration and static resources |
| frontend/ | Main React/TypeScript application and independent module frontend builds |
| deploy/ | Deployment-specific operational configuration, when required |
| .github/workflows/ | GitHub Actions workflows |

## Technology stack

| Layer | Technology |
| --- | --- |
| Backend | Java, Spring Boot 4, Spring Security |
| Persistence | Spring Data JPA, Hibernate ORM 7, Flyway |
| Databases | H2 for convenient local development; PostgreSQL for container deployment |
| API | Spring MVC, Bean Validation, springdoc OpenAPI / Swagger UI |
| Frontend | React 19, TypeScript, Vite, React Router, TanStack Query, PrimeReact |
| Analytics | Spring Data JPA, database-side aggregation, in-process statistical analysis, OLAP and BI report execution |
| Integration and scheduling | Spring Kafka client, Spring Batch, Quartz, JobRunr, Apache Camel |
| Build and tests | Gradle Wrapper, JUnit Platform, Testcontainers, Vitest |
| Containers | Docker multi-stage build, Docker Compose, Nginx frontend container |

CI currently uses Temurin Java 21 and Node.js 22. The root Dockerfile currently uses Temurin 25 for its backend build/runtime stages and Node.js 22 for the frontend build. Dependency versions are maintained in build.gradle and frontend/package.json.

## Quick start

### Prerequisites

- Git
- JDK 21 for the same Java version used by CI
- Node.js 22 and npm for frontend development
- Docker Engine and the Compose plugin if using containers

### 1. Get the source

```bash
git clone https://github.com/selimrepo/erp.git
cd erp
```

### 2. Start the backend

Linux/macOS:

```bash
chmod +x ./gradlew
./gradlew bootRun
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

The default local configuration includes the H2 runtime driver and enables the H2 console for development. Spring configuration and environment variables determine the active database and schema behavior. Do not treat local Hibernate schema-generation settings as a production migration strategy.

### 3. Start the frontend

In a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Vite serves the development UI at **http://localhost:5173** and proxies **/api** requests to **http://localhost:8080** (see frontend/vite.config.ts).

### 4. Useful local URLs

| URL | Purpose |
| --- | --- |
| http://localhost:5173 | Vite development frontend |
| http://localhost:8080/swagger-ui/index.html | Swagger UI |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |
| http://localhost:8080/actuator/health | Application health |
| http://localhost:8080/h2-console | H2 console; development only |

Some endpoints and consoles depend on the active Spring profile and security configuration.

## Docker Compose

The root Compose file currently starts PostgreSQL, the Spring Boot backend, and the frontend container. The frontend is exposed on port **8088** by default.

### 1. Create local environment configuration

```bash
cp .env.example .env
```

Set strong, unique values for **POSTGRES_PASSWORD** and **ERP_REGISTRATION_ADMIN_TOKEN**. Do not commit .env or share these secrets. The checked-in .env.example contains placeholders, not production credentials.

### 2. Build and start

```bash
docker compose config
docker compose up --build -d
docker compose ps
```

Open **http://localhost:8088** unless ERP_HTTP_PORT has been changed.

### 3. Inspect logs and health

```bash
docker compose logs -f backend
docker compose logs -f frontend
docker compose ps
```

### 4. Stop the stack

```bash
docker compose down
```

Named PostgreSQL data volumes are retained. To remove the database volume as well:

```bash
docker compose down --volumes
```

**Warning:** the last command permanently removes the Compose-managed database data. Use it only when that data is intentionally disposable.

The current Compose file does not define a Kafka service. Although the backend includes Kafka integration dependencies and the example environment file retains a Kafka port variable, a broker is not created by docker compose up in this repository revision.

## Web application

The main React application includes these routes:

| Route | Purpose |
| --- | --- |
| /login | Sign in |
| /register | User registration |
| /register-admin | Initial/admin registration flow |
| / | Dashboard |
| /resources | Browse registered resources |
| /resources/:slug | Browse a resource |
| /resources/:slug/new | Create a resource record |
| /resources/:slug/:id/edit | Edit a resource record |
| /accounts | Chart of accounts |
| /bi/:slug | BI query for a resource |
| /reports | Reports |
| /insights | Insights |
| /olap | OLAP exploration |
| /business-rules | Business-rule workflows |

The main application protects business routes behind its authentication state. Backend authorization remains authoritative: a visible route or UI control does not grant permission to read or mutate a resource.

The frontend package also defines independent build scripts for CRM, sales, inventory, finance, accounting, procurement, projects, support, communication, content, events, surveys, tax, administration, and BI:

```bash
cd frontend
npm run build:modules
```

These module builds use separate Vite configurations; they are not a replacement for the main application build.

## REST API

The backend exposes JSON endpoints under **/api**. Start the backend and use Swagger UI or **/v3/api-docs** to inspect exact request/response schemas, validation constraints, and security requirements for the running revision.

### Main API groups

| Route | Purpose |
| --- | --- |
| /api/resources | Discover registered resources and use generic CRUD operations |
| /api/workflows | Execute transactional sales, procurement, inventory, finance, and accounting commands |
| /api/tax-policies | Manage and calculate generic tax policies |
| /api/reports | Reports |
| /api/analytics | Dashboard, profiles, aggregates, analysis, and analytics jobs |
| /api/bi | BI queries, OLAP catalogue, cubes, and drill-down |
| /api/business-rules | Business-rule schema, evaluation, and execution |
| /api/auth | Registration/authentication-related endpoints |
| /actuator/health | Health information |

Generic resource operations commonly use **/api/resources/{slug}** for paginated listing, item retrieval, creation, update, and deletion. Discover the resource slug and writable fields from the runtime resource catalogue/OpenAPI schema; do not assume every JPA entity is exposed.

### Workflow command examples

Workflow commands keep multi-step business operations on the backend instead of splitting them into unrelated client-side CRUD calls.

| Command | Endpoint |
| --- | --- |
| Create sales order | POST /api/workflows/sales/orders |
| Ship/cancel sales order | POST /api/workflows/sales/orders/{id}/ship and POST /api/workflows/sales/orders/{id}/cancel |
| Convert quote to order | POST /api/workflows/sales/quotes/{id}/convert |
| Create purchase order | POST /api/workflows/procurement/purchases |
| Receive outstanding purchase lines | POST /api/workflows/procurement/purchases/{id}/receive?storeId={storeId} |
| Receive selected purchase lines | POST /api/workflows/procurement/purchases/{id}/receive-lines?storeId={storeId} |
| Create stock adjustment/transfer | POST /api/workflows/inventory/stock-adjustments and POST /api/workflows/inventory/stock-transfers |
| Pay invoice | POST /api/workflows/finance/invoices/{id}/payments |
| Create credit note | POST /api/workflows/finance/invoices/{id}/credit-notes |
| Import/reconcile bank statement | POST /api/workflows/finance/bank-statements and POST /api/workflows/finance/bank-statement-lines/{id}/reconcile/{paymentId} |
| Post invoice/payment | POST /api/workflows/finance/invoices/{id}/post and POST /api/workflows/finance/payments/{id}/post |
| Determine posting accounts automatically | POST /api/workflows/finance/invoices/{id}/post-auto and POST /api/workflows/finance/payments/{id}/post-auto |
| Post journal | POST /api/workflows/accounting/journals |

These commands are authorization-checked. Exact request records are defined in ErpWorkflowRequests and exposed through OpenAPI.

### Authentication

Protected API routes use the application's Spring Security configuration. The React client keeps current credentials in its authentication state and sends an Authorization header. The current integration uses HTTP Basic credentials; use HTTPS outside local development, protect browser sessions/devices, and never put credentials in source control or logs.

## Tax policy engine

The tax module provides tenant-configurable, date-effective TaxPolicy records. A policy may be global or company/contact scoped and may constrain jurisdiction, transaction type, and tax category. Resolution uses the matching policy specificity, priority, and effective-date ordering implemented by the service. If no active policy matches, calculation fails rather than silently selecting an arbitrary rate.

### Policy management endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | /api/tax-policies | List policies |
| POST | /api/tax-policies | Create a policy |
| PUT | /api/tax-policies/{id} | Update a policy |
| POST | /api/tax-policies/{id}/deactivate | Deactivate without deleting history |
| POST | /api/tax-policies/calculate | Resolve and calculate a policy |

These endpoints require the tax-policy resource's READ or WRITE authorization action as appropriate.

### Supported calculation primitives

- **Standard tax:** percentage-based tax calculation.
- **Tax-inclusive and tax-exclusive prices:** amounts use the ERP's integer minor-unit convention; inclusive calculations reconcile rounding to the entered gross amount.
- **Multiple components:** ordered components are stored with their name, type, rate, sequence, amount, and optional input/output tax-account mappings.
- **Compound tax:** later compound components can calculate over the net amount plus previously accumulated tax components.
- **Exempt treatment:** a policy can require an exact exemption code and evidence type; sales and purchase document lines retain the submitted evidence reference.
- **Withholding:** withholding reduces the payable amount and is represented separately for accounting.
- **Reverse charge:** input and output tax are represented separately and can be posted to their mapped tax accounts.
- **Credit-note reversal:** source tax component, withholding, and reverse-charge amounts are proportionally reversed and retained on the credit-note invoice. Tax-correction credit notes require reason/evidence details.

Sales order and purchase order lines resolve and persist the selected policy and calculated tax values. A full purchase receipt transfers purchase-line tax snapshots to the supplier invoice. Optional accounting posting can be requested as part of sales order creation or purchase receipt so that the workflow and journal share the command transaction.

A policy's component account mappings are snapshotted on document tax components; later policy edits should not rewrite historical document tax. Configure appropriate chart-of-account mappings before enabling automatic posting.

### Example policy request

The following illustrates the shape of a policy with ordered components. Replace IDs, dates, scope, and account mappings with valid values in the target database:

```json
{
  "name": "Example domestic sales tax",
  "companyId": null,
  "jurisdictionCode": "TR",
  "transactionType": "SALE",
  "taxCategory": "STANDARD",
  "rate": 10,
  "validFrom": "2026-01-01",
  "validUntil": "2026-12-31",
  "priority": 10,
  "taxInclusive": false,
  "active": true,
  "treatment": "STANDARD",
  "components": [
    {
      "name": "VAT",
      "componentType": "STANDARD",
      "rate": 10,
      "sequence": 1,
      "outputTaxAccountId": 391,
      "inputTaxAccountId": 191
    }
  ]
}
```

The policy-level rate remains part of the policy model; component definitions provide detailed component calculation and accounting mappings. Ensure policy and component values are consistent with the intended tenant rules.

### Tax compliance boundary

This is a configurable calculation and accounting engine, **not a built-in catalogue of current statutory rates and not a certification of legal compliance**. It does not independently validate exemption certificates against government registries, determine legal eligibility for every transaction, or provide every jurisdiction's statutory return/reporting format. Withholding thresholds and exclusions, reverse-charge eligibility, special invoice wording, statutory rounding, tax periods, and filing obligations must be configured and reviewed for each jurisdiction by qualified tax/accounting professionals.

## Transactional workflows and accounting

Workflow commands are designed to keep related state changes in one Spring database transaction:

- Sales order creation validates the request, resolves line taxes, reserves stock, records stock movements, and can create an invoice. Shipment and cancellation are guarded by order/stock locking and terminal-state checks.
- Purchase receipt updates stock balances, records receipt movements, updates received quantities/status, and creates the supplier invoice when the purchase is fully received. Partial receipt is supported.
- Optional invoice posting during sales order creation or purchase receipt runs within the same workflow transaction. A failure in a participating database operation should roll back the complete command.
- Invoice payments and credit notes update source invoice balances atomically and enforce amount/state checks.
- Bank statement import uses idempotent statement-line processing and conservative matching; ambiguous matches are left for review.
- Stock transfers lock source/destination balance rows in stable order and record paired movements.
- Journal posting validates fiscal year/period ownership and open state, entry date, posting-enabled accounts, one-sided positive debit/credit lines, and debit/credit equality.

### Accounting setup

Before posting operational documents, configure the tenant's chart of accounts and account-determination rules. The posting API can accept explicit accounts or use configured determination rules. Typical mappings include:

- Sales invoice: debit receivables; credit revenue and output tax.
- Purchase invoice: debit inventory/expense and input tax; credit payables.
- Customer payment: debit bank/cash; credit receivables.
- Supplier payment: debit payables; credit bank/cash.
- Withholding and reverse-charge entries: configure dedicated accounts and mappings consistent with the jurisdiction and the organization's accounting policy.

Journal and source-document posting should be treated as a controlled accounting operation. Corrections to posted journals should use reversal entries rather than editing posted history.

See [docs/ERP_BUSINESS_WORKFLOWS.md](docs/ERP_BUSINESS_WORKFLOWS.md) for more workflow detail.

## Business intelligence and analytics

The BI and analytics capabilities run inside the Spring Boot application and use the existing JPA persistence model. Analytical aggregation is delegated to the relational database where possible; advanced statistical analysis operates on bounded result sets returned by JPA queries. No Spark runtime, distributed compute cluster, external dataset catalogue, or separate analytics deployment is part of this project.

The current capabilities include:

- **Database-backed aggregation:** grouped dimensions, measures, filters, ordering, and bounded result sets.
- **Advanced analysis:** numeric field profiling, descriptive statistics, quartiles, outlier counts, and correlation matrices. Aggregate query results can also be analyzed.
- **OLAP exploration:** cube/catalogue discovery, multidimensional aggregation, and drill-down through the BI API.
- **Saved BI reports:** persist report definitions, edit and delete saved reports, and share reports with users or roles.
- **Scheduled execution:** enqueue due reports, execute queued work in the application, retain execution status/results, and optionally deliver completed reports by email.
- **React BI workspace:** query resources, define calculated measures, manage saved reports and schedules, queue executions, and monitor execution status.

All data access remains subject to the application's resource/action authorization and the relevant JPA/database access rules. Keep query dimensions, metrics, limits, and result sizes bounded; validate tenant and row-level access whenever adding a new analytics endpoint. See the `/api/analytics` and `/api/bi` API groups in Swagger UI for the runtime contract.

Kafka-backed notifications, where enabled elsewhere in the application, require a separately provisioned broker; the repository's current Compose stack does not provide one.

## Configuration

Common environment/configuration points include:

| Variable/property | Purpose |
| --- | --- |
| POSTGRES_PASSWORD | PostgreSQL password in Compose |
| ERP_REGISTRATION_ADMIN_TOKEN | Protects the initial/admin registration flow |
| ERP_HTTP_PORT | Host port for the frontend container; defaults to 8088 |
| SPRING_PROFILES_ACTIVE | Selects the Spring profile |
| SPRING_DATASOURCE_URL | Database JDBC URL |
| SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD | Database credentials |
| SPRING_JPA_HIBERNATE_DDL_AUTO | Hibernate schema-generation behavior |
| spring.flyway.enabled / ERP_FLYWAY_ENABLED | Flyway migration enablement |


The application supports standard Spring Boot externalized configuration. Confirm effective property values for the active profile before deployment. Never commit secrets.

## Testing and CI

### Backend

From the repository root:

```bash
./gradlew compileJava
./gradlew test
./gradlew bootJar
```

The test suite uses JUnit Platform. Testcontainers-based PostgreSQL tests may be skipped when Docker is unavailable, according to each test's configuration. A green local suite without Docker does not prove that every PostgreSQL migration/integration test ran.

### Frontend

```bash
cd frontend
npm install
npm test
npm run build
npm run build:modules
```

### GitHub Actions

The Build and Test workflow runs on pushes to main, feature/**, and chore/**, pull requests targeting main, and manual dispatch. It compiles and tests the backend, builds the Spring Boot JAR, installs frontend dependencies, runs frontend tests, builds the main frontend, and builds the independent module frontends.

Check the workflow run for the exact commit being reviewed; a previous green run does not establish that a later commit passed.

## Security and production readiness

Before exposing an instance to users or loading production data:

- Replace example secrets with unique, high-entropy values and use a secret manager where available.
- Use HTTPS through a trusted reverse proxy/load balancer; do not send Basic credentials over plaintext HTTP.
- Use PostgreSQL with least-privilege database credentials, tested backups, restore drills, and a documented migration/rollback plan.
- Review Hibernate DDL settings and Flyway migration behavior together. Development ddl-auto=update settings are not a substitute for reviewed schema migrations.
- Disable or tightly restrict the H2 console outside local development.
- Review registration/bootstrap flows and rotate or disable the initial admin token after setup.
- Verify resource-level, action-level, tenant, and row-level authorization across every service and nested relationship.
- Configure and test account determination, fiscal periods, tax policies, and tax component mappings before posting real documents.
- Review tax treatments with qualified professionals for every jurisdiction in which the system is used.
- Secure Actuator and additional operational endpoints; expose only the health/metrics information needed by monitoring.
- Test concurrency, idempotency, failure recovery, and PostgreSQL behavior using the same database engine/version planned for deployment.

## Repository layout

```text
.
├── .github/workflows/       # GitHub Actions
├── deploy/                  # Deployment configuration examples (if present)
├── docs/                    # Business workflow contracts and project docs
├── frontend/                # React/TypeScript application and module builds
├── gradle/                  # Gradle wrapper support
├── src/main/java/           # Spring Boot backend and domain modules
├── src/main/resources/      # Application config, static assets, Flyway migrations
├── build.gradle
├── docker-compose.yml
├── Dockerfile
└── gradlew
```

## Contributing

1. Fork the repository and create a focused branch.
2. Keep business operations in backend workflow services; do not emulate stock, settlement, or journal transactions as unrelated frontend CRUD calls.
3. Add or update backend and frontend tests for behavior changes. Include PostgreSQL/Testcontainers coverage for database-specific migrations and constraints where appropriate.
4. Run the relevant Gradle and frontend checks locally.
5. Open a pull request describing the behavior, transaction boundaries, migration impact, security/authorization implications, and test evidence.

When reporting a defect, include the affected endpoint or UI route, expected and actual behavior, reproduction steps, and sanitized logs. Never attach credentials, tokens, personal data, or production records.
