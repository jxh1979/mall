package com.complex.platform.merchantservice;

// 文件版本：V0.01

import java.time.Instant;

/**
 * 第十八批 L 段第 102 条 102-R3：报修、其他服务请求的关单方式（第十八批裁决 26：handler 处理人完成即结束、staff_acceptance 员工验收后关闭、
 * merchant_confirm 须商户确认）。保洁、投诉照 BSM:1385 写死，不在此配置。
 */
public record MerchantWorkOrderCloseMethodRecord(
        String workOrderType,
        String closeMethod,
        String updatedBy,
        Instant updatedAt
) {
}
