package com.complex.platform.customer.dto;

/**
 * 后台连续签到阶梯奖励配置请求（会员忠诚度 PRD §6 连续签到）。id 为空=新增、非空=更新。
 * streakDays 须>=1、awardPoints 须>=0（对齐 V089 DB CHECK）；projectId 项目级管理员由 token scope 覆盖。
 */
public record CheckInTierConfigRequest(
        String id,
        String projectId,
        Integer streakDays,
        Long awardPoints,
        String status
) {
}
