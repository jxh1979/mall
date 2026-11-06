package com.complex.platform.finance;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.finance.dto.ArApAdjustmentDecisionRequest;
import com.complex.platform.finance.dto.ArApAdjustmentRequest;
import com.complex.platform.finance.dto.CloseReconciliationDifferenceRequest;
import com.complex.platform.finance.dto.IdentifyReconciliationDifferenceRequest;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.settlement.InternalPayDayService;
import com.complex.platform.support.InternalPayDayFixtures;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 第十八批第 13 条（BSM 第 1597 行「凡涉及商户应收应付金额发生变化的事项一律短信通知，包括结算金额出账与调整、对账差异认定结果、
 * 押金抵扣与退还、违约金计提……通知中写明金额、原因与异议截止日期」；第 1598 行不可关闭）：财务端的调整、对账差异认定、押金、违约金
 * 业务提交后逐笔给商户发通知（总开关关着也发）；往来对方不是商户的不发；押金收取不是金额变化通知。通知在业务提交后才发，本类不包事务。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class MerchantAmountNoticeFinanceJdbcIntegrationTests {

    @Autowired FinanceService finance;
    @Autowired FinanceReconciliationDifferenceService differences;
    @Autowired SupervisorReviewService reviews;
    @Autowired JdbcFinanceRepository repository;
    @Autowired AccessRepository access;
    @Autowired JdbcTemplate jdbc;
    @Autowired PiiCipher cipher;
    @Autowired MockMvc mockMvc;
    @Autowired WorkingDayCalendar calendar;
    @Autowired InternalPayDayService payDays;

    String suffix, project, merchant, phone, financeA, financeB, platformFinance;

    @BeforeEach
    void seed() {
        InternalPayDayFixtures.seed(jdbc);
        suffix = UUID.randomUUID().toString().substring(0, 8);
        project = "mamt_p_" + suffix;
        merchant = "mamt_m_" + suffix;
        phone = "139" + String.format("%08d", Math.floorMod(suffix.hashCode(), 100_000_000));
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','金额通知项目','active',now(),now())", project);
        jdbc.update("""
                INSERT INTO merchants(id,merchant_name,merchant_type,project_id,contact_name,contact_phone,status,created_at,updated_at)
                VALUES(?,'金额通知商户','bar',?,'主要联系人',?,'active',now(),now())
                """, merchant, project, cipher.encrypt(phone));
        String role = "mamt_role_" + suffix;
        access.createRole(new RoleRecord(role, "金额通知项目财务-" + Long.parseLong(suffix, 16), null, "project", "active", Instant.now(),
                List.of("finance.ar_ap.view", "finance.deposit.manage", "finance.locked_backfill.request", "finance.locked_backfill.approve",
                        "finance.reconciliation.view", "finance.reconciliation.resolve", "governance.supervisor_review.approve"),
                null, false, List.of()));
        financeA = user(role);
        financeB = user(role);
        // 对账数据为平台级数据（FinanceService.requirePlatformReconciliationAccess），认定由平台财务办理。
        platformFinance = "mamt_pu_" + UUID.randomUUID();
        access.createUser(new AccessUser(platformFinance, platformFinance, "platform_admin", "org_1", null, "active", Instant.now()));
        access.assignUserRole(platformFinance, role);
        act(financeA);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 前端真实请求形状（蛇形键）：押金扣除、退还各发一条且带金额、原因、异议截止日（可查看之日次日起算本项目异议期）；收取不发。 */
    @Test
    void depositActionsNotifyDeductAndRefundNotCollect() throws Exception {
        deposit("collect", 10_000L, "进场押金");
        assertThat(notices()).isEmpty();

        deposit("deduct", 3_000L, "装修损坏");
        deposit("refund", 2_000L, "部分退还");

        List<Map<String, Object>> rows = notices();
        assertThat(rows).extracting(r -> r.get("event_key")).containsExactly("deposit_deduct", "deposit_refund");
        assertThat(rows).extracting(r -> r.get("amount")).containsExactly("3000", "2000");
        assertThat((String) rows.get(0).get("reason")).contains("押金扣除").contains("装修损坏");
        assertThat((String) rows.get(1).get("reason")).contains("押金退还").contains("部分退还");
        assertThat(rows).extracting(r -> r.get("deadline")).containsOnly(expectedDeadline());
        assertThat(rows.get(0).get("template_code")).isEqualTo("sms.merchant_amount.deposit_deduct");
        assertThat(cipher.decrypt((String) rows.get(0).get("recipient"))).isEqualTo(phone);
    }

    /** 提前退租结算落账端口：违约金计提、押金抵扣、应退押金各一条；面积/位置变更押金差额补缴与应退各一条。 */
    @Test
    void terminationAndDepositAdjustmentPortsNotify() {
        jdbc.update("""
                INSERT INTO deposit_ledger(id,merchant_id,action,amount_cent,balance_after_cent,ar_ap_item_id,reason,created_at)
                VALUES(?,?,'collect',20000,20000,NULL,'进场押金',now())
                """, "dep_" + suffix, merchant);
        String settlement = "cts_" + suffix;
        String penalty = finance.createTerminationPenaltyReceivable(merchant, 8_000L, settlement);
        finance.offsetReceivablesWithDeposit(merchant, settlement, List.of(penalty), 8_000L);
        long refunded = finance.createDepositRefundPayable(merchant, 12_000L, settlement, LocalDate.now(LocalZone.ID).plusDays(30));
        finance.registerDepositAdjustment(merchant, "ctc_" + suffix, "chg1_" + suffix, 5_000L, LocalDate.now(LocalZone.ID).plusDays(15));
        finance.registerDepositAdjustment(merchant, "ctc_" + suffix, "chg2_" + suffix, -3_000L, LocalDate.now(LocalZone.ID).plusDays(15));

        assertThat(refunded).isEqualTo(12_000L);
        List<Map<String, Object>> rows = notices();
        assertThat(rows).extracting(r -> r.get("event_key") + ":" + r.get("amount"))
                .containsExactly("penalty:8000", "deposit_deduct:8000", "deposit_refund:12000", "deposit_supplement:5000", "deposit_refund:3000");
        assertThat((String) rows.get(0).get("reason")).contains("提前退租违约金");
        assertThat((String) rows.get(1).get("reason")).contains("押金抵扣");
        assertThat(rows).extracting(r -> r.get("deadline")).containsOnly(expectedDeadline());
    }

    /** 锁账补录审批通过（结算金额调整）通知商户；驳回不发；往来对方不是商户的不发。 */
    @Test
    void adjustmentApprovalNotifiesMerchantOnly() {
        String approved = adjustment(lockedItem(merchant, merchant), "approve");
        adjustment(lockedItem(merchant, merchant), "reject");
        adjustment(lockedItem("mamt_supplier_" + suffix, null), "approve");

        List<Map<String, Object>> rows = notices();
        assertThat(rows).singleElement().satisfies(r -> {
            assertThat(r.get("event_key")).isEqualTo("adjustment");
            assertThat(r.get("request_id")).isEqualTo("mamt:adjustment:" + approved);
            assertThat(r.get("amount")).isEqualTo("30");
            assertThat((String) r.get("reason")).contains("调整：核实少记");
            assertThat(r.get("deadline")).isEqualTo(expectedDeadline());
        });
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notification_send_log WHERE request_id LIKE 'mamt:adjustment:%' AND params->>'merchant_id'=?",
                Integer.class, "mamt_supplier_" + suffix)).isZero();
    }

    /**
     * 对账差异认定（第十八批 Y 段第 181 条 181-R1 起：识别、提交关闭复核、提交人上级审批通过后关闭）通知所涉商户，原因含差异类型与认定结果；
     * 不在异议期适用清单，不写异议截止日；已关闭不能再提交复核、不重发。
     */
    @Test
    void reconciliationDifferenceCloseNotifiesMerchantWithoutDeadline() {
        String difference = difference(paidPayment(merchant));
        String foreign = difference(paidPayment("mamt_other_" + suffix));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", platformFinance, financeB);

        approvedClose(difference, "银行流水核对后以平台口径为准");
        approvedClose(foreign, "非商户往来");
        actAsPlatformFinance();
        assertThatThrownBy(() -> differences.submitReview(difference,
                new CloseReconciliationDifferenceRequest(key(), "再次关闭", List.of(), null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("RECONCILIATION_DIFFERENCE_STATUS_CONFLICT");

        assertThat(notices()).singleElement().satisfies(r -> {
            assertThat(r.get("event_key")).isEqualTo("reconciliation_difference");
            assertThat(r.get("request_id")).isEqualTo("mamt:reconciliation_difference:" + difference);
            assertThat(r.get("amount")).isEqualTo("700");
            assertThat((String) r.get("reason")).contains("金额差异").contains("认定结果：银行流水核对后以平台口径为准");
            assertThat(r.get("deadline")).isNull();
        });
    }

    // ---- helpers ----

    private void deposit(String action, long amount, String reason) throws Exception {
        // 押金台账按商户归属办理：平台财务（无商户归属的项目员工会被要求商户范围）。
        AccessActor actor = new AccessActor(platformFinance, platformFinance, "platform_admin", "org_1", null, "active");
        mockMvc.perform(post("/api/v1/finance/deposits/actions")
                        .with(authentication(new UsernamePasswordAuthenticationToken(actor, null, List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))))
                        .header("X-Terminal-Type", "web")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"" + key() + "\",\"merchant_id\":\"" + merchant + "\",\"action\":\"" + action
                                + "\",\"amount_cent\":" + amount + ",\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk());
    }

    /** 已锁账的往来（本类不包事务：锁账日用固定旧日期 1999-12-31，不把今天锁住而影响其他用例在今天记账）。 */
    private String lockedItem(String counterparty, String merchantColumn) {
        jdbc.update("""
                INSERT INTO finance_daily_closes(id,business_date,scope_type,scope_id,status,locked_at,created_at)
                VALUES(?,DATE '1999-12-31','merchant',?,'locked',now(),now()) ON CONFLICT (business_date,scope_type,scope_id) DO NOTHING
                """, "mamt_close_" + UUID.randomUUID().toString().substring(0, 12), counterparty);
        String close = jdbc.queryForObject(
                "SELECT id FROM finance_daily_closes WHERE business_date=DATE '1999-12-31' AND scope_type='merchant' AND scope_id=?",
                String.class, counterparty);
        String item = "mamt_lock_" + UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id,locked_day_close_id)
                VALUES(?,?,'receivable',?,'manual',?,100,0,100,'CNY',current_date,'2026-10','pending_collection',now(),now(),?,?,?)
                """, item, "MAMT-" + item, counterparty, item, merchantColumn, project, close);
        return item;
    }

    private String adjustment(String item, String decision) {
        act(financeA);
        var requested = finance.requestAdjustment(new ArApAdjustmentRequest(key(), "锁账后补录", List.of(), item, 30L, "核实少记"));
        act(financeB);
        finance.approveAdjustment(requested.id(), new ArApAdjustmentDecisionRequest(key(), "独立复核", List.of(), decision));
        act(financeA);
        return requested.id();
    }

    private String paidPayment(String counterparty) {
        String item = "mamt_paid_" + UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id)
                VALUES(?,?,'receivable',?,'manual',?,100,100,0,'CNY',current_date,'2026-10','settled',now(),now(),NULL,?)
                """, item, "MAMT-" + item, counterparty, item, project);
        String payment = "mamt_pay_" + UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,bank_flow_ref,created_at)
                VALUES(?,?,100,'bank_transfer',now(),'FLOW-'||?,now())
                """, payment, item, payment);
        return payment;
    }

    private String difference(String payment) {
        String batch = "mamt_rb_" + UUID.randomUUID().toString().substring(0, 12);
        repository.saveReconciliationBatch(new ReconciliationBatchRecord(batch, "RB-" + batch, "project", project,
                LocalDate.now(LocalZone.ID), "created", Instant.now()));
        String id = "mamt_rd_" + UUID.randomUUID().toString().substring(0, 12);
        repository.saveReconciliationDifference(new ReconciliationDifferenceRecord(id, batch, "amount", payment, 700L, "pending_identification",
                Instant.now()));
        return id;
    }

    /** 181-R1：平台财务识别、提交关闭复核，其直接上级（项目财务 B）审批通过。 */
    private void approvedClose(String difference, String conclusion) {
        actAsPlatformFinance();
        differences.identify(difference, new IdentifyReconciliationDifferenceRequest(key(), "银行流水与渠道对账单逐笔核对"));
        String reviewId = differences.submitReview(difference, new CloseReconciliationDifferenceRequest(key(), conclusion, List.of(), null))
                .reviewId();
        act(financeB);
        reviews.approve(reviewId, new SupervisorReviewApprovalRequest(key(), null, List.of(), null, "approved", "已核对银行流水"));
    }

    private void actAsPlatformFinance() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(platformFinance, platformFinance, "platform_admin", "org_1", null, "active"), "test", List.of()));
    }

    private List<Map<String, Object>> notices() {
        return jdbc.queryForList("""
                SELECT event_key, request_id, template_code, recipient, params->>'amount_cent' AS amount, params->>'reason' AS reason,
                       params->>'objection_deadline' AS deadline
                  FROM notification_send_log WHERE domain='merchant_amount' AND params->>'merchant_id'=? ORDER BY created_at, id
                """, merchant);
    }

    private String expectedDeadline() {
        return calendar.objectionDeadline(LocalDate.now(LocalZone.ID), payDays.objectionPolicy(project).workingDays(), project).toString();
    }

    private String user(String role) {
        String id = "mamt_u_" + UUID.randomUUID();
        access.createUser(new AccessUser(id, id, "staff", project, null, "active", Instant.now()));
        access.assignUserRole(id, role);
        return id;
    }

    private void act(String id) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(id, id, "staff", project, null, "active"), "test", List.of()));
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
