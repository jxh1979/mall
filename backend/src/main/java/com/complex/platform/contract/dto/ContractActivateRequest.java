package com.complex.platform.contract.dto;

import java.util.List;

/** 生效：固化快照 + 绑定所租空间。 */
public record ContractActivateRequest(
        String idempotencyKey,
        List<String> spaceIds
) {
}
