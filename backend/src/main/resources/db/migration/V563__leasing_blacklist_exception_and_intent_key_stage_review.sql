-- 文件版本：V0.01
-- 第十八批 D 段第 30 条 招商黑名单例外放行与意向关键阶段主管确认。
-- PGM 第 502 行「黑名单品牌拦截同品牌新线索，以及由拉黑前既有线索新建意向。确需继续的，须由具备单独授予的黑名单例外放行权限人员操作，
-- 按层级对应主管审批，申请人不得审批自己提交的申请，并记录操作人、审批人、理由、时间和关联线索或意向。」
-- PGM 第 502 行「意向五阶段中进入方案报价、商务谈判须主管确认才能推进」；审批走统一审批模型（PGM 第 540 行，发起人沿直接上级到其指定的最终审批人）。

-- ① 黑名单例外放行权限：授出须审批（单独授予），出厂只给平台管理员，不挂任何业务角色。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('leasing.brand.blacklist_exception', '黑名单例外放行', 'platform', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'leasing.brand.blacklist_exception')
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- ② 例外放行单：一单放行一件事（新建线索，或由某条线索新建意向），批准后申请人本人用一次。
--    project_id 取线索（由线索新建意向）或品牌（新建线索）的项目，与招商现表一致可空。
CREATE TABLE IF NOT EXISTS leasing_blacklist_exceptions (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64),
    brand_id VARCHAR(64) NOT NULL REFERENCES leasing_brands(id),
    target_type VARCHAR(32) NOT NULL,
    lead_id VARCHAR(64) REFERENCES leasing_leads(id),
    reason VARCHAR(500) NOT NULL,
    requested_by VARCHAR(64) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    review_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending_approval',
    approved_by VARCHAR(64),
    approved_at TIMESTAMPTZ,
    used_at TIMESTAMPTZ,
    used_lead_id VARCHAR(64),
    used_intent_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_leasing_blacklist_exceptions_target CHECK (target_type IN ('lead_create', 'intent_create')),
    CONSTRAINT chk_leasing_blacklist_exceptions_lead CHECK (target_type <> 'intent_create' OR lead_id IS NOT NULL),
    CONSTRAINT chk_leasing_blacklist_exceptions_status CHECK (status IN ('pending_approval', 'approved', 'rejected', 'used')),
    CONSTRAINT chk_leasing_blacklist_exceptions_reason CHECK (btrim(reason) <> '')
);
CREATE INDEX IF NOT EXISTS idx_leasing_blacklist_exceptions_review ON leasing_blacklist_exceptions (review_id);

-- ③ 意向关键阶段待主管确认：送审时记目标阶段与复核单，批准才改阶段，批准或驳回都清空。
ALTER TABLE leasing_intents ADD COLUMN IF NOT EXISTS pending_stage VARCHAR(32);
ALTER TABLE leasing_intents ADD COLUMN IF NOT EXISTS pending_review_id VARCHAR(64);
ALTER TABLE leasing_intents ADD CONSTRAINT chk_leasing_intents_pending_stage
    CHECK (pending_stage IS NULL OR pending_stage IN ('proposal_quoted', 'negotiation'));
CREATE INDEX IF NOT EXISTS idx_leasing_intents_pending_review
    ON leasing_intents (pending_review_id)
 WHERE pending_review_id IS NOT NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON leasing_blacklist_exceptions TO complex_app';
    END IF;
END $$;
