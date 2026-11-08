# Transactional ERP workflows

The module applications expose dedicated workflow consoles for Sales, Procurement, Finance and Accounting. The commands below execute inside Spring-managed JPA transactions; do not recreate these sequences by issuing multiple generic CRUD requests from the browser.

## Sales order

`POST /api/workflows/sales/orders`

- Validates customer, shipping contact, currency, store and products.
- Locks the warehouse row and the product/store balance while checking availability.
- Rejects insufficient stock and negative/zero quantities.
- Persists order header and order lines, decrements the stock balance, writes a stock movement and optionally creates a confirmed sales invoice.
- Uses an idempotency key on the order and stock movement rows.

Example request:

```json
{
  "idempotencyKey": "web-unique-request-id",
  "customerId": 12,
  "shippingContactId": 12,
  "currencyId": 1,
  "storeId": 2,
  "comment": "Customer order",
  "createInvoice": true,
  "lines": [{"productId": 42, "quantity": 3, "unitPrice": 1250}]
}
```

## Procurement and receipt

`POST /api/workflows/procurement/purchases`

Creates a supplier purchase order with supplier, store, currency and purchase detail lines.

`POST /api/workflows/procurement/purchases/{id}/receive?storeId={storeId}`

- Locks the purchase and warehouse.
- Requires the receipt warehouse to match the purchase warehouse.
- Increases product/store balances, writes stock movements and StockItem receipt records.
- Creates a confirmed purchase invoice for the supplier.
- Prevents a second receipt and records the receipt idempotency key.

## Invoice settlement

`POST /api/workflows/finance/invoices/{id}/payments`

- Locks the invoice to serialize concurrent payments.
- Rejects draft/cancelled invoices, mismatched payment direction and overpayments.
- Writes the payment transaction and updates paid/outstanding amounts in the same transaction.
- Moves invoice status to PARTIALLY_PAID or PAID.
- Uses an idempotency key to prevent duplicate payment writes.

## Journal posting

`POST /api/workflows/accounting/journals`

- Requires at least two lines.
- Rejects negative, empty or simultaneous debit/credit lines.
- Requires every account to be active for posting (not blocked and postingAllowed).
- Requires an open fiscal year and period, with period/year/date consistency.
- Requires exact debit/credit equality.
- Persists the journal header and lines with POSTED status in one transaction.

## Security

Every workflow endpoint checks the existing policy authorization service for the relevant resource and WRITE permission. Backend authorization remains authoritative; hiding a button in React is not a security boundary.

## Operational notes

- Idempotency keys should be unique per logical command and generated once by the client before the first attempt; retries must reuse the same key.
- Amounts remain integer minor units to match the current ERP model's Long amount/price fields. A future currency precision policy should make this explicit per currency.
- This implementation is a transactional baseline, not a complete tax/discount/shipping engine. Tax calculation, multi-line UI, returns, partial deliveries, reservations/fulfilment transitions, payment allocations across invoices, bank reconciliation and automatic chart-of-account mapping require additional domain rules.
- The existing application properties currently use `spring.jpa.hibernate.ddl-auto=create`. That is destructive on startup and must be changed to a controlled migration strategy before using this branch against persistent production data. The new entity/relationship fields require reviewed database migrations.
