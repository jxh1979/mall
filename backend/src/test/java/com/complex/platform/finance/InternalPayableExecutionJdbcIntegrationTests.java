package com.complex.platform.finance;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.dto.ArApPaymentRequest;
import com.complex.platform.support.InternalPayDayFixtures;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * FS-021-R5 内部应付执行状态（BSM L1683；ENUM L253；DEC-P1B-07、DEC-P1C-02、DEC-R4-53）：在途保持待付且不重付；
 * 明确失败标「付款失败待处理」并注明原因与预计重付时间（不迟于下一个付账日）；资金不足顺延至下一个付账日、不另行通知、
 * 待付状态对收款方可见；更换收款账户须收款方所属公司财务负责人审批；只有确认到账才结清。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class InternalPayableExecutionJdbcIntegrationTests {

    private static final int[] PAY_DAYS = {5, 15, 25};

    @Autowired InternalPayableExecutionService execution;
    @Autowired FinanceService finance;
    @Autowired AccessRepository access;
    @Autowired JdbcTemplate jdbc;

    String suffix, project, payee, otherMerchant, item, payeeHead, payeeClerk, otherHead, financeUser;
    LocalDate today, due;

    @BeforeEach
    void seed() {
        InternalPayDayFixtures.seed(jdbc);
        suffix = UUID.randomUUID().toString().substring(0, 8);
        project = "ipe_p_" + suffix;
        payee = "ipe_m_" + suffix;
        otherMerchant = "ipe_m2_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','执行项目','active',now(),now())", project);
        for (String m : List.of(payee, otherMerchant)) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at) VALUES(?,'收款酒吧','bar','active',now(),now())", m);
        }
        today = LocalDate.now(LocalZone.ID);
        due = nextPayDayOnOrAfter(today);
        item = "ipe_item_" + suffix;
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id)
                VALUES(?,?,'payable',?,'ticket_distribution',?,5000,0,5000,'CNY',?,?,'pending_payment',now(),now(),NULL,?)
                """, item, "IPE-" + suffix, payee, item, due, YearMonth.from(due).toString(), project);
        jdbc.update("""
                INSERT INTO internal_payable_schedules(ar_ap_item_id,route,project_id,counterparty_id,billing_period_start,visible_at,
                    objection_deadline,designated_pay_day,scheduled_due_date,pay_days_rule_id,route_rule_id,objection_working_days)
                VALUES(?,'ticket_distribution',?,?,?,?,?,?,?,'ipdr_initial_pay_days','ipdr_fixture_ticket_distribution',10)
                """, item, project, payee, today.withDayOfMonth(1).minusMonths(1), Timestamp.from(Instant.now().minusSeconds(86400 * 20)),
                today.minusDays(1), due, due);
        String headRole = "ipe_head_" + suffix;
        access.createRole(new RoleRecord(headRole, "收款方财务负责人", null, "project", "active", Instant.now(),
                List.of("merchant.portal.payee_account.approve", "merchant.portal.settlement.view"), null, false, List.of()));
        String clerkRole = "ipe_clerk_" + suffix;
        access.createRole(new RoleRecord(clerkRole, "收款方店员", null, "project", "active", Instant.now(),
                List.of("merchant.portal.settlement.view"), null, false, List.of()));
        String financeRole = "ipe_fin_" + suffix;
        access.createRole(new RoleRecord(financeRole, "付款方财务", null, "project", "active", Instant.now(),
                List.of("finance.ar_ap.view", "finance.payment.register"), null, false, List.of()));
        financeUser = user(null, financeRole);
        payeeHead = user(payee, headRole);
        payeeClerk = user(payee, clerkRole);
        otherHead = user(otherMerchant, headRole);
        actAsFinance();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 已发起未确认到账属银行在途：保持待付、对收款方可见，不得再发起重付。 */
    @Test
    void inTransitStaysPendingAndBlocksRepay() {
        execution.initiate(item, new InternalPayableExecutionService.InitiateRequest(key(), "BANK-1", null));

        var state = execution.execution(item);
        assertThat(state.executionStatus()).isEqualTo("pending");
        assertThat(state.inTransit()).isTrue();
        assertThat(state.unpaidAmountCent()).isEqualTo(5000L);
        Throwable again = catchThrowable(() -> execution.initiate(item, new InternalPayableExecutionService.InitiateRequest(key(), "BANK-2", null)));
        assertThat(again).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PAYMENT_IN_TRANSIT"));
    }

    /** 异议期未届满的账单不进入付款。 */
    @Test
    void initiateBeforeObjectionExpiryRejected() {
        jdbc.update("UPDATE internal_payable_schedules SET objection_deadline=? WHERE ar_ap_item_id=?", today.plusDays(3), item);

        Throwable error = catchThrowable(() -> execution.initiate(item, new InternalPayableExecutionService.InitiateRequest(key(), "BANK-1", null)));

        assertThat(error).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("OBJECTION_PERIOD_NOT_EXPIRED"));
    }

    /** 明确失败：标「付款失败待处理」并注明原因与预计重付时间（不迟于下一个付账日），收款方可见；之后可原路重付。 */
    @Test
    void explicitFailureMarksFailedPendingWithReasonAndRepayNoLaterThanNextPayDay() {
        var attempt = execution.initiate(item, new InternalPayableExecutionService.InitiateRequest(key(), "BANK-1", null));
        LocalDate nextPayDay = nextPayDayOnOrAfter(today.plusDays(1));

        Throwable late = catchThrowable(() -> execution.fail(item, attempt.id(),
                new InternalPayableExecutionService.FailureRequest(key(), "收款账户被冻结", nextPayDay.plusDays(1))));
        assertThat(late).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("REPAY_AFTER_NEXT_PAY_DAY"));
        execution.fail(item, attempt.id(), new InternalPayableExecutionService.FailureRequest(key(), "收款账户被冻结", nextPayDay));

        var state = execution.execution(item);
        assertThat(state.executionStatus()).isEqualTo("failed_pending");
        assertThat(state.failureReason()).isEqualTo("收款账户被冻结");
        assertThat(state.expectedRepayDate()).isEqualTo(nextPayDay);
        actAsMerchant(payeeClerk, payee);
        assertThat(execution.merchantTicketSettlements()).singleElement()
                .satisfies(v -> assertThat(v.executionStatus()).isEqualTo("failed_pending"));
        actAsFinance();
        execution.initiate(item, new InternalPayableExecutionService.InitiateRequest(key(), "BANK-2", null));
        assertThat(execution.execution(item).inTransit()).isTrue();
    }

    /** 资金不足：顺延至下一个付账日并逐次留痕，不另行通知，收款方看到的仍是待付与新的付款日。 */
    @Test
    void fundsInsufficientPostponesToNextPayDayVisibleToPayeeWithoutNotification() {
        LocalDate expected = nextPayDayOnOrAfter(due.plusDays(1));

        execution.fundsInsufficient(item, new InternalPayableExecutionService.FundsInsufficientRequest(key(), "账上资金不足"));

        assertThat(jdbc.queryForObject("SELECT due_date FROM ar_ap_items WHERE id=?", LocalDate.class, item)).isEqualTo(expected);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM internal_payable_postponements WHERE ar_ap_item_id=? AND reason_code='funds_insufficient'",
                Integer.class, item)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification_send_log WHERE request_id LIKE ?", Integer.class, "%" + item + "%"))
                .isZero();
        actAsMerchant(payeeClerk, payee);
        assertThat(execution.merchantTicketSettlements()).singleElement().satisfies(v -> {
            assertThat(v.executionStatus()).isEqualTo("pending");
            assertThat(v.dueDate()).isEqualTo(expected);
        });
    }

    /** 更换收款账户后重付须经收款方所属公司财务负责人审批；他家或无此权限的人不能代批，未批的账户变更不能用于重付。 */
    @Test
    void accountChangeRepayRequiresPayeeCompanyFinanceHeadApproval() {
        var attempt = execution.initiate(item, new InternalPayableExecutionService.InitiateRequest(key(), "BANK-1", null));
        execution.fail(item, attempt.id(), new InternalPayableExecutionService.FailureRequest(key(), "收款账户错误",
                nextPayDayOnOrAfter(today.plusDays(1))));
        Throwable bogus = catchThrowable(() -> execution.initiate(item,
                new InternalPayableExecutionService.InitiateRequest(key(), "BANK-2", "ipac_not_approved")));
        assertThat(bogus).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ACCOUNT_CHANGE_NOT_APPROVED"));

        actAsMerchant(otherHead, otherMerchant);
        assertThat(catchThrowable(() -> execution.approveAccountChange(item,
                new InternalPayableExecutionService.AccountChangeRequest(key(), "新账户 6222****0001", "原账户错误"))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        actAsMerchant(payeeClerk, payee);
        assertThat(catchThrowable(() -> execution.approveAccountChange(item,
                new InternalPayableExecutionService.AccountChangeRequest(key(), "新账户 6222****0001", "原账户错误"))))
                .isInstanceOf(ApiException.class);
        actAsMerchant(payeeHead, payee);
        var change = execution.approveAccountChange(item,
                new InternalPayableExecutionService.AccountChangeRequest(key(), "新账户 6222****0001", "原账户错误"));

        actAsFinance();
        var repay = execution.initiate(item, new InternalPayableExecutionService.InitiateRequest(key(), "BANK-3", (String) change.get("id")));
        assertThat(repay.accountChangeId()).isEqualTo(change.get("id"));
    }

    /**
     * 平台月结单过账的往来，台账行无项目归属（P2 显式归属取账单项目），付款时点按收款方所属项目固化：收款方财务负责人照样能在
     * 专区看到、审批更换收款账户（BSM L1683「待付状态须在系统中对收款方可见」「更换收款账户须经收款方所属公司财务负责人审批」）。
     */
    @Test
    void payeeHeadApprovesAccountChangeOnPlatformBillItemScheduledUnderPayeeProject() {
        var attempt = execution.initiate(item, new InternalPayableExecutionService.InitiateRequest(key(), "BANK-1", null));
        execution.fail(item, attempt.id(), new InternalPayableExecutionService.FailureRequest(key(), "收款账户错误",
                nextPayDayOnOrAfter(today.plusDays(1))));
        jdbc.update("UPDATE ar_ap_items SET project_id=NULL WHERE id=?", item); // 平台单形态：台账行无项目归属

        actAsMerchant(payeeHead, payee);
        assertThat(execution.merchantTicketSettlements()).singleElement()
                .satisfies(v -> assertThat(v.executionStatus()).isEqualTo("failed_pending"));
        Throwable error = catchThrowable(() -> execution.approveAccountChange(item,
                new InternalPayableExecutionService.AccountChangeRequest(key(), "新账户 6222****0002", "原账户错误")));
        assertThat(error).as("按付款时点项目判定，不因台账行无项目而拒绝").isNull();
    }

    /** 只有确认到账才结清：在途时仍待付，登记到账后结清并回写该次付款。 */
    @Test
    void onlyConfirmedSuccessSettlesItem() {
        var attempt = execution.initiate(item, new InternalPayableExecutionService.InitiateRequest(key(), "BANK-1", null));
        assertThat(jdbc.queryForObject("SELECT unpaid_amount_cent FROM ar_ap_items WHERE id=?", Long.class, item)).isEqualTo(5000L);

        finance.registerArApPayment(item, new ArApPaymentRequest(key(), "银行回单确认到账", List.of(), 5000L, "bank_transfer",
                Instant.now(), "BANK-1"));

        assertThat(execution.execution(item).executionStatus()).isEqualTo("settled");
        assertThat(jdbc.queryForObject("SELECT status FROM internal_payable_payment_attempts WHERE id=?", String.class, attempt.id()))
                .isEqualTo("succeeded");
    }

    /**
     * V344 红冲范式（与 FinanceService.settleArApItem 同码 AR_AP_ITEM_REVERSED）：已有冲正/重述行回指的原行不再是有效款项——
     * 不得再登记已发起付款（钱不会先汇出再被拦），执行状态为 reversed，收款方专区不再显示为待付。
     */
    @Test
    void reversedOriginalItemCannotBeInitiatedAndShowsReversed() {
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id,origin_item_id)
                VALUES(?,?,'payable',?,'ticket_distribution',?,-5000,0,-5000,'CNY',?,?,'reversed',now(),now(),NULL,?,?)
                """, item + "_rev", "IPE-REV-" + suffix, payee, item, due, YearMonth.from(due).toString(), project, item);

        Throwable error = catchThrowable(() -> execution.initiate(item, new InternalPayableExecutionService.InitiateRequest(key(), "BANK-1", null)));

        assertThat(error).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AR_AP_ITEM_REVERSED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM internal_payable_payment_attempts WHERE ar_ap_item_id=?", Integer.class, item)).isZero();
        assertThat(execution.execution(item).executionStatus()).isEqualTo("reversed");
        actAsMerchant(payeeClerk, payee);
        assertThat(execution.merchantTicketSettlements()).singleElement()
                .satisfies(v -> assertThat(v.executionStatus()).isEqualTo("reversed"));
    }

    // ---- helpers ----

    private static LocalDate nextPayDayOnOrAfter(LocalDate from) {
        for (YearMonth m = YearMonth.from(from); ; m = m.plusMonths(1)) {
            for (int d : PAY_DAYS) {
                if (!m.atDay(d).isBefore(from)) {
                    return m.atDay(d);
                }
            }
        }
    }

    private String user(String merchantId, String role) {
        String id = "ipe_u_" + UUID.randomUUID();
        access.createUser(new AccessUser(id, id, "staff", project, merchantId, "active", Instant.now()));
        access.assignUserRole(id, role);
        return id;
    }

    private void actAsFinance() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(financeUser, financeUser, "staff", project, null, "active"), "test", List.of()));
    }

    private void actAsMerchant(String id, String merchantId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(id, id, "staff", project, merchantId, "active"), "test", List.of()));
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
