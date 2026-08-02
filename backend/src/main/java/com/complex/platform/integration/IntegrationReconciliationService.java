package com.complex.platform.integration;

import static com.complex.platform.integration.IntegrationValidation.*;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

final class IntegrationReconciliationService {

    /** 人工登记差异的固定取数来源标记（金额仍系统取数，仅「登记」动作是人工）。 */
    private static final String SOURCE_MANUAL_REGISTRATION = "manual_registration";
    /** 系统自动对账产出的差异。 */
    static final String SOURCE_SYSTEM = "system";
    /** 人工登记差异强制进入的复核态。 */
    private static final String STATUS_NEEDS_REVIEW = "needs_review";

    private final IntegrationRepository repository;
    private final Clock clock;
    private final IntegrationOperationSupport support;

    IntegrationReconciliationService(IntegrationRepository repository, Clock clock, IntegrationOperationSupport support) {
        this.repository = repository;
        this.clock = clock;
        this.support = support;
    }

    PageResponse<ReconciliationBatchRecord> listReconciliationBatches() {
        return support.readPage("integration.reconciliation_batch.viewed", "integration.reconciliation_batch.view_failed",
                "integration.reconciliation.view", "integration_reconciliation_batch", repository::listReconciliationBatches);
    }

    ReconciliationBatchRecord createReconciliationBatch(ReconciliationBatchRequest request) {
        return support.executeWrite(
                "createIntegrationReconciliationBatch",
                request == null ? null : request.idempotencyKey(),
                request,
                ReconciliationBatchRecord.class,
                "integration.reconciliation_batch.created",
                "integration.reconciliation_batch.create_failed",
                "integration.reconciliation.manage",
                "integration_reconciliation_batch",
                () -> createReconciliationBatchOnce(request)
        );
    }

    PageResponse<ReconciliationDifferenceRecord> listReconciliationDifferences() {
        return support.readPage("integration.reconciliation_difference.viewed", "integration.reconciliation_difference.view_failed",
                "integration.reconciliation.view", "integration_reconciliation_difference", repository::listReconciliationDifferences);
    }

    ReconciliationDifferenceRecord createReconciliationDifference(ReconciliationDifferenceRequest request, String actorUserId) {
        return support.executeWrite(
                "createIntegrationReconciliationDifference",
                request == null ? null : request.idempotencyKey(),
                request,
                ReconciliationDifferenceRecord.class,
                "integration.reconciliation_difference.created",
                "integration.reconciliation_difference.create_failed",
                "integration.reconciliation.manage",
                "integration_reconciliation_difference",
                () -> createReconciliationDifferenceOnce(request, actorUserId)
        );
    }

    ReconciliationDifferenceRecord resolveReconciliationDifference(
            String id,
            ReconciliationDifferenceResolveRequest request,
            String actorUserId
    ) {
        String differenceId = requireTextValue(id, "id");
        return support.executeWrite(
                "resolveIntegrationReconciliationDifference:" + differenceId,
                request == null ? null : request.idempotencyKey(),
                new ResolveDifferenceFingerprint(differenceId, request),
                ReconciliationDifferenceRecord.class,
                "integration.reconciliation_difference.resolved",
                "integration.reconciliation_difference.resolve_failed",
                "integration.reconciliation.resolve",
                "integration_reconciliation_difference",
                () -> resolveReconciliationDifferenceOnce(differenceId, request, actorUserId)
        );
    }

