package com.complex.platform.finance;

// 文件版本：V0.05

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
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
@AutoConfigureMockMvc
@Transactional
class InternalPayableExecutionJdbcIntegrationTests {

    private static final int[] PAY_DAYS = {5, 15, 25};

    @Autowired InternalPayableExecutionService execution;
    @Autowired FinanceService finance;
    @Autowired AccessRepository access;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mockMvc;

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
        // 第十八批 AM 段第 241 条 241-R1：出款前置——该应付已有通过的付款确认（夹具）。
        com.complex.platform.support.PaymentConfirmationFixture.approvedArAp(jdbc, item);
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

    // ---- 第十八批 AV 段 310-R1、310-R2：退租押金退还应付的付款执行状态（ENUM:252「押金退还支付不受三个付账日与账单异议期约束」
    //      「押金退还付款状态按「内部应付执行状态」条执行」；ENUM:253；BSM:1683）。真实 HTTP，项目财务无商户编号、只到本人项目。----

    /** 查看执行状态、登记已发起（在途不重付）、登记明确失败（预计重付日不受下一个付账日上限）、不适用资金不足顺延；确认到账结清并回写该次付款。 */
    @Test
    void depositRefundPaymentExecutionOverHttp() throws Exception {
        collectDeposit(10_000L);
        String refund = depositRefund("dr1", 6_000L, today);
        RequestPostProcessor fin = web(financeUser, null);

        mockMvc.perform(get(base(refund)).with(fin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source_type").value("deposit_refund"))
                .andExpect(jsonPath("$.data.execution_status").value("pending"))
                .andExpect(jsonPath("$.data.objection_deadline").doesNotExist())
                .andExpect(jsonPath("$.data.designated_pay_day").doesNotExist());
        postJson(base(refund) + "/payment-attempts", fin, "{\"idempotency_key\":\"" + key() + "\",\"bank_ref\":\"BANK-D1\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("initiated"));
        postJson(base(refund) + "/payment-attempts", fin, "{\"idempotency_key\":\"" + key() + "\",\"bank_ref\":\"BANK-D2\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("PAYMENT_IN_TRANSIT"));
        LocalDate repay = today.plusDays(40);
        postJson(base(refund) + "/payment-attempts/" + inTransitAttempt(refund) + "/failure", fin, "{\"idempotency_key\":\"" + key()
                + "\",\"failure_reason\":\"收款账户已注销\",\"expected_repay_date\":\"" + repay + "\"}")
                .andExpect(status().isOk());
        mockMvc.perform(get(base(refund)).with(fin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.execution_status").value("failed_pending"))
                .andExpect(jsonPath("$.data.failure_reason").value("收款账户已注销"))
                .andExpect(jsonPath("$.data.expected_repay_date").value(repay.toString()));
        postJson(base(refund) + "/funds-insufficient", fin, "{\"idempotency_key\":\"" + key() + "\",\"note\":\"账上资金不足\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("INTERNAL_PAYABLE_NOT_SCHEDULED"));

        postJson(base(refund) + "/payment-attempts", fin, "{\"idempotency_key\":\"" + key() + "\",\"bank_ref\":\"BANK-D3\"}")
                .andExpect(status().isOk());
        postJson("/api/v1/finance/ar-ap-items/" + refund + "/payments", fin, "{\"idempotency_key\":\"" + key()
                + "\",\"amount_cent\":6000,\"payment_method\":\"bank_transfer\",\"bank_flow_ref\":\"BANK-D3\"}")
                .andExpect(status().isOk());
        mockMvc.perform(get(base(refund)).with(fin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.execution_status").value("settled"))
                .andExpect(jsonPath("$.data.attempts[1].status").value("succeeded"));
    }

    /** 押金退还不早于到期日（等待期届满或退租结算算账完成之日）发起付款，与登记付款同码。 */
    @Test
    void depositRefundInitiateBeforeDueDateRejectedOverHttp() throws Exception {
        collectDeposit(10_000L);
        String refund = depositRefund("dr2", 2_000L, today.plusDays(1));

        postJson(base(refund) + "/payment-attempts", web(financeUser, null), "{\"idempotency_key\":\"" + key() + "\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("DEPOSIT_REFUND_NOT_DUE"));
    }

    /** 商户专区可见押金退还的付款失败待处理（原因与预计重付日）；收款方财务负责人审批更换收款账户后换账户重付；别家商户看不到、不能代批。 */
    @Test
    void merchantZoneShowsDepositRefundFailureAndPayeeHeadApprovesAccountChangeOverHttp() throws Exception {
        collectDeposit(10_000L);
        String refund = depositRefund("dr3", 6_000L, today);
        RequestPostProcessor fin = web(financeUser, null);
        postJson(base(refund) + "/payment-attempts", fin, "{\"idempotency_key\":\"" + key() + "\",\"bank_ref\":\"BANK-D1\"}")
                .andExpect(status().isOk());
        postJson(base(refund) + "/payment-attempts/" + inTransitAttempt(refund) + "/failure", fin, "{\"idempotency_key\":\"" + key()
                + "\",\"failure_reason\":\"收款账户已注销\",\"expected_repay_date\":\"" + today.plusDays(40) + "\"}")
                .andExpect(status().isOk());

        String row = "$.data[?(@.ar_ap_item_id == '" + refund + "')]";
        mockMvc.perform(get("/api/v1/merchant-portal/ticket-settlements").with(web(payeeHead, payee))).andExpect(status().isOk())
                .andExpect(jsonPath(row + ".source_type").value("deposit_refund"))
                .andExpect(jsonPath(row + ".execution_status").value("failed_pending"))
                .andExpect(jsonPath(row + ".failure_reason").value("收款账户已注销"))
                .andExpect(jsonPath(row + ".expected_repay_date").value(today.plusDays(40).toString()));
        mockMvc.perform(get("/api/v1/merchant-portal/ticket-settlements").with(web(otherHead, otherMerchant))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].ar_ap_item_id", not(hasItem(refund))));
        String change = "{\"idempotency_key\":\"%s\",\"new_account_ref\":\"新账户 6222****0003\",\"reason\":\"原账户已注销\"}";
        postJson("/api/v1/merchant-portal/ticket-settlements/" + refund + "/account-change", web(otherHead, otherMerchant),
                change.formatted(key()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        postJson("/api/v1/merchant-portal/ticket-settlements/" + refund + "/account-change", web(payeeHead, payee),
                change.formatted(key()))
                .andExpect(status().isOk());
        String changeId = jdbc.queryForObject("SELECT id FROM internal_payable_account_changes WHERE ar_ap_item_id=?", String.class, refund);
        postJson(base(refund) + "/payment-attempts", fin, "{\"idempotency_key\":\"" + key() + "\",\"bank_ref\":\"BANK-D2\","
                + "\"account_change_id\":\"" + changeId + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.account_change_id").value(changeId));
    }

    /**
     * 第十八批 AZ 段 358-R1（BSM:1683「处理路径为原路重付，或更换收款账户后重付；更换收款账户须经收款方所属公司财务负责人审批。……每笔应付的实际到账结果
     * 回写该笔应付并可逐笔追溯」）：付款失败待处理后收款方财务负责人在商户专区审批新收款账户，付款方财务经真实接口查看执行状态即见该换账户单（编号、新账户、
     * 原因、审批时刻），换新账户登记已发起付款时带该单编号，付款尝试记下所用的换账户单；押金退还应付同一流程。
     */
    @Test
    void retryToApprovedPayeeAccountOverHttp() throws Exception {
        retryToApprovedPayeeAccount(item, nextPayDayOnOrAfter(today.plusDays(1)));
        collectDeposit(10_000L);
        retryToApprovedPayeeAccount(depositRefund("dr358", 6_000L, today), today.plusDays(40));
    }

    private void retryToApprovedPayeeAccount(String itemId, LocalDate repay) throws Exception {
        RequestPostProcessor fin = web(financeUser, null);
        postJson(base(itemId) + "/payment-attempts", fin, "{\"idempotency_key\":\"" + key() + "\",\"bank_ref\":\"BANK-358-1\"}")
                .andExpect(status().isOk());
        postJson(base(itemId) + "/payment-attempts/" + inTransitAttempt(itemId) + "/failure", fin, "{\"idempotency_key\":\"" + key()
                + "\",\"failure_reason\":\"收款账户被冻结\",\"expected_repay_date\":\"" + repay + "\"}")
                .andExpect(status().isOk());
        postJson("/api/v1/merchant-portal/ticket-settlements/" + itemId + "/account-change", web(payeeHead, payee),
                "{\"idempotency_key\":\"" + key() + "\",\"new_account_ref\":\"新账户 6222****0358\",\"reason\":\"原账户被冻结\"}")
                .andExpect(status().isOk());
        String changeId = jdbc.queryForObject("SELECT id FROM internal_payable_account_changes WHERE ar_ap_item_id=?", String.class, itemId);

        mockMvc.perform(get(base(itemId)).with(fin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.account_changes[0].id").value(changeId))
                .andExpect(jsonPath("$.data.account_changes[0].new_account_ref").value("新账户 6222****0358"))
                .andExpect(jsonPath("$.data.account_changes[0].reason").value("原账户被冻结"))
                .andExpect(jsonPath("$.data.account_changes[0].approved_at").exists());
        postJson(base(itemId) + "/payment-attempts", fin, "{\"idempotency_key\":\"" + key() + "\",\"bank_ref\":\"BANK-358-2\","
                + "\"account_change_id\":\"" + changeId + "\"}")
                .andExpect(status().isOk());
        mockMvc.perform(get(base(itemId)).with(fin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attempts[1].account_change_id").value(changeId));
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

    private void collectDeposit(long cent) {
        jdbc.update("""
                INSERT INTO deposit_ledger(id,merchant_id,action,amount_cent,balance_after_cent,ar_ap_item_id,reason,created_at)
                VALUES(?,?,'collect',?,?,NULL,'进场押金',now())
                """, "ipe_dep_" + suffix, payee, cent, cent);
    }

    /** 经真实退租落账端口挂一笔押金退还应付（项目＝本测试项目，310-R0），并让付款确认通过（第十八批裁决 55）。 */
    private String depositRefund(String tag, long cent, LocalDate dueDate) {
        String settlement = "ipe_cts_" + tag + "_" + suffix;
        finance.createDepositRefundPayable(payee, cent, settlement, dueDate, project);
        String id = jdbc.queryForObject("SELECT id FROM ar_ap_items WHERE source_type='deposit_refund' AND source_id=?", String.class,
                settlement);
        com.complex.platform.support.PaymentConfirmationFixture.approvedArAp(jdbc, id);
        return id;
    }

    private String inTransitAttempt(String itemId) {
        return jdbc.queryForObject("SELECT id FROM internal_payable_payment_attempts WHERE ar_ap_item_id=? AND status='initiated'",
                String.class, itemId);
    }

    private static String base(String itemId) {
        return "/api/v1/finance/internal-payables/" + itemId;
    }

    private ResultActions postJson(String path, RequestPostProcessor who, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body).with(who));
    }

    /** 后台网页真实请求形状（X-Terminal-Type: web）的员工账号（已建好、组织＝本测试项目）。 */
    private RequestPostProcessor web(String userId, String merchantId) {
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
