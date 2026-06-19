package com.complex.platform.ticketing;

import java.time.Instant;

public record EntitlementTemplateRecord(
        String id,
        String name,
        String entitlementType,
        int defaultTotalTimes,
        long defaultValueCent,
        String status,
        // R5 审计点②：权益归属（各可空）。联合通票各模板可填各自商户/场地/点位，
        // 发放时优先采用；为空则回退出票方商户（向后兼容既有单模板票）。
        String merchantId,
        String venueId,
        String pointId,
        Instant createdAt,
        Instant updatedAt
) {
}
