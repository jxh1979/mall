package com.complex.platform.customer.dto;

/**
 * 后台生日加倍规则配置请求（会员忠诚度 PRD §6 生日加倍）。id 为空=新增、非空=更新。
 * windowType=birthday_day（仅生日当天）/birthday_month（生日当月）；multiplierBasisPoints 须>10000（即>1.0x，对齐 V089 DB CHECK）。
 * 第十八批第 12 条同根：生日加倍规则全平台一套（BSM L831），不带综合体，仅全国范围账号可写；rule_name 全局唯一。
 */
public record BirthdayRuleConfigRequest(
        String id,
        String ruleName,
        String windowType,
        Integer multiplierBasisPoints,
        String status
) {
}
