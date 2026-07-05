package com.complex.platform.ticketing;

import java.time.Instant;

public record EntitlementTemplateRecord(
        String id,
        String name,
        String entitlementType,
        int defaultTotalTimes,
        long defaultValueCent,
        String status,
        // R5 审计点②：权益归属（各可空）。联合通票各模板可填各自商户/场地/点位，
        // 发放时优先采用；为空则回退出票方商户（向后兼容既有单模板票）。
        String merchantId,
        String venueId,
        String pointId,
        // R5 已落地#2：gift_drink 模板指向的吧台商品/库存 item（template 级，可空）。
        // 赠饮核销时取此作为 itemRef 发 ticket.gift_verified；为空则下游跳过库存扣减（接 R5 T15）。
        String barItemRef,
        Instant createdAt,
        Instant updatedAt,
        // 结算重构批3-1(PRD §6.1)：权益结算类别 physical(实物,走酒吧内部结算价)/virtual(虚拟,走签约单价)；
        // 可空=旧数据,判定回退 value_amount。发放时复制到 issued_entitlements。放末位便于既有构造点追加。
        String entitlementKind,
        // 虚拟权益签约单价(分)；仅 virtual 结算时取用，可空回退 value_amount。
        Long agreementPriceCent,
        // 结算重构批3-2(PRD §6.2)：消耗单位 per_use|per_session|per_day|unlimited_in_day；发放时复制到 issued_entitlements。
        // 仅 per_session 走场次逻辑；NULL/其余=现状逐次扣(per_use)。
        String consumptionWindow,
        // 结算重构批3-4(PRD §6.4)：专属某商户「不分摊」——非空=该权益核销收入全额归该商户，不进池不扣留存不稀释他家。
        // 发放时复制到 issued_entitlements。
        String exclusiveMerchantId,
        // 结算重构批3-6(用户2026-07-04)：购买/发放时是否要求拍脸照存档(全票种可配，游乐默认开=种子回填)。
        // 发放时复制到 issued_entitlements，为 true 时发放必须带人脸照 payload 否则拒 FACE_PHOTO_REQUIRED。放末位便于既有构造点追加。
        boolean requireFacePhoto
) {
    // ponytail: 向后兼容构造——批3-2 前的调用点(测试)不带 consumptionWindow，默认 null(=判定层走现状 per_use)。
    //           避免为 1 个可空字段改既有构造点；日后默认语义要变则删本构造让编译逼出所有点。
    public EntitlementTemplateRecord(
            String id, String name, String entitlementType, int defaultTotalTimes, long defaultValueCent, String status,
            String merchantId, String venueId, String pointId, String barItemRef, Instant createdAt, Instant updatedAt,
            String entitlementKind, Long agreementPriceCent) {
        this(id, name, entitlementType, defaultTotalTimes, defaultValueCent, status, merchantId, venueId, pointId,
                barItemRef, createdAt, updatedAt, entitlementKind, agreementPriceCent, null, null, false);
    }

    // ponytail: 批3-4 前带 consumptionWindow 但不带 exclusiveMerchantId 的调用点默认 null(=非专属分池)。
    public EntitlementTemplateRecord(
            String id, String name, String entitlementType, int defaultTotalTimes, long defaultValueCent, String status,
            String merchantId, String venueId, String pointId, String barItemRef, Instant createdAt, Instant updatedAt,
            String entitlementKind, Long agreementPriceCent, String consumptionWindow) {
        this(id, name, entitlementType, defaultTotalTimes, defaultValueCent, status, merchantId, venueId, pointId,
                barItemRef, createdAt, updatedAt, entitlementKind, agreementPriceCent, consumptionWindow, null, false);
    }

    // ponytail: 批3-6 前带 exclusiveMerchantId 但不带 requireFacePhoto 的调用点默认 false(=不要脸照)。
    public EntitlementTemplateRecord(
            String id, String name, String entitlementType, int defaultTotalTimes, long defaultValueCent, String status,
            String merchantId, String venueId, String pointId, String barItemRef, Instant createdAt, Instant updatedAt,
            String entitlementKind, Long agreementPriceCent, String consumptionWindow, String exclusiveMerchantId) {
        this(id, name, entitlementType, defaultTotalTimes, defaultValueCent, status, merchantId, venueId, pointId,
                barItemRef, createdAt, updatedAt, entitlementKind, agreementPriceCent, consumptionWindow,
                exclusiveMerchantId, false);
    }
}
