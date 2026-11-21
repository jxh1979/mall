-- 文件版本：V0.01
-- V736（第十八批 BX 段第 372 条 372-R1）：外部审计、监管账号可查看数据的起止日期（外审数据时段）。
-- 依据：PGM:682「审计人员分两类：内部稽核可查看本综合体全部数据，外部审计/监管只能查看划定的范围与时间段；两类都只能查看、不能修改任何业务数据。」
--   第十八批裁决 77 所选选项原文：「给外审开权限时必须划定数据起止日期，外审账号在各清单、明细和导出里只看得到这段时间内发生的记录；
--   没有发生日期的资料（规则设置、设备台账、平面图等）照常显示」。
-- 一个外部人员账号一行，起止两端都含（北京时间自然日）。不设划定人、划定时间列：谁在何时划定、改前改后以留痕
-- access.audit_data_window.set 为准。研发阶段无存量（UD-07），不回填。

CREATE TABLE audit_data_windows (
    user_id VARCHAR(64) PRIMARY KEY REFERENCES access_users(id),
    data_from DATE NOT NULL,
    data_to DATE NOT NULL,
    CONSTRAINT chk_adw_range CHECK (data_from <= data_to)
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON audit_data_windows TO complex_app';
    END IF;
END $$;
