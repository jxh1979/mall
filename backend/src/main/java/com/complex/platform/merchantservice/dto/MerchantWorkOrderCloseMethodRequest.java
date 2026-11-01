package com.complex.platform.merchantservice.dto;

// 文件版本：V0.01

/** 第十八批 L 段第 102 条 102-R3：保存报修或其他服务请求的关单方式（裁决 26）。 */
public record MerchantWorkOrderCloseMethodRequest(
        String workOrderType,
        String closeMethod,
        String idempotencyKey
) {
}
