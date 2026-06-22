-- P0 修复：票务超卖——票种增加库存数量列（按票种总量限量口径）。
-- stock_quantity 为 NULL = 不限量（仅计数）；非空 = 限量上限。
-- sold_quantity = 已售/已占数量，下单时原子 +quantity，超过 stock_quantity 时拒绝（防超卖）。
ALTER TABLE ticket_types
    ADD COLUMN IF NOT EXISTS stock_quantity INTEGER,
    ADD COLUMN IF NOT EXISTS sold_quantity INTEGER NOT NULL DEFAULT 0;

ALTER TABLE ticket_types
    ADD CONSTRAINT chk_ticket_types_sold_quantity_nonneg CHECK (sold_quantity >= 0);

ALTER TABLE ticket_types
    ADD CONSTRAINT chk_ticket_types_stock_quantity_nonneg CHECK (stock_quantity IS NULL OR stock_quantity >= 0);

-- 已售不得超过限量（原子扣减语句保证，约束做双保险）。
ALTER TABLE ticket_types
    ADD CONSTRAINT chk_ticket_types_sold_within_stock
        CHECK (stock_quantity IS NULL OR sold_quantity <= stock_quantity);
