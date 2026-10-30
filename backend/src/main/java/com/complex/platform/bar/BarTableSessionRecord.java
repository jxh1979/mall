package com.complex.platform.bar;

import java.time.Instant;
import java.time.LocalDate;

public record BarTableSessionRecord(
        String id,
        String barId,
        String merchantId,
        LocalDate businessDate,
        String tableId,
        String reservationId,
        String customerId,
        int guestCount,
        long minConsumptionSnapshotCent,
        int allowedPassFreeGuestCountSnapshot,
        String salesStaffId,
        String serviceStaffId,
        // 第十八批 K 段 95-R1（BSM 第 1021 行）：客人来源 platform_own 平台自有渠道／marketing 营销带客／walk_in 自来客（V579）。
        String guestSource,
        String status,
        Instant openedAt,
        Instant closedAt,
        String sourceSessionId,
        long sessionVersion,
        Instant createdAt,
        Instant updatedAt
) {
}
