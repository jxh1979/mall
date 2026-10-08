package com.complex.platform.access;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

/**
 * M736：viewAllProjects＝勾了「可以查看全部项目」；viewableProjectIds＝展开后逐个勾选的可查看项目
 * （项目编号，或城市项目公司、域主管公司的主体编号）。两项都没有的不是可跨项目角色。
 */
public record RoleRecord(
        String id,
        String roleName,
        String description,
        String dataScope,
        String status,
        Instant createdAt,
        List<String> permissionCodes,
        @JsonIgnore String organizationId,
        boolean viewAllProjects,
        List<String> viewableProjectIds
) {
    public RoleRecord {
        viewableProjectIds = viewableProjectIds == null ? List.of() : List.copyOf(viewableProjectIds);
    }

    /** 向后兼容构造：两项都未勾选。 */
    public RoleRecord(
            String id,
            String roleName,
            String description,
            String dataScope,
            String status,
            Instant createdAt,
            List<String> permissionCodes,
            String organizationId
    ) {
        this(id, roleName, description, dataScope, status, createdAt, permissionCodes, organizationId, false, List.of());
    }
}
