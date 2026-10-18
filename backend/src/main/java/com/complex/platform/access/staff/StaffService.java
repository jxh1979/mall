package com.complex.platform.access.staff;

// 文件版本：V0.19

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
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.gate.GuardedAction;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

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
    // 打卡拉候选班次的时间窗(打卡时刻 ±12 小时)。班次通常 8 小时,12h 足够覆盖早到/晚退,
    // 又不至于把隔天的同名班拉进来。只影响「无班次覆盖」时的兜底匹配:区间覆盖的班必定在窗内,不受它影响。
    // ponytail: 觉得兜底匹配太松(离班 11 小时还认)就把它调小,一个常量的事。
    private static final long SHIFT_MATCH_WINDOW_SECONDS = 12 * 3600L;

    // 一次批量排班最多生成多少个班次(员工数 × 天数)。一周 7 天 × 一个班组几十人绰绰有余,
    // 又拦住「全选员工 × 31 天」手滑一次排出上万条把请求拖死。
    // ponytail: 真有超大班组再往上调,一个常量的事。
    private static final int MAX_BATCH_SHIFTS = 500;

    private final JdbcStaffRepository repository;
    private final AccessRepository accessRepository;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final com.complex.platform.access.AccessCredentialProvisioner credentialProvisioner;
    // 打卡「当下」的来源。生产恒为 Clock.systemUTC()；单测注固定 Clock 对拍考勤日归属（见 StaffAttendanceLocalZoneTests）。
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    private com.complex.platform.access.AccessService accessService;
    @org.springframework.beans.factory.annotation.Autowired
    private com.complex.platform.terminal.TerminalDeviceRepository terminalRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private StaffApprovalService staffApprovals;
    @org.springframework.beans.factory.annotation.Autowired
    private StaffHrRoutingService staffHrRouting;
    @org.springframework.beans.factory.annotation.Autowired
    private StaffRegistrationService registrations;
    @org.springframework.beans.factory.annotation.Autowired
    private com.complex.platform.governance.PlatformRepository platformRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public StaffService(
            JdbcStaffRepository repository,
            AccessRepository accessRepository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            org.springframework.beans.factory.ObjectProvider<com.complex.platform.access.AccessCredentialProvisioner> credentialProvisioner
    ) {
        this(repository, accessRepository, idempotencyService, auditEventService, authorizationService,
                credentialProvisioner.getIfAvailable());
    }

    StaffService(
            JdbcStaffRepository repository,
            AccessRepository accessRepository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            com.complex.platform.access.AccessCredentialProvisioner credentialProvisioner
    ) {
        this(repository, accessRepository, idempotencyService, auditEventService, authorizationService,
                credentialProvisioner, Clock.systemUTC());
    }

    StaffService(
            JdbcStaffRepository repository,
            AccessRepository accessRepository,
            IdempotencyService idempotencyService,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            com.complex.platform.access.AccessCredentialProvisioner credentialProvisioner,
            Clock clock
    ) {
        this.clock = clock;
        this.repository = repository;
        this.accessRepository = accessRepository;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.credentialProvisioner = credentialProvisioner;
    }

    // ============================================================ 员工注册 / 审批

    @GuardedAction(permission = "staff.profile.submit")
    @org.springframework.transaction.annotation.Transactional
    public StaffProfileRecord submitRegistration(StaffRegistrationRequest request) {
        if (request == null) throw ApiException.badRequest("VALIDATION_FAILED", "须填写注册资料");
        requireRegistrationScope(request.projectId(), request.merchantId());
        String approvingCompany = registrations.merchantApprovingCompany(request.merchantId());
        try {
            return idempotencyService.execute(
                    "submitStaffRegistration",
                    request.idempotencyKey(),
                    request,
                    () -> {
                        String projectId = trimToNull(request.projectId());
                        if (projectId == null) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "必须选择项目");
                        }
                        String name = trimToNull(request.name());
                        if (name == null) {
                            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写员工姓名");
                        }
                        if (request.staffLevelId() != null
                                && accessRepository.findStaffLevelById(request.staffLevelId()).isEmpty()) {
                            throw ApiException.badRequest("UNKNOWN_STAFF_LEVEL",
                                    "无法识别的员工职级：" + request.staffLevelId());
                        }
                        Instant now = Instant.now();
                        StaffProfileRecord profile = new StaffProfileRecord(
                                "staff_" + UUID.randomUUID(),
                                projectId,
                                trimToNull(request.merchantId()),
                                null,
                                name,
                                trimToNull(request.phone()),
                                trimToNull(request.idDoc()),
                                trimToNull(request.staffLevelId()),
                                "submitted",
                                null,
                                null,
                                null,
                                "active",
                                now,
                                now
                        );
                        StaffProfileRecord saved = repository.saveProfile(profile.withCompanyRoute(approvingCompany, "merchant_submitted"));
                        repository.recordRegistrationInitiator(saved.id(), authorizationService.currentActor().id());
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
    @org.springframework.transaction.annotation.Transactional
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
                                    "登记不在已提交状态：" + profile.registrationStatus());
                        }
                        String decision = trimToNull(request.decision());
                        AccessActor actor = authorizationService.currentActor();
                        if (repository.isRegistrationInitiator(id, actor.id())
                                && !authorizationService.isMasterAuthority(actor)) {
                            throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN", "不能审批自己发起的员工注册");
                        }
                        if (!authorizationService.isMasterAuthority(actor)
                                && (trimToNull(actor.merchantId()) != null
                                    || profile.companyId() == null
                                    || !profile.companyId().equals(registrations.actorCompany(actor)))) {
                            throw ApiException.forbidden("COMPANY_APPROVAL_REQUIRED", "须由该申请所属公司或商户对应域公司审批");
                        }
                        Instant now = Instant.now();
                        if ("approve".equalsIgnoreCase(decision == null ? "" : decision)) {
                            if (credentialProvisioner == null) {
                                throw ApiException.conflict("CREDENTIAL_PROVIDER_UNAVAILABLE", "登录凭证服务未配置，不能开通员工账号");
                            }
                            String initialPassword = trimToNull(request.initialPassword());
                            if (initialPassword == null) {
                                throw ApiException.badRequest("VALIDATION_FAILED",
                                        "approve 时必须设置员工初始登录密码 (initial_password)");
                            }
                            AccessUser created = accessRepository.createUser(new AccessUser(
                                    "usr_" + profile.id(),
                                    "staff_" + profile.id(),
                                    "staff",
                                    profile.projectId(),
                                    profile.merchantId(),
                                    "active",
                                    now
                            ));
                            credentialProvisioner.provisionCredential(created.id(), initialPassword, "active", now);
                            // 既有缺口修复(2026-07-19):审批通过即为新员工分配"员工自助"角色 role_staff_self,
                            // 否则真实员工(userType=staff)无任何 user_roles 行 → 所有 BYOD 自助端点(我的薪资/
                            // 班次/考勤/请假/通知/打卡)对其一律 403;此前仅 platform_admin 桥可用。幂等(ON CONFLICT DO NOTHING)。
                            accessRepository.assignUserRole(created.id(), "role_staff_self");
                            if (profile.staffLevelId() != null) {
                                accessService.assignStaffLevel(new com.complex.platform.access.dto.StaffLevelAssignmentRequest(
                                        request.idempotencyKey() + ":level", request.operatorNote(), created.id(),
                                        profile.staffLevelId(), null, "CNY", null));
                            }
                            StaffProfileRecord approved = profile.withRegistrationReview(
                                    created.id(), "approved", actor.id(), now, null);
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
                                        "驳回时必须填写驳回理由");
                            }
                            StaffProfileRecord rejected = profile.withRegistrationReview(
                                    profile.userId(), "rejected", actor.id(), now, reason);
                            StaffProfileRecord saved = repository.saveProfile(rejected);
                            createNotice(saved.id(), "approval_result", "注册审批驳回", reason, false,
                                    actorIdOrSystem(actor));
                            auditEventService.recordSuccess("staff.rejected", "staff.profile.review",
                                    "staff_profile", saved.id(), null, request.idempotencyKey(),
                                    Map.of("reason", reason));
                            return saved;
                        }
                        throw ApiException.badRequest("VALIDATION_FAILED",
                                "审批结论必须为通过或驳回");
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
        return listProfiles(false);
    }

    /** V264：includeArchived=false（默认）排除已存档档案；true 供后台管理存档件。 */
    @GuardedAction(permission = "staff.profile.view")
    public PageResponse<StaffProfileRecord> listProfiles(boolean includeArchived) {
        AccessActor actor = authorizationService.currentActor();
        List<StaffProfileRecord> profiles;
        if (!authorizationService.isPlatformAdmin(actor) && trimToNull(actor.merchantId()) == null) {
            var scoped = new java.util.LinkedHashMap<String, StaffProfileRecord>();
            repository.listProfilesByProject(authorizationService.requireProjectScope(actor)).stream()
                    .filter(p -> p.companyId().equals(registrations.actorCompany(actor)))
                    .forEach(p -> scoped.put(p.id(),p));
            repository.listProfilesByCompany(registrations.actorCompany(actor)).forEach(p -> scoped.put(p.id(),p));
            profiles = scoped.values().stream().filter(p -> includeArchived || p.archivedAt() == null).toList();
        } else {
            profiles = repository.listProfiles(merchantScopeOrNull(), includeArchived);
        }
        auditEventService.recordSuccess("staff.profile.viewed", "staff.profile.view",
                "staff_profile", "all", null, null, Map.of("count", profiles.size()));
        return page(profiles);
    }

    // ================= V264 员工档案：存档与审批后删除 =================

    /** 当前操作者 id（存档/删除留痕 + 自审校验）；无鉴权上下文（测试/内部）回退 null。 */
    private String archiveActorId() {
        try {
            return authorizationService.currentActor().id();
        } catch (RuntimeException unauthenticated) {
            return null;
        }
    }

    @GuardedAction(permission = "staff.profile.submit")
    @org.springframework.transaction.annotation.Transactional
    public StaffProfileRecord archiveProfile(String id, boolean archive, String reason) {
        StaffProfileRecord profile = requireProfileInScope(id);
        String r = archive ? requireReason(reason) : null;
        Instant now = Instant.now();
        StaffProfileRecord updated = archive
                ? profile.withArchive(now, archiveActorId(), now)
                : profile.withArchive(null, null, now).withDelete("none", null, null, null, now);
        StaffProfileRecord saved = repository.saveProfile(updated);
        repository.setStaffProfileArchiveReason(id, r);
        auditEventService.recordSuccess(archive ? "staff.profile.archived" : "staff.profile.unarchived",
                "staff.profile.submit", "staff_profile", id, null, null, Map.of("archived", archive));
        return saved;
    }

    /**
     * V349 销售岗维护：人事在「员工档案」页给员工设/撤销「销售岗」标识——卡座提成按岗位判档的唯一依据
     * （销售岗按销售人员费率、其余按非销售人员费率；费率仍在「结算配置中心 → 提成与积分」配，见 V340）。
     *
     * <p>沿用 staff.profile.review（高敏·需审批）而不新增权限点：审员工注册与定这人是不是销售岗同属人事权，
     * 拆两个权限只会多一处要分配、少一处对得上。改动必留审计（改前/改后 + 事由），提成对账时可回溯是谁哪天改的。
     *
     * <p>只改档案标识，<b>不回算任何既有账单/提成</b>：账单在开单时已把角色档快照进 bar_bills.sales_role_type，
     * 今天改岗位只影响此后新开的账单——否则改一次岗位就悄悄改写历史提成，谁都对不上账。
     */
    /**
     * CG-12（BSM:1478「辞退与其他涉及员工的人力操作一样，须经 HR 与该员工直属领导、向上三级复核后生效，生效即转「离职」」）：
     * 任一目标状态（待入职 / 在职 / 试岗 / 停岗 / 离职）都只发起复核，不直接写入；全部通过后由
     * {@link StaffApprovalService#afterDecision} 写入。注册审批时系统置的初始状态不算「变更」，仍走注册流程。
     */
    @GuardedAction(permission = "staff.profile.review")
    @org.springframework.transaction.annotation.Transactional
    public com.complex.platform.governance.review.SupervisorReviewDecisionRecord setEmploymentStatus(String id,
            com.complex.platform.access.staff.dto.StaffEmploymentStatusRequest request) {
        try {
        StaffProfileRecord profile = requireProfileInScope(id);
        if (request == null || request.employmentStatus() == null
                || !java.util.Set.of("pending_entry", "employed", "trial", "suspended", "departed")
                        .contains(request.employmentStatus())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "员工状态须为待入职、在职、试岗、停岗或离职");
        }
        String reason = requireReason(request.reason());
        staffHrRouting.requireDirectSupervisor(profile.userId());
        // U05-011：与其它员工写口相同，按操作命名空间幂等去重。
        return idempotencyService.execute("staffEmploymentStatus:" + id, request.idempotencyKey(), request, () -> {
            var review = staffApprovals.submitEmploymentStatus(profile, request, reason);
            auditEventService.recordSuccess("staff.profile.employment_status_change_requested", "staff.profile.review",
                    "staff_profile", id, null, request.idempotencyKey(),
                    Map.of("before", String.valueOf(profile.employmentStatus()), "after", request.employmentStatus(), "reason", reason,
                            "review_id", review.reviewId()));
            return review;
        }, com.complex.platform.governance.review.SupervisorReviewDecisionRecord.class);
        } catch (RuntimeException exception) {
            // U14-008：失败路径按契约声明的 *_failed 事件留痕。
            auditEventService.recordFailure("staff.profile.employment_status_change_failed", "staff.profile.review", "staff_profile", id, null, idempotencyKeyOf(request), codeOf(exception));
            throw exception;
        }
    }

    @GuardedAction(permission = "staff.profile.review")
    @org.springframework.transaction.annotation.Transactional
    public StaffProfileRecord setSalesPosition(String id,
            com.complex.platform.access.staff.dto.StaffSalesPositionRequest request) {
        StaffProfileRecord profile = requireProfileInScope(id);
        if (request == null || request.salesPosition() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须指定是否为销售岗");
        }
        boolean salesPosition = request.salesPosition();
        String reason = requireReason(request.reason());
        StaffProfileRecord saved = repository.saveProfile(profile.withSalesPosition(salesPosition, Instant.now()));
        auditEventService.recordSuccess("staff.profile.sales_position_changed", "staff.profile.review",
                "staff_profile", id, null, null,
                Map.of("before", profile.salesPosition(), "after", salesPosition, "reason", reason));
        return saved;
    }

    @GuardedAction(permission = "staff.profile.review")
    @org.springframework.transaction.annotation.Transactional
    public StaffProfileRecord requestDeleteProfile(String id,
            com.complex.platform.access.staff.dto.StaffProfileDeleteRequest request) {
        // 最高权限逐笔开放（治理地图第 528 行第④类）：最高权限发起的删除只校验档案存在，不按公司或登记范围。
        StaffProfileRecord profile = authorizationService.isMasterAuthority(authorizationService.currentActor())
                ? repository.findProfileById(id).orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND", "员工档案不存在：" + id))
                : requireProfileInScope(id);
        if (profile.archivedAt() == null) {
            throw ApiException.badRequest("STAFF_PROFILE_NOT_ARCHIVED", "员工档案必须先存档才能申请删除");
        }
        if ("pending".equals(profile.deleteStatus())) {
            throw ApiException.conflict("STAFF_PROFILE_DELETE_ALREADY_PENDING", "该员工档案已有待审批的删除申请");
        }
        Instant now = Instant.now();
        String reason = requireReason(request == null ? null : request.reason());
        StaffProfileRecord saved = repository.saveProfile(profile.withDelete("pending", archiveActorId(), now, reason, now));
        auditEventService.recordSuccess("staff.profile.delete_requested", "staff.profile.review",
                "staff_profile", id, null, null, Map.of());
        if (authorizationService.isMasterAuthority(authorizationService.currentActor())) {
            approveDeleteProfile(id, new com.complex.platform.access.staff.dto.StaffProfileDeleteApprovalRequest(
                    "approved", reason));
            return saved.withDelete("approved", saved.deleteRequestedBy(), saved.deleteRequestedAt(), reason, now);
        }
        return saved;
    }

    @GuardedAction(permission = "staff.profile.approve")
    @org.springframework.transaction.annotation.Transactional
    public com.complex.platform.access.staff.dto.StaffProfileDeleteDecision approveDeleteProfile(String id,
            com.complex.platform.access.staff.dto.StaffProfileDeleteApprovalRequest request) {
        if (!authorizationService.isMasterAuthority(authorizationService.currentActor())) {
            throw ApiException.forbidden("MASTER_APPROVAL_REQUIRED", "员工档案删除须由最高权限本人批准");
        }
        // 最高权限逐笔开放（治理地图第 528 行第④类）：不按公司或登记范围取档案，只校验存在；留痕由 delete_approved/rejected 承担。
        StaffProfileRecord profile = repository.findProfileById(id)
                .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND", "员工档案不存在：" + id));
        if (!"pending".equals(profile.deleteStatus())) {
            throw ApiException.badRequest("STAFF_PROFILE_DELETE_NOT_PENDING", "该员工档案没有待审批的删除申请");
        }
        String decision = request == null || request.decision() == null
                ? null : request.decision().trim().toLowerCase(Locale.ROOT);
        if (!"approved".equals(decision) && !"rejected".equals(decision)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
        }
        if ("rejected".equals(decision)) {
            repository.saveProfile(profile.withDelete("none", null, null, null, Instant.now()));
            auditEventService.recordSuccess("staff.profile.delete_rejected", "staff.profile.approve",
                    "staff_profile", id, null, null, Map.of());
            return new com.complex.platform.access.staff.dto.StaffProfileDeleteDecision(id, "rejected", false);
        }
        long refs = repository.countProfileReferences(id);
        if (refs > 0) {
            throw ApiException.conflict("STAFF_PROFILE_IN_USE",
                    "员工仍挂有 " + refs + " 条考勤、请假、职级、未结束排班或未取寄存记录，不可删除");
        }
        repository.deleteProfile(id);
        auditEventService.recordSuccess("staff.profile.delete_approved", "staff.profile.approve",
                "staff_profile", id, null, null, Map.of());
        return new com.complex.platform.access.staff.dto.StaffProfileDeleteDecision(id, "approved", true);
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
                                    "必须填写员工档案、设备公钥与设备指纹");
                        }
                        // 1.9：拒绝占位设备凭证——必须是 Android Keystore 真实公钥/指纹，不接受 PLACEHOLDER_*。
                        if (publicKey.startsWith("PLACEHOLDER_") || fingerprint.startsWith("PLACEHOLDER_")) {
                            throw ApiException.badRequest("DEVICE_CREDENTIAL_PLACEHOLDER",
                                    "设备公钥与设备指纹必须为真实 Keystore 值，不能用占位符");
                        }
                        requireOwnProfile(staffId);
                        // 换机识别：已有 active 且指纹不同 → 新凭证 pending_review + supersedes 指向旧 active。
                        Optional<StaffDeviceCredentialRecord> active = repository.findActiveCredential(staffId);
                        String supersedes = null;
                        boolean rebind = repository.hasCredentialHistory(staffId);
                        if (active.isPresent()) {
                            if (fingerprint.equals(active.get().deviceFingerprintHash())) {
                                throw ApiException.conflict("ALREADY_BOUND",
                                        "该设备指纹已是生效中的凭证");
                            }
                            supersedes = active.get().id();
                            rebind = true;
                        }
                        Instant now = Instant.now();
                        StaffDeviceCredentialRecord credential = new StaffDeviceCredentialRecord(
                                "scred_" + UUID.randomUUID(),
                                staffId, publicKey, fingerprint, rebind ? "pending_review" : "active",
                                supersedes, "trusted", null, rebind ? null : now, null, now, now);
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
                                    "设备凭证不在待审核状态：" + credential.bindStatus());
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
                                        "员工设备凭证不存在：" + credId));
                        // 手机自助上报只处理本人凭证；后台吊销仍走独立授权入口。
                        requireOwnProfile(credential.staffProfileId());
                        String status = trimToNull(request.integrityStatus());
                        if (status == null
                                || !(status.equals("trusted") || INTEGRITY_FAILURES.contains(status))) {
                            throw ApiException.badRequest("VALIDATION_FAILED",
                                    "设备完整性状态必须为 trusted/root_detected/debug_detected/abnormal 之一");
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
                                    "设备凭证已失效：" + credential.bindStatus());
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

    /** 主管提出排班或修改；上一级批准后才写入生效排班。 */
    @org.springframework.transaction.annotation.Transactional
    public StaffShiftRecord upsertShift(com.complex.platform.access.staff.dto.StaffShiftRequest request) {
        try {
        authorizationService.requirePermission("staff.shift.manage");
        return idempotencyService.execute("staffShiftSubmit", request == null ? null : request.idempotencyKey(), request,
                () -> {
                    StaffShiftRecord submitted = staffApprovals.submitShifts(List.of(draftShift(request)), request.idempotencyKey()).shifts().getFirst();
                    // U16-006：operation 级成功事件；governance.supervisor_review.evaluated 只属于 evaluateSupervisorReview。
                    auditEventService.recordSuccess("staff.shift.submitted", "staff.shift.manage", "staff_shift", submitted.id(), null,
                            request.idempotencyKey(), Map.of("review_id", String.valueOf(submitted.reviewId()), "publication_status", String.valueOf(submitted.publicationStatus())));
                    return submitted;
                },
                StaffShiftRecord.class);
        } catch (RuntimeException exception) {
            // U14-008：失败路径按契约声明的 *_failed 事件留痕。
            auditEventService.recordFailure("staff.shift.save_failed", "staff.shift.manage", "staff_shift", null, null, idempotencyKeyOf(request), codeOf(exception));
            throw exception;
        }
    }

    private StaffShiftRecord draftShift(com.complex.platform.access.staff.dto.StaffShiftRequest request) {
        if (request == null || trimToNull(request.userId()) == null || request.workDate() == null
                || request.startAt() == null || request.endAt() == null || !request.endAt().isAfter(request.startAt())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "须填写员工、工作日期及有效的开始结束时间");
        }
        requireUserInScope(request.userId().trim());
        Instant now = clock.instant();
        StaffShiftRecord previous = trimToNull(request.shiftId()) == null
                ? repository.findShiftByUserStart(request.userId().trim(), request.startAt()).orElse(null)
                : repository.findShiftById(request.shiftId()).orElseThrow(() -> ApiException.notFound("SHIFT_NOT_FOUND", "原排班不存在"));
        if (!request.startAt().isAfter(now) || (previous != null && !previous.startAt().isAfter(now))) {
            throw ApiException.conflict("SHIFT_STARTED", "班次已经开始，不能改写排班；临时顶班须记录顶班安排");
        }
        String actor = authorizationService.currentActor().id();
        if (previous != null && (!previous.userId().equals(request.userId().trim()) || !actor.equals(previous.createdBy()))) {
            throw ApiException.forbidden("SHIFT_AUTHOR_REQUIRED", "排班修改须由原排班负责人办理，员工应先向负责人申请");
        }
        if (previous != null) requireReason(request.note());
        return new StaffShiftRecord(previous == null ? "shf_" + UUID.randomUUID() : previous.id(),
                request.userId().trim(), request.workDate(), request.startAt(), request.endAt(),
                request.pointSpaceIds() == null ? List.of() : request.pointSpaceIds(), request.note(), actor,
                previous == null ? now : previous.createdAt(), now);
    }

    /**
     * 按周批量排班（2026-08-02 用户拍板）：排班员一次排出一周的班，而不是一人一天点一次。
     *
     * <p>选一批员工 + 一个时段 + 勾哪几天 → 一次生成整周班次。日期窗口 = 起始日期起 {@code days} 天，
     * {@code weekdays} 非空只排勾中的星期几。每天只填「几点到几点」：结束时刻不晚于开始时刻
     * （如 22:00-06:00）即跨零点班，班次结束自动顺延到次日，排班员不用手动选日期。
     * 日历日→时刻一律按 {@link LocalZone}（北京）折算，与考勤按 start_at/end_at 区间配班同一把尺子。
     *
     * <p>逐条使用单班相同校验生成草稿，整批一张审批单；批准后整批生效，修改前后均追加留痕。
     */
    @org.springframework.transaction.annotation.Transactional
    public com.complex.platform.access.staff.dto.StaffShiftBatchResult batchUpsertShifts(
            com.complex.platform.access.staff.dto.StaffShiftBatchRequest request) {
        try {
        authorizationService.requirePermission("staff.shift.manage");
        return idempotencyService.execute("staffShiftBatchSubmit", request == null ? null : request.idempotencyKey(), request,
                () -> {
                    com.complex.platform.access.staff.dto.StaffShiftBatchResult submitted = draftShiftBatch(request);
                    // U16-006：一批一条 operation 级成功事件，business_id=审批单。
                    auditEventService.recordSuccess("staff.shift.submitted", "staff.shift.manage", "staff_shift", submitted.reviewId(), null,
                            request.idempotencyKey(), Map.of("created", submitted.createdCount(), "updated", submitted.updatedCount(), "publication_status", String.valueOf(submitted.publicationStatus())));
                    return submitted;
                }, com.complex.platform.access.staff.dto.StaffShiftBatchResult.class);
        } catch (RuntimeException exception) {
            // U14-008：失败路径按契约声明的 *_failed 事件留痕。
            auditEventService.recordFailure("staff.shift.save_failed", "staff.shift.manage", "staff_shift", null, null, idempotencyKeyOf(request), codeOf(exception));
            throw exception;
        }
    }

    private com.complex.platform.access.staff.dto.StaffShiftBatchResult draftShiftBatch(
            com.complex.platform.access.staff.dto.StaffShiftBatchRequest request) {
        if (request == null || request.startDate() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写排班起始日期");
        }
        List<String> userIds = request.userIds() == null ? List.of() : request.userIds().stream()
                .map(id -> trimToNull(id)).filter(java.util.Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须选择至少一名员工");
        }
        int days = request.days() == null ? 7 : request.days();
        if (days < 1 || days > 31) {
            throw ApiException.badRequest("VALIDATION_FAILED", "排班天数必须在 1 到 31 天之间");
        }
        java.util.Set<java.time.DayOfWeek> weekdays = parseWeekdays(request.weekdays());
        java.time.LocalTime startTime = parseShiftTime(request.startTime(), "开始");
        java.time.LocalTime endTime = parseShiftTime(request.endTime(), "结束");

        List<java.time.LocalDate> dates = java.util.stream.IntStream.range(0, days)
                .mapToObj(request.startDate()::plusDays)
                .filter(date -> weekdays.isEmpty() || weekdays.contains(date.getDayOfWeek()))
                .toList();
        if (dates.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "所选日期范围内没有勾中的星期几，请至少勾一天");
        }
        long total = (long) dates.size() * userIds.size();
        if (total > MAX_BATCH_SHIFTS) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "一次最多生成 " + MAX_BATCH_SHIFTS + " 个班次（本次 " + userIds.size() + " 名员工 × "
                            + dates.size() + " 天 = " + total + " 个），请分批排");
        }
        List<String> points = request.pointSpaceIds() == null ? List.of() : request.pointSpaceIds().stream()
                .map(id -> trimToNull(id)).filter(java.util.Objects::nonNull).distinct().toList();

        userIds.forEach(this::requireUserInScope);
        java.util.List<StaffShiftRecord> saved = new java.util.ArrayList<>();
        for (java.time.LocalDate date : dates) {
            Instant startAt = date.atTime(startTime).atZone(LocalZone.ID).toInstant();
            // 跨零点：结束时刻不晚于开始时刻 → 结束落在次日（22:00-06:00 排班员只填时段即可）。
            java.time.LocalDate endDate = endTime.isAfter(startTime) ? date : date.plusDays(1);
            Instant endAt = endDate.atTime(endTime).atZone(LocalZone.ID).toInstant();
            for (String userId : userIds) {
                StaffShiftRecord row = draftShift(new com.complex.platform.access.staff.dto.StaffShiftRequest(
                        request.idempotencyKey(), userId, date, startAt, endAt, points, request.note()));
                saved.add(row);
            }
        }
        return staffApprovals.submitShifts(List.copyOf(saved), request.idempotencyKey());
    }

    private static java.util.Set<java.time.DayOfWeek> parseWeekdays(List<Integer> weekdays) {
        if (weekdays == null || weekdays.isEmpty()) {
            return java.util.Set.of();
        }
        java.util.EnumSet<java.time.DayOfWeek> parsed = java.util.EnumSet.noneOf(java.time.DayOfWeek.class);
        for (Integer weekday : weekdays) {
            if (weekday == null || weekday < 1 || weekday > 7) {
                throw ApiException.badRequest("VALIDATION_FAILED", "星期几必须取 1（周一）到 7（周日）");
            }
            parsed.add(java.time.DayOfWeek.of(weekday));
        }
        return parsed;
    }

    private static java.time.LocalTime parseShiftTime(String value, String label) {
        String text = trimToNull(value);
        if (text == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写每日" + label + "时刻");
        }
        try {
            return java.time.LocalTime.parse(text);
        } catch (java.time.format.DateTimeParseException exception) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "每日" + label + "时刻格式必须为 时:分（如 22:00），当前填的是「" + text + "」");
        }
    }

    public List<StaffApprovalService.History> shiftHistory(String id) {
        auditEventService.recordSuccess("staff.shift.history_viewed", "staff.shift.manage", "staff_shift", id, null, null, Map.of());
        authorizationService.requirePermission("staff.shift.manage");
        var history = staffApprovals.history(id);
        if (history.isEmpty()) {
            var shift = repository.findShiftById(id).orElseThrow(() -> ApiException.notFound("SHIFT_NOT_FOUND", "排班不存在"));
            requireUserInScope(shift.userId());
        } else { requireUserInScope(history.getFirst().after().userId()); }
        return history;
    }

    /** E1:后台/主管按日(可选按人)查排班。 */
    public PageResponse<StaffShiftRecord> listShifts(java.time.LocalDate workDate, String userId) {
        return listShifts(workDate, userId, 1, 500);
    }

    /** U06-004（BSM L1443）：已开始/任意班次的临时顶班——领班直接安排，只追加记录，不改写原班次、不走上一级排班审批。 */
    @org.springframework.transaction.annotation.Transactional
    public StaffShiftCoverRecord coverShift(String shiftId, com.complex.platform.access.staff.dto.StaffShiftCoverRequest request) {
        try {
        authorizationService.requirePermission("staff.shift.manage");
        StaffShiftRecord shift = repository.findShiftById(trimToNull(shiftId) == null ? "" : shiftId.trim())
                .orElseThrow(() -> ApiException.notFound("SHIFT_NOT_FOUND", "原排班不存在"));
        String coverUserId = request == null ? null : trimToNull(request.coverUserId());
        if (coverUserId == null) throw ApiException.badRequest("VALIDATION_FAILED", "须填写顶班员工");
        if (coverUserId.equals(shift.userId())) throw ApiException.badRequest("VALIDATION_FAILED", "顶班员工不能是原班次员工");
        String reason = requireReason(request.reason());
        requireUserInScope(coverUserId);
        String actor = authorizationService.currentActor().id();
        return idempotencyService.execute("staffShiftCover:" + shift.id(), request.idempotencyKey(), request, () -> {
            StaffShiftCoverRecord saved = repository.saveShiftCover(new StaffShiftCoverRecord("cover_" + UUID.randomUUID(),
                    shift.id(), shift.userId(), coverUserId, actor, reason, clock.instant()));
            auditEventService.recordSuccess("staff.shift.covered", "staff.shift.manage", "staff_shift", shift.id(), null,
                    request.idempotencyKey(), Map.of("cover_user_id", coverUserId, "original_user_id", shift.userId(), "reason", reason));
            return saved;
        }, StaffShiftCoverRecord.class);
        } catch (RuntimeException exception) {
            // U14-008：失败路径按契约声明的 *_failed 事件留痕。
            auditEventService.recordFailure("staff.shift.cover_failed", "staff.shift.manage", "staff_shift", shiftId, null, idempotencyKeyOf(request), codeOf(exception));
            throw exception;
        }
    }

    public List<StaffShiftCoverRecord> shiftCovers(String shiftId) {
        authorizationService.requirePermission("staff.shift.manage");
        List<StaffShiftCoverRecord> covers = repository.listShiftCovers(shiftId);
        auditEventService.recordSuccess("staff.shift.covers_viewed", "staff.shift.manage", "staff_shift", shiftId, null, null, Map.of("count", covers.size()));
        return covers;
    }

    // AUD-005 真分页: SQL COUNT + LIMIT/OFFSET, total 取真值; 默认 500 保留既有一次拉取。
    public PageResponse<StaffShiftRecord> listShifts(java.time.LocalDate workDate, String userId, int page, int size) {
        authorizationService.requirePermission("staff.shift.manage");
        return repository.listShiftsPaged(workDate, userId, projectScope(), staffMerchantScope(), staffCompanyScope(), page, size);
    }

    /** U06-001：员工所属经营主体配置键，写读共用——商户员工 merchant:<merchantId>，公司员工 company:<companyId>；不回落 projectId。 */
    static String settingScopeKey(String merchantId, String companyId) {
        if (trimToNull(merchantId) != null) return "merchant:" + merchantId.trim();
        if (trimToNull(companyId) == null) throw ApiException.conflict("STAFF_COMPANY_REQUIRED", "员工档案未归属公司，无法确定经营主体配置");
        return "company:" + companyId.trim();
    }

    private String staffSettingScope() {
        AccessActor actor = authorizationService.currentActor();
        return settingScopeKey(actor.merchantId(), registrations.actorCompany(actor));
    }

    public StaffNumericSetting getShiftVisibility() {
        authorizationService.requirePermission("staff.shift.manage");
        String scope = staffSettingScope();
        StaffNumericSetting value = repository.numericSetting("shift_visible_days", scope);
        auditEventService.recordSuccess("staff.shift.visibility_viewed", "staff.shift.manage", "staff_policy", scope,
                null, null, Map.of("value", value.value(), "min", value.minValue(), "max", value.maxValue()));
        return value;
    }

    @org.springframework.transaction.annotation.Transactional
    public StaffNumericSetting setShiftVisibility(com.complex.platform.access.staff.dto.StaffNumericSettingRequest request) {
        try {
        authorizationService.requirePermission("staff.shift.manage");
        StaffNumericSetting value = requireNumericSetting(request, 1, Integer.MAX_VALUE);
        String scope = staffSettingScope();
        // U05-011：与其它员工写口相同，按操作命名空间幂等去重。
        return idempotencyService.execute("staffShiftVisibility:" + scope, request.idempotencyKey(), request, () -> {
            repository.saveNumericSetting("shift_visible_days", scope, value, authorizationService.currentActor().id());
            auditEventService.recordSuccess("staff.shift.visibility_changed", "staff.shift.manage", "staff_policy", scope,
                    null, null, Map.of("value", value.value(), "min", value.minValue(), "max", value.maxValue(), "reason", request.reason()));
            return value;
        }, StaffNumericSetting.class);
        } catch (RuntimeException exception) {
            // U14-008：失败路径按契约声明的 *_failed 事件留痕。
            auditEventService.recordFailure("staff.shift.visibility_change_failed", "staff.shift.manage", "staff_policy", "shift_visible_days", null, idempotencyKeyOf(request), codeOf(exception));
            throw exception;
        }
    }

    public StaffNumericSetting getPenaltyCap() {
        authorizationService.requirePermission("staff.payroll.manage");
        StaffNumericSetting value = repository.numericSetting("payroll_penalty_cap_bps", "global");
        auditEventService.recordSuccess("staff.payroll.penalty_cap_viewed", "staff.payroll.manage", "staff_policy", "global",
                null, null, Map.of("value", value.value(), "min", value.minValue(), "max", value.maxValue()));
        return value;
    }

    @org.springframework.transaction.annotation.Transactional
    public com.complex.platform.governance.review.SupervisorReviewDecisionRecord requestPenaltyCap(
            com.complex.platform.access.staff.dto.StaffPenaltyCapRequest request) {
        try {
        authorizationService.requirePermission("staff.payroll.manage");
        var value = requireNumericSetting(request == null ? null : new com.complex.platform.access.staff.dto.StaffNumericSettingRequest(
                request.idempotencyKey(), request.value(), request.minValue(), request.maxValue(), request.reason()), 0, 10000);
        return idempotencyService.execute("staffPenaltyCapRequest", request.idempotencyKey(), request,
                () -> {
                    var submitted = staffApprovals.submitPenaltyCap(value, request);
                    // U16-006：operation 级成功事件。
                    auditEventService.recordSuccess("staff.payroll.penalty_cap_requested", "staff.payroll.manage", "staff_policy", "payroll_penalty_cap_bps", null,
                            request.idempotencyKey(), Map.of("value", value.value(), "review_id", submitted.reviewId()));
                    return submitted;
                },
                com.complex.platform.governance.review.SupervisorReviewDecisionRecord.class);
        } catch (RuntimeException exception) {
            // U14-008：失败路径按契约声明的 *_failed 事件留痕。
            auditEventService.recordFailure("staff.payroll.penalty_cap_request_failed", "staff.payroll.manage", "staff_policy", "payroll_penalty_cap_bps", null, idempotencyKeyOf(request), codeOf(exception));
            throw exception;
        }
    }

    private static String requireReason(String reason) {
        String value = trimToNull(reason);
        if (value == null) throw ApiException.badRequest("VALIDATION_FAILED", "请填写改动缘由");
        return value;
    }

    private StaffNumericSetting requireNumericSetting(com.complex.platform.access.staff.dto.StaffNumericSettingRequest request,
            int lower, int upper) {
        if (request == null || request.value() == null || request.minValue() == null || request.maxValue() == null
                || request.minValue() < lower || request.maxValue() > upper
                || request.value() < request.minValue() || request.value() > request.maxValue()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "须同时填写参数值及有效上下限，参数值须在上下限内");
        }
        requireReason(request.reason());
        return new StaffNumericSetting(request.value(), request.minValue(), request.maxValue());
    }

    /** 员工查看所属经营主体配置天数内的本人排班，出厂为含今天在内七天。 */
    public PageResponse<StaffShiftRecord> listMyShifts() {
        authorizationService.requirePermission("staff.shift.view");
        var actor = authorizationService.currentActor();
        java.time.LocalDate today = java.time.LocalDate.ofInstant(clock.instant(), LocalZone.ID);
        StaffProfileRecord profile = repository.findProfileByUserId(actor.id())
                .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND", "员工档案不存在"));
        String scope = settingScopeKey(profile.merchantId(), profile.companyId());
        int days = repository.numericSetting("shift_visible_days", scope).value();
        return page(repository.listShiftsForUserBetween(actor.id(), today, today.plusDays(days - 1L)));
    }

    /** E2(2026-07-02 拍板):打卡——打卡时刻取服务器(客户端不可篡改);严格无宽限:上班晚于班次开始即迟到、
     * 下班早于班次结束即早退;匹配不到班次记 no_shift;只记录不自动扣罚(处理归人事)。
     *
     * <p>2026-08-02:配哪个班改按<b>时间区间</b>判定,不再折日历日——排班本来就存「几月几日几点→几月几日几点」,
     * 跨零点班(8/2 22:00→8/3 06:00)的 work_date 是 8/2,按打卡当天查必然漏,员工凌晨下班被记「无排班」。 */
    @org.springframework.transaction.annotation.Transactional
    public StaffAttendanceRecord punch(com.complex.platform.access.staff.dto.StaffAttendancePunchRequest request) {
        try {
        authorizationService.requirePermission("staff.attendance.punch");
        if (request == null || request.punchType() == null
                || (!request.punchType().equals("check_in") && !request.punchType().equals("check_out"))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "打卡类型必须为上班（check_in）或下班（check_out）");
        }
        String method = request.method();
        if (method == null || !java.util.Set.of("nfc", "qr", "charging_disconnect").contains(method)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "打卡须使用工作手机 NFC、扫码或离开充电设施事件");
        }
        if (!"charging_disconnect".equals(method) && trimToNull(request.tagRef()) == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "NFC 或扫码须包含读取到的点位标签");
        }
        if (("charging_disconnect".equals(method) || "nfc".equals(method)) && !"check_in".equals(request.punchType())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "NFC 和离开充电设施仅记录上班领取");
        }
        var actor = authorizationService.currentActor();
        var terminal = com.complex.platform.gate.TerminalIdentityHolder.current()
                .filter(t -> t.verified() && "staff_work_phone".equals(t.terminalType()))
                .orElseThrow(() -> ApiException.forbidden("WORK_PHONE_REQUIRED", "须从已验证的本人工作手机记录考勤"));
        var device = terminalRepository.findByDeviceId(terminal.deviceId())
                .filter(d -> "bound".equals(d.status()) && "staff_work_phone".equals(d.terminalType()))
                .orElseThrow(() -> ApiException.forbidden("WORK_PHONE_REQUIRED", "工作手机当前不可用"));
        var profile = repository.findProfileByUserId(actor.id())
                .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND", "员工档案不存在"));
        if (!profile.id().equals(device.staffId())) {
            throw ApiException.forbidden("WORK_PHONE_OWNER_REQUIRED", "只能用分配给本人的工作手机记录考勤");
        }
        return idempotencyService.execute("staffAttendancePunch", request.idempotencyKey(), request,
                () -> recordPunch(request, actor, terminal.deviceId()), StaffAttendanceRecord.class);
        } catch (RuntimeException exception) {
            // U14-008：失败路径按契约声明的 *_failed 事件留痕。
            auditEventService.recordFailure("staff.attendance.punch_failed", "staff.attendance.punch", "staff_attendance", null, null, idempotencyKeyOf(request), codeOf(exception));
            throw exception;
        }
    }

    private StaffAttendanceRecord recordPunch(
            com.complex.platform.access.staff.dto.StaffAttendancePunchRequest request, AccessActor actor, String deviceId) {
        repository.lockAttendanceUser(actor.id());
        String method = request.method();
        Instant now = clock.instant();
        java.util.List<StaffShiftRecord> shifts = repository.listShiftsForUserOverlapping(
                actor.id(), now.minusSeconds(SHIFT_MATCH_WINDOW_SECONDS), now.plusSeconds(SHIFT_MATCH_WINDOW_SECONDS));
        StaffShiftRecord shift = pickShift(shifts, request.punchType(), now);
        if ("check_in".equals(request.punchType())) {
            var first = repository.findFirstCheckIn(actor.id(), shift == null ? null : shift.id(),
                    java.time.LocalDate.ofInstant(now, LocalZone.ID));
            if (first.isPresent()) return first.get();
        }
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
                deviceId, shift == null ? null : shift.id(), judgement, now));
        auditEventService.recordSuccess("staff.attendance.punched", "staff.attendance.punch",
                "staff_attendance_record", saved.id(), null, request.idempotencyKey(),
                java.util.Map.of("punch_type", saved.punchType(), "judgement", judgement, "method", method));
        return saved;
    }

    /** 配班两段优先级:先认「区间覆盖打卡时刻」的班(跨零点班照样命中);一个都没覆盖再退回「距起止最近」
     * (保留早到/晚退也能匹配上的既有行为)。多个覆盖时仍取最近。 */
    private StaffShiftRecord pickShift(java.util.List<StaffShiftRecord> shifts, String punchType, Instant now) {
        java.util.List<StaffShiftRecord> covering = shifts.stream()
                .filter(shift -> !now.isBefore(shift.startAt()) && !now.isAfter(shift.endAt()))
                .toList();
        return nearestShift(covering.isEmpty() ? shifts : covering, punchType, now);
    }

    private static StaffShiftRecord nearestShift(java.util.List<StaffShiftRecord> shifts, String punchType,
            Instant now) {
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
        return listAttendance(date, 1, 1000);
    }

    public PageResponse<StaffAttendanceRecord> listAttendance(java.time.LocalDate date, int page, int size) {
        authorizationService.requirePermission("staff.attendance.view");
        return repository.listAttendanceOnPaged(date, projectScope(), staffMerchantScope(), staffCompanyScope(), page, size);
    }

    @GuardedAction(permission = "staff.notice.view")
    public PageResponse<EmployeeNoticeRecord> listNotices() {
        List<EmployeeNoticeRecord> notices = repository.listNoticesForUser(authorizationService.currentActor().id());
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
                                        "员工告知书不存在：" + noticeId));
                        requireOwnProfile(notice.staffProfileId());
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
                                    "该告知书必须明确选择同意或拒绝");
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

    // ── BYOD 二期 Wave1(2026-07-19 拍板):本人档案自助 + 工资条提成明细 ──

    /** Wave1①:员工查本人某月提成明细(酒水/卡座提成 + 服务点单份额),与工资条 commissionCent 对账。 */
    public PageResponse<StaffCommissionLineRecord> listMyCommissions(String month) {
        authorizationService.requirePermission("staff.payroll.view_mine");
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写月份，格式 YYYY-MM");
        }
        var actor = authorizationService.currentActor();
        return page(repository.listCommissionLines(actor.id(), month));
    }

    /** Wave1②:员工查本人档案(姓名/状态);身份按 token 派生,只返本人。 */
    public StaffProfileRecord getMyProfile() {
        authorizationService.requirePermission("staff.profile.self");
        var actor = authorizationService.currentActor();
        return repository.findProfileByUserId(actor.id())
                .orElseThrow(() -> ApiException.notFound("PROFILE_NOT_FOUND", "本账号暂无员工档案"));
    }

    /** Wave1②:员工改本人有限字段(当前仅姓名);token 派生本人,不信请求体 userId。 */
    public StaffProfileRecord updateMyProfile(
            com.complex.platform.access.staff.dto.StaffProfileSelfUpdateRequest request) {
        authorizationService.requirePermission("staff.profile.self");
        String name = trimToNull(request == null ? null : request.name());
        if (name == null || name.length() > 128) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写名称（不超过 128 字）");
        }
        var actor = authorizationService.currentActor();
        StaffProfileRecord current = repository.findProfileByUserId(actor.id())
                .orElseThrow(() -> ApiException.notFound("PROFILE_NOT_FOUND", "本账号暂无员工档案"));
        Instant now = Instant.now();
        StaffProfileRecord updated = new StaffProfileRecord(
                current.id(), current.projectId(), current.merchantId(), current.userId(), name,
                current.phone(), current.idDoc(), current.staffLevelId(), current.registrationStatus(),
                current.approvedBy(), current.approvedAt(), current.rejectReason(), current.status(),
                current.createdAt(), now, current.archivedAt(), current.archivedBy(), current.deleteStatus(),
                current.deleteRequestedBy(), current.deleteRequestedAt(), current.deleteReason(),
                // V349：员工自助只能改姓名，销售岗原样带过——员工不能自己把自己设成销售岗给自己提档提成。
                current.salesPosition(), current.employmentStatus(), current.companyId(), current.registrationSource());
        StaffProfileRecord saved = repository.saveProfile(updated);
        auditEventService.recordSuccess("staff.profile.self_updated", "staff.profile.self",
                "staff_profile", saved.id(), null, request.idempotencyKey(),
                java.util.Map.of("field", "name"));
        return saved;
    }

    /** 平台角色 → null（见全量）；商户角色 → merchantId（仅见本户）。 */
    private String merchantScopeOrNull() {
        return authorizationService.currentMerchantScope().orElse(null);
    }

    /** 读取并校验 profile 在当前 scope 内（跨商户不可见即拒绝）。 */
    private StaffProfileRecord requireProfileInScope(String id) {
        StaffProfileRecord profile = repository.findProfileById(id)
                .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND", "员工档案不存在：" + id));
        var actor = authorizationService.currentActor();
        if (!authorizationService.isPlatformAdmin(actor) && trimToNull(actor.merchantId()) == null) {
            if (!profile.companyId().equals(registrations.actorCompany(actor))) {
                throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "该员工不在当前公司范围内");
            }
        } else {
            requireRegistrationScope(profile.projectId(), profile.merchantId());
        }
        return profile;
    }

    private StaffProfileRecord requireOwnProfile(String id) {
        StaffProfileRecord profile = requireProfileInScope(id);
        if (!authorizationService.currentActor().id().equals(profile.userId())) {
            throw ApiException.forbidden("STAFF_OWNER_REQUIRED", "只能操作本人的员工档案及手机");
        }
        return profile;
    }

    private void requireRegistrationScope(String projectId, String merchantId) {
        AccessActor actor = authorizationService.currentActor();
        if (authorizationService.isPlatformAdmin(actor)) return;
        if (!authorizationService.requireProjectScope(actor).equals(projectId)) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "不能处理其他公司的员工登记");
        }
        if (trimToNull(actor.merchantId()) != null && !actor.merchantId().equals(trimToNull(merchantId))) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "不能处理其他商户的员工登记");
        }
    }

    private StaffDeviceCredentialRecord requireCredentialInScope(String credId) {
        StaffDeviceCredentialRecord credential = repository.findCredentialById(credId)
                .orElseThrow(() -> ApiException.notFound("CREDENTIAL_NOT_FOUND",
                        "员工设备凭证不存在：" + credId));
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

    // ============================================================
    // CG-07 派驻记录（BSM「派驻人员的成本与报酬」：所属主体、派驻酒吧、派驻起止时间须分别记录、可分别统计）
    // ============================================================

    /** CG-07：应收应付台账写入端口（提成月结内部结算应付）。字段注入免改构造点；无容器单测为 null 则不挂账。 */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.complex.platform.finance.FinanceRepository financeRepository;

    /** 登记派驻：所属主体取自员工档案 company_id；同一员工在同一酒吧只能有一条进行中的派驻。需 staff.profile.review。 */
    public StaffDispatchRecord createDispatch(com.complex.platform.access.staff.dto.StaffDispatchCreateRequest request) {
        try {
            requireDispatchOperator();
            if (request == null || trimToNull(request.userId()) == null || trimToNull(request.merchantId()) == null
                    || trimToNull(request.startDate()) == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "必须填写员工、派驻酒吧与起始日期");
            }
            var destination = requireDispatchScope(request.userId().trim(), request.merchantId().trim());
            return idempotencyService.execute("createStaffDispatch",
                    request.idempotencyKey(), request,
                    () -> doCreateDispatch(request, destination), StaffDispatchRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff.dispatch.create_failed", "staff.profile.review",
                    "staff_dispatch_record", request == null ? null : request.userId(), null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    private void requireDispatchOperator() {
        authorizationService.requirePermission("staff.profile.review");
        var actor = authorizationService.currentActor();
        if (trimToNull(actor.merchantId()) != null || java.util.Set.of("merchant", "merchant_admin", "outsourced_vendor")
                .contains(actor.accountType())) {
            throw ApiException.forbidden("STAFF_DISPATCH_PLATFORM_REQUIRED", "派驻登记及结束只允许平台侧获授权人员办理");
        }
    }

    private com.complex.platform.governance.MerchantRecord requireDispatchScope(String userId, String merchantId) {
        var actor = authorizationService.currentActor();
        var employee = accessRepository.findUserById(userId)
                .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND", "员工账号不存在"));
        if (!authorizationService.canReachUser(actor, employee, "staff.profile.review")) {
            throw ApiException.forbidden("CROSS_ORG_FORBIDDEN", "该员工不在当前授权组织范围内");
        }
        var merchant = platformRepository.findMerchantById(merchantId)
                .orElseThrow(() -> ApiException.notFound("MERCHANT_NOT_FOUND", "派驻商户不存在"));
        if (trimToNull(merchant.projectId()) == null
                || !authorizationService.canReachProject(actor, merchant.projectId(), "staff.profile.review")) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "派驻商户不在当前授权项目范围内");
        }
        return merchant;
    }

    private StaffDispatchRecord doCreateDispatch(com.complex.platform.access.staff.dto.StaffDispatchCreateRequest request,
            com.complex.platform.governance.MerchantRecord destination) {
        java.time.LocalDate start = parseDate(request.startDate(), "起始日期");
        String userId = request.userId().trim();
        String merchantId = request.merchantId().trim();
        StaffProfileRecord profile = repository.findProfileByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND", "员工档案不存在"));
        if (trimToNull(profile.companyId()) == null) {
            throw ApiException.conflict("STAFF_COMPANY_REQUIRED", "员工档案未归属主体，不能登记派驻");
        }
        if (repository.hasActiveDispatch(userId, merchantId)) {
            throw ApiException.conflict("STAFF_DISPATCH_ALREADY_ACTIVE", "该员工在该酒吧已有进行中的派驻记录");
        }
        Instant now = Instant.now();
        StaffDispatchRecord saved = repository.saveDispatch(new StaffDispatchRecord(
                "dsp_" + UUID.randomUUID(), userId, profile.companyId(), merchantId, destination.projectId(),
                start, null, "active", trimToNull(request.note()), actorIdOrSystem(authorizationService.currentActor()), now, null, null));
        auditEventService.recordSuccess("staff.dispatch.created", "staff.profile.review", "staff_dispatch_record",
                saved.id(), null, request.idempotencyKey(), Map.of("user_id", userId, "merchant_id", merchantId,
                        "company_id", saved.companyId(), "start_date", start.toString()));
        return saved;
    }

    /** 结束派驻：结束日期默认当日，不得早于起始日期。需 staff.profile.review。 */
    public StaffDispatchRecord endDispatch(String dispatchId, com.complex.platform.access.staff.dto.StaffDispatchEndRequest request) {
        try {
            requireDispatchOperator();
            StaffDispatchRecord scoped = repository.findDispatchById(requireText(dispatchId, "dispatch_id"))
                    .orElseThrow(() -> ApiException.notFound("STAFF_DISPATCH_NOT_FOUND", "派驻记录不存在"));
            requireDispatchScope(scoped.userId(), scoped.merchantId());
            if (!authorizationService.canReachProject(authorizationService.currentActor(), scoped.projectId(), "staff.profile.review")) {
                throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "派驻记录不在当前授权项目范围内");
            }
            return idempotencyService.execute("endStaffDispatch",
                    request == null ? null : request.idempotencyKey(), request,
                    () -> doEndDispatch(dispatchId, request), StaffDispatchRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff.dispatch.end_failed", "staff.profile.review",
                    "staff_dispatch_record", dispatchId, null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    private StaffDispatchRecord doEndDispatch(String dispatchId, com.complex.platform.access.staff.dto.StaffDispatchEndRequest request) {
        StaffDispatchRecord current = repository.findDispatchById(requireText(dispatchId, "dispatch_id"))
                .orElseThrow(() -> ApiException.notFound("STAFF_DISPATCH_NOT_FOUND", "派驻记录不存在"));
        if (!"active".equals(current.status())) {
            throw ApiException.conflict("STAFF_DISPATCH_ALREADY_ENDED", "该派驻记录已结束");
        }
        java.time.LocalDate end = request == null || trimToNull(request.endDate()) == null
                ? java.time.LocalDate.ofInstant(clock.instant(), LocalZone.ID) : parseDate(request.endDate(), "结束日期");
        if (end.isBefore(current.startDate())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "结束日期不得早于起始日期");
        }
        Instant now = Instant.now();
        StaffDispatchRecord saved = repository.saveDispatch(new StaffDispatchRecord(current.id(), current.userId(),
                current.companyId(), current.merchantId(), current.projectId(), current.startDate(), end, "ended",
                request == null || trimToNull(request.reason()) == null ? current.note() : request.reason().trim(),
                current.createdBy(), current.createdAt(), actorIdOrSystem(authorizationService.currentActor()), now));
        auditEventService.recordSuccess("staff.dispatch.ended", "staff.profile.review", "staff_dispatch_record",
                saved.id(), null, request == null ? null : request.idempotencyKey(),
                Map.of("user_id", saved.userId(), "merchant_id", saved.merchantId(), "end_date", end.toString()));
        return saved;
    }

    /** 派驻记录列表：按项目 scope 过滤；商户范围调用方只见本商户。需 staff.profile.view。 */
    public PageResponse<StaffDispatchRecord> listDispatches(String userId, String merchantId) {
        authorizationService.requirePermission("staff.profile.view");
        String merchantScope = staffMerchantScope();
        String effectiveMerchant = merchantScope != null ? merchantScope : trimToNull(merchantId);
        var actor = authorizationService.currentActor();
        var reach = authorizationService.projectReach(actor, "staff.profile.view");
        java.util.List<StaffDispatchRecord> items = repository.listDispatches(trimToNull(userId), effectiveMerchant,
                reach.onlyOwnProject().orElse(null)).stream()
                .filter(row -> reach.covers(row.projectId()))
                .filter(row -> merchantScope != null || accessRepository.findUserById(row.userId())
                        .filter(employee -> authorizationService.canReachUser(reach, employee)).isPresent())
                .toList();
        auditEventService.recordSuccess("staff.dispatch.viewed", "staff.profile.view", "staff_dispatch", "list",
                effectiveMerchant, null, Map.of("count", items.size()));
        return page(items);
    }

    // ============================================================
    // CG-07 提成月结内部结算（BSM「营销人员的归属与报酬」「派驻人员的成本与报酬」：由该酒吧按月拨付给人员所属主体，
    // 作为平台内部各主体之间的普通内部结算应收应付，按三个付账日结清；总额收付、不轧差）
    // ============================================================

    /** 按（酒吧商户 → 人员所属主体 → 月）汇总派驻人员提成，生成内部结算应付；重复执行不重复挂账。需 staff.payroll.manage。 */
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> closeCommissionSettlement(
            com.complex.platform.access.staff.dto.StaffCommissionSettlementCloseRequest request) {
        try {
            authorizationService.requirePermission("staff.payroll.manage");
            if (request == null || request.month() == null || !request.month().matches("\\d{4}-\\d{2}")) {
                throw ApiException.badRequest("VALIDATION_FAILED", "必须填写月份，格式 YYYY-MM");
            }
            int created = doCloseCommissionSettlement(request.month());
            auditEventService.recordSuccess("staff.commission_settlement.closed", "staff.payroll.manage",
                    "bar_commission_settlement", request.month(), null, request.idempotencyKey(),
                    Map.of("month", request.month(), "settlements", created));
            return Map.of("month", request.month(), "settlements", created);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff.commission_settlement.close_failed", "staff.payroll.manage",
                    "bar_commission_settlement", request == null ? null : request.month(), null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    /**
     * 每组（酒吧, 所属主体）：先写月结记录（同键已存在则跳过，重复月结不重复挂账），再向台账追加一条
     * direction=payable、source_type=dispatch_commission_transfer 的应付：counterparty=人员所属主体，merchant=酒吧，
     * 金额=该月派驻人员提成合计（总额收付、不轧差），到期=月末后的下一个付账日，会计期间=该月。
     */
    private int doCloseCommissionSettlement(String month) {
        if (financeRepository == null) {
            throw ApiException.conflict("FINANCE_LEDGER_UNAVAILABLE", "应收应付台账未接入，不得月结");
        }
        java.time.YearMonth yearMonth = java.time.YearMonth.parse(month);
        java.time.LocalDate dueDate = nextInternalPayDayAfter(yearMonth.atEndOfMonth());
        String actor = actorIdOrSystem(authorizationService.currentActor());
        java.time.Instant now = java.time.Instant.now();
        int created = 0;
        for (Map<String, Object> row : repository.aggregateDispatchedCommissions(month)) {
            String merchantId = (String) row.get("merchant_id");
            String companyId = (String) row.get("company_id");
            String projectId = (String) row.get("project_id");
            long amountCent = ((Number) row.get("amount_cent")).longValue();
            if (amountCent <= 0L) {
                continue;
            }
            // 结算键：(月, 酒吧, 主体) 的确定性摘要，保证台账 id / 单号 / source_id 落在 64 字符内且重放得到同一键。
            String key = java.util.UUID.nameUUIDFromBytes((month + "|" + merchantId + "|" + companyId)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString().replace("-", "");
            String settlementId = "bcs_" + month + "_" + key;
            String itemId = "arap_dct_" + month + "_" + key;
            boolean inserted = repository.insertCommissionSettlementIfAbsent(new BarCommissionSettlementRecord(
                    settlementId, month, merchantId, companyId, projectId, amountCent,
                    ((Number) row.get("calculation_count")).intValue(), ((Number) row.get("staff_count")).intValue(),
                    itemId, dueDate, actor, now));
            if (!inserted) {
                continue;
            }
            financeRepository.appendArApItem(new com.complex.platform.finance.ArApItemRecord(
                    itemId, "ARAP-DCT-" + month + "-" + key, "payable", companyId,
                    "dispatch_commission_transfer", settlementId, amountCent, 0L, amountCent, "CNY",
                    dueDate, month, "pending_payment", now, now, merchantId, projectId));
            financeRepository.saveArApItemSource(new com.complex.platform.finance.ArApItemSourceRecord(
                    "arap_src_" + itemId, itemId, "dispatch_commission_transfer", settlementId, amountCent, now));
            created++;
        }
        return created;
    }

    /** 指定日期之后的第一个付账日（三个付账日取现有结算配置；某付账日大于当月天数时以该月月末为准）。 */
    java.time.LocalDate nextInternalPayDayAfter(java.time.LocalDate date) {
        int[] days = repository.internalSettlementPayDays();
        java.time.LocalDate cursor = date.plusDays(1);
        for (int i = 0; i < 70; i++, cursor = cursor.plusDays(1)) {
            int dayOfMonth = cursor.getDayOfMonth();
            int length = cursor.lengthOfMonth();
            for (int day : days) {
                if (dayOfMonth == Math.min(day, length)) {
                    return cursor;
                }
            }
        }
        throw ApiException.conflict("POLICY_NOT_CONFIGURED", "付账日配置无效，不得结算");
    }

    public PageResponse<BarCommissionSettlementRecord> listCommissionSettlements(String month) {
        authorizationService.requirePermission("staff.payroll.manage");
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写月份，格式 YYYY-MM");
        }
        java.util.List<BarCommissionSettlementRecord> items = repository.listCommissionSettlements(month, projectScope());
        auditEventService.recordSuccess("staff.commission_settlement.viewed", "staff.payroll.manage",
                "bar_commission_settlement", month, null, null, Map.of("count", items.size()));
        return page(items);
    }

    private static java.time.LocalDate parseDate(String value, String label) {
        try {
            return java.time.LocalDate.parse(value.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw ApiException.badRequest("VALIDATION_FAILED", label + "格式不正确，应为 YYYY-MM-DD");
        }
    }

    /** P0 租户隔离(V214):主管/后台列表按调用方项目 scope 过滤;平台管理员/无组织全局员工=null 见全量。 */
    private String staffMerchantScope() {
        AccessActor actor = authorizationService.currentActor();
        return authorizationService.isPlatformAdmin(actor) ? null : trimToNull(actor.merchantId());
    }

    private String staffCompanyScope() {
        AccessActor actor = authorizationService.currentActor();
        return authorizationService.isPlatformAdmin(actor) || trimToNull(actor.merchantId()) != null
                ? null : registrations.actorCompany(actor);
    }

    private String projectScope() {
        return staffCompanyScope() == null ? authorizationService.currentProjectScopeQuietly().orElse(null) : null;
    }

    /** 清单 P1(2026-07-03 审计):按项目 scope 守卫写/审批——项目主管只可操作本综合体员工;
     * 平台管理员(空 scope)放行。scope 由 token 派生,不取请求体。 */
    private void requireUserInScope(String userId) {
        AccessActor actor = authorizationService.currentActor();
        StaffProfileRecord target = repository.findProfileByUserId(userId).orElse(null);
        if (target != null && target.companyId() != null) {
            requireProfileInScope(target.id());
            return;
        }
        if (!authorizationService.isPlatformAdmin(actor) && trimToNull(actor.merchantId()) != null) {
            StaffProfileRecord profile = repository.findProfileByUserId(userId)
                    .orElseThrow(() -> ApiException.notFound("STAFF_NOT_FOUND", "员工档案不存在"));
            if (!actor.merchantId().equals(profile.merchantId())) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "该员工不在当前商户范围内");
            }
        }
        authorizationService.currentProjectScopeQuietly().ifPresent(scope -> {
            String userProject = repository.findUserProjectId(userId).orElse(null);
            if (!scope.equals(userProject)) {
                throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN",
                        "该员工不在当前项目范围内");
            }
        });
    }

    /** F批(2026-07-02 拍板):固定工资=员工档案月薪,人事维护。 */
    public void setBaseSalary(com.complex.platform.access.staff.dto.StaffPayrollBaseRequest request) {
        try {
            authorizationService.requirePermission("staff.payroll.manage");
            if (request == null || request.userId() == null || request.userId().isBlank()
                    || request.monthlySalaryCent() == null || request.monthlySalaryCent() < 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "必须填写用户与非负的月薪金额");
            }
            requireUserInScope(request.userId().trim());
            var actor = authorizationService.currentActor();
            repository.upsertBaseSalary(request.userId().trim(), request.monthlySalaryCent(),
                    actor == null ? null : actor.id(), Instant.now());
            auditEventService.recordSuccess("staff.payroll.base_saved", "staff.payroll.manage",
                    "staff_base_salary", request.userId().trim(), null, request.idempotencyKey(),
                    java.util.Map.of("monthly_salary_cent", request.monthlySalaryCent()));
        } catch (RuntimeException exception) {
            // openapi-p0 契约:失败路径落 x-audit-event-failure 事件后再抛(审计闭合)。
            auditEventService.recordFailure("staff.payroll.base_save_failed", "staff.payroll.manage",
                    "staff_base_salary", request == null ? null : request.userId(), null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    /** F批:奖金/处罚手录——必须带事由,审计留痕。
     * P0 真幂等(2026-07-03):幂等键唯一占位+命中重放,重复提交不再重复入账。
     * CG-12（BSM:1487 / ENUM:283「绩效扣分与其他涉及员工的人力操作一样，须经 HR 与该员工直属领导、向上三级复核后生效」）：
     * bonus / penalty 都只发起复核，全部通过后由 {@link StaffApprovalService#afterDecision} 入账。 */
    @org.springframework.transaction.annotation.Transactional
    public com.complex.platform.governance.review.SupervisorReviewDecisionRecord addPayrollAdjustment(
            com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest request) {
        try {
            authorizationService.requirePermission("staff.payroll.manage");
            return idempotencyService.execute("addStaffPayrollAdjustment",
                    request == null ? null : request.idempotencyKey(), request,
                    () -> doAddPayrollAdjustment(request), com.complex.platform.governance.review.SupervisorReviewDecisionRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff.payroll.adjustment_add_failed", "staff.payroll.manage",
                    "staff_payroll_adjustment", request == null ? null : request.userId(), null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    private com.complex.platform.governance.review.SupervisorReviewDecisionRecord doAddPayrollAdjustment(
            com.complex.platform.access.staff.dto.StaffPayrollAdjustmentRequest request) {
        if (request == null || request.userId() == null || request.userId().isBlank()
                || request.month() == null || !request.month().matches("\\d{4}-\\d{2}")
                || request.adjType() == null || !java.util.Set.of("bonus", "penalty").contains(request.adjType())
                || request.amountCent() == null || request.amountCent() <= 0
                || request.reason() == null || request.reason().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "必须填写用户、月份（YYYY-MM）、调整类型（奖励/扣罚）、大于 0 的金额与理由");
        }
        requireUserInScope(request.userId().trim());
        var review = staffApprovals.submitPayrollAdjustment(request);
        auditEventService.recordSuccess("staff.payroll.adjustment_requested", "staff.payroll.manage",
                "staff_payroll_adjustment", review.objectId(), null, request.idempotencyKey(),
                java.util.Map.of("user_id", request.userId().trim(), "month", request.month(),
                        "adj_type", request.adjType(), "amount_cent", request.amountCent(),
                        "reason", request.reason().trim(), "review_id", review.reviewId()));
        return review;
    }

    /**
     * CG-12（BSM:1487「员工可对扣分提出申诉，申诉同样报至 HR 与直属领导向上三级裁定，过程与结论均留痕」）：
     * 只能由被扣罚员工本人对已生效的扣罚（penalty）提出，事由必填；同一扣罚只能有一笔裁定中的申诉，已撤销的不得再诉。
     */
    @org.springframework.transaction.annotation.Transactional
    public StaffAdjustmentAppealRecord submitAdjustmentAppeal(String adjustmentId,
            com.complex.platform.access.staff.dto.StaffAdjustmentAppealRequest request) {
        try {
            authorizationService.requirePermission("staff.payroll.view_mine");
            var adjustment = repository.findAdjustment(requireText(adjustmentId, "adjustment_id"))
                    .orElseThrow(() -> ApiException.notFound("ADJUSTMENT_NOT_FOUND", "奖惩记录不存在"));
            var actor = authorizationService.currentActor();
            if (actor == null || !actor.id().equals(adjustment.userId())) {
                throw ApiException.forbidden("APPEAL_SELF_ONLY", "只能由被扣罚员工本人提出申诉");
            }
            if (!"penalty".equals(adjustment.adjType())) {
                throw ApiException.badRequest("APPEAL_ONLY_FOR_PENALTY", "只能对扣分 / 处罚扣款提出申诉");
            }
            if (request == null || request.reason() == null || request.reason().isBlank()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "申诉事由必填");
            }
            if (repository.hasAppealInStatus(adjustmentId, "pending")) {
                throw ApiException.conflict("APPEAL_PENDING", "该扣罚已有申诉在裁定中");
            }
            if (repository.hasAppealInStatus(adjustmentId, "revoked")) {
                throw ApiException.conflict("APPEAL_ALREADY_REVOKED", "该扣罚已被撤销，无需再申诉");
            }
            return idempotencyService.execute("staffAdjustmentAppeal:" + adjustmentId, request.idempotencyKey(), request,
                    () -> staffApprovals.submitAdjustmentAppeal(adjustment, request, actor.id()), StaffAdjustmentAppealRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff.payroll.adjustment_appeal_submit_failed", "staff.payroll.view_mine",
                    "staff_payroll_adjustment", adjustmentId, null, request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    /** CG-12：申诉清单——本人看自己的（staff.payroll.view_mine）；人事按员工查看须 staff.payroll.manage 且员工在范围内。 */
    public PageResponse<StaffAdjustmentAppealRecord> listAdjustmentAppeals(String userId) {
        var actor = authorizationService.currentActor();
        String target = userId == null || userId.isBlank() ? (actor == null ? null : actor.id()) : userId.trim();
        if (actor != null && actor.id().equals(target)) {
            authorizationService.requirePermission("staff.payroll.view_mine");
        } else {
            authorizationService.requirePermission("staff.payroll.manage");
            requireUserInScope(requireText(target, "user_id"));
        }
        List<StaffAdjustmentAppealRecord> items = repository.listAppeals(target);
        auditEventService.recordSuccess("staff.payroll.adjustment_appeals_viewed", "staff.payroll.view_mine",
                "staff_payroll_adjustment_appeal", target, null, null, Map.of("count", items.size()));
        return new PageResponse<>(items, 1, Math.max(items.size(), 1), items.size());
    }

    /** F批:提成入台账(手录/系统写入口;服务点单等业务后续自动写)。
     * P0 真幂等:重复提交不再重复入提成台账。 */
    public void addCommission(com.complex.platform.access.staff.dto.StaffCommissionRequest request) {
        try {
            authorizationService.requirePermission("staff.payroll.manage");
            idempotencyService.execute("addStaffCommission",
                    request == null ? null : request.idempotencyKey(), request,
                    () -> doAddCommission(request), String.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff.payroll.commission_add_failed", "staff.payroll.manage",
                    "staff_commission_ledger", request == null ? null : request.userId(), null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    private String doAddCommission(com.complex.platform.access.staff.dto.StaffCommissionRequest request) {
        if (request == null || request.userId() == null || request.userId().isBlank()
                || request.month() == null || !request.month().matches("\\d{4}-\\d{2}")
                || request.amountCent() == null || request.amountCent() < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写用户、月份与非负金额");
        }
        requireUserInScope(request.userId().trim());
        var actor = authorizationService.currentActor();
        String id = "cml_" + UUID.randomUUID();
        repository.insertCommission(id, request.userId().trim(), request.month(),
                request.sourceType() == null ? "manual" : request.sourceType(), request.sourceId(),
                request.amountCent(), request.note(), Instant.now());
        auditEventService.recordSuccess("staff.payroll.commission_added", "staff.payroll.manage",
                "staff_commission_ledger", id, null, request.idempotencyKey(),
                java.util.Map.of("user_id", request.userId().trim(), "month", request.month(),
                        "amount_cent", request.amountCent()));
        return id;
    }

    /** F批(2026-07-02 拍板):月结——重算定格全员薪资单(可重复执行=覆盖,补录奖惩后重结);
     * total = 固定 + 提成(台账+酒吧提成) + 奖金 - 处罚;员工只看已定格月份。 */
    public java.util.Map<String, Object> closePayroll(com.complex.platform.access.staff.dto.StaffPayrollCloseRequest request) {
        try {
            authorizationService.requirePermission("staff.payroll.manage");
            if (request == null || request.month() == null || !request.month().matches("\\d{4}-\\d{2}")) {
                throw ApiException.badRequest("VALIDATION_FAILED", "必须填写月份，格式 YYYY-MM");
            }
            var actor = authorizationService.currentActor();
            int count = repository.closePayrollMonth(request.month(), actor == null ? null : actor.id(), Instant.now(), projectScope(), staffMerchantScope(), staffCompanyScope());
            auditEventService.recordSuccess("staff.payroll.month_closed", "staff.payroll.manage",
                    "staff_payroll_statement", request.month(), null, request.idempotencyKey(),
                    java.util.Map.of("month", request.month(), "statements", count));
            return java.util.Map.of("month", request.month(), "statements", count);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff.payroll.month_close_failed", "staff.payroll.manage",
                    "staff_payroll_statement", request == null ? null : request.month(), null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    /** 资金安全①(fix/payroll-safety):发放某员工某月已定格薪资单——落发放台账 + 状态置 paid,
     * 此后重结不覆盖(套现被封)。幂等:重复发放命中重放,不重复落台账。 */
    public java.util.Map<String, Object> markPayrollPaid(
            com.complex.platform.access.staff.dto.StaffPayrollPayRequest request) {
        try {
            authorizationService.requirePermission("staff.payroll.manage");
            if (request == null || request.month() == null || !request.month().matches("\\d{4}-\\d{2}")
                    || request.userId() == null || request.userId().isBlank()) {
                throw ApiException.badRequest("VALIDATION_FAILED", "必须填写月份（YYYY-MM）与用户");
            }
            requireUserInScope(request.userId().trim());
            return idempotencyService.execute("markStaffPayrollPaid",
                    request.idempotencyKey(), request,
                    () -> doMarkPayrollPaid(request), java.util.Map.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff.payroll.pay_failed", "staff.payroll.manage",
                    "staff_payroll_payout", request == null ? null : request.userId(), null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> doMarkPayrollPaid(
            com.complex.platform.access.staff.dto.StaffPayrollPayRequest request) {
        var actor = authorizationService.currentActor();
        int paid = repository.markPayrollPaid("payout_" + UUID.randomUUID(), request.userId().trim(),
                request.month(), trimToNull(request.payMethod()), trimToNull(request.payRef()),
                actor == null ? null : actor.id(), Instant.now());
        if (paid == 0) {
            throw ApiException.conflict("PAYROLL_NOT_PAYABLE",
                    "该用户该月没有可发放的已结账薪资单（已发放或尚未结账）");
        }
        auditEventService.recordSuccess("staff.payroll.paid", "staff.payroll.manage",
                "staff_payroll_payout", request.userId().trim(), null, request.idempotencyKey(),
                java.util.Map.of("user_id", request.userId().trim(), "month", request.month()));
        return java.util.Map.of("user_id", request.userId().trim(), "month", request.month(), "paid", paid);
    }

    /** 个税税表(V217):后台查劳务报酬预扣税表全档位(读复用 payroll.manage)。 */
    public java.util.List<PayrollTaxBracketRecord> listTaxBrackets(String category) {
        authorizationService.requirePermission("staff.payroll.manage");
        return repository.listTaxBrackets(category == null || category.isBlank() ? null : category.trim());
    }

    /** 个税税表(V217):维护一档(高敏,决定代扣个税口径);税法参数变更改配置不改代码;审计留痕。 */
    public PayrollTaxBracketRecord upsertTaxBracket(
            com.complex.platform.access.staff.dto.PayrollTaxBracketRequest request) {
        authorizationService.requirePermission("staff.tax_bracket.manage");
        if (request == null || request.id() == null || request.id().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "id is required");
        }
        String category = request.category() == null || request.category().isBlank()
                ? "labor_remuneration" : request.category().trim();
        if (request.seq() == null || request.incomeFloorCent() == null || request.incomeBaseBps() == null
                || request.rateBps() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写序号、收入下限、收入基数比例与费率");
        }
        if (request.incomeFloorCent() < 0
                || request.incomeBaseBps() <= 0 || request.incomeBaseBps() > 10000
                || request.rateBps() < 0 || request.rateBps() > 10000) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "收入基数比例须在 (0,10000] 基点、费率须在 [0,10000] 基点、收入下限不能为负");
        }
        if (request.incomeCeilingCent() != null && request.incomeCeilingCent() < request.incomeFloorCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "收入上限不能低于收入下限");
        }
        Instant now = Instant.now();
        var actor = authorizationService.currentActor();
        PayrollTaxBracketRecord saved = repository.upsertTaxBracket(new PayrollTaxBracketRecord(
                request.id().trim(), category, request.seq(), request.incomeFloorCent(),
                request.incomeCeilingCent(), request.incomeBaseBps(),
                request.flatDeductionCent() == null ? 0L : request.flatDeductionCent(), request.rateBps(),
                request.quickDeductionCent() == null ? 0L : request.quickDeductionCent(),
                request.effectiveFrom(), actor == null ? null : actor.id(), now));
        auditEventService.recordSuccess("staff.payroll.tax_bracket_changed", "staff.tax_bracket.manage",
                "payroll_tax_brackets", saved.id(), null, request.idempotencyKey(),
                java.util.Map.of("category", category, "seq", request.seq(),
                        "rate_bps", request.rateBps(), "income_floor_cent", request.incomeFloorCent()));
        return saved;
    }

    /** F批:后台按月查全员薪资单。 */
    public PageResponse<StaffPayrollStatementRecord> listPayroll(String month) {
        return listPayroll(month, 1, 1000);
    }

    public PageResponse<StaffPayrollStatementRecord> listPayroll(String month, int page, int size) {
        authorizationService.requirePermission("staff.payroll.manage");
        return repository.listPayrollStatementsPaged(month, null, projectScope(), staffMerchantScope(), staffCompanyScope(), page, size);
    }

    /** F批(拍板:月结后才可见):员工查自己薪资——只返回已定格薪资单,未结月自然为空。 */
    public PageResponse<StaffPayrollStatementRecord> listMyPayroll(String month) {
        authorizationService.requirePermission("staff.payroll.view_mine");
        var actor = authorizationService.currentActor();
        // 员工看自己:按 user 自限定,不再叠 project 谓词(员工无档案项目时也能看自己的单)。
        return page(repository.listPayrollStatements(month, actor.id(), null));
    }

    /** F批:员工提交请假(四类:事假/病假/调休/年假;主管审)。
     * P0 真幂等:重复提交重放首次结果,不再重复建请假单。 */
    public StaffLeaveRequestRecord submitLeave(com.complex.platform.access.staff.dto.StaffLeaveRequest request) {
        try {
            authorizationService.requirePermission("staff.leave.submit");
            return idempotencyService.execute("submitStaffLeave",
                    request == null ? null : request.idempotencyKey(), request,
                    () -> doSubmitLeave(request), StaffLeaveRequestRecord.class);
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff.leave.submit_failed", "staff.leave.submit",
                    "staff_leave_request", null, null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    private StaffLeaveRequestRecord doSubmitLeave(com.complex.platform.access.staff.dto.StaffLeaveRequest request) {
        if (request == null || request.leaveType() == null
                || !java.util.Set.of("personal", "sick", "comp", "annual").contains(request.leaveType())
                || request.startDate() == null || request.endDate() == null
                || request.endDate().isBefore(request.startDate())
                || request.reason() == null || request.reason().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "必须填写请假类型（事假/病假/调休/年假）、开始不晚于结束的日期与理由");
        }
        var actor = authorizationService.currentActor();
        if (repository.hasActiveLeaveOverlap(actor.id(), request.startDate(), request.endDate())) {
            throw ApiException.badRequest("LEAVE_OVERLAP",
                    "该日期范围内已有待审批或已批准的请假记录");
        }
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
        return page(repository.listLeaves(null, actor.id(), null));
    }

    /** F批:主管审请假(复用排班主管权限);通过区间考勤月视图记「请假」。 */
    public StaffLeaveRequestRecord reviewLeave(String id,
            com.complex.platform.access.staff.dto.StaffLeaveReviewRequest request) {
        try {
            authorizationService.requirePermission("staff.shift.manage");
            if (request == null || request.decision() == null
                    || !java.util.Set.of("approve", "reject").contains(request.decision())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过或驳回");
            }
            StaffLeaveRequestRecord current = repository.findLeaveById(id)
                    .orElseThrow(() -> ApiException.notFound("LEAVE_NOT_FOUND", "请假申请不存在或已被删除"));
            requireUserInScope(current.userId());
            if (!current.status().equals("pending")) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请假申请已审批，不能重复处理");
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
        } catch (RuntimeException exception) {
            auditEventService.recordFailure("staff.leave.review_failed", "staff.shift.manage",
                    "staff_leave_request", id, null,
                    request == null ? null : request.idempotencyKey(), codeOf(exception));
            throw exception;
        }
    }

    /** F批:主管/后台查请假(按状态)。 */
    public PageResponse<StaffLeaveRequestRecord> listLeaves(String status) {
        return listLeaves(status, 1, 500);
    }

    public PageResponse<StaffLeaveRequestRecord> listLeaves(String status, int page, int size) {
        authorizationService.requirePermission("staff.shift.manage");
        return repository.listLeavesPaged(status, null, projectScope(), staffMerchantScope(), staffCompanyScope(), page, size);
    }

    /** F批(2026-07-02 拍板六态口径):考勤月视图——出勤/缺勤(有排班无上班卡)/迟到/早退/
     * 未打下班卡/请假(approved 覆盖);无排班=休;同日多问题并列。 */
    public java.util.List<StaffAttendanceDayRecord> myAttendanceMonthView(String month) {
        authorizationService.requirePermission("staff.shift.view");
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须填写月份，格式 YYYY-MM");
        }
        var actor = authorizationService.currentActor();
        java.time.YearMonth yearMonth = java.time.YearMonth.parse(month);
        java.time.LocalDate first = yearMonth.atDay(1);
        java.time.LocalDate last = yearMonth.atEndOfMonth();
        List<StaffShiftRecord> shifts = repository.listShiftsEndingBetween(actor.id(),
                first.atStartOfDay(LocalZone.ID).toInstant(),
                last.plusDays(1).atStartOfDay(LocalZone.ID).toInstant());
        Map<java.time.LocalDate, List<StaffShiftRecord>> shiftsByDay = shifts.stream().collect(
                java.util.stream.Collectors.groupingBy(
                        shift -> java.time.LocalDate.ofInstant(shift.endAt(), LocalZone.ID)));
        java.util.Set<java.time.LocalDate> leaveDays = repository.approvedLeaveDates(actor.id(), first, last);
        List<StaffAttendanceRecord> punches = repository.listAttendanceForShifts(actor.id(),
                shifts.stream().map(StaffShiftRecord::id).toList());
        List<StaffAttendanceDayRecord> out = new java.util.ArrayList<>();
        for (java.time.LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            List<StaffShiftRecord> dayShifts = shiftsByDay.getOrDefault(day, List.of());
            Instant start = dayShifts.stream().map(StaffShiftRecord::startAt).min(Instant::compareTo).orElse(null);
            Instant end = dayShifts.stream().map(StaffShiftRecord::endAt).max(Instant::compareTo).orElse(null);
            if (leaveDays.contains(day)) {
                out.add(new StaffAttendanceDayRecord(day, List.of("leave"), start, end));
                continue;
            }
            if (dayShifts.isEmpty()) {
                out.add(new StaffAttendanceDayRecord(day, List.of("rest"), null, null));
                continue;
            }
            java.util.Set<String> statuses = new java.util.LinkedHashSet<>();
            for (StaffShiftRecord shift : dayShifts) {
                List<StaffAttendanceRecord> ownPunches = punches.stream()
                        .filter(p -> shift.id().equals(p.shiftId())).toList();
                StaffAttendanceRecord checkIn = ownPunches.stream()
                        .filter(p -> "check_in".equals(p.punchType())).findFirst().orElse(null);
                StaffAttendanceRecord checkOut = ownPunches.stream()
                        .filter(p -> "check_out".equals(p.punchType())).reduce((a, b) -> b).orElse(null);
                if (checkIn == null) {
                    statuses.add("absent");
                } else {
                    if ("late".equals(checkIn.judgement())) statuses.add("late");
                    if (checkOut == null) statuses.add("missing_checkout");
                    else if ("early_leave".equals(checkOut.judgement())) statuses.add("early_leave");
                }
            }
            if (statuses.isEmpty()) statuses.add("present");
            out.add(new StaffAttendanceDayRecord(day, List.copyOf(statuses), start, end));
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
