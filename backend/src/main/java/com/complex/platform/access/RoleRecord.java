package com.complex.platform.access;

import java.time.Instant;
import java.util.List;

public record RoleRecord(
        String id,
        String roleName,
        String description,
        String dataScope,
        String status,
        Instant createdAt,
        List<String> permissionCodes
) {
}
