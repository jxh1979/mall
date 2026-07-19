package com.complex.platform.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.analytics.AnalyticsRecords.MetricDetailRows;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * KPI 统一明细「项目作用域收紧」真库数据隔离铁证（2026-07-19 用户拍板：只有平台级管理者看平台口径聚合；
 * 项目级操作者只能看本项目内容）。本任务资金/隐私底线：项目 A 操作者绝不能看到项目 B 的明细/合计。
 *
 * <p>范式：种子项目 id 全部带每次运行随机后缀（{@link #proj}），故这些 scope 在共享 complex_test 上
 * baseline=0 —— scope=projA 的合计恰等于只 A 行数（若泄露 B 行则必炸），直证行级隔离；null scope=平台看全量。
 * 按环境跑（禁在本任务内执行 @SpringBootTest/JDBC，仅编译+提交，真跑见 CI/部署闸）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class MetricDetailProjectScopeJdbcIntegrationTests {

    private final String run = java.util.UUID.randomUUID().toString().substring(0, 8);

    private String proj(String stem) {
        return stem + "_" + run;
    }

    @Autowired
    private JdbcAnalyticsRepository repository;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Test
    void staffShiftDetailIsolatesByProjectId() {
        LocalDate day = LocalDate.parse("2026-06-20");
        String projA = proj("shiftA");
        String projB = proj("shiftB");
        // 平台全量基线（null scope），再种 A×2 / B×1。
        BigDecimal baseAll = repository.staffShiftDetail(day, day, 1, 0, null).summaryValue();
        insertShift(proj("sh_a1"), day, projA);
        insertShift(proj("sh_a2"), day, projA);
        insertShift(proj("sh_b1"), day, projB);

        // 隔离铁证：projA 操作者只见自己 2 行（唯一 project id → baseline 0 → 合计恰 2，绝不含 B 的 1 行）。
        assertThat(repository.staffShiftDetail(day, day, 200, 0, projA).summaryValue()).isEqualByComparingTo("2");
        assertThat(repository.staffShiftDetail(day, day, 200, 0, projB).summaryValue()).isEqualByComparingTo("1");
        // 平台管理员（null scope）看全量：净增 3。
        assertThat(repository.staffShiftDetail(day, day, 200, 0, null).summaryValue().subtract(baseAll))
                .isEqualByComparingTo("3");
        // 明细行数与合计同源（数字=明细合计），且 projA 明细翻页恰 2 行。
        assertThat(repository.staffShiftDetail(day, day, 200, 0, projA).rows()).hasSize(2);
    }

    @Test
    void arOpenReceivableDetailIsolatesByProjectId() {
        LocalDate asOf = LocalDate.parse("2026-06-20");
        LocalDate due = asOf.minusDays(10);
        String projA = proj("arA");
        String projB = proj("arB");
        BigDecimal baseAll = repository.arOpenReceivableDetail(asOf, 1, 0, null).summaryValue();
        insertReceivable(proj("ar_a1"), 5000, due, projA);
        insertReceivable(proj("ar_a2"), 3000, due, projA);
        insertReceivable(proj("ar_b1"), 9999, due, projB);

        // projA 只见自己两笔合计 8000；绝不含 B 的 9999（否则合计会变 17999 → 炸）。
        assertThat(repository.arOpenReceivableDetail(asOf, 200, 0, projA).summaryValue()).isEqualByComparingTo("8000");
        assertThat(repository.arOpenReceivableDetail(asOf, 200, 0, projB).summaryValue()).isEqualByComparingTo("9999");
        // 平台看全量：净增 5000+3000+9999。
        assertThat(repository.arOpenReceivableDetail(asOf, 200, 0, null).summaryValue().subtract(baseAll))
                .isEqualByComparingTo("17999");
        assertThat(repository.arOpenReceivableDetail(asOf, 200, 0, projA).rows()).hasSize(2);
    }

    @Test
    void settlementTransactionDetailIsolatesByProjectId() {
        String projA = proj("setA");
        String projB = proj("setB");
        BigDecimal baseAll = repository.settlementTransactionDetail(null, null, "count", 1, 0, null).summaryValue();
        insertSettlement(proj("set_a1"), projA);
        insertSettlement(proj("set_a2"), projA);
        insertSettlement(proj("set_b1"), projB);

        // projA 只见自己 2 笔；平台看全量净增 3。
        assertThat(repository.settlementTransactionDetail(null, null, "count", 200, 0, projA).summaryValue())
                .isEqualByComparingTo("2");
        assertThat(repository.settlementTransactionDetail(null, null, "count", 200, 0, projB).summaryValue())
                .isEqualByComparingTo("1");
        assertThat(repository.settlementTransactionDetail(null, null, "count", 200, 0, null).summaryValue().subtract(baseAll))
                .isEqualByComparingTo("3");
    }

    @Test
    void staffActiveHeadcountDetailIsolatesByOrganizationId() {
        // access_users 用 organization_id 作综合体列（AccessService.listUsers(organizationId) 同谓词）。
        LocalDate asOf = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        String orgA = proj("orgA");
        String orgB = proj("orgB");
        BigDecimal baseAll = repository.staffActiveHeadcountDetail(asOf, 1, 0, null).summaryValue();
        insertActiveStaff(proj("hc_a1"), orgA);
        insertActiveStaff(proj("hc_a2"), orgA);
        insertActiveStaff(proj("hc_b1"), orgB);

        assertThat(repository.staffActiveHeadcountDetail(asOf, 200, 0, orgA).summaryValue()).isEqualByComparingTo("2");
        assertThat(repository.staffActiveHeadcountDetail(asOf, 200, 0, orgB).summaryValue()).isEqualByComparingTo("1");
        assertThat(repository.staffActiveHeadcountDetail(asOf, 200, 0, null).summaryValue().subtract(baseAll))
                .isEqualByComparingTo("3");
    }

    // ---- 种子（均显式落 project_id / organization_id）----

    private void insertShift(String id, LocalDate workDate, String projectId) {
        jdbcTemplate.update("""
                INSERT INTO staff_shift_schedules (id, user_id, work_date, start_at, end_at, project_id, created_at, updated_at)
                VALUES (:id, :id, :work_date, :start_at, :end_at, :project_id, now(), now())
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("work_date", workDate)
                .addValue("start_at", java.sql.Timestamp.from(workDate.atTime(9, 0).toInstant(ZoneOffset.UTC)))
                .addValue("end_at", java.sql.Timestamp.from(workDate.atTime(18, 0).toInstant(ZoneOffset.UTC)))
                .addValue("project_id", projectId));
    }

    private void insertReceivable(String id, long unpaidCent, LocalDate dueDate, String projectId) {
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, amount_cent, paid_amount_cent,
                        unpaid_amount_cent, currency, due_date, status, project_id, created_at, updated_at, merchant_id)
                VALUES (:id, :id, 'receivable', 'cust_ps', :amount, 0, :amount, 'CNY', :due, 'open', :project_id, now(), now(), NULL)
                """, new MapSqlParameterSource().addValue("id", id).addValue("amount", unpaidCent)
                .addValue("due", dueDate).addValue("project_id", projectId));
    }

    private void insertSettlement(String id, String projectId) {
        java.sql.Timestamp at = java.sql.Timestamp.from(LocalDate.parse("2026-06-20").atTime(10, 0).toInstant(ZoneOffset.UTC));
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                        entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                        verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                        settlement_status, entitlement_valid_to, created_at, updated_at)
                VALUES (:id, :id, 'verification_settled', :id, 'ticket', 1, 1, 1, 100,
                        100, 0, 'merchant', 'mch_ps', :project_id, 'draft', :at, :at, :at)
                """, new MapSqlParameterSource().addValue("id", id).addValue("project_id", projectId).addValue("at", at));
    }

    private void insertActiveStaff(String id, String organizationId) {
        jdbcTemplate.update("""
                INSERT INTO access_users (id, username, account_type, organization_id, status, created_at)
                VALUES (:id, :id, 'staff', :organization_id, 'active', now())
                """, new MapSqlParameterSource().addValue("id", id).addValue("organization_id", organizationId));
    }
}
