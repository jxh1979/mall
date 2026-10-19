package com.complex.platform.ticketing;

// 文件版本：V0.02

/**
 * R5 T15 事件 {@code ticket.gift_verified}：赠饮权益（entitlement_type='gift_drink'）核销成功后发布。
 *
 * <p>事件驱动、同步发布（便于测试且与核销同事务）。bar 域 {@code BarGiftConsumer} 监听后生成 0 元赠饮单、
 * 按需扣吧台库存，并按内部结算价向提供方酒吧挂应付。
 *
 * @param entitlementId        被核销的 issued_entitlement id
 * @param verificationId       本次核销记录 id（幂等键来源：bar_gift_redemptions.verification_id 唯一）
 * @param itemRef              提供方商品 id（S5 起语义定死 = {@code bar_products.id}；建模板时已校验存在且属本提供方）
 * @param barId                提供方酒吧（= 权益的提供方商户；K16 已保证本次核销就发生在这家）
 * @param tableSessionId       关联台位会话 id（可选，留痕用）
 * @param merchantId           商户 id（自营吧为空 → 无商户对手方，不挂应付但成本照记）
 * @param settlementPriceCent  本次核销固化的内部结算价（分）。S5：由票务侧统一解析器算出并随事件下发，
 *                             兑付与预扣同源；为空 = 未解析（票务侧已 fail-closed，正常不会到这）
 * @param costBearer           S6 该权益票面配置的成本承担方（{@code ticket_type_entitlements.cost_bearer}）。
 *                             {@code merchant_concession}=商户让利型 → 平台不承担成本、<b>不挂商户应付</b>（HTML:737）；
 *                             {@code platform_funded}（或空=缺省）=平台出资型 → 按内部结算价挂应付。
 *                             履约（0 元单/出品/扣库）不受承担方影响——酒照给客人，只是钱谁出的问题。
 *                             {@link #COST_BEARER_COMPENSATION}＝FS-012 补偿补发的赠饮：由补偿批次承担主体经补偿应付与酒吧结算，
 *                             <b>不挂「平台→酒吧」赠饮应付</b>（BSM L800「补偿履约与原履约分别记录」）。
 */
public record GiftVerifiedEvent(
        String entitlementId,
        String verificationId,
        String itemRef,
        String barId,
        String tableSessionId,
        String merchantId,
        Long settlementPriceCent,
        String costBearer
) {

    /** FS-012：补偿补发赠饮的成本承担标记（承担主体经补偿应付结算，不挂平台赠饮应付）。 */
    public static final String COST_BEARER_COMPENSATION = "compensation";

    /** 兼容构造（costBearer 缺省 = platform_funded 平台出资型，即改造前唯一行为）：既有测试零改动。 */
    public GiftVerifiedEvent(String entitlementId, String verificationId, String itemRef, String barId,
            String tableSessionId, String merchantId, Long settlementPriceCent) {
        this(entitlementId, verificationId, itemRef, barId, tableSessionId, merchantId, settlementPriceCent,
                TicketTypeEntitlementRecord.COST_BEARER_PLATFORM_FUNDED);
    }
}
