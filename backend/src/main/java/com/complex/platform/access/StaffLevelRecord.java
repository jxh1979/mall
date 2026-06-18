package com.complex.platform.access;

import java.time.Instant;

/**
 * 职级定义（REQ-60）。main_level=M1..M10，sub_level=upper/middle/lower（上/中/下），
 * level_rank 1..30 派生（越大越高，上>中>下，M10>M1），供审批层级按 rank 计算。
 * salary_policy_summary 仅记职级对应的薪资大范围（REQ-61，具体薪资见 StaffLevelAssignment）。
 */
public record StaffLevelRecord(
        String id,
        String mainLevel,
        String subLevel,
        int levelRank,
        String salaryPolicySummary,
        String status,
        Instant createdAt
) {
}
