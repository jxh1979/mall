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
        Instant updatedAt,
        // 第十八批 BY 段第 387 条 387-R2（第十八批裁决 81「空间类型补存存档时间、存档人、理由」，V740）：存档时写、取消存档清空，
        // 申请删除、驳回删除等其余状态变化原样保留。
        Instant archivedAt,
        String archivedBy,
        String archiveReason
) {
    /** 387-R2：存档信息之前的签名，既有构造点零改动，三项取空。 */
    public SpaceTypeRecord(String id, String typeCode, String typeName, int sortOrder, String status,
            Instant createdAt, Instant updatedAt) {
        this(id, typeCode, typeName, sortOrder, status, createdAt, updatedAt, null, null, null);
    }
}
