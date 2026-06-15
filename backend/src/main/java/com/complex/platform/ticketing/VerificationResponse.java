package com.complex.platform.ticketing;

public record VerificationResponse(
        String id,
        String verificationNo,
        String status,
        String resultReason
) {
}
