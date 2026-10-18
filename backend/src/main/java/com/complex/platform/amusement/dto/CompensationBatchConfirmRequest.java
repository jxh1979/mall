package com.complex.platform.amusement.dto;

/** CG-08b 第二岗位确认并发放：须与发起人不同人、另一岗位。 */
public record CompensationBatchConfirmRequest(
        String idempotencyKey,
        String role,
        String note
) {
}
