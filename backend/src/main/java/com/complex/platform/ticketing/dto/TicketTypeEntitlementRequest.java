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
        Long componentPriceCent
) {
}
