package com.complex.platform.inventory;

// 文件版本：V0.01

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

/**
 * FS-017 存酒作废后的对客补存/赔付记录（saved_bottle_remedies）。BSM L1072：
 * 「存酒作废的，由该酒吧为客人以同品、同等剩余量补存，到期日不变，补酒成本计入该酒吧的经营损失」
 * 「无法替代的，双方确认赔付方案，经门店主管和财务审批后支付……实物或赔款实际交付后才结案」
 * 「内部报损、员工赔偿与对客处理分开……对客实际交付结案与内部追收状态分别记录」。
 *
 * <p>customerDeliveryStatus（对客交付）与 recoveryStatus（内部追收）互不读取：对客结案不等追收，追收不改对客状态。
 */
public record SavedBottleRemedyRecord(
        String id,
        String savedBottleId,
        @JsonIgnore String merchantId,
        String projectId,
        /** replacement=同品同量补存 | compensation=赔付。 */
        String remedyType,
        String replacementSavedBottleId,
        /** 赔付金额（分）。 */
        Long amountCent,
        /** 经营损失（分）＝补酒成本（内部结算价×作废剩余量）或赔付金额；内部结算价解析不到时留空由财务补计。 */
        Long lossCostCent,
        String lossArApItemId,
        /** original_merchant=原门店承担 | platform_company=接收后由娱乐平台公司承担。 */
        String bearerType,
        String bearerId,
        String supervisorApprovedBy,
        Instant supervisorApprovedAt,
        String financeApprovedBy,
        Instant financeApprovedAt,
        /** pending_approval | approved | closed（实物或赔款实际交付后才 closed）。 */
        String customerDeliveryStatus,
        Instant deliveredAt,
        List<String> deliveryEvidenceIds,
        String reason,
        /** none | original_merchant（平台垫付后向原门店追收）| staff（员工照价赔偿）。 */
        String recoveryPartyType,
        String recoveryPartyId,
        Long recoveryAmountCent,
        /** none | pending | recovered。 */
        String recoveryStatus,
        String recoveryArApItemId,
        String advanceApprovedBy,
        Instant advanceApprovedAt,
        List<String> recoveryEvidenceIds,
        String createdBy,
        Instant createdAt,
        Instant updatedAt
) {
    public SavedBottleRemedyRecord withApproval(String role, String by, Instant at) {
        boolean supervisor = "supervisor".equals(role);
        String supBy = supervisor ? by : supervisorApprovedBy;
        Instant supAt = supervisor ? at : supervisorApprovedAt;
        String finBy = supervisor ? financeApprovedBy : by;
        Instant finAt = supervisor ? financeApprovedAt : at;
        String status = supBy != null && finBy != null ? "approved" : customerDeliveryStatus;
        return new SavedBottleRemedyRecord(id, savedBottleId, merchantId, projectId, remedyType, replacementSavedBottleId,
                amountCent, lossCostCent, lossArApItemId, bearerType, bearerId, supBy, supAt, finBy, finAt, status,
                deliveredAt, deliveryEvidenceIds, reason, recoveryPartyType, recoveryPartyId, recoveryAmountCent,
                recoveryStatus, recoveryArApItemId, advanceApprovedBy, advanceApprovedAt, recoveryEvidenceIds,
                createdBy, createdAt, at);
    }

    /** 对客结案：实物/赔款实际交付；同时记经营损失台账行（不读任何追收字段）。 */
    public SavedBottleRemedyRecord withDelivery(Instant at, List<String> evidence, Long loss, String lossItemId) {
        return new SavedBottleRemedyRecord(id, savedBottleId, merchantId, projectId, remedyType, replacementSavedBottleId,
                amountCent, loss, lossItemId, bearerType, bearerId, supervisorApprovedBy, supervisorApprovedAt,
                financeApprovedBy, financeApprovedAt, "closed", at, evidence, reason, recoveryPartyType, recoveryPartyId,
                recoveryAmountCent, recoveryStatus, recoveryArApItemId, advanceApprovedBy, advanceApprovedAt,
                recoveryEvidenceIds, createdBy, createdAt, at);
    }

    public SavedBottleRemedyRecord withAdvance(String by, Instant at) {
        return new SavedBottleRemedyRecord(id, savedBottleId, merchantId, projectId, remedyType, replacementSavedBottleId,
                amountCent, lossCostCent, lossArApItemId, bearerType, bearerId, supervisorApprovedBy, supervisorApprovedAt,
                financeApprovedBy, financeApprovedAt, customerDeliveryStatus, deliveredAt, deliveryEvidenceIds, reason,
                recoveryPartyType, recoveryPartyId, recoveryAmountCent, recoveryStatus, recoveryArApItemId, by, at,
                recoveryEvidenceIds, createdBy, createdAt, at);
    }

    /** 内部追收状态变化（不改对客交付状态）。 */
    public SavedBottleRemedyRecord withRecovery(String partyType, String partyId, Long amount, String status,
                                                String arApItemId, List<String> evidence, Instant at) {
        return new SavedBottleRemedyRecord(id, savedBottleId, merchantId, projectId, remedyType, replacementSavedBottleId,
                amountCent, lossCostCent, lossArApItemId, bearerType, bearerId, supervisorApprovedBy, supervisorApprovedAt,
                financeApprovedBy, financeApprovedAt, customerDeliveryStatus, deliveredAt, deliveryEvidenceIds, reason,
                partyType, partyId, amount, status, arApItemId, advanceApprovedBy, advanceApprovedAt, evidence,
                createdBy, createdAt, at);
    }
}
