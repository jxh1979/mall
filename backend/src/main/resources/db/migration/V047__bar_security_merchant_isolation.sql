-- Bar domain: add merchant_id for tenant isolation (P1 security fix)
ALTER TABLE bar_tables           ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(100);
ALTER TABLE bar_reservations     ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(100);
ALTER TABLE bar_products         ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(100);
ALTER TABLE bar_orders           ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(100);
ALTER TABLE bar_production_tickets ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(100);
ALTER TABLE bar_bills            ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_bar_tables_merchant           ON bar_tables(merchant_id)            WHERE merchant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_bar_reservations_merchant     ON bar_reservations(merchant_id)      WHERE merchant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_bar_products_merchant         ON bar_products(merchant_id)          WHERE merchant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_bar_orders_merchant           ON bar_orders(merchant_id)            WHERE merchant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_bar_production_tickets_merchant ON bar_production_tickets(merchant_id) WHERE merchant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_bar_bills_merchant            ON bar_bills(merchant_id)             WHERE merchant_id IS NOT NULL;
