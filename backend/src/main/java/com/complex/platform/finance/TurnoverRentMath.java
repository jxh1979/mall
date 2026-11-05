package com.complex.platform.finance;

import com.complex.platform.common.money.MoneyRounding;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 提成租金计算（超额累进 + 保底下限，无封顶）。合同域「收租方式」再决定它与月租如何结合，不在此处。
 *
 * <p>阶梯语义（与个税同）：tiers 按 seq 升序，每档 upperBoundCent 为该档销售额上限（分），
 * 最高档 upperBoundCent=null 表无上限；提成 = Σ(落在各档区间的销售额 × 该档费率)。
 * 保底 minAmountCent：算出的提成低于保底则按保底收（可空=无保底）。
 */
public final class TurnoverRentMath {

    private TurnoverRentMath() {
    }

    /**
     * 阶梯超额累进提成（分）。tiers 为空时返回 0（调用方可回退单一费率旧算法）。
     *
     * <p>R-7（D15.1「提成」明列）：各档先累<b>精确</b>份额、末尾<b>只四舍五入一次</b>到分。
     * 旧写法逐档 {@code Math.floorDiv(seg*rateBps, 10000)} 既向下取整、又逐档累积误差
     * （N 档最多少收 N 分），项目公司恒少收且余数无留痕。
     */
    public static long tieredRent(long turnover, List<TurnoverRentRuleTierRecord> tiers, Long minAmountCent) {
        BigDecimal exact = BigDecimal.ZERO;
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
                    exact = exact.add(MoneyRounding.exactBps(seg, tier.rateBps()));
                }
                if (topTier) {
                    break;
                }
                lower = upper;
            }
        }
        long rent = MoneyRounding.toCent(exact);
        if (minAmountCent != null && rent < minAmountCent) {
            rent = minAmountCent;
        }
        return rent;
    }

    /** 第十八批 U 段第 155 条 155-R1（DEC-R8-54「超额累进下逐段拆分」）：一笔销售落在某档的那一段（费率、金额）。 */
    public record Segment(int rateBps, long amountCent) {
    }

    /**
     * 155-R1（财务 PRD:904「按其在当月累计中的位置逐段拆分」）：当月累计 [before, before+amount) 与各档区间的交集，按档升序。
     * 规则没有阶梯行时由调用方传一档（upperBoundCent=null、费率＝规则费率）。
     */
    public static List<Segment> segments(long before, long amount, List<TurnoverRentRuleTierRecord> tiers) {
        long end = Math.addExact(before, amount);
        List<Segment> out = new ArrayList<>();
        long lower = 0L;
        for (TurnoverRentRuleTierRecord tier : tiers) {
            long upper = tier.upperBoundCent() == null ? Long.MAX_VALUE : tier.upperBoundCent();
            long seg = Math.min(end, upper) - Math.max(before, lower);
            if (seg > 0) {
                out.add(new Segment(tier.rateBps(), seg));
            }
            if (end <= upper) {
                break;
            }
            lower = upper;
        }
        return List.copyOf(out);
    }

    /**
     * 155-R1（ENUM:257「冲正金额一律按该笔款项在其原账期实际计费时所适用的档位与费率原额冲回」）：该笔各段按原费率的精确抽成 × 退款 ÷ 该笔净额，
     * 部分退款按退款占该笔销售的比例；精确累加、末尾只舍入一次到分。
     */
    public static long reversalCent(long refundCent, long netCent, List<Segment> segments) {
        BigDecimal exact = BigDecimal.ZERO;
        for (Segment segment : segments) {
            exact = exact.add(MoneyRounding.exactBps(segment.amountCent(), segment.rateBps()));
        }
        return MoneyRounding.toCent(exact.multiply(BigDecimal.valueOf(refundCent))
                .divide(BigDecimal.valueOf(netCent), 12, RoundingMode.HALF_UP));
    }
}
