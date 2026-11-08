# Workflow hardening and production-readiness notes

## Design boundary

JPA entity listeners in this branch enforce invariants that must also hold when an entity is changed through Spring Data REST or a generic CRUD endpoint. They do not orchestrate cross-aggregate business operations and do not perform network calls, repository lookups or asynchronous work.

Transactional application services remain the only place where a business command changes multiple aggregates. They acquire pessimistic locks, validate the command, write the source document, update balances and append immutable movement/journal/audit records in one database transaction. A short-lived `@Transient` authorization flag is set by the command service immediately before a guarded change; the listener validates it and clears it after the persistence callback. This prevents generic CRUD from bypassing a workflow transition.

## Listener responsibilities

| Domain | Listener guarantee |
|---|---|
| Order | Creation and PROCESSING → SHIPPED/CANCELLED transitions require sales workflow commands; order lines cannot be removed after reservation |
| Purchase | Creation and receipt state transitions require procurement commands; received quantities cannot be edited directly |
| Stock | Existing stock balance changes require a stock movement; stock movements and stock adjustments are immutable |
| Quote | Quote stages require quote commands; CONVERTED requires the conversion workflow and an order reference |
| Invoice | Monetary snapshots are immutable after draft; payment/credit balances require settlement commands; ledger links require posting commands; confirmed invoices cannot be deleted |
| Payment | Completed payment facts cannot be edited, reopened or deleted; ledger links require posting commands |
| Journal | A journal must be created as DRAFT, balanced lines are inserted while DRAFT, posting requires the journal command, and posted headers/lines cannot be edited or deleted |
| Bank reconciliation | Statement import and line creation require import commands; matching/status changes require reconciliation commands; imported statements and lines are immutable |
| CRM / Projects / Support | Opportunity, quote, project, task and case state transitions require their workflow commands; cases require resolution text before closure |
| Tax / Account mapping | Tax percentages and account-determination rules are validated before persistence |

These listeners are intentionally local. For example, the quote listener validates the quote state and conversion reference; the conversion service resolves the customer, reserves stock, creates the order/invoice and records the conversion key atomically.

## Idempotency and concurrency

- Request records have bounded idempotency keys. The service hashes the complete request representation with SHA-256 and compares the stored hash on replay; a key reused with a different payload is rejected.
- Database unique indexes/constraints are the final guard against concurrent duplicate requests. Pessimistic locks serialize order, purchase, invoice, stock and bank reconciliation updates.
- Stock movements, purchase receipt lines, posted journal lines, completed payments, credit notes and bank statement lines are audit facts; corrections must be represented by compensating business commands rather than editing/deleting history.
- The migration sequence adds the workflow schema, backfills monetary snapshots, and adds idempotency indexes and relational/check constraints. Validate V2/V3 against a production-like PostgreSQL snapshot before rollout.

## Remaining production gates

This branch is a hardening iteration, not a blanket production certification. Before a production rollout, run the full suite against PostgreSQL (not only H2), test concurrent duplicate requests and lock contention, verify Flyway from both an empty database and an upgraded legacy snapshot, and add end-to-end tests covering sales, partial receipts, partial settlement, credit notes, ledger posting and bank reconciliation.

Remaining domain work includes provider-specific bank file ingestion, bank reconciliation reversal/unmatch, payment refunds, stock returns, partial sales shipment, automatic tax-jurisdiction/location rules, currency precision/minor-unit policy, tax-inclusive pricing, account-rule effective dates and tenant/company scoping. JPA lifecycle callbacks do not run for JPQL bulk updates or native SQL; protected aggregates must not be mutated through bulk update paths. If asynchronous integrations are added, publish through a transactional outbox rather than making network calls from entity listeners.
