package com.complex.platform.ticketing;

import java.time.Instant;

public record IssuedEntitlementRecord(
        String id,
        String entitlementCode,
        String ticketTypeId,
        String orderId,
        String entitlementTemplateId,
        String entitlementType,
        String merchantId,
        String venueId,
        String pointId,
        int totalTimes,
        int usedTimes,
        Long valueAmountCent,
        String status,
        Instant validFrom,
        Instant validTo,
        Instant createdAt,
        Instant updatedAt,
        // R5 T5：发放时记录的核销人数（多人/联合票>1，普通票=1）。来源待票种配置（见 TicketOrderService TODO），当前默认 1。
        int numPeople,
        // 待确认项#1：出票时算得的该权益分配收入（订单实付按各权益分项单价比例拆分；可空，无订单/单价时为 null）。
        // 按 entitlement_type 映射业态（bar/amusement/ticket）+ merchant_id 做业态/商户聚合的基础；
        // 仅作售票收入的业态拆分展示与法律双实体对账，不并入净收入（订单实付全额已计入 official_ticket/revenue.ticket）。
        Long allocatedRevenueCent
) {
    public int remainingTimes() {
        return totalTimes - usedTimes;
    }

    public IssuedEntitlementRecord useOnce(Instant updatedAt) {
        return new IssuedEntitlementRecord(
                id,
                entitlementCode,
                ticketTypeId,
                orderId,
                entitlementTemplateId,
                entitlementType,
                merchantId,
                venueId,
                pointId,
                totalTimes,
                usedTimes + 1,
                valueAmountCent,
                status,
                validFrom,
                validTo,
                createdAt,
                updatedAt,
                numPeople,
                allocatedRevenueCent
        );
    }
}
