CREATE TABLE IF NOT EXISTS tax_policy (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(120) NOT NULL,
    company_id BIGINT NULL,
    jurisdiction_code VARCHAR(32) NULL,
    transaction_type VARCHAR(40) NULL,
    tax_category VARCHAR(60) NULL,
    rate NUMERIC(12, 6) NOT NULL DEFAULT 0,
    valid_from DATE NOT NULL,
    valid_until DATE NOT NULL,
    priority INTEGER NOT NULL DEFAULT 100,
    tax_inclusive BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_tax_policy_rate CHECK (rate >= 0 AND rate <= 100),
    CONSTRAINT ck_tax_policy_dates CHECK (valid_until >= valid_from),
    CONSTRAINT ck_tax_policy_priority CHECK (priority >= 0),
    CONSTRAINT fk_tax_policy_company FOREIGN KEY (company_id) REFERENCES contact(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_tax_policy_lookup
    ON tax_policy (active, valid_from, valid_until, priority);

CREATE INDEX IF NOT EXISTS ix_tax_policy_company_scope
    ON tax_policy (company_id, jurisdiction_code, transaction_type, tax_category);
