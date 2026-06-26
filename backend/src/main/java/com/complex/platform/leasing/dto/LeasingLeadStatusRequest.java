package com.complex.platform.leasing.dto;

/** 招商线索状态流转请求（V161 重建）。status 为 7 态之一；lost/on_hold 须填 reason。 */
public record LeasingLeadStatusRequest(
        String status,
        String reason,
        String idempotencyKey
) {
}
