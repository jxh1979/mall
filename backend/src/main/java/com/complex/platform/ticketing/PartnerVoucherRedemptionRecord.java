package com.complex.platform.ticketing;

import java.time.Instant;

/**
 * 第三方券核销回流记录(批3):餐厅自己系统核销后,经对账文件/接口回流给我们的一条核销+结算留痕。
 * 幂等键 (partnerId, externalRedemptionRef);internal_purchased 才有 settlementAmountCent + arApItemId。
 */
public record PartnerVoucherRedemptionRecord(
        String id,
        String issuedEntitlementId,
        String entitlementCode,
        String partnerKind,
        String partnerId,
        String settlementMode,
        Instant redeemedAt,
        String externalRedemptionRef,
        Long settlementAmountCent,
        String arApItemId,
        String source,
        Instant createdAt
) {
}
