package com.complex.platform.analytics;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.RevenueRefreshMarks;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.ArApAdjustmentDecisionRequest;
import com.complex.platform.finance.dto.ArApAdjustmentRequest;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-04-R3 补录数量（BSM「经营看板」章「主要指标」：「风险：待复核、异常事件、补录数量、第三方差异。」；
 * BSM「记录缺失时允许人工补录……补录一律进入主管复核队列，复核通过才生效」；BSM「锁定后补录须上级审批」；
 * 实施台账 latestConfirmedDecisions.M02「补录提交即计数，状态另列」）：
 * 锁账后补录经 FinanceService 真实提交与审批——提交即计一笔，批准、驳回、同一提交编号重放都不加笔数，状态另列；
 * 卡片按提交时刻落治理营业日；明细项目级只见本项目。无可信领取时刻补证一源的真实提交见 M010RegressionTests 补录数量用例。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ManualBackfillMetricJdbcIntegrationTests {

    @Autowired private AnalyticsService analytics;
    @Autowired private AnalyticsRepository repository;
    @Autowired private FinanceService finance;
    @Autowired private AccessRepository access;
    @Autowired private BusinessDayResolver businessDayResolver;
    @Autowired private RevenueRefreshMarks marks;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "mbf_p_" + suffix;
    private final String otherProject = "mbf_q_" + suffix;
    private final String merchant = "mbf_m_" + suffix;
    private String requester;
    private String approver;
    private String close;

    @BeforeEach
    void seed() {
        for (String p : List.of(project, otherProject)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','补录数量项目','active',now(),now())", p);
        }
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,project_id) VALUES(?,'补录数量商户','bar','active',now(),now(),?)", merchant, project);
        // 财务动作的商户准入（与 FinanceProjectScopeRefundJdbcIntegrationTests 同一夹具口径）。
        jdbc.update("""
                INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,
                    allowed_business_scope,created_at,updated_at)
                VALUES(?,?,'subj_mbf','subj_mbf',current_date-1,current_date+365,'effective','["finance"]'::jsonb,now(),now())
                """, "mbf_c_" + suffix, merchant);
        String role = "mbf_role_" + suffix;
        access.createRole(new RoleRecord(role, "项目财务", null, "project", "active", Instant.now(),
                List.of("finance.ar_ap.view", "finance.locked_backfill.request", "finance.locked_backfill.approve"),
                null, false, List.of()));
        requester = user(role);
        approver = user(role);
        close = "mbf_close_" + suffix;
        jdbc.update("""
                INSERT INTO finance_daily_closes(id,business_date,scope_type,scope_id,status,locked_at,created_at)
                VALUES(?,current_date,'merchant',?,'locked',now(),now())
                """, close, merchant);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void backfillCountsEverySubmissionOnceWithStatusesListedSeparately() {
        LocalDate d = businessDayResolver.businessDate(null, null, Instant.now());
        act(requester);
        ArApAdjustmentRequest first = request(lockedReceivable());
        var approved = finance.requestAdjustment(first);
        finance.requestAdjustment(first); // 同一提交编号重放：不新建记录
        var rejected = finance.requestAdjustment(request(lockedReceivable()));
        var pending = finance.requestAdjustment(request(lockedReceivable()));
        var earlier = finance.requestAdjustment(request(lockedReceivable()));
        // 前一营业日提交的那笔：提交时刻移到本营业日窗之前
        jdbc.update("UPDATE ar_ap_adjustments SET created_at=? WHERE id=?",
                Timestamp.from(businessDayResolver.start(null, null, d).minusSeconds(60)), earlier.id());
        act(approver);
        finance.approveAdjustment(approved.id(), decision("approve"));
        finance.approveAdjustment(rejected.id(), decision("reject"));
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();

        AnalyticsRecords.MetricValue card = analytics.overview(Optional.of(merchant), d).metrics().get("risk.manual_backfill");
        assertThat(card.value()).isEqualByComparingTo("3");
        assertThat(card.dimension()).containsEntry("statusCounts", Map.of("pending", 1L, "approved", 1L, "rejected", 1L));

        // 快照（月度对比与异常提醒用）与卡片同谓词。
        new DashboardAggregationJob(repository, businessDayResolver, false).refreshSnapshots(d);
        assertThat(repository.readMetricSnapshots(d, merchant).get("risk.manual_backfill")).isEqualByComparingTo("3");

        // 明细：项目级只见本项目；数字＝明细合计。
        AnalyticsRecords.MetricDetailResponse detail = analytics.metricDetail("risk.manual_backfill", null, d, d, 1, 20, Set.of(project));
        assertThat(detail.summaryValue()).isEqualByComparingTo("3");
        assertThat(detail.rows()).hasSize(3).allSatisfy(row -> {
            assertThat(row).containsEntry("sourceType", "locked_backfill").containsEntry("projectId", project);
            assertThat(row).doesNotContainKey("reason");
        });
        assertThat(analytics.metricDetail("risk.manual_backfill", null, d, d, 1, 20, Set.of(otherProject)).totalRows()).isZero();

        // 批准剩下那笔：笔数不变，只是状态另列变化。
        act(approver);
        finance.approveAdjustment(pending.id(), decision("approve"));
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        card = analytics.overview(Optional.of(merchant), d).metrics().get("risk.manual_backfill");
        assertThat(card.value()).isEqualByComparingTo("3");
        assertThat(card.dimension()).containsEntry("statusCounts", Map.of("approved", 2L, "rejected", 1L));
    }

    /**
     * 第十八批 S 段第 160 条 160-R1（ENUM 第 190 行「且不改动已锁定的日结金额：补录金额归入当期」；第七批裁决 19「金额归入当期、不改原日结金额」）：
     * 冲正行与重述行仍沿用原行到期日，但看板合同应收按批准当日计补录差额——批准不再登记原到期日重算，定时刷新与再次全量刷新后
     * 原到期日的合同租金、净收入日值与当月月度累计都仍为原额，批准当日本商户合同租金为补录额。
     */
    @Test
    void approvedBackfillOnLockedContractRentCountsOnApprovalDayNotItsDueDate() {
        LocalDate d = LocalDate.of(2097, 7, 20);
        String item = "mbf_rent_" + suffix;
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id,locked_day_close_id)
                VALUES(?,?,'receivable',?,'contract_rent',?,100,0,100,'CNY',?,'2097-07','pending_collection',now(),now(),?,?,?)
                """, item, "MBF-" + item, merchant, item, d, merchant, project, close);
        new DashboardAggregationJob(repository, businessDayResolver, false).refreshSnapshots(d);
        assertThat(platformSnapshot("revenue.contract_rent", "daily", d)).isEqualByComparingTo("100");

        act(requester);
        var adjustment = finance.requestAdjustment(request(item));
        act(approver);
        finance.approveAdjustment(adjustment.id(), decision("approve"));
        DashboardAggregationJob job = new DashboardAggregationJob(repository,
                Clock.fixed(LocalDate.of(2097, 8, 5).atTime(12, 0).atZone(LocalZone.ID).toInstant(), LocalZone.ID),
                businessDayResolver, true);
        job.setRevenueRefreshMarks(marks);
        job.scheduledRefresh();
        new DashboardAggregationJob(repository, businessDayResolver, false).refreshSnapshots(d);

        assertThat(platformSnapshot("revenue.contract_rent", "daily", d)).isEqualByComparingTo("100");
        assertThat(platformSnapshot("revenue.net", "daily", d)).isEqualByComparingTo("100");
        assertThat(platformSnapshot("revenue.net", "monthly", d.withDayOfMonth(1))).isEqualByComparingTo("100");
        LocalDate today = LocalDate.now(LocalZone.ID);
        new DashboardAggregationJob(repository, businessDayResolver, false).refreshSnapshots(today);
        assertThat(segmentSnapshot("revenue.contract_rent", today, "merchant", merchant)).isEqualByComparingTo("30");
    }

    /**
     * 第十八批 S 段第 160 条 160-R1 真实流程（后台日结区块「申请补录」「批准」的请求形状，蛇形键；ENUM 第 190 行「补录金额归入当期」；
     * BSM 第 1323 行「提出异议期间商户的付款义务不暂停，先按原值付款，异议成立后多退少补。」；BSM 第 1347 行「缴费期限一律以各该账单自身载明的期限为准」）：
     * 锁在过去营业日 D 的合同租金应收经申请、他人批准后——原行不动；冲正行与重述行到期日仍为 D、不继承锁、回指补录单；不登记 D 重算；
     * 按日结查补录带原营业日 D。看板：D 的商户合同租金仍为原额，批准当日调增记 +30、调减记 −30（商户段、商户分项目段、商户净收入）；
     * 账龄按重述行到期日 D 算（逾期 10 天、欠 130）。
     */
    @Test
    void lockedContractRentBackfillApprovedThroughApiKeepsDueDateAndCountsOnApprovalDay() throws Exception {
        LocalDate d = LocalDate.of(2001, 1, 1).plusDays(Math.floorMod(suffix.hashCode(), 3000));
        LocalDate today = LocalDate.now(LocalZone.ID);
        String reduced = "mbf_n_" + suffix;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,project_id) VALUES(?,'补录调减商户','bar','active',now(),now(),?)", reduced, project);
        jdbc.update("""
                INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,
                    allowed_business_scope,created_at,updated_at)
                VALUES(?,?,'subj_mbf','subj_mbf',current_date-1,current_date+365,'effective','["finance"]'::jsonb,now(),now())
                """, "mbf_cn_" + suffix, reduced);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor bp = staffs.staff("mbf_bp_" + suffix, "staff", project, null,
                "finance.locked_backfill.request", "finance.daily_close.manage");
        RequestPostProcessor boss = staffs.staff("mbf_boss_" + suffix, "staff", project, null, "finance.locked_backfill.approve");
        String[] up = lockedContractRent(merchant, d);
        String[] down = lockedContractRent(reduced, d);
        Integer marksBefore = jdbc.queryForObject("SELECT COUNT(*) FROM analytics_revenue_refresh_dates WHERE business_date = ?", Integer.class, d);

        String upId = approvedBackfill(bp, boss, up[0], 30);
        approvedBackfill(bp, boss, down[0], -30);

        assertThat(jdbc.query("SELECT amount_cent, unpaid_amount_cent, due_date, locked_day_close_id, adjustment_id FROM ar_ap_items WHERE id = ?",
                (rs, n) -> tuple(rs.getLong(1), rs.getLong(2), rs.getObject(3, LocalDate.class), rs.getString(4), rs.getString(5)), up[0]))
                .containsExactly(tuple(100L, 100L, d, up[1], null));
        assertThat(jdbc.query("SELECT id, amount_cent, due_date, locked_day_close_id, adjustment_id FROM ar_ap_items WHERE origin_item_id = ?",
                (rs, n) -> tuple(rs.getString(1), rs.getLong(2), rs.getObject(3, LocalDate.class), rs.getString(4), rs.getString(5)), up[0]))
                .containsExactlyInAnyOrder(tuple("arap_rev_" + upId, -100L, d, null, upId), tuple("arap_res_" + upId, 130L, d, null, upId));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM analytics_revenue_refresh_dates WHERE business_date = ?", Integer.class, d))
                .isEqualTo(marksBefore);
        mockMvc.perform(get("/api/v1/finance/daily-closes/" + up[1] + "/adjustments").with(bp))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(upId))
                .andExpect(jsonPath("$.data.items[0].original_business_date").value(d.toString()));

        DashboardAggregationJob job = new DashboardAggregationJob(repository, businessDayResolver, false);
        job.refreshSnapshots(d);
        assertThat(segmentSnapshot("revenue.contract_rent", d, "merchant", merchant)).isEqualByComparingTo("100");
        assertThat(segmentSnapshot("revenue.contract_rent", d, "merchant", reduced)).isEqualByComparingTo("100");
        job.refreshSnapshots(today);
        assertThat(segmentSnapshot("revenue.contract_rent", today, "merchant", merchant)).isEqualByComparingTo("30");
        assertThat(segmentSnapshot("revenue.contract_rent", today, "merchant_project", merchant + "|" + project)).isEqualByComparingTo("30");
        assertThat(segmentSnapshot("revenue.net", today, "merchant", merchant)).isEqualByComparingTo("30");
        assertThat(segmentSnapshot("revenue.contract_rent", today, "merchant", reduced)).isEqualByComparingTo("-30");

        assertThat(repository.listOverdueDebtors(d.plusDays(10), null, 200, 0, List.of(project)))
                .filteredOn(row -> merchant.equals(row.merchantId()))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.overdueDays()).isEqualTo(10L);
                    assertThat(row.outstandingCent()).isEqualByComparingTo("130");
                    assertThat(row.oldestDueDate()).isEqualTo(d);
                });
    }

    /** 160-R1：该商户在营业日 d 的已锁定商户日结与锁在其上的一笔合同租金应收（100 分）；返回 {台账编号, 日结编号}。 */
    private String[] lockedContractRent(String merchantId, LocalDate d) {
        String closeId = "mbf_dc_" + merchantId;
        String item = "mbf_cr_" + merchantId;
        jdbc.update("""
                INSERT INTO finance_daily_closes(id,business_date,scope_type,scope_id,status,locked_at,created_at)
                VALUES(?,?,'merchant',?,'locked',now(),now())
                """, closeId, d, merchantId);
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id,locked_day_close_id)
                VALUES(?,?,'receivable',?,'contract_rent',?,100,0,100,'CNY',?,?,'pending_collection',now(),now(),?,?,?)
                """, item, "MBF-" + item, merchantId, item, d, d.toString().substring(0, 7), merchantId, project, closeId);
        return new String[] {item, closeId};
    }

    /** 160-R1：申请人提交锁定后补录、另一人批准（后台真实请求形状），返回补录单号。 */
    private String approvedBackfill(RequestPostProcessor requesterRequest, RequestPostProcessor approverRequest, String itemId,
            long amountCent) throws Exception {
        String id = JsonPath.read(mockMvc.perform(post("/api/v1/finance/ar-ap-adjustments").with(requesterRequest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"mbf-adj-" + itemId + "\",\"ar_ap_item_id\":\"" + itemId
                                + "\",\"adjustment_amount_cent\":" + amountCent + ",\"reason\":\"核实租金差额\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(post("/api/v1/finance/ar-ap-adjustments/" + id + "/approve").with(approverRequest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"mbf-apv-" + itemId + "\",\"decision\":\"approve\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approval_status").value("approved"));
        return id;
    }

    private BigDecimal segmentSnapshot(String key, LocalDate date, String segmentType, String segmentId) {
        List<BigDecimal> values = jdbc.queryForList("""
                SELECT actual_value FROM analytics_business_metric_snapshots
                 WHERE metric_key = ? AND period_type = 'daily' AND business_date = ? AND segment_type = ? AND segment_id = ?
                """, BigDecimal.class, key, date, segmentType, segmentId);
        return values.isEmpty() ? null : values.getFirst();
    }

    private BigDecimal platformSnapshot(String key, String period, LocalDate date) {
        List<BigDecimal> values = jdbc.queryForList("""
                SELECT actual_value FROM analytics_business_metric_snapshots
                 WHERE metric_key = ? AND period_type = ? AND business_date = ? AND segment_type = 'platform' AND segment_id IS NULL
                """, BigDecimal.class, key, period, date);
        return values.isEmpty() ? null : values.getFirst();
    }

    private String lockedReceivable() {
        String item = "mbf_lock_" + UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id,locked_day_close_id)
                VALUES(?,?,'receivable',?,'manual',?,100,0,100,'CNY',current_date,'2026-10','pending_collection',now(),now(),?,?,?)
                """, item, "MBF-" + item, merchant, item, merchant, project, close);
        return item;
    }

    private static ArApAdjustmentRequest request(String item) {
        return new ArApAdjustmentRequest(UUID.randomUUID().toString(), "锁账后补录", List.of(), item, 30L, "核实少记");
    }

    private static ArApAdjustmentDecisionRequest decision(String value) {
        return new ArApAdjustmentDecisionRequest(UUID.randomUUID().toString(), "独立复核", List.of(), value);
    }

    private String user(String role) {
        String id = "mbf_u_" + UUID.randomUUID();
        access.createUser(new AccessUser(id, id, "staff", project, null, "active", Instant.now()));
        access.assignUserRole(id, role);
        return id;
    }

    private void act(String id) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(id, id, "staff", project, null, "active"), "test", List.of()));
    }
}
