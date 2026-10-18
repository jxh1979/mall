package com.complex.platform.amusement;

// 文件版本：V0.01

import java.time.Instant;

/**
 * 票务权益实例在游乐核销链路所需的只读投影（§6.4 步骤 3 校验所需字段）。
 * 仅暴露校验/消费必需字段，避免游乐域耦合票务完整模型。
 */
public record AmusementEntitlementView(
        String id,
        String entitlementType,
        String status,
        Instant validFrom,
        Instant validTo,
        int remainingTimes,
        int numPeople,
        String complexProjectId
) {
    public AmusementEntitlementView(String id, String entitlementType, String status, Instant validFrom,
            Instant validTo, int remainingTimes, int numPeople) {
        this(id, entitlementType, status, validFrom, validTo, remainingTimes, numPeople, null);
    }
}
