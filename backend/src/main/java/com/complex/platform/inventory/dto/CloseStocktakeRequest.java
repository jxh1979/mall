package com.complex.platform.inventory.dto;

// 文件版本：V0.01

/**
 * 第十八批 BK 段修复项 F01：结束盘点请求（盘点中 → 已关闭），幂等。只带请求编号；盘点单编号在路径上。
 */
public record CloseStocktakeRequest(
        String idempotencyKey
) {
}
