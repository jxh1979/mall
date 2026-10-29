-- 文件版本：V0.01
-- 第 23 条 23-R2 在线页面暗纹与截屏、拍屏反查。PGM:687「后台、商户专区和员工手机端的所有在线页面都带有按同一规则生成的暗纹；暗纹可反向推算：
-- 把页面的截屏图片、或对着屏幕拍摄的照片上传到系统，即可算出该页面是由哪个账号、在哪一台设备上、于什么时间打开的」「打开敏感页这件事本身也要留痕……
-- 这条留痕不提供关闭开关」。
-- 每打开一次页面（kind=page）或生成一份导出文件（kind=export，23-R3）落一行：code 是暗纹二维码里的 8 位码，反查按它找回账号、设备编号
-- （浏览器设备编号或员工手机登记设备号，第十八批裁决 6）、接入网络与时间。留痕只增不改不删。

CREATE TABLE IF NOT EXISTS watermark_marks (
    code VARCHAR(12) PRIMARY KEY,
    kind VARCHAR(16) NOT NULL CHECK (kind IN ('page', 'export')),
    account_id VARCHAR(64) NOT NULL,
    device_no VARCHAR(128) NOT NULL,
    network VARCHAR(64),
    page_id VARCHAR(64),
    export_file_id VARCHAR(64),
    sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    object_refs JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_watermark_marks_account ON watermark_marks (account_id, created_at DESC);

CREATE TRIGGER trg_append_only_ud BEFORE UPDATE OR DELETE ON watermark_marks
    FOR EACH ROW EXECUTE FUNCTION complex_block_mutation();

COMMENT ON TABLE watermark_marks IS '暗纹码留痕（第 23 条）：页面打开与导出文件各一行，按码反查账号、设备、网络与时间；只增不改不删';

-- 反查权限：PGM:666、DEC-R3B-2026-08-04-10 未归类的新功能按高危处理、逐项单独授予，不自动放给平台管理员（不写 role_permissions）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('security.watermark.trace', '暗纹与导出密码反查', 'platform', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT ON watermark_marks TO complex_app';
    END IF;
END $$;
