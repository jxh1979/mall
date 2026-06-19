package com.complex.platform.inventory.dto;

/**
 * R5 T17 员工段扫码（主动留痕）：工作手机扫平台唯一码 → 对应 segment step done。
 * 同段重复 → duplicate 异常。operatorUserId 取自认证上下文，不取请求体。
 */
public record TraceScanSegmentRequest(
        String idempotencyKey,
        String segment
) {
}
