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
        // R5 T5：发放时记录的核销人数（多人/联合票>1，普通票=1）。来源为票种权益绑定配置；未配置时默认 1。
        int numPeople,
        // 待确认项#1：出票时算得的该权益分配收入（订单实付按各权益分项单价比例拆分；可空，无订单/单价时为 null）。
        // 按 entitlement_type 映射业态（bar/amusement/ticket）+ merchant_id 做业态/商户聚合的基础；
        // 仅作售票收入的业态拆分展示与法律双实体对账，不并入净收入（订单实付全额已计入 official_ticket/revenue.ticket）。
        Long allocatedRevenueCent,
        // 结算重构批3-1(PRD §6.1)：发放时从模板复制的结算类别 physical/virtual（可空回退 value_amount）。
        String entitlementKind,
        // 虚拟权益签约单价(分)，发放时从模板复制；仅 virtual 结算时取用，可空回退 value_amount。
        Long agreementPriceCent,
        // 结算重构批3-2(PRD §6.2)：消耗单位 per_use|per_session|per_day|unlimited_in_day，发放时从模板复制。
        // 仅 per_session 走场次逻辑(入过场当场次持续进出不重复消耗、过场作废)；NULL/其余=现状逐次扣(per_use)。
        String consumptionWindow
) {
    // ponytail: 向后兼容构造——批3-2 前的调用点(测试)不带 consumptionWindow，默认 null(=判定层走现状 per_use 逐次扣)。
    //           避免为 1 个可空字段改 18 处既有构造点；未改的既有用例即"consumption_window 缺省"的回归证。
    //           日后若默认语义要变（如强制显式），删本构造让编译逼出所有点。
    public IssuedEntitlementRecord(
            String id, String entitlementCode, String ticketTypeId, String orderId, String entitlementTemplateId,
            String entitlementType, String merchantId, String venueId, String pointId, int totalTimes, int usedTimes,
            Long valueAmountCent, String status, Instant validFrom, Instant validTo, Instant createdAt, Instant updatedAt,
            int numPeople, Long allocatedRevenueCent, String entitlementKind, Long agreementPriceCent) {
        this(id, entitlementCode, ticketTypeId, orderId, entitlementTemplateId, entitlementType, merchantId, venueId,
                pointId, totalTimes, usedTimes, valueAmountCent, status, validFrom, validTo, createdAt, updatedAt,
                numPeople, allocatedRevenueCent, entitlementKind, agreementPriceCent, null);
    }

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
                allocatedRevenueCent,
                entitlementKind,
                agreementPriceCent,
                consumptionWindow
        );
    }
}
