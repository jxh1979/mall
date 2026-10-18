package com.complex.platform.dealer.dto;

public record DealerCreateRequest(String idempotencyKey, String dealerName, String unifiedSocialCreditCode, String legalRepresentative,
        String contactName, String contactPhone) {
}
