package com.complex.platform.access.dto;

// 文件版本：V0.01

import java.util.List;

/** M736 第二步：增减角色可查看项目的入参，写明这次加哪几条、减哪几条（不是改后的整份清单）。finalApproverId 只在本次含减少时用。 */
public record RoleViewableProjectsRequest(
        String idempotencyKey,
        String operatorNote,
        Boolean addViewAllProjects,
        List<String> addViewableProjectIds,
        Boolean removeViewAllProjects,
        List<String> removeViewableProjectIds,
        String finalApproverId
) {
}
