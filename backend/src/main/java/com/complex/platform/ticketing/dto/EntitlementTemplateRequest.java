package com.complex.platform.ticketing.dto;

// 文件版本：V0.01

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
            String barItemRef, String merchantId, String venueId, String pointId,
            Long agreementPriceCent, String exclusiveMerchantId) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, name, entitlementType, defaultTotalTimes,
                defaultValueCent, status, barItemRef, merchantId, venueId, pointId,
                agreementPriceCent, exclusiveMerchantId, null);
    }

    // ponytail: 餐饮结算前不带内部结算价/专属商户的调用点默认 null(=非餐饮,走现状)。
    public EntitlementTemplateRequest(
            String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String name,
            String entitlementType, Integer defaultTotalTimes, Long defaultValueCent, String status,
            String barItemRef, String merchantId, String venueId, String pointId) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, name, entitlementType, defaultTotalTimes,
                defaultValueCent, status, barItemRef, merchantId, venueId, pointId, null, null);
    }
}
