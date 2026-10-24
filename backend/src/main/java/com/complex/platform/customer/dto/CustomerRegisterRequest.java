package com.complex.platform.customer.dto;

// 文件版本：V0.01

import java.time.LocalDate;

/**
 * ROOT-03-R3 会员注册（BSM 第 886 行「完成手机号短信验证、填写生日、满足平台年龄要求，并同意当前会员条款和隐私条款」）：
 * 两份条款版本须等于 {@code GET /api/v1/customer/terms} 返回的当前版本。
 */
public record CustomerRegisterRequest(
        String phone,
        String smsCode,
        LocalDate birthDate,
        String memberTermsVersion,
        String privacyTermsVersion
) {
}
