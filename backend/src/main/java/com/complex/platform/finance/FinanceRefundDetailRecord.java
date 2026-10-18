package com.complex.platform.finance;

// 文件版本：V0.01

import java.util.List;

public record FinanceRefundDetailRecord(FinanceRefundRecord refund, List<FinanceRefundAttemptRecord> attempts) {
}
