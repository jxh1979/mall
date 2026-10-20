package com.complex.platform.customer;

// 文件版本：V0.02

/** 客人短信验证码端口。FS-016 起改 public：存酒转让/取酒核验当前持有人短信（inventory 域直接注入本端口）。 */
public interface CustomerOtpVerifier {
    void verify(String phone, String smsCode, String scene);

    default void requestCode(String phone, String scene) {
        throw com.complex.platform.common.api.ApiException.conflict(
                "CUSTOMER_OTP_PROVIDER_UNAVAILABLE", "短信服务未配置，请联系管理员");
    }
}
