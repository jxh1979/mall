package com.complex.platform.voucher;

// 文件版本：V0.02

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.customer.CustomerRedemptionCatalogItemRecord;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.inventory.InventoryService;
import com.complex.platform.inventory.dto.StockMovementRequest;
import com.complex.platform.voucher.dto.VoucherDefinitionRequest;
import com.complex.platform.voucher.dto.VoucherIssueRequest;
import com.complex.platform.voucher.dto.VoucherRedeemRequest;
import com.complex.platform.voucher.dto.VoucherRevokeRequest;
import com.complex.platform.ticketing.TicketingRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
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
import static com.complex.platform.common.Strings.requireText;

/**
 * 积分/券统一台账（方案A，2026-07-18 拍板）：券定义/券实例/统一核销记录。
 *
 * <p>收编：积分兑换券（实现 {@link VoucherIssuePort}，redeemPoints 只发券不动库存）＋营销赠券
 * （后台发放入口，此前系统无营销赠券实现、无历史券）。通票权益与第三方合作券（V282）不搬表，
 * {@link #overview} 读层聚合并入"全部券"。
 *
 * <p>三类核销×结算在核销路径真正建成：商户折扣券＝结账抵扣（不结算）；店内实物券＝核销时扣库存＋
 * 按内部结算价挂应付（复用 {@link FinanceService} 内部价范式）；外部伙伴券仍走 V282
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
    /** ROOT-03-R32 客人动态核销码前缀（固定券码以 VC 开头，二者不重叠）。 */
    static final String REDEEM_CODE_PREFIX = "vrc_";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VoucherRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditedOperationExecutor operations;
    private final InventoryService inventoryService;
    // finance 结算端口（PartnerVoucherService 同款：@Lazy 防环；null＝兼容单测不结算）。
    private final FinanceService settlementPort;
    // V313 券转赠：作废/过期时作废在途转赠；操作员裁决第十三批第 9 条「和门票一致不冻结」：核销不再被在途转赠挡住，核销成功即作废在途转赠。
    private final JdbcVoucherTransferRepository transferRepository;
    // ROOT-03-R32：动态码窗口＝全场统一刷新间隔（BSM 第 585 行「这个刷新间隔全场用同一套统一设置」）；未注入（单测）时不签发。
    private TicketingRepository ticketingRepository;

    @Autowired(required = false)
    void setTicketingRepository(TicketingRepository ticketingRepository) {
        this.ticketingRepository = ticketingRepository;
    }

    @Autowired
    public VoucherService(
            VoucherRepository repository,
            AuthorizationService authorizationService,
            AuditedOperationExecutor operations,
            InventoryService inventoryService,
            @Lazy FinanceService settlementPort,
            JdbcVoucherTransferRepository transferRepository) {
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
     * 只读展示，不做客人端发起核销（核销在 POS/工作手机侧）。ROOT-03-R32：不下发固定券码，客人出示动态码（{@link #issueCustomerRedeemCode}）。
     */
    public PageResponse<VoucherOverviewRecord> customerOverview(String customerId, int page, int pageSize) {
        String scoped = blankToNull(customerId);
        if (scoped == null) {
            throw ApiException.unauthorized("CUSTOMER_REQUIRED", "客人登录凭证缺失，请重新登录");
        }
        return operations.readPageResult(
                AuditedOperation.of("voucher.customer_wallet_viewed", "voucher.customer_wallet_view_failed", VIEW_PERM, BUSINESS_TYPE),
                () -> {
                    PageResponse<VoucherOverviewRecord> rows = repository.overview(null, scoped, safePage(page), safeSize(pageSize));
                    return new PageResponse<>(rows.items().stream().map(VoucherOverviewRecord::withoutCode).toList(),
                            rows.page(), rows.pageSize(), rows.total());
                });
    }

    /**
     * ROOT-03-R32＝clients-R41（BSM 第 585 行「客人出示的……权益核销码……是每隔一段时间自动更新的动态码，有效期（刷新间隔）由后台统一设置……
     * 截图、转发出去的旧码过了有效期就作废」）：本人持有、当前可核销的统一台账券签发动态核销码；同一张券同时只一枚有效码（签发前撤旧码），
     * 明文只回给客人、库里只存 SHA-256。客人会话已由控制器校验。
     */
    @Transactional
    public VoucherRedeemCodeResponse issueCustomerRedeemCode(String instanceId, String customerId) {
        return operations.readSingle(
                AuditedOperation.of("voucher.redeem_code_issued", "voucher.redeem_code_issue_failed", "not_applicable", BUSINESS_TYPE),
                () -> {
                    VoucherInstanceRecord instance = repository.findInstanceById(requireText(instanceId, "voucher_instance_id"))
                            .orElseThrow(() -> ApiException.notFound("VOUCHER_NOT_FOUND", "券不存在或已被删除"));
                    if (!requireText(customerId, "customer_id").equals(instance.customerId())) {
                        throw ApiException.forbidden("VOUCHER_NOT_HOLDER", "该券不属于本人");
                    }
                    Instant now = Instant.now();
                    String block = VoucherStateMachine.redeemBlockReason(instance.status(), instance.expiresAt(), now);
                    if (block != null) {
                        throw ApiException.conflict(block, "该券当前不可使用：" + block);
                    }
                    if (ticketingRepository == null) {
                        throw ApiException.conflict("VOUCHER_REDEEM_CODE_UNAVAILABLE", "动态券码刷新间隔未配置");
                    }
                    int window = ticketingRepository.findQrRefreshIntervalSeconds();
                    repository.revokeActiveRedeemCodes(instance.id());
                    byte[] random = new byte[16];
                    RANDOM.nextBytes(random);
                    String code = REDEEM_CODE_PREFIX + HexFormat.of().formatHex(random);
                    repository.insertRedeemCode("vrcid_" + UUID.randomUUID().toString().replace("-", ""), instance.id(), customerId,
                            sha256Hex(code), now, now.plusSeconds(window), window);
                    return new VoucherRedeemCodeResponse(instance.id(), code, now.plusSeconds(window), window);
                },
                issued -> ((VoucherRedeemCodeResponse) issued).voucherInstanceId());
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
                throw ApiException.badRequest("VALIDATION_FAILED", "商户折扣券必须填写大于 0 的面值（face_value_cent）");
            }
            if (merchantId == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "商户折扣券必须指定商户（merchant_id）");
            }
        }
        if ("in_store_goods".equals(kind)) {
            requireText(request.inventoryItemId(), "inventory_item_id");
            requireText(request.stockLocationId(), "stock_location_id");
            if (request.stockQuantity() == null || request.stockQuantity() <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "到店商品券必须填写大于 0 的库存数量（stock_quantity）");
            }
        }
        // 批10/E(V380)：内部结算价唯一可编辑处 = SKU 目录。直填一律拒绝(静默忽略会骗运营"以为配好了")。
        if (request.internalPriceCent() != null) {
            throw ApiException.badRequest("VOUCHER_INTERNAL_PRICE_CATALOG_ONLY",
                    "内部结算价已收编进内部结算 SKU 目录：请引用目录条目，改价到目录走草案复核");
        }
        String catalogItemCode = blankToNull(request.catalogItemCode());
        Long internalPriceSnapshot = null;
        if ("internal_price".equals(mode)) {
            if (merchantId == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "internal_price requires merchant_id (结算商户)");
            }
            if (catalogItemCode == null) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "internal_price 结算口径须引用内部结算 SKU 目录条目(catalog_item_code)");
            }
            String boundMerchant = repository.findCatalogItemMerchant(catalogItemCode)
                    .orElseThrow(() -> ApiException.badRequest("CATALOG_ITEM_NOT_FOUND",
                            "所引用的内部结算 SKU 目录条目不存在"));
            if (!boundMerchant.equals(merchantId)) {
                throw ApiException.badRequest("CATALOG_ITEM_MERCHANT_MISMATCH",
                        "目录条目的绑定商户与本券结算商户不一致，不得跨商户/跨项目引用");
            }
            // 券口径(V301 形状约束)要求建券即有 >0 内部价:目录条目须已复核转正且价 >0,草案态先转正再建券。
            internalPriceSnapshot = repository.findActiveCatalogPriceCent(catalogItemCode)
                    .orElseThrow(() -> ApiException.badRequest("CATALOG_ITEM_NOT_EFFECTIVE",
                            "所引用的目录条目还没有复核转正的生效价，请先在结算配置中心完成复核"));
            if (internalPriceSnapshot <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED",
                        "internal_price 结算口径要求目录生效价 > 0(0 价属商户让利,不该按内部价挂应付)");
            }
        } else if (catalogItemCode != null) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "仅 internal_price 结算口径可引用内部结算 SKU 目录条目");
        }
        if (request.validDays() != null && request.validDays() <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "有效天数（valid_days）如填写必须大于 0");
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
                // 批10/E:快照列(只读)——建券落当刻目录生效价,此后由目录转正传播维护;券侧无任何改价入口。
                internalPriceSnapshot, request.validDays(), request.validUntil(),
                "active", transferable, now, now);
        repository.insertDefinition(definition);
        if (catalogItemCode != null) {
            repository.setDefinitionCatalogRef(id, catalogItemCode);
        }
        return repository.findDefinitionById(id)
                .orElseThrow(() -> ApiException.conflict("VOUCHER_DEFINITION_CONFLICT", "券定义创建时发生并发冲突，请重试"));
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
            throw ApiException.badRequest("VALIDATION_FAILED", "发放数量必须介于 1 到 " + MAX_ISSUE_QUANTITY + " 之间");
        }
        VoucherDefinitionRecord definition = repository.findDefinitionById(definitionId)
                .orElseThrow(() -> ApiException.notFound("VOUCHER_DEFINITION_NOT_FOUND", "券定义不存在或已被删除"));
        if (!"active".equals(definition.status())) {
            throw ApiException.conflict("VOUCHER_DEFINITION_DISABLED", "该券定义已停用");
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
                .orElseThrow(() -> ApiException.conflict("VOUCHER_DEFINITION_CONFLICT", "派生券定义创建时发生并发冲突，请重试"));
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
                .orElseThrow(() -> ApiException.notFound("VOUCHER_NOT_FOUND", "券不存在或已被删除"));
        VoucherDefinitionRecord definition = repository.findDefinitionById(instance.definitionId())
                .orElseThrow(() -> ApiException.notFound("VOUCHER_DEFINITION_NOT_FOUND", "券定义不存在或已被删除"));
        checkMerchantOwnership(definition.merchantId());
        if (!VoucherStateMachine.canTransition(instance.status(), VoucherStateMachine.REVOKED)) {
            throw ApiException.conflict("VOUCHER_NOT_REVOCABLE",
                    "仅未核销（issued）的券可作废，当前状态：" + instance.status());
        }
        Instant now = Instant.now();
        if (repository.updateInstanceStatus(instanceId, VoucherStateMachine.ISSUED, VoucherStateMachine.REVOKED, now, reason) == 0) {
            throw ApiException.conflict("VOUCHER_NOT_REVOCABLE", "券状态已被其他操作变更，请刷新后重试");
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

    /**
     * ROOT-03-R21（BSM 第 863 行「注销后……积分、券一律清零」）：本人全部未核销券作废（原因 account_deregistered），与单张作废同一写法——
     * 释放实物券预占、作废在途转赠；客人自身原因作废，不退积分。客人会话已由调用方校验。返回作废张数。
     */
    @Transactional
    public int revokeAllOnDeregistration(String customerId, Instant now) {
        int revoked = 0;
        // ponytail: 一页 1000 张即一个客人的全部未核销券；真有超过的再分页。
        for (VoucherInstanceRecord instance : repository.listInstances(null, customerId, null, VoucherStateMachine.ISSUED, null,
                1, 1000).items()) {
            if (repository.updateInstanceStatus(instance.id(), VoucherStateMachine.ISSUED, VoucherStateMachine.REVOKED, now,
                    "account_deregistered") == 0) {
                continue;
            }
            repository.findDefinitionById(instance.definitionId())
                    .filter(definition -> instance.stockReserved() && "in_store_goods".equals(definition.voucherKind()))
                    .ifPresent(definition -> inventoryService.releaseStockReservation(
                            definition.inventoryItemId(), definition.stockLocationId(), goodsQuantity(definition)));
            transferRepository.voidPendingForInstance(instance.id(), "voucher_revoked", now);
            revoked++;
        }
        return revoked;
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
        return redeemInternal(request.code(), request.voucherInstanceId(), request.note(), null);
    }

    /** 第十批 TK-28：结账抵扣上下文——账单归属商户 / 账单标识 / 本次可抵扣的应付额（不含小费）。 */
    private record BillDeduction(String merchantId, String billType, String billId, long deductibleCent) {
    }

    /**
     * 第十批 TK-28-R1（ENUM L81「商户折扣券——到对应商户抵扣」；ENUM L83「券抵扣部分不折现」；PGM L303）：
     * 结账路径核销一张商户折扣券。复用 {@link #redeemInternal} 全部闸（状态/过期/CAS/实例级唯一索引），
     * 额外要求券种为 merchant_discount 且券绑定商户＝账单商户（否则 VOUCHER_MERCHANT_MISMATCH，券不消耗）；
     * 抵扣额＝min(面值, 可抵扣应付)，面值多出部分不找零不退。不走后台 REDEEM_PERM 与独立幂等——由结账权限与结账事务承接，
     * 结账任一步失败整体回滚、券回到 issued。
     */
    public VoucherRedemptionEntryRecord redeemForBill(
            String code, String instanceId, String billMerchantId, String billType, String billId, long deductibleCent) {
        return redeemInternal(code, instanceId, null,
                new BillDeduction(requireText(billMerchantId, "merchant_id"), billType, requireText(billId, "bill_id"), deductibleCent));
    }

    private VoucherRedemptionEntryRecord redeemInternal(String rawCode, String rawInstanceId, String note, BillDeduction bill) {
        Instant now = Instant.now();
        String code = blankToNull(rawCode);
        String instanceId = blankToNull(rawInstanceId);
        VoucherInstanceRecord instance = resolveInstance(code, instanceId);
        VoucherDefinitionRecord definition = repository.findDefinitionById(instance.definitionId())
                .orElseThrow(() -> ApiException.notFound("VOUCHER_DEFINITION_NOT_FOUND", "券定义不存在或已被删除"));
        // 第十批 TK-28：酒吧商户的折扣券只能在结账时抵扣核销（独立核销会让券被消耗而账单全价）；结账路径只收折扣券。
        // 操作员裁决 2026-10-20 第三轮第 9 条「非酒吧保留独立核销」：只对券定义商户为酒吧（merchant_type=bar）的生效，
        // 餐饮、零售商户的折扣券仍由商户在后台独立核销（这些商户的收银不在本系统）。
        if (bill == null && "merchant_discount".equals(definition.voucherKind()) && "bar".equals(definition.merchantType())) {
            throw ApiException.badRequest("VOUCHER_REQUIRES_BILL", "商户折扣券须在结账时抵扣核销，不能单独核销");
        }
        if (bill != null) {
            if (!"merchant_discount".equals(definition.voucherKind())) {
                throw ApiException.badRequest("VOUCHER_KIND_NOT_DEDUCTIBLE", "只有商户折扣券可在结账时抵扣");
            }
            if (!bill.merchantId().equals(definition.merchantId())) {
                throw ApiException.badRequest("VOUCHER_MERCHANT_MISMATCH", "该券不属于本账单商户，不能抵扣");
            }
            if (bill.deductibleCent() <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "账单已无可抵扣的应付金额");
            }
        }
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
            throw ApiException.conflict(block, "该券当前不可核销：" + block);
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
            throw ApiException.conflict("VOUCHER_ALREADY_REDEEMED", "该券已被并发核销，请勿重复操作");
        }
        // 操作员裁决第十三批第 9 条「发起转赠后原持有人仍可自用……先成功的动作为准，其余自动作废」：核销成功即作废在途转赠
        // （领取方锁同一券行，后到者读到已作废；VoucherTransferService）。
        transferRepository.voidPendingForInstance(instance.id(), VoucherTransferService.VOID_HOLDER_REDEEMED, now);
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
            // 内部结算：按内部结算价给券归属商户挂应付（FinanceService 内部价范式复用）。
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
                blankToNull(note), now, now,
                bill == null ? null : bill.billType(),
                bill == null ? null : bill.billId(),
                bill == null ? null : Math.min(definition.faceValueCent(), bill.deductibleCent()));
        if (!repository.insertRedemption(entry)) {
            throw ApiException.conflict("VOUCHER_ALREADY_REDEEMED", "该券已有核销记录，请勿重复核销");
        }
        if (instanceId == null && code != null) {
            repository.consumeRedeemCode(sha256Hex(code.trim()), redemptionId);
        }
        return entry;
    }

    /**
     * 按券编号（员工台账查券）或客人出示的码定位券。ROOT-03-R32（BSM 第 585 行）：客人出示的只认未过期的动态码（vrc_ 前缀，且码签发对象仍是
     * 当前持有人——转赠完成即失效）；统一台账券都有持有客人，凭固定券码一律不再核销。
     */
    private VoucherInstanceRecord resolveInstance(String rawCode, String rawInstanceId) {
        String code = blankToNull(rawCode);
        String instanceId = blankToNull(rawInstanceId);
        if (code == null && instanceId == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "券码或券编号必须至少填写一项");
        }
        if (instanceId != null) {
            repository.lockInstance(instanceId);
            return repository.findInstanceById(instanceId)
                    .orElseThrow(() -> ApiException.notFound("VOUCHER_NOT_FOUND", "券不存在或已被删除"));
        }
        if (!code.startsWith(REDEEM_CODE_PREFIX)) {
            repository.findInstanceByCode(code).orElseThrow(() -> ApiException.notFound("VOUCHER_NOT_FOUND", "券不存在或已被删除"));
            throw ApiException.conflict("VOUCHER_DYNAMIC_CODE_REQUIRED", "请客人出示券包里的动态券码");
        }
        VoucherRepository.ActiveRedeemCode active = repository.findActiveRedeemCode(sha256Hex(code), Instant.now())
                .orElseThrow(VoucherService::redeemCodeExpired);
        // 操作员裁决第十三批第 9 条「先成功的动作为准」：锁券行后再读持有人——与领取并发时等领取提交，券已归对方即码失效。
        repository.lockInstance(active.voucherInstanceId());
        return repository.findInstanceById(active.voucherInstanceId())
                .filter(instance -> active.customerId().equals(instance.customerId()))
                .orElseThrow(VoucherService::redeemCodeExpired);
    }

    private static ApiException redeemCodeExpired() {
        return ApiException.conflict("VOUCHER_REDEEM_CODE_EXPIRED", "券码已过期或已失效，请客人刷新后重新出示");
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
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
                    "无权操作其他商户的券");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
    }

    private static String requireEnum(String value, Set<String> allowed, String field) {
        String trimmed = requireText(value, field);
        if (!allowed.contains(trimmed)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 的取值必须是以下之一：" + allowed);
        }
        return trimmed;
    }

    private static void requireOptionalEnum(String value, Set<String> allowed, String field) {
        if (value != null && !value.isBlank() && !allowed.contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "无法识别的 " + field + "：" + value);
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
