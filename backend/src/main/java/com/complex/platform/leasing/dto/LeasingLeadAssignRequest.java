package com.complex.platform.leasing.dto;

/** 招商线索分配请求（V161 重建）：招商主管手动指派负责人。 */
public record LeasingLeadAssignRequest(
        String ownerUserId,
        String idempotencyKey
) {
}
