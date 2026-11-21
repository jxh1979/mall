package com.complex.platform.access;

// 文件版本：V0.01

import java.time.LocalDate;

/** 第十八批 BX 段第 372 条 372-R1：外审数据时段一行（setAuditDataWindow 响应、listAuditDataWindows 条目；账号名由前端按账号列表对上）。 */
public record AuditDataWindowRecord(String userId, LocalDate dataFrom, LocalDate dataTo) {
}
