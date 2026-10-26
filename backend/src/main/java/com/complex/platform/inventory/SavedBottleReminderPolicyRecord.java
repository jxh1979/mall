package com.complex.platform.inventory;

// 文件版本：V0.01

import java.util.List;

/**
 * 存酒到期提醒节点（saved_bottle_reminder_policy）。BSM L1072「到期前按后台配置的提醒节点各发一次短信提醒（业务参数，按项目可配，
 * 出厂默认到期前第 14 天、第 7 天、第 3 天、第 1 天）」：scopeId=global（出厂行）| project:&lt;项目 id&gt;，缺行回落 global；
 * reminderDays 为到期前第 N 天（降序）。
 */
public record SavedBottleReminderPolicyRecord(String scopeId, List<Integer> reminderDays) {
}
