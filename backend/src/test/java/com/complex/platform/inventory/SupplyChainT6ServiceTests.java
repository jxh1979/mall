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
import com.complex.platform.inventory.dto.SupplierQualificationRequest;
import com.complex.platform.inventory.dto.SupplierSettlementMethodRequest;
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
    private PurchaseRequestService purchaseRequestService;
    private SupplierQualificationService qualificationService;
    private InMemoryAudit auditRepo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryRepo();
        authorizationService = mock(AuthorizationService.class);
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        when(authorizationService.currentActor()).thenReturn(ADMIN_ACTOR);
        IdempotencyService idem =
                new IdempotencyService(new InMemoryIdem(), new ObjectMapper());
        auditRepo = new InMemoryAudit();
        AuditEventService audit = new AuditEventService(auditRepo);
        contractService = new SupplyContractService(repo, idem, audit, authorizationService);
        warehouseService = new WarehouseService(repo, idem, audit, authorizationService);
        qualificationService = new SupplierQualificationService(repo, idem, audit, authorizationService);
        purchaseRequestService = new PurchaseRequestService(
                repo, idem, audit, authorizationService, contractService, qualificationService);
        // 默认种子：供应商 supplier_1 + 一条 active 未过期资质（让既有采购审批流转测试不受进货查验卡控影响）。
        repo.saveInventorySupplier(new InventorySupplierRecord(
                "supplier_1", "供应商一", "general", null, null, "active",
                java.time.Instant.now(), java.time.Instant.now(), null));
        repo.saveSupplierQualification(new SupplierQualificationRecord(
                "supplier_qual_seed", "supplier_1", "business_license", "BL-1", null,
                LocalDate.now().minusYears(1), LocalDate.now().plusYears(1), List.of(), "active",
                java.time.Instant.now(), java.time.Instant.now(), null));
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

    // ── 采购周期状态机 ──────────────────────────────────────────────────────────
    @Test
    void purchaseFullFlowTransitionsStepByStepAndLeavesEvents() {
        seedPurchase("pr_flow", "item_1", null, null);
        purchaseRequestService.approve("pr_flow",
                new com.complex.platform.inventory.dto.PurchaseRequestApproveRequest("a1", "approved", null, "ok"));
        assertThat(repo.purchaseRequests.get("pr_flow").status()).isEqualTo("approved");
        assertThat(repo.purchaseRequests.get("pr_flow").approvedBy()).isEqualTo("usr_admin");

        purchaseRequestService.order("pr_flow",
                new com.complex.platform.inventory.dto.PurchaseRequestTransitionRequest("a2", null));
        assertThat(repo.purchaseRequests.get("pr_flow").status()).isEqualTo("ordered");
        assertThat(repo.purchaseRequests.get("pr_flow").orderedAt()).isNotNull();

        purchaseRequestService.arrive("pr_flow",
                new com.complex.platform.inventory.dto.PurchaseRequestTransitionRequest("a3", null));
        assertThat(repo.purchaseRequests.get("pr_flow").status()).isEqualTo("arrived");
        assertThat(repo.purchaseRequests.get("pr_flow").actualArrivalDate()).isNotNull();

        purchaseRequestService.receive("pr_flow",
                new com.complex.platform.inventory.dto.PurchaseRequestReceiveRequest(
                        "a4", "loc_1", "wh_1", List.of("photo_1"), null));
        PurchaseRequestRecord received = repo.purchaseRequests.get("pr_flow");
        assertThat(received.status()).isEqualTo("received");
        assertThat(received.receivedBy()).isEqualTo("usr_admin");
        assertThat(received.receivedAt()).isNotNull();

        // 每步留事件（含 received）。
        assertThat(repo.listPurchaseRequestEvents("pr_flow"))
                .extracting(PurchaseRequestEventRecord::toStatus)
                .containsExactly("approved", "ordered", "arrived", "received");
        assertThat(auditRepo.events).extracting(AuditEvent::eventName)
                .contains("inventory.purchase_status_changed", "inventory.received");
    }

    @Test
    void receiveGeneratesInboundMovementToWarehouse() {
        seedPurchase("pr_recv", "item_1", null, null);
        purchaseRequestService.approve("pr_recv",
                new com.complex.platform.inventory.dto.PurchaseRequestApproveRequest("r1", "approved", null, null));
        purchaseRequestService.order("pr_recv",
                new com.complex.platform.inventory.dto.PurchaseRequestTransitionRequest("r2", null));
        purchaseRequestService.arrive("pr_recv",
                new com.complex.platform.inventory.dto.PurchaseRequestTransitionRequest("r3", null));
        purchaseRequestService.receive("pr_recv",
                new com.complex.platform.inventory.dto.PurchaseRequestReceiveRequest(
                        "r4", "loc_main", "wh_main", List.of(), null));

        // 入库流水 inbound，关联采购单，落库位 + 数量。
        assertThat(repo.stockMovements)
                .anySatisfy(m -> {
                    assertThat(m.movementType()).isEqualTo("inbound");
                    assertThat(m.businessId()).isEqualTo("pr_recv");
                    assertThat(m.targetLocationId()).isEqualTo("loc_main");
                    assertThat(m.quantity()).isEqualTo(7);
                });
        // 余额挂结构化库房 warehouse_id。
        assertThat(repo.findStockBalance("item_1", "loc_main"))
                .get()
                .extracting(StockBalanceRecord::availableQuantity, StockBalanceRecord::warehouseId)
                .containsExactly(7, "wh_main");
    }

    @Test
    void illegalStepTransitionsAreRejected() {
        seedPurchase("pr_bad", "item_1", null, null);
        // created → order 非法（须先 approve）。
        assertThatThrownBy(() -> purchaseRequestService.order("pr_bad",
                new com.complex.platform.inventory.dto.PurchaseRequestTransitionRequest("b1", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot transition");
        // created → receive 非法。
        assertThatThrownBy(() -> purchaseRequestService.receive("pr_bad",
                new com.complex.platform.inventory.dto.PurchaseRequestReceiveRequest("b2", "loc", null, List.of(), null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot transition");
        // rejected 后不能 order。
        purchaseRequestService.approve("pr_bad",
                new com.complex.platform.inventory.dto.PurchaseRequestApproveRequest("b3", "rejected", null, null));
        assertThatThrownBy(() -> purchaseRequestService.order("pr_bad",
                new com.complex.platform.inventory.dto.PurchaseRequestTransitionRequest("b4", null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("cannot transition");
    }

    @Test
    void approveEnforcesContractPurchasableScope() {
        SupplyContractRecord draft = createDraft("ck-pr-scope", null, List.of("item_1"));
        contractService.approveSupplyContract(
                draft.id(), new SupplyContractApproveRequest("ck-pr-scope-appr", "approved", null));
        // 采购引用合同但物料超出 purchasable_scope → approve 被卡。
        seedPurchase("pr_scope_bad", "item_999", null, draft.id());
        assertThatThrownBy(() -> purchaseRequestService.approve("pr_scope_bad",
                new com.complex.platform.inventory.dto.PurchaseRequestApproveRequest("s1", "approved", null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("purchasable scope");
        // 物料在 scope 内 → 放行。
        seedPurchase("pr_scope_ok", "item_1", null, draft.id());
        PurchaseRequestRecord ok = purchaseRequestService.approve("pr_scope_ok",
                new com.complex.platform.inventory.dto.PurchaseRequestApproveRequest("s2", "approved", null, null));
        assertThat(ok.status()).isEqualTo("approved");
        assertThat(ok.supplyContractId()).isEqualTo(draft.id());
    }

    @Test
    void merchantScopeCannotTransitionOtherMerchantPurchase() {
        seedPurchase("pr_iso", "item_1", "m_a", null);
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("m_b"));
        when(authorizationService.currentActor()).thenReturn(MERCHANT_B_ACTOR);
        assertThatThrownBy(() -> purchaseRequestService.approve("pr_iso",
                new com.complex.platform.inventory.dto.PurchaseRequestApproveRequest("i1", "approved", null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("scope");
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
                .hasMessageContaining("supplier");
    }

    @Test
    void requireQualifiedBlocksWhenNoActiveQualification() {
        repo.saveInventorySupplier(new InventorySupplierRecord(
                "supplier_noqual", "无证商", "general", null, null, "active",
                java.time.Instant.now(), java.time.Instant.now(), null));
        assertThatThrownBy(() -> qualificationService.requireQualified("supplier_noqual"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no active qualification");
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
                .hasMessageContaining("expired");
    }

    @Test
    void approveBlocksPurchaseWhenSupplierQualificationExpired() {
        repo.saveInventorySupplier(new InventorySupplierRecord(
                "supplier_exp2", "过期商2", "general", null, null, "active",
                java.time.Instant.now(), java.time.Instant.now(), null));
        qualificationService.createQualification(new SupplierQualificationRequest(
                "qk-exp2", "supplier_exp2", "food_business_license", "FB-X", null,
                LocalDate.now().minusYears(2), LocalDate.now().minusDays(3), List.of(), null));
        repo.savePurchaseRequest(new PurchaseRequestRecord(
                "pr_qual_bad", "PR-qual", "supplier_exp2", "item_1", 5, LocalDate.now(),
                "created", List.of(), java.time.Instant.now(), java.time.Instant.now(), null,
                null, null, null, null, null, null));
        assertThatThrownBy(() -> purchaseRequestService.approve("pr_qual_bad",
                new com.complex.platform.inventory.dto.PurchaseRequestApproveRequest("q1", "approved", null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("expired");
        // rejected 决议不触发进货查验卡控（仍可驳回过期供应商采购）。
        PurchaseRequestRecord rejected = purchaseRequestService.approve("pr_qual_bad",
                new com.complex.platform.inventory.dto.PurchaseRequestApproveRequest("q2", "rejected", null, null));
        assertThat(rejected.status()).isEqualTo("rejected");
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

    private void seedPurchase(String id, String itemId, String merchantId, String contractId) {
        repo.savePurchaseRequest(new PurchaseRequestRecord(
                id, "PR-" + id, "supplier_1", itemId, 7, LocalDate.now(),
                "created", List.of(), java.time.Instant.now(), java.time.Instant.now(), merchantId,
                contractId, null, null, null, null, null));
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
        final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }

    /** 仅实现 T6 服务用到的方法；其余抛 UnsupportedOperationException。 */
    private static final class InMemoryRepo implements InventoryRepository {
        private final Map<String, SupplyContractRecord> contracts = new LinkedHashMap<>();
        private final Map<String, WarehouseRecord> warehouses = new LinkedHashMap<>();
        final Map<String, PurchaseRequestRecord> purchaseRequests = new LinkedHashMap<>();
        final List<PurchaseRequestEventRecord> purchaseRequestEvents = new ArrayList<>();
        final Map<String, StockBalanceRecord> balances = new LinkedHashMap<>();
        final List<StockMovementRecord> stockMovements = new ArrayList<>();
        final Map<String, InventorySupplierRecord> suppliers = new LinkedHashMap<>();
        final Map<String, SupplierQualificationRecord> qualifications = new LinkedHashMap<>();
        final Map<String, SupplierSettlementMethodRecord> settlementMethods = new LinkedHashMap<>();

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
        @Override public InventorySupplierRecord saveInventorySupplier(InventorySupplierRecord s) {
            suppliers.put(s.id(), s);
            return s;
        }
        @Override public List<InventorySupplierRecord> listInventorySuppliers() {
            return new ArrayList<>(suppliers.values());
        }
        @Override public PurchaseRequestRecord savePurchaseRequest(PurchaseRequestRecord r) {
            purchaseRequests.put(r.id(), r);
            return r;
        }
        @Override public List<PurchaseRequestRecord> listPurchaseRequests() {
            return new ArrayList<>(purchaseRequests.values());
        }
        @Override public Optional<PurchaseRequestRecord> findPurchaseRequestByIdForUpdate(String id) {
            return Optional.ofNullable(purchaseRequests.get(id));
        }
        @Override public PurchaseRequestEventRecord savePurchaseRequestEvent(PurchaseRequestEventRecord e) {
            purchaseRequestEvents.add(e);
            return e;
        }
        @Override public List<PurchaseRequestEventRecord> listPurchaseRequestEvents(String requestId) {
            return purchaseRequestEvents.stream().filter(e -> requestId.equals(e.requestId())).toList();
        }
        @Override public Optional<StockBalanceRecord> findStockBalance(String i, String l) {
            return Optional.ofNullable(balances.get(i + ":" + l));
        }
        @Override public int sumStockBalanceByItemAndWarehouse(String i, String w) {
            return balances.values().stream()
                    .filter(b -> i.equals(b.itemId()) && w.equals(b.warehouseId()))
                    .mapToInt(StockBalanceRecord::quantity).sum();
        }
        @Override public StockBalanceRecord saveStockBalance(StockBalanceRecord b) {
            balances.put(b.itemId() + ":" + b.locationId(), b);
            return b;
        }
        @Override public List<StockBalanceRecord> listStockBalances() {
            return new ArrayList<>(balances.values());
        }
        @Override public InventoryStockBatchRecord saveStockBatch(InventoryStockBatchRecord b, String projectId) { return b; }
        @Override public List<InventoryStockBatchRecord> listActiveBatchesForConsumption(String i, String l) { return List.of(); }
        @Override public int atomicDecreaseBatchQuantity(String b, int q) { return 0; }
        @Override public List<InventoryStockBatchRecord> listStockBatches() { throw nyi(); }
        @Override public InventoryRiskAlertRecord saveRiskAlert(InventoryRiskAlertRecord a) { throw nyi(); }
        @Override public List<InventoryRiskAlertRecord> listRiskAlerts() { throw nyi(); }
        @Override public SupplierReconciliationRecord saveSupplierReconciliation(SupplierReconciliationRecord r) { throw nyi(); }
        @Override public List<SupplierReconciliationRecord> listSupplierReconciliations() { throw nyi(); }
        @Override public StockMovementRecord saveStockMovement(StockMovementRecord m, String projectId) {
            stockMovements.add(m);
            return m;
        }
        @Override public List<StockMovementRecord> listStockMovements() {
            return new ArrayList<>(stockMovements);
        }
        @Override public StockAdjustmentRecord saveStockAdjustment(StockAdjustmentRecord a) { throw nyi(); }
        @Override public Optional<StockAdjustmentRecord> findStockAdjustmentByIdForUpdate(String id) { throw nyi(); }
        @Override public SavedBottleRecord saveSavedBottle(SavedBottleRecord s) { throw nyi(); }
        @Override public Optional<SavedBottleRecord> findSavedBottleById(String id) { throw nyi(); }
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
        @Override public void atomicIncreaseStockBalance(String i, String l, int q, String id, String m, String w, String p) {
            String key = i + ":" + l;
            StockBalanceRecord existing = balances.get(key);
            if (existing == null) {
                balances.put(key, new StockBalanceRecord(id, i, l, q, 0, q, java.time.Instant.now(), m, w));
            } else {
                balances.put(key, new StockBalanceRecord(existing.id(), i, l,
                        existing.quantity() + q, existing.reservedQuantity(), existing.availableQuantity() + q,
                        java.time.Instant.now(), existing.merchantId(), w == null ? existing.warehouseId() : w));
            }
        }
        @Override public Optional<SavedBottleRecord> findSavedBottleByIdForUpdate(String id) { throw nyi(); }
        @Override public List<InventoryItemRecord> listInventoryItemsByMerchant(String m) { throw nyi(); }
        @Override public List<InventorySupplierRecord> listInventorySuppliersByMerchant(String m) {
            return suppliers.values().stream().filter(s -> m.equals(s.merchantId())).toList();
        }
        @Override public List<PurchaseRequestRecord> listPurchaseRequestsByMerchant(String m) { throw nyi(); }
        @Override public List<StockBalanceRecord> listStockBalancesByMerchant(String m) { throw nyi(); }
        @Override public List<StockBalanceRecord> listStockBalancesByProject(String p) { throw nyi(); }
        @Override public List<StockMovementRecord> listStockMovementsByMerchant(String m) { throw nyi(); }
        @Override public List<StockMovementRecord> listStockMovementsByProject(String p) { throw nyi(); }
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
