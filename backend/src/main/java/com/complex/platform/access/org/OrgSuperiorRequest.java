package com.complex.platform.access.org;

/** M087-A-01（PGM-1.7.1 组织层级）：设置某员工的直接上级；superiorId 为空表示清除；lineCode 为其所属部门/业务线（可空）。 */
public record OrgSuperiorRequest(
        String idempotencyKey,
        String operatorNote,
        String superiorId,
        String lineCode
) {
}
