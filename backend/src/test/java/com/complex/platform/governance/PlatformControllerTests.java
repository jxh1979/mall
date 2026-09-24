package com.complex.platform.governance;
// 文件版本：V0.04

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.complex.platform.governance.dto.MerchantArchiveRequest;
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

    // M011-002：mock 的合作方式取 HTML 六类之一（BEQR-148 lease），不再把已退役收费模板码当 API 形状。
    @Test
    void listingMerchantsReturnsApiResponsePageShape() throws Exception {
        when(service.listMerchants(eq(false), any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(
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
                        "lease",
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
                // P0-A/GAP-07（V339）：无 pii.contact_phone.view 的读端两个联系电话都只见掩码。
                .andExpect(jsonPath("$.data.items[0].contact_phone").value("188****0011"))
                .andExpect(jsonPath("$.data.items[0].secondary_contact_phone").value("188****0013"))
                .andExpect(jsonPath("$.data.items[0].rent_per_square_meter").value(12.30))
                .andExpect(jsonPath("$.data.items[0].cooperation_form").value("lease"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void listingMerchantsReturnsPlaintextPhoneWithPiiPermission() throws Exception {
        // P0-A/GAP-07（V339）：有 pii.contact_phone.view 的读端才见联系电话明文。
        when(authorizationService.currentActorHasPermission("pii.contact_phone.view")).thenReturn(true);
        when(service.listMerchants(eq(false), any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(
                List.of(new MerchantRecord(
                        "mch_1", "P0 Merchant", "other", "be_1", null, null, "Jane", "18800000011",
                        "Owner Wang", "1F-A", BigDecimal.valueOf(128.50), BigDecimal.valueOf(120.00),
                        "Jack", "18800000013", BigDecimal.valueOf(12.30), "lease", "active",
                        Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")
                )), 1, 50, 1));

        mockMvc.perform(get("/api/v1/platform/merchants")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].contact_phone").value("18800000011"))
                .andExpect(jsonPath("$.data.items[0].secondary_contact_phone").value("18800000013"));
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

    // M011-003：存档/删除 HTTP 面（BSM-004）：存档须 merchant.profile.manage + 幂等键；缺键 400 且不进服务层。
    @Test
    void archivingMerchantRequiresManagePermissionAndIdempotencyKey() throws Exception {
        when(service.archiveMerchant(eq("mch_1"), any(MerchantArchiveRequest.class)))
                .thenReturn(new MerchantRecord(
                        "mch_1", "P0 Merchant", "other", "be_1", null, null, "Jane", "18800000011",
                        "Owner Wang", "1F-A", BigDecimal.valueOf(128.50), BigDecimal.valueOf(120.00),
                        "Jack", "18800000013", BigDecimal.valueOf(12.30), "lease", "active",
                        Instant.parse("2026-06-14T00:00:00Z"), Instant.parse("2026-06-14T00:00:00Z")
                ).withArchive(Instant.parse("2026-06-15T00:00:00Z"), "ops", Instant.parse("2026-06-15T00:00:00Z")));

        mockMvc.perform(post("/api/v1/platform/merchants/mch_1/archive")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "archive-1",
                                  "reason": "退场清理"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("mch_1"))
                .andExpect(jsonPath("$.data.archived_at").isNotEmpty());
        verify(authorizationService).requirePermission("merchant.profile.manage");

        mockMvc.perform(post("/api/v1/platform/merchants/mch_1/archive")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "缺幂等键"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        verify(service, Mockito.times(1)).archiveMerchant(eq("mch_1"), any(MerchantArchiveRequest.class));
    }

    @Test
    void listBusinessEntityBindingsUsesEntityBindingViewPermission() throws Exception {
        when(service.listBusinessEntityBindings(anyInt(), anyInt())).thenReturn(new PageResponse<>(
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
        verify(authorizationService).requirePlatformScope(); // M009-015：读端与写端同法 fail-closed
    }

    // M009-015：治理读端必须带平台作用域守卫（BSM-072/BSM-074/PGM-027），不能只靠权限码。
    @Test
    void listingMerchantsRequiresPlatformScope() throws Exception {
        doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "商户账号无权查看平台级经营分析"))
                .when(authorizationService)
                .requirePlatformScope();

        mockMvc.perform(get("/api/v1/platform/merchants")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"));
    }

    // M011-004：其余五个治理读端同法 fail-closed（BSM-072/BSM-074/PGM-027）；任一读端退回只 requirePermission 即红。
    @Test
    void otherGovernanceReadEndpointsRequirePlatformScope() throws Exception {
        doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "商户账号无权查看平台级经营分析"))
                .when(authorizationService)
                .requirePlatformScope();

        for (String path : List.of("/api/v1/platform/business-entities", "/api/v1/platform/projects",
                "/api/v1/platform/spaces", "/api/v1/platform/space-types", "/api/v1/platform/venue-business-hours")) {
            mockMvc.perform(get(path)
                            .with(user("ops").roles("P0_SMOKE")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"));
        }
    }

    @Test
    // M011-005：方法名与断言一致——本用例只验证 entity.binding.manage 权限码；缺幂等键的共享闸见 creatingMerchantWithoutIdempotencyKeyReturnsValidationFailed。
    void createBusinessEntityBindingRequiresManagePermission() throws Exception {
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
    void changeBusinessEntityBindingRequiresManagePermission() throws Exception {
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



    @Test
    void projectAssociationEndpointsConsumeEachFunctionalPermissionOnce() throws Exception {
        MerchantRecord merchant = new MerchantRecord("mch_project", "项目商户", "other", null, null, null,
                null, "18800000001", null, null, null, null, null, null, null, null, "active",
                Instant.EPOCH, Instant.EPOCH).withProject("project_a");
        when(service.assignMerchantProject(eq(merchant.id()), any())).thenReturn(merchant);
        when(service.associateBusinessEntityProject(eq("entity_project"), any()))
                .thenReturn(new BusinessEntityRecord("entity_project", "公司", "management_company",
                        null, null, null, "active", Instant.EPOCH, Instant.EPOCH));
        String request = """
                {"idempotency_key":"project-association","project_id":"project_a","reason":"核实原始资料"}
                """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/v1/platform/merchants/mch_project/project")
                        .with(user("ops").roles("P0_SMOKE")).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value("project_a"))
                .andExpect(jsonPath("$.data.contact_phone").value("188****0001"));
        mockMvc.perform(post("/api/v1/platform/business-entities/entity_project/projects")
                        .with(user("ops").roles("P0_SMOKE")).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value("entity_project"));
        verify(authorizationService, Mockito.times(1)).requirePermission("merchant.profile.manage");
        verify(authorizationService, Mockito.times(1)).requirePermission("entity.binding.manage");
        var expected = new com.complex.platform.governance.dto.ProjectAssociationRequest(
                "project-association", "project_a", "核实原始资料");
        verify(service).assignMerchantProject("mch_project", expected);
        verify(service).associateBusinessEntityProject("entity_project", expected);
    }

    @Test
    void associationEndpointRejectsMissingKeyAndDeniedPermissionBeforeService() throws Exception {
        mockMvc.perform(post("/api/v1/platform/business-entities/entity_project/projects")
                        .with(user("ops").roles("P0_SMOKE")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"project_id\":\"project_a\",\"reason\":\"核实\"}"))
                .andExpect(status().isBadRequest());
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "无权限"))
                .when(authorizationService).requirePermission("merchant.profile.manage");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/v1/platform/merchants/mch_project/project")
                        .with(user("ops").roles("P0_SMOKE")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"denied\",\"project_id\":\"project_a\",\"reason\":\"核实\"}"))
                .andExpect(status().isForbidden());
        Mockito.verifyNoInteractions(service);
    }


    // M009-058：真实授权服务；单次许可只供一次响应，同组功能权限不能先把电话权限耗掉。
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void merchantPhoneSingleUseAppliesToOneResponse(boolean writeResponse, boolean sameGrant) {
        var repository = Mockito.mock(com.complex.platform.access.AccessRepository.class);
        var consumed = new java.util.concurrent.atomic.AtomicBoolean();
        String permission = writeResponse ? "merchant.profile.manage" : "merchant.profile.view";
        when(repository.permissionExists(any())).thenReturn(true);
        when(repository.findPermissionEffects(eq("phone_once"), any())).thenAnswer(call -> List.of(
                new com.complex.platform.access.AccessRepository.PermissionEffect("phone_once", permission,
                        com.complex.platform.access.AccessRepository.Effect.GRANT,
                        sameGrant ? com.complex.platform.access.AccessRepository.Source.TEMPORARY_GRANT
                                : com.complex.platform.access.AccessRepository.Source.ROLE,
                        sameGrant ? "phone_once_grant" : "profile_role", null, null,
                        sameGrant, sameGrant && consumed.get()),
                new com.complex.platform.access.AccessRepository.PermissionEffect("phone_once", "pii.contact_phone.view",
                        com.complex.platform.access.AccessRepository.Effect.GRANT,
                        com.complex.platform.access.AccessRepository.Source.TEMPORARY_GRANT,
                        "phone_once_grant", null, null, true, consumed.get())));
        when(repository.markSingleUsePermissionConsumed(eq("phone_once_grant"), any(), eq("phone_once"), any()))
                .thenAnswer(call -> consumed.compareAndSet(false, true));
        var authorization = new AuthorizationService(repository);
        var controller = new PlatformController(service, authorization, Mockito.mock(VenueBusinessHoursService.class));
        var merchant = new MerchantRecord("mch_once", "商户", "other", null, null, null,
                "甲", "18800000011", null, null, null, null, "乙", "18800000013",
                null, null, "active", Instant.EPOCH, Instant.EPOCH);
        when(service.listMerchants(false, null, 1, 50))
                .thenReturn(new PageResponse<>(List.of(merchant, merchant), 1, 50, 2));
        when(service.archiveMerchant(eq(merchant.id()), any())).thenReturn(merchant);
        var previous = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        new com.complex.platform.access.AccessActor("phone_once", "phone_once", "staff",
                                "project_a", null, "active"), "token", List.of()));
        java.util.function.Supplier<List<MerchantRecord>> request = () -> writeResponse
                ? List.of(controller.archiveMerchant(merchant.id(),
                        new MerchantArchiveRequest("phone-once", "保存测试")).data())
                : controller.listMerchants(false, null, 1, 50).data().items();
        try {
            assertThat(request.get()).allSatisfy(record -> {
                assertThat(record.contactPhone()).isEqualTo("18800000011");
                assertThat(record.secondaryContactPhone()).isEqualTo("18800000013");
            });
            assertThat(consumed.get()).isTrue();
            if (sameGrant) {
                org.assertj.core.api.Assertions.assertThatThrownBy(request::get)
                        .isInstanceOfSatisfying(ApiException.class,
                                error -> assertThat(error.code()).isEqualTo("PERMISSION_DENIED"));
            } else {
                assertThat(request.get()).allSatisfy(record -> {
                    assertThat(record.contactPhone()).isEqualTo("188****0011");
                    assertThat(record.secondaryContactPhone()).isEqualTo("188****0013");
                });
            }
            verify(repository, Mockito.times(1)).markSingleUsePermissionConsumed(
                    eq("phone_once_grant"), any(), eq("phone_once"), any());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

    @Test
    void failedPhonePermissionConsumptionDoesNotReturnPlaintext() throws Exception {
        when(authorizationService.currentActorHasPermission("pii.contact_phone.view")).thenReturn(true);
        when(service.listMerchants(eq(false), any(), anyInt(), anyInt())).thenReturn(new PageResponse<>(
                List.of(new MerchantRecord("mch_race", "商户", "other", null, null, null,
                        "甲", "18800000011", null, null, null, null, "乙", "18800000013",
                        null, null, "active", Instant.EPOCH, Instant.EPOCH)), 1, 50, 1));
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "单次授权已被使用"))
                .when(authorizationService).requirePermissions("merchant.profile.view", "pii.contact_phone.view");

        mockMvc.perform(get("/api/v1/platform/merchants").with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        Mockito.verifyNoInteractions(service);
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        PlatformService platformService() {
            return Mockito.mock(PlatformService.class);
        }

        // D1：PlatformController 现在还注入场地营业时间服务（本切片测试不测它，给个 mock 让上下文起得来）。
        @Bean
        VenueBusinessHoursService venueBusinessHoursService() {
            return Mockito.mock(VenueBusinessHoursService.class);
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
