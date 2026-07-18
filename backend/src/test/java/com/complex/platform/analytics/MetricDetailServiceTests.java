package com.complex.platform.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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

    // 2026-07-18T02:00Z、全局/域缺省日切 04:00 → 当前经营日 = 2026-07-17。
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-18T02:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate CURRENT_BUSINESS_DAY = LocalDate.parse("2026-07-17");

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

        // 经营日窗（ticketing 缺省 04:00）：[07-17 04:00Z, 07-18 04:00Z)。
        ArgumentCaptor<Instant> start = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> end = ArgumentCaptor.forClass(Instant.class);
        verify(repository).ticketRevenueDetail(start.capture(), end.capture(), eq(20), eq(0));
        assertThat(start.getValue()).isEqualTo(Instant.parse("2026-07-17T04:00:00Z"));
        assertThat(end.getValue()).isEqualTo(Instant.parse("2026-07-18T04:00:00Z"));

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
        when(repository.arOpenReceivableDetail(any(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(
                        List.of(row("id", "ar1", "unpaidAmountCent", 70000L)), 1, new BigDecimal("70000")));

        MetricDetailResponse response = service.metricDetail(
                "finance.ar_balance", null, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-10"), 1, 20);

        verify(repository).arOpenReceivableDetail(asOf.capture(), eq(20), eq(0));
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
        when(repository.arOpenReceivableDetail(any(), anyInt(), anyInt()))
                .thenReturn(new MetricDetailRows(List.of(), 0, BigDecimal.ZERO));

        MetricDetailResponse response = service.metricDetail("finance.ar_balance", null, null, null, 1, 20);

        verify(repository).arOpenReceivableDetail(eq(CURRENT_BUSINESS_DAY), eq(20), eq(0));
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

    @Test
    void registryFirstBatchCoversThreeValueTypesWithPermissionsAndCalibers() {
        assertThat(MetricRegistry.all()).extracting(MetricRegistry.MetricDefinition::metricKey)
                .containsExactly("revenue.ticket", "finance.recon_diff_count", "energy.total_electricity",
                        "energy.electricity_per_sqm", "finance.ar_balance");
        assertThat(MetricRegistry.all()).extracting(MetricRegistry.MetricDefinition::valueType)
                .contains(MetricRegistry.TYPE_ADDITIVE, MetricRegistry.TYPE_RATIO, MetricRegistry.TYPE_BALANCE);
        assertThat(MetricRegistry.require("finance.recon_diff_count").permission()).isEqualTo("finance.reconciliation.view");
        assertThat(MetricRegistry.require("energy.total_electricity").permission()).isEqualTo("energy.reading.manage");
        assertThat(MetricRegistry.require("finance.ar_balance").permission()).isEqualTo("finance.ar_ap.view");
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

    private static Map<String, Object> row(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
