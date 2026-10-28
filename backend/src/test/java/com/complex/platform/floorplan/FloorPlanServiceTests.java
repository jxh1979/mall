package com.complex.platform.floorplan;
// 文件版本：V0.06

import com.complex.platform.support.InMemoryAuditEventRepository;
import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.floorplan.dto.FloorPlanCreateRequest;
import com.complex.platform.floorplan.dto.FloorPlanImportCreateRequest;
import com.complex.platform.floorplan.dto.FloorPlanImportPublishRequest;
import com.complex.platform.floorplan.dto.FloorPlanShapeSaveRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FloorPlanServiceTests {

    private InMemoryFloorPlanRepository repository;
    private InMemoryAuditEventRepository auditEventRepository;
    private FloorPlanService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryFloorPlanRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        service = new FloorPlanService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                new FloorPlanImportParser(),
                noScopeAuth()
        );
    }

    @Test
    void createFloorPlanRejectsInvalidDimensionsAndWritesFailureAudit() {
        assertThatThrownBy(() -> service.createFloorPlan(new FloorPlanCreateRequest(
                "floor-plan-bad-size",
                "bad size",
                "prj_1",
                "A",
                "1F",
                "1F plan",
                "https://assets.example.com/floor-1.png",
                0,
                1200,
                "v1",
                "active"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("floor_plan.create_failed");
    }

    @Test
    void createFloorPlanPersistsRecordAndAuditEvent() {
        FloorPlanRecord created = service.createFloorPlan(createRequest("floor-plan-create"));

        assertThat(created.id()).startsWith("fp_");
        assertThat(created.floorNo()).isEqualTo("1F");
        assertThat(created.status()).isEqualTo("active");
        assertThat(repository.floorPlanSaveCount).isEqualTo(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("floor_plan.created");
    }

    @Test
    void saveShapeRejectsMissingFloorPlan() {
        assertThatThrownBy(() -> service.saveShape("fp_missing", shapeRequest("shape-missing-plan", null, polygon())))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("FLOOR_PLAN_NOT_FOUND"));
    }

    @Test
    void saveShapeRequiresAtLeastThreePolygonPoints() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("floor-plan-shape-validation"));

        assertThatThrownBy(() -> service.saveShape(floorPlan.id(), shapeRequest(
                "shape-two-points",
                null,
                List.of(new FloorPlanPoint(0.1, 0.1), new FloorPlanPoint(0.2, 0.2))
        ))).isInstanceOfSatisfying(ApiException.class, exception -> {
            assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
            // M017-006：M015-006 已改的中文整句
            assertThat(exception.getMessage()).isEqualTo("多边形至少需要 3 个顶点");
        });
    }

    @Test
    void saveShapeRejectsOutOfRangeNormalizedPoint() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("floor-plan-out-of-range"));

        assertThatThrownBy(() -> service.saveShape(floorPlan.id(), shapeRequest(
                "shape-out-of-range",
                null,
                List.of(new FloorPlanPoint(0.1, 0.1), new FloorPlanPoint(1.2, 0.2), new FloorPlanPoint(0.3, 0.3))
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void saveShapePersistsPolygonAndAuditEvent() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("floor-plan-shape-save"));

        FloorPlanShapeRecord shape = service.saveShape(floorPlan.id(), shapeRequest("shape-save", null, polygon()));

        assertThat(shape.id()).startsWith("fps_");
        assertThat(shape.floorPlanId()).isEqualTo(floorPlan.id());
        assertThat(shape.polygonPoints()).hasSize(4);
        assertThat(shape.shapeType()).isEqualTo("shop");
        assertThat(repository.findShapesByFloorPlanId(floorPlan.id())).hasSize(1);
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("floor_plan.shape_saved");
    }

    @Test
    void duplicateShapeIdempotencyKeyWithSameRequestReturnsFirstResult() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("floor-plan-shape-once"));
        FloorPlanShapeSaveRequest request = shapeRequest("shape-once", null, polygon());

        FloorPlanShapeRecord first = service.saveShape(floorPlan.id(), request);
        FloorPlanShapeRecord second = service.saveShape(floorPlan.id(), request);

        assertThat(second).isEqualTo(first);
        assertThat(repository.shapeSaveCount).isEqualTo(1);
    }

    @Test
    void duplicateFloorPlanIdempotencyKeyWithDifferentRequestReturnsConflict() {
        service.createFloorPlan(createRequest("floor-plan-conflict"));

        assertThatThrownBy(() -> service.createFloorPlan(new FloorPlanCreateRequest(
                "floor-plan-conflict",
                "different request",
                "prj_1",
                "A",
                "2F",
                "2F plan",
                "https://assets.example.com/floor-2.png",
                1600,
                1200,
                "v1",
                "active"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void validationFailureDoesNotReserveIdempotencyKey() {
        assertThatThrownBy(() -> service.createFloorPlan(new FloorPlanCreateRequest(
                "floor-plan-validation-retry",
                "bad size",
                "prj_1",
                "A",
                "1F",
                "1F plan",
                "https://assets.example.com/floor-1.png",
                -1,
                1200,
                "v1",
                "active"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));

        FloorPlanRecord retry = service.createFloorPlan(createRequest("floor-plan-validation-retry"));

        assertThat(retry.status()).isEqualTo("active");
    }

    @Test
    void createImportTaskPersistsCandidatesAndAuditEvent() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("floor-plan-import-create-plan"));

        FloorPlanImportTaskRecord task = service.createImportTask(floorPlan.id(), importRequest("floor-import-create"));

        assertThat(task.id()).startsWith("fpi_");
        assertThat(task.floorPlanId()).isEqualTo(floorPlan.id());
        assertThat(task.status()).isEqualTo("parsed");
        assertThat(task.candidateCount()).isEqualTo(1);
        assertThat(task.errorCount()).isZero();
        assertThat(repository.findImportCandidatesByTaskId(task.id()))
                .extracting(FloorPlanImportCandidateRecord::label)
                .containsExactly("A101");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("floor_plan.import_created");
    }

    @Test
    void createImportTaskRejectsMissingFloorPlanAndWritesFailureAudit() {
        assertThatThrownBy(() -> service.createImportTask("fp_missing", importRequest("floor-import-missing")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("FLOOR_PLAN_NOT_FOUND"));

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("floor_plan.import_create_failed");
    }

    @Test
    void duplicateImportTaskIdempotencyKeyWithSameRequestReturnsFirstResult() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("floor-plan-import-idempotency"));
        FloorPlanImportCreateRequest request = importRequest("floor-import-idempotency");

        FloorPlanImportTaskRecord first = service.createImportTask(floorPlan.id(), request);
        FloorPlanImportTaskRecord second = service.createImportTask(floorPlan.id(), request);

        assertThat(second).isEqualTo(first);
        assertThat(repository.importTaskSaveCount).isEqualTo(1);
    }

    @Test
    void publishImportTaskCreatesShapesAndMarksTaskPublished() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("floor-plan-import-publish"));
        FloorPlanImportTaskRecord task = service.createImportTask(floorPlan.id(), importRequest("floor-import-publish"));
        String candidateId = repository.findImportCandidatesByTaskId(task.id()).getFirst().id();

        FloorPlanImportTaskRecord published = service.publishImportTask(
                floorPlan.id(),
                task.id(),
                new FloorPlanImportPublishRequest(
                        "floor-import-publish-once",
                        "publish import",
                        List.of(candidateId)
                )
        );

        assertThat(published.status()).isEqualTo("published");
        assertThat(repository.findShapesByFloorPlanId(floorPlan.id()))
                .extracting(FloorPlanShapeRecord::label)
                .containsExactly("A101");
        assertThat(repository.findImportCandidatesByTaskId(task.id()).getFirst().status()).isEqualTo("published");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("floor_plan.import_published");
    }

    @Test
    void publishImportTaskRejectsAlreadyPublishedTask() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("floor-plan-import-republish"));
        FloorPlanImportTaskRecord task = service.createImportTask(floorPlan.id(), importRequest("floor-import-republish"));
        String candidateId = repository.findImportCandidatesByTaskId(task.id()).getFirst().id();
        service.publishImportTask(
                floorPlan.id(),
                task.id(),
                new FloorPlanImportPublishRequest("floor-import-publish-first", "publish import", List.of(candidateId))
        );

        assertThatThrownBy(() -> service.publishImportTask(
                floorPlan.id(),
                task.id(),
                new FloorPlanImportPublishRequest("floor-import-publish-second", "publish import again", List.of(candidateId))
        )).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
    }

    // M015-016：人工复核只发部分候选后，剩余待处理候选仍可继续发布（修复前第二次发布被「只有解析完成的导入任务才能发布」拒绝）
    @Test
    void publishRemainingPendingCandidatesAfterPartialPublish() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-publish-partial"));
        FloorPlanImportTaskRecord task = service.createImportTask(floorPlan.id(), importRequest("fp-import-partial", """
                <svg viewBox="0 0 1000 800">
                  <polygon id="A101" data-shape-type="shop" data-ref-type="space" data-ref-id="sp_101" points="100,80 300,80 300,240 100,240" />
                  <polygon id="A102" data-shape-type="booth" data-ref-type="space" data-ref-id="sp_102" points="400,80 600,80 600,240 400,240" />
                </svg>
                """));
        List<String> candidateIds = repository.findImportCandidatesByTaskId(task.id()).stream()
                .map(FloorPlanImportCandidateRecord::id)
                .toList();
        assertThat(candidateIds).hasSize(2);

        FloorPlanImportTaskRecord first = service.publishImportTask(floorPlan.id(), task.id(),
                new FloorPlanImportPublishRequest("publish-partial-1", "publish first", List.of(candidateIds.get(0))));
        assertThat(first.status()).isEqualTo("published");

        FloorPlanImportTaskRecord second = service.publishImportTask(floorPlan.id(), task.id(),
                new FloorPlanImportPublishRequest("publish-partial-2", "publish rest", List.of(candidateIds.get(1))));

        assertThat(second.status()).isEqualTo("published");
        assertThat(repository.findShapesByFloorPlanId(floorPlan.id()))
                .extracting(FloorPlanShapeRecord::label)
                .containsExactlyInAnyOrder("A101", "A102");
        assertThat(repository.findImportCandidatesByTaskId(task.id()))
                .extracting(FloorPlanImportCandidateRecord::status)
                .containsOnly("published");
    }

    // P1-2: saveShape 未校验 shapeId 归属 — 修复前此测试会红（无归属校验时覆写成功）
    @Test
    void saveShapeRejectsShapeIdBelongingToDifferentFloorPlan() {
        FloorPlanRecord planA = service.createFloorPlan(createRequest("fp-cross-a"));
        FloorPlanRecord planB = service.createFloorPlan(createRequest("fp-cross-b"));
        FloorPlanShapeRecord shapeOnA = service.saveShape(planA.id(), shapeRequest("shape-cross-plan", null, polygon()));

        assertThatThrownBy(() -> service.saveShape(planB.id(), shapeRequest("shape-cross-attempt", shapeOnA.id(), polygon())))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
    }

    // P1-1: 多租户范围过滤 — 修复前此测试会红（无 scope 过滤时两个商户的数据互相可见）
    @Test
    void listFloorPlansFiltersToMerchantScope() {
        FloorPlanService serviceM1 = serviceWithMerchantScope("m_1");
        FloorPlanService serviceM2 = serviceWithMerchantScope("m_2");
        serviceM1.createFloorPlan(createRequest("fp-scope-m1-only"));
        serviceM2.createFloorPlan(createRequest("fp-scope-m2-only"));

        PageResponse<FloorPlanRecord> resultM1 = serviceM1.listFloorPlans();
        PageResponse<FloorPlanRecord> resultM2 = serviceM2.listFloorPlans();
        PageResponse<FloorPlanRecord> resultAdmin = service.listFloorPlans();

        assertThat(resultM1.items()).extracting(FloorPlanRecord::merchantId).containsOnly("m_1");
        assertThat(resultM2.items()).extracting(FloorPlanRecord::merchantId).containsOnly("m_2");
        assertThat(resultAdmin.items()).hasSize(2);
    }

    // P1-1: 跨商户访问已有平面图应返回 CROSS_MERCHANT_FORBIDDEN
    @Test
    void accessingFloorPlanBelongingToDifferentMerchantIsForbidden() {
        FloorPlanService serviceM1 = serviceWithMerchantScope("m_1");
        FloorPlanRecord planM1 = serviceM1.createFloorPlan(createRequest("fp-access-m1"));

        FloorPlanService serviceM2 = serviceWithMerchantScope("m_2");
        assertThatThrownBy(() -> serviceM2.listShapes(planM1.id()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
    }

    // P0: same idempotency key from two merchants must not collide on PK and overwrite each other's plan
    @Test
    void createFloorPlanWithSameKeyAcrossMerchantsDoesNotOverwrite() {
        FloorPlanService serviceM1 = serviceWithMerchantScope("m_1");
        FloorPlanRecord planM1 = serviceM1.createFloorPlan(createRequest("shared-create-key"));

        FloorPlanService serviceM2 = serviceWithMerchantScope("m_2");
        FloorPlanRecord planM2 = serviceM2.createFloorPlan(createRequest("shared-create-key"));

        assertThat(planM2.id()).isNotEqualTo(planM1.id());
        assertThat(repository.findFloorPlanById(planM1.id()))
                .get().extracting(FloorPlanRecord::merchantId).isEqualTo("m_1");
        assertThat(repository.findFloorPlanById(planM2.id()))
                .get().extracting(FloorPlanRecord::merchantId).isEqualTo("m_2");
    }

    // M015-001: 图形/导入任务主键须含平面图域——跨平面图同一客户端幂等键不得覆盖他图数据（修复前此两例红）
    @Test
    void saveShapeWithSameKeyAcrossFloorPlansDoesNotOverwrite() {
        FloorPlanRecord planA = service.createFloorPlan(createRequest("fp-shape-key-a"));
        FloorPlanRecord planB = service.createFloorPlan(createRequest("fp-shape-key-b"));
        FloorPlanShapeRecord shapeOnA = service.saveShape(planA.id(), shapeRequest("shared-shape-key", null, polygon()));

        FloorPlanShapeRecord shapeOnB = service.saveShape(planB.id(), shapeRequest("shared-shape-key", null, polygon()));

        assertThat(shapeOnB.id()).isNotEqualTo(shapeOnA.id());
        assertThat(repository.findShapeById(shapeOnA.id())).get()
                .extracting(FloorPlanShapeRecord::floorPlanId).isEqualTo(planA.id());
        assertThat(repository.findShapesByFloorPlanId(planA.id())).hasSize(1);
        assertThat(repository.findShapesByFloorPlanId(planB.id())).hasSize(1);
    }

    @Test
    void createImportTaskWithSameKeyAcrossFloorPlansDoesNotOverwrite() {
        FloorPlanRecord planA = service.createFloorPlan(createRequest("fp-import-key-a"));
        FloorPlanRecord planB = service.createFloorPlan(createRequest("fp-import-key-b"));
        FloorPlanImportTaskRecord taskOnA = service.createImportTask(planA.id(), importRequest("shared-import-key"));

        FloorPlanImportTaskRecord taskOnB = service.createImportTask(planB.id(), importRequest("shared-import-key"));

        assertThat(taskOnB.id()).isNotEqualTo(taskOnA.id());
        assertThat(repository.findImportTaskById(taskOnA.id())).get()
                .extracting(FloorPlanImportTaskRecord::floorPlanId).isEqualTo(planA.id());
        assertThat(repository.findImportCandidatesByTaskId(taskOnA.id())).hasSize(1);
        assertThat(repository.findImportCandidatesByTaskId(taskOnB.id())).hasSize(1);
    }

    // M015-001（r2）：派生主键必须落入 V023/V034 的 id VARCHAR(64)——现网前端键 fpi-<13 位时间戳> 与 UUID 键都要能落库（修复前候选 id 68、UUID 图形 id 80 → 此例红）
    @Test
    void derivedIdsFitVarchar64Columns() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-varchar64"));
        FloorPlanImportTaskRecord task = service.createImportTask(floorPlan.id(), importRequest("fpi-1757570000000"));
        List<String> candidateIds = repository.findImportCandidatesByTaskId(task.id()).stream()
                .map(FloorPlanImportCandidateRecord::id).toList();
        service.publishImportTask(floorPlan.id(), task.id(),
                new FloorPlanImportPublishRequest("publish-varchar64", "publish import", candidateIds));
        FloorPlanShapeRecord shape = service.saveShape(floorPlan.id(),
                shapeRequest("6f1d2c3b-4a5e-4f60-9b7c-8d9e0f1a2b3c", null, polygon()));

        assertThat(task.id()).hasSizeLessThanOrEqualTo(64);
        assertThat(candidateIds).allSatisfy(id -> assertThat(id).hasSizeLessThanOrEqualTo(64));
        assertThat(repository.findShapesByFloorPlanId(floorPlan.id()))
                .extracting(FloorPlanShapeRecord::id)
                .allSatisfy(id -> assertThat(id).hasSizeLessThanOrEqualTo(64));
        assertThat(shape.id()).hasSizeLessThanOrEqualTo(64);
    }

    // M015-007：列表不分页时 pageSize/total 必须等于真实条数（修复前 pageSize 谎报 50 → 此例红）
    @Test
    void listShapesReportsHonestPageSizeAndTotal() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-honest-page"));
        service.saveShape(floorPlan.id(), shapeRequest("shape-page-1", null, polygon()));
        service.saveShape(floorPlan.id(), shapeRequest("shape-page-2", null, polygon()));

        PageResponse<FloorPlanShapeRecord> page = service.listShapes(floorPlan.id());

        assertThat(page.items()).hasSize(2);
        assertThat(page.total()).isEqualTo(2);
        assertThat(page.pageSize()).isEqualTo(2);
    }

    // M015-002: 平面图必须带综合体项目归属且项目须存在（PGM-017/PGM-022；修复前此两例红）
    @Test
    void createFloorPlanRequiresProjectId() {
        assertThatThrownBy(() -> service.createFloorPlan(createRequest("floor-plan-no-project", " ")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
        assertThat(repository.floorPlanSaveCount).isZero();
    }

    @Test
    void createFloorPlanRejectsUnknownProject() {
        assertThatThrownBy(() -> service.createFloorPlan(createRequest("floor-plan-unknown-project", "prj_missing")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PROJECT_NOT_FOUND"));
        assertThat(repository.floorPlanSaveCount).isZero();
    }

    // M015-003: 项目员工按项目 scope 隔离（BEQR-152）；修复前此三例红
    @Test
    void listFloorPlansFiltersToProjectScope() {
        service.createFloorPlan(createRequest("fp-project-scope-1", "prj_1"));
        service.createFloorPlan(createRequest("fp-project-scope-2", "prj_2"));

        PageResponse<FloorPlanRecord> scoped = serviceWithProjectScope("prj_1").listFloorPlans();

        assertThat(scoped.items()).extracting(FloorPlanRecord::projectId).containsOnly("prj_1");
        assertThat(service.listFloorPlans().items()).hasSize(2);
    }

    @Test
    void accessingFloorPlanBelongingToDifferentProjectIsForbidden() {
        FloorPlanRecord planOnProject2 = service.createFloorPlan(createRequest("fp-project-access-2", "prj_2"));

        assertThatThrownBy(() -> serviceWithProjectScope("prj_1").listShapes(planOnProject2.id()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
    }

    @Test
    void createFloorPlanOutsideProjectScopeIsForbidden() {
        assertThatThrownBy(() -> serviceWithProjectScope("prj_1").createFloorPlan(createRequest("fp-project-create-2", "prj_2")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("CROSS_PROJECT_FORBIDDEN"));
        assertThat(repository.floorPlanSaveCount).isZero();
    }

    // M015-015：无 organizationId 的平台管理员全量可达（修复前真 currentProjectScopeQuietly 抛 PROJECT_SCOPE_REQUIRED，本例与 setUp 用 service 的各例一起红）
    @Test
    void platformAdminWithoutOrganizationSeesAllFloorPlans() {
        service.createFloorPlan(createRequest("fp-admin-all-1", "prj_1"));
        service.createFloorPlan(createRequest("fp-admin-all-2", "prj_2"));

        assertThat(service.listFloorPlans().items())
                .extracting(FloorPlanRecord::projectId)
                .containsExactlyInAnyOrder("prj_1", "prj_2");
    }

    // M015-015：无项目归属的普通员工 fail-closed（红线④：不因缺归属放行全量）
    @Test
    void staffWithoutProjectScopeIsRejected() {
        FloorPlanRecord plan = service.createFloorPlan(createRequest("fp-orphan-1", "prj_1"));
        FloorPlanService orphan = new FloorPlanService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                new FloorPlanImportParser(),
                orphanStaffAuth()
        );

        assertThatThrownBy(orphan::listFloorPlans)
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PROJECT_SCOPE_REQUIRED"));
        assertThatThrownBy(() -> orphan.listShapes(plan.id()))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("PROJECT_SCOPE_REQUIRED"));
    }

    // M015-015：平台管理员（生产语义：只替换 currentActor，currentMerchantIdQuietly / currentProjectScopeQuietly / isPlatformAdmin 走真实现——
    // 无 organizationId 的平台管理员调 currentProjectScopeQuietly 会抛 PROJECT_SCOPE_REQUIRED，服务须经 isPlatformAdmin 判全量）
    private AuthorizationService noScopeAuth() {
        return actorAuth(new AccessActor("usr_platform_admin", "platform-admin", "platform_admin", null, null, "active"));
    }

    // M015-015：无项目归属的普通员工（生产语义：currentProjectScopeQuietly 抛 PROJECT_SCOPE_REQUIRED，fail-closed）
    private AuthorizationService orphanStaffAuth() {
        return actorAuth(new AccessActor("usr_orphan", "orphan", "staff", null, null, "active"));
    }

    private static AuthorizationService actorAuth(AccessActor actor) {
        return new AuthorizationService(null) {
            @Override
            public AccessActor currentActor() {
                return actor;
            }
            // 18E（第 19 条 19-R2）：服务改按入口权限码可触及的项目判；本类不挂可跨项目角色，只到本人项目（与现行等价）
            @Override
            public ProjectReach projectReach(AccessActor reachActor, String... permissionCodes) {
                return isPlatformAdmin(reachActor) ? super.projectReach(reachActor, permissionCodes)
                        : ProjectReach.ownOnly(requireProjectScope(reachActor));
            }
        };
    }

    private FloorPlanService serviceWithProjectScope(String projectId) {
        return new FloorPlanService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                new FloorPlanImportParser(),
                // 项目员工（生产语义：仅替换 currentActor；无 merchantId → currentMerchantScope 抛、currentMerchantIdQuietly 空；organizationId=项目 → 项目 scope 有值）
                actorAuth(new AccessActor("usr_" + projectId, projectId + "-staff", "staff", projectId, null, "active"))
        );
    }

    private FloorPlanService serviceWithMerchantScope(String merchantId) {
        return new FloorPlanService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                new FloorPlanImportParser(),
                // 商户员工（生产语义：仅替换 currentActor；有 merchantId、无 organizationId → currentProjectScopeQuietly 抛 PROJECT_SCOPE_REQUIRED）
                actorAuth(new AccessActor("usr_" + merchantId, merchantId + "-staff", "merchant_staff", null, merchantId, "active"))
        );
    }

    private FloorPlanCreateRequest createRequest(String idempotencyKey) {
        return createRequest(idempotencyKey, "prj_1");
    }

    private FloorPlanCreateRequest createRequest(String idempotencyKey, String projectId) {
        return new FloorPlanCreateRequest(
                idempotencyKey,
                "create floor plan",
                projectId,
                "A",
                "1F",
                "1F plan",
                "https://assets.example.com/floor-1.png",
                1600,
                1200,
                "v1",
                "active"
        );
    }

    private FloorPlanShapeSaveRequest shapeRequest(String idempotencyKey, String shapeId, List<FloorPlanPoint> polygon) {
        return new FloorPlanShapeSaveRequest(
                idempotencyKey,
                "save polygon",
                shapeId,
                "shop",
                "space",
                "sp_1",
                "Shop 101",
                polygon,
                10,
                "active"
        );
    }

    // M017-002：发布「不可静默覆盖既有布局证据」（治理地图 L472）——发布前已有图形须并存且原样
    @Test
    void publishImportTaskKeepsExistingShapes() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-publish-keep"));
        FloorPlanShapeRecord existing = service.saveShape(floorPlan.id(), shapeRequest("shape-existing", null, polygon()));
        FloorPlanImportTaskRecord task = service.createImportTask(floorPlan.id(), importRequest("fp-import-keep"));
        String candidateId = repository.findImportCandidatesByTaskId(task.id()).getFirst().id();

        service.publishImportTask(floorPlan.id(), task.id(),
                new FloorPlanImportPublishRequest("publish-keep", "publish import", List.of(candidateId)));

        assertThat(repository.findShapesByFloorPlanId(floorPlan.id()))
                .extracting(FloorPlanShapeRecord::label)
                .containsExactlyInAnyOrder("Shop 101", "A101");
        assertThat(repository.findShapeById(existing.id())).contains(existing);
    }

    // M017-002：解析失败的导入任务落 failed、错误落库，且不可发布
    @Test
    void importTaskWithOnlyErrorsIsFailedAndCannotBePublished() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-import-failed"));
        FloorPlanImportTaskRecord task = service.createImportTask(floorPlan.id(),
                importRequest("fp-import-failed-key", "<svg viewBox=\"0 0 1000 800\"><polygon id=\"bad\" points=\"1,1 2,2\" /></svg>"));

        assertThat(task.status()).isEqualTo("failed");
        assertThat(task.candidateCount()).isZero();
        assertThat(task.errorCount()).isEqualTo(1);
        assertThat(repository.findImportErrorsByTaskId(task.id()))
                .extracting(FloorPlanImportErrorRecord::errorCode)
                .containsExactly("POINTS_TOO_FEW");
        assertThatThrownBy(() -> service.publishImportTask(floorPlan.id(), task.id(),
                new FloorPlanImportPublishRequest("publish-failed", "publish import", List.of("fpic_x"))))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
    }

    // M017-002 / M017-006：候选项必须属于该导入任务（中文整句）；不存在的候选项 → IMPORT_CANDIDATE_NOT_FOUND；两次拒绝都不落图形
    @Test
    void publishRejectsCandidateOfAnotherTaskOrUnknownCandidate() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-publish-owner"));
        FloorPlanImportTaskRecord taskA = service.createImportTask(floorPlan.id(), importRequest("fp-import-owner-a"));
        FloorPlanImportTaskRecord taskB = service.createImportTask(floorPlan.id(), importRequest("fp-import-owner-b"));
        String candidateOfB = repository.findImportCandidatesByTaskId(taskB.id()).getFirst().id();

        assertThatThrownBy(() -> service.publishImportTask(floorPlan.id(), taskA.id(),
                new FloorPlanImportPublishRequest("publish-owner-1", "publish import", List.of(candidateOfB))))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(exception.getMessage()).isEqualTo("候选项不属于该导入任务");
                });
        assertThatThrownBy(() -> service.publishImportTask(floorPlan.id(), taskA.id(),
                new FloorPlanImportPublishRequest("publish-owner-2", "publish import", List.of("fpic_missing"))))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("IMPORT_CANDIDATE_NOT_FOUND"));
        assertThat(repository.findShapesByFloorPlanId(floorPlan.id())).isEmpty();
    }

    // M015-008 / M017-002 / M017-006：一次发布最多 200 个候选项；空列表拒绝（中文整句）
    @Test
    void publishRejectsMoreThanTwoHundredCandidatesAndEmptyList() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-publish-cap"));
        FloorPlanImportTaskRecord task = service.createImportTask(floorPlan.id(), importRequest("fp-import-cap"));
        List<String> tooMany = java.util.stream.IntStream.rangeClosed(1, 201).mapToObj(i -> "fpic_" + i).toList();

        assertThatThrownBy(() -> service.publishImportTask(floorPlan.id(), task.id(),
                new FloorPlanImportPublishRequest("publish-cap", "publish import", tooMany)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(exception.getMessage()).isEqualTo("一次发布的候选项不能超过 200 个");
                });
        assertThatThrownBy(() -> service.publishImportTask(floorPlan.id(), task.id(),
                new FloorPlanImportPublishRequest("publish-empty", "publish import", List.of())))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(exception.getMessage()).isEqualTo("候选项列表不能为空");
                });
    }

    // M017-002：导入任务列表与详情（含错误清单）可读，并写审计事件
    @Test
    void listImportTasksAndDetailsReturnRecordsAndWriteAuditEvents() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-import-details"));
        FloorPlanImportTaskRecord task = service.createImportTask(floorPlan.id(), importRequest("fp-import-details-key"));

        assertThat(service.listImportTasks(floorPlan.id()).items())
                .extracting(FloorPlanImportTaskRecord::id)
                .containsExactly(task.id());
        FloorPlanImportDetailsResponse details = service.getImportDetails(floorPlan.id(), task.id());

        assertThat(details.task().id()).isEqualTo(task.id());
        assertThat(details.candidates()).hasSize(1);
        assertThat(details.errors()).isEmpty();
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("floor_plan.import_viewed", "floor_plan.import_candidate_viewed");
    }

    // M017-006：导入内容上限文案（M015-006 已改中文整句）——1,000,001 字符被拒
    @Test
    void createImportTaskRejectsPayloadOverOneMillionCharsWithChineseMessage() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-import-huge"));
        String huge = "<svg viewBox=\"0 0 10 10\">" + "x".repeat(1_000_001) + "</svg>";

        assertThatThrownBy(() -> service.createImportTask(floorPlan.id(), importRequest("fp-import-huge-key", huge)))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(exception.getMessage()).isEqualTo("导入内容超过 100 万字符上限");
                });
    }

    // M017-003：平面图列表与导入任务列表同样诚实不分页（M015-007 三处；修复前只锁 listShapes）
    @Test
    void listFloorPlansAndImportTasksReportHonestPageSizeAndTotal() {
        FloorPlanRecord planA = service.createFloorPlan(createRequest("fp-honest-a"));
        service.createFloorPlan(createRequest("fp-honest-b"));
        service.createImportTask(planA.id(), importRequest("fp-honest-import-1"));
        service.createImportTask(planA.id(), importRequest("fp-honest-import-2"));

        PageResponse<FloorPlanRecord> plans = service.listFloorPlans();
        PageResponse<FloorPlanImportTaskRecord> tasks = service.listImportTasks(planA.id());

        assertThat(plans.items()).hasSize(2);
        assertThat(plans.total()).isEqualTo(2);
        assertThat(plans.pageSize()).isEqualTo(2);
        assertThat(tasks.items()).hasSize(2);
        assertThat(tasks.total()).isEqualTo(2);
        assertThat(tasks.pageSize()).isEqualTo(2);
    }

    // M017-005 / M017-006：非闭集的候选区域类型（如 corridor）须被服务白名单拒绝（中文整句）；corridor 不进白名单
    @Test
    void saveShapeRejectsShapeTypeOutsideClosedSet() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-corridor"));

        assertThatThrownBy(() -> service.saveShape(floorPlan.id(), shapeRequestOfType("shape-corridor", "corridor")))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.code()).isEqualTo("VALIDATION_FAILED");
                    assertThat(exception.getMessage()).isEqualTo("图形类型取值无效");
                });
        assertThat(repository.findShapesByFloorPlanId(floorPlan.id())).isEmpty();
    }

    // M016-002（M015-013 同批）：候选区域类型「摊位」「公共区」须被服务白名单接受（修复前白名单六值 → 此例红）
    @Test
    void saveShapeAcceptsBoothAndPublicArea() {
        FloorPlanRecord floorPlan = service.createFloorPlan(createRequest("fp-booth-types"));

        FloorPlanShapeRecord booth = service.saveShape(floorPlan.id(), shapeRequestOfType("shape-booth", "booth"));
        FloorPlanShapeRecord publicArea = service.saveShape(floorPlan.id(), shapeRequestOfType("shape-public-area", "public_area"));

        assertThat(booth.shapeType()).isEqualTo("booth");
        assertThat(publicArea.shapeType()).isEqualTo("public_area");
    }

    private FloorPlanShapeSaveRequest shapeRequestOfType(String idempotencyKey, String shapeType) {
        return new FloorPlanShapeSaveRequest(
                idempotencyKey,
                "save polygon",
                null,
                shapeType,
                "space",
                "sp_1",
                shapeType + " shape",
                polygon(),
                10,
                "active"
        );
    }

    private FloorPlanImportCreateRequest importRequest(String idempotencyKey, String svgPayload) {
        return new FloorPlanImportCreateRequest(idempotencyKey, "import svg", "svg", "1F.svg", svgPayload);
    }

    private FloorPlanImportCreateRequest importRequest(String idempotencyKey) {
        return new FloorPlanImportCreateRequest(
                idempotencyKey,
                "import svg",
                "svg",
                "1F.svg",
                """
                <svg viewBox="0 0 1000 800">
                  <polygon id="A101" data-shape-type="shop" data-ref-type="space" data-ref-id="sp_101"
                           points="100,80 300,80 300,240 100,240" />
                </svg>
                """
        );
    }

    private List<FloorPlanPoint> polygon() {
        return List.of(
                new FloorPlanPoint(0.1, 0.1),
                new FloorPlanPoint(0.4, 0.1),
                new FloorPlanPoint(0.4, 0.3),
                new FloorPlanPoint(0.1, 0.3)
        );
    }

    private static final class InMemoryFloorPlanRepository implements FloorPlanRepository {
        private final Map<String, FloorPlanRecord> floorPlans = new LinkedHashMap<>();
        private final Map<String, FloorPlanShapeRecord> shapes = new LinkedHashMap<>();
        private final Map<String, FloorPlanImportTaskRecord> importTasks = new LinkedHashMap<>();
        private final Map<String, FloorPlanImportCandidateRecord> importCandidates = new LinkedHashMap<>();
        private final Map<String, FloorPlanImportErrorRecord> importErrors = new LinkedHashMap<>();
        private int floorPlanSaveCount;
        private int shapeSaveCount;
        private int importTaskSaveCount;

        @Override
        public boolean projectExists(String projectId) {
            return Set.of("prj_1", "prj_2").contains(projectId);
        }

        @Override
        public List<FloorPlanRecord> findAllFloorPlansByProject(String projectId) {
            return floorPlans.values().stream()
                    .filter(fp -> projectId.equals(fp.projectId()))
                    .toList();
        }

        @Override
        public List<FloorPlanRecord> findAllFloorPlans() {
            return new ArrayList<>(floorPlans.values());
        }

        @Override
        public List<FloorPlanRecord> findAllFloorPlansByMerchant(String merchantId) {
            return floorPlans.values().stream()
                    .filter(fp -> merchantId.equals(fp.merchantId()))
                    .toList();
        }

        @Override
        public Optional<FloorPlanRecord> findFloorPlanById(String id) {
            return Optional.ofNullable(floorPlans.get(id));
        }

        @Override
        public FloorPlanRecord saveFloorPlan(FloorPlanRecord record) {
            floorPlans.put(record.id(), record);
            floorPlanSaveCount++;
            return record;
        }

        @Override
        public List<FloorPlanShapeRecord> findShapesByFloorPlanId(String floorPlanId) {
            return shapes.values().stream()
                    .filter(shape -> shape.floorPlanId().equals(floorPlanId))
                    .toList();
        }

        @Override
        public Optional<FloorPlanShapeRecord> findShapeById(String id) {
            return Optional.ofNullable(shapes.get(id));
        }

        @Override
        public FloorPlanShapeRecord saveShape(FloorPlanShapeRecord record) {
            shapes.put(record.id(), record);
            shapeSaveCount++;
            return record;
        }

        @Override
        public FloorPlanImportTaskRecord saveImportTask(FloorPlanImportTaskRecord record) {
            importTasks.put(record.id(), record);
            importTaskSaveCount++;
            return record;
        }

        @Override
        public Optional<FloorPlanImportTaskRecord> findImportTaskById(String id) {
            return Optional.ofNullable(importTasks.get(id));
        }

        @Override
        public List<FloorPlanImportTaskRecord> findImportTasksByFloorPlanId(String floorPlanId) {
            return importTasks.values().stream()
                    .filter(task -> task.floorPlanId().equals(floorPlanId))
                    .toList();
        }

        @Override
        public FloorPlanImportCandidateRecord saveImportCandidate(FloorPlanImportCandidateRecord record) {
            importCandidates.put(record.id(), record);
            return record;
        }

        @Override
        public Optional<FloorPlanImportCandidateRecord> findImportCandidateById(String id) {
            return Optional.ofNullable(importCandidates.get(id));
        }

        @Override
        public List<FloorPlanImportCandidateRecord> findImportCandidatesByTaskId(String importTaskId) {
            return importCandidates.values().stream()
                    .filter(candidate -> candidate.importTaskId().equals(importTaskId))
                    .toList();
        }

        @Override
        public FloorPlanImportErrorRecord saveImportError(FloorPlanImportErrorRecord record) {
            importErrors.put(record.id(), record);
            return record;
        }

        @Override
        public List<FloorPlanImportErrorRecord> findImportErrorsByTaskId(String importTaskId) {
            return importErrors.values().stream()
                    .filter(error -> error.importTaskId().equals(importTaskId))
                    .toList();
        }
    }
}
