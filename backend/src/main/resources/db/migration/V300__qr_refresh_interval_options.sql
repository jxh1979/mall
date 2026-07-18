-- 2026-07-18 拍板：凡出示验证的二维码一律动态刷新，默认 15 秒，后台可设（10/15/30/60 秒）。
-- V064（ticket_credentials）与 V113（ticket_types）原仅允许 {30,60}、默认 60，本迁移放宽白名单并把默认对齐 15。
-- 存量行取值不动（历史凭证/票种保持原窗口，改动仅影响默认与可选档位）。
ALTER TABLE ticket_credentials
    DROP CONSTRAINT IF EXISTS chk_ticket_credentials_refresh_interval;
ALTER TABLE ticket_credentials
    ADD CONSTRAINT chk_ticket_credentials_refresh_interval
        CHECK (refresh_interval_seconds IN (10, 15, 30, 60));
ALTER TABLE ticket_credentials
    ALTER COLUMN refresh_interval_seconds SET DEFAULT 15;
ALTER TABLE ticket_types
    DROP CONSTRAINT IF EXISTS chk_ticket_types_refresh_interval;
ALTER TABLE ticket_types
    ADD CONSTRAINT chk_ticket_types_refresh_interval
        CHECK (refresh_interval_seconds IS NULL OR refresh_interval_seconds IN (10, 15, 30, 60));
