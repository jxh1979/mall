package com.complex.platform.merchantservice;

import java.time.Instant;
import java.util.List;

public record MerchantWorkOrderRecord(
        String id,
        String merchantId,
        String spaceId,
        String contractId,
        String workOrderType,
        String priority,
        String status,
        String title,
        String description,
        String reportedByUserId,
        String reportedChannel,
        String ownerDepartment,
        String ownerUserId,
        Instant dueAt,
        Instant closedAt,
        String resolution,
        Integer satisfactionScore,
        List<String> evidenceAttachmentIds,
        Instant createdAt,
        Instant updatedAt
) {
    public MerchantWorkOrderRecord {
        evidenceAttachmentIds = evidenceAttachmentIds == null ? List.of() : List.copyOf(evidenceAttachmentIds);
    }

    public MerchantWorkOrderRecord assign(String ownerDepartment, String ownerUserId, Instant now) {
        return new MerchantWorkOrderRecord(
                id,
                merchantId,
                spaceId,
                contractId,
                workOrderType,
                priority,
                "assigned",
                title,
                description,
                reportedByUserId,
                reportedChannel,
                ownerDepartment,
                ownerUserId,
                dueAt,
                closedAt,
                resolution,
                satisfactionScore,
                evidenceAttachmentIds,
                createdAt,
                now
        );
    }

    public MerchantWorkOrderRecord transitionTo(String targetStatus, Instant now) {
        return new MerchantWorkOrderRecord(
                id,
                merchantId,
                spaceId,
                contractId,
                workOrderType,
                priority,
                targetStatus,
                title,
                description,
                reportedByUserId,
                reportedChannel,
                ownerDepartment,
                ownerUserId,
                dueAt,
                closedAt,
                resolution,
                satisfactionScore,
                evidenceAttachmentIds,
                createdAt,
                now
        );
    }

    public MerchantWorkOrderRecord close(String resolution, Integer satisfactionScore, Instant now) {
        return new MerchantWorkOrderRecord(
                id,
                merchantId,
                spaceId,
                contractId,
                workOrderType,
                priority,
                "closed",
                title,
                description,
                reportedByUserId,
                reportedChannel,
                ownerDepartment,
                ownerUserId,
                dueAt,
                now,
                resolution,
                satisfactionScore,
                evidenceAttachmentIds,
                createdAt,
                now
        );
    }
}
