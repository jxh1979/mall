package com.complex.platform.amusement.dto;

/** CG-08b 发起补偿批次（第一岗位）：role＝本次所用岗位；reason＝发放原因；bearerCompanyId＝决定发放补偿的域主管公司法律主体。 */
public record CompensationBatchInitiateRequest(
        String idempotencyKey,
        String role,
        String reason,
        String bearerCompanyId
) {
}
