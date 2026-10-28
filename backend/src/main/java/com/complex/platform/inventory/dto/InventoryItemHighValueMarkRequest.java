package com.complex.platform.inventory.dto;

// 文件版本：V0.01

/**
 * 第十八批第 24 条 24-R5 物料「手工标记高价值」与「取消标记」（ENUM:164「或由库房主管以上岗位手工标记为高价值，
 * 标记与取消标记都必须填写理由并留痕，一线库管无此权限」）。manualHighValue＝标记后的值（true 标记、false 取消），reason 必填。
 */
public record InventoryItemHighValueMarkRequest(
        String idempotencyKey,
        Boolean manualHighValue,
        String reason
) {
}
