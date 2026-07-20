package com.complex.platform.voucher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.customer.CustomerRedemptionCatalogItemRecord;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.PartnerVoucherSettlementPort;
import com.complex.platform.inventory.InventoryService;
import com.complex.platform.inventory.dto.StockMovementRequest;
import com.complex.platform.voucher.dto.VoucherDefinitionRequest;
import com.complex.platform.voucher.dto.VoucherIssueRequest;
import com.complex.platform.voucher.dto.VoucherRedeemRequest;
import com.complex.platform.voucher.dto.VoucherRevokeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 纯内存单测：三类核销（折扣不结算/实物扣库存＋内部结算/外部伙伴拒建）、核销幂等（载荷指纹）、
 * 越权防（商户 scope）、积分兑券不动库存、作废/过期/双核销状态机，不触数据库。
 */
class VoucherServiceTests {

    /** 内存仓储：只实现单测所需路径（overview 的通票第三方券 UNION 属 JDBC 集成测试范畴，此处用可设列表）。 */
    private static final class InMemoryVoucherRepository implements VoucherRepository {
        final Map<String, VoucherDefinitionRecord> definitions = new LinkedHashMap<>();
        final Map<String, VoucherInstanceRecord> instances = new LinkedHashMap<>();
        final Map<String, VoucherRedemptionEntryRecord> redemptions = new LinkedHashMap<>();
        final Map<String, String> catalogLinks = new LinkedHashMap<>();
        final List<VoucherOverviewRecord> partnerOverviewRows = new ArrayList<>();
        String lastOverviewMerchantScope = "UNSET";
        String lastOverviewCustomerId = "UNSET";

        @Override
        public Optional<VoucherDefinitionRecord> findDefinitionById(String id) {
            return Optional.ofNullable(definitions.get(id));
        }

        @Override
        public Optional<VoucherDefinitionRecord> findDefinitionForCatalogItem(String catalogItemId) {
            String definitionId = catalogLinks.getOrDefault(catalogItemId, "vdef_cat_" + catalogItemId);
            return Optional.ofNullable(definitions.get(definitionId));
        }

        @Override
        public boolean insertDefinition(VoucherDefinitionRecord definition) {
            return definitions.putIfAbsent(definition.id(), definition) == null;
        }

        @Override
        public PageResponse<VoucherDefinitionRecord> listDefinitions(
                String merchantScope, String status, String voucherKind, int page, int pageSize) {
            List<VoucherDefinitionRecord> filtered = definitions.values().stream()
                    .filter(d -> merchantScope == null || d.merchantId() == null || merchantScope.equals(d.merchantId()))
                    .filter(d -> status == null || d.status().equals(status))
                    .filter(d -> voucherKind == null || d.voucherKind().equals(voucherKind))
                    .toList();
            return new PageResponse<>(filtered, page, pageSize, filtered.size());
        }

        @Override
        public Optional<VoucherInstanceRecord> findInstanceById(String id) {
            return Optional.ofNullable(instances.get(id));
        }

        @Override
        public Optional<VoucherInstanceRecord> findInstanceByCode(String code) {
            return instances.values().stream().filter(i -> i.code().equals(code)).findFirst();
        }

        @Override
        public boolean insertInstance(VoucherInstanceRecord instance) {
            return instances.putIfAbsent(instance.id(), instance) == null;
        }

        @Override
        public int updateInstanceStatus(String id, String expectFrom, String toStatus, Instant at, String revokeReason) {
            VoucherInstanceRecord current = instances.get(id);
            if (current == null || !current.status().equals(expectFrom)) {
                return 0;
            }
            instances.put(id, new VoucherInstanceRecord(
                    current.id(), current.definitionId(), current.customerId(), current.code(),
                    current.sourceType(), current.sourceRef(), toStatus, current.issuedAt(), current.expiresAt(),
                    "redeemed".equals(toStatus) ? at : current.redeemedAt(),
                    "revoked".equals(toStatus) ? at : current.revokedAt(),
                    "revoked".equals(toStatus) ? revokeReason : current.revokeReason(),
                    current.stockReserved(), current.createdAt(), at));
            return 1;
        }

        @Override
        public int updateInstanceCustomer(String id, String newCustomerId, Instant at) {
            VoucherInstanceRecord current = instances.get(id);
            if (current == null || !"issued".equals(current.status())) {
                return 0;
            }
            instances.put(id, new VoucherInstanceRecord(
                    current.id(), current.definitionId(), newCustomerId, current.code(),
                    current.sourceType(), current.sourceRef(), current.status(), current.issuedAt(), current.expiresAt(),
                    current.redeemedAt(), current.revokedAt(), current.revokeReason(),
                    current.stockReserved(), current.createdAt(), at));
            return 1;
        }

        @Override
        public List<VoucherInstanceRecord> listExpiredReservedIssuedInstances(Instant now, int limit) {
            return instances.values().stream()
                    .filter(i -> "issued".equals(i.status()) && i.stockReserved())
                    .filter(i -> i.expiresAt() != null && !i.expiresAt().isAfter(now))
                    .limit(limit)
                    .toList();
        }

