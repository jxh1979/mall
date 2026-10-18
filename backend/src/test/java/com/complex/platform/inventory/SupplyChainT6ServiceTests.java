package com.complex.platform.inventory;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.inventory.dto.SupplierQualificationRequest;
import com.complex.platform.inventory.dto.SupplierSettlementMethodRequest;
import com.complex.platform.inventory.dto.WarehouseCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * T6：结构化库房、供应商资质和结算档；合同采购审批与账款由真实 JDBC 集成测试覆盖。
 * 用内存 repository 验证服务层语义；CHECK 拒非法枚举由 JDBC 集成测试覆盖。
 */
class SupplyChainT6ServiceTests {

    private static final AccessActor ADMIN_ACTOR =
            new AccessActor("usr_admin", "admin", "platform_admin", "org_platform", null, "active");
    private static final AccessActor MERCHANT_A_ACTOR =
            new AccessActor("usr_staff_a", "staff_a", "staff", "org_a", "m_a", "active");
    private static final AccessActor MERCHANT_B_ACTOR =
            new AccessActor("usr_staff_b", "staff_b", "staff", "org_b", "m_b", "active");

    private InMemoryRepo repo;
    private AuthorizationService authorizationService;
    private WarehouseService warehouseService;
    private SupplierQualificationService qualificationService;

