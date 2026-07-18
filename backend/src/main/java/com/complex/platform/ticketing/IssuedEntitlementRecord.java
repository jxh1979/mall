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
        String consumptionWindow,
        // 结算重构批3-4(PRD §6.4)：专属某商户「不分摊」，发放时从模板复制；非空=该权益核销收入全额归该商户，
        // 不进池、不扣留存、不参与他家分池排名/客数聚合。核销双写时带入 settlement_transactions 定格结算源真相。
        String exclusiveMerchantId,
        // AUD-066：该权益所属父票 id（方案2 整票退款/方案3 转赠锚点）；存量/直发手造可空。
        String parentTicketId,
        // AUD-066 人员权益组槽位（用户口径无 slot 0）：1..N=第几个人；0=无父票/存量未分组。
        int slotIndex
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
                numPeople, allocatedRevenueCent, entitlementKind, agreementPriceCent, null, null, null, 0);
    }

    // ponytail: 批3-4 前带 consumptionWindow 但不带 exclusiveMerchantId 的调用点默认 null(=非专属分池)。
    public IssuedEntitlementRecord(
            String id, String entitlementCode, String ticketTypeId, String orderId, String entitlementTemplateId,
            String entitlementType, String merchantId, String venueId, String pointId, int totalTimes, int usedTimes,
            Long valueAmountCent, String status, Instant validFrom, Instant validTo, Instant createdAt, Instant updatedAt,
            int numPeople, Long allocatedRevenueCent, String entitlementKind, Long agreementPriceCent,
            String consumptionWindow) {
        this(id, entitlementCode, ticketTypeId, orderId, entitlementTemplateId, entitlementType, merchantId, venueId,
                pointId, totalTimes, usedTimes, valueAmountCent, status, validFrom, validTo, createdAt, updatedAt,
                numPeople, allocatedRevenueCent, entitlementKind, agreementPriceCent, consumptionWindow, null, null, 0);
    }

    // ponytail: AUD-066 前带 exclusiveMerchantId 但不带 parent/slot 的调用点(测试)默认 parent=null、slot=0（无父票不分组）。
    public IssuedEntitlementRecord(
            String id, String entitlementCode, String ticketTypeId, String orderId, String entitlementTemplateId,
            String entitlementType, String merchantId, String venueId, String pointId, int totalTimes, int usedTimes,
            Long valueAmountCent, String status, Instant validFrom, Instant validTo, Instant createdAt, Instant updatedAt,
            int numPeople, Long allocatedRevenueCent, String entitlementKind, Long agreementPriceCent,
            String consumptionWindow, String exclusiveMerchantId) {
        this(id, entitlementCode, ticketTypeId, orderId, entitlementTemplateId, entitlementType, merchantId, venueId,
                pointId, totalTimes, usedTimes, valueAmountCent, status, validFrom, validTo, createdAt, updatedAt,
                numPeople, allocatedRevenueCent, entitlementKind, agreementPriceCent, consumptionWindow,
                exclusiveMerchantId, null, 0);
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
                consumptionWindow,
                exclusiveMerchantId,
                parentTicketId,
                slotIndex
        );
    }
}
