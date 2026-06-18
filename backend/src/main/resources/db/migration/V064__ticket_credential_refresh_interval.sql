-- R3 修复 C2（P2）：门票二维码刷新周期字段（REQ-08）。
-- 背景：R1-LOOP-11 指出"30 秒或 60 秒刷新"始终未拍板取值、无字段承载。本迁移落 refresh_interval_seconds，
-- 票种级可配，约束取值 {30,60}，默认 60（截图无效靠在线核验，刷新周期决定二维码有效窗口）。
ALTER TABLE ticket_credentials
    ADD COLUMN IF NOT EXISTS refresh_interval_seconds INTEGER NOT NULL DEFAULT 60;
ALTER TABLE ticket_credentials
    ADD CONSTRAINT chk_ticket_credentials_refresh_interval CHECK (refresh_interval_seconds IN (30, 60));
