# Erplancers ERP — Extended Developer Tutorial

## 1. Overview

Erplancers ERP is a metadata-driven modular monolith built around:

- Java and Spring Boot
- Spring Data JPA
- Hibernate ORM
- PostgreSQL
- React + TypeScript + Vite
- PrimeReact / PrimeIcons
- TanStack Query
- Spring Security
- Springdoc OpenAPI
- Batch, Quartz, Camel and JobRunr integrations

The architecture keeps business logic in the backend while exposing a generic REST/CRUD layer and analytical services to the React application.

```text
React / PrimeReact
       |
       | REST / JSON
       v
Spring Boot
       |
       +-- Dynamic CRUD
       +-- Domain Services
       +-- Reporting
       +-- OLAP
       |
       v
Spring Data JPA
       |
       v
Hibernate
       |
       v
PostgreSQL
```

## 2. Repository structure

Important locations:

```text
src/main/java/       Backend
src/test/java/       Backend tests
frontend/src/        React application
.github/workflows/   CI
build.gradle        Gradle configuration
docker-compose.yml  Container deployment
DEPLOYMENT.md       Deployment guide
```

Start by locating the application class, controllers, services and repositories for one domain module. Following one complete request is the fastest way to understand the architecture.

## 3. Local development

Clone the repository:

```bash
git clone https://github.com/erplancers/erp.git
cd erp
```

Compile:

```bash
./gradlew compileJava
```

Run tests:

```bash
./gradlew test
```

Build:

```bash
./gradlew build
```

Run Spring Boot:

```bash
./gradlew bootRun
```

For Windows use `gradlew.bat` instead of `./gradlew`.

The build currently uses Spring Boot 4.0.7 and Hibernate ORM 7.2.19.Final. See `build.gradle` for the authoritative dependency versions.

## 4. Docker development

The complete stack can be started with:

```bash
cp .env.example .env
docker compose up -d --build
```

Open:

```text
http://localhost:8088
```

Useful commands:

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f frontend
docker compose down
```

PostgreSQL data is persisted in the `postgres_data` volume.

## 5. Dynamic CRUD architecture

The generic CRUD API is centered around resource slugs:

```text
GET    /api/resources/{slug}
GET    /api/resources/{slug}/metadata
GET    /api/resources/{slug}/{id}
GET    /api/resources/{slug}/{id}/metadata

POST   /api/resources/{slug}
PUT    /api/resources/{slug}/{id}
PATCH  /api/resources/{slug}/{id}
DELETE /api/resources/{slug}/{id}
```

The request flow is approximately:

```text
RestCrudController
       |
       v
DynamicCrudService
       |
       +--> CrudRegistry
       +--> entity metadata
       +--> repository
       |
       v
JPA / Hibernate
```

This lets the React application build generic resource lists and editors without a separate controller for every entity.

## 6. Metadata-driven forms

The metadata endpoint is the foundation of dynamic editing.

A resource metadata response can describe:

- scalar fields
- references
- collections
- selectable reference values
- nested collections
- current values
- selected references
- nested options

A frontend editor can therefore render an entity from metadata instead of hard-coding every property.

This is especially valuable in an ERP where the number of entities and fields grows continuously.

## 7. Aggregate root editing

ERP entities frequently contain child collections.

Example:

```text
SalesOrder
 |
 +-- customer
 |
 +-- lines
       |
       +-- product
       +-- quantity
       +-- price
```

The dynamic CRUD layer supports nested child fields using:

```text
nested.<collection>.<index>.<field>
```

Example:

```text
nested.lines.0.product
nested.lines.0.quantity
nested.lines.0.price
nested.lines.1.product
nested.lines.1.quantity
nested.lines.1.price
```

Existing children can contain:

```text
_id
```

A child can be marked for deletion:

```text
nested.lines.0._delete=true
```

This allows one aggregate save to update the root and its submitted child rows.

### New child rows

New rows need stable client-side keys before they have database IDs. The React aggregate editor therefore uses an internal key rather than relying on an empty database ID.

This prevents DataTable row collisions when multiple unsaved children exist.

### Delete and restore

The UI keeps deleted existing children visible as pending deletions and provides Restore before the aggregate is saved.

This produces a safer editing workflow:

```text
Add -> Edit -> Delete -> Restore -> Save
```

## 8. JPA relationships

When adding a domain entity, carefully model relationships.

For example:

```java
@ManyToOne
private Contact customer;
```

or an aggregate collection:

```java
@OneToMany
private List<OrderDetail> lines;
```

Consider:

- ownership
- cascade behavior
- orphan removal
- lazy/eager loading
- identifier type
- collection size
- query requirements

Avoid making large collections eager merely for convenience.

## 9. OLAP architecture

The OLAP system derives analytical structure from the JPA/entity metadata.

```text
JPA Metamodel
      |
      v
