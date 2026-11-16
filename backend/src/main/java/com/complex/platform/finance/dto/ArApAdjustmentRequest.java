package com.complex.platform.finance.dto;

import java.util.List;

/**
 * R5 T7：锁定后补录审批申请（requestAdjustment）。
 * arApItemId 指向已锁定台账项；adjustmentAmountCent 为带符号补录变动额（正=增、负=减）。
 * 第十八批 Z 段第 186 条 186-R1：kind 缺省 amount（金额调整）；payment＝收付款补录，金额为正、paymentMethod 取线下四种，批准后登记收付款。
 * 186-R2：refund＝退款补录，金额为正，不填 paymentMethod（照原收款记录），批准后按原收款走原路退款。
 * 第十八批 AL 段第 253 条（第十八批裁决 56）：evidenceAttachmentIds 为原始凭证附件编号（至少一个，business_type=finance_locked_backfill 上传所得），
 * responsibleStaffId 为责任人账号（必填、须已存在）；第 245 条 245-R1：finalApproverId 为统一审批的最终审批人（可空＝申请人直接上级）。
 */
public record ArApAdjustmentRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String arApItemId,
        Long adjustmentAmountCent,
        String reason,
        String kind,
        String paymentMethod,
        String responsibleStaffId,
        String finalApproverId
) {
}
