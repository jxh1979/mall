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
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

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
    private static final String VIOLATION_HANDLE = "mdm.violation.handle";
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;

    /** P0#9 处置类型（与 V107 chk_mdm_policy_violations_disposition 对齐）。 */
    private static final Set<String> DISPOSITIONS = Set.of("acknowledged", "device_blocked", "dismissed");

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
                            .orElseThrow(() -> ApiException.notFound("DEVICE_NOT_FOUND", "设备不存在或已被删除"));
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
        // P0#9：blocked（违规处置封禁）/ lost（挂失）为显式终止态，无论 MDM 开关如何都硬拒绝，
        //       不走"开关关=代偿"口径——这是已发生的处置/事件，不可被代偿放行。
        boolean hardBlocked = "blocked".equals(managementStatus) || "lost".equals(managementStatus);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("device_id", id);
        payload.put("action_type", actionType);
        payload.put("mdm_enabled", enabled);
        payload.put("management_status", managementStatus);
        payload.put("compensated", !enabled && !hardBlocked);

        if (hardBlocked) {
            try {
                auditEventService.recordSuccess(
                        "device.policy_violation", DEVICE_MANAGE, "terminal_device", id, null, null, payload);
            } catch (RuntimeException ignored) {
                // 留痕失败不得掩盖拒绝结果
            }
            throw ApiException.forbidden("DEVICE_BLOCKED",
                    "设备已被封禁或挂失：" + id);
        }

        if (enabled && !managed) {
            try {
                auditEventService.recordSuccess(
                        "device.policy_violation", DEVICE_MANAGE, "terminal_device", id, null, null, payload);
            } catch (RuntimeException ignored) {
                // 留痕失败不得掩盖拒绝结果
            }
            throw ApiException.forbidden("DEVICE_NOT_MANAGED",
                    "设备未纳入 MDM 管理：" + id);
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
                            null,
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

    /**
     * P0#9 服务端违规检测（白名单表参与判定）：对上报的 app 包名 / 站点域名逐一比对启用中的白名单，
     * 未命中即产生 violation（unauthorized_app / non_whitelisted_site）并降级设备纳管状态。
     *
     * <p>与 {@link #reportPolicyViolation}（外部直接上报已判定的违规）互补：本方法把"判定"收口到服务端，
     * 不再仅依赖外部 POST 上报结果。返回本次检测命中的违规清单（命中为空=全部合规）。
     */
    @Transactional
    public MdmViolationDetectionResult detectViolations(
            String idempotencyKey, String deviceId, List<String> appPackages, List<String> siteDomains) {
        return executeWrite(
                "detectMdmViolations:" + requireText(deviceId, "device_id"),
                idempotencyKey,
                Map.of(
                        "device_id", deviceId,
                        "apps", appPackages == null ? List.of() : appPackages,
                        "sites", siteDomains == null ? List.of() : siteDomains),
                MdmViolationDetectionResult.class,
                "device.policy_violation",
                "device.policy_violation_failed",
                DEVICE_MANAGE,
                "mdm_policy_violation",
                () -> {
                    String id = requireText(deviceId, "device_id");
                    Instant now = Instant.now();
                    List<MdmPolicyViolationRecord> detected = new java.util.ArrayList<>();
                    if (appPackages != null) {
                        for (String pkg : appPackages) {
                            String packageName = trimToNull(pkg);
                            if (packageName != null && !mdmRepository.isAppWhitelisted(packageName)) {
                                detected.add(persistDetectedViolation(
                                        id, "unauthorized_app", "non-whitelisted app: " + packageName, now));
                            }
                        }
                    }
                    if (siteDomains != null) {
                        for (String site : siteDomains) {
                            String domain = trimToNull(site);
                            if (domain != null && !mdmRepository.isSiteWhitelisted(domain)) {
                                detected.add(persistDetectedViolation(
                                        id, "non_whitelisted_site", "non-whitelisted site: " + domain, now));
                            }
                        }
                    }
                    if (!detected.isEmpty()) {
                        mdmRepository.findDeviceManagement(id).ifPresent(device ->
                                mdmRepository.updateDeviceManagement(
                                        id, "violation", device.policyStatus(), device.lastPolicySyncAt(), device.enrolledAt()));
                    }
                    return new MdmViolationDetectionResult(id, detected.size(), List.copyOf(detected));
                },
                result -> result.deviceId()
        );
    }

    private MdmPolicyViolationRecord persistDetectedViolation(
            String deviceId, String violationType, String detail, Instant now) {
        return mdmRepository.saveViolation(new MdmPolicyViolationRecord(
                "mdm_vio_" + UUID.randomUUID().toString().replace("-", ""),
                deviceId,
                violationType,
                detail,
                now,
                false,
                null,
                null,
                null,
                now));
    }

    /**
     * P0#9 违规处置闭环（受 mdm.violation.handle）：把 handled 置真并落处置类型。
     * disposition=device_blocked 时联动：设备 management_status→blocked + 撤销设备凭证（消除"handled 无端点可置真"）。
     */
    @Transactional
    public MdmPolicyViolationRecord handleViolation(String violationId, String disposition, String handledNote, String idempotencyKey) {
        return executeWrite(
                "handleMdmViolation:" + requireText(violationId, "violation_id"),
                idempotencyKey,
                Map.of("violation_id", violationId, "disposition", disposition == null ? "" : disposition),
                MdmPolicyViolationRecord.class,
                "mdm.violation.handled",
                "mdm.violation.handle_failed",
                VIOLATION_HANDLE,
                "mdm_policy_violation",
                () -> {
                    String id = requireText(violationId, "violation_id");
                    String chosen = requireText(disposition, "disposition");
                    if (!DISPOSITIONS.contains(chosen)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "disposition is invalid");
                    }
                    MdmPolicyViolationRecord existing = mdmRepository.findViolationById(id)
                            .orElseThrow(() -> ApiException.notFound("VIOLATION_NOT_FOUND", "违规记录不存在或已被删除"));
                    if (existing.handled()) {
                        throw ApiException.conflict("VIOLATION_ALREADY_HANDLED", "该违规记录已处理");
                    }
                    Instant now = Instant.now();
                    Instant blockedAt = "device_blocked".equals(chosen) ? now : null;
                    // P2 并发缺口②修复：markViolationHandled 为条件 UPDATE（AND handled=FALSE），
                    // 受影响行数==1 才算本次处置成功。上面的 existing.handled() 校验给清晰错误码；
                    // 此处的行数==0 兜住"读后被并发处置"的真并发窗口（不同 idempotencyKey 重入），
                    // 杜绝重复 block/撤凭证——只有夺得该行的那一笔才会执行后续封禁联动。
                    int affected = mdmRepository.markViolationHandled(id, chosen, trimToNull(handledNote), blockedAt);
                    if (affected == 0) {
                        throw ApiException.conflict("VIOLATION_ALREADY_HANDLED", "该违规记录已处理");
                    }
                    if ("device_blocked".equals(chosen)) {
                        blockDeviceAndRevokeCredential(existing.deviceId(), now);
                    }
                    return new MdmPolicyViolationRecord(
                            existing.id(),
                            existing.deviceId(),
                            existing.violationType(),
                            existing.detail(),
                            existing.occurredAt(),
                            true,
                            trimToNull(handledNote),
                            chosen,
                            blockedAt,
                            existing.createdAt());
                },
                record -> record.id()
        );
    }

    /** 封禁联动：设备纳管状态→blocked 并撤销设备凭证（凭证撤销=kiosk 端会被踢出）。 */
    private void blockDeviceAndRevokeCredential(String deviceId, Instant now) {
        mdmRepository.findDeviceManagement(deviceId).ifPresent(device ->
                mdmRepository.updateDeviceManagement(
                        deviceId, "blocked", device.policyStatus(), device.lastPolicySyncAt(), device.enrolledAt()));
        terminalDeviceRepository.findDeviceCredentialByDeviceId(deviceId)
                .filter(credential -> "active".equals(credential.status()))
                .ifPresent(credential -> terminalDeviceRepository.saveDeviceCredential(new DeviceCredentialRecord(
                        credential.deviceId(),
                        credential.tokenSha256(),
                        "revoked",
                        credential.createdAt(),
                        now)));
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

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
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
