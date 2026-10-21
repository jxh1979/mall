package com.complex.platform.finance;

// 文件版本：V0.01

/**
 * 第十一批 TK-04-b：退款应付（direction=payable、source_type=refund）经统一回写点实付结清，即该笔退款已实际退出；
 * 同一事务内通知来源域按退款成功回写（BSM L823「退款成功时取消对应尚待确认积分」）。sourceId 为来源退款单据 id。
 */
public record RefundPayableSettled(String arApItemId, String sourceId) {
}
