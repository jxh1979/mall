-- 文件版本：V0.01
-- 第十八批 AC 段第 210 条 210-R1：费用台账来源加 auto_payroll（工资单月结人工成本与派驻、营销人员提成拨付自动挂接进人工类）。
-- 依据 BSM:1801「连接工资单月结，拿到这期的人工成本」；BSM:1774 人工成本「含薪资、销售提成、绩效、奖金与社保，并含主理人本人的报酬」。
ALTER TABLE merchant_expense_entries DROP CONSTRAINT IF EXISTS chk_mex_source;
ALTER TABLE merchant_expense_entries ADD CONSTRAINT chk_mex_source
    CHECK (source IN ('auto_energy', 'auto_property', 'auto_rent', 'auto_payroll', 'manual'));
