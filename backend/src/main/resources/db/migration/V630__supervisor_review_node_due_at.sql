-- 文件版本：V0.01
-- V630：第十八批 AO 段第 246 条 246-R1（审批时限按节点起算）。
-- 依据：PGM:529「任一业务审批节点超过后台配置的时限（业务参数，按审批类型可配，出厂默认 3 个工作日，涉钱审批出厂默认 1 个工作日，
--   须同时配置上下限）未处理的，系统自动提醒并升级到该节点的上一级，不得默认通过，也不得默认拒绝。」；PGM:536「主线与各条平行线全部通过合同才生效」。
-- 审批节点加到期时刻：节点自接手时起按该审批类型时限计时（发起时各线首节点、前一节点通过后本线下一节点）；还没轮到的节点为空。
-- 复核单到期（supervisor_review_decisions.due_at）取各当前节点中最早的（SupervisorReviewService 维护）。研发阶段无存量，不回填（UD-07）。
ALTER TABLE supervisor_review_nodes ADD COLUMN IF NOT EXISTS due_at TIMESTAMPTZ;
