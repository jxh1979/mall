-- V320 二维码时效统一（#172，2026-07-19 用户拍板）：凡「出示验证类」动态二维码的刷新间隔
--   全场统一为一套后台设置，取值以 5 秒为单位、最长 60 秒，默认 15 秒；不再按票种各配一套。
--   1) 新增全场统一配置表 ticket_qr_refresh_config（单行 id='default'），验票发凭证时读它决定刷新窗口。
--   2) 放宽 V300 收窄的 CHECK：ticket_credentials / ticket_types 的 refresh_interval_seconds
--      由 {10,15,30,60} 改为「能被 5 整除且 5≤x≤60」。存量取值 {10,15,30,60} 均满足新式，无需回填。
--   ticket_types.refresh_interval_seconds 自此转为休眠列（统一改由全场配置驱动，不再按票种取值）。

CREATE TABLE IF NOT EXISTS ticket_qr_refresh_config (
    id                       VARCHAR(32) PRIMARY KEY,
    refresh_interval_seconds INT NOT NULL DEFAULT 15,
    updated_by               VARCHAR(64),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_qr_refresh_config_interval
        CHECK (refresh_interval_seconds % 5 = 0 AND refresh_interval_seconds BETWEEN 5 AND 60)
);

-- 单行全场配置：默认 15 秒（缺行时应用层回退 15，此处显式落一行便于后台读改）。
INSERT INTO ticket_qr_refresh_config (id, refresh_interval_seconds)
VALUES ('default', 15)
ON CONFLICT (id) DO NOTHING;

ALTER TABLE ticket_credentials
    DROP CONSTRAINT IF EXISTS chk_ticket_credentials_refresh_interval;
ALTER TABLE ticket_credentials
    ADD CONSTRAINT chk_ticket_credentials_refresh_interval
        CHECK (refresh_interval_seconds % 5 = 0 AND refresh_interval_seconds BETWEEN 5 AND 60);

ALTER TABLE ticket_types
    DROP CONSTRAINT IF EXISTS chk_ticket_types_refresh_interval;
ALTER TABLE ticket_types
    ADD CONSTRAINT chk_ticket_types_refresh_interval
        CHECK (refresh_interval_seconds IS NULL
               OR (refresh_interval_seconds % 5 = 0 AND refresh_interval_seconds BETWEEN 5 AND 60));

-- 生产受限运行时权限（V298/V299 教训：新表必须显式 GRANT 给 complex_app，否则生产 500）。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON ticket_qr_refresh_config TO complex_app';
    END IF;
END $$;
