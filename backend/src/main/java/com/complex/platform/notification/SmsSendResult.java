package com.complex.platform.notification;

public record SmsSendResult(
        String status,
        String providerRef,
        String reason
) {
}
