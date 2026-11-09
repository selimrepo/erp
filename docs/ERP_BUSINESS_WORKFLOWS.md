# ERP Business Workflow Contracts

This document describes the transactional commands exposed by `/api/workflows`. The module frontends call these commands instead of emulating business transactions with generic CRUD.

## Sales — order, reservation, shipment, cancellation

1. Create an order with `POST /sales/orders`. The command validates the customer, shipping contact, currency, warehouse, products and available balances.
2. Each order line is persisted and stock is reserved under a pessimistic warehouse/stock-row lock. A `SALE_RESERVATION` movement is recorded for every line.
3. When requested, a confirmed sales invoice is created in the same transaction.
4. `POST /sales/orders/{id}/ship` moves a processing order to `SHIPPED` and records shipment movements. It does not decrement the balance again because reservation already reduced it.
5. `POST /sales/orders/{id}/cancel` moves a processing order to `CANCELLED`, restores every reservation to its original product/warehouse balance and records release movements.
6. Shipment/cancellation commands are guarded by a pessimistic order lock and terminal states make retries safe.

## Procurement — purchase order, receipt, supplier invoice

1. `POST /procurement/purchases` creates an `ORDERED` purchase with supplier, warehouse, currency and detail lines.
2. `POST /procurement/purchases/{id}/receive?storeId={storeId}` validates the warehouse and receives all outstanding purchase detail lines in one transaction.
3. Receipt increments product/warehouse balances, writes `PURCHASE_RECEIPT` movements and creates the supplier invoice.
4. Purchase state and receipt idempotency key prevent duplicate receipt processing. A receipt must target the purchase warehouse.

Partial receipts are supported through `POST /procurement/purchases/{id}/receive-lines`. Each request names purchase-detail IDs and positive received quantities; the purchase is locked, cumulative received quantities cannot exceed ordered quantities, and receipt lines and stock movements are written atomically. The legacy `/receive` command remains as a receive-all-outstanding convenience operation.

## Finance — invoice, settlement, credit notes and bank reconciliation

1. Sales and fully received purchase workflows create confirmed invoices with stable workflow keys and persisted net/tax/gross snapshots.
2. `POST /finance/invoices/{id}/payments` accepts partial or full payment transactions. The invoice is locked, currency and invoice direction are validated, overpayments are rejected, and paid/outstanding status is updated atomically.
3. `POST /finance/invoices/{id}/credit-notes` creates a linked credit note and updates the original invoice's credited balance; the credit cannot exceed the remaining unsettled balance.
4. `POST /finance/bank-statements` imports idempotent statement lines and performs conservative exact matching by currency, amount, reference, direction and payment date. Ambiguous matches remain unmatched for operator review.
5. `POST /finance/bank-statement-lines/{id}/reconcile/{paymentId}` manually reconciles a line after strict match checks. A payment can be matched to only one statement line.
6. Reusing a payment, credit-note or statement idempotency key with a different payload is rejected. Draft and cancelled invoices cannot be settled.

## Accounting — source document posting

- `POST /finance/invoices/{id}/post` posts a balanced two-line journal for a confirmed invoice.
- `POST /finance/payments/{id}/post` posts a balanced two-line journal for a completed payment.
- The caller can supply explicit debit/credit/tax accounts, or use the automatic posting endpoints backed by company-specific or global `AccountDeterminationRule` records. Account determination is configured against the customer's chart of accounts rather than hard-coded to arbitrary account IDs.
- The journal command validates open fiscal period/date, posting-enabled accounts, non-negative one-sided line amounts and exact debit/credit equality. Journal and source-posting operations share the same database transaction.
- For a sales invoice, configure debit = receivables and credit = sales revenue. For a purchase invoice, configure debit = inventory/expense and credit = payables. For a customer receipt, configure debit = bank/cash and credit = receivables. For a supplier payment, configure debit = payables and credit = bank/cash.

## Transaction and integration boundaries

- Each command is one Spring transaction; stock and invoice balance updates are not split into frontend-side CRUD calls.
- Pessimistic locks serialize updates to the order, invoice, purchase, warehouse and stock balance.
- Idempotency keys are backed by unique database constraints on the source records/movements.
- Generic CRUD remains available for master data and setup. It must not be used to post stock, settle invoices or create ledger entries.
- The current invoice model stores integer monetary amounts. Configure and display values using the application's existing minor-unit convention; currency precision, tax-inclusive pricing and jurisdiction-specific tax determination still need a formal tenant-level policy.
- Inventory corrections use `POST /inventory/stock-adjustments`, which stores an immutable reasoned adjustment and compensating stock movement. Warehouse transfers use `POST /inventory/stock-transfers`: source and destination warehouse rows are locked in stable ID order, both balances are updated in one transaction, and paired inbound/outbound movements share the command idempotency key. Generic CRUD cannot directly change an existing stock balance.


## Completion boundary

The workflow API provides transactional primitives, but production readiness still requires approved tenant accounting/tax policies, a verified migration strategy, complete return/refund treatment, and end-to-end tests against the supported database. The React console must expose the same line-level and reconciliation operations; an API capability is not operationally complete until the UI and authorization paths are covered.


## Generic tax policies

The tax module provides reusable, date-effective `TaxPolicy` records. A policy can be global or company-scoped and can optionally constrain jurisdiction, transaction type and tax category. Null scope values act as wildcards. Resolution chooses the most specific matching policy, then the lowest numeric priority, then the most recent effective date. Policies support tax-inclusive and tax-exclusive prices; the calculation API accepts and returns integer minor-unit amounts to match the current invoice amount convention.

A policy must have a non-negative rate no greater than 100%, a valid date interval and a non-negative priority. The service fails closed when no active policy matches. The Flyway migration creates the policy table and lookup indexes.

This is a generic policy engine, not a preconfigured legal-tax catalogue. Tenants must configure rates, exemptions and effective dates for their jurisdictions. Reverse charge, withholding, compound taxes, multiple tax components and statutory invoice reporting are not implied by this basic rate model. The calculation service must be invoked by each invoice-creation workflow before it can be considered end-to-end tax automation.

## Journal posting invariants

At the persistence boundary, a journal can transition from DRAFT to POSTED only when:
- its fiscal year and period exist, are open, and the entry date belongs to both;
- the period belongs to the selected fiscal year;
- it has at least two lines, each with exactly one positive debit or credit;
- every line uses an unblocked account with posting enabled; and
- total debit equals total credit and is greater than zero.

Posted journal headers and lines remain immutable; corrections require reversal entries. Workflow services must still enforce authorization, idempotency, source-document linkage and transaction boundaries around posting.
