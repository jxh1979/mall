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
        int slotIndex,
        // AUD-076 单店票挂酒吧：出票时从订单快照的酒吧商户 id；null=通票/平台票。撤场提前到期按此列检索。
        String barId,
        // D6 套票折扣落库：出票定价时记录该权益「满额份额」(各分项标准价按人拆的份额,分)。可空:
        //   component_price 全空(回退均摊)→ null=无分项满额价=无套票折扣。核销结算时与 allocated 走同一增量公式算
        //   verifiedFull,折扣增量=max(0, verifiedFull−verified) 落 settlement_transactions.discount_amount_cent。
        Long componentFullPriceCent,
        // CG-08b（ENUM「全园故障与补偿通票」）：非空＝本行是全园故障补偿补发的权益（原票下、完整票面），指向补偿记录；
        // 核销时不重复产生积分与成长值、履约锁定到该补偿记录、不计入原订单积分份额。原票行与普通发放为 null。
        String compensationItemId
) {

    /** 27 参兼容构造（无补偿标记）：既有发放/测试构造点不变，compensationItemId 置 null。 */
    public IssuedEntitlementRecord(
            String id, String entitlementCode, String ticketTypeId, String orderId, String entitlementTemplateId,
            String entitlementType, String merchantId, String venueId, String pointId, int totalTimes, int usedTimes,
            Long valueAmountCent, String status, Instant validFrom, Instant validTo, Instant createdAt, Instant updatedAt,
            int numPeople, Long allocatedRevenueCent, String entitlementKind, Long agreementPriceCent,
            String consumptionWindow, String exclusiveMerchantId, String parentTicketId, int slotIndex, String barId,
            Long componentFullPriceCent) {
        this(id, entitlementCode, ticketTypeId, orderId, entitlementTemplateId, entitlementType, merchantId, venueId,
                pointId, totalTimes, usedTimes, valueAmountCent, status, validFrom, validTo, createdAt, updatedAt,
                numPeople, allocatedRevenueCent, entitlementKind, agreementPriceCent, consumptionWindow,
                exclusiveMerchantId, parentTicketId, slotIndex, barId, componentFullPriceCent, null);
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
                slotIndex,
                barId,
                componentFullPriceCent,
                compensationItemId
        );
    }

    /** AUD-076 撤场提前到期：复制并把 valid_to 提前（保留 bar/parent/slot 快照）。 */
    public IssuedEntitlementRecord withValidTo(Instant validTo, Instant updatedAt) {
        return new IssuedEntitlementRecord(id, entitlementCode, ticketTypeId, orderId, entitlementTemplateId,
                entitlementType, merchantId, venueId, pointId, totalTimes, usedTimes, valueAmountCent, status,
                validFrom, validTo, createdAt, updatedAt, numPeople, allocatedRevenueCent, entitlementKind,
                agreementPriceCent, consumptionWindow, exclusiveMerchantId, parentTicketId, slotIndex, barId,
                componentFullPriceCent, compensationItemId);
    }
}
