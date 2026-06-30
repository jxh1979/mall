package com.complex.platform.governance.dto;

import java.util.List;

/** 条目3 空间类型字典 录入/修改请求（后台管理员维护）。typeCode 为唯一键，存在则更新名称/排序/状态。 */
public record SpaceTypeRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String typeCode,
        String typeName,
        Integer sortOrder,
        String status
) {
}
