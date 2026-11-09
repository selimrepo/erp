ALTER TABLE tax_policy
    ADD COLUMN IF NOT EXISTS treatment VARCHAR(16) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN IF NOT EXISTS exemption_code VARCHAR(80),
    ADD COLUMN IF NOT EXISTS required_evidence_type VARCHAR(120);

ALTER TABLE erp_order_line
    ADD COLUMN IF NOT EXISTS withholding_amount BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS reverse_charge_tax_amount BIGINT NOT NULL DEFAULT 0;

ALTER TABLE purchase_detail
    ADD COLUMN IF NOT EXISTS withholding_amount BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS reverse_charge_tax_amount BIGINT NOT NULL DEFAULT 0;

ALTER TABLE invoice
    ADD COLUMN IF NOT EXISTS withholding_amount BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS reverse_charge_tax_amount BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS credit_note_reason_code VARCHAR(32),
    ADD COLUMN IF NOT EXISTS tax_evidence_type VARCHAR(120),
    ADD COLUMN IF NOT EXISTS tax_evidence_reference VARCHAR(240);

CREATE TABLE IF NOT EXISTS tax_policy_component (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    tax_policy_id BIGINT NOT NULL REFERENCES tax_policy(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    component_type VARCHAR(24) NOT NULL,
    rate NUMERIC(12,6) NOT NULL,
    sequence_no INTEGER NOT NULL,
    output_tax_account_id BIGINT REFERENCES account(id) ON DELETE RESTRICT,
    input_tax_account_id BIGINT REFERENCES account(id) ON DELETE RESTRICT,
    CONSTRAINT ck_tax_policy_component_rate CHECK (rate >= 0 AND rate <= 100),
    CONSTRAINT ck_tax_policy_component_sequence CHECK (sequence_no >= 0),
    CONSTRAINT ck_tax_policy_component_type CHECK (component_type IN ('STANDARD','COMPOUND','WITHHOLDING','REVERSE_CHARGE')),
    CONSTRAINT uk_tax_policy_component_sequence UNIQUE (tax_policy_id, sequence_no)
);

CREATE INDEX IF NOT EXISTS ix_tax_policy_component_policy
    ON tax_policy_component(tax_policy_id, sequence_no);

CREATE TABLE IF NOT EXISTS invoice_tax_component (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    invoice_id BIGINT NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    component_type VARCHAR(24) NOT NULL,
    rate NUMERIC(12,6) NOT NULL,
    amount BIGINT NOT NULL,
    output_tax_account_id BIGINT REFERENCES account(id) ON DELETE RESTRICT,
    input_tax_account_id BIGINT REFERENCES account(id) ON DELETE RESTRICT,
    CONSTRAINT ck_invoice_tax_component_amount CHECK (amount >= 0)
);

CREATE TABLE IF NOT EXISTS purchase_detail_tax_component (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    purchase_detail_id BIGINT NOT NULL REFERENCES purchase_detail(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    component_type VARCHAR(24) NOT NULL,
    rate NUMERIC(12,6) NOT NULL,
    amount BIGINT NOT NULL,
    output_tax_account_id BIGINT REFERENCES account(id) ON DELETE RESTRICT,
    input_tax_account_id BIGINT REFERENCES account(id) ON DELETE RESTRICT,
    CONSTRAINT ck_purchase_detail_tax_component_amount CHECK (amount >= 0)
);

CREATE INDEX IF NOT EXISTS ix_invoice_tax_component_invoice ON invoice_tax_component(invoice_id);
CREATE INDEX IF NOT EXISTS ix_purchase_detail_tax_component_line ON purchase_detail_tax_component(purchase_detail_id);
