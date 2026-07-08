package com.complex.platform.ticketing;

import java.time.Instant;

/**
 * 第三方券配置(批3):把某票务权益模板标记为"第三方券"并挂结算口径。通票包含该权益即附赠此券。
 * settlementMode: free_marketing(免费营销·核销不结算) | internal_purchased(内部价采购·核销挂应付)。
 * partnerKind: merchant(入驻餐饮商户) | external_partner(外部合作餐厅 external_partner_orgs)。
 * internalPriceCent: 内部结算价(internal_purchased 时每核销一张挂此应付给第三方)。
 */
public record PartnerVoucherConfigRecord(
        String entitlementTemplateId,
        String settlementMode,
        String partnerKind,
        String partnerId,
        Long internalPriceCent,
        Instant createdAt
) {
}
