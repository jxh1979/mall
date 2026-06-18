ALTER TABLE merchants
    ADD COLUMN IF NOT EXISTS mall_block_code VARCHAR(128),
    ADD COLUMN IF NOT EXISTS area_square_meter NUMERIC(18,2),
    ADD COLUMN IF NOT EXISTS leased_area_square_meter NUMERIC(18,2),
    ADD COLUMN IF NOT EXISTS secondary_contact_name VARCHAR(120),
    ADD COLUMN IF NOT EXISTS secondary_contact_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS rent_per_square_meter NUMERIC(18,2),
    ADD COLUMN IF NOT EXISTS cooperation_form VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_merchants_mall_block
    ON merchants (mall_block_code);
