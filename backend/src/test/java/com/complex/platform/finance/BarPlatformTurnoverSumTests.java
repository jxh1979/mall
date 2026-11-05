package com.complex.platform.finance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.bar.BarRevenueSourceService;
import com.complex.platform.bar.BarRevenueSourceService.CollectionRevenue;
import com.complex.platform.bar.BarRevenueSourceService.GoodsRefund;
import com.complex.platform.bar.BarRevenueSourceService.WithheldRevenue;
import com.complex.platform.common.api.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 第十八批 Q 段第 138 条 138-R1（BSM:1070「商户营业额与营业额抽成基数按打折后的实收金额计」；BSM:1076「月结定格的扣留部分，扣留金额计入该商户营业额、
 * 抽成基数」；BSM:1347「减去当期已成功的退款金额」；BSM:1111「挂起订单在结清前不计入……营业额抽成基数」；ENUM:257「……服务费应发数与扣留数定数之后出账，
 * 账单一次出准，不使用预估数」）：酒吧平台内营业额＝已确认整单商品收入＋月结扣留−已退成商品分项；待付清的整单跳过，来源未定一律 409。
 */
class BarPlatformTurnoverSumTests {

    private final LocalDate first = LocalDate.of(2026, 7, 1);
    private final LocalDate after = LocalDate.of(2026, 8, 1);
    private final Instant from = Instant.parse("2026-07-01T02:00:00Z");
    private final Instant to = Instant.parse("2026-08-01T02:00:00Z");
    private final BarRevenueSourceService sources = mock(BarRevenueSourceService.class);
    private final JdbcFinanceRepository repository = new JdbcFinanceRepository(mock(NamedParameterJdbcTemplate.class), new ObjectMapper());

    @BeforeEach
    @SuppressWarnings("unchecked")
    void connectSource() {
        ObjectProvider<BarRevenueSourceService> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(sources);
        ReflectionTestUtils.setField(repository, "barRevenueSources", provider);
        when(sources.completedCollections("bar", from, to)).thenReturn(List.of(
                new CollectionRevenue("bill", "bar", "project", from, 10000, 8000L, 1000, 1000, "confirmed", List.of()),
                new CollectionRevenue("partial", "bar", "project", from, 3000, null, 0, 0, "payment_pending", List.of("订单尚未全渠道足额完成付款"))));
        when(sources.withheldServiceFees("bar", first, after)).thenReturn(List.of(
                new WithheldRevenue("bill", "bar", "project", first, "run", 300L, "confirmed", List.of())));
        when(sources.completedGoodsRefunds("bar", from, to)).thenReturn(List.of(
                new GoodsRefund("adj", "bill", "bar", from, 400L, "confirmed")));
    }

    @Test
    void confirmedGoodsPlusWithheldMinusRefundedGoods() {
        assertThat(repository.sumBarPlatformTurnoverCent("bar", from, to, first, after)).isEqualTo(7900L);
    }

    @Test
    void unresolvedCollectionBlocksTheBill() {
        when(sources.completedCollections("bar", from, to)).thenReturn(List.of(
                new CollectionRevenue("bill", "bar", null, from, 10000, null, 0, 0, "pending", List.of("收款账单缺少实际综合体"))));
        assertPending();
    }

    @Test
    void serviceFeeAwaitingMonthCloseBlocksTheBill() {
        when(sources.withheldServiceFees("bar", first, after)).thenReturn(List.of(
                new WithheldRevenue("bill", "bar", "project", first, null, null, "pending", List.of("服务费尚未月结"))));
        assertPending();
    }

    @Test
    void refundWithoutConfirmedBreakdownBlocksTheBill() {
        when(sources.completedGoodsRefunds("bar", from, to)).thenReturn(List.of(
                new GoodsRefund("adj", "bill", "bar", from, null, "pending")));
        assertPending();
    }

    private void assertPending() {
        assertThatThrownBy(() -> repository.sumBarPlatformTurnoverCent("bar", from, to, first, after))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("BAR_REVENUE_PENDING"))
                .hasMessageContaining("暂不能出营业额抽成账单");
    }
}
