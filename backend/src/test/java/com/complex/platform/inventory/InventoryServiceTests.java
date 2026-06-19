package com.complex.platform.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
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
import com.complex.platform.inventory.dto.InventoryItemRequest;
import com.complex.platform.inventory.dto.InventorySupplierRequest;
import com.complex.platform.inventory.dto.InventoryRiskScanRequest;
import com.complex.platform.inventory.dto.InventoryStockBatchRequest;
import com.complex.platform.inventory.dto.PurchaseRequestCreate;
import com.complex.platform.inventory.dto.SavedBottleCancelRequest;
import com.complex.platform.inventory.dto.SavedBottleClaimRequest;
import com.complex.platform.inventory.dto.SavedBottleRequest;
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
        service = new InventoryService(
                inventoryRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                authorizationService,
                terminalSwitchGuard
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
                "active"
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
                Instant.now().plusSeconds(3600)
        ));

        assertThatThrownBy(() -> service.claimSavedBottle(savedBottle.id(), new SavedBottleClaimRequest(
                "claim-too-much",
                null,
                List.of(),
                List.of(Map.of("quantity", 2)),
                "13800138000",
                "123456"
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
                Instant.now().plusSeconds(3600)
        ));
        SavedBottleClaimRequest firstClaim = new SavedBottleClaimRequest(
                "claim-once",
                null,
                List.of(),
                List.of(Map.of("quantity", 1)),
                "13800138000",
                "123456"
        );

        SavedBottleClaimRecord first = service.claimSavedBottle(savedBottle.id(), firstClaim);
        SavedBottleClaimRecord second = service.claimSavedBottle(savedBottle.id(), firstClaim);
        SavedBottleClaimRecord finalClaim = service.claimSavedBottle(savedBottle.id(), new SavedBottleClaimRequest(
                "claim-final",
                null,
                List.of(),
                List.of(Map.of("quantity", 1)),
                "13800138000",
                "123456"
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
                LocalDate.parse("2026-06-20")
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
                "active"
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
                "active"
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
                "LABEL-XYZ", "张三", "13800138000", true, 100000L));

        SavedBottleClaimRecord review = service.claimSavedBottle(savedBottle.id(), new SavedBottleClaimRequest(
                "claim-mismatch", null, List.of(), List.of(Map.of("quantity", 1)), "13800138000", "WRONG-LABEL"));

        assertThat(review.status()).isEqualTo("pending_identity_review");
        assertThat(inventoryRepository.findSavedBottleById(savedBottle.id()))
                .get()
                .extracting(SavedBottleRecord::status, SavedBottleRecord::remainingQuantity)
                .containsExactly("pending_identity_review", 2);
    }

    // A1/REQ-32: 高价值存酒作废必须照片+视频双证据
    @Test
    void highValueSavedBottleCancelRequiresDoubleEvidence() {
        InventoryItemRecord item = createItem("hv-cancel");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-hv", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Rare", "unit", "bottle")),
                Instant.now().plusSeconds(3600), null, null, null, true, 500000L));

        assertThatThrownBy(() -> service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-1", null, List.of("att_photo"), "broken")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("HIGH_VALUE_EVIDENCE_REQUIRED"));

        SavedBottleRecord cancelled = service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-2", null, List.of("att_photo", "att_video"), "broken"));
        assertThat(cancelled.status()).isEqualTo("cancelled");
    }

    // C4 §3 路径B: 文案与实现对齐 —— 附件<2 抛错，message 含"附件数≥2"且不含"supervisor review"
    @Test
    void highValueVoidWithFewerThanTwoAttachmentsThrowsWithAttachmentWording() {
        InventoryItemRecord item = createItem("hv-wording");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-hv-wording", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Rare", "unit", "bottle")),
                Instant.now().plusSeconds(3600), null, null, null, true, 500000L));

        assertThatThrownBy(() -> service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-wording", null, List.of("att_photo"), "broken")))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo("HIGH_VALUE_EVIDENCE_REQUIRED");
                    assertThat(e.getMessage()).contains("附件数≥2");
                    assertThat(e.getMessage()).doesNotContain("supervisor review");
                });
    }

    // C4 §3 路径B: 附件=2 作废成功（行为不变，回归保护）
    @Test
    void highValueVoidWithTwoAttachmentsSucceeds() {
        InventoryItemRecord item = createItem("hv-two");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-hv-two", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "Rare", "unit", "bottle")),
                Instant.now().plusSeconds(3600), null, null, null, true, 500000L));

        SavedBottleRecord cancelled = service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-hv-two", null, List.of("att_photo", "att_video"), "broken"));
        assertThat(cancelled.status()).isEqualTo("cancelled");
    }

    // C4 §3 路径B: 非高价值无附件可作废（回归保护）
    @Test
    void nonHighValueVoidIgnoresAttachmentRule() {
        InventoryItemRecord item = createItem("non-hv");
        SavedBottleRecord bottle = service.createSavedBottle(new SavedBottleRequest(
                "saved-non-hv", null, List.of(), "customer_1", "bar_1",
                List.of(Map.of("item_id", item.id(), "quantity", 1, "bottle_name", "House", "unit", "bottle")),
                Instant.now().plusSeconds(3600), null, null, null, false, 1000L));

        SavedBottleRecord cancelled = service.cancelSavedBottle(bottle.id(), new SavedBottleCancelRequest(
                "cancel-non-hv", null, List.of(), "broken"));
        assertThat(cancelled.status()).isEqualTo("cancelled");
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
                Instant.now().plusSeconds(3600)
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
                "inject via adjustment"
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
                "active"
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
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"), "m_a"
        ));
        // 商户 B 的数据
        inventoryRepository.saveInventoryItem(new InventoryItemRecord(
                "item_b", null, "Item B", "spirit", "bottle", null, false, 0, "active",
                Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z"), "m_b"
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
                "active"
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

        @Override
        public PurchaseRequestRecord savePurchaseRequest(PurchaseRequestRecord request) {
            purchaseRequests.add(request);
            return request;
        }

        @Override
        public List<PurchaseRequestRecord> listPurchaseRequests() {
            return new ArrayList<>(purchaseRequests);
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
            stockAdjustments.add(adjustment);
            return adjustment;
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

        private String balanceKey(String itemId, String locationId) {
            return itemId + ":" + locationId;
        }
    }
}
