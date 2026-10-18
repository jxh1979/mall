package com.complex.platform.dealer.dto;

/** 门户自助提交注册资料（法人、联系人、联系电话）并送审。 */
public record DealerPortalProfileRequest(String idempotencyKey, String legalRepresentative, String contactName, String contactPhone) {
}
