package com.complex.platform.customer;

import com.complex.platform.common.api.ApiException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
class FailClosedCustomerOtpVerifier implements CustomerOtpVerifier {

    @Override
    public void verify(String phone, String smsCode, String scene) {
        throw ApiException.unauthorized("CUSTOMER_OTP_PROVIDER_UNAVAILABLE", "短信服务未配置，请联系管理员");
    }
}
