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
import com.complex.platform.inventory.dto.SupplyContractApproveRequest;
import com.complex.platform.inventory.dto.SupplyContractCreateRequest;
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
 * R5 T6 单测：供应合同三模式（建/审批流转/可采购范围卡控/隔离）+ 结构化库房 CRUD。
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
    private SupplyContractService contractService;
    private WarehouseService warehouseService;

    @BeforeEach
    void setUp() {
        repo = new InMemoryRepo();
        authorizationService = mock(AuthorizationService.class);
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        when(authorizationService.currentActor()).thenReturn(ADMIN_ACTOR);
        IdempotencyService idem =
                new IdempotencyService(new InMemoryIdem(), new ObjectMapper());
        AuditEventService audit = new AuditEventService(new InMemoryAudit());
        contractService = new SupplyContractService(repo, idem, audit, authorizationService);
        warehouseService = new WarehouseService(repo, idem, audit, authorizationService);
    }

    // ── 合同：三模式建合同 ──────────────────────────────────────────────────────
    @Test
    void createsContractsForAllThreeModes() {
        for (String mode : List.of(
                "platform_self_use", "merchant_self_purchase", "platform_purchase_distribution")) {
            String merchant = mode.equals("merchant_self_purchase") ? "m_a" : null;
            SupplyContractRecord contract = contractService.createSupplyContract(new SupplyContractCreateRequest(
                    "ck-" + mode, "supplier_1", "proj_1", merchant, mode,
                    "scope", "terms", 30, LocalDate.now(), LocalDate.now().plusYears(1),
                    "summary", List.of("item_1")));
            assertThat(contract.contractMode()).isEqualTo(mode);
            assertThat(contract.approvalStatus()).isEqualTo("draft");
            assertThat(contract.status()).isEqualTo("active");
        }
    }

    @Test
    void rejectsInvalidContractMode() {
        assertThatThrownBy(() -> contractService.createSupplyContract(new SupplyContractCreateRequest(
                "ck-bad", "supplier_1", "proj_1", null, "totally_invalid",
                null, null, null, null, null, null, List.of())))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("contract_mode");
    }

    @Test
    void merchantSelfPurchaseRequiresMerchant() {
        assertThatThrownBy(() -> contractService.createSupplyContract(new SupplyContractCreateRequest(
                "ck-nomerchant", "supplier_1", "proj_1", null, "merchant_self_purchase",
                null, null, null, null, null, null, List.of())))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("merchant_id");
    }

    // ── 合同：审批流转 ──────────────────────────────────────────────────────────
    @Test
    void approveTransitionsDraftToApproved() {
        SupplyContractRecord draft = createDraft("ck-appr", null, List.of("item_1"));
        SupplyContractRecord approved = contractService.approveSupplyContract(
                draft.id(), new SupplyContractApproveRequest("ak-1", "approved", "ok"));
        assertThat(approved.approvalStatus()).isEqualTo("approved");
    }

    @Test
    void approveCanReject() {
        SupplyContractRecord draft = createDraft("ck-rej", null, List.of());
        SupplyContractRecord rejected = contractService.approveSupplyContract(
                draft.id(), new SupplyContractApproveRequest("ak-2", "rejected", "no"));
        assertThat(rejected.approvalStatus()).isEqualTo("rejected");
    }

    @Test
    void approveRejectsInvalidDecision() {
        SupplyContractRecord draft = createDraft("ck-baddec", null, List.of());
        assertThatThrownBy(() -> contractService.approveSupplyContract(
                draft.id(), new SupplyContractApproveRequest("ak-3", "maybe", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("decision");
    }

    @Test
    void approveRejectsNonDraftContract() {
        SupplyContractRecord draft = createDraft("ck-twice", null, List.of());
        contractService.approveSupplyContract(
                draft.id(), new SupplyContractApproveRequest("ak-4a", "approved", null));
        assertThatThrownBy(() -> contractService.approveSupplyContract(
                draft.id(), new SupplyContractApproveRequest("ak-4b", "approved", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("draft");
    }

    // ── 合同：可采购范围卡控 ────────────────────────────────────────────────────
    @Test
    void purchasableScopeAllowsListedItem() {
        SupplyContractRecord draft = createDraft("ck-scope-ok", null, List.of("item_1", "item_2"));
        contractService.approveSupplyContract(
                draft.id(), new SupplyContractApproveRequest("ak-scope-ok", "approved", null));
        SupplyContractRecord ok = contractService.requirePurchasable(draft.id(), "item_1");
        assertThat(ok.id()).isEqualTo(draft.id());
    }

    @Test
    void purchasableScopeBlocksUnlistedItem() {
        SupplyContractRecord draft = createDraft("ck-scope-bad", null, List.of("item_1"));
        contractService.approveSupplyContract(
                draft.id(), new SupplyContractApproveRequest("ak-scope-bad", "approved", null));
        assertThatThrownBy(() -> contractService.requirePurchasable(draft.id(), "item_999"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("purchasable scope");
    }

    @Test
    void purchasableScopeBlocksUnapprovedContract() {
        SupplyContractRecord draft = createDraft("ck-scope-draft", null, List.of("item_1"));
        assertThatThrownBy(() -> contractService.requirePurchasable(draft.id(), "item_1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("approved");
    }

    @Test
    void purchasableScopeBlocksExpiredContract() {
        SupplyContractRecord draft = contractService.createSupplyContract(new SupplyContractCreateRequest(
                "ck-expired", "supplier_1", "proj_1", null, "platform_self_use",
                null, null, null, LocalDate.now().minusYears(2), LocalDate.now().minusYears(1),
                null, List.of("item_1")));
        contractService.approveSupplyContract(
                draft.id(), new SupplyContractApproveRequest("ak-expired", "approved", null));
        assertThatThrownBy(() -> contractService.requirePurchasable(draft.id(), "item_1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("expired");
    }

    // ── 合同：商户隔离 ──────────────────────────────────────────────────────────
    @Test
    void merchantScopeOnlySeesOwnContracts() {
        // 商户 A 视角建合同
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_A_ACTOR);
        contractService.createSupplyContract(new SupplyContractCreateRequest(
                "ck-iso-a", "supplier_1", "proj_1", null, "merchant_self_purchase",
                null, null, null, null, null, null, List.of()));
        assertThat(contractService.listSupplyContracts().items()).hasSize(1);

        // 商户 B 视角看不到 A 的合同
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_B_ACTOR);
        assertThat(contractService.listSupplyContracts().items()).isEmpty();
    }

    @Test
    void merchantScopeCannotApproveOtherMerchantContract() {
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_a"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_A_ACTOR);
        SupplyContractRecord draft = contractService.createSupplyContract(new SupplyContractCreateRequest(
                "ck-iso-appr", "supplier_1", "proj_1", null, "merchant_self_purchase",
                null, null, null, null, null, null, List.of()));

        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_B_ACTOR);
        assertThatThrownBy(() -> contractService.approveSupplyContract(
                draft.id(), new SupplyContractApproveRequest("ak-iso", "approved", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("scope");
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

    private SupplyContractRecord createDraft(String key, String merchant, List<String> scope) {
        return contractService.createSupplyContract(new SupplyContractCreateRequest(
                key, "supplier_1", "proj_1", merchant, "platform_self_use",
                null, null, null, LocalDate.now().minusDays(1), LocalDate.now().plusYears(1),
                null, scope));
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
        @Override
        public AuditEvent save(AuditEvent event) {
            return event;
        }
    }

    /** 仅实现 T6 服务用到的方法；其余抛 UnsupportedOperationException。 */
    private static final class InMemoryRepo implements InventoryRepository {
        private final Map<String, SupplyContractRecord> contracts = new LinkedHashMap<>();
        private final Map<String, WarehouseRecord> warehouses = new LinkedHashMap<>();

        @Override
        public SupplyContractRecord saveSupplyContract(SupplyContractRecord contract) {
            contracts.put(contract.id(), contract);
            return contract;
        }

        @Override
        public Optional<SupplyContractRecord> findSupplyContractByIdForUpdate(String id) {
            return Optional.ofNullable(contracts.get(id));
        }

        @Override
        public List<SupplyContractRecord> listSupplyContracts() {
            return new ArrayList<>(contracts.values());
        }

        @Override
        public List<SupplyContractRecord> listSupplyContractsByMerchant(String merchantId) {
            return contracts.values().stream().filter(c -> merchantId.equals(c.merchantId())).toList();
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
            return warehouses.values().stream().filter(w -> merchantId.equals(w.merchantId())).toList();
        }

        // ── 以下为接口其余方法：T6 服务测试不触达，统一抛出。 ──
        @Override public InventoryItemRecord saveInventoryItem(InventoryItemRecord item) { throw nyi(); }
        @Override public List<InventoryItemRecord> listInventoryItems() { throw nyi(); }
        @Override public Optional<InventoryItemRecord> findInventoryItemById(String id) { throw nyi(); }
        @Override public InventorySupplierRecord saveInventorySupplier(InventorySupplierRecord s) { throw nyi(); }
        @Override public List<InventorySupplierRecord> listInventorySuppliers() { throw nyi(); }
        @Override public PurchaseRequestRecord savePurchaseRequest(PurchaseRequestRecord r) { throw nyi(); }
        @Override public List<PurchaseRequestRecord> listPurchaseRequests() { throw nyi(); }
        @Override public Optional<StockBalanceRecord> findStockBalance(String i, String l) { throw nyi(); }
        @Override public StockBalanceRecord saveStockBalance(StockBalanceRecord b) { throw nyi(); }
        @Override public List<StockBalanceRecord> listStockBalances() { throw nyi(); }
        @Override public InventoryStockBatchRecord saveStockBatch(InventoryStockBatchRecord b) { throw nyi(); }
        @Override public List<InventoryStockBatchRecord> listStockBatches() { throw nyi(); }
        @Override public InventoryRiskAlertRecord saveRiskAlert(InventoryRiskAlertRecord a) { throw nyi(); }
        @Override public List<InventoryRiskAlertRecord> listRiskAlerts() { throw nyi(); }
        @Override public SupplierReconciliationRecord saveSupplierReconciliation(SupplierReconciliationRecord r) { throw nyi(); }
        @Override public List<SupplierReconciliationRecord> listSupplierReconciliations() { throw nyi(); }
        @Override public StockMovementRecord saveStockMovement(StockMovementRecord m) { throw nyi(); }
        @Override public List<StockMovementRecord> listStockMovements() { throw nyi(); }
        @Override public StockAdjustmentRecord saveStockAdjustment(StockAdjustmentRecord a) { throw nyi(); }
        @Override public SavedBottleRecord saveSavedBottle(SavedBottleRecord s) { throw nyi(); }
        @Override public Optional<SavedBottleRecord> findSavedBottleById(String id) { throw nyi(); }
        @Override public List<SavedBottleRecord> listSavedBottles() { throw nyi(); }
        @Override public SavedBottleClaimRecord saveSavedBottleClaim(SavedBottleClaimRecord c) { throw nyi(); }
        @Override public TraceHandoverRecord saveTraceHandover(TraceHandoverRecord h) { throw nyi(); }
        @Override public Optional<TraceHandoverRecord> findTraceHandoverByIdForUpdate(String id) { throw nyi(); }
        @Override public List<TraceHandoverStepRecord> listTraceHandoverSteps(String h) { throw nyi(); }
        @Override public Optional<TraceHandoverStepRecord> findTraceHandoverStep(String h, String s) { throw nyi(); }
        @Override public TraceHandoverStepRecord saveTraceHandoverStep(TraceHandoverStepRecord s) { throw nyi(); }
        @Override public TraceHandoverExceptionRecord saveTraceHandoverException(TraceHandoverExceptionRecord e) { throw nyi(); }
        @Override public Optional<TraceHandoverExceptionRecord> findTraceHandoverExceptionByIdForUpdate(String id) { throw nyi(); }
        @Override public void saveRobotTaskEvent(String id, String ref, String type, String h, String p, java.time.Instant t) { throw nyi(); }
        @Override public int atomicDecreaseStockBalance(String i, String l, int q) { throw nyi(); }
        @Override public void atomicIncreaseStockBalance(String i, String l, int q, String id, String m, String w) { throw nyi(); }
        @Override public Optional<SavedBottleRecord> findSavedBottleByIdForUpdate(String id) { throw nyi(); }
        @Override public List<InventoryItemRecord> listInventoryItemsByMerchant(String m) { throw nyi(); }
        @Override public List<InventorySupplierRecord> listInventorySuppliersByMerchant(String m) { throw nyi(); }
        @Override public List<PurchaseRequestRecord> listPurchaseRequestsByMerchant(String m) { throw nyi(); }
        @Override public List<StockBalanceRecord> listStockBalancesByMerchant(String m) { throw nyi(); }
        @Override public List<StockMovementRecord> listStockMovementsByMerchant(String m) { throw nyi(); }
        @Override public List<InventoryStockBatchRecord> listStockBatchesByMerchant(String m) { throw nyi(); }
        @Override public List<InventoryRiskAlertRecord> listRiskAlertsByMerchant(String m) { throw nyi(); }
        @Override public List<SupplierReconciliationRecord> listSupplierReconciliationsByMerchant(String m) { throw nyi(); }
        @Override public List<SavedBottleRecord> listSavedBottlesByMerchant(String m) { throw nyi(); }

        private static UnsupportedOperationException nyi() {
            return new UnsupportedOperationException("not needed for T6 service tests");
        }
    }
}
