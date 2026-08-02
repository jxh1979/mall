package com.complex.platform.ticketing;

import java.time.Instant;

public record TicketTypeEntitlementRecord(
        String id,
        String ticketTypeId,
        String entitlementTemplateId,
        int quantity,
        int totalTimes,
        Long valueAmountCent,
        String effectiveRule,
        // R5 已落地#1：该票种该权益每份核销人数（binding 级）。联合票/多人票配 >1；默认 1。
        // 发放时写入 issued_entitlements.num_people，核销 verified_people 随之生效（接 R5 T5）。
        int numPeople,
        // 待确认项#1：该票种该权益（binding 级）分项标准单价/手动单价（可空）。
        // 联合票（酒吧分项 + 游乐分项）按各分项单价比例拆分订单实付到每张权益；
        // 全空时回退按数量均摊或全归票务（见 TicketOrderService.issueEntitlements 注释）。
        Long componentPriceCent,
        // S6(V372)免费/赠送权益的成本承担方(HTML:737「每个权益必须在票种配置里标明成本承担方」)：
        // platform_funded=平台出资型(进 G 预扣·核销按内部结算价与提供商户结算)；
        // merchant_concession=商户让利型(平台不承担成本·不与商户结算·不进 G)。仅对票面原价为 0 的权益有业务含义。
        String costBearer,
        Instant createdAt,
        Instant updatedAt
) {

    /** S6 成本承担方默认值 = 平台出资型（改造前唯一行为，存量绑定零行为变更）。 */
    public static final String COST_BEARER_PLATFORM_FUNDED = "platform_funded";
    /** S6 商户让利型：平台不承担成本、不与商户结算、不进 G 预扣。 */
    public static final String COST_BEARER_MERCHANT_CONCESSION = "merchant_concession";

    /** 兼容构造（costBearer 缺省 = platform_funded）：既有测试/调用零改动。 */
    public TicketTypeEntitlementRecord(String id, String ticketTypeId, String entitlementTemplateId,
            int quantity, int totalTimes, Long valueAmountCent, String effectiveRule, int numPeople,
            Long componentPriceCent, Instant createdAt, Instant updatedAt) {
        this(id, ticketTypeId, entitlementTemplateId, quantity, totalTimes, valueAmountCent, effectiveRule,
                numPeople, componentPriceCent, COST_BEARER_PLATFORM_FUNDED, createdAt, updatedAt);
    }
}
