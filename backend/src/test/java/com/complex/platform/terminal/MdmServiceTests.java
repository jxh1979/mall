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
import com.complex.platform.terminal.dto.MdmAppWhitelistRequest;
import com.complex.platform.terminal.dto.MdmDeviceEnrollRequest;
import com.complex.platform.terminal.dto.MdmPolicyViolationRequest;
import com.complex.platform.terminal.dto.MdmSiteWhitelistRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MdmServiceTests {

    private InMemoryMdmRepository mdmRepository;
    private SwitchOnlyTerminalRepository terminalRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private MdmService service;

    @BeforeEach
    void setUp() {
        mdmRepository = new InMemoryMdmRepository();
        terminalRepository = new SwitchOnlyTerminalRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        service = new MdmService(
                mdmRepository,
                terminalRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository)
        );
    }

    @Test
    void appWhitelistUpsertIsIdempotentAndWritesAudit() {
        MdmAppWhitelistRequest request = new MdmAppWhitelistRequest(
                "app-wl-1", "note", List.of(), "com.tencent.wework", "WeCom", "global", "global", true, "allow wecom");

        MdmAppWhitelistRecord first = service.upsertAppWhitelist("mdm_app_wecom", request);
        MdmAppWhitelistRecord replay = service.upsertAppWhitelist("mdm_app_wecom", request);

        assertThat(replay).isEqualTo(first);
        assertThat(first.packageName()).isEqualTo("com.tencent.wework");
        assertThat(first.enabled()).isTrue();
        assertThat(mdmRepository.apps).hasSize(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("mdm.app_whitelist.changed");
    }

    @Test
    void siteWhitelistUpsertPersistsAndListReturnsPage() {
        service.upsertSiteWhitelist("mdm_site_pay", new MdmSiteWhitelistRequest(
                "site-wl-1", "note", List.of(), "pay.example.com", "payment", "global", "global", true, null));

        assertThat(service.listSiteWhitelist().items())
                .extracting(MdmSiteWhitelistRecord::domain)
                .containsExactly("pay.example.com");
    }

    @Test
    void appWhitelistRejectsInvalidScopeType() {
        assertThatThrownBy(() -> service.upsertAppWhitelist("mdm_app_x", new MdmAppWhitelistRequest(
                "app-wl-bad", "note", List.of(), "com.x", "X", "bogus", "global", true, null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void mdmDisabledDoesNotBlockHighRiskActionAndRecordsCompensatedPolicyChecked() {
        terminalRepository.setMdmEnabled(false);
        // 未纳管设备，开关关：不阻断（代偿口径）。
        service.requireManagedForHighRiskAction("device_unmanaged", "bar.high_value_redeem");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("mdm.policy_checked");
        Map<String, Object> payload = auditEventRepository.events.get(0).metadata();
        assertThat(payload).containsEntry("compensated", true);
        assertThat(payload).containsEntry("mdm_enabled", false);
    }

    @Test
    void mdmEnabledBlocksHighRiskActionForUnmanagedDeviceWithPolicyViolationEvent() {
        terminalRepository.setMdmEnabled(true);

        assertThatThrownBy(() -> service.requireManagedForHighRiskAction("device_unmanaged", "bar.high_value_redeem"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("DEVICE_NOT_MANAGED");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("device.policy_violation");
    }

    @Test
    void mdmEnabledAllowsHighRiskActionForCompliantDevice() {
        terminalRepository.setMdmEnabled(true);
        mdmRepository.management.put("device_managed", new MdmDeviceManagementRecord(
                "device_managed", "staff_work_phone", "bound", "compliant", "applied", Instant.now(), Instant.now()));

        service.requireManagedForHighRiskAction("device_managed", "bar.high_value_redeem");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("mdm.policy_checked");
    }

    @Test
    void enrollDeviceSetsCompliantWhenPolicyApplied() {
        mdmRepository.management.put("device_1", new MdmDeviceManagementRecord(
                "device_1", "staff_work_phone", "bound", "unmanaged", null, null, null));

        MdmDeviceManagementRecord enrolled = service.enrollDevice(new MdmDeviceEnrollRequest(
                "enroll-1", "note", List.of(), "device_1", "applied", null));

        assertThat(enrolled.managementStatus()).isEqualTo("compliant");
        assertThat(enrolled.policyStatus()).isEqualTo("applied");
        assertThat(mdmRepository.management.get("device_1").managementStatus()).isEqualTo("compliant");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("mdm.policy_checked");
    }

    @Test
    void enrollDeviceRejectsMissingDevice() {
        assertThatThrownBy(() -> service.enrollDevice(new MdmDeviceEnrollRequest(
                "enroll-missing", "note", List.of(), "no_such_device", "applied", null)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("DEVICE_NOT_FOUND");
    }

    @Test
    void reportPolicyViolationPersistsAndWritesDevicePolicyViolationEvent() {
        mdmRepository.management.put("device_v", new MdmDeviceManagementRecord(
                "device_v", "staff_work_phone", "bound", "compliant", "applied", Instant.now(), Instant.now()));

        MdmPolicyViolationRecord saved = service.reportPolicyViolation(new MdmPolicyViolationRequest(
                "vio-1", "note", List.of(), "device_v", "unauthorized_app", "detected com.evil"));

        assertThat(saved.violationType()).isEqualTo("unauthorized_app");
        assertThat(mdmRepository.violations).hasSize(1);
        assertThat(mdmRepository.management.get("device_v").managementStatus()).isEqualTo("violation");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("device.policy_violation");
    }

    @Test
    void reportPolicyViolationRejectsInvalidType() {
        assertThatThrownBy(() -> service.reportPolicyViolation(new MdmPolicyViolationRequest(
                "vio-bad", "note", List.of(), "device_v", "bogus_type", "x")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");
    }

    // ── fakes ─────────────────────────────────────────────────────────────────
    private static final class InMemoryMdmRepository implements MdmRepository {
        private final Map<String, MdmAppWhitelistRecord> apps = new LinkedHashMap<>();
        private final Map<String, MdmSiteWhitelistRecord> sites = new LinkedHashMap<>();
        private final Map<String, MdmDeviceManagementRecord> management = new LinkedHashMap<>();
        private final List<MdmPolicyViolationRecord> violations = new ArrayList<>();

        @Override
        public List<MdmAppWhitelistRecord> listAppWhitelist() {
            return new ArrayList<>(apps.values());
        }

        @Override
        public Optional<MdmAppWhitelistRecord> findAppWhitelistById(String id) {
            return Optional.ofNullable(apps.get(id));
        }

        @Override
        public MdmAppWhitelistRecord saveAppWhitelist(MdmAppWhitelistRecord record) {
            apps.put(record.id(), record);
            return record;
        }

        @Override
        public List<MdmSiteWhitelistRecord> listSiteWhitelist() {
            return new ArrayList<>(sites.values());
        }

        @Override
        public Optional<MdmSiteWhitelistRecord> findSiteWhitelistById(String id) {
            return Optional.ofNullable(sites.get(id));
        }

        @Override
        public MdmSiteWhitelistRecord saveSiteWhitelist(MdmSiteWhitelistRecord record) {
            sites.put(record.id(), record);
            return record;
        }

        @Override
        public Optional<MdmDeviceManagementRecord> findDeviceManagement(String deviceId) {
            return Optional.ofNullable(management.get(deviceId));
        }

        @Override
        public void updateDeviceManagement(
                String deviceId, String managementStatus, String policyStatus,
                Instant lastPolicySyncAt, Instant enrolledAt) {
            MdmDeviceManagementRecord existing = management.get(deviceId);
            if (existing == null) {
                return;
            }
            management.put(deviceId, new MdmDeviceManagementRecord(
                    deviceId, existing.terminalType(), existing.status(),
                    managementStatus, policyStatus, lastPolicySyncAt,
                    existing.enrolledAt() == null ? enrolledAt : existing.enrolledAt()));
        }

        @Override
        public MdmPolicyViolationRecord saveViolation(MdmPolicyViolationRecord record) {
            violations.add(record);
            return record;
        }

        @Override
        public List<MdmPolicyViolationRecord> listViolations(String deviceId) {
            return violations.stream()
                    .filter(v -> deviceId == null || deviceId.equals(v.deviceId()))
                    .toList();
        }
    }

    /** 仅实现 resolveEffectiveSwitch（MdmService.isMdmEnabled 唯一依赖的方法）。 */
    private static final class SwitchOnlyTerminalRepository implements TerminalDeviceRepository {
        private boolean mdmEnabled;

        void setMdmEnabled(boolean enabled) {
            this.mdmEnabled = enabled;
        }

        @Override
        public Optional<TerminalCapabilitySwitchRecord> resolveEffectiveSwitch(
                String terminalType, String capabilityCode, Map<String, String> orderedScopes) {
            return Optional.of(new TerminalCapabilitySwitchRecord(
                    "tcs_mdm", "*", capabilityCode, "global", "global", mdmEnabled, null, Instant.now(), Instant.now()));
        }

        @Override
        public TerminalDeviceRecord save(TerminalDeviceRecord device) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TerminalDeviceRecord> findByDeviceCode(String deviceCode) {
            return Optional.empty();
        }

        @Override
        public Optional<TerminalDeviceRecord> findById(String id) {
            return Optional.empty();
        }

        @Override
        public Optional<TerminalDeviceRecord> findByDeviceId(String deviceId) {
            return Optional.empty();
        }

        @Override
        public Optional<DeviceCredentialRecord> findDeviceCredentialByDeviceId(String deviceId) {
            return Optional.empty();
        }

        @Override
        public DeviceCredentialRecord saveDeviceCredential(DeviceCredentialRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TerminalDeviceStatusEventRecord saveStatusEvent(TerminalDeviceStatusEventRecord event) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<TerminalCapabilitySwitchRecord> listCapabilitySwitches() {
            return List.of();
        }

        @Override
        public Optional<TerminalCapabilitySwitchRecord> findCapabilitySwitchById(String id) {
            return Optional.empty();
        }

        @Override
        public Optional<TerminalCapabilitySwitchRecord> findCapabilitySwitch(
                String terminalType, String capabilityCode, String scopeId) {
            return Optional.empty();
        }

        @Override
        public TerminalCapabilitySwitchRecord saveCapabilitySwitch(TerminalCapabilitySwitchRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<TerminalKioskConfigRecord> findKioskConfigByDeviceId(String deviceId) {
            return Optional.empty();
        }

        @Override
        public TerminalKioskConfigRecord saveKioskConfig(TerminalKioskConfigRecord record) {
            throw new UnsupportedOperationException();
        }

        @Override
        public KioskHeartbeatRecord saveKioskHeartbeat(KioskHeartbeatRecord record) {
            throw new UnsupportedOperationException();
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
