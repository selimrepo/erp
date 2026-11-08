-- Production integrity follow-up. Legacy core tables are optional on a fresh database;
-- V1 creates the new workflow tables and Hibernate creates the legacy domain schema.
ALTER TABLE IF EXISTS quote ADD COLUMN IF NOT EXISTS conversion_payload_hash VARCHAR(64);
ALTER TABLE IF EXISTS erporder ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(180);
ALTER TABLE IF EXISTS purchase ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(180);
ALTER TABLE IF EXISTS payment_transaction ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(180);
ALTER TABLE IF EXISTS journal_entry ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(180);
ALTER TABLE IF EXISTS stock_movement ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(180);
ALTER TABLE IF EXISTS invoice ADD COLUMN IF NOT EXISTS workflow_key VARCHAR(180);
ALTER TABLE IF EXISTS quote ADD COLUMN IF NOT EXISTS conversion_idempotency_key VARCHAR(180);

DO $$
BEGIN
    IF to_regclass('public.purchase_detail') IS NOT NULL THEN
        UPDATE purchase_detail
           SET tax_amount = COALESCE(tax_amount, 0),
               line_total = COALESCE(quantity, 0) * COALESCE(price, 0) + COALESCE(tax_amount, 0)
         WHERE line_total IS NULL OR line_total = 0;
    END IF;
    IF to_regclass('public.invoice') IS NOT NULL THEN
        UPDATE invoice
           SET tax_amount = COALESCE(tax_amount, 0),
               net_amount = COALESCE(amount, 0) - COALESCE(tax_amount, 0),
               credited_amount = COALESCE(credited_amount, 0)
         WHERE net_amount IS NULL OR (net_amount = 0 AND COALESCE(amount, 0) > COALESCE(tax_amount, 0));
    END IF;
    IF to_regclass('public.purchase') IS NOT NULL THEN
        UPDATE purchase SET tax_amount = COALESCE(tax_amount, 0) WHERE tax_amount IS NULL;
    END IF;
END $$;

-- Database uniqueness is the final concurrency boundary for idempotent commands.
DO $$
BEGIN
    IF to_regclass('public.erporder') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS ux_erporder_idempotency_key ON erporder(idempotency_key) WHERE idempotency_key IS NOT NULL';
    END IF;
    IF to_regclass('public.purchase') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS ux_purchase_idempotency_key ON purchase(idempotency_key) WHERE idempotency_key IS NOT NULL';
        IF to_regclass('public.purchase_detail') IS NOT NULL THEN
            EXECUTE 'CREATE INDEX IF NOT EXISTS ix_purchase_detail_purchase ON purchase_detail(purchase_id)';
        END IF;
    END IF;
    IF to_regclass('public.payment_transaction') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS ux_payment_transaction_idempotency_key ON payment_transaction(idempotency_key) WHERE idempotency_key IS NOT NULL';
        EXECUTE 'CREATE INDEX IF NOT EXISTS ix_payment_invoice_status ON payment_transaction(invoice_id, status)';
    END IF;
    IF to_regclass('public.journal_entry') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS ux_journal_entry_idempotency_key ON journal_entry(idempotency_key) WHERE idempotency_key IS NOT NULL';
        EXECUTE 'CREATE INDEX IF NOT EXISTS ix_journal_entry_source ON journal_entry(entry_id)';
    END IF;
    IF to_regclass('public.stock_movement') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS ux_stock_movement_idempotency_key ON stock_movement(idempotency_key)';
        EXECUTE 'CREATE INDEX IF NOT EXISTS ix_stock_movement_reference ON stock_movement(reference_type, reference_id, movement_type)';
    END IF;
    IF to_regclass('public.quote') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS ux_quote_conversion_idempotency_key ON quote(conversion_idempotency_key) WHERE conversion_idempotency_key IS NOT NULL';
    END IF;
    IF to_regclass('public.invoice') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS ux_invoice_workflow_key ON invoice(workflow_key) WHERE workflow_key IS NOT NULL';
    END IF;
    IF to_regclass('public.product_stock_location') IS NOT NULL THEN
        EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS ux_stock_product_store ON product_stock_location(product_id, store_id)';
    END IF;
    EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS ux_purchase_receipt_line_detail ON purchase_receipt_line(receipt_id, purchase_detail_id)';
    EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS ux_account_rule_company_event_priority ON account_determination_rule(company_id, event_type, priority) WHERE company_id IS NOT NULL';
    EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS ux_account_rule_global_event_priority ON account_determination_rule(event_type, priority) WHERE company_id IS NULL';
END $$;

