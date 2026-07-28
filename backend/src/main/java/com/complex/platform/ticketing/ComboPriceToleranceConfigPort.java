package com.complex.platform.ticketing;

/**
 * 凑整允许误差配置端口（V369，业务 HTML:741「允许误差后台可配」）。
 *
 * <p>依赖方向 ticketing → 本端口 ← settlement（实现方 {@code JdbcSettlementBillRepository}）：
 * 票务域只声明「我要一个可配的允许误差」，作用域优先级/草案闸/复核转正由结算配置中心统一承接，
 * 与 {@link PointsAccrualRateConfigPort} 同范式。
 *
 * <p>值取自「套票折扣归属」那张配置表（{@code ticket_combo_discount_configs.rounding_tolerance_cent}）——
 * 凑整产生的差额走的正是套票折扣那条承担口径，同一个项目公司、同一个「通票分账」栏目、同一位复核人，
 * 运营只需在一处维护，不另立第二套配置面。
 */
public interface ComboPriceToleranceConfigPort {

    /**
     * 取「凑整允许误差」（单位：分）。运营填的售价与系统算出售价的差额超过此值即拒绝发布。
     *
     * @param projectId 项目公司 id；null = 全局默认作用域
     * @return 允许误差（分），恒 &ge; 0
     * @throws com.complex.platform.common.api.ApiException 配置缺失（SETTLEMENT_CONFIG_MISSING）
     */
    long findRoundingToleranceCent(String projectId);
}
