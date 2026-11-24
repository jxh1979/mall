package com.complex.platform.customer;

// 文件版本：V0.02

import java.time.Instant;

/**
 * TK-19 通用卡充值批次（ENUM「储值卡类型与收款主体」：「通用卡的每笔充值分别保留当前剩余本金、支付通路和支付成功时固化费率，
 * 同一通路不同费率的充值分批保存，不得用新费率覆盖旧批次」）。id = 充值流水 id；feeBps = 该笔充值支付成功时固化的通路费率（万分比）。
 * 第十八批 CB 段第 396 条 396-R1（BSM:521「每一笔单据在生成时就把所用配置版本固化在单上」；第十八批裁决 72「每单记录所用版本号」）：
 * feeConfigId、feeConfigVersion＝所用通路费配置行与版本号，未配置通路为空。
 */
public record StoredValueTopupBatchRecord(
        String id,
        String accountId,
        String customerId,
        String projectId,
        String paymentChannel,
        int feeBps,
        String feeConfigId,
        Integer feeConfigVersion,
        long principalCent,
        long remainingPrincipalCent,
        Instant paidAt
) {
}
