package com.complex.platform.amusement;

import java.time.LocalDate;

/**
 * 游乐核销记录 只读筛选（阶段3）。全部可空=不加该谓词。
 *
 * @param projectId 项目筛选
 * @param sessionId 场次筛选
 * @param direction 方向 entry/exit
 * @param dateFrom  核销日期起（含，按 created_at 当日 00:00）
 * @param dateTo    核销日期止（含，按 created_at 次日 00:00 前）
 */
public record AmusementVerificationQuery(
        String projectId,
        String sessionId,
        String direction,
        LocalDate dateFrom,
        LocalDate dateTo
) {
    /** 空白串收敛为 null，避免 SQL 谓词误命中空串。 */
    public AmusementVerificationQuery {
        projectId = blankToNull(projectId);
        sessionId = blankToNull(sessionId);
        direction = blankToNull(direction);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
