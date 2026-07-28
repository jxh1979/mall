package com.complex.platform.inventory;

/**
 * 全平台「高价值物品」判定阈值端口（V350 归一；前身 V345 的 {@code BarVoidApprovalConfigPort}）。
 *
 * <p>依赖方向 inventory → 本端口 ← settlement（实现方 {@code JdbcHighValueThresholdRepository} 落在结算配置中心侧）：
 * 判定侧只声明「我要一个可配的金额阈值」，作用域优先级/草案闸/差人复核转正一律由结算配置中心统一承接。
 *
 * <p>本端口消灭的硬编码：BAR-VOID-A1 的「超过 5000 元即高价值」。对齐全局铁律
 * DEC-CFG-ALL-NUMERIC-001「时间与数字一律可配、禁止写死」。
 */
public interface HighValueThresholdPort {

    /**
     * 取高价值判定阈值（单位：分）。物品**进货价**严格大于本值即为高价值。
     *
     * <p>作用域按「越具体越优先」解析：项目公司 × 商户 →（缺则）项目公司 × 通用商户 →（缺则）全局默认。
     *
     * <p><b>返回 null = 阈值未配置</b>。调用方（{@link HighValueDecisionService}）此时一律按高价值处理、
     * 拦下走审批——业务HTML 明写「阈值未配置时一律按高价值处理，不得静默放行」。这里不抛错，
     * 是因为「拦下走审批」本身就是安全侧，比「整个退酒/报损入口不可用」更贴业务：
     * 主管仍能逐单放行，运营补配置后自动恢复分流。
     *
     * @param projectId  项目公司 id；null = 全局默认作用域
     * @param merchantId 商户 id；null = 不按商户细分
     * @return 阈值（分，≥0）；未配置返回 null
     */
    Long findHighValueThresholdCent(String projectId, String merchantId);
}
