package com.complex.platform.analytics;

// 文件版本：V0.01

import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.analytics.AnalyticsRecords.MetricDetailResponse;
import com.complex.platform.analytics.AnalyticsRecords.MetricDetailRows;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * KPI 统一明细（指标登记簿，2026-07-18 方案A）内存单测：
 * 口径铁律分派（可加=区间求和 / 比率=分子÷分母两页签 / 余额=区间末值）、
 * 「数字=明细合计」同源（summaryValue 与明细行来自同一仓储调用）、登记项校验 fail-closed、审计事件。
 * 真库行级一致性见 MetricDetailConsistencyJdbcIntegrationTests（JDBC 集成，按环境跑）。
 */
class MetricDetailServiceTests {

    // 日切小时按所在地（北京）时间读：2026-07-18T02:00Z = 北京 07-18 10:00，已过 04:00 日切 → 当前经营日 = 2026-07-18。
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-18T02:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate CURRENT_BUSINESS_DAY = LocalDate.parse("2026-07-18");
    // 经营日 D 的窗口 = 北京 D 04:00（含）~ D+1 04:00（不含）；D=07-18 → [07-17T20:00Z, 07-18T20:00Z)。
    private static final Instant CURRENT_WINDOW_START = Instant.parse("2026-07-17T20:00:00Z");
    private static final Instant CURRENT_WINDOW_END = Instant.parse("2026-07-18T20:00:00Z");

