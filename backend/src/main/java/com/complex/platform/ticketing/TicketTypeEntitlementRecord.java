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
        // R5 TODO#1：该票种该权益每份核销人数（binding 级）。联合票/多人票配 >1；默认 1。
        // 发放时写入 issued_entitlements.num_people，核销 verified_people 随之生效（接 R5 T5）。
        int numPeople,
        // 待确认项#1：该票种该权益（binding 级）分项标准单价/手动单价（可空）。
        // 联合票（酒吧分项 + 游乐分项）按各分项单价比例拆分订单实付到每张权益；
        // 全空时回退按数量均摊或全归票务（见 TicketOrderService.issueEntitlements 注释）。
        Long componentPriceCent,
        Instant createdAt,
        Instant updatedAt
) {
}