EntityDataAnalysisService
      |
      v
Field / relationship catalog
      |
      v
JpaOlapCubeService
      |
      v
Query execution
      |
      v
React OLAP page
```

This allows the application to discover dimensions and relationships instead of requiring a manually maintained cube definition for every entity.

## 10. OLAP dimensions

Scalar fields can be dimensions:

```text
status
entryDate
description
```

Entity relationships can also be dimensions:

```text
customer
lines
```

Nested properties can be dimensions:

```text
customer.name
lines.productName
```

A useful entity graph is:

```text
Order
 |
 +-- customer
 |     |
 |     +-- name
 |
 +-- lines
       |
       +-- productName
```

This is why relationship discovery must expose terminal relationships as well as scalar properties.

## 11. Aggregate relationships in the OLAP catalog

A frequent OLAP bug is to recursively discover only scalar fields. If `customer` is a `@ManyToOne`, the catalog can accidentally omit the relationship itself.

The field-analysis layer should expose:

- singular entity relationships such as `@ManyToOne` and `@OneToOne`
- entity collections such as `@OneToMany` and `@ManyToMany`
- nested scalar properties reached through those relationships

Conceptually:

```text
Invoice
 |
 +-- customer       <- dimension
 |     |
 |     +-- name     <- nested dimension
 |
 +-- lines          <- dimension
       |
       +-- product  <- nested dimension
```

The traversal depth must remain bounded to prevent uncontrolled entity-graph expansion.

## 12. OLAP measures

Typical measures include:

- count
- sum
- average
- minimum
- maximum
- count distinct

A count-distinct measure can be represented as:

```text
countDistinct:<field>
```

Be careful when using Java streams. After:

```java
.map(EntityDataAnalysisService.FieldSchema::name)
```

the stream contains `String` values. Therefore:

```java
.forEach(field -> measures.add("countDistinct:" + field));
```

is correct, while:

```java
.forEach(field -> measures.add("countDistinct:" + field.name()));
```

is not.

This distinction caused a real CI compilation regression and is covered by the current production code.

## 13. OLAP query model

A query combines:

- cube
- dimensions
- measures
- filters
- ordering
- limit
- pivot
- grand totals
- rollup

Think of it as:

```text
Dimensions -> GROUP BY
Measures   -> SELECT aggregates
Filters    -> WHERE
Order      -> ORDER BY
Pivot      -> presentation/grouping transformation
Totals     -> aggregate summary rows
Rollup     -> hierarchical grouping
```

Example:

```text
Cube: Invoice

Dimensions:
  customer.name
  status

Measures:
  count
  sum:amount

Filter:
  status = POSTED

Options:
  order by amount DESC
  limit 100
  grand totals = true
```

## 14. Auto-report generation

The automatic report workflow discovers relationships and builds a report from them:

```text
Fact cube
   |
   v
Relationship discovery
   |
   v
Choose dimensions
   |
   v
Choose measure
   |
   v
Generate report
   |
   v
