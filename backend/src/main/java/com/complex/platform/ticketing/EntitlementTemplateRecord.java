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
        Long agreementPriceCent
) {
}
