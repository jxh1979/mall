package com.complex.platform.merchantservice;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.config.SecurityConfig;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderAssignRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCloseRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCreateRequest;
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

@WebMvcTest(controllers = MerchantServiceController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, MerchantServiceControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class MerchantServiceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantServiceService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
    }

    @Test
    void unauthenticatedListReturnsUnauthorizedOrForbidden() throws Exception {
        int status = mockMvc.perform(get("/api/v1/merchant-service/work-orders"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(401, 403);
    }

    @Test
    void createWithoutIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/merchant-service/work-orders")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchant_id": "mch_1",
                                  "work_order_type": "repair",
                                  "priority": "high",
                                  "title": "Water leak",
                                  "description": "Shop ceiling is leaking",
                                  "reported_by_user_id": "staff_reporter",
                                  "reported_channel": "web"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createRequiresCreatePermissionAndReturnsWorkOrder() throws Exception {
        when(service.createWorkOrder(any(MerchantWorkOrderCreateRequest.class))).thenReturn(record("mswo_1", "created"));

        mockMvc.perform(post("/api/v1/merchant-service/work-orders")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "wo-create",
                                  "operator_note": "create work order",
                                  "evidence_attachment_ids": ["att-photo-1"],
                                  "merchant_id": "mch_1",
                                  "space_id": "sp_1",
                                  "contract_id": "ct_1",
                                  "work_order_type": "repair",
                                  "priority": "high",
                                  "title": "Water leak",
                                  "description": "Shop ceiling is leaking",
                                  "reported_by_user_id": "staff_reporter",
                                  "reported_channel": "web",
                                  "due_at": "2026-06-15T08:00:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("mswo_1"))
                .andExpect(jsonPath("$.data.status").value("created"));

        verify(authorizationService).requirePermission("merchant.service_work_order.create");
    }

    @Test
    void createPermissionDeniedReturnsForbidden() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("merchant.service_work_order.create");

        mockMvc.perform(post("/api/v1/merchant-service/work-orders")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "wo-denied",
                                  "merchant_id": "mch_1",
                                  "work_order_type": "repair",
                                  "priority": "high",
                                  "title": "Water leak",
                                  "description": "Shop ceiling is leaking",
                                  "reported_by_user_id": "staff_reporter",
                                  "reported_channel": "web"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void listUsesViewPermissionAndPageResponseShape() throws Exception {
        when(service.listWorkOrders()).thenReturn(new PageResponse<>(List.of(record("mswo_1", "created")), 1, 50, 1));

        mockMvc.perform(get("/api/v1/merchant-service/work-orders")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("mswo_1"))
                .andExpect(jsonPath("$.data.total").value(1));

        verify(authorizationService).requirePermission("merchant.service_work_order.view");
    }

    @Test
    void assignRequiresAssignPermission() throws Exception {
        when(service.assignWorkOrder(eq("mswo_1"), any(MerchantWorkOrderAssignRequest.class)))
                .thenReturn(record("mswo_1", "assigned"));

        mockMvc.perform(post("/api/v1/merchant-service/work-orders/mswo_1/assign")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "wo-assign",
                                  "operator_note": "assign",
                                  "owner_department": "property",
                                  "owner_user_id": "staff_owner",
                                  "operator_user_id": "staff_manager"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("assigned"));

        verify(authorizationService).requirePermission("merchant.service_work_order.assign");
    }

    @Test
    void closeRequiresClosePermissionAndReturnsClosedStatus() throws Exception {
        when(service.closeWorkOrder(eq("mswo_1"), any(MerchantWorkOrderCloseRequest.class)))
                .thenReturn(record("mswo_1", "closed"));

        mockMvc.perform(post("/api/v1/merchant-service/work-orders/mswo_1/close")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "wo-close",
                                  "operator_note": "close",
                                  "resolution": "Fixed and merchant confirmed",
                                  "satisfaction_score": 5,
                                  "operator_user_id": "staff_ops"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("closed"));

        verify(authorizationService).requirePermission("merchant.service_work_order.close");
    }

    private MerchantWorkOrderRecord record(String id, String status) {
        return new MerchantWorkOrderRecord(
                id,
                "mch_1",
                "sp_1",
                "ct_1",
                "repair",
                "high",
                status,
                "Water leak",
                "Shop ceiling is leaking",
                "staff_reporter",
                "web",
                "property",
                "staff_owner",
                Instant.parse("2026-06-15T08:00:00Z"),
                "closed".equals(status) ? Instant.parse("2026-06-15T10:00:00Z") : null,
                "closed".equals(status) ? "Fixed and merchant confirmed" : null,
                "closed".equals(status) ? 5 : null,
                List.of("att-photo-1"),
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T01:00:00Z")
        );
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        MerchantServiceService merchantServiceService() {
            return Mockito.mock(MerchantServiceService.class);
        }

        @Bean
        AuthorizationService authorizationService() {
            return Mockito.mock(AuthorizationService.class);
        }

        @Bean
        AuthService authService() {
            return new AuthService("", "");
        }
    }
}
