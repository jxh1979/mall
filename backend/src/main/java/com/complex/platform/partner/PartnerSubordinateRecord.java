package com.complex.platform.partner;

import java.time.Instant;

/** 外部机构下属人员账号（access_users 中 partner_org_id 指向某机构的账号视图）。 */
public record PartnerSubordinateRecord(
        String id,
        String username,
        String partnerOrgId,
        String accountType,
        String status,
        Instant createdAt
) {
}
