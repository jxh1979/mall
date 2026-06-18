package com.complex.platform.merchantservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
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
    private AuthorizationService authorizationService;
    private MerchantServiceService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMerchantServiceRepository();
        auditEventRepository = new InMemoryAuditEventRepository();
        authorizationService = mock(AuthorizationService.class);
        service = new MerchantServiceService(
                repository,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                authorizationService
        );
        // Default: merchant user scoped to "mch_1"
        when(authorizationService.currentActor())
                .thenReturn(new AccessActor("user_test", "test", "merchant", null, "mch_1", "active"));
        when(authorizationService.currentMerchantScope())
                .thenReturn(Optional.of("mch_1"));
    }

    @Test
    void createWorkOrderRequiresCoreFieldsAndWritesFailureAudit() {
        // Empty title is always required regardless of scope
        assertThatThrownBy(() -> service.createWorkOrder(new MerchantWorkOrderCreateRequest(
                "wo-missing-title",
                "missing title",
                List.of(),
                "mch_1",
                null,
                null,
                "repair",
                "high",
                " ",
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
        service.assignWorkOrder(created.id(), new MerchantWorkOrderAssignRequest(
                "wo-closed-assign", "assign", List.of(), "property", "staff_owner", "staff_manager"));
        service.addAction(created.id(), new MerchantWorkOrderActionRequest(
                "wo-closed-resolve", "resolve", List.of(), "resolved", "staff_ops"));
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
    void addActionRejectsIllegalForwardSkip() {
        MerchantWorkOrderRecord created = service.createWorkOrder(createRequest("wo-skip-create"));

        // created -> resolved skips assigned/processing and must be rejected
        assertThatThrownBy(() -> service.addAction(created.id(), new MerchantWorkOrderActionRequest(
                "wo-skip-action",
                "jump straight to resolved",
                List.of(),
                "resolved",
                "staff_ops"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("MERCHANT_WORK_ORDER_INVALID_TRANSITION"));
        assertThat(repository.findWorkOrderById(created.id()))
                .get().extracting(MerchantWorkOrderRecord::status).isEqualTo("created");
    }

    @Test
    void addActionRejectsStatusRegression() {
        MerchantWorkOrderRecord created = service.createWorkOrder(createRequest("wo-regress-create"));
        service.assignWorkOrder(created.id(), new MerchantWorkOrderAssignRequest(
                "wo-regress-assign", "assign", List.of(), "property", "staff_owner", "staff_manager"));
        service.addAction(created.id(), new MerchantWorkOrderActionRequest(
                "wo-regress-process", "start", List.of(), "processing", "staff_ops"));

        // processing -> assigned is a regression and must be rejected
        assertThatThrownBy(() -> service.addAction(created.id(), new MerchantWorkOrderActionRequest(
                "wo-regress-back",
                "go back to assigned",
                List.of(),
                "assigned",
                "staff_ops"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("MERCHANT_WORK_ORDER_INVALID_TRANSITION"));
    }

    @Test
    void addActionAllowsValidForwardTransition() {
        MerchantWorkOrderRecord created = service.createWorkOrder(createRequest("wo-forward-create"));
        service.assignWorkOrder(created.id(), new MerchantWorkOrderAssignRequest(
                "wo-forward-assign", "assign", List.of(), "property", "staff_owner", "staff_manager"));

        MerchantWorkOrderRecord processing = service.addAction(created.id(), new MerchantWorkOrderActionRequest(
                "wo-forward-process", "start processing", List.of(), "processing", "staff_ops"));

        assertThat(processing.status()).isEqualTo("processing");
        assertThat(repository.actions.getLast().toStatus()).isEqualTo("processing");
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
        service.assignWorkOrder(created.id(), new MerchantWorkOrderAssignRequest(
                "wo-close-assign", "assign", List.of(), "property", "staff_owner", "staff_manager"));
        service.addAction(created.id(), new MerchantWorkOrderActionRequest(
                "wo-close-resolve", "resolve", List.of(), "resolved", "staff_ops"));

        MerchantWorkOrderRecord closed = service.closeWorkOrder(created.id(), closeRequest("wo-close-success"));

        assertThat(closed.status()).isEqualTo("closed");
        assertThat(closed.closedAt()).isNotNull();
        assertThat(closed.resolution()).isEqualTo("Fixed and merchant confirmed");
        assertThat(closed.satisfactionScore()).isEqualTo(5);
        assertThat(repository.actions.getLast().actionType()).isEqualTo("closed");
    }

    // ── §1.2 用例1：从非 resolved 态直接 close → INVALID_STATE ─────────────────────
    //
    // 工单只能从 resolved 态收口关闭（closeWorkOrderOnce 的 resolved 前置守卫，镜像
    // assignWorkOrderOnce 的 created 守卫）。created/assigned/processing/pending_merchant
    // 直接 close 均为非法跳步，抛 INVALID_STATE；唯有 resolved → close 放行。
    @Test
    void closeWorkOrderFromNonResolvedStatusThrowsInvalidState() {
        // created 态直接 close → INVALID_STATE
        MerchantWorkOrderRecord createdOrder = service.createWorkOrder(createRequest("wo-close-from-created"));
        assertThat(createdOrder.status()).isEqualTo("created");
        assertThatThrownBy(() ->
                service.closeWorkOrder(createdOrder.id(), closeRequest("wo-close-from-created-key")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_STATE"));

        // assigned 态直接 close → INVALID_STATE
        MerchantWorkOrderRecord assignedBase = service.createWorkOrder(createRequest("wo-close-from-assigned"));
        service.assignWorkOrder(assignedBase.id(), new MerchantWorkOrderAssignRequest(
                "wo-close-from-assigned-assign", "assign", List.of(), "property", "staff_owner", "staff_manager"));
        assertThatThrownBy(() ->
                service.closeWorkOrder(assignedBase.id(), closeRequest("wo-close-from-assigned-key")))
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.code()).isEqualTo("INVALID_STATE"));

        // resolved 态 close → 成功（前置守卫放行）
        service.addAction(assignedBase.id(), new MerchantWorkOrderActionRequest(
                "wo-close-from-assigned-resolve", "resolve", List.of(), "resolved", "staff_ops"));
        MerchantWorkOrderRecord closed =
                service.closeWorkOrder(assignedBase.id(), closeRequest("wo-close-from-assigned-key2"));
        assertThat(closed.status()).isEqualTo("closed");
    }

    // ── §1.2 用例2：comment 类 action 在任意非终态允许且不推进状态 ────────────────
    @Test
    void addCommentActionAllowedInAnyNonTerminalStatusWithoutStatusChange() {
        MerchantWorkOrderRecord base = service.createWorkOrder(createRequest("wo-comment-create"));
        service.assignWorkOrder(base.id(), new MerchantWorkOrderAssignRequest(
                "wo-comment-assign", "assign", List.of(), "property", "staff_owner", "staff_manager"));

        // assigned 态：comment（无 targetStatus）允许且状态不变
        MerchantWorkOrderRecord afterAssignedComment = service.addAction(base.id(), new MerchantWorkOrderActionRequest(
                "wo-comment-on-assigned",
                "just a comment on assigned",
                List.of(),
                null,
                "staff_ops"
        ));
        assertThat(afterAssignedComment.status()).isEqualTo("assigned");
        assertThat(repository.actions.getLast().actionType()).isEqualTo("commented");
        assertThat(repository.actions.getLast().fromStatus()).isEqualTo("assigned");
        assertThat(repository.actions.getLast().toStatus()).isEqualTo("assigned");

        // 推进到 processing
        service.addAction(base.id(), new MerchantWorkOrderActionRequest(
                "wo-comment-to-processing", "start", List.of(), "processing", "staff_ops"));

        // processing 态：comment 同样允许且状态不变
        MerchantWorkOrderRecord afterProcessingComment = service.addAction(base.id(), new MerchantWorkOrderActionRequest(
                "wo-comment-on-processing",
                "just a comment on processing",
                List.of(),
                null,
                "staff_ops"
        ));
        assertThat(afterProcessingComment.status()).isEqualTo("processing");
        assertThat(repository.actions.getLast().actionType()).isEqualTo("commented");
        assertThat(repository.actions.getLast().fromStatus()).isEqualTo("processing");
        assertThat(repository.actions.getLast().toStatus()).isEqualTo("processing");
    }

    // ── §1.2 用例3：5 op 成功路径各落预期审计事件名 ──────────────────────────────
    //
    // 真实事件名（grep MerchantServiceService）：
    //   create → merchant_service.work_order.created
    //   assign → merchant_service.work_order.assigned
    //   addAction → merchant_service.work_order.action_added
    //   close → merchant_service.work_order.closed
    //   list → merchant_service.work_order.viewed
    @Test
    void eachWorkOrderOpEmitsSuccessAuditEventWithExpectedEventName() {
        MerchantWorkOrderRecord base = service.createWorkOrder(createRequest("wo-audit-create"));
        service.assignWorkOrder(base.id(), new MerchantWorkOrderAssignRequest(
                "wo-audit-assign", "assign", List.of(), "property", "staff_owner", "staff_manager"));
        service.addAction(base.id(), new MerchantWorkOrderActionRequest(
                "wo-audit-action", "start processing", List.of(), "processing", "staff_ops"));
        service.addAction(base.id(), new MerchantWorkOrderActionRequest(
                "wo-audit-resolve", "resolve", List.of(), "resolved", "staff_ops"));
        service.closeWorkOrder(base.id(), closeRequest("wo-audit-close"));
        service.listWorkOrders();

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains(
                        "merchant_service.work_order.created",
                        "merchant_service.work_order.assigned",
                        "merchant_service.work_order.action_added",
                        "merchant_service.work_order.closed",
                        "merchant_service.work_order.viewed"
                );
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
        // Empty title triggers VALIDATION_FAILED in preValidation
        assertThatThrownBy(() -> service.createWorkOrder(new MerchantWorkOrderCreateRequest(
                "wo-validation-retry",
                "missing title",
                List.of(),
                "mch_1",
                null,
                null,
                "repair",
                "high",
                " ",
                "Shop ceiling is leaking",
                "staff_reporter",
                "web",
                Instant.parse("2026-06-15T08:00:00Z")
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("VALIDATION_FAILED"));

        MerchantWorkOrderRecord retry = service.createWorkOrder(createRequest("wo-validation-retry"));

        assertThat(retry.status()).isEqualTo("created");
    }

    // ── P0: Tenant isolation ──────────────────────────────────────────────────

    @Test
    void listWorkOrdersFilteredByMerchantScope() {
        // Place work orders for two merchants directly in the repository
        repository.saveWorkOrder(workOrderRecord("mswo_m1", "mch_1", "created"));
        repository.saveWorkOrder(workOrderRecord("mswo_m2", "mch_2", "created"));

        // Default scope = "mch_1": must only see mch_1's order
        var result = service.listWorkOrders();
        assertThat(result.items()).extracting(MerchantWorkOrderRecord::id).containsExactly("mswo_m1");
        assertThat(result.items()).noneMatch(r -> "mch_2".equals(r.merchantId()));
    }

    @Test
    void platformAdminListWorkOrdersSeesAll() {
        repository.saveWorkOrder(workOrderRecord("mswo_m1", "mch_1", "created"));
        repository.saveWorkOrder(workOrderRecord("mswo_m2", "mch_2", "created"));

        when(authorizationService.currentActor())
                .thenReturn(new AccessActor("admin_user", "admin", "platform_admin", null, null, "active"));
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());

        var result = service.listWorkOrders();
        assertThat(result.items()).extracting(MerchantWorkOrderRecord::id)
                .contains("mswo_m1", "mswo_m2");
    }

    // ── P0: Cross-merchant write protection ──────────────────────────────────

    @Test
    void assignWorkOrderCrossMerchantThrowsForbidden() {
        // Work order belongs to mch_2
        repository.saveWorkOrder(workOrderRecord("mswo_other", "mch_2", "created"));

        // Caller is scoped to mch_1 (default mock)
        assertThatThrownBy(() -> service.assignWorkOrder("mswo_other", new MerchantWorkOrderAssignRequest(
                "wo-cross-assign",
                "assign",
                List.of(),
                "property",
                "staff_owner",
                "staff_manager"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
    }

    // ── P1: Assign state machine guard ───────────────────────────────────────

    @Test
    void assignAlreadyAssignedWorkOrderThrowsInvalidState() {
        MerchantWorkOrderRecord created = service.createWorkOrder(createRequest("wo-assign-state"));
        service.assignWorkOrder(created.id(), new MerchantWorkOrderAssignRequest(
                "wo-assign-first",
                "first assign",
                List.of(),
                "property",
                "staff_owner",
                "staff_manager"
        ));

        // Work order is now in "assigned" state; second assign must be rejected
        assertThatThrownBy(() -> service.assignWorkOrder(created.id(), new MerchantWorkOrderAssignRequest(
                "wo-assign-second",
                "second assign",
                List.of(),
                "property",
                "staff_owner2",
                "staff_manager"
        ))).isInstanceOfSatisfying(ApiException.class, exception ->
                assertThat(exception.code()).isEqualTo("INVALID_STATE"));
    }

    // ── P1: Operator identity from security context ───────────────────────────

    @Test
    void reportedByUserIdTakenFromSecurityContextOnCreate() {
        // Actor id is "user_test" (from mock); request supplies a different value that must be ignored
        MerchantWorkOrderRecord created = service.createWorkOrder(createRequest("wo-actor-create"));

        assertThat(created.reportedByUserId()).isEqualTo("user_test");
        assertThat(repository.actions.getFirst().operatorUserId()).isEqualTo("user_test");
    }

    @Test
    void operatorUserIdTakenFromSecurityContextOnAssign() {
        MerchantWorkOrderRecord created = service.createWorkOrder(createRequest("wo-actor-assign"));
        service.assignWorkOrder(created.id(), new MerchantWorkOrderAssignRequest(
                "wo-actor-assign-key",
                "assign",
                List.of(),
                "property",
                "staff_owner",
                "SHOULD_BE_IGNORED"
        ));

        MerchantWorkOrderActionRecord assignAction = repository.actions.getLast();
        assertThat(assignAction.operatorUserId()).isEqualTo("user_test");
        assertThat(assignAction.operatorUserId()).isNotEqualTo("SHOULD_BE_IGNORED");
    }

    // ── P1: Idempotency scoped per user ───────────────────────────────────────

    @Test
    void differentUsersCanUseSameIdempotencyKeyForCreate() {
        // user_test creates with key "shared-key"
        MerchantWorkOrderRecord first = service.createWorkOrder(createRequest("shared-key"));

        // Switch to a different user
        when(authorizationService.currentActor())
                .thenReturn(new AccessActor("user_other", "other", "merchant", null, "mch_1", "active"));
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("mch_1"));

        MerchantWorkOrderRecord second = service.createWorkOrder(createRequest("shared-key"));

        // Each user's slot is isolated; both records are created and distinct
        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(repository.saveCount).isEqualTo(2);
    }

    // ── P1: UUID-based work order IDs (no slug collision) ────────────────────

    @Test
    void workOrderIdIsUuidBasedAndNotDerivedFromIdempotencyKey() {
        MerchantWorkOrderRecord r1 = service.createWorkOrder(createRequest("key abc xyz"));
        MerchantWorkOrderRecord r2 = service.createWorkOrder(createRequest("key abc!xyz"));

        // Both keys slug to "key_abc_xyz" under the old scheme; with UUIDs they must differ
        assertThat(r1.id()).isNotEqualTo(r2.id());
        assertThat(r1.id()).matches("mswo_[0-9a-f]{32}");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

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

    private MerchantWorkOrderRecord workOrderRecord(String id, String merchantId, String status) {
        Instant now = Instant.parse("2026-06-14T00:00:00Z");
        return new MerchantWorkOrderRecord(
                id, merchantId, null, null, "repair", "high", status,
                "Test", "Test description", "reporter", "web",
                null, null, null, null, null, null, List.of(), now, now
        );
    }

    private static final class InMemoryMerchantServiceRepository implements MerchantServiceRepository {
        private final Map<String, MerchantWorkOrderRecord> workOrders = new LinkedHashMap<>();
        private final List<MerchantWorkOrderActionRecord> actions = new ArrayList<>();
        private int saveCount;

        @Override
        public List<MerchantWorkOrderRecord> findWorkOrdersByScope(Optional<String> merchantScope, int limit) {
            return workOrders.values().stream()
                    .filter(r -> merchantScope.isEmpty() || merchantScope.get().equals(r.merchantId()))
                    .limit(limit)
                    .toList();
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
