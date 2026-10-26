package com.complex.platform.dividend;
// 文件版本：V0.02

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.complex.platform.bar.BarRevenueSourceService;
import com.complex.platform.bar.BarRevenueSourceService.*;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class DividendBarRevenueTests {
    private final LocalDate day=LocalDate.of(2026,9,4);
    private final BusinessDayResolver days=new BusinessDayResolver();
    private final Instant from=days.start(null, null, day),to=days.end(null, null, day);
    private final BarRevenueSourceService sources=mock(BarRevenueSourceService.class);
    private final JdbcDividendRepository repository=new JdbcDividendRepository(mock(NamedParameterJdbcTemplate.class));

    @BeforeEach void connectSource() {
        ReflectionTestUtils.setField(repository,"barRevenueSources",sources);
        ReflectionTestUtils.setField(repository,"businessDays",days);
        when(sources.completedCollections("merchant",from,to)).thenReturn(List.of(
            new CollectionRevenue("bill","merchant","project",from,10000,8000L,1000,1000,"confirmed",List.of()),
            new CollectionRevenue("partial","merchant","project",from,3000,null,0,0,"payment_pending",List.of("整单未付清"))));
        when(sources.withheldServiceFees("merchant",day,day.plusDays(1))).thenReturn(List.of(
            new WithheldRevenue("bill","merchant","project",day,"run",300L,"confirmed",List.of())));
    }

    @Test void paidGoodsAndCurrentMonthlyWithheldAreRevenueButPartialPaymentIsNot() {
        assertThat(repository.sumBarRevenueCent("merchant",from,to)).isEqualTo(8300L);
        verify(sources).withheldServiceFees("merchant",day,day.plusDays(1));
    }
    @Test void unsettledServiceFeesPreventFinalDividend() {
        when(sources.withheldServiceFees("merchant",day,day.plusDays(1))).thenReturn(List.of(
            new WithheldRevenue("bill","merchant","project",day,null,null,"pending",List.of("待月结"))));
        assertThatThrownBy(()->repository.sumBarRevenueCent("merchant",from,to))
            .isInstanceOf(ApiException.class).hasMessageContaining("不能确认分红");
    }
    @Test void refundWithoutConfirmedBreakdownCannotBeAssumedAllGoods() {
        when(sources.completedGoodsRefunds("merchant",from,to)).thenReturn(List.of(
            new GoodsRefund("a","bill","merchant",from,null,"pending")));
        assertThatThrownBy(()->repository.sumBarRefundCent("merchant",from,to))
            .isInstanceOf(ApiException.class).hasMessageContaining("不能确认分红");
        when(sources.completedGoodsRefunds("merchant",from,to)).thenReturn(List.of(
            new GoodsRefund("a","bill","merchant",from,400L,"confirmed")));
        assertThat(repository.sumBarRefundCent("merchant",from,to)).isEqualTo(400L);
    }
}
