package com.complex.platform.amusement;

import java.time.Instant;

/** 游乐项目可用权益类型绑定记录（amusement_project_entitlements）。 */
public record AmusementProjectEntitlementRecord(
        String id,
        String projectId,
        String entitlementType,
        Instant createdAt
) {
}
