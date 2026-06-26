package com.complex.platform.leasing;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 招商线索（leasing_leads，V161 重建）：地产履约链最前端。
 * status 7 态对齐业务 HTML §10：pending_follow_up/following_up/converted_to_intent/negotiating/signed/lost/on_hold。
 * source 4 类：outbound/online_inquiry/referral/exhibition。
 */
public record LeasingLeadRecord(
        String id,
        String contactName,
        String contactPhone,
        String source,
        String intendedBusinessType,
        String intendedSpaceId,
        BigDecimal intendedAreaSqm,
        String brandId,
        String ownerUserId,
        String status,
        String lostOrHoldReason,
        Instant lastFollowUpAt,
        String projectId,
        Instant createdAt,
        Instant updatedAt
) {
}
