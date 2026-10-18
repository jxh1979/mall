package com.complex.platform.dealer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.StaffLevelAssignment;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.dealer.dto.DealerCancelRequest;
import com.complex.platform.dealer.dto.DealerCatalogDecisionRequest;
import com.complex.platform.dealer.dto.DealerCatalogDraftRequest;
import com.complex.platform.dealer.dto.DealerCatalogLineRequest;
import com.complex.platform.dealer.dto.DealerContractRequest;
import com.complex.platform.dealer.dto.DealerContractTermsRequest;
import com.complex.platform.dealer.dto.DealerCreateRequest;
import com.complex.platform.dealer.dto.DealerOrderDecisionRequest;
import com.complex.platform.dealer.dto.DealerOrderDraftRequest;
import com.complex.platform.dealer.dto.DealerOrderLineRequest;
import com.complex.platform.dealer.dto.DealerOverpaymentUseRequest;
import com.complex.platform.dealer.dto.DealerPaymentRequest;
import com.complex.platform.dealer.dto.DealerPaymentVerifyRequest;
import com.complex.platform.dealer.dto.DealerRefundExecuteRequest;
import com.complex.platform.dealer.dto.DealerReviewRequest;
import com.complex.platform.dealer.dto.DealerShipRequest;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * CG-11b（BSM:1226-1234 经销订单、付款与履约；ENUM:173 订单十五态、176 支付方式、177 库存预留、178 职责分离、179 取消/到账互斥、180 价格与税务快照；
 * E-13＝BSM:1231「到首付款截止时仍未达到合同门槛的订单取消，释放尚未出库的预留库存；已收部分按既有退款审批原路退回，取消后迟到的款项同样进入待退款；
 * 退款完成后订单回到已取消」）。真实 PostgreSQL（Flyway 全量迁移），@Transactional 回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class DealerOrderPaymentFulfillmentJdbcIntegrationTests {

    @Autowired private DealerService dealerService;
    @Autowired private DealerOrderService orders;
    @Autowired private SupervisorReviewService reviews;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.complex.platform.evidence.EvidenceEventService admissionFiles;
    @Autowired private AccessRepository access;
    @Autowired private PlatformTransactionManager transactionManager;

    private String suffix;
    private String sales;       // 建单 / 登记付款
    private String reviewer;    // 订单审核
    private String finance;     // 到账核验
    private String warehouse;   // 仓库出库 / 退款执行
    private String refundApprover; // 既有退款审批链上的审批人（finance 的直接上级）
    private String salesEntity;
    private String centralWarehouse;
    private String dealerId;
    private String creditContract;
    private String fullPayContract;
    private String itemA;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        sales = "usr_do_sales_" + suffix;
        reviewer = "usr_do_rev_" + suffix;
        finance = "usr_do_fin_" + suffix;
        warehouse = "usr_do_wh_" + suffix;
        refundApprover = "usr_do_rfa_" + suffix;
        String role = "role_do_" + suffix;
        jdbc.update("INSERT INTO roles(id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '经销订单测试', 'platform', 'active', now())",
                role, "经销订单测试角色" + System.nanoTime());
        jdbc.update("INSERT INTO role_permissions(role_id, permission_code) SELECT ?, permission_code FROM permissions WHERE permission_code LIKE 'dealer.%' OR permission_code = 'governance.supervisor_review.approve' ON CONFLICT DO NOTHING", role);
        for (String user : List.of(sales, reviewer, finance, warehouse, refundApprover)) {
            access.createUser(new AccessUser(user, user, "platform_admin", "project_main", null, "active", Instant.now()));
            access.assignUserRole(user, role);
        }
        // 既有退款审批：finance 的直接上级为 refundApprover（指定最终审批人，持审批角色且职级可解析）
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)", finance, refundApprover, sales);
        access.saveStaffLevelAssignment(new StaffLevelAssignment(refundApprover, "lvl_m3_lower", null, "CNY", null, "project_main", null, sales, Instant.now()));
        salesEntity = "be_do_" + suffix;
        jdbc.update("INSERT INTO business_entities(id, entity_name, entity_role, status, created_at, updated_at) VALUES (?, '经销销售主体', 'project_company', 'active', now(), now())", salesEntity);
        centralWarehouse = "wh_do_" + suffix;
        jdbc.update("INSERT INTO warehouses(id, project_id, warehouse_type, name, status) VALUES (?, 'project_main', 'main', '经销中心仓', 'active')", centralWarehouse);
        itemA = "item_do_a_" + suffix;
        jdbc.update("INSERT INTO inventory_items(id, item_name, item_type, unit, trace_required, min_quantity, status, created_at, updated_at) VALUES (?, '经销威士忌', 'beverage', '箱', false, 2, 'active', now(), now())", itemA);
        jdbc.update("INSERT INTO inventory_stock_balances(id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, warehouse_id, project_id) VALUES (?, ?, ?, 100, 0, 100, now(), ?, 'project_main')",
                "bal_do_" + suffix, itemA, centralWarehouse, centralWarehouse);

        actAs(sales);
        DealerRecord dealer = dealerService.createDealer(new DealerCreateRequest(key(), "订单经销商" + suffix, "9131" + System.nanoTime(), "法人", "联系人", "13900000000"));
        DealerTestDocuments.submit(dealerService, admissionFiles, dealer.id(), key());
        actAs(reviewer);
        dealerService.reviewDealer(dealer.id(), new DealerReviewRequest(key(), "approve", true, true, true, true, "通过"));
        dealerId = dealer.id();
        actAs(sales);
        creditContract = contractWithTerms("DC-CREDIT-" + suffix, true);
        fullPayContract = contractWithTerms("DC-FULL-" + suffix, false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 审核通过预留库存并固化目录版本、通知付款建应收；首付款达门槛才可出库；审核 / 核验 / 出库不得同人；签收不等于结清；全款到账才完成。 */
    @Test
    void orderFlowReservesStockFixesCatalogVersionAndEnforcesSeparationOfDuties() {
        actAs(sales);
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), creditContract,
                List.of(new DealerOrderLineRequest(itemA, "箱", 10, null)), "首单"));
        assertThat(draft.status()).isEqualTo("draft");
        assertThat(draft.goodsAmountCent()).isEqualTo(10_000L);
        assertThat(draft.freightCent()).as("未达包邮门槛，按合同运费模板收运费").isEqualTo(1_000L);
        assertThat(draft.totalAmountCent()).isEqualTo(11_000L);
        DealerOrderRecord pending = orders.submitOrder(draft.id(), key());
        assertThat(pending.status()).isEqualTo("pending_review");
        assertThat(balance("reserved_quantity")).isZero();

        actAs(reviewer);
        DealerOrderRecord notified = orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "合同、价格、库存、交期均核"));
        assertThat(notified.status()).isEqualTo("pending_payment");
        assertThat(notified.priceCatalogId()).isEqualTo(effectiveCatalogId(creditContract));
        assertThat(notified.firstPaymentRequiredCent()).as("首付款 30%").isEqualTo(3_300L);
        assertThat(notified.firstPaymentDeadlineAt()).isAfter(Instant.now().plus(2, ChronoUnit.DAYS));
        assertThat(balance("reserved_quantity")).isEqualTo(10);
        assertThat(balance("available_quantity")).isEqualTo(90);
        Map<String, Object> ar = jdbc.queryForMap("SELECT direction, source_type, amount_cent, unpaid_amount_cent, status, counterparty_id FROM ar_ap_items WHERE id = ?", notified.arApItemId());
        assertThat(ar).containsEntry("direction", "receivable").containsEntry("source_type", "dealer_order").containsEntry("amount_cent", 11_000L)
                .containsEntry("status", "open").containsEntry("counterparty_id", dealerId);

        actAs(sales);
        DealerOrderPaymentRecord p1 = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer", 3_000L, Instant.now(), "TXN-1"));
        assertThat(p1.status()).isEqualTo("pending_verification");
        assertThat(orders.getOrder(draft.id()).status()).isEqualTo("payment_verifying");
        actAs(reviewer);
        assertThatThrownBy(() -> orders.verifyPayment(p1.id(), new DealerPaymentVerifyRequest(key(), "approve", "核验")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_SOD_SAME_PERSON"));
        actAs(finance);
        orders.verifyPayment(p1.id(), new DealerPaymentVerifyRequest(key(), "approve", "核验"));
        DealerOrderRecord afterP1 = orders.getOrder(draft.id());
        assertThat(afterP1.paidAmountCent()).isEqualTo(3_000L);
        assertThat(afterP1.status()).as("未达首付款门槛不得出库").isEqualTo("pending_payment");
        actAs(warehouse);
        assertThatThrownBy(() -> orders.shipOrder(draft.id(), new DealerShipRequest(key(), "出库")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_FIRST_PAYMENT_NOT_MET"));

        actAs(sales);
        DealerOrderPaymentRecord p2 = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "platform_online", 500L, Instant.now(), "TXN-2"));
        actAs(finance);
        orders.verifyPayment(p2.id(), new DealerPaymentVerifyRequest(key(), "approve", "核验"));
        assertThat(orders.getOrder(draft.id()).status()).isEqualTo("pending_shipment");
        assertThatThrownBy(() -> orders.shipOrder(draft.id(), new DealerShipRequest(key(), "出库")))
                .as("到账核验人不得出库").isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_SOD_SAME_PERSON"));
        actAs(reviewer);
        assertThatThrownBy(() -> orders.shipOrder(draft.id(), new DealerShipRequest(key(), "出库")))
                .as("订单审核人不得出库").isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_SOD_SAME_PERSON"));
        actAs(warehouse);
        DealerOrderRecord shipping = orders.shipOrder(draft.id(), new DealerShipRequest(key(), "出库"));
        assertThat(shipping.status()).isEqualTo("shipping");
        assertThat(shipping.balanceDueAt()).isNotNull();
        assertThat(balance("quantity")).isEqualTo(90);
        assertThat(balance("reserved_quantity")).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inventory_stock_movements WHERE business_type = 'dealer_order' AND business_id = ? AND quantity = 10", Integer.class, draft.id())).isEqualTo(1);

        actAs(sales);
        DealerOrderRecord signed = orders.signOrder(draft.id(), key());
        assertThat(signed.status()).as("签收不表示货款已结清").isEqualTo("signed");
        assertThat(jdbc.queryForObject("SELECT unpaid_amount_cent FROM ar_ap_items WHERE id = ?", Long.class, draft.arApItemId() == null ? notified.arApItemId() : draft.arApItemId())).isEqualTo(7_500L);
        DealerOrderPaymentRecord p3 = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer", 7_500L, Instant.now(), "TXN-3"));
        actAs(finance);
        orders.verifyPayment(p3.id(), new DealerPaymentVerifyRequest(key(), "approve", "尾款核验"));
        DealerOrderRecord done = orders.getOrder(draft.id());
        assertThat(done.status()).isEqualTo("completed");
        assertThat(done.paidAmountCent()).isEqualTo(11_000L);
        assertThat(jdbc.queryForMap("SELECT unpaid_amount_cent, status FROM ar_ap_items WHERE id = ?", notified.arApItemId()))
                .containsEntry("unpaid_amount_cent", 0L).containsEntry("status", "settled");
        assertThat(jdbc.queryForObject("SELECT price_source FROM dealer_order_lines WHERE order_id = ?", String.class, draft.id())).isEqualTo("catalog");
    }

    /** E-13：首付款截止未达门槛 → 取消、释放预留；已收部分走既有退款审批原路退回，迟到款进待退款；退款执行人≠到账核验人；退款完成回到已取消。 */
    @Test
    void firstPaymentDeadlineCancelsReleasesReservationAndRefundsThroughExistingApproval() {
        actAs(sales);
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), creditContract,
                List.of(new DealerOrderLineRequest(itemA, "箱", 5, null)), "E-13"));
        orders.submitOrder(draft.id(), key());
        actAs(reviewer);
        DealerOrderRecord notified = orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "通过"));
        assertThat(notified.totalAmountCent()).isEqualTo(6_000L);
        assertThat(notified.firstPaymentRequiredCent()).isEqualTo(1_800L);
        assertThat(balance("reserved_quantity")).isEqualTo(5);
        actAs(sales);
        DealerOrderPaymentRecord partial = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer", 1_000L, Instant.now(), "TXN-P"));
        actAs(finance);
        orders.verifyPayment(partial.id(), new DealerPaymentVerifyRequest(key(), "approve", "核验"));
        assertThat(orders.getOrder(draft.id()).status()).isEqualTo("pending_payment");

        // 截止前不动
        assertThat(orders.cancelUnpaidAtDeadline(notified.firstPaymentDeadlineAt().minusSeconds(60))).isZero();
        assertThat(orders.getOrder(draft.id()).status()).isEqualTo("pending_payment");
        // 截止后取消：释放预留、已收 1000 进待退款
        assertThat(orders.cancelUnpaidAtDeadline(notified.firstPaymentDeadlineAt().plusSeconds(1))).isEqualTo(1);
        DealerOrderRecord cancelled = orders.getOrder(draft.id());
        assertThat(cancelled.status()).isEqualTo("pending_refund");
        assertThat(balance("reserved_quantity")).isZero();
        assertThat(balance("available_quantity")).isEqualTo(100);
        List<DealerOrderRefundRecord> refunds = orders.listRefunds(draft.id()).items();
        assertThat(refunds).hasSize(1);
        assertThat(refunds.get(0).amountCent()).isEqualTo(1_000L);
        assertThat(refunds.get(0).status()).isEqualTo("pending_approval");
        assertThat(refunds.get(0).reviewId()).isNotBlank();

        // 取消后迟到的款项同样进入待退款（不计入订单已收，不恢复订单）；由财务登记，退款审批链按财务的直接上级生成
        actAs(finance);
        DealerOrderPaymentRecord late = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer", 500L, Instant.now(), "TXN-LATE"));
        assertThat(late.status()).isEqualTo("late");
        assertThat(orders.getOrder(draft.id()).status()).isEqualTo("pending_refund");
        List<DealerOrderRefundRecord> twoRefunds = orders.listRefunds(draft.id()).items();
        assertThat(twoRefunds).hasSize(2);

        // 既有退款审批（refund 策略：直接上级节点 + 角色节点）：逐节点通过直至整单通过
        for (DealerOrderRefundRecord refund : twoRefunds) {
            actAs(refundApprover);
            for (int i = 0; i < 4 && "pending_review".equals(reviews.findReview(refund.reviewId()).orElseThrow().reviewStatus()); i++) {
                reviews.approve(refund.reviewId(), new SupervisorReviewApprovalRequest(key(), "退款审批", List.of(), null, "approved", "同意退款"));
            }
            assertThat(reviews.findReview(refund.reviewId()).orElseThrow().reviewStatus()).isEqualTo("approved");
        }
        assertThat(orders.listRefunds(draft.id()).items()).extracting(DealerOrderRefundRecord::status).containsOnly("approved");
        DealerOrderRefundRecord first = orders.listRefunds(draft.id()).items().stream().filter(r -> r.amountCent() == 1_000L).findFirst().orElseThrow();
        actAs(finance);
        assertThatThrownBy(() -> orders.executeRefund(first.id(), new DealerRefundExecuteRequest(key(), "RF-1")))
                .as("退款执行人与到账核验人不得为同一人").isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_SOD_SAME_PERSON"));
        actAs(warehouse);
        for (DealerOrderRefundRecord refund : orders.listRefunds(draft.id()).items()) {
            orders.executeRefund(refund.id(), new DealerRefundExecuteRequest(key(), "RF-" + refund.id()));
        }
        assertThat(orders.listRefunds(draft.id()).items()).extracting(DealerOrderRefundRecord::status).containsOnly("refunded");
        assertThat(orders.getOrder(draft.id()).status()).as("退款完成后订单回到已取消").isEqualTo("cancelled");
        assertThat(jdbc.queryForObject("SELECT status FROM ar_ap_items WHERE id = ?", String.class, notified.arApItemId())).isEqualTo("cancelled");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name = 'dealer.order.deadline_cancelled' AND business_id = ?", Integer.class, draft.id())).isEqualTo(1);
    }

    /** ENUM:179 取消审批与到账核验只能有一项先完成；取消审批生效后不得出库；取消审批人≠申请人。 */
    @Test
    void cancelApprovalAndPaymentVerificationAreMutuallyExclusiveAndNoShipmentAfterCancel() {
        actAs(sales);
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), fullPayContract,
                List.of(new DealerOrderLineRequest(itemA, "箱", 2, null)), "互斥"));
        orders.submitOrder(draft.id(), key());
        actAs(reviewer);
        DealerOrderRecord notified = orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "通过"));
        assertThat(notified.firstPaymentRequiredCent()).as("未约定账期＝全款门槛").isEqualTo(notified.totalAmountCent());
        actAs(sales);
        DealerOrderPaymentRecord p1 = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer", notified.totalAmountCent(), Instant.now(), "TXN-F"));
        assertThatThrownBy(() -> orders.requestCancel(draft.id(), new DealerCancelRequest(key(), "不要了")))
                .as("到账核验进行中不得发起取消").isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_PAYMENT_VERIFYING"));
        actAs(finance);
        orders.verifyPayment(p1.id(), new DealerPaymentVerifyRequest(key(), "approve", "全款到账"));
        assertThat(orders.getOrder(draft.id()).status()).isEqualTo("pending_shipment");

        actAs(sales);
        DealerOrderRecord cancelling = orders.requestCancel(draft.id(), new DealerCancelRequest(key(), "客户取消"));
        assertThat(cancelling.status()).isEqualTo("cancelling");
        DealerOrderPaymentRecord p2 = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer", 100L, Instant.now(), "TXN-X"));
        actAs(finance);
        assertThatThrownBy(() -> orders.verifyPayment(p2.id(), new DealerPaymentVerifyRequest(key(), "approve", "核验")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_CANCEL_IN_PROGRESS"));
        actAs(warehouse);
        assertThatThrownBy(() -> orders.shipOrder(draft.id(), new DealerShipRequest(key(), "出库")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_CANCEL_IN_PROGRESS"));
        actAs(sales);
        assertThatThrownBy(() -> orders.decideCancel(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "自审")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_SOD_SAME_PERSON"));
        actAs(reviewer);
        DealerOrderRecord afterCancel = orders.decideCancel(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "同意取消"));
        assertThat(afterCancel.status()).as("已到账订单取消须走退款审批").isEqualTo("pending_refund");
        assertThat(balance("reserved_quantity")).isZero();
        // 已核验的全款 + 取消时尚未核验的 100 分（按迟到款）都进入待退款
        assertThat(orders.listRefunds(draft.id()).items()).hasSize(2);
        actAs(warehouse);
        assertThatThrownBy(() -> orders.shipOrder(draft.id(), new DealerShipRequest(key(), "出库")))
                .as("取消审批生效后不得出库").isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_ORDER_STATUS_INVALID"));

        // 驳回取消申请：回到取消前状态
        actAs(sales);
        DealerOrderRecord second = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), fullPayContract,
                List.of(new DealerOrderLineRequest(itemA, "箱", 1, null)), "再来一单"));
        orders.submitOrder(second.id(), key());
        actAs(reviewer);
        orders.reviewOrder(second.id(), new DealerOrderDecisionRequest(key(), "approve", "通过"));
        actAs(sales);
        orders.requestCancel(second.id(), new DealerCancelRequest(key(), "误操作"));
        actAs(reviewer);
        assertThat(orders.decideCancel(second.id(), new DealerOrderDecisionRequest(key(), "reject", "继续履约")).status()).isEqualTo("pending_payment");
    }

    /** ENUM:176 只支持对公转账、平台线上支付（禁止现金和个人账户）；库 CHECK 拒绝闭集外的支付方式与订单状态（本例不触发服务层异常）。 */
    @Test
    void paymentMethodsAndOrderStatusAreClosedSets() {
        actAs(sales);
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), fullPayContract,
                List.of(new DealerOrderLineRequest(itemA, "箱", 1, null)), "闭集"));
        TransactionTemplate nested = new TransactionTemplate(transactionManager);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        for (String illegal : List.of("shipped", "paid", "approved")) {
            assertThatThrownBy(() -> nested.executeWithoutResult(s -> jdbc.update("UPDATE dealer_orders SET status = ? WHERE id = ?", illegal, draft.id())))
                    .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chk_dealer_orders_status");
        }
        for (String illegal : List.of("cash", "personal_account", "wechat_personal")) {
            assertThatThrownBy(() -> nested.executeWithoutResult(s -> jdbc.update(
                    "INSERT INTO dealer_order_payments (id, order_id, method, amount_cent, received_at, status, registered_by, created_at, updated_at) VALUES (?, ?, ?, 100, now(), 'pending_verification', 'x', now(), now())",
                    "pay_bad_" + illegal + suffix, draft.id(), illegal)))
                    .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chk_dealer_order_payments_method");
        }
    }

    @Test
    void cashAndPersonalAccountPaymentsAreRejectedBeforeRecording() {
        actAs(sales);
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), fullPayContract,
                List.of(new DealerOrderLineRequest(itemA, "箱", 1, null)), "现金"));
        orders.submitOrder(draft.id(), key());
        actAs(reviewer);
        orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "通过"));
        actAs(sales);
        for (String forbidden : List.of("cash", "personal_account")) {
            assertThatThrownBy(() -> orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), forbidden, 100L, Instant.now(), "X")))
                    .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PAYMENT_METHOD_FORBIDDEN"));
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dealer_order_payments WHERE order_id = ?", Integer.class, draft.id())).isZero();
    }

    /** BSM:1231 尾声：已出库订单尾款逾期 → 应收转逾期、暂停该经销商新增账期订单，仍允许全款订单；结清后解除；已发货订单不自动取消、不释放库存。 */
    @Test
    void overdueBalanceSuspendsNewCreditOrdersButAllowsFullPaymentOrders() {
        actAs(sales);
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), creditContract,
                List.of(new DealerOrderLineRequest(itemA, "箱", 4, null)), "账期单"));
        orders.submitOrder(draft.id(), key());
        actAs(reviewer);
        DealerOrderRecord notified = orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "通过"));
        actAs(sales);
        DealerOrderPaymentRecord first = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer", notified.firstPaymentRequiredCent(), Instant.now(), "TXN-1"));
        actAs(finance);
        orders.verifyPayment(first.id(), new DealerPaymentVerifyRequest(key(), "approve", "首付"));
        actAs(warehouse);
        DealerOrderRecord shipping = orders.shipOrder(draft.id(), new DealerShipRequest(key(), "出库"));
        assertThat(orders.markOverdue(shipping.balanceDueAt().minusSeconds(1))).isZero();
        assertThat(orders.markOverdue(shipping.balanceDueAt().plusSeconds(1))).isEqualTo(1);
        DealerOrderRecord overdue = orders.getOrder(draft.id());
        assertThat(overdue.overdue()).isTrue();
        assertThat(overdue.status()).as("已发货订单不自动取消").isEqualTo("shipping");
        assertThat(jdbc.queryForObject("SELECT status FROM ar_ap_items WHERE id = ?", String.class, notified.arApItemId())).isEqualTo("overdue");
        assertThat(jdbc.queryForObject("SELECT credit_suspended FROM dealers WHERE id = ?", Boolean.class, dealerId)).isTrue();

        actAs(sales);
        DealerOrderRecord creditDraft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), creditContract,
                List.of(new DealerOrderLineRequest(itemA, "箱", 1, null)), "逾期期间账期单"));
        assertThatThrownBy(() -> orders.submitOrder(creditDraft.id(), key()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_CREDIT_SUSPENDED"));
        DealerOrderRecord fullDraft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), fullPayContract,
                List.of(new DealerOrderLineRequest(itemA, "箱", 1, null)), "逾期期间全款单"));
        assertThat(orders.submitOrder(fullDraft.id(), key()).status()).isEqualTo("pending_review");

        DealerOrderPaymentRecord balance = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer", notified.totalAmountCent() - notified.firstPaymentRequiredCent(), Instant.now(), "TXN-B"));
        actAs(finance);
        orders.verifyPayment(balance.id(), new DealerPaymentVerifyRequest(key(), "approve", "尾款"));
        assertThat(orders.getOrder(draft.id()).overdue()).isFalse();
        assertThat(jdbc.queryForObject("SELECT credit_suspended FROM dealers WHERE id = ?", Boolean.class, dealerId)).isFalse();
        actAs(sales);
        assertThat(orders.submitOrder(creditDraft.id(), key()).status()).isEqualTo("pending_review");
    }

    /** BSM:1228 超收：单独记录尚未确认用途的金额，不自动抵账、不增加发货数量；用途确认后转预付款，同一金额不得重复抵用。 */
    @Test
    void overpaymentIsRecordedSeparatelyAndNeverAutoApplied() {
        actAs(sales);
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), fullPayContract,
                List.of(new DealerOrderLineRequest(itemA, "箱", 3, null)), "超收"));
        orders.submitOrder(draft.id(), key());
        actAs(reviewer);
        DealerOrderRecord notified = orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "通过"));
        actAs(sales);
        DealerOrderPaymentRecord over = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer", notified.totalAmountCent() + 2_500L, Instant.now(), "TXN-OVER"));
        actAs(finance);
        orders.verifyPayment(over.id(), new DealerPaymentVerifyRequest(key(), "approve", "超收"));
        DealerOrderRecord paid = orders.getOrder(draft.id());
        assertThat(paid.paidAmountCent()).isEqualTo(notified.totalAmountCent());
        assertThat(paid.status()).isEqualTo("pending_shipment");
        assertThat(jdbc.queryForObject("SELECT quantity FROM dealer_order_lines WHERE order_id = ?", Integer.class, draft.id())).isEqualTo(3);
        List<DealerOrderOverpaymentRecord> overs = orders.listOverpayments(dealerId).items();
        assertThat(overs).hasSize(1);
        assertThat(overs.get(0).amountCent()).isEqualTo(2_500L);
        assertThat(overs.get(0).status()).isEqualTo("unconfirmed");
        assertThat(jdbc.queryForMap("SELECT paid_amount_cent, unpaid_amount_cent FROM ar_ap_items WHERE id = ?", notified.arApItemId()))
                .containsEntry("paid_amount_cent", notified.totalAmountCent()).containsEntry("unpaid_amount_cent", 0L);
        DealerOrderOverpaymentRecord confirmed = orders.confirmOverpaymentUse(overs.get(0).id(), new DealerOverpaymentUseRequest(key(), "prepayment", null, "双方确认转预付款"));
        assertThat(confirmed.status()).isEqualTo("prepayment");
        assertThatThrownBy(() -> orders.confirmOverpaymentUse(overs.get(0).id(), new DealerOverpaymentUseRequest(key(), "prepayment", null, "再确认")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("OVERPAYMENT_ALREADY_CONFIRMED"));
    }

    @Test
    void contractEditsAfterOrderApprovalCannotChangeItsBalanceDeadline() {
        actAs(sales);
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), creditContract,
                List.of(new DealerOrderLineRequest(itemA, "箱", 3, null)), "合同锁版"));
        orders.submitOrder(draft.id(), key());
        actAs(reviewer);
        DealerOrderRecord approved = orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "通过"));
        actAs(sales);
        orders.updateContractTerms(creditContract, new DealerContractTermsRequest(key(), true, 5000, null, 7, 90, 2_000L, 50_000L, "dealer", null, null, null, "payment_notified", null, "shipped", null));
        DealerOrderPaymentRecord payment = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer",
                approved.firstPaymentRequiredCent(), Instant.now(), "LOCKED-TERMS"));
        actAs(finance);
        orders.verifyPayment(payment.id(), new DealerPaymentVerifyRequest(key(), "approve", "核实"));
        actAs(warehouse);
        DealerOrderRecord shipped = orders.shipOrder(draft.id(), new DealerShipRequest(key(), "出库"));
        assertThat(shipped.balanceDueAt()).isEqualTo(shipped.shippedAt().plusSeconds(30L * 86400));
        assertThat(shipped.firstPaymentDeadlineAt()).isEqualTo(approved.firstPaymentDeadlineAt());
        assertThat(shipped.freightCent()).isEqualTo(1_000L);
    }

    @Test
    void contractSigningAndReceiptAnchorsUseTheirFactsAndLockedDays() {
        actAs(sales);
        Instant signing = Instant.now().minusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        DealerContractRecord terms = orders.updateContractTerms(creditContract, new DealerContractTermsRequest(key(), true, 3000, null,
                3, 30, 1_000L, 50_000L, "dealer", null, null, signing, "contract_signed", null, "signed", null));
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), creditContract,
                List.of(new DealerOrderLineRequest(itemA, "箱", 3, null)), "按合同签约与签收起算"));
        orders.submitOrder(draft.id(), key());
        actAs(reviewer);
        DealerOrderRecord approved = orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "核对合同"));
        assertThat(approved.firstPaymentDeadlineAt()).isEqualTo(signing.plusSeconds(3L * 86400));
        assertThat(approved.contractSnapshot().termsVersion()).isEqualTo(terms.termsVersion());
        assertThat(approved.balanceDueAt()).isNull();
        actAs(sales);
        DealerOrderPaymentRecord payment = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer", approved.firstPaymentRequiredCent(), Instant.now(), "SIGNED-TERMS"));
        actAs(finance);
        orders.verifyPayment(payment.id(), new DealerPaymentVerifyRequest(key(), "approve", "到账"));
        assertThat(jdbc.queryForObject("SELECT due_date FROM ar_ap_items WHERE id = ?", java.sql.Date.class, approved.arApItemId())).isNull();
        actAs(sales);
        orders.updateContractTerms(creditContract, new DealerContractTermsRequest(key(), true, 5000, null,
                7, 90, 2_000L, 50_000L, "dealer", null, null, signing, "contract_signed", null, "shipped", null));
        actAs(warehouse);
        assertThat(orders.shipOrder(draft.id(), new DealerShipRequest(key(), "出库")).balanceDueAt()).isNull();
        actAs(sales);
        DealerOrderRecord signed = orders.signOrder(draft.id(), key());
        assertThat(signed.balanceDueAt()).isEqualTo(signed.signedAt().plusSeconds(30L * 86400));
        assertThat(signed.contractSnapshot().termsVersion()).isEqualTo(terms.termsVersion());
        assertThat(jdbc.queryForObject("SELECT due_date FROM ar_ap_items WHERE id = ?", java.sql.Date.class, approved.arApItemId()))
                .isEqualTo(java.sql.Date.valueOf(signed.balanceDueAt().atZone(java.time.ZoneOffset.UTC).toLocalDate()));
    }

    @Test
    void explicitFixedContractDeadlinesRemainUnchangedAfterShipping() {
        actAs(sales);
        Instant first = Instant.now().plusSeconds(3L * 86400).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        Instant balance = first.plusSeconds(30L * 86400);
        orders.updateContractTerms(creditContract, new DealerContractTermsRequest(key(), true, null, 1_000L,
                null, null, 1_000L, 50_000L, "dealer", null, null, null, "fixed_date", first, "fixed_date", balance));
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), creditContract,
                List.of(new DealerOrderLineRequest(itemA, "箱", 3, null)), "固定日期"));
        orders.submitOrder(draft.id(), key());
        actAs(reviewer);
        DealerOrderRecord approved = orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "核对"));
        assertThat(approved.firstPaymentDeadlineAt()).isEqualTo(first);
        assertThat(approved.balanceDueAt()).isEqualTo(balance);
        actAs(sales);
        DealerOrderPaymentRecord payment = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer", 1_000L, Instant.now(), "FIXED-TERMS"));
        actAs(finance);
        orders.verifyPayment(payment.id(), new DealerPaymentVerifyRequest(key(), "approve", "到账"));
        actAs(warehouse);
        assertThat(orders.shipOrder(draft.id(), new DealerShipRequest(key(), "出库")).balanceDueAt()).isEqualTo(balance);
    }

    @Test
    void paymentTermsRejectMissingOriginsAndAmbiguousFirstPayment() {
        assertThatThrownBy(() -> orders.updateContractTerms(creditContract, new DealerContractTermsRequest(key(), true, 3000, null,
                3, 30, 1_000L, 50_000L, "dealer", null, null, null, null, null, "shipped", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> orders.updateContractTerms(creditContract, new DealerContractTermsRequest(key(), true, 3000, 1_000L,
                3, 30, 1_000L, 50_000L, "dealer", null, null, null, "payment_notified", null, "shipped", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    // ── helpers ──

    private String contractWithTerms(String no, boolean creditTerm) {
        DealerContractRecord contract = dealerService.createContract(dealerId, new DealerContractRequest(key(), no, salesEntity, centralWarehouse,
                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)));
        orders.updateContractTerms(contract.id(), new DealerContractTermsRequest(key(), creditTerm, 3000, null, 3, 30, 1_000L, 50_000L, "dealer", null, null, null, "payment_notified", null, "shipped", null));
        DealerPriceCatalogRecord v1 = dealerService.createCatalogDraft(contract.id(), new DealerCatalogDraftRequest(key(), "目录",
                List.of(new DealerCatalogLineRequest(itemA, "箱", 1_000L, 1300, null))));
        dealerService.submitCatalog(v1.id(), key());
        actAs(reviewer);
        dealerService.decideCatalog(v1.id(), new DealerCatalogDecisionRequest(key(), "approve", "通过"));
        actAs(sales);
        return contract.id();
    }

    private String effectiveCatalogId(String contractId) {
        return jdbc.queryForObject("SELECT id FROM dealer_price_catalogs WHERE dealer_contract_id = ? AND status = 'effective'", String.class, contractId);
    }

    private int balance(String column) {
        return jdbc.queryForObject("SELECT " + column + " FROM inventory_stock_balances WHERE item_id = ? AND warehouse_id = ?", Integer.class, itemA, centralWarehouse);
    }

    private void actAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "platform_admin", "project_main", null, "active"), "test", List.of()));
    }

    private static String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
