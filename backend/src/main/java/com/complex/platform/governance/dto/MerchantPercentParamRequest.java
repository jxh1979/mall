package com.complex.platform.governance.dto;
// 文件版本：V0.01

/** 第十八批 C 段酒吧百分比参数配置请求（员工代订爽约率上限、损耗率阈值）：值、下限、上限三者同填且值在闭区间（整数百分比）。 */
public record MerchantPercentParamRequest(String idempotencyKey, Integer valuePct, Integer minPct, Integer maxPct) {
}
