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
        String deleteReason
) implements BarCatalogLifecycle<BarProductCategoryRecord> {
    // ponytail: V262 前的构造点（既有代码/测试）不带存档/删除字段，默认未存档 + delete_status=none。
    public BarProductCategoryRecord(
            String id, String barId, String merchantId, String categoryName, int sortOrder, String status,
            Instant createdAt, Instant updatedAt) {
        this(id, barId, merchantId, categoryName, sortOrder, status, createdAt, updatedAt,
                null, null, "none", null, null, null);
    }

    public BarProductCategoryRecord withArchive(Instant archivedAt, String archivedBy, Instant updatedAt) {
        return new BarProductCategoryRecord(id, barId, merchantId, categoryName, sortOrder, status, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
    }

    public BarProductCategoryRecord withDelete(String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, Instant updatedAt) {
        return new BarProductCategoryRecord(id, barId, merchantId, categoryName, sortOrder, status, createdAt, updatedAt,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason);
    }
}
