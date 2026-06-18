package com.complex.platform.bar;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * 酒吧账单结算流水（A2/A3, REQ-21）。记录结算渠道（统一词表，与 finance 对账归并）、现金收款人、POS 流水号。
 */
public record BarBillSettlementRecord(
        String id,
        String billId,
        String settleChannel,
        String cashierStaffId,
        String posTransactionNo,
        long settlementAmountCent,
        @JsonIgnore String merchantId,
        Instant settledAt
) {
}
