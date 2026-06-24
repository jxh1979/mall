package com.complex.platform.governance.dto;

import java.util.List;

/**
 * 合同生效请求（② 履约闭环 S1）：approved→effective，并绑定该合同所租空间。
 * spaceIds 非空（生效合同必须明确占用哪些经营空间，准入闸 over_space 据此判定）。
 */
public record ContractActivateRequest(
        List<String> spaceIds,
        String idempotencyKey
) {
}
