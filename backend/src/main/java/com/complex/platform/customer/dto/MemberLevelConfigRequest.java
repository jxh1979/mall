package com.complex.platform.customer.dto;

// 文件版本：V0.02

/**
 * 后台会员等级配置请求（会员忠诚度 PRD §3 等级）。id 为空=新增、非空=按 id 更新。
 * 第十八批第 12 条：会员等级全平台一套（BSM L820/L821），不带综合体，仅全国范围账号可写（服务层校验）。
 * pointMultiplierBasisPoints=该等级积分倍率万分比（>0，10000=1.0x）；multiplier/门槛对齐 V089 DB CHECK。
 * 保护期等三项等级参数全平台统一另行审批（OBS-13-01，BSM L820），不按等级录入。
 */
public record MemberLevelConfigRequest(
        String id,
        String levelCode,
        String levelName,
        Integer levelRank,
        Long upgradeThreshold,
        Integer pointMultiplierBasisPoints,
        String status
) {
}
