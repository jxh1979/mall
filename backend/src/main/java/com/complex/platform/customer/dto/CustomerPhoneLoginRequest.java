package com.complex.platform.customer.dto;

public record CustomerPhoneLoginRequest(
        String phone,
        String smsCode,
        String scene
) {
}
