package com.complex.platform.access.staff;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 薪资域资金安全修复（fix/payroll-safety）——TDD 红/绿。四项：
 *  1) 已发放(paid)工资单不得被重结静默改写(V238 状态机 + payouts 表)。
 *  2) 手工提成/奖惩业务级去重(V239 唯一约束),防同额同月不同幂等键翻倍。
 *  3) 关账/调薪高危授权:staff.payroll.manage requires_approval=TRUE(V240,与 tax_bracket.manage 同范式)。
 *  4) 个税税表同税目区间不重叠排他约束(V241),防重叠致月结 ON CONFLICT 崩溃(DoS)。
 * 走真实 Postgres,@Transactional 回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class PayrollSafetyJdbcIntegrationTests {

    @Autowired
    private StaffService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired com.complex.platform.access.AccessRepository access;

    @BeforeEach
    void setUp() {
        StaffTestActors.grant(jdbcTemplate,access,"usr_admin","project_main");
        authenticateAsPlatformAdmin();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── 1【P0】已关账/已发放工资单不得被重结静默改写 ────────────────────────────

    @Test
    void paidPayrollStatementIsNotSilentlyOverwrittenOnReclose() {
        String month = "2099-11";
        String user = "usr_paid_" + key();
        seedProfile("project_main", user);
        jdbcTemplate.update("INSERT INTO staff_base_salaries (user_id, monthly_salary_cent, updated_at, project_id) "
                + "VALUES (?, 2000000, now(), 'project_main')", user);

        // 首次关账 → total 2,000,000。
        service.closePayroll(new com.complex.platform.access.staff.dto.StaffPayrollCloseRequest(key(), month));
        assertThat(statementTotal(month, user)).isEqualTo(2000000L);

        // 发放:落 staff_payroll_payouts + 状态置 paid。
        service.markPayrollPaid(new com.complex.platform.access.staff.dto.StaffPayrollPayRequest(
                key(), month, user, "bank_transfer", "TXN-001"));
        assertThat(statementStatus(month, user)).isEqualTo("paid");

        // 事后补一笔 5,000 元罚款再重结:已发放的这行金额必须原封不动(套现被拒/跳过覆盖)。
        service.addPayrollAdjustment(new com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest(
                key(), user, month, "penalty", 500000L, "事后补罚"));
        service.closePayroll(new com.complex.platform.access.staff.dto.StaffPayrollCloseRequest(key(), month));

        assertThat(statementTotal(month, user)).isEqualTo(2000000L);   // 未被静默改写
        assertThat(statementStatus(month, user)).isEqualTo("paid");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_payroll_payouts WHERE month = ? AND user_id = ?",
                Integer.class, month, user)).isEqualTo(1);
    }

    @Test
    void closingUnpaidMonthStillRecomputesNormally() {
        // 未发放月份仍可重结覆盖(补录奖惩后重算),状态保持 closed。
        String month = "2099-12";
        String user = "usr_unpaid_" + key();
        seedProfile("project_main", user);
        jdbcTemplate.update("INSERT INTO staff_base_salaries (user_id, monthly_salary_cent, updated_at, project_id) "
                + "VALUES (?, 1000000, now(), 'project_main')", user);
        service.closePayroll(new com.complex.platform.access.staff.dto.StaffPayrollCloseRequest(key(), month));
        assertThat(statementTotal(month, user)).isEqualTo(1000000L);
        assertThat(statementStatus(month, user)).isEqualTo("closed");

        service.addPayrollAdjustment(new com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest(
                key(), user, month, "bonus", 300000L, "补录奖金"));
        service.closePayroll(new com.complex.platform.access.staff.dto.StaffPayrollCloseRequest(key(), month));
        assertThat(statementTotal(month, user)).isEqualTo(1300000L);   // 未发放照常重算
    }

    // ── 2【P0】手工提成/奖惩无去重 ───────────────────────────────────────────

    @Test
    void manualCommissionDedupedByBusinessKeyAcrossDifferentIdempotencyKeys() {
        String month = "2099-10";
        // 同 user/month/amount/note、不同幂等键提交两次 → 只入一条(业务级去重)。
        service.addCommission(new com.complex.platform.access.staff.dto.StaffCommissionRequest(
                key(), "usr_admin", month, "manual", null, 1000000L, "手工提成去重"));
        service.addCommission(new com.complex.platform.access.staff.dto.StaffCommissionRequest(
                key(), "usr_admin", month, "manual", null, 1000000L, "手工提成去重"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_commission_ledger WHERE user_id='usr_admin' AND month=? "
                        + "AND source_type='manual' AND note='手工提成去重'", Integer.class, month))
                .isEqualTo(1);
    }

    @Test
    void manualAdjustmentDedupedByBusinessKeyAcrossDifferentIdempotencyKeys() {
        String month = "2099-10";
        service.addPayrollAdjustment(new com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest(
                key(), "usr_admin", month, "bonus", 800000L, "手工奖金去重"));
        service.addPayrollAdjustment(new com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest(
                key(), "usr_admin", month, "bonus", 800000L, "手工奖金去重"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_payroll_adjustments WHERE user_id='usr_admin' AND month=? "
                        + "AND adj_type='bonus' AND amount_cent=800000 AND reason='手工奖金去重'",
                Integer.class, month)).isEqualTo(1);
    }

    // ── 3【P1】关账/调薪纳入高危授权审批 ─────────────────────────────────────

    @Test
    void payrollManagePermissionRequiresApprovalToGrant() {
        // 与 staff.tax_bracket.manage 同范式:高危薪资权限授出必须走审批链(requires_approval=TRUE)。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT requires_approval FROM permissions WHERE permission_code = 'staff.payroll.manage'",
                Boolean.class)).isTrue();
    }

    // ── 4【P2】个税税表档位不重叠排他约束 ────────────────────────────────────

    @Test
    void overlappingTaxBracketRangeForSameCategoryIsRejected() {
        // 与种子档1 [1,400000] 区间重叠的新档必须被 DB 排他约束拒绝(防月结 ON CONFLICT 命中多档崩溃)。
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO payroll_tax_brackets
                    (id, category, seq, income_floor_cent, income_ceiling_cent, income_base_bps, rate_bps)
                VALUES (?, 'labor_remuneration', 99, 200000, 500000, 8000, 2000)
                """, "ptb_overlap_" + key()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nonOverlappingTaxBracketForDifferentCategoryIsAccepted() {
        // 不同税目之间不受排他约束限制(约束按 category 分区)。
        jdbcTemplate.update("""
                INSERT INTO payroll_tax_brackets
                    (id, category, seq, income_floor_cent, income_ceiling_cent, income_base_bps, rate_bps)
                VALUES (?, 'other_category', 1, 200000, 500000, 8000, 2000)
                """, "ptb_other_" + key());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payroll_tax_brackets WHERE category='other_category'",
                Integer.class)).isEqualTo(1);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void seedProfile(String projectId, String userId) {
        jdbcTemplate.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id,
                        registration_status, status, created_at, updated_at)
                VALUES (?, ?, NULL, ?, 'scoped', ?, 'approved', 'active', now(), now())
                """, "stf_" + key(), projectId, userId, projectId);
    }

    private Long statementTotal(String month, String userId) {
        return jdbcTemplate.queryForObject(
                "SELECT total_cent FROM staff_payroll_statements WHERE month = ? AND user_id = ?",
                Long.class, month, userId);
    }

    private String statementStatus(String month, String userId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM staff_payroll_statements WHERE month = ? AND user_id = ?",
                String.class, month, userId);
    }

    private String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void authenticateAsPlatformAdmin() {
        AuthService.UserRecord user = new AuthService.UserRecord(
                "usr_admin", "seed-admin", "hash", "platform_admin", "seed-org", null, "active",
                Instant.parse("2026-06-14T00:00:00Z"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, "token", List.of(new SimpleGrantedAuthority("ROLE_P0_SMOKE"))));
    }
}