Display result
```

This is useful for exploratory ERP analysis because a developer or user can navigate relationships without manually designing every report.

## 15. OLAP UI scalability

A high-field ERP entity can expose hundreds of selectable fields. Rendering every selection as a large visible tag list makes the page unusable.

The current OLAP UI therefore uses:

- compact selected-value display
- selected-count summaries
- bounded selected-item lists
- scrollable selection panels
- virtual scrolling
- bounded filter lists
- bounded relationship-discovery lists
- viewport-constrained page scrolling
- horizontal overflow protection

The general rule is:

> Selection state should scroll inside a bounded component instead of increasing the height of the whole workspace.

This is particularly important for the Auto Report dimension selector.

## 16. Extended OLAP testing

The extended `JpaOlapCubeServiceTest` covers:

1. Scalar dimensions.
2. Singular aggregate relationships.
3. Collection aggregate relationships.
4. Nested aggregate dimensions.
5. Pivot configuration.
6. Unknown dimensions.
7. Unknown measures.
8. Empty-measure default behavior.
9. Relationship-depth normalization.

Run all tests:

```bash
./gradlew test
```

Run the focused test:

```bash
./gradlew test --tests '*JpaOlapCubeServiceTest'
```

When adding an OLAP feature, test both valid and invalid input.

For example:

```text
status                 -> valid
customer               -> valid
customer.name          -> valid
lines                  -> valid
lines.productName      -> valid
doesNotExist           -> invalid
sum:notAField          -> invalid
```

This catches regressions where the UI exposes a field that the backend cannot actually query.

## 17. JPA metamodel rules

The JPA metamodel is central to metadata-driven functionality.

Access it through:

```java
entityManager.getMetamodel()
```

Distinguish between:

- `ManagedType`
- `IdentifiableType`

Identifier operations belong to identifiable entity metadata. Do not assume that every managed type has an ID.

A safe conceptual pattern is:

```java
ManagedType<?> type = ...;

if (type instanceof IdentifiableType<?> identifiable) {
    // identifier metadata is available
}
```

This matters when resolving relationship dimensions and building entity-aware query paths.

## 18. Reporting services

Keep reporting logic out of JPA entities.

A useful structure is:

```text
domain/
repository/
service/
reporting/
```

A reporting service can calculate:

- totals
- grouped metrics
- enum distributions
- fiscal-period summaries
- account balances
- cost-center totals
- profit-center totals
- currency totals

For accounting, a basic journal-line balance is commonly:

```text
Balance = Debit - Credit
```

but actual accounting presentation must respect account type and domain rules.

## 19. Adding a new ERP module

Use this sequence:

### Step 1 — Domain

Create the entities and JPA relationships.

### Step 2 — Repository

Add Spring Data repositories and domain-specific query methods.

### Step 3 — Service

Put business rules in application/domain services rather than controllers.

### Step 4 — Reporting

Create module-specific reporting services where analytical behavior is required.

### Step 5 — CRUD metadata

Verify that the new entities appear in the dynamic resource registry and metadata endpoints.

### Step 6 — OLAP

Verify scalar, relationship and nested dimensions.

### Step 7 — Tests

Add persistence, service, CRUD, reporting and OLAP tests.

### Step 8 — UI

Use the generic React resource/editor infrastructure where possible. Add specialized pages only when a workflow requires domain-specific interaction.

## 20. React architecture

The frontend uses React, TypeScript, PrimeReact and TanStack Query.

A resource route conceptually looks like:

```text
/resources/{slug}
/resources/{slug}/new
/resources/{slug}/{id}/edit
```

The generic resource page can:

- load paginated rows
- infer columns
- navigate to editing
- delete records
- open BI functionality

The editor uses backend metadata to render fields and relationships.

## 21. TanStack Query

Server state should remain server-owned.

A list query can use a key such as:

```text
["rows", slug, page]
```

After a mutation, invalidate the corresponding query so the UI reloads the authoritative server state.

Avoid maintaining a second manually synchronized copy of the database in React state.

## 22. Performance guidelines

ERP datasets can become large.

Prefer:

- server-side pagination
- database-side aggregation
- bounded relationship traversal
- lazy relationships where appropriate
- indexes on frequent filters/grouping columns
- DTO/projection responses for large reports
- bounded UI lists
- virtual scrolling for large selectors

Avoid:

- loading complete tables into React
- recursively traversing unlimited entity graphs
- rendering thousands of selected values
- accidental eager loading of large collections
- performing huge analytical aggregations entirely in Java when the database can aggregate them

## 23. Security

The application uses Spring Security.

The current deployment documentation notes that the React UI retains Basic-auth credentials in `sessionStorage`. Therefore production deployments should use HTTPS.

Never commit:

```text
.env
database passwords
private keys
tokens
credentials
```

Use environment variables and deployment secrets.

## 24. Production deployment

The Docker deployment is:

```text
Browser
   |
 HTTPS / reverse proxy
   |
 Nginx
   |
   +-- React static files
   |
   +-- /api -> Spring Boot
                 |
              PostgreSQL
