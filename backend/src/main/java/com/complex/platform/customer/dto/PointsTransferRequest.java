package com.complex.platform.customer.dto;

/** 积分转赠请求（客人端，from=当前登录会员）。 */
public record PointsTransferRequest(
        String idempotencyKey,
        String toCustomerId,
        Long points,
        String reason,
        String projectId
) {
}
