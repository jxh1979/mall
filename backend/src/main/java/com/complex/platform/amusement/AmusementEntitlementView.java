package com.complex.platform.amusement;

// 文件版本：V0.02

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
        String complexProjectId,
        // 第十八批 CC 段第 399 条 399-R2（BSM:784「游乐（指定项目/次数/场次）」）：权益模板指定的游乐项目（可空），核销只认该项目。
        String amusementProjectId
) {
    public AmusementEntitlementView(String id, String entitlementType, String status, Instant validFrom,
            Instant validTo, int remainingTimes, int numPeople) {
        this(id, entitlementType, status, validFrom, validTo, remainingTimes, numPeople, null, null);
    }

    /** 399-R2：指定游乐项目之前的签名，amusementProjectId 取空。 */
    public AmusementEntitlementView(String id, String entitlementType, String status, Instant validFrom,
            Instant validTo, int remainingTimes, int numPeople, String complexProjectId) {
        this(id, entitlementType, status, validFrom, validTo, remainingTimes, numPeople, complexProjectId, null);
    }
}
