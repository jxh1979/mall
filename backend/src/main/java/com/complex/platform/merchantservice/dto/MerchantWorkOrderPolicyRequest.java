package com.complex.platform.merchantservice.dto;

// 文件版本：V0.01

/** 第十八批 I 段 77-R3：保存商户服务工单处理时限（工单类型×优先级一格）。 */
public record MerchantWorkOrderPolicyRequest(
        String workOrderType,
        String priority,
        Integer handleMinutes,
        String idempotencyKey
) {
}
