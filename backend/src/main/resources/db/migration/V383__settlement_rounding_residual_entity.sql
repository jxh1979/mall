-- V383 分账尾差的【责任主体】快照列(口径矫回·2026-08-03)。
--
-- 【业务依据】(最终解释权=业务 HTML,逐字)
--   platform-governance-map-for-operations §「全局原则 · 金额一律精确到分」:
--     「各方分别算完后如果和总数差了几分(尾差),这几分必须单独记账留痕,并由该笔业务
--       【实际承担结算责任的主体】承担,不得默认转给城市项目公司、顶层平台或其他无关主体,
--       也不得悄悄并进任一参与方。」
--   business-system-map-for-operations §「金额尾差」同义。
--   注:这是 2026-08-01 才翻转的口径;旧口径「统一归项目公司/平台兜底」(V343 注释里那句)已作废。
--
-- 【改前的错】
--   ① settlement_bills.rounding_residual_cent 只有金额没有主体 —— 对账时业务人员看到一笔尾差,
--     看不出该找谁认,只能按代码注释里的旧口径默认算给项目公司/平台。
--   ② 渠道费按业态分摊的尾差更糟:直接并进了酒吧那一支的 channel_fee_cent ——
--     酒吧替全场认了这几分,连单独留痕都没有,恰好撞上「不得悄悄并进任一参与方」。
--
-- 【落法】照抄 V375 pool_undistributed_entity_id 的既有范式:金额列 + 主体快照列并列,月结时定格。
--   通票分账这笔业务的实际结算责任主体 = all_pass_ticket 收入流的归属主体(娱乐平台公司;
--   后台可配、项目级可覆盖),与 settlement_bills.attributed_entity_id 同源解析,不在 Java 里另写一套。
--   尾差为 0 时主体置空(没有尾差就没有责任人)。
--   .160 为开发环境、库内数据最后全删 → 不做存量回填/兼容设计。
ALTER TABLE settlement_bills ADD COLUMN IF NOT EXISTS rounding_residual_entity_id VARCHAR(64);

COMMENT ON COLUMN settlement_bills.rounding_residual_entity_id IS
    '分账尾差(rounding_residual_cent)的实际结算责任主体快照(business_entities.id,解析自 revenue_stream_attribution 的 all_pass_ticket 流;月结时定格)。尾差为 0 时为空';

COMMENT ON COLUMN settlement_bills.rounding_residual_cent IS
    '按比例拆分的四舍五入尾差(分,带符号)＝池分账尾差(应分总额 − Σ各商户已舍入分账额) + 渠道费按业态分摊尾差。'
    ' 两者同为「按比例拆分四舍五入」成因,合列一笔留痕;由 rounding_residual_entity_id 指向的实际结算责任主体承担,'
    ' 不得默认转给城市项目公司/顶层平台,也不得并进任一参与方。|池分账尾差| ≤ 参与分账商户数(算法上界)';
