package com.complex.platform.dealer.dto;

public record DealerCreateRequest(String idempotencyKey, String dealerName, String unifiedSocialCreditCode, String legalRepresentative,
        String contactName, String contactPhone,
        // 第十八批 G 段 82b-R5：所属项目（可选）；不带取本人项目
        String projectId) {
    public DealerCreateRequest(String idempotencyKey, String dealerName, String unifiedSocialCreditCode, String legalRepresentative,
            String contactName, String contactPhone) {
        this(idempotencyKey, dealerName, unifiedSocialCreditCode, legalRepresentative, contactName, contactPhone, null);
    }
}
