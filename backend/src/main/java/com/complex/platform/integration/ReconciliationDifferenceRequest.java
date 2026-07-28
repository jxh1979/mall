package com.complex.platform.integration;

import java.util.List;
import java.util.Map;

/**
 * 登记对账差异请求。
 *
 * <p>P0-E：三个金额字段（internalAmountCent / providerAmountCent / differenceAmountCent）已<b>废止</b>——
 * 保留在 DTO 里只是为了对老调用方 fail-loud：带任一非空值即 400
 * {@code RECONCILIATION_MANUAL_AMOUNT_FORBIDDEN}，而不是被静默忽略。
 * 金额一律由 {@link IntegrationReconciliationService} 系统取数（第三方侧取回流聚合、平台侧本域无取数源恒 0）。
 */
public record ReconciliationDifferenceRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String batchId,
        String providerCode,
        String businessType,
        String externalBusinessId,
        String differenceType,
        // 已废止：平台侧金额禁止手填，传入即 400。
        Long internalAmountCent,
        // 已废止：第三方侧金额由回流系统取数，传入即 400。
        Long providerAmountCent,
        // 已废止：差额由系统两侧取数相减，传入即 400。
        Long differenceAmountCent,
        String assignedToUserId,
        Map<String, Object> providerPayload
) {
}
