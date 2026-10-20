package com.complex.platform.inventory.dto;

// 文件版本：V0.02

import java.util.List;

/**
 * FS-011 门店要货流转请求体：提交、拣配、送达（Step），供货主体审核与差异处理财务复核（SupplierReview），草稿改单（DraftLines），
 * 差异处理登记（Disposition）。
 */
public final class TransferFlowRequests {
    private TransferFlowRequests() {
    }

    public record Step(String idempotencyKey, String operatorNote) {
    }

    /** decision：approved（同意）或 rejected（不同意，须写原因）。供货主体审核不同意退回草稿；差异处理复核驳回则该项不生效。 */
    public record SupplierReview(String idempotencyKey, String decision, String reason) {
    }

    /** 草稿改单：整单替换明细。 */
    public record DraftLines(String idempotencyKey, List<TransferLineRequest> lines) {
    }

    /**
     * 差异处理登记（须附交接证据，财务复核后生效）：return_accepted 退回验收、loss 损失（responsibleParty 为 supplier/carrier/receiver）、
     * reship 补发（traceCodes 为补发货随货溯源码）针对 lineId 的 quantity 件；payment_adjustment 为货款调减 amountCent 分。
     * FS-019 借用物无法归还登记（POST /unreturnable）复用本体：lineId 的 quantity 件（traceCodes 为其中的码）、reason 原因、
     * amountCent 赔偿（空＝数量×建单锁定内部结算价）、disposalNote 实物处置方式、evidenceAttachmentIds 证据。
     */
    public record Disposition(String idempotencyKey, String dispositionType, String lineId, Integer quantity, String responsibleParty,
            Long amountCent, List<String> traceCodes, String reason, List<String> evidenceAttachmentIds, String disposalNote) {
    }
}
