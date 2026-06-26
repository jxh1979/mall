package com.complex.platform.contract.dto;

import java.util.Map;

/** 发起合同变更（6类，删转租）。 */
public record ContractChangeRequest(
        String idempotencyKey,
        String changeType,
        Map<String, Object> payload
) {
}
