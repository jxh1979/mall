package com.complex.platform.finance;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TurnoverRentMathTests {

    // 示例阶梯：0-20万 5%；20万-30万 8%；30万以上 10%（金额单位分：20万=20_000_000）。
    private static final List<TurnoverRentRuleTierRecord> TIERS = List.of(
            new TurnoverRentRuleTierRecord("t1", "r1", 1, 20_000_000L, 500),
            new TurnoverRentRuleTierRecord("t2", "r1", 2, 30_000_000L, 800),
            new TurnoverRentRuleTierRecord("t3", "r1", 3, null, 1000));

    @Test
    void 累进_35万_低于保底则取保底() {
        // 20万*5% + 10万*8% + 5万*10% = 100万+80万+50万分 = 2_300_000 分（2.3万）；保底3万 → 3_000_000。
        long rent = TurnoverRentMath.tieredRent(35_000_000L, TIERS, 3_000_000L);
        assertThat(rent).isEqualTo(3_000_000L);
    }

    @Test
    void 累进_50万_高于保底按累进() {
        // 100万+80万+ (20万*10%=200万) 分 = 3_800_000（3.8万）> 保底 → 3_800_000。
        long rent = TurnoverRentMath.tieredRent(50_000_000L, TIERS, 3_000_000L);
        assertThat(rent).isEqualTo(3_800_000L);
    }

    @Test
    void 无保底_低销售额只按第一档() {
        // 10万*5% = 500_000。
        assertThat(TurnoverRentMath.tieredRent(10_000_000L, TIERS, null)).isEqualTo(500_000L);
    }

    @Test
    void 零销售额_无保底为零_有保底取保底() {
        assertThat(TurnoverRentMath.tieredRent(0L, TIERS, null)).isEqualTo(0L);
        assertThat(TurnoverRentMath.tieredRent(0L, TIERS, 3_000_000L)).isEqualTo(3_000_000L);
    }

    @Test
    void 边界_恰好20万只走第一档() {
        assertThat(TurnoverRentMath.tieredRent(20_000_000L, TIERS, null)).isEqualTo(1_000_000L);
    }

    // 第十八批 U 段第 155 条 155-R1（DEC-R8-54「超额累进下逐段拆分」；财务 PRD:904「按其在当月累计中的位置逐段拆分」；
    // ENUM:257「冲正金额一律按该笔款项在其原账期实际计费时所适用的档位与费率原额冲回」）：0～10000 按 5%、以上按 10%。
    private static final List<TurnoverRentRuleTierRecord> TWO = List.of(
            new TurnoverRentRuleTierRecord("t1", "r2", 1, 10_000L, 500),
            new TurnoverRentRuleTierRecord("t2", "r2", 2, null, 1000));

    @Test
    void 逐段拆分_按当月累计位置跨档() {
        assertThat(TurnoverRentMath.segments(8_000L, 6_000L, TWO)).containsExactly(
                new TurnoverRentMath.Segment(500, 2_000L), new TurnoverRentMath.Segment(1000, 4_000L));
        assertThat(TurnoverRentMath.segments(0L, 8_000L, TWO)).containsExactly(new TurnoverRentMath.Segment(500, 8_000L));
    }

    @Test
    void 部分退款按该笔各段原费率按占比冲回() {
        // (2000×5% + 4000×10%) × 3000 ÷ 6000 = 250。
        assertThat(TurnoverRentMath.reversalCent(3_000L, 6_000L, List.of(
                new TurnoverRentMath.Segment(500, 2_000L), new TurnoverRentMath.Segment(1000, 4_000L)))).isEqualTo(250L);
    }

    @Test
    void 单一费率规则按一档拆分() {
        List<TurnoverRentRuleTierRecord> single = List.of(new TurnoverRentRuleTierRecord(null, "r3", 1, null, 1000));
        assertThat(TurnoverRentMath.segments(500L, 1_000L, single)).containsExactly(new TurnoverRentMath.Segment(1000, 1_000L));
    }

    @Test
    void 冲回额各段精确累加只舍入一次() {
        // 两段各 0.45 分：逐段舍入为 0，合计 0.9 分只舍入一次为 1。
        assertThat(TurnoverRentMath.reversalCent(18L, 18L, List.of(
                new TurnoverRentMath.Segment(500, 9L), new TurnoverRentMath.Segment(500, 9L)))).isEqualTo(1L);
    }
}
