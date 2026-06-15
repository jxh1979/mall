package com.complex.platform.finance;

public record ArApPaymentResponse(
        ArApPaymentRecord payment,
        ArApItemRecord item
) {
}
