package com.complex.platform.bar;

// 文件版本：V0.01

/**
 * 第十八批 K 段 95-R2：店长改派开台客人来源与销售人员（BSM 第 1021 行「来源与销售人员的事后改派须店长复核并留痕，只能在该账单结清前办理」）。
 * guestSource 为 platform_own／marketing／walk_in；salesStaffId 只在营销带客时采用，其余来源销售置空。
 */
public record BarTableSessionAttributionReassignRequest(String idempotencyKey, String guestSource, String salesStaffId) {
}
