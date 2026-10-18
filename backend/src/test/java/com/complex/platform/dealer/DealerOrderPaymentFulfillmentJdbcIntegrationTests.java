package com.complex.platform.dealer;

// 文件版本：V0.03

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
    @Autowired private com.complex.platform.finance.FinanceService financeService;
    @Autowired private DealerOrderService orders;
    @Autowired private DealerDeliveryService delivery;
    @Autowired private DealerReturnService dealerReturns;
    @Autowired private com.complex.platform.inventory.InventoryQuarantineService quarantine;
    @Autowired private com.complex.platform.inventory.InventoryRepository inventoryRepository;
    @Autowired private com.complex.platform.bar.BarRepository barRepository;
    @Autowired private JdbcDealerOrderRepository orderRepository;
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
        jdbc.update("INSERT INTO role_permissions(role_id, permission_code) SELECT ?, permission_code FROM permissions WHERE permission_code LIKE 'dealer.%' OR permission_code LIKE 'governance.supervisor_review.%' OR permission_code LIKE 'inventory.%' OR permission_code LIKE 'finance.%' OR permission_code LIKE 'evidence.%' ON CONFLICT DO NOTHING", role);
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

    /** FS-004 / D01：现有签收 API 不能忽略实际数量，将部分收货当整单交付。 */
    @Test
    void partialReceiptMustKeepDeliveryOpenAndNotStartReceiptAnchoredBalance() throws Exception {
        actAs(sales);
        orders.updateContractTerms(creditContract, new DealerContractTermsRequest(key(), true, 3000, null,
                3, 30, 1_000L, 50_000L, "dealer", null, null, Instant.now().truncatedTo(ChronoUnit.MICROS), "payment_notified", null, "signed", null));
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), creditContract,
                List.of(new DealerOrderLineRequest(itemA, "箱", 10, null)), "分次签收"));
        orders.submitOrder(draft.id(), key());
        actAs(reviewer);
        DealerOrderRecord approved = orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "通过"));
        actAs(sales);
        DealerOrderPaymentRecord payment = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer",
                approved.totalAmountCent(), Instant.now().truncatedTo(ChronoUnit.MICROS), "FS004-PAID"));
        actAs(finance);
        orders.verifyPayment(payment.id(), new DealerPaymentVerifyRequest(key(), "approve", "到账"));
        actAs(warehouse);
        orders.shipOrder(draft.id(), new DealerShipRequest(key(), "发出10箱"));
        String lineId = orders.listLines(draft.id()).items().get(0).id();
        actAs(sales);
        String proof = admissionFiles.uploadAttachment(new org.springframework.mock.web.MockMultipartFile(
                "file", "实际签收凭证.png", "image/png", java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jz1sAAAAASUVORK5CYII=")),
                "dealer_delivery", draft.id(), "other").id();
        String body = """
                {"idempotencyKey":"%s","deliveryBatchId":"%s","confirmedAt":"%s",
                 "buyerContact":"合同收货联系人","evidenceAttachmentIds":["%s"],
                 "lines":[{"orderLineId":"%s","acceptedQuantity":6,"rejectedQuantity":0,"damagedQuantity":0,"shortQuantity":0}],
                 "note":"本次实际合格收到6箱，其余4箱尚未处理"}
                """.formatted(key(), draft.id(), Instant.now().truncatedTo(ChronoUnit.MICROS), proof, lineId);
        org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new DealerOrderController(orders, delivery)).build()
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/dealers/orders/" + draft.id() + "/sign")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        DealerOrderRecord partial = orders.getOrder(draft.id());
        assertThat(partial.status()).as("全额付款不能把只签收6/10箱的交付完成").isEqualTo("shipping");
        assertThat(partial.signedAt()).isNull();
        assertThat(partial.balanceDueAt()).as("合同仅约签收后30天，全部数量处理完前不起算").isNull();
        assertThat(balance("quantity")).isEqualTo(90);
        var rest = new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest(key(), draft.id(), Instant.now().truncatedTo(ChronoUnit.MICROS), "合同收货联系人", List.of(proof),
                List.of(new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest.Line(lineId, 4, 0, 0, 0)), "余量实际收齐");
        DealerOrderRecord completed = delivery.receive(draft.id(), rest);
        assertThat(completed.status()).isEqualTo("completed");
        assertThat(completed.deliveryCompletedAt()).isEqualTo(rest.confirmedAt());
        assertThat(completed.balanceDueAt()).isEqualTo(rest.confirmedAt().plusSeconds(30L * 86400));
        assertThat(delivery.receive(draft.id(), rest).deliveryCompletedAt()).isEqualTo(completed.deliveryCompletedAt());
        assertThat(delivery.get(draft.id()).receipts()).hasSize(2);
    }

    @Test
    void approvedPhysicalReturnQuarantinesRealStockAndRefundsTwoOriginalPaymentsOnlyAfterFinanceSuccess() throws Exception {
        inventoryRepository.initCostStateIfAbsent(itemA);
        inventoryRepository.updateCostState(new com.complex.platform.inventory.InventoryItemCostStateRecord(itemA,null,100,50_000,500,50_000,0,"moving_average",true,Instant.now()));
        DealerOrderRecord order = deliveryOrder(10, 6_000L, 5_000L);
        actAs(sales);
        var line = orders.listLines(order.id()).items().getFirst();
        String proof = DealerTestDeliveries.proof(admissionFiles, order.id());
        delivery.receive(order.id(), new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest(key(), order.id(), Instant.now().truncatedTo(ChronoUnit.MICROS), "合同联系人", List.of(proof),
                List.of(new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest.Line(line.id(), 0, 0, 10, 0)), "实际收到10箱破损品"));
        var difference = delivery.get(order.id()).discrepancies().getFirst();
        var proposed = delivery.propose(difference.id(), resolutionRequest(10, "price_adjustment", "return_quarantine", 10_000L, proof));
        assertThat(orders.getOrder(order.id()).netReceivableCent()).isEqualTo(11_000L);
        assertThat(balance("quantity")).isEqualTo(90);
        approveDeliveryReview(proposed.reviewId());
        actAs(warehouse);
        assertThat(orders.getOrder(order.id()).deliveryCompletedAt()).isNull();
        var physical = new com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest(key(), Instant.now().truncatedTo(ChronoUnit.MICROS), List.of(proof));
        delivery.completePhysical(proposed.id(), physical);
        assertThat(balance("quantity")).isEqualTo(100);
        assertThat(balance("available_quantity")).isEqualTo(90);
        assertThat(balance("quarantined_quantity")).isEqualTo(10);
        assertThat(inventoryRepository.findCostState(itemA).orElseThrow().totalCostCent()).isEqualTo(50_000L);
        org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new com.complex.platform.inventory.InventoryQuarantineController(quarantine)).build()
                .perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/inventory/quarantine").param("warehouse_id", centralWarehouse))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        var lot = quarantine.list(centralWarehouse).getFirst();
        assertThat(lot.remainingQuantity()).isEqualTo(10);
        assertIsolationScope(lot.id());
        assertThat(jdbc.queryForMap("SELECT amount_cent,paid_amount_cent,unpaid_amount_cent FROM ar_ap_items WHERE id=?", order.arApItemId()))
                .containsEntry("amount_cent",1_000L).containsEntry("paid_amount_cent",1_000L).containsEntry("unpaid_amount_cent",0L);
        assertThat(orders.getOrder(order.id()).paidAmountCent()).as("原收款事实保留，多收部分单列待退款").isEqualTo(11_000L);
        assertThat(inventoryRepository.reserveStockBalance(itemA, centralWarehouse, 91)).isZero();
        assertThat(inventoryRepository.atomicDecreaseStockBalance(itemA, centralWarehouse, 91, null)).isZero();
        assertThat(orderRepository.reserveInWarehouse(itemA, centralWarehouse, 91)).isFalse();
        assertThat(barRepository.atomicDecreaseStock(itemA, centralWarehouse, 91)).isFalse();
        assertThatThrownBy(() -> isolatedFailure(() -> inventoryRepository.saveStockBalance(
                new com.complex.platform.inventory.StockBalanceRecord("bal_do_" + suffix, itemA, centralWarehouse, 100, 0, 100, Instant.now(), null, centralWarehouse))))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(balance("available_quantity")).isEqualTo(90);
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("signed");
        assertThat(orders.getOrder(order.id()).deliveryCompletedAt()).isNotNull();
        assertThat(orders.getOrder(order.id()).netReceivableCent()).isEqualTo(1_000L);
        assertThat(delivery.get(order.id()).pendingRefundCent()).isEqualTo(10_000L);
        List<DealerOrderRefundRecord> refunds = orders.listRefunds(order.id()).items();
        assertThat(refunds).hasSize(2).allSatisfy(r -> assertThat(r.reviewId()).isEqualTo(proposed.reviewId()));
        assertThat(refunds.stream().mapToLong(DealerOrderRefundRecord::amountCent).sum()).isEqualTo(10_000L);
        delivery.completePhysical(proposed.id(), physical);
        assertThat(orders.listRefunds(order.id()).items()).hasSize(2);
        for (var refund : refunds) {
            actAs(warehouse);
            var prepared = orders.executeRefund(refund.id(), new DealerRefundExecuteRequest(key()));
            executeAndConfirmRefund(prepared, "success");
        }
        actAs(warehouse);
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("completed");
        assertThat(delivery.get(order.id()).pendingRefundCent()).isZero();
        assertThat(quarantine.list(centralWarehouse).getFirst().remainingQuantity()).as("退款到账不能把仍在隔离的实物清掉").isEqualTo(10);
        String inspection = admissionFiles.uploadAttachment(DealerTestDeliveries.file(), "inventory_quarantine", lot.id(), "other").id();
        String inspectionVideo = admissionFiles.uploadAttachment(DealerTestDeliveries.video(), "inventory_quarantine", lot.id(), "other").id();
        var disposition = quarantine.propose(lot.id(), new com.complex.platform.inventory.InventoryQuarantineDispositionRequest(key(), 4, "scrap", "4箱经复核无法恢复，按批准方案实际处置",
                List.of(inspection, inspectionVideo), refundApprover, List.of(new com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine("finance_review", finance))));
        approveDeliveryReview(disposition.reviewId());
        actAs(warehouse);
        assertThat(balance("quantity")).isEqualTo(100);
        var completed = new com.complex.platform.inventory.InventoryQuarantineCompleteRequest(key(), Instant.now().truncatedTo(ChronoUnit.MICROS), List.of(inspection, inspectionVideo));
        quarantine.complete(disposition.id(), completed);
        quarantine.complete(disposition.id(), completed);
        assertThat(balance("quantity")).isEqualTo(96);
        assertThat(balance("available_quantity")).isEqualTo(90);
        assertThat(balance("quarantined_quantity")).isEqualTo(6);
        assertThat(inventoryRepository.findCostState(itemA).orElseThrow().onHandQuantity()).isEqualTo(96);
        assertThat(inventoryRepository.findCostState(itemA).orElseThrow().totalCostCent()).isEqualTo(48_000L);
        assertThat(quarantine.list(centralWarehouse).getFirst().remainingQuantity()).isEqualTo(6);
    }

    @Test
    void clearedStockReturnRestoresOriginalCostAndScrapUsesRestoredCost() throws Exception {
        seedSingleUnitCost();
        var order=deliveryOrder(1,2_000L);
        assertThat(inventoryRepository.findCostState(itemA).orElseThrow().totalCostCent()).isZero();
        var lot=returnDamagedUnit(order);
        assertThat(inventoryRepository.findCostState(itemA).orElseThrow().totalCostCent())
                .as("全部出库后退回仍须恢复原出库500分成本").isEqualTo(500L);
        assertThat(jdbc.queryForObject("SELECT total_cost_cent FROM inventory_stock_movements WHERE id=?",Long.class,"movement_"+lot.sourceId())).isEqualTo(500L);
        String disposition=scrapUnit(lot.id());
        assertThat(inventoryRepository.findCostState(itemA).orElseThrow().totalCostCent()).isZero();
        assertThat(jdbc.queryForObject("SELECT total_cost_cent FROM inventory_stock_movements WHERE id=?",Long.class,"movement_"+disposition)).isEqualTo(500L);
    }

    @Test
    void mixedStockReturnRestoresItsOwnSourceCostBeforeWeightedScrap() throws Exception {
        seedSingleUnitCost();
        var order=deliveryOrder(1,2_000L);
        jdbc.update("UPDATE inventory_stock_balances SET quantity=1,available_quantity=1 WHERE id=?","bal_do_"+suffix);
        inventoryRepository.saveStockMovement(new com.complex.platform.inventory.StockMovementRecord("other_receipt_"+suffix,"purchase",itemA,null,centralWarehouse,1,900L,null,
                "purchase","other_purchase_"+suffix,null,warehouse,List.of(),"completed",Instant.now(),null),"project_main");
        var lot=returnDamagedUnit(order);
        assertThat(inventoryRepository.findCostState(itemA).orElseThrow().totalCostCent())
                .as("新批成本900不能替代退回原批的500成本").isEqualTo(1_400L);
        assertThat(jdbc.queryForObject("SELECT total_cost_cent FROM inventory_stock_movements WHERE id=?",Long.class,"movement_"+lot.sourceId())).isEqualTo(500L);
        String disposition=scrapUnit(lot.id());
        assertThat(inventoryRepository.findCostState(itemA).orElseThrow().onHandQuantity()).isEqualTo(1);
        assertThat(inventoryRepository.findCostState(itemA).orElseThrow().totalCostCent()).isEqualTo(700L);
        assertThat(jdbc.queryForObject("SELECT total_cost_cent FROM inventory_stock_movements WHERE id=?",Long.class,"movement_"+disposition)).isEqualTo(700L);
    }

    @Test
    void splitReturnsRestoreExactSourceTotalIncludingLastCent() {
        jdbc.update("UPDATE inventory_stock_balances SET quantity=3,available_quantity=3 WHERE id=?","bal_do_"+suffix);
        inventoryRepository.initCostStateIfAbsent(itemA);
        inventoryRepository.updateCostState(new com.complex.platform.inventory.InventoryItemCostStateRecord(itemA,null,3,100,33,100,0,"moving_average",true,Instant.now()));
        var order=deliveryOrder(3,4_000L);actAs(sales);
        var line=orders.listLines(order.id()).items().getFirst();
        String proof=DealerTestDeliveries.proof(admissionFiles,order.id());
        delivery.receive(order.id(),new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest(key(),order.id(),Instant.now().truncatedTo(ChronoUnit.MICROS),"合同联系人",List.of(proof),
                List.of(new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest.Line(line.id(),0,0,3,0)),"三箱分次退回"));
        var difference=delivery.get(order.id()).discrepancies().getFirst();
        for(int quantity:List.of(1,2)) {
            actAs(sales);
            var plan=delivery.propose(difference.id(),resolutionRequest(quantity,"price_adjustment","return_quarantine",0L,proof));
            approveDeliveryReview(plan.reviewId());actAs(warehouse);
            var actual=new com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest(key(),Instant.now().truncatedTo(ChronoUnit.MICROS),List.of(proof));
            delivery.completePhysical(plan.id(),actual);
            delivery.completePhysical(plan.id(),actual);
            assertThat(jdbc.queryForObject("SELECT total_cost_cent FROM inventory_stock_movements WHERE id=?",Long.class,"movement_"+plan.id()))
                    .isEqualTo(quantity==1?33L:67L);
        }
        assertThat(inventoryRepository.findCostState(itemA).orElseThrow().onHandQuantity()).isEqualTo(3);
        assertThat(inventoryRepository.findCostState(itemA).orElseThrow().totalCostCent()).isEqualTo(100L);
    }

    @Test
    void unknownOriginalCostCannotBeReplacedByKnownLaterPurchaseCost() {
        var order=deliveryOrder(1,2_000L);
        assertThat(jdbc.queryForObject("SELECT total_cost_cent FROM inventory_stock_movements WHERE business_type='dealer_order' AND business_id=?",Long.class,order.id())).isNull();
        jdbc.update("UPDATE inventory_stock_balances SET quantity=quantity+1,available_quantity=available_quantity+1 WHERE id=?","bal_do_"+suffix);
        inventoryRepository.saveStockMovement(new com.complex.platform.inventory.StockMovementRecord("later_receipt_"+suffix,"purchase",itemA,null,centralWarehouse,1,900L,null,
                "purchase","later_purchase_"+suffix,null,warehouse,List.of(),"completed",Instant.now(),null),"project_main");
        int before=balance("quantity");
        assertThatThrownBy(()->isolatedFailure(()->returnDamagedUnit(order)))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code()).isEqualTo("INVENTORY_RETURN_COST_UNKNOWN"));
        assertThat(balance("quantity")).isEqualTo(before);
        assertThat(balance("quarantined_quantity")).isZero();
        assertThat(inventoryRepository.findCostState(itemA).orElseThrow().totalCostCent()).isEqualTo(900L);
    }

    @Test
    void invoiceRequestAndPaymentVerificationDoNotDeadlockOnOrderAndDealerForeignKeys() throws Exception {
        var order=deliveryOrderWithAnchor("shipped",1,1_000L);actAs(sales);
        String proof=DealerTestDeliveries.proof(admissionFiles,order.id());
        delivery.receive(order.id(),DealerTestDeliveries.full(order.id(),orders.listLines(order.id()).items(),proof));
        var payment=orders.registerPayment(order.id(),new DealerPaymentRequest(key(),"bank_transfer",1_000L,Instant.now(),"FK-PAY-"+suffix));
        var paymentRequest=new DealerPaymentVerifyRequest(key(),"approve","实际尾款到账");
        var invoiceRequest=new com.complex.platform.dealer.dto.DealerInvoiceRequest(key(),"真实发票申请并发回归");
        assertThat(orders.markOverdue(order.balanceDueAt().plusSeconds(1))).isEqualTo(1);
        jdbc.update("UPDATE dealers SET dealer_name=? WHERE id=?","发票外键并发回归-"+suffix,dealerId);
        org.springframework.test.context.transaction.TestTransaction.flagForCommit();
        org.springframework.test.context.transaction.TestTransaction.end();
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var dealerLocked=new java.util.concurrent.CountDownLatch(1);
        var releasePayment=new java.util.concurrent.CountDownLatch(1);
        var paymentPid=new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.Future<?> paymentTask=null,invoiceTask=null;
        Throwable primaryFailure=null;
        try {
            paymentTask=pool.submit(()->new TransactionTemplate(transactionManager).executeWithoutResult(tx->{
                actAs(finance);
                try {
                    paymentPid.set(jdbc.queryForObject("SELECT pg_backend_pid()",Integer.class));
                    // 使用付款入口自身的真实共同锁，暂停在 dealer 已锁而 order 未锁的合法交错点。
                    orders.lockDealerForOrder(order.id());
                    dealerLocked.countDown();
                    if(!releasePayment.await(15,java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("付款事务未获放行");
                    orders.verifyPayment(payment.id(),paymentRequest);
                } catch(InterruptedException e) {Thread.currentThread().interrupt();throw new AssertionError(e);}
                finally {SecurityContextHolder.clearContext();}
            }));
            assertThat(dealerLocked.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            String application="dealer-invoice-fk-"+suffix;
            invoiceTask=pool.submit(()->new TransactionTemplate(transactionManager).executeWithoutResult(tx->{
                actAs(finance);
                jdbc.queryForObject("SELECT set_config('application_name',?,true)",String.class,application);
                try {dealerReturns.requestInvoice(order.id(),invoiceRequest);} finally {SecurityContextHolder.clearContext();}
            }));
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            boolean progressed=false;
            while(System.nanoTime()<deadline) {
                if(invoiceTask.isDone() || Boolean.TRUE.equals(jdbc.queryForObject(
                        "SELECT EXISTS(SELECT 1 FROM pg_stat_activity WHERE application_name=? AND ?=ANY(pg_blocking_pids(pid)))",Boolean.class,application,paymentPid.get()))) {
                    progressed=true;break;
                }
                Thread.sleep(10);
            }
            assertThat(progressed).as("真实开票已完成或外键锁正等待付款事务持有的 dealer 锁").isTrue();
            releasePayment.countDown();
            Throwable paymentError=taskFailure(paymentTask),invoiceError=taskFailure(invoiceTask);
            org.assertj.core.api.SoftAssertions.assertSoftly(check->{
                check.assertThat(paymentError).as("真实付款核验应成功，不允许 40P01").isNull();
                check.assertThat(invoiceError).as("真实发票申请应成功，不允许 40P01").isNull();
            });
            assertThat(jdbc.queryForObject("SELECT paid_amount_cent FROM dealer_orders WHERE id=?",Long.class,order.id())).isEqualTo(2_000L);
            assertThat(jdbc.queryForObject("SELECT status FROM dealer_order_payments WHERE id=?",String.class,payment.id())).isEqualTo("verified");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dealer_invoices WHERE order_id=?",Long.class,order.id())).isEqualTo(1L);
            assertThat(jdbc.queryForObject("SELECT credit_suspended FROM dealers WHERE id=?",Boolean.class,dealerId)).isFalse();
        } catch(Exception | AssertionError error) {primaryFailure=error;throw error;}
        finally {
            releasePayment.countDown();
            if(paymentTask!=null) try {paymentTask.get(10,java.util.concurrent.TimeUnit.SECONDS);} catch(Exception ignored) {}
            if(invoiceTask!=null) try {invoiceTask.get(10,java.util.concurrent.TimeUnit.SECONDS);} catch(Exception ignored) {}
            pool.shutdownNow();pool.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS);
            try {
                actAs(finance);
                if("pending_verification".equals(jdbc.queryForObject("SELECT status FROM dealer_order_payments WHERE id=?",String.class,payment.id())))
                    orders.verifyPayment(payment.id(),new DealerPaymentVerifyRequest(key(),"approve","并发测试后核实原尾款，保留真实账本"));
                if(jdbc.queryForObject("SELECT COUNT(*) FROM dealer_invoices WHERE order_id=?",Long.class,order.id())==0L)
                    dealerReturns.requestInvoice(order.id(),new com.complex.platform.dealer.dto.DealerInvoiceRequest(key(),"并发测试后办理未成功的原发票申请"));
            } catch(RuntimeException cleanupError) {
                if(primaryFailure!=null) primaryFailure.addSuppressed(cleanupError);else throw cleanupError;
            }
        }
    }

    private Throwable taskFailure(java.util.concurrent.Future<?> task) throws Exception {
        try {task.get(15,java.util.concurrent.TimeUnit.SECONDS);return null;}
        catch(java.util.concurrent.ExecutionException error) {return error.getCause();}
    }

    @Test
    void concurrentDifferentItemAdjustmentsClearingLastDebtsResumeDealerCredit() throws Exception {
        concurrentDebtClear(false);
    }

    @Test
    void concurrentAdjustmentAndPaymentClearingLastDebtsResumeDealerCredit() throws Exception {
        concurrentDebtClear(true);
    }

    private void concurrentDebtClear(boolean secondIsPayment) throws Exception {
        var first=deliveryOrderWithAnchor("shipped",1,1_000L);
        var firstPhysical=prepareDamagedUnit(first,1_000L,"accepted_as_is");
        String originalItem=itemA;
        itemA="item_do_second_"+suffix;
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,trace_required,min_quantity,status,created_at,updated_at) VALUES (?,'另一商品','beverage','箱',false,2,'active',now(),now())",itemA);
        jdbc.update("INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,updated_at,warehouse_id,project_id) VALUES (?,?,?,100,0,100,now(),?,'project_main')","bal_second_"+suffix,itemA,centralWarehouse,centralWarehouse);
        actAs(sales);creditContract=contractWithTerms("DC-SECOND-"+suffix,true);
        var second=deliveryOrderWithAnchor("shipped",1,1_000L);
        Runnable secondAction;
        if(secondIsPayment) {
            actAs(sales);
            var payment=orders.registerPayment(second.id(),new DealerPaymentRequest(key(),"bank_transfer",1_000L,Instant.now(),"CLEAR-"+suffix));
            var request=new DealerPaymentVerifyRequest(key(),"approve","实际尾款到账");
            secondAction=()->{actAs(finance);orders.verifyPayment(payment.id(),request);};
        } else {
            var secondPhysical=prepareDamagedUnit(second,1_000L,"accepted_as_is");
            secondAction=()->{actAs(warehouse);delivery.completePhysical(secondPhysical.id(),secondPhysical.request());};
        }
        assertThat(orders.markOverdue(second.balanceDueAt().plusSeconds(1))).isEqualTo(2);
        // 真实夹具提交后，两工作线程各自开启独立数据库事务；保留唯一 marker，不删除不可变账本。
        jdbc.update("UPDATE dealers SET dealer_name=? WHERE id=?","并发信用回归-"+suffix,dealerId);
        org.springframework.test.context.transaction.TestTransaction.flagForCommit();
        org.springframework.test.context.transaction.TestTransaction.end();
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        var firstApplied=new java.util.concurrent.CountDownLatch(1);
        var releaseFirst=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Future<?> a=null,b=null;
        Throwable primaryFailure=null;
        try {
            a=pool.submit(()->new TransactionTemplate(transactionManager).executeWithoutResult(tx->{
                actAs(warehouse);
                try {
                    delivery.completePhysical(firstPhysical.id(),firstPhysical.request());
                    firstApplied.countDown();
                    if(!releaseFirst.await(15,java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("第一事务未获提交放行");
                } catch(InterruptedException e) {Thread.currentThread().interrupt();throw new AssertionError(e);}
                finally {SecurityContextHolder.clearContext();}
            }));
            assertThat(firstApplied.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            String application="dealer-clear-"+suffix;
            b=pool.submit(()->new TransactionTemplate(transactionManager).executeWithoutResult(tx->{
                jdbc.queryForObject("SELECT set_config('application_name',?,true)",String.class,application);
                try {secondAction.run();} finally {SecurityContextHolder.clearContext();}
            }));
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            boolean progressed=false;
            while(System.nanoTime()<deadline) {
                // 修复前 B 可以先提交；修复后 B 应真实等共同经销商锁。两种路径均可放行 A，不能用 sleep 猜测交错。
                if(b.isDone() || Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM pg_stat_activity WHERE application_name=? AND wait_event_type='Lock')",Boolean.class,application))) {progressed=true;break;}
                Thread.sleep(10);
            }
            assertThat(progressed).as("第二真实事务已完成或进入数据库行锁等待").isTrue();
            releaseFirst.countDown();a.get(10,java.util.concurrent.TimeUnit.SECONDS);b.get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dealer_orders WHERE dealer_id=? AND overdue",Long.class,dealerId)).isZero();
            assertThat(jdbc.queryForObject("SELECT credit_suspended FROM dealers WHERE id=?",Boolean.class,dealerId))
                    .as("最后两笔逾期债务均已真实结清，共同经销商限制必须解除").isFalse();
        } catch(Exception | AssertionError error) {
            primaryFailure=error;
            throw error;
        } finally {
            releaseFirst.countDown();
            if(a!=null) try {a.get(10,java.util.concurrent.TimeUnit.SECONDS);} catch(Exception ignored) {}
            if(b!=null) try {b.get(10,java.util.concurrent.TimeUnit.SECONDS);} catch(Exception ignored) {}
            pool.shutdownNow();pool.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS);
            try {
                // 仅修复本用例已真实清债后的派生信用标记；订单、付款、应收及审批记录全部保留。
                jdbc.update("UPDATE dealers SET credit_suspended=FALSE WHERE id=? AND NOT EXISTS(SELECT 1 FROM dealer_orders WHERE dealer_id=? AND overdue)",dealerId,dealerId);
            } catch(RuntimeException cleanupError) {
                if(primaryFailure!=null) primaryFailure.addSuppressed(cleanupError); else throw cleanupError;
            }
            itemA=originalItem;
        }
    }

    @Test
    void deliveryAdjustmentClearingLastOverdueDebtResumesCredit() {
        var order=deliveryOrderWithAnchor("shipped",1,1_000L);
        assertThat(orders.markOverdue(order.balanceDueAt().plusSeconds(1))).isEqualTo(1);
        adjustDamagedUnit(order,1_000L);
        assertThat(orders.getOrder(order.id()).overdue()).as("差异减款已清原单逾期余额").isFalse();
        assertThat(jdbc.queryForObject("SELECT credit_suspended FROM dealers WHERE id=?",Boolean.class,dealerId)).isFalse();
        assertThat(jdbc.queryForObject("SELECT status FROM ar_ap_items WHERE id=?",String.class,order.arApItemId())).isEqualTo("settled");
        actAs(sales);
        var next=orders.createOrderDraft(dealerId,new DealerOrderDraftRequest(key(),creditContract,List.of(new DealerOrderLineRequest(itemA,"箱",1,null)),"逾期已清后账期单"));
        assertThat(orders.submitOrder(next.id(),key()).status()).isEqualTo("pending_review");
    }

    @Test
    void deliveryAdjustmentClearsOnlyItsOrderAndKeepsOtherOverdueRestriction() {
        var order=deliveryOrderWithAnchor("shipped",1,1_000L);
        var other=deliveryOrderWithAnchor("shipped",1,1_000L);
        assertThat(orders.markOverdue(other.balanceDueAt().plusSeconds(1))).isEqualTo(2);
        adjustDamagedUnit(order,1_000L);
        assertThat(orders.getOrder(order.id()).overdue()).isFalse();
        assertThat(orders.getOrder(other.id()).overdue()).isTrue();
        assertThat(jdbc.queryForObject("SELECT credit_suspended FROM dealers WHERE id=?",Boolean.class,dealerId)).isTrue();
        assertThat(jdbc.queryForObject("SELECT status FROM ar_ap_items WHERE id=?",String.class,other.arApItemId())).isEqualTo("overdue");
    }

    @Test
    void partialDeliveryAdjustmentKeepsUnpaidDebtOverdueInLedger() {
        var order=deliveryOrderWithAnchor("shipped",1,1_000L);
        assertThat(orders.markOverdue(order.balanceDueAt().plusSeconds(1))).isEqualTo(1);
        adjustDamagedUnit(order,500L);
        assertThat(orders.getOrder(order.id()).overdue()).isTrue();
        assertThat(jdbc.queryForObject("SELECT credit_suspended FROM dealers WHERE id=?",Boolean.class,dealerId)).isTrue();
        assertThat(jdbc.queryForMap("SELECT unpaid_amount_cent,status FROM ar_ap_items WHERE id=?",order.arApItemId()))
                .containsEntry("unpaid_amount_cent",500L).containsEntry("status","overdue");
    }

    private void seedSingleUnitCost() {
        jdbc.update("UPDATE inventory_stock_balances SET quantity=1,available_quantity=1 WHERE id=?","bal_do_"+suffix);
        inventoryRepository.initCostStateIfAbsent(itemA);
        inventoryRepository.updateCostState(new com.complex.platform.inventory.InventoryItemCostStateRecord(itemA,null,1,500,500,500,0,"moving_average",true,Instant.now()));
    }

    private com.complex.platform.inventory.InventoryQuarantineLotRecord returnDamagedUnit(DealerOrderRecord order) {
        completeDamagedUnit(order,0L,"return_quarantine");
        return quarantine.list(centralWarehouse).getFirst();
    }

    private void adjustDamagedUnit(DealerOrderRecord order,long amount) {
        completeDamagedUnit(order,amount,"accepted_as_is");
    }

    private record PreparedPhysical(String id,com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest request) {}
    private void completeDamagedUnit(DealerOrderRecord order,long amount,String route) {
        var prepared=prepareDamagedUnit(order,amount,route);
        delivery.completePhysical(prepared.id(),prepared.request());
    }
    private PreparedPhysical prepareDamagedUnit(DealerOrderRecord order,long amount,String route) {
        actAs(sales);
        var line=orders.listLines(order.id()).items().getFirst();
        String proof=DealerTestDeliveries.proof(admissionFiles,order.id());
        delivery.receive(order.id(),new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest(key(),order.id(),Instant.now().truncatedTo(ChronoUnit.MICROS),"合同联系人",List.of(proof),
                List.of(new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest.Line(line.id(),0,0,1,0)),"实际收到1箱破损品"));
        var difference=delivery.get(order.id()).discrepancies().getFirst();
        var proposed=delivery.propose(difference.id(),resolutionRequest(1,"price_adjustment",route,amount,proof));
        approveDeliveryReview(proposed.reviewId());actAs(warehouse);
        return new PreparedPhysical(proposed.id(),new com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest(key(),Instant.now().truncatedTo(ChronoUnit.MICROS),List.of(proof)));
    }

    private String scrapUnit(String lotId) throws Exception {
        String photo=admissionFiles.uploadAttachment(DealerTestDeliveries.file(),"inventory_quarantine",lotId,"other").id();
        String video=admissionFiles.uploadAttachment(DealerTestDeliveries.video(),"inventory_quarantine",lotId,"other").id();
        var plan=quarantine.propose(lotId,new com.complex.platform.inventory.InventoryQuarantineDispositionRequest(key(),1,"scrap","按批准方案实际报损",List.of(photo,video),refundApprover,
                List.of(new com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance))));
        approveDeliveryReview(plan.reviewId());actAs(warehouse);
        quarantine.complete(plan.id(),new com.complex.platform.inventory.InventoryQuarantineCompleteRequest(key(),Instant.now().truncatedTo(ChronoUnit.MICROS),List.of(photo,video)));
        return plan.id();
    }

    @Test
    void dealerShipmentMustUseUnifiedCostLedger() {
        inventoryRepository.initCostStateIfAbsent(itemA);
        inventoryRepository.updateCostState(new com.complex.platform.inventory.InventoryItemCostStateRecord(itemA,null,100,50_000,500,50_000,0,"moving_average",true,Instant.now()));
        DealerOrderRecord order=deliveryOrder(3,4_000L);
        var cost=inventoryRepository.findCostState(itemA).orElseThrow();
        assertThat(cost.onHandQuantity()).as("经销出库必须与库存成本账同事务减数量").isEqualTo(97);
        assertThat(cost.totalCostCent()).isEqualTo(48_500L);
        assertThat(cost.cumulativeOutCostCent()).isEqualTo(1_500L);
        assertThat(jdbc.queryForObject("SELECT total_cost_cent FROM inventory_stock_movements WHERE business_type='dealer_order' AND business_id=?",Long.class,order.id())).isEqualTo(1_500L);
    }

    @Test
    void isolatedScrapRequiresBothRealPhotoAndVideoBeforeApprovalAndActualCompletion() {
        DealerOrderRecord order=deliveryOrder(1,2_000L);
        actAs(sales);
        var line=orders.listLines(order.id()).items().getFirst();
        String proof=DealerTestDeliveries.proof(admissionFiles,order.id());
        delivery.receive(order.id(),new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest(key(),order.id(),Instant.now().truncatedTo(ChronoUnit.MICROS),"合同联系人",List.of(proof),
                List.of(new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest.Line(line.id(),0,0,1,0)),"1箱实际破损"));
        var difference=delivery.get(order.id()).discrepancies().getFirst();
        var resolution=delivery.propose(difference.id(),resolutionRequest(1,"price_adjustment","return_quarantine",1_000L,proof));
        approveDeliveryReview(resolution.reviewId()); actAs(warehouse);
        delivery.completePhysical(resolution.id(),new com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest(key(),Instant.now().truncatedTo(ChronoUnit.MICROS),List.of(proof)));
        var lot=quarantine.list(centralWarehouse).getFirst();
        String photo=admissionFiles.uploadAttachment(DealerTestDeliveries.file(),"inventory_quarantine",lot.id(),"other").id();
        var onlyPhoto=new com.complex.platform.inventory.InventoryQuarantineDispositionRequest(key(),1,"scrap","确认报损",List.of(photo),refundApprover,
                List.of(new com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance)));
        assertThatThrownBy(()->isolatedFailure(()->quarantine.propose(lot.id(),onlyPhoto))).isInstanceOf(ApiException.class).hasMessageContaining("照片和视频");
        String video=admissionFiles.uploadAttachment(DealerTestDeliveries.video(),"inventory_quarantine",lot.id(),"other").id();
        var approved=quarantine.propose(lot.id(),new com.complex.platform.inventory.InventoryQuarantineDispositionRequest(key(),1,"scrap","确认报损",List.of(photo,video),refundApprover,onlyPhoto.parallelLines()));
        approveDeliveryReview(approved.reviewId()); actAs(warehouse);
        assertThatThrownBy(()->isolatedFailure(()->quarantine.complete(approved.id(),new com.complex.platform.inventory.InventoryQuarantineCompleteRequest(key(),Instant.now().truncatedTo(ChronoUnit.MICROS),List.of(photo)))))
                .isInstanceOf(ApiException.class).hasMessageContaining("照片和视频");
        assertThat(quarantine.list(centralWarehouse).getFirst().remainingQuantity()).isEqualTo(1);
        quarantine.complete(approved.id(),new com.complex.platform.inventory.InventoryQuarantineCompleteRequest(key(),Instant.now().truncatedTo(ChronoUnit.MICROS),List.of(photo,video)));
        assertThat(quarantine.list(centralWarehouse).getFirst().remainingQuantity()).isZero();
    }

    @Test
    void deliveryAndQuarantineRejectedWritesKeepFactsAndPersistFailureAudit() {
        DealerOrderRecord order=deliveryOrder(1,2_000L);
        actAs(sales);
        var line=orders.listLines(order.id()).items().getFirst();
        String proof=DealerTestDeliveries.proof(admissionFiles,order.id());
        var invalidReceipt=new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest(key(),order.id(),Instant.now().truncatedTo(ChronoUnit.MICROS),"合同联系人",List.of(proof),
                List.of(new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest.Line(line.id(),2,0,0,0)),"不得超收");
        assertThatThrownBy(()->isolatedFailure(()->delivery.receive(order.id(),invalidReceipt))).isInstanceOf(ApiException.class);
        assertThat(delivery.get(order.id()).receipts()).isEmpty();
        assertFailureEvent("dealer.delivery.receive_failed",order.id(),invalidReceipt.idempotencyKey());
        delivery.receive(order.id(),new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest(key(),order.id(),Instant.now().truncatedTo(ChronoUnit.MICROS),"合同联系人",List.of(proof),
                List.of(new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest.Line(line.id(),0,0,1,0)),"实际破损"));
        var difference=delivery.get(order.id()).discrepancies().getFirst();
        var invalidPlan=resolutionRequest(2,"price_adjustment","return_quarantine",0L,proof);
        assertThatThrownBy(()->isolatedFailure(()->delivery.propose(difference.id(),invalidPlan))).isInstanceOf(ApiException.class);
        assertThat(delivery.get(order.id()).discrepancies().getFirst().resolutions()).isEmpty();
        assertFailureEvent("dealer.delivery.resolution_request_failed",difference.id(),invalidPlan.idempotencyKey());
        var plan=delivery.propose(difference.id(),resolutionRequest(1,"price_adjustment","return_quarantine",0L,proof));
        actAs(warehouse);
        var physical=new com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest(key(),Instant.now().truncatedTo(ChronoUnit.MICROS),List.of(proof));
        assertThatThrownBy(()->isolatedFailure(()->delivery.completePhysical(plan.id(),physical))).isInstanceOf(ApiException.class);
        assertFailureEvent("dealer.delivery.physical_complete_failed",plan.id(),physical.idempotencyKey());
        var replacement=new com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest(key(),Instant.now().truncatedTo(ChronoUnit.MICROS),List.of(proof));
        assertThatThrownBy(()->isolatedFailure(()->delivery.shipReplacement(plan.id(),replacement))).isInstanceOf(ApiException.class);
        assertFailureEvent("dealer.delivery.replacement_ship_failed",plan.id(),replacement.idempotencyKey());
        assertThat(balance("quantity")).isEqualTo(99);
        assertThat(delivery.get(order.id()).batches()).hasSize(1);
        assertThat(orders.getOrder(order.id()).deliveryCompletedAt()).isNull();
        approveDeliveryReview(plan.reviewId());actAs(warehouse);
        delivery.completePhysical(plan.id(),new com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest(key(),Instant.now().truncatedTo(ChronoUnit.MICROS),List.of(proof)));
        var lot=quarantine.list(centralWarehouse).getFirst();
        String photo=admissionFiles.uploadAttachment(DealerTestDeliveries.file(),"inventory_quarantine",lot.id(),"other").id();
        var invalidDisposition=new com.complex.platform.inventory.InventoryQuarantineDispositionRequest(key(),2,"release_resalable","复检",List.of(photo),refundApprover,
                List.of(new com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance)));
        assertThatThrownBy(()->isolatedFailure(()->quarantine.propose(lot.id(),invalidDisposition))).isInstanceOf(ApiException.class);
        assertThat(quarantine.dispositions(lot.id())).isEmpty();
        assertFailureEvent("inventory.quarantine.disposition_request_failed",lot.id(),invalidDisposition.idempotencyKey());
        var disposition=quarantine.propose(lot.id(),new com.complex.platform.inventory.InventoryQuarantineDispositionRequest(key(),1,"release_resalable","复检",List.of(photo),refundApprover,invalidDisposition.parallelLines()));
        var complete=new com.complex.platform.inventory.InventoryQuarantineCompleteRequest(key(),Instant.now().truncatedTo(ChronoUnit.MICROS),List.of(photo));
        assertThatThrownBy(()->isolatedFailure(()->quarantine.complete(disposition.id(),complete))).isInstanceOf(ApiException.class);
        assertFailureEvent("inventory.quarantine.disposition_complete_failed",disposition.id(),complete.idempotencyKey());
        assertThat(balance("quantity")).isEqualTo(100);
        assertThat(balance("available_quantity")).isEqualTo(99);
        assertThat(balance("quarantined_quantity")).isEqualTo(1);
        assertThat(quarantine.dispositions(lot.id()).getFirst().status()).isEqualTo("pending_approval");
        assertThat(orders.getOrder(order.id()).netReceivableCent()).isEqualTo(2_000L);
        assertThat(orders.listRefunds(order.id()).items()).isEmpty();
    }
    private void assertFailureEvent(String event,String objectId,String key) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name=? AND business_id=? AND idempotency_key=? AND event_result='failure'",Long.class,event,objectId,key)).isEqualTo(1L);
    }

    @Test
    void projectStockViewerCanSelectOnlyAuthorizedQuarantineWarehousesWithoutMerchantScope() throws Exception {
        // 显式项目勾选须对应真实项目；其余既有平台管理员夹具不依赖这个项目候选行。
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES ('project_main','complex','本测试项目','active',now(),now()) ON CONFLICT(id) DO NOTHING");
        DealerOrderRecord order=deliveryOrder(1,2_000L);actAs(sales);
        var line=orders.listLines(order.id()).items().getFirst();String proof=DealerTestDeliveries.proof(admissionFiles,order.id());
        delivery.receive(order.id(),new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest(key(),order.id(),Instant.now().truncatedTo(ChronoUnit.MICROS),"合同联系人",List.of(proof),
                List.of(new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest.Line(line.id(),0,0,1,0)),"实际破损"));
        var plan=delivery.propose(delivery.get(order.id()).discrepancies().getFirst().id(),resolutionRequest(1,"price_adjustment","return_quarantine",0L,proof));
        approveDeliveryReview(plan.reviewId());actAs(warehouse);
        delivery.completePhysical(plan.id(),new com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest(key(),Instant.now().truncatedTo(ChronoUnit.MICROS),List.of(proof)));
        var lot=quarantine.list(centralWarehouse).getFirst();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inventory_quarantine_lots q JOIN warehouses w ON w.id=q.warehouse_id AND w.project_id=q.project_id JOIN inventory_stock_balances b ON b.id=q.stock_balance_id AND b.warehouse_id=w.id AND b.item_id=q.item_id WHERE q.id=?",Long.class,lot.id())).isEqualTo(1L);
        String user="usr_iso_view_"+suffix,role="role_iso_view_"+suffix,other="proj_iso_other_"+suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (?,'complex','其他项目','active',now(),now())",other);
        jdbc.update("INSERT INTO roles(id,role_name,data_scope,status,created_at) VALUES (?,?,'project','active',now())",role,role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES (?,'inventory.stock.view')",role);
        jdbc.update("INSERT INTO role_viewable_projects(role_id,viewable_id) VALUES (?,'project_main')",role);
        access.createUser(new AccessUser(user,user,"staff","project_main",null,"active",Instant.now()));access.assignUserRole(user,role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(user,user,"staff","project_main",null,"active"),"test",List.of()));
        var mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(new com.complex.platform.inventory.InventoryQuarantineController(quarantine)).build();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/inventory/quarantine/warehouses"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data[0].id").value(centralWarehouse));
        assertThat(quarantine.list(centralWarehouse)).extracting(com.complex.platform.inventory.InventoryQuarantineLotRecord::id).containsExactly(lot.id());
        assertThatThrownBy(()->isolatedFailure(()->quarantine.propose(lot.id(),null))).isInstanceOf(ApiException.class);
        assertThat(quarantine.dispositions(lot.id())).isEmpty();assertThat(balance("quarantined_quantity")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='inventory.quarantine.viewed' AND actor_id=? AND business_id=? AND event_result='success'",Long.class,user,centralWarehouse)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='inventory.quarantine.dispositions_viewed' AND actor_id=? AND business_id=? AND event_result='success'",Long.class,user,lot.id())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='dealer.delivery.viewed' AND business_id=? AND event_result='success'",Long.class,order.id())).isPositive();
        assertThatThrownBy(()->delivery.get(order.id())).isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='dealer.delivery.view_failed' AND actor_id=? AND business_id=? AND event_result='failure'",Long.class,user,order.id())).isEqualTo(1L);
        jdbc.update("UPDATE role_viewable_projects SET viewable_id=? WHERE role_id=?",other,role);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/inventory/quarantine/warehouses"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data").isEmpty());
        assertThatThrownBy(()->quarantine.list(centralWarehouse)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->quarantine.dispositions(lot.id())).isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='inventory.quarantine.view_failed' AND actor_id=? AND business_id=? AND event_result='failure'",Long.class,user,centralWarehouse)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='inventory.quarantine.dispositions_view_failed' AND actor_id=? AND business_id=? AND event_result='failure'",Long.class,user,lot.id())).isEqualTo(1L);
        jdbc.update("DELETE FROM role_permissions WHERE role_id=?",role);
        assertThatThrownBy(()->mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/inventory/quarantine/warehouses"))).hasRootCauseInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='inventory.quarantine.warehouses_viewed' AND actor_id=? AND event_result='success'",Long.class,user)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE event_name='inventory.quarantine.warehouses_view_failed' AND actor_id=? AND event_result='failure'",Long.class,user)).isEqualTo(1L);
    }

    @Test
    void quarantineMovementsPreserveRealStockOwner() {
        String merchant=jdbc.queryForObject("SELECT id FROM merchants ORDER BY id LIMIT 1",String.class);
        jdbc.update("UPDATE warehouses SET merchant_id=? WHERE id=?",merchant,centralWarehouse);
        jdbc.update("UPDATE inventory_stock_balances SET merchant_id=? WHERE id=?",merchant,"bal_do_"+suffix);
        DealerOrderRecord order=deliveryOrder(1,2_000L);actAs(sales);
        var line=orders.listLines(order.id()).items().getFirst();String proof=DealerTestDeliveries.proof(admissionFiles,order.id());
        delivery.receive(order.id(),new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest(key(),order.id(),Instant.now().truncatedTo(ChronoUnit.MICROS),"合同联系人",List.of(proof),
                List.of(new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest.Line(line.id(),0,0,1,0)),"实际破损"));
        var plan=delivery.propose(delivery.get(order.id()).discrepancies().getFirst().id(),resolutionRequest(1,"price_adjustment","return_quarantine",0L,proof));
        approveDeliveryReview(plan.reviewId());actAs(warehouse);
        delivery.completePhysical(plan.id(),new com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest(key(),Instant.now().truncatedTo(ChronoUnit.MICROS),List.of(proof)));
        assertThat(jdbc.queryForObject("SELECT merchant_id FROM inventory_stock_movements WHERE id=?",String.class,"movement_"+plan.id())).isEqualTo(merchant);
        var lot=quarantine.list(centralWarehouse).getFirst();
        String photo=admissionFiles.uploadAttachment(DealerTestDeliveries.file(),"inventory_quarantine",lot.id(),"other").id();
        var disposition=quarantine.propose(lot.id(),new com.complex.platform.inventory.InventoryQuarantineDispositionRequest(key(),1,"release_resalable","实际复检通过",List.of(photo),refundApprover,
                List.of(new com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance))));
        approveDeliveryReview(disposition.reviewId());actAs(warehouse);
        quarantine.complete(disposition.id(),new com.complex.platform.inventory.InventoryQuarantineCompleteRequest(key(),Instant.now().truncatedTo(ChronoUnit.MICROS),List.of(photo)));
        assertThat(jdbc.queryForObject("SELECT merchant_id FROM inventory_stock_movements WHERE id=?",String.class,"movement_"+disposition.id())).isEqualTo(merchant);
        assertThat(balance("available_quantity")).isEqualTo(100);
    }

    private void assertIsolationScope(String lotId) {
        String merchant=jdbc.queryForObject("SELECT id FROM merchants ORDER BY id LIMIT 1",String.class);
        String user="usr_isolation_"+suffix, role="role_isolation_"+suffix;
        jdbc.update("INSERT INTO roles(id,role_name,data_scope,status,created_at) VALUES (?,?,'project','active',now())",role,role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code LIKE 'inventory.%' OR permission_code LIKE 'evidence.%'",role);
        access.createUser(new AccessUser(user,user,"staff","project_main",merchant,"active",Instant.now()));
        access.assignUserRole(user,role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(user,user,"staff","project_main",merchant,"active"),"test",List.of()));
        assertThatThrownBy(()->quarantine.list(centralWarehouse)).isInstanceOf(ApiException.class).hasMessageContaining("仓库");
        assertThatThrownBy(()->quarantine.requireEvidenceScope(lotId,false)).isInstanceOf(ApiException.class).hasMessageContaining("仓库");
        jdbc.update("UPDATE warehouses SET merchant_id=? WHERE id=?",merchant,centralWarehouse);
        assertThat(quarantine.list(centralWarehouse)).isEmpty();
        assertThatThrownBy(()->quarantine.dispositions(lotId)).isInstanceOf(ApiException.class).hasMessageContaining("货权");
        jdbc.update("UPDATE inventory_stock_balances SET merchant_id=? WHERE id=?",merchant,"bal_do_"+suffix);
        assertThat(quarantine.list(centralWarehouse)).extracting(com.complex.platform.inventory.InventoryQuarantineLotRecord::id).containsExactly(lotId);
        quarantine.requireEvidenceScope(lotId,false);
        quarantine.requireEvidenceScope(lotId,true);
        jdbc.update("UPDATE warehouses SET merchant_id=NULL WHERE id=?",centralWarehouse);
        jdbc.update("UPDATE inventory_stock_balances SET merchant_id=NULL WHERE id=?","bal_do_"+suffix);
        actAs(warehouse);
    }

    @Test
    void shortageReplacementNeedsActualDispositionAndNewReceiptBeforeD01Starts() {
        inventoryRepository.initCostStateIfAbsent(itemA);
        inventoryRepository.updateCostState(new com.complex.platform.inventory.InventoryItemCostStateRecord(itemA,null,100,50_000,500,50_000,0,"moving_average",true,Instant.now()));
        DealerOrderRecord order = deliveryOrder(3, 2_000L);
        actAs(sales);
        var line = orders.listLines(order.id()).items().getFirst();
        String proof = DealerTestDeliveries.proof(admissionFiles, order.id());
        delivery.receive(order.id(), new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest(key(), order.id(), Instant.now().truncatedTo(ChronoUnit.MICROS), "合同联系人", List.of(proof),
                List.of(new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest.Line(line.id(), 2, 0, 0, 1)), "1箱短少"));
        var difference = delivery.get(order.id()).discrepancies().getFirst();
        var proposed = delivery.propose(difference.id(), resolutionRequest(1, "replacement", "loss_confirmed", 0L, proof));
        approveDeliveryReview(proposed.reviewId());
        actAs(warehouse);
        assertThatThrownBy(() -> isolatedFailure(() -> delivery.shipReplacement(proposed.id(), new com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest(key(), Instant.now().truncatedTo(ChronoUnit.MICROS), List.of(proof)))))
                .isInstanceOf(ApiException.class);
        delivery.completePhysical(proposed.id(), new com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest(key(), Instant.now().truncatedTo(ChronoUnit.MICROS), List.of(proof)));
        assertThat(balance("quantity")).as("损失处理不能虚回库").isEqualTo(97);
        var shipment = new com.complex.platform.dealer.dto.DealerDeliveryPhysicalRequest(key(), Instant.now().truncatedTo(ChronoUnit.MICROS), List.of(proof));
        var batch = delivery.shipReplacement(proposed.id(), shipment);
        delivery.shipReplacement(proposed.id(), shipment);
        assertThat(balance("quantity")).isEqualTo(96);
        assertThat(inventoryRepository.findCostState(itemA).orElseThrow().onHandQuantity()).isEqualTo(96);
        assertThat(inventoryRepository.findCostState(itemA).orElseThrow().cumulativeOutCostCent()).isEqualTo(2_000L);
        assertThat(batch.evidenceAttachmentIds()).containsExactly(proof);
        assertThat(orders.getOrder(order.id()).balanceDueAt()).isNull();
        actAs(sales);
        var receipt = new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest(key(), batch.id(), Instant.now().truncatedTo(ChronoUnit.MICROS), "合同联系人", List.of(proof),
                List.of(new com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest.Line(line.id(), 1, 0, 0, 0)), "补交实际收到");
        DealerOrderRecord signed = delivery.receive(order.id(), receipt);
        assertThat(signed.status()).isEqualTo("signed");
        assertThat(signed.balanceDueAt()).isEqualTo(receipt.confirmedAt().plusSeconds(30L * 86400));
        assertThat(signed.netReceivableCent()).isEqualTo(4_000L);
        assertThat(orders.listRefunds(order.id()).items()).isEmpty();
    }

    private DealerOrderRecord deliveryOrder(int quantity, long... payments) {
        return deliveryOrderWithAnchor("signed",quantity,payments);
    }
    private DealerOrderRecord deliveryOrderWithAnchor(String balanceAnchor,int quantity,long... payments) {
        actAs(sales);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id,line_code,updated_by) VALUES (?,?,'main',?) ON CONFLICT DO NOTHING", sales, refundApprover, sales);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id,line_code,updated_by) VALUES (?,?,'finance_review',?) ON CONFLICT DO NOTHING", sales, finance, sales);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id,line_code,updated_by) VALUES (?,?,'main',?) ON CONFLICT DO NOTHING", warehouse, refundApprover, sales);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id,line_code,updated_by) VALUES (?,?,'finance_review',?) ON CONFLICT DO NOTHING", warehouse, finance, sales);
        access.saveStaffLevelAssignment(new StaffLevelAssignment(finance, "lvl_m3_lower", null, "CNY", null, "project_main", null, sales, Instant.now().truncatedTo(ChronoUnit.MICROS)));
        orders.updateContractTerms(creditContract, new DealerContractTermsRequest(key(), true, 3000, null, 3, 30, 1_000L, 50_000L, "dealer", null, null, Instant.now().truncatedTo(ChronoUnit.MICROS), "payment_notified", null, balanceAnchor, null));
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), creditContract, List.of(new DealerOrderLineRequest(itemA, "箱", quantity, null)), "交付数量闭环"));
        orders.submitOrder(draft.id(), key());
        actAs(reviewer);
        orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "核实合同"));
        for (long amount : payments) {
            actAs(sales);
            var payment = orders.registerPayment(draft.id(), new DealerPaymentRequest(key(), "bank_transfer", amount, Instant.now().truncatedTo(ChronoUnit.MICROS), "FS004-" + key()));
            actAs(finance);
            orders.verifyPayment(payment.id(), new DealerPaymentVerifyRequest(key(), "approve", "实际到账"));
        }
        actAs(warehouse);
        return orders.shipOrder(draft.id(), new DealerShipRequest(key(), "实际出库"));
    }
    private com.complex.platform.dealer.dto.DealerDeliveryResolutionRequest resolutionRequest(int quantity,String type,String route,long amount,String proof) {
        return new com.complex.platform.dealer.dto.DealerDeliveryResolutionRequest(key(),quantity,type,route,amount,"合同销售主体","双方依据锁定合同确认该数量、责任、金额及实物路径",List.of(proof),refundApprover,
                List.of(new com.complex.platform.governance.review.SupervisorReviewEvaluateRequest.ParallelLine("finance_review",finance)));
    }
    private void approveDeliveryReview(String reviewId) {
        actAs(refundApprover);
        for (int count=0;count<12 && "pending_review".equals(reviews.findReview(reviewId).orElseThrow().reviewStatus());count++) {
            var node = reviews.chain(reviewId).stream().filter(com.complex.platform.governance.chain.ApprovalChainNode::pending)
                    .min(java.util.Comparator.comparingInt(com.complex.platform.governance.chain.ApprovalChainNode::seq)).orElseThrow();
            actAs(node.approverId()==null ? refundApprover : node.approverId());
            reviews.approve(reviewId,new SupervisorReviewApprovalRequest(key(),"按既有岗位审批",List.of(),null,"approved","双方证据、数量、金额和责任已核"));
        }
        assertThat(reviews.findReview(reviewId).orElseThrow().reviewStatus()).isEqualTo("approved");
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
        DealerOrderRecord signed = delivery.receive(draft.id(), DealerTestDeliveries.full(draft.id(), orders.listLines(draft.id()).items(), DealerTestDeliveries.proof(admissionFiles, draft.id())));
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
        orders.verifyPayment(late.id(), new DealerPaymentVerifyRequest(key(), "approve", "核实迟到款实际到账"));

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
        assertThatThrownBy(() -> isolatedFailure(() -> orders.executeRefund(first.id(), new DealerRefundExecuteRequest(key()))))
                .as("退款执行人与到账核验人不得为同一人").isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_SOD_SAME_PERSON"));
        actAs(warehouse);
        for (DealerOrderRefundRecord refund : orders.listRefunds(draft.id()).items()) {
            DealerOrderRefundRecord prepared = orders.executeRefund(refund.id(), new DealerRefundExecuteRequest(key()));
            assertThat(prepared.status()).isEqualTo("approved");
            assertThat(orders.getOrder(draft.id()).status()).isEqualTo("pending_refund");
            executeAndConfirmRefund(prepared, "success");
            actAs(warehouse);
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
        DealerOrderOverpaymentRecord confirmed = orders.confirmOverpaymentUse(overs.get(0).id(), new DealerOverpaymentUseRequest(key(), "prepayment", null, "双方确认转预付款", 2_500L, confirmation(overs.get(0).id())));
        assertThat(confirmed.status()).isEqualTo("prepayment");
        assertThatThrownBy(() -> orders.confirmOverpaymentUse(overs.get(0).id(), new DealerOverpaymentUseRequest(key(), "prepayment", null, "再确认", 2_500L, confirmation(overs.get(0).id()))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("OVERPAYMENT_BALANCE_INSUFFICIENT"));
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
        DealerOrderRecord signed = delivery.receive(draft.id(), DealerTestDeliveries.full(draft.id(), orders.listLines(draft.id()).items(), DealerTestDeliveries.proof(admissionFiles, draft.id())));
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

    @Test
    void cancelledPendingPaymentRemainsVerifiableWithoutRestoringOrder() {
        DealerOrderRecord order = approvedOrderForClosure();
        actAs(sales);
        DealerOrderPaymentRecord payment = orders.registerPayment(order.id(),
                new DealerPaymentRequest(key(), "bank_transfer", 500L, Instant.now(), "WAITING-RECEIPT"));
        actAs(finance);
        orders.cancelUnpaidAtDeadline(order.firstPaymentDeadlineAt().plusSeconds(1));
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("pending_refund");
        DealerOrderPaymentRecord verified = orders.verifyPayment(payment.id(),
                new DealerPaymentVerifyRequest(key(), "approve", "取消后核实原收款已到账"));
        assertThat(verified.status()).isEqualTo("verified");
        assertThat(verified.appliedCent()).isZero();
        assertThat(orders.getOrder(order.id()).paidAmountCent()).isZero();
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("pending_refund");
        assertThat(orders.listOverpayments(dealerId).items()).isEmpty();
    }

    @Test
    void approvedRefundCannotExecuteBeforeReceiptVerification() {
        DealerOrderRecord order = approvedOrderForClosure();
        actAs(finance);
        orders.cancelUnpaidAtDeadline(order.firstPaymentDeadlineAt().plusSeconds(1));
        orders.registerPayment(order.id(), new DealerPaymentRequest(key(), "bank_transfer", 500L, Instant.now(), "UNVERIFIED"));
        DealerOrderRefundRecord refund = orders.listRefunds(order.id()).items().getFirst();
        jdbc.update("UPDATE dealer_order_refunds SET status='approved' WHERE id=?", refund.id());
        actAs(warehouse);
        assertThatThrownBy(() -> orders.executeRefund(refund.id(), new DealerRefundExecuteRequest(key())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DEALER_RECEIPT_UNVERIFIED"));
        assertThat(orders.listRefunds(order.id()).items().getFirst().status()).isEqualTo("approved");
    }

    @Test
    void referenceAloneCannotMarkApprovedRefundSuccessful() {
        DealerOrderRecord order = approvedOrderForClosure();
        actAs(sales);
        DealerOrderPaymentRecord payment = orders.registerPayment(order.id(),
                new DealerPaymentRequest(key(), "bank_transfer", 500L, Instant.now(), "REAL-RECEIPT"));
        actAs(finance);
        orders.verifyPayment(payment.id(), new DealerPaymentVerifyRequest(key(), "approve", "真实到账"));
        orders.cancelUnpaidAtDeadline(order.firstPaymentDeadlineAt().plusSeconds(1));
        DealerOrderRefundRecord refund = orders.listRefunds(order.id()).items().getFirst();
        jdbc.update("UPDATE dealer_order_refunds SET status='approved' WHERE id=?", refund.id());
        actAs(warehouse);
        DealerOrderRefundRecord prepared = orders.executeRefund(refund.id(), new DealerRefundExecuteRequest(key()));
        assertThat(prepared.status()).isEqualTo("approved");
        assertThat(prepared.financeRefundId()).isNotBlank();
        assertThat(jdbc.queryForObject("SELECT project_id FROM dealers WHERE id=?", String.class, dealerId)).isNull();
        assertThat(jdbc.queryForObject("SELECT project_id FROM finance_refunds WHERE id=?", String.class, prepared.financeRefundId()))
                .isEqualTo(jdbc.queryForObject("SELECT project_id FROM warehouses WHERE id=?", String.class, centralWarehouse));
        assertThat(jdbc.queryForObject("SELECT status FROM finance_refunds WHERE id=?", String.class, prepared.financeRefundId())).isEqualTo("requested");
        assertThat(orders.listRefunds(order.id()).items().getFirst().status()).isEqualTo("approved");
        assertThat(orders.getOrder(order.id()).status()).isEqualTo("pending_refund");
    }

    @Test
    void overpaymentCanSplitAndPrepaymentCancellationRefundsOriginalCashOnly() {
        DealerOrderRecord source = approvedOrderForClosure();
        actAs(sales);
        DealerOrderPaymentRecord paid = orders.registerPayment(source.id(),
                new DealerPaymentRequest(key(), "bank_transfer", source.totalAmountCent() + 5_000L, Instant.now(), "ORIGINAL-OVERPAY"));
        actAs(finance);
        orders.verifyPayment(paid.id(), new DealerPaymentVerifyRequest(key(), "approve", "原款核实"));
        DealerOrderOverpaymentRecord over = orders.listOverpayments(dealerId).items().getFirst();
        List<String> proof = confirmation(over.id());
        DealerOrderOverpaymentRecord prepaid = orders.confirmOverpaymentUse(over.id(),
                new DealerOverpaymentUseRequest(key(), "prepayment", null, "双方确认留作预付", 5_000L, proof));
        assertThat(prepaid.prepaymentBalanceCent()).isEqualTo(5_000L);
        assertThat(prepaid.unconfirmedAmountCent()).isZero();
        DealerOrderRecord target = approvedOrderForClosure();
        actAs(finance);
        DealerOverpaymentUseRequest apply = new DealerOverpaymentUseRequest(key(), "prepayment_offset", target.id(), "双方确认抵乙订单", 3_000L, proof);
        DealerOrderOverpaymentRecord after = orders.confirmOverpaymentUse(over.id(), apply);
        orders.confirmOverpaymentUse(over.id(), apply);
        assertThat(after.amountCent()).isEqualTo(5_000L);
        assertThat(after.prepaymentBalanceCent()).isEqualTo(2_000L);
        assertThat(after.appliedAmountCent()).isEqualTo(3_000L);
        assertThat(orders.getOrder(target.id()).paidAmountCent()).isEqualTo(3_000L);
        assertThat(orders.listPayments(target.id()).items()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dealer_order_overpayment_allocations WHERE overpayment_id=?", Integer.class, over.id())).isEqualTo(2);
        actAs(sales);
        orders.requestCancel(target.id(), new DealerCancelRequest(key(), "乙订单取消，未出库"));
        actAs(reviewer);
        orders.decideCancel(target.id(), new DealerOrderDecisionRequest(key(), "approve", "同意取消"));
        DealerOrderRefundRecord refund = orders.listRefunds(target.id()).items().getFirst();
        assertThat(refund.paymentId()).isEqualTo(paid.id());
        assertThat(refund.amountCent()).isEqualTo(3_000L);
        assertThat(refund.allocationId()).isNotBlank();
        assertThat(orders.listOverpayments(dealerId).items().getFirst().prepaymentBalanceCent()).isEqualTo(2_000L);
        jdbc.update("UPDATE dealer_order_refunds SET status='approved' WHERE id=?", refund.id());
        actAs(warehouse);
        DealerOrderRefundRecord prepared = orders.executeRefund(refund.id(), new DealerRefundExecuteRequest(key()));
        assertThat(jdbc.queryForMap("SELECT original_payment_ref, refund_channel FROM finance_refunds WHERE id=?", prepared.financeRefundId()))
                .containsEntry("original_payment_ref", "ORIGINAL-OVERPAY").containsEntry("refund_channel", "bank_transfer");
        executeAndConfirmRefund(prepared, "failed");
        actAs(finance);
        assertThat(orders.listOverpayments(dealerId).items().getFirst().prepaymentBalanceCent()).isEqualTo(2_000L);
        assertThat(orders.getOrder(target.id()).status()).isEqualTo("pending_refund");
        actAs(warehouse);
        executeAndConfirmRefund(prepared, "success");
        actAs(finance);
        assertThat(orders.getOrder(target.id()).status()).isEqualTo("cancelled");
        assertThat(orders.listOverpayments(dealerId).items().getFirst().prepaymentBalanceCent()).isEqualTo(2_000L);
        assertThat(orders.listOverpayments(dealerId).items().getFirst().appliedAmountCent()).isEqualTo(3_000L);
    }

    @Test
    void partialOverpaymentAllocationRejectsExcessTargetAndWrongEvidence() {
        DealerOrderRecord source = approvedOrderForClosure();
        actAs(sales);
        DealerOrderPaymentRecord paid = orders.registerPayment(source.id(),
                new DealerPaymentRequest(key(), "bank_transfer", source.totalAmountCent() + 5_000L, Instant.now(), "SPLIT-OVERPAY"));
        actAs(finance);
        orders.verifyPayment(paid.id(), new DealerPaymentVerifyRequest(key(), "approve", "原款核实"));
        DealerOrderOverpaymentRecord over = orders.listOverpayments(dealerId).items().getFirst();
        List<String> proof = confirmation(over.id());
        DealerOrderOverpaymentRecord after = orders.confirmOverpaymentUse(over.id(),
                new DealerOverpaymentUseRequest(key(), "prepayment", null, "其中2000留作预付", 2_000L, proof));
        assertThat(after.unconfirmedAmountCent()).isEqualTo(3_000L);
        assertThat(after.prepaymentBalanceCent()).isEqualTo(2_000L);
        DealerOrderRecord target = approvedOrderForClosure();
        actAs(finance);
        assertThatThrownBy(() -> orders.confirmOverpaymentUse(over.id(),
                new DealerOverpaymentUseRequest(key(), "offset_order", target.id(), "不可超过可分配余额", 3_001L, proof)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("OVERPAYMENT_BALANCE_INSUFFICIENT"));
        assertThatThrownBy(() -> orders.confirmOverpaymentUse(over.id(),
                new DealerOverpaymentUseRequest(key(), "offset_order", target.id(), "缺少双方凭证", 3_000L, List.of())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("OVERPAYMENT_CONFIRMATION_REQUIRED"));
        after = orders.confirmOverpaymentUse(over.id(),
                new DealerOverpaymentUseRequest(key(), "offset_order", target.id(), "其余3000抵乙订单", 3_000L, proof));
        assertThat(after.unconfirmedAmountCent()).isZero();
        assertThat(after.prepaymentBalanceCent()).isEqualTo(2_000L);
        assertThat(orders.getOrder(target.id()).paidAmountCent()).isEqualTo(3_000L);
        assertThatThrownBy(() -> orders.confirmOverpaymentUse(over.id(),
                new DealerOverpaymentUseRequest(key(), "prepayment_offset", target.id(), "不得自动拆分", 2_000L, proof)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("OVERPAYMENT_EXCEEDS_TARGET"));
        assertThat(orders.listOverpayments(dealerId).items().getFirst().prepaymentBalanceCent()).isEqualTo(2_000L);
    }

    @Test
    void globalDealerFundsStillEnforceOriginalAndTargetWarehouseProjects() {
        DealerOrderRecord source = approvedOrderForClosure();
        actAs(sales);
        var payment = orders.registerPayment(source.id(), new DealerPaymentRequest(key(), "bank_transfer", source.totalAmountCent() + 5_000L, Instant.now(), "PROJECT-SOURCE"));
        actAs(finance);
        orders.verifyPayment(payment.id(), new DealerPaymentVerifyRequest(key(), "approve", "真实到账"));
        var over = orders.listOverpayments(dealerId).items().getFirst();
        List<String> proof = confirmation(over.id());
        DealerOrderRecord target = approvedOrderForClosure();
        String otherProject = "project_do_" + suffix;
        String otherWarehouse = "wh_do_other_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (?, 'complex', '异项目', 'active', now(),now())", otherProject);
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status) VALUES (?,?,'main','异项目仓','active')", otherWarehouse, otherProject);
        jdbc.update("UPDATE dealer_orders SET central_warehouse_id=? WHERE id=?", otherWarehouse, target.id());
        String scopedUser = "usr_do_scoped_" + suffix;
        String scopedRole = "role_do_scoped_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,data_scope,status,created_at) VALUES (?,?,'project','active',now())", scopedRole, scopedRole);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code LIKE 'dealer.%' OR permission_code LIKE 'evidence.%' OR permission_code LIKE 'finance.%'", scopedRole);
        access.createUser(new AccessUser(scopedUser, scopedUser, "staff", "project_main", null, "active", Instant.now()));
        access.assignUserRole(scopedUser, scopedRole);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(scopedUser, scopedUser, "staff", "project_main", null, "active"), "test", List.of()));
        assertThatThrownBy(() -> orders.confirmOverpaymentUse(over.id(), new DealerOverpaymentUseRequest(key(), "offset_order", target.id(), "不得跨目标项目", 1_000L, proof)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThat(orders.listOrders(dealerId).items()).extracting(DealerOrderRecord::id).contains(source.id()).doesNotContain(target.id());
        assertThat(orders.listOverpayments(dealerId).items()).extracting(DealerOrderOverpaymentRecord::id).contains(over.id());
        assertThatThrownBy(() -> orders.getOrder(target.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThatThrownBy(() -> orders.listLines(target.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThatThrownBy(() -> orders.listPayments(target.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThatThrownBy(() -> orders.listRefunds(target.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThatThrownBy(() -> orders.registerPayment(target.id(), new DealerPaymentRequest(key(), "bank_transfer", 100L, Instant.now(), "DENIED")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dealer_order_payments WHERE order_id=?", Integer.class, target.id())).isZero();
        assertThatThrownBy(() -> orders.requestCancel(target.id(), new DealerCancelRequest(key(), "不得越权取消目标")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        jdbc.update("UPDATE dealer_orders SET central_warehouse_id=? WHERE id=?", otherWarehouse, source.id());
        assertThat(orders.listOrders(dealerId).items()).isEmpty();
        assertThat(orders.listOverpayments(dealerId).items()).isEmpty();
        assertThatThrownBy(() -> confirmation(over.id()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThatThrownBy(() -> orders.confirmOverpaymentUse(over.id(), new DealerOverpaymentUseRequest(key(), "prepayment", null, "不得跨来源项目", 1_000L, proof)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dealer_order_overpayment_allocations WHERE overpayment_id=?", Integer.class, over.id())).isZero();
        actAs(finance);
        assertThat(orders.listOverpayments(dealerId).items().getFirst().unconfirmedAmountCent()).isEqualTo(5_000L);
    }

    @Test
    void confirmedOverpaymentWithoutAllocationFactsCannotBeSpentAgain() {
        DealerOrderRecord source = approvedOrderForClosure();
        actAs(sales);
        var payment = orders.registerPayment(source.id(), new DealerPaymentRequest(key(), "bank_transfer", source.totalAmountCent() + 5_000L, Instant.now(), "INCOMPLETE-ALLOCATION"));
        actAs(finance);
        orders.verifyPayment(payment.id(), new DealerPaymentVerifyRequest(key(), "approve", "真实到账"));
        var over = orders.listOverpayments(dealerId).items().getFirst();
        List<String> proof = confirmation(over.id());
        jdbc.update("UPDATE dealer_order_overpayments SET status='prepayment', confirmed_by=?, confirmed_at=now() WHERE id=?", finance, over.id());
        assertThatThrownBy(() -> orders.confirmOverpaymentUse(over.id(), new DealerOverpaymentUseRequest(key(), "prepayment", null, "缺失原分配事实不得重新分配", 3_000L, proof)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("OVERPAYMENT_ALLOCATION_INTEGRITY_INVALID"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM dealer_order_overpayment_allocations WHERE overpayment_id=?", Integer.class, over.id())).isZero();
    }

    @Test
    void projectStaffCannotVerifyAnotherProjectCancelledReceipt() {
        DealerOrderRecord order = pendingRefundInAnotherProject();
        DealerOrderPaymentRecord late = orders.listPayments(order.id()).items().getFirst();
        assertThat(late.status()).isEqualTo("late");
        actAsProjectScopedFinance();
        assertThatThrownBy(() -> orders.verifyPayment(late.id(), new DealerPaymentVerifyRequest(key(), "approve", "不得跨项目核实")))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT status FROM dealer_order_payments WHERE id=?", String.class, late.id())).isEqualTo("late");
    }

    @Test
    void projectStaffCannotReopenAnotherProjectRefundApproval() {
        DealerOrderRecord order = pendingRefundInAnotherProject();
        DealerOrderRefundRecord refund = orders.listRefunds(order.id()).items().getFirst();
        actAs(refundApprover);
        reviews.approve(refund.reviewId(), new SupervisorReviewApprovalRequest(key(), "原审批驳回", List.of(), null, "rejected", "重新核实"));
        assertThat(orders.listRefunds(order.id()).items().getFirst().status()).isEqualTo("rejected");
        actAsProjectScopedFinance();
        assertThatThrownBy(() -> orders.openRefundApproval(refund.id(), new com.complex.platform.dealer.dto.DealerRefundApprovalRequest(key(), null)))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT status FROM dealer_order_refunds WHERE id=?", String.class, refund.id())).isEqualTo("rejected");
    }

    private DealerOrderRecord pendingRefundInAnotherProject() {
        String project = "project_scope_" + suffix;
        centralWarehouse = "wh_scope_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (?, 'complex', '异项目', 'active', now(),now())", project);
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status) VALUES (?,?,'main','异项目仓','active')", centralWarehouse, project);
        jdbc.update("INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,updated_at,warehouse_id,project_id) VALUES (?,?,?,100,0,100,now(),?,?)",
                "bal_scope_" + suffix, itemA, centralWarehouse, centralWarehouse, project);
        actAs(sales);
        creditContract = contractWithTerms("DC-SCOPE-" + suffix, true);
        DealerOrderRecord order = approvedOrderForClosure();
        actAs(sales);
        orders.registerPayment(order.id(), new DealerPaymentRequest(key(), "bank_transfer", 500L, Instant.now(), "SCOPE-LATE"));
        actAs(finance);
        orders.cancelUnpaidAtDeadline(order.firstPaymentDeadlineAt().plusSeconds(1));
        return order;
    }

    private void actAsProjectScopedFinance() {
        String scopedUser = "usr_scope_" + suffix;
        String scopedRole = "role_scope_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,data_scope,status,created_at) VALUES (?,?,'project','active',now())", scopedRole, scopedRole);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code LIKE 'dealer.%' OR permission_code LIKE 'evidence.%' OR permission_code LIKE 'finance.%'", scopedRole);
        access.createUser(new AccessUser(scopedUser, scopedUser, "staff", "project_main", null, "active", Instant.now()));
        access.assignUserRole(scopedUser, scopedRole);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id,line_code,updated_by) VALUES (?,?,'main',?)", scopedUser, refundApprover, sales);
        access.saveStaffLevelAssignment(new StaffLevelAssignment(scopedUser, "lvl_m3_lower", null, "CNY", null, "project_main", null, sales, Instant.now()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(scopedUser, scopedUser, "staff", "project_main", null, "active"), "test", List.of()));
    }

    @Test
    void projectFinanceWithoutMerchantCanExecuteAndIndependentlyConfirmDealerRefund() {
        DealerOrderRefundRecord approved = approvedRefundForProjectFinance();
        String refundProject = jdbc.queryForObject("SELECT project_id FROM warehouses WHERE id=?", String.class, centralWarehouse);
        String executor = projectFinanceStaff("executor", refundProject, refundProject);
        String confirmer = projectFinanceStaff("confirmer", refundProject, refundProject);
        String foreignProject = "project_rf_foreign_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (?, 'complex', '异项目退款', 'active', now(),now())", foreignProject);
        String foreign = projectFinanceStaff("foreign", foreignProject, foreignProject);
        actAsProjectFinance(executor);
        assertThat(access.findUserById(executor).orElseThrow().merchantId()).isNull();
        DealerOrderRefundRecord prepared = orders.executeRefund(approved.id(), new DealerRefundExecuteRequest(key()));
        assertThat(financeService.getFinanceRefund(prepared.financeRefundId()).refund().status()).isEqualTo("requested");
        String executionEvidence = refundEvidence(prepared.financeRefundId());
        actAsProjectFinance(foreign);
        assertThatThrownBy(() -> isolatedFailure(() -> financeService.recordFinanceRefundExecution(prepared.financeRefundId(),
                new com.complex.platform.finance.dto.RefundExecutionRequest(key(), "异项目不得执行", List.of(executionEvidence), "PROJECT-REFUND", null))))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        actAsProjectFinance(executor);
        var processing = financeService.recordFinanceRefundExecution(prepared.financeRefundId(),
                new com.complex.platform.finance.dto.RefundExecutionRequest(key(), "项目财务按原路执行", List.of(executionEvidence), "PROJECT-REFUND", null));
        assertThat(processing.status()).isEqualTo("processing");
        actAsProjectFinance(foreign);
        assertThatThrownBy(() -> isolatedFailure(() -> financeService.confirmFinanceRefundResult(prepared.financeRefundId(),
                new com.complex.platform.finance.dto.RefundResultConfirmRequest(key(), "异项目不得确认", List.of(executionEvidence),
                        "success", prepared.amountCent(), "PROJECT-REFUND", executor, null))))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        actAsProjectFinance(confirmer);
        assertThat(access.findUserById(confirmer).orElseThrow().merchantId()).isNull();
        var success = financeService.confirmFinanceRefundResult(prepared.financeRefundId(),
                new com.complex.platform.finance.dto.RefundResultConfirmRequest(key(), "另一名项目财务独立确认", List.of(refundEvidence(prepared.financeRefundId())),
                        "success", prepared.amountCent(), "PROJECT-REFUND", executor, null));
        assertThat(success.status()).isEqualTo("success");
        assertThat(orders.listRefunds(prepared.orderId()).items().getFirst().status()).isEqualTo("refunded");
        assertThat(orders.getOrder(prepared.orderId()).status()).isEqualTo("cancelled");
    }

    @Test
    void refundConfirmationCannotUsePaymentRegisterPermissionFromAnotherProject() {
        DealerOrderRefundRecord approved = approvedRefundForProjectFinance();
        String refundProject = jdbc.queryForObject("SELECT project_id FROM warehouses WHERE id=?", String.class, centralWarehouse);
        actAs(warehouse);
        DealerOrderRefundRecord prepared = orders.executeRefund(approved.id(), new DealerRefundExecuteRequest(key()));
        financeService.recordFinanceRefundExecution(prepared.financeRefundId(),
                new com.complex.platform.finance.dto.RefundExecutionRequest(key(), "原经办人执行", List.of(refundEvidence(prepared.financeRefundId())), "MIXED-SCOPE", null));
        String paymentProject = "project_rf_register_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (?, 'complex', '仅收付款确认授权项目', 'active', now(),now())", paymentProject);
        String confirmer = projectFinanceStaff("mixed", refundProject, paymentProject);
        actAsProjectFinance(confirmer);
        assertThat(financeService.getFinanceRefund(prepared.financeRefundId()).refund().status()).isEqualTo("processing");
        String confirmationEvidence = refundEvidence(prepared.financeRefundId());
        assertThatThrownBy(() -> isolatedFailure(() -> financeService.confirmFinanceRefundResult(prepared.financeRefundId(),
                new com.complex.platform.finance.dto.RefundResultConfirmRequest(key(), "A项目收付款确认权不能确认B", List.of(confirmationEvidence),
                        "success", prepared.amountCent(), "MIXED-SCOPE", warehouse, null))))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT status FROM finance_refunds WHERE id=?", String.class, prepared.financeRefundId())).isEqualTo("processing");
        assertThat(jdbc.queryForObject("SELECT status FROM dealer_order_refunds WHERE id=?", String.class, prepared.id())).isEqualTo("processing");
    }

    private DealerOrderRefundRecord approvedRefundForProjectFinance() {
        DealerOrderRecord order = pendingRefundInAnotherProject();
        actAs(finance);
        DealerOrderPaymentRecord payment = orders.listPayments(order.id()).items().getFirst();
        orders.verifyPayment(payment.id(), new DealerPaymentVerifyRequest(key(), "approve", "核实取消前实际到账"));
        DealerOrderRefundRecord refund = orders.listRefunds(order.id()).items().getFirst();
        actAs(refundApprover);
        for (int i = 0; i < 4 && "pending_review".equals(reviews.findReview(refund.reviewId()).orElseThrow().reviewStatus()); i++) {
            reviews.approve(refund.reviewId(), new SupervisorReviewApprovalRequest(key(), "经销原退款审批", List.of(), null, "approved", "同意原路退回"));
        }
        DealerOrderRefundRecord approved = orders.listRefunds(order.id()).items().getFirst();
        assertThat(approved.status()).isEqualTo("approved");
        return approved;
    }

    private String projectFinanceStaff(String tag, String operationProject, String confirmProject) {
        String user = "usr_rf_" + tag + "_" + suffix;
        String operationRole = "role_rf_op_" + tag + "_" + suffix;
        String confirmationRole = "role_rf_pay_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,data_scope,status,created_at) VALUES (?,?,'project','active',now()), (?,?,'project','active',now())",
                operationRole, operationRole, confirmationRole, confirmationRole);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code LIKE 'dealer.%' OR permission_code LIKE 'evidence.%' OR permission_code IN ('finance.ar_ap.view','finance.refund.create')", operationRole);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES (?,'finance.payment.register')", confirmationRole);
        jdbc.update("INSERT INTO role_viewable_projects(role_id,viewable_id) VALUES (?,?),(?,?)", operationRole, operationProject, confirmationRole, confirmProject);
        access.createUser(new AccessUser(user, user, "staff", operationProject, null, "active", Instant.now()));
        access.assignUserRole(user, operationRole);
        access.assignUserRole(user, confirmationRole);
        return user;
    }

    private void actAsProjectFinance(String user) {
        AccessUser account = access.findUserById(user).orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(user, user, "staff", account.organizationId(), null, "active"), "test", List.of()));
    }

    private List<String> confirmation(String overpaymentId) {
        return List.of(admissionFiles.uploadAttachment(new org.springframework.mock.web.MockMultipartFile(
                "file", "双方资金用途确认.png", "image/png", java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jz1sAAAAASUVORK5CYII=")),
                "dealer_overpayment", overpaymentId, "other").id());
    }

    private String refundEvidence(String refundId) {
        return admissionFiles.uploadAttachment(new org.springframework.mock.web.MockMultipartFile(
                "file", "原路退款凭证.png", "image/png", java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jz1sAAAAASUVORK5CYII=")),
                "finance_refund", refundId, "payment_receipt").id();
    }

    // 预期失败请求独立回滚到保存点，避免测试外层事务的 rollback-only 污染下一次真实准入求值。
    private void isolatedFailure(Runnable operation) {
        TransactionTemplate nested = new TransactionTemplate(transactionManager);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        nested.executeWithoutResult(status -> operation.run());
    }

    private void executeAndConfirmRefund(DealerOrderRefundRecord prepared, String status) {
        actAs(warehouse);
        String reference = "RF-" + key();
        var execution = financeService.recordFinanceRefundExecution(prepared.financeRefundId(),
                new com.complex.platform.finance.dto.RefundExecutionRequest(key(), "沿原收款路径执行", List.of(refundEvidence(prepared.financeRefundId())), reference, null));
        assertThat(execution.status()).isEqualTo("processing");
        assertThat(orders.listRefunds(prepared.orderId()).items().stream().filter(item -> item.id().equals(prepared.id())).findFirst().orElseThrow().status()).isEqualTo("processing");
        assertThatThrownBy(() -> isolatedFailure(() -> financeService.recordFinanceRefundExecution(prepared.financeRefundId(),
                new com.complex.platform.finance.dto.RefundExecutionRequest(key(), "结果未知不得重退", List.of(refundEvidence(prepared.financeRefundId())), reference, null))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("REFUND_NOT_RETRYABLE"));
        actAs(refundApprover);
        financeService.confirmFinanceRefundResult(prepared.financeRefundId(),
                new com.complex.platform.finance.dto.RefundResultConfirmRequest(key(), "独立核对银行结果", List.of(refundEvidence(prepared.financeRefundId())),
                        status, prepared.amountCent(), reference, warehouse, null));
    }

    @Test
    void specifiedPartialOverpaymentMustNotConsumeWholeReceipt() throws Exception {
        DealerOrderRecord source = approvedOrderForClosure();
        actAs(sales);
        DealerOrderPaymentRecord payment = orders.registerPayment(source.id(),
                new DealerPaymentRequest(key(), "bank_transfer", source.totalAmountCent() + 5_000L, Instant.now(), "PARTIAL-RED"));
        actAs(finance);
        orders.verifyPayment(payment.id(), new DealerPaymentVerifyRequest(key(), "approve", "真实到账"));
        DealerOrderOverpaymentRecord over = orders.listOverpayments(dealerId).items().getFirst();
        String attachment = admissionFiles.uploadAttachment(new org.springframework.mock.web.MockMultipartFile(
                "file", "资金用途.png", "image/png", java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jz1sAAAAASUVORK5CYII=")),
                "dealer_overpayment", over.id(), "other").id();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper().disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        DealerOverpaymentUseRequest request = mapper.readValue(mapper.writeValueAsString(Map.of(
                "idempotencyKey", key(), "usage", "prepayment", "note", "只将2000分转为预付款",
                "amountCent", 2_000L, "confirmationAttachmentIds", List.of(attachment))), DealerOverpaymentUseRequest.class);
        assertThat(orders.confirmOverpaymentUse(over.id(), request).status()).isEqualTo("partially_allocated");
    }

    private DealerOrderRecord approvedOrderForClosure() {
        actAs(sales);
        DealerOrderRecord draft = orders.createOrderDraft(dealerId, new DealerOrderDraftRequest(key(), creditContract,
                List.of(new DealerOrderLineRequest(itemA, "箱", 3, null)), "资金闭环"));
        orders.submitOrder(draft.id(), key());
        actAs(reviewer);
        return orders.reviewOrder(draft.id(), new DealerOrderDecisionRequest(key(), "approve", "核对合同"));
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
