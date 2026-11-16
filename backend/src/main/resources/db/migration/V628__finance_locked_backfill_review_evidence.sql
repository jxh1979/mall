-- 文件版本：V0.01
-- V628：第十八批 AL 段第 245 条 245-R1（日结锁定后补录改走统一审批）、第 253 条 253-R1、253-R2（三种锁账补录附原始凭证、写责任人）。
-- 依据：ENUM:190「日结锁定后的补录须上级审批」；PGM:528「发起单据时只需指定该事项的最终审批人，系统按组织层级自动补全……逐级审批」；
--   PGM:529「涉钱审批出厂默认 1 个工作日……系统自动提醒并升级到该节点的上一级」；BSM:1642「记录缺失时允许人工补录，但必须写原因和责任人。……
--   补录必须附原始记录凭证（微信截图、通话录音等）并经审核」；第十八批裁决 56「三种锁账补录提交时必须上传原始凭证（银行回单、转账截图、异议处理结论等）
--   并写责任人；「业务发生后 3 天内」不套。」
-- ① ar_ap_adjustments 加复核单号（列宽同 V570 manual_backfills.review_id）、原始凭证附件编号、责任人账号。研发阶段无存量（UD-07）。
-- ② 旧审批接口（POST /api/v1/finance/ar-ap-adjustments/{id}/approve）同批删除，审批人改由统一审批按组织层级定：照 V578 退役
--   finance.locked_backfill.approve 并清掉全部授予来源、删 V554 种的能力开关行；照 V612 停用 V099 的审批入口超合同范围准入策略（受闸方法已删）。
--   历史迁移（V069、V099、V170、V251、V554）不回改。
ALTER TABLE ar_ap_adjustments
    ADD COLUMN review_id VARCHAR(64) UNIQUE,
    ADD COLUMN evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN responsible_staff_id VARCHAR(64);

UPDATE permissions SET status = 'retired' WHERE permission_code = 'finance.locked_backfill.approve';

DELETE FROM role_permissions WHERE permission_code = 'finance.locked_backfill.approve';
DELETE FROM permission_template_items WHERE permission_code = 'finance.locked_backfill.approve';
DELETE FROM user_permission_overrides WHERE permission_code = 'finance.locked_backfill.approve';

UPDATE temporary_permission_grants
   SET status = 'revoked', revoked_by = 'V628', revoked_at = now()
 WHERE permission_code = 'finance.locked_backfill.approve'
   AND status = 'active';

DELETE FROM terminal_capability_switches WHERE capability_code = 'finance.locked_backfill.approve';

UPDATE supervisor_review_policies SET enabled = false, updated_at = now() WHERE policy_code = 'finance_locked_approve_over_scope';
