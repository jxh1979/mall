package com.complex.platform.finance;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.EvidenceEventService;
import com.complex.platform.finance.dto.ArApAdjustmentDecisionRequest;
import com.complex.platform.finance.dto.ArApAdjustmentRequest;
import com.complex.platform.finance.dto.RefundExecutionRequest;
import com.complex.platform.finance.dto.RefundRequest;
import com.complex.platform.finance.dto.RefundResultConfirmRequest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * FS-021-R6（BSM L1673「所有数据默认按项目隔离，只有项目之上的获授权岗位可以跨项目汇总」；PGM L664 付款执行由财务中心负责；
 * 财务 PRD §4C）：共享财务退款、退款凭证、锁账补录按项目范围办理——项目财务没有商户归属也能办本项目的；他项目拒绝；
 * 商户账号仍只限本商户。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class FinanceProjectScopeRefundJdbcIntegrationTests {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jz1sAAAAASUVORK5CYII=");

    @Autowired FinanceService finance;
    @Autowired AccessRepository access;
    @Autowired EvidenceEventService evidence;
    @Autowired JdbcTemplate jdbc;

    String suffix, project, otherProject, merchant, otherMerchant, financeA, financeB, outsider, merchantStaff;

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        project = "psr_p_" + suffix;
        otherProject = "psr_q_" + suffix;
        merchant = "psr_m_" + suffix;
        otherMerchant = "psr_m2_" + suffix;
        for (String p : List.of(project, otherProject)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','退款范围项目','active',now(),now())", p);
        }
        for (String m : List.of(merchant, otherMerchant)) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at) VALUES(?,'退款范围商户','bar','active',now(),now())", m);
            // 商户账号办理财务动作须有生效合同准入（与采购回归同一夹具口径）。
            jdbc.update("""
                    INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,
                        allowed_business_scope,created_at,updated_at)
                    VALUES(?,?,'subj_psr','subj_psr',current_date-1,current_date+365,'effective','["finance"]'::jsonb,now(),now())
                    """, "psr_c_" + m, m);
        }
        String role = "psr_role_" + suffix;
        access.createRole(new RoleRecord(role, "项目财务", null, "project", "active", Instant.now(),
                List.of("finance.ar_ap.view", "finance.payment.register", "finance.refund.create",
                        "finance.locked_backfill.request", "finance.locked_backfill.approve", "evidence.attachment.create"),
                null, false, List.of()));
        financeA = user(project, null, role);
        financeB = user(project, null, role);
        outsider = user(otherProject, null, role);
        merchantStaff = user(project, otherMerchant, role);
        for (String u : List.of(financeA, financeB, outsider, merchantStaff)) {
            jdbc.update("""
                    INSERT INTO refund_authority_limits(id,subject_type,subject_id,refund_domain,limit_amount_cent,project_id,status)
                    VALUES(?,'staff',?,'finance',1000000,NULL,'active')
                    """, "ral_" + u, u);
        }
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 项目财务（无商户编号）按前端真实请求形状办理本项目已收款的原路退款。 */
    @Test
    void projectFinanceWithoutMerchantCreatesRefundOnOwnProjectPayment() {
        String payment = paidReceivable(project, merchant, false);
        act(financeA, project);

        Throwable error = catchThrowable(() -> finance.createRefund(refund(payment, 60L)));

        assertThat(error).as("项目财务无商户编号也能办本项目退款").isNull();
        assertThat(jdbc.queryForObject("SELECT project_id FROM finance_refunds WHERE target_id=?", String.class, payment))
                .isEqualTo(project);
    }

    @Test
    void projectFinanceCannotRefundOtherProject() {
        String payment = paidReceivable(project, merchant, false);
        act(outsider, otherProject);

        assertThatThrownBy(() -> finance.createRefund(refund(payment, 60L)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
    }

    @Test
    void merchantActorStillLimitedToOwnMerchant() {
        String payment = paidReceivable(project, merchant, false);
        act(merchantStaff, project, otherMerchant);

        assertThatThrownBy(() -> finance.createRefund(refund(payment, 60L)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
    }

    @Test
    void listRefundsReturnsOnlyReachableProjects() {
        String own = paidReceivable(project, merchant, false);
        String foreign = paidReceivable(otherProject, merchant, false);
        act(financeA, project);
        finance.createRefund(refund(own, 10L));
        act(outsider, otherProject);
        finance.createRefund(refund(foreign, 10L));

        act(financeA, project);
        List<FinanceRefundRecord> listed = finance.listFinanceRefunds().items();
        assertThat(listed).extracting(FinanceRefundRecord::targetId).contains(own).doesNotContain(foreign);
    }

    @Test
    void projectFinanceConfirmsRefundWithBoundEvidence() {
        String payment = paidReceivable(project, merchant, false);
        act(financeA, project);
        FinanceRefundRecord created = finance.createRefund(refund(payment, 60L));
        String reference = "RF-" + suffix;
        finance.recordFinanceRefundExecution(created.id(), new RefundExecutionRequest(key(), "沿原收款路径执行",
                List.of(proof(created.id())), reference, null));

        act(financeB, project);
        FinanceRefundRecord confirmed = finance.confirmFinanceRefundResult(created.id(), new RefundResultConfirmRequest(key(),
                "独立核对银行结果", List.of(proof(created.id())), "success", 60L, reference, financeA, null));

        assertThat(confirmed.status()).isEqualTo("success");
        assertThat(finance.getFinanceRefund(created.id()).refund().id()).isEqualTo(created.id());
    }

    @Test
    void lockedBackfillRequestAndApproveByTwoProjectFinanceUsers() {
        String item = lockedReceivable(project, merchant);
        act(financeA, project);
        Throwable error = catchThrowable(() -> {
            var adjustment = finance.requestAdjustment(new ArApAdjustmentRequest(key(), "锁账后补录", List.of(), item, 30L, "核实少记"));
            act(financeB, project);
            assertThat(finance.approveAdjustment(adjustment.id(), new ArApAdjustmentDecisionRequest(key(), "独立复核", List.of(), "approve"))
                    .approvalStatus()).isEqualTo("approved");
        });
        assertThat(error).as("项目财务无商户编号也能申请与审批本项目锁账补录").isNull();
        assertThat(jdbc.queryForObject("SELECT sum(amount_cent) FROM ar_ap_items WHERE id=? OR origin_item_id=?", Long.class, item, item))
                .isEqualTo(130L);
    }

    @Test
    void sameUserCannotApproveOwnBackfill() {
        String item = lockedReceivable(project, merchant);
        act(financeA, project);
        var adjustment = finance.requestAdjustment(new ArApAdjustmentRequest(key(), "锁账后补录", List.of(), item, 30L, "核实少记"));

        assertThatThrownBy(() -> finance.approveAdjustment(adjustment.id(),
                new ArApAdjustmentDecisionRequest(key(), "自审", List.of(), "approve")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SELF_APPROVAL_FORBIDDEN"));
    }

    // ---- helpers ----

    private RefundRequest refund(String payment, long amount) {
        // 前端 FinanceRefundPanel 提交 {target_id, refund_amount_cent, reason, refund_direction:"original"}。
        return new RefundRequest(key(), "核实原路退款", List.of(), payment, amount, "客户申请退款", null, "original");
    }

    /** 本项目商户已收款 100 的应收及其收款记录，返回收款记录编号。 */
    private String paidReceivable(String projectId, String merchantId, boolean locked) {
        String item = "psr_item_" + UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id)
                VALUES(?,?,'receivable',?,'manual',?,100,100,0,'CNY',current_date,'2026-10','settled',now(),now(),?,?)
                """, item, "PSR-" + item, merchantId, item, merchantId, projectId);
        String payment = "psr_pay_" + UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,bank_flow_ref,created_at)
                VALUES(?,?,100,'bank_transfer',now(),'FLOW-'||?,now())
                """, payment, item, payment);
        return payment;
    }

    private String lockedReceivable(String projectId, String merchantId) {
        String close = "psr_close_" + UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO finance_daily_closes(id,business_date,scope_type,scope_id,status,locked_at,created_at)
                VALUES(?,current_date,'merchant',?,'locked',now(),now())
                """, close, merchantId);
        String item = "psr_lock_" + UUID.randomUUID().toString().substring(0, 12);
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id,locked_day_close_id)
                VALUES(?,?,'receivable',?,'manual',?,100,0,100,'CNY',current_date,'2026-10','pending_collection',now(),now(),?,?,?)
                """, item, "PSR-" + item, merchantId, item, merchantId, projectId, close);
        return item;
    }

    private String proof(String refundId) {
        return evidence.uploadAttachment(new MockMultipartFile("file", "原路退款凭证.png", "image/png", PNG),
                "finance_refund", refundId, "payment_receipt").id();
    }

    private String user(String projectId, String merchantId, String role) {
        String id = "psr_u_" + UUID.randomUUID();
        access.createUser(new AccessUser(id, id, "staff", projectId, merchantId, "active", Instant.now()));
        access.assignUserRole(id, role);
        return id;
    }

    private void act(String id, String projectId) {
        act(id, projectId, null);
    }

    private void act(String id, String projectId, String merchantId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(id, id, "staff", projectId, merchantId, "active"), "test", List.of()));
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
