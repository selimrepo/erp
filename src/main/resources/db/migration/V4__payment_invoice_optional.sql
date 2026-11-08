-- Payment transactions may represent standalone bank/cash movements as well as invoice settlements.
-- Keep the relationship optional for legacy and non-invoice payment records.
ALTER TABLE IF EXISTS payment_transaction ADD COLUMN IF NOT EXISTS invoice_id BIGINT;
ALTER TABLE IF EXISTS payment_transaction ALTER COLUMN invoice_id DROP NOT NULL;

DO $$
BEGIN
    IF to_regclass('public.payment_transaction') IS NOT NULL
       AND to_regclass('public.invoice') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM pg_constraint
           WHERE conname = 'fk_payment_transaction_invoice'
             AND conrelid = 'public.payment_transaction'::regclass
       ) THEN
        ALTER TABLE payment_transaction
            ADD CONSTRAINT fk_payment_transaction_invoice
            FOREIGN KEY (invoice_id) REFERENCES invoice(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_payment_transaction_invoice_status
    ON payment_transaction(invoice_id, status);
