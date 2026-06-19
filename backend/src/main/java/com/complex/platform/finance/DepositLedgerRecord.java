package com.complex.platform.finance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

/**
 * 保证金台账一行（P1#14）。action：collect|refund|deduct|forfeit；
 * amountCent 恒正；balanceAfterCent 为本动作后运行余额（>=0）。
 */
public record DepositLedgerRecord(
        String id,
        @JsonIgnore String merchantId,
        String action,
        long amountCent,
        long balanceAfterCent,
        String arApItemId,
        String reason,
        Instant createdAt
) {
}
