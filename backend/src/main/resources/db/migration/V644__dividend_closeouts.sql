-- 文件版本：V0.01
-- V644：第十八批 AQ 段第 243 条 243-R1（酒吧关停结算）。
-- 依据：BSM:1787「酒吧关闭须完成关停结算，依法核清本次分红、已算未发分红与发放保护线待释放款项；门店关停结算与公司依法解散清算分别办理。」
--   「已确认应付及待释款项的法律性质和处理由财务、法务核实，不因关店自动核销或一概改为剩余财产分配；相关款项须按核实结果处理并留痕后才能结案。」；
--   PGM:528「需要平行线审批的（如法务、财务），另行指定该条线的最终审批人」。
-- ① dividend_closeouts：一家酒吧一条关停结算登记。closed_business_date＝关店最后营业日（关店所在期正式单只查期初至该日的五类最终标记，
--    关店后的期间不再分红）。status：open 待提交核实 → pending_review 财务法务核实中（驳回退回 open）→ verified 已核实待处理 → closed 已结案。
-- ② dividend_closeout_items：提交核实时的款项快照（本次分红、已算未发分红、保护线待释放），逐条记财务、法务核实的法律性质与处理方式，
--    核实通过后逐条登记处理留痕；驳回后再提交整份重写。不动 ar_ap_items 与 dividend_statements。
-- 研发阶段无存量（UD-07）。
CREATE TABLE IF NOT EXISTS dividend_closeouts (
    id varchar(64) PRIMARY KEY,
    merchant_id varchar(64) NOT NULL UNIQUE,
    closed_business_date date NOT NULL,
    reason text NOT NULL,
    status varchar(24) NOT NULL,
    review_id varchar(80),
    created_by varchar(80) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    submitted_by varchar(80),
    submitted_at timestamptz,
    closed_by varchar(80),
    closed_at timestamptz,
    CONSTRAINT chk_dco_status CHECK (status IN ('open', 'pending_review', 'verified', 'closed'))
);

CREATE TABLE IF NOT EXISTS dividend_closeout_items (
    id varchar(64) PRIMARY KEY,
    closeout_id varchar(64) NOT NULL REFERENCES dividend_closeouts(id),
    sort_order int NOT NULL,
    item_type varchar(24) NOT NULL,
    statement_id varchar(64),
    ar_ap_item_id varchar(64),
    amount_cent bigint NOT NULL,
    legal_nature text NOT NULL,
    handling text NOT NULL,
    handled_note text,
    handled_by varchar(80),
    handled_at timestamptz,
    CONSTRAINT chk_dcoi_item_type CHECK (item_type IN ('current_period', 'unpaid_dividend', 'reserve_pending'))
);

CREATE INDEX IF NOT EXISTS ix_dcoi_closeout ON dividend_closeout_items (closeout_id, sort_order);

COMMENT ON TABLE dividend_closeouts IS '酒吧关停结算登记（BSM:1787；一家酒吧一条，经财务、法务核实，逐条处理留痕后结案）';
COMMENT ON TABLE dividend_closeout_items IS '酒吧关停结算款项快照（本次分红、已算未发分红、保护线待释放；法律性质与处理方式由财务、法务核实）';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON dividend_closeouts TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON dividend_closeout_items TO complex_app';
    END IF;
END $$;
