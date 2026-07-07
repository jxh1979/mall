package com.complex.platform.access;

import java.time.Instant;
import java.util.List;

public record PermissionTemplate(
        String id,
        String templateCode,
        String templateName,
        String description,
        String templateType,
        String sourceTemplateId,
        String ownerScopeType,
        String ownerScopeId,
        String usageMode,
        Instant validFrom,
        Instant validUntil,
        boolean singleUse,
        String status,
        String reason,
        String approvedBy,
        Instant approvedAt,
        Instant createdAt,
        Instant updatedAt,
        List<String> permissionCodes
) {
    /** 向后兼容构造器:不带权限码(测试/旧写路径),permissionCodes 默认空;读路径由仓库 array_agg 填充。 */
    public PermissionTemplate(
            String id, String templateCode, String templateName, String description, String templateType,
            String sourceTemplateId, String ownerScopeType, String ownerScopeId, String usageMode,
            Instant validFrom, Instant validUntil, boolean singleUse, String status, String reason,
            String approvedBy, Instant approvedAt, Instant createdAt, Instant updatedAt) {
        this(id, templateCode, templateName, description, templateType, sourceTemplateId, ownerScopeType,
                ownerScopeId, usageMode, validFrom, validUntil, singleUse, status, reason, approvedBy,
                approvedAt, createdAt, updatedAt, List.of());
    }
}
