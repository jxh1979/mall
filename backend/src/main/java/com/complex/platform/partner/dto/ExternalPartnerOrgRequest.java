package com.complex.platform.partner.dto;

import java.time.Instant;

/** 平台管理员 录入/修改 外部协作机构。 */
public record ExternalPartnerOrgRequest(
        String idempotencyKey,
        String projectId,
        String orgName,
        String orgCategory,
        String linkedSupplierId,
        Integer maxSubordinates,
        Instant validUntil,
        String status
) {
}
