package com.complex.platform.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiRequestsTests {

    @Test
    void nullIdempotencyKeyIsRejectedWithValidationContract() {
        assertThatThrownBy(() -> ApiRequests.requireIdempotencyKey(null))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.status()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(exception.getMessage()).isEqualTo("idempotency_key is required");
                });
    }

    @Test
    void blankIdempotencyKeyIsRejected() {
        assertThatThrownBy(() -> ApiRequests.requireIdempotencyKey("   "))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void nonBlankIdempotencyKeyPasses() {
        assertThatCode(() -> ApiRequests.requireIdempotencyKey("idem-123"))
                .doesNotThrowAnyException();
    }
}
