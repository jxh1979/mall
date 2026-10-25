package com.complex.platform.inventory;

// 文件版本：V0.03

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.inventory.dto.HighValueReviewDecisionRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import com.complex.platform.common.Strings;
import static com.complex.platform.common.Strings.requireText;

/**
 * R5 T6（库存域内自建）高价值复核服务（inventory_high_value_reviews）。
 *
 * <p>裁决②：高价值复核 = 库存域内自建，不复用 governance/SupervisorReview。替换旧"仅校验附件数≥2、
 * 复核未接入"现状——高价值动作（存酒作废 {@code saved_bottle_void}、报损 {@code damage_report}、
 * 盘点差异 {@code stock_variance}）命中 {@code is_high_value} 时<b>不直接生效</b>，改为：
 *
 * <ol>
 *   <li><b>建复核单（pending）</b>：要求<b>照片 + 视频双证据</b>（{@code photoAttachmentIds} 与
 *       {@code videoAttachmentIds} 各非空，真正区分类型）；{@code subjectId} 关联
 *       movement/adjustment/saved_bottle。事件 {@code inventory.high_value_review_created}。</li>
 *   <li><b>主管复核 {@link #review}（approve/reject）</b>：approve → 动作生效
 *       （存酒置 {@code cancelled}；报损/盘点差异落账由对应动作回调生效）；reject → 回滚（保持原态、不扣减）。
 *       写 reviewer/reason/reviewed_at，事件 {@code inventory.high_value_review_decided}。权限
 *       {@code inventory.high_value.review}。</li>
 * </ol>
 *
 * <p>本期仅 {@code saved_bottle_void} 由 {@link InventoryService#cancelSavedBottle} 接入活跃链路
 * （approve 回调把存酒由中间态置 cancelled，reject 维持原态）；{@code damage_report}/{@code stock_variance}
 * 子类型表与复核服务已支持，但报损/盘点差异入口尚未接复核（见类尾遗留说明），作为后续。
 */
