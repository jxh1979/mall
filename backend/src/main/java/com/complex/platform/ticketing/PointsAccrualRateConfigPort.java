package com.complex.platform.ticketing;

/**
 * 积分累计率配置端口(P0-D/CFG-01,V340)。
 *
 * <p>依赖方向 ticketing → 本端口 ← settlement(实现方 {@code JdbcCommissionPointsConfigRepository}):
 * 票务域只声明「我要一个可配的积分累计率」,作用域优先级/草案闸/复核转正由结算配置中心统一承接。
 *
 * <p>本端口消灭的硬编码:{@code TicketOrderService} 原 {@code POINTS_PER_YUAN_CENT = 100L}(积分基础分「每元 1 分」写死)。
 * 等级倍率/生日倍率仍由 {@code LoyaltyService} 在基础分之上叠加,不属本端口。
 */
public interface PointsAccrualRateConfigPort {

    /**
     * 取「每积 1 基础分所需的实付消费金额」(单位:分)。
     *
     * <p>100 = 每 1 元积 1 分(出厂默认 = 改造前写死值);50 = 每 1 元积 2 分;200 = 每 2 元积 1 分。
     * 作用域按「越具体越优先」解析:项目公司 × 商户 →(缺则)项目公司 × 通用商户 →(缺则)全局默认。
     * 全空时<b>响亮抛错</b>而非静默套硬编码默认,与结算配置中心 {@code requireConfigured} 同口径。
     *
     * @param projectId  项目公司 id;null = 全局默认作用域
     * @param merchantId 受理商户 id;null = 不按商户细分(平台官方票务)
     * @return 每积 1 分所需消费金额(分),恒 &gt; 0
     * @throws com.complex.platform.common.api.ApiException 配置缺失(SETTLEMENT_CONFIG_MISSING)
     */
    long findAmountCentPerPoint(String projectId, String merchantId);
}
