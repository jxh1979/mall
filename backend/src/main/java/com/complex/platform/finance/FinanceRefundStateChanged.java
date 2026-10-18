package com.complex.platform.finance;

// 文件版本：V0.01

/** 统一退款真实状态保存后的同步事务事件。监听失败必须使整笔结果回滚。 */
public record FinanceRefundStateChanged(FinanceRefundRecord refund) {
}