-- Add foreign keys only when the corresponding legacy target table exists.
DO $$
BEGIN
    IF to_regclass('public.purchase') IS NOT NULL AND NOT EXISTS
       (SELECT 1 FROM pg_constraint WHERE conname = 'fk_purchase_receipt_purchase') THEN
        ALTER TABLE purchase_receipt ADD CONSTRAINT fk_purchase_receipt_purchase FOREIGN KEY (purchase_id) REFERENCES purchase(id);
    END IF;
    IF to_regclass('public.purchase_receipt') IS NOT NULL AND NOT EXISTS
       (SELECT 1 FROM pg_constraint WHERE conname = 'fk_purchase_receipt_line_receipt') THEN
        ALTER TABLE purchase_receipt_line ADD CONSTRAINT fk_purchase_receipt_line_receipt FOREIGN KEY (receipt_id) REFERENCES purchase_receipt(id) ON DELETE CASCADE;
    END IF;
    IF to_regclass('public.purchase_detail') IS NOT NULL AND NOT EXISTS
       (SELECT 1 FROM pg_constraint WHERE conname = 'fk_purchase_receipt_line_detail') THEN
        ALTER TABLE purchase_receipt_line ADD CONSTRAINT fk_purchase_receipt_line_detail FOREIGN KEY (purchase_detail_id) REFERENCES purchase_detail(id);
    END IF;
    IF to_regclass('public.currency') IS NOT NULL AND NOT EXISTS
       (SELECT 1 FROM pg_constraint WHERE conname = 'fk_bank_statement_currency') THEN
        ALTER TABLE bank_statement ADD CONSTRAINT fk_bank_statement_currency FOREIGN KEY (currency_id) REFERENCES currency(id);
    END IF;
    IF to_regclass('public.payment_transaction') IS NOT NULL AND NOT EXISTS
       (SELECT 1 FROM pg_constraint WHERE conname = 'fk_bank_statement_line_payment') THEN
        ALTER TABLE bank_statement_line ADD CONSTRAINT fk_bank_statement_line_payment FOREIGN KEY (matched_payment_id) REFERENCES payment_transaction(id);
    END IF;
    IF to_regclass('public.bank_statement') IS NOT NULL AND NOT EXISTS
       (SELECT 1 FROM pg_constraint WHERE conname = 'fk_bank_statement_line_statement') THEN
        ALTER TABLE bank_statement_line ADD CONSTRAINT fk_bank_statement_line_statement FOREIGN KEY (statement_id) REFERENCES bank_statement(id) ON DELETE CASCADE;
    END IF;
    IF to_regclass('public.account') IS NOT NULL AND NOT EXISTS
       (SELECT 1 FROM pg_constraint WHERE conname = 'fk_account_rule_debit') THEN
        ALTER TABLE account_determination_rule ADD CONSTRAINT fk_account_rule_debit FOREIGN KEY (debit_account_id) REFERENCES account(id);
    END IF;
    IF to_regclass('public.account') IS NOT NULL AND NOT EXISTS
       (SELECT 1 FROM pg_constraint WHERE conname = 'fk_account_rule_credit') THEN
        ALTER TABLE account_determination_rule ADD CONSTRAINT fk_account_rule_credit FOREIGN KEY (credit_account_id) REFERENCES account(id);
    END IF;
    IF to_regclass('public.account') IS NOT NULL AND NOT EXISTS
       (SELECT 1 FROM pg_constraint WHERE conname = 'fk_account_rule_tax') THEN
        ALTER TABLE account_determination_rule ADD CONSTRAINT fk_account_rule_tax FOREIGN KEY (tax_account_id) REFERENCES account(id);
    END IF;
    IF to_regclass('public.contact') IS NOT NULL AND NOT EXISTS
       (SELECT 1 FROM pg_constraint WHERE conname = 'fk_account_rule_company') THEN
        ALTER TABLE account_determination_rule ADD CONSTRAINT fk_account_rule_company FOREIGN KEY (company_id) REFERENCES contact(id);
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.purchase_detail') IS NOT NULL AND NOT EXISTS
       (SELECT 1 FROM pg_constraint WHERE conname = 'ck_purchase_detail_received_range') THEN
        ALTER TABLE purchase_detail ADD CONSTRAINT ck_purchase_detail_received_range CHECK (received_quantity >= 0 AND received_quantity <= quantity);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_purchase_receipt_line_positive') THEN
        ALTER TABLE purchase_receipt_line ADD CONSTRAINT ck_purchase_receipt_line_positive CHECK (quantity > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_bank_statement_line_nonzero') THEN
        ALTER TABLE bank_statement_line ADD CONSTRAINT ck_bank_statement_line_nonzero CHECK (amount <> 0);
    END IF;
END $$;
