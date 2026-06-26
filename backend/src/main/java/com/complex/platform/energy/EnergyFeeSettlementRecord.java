package com.complex.platform.energy;

import java.time.Instant;

/** 能源费结算后写入 ar_ap_items 的应收摘要。 */
public record EnergyFeeSettlementRecord(
        String id,
        String contractId,
        String merchantId,
        String projectId,
        String period,
        String energyFeeBasis,
        long amountCent,
        String arApItemId,
        String sourceType,
        String sourceId,
        String direction,
        String status,
        Instant createdAt) {
}
