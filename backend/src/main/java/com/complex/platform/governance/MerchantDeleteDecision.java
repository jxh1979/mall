package com.complex.platform.governance;
// 文件版本：V0.01

/**
 * 商户删除审批结果。approved 时商户已按软删除标记（delete_status=approved，行保留、默认列表不显示、可查可恢复，BSM-004），deleted=true；
 * rejected 时商户回到 none 态、未删除。
 */
public record MerchantDeleteDecision(String merchantId, String decision, boolean deleted) {
}
