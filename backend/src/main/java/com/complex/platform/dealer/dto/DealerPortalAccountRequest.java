package com.complex.platform.dealer.dto;

/** 内部后台为经销商开通门户账号（企业联系人登录名与初始密码）。 */
public record DealerPortalAccountRequest(String idempotencyKey, String username, String password, String contactName) {
}
