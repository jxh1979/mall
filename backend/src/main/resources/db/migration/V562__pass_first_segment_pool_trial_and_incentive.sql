-- 文件版本：V0.01
-- V562：第 75 条 酒吧通票第一段事前试算、结算环节去掉「超池待审批」、短期经营激励申请与审批、激励期限上限补上下限。
-- 依据：ENUM L52「第一段的固定金额或固定单价在配置生效之前，必须先按该票种在当前各项配置下的最低可能可分配池金额试算……试算不通过的配置无效、
--   不得生效；超池的判定与审批一律在事前完成，结算环节不再设“超池待审批”。娱乐平台公司确需让第一段合计高于池金额的，须在配置生效前提交
--   短期经营激励申请并经项目之上的平台权限审批，超出部分由娱乐平台公司承担，初始有效期不超过后台配置的上限（业务参数，按项目可配，
--   出厂默认三个月，须同时配置上下限）、到期重审」；BSM L752 同义（「期限届满须重新提交审批，不得自动续期」）；
--   DEC-R3-2026-08-04-13（审批人为项目之上的平台权限）。研发阶段无存量（UD-07），不回填、不兼容。

-- 1) 结算环节不再设「超池待审批」：结算单去掉特批引用与特批到期日两列（超池额照旧单列 pool_overflow_cent，过账时由娱乐平台公司承担）。
ALTER TABLE settlement_bills DROP CONSTRAINT IF EXISTS chk_sb_special_approval_term;
ALTER TABLE settlement_bills
    DROP COLUMN IF EXISTS special_approval_ref,
    DROP COLUMN IF EXISTS special_approval_valid_until;

-- 2) 短期经营激励单次有效期上限：复用 ticket_distribution_configs.special_approval_term_value/_unit（按项目可配、项目特例＞全局默认、
--    出厂 3 个月、草案→转正），补该参数的上下限（同一单位）；出厂行上下限留空（同 V556 通告期范式），写入时四项同时必填。
ALTER TABLE ticket_distribution_configs
    ADD COLUMN special_approval_term_min_value INTEGER,
    ADD COLUMN special_approval_term_max_value INTEGER;
ALTER TABLE ticket_distribution_configs
    ADD CONSTRAINT chk_tdc_sa_term_bounds CHECK (
        (special_approval_term_min_value IS NULL) = (special_approval_term_max_value IS NULL)
        AND (special_approval_term_min_value IS NULL
             OR (1 <= special_approval_term_min_value AND special_approval_term_min_value <= special_approval_term_value
                 AND special_approval_term_value <= special_approval_term_max_value)));
COMMENT ON COLUMN ticket_distribution_configs.special_approval_term_value IS
    '短期经营激励单次有效期上限数值（第 75 条，ENUM L52），配合 special_approval_term_unit；出厂 3（月）';
COMMENT ON COLUMN ticket_distribution_configs.special_approval_term_unit IS '短期经营激励单次有效期上限单位：day 天 / month 月';
COMMENT ON COLUMN ticket_distribution_configs.special_approval_term_min_value IS '短期经营激励有效期上限这一参数的下限（同单位）';
COMMENT ON COLUMN ticket_distribution_configs.special_approval_term_max_value IS '短期经营激励有效期上限这一参数的上限（同单位）';

-- 3) 审批权限点复用给短期经营激励审批（只授 role_platform_admin，授予关系不动）。
UPDATE permissions SET permission_name = '短期经营激励审批（项目之上的平台权限·高敏）'
 WHERE permission_code = 'settlement.bill.special_approve';

-- 4) 短期经营激励申请：initial＝配置生效前提交（转正试算不通过时凭已批准的这一笔放行）；renewal＝到期重审。
--    trial_result＝提交时逐票种试算（票种、最低实付价、最不利一份可分配池、第一段应分合计、超出额，分）。
--    有效期：首次申请在该版转正时自生效营业日起算；重审获批从上一笔已批准的到期日接续。没有任何自动续期作业。
CREATE TABLE pass_incentive_applications (
    id                VARCHAR(64) PRIMARY KEY,
    scheme_version_id VARCHAR(64) NOT NULL REFERENCES ticket_distribution_scheme_versions(id),
    project_id        VARCHAR(64),
    kind              VARCHAR(16) NOT NULL,
    status            VARCHAR(16) NOT NULL,
    reason            TEXT NOT NULL,
    term_value        INTEGER NOT NULL,
    term_unit         VARCHAR(8) NOT NULL,
    trial_result      JSONB NOT NULL,
    max_excess_cent   BIGINT NOT NULL,
    submitted_by      VARCHAR(64),
    submitted_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_by        VARCHAR(64),
    decided_at        TIMESTAMPTZ,
    decision_note     TEXT,
    valid_from        TIMESTAMPTZ,
    valid_until       TIMESTAMPTZ,
    CONSTRAINT chk_pia_kind CHECK (kind IN ('initial', 'renewal')),
    CONSTRAINT chk_pia_status CHECK (status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT chk_pia_term CHECK (term_value >= 1 AND term_unit IN ('day', 'month')),
    CONSTRAINT chk_pia_reason CHECK (btrim(reason) <> '')
);
CREATE INDEX idx_pia_version ON pass_incentive_applications (scheme_version_id, status);
COMMENT ON TABLE pass_incentive_applications IS
    '短期经营激励申请（第 75 条，ENUM L52）：第一段合计高于池金额须在配置生效前申请、经项目之上的平台权限审批，到期重审';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON pass_incentive_applications TO complex_app';
    END IF;
END $$;
