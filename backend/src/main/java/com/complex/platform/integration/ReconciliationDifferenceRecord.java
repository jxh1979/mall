package com.complex.platform.integration;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 对账差异一行。
 *
 * <p>P0-E：{@code internalAmountCent}（平台侧）与 {@code providerAmountCent}（第三方侧）<b>只能由系统取数落值</b>，
 * 任何路径都不接受请求体手填（见 {@link IntegrationReconciliationService}）。
 * {@code amountSource} 标注该行由系统自动对账产出（system）还是人工登记（manual_registration，金额仍是系统取数）；
 * {@code registeredByUserId} 为人工登记人，复核时用于「登记人不得自审」守卫。
 */
public record ReconciliationDifferenceRecord(
        String id,
        String batchId,
        String providerCode,
        String businessType,
        String externalBusinessId,
        String differenceType,
        long internalAmountCent,
        long providerAmountCent,
        long differenceAmountCent,
        String status,
        String sourceDomain,
        String assignedToUserId,
        String resolutionNote,
        Map<String, Object> providerPayload,
        List<String> evidenceAttachmentIds,
        String amountSource,
        String registeredByUserId,
        Instant createdAt,
        Instant updatedAt
) {
}
