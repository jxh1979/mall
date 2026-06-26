package com.complex.platform.governance;

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
import com.complex.platform.governance.dto.BusinessEntityBindingRequest;
import com.complex.platform.governance.dto.SpaceRequest;
import com.complex.platform.governance.dto.StatusChangeRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
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

@WebMvcTest(controllers = PlatformController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, PlatformControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class PlatformControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PlatformService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
    }

    @Test
    void unauthenticatedPlatformListReturnsUnauthorizedOrForbidden() throws Exception {
        int status = mockMvc.perform(get("/api/v1/platform/merchants"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(status).isIn(401, 403);
    }

    @Test
    void creatingMerchantWithoutManagePermissionReturnsPermissionDenied() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("merchant.profile.manage");

        mockMvc.perform(post("/api/v1/platform/merchants")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "merchant-denied",
                                  "merchant_name": "P0 Merchant",
                                  "merchant_type": "other"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    @Test
    void creatingMerchantWithoutIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/platform/merchants")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchant_name": "P0 Merchant",
                                  "merchant_type": "other"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    @Test
    void listingMerchantsReturnsApiResponsePageShape() throws Exception {
        when(service.listMerchants()).thenReturn(new PageResponse<>(
                List.of(new MerchantRecord(
                        "mch_1",
                        "P0 Merchant",
                        "other",
                        "be_1",
                        null,
                        null,
                        "Jane",
                        "18800000011",
                        "Owner Wang",
                        "1F-A",
                        BigDecimal.valueOf(128.50),
                        BigDecimal.valueOf(120.00),
                        "Jack",
                        "18800000013",
                        BigDecimal.valueOf(12.30),
                        "fixed_rent_plus_commission",
                        "active",
                        Instant.parse("2026-06-14T00:00:00Z"),
                        Instant.parse("2026-06-14T00:00:00Z")
                )),
                1,
                50,
                1
        ));

        mockMvc.perform(get("/api/v1/platform/merchants")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[0].id").value("mch_1"))
                .andExpect(jsonPath("$.data.items[0].merchant_name").value("P0 Merchant"))
                .andExpect(jsonPath("$.data.items[0].mall_block_code").value("1F-A"))
                .andExpect(jsonPath("$.data.items[0].area_square_meter").value(128.50))
                .andExpect(jsonPath("$.data.items[0].leased_area_square_meter").value(120.00))
                .andExpect(jsonPath("$.data.items[0].secondary_contact_name").value("Jack"))
                .andExpect(jsonPath("$.data.items[0].secondary_contact_phone").value("18800000013"))
                .andExpect(jsonPath("$.data.items[0].rent_per_square_meter").value(12.30))
                .andExpect(jsonPath("$.data.items[0].cooperation_form").value("fixed_rent_plus_commission"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void creatingSpaceUsesGovernanceSpaceManagePermission() throws Exception {
        when(service.createSpace(any(SpaceRequest.class))).thenReturn(new SpaceRecord(
                "sp_1",
                "prj_1",
                "mch_1",
                null,
                "P0 Shop",
                "shop",
                "1F",
                null,
                List.of(),
                "active",
                Instant.parse("2026-06-14T00:00:00Z"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));

        mockMvc.perform(post("/api/v1/platform/spaces")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "space-create",
                                  "space_name": "P0 Shop",
                                  "space_type": "shop"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("sp_1"));

        verify(authorizationService).requirePermission("governance.space.manage");
    }

    @Test
    void listBusinessEntityBindingsUsesEntityBindingViewPermission() throws Exception {
        when(service.listBusinessEntityBindings()).thenReturn(new PageResponse<>(
                List.of(new BusinessEntityBindingRecord(
                        "beb_1",
                        "mch_1",
                        "be_operation",
                        "be_contract",
                        "be_settlement",
                        "active",
                        LocalDate.parse("2026-06-14"),
                        null,
                        "initial",
                        Instant.parse("2026-06-14T00:00:00Z"),
                        Instant.parse("2026-06-14T00:00:00Z")
                )),
                1,
                50,
                1
        ));

        mockMvc.perform(get("/api/v1/platform/business-entity-bindings")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("beb_1"))
                .andExpect(jsonPath("$.data.items[0].merchant_id").value("mch_1"));

        verify(authorizationService).requirePermission("entity.binding.view");
    }

    @Test
    void createBusinessEntityBindingRequiresIdempotencyAndManagePermission() throws Exception {
        when(service.createBusinessEntityBinding(any(BusinessEntityBindingRequest.class)))
                .thenReturn(new BusinessEntityBindingRecord(
                        "beb_created",
                        "mch_1",
                        "be_operation",
                        "be_contract",
                        "be_settlement",
                        "active",
                        LocalDate.parse("2026-06-14"),
                        null,
                        "initial",
                        Instant.parse("2026-06-14T00:00:00Z"),
                        Instant.parse("2026-06-14T00:00:00Z")
                ));

        mockMvc.perform(post("/api/v1/platform/business-entity-bindings")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "binding-create",
                                  "merchant_id": "mch_1",
                                  "operation_entity_id": "be_operation",
                                  "contract_entity_id": "be_contract",
                                  "settlement_entity_id": "be_settlement",
                                  "effective_from": "2026-06-14",
                                  "change_reason": "initial"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("beb_created"));

        verify(authorizationService).requirePermission("entity.binding.manage");
    }

    @Test
    void changeBusinessEntityBindingRequiresIdempotencyAndManagePermission() throws Exception {
        when(service.changeBusinessEntityBinding(eq("beb_1"), any(StatusChangeRequest.class)))
                .thenReturn(new BusinessEntityBindingRecord(
                        "beb_1",
                        "mch_1",
                        "be_operation",
                        "be_contract",
                        "be_settlement",
                        "inactive",
                        LocalDate.parse("2026-06-14"),
                        null,
                        "contract changed",
                        Instant.parse("2026-06-14T00:00:00Z"),
                        Instant.parse("2026-06-14T01:00:00Z")
                ));

        mockMvc.perform(post("/api/v1/platform/business-entity-bindings/beb_1/change")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "binding-change",
                                  "action": "inactive",
                                  "reason": "contract changed"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("inactive"));

        verify(authorizationService).requirePermission("entity.binding.manage");
    }


    @TestConfiguration
    static class TestBeans {
        @Bean
        PlatformService platformService() {
            return Mockito.mock(PlatformService.class);
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
