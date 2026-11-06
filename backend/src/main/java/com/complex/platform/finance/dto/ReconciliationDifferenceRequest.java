package com.complex.platform.finance.dto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/**
 * P0#7 / R6-D07：创建对账差异写端点请求。
 * <p>关键约束（不可绕过）：
 * <ul>
 *   <li>请求体<b>不接受 expected</b>——平台侧 expected 一律由系统取数（按 businessRef 聚合平台权威台账）。</li>
 *   <li>source=system：平台侧系统取数比对；source=manual：第三方侧未接入时手工录入 actual，强制进复核。</li>
 *   <li>纯手填（source=manual 且无平台侧系统取数支撑）由服务层拒绝。</li>
 * </ul>
 */
public record ReconciliationDifferenceRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String batchId,
        String differenceType,
        String businessRef,
        // 第十八批 Y 段第 180 条 180-R2（ENUM:189「缺失差异（单边缺失）」）：可为 0，只有缺失差异收 0（服务层判）。
        @NotNull @PositiveOrZero Long actualAmountCent,
        String source
) {
}
