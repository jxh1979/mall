package com.complex.platform.access;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.config.SecurityConfig;
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

@WebMvcTest(controllers = AccessController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, AccessControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class AccessControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
    }

    @Test
    void unauthenticatedAccessEndpointReturnsUnauthorizedOrForbidden() throws Exception {
        int status = mockMvc.perform(get("/api/v1/access/permissions"))
                .andReturn()
                .getResponse()
                .getStatus();

        org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
    }

    @Test
    void createTemplateWithoutPermissionReturnsPermissionDenied() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("access.permission_template.manage");

        mockMvc.perform(post("/api/v1/access/permission-templates")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "template-denied",
                                  "template_name": "Ops template",
                                  "permission_codes": ["ops.production_readiness.view"]
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void listPermissionsReturnsApiResponsePageShape() throws Exception {
        when(service.listPermissions()).thenReturn(new PageResponse<>(
                List.of(new PermissionDefinition(
                        "ops.production_readiness.view",
                        "View production readiness",
                        "ops",
                        "medium",
                        false,
                        "active"
                )),
                1,
                50,
                1
        ));

        mockMvc.perform(get("/api/v1/access/permissions")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[0].permission_code").value("ops.production_readiness.view"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void createPermissionTemplateMissingIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/access/permission-templates")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "template_name": "Ops template",
                                  "permission_codes": ["ops.production_readiness.view"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createTemporaryGrantMissingValidUntilReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/access/temporary-grants")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "temporary-missing-validity",
                                  "target_user_id": "usr_1",
                                  "permission_codes": ["ops.production_readiness.check"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void listStaffLevelsReturnsApiResponsePageShape() throws Exception {
        when(service.listStaffLevels()).thenReturn(new PageResponse<>(
                List.of(new StaffLevelRecord(
                        "lvl_m1_lower", "M1", "lower", 1, "M1 薪资大范围", "active",
                        java.time.Instant.parse("2026-06-14T00:00:00Z"))),
                1, 50, 1));

        mockMvc.perform(get("/api/v1/access/staff-levels")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[0].id").value("lvl_m1_lower"))
                .andExpect(jsonPath("$.data.items[0].level_rank").value(1));
    }

    @Test
    void assignStaffLevelReturnsOkWithAssignment() throws Exception {
        when(service.assignStaffLevel(any())).thenReturn(new StaffLevelAssignment(
                "usr_emp1", "lvl_m1_lower", 100000L, "CNY", "月薪", "org", "merchant",
                "access-api", java.time.Instant.parse("2026-06-14T00:00:00Z")));

        mockMvc.perform(put("/api/v1/access/users/usr_emp1/staff-level")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "assign-1",
                                  "staff_level_id": "lvl_m1_lower",
                                  "base_salary_amount_cent": 100000,
                                  "salary_currency": "CNY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.staff_level_id").value("lvl_m1_lower"))
                .andExpect(jsonPath("$.data.salary_currency").value("CNY"));
    }

    @Test
    void assignStaffLevelMissingIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/access/users/usr_emp1/staff-level")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "staff_level_id": "lvl_m1_lower"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void effectivePermissionsSelfReturnsGrantsDeniesAndWildcard() throws Exception {
        when(authorizationService.effectivePermissions()).thenReturn(new EffectivePermissions(
                "usr_self",
                List.of("ticket.order.view", "bar.table.open_close"),
                List.of("finance.refund.create"),
                false,
                java.time.Instant.parse("2026-06-18T08:30:00Z")
        ));

        mockMvc.perform(get("/api/v1/access/effective-permissions")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.user_id").value("usr_self"))
                .andExpect(jsonPath("$.data.grants[0]").value("ticket.order.view"))
                .andExpect(jsonPath("$.data.denies[0]").value("finance.refund.create"))
                .andExpect(jsonPath("$.data.wildcard").value(false))
                .andExpect(jsonPath("$.data.computed_at").value("2026-06-18T08:30:00Z"));
    }

    @Test
    void userEffectivePermissionsWithoutUserViewPermissionReturnsPermissionDenied() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("access.user.view");

        mockMvc.perform(get("/api/v1/access/users/usr_target/effective-permissions")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void userEffectivePermissionsReturnsTargetUserSetWithUserViewPermission() throws Exception {
        when(authorizationService.effectivePermissionsForUser("usr_target")).thenReturn(new EffectivePermissions(
                "usr_target",
                List.of("amusement.project.view"),
                List.of(),
                true,
                java.time.Instant.parse("2026-06-18T09:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/access/users/usr_target/effective-permissions")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.user_id").value("usr_target"))
                .andExpect(jsonPath("$.data.grants[0]").value("amusement.project.view"))
                .andExpect(jsonPath("$.data.wildcard").value(true));
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        AccessService accessService() {
            return Mockito.mock(AccessService.class);
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
