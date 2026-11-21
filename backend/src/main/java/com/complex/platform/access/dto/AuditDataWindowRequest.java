package com.complex.platform.access.dto;

// 文件版本：V0.01

import java.time.LocalDate;

/** 第十八批 BX 段第 372 条 372-R1：划定外审数据时段（setAuditDataWindow 请求体；起止两端都含，北京时间自然日）。 */
public record AuditDataWindowRequest(
        String idempotencyKey,
        LocalDate dataFrom,
        LocalDate dataTo
) {
}
