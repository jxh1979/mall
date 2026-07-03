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
    void missingRequiredQueryParameterIs400NotInternalError() {
        // 复现(request_id f5a9eddd):GET /api/v1/leasing/follow-ups 不带必填 ref_type/ref_id,
        // MissingServletRequestParameterException 此前无专属处理器 → 兜底 500;修复后=400 PARAM_INVALID。
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleMissingParameter(
                new org.springframework.web.bind.MissingServletRequestParameterException("ref_type", "String"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("PARAM_INVALID");
        assertThat(response.getBody().message()).contains("ref_type");
    }

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
