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
        String pointId
) {
}
