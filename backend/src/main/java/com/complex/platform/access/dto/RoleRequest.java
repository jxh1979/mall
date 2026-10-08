package com.complex.platform.access.dto;

import java.util.List;

/** 建角色入参。M736：viewAllProjects＝勾「可以查看全部项目」（不给按未勾选），viewableProjectIds＝展开后逐个勾选的可查看项目。 */
public record RoleRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String roleName,
        String description,
        List<String> permissionCodes,
        String dataScope,
        Boolean viewAllProjects,
        List<String> viewableProjectIds
) {
    /** 向后兼容构造：两项都不给（不勾选可查看项目）。 */
    public RoleRequest(
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds,
            String roleName,
            String description,
            List<String> permissionCodes,
            String dataScope
    ) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, roleName, description, permissionCodes, dataScope, null, null);
    }
}
