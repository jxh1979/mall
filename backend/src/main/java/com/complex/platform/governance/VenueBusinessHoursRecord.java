package com.complex.platform.governance;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 场地营业时间（V226 建表 venue_business_hours，本记录=其后台配置面）。
 *
 * <p>一条 = 某场地某个星期几的「开业→歇业」时刻。核销时按这张表判「当前是否营业」并惰性开场次；
 * 入场资格权益（consumption_window=per_session）与当天无限权益都靠它划场次，配错=当场核销全被拒。
 *
 * @param dayOfWeek   0=周日 .. 6=周六（与 V226 库内口径一致，Java DayOfWeek 的归一在核销侧）
 * @param openTime    开业时刻 HH:mm（墙钟本地时间）
 * @param closeTime   歇业时刻 HH:mm；小于 openTime 表示跨零点（如 20:00→次日 02:00）
 * @param disabled    该日配置是否停用（库内 is_draft=TRUE）。停用=不参与场次判定=该日不营业，
 *                    但配置行保留、可一键恢复（不做物理删除）
 */
public record VenueBusinessHoursRecord(
        String id,
        String venueId,
        int dayOfWeek,
        String openTime,
        String closeTime,
        boolean disabled,
        LocalDate effectiveFrom,
        Instant createdAt,
        Instant updatedAt
) {
}
