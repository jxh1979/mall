package com.complex.platform.voucher;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.CustomerRedemptionCatalogItemRecord;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.PartnerVoucherSettlementPort;
import com.complex.platform.inventory.InventoryService;
import com.complex.platform.inventory.dto.StockMovementRequest;
import com.complex.platform.voucher.dto.VoucherDefinitionRequest;
import com.complex.platform.voucher.dto.VoucherIssueRequest;
import com.complex.platform.voucher.dto.VoucherRedeemRequest;
import com.complex.platform.voucher.dto.VoucherRevokeRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 积分/券统一台账（方案A，2026-07-18 拍板）：券定义/券实例/统一核销记录。
 *
 * <p>收编：积分兑换券（实现 {@link VoucherIssuePort}，redeemPoints 只发券不动库存）＋营销赠券
 * （后台发放入口，此前系统无营销赠券实现、无历史券）。通票权益与第三方合作券（V282）不搬表，
 * {@link #overview} 读层聚合并入"全部券"。
 *
 * <p>三类核销×结算在核销路径真正建成：商户折扣券＝结账抵扣（不结算）；店内实物券＝核销时扣库存＋
 * 按内部结算价挂应付（复用 {@link PartnerVoucherSettlementPort} 内部价范式）；外部伙伴券仍走 V282
 * 对账回流，本服务不发其实例。核销幂等＝executeWrite 载荷指纹＋实例 CAS＋核销记录实例级唯一索引三道闸；
 * 越权防＝定义绑定商户的券，商户 scope 账号只能核自家（checkMerchantOwnership 同款）。
 */
@Service
public class VoucherService implements VoucherIssuePort {

    static final String VIEW_PERM = "customer.voucher.view";
    static final String MANAGE_PERM = "customer.voucher.manage";
    static final String REDEEM_PERM = "customer.voucher.redeem";
    private static final String BUSINESS_TYPE = "voucher_instance";

    private static final Set<String> KINDS = Set.of("merchant_discount", "in_store_goods", "external_partner");
    private static final Set<String> METHODS = Set.of("in_store_staff", "onsite_code", "external_reflow");
    private static final Set<String> MODES = Set.of("none", "internal_price", "pass_settlement");
    private static final Set<String> SOURCES = Set.of("points_redemption", "marketing_grant");
    private static final Set<String> STATUSES =
            Set.of(VoucherStateMachine.ISSUED, VoucherStateMachine.REDEEMED,
                    VoucherStateMachine.EXPIRED, VoucherStateMachine.REVOKED);
    private static final int MAX_ISSUE_QUANTITY = 100;

    private final VoucherRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditedOperationExecutor operations;
    private final InventoryService inventoryService;
    // finance 结算端口（PartnerVoucherService 同款：@Lazy 防环；null＝兼容单测不结算）。
    private final PartnerVoucherSettlementPort settlementPort;
    // V313 转赠冻结：核销/作废时判该券是否有在途转赠（防"转赠中又核销"），并在作废时作废在途转赠。
    private final VoucherTransferRepository transferRepository;

    @Autowired
    public VoucherService(
            VoucherRepository repository,
            AuthorizationService authorizationService,
            AuditedOperationExecutor operations,
            InventoryService inventoryService,
            @Lazy PartnerVoucherSettlementPort settlementPort,
            VoucherTransferRepository transferRepository) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.operations = operations;
        this.inventoryService = inventoryService;
        this.settlementPort = settlementPort;
        this.transferRepository = transferRepository;
    }

    // ── 读路径 ─────────────────────────────────────────────────────────────

    public PageResponse<VoucherDefinitionRecord> listDefinitions(String status, String voucherKind, int page, int pageSize) {
        authorizationService.requirePermission(VIEW_PERM);
        return operations.readPageResult(
                AuditedOperation.of("voucher.definitions_viewed", "voucher.definitions_view_failed", VIEW_PERM, BUSINESS_TYPE),
                () -> {
                    requireOptionalEnum(status, Set.of("active", "disabled"), "status");
                    requireOptionalEnum(voucherKind, KINDS, "voucher_kind");
                    return repository.listDefinitions(
                            merchantScope(), blankToNull(status), blankToNull(voucherKind), safePage(page), safeSize(pageSize));
                });
    }

    public PageResponse<VoucherInstanceRecord> listInstances(
            String customerId, String definitionId, String status, String sourceType, int page, int pageSize) {
        authorizationService.requirePermission(VIEW_PERM);
        return operations.readPageResult(
                AuditedOperation.of("voucher.instances_viewed", "voucher.instances_view_failed", VIEW_PERM, BUSINESS_TYPE),
                () -> {
                    requireOptionalEnum(status, STATUSES, "status");
                    requireOptionalEnum(sourceType, SOURCES, "source_type");
                    return repository.listInstances(merchantScope(), blankToNull(customerId), blankToNull(definitionId),
                            blankToNull(status), blankToNull(sourceType), safePage(page), safeSize(pageSize));
                });
    }

    public PageResponse<VoucherRedemptionEntryRecord> listRedemptions(
            String merchantId, String voucherInstanceId, int page, int pageSize) {
        authorizationService.requirePermission(VIEW_PERM);
        return operations.readPageResult(
                AuditedOperation.of("voucher.redemptions_viewed", "voucher.redemptions_view_failed", VIEW_PERM, BUSINESS_TYPE),
                () -> {
                    // 商户 scope 账号强制只看自家核销记录（覆盖请求参数）。
                    String scoped = authorizationService.currentMerchantScope().orElse(blankToNull(merchantId));
                    return repository.listRedemptions(scoped, blankToNull(voucherInstanceId), safePage(page), safeSize(pageSize));
                });
    }

    /** "全部券"聚合（读层并入不搬表）：统一台账实例 ∪ V282 通票第三方合作券权益实例。 */
    public PageResponse<VoucherOverviewRecord> overview(String customerId, int page, int pageSize) {
        authorizationService.requirePermission(VIEW_PERM);
        return operations.readPageResult(
                AuditedOperation.of("voucher.overview_viewed", "voucher.overview_view_failed", VIEW_PERM, BUSINESS_TYPE),
                () -> repository.overview(merchantScope(), blankToNull(customerId), safePage(page), safeSize(pageSize)));
    }

    /**
     * 客人端「我的券包」自助只读：控制器按客人 token 解析并强制 customer_id（不可查他人），
     * 不校 staff 权限（客人无 VIEW_PERM），merchantScope=null（客人不受商户 scope 限制，看本人全部券）。
     * 内部结算价客人天然不可见——VoucherOverviewRecord 不含 internal_price/settlement 字段。
     * 只读展示，不做客人端发起核销（核销在 POS/工作手机侧）。
     */
    public PageResponse<VoucherOverviewRecord> customerOverview(String customerId, int page, int pageSize) {
        String scoped = blankToNull(customerId);
        if (scoped == null) {
            throw ApiException.unauthorized("CUSTOMER_REQUIRED", "customer token required");
        }
        return operations.readPageResult(
                AuditedOperation.of("voucher.customer_wallet_viewed", "voucher.customer_wallet_view_failed", VIEW_PERM, BUSINESS_TYPE),
                () -> repository.overview(null, scoped, safePage(page), safeSize(pageSize)));
    }

    // ── 券定义 ─────────────────────────────────────────────────────────────

    @Transactional
    public VoucherDefinitionRecord createDefinition(VoucherDefinitionRequest request) {
        authorizationService.requirePermission(MANAGE_PERM);
        requireRequest(request);
        return operations.executeWrite(
                "createVoucherDefinition",
                request.idempotencyKey(),
                request,
                VoucherDefinitionRecord.class,
                AuditedOperation.of("voucher.definition_created", "voucher.definition_create_failed", MANAGE_PERM, BUSINESS_TYPE),
                () -> createDefinitionInternal(request),
                VoucherService::businessId,
                Map.of());
    }

    private VoucherDefinitionRecord createDefinitionInternal(VoucherDefinitionRequest request) {
        String name = requireText(request.name(), "name");
        String kind = requireEnum(request.voucherKind(), KINDS, "voucher_kind");
        if ("external_partner".equals(kind)) {
            // 边界拍板：外部伙伴券的实例＝通票权益（V282 partner_voucher 配置在权益模板上），不搬家也不在统一表另建。
            throw ApiException.badRequest("VOUCHER_KIND_NOT_SUPPORTED",
                    "外部伙伴券请在通票权益模板上配置第三方券（V282），统一台账不建其定义");
        }
        String method = requireEnum(request.redeemMethod(), METHODS, "redeem_method");
        if ("external_reflow".equals(method)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "external_reflow 核销方式仅用于外部伙伴券对账回流，统一台账定义请用 in_store_staff/onsite_code");
        }
        String mode = requireEnum(request.settlementMode(), MODES, "settlement_mode");
        if ("pass_settlement".equals(mode)) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "pass_settlement 结算口径属通票权益核销（13条口径），统一台账定义请用 none/internal_price");
        }
        String merchantId = blankToNull(request.merchantId());
        if ("merchant_discount".equals(kind)) {
            if (request.faceValueCent() == null || request.faceValueCent() <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "merchant_discount requires face_value_cent > 0");
            }
            if (merchantId == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "merchant_discount requires merchant_id");
            }
        }
        if ("in_store_goods".equals(kind)) {
            requireText(request.inventoryItemId(), "inventory_item_id");
            requireText(request.stockLocationId(), "stock_location_id");
            if (request.stockQuantity() == null || request.stockQuantity() <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "in_store_goods requires stock_quantity > 0");
            }
        }
        if ("internal_price".equals(mode)) {
            if (request.internalPriceCent() == null || request.internalPriceCent() <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "internal_price requires internal_price_cent > 0");
            }
            if (merchantId == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "internal_price requires merchant_id (结算商户)");
            }
        }
        if (request.validDays() != null && request.validDays() <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "valid_days must be positive when present");
        }
        // I-2 商户 scope 守卫：商户账号只能建自家商户的券定义。
        checkMerchantOwnership(merchantId);
        Instant now = Instant.now();
        String id = "vdef_" + stableToken(request.idempotencyKey());
        // V313 券种级"可转让"开关：请求未给（null）＝默认可转让。
        boolean transferable = request.transferable() == null || request.transferable();
        VoucherDefinitionRecord definition = new VoucherDefinitionRecord(
                id, name, kind, method, mode, merchantId,
                request.faceValueCent(),
                blankToNull(request.inventoryItemId()), blankToNull(request.stockLocationId()), request.stockQuantity(),
                request.internalPriceCent(), request.validDays(), request.validUntil(),
                "active", transferable, now, now);
        repository.insertDefinition(definition);
        return repository.findDefinitionById(id)
                .orElseThrow(() -> ApiException.conflict("VOUCHER_DEFINITION_CONFLICT", "voucher definition insert lost race"));
    }

    // ── 发放（营销赠券收编入口）───────────────────────────────────────────

    @Transactional
    public VoucherIssueResultRecord issueVouchers(VoucherIssueRequest request) {
        authorizationService.requirePermission(MANAGE_PERM);
        requireRequest(request);
        return operations.executeWrite(
                "issueVouchers",
                request.idempotencyKey(),
                request,
                VoucherIssueResultRecord.class,
                AuditedOperation.of("voucher.issued", "voucher.issue_failed", MANAGE_PERM, BUSINESS_TYPE),
                () -> issueInternal(request),
                VoucherService::businessId,
                Map.of());
    }

    private VoucherIssueResultRecord issueInternal(VoucherIssueRequest request) {
        String definitionId = requireText(request.definitionId(), "definition_id");
        String customerId = requireText(request.customerId(), "customer_id");
        int quantity = request.quantity() == null ? 1 : request.quantity();
        if (quantity <= 0 || quantity > MAX_ISSUE_QUANTITY) {
            throw ApiException.badRequest("VALIDATION_FAILED", "quantity must be between 1 and " + MAX_ISSUE_QUANTITY);
        }
        VoucherDefinitionRecord definition = repository.findDefinitionById(definitionId)
                .orElseThrow(() -> ApiException.notFound("VOUCHER_DEFINITION_NOT_FOUND", "voucher definition not found"));
        if (!"active".equals(definition.status())) {
            throw ApiException.conflict("VOUCHER_DEFINITION_DISABLED", "voucher definition is disabled");
        }
        // 商户 scope 账号只能发自家商户的券（平台通用定义商户账号不发）。
        if (authorizationService.currentMerchantScope().isPresent()) {
            checkMerchantOwnership(definition.merchantId());
        }
        Instant now = Instant.now();
        // 发放操作引用＝幂等键派生（营销赠发的 source_ref 锚点，可据此回查一次发放批次）。
        String grantRef = "vgrant_" + stableToken(request.idempotencyKey());
        List<VoucherInstanceRecord> issued = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            issued.add(issueOne(definition, customerId, "marketing_grant", grantRef,
                    "vi_" + stableToken(request.idempotencyKey() + ":" + i), now));
        }
        return new VoucherIssueResultRecord(definition.id(), customerId, quantity, issued);
    }

    // ── 积分兑换发券端口（CustomerService.redeemPoints 调用；客人 token 上下文，无后台权限校验）──

    @Override
    @Transactional
    public List<VoucherInstanceRecord> issueForPointsRedemption(
            CustomerRedemptionCatalogItemRecord catalogItem, String customerId, String redemptionId,
            int quantity, Instant now) {
        VoucherDefinitionRecord definition = repository.findDefinitionForCatalogItem(catalogItem.id())
                .orElseGet(() -> provisionCatalogDefinition(catalogItem, now));
        List<VoucherInstanceRecord> issued = new ArrayList<>();
        for (int i = 0; i < quantity; i++) {
            // 实例 id 由兑换记录 id 派生：redeemPoints 幂等重放不重复发券。
            issued.add(issueOne(definition, customerId, "points_redemption", redemptionId,
                    "vi_" + stableToken(redemptionId + ":" + i), now));
        }
        return issued;
    }

    /** 未显式挂接券定义的兑换目录项：按目录项派生实物券定义（get-or-create，平台通用、不结算）。 */
    private VoucherDefinitionRecord provisionCatalogDefinition(CustomerRedemptionCatalogItemRecord item, Instant now) {
        if (item.inventoryItemId() == null || item.stockLocationId() == null) {
            throw ApiException.conflict("REDEMPTION_ITEM_NOT_LINKABLE",
                    "兑换目录项未挂接券定义且无实物货品信息，无法派生券模板");
        }
        String id = "vdef_cat_" + item.id();
        repository.insertDefinition(new VoucherDefinitionRecord(
                id, item.itemName(), "in_store_goods", "in_store_staff", "none", null,
                null, item.inventoryItemId(), item.stockLocationId(),
                Math.max(item.stockQuantity(), 1), null, null, null, "active", true, now, now));
        return repository.findDefinitionById(id)
                .orElseThrow(() -> ApiException.conflict("VOUCHER_DEFINITION_CONFLICT", "derived definition insert lost race"));
    }

    private VoucherInstanceRecord issueOne(
            VoucherDefinitionRecord definition, String customerId, String sourceType, String sourceRef,
            String instanceId, Instant now) {
        // 幂等重放（redeemPoints/issueVouchers 幂等键派生同一 instanceId）：实例已存在直接返回，不重复预占库存。
        Optional<VoucherInstanceRecord> existing = repository.findInstanceById(instanceId);
        if (existing.isPresent()) {
            return existing.get();
        }
        // V312 发券预占库存（防超发，对齐 PRD §5「先扣礼品库存，不足则拒绝兑换、不扣积分」）：
        // 店内实物券发券即从承接商户实物库存预占 stock_quantity 件（available->reserved，quantity 不变）；
        // 可用不足则拒绝发券/兑换 → 抛错回滚整事务（redeemPoints 的积分扣减一并撤销，即"不扣积分"）。
        boolean reserved = "in_store_goods".equals(definition.voucherKind());
        int reserveQty = reserved ? goodsQuantity(definition) : 0;
        if (reserved && !inventoryService.reserveStock(
                definition.inventoryItemId(), definition.stockLocationId(), reserveQty)) {
            throw ApiException.conflict("VOUCHER_STOCK_INSUFFICIENT",
                    "库存不足，本次兑换/发券未完成（未扣积分）");
        }
        VoucherInstanceRecord instance = new VoucherInstanceRecord(
                instanceId, definition.id(), customerId,
                "VC" + stableToken(instanceId),
                sourceType, sourceRef, VoucherStateMachine.ISSUED, now,
                VoucherStateMachine.computeExpiry(now, definition.validDays(), definition.validUntil()),
                null, null, null, reserved, now, now);
        boolean created = repository.insertInstance(instance);
        if (!created && reserved) {
            // 幂等竞态输家（并发同键已写入实例，ON CONFLICT DO NOTHING 返回 false）：回退我刚占的预占，避免双占。
            inventoryService.releaseStockReservation(
                    definition.inventoryItemId(), definition.stockLocationId(), reserveQty);
        }
        return repository.findInstanceById(instanceId).orElse(instance);
    }

    /** 实物券每张核销/预占消耗的实物件数（定义 stock_quantity；缺省 1）。 */
    private static int goodsQuantity(VoucherDefinitionRecord definition) {
        return definition.stockQuantity() == null ? 1 : definition.stockQuantity();
    }

    // ── 作废 ───────────────────────────────────────────────────────────────

    @Transactional
    public VoucherInstanceRecord revoke(String instanceId, VoucherRevokeRequest request) {
        authorizationService.requirePermission(MANAGE_PERM);
        requireRequest(request);
        String id = requireText(instanceId, "voucher_instance_id");
        return operations.executeWrite(
                "revokeVoucher:" + id,
                request.idempotencyKey(),
                request,
                VoucherInstanceRecord.class,
                AuditedOperation.of("voucher.revoked", "voucher.revoke_failed", MANAGE_PERM, BUSINESS_TYPE),
                () -> revokeInternal(id, request),
                VoucherService::businessId,
                Map.of());
    }

    private VoucherInstanceRecord revokeInternal(String instanceId, VoucherRevokeRequest request) {
        String reason = requireText(request.reason(), "reason");
        VoucherInstanceRecord instance = repository.findInstanceById(instanceId)
                .orElseThrow(() -> ApiException.notFound("VOUCHER_NOT_FOUND", "voucher instance not found"));
        VoucherDefinitionRecord definition = repository.findDefinitionById(instance.definitionId())
                .orElseThrow(() -> ApiException.notFound("VOUCHER_DEFINITION_NOT_FOUND", "voucher definition not found"));
        checkMerchantOwnership(definition.merchantId());
        if (!VoucherStateMachine.canTransition(instance.status(), VoucherStateMachine.REVOKED)) {
            throw ApiException.conflict("VOUCHER_NOT_REVOCABLE",
                    "仅未核销（issued）的券可作废，当前状态：" + instance.status());
        }
        Instant now = Instant.now();
        if (repository.updateInstanceStatus(instanceId, VoucherStateMachine.ISSUED, VoucherStateMachine.REVOKED, now, reason) == 0) {
            throw ApiException.conflict("VOUCHER_NOT_REVOCABLE", "voucher state changed concurrently");
        }
        // V312：作废店内实物券即释放其预占（reserved->available），把库存还给可用池。
        if (instance.stockReserved() && "in_store_goods".equals(definition.voucherKind())) {
            inventoryService.releaseStockReservation(
                    definition.inventoryItemId(), definition.stockLocationId(), goodsQuantity(definition));
        }
        // V313：作废券即作废其在途转赠（解冻受赠人的待领取）。
        transferRepository.voidPendingForInstance(instanceId, "voucher_revoked", now);
        return repository.findInstanceById(instanceId).orElseThrow();
    }

    // ── V312 过期预占释放（定时作业）───────────────────────────────────────

    /**
     * 过期释放扫描（{@link VoucherReservationExpiryJob} 调用）：把 issued+已预占+已过期的券逐条 CAS 过期并释放预占，
     * 避免过期未核销的实物券把库存永久占死（惰性过期口径下这是预占的兜底回收）。返回本轮实际释放的券数。
     */
    @Transactional
    public int expireReservedVouchers(Instant now, int limit) {
        int released = 0;
        for (VoucherInstanceRecord instance : repository.listExpiredReservedIssuedInstances(now, safeSize(limit))) {
            if (repository.updateInstanceStatus(instance.id(), VoucherStateMachine.ISSUED,
                    VoucherStateMachine.EXPIRED, now, null) == 0) {
                continue; // 并发已被核销/撤回/别的扫描迁走
            }
            repository.findDefinitionById(instance.definitionId()).ifPresent(def ->
                    inventoryService.releaseStockReservation(
                            def.inventoryItemId(), def.stockLocationId(), goodsQuantity(def)));
            // V313：过期券即作废其在途转赠。
            transferRepository.voidPendingForInstance(instance.id(), "voucher_expired", now);
            released++;
        }
        return released;
    }

    // ── 统一核销（三类核销在这层真正建成）─────────────────────────────────

    @Transactional
    public VoucherRedemptionEntryRecord redeem(VoucherRedeemRequest request) {
        authorizationService.requirePermission(REDEEM_PERM);
        requireRequest(request);
        return operations.executeWrite(
                "redeemVoucher",
                request.idempotencyKey(),
                request,
                VoucherRedemptionEntryRecord.class,
                AuditedOperation.of("voucher.redeemed", "voucher.redeem_failed", REDEEM_PERM, BUSINESS_TYPE),
                () -> redeemInternal(request),
                VoucherService::businessId,
                Map.of());
    }

    private VoucherRedemptionEntryRecord redeemInternal(VoucherRedeemRequest request) {
        Instant now = Instant.now();
        VoucherInstanceRecord instance = resolveInstance(request);
        VoucherDefinitionRecord definition = repository.findDefinitionById(instance.definitionId())
                .orElseThrow(() -> ApiException.notFound("VOUCHER_DEFINITION_NOT_FOUND", "voucher definition not found"));
        // 越权防：定义绑定商户的券，商户 scope 账号只能核自家；平台通用券（merchant_id 空）任意核销点可核。
        checkMerchantOwnership(definition.merchantId());
        String block = VoucherStateMachine.redeemBlockReason(instance.status(), instance.expiresAt(), now);
        if (block != null) {
            if ("VOUCHER_EXPIRED".equals(block) && VoucherStateMachine.ISSUED.equals(instance.status())) {
                // 惰性过期落库（核销路径顺手转正），失败不影响拒绝结论。V312：转正成功且该券持预占则一并释放预占。
                if (repository.updateInstanceStatus(instance.id(), VoucherStateMachine.ISSUED,
                        VoucherStateMachine.EXPIRED, now, null) > 0
                        && instance.stockReserved() && "in_store_goods".equals(definition.voucherKind())) {
                    inventoryService.releaseStockReservation(
                            definition.inventoryItemId(), definition.stockLocationId(), goodsQuantity(definition));
                }
            }
            throw ApiException.conflict(block, "voucher is not redeemable: " + block);
        }
        // V313 转赠冻结：券有在途转赠（pending）时不能核销，须持券人先撤回（与领取端 CAS 构成双花双闸）。
        if (transferRepository.existsPendingForInstance(instance.id())) {
            throw ApiException.conflict("VOUCHER_TRANSFERRING",
                    "该券正在转赠中，不能核销（请让持券人先撤回转赠）");
        }
        // 店内实物券：先做库存前置校验（R3 P0-7 同款顺序纪律——所有前置校验完成前不动任何状态），
        // 缺货直接拒绝，券保持 issued 可换点再核；真实出库在 CAS 占位之后、同事务内执行。
        int goodsQuantity = 0;
        if ("in_store_goods".equals(definition.voucherKind())) {
            goodsQuantity = goodsQuantity(definition);
            if (!instance.stockReserved()) {
                // 历史券（本轮 V312 前发放，未预占）：仍走旧 available 直扣前置校验；缺货直接拒绝，券保持 issued。
                int available = inventoryService.availableStock(definition.inventoryItemId(), definition.stockLocationId());
                if (available < goodsQuantity) {
                    throw ApiException.conflict("VOUCHER_STOCK_INSUFFICIENT",
                            "库存不足，本次核销未完成（券未被消耗）");
                }
            }
            // 已预占券：库存已在 reserved_quantity 中为本券留存，无需 available 前置校验（核销时转实扣）。
        }
        // CAS 占位：并发双核销第一道闸（第二道＝核销记录实例级唯一索引）。
        if (repository.updateInstanceStatus(instance.id(), VoucherStateMachine.ISSUED,
                VoucherStateMachine.REDEEMED, now, null) == 0) {
            throw ApiException.conflict("VOUCHER_ALREADY_REDEEMED", "voucher was redeemed concurrently");
        }
        String redemptionId = "vrr_" + instance.id();
        String stockMovementRef = null;
        if ("in_store_goods".equals(definition.voucherKind())) {
            // 店内实物券：核销时才出库（兑换只发券）。经 IdempotencyService 的 TransactionTemplate（REQUIRED）
            // 并入本事务，出库失败整体回滚、券状态一并回到 issued。
            // V312：已预占券先把预占还回 available（reserved->available），再走 redeem_consume（available->出库+quantity-=n），
            // 净效果 = 预占转实扣（reserved-=n, quantity-=n）。同事务同一余额行行锁串行，中途另一笔抢不走这份库存。
            if (instance.stockReserved()) {
                inventoryService.releaseStockReservation(
                        definition.inventoryItemId(), definition.stockLocationId(), goodsQuantity);
            }
            stockMovementRef = "voucher-redeem-" + instance.id();
            inventoryService.createTypedStockMovement("redeem_consume", new StockMovementRequest(
                    stockMovementRef,
                    "unified voucher redemption " + instance.id(),
                    List.of(),
                    definition.stockLocationId(),
                    null,
                    List.of(Map.of(
                            "item_id", definition.inventoryItemId(),
                            "quantity", goodsQuantity
                    )),
                    "voucher_redemption:" + instance.id()));
        }
        Long settlementAmount = null;
        String arApItemId = null;
        if ("internal_price".equals(definition.settlementMode()) && definition.internalPriceCent() != null) {
            // 内部结算：按内部结算价给券归属商户挂应付（PartnerVoucherSettlementPort 内部价范式复用）。
            settlementAmount = definition.internalPriceCent();
            if (settlementPort != null) {
                arApItemId = settlementPort.createPartnerVoucherPayable(
                        definition.merchantId(), settlementAmount, redemptionId);
            }
        }
        VoucherRedemptionEntryRecord entry = new VoucherRedemptionEntryRecord(
                redemptionId, instance.id(), definition.id(), definition.voucherKind(), instance.customerId(),
                definition.merchantId() != null
                        ? definition.merchantId()
                        : authorizationService.currentMerchantScope().orElse(null),
                currentOperator(), definition.redeemMethod(), definition.settlementMode(),
                settlementAmount, arApItemId, stockMovementRef, null,
                blankToNull(request.note()), now, now);
        if (!repository.insertRedemption(entry)) {
            throw ApiException.conflict("VOUCHER_ALREADY_REDEEMED", "redemption record already exists for this voucher");
        }
        return entry;
    }

    private VoucherInstanceRecord resolveInstance(VoucherRedeemRequest request) {
        String code = blankToNull(request.code());
        String instanceId = blankToNull(request.voucherInstanceId());
        if (code == null && instanceId == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "code or voucher_instance_id is required");
        }
        Optional<VoucherInstanceRecord> found =
                instanceId != null ? repository.findInstanceById(instanceId) : repository.findInstanceByCode(code);
        return found.orElseThrow(() -> ApiException.notFound("VOUCHER_NOT_FOUND", "voucher instance not found"));
    }

    // ── 工具 ───────────────────────────────────────────────────────────────

    /** 商户 scope 账号见（自家∪平台通用）券；平台账号无限制。 */
    private String merchantScope() {
        return authorizationService.currentMerchantScope().orElse(null);
    }

    /** FinanceService.checkMerchantOwnership 同款守卫：resourceMerchantId 空＝平台通用资源，放行。 */
    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && resourceMerchantId != null && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "access to another merchant's voucher is not allowed");
        }
    }

    private String currentOperator() {
        return authorizationService.currentActor().id();
    }

    private static String businessId(Object saved) {
        if (saved instanceof VoucherRedemptionEntryRecord entry) {
            return entry.voucherInstanceId();
        }
        if (saved instanceof VoucherInstanceRecord instance) {
            return instance.id();
        }
        if (saved instanceof VoucherDefinitionRecord definition) {
            return definition.id();
        }
        if (saved instanceof VoucherIssueResultRecord result) {
            return result.definitionId();
        }
        return "all";
    }

    private static void requireRequest(Object request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return value.trim();
    }

    private static String requireEnum(String value, Set<String> allowed, String field) {
        String trimmed = requireText(value, field);
        if (!allowed.contains(trimmed)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be one of " + allowed);
        }
        return trimmed;
    }

    private static void requireOptionalEnum(String value, Set<String> allowed, String field) {
        if (value != null && !value.isBlank() && !allowed.contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "unknown " + field + ": " + value);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static int safePage(int page) {
        return Math.max(page, 1);
    }

    private static int safeSize(int pageSize) {
        return Math.min(Math.max(pageSize, 1), 200);
    }

    private static String stableToken(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
    }
}
