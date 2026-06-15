package com.complex.platform.access;

public record PermissionTemplateItem(
        String templateId,
        String permissionCode,
        AccessRepository.Effect effect,
        String dataScope,
        boolean sensitive
) {
}
