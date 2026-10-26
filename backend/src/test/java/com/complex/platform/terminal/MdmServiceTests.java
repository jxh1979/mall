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

    // ── P0#9 服务端违规检测 ──────────────────────────────────────────────────
    @Test
    void detectViolationsProducesViolationsForNonWhitelistedAppAndSite() {
        mdmRepository.management.put("device_d", new MdmDeviceManagementRecord(
                "device_d", "staff_work_phone", "bound", "compliant", "applied", Instant.now(), Instant.now()));
        // 仅企微 app + 一个站点在白名单。
        service.upsertAppWhitelist("mdm_app_wecom", new MdmAppWhitelistRequest(
                "wl-app", "n", List.of(), "com.tencent.wework", "WeCom", "global", "global", true, null));
        service.upsertSiteWhitelist("mdm_site_ok", new MdmSiteWhitelistRequest(
                "wl-site", "n", List.of(), "ok.example.com", "ok", "global", "global", true, null));

        MdmViolationDetectionResult result = service.detectViolations(
                "detect-1", "device_d",
                List.of("com.tencent.wework", "com.evil.app"),
                List.of("ok.example.com", "evil.example.com"));

        assertThat(result.violationCount()).isEqualTo(2);
        assertThat(result.violations())
                .extracting(MdmPolicyViolationRecord::violationType)
                .containsExactlyInAnyOrder("unauthorized_app", "non_whitelisted_site");
        assertThat(mdmRepository.violations).hasSize(2);
        // 命中违规即把设备纳管状态降级为 violation。
        assertThat(mdmRepository.management.get("device_d").managementStatus()).isEqualTo("violation");
    }

    @Test
    void detectViolationsEmptyWhenAllWhitelisted() {
        service.upsertAppWhitelist("mdm_app_wecom", new MdmAppWhitelistRequest(
                "wl-app", "n", List.of(), "com.tencent.wework", "WeCom", "global", "global", true, null));

        MdmViolationDetectionResult result = service.detectViolations(
                "detect-clean", "device_clean", List.of("com.tencent.wework"), List.of());

        assertThat(result.violationCount()).isZero();
        assertThat(mdmRepository.violations).isEmpty();
    }

    // ── P0#9 违规处置闭环 ────────────────────────────────────────────────────
    @Test
    void handleViolationBlocksDeviceAndRevokesCredential() {
        mdmRepository.management.put("device_h", new MdmDeviceManagementRecord(
                "device_h", "staff_work_phone", "bound", "violation", "failed", Instant.now(), Instant.now()));
        MdmPolicyViolationRecord saved = service.reportPolicyViolation(new MdmPolicyViolationRequest(
                "vio-h", "note", List.of(), "device_h", "root_detected", "root"));

        MdmPolicyViolationRecord handled = service.handleViolation(
                saved.id(), "device_blocked", "confiscated", "handle-h");

        assertThat(handled.handled()).isTrue();
        assertThat(handled.disposition()).isEqualTo("device_blocked");
        assertThat(handled.blockedAt()).isNotNull();
        assertThat(mdmRepository.management.get("device_h").managementStatus()).isEqualTo("blocked");
        assertThat(mdmRepository.findViolationById(saved.id()))
                .get()
                .extracting(MdmPolicyViolationRecord::handled)
                .isEqualTo(true);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("mdm.violation.handled");
    }

    @Test
    void handleViolationAcknowledgeDoesNotBlockDevice() {
        mdmRepository.management.put("device_a", new MdmDeviceManagementRecord(
                "device_a", "staff_work_phone", "bound", "violation", "failed", Instant.now(), Instant.now()));
        MdmPolicyViolationRecord saved = service.reportPolicyViolation(new MdmPolicyViolationRequest(
                "vio-a", "note", List.of(), "device_a", "other", "x"));

        service.handleViolation(saved.id(), "acknowledged", "logged", "handle-a");

        assertThat(mdmRepository.management.get("device_a").managementStatus()).isEqualTo("violation");
        assertThat(mdmRepository.findViolationById(saved.id()))
                .get()
                .satisfies(v -> {
                    assertThat(v.handled()).isTrue();
                    assertThat(v.blockedAt()).isNull();
                });
    }

    @Test
    void handleViolationRejectsAlreadyHandled() {
        MdmPolicyViolationRecord saved = service.reportPolicyViolation(new MdmPolicyViolationRequest(
                "vio-twice", "note", List.of(), "device_t", "other", "x"));
        service.handleViolation(saved.id(), "dismissed", "false positive", "handle-1");

        assertThatThrownBy(() -> service.handleViolation(saved.id(), "acknowledged", "again", "handle-2"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VIOLATION_ALREADY_HANDLED");
    }

    @Test
    void markViolationHandledReturnsZeroRowsWhenAlreadyHandled() {
        // P2 并发缺口②红线：条件 UPDATE（AND handled=FALSE）对已处置 violation 返回 0 行。
        // 复现"不同 idempotencyKey 并发重入"——第二笔即使绕过 Service 的 existing.handled() 串行校验，
        // 也会因条件 UPDATE 命中 0 行被兜底拒绝，避免重复 block/撤凭证。
        MdmPolicyViolationRecord saved = service.reportPolicyViolation(new MdmPolicyViolationRequest(
                "vio-cas", "note", List.of(), "device_cas", "other", "x"));

        int first = mdmRepository.markViolationHandled(saved.id(), "dismissed", "first", null);
        int second = mdmRepository.markViolationHandled(saved.id(), "acknowledged", "second", null);

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        // 仅第一笔处置生效：disposition 仍为首笔，未被第二笔覆盖。
        assertThat(mdmRepository.findViolationById(saved.id()))
                .get()
                .extracting(MdmPolicyViolationRecord::disposition)
                .isEqualTo("dismissed");
    }

    @Test
    void handleViolationDeviceBlockedTwiceDoesNotRepeatBlock() {
        // device_blocked 处置后二次重入：第二笔被条件 UPDATE 行数兜底拒绝，不再重复 block/撤凭证。
        mdmRepository.management.put("device_2x", new MdmDeviceManagementRecord(
                "device_2x", "staff_work_phone", "bound", "violation", "failed", Instant.now(), Instant.now()));
        MdmPolicyViolationRecord saved = service.reportPolicyViolation(new MdmPolicyViolationRequest(
                "vio-2x", "note", List.of(), "device_2x", "root_detected", "root"));

        service.handleViolation(saved.id(), "device_blocked", "first", "handle-2x-a");

        assertThatThrownBy(() -> service.handleViolation(saved.id(), "device_blocked", "second", "handle-2x-b"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VIOLATION_ALREADY_HANDLED");
        // 设备仍为 blocked（首笔结果），第二笔未重复联动。
        assertThat(mdmRepository.management.get("device_2x").managementStatus()).isEqualTo("blocked");
    }

    @Test
    void mdmEnabledHardBlocksLostDeviceRegardlessOfManaged() {
        terminalRepository.setMdmEnabled(false);
        mdmRepository.management.put("device_lost", new MdmDeviceManagementRecord(
                "device_lost", "staff_work_phone", "lost", "lost", null, null, null));

        assertThatThrownBy(() -> service.requireManagedForHighRiskAction("device_lost", "terminal.device.unbind"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("DEVICE_BLOCKED");
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
    @Test
    void missingMdmSwitchRowMeansDisabled() {
        // PGM 第 742 行「开关缺行不得视为开启」。
        terminalRepository.removeMdmRow();

        assertThat(service.isMdmEnabled()).isFalse();
    }

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
        public boolean isAppWhitelisted(String packageName) {
            return apps.values().stream()
                    .anyMatch(r -> r.enabled() && packageName.equals(r.packageName()));
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
        public boolean isSiteWhitelisted(String domain) {
            return sites.values().stream()
                    .anyMatch(r -> r.enabled() && domain.equals(r.domain()));
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
        public Optional<MdmPolicyViolationRecord> findViolationById(String id) {
            return violations.stream().filter(v -> v.id().equals(id)).findFirst();
        }

        @Override
        public int markViolationHandled(String id, String disposition, String handledNote, Instant blockedAt) {
            for (int i = 0; i < violations.size(); i++) {
                MdmPolicyViolationRecord v = violations.get(i);
                if (v.id().equals(id)) {
                    // 条件 UPDATE 语义：仅当 handled=FALSE 才生效（受影响 1 行）；
                    // 已处置（handled=true）则受影响 0 行，复现并发重复处置被拒。
                    if (v.handled()) {
                        return 0;
                    }
                    violations.set(i, new MdmPolicyViolationRecord(
                            v.id(), v.deviceId(), v.violationType(), v.detail(), v.occurredAt(),
                            true, handledNote, disposition, blockedAt, v.createdAt()));
                    return 1;
                }
            }
            return 0;
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
        private boolean noRow;

        void setMdmEnabled(boolean enabled) {
            this.mdmEnabled = enabled;
        }

        void removeMdmRow() {
            this.noRow = true;
        }

        @Override
        public Optional<TerminalCapabilitySwitchRecord> resolveEffectiveSwitch(
                String terminalType, String capabilityCode, Map<String, String> orderedScopes) {
            if (noRow) {
                return Optional.empty();
            }
            return Optional.of(new TerminalCapabilitySwitchRecord(
                    "tcs_mdm", "*", capabilityCode, "global", "global", mdmEnabled, null, Instant.now(), Instant.now()));
        }

        @Override
        public TerminalDeviceRecord save(TerminalDeviceRecord device) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int compareAndSetStatus(String id, String expectedFrom, TerminalDeviceRecord updated) {
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
        public KioskHeartbeatRecord saveKioskHeartbeat(KioskHeartbeatRecord record) {
            throw new UnsupportedOperationException();
        }
    }
}
