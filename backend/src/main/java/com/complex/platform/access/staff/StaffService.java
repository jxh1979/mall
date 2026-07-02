package com.complex.platform.access.staff;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.dto.EmployeeNoticeAcknowledgeRequest;
import com.complex.platform.access.staff.dto.StaffDeviceBindRequest;
import com.complex.platform.access.staff.dto.StaffDeviceIntegrityReportRequest;
import com.complex.platform.access.staff.dto.StaffDeviceRebindDecisionRequest;
import com.complex.platform.access.staff.dto.StaffDeviceRevokeRequest;
import com.complex.platform.access.staff.dto.StaffRegistrationRequest;
import com.complex.platform.access.staff.dto.StaffRegistrationReviewRequest;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.gate.GuardedAction;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 员工三表领域服务（R5 T1）：员工注册→审批闭环；设备凭证绑定/换机（解绑旧→绑新 pending_review→生效，
 * 唯一 active）；Root/调试/异常完整性上报→失效复核；通知收发。
 *
 * <p>隔离：所有列表/查询注入 {@link AuthorizationService#currentMerchantScope()}——平台角色见全量，
 * 商户角色仅见本 merchant_id 行；写操作经 currentMerchantScope 校验越权。状态机硬约束在本服务，
 * DB CHECK + 唯一索引兜底。事件经 {@link AuditEventService} 落 audit_events（D4 点+下划线）。
 *
 * <p>边界：staff_device_credentials 与 kiosk 的 terminal_device_credentials 是两套实体，本服务从不读写后者。
 */
@Service
public class StaffService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final Set<String> INTEGRITY_FAILURES =
            Set.of("root_detected", "debug_detected", "abnormal");

    private final StaffRepository repository;
    private final AccessRepository accessRepository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public StaffService(
            StaffRepository repository,
            AccessRepository accessRepository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService
    ) {
        this.repository = repository;
        this.accessRepository = accessRepository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    // ============================================================ 员工注册 / 审批

    @GuardedAction(permission = "staff.profile.submit")
    public StaffProfileRecord submitRegistration(StaffRegistrationRequest request) {
        // F-AUTHZ-02 防跨商户越权注册：员工注册非裁决#2的治理写（merchant.profile/entity.binding/
        // contract.draft/governance.space.manage），故用 merchant scope 守卫而非 requirePlatformScope——
        // 平台管理员可为任意商户注册；merchant-scoped 调用者只能为本商户注册，允许商户自助管本商户员工。
        authorizationService.currentMerchantScope().ifPresent(scope -> {
            if (!scope.equals(trimToNull(request.merchantId()))) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                        "cannot register staff for another merchant");
            }
        });
        try {
            return idempotencyService.execute(
                    "submitStaffRegistration",
                    request.idempotencyKey(),
                    request,
                    () -> {
                        String projectId = trimToNull(request.projectId());
                        if (projectId == null) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "project_id is required");
                        }
                        String name = trimToNull(request.name());
                        if (name == null) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "name is required");
                        }
                        if (request.staffLevelId() != null
                                && accessRepository.findStaffLevelById(request.staffLevelId()).isEmpty()) {
                            throw ApiException.badRequest("UNKNOWN_STAFF_LEVEL",
                                    "unknown staff_level_id: " + request.staffLevelId());
                        }
                        Instant now = Instant.now();
                        StaffProfileRecord profile = new StaffProfileRecord(
                                "staff_" + slug(request.idempotencyKey()),
                                projectId,
                                trimToNull(request.merchantId()),
                                null,
                                name,
                                trimToNull(request.phoneHash()),
                                trimToNull(request.idDocHash()),
                                trimToNull(request.staffLevelId()),
                                "submitted",
                                null,
                                null,
                                null,
                                "active",
                                now,
                                now
                        );
                        StaffProfileRecord saved = repository.saveProfile(profile);
                        auditEventService.recordSuccess("staff.registered", "staff.profile.submit",
                                "staff_profile", saved.id(), null, request.idempotencyKey(),
                                Map.of("project_id", saved.projectId()));
                        return saved;
                    },
                    StaffProfileRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff.register_failed", "staff.profile.submit",
                    "staff_profile", null, null, idempotencyKeyOf(request), codeOf(exception));
            throw exception;
        }
    }

    @GuardedAction(permission = "staff.profile.review")
    public StaffProfileRecord reviewRegistration(String id, StaffRegistrationReviewRequest request) {
        try {
            return idempotencyService.execute(
                    "reviewStaffRegistration:" + id,
                    request.idempotencyKey(),
                    request,
                    () -> {
                        StaffProfileRecord profile = requireProfileInScope(id);
                        if (!"submitted".equals(profile.registrationStatus())) {
                            throw ApiException.conflict("INVALID_STATE",
                                    "registration is not in submitted state: " + profile.registrationStatus());
                        }
                        String decision = trimToNull(request.decision());
                        AccessActor actor = authorizationService.currentActor();
                        Instant now = Instant.now();
                        if ("approve".equalsIgnoreCase(decision == null ? "" : decision)) {
                            AccessUser created = accessRepository.createUser(new AccessUser(
                                    "usr_" + profile.id(),
                                    "staff_" + profile.id(),
                                    "staff",
                                    null,
                                    profile.merchantId(),
                                    "active",
                                    now
                            ));
                            StaffProfileRecord approved = new StaffProfileRecord(
                                    profile.id(), profile.projectId(), profile.merchantId(), created.id(),
                                    profile.name(), profile.phoneHash(), profile.idDocHash(), profile.staffLevelId(),
                                    "approved", actor.id(), now, null, profile.status(),
                                    profile.createdAt(), now);
                            StaffProfileRecord saved = repository.saveProfile(approved);
                            createNotice(saved.id(), "approval_result", "注册审批通过",
                                    "您的员工注册已通过审批，已生成账号。", false, actorIdOrSystem(actor));
                            auditEventService.recordSuccess("staff.approved", "staff.profile.review",
                                    "staff_profile", saved.id(), null, request.idempotencyKey(),
                                    Map.of("user_id", created.id()));
                            return saved;
                        }
                        if ("reject".equalsIgnoreCase(decision == null ? "" : decision)) {
                            String reason = trimToNull(request.rejectReason());
                            if (reason == null) {
                                throw ApiException.badRequest("VALIDATION_FAILED",
                                        "reject_reason is required when rejecting");
                            }
                            StaffProfileRecord rejected = new StaffProfileRecord(
                                    profile.id(), profile.projectId(), profile.merchantId(), profile.userId(),
                                    profile.name(), profile.phoneHash(), profile.idDocHash(), profile.staffLevelId(),
                                    "rejected", actor.id(), now, reason, profile.status(),
                                    profile.createdAt(), now);
                            StaffProfileRecord saved = repository.saveProfile(rejected);
                            createNotice(saved.id(), "approval_result", "注册审批驳回", reason, false,
                                    actorIdOrSystem(actor));
                            auditEventService.recordSuccess("staff.rejected", "staff.profile.review",
                                    "staff_profile", saved.id(), null, request.idempotencyKey(),
                                    Map.of("reason", reason));
                            return saved;
                        }
                        throw ApiException.badRequest("VALIDATION_FAILED",
                                "decision must be 'approve' or 'reject'");
                    },
                    StaffProfileRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff.review_failed", "staff.profile.review",
                    "staff_profile", id, null, idempotencyKeyOf(request), codeOf(exception));
            throw exception;
        }
    }

    @GuardedAction(permission = "staff.profile.view")
    public PageResponse<StaffProfileRecord> listProfiles() {
        List<StaffProfileRecord> profiles = repository.listProfiles(merchantScopeOrNull());
        auditEventService.recordSuccess("staff.profile.viewed", "staff.profile.view",
                "staff_profile", "all", null, null, Map.of("count", profiles.size()));
        return page(profiles);
    }

    // ============================================================ 设备凭证：绑定 / 换机

    @GuardedAction(permission = "staff.device.bind")
    public StaffDeviceCredentialRecord bindCredential(StaffDeviceBindRequest request) {
        try {
            return idempotencyService.execute(
                    "bindStaffDeviceCredential",
                    request.idempotencyKey(),
                    request,
                    () -> {
                        String staffId = trimToNull(request.staffProfileId());
                        String publicKey = trimToNull(request.devicePublicKey());
                        String fingerprint = trimToNull(request.deviceFingerprintHash());
                        if (staffId == null || publicKey == null || fingerprint == null) {
                            throw ApiException.badRequest("VALIDATION_FAILED",
                                    "staff_profile_id, device_public_key and device_fingerprint_hash are required");
                        }
                        // 1.9：拒绝占位设备凭证——必须是 Android Keystore 真实公钥/指纹，不接受 PLACEHOLDER_*。
                        if (publicKey.startsWith("PLACEHOLDER_") || fingerprint.startsWith("PLACEHOLDER_")) {
                            throw ApiException.badRequest("DEVICE_CREDENTIAL_PLACEHOLDER",
                                    "device_public_key/device_fingerprint_hash must be real Keystore values, not placeholders");
                        }
                        requireProfileInScope(staffId);
                        // 换机识别：已有 active 且指纹不同 → 新凭证 pending_review + supersedes 指向旧 active。
                        Optional<StaffDeviceCredentialRecord> active = repository.findActiveCredential(staffId);
                        String supersedes = null;
                        boolean rebind = false;
                        if (active.isPresent()) {
                            if (fingerprint.equals(active.get().deviceFingerprintHash())) {
                                throw ApiException.conflict("ALREADY_BOUND",
                                        "this device fingerprint is already the active credential");
                            }
                            supersedes = active.get().id();
                            rebind = true;
                        }
                        Instant now = Instant.now();
                        StaffDeviceCredentialRecord credential = new StaffDeviceCredentialRecord(
                                "scred_" + slug(request.idempotencyKey()),
                                staffId, publicKey, fingerprint, "pending_review",
                                supersedes, "trusted", null, null, null, now, now);
                        StaffDeviceCredentialRecord saved = repository.saveCredential(credential);
                        String event = rebind ? "staff_device.rebind_requested" : "staff_device.bound";
                        auditEventService.recordSuccess(event, "staff.device.bind",
                                "staff_device_credential", saved.id(), null, request.idempotencyKey(),
                                Map.of("staff_profile_id", staffId, "rebind", rebind));
                        return saved;
                    },
                    StaffDeviceCredentialRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff_device.bind_failed", "staff.device.bind",
                    "staff_device_credential", null, null, idempotencyKeyOf(request), codeOf(exception));
            throw exception;
        }
    }

    @GuardedAction(permission = "staff.device.review")
    public StaffDeviceCredentialRecord approveRebind(String credId, StaffDeviceRebindDecisionRequest request) {
        try {
            return idempotencyService.execute(
                    "approveStaffDeviceRebind:" + credId,
                    request.idempotencyKey(),
                    request,
                    () -> {
                        StaffDeviceCredentialRecord credential = requireCredentialInScope(credId);
                        if (!"pending_review".equals(credential.bindStatus())) {
                            throw ApiException.conflict("INVALID_STATE",
                                    "credential is not pending_review: " + credential.bindStatus());
                        }
                        Instant now = Instant.now();
                        // 换机：先把旧 active 置 superseded，再把新凭证置 active，保证唯一 active 索引始终成立。
                        if (credential.supersedesCredentialId() != null) {
                            repository.findCredentialById(credential.supersedesCredentialId())
                                    .filter(old -> "active".equals(old.bindStatus()))
                                    .ifPresent(old -> repository.saveCredential(withBindStatus(old, "superseded", now)));
                        }
                        StaffDeviceCredentialRecord activated = new StaffDeviceCredentialRecord(
                                credential.id(), credential.staffProfileId(), credential.devicePublicKey(),
                                credential.deviceFingerprintHash(), "active", credential.supersedesCredentialId(),
                                credential.integrityStatus(), null, now, null,
                                credential.createdAt(), now);
                        StaffDeviceCredentialRecord saved = repository.saveCredential(activated);
                        auditEventService.recordSuccess("staff_device.rebound", "staff.device.review",
                                "staff_device_credential", saved.id(), null, request.idempotencyKey(),
                                Map.of("staff_profile_id", saved.staffProfileId()));
                        return saved;
                    },
                    StaffDeviceCredentialRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff_device.rebind_failed", "staff.device.review",
                    "staff_device_credential", credId, null, idempotencyKeyOf(request), codeOf(exception));
            throw exception;
        }
    }

    /**
     * 端侧完整性上报。非 trusted（root/debug/abnormal）→ 凭证自动 revoked + 发 integrity_failed + 通知复核。
     * 由端侧鉴权调用，不挂业务权限点。
     */
    public StaffDeviceCredentialRecord reportIntegrity(String credId, StaffDeviceIntegrityReportRequest request) {
        try {
            return idempotencyService.execute(
                    "reportStaffDeviceIntegrity:" + credId,
                    request.idempotencyKey(),
                    request,
                    () -> {
                        StaffDeviceCredentialRecord credential = repository.findCredentialById(credId)
                                .orElseThrow(() -> ApiException.notFound("CREDENTIAL_NOT_FOUND",
                                        "staff device credential not found: " + credId));
                        // P1-2 越权修复：完整性上报此前不校验凭证归属，任意已认证用户可按 credId 跨商户吊销他人设备凭证（DoS）。
                        // 改为按调用者商户范围校验凭证所属员工档案归属（与 revokeCredential 同范式；平台 scope 放行）。
                        requireProfileInScope(credential.staffProfileId());
                        String status = trimToNull(request.integrityStatus());
                        if (status == null
                                || !(status.equals("trusted") || INTEGRITY_FAILURES.contains(status))) {
                            throw ApiException.badRequest("VALIDATION_FAILED",
                                    "integrity_status must be one of trusted/root_detected/debug_detected/abnormal");
                        }
                        Instant now = Instant.now();
                        if (INTEGRITY_FAILURES.contains(status)) {
                            StaffDeviceCredentialRecord revoked = new StaffDeviceCredentialRecord(
                                    credential.id(), credential.staffProfileId(), credential.devicePublicKey(),
                                    credential.deviceFingerprintHash(), "revoked", credential.supersedesCredentialId(),
                                    status, "integrity:" + status, credential.boundAt(), now,
                                    credential.createdAt(), now);
                            StaffDeviceCredentialRecord saved = repository.saveCredential(revoked);
                            createNotice(saved.staffProfileId(), "status_change", "设备完整性异常，凭证已失效",
                                    "检测到 " + status + "，设备凭证已自动失效，请复核后重新绑定。", false, "system");
                            auditEventService.recordSuccess("staff_device.integrity_failed", "staff.device.bind",
                                    "staff_device_credential", saved.id(), null, request.idempotencyKey(),
                                    Map.of("integrity_status", status));
                            return saved;
                        }
                        // trusted：仅刷新完整性标记，不改 bind_status。
                        StaffDeviceCredentialRecord trusted = withIntegrity(credential, "trusted", now);
                        StaffDeviceCredentialRecord saved = repository.saveCredential(trusted);
                        auditEventService.recordSuccess("staff_device.integrity_reported", "staff.device.bind",
                                "staff_device_credential", saved.id(), null, request.idempotencyKey(),
                                Map.of("integrity_status", status));
                        return saved;
                    },
                    StaffDeviceCredentialRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff_device.integrity_report_failed", "staff.device.bind",
                    "staff_device_credential", credId, null, idempotencyKeyOf(request), codeOf(exception));
            throw exception;
        }
    }

    @GuardedAction(permission = "staff.device.revoke")
    public StaffDeviceCredentialRecord revokeCredential(String credId, StaffDeviceRevokeRequest request) {
        try {
            return idempotencyService.execute(
                    "revokeStaffDeviceCredential:" + credId,
                    request.idempotencyKey(),
                    request,
                    () -> {
                        StaffDeviceCredentialRecord credential = requireCredentialInScope(credId);
                        if ("revoked".equals(credential.bindStatus())
                                || "superseded".equals(credential.bindStatus())) {
                            throw ApiException.conflict("INVALID_STATE",
                                    "credential is already inactive: " + credential.bindStatus());
                        }
                        Instant now = Instant.now();
                        String reason = trimToNull(request.reason());
                        StaffDeviceCredentialRecord revoked = new StaffDeviceCredentialRecord(
                                credential.id(), credential.staffProfileId(), credential.devicePublicKey(),
                                credential.deviceFingerprintHash(), "revoked", credential.supersedesCredentialId(),
                                credential.integrityStatus(), reason == null ? "manual_revoke" : reason,
                                credential.boundAt(), now, credential.createdAt(), now);
                        StaffDeviceCredentialRecord saved = repository.saveCredential(revoked);
                        auditEventService.recordSuccess("staff_device.revoked", "staff.device.revoke",
                                "staff_device_credential", saved.id(), null, request.idempotencyKey(),
                                Map.of("reason", reason == null ? "" : reason));
                        return saved;
                    },
                    StaffDeviceCredentialRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff_device.revoke_failed", "staff.device.revoke",
                    "staff_device_credential", credId, null, idempotencyKeyOf(request), codeOf(exception));
            throw exception;
        }
    }

    // ============================================================ 员工通知

    @GuardedAction(permission = "staff.notice.view")
    /** E1(2026-07-02 拍板):主管排班——双端(后台页+主管App);班次到分钟+指定点位;
     * 同员工同开始时刻幂等覆盖;审计留痕。 */
    public StaffShiftRecord upsertShift(com.complex.platform.access.staff.dto.StaffShiftRequest request) {
        authorizationService.requirePermission("staff.shift.manage");
        if (request == null || request.userId() == null || request.userId().isBlank()
                || request.workDate() == null || request.startAt() == null || request.endAt() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "user_id/work_date/start_at/end_at are required");
        }
        if (!request.endAt().isAfter(request.startAt())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "end_at must be after start_at");
        }
        var actor = authorizationService.currentActor();
        Instant now = Instant.now();
        StaffShiftRecord saved = repository.upsertShift(new StaffShiftRecord(
                "shf_" + UUID.randomUUID(), request.userId().trim(), request.workDate(),
                request.startAt(), request.endAt(),
                request.pointSpaceIds() == null ? List.of() : request.pointSpaceIds(),
                request.note(), actor == null ? null : actor.id(), now, now));
        auditEventService.recordSuccess("staff.shift.saved", "staff.shift.manage",
                "staff_shift_schedule", saved.id(), null, request.idempotencyKey(),
                java.util.Map.of("user_id", saved.userId(), "work_date", saved.workDate().toString()));
        return saved;
    }

    /** E1:后台/主管按日(可选按人)查排班。 */
    public PageResponse<StaffShiftRecord> listShifts(java.time.LocalDate workDate, String userId) {
        authorizationService.requirePermission("staff.shift.manage");
        return page(repository.listShifts(workDate, userId));
    }

    /** E1:员工自有手机看自己的班次与点位(今天起 7 天)。 */
    public PageResponse<StaffShiftRecord> listMyShifts() {
        authorizationService.requirePermission("staff.shift.view");
        var actor = authorizationService.currentActor();
        java.time.LocalDate today = java.time.LocalDate.ofInstant(Instant.now(), java.time.ZoneOffset.UTC);
        return page(repository.listShiftsForUserBetween(actor.id(), today, today.plusDays(7)));
    }

    /** E2(2026-07-02 拍板):打卡——时间以服务器为准;严格无宽限:上班晚于班次开始即迟到、
     * 下班早于班次结束即早退;当日无班次记 no_shift;只记录不自动扣罚(处理归人事)。 */
    public StaffAttendanceRecord punch(com.complex.platform.access.staff.dto.StaffAttendancePunchRequest request) {
        authorizationService.requirePermission("staff.attendance.punch");
        if (request == null || request.punchType() == null
                || (!request.punchType().equals("check_in") && !request.punchType().equals("check_out"))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "punch_type must be check_in or check_out");
        }
        String method = request.method() == null ? "manual" : request.method();
        if (!java.util.Set.of("nfc", "qr", "manual").contains(method)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "method must be nfc/qr/manual");
        }
        var actor = authorizationService.currentActor();
        Instant now = Instant.now();
        java.time.LocalDate today = java.time.LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);
        java.util.List<StaffShiftRecord> shifts = repository.listShiftsForUserBetween(actor.id(), today, today);
        StaffShiftRecord shift = pickShift(shifts, request.punchType(), now);
        String judgement;
        if (shift == null) {
            judgement = "no_shift";
        } else if (request.punchType().equals("check_in")) {
            judgement = now.isAfter(shift.startAt()) ? "late" : "normal";
        } else {
            judgement = now.isBefore(shift.endAt()) ? "early_leave" : "normal";
        }
        StaffAttendanceRecord saved = repository.saveAttendance(new StaffAttendanceRecord(
                "att_" + UUID.randomUUID(), actor.id(), request.punchType(), now, method,
                trimToNull(request.tagRef()), trimToNull(request.spaceId()),
                trimToNull(request.deviceFingerprint()), shift == null ? null : shift.id(), judgement, now));
        auditEventService.recordSuccess("staff.attendance.punched", "staff.attendance.punch",
                "staff_attendance_record", saved.id(), null, request.idempotencyKey(),
                java.util.Map.of("punch_type", saved.punchType(), "judgement", judgement, "method", method));
        return saved;
    }

    private StaffShiftRecord pickShift(java.util.List<StaffShiftRecord> shifts, String punchType, Instant now) {
        return shifts.stream().min(java.util.Comparator.comparingLong(shift -> Math.abs(
                (punchType.equals("check_in") ? shift.startAt() : shift.endAt()).toEpochMilli()
                        - now.toEpochMilli()))).orElse(null);
    }

    /** E2:员工看自己最近 30 天打卡记录。 */
    public PageResponse<StaffAttendanceRecord> listMyAttendance() {
        authorizationService.requirePermission("staff.shift.view");
        var actor = authorizationService.currentActor();
        return page(repository.listAttendanceForUser(actor.id(),
                Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS)));
    }

    /** E2:后台/主管按日查考勤(判定列:正常/迟到/早退/无班次)。 */
    public PageResponse<StaffAttendanceRecord> listAttendance(java.time.LocalDate date) {
        authorizationService.requirePermission("staff.attendance.view");
        return page(repository.listAttendanceOn(date));
    }

    public PageResponse<EmployeeNoticeRecord> listNotices() {
        List<EmployeeNoticeRecord> notices = repository.listNotices(merchantScopeOrNull());
        auditEventService.recordSuccess("staff.notice.viewed", "staff.notice.view",
                "employee_notice", "all", null, null, Map.of("count", notices.size()));
        return page(notices);
    }

    @GuardedAction(permission = "staff.notice.view")
    public EmployeeNoticeRecord acknowledge(String noticeId, EmployeeNoticeAcknowledgeRequest request) {
        try {
            return idempotencyService.execute(
                    "acknowledgeEmployeeNotice:" + noticeId,
                    request.idempotencyKey(),
                    request,
                    () -> {
                        EmployeeNoticeRecord notice = repository.findNoticeById(noticeId)
                                .orElseThrow(() -> ApiException.notFound("NOTICE_NOT_FOUND",
                                        "employee notice not found: " + noticeId));
                        requireProfileInScope(notice.staffProfileId());
                        String next;
                        if (Boolean.TRUE.equals(request.decline())) {
                            next = "declined";
                        } else if (Boolean.TRUE.equals(request.consent())) {
                            next = "consented";
                        } else {
                            next = "acknowledged";
                        }
                        if (notice.consentRequired() && "acknowledged".equals(next)) {
                            throw ApiException.badRequest("CONSENT_REQUIRED",
                                    "this notice requires explicit consent or decline");
                        }
                        Instant now = Instant.now();
                        EmployeeNoticeRecord updated = new EmployeeNoticeRecord(
                                notice.id(), notice.staffProfileId(), notice.noticeType(), notice.title(),
                                notice.body(), notice.consentRequired(), next,
                                "consented".equals(next) ? now : notice.consentedAt(),
                                notice.createdBy(), notice.createdAt(), now);
                        EmployeeNoticeRecord saved = repository.saveNotice(updated);
                        auditEventService.recordSuccess("staff.notice_acknowledged", "staff.notice.view",
                                "employee_notice", saved.id(), null, request.idempotencyKey(),
                                Map.of("consent_status", next));
                        return saved;
                    },
                    EmployeeNoticeRecord.class
            );
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff.notice_acknowledge_failed", "staff.notice.view",
                    "employee_notice", noticeId, null, idempotencyKeyOf(request), codeOf(exception));
            throw exception;
        }
    }

    /**
     * 通知写入方（审批/状态变更/合规模块调用，保证 employee_notices 有写入方）。
     * 包内可见，供本服务的审批/完整性等流程内部下发，不直接暴露为端点。
     */
    EmployeeNoticeRecord createNotice(String staffProfileId, String type, String title, String body,
                                      boolean consentRequired, String createdBy) {
        Instant now = Instant.now();
        EmployeeNoticeRecord notice = new EmployeeNoticeRecord(
                "notice_" + UUID.randomUUID().toString().replace("-", ""),
                staffProfileId, type, title, body, consentRequired,
                consentRequired ? "pending" : "pending", null,
                createdBy == null ? "system" : createdBy, now, now);
        EmployeeNoticeRecord saved = repository.saveNotice(notice);
        auditEventService.recordSuccess("staff.notice_created", "staff.notice.view",
                "employee_notice", saved.id(), null, null,
                Map.of("notice_type", type, "staff_profile_id", staffProfileId));
        return saved;
    }

    // ============================================================ 隔离 / 工具

    /** 平台角色 → null（见全量）；商户角色 → merchantId（仅见本户）。 */
    private String merchantScopeOrNull() {
        return authorizationService.currentMerchantScope().orElse(null);
    }

    /** 读取并校验 profile 在当前 scope 内（跨商户不可见即拒绝）。 */
    private StaffProfileRecord requireProfileInScope(String id) {
        StaffProfileRecord profile = repository.findProfileById(id)
                .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND", "staff profile not found: " + id));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(profile.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "cannot access a staff profile in another merchant scope");
        }
        return profile;
    }

    private StaffDeviceCredentialRecord requireCredentialInScope(String credId) {
        StaffDeviceCredentialRecord credential = repository.findCredentialById(credId)
                .orElseThrow(() -> ApiException.notFound("CREDENTIAL_NOT_FOUND",
                        "staff device credential not found: " + credId));
        requireProfileInScope(credential.staffProfileId());
        return credential;
    }

    private StaffDeviceCredentialRecord withBindStatus(StaffDeviceCredentialRecord c, String status, Instant now) {
        return new StaffDeviceCredentialRecord(c.id(), c.staffProfileId(), c.devicePublicKey(),
                c.deviceFingerprintHash(), status, c.supersedesCredentialId(), c.integrityStatus(),
                c.revokedReason(), c.boundAt(), c.revokedAt(), c.createdAt(), now);
    }

    private StaffDeviceCredentialRecord withIntegrity(StaffDeviceCredentialRecord c, String integrity, Instant now) {
        return new StaffDeviceCredentialRecord(c.id(), c.staffProfileId(), c.devicePublicKey(),
                c.deviceFingerprintHash(), c.bindStatus(), c.supersedesCredentialId(), integrity,
                c.revokedReason(), c.boundAt(), c.revokedAt(), c.createdAt(), now);
    }

    private String actorIdOrSystem(AccessActor actor) {
        return actor == null || actor.id() == null ? "system" : actor.id();
    }

    /** F批(2026-07-02 拍板):固定工资=员工档案月薪,人事维护。 */
    public void setBaseSalary(com.complex.platform.access.staff.dto.StaffPayrollBaseRequest request) {
        authorizationService.requirePermission("staff.payroll.manage");
        if (request == null || request.userId() == null || request.userId().isBlank()
                || request.monthlySalaryCent() == null || request.monthlySalaryCent() < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "user_id and non-negative monthly_salary_cent required");
        }
        var actor = authorizationService.currentActor();
        repository.upsertBaseSalary(request.userId().trim(), request.monthlySalaryCent(),
                actor == null ? null : actor.id(), Instant.now());
        auditEventService.recordSuccess("staff.payroll.base_saved", "staff.payroll.manage",
                "staff_base_salary", request.userId().trim(), null, request.idempotencyKey(),
                java.util.Map.of("monthly_salary_cent", request.monthlySalaryCent()));
    }

    /** F批:奖金/处罚手录——必须带事由,审计留痕。 */
    public void addPayrollAdjustment(com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest request) {
        authorizationService.requirePermission("staff.payroll.manage");
        if (request == null || request.userId() == null || request.userId().isBlank()
                || request.month() == null || !request.month().matches("\\d{4}-\\d{2}")
                || request.adjType() == null || !java.util.Set.of("bonus", "penalty").contains(request.adjType())
                || request.amountCent() == null || request.amountCent() <= 0
                || request.reason() == null || request.reason().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "user_id/month(YYYY-MM)/adj_type(bonus|penalty)/amount_cent>0/reason are required");
        }
        var actor = authorizationService.currentActor();
        String id = "adj_" + UUID.randomUUID();
        repository.insertPayrollAdjustment(id, request.userId().trim(), request.month(), request.adjType(),
                request.amountCent(), request.reason().trim(), actor == null ? null : actor.id(), Instant.now());
        auditEventService.recordSuccess("staff.payroll.adjustment_added", "staff.payroll.manage",
                "staff_payroll_adjustment", id, null, request.idempotencyKey(),
                java.util.Map.of("user_id", request.userId().trim(), "month", request.month(),
                        "adj_type", request.adjType(), "amount_cent", request.amountCent(),
                        "reason", request.reason().trim()));
    }

    /** F批:提成入台账(手录/系统写入口;服务点单等业务后续自动写)。 */
    public void addCommission(com.complex.platform.access.staff.dto.StaffCommissionRequest request) {
        authorizationService.requirePermission("staff.payroll.manage");
        if (request == null || request.userId() == null || request.userId().isBlank()
                || request.month() == null || !request.month().matches("\\d{4}-\\d{2}")
                || request.amountCent() == null || request.amountCent() < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "user_id/month/amount_cent>=0 required");
        }
        var actor = authorizationService.currentActor();
        String id = "cml_" + UUID.randomUUID();
        repository.insertCommission(id, request.userId().trim(), request.month(),
                request.sourceType() == null ? "manual" : request.sourceType(), request.sourceId(),
                request.amountCent(), request.note(), Instant.now());
        auditEventService.recordSuccess("staff.payroll.commission_added", "staff.payroll.manage",
                "staff_commission_ledger", id, null, request.idempotencyKey(),
                java.util.Map.of("user_id", request.userId().trim(), "month", request.month(),
                        "amount_cent", request.amountCent()));
    }

    /** F批(2026-07-02 拍板):月结——重算定格全员薪资单(可重复执行=覆盖,补录奖惩后重结);
     * total = 固定 + 提成(台账+酒吧提成) + 奖金 - 处罚;员工只看已定格月份。 */
    public java.util.Map<String, Object> closePayroll(com.complex.platform.access.staff.dto.StaffPayrollCloseRequest request) {
        authorizationService.requirePermission("staff.payroll.manage");
        if (request == null || request.month() == null || !request.month().matches("\\d{4}-\\d{2}")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "month(YYYY-MM) is required");
        }
        var actor = authorizationService.currentActor();
        int count = repository.closePayrollMonth(request.month(), actor == null ? null : actor.id(), Instant.now());
        auditEventService.recordSuccess("staff.payroll.month_closed", "staff.payroll.manage",
                "staff_payroll_statement", request.month(), null, request.idempotencyKey(),
                java.util.Map.of("month", request.month(), "statements", count));
        return java.util.Map.of("month", request.month(), "statements", count);
    }

    /** F批:后台按月查全员薪资单。 */
    public PageResponse<StaffPayrollStatementRecord> listPayroll(String month) {
        authorizationService.requirePermission("staff.payroll.manage");
        return page(repository.listPayrollStatements(month, null));
    }

    /** F批(拍板:月结后才可见):员工查自己薪资——只返回已定格薪资单,未结月自然为空。 */
    public PageResponse<StaffPayrollStatementRecord> listMyPayroll(String month) {
        authorizationService.requirePermission("staff.payroll.view_mine");
        var actor = authorizationService.currentActor();
        return page(repository.listPayrollStatements(month, actor.id()));
    }

    /** F批:员工提交请假(四类:事假/病假/调休/年假;主管审)。 */
    public StaffLeaveRequestRecord submitLeave(com.complex.platform.access.staff.dto.StaffLeaveRequest request) {
        authorizationService.requirePermission("staff.leave.submit");
        if (request == null || request.leaveType() == null
                || !java.util.Set.of("personal", "sick", "comp", "annual").contains(request.leaveType())
                || request.startDate() == null || request.endDate() == null
                || request.endDate().isBefore(request.startDate())
                || request.reason() == null || request.reason().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "leave_type(personal|sick|comp|annual)/start_date<=end_date/reason are required");
        }
        var actor = authorizationService.currentActor();
        Instant now = Instant.now();
        StaffLeaveRequestRecord saved = repository.saveLeaveRequest(new StaffLeaveRequestRecord(
                "lv_" + UUID.randomUUID(), actor.id(), request.leaveType(), request.startDate(),
                request.endDate(), request.reason().trim(), "pending", null, null, now, now));
        auditEventService.recordSuccess("staff.leave.submitted", "staff.leave.submit",
                "staff_leave_request", saved.id(), null, request.idempotencyKey(),
                java.util.Map.of("leave_type", saved.leaveType(),
                        "start_date", saved.startDate().toString(), "end_date", saved.endDate().toString()));
        return saved;
    }

    /** F批:我的请假列表。 */
    public PageResponse<StaffLeaveRequestRecord> listMyLeaves() {
        authorizationService.requirePermission("staff.leave.submit");
        var actor = authorizationService.currentActor();
        return page(repository.listLeaves(null, actor.id()));
    }

    /** F批:主管审请假(复用排班主管权限);通过区间考勤月视图记「请假」。 */
    public StaffLeaveRequestRecord reviewLeave(String id,
            com.complex.platform.access.staff.dto.StaffLeaveReviewRequest request) {
        authorizationService.requirePermission("staff.shift.manage");
        if (request == null || request.decision() == null
                || !java.util.Set.of("approve", "reject").contains(request.decision())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "decision must be approve or reject");
        }
        StaffLeaveRequestRecord current = repository.findLeaveById(id)
                .orElseThrow(() -> ApiException.notFound("LEAVE_NOT_FOUND", "leave request not found"));
        if (!current.status().equals("pending")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "leave request already reviewed");
        }
        var actor = authorizationService.currentActor();
        StaffLeaveRequestRecord saved = repository.saveLeaveRequest(new StaffLeaveRequestRecord(
                current.id(), current.userId(), current.leaveType(), current.startDate(), current.endDate(),
                current.reason(), request.decision().equals("approve") ? "approved" : "rejected",
                actor == null ? null : actor.id(), request.reviewNote(), current.createdAt(), Instant.now()));
        auditEventService.recordSuccess("staff.leave.reviewed", "staff.shift.manage",
                "staff_leave_request", saved.id(), null, request.idempotencyKey(),
                java.util.Map.of("decision", request.decision(), "user_id", saved.userId()));
        return saved;
    }

    /** F批:主管/后台查请假(按状态)。 */
    public PageResponse<StaffLeaveRequestRecord> listLeaves(String status) {
        authorizationService.requirePermission("staff.shift.manage");
        return page(repository.listLeaves(status, null));
    }

    /** F批(2026-07-02 拍板六态口径):考勤月视图——出勤/缺勤(有排班无上班卡)/迟到/早退/
     * 未打下班卡/请假(approved 覆盖);无排班=休;同日多问题并列。 */
    public java.util.List<StaffAttendanceDayRecord> myAttendanceMonthView(String month) {
        authorizationService.requirePermission("staff.shift.view");
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "month(YYYY-MM) is required");
        }
        var actor = authorizationService.currentActor();
        java.time.YearMonth yearMonth = java.time.YearMonth.parse(month);
        java.time.LocalDate first = yearMonth.atDay(1);
        java.time.LocalDate last = yearMonth.atEndOfMonth();
        java.util.List<StaffShiftRecord> shifts = repository.listShiftsForUserBetween(actor.id(), first, last);
        java.util.Map<java.time.LocalDate, StaffShiftRecord> shiftByDay = new java.util.LinkedHashMap<>();
        for (StaffShiftRecord shift : shifts) {
            shiftByDay.putIfAbsent(shift.workDate(), shift);
        }
        java.util.Set<java.time.LocalDate> leaveDays = repository.approvedLeaveDates(actor.id(), first, last);
        java.util.List<StaffAttendanceRecord> punches = repository.listAttendanceForUserBetween(actor.id(),
                first.atStartOfDay().toInstant(java.time.ZoneOffset.UTC),
                last.plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
        java.util.List<StaffAttendanceDayRecord> out = new java.util.ArrayList<>();
        for (java.time.LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            final java.time.LocalDate current = day;
            if (leaveDays.contains(current)) {
                StaffShiftRecord shift = shiftByDay.get(current);
                out.add(new StaffAttendanceDayRecord(current, java.util.List.of("leave"),
                        shift == null ? null : shift.startAt(), shift == null ? null : shift.endAt()));
                continue;
            }
            StaffShiftRecord shift = shiftByDay.get(current);
            if (shift == null) {
                out.add(new StaffAttendanceDayRecord(current, java.util.List.of("rest"), null, null));
                continue;
            }
            java.util.List<StaffAttendanceRecord> dayPunches = punches.stream()
                    .filter(p -> shift.id().equals(p.shiftId())
                            || java.time.LocalDate.ofInstant(p.punchedAt(), java.time.ZoneOffset.UTC).equals(current))
                    .toList();
            StaffAttendanceRecord checkIn = dayPunches.stream()
                    .filter(p -> p.punchType().equals("check_in")).findFirst().orElse(null);
            StaffAttendanceRecord checkOut = dayPunches.stream()
                    .filter(p -> p.punchType().equals("check_out"))
                    .reduce((a, b) -> b).orElse(null);
            java.util.List<String> statuses = new java.util.ArrayList<>();
            if (checkIn == null) {
                statuses.add("absent");
            } else {
                if (checkIn.judgement().equals("late")) {
                    statuses.add("late");
                }
                if (checkOut == null) {
                    statuses.add("missing_checkout");
                } else if (checkOut.judgement().equals("early_leave")) {
                    statuses.add("early_leave");
                }
                if (statuses.isEmpty()) {
                    statuses.add("present");
                }
            }
            out.add(new StaffAttendanceDayRecord(current, statuses, shift.startAt(), shift.endAt()));
        }
        return out;
    }

    private <T> PageResponse<T> page(List<T> items) {
        return new PageResponse<>(items, DEFAULT_PAGE, DEFAULT_PAGE_SIZE, items.size());
    }

    private String idempotencyKeyOf(Object request) {
        if (request instanceof StaffRegistrationRequest r) return r.idempotencyKey();
        if (request instanceof StaffRegistrationReviewRequest r) return r.idempotencyKey();
        if (request instanceof StaffDeviceBindRequest r) return r.idempotencyKey();
        if (request instanceof StaffDeviceRebindDecisionRequest r) return r.idempotencyKey();
        if (request instanceof StaffDeviceIntegrityReportRequest r) return r.idempotencyKey();
        if (request instanceof StaffDeviceRevokeRequest r) return r.idempotencyKey();
        if (request instanceof EmployeeNoticeAcknowledgeRequest r) return r.idempotencyKey();
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String slug(String value) {
        String slug = (value == null ? "" : value).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return slug.isEmpty() ? UUID.randomUUID().toString().replace("-", "") : slug;
    }

    private String codeOf(RuntimeException exception) {
        if (exception instanceof ApiException apiException) {
            return apiException.code();
        }
        return exception.getClass().getSimpleName();
    }
}
