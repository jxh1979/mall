package com.complex.platform.customer;

interface CustomerOtpVerifier {
    void verify(String phone, String smsCode, String scene);
}
