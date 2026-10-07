package com.complex.platform.terminal;

// 文件版本：V0.05

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.MerchantRecord;
import com.complex.platform.terminal.dto.CapabilitySwitchRequest;
import com.complex.platform.terminal.dto.DeviceBindRequest;
import com.complex.platform.terminal.dto.DeviceLifecycleRequest;
import com.complex.platform.terminal.dto.DeviceMerchantRequest;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

@Service
public class TerminalDeviceService {

    private static final String BIND_PERMISSION = "terminal.device.bind";
    private static final String UNBIND_PERMISSION = "terminal.device.unbind";
    private static final String LIFECYCLE_PERMISSION = "terminal.device.lifecycle";
    // 商户归属改绑影响资金核销路径（K16），从严沿用设备治理权限（高危·需审批·仅平台管理员）。
    private static final String MERCHANT_PERMISSION = "terminal.device.lifecycle";
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
    private final DeviceSigningKeyStore deviceSigningKeyStore;
    private final TerminalMerchantDirectory merchantDirectory;
    // M723-F21（治理地图第 653 行项目与商户隔离；操作员 2026-10-06 裁决）：操作人的商户、项目范围取自登录主体（平台管理员全量）。
    private final AuthorizationService authorizationService;

    public TerminalDeviceService(
            TerminalDeviceRepository repository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            MdmService mdmService,
            DeviceSigningKeyStore deviceSigningKeyStore,
            TerminalMerchantDirectory merchantDirectory,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.mdmService = mdmService;
        this.deviceSigningKeyStore = deviceSigningKeyStore;
        this.merchantDirectory = merchantDirectory;
        this.authorizationService = authorizationService;
    }

