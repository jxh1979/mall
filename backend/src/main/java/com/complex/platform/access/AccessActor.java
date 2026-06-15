package com.complex.platform.access;

public record AccessActor(
        String id,
        String username,
        String accountType,
        String organizationId,
        String merchantId,
        String status
) {
}
