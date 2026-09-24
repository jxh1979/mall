package com.complex.platform.governance.dto;
// 文件版本：V0.01

import java.util.List;

public record ProjectRequest(
        String idempotencyKey,
        String operatorNote,
        List<String> evidenceAttachmentIds,
        String merchantId,
        String venueType,
        String name,
        String code,
        String floor,
        Integer capacity,
        String status,
        String ownerPerson,
        String address,
        String businessScope
) {
    public ProjectRequest(String idempotencyKey, String operatorNote, List<String> evidenceAttachmentIds,
            String merchantId, String venueType, String name, String code, String floor, Integer capacity, String status) {
        this(idempotencyKey, operatorNote, evidenceAttachmentIds, merchantId, venueType, name, code, floor,
                capacity, status, null, null, null);
    }
}
