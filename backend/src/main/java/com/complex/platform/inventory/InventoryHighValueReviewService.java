package com.complex.platform.inventory;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.gate.GuardedAction;
import com.complex.platform.inventory.dto.HighValueReviewDecisionRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private static final Set<String> SUBJECT_TYPES =
            Set.of(SUBJECT_SAVED_BOTTLE_VOID, SUBJECT_DAMAGE_REPORT, SUBJECT_STOCK_VARIANCE);
    private static final Set<String> DECISIONS = Set.of("approved", "rejected");

    private final InventoryRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public InventoryHighValueReviewService(
            InventoryRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    public PageResponse<InventoryHighValueReviewRecord> listHighValueReviews() {
        return readPage(
                "inventory.high_value_review.viewed",
                "inventory.high_value_review.view_failed",
                () -> repository.listHighValueReviews());
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
        String type = normalizeAllowed(subjectType, SUBJECT_TYPES, "subject_type");
        String subject = requireText(subjectId, "subject_id");
        List<String> photos = requireNonEmptyList(photoAttachmentIds, "photo_attachment_ids");
        List<String> videos = requireNonEmptyList(videoAttachmentIds, "video_attachment_ids");
        Instant now = Instant.now();
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
    @GuardedAction(capability = "inventory.high_value.review")
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
                                    "HIGH_VALUE_REVIEW_NOT_FOUND", "high value review not found"));
                    if (!"pending".equals(current.reviewStatus())) {
                        throw ApiException.conflict(
                                "HIGH_VALUE_REVIEW_NOT_PENDING", "high value review is not pending");
                    }
                    String reviewer = authorizationService.currentActor().id();
                    Instant now = Instant.now();
                    InventoryHighValueReviewRecord resolved = new InventoryHighValueReviewRecord(
                            current.id(), current.subjectType(), current.subjectId(),
                            current.photoAttachmentIds(), current.videoAttachmentIds(),
                            decision, reviewer, trimToNull(actual.reason()), current.createdAt(), now);
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
            repository.saveSavedBottle(new SavedBottleRecord(
                    bottle.id(), bottle.customerId(), bottle.barProjectId(), bottle.itemId(), bottle.bottleName(),
                    bottle.initialQuantity(), bottle.remainingQuantity(), bottle.unit(), bottle.expireAt(),
                    "cancelled", bottle.evidenceAttachmentIds(), bottle.createdAt(), Instant.now(),
                    bottle.merchantId(), bottle.isHighValue(), bottle.highValueThresholdCents(),
                    bottle.bottleLabelValue(), bottle.customerNameOrNickname(),
                    bottle.customerPhone(), bottle.customerPhoneHash()));
        } else if (SUBJECT_DAMAGE_REPORT.equals(resolved.subjectType())
                || SUBJECT_STOCK_VARIANCE.equals(resolved.subjectType())) {
            StockAdjustmentRecord adjustment = loadAdjustmentPending(resolved.subjectId());
            // approve → 真正扣减（报损/盘点差异均为减少库存）+ 置 completed。
            int affected = repository.atomicDecreaseStockBalance(
                    adjustment.itemId(), adjustment.locationId(), adjustment.quantity());
            if (affected == 0) {
                throw ApiException.conflict("STOCK_INSUFFICIENT", "available stock is insufficient");
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
                    "inventory_stock_adjustment",
                    adjustment.id(),
                    null,
                    null,
                    adjustment.evidenceAttachmentIds(),
                    "completed",
                    Instant.now(),
                    adjustment.merchantId()
            ));
            repository.saveStockAdjustment(withAdjustmentStatus(adjustment, "completed"));
        }
    }

    /** reject 收口：报损/盘点差异调整单由 {@code pending_review} 置 {@code rejected}（不扣减）。存酒维持冻结由 approve 不触发实现。 */
    private void applyRejectEffect(InventoryHighValueReviewRecord resolved) {
        if (SUBJECT_DAMAGE_REPORT.equals(resolved.subjectType())
                || SUBJECT_STOCK_VARIANCE.equals(resolved.subjectType())) {
            StockAdjustmentRecord adjustment = loadAdjustmentPending(resolved.subjectId());
            repository.saveStockAdjustment(withAdjustmentStatus(adjustment, "rejected"));
        }
    }

    private SavedBottleRecord loadSavedBottleScoped(String subjectId) {
        SavedBottleRecord bottle = repository.findSavedBottleByIdForUpdate(subjectId)
                .orElseThrow(() -> ApiException.notFound("SAVED_BOTTLE_NOT_FOUND", "saved bottle not found"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(bottle.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "saved bottle does not belong to your merchant");
        }
        return bottle;
    }

    private StockAdjustmentRecord loadAdjustmentPending(String subjectId) {
        StockAdjustmentRecord adjustment = repository.findStockAdjustmentByIdForUpdate(subjectId)
                .orElseThrow(() -> ApiException.notFound(
                        "STOCK_ADJUSTMENT_NOT_FOUND", "stock adjustment not found"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(adjustment.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "stock adjustment does not belong to your merchant");
        }
        if (!"pending_review".equals(adjustment.status())) {
            throw ApiException.conflict(
                    "STOCK_ADJUSTMENT_NOT_PENDING", "stock adjustment is not pending review");
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
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private List<String> requireNonEmptyList(List<String> value, String fieldName) {
        List<String> cleaned = value == null ? List.of()
                : value.stream().map(this::trimToNull).filter(java.util.Objects::nonNull).toList();
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String slug(String seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                            requireText(seed, "seed").getBytes(StandardCharsets.UTF_8)))
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
