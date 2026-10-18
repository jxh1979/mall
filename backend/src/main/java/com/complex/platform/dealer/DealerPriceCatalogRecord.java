package com.complex.platform.dealer;

import java.time.Instant;

/** 合同销售价格目录版本。status：draft 草稿 / pending_approval 待审批 / effective 已生效 / expired 已失效（ENUM:172）。 */
public record DealerPriceCatalogRecord(
        String id,
        String dealerContractId,
        int version,
        String status,
        String editedBy,
        String submittedBy,
        Instant submittedAt,
        String approvedBy,
        Instant approvedAt,
        Instant effectiveAt,
        Instant expiredAt,
        String note,
        Instant createdAt,
        Instant updatedAt
) {
}
