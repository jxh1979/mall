package com.complex.platform.access.org;

// 文件版本：V0.01

import java.util.List;

/**
 * 第十八批 AM 段第 241 条 241-R8（BSM:1803「具体流程按支付类型分别设定」；第十八批裁决 67「审批线页设定」）：某支付类型在财务、法务两线之外还必带的
 * 平行线（在用线的 line_code；空表＝只带两线）。
 */
public record PaymentConfirmationFlowRequest(String idempotencyKey, List<String> lineCodes) {
}
