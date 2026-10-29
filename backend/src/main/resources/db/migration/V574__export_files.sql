-- 文件版本：V0.01
-- 第 23 条 23-R3 导出文件一律带密码、不可复制不可编辑、带暗纹。PGM:687「所有导出的文件一律带密码，密码不可取消、不可由导出人自行设定。
-- 密码由导出人账号、导出时刻、导出所用设备的唯一识别号……以及接入网络等可查证要素按统一算法生成，长度不超过十位，且可由系统反向推算出对应的
-- 导出人、导出时间与导出设备；导出人可以在专门的地方查到自己导出文件的密码」；PGM:686「每一次导出——不论脱敏与否——都记录导出人、导出时间、
-- 导出所用设备、导出内容范围与筛选条件，导出记录本身永久保存、不在任何清理范围内」。
-- 每生成一份导出文件落一行（只增不改不删）：密码与文件本身都不落库，只存密码摘要（SHA-256）供反查；「我的导出」按行内要素重算密码。
-- device_no 为浏览器设备编号或员工手机登记设备号（第十八批裁决 6）；filters 同时记请求筛选参数与本次取数所按的范围；
-- mark_code 是文件每页暗纹里的码（watermark_marks.kind='export'）；editable、source_export_id、review_id 供可编辑版（23-R4）。

CREATE TABLE IF NOT EXISTS export_files (
    id VARCHAR(64) PRIMARY KEY,
    exporter_id VARCHAR(64) NOT NULL,
    exported_at TIMESTAMPTZ NOT NULL,
    device_no VARCHAR(128) NOT NULL,
    network VARCHAR(64),
    export_type VARCHAR(64) NOT NULL CHECK (export_type IN (
        'analytics_monthly_report', 'finance_ar_ap_ledger', 'audit_operation_events', 'settlement_invoice_items')),
    filters JSONB NOT NULL,
    row_count INT NOT NULL,
    password_digest CHAR(64) NOT NULL UNIQUE,
    mark_code VARCHAR(12) NOT NULL,
    editable BOOLEAN NOT NULL DEFAULT FALSE,
    source_export_id VARCHAR(64),
    review_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_export_files_exporter ON export_files (exporter_id, exported_at DESC);

CREATE TRIGGER trg_append_only_ud BEFORE UPDATE OR DELETE ON export_files
    FOR EACH ROW EXECUTE FUNCTION complex_block_mutation();

COMMENT ON TABLE export_files IS '导出文件记录（第 23 条）：导出人、时间、设备、网络、内容与筛选、行数、密码摘要与暗纹码；永久保存，只增不改不删';

-- 开票清单导出：PGM:685「导出须单独授权、逐次留痕」，PGM:666、DEC-R3B-2026-08-04-10 未归类的新功能按高危处理、逐项单独授予，
-- 不预授任何角色（不写 role_permissions），经既有临时、单次授权取得。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('settlement.invoice.export', '导出开票清单', 'settlement', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT ON export_files TO complex_app';
    END IF;
END $$;
