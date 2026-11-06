package com.complex.platform.finance;

import java.time.Instant;
import java.util.List;

/**
 * 对账差异（P0#7 / R6-D07 强制化）。
 * <p>expectedAmountCent：平台侧系统取数（不接受请求体手填）；actualAmountCent：第三方侧申报值
 * （未接入前 source=manual 录入并进复核）；source：system|manual。amountCent 为差额（expected−actual），
 * 由服务层从系统取数推导，留作展示与历史兼容。第十八批 Y 段第 180 条 180-R2（ENUM:202）：evidenceAttachmentIds 为渠道对账单原件附件编号，
 * 不带原件为空（待补凭证）。第十八批 Y 段第 181 条 181-R1（ENUM:191「待识别、待处理、待复核、已关闭」）：status 为
 * pending_identification、pending_handling、pending_review、closed；responsibleUserId 为当前负责人（登记人、识别人），stageEnteredAt 为进入
 * 本阶段的时刻，identifiedCause 为识别写下的差异原因，reviewId 为待复核时的复核单号。第十八批 Y 段第 181 条 181-R2：stageEscalatedAt 为本阶段
 * （待识别或待处理）已超时升级的时刻，未升级为空（只读，由超时作业写入）。
 */
public record ReconciliationDifferenceRecord(
        String id,
        String batchId,
        String differenceType,
        String businessRef,
        long amountCent,
        Long expectedAmountCent,
        Long actualAmountCent,
        String source,
        String status,
        Instant createdAt,
        List<String> evidenceAttachmentIds,
        String responsibleUserId,
        Instant stageEnteredAt,
        String identifiedCause,
        String reviewId,
        Instant stageEscalatedAt
) {

    /**
     * 兼容构造（V105 前的 7 参列）：无 expected/actual、source 默认 system。
     * 仅供历史调用点与测试双子过渡使用，新写端点应使用全参构造并显式给出系统取数。
     */
    public ReconciliationDifferenceRecord(
            String id,
            String batchId,
            String differenceType,
            String businessRef,
            long amountCent,
            String status,
            Instant createdAt
    ) {
        this(id, batchId, differenceType, businessRef, amountCent, null, null, "system", status, createdAt, List.of(),
                null, createdAt, null, null, null);
    }
}
