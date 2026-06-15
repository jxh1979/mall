package com.complex.platform.access;

import java.util.List;

public record PermissionTemplateApplyResult(
        List<PermissionTemplateApplication> applications
) {
}
