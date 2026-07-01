package com.complex.platform.customer;

import java.time.Instant;
import java.time.LocalDate;

public record CustomerRecord(
        String id,
        String phone,
        String maskedPhone,
        String status,
        LocalDate birthDate,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt,
        // V194 需求：临时封禁(suspended)到期日；到期自动恢复 active；null=非临时封禁。
        Instant suspendedUntil
) {
    /** 兼容构造（原 8 字段）：suspendedUntil=null。既有调用零改动。 */
    public CustomerRecord(String id, String phone, String maskedPhone, String status,
            LocalDate birthDate, Instant lastLoginAt, Instant createdAt, Instant updatedAt) {
        this(id, phone, maskedPhone, status, birthDate, lastLoginAt, createdAt, updatedAt, null);
    }
}
