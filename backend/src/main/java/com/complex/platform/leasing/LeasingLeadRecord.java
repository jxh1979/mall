package com.complex.platform.leasing;

import java.time.Instant;

/** 招商线索（leasing_leads，V164，② 履约闭环 S4）：潜在商户/意向。status: new→contacted→qualified→converted/lost。 */
public record LeasingLeadRecord(
        String id,
        String contactName,
        String intendedBusinessType,
        String intendedSpaceId,
        String source,
        String status,
        String projectId,
        Instant createdAt,
        Instant updatedAt
) {
}
