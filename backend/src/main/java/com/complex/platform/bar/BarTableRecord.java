package com.complex.platform.bar;

// 文件版本：V0.01

import java.time.Instant;

public record BarTableRecord(
        String id,
        String barId,
        String merchantId,
        String tableCode,
        String tableName,
        String tableType,
        int capacity,
        int allowedPassFreeGuestCount,
        long minConsumptionCent,
        String currency,
        String status,
        Instant createdAt,
        Instant updatedAt,
        // 第十批 TK-27-R2（BSM L1058 待清台超时提醒）：只在列表读取面计算——进入待清台的时刻（status=pending_clear 时的
        // updated_at）与是否已超过酒吧（商户）配置的提醒时限 merchants.pending_clear_alert_minutes；写入面与单条读取恒为 null/false。
        Instant pendingClearSince,
        boolean pendingClearOverdue
) {
    public BarTableRecord(
            String id,
            String barId,
            String merchantId,
            String tableCode,
            String tableName,
            String tableType,
            int capacity,
            int allowedPassFreeGuestCount,
            long minConsumptionCent,
            String currency,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(id, barId, merchantId, tableCode, tableName, tableType, capacity, allowedPassFreeGuestCount,
                minConsumptionCent, currency, status, createdAt, updatedAt, null, false);
    }
}
