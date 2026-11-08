-- Payment transactions may represent standalone bank/cash movements as well as invoice settlements.
-- Invoice-linked payment commands still enforce the invoice relationship in the application service.
ALTER TABLE IF EXISTS payment_transaction ALTER COLUMN invoice_id DROP NOT NULL;
