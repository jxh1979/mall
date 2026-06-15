package com.complex.platform.terminal;

import java.time.Instant;

public record DeviceCredentialRecord(
        String deviceId,
        String tokenSha256,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
