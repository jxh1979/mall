-- V369 综合／联合通票售价自动算 + 凑整误差闸(用户 2026-07-28 拍板)。
--
-- 【业务依据】docs/business-system-map-for-operations-v0.1.html:741 逐字:
--     「综合／联合通票的定价构成:售价由各业态价格相加后统一打折得出——游乐业态价格 + 酒吧业态价格,
--       若餐饮／零售权益是收费的则一并加入,免费的(价格为 0)不计入价值;相加后按票种配置的折扣,
--       由系统自动算出售价。运营可在此基础上填一个近似售价用于凑整(使最终售价为整数、且尾数为 8、9 或 0),
--       上调下调均可,但与系统算出的售价之间的差额不得超过后台配置的允许误差,超出即拒绝发布;
--       允许误差后台可配,凑整产生的差额按既有折扣承担口径处理。」
--   同 docs/business-enum-quick-reference-v0.1.html「综合通票定价」行。
--
-- 【现在为什么会出错(业务人员角度)】
--   运营在票种上直接填一个售价,又在每条权益上分别填票面原价,两处各填各的、系统从不比对。
--   填成 179 而不是 197 时没有任何人会发现——票照卖,少收的那 18 块在月度结算里被当成「套票折扣」,
--   默认由项目公司吃掉。等到财务发现毛利不对,票已经卖出去几百张,只能人工调账。
--   本迁移给系统装上这两样东西:①一个能自己算出售价的折扣;②一个「差多少才算填错」的可配阈值。
--
-- 【① ticket_types.combo_discount_rate】票种折扣(百分数,100=不打折,85=八五折)。
--   系统算出售价 = Σ(各权益票面原价 × 份数) × 折扣 ÷ 100,四舍五入到分(同 MoneyRounding 全场口径)。
--   免费权益票面原价填 0 → 自然不计入价值,与 S11 的 NULL/0 语义一致(NULL=没配价,发布时已被拒)。
--   默认 100 = 不打折 = 算出售价恰为各业态票面原价之和。
--
-- 【② ticket_combo_discount_configs.rounding_tolerance_cent】凑整允许误差(分)。
--   刻意挂在「套票折扣归属」这张配置表上,而不是另起一张:
--   凑整产生的差额走的就是套票折扣那条口径(满额−净额,由 discount_bearer 决定谁承担),
--   同一个项目公司、同一个「通票分账」栏目、同一位复核人、同一套草案转正闸——运营只需在一处维护。
--   出厂默认 500 分(5 元):从任意算出价凑到最近的「整元且尾数 8/9/0」最多差 4 元(190↔198 之间那段最宽),
--   5 元恰好够凑整、又远小于一次真正的填错(197→179 差 18 元),不会把该拦的放过去。
--
-- 【零金额影响】本迁移只加两列 + 两条 CHECK,不改任何既有金额列、不回填任何历史数据
--   (.160 为开发环境,库内数据最后全删,不做存量兼容设计)。

-- ① 票种折扣(综合／联合通票售价自动算的折扣项)
ALTER TABLE ticket_types
    ADD COLUMN IF NOT EXISTS combo_discount_rate NUMERIC(5,2) NOT NULL DEFAULT 100;
ALTER TABLE ticket_types DROP CONSTRAINT IF EXISTS chk_tt_combo_discount_rate;
ALTER TABLE ticket_types ADD CONSTRAINT chk_tt_combo_discount_rate
    CHECK (combo_discount_rate >= 0 AND combo_discount_rate <= 100);

COMMENT ON COLUMN ticket_types.combo_discount_rate IS
    '票种折扣(百分数,100=不打折):系统算出售价 = Σ(各权益票面原价 × 份数) × 本折扣 ÷ 100';

-- ② 凑整允许误差(后台可配,复用套票折扣归属配置表的草案转正闸)
ALTER TABLE ticket_combo_discount_configs
    ADD COLUMN IF NOT EXISTS rounding_tolerance_cent BIGINT NOT NULL DEFAULT 500;
ALTER TABLE ticket_combo_discount_configs DROP CONSTRAINT IF EXISTS chk_tcdc_rounding_tolerance;
ALTER TABLE ticket_combo_discount_configs ADD CONSTRAINT chk_tcdc_rounding_tolerance
    CHECK (rounding_tolerance_cent >= 0);

COMMENT ON COLUMN ticket_combo_discount_configs.rounding_tolerance_cent IS
    '凑整允许误差(分):运营填的近似售价与系统算出售价的差额上限,超出即拒绝发布';
