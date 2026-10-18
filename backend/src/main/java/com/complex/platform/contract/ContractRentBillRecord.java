package com.complex.platform.contract;

// 文件版本：V0.01

import java.time.Instant;

/** 合同租金账单（生效合同按 ContractRentMath 逐期挂账 → 联动 ar_ap_items 应收）。 */
public record ContractRentBillRecord(
        String id,
        String contractId,
        String merchantId,
        String projectId,
        String period,          // YYYY-MM
        String feeType,         // rent / property_fee / energy_fee
        long amountCent,
        String arApItemId,
        Instant createdAt,
        String turnoverRuleId  // 固定租金及非租金费目为空；抽成租金锁定本次采用的规则版本。
) {
}