        @Override
        public PageResponse<VoucherInstanceRecord> listInstances(
                String merchantScope, String customerId, String definitionId, String status, String sourceType,
                int page, int pageSize) {
            List<VoucherInstanceRecord> filtered = instances.values().stream()
                    .filter(i -> {
                        if (merchantScope == null) {
                            return true;
                        }
                        VoucherDefinitionRecord d = definitions.get(i.definitionId());
                        return d != null && (d.merchantId() == null || merchantScope.equals(d.merchantId()));
                    })
                    .filter(i -> customerId == null || i.customerId().equals(customerId))
                    .filter(i -> definitionId == null || i.definitionId().equals(definitionId))
                    .filter(i -> status == null || i.status().equals(status))
                    .filter(i -> sourceType == null || i.sourceType().equals(sourceType))
                    .toList();
            return new PageResponse<>(filtered, page, pageSize, filtered.size());
        }

        @Override
        public boolean insertRedemption(VoucherRedemptionEntryRecord entry) {
            boolean duplicated = redemptions.values().stream()
                    .anyMatch(existing -> existing.voucherInstanceId().equals(entry.voucherInstanceId()));
            if (duplicated) {
                return false;
            }
            return redemptions.putIfAbsent(entry.id(), entry) == null;
        }

        @Override
        public PageResponse<VoucherRedemptionEntryRecord> listRedemptions(
                String merchantId, String voucherInstanceId, int page, int pageSize) {
            List<VoucherRedemptionEntryRecord> filtered = redemptions.values().stream()
                    .filter(r -> merchantId == null || merchantId.equals(r.merchantId()))
                    .filter(r -> voucherInstanceId == null || voucherInstanceId.equals(r.voucherInstanceId()))
                    .toList();
            return new PageResponse<>(filtered, page, pageSize, filtered.size());
        }

        @Override
        public PageResponse<VoucherOverviewRecord> overview(
                String merchantScope, String customerId, int page, int pageSize) {
            lastOverviewMerchantScope = merchantScope;
            lastOverviewCustomerId = customerId;
            List<VoucherOverviewRecord> rows = new ArrayList<>();
            for (VoucherInstanceRecord i : instances.values()) {
                if (customerId != null && !i.customerId().equals(customerId)) {
                    continue;
                }
                VoucherDefinitionRecord d = definitions.get(i.definitionId());
                rows.add(new VoucherOverviewRecord(i.id(), "unified", d == null ? "?" : d.name(),
                        d == null ? "?" : d.voucherKind(), i.customerId(), i.status(),
                        "issued".equals(i.status()) ? 1 : 0, i.expiresAt(),
                        d == null ? null : d.faceValueCent(), d == null ? null : d.redeemMethod(), i.code()));
            }
            partnerOverviewRows.stream()
                    .filter(r -> customerId == null || customerId.equals(r.customerId()))
                    .forEach(rows::add);
            return new PageResponse<>(rows, page, pageSize, rows.size());
        }
    }

    /** 内存幂等仓储（MerchantExpenseServiceTests 同款）：载荷指纹与重放语义走真 IdempotencyService。 */
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

