-- P1 清单#19：防伪二维码刷新窗口（refresh_interval）接票种级配置。
-- 背景：V064 已在 ticket_credentials 落 refresh_interval_seconds（取值 {30,60} 默认 60），
-- 但 TicketVerificationService.refreshCredential 恒硬编码 60，未接票种配置；本迁移仿 V075.validity_days
-- 在 ticket_types 落票种级 refresh_interval_seconds（可空），发凭证时按票种取值，为空回退默认 60（向后兼容）。
ALTER TABLE ticket_types
    ADD COLUMN IF NOT EXISTS refresh_interval_seconds INTEGER;
ALTER TABLE ticket_types
    ADD CONSTRAINT chk_ticket_types_refresh_interval
        CHECK (refresh_interval_seconds IS NULL OR refresh_interval_seconds IN (30, 60));
