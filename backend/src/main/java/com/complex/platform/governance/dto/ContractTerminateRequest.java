package com.complex.platform.governance.dto;

/** 合同退租请求（② 履约闭环 S3）：effective→terminated，录退租原因 + 释放所租空间。 */
public record ContractTerminateRequest(
        String reason,
        String idempotencyKey
) {
}
