package com.complex.platform.amusement;

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
    private RiskAlertService riskAlertService;

    @BeforeEach
    void setUp() {
        amusementRepository = new InMemoryAmusementRepository();
        // 检修单位须为供应商主表真实记录：默认登记一个 active 供应商供既有检修用例使用。
        amusementRepository.registerMaintenanceVendor("supplier_maint_1", "active");
        auditEventRepository = new InMemoryAuditEventRepository();
        entitlementGateway = new InMemoryAmusementEntitlementGateway();
        safetyRepository = new InMemorySafetyRepository();
        authorizationService = new AuthorizationService(mock(AccessRepository.class));
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
        entitlementGateway.register(credentialPayload, "amusement_single", "active", null, null, totalTimes);
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
                List.of(),
                project.id(),
                project.safetyTemplateId(),
                List.of(Map.of("name", "restraint", "result", "pass")),
                "pass",
                Instant.parse("2026-06-14T08:00:00Z"),
                "inspector_pass",
                null
        ));

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
    void warnOnlyDisposalModeDoesNotBlockHardOverCapacityButStillRecordsIncidentAndEvent() {
        // 处置模式 warn_only：硬上限耗尽亦不阻断（留痕放行），但必落 over_capacity incident + 发事件。
        AmusementProjectRecord project = createProject("oc-warn", createTemplate("oc-warn-tpl").id(), "open", 1);
        submitPassingInspection(project, "oc-warn");
        AmusementSessionRecord session = createSession("oc-warn-session", project.id(), 100, "open");
        amusementRepository.configureOverCapacityPolicy(project.id(), 1, AmusementOverCapacityPolicy.MODE_WARN_ONLY);
        registerAmusementEntitlement("credential-oc-warn-1", 1);
        registerAmusementEntitlement("credential-oc-warn-2", 1);

        // 第一次占满硬上限（1/1）成功。
        assertThat(service.createVerification(verification("v-oc-warn-1", project.id(), session.id(), "credential-oc-warn-1"))
                .verificationResult()).isEqualTo("success");

        // 第二次达硬上限：warn_only 不阻断，仍放行（留痕），且不回补权益。
        assertThat(service.createVerification(verification("v-oc-warn-2", project.id(), session.id(), "credential-oc-warn-2"))
                .verificationResult()).isEqualTo("success");
        assertThat(entitlementGateway.usedTimes("credential-oc-warn-2")).isEqualTo(1);

        // 两次（第一次软超容达运营容量 + 第二次硬超容留痕）均落 over_capacity 事件。
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
        service.deleteProjectEntitlement(project.id(), "amusement_pass", "ent-rm-del-key");
        assertThat(service.listProjectEntitlements(project.id()).items()).isEmpty();
        registerAmusementEntitlement("credential-ent-rm", 1);

        // 解绑后无任何绑定 → 回退按游乐类放行（向后兼容）。
        assertThat(service.createVerification(verification("v-ent-rm", project.id(), session.id(), "credential-ent-rm"))
                .verificationResult()).isEqualTo("success");
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
                List.of(),
                project.id(),
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
        private final Map<String, List<String>> projectEntitlementTypes = new LinkedHashMap<>();
        private final Map<String, AmusementProjectEntitlementRecord> projectEntitlements = new LinkedHashMap<>();
        private final Map<String, AmusementCapacityConfigRecord> capacityConfigs = new LinkedHashMap<>();
        // §6.4 三维度容量：点位/设备维容量配置（key=projectId|pointId|deviceId）与维度快照在场计数。
        private final Map<String, Integer> pointDeviceCapacityConfig = new LinkedHashMap<>();
        private final Map<String, Integer> snapshotOccupied = new LinkedHashMap<>();

        void bindProjectEntitlementType(String projectId, String entitlementType) {
            projectEntitlementTypes.computeIfAbsent(projectId, key -> new ArrayList<>()).add(entitlementType);
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
            return new ArrayList<>(projectEntitlementTypes.getOrDefault(projectId, List.of()));
        }

        @Override
        public AmusementProjectEntitlementRecord saveProjectEntitlement(AmusementProjectEntitlementRecord entitlement) {
            List<String> types = projectEntitlementTypes.computeIfAbsent(entitlement.projectId(), key -> new ArrayList<>());
            if (!types.contains(entitlement.entitlementType())) {
                types.add(entitlement.entitlementType());
            }
            projectEntitlements.put(entitlement.projectId() + "|" + entitlement.entitlementType(), entitlement);
            return entitlement;
        }

        @Override
        public List<AmusementProjectEntitlementRecord> listProjectEntitlements(String projectId) {
            return projectEntitlements.values().stream()
                    .filter(record -> projectId.equals(record.projectId()))
                    .sorted(java.util.Comparator.comparing(AmusementProjectEntitlementRecord::entitlementType))
                    .toList();
        }

        @Override
        public boolean deleteProjectEntitlement(String projectId, String entitlementType) {
            boolean removed = projectEntitlements.remove(projectId + "|" + entitlementType) != null;
            List<String> types = projectEntitlementTypes.get(projectId);
            if (types != null) {
                types.remove(entitlementType);
            }
            return removed;
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

        // 清单#6：超容处置策略夹具（运营容量软上限 + 处置模式）。
        private final Map<String, AmusementOverCapacityPolicy> overCapacityPolicies = new LinkedHashMap<>();

        void configureOverCapacityPolicy(String projectId, int operatingCapacity, String disposalMode) {
            overCapacityPolicies.put(projectId,
                    new AmusementOverCapacityPolicy(projectId, operatingCapacity, disposalMode, true));
        }

        @Override
        public AmusementOverCapacityPolicy findOverCapacityPolicy(String projectId) {
            return overCapacityPolicies.getOrDefault(projectId, AmusementOverCapacityPolicy.disabled(projectId));
        }

        @Override
        public Optional<AmusementProjectRecord> findProjectById(String id) {
            return Optional.ofNullable(projects.get(id));
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

            controlled.add("session");
            if (!incrementSessionUsedCapacityIfAvailable(context.sessionId())) {
                return new CapacityClaimResult(false, "session", controlled);
            }
            incremented.add("session");

            Integer pointDeviceLimit = pointDeviceCapacityConfig.get(
                    pointDeviceKey(context.projectId(), context.locationPointId(), context.deviceId()));
            if (pointDeviceLimit != null) {
                controlled.add("point_device");
                String key = "pd|" + pointDeviceKey(context.projectId(), context.locationPointId(), context.deviceId());
                if (snapshotOccupied.getOrDefault(key, 0) >= pointDeviceLimit) {
                    rollback(incremented, context);
                    return new CapacityClaimResult(false, "point_device", controlled);
                }
                snapshotOccupied.merge(key, 1, Integer::sum);
                incremented.add("point_device");
            }

            if (context.projectSafetyCapacity() > 0) {
                controlled.add("project_safety");
                String key = "proj|" + context.projectId();
                if (snapshotOccupied.getOrDefault(key, 0) >= context.projectSafetyCapacity()) {
                    rollback(incremented, context);
                    return new CapacityClaimResult(false, "project_safety", controlled);
                }
                snapshotOccupied.merge(key, 1, Integer::sum);
                incremented.add("project_safety");
            }

            return new CapacityClaimResult(true, null, controlled);
        }

        private void rollback(List<String> incremented, CapacityClaimContext context) {
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

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }

    }

    /** 内存版游乐权益端口：按 credentialPayload 索引票务权益，支持消费/回补，用于核销校验测试。 */
    private static final class InMemoryAmusementEntitlementGateway implements AmusementEntitlementGateway {
        private final Map<String, FakeEntitlement> byCredential = new LinkedHashMap<>();
        private final Map<String, FakeEntitlement> byId = new LinkedHashMap<>();

        void register(String credentialPayload, String entitlementType, String status,
                      Instant validFrom, Instant validTo, int totalTimes) {
            FakeEntitlement entitlement = new FakeEntitlement(
                    "ent_" + UUID.randomUUID(), entitlementType, status, validFrom, validTo, totalTimes);
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
                    entitlement.validFrom, entitlement.validTo, entitlement.totalTimes - entitlement.usedTimes));
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
            private int usedTimes;

            private FakeEntitlement(String id, String entitlementType, String status,
                                    Instant validFrom, Instant validTo, int totalTimes) {
                this.id = id;
                this.entitlementType = entitlementType;
                this.status = status;
                this.validFrom = validFrom;
                this.validTo = validTo;
                this.totalTimes = totalTimes;
            }
        }
    }
}
