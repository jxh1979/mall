package com.complex.platform.amusement;

import java.time.Instant;

/**
 * 游乐巡检到期工单记录（amusement_inspection_tasks，V143）。
 * T3.1 激活该表：由 {@link AmusementInspectionTaskJob} 按设备安全检查周期预生成（pending）、到期翻 overdue、
 * 巡检 PASS 关联完成。deviceId 为空表示项目级任务（V143 注释）。
 */
public record AmusementInspectionTaskRecord(
        String id,
        String projectId,
        String deviceId,
        Instant dueAt,
        String status,
        Instant generatedAt,
        String completedInspectionId,
        Instant completedAt,
        String merchantId,
        Instant createdAt,
        Instant updatedAt
) {
}
