package com.complex.platform.access.staff;

// 文件版本：V0.04

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
    @Autowired private StaffPayrollService payroll;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired private com.complex.platform.finance.FinancePayrollService bankPayroll;
    @Autowired private com.complex.platform.evidence.EvidenceEventService evidence;

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
        PayrollApprovedCloseFixture.prepare(jdbcTemplate,json,user,month,2000000);
        var original=PayrollApprovedCloseFixture.closeWithApprovedZeroTax(jdbcTemplate,json,payroll,user,month,"monthly");
        assertThat(statementTotal(month, user)).isEqualTo(2000000L);

        // 发放:落 staff_payroll_payouts + 状态置 paid。
        assertThatThrownBy(()->service.markPayrollPaid(new com.complex.platform.access.staff.dto.StaffPayrollPayRequest(key(),month,user,"bank_transfer","TXN-001"))).isInstanceOf(ApiException.class);
        payActually(original.statement().id(),user);
        assertThat(statementStatus(month, user)).isEqualTo("paid");

        // 事后补一笔 5,000 元罚款再重结:已发放的这行金额必须原封不动(套现被拒/跳过覆盖)。
        bookAdjustment(user, month, "penalty", 500000L, "事后补罚"); // CG-12：奖惩入账改走复核链，此处直接落已生效记录
        assertThatThrownBy(()->PayrollApprovedCloseFixture.closeWithApprovedZeroTax(jdbcTemplate,json,payroll,user,month,"monthly")).isInstanceOf(ApiException.class);

        assertThat(statementTotal(month, user)).isEqualTo(2000000L);   // 未被静默改写
        assertThat(statementStatus(month, user)).isEqualTo("paid");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM staff_payroll_payouts WHERE month = ? AND user_id = ?",
                Integer.class, month, user)).isEqualTo(1);
    }

    @Test
    void closingUnpaidMonthKeepsOriginalAndCreatesSupplement() {
        // 已定格原件不因未付款就覆盖，后到奖金单列补发。
        String month = "2099-12";
        String user = "usr_unpaid_" + key();
        seedProfile("project_main", user);
        jdbcTemplate.update("INSERT INTO staff_base_salaries (user_id, monthly_salary_cent, updated_at, project_id) "
                + "VALUES (?, 1000000, now(), 'project_main')", user);
        PayrollApprovedCloseFixture.prepare(jdbcTemplate,json,user,month,1000000);
        var original=PayrollApprovedCloseFixture.closeWithApprovedZeroTax(jdbcTemplate,json,payroll,user,month,"monthly");
        assertThat(statementTotal(month, user)).isEqualTo(1000000L);
        assertThat(statementStatus(month, user)).isEqualTo("closed");

        bookAdjustment(user, month, "bonus", 300000L, "补录奖金");
        var supplement=PayrollApprovedCloseFixture.closeWithApprovedZeroTax(jdbcTemplate,json,payroll,user,month,"supplemental");
        assertThat(statementTotal(month, user)).isEqualTo(1300000L);
        assertThat(supplement.statement().totalCent()).isEqualTo(300000);
        assertThat(jdbcTemplate.queryForObject("SELECT total_cent FROM staff_payroll_statements WHERE id=?",Long.class,original.statement().id())).isEqualTo(1000000);
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
        // CG-12：入账发生在复核通过时（同一仓储写口），业务键去重仍由 V239 唯一索引保证。
        bookAdjustment("usr_admin", month, "bonus", 800000L, "手工奖金去重");
        bookAdjustment("usr_admin", month, "bonus", 800000L, "手工奖金去重");
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

    private void payActually(String statement,String employee) {
        jdbcTemplate.update("INSERT INTO role_permissions(role_id,permission_code) SELECT 'role_m010_fixture',permission_code FROM permissions WHERE permission_code LIKE 'finance.payroll.%' OR permission_code='evidence.attachment.create' ON CONFLICT DO NOTHING");
        long total=jdbcTemplate.queryForObject("SELECT total_cent FROM staff_payroll_statements WHERE id=?",Long.class,statement);
        String id=(String)bankPayroll.prepare(statement,new com.complex.platform.finance.FinancePayrollService.PrepareRequest(key(),total)).get("id");
        String proof=evidence.uploadAttachment(new org.springframework.mock.web.MockMultipartFile("file","bank.png","image/png",java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jCYQAAAAASUVORK5CYII=")),"staff_payroll_payment",id,"bank_proof").id();
        var at=Instant.now().minusSeconds(60);
        bankPayroll.submit(id,new com.complex.platform.finance.FinancePayrollService.SubmitRequest(key(),"request_"+id,at,employee,List.of(proof)));
        bankPayroll.result(id,new com.complex.platform.finance.FinancePayrollService.ResultRequest(key(),"succeeded",at.plusSeconds(1),"flow_"+id,"测试实际银行成功原件",List.of(proof)));
    }

    private void bookAdjustment(String userId, String month, String adjType, long amountCent, String reason) {
        jdbcTemplate.update("""
                INSERT INTO staff_payroll_adjustments (id, user_id, month, adj_type, amount_cent, reason, created_by, created_at, project_id)
                VALUES (?, ?, ?, ?, ?, ?, 'usr_admin', now(),
                        (SELECT p.project_id FROM staff_profiles p WHERE p.user_id = ? ORDER BY p.created_at DESC LIMIT 1))
                ON CONFLICT DO NOTHING
                """, "adj_" + key(), userId, month, adjType, amountCent, reason, userId);
    }

    private void seedProfile(String projectId, String userId) {
        jdbcTemplate.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, company_id,
                        registration_status, status, created_at, updated_at)
                VALUES (?, ?, NULL, ?, 'scoped', ?, 'approved', 'active', now(), now())
                """, "stf_" + key(), projectId, userId, projectId);
    }

    private Long statementTotal(String month, String userId) {
        return jdbcTemplate.queryForObject(
                "SELECT sum(total_cent) FROM staff_payroll_statements WHERE month = ? AND user_id = ?",
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
        StaffTestActors.alignRowToSession(jdbcTemplate); // XM-M001-AF：账号行与会话一致
    }
}
