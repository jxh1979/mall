package com.complex.platform.governance.dto;
// 文件版本：V0.02

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
        String projectId
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
}
