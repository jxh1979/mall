package com.complex.platform.customer;

import java.time.Instant;

/**
 * 储值余额账户（gap-analysis §7A P1：预付卡 / 余额账户）。
 * balanceCent=当前余额（分）；status=active|frozen|closed；每客每项目唯一。
 */
public record CustomerStoredValueAccountRecord(
        String id,
        String customerId,
        String projectId,
        long balanceCent,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
