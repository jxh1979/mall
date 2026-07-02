package com.complex.platform.terminal;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.terminal.dto.CapabilitySwitchRequest;
import com.complex.platform.terminal.dto.DeviceBindRequest;
import com.complex.platform.terminal.dto.DeviceLifecycleRequest;
import com.complex.platform.terminal.dto.DeviceReturnRequest;
import com.complex.platform.terminal.dto.DeviceUnbindRequest;
import com.complex.platform.terminal.dto.KioskHeartbeatRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TerminalDeviceService {

    private static final String BIND_PERMISSION = "terminal.device.bind";
    private static final String UNBIND_PERMISSION = "terminal.device.unbind";
    private static final String LIFECYCLE_PERMISSION = "terminal.device.lifecycle";
    // P0#9 设备生命周期：终态（不可再流转、拒绝任何业务动作/领用）。
    private static final Set<String> TERMINAL_STATUSES = Set.of("scrapped", "blocked");
    // P0#9：挂失/报废/送修/封禁设备拒绝领用（绑定）与高风险业务动作。
    private static final Set<String> NON_OPERATIONAL_STATUSES = Set.of("lost", "scrapped", "repair", "blocked");
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final Set<String> TERMINAL_TYPES = Set.of("staff_work_phone", "tablet", "kiosk");
    private static final Set<String> KIOSK_MODES = Set.of("ticketing", "service");
    private static final Set<String> KIOSK_STATUSES = Set.of("online", "degraded", "offline");
    private static final Set<String> DEVICE_CREDENTIAL_REJECTION_CODES = Set.of(
            "DEVICE_CREDENTIAL_REQUIRED",
            "DEVICE_CREDENTIAL_INVALID",
            "DEVICE_ACCESS_REJECTED"
    );
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TerminalDeviceRepository repository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final MdmService mdmService;

    public TerminalDeviceService(
            TerminalDeviceRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            MdmService mdmService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.mdmService = mdmService;
    }

    @Transactional
    public DeviceBindingResponse bindDevice(DeviceBindRequest request) {
        return executeWrite(
                "bindTerminalDevice",
                request == null ? null : request.idempotencyKey(),
                request,
                DeviceBindingResponse.class,
                "terminal.device.bound",
                "terminal.device.bind_failed",
                BIND_PERMISSION,
                () -> bind(request),
                this::withoutDeviceToken
        );
    }

    @Transactional
    public DeviceBindingResponse returnDevice(DeviceReturnRequest request) {
        return executeWrite(
                "returnTerminalDevice",
                request == null ? null : request.idempotencyKey(),
                request,
                DeviceBindingResponse.class,
                "terminal.device.returned",
                "terminal.device.returned_failed",
                BIND_PERMISSION,
                () -> returnBoundDevice(request)
        );
    }

    @Transactional
    public DeviceBindingResponse unbindDevice(String id, DeviceUnbindRequest request) {
        return executeWrite(
                "unbindTerminalDevice:" + requireText(id, "id"),
                request == null ? null : request.idempotencyKey(),
                request,
                DeviceBindingResponse.class,
                "terminal.device.unbound",
                "terminal.device.unbind_failed",
                UNBIND_PERMISSION,
                () -> unbind(id, request)
        );
    }

    // ── P0#9 设备生命周期状态机（送修 / 挂失 / 找回 / 报废） ────────────────────
    @Transactional
    public DeviceBindingResponse repairDevice(String id, DeviceLifecycleRequest request) {
        return transitionLifecycle(
                id, request, "repair", Set.of("bound", "returned"),
                "terminal.device.repaired", "terminal.device.repair_failed");
    }

    @Transactional
    public DeviceBindingResponse markDeviceLost(String id, DeviceLifecycleRequest request) {
        return transitionLifecycle(
                id, request, "lost", Set.of("bound", "returned", "repair", "unbound"),
                "terminal.device.lost", "terminal.device.lost_failed");
    }

    @Transactional
    public DeviceBindingResponse markDeviceFound(String id, DeviceLifecycleRequest request) {
        return transitionLifecycle(
                id, request, "found", Set.of("lost"),
                "terminal.device.found", "terminal.device.found_failed");
    }

    @Transactional
    public DeviceBindingResponse scrapDevice(String id, DeviceLifecycleRequest request) {
        return transitionLifecycle(
                id, request, "scrapped", Set.of("bound", "returned", "unbound", "repair", "lost", "found"),
                "terminal.device.scrapped", "terminal.device.scrap_failed");
    }

    private DeviceBindingResponse transitionLifecycle(
            String id,
            DeviceLifecycleRequest request,
            String targetStatus,
            Set<String> allowedFrom,
            String successEvent,
            String failureEvent
    ) {
        String deviceId = requireText(id, "id");
        return executeWrite(
                "lifecycle:" + targetStatus + ":" + deviceId,
                request == null ? null : request.idempotencyKey(),
                request,
                DeviceBindingResponse.class,
                successEvent,
                failureEvent,
                LIFECYCLE_PERMISSION,
                () -> applyLifecycle(id, request, targetStatus, allowedFrom, successEvent)
        );
    }

    private DeviceBindingResponse applyLifecycle(
            String id,
            DeviceLifecycleRequest request,
            String targetStatus,
            Set<String> allowedFrom,
            String eventType
    ) {
        DeviceLifecycleRequest actual = requireRequest(request);
        String reason = requireText(actual.reason(), "reason");
        TerminalDeviceRecord existing = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("DEVICE_NOT_FOUND", "device not found"));
        // 终态（scrapped/blocked）封锁：不可再流转。
        if (TERMINAL_STATUSES.contains(existing.status())) {
            throw ApiException.conflict("DEVICE_TERMINAL_STATUS",
                    "device is in terminal status " + existing.status() + " and cannot transition");
        }
        if (!allowedFrom.contains(existing.status())) {
            throw ApiException.conflict("INVALID_DEVICE_STATUS",
                    "device status " + existing.status() + " cannot transition to " + targetStatus);
        }
        Instant now = Instant.now();
        TerminalDeviceRecord updated = new TerminalDeviceRecord(
                existing.id(),
                existing.deviceId(),
                existing.deviceCode(),
                existing.terminalType(),
                existing.bindingScope(),
                existing.boundSpaceId(),
                existing.mdmDeviceRef(),
                existing.kioskMode(),
                existing.merchantId(),
                existing.venueId(),
                existing.areaId(),
                existing.locationPointId(),
                firstNonBlank(actual.staffId(), existing.staffId()),
                targetStatus,
                existing.boundAt(),
                existing.returnedAt(),
                existing.unboundAt(),
                existing.returnReason(),
                existing.unbindReason(),
                existing.createdAt(),
                now
        );
        // P2 并发缺口①修复：状态流转改条件 UPDATE（WHERE id AND status=:expectedFrom），
        // 按受影响行数判冲突。上面的前态校验给出清晰错误码（INVALID/TERMINAL_STATUS）；
        // 此处的行数==0 兜住"读后被并发改过"的真并发窗口（如 found 与 scrap 同时），
        // 受影响 0 行即抛冲突，保证仅一个流转成功、终态不可逆。
        int affected = repository.compareAndSetStatus(existing.id(), existing.status(), updated);
        if (affected == 0) {
            throw ApiException.conflict("DEVICE_TRANSITION_CONFLICT",
                    "device status changed concurrently; transition to " + targetStatus + " rejected");
        }
        TerminalDeviceRecord saved = updated;
        // 挂失/报废/封禁：撤销 kiosk 凭证（设备被踢出）。
        if (NON_OPERATIONAL_STATUSES.contains(targetStatus) && !"repair".equals(targetStatus)) {
            revokeDeviceCredential(saved.deviceId(), now);
        }
        repository.saveStatusEvent(new TerminalDeviceStatusEventRecord(
                "dev_evt_" + UUID.randomUUID().toString().replace("-", ""),
                saved.id(),
                saved.deviceCode(),
                eventType,
                saved.status(),
                reason,
                saved.staffId(),
                now
        ));
        return toResponse(saved, null);
    }

    public PageResponse<TerminalCapabilitySwitchRecord> listCapabilitySwitches() {
        try {
            List<TerminalCapabilitySwitchRecord> records = repository.listCapabilitySwitches();
            auditEventService.recordSuccess(
                    "terminal_switch.viewed",
                    "terminal.switch.view",
                    "terminal_capability_switch",
                    "all",
                    null,
                    null,
                    Map.of("count", records.size())
            );
            return new PageResponse<>(records, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, records.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "terminal_switch.view_failed",
                    "terminal.switch.view",
                    "terminal_capability_switch",
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    /**
     * 3.19 二期：某终端的已关闭开关行（前端菜单 gating 用，只回 enabled=false 的行）。
     * terminalType 空/未知按 "web" 兜底；配置表全量 ≤ 数百行，内存过滤足够。
     */
    public PageResponse<TerminalCapabilitySwitchRecord> listDisabledSwitchesFor(String terminalType) {
        String type = terminalType == null || terminalType.isBlank() ? "web" : terminalType.trim();
        List<TerminalCapabilitySwitchRecord> disabled = repository.listCapabilitySwitches().stream()
                .filter(record -> !record.enabled())
                .filter(record -> type.equals(record.terminalType()) || "*".equals(record.terminalType()))
                .toList();
        return new PageResponse<>(disabled, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, disabled.size());
    }

    @Transactional
    public TerminalCapabilitySwitchRecord updateCapabilitySwitch(String id, CapabilitySwitchRequest request) {
        String switchId = requireText(id, "id");
        return executeWrite(
                "updateCapabilitySwitch:" + switchId,
                request == null ? null : request.idempotencyKey(),
                request,
                TerminalCapabilitySwitchRecord.class,
                "terminal_switch.changed",
                "terminal_switch.change_failed",
                "terminal.switch.manage",
                "terminal_capability_switch",
                () -> {
                    CapabilitySwitchRequest actual = requireRequest(request);
                    if (actual.enabled() == null) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "enabled is required");
                    }
                    Instant now = Instant.now();
                    // 3.19 二期修复：既有行 = 纯开/关切换，身份字段（端/能力/scope）保持原行——
                    // 旧实现按请求重推导 scope_type（非 global 一律改成 device），会把 V198 的 module 行
                    // 改写成 device 行，守卫按模块解析不到 → 关了开关接口照放行（活证据抓到的真 bug）。
                    java.util.Optional<TerminalCapabilitySwitchRecord> existing =
                            repository.findCapabilitySwitchById(switchId);
                    if (existing.isPresent()) {
                        TerminalCapabilitySwitchRecord current = existing.get();
                        return repository.saveCapabilitySwitch(new TerminalCapabilitySwitchRecord(
                                current.id(),
                                current.terminalType(),
                                current.capabilityCode(),
                                current.scopeType(),
                                current.scopeId(),
                                actual.enabled(),
                                trimToNull(actual.operatorNote()),
                                current.createdAt(),
                                now
                        ));
                    }
                    // 新建行：沿用既有推导（kiosk 设备级开关为主的历史口径）。
                    String terminalType = trimToDefault(actual.terminalType(), "kiosk");
                    String capabilityCode = requireText(actual.capabilityCode(), "capability_code");
                    String scopeId = trimToDefault(actual.scopeId(), "global");
                    String scopeType = "global".equals(scopeId) ? "global" : "device";
                    return repository.saveCapabilitySwitch(new TerminalCapabilitySwitchRecord(
                            switchId,
                            terminalType,
                            capabilityCode,
                            scopeType,
                            scopeId,
                            actual.enabled(),
                            trimToNull(actual.operatorNote()),
                            now,
                            now
                    ));
                }
        );
    }

    public KioskConfigResponse getKioskConfig(String deviceId, String deviceToken) {
        String id = requireText(deviceId, "device_id");
        try {
            requireBoundKioskDeviceCredential(id, deviceToken);
            TerminalKioskConfigRecord config = repository.findKioskConfigByDeviceId(id)
                    .orElseThrow(() -> ApiException.notFound("DEVICE_NOT_FOUND", "kiosk device not found"));
            boolean ticketingEnabled = "ticketing".equals(config.kioskMode())
                    && config.ticketingEnabled()
                    && repository.findCapabilitySwitch("kiosk", "ticketing", id)
                    .map(TerminalCapabilitySwitchRecord::enabled)
                    .orElse(true);
            KioskConfigResponse response = new KioskConfigResponse(
                    config.deviceId(),
                    config.kioskMode(),
                    ticketingEnabled,
                    config.boundScope(),
                    config.config(),
                    config.updatedAt()
            );
            auditEventService.recordSuccess(
                    "terminal.kiosk_config_viewed",
                    "not_applicable",
                    "terminal_kiosk_config",
                    id,
                    null,
                    null,
                    Map.of("kiosk_mode", config.kioskMode(), "ticketing_enabled", ticketingEnabled)
            );
            return response;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "terminal.access_rejected",
                    "not_applicable",
                    "terminal_kiosk_config",
                    id,
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    @Transactional
    public KioskHeartbeatRecord recordKioskHeartbeat(String deviceId, String deviceToken, KioskHeartbeatRequest request) {
        String id = requireText(deviceId, "device_id");
        try {
            requireBoundKioskDeviceCredential(id, deviceToken);
            KioskHeartbeatRequest actual = requireRequest(request);
            if (!id.equals(requireText(actual.deviceId(), "device_id"))) {
                throw ApiException.badRequest("VALIDATION_FAILED", "device_id path and body must match");
            }
            if (!"kiosk".equals(requireText(actual.terminalType(), "terminal_type"))) {
                throw ApiException.badRequest("VALIDATION_FAILED", "terminal_type must be kiosk");
            }
            String status = requireText(actual.status(), "status");
            if (!KIOSK_STATUSES.contains(status)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "status is invalid");
            }
            TerminalKioskConfigRecord config = repository.findKioskConfigByDeviceId(id)
                    .orElseThrow(() -> ApiException.notFound("DEVICE_NOT_FOUND", "kiosk device not found"));
            String kioskMode = trimToDefault(actual.kioskMode(), config.kioskMode());
            Instant now = Instant.now();
            KioskHeartbeatRecord heartbeat = repository.saveKioskHeartbeat(new KioskHeartbeatRecord(
                    id,
                    status,
                    trimToNull(actual.appVersion()),
                    actual.capabilities() == null ? List.of() : List.copyOf(actual.capabilities()),
                    trimToNull(actual.lastErrorCode()),
                    kioskMode,
                    now
            ));
            auditEventService.recordSuccess(
                    "terminal.kiosk_heartbeat",
                    "not_applicable",
                    "terminal_kiosk_heartbeat",
                    id,
                    null,
                    null,
                    Map.of("status", status, "kiosk_mode", kioskMode)
            );
            return heartbeat;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    isDeviceCredentialRejection(exception) ? "terminal.access_rejected" : "terminal.kiosk_heartbeat_failed",
                    "not_applicable",
                    "terminal_kiosk_heartbeat",
                    id,
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private DeviceBindingResponse bind(DeviceBindRequest request) {
        DeviceBindRequest actual = requireRequest(request);
        String deviceId = requireText(actual.deviceId(), "device_id");
        String terminalType = requireText(actual.terminalType(), "terminal_type");
        if (!TERMINAL_TYPES.contains(terminalType)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "terminal_type is invalid");
        }
        String kioskMode = trimToNull(actual.kioskMode());
        if (kioskMode != null && (!"kiosk".equals(terminalType) || !KIOSK_MODES.contains(kioskMode))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "kiosk_mode is invalid");
        }
        // P0#9：挂失/报废/送修/封禁设备拒绝领用（重新绑定）——bind 走 device_id upsert，
        //       若不拦截，报废设备会被静默复活。
        repository.findByDeviceId(deviceId).ifPresent(existing -> {
            if (NON_OPERATIONAL_STATUSES.contains(existing.status())) {
                throw ApiException.conflict("INVALID_DEVICE_STATUS",
                        "device is " + existing.status() + " and cannot be bound");
            }
        });

        Instant now = Instant.now();
        TerminalDeviceRecord record = new TerminalDeviceRecord(
                "dev_" + slug(deviceId),
                deviceId,
                deviceId,
                terminalType,
                requireText(actual.bindingScope(), "binding_scope"),
                trimToNull(actual.boundSpaceId()),
                trimToNull(actual.mdmDeviceRef()),
                kioskMode,
                null,
                null,
                null,
                trimToNull(actual.boundSpaceId()),
                null,
                "bound",
                now,
                null,
                null,
                null,
                null,
                now,
                now
        );
        String deviceToken = "kiosk".equals(record.terminalType()) ? generateDeviceToken() : null;
        TerminalDeviceRecord saved = repository.save(record);
        if (deviceToken != null) {
            repository.saveDeviceCredential(new DeviceCredentialRecord(
                    saved.deviceId(),
                    sha256(deviceToken),
                    "active",
                    now,
                    now
            ));
        }
        repository.saveStatusEvent(new TerminalDeviceStatusEventRecord(
                "dev_evt_" + UUID.randomUUID().toString().replace("-", ""),
                saved.id(),
                saved.deviceCode(),
                "terminal.device.bound",
                saved.status(),
                null,
                saved.staffId(),
                now
        ));
        return toResponse(saved, deviceToken);
    }

    private DeviceBindingResponse returnBoundDevice(DeviceReturnRequest request) {
        DeviceReturnRequest actual = requireRequest(request);
        String deviceCode = requireText(actual.deviceCode(), "device_code");
        if (Boolean.TRUE.equals(actual.hasPendingTasks())) {
            throw ApiException.conflict("RETURN_BLOCKED_PENDING_TASKS", "device has pending tasks");
        }
        TerminalDeviceRecord existing = repository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> ApiException.notFound("DEVICE_NOT_FOUND", "device not found"));
        if (!"bound".equals(existing.status())) {
            throw ApiException.conflict("INVALID_DEVICE_STATUS", "device must be bound to return");
        }
        Instant now = Instant.now();
        TerminalDeviceRecord returned = new TerminalDeviceRecord(
                existing.id(),
                existing.deviceId(),
                existing.deviceCode(),
                existing.terminalType(),
                existing.bindingScope(),
                existing.boundSpaceId(),
                existing.mdmDeviceRef(),
                existing.kioskMode(),
                existing.merchantId(),
                existing.venueId(),
                existing.areaId(),
                existing.locationPointId(),
                firstNonBlank(actual.staffId(), existing.staffId()),
                "returned",
                existing.boundAt(),
                now,
                existing.unboundAt(),
                trimToNull(actual.reason()),
                existing.unbindReason(),
                existing.createdAt(),
                now
        );
        TerminalDeviceRecord saved = repository.save(returned);
        revokeDeviceCredential(saved.deviceId(), now);
        repository.saveStatusEvent(new TerminalDeviceStatusEventRecord(
                "dev_evt_" + UUID.randomUUID().toString().replace("-", ""),
                saved.id(),
                saved.deviceCode(),
                "terminal.device.returned",
                saved.status(),
                trimToNull(actual.reason()),
                saved.staffId(),
                now
        ));
        return toResponse(saved, null);
    }

    private DeviceBindingResponse unbind(String id, DeviceUnbindRequest request) {
        DeviceUnbindRequest actual = requireRequest(request);
        String reason = requireText(actual.reason(), "reason");
        TerminalDeviceRecord existing = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("DEVICE_NOT_FOUND", "device not found"));
        // P0#9 MDM 门控接入点（terminal 自身高风险端点·解绑）：开关开时未纳管硬拒绝；
        //       blocked/lost 设备无论开关都硬拒绝（消除 requireManagedForHighRiskAction dead code）。
        mdmService.requireManagedForHighRiskAction(existing.deviceId(), "terminal.device.unbind");
        if ("unbound".equals(existing.status())) {
            throw ApiException.conflict("INVALID_DEVICE_STATUS", "device is already unbound");
        }
        Instant now = Instant.now();
        TerminalDeviceRecord unbound = new TerminalDeviceRecord(
                existing.id(),
                existing.deviceId(),
                existing.deviceCode(),
                existing.terminalType(),
                existing.bindingScope(),
                existing.boundSpaceId(),
                existing.mdmDeviceRef(),
                existing.kioskMode(),
                existing.merchantId(),
                existing.venueId(),
                existing.areaId(),
                existing.locationPointId(),
                existing.staffId(),
                "unbound",
                existing.boundAt(),
                existing.returnedAt(),
                now,
                existing.returnReason(),
                reason,
                existing.createdAt(),
                now
        );
        TerminalDeviceRecord saved = repository.save(unbound);
        revokeDeviceCredential(saved.deviceId(), now);
        repository.saveStatusEvent(new TerminalDeviceStatusEventRecord(
                "dev_evt_" + UUID.randomUUID().toString().replace("-", ""),
                saved.id(),
                saved.deviceCode(),
                "terminal.device.unbound",
                saved.status(),
                reason,
                saved.staffId(),
                now
        ));
        return toResponse(saved, null);
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            Supplier<T> action
    ) {
        return executeWrite(
                operationId,
                idempotencyKey,
                request,
                responseType,
                successEvent,
                failureEvent,
                permissionCode,
                action,
                Function.identity()
        );
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            Supplier<T> action,
            Function<T, T> storedResponseTransformer
    ) {
        return executeWrite(
                operationId,
                idempotencyKey,
                request,
                responseType,
                successEvent,
                failureEvent,
                permissionCode,
                "terminal_device",
                action,
                storedResponseTransformer
        );
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
            Supplier<T> action
    ) {
        return executeWrite(
                operationId,
                idempotencyKey,
                request,
                responseType,
                successEvent,
                failureEvent,
                permissionCode,
                businessType,
                action,
                Function.identity()
        );
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
            Function<T, T> storedResponseTransformer
    ) {
        try {
            return idempotencyService.execute(
                    operationId,
                    idempotencyKey,
                    request,
                    () -> {
                        T response = action.get();
                        auditEventService.recordSuccess(
                                successEvent,
                                permissionCode,
                                businessType,
                                businessId(response),
                                null,
                                idempotencyKey,
                                Map.of()
                        );
                        return response;
                    },
                    responseType,
                    storedResponseTransformer
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    failureEvent,
                    permissionCode,
                    businessType,
                    null,
                    null,
                    idempotencyKey,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    private DeviceBindingResponse toResponse(TerminalDeviceRecord record, String deviceToken) {
        return new DeviceBindingResponse(
                record.id(),
                record.terminalType(),
                record.deviceCode(),
                record.mdmDeviceRef(),
                record.kioskMode(),
                record.boundSpaceId(),
                record.merchantId(),
                record.venueId(),
                record.areaId(),
                record.locationPointId(),
                record.staffId(),
                record.status(),
                record.updatedAt(),
                deviceToken
        );
    }

    private DeviceBindingResponse withoutDeviceToken(DeviceBindingResponse response) {
        if (response == null || response.deviceToken() == null) {
            return response;
        }
        return new DeviceBindingResponse(
                response.id(),
                response.deviceType(),
                response.deviceCode(),
                response.mdmDeviceRef(),
                response.kioskMode(),
                response.boundSpaceId(),
                response.merchantId(),
                response.venueId(),
                response.areaId(),
                response.locationPointId(),
                response.staffId(),
                response.status(),
                response.updatedAt(),
                null
        );
    }

    private void revokeDeviceCredential(String deviceId, Instant now) {
        repository.findDeviceCredentialByDeviceId(deviceId)
                .filter(credential -> "active".equals(credential.status()))
                .ifPresent(credential -> repository.saveDeviceCredential(new DeviceCredentialRecord(
                        credential.deviceId(),
                        credential.tokenSha256(),
                        "revoked",
                        credential.createdAt(),
                        now
                )));
    }

    private String businessId(Object response) {
        if (response instanceof DeviceBindingResponse binding) {
            return binding.id();
        }
        if (response instanceof TerminalCapabilitySwitchRecord record) {
            return record.id();
        }
        return null;
    }

    private void requireBoundKioskDeviceCredential(String deviceId, String deviceToken) {
        String token = trimToNull(deviceToken);
        if (token == null) {
            throw ApiException.unauthorized("DEVICE_CREDENTIAL_REQUIRED", "X-Device-Token is required");
        }
        TerminalDeviceRecord device = repository.findByDeviceId(deviceId)
                .orElseThrow(() -> ApiException.forbidden("DEVICE_ACCESS_REJECTED", "device access rejected"));
        if (!"kiosk".equals(device.terminalType()) || !"bound".equals(device.status())) {
            throw ApiException.forbidden("DEVICE_ACCESS_REJECTED", "device access rejected");
        }
        DeviceCredentialRecord credential = repository.findDeviceCredentialByDeviceId(deviceId)
                .orElseThrow(() -> ApiException.forbidden("DEVICE_CREDENTIAL_INVALID", "device token is invalid"));
        if (!"active".equals(credential.status()) || !constantTimeEquals(credential.tokenSha256(), sha256(token))) {
            throw ApiException.forbidden("DEVICE_CREDENTIAL_INVALID", "device token is invalid");
        }
    }

    private String generateDeviceToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return "kiosk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("device token hashing unavailable", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isDeviceCredentialRejection(RuntimeException exception) {
        return exception instanceof ApiException apiException
                && DEVICE_CREDENTIAL_REJECTION_CODES.contains(apiException.code());
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

    private String firstNonBlank(String first, String second) {
        String trimmed = trimToNull(first);
        return trimmed == null ? trimToNull(second) : trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String slug(String value) {
        String slug = value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String resolved = slug.isEmpty() ? UUID.randomUUID().toString().replace("-", "") : slug;
        return resolved.length() > 58 ? resolved.substring(0, 58) : resolved;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
