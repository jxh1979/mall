package com.complex.platform.floorplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
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
                new FloorPlanImportParser()
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
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
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

    private FloorPlanCreateRequest createRequest(String idempotencyKey) {
        return new FloorPlanCreateRequest(
                idempotencyKey,
                "create floor plan",
                "prj_1",
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
        public List<FloorPlanRecord> findAllFloorPlans() {
            return new ArrayList<>(floorPlans.values());
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

    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

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
    }

    private static final class InMemoryAuditEventRepository implements AuditEventRepository {
        private final List<AuditEvent> events = new ArrayList<>();

        @Override
        public AuditEvent save(AuditEvent event) {
            events.add(event);
            return event;
        }
    }
}
