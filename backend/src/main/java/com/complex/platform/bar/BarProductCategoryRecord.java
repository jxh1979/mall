package com.complex.platform.bar;

import java.time.Instant;

/** 酒吧商品分类（V256 图形化点单 POS 阶段1）：可配置分类，POS 按此分屏列菜。 */
public record BarProductCategoryRecord(
        String id,
        String barId,
        String merchantId,
        String categoryName,
        int sortOrder,
        String status,
        Instant createdAt,
        Instant updatedAt,
        // V262 数据存档与审批后删除范式（照 V255 通票权益模板先例）。存档=软标记可恢复；删除=硬删走审批。
        Instant archivedAt,
        String archivedBy,
        String deleteStatus,
        String deleteRequestedBy,
        Instant deleteRequestedAt,
        String deleteReason,
        // 第十八批 BY 段第 387 条 387-R1（第十八批裁决 81「每个分类都显示存档理由」）：含已存档列表带出存档理由（V271 archive_reason 列）。
        String archiveReason
) implements BarCatalogLifecycle<BarProductCategoryRecord> {
    /** 387-R1：存档理由之前的签名，既有构造点零改动，archiveReason 取空。 */
    public BarProductCategoryRecord(String id, String barId, String merchantId, String categoryName, int sortOrder, String status, Instant createdAt, Instant updatedAt, Instant archivedAt, String archivedBy, String deleteStatus, String deleteRequestedBy, Instant deleteRequestedAt, String deleteReason) {
        this(id, barId, merchantId, categoryName, sortOrder, status, createdAt, updatedAt, archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, null);
    }

    // ponytail: V262 前的构造点（既有代码/测试）不带存档/删除字段，默认未存档 + delete_status=none。
    public BarProductCategoryRecord(
            String id, String barId, String merchantId, String categoryName, int sortOrder, String status,
            Instant createdAt, Instant updatedAt) {
        this(id, barId, merchantId, categoryName, sortOrder, status, createdAt, updatedAt,
                null, null, "none", null, null, null);
    }

    public BarProductCategoryRecord withArchive(Instant archivedAt, String archivedBy, Instant updatedAt) {
        return new BarProductCategoryRecord(id, barId, merchantId, categoryName, sortOrder, status, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, archivedAt == null ? null : archiveReason);
    }

    public BarProductCategoryRecord withDelete(String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, Instant updatedAt) {
        return new BarProductCategoryRecord(id, barId, merchantId, categoryName, sortOrder, status, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, archiveReason);
    }
}
