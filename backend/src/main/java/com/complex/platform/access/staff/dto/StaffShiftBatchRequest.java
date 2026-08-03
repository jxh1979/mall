package com.complex.platform.access.staff.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 按周批量排班请求（2026-08-02 用户拍板）：排班员一次排出一周的班，而不是一人一天点一次。
 *
 * <p>一次提交 = 一批员工 × 一个日期窗口 × 每天同一个时段。日期窗口由 {@code startDate} + {@code days}
 * 框出；{@code weekdays} 非空时只排勾中的星期几（1=周一 … 7=周日），为空则窗口内每天都排。
 *
 * <p>跨零点班只填时段：{@code endTime} 不晚于 {@code startTime}（如 22:00-06:00）即视为跨零点，
 * 班次结束自动顺延到次日，排班员不用手动选日期。
 */
public record StaffShiftBatchRequest(
        String idempotencyKey,
        List<String> userIds,
        LocalDate startDate,
        Integer days,
        List<Integer> weekdays,
        String startTime,
        String endTime,
        List<String> pointSpaceIds,
        String note
) {
}
