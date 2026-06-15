package com.complex.platform.merchantservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEvent;
import com.complex.platform.evidence.AuditEventRepository;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderActionRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderAssignRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCloseRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCreateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MerchantServiceServiceTests {

    private InMemoryMerchantServiceRepository repository;
    private InMemoryAuditEventRepository auditEventRepository;
    private MerchantServiceService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMerchantServiceRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        service = new MerchantServiceService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository)
        );
    }

    @Test
    void createWorkOrderRequiresCoreFieldsAndWritesFailureAudit() {
        assertThatThrownBy(() -> service.createWorkOrder(new MerchantWorkOrderCreateRequest(
                "wo-missing-merchant",
                "missing merchant",
                List.of(),
                " ",
                null,
                null,
                "repair",
                "high",
                "Water leak",
                "Shop ceiling is leaking",
                "staff_reporter",
                "web",
                Instant.parse("2026-06-15T08:00:00Z")
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("merchant_service.work_order.create_failed");
    }

    @Test
    void createWorkOrderPersistsCreatedActionAndAuditEvent() {
        MerchantWorkOrderRecord created = service.createWorkOrder(createRequest("wo-create"));

        assertThat(created.id()).startsWith("mswo_");
        assertThat(created.status()).isEqualTo("created");
        assertThat(created.workOrderType()).isEqualTo("repair");
        assertThat(created.priority()).isEqualTo("high");
        assertThat(created.evidenceAttachmentIds()).containsExactly("att-photo-1");
        assertThat(repository.actions).hasSize(1);
        assertThat(repository.actions.getFirst().actionType()).isEqualTo("created");
        assertThat(repository.actions.getFirst().toStatus()).isEqualTo("created");
        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("merchant_service.work_order.created");
    }

    @Test
    void assignWorkOrderMovesToAssignedAndRecordsAction() {
        MerchantWorkOrderRecord created = service.createWorkOrder(createRequest("wo-assign-create"));

        MerchantWorkOrderRecord assigned = service.assignWorkOrder(created.id(), new MerchantWorkOrderAssignRequest(
                "wo-assign",
                "assign to property ops",
                List.of("att-assign"),
                "property",
                "staff_owner",
                "staff_manager"
        ));

        assertThat(assigned.status()).isEqualTo("assigned");
        assertThat(assigned.ownerDepartment()).isEqualTo("property");
        assertThat(assigned.ownerUserId()).isEqualTo("staff_owner");
        assertThat(repository.actions)
                .extracting(MerchantWorkOrderActionRecord::actionType)
                .contains("created", "assigned");
        assertThat(repository.actions.getLast().fromStatus()).isEqualTo("created");
        assertThat(repository.actions.getLast().toStatus()).isEqualTo("assigned");
    }

    @Test
    void addActionRejectsClosedWorkOrder() {
        MerchantWorkOrderRecord created = service.createWorkOrder(createRequest("wo-closed-create"));
        service.closeWorkOrder(created.id(), closeRequest("wo-close"));

        assertThatThrownBy(() -> service.addAction(created.id(), new MerchantWorkOrderActionRequest(
                "wo-action-after-close",
                "try to process closed order",
                List.of(),
                "processing",
                "staff_ops"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("WORK_ORDER_CLOSED"));
    }

    @Test
    void closeWorkOrderRequiresResolutionAndValidSatisfactionScore() {
        MerchantWorkOrderRecord created = service.createWorkOrder(createRequest("wo-close-validation-create"));

        assertThatThrownBy(() -> service.closeWorkOrder(created.id(), new MerchantWorkOrderCloseRequest(
                "wo-close-missing-resolution",
                "close",
                List.of(),
                " ",
                5,
                "staff_ops"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));

        assertThatThrownBy(() -> service.closeWorkOrder(created.id(), new MerchantWorkOrderCloseRequest(
                "wo-close-bad-score",
                "close",
                List.of(),
                "Fixed and merchant confirmed",
                6,
                "staff_ops"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void closeWorkOrderMovesToClosedAndRecordsResolution() {
        MerchantWorkOrderRecord created = service.createWorkOrder(createRequest("wo-close-create"));

        MerchantWorkOrderRecord closed = service.closeWorkOrder(created.id(), closeRequest("wo-close-success"));

        assertThat(closed.status()).isEqualTo("closed");
        assertThat(closed.closedAt()).isNotNull();
        assertThat(closed.resolution()).isEqualTo("Fixed and merchant confirmed");
        assertThat(closed.satisfactionScore()).isEqualTo(5);
        assertThat(repository.actions.getLast().actionType()).isEqualTo("closed");
    }

    @Test
    void duplicateIdempotencyKeyWithSameRequestReturnsFirstResult() {
        MerchantWorkOrderCreateRequest request = createRequest("wo-once");

        MerchantWorkOrderRecord first = service.createWorkOrder(request);
        MerchantWorkOrderRecord second = service.createWorkOrder(request);

        assertThat(second).isEqualTo(first);
        assertThat(repository.saveCount).isEqualTo(1);
        assertThat(auditEventRepository.events)
                .filteredOn(event -> "merchant_service.work_order.created".equals(event.eventName()))
                .hasSize(1);
    }

    @Test
    void duplicateIdempotencyKeyWithDifferentRequestReturnsConflict() {
        service.createWorkOrder(createRequest("wo-conflict"));

        assertThatThrownBy(() -> service.createWorkOrder(new MerchantWorkOrderCreateRequest(
                "wo-conflict",
                "different request",
                List.of(),
                "mch_1",
                null,
                null,
                "complaint",
                "high",
                "Noise complaint",
                "Different body",
                "staff_reporter",
                "web",
                Instant.parse("2026-06-15T08:00:00Z")
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void validationFailureDoesNotReserveIdempotencyKey() {
        assertThatThrownBy(() -> service.createWorkOrder(new MerchantWorkOrderCreateRequest(
                "wo-validation-retry",
                "missing merchant",
                List.of(),
                " ",
                null,
                null,
                "repair",
                "high",
                "Water leak",
                "Shop ceiling is leaking",
                "staff_reporter",
                "web",
                Instant.parse("2026-06-15T08:00:00Z")
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));

        MerchantWorkOrderRecord retry = service.createWorkOrder(createRequest("wo-validation-retry"));

        assertThat(retry.status()).isEqualTo("created");
    }

    private MerchantWorkOrderCreateRequest createRequest(String idempotencyKey) {
        return new MerchantWorkOrderCreateRequest(
                idempotencyKey,
                "create work order",
                List.of("att-photo-1"),
                "mch_1",
                "sp_1",
                "ct_1",
                "repair",
                "high",
                "Water leak",
                "Shop ceiling is leaking",
                "staff_reporter",
                "web",
                Instant.parse("2026-06-15T08:00:00Z")
        );
    }

    private MerchantWorkOrderCloseRequest closeRequest(String idempotencyKey) {
        return new MerchantWorkOrderCloseRequest(
                idempotencyKey,
                "close work order",
                List.of("att-close"),
                "Fixed and merchant confirmed",
                5,
                "staff_ops"
        );
    }

    private static final class InMemoryMerchantServiceRepository implements MerchantServiceRepository {
        private final Map<String, MerchantWorkOrderRecord> workOrders = new LinkedHashMap<>();
        private final List<MerchantWorkOrderActionRecord> actions = new ArrayList<>();
        private int saveCount;

        @Override
        public List<MerchantWorkOrderRecord> findAllWorkOrders() {
            return new ArrayList<>(workOrders.values());
        }

        @Override
        public Optional<MerchantWorkOrderRecord> findWorkOrderById(String id) {
            return Optional.ofNullable(workOrders.get(id));
        }

        @Override
        public MerchantWorkOrderRecord saveWorkOrder(MerchantWorkOrderRecord record) {
            workOrders.put(record.id(), record);
            saveCount++;
            return record;
        }

        @Override
        public MerchantWorkOrderActionRecord saveAction(MerchantWorkOrderActionRecord record) {
            actions.add(record);
            return record;
        }

        @Override
        public List<MerchantWorkOrderActionRecord> findActionsByWorkOrderId(String workOrderId) {
            return actions.stream()
                    .filter(action -> action.workOrderId().equals(workOrderId))
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
