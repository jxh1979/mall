package com.complex.platform.governance.dto;
// 文件版本：V0.03

import java.math.BigDecimal;
import java.util.List;

public record MerchantProfileRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String merchantName,
        String merchantType,
        String businessEntityId,
        String contractEntityId,
        String settlementEntityId,
        String contactName,
        String contactPhone,
        String ownerPerson,
        String mallBlockCode,
        BigDecimal areaSquareMeter,
        BigDecimal leasedAreaSquareMeter,
        String secondaryContactName,
        String secondaryContactPhone,
        BigDecimal rentPerSquareMeter,
        String cooperationForm,
        String status,
        String projectId,
        // FS-024（ENUM L195）：业态为「其他」时必填的主管公司（在用的城市项目公司／域主管公司）与指定理由；其他业态不得填写。
        String supervisingCompanyId,
        String supervisingCompanyReason
) {
    public MerchantProfileRequest(
            String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String merchantName,
            String merchantType, String businessEntityId, String contractEntityId, String settlementEntityId,
            String contactName, String contactPhone, String ownerPerson, String mallBlockCode,
            BigDecimal areaSquareMeter, BigDecimal leasedAreaSquareMeter, String secondaryContactName,
            String secondaryContactPhone, BigDecimal rentPerSquareMeter, String cooperationForm, String status) {
        this(
                idempotencyKey, operatorNote, evidenceAttachmentIds, merchantName, merchantType, businessEntityId,
                contractEntityId, settlementEntityId, contactName, contactPhone, ownerPerson, mallBlockCode,
                areaSquareMeter, leasedAreaSquareMeter, secondaryContactName, secondaryContactPhone,
                rentPerSquareMeter, cooperationForm, status, null);
    }

    /** FS-024 前的 20 参构造点（既有代码/测试）：不带主管公司两项。 */
    public MerchantProfileRequest(
            String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds, String merchantName,
            String merchantType, String businessEntityId, String contractEntityId, String settlementEntityId,
            String contactName, String contactPhone, String ownerPerson, String mallBlockCode,
            BigDecimal areaSquareMeter, BigDecimal leasedAreaSquareMeter, String secondaryContactName,
            String secondaryContactPhone, BigDecimal rentPerSquareMeter, String cooperationForm, String status,
            String projectId) {
        this(
                idempotencyKey, operatorNote, evidenceAttachmentIds, merchantName, merchantType, businessEntityId,
                contractEntityId, settlementEntityId, contactName, contactPhone, ownerPerson, mallBlockCode,
                areaSquareMeter, leasedAreaSquareMeter, secondaryContactName, secondaryContactPhone,
                rentPerSquareMeter, cooperationForm, status, projectId, null, null);
    }
}