    private InMemoryVoucherRepository repository;
    private AuthorizationService authorizationService;
    private InventoryService inventoryService;
    private PartnerVoucherSettlementPort settlementPort;
    private VoucherTransferRepository transferRepository;
    private VoucherService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryVoucherRepository();
        authorizationService = mock(AuthorizationService.class);
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor("usr_admin", "platform_admin_user", "platform_admin", "org_1", null, "active"));
        inventoryService = mock(InventoryService.class);
        // V312 发券预占：默认库存充足（reserveStock 成功）；释放预占返回 1。个别缺货用例在测内改写。
        when(inventoryService.reserveStock(anyString(), anyString(), anyInt())).thenReturn(true);
        when(inventoryService.releaseStockReservation(anyString(), anyString(), anyInt())).thenReturn(1);
        settlementPort = mock(PartnerVoucherSettlementPort.class);
        // V313 转赠冻结：默认无在途转赠（existsPendingForInstance=false，mock 布尔默认即 false）。
        transferRepository = mock(VoucherTransferRepository.class);
        AuditedOperationExecutor operations = new AuditedOperationExecutor(
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                mock(AuditEventService.class));
        service = new VoucherService(repository, authorizationService, operations, inventoryService,
                settlementPort, transferRepository);
    }

    // ── 定义构造辅助 ────────────────────────────────────────────────────────

    private VoucherDefinitionRecord discountDefinition(String id, String merchantId) {
        VoucherDefinitionRecord definition = new VoucherDefinitionRecord(
                id, "满减券", "merchant_discount", "in_store_staff", "none", merchantId,
                5000L, null, null, null, null, null, null, "active", true,
                Instant.parse("2026-07-18T00:00:00Z"), Instant.parse("2026-07-18T00:00:00Z"));
        repository.insertDefinition(definition);
        return definition;
    }

    private VoucherDefinitionRecord goodsDefinition(String id, String merchantId, Long internalPriceCent) {
        VoucherDefinitionRecord definition = new VoucherDefinitionRecord(
                id, "店内实物券", "in_store_goods", "in_store_staff",
                internalPriceCent == null ? "none" : "internal_price", merchantId,
                null, "item_1", "loc_1", 2, internalPriceCent, null, null, "active", true,
                Instant.parse("2026-07-18T00:00:00Z"), Instant.parse("2026-07-18T00:00:00Z"));
        repository.insertDefinition(definition);
        return definition;
    }

    private VoucherInstanceRecord issueTo(String customerId, VoucherDefinitionRecord definition, String key) {
        return service.issueVouchers(new VoucherIssueRequest(key, definition.id(), customerId, 1))
                .instances().get(0);
    }

    // ── 券定义 ─────────────────────────────────────────────────────────────

    @Test
    void createDefinitionValidatesShapePerKind() {
        // 折扣券缺面额/缺商户拒绝。
        assertThatThrownBy(() -> service.createDefinition(new VoucherDefinitionRequest(
                "k1", "满减券", "merchant_discount", "in_store_staff", "none", "merchant-1",
                null, null, null, null, null, null, null, null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("face_value_cent");
        assertThatThrownBy(() -> service.createDefinition(new VoucherDefinitionRequest(
                "k2", "满减券", "merchant_discount", "in_store_staff", "none", null,
                5000L, null, null, null, null, null, null, null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("merchant_id");
        // 实物券缺货品信息拒绝。
        assertThatThrownBy(() -> service.createDefinition(new VoucherDefinitionRequest(
                "k3", "实物券", "in_store_goods", "in_store_staff", "none", null,
                null, null, null, null, null, null, null, null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("inventory_item_id");
        // 内部结算缺内部价拒绝。
        assertThatThrownBy(() -> service.createDefinition(new VoucherDefinitionRequest(
                "k4", "实物券", "in_store_goods", "in_store_staff", "internal_price", "merchant-1",
                null, "item_1", "loc_1", 1, null, null, null, null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("internal_price_cent");
        assertThat(repository.definitions).isEmpty();
        // 合法折扣券可建。
        VoucherDefinitionRecord created = service.createDefinition(new VoucherDefinitionRequest(
                "k5", "满减券", "merchant_discount", "in_store_staff", "none", "merchant-1",
                5000L, null, null, null, null, 30, null, null));
        assertThat(created.voucherKind()).isEqualTo("merchant_discount");
        assertThat(created.validDays()).isEqualTo(30);
        // V313：请求未给 transferable → 券种默认可转让。
        assertThat(created.transferable()).isTrue();
        // 显式关闭"可转让"开关：券种落库 transferable=false（供 VoucherTransferService 前置拒绝转赠）。
        VoucherDefinitionRecord noTransfer = service.createDefinition(new VoucherDefinitionRequest(
                "k6", "不可转券", "merchant_discount", "in_store_staff", "none", "merchant-1",
                5000L, null, null, null, null, null, null, false));
        assertThat(noTransfer.transferable()).isFalse();
    }

    @Test
    void createDefinitionRejectsExternalPartnerKindAndPassSettlement() {
        // 边界拍板：外部伙伴券实例＝通票权益（V282），统一台账不建其定义；pass_settlement 属通票结算口径。
        assertThatThrownBy(() -> service.createDefinition(new VoucherDefinitionRequest(
                "k-ext", "第三方券", "external_partner", "external_reflow", "none", null,
                null, null, null, null, null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_KIND_NOT_SUPPORTED"));
        assertThatThrownBy(() -> service.createDefinition(new VoucherDefinitionRequest(
                "k-pass", "通票口径券", "merchant_discount", "in_store_staff", "pass_settlement", "merchant-1",
                5000L, null, null, null, null, null, null, null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("pass_settlement");
    }

    @Test
    void createDefinitionSameKeyDifferentPayloadIsRejected() {
        service.createDefinition(new VoucherDefinitionRequest(
                "dup-key", "满减券", "merchant_discount", "in_store_staff", "none", "merchant-1",
                5000L, null, null, null, null, null, null, null));
        assertThatThrownBy(() -> service.createDefinition(new VoucherDefinitionRequest(
                "dup-key", "满减券", "merchant_discount", "in_store_staff", "none", "merchant-1",
                9900L, null, null, null, null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
        assertThat(repository.definitions).hasSize(1);
    }

    // ── 发放（营销赠券）────────────────────────────────────────────────────

    @Test
    void issueVouchersCreatesMarketingInstancesWithExpiry() {
        VoucherDefinitionRecord definition = service.createDefinition(new VoucherDefinitionRequest(
                "def-key", "拉新赠券", "merchant_discount", "in_store_staff", "none", "merchant-1",
                5000L, null, null, null, null, 30, null, null));
        VoucherIssueResultRecord result = service.issueVouchers(
                new VoucherIssueRequest("issue-key", definition.id(), "customer-1", 3));
        assertThat(result.instances()).hasSize(3);
        assertThat(result.instances()).allSatisfy(instance -> {
            assertThat(instance.sourceType()).isEqualTo("marketing_grant");
            assertThat(instance.customerId()).isEqualTo("customer-1");
            assertThat(instance.status()).isEqualTo("issued");
            assertThat(instance.expiresAt()).isNotNull();
            assertThat(instance.code()).startsWith("VC");
        });
        // 同键重放返回首次响应，不重复发券。
        VoucherIssueResultRecord replay = service.issueVouchers(
                new VoucherIssueRequest("issue-key", definition.id(), "customer-1", 3));
        assertThat(replay.instances()).extracting(VoucherInstanceRecord::id)
                .containsExactlyElementsOf(result.instances().stream().map(VoucherInstanceRecord::id).toList());
        assertThat(repository.instances).hasSize(3);
    }

    @Test
    void issueRejectsMissingDefinitionDisabledDefinitionAndBadQuantity() {
        assertThatThrownBy(() -> service.issueVouchers(new VoucherIssueRequest("k1", "missing", "customer-1", 1)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_DEFINITION_NOT_FOUND"));
        repository.insertDefinition(new VoucherDefinitionRecord(
                "vdef_disabled", "停用券", "merchant_discount", "in_store_staff", "none", "merchant-1",
                5000L, null, null, null, null, null, null, "disabled", true,
                Instant.parse("2026-07-18T00:00:00Z"), Instant.parse("2026-07-18T00:00:00Z")));
        assertThatThrownBy(() -> service.issueVouchers(new VoucherIssueRequest("k2", "vdef_disabled", "customer-1", 1)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_DEFINITION_DISABLED"));
        VoucherDefinitionRecord active = discountDefinition("vdef_active", "merchant-1");
        assertThatThrownBy(() -> service.issueVouchers(new VoucherIssueRequest("k3", active.id(), "customer-1", 0)))
                .isInstanceOf(ApiException.class).hasMessageContaining("quantity");
        assertThatThrownBy(() -> service.issueVouchers(new VoucherIssueRequest("k4", active.id(), "customer-1", 101)))
                .isInstanceOf(ApiException.class).hasMessageContaining("quantity");
        assertThat(repository.instances).isEmpty();
    }

    // ── 三类核销 ───────────────────────────────────────────────────────────

    @Test
    void redeemDiscountVoucherMarksRedeemedWithoutInventoryOrSettlement() {
        VoucherDefinitionRecord definition = discountDefinition("vdef_d1", "merchant-1");
        VoucherInstanceRecord instance = issueTo("customer-1", definition, "issue-d1");

        VoucherRedemptionEntryRecord entry = service.redeem(
                new VoucherRedeemRequest("redeem-d1", instance.code(), null, "结账抵扣"));

        assertThat(entry.voucherKind()).isEqualTo("merchant_discount");
        assertThat(entry.settlementMode()).isEqualTo("none");
        assertThat(entry.settlementAmountCent()).isNull();
        assertThat(entry.arApItemId()).isNull();
        assertThat(entry.merchantId()).isEqualTo("merchant-1");
        assertThat(repository.instances.get(instance.id()).status()).isEqualTo("redeemed");
        verifyNoInteractions(inventoryService);
        verifyNoInteractions(settlementPort);
    }

    @Test
    void redeemGoodsVoucherReleasesReservationDeductsStockAndHooksInternalSettlement() {
        VoucherDefinitionRecord definition = goodsDefinition("vdef_g1", "merchant-1", 1500L);
        // V312：发券即预占 2 件（stock_quantity=2）。
        VoucherInstanceRecord instance = issueTo("customer-1", definition, "issue-g1");
        assertThat(instance.stockReserved()).isTrue();
        verify(inventoryService).reserveStock("item_1", "loc_1", 2);
        when(settlementPort.createPartnerVoucherPayable(eq("merchant-1"), eq(1500L), anyString()))
                .thenReturn("arap_voucher_1");

        VoucherRedemptionEntryRecord entry = service.redeem(
                new VoucherRedeemRequest("redeem-g1", null, instance.id(), "实物领取"));

        // V312：核销转实扣＝先释放预占（reserved->available）再 redeem_consume（available->出库+quantity），净 reserved->出库。
        verify(inventoryService).releaseStockReservation("item_1", "loc_1", 2);
        // 实物出库发生在核销阶段（兑换只发券），出库幂等引用落核销记录。
        verify(inventoryService).createTypedStockMovement(eq("redeem_consume"), any(StockMovementRequest.class));
        assertThat(entry.stockMovementRef()).isEqualTo("voucher-redeem-" + instance.id());
        // 内部结算价挂应付（内部价范式复用），结算金额与应付 id 同落核销记录。
        assertThat(entry.settlementMode()).isEqualTo("internal_price");
        assertThat(entry.settlementAmountCent()).isEqualTo(1500L);
        assertThat(entry.arApItemId()).isEqualTo("arap_voucher_1");
        assertThat(repository.instances.get(instance.id()).status()).isEqualTo("redeemed");
    }

    @Test
    void issueGoodsVoucherRejectsWhenReservationInsufficientWithoutCreatingInstance() {
        // V312 防超发：发券预占失败（可用不足）→ 拒绝发放、不建券实例（redeemPoints 场景即"不扣积分"由外层事务回滚兜底）。
        VoucherDefinitionRecord definition = goodsDefinition("vdef_g2", "merchant-1", null);
        when(inventoryService.reserveStock("item_1", "loc_1", 2)).thenReturn(false);

        assertThatThrownBy(() -> service.issueVouchers(new VoucherIssueRequest("issue-g2", definition.id(), "customer-1", 1)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_STOCK_INSUFFICIENT"));
        assertThat(repository.instances).isEmpty();
    }

    @Test
    void redeemLegacyGoodsVoucherOutOfStockRejectsWithoutConsumingVoucher() {
        // 历史券（stock_reserved=false，本轮 V312 前发放）：核销仍走旧 available 直扣前置校验，缺货直接拒绝、券保持 issued。
        VoucherDefinitionRecord definition = goodsDefinition("vdef_g2b", "merchant-1", null);
        Instant t = Instant.parse("2026-07-18T00:00:00Z");
        repository.insertInstance(new VoucherInstanceRecord(
                "vi_legacy_g2b", definition.id(), "customer-1", "VCLEGACYG2B", "marketing_grant", "g1",
                "issued", t, null, null, null, null, false, t, t));
        when(inventoryService.availableStock("item_1", "loc_1")).thenReturn(1); // 需 2 件

        assertThatThrownBy(() -> service.redeem(new VoucherRedeemRequest("redeem-g2b", "VCLEGACYG2B", null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_STOCK_INSUFFICIENT"));
        verify(inventoryService, never()).createTypedStockMovement(any(), any(StockMovementRequest.class));
        assertThat(repository.redemptions).isEmpty();
        assertThat(repository.instances.get("vi_legacy_g2b").status()).isEqualTo("issued");
    }

    @Test
    void redeemRejectsExpiredRevokedAndAlreadyRedeemed() {
        VoucherDefinitionRecord definition = discountDefinition("vdef_d2", "merchant-1");
        // 过期券：拒绝且惰性落 expired。
        Instant past = Instant.parse("2026-01-01T00:00:00Z");
        repository.insertInstance(new VoucherInstanceRecord(
                "vi_expired", definition.id(), "customer-1", "VCEXPIRED", "marketing_grant", "g1",
                "issued", past, past.plusSeconds(60), null, null, null, false, past, past));
        assertThatThrownBy(() -> service.redeem(new VoucherRedeemRequest("k-exp", "VCEXPIRED", null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_EXPIRED"));
        assertThat(repository.instances.get("vi_expired").status()).isEqualTo("expired");
        // 已作废券拒绝。
        VoucherInstanceRecord revoked = issueTo("customer-1", definition, "issue-rv");
        service.revoke(revoked.id(), new VoucherRevokeRequest("rv-key", "活动取消"));
        assertThatThrownBy(() -> service.redeem(new VoucherRedeemRequest("k-rv", revoked.code(), null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_REVOKED"));
        // 已核销券再核（换新幂等键）拒绝——双花闸。
        VoucherInstanceRecord redeemed = issueTo("customer-1", definition, "issue-rd");
        service.redeem(new VoucherRedeemRequest("k-rd-1", redeemed.code(), null, null));
        assertThatThrownBy(() -> service.redeem(new VoucherRedeemRequest("k-rd-2", redeemed.code(), null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_ALREADY_REDEEMED"));
        assertThat(repository.redemptions).hasSize(1);
    }

    @Test
    void redeemIsIdempotentBySameKeyAndRejectsSameKeyDifferentPayload() {
        VoucherDefinitionRecord definition = discountDefinition("vdef_d3", "merchant-1");
        VoucherInstanceRecord instance = issueTo("customer-1", definition, "issue-idem");
        VoucherRedemptionEntryRecord first = service.redeem(
                new VoucherRedeemRequest("same-key", instance.code(), null, "第一次"));
        // 同键同载荷重放：返回首次核销记录，不产生第二行。
        VoucherRedemptionEntryRecord replay = service.redeem(
                new VoucherRedeemRequest("same-key", instance.code(), null, "第一次"));
        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(repository.redemptions).hasSize(1);
        // 同键不同载荷（note 不同）：载荷指纹冲突拒绝。
        assertThatThrownBy(() -> service.redeem(new VoucherRedeemRequest("same-key", instance.code(), null, "换个理由")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void merchantScopedActorCannotRedeemOtherMerchantsVoucherButCanRedeemOwnAndPlatform() {
        VoucherDefinitionRecord otherMerchant = discountDefinition("vdef_other", "merchant-other");
        VoucherDefinitionRecord ownMerchant = discountDefinition("vdef_own", "merchant-scope");
        VoucherDefinitionRecord platform = goodsDefinition("vdef_platform", null, null);
        VoucherInstanceRecord otherInstance = issueTo("customer-1", otherMerchant, "issue-other");
        VoucherInstanceRecord ownInstance = issueTo("customer-1", ownMerchant, "issue-own");
        VoucherInstanceRecord platformInstance = issueTo("customer-1", platform, "issue-platform");

        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant-scope"));
        // 他商户券：拒绝（CROSS_MERCHANT_FORBIDDEN），券保持可核。
        assertThatThrownBy(() -> service.redeem(new VoucherRedeemRequest("k-x", otherInstance.code(), null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(repository.instances.get(otherInstance.id()).status()).isEqualTo("issued");
        // 自家券：可核。
        assertThat(service.redeem(new VoucherRedeemRequest("k-own", ownInstance.code(), null, null)).merchantId())
                .isEqualTo("merchant-scope");
        // 平台通用券（定义无商户）：任意核销点可核，核销点商户记 scope。
        when(inventoryService.availableStock("item_1", "loc_1")).thenReturn(10);
        assertThat(service.redeem(new VoucherRedeemRequest("k-platform", platformInstance.code(), null, null)).merchantId())
                .isEqualTo("merchant-scope");
    }

    // ── 作废 ───────────────────────────────────────────────────────────────

    @Test
    void revokeRequiresReasonOnlyIssuedRevocableAndReplayIsIdempotent() {
        VoucherDefinitionRecord definition = discountDefinition("vdef_rv", "merchant-1");
        VoucherInstanceRecord instance = issueTo("customer-1", definition, "issue-rv2");
        assertThatThrownBy(() -> service.revoke(instance.id(), new VoucherRevokeRequest("k0", " ")))
                .isInstanceOf(ApiException.class).hasMessageContaining("reason");

        VoucherInstanceRecord revoked = service.revoke(instance.id(), new VoucherRevokeRequest("k1", "活动下线"));
        assertThat(revoked.status()).isEqualTo("revoked");
        assertThat(revoked.revokeReason()).isEqualTo("活动下线");
        // 同键重放：返回首次响应。
        assertThat(service.revoke(instance.id(), new VoucherRevokeRequest("k1", "活动下线")).status())
                .isEqualTo("revoked");
        // 换新键再作废已作废券：拒绝。
        assertThatThrownBy(() -> service.revoke(instance.id(), new VoucherRevokeRequest("k2", "再来一次")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_NOT_REVOCABLE"));
        // 已核销券不可作废。
        VoucherInstanceRecord redeemed = issueTo("customer-1", definition, "issue-rd2");
        service.redeem(new VoucherRedeemRequest("k-rd", redeemed.code(), null, null));
        assertThatThrownBy(() -> service.revoke(redeemed.id(), new VoucherRevokeRequest("k3", "想反悔")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_NOT_REVOCABLE"));
    }

    // ── 积分兑券端口（redeemPoints 只发券不动库存）────────────────────────

    @Test
    void pointsRedemptionIssuesDerivedVouchersAndReservesGiftStock() {
        CustomerRedemptionCatalogItemRecord item = new CustomerRedemptionCatalogItemRecord(
                "gift_1", "毛绒玩偶", "gift", 120L, "item_gift", "gift_shelf", 2, "active",
                null, null, Instant.parse("2026-07-18T00:00:00Z"), Instant.parse("2026-07-18T00:00:00Z"));

        List<VoucherInstanceRecord> issued = service.issueForPointsRedemption(
                item, "customer-1", "redemption_abc", 2, Instant.parse("2026-07-18T00:00:00Z"));

        // 目录未挂接 → 派生实物券定义（vdef_cat_<目录id>，平台通用、不结算）。
        assertThat(repository.definitions).containsKey("vdef_cat_gift_1");
        VoucherDefinitionRecord derived = repository.definitions.get("vdef_cat_gift_1");
        assertThat(derived.voucherKind()).isEqualTo("in_store_goods");
        assertThat(derived.settlementMode()).isEqualTo("none");
        assertThat(issued).hasSize(2);
        assertThat(issued).allSatisfy(instance -> {
            assertThat(instance.sourceType()).isEqualTo("points_redemption");
            assertThat(instance.sourceRef()).isEqualTo("redemption_abc");
            // V312：兑换出的实物券已预占库存。
            assertThat(instance.stockReserved()).isTrue();
        });
        // V312：兑换阶段预占礼品库存（防超发，对齐 PRD §5）；每张券预占 stock_quantity=2 件，共 2 张。
        verify(inventoryService, times(2)).reserveStock("item_gift", "gift_shelf", 2);
        // 实物出库仍在统一核销阶段，兑换阶段不出库。
        verify(inventoryService, never()).createTypedStockMovement(any(), any(StockMovementRequest.class));
        // 幂等重放（同兑换记录 id）：实例 id 派生一致，不重复发券、不重复预占。
        List<VoucherInstanceRecord> replay = service.issueForPointsRedemption(
                item, "customer-1", "redemption_abc", 2, Instant.parse("2026-07-18T00:00:01Z"));
        assertThat(replay).extracting(VoucherInstanceRecord::id)
                .containsExactlyElementsOf(issued.stream().map(VoucherInstanceRecord::id).toList());
        assertThat(repository.instances).hasSize(2);
        verify(inventoryService, times(2)).reserveStock("item_gift", "gift_shelf", 2);
    }

    @Test
    void pointsRedemptionPrefersExplicitlyLinkedDefinition() {
        VoucherDefinitionRecord linked = discountDefinition("vdef_linked", "merchant-1");
        repository.catalogLinks.put("gift_2", "vdef_linked");
        CustomerRedemptionCatalogItemRecord item = new CustomerRedemptionCatalogItemRecord(
                "gift_2", "折扣礼遇", "gift", 200L, null, null, 0, "active",
                null, null, Instant.parse("2026-07-18T00:00:00Z"), Instant.parse("2026-07-18T00:00:00Z"));

        List<VoucherInstanceRecord> issued = service.issueForPointsRedemption(
                item, "customer-2", "redemption_def", 1, Instant.parse("2026-07-18T00:00:00Z"));

        assertThat(issued).singleElement().satisfies(instance ->
                assertThat(instance.definitionId()).isEqualTo(linked.id()));
        // 显式挂接时不派生 vdef_cat_ 定义。
        assertThat(repository.definitions).doesNotContainKey("vdef_cat_gift_2");
    }

    @Test
    void pointsRedemptionWithoutDefinitionOrGoodsInfoIsRejected() {
        CustomerRedemptionCatalogItemRecord item = new CustomerRedemptionCatalogItemRecord(
                "gift_3", "无货品目录项", "gift", 100L, null, null, 0, "active",
                null, null, Instant.parse("2026-07-18T00:00:00Z"), Instant.parse("2026-07-18T00:00:00Z"));
        assertThatThrownBy(() -> service.issueForPointsRedemption(
                item, "customer-3", "redemption_ghi", 1, Instant.parse("2026-07-18T00:00:00Z")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("REDEMPTION_ITEM_NOT_LINKABLE"));
        assertThat(repository.instances).isEmpty();
    }

    // ── 读路径 ─────────────────────────────────────────────────────────────

    @Test
    void overviewAggregatesUnifiedAndPartnerPassSources() {
        VoucherDefinitionRecord definition = discountDefinition("vdef_ov", "merchant-1");
        issueTo("customer-1", definition, "issue-ov");
        repository.partnerOverviewRows.add(new VoucherOverviewRecord(
                "ie_1", "pass_partner", "餐厅第三方券", "external_partner", "customer-1", "active", 3,
                Instant.parse("2026-12-31T00:00:00Z"), null, "external_reflow", null));

        PageResponse<VoucherOverviewRecord> page = service.overview("customer-1", 1, 50);

        assertThat(page.items()).extracting(VoucherOverviewRecord::source)
                .containsExactlyInAnyOrder("unified", "pass_partner");
        // 计次模型兼容映射：通票第三方券带剩余次数展示，不硬套一次性状态机。
        assertThat(page.items()).filteredOn(row -> "pass_partner".equals(row.source()))
                .singleElement().satisfies(row -> assertThat(row.remainingTimes()).isEqualTo(3));
    }

    // ── 客人端「我的券包」自助只读 ─────────────────────────────────────────

    @Test
    void customerWalletForcesCallerCustomerIdAndDropsMerchantScope() {
        // 两个客人各持券 + 一张第三方券挂 customer-2；客人自助只看本人（不可查他人）。
        VoucherDefinitionRecord def = discountDefinition("vdef_cw", "merchant-1");
        issueTo("customer-1", def, "cw-1");
        issueTo("customer-2", def, "cw-2");
        repository.partnerOverviewRows.add(new VoucherOverviewRecord(
                "ie_cw", "pass_partner", "第三方券", "external_partner", "customer-2", "active", 2,
                Instant.parse("2026-12-31T00:00:00Z"), null, "external_reflow", null));

        PageResponse<VoucherOverviewRecord> page = service.customerOverview("customer-1", 1, 50);

        // 强制本人 customer_id：统一券 + 第三方券都不越界，只见 customer-1。
        assertThat(page.items()).extracting(VoucherOverviewRecord::customerId).containsOnly("customer-1");
        // merchantScope 传 null（客人不受商户 scope 限制），customer_id 传本人。
        assertThat(repository.lastOverviewMerchantScope).isNull();
        assertThat(repository.lastOverviewCustomerId).isEqualTo("customer-1");
    }

    @Test
    void customerWalletCarriesFaceValueAndMethodButHasNoInternalPriceField() {
        // 面额/折扣 + 核销方式随聚合视图返回，供券卡展示。
        VoucherDefinitionRecord def = discountDefinition("vdef_cw2", "merchant-1"); // 面额 5000，核销方式 in_store_staff
        issueTo("customer-9", def, "cw2-1");

        PageResponse<VoucherOverviewRecord> page = service.customerOverview("customer-9", 1, 50);

        assertThat(page.items()).singleElement().satisfies(row -> {
            assertThat(row.faceValueCent()).isEqualTo(5000L);
            assertThat(row.redeemMethod()).isEqualTo("in_store_staff");
        });
        // 结构性铁证：聚合视图 record 无任何内部结算价/成本访问器（客人天然不可见）。
        assertThat(java.util.Arrays.stream(VoucherOverviewRecord.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName))
                .doesNotContain("internalPriceCent", "settlementAmountCent");
    }

    @Test
    void customerWalletRejectsBlankCustomerId() {
        // 兜底防越权：空/空白 customer_id 直接 401，绝不退化为读全部客人券。
        assertThatThrownBy(() -> service.customerOverview("  ", 1, 50))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("CUSTOMER_REQUIRED"));
    }

    // ── 内部结算价脱敏（V279 bar 同款铁律，成本仅授权后台可见）─────────────

    @Test
    void withoutInternalPriceNullsCostOnDefinitionAndRedemptionButKeepsOtherFields() {
        // 定义脱敏：internal_price_cent 置 null，其余字段（面额/结算模式等）保留。
        VoucherDefinitionRecord definition = goodsDefinition("vdef_mask", "merchant-1", 1500L);
        VoucherDefinitionRecord masked = definition.withoutInternalPrice();
        assertThat(masked.internalPriceCent()).isNull();
        assertThat(masked.settlementMode()).isEqualTo("internal_price");
        assertThat(masked.merchantId()).isEqualTo("merchant-1");
        assertThat(masked.stockQuantity()).isEqualTo(2);
        // 原对象不被改动（record 副本语义）。
        assertThat(definition.internalPriceCent()).isEqualTo(1500L);

        // 核销记录脱敏：settlement_amount_cent（=内部结算价）置 null，结算模式/应付 id 保留（非成本数值本身）。
        VoucherInstanceRecord instance = issueTo("customer-1", definition, "issue-mask");
        when(inventoryService.availableStock("item_1", "loc_1")).thenReturn(10);
        when(settlementPort.createPartnerVoucherPayable(eq("merchant-1"), eq(1500L), anyString()))
                .thenReturn("arap_mask_1");
        VoucherRedemptionEntryRecord entry = service.redeem(
                new VoucherRedeemRequest("redeem-mask", null, instance.id(), null));
        VoucherRedemptionEntryRecord maskedEntry = entry.withoutInternalPrice();
        assertThat(maskedEntry.settlementAmountCent()).isNull();
        assertThat(maskedEntry.settlementMode()).isEqualTo("internal_price");
        assertThat(maskedEntry.arApItemId()).isEqualTo("arap_mask_1");
        // 有权限侧仍见真实成本。
        assertThat(entry.settlementAmountCent()).isEqualTo(1500L);
    }

    @Test
    void listInstancesValidatesFilterEnumsAndScopesToMerchant() {
        VoucherDefinitionRecord own = discountDefinition("vdef_scope_own", "merchant-scope");
        VoucherDefinitionRecord other = discountDefinition("vdef_scope_other", "merchant-other");
        issueTo("customer-1", own, "issue-scope-own");
        issueTo("customer-1", other, "issue-scope-other");
        assertThatThrownBy(() -> service.listInstances(null, null, "bad-status", null, 1, 50))
                .isInstanceOf(ApiException.class).hasMessageContaining("status");
        assertThatThrownBy(() -> service.listInstances(null, null, null, "bad-source", 1, 50))
                .isInstanceOf(ApiException.class).hasMessageContaining("source_type");

        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant-scope"));
        PageResponse<VoucherInstanceRecord> page = service.listInstances(null, null, null, null, 1, 50);
        assertThat(page.items()).extracting(VoucherInstanceRecord::definitionId)
                .containsExactly("vdef_scope_own");
    }

    // ── V313 核销转赠冻结 + V312 过期预占释放 ─────────────────────────────

    @Test
    void redeemBlockedWhileTransferPending() {
        VoucherDefinitionRecord definition = discountDefinition("vdef_tf", "merchant-1");
        VoucherInstanceRecord instance = issueTo("customer-1", definition, "issue-tf");
        // 该券有在途转赠：核销冻结拒绝，券保持 issued，不产生核销记录。
        when(transferRepository.existsPendingForInstance(instance.id())).thenReturn(true);
        assertThatThrownBy(() -> service.redeem(new VoucherRedeemRequest("k-tf", instance.code(), null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VOUCHER_TRANSFERRING"));
        assertThat(repository.instances.get(instance.id()).status()).isEqualTo("issued");
        assertThat(repository.redemptions).isEmpty();
    }

    @Test
    void expireReservedVouchersReleasesOnlyIssuedReservedExpiredAndVoidsTransfers() {
        VoucherDefinitionRecord definition = goodsDefinition("vdef_exp", "merchant-1", null); // item_1/loc_1, stock_quantity=2
        Instant now = Instant.parse("2026-07-20T00:00:00Z");
        Instant past = now.minusSeconds(3600);
        Instant future = now.plusSeconds(3600);
        // ① issued + 已预占 + 已过期 → 命中：过期并释放预占。
        repository.insertInstance(new VoucherInstanceRecord(
                "vi_exp_hit", definition.id(), "customer-1", "VCEXPHIT", "marketing_grant", "g",
                "issued", past, past.plusSeconds(1), null, null, null, true, past, past));
        // ② issued + 已预占 + 未过期 → 不动。
        repository.insertInstance(new VoucherInstanceRecord(
                "vi_exp_future", definition.id(), "customer-1", "VCEXPFUT", "marketing_grant", "g",
                "issued", past, future, null, null, null, true, past, past));
        // ③ issued + 未预占（历史券）+ 已过期 → 不释放预占。
        repository.insertInstance(new VoucherInstanceRecord(
                "vi_exp_legacy", definition.id(), "customer-1", "VCEXPLEG", "marketing_grant", "g",
                "issued", past, past.plusSeconds(1), null, null, null, false, past, past));

        int released = service.expireReservedVouchers(now, 100);

        assertThat(released).isEqualTo(1);
        assertThat(repository.instances.get("vi_exp_hit").status()).isEqualTo("expired");
        assertThat(repository.instances.get("vi_exp_future").status()).isEqualTo("issued");
        assertThat(repository.instances.get("vi_exp_legacy").status()).isEqualTo("issued");
        // 守恒：命中券释放预占（reserved->available，件数=定义 stock_quantity=2），并作废其在途转赠；未命中券零动作。
        verify(inventoryService).releaseStockReservation("item_1", "loc_1", 2);
        verify(transferRepository).voidPendingForInstance("vi_exp_hit", "voucher_expired", now);
        verify(transferRepository, never()).voidPendingForInstance(eq("vi_exp_future"), anyString(), any());
        verify(transferRepository, never()).voidPendingForInstance(eq("vi_exp_legacy"), anyString(), any());
    }

    @Test
    void reservationExpiryJobRunOnceDelegatesToService() {
        VoucherDefinitionRecord definition = goodsDefinition("vdef_job", "merchant-1", null);
        Instant now = Instant.parse("2026-07-20T00:00:00Z");
        Instant past = now.minusSeconds(3600);
        repository.insertInstance(new VoucherInstanceRecord(
                "vi_job_hit", definition.id(), "customer-1", "VCJOB", "marketing_grant", "g",
                "issued", past, past.plusSeconds(1), null, null, null, true, past, past));
        int released = new VoucherReservationExpiryJob(service, java.time.Clock.systemUTC()).runOnce(now);
        assertThat(released).isEqualTo(1);
        assertThat(repository.instances.get("vi_job_hit").status()).isEqualTo("expired");
    }
}
