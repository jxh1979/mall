package com.complex.platform.finance;

/**
 * 第三方券结算端口(批3):内部价采购券核销回流后,按内部结算价给第三方(商户/外部伙伴)挂应付。
 * finance 侧实现(建 ar_ap_items direction=payable, counterparty=partnerId)。返回 ar_ap_item_id;幂等键防重挂。
 * 依赖方向 ticketing→finance。
 */
public interface PartnerVoucherSettlementPort {
    String createPartnerVoucherPayable(String partnerId, long internalPriceCent, String idempotencyKey);
}
