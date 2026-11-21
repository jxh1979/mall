package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 第十八批 BP 段第 359 条 359-R1、359-R3：动态价条件的时段左闭右开、跨零点两侧边界，到访当天已售门槛，以及复核单摘要文字。
 */
class TicketDynamicPricingConditionsTests {

    private static final Map<String, Object> OVERNIGHT = Map.of("time_from", "22:00", "time_to", "02:00");

    @Test
    void overnightWindowIncludesStartAndExcludesEndOnBothSidesOfMidnight() {
        assertThat(matches(OVERNIGHT, "21:59")).isFalse();
        assertThat(matches(OVERNIGHT, "22:00")).isTrue();
        assertThat(matches(OVERNIGHT, "01:59")).isTrue();
        assertThat(matches(OVERNIGHT, "02:00")).isFalse();
        Map<String, Object> evening = Map.of("time_from", "20:00", "time_to", "22:00");
        assertThat(matches(evening, "20:00")).isTrue();
        assertThat(matches(evening, "22:00")).isFalse();
        assertThat(matches(Map.of(), "03:00")).isTrue();
    }

    @Test
    void daySoldThresholdMatchesFromTheThresholdOn() {
        Map<String, Object> rule = Map.of("day_sold_min", 5);
        assertThat(TicketDynamicPricingConditions.matches(rule, LocalTime.NOON, () -> 4)).isFalse();
        assertThat(TicketDynamicPricingConditions.matches(rule, LocalTime.NOON, () -> 5)).isTrue();
    }

    @Test
    void describeWritesTheWindowAndTheThresholdInChinese() {
        assertThat(TicketDynamicPricingConditions.describe(Map.of("time_from", "20:00", "time_to", "22:00"))).isEqualTo("时段 20:00～22:00");
        assertThat(TicketDynamicPricingConditions.describe(Map.of("time_from", "22:00", "time_to", "02:00", "day_sold_min", 50)))
                .isEqualTo("时段 22:00～次日 02:00 · 到访当天已售满 50 张起");
        assertThat(TicketDynamicPricingConditions.describe(Map.of())).isEmpty();
    }

    private static boolean matches(Map<String, Object> rule, String at) {
        return TicketDynamicPricingConditions.matches(rule, LocalTime.parse(at), () -> 0);
    }
}
