package com.complex.platform.ticketing;

// 文件版本：V0.01

/**
 * S5 票面按提供方（酒吧/餐厅/零售商户）分组的权益投影行：一行 = 该票种绑定的一条权益 + 它的提供方商户。
 *
 * <p>两处共用同一份数据，杜绝「发布校验」与「票面展示」各算一遍：
 * <ul>
 *   <li><b>通票赠酒下限</b>（第十八批 AH 段 215-R1，ENUM:34）：发布时按提供方分组核对每家酒吧的赠酒杯数与平台出资杯数；</li>
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
 * @param totalTimes             每份可用次数（一份赠酒次数 3＝3 杯）
 * @param costBearer             成本承担方（platform_funded 平台出资型／merchant_concession 商户让利型）
 * @param componentPriceCent     票面价（0＝赠送；赠酒带票面价的是兑换券，ENUM:36）
 */
public record TicketTypeProviderEntitlementRow(
        String entitlementTemplateId,
        String templateName,
        String entitlementType,
        String providerMerchantId,
        String providerMerchantName,
        String providerMerchantType,
        int quantity,
        int numPeople,
        int totalTimes,
        String costBearer,
        Long componentPriceCent
) {
    /** 票面杯数/件数 = 份数 × 次数 × 每份人数（BSM:1032「各酒吧分别赠几杯在售票时写明票面」）。 */
    public int units() {
        return quantity * totalTimes * numPeople;
    }
}
