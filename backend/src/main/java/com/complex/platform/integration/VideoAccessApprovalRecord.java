package com.complex.platform.integration;

import java.time.Instant;

/**
 * 视频调阅主管复核审批（2.2）。approval_ref = id；调阅时须匹配 status=approved、viewer_user_id、
 * 授权对象 point_id（NULL=全部）且未过 valid_until，否则 403。
 */
public record VideoAccessApprovalRecord(
        String id,
        String viewerUserId,
        String pointId,
        String status,
        String approvedBy,
        Instant validUntil,
        Instant createdAt,
        Instant updatedAt
) {
}
