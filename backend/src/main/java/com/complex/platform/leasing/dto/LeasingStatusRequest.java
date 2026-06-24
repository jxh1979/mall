package com.complex.platform.leasing.dto;

/** 招商线索/租赁报价状态流转请求（② 履约闭环 S4）：lead(contacted/qualified/lost) 或 offer(sent/accepted/rejected)。 */
public record LeasingStatusRequest(
        String status,
        String idempotencyKey
) {
}
