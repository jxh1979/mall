package com.complex.platform.access;

import java.time.Instant;

/**
 * 高危授权申请（access_grant_requests，V170，④ S2）：承载待审批的角色装配/角色加权限意图，关联主管复核 review_id。
 * requestType: user_role|role_permission；status: pending|applied|rejected。
 */
public record AccessGrantRequestRecord(
        String id,
        String requestType,
        String targetUserId,
        String targetRoleId,
        String permissionCode,
        String status,
        String reviewId,
        String reason,
        String requestedBy,
        String decidedBy,
        Instant decidedAt,
        String projectId,
        String merchantId,
        Instant createdAt,
        Instant updatedAt
) {
}
