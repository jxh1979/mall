package com.complex.platform.amusement;

import java.time.Instant;
import java.util.Map;

public record AmusementProjectRecord(
        String id,
        String projectName,
        int capacity,
        String openStatus,
        String safetyTemplateId,
        Map<String, Object> eligibilityRule,
        // V114 清单#21：项目入口点位（可空；未绑定为 NULL，向后兼容）。
        String locationPointId,
        Instant createdAt,
        Instant updatedAt,
        // 第十八批 CC 段 399-R1：项目所属游乐商户（空＝游乐公司自营）随项目返回，后台据此按项目取游乐权益的提供方。
        String merchantId,
        // 第十八批 BR 段 357-R1（BSM:753；ENUM:53）：游乐公司为该项目引用的所属商户内部结算价目录条目；空＝不参与域内结算。
        String catalogItemCode
) {
    /** 兼容构造（建项目时尚未引用目录条目）。 */
    public AmusementProjectRecord(String id, String projectName, int capacity, String openStatus, String safetyTemplateId,
            Map<String, Object> eligibilityRule, String locationPointId, Instant createdAt, Instant updatedAt, String merchantId) {
        this(id, projectName, capacity, openStatus, safetyTemplateId, eligibilityRule, locationPointId, createdAt, updatedAt, merchantId,
                null);
    }
}