    /**
     * AUD-010 ②：登记/轮换 kiosk/tablet 设备级签名公钥。校验设备存在且已 bound（不建孤儿密钥行）；
     * 幂等；写审计（谁/哪台/登记 or 轮换）——静默换钥即可冒充设备，故必留痕。返回 "registered"/"rotated"。
     */
    @Transactional
    public String registerSigningKey(String deviceId, String publicKey, String idempotencyKey) {
        String id = requireText(deviceId, "device_id");
        String key = requireText(publicKey, "public_key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
        try {
            // M723-F21（治理地图第 653 行项目与商户隔离；操作员 2026-10-06 裁决）：先核设备归属范围，再进幂等层——越范围的请求
            // 不占位、不执行，也拿不到别人那次登记的结果；设备不存在留给下面照旧报 404。
            repository.findByDeviceId(id).ifPresent(this::requireDeviceInScope);
            return idempotencyService.execute(
                    "registerDeviceSigningKey:" + id,
                    idempotencyKey,
                    // M723-F18：公钥决定登记／轮换的内容，计入请求摘要——同一提交编号换了公钥按内容不同拒绝（409），不当成重复提交
                    java.util.Map.of("device_id", id, "public_key", key),
                    () -> {
                        TerminalDeviceRecord device = repository.findByDeviceId(id)
                                .orElseThrow(() -> ApiException.notFound(
                                        "TERMINAL_DEVICE_NOT_FOUND", "终端设备不存在：" + id));
                        if (!"bound".equals(device.status())) {
                            throw ApiException.conflict(
                                    "TERMINAL_DEVICE_NOT_BOUND", "终端设备未绑定：" + id);
                        }
                        boolean rotating = deviceSigningKeyStore.findActivePublicKey(id).isPresent();
                        deviceSigningKeyStore.upsert(id, key);
                        String status = rotating ? "rotated" : "registered";
                        auditEventService.recordSuccess(
                                "terminal.device.signing_key_registered",
                                BIND_PERMISSION,
                                "terminal_device",
                                id,
                                null,
                                idempotencyKey,
                                java.util.Map.of("action", status, "device_id", id));
                        return status;
                    },
                    String.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "terminal.device.signing_key_register_failed",
                    BIND_PERMISSION,
                    "terminal_device",
                    deviceId,
                    null,
                    idempotencyKey,
                    codeOf(exception));
            throw exception;
        }
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
                // M723-F21（治理地图第 653 行项目与商户隔离；操作员 2026-10-06 裁决）：同号旧设备与绑定后的归属都要在本人能管的范围内
                () -> requireBindInScope(request),
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
                // M723-F21（治理地图第 653 行项目与商户隔离；操作员 2026-10-06 裁决）：按设备码取到的设备要在本人能管的范围内
                () -> requireReturnInScope(request),
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
                // M723-F21（治理地图第 653 行项目与商户隔离；操作员 2026-10-06 裁决）：按 id 取到的设备要在本人能管的范围内
                () -> repository.findById(id).ifPresent(this::requireDeviceInScope),
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
                // M723-F21（治理地图第 653 行项目与商户隔离；操作员 2026-10-06 裁决）：送修、挂失、找回、报废共用——
                // 按 id 取到的设备要在本人能管的范围内；顺带改写持有员工的，改完后也要仍在范围内
                () -> repository.findById(id).ifPresent(
                        device -> requireHolderChangeInScope(device, request == null ? null : request.staffId())),
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
                .orElseThrow(() -> ApiException.notFound("DEVICE_NOT_FOUND", "设备不存在或已被删除"));
        // 终态（scrapped/blocked）封锁：不可再流转。
        if (TERMINAL_STATUSES.contains(existing.status())) {
            throw ApiException.conflict("DEVICE_TERMINAL_STATUS",
                    "设备处于终态 " + existing.status() + "，不能再流转");
        }
        if (!allowedFrom.contains(existing.status())) {
            throw ApiException.conflict("INVALID_DEVICE_STATUS",
                    "设备状态 " + existing.status() + " 不能流转到 " + targetStatus);
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
                    "设备状态已被并发变更，流转到 " + targetStatus + " 被拒绝");
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

    /**
     * 平台后台设备台账（K16 改绑工作台数据源）：本人能管的全部设备 + 商户归属列。
     * 读权限沿用终端模块视图权限 terminal.switch.view（与 ADM-TER-001 同闸）。
     * M723-F21、F24：只列本人能管的设备（平台管理员仍是全量），完整列出、不截断。
     */
    public PageResponse<TerminalDeviceRecord> listDevices() {
        try {
            // M723-F21、F24（治理地图第 653 行项目与商户隔离；操作员 2026-10-06 裁决）：设备列表只显示本人能管的设备，按范围在库里查、
            // 完整列出——不再先取全平台最近更新的 200 台再过滤（范围外的设备占满名额时，范围内的设备会列不全）。范围口径与写入口的
            // attributionInScope 相同：带商户归属 → 本商户；否则项目范围空＝平台管理员全量，非空 → 认定为本项目的设备。
            Optional<String> actorMerchant = authorizationService.currentMerchantIdQuietly();
            List<TerminalDeviceRecord> records = repository.listDevicesInScope(
                    actorMerchant.orElse(null),
                    actorMerchant.isPresent() ? null : authorizationService.currentProjectScopeQuietly().orElse(null));
            auditEventService.recordSuccess(
                    "terminal.device.viewed",
                    "terminal.switch.view",
                    "terminal_device",
                    "all",
                    null,
                    null,
                    Map.of("count", records.size())
            );
            return new PageResponse<>(records, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, records.size());
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "terminal.device.view_failed",
                    "terminal.switch.view",
                    "terminal_device",
                    "all",
                    null,
                    null,
                    codeOf(exception)
            );
            throw exception;
        }
    }

    /**
     * 设备商户归属设置/更换/解除（K16 核销上下文写入面）。
     * <p>业务口径：设备归属决定“核销发生在哪家”的账上事实——A 吧的机器核 B 吧赠饮月底两家都对不平，
     * 所以改归属必须留痕（谁改的/从谁改到谁/为什么，理由必填），且目标商户必须存在且未归档；
     * 解除绑定后该设备上赠饮/餐饮类权益一律被 K16 fail-closed 拒核销。
     * 终态设备（scrapped/blocked）不可改归属。审计明细带 from/to，状态事件表（append-only）同步留痕。
     */
    @Transactional
    public DeviceBindingResponse updateDeviceMerchant(String id, DeviceMerchantRequest request) {
        String deviceRecordId = requireText(id, "id");
        DeviceMerchantRequest actual = requireRequest(request);
        String idempotencyKey = actual.idempotencyKey();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "idempotency_key is required");
        }
        try {
            String reason = requireText(actual.reason(), "reason");
            // M723-F21（治理地图第 653 行项目与商户隔离；操作员 2026-10-06 裁决）：先核范围，再进幂等层——原设备要在本人能管的
            // 范围内，改完后的归属（目标商户＋原绑定空间＋原持有员工）也要仍在范围内：带商户归属的员工只能改到本商户，
            // 项目员工改完后仍须认定为本项目。设备不存在留给下面照旧报 404。
            repository.findById(deviceRecordId).ifPresent(device -> {
                TerminalDeviceRecord locked = requireDeviceInScope(device);
                requireAttributionInScope(trimToNull(actual.merchantId()), locked.boundSpaceId(), locked.staffId());
            });
            return idempotencyService.execute(
                    "updateTerminalDeviceMerchant:" + deviceRecordId,
                    idempotencyKey,
                    actual,
                    () -> {
                        TerminalDeviceRecord existing = repository.findById(deviceRecordId)
                                .orElseThrow(() -> ApiException.notFound("DEVICE_NOT_FOUND", "设备不存在或已被删除"));
                        if (TERMINAL_STATUSES.contains(existing.status())) {
                            throw ApiException.conflict("DEVICE_TERMINAL_STATUS",
                                    "设备处于终态 " + existing.status() + "，不能变更所属商户");
                        }
                        String target = resolveMerchantForWrite(trimToNull(actual.merchantId()));
                        String from = existing.merchantId();
                        if (java.util.Objects.equals(from, target)) {
                            throw ApiException.conflict("DEVICE_MERCHANT_UNCHANGED",
                                    "设备所属商户没有变化");
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
                                target,
                                existing.venueId(),
                                existing.areaId(),
                                existing.locationPointId(),
                                existing.staffId(),
                                existing.status(),
                                existing.boundAt(),
                                existing.returnedAt(),
                                existing.unboundAt(),
                                existing.returnReason(),
                                existing.unbindReason(),
                                existing.createdAt(),
                                now
                        );
                        TerminalDeviceRecord saved = repository.save(updated);
                        repository.saveStatusEvent(new TerminalDeviceStatusEventRecord(
                                "dev_evt_" + UUID.randomUUID().toString().replace("-", ""),
                                saved.id(),
                                saved.deviceCode(),
                                "terminal.device.merchant_changed",
                                saved.status(),
                                reason,
                                saved.staffId(),
                                now
                        ));
                        java.util.Map<String, Object> details = new java.util.LinkedHashMap<>();
                        details.put("from_merchant_id", from == null ? "" : from);
                        details.put("to_merchant_id", target == null ? "" : target);
                        details.put("reason", reason);
                        auditEventService.recordSuccess(
                                "terminal.device.merchant_changed",
                                MERCHANT_PERMISSION,
                                "terminal_device",
                                saved.id(),
                                null,
                                idempotencyKey,
                                details);
                        return toResponse(saved, null);
                    },
                    DeviceBindingResponse.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "terminal.device.merchant_change_failed",
                    MERCHANT_PERMISSION,
                    "terminal_device",
                    deviceRecordId,
                    null,
                    idempotencyKey,
                    codeOf(exception));
            throw exception;
        }
    }

    /**
     * K16 前置校验：设备商户归属决定核销记谁的账，绑定/改绑前先证目标商户存在且未归档。
     * 传空返回空（= 不绑/解除，由调用方定语义）。
     */
    private String resolveMerchantForWrite(String merchantId) {
        if (merchantId == null) {
            return null;
        }
        MerchantRecord merchant = merchantDirectory.findMerchant(merchantId)
                .orElseThrow(() -> ApiException.notFound(
                        "MERCHANT_NOT_FOUND", "商户不存在：" + merchantId));
        if (merchant.archivedAt() != null) {
            throw ApiException.conflict("MERCHANT_ARCHIVED", "商户已归档：" + merchantId);
        }
        return merchant.id();
    }

    // ── M723-F21（治理地图第 653 行项目与商户隔离；操作员 2026-10-06 裁决）设备归属范围 ─────────────
    // 平台管理员全部设备都能管；带商户归属的员工只能管本商户的设备；不带商户归属的项目员工能管本项目内的全部设备
    // （含本项目内各商户的设备和不挂商户的设备）。认不出项目的设备只有平台管理员能管。

    // 先锁住这台设备的行，再按锁后的归属判定：各写入口都在事务里，本次写入结束之前别人改不了它的归属
    // （判定与写入用同一份事实）。返回锁后的那一行。
    private TerminalDeviceRecord requireDeviceInScope(TerminalDeviceRecord device) {
        repository.lockDevice(device.id());
        TerminalDeviceRecord locked = repository.findById(device.id()).orElse(device);
        requireAttributionInScope(locked.merchantId(), locked.boundSpaceId(), locked.staffId());
        return locked;
    }

    // 归还、送修、挂失、找回、报废可以顺带改写持有员工：原设备要在本人能管的范围内，改完后的归属也要仍在范围内
    // （不挂商户、不绑空间的设备按持有员工认定项目——不能借改持有员工把设备挪到别的项目）。
    private void requireHolderChangeInScope(TerminalDeviceRecord device, String requestedStaffId) {
        TerminalDeviceRecord locked = requireDeviceInScope(device);
        if (trimToNull(requestedStaffId) != null) {
            requireAttributionInScope(locked.merchantId(), locked.boundSpaceId(), requestedStaffId);
            requireNewHolderInScope(requestedStaffId, locked.staffId());
        }
    }

    // M723-F22（治理地图第 653 行「所有业务数据默认按项目隔离」；操作员 2026-10-06 裁决）：项目员工改写持有员工时，新的持有员工
    // 也须是本项目的人——设备按所属商户或绑定空间认定为本项目时，只核设备的项目拦不住把另一项目的员工写成持有人。
    // 带商户归属的员工仍只按商户判断（裁决第二点）；平台管理员不受限；没有换人（与原持有员工相同）不核。
    private void requireNewHolderInScope(String requestedStaffId, String currentStaffId) {
        String staff = trimToNull(requestedStaffId);
        if (staff == null || staff.equals(trimToNull(currentStaffId))
                || authorizationService.currentMerchantIdQuietly().isPresent()) {
            return;
        }
        Optional<String> projectScope = authorizationService.currentProjectScopeQuietly();
        if (projectScope.isPresent() && !projectScope.get().equals(repository.findStaffProjectId(staff).orElse(null))) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "持有员工不在当前综合体项目范围内");
        }
    }

    // 归属三项（所属商户、绑定空间、持有员工）既可以是库里现有的，也可以是绑定、改归属之后将要写入的。
    // 拒绝码沿用同域范式（FloorPlanService.checkMerchantOwnership / checkProjectOwnership）。
    private void requireAttributionInScope(String merchantId, String boundSpaceId, String staffId) {
        if (attributionInScope(merchantId, boundSpaceId, staffId)) {
            return;
        }
        throw authorizationService.currentMerchantIdQuietly().isPresent()
                ? ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "设备归属不在当前商户范围内")
                : ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "设备归属不在当前综合体项目范围内");
    }

    // 判定顺序：操作人带商户归属 → 设备所属商户必须等于本人商户；否则取项目范围，空＝平台管理员放行；
    // 非空 → 设备认定出的项目必须等于它，认不出也拒绝。非平台管理员没有项目归属时 currentProjectScopeQuietly 抛 PROJECT_SCOPE_REQUIRED。
    private boolean attributionInScope(String merchantId, String boundSpaceId, String staffId) {
        Optional<String> actorMerchant = authorizationService.currentMerchantIdQuietly();
        if (actorMerchant.isPresent()) {
            return actorMerchant.get().equals(merchantId);
        }
        Optional<String> projectScope = authorizationService.currentProjectScopeQuietly();
        return projectScope.isEmpty()
                || projectScope.get().equals(deviceProjectId(merchantId, boundSpaceId, staffId).orElse(null));
    }

    // 设备属于哪个项目：先看所属商户的项目，没有商户的看绑定空间的项目，再没有的看持有员工的项目；
    // 三样都没填、或填了的那一样查不出项目，都是认不出（返回空）。
    private Optional<String> deviceProjectId(String merchantId, String boundSpaceId, String staffId) {
        String merchant = trimToNull(merchantId);
        if (merchant != null) {
            return merchantDirectory.findMerchant(merchant).map(MerchantRecord::projectId);
        }
        String space = trimToNull(boundSpaceId);
        if (space != null) {
            return repository.findSpaceProjectId(space);
        }
        String staff = trimToNull(staffId);
        return staff == null ? Optional.empty() : repository.findStaffProjectId(staff);
    }

    // 登记设备：已存在的同号设备要在本人能管的范围内；绑定后的归属（请求里的商户或沿用原商户、请求里的绑定空间、
    // 请求里的持有员工或沿用原持有员工，取值与 bind() 一致）也要在范围内。请求缺设备号时不在这里拦，留给 bind() 照旧报必填。
    private void requireBindInScope(DeviceBindRequest request) {
        String deviceId = request == null ? null : trimToNull(request.deviceId());
        if (deviceId == null) {
            return;
        }
        TerminalDeviceRecord existing = repository.findByDeviceId(deviceId).orElse(null);
        if (existing != null) {
            existing = requireDeviceInScope(existing);
        }
        requireAttributionInScope(
                firstNonBlank(request.merchantId(), existing == null ? null : existing.merchantId()),
                trimToNull(request.boundSpaceId()),
                firstNonBlank(request.staffId(), existing == null ? null : existing.staffId()));
        requireNewHolderInScope(request.staffId(), existing == null ? null : existing.staffId());
    }

    // 归还按设备码定位；请求缺设备码、设备不存在都不在这里拦，留给 returnBoundDevice() 照旧报必填／404。
    private void requireReturnInScope(DeviceReturnRequest request) {
        String deviceCode = request == null ? null : trimToNull(request.deviceCode());
        if (deviceCode != null) {
            repository.findByDeviceCode(deviceCode).ifPresent(device -> requireHolderChangeInScope(device, request.staffId()));
        }
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
                    .orElseThrow(() -> ApiException.notFound("DEVICE_NOT_FOUND", "自助机设备不存在或已被删除"));
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
                throw ApiException.badRequest("VALIDATION_FAILED", "路径与请求体中的设备编号不一致");
            }
            if (!"kiosk".equals(requireText(actual.terminalType(), "terminal_type"))) {
                throw ApiException.badRequest("VALIDATION_FAILED", "终端类型必须为自助机（kiosk）");
            }
            String status = requireText(actual.status(), "status");
            if (!KIOSK_STATUSES.contains(status)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "status is invalid");
            }
            TerminalKioskConfigRecord config = repository.findKioskConfigByDeviceId(id)
                    .orElseThrow(() -> ApiException.notFound("DEVICE_NOT_FOUND", "自助机设备不存在或已被删除"));
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
        TerminalDeviceRecord existingDevice = repository.findByDeviceId(deviceId).orElse(null);
        if (existingDevice != null && NON_OPERATIONAL_STATUSES.contains(existingDevice.status())) {
            throw ApiException.conflict("INVALID_DEVICE_STATUS",
                    "设备处于 " + existingDevice.status() + " 状态，不能绑定");
        }

        // K16 一步到位：登记时可直接给定所属商户（核销上下文）；校验同改绑口径（存在且未归档）。
        // 不传商户 = 不动归属：bind 走 device_id upsert，若这里写 null，归还后再领用会把已配好的
        // 归属静默抹掉 → 该设备上赠饮/餐饮权益核销全被 K16 fail-closed 拒绝。改归属只走 merchant 端点留痕。
        String merchantId = resolveMerchantForWrite(trimToNull(actual.merchantId()));
        if (merchantId == null && existingDevice != null) {
            merchantId = existingDevice.merchantId();
        }
        // U07-001（PGM L611/L617）：工作手机领用即绑定持有员工，验签按 device.staffId 收口；不传则沿用既有归属，不靠归还回填。
        String staffId = trimToNull(actual.staffId());
        if (staffId == null && existingDevice != null) {
            staffId = existingDevice.staffId();
        }
        if ("staff_work_phone".equals(terminalType) && staffId == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "工作手机领用必须指定持有员工 (staff_id)");
        }

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
                merchantId,
                null,
                null,
                trimToNull(actual.boundSpaceId()),
                staffId,
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
            throw ApiException.conflict("RETURN_BLOCKED_PENDING_TASKS", "设备还有未完成的任务");
        }
        TerminalDeviceRecord existing = repository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> ApiException.notFound("DEVICE_NOT_FOUND", "设备不存在或已被删除"));
        if (!"bound".equals(existing.status())) {
            throw ApiException.conflict("INVALID_DEVICE_STATUS", "只有已绑定的设备才能归还");
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
                .orElseThrow(() -> ApiException.notFound("DEVICE_NOT_FOUND", "设备不存在或已被删除"));
        // P0#9 MDM 门控接入点（terminal 自身高风险端点·解绑）：开关开时未纳管硬拒绝；
        //       blocked/lost 设备无论开关都硬拒绝（消除 requireManagedForHighRiskAction dead code）。
        mdmService.requireManagedForHighRiskAction(existing.deviceId(), "terminal.device.unbind");
        if ("unbound".equals(existing.status())) {
            throw ApiException.conflict("INVALID_DEVICE_STATUS", "设备已是未绑定状态");
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
            Runnable scopeCheck,
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
                scopeCheck,
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
            Runnable scopeCheck,
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
                scopeCheck,
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
                // 能力开关不是设备，没有设备归属范围可核（平台口径由控制器 requirePlatformScope 把关）
                () -> { },
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
            Runnable scopeCheck,
            Supplier<T> action,
            Function<T, T> storedResponseTransformer
    ) {
        try {
            // M723-F21（治理地图第 653 行项目与商户隔离；操作员 2026-10-06 裁决）：设备归属范围校验先于幂等层——越范围的请求
            // 不占位、不执行，也拿不到别人那次操作的结果；拒绝与其他失败一样留失败痕。
            scopeCheck.run();
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
            throw ApiException.unauthorized("DEVICE_CREDENTIAL_REQUIRED", "缺少设备令牌（X-Device-Token）");
        }
        TerminalDeviceRecord device = repository.findByDeviceId(deviceId)
                .orElseThrow(() -> ApiException.forbidden("DEVICE_ACCESS_REJECTED", "设备访问被拒绝"));
        if (!"kiosk".equals(device.terminalType()) || !"bound".equals(device.status())) {
            throw ApiException.forbidden("DEVICE_ACCESS_REJECTED", "设备访问被拒绝");
        }
        DeviceCredentialRecord credential = repository.findDeviceCredentialByDeviceId(deviceId)
                .orElseThrow(() -> ApiException.forbidden("DEVICE_CREDENTIAL_INVALID", "设备令牌无效"));
        if (!"active".equals(credential.status()) || !constantTimeEquals(credential.tokenSha256(), sha256(token))) {
            throw ApiException.forbidden("DEVICE_CREDENTIAL_INVALID", "设备令牌无效");
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

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private String firstNonBlank(String first, String second) {
        String trimmed = trimToNull(first);
        return trimmed == null ? trimToNull(second) : trimmed;
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
