package com.complex.platform.governance;
// 文件版本：V0.01

import java.time.Instant;

public record ProjectRecord(
        String id,
        String merchantId,
        String venueType,
        String name,
        String code,
        String floor,
        Integer capacity,
        String status,
        Instant createdAt,
        Instant updatedAt,
        String ownerPerson,
        String address,
        String businessScope
) {
    public ProjectRecord(String id, String merchantId, String venueType, String name, String code, String floor,
            Integer capacity, String status, Instant createdAt, Instant updatedAt) {
        this(id, merchantId, venueType, name, code, floor, capacity, status, createdAt, updatedAt, null, null, null);
    }
}
