package com.complex.platform.ticketing.dto;

import java.util.List;

public record EntitlementTemplateRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String name,
        String entitlementType,
        Integer defaultTotalTimes,
        Long defaultValueCent,
        String status,
        // #3 运营配置入口：gift_drink 模板指向的吧台商品/库存 item（可选）。赠饮核销取此作 itemRef 扣减库存。
        String barItemRef,
        // 权益归属配置入口（各可选）：联合通票各模板可填各自商户/场地/点位；为空发放回退出票方商户。
        String merchantId,
        String venueId,
        String pointId,
        // 结算重构批3-6(用户2026-07-04)：模板级开关——购买/发放时是否要求拍脸照存档(可空=false，游乐默认开走 V229 种子回填)。
        Boolean requireFacePhoto,
        // 餐饮结算(用户2026-07-06 拍板)：内部结算价(分,可空)。餐饮权益按此价核销归餐厅商户，与「实物内部结算价」同义。
        //   复用 agreement_price_cent 承载(virtual 权益核销取签约单价=内部结算价);为空回退 value_amount。
        Long agreementPriceCent,
        // 餐饮结算(用户2026-07-06 拍板)：专属商户「不分摊」——非空=该权益核销收入全额归该商户不进池。
        //   餐饮权益填餐厅商户,使核销结算全额按内部结算价直付餐厅(§6.4 exclusive_merchant_id 机制)。
        String exclusiveMerchantId
) {
    // ponytail: 向后兼容构造——批3-6 前的调用点(既有测试/前端)不带 requireFacePhoto，默认 null(=false 不要脸照)。
    public EntitlementTemplateRequest(
            String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String name,
            String entitlementType, Integer defaultTotalTimes, Long defaultValueCent, String status,
            String barItemRef, String merchantId, String venueId, String pointId) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, name, entitlementType, defaultTotalTimes,
                defaultValueCent, status, barItemRef, merchantId, venueId, pointId, null, null, null);
    }

    // ponytail: 餐饮结算前带 requireFacePhoto 但不带内部结算价/专属商户的调用点默认 null(=非餐饮,走现状)。
    public EntitlementTemplateRequest(
            String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String name,
            String entitlementType, Integer defaultTotalTimes, Long defaultValueCent, String status,
            String barItemRef, String merchantId, String venueId, String pointId, Boolean requireFacePhoto) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, name, entitlementType, defaultTotalTimes,
                defaultValueCent, status, barItemRef, merchantId, venueId, pointId, requireFacePhoto, null, null);
    }
}
