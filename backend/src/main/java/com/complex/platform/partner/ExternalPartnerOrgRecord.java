package com.complex.platform.partner;

import java.time.Instant;

/** 外部协作机构（外包/服务机构主体，独立于供货 inventory_suppliers）。条目2 门户。 */
public record ExternalPartnerOrgRecord(
        String id,
        String projectId,
        String orgName,
        String orgCategory,
        String linkedSupplierId,
        int maxSubordinates,
        Instant validUntil,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
