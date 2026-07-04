package com.complex.platform.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.complex.platform.analytics.AnalyticsRecords.AiQualityResultRecord;
import com.complex.platform.analytics.AnalyticsRecords.AiQualityTaskRecord;
import com.complex.platform.analytics.AnalyticsRecords.BusinessMetricSnapshotRecord;
import com.complex.platform.analytics.AnalyticsRecords.CustomerIntentRecord;
import com.complex.platform.analytics.AnalyticsRecords.DashboardSnapshotRecord;
import com.complex.platform.analytics.AnalyticsRecords.MerchantHealthScoreRecord;
import com.complex.platform.analytics.AnalyticsRecords.MerchantSummaryRecord;
import com.complex.platform.analytics.AnalyticsRecords.MetricAnomalyRecord;
import com.complex.platform.analytics.AnalyticsRecords.MetricDefinitionRecord;
import com.complex.platform.analytics.AnalyticsRecords.RiskAlertRecord;
import com.complex.platform.analytics.AnalyticsRecords.RiskRuleRecord;
import com.complex.platform.analytics.AnalyticsRecords.RobotMetricRecord;
import com.complex.platform.analytics.AnalyticsRecords.StaffServiceScoreRecord;
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

class AnalyticsServiceTests {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-14T00:00:00Z"), ZoneOffset.UTC);

    private InMemoryAnalyticsRepository analyticsRepository;
    private InMemoryIdempotencyRepository idempotencyRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private CapturingLocalLlmClient localLlmClient;
    private AnalyticsService service;

    @BeforeEach
    void setUp() {
        analyticsRepository = new InMemoryAnalyticsRepository();
        idempotencyRepository = new InMemoryIdempotencyRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        localLlmClient = new CapturingLocalLlmClient();
        seedReadModels();
        AuditedOperationExecutor operations = new AuditedOperationExecutor(
                new IdempotencyService(idempotencyRepository, new ObjectMapper()),
                new AuditEventService(auditEventRepository)
        );
        service = new AnalyticsService(
                analyticsRepository,
                operations,
                FIXED_CLOCK,
                localLlmClient
        );
    }

