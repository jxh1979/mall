package com.complex.platform.merchantservice;

// 文件版本：V0.01

import java.time.Instant;

/**
 * 第十八批 I 段 77-R3：商户服务工单处理时限（工单类型×优先级，分钟）。
 * BSM:1385「响应/处理时限按「工单类型×优先级」矩阵后台配置」「商户服务工单不设首次响应环节」——只有处理时限。
 */
public record MerchantWorkOrderPolicyRecord(
        String workOrderType,
        String priority,
        int handleMinutes,
        String updatedBy,
        Instant updatedAt
) {
}
