-- 文件版本：V0.01
-- V764（第十八批 CB 段第 394 条 394-R1）：付账日配置（三个付账日与钱路指派规则）的提交人与审批人分离约束放行最高权限本人。
-- 依据：BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」；第十八批裁决 71。
-- V508 的 chk_ipdr_sod 在库里强制审批人≠提交人，最高权限本人提交、本人审批（InternalPayDayService.decide 已放行）会被它拦成 500；
--   改为审批人≠提交人，或审批人是最高权限账号（usr_master_authority，同 V435 写法）。其余账号仍须他人审批。
ALTER TABLE internal_pay_day_rules DROP CONSTRAINT chk_ipdr_sod;
ALTER TABLE internal_pay_day_rules ADD CONSTRAINT chk_ipdr_sod
    CHECK (decided_by IS NULL OR decided_by <> submitted_by OR decided_by = 'usr_master_authority');
