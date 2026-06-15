package com.complex.platform.customer;

public record CustomerBarReservationOptionResponse(
        String barId,
        String tableId,
        String tableType,
        String tableName,
        int capacity,
        int allowedPassFreeGuestCount,
        long minConsumptionCent,
        String currency,
        String status
) {
}
