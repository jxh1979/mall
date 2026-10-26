package com.complex.platform.inventory.dto;

// 文件版本：V0.01

import java.util.List;

/**
 * 存酒到期提醒节点配置（BSM L1072，业务参数，按项目可配）：scope_id=global | project:&lt;项目 id&gt;；
 * reminder_days 为到期前第 N 天（N≥1、不重复、至少一个）。
 */
public record SavedBottleReminderPolicyRequest(
        String idempotencyKey,
        String scopeId,
        List<Integer> reminderDays
) {
}
