package com.complex.platform.finance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DailyCloseRecord(
        String id,
        LocalDate businessDate,
        String scopeType,
        String scopeId,
        String status,
        Instant lockedAt,
        Instant createdAt,
        // 第十八批 R 段 143-R1：确认时间；143-R5：归档时间（第十八批裁决 33）。
        Instant confirmedAt,
        Instant archivedAt,
        // 第十八批 R 段 143-R2：已标记为最终的数据类别（ENUM:190 五类，finance_daily_close_final_marks）。
        List<String> finalMarkCategories
) {
}
