package com.complex.platform.terminal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.auth.AuthService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.GlobalExceptionHandler;
import com.complex.platform.config.SecurityConfig;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.terminal.dto.CapabilitySwitchRequest;
import com.complex.platform.terminal.dto.DeviceBindRequest;
import com.complex.platform.terminal.dto.DeviceReturnRequest;
import com.complex.platform.terminal.dto.DeviceUnbindRequest;
import com.complex.platform.terminal.dto.KioskHeartbeatRequest;
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

@WebMvcTest(controllers = TerminalDeviceController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, TerminalDeviceControllerTests.TestBeans.class})
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class
})
class TerminalDeviceControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TerminalDeviceService service;

    @Autowired
    private AuthorizationService authorizationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service, authorizationService);
    }

    @Test
    void unauthenticatedBindIsRejected() throws Exception {
        int status = mockMvc.perform(post("/api/v1/terminals/devices/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bindBody("bind-unauthenticated")))
                .andReturn()
                .getResponse()
                .getStatus();

        org.assertj.core.api.Assertions.assertThat(status).isIn(401, 403);
    }

    @Test
    void bindRequiresTerminalDeviceBindPermission() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("terminal.device.bind");

        mockMvc.perform(post("/api/v1/terminals/devices/bind")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bindBody("bind-denied")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    @Test
    void bindMissingIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/terminals/devices/bind")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "device_id": "P0-ANDROID-1",
                                  "terminal_type": "staff_work_phone",
                                  "binding_scope": "space",
                                  "bound_space_id": "point_1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("idempotency_key is required"));
    }

    @Test
    void returnMissingIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(post("/api/v1/terminals/devices/return")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "device_code": "P0-ANDROID-1",
                                  "has_pending_tasks": false,
                                  "staff_id": "staff_1",
                                  "reason": "shift closed"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("idempotency_key is required"));
    }

    @Test
    void returnWithPendingTasksReturnsOpenApiConflictCode() throws Exception {
        when(service.returnDevice(any(DeviceReturnRequest.class)))
                .thenThrow(ApiException.conflict("RETURN_BLOCKED_PENDING_TASKS", "device has pending tasks"));

        mockMvc.perform(post("/api/v1/terminals/devices/return")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "return-pending",
                                  "device_code": "P0-ANDROID-1",
                                  "has_pending_tasks": true,
                                  "staff_id": "staff_1",
                                  "reason": "pending offline tasks"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("RETURN_BLOCKED_PENDING_TASKS"));

        verify(authorizationService).requirePermission("terminal.device.bind");
    }

    @Test
    void unbindUsesTerminalDeviceUnbindPermission() throws Exception {
        when(service.unbindDevice(eq("dev_1"), any(DeviceUnbindRequest.class)))
                .thenReturn(new DeviceBindingResponse(
                        "dev_1",
                        "staff_work_phone",
                        "P0-ANDROID-1",
                        null,
                        null,
                        "point_1",
                        null,
                        null,
                        null,
                        "point_1",
                        null,
                        "unbound",
                        Instant.parse("2026-06-14T00:00:00Z"),
                        null
                ));

        mockMvc.perform(post("/api/v1/terminals/devices/dev_1/unbind")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "unbind-device",
                                  "reason": "device replacement"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("unbound"));

        verify(authorizationService).requirePermission("terminal.device.unbind");
    }

    @Test
    void listCapabilitySwitchesUsesTerminalSwitchViewPermission() throws Exception {
        when(service.listCapabilitySwitches()).thenReturn(new PageResponse<>(
                List.of(new TerminalCapabilitySwitchRecord(
                        "switch_1",
                        "kiosk",
                        "ticketing",
                        "device",
                        "device_ticketing_1",
                        true,
                        "enabled",
                        Instant.parse("2026-06-14T00:00:00Z"),
                        Instant.parse("2026-06-14T00:00:00Z")
                )),
                1,
                50,
                1
        ));

        mockMvc.perform(get("/api/v1/terminals/capability-switches")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("switch_1"))
                .andExpect(jsonPath("$.data.items[0].enabled").value(true));

        verify(authorizationService).requirePermission("terminal.switch.view");
    }

    @Test
    void updateCapabilitySwitchRequiresIdempotencyAndManagePermission() throws Exception {
        when(service.updateCapabilitySwitch(eq("switch_1"), any(CapabilitySwitchRequest.class)))
                .thenReturn(new TerminalCapabilitySwitchRecord(
                        "switch_1",
                        "kiosk",
                        "ticketing",
                        "device",
                        "device_ticketing_1",
                        false,
                        "disabled for maintenance",
                        Instant.parse("2026-06-14T00:00:00Z"),
                        Instant.parse("2026-06-14T01:00:00Z")
                ));

        mockMvc.perform(put("/api/v1/terminals/capability-switches/switch_1")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "switch-update",
                                  "enabled": false,
                                  "terminal_type": "kiosk",
                                  "capability_code": "ticketing",
                                  "scope_id": "device_ticketing_1",
                                  "operator_note": "disabled for maintenance"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        verify(authorizationService).requirePermission("terminal.switch.manage");
    }

    @Test
    void kioskConfigUsesDeviceTokenWithoutStaffPermission() throws Exception {
        when(service.getKioskConfig("device_ticketing_1", "token_ticketing_1")).thenReturn(new KioskConfigResponse(
                "device_ticketing_1",
                "ticketing",
                true,
                "project:p0",
                Map.of("home", "ticketing"),
                Instant.parse("2026-06-14T00:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/terminals/kiosks/device_ticketing_1/config")
                        .header("X-Device-Token", "token_ticketing_1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketing_enabled").value(true));

        verify(service).getKioskConfig("device_ticketing_1", "token_ticketing_1");
        verifyNoInteractions(authorizationService);
    }

    @Test
    void kioskConfigRejectsMissingDeviceTokenThroughService() throws Exception {
        when(service.getKioskConfig("device_ticketing_1", null))
                .thenThrow(ApiException.unauthorized("DEVICE_CREDENTIAL_REQUIRED", "X-Device-Token is required"));

        mockMvc.perform(get("/api/v1/terminals/kiosks/device_ticketing_1/config"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_CREDENTIAL_REQUIRED"));

        verify(service).getKioskConfig("device_ticketing_1", null);
        verifyNoInteractions(authorizationService);
    }

    @Test
    void kioskConfigPropagatesInvalidDeviceTokenAsForbidden() throws Exception {
        when(service.getKioskConfig("device_ticketing_1", "wrong-token"))
                .thenThrow(ApiException.forbidden("DEVICE_CREDENTIAL_INVALID", "device token is invalid"));

        mockMvc.perform(get("/api/v1/terminals/kiosks/device_ticketing_1/config")
                        .header("X-Device-Token", "wrong-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_CREDENTIAL_INVALID"));

        verifyNoInteractions(authorizationService);
    }

    @Test
    void kioskHeartbeatUsesDeviceTokenWithoutStaffPermission() throws Exception {
        when(service.recordKioskHeartbeat(eq("device_ticketing_1"), eq("token_ticketing_1"), any(KioskHeartbeatRequest.class)))
                .thenReturn(new KioskHeartbeatRecord(
                        "device_ticketing_1",
                        "online",
                        "1.0.0",
                        List.of("ticketing"),
                        null,
                        "ticketing",
                        Instant.parse("2026-06-14T00:00:00Z")
                ));

        mockMvc.perform(post("/api/v1/terminals/kiosks/device_ticketing_1/heartbeat")
                        .header("X-Device-Token", "token_ticketing_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "device_id": "device_ticketing_1",
                                  "terminal_type": "kiosk",
                                  "status": "online",
                                  "app_version": "1.0.0",
                                  "capabilities": ["ticketing"],
                                  "kiosk_mode": "ticketing"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("online"));

        verify(service).recordKioskHeartbeat(eq("device_ticketing_1"), eq("token_ticketing_1"), any(KioskHeartbeatRequest.class));
        verifyNoInteractions(authorizationService);
    }

    @Test
    void kioskHeartbeatRejectsMissingDeviceTokenThroughService() throws Exception {
        when(service.recordKioskHeartbeat(eq("device_ticketing_1"), eq(null), any(KioskHeartbeatRequest.class)))
                .thenThrow(ApiException.unauthorized("DEVICE_CREDENTIAL_REQUIRED", "X-Device-Token is required"));

        mockMvc.perform(post("/api/v1/terminals/kiosks/device_ticketing_1/heartbeat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "device_id": "device_ticketing_1",
                                  "terminal_type": "kiosk",
                                  "status": "online"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_CREDENTIAL_REQUIRED"));

        verify(service).recordKioskHeartbeat(eq("device_ticketing_1"), eq(null), any(KioskHeartbeatRequest.class));
        verifyNoInteractions(authorizationService);
    }

    @Test
    void kioskHeartbeatPropagatesInvalidDeviceTokenAsForbidden() throws Exception {
        when(service.recordKioskHeartbeat(eq("device_ticketing_1"), eq("wrong-token"), any(KioskHeartbeatRequest.class)))
                .thenThrow(ApiException.forbidden("DEVICE_CREDENTIAL_INVALID", "device token is invalid"));

        mockMvc.perform(post("/api/v1/terminals/kiosks/device_ticketing_1/heartbeat")
                        .header("X-Device-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "device_id": "device_ticketing_1",
                                  "terminal_type": "kiosk",
                                  "status": "online"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("DEVICE_CREDENTIAL_INVALID"));

        verifyNoInteractions(authorizationService);
    }

    // ── K16 设备商户归属写入面：权限/幂等闸 ─────────────────────────────────
    @Test
    void listDevicesUsesTerminalSwitchViewPermission() throws Exception {
        when(service.listDevices()).thenReturn(new PageResponse<>(List.of(), 1, 50, 0));

        mockMvc.perform(get("/api/v1/terminals/devices")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isOk());

        verify(authorizationService).requirePermission("terminal.switch.view");
    }

    @Test
    void listDevicesWithoutPermissionIsForbidden() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("terminal.switch.view");

        mockMvc.perform(get("/api/v1/terminals/devices")
                        .with(user("ops").roles("P0_SMOKE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    @Test
    void updateDeviceMerchantUsesLifecyclePermission() throws Exception {
        when(service.updateDeviceMerchant(eq("dev_1"), any(com.complex.platform.terminal.dto.DeviceMerchantRequest.class)))
                .thenReturn(new DeviceBindingResponse(
                        "dev_1", "staff_work_phone", "P0-ANDROID-1", null, null, "point_1",
                        "merchant_a", null, null, "point_1", null, "bound",
                        Instant.parse("2026-06-14T00:00:00Z"), null));

        mockMvc.perform(put("/api/v1/terminals/devices/dev_1/merchant")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "dm-1",
                                  "merchant_id": "merchant_a",
                                  "reason": "设备划归 A 吧"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchant_id").value("merchant_a"));

        verify(authorizationService).requirePermission("terminal.device.lifecycle");
    }

    @Test
    void updateDeviceMerchantWithoutPermissionIsForbidden() throws Exception {
        doThrow(ApiException.forbidden("PERMISSION_DENIED", "permission denied"))
                .when(authorizationService)
                .requirePermission("terminal.device.lifecycle");

        mockMvc.perform(put("/api/v1/terminals/devices/dev_1/merchant")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idempotency_key": "dm-denied",
                                  "merchant_id": "merchant_a",
                                  "reason": "理由"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));

        verifyNoInteractions(service);
    }

    @Test
    void updateDeviceMerchantMissingIdempotencyKeyReturnsValidationFailed() throws Exception {
        mockMvc.perform(put("/api/v1/terminals/devices/dev_1/merchant")
                        .with(user("ops").roles("P0_SMOKE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "merchant_id": "merchant_a",
                                  "reason": "理由"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("idempotency_key is required"));
    }

    private String bindBody(String idempotencyKey) {
        return """
                {
                  "idempotency_key": "%s",
                  "device_id": "P0-ANDROID-1",
                  "terminal_type": "staff_work_phone",
                  "binding_scope": "space",
                  "bound_space_id": "point_1",
                  "mdm_device_ref": "mdm-1"
                }
                """.formatted(idempotencyKey);
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        TerminalDeviceService terminalDeviceService() {
            return Mockito.mock(TerminalDeviceService.class);
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
