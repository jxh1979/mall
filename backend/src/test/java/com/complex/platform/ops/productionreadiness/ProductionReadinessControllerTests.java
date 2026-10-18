// 文件版本：V0.01
package com.complex.platform.ops.productionreadiness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.auth.AuthService;
import com.complex.platform.config.SecurityConfig;
import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessCheckRequest;
import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessCheckResponse;
import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessItemResponse;
import com.complex.platform.ops.productionreadiness.dto.ProductionReadinessItemUpdateRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import org.springframework.beans.factory.annotation.Autowired;

@WebMvcTest(controllers = ProductionReadinessController.class)
@Import({com.complex.platform.support.AuthAccessTestConfiguration.class, GlobalExceptionHandler.class, SecurityConfig.class, ProductionReadinessControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class ProductionReadinessControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductionReadinessService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
    }

    @Test
    void unauthenticatedAccessReturnsUnauthorizedOrForbidden() throws Exception {
        var result = mockMvc.perform(get("/api/v1/ops/production-readiness/items"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isIn(401, 403);
    }

    @Test
    void listItemsReturnsApiResponseAndPageShape() throws Exception {
        when(service.listItems()).thenReturn(new PageResponse<>(
                List.of(itemResponse("mdm-device-enrollment", ProductionReadinessStatus.PENDING_INFO)),
                1,
                50,
                1
        ));

        mockMvc.perform(get("/api/v1/ops/production-readiness/items")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[0].id").value("mdm-device-enrollment"))
                .andExpect(jsonPath("$.data.items[0].status").value("pending_info"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void updateWithoutIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/ops/production-readiness/items/mdm-device-enrollment")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "passed",
                                  "owner_department": "IT",
                                  "owner_person": "ops-owner",
                                  "target_date": "2026-06-30",
                                  "blocking_scope": "none",
                                  "fallback_mode": "mock only"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void createCheckReturnsBlockerCount() throws Exception {
        when(service.createCheck(any(ProductionReadinessCheckRequest.class))).thenReturn(new ProductionReadinessCheckResponse(
                "chk-1",
                "all",
                "blocked",
                1,
                List.of(itemResponse("payment-channel-contract", ProductionReadinessStatus.BLOCKED)),
                Instant.parse("2026-06-13T00:00:00Z")
        ));

        mockMvc.perform(post("/api/v1/ops/production-readiness/checks")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "check-1",
                                  "check_scope": "all",
                                  "operator_note": "preflight"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("blocked"))
                .andExpect(jsonPath("$.data.blocker_count").value(1));
    }

    @Test
    void updateDelegatesSnakeCaseRequestToService() throws Exception {
        when(service.updateItem(eq("mdm-device-enrollment"), any(ProductionReadinessItemUpdateRequest.class)))
                .thenReturn(itemResponse("mdm-device-enrollment", ProductionReadinessStatus.PASSED));

        mockMvc.perform(put("/api/v1/ops/production-readiness/items/mdm-device-enrollment")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "update-1",
                                  "status": "passed",
                                  "owner_department": "IT",
                                  "owner_person": "ops-owner",
                                  "target_date": "2026-06-30",
                                  "blocking_scope": "none",
                                  "fallback_mode": "mock only",
                                  "credential_ref": "vault://complex/payment"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("passed"))
                .andExpect(jsonPath("$.data.credential_ref").value("vault://complex/payment"));
    }

    @Test
    void viewPermissionAllowsGetButNotUpdateOrCheck() throws Exception {
        when(service.listItems()).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("ops.production_readiness.manage");
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("ops.production_readiness.check");

        mockMvc.perform(get("/api/v1/ops/production-readiness/items")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/ops/production-readiness/items/mdm-device-enrollment")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("update-view-only")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        mockMvc.perform(post("/api/v1/ops/production-readiness/checks")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkBody("check-view-only")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    @Test
    void managePermissionAllowsUpdateButNotGetOrCheck() throws Exception {
        when(service.updateItem(eq("mdm-device-enrollment"), any(ProductionReadinessItemUpdateRequest.class)))
                .thenReturn(itemResponse("mdm-device-enrollment", ProductionReadinessStatus.PASSED));
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("ops.production_readiness.view");
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("ops.production_readiness.check");

        mockMvc.perform(put("/api/v1/ops/production-readiness/items/mdm-device-enrollment")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("update-manage-only")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/ops/production-readiness/items")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        mockMvc.perform(post("/api/v1/ops/production-readiness/checks")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkBody("check-manage-only")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    @Test
    void checkPermissionAllowsCheckButNotGetOrUpdate() throws Exception {
        when(service.createCheck(any(ProductionReadinessCheckRequest.class))).thenReturn(new ProductionReadinessCheckResponse(
                "chk-1",
                "all",
                "passed",
                0,
                List.of(),
                Instant.parse("2026-06-13T00:00:00Z")
        ));
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("ops.production_readiness.view");
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("ops.production_readiness.manage");

        mockMvc.perform(post("/api/v1/ops/production-readiness/checks")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkBody("check-check-only")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/ops/production-readiness/items")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        mockMvc.perform(put("/api/v1/ops/production-readiness/items/mdm-device-enrollment")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("update-check-only")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    private static ProductionReadinessItemResponse itemResponse(String id, ProductionReadinessStatus status) {
        return new ProductionReadinessItemResponse(
                id,
                "payment",
                "Payment contract",
                "Use mock until provider data is ready",
                "IT",
                "ops-owner",
                "supplier",
                status,
                LocalDate.parse("2026-06-30"),
                "runbook://evidence",
                Instant.parse("2026-06-13T00:00:00Z"),
                "scope",
                "fallback",
                "vault://complex/payment",
                "runbook://production-readiness",
                "finish verification",
                Instant.parse("2026-06-13T00:00:00Z"),
                "ops"
        );
    }

    private String updateBody(String idempotencyKey) {
        return """
                {
                  "idempotency_key": "%s",
                  "status": "passed",
                  "owner_department": "IT",
                  "owner_person": "ops-owner",
                  "target_date": "2026-06-30",
                  "blocking_scope": "none",
                  "fallback_mode": "mock only"
                }
                """.formatted(idempotencyKey);
    }

    private String checkBody(String idempotencyKey) {
        return """
                {
                  "idempotency_key": "%s",
                  "check_scope": "all",
                  "operator_note": "preflight"
                }
                """.formatted(idempotencyKey);
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        ProductionReadinessService productionReadinessService() {
            return Mockito.mock(ProductionReadinessService.class);
        }

        @Bean
        AuthService authService() {
            return new AuthService("", "");
        }

        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }
    }
}
