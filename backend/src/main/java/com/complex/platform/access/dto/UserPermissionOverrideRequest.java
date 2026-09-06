package com.complex.platform.access.dto;

import java.util.List;

/** M006-G-01（PGM-1.10.1「员工单独授予 − 显式拒绝」）：员工级覆盖入参；effect 取 grant | deny。 */
public record UserPermissionOverrideRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> permissionCodes,
        String effect,
        String reason
) {
}
