package com.complex.platform.customer;

// 文件版本：V0.01

interface CustomerOtpVerifier {
    void verify(String phone, String smsCode, String scene);

    default void requestCode(String phone, String scene) {
        throw com.complex.platform.common.api.ApiException.conflict(
                "CUSTOMER_OTP_PROVIDER_UNAVAILABLE", "短信服务未配置，请联系管理员");
    }
}
