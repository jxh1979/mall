-- R3 修复 P0-4/P0-5：职级 M1-M10 上中下（REQ-60）+ 薪资字段（REQ-61）
-- 背景：R3 迁移级审计确认 staff_levels / level_rank / 薪资字段此前在迁移与代码中零命中，
-- 矩阵"已覆盖"为设计稿声明、未落地。本迁移补齐数据模型，供审批层级（P0-2）按 level_rank 计算。
-- 口径：金额整数分（D1）；币种默认 CNY（D12）；职级只给薪资大范围，具体薪资单独填写（REQ-61）。

-- 职级定义（平台全局参考数据，single_complex_default 全局可见）
CREATE TABLE IF NOT EXISTS staff_levels (
    id VARCHAR(64) PRIMARY KEY,
    main_level VARCHAR(8) NOT NULL,
    sub_level VARCHAR(8) NOT NULL,
    level_rank SMALLINT NOT NULL,
    salary_policy_summary VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_staff_levels_main CHECK (main_level IN ('M1','M2','M3','M4','M5','M6','M7','M8','M9','M10')),
    CONSTRAINT chk_staff_levels_sub CHECK (sub_level IN ('upper','middle','lower')),
    CONSTRAINT chk_staff_levels_rank CHECK (level_rank BETWEEN 1 AND 30),
    CONSTRAINT uq_staff_levels_main_sub UNIQUE (main_level, sub_level),
    CONSTRAINT uq_staff_levels_rank UNIQUE (level_rank)
);

-- 员工职级与具体薪资归属（不改 access_users，避免触碰既有 OpenAPI 契约）
CREATE TABLE IF NOT EXISTS staff_level_assignments (
    user_id VARCHAR(64) PRIMARY KEY REFERENCES access_users(id) ON DELETE CASCADE,
    staff_level_id VARCHAR(64) NOT NULL REFERENCES staff_levels(id),
    base_salary_amount_cent BIGINT,
    salary_currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
    salary_detail TEXT,
    organization_id VARCHAR(64),
    merchant_id VARCHAR(64),
    assigned_by VARCHAR(128),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_staff_salary_currency CHECK (salary_currency = 'CNY'),
    CONSTRAINT chk_staff_salary_nonneg CHECK (base_salary_amount_cent IS NULL OR base_salary_amount_cent >= 0)
);
CREATE INDEX IF NOT EXISTS idx_staff_level_assignments_merchant
    ON staff_level_assignments(merchant_id) WHERE merchant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_staff_level_assignments_org
    ON staff_level_assignments(organization_id) WHERE organization_id IS NOT NULL;

-- 种子 30 个职级：M1..M10 × {lower,middle,upper}
-- level_rank = (n-1)*3 + sub_rank，其中 lower=1 / middle=2 / upper=3（rank 越大越高，上>中>下，M10>M1）
INSERT INTO staff_levels (id, main_level, sub_level, level_rank, salary_policy_summary) VALUES
('lvl_m1_lower','M1','lower',1,'M1 薪资大范围'),
('lvl_m1_middle','M1','middle',2,'M1 薪资大范围'),
('lvl_m1_upper','M1','upper',3,'M1 薪资大范围'),
('lvl_m2_lower','M2','lower',4,'M2 薪资大范围'),
('lvl_m2_middle','M2','middle',5,'M2 薪资大范围'),
('lvl_m2_upper','M2','upper',6,'M2 薪资大范围'),
('lvl_m3_lower','M3','lower',7,'M3 薪资大范围'),
('lvl_m3_middle','M3','middle',8,'M3 薪资大范围'),
('lvl_m3_upper','M3','upper',9,'M3 薪资大范围'),
('lvl_m4_lower','M4','lower',10,'M4 薪资大范围'),
('lvl_m4_middle','M4','middle',11,'M4 薪资大范围'),
('lvl_m4_upper','M4','upper',12,'M4 薪资大范围'),
('lvl_m5_lower','M5','lower',13,'M5 薪资大范围'),
('lvl_m5_middle','M5','middle',14,'M5 薪资大范围'),
('lvl_m5_upper','M5','upper',15,'M5 薪资大范围'),
('lvl_m6_lower','M6','lower',16,'M6 薪资大范围'),
('lvl_m6_middle','M6','middle',17,'M6 薪资大范围'),
('lvl_m6_upper','M6','upper',18,'M6 薪资大范围'),
('lvl_m7_lower','M7','lower',19,'M7 薪资大范围'),
('lvl_m7_middle','M7','middle',20,'M7 薪资大范围'),
('lvl_m7_upper','M7','upper',21,'M7 薪资大范围'),
('lvl_m8_lower','M8','lower',22,'M8 薪资大范围'),
('lvl_m8_middle','M8','middle',23,'M8 薪资大范围'),
('lvl_m8_upper','M8','upper',24,'M8 薪资大范围'),
('lvl_m9_lower','M9','lower',25,'M9 薪资大范围'),
('lvl_m9_middle','M9','middle',26,'M9 薪资大范围'),
('lvl_m9_upper','M9','upper',27,'M9 薪资大范围'),
('lvl_m10_lower','M10','lower',28,'M10 薪资大范围'),
('lvl_m10_middle','M10','middle',29,'M10 薪资大范围'),
('lvl_m10_upper','M10','upper',30,'M10 薪资大范围')
ON CONFLICT (id) DO NOTHING;