    private AnalyticsRepository repository;
    private InMemoryAuditEventRepository auditEvents;
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        repository = mock(AnalyticsRepository.class);
        auditEvents = new InMemoryAuditEventRepository();
        AuditedOperationExecutor operations = new AuditedOperationExecutor(
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEvents)
        );
        service = new AnalyticsService(repository, operations, FIXED_CLOCK, null);
    }

    @Test
    void additiveMetricDefaultsToCurrentBusinessDayAndSummaryEqualsDetailSum() {
        List<Map<String, Object>> rows = List.of(
                row("id", "t1", "orderNo", "TK-1", "amountCent", 10000L),
                row("id", "t2", "orderNo", "TK-2", "amountCent", 2500L));
        when(repository.ticketRevenueDetail(any(), any(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(rows, 2, new BigDecimal("12500")));

        MetricDetailResponse response = service.metricDetail("revenue.ticket", null, null, null, 1, 20);

        // 数字=明细合计（同一仓储调用同一谓词）：summaryValue == Σ 明细行 amountCent。
        long rowSum = response.rows().stream().mapToLong(item -> ((Number) item.get("amountCent")).longValue()).sum();
        assertThat(response.summaryValue()).isEqualByComparingTo(BigDecimal.valueOf(rowSum));
        assertThat(response.valueType()).isEqualTo("additive");
        assertThat(response.unit()).isEqualTo("分");
        assertThat(response.from()).isEqualTo(CURRENT_BUSINESS_DAY);
        assertThat(response.to()).isEqualTo(CURRENT_BUSINESS_DAY);
        assertThat(response.dataAsOf()).isEqualTo(FIXED_CLOCK.instant());
        assertThat(response.caliber()).contains("区间求和");
        assertThat(response.traceLinkPageId()).isEqualTo("ADM-TKT-003");
        assertThat(response.traceLinkColumnKey()).isEqualTo("id");
        assertThat(response.columns()).extracting(MetricRegistry.MetricColumn::key).contains("orderNo", "amountCent");

        // 经营日窗（ticketing 缺省 04:00）：北京 [07-18 04:00, 07-19 04:00) = [07-17T20:00Z, 07-18T20:00Z)。
        ArgumentCaptor<Instant> start = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> end = ArgumentCaptor.forClass(Instant.class);
        verify(repository).ticketRevenueDetail(start.capture(), end.capture(), eq(20), eq(0));
        assertThat(start.getValue()).isEqualTo(CURRENT_WINDOW_START);
        assertThat(end.getValue()).isEqualTo(CURRENT_WINDOW_END);

        assertThat(auditEvents.events).extracting(AuditEvent::eventName).contains("analytics.metric_detail.viewed");
    }

    @Test
    void reconDiffCountWithoutRangeQueriesFullHistory() {
        when(repository.reconciliationDifferenceDetail(any(), any(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "d1")), 3, new BigDecimal("3")));

        MetricDetailResponse response = service.metricDetail("finance.recon_diff_count", null, null, null, 1, 20);

        assertThat(response.summaryValue()).isEqualByComparingTo("3");
        assertThat(response.totalRows()).isEqualTo(3);
        assertThat(response.from()).isNull();
        assertThat(response.to()).isNull();
        verify(repository).reconciliationDifferenceDetail(eq(null), eq(null), eq(20), eq(0));
    }

    @Test
    void ratioMetricDividesNumeratorByDenominatorAndSwitchesColumnsByPart() {
        when(repository.electricityReadingDetail(any(), any(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "r1", "consumption", new BigDecimal("1200"))), 1, new BigDecimal("1200")));
        when(repository.rentableSpaceDetail(anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "sp1", "areaSquareMeter", new BigDecimal("400"))), 1, new BigDecimal("400")));

        MetricDetailResponse numerator = service.metricDetail("energy.electricity_per_sqm", null, null, null, 1, 20);
        assertThat(numerator.part()).isEqualTo("numerator");
        assertThat(numerator.summaryValue()).isEqualByComparingTo("3.0000");
        assertThat(numerator.numeratorValue()).isEqualByComparingTo("1200");
        assertThat(numerator.denominatorValue()).isEqualByComparingTo("400");
        assertThat(numerator.columns()).extracting(MetricRegistry.MetricColumn::key).contains("consumption");
        assertThat(numerator.traceLinkPageId()).isEqualTo("ADM-ENE-001");

        MetricDetailResponse denominator = service.metricDetail("energy.electricity_per_sqm", "denominator", null, null, 1, 20);
        assertThat(denominator.part()).isEqualTo("denominator");
        assertThat(denominator.columns()).extracting(MetricRegistry.MetricColumn::key).contains("spaceName", "areaSquareMeter");
        // 分母页签行=空间台账，无单据下钻。
        assertThat(denominator.traceLinkPageId()).isNull();
    }

    @Test
    void ratioMetricWithZeroDenominatorDoesNotFabricateValue() {
        when(repository.electricityReadingDetail(any(), any(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(List.of(), 0, new BigDecimal("1200")));
        when(repository.rentableSpaceDetail(anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(List.of(), 0, BigDecimal.ZERO));

        MetricDetailResponse response = service.metricDetail("energy.electricity_per_sqm", null, null, null, 1, 20);

        assertThat(response.summaryValue()).isNull();
        assertThat(response.summaryNote()).contains("不产出");
    }

    @Test
    void balanceMetricUsesEndOfRangeAsOfAndIgnoresFrom() {
        ArgumentCaptor<LocalDate> asOf = ArgumentCaptor.forClass(LocalDate.class);
        when(repository.arOpenReceivableDetail(any(), anyInt(), anyInt(), any()))
                .thenReturn(new MetricDetailRows(
                        List.of(row("id", "ar1", "unpaidAmountCent", 70000L)), 1, new BigDecimal("70000")));

        MetricDetailResponse response = service.metricDetail(
                "finance.ar_balance", null, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-10"), 1, 20);

        // 平台口径（6 参重载=projectScope null）：仓储收到 null scope（看全量）。
        verify(repository).arOpenReceivableDetail(asOf.capture(), eq(20), eq(0), isNull());
        // 余额类=区间末值：as-of=to，from 不参与（summaryNote 说明）。
        assertThat(asOf.getValue()).isEqualTo(LocalDate.parse("2026-07-10"));
        assertThat(response.from()).isNull();
        assertThat(response.to()).isEqualTo(LocalDate.parse("2026-07-10"));
        assertThat(response.summaryValue()).isEqualByComparingTo("70000");
        assertThat(response.summaryNote()).contains("区间末值");
        assertThat(response.valueType()).isEqualTo("balance");
    }

    @Test
    void balanceMetricDefaultsAsOfToCurrentBusinessDay() {
        when(repository.arOpenReceivableDetail(any(), anyInt(), anyInt(), any()))
                .thenReturn(new MetricDetailRows(List.of(), 0, BigDecimal.ZERO));

        MetricDetailResponse response = service.metricDetail("finance.ar_balance", null, null, null, 1, 20);

        verify(repository).arOpenReceivableDetail(eq(CURRENT_BUSINESS_DAY), eq(20), eq(0), isNull());
        assertThat(response.to()).isEqualTo(CURRENT_BUSINESS_DAY);
        assertThat(response.summaryNote()).isNull();
    }

    @Test
    void validationFailsClosedOnUnknownMetricInvalidPartAndReversedRange() {
        assertThatThrownBy(() -> service.metricDetail("revenue.unknown", null, null, null, 1, 20))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("未登记的指标编码");
        assertThatThrownBy(() -> service.metricDetail("revenue.ticket", "numerator", null, null, 1, 20))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("仅比率类指标可用");
        assertThatThrownBy(() -> service.metricDetail("energy.electricity_per_sqm", "half", null, null, 1, 20))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("numerator/denominator");
        assertThatThrownBy(() -> service.metricDetail(
                "revenue.ticket", null, LocalDate.parse("2026-07-10"), LocalDate.parse("2026-07-01"), 1, 20))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("起始日期不能晚于结束日期");
        assertThat(auditEvents.events).extracting(AuditEvent::eventName).contains("analytics.metric_detail.view_failed");
    }

    // ==== 第二批（酒吧域）分派 ====

    @Test
    void barRevenueAdditiveDefaultsToCurrentBusinessDayWindow() {
        when(repository.barSettlementRevenueDetail(any(), any(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(
                        List.of(row("id", "bbs1", "billId", "bill-1", "settlementAmountCent", 8800L)), 1, new BigDecimal("8800")));

        MetricDetailResponse response = service.metricDetail("revenue.bar", null, null, null, 1, 20);

        long rowSum = response.rows().stream().mapToLong(item -> ((Number) item.get("settlementAmountCent")).longValue()).sum();
        assertThat(response.summaryValue()).isEqualByComparingTo(BigDecimal.valueOf(rowSum));
        assertThat(response.unit()).isEqualTo("分");
        assertThat(response.caliber()).contains("冲正");
        assertThat(response.traceLinkPageId()).isEqualTo("ADM-BAR-002");
        assertThat(response.traceLinkColumnKey()).isEqualTo("billId");
        // 酒吧经营日窗（单测无域配置回落全局 04:00）：北京 [07-18 04:00, 07-19 04:00) = [07-17T20:00Z, 07-18T20:00Z)。
        ArgumentCaptor<Instant> start = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> end = ArgumentCaptor.forClass(Instant.class);
        verify(repository).barSettlementRevenueDetail(start.capture(), end.capture(), eq(20), eq(0));
        assertThat(start.getValue()).isEqualTo(CURRENT_WINDOW_START);
        assertThat(end.getValue()).isEqualTo(CURRENT_WINDOW_END);
    }

    @Test
    void barOrderCountAndAmountWithoutRangeQueryFullHistory() {
        when(repository.barOrderDetail(any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(
                        List.of(row("id", "bo1", "totalAmountCent", 12000L)), 3, new BigDecimal("3")));

        MetricDetailResponse count = service.metricDetail("bar.order_count", null, null, null, 1, 20);
        assertThat(count.summaryValue()).isEqualByComparingTo("3");
        assertThat(count.unit()).isEqualTo("笔");
        assertThat(count.from()).isNull();
        assertThat(count.to()).isNull();
        verify(repository).barOrderDetail(eq(null), eq(null), eq(false), eq(20), eq(0));

        when(repository.barOrderDetail(any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(
                        List.of(row("id", "bo1", "totalAmountCent", 12000L)), 1, new BigDecimal("12000")));
        MetricDetailResponse amount = service.metricDetail("bar.order_amount", null, null, null, 1, 20);
        assertThat(amount.summaryValue()).isEqualByComparingTo("12000");
        assertThat(amount.unit()).isEqualTo("分");
        verify(repository).barOrderDetail(eq(null), eq(null), eq(true), eq(20), eq(0));
    }

    @Test
    void barOpenOrderBalancePassesBarWindowEndAsOfAndIgnoresFrom() {
        when(repository.barOpenOrderDetail(any(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "bo-open")), 1, new BigDecimal("1")));

        MetricDetailResponse response = service.metricDetail(
                "bar.open_order_count", null, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-10"), 1, 20);

        // 余额类=区间末值：as-of=to 的酒吧经营日窗末（单测回落全局 04:00 → 北京 07-11 04:00 = 07-10T20:00Z），from 不参与。
        verify(repository).barOpenOrderDetail(eq(Instant.parse("2026-07-10T20:00:00Z")), eq(20), eq(0));
        assertThat(response.from()).isNull();
        assertThat(response.to()).isEqualTo(LocalDate.parse("2026-07-10"));
        assertThat(response.summaryNote()).contains("区间末值");
        assertThat(response.valueType()).isEqualTo("balance");
    }

    @Test
    void savedBottleMetricsDispatchByLedgerCurrentState() {
        when(repository.savedBottleDetail(any(), any(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "sb1", "remainingQuantity", 2)), 4, new BigDecimal("4")));
        when(repository.savedBottleStoredDetail(any(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "sb1", "status", "stored")), 2, new BigDecimal("2")));
        when(repository.savedBottleQuantityDetail(any(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "sb1", "remainingQuantity", 2)), 4, new BigDecimal("7")));

        // 可加计数：不传日期=全部历史（与存酒页全量列表一致）。
        MetricDetailResponse count = service.metricDetail("bar.saved_bottle_count", null, null, null, 1, 20);
        assertThat(count.summaryValue()).isEqualByComparingTo("4");
        verify(repository).savedBottleDetail(eq(null), eq(null), eq(20), eq(0));

        // 余额类缺省 as-of=当前经营日（07-18）→ 窗末 = 北京 07-19 04:00 = 07-18T20:00Z（单测回落全局日切）。
        MetricDetailResponse stored = service.metricDetail("bar.saved_bottle_stored", null, null, null, 1, 20);
        assertThat(stored.summaryValue()).isEqualByComparingTo("2");
        assertThat(stored.to()).isEqualTo(CURRENT_BUSINESS_DAY);
        verify(repository).savedBottleStoredDetail(eq(CURRENT_WINDOW_END), eq(20), eq(0));

        MetricDetailResponse quantity = service.metricDetail("bar.saved_bottle_quantity", null, null, null, 1, 20);
        assertThat(quantity.summaryValue()).isEqualByComparingTo("7");
        assertThat(quantity.caliber()).contains("直加");
        verify(repository).savedBottleQuantityDetail(eq(CURRENT_WINDOW_END), eq(20), eq(0));
    }

    // ==== 第三批（票务域）分派 ====

    @Test
    void ticketOrderCountAndPaidWithoutRangeQueryFullHistory() {
        when(repository.ticketOrderDetail(any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "to1", "amountCent", 10000L)), 5, new BigDecimal("5")));

        MetricDetailResponse count = service.metricDetail("ticket.order_count", null, null, null, 1, 20);
        assertThat(count.summaryValue()).isEqualByComparingTo("5");
        assertThat(count.unit()).isEqualTo("笔");
        assertThat(count.from()).isNull();
        assertThat(count.to()).isNull();
        assertThat(count.traceLinkPageId()).isEqualTo("ADM-TKT-003");
        verify(repository).ticketOrderDetail(eq(null), eq(null), eq(false), eq(20), eq(0));

        service.metricDetail("ticket.paid_order_count", null, null, null, 1, 20);
        verify(repository).ticketOrderDetail(eq(null), eq(null), eq(true), eq(20), eq(0));
    }

    @Test
    void ticketPendingIssueBalancePassesTicketingWindowEndAsOf() {
        when(repository.ticketPendingIssueDetail(any(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "to-pend")), 2, new BigDecimal("2")));

        MetricDetailResponse response = service.metricDetail(
                "ticket.pending_issue_count", null, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-10"), 1, 20);

        // 余额类=区间末值：as-of=to 的票务经营日窗末（单测回落全局 04:00 → 北京 07-11 04:00 = 07-10T20:00Z），from 不参与。
        verify(repository).ticketPendingIssueDetail(eq(Instant.parse("2026-07-10T20:00:00Z")), eq(20), eq(0));
        assertThat(response.from()).isNull();
        assertThat(response.to()).isEqualTo(LocalDate.parse("2026-07-10"));
        assertThat(response.valueType()).isEqualTo("balance");
    }

    @Test
    void amusementMetricsUseComplexVersionDatesAndProjectScope() {
        when(repository.amusementComplexDetail(any(),any(),any(),any(),org.mockito.ArgumentMatchers.anyBoolean(),anyInt(),anyInt(),any()))
                .thenReturn(new MetricDetailRows(List.of(row("id","v1","complexId","complex-a")),3,new BigDecimal("3")));
        for(String key:List.of("amusement.session_count","amusement.group_booking_count","amusement.group_reserved_capacity",
                "amusement.entry_count","amusement.exit_count","amusement.inspection_count")) {
            MetricDetailResponse response=service.metricDetail(key,null,null,null,1,20,List.of("complex-a"));
            assertThat(response.summaryValue()).isEqualByComparingTo("3");
            verify(repository).amusementComplexDetail(eq(key),eq(null),eq(null),any(),eq(true),eq(20),eq(0),eq(List.of("complex-a")));
        }
        MetricDetailResponse capacity=service.metricDetail("amusement.session_capacity",null,null,null,1,20,List.of("complex-a"));
        assertThat(capacity.valueType()).isEqualTo("balance");
        assertThat(capacity.unit()).isEqualTo("人");
        verify(repository).amusementComplexDetail(eq("amusement.session_capacity"),eq(null),eq(null),any(),eq(false),eq(20),eq(0),eq(List.of("complex-a")));
        MetricDetailResponse pending=service.metricDetail("amusement.inspection_pending_rectification",null,null,null,1,20,List.of("complex-a"));
        assertThat(pending.to()).isNull();
        assertThat(pending.valueType()).isEqualTo("balance");
        verify(repository).amusementComplexDetail(eq("amusement.inspection_pending_rectification"),eq(null),eq(null),any(),eq(false),eq(20),eq(0),eq(List.of("complex-a")));
        verify(repository,org.mockito.Mockito.never()).amusementSessionDetail(any(),any(),org.mockito.ArgumentMatchers.anyBoolean(),anyInt(),anyInt());
    }

    @Test
    void registryFirstBatchCoversThreeValueTypesWithPermissionsAndCalibers() {
        assertThat(MetricRegistry.all()).extracting(MetricRegistry.MetricDefinition::metricKey)
                .containsExactly("revenue.ticket", "finance.recon_diff_count", "energy.total_electricity",
                        "energy.electricity_per_sqm", "finance.ar_balance",
                        // 第二批（酒吧域）
                        "revenue.bar", "bar.order_count", "bar.order_amount", "bar.open_order_count",
                        "bar.saved_bottle_count", "bar.saved_bottle_stored", "bar.saved_bottle_quantity",
                        // 第三批（票务域）
                        "ticket.order_count", "ticket.paid_order_count", "ticket.pending_issue_count",
                        "amusement.session_count", "amusement.session_capacity",
                        "amusement.group_booking_count", "amusement.group_reserved_capacity",
                        "amusement.entry_count", "amusement.exit_count",
                        "amusement.inspection_count", "amusement.inspection_pending_rectification",
                        // 第四批（财务域）
                        "finance.ap_balance", "finance.ar_balance_aging90p",
                        "settlement.transaction_count", "settlement.verified_amount", "settlement.verified_count",
                        // 第五批（人事域）
                        "staff.shift_count", "staff.attendance_count", "staff.attendance_late_count",
                        "staff.payroll_net_total", "staff.payroll_statement_count",
                        "staff.pending_leave_count", "staff.active_headcount",
                        // P0总览剩余四卡
                        "traffic.amusement", "verify.done", "member.new", "risk.pending_review");
        assertThat(MetricRegistry.all()).extracting(MetricRegistry.MetricDefinition::valueType)
                .contains(MetricRegistry.TYPE_ADDITIVE, MetricRegistry.TYPE_RATIO, MetricRegistry.TYPE_BALANCE);
        assertThat(MetricRegistry.require("finance.recon_diff_count").permission()).isEqualTo("finance.reconciliation.view");
        assertThat(MetricRegistry.require("energy.total_electricity").permission()).isEqualTo("energy.reading.manage");
        assertThat(MetricRegistry.require("finance.ar_balance").permission()).isEqualTo("finance.ar_ap.view");
        // 第二批权限=各页既有域权限（宽权限+登记项二次校验范式）。
        assertThat(MetricRegistry.require("revenue.bar").permission()).isEqualTo("analytics.dashboard.view");
        assertThat(MetricRegistry.require("bar.order_count").permission()).isEqualTo("bar.order.view");
        assertThat(MetricRegistry.require("bar.open_order_count").permission()).isEqualTo("bar.order.view");
        assertThat(MetricRegistry.require("bar.saved_bottle_count").permission()).isEqualTo("bar.saved_bottle.manage");
        assertThat(MetricRegistry.require("bar.saved_bottle_quantity").permission()).isEqualTo("bar.saved_bottle.manage");
        // 第三批权限=各页既有域权限（宽权限+登记项二次校验范式）。
        assertThat(MetricRegistry.require("ticket.order_count").permission()).isEqualTo("ticket.order.read");
        assertThat(MetricRegistry.require("ticket.pending_issue_count").permission()).isEqualTo("ticket.order.read");
        assertThat(MetricRegistry.require("amusement.session_count").permission()).isEqualTo("amusement.session.view");
        assertThat(MetricRegistry.require("amusement.group_booking_count").permission()).isEqualTo("amusement.group_booking.view");
        assertThat(MetricRegistry.require("amusement.entry_count").permission()).isEqualTo("amusement.verification.view");
        assertThat(MetricRegistry.require("amusement.inspection_pending_rectification").permission())
                .isEqualTo("amusement.safety_inspection.view");
        // 第四批权限=各财务页既有域权限。
        assertThat(MetricRegistry.require("finance.ap_balance").permission()).isEqualTo("finance.ar_ap.view");
        assertThat(MetricRegistry.require("finance.ar_balance_aging90p").permission()).isEqualTo("finance.ar_ap.view");
        assertThat(MetricRegistry.require("settlement.transaction_count").permission()).isEqualTo("finance.settlement.view");
        assertThat(MetricRegistry.require("settlement.verified_amount").permission()).isEqualTo("finance.settlement.view");
        assertThat(MetricRegistry.require("settlement.verified_count").permission()).isEqualTo("finance.settlement.view");
        // 第五批权限=各人事页既有域权限（宽权限 analytics.dashboard.view + 登记项二次校验范式）。
        assertThat(MetricRegistry.require("staff.shift_count").permission()).isEqualTo("staff.shift.manage");
        assertThat(MetricRegistry.require("staff.attendance_count").permission()).isEqualTo("staff.attendance.view");
        assertThat(MetricRegistry.require("staff.attendance_late_count").permission()).isEqualTo("staff.attendance.view");
        assertThat(MetricRegistry.require("staff.payroll_net_total").permission()).isEqualTo("staff.payroll.manage");
        assertThat(MetricRegistry.require("staff.payroll_statement_count").permission()).isEqualTo("staff.payroll.manage");
        assertThat(MetricRegistry.require("staff.pending_leave_count").permission()).isEqualTo("staff.shift.manage");
        assertThat(MetricRegistry.require("staff.active_headcount").permission()).isEqualTo("access.user.view");
        // 每条登记必须有中文名/口径说明/数据源说明/列定义（明细页顶部展示，业务中文）。
        for (MetricRegistry.MetricDefinition definition : MetricRegistry.all()) {
            assertThat(definition.name()).isNotBlank();
            assertThat(definition.caliber()).isNotBlank();
            assertThat(definition.source()).isNotBlank();
            assertThat(definition.columns()).isNotEmpty();
            if (MetricRegistry.TYPE_RATIO.equals(definition.valueType())) {
                assertThat(definition.denominatorColumns()).isNotEmpty();
            }
        }
    }

    @Test
    void financeFourthBatchDispatchByMetricKey() {
        // 应付余额（as-of=当前财务经营日；财务缺省日切回落全局日切）
        when(repository.apOpenPayableDetail(any(), anyInt(), anyInt(), any()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "ap1", "unpaidAmountCent", 30000L)), 1, new BigDecimal("30000")));
        MetricDetailResponse ap = service.metricDetail("finance.ap_balance", null, null, null, 1, 20);
        assertThat(ap.valueType()).isEqualTo("balance");
        assertThat(ap.unit()).isEqualTo("分");
        assertThat(ap.to()).isEqualTo(CURRENT_BUSINESS_DAY);
        assertThat(ap.summaryValue()).isEqualByComparingTo("30000");
        verify(repository).apOpenPayableDetail(eq(CURRENT_BUSINESS_DAY), eq(20), eq(0), isNull());

        // 坏账预警区（应收逾期90+，as-of 口径）
        when(repository.arOverdueAging90pDetail(any(), anyInt(), anyInt(), any()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "ag1", "unpaidAmountCent", 12000L)), 1, new BigDecimal("12000")));
        MetricDetailResponse aging = service.metricDetail("finance.ar_balance_aging90p", null, null, null, 1, 20);
        assertThat(aging.valueType()).isEqualTo("balance");
        assertThat(aging.summaryValue()).isEqualByComparingTo("12000");
        verify(repository).arOverdueAging90pDetail(eq(CURRENT_BUSINESS_DAY), eq(20), eq(0), isNull());

        // 结算三指标：同一 settlementTransactionDetail 按 summaryMode 分派（count/amount/tickets），全历史（rangeFilterOptional）。
        when(repository.settlementTransactionDetail(any(), any(), org.mockito.ArgumentMatchers.anyString(), anyInt(), anyInt(), any()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "s1")), 4, new BigDecimal("4")));
        MetricDetailResponse cnt = service.metricDetail("settlement.transaction_count", null, null, null, 1, 20);
        assertThat(cnt.unit()).isEqualTo("笔");
        assertThat(cnt.from()).isNull();
        assertThat(cnt.to()).isNull();
        verify(repository).settlementTransactionDetail(eq(null), eq(null), eq("count"), eq(20), eq(0), isNull());
        service.metricDetail("settlement.verified_amount", null, null, null, 1, 20);
        verify(repository).settlementTransactionDetail(eq(null), eq(null), eq("amount"), eq(20), eq(0), isNull());
        service.metricDetail("settlement.verified_count", null, null, null, 1, 20);
        verify(repository).settlementTransactionDetail(eq(null), eq(null), eq("tickets"), eq(20), eq(0), isNull());
    }

    @Test
    void staffFifthBatchDispatchByMetricKey() {
        // 可加类（人事域）：班次/考勤/薪资单按 LocalDate 区间端点分派（effectiveFrom/effectiveTo），缺省=当前经营日。
        when(repository.staffShiftDetail(any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "sh1")), 2, new BigDecimal("2")));
        MetricDetailResponse shift = service.metricDetail("staff.shift_count", null, null, null, 1, 20);
        assertThat(shift.valueType()).isEqualTo("additive");
        assertThat(shift.unit()).isEqualTo("班");
        assertThat(shift.from()).isEqualTo(CURRENT_BUSINESS_DAY);
        assertThat(shift.to()).isEqualTo(CURRENT_BUSINESS_DAY);
        assertThat(shift.summaryValue()).isEqualByComparingTo("2");
        verify(repository).staffShiftDetail(eq(CURRENT_BUSINESS_DAY), eq(CURRENT_BUSINESS_DAY), eq(20), eq(0), isNull());

        // 考勤打卡数 / 迟到打卡数：同一 staffAttendanceDetail 按 lateOnly 分派。
        when(repository.staffAttendanceDetail(any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), anyInt(), anyInt(), any()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "at1")), 3, new BigDecimal("3")));
        service.metricDetail("staff.attendance_count", null, LocalDate.parse("2026-07-15"), LocalDate.parse("2026-07-15"), 1, 20);
        verify(repository).staffAttendanceDetail(eq(LocalDate.parse("2026-07-15")), eq(LocalDate.parse("2026-07-15")), eq(false), eq(20), eq(0), isNull());
        service.metricDetail("staff.attendance_late_count", null, LocalDate.parse("2026-07-15"), LocalDate.parse("2026-07-15"), 1, 20);
        verify(repository).staffAttendanceDetail(eq(LocalDate.parse("2026-07-15")), eq(LocalDate.parse("2026-07-15")), eq(true), eq(20), eq(0), isNull());

        // 实发合计 / 薪资单数：同一 staffPayrollDetail 按 sumTotal 分派。
        when(repository.staffPayrollDetail(any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), anyInt(), anyInt(), any()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "pay1", "totalCent", 120000L)), 1, new BigDecimal("120000")));
        MetricDetailResponse total = service.metricDetail("staff.payroll_net_total", null, LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-01"), 1, 20);
        assertThat(total.unit()).isEqualTo("分");
        assertThat(total.summaryValue()).isEqualByComparingTo("120000");
        verify(repository).staffPayrollDetail(eq(LocalDate.parse("2026-06-01")), eq(LocalDate.parse("2026-06-01")), eq(true), eq(20), eq(0), isNull());
        service.metricDetail("staff.payroll_statement_count", null, LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-01"), 1, 20);
        verify(repository).staffPayrollDetail(eq(LocalDate.parse("2026-06-01")), eq(LocalDate.parse("2026-06-01")), eq(false), eq(20), eq(0), isNull());

        // 余额类（人事域）：待审请假 / 在职员工数 as-of=to（缺省=当前经营日），from 不参与。
        when(repository.staffPendingLeaveDetail(any(), anyInt(), anyInt(), any()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "lv1")), 1, new BigDecimal("1")));
        MetricDetailResponse leave = service.metricDetail("staff.pending_leave_count", null, null, null, 1, 20);
        assertThat(leave.valueType()).isEqualTo("balance");
        assertThat(leave.unit()).isEqualTo("笔");
        assertThat(leave.to()).isEqualTo(CURRENT_BUSINESS_DAY);
        verify(repository).staffPendingLeaveDetail(eq(CURRENT_BUSINESS_DAY), eq(20), eq(0), isNull());

        when(repository.staffActiveHeadcountDetail(any(), anyInt(), anyInt(), any()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "u1")), 5, new BigDecimal("5")));
        MetricDetailResponse headcount = service.metricDetail("staff.active_headcount", null, null, null, 1, 20);
        assertThat(headcount.valueType()).isEqualTo("balance");
        assertThat(headcount.unit()).isEqualTo("人");
        assertThat(headcount.summaryValue()).isEqualByComparingTo("5");
        verify(repository).staffActiveHeadcountDetail(eq(CURRENT_BUSINESS_DAY), eq(20), eq(0), isNull());
    }

    // ==== 项目作用域收紧（2026-07-19 用户拍板：只有平台级管理者看平台口径聚合；项目级只看本项目）====

    @Test
    void platformOnlyMetricsRejectProjectScopedCallerFailClosed() {
        // 平台口径指标（来源表无综合体维度）遇非空项目 scope → 403（服务层纵深防御，防绕过 controller 直调）。
        for (String platformOnly : List.of("revenue.ticket", "revenue.bar", "bar.order_count",
                "ticket.order_count",
                "energy.total_electricity", "verify.done", "member.new", "risk.pending_review",
                "finance.recon_diff_count")) {
            assertThatThrownBy(() -> service.metricDetail(platformOnly, null, null, null, 1, 20, List.of("proj-A")))
                    .as("platform_only 指标 %s 项目级应拒", platformOnly)
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("平台口径");
        }
        // 平台管理员（null scope）访问平台口径指标不受影响。
        when(repository.ticketRevenueDetail(any(), any(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "t1", "amountCent", 100L)), 1, new BigDecimal("100")));
        assertThat(service.metricDetail("revenue.ticket", null, null, null, 1, 20, null).summaryValue())
                .isEqualByComparingTo("100");
    }

    @Test
    void projectScopedFinanceMetricPassesScopeAndNumberEqualsDetail() {
        when(repository.arOpenReceivableDetail(any(), anyInt(), anyInt(), any()))
                .thenReturn(new MetricDetailRows(
                        List.of(row("id", "arA", "unpaidAmountCent", 5000L)), 1, new BigDecimal("5000")));

        MetricDetailResponse response = service.metricDetail("finance.ar_balance", null, null, null, 1, 20, List.of("proj-A"));

        // 项目级：scope 透传仓储（按 project_id 过滤）；明细与合计同一仓储调用同谓词 → 数字=明细合计。
        verify(repository).arOpenReceivableDetail(eq(CURRENT_BUSINESS_DAY), eq(20), eq(0), eq(List.of("proj-A")));
        assertThat(response.summaryValue()).isEqualByComparingTo("5000");
        assertThat(response.rows()).hasSize(1);
        assertThat(response.summaryNote()).contains("您可查看的项目");
    }

    @Test
    void projectScopedStaffAdditiveMetricPassesScopeToRepository() {
        when(repository.staffShiftDetail(any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "shA")), 1, new BigDecimal("1")));

        MetricDetailResponse response = service.metricDetail("staff.shift_count", null, null, null, 1, 20, List.of("proj-A"));

        verify(repository).staffShiftDetail(eq(CURRENT_BUSINESS_DAY), eq(CURRENT_BUSINESS_DAY), eq(20), eq(0), eq(List.of("proj-A")));
        assertThat(response.summaryNote()).contains("您可查看的项目");
    }

    @Test
    void projectScopedSettlementMetricPassesScopeToRepository() {
        when(repository.settlementTransactionDetail(any(), any(), org.mockito.ArgumentMatchers.anyString(), anyInt(), anyInt(), any()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "s1")), 2, new BigDecimal("2")));

        service.metricDetail("settlement.transaction_count", null, null, null, 1, 20, List.of("proj-A"));

        verify(repository).settlementTransactionDetail(eq(null), eq(null), eq("count"), eq(20), eq(0), eq(List.of("proj-A")));
    }

    @Test
    void projectScopedHeadcountMetricPassesScopeViaOrganizationColumn() {
        when(repository.staffActiveHeadcountDetail(any(), anyInt(), anyInt(), any()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "u1")), 3, new BigDecimal("3")));

        service.metricDetail("staff.active_headcount", null, null, null, 1, 20, List.of("proj-A"));

        // access_users 用 organization_id 作综合体列；scope 透传仓储（仓储层用 organization_id 谓词）。
        verify(repository).staffActiveHeadcountDetail(eq(CURRENT_BUSINESS_DAY), eq(20), eq(0), eq(List.of("proj-A")));
    }

    @Test
    void platformAdminNullScopeSeesFullPlatformForProjectScopedMetric() {
        when(repository.staffPayrollDetail(any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), anyInt(), anyInt(), any()))
                .thenReturn(new MetricDetailRows(List.of(row("id", "p1", "totalCent", 9L)), 1, new BigDecimal("9")));

        MetricDetailResponse response = service.metricDetail(
                "staff.payroll_net_total", null, LocalDate.parse("2026-06-01"), LocalDate.parse("2026-06-01"), 1, 20, null);

        // 平台管理员=null scope → 仓储收到 null（看全量），不加项目口径说明。
        verify(repository).staffPayrollDetail(any(), any(), eq(true), eq(20), eq(0), isNull());
        assertThat(response.summaryNote()).isNull();
    }

    @Test
    void registryClassifiesEveryMetricProjectScopeFailClosed() {
        // 项目作用域指标逐项固定 scope 列；其余全部 platform_only，防止新增指标意外扩大范围。
        Map<String, String> expectedScoped = new LinkedHashMap<>();
        for (String k : List.of("finance.ar_balance", "finance.ap_balance", "finance.ar_balance_aging90p",
                "settlement.transaction_count", "settlement.verified_amount", "settlement.verified_count",
                "staff.shift_count", "staff.attendance_count", "staff.attendance_late_count",
                "staff.payroll_net_total", "staff.payroll_statement_count", "staff.pending_leave_count")) {
            expectedScoped.put(k, "project_id");
        }
        expectedScoped.put("staff.active_headcount", "organization_id");
        for (String k : List.of("amusement.session_count", "amusement.session_capacity",
                "amusement.group_booking_count", "amusement.group_reserved_capacity",
                "amusement.entry_count", "amusement.exit_count", "amusement.inspection_count",
                "amusement.inspection_pending_rectification", "traffic.amusement")) {
            expectedScoped.put(k, "complex_id");
        }

        for (MetricRegistry.MetricDefinition def : MetricRegistry.all()) {
            Optional<String> col = MetricRegistry.projectScopeColumn(def.metricKey());
            if (expectedScoped.containsKey(def.metricKey())) {
                assertThat(col).as("指标 %s 应为项目作用域", def.metricKey())
                        .contains(expectedScoped.get(def.metricKey()));
                assertThat(MetricRegistry.isProjectScoped(def)).isTrue();
            } else {
                assertThat(col).as("指标 %s 应为平台口径 platform_only", def.metricKey()).isEmpty();
                assertThat(MetricRegistry.isProjectScoped(def)).isFalse();
            }
        }
        assertThat(expectedScoped).hasSize(22);
    }

    private static Map<String, Object> row(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }
}
