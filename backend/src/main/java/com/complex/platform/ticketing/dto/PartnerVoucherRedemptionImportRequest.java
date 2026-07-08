package com.complex.platform.ticketing.dto;

import java.time.Instant;

/** 第三方券核销回流导入请求(批3):餐厅自己系统核销后经对账文件/接口回流。 */
public record PartnerVoucherRedemptionImportRequest(
        String idempotencyKey,
        String entitlementCode,
        Instant redeemedAt,
        String externalRedemptionRef,
        String source
) {
}
