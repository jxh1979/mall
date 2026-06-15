package com.complex.platform.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTests {

    @Test
    void unexpectedExceptionsKeepGenericResponseAndLogRootCause(CapturedOutput output) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleUnexpected(new IllegalStateException("jdbc root cause"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("internal server error");
        assertThat(output).contains("Unhandled API exception")
                .contains("jdbc root cause");
    }
}
