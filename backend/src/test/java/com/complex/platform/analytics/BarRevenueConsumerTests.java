package com.complex.platform.analytics;
// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.complex.platform.bar.BarRevenueSourceService;
import com.complex.platform.bar.BarRevenueSourceService.*;
import com.complex.platform.common.BusinessDayResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;

class BarRevenueConsumerTests {
    private final LocalDate day=LocalDate.of(2026,9,4);
    private final BusinessDayResolver days=new BusinessDayResolver();
    private final Instant from=days.start(null, null, day),to=days.end(null, null, day);
    private final BarRevenueSourceService sources=mock(BarRevenueSourceService.class);
    private final NamedParameterJdbcTemplate jdbc=mock(NamedParameterJdbcTemplate.class);
    private final JdbcAnalyticsRepository repository=new JdbcAnalyticsRepository(jdbc,new ObjectMapper(),days);

    @Test void detailAndEtlUseConfirmedGoodsAndWithheldWithoutCountingCustodialCash() {
        ReflectionTestUtils.setField(repository,"barRevenueSources",sources);
        when(jdbc.queryForList(anyString(),any(SqlParameterSource.class),eq(String.class))).thenReturn(List.of("merchant"));
        when(sources.completedCollections("merchant",from,to)).thenReturn(List.of(
            new CollectionRevenue("bill","merchant","project",from,10000,8000L,1000,1000,"confirmed",List.of()),
            new CollectionRevenue("partial","merchant","project",from,3000,null,0,0,"payment_pending",List.of("整单未付清"))));
        when(sources.withheldServiceFees("merchant",day,day.plusDays(1))).thenReturn(List.of(
            new WithheldRevenue("bill","merchant","project",day,"run",300L,"confirmed",List.of()),
            new WithheldRevenue("unclosed","merchant","project",day,null,null,"pending",List.of("待月结"))));
        var detail=repository.barSettlementRevenueDetail(from,to,10,0);
        assertThat(detail.summaryValue()).isEqualByComparingTo("8300");
        assertThat(detail.totalRows()).isEqualTo(4);
        assertThat(detail.rows().stream().filter(row->row.get("settlementAmountCent")==null)).hasSize(2);
        assertThat(detail.rows().getFirst()).containsEntry("collectedAmountCent",10000L)
            .containsEntry("serviceFeeCent",1000L).containsEntry("tipCent",1000L);
        var secondPage=repository.barSettlementRevenueDetail(from,to,2,2);
        assertThat(secondPage.rows()).hasSize(2);
        assertThat(secondPage.summaryValue()).isEqualByComparingTo(detail.summaryValue());
        assertThat(repository.sumBarRevenueCentsByMerchant(day,"bar").get("merchant"))
            .isEqualByComparingTo(detail.summaryValue());
    }

    @Test void refundsCountOnlyConfirmedGoodsAndRetainOriginalReceiptAmount() {
        ReflectionTestUtils.setField(repository,"barRevenueSources",sources);
        when(jdbc.queryForList(anyString(),any(SqlParameterSource.class),eq(String.class))).thenReturn(List.of("merchant"));
        when(sources.completedGoodsRefunds("merchant",from,to)).thenReturn(List.of(
            new GoodsRefund("a","bill","merchant",from,400L,"confirmed"),
            new GoodsRefund("b","bill","merchant",from,null,"pending")));
        assertThat(repository.sumBarRefundCentsByMerchant(day,"bar").get("merchant")).isEqualByComparingTo("400");
        verify(sources).completedGoodsRefunds("merchant",from,to);
    }
}
