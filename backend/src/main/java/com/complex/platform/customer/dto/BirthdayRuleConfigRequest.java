package com.complex.platform.customer.dto;

/**
 * 后台生日加倍规则配置请求（会员忠诚度 PRD §6 生日加倍）。id 为空=新增、非空=更新。
 * windowType=birthday_day（仅生日当天）/birthday_month（生日当月）；multiplierBasisPoints 须>10000（即>1.0x，对齐 V089 DB CHECK）。
 * projectId 项目级管理员由 token scope 覆盖；生日规则 (project_id, rule_name) 唯一。
 */
public record BirthdayRuleConfigRequest(
        String id,
        String projectId,
        String ruleName,
        String windowType,
        Integer multiplierBasisPoints,
        String status
) {
}
