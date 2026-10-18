package com.complex.platform.amusement;

import java.time.Instant;

/** CG-08b 补偿有效期参数（按综合体项目；'__global__' 出厂回落行，默认一年；上下限须同时配置）。 */
public record AmusementCompensationPolicyRecord(
        String projectId,
        int validityDays,
        int minDays,
        int maxDays,
        String updatedBy,
        Instant updatedAt
) {
    public static final String GLOBAL_SCOPE = "__global__";
}
