-- 文件版本：V0.01
-- 第 23 条 23-R1 浏览器设备编号后台登记。第十八批裁决 6：「浏览器第一次登录时系统发一个设备编号存进这个浏览器并在后台登记，
-- 导出密码和页面暗纹用这个编号（清浏览器数据或换浏览器算新设备），不用装软件」。
-- device_no＝发给浏览器的编号（Cookie complex_device_no）；first_user_id＝第一次拿到该编号登录的账号。只增不改。

CREATE TABLE IF NOT EXISTS browser_devices (
    device_no VARCHAR(32) PRIMARY KEY,
    first_user_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMENT ON TABLE browser_devices IS '浏览器设备编号登记（第十八批裁决 6）：网页第一次登录时发号存进浏览器，导出密码与页面暗纹用它';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT ON browser_devices TO complex_app';
    END IF;
END $$;
