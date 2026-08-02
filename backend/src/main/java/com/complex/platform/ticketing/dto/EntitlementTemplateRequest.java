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
        // ⚠️ 批10/E(V380)起本字段已封写:内部结算价唯一可编辑处 = 内部结算 SKU 目录(草案→他人复核转正)。
        //   请求携带非空值一律 400(防"以为改好了"的无声空转);模板上的 agreement_price_cent 只是目录价快照。
        Long agreementPriceCent,
        // 餐饮结算(用户2026-07-06 拍板)：专属商户「不分摊」——非空=该权益核销收入全额归该商户不进池。
        //   餐饮权益填餐厅商户,使核销结算全额按内部结算价直付餐厅(§6.4 exclusive_merchant_id 机制)。
        String exclusiveMerchantId,
        // 批10/E(V380)：内部结算 SKU 目录引用(internal_price_catalog.item_code,可空)。
        //   餐饮/零售等虚拟权益的内部结算价由此取:引用既有条目或先在目录「就地新建」再引用;
        //   条目绑定商户必须 = 本模板提供方商户。
        String catalogItemCode
) {
    // ponytail: 批10/E 前带 exclusiveMerchantId 但不带目录引用的调用点(既有测试/前端)默认 null=未挂目录。
    public EntitlementTemplateRequest(
            String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String name,
            String entitlementType, Integer defaultTotalTimes, Long defaultValueCent, String status,
            String barItemRef, String merchantId, String venueId, String pointId, Boolean requireFacePhoto,
            Long agreementPriceCent, String exclusiveMerchantId) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, name, entitlementType, defaultTotalTimes,
                defaultValueCent, status, barItemRef, merchantId, venueId, pointId, requireFacePhoto,
                agreementPriceCent, exclusiveMerchantId, null);
    }
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
