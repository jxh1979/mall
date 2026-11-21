package com.complex.platform.bar;

import java.time.Instant;
import java.util.List;

/** 酒吧套餐（V257）：后台从 SKU 组合而成，有独立售价/内部结算价/图片。items 为组合明细。 */
public record BarComboRecord(
        String id,
        String barId,
        String merchantId,
        String comboName,
        long priceCent,
        Long internalSettlementPriceCent,
        String imageUrl,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<BarComboItemRecord> items,
        // V262 数据存档与审批后删除范式（照 V255 通票权益模板先例）。存档=软标记可恢复；删除=硬删走审批。
        Instant archivedAt,
        String archivedBy,
        String deleteStatus,
        String deleteRequestedBy,
        Instant deleteRequestedAt,
        String deleteReason,
        // 第十八批 BY 段第 387 条 387-R1（第十八批裁决 81「每个分类都显示存档理由」）：含已存档列表带出存档理由（V271 archive_reason 列）。
        String archiveReason
) implements BarCatalogLifecycle<BarComboRecord> {
    /** 387-R1：存档理由之前的签名，既有构造点零改动，archiveReason 取空。 */
    public BarComboRecord(String id, String barId, String merchantId, String comboName, long priceCent, Long internalSettlementPriceCent, String imageUrl, String status, Instant createdAt, Instant updatedAt, List<BarComboItemRecord> items, Instant archivedAt, String archivedBy, String deleteStatus, String deleteRequestedBy, Instant deleteRequestedAt, String deleteReason) {
        this(id, barId, merchantId, comboName, priceCent, internalSettlementPriceCent, imageUrl, status, createdAt, updatedAt, items, archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, null);
    }

    // ponytail: V262 前的构造点（既有代码/测试）不带存档/删除字段，默认未存档 + delete_status=none。
    public BarComboRecord(
            String id, String barId, String merchantId, String comboName, long priceCent,
            Long internalSettlementPriceCent, String imageUrl, String status, Instant createdAt, Instant updatedAt,
            List<BarComboItemRecord> items) {
        this(id, barId, merchantId, comboName, priceCent, internalSettlementPriceCent, imageUrl, status,
                createdAt, updatedAt, items, null, null, "none", null, null, null);
    }

    /** 内部结算价按权限脱敏:无权限读端返回副本内部价置 null；387-R1：存档与删除字段、存档理由原样带上。 */
    public BarComboRecord withoutInternalPrice() {
        return new BarComboRecord(id, barId, merchantId, comboName, priceCent, null, imageUrl, status,
                createdAt, updatedAt, items,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, archiveReason);
    }

    public BarComboRecord withArchive(Instant archivedAt, String archivedBy, Instant updatedAt) {
        return new BarComboRecord(id, barId, merchantId, comboName, priceCent, internalSettlementPriceCent, imageUrl,
                status, createdAt, updatedAt, items,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, archivedAt == null ? null : archiveReason);
    }

    public BarComboRecord withDelete(String deleteStatus, String deleteRequestedBy,
            Instant deleteRequestedAt, String deleteReason, Instant updatedAt) {
        return new BarComboRecord(id, barId, merchantId, comboName, priceCent, internalSettlementPriceCent, imageUrl,
                status, createdAt, updatedAt, items,
                archivedAt, archivedBy, deleteStatus, deleteRequestedBy, deleteRequestedAt, deleteReason, archiveReason);
    }
}
