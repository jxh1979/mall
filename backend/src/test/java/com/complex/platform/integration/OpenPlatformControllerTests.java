// 文件版本：V0.01
package com.complex.platform.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.config.SecurityConfig;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OpenPlatformController.class)
@Import({com.complex.platform.support.AuthAccessTestConfiguration.class, GlobalExceptionHandler.class, SecurityConfig.class, OpenPlatformControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class OpenPlatformControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IntegrationService service;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service);
    }

    @Test
    void openWebhookAcceptsServerToServerRequestAndDoesNotNeedStaffAuthentication() throws Exception {
        when(service.receiveWebhook(
                eq("p0-default"),
                eq("mock-provider"),
                eq("payment.paid"),
                eq("signature"),
                eq(1781424000000L),
                eq("nonce-controller"),
                eq("idem-controller"),
                any()
        )).thenReturn(new WebhookAcceptedResponse("evt-controller", false));

        mockMvc.perform(post("/api/open/d/p0-default/webhooks/mock-provider/payment.paid")
                        .header("X-Webhook-Signature", "signature")
                        .header("X-Open-Timestamp", "1781424000000")
                        .header("X-Open-Nonce", "nonce-controller")
                        .header("X-Idempotency-Key", "idem-controller")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "event_id": "evt-controller",
                                  "payload": {"status": "paid"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.event_id").value("evt-controller"))
                .andExpect(jsonPath("$.data.deduplicated").value(false));
    }

    @Test
    void openWebhookRejectsMissingIdempotencyKeyBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/open/d/p0-default/webhooks/mock-provider/payment.paid")
                        .header("X-Webhook-Signature", "signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "event_id": "evt-missing-idem"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));

        verifyNoInteractions(service);
    }

    @Test
    void openEventResultQueryUsesOpenAppSignatureContext() throws Exception {
        when(service.getOpenEventResult(eq("p0-default"), eq("evt-controller"), any()))
                .thenReturn(new OpenEventResultResponse("evt-controller", "accepted", Map.of("normalized_status", "paid")));

        mockMvc.perform(get("/api/open/d/p0-default/events/evt-controller")
                        .header("X-Open-App-Key", "p0-open-app")
                        .header("X-Open-Timestamp", "1781424000000")
                        .header("X-Open-Nonce", "nonce-query")
                        .header("X-Open-Signature", "signature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.event_id").value("evt-controller"))
                .andExpect(jsonPath("$.data.event_status").value("accepted"))
                .andExpect(jsonPath("$.data.result.normalized_status").value("paid"));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        IntegrationService integrationService() {
            return Mockito.mock(IntegrationService.class);
        }

        @Bean
        AuthService authService() {
            return new AuthService("", "");
        }
    }
}
