package com.complex.platform.governance.review;

// 文件版本：V0.05

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessService;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.dto.AccessGrantReviewRequest;
import com.complex.platform.access.staff.StaffHrRoutingService;
import com.complex.platform.common.api.PageResponse;
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
@Import({com.complex.platform.support.AuthAccessTestConfiguration.class, GlobalExceptionHandler.class, SecurityConfig.class, SupervisorReviewControllerTests.TestBeans.class})
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

    @Autowired
    private AccessService accessService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService, accessService);
        // M002-F40：通用复核入口先做不消耗来源的主管复核审批预检；切片里默认当前主体持有，拒绝路径由 requirePermission(s) 的 doThrow 表达。
        when(authorizationService.currentActorHasPermission("governance.supervisor_review.approve")).thenReturn(true);
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
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
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
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
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
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
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

    // M087-I5-01（PGM-1.10.1「审批通过后方可生效」、PGM-1.7.1「任一节点驳回则整单退回起草人」）：授权申请类复核单从审批链页裁决时，
    // 汇入授权申请的落地路径（reviewGrant），不在复核单侧单独判终态。
    @Test
    void approveOnAGrantRequestReviewDelegatesToTheGrantLandingPath() throws Exception {
        SupervisorReviewDecisionRecord pending = new SupervisorReviewDecisionRecord("review_grant", "permission_grant_l1", "access.scope_change",
                "access_grant_request", "agr_1", true, "pending_review", 1, null, null, "scope", List.of(),
                Instant.parse("2026-06-16T00:00:00Z"), "usr_hr", "one_level_up", null);
        SupervisorReviewDecisionRecord approved = new SupervisorReviewDecisionRecord("review_grant", "permission_grant_l1", "access.scope_change",
                "access_grant_request", "agr_1", false, "approved", 1, 1, "approved", "scope", List.of(),
                Instant.parse("2026-06-16T00:00:00Z"), "usr_hr", "one_level_up", null);
        when(service.findReview("review_grant", "governance.supervisor_review.approve")).thenReturn(java.util.Optional.of(pending), java.util.Optional.of(approved));

        mockMvc.perform(post("/api/v1/platform/supervisor-review/review_grant/approve")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "chain-approve-grant",
                                  "operator_note": "approve",
                                  "evidence_attachment_ids": ["att-approval-chain"],
                                  "decision": "approved",
                                  "comment": "ok"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("approved"));

        org.mockito.ArgumentCaptor<AccessGrantReviewRequest> captor = org.mockito.ArgumentCaptor.forClass(AccessGrantReviewRequest.class);
        verify(accessService).reviewGrant(eq("agr_1"), captor.capture());
        assertThat(captor.getValue().decision()).isEqualTo("approve");
        assertThat(captor.getValue().idempotencyKey()).isEqualTo("chain-approve-grant");
        verify(service, never()).approve(any(), any());
        // M002-F40：与授权申请页同一门禁——两码一次核验（同一单次来源只消耗一次），不再单查主管复核审批。
        verify(authorizationService).requirePermissions("access.grant_request.review", "governance.supervisor_review.approve");
        verify(authorizationService, never()).requirePermission("governance.supervisor_review.approve");
    }

    // M002-F40（治理地图第 655 行「显式拒绝优先级最高」）：授权申请类待审复核单经通用入口审批，缺「审核高危授权申请」或被显式拒绝（两码核验抛出）即 403，
    // 不进授权申请审批，也不落到通用复核。
    @Test
    void approveOnAGrantRequestReviewIsRefusedWithoutTheGrantReviewPermission() throws Exception {
        SupervisorReviewDecisionRecord pending = new SupervisorReviewDecisionRecord("review_grant", "permission_grant_l1", "access.scope_change",
                "access_grant_request", "agr_1", true, "pending_review", 1, null, null, "scope", List.of(),
                Instant.parse("2026-06-16T00:00:00Z"), "usr_hr", "one_level_up", null);
        when(service.findReview("review_grant", "governance.supervisor_review.approve")).thenReturn(java.util.Optional.of(pending));
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "没有操作权限：access.grant_request.review"))
                .when(authorizationService)
                .requirePermissions("access.grant_request.review", "governance.supervisor_review.approve");

        mockMvc.perform(post("/api/v1/platform/supervisor-review/review_grant/approve")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "chain-approve-denied", "decision": "rejected", "comment": "no"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        verify(accessService, never()).reviewGrant(any(), any());
        verify(service, never()).approve(any(), any());
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
        // M002-F40：非授权申请类复核单仍只查主管复核审批
        verify(authorizationService, never()).requirePermissions("access.grant_request.review", "governance.supervisor_review.approve");
    }

    // M007-A-02：待复核清单须 view 权限；转发须 approve 权限 + 幂等键。
    @Test
    void pendingListAndForwardEndpointsAreGated() throws Exception {
        when(service.listPending(anyInt())).thenReturn(new PageResponse<>(List.of(), 1, 200, 0));
        mockMvc.perform(get("/api/v1/platform/supervisor-review/pending").with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        verify(authorizationService).requirePermission("governance.supervisor_review.view");

        mockMvc.perform(post("/api/v1/platform/supervisor-review/review_1/forward")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"forwarded_to": "usr_2", "comment": "no key"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        verify(authorizationService).requirePermission("governance.supervisor_review.approve");
    }

    // R6 §1.3-4 契约：三个 op 各自的权限点缺失时均返回 403 PERMISSION_DENIED。
    // listPolicies→view、evaluate→manage 已分别在上方有专用用例；此处聚合三 op，
    // 重点补齐 approve→governance.supervisor_review.approve 的 403 契约缺口。
    @Test
    void supervisorReviewEndpointsRequireReviewPermission() throws Exception {
        // listPolicies 需要 view 权限
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("governance.supervisor_review.view");
        mockMvc.perform(get("/api/v1/platform/supervisor-review-policies")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));

        // evaluate 需要 manage 权限
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("governance.supervisor_review.manage");
        mockMvc.perform(post("/api/v1/platform/supervisor-review/evaluate")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "review-perm-evaluate",
                                  "action_type": "contract_violation",
                                  "object_type": "contract",
                                  "object_id": "contract_1"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));

        // approve 需要 approve 权限
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("governance.supervisor_review.approve");
        mockMvc.perform(post("/api/v1/platform/supervisor-review/review_1/approve")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "review-perm-approve",
                                  "decision": "approved",
                                  "comment": "checked"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));

        verify(authorizationService).requirePermission("governance.supervisor_review.view");
        verify(authorizationService).requirePermission("governance.supervisor_review.manage");
        verify(authorizationService).requirePermission("governance.supervisor_review.approve");
    }

    @TestConfiguration
    static class TestBeans {
        // 第十八批 N 段第 124 条 124-R5：控制器写口经 AuditedOperationExecutor.recordingFailure 落失败事件，切片里给真执行器（留痕服务为桩）
        @Bean
        com.complex.platform.evidence.AuditedOperationExecutor auditedOperationExecutor() {
            return new com.complex.platform.evidence.AuditedOperationExecutor(null, Mockito.mock(com.complex.platform.evidence.AuditEventService.class));
        }
        @Bean
        SupervisorReviewService supervisorReviewService() {
            return Mockito.mock(SupervisorReviewService.class);
        }

        @Bean
        StaffHrRoutingService staffHrRoutingService() {
            return Mockito.mock(StaffHrRoutingService.class);
        }

        // 18E 复核后操作员裁决补充第 16 条：服务按字段注入人事仓储（三类人事复核单按所处理员工的登记项目判可见），切片里给桩
        @Bean
        com.complex.platform.access.staff.JdbcStaffRepository jdbcStaffRepository() {
            return Mockito.mock(com.complex.platform.access.staff.JdbcStaffRepository.class);
        }

        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }

        @Bean
        AccessService accessService() {
            return Mockito.mock(AccessService.class);
        }

        // 18F（第 81 条）：AccessService 按字段注入平台仓储（挂商户时核商户经营项目），切片里给桩
        @Bean
        com.complex.platform.governance.PlatformRepository platformRepository() {
            return Mockito.mock(com.complex.platform.governance.PlatformRepository.class);
        }

        @Bean
        AuthService authService() {
            return Mockito.mock(AuthService.class);
        }
    }
}
