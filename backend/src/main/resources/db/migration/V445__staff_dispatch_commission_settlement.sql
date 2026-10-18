-- CG-07 派驻/营销人员提成：酒吧按月拨付所属主体（内部结算应收应付）、列入所属主体工资单。
-- 依据：BSM「营销人员的归属与报酬」：「营销在某家酒吧促成的订单产生的提成，由该酒吧按月拨付给娱乐平台公司，列入营销本人的工资单由娱乐平台公司
--       统一发放并代扣个人所得税；拨付作为平台内部各主体之间的普通内部结算应收应付，按三个付账日结清」；
--       BSM「派驻人员的成本与报酬」：「其中应付给本人的提成、服务费等报酬，由该酒吧按月拨付给人员所属主体，列入该人员的工资单由人员所属主体
--       统一发放并代扣个人所得税……每一名派驻人员的所属主体、派驻酒吧、派驻起止时间，以及由人员所属主体支付和由酒吧承担并拨付的各项金额，
--       均须分别记录、可分别统计，不得只保存合并后的净额」；
--       BSM「会员与积分规则·积分结算账期」：「付账日为业务参数，全平台统一可配，初始为每月 5 日、15 日、25 日」。

-- 1) 派驻记录：员工、所属主体、派驻酒吧（商户）、起止时间
CREATE TABLE IF NOT EXISTS staff_dispatch_records (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,              -- 员工账号（access_users.id）
    company_id VARCHAR(64) NOT NULL,           -- 人员所属主体（登记时取自员工档案 staff_profiles.company_id）
    merchant_id VARCHAR(64) NOT NULL,          -- 派驻酒吧（商户）
    project_id VARCHAR(64),
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    note VARCHAR(255),
    created_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_by VARCHAR(64),
    ended_at TIMESTAMPTZ,
    CONSTRAINT chk_staff_dispatch_status CHECK (status IN ('active', 'ended')),
    CONSTRAINT chk_staff_dispatch_dates CHECK (end_date IS NULL OR end_date >= start_date)
);
CREATE INDEX IF NOT EXISTS idx_staff_dispatch_user ON staff_dispatch_records (user_id, start_date DESC);
CREATE INDEX IF NOT EXISTS idx_staff_dispatch_merchant ON staff_dispatch_records (merchant_id, status);
COMMENT ON TABLE staff_dispatch_records IS '派驻记录：员工、所属主体、派驻酒吧、派驻起止时间（BSM「派驻人员的成本与报酬」）';

-- 2) 提成月结内部结算：按（酒吧商户 → 人员所属主体 → 月）汇总派驻人员提成，生成一条内部结算应付
CREATE TABLE IF NOT EXISTS bar_commission_settlements (
    id VARCHAR(64) PRIMARY KEY,
    month VARCHAR(7) NOT NULL,                 -- YYYY-MM
    merchant_id VARCHAR(64) NOT NULL,          -- 承担并拨付的酒吧
    company_id VARCHAR(64) NOT NULL,           -- 收款的人员所属主体
    project_id VARCHAR(64),
    amount_cent BIGINT NOT NULL,
    calculation_count INTEGER NOT NULL,
    staff_count INTEGER NOT NULL,
    ar_ap_item_id VARCHAR(64),
    due_date DATE NOT NULL,
    created_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_bar_commission_settlement_amount CHECK (amount_cent >= 0),
    CONSTRAINT uq_bar_commission_settlement UNIQUE (month, merchant_id, company_id)
);
COMMENT ON TABLE bar_commission_settlements IS '派驻人员提成月结：酒吧→所属主体内部结算应付（总额收付、不轧差；BSM「营销人员的归属与报酬」「派驻人员的成本与报酬」）';

-- 3) 工资单发放主体（人员所属主体统一发放并代扣个税）
ALTER TABLE staff_payroll_statements ADD COLUMN IF NOT EXISTS payer_company_id VARCHAR(64);
COMMENT ON COLUMN staff_payroll_statements.payer_company_id IS '工资单发放主体＝人员所属主体（staff_profiles.company_id）';

-- 4) 平台内部各主体之间的三个付账日（业务参数，全平台统一可配；初始 5 / 15 / 25）
CREATE TABLE IF NOT EXISTS internal_settlement_pay_days (
    scope_id VARCHAR(32) PRIMARY KEY,
    day_1 INTEGER NOT NULL,
    day_2 INTEGER NOT NULL,
    day_3 INTEGER NOT NULL,
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_internal_pay_days_range CHECK (day_1 BETWEEN 1 AND 28 AND day_2 BETWEEN 1 AND 28 AND day_3 BETWEEN 1 AND 28),
    CONSTRAINT chk_internal_pay_days_order CHECK (day_1 < day_2 AND day_2 < day_3)
);
INSERT INTO internal_settlement_pay_days (scope_id, day_1, day_2, day_3) VALUES ('global', 5, 15, 25)
ON CONFLICT (scope_id) DO NOTHING;
COMMENT ON TABLE internal_settlement_pay_days IS '平台内部各主体之间应收应付的三个付账日（全平台统一；BSM「会员与积分规则·积分结算账期」初始 5/15/25）';
