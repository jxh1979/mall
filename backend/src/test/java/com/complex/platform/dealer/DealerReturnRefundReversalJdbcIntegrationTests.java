package com.complex.platform.dealer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.StaffLevelAssignment;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.dealer.dto.DealerCatalogDecisionRequest;
import com.complex.platform.dealer.dto.DealerCatalogDraftRequest;
import com.complex.platform.dealer.dto.DealerCatalogLineRequest;
import com.complex.platform.dealer.dto.DealerContractRequest;
import com.complex.platform.dealer.dto.DealerContractTermsRequest;
import com.complex.platform.dealer.dto.DealerCreateRequest;
import com.complex.platform.dealer.dto.DealerInvoiceIssueRequest;
import com.complex.platform.dealer.dto.DealerInvoiceRequest;
import com.complex.platform.dealer.dto.DealerOrderDecisionRequest;
import com.complex.platform.dealer.dto.DealerOrderDraftRequest;
import com.complex.platform.dealer.dto.DealerOrderLineRequest;
import com.complex.platform.dealer.dto.DealerPaymentRequest;
import com.complex.platform.dealer.dto.DealerPaymentVerifyRequest;
import com.complex.platform.dealer.dto.DealerReturnDecisionRequest;
import com.complex.platform.dealer.dto.DealerReturnFreightConfirmRequest;
import com.complex.platform.dealer.dto.DealerReturnInspectionLineRequest;
import com.complex.platform.dealer.dto.DealerReturnInspectionRequest;
import com.complex.platform.dealer.dto.DealerReturnLineRequest;
import com.complex.platform.dealer.dto.DealerReturnRefundResultRequest;
import com.complex.platform.dealer.dto.DealerReturnRequest;
import com.complex.platform.dealer.dto.DealerReviewRequest;
import com.complex.platform.dealer.dto.DealerShipRequest;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * CG-11c（BSM:1238-1251 经销退货、退款、红冲与资金归属；ENUM:174 退货单十态、178 退货验收与退款执行分离、179 取消/退货/开票、182 资金归属）。
 * 真实 PostgreSQL（Flyway 全量迁移），@Transactional 回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${COMPLEX_DATABASE_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=${COMPLEX_DATABASE_USERNAME:complex}",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class DealerReturnRefundReversalJdbcIntegrationTests {

    @Autowired private DealerService dealerService;
    @Autowired private DealerOrderService orders;
    @Autowired private DealerReturnService returns;
    @Autowired private SupervisorReviewService reviews;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AccessRepository access;
    @Autowired private PlatformTransactionManager transactionManager;

    private String suffix;
    private String sales;      // 建单 / 退货申请
    private String reviewer;   // 订单审核 / 退货审批
    private String finance;    // 到账核验 / 红冲开票 / 退款执行
    private String warehouse;  // 出库 / 收货验收
    private String refundApprover;
    private String salesEntity;
    private String centralWarehouse;
    private String dealerId;
    private String contractId;
    private String itemA;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        sales = "usr_dr_sales_" + suffix;
        reviewer = "usr_dr_rev_" + suffix;
        finance = "usr_dr_fin_" + suffix;
        warehouse = "usr_dr_wh_" + suffix;
        refundApprover = "usr_dr_rfa_" + suffix;
        String role = "role_dr_" + suffix;
        jdbc.update("INSERT INTO roles(id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '经销退货测试', 'platform', 'active', now())",
                role, "经销退货测试角色" + System.nanoTime());
        jdbc.update("INSERT INTO role_permissions(role_id, permission_code) SELECT ?, permission_code FROM permissions WHERE permission_code LIKE 'dealer.%' OR permission_code = 'governance.supervisor_review.approve' ON CONFLICT DO NOTHING", role);
        for (String user : List.of(sales, reviewer, finance, warehouse, refundApprover)) {
            access.createUser(new AccessUser(user, user, "platform_admin", "project_main", null, "active", Instant.now()));
            access.assignUserRole(user, role);
        }
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)", finance, refundApprover, sales);
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)", warehouse, refundApprover, sales);
        access.saveStaffLevelAssignment(new StaffLevelAssignment(refundApprover, "lvl_m3_lower", null, "CNY", null, "project_main", null, sales, Instant.now()));
        salesEntity = "be_dr_" + suffix;
        jdbc.update("INSERT INTO business_entities(id, entity_name, entity_role, status, created_at, updated_at) VALUES (?, '经销销售主体', 'project_company', 'active', now(), now())", salesEntity);
        centralWarehouse = "wh_dr_" + suffix;
        jdbc.update("INSERT INTO warehouses(id, project_id, warehouse_type, name, status) VALUES (?, 'project_main', 'main', '经销中心仓', 'active')", centralWarehouse);
        itemA = "item_dr_a_" + suffix;
        jdbc.update("INSERT INTO inventory_items(id, item_name, item_type, unit, trace_required, min_quantity, status, created_at, updated_at) VALUES (?, '经销威士忌', 'beverage', '箱', false, 2, 'active', now(), now())", itemA);
        jdbc.update("INSERT INTO inventory_stock_balances(id, item_id, location_id, quantity, reserved_quantity, available_quantity, updated_at, warehouse_id, project_id) VALUES (?, ?, ?, 100, 0, 100, now(), ?, 'project_main')",
                "bal_dr_" + suffix, itemA, centralWarehouse, centralWarehouse);

        actAs(sales);
        DealerRecord dealer = dealerService.createDealer(new DealerCreateRequest(key(), "退货经销商" + suffix, "9131" + System.nanoTime(), "法人", "联系人", "13900000000"));
        dealerService.submitDealer(dealer.id(), key());
        actAs(reviewer);
        dealerService.reviewDealer(dealer.id(), new DealerReviewRequest(key(), "approve", true, true, true, true, "通过"));
        dealerId = dealer.id();
        actAs(sales);
        DealerContractRecord contract = dealerService.createContract(dealerId, new DealerContractRequest(key(), "DC-RET-" + suffix, salesEntity, centralWarehouse,
                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)));
        contractId = contract.id();
        // 账期合同：首付 30%、截止 3 天、尾款 30 天；运费 10 元、包邮 500 元、承担方经销商；退货运费与首程运费约定留空（待双方确认）
        orders.updateContractTerms(contractId, new DealerContractTermsRequest(key(), true, 3000, null, 3, 30, 1_000L, 50_000L, "dealer"));
        DealerPriceCatalogRecord v1 = dealerService.createCatalogDraft(contractId, new DealerCatalogDraftRequest(key(), "目录",
                List.of(new DealerCatalogLineRequest(itemA, "箱", 1_000L, 1300, null))));
        dealerService.submitCatalog(v1.id(), key());
        actAs(reviewer);
        dealerService.decideCatalog(v1.id(), new DealerCatalogDecisionRequest(key(), "approve", "通过"));
        actAs(sales);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** 未开票账期单部分退货：按验收数量按原成交价算退货金额，先冲未收货款，超出部分原路退款；可复售回库、质量问题隔离禁售；退货验收与退款执行分离；订单转部分退货。 */
    @Test
    void partialReturnOffsetsReceivableFirstThenRefundsAndRoutesStockByDisposition() {
        DealerOrderRecord order = signedOrder(5, 4_000L); // 货款 5000 + 运费 1000 = 6000；已收 4000，未收 2000
        assertThat(order.status()).isEqualTo("signed");
        assertThat(balance("quantity")).isEqualTo(95);

        actAs(sales);
        DealerReturnRecord request = returns.requestReturn(order.id(), new DealerReturnRequest(key(), "quality_issue",
                List.of(new DealerReturnLineRequest(itemA, "箱", 4)), "两箱破损"));
        assertThat(request.status()).isEqualTo("pending_approval");
        assertThat(request.reasonType()).isEqualTo("quality_issue");
        assertThat(request.returnFreightBearer()).as("质量问题的退回运费由合同销售主体承担").isEqualTo("sales_entity");
        assertThatThrownBy(() -> returns.decideReturn(request.id(), new DealerReturnDecisionRequest(key(), "approve", "自审")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_SOD_SAME_PERSON"));
        actAs(reviewer);
        DealerReturnRecord approved = returns.decideReturn(request.id(), new DealerReturnDecisionRequest(key(), "approve", "同意退货"));
        assertThat(approved.status()).isEqualTo("pending_receipt");
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("returning");

        actAs(warehouse);
        assertThat(returns.receiveReturn(request.id(), key()).status()).isEqualTo("inspecting");
        DealerReturnRecord inspected = returns.inspectReturn(request.id(), new DealerReturnInspectionRequest(key(), List.of(
                new DealerReturnInspectionLineRequest(itemA, "箱", 3, 2, 1, "两箱完好一箱漏液")), "验收 3 箱"));
        assertThat(inspected.acceptedAmountCent()).as("按原成交价 × 验收数量").isEqualTo(3_000L);
        assertThat(inspected.offsetReceivableCent()).as("先冲减未收货款 2000").isEqualTo(2_000L);
        assertThat(inspected.refundCent()).as("超过未收货款的 1000 才原路退款").isEqualTo(1_000L);
        assertThat(inspected.status()).as("未开票 → 待退款").isEqualTo("pending_refund");
        assertThat(inspected.refundReviewId()).isNotBlank();
        assertThat(balance("quantity")).as("可复售 2 箱回可用库存").isEqualTo(97);
        assertThat(balance("available_quantity")).isEqualTo(97);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inventory_stock_movements WHERE business_type = 'dealer_return' AND business_id = ? AND movement_type = 'dealer_return_quarantine' AND quantity = 1",
                Integer.class, request.id())).as("质量问题 1 箱隔离禁售，不回可用库存").isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT amount_cent, unpaid_amount_cent, paid_amount_cent FROM ar_ap_items WHERE id = ?", order.arApItemId()))
                .as("应收冲减 2000：6000 → 4000，未收 0").containsEntry("amount_cent", 4_000L).containsEntry("unpaid_amount_cent", 0L).containsEntry("paid_amount_cent", 4_000L);

        // 既有退款审批通过
        actAs(refundApprover);
        for (int i = 0; i < 4 && "pending_review".equals(reviews.findReview(inspected.refundReviewId()).orElseThrow().reviewStatus()); i++) {
            reviews.approve(inspected.refundReviewId(), new SupervisorReviewApprovalRequest(key(), "退款审批", List.of(), null, "approved", "同意"));
        }
        assertThat(returns.getReturn(request.id()).refundApproved()).isTrue();
        actAs(warehouse);
        assertThatThrownBy(() -> returns.recordRefundResult(request.id(), new DealerReturnRefundResultRequest(key(), "success", "RF-1", null)))
                .as("退货验收人不得执行退款").isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_SOD_SAME_PERSON"));
        actAs(finance);
        DealerReturnRecord failed = returns.recordRefundResult(request.id(), new DealerReturnRefundResultRequest(key(), "failed", null, "银行退回"));
        assertThat(failed.status()).isEqualTo("refund_failed");
        DealerReturnRecord done = returns.recordRefundResult(request.id(), new DealerReturnRefundResultRequest(key(), "success", "RF-2", null));
        assertThat(done.status()).as("验收 3 < 申请 4 → 部分完成").isEqualTo("partially_completed");
        assertThat(done.refundedCent()).isEqualTo(1_000L);
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("partially_returned");
        assertThat(orders.getOrder(order.id()).returnedAmountCent()).isEqualTo(3_000L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name = 'dealer.return.refund_executed' AND business_id = ?", Integer.class, request.id())).isEqualTo(1);
    }

    /** 已开票订单部分退货先全额红冲原发票，再按最新未退金额重开；全部退货红冲后不重开；签收后才可按最终未退金额申请电子发票。 */
    @Test
    void invoicedReturnReversesOriginalInvoiceAndReissuesForRemainingAmount() {
        DealerOrderRecord order = signedOrder(4, 5_000L); // 4000 + 1000 运费 = 5000 全额到账
        actAs(sales);
        assertThatThrownBy(() -> returns.requestInvoice(pendingOrder().id(), new DealerInvoiceRequest(key(), null)))
                .as("签收后才可申请发票").isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_ORDER_STATUS_INVALID"));
        DealerInvoiceRecord invoice = returns.requestInvoice(order.id(), new DealerInvoiceRequest(key(), null));
        assertThat(invoice.status()).isEqualTo("pending");
        assertThat(invoice.amountCent()).as("按最终未退金额").isEqualTo(5_000L);
        assertThat(invoice.salesEntityId()).as("开票主体＝合同销售主体").isEqualTo(salesEntity);
        actAs(finance);
        DealerInvoiceRecord issued = returns.issueInvoice(invoice.id(), new DealerInvoiceIssueRequest(key(), "INV-0001"));
        assertThat(issued.status()).isEqualTo("issued");

        actAs(sales);
        DealerReturnRecord request = returns.requestReturn(order.id(), new DealerReturnRequest(key(), "unopened_resalable",
                List.of(new DealerReturnLineRequest(itemA, "箱", 1)), "未拆封退一箱"));
        assertThat(request.returnFreightBearer()).as("非质量退货运费按合同约定，未约定先由双方确认").isEqualTo("pending_confirmation");
        actAs(reviewer);
        returns.decideReturn(request.id(), new DealerReturnDecisionRequest(key(), "approve", "同意"));
        actAs(warehouse);
        returns.receiveReturn(request.id(), key());
        DealerReturnRecord inspected = returns.inspectReturn(request.id(), new DealerReturnInspectionRequest(key(), List.of(
                new DealerReturnInspectionLineRequest(itemA, "箱", 1, 1, 0, "完好")), "验收"));
        assertThat(inspected.status()).as("已开票 → 待红冲").isEqualTo("pending_reversal");
        assertThat(inspected.offsetReceivableCent()).isZero();
        assertThat(inspected.refundCent()).isEqualTo(1_000L);
        actAs(finance);
        DealerReturnRecord reversed = returns.reverseInvoice(request.id(), new DealerInvoiceIssueRequest(key(), "INV-0002"));
        assertThat(reversed.status()).isEqualTo("pending_refund");
        List<DealerInvoiceRecord> invoices = returns.listInvoices(order.id()).items();
        assertThat(invoices).extracting(DealerInvoiceRecord::status).containsExactlyInAnyOrder("reversed", "issued");
        DealerInvoiceRecord reissued = invoices.stream().filter(i -> "issued".equals(i.status())).findFirst().orElseThrow();
        assertThat(reissued.amountCent()).as("按最新未退金额重开：5000 − 1000").isEqualTo(4_000L);
        assertThat(reissued.reissuedFromInvoiceId()).isEqualTo(invoice.id());
        actAs(sales);
        DealerReturnRecord confirmed = returns.confirmReturnFreight(request.id(), new DealerReturnFreightConfirmRequest(key(), "dealer", false, "双方确认"));
        assertThat(confirmed.returnFreightBearer()).isEqualTo("dealer");

        // 全部退货：再退剩余 3 箱 → 红冲后不重开
        actAs(refundApprover);
        for (int i = 0; i < 4 && "pending_review".equals(reviews.findReview(reversed.refundReviewId()).orElseThrow().reviewStatus()); i++) {
            reviews.approve(reversed.refundReviewId(), new SupervisorReviewApprovalRequest(key(), "退款审批", List.of(), null, "approved", "同意"));
        }
        actAs(finance);
        assertThat(returns.recordRefundResult(request.id(), new DealerReturnRefundResultRequest(key(), "success", "RF-A", null)).status()).isEqualTo("completed");
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("partially_returned");
        actAs(sales);
        DealerReturnRecord rest = returns.requestReturn(order.id(), new DealerReturnRequest(key(), "unopened_resalable",
                List.of(new DealerReturnLineRequest(itemA, "箱", 3)), "全退"));
        actAs(reviewer);
        returns.decideReturn(rest.id(), new DealerReturnDecisionRequest(key(), "approve", "同意"));
        actAs(warehouse);
        returns.receiveReturn(rest.id(), key());
        returns.inspectReturn(rest.id(), new DealerReturnInspectionRequest(key(), List.of(new DealerReturnInspectionLineRequest(itemA, "箱", 3, 3, 0, "完好")), "验收"));
        actAs(finance);
        DealerReturnRecord allReversed = returns.reverseInvoice(rest.id(), new DealerInvoiceIssueRequest(key(), null));
        assertThat(allReversed.status()).isEqualTo("pending_refund");
        assertThat(returns.listInvoices(order.id()).items()).extracting(DealerInvoiceRecord::status).as("全部退货红冲后不重开").containsOnly("reversed");
        actAs(refundApprover);
        for (int i = 0; i < 4 && "pending_review".equals(reviews.findReview(allReversed.refundReviewId()).orElseThrow().reviewStatus()); i++) {
            reviews.approve(allReversed.refundReviewId(), new SupervisorReviewApprovalRequest(key(), "退款审批", List.of(), null, "approved", "同意"));
        }
        actAs(finance);
        assertThat(returns.recordRefundResult(rest.id(), new DealerReturnRefundResultRequest(key(), "success", "RF-B", null)).status()).isEqualTo("completed");
        assertThat(orders.getOrder(order.id()).status()).as("货款部分全部退回 → 已退货").isEqualTo("returned");
        assertThat(balance("quantity")).isEqualTo(100);
    }

    /** 合同期限内、仅未拆封可复售 / 质量问题两类、数量不得超过已出库未退数量、签收前不得退货；退货单十态与退货原因 CHECK 闭集。 */
    @Test
    void returnEligibilityAndClosedSets() {
        DealerOrderRecord order = signedOrder(2, 3_000L);
        actAs(sales);
        assertThatThrownBy(() -> returns.requestReturn(order.id(), new DealerReturnRequest(key(), "changed_mind", List.of(new DealerReturnLineRequest(itemA, "箱", 1)), "不想要")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("RETURN_REASON_NOT_ALLOWED"));
        assertThatThrownBy(() -> returns.requestReturn(order.id(), new DealerReturnRequest(key(), "unopened_resalable", List.of(new DealerReturnLineRequest(itemA, "箱", 3)), "超量")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("RETURN_QUANTITY_EXCEEDS"));
        assertThatThrownBy(() -> returns.requestReturn(pendingOrder().id(), new DealerReturnRequest(key(), "unopened_resalable", List.of(new DealerReturnLineRequest(itemA, "箱", 1)), "未签收")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_ORDER_STATUS_INVALID"));
        jdbc.update("UPDATE dealer_contracts SET valid_to = ? WHERE id = ?", java.sql.Date.valueOf(LocalDate.now().minusDays(1)), contractId);
        assertThatThrownBy(() -> returns.requestReturn(order.id(), new DealerReturnRequest(key(), "unopened_resalable", List.of(new DealerReturnLineRequest(itemA, "箱", 1)), "过期")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_CONTRACT_EXPIRED"));
    }

    @Test
    void returnStatusAndReasonChecksRejectValuesOutsideClosedSets() {
        DealerOrderRecord order = signedOrder(1, 2_000L);
        actAs(sales);
        DealerReturnRecord request = returns.requestReturn(order.id(), new DealerReturnRequest(key(), "unopened_resalable", List.of(new DealerReturnLineRequest(itemA, "箱", 1)), "闭集"));
        assertThat(returns.requestInvoice(order.id(), new DealerInvoiceRequest(key(), null)).status()).isEqualTo("pending");
        TransactionTemplate nested = new TransactionTemplate(transactionManager);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        for (String illegal : List.of("approved", "received", "closed")) {
            assertThatThrownBy(() -> nested.executeWithoutResult(s -> jdbc.update("UPDATE dealer_returns SET status = ? WHERE id = ?", illegal, request.id())))
                    .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chk_dealer_returns_status");
        }
        assertThatThrownBy(() -> nested.executeWithoutResult(s -> jdbc.update("UPDATE dealer_returns SET reason_type = 'changed_mind' WHERE id = ?", request.id())))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chk_dealer_returns_reason");
        assertThatThrownBy(() -> nested.executeWithoutResult(s -> jdbc.update("UPDATE dealer_invoices SET status = 'printed' WHERE order_id = ?", order.id())))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("chk_dealer_invoices_status");
    }

    // ── helpers ──

    /** 建单 → 审核（预留、通知付款）→ 到账核验 paid 分 → 出库 → 签收。 */
    private DealerOrderRecord signedOrder(int quantity, long paidCent) {
        actAs(sales);
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), contractId, List.of(new DealerOrderLineRequest(itemA, "箱", quantity, null)), "退货测试单"));
        orders.submitOrder(draft.id(), key());
        actAs(reviewer);
        orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "通过"));
        actAs(sales);
        DealerOrderPaymentRecord payment = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer", paidCent, Instant.now(), "TXN-" + key()));
        actAs(finance);
        orders.verifyPayment(payment.id(), new DealerPaymentVerifyRequest(key(), "approve", "核验"));
        actAs(warehouse);
        orders.shipOrder(draft.id(), new DealerShipRequest(key(), "出库"));
        actAs(sales);
        return orders.signOrder(draft.id(), key());
    }

    private DealerOrderRecord pendingOrder() {
        actAs(sales);
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), contractId, List.of(new DealerOrderLineRequest(itemA, "箱", 1, null)), "未签收"));
        orders.submitOrder(draft.id(), key());
        actAs(reviewer);
        DealerOrderRecord notified = orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "通过"));
        actAs(sales);
        return notified;
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
