package com.complex.platform.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.complex.platform.inventory.dto.SavedBottleClaimRequest;
import com.complex.platform.inventory.dto.SavedBottleRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventoryServiceTests {

    private InMemoryInventoryRepository inventoryRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        inventoryRepository = new InMemoryInventoryRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        service = new InventoryService(
                inventoryRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository)
        );
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
                Instant.parse("2026-06-14T00:00:00Z")
        ));
        inventoryRepository.saveStockBalance(new StockBalanceRecord(
                "balance_out",
                outOfStockItem.id(),
                "warehouse_1",
                0,
                0,
                0,
                Instant.parse("2026-06-14T00:00:00Z")
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
                        org.assertj.core.groups.Tuple.tuple("safety_stock_low", "medium", lowStockItem.id()),
                        org.assertj.core.groups.Tuple.tuple("out_of_stock", "high", outOfStockItem.id()),
                        org.assertj.core.groups.Tuple.tuple("expiry_warning", "high", lowStockItem.id())
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
        private final List<InventoryRiskAlertRecord> riskAlerts = new ArrayList<>();
        private final Map<String, SupplierReconciliationRecord> supplierReconciliations = new LinkedHashMap<>();
        private final List<PurchaseRequestRecord> purchaseRequests = new ArrayList<>();
        private final List<StockMovementRecord> stockMovements = new ArrayList<>();
        private final List<StockAdjustmentRecord> stockAdjustments = new ArrayList<>();
        private final List<SavedBottleClaimRecord> savedBottleClaims = new ArrayList<>();
        private final List<TraceHandoverRecord> traceHandovers = new ArrayList<>();

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
        public PurchaseRequestRecord savePurchaseRequest(PurchaseRequestRecord request) {
            purchaseRequests.add(request);
            return request;
        }

        @Override
        public List<PurchaseRequestRecord> listPurchaseRequests() {
            return new ArrayList<>(purchaseRequests);
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
        public InventoryStockBatchRecord saveStockBatch(InventoryStockBatchRecord batch) {
            stockBatches.put(batch.id(), batch);
            return batch;
        }

        @Override
        public List<InventoryStockBatchRecord> listStockBatches() {
            return new ArrayList<>(stockBatches.values());
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
        public SupplierReconciliationRecord saveSupplierReconciliation(SupplierReconciliationRecord reconciliation) {
            supplierReconciliations.put(reconciliation.id(), reconciliation);
            return reconciliation;
        }

        @Override
        public List<SupplierReconciliationRecord> listSupplierReconciliations() {
            return new ArrayList<>(supplierReconciliations.values());
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

        @Override
        public List<SavedBottleRecord> listSavedBottles() {
            return new ArrayList<>(savedBottles.values());
        }

        @Override
        public SavedBottleClaimRecord saveSavedBottleClaim(SavedBottleClaimRecord claim) {
            savedBottleClaims.add(claim);
            return claim;
        }

        @Override
        public TraceHandoverRecord saveTraceHandover(TraceHandoverRecord handover) {
            traceHandovers.add(handover);
            return handover;
        }

        private String balanceKey(String itemId, String locationId) {
            return itemId + ":" + locationId;
        }
    }
}
