package com.complex.platform.bar;

/**
 * 待回填空间的卡座/桌台清单项（space_id 为空）。
 *
 * <p>供运营核对哪些桌台尚未录入 space_id（over_space 对这些桌台关联账单不生效，保守放行）。</p>
 */
public record BarTableSpaceBacklogRecord(
        String id,
        String barId,
        String merchantId,
        String tableCode,
        String tableName,
        String status
) {
}
