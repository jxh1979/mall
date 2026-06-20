package com.complex.platform.inventory;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.RiskAlertService;
import com.complex.platform.evidence.TraceabilityProcessInstanceRecord;
import com.complex.platform.evidence.TraceabilityService;
import com.complex.platform.evidence.TraceabilityStepRecord;
import com.complex.platform.evidence.dto.RiskAlertReportRequest;
import com.complex.platform.gate.TerminalSwitchGuard;
import com.complex.platform.inventory.dto.InventoryItemRequest;
import com.complex.platform.inventory.dto.InventorySupplierRequest;
import com.complex.platform.inventory.dto.InventoryRiskScanRequest;
import com.complex.platform.inventory.dto.InventoryStockBatchRequest;
import com.complex.platform.inventory.dto.PurchaseRequestCreate;
import com.complex.platform.inventory.dto.RegisterTraceCodeRequest;
import com.complex.platform.inventory.dto.SavedBottleCancelRequest;
import com.complex.platform.inventory.dto.SavedBottleClaimRequest;
import com.complex.platform.inventory.dto.SavedBottleRequest;
import com.complex.platform.inventory.dto.StockAdjustmentRequest;
import com.complex.platform.inventory.dto.StockInRequest;
import com.complex.platform.inventory.dto.StockMovementRequest;
import com.complex.platform.inventory.dto.SupplierReconciliationRequest;
import com.complex.platform.inventory.dto.TraceExceptionReviewRequest;
import com.complex.platform.inventory.dto.TraceHandoverCreateRequest;
import com.complex.platform.inventory.dto.TraceRobotReportRequest;
import com.complex.platform.inventory.dto.TraceScanSegmentRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final Set<String> STOCK_BATCH_STATUSES = Set.of("active", "consumed", "expired", "archived");
    private static final Set<String> SUPPLIER_RECONCILIATION_STATUSES = Set.of("pending", "matched", "difference_found");

    // R5 T17：五段交接链（seq 1..5）。dispatch 段为机器人被动留痕，其余为员工主动扫码。
    private static final List<String> HANDOVER_SEGMENTS =
            List.of("out_verify", "dispatch", "kitchen_receive", "staff_pickup", "deliver_customer");
    private static final String ROBOT_SEGMENT = "dispatch";
    private static final String ROBOT_SYNC_CAPABILITY = "integration.robot.sync";

    private final InventoryRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final InventoryStockMovementService stockMovementService;
    private final TerminalSwitchGuard terminalSwitchGuard;
    private final InventoryHighValueReviewService highValueReviewService;
    private final TraceabilityService traceabilityService;
    // P1：evidence 统一异常责任链服务。库存风险扫描命中 + 溯源交接断链异常，除写孤岛表外再接入统一
    // risk_alerts（source_domain='inventory'），让统一表真实承载本域风险数据。复刻 Wave 1 Wecom 先例。
    // 可为 null（向后兼容/旧 6 参构造器/单测无注入）；接入为 fail-safe，不阻塞主链。
    private final RiskAlertService riskAlertService;

    // 本域风险级 → 统一表 RISK_LEVELS({normal,suspicious,high,critical}) 映射。
    // inventory 用的 medium 在统一表无对应枚举，必须映射为 suspicious，否则 report 抛 VALIDATION_FAILED。
    private static final Map<String, String> RISK_LEVEL_TO_UNIFIED = Map.of(
            "medium", "suspicious",
            "high", "high");

    // 向后兼容旧 6 参构造器：未注入统一责任链服务（riskAlertService=null），双写短路、仅孤岛表权威。
    public InventoryService(
            InventoryRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            TerminalSwitchGuard terminalSwitchGuard,
            TraceabilityService traceabilityService
    ) {
        this(repository, idempotencyService, auditEventService, authorizationService,
                terminalSwitchGuard, traceabilityService, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public InventoryService(
            InventoryRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            TerminalSwitchGuard terminalSwitchGuard,
            TraceabilityService traceabilityService,
            RiskAlertService riskAlertService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.terminalSwitchGuard = terminalSwitchGuard;
        this.traceabilityService = traceabilityService;
        this.riskAlertService = riskAlertService;
        this.highValueReviewService = new InventoryHighValueReviewService(
                repository, idempotencyService, auditEventService, authorizationService);
        this.stockMovementService = new InventoryStockMovementService(
                repository, idempotencyService, auditEventService, authorizationService,
                highValueReviewService);
    }

    public PageResponse<InventoryItemRecord> listInventoryItems() {
        return readPage(
                "inventory.item.viewed",
                "inventory.item.viewed_failed",
                "inventory.stock.view",
                "inventory_item",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listInventoryItemsByMerchant)
                                .orElseGet(repository::listInventoryItems);
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stock.move")
    @Transactional
    public InventoryItemRecord createInventoryItem(InventoryItemRequest request) {
        return executeWrite(
                "createInventoryItem",
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryItemRecord.class,
                "inventory.item.created",
                "inventory.item.created_failed",
                "inventory.stock.move",
                "inventory_item",
                () -> {
                    InventoryItemRequest actual = requireRequest(request);
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    InventoryItemRecord item = new InventoryItemRecord(
                            "item_" + slug(actual.idempotencyKey()),
                            null,
                            requireText(actual.itemName(), "item_name"),
                            requireText(actual.category(), "category"),
                            requireText(actual.unit(), "unit"),
                            null,
                            false,
                            nonNegativeOrDefault(actual.minQuantity(), 0, "min_quantity"),
                            trimToDefault(actual.status(), "active"),
                            now,
                            now,
                            merchantId,
                            nonNegativeOrNull(actual.highValueThresholdCents(), "high_value_threshold_cents")
                    );
                    return repository.saveInventoryItem(item);
                }
        );
    }

    public PageResponse<PurchaseRequestRecord> listPurchaseRequests() {
        return readPage(
                "inventory.purchase_request_viewed",
                "inventory.purchase_request_view_failed",
                "inventory.purchase_request.view",
                "inventory_purchase_request",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listPurchaseRequestsByMerchant)
                                .orElseGet(repository::listPurchaseRequests);
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.purchase_request.manage")
    @Transactional
    public PurchaseRequestRecord createPurchaseRequest(PurchaseRequestCreate request) {
        return executeWrite(
                "createPurchaseRequest",
                request == null ? null : request.idempotencyKey(),
                request,
                PurchaseRequestRecord.class,
                "inventory.purchase_request_changed",
                "inventory.purchase_request_change_failed",
                "inventory.purchase_request.manage",
                "inventory_purchase_request",
                () -> {
                    PurchaseRequestCreate actual = requireRequest(request);
                    Map<String, Object> item = firstItem(actual.items(), "items");
                    String itemId = requireItemText(item, "item_id");
                    int quantity = requirePositiveItemQuantity(item, "quantity");
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    String slug = slug(actual.idempotencyKey());
                    return repository.savePurchaseRequest(new PurchaseRequestRecord(
                            "purchase_" + slug,
                            "PR" + now.toEpochMilli() + slug.substring(0, Math.min(6, slug.length())).toUpperCase(Locale.ROOT),
                            trimToNull(actual.supplierId()),
                            itemId,
                            quantity,
                            actual.expectedArrivalDate() == null ? LocalDate.now() : actual.expectedArrivalDate(),
                            // R5 T6：起点保留 'created'（兼容既有写入/测试），视为 submitted 同义起点；
                            // 审批流转入口 approve 从 created/submitted 均可进入。
                            "created",
                            safeList(actual.evidenceAttachmentIds()),
                            now,
                            now,
                            merchantId,
                            trimToNull(actual.supplyContractId()),
                            null,
                            null,
                            null,
                            null,
                            null
                    ));
                }
        );
    }

    public PageResponse<SavedBottleRecord> listSavedBottles() {
        return readPage(
                "bar.saved_bottle.viewed",
                "bar.saved_bottle.view_failed",
                "bar.saved_bottle.manage",
                "saved_bottle",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listSavedBottlesByMerchant)
                                .orElseGet(repository::listSavedBottles);
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage")
    @Transactional
    public SavedBottleRecord createSavedBottle(SavedBottleRequest request) {
        return executeWrite(
                "createSavedBottle",
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleRecord.class,
                "bar.saved_bottle.created",
                "bar.saved_bottle.create_failed",
                "bar.saved_bottle.manage",
                "saved_bottle",
                () -> {
                    SavedBottleRequest actual = requireRequest(request);
                    Map<String, Object> item = firstItem(actual.items(), "items");
                    String itemId = requireItemText(item, "item_id");
                    int quantity = requirePositiveItemQuantity(item, "quantity");
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    // P0#4 散客存酒（R6-D05）：customer_id 可空（不再 requireText 强制），允许无会员散客存酒。
                    // 登记必填核身三件套：姓名/昵称 + 瓶底标签值 + 登记照片证据（取酒核身依据）。
                    String customerId = trimToNull(actual.customerId());
                    String nameOrNickname = requireText(actual.customerNameOrNickname(), "customer_name_or_nickname");
                    String bottleLabelValue = requireText(actual.bottleLabelValue(), "bottle_label_value");
                    List<String> registrationPhotoRefs = safeList(actual.registrationPhotoRefs());
                    if (registrationPhotoRefs.isEmpty()) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "registration_photo_refs is required");
                    }
                    return repository.saveSavedBottle(new SavedBottleRecord(
                            "saved_" + slug(actual.idempotencyKey()),
                            customerId,
                            requireText(actual.barId(), "bar_id"),
                            itemId,
                            trimToDefault(textFromItem(item, "bottle_name"), itemId),
                            quantity,
                            quantity,
                            trimToDefault(textFromItem(item, "unit"), "bottle"),
                            actual.expireAt(),
                            "stored",
                            safeList(actual.evidenceAttachmentIds()),
                            now,
                            now,
                            merchantId,
                            Boolean.TRUE.equals(actual.isHighValue()),
                            actual.highValueThresholdCents(),
                            bottleLabelValue,
                            nameOrNickname,
                            trimToNull(actual.customerPhone()),
                            phoneHash(actual.customerPhone()),
                            registrationPhotoRefs,
                            trimToNull(actual.manualNote()),
                            null,
                            null,
                            null,
                            null
                    ));
                }
        );
    }

    /** 取酒核身用：手机号 SHA-256 哈希（精确检索，不依赖明文解密）。null/空返回 null。 */
    private static String phoneHash(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(phone.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    /** 重建存酒记录（携带 A1 扩展字段），仅改 status/remaining/updatedAt。 */
    private SavedBottleRecord savedBottleWith(SavedBottleRecord b, String status, int remaining, Instant updatedAt) {
        return new SavedBottleRecord(b.id(), b.customerId(), b.barProjectId(), b.itemId(), b.bottleName(),
                b.initialQuantity(), remaining, b.unit(), b.expireAt(), status, b.evidenceAttachmentIds(),
                b.createdAt(), updatedAt, b.merchantId(), b.isHighValue(), b.highValueThresholdCents(),
                b.bottleLabelValue(), b.customerNameOrNickname(), b.customerPhone(), b.customerPhoneHash(),
                b.registrationPhotoRefs(), b.manualNote(),
                b.transferredAt(), b.transferToNameOrNickname(), b.transferToPhone(), b.transferToPhoneHash());
    }

    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage", admissionAction = "inventory.saved_bottle_claim", objectType = "saved_bottle")
    @Transactional
    public SavedBottleClaimRecord claimSavedBottle(String savedBottleId, SavedBottleClaimRequest request) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return executeWrite(
                "claimSavedBottle:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleClaimRecord.class,
                "bar.saved_bottle_picked_up",
                "bar.saved_bottle.pickup_failed",
                "bar.saved_bottle.manage",
                "saved_bottle_claim",
                () -> {
                    SavedBottleClaimRequest actual = requireRequest(request);
                    // P1-3: verificationCode 必须提供，不可绕过
                    requireText(actual.verificationCode(), "verification_code");
                    // P0-2: SELECT FOR UPDATE 防止并发超领
                    SavedBottleRecord savedBottle = repository.findSavedBottleByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "saved bottle not found"));
                    // P0-2: 跨商户越权检查
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent() && !scope.get().equals(savedBottle.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "saved bottle does not belong to your merchant");
                    }
                    if (!"stored".equals(savedBottle.status())) {
                        throw ApiException.conflict("SAVED_BOTTLE_NOT_CLAIMABLE", "saved bottle is not claimable");
                    }
                    if (savedBottle.expireAt() != null && savedBottle.expireAt().isBefore(Instant.now())) {
                        throw ApiException.conflict("SAVED_BOTTLE_EXPIRED", "saved bottle is expired");
                    }
                    // 取酒核身（REQ-30 + P0#4 F4/F7）：登记了瓶底标签值/姓名昵称时，取酒必须比对一致；
                    // 任一不符进入人工复核（不出酒）。比对项（取请求显式核身字段，缺省回退 verificationCode 兼容旧端）：
                    //   1) 瓶底标签值 bottleLabelValue（缺省回退 verificationCode）必须 == 登记标签值；
                    //   2) 姓名/昵称 customerNameOrNickname 必须 == 登记姓名/昵称（登记了才校验）。
                    Instant nowReview = Instant.now();
                    String claimLabel = trimToNull(actual.bottleLabelValue());
                    if (claimLabel == null) {
                        claimLabel = actual.verificationCode().trim();
                    }
                    String claimName = trimToNull(actual.customerNameOrNickname());
                    boolean labelMismatch = savedBottle.bottleLabelValue() != null
                            && !savedBottle.bottleLabelValue().equals(claimLabel);
                    boolean nameMismatch = savedBottle.customerNameOrNickname() != null
                            && !savedBottle.customerNameOrNickname().equals(claimName);
                    if (labelMismatch || nameMismatch) {
                        repository.saveSavedBottle(savedBottleWith(
                                savedBottle, "pending_identity_review", savedBottle.remainingQuantity(), nowReview));
                        return repository.saveSavedBottleClaim(new SavedBottleClaimRecord(
                                "claim_" + slug(actual.idempotencyKey()),
                                savedBottle.id(),
                                0,
                                trimToNull(actual.claimByPhone()),
                                "staff_work_phone",
                                null,
                                "pending_identity_review",
                                safeList(actual.evidenceAttachmentIds()),
                                nowReview
                        ));
                    }
                    Map<String, Object> claimItem = firstItem(actual.claimItems(), "claim_items");
                    int quantity = requirePositiveItemQuantity(claimItem, "quantity");
                    if (quantity > savedBottle.remainingQuantity()) {
                        throw ApiException.conflict("SAVED_BOTTLE_INSUFFICIENT", "saved bottle remaining quantity is insufficient");
                    }
                    int remaining = savedBottle.remainingQuantity() - quantity;
                    Instant now = Instant.now();
                    repository.saveSavedBottle(savedBottleWith(
                            savedBottle, remaining == 0 ? "claimed" : "stored", remaining, now));
                    return repository.saveSavedBottleClaim(new SavedBottleClaimRecord(
                            "claim_" + slug(actual.idempotencyKey()),
                            savedBottle.id(),
                            quantity,
                            trimToNull(actual.claimByPhone()),
                            "staff_work_phone",
                            null,
                            "picked_up",
                            safeList(actual.evidenceAttachmentIds()),
                            now
                    ));
                }
        );
    }

    /**
     * A1/REQ-32 + R5 T6：存酒作废（库存域内高价值复核闭环）。
     *
     * <p><b>高价值</b>存酒作废<b>不直接生效</b>：要求<b>照片 + 视频双证据</b>（{@code photoAttachmentIds} 与
     * {@code videoAttachmentIds} 各非空，真正区分类型，替换旧"附件数≥2 不分类型"弱校验）；命中后建库存域内高价值
     * 复核单（{@code inventory_high_value_reviews}，pending，subject_type=saved_bottle_void、subject_id=存酒 id），
     * 并把存酒置中间态 {@code disputed_frozen}（待主管复核冻结，不直接 cancelled）。主管经
     * {@link InventoryHighValueReviewService#review} approve 后存酒才真正置 {@code cancelled}；reject 则维持
     * {@code disputed_frozen}（不作废）。
     *
     * <p><b>非高价值</b>作废维持<b>直接 cancel</b>（向后兼容，无证据要求）。
     *
     * <p>中间态选用 {@code disputed_frozen}（V061 既有状态："争议冻结/待人工复核"语义，已在可作废源态集内），
     * 因本期无新迁移、{@code saved_bottles.status} CHECK 不含 pending_review。
     */
    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage", admissionAction = "inventory.saved_bottle_cancel", objectType = "saved_bottle")
    @Transactional
    public SavedBottleRecord cancelSavedBottle(String savedBottleId, SavedBottleCancelRequest request) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return executeWrite(
                "cancelSavedBottle:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleRecord.class,
                "bar.saved_bottle.cancelled",
                "bar.saved_bottle.cancel_failed",
                "bar.saved_bottle.manage",
                "saved_bottle",
                () -> {
                    SavedBottleCancelRequest actual = requireRequest(request);
                    SavedBottleRecord savedBottle = repository.findSavedBottleByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "saved bottle not found"));
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent() && !scope.get().equals(savedBottle.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "saved bottle does not belong to your merchant");
                    }
                    if (!List.of("stored", "pending_identity_review").contains(savedBottle.status())) {
                        throw ApiException.conflict("SAVED_BOTTLE_NOT_CANCELLABLE", "saved bottle is not cancellable");
                    }
                    if (savedBottle.isHighValue()) {
                        // 高价值：双证据（照片+视频各非空）→ 建 pending 复核单 + 存酒置中间态 disputed_frozen（不直接作废）。
                        highValueReviewService.createPending(
                                actual.idempotencyKey(),
                                InventoryHighValueReviewService.SUBJECT_SAVED_BOTTLE_VOID,
                                savedBottle.id(),
                                actual.photoAttachmentIds(),
                                actual.videoAttachmentIds());
                        return repository.saveSavedBottle(
                                savedBottleWith(savedBottle, "disputed_frozen", savedBottle.remainingQuantity(), Instant.now()));
                    }
                    // 非高价值：直接 cancel（向后兼容）。
                    return repository.saveSavedBottle(
                            savedBottleWith(savedBottle, "cancelled", savedBottle.remainingQuantity(), Instant.now()));
                }
        );
    }

    /**
     * P0#4 存酒转赠（R6-D05）：将 stored 状态存酒整瓶转赠给受让人。
     *
     * <p>原瓶置 {@code transferred} 终态；新建一瓶 {@code stored} 归属受让人（姓名昵称 +（可选）手机号），
     * 沿用剩余量/到期/标签/登记证据，受让人凭新瓶取酒核身。写 saved_bottle_transfers 流水留痕，
     * 发 {@code saved_bottle.transferred} 事件（由 executeWrite 成功事件承载）。
     */
    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage", admissionAction = "inventory.saved_bottle_transfer", objectType = "saved_bottle")
    @Transactional
    public SavedBottleRecord transferSavedBottle(
            String savedBottleId, com.complex.platform.inventory.dto.SavedBottleTransferRequest request) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return executeWrite(
                "transferSavedBottle:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleRecord.class,
                "bar.saved_bottle.transferred",
                "bar.saved_bottle.transfer_failed",
                "bar.saved_bottle.manage",
                "saved_bottle",
                () -> {
                    com.complex.platform.inventory.dto.SavedBottleTransferRequest actual = requireRequest(request);
                    String toName = requireText(actual.toNameOrNickname(), "to_name_or_nickname");
                    SavedBottleRecord savedBottle = repository.findSavedBottleByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "saved bottle not found"));
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent() && !scope.get().equals(savedBottle.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "saved bottle does not belong to your merchant");
                    }
                    if (!"stored".equals(savedBottle.status())) {
                        throw ApiException.conflict("SAVED_BOTTLE_NOT_TRANSFERABLE", "saved bottle is not transferable");
                    }
                    if (savedBottle.expireAt() != null && savedBottle.expireAt().isBefore(Instant.now())) {
                        throw ApiException.conflict("SAVED_BOTTLE_EXPIRED", "saved bottle is expired");
                    }
                    Instant now = Instant.now();
                    String toPhone = trimToNull(actual.toPhone());
                    // 原瓶置 transferred 终态，并落受让信息（便于追溯）。
                    repository.saveSavedBottle(new SavedBottleRecord(
                            savedBottle.id(), savedBottle.customerId(), savedBottle.barProjectId(),
                            savedBottle.itemId(), savedBottle.bottleName(), savedBottle.initialQuantity(),
                            savedBottle.remainingQuantity(), savedBottle.unit(), savedBottle.expireAt(),
                            "transferred", savedBottle.evidenceAttachmentIds(), savedBottle.createdAt(), now,
                            savedBottle.merchantId(), savedBottle.isHighValue(), savedBottle.highValueThresholdCents(),
                            savedBottle.bottleLabelValue(), savedBottle.customerNameOrNickname(),
                            savedBottle.customerPhone(), savedBottle.customerPhoneHash(),
                            savedBottle.registrationPhotoRefs(), savedBottle.manualNote(),
                            now, toName, toPhone, phoneHash(toPhone)));
                    // 流水留痕。
                    repository.saveSavedBottleTransfer(new SavedBottleTransferRecord(
                            "transfer_" + slug(actual.idempotencyKey()),
                            savedBottle.id(),
                            savedBottle.customerId(),
                            savedBottle.customerNameOrNickname(),
                            toName,
                            toPhone,
                            phoneHash(toPhone),
                            savedBottle.remainingQuantity(),
                            authorizationService.currentActor().id(),
                            "transferred",
                            safeList(actual.evidenceAttachmentIds()),
                            now));
                    // 新建归属受让人的存酒（stored），沿用登记信息，受让人后续取酒核身。
                    return repository.saveSavedBottle(new SavedBottleRecord(
                            "saved_" + slug(actual.idempotencyKey() + ":to"),
                            null,
                            savedBottle.barProjectId(),
                            savedBottle.itemId(),
                            savedBottle.bottleName(),
                            savedBottle.remainingQuantity(),
                            savedBottle.remainingQuantity(),
                            savedBottle.unit(),
                            savedBottle.expireAt(),
                            "stored",
                            safeList(actual.evidenceAttachmentIds()),
                            now,
                            now,
                            savedBottle.merchantId(),
                            savedBottle.isHighValue(),
                            savedBottle.highValueThresholdCents(),
                            savedBottle.bottleLabelValue(),
                            toName,
                            toPhone,
                            phoneHash(toPhone),
                            savedBottle.registrationPhotoRefs(),
                            savedBottle.manualNote(),
                            null, null, null, null));
                }
        );
    }

    /**
     * P0#4 争议受理（R6-D05）：将 stored/pending_identity_review 存酒置 {@code disputed} 态，进争议处理流程
     * （区别于高价值作废的 disputed_frozen）。受理后停止取酒/转赠，待线下核实后由作废/复核流程收口。
     */
    @com.complex.platform.gate.GuardedAction(capability = "bar.saved_bottle.manage", admissionAction = "inventory.saved_bottle_dispute", objectType = "saved_bottle")
    @Transactional
    public SavedBottleRecord disputeSavedBottle(String savedBottleId, SavedBottleCancelRequest request) {
        String id = requireText(savedBottleId, "saved_bottle_id");
        return executeWrite(
                "disputeSavedBottle:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                SavedBottleRecord.class,
                "bar.saved_bottle.disputed",
                "bar.saved_bottle.dispute_failed",
                "bar.saved_bottle.manage",
                "saved_bottle",
                () -> {
                    SavedBottleCancelRequest actual = requireRequest(request);
                    requireText(actual.reason(), "reason");
                    SavedBottleRecord savedBottle = repository.findSavedBottleByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "saved bottle not found"));
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent() && !scope.get().equals(savedBottle.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "saved bottle does not belong to your merchant");
                    }
                    if (!List.of("stored", "pending_identity_review").contains(savedBottle.status())) {
                        throw ApiException.conflict("SAVED_BOTTLE_NOT_DISPUTABLE", "saved bottle is not disputable");
                    }
                    return repository.saveSavedBottle(savedBottleWith(
                            savedBottle, "disputed", savedBottle.remainingQuantity(), Instant.now()));
                }
        );
    }

    /** 暴露给 Controller：列高价值复核单 + approve/reject。 */
    public PageResponse<InventoryHighValueReviewRecord> listHighValueReviews() {
        return highValueReviewService.listHighValueReviews();
    }

    @Transactional
    public InventoryHighValueReviewRecord reviewHighValue(
            String reviewId, com.complex.platform.inventory.dto.HighValueReviewDecisionRequest request) {
        return highValueReviewService.review(reviewId, request);
    }

    // ── V108/V126：盘点录入闭环（建盘点单 → 录差异 → 差异复核(approved/rejected) → 查询）。 ──
    // 复核 approved 是解锁 finance postInventoryVariance 入账的关键（finance 依赖 review_status='approved'）。

    private static final Set<String> INVENTORY_VARIANCE_REASONS =
            Set.of("damage", "shortage", "overage", "supplier_fault");

    // ── V128 / REQ-39：溯源码主数据生命周期。 ──
    private static final Set<String> TRACE_CODE_STATUSES =
            Set.of("registered", "printed", "bound", "consumed");

    public PageResponse<StocktakeRecord> listStocktakes() {
        return readPage(
                "inventory.stocktake.viewed",
                "inventory.stocktake.view_failed",
                "inventory.stocktake.view",
                "stocktake",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listStocktakesByMerchant)
                                .orElseGet(repository::listStocktakes);
                }
        );
    }

    public StocktakeDetailRecord getStocktake(String stocktakeId) {
        String id = requireText(stocktakeId, "stocktake_id");
        try {
            StocktakeRecord stocktake = loadStocktakeScoped(id);
            List<InventoryVarianceRecord> variances = repository.listInventoryVariancesByStocktake(id);
            auditEventService.recordSuccess(
                    "inventory.stocktake.detail_viewed", "inventory.stocktake.view", "stocktake",
                    id, null, null, Map.of("variance_count", variances.size()));
            return new StocktakeDetailRecord(stocktake, variances);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "inventory.stocktake.detail_view_failed", "inventory.stocktake.view", "stocktake",
                    id, null, null, codeOf(exception));
            throw exception;
        }
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stocktake.create")
    @Transactional
    public StocktakeRecord createStocktake(com.complex.platform.inventory.dto.CreateStocktakeRequest request) {
        return executeWrite(
                "createStocktake",
                request == null ? null : request.idempotencyKey(),
                request,
                StocktakeRecord.class,
                "inventory.stocktake.created",
                "inventory.stocktake.create_failed",
                "inventory.stocktake.create",
                "stocktake",
                () -> {
                    com.complex.platform.inventory.dto.CreateStocktakeRequest actual = requireRequest(request);
                    String warehouseId = requireText(actual.warehouseId(), "warehouse_id");
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    LocalDate businessDate =
                            actual.businessDate() == null ? LocalDate.now() : actual.businessDate();
                    Instant now = Instant.now();
                    String slug = slug(actual.idempotencyKey());
                    return repository.saveStocktake(new StocktakeRecord(
                            "stocktake_" + slug,
                            "ST" + now.toEpochMilli() + slug.substring(0, Math.min(6, slug.length())).toUpperCase(Locale.ROOT),
                            warehouseId,
                            merchantId,
                            businessDate,
                            "counting",
                            trimToNull(actual.remark()),
                            now,
                            now));
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stocktake.variance_record")
    @Transactional
    public InventoryVarianceRecord recordVariance(
            String stocktakeId, com.complex.platform.inventory.dto.RecordVarianceRequest request) {
        String id = requireText(stocktakeId, "stocktake_id");
        return executeWrite(
                "recordVariance:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryVarianceRecord.class,
                "inventory.stocktake.variance_recorded",
                "inventory.stocktake.variance_record_failed",
                "inventory.stocktake.variance_record",
                "inventory_variance",
                () -> {
                    com.complex.platform.inventory.dto.RecordVarianceRequest actual = requireRequest(request);
                    StocktakeRecord stocktake = loadStocktakeScoped(id);
                    if (!List.of("counting", "reviewing").contains(stocktake.status())) {
                        throw ApiException.conflict("STOCKTAKE_NOT_OPEN",
                                "stocktake is closed; cannot record variance");
                    }
                    String itemId = requireText(actual.itemId(), "item_id");
                    String reason = normalizeAllowed(actual.reason(), INVENTORY_VARIANCE_REASONS, "reason");
                    java.math.BigDecimal bookQty = actual.bookQty() == null
                            ? java.math.BigDecimal.ZERO : actual.bookQty();
                    java.math.BigDecimal countedQty = actual.countedQty() == null
                            ? java.math.BigDecimal.ZERO : actual.countedQty();
                    // variance_qty = counted − book（带符号）。
                    java.math.BigDecimal varianceQty = countedQty.subtract(bookQty);
                    // variance_amount_cent：显式传入优先；缺省按 variance_qty × unitCostCents 估算（成本估算，带符号）。
                    long varianceAmountCent = resolveVarianceAmountCent(
                            actual.varianceAmountCent(), varianceQty, actual.unitCostCents());
                    Instant now = Instant.now();
                    return repository.saveInventoryVariance(new InventoryVarianceRecord(
                            "variance_" + slug(actual.idempotencyKey()),
                            stocktake.id(),
                            itemId,
                            trimToNull(actual.batchNo()),
                            bookQty,
                            countedQty,
                            varianceQty,
                            varianceAmountCent,
                            reason,
                            trimToNull(actual.supplierId()),
                            "pending",
                            null,
                            null,
                            null,
                            stocktake.merchantId(),
                            now,
                            now));
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stocktake.variance_review", admissionAction = "inventory.stocktake_variance_review", objectType = "inventory_variance")
    @Transactional
    public InventoryVarianceRecord reviewVariance(
            String stocktakeId, String varianceId,
            com.complex.platform.inventory.dto.ReviewVarianceRequest request) {
        String stId = requireText(stocktakeId, "stocktake_id");
        String vId = requireText(varianceId, "variance_id");
        return executeWrite(
                "reviewVariance:" + stId + ":" + vId,
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryVarianceRecord.class,
                "inventory.stocktake.variance_reviewed",
                "inventory.stocktake.variance_review_failed",
                "inventory.stocktake.variance_review",
                "inventory_variance",
                () -> {
                    com.complex.platform.inventory.dto.ReviewVarianceRequest actual = requireRequest(request);
                    String decision = requireDecision(actual.decision());
                    InventoryVarianceRecord variance = repository.findInventoryVarianceByIdForUpdate(vId)
                            .orElseThrow(() -> ApiException.notFound(
                                    "INVENTORY_VARIANCE_NOT_FOUND", "inventory variance not found"));
                    if (!variance.stocktakeId().equals(stId)) {
                        throw ApiException.notFound("INVENTORY_VARIANCE_NOT_FOUND",
                                "inventory variance does not belong to the stocktake");
                    }
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent() && !scope.get().equals(variance.merchantId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "inventory variance does not belong to your merchant");
                    }
                    if (!"pending".equals(variance.reviewStatus())) {
                        throw ApiException.conflict("INVENTORY_VARIANCE_NOT_PENDING",
                                "inventory variance is not pending review");
                    }
                    String reviewer = authorizationService.currentActor().id();
                    Instant now = Instant.now();
                    // 条件 UPDATE WHERE review_status='pending'，受影响行数==1 才成功（防并发重复复核）。
                    int affected = repository.reviewInventoryVarianceIfPending(vId, decision, reviewer, now);
                    if (affected != 1) {
                        throw ApiException.conflict("INVENTORY_VARIANCE_NOT_PENDING",
                                "inventory variance was reviewed concurrently");
                    }
                    return new InventoryVarianceRecord(
                            variance.id(), variance.stocktakeId(), variance.itemId(), variance.batchNo(),
                            variance.bookQty(), variance.countedQty(), variance.varianceQty(),
                            variance.varianceAmountCent(), variance.reason(), variance.supplierId(),
                            decision, reviewer, now, variance.postedArApItemId(),
                            variance.merchantId(), variance.createdAt(), now);
                }
        );
    }

    /** 按 id 读取盘点单并校验归属（merchant scope）。不存在抛 404，跨商户抛 403。 */
    private StocktakeRecord loadStocktakeScoped(String stocktakeId) {
        StocktakeRecord stocktake = repository.findStocktakeById(stocktakeId)
                .orElseThrow(() -> ApiException.notFound("STOCKTAKE_NOT_FOUND", "stocktake not found"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(stocktake.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "stocktake does not belong to your merchant");
        }
        return stocktake;
    }

    /** variance_amount_cent：显式传入优先；否则 round(variance_qty × unitCostCents)（带符号，可为 0）。 */
    private long resolveVarianceAmountCent(
            Long explicitAmountCent, java.math.BigDecimal varianceQty, Long unitCostCents) {
        if (explicitAmountCent != null) {
            return explicitAmountCent;
        }
        if (unitCostCents == null) {
            return 0L;
        }
        if (unitCostCents < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "unit_cost_cents must be non-negative");
        }
        return varianceQty
                .multiply(java.math.BigDecimal.valueOf(unitCostCents))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact();
    }

    // ── V128 / REQ-39：溯源码主数据 CRUD（贴码登记 → 出库打印 → 责任链查询/列表）。 ──
    // 与扫码交接（scan-handover / TraceHandover）互补：本组端点是码本身的生命周期主数据。

    public PageResponse<InventoryTraceCodeRecord> listTraceCodes() {
        return readPage(
                "inventory.trace_code.listed",
                "inventory.trace_code.list_failed",
                "inventory.trace_code.view",
                "inventory_trace_code",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listTraceCodesByMerchant)
                                .orElseGet(repository::listTraceCodes);
                }
        );
    }

    /** REQ-39 责任链查询：按 id 或 trace_code 任一定位（merchant scope）。不存在 404，跨商户 403。 */
    public InventoryTraceCodeRecord getTraceCode(String idOrCode) {
        String key = requireText(idOrCode, "trace_code_id");
        try {
            InventoryTraceCodeRecord traceCode = loadTraceCodeScoped(key);
            auditEventService.recordSuccess(
                    "inventory.trace_code.viewed", "inventory.trace_code.view", "inventory_trace_code",
                    traceCode.id(), null, null, Map.of("trace_code", traceCode.traceCode()));
            return traceCode;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "inventory.trace_code.view_failed", "inventory.trace_code.view", "inventory_trace_code",
                    key, null, null, codeOf(exception));
            throw exception;
        }
    }

    /** REQ-39 贴码登记（入库）：status=registered，幂等；trace_code 缺省服务端生成，重复 trace_code 冲突拒绝。 */
    @com.complex.platform.gate.GuardedAction(capability = "inventory.trace_code.register")
    @Transactional
    public InventoryTraceCodeRecord registerTraceCode(RegisterTraceCodeRequest request) {
        return executeWrite(
                "registerTraceCode",
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryTraceCodeRecord.class,
                "inventory.trace_code.registered",
                "inventory.trace_code.register_failed",
                "inventory.trace_code.register",
                "inventory_trace_code",
                () -> {
                    RegisterTraceCodeRequest actual = requireRequest(request);
                    String itemId = requireText(actual.itemId(), "item_id");
                    repository.findInventoryItemById(itemId)
                            .orElseThrow(() -> ApiException.notFound(
                                    "INVENTORY_ITEM_NOT_FOUND", "inventory item not found"));
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    String slug = slug(actual.idempotencyKey());
                    // trace_code 缺省服务端生成（基于幂等键稳定，重复提交命中幂等不重复建）。
                    String traceCode = trimToDefault(actual.traceCode(), "TRC-" + slug.toUpperCase(Locale.ROOT));
                    // 唯一冲突处理：码值已存在（非本次幂等重放）→ 409 拒绝。
                    repository.findTraceCodeByCode(traceCode).ifPresent(existing -> {
                        throw ApiException.conflict("TRACE_CODE_DUPLICATE",
                                "trace code already exists");
                    });
                    Instant now = Instant.now();
                    return repository.saveTraceCode(new InventoryTraceCodeRecord(
                            "trace_code_" + slug,
                            traceCode,
                            itemId,
                            trimToNull(actual.batchId()),
                            merchantId,
                            "registered",
                            null,
                            null,
                            authorizationService.currentActor().id(),
                            now,
                            now));
                }
        );
    }

    /** REQ-39 出库打印码：status registered→printed，记 printed_at，幂等；条件 UPDATE 受影响 1 行才成功，防重复打印。 */
    @com.complex.platform.gate.GuardedAction(capability = "inventory.trace_code.print")
    @Transactional
    public InventoryTraceCodeRecord printTraceCode(
            String traceCodeId, com.complex.platform.inventory.dto.PrintTraceCodeRequest request) {
        String id = requireText(traceCodeId, "trace_code_id");
        return executeWrite(
                "printTraceCode:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryTraceCodeRecord.class,
                "inventory.trace_code.printed",
                "inventory.trace_code.print_failed",
                "inventory.trace_code.print",
                "inventory_trace_code",
                () -> {
                    requireRequest(request);
                    InventoryTraceCodeRecord traceCode = loadTraceCodeScoped(id);
                    if (!"registered".equals(traceCode.status())) {
                        throw ApiException.conflict("TRACE_CODE_NOT_PRINTABLE",
                                "trace code is not in registered status");
                    }
                    Instant now = Instant.now();
                    // 条件 UPDATE WHERE status='registered'，受影响行数==1 才成功（防并发重复打印）。
                    int affected = repository.printTraceCodeIfRegistered(id, now, null);
                    if (affected != 1) {
                        throw ApiException.conflict("TRACE_CODE_NOT_PRINTABLE",
                                "trace code was printed concurrently");
                    }
                    return new InventoryTraceCodeRecord(
                            traceCode.id(), traceCode.traceCode(), traceCode.itemId(), traceCode.batchId(),
                            traceCode.merchantId(), "printed", traceCode.boundRef(), now,
                            traceCode.createdBy(), traceCode.createdAt(), now);
                }
        );
    }

    /** 按 id 或 trace_code 读取溯源码并校验归属（merchant scope）。不存在抛 404，跨商户抛 403。 */
    private InventoryTraceCodeRecord loadTraceCodeScoped(String idOrCode) {
        InventoryTraceCodeRecord traceCode = repository.findTraceCodeById(idOrCode)
                .or(() -> repository.findTraceCodeByCode(idOrCode))
                .orElseThrow(() -> ApiException.notFound("TRACE_CODE_NOT_FOUND", "trace code not found"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(traceCode.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "trace code does not belong to your merchant");
        }
        return traceCode;
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stock.adjust", admissionAction = "inventory.stock_adjust", objectType = "inventory_stock_adjustment")
    @Transactional
    public StockAdjustmentRecord createStockAdjustment(StockAdjustmentRequest request) {
        return stockMovementService.createStockAdjustment(request);
    }

    public PageResponse<StockBalanceRecord> listStockBalances() {
        return readPage(
                "inventory.stock_balance.viewed",
                "inventory.stock_balance.viewed_failed",
                "inventory.stock.view",
                "inventory_stock_balance",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listStockBalancesByMerchant)
                                .orElseGet(repository::listStockBalances);
                }
        );
    }

    public PageResponse<InventoryStockBatchRecord> listStockBatches() {
        return readPage(
                "inventory.stock_batch.viewed",
                "inventory.stock_batch.view_failed",
                "inventory.stock.view",
                "inventory_stock_batch",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listStockBatchesByMerchant)
                                .orElseGet(repository::listStockBatches);
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stock.batch.manage")
    @Transactional
    public InventoryStockBatchRecord createStockBatch(InventoryStockBatchRequest request) {
        return executeWrite(
                "createInventoryStockBatch",
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryStockBatchRecord.class,
                "inventory.stock_batch.recorded",
                "inventory.stock_batch.record_failed",
                "inventory.stock.batch.manage",
                "inventory_stock_batch",
                () -> createStockBatchOnce(request)
        );
    }

    public PageResponse<InventoryRiskAlertRecord> listRiskAlerts() {
        return readPage(
                "inventory.risk_alert.viewed",
                "inventory.risk_alert.view_failed",
                "inventory.risk_alert.view",
                "inventory_risk_alert",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listRiskAlertsByMerchant)
                                .orElseGet(repository::listRiskAlerts);
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.risk.scan")
    @Transactional
    public InventoryRiskScanResult runInventoryRiskScan(InventoryRiskScanRequest request) {
        return executeWrite(
                "runInventoryRiskScan",
                request == null ? null : request.idempotencyKey(),
                request,
                InventoryRiskScanResult.class,
                "inventory.risk_scan.completed",
                "inventory.risk_scan.failed",
                "inventory.risk.scan",
                "inventory_risk_scan",
                () -> scanInventoryRisks(request)
        );
    }

    public PageResponse<SupplierReconciliationRecord> listSupplierReconciliations() {
        return readPage(
                "inventory.supplier_reconciliation.viewed",
                "inventory.supplier_reconciliation.view_failed",
                "inventory.supplier.reconcile",
                "inventory_supplier_reconciliation",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listSupplierReconciliationsByMerchant)
                                .orElseGet(repository::listSupplierReconciliations);
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.supplier.reconcile")
    @Transactional
    public SupplierReconciliationRecord createSupplierReconciliation(SupplierReconciliationRequest request) {
        return executeWrite(
                "createSupplierReconciliation",
                request == null ? null : request.idempotencyKey(),
                request,
                SupplierReconciliationRecord.class,
                "inventory.supplier_reconciliation.created",
                "inventory.supplier_reconciliation.create_failed",
                "inventory.supplier.reconcile",
                "inventory_supplier_reconciliation",
                () -> createSupplierReconciliationOnce(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stock.move")
    @Transactional
    public StockMovementRecord createStockIn(StockInRequest request) {
        return stockMovementService.createStockIn(request);
    }

    public PageResponse<StockMovementRecord> listStockMovements() {
        return stockMovementService.listStockMovements();
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stock.move")
    @Transactional
    public StockMovementRecord createStockMovement(StockMovementRequest request) {
        return stockMovementService.createStockMovement(request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stock.move")
    @Transactional
    public StockMovementRecord transferStockMovement(StockMovementRequest request) {
        return stockMovementService.transferStockMovement(request);
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.stock.move")
    @Transactional
    public StockMovementRecord createTypedStockMovement(String movementType, StockMovementRequest request) {
        return stockMovementService.createTypedStockMovement(movementType, request);
    }

    public PageResponse<InventorySupplierRecord> listInventorySuppliers() {
        return readPage(
                "inventory.supplier_viewed",
                "inventory.supplier_view_failed",
                "inventory.supplier.view",
                "inventory_supplier",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(repository::listInventorySuppliersByMerchant)
                                .orElseGet(repository::listInventorySuppliers);
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "inventory.supplier.manage")
    @Transactional
    public InventorySupplierRecord createInventorySupplier(InventorySupplierRequest request) {
        return executeWrite(
                "createInventorySupplier",
                request == null ? null : request.idempotencyKey(),
                request,
                InventorySupplierRecord.class,
                "inventory.supplier_changed",
                "inventory.supplier_change_failed",
                "inventory.supplier.manage",
                "inventory_supplier",
                () -> {
                    InventorySupplierRequest actual = requireRequest(request);
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    Instant now = Instant.now();
                    return repository.saveInventorySupplier(new InventorySupplierRecord(
                            "supplier_" + slug(actual.idempotencyKey()),
                            requireText(actual.supplierName(), "supplier_name"),
                            "general",
                            trimToNull(actual.contactName()),
                            trimToNull(actual.contactPhone()),
                            trimToDefault(actual.status(), "active"),
                            now,
                            now,
                            merchantId
                    ));
                }
        );
    }

    /**
     * R5 T17 兼容入口：旧 scan-handover 端点不再写死 handover_result='accepted'。
     * 现内部走新分段流程——建一次交接任务（表头 in_progress + 预置五段 steps pending），
     * 返回的 handover_result 标记为 'chain_started'，真实可追责状态由 overall_status/steps 承载。
     */
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability = "inventory.handover.scan")
    public TraceHandoverRecord scanTraceCodeHandover(VerificationRequest request) {
        return executeWrite(
                "scanTraceCodeHandover",
                request == null ? null : request.idempotencyKey(),
                request,
                TraceHandoverRecord.class,
                "operation_handover.created",
                "operation_handover.failed",
                "inventory.handover.scan",
                "inventory_trace_handover",
                () -> {
                    VerificationRequest actual = requireRequest(request);
                    Map<String, Object> context = actual.businessContext() == null ? Map.of() : actual.businessContext();
                    String handoverId = "trace_" + slug(actual.idempotencyKey());
                    String traceCode = requireText(actual.credentialPayload(), "credential_payload");
                    String deviceId = requireText(actual.deviceId(), "device_id");
                    String itemId = trimToNull(textFromMap(context, "item_id", "itemId"));
                    String fromStaffId = trimToNull(textFromMap(context, "from_staff_id", "fromStaffId"));
                    String toStaffId = trimToNull(textFromMap(context, "to_staff_id", "toStaffId"));
                    return createHandoverChain(handoverId, traceCode, itemId, deviceId, fromStaffId, toStaffId, "chain_started");
                }
        );
    }

    /** R5 T17：建交接任务——表头 overall_status=in_progress + 预置五段 steps(pending, seq 1..5)。 */
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability = "inventory.handover.scan")
    public TraceHandoverChainView createTraceHandover(TraceHandoverCreateRequest request) {
        return executeWrite(
                "createTraceHandover",
                request == null ? null : request.idempotencyKey(),
                request,
                TraceHandoverChainView.class,
                "inventory.handover_created",
                "inventory.handover_create_failed",
                "inventory.handover.scan",
                "inventory_trace_handover",
                () -> {
                    TraceHandoverCreateRequest actual = requireRequest(request);
                    String handoverId = "trace_" + slug(actual.idempotencyKey());
                    String traceCode = requireText(actual.traceCode(), "trace_code");
                    String deviceId = requireText(actual.deviceId(), "device_id");
                    String itemId = trimToNull(actual.itemId());
                    TraceHandoverRecord header =
                            createHandoverChain(handoverId, traceCode, itemId, deviceId, null, null, "chain_started");
                    return chainView(header.id());
                }
        );
    }

    /** R5 T17：员工段扫码（主动留痕）。同段重复→duplicate 异常；全段 done→overall completed。 */
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability = "inventory.handover.scan")
    public TraceHandoverChainView scanTraceSegment(String handoverId, TraceScanSegmentRequest request) {
        String id = requireText(handoverId, "handover_id");
        return executeWrite(
                "scanTraceSegment:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TraceHandoverChainView.class,
                "inventory.handover_segment_scanned",
                "inventory.handover_segment_scan_failed",
                "inventory.handover.scan",
                "inventory_trace_handover_step",
                () -> {
                    TraceScanSegmentRequest actual = requireRequest(request);
                    String segment = requireSegment(actual.segment());
                    if (ROBOT_SEGMENT.equals(segment)) {
                        throw ApiException.badRequest("HANDOVER_SEGMENT_NOT_STAFF",
                                "dispatch segment is robot-passive; use robot-report");
                    }
                    TraceHandoverRecord header = requireHandover(id);
                    TraceHandoverStepRecord step = requireStep(id, segment);
                    String operator = authorizationService.currentActor().id();
                    Instant now = Instant.now();
                    if ("done".equals(step.stepStatus())) {
                        // 同段重复扫码 → duplicate 异常 + overall exception。
                        markStep(step, operator, null, "duplicate", now, "duplicate scan on segment " + segment);
                        raiseException(id, segment, "duplicate", "duplicate scan on segment " + segment, now);
                        return chainView(id);
                    }
                    markStep(step, operator, null, "done", now, null);
                    completeOrAdvance(header, segment, now);
                    return chainView(id);
                }
        );
    }

    /** R5 T17：机器人段(dispatch)上报（被动留痕），受 T14 integration.robot.sync 开关门控 + 写 T14 事件。 */
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability = "inventory.handover.scan")
    public TraceHandoverChainView reportRobotSegment(String handoverId, TraceRobotReportRequest request) {
        String id = requireText(handoverId, "handover_id");
        return executeWrite(
                "reportRobotSegment:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TraceHandoverChainView.class,
                "inventory.handover_segment_scanned",
                "inventory.handover_segment_scan_failed",
                "inventory.handover.scan",
                "inventory_trace_handover_step",
                () -> {
                    TraceRobotReportRequest actual = requireRequest(request);
                    // 受 T14 第三方开关门控（编程式）：关闭则 reject 留痕、不放行被动留痕。
                    terminalSwitchGuard.check(ROBOT_SYNC_CAPABILITY);
                    String robotTaskRef = requireText(actual.robotTaskRef(), "robot_task_ref");
                    String status = trimToDefault(actual.status(), "delivered");
                    TraceHandoverRecord header = requireHandover(id);
                    TraceHandoverStepRecord step = requireStep(id, ROBOT_SEGMENT);
                    Instant now = Instant.now();
                    // T14 关联：机器人段被动留痕写 integration_robot_task_events(handover_id)。
                    repository.saveRobotTaskEvent(
                            "robotevt_" + slug(actual.idempotencyKey()),
                            robotTaskRef,
                            "delivered".equals(status) ? "delivered" : "failed",
                            id,
                            robotEventPayload(status),
                            now);
                    if ("done".equals(step.stepStatus())) {
                        markStep(step, null, robotTaskRef, "duplicate", now, "duplicate robot report on dispatch");
                        raiseException(id, ROBOT_SEGMENT, "duplicate", "duplicate robot report on dispatch", now);
                        return chainView(id);
                    }
                    if ("delivered_no_receiver".equals(status)) {
                        // 机器人上报送达但无人工接收扫码 → gap 异常进复核。
                        markStep(step, null, robotTaskRef, "gap", now, "robot delivered but no staff receipt");
                        raiseException(id, ROBOT_SEGMENT, "gap", "robot delivered but no staff receipt", now);
                        return chainView(id);
                    }
                    boolean diff = Boolean.TRUE.equals(actual.reportDiff()) || !"delivered".equals(status);
                    if (diff) {
                        // 机器人上报与人工预期不符（点位/物料/状态）→ diff 异常进复核。
                        markStep(step, null, robotTaskRef, "diff", now, "robot report diff: status=" + status);
                        raiseException(id, ROBOT_SEGMENT, "diff", "robot report diff: status=" + status, now);
                        return chainView(id);
                    }
                    markStep(step, null, robotTaskRef, "done", now, null);
                    completeOrAdvance(header, ROBOT_SEGMENT, now);
                    return chainView(id);
                }
        );
    }

    /** R5 T17：异常复核 approve/reject 闭合 + 留结论；approve 时把该段标 gap-closed 不阻塞，reject 维持 exception。 */
    @Transactional
    @com.complex.platform.gate.GuardedAction(capability = "inventory.handover.review")
    public TraceHandoverExceptionRecord reviewException(String exceptionId, TraceExceptionReviewRequest request) {
        String id = requireText(exceptionId, "exception_id");
        return executeWrite(
                "reviewTraceException:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TraceHandoverExceptionRecord.class,
                "inventory.handover_exception_resolved",
                "inventory.handover_exception_resolve_failed",
                "inventory.handover.review",
                "inventory_trace_handover_exception",
                () -> {
                    TraceExceptionReviewRequest actual = requireRequest(request);
                    String decision = requireDecision(actual.decision());
                    TraceHandoverExceptionRecord exception = repository.findTraceHandoverExceptionByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound(
                                    "HANDOVER_EXCEPTION_NOT_FOUND", "trace handover exception not found"));
                    if (!"pending".equals(exception.reviewStatus())) {
                        throw ApiException.conflict("HANDOVER_EXCEPTION_NOT_PENDING",
                                "trace handover exception is not pending");
                    }
                    String reviewer = authorizationService.currentActor().id();
                    Instant now = Instant.now();
                    TraceHandoverExceptionRecord resolved = new TraceHandoverExceptionRecord(
                            exception.id(), exception.handoverId(), exception.segment(), exception.exceptionType(),
                            decision, reviewer, trimToNull(actual.reason()), exception.createdAt(), now);
                    repository.saveTraceHandoverException(resolved);
                    closeExceptionOnHeader(exception.handoverId(), now);
                    return resolved;
                }
        );
    }

    private TraceHandoverRecord createHandoverChain(
            String handoverId, String traceCode, String itemId, String deviceId,
            String fromStaffId, String toStaffId, String handoverResult) {
        Instant now = Instant.now();
        TraceHandoverRecord header = repository.saveTraceHandover(new TraceHandoverRecord(
                handoverId, traceCode, itemId, fromStaffId, toStaffId, deviceId,
                handoverResult, "in_progress", HANDOVER_SEGMENTS.getFirst(), List.of(), now));
        for (int i = 0; i < HANDOVER_SEGMENTS.size(); i++) {
            String segment = HANDOVER_SEGMENTS.get(i);
            boolean robot = ROBOT_SEGMENT.equals(segment);
            repository.saveTraceHandoverStep(new TraceHandoverStepRecord(
                    handoverId + "_" + segment,
                    handoverId,
                    segment,
                    i + 1,
                    robot ? "robot" : "staff",
                    null,
                    null,
                    null,
                    "pending",
                    null,
                    now));
        }
        // 三层留痕接线（REQ-34/37）：供应链五段交接=一条真实业务流程，建过程实例（process_type=inventory_delivery）。
        // handoverId 复用为 process_instance_id（1:1），merchant 由 token 注入（TraceabilityService 内部覆盖）。
        traceabilityService.recordProcessInstance(new TraceabilityProcessInstanceRecord(
                handoverId, "inventory_delivery", "inventory_item",
                itemId == null ? handoverId : itemId, null, null, null, null,
                null, null, HANDOVER_SEGMENTS.getFirst(), "running", now, null));
        return header;
    }

    private void markStep(
            TraceHandoverStepRecord step, String operatorUserId, String robotTaskRef,
            String status, Instant scanAt, String note) {
        repository.saveTraceHandoverStep(new TraceHandoverStepRecord(
                step.id(), step.handoverId(), step.segment(), step.seq(), step.actorType(),
                operatorUserId != null ? operatorUserId : step.operatorUserId(),
                robotTaskRef != null ? robotTaskRef : step.robotTaskRef(),
                scanAt, status, note, step.createdAt()));
    }

    private void raiseException(String handoverId, String segment, String type, String reason, Instant now) {
        String exceptionId = "traceexc_" + slug(handoverId + ":" + segment + ":" + type + ":" + now.toEpochMilli());
        repository.saveTraceHandoverException(new TraceHandoverExceptionRecord(
                exceptionId,
                handoverId, segment, type, "pending", null, reason, now, null));
        TraceHandoverRecord header = requireHandover(handoverId);
        repository.saveTraceHandover(withOverall(header, "exception", segment));
        // P1：落孤岛表 inventory_trace_handover_exceptions 后，双写统一 risk_alerts（汇聚镜像，fail-safe）。
        reportTraceHandoverExceptionToUnified(exceptionId, handoverId, segment, type, reason);
    }

    /**
     * P1：库存风险告警双写统一异常责任链 risk_alerts（evidence/RiskAlertService.report）。
     *
     * <p>source_domain='inventory'，alertType 沿用本域值，risk_level 经映射（medium→suspicious，high→high），
     * source_ref=该 risk alert 的 id，幂等键稳定唯一（id + ":unified"），重复扫描不重复上报。
     *
     * <p>fail-safe：未注入服务（旧构造器/单测）或上报异常都不阻塞扫描主链——
     * 孤岛 inventory_risk_alerts 仍是命中落地的权威，统一表为汇聚镜像。
     */
    private void reportRiskAlertToUnified(InventoryRiskAlertRecord alert) {
        if (riskAlertService == null) {
            return;
        }
        // 未知风险级映射不到统一枚举则按 high 兜底（避免 report 抛 VALIDATION_FAILED 静默丢数据）。
        String unifiedLevel = RISK_LEVEL_TO_UNIFIED.getOrDefault(alert.severity(), "high");
        try {
            riskAlertService.report(new RiskAlertReportRequest(
                    alert.id() + ":unified",
                    "inventory",
                    alert.id(),
                    alert.alertType(),
                    unifiedLevel,
                    "Inventory risk: " + alert.alertType() + " on item " + alert.itemId(),
                    null,
                    "alert_type=" + alert.alertType() + "; item=" + alert.itemId()
                            + "; location=" + alert.locationId(),
                    List.of()));
        } catch (RuntimeException exception) {
            // 统一表上报失败不影响扫描主链（命中已落孤岛 inventory_risk_alerts）。
            auditEventService.recordFailure(
                    "inventory.risk.unified_alert_failed",
                    "inventory.risk.scan",
                    "risk_alert",
                    alert.id(),
                    null,
                    alert.id() + ":unified",
                    codeOf(exception));
        }
    }

    /**
     * P1：溯源交接断链异常双写统一异常责任链 risk_alerts。
     *
     * <p>source_domain='inventory'，alertType='trace_handover_exception'，risk_level='high'，
     * source_ref=异常 id，幂等键稳定唯一（exceptionId + ":unified"）。fail-safe 同上：
     * 孤岛 inventory_trace_handover_exceptions 仍权威，统一表为汇聚镜像。
     */
    private void reportTraceHandoverExceptionToUnified(
            String exceptionId, String handoverId, String segment, String type, String reason) {
        if (riskAlertService == null) {
            return;
        }
        try {
            riskAlertService.report(new RiskAlertReportRequest(
                    exceptionId + ":unified",
                    "inventory",
                    exceptionId,
                    "trace_handover_exception",
                    "high",
                    "Trace handover exception: " + type + " on segment " + segment,
                    null,
                    "handover=" + handoverId + "; segment=" + segment + "; type=" + type
                            + "; reason=" + reason,
                    List.of()));
        } catch (RuntimeException exception) {
            // 统一表上报失败不影响交接主链（异常已落孤岛 inventory_trace_handover_exceptions）。
            auditEventService.recordFailure(
                    "inventory.trace.unified_alert_failed",
                    "inventory.handover.scan",
                    "risk_alert",
                    exceptionId,
                    null,
                    exceptionId + ":unified",
                    codeOf(exception));
        }
    }

    /** 一段 done 后：若五段全 done → overall completed + 事件；否则推进 current_segment 到下一未完成段。 */
    private void completeOrAdvance(TraceHandoverRecord header, String segment, Instant now) {
        List<TraceHandoverStepRecord> steps = repository.listTraceHandoverSteps(header.id());
        boolean allDone = steps.stream().allMatch(s -> "done".equals(s.stepStatus()));
        if (allDone) {
            repository.saveTraceHandover(withOverall(header, "completed", segment));
            // 三层留痕：本段步骤入库 + 过程实例收口（completed）。
            recordTraceStep(header, segment, now, "completed");
            auditEventService.recordSuccess(
                    "inventory.handover_completed", "inventory.handover.scan",
                    "inventory_trace_handover", header.id(), null, null,
                    Map.of("handover_id", header.id()));
            return;
        }
        String next = steps.stream()
                .filter(s -> "pending".equals(s.stepStatus()))
                .map(TraceHandoverStepRecord::segment)
                .findFirst()
                .orElse(segment);
        // 仅当当前非 exception 时维持 in_progress 推进；exception 不被一段 done 覆盖。
        if (!"exception".equals(header.overallStatus())) {
            repository.saveTraceHandover(withOverall(header, "in_progress", next));
        }
        // 三层留痕：本段步骤入库 + 过程实例当前步推进（running）。
        recordTraceStep(header, segment, now, next);
    }

    /**
     * 三层留痕步骤接线（REQ-34/37）：某段 done 时写过程步骤记录 + 推进过程实例 current_step。
     *
     * <p>actor/capture_source 区分人工扫码段（staff/qr_scan）与机器人段（robot/robot_callback）；
     * 机器人段被动留痕，requiresManualReason 维持 false。instanceStatus 为新的过程实例状态。
     */
    private void recordTraceStep(TraceHandoverRecord header, String segment, Instant now, String nextStepCode) {
        boolean robot = ROBOT_SEGMENT.equals(segment);
        String actorId = robot ? null : authorizationService.currentActor().id();
        traceabilityService.recordStep(new TraceabilityStepRecord(
                header.id() + "_step_" + segment,
                header.id(),
                segment,
                segment,
                robot ? "robot" : "staff",
                actorId,
                robot ? null : "tablet",
                header.deviceId(),
                null,
                null,
                true,
                robot ? "robot_callback" : "qr_scan",
                false,
                "success",
                now));
        boolean completed = "completed".equals(nextStepCode);
        traceabilityService.recordProcessInstance(new TraceabilityProcessInstanceRecord(
                header.id(), "inventory_delivery", "inventory_item",
                header.itemId() == null ? header.id() : header.itemId(), null, null, null, null,
                null, null, completed ? segment : nextStepCode,
                completed ? "completed" : "running", null, completed ? now : null));
    }

    /** 复核闭合后：若该交接已无 pending 异常，则按 steps 真实状态回算 overall。 */
    private void closeExceptionOnHeader(String handoverId, Instant now) {
        TraceHandoverRecord header = requireHandover(handoverId);
        List<TraceHandoverStepRecord> steps = repository.listTraceHandoverSteps(handoverId);
        boolean allDone = steps.stream().allMatch(s -> "done".equals(s.stepStatus()));
        String next = steps.stream()
                .filter(s -> "pending".equals(s.stepStatus()))
                .map(TraceHandoverStepRecord::segment)
                .findFirst()
                .orElse(header.currentSegment());
        repository.saveTraceHandover(withOverall(header, allDone ? "completed" : "in_progress", next));
    }

    private TraceHandoverRecord withOverall(TraceHandoverRecord h, String overall, String currentSegment) {
        TraceHandoverRecord updated = new TraceHandoverRecord(
                h.id(), h.traceCode(), h.itemId(), h.fromStaffId(), h.toStaffId(), h.deviceId(),
                h.handoverResult(), overall, currentSegment, h.evidenceAttachmentIds(), h.createdAt());
        if ("exception".equals(overall)) {
            auditEventService.recordSuccess(
                    "inventory.handover_exception_raised", "inventory.handover.scan",
                    "inventory_trace_handover", h.id(), null, null,
                    Map.of("handover_id", h.id(), "segment", currentSegment == null ? "" : currentSegment));
        }
        return updated;
    }

    private TraceHandoverChainView chainView(String handoverId) {
        TraceHandoverRecord header = requireHandover(handoverId);
        return new TraceHandoverChainView(
                header,
                repository.listTraceHandoverSteps(handoverId),
                List.of());
    }

    private TraceHandoverRecord requireHandover(String handoverId) {
        return repository.findTraceHandoverByIdForUpdate(handoverId)
                .orElseThrow(() -> ApiException.notFound("HANDOVER_NOT_FOUND", "trace handover not found"));
    }

    private TraceHandoverStepRecord requireStep(String handoverId, String segment) {
        return repository.findTraceHandoverStep(handoverId, segment)
                .orElseThrow(() -> ApiException.notFound("HANDOVER_STEP_NOT_FOUND", "trace handover step not found"));
    }

    private String requireSegment(String segment) {
        String value = requireText(segment, "segment");
        if (!HANDOVER_SEGMENTS.contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "segment is invalid");
        }
        return value;
    }

    private String requireDecision(String decision) {
        String value = requireText(decision, "decision").toLowerCase(Locale.ROOT);
        if (!Set.of("approved", "rejected").contains(value)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "decision must be approved or rejected");
        }
        return value;
    }

    private String robotEventPayload(String status) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(Map.of("status", status, "source", "trace_handover_chain"));
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            return "{}";
        }
    }

    private InventoryStockBatchRecord createStockBatchOnce(InventoryStockBatchRequest request) {
        InventoryStockBatchRequest actual = requireRequest(request);
        String itemId = requireText(actual.itemId(), "item_id");
        repository.findInventoryItemById(itemId)
                .orElseThrow(() -> ApiException.notFound("INVENTORY_ITEM_NOT_FOUND", "inventory item not found"));
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        return repository.saveStockBatch(new InventoryStockBatchRecord(
                "batch_" + slug(actual.idempotencyKey()),
                itemId,
                requireText(actual.locationId(), "location_id"),
                requireText(actual.batchNo(), "batch_no"),
                trimToNull(actual.supplierId()),
                requirePositiveInteger(actual.quantity(), "quantity"),
                actual.expireAt(),
                normalizeAllowed(trimToDefault(actual.status(), "active"), STOCK_BATCH_STATUSES, "status"),
                safeList(actual.evidenceAttachmentIds()),
                now,
                now,
                merchantId
        ));
    }

    private InventoryRiskScanResult scanInventoryRisks(InventoryRiskScanRequest request) {
        InventoryRiskScanRequest actual = requireRequest(request);
        requireText(actual.idempotencyKey(), "idempotency_key");
        // P1-5: operatorUserId 一律取自认证上下文，禁止来自请求体
        String operatorUserId = authorizationService.currentActor().id();
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        int expiryWarningDays = expiryWarningDays(actual.expiryWarningDays());
        Instant now = Instant.now();
        Instant expiryCutoff = now.plusSeconds(expiryWarningDays * 86_400L);

        // 使用租户隔离的列表
        List<InventoryItemRecord> items = merchantId != null
                ? repository.listInventoryItemsByMerchant(merchantId)
                : repository.listInventoryItems();
        List<StockBalanceRecord> balances = merchantId != null
                ? repository.listStockBalancesByMerchant(merchantId)
                : repository.listStockBalances();
        List<InventoryStockBatchRecord> batches = merchantId != null
                ? repository.listStockBatchesByMerchant(merchantId)
                : repository.listStockBatches();

        Map<String, InventoryItemRecord> itemsById = items.stream()
                .collect(java.util.stream.Collectors.toMap(InventoryItemRecord::id, item -> item, (left, right) -> left));
        java.util.ArrayList<InventoryRiskAlertRecord> alerts = new java.util.ArrayList<>();
        int index = 0;

        for (StockBalanceRecord balance : balances) {
            InventoryItemRecord item = itemsById.get(balance.itemId());
            int minQuantity = item == null ? 0 : item.minQuantity();
            if (balance.availableQuantity() <= 0) {
                alerts.add(riskAlert(actual.idempotencyKey(), index++, "out_of_stock", "high", balance.itemId(),
                        balance.locationId(), null, balance.availableQuantity(), minQuantity, null, operatorUserId, merchantId, now));
            } else if (minQuantity > 0 && balance.availableQuantity() < minQuantity) {
                alerts.add(riskAlert(actual.idempotencyKey(), index++, "safety_stock_low", "medium", balance.itemId(),
                        balance.locationId(), null, balance.availableQuantity(), minQuantity, null, operatorUserId, merchantId, now));
            }
        }

        for (InventoryStockBatchRecord batch : batches) {
            if (!"active".equals(batch.status()) || batch.expireAt() == null || batch.quantity() <= 0) {
                continue;
            }
            if (!batch.expireAt().isAfter(expiryCutoff)) {
                alerts.add(riskAlert(actual.idempotencyKey(), index++, "expiry_warning", "high", batch.itemId(),
                        batch.locationId(), batch.id(), batch.quantity(), null, batch.expireAt(), operatorUserId, merchantId, now));
            }
        }

        List<InventoryRiskAlertRecord> saved = alerts.stream()
                .map(repository::saveRiskAlert)
                .toList();
        // P1：落孤岛表 inventory_risk_alerts 后，双写统一 risk_alerts（汇聚镜像，fail-safe）。
        saved.forEach(this::reportRiskAlertToUnified);
        return new InventoryRiskScanResult(saved.size(), saved);
    }

    private InventoryRiskAlertRecord riskAlert(
            String idempotencyKey,
            int index,
            String alertType,
            String severity,
            String itemId,
            String locationId,
            String batchId,
            Integer availableQuantity,
            Integer thresholdQuantity,
            Instant expireAt,
            String operatorUserId,
            String merchantId,
            Instant now
    ) {
        return new InventoryRiskAlertRecord(
                "risk_" + slug(idempotencyKey + ":" + index),
                alertType,
                severity,
                itemId,
                locationId,
                batchId,
                availableQuantity,
                thresholdQuantity,
                expireAt,
                "open",
                operatorUserId,
                now,
                merchantId
        );
    }

    private SupplierReconciliationRecord createSupplierReconciliationOnce(SupplierReconciliationRequest request) {
        SupplierReconciliationRequest actual = requireRequest(request);
        String supplierId = requireText(actual.supplierId(), "supplier_id");
        boolean supplierExists = repository.listInventorySuppliers().stream()
                .anyMatch(supplier -> supplier.id().equals(supplierId));
        if (!supplierExists) {
            throw ApiException.notFound("SUPPLIER_NOT_FOUND", "inventory supplier not found");
        }
        LocalDate periodStart = requireDate(actual.periodStart(), "period_start");
        LocalDate periodEnd = requireDate(actual.periodEnd(), "period_end");
        if (periodEnd.isBefore(periodStart)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period_end must be on or after period_start");
        }
        long expectedAmountCent = requireNonNegativeLong(actual.expectedAmountCent(), "expected_amount_cent");
        long actualAmountCent = requireNonNegativeLong(actual.actualAmountCent(), "actual_amount_cent");
        // P1-5: operatorUserId 一律取自认证上下文，禁止来自请求体
        String operatorUserId = authorizationService.currentActor().id();
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        Instant now = Instant.now();
        return repository.saveSupplierReconciliation(new SupplierReconciliationRecord(
                "supplier_recon_" + slug(actual.idempotencyKey()),
                supplierId,
                periodStart,
                periodEnd,
                expectedAmountCent,
                actualAmountCent,
                actualAmountCent - expectedAmountCent,
                normalizeAllowed(trimToDefault(actual.status(), "pending"), SUPPLIER_RECONCILIATION_STATUSES, "status"),
                safeList(actual.evidenceAttachmentIds()),
                operatorUserId,
                now,
                now,
                merchantId
        ));
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> supplier
    ) {
        try {
            List<T> items = supplier.get();
            auditEventService.recordSuccess(
                    successEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    Map.of("count", items.size())
            );
            return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        T response = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                permissionCode,
                                businessType,
                                businessId(response),
                                null,
                                idempotencyKey,
                                Map.of("operation_id", operationId)
                        );
                        return response;
                    },
                    responseType
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    "unknown",
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private String businessId(Object response) {
        if (response instanceof InventoryItemRecord record) {
            return record.id();
        }
        if (response instanceof InventorySupplierRecord record) {
            return record.id();
        }
        if (response instanceof PurchaseRequestRecord record) {
            return record.id();
        }
        if (response instanceof SavedBottleRecord record) {
            return record.id();
        }
        if (response instanceof SavedBottleClaimRecord record) {
            return record.id();
        }
        if (response instanceof SavedBottleTransferRecord record) {
            return record.id();
        }
        if (response instanceof StockMovementRecord record) {
            return record.id();
        }
        if (response instanceof StockAdjustmentRecord record) {
            return record.id();
        }
        if (response instanceof InventoryStockBatchRecord record) {
            return record.id();
        }
        if (response instanceof InventoryRiskScanResult) {
            return "inventory_risk_scan";
        }
        if (response instanceof InventoryRiskAlertRecord record) {
            return record.id();
        }
        if (response instanceof SupplierReconciliationRecord record) {
            return record.id();
        }
        if (response instanceof TraceHandoverRecord record) {
            return record.id();
        }
        if (response instanceof TraceHandoverChainView view) {
            return view.handover().id();
        }
        if (response instanceof TraceHandoverExceptionRecord record) {
            return record.id();
        }
        if (response instanceof InventoryHighValueReviewRecord record) {
            return record.id();
        }
        if (response instanceof StocktakeRecord record) {
            return record.id();
        }
        if (response instanceof InventoryVarianceRecord record) {
            return record.id();
        }
        if (response instanceof InventoryTraceCodeRecord record) {
            return record.id();
        }
        return "unknown";
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private Map<String, Object> firstItem(List<Map<String, Object>> items, String fieldName) {
        return requireItems(items, fieldName).getFirst();
    }

    private List<Map<String, Object>> requireItems(List<Map<String, Object>> items, String fieldName) {
        if (items == null || items.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return items;
    }

    private String requireItemText(Map<String, Object> item, String fieldName) {
        return requireText(textFromItem(item, fieldName), fieldName);
    }

    private String textFromItem(Map<String, Object> item, String fieldName) {
        return textFromMap(item, fieldName, snakeToCamel(fieldName));
    }

    private String textFromMap(Map<String, Object> map, String snakeName, String camelName) {
        Object value = map.get(snakeName);
        if (value == null) {
            value = map.get(camelName);
        }
        return value == null ? null : value.toString();
    }

    private int requirePositiveItemQuantity(Map<String, Object> item, String fieldName) {
        String raw = textFromItem(item, fieldName);
        if (raw == null || !raw.matches("[0-9]+")) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be a positive integer");
        }
        try {
            int quantity = Integer.parseInt(raw);
            if (quantity <= 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
            }
            return quantity;
        } catch (NumberFormatException exception) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is out of range");
        }
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private int nonNegativeOrDefault(Integer value, int defaultValue, String fieldName) {
        int actual = value == null ? defaultValue : value;
        if (actual < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return actual;
    }

    /** 可空非负长整：null 透传（如高价值阈值 null=不视高价值），非空须 >= 0。 */
    private Long nonNegativeOrNull(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return value;
    }

    private int requirePositiveInteger(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
        }
        return value;
    }

    private long requireNonNegativeLong(Long value, String fieldName) {
        if (value == null || value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-negative");
        }
        return value;
    }

    private LocalDate requireDate(LocalDate value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return value;
    }

    private String normalizeAllowed(String value, Set<String> allowed, String fieldName) {
        String normalized = requireText(value, fieldName).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return normalized;
    }

    private int expiryWarningDays(Integer value) {
        int actual = value == null ? 7 : value;
        if (actual < 1 || actual > 365) {
            throw ApiException.badRequest("VALIDATION_FAILED", "expiry_warning_days must be between 1 and 365");
        }
        return actual;
    }

    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : List.copyOf(value);
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String snakeToCamel(String value) {
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '_') {
                upperNext = true;
            } else if (upperNext) {
                builder.append(Character.toUpperCase(character));
                upperNext = false;
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    private String slug(String seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(requireText(seed, "idempotency_key").getBytes(StandardCharsets.UTF_8)))
                    .substring(0, 16);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("slug generation unavailable", exception);
        }
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return "INTERNAL_ERROR";
    }
}
