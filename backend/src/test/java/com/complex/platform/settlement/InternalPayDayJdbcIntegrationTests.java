package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.TestSecurityActors;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * FS-021-R3/R4：三个付账日配置与钱路指派规则（BSM L1673、L1683；DEC-R4-35/36/38、DEC-R8-56/57/58），
 * 结算账单出账可见、异议期（BSM L1597）与财务指定付账日推算（替换「今天+30 天」）。
 * 用例在测试事务内种规则与 2041 年起的逐日日历（周一至周五为工作日与银行工作日），结束回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@Transactional
class InternalPayDayJdbcIntegrationTests {

    private static final LocalDate JULY_2041 = LocalDate.of(2041, 7, 1);
    private static final Instant POSTED = Instant.parse("2041-07-31T12:00:00Z");

    @Autowired private InternalPayDayService payDays;
    @Autowired private SettlementPostingService postingService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.complex.platform.access.AccessRepository access;
    @Autowired private org.springframework.test.web.servlet.MockMvc mockMvc;

    private String project;
    private String financeA;
    private String financeB;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        project = "ipd_p_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','付账日项目','active',now(),now())",
                project);
        weekdayCalendar(LocalDate.of(2041, 1, 1), LocalDate.of(2042, 3, 31));
        InternalPayDayFixtures.seed(jdbc);
        // 配置提交与审批：平台财务两人（无商户归属），持结算配置权限。
        String role = "ipd_role_" + UUID.randomUUID().toString().substring(0, 8);
        access.createRole(new com.complex.platform.access.RoleRecord(role, "平台财务", null, "project", "active", Instant.now(),
                java.util.List.of("settlement.config.view", "settlement.config.manage"), null, false, java.util.List.of()));
        financeA = financeUser(role);
        financeB = financeUser(role);
    }

    private String financeUser(String role) {
        String id = "ipd_u_" + UUID.randomUUID();
        access.createUser(new com.complex.platform.access.AccessUser(id, id, "staff", project, null, "active", Instant.now()));
        access.assignUserRole(id, role);
        return id;
    }

    private void act(String id) {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor(id, id, "staff", project, null, "active"), "test",
                        java.util.List.of()));
    }

    @AfterEach
    void clear() {
        TestSecurityActors.clear();
    }

    // ─── R4 推算 ───────────────────────────────────────────────

    /** 财务指定第 3 个付账日（25 日）：异议 8 日届满，不提前改到 15 日；25 日是周日，顺延至下一个银行工作日 26 日。 */
    @Test
    void financeChosenTwentyFifthIsNotMovedToFifteenth() {
        routeRule("ticket_distribution", 1, 3);
        objectionDays(5);

        var s = payDays.schedule("ticket_distribution", project, JULY_2041, POSTED);

        assertThat(s.objectionDeadline()).isEqualTo(LocalDate.of(2041, 8, 8));
        assertThat(s.designatedPayDay()).isEqualTo(LocalDate.of(2041, 8, 25));
        assertThat(s.dueDate()).isEqualTo(LocalDate.of(2041, 8, 26));
    }

    /** 指定 5 日早于异议届满（8 日）→ 顺延至本账期下一个满足顺序的付账日 15 日。 */
    @Test
    void designatedFifthBeforeObjectionExpiryRollsToNextSatisfyingDay() {
        routeRule("ticket_distribution", 1, 1);
        objectionDays(5);

        var s = payDays.schedule("ticket_distribution", project, JULY_2041, POSTED);

        assertThat(s.designatedPayDay()).isEqualTo(LocalDate.of(2041, 8, 5));
        assertThat(s.dueDate()).isEqualTo(LocalDate.of(2041, 8, 15));
    }

    /** 异议 20 个工作日届满于 29 日，本账期三个付账日均不满足 → 下一账期第一个满足顺序的付账日 9 月 5 日。 */
    @Test
    void noDayInPeriodSatisfiedRollsToNextPeriodFirstSatisfying() {
        routeRule("ticket_distribution", 1, 2);
        objectionDays(20);

        var s = payDays.schedule("ticket_distribution", project, JULY_2041, POSTED);

        assertThat(s.objectionDeadline()).isEqualTo(LocalDate.of(2041, 8, 29));
        assertThat(s.dueDate()).isEqualTo(LocalDate.of(2041, 9, 5));
    }

    /** 规则生效日不可回溯、过账总在账期结束之后：按过账时刻取当时已生效的钱路规则，账期首日之后才生效的规则照样适用。 */
    @Test
    void routeRuleEffectiveAfterPeriodStartAppliesAtPosting() {
        jdbc.update("""
                INSERT INTO internal_pay_day_rules (id, kind, route, statement_day, pay_slot, effective_from, status, reason,
                    submitted_by, decided_by, decided_at)
                VALUES (?, 'route', 'ticket_distribution', 1, 3, DATE '2041-07-20', 'approved', '上线当月审批', 'fin_a', 'fin_b', now())
                """, "ipdr_t_" + UUID.randomUUID().toString().substring(0, 12));
        objectionDays(5);

        var s = payDays.schedule("ticket_distribution", project, JULY_2041, POSTED);

        assertThat(s.designatedPayDay()).as("取过账时已生效的规则（第 3 个付账日）").isEqualTo(LocalDate.of(2041, 8, 25));
    }

    /** 商户专区整日不可用的自然日不计入异议期；部分时段不可用不顺延。 */
    @Test
    void wholeDayPortalOutageExtendsObjectionButPartialDoesNot() {
        routeRule("ticket_distribution", 1, 3);
        objectionDays(5);
        outage(LocalDateTime.of(2041, 8, 5, 0, 0), LocalDateTime.of(2041, 8, 6, 0, 0));   // 8 月 5 日全天
        outage(LocalDateTime.of(2041, 8, 6, 9, 0), LocalDateTime.of(2041, 8, 6, 11, 0));  // 8 月 6 日两小时

        var s = payDays.schedule("ticket_distribution", project, JULY_2041, POSTED);

        assertThat(s.objectionDeadline()).isEqualTo(LocalDate.of(2041, 8, 9));
    }

    /** 过账时该钱路没有已生效的已批规则 → 拒绝过账、账单保持已确认、不挂往来，不填默认日期。 */
    @Test
    void postingWithoutRouteRuleKeepsBillConfirmed() {
        jdbc.update("UPDATE internal_pay_day_rules SET status='rejected' WHERE kind='route' AND route='ticket_distribution'");
        String bill = "sb_ipd_" + System.nanoTime();
        jdbc.update("""
                INSERT INTO settlement_bills (id, project_id, period_start, period_end, ticket_revenue_cent, available_pool_cent,
                    distributed_cent, distribution_method, status)
                VALUES (?, ?, DATE '1999-05-01', DATE '1999-05-31', 5000, 5000, 5000, 'by_rank_tier', 'confirmed')
                """, bill, project);
        String item = "si_ipd_" + System.nanoTime();
        jdbc.update("""
                INSERT INTO settlement_items (id, bill_id, merchant_id, distribution_method, calculated_amount_cent,
                    final_amount_cent, status) VALUES (?, ?, 'ipd_bar', 'by_rank_tier', 5000, 5000, 'draft')
                """, item, bill);

        Throwable error = catchThrowable(() -> postingService.postBill(bill));

        assertThat(error).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("PAY_DAY_NOT_CONFIGURED"));
        assertThat(jdbc.queryForObject("SELECT status FROM settlement_bills WHERE id=?", String.class, bill)).isEqualTo("confirmed");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_id=?", Integer.class, item)).isZero();
    }

    /** 过账固化：应付到期日＝推算出的有效付账日，可查看时刻、异议截止与指定付账日逐笔可查；规则取过账时刻（今天）已生效的版本。 */
    @Test
    void postedPayableCarriesScheduleInsteadOfThirtyDays() {
        jdbc.update("""
                INSERT INTO internal_pay_day_rules (id, kind, route, statement_day, pay_slot, effective_from, status, reason,
                    submitted_by, decided_by, decided_at)
                VALUES (?, 'route', 'ticket_distribution', 1, 3, DATE '2001-01-01', 'approved', '测试', 'fin_a', 'fin_b', now())
                """, "ipdr_t_" + UUID.randomUUID().toString().substring(0, 12));
        objectionDays(5);
        String bill = "sb_ipd_" + System.nanoTime();
        jdbc.update("""
                INSERT INTO settlement_bills (id, project_id, period_start, period_end, ticket_revenue_cent, available_pool_cent,
                    distributed_cent, distribution_method, status)
                VALUES (?, ?, DATE '2041-07-01', DATE '2041-07-31', 5000, 5000, 5000, 'by_rank_tier', 'confirmed')
                """, bill, project);
        String item = "si_ipd_" + System.nanoTime();
        jdbc.update("""
                INSERT INTO settlement_items (id, bill_id, merchant_id, distribution_method, calculated_amount_cent,
                    final_amount_cent, status) VALUES (?, ?, 'ipd_bar', 'by_rank_tier', 5000, 5000, 'draft')
                """, item, bill);

        postingService.postBill(bill);

        var row = jdbc.queryForMap("""
                SELECT a.due_date, s.objection_deadline, s.designated_pay_day FROM ar_ap_items a
                  JOIN internal_payable_schedules s ON s.ar_ap_item_id = a.id WHERE a.source_id = ?
                """, item);
        assertThat(((Date) row.get("due_date")).toLocalDate()).isEqualTo(LocalDate.of(2041, 8, 26));
        assertThat(((Date) row.get("objection_deadline")).toLocalDate()).isEqualTo(LocalDate.of(2041, 8, 8));
        assertThat(((Date) row.get("designated_pay_day")).toLocalDate()).isEqualTo(LocalDate.of(2041, 8, 25));
    }

    // ─── R3 配置 ───────────────────────────────────────────────

    /** 钱路指派规则一经确定一年内不得变更；满一年后的新规则可提交。 */
    @Test
    void routeRuleChangeWithinOneYearRejected() {
        act(financeA);
        var first = payDays.submit(routeRequest("ticket_distribution", 1, 2, LocalDate.of(2041, 1, 1)));
        act(financeB);
        payDays.decide(first.id(), new InternalPayDayService.DecisionRequest(key(), "approve"));
        act(financeA);

        Throwable early = catchThrowable(() -> payDays.submit(routeRequest("ticket_distribution", 1, 3, LocalDate.of(2041, 6, 1))));
        assertThat(early).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PAY_DAY_RULE_LOCKED"));
        assertThat(payDays.submit(routeRequest("ticket_distribution", 1, 3, LocalDate.of(2042, 1, 1))).status()).isEqualTo("pending");
    }

    /** 三个付账日一个都不能满足「出账 → 异议期届满 → 付账日」顺序的配置拒绝保存。 */
    @Test
    void configWhereNoPayDaySatisfiesOrderIsRejected() {
        act(financeA);
        Throwable error = catchThrowable(() -> payDays.submit(payDaysRequest(1, 2, 3)));

        assertThat(error).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("PAY_DAY_ORDER_UNSATISFIABLE"));
    }

    /** 只有一个付账日能满足顺序即可保存，不因单个日期不满足就整组拒绝。 */
    @Test
    void configWhereOnlyOneDaySatisfiesIsAccepted() {
        act(financeA);
        assertThat(payDays.submit(payDaysRequest(5, 6, 28)).status()).isEqualTo("pending");
    }

    @Test
    void submitterCannotApproveOwnRule() {
        act(financeA);
        var pending = payDays.submit(routeRequest("ticket_distribution", 1, 2, LocalDate.of(2041, 1, 1)));

        Throwable error = catchThrowable(() -> payDays.decide(pending.id(), new InternalPayDayService.DecisionRequest(key(), "approve")));

        assertThat(error).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo("SELF_APPROVAL_FORBIDDEN"));
    }

    @Test
    void objectionPolicyRequiresBoundsAroundValue() {
        act(financeA);
        assertThatThrownBy(() -> payDays.saveObjectionPolicy(new InternalPayDayService.ObjectionPolicyRequest(
                key(), project, 12, null, null, "按项目配置异议期")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(payDays.saveObjectionPolicy(new InternalPayDayService.ObjectionPolicyRequest(
                key(), project, 12, 8, 15, "按项目配置异议期")).workingDays()).isEqualTo(12);
    }

    /** 按前端真实请求形状（adminClient.mapKeys 蛇形键）经网关提交钱路规则。 */
    @Test
    void submitRouteRuleWithFrontendSnakeCaseBody() throws Exception {
        var actor = new com.complex.platform.access.AccessActor(financeA, financeA, "staff", project, null, "active");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/settlement/internal-pay-days/rules")
                        .with(request -> {
                            request.addHeader("X-Terminal-Type", "web");
                            return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication(
                                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(actor, null,
                                            java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_P0_STAFF"))))
                                    .postProcessRequest(request);
                        })
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key":"%s","kind":"route","route":"ticket_distribution","statement_day":1,"pay_slot":2,
                                 "effective_from":"2041-01-01","reason":"按工作负荷统一制定"}
                                """.formatted(key())))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        assertThat(jdbc.queryForObject("""
                SELECT pay_slot FROM internal_pay_day_rules
                 WHERE route='ticket_distribution' AND effective_from=DATE '2041-01-01' AND status='pending' AND submitted_by=?
                """, Integer.class, financeA)).isEqualTo(2);
    }

    // ---- helpers ----

    private InternalPayDayService.RuleRequest routeRequest(String route, int statementDay, int slot, LocalDate from) {
        return new InternalPayDayService.RuleRequest(key(), "route", route, null, null, null, statementDay, slot, from,
                "按财务工作负荷统一制定");
    }

    private InternalPayDayService.RuleRequest payDaysRequest(int d1, int d2, int d3) {
        return new InternalPayDayService.RuleRequest(key(), "pay_days", null, d1, d2, d3, null, null, null, "调整三个付账日");
    }

    private void routeRule(String route, int statementDay, int slot) {
        jdbc.update("""
                INSERT INTO internal_pay_day_rules (id, kind, route, statement_day, pay_slot, effective_from, status, reason,
                    submitted_by, decided_by, decided_at)
                VALUES (?, 'route', ?, ?, ?, DATE '2041-01-01', 'approved', '测试', 'fin_a', 'fin_b', now())
                """, "ipdr_t_" + UUID.randomUUID().toString().substring(0, 12), route, statementDay, slot);
    }

    private void objectionDays(int days) {
        jdbc.update("INSERT INTO settlement_objection_period_policy(project_id,working_days,min_days,max_days) VALUES(?,?,0,30)",
                project, days);
    }

    private void outage(LocalDateTime from, LocalDateTime to) {
        jdbc.update("""
                INSERT INTO ticket_points_portal_outages(id,project_id,unavailable_from,unavailable_to,reason,status,created_by,created_at,approved_at)
                VALUES(?,?,?,?,'已核实故障','approved','outage-review',now(),now())
                """, "out_" + UUID.randomUUID(), project, Timestamp.from(from.atZone(LocalZone.ID).toInstant()),
                Timestamp.from(to.atZone(LocalZone.ID).toInstant()));
    }

    private void weekdayCalendar(LocalDate from, LocalDate to) {
        jdbc.update("""
                INSERT INTO ticket_points_calendar_cases (id, days, source_note, status, created_by, created_at, approved_at)
                VALUES ('tpcc_ipd_2041', '[]', '测试给定每日事实', 'approved', 'fixture', now(), now()) ON CONFLICT (id) DO NOTHING
                """);
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            boolean working = d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY;
            jdbc.update("""
                    INSERT INTO ticket_points_calendar_days(calendar_date,working_day,bank_working_day,case_id) VALUES(?,?,?,'tpcc_ipd_2041')
                    ON CONFLICT (calendar_date) DO NOTHING
                    """, d, working, working);
        }
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
