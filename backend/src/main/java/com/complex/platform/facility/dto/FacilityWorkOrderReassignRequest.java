package com.complex.platform.facility.dto;

// 文件版本：V0.01

/**
 * 第十二批操作员裁决第 9 条「补转派」：已派单或处理中的设施工单由物业主管改派给他人；处理时限不重算；
 * 留痕原处理人、新处理人和原因（原因必填）。
 */
public record FacilityWorkOrderReassignRequest(
        String assigneeId,
        String reason,
        String idempotencyKey) {}