```

Production guidance:

- use HTTPS
- use a strong PostgreSQL password
- keep `.env` out of Git
- back up PostgreSQL
- monitor application and database health
- introduce migrations before production schema evolution
- once migrations are established, use Hibernate `validate` rather than relying on `update`

## 25. CI workflow

The GitHub Actions workflow is:

```text
.github/workflows/build-test.yml
```

The Build and Test workflow covers:

1. Java setup
2. backend compilation
3. backend tests
4. application build
5. frontend setup/test/build

A reliable development loop is:

```bash
./gradlew test
git diff
git status
git add .
git commit -m "Describe the change"
git push
```

Then inspect GitHub Actions and do not consider the feature complete until CI is green.

## 26. Debugging CI failures

### Compilation failure

Fix errors such as:

```text
cannot find symbol
package ... does not exist
illegal character
')' or ',' expected
```

before investigating runtime behavior.

### Test compilation failure

If production compilation passes but `compileTestJava` fails, inspect the exact test file and line first. Syntax errors in test fixtures are independent of production code.

### Test execution failure

Inspect:

- assertion differences
- mock metadata
- generated query paths
- test database state
- exception causes

### Frontend failure

Inspect:

- TypeScript compilation
- Vite build
- frontend tests
- API response/type mismatches

## 27. Feature-development workflow

For a substantial feature:

```text
Understand domain
      |
      v
Inspect existing implementation
      |
      v
Implement backend
      |
      v
Add focused tests
      |
      v
Update frontend
      |
      v
Run local tests/build
      |
      v
Push
      |
      v
Check CI
      |
      v
Verify API and UI behavior
```

Metadata and OLAP changes deserve regression tests because they can affect many entities and screens.

## 28. Contribution checklist

Before pushing a significant change:

- [ ] Backend compiles.
- [ ] Relevant tests pass.
- [ ] New behavior has regression tests.
- [ ] JPA relationships are correct.
- [ ] Dynamic CRUD metadata works.
- [ ] Aggregate create/edit/delete/restore works.
- [ ] OLAP scalar dimensions work.
- [ ] OLAP aggregate dimensions work.
- [ ] Nested dimensions work.
- [ ] Invalid dimensions/measures are rejected.
- [ ] Large selections do not break the UI.
- [ ] React builds successfully.
- [ ] No secrets are committed.
- [ ] GitHub Actions is green.

## 29. Recommended learning path

A new developer should work through the system in this order:

1. Start the backend.
2. Start the frontend or Docker stack.
3. Open a generic resource page.
4. Inspect its metadata endpoint.
5. Trace the request into `DynamicCrudService`.
6. Inspect one JPA aggregate.
7. Edit the aggregate in the React UI.
8. Inspect the OLAP catalog.
9. Query a scalar dimension.
10. Query a relationship dimension.
11. Query a nested relationship dimension.
12. Read `JpaOlapCubeServiceTest`.
13. Add a small reporting service.
14. Add tests for the service.
15. Run the complete build and verify CI.

## 30. Design philosophy

The platform is moving toward a metadata-driven ERP architecture.

The JPA domain model can serve as a structural source for:

```text
JPA domain
   |
   +--> REST resources
   +--> CRUD metadata
   +--> dynamic forms
   +--> relationship discovery
   +--> OLAP dimensions
   +--> automatic reporting
   +--> OpenAPI
```

This does not mean all ERP behavior should be generic. Generic infrastructure should handle repetitive CRUD and analytical work, while specialized services and screens should contain business-specific workflows.

The key architectural rule is:

> Keep the domain model expressive, keep business rules in services, keep persistence concerns in repositories, and use metadata-driven infrastructure to eliminate repetitive ERP plumbing.

## Quick reference

```bash
# Compile
./gradlew compileJava

# Test
./gradlew test

# Focused OLAP test
./gradlew test --tests '*JpaOlapCubeServiceTest'

# Build
./gradlew build

# Run backend
./gradlew bootRun

# Docker
docker compose up -d --build

# Backend logs
docker compose logs -f backend

# Stop Docker
docker compose down
```
