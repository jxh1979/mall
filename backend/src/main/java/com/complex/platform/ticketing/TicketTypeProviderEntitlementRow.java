package com.complex.platform.ticketing;

/**
 * S5 票面按提供方（酒吧/餐厅/零售商户）分组的权益投影行：一行 = 该票种绑定的一条权益 + 它的提供方商户。
 *
 * <p>两处共用同一份数据，杜绝「发布校验」与「票面展示」各算一遍：
 * <ul>
 *   <li><b>K17 每吧至少一杯</b>：发布时按提供方分组核对赠酒杯数；</li>
 *   <li><b>票面清单</b>：售票时写明各酒吧分别赠几杯（业务 HTML §通票赠酒按酒吧分配）。</li>
 * </ul>
 *
 * @param entitlementTemplateId  权益模板 id
 * @param templateName           权益名称（票面展示用）
 * @param entitlementType        权益类型（entry/admission/gift_drink/...）
 * @param providerMerchantId     提供方商户 id（权益模板的归属商户）
 * @param providerMerchantName   提供方商户名称
 * @param providerMerchantType   提供方商户业态（bar/catering/retail/amusement/media/other，V348+V384 六值封口）
 * @param quantity               该权益在票面上的份数
 * @param numPeople              每份的核销人数（多人票 &gt; 1）
 */
public record TicketTypeProviderEntitlementRow(
        String entitlementTemplateId,
        String templateName,
        String entitlementType,
        String providerMerchantId,
        String providerMerchantName,
        String providerMerchantType,
        int quantity,
        int numPeople
) {
    /** 票面杯数/件数 = 份数 × 每份人数。 */
    public int units() {
        return quantity * numPeople;
    }
}
