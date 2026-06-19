package com.complex.platform.bar;

import java.util.List;

/**
 * 卡座/桌台空间指派（回填）请求载荷。
 *
 * <p>处理清单 #8「存量 space_id 数据回填」：bar_tables（V013 仅 bar_id；bars 表无 space 列）
 * 无可派生源，无法 SQL 自动回填。本请求让运营录入/回填 bar_tables.space_id；录入后
 * AdmissionGuard over_space 即对该桌台关联账单（bar_bill→table_id→space_id）真实生效。</p>
 *
 * @param idempotencyKey        操作幂等键（重复提交返回同一结果）。
 * @param spaceId               目标空间 id；必须存在且属于同一商户（spaces.merchant_id）。
 * @param operatorNote          操作备注，进入审计留痕。
 * @param evidenceAttachmentIds 证据附件 id 列表。
 */
public record BarTableSpaceAssignmentRequest(
        String idempotencyKey,
        String spaceId,
        String operatorNote,
        List<String> evidenceAttachmentIds
) {
}
