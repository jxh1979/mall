package com.complex.platform.inventory;

// 文件版本：V0.02

import java.time.Instant;

/**
 * 溯源码归属变更历史(append-only·防调包+结算对账)。V277。
 * FS-019（BSM L1201「记录三个时间：借出时间、每一次保管人变更的时间、实际归还时间……都由已有的扫码动作自动生成」）：
 * changeKind 为 ownership（所有权变更）或 custody（保管人变更，借出交货、借入收货、归还收货、实物处置完成各一行，changedAt＝扫码时刻）。
 */
public record InventoryTraceOwnershipHistoryRecord(
        String id,
        String traceCode,
        String fromOwnerType,
        String fromOwnerId,
        String toOwnerType,
        String toOwnerId,
        String transferOrderId,
        Instant changedAt,
        String changeKind
) {
    public static final String OWNERSHIP = "ownership";
    public static final String CUSTODY = "custody";

    /** 所有权变更（买断、供货签收）。 */
    public InventoryTraceOwnershipHistoryRecord(String id, String traceCode, String fromOwnerType, String fromOwnerId,
            String toOwnerType, String toOwnerId, String transferOrderId, Instant changedAt) {
        this(id, traceCode, fromOwnerType, fromOwnerId, toOwnerType, toOwnerId, transferOrderId, changedAt, OWNERSHIP);
    }
}
