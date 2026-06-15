package com.complex.platform.access;

public record PermissionDefinition(
        String permissionCode,
        String permissionName,
        String domain,
        String riskLevel,
        boolean requiresApproval,
        String status
) {
}
