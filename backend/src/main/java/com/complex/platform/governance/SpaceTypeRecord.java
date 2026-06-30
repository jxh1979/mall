package com.complex.platform.governance;

import java.time.Instant;

/** 条目3 空间类型字典一行（后台可维护、前端下拉、createSpace 硬校验数据源）。 */
public record SpaceTypeRecord(
        String id,
        String typeCode,
        String typeName,
        int sortOrder,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
