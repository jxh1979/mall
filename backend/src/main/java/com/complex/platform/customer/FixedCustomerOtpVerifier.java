package com.complex.platform.customer;

import com.complex.platform.common.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "dev", "test"})
class FixedCustomerOtpVerifier implements CustomerOtpVerifier {

    private final String expectedCode;

    FixedCustomerOtpVerifier(@Value("${complex.customer-auth.otp.local-code:000000}") String expectedCode) {
        this.expectedCode = expectedCode;
    }

    @Override
    public void verify(String phone, String smsCode, String scene) {
        if (!expectedCode.equals(smsCode)) {
            throw ApiException.unauthorized("CUSTOMER_OTP_INVALID", "customer sms code is invalid");
        }
    }
}
