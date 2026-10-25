package com.complex.platform.customer;

// 文件版本：V0.01

import java.time.Instant;

/**
 * 会员等级配置：upgradeThreshold＝升级门槛（滚动统计周期内的成长值，BSM L820）；
 * pointMultiplierBasisPoints＝该等级积分倍率（万分比，10000=1.0x）。
 * 滚动统计周期、保护期、提前通知天数三项等级参数全平台统一，按版本审批（{@link TicketRewardConfigService#LEVEL_KINDS}）。
 */
public record CustomerMemberLevelRecord(
        String id,
        String projectId,
        String levelCode,
        String levelName,
        int levelRank,
        long upgradeThreshold,
        int pointMultiplierBasisPoints,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