    @BeforeEach
    void setUp() {
        repo = new InMemoryRepo();
        authorizationService = mock(AuthorizationService.class);
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        when(authorizationService.currentActor()).thenReturn(ADMIN_ACTOR);
        IdempotencyService idem =
                new IdempotencyService(new InMemoryIdem(), new ObjectMapper());
        var auditRepo = new InMemoryAudit();
        AuditEventService audit = new AuditEventService(auditRepo);
        warehouseService = new WarehouseService(repo, idem, audit, authorizationService);
        qualificationService = new SupplierQualificationService(repo, idem, audit, authorizationService);
        // 默认种子：供应商 supplier_1 + 一条 active 未过期资质（验证资质查验规则）。
        repo.saveInventorySupplier(new InventorySupplierRecord(
                "supplier_1", "供应商一", "general", null, null, "active",
                java.time.Instant.now(), java.time.Instant.now(), null));
        repo.saveSupplierQualification(new SupplierQualificationRecord(
                "supplier_qual_seed", "supplier_1", "business_license", "BL-1", null,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), List.of(), "active",
                java.time.Instant.now(), java.time.Instant.now(), null));
    }

    // ── 库房：CRUD ──────────────────────────────────────────────────────────────
    @Test
    void createsWarehouseOfEachType() {
        for (String type : List.of("main", "bar", "bar_counter", "consumable", "gift", "saved_bottle")) {
            WarehouseRecord wh = warehouseService.createWarehouse(new WarehouseCreateRequest(
                    "wk-" + type, "proj_1", null, type, "库房-" + type, null));
            assertThat(wh.warehouseType()).isEqualTo(type);
            assertThat(wh.status()).isEqualTo("active");
        }
        assertThat(warehouseService.listWarehouses().items()).hasSize(6);
    }

    @Test
    void rejectsInvalidWarehouseType() {
        assertThatThrownBy(() -> warehouseService.createWarehouse(new WarehouseCreateRequest(
                "wk-bad", "proj_1", null, "spaceship", "x", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("warehouse_type");
    }

    @Test
    void warehouseMerchantScopeIsolated() {
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_A_ACTOR);
        warehouseService.createWarehouse(new WarehouseCreateRequest(
                "wk-iso-a", "proj_1", null, "main", "A仓", null));

        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_B_ACTOR);
        assertThat(warehouseService.listWarehouses().items()).isEmpty();
    }

    // ── P1#18：供应商资质 CRUD + 进货查验过期卡控 ────────────────────────────────
    @Test
    void createsQualificationOfEachType() {
        for (String type : List.of(
                "business_license", "food_business_license", "production_license",
                "inspection_report", "hygiene_permit", "import_record", "other")) {
            SupplierQualificationRecord q = qualificationService.createQualification(new SupplierQualificationRequest(
                    "qk-" + type, "supplier_1", type, "NO-" + type, "机构",
                    LocalDate.now().minusMonths(1), LocalDate.now().plusYears(1), List.of("att_1"), null));
            assertThat(q.qualificationType()).isEqualTo(type);
            assertThat(q.status()).isEqualTo("active");
        }
        // 含 setUp 种子资质共 8 条。
        assertThat(qualificationService.listQualifications("supplier_1").items()).hasSize(8);
    }

    @Test
    void rejectsInvalidQualificationType() {
        assertThatThrownBy(() -> qualificationService.createQualification(new SupplierQualificationRequest(
                "qk-bad", "supplier_1", "spaceship_permit", null, null, null, null, List.of(), null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("qualification_type");
    }

    @Test
    void rejectsQualificationForUnknownSupplier() {
        assertThatThrownBy(() -> qualificationService.createQualification(new SupplierQualificationRequest(
                "qk-nosup", "supplier_404", "business_license", null, null, null, null, List.of(), null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("供应商不存在");
    }

    @Test
    void requireQualifiedBlocksWhenNoActiveQualification() {
        repo.saveInventorySupplier(new InventorySupplierRecord(
                "supplier_noqual", "无证商", "general", null, null, "active",
                java.time.Instant.now(), java.time.Instant.now(), null));
        assertThatThrownBy(() -> qualificationService.requireQualified("supplier_noqual"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("没有生效中的资质");
    }

    @Test
    void requireQualifiedBlocksFutureEffectiveQualification() {
        repo.saveInventorySupplier(new InventorySupplierRecord(
                "supplier_future", "未生效资质供应商", "general", null, null, "active",
                java.time.Instant.now(), java.time.Instant.now(), null));
        qualificationService.createQualification(new SupplierQualificationRequest(
                "qk-future", "supplier_future", "business_license", "BL-F", null,
                LocalDate.now(LocalZone.ID).plusDays(1), LocalDate.now().plusYears(1), List.of(), null));
        assertThatThrownBy(() -> qualificationService.requireQualified("supplier_future"))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.code()).isEqualTo("SUPPLIER_QUALIFICATION_NOT_YET_VALID"));
    }

    @Test
    void requireQualifiedBlocksWhenQualificationExpired() {
        repo.saveInventorySupplier(new InventorySupplierRecord(
                "supplier_expired", "过期商", "general", null, null, "active",
                java.time.Instant.now(), java.time.Instant.now(), null));
        qualificationService.createQualification(new SupplierQualificationRequest(
                "qk-exp", "supplier_expired", "business_license", "BL-X", null,
                LocalDate.now().minusYears(2), LocalDate.now().minusDays(1), List.of(), null));
        assertThatThrownBy(() -> qualificationService.requireQualified("supplier_expired"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("已过期");
    }

    // ── P1#18：结算多档/账期 CRUD ─────────────────────────────────────────────────
    @Test
    void createsSettlementMethodsMultipleTiersWithDefault() {
        qualificationService.createSettlementMethod(new SupplierSettlementMethodRequest(
                "sk-monthly", "supplier_1", "monthly", 30, "spirit", true, null));
        qualificationService.createSettlementMethod(new SupplierSettlementMethodRequest(
                "sk-cod", "supplier_1", "cash_on_delivery", 0, "beverage", false, null));
        qualificationService.createSettlementMethod(new SupplierSettlementMethodRequest(
                "sk-credit", "supplier_1", "credit_term", 60, null, false, null));
        List<SupplierSettlementMethodRecord> tiers =
                qualificationService.listSettlementMethods("supplier_1").items();
        assertThat(tiers).hasSize(3);
        assertThat(tiers).filteredOn(SupplierSettlementMethodRecord::isDefault).hasSize(1);
        assertThat(tiers).extracting(SupplierSettlementMethodRecord::paymentTermDays)
                .contains(30, 0, 60);
    }

    @Test
    void rejectsInvalidSettlementMethod() {
        assertThatThrownBy(() -> qualificationService.createSettlementMethod(new SupplierSettlementMethodRequest(
                "sk-bad", "supplier_1", "barter", 10, null, false, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("settlement_method");
    }

    @Test
    void rejectsNegativePaymentTermDays() {
        assertThatThrownBy(() -> qualificationService.createSettlementMethod(new SupplierSettlementMethodRequest(
                "sk-neg", "supplier_1", "monthly", -5, null, false, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("payment_term_days");
    }

    // ── 内存测试替身 ────────────────────────────────────────────────────────────
    private static final class InMemoryIdem implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String op, String key) {
            return Optional.ofNullable(records.get(op + ":" + key));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String k = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(k)) {
                return false;
            }
            records.put(k, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class InMemoryAudit implements AuditEventRepository {
        final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }

    /** 仅实现 T6 服务用到的方法；其余抛 UnsupportedOperationException。 */
    private static final class InMemoryRepo implements InventoryRepository {
        private final Map<String, WarehouseRecord> warehouses = new LinkedHashMap<>();
        final Map<String, InventorySupplierRecord> suppliers = new LinkedHashMap<>();
        final Map<String, SupplierQualificationRecord> qualifications = new LinkedHashMap<>();
        final Map<String, SupplierSettlementMethodRecord> settlementMethods = new LinkedHashMap<>();

        @Override
        public SupplyContractRecord saveSupplyContract(SupplyContractRecord contract) { throw nyi(); }

        @Override
        public Optional<SupplyContractRecord> findSupplyContractByIdForUpdate(String id) { throw nyi(); }

        @Override
        public List<SupplyContractRecord> listSupplyContracts() { throw nyi(); }

        @Override
        public List<SupplyContractRecord> listSupplyContractsByMerchant(String merchantId) { throw nyi(); }

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
            return warehouses.values().stream().filter(w -> merchantId.equals(w.merchantId())).toList();
        }

        // ── 以下为接口其余方法：T6 服务测试不触达，统一抛出。 ──
        @Override public InventoryItemRecord saveInventoryItem(InventoryItemRecord item) { throw nyi(); }
        @Override public List<InventoryItemRecord> listInventoryItems() { throw nyi(); }
        @Override public Optional<InventoryItemRecord> findInventoryItemById(String id) { throw nyi(); }
        @Override public InventorySupplierRecord saveInventorySupplier(InventorySupplierRecord s) {
            suppliers.put(s.id(), s);
            return s;
        }
        @Override public List<InventorySupplierRecord> listInventorySuppliers() {
            return new ArrayList<>(suppliers.values());
        }
        @Override public PurchaseRequestRecord savePurchaseRequest(PurchaseRequestRecord r) { throw nyi(); }
        @Override public List<PurchaseRequestRecord> listPurchaseRequests() { throw nyi(); }
        @Override public Optional<PurchaseRequestRecord> findPurchaseRequestByIdForUpdate(String id) { throw nyi(); }
        @Override public PurchaseRequestEventRecord savePurchaseRequestEvent(PurchaseRequestEventRecord e) { throw nyi(); }

        @Override public Optional<StockBalanceRecord> findStockBalance(String i, String l) { throw nyi(); }
        @Override public int sumStockBalanceByItemAndWarehouse(String i, String w) { throw nyi(); }
        @Override public StockBalanceRecord saveStockBalance(StockBalanceRecord b) { throw nyi(); }
        @Override public List<StockBalanceRecord> listStockBalances() { throw nyi(); }
        @Override public InventoryStockBatchRecord saveStockBatch(InventoryStockBatchRecord b, String projectId) { throw nyi(); }
        @Override public List<InventoryStockBatchRecord> listActiveBatchesForConsumption(String i, String l) { throw nyi(); }
        @Override public int atomicDecreaseBatchQuantity(String b, int q) { throw nyi(); }
        @Override public List<InventoryStockBatchRecord> listStockBatches() { throw nyi(); }
        @Override public InventoryRiskAlertRecord saveRiskAlert(InventoryRiskAlertRecord a) { throw nyi(); }
        @Override public List<InventoryRiskAlertRecord> listRiskAlerts() { throw nyi(); }
        @Override public SupplierReconciliationRecord saveSupplierReconciliation(SupplierReconciliationRecord r) { throw nyi(); }
        @Override public List<SupplierReconciliationRecord> listSupplierReconciliations() { throw nyi(); }
        @Override public StockMovementRecord saveStockMovement(StockMovementRecord m, String projectId) { throw nyi(); }
        @Override public List<StockMovementRecord> listStockMovements() { throw nyi(); }
        @Override public StockAdjustmentRecord saveStockAdjustment(StockAdjustmentRecord a) { throw nyi(); }
        @Override public Optional<StockAdjustmentRecord> findStockAdjustmentByIdForUpdate(String id) { throw nyi(); }
        @Override public SavedBottleRecord saveSavedBottle(SavedBottleRecord s) { throw nyi(); }
        @Override public List<SavedBottleRecord> listSavedBottles() { throw nyi(); }
        @Override public SavedBottleClaimRecord saveSavedBottleClaim(SavedBottleClaimRecord c) { throw nyi(); }
        @Override public SavedBottleTransferRecord saveSavedBottleTransfer(SavedBottleTransferRecord t) { throw nyi(); }
        @Override public TraceHandoverRecord saveTraceHandover(TraceHandoverRecord h) { throw nyi(); }
        @Override public Optional<TraceHandoverRecord> findTraceHandoverByIdForUpdate(String id) { throw nyi(); }
        @Override public List<TraceHandoverStepRecord> listTraceHandoverSteps(String h) { throw nyi(); }
        @Override public Optional<TraceHandoverStepRecord> findTraceHandoverStep(String h, String s) { throw nyi(); }
        @Override public TraceHandoverStepRecord saveTraceHandoverStep(TraceHandoverStepRecord s) { throw nyi(); }
        @Override public TraceHandoverExceptionRecord saveTraceHandoverException(TraceHandoverExceptionRecord e) { throw nyi(); }
        @Override public Optional<TraceHandoverExceptionRecord> findTraceHandoverExceptionByIdForUpdate(String id) { throw nyi(); }
        @Override public void saveRobotTaskEvent(String id, String ref, String type, String h, String p, java.time.Instant t) { throw nyi(); }
        @Override public int atomicDecreaseStockBalance(String i, String l, int q, String scope) { throw nyi(); }
        @Override public int reserveStockBalance(String i, String l, int q) { throw nyi(); }
        @Override public int releaseStockReservation(String i, String l, int q) { throw nyi(); }
        @Override public void atomicIncreaseStockBalance(String i, String l, int q, String id, String m, String w, String p) { throw nyi(); }
        @Override public Optional<SavedBottleRecord> findSavedBottleByIdForUpdate(String id) { throw nyi(); }
        @Override public List<InventoryItemRecord> listInventoryItemsByMerchant(String m) { throw nyi(); }
        @Override public List<InventorySupplierRecord> listInventorySuppliersByMerchant(String m) {
            return suppliers.values().stream().filter(s -> m.equals(s.merchantId())).toList();
        }
        @Override public List<PurchaseRequestRecord> listPurchaseRequestsByMerchant(String m) { throw nyi(); }
        @Override public List<StockBalanceRecord> listStockBalancesByMerchant(String m) { throw nyi(); }
        @Override public List<StockBalanceRecord> listStockBalancesByProject(String p) { throw nyi(); }
        @Override public com.complex.platform.common.api.PageResponse<StockMovementRecord> listStockMovementsPaged(String m, String p, int page, int size) { throw nyi(); }
        @Override public List<InventoryStockBatchRecord> listStockBatchesByMerchant(String m) { throw nyi(); }
        @Override public List<InventoryStockBatchRecord> listStockBatchesByProject(String p) { throw nyi(); }
        @Override public List<InventoryRiskAlertRecord> listRiskAlertsByMerchant(String m) { throw nyi(); }
        @Override public List<SupplierReconciliationRecord> listSupplierReconciliationsByMerchant(String m) { throw nyi(); }
        @Override public List<SavedBottleRecord> listSavedBottlesByMerchant(String m) { throw nyi(); }
        @Override public InventoryHighValueReviewRecord saveHighValueReview(InventoryHighValueReviewRecord r) { throw nyi(); }
        @Override public Optional<InventoryHighValueReviewRecord> findHighValueReviewByIdForUpdate(String id) { throw nyi(); }
        @Override public List<InventoryHighValueReviewRecord> listHighValueReviews() { throw nyi(); }
        @Override public SupplierQualificationRecord saveSupplierQualification(SupplierQualificationRecord q) {
            qualifications.put(q.id(), q);
            return q;
        }
        @Override public List<SupplierQualificationRecord> listSupplierQualificationsBySupplier(String s) {
            return qualifications.values().stream().filter(q -> s.equals(q.supplierId())).toList();
        }
        @Override public List<SupplierQualificationRecord> listSupplierQualificationsByMerchant(String m) {
            return qualifications.values().stream().filter(q -> m.equals(q.merchantId())).toList();
        }
        @Override public List<SupplierQualificationRecord> listSupplierQualifications() {
            return new ArrayList<>(qualifications.values());
        }
        @Override public SupplierSettlementMethodRecord saveSupplierSettlementMethod(SupplierSettlementMethodRecord m) {
            settlementMethods.put(m.id(), m);
            return m;
        }
        @Override public List<SupplierSettlementMethodRecord> listSupplierSettlementMethodsBySupplier(String s) {
            return settlementMethods.values().stream().filter(m -> s.equals(m.supplierId())).toList();
        }
        @Override public List<SupplierSettlementMethodRecord> listSupplierSettlementMethodsByMerchant(String m) {
            return settlementMethods.values().stream().filter(x -> m.equals(x.merchantId())).toList();
        }
        @Override public List<SupplierSettlementMethodRecord> listSupplierSettlementMethods() {
            return new ArrayList<>(settlementMethods.values());
        }

        // ── V108/V126：盘点单 + 库存差异明细写入侧（T6 测试不直接覆盖，未用方法抛 nyi）。 ──
        @Override public StocktakeRecord saveStocktake(StocktakeRecord stocktake) {
            throw nyi();
        }
        @Override public boolean hasActiveStocktakeForWarehouse(String warehouseId) {
            return false;
        }
        @Override public Optional<StocktakeRecord> findStocktakeById(String id) {
            throw nyi();
        }
        @Override public List<StocktakeRecord> listStocktakes() {
            throw nyi();
        }
        @Override public List<StocktakeRecord> listStocktakesByMerchant(String merchantId) {
            throw nyi();
        }
        @Override public InventoryVarianceRecord saveInventoryVariance(InventoryVarianceRecord variance) {
            throw nyi();
        }
        @Override public Optional<InventoryVarianceRecord> findInventoryVarianceByIdForUpdate(String id) {
            throw nyi();
        }
        @Override public List<InventoryVarianceRecord> listInventoryVariancesByStocktake(String stocktakeId) {
            throw nyi();
        }
        @Override public int reviewInventoryVarianceIfPending(
                String varianceId, String decision, String reviewedBy, java.time.Instant reviewedAt) {
            throw nyi();
        }
        @Override public InventoryTraceCodeRecord saveTraceCode(InventoryTraceCodeRecord traceCode) {
            throw nyi();
        }
        @Override public Optional<InventoryTraceCodeRecord> findTraceCodeById(String id) {
            throw nyi();
        }
        @Override public Optional<InventoryTraceCodeRecord> findTraceCodeByCode(String traceCode) {
            throw nyi();
        }
        @Override public List<InventoryTraceCodeRecord> listTraceCodes() {
            throw nyi();
        }
        @Override public List<InventoryTraceCodeRecord> listTraceCodesByMerchant(String merchantId) {
            throw nyi();
        }
        @Override public int printTraceCodeIfRegistered(String id, java.time.Instant printedAt, String boundRef) {
            throw nyi();
        }

        private static UnsupportedOperationException nyi() {
            return new UnsupportedOperationException("not needed for T6 service tests");
        }
    }
}
