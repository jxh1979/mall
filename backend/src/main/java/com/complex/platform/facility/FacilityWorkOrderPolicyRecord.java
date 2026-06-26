package com.complex.platform.facility;

import java.time.Instant;

/** 楼宇维保工单 SLA/验收策略（类型 × 优先级）。 */
public record FacilityWorkOrderPolicyRecord(
        String id,
        String workOrderType,
        String priority,
        int responseMinutes,
        int handleMinutes,
        boolean requiresAcceptance,
        String acceptanceBy,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {}
