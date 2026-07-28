package com.complex.platform.governance.dto;

import java.time.LocalDate;

/**
 * 场地营业时间配置写入请求（一次改一个场地的一个星期几）。
 *
 * @param dayOfWeek  0=周日 .. 6=周六
 * @param openTime   开业时刻 HH:mm
 * @param closeTime  歇业时刻 HH:mm；小于 openTime 视为跨零点（如 20:00→次日 02:00）
 * @param disabled   true=该日不营业（停用配置，行保留可随时恢复，不物理删）
 */
public record VenueBusinessHoursRequest(
        String idempotencyKey,
        String venueId,
        Integer dayOfWeek,
        String openTime,
        String closeTime,
        Boolean disabled,
        LocalDate effectiveFrom
) {
}
