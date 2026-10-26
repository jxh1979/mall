package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * 第十八批第 1 条（ENUM:187「营业日从当日上午 10:00 起算，至次日上午 10:00 止」）：酒吧报酬月结的「本月尚未结束」按营业日判断——
 * 此刻是北京 2025-12-01 08:00，仍在 11-30 营业日内，11 月月结照旧拒绝（自然月已到 12 月）。
 * 「此刻」由解析器桩固定（服务按 Instant.now() 取当前营业日）。
 */
class BarPayrollMonthCloseBusinessDayTests {

    private static final Instant BEIJING_DEC_FIRST_EIGHT = Instant.parse("2025-12-01T00:00:00Z");

    @Test
    void monthIsNotOverUntilItsLastBusinessDayEnds() {
        BarPayrollAllocationService service = new BarPayrollAllocationService(mock(BarPayrollSupport.class),
                null, null, null, null, null, null, null);
        service.businessDays = new BusinessDayResolver() {
            @Override
            public LocalDate businessDate(String projectId, String barId, Instant at) {
                return super.businessDate(projectId, barId, BEIJING_DEC_FIRST_EIGHT);
            }
        };
        assertThatThrownBy(() -> service.close(new BarPayrollRequests.CloseMonth("bpm-close-2025-11", "prj_bpm", "bar_bpm", "2025-11")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("本月尚未结束");
    }
}
