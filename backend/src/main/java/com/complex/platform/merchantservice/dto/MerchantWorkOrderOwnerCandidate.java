package com.complex.platform.merchantservice.dto;

// 文件版本：V0.01

/** 第十八批 L 段第 102 条 102-R1：派单负责人候选（在岗、持处理权限、能管到该商户经营项目的平台员工）。 */
public record MerchantWorkOrderOwnerCandidate(
        String userId,
        String username,
        String staffName
) {
}
