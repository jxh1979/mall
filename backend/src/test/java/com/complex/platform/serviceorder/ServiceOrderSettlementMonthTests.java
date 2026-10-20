// 文件版本：V0.01
package com.complex.platform.serviceorder;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.common.BusinessDayResolver;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * TK-23-R2(ENUM L291 / BSM L1138「服务点单份额与管理费的归属月份按服务完成时点所在营业日确定」;
 * ENUM L187「全系统的钱账统一按『营业日』归集」;DEC-R6-10 时区取项目所在地):
 * 归属月 = 完成时点所在 bar 营业日(Asia/Shanghai + 切日)的年月,不按 UTC 自然月。纯单元,切日固定 06:00。
 */
class ServiceOrderSettlementMonthTests {

    private final BusinessDayResolver resolver = new BusinessDayResolver(6);

    @Test
    void utcJanuaryLateNightBelongsToFebruaryBusinessDay() {
        // UTC 1/31 23:30 = 北京 2/1 07:30,已过 06:00 切日 → 营业日 2026-02-01 → 归属 2026-02。
        assertThat(ServiceOrderService.settlementMonth(resolver, Instant.parse("2026-01-31T23:30:00Z")))
                .isEqualTo("2026-02");
    }

    @Test
    void utcFebruaryMorningStaysFebruary() {
        // UTC 2/1 01:00 = 北京 09:00 → 2026-02。
        assertThat(ServiceOrderService.settlementMonth(resolver, Instant.parse("2026-02-01T01:00:00Z")))
                .isEqualTo("2026-02");
    }

    @Test
    void beijingBeforeCutoffStillBelongsToPreviousBusinessDay() {
        // UTC 1/31 21:00 = 北京 2/1 05:00,未到 06:00 切日 → 仍属 1/31 营业日 → 2026-01。
        assertThat(ServiceOrderService.settlementMonth(resolver, Instant.parse("2026-01-31T21:00:00Z")))
                .isEqualTo("2026-01");
    }
}
