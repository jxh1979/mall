package com.complex.platform.inventory;

// 文件版本：V0.01

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

/**
 * FS-016 人工例外取酒申请（saved_bottle_manual_pickups）。BSM L1073 四项：店长发起（含消费历史条件）→ 另一主管复核
 * → 营销确认 → 本人到场放酒；状态线性：initiated → reviewed → marketing_confirmed → dispensed。
 */
public record SavedBottleManualPickupRecord(
        String id,
        String savedBottleId,
        @JsonIgnore String merchantId,
        String projectId,
        String customerId,
        @JsonIgnore String phoneHash,
        String reason,
        String historyCondition,
        String initiatedBy,
        Instant initiatedAt,
        String reviewedBy,
        Instant reviewedAt,
        String marketingConfirmedBy,
        String marketingConfirmBasis,
        Instant marketingConfirmedAt,
        String dispensedBy,
        Instant dispensedAt,
        String claimId,
        String status,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt
) {
}
