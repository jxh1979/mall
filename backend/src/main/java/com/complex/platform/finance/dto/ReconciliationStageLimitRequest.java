package com.complex.platform.finance.dto;

// 文件版本：V0.01

/**
 * 第十八批 Y 段第 181 条 181-R2（ENUM:191「待识别与待处理两个阶段各自的停留时限为业务参数，按项目可配，出厂默认 1 个工作日，须同时配置上下限」）：
 * 配置项目某一阶段（pending_identification 待识别、pending_handling 待处理）的停留时限与上下限（工作日），三值须同时填写。
 */
public record ReconciliationStageLimitRequest(String idempotencyKey, String stage, Integer workingDays, Integer minWorkingDays,
        Integer maxWorkingDays) {}