@Service
public class InventoryHighValueReviewService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;

    static final String SUBJECT_SAVED_BOTTLE_VOID = "saved_bottle_void";
    static final String SUBJECT_DAMAGE_REPORT = "damage_report";
    static final String SUBJECT_STOCK_VARIANCE = "stock_variance";
    /**
     * OBS-13-02（BSM L1072「转库存之后的报废、赠送、内部使用等处置必须走审批并保留操作记录」；操作员裁决补充第十四批第 4 条
     * 「门店主管与财务审批」）：存酒库房（到期/放弃存酒转入）存货的出库与报废。subject 为待审调整单；门店主管与财务两道都批才扣库存，
     * 同赔付方案审批（{@link InventoryService#approveSavedBottleRemedy}）：角色各核权限码、审批人不得是发起人、两道不得同一人。
     */
    static final String SUBJECT_SAVED_BOTTLE_STOCK_DISPOSAL = "saved_bottle_stock_disposal";
    private static final String REVIEW_PERMISSION = "inventory.high_value.review";
    private static final Set<String> SUBJECT_TYPES =
            Set.of(SUBJECT_SAVED_BOTTLE_VOID, SUBJECT_DAMAGE_REPORT, SUBJECT_STOCK_VARIANCE, SUBJECT_SAVED_BOTTLE_STOCK_DISPOSAL);
    private static final Set<String> ADJUSTMENT_SUBJECTS =
            Set.of(SUBJECT_DAMAGE_REPORT, SUBJECT_STOCK_VARIANCE, SUBJECT_SAVED_BOTTLE_STOCK_DISPOSAL);
    private static final Set<String> DECISIONS = Set.of("approved", "rejected");

    private final InventoryRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    /**
     * 附件文件/类型校验器；未注入时不能提交或批准高价值复核。
     * 生产由 InventoryService 构造时透传 Spring bean。
     */
    private final com.complex.platform.evidence.EvidenceAttachmentValidator evidenceAttachmentValidator;

    public InventoryHighValueReviewService(
            InventoryRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this(repository, idempotencyService, auditEventService, authorizationService, null);
    }

    // 本类同时是 Spring bean（历史遗留）与 InventoryService/PurchaseRequestService 手 new 的域内组件；
    // 多构造器下必须显式标注注入点，否则 Spring 找默认构造器失败。
    @org.springframework.beans.factory.annotation.Autowired
    public InventoryHighValueReviewService(
            InventoryRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            com.complex.platform.evidence.EvidenceAttachmentValidator evidenceAttachmentValidator
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.evidenceAttachmentValidator = evidenceAttachmentValidator;
    }

    public PageResponse<InventoryHighValueReviewRecord> listHighValueReviews() {
        return readPage(
                "inventory.high_value_review.viewed",
                "inventory.high_value_review.view_failed",
                () -> {
                    // 读路径按商户范围（同 reviewInternal 的 C3 口径）：有商户范围只列本商户的复核单；平台、项目账号空范围照旧全列。
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return repository.listHighValueReviews().stream()
                            .filter(review -> scope.isEmpty() || scope.get().equals(review.merchantId()))
                            .toList();
                });
    }

    /**
     * 建高价值复核单（pending）。要求照片 + 视频双证据各非空（替换"附件数≥2 不分类型"弱校验）。
     *
     * <p>在调用方动作事务内调用（如 cancelSavedBottle）：建单后调用方把对象置中间态（不直接生效）。
     *
     * @param idempotencySeed 复核单 id 派生种子（取调用方幂等键，确保同一动作幂等）
     * @param subjectType     damage_report / stock_variance / saved_bottle_void
     * @param subjectId       关联 movement/adjustment/saved_bottle id
     * @param photoAttachmentIds 照片证据 id（必须非空）
     * @param videoAttachmentIds 视频证据 id（必须非空）
     * @return 建好的 pending 复核单
     */
    InventoryHighValueReviewRecord createPending(
            String idempotencySeed,
            String subjectType,
            String subjectId,
            List<String> photoAttachmentIds,
            List<String> videoAttachmentIds
    ) {
        return createPending(idempotencySeed, subjectType, subjectId, photoAttachmentIds, videoAttachmentIds, true);
    }

    /** {@code requireDualEvidence=false} 仅用于存酒库房存货处置中非高价值的报废与出库（照片/视频不强制，不入复核单）。 */
    InventoryHighValueReviewRecord createPending(
            String idempotencySeed,
            String subjectType,
            String subjectId,
            List<String> photoAttachmentIds,
            List<String> videoAttachmentIds,
            boolean requireDualEvidence
    ) {
        String type = normalizeAllowed(subjectType, SUBJECT_TYPES, "subject_type");
        String subject = requireText(subjectId, "subject_id");
        List<String> photos = requireDualEvidence ? requireNonEmptyList(photoAttachmentIds, "photo_attachment_ids") : List.of();
        List<String> videos = requireDualEvidence ? requireNonEmptyList(videoAttachmentIds, "video_attachment_ids") : List.of();
        // 双证据闭环：附件必须**真实存在**且类型对得上（照片栏必须是照片、视频栏必须是视频）。
        // 改造前只校两栏各自非空 —— 同一个随手编的假 id 填两栏就算「照片+视频双证据」。
        if (requireDualEvidence) {
            requireEvidence(photos, videos);
        }
        Instant now = Instant.now();
        // C3：建单即落归属商户（当前 scope），供 reviewInternal 拒绝跨商户翻转。平台/项目管理员空 scope → null（放行）。
        String merchantId = authorizationService.currentMerchantScope().orElse(null);
        com.complex.platform.access.AccessActor creator = authorizationService.currentActor();
        InventoryHighValueReviewRecord review = repository.saveHighValueReview(new InventoryHighValueReviewRecord(
                "hvr_" + slug(idempotencySeed + ":" + type + ":" + subject),
                type,
                subject,
                photos,
                videos,
                "pending",
                null,
                null,
                now,
                null,
                merchantId,
                creator == null ? null : creator.id(),
                null,
                null));
        auditEventService.recordSuccess(
                "inventory.high_value_review_created",
                "inventory.high_value.review",
                "inventory_high_value_review",
                review.id(),
                null,
                null,
                Map.of("subject_type", type, "subject_id", subject));
        return review;
    }

    /**
     * 主管复核 approve/reject。approve → 动作生效（按 subject_type 回调）；reject → 回滚（保持原态）。
     *
     * <p>{@code onApprove} 由调用域注入：对 {@code saved_bottle_void} 即把存酒由中间态置 cancelled。
     * reject 不调 {@code onApprove}（保持原态、不扣减）。两路均写 reviewer/reason/reviewed_at + 事件。
     */
    // AUD-020: this service is hand-newed by InventoryService/PurchaseRequestService (not a Spring proxy),
    // so the capability annotation here was dead (the aspect never matched). The gate now lives on the
    // proxied outward entry InventoryService.reviewHighValue(); this method is only reached via it.
    @Transactional
    public InventoryHighValueReviewRecord review(String reviewId, HighValueReviewDecisionRequest request) {
        return reviewInternal(reviewId, request, this::applyDecisionEffect);
    }

    /**
     * 供同域调用方在事务内复用的复核入口（带自定义生效回调）。Controller 走 {@link #review}。
     */
    InventoryHighValueReviewRecord reviewInternal(
            String reviewId,
            HighValueReviewDecisionRequest request,
            java.util.function.Consumer<InventoryHighValueReviewRecord> onApprove
    ) {
        String id = requireText(reviewId, "review_id");
        return executeWrite(
                "reviewHighValue:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                "inventory.high_value_review_decided",
                "inventory.high_value_review_decide_failed",
                () -> {
                    HighValueReviewDecisionRequest actual = requireRequest(request);
                    String decision = normalizeAllowed(actual.decision(), DECISIONS, "decision");
                    InventoryHighValueReviewRecord current = repository.findHighValueReviewByIdForUpdate(id)
                            .orElseThrow(() -> ApiException.notFound(
                                    "HIGH_VALUE_REVIEW_NOT_FOUND", "高值复核不存在或已被删除"));
                    // C3：复核前校验归属——有商户 scope 时复核单必须归属本商户，否则 403（含 reject 分支，
                    // 此前 saved_bottle_void reject 不加载 subject 即无任何 scope 校验，B 可翻转 A 的复核单）。
                    authorizationService.currentMerchantScope().ifPresent(scope -> {
                        if (!scope.equals(current.merchantId())) {
                            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                    "该高值复核不属于当前商户");
                        }
                    });
                    if (!"pending".equals(current.reviewStatus())) {
                        throw ApiException.conflict(
                                "HIGH_VALUE_REVIEW_NOT_PENDING", "该高值复核不在待处理状态");
                    }
                    if (SUBJECT_SAVED_BOTTLE_STOCK_DISPOSAL.equals(current.subjectType())) {
                        return repository.saveHighValueReview(decideDisposal(current, actual, decision, onApprove));
                    }
                    // 审批角色只用于存酒库房存货处置：其余复核对象仍只认主管复核码（Controller 按 role 选码，这里堵住持财务码裁决普通复核）。
                    authorizationService.requirePermission(REVIEW_PERMISSION);
                    if ("approved".equals(decision)) {
                        requireEvidence(current.photoAttachmentIds(), current.videoAttachmentIds());
                    }
                    String reviewer = authorizationService.currentActor().id();
                    Instant now = Instant.now();
                    InventoryHighValueReviewRecord resolved = new InventoryHighValueReviewRecord(
                            current.id(), current.subjectType(), current.subjectId(),
                            current.photoAttachmentIds(), current.videoAttachmentIds(),
                            decision, reviewer, trimToNull(actual.reason()), current.createdAt(), now,
                            current.merchantId());
                    // approve → 动作生效（回调）；reject → 收口（报损/盘点差异置 rejected，存酒维持冻结/不扣减）。
                    if ("approved".equals(decision) && onApprove != null) {
                        onApprove.accept(resolved);
                    } else if ("rejected".equals(decision)) {
                        applyRejectEffect(resolved);
                    }
                    return repository.saveHighValueReview(resolved);
                });
    }

    /**
     * 存酒库房存货处置的两道审批（同赔付方案审批 {@link InventoryService#approveSavedBottleRemedy}）：role=supervisor 核门店主管复核码、
     * role=finance 核财务应收应付管理码；审批人不得是发起人；两道不得同一人；任一道驳回即驳回（不扣）；两道都批才回调扣库存。
     */
    private InventoryHighValueReviewRecord decideDisposal(
            InventoryHighValueReviewRecord current,
            HighValueReviewDecisionRequest actual,
            String decision,
            java.util.function.Consumer<InventoryHighValueReviewRecord> onApprove
    ) {
        String role = requireText(actual.role(), "role");
        boolean supervisor = "supervisor".equals(role);
        if (!supervisor && !"finance".equals(role)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "role 须为 supervisor（门店主管）或 finance（财务）");
        }
        authorizationService.requirePermission(supervisor ? InventoryService.REMEDY_SUPERVISOR_APPROVE : InventoryService.REMEDY_FINANCE_APPROVE);
        String approver = authorizationService.currentActor().id();
        if (approver.equals(current.createdBy())) {
            throw ApiException.conflict("HIGH_VALUE_REVIEW_SELF_APPROVAL", "审批人不得是发起人");
        }
        String supervisorBy = current.supervisorApprovedBy();
        String financeBy = current.financeApprovedBy();
        if ("approved".equals(decision)) {
            if (approver.equals(supervisor ? financeBy : supervisorBy)) {
                throw ApiException.conflict("HIGH_VALUE_REVIEW_SAME_APPROVER", "门店主管审批与财务审批不得由同一人完成");
            }
            supervisorBy = supervisor ? approver : supervisorBy;
            financeBy = supervisor ? financeBy : approver;
        }
        boolean done = "rejected".equals(decision) || (supervisorBy != null && financeBy != null);
        InventoryHighValueReviewRecord resolved = new InventoryHighValueReviewRecord(
                current.id(), current.subjectType(), current.subjectId(),
                current.photoAttachmentIds(), current.videoAttachmentIds(),
                done ? decision : "pending", approver, trimToNull(actual.reason()), current.createdAt(), done ? Instant.now() : null,
                current.merchantId(), current.createdBy(), supervisorBy, financeBy);
        if ("rejected".equals(decision)) {
            applyRejectEffect(resolved);
        } else if (done) {
            // 高价值报废建单时已强制照片+视频（沿用报损口径），批准时照旧复核附件；非高价值处置未要求证据。
            if (!current.photoAttachmentIds().isEmpty()) {
                requireEvidence(current.photoAttachmentIds(), current.videoAttachmentIds());
            }
            onApprove.accept(resolved);
        }
        return resolved;
    }

    private void requireEvidence(List<String> photos, List<String> videos) {
        if (evidenceAttachmentValidator == null) {
            throw ApiException.conflict("EVIDENCE_STORAGE_UNAVAILABLE", "无法核实高价值业务证据文件");
        }
        evidenceAttachmentValidator.requirePhotos(requireNonEmptyList(photos, "photo_attachment_ids"), "photo_attachment_ids");
        evidenceAttachmentValidator.requireVideos(requireNonEmptyList(videos, "video_attachment_ids"), "video_attachment_ids");
    }

    /**
     * approve 生效分发：
     * <ul>
     *   <li>{@code saved_bottle_void} → 存酒由中间态 {@code disputed_frozen} 置 {@code cancelled}；</li>
     *   <li>{@code damage_report}/{@code stock_variance} → 调整单由 {@code pending_review} 真正扣减库存并置
     *       {@code completed}（即此前未即时扣减的报损/盘点差异，approve 时才落库存变更）。</li>
     * </ul>
     */
    private void applyDecisionEffect(InventoryHighValueReviewRecord resolved) {
        if (SUBJECT_SAVED_BOTTLE_VOID.equals(resolved.subjectType())) {
            SavedBottleRecord bottle = loadSavedBottleScoped(resolved.subjectId());
            SavedBottleRecord b = bottle;
            repository.saveSavedBottle(new SavedBottleRecord(
                    b.id(), b.customerId(), b.barProjectId(), b.itemId(), b.bottleName(),
                    b.initialQuantity(), b.remainingQuantity(), b.unit(), b.expireAt(),
                    "cancelled", b.evidenceAttachmentIds(), b.createdAt(), Instant.now(),
                    b.merchantId(), b.isHighValue(), b.highValueThresholdCents(),
                    b.bottleLabelValue(), b.customerNameOrNickname(),
                    b.customerPhone(), b.customerPhoneHash(), b.registrationPhotoRefs(), b.manualNote(),
                    b.transferredAt(), b.transferToNameOrNickname(), b.transferToPhone(), b.transferToPhoneHash(),
                    b.remedyOfSavedBottleId(), b.custodianEntityId(), b.pickupLocation(), b.handedOverAt(), b.handoverId()));
        } else if (ADJUSTMENT_SUBJECTS.contains(resolved.subjectType())) {
            StockAdjustmentRecord adjustment = loadAdjustmentPending(resolved.subjectId());
            // approve → 真正扣减（报损/盘点差异/存酒库房处置均为减少库存）+ 置 completed；存酒库房处置的流水类型取原出库或报损类型。
            int affected = repository.atomicDecreaseStockBalance(
                    adjustment.itemId(), adjustment.locationId(), adjustment.quantity(),
                    authorizationService.currentMerchantScope().orElse(null));
            if (affected == 0) {
                throw ApiException.conflict("STOCK_INSUFFICIENT", "可用库存不足");
            }
            // 2e：高价值复核 approve 扣减（报损/盘点差异）也 FEFO 逐批扣批次 quantity，与 stock-movement 出库口径一致。
            int remaining = adjustment.quantity();
            for (InventoryStockBatchRecord batch : repository.listActiveBatchesForConsumption(
                    adjustment.itemId(), adjustment.locationId())) {
                if (remaining <= 0) {
                    break;
                }
                int take = Math.min(remaining, batch.quantity());
                if (repository.atomicDecreaseBatchQuantity(batch.id(), take) > 0) {
                    remaining -= take;
                }
            }
            // 裁决#7 守恒账本：高价值报损/盘点差异 approve 时才真正扣减余额，必须在同事务（@Transactional review）
            // 补写一条 inventory_stock_movements，否则此前 pending_review 阶段未写、approve 落账亦无流水，
            // "按流水重建库存"将缺这笔核减、内部人高价值报损无痕迹。
            // movement_type 忠实取 adjustment_type（报损族 loss/damage/waste、盘点差异 count_correction，均为减少 → 源库位出）。
            repository.saveStockMovement(new StockMovementRecord(
                    "movement_" + slug(adjustment.id() + ":hv_approve"),
                    adjustment.adjustmentType(),
                    adjustment.itemId(),
                    adjustment.locationId(),
                    null,
                    adjustment.quantity(),
                    // V296：高价值报损/盘点差异为出库，成本由仓储层按移动加权均价自动定稿（不用申报单价）。
                    null,
                    null,
                    "inventory_stock_adjustment",
                    adjustment.id(),
                    null,
                    null,
                    adjustment.evidenceAttachmentIds(),
                    "completed",
                    Instant.now(),
                    adjustment.merchantId()
            ), authorizationService.currentProjectScopeQuietly().orElse(null));
            repository.saveStockAdjustment(withAdjustmentStatus(adjustment, "completed"));
        }
    }

    /** reject 收口：报损/盘点差异调整单由 {@code pending_review} 置 {@code rejected}（不扣减）。存酒维持冻结由 approve 不触发实现。 */
    private void applyRejectEffect(InventoryHighValueReviewRecord resolved) {
        if (ADJUSTMENT_SUBJECTS.contains(resolved.subjectType())) {
            StockAdjustmentRecord adjustment = loadAdjustmentPending(resolved.subjectId());
            repository.saveStockAdjustment(withAdjustmentStatus(adjustment, "rejected"));
        }
    }

    private SavedBottleRecord loadSavedBottleScoped(String subjectId) {
        SavedBottleRecord bottle = repository.findSavedBottleByIdForUpdate(subjectId)
                .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "存酒记录不存在或已被删除"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(bottle.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "该存酒记录不属于当前商户");
        }
        return bottle;
    }

    private StockAdjustmentRecord loadAdjustmentPending(String subjectId) {
        StockAdjustmentRecord adjustment = repository.findStockAdjustmentByIdForUpdate(subjectId)
                .orElseThrow(() -> ApiException.notFound(
                        "STOCK_ADJUSTMENT_NOT_FOUND", "库存调整不存在或已被删除"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(adjustment.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "该库存调整不属于当前商户");
        }
        if (!"pending_review".equals(adjustment.status())) {
            throw ApiException.conflict(
                    "STOCK_ADJUSTMENT_NOT_PENDING", "该库存调整不在待审核状态");
        }
        return adjustment;
    }

    private StockAdjustmentRecord withAdjustmentStatus(StockAdjustmentRecord a, String status) {
        return new StockAdjustmentRecord(
                a.id(), a.itemId(), a.locationId(), a.adjustmentType(), a.quantity(), a.reason(),
                a.evidenceAttachmentIds(), status, a.createdAt(), a.merchantId(), a.unitCostCents());
    }

    private InventoryHighValueReviewRecord executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            String successEvent,
            String failureEvent,
            Supplier<InventoryHighValueReviewRecord> action
    ) {
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        InventoryHighValueReviewRecord response = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                "inventory.high_value.review",
                                "inventory_high_value_review",
                                response.id(),
                                null,
                                idempotencyKey,
                                Map.of("operation_id", operationId, "review_status", response.reviewStatus()));
                        return response;
                    },
                    InventoryHighValueReviewRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    "inventory.high_value.review",
                    "inventory_high_value_review",
                    "unknown",
                    null,
                    idempotencyKey,
                    codeOf(exception));
            throw exception;
        }
    }

    private PageResponse<InventoryHighValueReviewRecord> readPage(
            String successEvent,
            String failureEvent,
            Supplier<List<InventoryHighValueReviewRecord>> supplier
    ) {
        try {
            List<InventoryHighValueReviewRecord> items = supplier.get();
            auditEventService.recordSuccess(
                    successEvent, "inventory.high_value.review", "inventory_high_value_review",
                    "all", null, null, Map.of("count", items.size()));
            return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent, "inventory.high_value.review", "inventory_high_value_review",
                    "all", null, null, codeOf(exception));
            throw exception;
        }
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private List<String> requireNonEmptyList(List<String> value, String fieldName) {
        List<String> cleaned = value == null ? List.of()
                : value.stream().map(Strings::trimToNull).filter(java.util.Objects::nonNull).toList();
        if (cleaned.isEmpty()) {
            throw ApiException.badRequest("HIGH_VALUE_EVIDENCE_REQUIRED",
                    "高价值动作需照片+视频双证据；" + fieldName + " 不能为空");
        }
        return cleaned;
    }

    private String normalizeAllowed(String value, Set<String> allowed, String fieldName) {
        String normalized = requireText(value, fieldName).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return normalized;
    }

    private String slug(String seed) {
        return com.complex.platform.common.Hashing.idempotencyCode(requireText(seed, "seed"));
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return "INTERNAL_ERROR";
    }
}
