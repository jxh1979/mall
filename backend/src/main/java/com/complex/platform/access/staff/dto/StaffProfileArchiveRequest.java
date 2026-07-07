package com.complex.platform.access.staff.dto;

/** 员工档案存档请求。reason=存档理由(存档时必填,业务HTML 2026-07-07)。 */
public record StaffProfileArchiveRequest(String reason) {
}
