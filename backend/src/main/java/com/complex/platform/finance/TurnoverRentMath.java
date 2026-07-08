package com.complex.platform.finance;

import java.util.List;

/**
 * 提成租金计算（超额累进 + 保底下限，无封顶）。合同域「收租方式」再决定它与月租如何结合，不在此处。
 *
 * <p>阶梯语义（与个税同）：tiers 按 seq 升序，每档 upperBoundCent 为该档销售额上限（分），
 * 最高档 upperBoundCent=null 表无上限；提成 = Σ(落在各档区间的销售额 × 该档费率)。
 * 保底 minAmountCent：算出的提成低于保底则按保底收（可空=无保底）。
 */
public final class TurnoverRentMath {

    private static final int BPS_DENOMINATOR = 10000;

    private TurnoverRentMath() {
    }

    /** 阶梯超额累进提成（分）。tiers 为空时返回 0（调用方可回退单一费率旧算法）。 */
    public static long tieredRent(long turnover, List<TurnoverRentRuleTierRecord> tiers, Long minAmountCent) {
        long rent = 0L;
        long lower = 0L;
        if (turnover > 0 && tiers != null) {
            for (TurnoverRentRuleTierRecord tier : tiers) {
                if (turnover <= lower) {
                    break;
                }
                boolean topTier = tier.upperBoundCent() == null;
                long upper = topTier ? Long.MAX_VALUE : tier.upperBoundCent();
                long seg = Math.min(turnover, upper) - lower;
                if (seg > 0) {
                    rent += Math.floorDiv(seg * (long) tier.rateBps(), BPS_DENOMINATOR);
                }
                if (topTier) {
                    break;
                }
                lower = upper;
            }
        }
        if (minAmountCent != null && rent < minAmountCent) {
            rent = minAmountCent;
        }
        return rent;
    }
}
