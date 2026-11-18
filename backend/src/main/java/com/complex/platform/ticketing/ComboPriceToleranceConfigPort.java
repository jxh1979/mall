package com.complex.platform.ticketing;

/**
 * 凑整允许误差与允许尾数配置端口（V369，业务 HTML:741「允许误差后台可配」；第十八批 AT 段 292-R1 加允许尾数集合）。
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

    /** 第十八批 AT 段 292-R1：出厂允许尾数 8、9、0（BSM:754「出厂默认 8、9、0」），存成升序数字串。 */
    String FACTORY_ALLOWED_ENDINGS = "089";

    /**
     * 取「凑整允许误差」（单位：分；运营填的售价与系统算出售价的差额超过此值即拒绝发布）与「凑整允许尾数集合」
     * （第十八批 AT 段 292-R1，BSM:754「尾数落在后台配置的允许尾数集合内——业务参数，全平台统一可配，出厂默认 8、9、0」）。
     * 误差按项目取（项目行优先，否则全局默认行）；尾数只认全平台默认行。
     *
     * @param projectId 项目公司 id；null = 全局默认作用域
     * @throws com.complex.platform.common.api.ApiException 配置缺失（SETTLEMENT_CONFIG_MISSING）
     */
    ComboRounding findComboRounding(String projectId);

    /**
     * @param toleranceCent  允许误差（分），恒 &ge; 0
     * @param allowedEndings 允许尾数（升序去重的数字串，如 "089"＝8、9、0）
     */
    record ComboRounding(long toleranceCent, String allowedEndings) {
    }
}
