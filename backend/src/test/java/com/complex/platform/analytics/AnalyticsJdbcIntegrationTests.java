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

    @org.springframework.beans.factory.annotation.Autowired
    private com.complex.platform.common.BusinessDayResolver businessDayResolver;

    @Test
    void occupancyAreasHonorDenominatorRulingAndVacantListMatches() {
        // S3 分母口径(拍板):shop+booth 且非 unavailable;车位单列;容器/公共区域不进;
        // pending_effective 算「已签待交」不算已租。
        java.time.LocalDate asOf = java.time.LocalDate.parse("2026-06-20");
        AnalyticsRecords.OccupancyAreas baseline = repository.occupancyAreas(asOf);
        insertSpace("sp_s3_shop_let", "shop", "100");
        insertSpace("sp_s3_shop_vacant", "shop", "60");
        insertSpace("sp_s3_booth", "booth", "40");
        insertSpace("sp_s3_unavailable", "shop", "500");
        jdbcTemplate.update("UPDATE spaces SET status='unavailable' WHERE id='sp_s3_unavailable'", java.util.Map.of());
        insertSpace("sp_s3_public", "public_area", "300");
        insertSpace("sp_s3_parking", "parking", "20");
        insertContractWithSpace("ct_s3_let", "sp_s3_shop_let", "effective", asOf.minusDays(30), asOf.plusDays(60));
        insertContractWithSpace("ct_s3_pending", "sp_s3_booth", "pending_effective", asOf.plusDays(10), asOf.plusDays(370));
        insertContractWithSpace("ct_s3_parking", "sp_s3_parking", "effective", asOf.minusDays(5), asOf.plusDays(30));

        AnalyticsRecords.OccupancyAreas areas = repository.occupancyAreas(asOf);
        // 共享 dev 库有真实铺位(活证据数据保留至首次打包),断言用基线差值口径。
        assertThat(areas.rentableArea().subtract(baseline.rentableArea())).isEqualByComparingTo("200");
        assertThat(areas.leasedArea().subtract(baseline.leasedArea())).isEqualByComparingTo("100");
        assertThat(areas.signedPendingArea().subtract(baseline.signedPendingArea())).isEqualByComparingTo("40");
        assertThat(areas.parkingRentableArea().subtract(baseline.parkingRentableArea())).isEqualByComparingTo("20");
        assertThat(areas.parkingLeasedArea().subtract(baseline.parkingLeasedArea())).isEqualByComparingTo("20");

        java.util.List<AnalyticsRecords.VacantSpaceRow> vacant = repository.listVacantSpaces(asOf, 200, 0).stream()
                .filter(row -> row.spaceId().startsWith("sp_s3_")).toList();
        assertThat(vacant).extracting(AnalyticsRecords.VacantSpaceRow::spaceId)
                .containsExactlyInAnyOrder("sp_s3_shop_vacant", "sp_s3_booth");

        assertThat(repository.countExpiringContracts(asOf, 90)).isGreaterThanOrEqualTo(2);
        java.util.List<AnalyticsRecords.ExpiringContractRow> expiring = repository.listExpiringContracts(asOf, 90, 200, 0).stream()
                .filter(row -> row.contractId().startsWith("ct_s3_")).toList();
        assertThat(expiring).extracting(AnalyticsRecords.ExpiringContractRow::contractId)
                .containsExactly("ct_s3_parking", "ct_s3_let");
        assertThat(expiring.get(1).boundArea()).isEqualByComparingTo("100");
    }

    private void insertSpace(String id, String type, String area) {
        jdbcTemplate.update("""
                INSERT INTO spaces (id, space_name, space_type, floor_no, area_square_meter, polygon_points, status, created_at, updated_at)
                VALUES (:id, :id, :type, '1F', :area, '[]'::jsonb, 'active', now(), now())
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", id).addValue("type", type).addValue("area", new java.math.BigDecimal(area)));
    }

    private void insertContractWithSpace(String contractId, String spaceId, String lifecycle,
            java.time.LocalDate effective, java.time.LocalDate expiry) {
        jdbcTemplate.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id,
                        effective_date, expiry_date, lifecycle_status, created_at, updated_at)
                VALUES (:id, 'mch_s3', 'subj_s3', 'subj_s3', :eff, :exp, :lc, now(), now())
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", contractId).addValue("eff", effective).addValue("exp", expiry).addValue("lc", lifecycle));
        jdbcTemplate.update("INSERT INTO contract_spaces (contract_id, space_id) VALUES (:c, :s)",
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                        .addValue("c", contractId).addValue("s", spaceId));
    }

    @Test
    void arAgingBucketsAndDebtorsReflectRealLedgerRows() {
        // S2 口径:桶按 as-of−due_date;未清=unpaid_amount_cent;Σ桶=欠费总额;明细与快照同过滤。
        java.time.LocalDate asOf = java.time.LocalDate.parse("2026-06-20");
        insertArItem("ar_s2_a", "mch_s2_x", 10000L, 10000L, asOf.minusDays(10));
        insertArItem("ar_s2_b", "mch_s2_x", 20000L, 5000L, asOf.minusDays(45));
        insertArItem("ar_s2_c", "mch_s2_y", 30000L, 30000L, asOf.minusDays(100));
        insertArItem("ar_s2_paidoff", "mch_s2_x", 9000L, 0L, asOf.minusDays(70));
        insertArItem("ar_s2_future", "mch_s2_x", 8000L, 8000L, asOf.plusDays(5));
        // 平台手工录入的应收 merchant_id 为空、商户在 counterparty_id——COALESCE 口径必须归到商户(S2 活证据实测抓出的血缘缺口)。
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, amount_cent, paid_amount_cent,
                        unpaid_amount_cent, currency, due_date, status, created_at, updated_at, merchant_id)
                VALUES ('ar_s2_manual', 'ar_s2_manual', 'receivable', 'mch_s2_x', 4000, 0, 4000, 'CNY',
                        :due, 'open', now(), now(), NULL)
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("due", asOf.minusDays(12)));

        Map<String, Map<String, java.math.BigDecimal>> aging = repository.arOverdueAgingByMerchant(asOf);
        assertThat(aging.get("a0_30").get("mch_s2_x")).isEqualByComparingTo("14000");
        assertThat(aging.get("a31_60").get("mch_s2_x")).isEqualByComparingTo("5000");
        assertThat(aging.get("a90p").get("mch_s2_y")).isEqualByComparingTo("30000");
        assertThat(aging.getOrDefault("a61_90", java.util.Map.of())).doesNotContainKey("mch_s2_x");

        // 共享 dev 库可能有其它会话残留欠费行:只对本用例造的两商户断言(排序=未清金额降序)。
        java.util.List<AnalyticsRecords.ArDebtorRow> debtors = repository.listOverdueDebtors(asOf, null, 200, 0).stream()
                .filter(row -> row.merchantId() != null && row.merchantId().startsWith("mch_s2_")).toList();
        assertThat(debtors).extracting(AnalyticsRecords.ArDebtorRow::merchantId)
                .containsExactly("mch_s2_y", "mch_s2_x");
        assertThat(debtors.get(0).outstandingCent()).isEqualByComparingTo("30000");
        assertThat(debtors.get(1).outstandingCent()).isEqualByComparingTo("19000");
        assertThat(repository.listOverdueDebtors(asOf, "a90p", 200, 0).stream()
                .filter(row -> row.merchantId() != null && row.merchantId().startsWith("mch_s2_")).toList())
                .extracting(AnalyticsRecords.ArDebtorRow::merchantId).containsExactly("mch_s2_y");
    }

    @Test
    void leasingFunnelAndRentToSalesMetricsAreEmittedAtPlatformScope() {
        // V220：招商漏斗 + 租售比 ETL。2099 窗隔离共享库。
        java.time.LocalDate businessDate = java.time.LocalDate.parse("2099-03-10");
        Instant at = Instant.parse("2099-03-10T05:00:00Z");
        // 5 条线索(created_at 落窗)：2 pending / 1 converted_to_intent / 1 negotiating / 1 signed。
        insertLeasingLead("lead_v220_1", "pending_follow_up", at);
        insertLeasingLead("lead_v220_2", "following_up", at);
        insertLeasingLead("lead_v220_3", "converted_to_intent", at);
        insertLeasingLead("lead_v220_4", "negotiating", at);
        insertLeasingLead("lead_v220_5", "signed", at);
        // 合同租金应收 1000.00 元(source_type=contract_rent, due=当日) → 租售比分子。
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, source_type, counterparty_id, amount_cent,
                        paid_amount_cent, unpaid_amount_cent, currency, due_date, status, created_at, updated_at, merchant_id)
                VALUES ('ar_v220_rent', 'ar_v220_rent', 'receivable', 'contract_rent', 'mch_v220', 100000, 0, 100000,
                        'CNY', :due, 'open', :at, :at, 'mch_v220')
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("due", businessDate).addValue("at", java.sql.Timestamp.from(at)));
        // 第三方回流营业额 5000.00 元(synced order) → 租售比分母。
        jdbcTemplate.update("""
                INSERT INTO integration_order_syncs (id, provider_code, business_type, sync_type, channel,
                        external_business_id, status, amount_cent, currency, created_at, updated_at)
                VALUES ('ios_v220', 'meituan', 'catering', 'order', 'meituan', 'ext_v220', 'synced', 500000, 'CNY', :at, :at)
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("at", java.sql.Timestamp.from(at)));

        var job = new DashboardAggregationJob(repository,
                java.time.Clock.fixed(at, java.time.ZoneOffset.UTC));
        job.refreshSnapshots(businessDate);

        java.util.Map<String, java.math.BigDecimal> platform = repository.readMetricSnapshots(businessDate, null);
        assertThat(platform.get("leasing.lead_count")).isEqualByComparingTo("5");
        // 转意向率 = (converted+negotiating+signed)/total = 3/5 = 0.6。
        assertThat(platform.get("leasing.intent_conversion_rate")).isEqualByComparingTo("0.6000");
        // 意向转签约率 = signed/intent = 1/3 ≈ 0.3333。
        assertThat(platform.get("leasing.signed_conversion_rate")).isEqualByComparingTo("0.3333");
        // 租售比 = 合同租金 100000 / 第三方营业额 500000 = 0.2。
        assertThat(platform.get("leasing.rent_to_sales")).isEqualByComparingTo("0.2000");
    }

    private void insertLeasingLead(String id, String status, Instant createdAt) {
        jdbcTemplate.update("""
                INSERT INTO leasing_leads (id, contact_name, source, status, created_at, updated_at)
                VALUES (:id, :id, 'outbound', :status, :at, :at)
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", id).addValue("status", status)
                .addValue("at", java.sql.Timestamp.from(createdAt)));
    }

    private void insertArItem(String id, String merchantId, long amountCent, long unpaidCent, java.time.LocalDate dueDate) {
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, amount_cent, paid_amount_cent,
                        unpaid_amount_cent, currency, due_date, status, created_at, updated_at, merchant_id)
                VALUES (:id, :id, 'receivable', :mid, :amount, :paid, :unpaid, 'CNY', :due,
                        CASE WHEN :unpaid = 0 THEN 'closed' ELSE 'open' END, now(), now(), :mid)
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", id).addValue("mid", merchantId)
                .addValue("amount", amountCent).addValue("paid", amountCent - unpaidCent)
                .addValue("unpaid", unpaidCent).addValue("due", dueDate));
    }

    @Test
    void workOrderSlaHonorsWindowAndOverdueDrilldownMatches() {
        // S4(真库):P50 只算落窗、超时含"完成晚于时限"与"未完成且时限已过"、明细与分子同过滤。2099 窗隔离共享库。
        java.time.Instant start = java.time.Instant.parse("2099-01-01T04:00:00Z");
        java.time.Instant end = java.time.Instant.parse("2099-01-02T04:00:00Z");
        java.time.Instant asOf = java.time.Instant.parse("2099-01-02T00:00:00Z");
        // A 按时:响应1h、处理2h、时限内完成。
        insertWorkOrder("wo_s4_a", "2099-01-01T05:00:00Z", "2099-01-01T05:30:00Z", "2099-01-01T06:00:00Z",
                "2099-01-01T10:00:00Z", "2099-01-01T07:30:00Z", "accepted");
        // B 超时完成:响应4h、处理7h、完成晚于时限6h。
        insertWorkOrder("wo_s4_b", "2099-01-01T05:00:00Z", "2099-01-01T05:00:00Z", "2099-01-01T09:00:00Z",
                "2099-01-01T06:00:00Z", "2099-01-01T12:00:00Z", "closed");
        // C 超时未完成:时限已过 17h 仍未完成(积压)。
        insertWorkOrder("wo_s4_c", "2099-01-01T05:00:00Z", null, null,
                "2099-01-01T07:00:00Z", null, "processing");

        AnalyticsRecords.WorkOrderSla sla = repository.workOrderSla(start, end, asOf);
        assertThat(sla.responseP50Hours()).isEqualByComparingTo("2.5");   // (1h+4h)/2
        assertThat(sla.handleP50Hours()).isEqualByComparingTo("4.5");     // (2h+7h)/2
        assertThat(sla.dueCount()).isEqualTo(3);
        assertThat(sla.overdueCount()).isEqualTo(2);
        assertThat(sla.backlog()).isGreaterThanOrEqualTo(1);              // 共享库允许其它存量

        java.util.List<AnalyticsRecords.OverdueWorkOrderRow> rows =
                repository.listOverdueWorkOrders(start, end, asOf, 200, 0);
        java.util.List<String> ids = rows.stream().map(AnalyticsRecords.OverdueWorkOrderRow::workOrderId).toList();
        assertThat(ids).contains("wo_s4_b", "wo_s4_c").doesNotContain("wo_s4_a");
        AnalyticsRecords.OverdueWorkOrderRow b = rows.stream()
                .filter(r -> r.workOrderId().equals("wo_s4_b")).findFirst().orElseThrow();
        assertThat(b.overdueHours()).isEqualByComparingTo("6.0");
        AnalyticsRecords.OverdueWorkOrderRow c = rows.stream()
                .filter(r -> r.workOrderId().equals("wo_s4_c")).findFirst().orElseThrow();
        assertThat(c.overdueHours()).isEqualByComparingTo("17.0");        // asOf-时限,未完成按当下计
    }

    @Test
    void merchantWorkOrderSlaUsesClosedAtAndSlaBreachedRulings() {
        // 清单#13(真库):商户单无响应节点(responseP50 恒 null);处理=提交→关单;
        // 超时=sla_breached(在办期间过限)或关单晚于时限或未完成且时限已过;cancelled 不进分母;
        // resolved 未过限(sla_breached=false)不算超时(表无 resolved 时点,以升级作业标记为准)。
        java.time.Instant start = java.time.Instant.parse("2099-01-01T04:00:00Z");
        java.time.Instant end = java.time.Instant.parse("2099-01-02T04:00:00Z");
        java.time.Instant asOf = java.time.Instant.parse("2099-01-02T00:00:00Z");
        // A 按时关单:2h。
        insertMerchantWorkOrder("mwo_s4_a", "2099-01-01T05:00:00Z", "2099-01-01T10:00:00Z",
                "2099-01-01T07:00:00Z", "closed", false);
        // B 超时关单:7h,晚于时限 6h(升级作业已标记)。
        insertMerchantWorkOrder("mwo_s4_b", "2099-01-01T05:00:00Z", "2099-01-01T06:00:00Z",
                "2099-01-01T12:00:00Z", "closed", true);
        // C 超时未完成:时限已过 17h 仍在办(实时兜底,升级作业未及标记)。
        insertMerchantWorkOrder("mwo_s4_c", "2099-01-01T05:00:00Z", "2099-01-01T07:00:00Z",
                null, "processing", false);
        // D 已取消:不进分母分子。
        insertMerchantWorkOrder("mwo_s4_d", "2099-01-01T05:00:00Z", "2099-01-01T08:00:00Z",
                null, "cancelled", false);
        // E 已处理待关闭且未过限:进分母不算超时。
        insertMerchantWorkOrder("mwo_s4_e", "2099-01-01T05:00:00Z", "2099-01-01T09:00:00Z",
                null, "resolved", false);

        AnalyticsRecords.WorkOrderSla sla = repository.merchantWorkOrderSla(start, end, asOf);
        assertThat(sla.responseP50Hours()).isNull();
        assertThat(sla.handleP50Hours()).isEqualByComparingTo("4.5");   // (2h+7h)/2
        assertThat(sla.dueCount()).isEqualTo(4);                        // A/B/C/E(D cancelled 除外)
        assertThat(sla.overdueCount()).isEqualTo(2);                    // B/C
        assertThat(sla.backlog()).isGreaterThanOrEqualTo(1);            // C(共享库允许其它存量)

        java.util.List<AnalyticsRecords.OverdueWorkOrderRow> rows =
                repository.listOverdueMerchantWorkOrders(start, end, asOf, 200, 0);
        java.util.List<String> ids = rows.stream().map(AnalyticsRecords.OverdueWorkOrderRow::workOrderId).toList();
        assertThat(ids).contains("mwo_s4_b", "mwo_s4_c").doesNotContain("mwo_s4_a", "mwo_s4_d", "mwo_s4_e");
        AnalyticsRecords.OverdueWorkOrderRow b = rows.stream()
                .filter(r -> r.workOrderId().equals("mwo_s4_b")).findFirst().orElseThrow();
        assertThat(b.overdueHours()).isEqualByComparingTo("6.0");
        AnalyticsRecords.OverdueWorkOrderRow c = rows.stream()
                .filter(r -> r.workOrderId().equals("mwo_s4_c")).findFirst().orElseThrow();
        assertThat(c.overdueHours()).isEqualByComparingTo("17.0");
    }

    @Test
    void energyConsumptionByTypeHonorsWindowAndAbnormalDrilldownMatches() {
        // S5(真库):按表计类型分列合计、窗外读数不进、异常明细与计数同过滤。
        insertMeter("mt_s5_e", "S5电表", "electricity");
        insertMeter("mt_s5_w", "S5水表", "water");
        insertReading("rd_s5_e1", "mt_s5_e", "2099-01-01T08:00:00Z", "60.5", null, "2099-01");
        insertReading("rd_s5_e2", "mt_s5_e", "2099-01-01T20:00:00Z", "40", "surge", "2099-02");
        insertReading("rd_s5_w1", "mt_s5_w", "2099-01-01T09:00:00Z", "8", null, "2099-01");
        insertReading("rd_s5_out", "mt_s5_e", "2098-12-31T08:00:00Z", "999", null, "2098-12");

        java.time.Instant start = java.time.Instant.parse("2099-01-01T04:00:00Z");
        java.time.Instant end = java.time.Instant.parse("2099-01-02T04:00:00Z");
        java.util.Map<String, java.math.BigDecimal> byType = repository.energyConsumptionByType(start, end);
        assertThat(byType.get("electricity")).isEqualByComparingTo("100.5");
        assertThat(byType.get("water")).isEqualByComparingTo("8");
        assertThat(byType).doesNotContainKey("gas");

        assertThat(repository.countAbnormalReadings(start, end)).isEqualTo(1);
        java.util.List<AnalyticsRecords.AbnormalReadingRow> rows =
                repository.listAbnormalReadings(start, end, 200, 0);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).readingId()).isEqualTo("rd_s5_e2");
        assertThat(rows.get(0).meterType()).isEqualTo("electricity");
        assertThat(rows.get(0).abnormalReason()).isEqualTo("surge");
    }

    private void insertWorkOrder(String id, String createdAt, String assignedAt, String respondedAt,
            String handleDueAt, String completedAt, String status) {
        jdbcTemplate.update("""
                INSERT INTO facility_work_orders (id, code, work_order_type, priority, status, title, source,
                        created_at, updated_at, assigned_at, responded_at, handle_due_at, completed_at)
                VALUES (:id, :id, 'repair', 'normal', :status, :id, 'platform',
                        :created_at::timestamptz, :created_at::timestamptz, :assigned_at::timestamptz,
                        :responded_at::timestamptz, :handle_due_at::timestamptz, :completed_at::timestamptz)
                """, new java.util.HashMap<String, Object>() {{
                    put("id", id); put("status", status); put("created_at", createdAt);
                    put("assigned_at", assignedAt); put("responded_at", respondedAt);
                    put("handle_due_at", handleDueAt); put("completed_at", completedAt);
                }});
    }

    private void insertMerchantWorkOrder(String id, String createdAt, String dueAt,
            String closedAt, String status, boolean slaBreached) {
        jdbcTemplate.update("""
                INSERT INTO merchant_service_work_orders (id, merchant_id, work_order_type, priority, status,
                        title, description, reported_by_user_id, reported_channel, evidence_attachment_ids,
                        due_at, closed_at, created_at, updated_at, sla_breached)
                VALUES (:id, 'mch_s4m', 'repair', 'medium', :status,
                        :id, :id, 'usr_s4m', 'web', '[]'::jsonb,
                        :due_at::timestamptz, :closed_at::timestamptz, :created_at::timestamptz,
                        :created_at::timestamptz, :sla_breached)
                """, new java.util.HashMap<String, Object>() {{
                    put("id", id); put("status", status); put("created_at", createdAt);
                    put("due_at", dueAt); put("closed_at", closedAt); put("sla_breached", slaBreached);
                }});
    }

    private void insertMeter(String id, String name, String meterType) {
        jdbcTemplate.update("""
                INSERT INTO energy_meters (id, name, meter_type, gather_object, read_mode)
                VALUES (:id, :name, :meter_type, 'public_area', 'manual')
                """, java.util.Map.of("id", id, "name", name, "meter_type", meterType));
    }

    private void insertReading(String id, String meterId, String readAt, String consumption, String abnormalReason, String period) {
        jdbcTemplate.update("""
                INSERT INTO energy_meter_readings (id, meter_id, period, reading_value, prev_reading_value,
                        consumption, status, abnormal_reason, read_mode, read_at)
                VALUES (:id, :meter_id, :period, :consumption::numeric, 0, :consumption::numeric,
                        'recorded', :abnormal_reason, 'manual', :read_at::timestamptz)
                """, new java.util.HashMap<String, Object>() {{
                    put("id", id); put("meter_id", meterId); put("consumption", consumption);
                    put("abnormal_reason", abnormalReason); put("read_at", readAt);
                    put("period", period);
                }});
    }

    @Test
    void v209SeedsHolidaysAndRangeAggregationFollowsMixedRuling() {
        // H1:V209 种子 2025 法定假期;区间聚合=流量累计+存量区间末;isWithinHoliday 免报判定;upsert 幂等。
        java.util.List<AnalyticsRecords.HolidayPeriod> seeded = repository.listHolidayPeriods(2025);
        assertThat(seeded).extracting(AnalyticsRecords.HolidayPeriod::holidayName)
                .contains("春节", "劳动节", "国庆");
        AnalyticsRecords.HolidayPeriod spring = seeded.stream()
                .filter(h -> h.holidayName().equals("春节")).findFirst().orElseThrow();
        assertThat(spring.startDate()).isEqualTo(java.time.LocalDate.parse("2025-01-28"));
        assertThat(spring.endDate()).isEqualTo(java.time.LocalDate.parse("2025-02-04"));

        assertThat(repository.isWithinHoliday(java.time.LocalDate.parse("2025-02-01"))).isTrue();
        assertThat(repository.isWithinHoliday(java.time.LocalDate.parse("2025-03-15"))).isFalse();

        repository.upsertHolidayPeriod("测试假", 2099,
                java.time.LocalDate.parse("2099-07-01"), java.time.LocalDate.parse("2099-07-03"), "manual",
                java.time.Instant.parse("2099-06-01T00:00:00Z"));
        repository.upsertHolidayPeriod("测试假", 2099,
                java.time.LocalDate.parse("2099-07-01"), java.time.LocalDate.parse("2099-07-04"), "manual",
                java.time.Instant.parse("2099-06-02T00:00:00Z"));
        java.util.List<AnalyticsRecords.HolidayPeriod> test = repository.listHolidayPeriods(2099);
        assertThat(test).hasSize(1);
        assertThat(test.get(0).endDate()).isEqualTo(java.time.LocalDate.parse("2099-07-04"));

        java.time.Instant now = java.time.Instant.parse("2099-07-05T00:00:00Z");
        upsertDaily("revenue.net", "finance", "2099-07-01", "100", now);
        upsertDaily("revenue.net", "finance", "2099-07-02", "200", now);
        upsertDaily("revenue.net", "finance", "2099-07-03", "300", now);
        upsertDaily("ar.balance", "finance", "2099-07-01", "500", now);
        upsertDaily("ar.balance", "finance", "2099-07-03", "700", now);
        Map<String, java.math.BigDecimal> agg = repository.aggregateRange(
                java.time.LocalDate.parse("2099-07-01"), java.time.LocalDate.parse("2099-07-03"),
                java.util.List.of("revenue.net"), java.util.List.of("ar.balance"));
        assertThat(agg.get("revenue.net")).isEqualByComparingTo("600");   // 区间累计
        assertThat(agg.get("ar.balance")).isEqualByComparingTo("700");    // 区间末
    }

    @Test
    void merchantSummariesRollupAggregatesRealSources() {
        // M4(真库):汇总=当日商户维净收入快照+当日核销量+open风险告警数+最新账单状态;V208 演示种子已清。
        assertThat(countRows("SELECT COUNT(*) FROM analytics_merchant_summaries WHERE merchant_id='merchant_p0'")).isZero();

        java.time.Instant now = java.time.Instant.parse("2099-06-01T12:00:00Z");
        java.time.LocalDate day = java.time.LocalDate.parse("2099-06-01");
        jdbcTemplate.update("""
                INSERT INTO business_entities (id, entity_name, entity_role, unified_social_credit_code,
                        legal_representative, bank_account_ref, status, created_at, updated_at)
                VALUES ('org_m4', 'M4主体', 'operation', 'm4-code', 'M4', 'vault://m4', 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """, java.util.Map.of());
        jdbcTemplate.update("""
                INSERT INTO merchants (id, business_entity_id, merchant_name, merchant_type, contact_name,
                        contact_phone, status, created_at, updated_at)
                VALUES ('mch_m4', 'org_m4', 'M4汇总商户', 'other', 'M4', 'enc:m4', 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """, java.util.Map.of());
        repository.upsertMetricSnapshot(new AnalyticsRecords.MetricSnapshotKey(
                "revenue.net", "finance", "daily", day, "merchant", "mch_m4"),
                new java.math.BigDecimal("88800"), "etl_aggregate", now);
        repository.upsertMetricSnapshot(new AnalyticsRecords.MetricSnapshotKey(
                "verify.done", "ticketing", "daily", day, "merchant", "mch_m4"),
                new java.math.BigDecimal("7"), "etl_aggregate", now);
        jdbcTemplate.update("""
                INSERT INTO risk_alerts (alert_id, source_domain, alert_type, risk_level, status, summary,
                        first_reported_by, merchant_id, created_at, updated_at)
                VALUES ('risk_m4', 'analytics', 'customer_risk', 'high', 'open', 'm4', 'tester', 'mch_m4', now(), now())
                """, java.util.Map.of());

        repository.refreshMerchantSummaries(day, now);

        java.util.Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT merchant_name, revenue_amount_cents, order_count, risk_alert_count, settlement_status "
                        + "FROM analytics_merchant_summaries WHERE merchant_id='mch_m4'", java.util.Map.of());
        assertThat(row.get("merchant_name")).isEqualTo("M4汇总商户");
        assertThat(((Number) row.get("revenue_amount_cents")).longValue()).isEqualTo(88800L);
        assertThat(((Number) row.get("order_count")).intValue()).isEqualTo(7);
        assertThat(((Number) row.get("risk_alert_count")).intValue()).isEqualTo(1);
        assertThat(row.get("settlement_status")).isEqualTo("none");

        // 幂等重跑:仍单行,值更新不重复建。
        repository.refreshMerchantSummaries(day, now);
        assertThat(countRows("SELECT COUNT(*) FROM analytics_merchant_summaries WHERE merchant_id='mch_m4'")).isEqualTo(1);
    }

    @Test
    void v207CleansDemoSeedsAndAnomalyPipelineIsIdempotent() {
        // M3:V207 清定义/异常演示种子(V017 冒真);定义同步 upsert;异常按 id 幂等;阈值配置往返可调。
        assertThat(countRows("SELECT COUNT(*) FROM analytics_metric_anomalies WHERE id='anomaly_p0'")).isZero();
        assertThat(countRows("SELECT COUNT(*) FROM analytics_metric_definitions WHERE metric_key='gmv_amount_cents'")).isZero();

        AnalyticsRecords.AnomalyConfig seeded = repository.readAnomalyConfig();
        assertThat(seeded.surgePct()).isEqualByComparingTo("0.5");
        assertThat(seeded.highPct()).isEqualByComparingTo("0.8");
        assertThat(seeded.enabled()).isTrue();

        java.time.Instant now = java.time.Instant.parse("2099-05-01T00:00:00Z");
        repository.syncMetricDefinitions(java.util.List.of(
                new AnalyticsRecords.MetricDefinitionSyncRow("m3.test_key", "finance", "cent", true, "note-1")), now);
        repository.syncMetricDefinitions(java.util.List.of(
                new AnalyticsRecords.MetricDefinitionSyncRow("m3.test_key", "finance", "cent", true, "note-2")), now);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT calculation_note FROM analytics_metric_definitions WHERE metric_key='m3.test_key'",
                java.util.Map.of(), String.class)).isEqualTo("note-2");

        repository.insertMetricAnomaly("ano_m3_test", "m3.test_key", "finance", "suspicious",
                new java.math.BigDecimal("150"), new java.math.BigDecimal("100"), "s", now);
        repository.insertMetricAnomaly("ano_m3_test", "m3.test_key", "finance", "high",
                new java.math.BigDecimal("150"), new java.math.BigDecimal("100"), "s", now);
        assertThat(countRows("SELECT COUNT(*) FROM analytics_metric_anomalies WHERE id='ano_m3_test'")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT risk_level FROM analytics_metric_anomalies WHERE id='ano_m3_test'",
                java.util.Map.of(), String.class)).isEqualTo("suspicious");

        AnalyticsRecords.AnomalyConfig updated = repository.upsertAnomalyConfig(
                new java.math.BigDecimal("0.6"), new java.math.BigDecimal("0.9"), true, "admin", now);
        assertThat(updated.surgePct()).isEqualByComparingTo("0.6");
        assertThat(repository.readAnomalyConfig().highPct()).isEqualByComparingTo("0.9");
    }

    private long countRows(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, java.util.Map.of(), Long.class);
        return value == null ? 0 : value;
    }

    @Test
    void v206SeedsReportExportPermissionWithSeparationOfDuty() {
        // M2(V196 范式):报表导出与查看分权——独立高危权限点,平台管理员直授,审计角色不得直授。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM permissions WHERE permission_code='analytics.report.export' AND risk_level='high'",
                java.util.Map.of(), Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role_permissions WHERE permission_code='analytics.report.export' AND role_id='role_platform_admin'",
                java.util.Map.of(), Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM role_permissions WHERE permission_code='analytics.report.export' AND role_id LIKE 'role_auditor%'",
                java.util.Map.of(), Integer.class)).isEqualTo(0);
    }

    @Test
    void monthlyRollupSumsFlowsTakesPeriodEndForStocksAndIsIdempotent() {
        // M1(拍板④混合口径):流量类=月累计 SUM,存量类=期末时点 LAST;滚动重跑幂等。2099-04 隔离共享库。
        java.time.Instant now = java.time.Instant.parse("2099-04-02T10:00:00Z");
        upsertDaily("revenue.net", "finance", "2099-04-01", "100", now);
        upsertDaily("revenue.net", "finance", "2099-04-02", "200", now);
        upsertDaily("ar.balance", "finance", "2099-04-01", "500", now);
        upsertDaily("ar.balance", "finance", "2099-04-02", "700", now);
        upsertDaily("wo.overdue_rate", "facility", "2099-04-02", "0.5", now);

        java.time.LocalDate monthFirst = java.time.LocalDate.parse("2099-04-01");
        repository.rollupMonthlySnapshots(monthFirst, java.time.LocalDate.parse("2099-04-30"),
                java.util.List.of("revenue.net"), java.util.List.of("ar.balance"), now);

        java.util.Map<String, java.math.BigDecimal> monthly = repository.readMonthlySnapshots(monthFirst);
        assertThat(monthly.get("revenue.net")).isEqualByComparingTo("300");   // SUM
        assertThat(monthly.get("ar.balance")).isEqualByComparingTo("700");    // 期末=最后经营日值
        assertThat(monthly).doesNotContainKey("wo.overdue_rate");             // 白名单外不产出

        // 滚动:再来一天,重跑 rollup 覆盖更新(幂等,不重复建行)。
        upsertDaily("revenue.net", "finance", "2099-04-03", "50", now);
        repository.rollupMonthlySnapshots(monthFirst, java.time.LocalDate.parse("2099-04-30"),
                java.util.List.of("revenue.net"), java.util.List.of("ar.balance"), now);
        java.util.Map<String, java.math.BigDecimal> again = repository.readMonthlySnapshots(monthFirst);
        assertThat(again.get("revenue.net")).isEqualByComparingTo("350");
        Integer rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM analytics_business_metric_snapshots "
                        + "WHERE period_type='monthly' AND business_date='2099-04-01' AND metric_key='revenue.net'",
                java.util.Map.of(), Integer.class);
        assertThat(rowCount).isEqualTo(1);
    }

    private void upsertDaily(String metricKey, String domain, String businessDate, String value, java.time.Instant now) {
        repository.upsertMetricSnapshot(new AnalyticsRecords.MetricSnapshotKey(
                metricKey, domain, "daily", java.time.LocalDate.parse(businessDate), "platform", null),
                new java.math.BigDecimal(value), "etl_aggregate", now);
    }

    @Test
    void countNewMembersExcludesDeregisteredAndBlacklisted() {
        // S7(拍板⑨):member.new 排除已注销/黑名单——新增会员是经营口径,不该把已出局的算进来。
        java.time.LocalDate day = java.time.LocalDate.parse("2099-03-01");
        java.math.BigDecimal baseline = repository.countNewMembers(day);
        insertCustomer("cus_s7_active", "active", "2099-03-01T12:00:00Z");
        insertCustomer("cus_s7_dereg", "deregistered", "2099-03-01T12:00:00Z");
        insertCustomer("cus_s7_black", "blacklisted", "2099-03-01T12:00:00Z");
        assertThat(repository.countNewMembers(day).subtract(baseline)).isEqualByComparingTo("1");
    }

    private void insertCustomer(String id, String status, String createdAt) {
        jdbcTemplate.update("""
                INSERT INTO customers (id, phone, masked_phone, status, created_at, updated_at)
                VALUES (:id, :id, '138****0000', :status, :created_at::timestamptz, :created_at::timestamptz)
                """, new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("id", id).addValue("status", status).addValue("created_at", createdAt));
    }

    @Test
    void v201SeedsDomainCutoffsAndResolverAppliesBarWindow() {
        // S0(2026-07-02 拍板):经营日按域切分——bar=06:00、merchant=10:00、platform=04:00 基准。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT cutoff_hour FROM business_day_cutoff_config WHERE domain='bar'", java.util.Map.of(), Integer.class)).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT cutoff_hour FROM business_day_cutoff_config WHERE domain='merchant'", java.util.Map.of(), Integer.class)).isEqualTo(10);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT cutoff_hour FROM business_day_cutoff_config WHERE domain='platform'", java.util.Map.of(), Integer.class)).isEqualTo(4);
        // 凌晨 05:00 的流水:酒吧域归前一经营日(06:00 切),平台基准归当日(04:00 切)。
        java.time.Instant fiveAm = java.time.Instant.parse("2026-06-18T05:00:00Z");
        assertThat(businessDayResolver.businessDate("bar", fiveAm)).isEqualTo(java.time.LocalDate.parse("2026-06-17"));
        assertThat(businessDayResolver.businessDate(fiveAm)).isEqualTo(java.time.LocalDate.parse("2026-06-18"));
    }

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
        // M4(V208):商户汇总/健康分演示种子已清——汇总由 ETL 真管道回填,健康分按裁决未启用。
        // M3(V207):异常演示种子已清,真实异常由日环比检测写入,不再断言演示行。
        assertThat(rowCount("analytics_risk_rules")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_risk_alerts")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_ai_quality_tasks")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_ai_quality_results")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_customer_intents")).isGreaterThanOrEqualTo(1);
        assertThat(rowCount("analytics_robot_metrics")).isGreaterThanOrEqualTo(1);
        // M4(V208):服务分演示种子已清(按 2026-07-02 裁决未启用,读端点返回真实空集)。
    }

    @Test
    void repositoryListsSeededAnalyticsReadModels() {
        // M3(V207):gmv 演示种子已清;定义表由代码目录同步,这里验证同步行可被读端点读回。
        repository.syncMetricDefinitions(java.util.List.of(
                new AnalyticsRecords.MetricDefinitionSyncRow("revenue.net", "finance", "cent", true, "m3")),
                java.time.Instant.parse("2099-05-01T00:00:00Z"));
        assertThat(repository.listMetricDefinitions())
                .extracting(AnalyticsRecords.MetricDefinitionRecord::metricKey, AnalyticsRecords.MetricDefinitionRecord::unit)
                .contains(tuple("revenue.net", "cent"));
        assertThat(repository.getP0OverviewDashboard().snapshotKey()).isEqualTo("p0_overview_today");
        assertThat(repository.listBusinessMetricSnapshots())
                .extracting(AnalyticsRecords.BusinessMetricSnapshotRecord::metricKey, AnalyticsRecords.BusinessMetricSnapshotRecord::trendDirection)
                .contains(tuple("gmv_amount_cents", "up"));
        // M4(V208):merchant_p0 演示行已清;汇总行由 refreshMerchantSummaries 真管道回填(单测见 M4 用例)。
        assertThat(repository.listAnalyticsMerchants())
                .extracting(AnalyticsRecords.MerchantSummaryRecord::merchantId)
                .doesNotContain("merchant_p0");
        // 健康分按 2026-07-02 裁决未启用,演示行清空后读端点返回真实空集。
        assertThat(repository.listMerchantHealthScores())
                .extracting(AnalyticsRecords.MerchantHealthScoreRecord::merchantId)
                .doesNotContain("merchant_p0");
        // M3(V207):异常演示种子已清;这里验证真管道写入的异常行可被读端点读回。
        repository.insertMetricAnomaly("ano_m3_read_test", "revenue.net", "finance", "suspicious",
                new java.math.BigDecimal("150"), new java.math.BigDecimal("100"), "m3",
                java.time.Instant.parse("2099-05-01T00:00:00Z"));
        assertThat(repository.listMetricAnomalies())
                .extracting(AnalyticsRecords.MetricAnomalyRecord::metricKey, AnalyticsRecords.MetricAnomalyRecord::riskLevel)
                .contains(tuple("revenue.net", "suspicious"));
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
        // M4(V208):服务分演示行已清(裁决未启用),读端点不再返回演示数据。
        assertThat(repository.listStaffServiceScores())
                .extracting(AnalyticsRecords.StaffServiceScoreRecord::scoreId)
                .doesNotContain("staff_score_p0");
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

        Map<String, java.math.BigDecimal> barShare = repository.sumTicketBarShareCentsByMerchant(businessDate, "ticketing");
        Map<String, java.math.BigDecimal> amuShare = repository.sumTicketAmusementShareCentsByMerchant(businessDate, "ticketing");

        assertThat(barShare.get("mch_bar")).isEqualByComparingTo("480");
        assertThat(barShare).doesNotContainKey("mch_amu");
        assertThat(amuShare.get("mch_amu")).isEqualByComparingTo("320");
        assertThat(amuShare).doesNotContainKey("mch_bar");
    }

    @Test
    void ticketAllocatedRevenueByMerchantAndRefundAttributedToSalesMerchant() {
        // 清单#4 + V220：售票分量按商户(allocated)；售票退款直挂原单销售商户 merchant_id（不再按 allocated 占比分摊）。
        java.time.LocalDate businessDate = java.time.LocalDate.parse("2026-06-16");
        // 订单 ord_net 实付 1000，发两张权益：mch_a 分摊 600 / mch_b 分摊 400（allocated 分量口径不变）。
        insertTicketOrder("ord_net", 1000L, businessDate);
        insertIssuedEntitlementForOrder("ent_net_a", "ord_net", "drink", "mch_a", 600L, businessDate);
        insertIssuedEntitlementForOrder("ent_net_b", "ord_net", "amusement", "mch_b", 400L, businessDate);

        Map<String, java.math.BigDecimal> allocated = repository.sumTicketAllocatedRevenueByMerchant(businessDate, "platform");
        assertThat(allocated.get("mch_a")).isEqualByComparingTo("600");
        assertThat(allocated.get("mch_b")).isEqualByComparingTo("400");

        // V220：退款 300 直挂受理销售商户 mch_a（=原单 merchant_id），整额归 mch_a，不再拆分到 mch_b。
        insertTicketRefund("rf_net", "ord_net", 300L, "mch_a", businessDate);
        Map<String, java.math.BigDecimal> refundShare = repository.sumTicketRefundByMerchant(businessDate, "platform");
        assertThat(refundShare.get("mch_a")).isEqualByComparingTo("300");
        assertThat(refundShare).doesNotContainKey("mch_b");
    }

    @Test
    void ticketRefundForOrderWithoutAllocatedEntitlementsIsNotAttributedToAnyMerchant() {
        // 清单#4：订单无 allocated 权益（direct 出票/历史）→ 该退款保守不归任何商户。
        java.time.LocalDate businessDate = java.time.LocalDate.parse("2026-06-15");
        insertTicketOrder("ord_noalloc", 500L, businessDate);
        insertIssuedEntitlementForOrder("ent_noalloc", "ord_noalloc", "entry", "mch_x", null, businessDate);
        insertTicketRefund("rf_noalloc", "ord_noalloc", 200L, null, businessDate);

        Map<String, java.math.BigDecimal> refundShare = repository.sumTicketRefundByMerchant(businessDate, "platform");
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
        insertTicketOrder(id, allocatedRevenueCent == null ? 0L : allocatedRevenueCent, businessDate);
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

    private void insertTicketRefund(String id, String targetOrderId, long refundAmountCent,
                                    String merchantId, java.time.LocalDate businessDate) {
        Instant at = businessDate.atTime(11, 0).toInstant(java.time.ZoneOffset.UTC);
        var params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("target", targetOrderId)
                .addValue("amount", refundAmountCent)
                .addValue("merchant_id", merchantId)
                .addValue("at", java.sql.Timestamp.from(at));
        jdbcTemplate.update("""
                INSERT INTO ticket_refunds (id, target_id, refund_amount_cent, reason, refund_channel, status,
                    created_at, updated_at, merchant_id)
                VALUES (:id, :target, :amount, 'test refund', 'original', 'approved', :at, :at, :merchant_id)
                """, params);
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

    @Test
    void reviewLockPersistsAndIsProjectScoped() {
        // V216: 锁定复盘真落库 + 项目级隔离(projectScope=null 见全量;非空仅本项目)。
        String suffix = Long.toString(System.nanoTime());
        AnalyticsRecords.BusinessReviewLockRecord platformLock = repository.saveReviewLock(
                new AnalyticsRecords.BusinessReviewLockRecord(
                        "brl_platform_" + suffix, null, "all", "总控周会复盘锁定",
                        42, "user_platform_admin", Instant.now(), "idem-platform-" + suffix));
        AnalyticsRecords.BusinessReviewLockRecord projX = repository.saveReviewLock(
                new AnalyticsRecords.BusinessReviewLockRecord(
                        "brl_x_" + suffix, "proj_x_" + suffix, "revenue", "X项目复盘",
                        7, "user_x", Instant.now(), "idem-x-" + suffix));
        AnalyticsRecords.BusinessReviewLockRecord projY = repository.saveReviewLock(
                new AnalyticsRecords.BusinessReviewLockRecord(
                        "brl_y_" + suffix, "proj_y_" + suffix, "bar", "Y项目复盘",
                        3, "user_y", Instant.now(), "idem-y-" + suffix));

        // 平台 scope(null)=全量：三条都可见。
        List<String> platformIds = repository.listReviewLocks(null).stream()
                .map(AnalyticsRecords.BusinessReviewLockRecord::id).toList();
        assertThat(platformIds).contains(platformLock.id(), projX.id(), projY.id());

        // 项目 X scope：只见本项目 X，绝不见 Y，也不见平台 NULL 行(与 V214 staff 同口径)。
        List<AnalyticsRecords.BusinessReviewLockRecord> xScoped = repository.listReviewLocks("proj_x_" + suffix);
        assertThat(xScoped).extracting(AnalyticsRecords.BusinessReviewLockRecord::id)
                .containsExactly(projX.id());
        assertThat(xScoped).extracting(AnalyticsRecords.BusinessReviewLockRecord::id)
                .doesNotContain(projY.id(), platformLock.id());
        // 落库字段核验。
        assertThat(xScoped.get(0).operatorNote()).isEqualTo("X项目复盘");
        assertThat(xScoped.get(0).metricCount()).isEqualTo(7);
        assertThat(xScoped.get(0).lockedBy()).isEqualTo("user_x");
    }

    @Test
    void reviewLockPermissionSeededToPlatformAdmin() {
        assertThat(platformAdminPermissionCodes()).contains("analytics.review.lock");
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

    // 负向越权：analytics_business_metric_snapshots 的多态维度 scoped 读通道
    // (segment_type/segment_id) 必须做行级隔离——project_x 只见 x、平台段 (segment_id IS NULL)
    // 与其他 project 互不串读。对齐 CSV direct(segment_id) 定性与 AC-ISO-1。
    @Test
    void businessMetricSnapshotsSegmentScopedReadIsTenantIsolated() {
        java.time.LocalDate day = java.time.LocalDate.of(2026, 6, 30);
        insertMetricSnapshot("snap_projx", "gmv", "project", "px", day, "1000");
        insertMetricSnapshot("snap_projy", "gmv", "project", "py", day, "2000");
        insertMetricSnapshot("snap_plat", "gmv", "platform", null, day, "9999");

        Map<String, java.math.BigDecimal> px = repository.readProjectMetricSnapshots(day, "px");
        assertThat(px).containsOnlyKeys("gmv");
        assertThat(px.get("gmv")).isEqualByComparingTo("1000");

        Map<String, java.math.BigDecimal> py = repository.readProjectMetricSnapshots(day, "py");
        assertThat(py.get("gmv")).isEqualByComparingTo("2000");

        // 越权断言：project_x 的读结果不得等于 project_y 的值，且平台段独立。
        assertThat(px.get("gmv")).isNotEqualByComparingTo(py.get("gmv"));

        // 平台段 (scope=null → segment_id IS NULL) 只见平台行,不吸入任一 project 行。
        Map<String, java.math.BigDecimal> platform = repository.readMetricSnapshots(day, null);
        assertThat(platform.get("gmv")).isEqualByComparingTo("9999");

        // project 维度不误读平台段;不存在的 project 返回空。
        assertThat(repository.readProjectMetricSnapshots(day, "pz")).isEmpty();
    }

    private void insertMetricSnapshot(String id, String metricKey, String segmentType,
            String segmentId, java.time.LocalDate businessDate, String actual) {
        jdbcTemplate.update("""
                INSERT INTO analytics_business_metric_snapshots
                    (snapshot_id, metric_key, domain, period_type, business_date,
                     segment_type, segment_id, actual_value, trend_direction,
                     projection_method, created_at)
                VALUES
                    (:id, :metric_key, 'analytics', 'daily', :business_date,
                     :segment_type, :segment_id, :actual, 'flat',
                     'etl_aggregate', now())
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("metric_key", metricKey)
                .addValue("business_date", businessDate)
                .addValue("segment_type", segmentType)
                .addValue("segment_id", segmentId)
                .addValue("actual", new java.math.BigDecimal(actual)));
    }

}
