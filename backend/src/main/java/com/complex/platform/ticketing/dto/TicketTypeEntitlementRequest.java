package com.complex.platform.ticketing.dto;

import java.util.List;

public record TicketTypeEntitlementRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String entitlementTemplateId,
        Integer quantity,
        Integer totalTimes,
        Long valueAmountCent,
        String effectiveRule,
        // #3 运营配置入口：每份核销人数（binding 级，可选）。多人票/联合票配 >1；缺省回退 1（向后兼容）。
        Integer numPeople,
        // 待确认项#1 配置入口：该权益分项标准单价/手动单价（可选）。联合票各分项配单价以按比例拆分订单实付。
        Long componentPriceCent,
        // S6 配置入口：免费/赠送权益成本承担方（可选，缺省 platform_funded=平台出资型）。
        // platform_funded=平台出资型(进赠送权益预估成本预扣·核销按内部结算价与提供商户结算)；
        // merchant_concession=商户让利型(平台不承担成本·不与商户结算·不进预扣)。
        String costBearer
) {

    /** 兼容构造（costBearer 缺省 null → 服务层回退 platform_funded）：既有测试/调用零改动。 */
    public TicketTypeEntitlementRequest(String idempotencyKey, String operatorNote,
            List<String> evidenceAttachmentIds, String entitlementTemplateId, Integer quantity,
            Integer totalTimes, Long valueAmountCent, String effectiveRule, Integer numPeople,
            Long componentPriceCent) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, entitlementTemplateId, quantity,
                totalTimes, valueAmountCent, effectiveRule, numPeople, componentPriceCent, null);
    }
}
