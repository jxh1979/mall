package com.complex.platform.settlement;

/**
 * P6(2026-07-21)综合服务费「每笔」计算——供 {@link SettlementSettledConsumer} 核销时算实收 fee 落库,
 * 与 {@link TicketDistributionSettlementService#settleMonth} 的毛额/折扣口径同源(共用本类防两处逻辑漂移)。
 *
 * <p>本类只算<b>未封顶</b>的「每笔」值;<b>月度封顶</b>由消费者按月【增量】应用(月内同「商户×规则」Σfee ≤ cap),
 * 而非月末对聚合重算——这样每笔实收成为不可变事实,冲正严格取原行之反,固定额/封顶/比例三模式跨月红冲全守恒
 * (旧月末重算 min(fixed×count,gross)/min(raw,cap) 是月度非线性,f(-x)≠-f(x),过账开启后跨月红冲不守恒,对抗审计坐实)。
 */
final class ComprehensiveServiceFee {

    private ComprehensiveServiceFee() {
    }

    /**
     * 本笔未封顶实收综合服务费:固定额=min(fixedAmount×verifiedCount, gross)(上限本笔毛额防净额为负);
     * 否则比例=gross×feeBps/10000(整数分截断余数留商户,不多扣)。默认比例 0 / gross<=0 → 0(保值)。
     * 与 settleMonth 旧 rawFee 逐行口径一字不差,保证核销时算的每笔 Σ = 旧月末聚合重算(无红冲时)。
     */
    static long perTicketCent(JdbcSettlementBillRepository.ServiceFeeConfig cfg, long grossCent, int verifiedCount) {
        if (cfg == null || grossCent <= 0L || verifiedCount <= 0) {
            return 0L;
        }
        return cfg.fixedAmountCent() != null
                ? Math.min(cfg.fixedAmountCent() * verifiedCount, grossCent)
                : grossCent * cfg.feeBps() / 10000;
    }

    /**
     * 项目承担的套票折扣额(D6):platform_absorb=全额、split_by_ratio=平台份额、merchant_absorb/未知=0。
     * 综合服务费的毛额 = 净收入 + 本项(与 settleMonth 同口径:项目承担折扣使商户拿满额,fee 在满额毛额上算)。
     */
    static long platformBorneCent(JdbcSettlementBillRepository.ComboDiscountConfig cfg, long discountCent) {
        if (cfg == null) {
            return 0L;
        }
        return switch (cfg.discountBearer()) {
            case "platform_absorb" -> discountCent;
            case "split_by_ratio" -> discountCent * cfg.splitPlatformBps() / 10000;
            default -> 0L;
        };
    }
}
