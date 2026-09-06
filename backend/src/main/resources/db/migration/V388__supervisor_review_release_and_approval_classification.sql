-- V388：M007 审计（2026-09-06）A-01 / F-05。
-- A-01（PGM-1.2 第 5 步「通过、拒绝、卡住、升级审批和放行结果都要记录」、PGM-1.7.1「审批通过才生效」「逐笔放行」）：
--   复核单记录放行时刻与放行人；准入闸消费已批准且未放行的同对象复核单后放行一次，再次执行须重新审批。
ALTER TABLE supervisor_review_decisions
    ADD COLUMN IF NOT EXISTS released_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS released_by VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_supervisor_review_decisions_release
    ON supervisor_review_decisions (action_type, object_type, object_id, initiated_by)
    WHERE review_status = 'approved' AND released_at IS NULL;

-- F-05（PGM-1.10.1 五类权限授出前须先审批才生效：资金/分红发放类；敏感经营数据的查看/导出）：
--   四个 high 风险码此前以 requires_approval=FALSE 播种，被当作日常权限随平台管理员基线与直接授予即时生效。
UPDATE permissions SET requires_approval = TRUE
 WHERE permission_code IN ('finance.dividend.manage', 'finance.ar_ap.export',
                           'analytics.report.export', 'bar.internal_settlement_price.view');
