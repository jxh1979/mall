package com.complex.platform.bar;

import java.time.Instant;

public record BarCommissionCalculationRecord(
        String id,
        String billId,
        String salesStaffId,
        String salesRoleType,
        long commissionBaseCent,
        int commissionRateBp,
        long commissionAmountCent,
        String status,
        Instant createdAt
) {
}
