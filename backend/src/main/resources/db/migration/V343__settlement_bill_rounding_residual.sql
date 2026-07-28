-- V343 R-7 分账尾差单列留痕(2026-07-22 用户拍板 D15.1 / 治理地图 HTML「全局原则·金额一律精确到分」)。
--   口径:一切按比例拆分(通票分账/费用分摊/提成)四舍五入到分,各方分别舍入后与「应分总额」的差额=尾差,
--        统一由项目公司/平台兜底、【单列一笔留痕】,绝不悄悄并进任一参与方,也不得静默吞掉。
--   本迁移只给月度结算单头加一列(带符号尾差),不改任何既有列/约束/数值;存量行默认 0(旧算法整除截断,
--   尾差历史上沉淀在「总池 − Σ分账」里未单列,不追溯改写既有账,只从本版起显式记账)。
--   守恒恒等式(引擎 TicketDistributionSettlementService 自检 + 集成测试断言):
--        Σ池分账明细 + rounding_residual_cent ≡ 应分总额(对各家精确份额合计只舍入一次)

ALTER TABLE settlement_bills
    ADD COLUMN IF NOT EXISTS rounding_residual_cent BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN settlement_bills.rounding_residual_cent IS
    '按比例分账的四舍五入尾差(分,带符号):=应分总额 − Σ各商户已舍入分账额。归项目公司/平台兜底,不并入任一商户。'
    ' 正数=平台补足各家舍入后的缺口;负数=各家舍入后多出、由平台追回。|绝对值| ≤ 参与分账商户数(算法上界)。';
