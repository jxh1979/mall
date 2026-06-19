package com.complex.platform.terminal;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.dto.MdmAppWhitelistRequest;
import com.complex.platform.terminal.dto.MdmDeviceEnrollRequest;
import com.complex.platform.terminal.dto.MdmPolicyViolationRequest;
import com.complex.platform.terminal.dto.MdmSiteWhitelistRequest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * R5 T9 MDM 自有侧服务（裁决⑨ 落地；MDM 开关默认关）。
 *
 * <p>能力开关 {@code managed_work_devices} 门控（默认关，见 V088）：
 * <ul>
 *   <li><b>开关关</b>：MDM 纳管/策略校验不阻断业务（代偿口径）——"未纳管禁用高风险动作"由
 *       终端能力开关 + 人工准入代偿（与 p0-action-event-evidence §3.1 / D10 收口一致）；
 *       纳管/策略字段、白名单、违规留痕仍可写入，为预留。</li>
 *   <li><b>开关开</b>：未纳管设备执行高风险动作被 {@link #requireManagedForHighRiskAction} 硬拒绝。</li>
 * </ul>
 */
@Service
public class MdmService {

    static final String MDM_CAPABILITY = "managed_work_devices";

    private static final String POLICY_MANAGE = "mdm.policy.manage";
    private static final String DEVICE_MANAGE = "mdm.device.manage";
    private static final String VIOLATION_VIEW = "mdm.violation.view";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;

    private static final Set<String> SCOPE_TYPES = Set.of("global", "project", "device_model");
    private static final Set<String> POLICY_STATUSES = Set.of("pending", "applied", "failed", "outdated");
    private static final Set<String> VIOLATION_TYPES = Set.of(
            "unauthorized_app", "non_whitelisted_site", "wecom_bypass",
            "root_detected", "policy_sync_failed", "other"
    );

    private final MdmRepository mdmRepository;
    private final TerminalDeviceRepository terminalDeviceRepository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    public MdmService(
            MdmRepository mdmRepository,
            TerminalDeviceRepository terminalDeviceRepository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService
    ) {
        this.mdmRepository = mdmRepository;
        this.terminalDeviceRepository = terminalDeviceRepository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
    }

    // ── 开关状态 ────────────────────────────────────────────────────────────
    /** MDM 自有侧是否启用（managed_work_devices 显式开启为开；无行=默认开启，但 V088 已显式插 false）。 */
    public boolean isMdmEnabled() {
        Map<String, String> scopes = new LinkedHashMap<>();
        scopes.put("global", "global");
        return terminalDeviceRepository
                .resolveEffectiveSwitch("*", MDM_CAPABILITY, scopes)
                .map(TerminalCapabilitySwitchRecord::enabled)
                .orElse(true);
    }

    // ── app 白名单 CRUD（受 mdm.policy.manage） ───────────────────────────────
    public PageResponse<MdmAppWhitelistRecord> listAppWhitelist() {
        return readPage("mdm.app_whitelist.viewed", "mdm.app_whitelist.view_failed",
                "mdm_app_whitelist", mdmRepository::listAppWhitelist);
    }

    @Transactional
    public MdmAppWhitelistRecord upsertAppWhitelist(String id, MdmAppWhitelistRequest request) {
        String entryId = id == null ? "mdm_app_" + UUID.randomUUID().toString().replace("-", "") : requireText(id, "id");
        return executeWrite(
                "upsertMdmAppWhitelist:" + entryId,
                request == null ? null : request.idempotencyKey(),
                request,
                MdmAppWhitelistRecord.class,
                "mdm.app_whitelist.changed",
                "mdm.app_whitelist.change_failed",
                POLICY_MANAGE,
                "mdm_app_whitelist",
                () -> {
                    MdmAppWhitelistRequest actual = requireRequest(request);
                    String scopeType = trimToDefault(actual.scopeType(), "global");
                    requireScopeType(scopeType);
                    Instant now = Instant.now();
                    Instant createdAt = mdmRepository.findAppWhitelistById(entryId)
                            .map(MdmAppWhitelistRecord::createdAt)
                            .orElse(now);
                    return mdmRepository.saveAppWhitelist(new MdmAppWhitelistRecord(
                            entryId,
                            requireText(actual.packageName(), "package_name"),
                            trimToNull(actual.appLabel()),
                            scopeType,
                            trimToDefault(actual.scopeId(), "global"),
                            actual.enabled() == null || actual.enabled(),
                            trimToNull(actual.reason()),
                            createdAt,
                            now
                    ));
                },
                record -> record.id()
        );
    }

    // ── site 白名单 CRUD（受 mdm.policy.manage） ──────────────────────────────
    public PageResponse<MdmSiteWhitelistRecord> listSiteWhitelist() {
        return readPage("mdm.site_whitelist.viewed", "mdm.site_whitelist.view_failed",
                "mdm_site_whitelist", mdmRepository::listSiteWhitelist);
    }

    @Transactional
    public MdmSiteWhitelistRecord upsertSiteWhitelist(String id, MdmSiteWhitelistRequest request) {
        String entryId = id == null ? "mdm_site_" + UUID.randomUUID().toString().replace("-", "") : requireText(id, "id");
        return executeWrite(
                "upsertMdmSiteWhitelist:" + entryId,
                request == null ? null : request.idempotencyKey(),
                request,
                MdmSiteWhitelistRecord.class,
                "mdm.site_whitelist.changed",
                "mdm.site_whitelist.change_failed",
                POLICY_MANAGE,
                "mdm_site_whitelist",
                () -> {
                    MdmSiteWhitelistRequest actual = requireRequest(request);
                    String scopeType = trimToDefault(actual.scopeType(), "global");
                    requireScopeType(scopeType);
                    Instant now = Instant.now();
                    Instant createdAt = mdmRepository.findSiteWhitelistById(entryId)
                            .map(MdmSiteWhitelistRecord::createdAt)
                            .orElse(now);
                    return mdmRepository.saveSiteWhitelist(new MdmSiteWhitelistRecord(
                            entryId,
                            requireText(actual.domain(), "domain"),
                            trimToNull(actual.description()),
                            scopeType,
                            trimToDefault(actual.scopeId(), "global"),
                            actual.enabled() == null || actual.enabled(),
                            trimToNull(actual.reason()),
                            createdAt,
                            now
                    ));
                },
                record -> record.id()
        );
    }

    // ── 设备纳管 / 策略同步（受 mdm.device.manage） ───────────────────────────
    @Transactional
    public MdmDeviceManagementRecord enrollDevice(MdmDeviceEnrollRequest request) {
        return executeWrite(
                "enrollMdmDevice",
                request == null ? null : request.idempotencyKey(),
                request,
                MdmDeviceManagementRecord.class,
                "mdm.policy_checked",
                "mdm.policy_check_failed",
                DEVICE_MANAGE,
                "terminal_device",
                () -> {
                    MdmDeviceEnrollRequest actual = requireRequest(request);
                    String deviceId = requireText(actual.deviceId(), "device_id");
                    MdmDeviceManagementRecord existing = mdmRepository.findDeviceManagement(deviceId)
                            .orElseThrow(() -> ApiException.notFound("DEVICE_NOT_FOUND", "device not found"));
                    String policyStatus = trimToDefault(actual.policyStatus(), "applied");
                    if (!POLICY_STATUSES.contains(policyStatus)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "policy_status is invalid");
                    }
                    // applied → compliant；failed/outdated → violation；pending → enrolled。
                    String managementStatus = switch (policyStatus) {
                        case "applied" -> "compliant";
                        case "failed", "outdated" -> "violation";
                        default -> "enrolled";
                    };
                    Instant now = Instant.now();
                    mdmRepository.updateDeviceManagement(deviceId, managementStatus, policyStatus, now, now);
                    return new MdmDeviceManagementRecord(
                            deviceId,
                            existing.terminalType(),
                            existing.status(),
                            managementStatus,
                            policyStatus,
                            now,
                            existing.enrolledAt() == null ? now : existing.enrolledAt()
                    );
                },
                record -> record.deviceId()
        );
    }

    /**
     * 高风险动作前置纳管校验（编程式门控）。
     * 开关关=代偿口径，纳管校验不阻断（由终端开关+人工准入代偿）；
     * 开关开=未纳管（management_status 非 compliant/enrolled）设备被硬拒绝。
     */
    public void requireManagedForHighRiskAction(String deviceId, String actionType) {
        String id = requireText(deviceId, "device_id");
        boolean enabled = isMdmEnabled();
        MdmDeviceManagementRecord device = mdmRepository.findDeviceManagement(id).orElse(null);
        String managementStatus = device == null ? "unmanaged" : device.managementStatus();
        boolean managed = "compliant".equals(managementStatus) || "enrolled".equals(managementStatus);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("device_id", id);
        payload.put("action_type", actionType);
        payload.put("mdm_enabled", enabled);
        payload.put("management_status", managementStatus);
        payload.put("compensated", !enabled);

        if (enabled && !managed) {
            try {
                auditEventService.recordSuccess(
                        "device.policy_violation", DEVICE_MANAGE, "terminal_device", id, null, null, payload);
            } catch (RuntimeException ignored) {
                // 留痕失败不得掩盖拒绝结果
            }
            throw ApiException.forbidden("DEVICE_NOT_MANAGED",
                    "device is not managed by MDM: " + id);
        }
        // 开关关（代偿）或已纳管：记录校验通过留痕，不阻断。
        try {
            auditEventService.recordSuccess(
                    "mdm.policy_checked", DEVICE_MANAGE, "terminal_device", id, null, null, payload);
        } catch (RuntimeException ignored) {
            // best-effort
        }
    }

    // ── 违规上报 / 留痕（写=mdm.device.manage；查=mdm.violation.view） ─────────
    @Transactional
    public MdmPolicyViolationRecord reportPolicyViolation(MdmPolicyViolationRequest request) {
        return executeWrite(
                "reportMdmPolicyViolation",
                request == null ? null : request.idempotencyKey(),
                request,
                MdmPolicyViolationRecord.class,
                "device.policy_violation",
                "device.policy_violation_failed",
                DEVICE_MANAGE,
                "mdm_policy_violation",
                () -> {
                    MdmPolicyViolationRequest actual = requireRequest(request);
                    String deviceId = requireText(actual.deviceId(), "device_id");
                    String violationType = requireText(actual.violationType(), "violation_type");
                    if (!VIOLATION_TYPES.contains(violationType)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "violation_type is invalid");
                    }
                    Instant now = Instant.now();
                    MdmPolicyViolationRecord saved = mdmRepository.saveViolation(new MdmPolicyViolationRecord(
                            "mdm_vio_" + UUID.randomUUID().toString().replace("-", ""),
                            deviceId,
                            violationType,
                            trimToNull(actual.detail()),
                            now,
                            false,
                            null,
                            now
                    ));
                    // 违规即把设备纳管状态降级为 violation（若设备存在）。
                    mdmRepository.findDeviceManagement(deviceId).ifPresent(device ->
                            mdmRepository.updateDeviceManagement(
                                    deviceId, "violation", device.policyStatus(), device.lastPolicySyncAt(), device.enrolledAt()));
                    return saved;
                },
                record -> record.id()
        );
    }

    public PageResponse<MdmPolicyViolationRecord> listViolations(String deviceId) {
        String id = trimToNull(deviceId);
        try {
            List<MdmPolicyViolationRecord> records = mdmRepository.listViolations(id);
            auditEventService.recordSuccess(
                    "mdm.violation.viewed", VIOLATION_VIEW, "mdm_policy_violation",
                    id == null ? "all" : id, null, null, Map.of("count", records.size()));
            return new PageResponse<>(records, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, records.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "mdm.violation.view_failed", VIOLATION_VIEW, "mdm_policy_violation",
                    id == null ? "all" : id, null, null, codeOf(exception));
            throw exception;
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────
    private <T> PageResponse<T> readPage(
            String successEvent, String failureEvent, String businessType, Supplier<List<T>> supplier) {
        try {
            List<T> records = supplier.get();
            auditEventService.recordSuccess(
                    successEvent, POLICY_MANAGE, businessType, "all", null, null, Map.of("count", records.size()));
            return new PageResponse<>(records, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, records.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent, POLICY_MANAGE, businessType, "all", null, null, codeOf(exception));
            throw exception;
        }
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action,
            java.util.function.Function<T, String> businessIdOf
    ) {
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        T response = action.get();
                        auditEventService.recordSuccess(
                                successEvent, permissionCode, businessType, businessIdOf.apply(response),
                                null, idempotencyKey, Map.of());
                        return response;
                    },
                    responseType
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent, permissionCode, businessType, null, null, idempotencyKey, codeOf(exception));
            throw exception;
        }
    }

    private void requireScopeType(String scopeType) {
        if (!SCOPE_TYPES.contains(scopeType)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "scope_type is invalid");
        }
    }

    private String requireText(String value, String fieldName) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
