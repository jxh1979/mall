package com.complex.platform.customer;

// 文件版本：V0.01

import java.time.Instant;

/** ROOT-03-R23 黑名单列入／解除申请（ENUM 第 68 行「保安团队申请、夜班值班经理批准、娱乐平台负责人批准」）。 */
public record CustomerBlacklistRequestRecord(
        String id,
        String customerId,
        String projectId,
        String action,
        String reason,
        String status,
        String requestedBy,
        Instant requestedAt,
        String nightManagerApprovedBy,
        Instant nightManagerApprovedAt,
        String platformHeadApprovedBy,
        Instant platformHeadApprovedAt,
        String rejectedBy,
        Instant rejectedAt
) {
}
