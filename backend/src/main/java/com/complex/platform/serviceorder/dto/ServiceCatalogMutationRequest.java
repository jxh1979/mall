package com.complex.platform.serviceorder.dto;

/** 服务目录 存档/删除申请/删除审批 请求(V272 软删)。reason=存档或删除理由;decision=approved/rejected(仅审批用)。 */
public record ServiceCatalogMutationRequest(
        String idempotencyKey,
        String reason,
        String decision
) {
}
