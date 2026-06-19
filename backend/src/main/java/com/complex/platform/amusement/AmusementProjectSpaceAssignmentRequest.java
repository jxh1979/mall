package com.complex.platform.amusement;

import java.util.List;

/**
 * 游乐项目空间指派（回填）请求载荷。
 *
 * <p>处理清单 #8「存量 space_id 数据回填」：amusement_projects（V014 无 area/store/space 列）
 * 无可派生源，无法 SQL 自动回填。本请求让运营录入/回填 amusement_projects.space_id；录入后
 * AdmissionGuard over_space 即对该项目相关受控对象（inspection / maintenance_record /
 * incident / group_booking 经各自 project_id → amusement_projects.space_id）真实生效。</p>
 *
 * @param idempotencyKey        操作幂等键（重复提交返回同一结果）。
 * @param spaceId               目标空间 id；必须存在且属于同一商户（spaces.merchant_id）。
 * @param operatorNote          操作备注，进入审计留痕。
 * @param evidenceAttachmentIds 证据附件 id 列表。
 */
public record AmusementProjectSpaceAssignmentRequest(
        String idempotencyKey,
        String spaceId,
        String operatorNote,
        List<String> evidenceAttachmentIds
) {
}
