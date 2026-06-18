ALTER TABLE mall_floor_plans ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_mall_floor_plans_merchant
    ON mall_floor_plans (merchant_id)
 WHERE merchant_id IS NOT NULL;
