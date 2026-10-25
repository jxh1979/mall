package com.complex.platform.customer.dto;

// 文件版本：V0.01

/**
 * 后台会员等级配置请求（会员忠诚度 PRD §3 等级）。id 为空=新增、非空=按 id 更新（服务层校验归属项目）。
 * projectId 仅平台管理员可显式指定；项目级管理员一律绑定其 token scope（服务层覆盖请求体，遵循写路径不信任 project_id）。
 * pointMultiplierBasisPoints=该等级积分倍率万分比（>0，10000=1.0x）；multiplier/门槛对齐 V089 DB CHECK。
 * 保护期等三项等级参数全平台统一另行审批（OBS-13-01，BSM L820），不按等级录入。
 */
public record MemberLevelConfigRequest(
        String id,
        String projectId,
        String levelCode,
        String levelName,
        Integer levelRank,
        Long upgradeThreshold,
        Integer pointMultiplierBasisPoints,
        String status
) {
}
