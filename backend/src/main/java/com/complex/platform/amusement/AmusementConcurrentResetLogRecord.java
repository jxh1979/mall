package com.complex.platform.amusement;

import java.time.Instant;
import java.time.LocalDate;

/** CG-03 在玩人数系统清零留痕：business_day_end（营业日结束清零）/ session_end（场次结束清空），系统主体，无人工入口。 */
public record AmusementConcurrentResetLogRecord(
        String id,
        String projectId,
        String scope,
        LocalDate businessDate,
        String sessionId,
        int clearedRows,
        int clearedPeople,
        Instant boundaryAt,
        Instant resetAt
) {
    public static final String SCOPE_BUSINESS_DAY_END = "business_day_end";
    public static final String SCOPE_SESSION_END = "session_end";
}
