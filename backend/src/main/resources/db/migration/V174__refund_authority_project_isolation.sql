-- B1d② 退款分级授权按综合体(project)隔离（用户裁决：每个综合体各管各的退款审批规则）。
-- 现 unique 不含 project_id → 不同综合体同 subject/domain（或同 domain/rule_type）会互相覆盖、跨综合体串配置。
-- 改为含 project_id 的唯一键；project_id 可空（NULL=全局/平台默认，单综合体沿用），故用 COALESCE 把 NULL 当
-- 「__global__」一个确定槽，避免 Postgres「多个 NULL 互不冲突」导致同一全局键重复落多行。

ALTER TABLE refund_authority_limits DROP CONSTRAINT IF EXISTS uq_refund_authority;
CREATE UNIQUE INDEX IF NOT EXISTS uq_refund_authority_project
    ON refund_authority_limits (subject_type, subject_id, refund_domain, COALESCE(project_id, '__global__'));

ALTER TABLE refund_auto_rules DROP CONSTRAINT IF EXISTS uq_refund_auto_rule;
CREATE UNIQUE INDEX IF NOT EXISTS uq_refund_auto_rule_project
    ON refund_auto_rules (refund_domain, rule_type, COALESCE(project_id, '__global__'));
