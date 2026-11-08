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

Current model limitation: receipt is all-lines/all-quantity. Partial receipts and per-line received quantities require a persisted received-quantity field and a schema migration; the current API intentionally does not pretend to support them.

## Finance — invoice and settlement

1. Sales and purchase workflows create confirmed invoices with a stable workflow key.
2. `POST /finance/invoices/{id}/payments` accepts partial or full payment transactions. The invoice is locked, currency and invoice direction are validated, overpayments are rejected, and invoice paid/outstanding status is updated atomically.
3. Reusing a payment idempotency key for another invoice or amount is rejected.
4. Draft and cancelled invoices cannot be settled.

## Accounting — source document posting

- `POST /finance/invoices/{id}/post` posts a balanced two-line journal for a confirmed invoice.
- `POST /finance/payments/{id}/post` posts a balanced two-line journal for a completed payment.
- The caller supplies fiscal year/period, posting date, debit account and credit account. This is deliberate: account determination must be configured against the customer's chart of accounts rather than hard-coded to arbitrary account IDs.
- The journal command validates open fiscal period/date, posting-enabled accounts, non-negative one-sided line amounts and exact debit/credit equality. Journal and source-posting operations share the same database transaction.
- For a sales invoice, configure debit = receivables and credit = sales revenue. For a purchase invoice, configure debit = inventory/expense and credit = payables. For a customer receipt, configure debit = bank/cash and credit = receivables. For a supplier payment, configure debit = payables and credit = bank/cash.

## Transaction and integration boundaries

- Each command is one Spring transaction; stock and invoice balance updates are not split into frontend-side CRUD calls.
- Pessimistic locks serialize updates to the order, invoice, purchase, warehouse and stock balance.
- Idempotency keys are backed by unique database constraints on the source records/movements.
- Generic CRUD remains available for master data and setup. It must not be used to post stock, settle invoices or create ledger entries.
- The current invoice model stores integer monetary amounts. Configure and display values using the application's existing minor-unit convention; currency precision and tax-inclusive pricing still need a formal tenant-level policy.
- This iteration does not add automatic account mapping, tax calculation, delivery carrier integration, returns/credit notes, bank reconciliation or partial goods receipt. Those require explicit business configuration and additional domain fields.
