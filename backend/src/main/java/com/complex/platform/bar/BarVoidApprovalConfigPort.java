package com.complex.platform.bar;

/**
 * 高价值退酒审批阈值配置端口（P0-C/FLOW-07，V345，BAR-VOID-A1）。
 *
 * <p>依赖方向 bar → 本端口 ← settlement（实现方 {@code JdbcBarVoidApprovalConfigRepository} 落在结算配置中心侧）：
 * 酒吧域只声明「我要一个可配的高价值阈值」，阈值的作用域优先级/草案闸/复核转正一律由结算配置中心统一承接，
 * 酒吧域不感知配置表结构 —— 与 {@link BarCommissionRateConfigPort} 同一套端口范式。
 *
 * <p>本端口消灭的硬编码：BAR-VOID-A1 的「超过 5000 元即高价值」。PRD §10.2.1 明写
 * 「不得在代码写死」，对齐全局铁律 DEC-CFG-ALL-NUMERIC-001「时间与数字一律可配、禁止写死」。
 */
public interface BarVoidApprovalConfigPort {

    /**
     * 取高价值退酒阈值（单位：分）。单笔已出品退品金额 <b>严格大于</b>本值即为高价值，须先审后行 + 双证据。
     *
     * <p>作用域按「越具体越优先」解析：项目公司 × 商户 →（缺则）项目公司 × 通用商户 →（缺则）全局默认。
     * 三层全空（连 V345 的 {@code bvac_default} 种子都没有 = 部署坏）时<b>响亮抛错</b>而非静默套 5000 元，
     * 与结算配置中心 {@code requireConfigured} 同口径。
     *
     * @param projectId  项目公司 id；null = 全局默认作用域
     * @param merchantId 商户（酒吧）id；null = 不按商户细分
     * @return 高价值阈值（分），≥0
     * @throws com.complex.platform.common.api.ApiException 配置缺失（SETTLEMENT_CONFIG_MISSING）
     */
    long findVoidHighValueThresholdCent(String projectId, String merchantId);
}
