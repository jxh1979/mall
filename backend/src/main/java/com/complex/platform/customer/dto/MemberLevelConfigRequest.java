package com.complex.platform.customer.dto;

/**
 * 后台会员等级配置请求（会员忠诚度 PRD §3 等级/保级）。id 为空=新增、非空=按 id 更新（服务层校验归属项目）。
 * projectId 仅平台管理员可显式指定；项目级管理员一律绑定其 token scope（服务层覆盖请求体，遵循写路径不信任 project_id）。
 * pointMultiplierBasisPoints=该等级积分倍率万分比（>0，10000=1.0x）；keepPeriodDays 须>0；multiplier/门槛对齐 V089 DB CHECK。
 */
public record MemberLevelConfigRequest(
        String id,
        String projectId,
        String levelCode,
        String levelName,
        Integer levelRank,
        Long upgradeThreshold,
        Long keepThreshold,
        Integer keepPeriodDays,
        Integer graceDays,
        Integer pointMultiplierBasisPoints,
        String status
) {
}
