package com.complex.platform.inventory.dto;

/**
 * V128 / REQ-39 溯源码贴码登记请求（酒入库贴码）。
 *
 * <p>{@code itemId} 必填（物料/商品，供应链域只读引用）；{@code batchId} 批次可空；{@code traceCode} 码值可空，
 * 缺省由服务端按幂等键稳定生成。status 固定为 registered，merchantId 不由请求体传入，强制取自认证上下文
 * （归属隔离）。建码幂等：同一幂等键重复提交返回同一登记结果。
 */
public record RegisterTraceCodeRequest(
        String idempotencyKey,
        String itemId,
        String batchId,
        String traceCode
) {
}
