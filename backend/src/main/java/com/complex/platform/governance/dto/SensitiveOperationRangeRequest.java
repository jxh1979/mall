package com.complex.platform.governance.dto;

// 文件版本：V0.01

import java.math.BigDecimal;

/** 第 123 条 123-R1：配置项目敏感操作授权范围；圆心、授权半径与半径上下限须同时填写。 */
public record SensitiveOperationRangeRequest(String idempotencyKey, BigDecimal centerLatitude, BigDecimal centerLongitude,
        Integer radiusMeters, Integer minRadiusMeters, Integer maxRadiusMeters) {}
