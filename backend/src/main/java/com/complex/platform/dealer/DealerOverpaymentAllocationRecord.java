package com.complex.platform.dealer;

// 文件版本：V0.01

import java.time.Instant;
import java.util.List;

/** 分配原超收款；抵款取消后仍占用原分配额，现金沿原收款退款，不回预付余额。 */
public record DealerOverpaymentAllocationRecord(String id, String overpaymentId, String usage,
        long amountCent, String targetOrderId, List<String> confirmationAttachmentIds,
        String confirmedBy, Instant confirmedAt, String note, String refundId) {
}
