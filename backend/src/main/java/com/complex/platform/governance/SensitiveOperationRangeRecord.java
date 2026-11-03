package com.complex.platform.governance;

// 文件版本：V0.01

import java.math.BigDecimal;

/**
 * 第 123 条 123-R1：项目敏感操作授权范围（BSM:1630「授权范围按项目在后台配置（业务参数，按项目可配，须同时配置上下限）。」）。
 * 圆心为 GPS 原始坐标（WGS-84，十进制度），半径单位米且落在上下限内；未配置的项目除 projectId 外皆为空（第十八批裁决 32：没配就不查位置）。
 */
public record SensitiveOperationRangeRecord(String projectId, BigDecimal centerLatitude, BigDecimal centerLongitude,
        Integer radiusMeters, Integer minRadiusMeters, Integer maxRadiusMeters) {

    static SensitiveOperationRangeRecord unconfigured(String projectId) {
        return new SensitiveOperationRangeRecord(projectId, null, null, null, null, null);
    }
}
