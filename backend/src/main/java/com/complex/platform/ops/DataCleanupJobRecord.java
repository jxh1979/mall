package com.complex.platform.ops;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** 一条数据清理作业台账（dry-run 预览→提交审批→审批执行的全生命周期留痕）。 */
public record DataCleanupJobRecord(
        String id,
        String scopeType,
        Map<String, Object> filterParams,
        String status,
        int previewCount,
        List<Map<String, Object>> previewSample,
        String reason,
        String requestedBy,
        Instant requestedAt,
        String approvedBy,
        Instant approvedAt,
        Instant executedAt,
        Integer deletedCount,
        Instant createdAt,
        Instant updatedAt) {
}
