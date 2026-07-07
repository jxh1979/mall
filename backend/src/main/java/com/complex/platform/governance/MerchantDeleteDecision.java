package com.complex.platform.governance;

/**
 * 商户删除审批结果。approved 时商户已物理删除（merchantId 仅供留痕），deleted=true；
 * rejected 时商户回到 none 态、未删除。
 */
public record MerchantDeleteDecision(String merchantId, String decision, boolean deleted) {
}
