package com.complex.platform.access.staff.dto;

import com.complex.platform.access.staff.StaffShiftRecord;
import java.util.List;

/**
 * 批量排班结果：排班员提交完要看到「到底排出了什么」——新建几个班、覆盖改了几个班、逐条明细。
 *
 * <p>新建 vs 覆盖的判据：落库走 {@code ON CONFLICT (user_id, start_at) DO UPDATE}，冲突分支不改
 * {@code created_at}，所以回读到的行 {@code createdAt == updatedAt} 即本次新建，否则是覆盖了旧班。
 */
public record StaffShiftBatchResult(
        int createdCount,
        int updatedCount,
        List<StaffShiftRecord> shifts
) {
}
