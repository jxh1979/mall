package com.complex.platform.governance.dto;
// 文件版本：V0.01

import java.util.List;

/** 空间类型字典新建请求（后台管理员维护）。typeCode 不得重复，新建状态固定为启用。 */
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
