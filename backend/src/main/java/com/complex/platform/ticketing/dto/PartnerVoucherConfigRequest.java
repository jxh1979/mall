package com.complex.platform.ticketing.dto;

/** 第三方券配置请求(批3):把某票务权益模板标记为第三方券并挂结算口径。 */
public record PartnerVoucherConfigRequest(
        String idempotencyKey,
        String entitlementTemplateId,
        String settlementMode,
        String partnerKind,
        String partnerId,
        Long internalPriceCent
) {
}
