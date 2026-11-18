-- 文件版本：V0.01
-- V645：第十八批 AR 段（batch64）第 233 条税务处理 A 段：233-R1 实际缴纳金额登记、233-R3 原期间已申报的差异调整单。
-- 依据：BSM:1760「税费金额分三个阶段分别保存、互不覆盖：系统计算金额、实际申报金额、实际缴纳金额」「缴税差异等于实际缴纳金额减实际申报金额」
--   「实缴与申报不一致的部分先按未缴、预缴、多缴、抵扣或待退分别处理，不得直接混入税费成本」「原期间已经关账、开票或申报的，只能在当前开放期间生成
--   关联原期间的差异调整单。所有税费规则和税费调整均须经过财务审核、税务审核、法务审核，再由顶层平台公司的老板最终确认后生效」；ENUM:201「原期间已经
--   关账、开票或申报的，不得修改历史数据，只能在当前开放期间生成关联原期间的差异调整单」。两表都挂已生效的 entity_tax_declarations 行（V616），
--   登记即提交审批（finance.tax_rule.adjust 复核链，同 210-R2）：pending_review → approved／rejected。研发阶段无存量（UD-07）。

-- 1) 233-R1 实际缴纳金额登记。paid_amount_cent＝截至本次登记该申报累计实际缴纳；difference_cent＝paid − 登记时的实际申报金额（申报额＋已生效
--    period_difference 差异调整单，即更正后的申报额），落库后不改；difference_breakdown＝[{category, amount_cent}]，category 取 BSM:1760 五类
--    （unpaid 未缴、prepaid 预缴、overpaid 多缴、offset 抵扣、refund_pending 待退），合计等于差额绝对值（服务层校验）。同一申报可再登记（如未缴部分后来
--    补缴），当前实际缴纳金额取最近一条已生效登记，旧登记留作历史；同一申报至多一条待审批。实缴只记录在税务系统已完成的缴纳，不生成应付、不进分红。
CREATE TABLE IF NOT EXISTS entity_tax_payments (
    id VARCHAR(64) PRIMARY KEY,
    declaration_id VARCHAR(64) NOT NULL REFERENCES entity_tax_declarations (id),
    business_entity_id VARCHAR(64) NOT NULL,
    tax_type VARCHAR(32) NOT NULL,
    paid_amount_cent BIGINT NOT NULL,
    difference_cent BIGINT NOT NULL,
    difference_breakdown JSONB NOT NULL DEFAULT '[]'::jsonb,
    reason TEXT NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(16) NOT NULL,
    review_id VARCHAR(64),
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at TIMESTAMPTZ,
    CONSTRAINT chk_etp_amount CHECK (paid_amount_cent >= 0),
    CONSTRAINT chk_etp_status CHECK (status IN ('pending_review', 'approved', 'rejected'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_etp_declaration_pending ON entity_tax_payments (declaration_id) WHERE status = 'pending_review';
CREATE INDEX IF NOT EXISTS idx_etp_declaration_status ON entity_tax_payments (declaration_id, status, approved_at);
COMMENT ON TABLE entity_tax_payments IS '经营主体税费实际缴纳金额登记（BSM:1760：缴税差异＝实际缴纳−实际申报，按未缴、预缴、多缴、抵扣、待退分别处理，不混入税费成本）';

-- 2) 233-R3 差异调整单。kind 三值一次建好：period_difference 原期间已申报的更正（本段，可正可负、不为 0）；supplement 补提、reversal 冲回（233-R5 生成，
--    大于 0）。origin_period_* 从申报行带入（关联原期间）；accounting_month＝当前开放期间＝批准当日北京时间所在月，生效时写入、待审时为空。
--    原申报行一律不改；已生效 period_difference 按 accounting_month 计入该月所在期分红应扣税费（JdbcDividendRepository.sumEntityTaxByType）。
CREATE TABLE IF NOT EXISTS entity_tax_adjustments (
    id VARCHAR(64) PRIMARY KEY,
    declaration_id VARCHAR(64) NOT NULL REFERENCES entity_tax_declarations (id),
    business_entity_id VARCHAR(64) NOT NULL,
    tax_type VARCHAR(32) NOT NULL,
    origin_period_start_month VARCHAR(7) NOT NULL,
    origin_period_end_month VARCHAR(7) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    amount_cent BIGINT NOT NULL,
    accounting_month VARCHAR(7),
    reason TEXT NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(16) NOT NULL,
    review_id VARCHAR(64),
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    approved_at TIMESTAMPTZ,
    CONSTRAINT chk_eta_kind CHECK (kind IN ('supplement', 'reversal', 'period_difference')),
    CONSTRAINT chk_eta_amount CHECK (CASE WHEN kind = 'period_difference' THEN amount_cent <> 0 ELSE amount_cent > 0 END),
    CONSTRAINT chk_eta_status CHECK (status IN ('pending_review', 'approved', 'rejected'))
);
CREATE INDEX IF NOT EXISTS idx_eta_entity_status_month ON entity_tax_adjustments (business_entity_id, status, accounting_month);
CREATE INDEX IF NOT EXISTS idx_eta_declaration ON entity_tax_adjustments (declaration_id);
COMMENT ON TABLE entity_tax_adjustments IS '税费差异调整单（BSM:1760、ENUM:201：原期间已申报的只能在当前开放期间生成关联原期间的差异调整单；补提、冲回调整单同表）';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON entity_tax_payments TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON entity_tax_adjustments TO complex_app';
    END IF;
END $$;
