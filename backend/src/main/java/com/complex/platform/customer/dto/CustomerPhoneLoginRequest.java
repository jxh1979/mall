package com.complex.platform.customer.dto;

// 文件版本：V0.01

/** 手机号短信验证码登录。登录端取本请求可信终端（ROOT-03-R2），不收请求体。 */
public record CustomerPhoneLoginRequest(
        String phone,
        String smsCode
) {
}
