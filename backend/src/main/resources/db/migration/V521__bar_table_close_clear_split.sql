-- 文件版本：V0.01
-- V521：TK-26 部分送达留痕、TK-27 关台/清台/拆台（第十批）。
-- 依据：BSM L1025「分别标注已上菜与未上菜」、ENUM L112 出品票据状态流转（部分送达只记事件、票据不置已送达）；
--   BSM L1058「待清台超过后台配置时限（业务参数，按酒吧可配，出厂默认 15 分钟，须同时配置上下限）未恢复空闲的，系统提醒当班主管」；V267「酒吧门店就是商户本身」；
--   BSM L1079 拆台「由授权岗位操作」「每次拆单、合单、转台、并台、拆台……均须留痕」；裁决 DEC-R6-19、DEC-R8-95。
-- 铁律 8：不为存量数据回填；只扩闭集、加列带默认。

-- 1) 出品票据事件：部分送达事件 partial_deliver（from=to=票据当前状态；新票初始状态 created 也要能落 to_status）。
ALTER TABLE bar_production_ticket_events DROP CONSTRAINT IF EXISTS chk_bar_production_ticket_event_action;
ALTER TABLE bar_production_ticket_events ADD CONSTRAINT chk_bar_production_ticket_event_action CHECK (
    action_type IN ('accept', 'start_prepare', 'mark_ready', 'cancel', 'exception', 'deliver', 'partial_deliver')
);
ALTER TABLE bar_production_ticket_events DROP CONSTRAINT IF EXISTS chk_bar_production_ticket_event_status;
ALTER TABLE bar_production_ticket_events ADD CONSTRAINT chk_bar_production_ticket_event_status CHECK (
    to_status IN ('created', 'accepted', 'preparing', 'ready', 'cancelled', 'exception', 'delivered')
);

-- 2) 台位会话调整类型闭集扩 split（拆台留痕）。
ALTER TABLE bar_table_session_adjustments DROP CONSTRAINT IF EXISTS chk_bar_table_adjustment_type;
ALTER TABLE bar_table_session_adjustments ADD CONSTRAINT chk_bar_table_adjustment_type CHECK (
    adjustment_type IN ('transfer', 'merge', 'renew', 'split')
);

-- 3) 拆台权限（范式同 V019）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('bar.table_session.split', '拆分酒吧桌台会话', 'bar', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code = 'bar.table_session.split'
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- 4) 待清台提醒时限：按酒吧可配。酒吧＝merchant_type='bar' 的商户（V267 裁定），参数落 merchants 列；
--    按仓库「值与上下限同配」范式（V444 stored_value_bonus_policies 同款）：未配置＝三列皆空，按出厂 15 分钟；
--    配置时值、下限、上限三者同填且值落在闭区间内（不另设硬上下限）。
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS pending_clear_alert_minutes INTEGER;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS pending_clear_alert_min_minutes INTEGER;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS pending_clear_alert_max_minutes INTEGER;
ALTER TABLE merchants DROP CONSTRAINT IF EXISTS chk_merchants_pending_clear_alert_minutes;
ALTER TABLE merchants ADD CONSTRAINT chk_merchants_pending_clear_alert_minutes CHECK (
    (pending_clear_alert_minutes IS NULL AND pending_clear_alert_min_minutes IS NULL AND pending_clear_alert_max_minutes IS NULL)
    OR (pending_clear_alert_minutes IS NOT NULL AND pending_clear_alert_min_minutes IS NOT NULL AND pending_clear_alert_max_minutes IS NOT NULL
        AND pending_clear_alert_min_minutes >= 0
        AND pending_clear_alert_min_minutes <= pending_clear_alert_minutes
        AND pending_clear_alert_minutes <= pending_clear_alert_max_minutes)
);
COMMENT ON COLUMN merchants.pending_clear_alert_minutes IS '酒吧台位待清台超过该分钟数未恢复空闲即标记超时（未配置＝空，按出厂 15；与上下限同配）';
COMMENT ON COLUMN merchants.pending_clear_alert_min_minutes IS '待清台提醒时限已配置下限（分钟，与值、上限同配）';
COMMENT ON COLUMN merchants.pending_clear_alert_max_minutes IS '待清台提醒时限已配置上限（分钟，与值、下限同配）';

-- 5) 待清台起点单独留痕（复核 money m4）：进入待清台时写入、离开待清台清空；编辑台位资料（updated_at 变化）不重置计时。
ALTER TABLE bar_tables ADD COLUMN IF NOT EXISTS pending_clear_since TIMESTAMPTZ;
COMMENT ON COLUMN bar_tables.pending_clear_since IS '台位进入待清台的时刻（status=pending_clear 时有值；超时提醒按此计时）';
