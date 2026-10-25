-- 文件版本：V0.01
-- V545：OBS-13-03 订台相关短信（第十四批）。
-- 依据：BSM L1596「给客人：……订台成功确认（员工代订的同时向客人知会代订人与订台内容）、最晚到店时间前提醒（提前量业务参数，按项目可配，
--   出厂默认 30 分钟，须同时配置上下限）、超时释放台位通知、定金没收通知、定金原路退回通知、订台取消确认」；BSM L1598「管理员可在后台
--   自行决定哪些提醒发短信」；DEC-R8-97-20。不回填存量数据。

-- ① 员工代订录入的客人手机号加密留存（员工代订没有会员编号，订台短信唯一收件人来源；裁决第十三批第 7 条员工代订必须录入客人手机号）。
ALTER TABLE bar_reservations ADD COLUMN IF NOT EXISTS customer_phone TEXT;
ALTER TABLE bar_reservations DROP CONSTRAINT IF EXISTS chk_bar_reservations_customer_phone_pii;
ALTER TABLE bar_reservations
    ADD CONSTRAINT chk_bar_reservations_customer_phone_pii
    CHECK (customer_phone IS NULL OR customer_phone = '' OR customer_phone LIKE 'enc:v1:%');

-- ② 最晚到店时间前提醒提前量（分钟，按项目；项目无自有行即用 '__global__' 出厂行 30 分钟；配置时值与上下限同填）。
CREATE TABLE IF NOT EXISTS bar_arrival_reminder_policies (
    project_id VARCHAR(64) PRIMARY KEY,
    remind_minutes INTEGER NOT NULL,
    min_minutes INTEGER,
    max_minutes INTEGER,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_bar_arrival_reminder_minutes CHECK (remind_minutes >= 1),
    CONSTRAINT chk_bar_arrival_reminder_bounds CHECK (
        (min_minutes IS NULL AND max_minutes IS NULL)
        OR (min_minutes IS NOT NULL AND max_minutes IS NOT NULL
            AND min_minutes >= 1 AND min_minutes <= remind_minutes AND remind_minutes <= max_minutes))
);
INSERT INTO bar_arrival_reminder_policies (project_id, remind_minutes) VALUES ('__global__', 30)
ON CONFLICT (project_id) DO NOTHING;

-- ③ 订台短信开关（域＋六类事件，照 V177；受 sms_notification 总开关管）。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_sms_notify_bar_reservation_domain', '*', 'sms.notify.bar_reservation', 'global', 'global', TRUE,
     '订台短信域开关（BSM L1596）', now(), now()),
    ('tcs_sms_notify_bar_reservation_confirmed', '*', 'sms.notify.bar_reservation.confirmed', 'global', 'global', TRUE,
     '订台成功确认', now(), now()),
    ('tcs_sms_notify_bar_reservation_arrival_reminder', '*', 'sms.notify.bar_reservation.arrival_reminder', 'global', 'global', TRUE,
     '最晚到店时间前提醒', now(), now()),
    ('tcs_sms_notify_bar_reservation_timeout_released', '*', 'sms.notify.bar_reservation.timeout_released', 'global', 'global', TRUE,
     '超时释放台位通知', now(), now()),
    ('tcs_sms_notify_bar_reservation_cancelled', '*', 'sms.notify.bar_reservation.cancelled', 'global', 'global', TRUE,
     '订台取消确认', now(), now()),
    ('tcs_sms_notify_bar_reservation_deposit_forfeited', '*', 'sms.notify.bar_reservation.deposit_forfeited', 'global', 'global', TRUE,
     '定金没收通知', now(), now()),
    ('tcs_sms_notify_bar_reservation_deposit_refunded', '*', 'sms.notify.bar_reservation.deposit_refunded', 'global', 'global', TRUE,
     '定金原路退回通知', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON bar_arrival_reminder_policies TO complex_app';
    END IF;
END $$;
