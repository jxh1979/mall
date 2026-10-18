-- 文件版本：V0.03
-- BSM L1481：销假或撤销发生在已月结定格月份之后的，在当期薪资单冲正并注明来源，不回改已定格月份。
-- 2026-10-18 操作员裁决：销假改为「本人申请＋主管批准」——员工本人对已批准的假发起销假申请（revoke_requested，理由必填），
-- 有排班管理权的主管批准后才转「已销假」(revoked) 并按 BSM L1481 冲正/不扣薪；驳回则回到 approved、假仍有效。原请假单保留，不删除。
ALTER TABLE staff_leave_requests DROP CONSTRAINT IF EXISTS chk_leave_status;
ALTER TABLE staff_leave_requests
    ADD CONSTRAINT chk_leave_status CHECK (status IN ('pending', 'approved', 'rejected', 'revoke_requested', 'revoked'));
-- 销假申请留痕（员工本人）：申请人、申请时间、申请理由；驳回后三列保留为最近一次申请的痕迹，再次申请时覆盖。
ALTER TABLE staff_leave_requests
    ADD COLUMN IF NOT EXISTS revoke_requested_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS revoke_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS revoke_request_reason VARCHAR(255);
-- 销假批准留痕（主管）：批准人、批准时间、批准说明（可空）；原「主管批准请假」的 reviewed_by / review_note 保留不改
-- （BSM L1481「只有主管批准的请假才参与扣薪」的批准证据留在单据层）。
ALTER TABLE staff_leave_requests
    ADD COLUMN IF NOT EXISTS revoked_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS revoked_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS revoke_reason VARCHAR(255);
