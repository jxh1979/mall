package com.complex.platform.contract.dto;

/** 主管裁决退租争议，续算至结清。 */
public record ContractResolveDisputeRequest(
        String idempotencyKey,
        String resolutionNote
) {
}
