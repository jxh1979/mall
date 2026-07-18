-- 方案2 整票退款（AUD-006）+ 规则2 可退配置。依赖 V285 父票实例地基。
-- 用户口径：测试环境上线前全删，不做任何存量回填；本迁移只加列/扩状态机，无回填 SQL。

-- 1) 票种可退配置（规则2）：默认不可退。可退=未核销可退+到期未使用系统自动退；
--    不可退=客人不能申请退+到期直接作废不退。一个总开关管两者。
ALTER TABLE ticket_types
    ADD COLUMN IF NOT EXISTS refundable BOOLEAN NOT NULL DEFAULT false;

-- 2) 父票实例状态机补退款态（申请即冻结 refund_pending → 生效 refunded / 驳回恢复 active）。
--    一并预留方案3 转赠态 transferring，避免方案3 再动迁移（三方案共用一张状态机，值域一次对齐）。
ALTER TABLE ticket_instances DROP CONSTRAINT IF EXISTS chk_ticket_instances_status;
ALTER TABLE ticket_instances ADD CONSTRAINT chk_ticket_instances_status
    CHECK (status IN ('active', 'refund_pending', 'refunded', 'forfeited', 'transferring'));
