package com.complex.platform.amusement;

import java.util.List;

/**
 * 游乐项目点位/设备并发容量配置请求体（写入 amusement_capacity_config，供 V083 三维容量第三维使用）。
 * locationPointId / deviceId 至少配置其一；maxConcurrent 为该点位/设备的并发在场上限（>=0）。
 * enabled 省略默认 true；effectiveSessionPattern 省略默认空串（适用全部场次）。
 */
public record AmusementCapacityConfigRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String locationPointId,
        String deviceId,
        Integer maxConcurrent,
        String effectiveSessionPattern,
        Boolean enabled
) {
}