    @Test
    void readEndpointsReturnRepositoryDataAndRecordExactOpenApiAuditEvents() {
        assertThat(service.listAiQualityResults().items()).extracting(AiQualityResultRecord::resultId).contains("result_1");
        assertThat(service.listAiQualityTasks().items()).extracting(AiQualityTaskRecord::taskId).contains("task_1");
        assertThat(service.listCustomerIntents().items()).extracting(CustomerIntentRecord::intentId).contains("intent_1");
        assertThat(service.getP0OverviewDashboard().snapshotKey()).isEqualTo("p0_overview_today");
        assertThat(service.listBusinessMetricSnapshots().items()).extracting(BusinessMetricSnapshotRecord::metricKey).contains("gmv_amount_cents");
        assertThat(service.listMerchantHealthScores(java.util.Optional.empty()).items()).extracting(MerchantHealthScoreRecord::merchantId).contains("merchant_1");
        assertThat(service.listAnalyticsMerchants(java.util.Optional.empty()).items()).extracting(MerchantSummaryRecord::merchantId).contains("merchant_1");
        assertThat(service.listMetricAnomalies().items()).extracting(MetricAnomalyRecord::id).contains("anomaly_1");
        assertThat(service.listMetricDefinitions().items()).extracting(MetricDefinitionRecord::metricKey).contains("gmv_amount_cents");
        assertThat(service.listRiskAlerts().items()).extracting(RiskAlertRecord::id).contains("alert_1");
        assertThat(service.listRiskRules().items()).extracting(RiskRuleRecord::ruleId).contains("rule_1");
        assertThat(service.getRobotAnalytics().items()).extracting(RobotMetricRecord::metricId).contains("robot_metric_1");
        assertThat(service.listStaffServiceScores().items()).extracting(StaffServiceScoreRecord::scoreId).contains("score_1");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "analytics.ai_quality_result.viewed",
                        "analytics.ai_quality_task.viewed",
                        "analytics.customer_intent.viewed",
                        "analytics.dashboard_viewed",
                        "analytics.business_metric_snapshot.viewed",
                        "analytics.merchant_health.viewed",
                        "analytics.merchant_summary.viewed",
                        "analytics.metric_anomaly.viewed",
                        "analytics.metric_definition.viewed",
                        "risk_alert.viewed",
                        "analytics.risk_rule.viewed",
                        "analytics.robot.viewed",
                        "analytics.staff_service_score.viewed"
                );
    }

    @Test
    void businessDayCutoffUpsertValidatesDomainAndHourRange() {
        // S0:域白名单+0-23 校验;合法写入落审计。
        OperatingParamsService ops = new OperatingParamsService(
                analyticsRepository, new AuditEventService(auditEventRepository));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ops.upsertBusinessDayCutoff(
                new BusinessDayCutoffRequest("k1", null, "nonsense", 6)))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ops.upsertBusinessDayCutoff(
                new BusinessDayCutoffRequest("k2", null, "bar", 24)))
                .isInstanceOfSatisfying(com.complex.platform.common.api.ApiException.class,
                        e -> org.assertj.core.api.Assertions.assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        org.assertj.core.api.Assertions.assertThat(ops.upsertBusinessDayCutoff(
                new BusinessDayCutoffRequest("k3", null, "bar", 6)).cutoffHour()).isEqualTo(6);
    }

    @Test
    void overviewReturnsMetricCodeKeyedMapForPlatformScope() {
        // 平台口径快照（ETL 落地）。
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.ticket", "platform", null), new BigDecimal("128800"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.bar", "platform", null), new BigDecimal("68800"), "etl_aggregate", Instant.now(FIXED_CLOCK));

        var overview = service.overview(Optional.empty());

        assertThat(overview.scope()).isNull();
        // DTO 契约：键为 metric_code。
        assertThat(overview.metrics()).containsKeys("revenue.ticket", "revenue.bar", "verify.done", "member.new", "risk.pending_review");
        assertThat(overview.metrics().get("revenue.ticket").value()).isEqualByComparingTo("128800");
        assertThat(overview.metrics().get("revenue.bar").value()).isEqualByComparingTo("68800");
    }

    @Test
    void overviewHonorsRequestedBusinessDateInsteadOfLatestEtlDay() {
        // T4.1：修总览日期「假筛选」——overview 接受指定经营日 as-of 返回该日；此前永远取最新 ETL 日、忽略前端日期。
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.ticket", "platform", null),
                new BigDecimal("128800"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        java.time.LocalDate requested = java.time.LocalDate.parse("2026-05-01");

        var overview = service.overview(Optional.empty(), requested);
        assertThat(overview.businessDate()).isEqualTo(requested);

        // 不传日期 → 退回最新 ETL 日（默认行为不变）。
        assertThat(service.overview(Optional.empty(), null).businessDate()).isNotNull();
    }

    @Test
    void trafficHourlyReturns24ZeroFilledBucketsForRequestedDate() {
        // T4.3：分时段客流恒返回 0~23 共 24 桶，命中小时填真值、其余补 0；落审计事件。
        analyticsRepository.hourlyTraffic.put(9, 3L);
        analyticsRepository.hourlyTraffic.put(14, 5L);

        var resp = service.trafficHourly(Optional.empty(), java.time.LocalDate.parse("2026-06-14"));

        assertThat(resp.businessDate()).isEqualTo(java.time.LocalDate.parse("2026-06-14"));
        assertThat(resp.buckets()).hasSize(24);
        assertThat(resp.buckets()).extracting(AnalyticsRecords.HourlyTrafficBucket::hour)
                .containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23);
        assertThat(resp.buckets().get(9).count()).isEqualTo(3L);
        assertThat(resp.buckets().get(14).count()).isEqualTo(5L);
        assertThat(resp.buckets().get(0).count()).isZero();
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName)
                .contains("analytics.traffic_hourly.viewed");
    }

    @Test
    void operatingParamsUpsertPersistsAreaRejectsNonPositiveAndRecordsAudit() {
        // T4.2 坪效面积配置：upsert 落库 + 审计；面积必须为正。
        OperatingParamsService paramsService = new OperatingParamsService(
                analyticsRepository, new AuditEventService(auditEventRepository), FIXED_CLOCK);

        var saved = paramsService.upsertOperatingParams(
                new OperatingParamsRequest("project_1", new BigDecimal("1200.00"), null, "op-key-1"));
        assertThat(saved.operatingAreaSqm()).isEqualByComparingTo("1200.00");
        assertThat(saved.status()).isEqualTo("active");
        assertThat(paramsService.listOperatingParams().items())
                .extracting(OperatingParamsRecord::projectId).contains("project_1");
        assertThat(analyticsRepository.findOperatingParams("project_1")).isPresent();
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).contains("operating_params.changed");

        // 非正数面积被拒（不落库分母为 0/负）。
        assertThatThrownBy(() -> paramsService.upsertOperatingParams(
                new OperatingParamsRequest("project_1", BigDecimal.ZERO, null, "op-key-2")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void overviewEnforcesMerchantScopeIsolation() {
        // 商户 A 与 B 的酒吧收入分别落快照（行级隔离按 segment_id）。
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.bar", "merchant", "merchant_a"), new BigDecimal("5000"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.bar", "merchant", "merchant_b"), new BigDecimal("9000"), "etl_aggregate", Instant.now(FIXED_CLOCK));

        var merchantA = service.overview(Optional.of("merchant_a"));

        assertThat(merchantA.scope()).isEqualTo("merchant_a");
        assertThat(merchantA.metrics().get("revenue.bar").value()).isEqualByComparingTo("5000");
        // 跨商户不可见：商户 A 不应看到商户 B 的 9000。
        assertThat(merchantA.metrics().get("revenue.bar").value()).isNotEqualByComparingTo("9000");
    }

    @Test
    void overviewReturnsNullWithoutFabricationForUnreadyAndScopelessMetrics() {
        // 仅落地平台口径 revenue.bar；商户口径无该指标行。
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.bar", "merchant", "merchant_a"), new BigDecimal("5000"), "etl_aggregate", Instant.now(FIXED_CLOCK));

        var merchantA = service.overview(Optional.of("merchant_a"));

        // 数据源未就绪的指标不伪造：value=null + note。
        assertThat(merchantA.metrics().get("supply.received").value()).isNull();
        assertThat(merchantA.metrics().get("supply.received").note()).contains("T6");
        assertThat(merchantA.metrics().get("risk.manual_backfill").value()).isNull();
        assertThat(merchantA.metrics().get("revenue.thirdparty").value()).isNull();
        // 源表无 merchant_id 的指标在商户口径下无快照行 → null（不伪造），带 scope note。
        assertThat(merchantA.metrics().get("revenue.ticket").value()).isNull();
        assertThat(merchantA.metrics().get("revenue.ticket").note()).contains("merchant_id");
    }

    @Test
    void groupOverviewRollsUpPlatformEtlSnapshotsByProjectWithRealMerchantCount() {
        // 平台口径净收入/客流/核销/风险 + 两个商户口径快照（用于商户数计数）。
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.net", "platform", null), new BigDecimal("120000"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        analyticsRepository.upsertMetricSnapshot(metricKey("traffic.amusement", "platform", null), new BigDecimal("42"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        analyticsRepository.upsertMetricSnapshot(metricKey("verify.done", "platform", null), new BigDecimal("30"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        analyticsRepository.upsertMetricSnapshot(metricKey("risk.pending_review", "platform", null), new BigDecimal("3"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.bar", "merchant", "merchant_a"), new BigDecimal("5000"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.bar", "merchant", "merchant_b"), new BigDecimal("9000"), "etl_aggregate", Instant.now(FIXED_CLOCK));

        var group = service.groupOverview();

        // 单综合体 → 单 project rollup（不伪造多 project）。
        assertThat(group.projects()).hasSize(1);
        var project = group.projects().get(0);
        assertThat(project.projectId()).isEqualTo("project_1");
        // 商户数 = 当日有 ETL 商户口径快照的去重商户数（真实计数）。
        assertThat(project.merchantCount()).isEqualTo(2L);
        // 关键指标按平台口径真实 rollup。
        assertThat(project.metrics().get("revenue.net").value()).isEqualByComparingTo("120000");
        assertThat(project.metrics().get("traffic.amusement").value()).isEqualByComparingTo("42");
        assertThat(project.metrics().get("verify.done").value()).isEqualByComparingTo("30");
        assertThat(project.metrics().get("risk.pending_review").value()).isEqualByComparingTo("3");
    }

    @Test
    void groupOverviewReturnsHonestEmptyStateWhenNoEtlSnapshotExists() {
        // 无任何 ETL 快照 → metrics 全 null + note，商户数 0，不伪造。
        var group = service.groupOverview();

        assertThat(group.projects()).hasSize(1);
        var project = group.projects().get(0);
        assertThat(project.merchantCount()).isZero();
        assertThat(project.metrics().get("revenue.net").value()).isNull();
        assertThat(project.metrics().get("revenue.net").note()).isNotBlank();
    }

    @Test
    void groupOverviewReadsProjectSegmentRollupWhenEtlEmitsProjectDimension() {
        // ⑤ 集团多项目 ETL：有 segment_type='project' 快照时，group-overview 按 project_id 产出 per-project rollup。
        // 单综合体 → 单 project_1 行，值取 project 口径快照（与平台口径一致，口径不漂移、不伪造）。
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.net", "project", "project_1"), new BigDecimal("120000"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        analyticsRepository.upsertMetricSnapshot(metricKey("traffic.amusement", "project", "project_1"), new BigDecimal("42"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        // 商户口径快照用于真实商户数计数（不进 project 桶）。
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.bar", "merchant", "merchant_a"), new BigDecimal("5000"), "etl_aggregate", Instant.now(FIXED_CLOCK));

        var group = service.groupOverview();

        assertThat(group.projects()).hasSize(1);
        var project = group.projects().get(0);
        assertThat(project.projectId()).isEqualTo("project_1");
        assertThat(project.metrics().get("revenue.net").value()).isEqualByComparingTo("120000");
        assertThat(project.metrics().get("traffic.amusement").value()).isEqualByComparingTo("42");
    }

    @Test
    void groupOverviewReturnsMultipleRowsWhenEtlEmitsMultipleProjects() {
        // ⑤ 多综合体：多个 project_id 的 project 口径快照 → group-overview 返回多行真实 rollup（不伪造、按 project 分组）。
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.net", "project", "project_1"), new BigDecimal("120000"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.net", "project", "project_2"), new BigDecimal("88000"), "etl_aggregate", Instant.now(FIXED_CLOCK));

        var group = service.groupOverview();

        assertThat(group.projects()).hasSize(2);
        assertThat(group.projects().stream().map(p -> p.projectId()))
                .containsExactlyInAnyOrder("project_1", "project_2");
        var p1 = group.projects().stream().filter(p -> p.projectId().equals("project_1")).findFirst().orElseThrow();
        var p2 = group.projects().stream().filter(p -> p.projectId().equals("project_2")).findFirst().orElseThrow();
        assertThat(p1.metrics().get("revenue.net").value()).isEqualByComparingTo("120000");
        assertThat(p2.metrics().get("revenue.net").value()).isEqualByComparingTo("88000");
    }

    @Test
    void groupOverviewProjectGroupByFlagsBelowBenchmarkMetrics() {
        // 均值 75_000；阈值默认 20%，低于 60_000 标 below_benchmark。
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.net", "project", "project_1"), new BigDecimal("120000"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.net", "project", "project_2"), new BigDecimal("30000"), "etl_aggregate", Instant.now(FIXED_CLOCK));

        var group = service.groupOverview("project");

        assertThat(group.groupBy()).isEqualTo("project");
        assertThat(group.groupAverages().get("revenue.net").value()).isEqualByComparingTo("75000");
        var p1 = group.projects().stream().filter(p -> p.projectId().equals("project_1")).findFirst().orElseThrow();
        var p2 = group.projects().stream().filter(p -> p.projectId().equals("project_2")).findFirst().orElseThrow();
        assertThat(p1.belowBenchmarkMetrics()).doesNotContain("revenue.net");
        assertThat(p2.belowBenchmarkMetrics()).containsExactly("revenue.net");
        assertThat(p2.metrics().get("revenue.net").dimension()).containsEntry("below_benchmark", true);
    }

    @Test
    void groupOverviewEntityGroupByAggregatesProjectMetricsByBusinessEntity() {
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.net", "project", "project_1"), new BigDecimal("120000"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.net", "project", "project_2"), new BigDecimal("80000"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.net", "project", "project_3"), new BigDecimal("70000"), "etl_aggregate", Instant.now(FIXED_CLOCK));
        analyticsRepository.projectEntityMap.put("project_1", "entity_a");
        analyticsRepository.projectEntityMap.put("project_2", "entity_a");
        analyticsRepository.projectEntityMap.put("project_3", "entity_b");

        var group = service.groupOverview("entity");

        assertThat(group.groupBy()).isEqualTo("entity");
        assertThat(group.projects()).extracting(AnalyticsRecords.GroupProjectRollup::projectId)
                .containsExactly("entity_a", "entity_b");
        var entityA = group.projects().stream().filter(p -> p.projectId().equals("entity_a")).findFirst().orElseThrow();
        assertThat(entityA.metrics().get("revenue.net").value()).isEqualByComparingTo("200000");
    }

    @Test
    void dashboardDomainFiltersByDomainAndIsScopeIsolated() {
        analyticsRepository.upsertMetricSnapshot(metricKey("revenue.bar", "merchant", "merchant_a"), new BigDecimal("5000"), "etl_aggregate", Instant.now(FIXED_CLOCK));

        var barDomain = service.dashboardDomain("bar", Optional.of("merchant_a"));

        // bar 域含现场消费实收（revenue.bar）与结账后退款冲减（revenue.bar_refund，待确认#2）。
        assertThat(barDomain.metrics()).containsOnlyKeys("revenue.bar", "revenue.bar_refund");
        assertThat(barDomain.metrics().get("revenue.bar").value()).isEqualByComparingTo("5000");
    }

    @Test
    void repositoryFailureRecordsExactFailureAuditEventAndRethrows() {
        analyticsRepository.failAiQualityResults = true;

        assertThatThrownBy(service::listAiQualityResults)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("analytics store unavailable");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName, AuditEvent::eventResult)
                .containsExactly(tuple("analytics.ai_quality_result.viewed_failed", "failure"));
    }

    @Test
    void createRiskAlertRejectsInvalidRiskLevel() {
        assertThatThrownBy(() -> service.createRiskAlert(riskAlertRequest("bad_risk", "metric_anomaly", "unknown", "merchant", "summary")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        assertThat(analyticsRepository.riskAlertSaveCount).isZero();
        assertThat(auditEventRepository.events).extracting(AuditEvent::eventName).containsExactly("risk_alert.create_failed");
    }

    @Test
    void createRiskAlertRejectsMissingRequiredFields() {
        assertThatThrownBy(() -> service.createRiskAlert(riskAlertRequest("missing_type", " ", "high", "merchant", "summary")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
        assertThatThrownBy(() -> service.createRiskAlert(riskAlertRequest("missing_object", "metric_anomaly", "high", " ", "summary")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
        assertThatThrownBy(() -> service.createRiskAlert(riskAlertRequest("missing_summary", "metric_anomaly", "high", "merchant", " ")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        assertThat(analyticsRepository.riskAlertSaveCount).isZero();
    }

    @Test
    void createRiskAlertUsesAnalyticsCreateRiskAlertIdempotencyOperation() {
        RiskAlertRequest request = riskAlertRequest("create-once", "metric_anomaly", "high", "merchant", "Revenue dropped");

        RiskAlertRecord first = service.createRiskAlert(request);
        RiskAlertRecord second = service.createRiskAlert(request);

        assertThat(second).isEqualTo(first);
        assertThat(first)
                .extracting(RiskAlertRecord::riskLevel, RiskAlertRecord::status, RiskAlertRecord::idempotencyKey)
                .containsExactly("high", "open", "create-once");
        assertThat(analyticsRepository.riskAlertSaveCount).isEqualTo(1);
        assertThat(idempotencyRepository.records).containsKey("analytics.create-risk-alert:create-once");
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "risk_alert.created".equals(event.eventName()))
                .hasSize(1);
    }

    @Test
    void createRiskAlertDoubleWritesToUnifiedRiskAlertServiceWithAnalyticsDomainAndMappedLevel() {
        com.complex.platform.evidence.RiskAlertService riskAlertService =
                mock(com.complex.platform.evidence.RiskAlertService.class);
        AnalyticsService unifiedService = new AnalyticsService(
                analyticsRepository, operations(), FIXED_CLOCK, localLlmClient, riskAlertService);

        RiskAlertRecord saved = unifiedService.createRiskAlert(
                riskAlertRequest("unified-dual-write", "metric_anomaly", "high", "merchant", "Revenue dropped"));

        // 孤岛表仍权威：照常落 analytics_risk_alerts。
        assertThat(analyticsRepository.riskAlertSaveCount).isEqualTo(1);

        org.mockito.ArgumentCaptor<com.complex.platform.evidence.dto.RiskAlertReportRequest> captor =
                org.mockito.ArgumentCaptor.forClass(com.complex.platform.evidence.dto.RiskAlertReportRequest.class);
        verify(riskAlertService).report(captor.capture());
        com.complex.platform.evidence.dto.RiskAlertReportRequest reported = captor.getValue();
        assertThat(reported.sourceDomain()).isEqualTo("analytics");
        assertThat(reported.alertType()).isEqualTo("metric_anomaly");
        assertThat(reported.riskLevel()).isEqualTo("high");
        assertThat(reported.sourceRef()).isEqualTo(saved.id());
        assertThat(reported.idempotencyKey()).isEqualTo(saved.id() + ":unified");
    }

    @Test
    void createRiskAlertMapsMediumSeverityToSuspiciousForUnifiedReport() {
        // 本域校验只接受统一枚举，但若孤岛记录风险级为别名（如 medium），双写须映射为 suspicious（不丢数据）。
        com.complex.platform.evidence.RiskAlertService riskAlertService =
                mock(com.complex.platform.evidence.RiskAlertService.class);
        // 用一个把保存记录强制改写为 medium 风险级的仓储装饰，验证映射分支。
        analyticsRepository.overrideSavedRiskLevel = "medium";
        AnalyticsService unifiedService = new AnalyticsService(
                analyticsRepository, operations(), FIXED_CLOCK, localLlmClient, riskAlertService);

        unifiedService.createRiskAlert(
                riskAlertRequest("unified-medium", "metric_anomaly", "high", "merchant", "Revenue dropped"));

        org.mockito.ArgumentCaptor<com.complex.platform.evidence.dto.RiskAlertReportRequest> captor =
                org.mockito.ArgumentCaptor.forClass(com.complex.platform.evidence.dto.RiskAlertReportRequest.class);
        verify(riskAlertService).report(captor.capture());
        assertThat(captor.getValue().riskLevel()).isEqualTo("suspicious");
    }

    @Test
    void createRiskAlertFailSafeWhenRiskAlertServiceNull() {
        // fail-safe：未注入统一服务（旧构造器）时，主流程不抛、孤岛表照常写。
        AnalyticsService nullSafeService = new AnalyticsService(
                analyticsRepository, operations(), FIXED_CLOCK, localLlmClient);

        assertThatCode(() -> nullSafeService.createRiskAlert(
                riskAlertRequest("unified-null", "metric_anomaly", "high", "merchant", "Revenue dropped")))
                .doesNotThrowAnyException();
        assertThat(analyticsRepository.riskAlertSaveCount).isEqualTo(1);
    }

    @Test
    void createRiskAlertFailSafeWhenUnifiedReportThrows() {
        // fail-safe：统一表上报抛异常不影响创建主链，孤岛表仍权威落地。
        com.complex.platform.evidence.RiskAlertService riskAlertService =
                mock(com.complex.platform.evidence.RiskAlertService.class);
        doThrow(new RuntimeException("unified down")).when(riskAlertService).report(any());
        AnalyticsService unifiedService = new AnalyticsService(
                analyticsRepository, operations(), FIXED_CLOCK, localLlmClient, riskAlertService);

        RiskAlertRecord saved = unifiedService.createRiskAlert(
                riskAlertRequest("unified-throws", "metric_anomaly", "high", "merchant", "Revenue dropped"));

        assertThat(saved).isNotNull();
        assertThat(analyticsRepository.riskAlertSaveCount).isEqualTo(1);
        verify(riskAlertService).report(any());
    }

    @Test
    void resolveRiskAlertRejectsMissingTargetStatusOrReason() {
        assertThatThrownBy(() -> service.resolveRiskAlert("alert_1", new StatusChangeRequest("resolve-missing-action", " ", "handled", List.of())))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
        assertThatThrownBy(() -> service.resolveRiskAlert("alert_1", new StatusChangeRequest("resolve-missing-note", "resolved", " ", List.of())))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void resolveRiskAlertRejectsInvalidActionWithApiExceptionCodeValidationFailed() {
        assertThatThrownBy(() -> service.resolveRiskAlert(
                        "alert_1",
                        new StatusChangeRequest("resolve-invalid-action", "send_wechat", "handled", List.of())
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        assertThat(analyticsRepository.riskAlertUpdateCount).isZero();
    }

    @Test
    void resolveRiskAlertRejectsUnknownAlertId() {
        assertThatThrownBy(() -> service.resolveRiskAlert("missing_alert", resolveRequest("resolve-missing")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("RISK_ALERT_NOT_FOUND");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("analytics.risk_alert.resolved_failed");
    }

    @Test
    void resolveRiskAlertRejectsAlreadyResolvedAlert() {
        RiskAlertRecord resolved = resolvedAlert("alert_resolved");
        analyticsRepository.riskAlerts.put(resolved.id(), resolved);

        assertThatThrownBy(() -> service.resolveRiskAlert(resolved.id(), resolveRequest("resolve-twice")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("RISK_ALERT_ALREADY_RESOLVED");
    }

    @Test
    void resolveRiskAlertUsesAnalyticsResolveRiskAlertIdempotencyOperation() {
        StatusChangeRequest request = resolveRequest("resolve-once");

        RiskAlertRecord first = service.resolveRiskAlert("alert_1", request);
        RiskAlertRecord second = service.resolveRiskAlert("alert_1", request);

        assertThat(second).isEqualTo(first);
        assertThat(first.status()).isEqualTo("resolved");
        assertThat(first.operatorNote()).isEqualTo("handled by operator");
        assertThat(first.resolvedAt()).isEqualTo(Instant.now(FIXED_CLOCK));
        assertThat(analyticsRepository.riskAlertUpdateCount).isEqualTo(1);
        assertThat(idempotencyRepository.records).containsKey("analytics.resolve-risk-alert:resolve-once");
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "analytics.risk_alert.resolved".equals(event.eventName()))
                .hasSize(1);
    }


    @Test
    void generateLocalLlmAiQualitySuggestionStoresResultAndRedactsSensitiveInput() {
        localLlmClient.nextSuggestion = LocalLlmSuggestion.completed(
                "local_llm",
                "gemma-local",
                88,
                "suspicious",
                new BigDecimal("0.72"),
                "service tone risk found",
                2
        );

        AiQualitySuggestionResponse response = service.generateLocalLlmAiQualitySuggestion(new AiQualitySuggestionRequest(
                "llm-once",
                "ticket_call",
                "call_160",
                "phone 13812345678 id 110101199003071234 email guest@example.com card 6222021234567890123 complaint text",
                "staff_1"
        ));

        assertThat(response.modelStatus()).isEqualTo("completed");
        assertThat(response.redactionApplied()).isTrue();
        assertThat(response.manualReviewRequired()).isFalse();
        assertThat(localLlmClient.lastSourceText)
                .doesNotContain("13812345678")
                .doesNotContain("110101199003071234")
                .doesNotContain("guest@example.com")
                .doesNotContain("6222021234567890123")
                .contains("[PHONE_REDACTED]")
                .contains("[ID_REDACTED]")
                .contains("[EMAIL_REDACTED]")
                .contains("[CARD_REDACTED]");
        assertThat(response.task().processingMode()).isEqualTo("local_llm_suggestion");
        assertThat(response.task().modelProvider()).isEqualTo("local_llm");
        assertThat(response.result().qualityScore()).isEqualTo(88);
        assertThat(response.result().riskLevel()).isEqualTo("suspicious");
        assertThat(analyticsRepository.aiQualityTaskSaveCount).isEqualTo(1);
        assertThat(analyticsRepository.aiQualityResultSaveCount).isEqualTo(1);
    }

    @Test
    void localLlmSuggestionRedactsInternationalPhonesIbanSecretsAndLongTokens() {
        localLlmClient.nextSuggestion = LocalLlmSuggestion.completed(
                "local_llm", "gemma-local", 80, "normal", new BigDecimal("0.10"), "ok", 0);

        AiQualitySuggestionResponse response = service.generateLocalLlmAiQualitySuggestion(new AiQualitySuggestionRequest(
                "llm-intl-redact",
                "ticket_call",
                "call_intl",
                "intl +14155552671 iban GB82WEST12345698765432 aws AKIAIOSFODNN7EXAMPLE "
                        + "key sk-live_abcdefghijklmnop token deadbeefdeadbeefdeadbeefdeadbeef12345678 ssn 123-45-6789",
                "staff_1"
        ));

        assertThat(response.redactionApplied()).isTrue();
        assertThat(localLlmClient.lastSourceText)
                .doesNotContain("+14155552671")
                .doesNotContain("GB82WEST12345698765432")
                .doesNotContain("AKIAIOSFODNN7EXAMPLE")
                .doesNotContain("sk-live_abcdefghijklmnop")
                .doesNotContain("deadbeefdeadbeefdeadbeefdeadbeef12345678")
                .doesNotContain("123-45-6789")
                .contains("[PHONE_REDACTED]")
                .contains("[SECRET_REDACTED]")
                .contains("[ID_REDACTED]");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("analytics.ai_quality.local_llm_suggestion.created");
    }

    @Test
    void generateLocalLlmAiQualitySuggestionFallsBackToManualReviewWhenModelUnavailable() {
        localLlmClient.nextSuggestion = LocalLlmSuggestion.unavailable("disabled", "local-model");

        AiQualitySuggestionResponse response = service.generateLocalLlmAiQualitySuggestion(new AiQualitySuggestionRequest(
                "llm-unavailable",
                "wecom_message",
                "msg_160",
                "guest asks for refund after failed service",
                "staff_2"
        ));

        assertThat(response.modelStatus()).isEqualTo("unavailable");
        assertThat(response.manualReviewRequired()).isTrue();
        assertThat(response.result().confidenceScore()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(response.result().summary()).contains("manual review");
        assertThat(response.task().status()).isEqualTo("failed");
        assertThat(analyticsRepository.aiQualityTaskSaveCount).isEqualTo(1);
        assertThat(analyticsRepository.aiQualityResultSaveCount).isEqualTo(1);
    }

    @Test
    void generateLocalLlmAiQualitySuggestionNormalizesUntrustedModelOutputAndRequiresManualReview() {
        localLlmClient.nextSuggestion = LocalLlmSuggestion.completed(
                "local_llm",
                "gemma-local",
                128,
                "model_invented_risk",
                new BigDecimal("1.70"),
                "x".repeat(600),
                -7
        );

        AiQualitySuggestionResponse response = service.generateLocalLlmAiQualitySuggestion(new AiQualitySuggestionRequest(
                "llm-dirty-output",
                "ticket_call",
                "call_dirty",
                "guest asks for manual escalation",
                "staff_3"
        ));

        assertThat(response.modelStatus()).isEqualTo("completed");
        assertThat(response.manualReviewRequired()).isTrue();
        assertThat(response.result().qualityScore()).isEqualTo(100);
        assertThat(response.result().riskLevel()).isEqualTo("suspicious");
        assertThat(response.result().confidenceScore()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(response.result().summary()).hasSize(500);
        assertThat(response.result().findingCount()).isZero();
        assertThat(analyticsRepository.aiQualityTaskSaveCount).isEqualTo(1);
        assertThat(analyticsRepository.aiQualityResultSaveCount).isEqualTo(1);
    }

    private AuditedOperationExecutor operations() {
        return new AuditedOperationExecutor(
                new IdempotencyService(idempotencyRepository, new ObjectMapper()),
                new AuditEventService(auditEventRepository)
        );
    }

    private com.complex.platform.analytics.AnalyticsRecords.MetricSnapshotKey metricKey(
            String metricCode,
            String segmentType,
            String segmentId
    ) {
        return new com.complex.platform.analytics.AnalyticsRecords.MetricSnapshotKey(
                metricCode, "test", "daily", LocalDate.parse("2026-06-14"), segmentType, segmentId);
    }

    private RiskAlertRequest riskAlertRequest(
            String idempotencyKey,
            String alertType,
            String riskLevel,
            String objectType,
            String summary
    ) {
        return new RiskAlertRequest(
                idempotencyKey,
                "created by control desk",
                List.of("att_1"),
                "rule_1",
                alertType,
                riskLevel,
                objectType,
                "merchant_1",
                summary
        );
    }

    private StatusChangeRequest resolveRequest(String idempotencyKey) {
        return new StatusChangeRequest(idempotencyKey, "resolved", "handled by operator", List.of("att_2"));
    }

    private RiskAlertRecord resolvedAlert(String id) {
        Instant now = Instant.now(FIXED_CLOCK);
        return new RiskAlertRecord(
                id,
                "rule_1",
                "metric_anomaly",
                "high",
                "merchant",
                "merchant_1",
                "Revenue dropped below threshold",
                "resolved",
                "already handled",
                List.of("att_1"),
                "seed-resolved",
                now.minusSeconds(3600),
                now,
                now
        );
    }

    private void seedReadModels() {
        Instant now = Instant.now(FIXED_CLOCK);
        analyticsRepository.metricDefinitions.add(new MetricDefinitionRecord(
                "gmv_amount_cents",
                "GMV",
                "finance",
                "cent",
                "sum paid order amount",
                "finance_manager",
                "active",
                now,
                now
        ));
        analyticsRepository.dashboardSnapshot = new DashboardSnapshotRecord(
                "p0_overview_today",
                "project_1",
                LocalDate.parse("2026-06-14"),
                128800L,
                23,
                2,
                1,
                Map.of("top_domain", "bar"),
                now
        );
        analyticsRepository.businessMetricSnapshots.add(new BusinessMetricSnapshotRecord(
                "metric_snapshot_1",
                "gmv_amount_cents",
                "finance",
                "daily",
                LocalDate.parse("2026-06-14"),
                "project",
                "project_1",
                new BigDecimal("128800"),
                new BigDecimal("118800"),
                new BigDecimal("8.42"),
                "up",
                "manual_baseline",
                new BigDecimal("135000"),
                new BigDecimal("0.70"),
                Map.of("source", "p2_seed"),
                now
        ));
        analyticsRepository.merchantSummaries.add(new MerchantSummaryRecord(
                "merchant_1",
                "Blue Bar",
                "bar",
                128800L,
                23,
                1,
                "settled",
                LocalDate.parse("2026-06-14"),
                now,
                now
        ));
        analyticsRepository.merchantHealthScores.add(new MerchantHealthScoreRecord(
                "merchant_1",
                86,
                "healthy",
                1,
                0L,
                now,
                "stable",
                now,
                now
        ));
        analyticsRepository.metricAnomalies.add(new MetricAnomalyRecord(
                "anomaly_1",
                "gmv_amount_cents",
                "finance",
                "high",
                new BigDecimal("128800"),
                new BigDecimal("188800"),
                "GMV below expected range",
                "open",
                now,
                null
        ));
        analyticsRepository.riskRules.add(new RiskRuleRecord(
                "rule_1",
                "GMV_DROP",
                "GMV drop",
                "finance",
                "high",
                true,
                Map.of("threshold_percent", 30),
                "notify manager",
                now,
                now
        ));
        RiskAlertRecord riskAlert = new RiskAlertRecord(
                "alert_1",
                "rule_1",
                "metric_anomaly",
                "high",
                "merchant",
                "merchant_1",
                "Revenue dropped below threshold",
                "open",
                "created by control desk",
                List.of("att_1"),
                "seed-alert",
                now,
                null,
                now
        );
        analyticsRepository.riskAlerts.put(riskAlert.id(), riskAlert);
        analyticsRepository.aiQualityTasks.add(new AiQualityTaskRecord(
                "task_1",
                "ticket_call",
                "call_1",
                "manual_review",
                "none",
                "none",
                "completed",
                "staff_1",
                now,
                now
        ));
        analyticsRepository.aiQualityResults.add(new AiQualityResultRecord(
                "result_1",
                "task_1",
                "ticket_call",
                "call_1",
                92,
                "normal",
                new BigDecimal("0.91"),
                "clear service interaction",
                1,
                now
        ));
        analyticsRepository.customerIntents.add(new CustomerIntentRecord(
                "intent_1",
                "ticket_call",
                "call_1",
                "customer_1",
                "refund_request",
                new BigDecimal("0.76"),
                "suspicious",
                "asks for refund",
                now
        ));
        analyticsRepository.robotMetrics.add(new RobotMetricRecord(
                "robot_metric_1",
                "robot_1",
                "mock_provider",
                10,
                9,
                1,
                LocalDate.parse("2026-06-14"),
                "one failed delivery task",
                now
        ));
        analyticsRepository.staffServiceScores.add(new StaffServiceScoreRecord(
                "score_1",
                "staff_1",
                "Alex",
                "frontdesk",
                95,
                0,
                12,
                "normal",
                LocalDate.parse("2026-06-14"),
                now
        ));
    }

    @Test
    void holidayComparisonUsesTotalsAndDailyAveragesAcrossSameNamedHolidays() {
        // H1(2026-07-02 拍板):同名节假日今年 vs 去年;流量=总额+日均双列(天数不同看日均才公平);
        // 去年无同名区间 → previous 侧 null(积累中),同比不出。
        analyticsRepository.holidayPeriods = java.util.List.of(
                new AnalyticsRecords.HolidayPeriod("春节", 2026,
                        LocalDate.parse("2026-02-15"), LocalDate.parse("2026-02-22"), "manual", null),
                new AnalyticsRecords.HolidayPeriod("春节", 2025,
                        LocalDate.parse("2025-01-28"), LocalDate.parse("2025-02-04"), "seed", null));
        analyticsRepository.rangeAggregates = java.util.Map.of(
                "2026-02-15|2026-02-22", java.util.Map.of("revenue.net", new java.math.BigDecimal("800000")),
                "2025-01-28|2025-02-04", java.util.Map.of("revenue.net", new java.math.BigDecimal("640000")));

        AnalyticsRecords.HolidayComparisonResponse response = service.holidayComparison("春节", 2026);
        assertThat(response.currentStart()).isEqualTo(LocalDate.parse("2026-02-15"));
        assertThat(response.previousYear()).isEqualTo(2025);
        AnalyticsRecords.HolidayComparisonRow net = response.rows().stream()
                .filter(row -> row.metricKey().equals("revenue.net")).findFirst().orElseThrow();
        assertThat(net.currentTotal()).isEqualByComparingTo("800000");
        assertThat(net.currentDailyAvg()).isEqualByComparingTo("100000");   // 8 天
        assertThat(net.previousTotal()).isEqualByComparingTo("640000");
        assertThat(net.previousDailyAvg()).isEqualByComparingTo("80000");   // 8 天(1.28-2.4)
        assertThat(net.yoyPct()).isEqualByComparingTo("0.2500");            // 按日均比
        // 去年无同名 → previous null
        analyticsRepository.holidayPeriods = java.util.List.of(
                new AnalyticsRecords.HolidayPeriod("春节", 2026,
                        LocalDate.parse("2026-02-15"), LocalDate.parse("2026-02-22"), "manual", null));
        AnalyticsRecords.HolidayComparisonResponse missing = service.holidayComparison("春节", 2026);
        AnalyticsRecords.HolidayComparisonRow row = missing.rows().stream()
                .filter(r -> r.metricKey().equals("revenue.net")).findFirst().orElseThrow();
        assertThat(row.previousTotal()).isNull();
        assertThat(row.yoyPct()).isNull();
    }

    @Test
    void monthlyComparisonComputesMomYoyAndMarksMissingAsNull() {
        // M1:环比=上月 monthly 行,同比=去年同月;缺行=null(前端标「积累中」),分母0不出百分比。
        analyticsRepository.monthlySnapshots = java.util.Map.of(
                LocalDate.parse("2026-07-01"), java.util.Map.of(
                        "revenue.net", new java.math.BigDecimal("300"),
                        "ar.balance", new java.math.BigDecimal("700")),
                LocalDate.parse("2026-06-01"), java.util.Map.of(
                        "revenue.net", new java.math.BigDecimal("200")));

        AnalyticsRecords.MonthlyComparisonResponse response = service.monthlyComparison("2026-07");
        assertThat(response.month()).isEqualTo("2026-07");
        AnalyticsRecords.MonthlyComparisonRow net = response.rows().stream()
                .filter(row -> row.metricKey().equals("revenue.net")).findFirst().orElseThrow();
        assertThat(net.current()).isEqualByComparingTo("300");
        assertThat(net.prevMonth()).isEqualByComparingTo("200");
        assertThat(net.momPct()).isEqualByComparingTo("0.5000");
        assertThat(net.yoySameMonth()).isNull();
        assertThat(net.yoyPct()).isNull();
        assertThat(net.aggregation()).isEqualTo("monthly_sum");
        AnalyticsRecords.MonthlyComparisonRow balance = response.rows().stream()
                .filter(row -> row.metricKey().equals("ar.balance")).findFirst().orElseThrow();
        assertThat(balance.momPct()).isNull();   // 上月缺行 → 不出百分比
        assertThat(balance.aggregation()).isEqualTo("period_end");
    }

    private static final class InMemoryAnalyticsRepository implements AnalyticsRepository {
        Map<String, Map<String, BigDecimal>> arAging = Map.of();
        BigDecimal arCollected = BigDecimal.ZERO;
        BigDecimal arReceivableMtd = BigDecimal.ZERO;

        public Map<String, Map<String, BigDecimal>> arOverdueAgingByMerchant(LocalDate asOf) {
            return arAging;
        }

        public BigDecimal arCollectedCentsBetween(java.time.Instant start, java.time.Instant end) {
            return arCollected;
        }

        public BigDecimal arReceivableDueCentsBetween(LocalDate monthStart, LocalDate endExclusive) {
            return arReceivableMtd;
        }

        public java.util.List<AnalyticsRecords.ArDebtorRow> listOverdueDebtors(LocalDate asOf, String bucket, int limit, int offset) {
            return java.util.List.of();
        }

        AnalyticsRecords.OccupancyAreas occupancy = new AnalyticsRecords.OccupancyAreas(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        long expiringContracts = 0;

        public AnalyticsRecords.OccupancyAreas occupancyAreas(LocalDate asOf) {
            return occupancy;
        }

        public long countExpiringContracts(LocalDate asOf, int windowDays) {
            return expiringContracts;
        }

        public java.util.List<AnalyticsRecords.VacantSpaceRow> listVacantSpaces(LocalDate asOf, int limit, int offset) {
            return java.util.List.of();
        }

        public java.util.List<AnalyticsRecords.ExpiringContractRow> listExpiringContracts(LocalDate asOf, int windowDays, int limit, int offset) {
            return java.util.List.of();
        }

        java.util.Map<LocalDate, java.util.Map<String, java.math.BigDecimal>> monthlySnapshots = java.util.Map.of();

        java.util.List<AnalyticsRecords.HolidayPeriod> holidayPeriods = java.util.List.of();
        java.util.Map<String, java.util.Map<String, java.math.BigDecimal>> rangeAggregates = java.util.Map.of();

        public java.util.List<AnalyticsRecords.HolidayPeriod> listHolidayPeriods(Integer year) {
            return holidayPeriods;
        }

        public AnalyticsRecords.HolidayPeriod upsertHolidayPeriod(String holidayName, int holidayYear,
                LocalDate startDate, LocalDate endDate, String source, java.time.Instant now) {
            return new AnalyticsRecords.HolidayPeriod(holidayName, holidayYear, startDate, endDate, source, now);
        }

        public boolean isWithinHoliday(LocalDate businessDate) {
            return false;
        }

        public java.util.Map<String, java.math.BigDecimal> aggregateRange(LocalDate start, LocalDate end,
                java.util.List<String> sumKeys, java.util.List<String> lastKeys) {
            return rangeAggregates.getOrDefault(start + "|" + end, java.util.Map.of());
        }

        public void refreshMerchantSummaries(LocalDate businessDate, java.time.Instant now) {
        }

        public void syncMetricDefinitions(java.util.List<AnalyticsRecords.MetricDefinitionSyncRow> rows, java.time.Instant now) {
        }

        public void insertMetricAnomaly(String id, String metricKey, String domain, String riskLevel,
                java.math.BigDecimal observedValue, java.math.BigDecimal expectedValue, String summary, java.time.Instant detectedAt) {
        }

        public AnalyticsRecords.AnomalyConfig readAnomalyConfig() {
            return new AnalyticsRecords.AnomalyConfig(
                    new java.math.BigDecimal("0.5"), new java.math.BigDecimal("0.8"), true, null, null);
        }

        public java.util.List<AnalyticsRecords.NotificationWindow> listNotificationWindows() {
            return java.util.List.of();
        }

        public AnalyticsRecords.NotificationWindow upsertNotificationWindow(String windowKey, int days,
                String description, String updatedBy, java.time.Instant now) {
            return new AnalyticsRecords.NotificationWindow(windowKey, days, description, updatedBy, now);
        }

        public AnalyticsRecords.AiThresholds readAiThresholds() {
            return new AnalyticsRecords.AiThresholds(
                    new java.math.BigDecimal("0.60"), new java.math.BigDecimal("0.20"), null, null);
        }

        public AnalyticsRecords.AiThresholds upsertAiThresholds(java.math.BigDecimal minConfidence,
                java.math.BigDecimal benchmarkDeviation, String updatedBy, java.time.Instant now) {
            return new AnalyticsRecords.AiThresholds(minConfidence, benchmarkDeviation, updatedBy, now);
        }

        public AnalyticsRecords.AnomalyConfig upsertAnomalyConfig(java.math.BigDecimal surgePct, java.math.BigDecimal highPct,
                boolean enabled, String updatedBy, java.time.Instant now) {
            return new AnalyticsRecords.AnomalyConfig(surgePct, highPct, enabled, updatedBy, now);
        }

        public void rollupMonthlySnapshots(LocalDate monthFirst, LocalDate upTo,
                java.util.List<String> sumKeys, java.util.List<String> lastKeys, java.time.Instant now) {
        }

        public java.util.Map<String, java.math.BigDecimal> readMonthlySnapshots(LocalDate monthFirst) {
            return monthlySnapshots.getOrDefault(monthFirst, java.util.Map.of());
        }

        public AnalyticsRecords.WorkOrderSla workOrderSla(java.time.Instant start, java.time.Instant end, java.time.Instant asOf) {
            return new AnalyticsRecords.WorkOrderSla(null, null, 0, 0, 0);
        }

        public AnalyticsRecords.WorkOrderSla merchantWorkOrderSla(java.time.Instant start, java.time.Instant end, java.time.Instant asOf) {
            return new AnalyticsRecords.WorkOrderSla(null, null, 0, 0, 0);
        }

        public java.util.List<AnalyticsRecords.OverdueWorkOrderRow> listOverdueMerchantWorkOrders(
                java.time.Instant start, java.time.Instant end, java.time.Instant asOf, int limit, int offset) {
            return java.util.List.of();
        }

        public java.util.List<AnalyticsRecords.OverdueWorkOrderRow> listOverdueWorkOrders(
                java.time.Instant start, java.time.Instant end, java.time.Instant asOf, int limit, int offset) {
            return java.util.List.of();
        }

        public java.util.Map<String, java.math.BigDecimal> energyConsumptionByType(java.time.Instant start, java.time.Instant end) {
            return java.util.Map.of();
        }

        public long countAbnormalReadings(java.time.Instant start, java.time.Instant end) {
            return 0;
        }

        public java.util.List<AnalyticsRecords.AbnormalReadingRow> listAbnormalReadings(
                java.time.Instant start, java.time.Instant end, int limit, int offset) {
            return java.util.List.of();
        }

        public java.util.List<AnalyticsRecords.BusinessDayCutoffRecord> listBusinessDayCutoffs() {
            return java.util.List.of();
        }

        public AnalyticsRecords.BusinessDayCutoffRecord upsertBusinessDayCutoff(String domain, int cutoffHour, String updatedBy, java.time.Instant now) {
            return new AnalyticsRecords.BusinessDayCutoffRecord(domain, cutoffHour, now, updatedBy);
        }

        private final Map<String, String> projectEntityMap = new LinkedHashMap<>();
        private final List<MetricDefinitionRecord> metricDefinitions = new ArrayList<>();
        private DashboardSnapshotRecord dashboardSnapshot;
        private final List<BusinessMetricSnapshotRecord> businessMetricSnapshots = new ArrayList<>();
        private final List<MerchantSummaryRecord> merchantSummaries = new ArrayList<>();
        private final List<MerchantHealthScoreRecord> merchantHealthScores = new ArrayList<>();
        private final List<MetricAnomalyRecord> metricAnomalies = new ArrayList<>();
        private final List<RiskRuleRecord> riskRules = new ArrayList<>();
        private final Map<String, RiskAlertRecord> riskAlerts = new LinkedHashMap<>();
        private final List<AiQualityTaskRecord> aiQualityTasks = new ArrayList<>();
        private final List<AiQualityResultRecord> aiQualityResults = new ArrayList<>();
        private final List<CustomerIntentRecord> customerIntents = new ArrayList<>();
        private final List<RobotMetricRecord> robotMetrics = new ArrayList<>();
        private final List<StaffServiceScoreRecord> staffServiceScores = new ArrayList<>();
        private boolean failAiQualityResults;
        // 测试钩子：非 null 时把保存的风险预警 riskLevel 强制改写，用于验证双写映射分支（如 medium→suspicious）。
        private String overrideSavedRiskLevel;
        private int riskAlertSaveCount;
        private int riskAlertUpdateCount;
        private int aiQualityTaskSaveCount;
        private int aiQualityResultSaveCount;

        @Override
        public Map<String, String> projectBusinessEntityMap() {
            return projectEntityMap;
        }

        @Override
        public List<AiQualityResultRecord> listAiQualityResults() {
            if (failAiQualityResults) {
                throw new IllegalStateException("analytics store unavailable");
            }
            return aiQualityResults;
        }

        @Override
        public List<AiQualityTaskRecord> listAiQualityTasks() {
            return aiQualityTasks;
        }

        @Override
        public AiQualityTaskRecord saveAiQualityTask(AiQualityTaskRecord task) {
            aiQualityTaskSaveCount += 1;
            aiQualityTasks.add(0, task);
            return task;
        }

        @Override
        public AiQualityResultRecord saveAiQualityResult(AiQualityResultRecord result) {
            aiQualityResultSaveCount += 1;
            aiQualityResults.add(0, result);
            return result;
        }

        @Override
        public List<CustomerIntentRecord> listCustomerIntents() {
            return customerIntents;
        }

        @Override
        public DashboardSnapshotRecord getP0OverviewDashboard() {
            return dashboardSnapshot;
        }

        @Override
        public List<BusinessMetricSnapshotRecord> listBusinessMetricSnapshots() {
            return businessMetricSnapshots;
        }

        @Override
        public List<MerchantHealthScoreRecord> listMerchantHealthScores() {
            return merchantHealthScores;
        }

        @Override
        public List<MerchantSummaryRecord> listAnalyticsMerchants() {
            return merchantSummaries;
        }

        @Override
        public List<MetricAnomalyRecord> listMetricAnomalies() {
            return metricAnomalies;
        }

        @Override
        public List<MetricDefinitionRecord> listMetricDefinitions() {
            return metricDefinitions;
        }

        @Override
        public List<RiskAlertRecord> listRiskAlerts() {
            return new ArrayList<>(riskAlerts.values());
        }

        @Override
        public AnalyticsRecords.BusinessReviewLockRecord saveReviewLock(AnalyticsRecords.BusinessReviewLockRecord lock) {
            return lock;
        }

        @Override
        public List<AnalyticsRecords.BusinessReviewLockRecord> listReviewLocks(String projectScope) {
            return List.of();
        }

        @Override
        public RiskAlertRecord saveRiskAlert(RiskAlertRecord alert) {
            riskAlertSaveCount += 1;
            RiskAlertRecord stored = overrideSavedRiskLevel == null
                    ? alert
                    : new RiskAlertRecord(
                            alert.id(), alert.ruleId(), alert.alertType(), overrideSavedRiskLevel,
                            alert.objectType(), alert.objectId(), alert.summary(), alert.status(),
                            alert.operatorNote(), alert.evidenceAttachmentIds(), alert.idempotencyKey(),
                            alert.createdAt(), alert.resolvedAt(), alert.updatedAt());
            riskAlerts.put(stored.id(), stored);
            return stored;
        }

        @Override
        public Optional<RiskAlertRecord> findRiskAlertById(String id) {
            return Optional.ofNullable(riskAlerts.get(id));
        }

        @Override
        public RiskAlertRecord updateRiskAlert(RiskAlertRecord alert) {
            riskAlertUpdateCount += 1;
            riskAlerts.put(alert.id(), alert);
            return alert;
        }

        @Override
        public List<RiskRuleRecord> listRiskRules() {
            return riskRules;
        }

        @Override
        public List<RobotMetricRecord> listRobotMetrics() {
            return robotMetrics;
        }

        @Override
        public List<StaffServiceScoreRecord> listStaffServiceScores() {
            return staffServiceScores;
        }

        // --- T12 看板真实聚合 / 行级隔离（内存实现，用 metricSnapshots 模拟 ETL 落地的快照） ---
        // key = scope|metricKey（scope=null 用 "__platform__"），value = actual_value。
        private final Map<String, Map<String, BigDecimal>> metricSnapshots = new LinkedHashMap<>();
        private LocalDate etlBusinessDate;

        @Override
        public Map<String, BigDecimal> readMetricSnapshots(LocalDate businessDate, String scope) {
            String key = scope == null ? "__platform__" : scope;
            return new LinkedHashMap<>(metricSnapshots.getOrDefault(key, Map.of()));
        }

        @Override
        public Optional<LocalDate> latestEtlBusinessDate(String scope) {
            return Optional.ofNullable(etlBusinessDate);
        }

        // project 口径快照：key = projectId，value = metricKey -> actual_value（与平台/商户口径分桶存储）。
        private final Map<String, Map<String, BigDecimal>> projectSnapshots = new LinkedHashMap<>();

        @Override
        public void upsertMetricSnapshot(
                com.complex.platform.analytics.AnalyticsRecords.MetricSnapshotKey snapshotKey,
                BigDecimal actualValue,
                String projectionMethod,
                Instant createdAt
        ) {
            etlBusinessDate = snapshotKey.businessDate();
            if ("project".equals(snapshotKey.segmentType())) {
                projectSnapshots.computeIfAbsent(snapshotKey.segmentId(), ignored -> new LinkedHashMap<>())
                        .put(snapshotKey.metricKey(), actualValue);
                return;
            }
            String scope = "merchant".equals(snapshotKey.segmentType()) ? snapshotKey.segmentId() : "__platform__";
            metricSnapshots.computeIfAbsent(scope, ignored -> new LinkedHashMap<>())
                    .put(snapshotKey.metricKey(), actualValue);
        }

        @Override
        public List<String> listProjectIdsWithEtlSnapshot(LocalDate businessDate) {
            return new ArrayList<>(projectSnapshots.keySet());
        }

        @Override
        public Map<String, BigDecimal> readProjectMetricSnapshots(LocalDate businessDate, String projectId) {
            return new LinkedHashMap<>(projectSnapshots.getOrDefault(projectId, Map.of()));
        }

        @Override
        public BigDecimal sumTicketRevenueCents(LocalDate businessDate, String windowDomain) {
            return BigDecimal.ZERO;
        }

        @Override
        public BigDecimal sumTicketRefundCents(LocalDate businessDate, String windowDomain) {
            return BigDecimal.ZERO;
        }

        @Override
        public Map<String, BigDecimal> sumBarRevenueCentsByMerchant(LocalDate businessDate, String windowDomain) {
            return Map.of();
        }

        @Override
        public Map<String, BigDecimal> sumBarRefundCentsByMerchant(LocalDate businessDate, String windowDomain) {
            return Map.of();
        }

        @Override
        public Map<String, BigDecimal> sumTicketBarShareCentsByMerchant(LocalDate businessDate, String windowDomain) {
            return Map.of();
        }

        @Override
        public Map<String, BigDecimal> sumTicketAmusementShareCentsByMerchant(LocalDate businessDate, String windowDomain) {
            return Map.of();
        }

        @Override
        public Map<String, BigDecimal> sumTicketAllocatedRevenueByMerchant(LocalDate businessDate, String windowDomain) {
            return Map.of();
        }

        @Override
        public Map<String, BigDecimal> sumTicketRefundByMerchant(LocalDate businessDate, String windowDomain) {
            return Map.of();
        }

        @Override
        public Map<String, BigDecimal> sumContractRentReceivableByMerchant(LocalDate businessDate, String windowDomain) {
            return Map.of();
        }

        @Override
        public BigDecimal countAmusementVerificationSuccess(LocalDate businessDate) {
            return BigDecimal.ZERO;
        }

        @Override
        public Map<String, BigDecimal> countTicketVerificationSuccessByMerchant(LocalDate businessDate) {
            return Map.of();
        }

        final Map<Integer, Long> hourlyTraffic = new LinkedHashMap<>();

        @Override
        public Map<Integer, Long> hourlyTrafficCounts(LocalDate businessDate, Optional<String> merchantScope) {
            return hourlyTraffic;
        }

        final Map<String, OperatingParamsRecord> operatingParams = new LinkedHashMap<>();

        @Override
        public Optional<OperatingParamsRecord> findOperatingParams(String projectId) {
            return Optional.ofNullable(operatingParams.get(projectId));
        }

        @Override
        public List<OperatingParamsRecord> listOperatingParams() {
            return new ArrayList<>(operatingParams.values());
        }

        @Override
        public OperatingParamsRecord upsertOperatingParams(OperatingParamsRecord record) {
            operatingParams.put(record.projectId(), record);
            return record;
        }

        @Override
        public Map<String, BigDecimal> countTicketIssuedByMerchant(LocalDate businessDate) {
            return Map.of();
        }

        @Override
        public BigDecimal countNewMembers(LocalDate businessDate) {
            return BigDecimal.ZERO;
        }

        @Override
        public Map<String, BigDecimal> sumInventoryBalanceByMerchant() {
            return Map.of();
        }

        @Override
        public Map<String, BigDecimal> countLowStockByMerchant() {
            return Map.of();
        }

        @Override
        public BigDecimal countPendingReview() {
            return BigDecimal.ZERO;
        }

        @Override
        public long countMerchantsWithEtlSnapshot(LocalDate businessDate) {
            // 内存模拟：非平台 scope 且有快照的去重商户数。
            return metricSnapshots.entrySet().stream()
                    .filter(e -> !"__platform__".equals(e.getKey()))
                    .filter(e -> !e.getValue().isEmpty())
                    .count();
        }
    }



    private static final class CapturingLocalLlmClient implements LocalLlmClient {
        private String lastSourceText;
        private LocalLlmSuggestion nextSuggestion = LocalLlmSuggestion.unavailable("disabled", "local-model");

        @Override
        public LocalLlmSuggestion suggestQuality(String sourceType, String sourceId, String redactedText) {
            lastSourceText = redactedText;
            return nextSuggestion;
        }
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
