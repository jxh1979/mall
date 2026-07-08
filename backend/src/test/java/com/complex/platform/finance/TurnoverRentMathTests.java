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
}
