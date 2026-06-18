package com.complex.platform.governance.review;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.config.SecurityConfig;
import java.time.Instant;
import java.util.List;
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

@WebMvcTest(controllers = SupervisorReviewController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, SupervisorReviewControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class SupervisorReviewControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupervisorReviewService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
    }

    @Test
    void unauthenticatedListReturnsUnauthorizedOrForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/platform/supervisor-review-policies"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void listPoliciesRequiresViewPermission() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("governance.supervisor_review.view");

        mockMvc.perform(get("/api/v1/platform/supervisor-review-policies")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void evaluateWithoutIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/platform/supervisor-review/evaluate")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "action_type": "contract_violation",
                                  "object_type": "contract",
                                  "object_id": "contract_1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void evaluateRequiresManagePermission() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("governance.supervisor_review.manage");

        mockMvc.perform(post("/api/v1/platform/supervisor-review/evaluate")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "review-denied",
                                  "action_type": "contract_violation",
                                  "object_type": "contract",
                                  "object_id": "contract_1"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void evaluateReturnsBlockedDecision() throws Exception {
        when(service.evaluate(any(SupervisorReviewEvaluateRequest.class))).thenReturn(new SupervisorReviewDecisionRecord(
                "review_1",
                "contract_violation_l3",
                "contract_violation",
                "contract",
                "contract_1",
                true,
                "pending_review",
                3,
                null,
                "blocked",
                "contract mismatch",
                List.of("att_1"),
                Instant.parse("2026-06-16T00:00:00Z"),
                "usr_operator",
                "two_levels_up",
                null
        ));

        mockMvc.perform(post("/api/v1/platform/supervisor-review/evaluate")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "review-evaluate-ok",
                                  "operator_note": "evaluate",
                                  "evidence_attachment_ids": ["att_1"],
                                  "action_type": "contract_violation",
                                  "object_type": "contract",
                                  "object_id": "contract_1",
                                  "violation_summary": "contract mismatch"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.blocked").value(true))
                .andExpect(jsonPath("$.data.review_status").value("pending_review"));

        verify(authorizationService).requirePermission("governance.supervisor_review.manage");
    }

    @Test
    void approveReturnsApprovedDecision() throws Exception {
        when(service.approve(eq("review_1"), any(SupervisorReviewApprovalRequest.class))).thenReturn(new SupervisorReviewDecisionRecord(
                "review_1",
                "manual_supervisor_review",
                "supervisor_review",
                "business_operation",
                "review_1",
                false,
                "approved",
                3,
                3,
                "approved",
                "checked",
                List.of("att_2"),
                Instant.parse("2026-06-16T00:00:00Z"),
                "usr_operator",
                "two_levels_up",
                null
        ));

        mockMvc.perform(post("/api/v1/platform/supervisor-review/review_1/approve")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "review-approve-ok",
                                  "operator_note": "approve",
                                  "evidence_attachment_ids": ["att_2"],
                                  "reviewer_level": 3,
                                  "decision": "approved",
                                  "comment": "checked"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("approved"))
                .andExpect(jsonPath("$.data.reviewer_level").value(3));

        verify(authorizationService).requirePermission("governance.supervisor_review.approve");
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        SupervisorReviewService supervisorReviewService() {
            return Mockito.mock(SupervisorReviewService.class);
        }

        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }

        @Bean
        AuthService authService() {
            return Mockito.mock(AuthService.class);
        }
    }
}
