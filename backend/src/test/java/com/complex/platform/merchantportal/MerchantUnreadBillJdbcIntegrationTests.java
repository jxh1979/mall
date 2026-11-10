package com.complex.platform.merchantportal;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.dividend.DividendRecords;
import com.complex.platform.dividend.DividendRecords.DividendStatementRecord;
import com.complex.platform.dividend.DividendService;
import com.complex.platform.dividend.DividendRepository;
import com.complex.platform.dividend.dto.DividendActionRequest;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.StaffRequestFixture;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第 92 条（BSM:1597「商户专区首页保留未读账单提醒，该提醒不因异议期届满而消失。」）：账单在商户专区可查看、商户还没看过即未读；
 * 商户在商户专区看过即记已读（综合账单按计费自然月、门票分账、分红单），按商户记，登录不算读，没有按时间消失。
 * 走真实入口与真实授权链，按前端蛇形键请求。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class MerchantUnreadBillJdbcIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DividendService dividends;
    @Autowired
    private DividendRepository dividendRepository;
    @Autowired
    private AccessRepository access;

    private final String sfx = Long.toString(System.nanoTime(), 36);
    private StaffRequestFixture staffs;

    @BeforeEach
    void seed() {
        InternalPayDayFixtures.seed(jdbc);
        staffs = new StaffRequestFixture(jdbc);
    }

    @AfterEach
    void clear() {
        TestSecurityActors.clear();
    }

    /** (a)(b)(d)(e)：综合账单按计费自然月计未读，商户打开我的账单该月即已读；首次已读时间不改；同月再出账重新未读；按商户记。 */
    @Test
    void feeStatementMonthStaysUnreadUntilViewedAndReadIsPerMerchant() throws Exception {
        String m = merchant("mch_ub_" + sfx, "未读账单店");
        String b = merchant("mch_ubb_" + sfx, "别家未读店");
        RequestPostProcessor finance = staffs.staff("usr_ub_fin_" + sfx, "platform_admin", null, null, "finance.ar_ap.manage");
        mockMvc.perform(post("/api/v1/finance/ar-ap-items").with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "ub-a-%s", "direction": "receivable", "counterparty_id": "%s", "source_type": "manual",
                                 "amount_cent": 12000, "due_date": "2026-08-28", "accounting_period": "2026-08"}
                                """.formatted(sfx, m)))
                .andExpect(status().isOk());
        RequestPostProcessor shop = staffs.staff("usr_ub_m1_" + sfx, "staff", null, m, "merchant.billing.view");
        assertThat(billMonths(unread(shop))).containsExactly(entry("2026-08", 1L));

        feeStatement(shop, "2026-09");
        assertThat(billMonths(unread(shop))).as("看别的月不算看过 8 月").containsExactly(entry("2026-08", 1L));
        feeStatement(shop, "2026-08");
        assertThat(billMonths(unread(shop))).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM merchant_bill_reads WHERE merchant_id = ? AND bill_kind = 'ar_ap_item'",
                Integer.class, m)).isEqualTo(1);
        jdbc.update("UPDATE merchant_bill_reads SET read_at = TIMESTAMPTZ '2026-01-01 00:00:00+08' WHERE merchant_id = ?", m);
        feeStatement(shop, "2026-08");
        assertThat(jdbc.queryForObject("SELECT read_at = TIMESTAMPTZ '2026-01-01 00:00:00+08' FROM merchant_bill_reads WHERE merchant_id = ?",
                Boolean.class, m)).as("再看一次首次已读时间不改").isTrue();

        // (d) 已读的 8 月再挂一笔同账期应收：该月重新出现 1 笔未读。
        item("ubd_" + sfx, "receivable", m, "manual", "2026-08-30", "2026-08");
        assertThat(billMonths(unread(shop))).containsExactly(entry("2026-08", 1L));

        // (e) 商户 B 看自己的账单不影响 M；同商户另一账号看过，第一个账号也不再未读。
        item("ubb_" + sfx, "receivable", b, "manual", "2026-08-30", "2026-08");
        RequestPostProcessor other = staffs.staff("usr_ub_b_" + sfx, "staff", null, b, "merchant.billing.view");
        feeStatement(other, "2026-08");
        assertThat(billMonths(unread(other))).isEmpty();
        assertThat(billMonths(unread(shop))).containsExactly(entry("2026-08", 1L));
        RequestPostProcessor colleague = staffs.staff("usr_ub_m2_" + sfx, "staff", null, m, "merchant.billing.view");
        feeStatement(colleague, "2026-08");
        assertThat(billMonths(unread(shop))).isEmpty();
    }

    /** (c)：异议期早已届满的账单照样未读（提醒不因异议期届满而消失），直到看过那个月；新月在前。 */
    @Test
    void longPastMonthStaysUnreadUntilViewed() throws Exception {
        String m = merchant("mch_ubc_" + sfx, "旧账单店");
        item("ubc1_" + sfx, "receivable", m, "manual", "2025-01-31", "2025-01");
        item("ubc2_" + sfx, "receivable", m, "manual", "2026-08-30", "2026-08");
        item("ubc3_" + sfx, "receivable", m, "manual", "2026-08-31", "2026-08");
        RequestPostProcessor shop = staffs.staff("usr_ubc_" + sfx, "staff", null, m, "merchant.billing.view");
        assertThat(billMonths(unread(shop))).containsExactly(entry("2026-08", 2L), entry("2025-01", 1L));
        feeStatement(shop, "2025-01");
        assertThat(billMonths(unread(shop))).containsExactly(entry("2026-08", 2L));
    }

    /** (f)：门票分账只数已可查看、应付本商户、没看过的；商户应付平台的那笔只在综合账单里计；商户看过门票分账即已读。 */
    @Test
    void ticketSettlementsCountVisiblePayablesUntilListed() throws Exception {
        String m = merchant("mch_ubt_" + sfx, "门票分账未读店");
        schedule(item("ubt1_" + sfx, "payable", m, "ticket_distribution", "2099-03-31", "2099-03"), m, "now() - interval '1 hour'");
        schedule(item("ubt2_" + sfx, "payable", m, "ticket_distribution", "2099-03-31", "2099-03"), m, "now() + interval '1 day'");
        schedule(item("ubt3_" + sfx, "receivable", m, "single_bar_service_fee", "2099-03-31", "2099-03"), m, "now() - interval '1 hour'");
        RequestPostProcessor shop = staffs.staff("usr_ubt_" + sfx, "staff", null, m, "merchant.billing.view",
                "merchant.portal.settlement.view");
        JsonNode before = unread(shop);
        assertThat(before.path("ticket_settlement_unread_count").asLong()).isEqualTo(1);
        assertThat(billMonths(before)).as("商户应付平台的那笔按计费月在综合账单里计").containsExactly(entry("2099-02", 1L));

        mockMvc.perform(get("/api/v1/merchant-portal/ticket-settlements").with(shop)).andExpect(status().isOk());
        JsonNode after = unread(shop);
        assertThat(after.path("ticket_settlement_unread_count").asLong()).isZero();
        assertThat(billMonths(after)).as("门票分账里列出过即已读").isEmpty();
        assertThat(jdbc.queryForList("SELECT bill_id FROM merchant_bill_reads WHERE merchant_id = ? ORDER BY bill_id", String.class, m))
                .as("明天才可查看的那笔没列出、不记已读").containsExactly("ubt1_" + sfx, "ubt3_" + sfx);
    }

    /** (g)：无商户归属 403 MERCHANT_SCOPE_REQUIRED；三个码都没有 403；只有分红查看码只返回分红单。 */
    @Test
    void unreadBillsFailClosedAndFollowHeldPermissions() throws Exception {
        String m = merchant("mch_ubg_" + sfx, "未读权限店");
        mockMvc.perform(get("/api/v1/merchant-portal/unread-bills")
                        .with(staffs.staff("usr_ubg_p_" + sfx, "staff", null, null, "merchant.billing.view")))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("MERCHANT_SCOPE_REQUIRED"));
        mockMvc.perform(get("/api/v1/merchant-portal/unread-bills")
                        .with(staffs.staff("usr_ubg_n_" + sfx, "staff", null, m, "contract.view")))
                .andExpect(status().isForbidden());
        JsonNode onlyDividends = unread(staffs.staff("usr_ubg_d_" + sfx, "staff", null, m, "finance.dividend.view"));
        assertThat(onlyDividends.hasNonNull("bill_months")).isFalse();
        assertThat(onlyDividends.hasNonNull("ticket_settlement_unread_count")).isFalse();
        assertThat(onlyDividends.path("dividend_statements").isArray()).isTrue();
    }

    /** (h)：审批通过（含已发放）的分红单才算出单可查看；商户在我的分红读到即已读；平台账号看列表不写已读。 */
    @Test
    void approvedDividendStatementsStayUnreadUntilMerchantListsThem() throws Exception {
        String m = merchant("mch_ubh_" + sfx, "分红未读店");
        String plan = "ubh_plan_" + sfx;
        Instant now = Instant.now();
        dividendRepository.insertPlan(new DividendRecords.DividendPlanRecord(plan, m, "usr_principal_" + sfx, "主理人甲",
                8000, 2000, "monthly", List.of("bar", "ticket"), true, List.of("labor"),
                null, "active", null, "usr_div_" + sfx, now, now, false, false, false, 0, 0L));
        jdbc.update("UPDATE dividend_plans SET payout_pay_slot = 1 WHERE id = ?", plan);   // 211-R2 发放付账日
        String role = "ubh_role_" + sfx;
        access.createRole(new RoleRecord(role, "未读分红审批-" + sfx, null, "platform", "active", now,
                List.of("finance.dividend.manage", "finance.dividend.view"), null, false, List.of()));
        String submitter = platformUser(role);
        String approver = platformUser(role);
        String paid = statement(m, plan, "2026-04");
        String approved = statement(m, plan, "2026-05");
        String pending = statement(m, plan, "2026-06");
        statement(m, plan, "2026-07");
        TestSecurityActors.asPlatformAdmin(submitter);
        for (String id : List.of(paid, approved, pending)) {
            dividends.submit(id, new DividendActionRequest(UUID.randomUUID().toString(), null));
        }
        TestSecurityActors.asPlatformAdmin(approver);
        dividends.approve(paid, new DividendActionRequest(UUID.randomUUID().toString(), null));
        dividends.markPaid(paid, new DividendActionRequest(UUID.randomUUID().toString(), null));
        dividends.approve(approved, new DividendActionRequest(UUID.randomUUID().toString(), null));
        TestSecurityActors.clear();

        RequestPostProcessor shop = staffs.staff("usr_ubh_" + sfx, "staff", null, m, "finance.dividend.view");
        assertThat(dividendIds(unread(shop))).containsExactlyInAnyOrder(paid, approved);

        RequestPostProcessor platform = staffs.staff("usr_ubh_p_" + sfx, "platform_admin", null, null, "finance.dividend.view");
        mockMvc.perform(get("/api/v1/finance/dividends/statements").param("merchant_id", m).param("page", "1").param("page_size", "20")
                .with(platform)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM merchant_bill_reads WHERE merchant_id = ?", Integer.class, m))
                .as("平台账号看列表不写已读").isZero();

        mockMvc.perform(get("/api/v1/finance/dividends/statements").param("page", "1").param("page_size", "20").with(shop))
                .andExpect(status().isOk());
        assertThat(dividendIds(unread(shop))).isEmpty();
    }

    // ── 请求 ───────────────────────────────────────────────────────────────────────────────

    private JsonNode unread(RequestPostProcessor who) throws Exception {
        String body = mockMvc.perform(get("/api/v1/merchant-portal/unread-bills").with(who))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return new ObjectMapper().readTree(body).path("data");
    }

    private void feeStatement(RequestPostProcessor who, String month) throws Exception {
        mockMvc.perform(get("/api/v1/merchant-portal/fee-statement").param("month", month).with(who)).andExpect(status().isOk());
    }

    private static Map<String, Long> billMonths(JsonNode data) {
        Map<String, Long> months = new LinkedHashMap<>();
        data.path("bill_months").forEach(n -> months.put(n.path("month").asText(), n.path("unread_count").asLong()));
        return months;
    }

    private static List<String> dividendIds(JsonNode data) {
        List<String> ids = new ArrayList<>();
        data.path("dividend_statements").forEach(n -> ids.add(n.path("id").asText()));
        return ids;
    }

    // ── 造数 ───────────────────────────────────────────────────────────────────────────────

    private String merchant(String id, String name) {
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)"
                + " VALUES (?, ?, 'bar', 'active', now(), now())", id, name);
        return id;
    }

    /** 往来对方为本商户的台账行一笔（金额 5000 分、未付）。 */
    private String item(String id, String direction, String counterparty, String sourceType, String due, String period) {
        jdbc.update("INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, amount_cent, paid_amount_cent,"
                + " unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, 5000, 0, 5000, 'CNY', CAST(? AS date), ?, ?, now(), now())",
                id, id, direction, counterparty, sourceType, due, period,
                "receivable".equals(direction) ? "pending_collection" : "pending_payment");
        return id;
    }

    /** 内部应付计划一行：计费月 2099-02，可查看时刻按 visibleAt 表达式。 */
    private void schedule(String itemId, String counterparty, String visibleAt) {
        jdbc.update("INSERT INTO internal_payable_schedules (ar_ap_item_id, route, counterparty_id, billing_period_start, visible_at,"
                + " objection_deadline, designated_pay_day, scheduled_due_date, pay_days_rule_id, route_rule_id, objection_working_days)"
                + " VALUES (?, 'ticket_distribution', ?, DATE '2099-02-01', " + visibleAt + ", DATE '2099-03-20', DATE '2099-03-31',"
                + " DATE '2099-03-31', 'ipdr_initial_pay_days', 'ipdr_fixture_ticket_distribution', 10)", itemId, counterparty);
    }

    /** 净利 10 万：平台 8 万、主理人税前 2 万、税 4000、税后 1.6 万，留存 1600、实发 1.44 万；已登记依法可分配金额。 */
    private String statement(String merchantId, String plan, String month) {
        String id = "ubh_s_" + month + "_" + sfx;
        YearMonth ym = YearMonth.parse(month);
        Instant now = Instant.now();
        dividendRepository.insertStatement(new DividendStatementRecord(id, merchantId, plan, "official", month, ym.atDay(1),
                ym.atEndOfMonth(), "usr_principal_" + sfx, 8000, 2000, List.of("bar", "ticket"), true, List.of("labor"),
                100_000L, 0L, 0L, 100_000L, 100_000L, 80_000L, 20_000L, 4_000L, 16_000L,
                0L, "draft", null, null, "usr_div_" + sfx, null, null, null, null, null, null, now, now,
                false, 0L, 0L, 0L, 0L, 1_600L, 14_400L, "individual", 0L, 1_000_000_000L));
        return id;
    }

    private String platformUser(String role) {
        String id = "ubh_u_" + UUID.randomUUID();
        access.createUser(new AccessUser(id, id, "platform_admin", "org_1", null, "active", Instant.now()));
        access.assignUserRole(id, role);
        return id;
    }
}
