package com.complex.platform.bar;

// 文件版本：V0.02

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
        boolean pendingClearOverdue,
        // ROOT-03-R19（ENUM 第 100 行「排他锁，不是展示标记」）：只在列表读取面带出——正占着该台位的订台号（已预留），员工到店开台须带它。
        String heldReservationId
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
            Instant updatedAt,
            Instant pendingClearSince,
            boolean pendingClearOverdue
    ) {
        this(id, barId, merchantId, tableCode, tableName, tableType, capacity, allowedPassFreeGuestCount,
                minConsumptionCent, currency, status, createdAt, updatedAt, pendingClearSince, pendingClearOverdue, null);
    }

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
