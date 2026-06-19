package com.complex.platform.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.complex.platform.analytics.AnalyticsRecords.RiskAlertRecord;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class AnalyticsJdbcIntegrationTests {

    @Autowired
    private JdbcAnalyticsRepository repository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesAnalyticsTablesSeedsPermissionsAndCurrentFlywayVersion() {
        assertThat(regclass("analytics_metric_definitions")).isEqualTo("analytics_metric_definitions");
        assertThat(regclass("analytics_dashboard_snapshots")).isEqualTo("analytics_dashboard_snapshots");
        assertThat(regclass("analytics_business_metric_snapshots")).isEqualTo("analytics_business_metric_snapshots");
        assertThat(regclass("analytics_merchant_summaries")).isEqualTo("analytics_merchant_summaries");
        assertThat(regclass("analytics_merchant_health_scores")).isEqualTo("analytics_merchant_health_scores");
        assertThat(regclass("analytics_metric_anomalies")).isEqualTo("analytics_metric_anomalies");
        assertThat(regclass("analytics_risk_rules")).isEqualTo("analytics_risk_rules");
        assertThat(regclass("analytics_risk_alerts")).isEqualTo("analytics_risk_alerts");
        assertThat(regclass("analytics_ai_quality_tasks")).isEqualTo("analytics_ai_quality_tasks");
        assertThat(regclass("analytics_ai_quality_results")).isEqualTo("analytics_ai_quality_results");
        assertThat(regclass("analytics_customer_intents")).isEqualTo("analytics_customer_intents");
        assertThat(regclass("analytics_robot_metrics")).isEqualTo("analytics_robot_metrics");
        assertThat(regclass("analytics_staff_service_scores")).isEqualTo("analytics_staff_service_scores");
        assertThat(currentFlywayVersion()).isEqualTo(com.complex.platform.support.MigrationVersions.latest());

        assertThat(permissionCodes()).contains(
                "analytics.dashboard.view",
                "analytics.risk_alert.view",
                "analytics.risk_alert.manage"
        );
        assertThat(platformAdminPermissionCodes()).contains(
                "analytics.dashboard.view",
                "analytics.risk_alert.view",
                "analytics.risk_alert.manage"
        );
    }

    @Test
    void migrationSeedsOneDeterministicRowForEveryReadEndpoint() {
        assertThat(rowCount("analytics_metric_definitions")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_dashboard_snapshots")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_business_metric_snapshots")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_merchant_summaries")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_merchant_health_scores")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_metric_anomalies")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_risk_rules")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_risk_alerts")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_ai_quality_tasks")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_ai_quality_results")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_customer_intents")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_robot_metrics")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_staff_service_scores")).isGreaterThanOrEqualTo(1);
    }

    @Test
    void repositoryListsSeededAnalyticsReadModels() {
        assertThat(repository.listMetricDefinitions())
                .extracting(AnalyticsRecords.MetricDefinitionRecord::metricKey, AnalyticsRecords.MetricDefinitionRecord::unit)
                .contains(tuple("gmv_amount_cents", "cent"));
        assertThat(repository.getP0OverviewDashboard().snapshotKey()).isEqualTo("p0_overview_today");
        assertThat(repository.listBusinessMetricSnapshots())
                .extracting(AnalyticsRecords.BusinessMetricSnapshotRecord::metricKey, AnalyticsRecords.BusinessMetricSnapshotRecord::trendDirection)
                .contains(tuple("gmv_amount_cents", "up"));
        assertThat(repository.listAnalyticsMerchants())
                .extracting(AnalyticsRecords.MerchantSummaryRecord::merchantId, AnalyticsRecords.MerchantSummaryRecord::revenueAmountCents)
                .contains(tuple("merchant_p0", 128800L));
        assertThat(repository.listMerchantHealthScores())
                .extracting(AnalyticsRecords.MerchantHealthScoreRecord::merchantId, AnalyticsRecords.MerchantHealthScoreRecord::scoreLevel)
                .contains(tuple("merchant_p0", "healthy"));
        assertThat(repository.listMetricAnomalies())
                .extracting(AnalyticsRecords.MetricAnomalyRecord::metricKey, AnalyticsRecords.MetricAnomalyRecord::riskLevel)
                .contains(tuple("gmv_amount_cents", "suspicious"));
        assertThat(repository.listRiskRules())
                .extracting(AnalyticsRecords.RiskRuleRecord::ruleId, AnalyticsRecords.RiskRuleRecord::enabled)
                .contains(tuple("rule_p0_gmv_drop", true));
        assertThat(repository.listAiQualityTasks())
                .extracting(AnalyticsRecords.AiQualityTaskRecord::taskId, AnalyticsRecords.AiQualityTaskRecord::processingMode)
                .contains(tuple("ai_task_p0", "manual_review"));
        assertThat(repository.listAiQualityResults())
                .extracting(AnalyticsRecords.AiQualityResultRecord::resultId, AnalyticsRecords.AiQualityResultRecord::riskLevel)
                .contains(tuple("ai_result_p0", "normal"));
        assertThat(repository.listCustomerIntents())
                .extracting(AnalyticsRecords.CustomerIntentRecord::intentId, AnalyticsRecords.CustomerIntentRecord::intentType)
                .contains(tuple("intent_p0", "refund_request"));
        assertThat(repository.listRobotMetrics())
                .extracting(AnalyticsRecords.RobotMetricRecord::metricId, AnalyticsRecords.RobotMetricRecord::successCount)
                .contains(tuple("robot_metric_p0", 9));
        assertThat(repository.listStaffServiceScores())
                .extracting(AnalyticsRecords.StaffServiceScoreRecord::scoreId, AnalyticsRecords.StaffServiceScoreRecord::serviceScore)
                .contains(tuple("staff_score_p0", 95));
        assertThat(repository.listRiskAlerts())
                .extracting(RiskAlertRecord::id, RiskAlertRecord::status)
                .contains(tuple("alert_p0", "open"));
    }

    @Test
    void createListAndResolveRiskAlertPersistsAuditBoundaryFields() {
        Instant createdAt = Instant.parse("2026-06-14T03:00:00Z");
        RiskAlertRecord alert = repository.saveRiskAlert(new RiskAlertRecord(
                "alert_jdbc",
                "rule_p0_gmv_drop",
                "metric_anomaly",
                "critical",
                "merchant",
                "merchant_jdbc",
                "Revenue drop needs review",
                "open",
                "created by integration test",
                List.of("att_create"),
                "analytics-jdbc-create",
                createdAt,
                null,
                createdAt
        ));

        assertThat(alert.id()).isEqualTo("alert_jdbc");
        assertThat(repository.listRiskAlerts())
                .extracting(RiskAlertRecord::id, RiskAlertRecord::idempotencyKey, RiskAlertRecord::operatorNote)
                .contains(tuple("alert_jdbc", "analytics-jdbc-create", "created by integration test"));

        Instant resolvedAt = Instant.parse("2026-06-14T03:10:00Z");
        RiskAlertRecord resolved = repository.updateRiskAlert(new RiskAlertRecord(
                alert.id(),
                alert.ruleId(),
                alert.alertType(),
                alert.riskLevel(),
                alert.objectType(),
                alert.objectId(),
                alert.summary(),
                "resolved",
                "handled by integration test",
                List.of("att_resolve"),
                alert.idempotencyKey(),
                alert.createdAt(),
                resolvedAt,
                resolvedAt
        ));

        assertThat(resolved)
                .extracting(RiskAlertRecord::status, RiskAlertRecord::operatorNote, RiskAlertRecord::evidenceAttachmentIds, RiskAlertRecord::resolvedAt)
                .containsExactly("resolved", "handled by integration test", List.of("att_resolve"), resolvedAt);
        assertThat(repository.findRiskAlertById("alert_jdbc"))
                .get()
                .extracting(RiskAlertRecord::status, RiskAlertRecord::idempotencyKey, RiskAlertRecord::operatorNote, RiskAlertRecord::resolvedAt)
                .containsExactly("resolved", "analytics-jdbc-create", "handled by integration test", resolvedAt);
    }

    @Test
    void dashboardEtlAggregatesRealBusinessTablesAndUpsertsNonSeedSnapshots() {
        java.time.LocalDate businessDate = java.time.LocalDate.parse("2026-06-17");
        // 造真实业务数据：当日 2 名新会员 + 1 条 pending 复核。
        insertCustomer("cust_etl_1", "13900000001", businessDate);
        insertCustomer("cust_etl_2", "13900000002", businessDate);
        insertPendingReview("review_etl_1");

        var job = new DashboardAggregationJob(repository,
                java.time.Clock.fixed(Instant.parse("2026-06-17T05:00:00Z"), java.time.ZoneOffset.UTC));
        job.refreshSnapshots(businessDate);

        java.util.Map<String, java.math.BigDecimal> platform = repository.readMetricSnapshots(businessDate, null);
        // 对拍真实业务表：member.new = 当日真实新建会员数（非种子）。
        assertThat(platform.get("member.new")).isEqualByComparingTo("2");
        // risk.pending_review 含 supervisor_review_decisions pending + analytics_risk_alerts open（种子 alert_p0 open=1） → >=2。
        assertThat(platform.get("risk.pending_review")).isGreaterThanOrEqualTo(java.math.BigDecimal.valueOf(2));
        // projection_method 标注为 etl_aggregate（非 manual_baseline 种子）。
        assertThat(jdbcTemplate.queryForObject("""
                SELECT projection_method FROM analytics_business_metric_snapshots
                 WHERE metric_key = 'member.new' AND business_date = :d AND segment_type = 'platform'
                """, Map.of("d", businessDate), String.class)).isEqualTo("etl_aggregate");
    }

    @Test
    void groupOverviewCountsDistinctMerchantsWithEtlSnapshotAndSeedsPermission() {
        java.time.LocalDate businessDate = java.time.LocalDate.parse("2026-06-15");
        Instant now = Instant.parse("2026-06-15T05:00:00Z");
        var keyA = new AnalyticsRecords.MetricSnapshotKey("revenue.bar", "bar", "daily", businessDate, "merchant", "merchant_grp_a");
        var keyB = new AnalyticsRecords.MetricSnapshotKey("revenue.net", "finance", "daily", businessDate, "merchant", "merchant_grp_b");
        var keyADup = new AnalyticsRecords.MetricSnapshotKey("verify.done", "ticketing", "daily", businessDate, "merchant", "merchant_grp_a");
        repository.upsertMetricSnapshot(keyA, new java.math.BigDecimal("5000"), "etl_aggregate", now);
        repository.upsertMetricSnapshot(keyB, new java.math.BigDecimal("9000"), "etl_aggregate", now);
        repository.upsertMetricSnapshot(keyADup, new java.math.BigDecimal("12"), "etl_aggregate", now);

        // 去重商户数：merchant_grp_a 出现两次（两指标）仍计 1，加 merchant_grp_b → 2。
        assertThat(repository.countMerchantsWithEtlSnapshot(businessDate)).isEqualTo(2L);
        // 无 ETL 商户快照的业务日 → 0（不伪造）。
        assertThat(repository.countMerchantsWithEtlSnapshot(java.time.LocalDate.parse("2099-01-01"))).isZero();

        // 集团总览权限已种入并授予平台管理员。
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM permissions WHERE permission_code = 'analytics.group.view' AND status = 'active'
                """, Map.of(), Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM role_permissions
                 WHERE role_id = 'role_platform_admin' AND permission_code = 'analytics.group.view'
                """, Map.of(), Long.class)).isEqualTo(1L);
    }

    @Test
    void metricSnapshotUpsertIsIdempotentAndScopeIsolated() {
        java.time.LocalDate businessDate = java.time.LocalDate.parse("2026-06-16");
        Instant now = Instant.parse("2026-06-16T05:00:00Z");
        var keyMerchantA = new AnalyticsRecords.MetricSnapshotKey("revenue.bar", "bar", "daily", businessDate, "merchant", "merchant_iso_a");
        var keyMerchantB = new AnalyticsRecords.MetricSnapshotKey("revenue.bar", "bar", "daily", businessDate, "merchant", "merchant_iso_b");

        repository.upsertMetricSnapshot(keyMerchantA, new java.math.BigDecimal("5000"), "etl_aggregate", now);
        repository.upsertMetricSnapshot(keyMerchantB, new java.math.BigDecimal("9000"), "etl_aggregate", now);
        // 重复 upsert 同键 → 幂等（不重复插入，更新值）。
        repository.upsertMetricSnapshot(keyMerchantA, new java.math.BigDecimal("7000"), "etl_aggregate", now);

        // 行级隔离：商户 A 只见自身值 7000，看不到 B 的 9000。
        assertThat(repository.readMetricSnapshots(businessDate, "merchant_iso_a").get("revenue.bar")).isEqualByComparingTo("7000");
        assertThat(repository.readMetricSnapshots(businessDate, "merchant_iso_b").get("revenue.bar")).isEqualByComparingTo("9000");
        // 幂等：A 在该业务日只有一行。
        assertThat(jdbcTemplate.queryForObject("""
                SELECT count(*) FROM analytics_business_metric_snapshots
                 WHERE metric_key = 'revenue.bar' AND business_date = :d AND segment_type = 'merchant' AND segment_id = 'merchant_iso_a'
                """, Map.of("d", businessDate), Long.class)).isEqualTo(1L);
    }

    @Test
    void ticketRevenueShareSplitsAllocatedRevenueByBusinessTypeAndMerchantWithoutTouchingNet() {
        // 待确认项#1：通票售票收入业态拆分展示口径——按 issued_entitlements.allocated_revenue_cent 汇总，
        // gift_drink→酒吧分摊、amusement→游乐分摊，按商户行级；不影响 revenue.bar（消费实收）/net 口径。
        java.time.LocalDate businessDate = java.time.LocalDate.parse("2026-06-17");
        // 组合票实付 800 拆分：酒吧 480（mch_bar）/ 游乐 320（mch_amu）。
        insertIssuedEntitlement("ent_share_bar", "gift_drink", "mch_bar", 480L, businessDate);
        insertIssuedEntitlement("ent_share_amu", "amusement", "mch_amu", 320L, businessDate);
        // 无 allocated（direct 出票）→ 不计入任何 share。
        insertIssuedEntitlement("ent_share_null", "entry", "mch_bar", null, businessDate);

        Map<String, java.math.BigDecimal> barShare = repository.sumTicketBarShareCentsByMerchant(businessDate);
        Map<String, java.math.BigDecimal> amuShare = repository.sumTicketAmusementShareCentsByMerchant(businessDate);

        assertThat(barShare.get("mch_bar")).isEqualByComparingTo("480");
        assertThat(barShare).doesNotContainKey("mch_amu");
        assertThat(amuShare.get("mch_amu")).isEqualByComparingTo("320");
        assertThat(amuShare).doesNotContainKey("mch_bar");
    }

    @Test
    void ticketAllocatedRevenueAndRefundAreAggregatedAndDistributedByMerchant() {
        // 清单#4：售票分量按商户(allocated) + 售票退款按 allocated 占比分摊到商户。
        java.time.LocalDate businessDate = java.time.LocalDate.parse("2026-06-16");
        // 订单 ord_net 实付 1000，发两张权益：mch_a 分摊 600 / mch_b 分摊 400（Σ=订单实付）。
        insertTicketOrder("ord_net", 1000L, businessDate);
        insertIssuedEntitlementForOrder("ent_net_a", "ord_net", "drink", "mch_a", 600L, businessDate);
        insertIssuedEntitlementForOrder("ent_net_b", "ord_net", "amusement", "mch_b", 400L, businessDate);

        Map<String, java.math.BigDecimal> allocated = repository.sumTicketAllocatedRevenueByMerchant(businessDate);
        assertThat(allocated.get("mch_a")).isEqualByComparingTo("600");
        assertThat(allocated.get("mch_b")).isEqualByComparingTo("400");

        // 退款 300 针对 ord_net：按 allocated 占比分摊 → mch_a=180(=300*600/1000)、mch_b=120(=300*400/1000)。
        insertTicketRefund("rf_net", "ord_net", 300L, businessDate);
        Map<String, java.math.BigDecimal> refundShare = repository.sumTicketRefundByMerchant(businessDate);
        assertThat(refundShare.get("mch_a")).isEqualByComparingTo("180");
        assertThat(refundShare.get("mch_b")).isEqualByComparingTo("120");
        // Σ分摊 == 退款额（不丢分）。
        assertThat(refundShare.get("mch_a").add(refundShare.get("mch_b"))).isEqualByComparingTo("300");
    }

    @Test
    void ticketRefundForOrderWithoutAllocatedEntitlementsIsNotAttributedToAnyMerchant() {
        // 清单#4：订单无 allocated 权益（direct 出票/历史）→ 该退款保守不归任何商户。
        java.time.LocalDate businessDate = java.time.LocalDate.parse("2026-06-15");
        insertTicketOrder("ord_noalloc", 500L, businessDate);
        insertIssuedEntitlementForOrder("ent_noalloc", "ord_noalloc", "entry", "mch_x", null, businessDate);
        insertTicketRefund("rf_noalloc", "ord_noalloc", 200L, businessDate);

        Map<String, java.math.BigDecimal> refundShare = repository.sumTicketRefundByMerchant(businessDate);
        assertThat(refundShare).doesNotContainKey("mch_x");
        assertThat(refundShare).isEmpty();
    }

    @Test
    void lowStockCountsSkusBelowPerSkuAbsoluteThresholdAndIgnoresZeroThreshold() {
        // 财务确认：缺货预警 = 绝对值阈值/SKU 可配（inventory_items.min_quantity），阈值 ≤ 0 不预警。
        // SKU 低于阈值 → 计入。
        insertInventoryItem("item_low", "merchant_low_stock", 100);
        insertStockBalance("bal_low", "item_low", "loc_1", 30);
        // SKU 充足（>= 阈值）→ 不计入。
        insertInventoryItem("item_ok", "merchant_low_stock", 100);
        insertStockBalance("bal_ok", "item_ok", "loc_1", 150);
        // SKU 阈值 0（不预警）→ 即使为 0 库存也不计入。
        insertInventoryItem("item_no_threshold", "merchant_low_stock", 0);
        insertStockBalance("bal_no_threshold", "item_no_threshold", "loc_1", 0);

        Map<String, java.math.BigDecimal> byMerchant = repository.countLowStockByMerchant();
        assertThat(byMerchant.get("merchant_low_stock")).isEqualByComparingTo("1");
    }

    private void insertIssuedEntitlement(String id, String entitlementType, String merchantId,
                                         Long allocatedRevenueCent, java.time.LocalDate businessDate) {
        Instant at = businessDate.atTime(10, 0).toInstant(java.time.ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(at);
        // 满足 FK：ticket_type / entitlement_template 先存在（共享父行，幂等）。
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES ('tt_share', 'Share Pass', 'weekday_weekend_holiday', FALSE, 'published', :at, :at)
                ON CONFLICT (id) DO NOTHING
                """, Map.of("at", ts));
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent,
                                                   status, created_at, updated_at)
                VALUES ('tpl_share', 'Share Tpl', 'entry', 1, 0, 'active', :at, :at)
                ON CONFLICT (id) DO NOTHING
                """, Map.of("at", ts));
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("type", entitlementType)
                .addValue("merchant_id", merchantId)
                .addValue("allocated", allocatedRevenueCent)
                .addValue("at", ts);
        jdbcTemplate.update("""
                INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id, entitlement_code,
                    entitlement_type, merchant_id, total_times, used_times, status, valid_from, valid_to,
                    created_at, updated_at, num_people, allocated_revenue_cent)
                VALUES (:id, 'tt_share', :id, 'tpl_share', :id, :type, :merchant_id, 1, 0, 'active', :at, :at,
                    :at, :at, 1, :allocated)
                """, params);
    }

    private void insertTicketOrder(String id, long amountCent, java.time.LocalDate businessDate) {
        Instant at = businessDate.atTime(10, 0).toInstant(java.time.ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(at);
        // 满足 FK：ticket_type 先存在（共享 tt_share，幂等）。
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES ('tt_share', 'Share Pass', 'weekday_weekend_holiday', FALSE, 'published', :at, :at)
                ON CONFLICT (id) DO NOTHING
                """, Map.of("at", ts));
        jdbcTemplate.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent,
                    pay_status, issue_status, status, created_at, updated_at)
                VALUES (:id, :id, 'tt_share', 1, 'online', :amount, 'paid', 'issued', 'active', :at, :at)
                """, Map.of("id", id, "amount", amountCent, "at", ts));
    }

    private void insertIssuedEntitlementForOrder(String id, String orderId, String entitlementType, String merchantId,
                                                 Long allocatedRevenueCent, java.time.LocalDate businessDate) {
        Instant at = businessDate.atTime(10, 0).toInstant(java.time.ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(at);
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES ('tt_share', 'Share Pass', 'weekday_weekend_holiday', FALSE, 'published', :at, :at)
                ON CONFLICT (id) DO NOTHING
                """, Map.of("at", ts));
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent,
                                                   status, created_at, updated_at)
                VALUES ('tpl_share', 'Share Tpl', 'entry', 1, 0, 'active', :at, :at)
                ON CONFLICT (id) DO NOTHING
                """, Map.of("at", ts));
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("order_id", orderId)
                .addValue("type", entitlementType)
                .addValue("merchant_id", merchantId)
                .addValue("allocated", allocatedRevenueCent)
                .addValue("at", ts);
        jdbcTemplate.update("""
                INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id, entitlement_code,
                    entitlement_type, merchant_id, total_times, used_times, status, valid_from, valid_to,
                    created_at, updated_at, num_people, allocated_revenue_cent)
                VALUES (:id, 'tt_share', :order_id, 'tpl_share', :id, :type, :merchant_id, 1, 0, 'active', :at, :at,
                    :at, :at, 1, :allocated)
                """, params);
    }

    private void insertTicketRefund(String id, String targetOrderId, long refundAmountCent, java.time.LocalDate businessDate) {
        Instant at = businessDate.atTime(11, 0).toInstant(java.time.ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO ticket_refunds (id, target_id, refund_amount_cent, reason, refund_channel, status,
                    created_at, updated_at)
                VALUES (:id, :target, :amount, 'test refund', 'original', 'approved', :at, :at)
                """, Map.of("id", id, "target", targetOrderId, "amount", refundAmountCent,
                "at", java.sql.Timestamp.from(at)));
    }

    private void insertInventoryItem(String id, String merchantId, int minQuantity) {
        Instant at = Instant.parse("2026-06-17T00:00:00Z");
        jdbcTemplate.update("""
                INSERT INTO inventory_items (id, item_name, item_type, unit, min_quantity, status,
                                             created_at, updated_at, merchant_id)
                VALUES (:id, :name, 'liquor', 'bottle', :minq, 'active', :at, :at, :mid)
                """, Map.of("id", id, "name", id, "minq", minQuantity, "at", java.sql.Timestamp.from(at), "mid", merchantId));
    }

    private void insertStockBalance(String id, String itemId, String locationId, int available) {
        Instant at = Instant.parse("2026-06-17T00:00:00Z");
        jdbcTemplate.update("""
                INSERT INTO inventory_stock_balances (id, item_id, location_id, quantity,
                                                      reserved_quantity, available_quantity, updated_at)
                VALUES (:id, :item, :loc, :qty, 0, :avail, :at)
                """, Map.of("id", id, "item", itemId, "loc", locationId, "qty", available,
                "avail", available, "at", java.sql.Timestamp.from(at)));
    }

    private void insertCustomer(String id, String phone, java.time.LocalDate businessDate) {
        Instant at = businessDate.atTime(10, 0).toInstant(java.time.ZoneOffset.UTC);
        jdbcTemplate.update("""
                INSERT INTO customers (id, phone, masked_phone, status, created_at, updated_at)
                VALUES (:id, :phone, :masked, 'active', :at, :at)
                """, Map.of("id", id, "phone", phone, "masked", "139****" + phone.substring(7), "at", java.sql.Timestamp.from(at)));
    }

    private void insertPendingReview(String reviewId) {
        jdbcTemplate.update("""
                INSERT INTO supervisor_review_decisions (
                    review_id, policy_code, action_type, object_type, object_id, blocked,
                    review_status, required_level, evaluated_at
                )
                VALUES (:id, 'pol_etl', 'high_risk', 'merchant', 'merchant_etl', TRUE, 'pending', 5, now())
                """, Map.of("id", reviewId));
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("""
                SELECT to_regclass(:qualified_name)::text
                """, Map.of("qualified_name", "public." + tableName), String.class);
    }

    private long rowCount(String tableName) {
        String sql = switch (tableName) {
            case "analytics_metric_definitions" -> """
                    SELECT count(*)
                      FROM analytics_metric_definitions
                    """;
            case "analytics_dashboard_snapshots" -> """
                    SELECT count(*)
                      FROM analytics_dashboard_snapshots
                    """;
            case "analytics_business_metric_snapshots" -> """
                    SELECT count(*)
                      FROM analytics_business_metric_snapshots
                    """;
            case "analytics_merchant_summaries" -> """
                    SELECT count(*)
                      FROM analytics_merchant_summaries
                    """;
            case "analytics_merchant_health_scores" -> """
                    SELECT count(*)
                      FROM analytics_merchant_health_scores
                    """;
            case "analytics_metric_anomalies" -> """
                    SELECT count(*)
                      FROM analytics_metric_anomalies
                    """;
            case "analytics_risk_rules" -> """
                    SELECT count(*)
                      FROM analytics_risk_rules
                    """;
            case "analytics_risk_alerts" -> """
                    SELECT count(*)
                      FROM analytics_risk_alerts
                    """;
            case "analytics_ai_quality_tasks" -> """
                    SELECT count(*)
                      FROM analytics_ai_quality_tasks
                    """;
            case "analytics_ai_quality_results" -> """
                    SELECT count(*)
                      FROM analytics_ai_quality_results
                    """;
            case "analytics_customer_intents" -> """
                    SELECT count(*)
                      FROM analytics_customer_intents
                    """;
            case "analytics_robot_metrics" -> """
                    SELECT count(*)
                      FROM analytics_robot_metrics
                    """;
            case "analytics_staff_service_scores" -> """
                    SELECT count(*)
                      FROM analytics_staff_service_scores
                    """;
            default -> throw new IllegalArgumentException("unsupported analytics table: " + tableName);
        };
        return jdbcTemplate.queryForObject(sql, Map.of(), Long.class);
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM permissions
                 ORDER BY permission_code
                """, Map.of(), String.class);
    }

    private List<String> platformAdminPermissionCodes() {
        return jdbcTemplate.queryForList("""
                SELECT permission_code
                  FROM role_permissions
                 WHERE role_id = 'role_platform_admin'
                 ORDER BY permission_code
                """, Map.of(), String.class);
    }

    private String currentFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version
                  FROM flyway_schema_history
                 WHERE success = TRUE
                 ORDER BY installed_rank DESC
                 LIMIT 1
                """, Map.of(), String.class);
    }
}
