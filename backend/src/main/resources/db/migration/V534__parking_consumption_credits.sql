-- 文件版本：V0.01
-- HP-P01 第二组：停车消费抵免（资格、成本分担、退款重核）。
-- 依据：BSM「停车场」章「消费抵免」条（同一客人当日累计判断门槛、每日一次、关联构成资格的消费及其归属经营主体、
--   抵免成本按组成有效消费金额比例由各笔原消费归属经营主体按内部结算价与城市项目公司走普通内部结算应收应付按月结算、
--   同一次抵免不得重复使用、退款后未使用的重核或撤销、已使用的保留）；决策台账 N02、N03；
--   2026-10-21 第十二批操作员裁决补充第 3 条（门票不计入门槛、也不承担）、第 4 条（实际抵掉小时数×每小时内部价；
--   免费时长内离场、未抵掉计费时长的不算用掉抵免、不产生成本）。不回填存量数据。

-- ① 抵免：同一客人、本项目、同一营业日至多一条；门槛与抵免时长在生成时取本项目参数快照。
--    used 后组成冻结：offset_hours＝实际抵掉的计费小时数，hour_price_cent＝首次使用时固化的每小时内部结算价，cost_cent＝两者之积。
CREATE TABLE parking_credits (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    customer_id VARCHAR(64) NOT NULL REFERENCES customers(id),
    business_date DATE NOT NULL,
    threshold_cent BIGINT NOT NULL,
    credit_minutes INTEGER NOT NULL,
    eligible_amount_cent BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL,
    pending_payment_id VARCHAR(64),
    session_id VARCHAR(64) REFERENCES parking_sessions(id),
    offset_hours INTEGER,
    hour_price_cent BIGINT,
    price_entry_id VARCHAR(80),
    cost_cent BIGINT,
    receiver_entity_id VARCHAR(64),
    used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_parking_credits_day UNIQUE (project_id, customer_id, business_date),
    CONSTRAINT chk_pc_status CHECK (status IN ('available', 'used', 'revoked')),
    CONSTRAINT chk_pc_used CHECK ((status = 'used') = (session_id IS NOT NULL AND used_at IS NOT NULL AND offset_hours > 0
        AND hour_price_cent IS NOT NULL AND cost_cent IS NOT NULL AND receiver_entity_id IS NOT NULL))
);
CREATE INDEX idx_parking_credits_project ON parking_credits (project_id, business_date);

-- ② 组成：构成资格的酒吧账单与商户服务点单（门票不计入），一笔消费只进一次抵免；承担方＝消费商户及其合同主体。
CREATE TABLE parking_credit_components (
    credit_id VARCHAR(64) NOT NULL REFERENCES parking_credits(id),
    source_type VARCHAR(24) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL REFERENCES merchants(id),
    bearer_entity_id VARCHAR(64) NOT NULL,
    amount_cent BIGINT NOT NULL,
    consumed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (source_type, source_id),
    CONSTRAINT chk_pcc_source CHECK (source_type IN ('bar_bill', 'service_order')),
    CONSTRAINT chk_pcc_amount CHECK (amount_cent > 0)
);
CREATE INDEX idx_parking_credit_components_credit ON parking_credit_components (credit_id);

-- ③ 使用后的成本分担：同一商户的多笔组成先合并，按组成金额比例四舍五入到分；尾差单列一行（merchant_id 为空），
--    由该笔停车业务的收款主体城市项目公司承担（PGM「全局原则 · 金额精度与税务例外」尾差单独记账留痕）。
--    monthly_id 非空即已并入月度内部结算，不再改动。
CREATE TABLE parking_credit_cost_allocations (
    id VARCHAR(160) PRIMARY KEY,
    credit_id VARCHAR(64) NOT NULL REFERENCES parking_credits(id),
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    merchant_id VARCHAR(64) REFERENCES merchants(id),
    bearer_entity_id VARCHAR(64) NOT NULL,
    component_amount_cent BIGINT NOT NULL,
    share_cent BIGINT NOT NULL,
    cost_month DATE NOT NULL,
    monthly_id VARCHAR(200),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_parking_credit_cost_allocations_month ON parking_credit_cost_allocations (cost_month, project_id) WHERE monthly_id IS NULL;

-- ④ 按月：同一项目、同一承担商户、同一月一笔普通内部结算应收（城市项目公司向承担方收取，钱路 parking_credit_cost）。
CREATE TABLE parking_credit_cost_months (
    id VARCHAR(200) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    merchant_id VARCHAR(64) NOT NULL REFERENCES merchants(id),
    bearer_entity_id VARCHAR(64) NOT NULL,
    receiver_entity_id VARCHAR(64) NOT NULL,
    cost_month DATE NOT NULL,
    amount_cent BIGINT NOT NULL,
    ar_ap_item_id VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_parking_credit_cost_months UNIQUE (project_id, merchant_id, cost_month)
);

-- ⑤ 缴费关联抵免：抵免把应缴抵到 0 时记一笔 0 元已缴（无支付意向），供停车系统按缴费记录放行；
--    缴费时固化每小时内部结算价与实际抵掉小时数，确认后转入抵免；已用在本次停车上的抵免续缴时也带上，实际抵掉小时可为 0
--    （按整次停车累计一分没少收，确认后抵免退回可用）。
ALTER TABLE parking_payments
    ADD COLUMN credit_id VARCHAR(64) REFERENCES parking_credits(id),
    ADD COLUMN credit_offset_hours INTEGER,
    ADD COLUMN credit_hour_price_cent BIGINT,
    ADD COLUMN credit_price_entry_id VARCHAR(80),
    ALTER COLUMN payment_intent_id DROP NOT NULL,
    DROP CONSTRAINT chk_ppay_amount,
    ADD CONSTRAINT chk_ppay_amount CHECK (amount_cent > 0 OR (amount_cent = 0 AND credit_id IS NOT NULL)),
    ADD CONSTRAINT chk_ppay_intent CHECK ((payment_intent_id IS NULL) = (amount_cent = 0)),
    ADD CONSTRAINT chk_ppay_credit CHECK ((credit_id IS NULL) = (credit_offset_hours IS NULL)
        AND (credit_id IS NULL OR (credit_offset_hours >= 0 AND credit_hour_price_cent IS NOT NULL)));

-- ⑥ 停车抵免每小时内部结算价：城市项目公司经营停车而非商户，内部结算价目录条目的提供方可为经营主体（与商户二选一）；
--    草案→他人复核转正沿用目录既有流程（BSM「内部结算价目录」）。
ALTER TABLE internal_price_catalog
    ALTER COLUMN merchant_id DROP NOT NULL,
    ADD COLUMN provider_entity_id VARCHAR(64),
    ADD CONSTRAINT chk_ipc_provider CHECK (num_nonnulls(merchant_id, provider_entity_id) = 1);
CREATE INDEX idx_ipc_provider_entity ON internal_price_catalog (provider_entity_id, category, status) WHERE provider_entity_id IS NOT NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON parking_credits, parking_credit_components,
            parking_credit_cost_allocations, parking_credit_cost_months TO complex_app';
    END IF;
END $$;
