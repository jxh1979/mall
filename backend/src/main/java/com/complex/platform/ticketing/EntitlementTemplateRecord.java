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
        boolean requireFacePhoto,
        // 数据存档与审批后删除范式(V253,试点)。存档=软标记可恢复;删除=硬删走审批。
        // archivedAt 非空=已存档;deleteStatus none|pending|approved。详见 docs/data-archive-delete-pattern-design-v0.1.md。
        java.time.Instant archivedAt,
        String archivedBy,
        String deleteStatus,
        String deleteRequestedBy,
        java.time.Instant deleteRequestedAt,
        String deleteReason,
        // 批10/E(V380):内部结算 SKU 目录引用(internal_price_catalog.item_code)。agreement_price_cent 自此为
        // 目录价快照(只读,系统在设引用/目录转正时同步);改价一律回目录走草案复核,模板侧无任何改价入口。
        String catalogItemCode
) {

    // ponytail: V253 前的构造点(既有代码/测试)不带存档/删除字段,默认未存档 + delete_status=none。
    //           避免为 6 个新字段改所有既有 17 参构造点;日后语义要变则删本构造让编译逼出所有点。
    public EntitlementTemplateRecord(
            String id, String name, String entitlementType, int defaultTotalTimes, long defaultValueCent, String status,
            String merchantId, String venueId, String pointId, String barItemRef, Instant createdAt, Instant updatedAt,
            String entitlementKind, Long agreementPriceCent, String consumptionWindow, String exclusiveMerchantId,
            boolean requireFacePhoto) {
        this(id, name, entitlementType, defaultTotalTimes, defaultValueCent, status, merchantId, venueId, pointId,
                barItemRef, createdAt, updatedAt, entitlementKind, agreementPriceCent, consumptionWindow,
                exclusiveMerchantId, requireFacePhoto, null, null, "none", null, null, null, null);
    }

    // --- V253 存档/删除范式：不可变 record 的字段更新辅助（避免 24 参构造散落业务代码）。 ---
    public EntitlementTemplateRecord withArchive(java.time.Instant archivedAt, String archivedBy, Instant updatedAt) {
        return new EntitlementTemplateRecord(id, name, entitlementType, defaultTotalTimes, defaultValueCent, status,
                merchantId, venueId, pointId, barItemRef, createdAt, updatedAt, entitlementKind, agreementPriceCent,
                consumptionWindow, exclusiveMerchantId, requireFacePhoto,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                catalogItemCode);
    }

    public EntitlementTemplateRecord withDelete(String deleteStatus, String deleteRequestedBy,
            java.time.Instant deleteRequestedAt, String deleteReason, Instant updatedAt) {
        return new EntitlementTemplateRecord(id, name, entitlementType, defaultTotalTimes, defaultValueCent, status,
                merchantId, venueId, pointId, barItemRef, createdAt, updatedAt, entitlementKind, agreementPriceCent,
                consumptionWindow, exclusiveMerchantId, requireFacePhoto,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                catalogItemCode);
    }

    /**
     * 批10/E:挂目录引用 + 同步签约价快照(系统写,创建/设引用时用)。
     * 改价没有对应的 with:唯一可编辑处在目录(草案→他人复核转正→快照传播),模板侧改价入口已随 PR#274 端点下线。
     */
    public EntitlementTemplateRecord withCatalogRef(String catalogItemCode, Long snapshotPriceCent, Instant updatedAt) {
        return new EntitlementTemplateRecord(id, name, entitlementType, defaultTotalTimes, defaultValueCent, status,
                merchantId, venueId, pointId, barItemRef, createdAt, updatedAt, entitlementKind, snapshotPriceCent,
                consumptionWindow, exclusiveMerchantId, requireFacePhoto,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason,
                catalogItemCode);
    }
}
