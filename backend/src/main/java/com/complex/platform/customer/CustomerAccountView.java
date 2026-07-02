package com.complex.platform.customer;

import java.time.Instant;

/** 平台侧客人账号安全投影（V194）：只给掩码手机号，明文/密文均不出网。 */
public record CustomerAccountView(
        String id,
        String maskedPhone,
        String status,
        Instant suspendedUntil,
        Instant createdAt
) {
    static CustomerAccountView of(CustomerRecord record) {
        return new CustomerAccountView(record.id(), record.maskedPhone(), record.status(),
                record.suspendedUntil(), record.createdAt());
    }
}
