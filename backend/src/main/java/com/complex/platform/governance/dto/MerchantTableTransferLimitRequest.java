package com.complex.platform.governance.dto;
// 文件版本：V0.01

/** 第十八批 AB 段第 190 条 190-R1 转台次数上限配置请求：值、下限、上限三者同填且 0≤下限≤值≤上限（次）。 */
public record MerchantTableTransferLimitRequest(String idempotencyKey, Integer valueCount, Integer minCount, Integer maxCount) {
}
