package com.complex.platform.amusement;

import com.complex.platform.support.InMemoryAuditEventRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.RiskAlertService;
import com.complex.platform.evidence.dto.RiskAlertReportRequest;
import org.mockito.ArgumentCaptor;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AmusementServiceTests {

    private InMemoryAmusementRepository amusementRepository;
    private InMemoryAuditEventRepository auditEventRepository;
    private InMemoryAmusementEntitlementGateway entitlementGateway;
    private InMemorySafetyRepository safetyRepository;
    private AmusementService service;
    private AuthorizationService authorizationService;
    // V341（R-12）：软删审批闸测试需要按权限码打桩 permissionExists，故把 mock 留成字段。
    private AccessRepository accessRepository;
    private RiskAlertService riskAlertService;

    @BeforeEach
    void setUp() {
        amusementRepository = new InMemoryAmusementRepository();
        // 检修单位须为供应商主表真实记录：默认登记一个 active 供应商供既有检修用例使用。
        amusementRepository.registerMaintenanceVendor("supplier_maint_1", "active");
        auditEventRepository = new InMemoryAuditEventRepository();
        entitlementGateway = new InMemoryAmusementEntitlementGateway();
        safetyRepository = new InMemorySafetyRepository();
        accessRepository = mock(AccessRepository.class);
        authorizationService = new AuthorizationService(accessRepository);
        riskAlertService = mock(RiskAlertService.class);
        TestSecurityActors.asPlatformAdmin();
        service = new AmusementService(
                amusementRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                authorizationService,
                entitlementGateway,
                safetyRepository,
                riskAlertService
        );
    }

    /** 注册一张有效的游乐权益（active、无有效期限制、剩余 times 次、type 默认 amusement_single）。 */
    private void registerAmusementEntitlement(String credentialPayload, int totalTimes) {
        registerAmusementEntitlement(credentialPayload, totalTimes, 1);
    }

    private void registerAmusementEntitlement(String credentialPayload, int totalTimes, int numPeople) {
        entitlementGateway.register(credentialPayload, "amusement_single", "active", null, null, totalTimes, numPeople);
    }

    @AfterEach
    void tearDown() {
        TestSecurityActors.clear();
    }

    @Test
    void createSessionRejectsCrossMerchantProject() {
        TestSecurityActors.asMerchantUser("merchant_a");
        AmusementSafetyTemplateRecord template = createTemplate("xmerchant");
        AmusementProjectRecord project = createProject("xmerchant", template.id(), "open", 2);

        TestSecurityActors.asMerchantUser("merchant_b");
        assertThatThrownBy(() -> createSession("xmerchant", project.id(), 2, "open"))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void createManualOverrideRejectsCrossMerchantProject() {
        TestSecurityActors.asMerchantUser("merchant_a");
        AmusementSafetyTemplateRecord template = createTemplate("xmerchant-override");
        AmusementProjectRecord project = createProject("xmerchant-override", template.id(), "open", 2);

        TestSecurityActors.asMerchantUser("merchant_b");
        assertThatThrownBy(() -> service.createManualOverride(new AmusementManualOverrideRequest(
                        "override-xmerchant",
                        "override",
                        List.of("att_1"),
                        project.id(),
                        null,
                        "allow",
                        "supervisor approved")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 处理清单 #8：amusement_projects 空间指派/回填（over_space 依赖 space_id）
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void assignProjectSpaceSetsSpaceIdAndIsReadableForBacklog() {
        TestSecurityActors.asMerchantUser("merchant_a");
        AmusementSafetyTemplateRecord template = createTemplate("space-assign");
        AmusementProjectRecord project = createProject("space-assign", template.id(), "open", 2);
        amusementRepository.registerSpace("space_a", "merchant_a");

        // 回填前：出现在「未回填空间」清单。
        assertThat(service.listProjectsWithoutSpace().items())
                .extracting(AmusementProjectSpaceBacklogRecord::id)
                .contains(project.id());

        AmusementProjectSpaceAssignmentRecord assigned = service.assignProjectSpace(project.id(),
                new AmusementProjectSpaceAssignmentRequest("assign-key", "space_a", "运营回填", List.of()));
        assertThat(assigned.spaceId()).isEqualTo("space_a");
        // 回读 amusement_projects.space_id 命中目标空间。
        assertThat(amusementRepository.projectSpaceId(project.id())).isEqualTo("space_a");
        // 回填后：不再出现在「未回填空间」清单。
        assertThat(service.listProjectsWithoutSpace().items())
                .extracting(AmusementProjectSpaceBacklogRecord::id)
                .doesNotContain(project.id());
    }

    @Test
    void assignProjectSpaceRejectsCrossMerchantSpaceWithForbiddenError() {
        TestSecurityActors.asMerchantUser("merchant_a");
        AmusementSafetyTemplateRecord template = createTemplate("space-cross");
        AmusementProjectRecord project = createProject("space-cross", template.id(), "open", 2);
        amusementRepository.registerSpace("space_b", "merchant_b");

        assertThatThrownBy(() -> service.assignProjectSpace(project.id(),
                new AmusementProjectSpaceAssignmentRequest("assign-cross", "space_b", null, List.of())))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void assignProjectSpaceRejectsMissingSpaceWithNotFoundError() {
        TestSecurityActors.asMerchantUser("merchant_a");
        AmusementSafetyTemplateRecord template = createTemplate("space-missing");
        AmusementProjectRecord project = createProject("space-missing", template.id(), "open", 2);

        assertThatThrownBy(() -> service.assignProjectSpace(project.id(),
                new AmusementProjectSpaceAssignmentRequest("assign-missing", "space_ghost", null, List.of())))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SPACE_NOT_FOUND");
    }

    @Test
    void getOpenEligibilityRejectsCrossMerchantProject() {
        TestSecurityActors.asMerchantUser("merchant_a");
        AmusementSafetyTemplateRecord template = createTemplate("xmerchant-eligibility");
        AmusementProjectRecord project = createProject("xmerchant-eligibility", template.id(), "open", 2);

        TestSecurityActors.asMerchantUser("merchant_b");
        assertThatThrownBy(() -> service.getOpenEligibility(project.id()))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void safetyTemplateRejectsEmptyItems() {
        assertThatThrownBy(() -> service.createSafetyTemplate(new AmusementSafetyTemplateRequest(
                        "template-empty",
                        "empty",
                        List.of(),
                        "Daily Opening",
                        List.of(),
                        LocalDate.parse("2026-06-14")
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        assertThat(amusementRepository.safetyTemplates).isEmpty();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .containsExactly("amusement.safety_template.change_failed");
    }

    @Test
    void projectSessionCreateAndListWriteOpenApiAuditEvents() {
        AmusementSafetyTemplateRecord template = createTemplate("project-session-template");
        AmusementProjectRecord project = createProject("project-session", template.id(), "open", 2);
        AmusementSessionRecord session = createSession("project-session", project.id(), 2, "open");

        assertThat(service.listProjects().items())
                .extracting(AmusementProjectRecord::id, AmusementProjectRecord::projectName)
                .contains(tuple(project.id(), "Roller Coaster project-session"));
        assertThat(service.listSessions().items())
                .extracting(AmusementSessionRecord::id, AmusementSessionRecord::projectId, AmusementSessionRecord::capacity)
                .contains(tuple(session.id(), project.id(), 2));
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "amusement.safety_template.changed",
                        "amusement.session.changed",
                        "amusement.session.viewed"
                );
    }

    @Test
    void failedInspectionBlocksOpenEligibilityUntilRectifyRecheckAndApprovalComplete() {
        AmusementProjectRecord project = projectWithTemplate("blocked");
        AmusementSafetyInspectionRecord failed = service.createSafetyInspection(new AmusementSafetyInspectionRequest(
                "inspection-blocked",
                "inspect",
                List.of("att_fail"),
                project.id(),
                null,
                project.safetyTemplateId(),
                List.of(Map.of("name", "restraint", "result", "fail")),
                "fail",
                Instant.parse("2026-06-14T08:00:00Z"),
                "inspector_blocked",
                "restraint sensor abnormal"
        ));

        service.submitSafetyInspection(failed.id(), action("submit-blocked", "submit", "submit failed inspection"));
        AmusementOpenEligibilityResponse blocked = service.getOpenEligibility(project.id());

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.blockedReasons()).contains("SAFETY_REOPEN_PENDING");

        service.rectifySafetyInspection(failed.id(), action("rectify-blocked", "submitted", "fixed restraint sensor"));
        service.recheckSafetyInspection(failed.id(), action("recheck-blocked", "pass", "passed recheck"));
        AmusementSafetyInspectionRecord approved = service.approveReopen(
                failed.id(),
                new StatusChangeRequest("approve-blocked", "approve", "supervisor approved", List.of("att_approve"))
        );

        assertThat(approved.reopenStatus()).isEqualTo("approved");
        assertThat(service.getOpenEligibility(project.id()).allowed()).isTrue();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "amusement.safety_inspection.blocked",
                        "amusement.open_eligibility.blocked",
                        "amusement.safety_inspection.rectification_submitted",
                        "amusement.safety_inspection.rechecked",
                        "amusement.safety_inspection.reopened",
                        "amusement.open_eligibility.viewed"
                );
    }

    @Test
    void passInspectionSubmitAllowsOpenEligibility() {
        AmusementProjectRecord project = projectWithTemplate("pass");
        AmusementSafetyInspectionRecord inspection = service.createSafetyInspection(new AmusementSafetyInspectionRequest(
                "inspection-pass",
                "inspect",
                List.of("att_pass"),
                project.id(),
                null,
                project.safetyTemplateId(),
                List.of(Map.of("name", "restraint", "result", "pass")),
                "pass",
                Instant.parse("2026-06-14T08:00:00Z"),
                "inspector_pass",
                null
        ));

        // 2b 防伪造 pass 专项：pass 无证据被拒；item result=fail 与 overall=pass 矛盾被拒。
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createSafetyInspection(
                new AmusementSafetyInspectionRequest("insp-noev", "inspect", List.of(), project.id(),
                        null, project.safetyTemplateId(), List.of(Map.of("name", "restraint", "result", "pass")),
                        "pass", Instant.parse("2026-06-14T08:00:00Z"), "inspector_x", null)))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.createSafetyInspection(
                new AmusementSafetyInspectionRequest("insp-conflict", "inspect", List.of("att_x"), project.id(),
                        null, project.safetyTemplateId(), List.of(Map.of("name", "restraint", "result", "fail")),
                        "pass", Instant.parse("2026-06-14T08:00:00Z"), "inspector_x", null)))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class);

        AmusementSafetyInspectionRecord submitted = service.submitSafetyInspection(
                inspection.id(),
                action("submit-pass", "submit", "submit pass")
        );

        assertThat(submitted)
                .extracting(AmusementSafetyInspectionRecord::inspectionStatus, AmusementSafetyInspectionRecord::reopenStatus)
                .containsExactly("submitted", "not_required");
        assertThat(service.getOpenEligibility(project.id()).allowed()).isTrue();
    }

    @Test
    void approveReopenRejectsBeforeRecheckPass() {
        AmusementProjectRecord project = projectWithTemplate("approve-reject");
        AmusementSafetyInspectionRecord inspection = service.createSafetyInspection(new AmusementSafetyInspectionRequest(
                "inspection-approve-reject",
                "inspect",
                List.of(),
                project.id(),
                null,
                project.safetyTemplateId(),
                List.of(Map.of("name", "restraint", "result", "fail")),
                "fail",
                Instant.parse("2026-06-14T08:00:00Z"),
                "inspector_approve_reject",
                "restraint abnormal"
        ));
        service.submitSafetyInspection(inspection.id(), action("submit-approve-reject", "submit", "submit fail"));
        service.rectifySafetyInspection(inspection.id(), action("rectify-approve-reject", "submitted", "fixed"));

        assertThatThrownBy(() -> service.approveReopen(
                        inspection.id(),
                        new StatusChangeRequest("approve-too-early", "approve", "too early", List.of())
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("RECHECK_PASS_REQUIRED");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.safety_inspection.reopen_rejected");
    }

    @Test
    void approveReopenRestoresProjectOpenStatusAndPersistsStructuredInspector() {
        // 清单#18 断链修复：失败巡检整改/复检通过后，主管"批准恢复营业"应联动 amusement_projects.open_status
        // 从停业态（此处 closed）翻转为 open；并验证巡检 inspector_id 等结构化字段落库（不再只塞 operator_note）。
        AmusementProjectRecord project = projectWithTemplate("reopen-link");
        // 将项目置于停业态 closed（模拟失败巡检导致的停业）。
        amusementRepository.saveProject(new AmusementProjectRecord(
                project.id(),
                project.projectName(),
                project.capacity(),
                "closed",
                project.safetyTemplateId(),
                project.eligibilityRule(),
                project.locationPointId(),
                project.createdAt(),
                project.updatedAt(),
                project.merchantId()
        ));

        AmusementSafetyInspectionRecord failed = service.createSafetyInspection(new AmusementSafetyInspectionRequest(
                "inspection-reopen-link",
                "inspect",
                List.of(),
                project.id(),
                null,
                project.safetyTemplateId(),
                List.of(Map.of("name", "restraint", "result", "fail")),
                "fail",
                Instant.parse("2026-06-14T08:00:00Z"),
                "inspector_reopen_link",
                "restraint sensor abnormal"
        ));
        // 创建即结构化落库：巡检人、结构化结论、异常标记/说明。
        assertThat(failed)
                .extracting(
                        AmusementSafetyInspectionRecord::inspectorId,
                        AmusementSafetyInspectionRecord::inspectionResult,
                        AmusementSafetyInspectionRecord::abnormalFound,
                        AmusementSafetyInspectionRecord::abnormalNote)
                .containsExactly("inspector_reopen_link", "fail", true, "restraint sensor abnormal");

        service.submitSafetyInspection(failed.id(), action("submit-reopen-link", "submit", "submit fail"));
        service.rectifySafetyInspection(failed.id(), action("rectify-reopen-link", "submitted", "fixed"));
        service.recheckSafetyInspection(failed.id(), action("recheck-reopen-link", "pass", "passed recheck"));
        AmusementSafetyInspectionRecord approved = service.approveReopen(
                failed.id(),
                new StatusChangeRequest("approve-reopen-link", "approve", "supervisor approved", List.of())
        );

        assertThat(approved.reopenStatus()).isEqualTo("approved");
        // 复检 pass 后结构化结论同步刷新为 pass、异常消除；巡检人保留。
        assertThat(approved)
                .extracting(
                        AmusementSafetyInspectionRecord::inspectorId,
                        AmusementSafetyInspectionRecord::inspectionResult,
                        AmusementSafetyInspectionRecord::abnormalFound)
                .containsExactly("inspector_reopen_link", "pass", false);
        // 断链修复：批准恢复营业联动 open_status closed → open。
        assertThat(amusementRepository.findProjectById(project.id()))
                .get().extracting(AmusementProjectRecord::openStatus).isEqualTo("open");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.safety_inspection.reopened");
    }

    @Test
    void approveReopenDoesNotOverridePausedRunningProject() {
        // 取舍验证：项目已处运营态（paused 人工暂停）时，批准恢复营业不强翻 open_status（避免与暂停线冲突）。
        AmusementProjectRecord project = projectWithTemplate("reopen-paused");
        amusementRepository.saveProject(new AmusementProjectRecord(
                project.id(),
                project.projectName(),
                project.capacity(),
                "paused",
                project.safetyTemplateId(),
                project.eligibilityRule(),
                project.locationPointId(),
                project.createdAt(),
                project.updatedAt(),
                project.merchantId()
        ));

        AmusementSafetyInspectionRecord failed = service.createSafetyInspection(new AmusementSafetyInspectionRequest(
                "inspection-reopen-paused",
                "inspect",
                List.of(),
                project.id(),
                null,
                project.safetyTemplateId(),
                List.of(Map.of("name", "restraint", "result", "fail")),
                "fail",
                Instant.parse("2026-06-14T08:00:00Z"),
                "inspector_reopen_paused",
                "abnormal"
        ));
        service.submitSafetyInspection(failed.id(), action("submit-reopen-paused", "submit", "submit fail"));
        service.rectifySafetyInspection(failed.id(), action("rectify-reopen-paused", "submitted", "fixed"));
        service.recheckSafetyInspection(failed.id(), action("recheck-reopen-paused", "pass", "passed recheck"));
        service.approveReopen(
                failed.id(),
                new StatusChangeRequest("approve-reopen-paused", "approve", "supervisor approved", List.of())
        );

        // paused 属运营态，不被巡检审批强翻为 open。
        assertThat(amusementRepository.findProjectById(project.id()))
                .get().extracting(AmusementProjectRecord::openStatus).isEqualTo("paused");
    }

    @Test
    void verificationRejectsClosedProjectBlockedSafetyFullSessionAndBlankCredential() {
        AmusementProjectRecord closedProject = projectWithTemplate("closed");
        amusementRepository.saveProject(new AmusementProjectRecord(
                closedProject.id(),
                closedProject.projectName(),
                closedProject.capacity(),
                "closed",
                closedProject.safetyTemplateId(),
                closedProject.eligibilityRule(),
                closedProject.locationPointId(),
                closedProject.createdAt(),
                Instant.parse("2026-06-14T09:00:00Z"),
                null
        ));
        AmusementSessionRecord closedSession = createSession("closed-session", closedProject.id(), 1, "open");

        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-closed",
                        closedProject.id(),
                        closedSession.id(),
                        "credential-1"
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("PROJECT_NOT_OPEN");

        AmusementProjectRecord blockedProject = projectWithTemplate("safety-blocked");
        AmusementSessionRecord blockedSession = createSession("blocked-session", blockedProject.id(), 1, "open");
        AmusementSafetyInspectionRecord failed = service.createSafetyInspection(new AmusementSafetyInspectionRequest(
                "inspection-safety-blocked",
                "inspect",
                List.of(),
                blockedProject.id(),
                null,
                blockedProject.safetyTemplateId(),
                List.of(Map.of("name", "restraint", "result", "fail")),
                "fail",
                Instant.parse("2026-06-14T08:00:00Z"),
                "inspector_safety_blocked",
                "restraint abnormal"
        ));
        service.submitSafetyInspection(failed.id(), action("submit-safety-blocked", "submit", "submit fail"));

        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-safety-blocked",
                        blockedProject.id(),
                        blockedSession.id(),
                        "credential-2"
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SAFETY_BLOCKED");

        AmusementProjectRecord fullProject = projectWithTemplate("full");
        submitPassingInspection(fullProject, "full");
        AmusementSessionRecord fullSession = createSession("full-session", fullProject.id(), 1, "open");
        registerAmusementEntitlement("credential-3", 1);
        registerAmusementEntitlement("credential-4", 1);
        service.createVerification(verification("verify-full-first", fullProject.id(), fullSession.id(), "credential-3"));

        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-full-second",
                        fullProject.id(),
                        fullSession.id(),
                        "credential-4"
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SESSION_CAPACITY_EXHAUSTED");

        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-blank",
                        fullProject.id(),
                        fullSession.id(),
                        " "
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.verification_failed");
    }

    @Test
    void generateDueInspectionTasksUpsertsPendingDeduplicatesAndMarksOverdue() {
        // T3.1：激活 V143 巡检到期工单表。项目有一台在役设备、最近 PASS 在 48h 前、周期 24h
        // → 到期点 = now-24h（已过）。生成应产 1 张待办并翻 overdue；重复调度去重仍 1 张。
        AmusementProjectRecord project = projectWithTemplate("t31-inspection-task");
        Instant now = Instant.parse("2026-06-23T12:00:00Z");
        Instant lastPass = now.minus(java.time.Duration.ofHours(48));
        amusementRepository.saveDevice(new AmusementDeviceRecord(
                "dev_t31", project.id(), "DEV-T31", "巡检Job测试设备", "active",
                24, null, lastPass, null, lastPass, lastPass));

        service.generateDueInspectionTasks(now);
        service.generateDueInspectionTasks(now); // 重复调度

        List<AmusementInspectionTaskRecord> tasks = amusementRepository.allInspectionTasks();
        assertThat(tasks).hasSize(1); // 去重：同到期点只 1 张
        AmusementInspectionTaskRecord task = tasks.get(0);
        assertThat(task.projectId()).isEqualTo(project.id());
        assertThat(task.deviceId()).isEqualTo("dev_t31");
        assertThat(task.dueAt()).isEqualTo(now.minus(java.time.Duration.ofHours(24)));
        assertThat(task.status()).isEqualTo("overdue");
    }

    @Test
    void enforceDeviceRegistryBlocksVerificationForProjectWithNoActiveDevice() {
        // T3.2（A4 根因修）：开启强制建档后，零设备档案项目（即便项目级巡检 PASS）核销被拒；默认关不影响既有行为。
        AmusementProjectRecord project = projectWithTemplate("t32-registry");
        submitPassingInspection(project, "t32-registry");
        AmusementSessionRecord session = createSession("t32-registry-session", project.id(), 5, "open");

        // 默认关（enforceDeviceRegistry=false）：零设备不阻断，核销成功。
        registerAmusementEntitlement("credential-t32-off", 1);
        AmusementVerificationRecord ok = service.createVerification(verification(
                "verify-t32-off", project.id(), session.id(), "credential-t32-off"));
        assertThat(ok).isNotNull();

        // 开启强制建档：零在役设备档案项目核销被拒（SAFETY_BLOCKED）。
        service.enforceDeviceRegistry = true;
        registerAmusementEntitlement("credential-t32-on", 1);
        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-t32-on", project.id(), session.id(), "credential-t32-on")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SAFETY_BLOCKED");
    }

    @Test
    void listsProjectsMissingDeviceRegistryUntilActiveDeviceArchived() {
        // T3.3：无在役设备档案的项目出现在「待建档」清单；退役设备不算建档；补一台 active 设备后移出。
        AmusementProjectRecord missing = projectWithTemplate("t33-missing");
        AmusementProjectRecord archived = projectWithTemplate("t33-archived");
        service.createDevice(archived.id(), new AmusementDeviceRequest(
                "t33-archived-dev", null, List.of(), archived.id(),
                "DEV-T33-A", "过山车", "active", 24, null));
        // 退役设备不在役，missing 仅有 retired 设备仍属待建档。
        service.createDevice(missing.id(), new AmusementDeviceRequest(
                "t33-missing-retired", null, List.of(), missing.id(),
                "DEV-T33-R", "退役设备", "retired", 24, null));

        assertThat(service.listProjectsMissingDeviceRegistry().items())
                .extracting(AmusementProjectDeviceRegistryBacklogRecord::id)
                .contains(missing.id())
                .doesNotContain(archived.id());

        // 补一台在役设备 → 移出待建档清单。
        service.createDevice(missing.id(), new AmusementDeviceRequest(
                "t33-missing-active", null, List.of(), missing.id(),
                "DEV-T33-M", "旋转木马", "active", 24, null));
        assertThat(service.listProjectsMissingDeviceRegistry().items())
                .extracting(AmusementProjectDeviceRegistryBacklogRecord::id)
                .doesNotContain(missing.id());
    }

    @Test
    void deviceInspectionExpiryBlocksVerificationEvenWhenRequestOmitsDeviceId() {
        // P0-3 回归：项目级巡检 PASS（旧路径会放行），但项目下有一台在役(active)设备从未通过巡检
        // （lastPass=null=超期）。核销请求不传 deviceId（最常见形态）仍必须被巡检超期项目级兜底阻断，杜绝绕过。
        AmusementProjectRecord project = projectWithTemplate("dev-expiry");
        submitPassingInspection(project, "dev-expiry");
        AmusementSessionRecord session = createSession("dev-expiry-session", project.id(), 5, "open");
        service.createDevice(project.id(), new AmusementDeviceRequest(
                "dev-expiry-create", null, List.of(), project.id(),
                "DEV-EXP-1", "过山车A", "active", 24, null));
        registerAmusementEntitlement("credential-dev-expiry", 1);
        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-dev-expiry", project.id(), session.id(), "credential-dev-expiry")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SAFETY_BLOCKED");
    }

    @Test
    void retiredDeviceWithExpiredInspectionDoesNotBlockProjectVerification() {
        // 对照：退役(retired)设备即便巡检超期也不在役、不参与项目级兜底；项目级巡检 PASS → 正常放行成功。
        AmusementProjectRecord project = projectWithTemplate("dev-retired");
        submitPassingInspection(project, "dev-retired");
        AmusementSessionRecord session = createSession("dev-retired-session", project.id(), 5, "open");
        service.createDevice(project.id(), new AmusementDeviceRequest(
                "dev-retired-create", null, List.of(), project.id(),
                "DEV-RET-1", "退役设备", "retired", 24, null));
        registerAmusementEntitlement("credential-dev-retired", 1);
        AmusementVerificationRecord response = service.createVerification(verification(
                "verify-dev-retired", project.id(), session.id(), "credential-dev-retired"));
        assertThat(response).isNotNull();
    }

    @Test
    void verificationIncrementsUsedCapacityOnlyOnSuccessAndIsIdempotent() {
        AmusementProjectRecord project = projectWithTemplate("verify-success");
        submitPassingInspection(project, "verify-success");
        AmusementSessionRecord session = createSession("verify-session", project.id(), 2, "open");
        registerAmusementEntitlement("credential-success", 3);
        VerificationRequest request = verification("verify-once", project.id(), session.id(), "credential-success");

        AmusementVerificationRecord first = service.createVerification(request);
        AmusementVerificationRecord second = service.createVerification(request);

        // credentialPayload is intentionally excluded from idempotency cache (P1 security fix)
        assertThat(second).usingRecursiveComparison().ignoringFields("credentialPayload").isEqualTo(first);
        assertThat(amusementRepository.findSessionById(session.id()))
                .get()
                .extracting(AmusementSessionRecord::usedCapacity)
                .isEqualTo(1);
        // 权益只消费一次（幂等命中第二次不重复扣 used_times）。
        assertThat(entitlementGateway.usedTimes("credential-success")).isEqualTo(1);
        assertThat(amusementRepository.verifications).hasSize(1);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "amusement.verification_succeeded".equals(event.eventName()))
                .hasSize(1);
    }

    @Test
    void multiPersonAmusementEntitlementClaimsCapacityAtomicallyByPeopleCount() {
        AmusementProjectRecord project = createProject("multi-people", createTemplate("multi-people-tpl").id(), "open", 100);
        submitPassingInspection(project, "multi-people");
        AmusementSessionRecord enoughSession = createSession("multi-people-enough", project.id(), 5, "open");
        registerAmusementEntitlement("credential-multi-4", 1, 4);

        AmusementVerificationRecord record = service.createVerification(verification(
                "verify-multi-4", project.id(), enoughSession.id(), "credential-multi-4"));

        assertThat(record.verificationResult()).isEqualTo("success");
        assertThat(record.verifiedPeople()).isEqualTo(4);
        assertThat(amusementRepository.findSessionById(enoughSession.id()))
                .get().extracting(AmusementSessionRecord::usedCapacity).isEqualTo(4);
        assertThat(entitlementGateway.usedTimes("credential-multi-4")).isEqualTo(1);

        AmusementSessionRecord tightSession = createSession("multi-people-tight", project.id(), 3, "open");
        registerAmusementEntitlement("credential-multi-denied", 1, 4);

        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-multi-denied", project.id(), tightSession.id(), "credential-multi-denied")))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SESSION_CAPACITY_EXHAUSTED");
        assertThat(amusementRepository.findSessionById(tightSession.id()))
                .get().extracting(AmusementSessionRecord::usedCapacity).isEqualTo(0);
        assertThat(entitlementGateway.usedTimes("credential-multi-denied")).isEqualTo(0);
    }

    @Test
    void verificationRejectsMissingExpiredUsedUpNonAmusementAndUnboundEntitlement() {
        AmusementProjectRecord project = projectWithTemplate("entitlement-checks");
        submitPassingInspection(project, "entitlement-checks");
        AmusementSessionRecord session = createSession("entitlement-session", project.id(), 10, "open");

        // 1) 无权益 → ticket_not_found（凭证查不到票务权益）。
        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-no-entitlement", project.id(), session.id(), "credential-missing")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("TICKET_NOT_FOUND");

        // 2) 过期权益 → entitlement_expired（validTo 在过去）。
        entitlementGateway.register("credential-expired", "amusement_single", "active",
                null, Instant.parse("2000-01-01T00:00:00Z"), 1);
        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-expired", project.id(), session.id(), "credential-expired")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ENTITLEMENT_EXPIRED");

        // 3) 次数用尽权益 → entitlement_used（remaining=0）。
        entitlementGateway.register("credential-usedup", "amusement_single", "active", null, null, 0);
        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-usedup", project.id(), session.id(), "credential-usedup")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ENTITLEMENT_USED");

        // 4) 非游乐类权益 → entitlement_not_amusement（如赠饮 gift_drink）。
        entitlementGateway.register("credential-gift", "gift_drink", "active", null, null, 1);
        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-gift", project.id(), session.id(), "credential-gift")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ENTITLEMENT_NOT_AMUSEMENT");
        // 非游乐类被拒后不得消费权益。
        assertThat(entitlementGateway.usedTimes("credential-gift")).isEqualTo(0);

        // 5) 项目配了绑定但该权益 type 不在内 → entitlement_not_for_project。
        amusementRepository.bindProjectEntitlementType(project.id(), "amusement_pass");
        entitlementGateway.register("credential-other-type", "amusement_single", "active", null, null, 1);
        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-unbound", project.id(), session.id(), "credential-other-type")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ENTITLEMENT_NOT_FOR_PROJECT");
    }

    @Test
    void verificationPassesAmusementEntitlementWhenProjectHasNoBindingFallback() {
        AmusementProjectRecord project = projectWithTemplate("fallback");
        submitPassingInspection(project, "fallback");
        AmusementSessionRecord session = createSession("fallback-session", project.id(), 5, "open");
        // 项目无绑定配置 → 回退按「游乐类」放行（向后兼容）。
        registerAmusementEntitlement("credential-fallback", 2);

        AmusementVerificationRecord record = service.createVerification(verification(
                "verify-fallback", project.id(), session.id(), "credential-fallback"));

        assertThat(record.verificationResult()).isEqualTo("success");
        assertThat(entitlementGateway.usedTimes("credential-fallback")).isEqualTo(1);
        assertThat(amusementRepository.findSessionById(session.id()))
                .get().extracting(AmusementSessionRecord::usedCapacity).isEqualTo(1);
    }

    @Test
    void verificationRejectsWhenPointDeviceDimensionFullEvenIfSessionHasRoom() {
        // §6.4：点位/设备维满即拒绝，即便场次仍有余量（任一受控维度余量 0 即拒）。
        AmusementProjectRecord project = createProject("pd-full", createTemplate("pd-full-tpl").id(), "open", 100);
        submitPassingInspection(project, "pd-full");
        AmusementSessionRecord session = createSession("pd-full-session", project.id(), 100, "open");
        // 点位/设备维只配 1 个并发名额（设备 gate_1），场次/项目余量充足。
        amusementRepository.configurePointDeviceCapacity(project.id(), null, "gate_1", 1);
        registerAmusementEntitlement("credential-pd-1", 1);
        registerAmusementEntitlement("credential-pd-2", 1);

        AmusementVerificationRecord first = service.createVerification(verification(
                "verify-pd-1", project.id(), session.id(), "credential-pd-1"));
        assertThat(first.verificationResult()).isEqualTo("success");

        // 第二次：场次还有 99 余量，但点位/设备维已满 → 拒绝且 SESSION_CAPACITY_EXHAUSTED。
        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-pd-2", project.id(), session.id(), "credential-pd-2")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SESSION_CAPACITY_EXHAUSTED");
        // 点位维失败回补权益（未被白扣），场次维已回滚（仍只占用 1）。
        assertThat(entitlementGateway.usedTimes("credential-pd-2")).isEqualTo(0);
        assertThat(amusementRepository.findSessionById(session.id()))
                .get().extracting(AmusementSessionRecord::usedCapacity).isEqualTo(1);
    }

    @Test
    void verificationTakesMinimumAcrossThreeConfiguredDimensions() {
        // §6.4：三维度均配置时取最小可放行余量——最小维度（项目安全上限=2）满即整体拒绝。
        AmusementProjectRecord project = createProject("min-dim", createTemplate("min-dim-tpl").id(), "open", 2);
        submitPassingInspection(project, "min-dim");
        AmusementSessionRecord session = createSession("min-dim-session", project.id(), 10, "open");
        amusementRepository.configurePointDeviceCapacity(project.id(), null, "gate_1", 5);
        registerAmusementEntitlement("credential-min-1", 1);
        registerAmusementEntitlement("credential-min-2", 1);
        registerAmusementEntitlement("credential-min-3", 1);

        // 前两次成功（项目安全上限=2 是最小维度，放行 2 个）。
        assertThat(service.createVerification(verification("v-min-1", project.id(), session.id(), "credential-min-1"))
                .verificationResult()).isEqualTo("success");
        assertThat(service.createVerification(verification("v-min-2", project.id(), session.id(), "credential-min-2"))
                .verificationResult()).isEqualTo("success");

        // 第三次：场次(8 余)与点位(3 余)都有余，但项目安全上限维已满(2/2) → 取最小后拒绝。
        assertThatThrownBy(() -> service.createVerification(verification(
                        "v-min-3", project.id(), session.id(), "credential-min-3")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SESSION_CAPACITY_EXHAUSTED");
        assertThat(entitlementGateway.usedTimes("credential-min-3")).isEqualTo(0);
    }

    @Test
    void verificationDegradesToSessionOnlyWhenNoCapacityConfig() {
        // 向后兼容：无 capacity_config 且项目安全上限=0 时退化为仅场次维（既有单维行为不变）。
        AmusementProjectRecord project = createProject("degrade", createTemplate("degrade-tpl").id(), "open", 0);
        submitPassingInspection(project, "degrade");
        AmusementSessionRecord session = createSession("degrade-session", project.id(), 1, "open");
        registerAmusementEntitlement("credential-deg-1", 1);
        registerAmusementEntitlement("credential-deg-2", 1);

        assertThat(service.createVerification(verification("v-deg-1", project.id(), session.id(), "credential-deg-1"))
                .verificationResult()).isEqualTo("success");
        // 场次满(1/1) → 拒绝（仅场次维生效）。
        assertThatThrownBy(() -> service.createVerification(verification(
                        "v-deg-2", project.id(), session.id(), "credential-deg-2")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SESSION_CAPACITY_EXHAUSTED");
        assertThat(amusementRepository.findSessionById(session.id()))
                .get().extracting(AmusementSessionRecord::usedCapacity).isEqualTo(1);
    }

    @Test
    void verificationPassesWhenEntitlementBoundToProject() {
        AmusementProjectRecord project = projectWithTemplate("bound");
        submitPassingInspection(project, "bound");
        AmusementSessionRecord session = createSession("bound-session", project.id(), 5, "open");
        amusementRepository.bindProjectEntitlementType(project.id(), "amusement_single");
        registerAmusementEntitlement("credential-bound", 2);

        AmusementVerificationRecord record = service.createVerification(verification(
                "verify-bound", project.id(), session.id(), "credential-bound"));

        assertThat(record.verificationResult()).isEqualTo("success");
        assertThat(entitlementGateway.usedTimes("credential-bound")).isEqualTo(1);
    }

    @Test
    void capacityConfigEndpointEnforcesPointDeviceDimensionAfterCreate() {
        // 清单#6：通过 capacity-config 端点录入点位/设备并发上限后，核销三维容量按配置立即生效。
        AmusementProjectRecord project = createProject("cfg-ep", createTemplate("cfg-ep-tpl").id(), "open", 100);
        submitPassingInspection(project, "cfg-ep");
        AmusementSessionRecord session = createSession("cfg-ep-session", project.id(), 100, "open");
        registerAmusementEntitlement("credential-cfg-1", 1);
        registerAmusementEntitlement("credential-cfg-2", 1);

        AmusementCapacityConfigRecord config = service.createCapacityConfig(project.id(),
                new AmusementCapacityConfigRequest("cfg-ep-key", "note", List.of(), null, "gate_1", 1, null, null));
        assertThat(config.maxConcurrent()).isEqualTo(1);
        assertThat(config.enabled()).isTrue();
        assertThat(service.listCapacityConfigs(project.id()).items()).hasSize(1);

        assertThat(service.createVerification(verification("v-cfg-1", project.id(), session.id(), "credential-cfg-1"))
                .verificationResult()).isEqualTo("success");
        // 点位/设备维已满(1/1)，场次仍有余 → 录入即生效，第二次被拒。
        assertThatThrownBy(() -> service.createVerification(verification(
                        "v-cfg-2", project.id(), session.id(), "credential-cfg-2")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SESSION_CAPACITY_EXHAUSTED");
    }

    @Test
    void disableCapacityConfigStopsEnforcingPointDeviceDimension() {
        AmusementProjectRecord project = createProject("cfg-dis", createTemplate("cfg-dis-tpl").id(), "open", 100);
        submitPassingInspection(project, "cfg-dis");
        AmusementSessionRecord session = createSession("cfg-dis-session", project.id(), 100, "open");
        registerAmusementEntitlement("credential-dis-1", 1);
        registerAmusementEntitlement("credential-dis-2", 1);

        AmusementCapacityConfigRecord config = service.createCapacityConfig(project.id(),
                new AmusementCapacityConfigRequest("cfg-dis-key", null, List.of(), null, "gate_1", 1, null, true));
        assertThat(service.createVerification(verification("v-dis-1", project.id(), session.id(), "credential-dis-1"))
                .verificationResult()).isEqualTo("success");

        // 停用后该维度不再受控 → 第二次场次仍有余即放行。
        AmusementCapacityConfigRecord disabled = service.disableCapacityConfig(config.id(), "cfg-dis-disable-key");
        assertThat(disabled.enabled()).isFalse();
        assertThat(service.createVerification(verification("v-dis-2", project.id(), session.id(), "credential-dis-2"))
                .verificationResult()).isEqualTo("success");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 清单#6：超容落事件（§6.4/§7.18 + R6-D10）——硬上限阻断、软上限告警放行、仅告警模式留痕不阻断，均落 over_capacity 事件。
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void hardOverCapacityBlocksAndRecordsOverCapacityIncidentAndEvent() {
        // 安全容量（硬上限）=1：第二次核销达硬上限被阻断，且落 over_capacity incident + 发事件。
        AmusementProjectRecord project = createProject("oc-hard", createTemplate("oc-hard-tpl").id(), "open", 1);
        submitPassingInspection(project, "oc-hard");
        AmusementSessionRecord session = createSession("oc-hard-session", project.id(), 100, "open");
        registerAmusementEntitlement("credential-oc-hard-1", 1);
        registerAmusementEntitlement("credential-oc-hard-2", 1);

        assertThat(service.createVerification(verification("v-oc-hard-1", project.id(), session.id(), "credential-oc-hard-1"))
                .verificationResult()).isEqualTo("success");

        assertThatThrownBy(() -> service.createVerification(verification(
                        "v-oc-hard-2", project.id(), session.id(), "credential-oc-hard-2")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SESSION_CAPACITY_EXHAUSTED");

        // 硬超容被阻断仍落 over_capacity incident。
        assertThat(safetyRepository.incidents())
                .extracting(AmusementIncidentRecord::incidentType)
                .containsExactly("over_capacity");
        // 发 amusement.incident.over_capacity 事件。
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.incident.over_capacity");
        // 硬超容被阻断回补权益（未被白扣）。
        assertThat(entitlementGateway.usedTimes("credential-oc-hard-2")).isEqualTo(0);
    }

    @Test
    void softOverCapacityWarnsAndAllowsAndRecordsOverCapacityIncidentAndEvent() {
        // 运营容量（软上限）=1、硬上限充足：达运营容量即告警放行，落 over_capacity incident + 发事件。
        AmusementProjectRecord project = createProject("oc-soft", createTemplate("oc-soft-tpl").id(), "open", 100);
        submitPassingInspection(project, "oc-soft");
        AmusementSessionRecord session = createSession("oc-soft-session", project.id(), 100, "open");
        amusementRepository.configureOverCapacityPolicy(project.id(), 1, AmusementOverCapacityPolicy.MODE_BLOCK_HARD);
        registerAmusementEntitlement("credential-oc-soft-1", 1);

        // 放行成功（软上限不阻断），但落 over_capacity 告警事件。
        assertThat(service.createVerification(verification("v-oc-soft-1", project.id(), session.id(), "credential-oc-soft-1"))
                .verificationResult()).isEqualTo("success");
        assertThat(entitlementGateway.usedTimes("credential-oc-soft-1")).isEqualTo(1);

        assertThat(safetyRepository.incidents())
                .extracting(AmusementIncidentRecord::incidentType)
                .containsExactly("over_capacity");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.incident.over_capacity");
    }

    @Test
    void warnOnlyDisposalModeDoesNotBypassHardOverCapacityAndStillRecordsIncidentAndEvent() {
        // F-OPS-03（裁决 #6）：warn_only 仅作用于软上限；硬上限=安全容量=人身安全红线，
        // 任何处置模式都必须阻断并回补权益（未被白扣），且仍落 over_capacity incident + 发事件。
        AmusementProjectRecord project = createProject("oc-warn", createTemplate("oc-warn-tpl").id(), "open", 1);
        submitPassingInspection(project, "oc-warn");
        AmusementSessionRecord session = createSession("oc-warn-session", project.id(), 100, "open");
        amusementRepository.configureOverCapacityPolicy(project.id(), 1, AmusementOverCapacityPolicy.MODE_WARN_ONLY);
        registerAmusementEntitlement("credential-oc-warn-1", 1);
        registerAmusementEntitlement("credential-oc-warn-2", 1);

        // 第一次占满硬上限（1/1）成功。
        assertThat(service.createVerification(verification("v-oc-warn-1", project.id(), session.id(), "credential-oc-warn-1"))
                .verificationResult()).isEqualTo("success");

        // 第二次达硬上限：warn_only 也不得旁路，必须阻断并回补权益（未被白扣）。
        assertThatThrownBy(() -> service.createVerification(verification(
                        "v-oc-warn-2", project.id(), session.id(), "credential-oc-warn-2")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SESSION_CAPACITY_EXHAUSTED");
        assertThat(entitlementGateway.usedTimes("credential-oc-warn-2")).isEqualTo(0);

        // 两次（第一次软超容达运营容量 + 第二次硬超容阻断）均落 over_capacity 事件。
        assertThat(safetyRepository.incidents())
                .extracting(AmusementIncidentRecord::incidentType)
                .containsOnly("over_capacity");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .filteredOn(name -> name.equals("amusement.incident.over_capacity"))
                .hasSizeGreaterThanOrEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 统一责任链双写（Wave 2）：超容 incident 落孤岛表后镜像到统一 risk_alerts。
    // source_domain='amusement'、alert_type='over_capacity'；severity→risk_level 映射。
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void hardOverCapacityDoubleWritesUnifiedRiskAlertWithHighLevel() {
        // 硬超容（block_hard 阻断）severity=high → 统一表 risk_level 保持 high。
        AmusementProjectRecord project = createProject("oc-hard-unified", createTemplate("oc-hard-unified-tpl").id(), "open", 1);
        submitPassingInspection(project, "oc-hard-unified");
        AmusementSessionRecord session = createSession("oc-hard-unified-session", project.id(), 100, "open");
        registerAmusementEntitlement("credential-ochu-1", 1);
        registerAmusementEntitlement("credential-ochu-2", 1);

        service.createVerification(verification("v-ochu-1", project.id(), session.id(), "credential-ochu-1"));
        assertThatThrownBy(() -> service.createVerification(verification(
                        "v-ochu-2", project.id(), session.id(), "credential-ochu-2")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SESSION_CAPACITY_EXHAUSTED");

        ArgumentCaptor<RiskAlertReportRequest> captor = ArgumentCaptor.forClass(RiskAlertReportRequest.class);
        verify(riskAlertService, atLeastOnce()).report(captor.capture());
        RiskAlertReportRequest req = captor.getValue();
        assertThat(req.sourceDomain()).isEqualTo("amusement");
        assertThat(req.alertType()).isEqualTo("over_capacity");
        // 硬超容 high 保持 high（未被错误降级）。
        assertThat(req.riskLevel()).isEqualTo("high");
        // sourceRef = incident id；幂等键稳定唯一（incidentId + ":unified"）。
        assertThat(req.idempotencyKey()).isEqualTo(req.sourceRef() + ":unified");
        // sourceRef 与孤岛表 incident id 一致。
        assertThat(req.sourceRef()).isEqualTo(safetyRepository.incidents().get(0).id());
    }

    @Test
    void softOverCapacityDoubleWritesUnifiedRiskAlertMappingMediumToSuspicious() {
        // 软超容（达运营容量告警放行）severity=medium → 统一表 risk_level 必须映射为 suspicious（否则丢数据）。
        AmusementProjectRecord project = createProject("oc-soft-unified", createTemplate("oc-soft-unified-tpl").id(), "open", 100);
        submitPassingInspection(project, "oc-soft-unified");
        AmusementSessionRecord session = createSession("oc-soft-unified-session", project.id(), 100, "open");
        amusementRepository.configureOverCapacityPolicy(project.id(), 1, AmusementOverCapacityPolicy.MODE_BLOCK_HARD);
        registerAmusementEntitlement("credential-ocsu-1", 1);

        service.createVerification(verification("v-ocsu-1", project.id(), session.id(), "credential-ocsu-1"));

        ArgumentCaptor<RiskAlertReportRequest> captor = ArgumentCaptor.forClass(RiskAlertReportRequest.class);
        verify(riskAlertService, atLeastOnce()).report(captor.capture());
        RiskAlertReportRequest req = captor.getValue();
        assertThat(req.sourceDomain()).isEqualTo("amusement");
        assertThat(req.alertType()).isEqualTo("over_capacity");
        // 关键：软超容 medium 映射为 suspicious。
        assertThat(req.riskLevel()).isEqualTo("suspicious");
    }

    @Test
    void overCapacityStillWritesIslandIncidentWhenUnifiedServiceAbsent() {
        // fail-safe：未注入统一责任链服务（riskAlertService=null），核销主流程照常、孤岛 incident 照写。
        AmusementService noUnified = new AmusementService(
                amusementRepository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                authorizationService,
                entitlementGateway,
                safetyRepository);
        AmusementProjectRecord project = createProject("oc-null", createTemplate("oc-null-tpl").id(), "open", 1);
        submitPassingInspection(project, "oc-null");
        AmusementSessionRecord session = createSession("oc-null-session", project.id(), 100, "open");
        registerAmusementEntitlement("credential-ocn-1", 1);
        registerAmusementEntitlement("credential-ocn-2", 1);

        noUnified.createVerification(verification("v-ocn-1", project.id(), session.id(), "credential-ocn-1"));
        // 主流程不抛非预期异常（仍是容量耗尽业务异常）。
        assertThatThrownBy(() -> noUnified.createVerification(verification(
                        "v-ocn-2", project.id(), session.id(), "credential-ocn-2")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SESSION_CAPACITY_EXHAUSTED");
        // 孤岛 incident 仍写入（权威不受统一表缺失影响）。
        assertThat(safetyRepository.incidents())
                .extracting(AmusementIncidentRecord::incidentType)
                .contains("over_capacity");
    }

    @Test
    void overCapacityUnifiedReportFailureDoesNotBreakIslandIncident() {
        // fail-safe：统一表 report 抛 RuntimeException，核销主流程不受影响、孤岛 incident 照写。
        when(riskAlertService.report(any())).thenThrow(new RuntimeException("unified down"));
        AmusementProjectRecord project = createProject("oc-fail", createTemplate("oc-fail-tpl").id(), "open", 1);
        submitPassingInspection(project, "oc-fail");
        AmusementSessionRecord session = createSession("oc-fail-session", project.id(), 100, "open");
        registerAmusementEntitlement("credential-ocf-1", 1);
        registerAmusementEntitlement("credential-ocf-2", 1);

        service.createVerification(verification("v-ocf-1", project.id(), session.id(), "credential-ocf-1"));
        assertThatThrownBy(() -> service.createVerification(verification(
                        "v-ocf-2", project.id(), session.id(), "credential-ocf-2")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SESSION_CAPACITY_EXHAUSTED");
        assertThat(safetyRepository.incidents())
                .extracting(AmusementIncidentRecord::incidentType)
                .contains("over_capacity");
        // report 被调用过（说明双写发生但被 fail-safe 吞掉）。
        verify(riskAlertService, atLeastOnce()).report(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // V104 超容处置策略写入侧（V127）：PUT upsert 策略 + GET 读取。
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void configureOverCapacityPolicyUpsertsNewPolicyAndIsReadable() {
        AmusementProjectRecord project = createProject("ocp-new", createTemplate("ocp-new-tpl").id(), "open", 100);
        AmusementOverCapacityPolicy saved = service.configureOverCapacityPolicy(project.id(),
                new ConfigureOverCapacityPolicyRequest("ocp-new-key", null, List.of(),
                        20, AmusementOverCapacityPolicy.MODE_WARN_ONLY, true));
        assertThat(saved.operatingCapacity()).isEqualTo(20);
        assertThat(saved.disposalMode()).isEqualTo(AmusementOverCapacityPolicy.MODE_WARN_ONLY);
        assertThat(saved.enabled()).isTrue();

        AmusementOverCapacityPolicy read = service.getOverCapacityPolicy(project.id());
        assertThat(read.operatingCapacity()).isEqualTo(20);
        assertThat(read.disposalMode()).isEqualTo(AmusementOverCapacityPolicy.MODE_WARN_ONLY);
        assertThat(read.enabled()).isTrue();
    }

    @Test
    void configureOverCapacityPolicyUpsertOverwritesExistingPolicy() {
        AmusementProjectRecord project = createProject("ocp-upd", createTemplate("ocp-upd-tpl").id(), "open", 100);
        service.configureOverCapacityPolicy(project.id(),
                new ConfigureOverCapacityPolicyRequest("ocp-upd-1", null, List.of(),
                        10, AmusementOverCapacityPolicy.MODE_BLOCK_HARD, true));
        // 同 projectId 二次配置改 disposal_mode 与 operating_capacity（幂等 upsert，覆盖既有行）。
        AmusementOverCapacityPolicy updated = service.configureOverCapacityPolicy(project.id(),
                new ConfigureOverCapacityPolicyRequest("ocp-upd-2", null, List.of(),
                        30, AmusementOverCapacityPolicy.MODE_WARN_ONLY, true));
        assertThat(updated.operatingCapacity()).isEqualTo(30);
        assertThat(updated.disposalMode()).isEqualTo(AmusementOverCapacityPolicy.MODE_WARN_ONLY);
        // 读出仅一行（覆盖而非新增）。
        AmusementOverCapacityPolicy read = service.getOverCapacityPolicy(project.id());
        assertThat(read.operatingCapacity()).isEqualTo(30);
        assertThat(read.disposalMode()).isEqualTo(AmusementOverCapacityPolicy.MODE_WARN_ONLY);
    }

    @Test
    void configureOverCapacityPolicyActuallyEnablesSoftWarnDuringVerification() {
        // 关键端到端：配 operating_capacity 软上限后，达软上限走【告警放行】分支（不阻断）；
        // 硬上限（安全容量=项目 capacity）设足够大不触发硬阻断，验证 hasOperatingCapacity 软告警链已接线。
        AmusementProjectRecord project = createProject("ocp-warn", createTemplate("ocp-warn-tpl").id(), "open", 100);
        submitPassingInspection(project, "ocp-warn");
        AmusementSessionRecord session = createSession("ocp-warn-session", project.id(), 100, "open");
        service.configureOverCapacityPolicy(project.id(),
                new ConfigureOverCapacityPolicyRequest("ocp-warn-key", null, List.of(),
                        1, AmusementOverCapacityPolicy.MODE_WARN_ONLY, true));
        registerAmusementEntitlement("credential-ocp-warn-1", 1);
        registerAmusementEntitlement("credential-ocp-warn-2", 1);

        // 第一次：达运营容量（软上限 1）→ 告警但放行成功。
        assertThat(service.createVerification(verification("v-ocp-warn-1", project.id(), session.id(), "credential-ocp-warn-1"))
                .verificationResult()).isEqualTo("success");
        // 第二次：超运营容量（软上限）→ 仍告警放行成功（硬上限 100 未触发）。
        assertThat(service.createVerification(verification("v-ocp-warn-2", project.id(), session.id(), "credential-ocp-warn-2"))
                .verificationResult()).isEqualTo("success");
        assertThat(safetyRepository.incidents())
                .extracting(AmusementIncidentRecord::incidentType)
                .containsOnly("over_capacity");
    }

    @Test
    void hardSafetyCapacityCannotBeBypassedEvenUnderWarnOnly() {
        // F-OPS-03（裁决#6）：硬上限=安全容量=人身安全红线，warn_only 处置模式不可旁路；
        // 达硬上限一律阻断（SESSION_CAPACITY_EXHAUSTED）并触发自动停运，不因处置模式降级。
        AmusementProjectRecord project = createProject("ocp-hard", createTemplate("ocp-hard-tpl").id(), "open", 1);
        submitPassingInspection(project, "ocp-hard");
        AmusementSessionRecord session = createSession("ocp-hard-session", project.id(), 100, "open");
        service.configureOverCapacityPolicy(project.id(),
                new ConfigureOverCapacityPolicyRequest("ocp-hard-key", null, List.of(),
                        1, AmusementOverCapacityPolicy.MODE_WARN_ONLY, true));
        registerAmusementEntitlement("credential-ocp-hard-1", 1);
        registerAmusementEntitlement("credential-ocp-hard-2", 1);

        // 第一次占满硬上限（1/1）成功。
        assertThat(service.createVerification(verification("v-ocp-hard-1", project.id(), session.id(), "credential-ocp-hard-1"))
                .verificationResult()).isEqualTo("success");
        // 第二次达硬上限：即便 warn_only 也必须阻断（不可旁路安全容量）。
        assertThatThrownBy(() -> service.createVerification(
                verification("v-ocp-hard-2", project.id(), session.id(), "credential-ocp-hard-2")))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("SESSION_CAPACITY_EXHAUSTED");
    }

    @Test
    void configureOverCapacityPolicyRejectsInvalidDisposalMode() {
        AmusementProjectRecord project = createProject("ocp-mode", createTemplate("ocp-mode-tpl").id(), "open", 100);
        assertThatThrownBy(() -> service.configureOverCapacityPolicy(project.id(),
                new ConfigureOverCapacityPolicyRequest("ocp-mode-key", null, List.of(),
                        10, "soft_warn", true)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void configureOverCapacityPolicyRejectsNegativeOperatingCapacity() {
        AmusementProjectRecord project = createProject("ocp-neg", createTemplate("ocp-neg-tpl").id(), "open", 100);
        assertThatThrownBy(() -> service.configureOverCapacityPolicy(project.id(),
                new ConfigureOverCapacityPolicyRequest("ocp-neg-key", null, List.of(),
                        -1, AmusementOverCapacityPolicy.MODE_BLOCK_HARD, true)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void configureOverCapacityPolicyRejectsUnknownProject() {
        assertThatThrownBy(() -> service.configureOverCapacityPolicy("project_missing",
                new ConfigureOverCapacityPolicyRequest("ocp-missing-key", null, List.of(),
                        10, AmusementOverCapacityPolicy.MODE_BLOCK_HARD, true)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("PROJECT_NOT_FOUND");
    }

    @Test
    void configureOverCapacityPolicyRejectsCrossMerchantProject() {
        TestSecurityActors.asMerchantUser("merchant_a");
        AmusementProjectRecord project = createProject("ocp-xm", createTemplate("ocp-xm-tpl").id(), "open", 100);
        TestSecurityActors.asMerchantUser("merchant_b");
        assertThatThrownBy(() -> service.configureOverCapacityPolicy(project.id(),
                new ConfigureOverCapacityPolicyRequest("ocp-xm-key", null, List.of(),
                        10, AmusementOverCapacityPolicy.MODE_BLOCK_HARD, true)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void getOverCapacityPolicyReturnsDisabledPlaceholderWhenUnconfigured() {
        AmusementProjectRecord project = createProject("ocp-read", createTemplate("ocp-read-tpl").id(), "open", 100);
        AmusementOverCapacityPolicy read = service.getOverCapacityPolicy(project.id());
        assertThat(read.projectId()).isEqualTo(project.id());
        assertThat(read.enabled()).isFalse();
        assertThat(read.disposalMode()).isEqualTo(AmusementOverCapacityPolicy.MODE_BLOCK_HARD);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REQ-53 权益会话（V129 entitlement_sessions）：发起 → 展示 → 领取核销（防错领 + 会话时效）。
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void initiateEntitlementSessionCreatesInitiatedSessionAndIsIdempotent() {
        EntitlementSessionRecord first = service.initiateEntitlementSession(
                new InitiateEntitlementSessionRequest("es-init-1", "issued_ent_1", null, 300, "phone_1"));
        assertThat(first.status()).isEqualTo("initiated");
        assertThat(first.entitlementRef()).isEqualTo("issued_ent_1");
        assertThat(first.initiatedBy()).isEqualTo("phone_1");
        assertThat(first.expiresAt()).isAfter(Instant.now());
        // 同幂等键重复发起返回同一记录（不新增）。
        EntitlementSessionRecord again = service.initiateEntitlementSession(
                new InitiateEntitlementSessionRequest("es-init-1", "issued_ent_1", null, 300, "phone_1"));
        assertThat(again.id()).isEqualTo(first.id());
        assertThat(service.listEntitlementSessions().items()).hasSize(1);
    }

    @Test
    void initiateEntitlementSessionRejectsBlankEntitlementRef() {
        assertThatThrownBy(() -> service.initiateEntitlementSession(
                new InitiateEntitlementSessionRequest("es-blank", "  ", null, 300, null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void initiateEntitlementSessionRejectsNonPositiveTtl() {
        assertThatThrownBy(() -> service.initiateEntitlementSession(
                new InitiateEntitlementSessionRequest("es-ttl", "issued_ent_1", null, 0, null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void getEntitlementSessionTransitionsInitiatedToDisplayedAndReadsByCode() {
        EntitlementSessionRecord created = service.initiateEntitlementSession(
                new InitiateEntitlementSessionRequest("es-view-1", "issued_ent_1", null, 300, null));
        // 一体机首次展示：initiated → displayed。
        EntitlementSessionRecord viewed = service.getEntitlementSession(created.id());
        assertThat(viewed.status()).isEqualTo("displayed");
        // 也可按 session_code 查询，仍返回 displayed（不回退）。
        EntitlementSessionRecord byCode = service.getEntitlementSession(created.sessionCode());
        assertThat(byCode.status()).isEqualTo("displayed");
    }

    @Test
    void claimEntitlementSessionSucceedsForInitiatedSessionAndIsIdempotent() {
        EntitlementSessionRecord created = service.initiateEntitlementSession(
                new InitiateEntitlementSessionRequest("es-claim-1", "issued_ent_1", null, 300, null));
        EntitlementSessionRecord claimed = service.claimEntitlementSession(created.id(),
                new ClaimEntitlementSessionRequest("es-claim-key-1", "kiosk_1"));
        assertThat(claimed.status()).isEqualTo("claimed");
        assertThat(claimed.claimedBy()).isEqualTo("kiosk_1");
        assertThat(claimed.claimedAt()).isNotNull();
        // 同幂等键重复领取返回同一结果（幂等缓存命中，仍 claimed）。
        EntitlementSessionRecord again = service.claimEntitlementSession(created.id(),
                new ClaimEntitlementSessionRequest("es-claim-key-1", "kiosk_1"));
        assertThat(again.status()).isEqualTo("claimed");
    }

    @Test
    void claimEntitlementSessionRejectsExpiredSession() {
        // 写入一条已过期会话（expires_at 在过去）。
        Instant past = Instant.now().minusSeconds(60);
        amusementRepository.putEntitlementSession(new EntitlementSessionRecord(
                "esession_expired", "escode_expired", "issued_ent_1", null, null,
                "initiated", past, "phone_1", null, null, past, past));
        assertThatThrownBy(() -> service.claimEntitlementSession("esession_expired",
                new ClaimEntitlementSessionRequest("es-exp-key", "kiosk_1")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ENTITLEMENT_SESSION_EXPIRED");
    }

    @Test
    void claimEntitlementSessionRejectsDoubleClaim() {
        EntitlementSessionRecord created = service.initiateEntitlementSession(
                new InitiateEntitlementSessionRequest("es-dbl-1", "issued_ent_1", null, 300, null));
        service.claimEntitlementSession(created.id(),
                new ClaimEntitlementSessionRequest("es-dbl-key-1", "kiosk_1"));
        // 不同幂等键再次领取已 claimed 会话：条件 UPDATE 0 行 → 拒绝（防重复领）。
        assertThatThrownBy(() -> service.claimEntitlementSession(created.id(),
                new ClaimEntitlementSessionRequest("es-dbl-key-2", "kiosk_2")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ENTITLEMENT_SESSION_NOT_CLAIMABLE");
    }

    @Test
    void claimEntitlementSessionRejectsCancelledSession() {
        Instant now = Instant.now();
        amusementRepository.putEntitlementSession(new EntitlementSessionRecord(
                "esession_cancelled", "escode_cancelled", "issued_ent_1", null, null,
                "cancelled", now.plusSeconds(300), "phone_1", null, null, now, now));
        assertThatThrownBy(() -> service.claimEntitlementSession("esession_cancelled",
                new ClaimEntitlementSessionRequest("es-cxl-key", "kiosk_1")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ENTITLEMENT_SESSION_NOT_CLAIMABLE");
    }

    @Test
    void claimEntitlementSessionRejectsUnknownSession() {
        assertThatThrownBy(() -> service.claimEntitlementSession("esession_missing",
                new ClaimEntitlementSessionRequest("es-miss-key", "kiosk_1")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ENTITLEMENT_SESSION_NOT_FOUND");
    }

    @Test
    void listEntitlementSessionsIsolatesByMerchant() {
        TestSecurityActors.asMerchantUser("merchant_a");
        service.initiateEntitlementSession(
                new InitiateEntitlementSessionRequest("es-iso-a", "issued_ent_a", null, 300, null));
        TestSecurityActors.asMerchantUser("merchant_b");
        service.initiateEntitlementSession(
                new InitiateEntitlementSessionRequest("es-iso-b", "issued_ent_b", null, 300, null));
        // merchant_b 只看到自己的一条。
        assertThat(service.listEntitlementSessions().items())
                .extracting(EntitlementSessionRecord::entitlementRef)
                .containsExactly("issued_ent_b");
    }

    @Test
    void getEntitlementSessionRejectsCrossMerchant() {
        TestSecurityActors.asMerchantUser("merchant_a");
        EntitlementSessionRecord created = service.initiateEntitlementSession(
                new InitiateEntitlementSessionRequest("es-xm-1", "issued_ent_1", null, 300, null));
        TestSecurityActors.asMerchantUser("merchant_b");
        assertThatThrownBy(() -> service.getEntitlementSession(created.id()))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void claimEntitlementSessionRejectsCrossMerchant() {
        TestSecurityActors.asMerchantUser("merchant_a");
        EntitlementSessionRecord created = service.initiateEntitlementSession(
                new InitiateEntitlementSessionRequest("es-xmc-1", "issued_ent_1", null, 300, null));
        TestSecurityActors.asMerchantUser("merchant_b");
        assertThatThrownBy(() -> service.claimEntitlementSession(created.id(),
                new ClaimEntitlementSessionRequest("es-xmc-key", "kiosk_b")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void capacityConfigRequiresPointOrDeviceAndMaxConcurrent() {
        AmusementProjectRecord project = createProject("cfg-val", createTemplate("cfg-val-tpl").id(), "open", 5);
        assertThatThrownBy(() -> service.createCapacityConfig(project.id(),
                new AmusementCapacityConfigRequest("cfg-val-a", null, List.of(), null, null, 3, null, null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
        assertThatThrownBy(() -> service.createCapacityConfig(project.id(),
                new AmusementCapacityConfigRequest("cfg-val-b", null, List.of(), null, "gate_1", null, null, null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void capacityConfigRejectsCrossMerchantProject() {
        TestSecurityActors.asMerchantUser("merchant_a");
        AmusementProjectRecord project = createProject("cfg-xm", createTemplate("cfg-xm-tpl").id(), "open", 5);
        TestSecurityActors.asMerchantUser("merchant_b");
        assertThatThrownBy(() -> service.createCapacityConfig(project.id(),
                new AmusementCapacityConfigRequest("cfg-xm-key", null, List.of(), null, "gate_1", 1, null, null)))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("CROSS_MERCHANT_FORBIDDEN");
    }

    @Test
    void projectEntitlementEndpointTightensVerificationBinding() {
        // 清单#6：录入项目↔权益绑定后，核销收紧——未绑定的游乐类权益不再回退放行。
        AmusementProjectRecord project = projectWithTemplate("ent-ep");
        submitPassingInspection(project, "ent-ep");
        AmusementSessionRecord session = createSession("ent-ep-session", project.id(), 5, "open");
        // 绑定 amusement_pass；注册的是 amusement_single（默认）→ 收紧后被拒。
        AmusementProjectEntitlementRecord binding = service.createProjectEntitlement(project.id(),
                new AmusementProjectEntitlementRequest("ent-ep-key", "note", List.of(), "amusement_pass"));
        assertThat(binding.entitlementType()).isEqualTo("amusement_pass");
        assertThat(service.listProjectEntitlements(project.id()).items()).hasSize(1);
        registerAmusementEntitlement("credential-ent-1", 1);

        assertThatThrownBy(() -> service.createVerification(verification(
                        "v-ent-1", project.id(), session.id(), "credential-ent-1")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ENTITLEMENT_NOT_FOR_PROJECT");
        // 权益未被白扣（绑定校验在消费前）。
        assertThat(entitlementGateway.usedTimes("credential-ent-1")).isEqualTo(0);
    }

    @Test
    void projectEntitlementEndpointAllowsMatchingBoundType() {
        AmusementProjectRecord project = projectWithTemplate("ent-ok");
        submitPassingInspection(project, "ent-ok");
        AmusementSessionRecord session = createSession("ent-ok-session", project.id(), 5, "open");
        service.createProjectEntitlement(project.id(),
                new AmusementProjectEntitlementRequest("ent-ok-key", null, List.of(), "amusement_single"));
        registerAmusementEntitlement("credential-ent-ok", 1);

        assertThat(service.createVerification(verification("v-ent-ok", project.id(), session.id(), "credential-ent-ok"))
                .verificationResult()).isEqualTo("success");
    }

    @Test
    void removeProjectEntitlementRestoresFallback() {
        AmusementProjectRecord project = projectWithTemplate("ent-rm");
        submitPassingInspection(project, "ent-rm");
        AmusementSessionRecord session = createSession("ent-rm-session", project.id(), 5, "open");
        service.createProjectEntitlement(project.id(),
                new AmusementProjectEntitlementRequest("ent-rm-key", null, List.of(), "amusement_pass"));
        // V341（R-12）：解绑=存档软删（必填理由），行留库不物理删。
        service.archiveProjectEntitlement(project.id(), "amusement_pass", "配错权益类型", "ent-rm-del-key");
        // 默认列表不显示已存档绑定（删除铁律「前台不显」）。
        assertThat(service.listProjectEntitlements(project.id()).items()).isEmpty();
        registerAmusementEntitlement("credential-ent-rm", 1);

        // 存档后无任何生效绑定 → 回退按游乐类放行（向后兼容）。
        assertThat(service.createVerification(verification("v-ent-rm", project.id(), session.id(), "credential-ent-rm"))
                .verificationResult()).isEqualTo("success");
    }

    // ================= V341（v7 审计 R-12）游乐权益绑定软删范式 =================

    @Test
    void archivedProjectEntitlementStaysInStoreAndIsRecoverable() {
        AmusementProjectRecord project = projectWithTemplate("ent-soft");
        service.createProjectEntitlement(project.id(),
                new AmusementProjectEntitlementRequest("ent-soft-key", null, List.of(), "amusement_pass"));

        service.archiveProjectEntitlement(project.id(), "amusement_pass", "赛季结束下线", "ent-soft-arch");

        // 存库：includeArchived=true 仍能读到该行，且理由/存档时间留痕（不是物理删）。
        List<AmusementProjectEntitlementRecord> archived = service.listProjectEntitlements(project.id(), true).items();
        assertThat(archived).hasSize(1);
        assertThat(archived.get(0).archivedAt()).isNotNull();
        assertThat(archived.get(0).archiveReason()).isEqualTo("赛季结束下线");
        // 前台不显：默认视图为空。
        assertThat(service.listProjectEntitlements(project.id()).items()).isEmpty();

        // 可恢复：恢复后回到生效中，默认视图重新可见。
        service.restoreProjectEntitlement(project.id(), "amusement_pass", "ent-soft-restore");
        assertThat(service.listProjectEntitlements(project.id()).items())
                .extracting(AmusementProjectEntitlementRecord::entitlementType)
                .containsExactly("amusement_pass");
        assertThat(service.listProjectEntitlements(project.id()).items().get(0).archiveReason()).isNull();
    }

    @Test
    void archiveProjectEntitlementRejectsBlankReason() {
        AmusementProjectRecord project = projectWithTemplate("ent-noreason");
        service.createProjectEntitlement(project.id(),
                new AmusementProjectEntitlementRequest("ent-noreason-key", null, List.of(), "amusement_pass"));

        assertThatThrownBy(() -> service.archiveProjectEntitlement(project.id(), "amusement_pass", "   ", "ent-noreason-arch"))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
        // 理由为空被拒后绑定仍生效（未被误存档）。
        assertThat(service.listProjectEntitlements(project.id()).items()).hasSize(1);
    }

    @Test
    void archivedProjectEntitlementNoLongerGatesVerification() {
        // 读取路径①核销绑定校验：存档后该绑定不再被采信（既不收紧核销、也不留幽灵绑定）。
        AmusementProjectRecord project = projectWithTemplate("ent-gate");
        submitPassingInspection(project, "ent-gate");
        AmusementSessionRecord session = createSession("ent-gate-session", project.id(), 5, "open");
        service.createProjectEntitlement(project.id(),
                new AmusementProjectEntitlementRequest("ent-gate-key", null, List.of(), "amusement_pass"));
        service.archiveProjectEntitlement(project.id(), "amusement_pass", "改配", "ent-gate-arch");
        registerAmusementEntitlement("credential-ent-gate", 1);

        assertThat(service.createVerification(verification("v-ent-gate", project.id(), session.id(), "credential-ent-gate"))
                .verificationResult()).isEqualTo("success");
    }

    @Test
    void deleteProjectEntitlementRequiresArchiveThenApprovalAndKeepsTombstone() {
        AmusementProjectRecord project = projectWithTemplate("ent-del");
        service.createProjectEntitlement(project.id(),
                new AmusementProjectEntitlementRequest("ent-del-key", null, List.of(), "amusement_pass"));

        // 未存档不得申请删除。
        assertThatThrownBy(() -> service.requestDeleteProjectEntitlement(
                project.id(), "amusement_pass", "配错了", "ent-del-req0"))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("PROJECT_ENTITLEMENT_NOT_ARCHIVED");

        service.archiveProjectEntitlement(project.id(), "amusement_pass", "先下线", "ent-del-arch");
        // 删除理由必填。
        assertThatThrownBy(() -> service.requestDeleteProjectEntitlement(
                project.id(), "amusement_pass", null, "ent-del-req-blank"))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");

        AmusementProjectEntitlementRecord pending = service.requestDeleteProjectEntitlement(
                project.id(), "amusement_pass", "配错了不再用", "ent-del-req");
        assertThat(pending.deleteStatus()).isEqualTo("pending");
        assertThat(pending.deleteReason()).isEqualTo("配错了不再用");

        // 审批通过 = 落墓碑，行仍在库（includeArchived 可见），默认视图不显示。
        // 职责分离：换一名平台管理员审批（同人审批见 selfApprove 用例）。
        TestSecurityActors.asPlatformAdmin("usr_admin_approver");
        AmusementProjectEntitlementRecord tomb = service.decideDeleteProjectEntitlement(
                project.id(), "amusement_pass", "approved", "ent-del-appr");
        assertThat(tomb.deleteStatus()).isEqualTo("approved");
        assertThat(service.listProjectEntitlements(project.id()).items()).isEmpty();
        assertThat(service.listProjectEntitlements(project.id(), true).items()).hasSize(1);
        assertThat(amusementRepository.listProjectEntitlementTypes(project.id())).isEmpty();
    }

    @Test
    void rejectedDeleteProjectEntitlementFallsBackToArchivedOnly() {
        AmusementProjectRecord project = projectWithTemplate("ent-rej");
        service.createProjectEntitlement(project.id(),
                new AmusementProjectEntitlementRequest("ent-rej-key", null, List.of(), "amusement_pass"));
        service.archiveProjectEntitlement(project.id(), "amusement_pass", "先下线", "ent-rej-arch");
        service.requestDeleteProjectEntitlement(project.id(), "amusement_pass", "误提", "ent-rej-req");

        TestSecurityActors.asPlatformAdmin("usr_admin_approver");
        AmusementProjectEntitlementRecord decided = service.decideDeleteProjectEntitlement(
                project.id(), "amusement_pass", "rejected", "ent-rej-dec");
        assertThat(decided.deleteStatus()).isEqualTo("none");
        assertThat(decided.archivedAt()).isNotNull();
    }

    @Test
    void deleteRequesterCannotApproveOwnProjectEntitlementDeletion() {
        AmusementProjectRecord project = projectWithTemplate("ent-self");
        service.createProjectEntitlement(project.id(),
                new AmusementProjectEntitlementRequest("ent-self-key", null, List.of(), "amusement_pass"));
        service.archiveProjectEntitlement(project.id(), "amusement_pass", "先下线", "ent-self-arch");
        service.requestDeleteProjectEntitlement(project.id(), "amusement_pass", "误配", "ent-self-req");

        // 发起人=当前主体（usr_admin）→ 禁自审。
        assertThatThrownBy(() -> service.decideDeleteProjectEntitlement(
                project.id(), "amusement_pass", "approved", "ent-self-appr"))
                .isInstanceOf(ApiException.class).extracting("code")
                .isEqualTo("PROJECT_ENTITLEMENT_SELF_APPROVE_FORBIDDEN");
    }

    @Test
    void tombstonedProjectEntitlementRestoreRequiresApprovePermission() {
        AmusementProjectRecord project = projectWithTemplate("ent-tomb");
        service.createProjectEntitlement(project.id(),
                new AmusementProjectEntitlementRequest("ent-tomb-key", null, List.of(), "amusement_pass"));
        service.archiveProjectEntitlement(project.id(), "amusement_pass", "先下线", "ent-tomb-arch");
        service.requestDeleteProjectEntitlement(project.id(), "amusement_pass", "作废", "ent-tomb-req");
        TestSecurityActors.asPlatformAdmin("usr_admin_approver");
        service.decideDeleteProjectEntitlement(project.id(), "amusement_pass", "approved", "ent-tomb-appr");

        // 权限库里没有 amusement.project_entitlement.approve（permissionExists=false）→ 墓碑恢复被拒。
        assertThatThrownBy(() -> service.restoreProjectEntitlement(project.id(), "amusement_pass", "ent-tomb-res1"))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("PERMISSION_DENIED");

        // 授予审批权（最高权限 role_platform_admin 持有）后墓碑可恢复——删除铁律「可恢复」。
        when(accessRepository.permissionExists("amusement.project_entitlement.approve")).thenReturn(true);
        AmusementProjectEntitlementRecord restored =
                service.restoreProjectEntitlement(project.id(), "amusement_pass", "ent-tomb-res2");
        assertThat(restored.deleteStatus()).isEqualTo("none");
        assertThat(restored.archivedAt()).isNull();
        assertThat(amusementRepository.listProjectEntitlementTypes(project.id())).containsExactly("amusement_pass");
    }

    @Test
    void decideDeleteProjectEntitlementRejectsUnknownDecision() {
        AmusementProjectRecord project = projectWithTemplate("ent-dec");
        service.createProjectEntitlement(project.id(),
                new AmusementProjectEntitlementRequest("ent-dec-key", null, List.of(), "amusement_pass"));
        service.archiveProjectEntitlement(project.id(), "amusement_pass", "先下线", "ent-dec-arch");
        service.requestDeleteProjectEntitlement(project.id(), "amusement_pass", "待定", "ent-dec-req");

        assertThatThrownBy(() -> service.decideDeleteProjectEntitlement(
                project.id(), "amusement_pass", "maybe", "ent-dec-bad"))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void rebindingArchivedProjectEntitlementRevivesSameRow() {
        // 唯一键 (project_id, entitlement_type) 使已存档行占位；复绑必须复活既有行而非静默失效。
        AmusementProjectRecord project = projectWithTemplate("ent-revive");
        AmusementProjectEntitlementRecord created = service.createProjectEntitlement(project.id(),
                new AmusementProjectEntitlementRequest("ent-revive-key", null, List.of(), "amusement_pass"));
        service.archiveProjectEntitlement(project.id(), "amusement_pass", "临时下线", "ent-revive-arch");

        AmusementProjectEntitlementRecord rebound = service.createProjectEntitlement(project.id(),
                new AmusementProjectEntitlementRequest("ent-revive-key2", null, List.of(), "amusement_pass"));
        assertThat(rebound.id()).isEqualTo(created.id());
        assertThat(rebound.archivedAt()).isNull();
        assertThat(amusementRepository.listProjectEntitlementTypes(project.id())).containsExactly("amusement_pass");
    }

    @Test
    void projectEntitlementRejectsNonAmusementType() {
        AmusementProjectRecord project = projectWithTemplate("ent-bad");
        assertThatThrownBy(() -> service.createProjectEntitlement(project.id(),
                new AmusementProjectEntitlementRequest("ent-bad-key", null, List.of(), "ticket_single")))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("ENTITLEMENT_NOT_AMUSEMENT");
    }

    @Test
    void manualOverrideRequiresReasonAndRecordsOverride() {
        AmusementProjectRecord project = projectWithTemplate("override");
        AmusementSessionRecord session = createSession("override-session", project.id(), 1, "open");

        assertThatThrownBy(() -> service.createManualOverride(new AmusementManualOverrideRequest(
                        "override-missing-reason",
                        "override",
                        List.of("att_1"),
                        project.id(),
                        session.id(),
                        "allow",
                        " "
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("VALIDATION_FAILED");

        AmusementManualOverrideRecord override = service.createManualOverride(new AmusementManualOverrideRequest(
                "override-once",
                "override",
                List.of("att_1"),
                project.id(),
                session.id(),
                "allow",
                "supervisor approved"
        ));

        assertThat(override)
                .extracting(AmusementManualOverrideRecord::overrideType, AmusementManualOverrideRecord::status)
                .containsExactly("allow", "approved");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.verification_override_failed", "amusement.verification_overridden");
    }

    @Test
    void reservationAndQueueWritesAreIdempotentStatusedAndAudited() {
        AmusementProjectRecord project = projectWithTemplate("reservation-queue");
        AmusementSessionRecord session = createSession("reservation-queue-session", project.id(), 3, "open");

        AmusementReservationRequest reservationRequest = new AmusementReservationRequest(
                "reservation-queue-once",
                project.id(),
                session.id(),
                "customer_1",
                null
        );
        AmusementReservationRecord firstReservation = service.createReservation(reservationRequest);
        AmusementReservationRecord secondReservation = service.createReservation(reservationRequest);

        assertThat(secondReservation).isEqualTo(firstReservation);
        assertThat(firstReservation)
                .extracting(
                        AmusementReservationRecord::projectId,
                        AmusementReservationRecord::sessionId,
                        AmusementReservationRecord::customerId,
                        AmusementReservationRecord::reservationStatus
                )
                .containsExactly(project.id(), session.id(), "customer_1", "reserved");

        AmusementReservationRecord completedReservation = service.changeReservationStatus(
                firstReservation.id(),
                new StatusChangeRequest("reservation-complete-once", "completed", "customer arrived", List.of("att_reservation"))
        );

        AmusementQueueEntryRequest queueRequest = new AmusementQueueEntryRequest(
                "queue-once",
                project.id(),
                session.id(),
                "customer_2",
                "A010",
                null
        );
        AmusementQueueEntryRecord firstQueue = service.createQueueEntry(queueRequest);
        AmusementQueueEntryRecord secondQueue = service.createQueueEntry(queueRequest);

        assertThat(secondQueue).isEqualTo(firstQueue);
        assertThat(firstQueue)
                .extracting(
                        AmusementQueueEntryRecord::projectId,
                        AmusementQueueEntryRecord::sessionId,
                        AmusementQueueEntryRecord::customerId,
                        AmusementQueueEntryRecord::queueNo,
                        AmusementQueueEntryRecord::status
                )
                .containsExactly(project.id(), session.id(), "customer_2", "A010", "waiting");

        AmusementQueueEntryRecord calledQueue = service.changeQueueEntryStatus(
                firstQueue.id(),
                new StatusChangeRequest("queue-call-once", "called", "notify customer", List.of("att_queue"))
        );

        assertThat(completedReservation.reservationStatus()).isEqualTo("completed");
        assertThat(calledQueue.status()).isEqualTo("called");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "amusement.reservation.created",
                        "amusement.reservation.status_changed",
                        "amusement.queue.created",
                        "amusement.queue.status_changed"
                );
    }

    @Test
    void reservationAndQueueRejectSessionMismatchClosedSessionAndMissingRows() {
        AmusementProjectRecord project = projectWithTemplate("reservation-reject");
        AmusementProjectRecord otherProject = projectWithTemplate("reservation-reject-other");
        AmusementSessionRecord otherSession = createSession("reservation-reject-other-session", otherProject.id(), 2, "open");
        AmusementSessionRecord closedSession = createSession("reservation-reject-closed-session", project.id(), 2, "closed");

        assertThatThrownBy(() -> service.createReservation(new AmusementReservationRequest(
                        "reservation-mismatch",
                        project.id(),
                        otherSession.id(),
                        "customer_1",
                        null
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SESSION_PROJECT_MISMATCH");

        assertThatThrownBy(() -> service.createQueueEntry(new AmusementQueueEntryRequest(
                        "queue-closed",
                        project.id(),
                        closedSession.id(),
                        "customer_1",
                        "A011",
                        null
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SESSION_NOT_ACCEPTING_ENTRY");

        assertThatThrownBy(() -> service.changeReservationStatus(
                        "missing_reservation",
                        new StatusChangeRequest("reservation-missing", "cancelled", "not found", List.of())
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("RESERVATION_NOT_FOUND");

        assertThatThrownBy(() -> service.changeQueueEntryStatus(
                        "missing_queue",
                        new StatusChangeRequest("queue-missing", "cancelled", "not found", List.of())
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("QUEUE_ENTRY_NOT_FOUND");
    }

    @Test
    void groupBookingApprovalLocksCapacityAndReleaseRestoresAvailability() {
        AmusementProjectRecord project = projectWithTemplate("group-lock");
        submitPassingInspection(project, "group-lock");
        AmusementSessionRecord session = createSession("group-lock-session", project.id(), 2, "open");

        AmusementGroupBookingRecord booking = service.createGroupBooking(new AmusementGroupBookingRequest(
                "group-lock-create",
                "operator note",
                List.of("att_group"),
                project.id(),
                session.id(),
                "group",
                "Company Team",
                "13800000000",
                2,
                2
        ));

        assertThat(booking.bookingStatus()).isEqualTo("pending_approval");
        assertThat(amusementRepository.findSessionById(session.id()))
                .get()
                .extracting(AmusementSessionRecord::lockedCapacity)
                .isEqualTo(0);

        AmusementGroupBookingRecord approved = service.approveGroupBooking(
                booking.id(),
                new AmusementGroupBookingApprovalRequest("group-lock-approve", "ops approved", List.of("att_approve"))
        );

        assertThat(approved.bookingStatus()).isEqualTo("approved");
        // V231 资金/时间铁律：本域无独立支付步，审批确认即预定确认落点，paid_at=approved_at，非 null。
        assertThat(approved.paidAt()).isNotNull();
        assertThat(approved.paidAt()).isEqualTo(approved.approvedAt());
        assertThat(booking.paidAt()).as("审批前预定 paid_at 为 null").isNull();
        assertThat(amusementRepository.findSessionById(session.id()))
                .get()
                .extracting(AmusementSessionRecord::lockedCapacity)
                .isEqualTo(2);
        registerAmusementEntitlement("credential-group-locked", 1);
        assertThatThrownBy(() -> service.createVerification(verification(
                        "verify-group-locked",
                        project.id(),
                        session.id(),
                        "credential-group-locked"
                )))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SESSION_CAPACITY_EXHAUSTED");
        // 容量耗尽时已消费的权益被回补（补偿回滚），used_times 回到 0。
        assertThat(entitlementGateway.usedTimes("credential-group-locked")).isEqualTo(0);

        AmusementGroupBookingRecord released = service.releaseGroupBooking(
                booking.id(),
                new AmusementGroupBookingReleaseRequest("group-lock-release", "customer changed plan", List.of("att_release"))
        );

        assertThat(released.bookingStatus()).isEqualTo("released");
        assertThat(amusementRepository.findSessionById(session.id()))
                .get()
                .extracting(AmusementSessionRecord::lockedCapacity)
                .isEqualTo(0);
        registerAmusementEntitlement("credential-after-release", 1);
        assertThat(service.createVerification(verification(
                "verify-group-after-release",
                project.id(),
                session.id(),
                "credential-after-release"
        )).verificationResult()).isEqualTo("success");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "amusement.group_booking.created",
                        "amusement.group_booking.approved",
                        "amusement.group_booking.released"
                );
    }

    @Test
    void groupBookingRejectsRepeatedApproveAndRepeatedReleaseWithoutCapacityDrift() {
        AmusementProjectRecord project = projectWithTemplate("group-repeat");
        submitPassingInspection(project, "group-repeat");
        AmusementSessionRecord session = createSession("group-repeat-session", project.id(), 3, "open");
        AmusementGroupBookingRecord booking = service.createGroupBooking(new AmusementGroupBookingRequest(
                "group-repeat-create",
                "operator note",
                List.of("att_group"),
                project.id(),
                session.id(),
                "group",
                "Company Team",
                "13800000000",
                2,
                2
        ));

        service.approveGroupBooking(
                booking.id(),
                new AmusementGroupBookingApprovalRequest("group-repeat-approve", "ops approved", List.of())
        );

        assertThatThrownBy(() -> service.approveGroupBooking(
                        booking.id(),
                        new AmusementGroupBookingApprovalRequest("group-repeat-approve-again", "ops approved again", List.of())
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("GROUP_BOOKING_NOT_PENDING");
        assertThat(amusementRepository.findSessionById(session.id()))
                .get()
                .extracting(AmusementSessionRecord::lockedCapacity)
                .isEqualTo(2);

        service.releaseGroupBooking(
                booking.id(),
                new AmusementGroupBookingReleaseRequest("group-repeat-release", "customer changed plan", List.of())
        );

        assertThatThrownBy(() -> service.releaseGroupBooking(
                        booking.id(),
                        new AmusementGroupBookingReleaseRequest("group-repeat-release-again", "customer changed plan again", List.of())
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("GROUP_BOOKING_NOT_APPROVED");
        assertThat(amusementRepository.findSessionById(session.id()))
                .get()
                .extracting(AmusementSessionRecord::lockedCapacity)
                .isEqualTo(0);
    }

    @Test
    void groupBookingApprovalRejectsInsufficientRemainingCapacityWithoutMutation() {
        AmusementProjectRecord project = projectWithTemplate("group-capacity");
        submitPassingInspection(project, "group-capacity");
        AmusementSessionRecord session = createSession("group-capacity-session", project.id(), 2, "open");
        registerAmusementEntitlement("credential-before-lock", 1);
        service.createVerification(verification("verify-before-group-lock", project.id(), session.id(), "credential-before-lock"));

        AmusementGroupBookingRecord booking = service.createGroupBooking(new AmusementGroupBookingRequest(
                "group-capacity-create",
                "operator note",
                List.of("att_group"),
                project.id(),
                session.id(),
                "private",
                "Private Event",
                "13900000000",
                2,
                2
        ));

        assertThatThrownBy(() -> service.approveGroupBooking(
                        booking.id(),
                        new AmusementGroupBookingApprovalRequest("group-capacity-approve", "ops approved", List.of())
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SESSION_CAPACITY_LOCK_FAILED");

        assertThat(amusementRepository.findGroupBookingById(booking.id()))
                .get()
                .extracting(AmusementGroupBookingRecord::bookingStatus)
                .isEqualTo("pending_approval");
        assertThat(amusementRepository.findSessionById(session.id()))
                .get()
                .extracting(AmusementSessionRecord::usedCapacity, AmusementSessionRecord::lockedCapacity)
                .containsExactly(1, 0);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.group_booking.approve_failed");
    }

    @Test
    void maintenanceRecordPersistsStructuredFieldsAndKeepsProjectInMaintenance() {
        AmusementSafetyTemplateRecord template = createTemplate("maint-pass");
        AmusementProjectRecord project = createProject("maint-pass", template.id(), "maintenance", 2);

        AmusementMaintenanceRecord record = service.createMaintenanceRecord(new AmusementMaintenanceRecordRequest(
                "maint-pass-create",
                project.id(),
                "device_1",
                "point_1",
                "supplier_maint_1",
                "engineer_1",
                "张工",
                Instant.parse("2026-06-18T02:00:00Z"),
                "pass",
                null,
                "kiosk"
        ));

        assertThat(record)
                .extracting(
                        AmusementMaintenanceRecord::projectId,
                        AmusementMaintenanceRecord::deviceId,
                        AmusementMaintenanceRecord::locationPointId,
                        AmusementMaintenanceRecord::maintenanceVendorId,
                        AmusementMaintenanceRecord::engineerName,
                        AmusementMaintenanceRecord::result,
                        AmusementMaintenanceRecord::sourceTerminal,
                        AmusementMaintenanceRecord::status
                )
                .containsExactly(project.id(), "device_1", "point_1", "supplier_maint_1", "张工", "pass", "kiosk", "pending_confirm");
        // 提交检修不联动 open_status，仍保持 maintenance。
        assertThat(amusementRepository.findProjectById(project.id()))
                .get().extracting(AmusementProjectRecord::openStatus).isEqualTo("maintenance");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.maintenance_recorded");
    }

    @Test
    void confirmResumeRejectsFailedMaintenanceAndKeepsProjectInMaintenance() {
        AmusementSafetyTemplateRecord template = createTemplate("maint-fail");
        AmusementProjectRecord project = createProject("maint-fail", template.id(), "maintenance", 2);

        AmusementMaintenanceRecord record = service.createMaintenanceRecord(new AmusementMaintenanceRecordRequest(
                "maint-fail-create",
                project.id(),
                "device_1",
                "point_1",
                "supplier_maint_1",
                "engineer_1",
                "李工",
                Instant.parse("2026-06-18T02:00:00Z"),
                "fail",
                "限位器异常",
                "kiosk"
        ));

        assertThatThrownBy(() -> service.confirmMaintenanceResume(
                        record.id(),
                        new AmusementMaintenanceResumeRequest("maint-fail-confirm", "supervisor_1", "尝试恢复")
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("MAINTENANCE_NOT_PASSED");

        assertThat(amusementRepository.findProjectById(project.id()))
                .get().extracting(AmusementProjectRecord::openStatus).isEqualTo("maintenance");
        assertThat(amusementRepository.findMaintenanceRecordById(record.id()))
                .get().extracting(AmusementMaintenanceRecord::status).isEqualTo("submitted");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.maintenance_resume_failed");
    }

    @Test
    void confirmResumeRejectsWhenProjectNotInMaintenance() {
        AmusementSafetyTemplateRecord template = createTemplate("maint-open");
        AmusementProjectRecord project = createProject("maint-open", template.id(), "open", 2);

        AmusementMaintenanceRecord record = service.createMaintenanceRecord(new AmusementMaintenanceRecordRequest(
                "maint-open-create",
                project.id(),
                "device_1",
                "point_1",
                "supplier_maint_1",
                "engineer_1",
                "王工",
                Instant.parse("2026-06-18T02:00:00Z"),
                "pass",
                null,
                "kiosk"
        ));

        assertThatThrownBy(() -> service.confirmMaintenanceResume(
                        record.id(),
                        new AmusementMaintenanceResumeRequest("maint-open-confirm", "supervisor_1", "恢复")
                ))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("PROJECT_NOT_IN_MAINTENANCE");

        assertThat(amusementRepository.findProjectById(project.id()))
                .get().extracting(AmusementProjectRecord::openStatus).isEqualTo("open");
    }

    @Test
    void confirmResumeOnPassRestoresProjectOpenStatusAndEmitsResumedEvent() {
        AmusementSafetyTemplateRecord template = createTemplate("maint-resume");
        AmusementProjectRecord project = createProject("maint-resume", template.id(), "maintenance", 2);

        AmusementMaintenanceRecord record = service.createMaintenanceRecord(new AmusementMaintenanceRecordRequest(
                "maint-resume-create",
                project.id(),
                "device_1",
                "point_1",
                "supplier_maint_1",
                "engineer_1",
                "赵工",
                Instant.parse("2026-06-18T02:00:00Z"),
                "pass",
                null,
                "kiosk"
        ));

        AmusementMaintenanceRecord confirmed = service.confirmMaintenanceResume(
                record.id(),
                new AmusementMaintenanceResumeRequest("maint-resume-confirm", "supervisor_1", "检修合格恢复")
        );

        assertThat(confirmed.status()).isEqualTo("confirmed");
        assertThat(confirmed.supervisorConfirmedBy()).isEqualTo("supervisor_1");
        assertThat(confirmed.confirmedAt()).isNotNull();
        assertThat(confirmed.resumeBusinessAt()).isNotNull();
        // 联动 open_status：maintenance → open。
        assertThat(amusementRepository.findProjectById(project.id()))
                .get().extracting(AmusementProjectRecord::openStatus).isEqualTo("open");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.maintenance_recorded", "amusement.project.resumed");
    }

    @Test
    void createMaintenanceRejectsUnknownVendor() {
        AmusementSafetyTemplateRecord template = createTemplate("maint-unknown-vendor");
        AmusementProjectRecord project = createProject("maint-unknown-vendor", template.id(), "maintenance", 2);

        assertThatThrownBy(() -> service.createMaintenanceRecord(new AmusementMaintenanceRecordRequest(
                "maint-unknown-vendor-create",
                project.id(),
                "device_1",
                "point_1",
                "supplier_not_exist",
                "engineer_1",
                "钱工",
                Instant.parse("2026-06-18T02:00:00Z"),
                "pass",
                null,
                "kiosk")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo("MAINTENANCE_VENDOR_NOT_FOUND");

        assertThat(amusementRepository.listMaintenanceRecords()).isEmpty();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("amusement.maintenance_record_failed");
    }

    @Test
    void createMaintenanceRejectsInactiveVendor() {
        AmusementSafetyTemplateRecord template = createTemplate("maint-inactive-vendor");
        AmusementProjectRecord project = createProject("maint-inactive-vendor", template.id(), "maintenance", 2);
        amusementRepository.registerMaintenanceVendor("supplier_archived", "archived");

        assertThatThrownBy(() -> service.createMaintenanceRecord(new AmusementMaintenanceRecordRequest(
                "maint-inactive-vendor-create",
                project.id(),
                "device_1",
                "point_1",
                "supplier_archived",
                "engineer_1",
                "孙工",
                Instant.parse("2026-06-18T02:00:00Z"),
                "pass",
                null,
                "kiosk")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).code())
                .isEqualTo("MAINTENANCE_VENDOR_NOT_AVAILABLE");

        assertThat(amusementRepository.listMaintenanceRecords()).isEmpty();
    }

    @Test
    void createMaintenanceAcceptsRealActiveVendor() {
        AmusementSafetyTemplateRecord template = createTemplate("maint-real-vendor");
        AmusementProjectRecord project = createProject("maint-real-vendor", template.id(), "maintenance", 2);
        amusementRepository.registerMaintenanceVendor("supplier_real_active", "active");

        AmusementMaintenanceRecord record = service.createMaintenanceRecord(new AmusementMaintenanceRecordRequest(
                "maint-real-vendor-create",
                project.id(),
                "device_1",
                "point_1",
                "supplier_real_active",
                "engineer_1",
                "李工",
                Instant.parse("2026-06-18T02:00:00Z"),
                "pass",
                null,
                "kiosk"));

        assertThat(record.maintenanceVendorId()).isEqualTo("supplier_real_active");
        assertThat(record.status()).isEqualTo("pending_confirm");
    }

    private AmusementProjectRecord projectWithTemplate(String key) {
        AmusementSafetyTemplateRecord template = createTemplate(key + "-template");
        return createProject(key, template.id(), "open", 2);
    }

    private AmusementSafetyTemplateRecord createTemplate(String key) {
        return service.createSafetyTemplate(new AmusementSafetyTemplateRequest(
                "template-" + key,
                "create template",
                List.of("att_template"),
                "Daily Opening " + key,
                List.of(Map.of("name", "restraint", "required", true)),
                LocalDate.parse("2026-06-14")
        ));
    }

    private AmusementProjectRecord createProject(String key, String templateId, String status, int capacity) {
        return service.createProject(new AmusementProjectRequest(
                "project-" + key,
                "create project",
                List.of("att_project"),
                "Roller Coaster " + key,
                capacity,
                status,
                templateId,
                Map.of("min_age", 12),
                "point_" + key
        ));
    }

    private AmusementSessionRecord createSession(String key, String projectId, int capacity, String status) {
        return service.createSession(new AmusementSessionRequest(
                "session-" + key,
                "create session",
                List.of("att_session"),
                projectId,
                Instant.parse("2026-06-14T10:00:00Z"),
                Instant.parse("2026-06-14T11:00:00Z"),
                capacity,
                status
        ));
    }

    private AmusementSafetyActionRequest action(String key, String result, String comment) {
        return new AmusementSafetyActionRequest(key, comment, List.of("att_action"), result, comment, List.of("att_action"));
    }

    private void submitPassingInspection(AmusementProjectRecord project, String key) {
        AmusementSafetyInspectionRecord inspection = service.createSafetyInspection(new AmusementSafetyInspectionRequest(
                "inspection-pass-" + key,
                "inspect",
                List.of("att_pass"),
                project.id(),
                null,
                project.safetyTemplateId(),
                List.of(Map.of("name", "restraint", "result", "pass")),
                "pass",
                Instant.parse("2026-06-14T08:00:00Z"),
                "inspector_" + key,
                null
        ));
        service.submitSafetyInspection(inspection.id(), action("submit-pass-" + key, "submit", "submit pass"));
    }

    private VerificationRequest verification(String key, String projectId, String sessionId, String credentialPayload) {
        return new VerificationRequest(
                key,
                "qr",
                credentialPayload,
                "gate_1",
                projectId,
                Map.of("project_id", projectId, "session_id", sessionId, "staff_id", "staff_1")
        );
    }

    /** 清单#6：仅承载超容 over_capacity incident 落库的最小内存 safety 仓储。 */
    private static final class InMemorySafetyRepository implements AmusementSafetyRepository {
        private final Map<String, AmusementIncidentRecord> incidents = new LinkedHashMap<>();

        @Override
        public boolean updateProjectOpenStatus(String projectId, String expectedCurrentStatus, String newStatus) {
            return false; // 该 fake 不跟踪项目状态；事故停运联动在 AmusementSafetyServiceTests 专项验证。
        }

        @Override
        public AmusementIncidentRecord saveIncident(AmusementIncidentRecord record) {
            incidents.put(record.id(), record);
            return record;
        }

        List<AmusementIncidentRecord> incidents() {
            return new ArrayList<>(incidents.values());
        }

        @Override
        public List<AmusementIncidentRecord> findAllIncidents() {
            return incidents();
        }

        @Override
        public List<AmusementIncidentRecord> findIncidentsByMerchant(String merchantId) {
            return incidents.values().stream().filter(i -> merchantId.equals(i.merchantId())).toList();
        }

        @Override
        public Optional<AmusementIncidentRecord> findIncidentById(String id) {
            return Optional.ofNullable(incidents.get(id));
        }

        @Override
        public List<AmusementSafetyNoticeRecord> findAllSafetyNotices() {
            return List.of();
        }

        @Override
        public List<AmusementSafetyNoticeRecord> findSafetyNoticesByMerchant(String merchantId) {
            return List.of();
        }

        @Override
        public Optional<AmusementSafetyNoticeRecord> findSafetyNoticeById(String id) {
            return Optional.empty();
        }

        @Override
        public AmusementSafetyNoticeRecord saveSafetyNotice(AmusementSafetyNoticeRecord record) {
            return record;
        }

        @Override
        public AmusementWaiverAcknowledgementRecord saveWaiverAcknowledgement(AmusementWaiverAcknowledgementRecord record) {
            return record;
        }

        @Override
        public AmusementIncidentActionRecord saveIncidentAction(AmusementIncidentActionRecord record) {
            return record;
        }

        @Override
        public List<AmusementIncidentActionRecord> findIncidentActionsByIncidentId(String incidentId) {
            return List.of();
        }
    }

    private static final class InMemoryAmusementRepository implements AmusementRepository {
        private final Map<String, AmusementSafetyTemplateRecord> safetyTemplates = new LinkedHashMap<>();
        private final Map<String, AmusementProjectRecord> projects = new LinkedHashMap<>();
        private final Map<String, AmusementProjectRuleRecord> projectRules = new LinkedHashMap<>();
        private final Map<String, AmusementSessionRecord> sessions = new LinkedHashMap<>();
        private final Map<String, AmusementSafetyInspectionRecord> inspections = new LinkedHashMap<>();
        private final Map<String, AmusementVerificationRecord> verifications = new LinkedHashMap<>();
        private final Map<String, AmusementManualOverrideRecord> manualOverrides = new LinkedHashMap<>();
        private final Map<String, AmusementDeviceStatusRecord> deviceStatuses = new LinkedHashMap<>();
        private final Map<String, AmusementQueueEntryRecord> queueEntries = new LinkedHashMap<>();
        private final Map<String, AmusementReservationRecord> reservations = new LinkedHashMap<>();
        private final Map<String, AmusementGroupBookingRecord> groupBookings = new LinkedHashMap<>();
        private final Map<String, AmusementMaintenanceRecord> maintenanceRecords = new LinkedHashMap<>();
        // 供应链供应商主数据（inventory_suppliers）镜像：vendorId → status（检修单位存在性/可用性校验）。
        private final Map<String, String> maintenanceVendors = new LinkedHashMap<>();
        private final List<AmusementSafetyActionRecord> safetyActions = new ArrayList<>();
        // V341（R-12 软删）：绑定行是唯一真源，存档/删除只改标记不移除；生效绑定由 active() 派生。
        private final Map<String, AmusementProjectEntitlementRecord> projectEntitlements = new LinkedHashMap<>();
        private final Map<String, AmusementCapacityConfigRecord> capacityConfigs = new LinkedHashMap<>();
        // §6.4 三维度容量：点位/设备维容量配置（key=projectId|pointId|deviceId）与维度快照在场计数。
        private final Map<String, Integer> pointDeviceCapacityConfig = new LinkedHashMap<>();
        private final Map<String, Integer> snapshotOccupied = new LinkedHashMap<>();

        void bindProjectEntitlementType(String projectId, String entitlementType) {
            projectEntitlements.put(projectId + "|" + entitlementType, new AmusementProjectEntitlementRecord(
                    "ament_" + projectId + "_" + entitlementType, projectId, entitlementType, Instant.now()));
        }

        /** 测试夹具：为项目的点位/设备维配置最大并发上限（落地点位·设备维度受控）。 */
        void configurePointDeviceCapacity(String projectId, String pointId, String deviceId, int maxConcurrent) {
            pointDeviceCapacityConfig.put(pointDeviceKey(projectId, pointId, deviceId), maxConcurrent);
        }

        private String pointDeviceKey(String projectId, String pointId, String deviceId) {
            return projectId + "|" + (pointId == null ? "" : pointId) + "|" + (deviceId == null ? "" : deviceId);
        }

        @Override
        public AmusementSafetyTemplateRecord saveSafetyTemplate(AmusementSafetyTemplateRecord template) {
            safetyTemplates.put(template.id(), template);
            return template;
        }

        @Override
        public List<AmusementSafetyTemplateRecord> listSafetyTemplates() {
            return new ArrayList<>(safetyTemplates.values());
        }

        @Override
        public Optional<AmusementSafetyTemplateRecord> findSafetyTemplateById(String id) {
            return Optional.ofNullable(safetyTemplates.get(id));
        }

        @Override
        public AmusementProjectRecord saveProject(AmusementProjectRecord project) {
            projects.put(project.id(), project);
            return project;
        }

        @Override
        public List<AmusementProjectRecord> listProjects() {
            return new ArrayList<>(projects.values());
        }

        @Override
        public List<String> listProjectEntitlementTypes(String projectId) {
            // 核销绑定校验只认生效绑定（已存档/墓碑视同不存在）。
            return projectEntitlements.values().stream()
                    .filter(record -> projectId.equals(record.projectId()))
                    .filter(AmusementProjectEntitlementRecord::active)
                    .map(AmusementProjectEntitlementRecord::entitlementType)
                    .sorted()
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }

        @Override
        public AmusementProjectEntitlementRecord saveProjectEntitlement(AmusementProjectEntitlementRecord entitlement) {
            String key = entitlement.projectId() + "|" + entitlement.entitlementType();
            AmusementProjectEntitlementRecord existing = projectEntitlements.get(key);
            // 复绑=恢复：命中既有行时保留原 id/created_at，清空存档/删除标记（对齐 JDBC 的 ON CONFLICT DO UPDATE）。
            AmusementProjectEntitlementRecord saved = existing == null
                    ? entitlement
                    : new AmusementProjectEntitlementRecord(existing.id(), existing.projectId(),
                            existing.entitlementType(), existing.createdAt());
            projectEntitlements.put(key, saved);
            return saved;
        }

        @Override
        public List<AmusementProjectEntitlementRecord> listProjectEntitlements(String projectId, boolean includeArchived) {
            return projectEntitlements.values().stream()
                    .filter(record -> projectId.equals(record.projectId()))
                    .filter(record -> includeArchived || record.active())
                    .sorted(java.util.Comparator.comparing(AmusementProjectEntitlementRecord::entitlementType))
                    .toList();
        }

        @Override
        public Optional<AmusementProjectEntitlementRecord> findProjectEntitlement(String projectId, String entitlementType) {
            return Optional.ofNullable(projectEntitlements.get(projectId + "|" + entitlementType));
        }

        @Override
        public int archiveProjectEntitlement(String projectId, String entitlementType, String reason, String actorId) {
            String key = projectId + "|" + entitlementType;
            AmusementProjectEntitlementRecord existing = projectEntitlements.get(key);
            if (existing == null || existing.archivedAt() != null || !"none".equals(existing.deleteStatus())) {
                return 0;
            }
            projectEntitlements.put(key, new AmusementProjectEntitlementRecord(
                    existing.id(), existing.projectId(), existing.entitlementType(), existing.createdAt(),
                    Instant.now(), actorId, reason,
                    existing.deleteStatus(), existing.deleteRequestedBy(), existing.deleteRequestedAt(), existing.deleteReason()));
            return 1;
        }

        @Override
        public int restoreProjectEntitlement(String projectId, String entitlementType) {
            String key = projectId + "|" + entitlementType;
            AmusementProjectEntitlementRecord existing = projectEntitlements.get(key);
            if (existing == null) {
                return 0;
            }
            projectEntitlements.put(key, new AmusementProjectEntitlementRecord(
                    existing.id(), existing.projectId(), existing.entitlementType(), existing.createdAt()));
            return 1;
        }

        @Override
        public int requestDeleteProjectEntitlement(String projectId, String entitlementType, String reason, String actorId) {
            String key = projectId + "|" + entitlementType;
            AmusementProjectEntitlementRecord existing = projectEntitlements.get(key);
            if (existing == null || existing.archivedAt() == null || !"none".equals(existing.deleteStatus())) {
                return 0;
            }
            projectEntitlements.put(key, new AmusementProjectEntitlementRecord(
                    existing.id(), existing.projectId(), existing.entitlementType(), existing.createdAt(),
                    existing.archivedAt(), existing.archivedBy(), existing.archiveReason(),
                    "pending", actorId, Instant.now(), reason));
            return 1;
        }

        @Override
        public int decideDeleteProjectEntitlement(String projectId, String entitlementType, boolean approved) {
            String key = projectId + "|" + entitlementType;
            AmusementProjectEntitlementRecord existing = projectEntitlements.get(key);
            if (existing == null || !"pending".equals(existing.deleteStatus())) {
                return 0;
            }
            projectEntitlements.put(key, new AmusementProjectEntitlementRecord(
                    existing.id(), existing.projectId(), existing.entitlementType(), existing.createdAt(),
                    existing.archivedAt(), existing.archivedBy(), existing.archiveReason(),
                    approved ? "approved" : "none",
                    existing.deleteRequestedBy(), existing.deleteRequestedAt(), existing.deleteReason()));
            return 1;
        }

        @Override
        public AmusementCapacityConfigRecord saveCapacityConfig(AmusementCapacityConfigRecord config) {
            capacityConfigs.put(config.id(), config);
            // 录入后即生效：启用的点位/设备配置同步到 claim 受控夹具；停用则移除。
            String key = pointDeviceKey(config.projectId(), config.locationPointId(), config.deviceId());
            if (config.enabled()) {
                pointDeviceCapacityConfig.put(key, config.maxConcurrent());
            } else {
                pointDeviceCapacityConfig.remove(key);
            }
            return config;
        }

        @Override
        public List<AmusementCapacityConfigRecord> listCapacityConfigs(String projectId) {
            return capacityConfigs.values().stream()
                    .filter(record -> projectId.equals(record.projectId()))
                    .toList();
        }

        @Override
        public Optional<AmusementCapacityConfigRecord> findCapacityConfigById(String id) {
            return Optional.ofNullable(capacityConfigs.get(id));
        }

        // V143 设备主数据内存桩（amusement_devices）。
        private final Map<String, AmusementDeviceRecord> devices = new LinkedHashMap<>();

        @Override
        public AmusementDeviceRecord saveDevice(AmusementDeviceRecord device) {
            devices.put(device.id(), device);
            return device;
        }

        @Override
        public List<AmusementDeviceRecord> listDevicesByProject(String projectId) {
            return devices.values().stream()
                    .filter(record -> projectId.equals(record.projectId()))
                    .toList();
        }

        @Override
        public Optional<AmusementDeviceRecord> findDeviceById(String id) {
            return Optional.ofNullable(devices.get(id));
        }

        @Override
        public Optional<AmusementDeviceRecord> findDeviceByCode(String projectId, String deviceCode) {
            return devices.values().stream()
                    .filter(d -> projectId.equals(d.projectId()) && deviceCode.equals(d.deviceCode()))
                    .findFirst();
        }

        @Override
        public void updateDeviceLastPassInspection(String projectId, String deviceCode, java.time.Instant inspectedAt) {
            devices.values().stream()
                    .filter(d -> projectId.equals(d.projectId()) && deviceCode.equals(d.deviceCode()))
                    .findFirst()
                    .ifPresent(d -> devices.put(d.id(), new AmusementDeviceRecord(
                            d.id(), d.projectId(), d.deviceCode(), d.name(), d.status(),
                            d.safetyCheckIntervalHours(), d.locationPointId(), inspectedAt,
                            d.merchantId(), d.createdAt(), inspectedAt)));
        }

        @Override
        public int findProjectSafetyCheckIntervalHours(String projectId) {
            return 24;
        }

        @Override
        public void updateProjectDevicesLastPass(String projectId, java.time.Instant inspectedAt) {
            devices.values().stream()
                    .filter(d -> projectId.equals(d.projectId()))
                    .toList()
                    .forEach(d -> devices.put(d.id(), new AmusementDeviceRecord(
                            d.id(), d.projectId(), d.deviceCode(), d.name(), d.status(),
                            d.safetyCheckIntervalHours(), d.locationPointId(), inspectedAt,
                            d.merchantId(), d.createdAt(), inspectedAt)));
        }

        // T3.1：巡检到期工单（amusement_inspection_tasks）内存桩。
        private final Map<String, AmusementInspectionTaskRecord> inspectionTasks = new LinkedHashMap<>();

        @Override
        public void upsertInspectionTask(AmusementInspectionTaskRecord task) {
            // 自然键 (project, device|'', due_at) 去重，命中不覆盖（对齐 DB ON CONFLICT DO NOTHING）。
            String key = task.projectId() + "|" + (task.deviceId() == null ? "" : task.deviceId())
                    + "|" + task.dueAt().toEpochMilli();
            inspectionTasks.putIfAbsent(key, task);
        }

        @Override
        public int markOverdueInspectionTasks(java.time.Instant now) {
            int n = 0;
            for (Map.Entry<String, AmusementInspectionTaskRecord> e : inspectionTasks.entrySet()) {
                AmusementInspectionTaskRecord t = e.getValue();
                if ("pending".equals(t.status()) && t.dueAt().isBefore(now)) {
                    e.setValue(new AmusementInspectionTaskRecord(t.id(), t.projectId(), t.deviceId(), t.dueAt(),
                            "overdue", t.generatedAt(), t.completedInspectionId(), t.completedAt(),
                            t.merchantId(), t.createdAt(), now));
                    n++;
                }
            }
            return n;
        }

        @Override
        public List<AmusementInspectionTaskRecord> listInspectionTasks(java.util.Optional<String> merchantScope) {
            return inspectionTasks.values().stream()
                    .filter(t -> merchantScope.isEmpty() || merchantScope.get().equals(t.merchantId()))
                    .sorted(java.util.Comparator.comparing(AmusementInspectionTaskRecord::dueAt))
                    .collect(java.util.stream.Collectors.toList());
        }

        List<AmusementInspectionTaskRecord> allInspectionTasks() {
            return new ArrayList<>(inspectionTasks.values());
        }

        // 清单#6：超容处置策略夹具（运营容量软上限 + 处置模式）。
        private final Map<String, AmusementOverCapacityPolicy> overCapacityPolicies = new LinkedHashMap<>();

        void configureOverCapacityPolicy(String projectId, int operatingCapacity, String disposalMode) {
            overCapacityPolicies.put(projectId,
                    new AmusementOverCapacityPolicy(projectId, operatingCapacity, disposalMode, true));
        }

        @Override
        public AmusementOverCapacityPolicy saveOverCapacityPolicy(AmusementOverCapacityPolicy policy) {
            // 幂等 upsert：按 projectId 覆盖既有行（与 ON CONFLICT(project_id) DO UPDATE 同语义）。
            overCapacityPolicies.put(policy.projectId(), policy);
            return policy;
        }

        @Override
        public AmusementOverCapacityPolicy findOverCapacityPolicy(String projectId) {
            return overCapacityPolicies.getOrDefault(projectId, AmusementOverCapacityPolicy.disabled(projectId));
        }

        @Override
        public Optional<AmusementProjectRecord> findProjectById(String id) {
            return Optional.ofNullable(projects.get(id));
        }

        @Override
        public Optional<String> findProjectIdByPointOrDevice(String pointId, String deviceId) {
            // 内存夹具：设备优先按 device_code / location_point_id 命中，其次按项目入口点位。
            for (AmusementDeviceRecord device : devices.values()) {
                if ((deviceId != null && deviceId.equals(device.deviceCode()))
                        || (pointId != null && pointId.equals(device.locationPointId()))) {
                    return Optional.of(device.projectId());
                }
            }
            if (pointId == null) {
                return Optional.empty();
            }
            return projects.values().stream()
                    .filter(p -> pointId.equals(p.locationPointId()))
                    .map(AmusementProjectRecord::id)
                    .findFirst();
        }

        @Override
        public Optional<AmusementSessionRecord> resolveOpenSessionForProject(String projectId, java.time.Instant now) {
            return sessions.values().stream()
                    .filter(s -> projectId.equals(s.projectId()))
                    .filter(s -> "open".equals(s.status()))
                    .filter(s -> !now.isBefore(s.startTime()) && !now.isAfter(s.endTime()))
                    .reduce((a, b) -> b.startTime().isAfter(a.startTime()) ? b : a);
        }

        // 处理清单 #8：内存版空间映射（spaceId -> merchantId；空串表示无商户/共享空间）。
        private final Map<String, String> spaceMerchants = new LinkedHashMap<>();
        private final Map<String, String> projectSpaceIds = new LinkedHashMap<>();

        void registerSpace(String spaceId, String merchantId) {
            spaceMerchants.put(spaceId, merchantId == null ? "" : merchantId);
        }

        String projectSpaceId(String projectId) {
            return projectSpaceIds.get(projectId);
        }

        @Override
        public int assignProjectSpace(String projectId, String spaceId) {
            if (!projects.containsKey(projectId)) {
                return 0;
            }
            projectSpaceIds.put(projectId, spaceId);
            return 1;
        }

        @Override
        public Optional<String> findSpaceMerchant(String spaceId) {
            return Optional.ofNullable(spaceMerchants.get(spaceId));
        }

        @Override
        public List<AmusementProjectSpaceBacklogRecord> listProjectsWithoutSpace(Optional<String> merchantScope) {
            return projects.values().stream()
                    .filter(p -> projectSpaceIds.get(p.id()) == null)
                    .filter(p -> merchantScope.isEmpty() || merchantScope.get().equals(p.merchantId()))
                    .map(p -> new AmusementProjectSpaceBacklogRecord(
                            p.id(), p.projectName(), p.openStatus(), p.merchantId()))
                    .toList();
        }

        @Override
        public List<AmusementProjectDeviceRegistryBacklogRecord> listProjectsMissingDeviceRegistry(Optional<String> merchantScope) {
            return projects.values().stream()
                    .filter(p -> devices.values().stream()
                            .noneMatch(d -> p.id().equals(d.projectId()) && "active".equals(d.status())))
                    .filter(p -> merchantScope.isEmpty() || merchantScope.get().equals(p.merchantId()))
                    .map(p -> new AmusementProjectDeviceRegistryBacklogRecord(
                            p.id(), p.projectName(), p.openStatus(), p.merchantId()))
                    .toList();
        }

        // REQ-53 权益会话内存夹具（V129 entitlement_sessions）。
        private final Map<String, EntitlementSessionRecord> entitlementSessions = new LinkedHashMap<>();

        /** 测试夹具：直接写入一条会话（可注入已过期 expires_at / 任意状态，用于过期/重复领/取消用例）。 */
        void putEntitlementSession(EntitlementSessionRecord session) {
            entitlementSessions.put(session.id(), session);
        }

        @Override
        public EntitlementSessionRecord saveEntitlementSession(EntitlementSessionRecord session) {
            entitlementSessions.put(session.id(), session);
            return session;
        }

        @Override
        public Optional<EntitlementSessionRecord> findEntitlementSessionById(String id) {
            return Optional.ofNullable(entitlementSessions.get(id));
        }

        @Override
        public Optional<EntitlementSessionRecord> findEntitlementSessionByCode(String sessionCode) {
            return entitlementSessions.values().stream()
                    .filter(s -> sessionCode.equals(s.sessionCode()))
                    .findFirst();
        }

        @Override
        public List<EntitlementSessionRecord> listEntitlementSessionsByMerchant(String merchantId) {
            return entitlementSessions.values().stream()
                    .filter(s -> merchantId.equals(s.merchantId()))
                    .toList();
        }

        @Override
        public List<EntitlementSessionRecord> listEntitlementSessions() {
            return new ArrayList<>(entitlementSessions.values());
        }

        @Override
        public boolean claimEntitlementSession(String id, String claimedBy, Instant now) {
            EntitlementSessionRecord s = entitlementSessions.get(id);
            // 防错领条件 UPDATE 等价语义：仅 initiated/displayed 且未过期可领，受影响 1 行才成功。
            if (s == null
                    || !(s.status().equals("initiated") || s.status().equals("displayed"))
                    || s.expiresAt() == null || !now.isBefore(s.expiresAt())) {
                return false;
            }
            entitlementSessions.put(id, new EntitlementSessionRecord(
                    s.id(), s.sessionCode(), s.entitlementRef(), s.merchantId(), s.projectId(),
                    "claimed", s.expiresAt(), s.initiatedBy(), claimedBy, now, s.createdAt(), now));
            return true;
        }

        @Override
        public boolean markEntitlementSessionDisplayed(String id, Instant now) {
            EntitlementSessionRecord s = entitlementSessions.get(id);
            if (s == null || !"initiated".equals(s.status())) {
                return false;
            }
            entitlementSessions.put(id, new EntitlementSessionRecord(
                    s.id(), s.sessionCode(), s.entitlementRef(), s.merchantId(), s.projectId(),
                    "displayed", s.expiresAt(), s.initiatedBy(), s.claimedBy(), s.claimedAt(),
                    s.createdAt(), now));
            return true;
        }

        @Override
        public AmusementProjectRuleRecord saveProjectRule(AmusementProjectRuleRecord rule) {
            projectRules.put(rule.id(), rule);
            return rule;
        }

        @Override
        public List<AmusementProjectRuleRecord> listActiveBlockingProjectRules(String projectId) {
            return projectRules.values().stream()
                    .filter(rule -> projectId.equals(rule.projectId()))
                    .filter(AmusementProjectRuleRecord::blocking)
                    .filter(rule -> "active".equals(rule.status()))
                    .toList();
        }

        @Override
        public AmusementSessionRecord saveSession(AmusementSessionRecord session) {
            sessions.put(session.id(), session);
            return session;
        }

        @Override
        public List<AmusementSessionRecord> listSessions() {
            return new ArrayList<>(sessions.values());
        }

        @Override
        public Optional<AmusementSessionRecord> findSessionById(String id) {
            return Optional.ofNullable(sessions.get(id));
        }

        @Override
        public boolean incrementSessionUsedCapacityIfAvailable(String sessionId) {
            AmusementSessionRecord session = sessions.get(sessionId);
            if (session == null || session.usedCapacity() + session.lockedCapacity() >= session.capacity()) {
                return false;
            }
            Instant now = Instant.parse("2026-06-14T12:00:00Z");
            sessions.put(sessionId, new AmusementSessionRecord(
                    session.id(),
                    session.projectId(),
                    session.startTime(),
                    session.endTime(),
                    session.capacity(),
                    session.usedCapacity() + 1,
                    session.lockedCapacity(),
                    session.status(),
                    session.evidenceAttachmentIds(),
                    session.createdAt(),
                    now
            ));
            return true;
        }

        @Override
        public CapacityClaimResult claimCapacityAcrossDimensions(CapacityClaimContext context) {
            // 与 Jdbc 实现同口径：场次 → 点位/设备 → 项目安全上限逐维原子 +1，任一失败回滚已增维度。
            List<String> controlled = new ArrayList<>();
            List<String> incremented = new ArrayList<>();

            int peopleCount = Math.max(context.peopleCount(), 1);
            controlled.add("session");
            if (!incrementSessionUsedCapacityIfAvailable(context.sessionId(), peopleCount)) {
                return new CapacityClaimResult(false, "session", controlled);
            }
            incremented.add("session");

            Integer pointDeviceLimit = pointDeviceCapacityConfig.get(
                    pointDeviceKey(context.projectId(), context.locationPointId(), context.deviceId()));
            if (pointDeviceLimit != null) {
                controlled.add("point_device");
                String key = "pd|" + pointDeviceKey(context.projectId(), context.locationPointId(), context.deviceId());
                if (snapshotOccupied.getOrDefault(key, 0) + peopleCount > pointDeviceLimit) {
                    rollback(incremented, context, peopleCount);
                    return new CapacityClaimResult(false, "point_device", controlled);
                }
                snapshotOccupied.merge(key, peopleCount, Integer::sum);
                incremented.add("point_device");
            }

            if (context.projectSafetyCapacity() > 0) {
                controlled.add("project_safety");
                String key = "proj|" + context.projectId();
                if (snapshotOccupied.getOrDefault(key, 0) + peopleCount > context.projectSafetyCapacity()) {
                    rollback(incremented, context, peopleCount);
                    return new CapacityClaimResult(false, "project_safety", controlled);
                }
                snapshotOccupied.merge(key, peopleCount, Integer::sum);
                incremented.add("project_safety");
            }

            return new CapacityClaimResult(true, null, controlled);
        }

        private boolean incrementSessionUsedCapacityIfAvailable(String sessionId, int peopleCount) {
            AmusementSessionRecord session = sessions.get(sessionId);
            if (session == null || peopleCount <= 0 || session.usedCapacity() + session.lockedCapacity() + peopleCount > session.capacity()) {
                return false;
            }
            Instant now = Instant.parse("2026-06-14T12:00:00Z");
            sessions.put(sessionId, new AmusementSessionRecord(
                    session.id(), session.projectId(), session.startTime(), session.endTime(), session.capacity(),
                    session.usedCapacity() + peopleCount, session.lockedCapacity(), session.status(),
                    session.evidenceAttachmentIds(), session.createdAt(), now));
            return true;
        }

        private void rollback(List<String> incremented, CapacityClaimContext context, int peopleCount) {
            for (String dimension : incremented) {
                switch (dimension) {
                    case "session" -> {
                        AmusementSessionRecord s = sessions.get(context.sessionId());
                        if (s != null) {
                            sessions.put(s.id(), new AmusementSessionRecord(
                                    s.id(), s.projectId(), s.startTime(), s.endTime(), s.capacity(),
                                    Math.max(s.usedCapacity() - 1, 0), s.lockedCapacity(), s.status(),
                                    s.evidenceAttachmentIds(), s.createdAt(), s.updatedAt()));
                        }
                    }
                    case "point_device" -> snapshotOccupied.computeIfPresent(
                            "pd|" + pointDeviceKey(context.projectId(), context.locationPointId(), context.deviceId()),
                            (k, v) -> Math.max(v - 1, 0));
                    case "project_safety" -> snapshotOccupied.computeIfPresent(
                            "proj|" + context.projectId(), (k, v) -> Math.max(v - 1, 0));
                    default -> { }
                }
            }
        }

        @Override
        public boolean lockSessionCapacityIfAvailable(String sessionId, int capacityToLock) {
            AmusementSessionRecord session = sessions.get(sessionId);
            if (session == null || capacityToLock <= 0 || session.usedCapacity() + session.lockedCapacity() + capacityToLock > session.capacity()) {
                return false;
            }
            Instant now = Instant.parse("2026-06-14T12:00:00Z");
            sessions.put(sessionId, new AmusementSessionRecord(
                    session.id(),
                    session.projectId(),
                    session.startTime(),
                    session.endTime(),
                    session.capacity(),
                    session.usedCapacity(),
                    session.lockedCapacity() + capacityToLock,
                    session.status(),
                    session.evidenceAttachmentIds(),
                    session.createdAt(),
                    now
            ));
            return true;
        }

        @Override
        public void releaseSessionLockedCapacity(String sessionId, int capacityToRelease) {
            AmusementSessionRecord session = sessions.get(sessionId);
            if (session == null || capacityToRelease <= 0) {
                return;
            }
            Instant now = Instant.parse("2026-06-14T12:00:00Z");
            sessions.put(sessionId, new AmusementSessionRecord(
                    session.id(),
                    session.projectId(),
                    session.startTime(),
                    session.endTime(),
                    session.capacity(),
                    session.usedCapacity(),
                    Math.max(session.lockedCapacity() - capacityToRelease, 0),
                    session.status(),
                    session.evidenceAttachmentIds(),
                    session.createdAt(),
                    now
            ));
        }

        @Override
        public AmusementGroupBookingRecord saveGroupBooking(AmusementGroupBookingRecord booking) {
            groupBookings.put(booking.id(), booking);
            return booking;
        }

        @Override
        public List<AmusementGroupBookingRecord> listGroupBookings() {
            return new ArrayList<>(groupBookings.values());
        }

        @Override
        public com.complex.platform.common.api.PageResponse<AmusementGroupBookingRecord> listGroupBookingsPaged(
                String merchantId, int page, int size) {
            List<AmusementGroupBookingRecord> all = listGroupBookings();
            int safePage = Math.max(1, page);
            int safeSize = Math.min(Math.max(1, size), 200);
            int from = Math.min((safePage - 1) * safeSize, all.size());
            int to = Math.min(from + safeSize, all.size());
            return new com.complex.platform.common.api.PageResponse<>(all.subList(from, to), safePage, safeSize, all.size());
        }

        @Override
        public Optional<AmusementGroupBookingRecord> findGroupBookingById(String id) {
            return Optional.ofNullable(groupBookings.get(id));
        }

        @Override
        public Optional<AmusementGroupBookingRecord> approveGroupBookingWithCapacityLock(
                String id,
                String approvalNote,
                List<String> evidenceAttachmentIds,
                Instant approvedAt
        ) {
            AmusementGroupBookingRecord existing = groupBookings.get(id);
            if (existing == null || !"pending_approval".equals(existing.bookingStatus())) {
                return Optional.empty();
            }
            if (!lockSessionCapacityIfAvailable(existing.sessionId(), existing.reservedCapacity())) {
                return Optional.empty();
            }
            AmusementGroupBookingRecord approved = new AmusementGroupBookingRecord(
                    existing.id(),
                    existing.projectId(),
                    existing.sessionId(),
                    existing.bookingType(),
                    existing.customerName(),
                    existing.contactPhone(),
                    existing.partySize(),
                    existing.reservedCapacity(),
                    "approved",
                    approvalNote,
                    existing.operatorNote(),
                    evidenceAttachmentIds,
                    approvedAt,
                    existing.releasedAt(),
                    existing.createdAt(),
                    approvedAt,
                    // V231：与真实 SQL 一致——审批确认即预定确认落点，paid_at=approved_at。
                    approvedAt
            );
            groupBookings.put(id, approved);
            return Optional.of(approved);
        }

        @Override
        public Optional<AmusementGroupBookingRecord> releaseGroupBookingAndCapacity(
                String id,
                String operatorNote,
                List<String> evidenceAttachmentIds,
                Instant releasedAt
        ) {
            AmusementGroupBookingRecord existing = groupBookings.get(id);
            if (existing == null || !"approved".equals(existing.bookingStatus())) {
                return Optional.empty();
            }
            releaseSessionLockedCapacity(existing.sessionId(), existing.reservedCapacity());
            AmusementGroupBookingRecord released = new AmusementGroupBookingRecord(
                    existing.id(),
                    existing.projectId(),
                    existing.sessionId(),
                    existing.bookingType(),
                    existing.customerName(),
                    existing.contactPhone(),
                    existing.partySize(),
                    existing.reservedCapacity(),
                    "released",
                    existing.approvalNote(),
                    operatorNote,
                    evidenceAttachmentIds,
                    existing.approvedAt(),
                    releasedAt,
                    existing.createdAt(),
                    releasedAt
            );
            groupBookings.put(id, released);
            return Optional.of(released);
        }

        @Override
        public AmusementSafetyInspectionRecord saveSafetyInspection(AmusementSafetyInspectionRecord inspection) {
            inspections.put(inspection.id(), inspection);
            return inspection;
        }

        @Override
        public List<AmusementSafetyInspectionRecord> listSafetyInspections() {
            return new ArrayList<>(inspections.values());
        }

        @Override
        public Optional<AmusementSafetyInspectionRecord> findSafetyInspectionById(String id) {
            return Optional.ofNullable(inspections.get(id));
        }

        @Override
        public Optional<AmusementSafetyInspectionRecord> latestSafetyInspectionForProject(String projectId) {
            return inspections.values().stream()
                    .filter(inspection -> projectId.equals(inspection.projectId()))
                    .reduce((first, second) -> second);
        }

        @Override
        public AmusementSafetyActionRecord saveSafetyAction(AmusementSafetyActionRecord action) {
            safetyActions.add(action);
            return action;
        }

        @Override
        public AmusementVerificationRecord saveVerification(AmusementVerificationRecord verification) {
            verifications.put(verification.id(), verification);
            return verification;
        }

        @Override
        public java.util.Optional<AmusementVerificationRecord> findActiveEntryByEntitlement(String entitlementId) {
            return verifications.values().stream()
                    .filter(v -> "entry".equals(v.direction()) && "success".equals(v.verificationResult())
                            && v.releasedAt() == null && entitlementId != null
                            && entitlementId.equals(v.entitlementId()))
                    .reduce((a, b) -> b);
        }

        @Override
        public java.util.Optional<AmusementVerificationRecord> findExitByEntry(String entryVerificationId) {
            return verifications.values().stream()
                    .filter(v -> "exit".equals(v.direction()) && entryVerificationId != null
                            && entryVerificationId.equals(v.entryVerificationId()))
                    .reduce((a, b) -> b);
        }

        @Override
        public java.util.Optional<AmusementVerificationRecord> findLatestExitByEntitlement(String entitlementId) {
            return verifications.values().stream()
                    .filter(v -> "exit".equals(v.direction()) && entitlementId != null
                            && entitlementId.equals(v.entitlementId()))
                    .reduce((a, b) -> b);
        }

        @Override
        public boolean releaseEntryConcurrentCapacity(
                AmusementVerificationRecord entry, AmusementVerificationRecord exitRecord, java.time.Instant now) {
            AmusementVerificationRecord current = verifications.get(entry.id());
            if (current == null || !"entry".equals(current.direction()) || current.releasedAt() != null) {
                return false;
            }
            verifications.put(current.id(), new AmusementVerificationRecord(
                    current.id(), current.projectId(), current.sessionId(), current.credentialPayload(),
                    current.deviceId(), current.staffId(), current.verificationResult(), current.failureReason(),
                    current.evidenceAttachmentIds(), current.createdAt(), current.verifiedPeople(),
                    current.direction(), now, current.entryVerificationId(),
                    current.entitlementId(), current.locationPointId()));
            saveVerification(exitRecord);
            return true;
        }

        @Override
        public int clearProjectConcurrentCapacity(String projectId, java.time.Instant now) {
            return 0;
        }

        @Override
        public List<AmusementVerificationRecord> listVerifications() {
            return new ArrayList<>(verifications.values());
        }

        @Override
        public AmusementManualOverrideRecord saveManualOverride(AmusementManualOverrideRecord override) {
            manualOverrides.put(override.id(), override);
            return override;
        }

        @Override
        public List<AmusementManualOverrideRecord> listManualOverrides() {
            return new ArrayList<>(manualOverrides.values());
        }

        @Override
        public AmusementDeviceStatusRecord saveDeviceStatus(AmusementDeviceStatusRecord status) {
            deviceStatuses.put(status.id(), status);
            return status;
        }

        @Override
        public List<AmusementDeviceStatusRecord> listDeviceStatuses() {
            return new ArrayList<>(deviceStatuses.values());
        }

        @Override
        public AmusementQueueEntryRecord saveQueueEntry(AmusementQueueEntryRecord entry) {
            queueEntries.put(entry.id(), entry);
            return entry;
        }

        @Override
        public List<AmusementQueueEntryRecord> listQueueEntries() {
            return new ArrayList<>(queueEntries.values());
        }

        @Override
        public com.complex.platform.common.api.PageResponse<AmusementQueueEntryRecord> listQueueEntriesPaged(
                String merchantId, int page, int size) {
            List<AmusementQueueEntryRecord> all = listQueueEntries();
            int safePage = Math.max(1, page);
            int safeSize = Math.min(Math.max(1, size), 200);
            int from = Math.min((safePage - 1) * safeSize, all.size());
            int to = Math.min(from + safeSize, all.size());
            return new com.complex.platform.common.api.PageResponse<>(all.subList(from, to), safePage, safeSize, all.size());
        }

        @Override
        public Optional<AmusementQueueEntryRecord> findQueueEntryById(String id) {
            return Optional.ofNullable(queueEntries.get(id));
        }

        @Override
        public AmusementReservationRecord saveReservation(AmusementReservationRecord reservation) {
            reservations.put(reservation.id(), reservation);
            return reservation;
        }

        @Override
        public List<AmusementReservationRecord> listReservations() {
            return new ArrayList<>(reservations.values());
        }

        @Override
        public com.complex.platform.common.api.PageResponse<AmusementReservationRecord> listReservationsPaged(
                String merchantId, int page, int size) {
            List<AmusementReservationRecord> all = listReservations();
            int safePage = Math.max(1, page);
            int safeSize = Math.min(Math.max(1, size), 200);
            int from = Math.min((safePage - 1) * safeSize, all.size());
            int to = Math.min(from + safeSize, all.size());
            return new com.complex.platform.common.api.PageResponse<>(all.subList(from, to), safePage, safeSize, all.size());
        }

        @Override
        public Optional<AmusementReservationRecord> findReservationById(String id) {
            return Optional.ofNullable(reservations.get(id));
        }

        @Override
        public List<AmusementSafetyTemplateRecord> listSafetyTemplatesByMerchant(String merchantId) {
            return safetyTemplates.values().stream()
                    .filter(t -> merchantId.equals(t.merchantId()))
                    .toList();
        }

        @Override
        public List<AmusementProjectRecord> listProjectsByMerchant(String merchantId) {
            return projects.values().stream()
                    .filter(p -> merchantId.equals(p.merchantId()))
                    .toList();
        }

        @Override
        public List<AmusementSessionRecord> listSessionsByMerchant(String merchantId) {
            return sessions.values().stream()
                    .filter(s -> projects.containsKey(s.projectId()) && merchantId.equals(projects.get(s.projectId()).merchantId()))
                    .toList();
        }

        @Override
        public List<AmusementGroupBookingRecord> listGroupBookingsByMerchant(String merchantId) {
            return groupBookings.values().stream()
                    .filter(b -> projects.containsKey(b.projectId()) && merchantId.equals(projects.get(b.projectId()).merchantId()))
                    .toList();
        }

        @Override
        public List<AmusementSafetyInspectionRecord> listSafetyInspectionsByMerchant(String merchantId) {
            return inspections.values().stream()
                    .filter(i -> projects.containsKey(i.projectId()) && merchantId.equals(projects.get(i.projectId()).merchantId()))
                    .toList();
        }

        @Override
        public List<AmusementVerificationRecord> listVerificationsByMerchant(String merchantId) {
            return verifications.values().stream()
                    .filter(v -> projects.containsKey(v.projectId()) && merchantId.equals(projects.get(v.projectId()).merchantId()))
                    .toList();
        }

        @Override
        public List<AmusementVerificationListItem> listVerificationItems(AmusementVerificationQuery query, String merchantId) {
            return verifications.values().stream()
                    .filter(v -> merchantId == null
                            || (projects.containsKey(v.projectId()) && merchantId.equals(projects.get(v.projectId()).merchantId())))
                    .filter(v -> query.projectId() == null || query.projectId().equals(v.projectId()))
                    .filter(v -> query.sessionId() == null || query.sessionId().equals(v.sessionId()))
                    .filter(v -> query.direction() == null || query.direction().equals(v.direction()))
                    .map(v -> new AmusementVerificationListItem(
                            v.id(), v.projectId(), v.sessionId(), v.deviceId(), v.staffId(),
                            v.verificationResult(), v.verifiedPeople(), v.direction(), v.createdAt(),
                            v.releasedAt(), v.entryVerificationId(), v.entitlementId(), v.locationPointId(), null))
                    .toList();
        }

        @Override
        public List<AmusementCapacitySnapshotRecord> listCapacitySnapshots(String merchantId) {
            return List.of();
        }

        @Override
        public List<AmusementManualOverrideRecord> listManualOverridesByMerchant(String merchantId) {
            return manualOverrides.values().stream()
                    .filter(o -> projects.containsKey(o.projectId()) && merchantId.equals(projects.get(o.projectId()).merchantId()))
                    .toList();
        }

        @Override
        public List<AmusementDeviceStatusRecord> listDeviceStatusesByMerchant(String merchantId) {
            return deviceStatuses.values().stream()
                    .filter(d -> projects.containsKey(d.projectId()) && merchantId.equals(projects.get(d.projectId()).merchantId()))
                    .toList();
        }

        @Override
        public List<AmusementQueueEntryRecord> listQueueEntriesByMerchant(String merchantId) {
            return queueEntries.values().stream()
                    .filter(q -> projects.containsKey(q.projectId()) && merchantId.equals(projects.get(q.projectId()).merchantId()))
                    .toList();
        }

        @Override
        public List<AmusementReservationRecord> listReservationsByMerchant(String merchantId) {
            return reservations.values().stream()
                    .filter(r -> projects.containsKey(r.projectId()) && merchantId.equals(projects.get(r.projectId()).merchantId()))
                    .toList();
        }

        @Override
        public AmusementMaintenanceRecord saveMaintenanceRecord(AmusementMaintenanceRecord record) {
            maintenanceRecords.put(record.id(), record);
            return record;
        }

        @Override
        public List<AmusementMaintenanceRecord> listMaintenanceRecords() {
            return new ArrayList<>(maintenanceRecords.values());
        }

        @Override
        public List<AmusementMaintenanceRecord> listMaintenanceRecordsByMerchant(String merchantId) {
            return maintenanceRecords.values().stream()
                    .filter(m -> projects.containsKey(m.projectId()) && merchantId.equals(projects.get(m.projectId()).merchantId()))
                    .toList();
        }

        @Override
        public Optional<AmusementMaintenanceRecord> findMaintenanceRecordById(String id) {
            return Optional.ofNullable(maintenanceRecords.get(id));
        }

        /** 测试夹具：在供应商主表登记一个检修单位（vendorId → status）。 */
        void registerMaintenanceVendor(String vendorId, String status) {
            maintenanceVendors.put(vendorId, status);
        }

        @Override
        public Optional<String> findMaintenanceVendorStatus(String vendorId) {
            return Optional.ofNullable(maintenanceVendors.get(vendorId));
        }

        @Override
        public boolean updateProjectOpenStatus(String projectId, String expectedCurrentStatus, String newStatus) {
            AmusementProjectRecord project = projects.get(projectId);
            if (project == null || !expectedCurrentStatus.equals(project.openStatus())) {
                return false;
            }
            projects.put(projectId, new AmusementProjectRecord(
                    project.id(),
                    project.projectName(),
                    project.capacity(),
                    newStatus,
                    project.safetyTemplateId(),
                    project.eligibilityRule(),
                    project.locationPointId(),
                    project.createdAt(),
                    Instant.now(),
                    project.merchantId()));
            return true;
        }
    }

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

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

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }
    }

    /** 内存版游乐权益端口：按 credentialPayload 索引票务权益，支持消费/回补，用于核销校验测试。 */
    private static final class InMemoryAmusementEntitlementGateway implements AmusementEntitlementGateway {
        private final Map<String, FakeEntitlement> byCredential = new LinkedHashMap<>();
        private final Map<String, FakeEntitlement> byId = new LinkedHashMap<>();

        void register(String credentialPayload, String entitlementType, String status,
                      Instant validFrom, Instant validTo, int totalTimes) {
            register(credentialPayload, entitlementType, status, validFrom, validTo, totalTimes, 1);
        }

        void register(String credentialPayload, String entitlementType, String status,
                      Instant validFrom, Instant validTo, int totalTimes, int numPeople) {
            FakeEntitlement entitlement = new FakeEntitlement(
                    "ent_" + UUID.randomUUID(), entitlementType, status, validFrom, validTo, totalTimes, numPeople);
            byCredential.put(credentialPayload, entitlement);
            byId.put(entitlement.id, entitlement);
        }

        @Override
        public Optional<AmusementEntitlementView> findEntitlementByCredential(String credentialPayload) {
            FakeEntitlement entitlement = byCredential.get(credentialPayload);
            if (entitlement == null) {
                return Optional.empty();
            }
            return Optional.of(new AmusementEntitlementView(
                    entitlement.id, entitlement.entitlementType, entitlement.status,
                    entitlement.validFrom, entitlement.validTo, entitlement.totalTimes - entitlement.usedTimes,
                    entitlement.numPeople));
        }

        @Override
        public boolean consumeEntitlement(String entitlementId) {
            FakeEntitlement entitlement = byId.get(entitlementId);
            if (entitlement == null || entitlement.usedTimes >= entitlement.totalTimes) {
                return false;
            }
            entitlement.usedTimes++;
            return true;
        }

        @Override
        public void reverseEntitlement(String entitlementId) {
            FakeEntitlement entitlement = byId.get(entitlementId);
            if (entitlement != null && entitlement.usedTimes > 0) {
                entitlement.usedTimes--;
            }
        }

        int usedTimes(String credentialPayload) {
            FakeEntitlement entitlement = byCredential.get(credentialPayload);
            return entitlement == null ? -1 : entitlement.usedTimes;
        }

        private static final class FakeEntitlement {
            private final String id;
            private final String entitlementType;
            private final String status;
            private final Instant validFrom;
            private final Instant validTo;
            private final int totalTimes;
            private final int numPeople;
            private int usedTimes;

            private FakeEntitlement(String id, String entitlementType, String status,
                                    Instant validFrom, Instant validTo, int totalTimes, int numPeople) {
                this.id = id;
                this.entitlementType = entitlementType;
                this.status = status;
                this.validFrom = validFrom;
                this.validTo = validTo;
                this.totalTimes = totalTimes;
                this.numPeople = numPeople;
            }
        }
    }
}
