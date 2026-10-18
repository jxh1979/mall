package com.complex.platform.dealer;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
    @Autowired private DealerDeliveryService delivery;
    @Autowired private DealerReturnService returns;
    @Autowired private DealerReturnFinancialService returnMoney;
    @Autowired private com.complex.platform.finance.FinanceService financeService;
    @Autowired private SupervisorReviewService reviews;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.complex.platform.inventory.InventoryRepository inventory;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Autowired private com.complex.platform.evidence.EvidenceEventService admissionFiles;
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
        jdbc.update("INSERT INTO role_permissions(role_id, permission_code) SELECT ?, permission_code FROM permissions WHERE permission_code LIKE 'dealer.%' OR permission_code LIKE 'finance.%' OR permission_code LIKE 'evidence.%' OR permission_code = 'governance.supervisor_review.approve' ON CONFLICT DO NOTHING", role);
        for (String user : List.of(sales, reviewer, finance, warehouse, refundApprover)) {
            access.createUser(new AccessUser(user, user, "platform_admin", "project_main", null, "active", Instant.now()));
            access.assignUserRole(user, role);
        }
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id, line_code, updated_by) VALUES (?, ?, 'main', ?)", sales, refundApprover, sales);
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
        DealerTestDocuments.submit(dealerService, admissionFiles, dealer.id(), key());
        actAs(reviewer);
        dealerService.reviewDealer(dealer.id(), new DealerReviewRequest(key(), "approve", true, true, true, true, "通过"));
        dealerId = dealer.id();
        actAs(sales);
        DealerContractRecord contract = dealerService.createContract(dealerId, new DealerContractRequest(key(), "DC-RET-" + suffix, salesEntity, centralWarehouse,
                LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)));
        contractId = contract.id();
        // 账期合同：首付 30%、截止 3 天、尾款 30 天；运费 10 元、包邮 500 元、承担方经销商；退货运费与首程运费约定留空（待双方确认）
        orders.updateContractTerms(contractId, new DealerContractTermsRequest(key(), true, 3000, null, 3, 30, 1_000L, 50_000L, "dealer", null, null, null, "payment_notified", null, "shipped", null));
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

    @Test
    void manuallyDeclaredRefundSuccessCannotCloseReturnWithoutActualFinanceResult() {
        DealerOrderRecord order = signedOrder(2, 3_000L);
        actAs(sales);
        DealerReturnRecord ret = returns.requestReturn(order.id(), new DealerReturnRequest(key(), "unopened_resalable",
                List.of(new DealerReturnLineRequest(itemA, "箱", 1)), "原收款退货"));
        actAs(reviewer);
        returns.decideReturn(ret.id(), new DealerReturnDecisionRequest(key(), "approve", "同意"));
        actAs(warehouse);
        returns.receiveReturn(ret.id(), key());
        DealerReturnRecord inspected = returns.inspectReturn(ret.id(), new DealerReturnInspectionRequest(key(), List.of(
                new DealerReturnInspectionLineRequest(itemA, "箱", 1, 1, 0, "实物验收")), "验收", proof("dealer_return",ret.id())));
        actAs(refundApprover);
        for (int i = 0; i < 4 && "pending_review".equals(reviews.findReview(inspected.refundReviewId()).orElseThrow().reviewStatus()); i++) {
            reviews.approve(inspected.refundReviewId(), new SupervisorReviewApprovalRequest(key(), "退款审批", List.of(), null, "approved", "同意"));
        }
        assertThat(returns.getReturn(ret.id()).refundApproved()).isTrue();
        actAs(finance);
        assertThatThrownBy(() -> isolated(() -> returns.recordRefundResult(ret.id(), new DealerReturnRefundResultRequest(key(), "success", null, null))))
                .as("手填成功、没有统一财务执行及独立结果确认，不能记成实际退款")
                .isInstanceOf(ApiException.class);
        assertThat(returns.getReturn(ret.id()).refundedCent()).isZero();
        assertThat(returns.getReturn(ret.id()).status()).isEqualTo("pending_refund");
    }

    @Test
    void freightConfirmationCannotOverrideOriginalOrderLockedContract() {
        actAs(sales);
        orders.updateContractTerms(contractId, new DealerContractTermsRequest(key(), true, 3000, null, 3, 30,
                1_000L, 50_000L, "dealer", "dealer", false, null, "payment_notified", null, "shipped", null));
        DealerOrderRecord order = signedOrder(2, 3_000L);
        actAs(sales);
        DealerReturnRecord ret = returns.requestReturn(order.id(), new DealerReturnRequest(key(), "unopened_resalable",
                List.of(new DealerReturnLineRequest(itemA, "箱", 1)), "锁定合同约定首程不退"));
        assertThat(ret.firstLegFreightRefundable()).isFalse();
        actAs(reviewer);returns.decideReturn(ret.id(),new DealerReturnDecisionRequest(key(),"approve","同意退货"));actAs(sales);
        assertThatThrownBy(() -> isolated(() -> returns.confirmReturnFreight(ret.id(),
                new DealerReturnFreightConfirmRequest(key(), "sales_entity", true, "不能靠备注改合同"))))
                .as("已锁定合同的承担方与首程退费约定，不能被普通运费确认覆盖")
                .isInstanceOf(ApiException.class);
        assertThat(returns.getReturn(ret.id()).firstLegFreightRefundable()).isFalse();
        assertThat(returns.getReturn(ret.id()).returnFreightBearer()).isEqualTo("dealer");
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
        assertThatThrownBy(() -> isolated(() -> returns.decideReturn(request.id(), new DealerReturnDecisionRequest(key(), "approve", "自审"))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_SOD_SAME_PERSON"));
        actAs(reviewer);
        DealerReturnRecord approved = returns.decideReturn(request.id(), new DealerReturnDecisionRequest(key(), "approve", "同意退货"));
        assertThat(approved.status()).isEqualTo("pending_receipt");
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("returning");

        actAs(warehouse);
        assertThat(returns.receiveReturn(request.id(), key()).status()).isEqualTo("inspecting");
        DealerReturnRecord inspected = returns.inspectReturn(request.id(), new DealerReturnInspectionRequest(key(), List.of(
                new DealerReturnInspectionLineRequest(itemA, "箱", 3, 2, 1, "两箱完好一箱漏液")), "验收 3 箱", proof("dealer_return",request.id())));
        assertThat(inspected.acceptedAmountCent()).as("按原成交价 × 验收数量").isEqualTo(3_000L);
        assertThat(inspected.offsetReceivableCent()).as("先冲减未收货款 2000").isEqualTo(2_000L);
        assertThat(inspected.refundCent()).as("超过未收货款的 1000 才原路退款").isEqualTo(1_000L);
        assertThat(inspected.status()).as("未开票 → 待退款").isEqualTo("pending_refund");
        assertThat(inspected.refundReviewId()).isNotBlank();
        assertThat(balance("quantity")).as("2箱可售及1箱隔离均计入实物总量").isEqualTo(98);
        assertThat(balance("available_quantity")).isEqualTo(97);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inventory_quarantine_lots q JOIN dealer_return_lines l ON l.id=q.source_id WHERE q.source_type='dealer_return_line' AND l.return_id=? AND q.remaining_quantity=1",
                Integer.class, request.id())).as("质量问题 1 箱隔离禁售，不回可用库存").isEqualTo(1);
        assertThat(jdbc.queryForMap("SELECT amount_cent, unpaid_amount_cent, paid_amount_cent FROM ar_ap_items WHERE id = ?", order.arApItemId()))
                .as("应收按实际验收减款3000：6000→3000；原到账4000保留，另列待退1000").containsEntry("amount_cent", 3_000L).containsEntry("unpaid_amount_cent", 0L).containsEntry("paid_amount_cent", 3_000L);

        actAs(sales);
        DealerOrderPaymentRecord extra = orders.registerPayment(order.id(),
                new DealerPaymentRequest(key(), "bank_transfer", 1_000L, Instant.now(), "EXCESS-" + suffix));
        actAs(finance);
        DealerOrderPaymentRecord extraVerified = orders.verifyPayment(extra.id(),
                new DealerPaymentVerifyRequest(key(), "approve", "退货后多收款"));
        assertThat(extraVerified.appliedCent()).isZero();
        assertThat(jdbc.queryForMap("SELECT amount_cent, unpaid_amount_cent, paid_amount_cent FROM ar_ap_items WHERE id = ?", order.arApItemId()))
                .as("全额属于超收的到账不得重写退货后已经结清的应收")
                .containsEntry("amount_cent", 3_000L).containsEntry("unpaid_amount_cent", 0L).containsEntry("paid_amount_cent", 3_000L);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM dealer_order_overpayments WHERE payment_id = ?", Long.class, extra.id()))
                .isEqualTo(1_000L);

        // 既有退款审批通过
        actAs(refundApprover);
        for (int i = 0; i < 4 && "pending_review".equals(reviews.findReview(inspected.refundReviewId()).orElseThrow().reviewStatus()); i++) {
            reviews.approve(inspected.refundReviewId(), new SupervisorReviewApprovalRequest(key(), "退款审批", List.of(), null, "approved", "同意"));
        }
        assertThat(returns.getReturn(request.id()).refundApproved()).isTrue();
        actAs(warehouse);
        assertThatThrownBy(() -> isolated(() -> returnMoney.executeRefunds(request.id(),new com.complex.platform.dealer.dto.DealerIdempotentRequest(key()))))
                .as("验收人不能执行退款").isInstanceOf(ApiException.class);
        var pending=prepareRefunds(request.id());
        assertThat(pending.goodsRefundedCent()).isZero();
        settle(pending.refunds().getFirst(),"failed");
        assertThat(returns.getReturn(request.id()).status()).isEqualTo("refund_failed");
        settle(pending.refunds().getFirst(),"success");
        DealerReturnRecord done=returns.getReturn(request.id());
        assertThat(done.status()).as("验收 3 < 申请 4 → 部分完成").isEqualTo("partially_completed");
        assertThat(done.refundedCent()).isEqualTo(1_000L);
        assertThat(orders.listOverpayments(dealerId).items()).singleElement()
                .satisfies(over -> assertThat(over.amountCent()).isEqualTo(1_000L));
        assertThat(jdbc.queryForMap("SELECT amount_cent, unpaid_amount_cent, paid_amount_cent FROM ar_ap_items WHERE id = ?", order.arApItemId()))
                .containsEntry("amount_cent", 3_000L).containsEntry("unpaid_amount_cent", 0L).containsEntry("paid_amount_cent", 3_000L);
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("partially_returned");
        assertThat(orders.getOrder(order.id()).returnedAmountCent()).isEqualTo(3_000L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dealer_order_refunds WHERE return_id=?",Integer.class,request.id())).isEqualTo(1);
    }

    /** 已开票订单部分退货先全额红冲原发票，再按最新未退金额重开；全部退货红冲后不重开；签收后才可按最终未退金额申请电子发票。 */
    @Test
    void invoicedReturnReversesOriginalInvoiceAndReissuesForRemainingAmount() {
        DealerOrderRecord order = signedOrder(4, 5_000L); // 4000 + 1000 运费 = 5000 全额到账
        actAs(sales);
        assertThatThrownBy(() -> isolated(() -> returns.requestInvoice(pendingOrder().id(), new DealerInvoiceRequest(key(), null))))
                .as("签收后才可申请发票").isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_RETURN_FINANCIAL_INVALID"));
        DealerInvoiceRecord invoice = returns.requestInvoice(order.id(), new DealerInvoiceRequest(key(), null));
        assertThat(invoice.status()).isEqualTo("pending");
        assertThat(invoice.amountCent()).as("按最终未退金额").isEqualTo(5_000L);
        assertThat(invoice.salesEntityId()).as("开票主体＝合同销售主体").isEqualTo(salesEntity);
        actAs(finance);
        DealerInvoiceRecord issued = returns.issueInvoice(invoice.id(), new DealerInvoiceIssueRequest(key(), "INV-0001",proof("dealer_invoice",invoice.id())));
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
                new DealerReturnInspectionLineRequest(itemA, "箱", 1, 1, 0, "完好")), "验收", proof("dealer_return",request.id())));
        assertThat(inspected.status()).as("已开票 → 待红冲").isEqualTo("pending_reversal");
        assertThat(inspected.offsetReceivableCent()).isZero();
        assertThat(inspected.refundCent()).isEqualTo(1_000L);
        actAs(finance);
        DealerReturnRecord submitted = returns.reverseInvoice(request.id(), new DealerInvoiceIssueRequest(key(), null));
        assertThat(submitted.status()).isEqualTo("pending_reversal");
        assertThat(returns.listInvoices(order.id()).items().getFirst().status()).isEqualTo("issued");
        recordRed(invoice.id(),"success");
        DealerReturnRecord reversed=returns.getReturn(request.id());
        assertThat(reversed.status()).isEqualTo("pending_refund");
        List<DealerInvoiceRecord> invoices = returns.listInvoices(order.id()).items();
        assertThat(invoices).extracting(DealerInvoiceRecord::status).containsExactlyInAnyOrder("reversed", "pending");
        DealerInvoiceRecord reissued = invoices.stream().filter(i -> "pending".equals(i.status())).findFirst().orElseThrow();
        assertThat(reissued.amountCent()).as("按最新未退金额重开：5000 − 1000").isEqualTo(4_000L);
        assertThat(reissued.reissuedFromInvoiceId()).isEqualTo(invoice.id());
        returns.issueInvoice(reissued.id(),new DealerInvoiceIssueRequest(key(),"INV-0002",proof("dealer_invoice",reissued.id())));
        actAs(sales);
        DealerReturnRecord confirmed = returns.confirmReturnFreight(request.id(), new DealerReturnFreightConfirmRequest(key(), "dealer", false, "双方确认",0L,proof("dealer_return",request.id())));
        assertThat(confirmed.returnFreightBearer()).isEqualTo("dealer");

        // 全部退货：再退剩余 3 箱 → 红冲后不重开
        actAs(refundApprover);
        for (int i = 0; i < 4 && "pending_review".equals(reviews.findReview(reversed.refundReviewId()).orElseThrow().reviewStatus()); i++) {
            reviews.approve(reversed.refundReviewId(), new SupervisorReviewApprovalRequest(key(), "退款审批", List.of(), null, "approved", "同意"));
        }
        actAs(finance);
        for(var refund:prepareRefunds(request.id()).refunds()) settle(refund,"success");
        assertThat(returns.getReturn(request.id()).status()).isEqualTo("completed");
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("partially_returned");
        actAs(sales);
        DealerReturnRecord rest = returns.requestReturn(order.id(), new DealerReturnRequest(key(), "unopened_resalable",
                List.of(new DealerReturnLineRequest(itemA, "箱", 3)), "全退"));
        actAs(reviewer);
        returns.decideReturn(rest.id(), new DealerReturnDecisionRequest(key(), "approve", "同意"));
        actAs(warehouse);
        returns.receiveReturn(rest.id(), key());
        returns.inspectReturn(rest.id(), new DealerReturnInspectionRequest(key(), List.of(new DealerReturnInspectionLineRequest(itemA, "箱", 3, 3, 0, "完好")), "验收", proof("dealer_return",rest.id())));
        actAs(finance);
        returns.reverseInvoice(rest.id(), new DealerInvoiceIssueRequest(key(), null));
        recordRed(reissued.id(),"success");
        DealerReturnRecord allReversed=returns.getReturn(rest.id());
        assertThat(allReversed.status()).isEqualTo("pending_refund");
        assertThat(returns.listInvoices(order.id()).items()).extracting(DealerInvoiceRecord::status).as("全部退货红冲后不重开").containsOnly("reversed");
        actAs(refundApprover);
        for (int i = 0; i < 4 && "pending_review".equals(reviews.findReview(allReversed.refundReviewId()).orElseThrow().reviewStatus()); i++) {
            reviews.approve(allReversed.refundReviewId(), new SupervisorReviewApprovalRequest(key(), "退款审批", List.of(), null, "approved", "同意"));
        }
        actAs(finance);
        for(var refund:prepareRefunds(rest.id()).refunds()) settle(refund,"success");
        assertThat(returns.getReturn(rest.id()).status()).isEqualTo("completed");
        assertThat(orders.getOrder(order.id()).status()).as("货款部分全部退回 → 已退货").isEqualTo("returned");
        assertThat(balance("quantity")).isEqualTo(100);
    }

    @Test
    void actualReversalClosesPartialReturnThatOnlyOffsetsReceivable() {
        var order=signedOrder(5,1_800L);
        actAs(finance);
        var invoice=returns.requestInvoice(order.id(),new DealerInvoiceRequest(key(),null));
        returns.issueInvoice(invoice.id(),new DealerInvoiceIssueRequest(key(),"INV-OFFSET-PART",proof("dealer_invoice",invoice.id())));
        var ret=inspectAcceptedReturn(order.id(),2);
        assertThat(ret.refundCent()).isZero();
        assertThat(ret.offsetReceivableCent()).isEqualTo(2_000L);
        recordRed(invoice.id(),"processing");
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("returning");
        recordRed(invoice.id(),"success");
        assertThat(returns.getReturn(ret.id()).status()).isEqualTo("completed");
        assertThat(orders.getOrder(order.id()).status()).as("无现金退款时，真实红冲成功须完成原订单退货状态").isEqualTo("partially_returned");
        assertThat(returns.listInvoices(order.id()).items()).extracting(DealerInvoiceRecord::status).containsExactlyInAnyOrder("reversed","pending");
    }

    @Test
    void actualReversalClosesFullReturnThatOnlyOffsetsReceivable() {
        var order=signedOrder(2,900L);
        actAs(finance);
        var invoice=returns.requestInvoice(order.id(),new DealerInvoiceRequest(key(),null));
        returns.issueInvoice(invoice.id(),new DealerInvoiceIssueRequest(key(),"INV-OFFSET-ALL",proof("dealer_invoice",invoice.id())));
        var ret=inspectAcceptedReturn(order.id(),2);
        assertThat(ret.refundCent()).isZero();
        recordRed(invoice.id(),"success");
        assertThat(returns.getReturn(ret.id()).status()).isEqualTo("completed");
        assertThat(orders.getOrder(order.id()).status()).as("商品全退且无现金退款，不能永久停在退货中").isEqualTo("returned");
        assertThat(returns.listInvoices(order.id()).items()).extracting(DealerInvoiceRecord::status).containsOnly("reversed");
    }

    @Test
    void actualReversalKeepsOrderReturningWhileAnotherReturnIsOpen() {
        var order=signedOrder(5,1_800L);
        actAs(finance);
        var invoice=returns.requestInvoice(order.id(),new DealerInvoiceRequest(key(),null));
        returns.issueInvoice(invoice.id(),new DealerInvoiceIssueRequest(key(),"INV-OFFSET-OPEN",proof("dealer_invoice",invoice.id())));
        var ret=inspectAcceptedReturn(order.id(),1);
        actAs(sales);
        var open=returns.requestReturn(order.id(),new DealerReturnRequest(key(),"quality_issue",
                List.of(new DealerReturnLineRequest(itemA,"箱",1)),"另一笔待处理退货"));
        recordRed(invoice.id(),"success");
        assertThat(returns.getReturn(ret.id()).status()).isEqualTo("completed");
        assertThat(returns.getReturn(open.id()).status()).isEqualTo("pending_approval");
        assertThat(orders.getOrder(order.id()).status()).as("其他退货尚未处理完不能提前完成订单").isEqualTo("returning");
    }

    @Test
    void returnFreightUsesTheOrderContractSnapshotAfterTermsChange() {
        DealerOrderRecord order = signedOrder(2, 3000L);
        actAs(sales);
        orders.updateContractTerms(contractId, new DealerContractTermsRequest(key(), true, 3000, null, 3, 90, 1000L, 50000L,
                "dealer", "sales_entity", true, null, "payment_notified", null, "signed", null));
        DealerReturnRecord request = returns.requestReturn(order.id(), new DealerReturnRequest(key(), "unopened_resalable",
                List.of(new DealerReturnLineRequest(itemA, "箱", 1)), "沿原订单合同"));
        assertThat(request.returnFreightBearer()).isEqualTo("pending_confirmation");
        assertThat(request.firstLegFreightRefundable()).isNull();
    }

    /** 合同期限内、仅未拆封可复售 / 质量问题两类、数量不得超过已出库未退数量、签收前不得退货；退货单十态与退货原因 CHECK 闭集。 */
    @Test
    void returnEligibilityAndClosedSets() throws Exception {
        DealerOrderRecord order = signedOrder(2, 3_000L);
        actAs(sales);
        assertThatThrownBy(() -> isolated(() -> returns.requestReturn(order.id(), new DealerReturnRequest(key(), "changed_mind", List.of(new DealerReturnLineRequest(itemA, "箱", 1)), "不想要"))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("RETURN_REASON_NOT_ALLOWED"));
        assertThatThrownBy(() -> isolated(() -> returns.requestReturn(order.id(), new DealerReturnRequest(key(), "unopened_resalable", List.of(new DealerReturnLineRequest(itemA, "箱", 3)), "超量"))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("RETURN_QUANTITY_EXCEEDS"));
        assertThatThrownBy(() -> isolated(() -> returns.requestReturn(pendingOrder().id(), new DealerReturnRequest(key(), "unopened_resalable", List.of(new DealerReturnLineRequest(itemA, "箱", 1)), "未签收"))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_ORDER_STATUS_INVALID"));
        DealerContractRecord c = orders.getOrder(order.id()).contractSnapshot();
        DealerContractRecord expiredSnapshot = new DealerContractRecord(c.id(), c.dealerId(), c.contractNo(), c.salesEntityId(), c.centralWarehouseId(),
                c.validFrom(), LocalDate.now().minusDays(1), c.status(), c.createdBy(), c.createdAt(), c.creditTerm(), c.firstPaymentRatioBps(),
                c.firstPaymentAmountCent(), c.firstPaymentDeadlineDays(), c.balanceDueDays(), c.freightCent(), c.freeShippingThresholdCent(),
                c.freightBearer(), c.freightTemplateVersion(), c.returnFreightBearer(), c.firstLegFreightRefundable(), c.termsVersion(), c.contractSignedAt(),
                c.firstPaymentAnchor(), c.firstPaymentFixedAt(), c.balanceDueAnchor(), c.balanceDueFixedAt());
        jdbc.update("UPDATE dealer_orders SET contract_snapshot = CAST(? AS jsonb) WHERE id = ?", objectMapper.writeValueAsString(expiredSnapshot), order.id());
        assertThatThrownBy(() -> isolated(() -> returns.requestReturn(order.id(), new DealerReturnRequest(key(), "unopened_resalable", List.of(new DealerReturnLineRequest(itemA, "箱", 1)), "过期"))))
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

    @Test
    void partialReturnRemainingBalanceCanBeCollectedAndClearsOverdueCreditHold() {
        DealerOrderRecord order = signedOrder(9, 3_000L);
        inspectAcceptedReturn(order.id(), 2);
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("partially_returned");
        assertThat(orders.markOverdue(Instant.now().plusSeconds(60L * 86400))).isEqualTo(1);
        assertThat(dealerService.getDealer(dealerId).creditSuspended()).isTrue();
        actAs(sales);
        DealerOrderPaymentRecord payment = orders.registerPayment(order.id(),
                new DealerPaymentRequest(key(), "bank_transfer", 5_000L, Instant.now(), "NET-BALANCE"));
        actAs(finance);
        assertThat(orders.verifyPayment(payment.id(), new DealerPaymentVerifyRequest(key(), "approve", "尾款到账")).appliedCent()).isEqualTo(5_000L);
        DealerOrderRecord after = orders.getOrder(order.id());
        assertThat(after.status()).as("付款结清不抹掉部分退货履约事实").isEqualTo("partially_returned");
        assertThat(after.returnedAmountCent()).isEqualTo(2_000L);
        assertThat(after.paidAmountCent()).isEqualTo(8_000L);
        assertThat(after.overdue()).isFalse();
        assertThat(dealerService.getDealer(dealerId).creditSuspended()).isFalse();
        assertThat(jdbc.queryForMap("SELECT amount_cent, unpaid_amount_cent, paid_amount_cent, status FROM ar_ap_items WHERE id = ?", order.arApItemId()))
                .containsEntry("amount_cent", 8_000L).containsEntry("unpaid_amount_cent", 0L).containsEntry("paid_amount_cent", 8_000L).containsEntry("status", "settled");
    }

    @Test
    void paymentAfterPartialReturnAppliesOnlyNetReceivableAndSeparatesExcess() {
        DealerOrderRecord order = signedOrder(9, 3_000L);
        inspectAcceptedReturn(order.id(), 2);
        actAs(sales);
        DealerOrderPaymentRecord payment = orders.registerPayment(order.id(),
                new DealerPaymentRequest(key(), "bank_transfer", 6_000L, Instant.now(), "NET-EXCESS"));
        actAs(finance);
        assertThat(orders.verifyPayment(payment.id(), new DealerPaymentVerifyRequest(key(), "approve", "核验到账")).appliedCent()).isEqualTo(5_000L);
        assertThat(orders.listOverpayments(dealerId).items()).singleElement().satisfies(over -> assertThat(over.amountCent()).isEqualTo(1_000L));
        assertThat(orders.markOverdue(Instant.now().plusSeconds(60L * 86400))).isZero();
    }

    @Test
    void approvedReturnClearsItsOverdueBalanceButKeepsOtherOverdueOrdersSuspended() {
        DealerOrderRecord first = signedOrder(4, 3_000L);
        DealerOrderRecord second = signedOrder(4, 3_000L);
        assertThat(orders.markOverdue(Instant.now().plusSeconds(60L * 86400))).isEqualTo(2);
        inspectAcceptedReturn(first.id(), 2);
        assertThat(orders.getOrder(first.id()).overdue()).isFalse();
        assertThat(dealerService.getDealer(dealerId).creditSuspended()).isTrue();
        inspectAcceptedReturn(second.id(), 2);
        assertThat(orders.getOrder(second.id()).overdue()).isFalse();
        assertThat(dealerService.getDealer(dealerId).creditSuspended()).isFalse();
    }

    private DealerReturnRecord inspectAcceptedReturn(String orderId, int quantity) {
        actAs(sales);
        DealerReturnRecord request = returns.requestReturn(orderId, new DealerReturnRequest(key(), "quality_issue",
                List.of(new DealerReturnLineRequest(itemA, "箱", quantity)), "质量退货"));
        actAs(reviewer);
        returns.decideReturn(request.id(), new DealerReturnDecisionRequest(key(), "approve", "确认"));
        actAs(warehouse);
        returns.receiveReturn(request.id(), key());
        return returns.inspectReturn(request.id(), new DealerReturnInspectionRequest(key(),
                List.of(new DealerReturnInspectionLineRequest(itemA, "箱", quantity, 0, quantity, "质量问题隔离")), "核验数量", proof("dealer_return",request.id())));
    }

    /** 建单 → 审核（预留、通知付款）→ 到账核验 paid 分 → 出库 → 签收。 */
    private DealerOrderRecord signedOrder(int quantity, long paidCent) {
        var shipped=shippedOrder(quantity,paidCent);
        actAs(sales);
        return delivery.receive(shipped.id(), DealerTestDeliveries.full(shipped.id(), orders.listLines(shipped.id()).items(), DealerTestDeliveries.proof(admissionFiles, shipped.id())));
    }

    private DealerOrderRecord shippedOrder(int quantity, long paidCent) {
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
        return orders.shipOrder(draft.id(), new DealerShipRequest(key(), "出库"));
    }

    @Test
    void returningAllSixAcceptedGoodsClosesOriginalTenUnitOrderWithoutCash() {
        var order=sixAcceptedFourResolved(1_000L);
        var ret=inspectAcceptedReturn(order.id(),6);
        assertThat(ret.refundCent()).isZero();
        assertThat(ret.offsetReceivableCent()).isEqualTo(6_000L);
        assertThat(orders.getOrder(order.id()).status()).as("原订10件中4件差异已关闭，实际合格6件全退即全部退货").isEqualTo("returned");
        assertThat(orders.listLines(order.id()).items().getFirst().quantity()).isEqualTo(10);
    }

    @Test
    void reversingInvoiceAfterAllSixAcceptedGoodsReturnDoesNotReissueRetainedFreightWithoutCash() {
        var order=sixAcceptedFourResolved(1_000L);
        actAs(finance);
        var invoice=returns.requestInvoice(order.id(),new DealerInvoiceRequest(key(),null));
        returns.issueInvoice(invoice.id(),new DealerInvoiceIssueRequest(key(),"INV-SIX-OFFSET",proof("dealer_invoice",invoice.id())));
        var ret=inspectAcceptedReturn(order.id(),6);
        assertThat(ret.refundCent()).isZero();
        recordRed(invoice.id(),"success");
        assertThat(returns.listInvoices(order.id()).items()).as("商品已全部退回，不因仍保留首程运费而重开").extracting(DealerInvoiceRecord::status).containsOnly("reversed");
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("returned");
    }

    @Test
    void refundingAllSixAcceptedGoodsClosesOriginalTenUnitOrderAfterActualCash() {
        var order=sixAcceptedFourResolved(11_000L);
        actAs(finance);
        var invoice=returns.requestInvoice(order.id(),new DealerInvoiceRequest(key(),null));
        assertThat(invoice.amountCent()).isEqualTo(7_000L);
        returns.issueInvoice(invoice.id(),new DealerInvoiceIssueRequest(key(),"INV-SIX-CASH",proof("dealer_invoice",invoice.id())));
        var ret=inspectAcceptedReturn(order.id(),6);
        assertThat(ret.refundCent()).isEqualTo(6_000L);
        recordRed(invoice.id(),"success");
        assertThat(returns.listInvoices(order.id()).items()).extracting(DealerInvoiceRecord::status).containsOnly("reversed");
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("returning");
        approve(returns.getReturn(ret.id()).refundReviewId());
        for(var refund:prepareRefunds(ret.id()).refunds()) settle(refund,"success");
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("returned");
        assertThat(orders.listLines(order.id()).items().getFirst().returnedQuantity()).isEqualTo(6);
    }

    @Test
    void completedAcceptedAsIsCreatesReturnSourceWithoutChangingOriginalReceipt() {
        var order=shippedOrder(1,2_000L);
        actAs(sales);
        var line=orders.listLines(order.id()).items().getFirst();
        var proof=List.of(DealerTestDeliveries.proof(admissionFiles,order.id()));
        delivery.receive(order.id(),new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest(key(),order.id(),Instant.now(),"合同联系人",proof,
                List.of(new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest.Line(line.id(),0,0,1,0)),"初验破损1件，待双方处置"));
        var difference=delivery.get(order.id()).discrepancies().getFirst();
        var plan=delivery.propose(difference.id(),new com.complex.platform.dealer.dto.DealerDeliveryResolutionRequest(key(),1,"price_adjustment","accepted_as_is",0L,
                "合同销售主体","双方确认原价留用，已检查可继续使用",proof,refundApprover,List.of(new com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance))));
        approveDeliveryPlan(plan.reviewId());
        assertThat(returnMoney.returnSources(order.id())).as("仅批准、尚未实际完成留用不能作为已接收退货来源").isEmpty();
        actAs(warehouse);
        delivery.completePhysical(plan.id(),new com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest(key(),Instant.now(),proof));
        assertThat(returnMoney.returnSources(order.id())).as("实际完成留用后可按原收货行选退货来源")
                .singleElement().satisfies(source->assertThat(source.availableQuantity()).isEqualTo(1));
        assertThat(jdbc.queryForObject("SELECT accepted_quantity FROM dealer_delivery_receipt_lines WHERE id=?",Integer.class,difference.receiptLineId())).isZero();
        var ret=inspectAcceptedReturn(order.id(),1);
        approve(ret.refundReviewId());
        for(var refund:prepareRefunds(ret.id()).refunds()) settle(refund,"success");
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("returned");
        assertThat(returnMoney.returnSources(order.id())).singleElement().satisfies(source->assertThat(source.availableQuantity()).isZero());
    }

    @Test
    void discountedAcceptedAsIsWaitsForPriceBasisWithoutUsingOtherGoodsBalance() {
        actAs(sales);
        orders.updateContractTerms(contractId,new DealerContractTermsRequest(key(),true,null,1_000L,3,30,1_000L,50_000L,"dealer",null,null,null,"payment_notified",null,"shipped",null));
        var order=shippedOrder(3,1_000L);
        actAs(sales);
        var line=orders.listLines(order.id()).items().getFirst();
        var evidence=List.of(DealerTestDeliveries.proof(admissionFiles,order.id()));
        delivery.receive(order.id(),new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest(key(),order.id(),Instant.now(),"合同联系人",evidence,
                List.of(new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest.Line(line.id(),2,0,1,0)),"合格2件，1件破损后协商留用"));
        var difference=delivery.get(order.id()).discrepancies().getFirst();
        var plan=delivery.propose(difference.id(),new com.complex.platform.dealer.dto.DealerDeliveryResolutionRequest(key(),1,"price_adjustment","accepted_as_is",200L,
                "合同销售主体","双方确认该1件减价200分留用",evidence,refundApprover,List.of(new com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance))));
        approveDeliveryPlan(plan.reviewId());actAs(warehouse);
        delivery.completePhysical(plan.id(),new com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest(key(),Instant.now(),evidence));
        actAs(sales);
        var requested=returns.requestReturn(order.id(),new DealerReturnRequest(key(),"quality_issue",
                List.of(new DealerReturnLineRequest(itemA,"箱",1,difference.receiptLineId())),"减价留用品退货"));
        actAs(reviewer);returns.decideReturn(requested.id(),new DealerReturnDecisionRequest(key(),"approve","确认实物退回"));
        actAs(warehouse);returns.receiveReturn(requested.id(),key());
        var before=orders.getOrder(order.id());
        var ledger=jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?",before.arApItemId());
        var inspection=new DealerReturnInspectionRequest(key(),List.of(new DealerReturnInspectionLineRequest(itemA,"箱",1,0,1,"质量退回")),
                "等待减价后退货金额规则",proof("dealer_return",requested.id()));
        assertThatThrownBy(()->isolated(()->returns.inspectReturn(requested.id(),inspection)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("DEALER_RETURN_PRICE_BASIS_PENDING"));
        assertThat(returns.getReturn(requested.id()).status()).isEqualTo("inspecting");
        assertThat(orders.getOrder(order.id()).returnedAmountCent()).isEqualTo(before.returnedAmountCent());
        assertThat(orders.listLines(order.id()).items().getFirst().returnedQuantity()).isZero();
        assertThat(jdbc.queryForMap("SELECT * FROM ar_ap_items WHERE id=?",before.arApItemId())).isEqualTo(ledger);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM dealer_order_refunds WHERE return_id=?",Integer.class,requested.id())).isZero();
    }

    private DealerOrderRecord sixAcceptedFourResolved(long paid) {
        actAs(sales);
        orders.updateContractTerms(contractId,new DealerContractTermsRequest(key(),true,null,1_000L,3,30,1_000L,50_000L,"dealer",null,null,null,"payment_notified",null,"shipped",null));
        var order=shippedOrder(10,paid);
        actAs(sales);
        var line=orders.listLines(order.id()).items().getFirst();
        var proof=List.of(DealerTestDeliveries.proof(admissionFiles,order.id()));
        delivery.receive(order.id(),new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest(key(),order.id(),Instant.now(),"合同联系人",proof,
                List.of(new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest.Line(line.id(),6,0,0,4)),"实际合格6件，短少4件"));
        assertThat(orders.getOrder(order.id()).deliveryCompletedAt()).isNull();
        assertThatThrownBy(()->isolated(()->returns.requestReturn(order.id(),new DealerReturnRequest(key(),"quality_issue",List.of(new DealerReturnLineRequest(itemA,"箱",6)),"交付差异未完成")))).isInstanceOf(ApiException.class);
        var difference=delivery.get(order.id()).discrepancies().getFirst();
        var plan=delivery.propose(difference.id(),new com.complex.platform.dealer.dto.DealerDeliveryResolutionRequest(key(),4,"price_adjustment","loss_confirmed",4_000L,
                "合同销售主体","双方确认未交4件减款并完成损失处理",proof,refundApprover,List.of(new com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance))));
        approveDeliveryPlan(plan.reviewId());
        actAs(warehouse);
        delivery.completePhysical(plan.id(),new com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest(key(),Instant.now(),proof));
        for(var refund:orders.listRefunds(order.id()).items()) {
            actAs(reviewer);
            var prepared=orders.executeRefund(refund.id(),new com.complex.platform.dealer.dto.DealerRefundExecuteRequest(key()));
            settle(new DealerReturnFinancialRecord.Refund(prepared.id(),"goods",prepared.amountCent(),prepared.status(),prepared.financeRefundId()),"success");
        }
        actAs(sales);
        assertThat(orders.getOrder(order.id()).deliveryCompletedAt()).isNotNull();
        assertThat(delivery.get(order.id()).pendingRefundCent()).isZero();
        return orders.getOrder(order.id());
    }

    private void approveDeliveryPlan(String reviewId) {
        actAs(refundApprover);
        for(int i=0;i<12 && "pending_review".equals(reviews.findReview(reviewId).orElseThrow().reviewStatus());i++) {
            var node=reviews.chain(reviewId).stream().filter(com.complex.platform.governance.chain.ApprovalChainNode::pending)
                    .min(java.util.Comparator.comparingInt(com.complex.platform.governance.chain.ApprovalChainNode::seq)).orElseThrow();
            actAs(node.approverId()==null?refundApprover:node.approverId());
            reviews.approve(reviewId,new SupervisorReviewApprovalRequest(key(),"按原组织审批",List.of(),null,"approved","实际数量、责任与款项已核"));
        }
        assertThat(reviews.findReview(reviewId).orElseThrow().reviewStatus()).isEqualTo("approved");
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
        AccessUser account=access.findUserById(userId).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, account.accountType(), account.organizationId(), account.merchantId(), "active"), "test", List.of()));
    }

    private static String key() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    @Test
    void actualFirstLegRefundChangesPendingInvoiceOnlyAfterSuccessfulCashResult() {
        var order=signedOrder(4,5_000L);var ret=inspectAcceptedReturn(order.id(),1);approve(ret.refundReviewId());
        actAs(finance);var invoice=returns.requestInvoice(order.id(),new DealerInvoiceRequest(key(),"原未退金额"));
        assertThat(invoice.amountCent()).isEqualTo(4_000L);
        approveFirstLeg(ret.id(),1_000L);
        actAs(reviewer);var pending=prepareRefunds(ret.id());
        assertThat(pending.refunds()).hasSize(2);
        assertThat(pending.firstLegRefundedCent()).isZero();
        assertThat(returns.listInvoices(order.id()).items().stream().filter(i->"pending".equals(i.status())).findFirst().orElseThrow().amountCent()).isEqualTo(4_000L);
        var goods=pending.refunds().stream().filter(r->"goods".equals(r.component())).findFirst().orElseThrow();
        var freight=pending.refunds().stream().filter(r->"first_leg".equals(r.component())).findFirst().orElseThrow();
        settle(goods,"success");settle(freight,"failed");
        assertThat(returnMoney.get(ret.id()).firstLegRefundedCent()).isZero();
        assertThat(returns.listInvoices(order.id()).items().stream().filter(i->"pending".equals(i.status())).findFirst().orElseThrow().amountCent()).isEqualTo(4_000L);
        settle(freight,"success");
        var current=returns.listInvoices(order.id()).items().stream().filter(i->"pending".equals(i.status())).findFirst().orElseThrow();
        assertThat(current.amountCent()).isEqualTo(3_000L);assertThat(current.retainedFreightCent()).isZero();
        assertThat(current.retainedGoodsCent()).isEqualTo(3_000L);
        assertThat(returns.listInvoices(order.id()).items().stream().filter(i->i.id().equals(invoice.id())).findFirst().orElseThrow().status()).isEqualTo("voided");
        assertThat(prepareRefunds(ret.id()).refunds()).hasSize(2);
        assertThat(orders.getOrder(order.id()).firstLegAdjustmentCent()).isEqualTo(1_000L);
    }

    @Test
    void actualFirstLegRefundRequiresRealReversalOfAlreadyIssuedInvoice() {
        var order=signedOrder(4,5_000L);var ret=inspectAcceptedReturn(order.id(),1);approve(ret.refundReviewId());
        actAs(finance);var invoice=returns.requestInvoice(order.id(),new DealerInvoiceRequest(key(),"已开票"));
        returns.issueInvoice(invoice.id(),new DealerInvoiceIssueRequest(key(),"INV-FREIGHT",proof("dealer_invoice",invoice.id())));
        approveFirstLeg(ret.id(),1_000L);
        for(var refund:prepareRefunds(ret.id()).refunds()) settle(refund,"success");
        var unchanged=returns.listInvoices(order.id()).items().getFirst();
        assertThat(unchanged.amountCent()).isEqualTo(4_000L);assertThat(unchanged.status()).isEqualTo("issued");assertThat(unchanged.requiresReversal()).isTrue();
        recordRed(invoice.id(),"processing");assertThat(returns.listInvoices(order.id()).items()).hasSize(1);
        recordRed(invoice.id(),"failed");assertThat(returns.listInvoices(order.id()).items().getFirst().status()).isEqualTo("issued");
        recordRed(invoice.id(),"success");
        var replacement=returns.listInvoices(order.id()).items().stream().filter(i->"pending".equals(i.status())).findFirst().orElseThrow();
        assertThat(replacement.amountCent()).isEqualTo(3_000L);assertThat(replacement.reissuedFromInvoiceId()).isEqualTo(invoice.id());
        assertThatThrownBy(()->isolated(()->recordRed(invoice.id(),"success"))).isInstanceOf(ApiException.class);
        assertThat(returns.listInvoices(order.id()).items()).hasSize(2);
    }

    @Test
    void unpaidFirstLegAdjustmentOffsetsOriginalReceivableAndKeepsNoCashRefund() {
        var order=signedOrder(4,2_000L);var ret=inspectAcceptedReturn(order.id(),1);
        approveFirstLeg(ret.id(),1_000L);actAs(reviewer);
        var facts=returnMoney.get(ret.id());assertThat(facts.firstLegOffsetCent()).isEqualTo(1_000L);assertThat(facts.firstLegRefundCent()).isZero();
        assertThat(facts.refunds()).isEmpty();
        assertThat(jdbc.queryForMap("SELECT amount_cent,paid_amount_cent,unpaid_amount_cent FROM ar_ap_items WHERE id=?",order.arApItemId()))
                .containsEntry("amount_cent",3_000L).containsEntry("paid_amount_cent",2_000L).containsEntry("unpaid_amount_cent",1_000L);
        actAs(finance);assertThat(returns.requestInvoice(order.id(),new DealerInvoiceRequest(key(),"依法按未退净额开票")).amountCent()).isEqualTo(3_000L);
    }

    @Test
    void normalQualityReturnRestoresOriginalCostAfterStockWasCleared() {
        jdbc.update("UPDATE inventory_stock_balances SET quantity=1,available_quantity=1 WHERE id=?","bal_dr_"+suffix);
        inventory.initCostStateIfAbsent(itemA);
        inventory.updateCostState(new com.complex.platform.inventory.InventoryItemCostStateRecord(itemA,null,1,500,500,500,0,"moving_average",true,Instant.now()));
        var order=signedOrder(1,2_000L);assertThat(inventory.findCostState(itemA).orElseThrow().totalCostCent()).isZero();
        var ret=inspectAcceptedReturn(order.id(),1);
        assertThat(balance("quantity")).isEqualTo(1);assertThat(balance("available_quantity")).isZero();assertThat(balance("quarantined_quantity")).isEqualTo(1);
        assertThat(inventory.findCostState(itemA).orElseThrow().totalCostCent()).isEqualTo(500L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inventory_quarantine_lots q JOIN dealer_return_lines l ON l.id=q.source_id WHERE q.source_type='dealer_return_line' AND l.return_id=? AND q.remaining_quantity=1",Integer.class,ret.id())).isEqualTo(1);
    }

    @Test
    void normalQualityReturnKeepsItsOriginalCostWhenNewStockHasDifferentCost() {
        jdbc.update("UPDATE inventory_stock_balances SET quantity=1,available_quantity=1 WHERE id=?","bal_dr_"+suffix);
        inventory.initCostStateIfAbsent(itemA);
        inventory.updateCostState(new com.complex.platform.inventory.InventoryItemCostStateRecord(itemA,null,1,500,500,500,0,"moving_average",true,Instant.now()));
        var order=signedOrder(1,2_000L);
        jdbc.update("UPDATE inventory_stock_balances SET quantity=1,available_quantity=1 WHERE id=?","bal_dr_"+suffix);
        inventory.saveStockMovement(new com.complex.platform.inventory.StockMovementRecord("other_"+suffix,"purchase",itemA,null,centralWarehouse,1,900L,null,
                "purchase","other_"+suffix,null,warehouse,List.of(),"completed",Instant.now(),null),"project_main");
        inspectAcceptedReturn(order.id(),1);
        assertThat(inventory.findCostState(itemA).orElseThrow().totalCostCent()).isEqualTo(1_400L);
        assertThat(balance("available_quantity")).isEqualTo(1);assertThat(balance("quarantined_quantity")).isEqualTo(1);
    }

    @Test
    void repeatedReturnRequestReplaysOriginalBeforeRecheckingConsumedSource() {
        var order=signedOrder(2,3_000L);actAs(sales);
        var request=new DealerReturnRequest(key(),"quality_issue",List.of(new DealerReturnLineRequest(itemA,"箱",2)),"原实际签收全部退回");
        var first=returns.requestReturn(order.id(),request);
        var replay=returns.requestReturn(order.id(),request);
        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dealer_returns WHERE order_id=?",Integer.class,order.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT SUM(requested_quantity) FROM dealer_return_lines WHERE return_id=?",Integer.class,first.id())).isEqualTo(2);
        String other="project_ret_replay_"+suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (?,'complex','异项目不可重放退货','active',now(),now())",other);
        String user=returnFinanceStaff("replay_other",other,other);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES (?,'dealer.return.manage')","role_ret_replay_other_"+suffix);
        actAsStaff(user);
        assertThatThrownBy(()->isolated(()->returns.requestReturn(order.id(),request)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
    }

    @Test
    void projectReturnFinanceWithoutMerchantOrOrderRefundPermissionCompletesRealRefundOnlyForReturnSource() {
        String project=realReturnFinanceProject();
        var order=signedOrder(2,3_000L);var ret=inspectAcceptedReturn(order.id(),1);approve(ret.refundReviewId());
        String executor=returnFinanceStaff("exec",project,project),confirmer=returnFinanceStaff("confirm",project,project);
        actAsStaff(executor);var facts=returnMoney.executeRefunds(ret.id(),new com.complex.platform.dealer.dto.DealerIdempotentRequest(key()));
        var refund=facts.refunds().getFirst();assertThat(refund.financeRefundId()).isNotBlank();
        financeService.recordFinanceRefundExecution(refund.financeRefundId(),new com.complex.platform.finance.dto.RefundExecutionRequest(key(),"实际退货原路执行",proof("finance_refund",refund.financeRefundId()),"RETURN-PROJECT-RF",null));
        actAsStaff(confirmer);financeService.confirmFinanceRefundResult(refund.financeRefundId(),new com.complex.platform.finance.dto.RefundResultConfirmRequest(key(),"独立确认",proof("finance_refund",refund.financeRefundId()),"success",refund.amountCent(),"RETURN-PROJECT-RF",executor,null));
        assertThat(returnMoney.get(ret.id()).goodsRefundedCent()).isEqualTo(1_000L);
        String ordinary="ordinary_"+suffix;
        jdbc.update("""
                INSERT INTO dealer_order_refunds(id,order_id,payment_id,amount_cent,status,review_id,requested_by,reason,created_at,updated_at)
                SELECT ?,order_id,payment_id,100,'approved',review_id,requested_by,'普通退款权限对照',now(),now() FROM dealer_order_refunds WHERE id=?
                """,ordinary,refund.id());
        actAsStaff(executor);
        String sourceReference=jdbc.queryForObject("SELECT p.reference_no FROM dealer_order_payments p JOIN dealer_order_refunds r ON r.payment_id=p.id WHERE r.id=?",String.class,ordinary);
        assertThatThrownBy(()->isolated(()->financeService.recordDealerOrderRefund(ordinary,100,"bank_transfer",sourceReference,project,"普通退款不得借退货权限"))).isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT finance_refund_id FROM dealer_order_refunds WHERE id=?",String.class,ordinary)).isNull();
    }

    @Test
    void returnRefundResultNeedsPaymentRegisterPermissionForSameRealOrderProject() {
        String project=realReturnFinanceProject();
        var order=signedOrder(2,3_000L);var ret=inspectAcceptedReturn(order.id(),1);approve(ret.refundReviewId());
        String executor=returnFinanceStaff("exec2",project,project);actAsStaff(executor);
        var refund=returnMoney.executeRefunds(ret.id(),new com.complex.platform.dealer.dto.DealerIdempotentRequest(key())).refunds().getFirst();
        financeService.recordFinanceRefundExecution(refund.financeRefundId(),new com.complex.platform.finance.dto.RefundExecutionRequest(key(),"实际执行",proof("finance_refund",refund.financeRefundId()),"RETURN-SCOPE-RF",null));
        String other="project_ret_other_"+suffix;jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (?,'complex','仅确认另项目','active',now(),now())",other);
        String confirmer=returnFinanceStaff("confirm2",project,other);actAsStaff(confirmer);
        var evidence=proof("finance_refund",refund.financeRefundId());
        assertThatThrownBy(()->isolated(()->financeService.confirmFinanceRefundResult(refund.financeRefundId(),new com.complex.platform.finance.dto.RefundResultConfirmRequest(
                key(),"确认权不可跨用",evidence,"success",refund.amountCent(),"RETURN-SCOPE-RF",executor,null)))).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT status FROM finance_refunds WHERE id=?",String.class,refund.financeRefundId())).isEqualTo("processing");
        assertThat(returnMoney.get(ret.id()).goodsRefundedCent()).isZero();
    }

    @Test
    void crossProjectPrepaymentReturnPreservesOriginalPathAndRequiresBothProjectScopes() {
        var setup=crossProjectPrepaidReturn();
        String onlyA=returnFinanceStaff("only_a",setup.sourceProject(),setup.sourceProject());
        String onlyB=returnFinanceStaff("only_b",setup.targetProject(),setup.targetProject());
        var prepare=new com.complex.platform.dealer.dto.DealerIdempotentRequest(key());
        for(String user:List.of(onlyA,onlyB)) {
            actAsStaff(user);
            assertThatThrownBy(()->isolated(()->returnMoney.executeRefunds(setup.returnId(),prepare))).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        }
        String executor=returnFinanceStaff("cross_exec",setup.targetProject(),setup.targetProject());
        allowSourceProject("cross_exec",setup.sourceProject(),true);
        actAsStaff(executor);
        assertThatCode(()->returnMoney.executeRefunds(setup.returnId(),prepare))
                .as("同经销商同销售主体的跨项目预付款允许沿原项目收款路径退货退款").doesNotThrowAnyException();
        var refund=returnMoney.get(setup.returnId()).refunds().getFirst();
        assertThat(jdbc.queryForMap("SELECT project_id,refund_channel,original_payment_ref FROM finance_refunds WHERE id=?",refund.financeRefundId()))
                .containsEntry("project_id",setup.targetProject()).containsEntry("refund_channel","bank_transfer").containsEntry("original_payment_ref",setup.originalReference());
        actAsStaff(onlyB);
        assertThatThrownBy(()->isolated(()->returnMoney.executeRefunds(setup.returnId(),prepare))).as("准备待办旧key仍须有原款项目授权").isInstanceOf(ApiException.class);
        actAsStaff(executor);
        String reference="CROSS-REAL-"+key();
        var execute=new com.complex.platform.finance.dto.RefundExecutionRequest(key(),"按A原款实际退回",proof("finance_refund",refund.financeRefundId()),reference,null);
        for(String user:List.of(onlyA,onlyB)) {
            actAsStaff(user);
            assertThatThrownBy(()->isolated(()->financeService.recordFinanceRefundExecution(refund.financeRefundId(),execute))).isInstanceOf(ApiException.class);
            assertRefundFailure("finance_refund.execution_failed",refund.financeRefundId(),execute.idempotencyKey(),user);
        }
        actAsStaff(executor);
        financeService.recordFinanceRefundExecution(refund.financeRefundId(),execute);
        actAsStaff(onlyB);
        assertThatThrownBy(()->isolated(()->financeService.recordFinanceRefundExecution(refund.financeRefundId(),execute))).as("已执行旧key仍须重新核原款项目").isInstanceOf(ApiException.class);
        assertRefundFailure("finance_refund.execution_failed",refund.financeRefundId(),execute.idempotencyKey(),onlyB);

        String confirmer=returnFinanceStaff("cross_confirm",setup.targetProject(),setup.targetProject());
        allowSourceProject("cross_confirm",setup.sourceProject(),false);
        actAsStaff(confirmer);
        var confirm=new com.complex.platform.finance.dto.RefundResultConfirmRequest(key(),"核实A原款退款",proof("finance_refund",refund.financeRefundId()),"success",refund.amountCent(),reference,executor,null);
        assertThatThrownBy(()->isolated(()->financeService.confirmFinanceRefundResult(refund.financeRefundId(),confirm)))
                .as("有A/B退款权但只有B收付款确认权，不能确认A原款结果").isInstanceOf(ApiException.class);
        assertRefundFailure("finance_refund.confirm_failed",refund.financeRefundId(),confirm.idempotencyKey(),confirmer);
        allowSourceProject("cross_confirm",setup.sourceProject(),true);
        financeService.confirmFinanceRefundResult(refund.financeRefundId(),confirm);
        assertThat(returns.getReturn(setup.returnId()).status()).isEqualTo("completed");
        jdbc.update("DELETE FROM role_viewable_projects WHERE role_id=? AND viewable_id=?","role_ret_cross_confirm_"+suffix+"_pay",setup.sourceProject());
        assertThatThrownBy(()->isolated(()->financeService.confirmFinanceRefundResult(refund.financeRefundId(),confirm)))
                .as("成功旧key也不能绕过已收窄的原款确认范围").isInstanceOf(ApiException.class);
        assertRefundFailure("finance_refund.confirm_failed",refund.financeRefundId(),confirm.idempotencyKey(),confirmer);
        actAs(finance);
        assertThat(orders.listOverpayments(dealerId).items().getFirst().prepaymentBalanceCent()).isEqualTo(2_000L);
    }

    private record CrossProjectReturn(String sourceProject,String targetProject,String returnId,String originalReference) {}
    private CrossProjectReturn crossProjectPrepaidReturn() {
        String sourceProject=realReturnFinanceProject();
        var source=signedOrder(1,7_000L);
        var payment=orders.listPayments(source.id()).items().getFirst();
        actAs(finance);
        var over=orders.listOverpayments(dealerId).items().getFirst();
        var evidence=proof("dealer_overpayment",over.id());
        orders.confirmOverpaymentUse(over.id(),new com.complex.platform.dealer.dto.DealerOverpaymentUseRequest(key(),"prepayment",null,"双方确认5000分留作预付款",5_000L,evidence));

        String targetProject="project_ret_target_"+suffix;
        centralWarehouse="wh_ret_target_"+suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (?,'complex','目标交付项目','active',now(),now())",targetProject);
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status) VALUES (?,?,'main','目标项目中心仓','active')",centralWarehouse,targetProject);
        jdbc.update("INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,updated_at,warehouse_id,project_id) VALUES (?,?,?,100,0,100,now(),?,?)","bal_target_"+suffix,itemA,centralWarehouse,centralWarehouse,targetProject);
        jdbc.update("UPDATE access_users SET organization_id=? WHERE id=?",targetProject,warehouse);
        actAs(sales);
        var contract=dealerService.createContract(dealerId,new DealerContractRequest(key(),"TARGET-"+suffix,salesEntity,centralWarehouse,LocalDate.now().minusDays(1),LocalDate.now().plusYears(1)));
        contractId=contract.id();
        orders.updateContractTerms(contractId,new DealerContractTermsRequest(key(),true,3000,null,3,30,1_000L,50_000L,"dealer",null,null,null,"payment_notified",null,"shipped",null));
        var catalog=dealerService.createCatalogDraft(contractId,new DealerCatalogDraftRequest(key(),"目标项目目录",List.of(new DealerCatalogLineRequest(itemA,"箱",1_000L,1300,null))));
        dealerService.submitCatalog(catalog.id(),key());
        actAs(reviewer);dealerService.decideCatalog(catalog.id(),new DealerCatalogDecisionRequest(key(),"approve","同意"));
        actAs(sales);
        var target=orders.createOrderDraft(dealerId,new DealerOrderDraftRequest(key(),contractId,List.of(new DealerOrderLineRequest(itemA,"箱",2,null)),"跨项目预付款采购"));
        orders.submitOrder(target.id(),key());actAs(reviewer);orders.reviewOrder(target.id(),new DealerOrderDecisionRequest(key(),"approve","同意"));
        actAs(finance);
        orders.confirmOverpaymentUse(over.id(),new com.complex.platform.dealer.dto.DealerOverpaymentUseRequest(key(),"prepayment_offset",target.id(),"双方确认抵目标订单3000分",3_000L,evidence));
        actAs(warehouse);orders.shipOrder(target.id(),new DealerShipRequest(key(),"真实出库"));
        actAs(sales);delivery.receive(target.id(),DealerTestDeliveries.full(target.id(),orders.listLines(target.id()).items(),DealerTestDeliveries.proof(admissionFiles,target.id())));
        var ret=inspectAcceptedReturn(target.id(),2);approve(ret.refundReviewId());
        return new CrossProjectReturn(sourceProject,targetProject,ret.id(),payment.referenceNo());
    }

    private void allowSourceProject(String tag,String project,boolean payment) {
        jdbc.update("INSERT INTO role_viewable_projects(role_id,viewable_id) VALUES (?,?) ON CONFLICT DO NOTHING","role_ret_"+tag+"_"+suffix,project);
        if(payment) jdbc.update("INSERT INTO role_viewable_projects(role_id,viewable_id) VALUES (?,?) ON CONFLICT DO NOTHING","role_ret_"+tag+"_"+suffix+"_pay",project);
    }

    private void assertRefundFailure(String event,String refundId,String key,String user) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name=? AND business_id=? AND idempotency_key=? AND actor_id=? AND event_result='failure'",Integer.class,event,refundId,key,user)).isPositive();
    }

    private String realReturnFinanceProject() {
        String project="project_ret_actual_"+suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (?,'complex','退货真实授权项目','active',now(),now())",project);
        jdbc.update("UPDATE warehouses SET project_id=? WHERE id=?",project,centralWarehouse);
        jdbc.update("UPDATE access_users SET organization_id=? WHERE id=?",project,warehouse);
        jdbc.update("UPDATE inventory_stock_balances SET project_id=? WHERE warehouse_id=?",project,centralWarehouse);
        return project;
    }
    private String returnFinanceStaff(String tag,String operationProject,String confirmationProject) {
        String user="usr_ret_"+tag+"_"+suffix,role="role_ret_"+tag+"_"+suffix,payRole=role+"_pay";
        String displaySuffix=new java.math.BigInteger(key(),16).toString();
        jdbc.update("INSERT INTO roles(id,role_name,data_scope,status,created_at) VALUES (?,?,'project','active',now()),(?,?,'project','active',now())",role,"经销退款办理"+displaySuffix,payRole,"经销退款确认"+displaySuffix);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN ('dealer.return.view','dealer.return.refund.execute','finance.ar_ap.view','finance.refund.create') OR permission_code LIKE 'evidence.%'",role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES (?,'finance.payment.register')",payRole);
        jdbc.update("INSERT INTO role_viewable_projects(role_id,viewable_id) VALUES (?,?),(?,?)",role,operationProject,payRole,confirmationProject);
        access.createUser(new AccessUser(user,user,"staff",operationProject,null,"active",Instant.now()));access.assignUserRole(user,role);access.assignUserRole(user,payRole);return user;
    }
    private void actAsStaff(String user) {
        var account=access.findUserById(user).orElseThrow();SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(user,user,"staff",account.organizationId(),null,"active"),"test",List.of()));
    }

    private void approveFirstLeg(String returnId,long amount) {
        actAs(warehouse);returns.confirmReturnFreight(returnId,new DealerReturnFreightConfirmRequest(key(),"sales_entity",true,"双方确认首程退款金额",amount,proof("dealer_return",returnId)));
        String review=jdbc.queryForObject("SELECT review_id FROM dealer_return_freight_adjustments WHERE return_id=? AND status<>'rejected'",String.class,returnId);
        approve(review);
    }
    private void approve(String reviewId) {
        actAs(refundApprover);
        for(int i=0;i<5 && "pending_review".equals(reviews.findReview(reviewId).orElseThrow().reviewStatus());i++)
            reviews.approve(reviewId,new SupervisorReviewApprovalRequest(key(),"真实业务审批",List.of(),null,"approved","同意"));
        assertThat(reviews.findReview(reviewId).orElseThrow().reviewStatus()).isEqualTo("approved");
    }

    private List<String> proof(String type,String id) {
        return List.of(admissionFiles.uploadAttachment(new org.springframework.mock.web.MockMultipartFile("file","实际业务凭证.png","image/png",
                java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jz1sAAAAASUVORK5CYII=")),type,id,"other").id());
    }
    private void isolated(Runnable operation) {
        TransactionTemplate nested=new TransactionTemplate(transactionManager);nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        nested.executeWithoutResult(status->operation.run());
    }
    private DealerReturnFinancialRecord prepareRefunds(String returnId) {
        actAs(reviewer);return returnMoney.executeRefunds(returnId,new com.complex.platform.dealer.dto.DealerIdempotentRequest(key()));
    }
    private void settle(DealerReturnFinancialRecord.Refund refund,String status) {
        actAs(reviewer);String reference="REAL-RF-"+key();
        financeService.recordFinanceRefundExecution(refund.financeRefundId(),new com.complex.platform.finance.dto.RefundExecutionRequest(
                key(),"按原收款路线实际退款",proof("finance_refund",refund.financeRefundId()),reference,null));
        actAs(refundApprover);
        financeService.confirmFinanceRefundResult(refund.financeRefundId(),new com.complex.platform.finance.dto.RefundResultConfirmRequest(
                key(),"独立核实实际银行结果",proof("finance_refund",refund.financeRefundId()),status,refund.amountCent(),reference,reviewer,
                "failed".equals(status)?"银行明确失败":null));
    }
    private void recordRed(String invoiceId,String status) {
        actAs(finance);returnMoney.recordReversal(invoiceId,new com.complex.platform.dealer.dto.DealerInvoiceReversalResultRequest(
                key(),status,"REAL-RED-"+key(),proof("dealer_invoice",invoiceId),"failed".equals(status)?"税务系统明确失败":null));
    }

}
