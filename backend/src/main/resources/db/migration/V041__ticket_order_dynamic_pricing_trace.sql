ALTER TABLE ticket_orders
    ADD COLUMN IF NOT EXISTS dynamic_pricing_rule_id VARCHAR(64) REFERENCES ticket_dynamic_pricing_rules(id),
    ADD COLUMN IF NOT EXISTS base_amount_cent BIGINT,
    ADD COLUMN IF NOT EXISTS dynamic_adjustment_cent BIGINT NOT NULL DEFAULT 0;

UPDATE ticket_orders
   SET base_amount_cent = amount_cent
 WHERE base_amount_cent IS NULL;

CREATE INDEX IF NOT EXISTS idx_ticket_orders_dynamic_pricing_rule
    ON ticket_orders (dynamic_pricing_rule_id, created_at DESC);
