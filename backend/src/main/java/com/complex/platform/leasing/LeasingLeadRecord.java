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

    /**
     * P0-A/GAP-07 展示脱敏：无 {@code pii.contact_phone.view} 权限的读端返回掩码手机号。
     * 脱敏放在 controller（service 外），与 V279 内部结算价同款范式。
     */
    public LeasingLeadRecord withMaskedPhone() {
        return new LeasingLeadRecord(id, contactName, com.complex.platform.common.PhoneMask.mask(contactPhone),
                source, intendedBusinessType, intendedSpaceId, intendedAreaSqm, brandId, ownerUserId, status,
                lostOrHoldReason, lastFollowUpAt, projectId, createdAt, updatedAt);
    }
}
