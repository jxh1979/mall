-- V159 A6 数据完整性硬约束（第一阶段审计发现 #6）：账平 CHECK + 关键血缘外键 + 资金决策表 append-only。
-- 只读校验已确认：ar_ap_items 账平 0 违例（表空）；issued_entitlements/ticket_verifications 存量 android_p0
--   冒烟孤儿（ticket_orders 表空致整链悬空，纯 dev 测试数据）。
-- 外键一律 NOT VALID + ON DELETE RESTRICT：放行存量 dev 冒烟孤儿、对**新写**强制引用完整性 + 删主表 RESTRICT 防悬挂；
--   生产环境数据干净 → 等价全效（后续可在干净环境 `VALIDATE CONSTRAINT` 收紧为全量校验）。
-- append-only：复用 V140 complex_block_mutation() 通用阻断函数，对尚未保护的资金决策表加 DELETE 锁（永久留痕；
--   这些表仅 INSERT / 幂等 ON CONFLICT DO UPDATE，无合法 DELETE 路径）。ticket_refund_lines / finance_daily_closes /
--   supervisor_review_decisions 已由 V031/V149/V148 保护，不重复。
-- 范围说明：ticket_verifications.point_id（sp_* 逻辑扫描点、无目标主表）与 bar_id（无 bars 主表）目标不明确，本迁移不纳入。

-- ① ar_ap_items 账平恒等：已付 + 未付 = 总额（破坏即拒写）。
ALTER TABLE ar_ap_items
    ADD CONSTRAINT chk_ar_ap_items_amount_balance
    CHECK (paid_amount_cent + unpaid_amount_cent = amount_cent);

-- ② 关键血缘外键（NOT VALID + ON DELETE RESTRICT）。
ALTER TABLE ticket_credentials
    ADD CONSTRAINT fk_ticket_credentials_order
    FOREIGN KEY (order_id) REFERENCES ticket_orders (id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE issued_entitlements
    ADD CONSTRAINT fk_issued_entitlements_order
    FOREIGN KEY (order_id) REFERENCES ticket_orders (id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ticket_verifications
    ADD CONSTRAINT fk_ticket_verifications_entitlement
    FOREIGN KEY (entitlement_id) REFERENCES issued_entitlements (id) ON DELETE RESTRICT NOT VALID;
ALTER TABLE ticket_verifications
    ADD CONSTRAINT fk_ticket_verifications_device
    FOREIGN KEY (device_id) REFERENCES terminal_devices (id) ON DELETE RESTRICT NOT VALID;

-- ③ 资金决策表 append-only（DELETE 锁，复用 V140 触发器函数 + 同名 trg_append_only_d 约定）。
DO $$
DECLARE
    decision_tables text[] := ARRAY[
        'ar_ap_adjustments',
        'ticket_refunds',
        'bar_bill_adjustments',
        'bar_order_item_voids'
    ];
    t text;
BEGIN
    FOREACH t IN ARRAY decision_tables LOOP
        IF to_regclass('public.' || t) IS NOT NULL THEN
            EXECUTE format('DROP TRIGGER IF EXISTS trg_append_only_d ON %I', t);
            EXECUTE format(
                'CREATE TRIGGER trg_append_only_d BEFORE DELETE ON %I '
                || 'FOR EACH ROW EXECUTE FUNCTION complex_block_mutation()', t);
        END IF;
    END LOOP;
END;
$$;
