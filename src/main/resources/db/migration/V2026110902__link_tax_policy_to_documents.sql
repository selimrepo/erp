ALTER TABLE erp_order_line
    ADD COLUMN IF NOT EXISTS tax_policy_id BIGINT NULL;
ALTER TABLE erp_order_line
    ADD CONSTRAINT fk_erp_order_line_tax_policy
    FOREIGN KEY (tax_policy_id) REFERENCES tax_policy(id) ON DELETE RESTRICT;

ALTER TABLE purchase_detail
    ADD COLUMN IF NOT EXISTS tax_policy_id BIGINT NULL;
ALTER TABLE purchase_detail
    ADD CONSTRAINT fk_purchase_detail_tax_policy
    FOREIGN KEY (tax_policy_id) REFERENCES tax_policy(id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS ix_erp_order_line_tax_policy ON erp_order_line(tax_policy_id);
CREATE INDEX IF NOT EXISTS ix_purchase_detail_tax_policy ON purchase_detail(tax_policy_id);
