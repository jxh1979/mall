package com.complex.platform.partner.dto;

/** 机构管理员 自助开通下属账号。 */
public record PartnerSubordinateRequest(
        String idempotencyKey,
        String username
) {
}
