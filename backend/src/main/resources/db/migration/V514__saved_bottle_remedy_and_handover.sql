-- 文件版本：V0.01
-- FS-017 存酒作废后对客补存/赔付、门店退出交接、责任归属与内部追收（第九批）。依据 BSM L1072 / ENUM L120：
-- ①「由原门店承担的作废酒品价值按内部结算价计入该酒吧的经营损失，经复核认定责任在具体员工的，属员工损坏店内物品、照价赔偿」
-- ②「存酒作废的，由该酒吧为客人以同品、同等剩余量补存，到期日不变，补酒成本计入该酒吧的经营损失」
-- ③「退出前清点存酒并通知客人在原店取回；无法交还的，优先以同品、同等剩余量替代；无法替代的，双方确认赔付方案，经门店主管和财务审批后支付。
--    内部报损、员工赔偿与对客处理分开，员工未赔付不能阻止对客交付或赔付；实物或赔款实际交付后才结案」
-- ④「酒吧退出后，所属娱乐平台公司接收仍在保管有效期内的未结存酒实物和存酒记录，并指定领取地点，继续按原门店的保管规则和原有效期处理……
--    接收时已存在的毁损、短少及原店应承担的替代赔付费用，仍由原门店承担，该娱乐平台公司可经审批先垫付并向原门店追收；接收后因该娱乐平台公司
--    保管造成的损失由该公司承担。不得因内部追收未完成拖延对客处理。对客实际交付结案与内部追收状态分别记录」。

-- 1) 存酒：补存瓶回指原作废瓶；门店退出交平台后的保管主体（business_entities 松引用，范式同 V318）、指定领取地点、交接时间与交接单。
ALTER TABLE saved_bottles
    ADD COLUMN IF NOT EXISTS remedy_of_saved_bottle_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS custodian_entity_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS pickup_location VARCHAR(255),
    ADD COLUMN IF NOT EXISTS handed_over_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS handover_id VARCHAR(64);
-- 取酒流水：交平台后的取酒地点快照（BSM L867「并在指定地点领取」）。
ALTER TABLE saved_bottle_claims ADD COLUMN IF NOT EXISTS pickup_location VARCHAR(255);

-- 2) 对客补存/赔付记录：对客交付状态（customer_delivery_status）与内部追收状态（recovery_status）分别记录、互不约束。
CREATE TABLE IF NOT EXISTS saved_bottle_remedies (
    id VARCHAR(64) PRIMARY KEY,
    saved_bottle_id VARCHAR(64) NOT NULL REFERENCES saved_bottles(id),
    merchant_id VARCHAR(64),
    project_id VARCHAR(64),
    remedy_type VARCHAR(16) NOT NULL,
    replacement_saved_bottle_id VARCHAR(64),
    amount_cent BIGINT,
    loss_cost_cent BIGINT,
    loss_ar_ap_item_id VARCHAR(64),
    bearer_type VARCHAR(32) NOT NULL,
    bearer_id VARCHAR(64),
    supervisor_approved_by VARCHAR(128),
    supervisor_approved_at TIMESTAMPTZ,
    finance_approved_by VARCHAR(128),
    finance_approved_at TIMESTAMPTZ,
    customer_delivery_status VARCHAR(32) NOT NULL,
    delivered_at TIMESTAMPTZ,
    delivery_evidence_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    reason VARCHAR(500),
    recovery_party_type VARCHAR(32) NOT NULL DEFAULT 'none',
    recovery_party_id VARCHAR(128),
    recovery_amount_cent BIGINT,
    recovery_status VARCHAR(16) NOT NULL DEFAULT 'none',
    recovery_ar_ap_item_id VARCHAR(64),
    advance_approved_by VARCHAR(128),
    advance_approved_at TIMESTAMPTZ,
    recovery_evidence_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_by VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_saved_bottle_remedies_type CHECK (remedy_type IN ('replacement', 'compensation')),
    CONSTRAINT chk_saved_bottle_remedies_amount CHECK (remedy_type <> 'compensation' OR (amount_cent IS NOT NULL AND amount_cent > 0)),
    CONSTRAINT chk_saved_bottle_remedies_bearer CHECK (bearer_type IN ('original_merchant', 'platform_company')),
    CONSTRAINT chk_saved_bottle_remedies_delivery_status
        CHECK (customer_delivery_status IN ('pending_approval', 'approved', 'closed')),
    CONSTRAINT chk_saved_bottle_remedies_recovery_party CHECK (recovery_party_type IN ('none', 'original_merchant', 'staff')),
    CONSTRAINT chk_saved_bottle_remedies_recovery_status CHECK (recovery_status IN ('none', 'pending', 'recovered')),
    -- 审批人≠登记人（范式同 V508 chk_ipdr_sod）；门店主管与财务两道审批不得是同一人（BSM L1072「经门店主管和财务审批后支付」）。
    CONSTRAINT chk_saved_bottle_remedies_sod CHECK (
        (supervisor_approved_by IS NULL OR supervisor_approved_by <> created_by)
        AND (finance_approved_by IS NULL OR finance_approved_by <> created_by)
        AND (supervisor_approved_by IS NULL OR finance_approved_by IS NULL OR supervisor_approved_by <> finance_approved_by))
);
CREATE INDEX IF NOT EXISTS idx_saved_bottle_remedies_bottle ON saved_bottle_remedies (saved_bottle_id, created_at);
CREATE INDEX IF NOT EXISTS idx_saved_bottle_remedies_merchant_status ON saved_bottle_remedies (merchant_id, customer_delivery_status);
CREATE INDEX IF NOT EXISTS idx_saved_bottle_remedies_recovery_item ON saved_bottle_remedies (recovery_ar_ap_item_id);

-- 3) 门店退出存酒交接单（由所属娱乐平台公司接收；逐瓶实收数量与状态）。
CREATE TABLE IF NOT EXISTS saved_bottle_handovers (
    id VARCHAR(64) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64),
    platform_entity_id VARCHAR(64) NOT NULL,
    pickup_location VARCHAR(255) NOT NULL,
    counted_total INTEGER NOT NULL DEFAULT 0,
    received_intact INTEGER NOT NULL DEFAULT 0,
    damaged_or_short INTEGER NOT NULL DEFAULT 0,
    photo_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    video_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_by VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_saved_bottle_handovers_merchant ON saved_bottle_handovers (merchant_id, created_at DESC);
CREATE TABLE IF NOT EXISTS saved_bottle_handover_items (
    handover_id VARCHAR(64) NOT NULL REFERENCES saved_bottle_handovers(id),
    saved_bottle_id VARCHAR(64) NOT NULL REFERENCES saved_bottles(id),
    received_quantity INTEGER NOT NULL CHECK (received_quantity >= 0),
    condition VARCHAR(16) NOT NULL,
    PRIMARY KEY (handover_id, saved_bottle_id),
    CONSTRAINT chk_saved_bottle_handover_items_condition CHECK (condition IN ('intact', 'damaged', 'short'))
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON saved_bottle_remedies TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON saved_bottle_handovers TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON saved_bottle_handover_items TO complex_app';
    END IF;
END $$;
