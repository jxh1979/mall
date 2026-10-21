package com.complex.platform.customer;

// 文件版本：V0.01

import com.complex.platform.common.api.ApiException;

/** 测试替身：短信服务未配置的验证码端口（ROOT-05-R3 起生产由 SmsCustomerOtpService 接管，本类只供测试构造「未配置」场景）。 */
class FailClosedCustomerOtpVerifier implements CustomerOtpVerifier {

    @Override
    public void verify(String phone, String smsCode, String scene) {
        throw ApiException.unauthorized("CUSTOMER_OTP_PROVIDER_UNAVAILABLE", "短信服务未配置，请联系管理员");
    }
}
