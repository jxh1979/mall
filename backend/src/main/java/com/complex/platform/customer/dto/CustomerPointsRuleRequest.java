package com.complex.platform.customer.dto;

/**
 * 后台积分规则配置请求（REQ-12, P0-6）。projectId 仅平台管理员创建时可显式指定；项目级管理员
 * 一律绑定其 token scope（服务层覆盖请求体，遵循 AC-ISO-2 写路径不信任请求体 project_id）。
 * levelThreshold 为等级阈值 JSON 文本（如 {"silver":1000,"gold":5000}）。
 */
public record CustomerPointsRuleRequest(
        String projectId,
        String ruleName,
        String earnMethod,
        Long earnRatio,
        String levelThreshold,
        String validityType,
        Integer validityDays,
        String expireMode,
        String clearDay,
        String status
) {
}
