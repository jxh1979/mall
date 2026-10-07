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
     * P0-A/GAP-07 展示脱敏：返回掩码手机号。M724（治理地图第 871 行，操作员 2026-10-07 确认）起线索列表和操作结果一律走这里，
     * 不论是否持有 {@code pii.contact_phone.view}；完整号码只经「查看完整号码」接口按条给出。
     * 脱敏放在 controller（service 外），与 V279 内部结算价同款范式。
     */
    public LeasingLeadRecord withMaskedPhone() {
        return new LeasingLeadRecord(id, contactName, com.complex.platform.common.PhoneMask.mask(contactPhone),
                source, intendedBusinessType, intendedSpaceId, intendedAreaSqm, brandId, ownerUserId, status,
                lostOrHoldReason, lastFollowUpAt, projectId, createdAt, updatedAt);
    }
}
