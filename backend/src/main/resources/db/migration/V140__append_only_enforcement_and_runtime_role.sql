-- §4 对抗审计 P0 #1：append-only 防篡改硬强制（用户裁定：触发器全环境 + REVOKE + 受限运行时 role）。
-- 背景：审计/证据/流水/留痕表此前无任何 DB 层防护（全 139 迁移 0 个 CREATE TRIGGER / 0 REVOKE / 0 受限 role），
--   应用以 superuser/owner 连接，可任意 UPDATE/DELETE 抹改 audit_events / *_ledger / 各审计与事件表；
--   且多处 ON DELETE CASCADE 使"删父业务行"即连带抹除子表留痕。
-- 防护分两层：
--   ① 触发器（全环境，对 superuser/owner 也触发）：拦截常规 UPDATE/DELETE，是测试/生产共同生效的主防线，
--      同时令任何级联删除（CASCADE）在抹除受保护子行时整事务 RAISE 回滚——等价于 CASCADE→RESTRICT 的资金/审计保护，
--      且比脆弱的 FK 改造更鲁棒。
--   ② 受限运行时 role（生产）：REVOKE UPDATE/DELETE，并令应用改用非 owner、无 DDL 权限的 complex_app 连接，
--      防住"DROP 触发器 / SET session_replication_role=replica"这类需 owner/superuser 的绕过。
-- 表分类（依据：逐表解析写入模式 INSERT/DO NOTHING/DO UPDATE + 显式 UPDATE/DELETE）：
--   immutable（纯 INSERT，加 UPDATE+DELETE 双锁）；append-only-intent（幂等 upsert 含 DO UPDATE，仅加 DELETE 锁，
--   并对该 role REVOKE DELETE，保留 UPDATE 供幂等回写）。
-- 排除：带状态机/可变标记的业务表（*_payment_intents 状态、bar_bill_settlements.reversed、customer_points_ledger 余额回写
--   等）不纳入 UPDATE 锁；其中 customer_points_ledger 仍受 DELETE 锁（其被 UPDATE 是既有口径，理想应改追加冲正分录，另议）。

-- ===== 通用阻断触发器函数 =====
CREATE OR REPLACE FUNCTION complex_block_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'COMPLEX_APPEND_ONLY_VIOLATION: % on table % is forbidden (immutable audit/ledger/trace record)',
        TG_OP, TG_TABLE_NAME
        USING ERRCODE = '23514';
END;
$$;

-- ===== 贴触发器（按表是否存在条件创建，兼容已 DROP/改名表）=====
DO $$
DECLARE
    -- 纯不可变：禁 UPDATE 与 DELETE
    immutable_tables text[] := ARRAY[
        'audit_events',
        'evidence_events',
        'customer_stored_value_transactions',
        'deposit_ledger',
        'risk_alert_actions',
        'ar_ap_payments',
        'integration_robot_task_events',
        'terminal_device_status_events',
        'inventory_purchase_request_events',
        'integration_video_access_audits',
        'wecom_access_audits',
        'cam_trueup_lines',
        'cam_trueup_runs',
        'integration_read_ledger',
        'permission_template_usage_logs'
    ];
    -- 幂等 upsert / 被合法 UPDATE：仅禁 DELETE（永久留痕，不可物理删除）
    delete_only_tables text[] := ARRAY[
        'evidence_reviews',
        'evidence_attachments',
        'ar_ap_item_sources',
        'bar_production_ticket_events',
        'open_api_events',
        'inventory_stock_movements',
        'finance_reconciliation_differences',
        'finance_reconciliation_batches',
        'ticket_payment_intents',
        'finance_payment_intents',
        'inventory_trace_handovers',
        'inventory_trace_handover_steps',
        'inventory_trace_handover_exceptions',
        'ticket_refund_lines',
        'customer_points_ledger'
    ];
    t text;
BEGIN
    FOREACH t IN ARRAY immutable_tables LOOP
        IF to_regclass(t) IS NOT NULL THEN
            EXECUTE format('DROP TRIGGER IF EXISTS trg_append_only_ud ON %I', t);
            EXECUTE format(
                'CREATE TRIGGER trg_append_only_ud BEFORE UPDATE OR DELETE ON %I '
                || 'FOR EACH ROW EXECUTE FUNCTION complex_block_mutation()', t);
        END IF;
    END LOOP;
    FOREACH t IN ARRAY delete_only_tables LOOP
        IF to_regclass(t) IS NOT NULL THEN
            EXECUTE format('DROP TRIGGER IF EXISTS trg_append_only_d ON %I', t);
            EXECUTE format(
                'CREATE TRIGGER trg_append_only_d BEFORE DELETE ON %I '
                || 'FOR EACH ROW EXECUTE FUNCTION complex_block_mutation()', t);
        END IF;
    END LOOP;
END;
$$;

-- ===== 受限运行时 role：complex_app（非 owner、无 DDL）=====
-- 角色由迁移以 owner 身份尽力创建；若 owner 无 CREATEROLE（受限生产），则跳过、由运维预置。
-- 口令不写入迁移：运维通过 ALTER ROLE complex_app PASSWORD '...' 配置后，应用以该角色连接（见 application-prod.yml）。
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        BEGIN
            CREATE ROLE complex_app LOGIN NOINHERIT;
        EXCEPTION
            WHEN insufficient_privilege THEN
                RAISE NOTICE 'skip CREATE ROLE complex_app (insufficient privilege); ops must provision the restricted runtime role';
        END;
    END IF;
END;
$$;

-- ===== 授权与 REVOKE（仅当 role 存在）=====
DO $$
DECLARE
    immutable_tables text[] := ARRAY[
        'audit_events','evidence_events','customer_stored_value_transactions','deposit_ledger',
        'risk_alert_actions','ar_ap_payments','integration_robot_task_events','terminal_device_status_events',
        'inventory_purchase_request_events','integration_video_access_audits','wecom_access_audits',
        'cam_trueup_lines','cam_trueup_runs','integration_read_ledger','permission_template_usage_logs'
    ];
    delete_only_tables text[] := ARRAY[
        'evidence_reviews','evidence_attachments','ar_ap_item_sources','bar_production_ticket_events',
        'open_api_events','inventory_stock_movements','finance_reconciliation_differences',
        'finance_reconciliation_batches','ticket_payment_intents','finance_payment_intents',
        'inventory_trace_handovers','inventory_trace_handover_steps','inventory_trace_handover_exceptions',
        'ticket_refund_lines','customer_points_ledger'
    ];
    t text;
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        -- 运行时基础 DML
        EXECUTE 'GRANT USAGE ON SCHEMA public TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO complex_app';
        EXECUTE 'GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO complex_app';
        -- 未来迁移（以 owner 建表）自动对 complex_app 授予同等 DML
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO complex_app';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO complex_app';
        -- append-only 硬强制：不可变表收回 UPDATE+DELETE
        FOREACH t IN ARRAY immutable_tables LOOP
            IF to_regclass(t) IS NOT NULL THEN
                EXECUTE format('REVOKE UPDATE, DELETE ON %I FROM complex_app', t);
            END IF;
        END LOOP;
        -- 永久留痕表收回 DELETE（保留 UPDATE 供幂等回写）
        FOREACH t IN ARRAY delete_only_tables LOOP
            IF to_regclass(t) IS NOT NULL THEN
                EXECUTE format('REVOKE DELETE ON %I FROM complex_app', t);
            END IF;
        END LOOP;
    END IF;
END;
$$;
