package com.complex.platform.terminal;

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

    @BeforeEach
    void setUp() {
        deviceRepository = new InMemoryTerminalDeviceRepository();
        idempotencyRepository = new InMemoryIdempotencyRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        service = new TerminalDeviceService(
                deviceRepository,
                new IdempotencyService(idempotencyRepository, new ObjectMapper()),
                new AuditEventService(auditEventRepository)
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
        deviceRepository.saveKioskConfig(new TerminalKioskConfigRecord(
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
        deviceRepository.saveKioskConfig(new TerminalKioskConfigRecord(
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
        deviceRepository.saveKioskConfig(new TerminalKioskConfigRecord(
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
        deviceRepository.saveKioskConfig(new TerminalKioskConfigRecord(
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
        return new DeviceBindRequest(
                idempotencyKey,
                "bind",
                List.of(),
                deviceId,
                "staff_work_phone",
                "space",
                "point_1",
                "mdm-" + deviceId,
                null
        );
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
                "ticketing"
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
        public TerminalDeviceRecord save(TerminalDeviceRecord device) {
            deviceSaveCount += 1;
            devicesById.put(device.id(), device);
            devicesByCode.put(device.deviceCode(), device);
            devicesByDeviceId.put(device.deviceId(), device);
            return device;
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
        public TerminalCapabilitySwitchRecord saveCapabilitySwitch(TerminalCapabilitySwitchRecord record) {
            capabilitySwitches.put(record.id(), record);
            return record;
        }

        @Override
        public java.util.Optional<TerminalKioskConfigRecord> findKioskConfigByDeviceId(String deviceId) {
            return java.util.Optional.ofNullable(kioskConfigs.get(deviceId));
        }

        @Override
        public TerminalKioskConfigRecord saveKioskConfig(TerminalKioskConfigRecord record) {
            kioskConfigs.put(record.deviceId(), record);
            return record;
        }

        @Override
        public KioskHeartbeatRecord saveKioskHeartbeat(KioskHeartbeatRecord record) {
            heartbeats.put(record.deviceId(), record);
            return record;
        }
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
