package com.complex.platform.customer;

// 文件版本：V0.01

import java.time.Instant;

/**
 * TK-19 消费本金按充值批次分摊的原消费记录（ENUM「储值卡类型与收款主体」：「每次消费需扣本金时，按消费前各批次剩余本金占总剩余本金的
 * 比例分摊本次本金扣减，并逐批按其原固化费率计算对应通路费」）。restoredPrincipalCent = 退款累计恢复到该批次的本金，不超 principalCent。
 */
public record StoredValueConsumeAllocationRecord(
        String id,
        String consumeTxnId,
        String batchId,
        long principalCent,
        long restoredPrincipalCent,
        int feeBps,
        long feeCent,
        Instant createdAt
) {
}
