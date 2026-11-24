package com.complex.platform.inventory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * V108 库存差异明细（库存域写入侧）。与 finance 包内同名只读投影对应同一表 inventory_variance，
 * 本类承载库存域建单/录差异/复核写路径；finance 侧只读差异并入账（review_status='approved' 后）。
 *
 * <p>varianceQty/varianceAmountCent 带符号：负=盘亏/报损，正=盘盈。reason：damage/shortage/overage/supplier_fault。
 * reviewStatus：pending|approved|rejected。reviewedBy/reviewedAt 复核留痕。postedArApItemId 非空表示已入账。
 * merchantId 经 stocktake 归属（隔离用），随明细冗余便于范围查询与审计。
 * recordedBy 录差异的账号（第十八批第 24 条：复核人不得是录入人，PGM:529）。
 * photoAttachmentIds/videoAttachmentIds 差异照片、视频证据（第十八批第 24 条 24-R4：高价值差异必填）。
 * hasVarianceAmount 差异金额不为 0 即 true（不含价格）。第十八批 BK 段复核修复（PGM:660「系统自动使用某个价格完成计算，
 * 不代表当前操作人取得查看或修改权限。」）：差异金额可由系统按当前平均进货价估算，按差异数量一除即成本价——不持 inventory.cost.view
 * 的读者由控制器用 {@link #withoutCosts()} 隐去金额，页面按 hasVarianceAmount 判能否过账。
 */
public record InventoryVarianceRecord(
        String id,
        String stocktakeId,
        String itemId,
        String batchNo,
        BigDecimal bookQty,
        BigDecimal countedQty,
        BigDecimal varianceQty,
        Long varianceAmountCent,
        String reason,
        String supplierId,
        String reviewStatus,
        String reviewedBy,
        Instant reviewedAt,
        String postedArApItemId,
        @JsonIgnore String merchantId,
        Instant createdAt,
        Instant updatedAt,
        String recordedBy,
        List<String> photoAttachmentIds,
        List<String> videoAttachmentIds,
        boolean hasVarianceAmount
) {
    public InventoryVarianceRecord(String id, String stocktakeId, String itemId, String batchNo, BigDecimal bookQty,
            BigDecimal countedQty, BigDecimal varianceQty, Long varianceAmountCent, String reason, String supplierId,
            String reviewStatus, String reviewedBy, Instant reviewedAt, String postedArApItemId, String merchantId,
            Instant createdAt, Instant updatedAt, String recordedBy, List<String> photoAttachmentIds, List<String> videoAttachmentIds) {
        this(id, stocktakeId, itemId, batchNo, bookQty, countedQty, varianceQty, varianceAmountCent, reason, supplierId,
                reviewStatus, reviewedBy, reviewedAt, postedArApItemId, merchantId, createdAt, updatedAt, recordedBy,
                photoAttachmentIds, videoAttachmentIds, varianceAmountCent != null && varianceAmountCent != 0);
    }

    public InventoryVarianceRecord(String id, String stocktakeId, String itemId, String batchNo, BigDecimal bookQty,
            BigDecimal countedQty, BigDecimal varianceQty, long varianceAmountCent, String reason, String supplierId,
            String reviewStatus, String reviewedBy, Instant reviewedAt, String postedArApItemId, String merchantId,
            Instant createdAt, Instant updatedAt) {
        this(id, stocktakeId, itemId, batchNo, bookQty, countedQty, varianceQty, varianceAmountCent, reason, supplierId,
                reviewStatus, reviewedBy, reviewedAt, postedArApItemId, merchantId, createdAt, updatedAt, null, List.of(), List.of());
    }

    /** 差异金额脱敏副本（同 {@link StockMovementRecord#withoutCosts()}）：金额置空，hasVarianceAmount 保留。 */
    public InventoryVarianceRecord withoutCosts() {
        if (varianceAmountCent == null) {
            return this;
        }
        return new InventoryVarianceRecord(id, stocktakeId, itemId, batchNo, bookQty, countedQty, varianceQty, null, reason,
                supplierId, reviewStatus, reviewedBy, reviewedAt, postedArApItemId, merchantId, createdAt, updatedAt, recordedBy,
                photoAttachmentIds, videoAttachmentIds, hasVarianceAmount);
    }
}
