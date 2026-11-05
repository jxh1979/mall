-- 文件版本：V0.01
-- 第十八批 R 段第 143 条 143-R1：日结拆成生成、确认、锁定三步。
-- 依据：ENUM:190「待生成、待确认、有差异、已确认、已锁定、已归档」；BSM:1747「待生成、待确认、有差异、已确认、已锁定、已归档；锁定后补录须上级审批。」；
-- BSM:1682「按营业日生成日结批次。」。生成即入库为待确认（pending_confirm），确认为 confirmed，锁定为 locked；
-- 「待生成」按该营业日该范围查不到日结显示、不入库；「有差异」按本营业日本范围的收银班次实时判定、不入库（143-R6）；
-- 已归档（archived）与归档时间由 143-R5 使用（第十八批裁决 33「锁定后，由该店持日结锁定权限的财务 BP 点「归档」」）。
-- 生成时尚未锁定，锁定时间改可空；研发期无存量（UD-07），不回填。
ALTER TABLE finance_daily_closes
    ALTER COLUMN locked_at DROP NOT NULL,
    ADD COLUMN confirmed_at TIMESTAMPTZ,
    ADD COLUMN archived_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_finance_daily_closes_status
        CHECK (status IN ('pending_confirm', 'confirmed', 'locked', 'archived'));
COMMENT ON COLUMN finance_daily_closes.confirmed_at IS '日结确认时间（143-R1）';
COMMENT ON COLUMN finance_daily_closes.archived_at IS '日结归档时间（143-R5，第十八批裁决 33）';

-- 143-R2：五类最终标记（ENUM:190「需要逐类标记为最终的数据集有五类：当日订单与核销记录、当日退款与冲正记录、当日出品与物品交接记录、
-- 当日考勤记录、当日异常处置记录。由各域主管公司的财务岗在该营业日闭店之后完成核对并逐类标记」「标记后不可撤销，事后发现错误只能走冲正，
-- 不得改回未最终。」）。挂在日结上，每类一行；标记人由统一留痕记录，不另存列；不可撤销靠不提供撤销、修改接口达成。
CREATE TABLE finance_daily_close_final_marks (
    daily_close_id VARCHAR(64) NOT NULL REFERENCES finance_daily_closes(id),
    category VARCHAR(40) NOT NULL CHECK (category IN ('orders_verifications', 'refunds_reversals', 'production_handover',
        'attendance', 'anomaly_resolutions')),
    marked_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (daily_close_id, category)
);
COMMENT ON TABLE finance_daily_close_final_marks IS '日结五类数据最终标记（143-R2）';