    private ReconciliationBatchRecord createReconciliationBatchOnce(ReconciliationBatchRequest request) {
        validateReconciliationBatchRequest(request);
        int expectedCount = nonNegative(request.expectedCount(), "expected_count");
        int actualCount = nonNegative(request.actualCount(), "actual_count");
        int differenceCount = nonNegative(request.differenceCount(), "difference_count");
        if (expectedCount != actualCount && differenceCount == 0) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "期望笔数与实际笔数不一致时，差异笔数必须大于 0");
        }
        Instant now = clock.instant();
        return repository.saveReconciliationBatch(new ReconciliationBatchRecord(
                "reconciliation_batch_" + UUID.randomUUID(),
                request.providerCode(),
                request.businessType(),
                request.periodStart(),
                request.periodEnd(),
                differenceCount > 0 ? "difference_found" : "matched",
                expectedCount,
                actualCount,
                differenceCount,
                nonNegative(request.totalDifferenceCent(), "total_difference_cent"),
                now,
                now
        ));
    }

    /**
     * P0-E 对账旁路手填封堵：人工端点改为「<b>登记差异</b>」，一切金额由系统取数，请求体不接受任何手填金额。
     *
     * <ul>
     *   <li><b>禁手填</b>：请求体带 internal_amount_cent / provider_amount_cent / difference_amount_cent 任一 →
     *       400 {@code RECONCILIATION_MANUAL_AMOUNT_FORBIDDEN}（fail-loud，不静默忽略——静默会让操作员
     *       以为自己填的数生效了）。</li>
     *   <li><b>第三方侧系统取数</b>：provider_amount_cent 由 integration_order_syncs 按
     *       (provider_code, business_type, external_business_id) 聚合（订单额 − 退款额，钳 0）。
     *       取数 ≤ 0（该单第三方根本没回流）→ 400 {@code RECONCILIATION_NO_SYSTEM_TAKINGS}，
     *       与 FinanceService.createDifference 的「无系统取数支撑一律拒绝」同口径：
     *       要登记这笔，先把第三方回流按正规通道落库（order-sync 摄入 / 渠道自动对账），而不是在这里手打数字。</li>
     *   <li><b>平台侧恒 0</b>：本域没有任何按 external_business_id 可查的平台侧金额台账，
     *       所以人工登记只能表达「第三方回流有这笔、平台侧无对应取数」这一件事。
     *       平台侧确有对应取数的差异，必须走 {@code POST /reconciliation/channel-auto}（系统比对产出），
     *       绝不允许在此手敲平台侧金额。</li>
     *   <li><b>强制进复核</b>：status = needs_review，且记 registered_by_user_id；
     *       复核（resolve）时守卫登记人不得自审。</li>
     * </ul>
     */
    private ReconciliationDifferenceRecord createReconciliationDifferenceOnce(
            ReconciliationDifferenceRequest request, String actorUserId) {
        validateReconciliationDifferenceRequest(request);
        rejectManualAmounts(request);
        ReconciliationBatchRecord batch = repository.findReconciliationBatchById(request.batchId())
                .orElseThrow(() -> ApiException.notFound("RECONCILIATION_BATCH_NOT_FOUND", "对账批次不存在或已被删除"));
        if (!batch.providerCode().equals(request.providerCode()) || !batch.businessType().equals(request.businessType())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "差异范围必须与对账批次一致");
        }

        // 第三方侧一律系统取数；无回流支撑 → 拒绝（纯手填差异不存在合法入口）。
        long providerAmountCent = repository.aggregateProviderTakingsForExternalBusiness(
                request.providerCode(), request.businessType(), request.externalBusinessId());
        if (providerAmountCent <= 0) {
            throw ApiException.badRequest("RECONCILIATION_NO_SYSTEM_TAKINGS",
                    "差异登记被拒绝：外部业务单号 " + request.externalBusinessId() + " 无第三方系统收款，请先登记渠道回流数据，不允许仅手工登记的差异");
        }
        // 平台侧无按外部单号可查的取数源 → 恒 0（= 平台侧无对应取数），差额即第三方回流全额。
        long internalAmountCent = 0L;
        long differenceAmountCent = Math.abs(internalAmountCent - providerAmountCent);

        Instant now = clock.instant();
        return repository.saveReconciliationDifference(new ReconciliationDifferenceRecord(
                "reconciliation_difference_" + UUID.randomUUID(),
                request.batchId(),
                request.providerCode(),
                request.businessType(),
                request.externalBusinessId(),
                requireTextValue(request.differenceType(), "difference_type"),
                internalAmountCent,
                providerAmountCent,
                differenceAmountCent,
                STATUS_NEEDS_REVIEW,
                "platform",
                request.assignedToUserId(),
                null,
                safeMap(request.providerPayload()),
                safeList(request.evidenceAttachmentIds()),
                SOURCE_MANUAL_REGISTRATION,
                requireTextValue(actorUserId, "actor_user_id"),
                now,
                now
        ));
    }

    /** 手填金额一律拒绝（fail-loud）：三个金额字段任一非空即 400。 */
    private void rejectManualAmounts(ReconciliationDifferenceRequest request) {
        if (request.internalAmountCent() != null
                || request.providerAmountCent() != null
                || request.differenceAmountCent() != null) {
            throw ApiException.badRequest("RECONCILIATION_MANUAL_AMOUNT_FORBIDDEN",
                    "对账金额只能由系统计算得出：" + "内部金额/渠道金额/差异金额不允许手工填写");
        }
    }

    /**
     * 复核（处置）差异。P0-E 追加守卫：人工登记的差异（needs_review）<b>登记人不得自审</b>，
     * 须由另一名有 integration.reconciliation.resolve 的同事复核，对齐结算配置中心的差人复核范式。
     * actorUserId 取自会话（controller 传入），不采信请求体 operator_user_id（那是可自报的）。
     */
    private ReconciliationDifferenceRecord resolveReconciliationDifferenceOnce(
            String id,
            ReconciliationDifferenceResolveRequest request,
            String actorUserId
    ) {
        validateReconciliationDifferenceResolveRequest(request);
        ReconciliationDifferenceRecord existing = repository.findReconciliationDifferenceById(id)
                .orElseThrow(() -> ApiException.notFound("RECONCILIATION_DIFFERENCE_NOT_FOUND", "对账差异不存在或已被删除"));
        requireDifferentReviewer(existing, actorUserId);
        ReconciliationDifferenceRecord updated = new ReconciliationDifferenceRecord(
                existing.id(),
                existing.batchId(),
                existing.providerCode(),
                existing.businessType(),
                existing.externalBusinessId(),
                existing.differenceType(),
                existing.internalAmountCent(),
                existing.providerAmountCent(),
                existing.differenceAmountCent(),
                reconciliationStatus(request.status()),
                existing.sourceDomain(),
                request.operatorUserId(),
                request.resolutionNote(),
                existing.providerPayload(),
                safeList(request.evidenceAttachmentIds()),
                existing.amountSource(),
                existing.registeredByUserId(),
                existing.createdAt(),
                clock.instant()
        );
        return repository.saveReconciliationDifference(updated);
    }

    /** 差人守卫：登记人 = 复核人 → 403。系统自动产出的差异（无登记人）不受此限。 */
    private void requireDifferentReviewer(ReconciliationDifferenceRecord existing, String actorUserId) {
        String registeredBy = existing.registeredByUserId();
        if (registeredBy != null && !registeredBy.isBlank() && registeredBy.equals(actorUserId)) {
            throw ApiException.forbidden("SELF_REVIEW_FORBIDDEN",
                    "登记人不能复核自己登记的对账差异，须由另一位有对账处置权限的同事复核");
        }
    }

    private record ResolveDifferenceFingerprint(String id, ReconciliationDifferenceResolveRequest request) {
    }
}
