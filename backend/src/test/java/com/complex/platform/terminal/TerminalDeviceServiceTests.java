package com.complex.platform.terminal;

import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.dto.CapabilitySwitchRequest;
import com.complex.platform.terminal.dto.DeviceBindRequest;
import com.complex.platform.terminal.dto.DeviceLifecycleRequest;
import com.complex.platform.terminal.dto.DeviceMerchantRequest;
import com.complex.platform.terminal.dto.DeviceReturnRequest;
import com.complex.platform.terminal.dto.DeviceUnbindRequest;
import com.complex.platform.terminal.dto.KioskHeartbeatRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class TerminalDeviceServiceTests {

    private InMemoryTerminalDeviceRepository deviceRepository;
    private InMemoryIdempotencyRepository idempotencyRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private TerminalDeviceService service;
    private final Map<String, com.complex.platform.governance.MerchantRecord> merchants = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        deviceRepository = new InMemoryTerminalDeviceRepository();
        idempotencyRepository = new InMemoryIdempotencyRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        AuditEventService auditEventService = new AuditEventService(auditEventRepository);
        // MDM 开关默认关（与 V088 显式 seed false 一致）：确保现有解绑测试走"代偿不阻断"口径。
        deviceRepository.saveCapabilitySwitch(new TerminalCapabilitySwitchRecord(
                "tcs_mdm_managed_work_devices_off", "*", MdmService.MDM_CAPABILITY, "global", "global",
                false, null, java.time.Instant.parse("2026-06-14T00:00:00Z"),
                java.time.Instant.parse("2026-06-14T00:00:00Z")));
        MdmService mdmService = new MdmService(
                new NoopMdmRepository(),
                deviceRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                auditEventService);
        merchants.clear();
        service = new TerminalDeviceService(
                deviceRepository,
                new IdempotencyService(idempotencyRepository, new ObjectMapper()),
                auditEventService,
                mdmService,
                org.mockito.Mockito.mock(DeviceSigningKeyStore.class),
                id -> Optional.ofNullable(merchants.get(id)),
                // M723-F21：桩对返回 Optional 的范围方法默认给空＝平台管理员（全部设备都能管），既有用例语义不变
                org.mockito.Mockito.mock(com.complex.platform.access.AuthorizationService.class)
        );
    }

    @Test
    void bindDeviceIsIdempotentAndWritesOpenApiAuditEvent() {
        DeviceBindRequest request = bindRequest("bind-device-once", "P0-ANDROID-1");

        DeviceBindingResponse first = service.bindDevice(request);
        DeviceBindingResponse second = service.bindDevice(request);

        assertThat(second).isEqualTo(first);
        assertThat(first.deviceCode()).isEqualTo("P0-ANDROID-1");
        assertThat(first.deviceType()).isEqualTo("staff_work_phone");
        assertThat(first.locationPointId()).isEqualTo("point_1");
        assertThat(deviceRepository.deviceSaveCount).isEqualTo(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("terminal.device.bound");
        assertThat(deviceRepository.statusEvents)
                .extracting(TerminalDeviceStatusEventRecord::eventType)
                .containsExactly("terminal.device.bound");
    }

    @Test
    void bindKioskReturnsDeviceTokenAndStoresOnlySha256Hash() {
        DeviceBindingResponse response = service.bindDevice(kioskBindRequest(
                "bind-kiosk-token",
                "device_ticketing_1"
        ));

        assertThat(response.deviceToken()).isNotBlank();
        assertThat(response.deviceType()).isEqualTo("kiosk");
        assertThat(deviceRepository.findDeviceCredentialByDeviceId("device_ticketing_1"))
                .get()
                .satisfies(credential -> {
                    assertThat(credential.tokenSha256()).isEqualTo(sha256(response.deviceToken()));
                    assertThat(credential.tokenSha256()).isNotEqualTo(response.deviceToken());
                    assertThat(credential.status()).isEqualTo("active");
                });
    }

    @Test
    void bindKioskReturnsDeviceTokenOnceAndStoresSanitizedIdempotencyResponse() {
        DeviceBindRequest request = kioskBindRequest("bind-kiosk-token-once", "device_ticketing_once");

        DeviceBindingResponse first = service.bindDevice(request);
        DeviceBindingResponse replay = service.bindDevice(request);

        assertThat(first.deviceToken()).isNotBlank();
        assertThat(replay.deviceToken()).isNull();
        assertThat(replay)
                .usingRecursiveComparison()
                .ignoringFields("deviceToken")
                .isEqualTo(first);
        assertThat(deviceRepository.deviceSaveCount).isEqualTo(1);
        assertThat(idempotencyRepository.findByOperationIdAndIdempotencyKey("bindTerminalDevice", "bind-kiosk-token-once"))
                .hasValueSatisfying(record -> {
                    assertThat(record.responseBody()).doesNotContain(first.deviceToken());
                    assertThat(record.responseBody()).contains("\"deviceToken\":null");
                });
    }

    @Test
    void bindStaffWorkPhoneDoesNotIssueDeviceToken() {
        DeviceBindingResponse response = service.bindDevice(bindRequest("bind-phone-no-token", "P0-ANDROID-PHONE"));

        assertThat(response.deviceToken()).isNull();
        assertThat(deviceRepository.findDeviceCredentialByDeviceId("P0-ANDROID-PHONE")).isEmpty();
    }

    @Test
    void returnDeviceChangesStatusToReturned() {
        service.bindDevice(bindRequest("bind-return-device", "P0-ANDROID-2"));

        DeviceBindingResponse returned = service.returnDevice(new DeviceReturnRequest(
                "return-device",
                "return",
                List.of(),
                "P0-ANDROID-2",
                "staff_1",
                false,
                "shift closed"
        ));

        assertThat(returned.status()).isEqualTo("returned");
        assertThat(deviceRepository.findByDeviceCode("P0-ANDROID-2"))
                .get()
                .extracting(TerminalDeviceRecord::status, TerminalDeviceRecord::returnReason)
                .containsExactly("returned", "shift closed");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("terminal.device.returned");
    }

    @Test
    void returnKioskRevokesDeviceCredential() {
        DeviceBindingResponse bound = service.bindDevice(kioskBindRequest("bind-kiosk-return", "device_ticketing_return"));

        service.returnDevice(new DeviceReturnRequest(
                "return-kiosk",
                "return",
                List.of(),
                "device_ticketing_return",
                "staff_1",
                false,
                "shift closed"
        ));

        assertThat(deviceRepository.findDeviceCredentialByDeviceId("device_ticketing_return"))
                .get()
                .satisfies(credential -> {
                    assertThat(credential.status()).isEqualTo("revoked");
                    assertThat(credential.tokenSha256()).isEqualTo(sha256(bound.deviceToken()));
                    assertThat(credential.tokenSha256()).isNotEqualTo(bound.deviceToken());
                });
    }

    @Test
    void unbindDeviceChangesStatusToUnbound() {
        DeviceBindingResponse bound = service.bindDevice(bindRequest("bind-unbind-device", "P0-ANDROID-3"));

        DeviceBindingResponse unbound = service.unbindDevice(bound.id(), new DeviceUnbindRequest(
                "unbind-device",
                "device replacement",
                "unbind",
                List.of()
        ));

        assertThat(unbound.status()).isEqualTo("unbound");
        assertThat(deviceRepository.findById(bound.id()))
                .get()
                .extracting(TerminalDeviceRecord::status, TerminalDeviceRecord::unbindReason)
                .containsExactly("unbound", "device replacement");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("terminal.device.unbound");
    }

    @Test
    void unbindKioskRevokesDeviceCredential() {
        DeviceBindingResponse bound = service.bindDevice(kioskBindRequest("bind-kiosk-unbind", "device_ticketing_unbind"));

        service.unbindDevice(bound.id(), new DeviceUnbindRequest(
                "unbind-kiosk",
                "device replacement",
                "unbind",
                List.of()
        ));

        assertThat(deviceRepository.findDeviceCredentialByDeviceId("device_ticketing_unbind"))
                .get()
                .satisfies(credential -> {
                    assertThat(credential.status()).isEqualTo("revoked");
                    assertThat(credential.tokenSha256()).isEqualTo(sha256(bound.deviceToken()));
                    assertThat(credential.tokenSha256()).isNotEqualTo(bound.deviceToken());
                });
    }

    @Test
    void returnWithPendingTasksDoesNotMarkDeviceReturned() {
        service.bindDevice(bindRequest("bind-pending-return-device", "P0-ANDROID-4"));

        assertThatThrownBy(() -> service.returnDevice(new DeviceReturnRequest(
                "return-device-pending",
                "return",
                List.of(),
                "P0-ANDROID-4",
                "staff_1",
                true,
                "pending tasks"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("RETURN_BLOCKED_PENDING_TASKS");

        assertThat(deviceRepository.findByDeviceCode("P0-ANDROID-4"))
                .get()
                .extracting(TerminalDeviceRecord::status, TerminalDeviceRecord::returnedAt)
                .containsExactly("bound", null);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("terminal.device.returned_failed");
    }

    @Test
    void returnDeviceRejectsUnboundDeviceWithInvalidStatus() {
        DeviceBindingResponse bound = service.bindDevice(bindRequest("bind-return-unbound", "P0-ANDROID-5"));
        service.unbindDevice(bound.id(), new DeviceUnbindRequest(
                "unbind-before-return-5",
                "replaced",
                "unbind",
                List.of()
        ));

        assertThatThrownBy(() -> service.returnDevice(new DeviceReturnRequest(
                "return-unbound-5",
                "return",
                List.of(),
                "P0-ANDROID-5",
                "staff_1",
                false,
                "shift closed"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_DEVICE_STATUS");

        assertThat(deviceRepository.findByDeviceCode("P0-ANDROID-5"))
                .get()
                .extracting(TerminalDeviceRecord::status)
                .isEqualTo("unbound");
    }

    @Test
    void returnDeviceRejectsAlreadyReturnedDeviceWithInvalidStatus() {
        service.bindDevice(bindRequest("bind-return-twice", "P0-ANDROID-6"));
        service.returnDevice(new DeviceReturnRequest(
                "return-first-6",
                "return",
                List.of(),
                "P0-ANDROID-6",
                "staff_1",
                false,
                "shift closed"
        ));

        assertThatThrownBy(() -> service.returnDevice(new DeviceReturnRequest(
                "return-second-6",
                "return",
                List.of(),
                "P0-ANDROID-6",
                "staff_1",
                false,
                "shift closed again"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_DEVICE_STATUS");

        assertThat(deviceRepository.findByDeviceCode("P0-ANDROID-6"))
                .get()
                .extracting(TerminalDeviceRecord::status)
                .isEqualTo("returned");
    }

    @Test
    void unbindDeviceRejectsAlreadyUnboundDeviceWithInvalidStatus() {
        DeviceBindingResponse bound = service.bindDevice(bindRequest("bind-unbind-twice", "P0-ANDROID-7"));
        service.unbindDevice(bound.id(), new DeviceUnbindRequest(
                "unbind-first-7",
                "replaced",
                "unbind",
                List.of()
        ));

        assertThatThrownBy(() -> service.unbindDevice(bound.id(), new DeviceUnbindRequest(
                "unbind-second-7",
                "replaced again",
                "unbind",
                List.of()
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_DEVICE_STATUS");

        assertThat(deviceRepository.findById(bound.id()))
                .get()
                .extracting(TerminalDeviceRecord::status)
                .isEqualTo("unbound");
    }

    // ── P0#9 设备生命周期状态机测试 ───────────────────────────────────────────
    @Test
    void lostDeviceRejectsRebindAndWritesLostEvent() {
        DeviceBindingResponse bound = service.bindDevice(bindRequest("bind-lost", "P0-LOST-1"));
        DeviceBindingResponse lost = service.markDeviceLost(bound.id(), lifecycleRequest("lost-1", "stolen on shift"));

        assertThat(lost.status()).isEqualTo("lost");
        // 挂失设备拒绝重新领用（绑定）。
        assertThatThrownBy(() -> service.bindDevice(bindRequest("rebind-lost", "P0-LOST-1")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_DEVICE_STATUS");
        assertThat(deviceRepository.statusEvents)
                .extracting(TerminalDeviceStatusEventRecord::eventType)
                .contains("terminal.device.lost");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("terminal.device.lost");
    }

    @Test
    void foundDeviceCanBeRebound() {
        DeviceBindingResponse bound = service.bindDevice(bindRequest("bind-found", "P0-FOUND-1"));
        service.markDeviceLost(bound.id(), lifecycleRequest("lost-found-1", "lost"));
        DeviceBindingResponse found = service.markDeviceFound(bound.id(), lifecycleRequest("found-1", "recovered"));

        assertThat(found.status()).isEqualTo("found");
        // found 不是 NON_OPERATIONAL，可重新领用。
        DeviceBindingResponse rebound = service.bindDevice(bindRequest("rebind-found", "P0-FOUND-1"));
        assertThat(rebound.status()).isEqualTo("bound");
    }

    @Test
    void scrappedDeviceIsTerminalAndRejectsFurtherTransitions() {
        DeviceBindingResponse bound = service.bindDevice(bindRequest("bind-scrap", "P0-SCRAP-1"));
        DeviceBindingResponse scrapped = service.scrapDevice(bound.id(), lifecycleRequest("scrap-1", "broken screen"));

        assertThat(scrapped.status()).isEqualTo("scrapped");
        // 终态封锁：不可再送修。
        assertThatThrownBy(() -> service.repairDevice(bound.id(), lifecycleRequest("repair-after-scrap", "x")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("DEVICE_TERMINAL_STATUS");
        // 报废设备拒绝重新领用。
        assertThatThrownBy(() -> service.bindDevice(bindRequest("rebind-scrap", "P0-SCRAP-1")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_DEVICE_STATUS");
    }

    // ── P2 并发缺口①：状态机条件流转（乐观并发保护）─────────────────────────────
    @Test
    void concurrentTransitionLosesToScrapAndIsRejectedByConditionalUpdate() {
        // found（lost→found）与 scrap 并发：本测试模拟 found 在"读到 lost 前态"之后、
        // 写入之前，被另一笔 scrap 抢先改为终态 scrapped。
        DeviceBindingResponse bound = service.bindDevice(bindRequest("bind-race", "P0-RACE-1"));
        service.markDeviceLost(bound.id(), lifecycleRequest("race-lost", "lost"));

        // 注入并发：found 流转执行 compareAndSetStatus 前，先把设备改为终态 scrapped。
        deviceRepository.beforeCompareAndSet = id -> {
            TerminalDeviceRecord current = deviceRepository.findById(id).orElseThrow();
            deviceRepository.save(new TerminalDeviceRecord(
                    current.id(), current.deviceId(), current.deviceCode(), current.terminalType(),
                    current.bindingScope(), current.boundSpaceId(), current.mdmDeviceRef(), current.kioskMode(),
                    current.merchantId(), current.venueId(), current.areaId(), current.locationPointId(),
                    current.staffId(), "scrapped", current.boundAt(), current.returnedAt(), current.unboundAt(),
                    current.returnReason(), current.unbindReason(), current.createdAt(), current.updatedAt()));
            deviceRepository.beforeCompareAndSet = null; // 仅触发一次
        };

        // found 的前态校验通过（读到 lost），但条件 UPDATE 因前态已变 scrapped 命中 0 行 → 冲突。
        assertThatThrownBy(() -> service.markDeviceFound(bound.id(), lifecycleRequest("race-found", "recovered")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("DEVICE_TRANSITION_CONFLICT");

        // 仅一个流转生效：设备停在终态 scrapped，未被 found 盲写丢更新。
        assertThat(deviceRepository.findById(bound.id()))
                .get()
                .extracting(TerminalDeviceRecord::status)
                .isEqualTo("scrapped");
    }

    @Test
    void compareAndSetStatusReturnsZeroRowsWhenExpectedFromMismatch() {
        // 红线：前态不符的条件 UPDATE 必须返回 0 行（被并发改过/非法前态被拒）。
        DeviceBindingResponse bound = service.bindDevice(bindRequest("bind-cas", "P0-CAS-1"));
        TerminalDeviceRecord existing = deviceRepository.findById(bound.id()).orElseThrow();

        // 当前态为 bound，用错误的 expectedFrom=lost 发起条件流转。
        int affected = deviceRepository.compareAndSetStatus(
                existing.id(), "lost", recordWithStatus(existing, "found"));

        assertThat(affected).isZero();
        // 前态不符未写入，设备仍为 bound。
        assertThat(deviceRepository.findById(bound.id()))
                .get()
                .extracting(TerminalDeviceRecord::status)
                .isEqualTo("bound");
    }

    private TerminalDeviceRecord recordWithStatus(TerminalDeviceRecord base, String status) {
        return new TerminalDeviceRecord(
                base.id(), base.deviceId(), base.deviceCode(), base.terminalType(),
                base.bindingScope(), base.boundSpaceId(), base.mdmDeviceRef(), base.kioskMode(),
                base.merchantId(), base.venueId(), base.areaId(), base.locationPointId(),
                base.staffId(), status, base.boundAt(), base.returnedAt(), base.unboundAt(),
                base.returnReason(), base.unbindReason(), base.createdAt(), base.updatedAt());
    }

    @Test
    void repairTransitionRejectedFromUnboundStatus() {
        DeviceBindingResponse bound = service.bindDevice(bindRequest("bind-repair-bad", "P0-REPAIR-1"));
        service.unbindDevice(bound.id(), new DeviceUnbindRequest("unbind-repair-1", "replaced", "unbind", List.of()));

        assertThatThrownBy(() -> service.repairDevice(bound.id(), lifecycleRequest("repair-bad-1", "x")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("INVALID_DEVICE_STATUS");
    }

    @Test
    void updateCapabilitySwitchPersistsEnabledStateAndWritesAudit() {
        TerminalCapabilitySwitchRecord enabled = service.updateCapabilitySwitch("switch_ticketing", new CapabilitySwitchRequest(
                "switch-enable",
                "enable ticketing",
                List.of(),
                true,
                "kiosk",
                "ticketing",
                "device_ticketing_1"
        ));

        TerminalCapabilitySwitchRecord replay = service.updateCapabilitySwitch("switch_ticketing", new CapabilitySwitchRequest(
                "switch-enable",
                "enable ticketing",
                List.of(),
                true,
                "kiosk",
                "ticketing",
                "device_ticketing_1"
        ));

        assertThat(replay).isEqualTo(enabled);
        assertThat(enabled.enabled()).isTrue();
        assertThat(deviceRepository.capabilitySwitches.get("switch_ticketing").enabled()).isTrue();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("terminal_switch.changed");
    }

    @Test
    void kioskConfigUsesModeCapabilitySwitchAndValidDeviceToken() {
        saveBoundKiosk("device_ticketing_1", "ticketing", "valid-token");
        deviceRepository.seedKioskConfig(new TerminalKioskConfigRecord(
                "device_ticketing_1",
                "ticketing",
                true,
                "project:p0",
                Map.of("home", "ticketing"),
                java.time.Instant.parse("2026-06-14T00:00:00Z")
        ));
        service.updateCapabilitySwitch("switch_ticketing_1", new CapabilitySwitchRequest(
                "switch-ticketing-on",
                "ticketing on",
                List.of(),
                true,
                "kiosk",
                "ticketing",
                "device_ticketing_1"
        ));
        saveBoundKiosk("device_service_1", "service", "service-token");
        deviceRepository.seedKioskConfig(new TerminalKioskConfigRecord(
                "device_service_1",
                "service",
                true,
                "project:p0",
                Map.of("home", "service"),
                java.time.Instant.parse("2026-06-14T00:00:00Z")
        ));

        KioskConfigResponse ticketing = service.getKioskConfig("device_ticketing_1", "valid-token");
        KioskConfigResponse serviceOnly = service.getKioskConfig("device_service_1", "service-token");
        KioskHeartbeatRecord heartbeat = service.recordKioskHeartbeat("device_ticketing_1", "valid-token", new KioskHeartbeatRequest(
                "device_ticketing_1",
                "kiosk",
                "online",
                "1.0.0",
                List.of("ticketing"),
                null,
                "ticketing"
        ));

        assertThat(ticketing.ticketingEnabled()).isTrue();
        assertThat(serviceOnly.ticketingEnabled()).isFalse();
        assertThat(heartbeat.status()).isEqualTo("online");
        assertThat(deviceRepository.heartbeats.get("device_ticketing_1").appVersion()).isEqualTo("1.0.0");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("terminal.kiosk_config_viewed", "terminal.kiosk_heartbeat");
    }

    @Test
    void kioskConfigRejectsMissingAndWrongDeviceTokenWithAccessRejectedAudit() {
        saveBoundKiosk("device_ticketing_1", "ticketing", "valid-token");
        deviceRepository.seedKioskConfig(new TerminalKioskConfigRecord(
                "device_ticketing_1",
                "ticketing",
                true,
                "project:p0",
                Map.of(),
                java.time.Instant.parse("2026-06-14T00:00:00Z")
        ));

        assertThatThrownBy(() -> service.getKioskConfig("device_ticketing_1", null))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(apiException.code()).isEqualTo("DEVICE_CREDENTIAL_REQUIRED");
                });

        assertThatThrownBy(() -> service.getKioskConfig("device_ticketing_1", "wrong-token"))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID");
                });

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("terminal.access_rejected", "terminal.access_rejected");
    }

    @Test
    void kioskConfigRejectsReturnedUnboundDisabledAndNonKioskDevices() {
        saveDeviceWithCredential("returned_device", "returned", "kiosk", "ticketing", "returned-token");
        saveDeviceWithCredential("unbound_device", "unbound", "kiosk", "ticketing", "unbound-token");
        saveDeviceWithCredential("disabled_device", "disabled", "kiosk", "ticketing", "disabled-token");
        saveDeviceWithCredential("tablet_device", "bound", "tablet", null, "tablet-token");

        assertRejectedAccess("returned_device", "returned-token");
        assertRejectedAccess("unbound_device", "unbound-token");
        assertRejectedAccess("disabled_device", "disabled-token");
        assertRejectedAccess("tablet_device", "tablet-token");
    }

    @Test
    void kioskHeartbeatRejectsWrongDeviceTokenWithAccessRejectedAudit() {
        saveBoundKiosk("device_ticketing_1", "ticketing", "valid-token");
        deviceRepository.seedKioskConfig(new TerminalKioskConfigRecord(
                "device_ticketing_1",
                "ticketing",
                true,
                "project:p0",
                Map.of(),
                java.time.Instant.parse("2026-06-14T00:00:00Z")
        ));

        assertThatThrownBy(() -> service.recordKioskHeartbeat("device_ticketing_1", "wrong-token", new KioskHeartbeatRequest(
                "device_ticketing_1",
                "kiosk",
                "online",
                "1.0.0",
                List.of("ticketing"),
                null,
                "ticketing"
        )))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.code()).isEqualTo("DEVICE_CREDENTIAL_INVALID");
                });

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("terminal.access_rejected");
        assertThat(deviceRepository.heartbeats).isEmpty();
    }

    @Test
    void kioskHeartbeatKeepsPathDeviceIdValidationAfterCredentialCheck() {
        saveBoundKiosk("device_ticketing_1", "ticketing", "valid-token");

        assertThatThrownBy(() -> service.recordKioskHeartbeat("device_ticketing_1", "valid-token", new KioskHeartbeatRequest(
                "different_device",
                "kiosk",
                "online",
                "1.0.0",
                List.of("ticketing"),
                null,
                "ticketing"
        )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    private DeviceBindRequest bindRequest(String idempotencyKey, String deviceId) {
        // U07-001：工作手机领用即绑定持有员工（staff_id 必填）。
        return new DeviceBindRequest(
                idempotencyKey,
                "bind",
                List.of(),
                deviceId,
                "staff_work_phone",
                "space",
                "point_1",
                "mdm-" + deviceId,
                null,
                null,
                "staff_1"
        );
    }

    private DeviceLifecycleRequest lifecycleRequest(String idempotencyKey, String reason) {
        return new DeviceLifecycleRequest(idempotencyKey, "note", List.of(), "staff_1", reason);
    }

    private DeviceBindRequest kioskBindRequest(String idempotencyKey, String deviceId) {
        return new DeviceBindRequest(
                idempotencyKey,
                "bind",
                List.of(),
                deviceId,
                "kiosk",
                "project",
                "project_p0",
                "mdm-" + deviceId,
                "ticketing",
                null
        );
    }

    private void saveBoundKiosk(String deviceId, String kioskMode, String token) {
        saveDeviceWithCredential(deviceId, "bound", "kiosk", kioskMode, token);
    }

    private void saveDeviceWithCredential(
            String deviceId,
            String status,
            String terminalType,
            String kioskMode,
            String token
    ) {
        saveDevice(deviceId, status, terminalType, kioskMode);
        deviceRepository.saveDeviceCredential(new DeviceCredentialRecord(
                deviceId,
                sha256(token),
                "active",
                java.time.Instant.parse("2026-06-14T00:00:00Z"),
                java.time.Instant.parse("2026-06-14T00:00:00Z")
        ));
    }

    private void saveDevice(String deviceId, String status, String terminalType, String kioskMode) {
        java.time.Instant now = java.time.Instant.parse("2026-06-14T00:00:00Z");
        deviceRepository.save(new TerminalDeviceRecord(
                "dev_" + deviceId,
                deviceId,
                deviceId,
                terminalType,
                "project",
                "project_p0",
                "mdm-" + deviceId,
                kioskMode,
                null,
                null,
                null,
                "project_p0",
                null,
                status,
                now,
                null,
                "unbound".equals(status) ? now : null,
                "returned".equals(status) ? "returned" : null,
                "unbound".equals(status) ? "unbound" : null,
                now,
                now
        ));
    }

    private void assertRejectedAccess(String deviceId, String token) {
        assertThatThrownBy(() -> service.getKioskConfig(deviceId, token))
                .isInstanceOf(ApiException.class)
                .satisfies(exception -> {
                    ApiException apiException = (ApiException) exception;
                    assertThat(apiException.status()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.code()).isEqualTo("DEVICE_ACCESS_REJECTED");
                });
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static final class InMemoryTerminalDeviceRepository implements TerminalDeviceRepository {
        private final Map<String, TerminalDeviceRecord> devicesById = new LinkedHashMap<>();
        private final Map<String, TerminalDeviceRecord> devicesByCode = new LinkedHashMap<>();
        private final Map<String, TerminalDeviceRecord> devicesByDeviceId = new LinkedHashMap<>();
        private final Map<String, DeviceCredentialRecord> deviceCredentials = new LinkedHashMap<>();
        private final List<TerminalDeviceStatusEventRecord> statusEvents = new ArrayList<>();
        private final Map<String, TerminalCapabilitySwitchRecord> capabilitySwitches = new LinkedHashMap<>();
        private final Map<String, TerminalKioskConfigRecord> kioskConfigs = new LinkedHashMap<>();
        private final Map<String, KioskHeartbeatRecord> heartbeats = new LinkedHashMap<>();
        private int deviceSaveCount;

        @Override
        public List<TerminalDeviceRecord> listDevices() {
            return List.copyOf(devicesById.values());
        }

        // 测试钩子：注入"读后写前"被并发改态的场景，复现真并发窗口。
        private java.util.function.Consumer<String> beforeCompareAndSet;

        @Override
        public TerminalDeviceRecord save(TerminalDeviceRecord device) {
            deviceSaveCount += 1;
            devicesById.put(device.id(), device);
            devicesByCode.put(device.deviceCode(), device);
            devicesByDeviceId.put(device.deviceId(), device);
            return device;
        }

        @Override
        public int compareAndSetStatus(String id, String expectedFrom, TerminalDeviceRecord updated) {
            // 模拟条件 UPDATE 的并发窗口：在比对前态前先让注入的并发流转改态。
            if (beforeCompareAndSet != null) {
                beforeCompareAndSet.accept(id);
            }
            TerminalDeviceRecord current = devicesById.get(id);
            // 条件 UPDATE 语义：仅当当前 status = expectedFrom 才整行更新，否则 0 行（前态不符/不存在）。
            if (current == null || !expectedFrom.equals(current.status())) {
                return 0;
            }
            devicesById.put(updated.id(), updated);
            devicesByCode.put(updated.deviceCode(), updated);
            devicesByDeviceId.put(updated.deviceId(), updated);
            return 1;
        }

        @Override
        public Optional<TerminalDeviceRecord> findByDeviceCode(String deviceCode) {
            return Optional.ofNullable(devicesByCode.get(deviceCode));
        }

        @Override
        public Optional<TerminalDeviceRecord> findById(String id) {
            return Optional.ofNullable(devicesById.get(id));
        }

        @Override
        public Optional<TerminalDeviceRecord> findByDeviceId(String deviceId) {
            return Optional.ofNullable(devicesByDeviceId.get(deviceId));
        }

        @Override
        public Optional<DeviceCredentialRecord> findDeviceCredentialByDeviceId(String deviceId) {
            return Optional.ofNullable(deviceCredentials.get(deviceId));
        }

        @Override
        public DeviceCredentialRecord saveDeviceCredential(DeviceCredentialRecord record) {
            deviceCredentials.put(record.deviceId(), record);
            return record;
        }

        @Override
        public TerminalDeviceStatusEventRecord saveStatusEvent(TerminalDeviceStatusEventRecord event) {
            statusEvents.add(event);
            return event;
        }

        @Override
        public List<TerminalCapabilitySwitchRecord> listCapabilitySwitches() {
            return new ArrayList<>(capabilitySwitches.values());
        }

        @Override
        public java.util.Optional<TerminalCapabilitySwitchRecord> findCapabilitySwitchById(String id) {
            return java.util.Optional.ofNullable(capabilitySwitches.get(id));
        }

        @Override
        public java.util.Optional<TerminalCapabilitySwitchRecord> findCapabilitySwitch(String terminalType, String capabilityCode, String scopeId) {
            return capabilitySwitches.values().stream()
                    .filter(record -> terminalType.equals(record.terminalType()))
                    .filter(record -> capabilityCode.equals(record.capabilityCode()))
                    .filter(record -> scopeId.equals(record.scopeId()))
                    .findFirst();
        }

        @Override
        public java.util.Optional<TerminalCapabilitySwitchRecord> resolveEffectiveSwitch(
                String terminalType, String capabilityCode, Map<String, String> orderedScopes) {
            for (Map.Entry<String, String> scope : orderedScopes.entrySet()) {
                if (scope.getValue() == null || scope.getValue().isBlank()) {
                    continue;
                }
                java.util.Optional<TerminalCapabilitySwitchRecord> hit = capabilitySwitches.values().stream()
                        .filter(r -> terminalType.equals(r.terminalType()) || "*".equals(r.terminalType()))
                        .filter(r -> capabilityCode.equals(r.capabilityCode()))
                        .filter(r -> scope.getKey().equals(r.scopeType()))
                        .filter(r -> scope.getValue().equals(r.scopeId()))
                        .findFirst();
                if (hit.isPresent()) {
                    return hit;
                }
            }
            return java.util.Optional.empty();
        }

        @Override
        public TerminalCapabilitySwitchRecord saveCapabilitySwitch(TerminalCapabilitySwitchRecord record) {
            capabilitySwitches.put(record.id(), record);
            return record;
        }

        @Override
        public java.util.Optional<TerminalKioskConfigRecord> findKioskConfigByDeviceId(String deviceId) {
            return java.util.Optional.ofNullable(kioskConfigs.get(deviceId));
        }

        public TerminalKioskConfigRecord seedKioskConfig(TerminalKioskConfigRecord record) {
            kioskConfigs.put(record.deviceId(), record);
            return record;
        }

        @Override
        public KioskHeartbeatRecord saveKioskHeartbeat(KioskHeartbeatRecord record) {
            heartbeats.put(record.deviceId(), record);
            return record;
        }
    }

    /** 门控用空 MDM 仓储：无纳管视图（findDeviceManagement 空），不参与生命周期测试主断言。 */
    private static final class NoopMdmRepository implements MdmRepository {
        @Override
        public List<MdmAppWhitelistRecord> listAppWhitelist() {
            return List.of();
        }

        @Override
        public Optional<MdmAppWhitelistRecord> findAppWhitelistById(String id) {
            return Optional.empty();
        }

        @Override
        public MdmAppWhitelistRecord saveAppWhitelist(MdmAppWhitelistRecord record) {
            return record;
        }

        @Override
        public boolean isAppWhitelisted(String packageName) {
            return false;
        }

        @Override
        public List<MdmSiteWhitelistRecord> listSiteWhitelist() {
            return List.of();
        }

        @Override
        public Optional<MdmSiteWhitelistRecord> findSiteWhitelistById(String id) {
            return Optional.empty();
        }

        @Override
        public MdmSiteWhitelistRecord saveSiteWhitelist(MdmSiteWhitelistRecord record) {
            return record;
        }

        @Override
        public boolean isSiteWhitelisted(String domain) {
            return false;
        }

        @Override
        public Optional<MdmDeviceManagementRecord> findDeviceManagement(String deviceId) {
            return Optional.empty();
        }

        @Override
        public void updateDeviceManagement(
                String deviceId, String managementStatus, String policyStatus,
                java.time.Instant lastPolicySyncAt, java.time.Instant enrolledAt) {
        }

        @Override
        public MdmPolicyViolationRecord saveViolation(MdmPolicyViolationRecord record) {
            return record;
        }

        @Override
        public Optional<MdmPolicyViolationRecord> findViolationById(String id) {
            return Optional.empty();
        }

        @Override
        public int markViolationHandled(
                String id, String disposition, String handledNote, java.time.Instant blockedAt) {
            return 0;
        }

        @Override
        public List<MdmPolicyViolationRecord> listViolations(String deviceId) {
            return List.of();
        }
    }

    // ── K16 设备商户归属写入面 ───────────────────────────────────────────────
    private void seedMerchant(String id, java.time.Instant archivedAt) {
        java.time.Instant now = java.time.Instant.parse("2026-06-14T00:00:00Z");
        com.complex.platform.governance.MerchantRecord merchant =
                new com.complex.platform.governance.MerchantRecord(
                        id, "商户" + id, "bar", null, null, null, null, null, null, null,
                        null, null, null, null, null, null, "active", now, now);
        merchants.put(id, archivedAt == null ? merchant : merchant.withArchive(archivedAt, "tester", now));
    }

    private DeviceMerchantRequest merchantRequest(String idempotencyKey, String merchantId, String reason) {
        return new DeviceMerchantRequest(idempotencyKey, merchantId, reason);
    }

    @Test
    void updateDeviceMerchantSetsMerchantAndLeavesFromToTrail() {
        saveDevice("P0-ANDROID-1", "bound", "staff_work_phone", null);
        seedMerchant("merchant_a", null);

        DeviceBindingResponse response = service.updateDeviceMerchant(
                "dev_P0-ANDROID-1", merchantRequest("dm-set-1", "merchant_a", "新店开业，设备划归 A 吧"));

        assertThat(response.merchantId()).isEqualTo("merchant_a");
        assertThat(deviceRepository.findById("dev_P0-ANDROID-1")).get()
                .extracting(TerminalDeviceRecord::merchantId).isEqualTo("merchant_a");
        // 留痕：审计事件带 from/to/reason 明细。
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "terminal.device.merchant_changed".equals(event.eventName()))
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.metadata()).containsEntry("from_merchant_id", "");
                    assertThat(event.metadata()).containsEntry("to_merchant_id", "merchant_a");
                    assertThat(event.metadata()).containsEntry("reason", "新店开业，设备划归 A 吧");
                });
        // 留痕：append-only 状态事件一条。
        assertThat(deviceRepository.statusEvents)
                .filteredOn(event -> "terminal.device.merchant_changed".equals(event.eventType()))
                .singleElement()
                .satisfies(event -> assertThat(event.reason()).isEqualTo("新店开业，设备划归 A 吧"));
    }

    @Test
    void updateDeviceMerchantRebindLeavesFromToTrail() {
        saveDevice("P0-ANDROID-1", "bound", "staff_work_phone", null);
        seedMerchant("merchant_a", null);
        seedMerchant("merchant_b", null);
        service.updateDeviceMerchant("dev_P0-ANDROID-1", merchantRequest("dm-a", "merchant_a", "划归 A"));

        service.updateDeviceMerchant("dev_P0-ANDROID-1", merchantRequest("dm-b", "merchant_b", "设备挪到 B 吧"));

        assertThat(auditEventRepository.events)
                .filteredOn(event -> "terminal.device.merchant_changed".equals(event.eventName()))
                .hasSize(2)
                .last()
                .satisfies(event -> {
                    assertThat(event.metadata()).containsEntry("from_merchant_id", "merchant_a");
                    assertThat(event.metadata()).containsEntry("to_merchant_id", "merchant_b");
                });
    }

    @Test
    void clearDeviceMerchantLeavesTrailAndUnbindsAttribution() {
        saveDevice("P0-ANDROID-1", "bound", "staff_work_phone", null);
        seedMerchant("merchant_a", null);
        service.updateDeviceMerchant("dev_P0-ANDROID-1", merchantRequest("dm-a", "merchant_a", "划归 A"));

        DeviceBindingResponse response = service.updateDeviceMerchant(
                "dev_P0-ANDROID-1", merchantRequest("dm-clear", null, "商户退场，解除归属"));

        assertThat(response.merchantId()).isNull();
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "terminal.device.merchant_changed".equals(event.eventName()))
                .last()
                .satisfies(event -> {
                    assertThat(event.metadata()).containsEntry("from_merchant_id", "merchant_a");
                    assertThat(event.metadata()).containsEntry("to_merchant_id", "");
                });
    }

    @Test
    void updateDeviceMerchantRejectsBlankReason() {
        saveDevice("P0-ANDROID-1", "bound", "staff_work_phone", null);
        seedMerchant("merchant_a", null);

        assertThatThrownBy(() -> service.updateDeviceMerchant(
                "dev_P0-ANDROID-1", merchantRequest("dm-noreason", "merchant_a", " ")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(deviceRepository.findById("dev_P0-ANDROID-1")).get()
                .extracting(TerminalDeviceRecord::merchantId).isNull();
    }

    @Test
    void updateDeviceMerchantRejectsUnknownMerchant() {
        saveDevice("P0-ANDROID-1", "bound", "staff_work_phone", null);

        assertThatThrownBy(() -> service.updateDeviceMerchant(
                "dev_P0-ANDROID-1", merchantRequest("dm-unknown", "no_such_merchant", "理由")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MERCHANT_NOT_FOUND"));
    }

    @Test
    void updateDeviceMerchantRejectsArchivedMerchant() {
        saveDevice("P0-ANDROID-1", "bound", "staff_work_phone", null);
        seedMerchant("merchant_gone", java.time.Instant.parse("2026-06-01T00:00:00Z"));

        assertThatThrownBy(() -> service.updateDeviceMerchant(
                "dev_P0-ANDROID-1", merchantRequest("dm-archived", "merchant_gone", "理由")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MERCHANT_ARCHIVED"));
    }

    @Test
    void updateDeviceMerchantRejectsTerminalStatusDevice() {
        saveDevice("P0-ANDROID-1", "scrapped", "staff_work_phone", null);
        seedMerchant("merchant_a", null);

        assertThatThrownBy(() -> service.updateDeviceMerchant(
                "dev_P0-ANDROID-1", merchantRequest("dm-scrapped", "merchant_a", "理由")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("DEVICE_TERMINAL_STATUS"));
    }

    @Test
    void updateDeviceMerchantRejectsNoChange() {
        saveDevice("P0-ANDROID-1", "bound", "staff_work_phone", null);
        seedMerchant("merchant_a", null);
        service.updateDeviceMerchant("dev_P0-ANDROID-1", merchantRequest("dm-a", "merchant_a", "划归 A"));

        assertThatThrownBy(() -> service.updateDeviceMerchant(
                "dev_P0-ANDROID-1", merchantRequest("dm-same", "merchant_a", "重复划归")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("DEVICE_MERCHANT_UNCHANGED"));
    }

    @Test
    void updateDeviceMerchantIsIdempotent() {
        saveDevice("P0-ANDROID-1", "bound", "staff_work_phone", null);
        seedMerchant("merchant_a", null);
        DeviceMerchantRequest request = merchantRequest("dm-idem", "merchant_a", "划归 A");

        DeviceBindingResponse first = service.updateDeviceMerchant("dev_P0-ANDROID-1", request);
        DeviceBindingResponse second = service.updateDeviceMerchant("dev_P0-ANDROID-1", request);

        assertThat(second).isEqualTo(first);
        assertThat(deviceRepository.statusEvents)
                .filteredOn(event -> "terminal.device.merchant_changed".equals(event.eventType()))
                .hasSize(1);
    }

    @Test
    void bindWithInitialMerchantValidatesAndSetsAttribution() {
        seedMerchant("merchant_a", null);
        DeviceBindingResponse response = service.bindDevice(new DeviceBindRequest(
                "bind-with-merchant", "bind", List.of(), "P0-ANDROID-9", "staff_work_phone",
                "space", "point_1", "mdm-P0-ANDROID-9", null, "merchant_a", "staff_1"));

        assertThat(response.merchantId()).isEqualTo("merchant_a");

        assertThatThrownBy(() -> service.bindDevice(new DeviceBindRequest(
                "bind-with-bad-merchant", "bind", List.of(), "P0-ANDROID-10", "staff_work_phone",
                "space", "point_1", "mdm-P0-ANDROID-10", null, "no_such_merchant", "staff_1")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MERCHANT_NOT_FOUND"));
    }

    @Test
    void rebindWithoutMerchantKeepsExistingAttribution() {
        // 归还后再领用不传商户：既有归属保留，不被 upsert 抹成 null（否则 K16 直接 fail-closed）。
        seedMerchant("merchant_a", null);
        service.bindDevice(new DeviceBindRequest(
                "bind-keep-1", "bind", List.of(), "P0-ANDROID-11", "staff_work_phone",
                "space", "point_1", "mdm-P0-ANDROID-11", null, "merchant_a", "staff_1"));
        service.returnDevice(new DeviceReturnRequest(
                "return-keep-1", "note", List.of(), "P0-ANDROID-11", "staff_1", false, "shift done"));

        DeviceBindingResponse rebound = service.bindDevice(bindRequest("bind-keep-2", "P0-ANDROID-11"));

        assertThat(rebound.merchantId()).isEqualTo("merchant_a");
    }

    @Test
    void listDevicesReturnsLedgerRows() {
        saveDevice("P0-ANDROID-1", "bound", "staff_work_phone", null);

        assertThat(service.listDevices().items())
                .extracting(TerminalDeviceRecord::deviceCode)
                .contains("P0-ANDROID-1");
    }

    @Test
    void registerSigningKeyRejectsUnknownDevice() {
        // 存在性校验：对不存在的 deviceId 登记签名密钥被拒（不建孤儿密钥行）。
        assertThatThrownBy(() -> service.registerSigningKey("no-such-device", "PUBKEY", "idem-1"))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("TERMINAL_DEVICE_NOT_FOUND"));
    }
}
