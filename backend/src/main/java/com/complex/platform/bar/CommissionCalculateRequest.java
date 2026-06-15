package com.complex.platform.bar;

public record CommissionCalculateRequest(
        String idempotencyKey,
        String billId,
        Boolean includeOnlyTableSales,
        Boolean recalculate
) {
}
