package com.complex.platform.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.inventory.dto.CreateStocktakeRequest;
import com.complex.platform.inventory.dto.InventoryItemRequest;
import com.complex.platform.inventory.dto.InventorySupplierRequest;
import com.complex.platform.inventory.dto.RecordVarianceRequest;
import com.complex.platform.inventory.dto.ReviewVarianceRequest;
import com.complex.platform.inventory.dto.InventoryRiskScanRequest;
import com.complex.platform.inventory.dto.InventoryStockBatchRequest;
import com.complex.platform.inventory.dto.PurchaseRequestCreate;
import com.complex.platform.inventory.dto.SavedBottleCancelRequest;
import com.complex.platform.inventory.dto.SavedBottleClaimRequest;
import com.complex.platform.inventory.dto.SavedBottleRequest;
import com.complex.platform.inventory.dto.SavedBottleTransferRequest;
import com.complex.platform.inventory.dto.StockAdjustmentRequest;
import com.complex.platform.inventory.dto.StockInRequest;
import com.complex.platform.inventory.dto.StockMovementRequest;
import com.complex.platform.inventory.dto.SupplierReconciliationRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class InventoryServiceTests {

    private InMemoryInventoryRepository inventoryRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private AuthorizationService authorizationService;
    private com.complex.platform.terminal.TerminalDeviceRepository terminalDeviceRepository;
    private InMemoryTraceabilityRepository traceabilityRepository;
    private com.complex.platform.evidence.RiskAlertService riskAlertService;
    private InventoryService service;

    private static final AccessActor ADMIN_ACTOR =
            new AccessActor("usr_admin", "admin", "platform_admin", "org_platform", null, "active");
    private static final AccessActor MERCHANT_A_ACTOR =
            new AccessActor("usr_staff_a", "staff_a", "staff", "org_a", "m_a", "active");
    private static final AccessActor MERCHANT_B_ACTOR =
            new AccessActor("usr_staff_b", "staff_b", "staff", "org_b", "m_b", "active");

    @BeforeEach
    void setUp() {
        inventoryRepository = new InMemoryInventoryRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        authorizationService = mock(AuthorizationService.class);
        // 默认：平台管理员视角——看全部，写入时 merchantId = null
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        when(authorizationService.currentActor()).thenReturn(ADMIN_ACTOR);
        terminalDeviceRepository = mock(com.complex.platform.terminal.TerminalDeviceRepository.class);
        // 默认：无显式开关行 → opt-out 语义放行（机器人段被动留痕允许）。
        when(terminalDeviceRepository.resolveEffectiveSwitch(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyMap())).thenReturn(Optional.empty());
        com.complex.platform.gate.TerminalSwitchGuard terminalSwitchGuard =
                new com.complex.platform.gate.TerminalSwitchGuard(
                        terminalDeviceRepository, new AuditEventService(auditEventRepository));
        traceabilityRepository = new InMemoryTraceabilityRepository();
        riskAlertService = mock(com.complex.platform.evidence.RiskAlertService.class);
        service = new InventoryService(
                inventoryRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                authorizationService,
                terminalSwitchGuard,
                new com.complex.platform.evidence.TraceabilityService(traceabilityRepository, authorizationService),
                riskAlertService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void stockInOutboundAndTransferUpdateIntegerBalancesAndAuditEvents() {
        InventoryItemRecord item = service.createInventoryItem(new InventoryItemRequest(
                "item-create",
                "create",
                List.of(),
                null,
                null,
                "Vodka",
                "spirit",
                "bottle",
                1,
                "active",
                null
        ));

        service.createStockIn(new StockInRequest(
                "stock-in",
                "purchase",
                List.of(),
                "purchase",
                "purchase_1",
                "warehouse_1",
                List.of(Map.of("item_id", item.id(), "quantity", 10))
        ));
        service.createStockMovement(new StockMovementRequest(
                "outbound",
                "move to bar",
                List.of(),
                "warehouse_1",
                "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 4)),
                "TRACE-1"
        ));
        service.transferStockMovement(new StockMovementRequest(
                "transfer",
                "transfer",
                List.of(),
                "warehouse_1",
                "bar_2",
                List.of(Map.of("item_id", item.id(), "quantity", 3)),
                null
        ));

        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1"))
                .get()
                .extracting(StockBalanceRecord::quantity, StockBalanceRecord::availableQuantity)
                .containsExactly(3, 3);
        assertThat(inventoryRepository.findStockBalance(item.id(), "bar_1")).isEmpty();
        assertThat(inventoryRepository.findStockBalance(item.id(), "bar_2"))
                .get()
                .extracting(StockBalanceRecord::quantity)
                .isEqualTo(3);
        assertThat(inventoryRepository.stockMovements)
                .extracting(StockMovementRecord::quantity)
                .containsExactly(10, 4, 3);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "inventory.item.created",
                        "inventory.inbound_confirmed",
                        "inventory.outbound_verified",
                        "inventory.stock_transfer.created"
                );
    }

    @Test
    void stockMovementAndSavedBottleClaimRejectInsufficientQuantity() {
        InventoryItemRecord item = createItem("conflict-item");
        service.createStockIn(new StockInRequest(
                "stock-in-conflict",
                null,
                List.of(),
                "initial",
                null,
                "warehouse_1",
                List.of(Map.of("item_id", item.id(), "quantity", 2))
        ));

        assertThatThrownBy(() -> service.createStockMovement(new StockMovementRequest(
                "outbound-too-much",
                null,
                List.of(),
                "warehouse_1",
                "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 3)),
                null
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("STOCK_INSUFFICIENT");
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1"))
                .get()
                .extracting(StockBalanceRecord::quantity)
                .isEqualTo(2);

        SavedBottleRecord savedBottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-create",
                null,
                List.of(),
                "customer_1",
                "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Vodka", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "123456", "客人甲", null, null, null,
                List.of("reg_photo"), null
        ));

        assertThatThrownBy(() -> service.claimSavedBottle(savedBottle.id(), new SavedBottleClaimRequest(
                "claim-too-much",
                null,
                List.of(),
                List.of(Map.of("quantity", 2)),
                "13800138000",
                "123456",
                "123456", "客人甲"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SAVED_BOTTLE_INSUFFICIENT");
    }

    @Test
    void savedBottleClaimIsIdempotentAndMarksClaimedAtZero() {
        InventoryItemRecord item = createItem("claim-item");
        SavedBottleRecord savedBottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-idempotent",
                null,
                List.of(),
                "customer_1",
                "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 2, "bottle_name", "Vodka", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "123456", "客人甲", null, null, null,
                List.of("reg_photo"), null
        ));
        SavedBottleClaimRequest firstClaim = new SavedBottleClaimRequest(
                "claim-once",
                null,
                List.of(),
                List.of(Map.of("quantity", 1)),
                "13800138000",
                "123456",
                "123456", "客人甲"
        );

        SavedBottleClaimRecord first = service.claimSavedBottle(savedBottle.id(), firstClaim);
        SavedBottleClaimRecord second = service.claimSavedBottle(savedBottle.id(), firstClaim);
        SavedBottleClaimRecord finalClaim = service.claimSavedBottle(savedBottle.id(), new SavedBottleClaimRequest(
                "claim-final",
                null,
                List.of(),
                List.of(Map.of("quantity", 1)),
                "13800138000",
                "123456",
                "123456", "客人甲"
        ));

        assertThat(second).isEqualTo(first);
        assertThat(finalClaim.status()).isEqualTo("picked_up");
        assertThat(inventoryRepository.findSavedBottleById(savedBottle.id()))
                .get()
                .extracting(SavedBottleRecord::remainingQuantity, SavedBottleRecord::status)
                .containsExactly(0, "claimed");
        assertThat(inventoryRepository.savedBottleClaims).hasSize(2);
    }

    @Test
    void supplierPurchaseRequestAndTraceHandoverWriteExpectedAuditEvents() {
        InventoryItemRecord item = createItem("purchase-item");
        InventorySupplierRecord supplier = service.createInventorySupplier(new InventorySupplierRequest(
                "supplier-create",
                null,
                List.of("attachment_1"),
                "Supplier A",
                "Alice",
                "13800138000",
                "active"
        ));
        PurchaseRequestRecord purchaseRequest = service.createPurchaseRequest(new PurchaseRequestCreate(
                "purchase-create",
                null,
                List.of("attachment_2"),
                supplier.id(),
                List.of(Map.of("item_id", item.id(), "quantity", 5)),
                LocalDate.parse("2026-06-20"),
                null
        ));
        TraceHandoverRecord handover = service.scanTraceCodeHandover(new VerificationRequest(
                "trace-create",
                "manual",
                "TRACE-1",
                "device_1",
                "bar_1",
                Map.of("item_id", item.id(), "from_staff_id", "staff_a", "to_staff_id", "staff_b")
        ));

        assertThat(purchaseRequest.supplierId()).isEqualTo(supplier.id());
        assertThat(handover.traceCode()).isEqualTo("TRACE-1");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "inventory.supplier_changed",
                        "inventory.purchase_request_changed",
                        "operation_handover.created"
                );
        // P1#18 三层留痕接线：建交接链即建过程实例（process_type=inventory_delivery），消除孤岛。
        assertThat(traceabilityRepository.instances).containsKey(handover.id());
        assertThat(traceabilityRepository.instances.get(handover.id()).processType())
                .isEqualTo("inventory_delivery");
    }

    @Test
    void traceHandoverFullChainWritesThreeLayerProcessStepsAndCompletes() {
        InventoryItemRecord item = createItem("trace-chain-item");
        TraceHandoverChainView created = service.createTraceHandover(
                new com.complex.platform.inventory.dto.TraceHandoverCreateRequest(
                        "chain-create", "TRACE-CHAIN", item.id(), "device_chain", List.of()));
        String handoverId = created.handover().id();

        // 五段：out_verify(员工) → dispatch(机器人) → kitchen_receive → staff_pickup → deliver_customer。
        service.scanTraceSegment(handoverId, new com.complex.platform.inventory.dto.TraceScanSegmentRequest(
                "seg-out", "out_verify"));
        service.reportRobotSegment(handoverId, new com.complex.platform.inventory.dto.TraceRobotReportRequest(
                "seg-dispatch", "robot_task_1", "delivered", false));
        service.scanTraceSegment(handoverId, new com.complex.platform.inventory.dto.TraceScanSegmentRequest(
                "seg-kitchen", "kitchen_receive"));
        service.scanTraceSegment(handoverId, new com.complex.platform.inventory.dto.TraceScanSegmentRequest(
                "seg-pickup", "staff_pickup"));
        TraceHandoverChainView done = service.scanTraceSegment(
                handoverId, new com.complex.platform.inventory.dto.TraceScanSegmentRequest(
                        "seg-deliver", "deliver_customer"));

        assertThat(done.handover().overallStatus()).isEqualTo("completed");
        // 三层留痕：过程实例收口 completed + 五段过程步骤入库（含机器人段 robot_callback）。
        assertThat(traceabilityRepository.instances.get(handoverId).status()).isEqualTo("completed");
        assertThat(traceabilityRepository.listStepsByInstance(handoverId))
                .extracting(com.complex.platform.evidence.TraceabilityStepRecord::stepCode)
                .containsExactlyInAnyOrder(
                        "out_verify", "dispatch", "kitchen_receive", "staff_pickup", "deliver_customer");
        assertThat(traceabilityRepository.listStepsByInstance(handoverId))
                .filteredOn(s -> "dispatch".equals(s.stepCode()))
                .singleElement()
                .satisfies(s -> {
                    assertThat(s.actorType()).isEqualTo("robot");
                    assertThat(s.captureSource()).isEqualTo("robot_callback");
                });
    }

    @Test
    void riskScanGeneratesSafetyStockOutOfStockAndExpiryAlertsIdempotently() {
        InventoryItemRecord lowStockItem = service.createInventoryItem(new InventoryItemRequest(
                "risk-low-item",
                null,
                List.of(),
                null,
                null,
                "Low Stock Vodka",
                "spirit",
                "bottle",
                5,
                "active",
                null
        ));
        InventoryItemRecord outOfStockItem = service.createInventoryItem(new InventoryItemRequest(
                "risk-out-item",
                null,
                List.of(),
                null,
                null,
                "Out Stock Gin",
                "spirit",
                "bottle",
                2,
                "active",
                null
        ));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "balance_low",
                lowStockItem.id(),
                "warehouse_1",
                2,
                0,
                2,
                Instant.parse("2026-06-14T00:00:00Z"),
                null,
                null
        ));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "balance_out",
                outOfStockItem.id(),
                "warehouse_1",
                0,
                0,
                0,
                Instant.parse("2026-06-14T00:00:00Z"),
                null,
                null
        ));
        service.createStockBatch(new InventoryStockBatchRequest(
                "batch-expiring",
                "record batch",
                List.of("att_batch"),
                lowStockItem.id(),
                "warehouse_1",
                "BATCH-EXP-001",
                "supplier_1",
                2,
                Instant.now().plusSeconds(3 * 86400L),
                "active"
        ));
        InventoryRiskScanRequest request = new InventoryRiskScanRequest(
                "risk-scan",
                "daily scan",
                7,
                "staff_ops"
        );

        InventoryRiskScanResult first = service.runInventoryRiskScan(request);
        InventoryRiskScanResult second = service.runInventoryRiskScan(request);

        assertThat(second).isEqualTo(first);
        assertThat(first.generatedCount()).isEqualTo(3);
        assertThat(first.alerts())
                .extracting(InventoryRiskAlertRecord::alertType, InventoryRiskAlertRecord::severity, InventoryRiskAlertRecord::itemId)
                .contains(
                        Tuple.tuple("safety_stock_low", "medium", lowStockItem.id()),
                        Tuple.tuple("out_of_stock", "high", outOfStockItem.id()),
                        Tuple.tuple("expiry_warning", "high", lowStockItem.id())
                );
        assertThat(inventoryRepository.riskAlerts).hasSize(3);
        assertThat(service.listRiskAlerts().total()).isEqualTo(3);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("inventory.risk_scan.completed", "inventory.risk_alert.viewed");
    }

    @Test
    void supplierReconciliationCalculatesDifferenceAndStoresEvidenceIdempotently() {
        InventorySupplierRecord supplier = service.createInventorySupplier(new InventorySupplierRequest(
                "supplier-recon",
                null,
                List.of(),
                "Supplier Recon",
                "Bob",
                "13900139000",
                "active"
        ));
        SupplierReconciliationRequest request = new SupplierReconciliationRequest(
                "supplier-recon-202606",
                "monthly reconciliation",
                List.of("att_invoice", "att_bank_flow"),
                supplier.id(),
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                100000L,
                88000L,
                "difference_found",
                "staff_finance"
        );

        SupplierReconciliationRecord first = service.createSupplierReconciliation(request);
        SupplierReconciliationRecord second = service.createSupplierReconciliation(request);

        assertThat(second).isEqualTo(first);
        assertThat(first)
                .extracting(
                        SupplierReconciliationRecord::supplierId,
                        SupplierReconciliationRecord::expectedAmountCent,
                        SupplierReconciliationRecord::actualAmountCent,
                        SupplierReconciliationRecord::differenceAmountCent,
                        SupplierReconciliationRecord::status
                )
                .containsExactly(supplier.id(), 100000L, 88000L, -12000L, "difference_found");
        assertThat(first.evidenceAttachmentIds()).containsExactly("att_invoice", "att_bank_flow");
        assertThat(service.listSupplierReconciliations().total()).isEqualTo(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("inventory.supplier_reconciliation.created", "inventory.supplier_reconciliation.viewed");
    }

    // A1(REQ-30): 登记瓶底标签后，取酒 verificationCode 不一致进入人工复核且不出酒
    @Test
    void claimWithMismatchedLabelEntersIdentityReviewWithoutDispensing() {
        InventoryItemRecord item = createItem("identity-item");
        SavedBottleRecord savedBottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-identity", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 2, "bottle_name", "Whisky", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "LABEL-XYZ", "张三", "13800138000", true, 100000L,
                List.of("reg_photo"), null));

        SavedBottleClaimRecord review = service.claimSavedBottle(savedBottle.id(), new SavedBottleClaimRequest(
                "claim-mismatch", null, List.of(), List.of(Map.of("quantity", 1)), "13800138000", "WRONG-LABEL"));

        assertThat(review.status()).isEqualTo("pending_identity_review");
        assertThat(inventoryRepository.findSavedBottleById(savedBottle.id()))
                .get()
                .extracting(SavedBottleRecord::status, SavedBottleRecord::remainingQuantity)
                .containsExactly("pending_identity_review", 2);
    }

    // P0#4(R6-D05): 散客（customer_id 可空）可存，登记三件套（姓名昵称+标签值+登记照片）落库。
    @Test
    void walkInGuestCanStoreBottleWithoutCustomerId() {
        InventoryItemRecord item = createItem("walkin-item");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-walkin", null, List.of(), null /* 散客无 customer_id */, "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Gin", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "LBL-WALKIN", "散客张", "13800138000", null, null,
                List.of("reg_photo_1"), "前台手填"));

        assertThat(bottle.customerId()).isNull();
        assertThat(bottle.status()).isEqualTo("stored");
        assertThat(bottle.bottleLabelValue()).isEqualTo("LBL-WALKIN");
        assertThat(bottle.customerNameOrNickname()).isEqualTo("散客张");
        assertThat(bottle.registrationPhotoRefs()).containsExactly("reg_photo_1");
        assertThat(bottle.manualNote()).isEqualTo("前台手填");
    }

    // P0#4: 登记缺标签值/姓名昵称/登记照片任一 → 拒。
    @Test
    void createSavedBottleRejectsWhenRegistrationMissing() {
        InventoryItemRecord item = createItem("reg-missing");
        assertThatThrownBy(() -> service.createSavedBottle(new SavedBottleRequest(
                "saved-reg-miss", null, List.of(), null, "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Gin", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                null, "散客张", null, null, null, List.of("reg_photo"), null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");

        assertThatThrownBy(() -> service.createSavedBottle(new SavedBottleRequest(
                "saved-reg-miss2", null, List.of(), null, "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Gin", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "LBL", "散客张", null, null, null, List.of() /* 无登记照片 */, null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
    }

    // P0#4: 取酒核身——标签值+姓名昵称同时一致才出酒（正例）。
    @Test
    void claimWithMatchingLabelAndNicknameDispenses() {
        InventoryItemRecord item = createItem("claim-id-ok");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-idok", null, List.of(), null, "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 2, "bottle_name", "Rum", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "LBL-OK", "李四", "13800138000", null, null, List.of("reg_photo"), null));

        SavedBottleClaimRecord ok = service.claimSavedBottle(bottle.id(), new SavedBottleClaimRequest(
                "claim-ok", null, List.of(), List.of(Map.of("quantity", 1)), "13800138000", "x",
                "LBL-OK", "李四"));
        assertThat(ok.status()).isEqualTo("picked_up");
    }

    // P0#4: 取酒核身——姓名昵称不符即使标签值一致也进人工复核、不出酒（反例）。
    @Test
    void claimWithMismatchedNicknameEntersReview() {
        InventoryItemRecord item = createItem("claim-id-bad");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-idbad", null, List.of(), null, "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 2, "bottle_name", "Rum", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "LBL-OK", "李四", "13800138000", null, null, List.of("reg_photo"), null));

        SavedBottleClaimRecord review = service.claimSavedBottle(bottle.id(), new SavedBottleClaimRequest(
                "claim-name-bad", null, List.of(), List.of(Map.of("quantity", 1)), "13800138000", "x",
                "LBL-OK", "王五"));
        assertThat(review.status()).isEqualTo("pending_identity_review");
        assertThat(inventoryRepository.findSavedBottleById(bottle.id()).orElseThrow().remainingQuantity())
                .isEqualTo(2);
    }

    // P0#4: 转赠——原瓶 transferred、流水留痕、新瓶 stored 归受让人。
    @Test
    void transferSavedBottleReassignsToNewOwner() {
        InventoryItemRecord item = createItem("transfer-item");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-transfer", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 3, "bottle_name", "Tequila", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "LBL-TR", "原主", "13800138000", null, null, List.of("reg_photo"), null));

        SavedBottleRecord newBottle = service.transferSavedBottle(bottle.id(), new SavedBottleTransferRequest(
                "transfer-1", null, List.of("transfer_evidence"), "受赠人赵", "13900139000", "生日礼"));

        assertThat(inventoryRepository.findSavedBottleById(bottle.id()).orElseThrow().status())
                .isEqualTo("transferred");
        assertThat(newBottle.status()).isEqualTo("stored");
        assertThat(newBottle.customerId()).isNull();
        assertThat(newBottle.customerNameOrNickname()).isEqualTo("受赠人赵");
        assertThat(newBottle.remainingQuantity()).isEqualTo(3);
        assertThat(inventoryRepository.savedBottleTransfers).hasSize(1);
        assertThat(inventoryRepository.savedBottleTransfers.getFirst().toNameOrNickname()).isEqualTo("受赠人赵");
    }

    // P0#4: 已转赠/非 stored 不可再转赠。
    @Test
    void transferRejectsWhenNotStored() {
        InventoryItemRecord item = createItem("transfer-bad");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-transfer-bad", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Tequila", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "LBL-TRB", "原主", null, null, null, List.of("reg_photo"), null));
        service.transferSavedBottle(bottle.id(), new SavedBottleTransferRequest(
                "transfer-bad-1", null, List.of(), "受赠人", null, null));

        assertThatThrownBy(() -> service.transferSavedBottle(bottle.id(), new SavedBottleTransferRequest(
                "transfer-bad-2", null, List.of(), "再受赠人", null, null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SAVED_BOTTLE_NOT_TRANSFERABLE");
    }

    // P0#4: 争议受理——stored 置 disputed。
    @Test
    void disputeSavedBottleMovesToDisputedState() {
        InventoryItemRecord item = createItem("dispute-item");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-dispute", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Brandy", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "LBL-DP", "客", null, null, null, List.of("reg_photo"), null));

        SavedBottleRecord disputed = service.disputeSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "dispute-1", null, List.of(), "客人质疑酒量", List.of(), List.of()));
        assertThat(disputed.status()).isEqualTo("disputed");
    }

    // R5 T6: 高价值存酒作废缺视频（仅照片）→ 拒（双证据须分类）
    @Test
    void highValueSavedBottleCancelRejectsWhenVideoMissing() {
        InventoryItemRecord item = createItem("hv-cancel");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-hv", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Rare", "unit", "bottle")),
                Instant.now().plusSeconds(3600), "LBL-HV", "客", null, true, 500000L,
                List.of("reg_photo"), null));

        // 仅照片、缺视频 → 拒。
        assertThatThrownBy(() -> service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-1", null, List.of(), "broken", List.of("att_photo"), List.of())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("HIGH_VALUE_EVIDENCE_REQUIRED"));

        // 照片+视频双证据 → 不直接作废：建 pending 复核 + 存酒置中间态 disputed_frozen。
        SavedBottleRecord frozen = service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-2", null, List.of(), "broken", List.of("att_photo"), List.of("att_video")));
        assertThat(frozen.status()).isEqualTo("disputed_frozen");
        assertThat(inventoryRepository.listHighValueReviews()).hasSize(1);
        InventoryHighValueReviewRecord review = inventoryRepository.listHighValueReviews().getFirst();
        assertThat(review.reviewStatus()).isEqualTo("pending");
        assertThat(review.subjectType()).isEqualTo("saved_bottle_void");
        assertThat(review.subjectId()).isEqualTo(bottle.id());
    }

    // R5 T6: 高价值缺照片（仅视频）→ 拒，message 含证据文案且不含 supervisor review
    @Test
    void highValueVoidRejectsWhenPhotoMissingWithEvidenceWording() {
        InventoryItemRecord item = createItem("hv-wording");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-hv-wording", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Rare", "unit", "bottle")),
                Instant.now().plusSeconds(3600), "LBL-HVW", "客", null, true, 500000L,
                List.of("reg_photo"), null));

        assertThatThrownBy(() -> service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-wording", null, List.of(), "broken", List.of(), List.of("att_video"))))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("HIGH_VALUE_EVIDENCE_REQUIRED");
                    assertThat(e.getMessage()).contains("照片+视频双证据");
                    assertThat(e.getMessage()).doesNotContain("supervisor review");
                });
    }

    // R5 T6: 双证据 → pending → 主管 approve 后存酒 cancelled
    @Test
    void highValueVoidApproveCancelsBottle() {
        InventoryItemRecord item = createItem("hv-approve");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-hv-approve", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Rare", "unit", "bottle")),
                Instant.now().plusSeconds(3600), "LBL-HVA", "客", null, true, 500000L,
                List.of("reg_photo"), null));

        SavedBottleRecord frozen = service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-approve", null, List.of(), "broken", List.of("att_photo"), List.of("att_video")));
        assertThat(frozen.status()).isEqualTo("disputed_frozen");
        String reviewId = inventoryRepository.listHighValueReviews().getFirst().id();

        InventoryHighValueReviewRecord decided = service.reviewHighValue(reviewId,
                new com.complex.platform.inventory.dto.HighValueReviewDecisionRequest(
                        "review-approve", "approved", "confirmed"));
        assertThat(decided.reviewStatus()).isEqualTo("approved");
        assertThat(inventoryRepository.findSavedBottleById(bottle.id()).orElseThrow().status())
                .isEqualTo("cancelled");
    }

    // R5 T6: 主管 reject → 存酒保留（维持 disputed_frozen，不作废）
    @Test
    void highValueVoidRejectKeepsBottle() {
        InventoryItemRecord item = createItem("hv-reject");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-hv-reject", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Rare", "unit", "bottle")),
                Instant.now().plusSeconds(3600), "LBL-HVR", "客", null, true, 500000L,
                List.of("reg_photo"), null));

        service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-reject", null, List.of(), "broken", List.of("att_photo"), List.of("att_video")));
        String reviewId = inventoryRepository.listHighValueReviews().getFirst().id();

        InventoryHighValueReviewRecord decided = service.reviewHighValue(reviewId,
                new com.complex.platform.inventory.dto.HighValueReviewDecisionRequest(
                        "review-reject", "rejected", "insufficient"));
        assertThat(decided.reviewStatus()).isEqualTo("rejected");
        assertThat(inventoryRepository.findSavedBottleById(bottle.id()).orElseThrow().status())
                .isEqualTo("disputed_frozen");
    }

    // R5 T6: 非高价值无证据可直接作废（向后兼容回归保护）
    @Test
    void nonHighValueVoidIgnoresEvidenceRuleAndCancelsDirectly() {
        InventoryItemRecord item = createItem("non-hv");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-non-hv", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "House", "unit", "bottle")),
                Instant.now().plusSeconds(3600), "LBL-NHV", "客", null, false, 1000L,
                List.of("reg_photo"), null));

        SavedBottleRecord cancelled = service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-non-hv", null, List.of(), "broken", List.of(), List.of()));
        assertThat(cancelled.status()).isEqualTo("cancelled");
        assertThat(inventoryRepository.listHighValueReviews()).isEmpty();
    }

    // ── R5 T6-4：报损(damage_report)/盘点差异(stock_variance)高价值入口接入域内复核 ──────────────

    // 高价值报损（loss）无双证据 → 拒（HIGH_VALUE_EVIDENCE_REQUIRED）。
    @Test
    void highValueDamageReportRejectsWhenEvidenceMissing() {
        InventoryItemRecord item = createHighValueItem("hv-damage-noevid", 100_000L); // 阈值 1000 元
        seedStock(item.id(), "warehouse_1", 10);

        // quantity 5 × 单价 30000 分 = 150000 ≥ 100000 → 高价值，缺视频 → 拒。
        assertThatThrownBy(() -> service.createStockAdjustment(new StockAdjustmentRequest(
                "damage-noevid", null, List.of(), "loss",
                List.of(Map.of("item_id", item.id(), "quantity", 5, "location_id", "warehouse_1")),
                "broken case", 30_000L, List.of("att_photo"), List.of())))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("HIGH_VALUE_EVIDENCE_REQUIRED"));

        // 库存未变更（未即时扣减）。
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(10);
    }

    // 高价值报损双证据 → 建 pending 复核 + 调整 pending_review + 未即时扣减。
    @Test
    void highValueDamageReportCreatesPendingAndDoesNotDeductImmediately() {
        InventoryItemRecord item = createHighValueItem("hv-damage-pending", 100_000L);
        seedStock(item.id(), "warehouse_1", 10);

        StockAdjustmentRecord pending = service.createStockAdjustment(new StockAdjustmentRequest(
                "damage-pending", null, List.of(), "loss",
                List.of(Map.of("item_id", item.id(), "quantity", 5, "location_id", "warehouse_1")),
                "broken case", 30_000L, List.of("att_photo"), List.of("att_video")));

        assertThat(pending.status()).isEqualTo("pending_review");
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(10); // 未即时扣减
        assertThat(inventoryRepository.listHighValueReviews()).hasSize(1);
        InventoryHighValueReviewRecord review = inventoryRepository.listHighValueReviews().getFirst();
        assertThat(review.reviewStatus()).isEqualTo("pending");
        assertThat(review.subjectType()).isEqualTo("damage_report");
        assertThat(review.subjectId()).isEqualTo(pending.id());
    }

    // 高价值报损 approve → 真正扣减 + 调整 completed。
    @Test
    void highValueDamageReportApproveDeductsStock() {
        InventoryItemRecord item = createHighValueItem("hv-damage-approve", 100_000L);
        seedStock(item.id(), "warehouse_1", 10);

        StockAdjustmentRecord pending = service.createStockAdjustment(new StockAdjustmentRequest(
                "damage-approve", null, List.of(), "loss",
                List.of(Map.of("item_id", item.id(), "quantity", 5, "location_id", "warehouse_1")),
                "broken case", 30_000L, List.of("att_photo"), List.of("att_video")));
        String reviewId = inventoryRepository.listHighValueReviews().getFirst().id();

        InventoryHighValueReviewRecord decided = service.reviewHighValue(reviewId,
                new com.complex.platform.inventory.dto.HighValueReviewDecisionRequest(
                        "damage-approve-review", "approved", "confirmed"));

        assertThat(decided.reviewStatus()).isEqualTo("approved");
        assertThat(inventoryRepository.findStockAdjustmentByIdForUpdate(pending.id()).orElseThrow().status())
                .isEqualTo("completed");
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(5); // 10 - 5 扣减生效
    }

    // 高价值报损 reject → 不扣减 + 调整 rejected。
    @Test
    void highValueDamageReportRejectDoesNotDeductStock() {
        InventoryItemRecord item = createHighValueItem("hv-damage-reject", 100_000L);
        seedStock(item.id(), "warehouse_1", 10);

        StockAdjustmentRecord pending = service.createStockAdjustment(new StockAdjustmentRequest(
                "damage-reject", null, List.of(), "loss",
                List.of(Map.of("item_id", item.id(), "quantity", 5, "location_id", "warehouse_1")),
                "broken case", 30_000L, List.of("att_photo"), List.of("att_video")));
        String reviewId = inventoryRepository.listHighValueReviews().getFirst().id();

        InventoryHighValueReviewRecord decided = service.reviewHighValue(reviewId,
                new com.complex.platform.inventory.dto.HighValueReviewDecisionRequest(
                        "damage-reject-review", "rejected", "insufficient"));

        assertThat(decided.reviewStatus()).isEqualTo("rejected");
        assertThat(inventoryRepository.findStockAdjustmentByIdForUpdate(pending.id()).orElseThrow().status())
                .isEqualTo("rejected");
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(10); // 未扣减
    }

    // 高价值盘点差异（count_correction）双证据 → pending；approve → 扣减生效。
    @Test
    void highValueStockVarianceCreatesPendingThenApproveDeducts() {
        InventoryItemRecord item = createHighValueItem("hv-variance", 100_000L);
        seedStock(item.id(), "warehouse_1", 8);

        StockAdjustmentRecord pending = service.createStockAdjustment(new StockAdjustmentRequest(
                "variance-pending", null, List.of(), "count_correction",
                List.of(Map.of("item_id", item.id(), "quantity", 3, "location_id", "warehouse_1")),
                "盘点短缺", 40_000L, List.of("att_photo"), List.of("att_video")));

        assertThat(pending.status()).isEqualTo("pending_review");
        InventoryHighValueReviewRecord review = inventoryRepository.listHighValueReviews().getFirst();
        assertThat(review.subjectType()).isEqualTo("stock_variance");
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(8); // 未即时扣减

        service.reviewHighValue(review.id(),
                new com.complex.platform.inventory.dto.HighValueReviewDecisionRequest(
                        "variance-approve-review", "approved", "确认差异"));
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(5); // 8 - 3 扣减生效
    }

    // 非高价值报损（金额低于阈值）→ 即时扣减（向后兼容回归保护）。
    @Test
    void belowThresholdDamageReportAdjustsImmediately() {
        InventoryItemRecord item = createHighValueItem("below-threshold", 1_000_000L); // 阈值 1 万元
        seedStock(item.id(), "warehouse_1", 10);

        StockAdjustmentRecord adjustment = service.createStockAdjustment(new StockAdjustmentRequest(
                "below-threshold-adjust", null, List.of(), "loss",
                List.of(Map.of("item_id", item.id(), "quantity", 2, "location_id", "warehouse_1")),
                "小额报损", 30_000L, null, null)); // 2 × 30000 = 60000 < 1000000 → 非高价值

        assertThat(adjustment.status()).isEqualTo("completed");
        assertThat(inventoryRepository.listHighValueReviews()).isEmpty();
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(8); // 即时扣减
    }

    // 物料无阈值（null）→ 报损永不触发复核，即时扣减（向后兼容）。
    @Test
    void nullThresholdItemDamageReportAdjustsImmediately() {
        InventoryItemRecord item = createItem("no-threshold-item"); // 阈值 null
        seedStock(item.id(), "warehouse_1", 10);

        StockAdjustmentRecord adjustment = service.createStockAdjustment(new StockAdjustmentRequest(
                "no-threshold-adjust", null, List.of(), "loss",
                List.of(Map.of("item_id", item.id(), "quantity", 5, "location_id", "warehouse_1")),
                "报损", 9_999_999L, null, null)); // 单价很高但阈值 null → 不视高价值

        assertThat(adjustment.status()).isEqualTo("completed");
        assertThat(inventoryRepository.listHighValueReviews()).isEmpty();
        assertThat(inventoryRepository.findStockBalance(item.id(), "warehouse_1").orElseThrow().availableQuantity())
                .isEqualTo(5);
    }

    private InventoryItemRecord createHighValueItem(String idempotencyKey, long thresholdCents) {
        return service.createInventoryItem(new InventoryItemRequest(
                idempotencyKey, null, List.of(), null, null,
                "HV " + idempotencyKey, "spirit", "bottle", 0, "active", thresholdCents));
    }

    private void seedStock(String itemId, String locationId, int quantity) {
        service.createStockIn(new StockInRequest(
                "seed-" + itemId + "-" + locationId, null, List.of(), "initial", null, locationId,
                List.of(Map.of("item_id", itemId, "quantity", quantity))));
    }

    // P1-3: verificationCode 不可缺失
    @Test
    void claimSavedBottleRequiresVerificationCode() {
        InventoryItemRecord item = createItem("verify-item");
        SavedBottleRecord savedBottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-verify",
                null,
                List.of(),
                "customer_1",
                "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Vodka", "unit", "bottle")),
                Instant.now().plusSeconds(3600),
                "VERIFY-XYZ", "客人甲", null, null, null,
                List.of("reg_photo"), null
        ));

        assertThatThrownBy(() -> service.claimSavedBottle(savedBottle.id(), new SavedBottleClaimRequest(
                "claim-no-code",
                null,
                List.of(),
                List.of(Map.of("quantity", 1)),
                "13800138000",
                null  // P1-3: verificationCode 缺失
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        assertThatThrownBy(() -> service.claimSavedBottle(savedBottle.id(), new SavedBottleClaimRequest(
                "claim-blank-code",
                null,
                List.of(),
                List.of(Map.of("quantity", 1)),
                "13800138000",
                "   "  // P1-3: verificationCode 空白
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    // P0-2: 跨商户越权访问寄存酒瓶必须被拒绝
    @Test
    void claimSavedBottleRejectsCrossMerchantAccess() {
        // 商户 B 创建的寄存记录
        inventoryRepository.saveSavedBottle(new SavedBottleRecord(
                "victim_bottle",
                "cust_b",
                "bar_b",
                "item_b",
                "Whiskey",
                1,
                1,
                "bottle",
                Instant.now().plusSeconds(3600),
                "stored",
                List.of(),
                Instant.now(),
                Instant.now(),
                "m_b",
                false,
                null,
                null,
                null,
                null,
                null
        ));

        // 商户 A 的用户尝试领取商户 B 的寄存酒瓶
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_A_ACTOR);

        assertThatThrownBy(() -> service.claimSavedBottle("victim_bottle", new SavedBottleClaimRequest(
                "claim-cross-merchant",
                null,
                List.of(),
                List.of(Map.of("quantity", 1)),
                "13800138000",
                "VERIFY-XYZ"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    // P1-2: 通过 typed 端点注入 inbound 类型必须被拒绝
    @Test
    void createTypedStockMovementRejectsInboundType() {
        InventoryItemRecord item = createItem("typed-item");
        service.createStockIn(new StockInRequest(
                "typed-stock-in",
                null,
                List.of(),
                "initial",
                null,
                "warehouse_1",
                List.of(Map.of("item_id", item.id(), "quantity", 10))
        ));

        assertThatThrownBy(() -> service.createTypedStockMovement("inbound", new StockMovementRequest(
                "typed-inbound-inject",
                null,
                List.of(),
                null,
                "warehouse_1",
                List.of(Map.of("item_id", item.id(), "quantity", 100)),
                null
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_MOVEMENT_TYPE");
    }

    // P1-4: adjustmentType 白名单校验
    @Test
    void createStockAdjustmentRejectsInvalidAdjustmentType() {
        InventoryItemRecord item = createItem("adjust-item");
        service.createStockIn(new StockInRequest(
                "adjust-stock-in",
                null,
                List.of(),
                "initial",
                null,
                "warehouse_1",
                List.of(Map.of("item_id", item.id(), "quantity", 10))
        ));

        assertThatThrownBy(() -> service.createStockAdjustment(new StockAdjustmentRequest(
                "adjust-invalid-type",
                null,
                List.of(),
                "credit_in",  // P1-4: 非法类型，不在白名单
                List.of(Map.of("item_id", item.id(), "quantity", 5, "location_id", "warehouse_1")),
                "inject via adjustment",
                null,
                null,
                null
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_ADJUSTMENT_TYPE");
    }

    // P1-5: operatorUserId 必须来自认证上下文，请求体中的值必须被忽略
    @Test
    void scanInventoryRisksIgnoresRequestBodyOperatorUserId() {
        InventoryItemRecord item = service.createInventoryItem(new InventoryItemRequest(
                "p15-risk-item",
                null,
                List.of(),
                null,
                null,
                "P15 Vodka",
                "spirit",
                "bottle",
                0,
                "active",
                null
        ));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "p15-bal",
                item.id(),
                "warehouse_1",
                0,
                0,
                0,
                Instant.parse("2026-06-14T00:00:00Z"),
                null,
                null
        ));
        // 请求体包含伪造的 operatorUserId
        InventoryRiskScanRequest request = new InventoryRiskScanRequest(
                "p15-risk-scan",
                "scan",
                7,
                "forged_operator_id"
        );

        InventoryRiskScanResult result = service.runInventoryRiskScan(request);

        // operatorUserId 必须是 "usr_admin"（来自 authorizationService.currentActor().id()），而非 "forged_operator_id"
        assertThat(result.alerts())
                .extracting(InventoryRiskAlertRecord::operatorUserId)
                .containsOnly(ADMIN_ACTOR.id());
    }

    // ----------------------------------------------------------------------------------------- P1 双写统一 risk_alerts

    /** 库存风险扫描双写统一表：source_domain=inventory，alertType 沿用本域值，medium→suspicious，high→high。 */
    @Test
    void scanInventoryRisksDoubleWritesToUnifiedRiskAlertsWithLevelMapping() {
        // 缺货(out_of_stock=high) item
        InventoryItemRecord outItem = service.createInventoryItem(new InventoryItemRequest(
                "dw-out-item", null, List.of(), null, null, "DW Out", "spirit", "bottle", 0, "active", null));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "dw-out-bal", outItem.id(), "warehouse_1", 0, 0, 0,
                Instant.parse("2026-06-14T00:00:00Z"), null, null));
        // 安全库存不足(safety_stock_low=medium) item：minQuantity>available>0
        InventoryItemRecord lowItem = service.createInventoryItem(new InventoryItemRequest(
                "dw-low-item", null, List.of(), null, null, "DW Low", "spirit", "bottle", 10, "active", null));
        // StockBalanceRecord 第6字段才是 availableQuantity：quantity=3, reserved=0, available=3，
        // 使 0<available(3)<minQuantity(10) 命中 safety_stock_low(medium)，而非 available=0 的 out_of_stock。
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "dw-low-bal", lowItem.id(), "warehouse_1", 3, 0, 3,
                Instant.parse("2026-06-14T00:00:00Z"), null, null));

        service.runInventoryRiskScan(new InventoryRiskScanRequest("dw-risk-scan", "scan", 7, null));

        org.mockito.ArgumentCaptor<com.complex.platform.evidence.dto.RiskAlertReportRequest> captor =
                org.mockito.ArgumentCaptor.forClass(com.complex.platform.evidence.dto.RiskAlertReportRequest.class);
        verify(riskAlertService, org.mockito.Mockito.times(2)).report(captor.capture());

        // 全部 source_domain=inventory
        assertThat(captor.getAllValues())
                .extracting(com.complex.platform.evidence.dto.RiskAlertReportRequest::sourceDomain)
                .containsOnly("inventory");
        // out_of_stock 保持 high
        assertThat(captor.getAllValues())
                .filteredOn(r -> "out_of_stock".equals(r.alertType()))
                .singleElement()
                .extracting(com.complex.platform.evidence.dto.RiskAlertReportRequest::riskLevel)
                .isEqualTo("high");
        // safety_stock_low(medium) 必须映射为 suspicious
        assertThat(captor.getAllValues())
                .filteredOn(r -> "safety_stock_low".equals(r.alertType()))
                .singleElement()
                .extracting(com.complex.platform.evidence.dto.RiskAlertReportRequest::riskLevel)
                .isEqualTo("suspicious");
    }

    /** 溯源交接断链异常双写统一表：source_domain=inventory，alertType=trace_handover_exception，riskLevel=high。 */
    @Test
    void traceHandoverExceptionDoubleWritesToUnifiedRiskAlerts() {
        InventoryItemRecord item = createItem("dw-handover-item");
        TraceHandoverChainView created = service.createTraceHandover(
                new com.complex.platform.inventory.dto.TraceHandoverCreateRequest(
                        "dw-h-create", "TRACE-DW", item.id(), "device_dw", List.of()));
        String handoverId = created.handover().id();
        // 先正常完成 out_verify 段
        service.scanTraceSegment(handoverId, new com.complex.platform.inventory.dto.TraceScanSegmentRequest(
                "dw-seg-out", "out_verify"));
        // 同段重复扫码 → duplicate 异常，触发 raiseException
        service.scanTraceSegment(handoverId, new com.complex.platform.inventory.dto.TraceScanSegmentRequest(
                "dw-seg-out-dup", "out_verify"));

        org.mockito.ArgumentCaptor<com.complex.platform.evidence.dto.RiskAlertReportRequest> captor =
                org.mockito.ArgumentCaptor.forClass(com.complex.platform.evidence.dto.RiskAlertReportRequest.class);
        verify(riskAlertService).report(captor.capture());
        assertThat(captor.getValue().sourceDomain()).isEqualTo("inventory");
        assertThat(captor.getValue().alertType()).isEqualTo("trace_handover_exception");
        assertThat(captor.getValue().riskLevel()).isEqualTo("high");
        // 孤岛表仍权威：异常已落 inventory_trace_handover_exceptions。
        assertThat(inventoryRepository.traceHandoverExceptions).isNotEmpty();
    }

    /** fail-safe：RiskAlertService 为 null 时主流程不抛、孤岛风险表照常写。 */
    @Test
    void scanInventoryRisksFailSafeWhenRiskAlertServiceNull() {
        InventoryService nullSafeService = new InventoryService(
                inventoryRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                authorizationService,
                new com.complex.platform.gate.TerminalSwitchGuard(
                        terminalDeviceRepository, new AuditEventService(auditEventRepository)),
                new com.complex.platform.evidence.TraceabilityService(traceabilityRepository, authorizationService),
                null);
        InventoryItemRecord outItem = nullSafeService.createInventoryItem(new InventoryItemRequest(
                "fs-out-item", null, List.of(), null, null, "FS Out", "spirit", "bottle", 0, "active", null));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "fs-out-bal", outItem.id(), "warehouse_1", 0, 0, 0,
                Instant.parse("2026-06-14T00:00:00Z"), null, null));

        InventoryRiskScanResult result =
                nullSafeService.runInventoryRiskScan(new InventoryRiskScanRequest("fs-risk-scan", "scan", 7, null));

        // 孤岛风险表照常写（权威）；未注入服务也不抛。
        assertThat(result.alerts())
                .extracting(InventoryRiskAlertRecord::alertType)
                .contains("out_of_stock");
    }

    /** fail-safe：report 抛 RuntimeException 时主流程不受影响，孤岛风险表仍写入。 */
    @Test
    void scanInventoryRisksFailSafeWhenReportThrows() {
        doThrow(new RuntimeException("unified down")).when(riskAlertService).report(any());
        InventoryItemRecord outItem = service.createInventoryItem(new InventoryItemRequest(
                "ft-out-item", null, List.of(), null, null, "FT Out", "spirit", "bottle", 0, "active", null));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "ft-out-bal", outItem.id(), "warehouse_1", 0, 0, 0,
                Instant.parse("2026-06-14T00:00:00Z"), null, null));

        InventoryRiskScanResult result =
                service.runInventoryRiskScan(new InventoryRiskScanRequest("ft-risk-scan", "scan", 7, null));

        // report 抛异常被 catch，主流程不受影响，孤岛表照常写。
        assertThat(result.alerts())
                .extracting(InventoryRiskAlertRecord::alertType)
                .contains("out_of_stock");
        verify(riskAlertService).report(any());
    }

    // 守卫：非 medium/high 之外的 alertType 不应误报（占位，确保无额外 report 调用）。
    @Test
    void scanInventoryRisksWithNoRiskDoesNotReport() {
        service.runInventoryRiskScan(new InventoryRiskScanRequest("noop-risk-scan", "scan", 7, null));
        verify(riskAlertService, never()).report(any());
    }

    // P1-5: 供应商对账 operatorUserId 必须来自认证上下文
    @Test
    void createSupplierReconciliationIgnoresRequestBodyOperatorUserId() {
        InventorySupplierRecord supplier = service.createInventorySupplier(new InventorySupplierRequest(
                "supplier-p15",
                null,
                List.of(),
                "P15 Supplier",
                "Eve",
                "13700137000",
                "active"
        ));
        SupplierReconciliationRequest request = new SupplierReconciliationRequest(
                "recon-p15",
                null,
                List.of(),
                supplier.id(),
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-30"),
                50000L,
                50000L,
                "matched",
                "forged_operator_id"  // P1-5: 请求体中的 operatorUserId 必须被忽略
        );

        SupplierReconciliationRecord result = service.createSupplierReconciliation(request);

        // operatorUserId 必须是 "usr_admin"（来自认证上下文），而非 "forged_operator_id"
        assertThat(result.operatorUserId()).isEqualTo(ADMIN_ACTOR.id());
    }

    // P1-1: 商户用户只能看到本商户数据
    @Test
    void listMethodsRespectMerchantScope() {
        // 商户 A 的数据
        inventoryRepository.saveInventoryItem(new InventoryItemRecord(
                "item_a", null, "Item A", "spirit", "bottle", null, false, 0, "active",
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"), "m_a", null
        ));
        // 商户 B 的数据
        inventoryRepository.saveInventoryItem(new InventoryItemRecord(
                "item_b", null, "Item B", "spirit", "bottle", null, false, 0, "active",
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"), "m_b", null
        ));

        // 平台管理员看全部
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        assertThat(service.listInventoryItems().items()).hasSize(2);

        // 商户 A 只看到商户 A 的数据
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        assertThat(service.listInventoryItems().items())
                .hasSize(1)
                .extracting(InventoryItemRecord::id)
                .containsExactly("item_a");

        // 商户 B 只看到商户 B 的数据
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        assertThat(service.listInventoryItems().items())
                .hasSize(1)
                .extracting(InventoryItemRecord::id)
                .containsExactly("item_b");
    }

    // ── V108/V126：盘点录入闭环测试。 ──

    @Test
    void createStocktakeStartsInCountingAndIsIdempotent() {
        CreateStocktakeRequest request = new CreateStocktakeRequest(
                "stocktake-1", "wh_main", LocalDate.parse("2026-06-19"), "month-end count");
        StocktakeRecord first = service.createStocktake(request);
        StocktakeRecord second = service.createStocktake(request);

        assertThat(second).isEqualTo(first);
        assertThat(first.status()).isEqualTo("counting");
        assertThat(first.warehouseId()).isEqualTo("wh_main");
        assertThat(first.stocktakeNo()).isNotBlank();
        assertThat(inventoryRepository.listStocktakes()).hasSize(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("inventory.stocktake.created");
    }

    @Test
    void recordVarianceComputesSignedVarianceQtyAndEstimatesAmountFromCost() {
        StocktakeRecord stocktake = service.createStocktake(
                new CreateStocktakeRequest("st-var", "wh_main", null, null));
        // book=10, counted=7 → variance_qty=-3（盘亏）；unit_cost=500 分 → amount=-1500。
        InventoryVarianceRecord variance = service.recordVariance(stocktake.id(), new RecordVarianceRequest(
                "var-1", "item_x", null,
                new java.math.BigDecimal("10"), new java.math.BigDecimal("7"),
                "shortage", null, null, 500L));

        assertThat(variance.varianceQty()).isEqualByComparingTo("-3");
        assertThat(variance.varianceAmountCent()).isEqualTo(-1500L);
        assertThat(variance.reviewStatus()).isEqualTo("pending");
        assertThat(variance.stocktakeId()).isEqualTo(stocktake.id());
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("inventory.stocktake.variance_recorded");
    }

    @Test
    void recordVarianceUsesExplicitAmountOverCostEstimate() {
        StocktakeRecord stocktake = service.createStocktake(
                new CreateStocktakeRequest("st-var2", "wh_main", null, null));
        InventoryVarianceRecord variance = service.recordVariance(stocktake.id(), new RecordVarianceRequest(
                "var-2", "item_y", "BATCH-1",
                new java.math.BigDecimal("5"), new java.math.BigDecimal("8"),
                "overage", null, 9999L, 100L));

        assertThat(variance.varianceQty()).isEqualByComparingTo("3");
        // 显式 9999 优先于 cost 估算 300。
        assertThat(variance.varianceAmountCent()).isEqualTo(9999L);
        assertThat(variance.batchNo()).isEqualTo("BATCH-1");
    }

    @Test
    void reviewVarianceApprovedUnlocksFinancePostingPrerequisite() {
        StocktakeRecord stocktake = service.createStocktake(
                new CreateStocktakeRequest("st-appr", "wh_main", null, null));
        InventoryVarianceRecord variance = service.recordVariance(stocktake.id(), new RecordVarianceRequest(
                "var-appr", "item_a", null,
                new java.math.BigDecimal("3"), new java.math.BigDecimal("0"),
                "damage", null, -3000L, null));

        InventoryVarianceRecord reviewed = service.reviewVariance(stocktake.id(), variance.id(),
                new ReviewVarianceRequest("rev-appr", "approved", "confirmed damage"));

        assertThat(reviewed.reviewStatus()).isEqualTo("approved");
        assertThat(reviewed.reviewedBy()).isEqualTo("usr_admin");
        assertThat(reviewed.reviewedAt()).isNotNull();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("inventory.stocktake.variance_reviewed");
    }

    @Test
    void reviewVarianceRejectedSetsTerminalRejected() {
        StocktakeRecord stocktake = service.createStocktake(
                new CreateStocktakeRequest("st-rej", "wh_main", null, null));
        InventoryVarianceRecord variance = service.recordVariance(stocktake.id(), new RecordVarianceRequest(
                "var-rej", "item_b", null,
                new java.math.BigDecimal("4"), new java.math.BigDecimal("4"),
                "supplier_fault", "sup_1", 1L, null));

        InventoryVarianceRecord reviewed = service.reviewVariance(stocktake.id(), variance.id(),
                new ReviewVarianceRequest("rev-rej", "rejected", "not justified"));

        assertThat(reviewed.reviewStatus()).isEqualTo("rejected");
    }

    @Test
    void reviewVarianceRejectsDuplicateReviewOnNonPending() {
        StocktakeRecord stocktake = service.createStocktake(
                new CreateStocktakeRequest("st-dup", "wh_main", null, null));
        InventoryVarianceRecord variance = service.recordVariance(stocktake.id(), new RecordVarianceRequest(
                "var-dup", "item_c", null,
                new java.math.BigDecimal("2"), new java.math.BigDecimal("1"),
                "shortage", null, -500L, null));
        service.reviewVariance(stocktake.id(), variance.id(),
                new ReviewVarianceRequest("rev-dup-1", "approved", null));

        // 不同幂等键的第二次复核 → 非 pending，被拒（条件 UPDATE 0 行）。
        assertThatThrownBy(() -> service.reviewVariance(stocktake.id(), variance.id(),
                new ReviewVarianceRequest("rev-dup-2", "rejected", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("INVENTORY_VARIANCE_NOT_PENDING");
    }

    @Test
    void stocktakeListAndDetailAreMerchantScoped() {
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_A_ACTOR);
        StocktakeRecord ownStocktake = service.createStocktake(
                new CreateStocktakeRequest("st-a", "wh_a", null, null));
        service.recordVariance(ownStocktake.id(), new RecordVarianceRequest(
                "va-a", "item_a", null,
                new java.math.BigDecimal("1"), new java.math.BigDecimal("0"),
                "shortage", null, -100L, null));

        // 详情含差异明细，归属一致。
        StocktakeDetailRecord detail = service.getStocktake(ownStocktake.id());
        assertThat(detail.variances()).hasSize(1);
        assertThat(service.listStocktakes().items())
                .extracting(StocktakeRecord::id)
                .containsExactly(ownStocktake.id());

        // 商户 B 看不到商户 A 的盘点单，详情访问被拒。
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_B_ACTOR);
        assertThat(service.listStocktakes().items()).isEmpty();
        assertThatThrownBy(() -> service.getStocktake(ownStocktake.id()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void reviewVarianceRejectsForeignStocktakeBinding() {
        StocktakeRecord stocktake = service.createStocktake(
                new CreateStocktakeRequest("st-bind", "wh_main", null, null));
        InventoryVarianceRecord variance = service.recordVariance(stocktake.id(), new RecordVarianceRequest(
                "var-bind", "item_d", null,
                new java.math.BigDecimal("2"), new java.math.BigDecimal("0"),
                "damage", null, -200L, null));

        // 用错误的 stocktakeId 复核该差异行 → 404（不属于该盘点单）。
        assertThatThrownBy(() -> service.reviewVariance("stocktake_wrong", variance.id(),
                new ReviewVarianceRequest("rev-bind", "approved", null)))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).code())
                .isEqualTo("INVENTORY_VARIANCE_NOT_FOUND");
    }

    private InventoryItemRecord createItem(String idempotencyKey) {
        return service.createInventoryItem(new InventoryItemRequest(
                idempotencyKey,
                null,
                List.of(),
                null,
                null,
                "Vodka " + idempotencyKey,
                "spirit",
                "bottle",
                0,
                "active",
                null
        ));
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }

    private static final class InMemoryInventoryRepository implements InventoryRepository {
        private final Map<String, InventoryItemRecord> inventoryItems = new LinkedHashMap<>();
        private final Map<String, InventorySupplierRecord> suppliers = new LinkedHashMap<>();
        private final Map<String, StockBalanceRecord> balances = new LinkedHashMap<>();
        private final Map<String, SavedBottleRecord> savedBottles = new LinkedHashMap<>();
        private final Map<String, InventoryStockBatchRecord> stockBatches = new LinkedHashMap<>();
        final List<InventoryRiskAlertRecord> riskAlerts = new ArrayList<>();
        private final Map<String, SupplierReconciliationRecord> supplierReconciliations = new LinkedHashMap<>();
        private final List<PurchaseRequestRecord> purchaseRequests = new ArrayList<>();
        final List<StockMovementRecord> stockMovements = new ArrayList<>();
        private final List<StockAdjustmentRecord> stockAdjustments = new ArrayList<>();
        final List<SavedBottleClaimRecord> savedBottleClaims = new ArrayList<>();
        private final List<TraceHandoverRecord> traceHandovers = new ArrayList<>();
        final Map<String, TraceHandoverRecord> traceHandoverHeaders = new LinkedHashMap<>();
        final Map<String, TraceHandoverStepRecord> traceHandoverSteps = new LinkedHashMap<>();
        final Map<String, TraceHandoverExceptionRecord> traceHandoverExceptions = new LinkedHashMap<>();
        final List<Map<String, Object>> robotTaskEvents = new ArrayList<>();

        @Override
        public InventoryItemRecord saveInventoryItem(InventoryItemRecord item) {
            inventoryItems.put(item.id(), item);
            return item;
        }

        @Override
        public List<InventoryItemRecord> listInventoryItems() {
            return new ArrayList<>(inventoryItems.values());
        }

        @Override
        public List<InventoryItemRecord> listInventoryItemsByMerchant(String merchantId) {
            return inventoryItems.values().stream()
                    .filter(item -> merchantId.equals(item.merchantId()))
                    .toList();
        }

        @Override
        public Optional<InventoryItemRecord> findInventoryItemById(String id) {
            return Optional.ofNullable(inventoryItems.get(id));
        }

        @Override
        public InventorySupplierRecord saveInventorySupplier(InventorySupplierRecord supplier) {
            suppliers.put(supplier.id(), supplier);
            return supplier;
        }

        @Override
        public List<InventorySupplierRecord> listInventorySuppliers() {
            return new ArrayList<>(suppliers.values());
        }

        @Override
        public List<InventorySupplierRecord> listInventorySuppliersByMerchant(String merchantId) {
            return suppliers.values().stream()
                    .filter(s -> merchantId.equals(s.merchantId()))
                    .toList();
        }

        final List<PurchaseRequestEventRecord> purchaseRequestEvents = new ArrayList<>();

        @Override
        public PurchaseRequestRecord savePurchaseRequest(PurchaseRequestRecord request) {
            purchaseRequests.removeIf(r -> r.id().equals(request.id()));
            purchaseRequests.add(request);
            return request;
        }

        @Override
        public List<PurchaseRequestRecord> listPurchaseRequests() {
            return new ArrayList<>(purchaseRequests);
        }

        @Override
        public Optional<PurchaseRequestRecord> findPurchaseRequestByIdForUpdate(String id) {
            return purchaseRequests.stream().filter(r -> r.id().equals(id)).reduce((first, second) -> second);
        }

        @Override
        public PurchaseRequestEventRecord savePurchaseRequestEvent(PurchaseRequestEventRecord event) {
            purchaseRequestEvents.add(event);
            return event;
        }

        @Override
        public List<PurchaseRequestEventRecord> listPurchaseRequestEvents(String requestId) {
            return purchaseRequestEvents.stream()
                    .filter(e -> requestId.equals(e.requestId()))
                    .toList();
        }

        @Override
        public List<PurchaseRequestRecord> listPurchaseRequestsByMerchant(String merchantId) {
            return purchaseRequests.stream()
                    .filter(r -> merchantId.equals(r.merchantId()))
                    .toList();
        }

        @Override
        public Optional<StockBalanceRecord> findStockBalance(String itemId, String locationId) {
            return Optional.ofNullable(balances.get(balanceKey(itemId, locationId)));
        }

        @Override
        public StockBalanceRecord saveStockBalance(StockBalanceRecord balance) {
            balances.put(balanceKey(balance.itemId(), balance.locationId()), balance);
            return balance;
        }

        @Override
        public List<StockBalanceRecord> listStockBalances() {
            return new ArrayList<>(balances.values());
        }

        @Override
        public List<StockBalanceRecord> listStockBalancesByMerchant(String merchantId) {
            return balances.values().stream()
                    .filter(b -> merchantId.equals(b.merchantId()))
                    .toList();
        }

        // P0-1: 原子减少库存余额（available_quantity >= qty 检查）
        @Override
        public int atomicDecreaseStockBalance(String itemId, String locationId, int qty) {
            String key = balanceKey(itemId, locationId);
            StockBalanceRecord existing = balances.get(key);
            if (existing == null || existing.availableQuantity() < qty) {
                return 0;
            }
            balances.put(key, new StockBalanceRecord(
                    existing.id(),
                    itemId,
                    locationId,
                    existing.quantity() - qty,
                    existing.reservedQuantity(),
                    existing.availableQuantity() - qty,
                    Instant.now(),
                    existing.merchantId(),
                    existing.warehouseId()
            ));
            return 1;
        }

        // P0-1: 原子增加库存余额（INSERT ON CONFLICT DO UPDATE 语义）
        @Override
        public void atomicIncreaseStockBalance(String itemId, String locationId, int qty, String newId, String merchantId, String warehouseId) {
            String key = balanceKey(itemId, locationId);
            StockBalanceRecord existing = balances.get(key);
            if (existing == null) {
                balances.put(key, new StockBalanceRecord(
                        newId, itemId, locationId, qty, 0, qty, Instant.now(), merchantId, warehouseId));
            } else {
                balances.put(key, new StockBalanceRecord(
                        existing.id(),
                        itemId,
                        locationId,
                        existing.quantity() + qty,
                        existing.reservedQuantity(),
                        existing.availableQuantity() + qty,
                        Instant.now(),
                        existing.merchantId(),
                        warehouseId == null ? existing.warehouseId() : warehouseId
                ));
            }
        }

        @Override
        public InventoryStockBatchRecord saveStockBatch(InventoryStockBatchRecord batch) {
            stockBatches.put(batch.id(), batch);
            return batch;
        }

        @Override
        public List<InventoryStockBatchRecord> listStockBatches() {
            return new ArrayList<>(stockBatches.values());
        }

        @Override
        public List<InventoryStockBatchRecord> listStockBatchesByMerchant(String merchantId) {
            return stockBatches.values().stream()
                    .filter(b -> merchantId.equals(b.merchantId()))
                    .toList();
        }

        @Override
        public InventoryRiskAlertRecord saveRiskAlert(InventoryRiskAlertRecord alert) {
            riskAlerts.add(alert);
            return alert;
        }

        @Override
        public List<InventoryRiskAlertRecord> listRiskAlerts() {
            return new ArrayList<>(riskAlerts);
        }

        @Override
        public List<InventoryRiskAlertRecord> listRiskAlertsByMerchant(String merchantId) {
            return riskAlerts.stream()
                    .filter(a -> merchantId.equals(a.merchantId()))
                    .toList();
        }

        @Override
        public SupplierReconciliationRecord saveSupplierReconciliation(SupplierReconciliationRecord reconciliation) {
            supplierReconciliations.put(reconciliation.id(), reconciliation);
            return reconciliation;
        }

        @Override
        public List<SupplierReconciliationRecord> listSupplierReconciliations() {
            return new ArrayList<>(supplierReconciliations.values());
        }

        @Override
        public List<SupplierReconciliationRecord> listSupplierReconciliationsByMerchant(String merchantId) {
            return supplierReconciliations.values().stream()
                    .filter(r -> merchantId.equals(r.merchantId()))
                    .toList();
        }

        @Override
        public StockMovementRecord saveStockMovement(StockMovementRecord movement) {
            stockMovements.add(movement);
            return movement;
        }

        @Override
        public List<StockMovementRecord> listStockMovements() {
            return new ArrayList<>(stockMovements);
        }

        @Override
        public List<StockMovementRecord> listStockMovementsByMerchant(String merchantId) {
            return stockMovements.stream()
                    .filter(m -> merchantId.equals(m.merchantId()))
                    .toList();
        }

        @Override
        public StockAdjustmentRecord saveStockAdjustment(StockAdjustmentRecord adjustment) {
            stockAdjustments.removeIf(a -> a.id().equals(adjustment.id()));
            stockAdjustments.add(adjustment);
            return adjustment;
        }

        @Override
        public Optional<StockAdjustmentRecord> findStockAdjustmentByIdForUpdate(String id) {
            return stockAdjustments.stream().filter(a -> a.id().equals(id)).reduce((first, second) -> second);
        }

        @Override
        public SavedBottleRecord saveSavedBottle(SavedBottleRecord savedBottle) {
            savedBottles.put(savedBottle.id(), savedBottle);
            return savedBottle;
        }

        @Override
        public Optional<SavedBottleRecord> findSavedBottleById(String id) {
            return Optional.ofNullable(savedBottles.get(id));
        }

        // P0-2: 行锁读取（内存实现等价于普通读取）
        @Override
        public Optional<SavedBottleRecord> findSavedBottleByIdForUpdate(String id) {
            return Optional.ofNullable(savedBottles.get(id));
        }

        @Override
        public List<SavedBottleRecord> listSavedBottles() {
            return new ArrayList<>(savedBottles.values());
        }

        @Override
        public List<SavedBottleRecord> listSavedBottlesByMerchant(String merchantId) {
            return savedBottles.values().stream()
                    .filter(b -> merchantId.equals(b.merchantId()))
                    .toList();
        }

        @Override
        public SavedBottleClaimRecord saveSavedBottleClaim(SavedBottleClaimRecord claim) {
            savedBottleClaims.add(claim);
            return claim;
        }

        final List<SavedBottleTransferRecord> savedBottleTransfers = new ArrayList<>();

        @Override
        public SavedBottleTransferRecord saveSavedBottleTransfer(SavedBottleTransferRecord transfer) {
            savedBottleTransfers.removeIf(t -> t.id().equals(transfer.id()));
            savedBottleTransfers.add(transfer);
            return transfer;
        }

        @Override
        public TraceHandoverRecord saveTraceHandover(TraceHandoverRecord handover) {
            traceHandovers.removeIf(h -> h.id().equals(handover.id()));
            traceHandovers.add(handover);
            traceHandoverHeaders.put(handover.id(), handover);
            return handover;
        }

        @Override
        public Optional<TraceHandoverRecord> findTraceHandoverByIdForUpdate(String id) {
            return Optional.ofNullable(traceHandoverHeaders.get(id));
        }

        @Override
        public List<TraceHandoverStepRecord> listTraceHandoverSteps(String handoverId) {
            return traceHandoverSteps.values().stream()
                    .filter(s -> s.handoverId().equals(handoverId))
                    .sorted(java.util.Comparator.comparingInt(TraceHandoverStepRecord::seq))
                    .toList();
        }

        @Override
        public Optional<TraceHandoverStepRecord> findTraceHandoverStep(String handoverId, String segment) {
            return traceHandoverSteps.values().stream()
                    .filter(s -> s.handoverId().equals(handoverId) && s.segment().equals(segment))
                    .findFirst();
        }

        @Override
        public TraceHandoverStepRecord saveTraceHandoverStep(TraceHandoverStepRecord step) {
            traceHandoverSteps.put(step.id(), step);
            return step;
        }

        @Override
        public TraceHandoverExceptionRecord saveTraceHandoverException(TraceHandoverExceptionRecord exception) {
            traceHandoverExceptions.put(exception.id(), exception);
            return exception;
        }

        @Override
        public Optional<TraceHandoverExceptionRecord> findTraceHandoverExceptionByIdForUpdate(String id) {
            return Optional.ofNullable(traceHandoverExceptions.get(id));
        }

        @Override
        public void saveRobotTaskEvent(
                String id, String robotTaskRef, String eventType, String handoverId, String payloadJson, Instant reportedAt) {
            robotTaskEvents.add(Map.of(
                    "id", id,
                    "robot_task_ref", robotTaskRef == null ? "" : robotTaskRef,
                    "event_type", eventType == null ? "" : eventType,
                    "handover_id", handoverId == null ? "" : handoverId));
        }

        // R5 T6：供应合同 + 库房 in-memory 存储。
        final Map<String, SupplyContractRecord> supplyContracts = new LinkedHashMap<>();
        final Map<String, WarehouseRecord> warehouses = new LinkedHashMap<>();

        @Override
        public SupplyContractRecord saveSupplyContract(SupplyContractRecord contract) {
            supplyContracts.put(contract.id(), contract);
            return contract;
        }

        @Override
        public Optional<SupplyContractRecord> findSupplyContractByIdForUpdate(String id) {
            return Optional.ofNullable(supplyContracts.get(id));
        }

        @Override
        public List<SupplyContractRecord> listSupplyContracts() {
            return new ArrayList<>(supplyContracts.values());
        }

        @Override
        public List<SupplyContractRecord> listSupplyContractsByMerchant(String merchantId) {
            return supplyContracts.values().stream()
                    .filter(c -> merchantId.equals(c.merchantId()))
                    .toList();
        }

        @Override
        public WarehouseRecord saveWarehouse(WarehouseRecord warehouse) {
            warehouses.put(warehouse.id(), warehouse);
            return warehouse;
        }

        @Override
        public List<WarehouseRecord> listWarehouses() {
            return new ArrayList<>(warehouses.values());
        }

        @Override
        public List<WarehouseRecord> listWarehousesByMerchant(String merchantId) {
            return warehouses.values().stream()
                    .filter(w -> merchantId.equals(w.merchantId()))
                    .toList();
        }

        final Map<String, InventoryHighValueReviewRecord> highValueReviews = new LinkedHashMap<>();

        @Override
        public InventoryHighValueReviewRecord saveHighValueReview(InventoryHighValueReviewRecord review) {
            highValueReviews.put(review.id(), review);
            return review;
        }

        @Override
        public Optional<InventoryHighValueReviewRecord> findHighValueReviewByIdForUpdate(String id) {
            return Optional.ofNullable(highValueReviews.get(id));
        }

        @Override
        public List<InventoryHighValueReviewRecord> listHighValueReviews() {
            return new ArrayList<>(highValueReviews.values());
        }

        // P1#18：供应商资质/结算子表（本测试不触达，统一抛出）。
        @Override public SupplierQualificationRecord saveSupplierQualification(SupplierQualificationRecord q) {
            throw new UnsupportedOperationException("not needed");
        }
        @Override public List<SupplierQualificationRecord> listSupplierQualificationsBySupplier(String s) {
            throw new UnsupportedOperationException("not needed");
        }
        @Override public List<SupplierQualificationRecord> listSupplierQualificationsByMerchant(String m) {
            throw new UnsupportedOperationException("not needed");
        }
        @Override public List<SupplierQualificationRecord> listSupplierQualifications() {
            throw new UnsupportedOperationException("not needed");
        }
        @Override public SupplierSettlementMethodRecord saveSupplierSettlementMethod(SupplierSettlementMethodRecord m) {
            throw new UnsupportedOperationException("not needed");
        }
        @Override public List<SupplierSettlementMethodRecord> listSupplierSettlementMethodsBySupplier(String s) {
            throw new UnsupportedOperationException("not needed");
        }
        @Override public List<SupplierSettlementMethodRecord> listSupplierSettlementMethodsByMerchant(String m) {
            throw new UnsupportedOperationException("not needed");
        }
        @Override public List<SupplierSettlementMethodRecord> listSupplierSettlementMethods() {
            throw new UnsupportedOperationException("not needed");
        }

        // ── V108/V126：盘点单 + 库存差异明细写入侧内存替身。 ──
        final Map<String, StocktakeRecord> stocktakes = new LinkedHashMap<>();
        final Map<String, InventoryVarianceRecord> variances = new LinkedHashMap<>();

        @Override public StocktakeRecord saveStocktake(StocktakeRecord stocktake) {
            stocktakes.put(stocktake.id(), stocktake);
            return stocktake;
        }

        @Override public Optional<StocktakeRecord> findStocktakeById(String id) {
            return Optional.ofNullable(stocktakes.get(id));
        }

        @Override public List<StocktakeRecord> listStocktakes() {
            return new ArrayList<>(stocktakes.values());
        }

        @Override public List<StocktakeRecord> listStocktakesByMerchant(String merchantId) {
            return stocktakes.values().stream()
                    .filter(s -> merchantId.equals(s.merchantId()))
                    .toList();
        }

        @Override public InventoryVarianceRecord saveInventoryVariance(InventoryVarianceRecord variance) {
            variances.put(variance.id(), variance);
            return variance;
        }

        @Override public Optional<InventoryVarianceRecord> findInventoryVarianceByIdForUpdate(String id) {
            return Optional.ofNullable(variances.get(id));
        }

        @Override public List<InventoryVarianceRecord> listInventoryVariancesByStocktake(String stocktakeId) {
            return variances.values().stream()
                    .filter(v -> stocktakeId.equals(v.stocktakeId()))
                    .toList();
        }

        @Override public int reviewInventoryVarianceIfPending(
                String varianceId, String decision, String reviewedBy, Instant reviewedAt) {
            InventoryVarianceRecord current = variances.get(varianceId);
            if (current == null || !"pending".equals(current.reviewStatus())) {
                return 0;
            }
            variances.put(varianceId, new InventoryVarianceRecord(
                    current.id(), current.stocktakeId(), current.itemId(), current.batchNo(),
                    current.bookQty(), current.countedQty(), current.varianceQty(),
                    current.varianceAmountCent(), current.reason(), current.supplierId(),
                    decision, reviewedBy, reviewedAt, current.postedArApItemId(),
                    current.merchantId(), current.createdAt(), reviewedAt));
            return 1;
        }

        private String balanceKey(String itemId, String locationId) {
            return itemId + ":" + locationId;
        }
    }

    /** 三层留痕内存替身：仅承载交接流程接线所需的过程实例/步骤 upsert。 */
    private static final class InMemoryTraceabilityRepository
            implements com.complex.platform.evidence.TraceabilityRepository {
        final Map<String, com.complex.platform.evidence.TraceabilityProcessInstanceRecord> instances =
                new LinkedHashMap<>();
        final Map<String, com.complex.platform.evidence.TraceabilityStepRecord> steps = new LinkedHashMap<>();
        final Map<String, com.complex.platform.evidence.TraceabilityResponsibilityRecord> responsibilities =
                new LinkedHashMap<>();

        @Override
        public com.complex.platform.evidence.TraceabilityProcessInstanceRecord saveProcessInstance(
                com.complex.platform.evidence.TraceabilityProcessInstanceRecord record) {
            instances.put(record.id(), record);
            return record;
        }

        @Override
        public Optional<com.complex.platform.evidence.TraceabilityProcessInstanceRecord> findProcessInstanceById(
                String id) {
            return Optional.ofNullable(instances.get(id));
        }

        @Override
        public List<com.complex.platform.evidence.TraceabilityProcessInstanceRecord> listProcessInstances(
                String merchantScope) {
            return new ArrayList<>(instances.values());
        }

        @Override
        public com.complex.platform.evidence.TraceabilityStepRecord saveStep(
                com.complex.platform.evidence.TraceabilityStepRecord record) {
            steps.put(record.id(), record);
            return record;
        }

        @Override
        public List<com.complex.platform.evidence.TraceabilityStepRecord> listStepsByInstance(
                String processInstanceId) {
            return steps.values().stream()
                    .filter(s -> processInstanceId.equals(s.processInstanceId()))
                    .toList();
        }

        @Override
        public com.complex.platform.evidence.TraceabilityResponsibilityRecord saveResponsibility(
                com.complex.platform.evidence.TraceabilityResponsibilityRecord record) {
            responsibilities.put(record.id(), record);
            return record;
        }

        @Override
        public Optional<com.complex.platform.evidence.TraceabilityResponsibilityRecord> findResponsibilityById(
                String id) {
            return Optional.ofNullable(responsibilities.get(id));
        }

        @Override
        public List<com.complex.platform.evidence.TraceabilityResponsibilityRecord> listResponsibilities(
                String merchantScope) {
            return new ArrayList<>(responsibilities.values());
        }
    }
}
