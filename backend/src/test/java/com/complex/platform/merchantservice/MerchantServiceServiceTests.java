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
        // Default: merchant user scoped 流转到 "mch_1"
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
                "assign 流转到 property ops",
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
                "try 流转到 process closed order",
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
                "jump straight 流转到 resolved",
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
                "go back 流转到 assigned",
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
        // Work order belongs 流转到 mch_2
        repository.saveWorkOrder(workOrderRecord("mswo_other", "mch_2", "created"));

        // Caller is scoped 流转到 mch_1 (default mock)
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

        // Switch 流转到 a different user
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

        // Both keys slug 流转到 "key_abc_xyz" under the old scheme; with UUIDs they must differ
        assertThat(r1.id()).isNotEqualTo(r2.id());
        assertThat(r1.id()).matches("mswo_[0-9a-f]{32}");
    }

    @Test
    void escalateOverdueWorkOrdersMarksBreachedAndWritesEscalatedAction() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        // overdue（due_at 已过）且未完成（processing）→ 应被升级
        repository.saveWorkOrder(overdueRecord("mswo_overdue", "processing",
                Instant.parse("2026-06-18T00:00:00Z")));
        // 未到期（due_at 在 now 之后）→ 不应动
        repository.saveWorkOrder(overdueRecord("mswo_future", "processing",
                Instant.parse("2026-06-20T00:00:00Z")));
        // 已完成态（resolved）虽过期但不属未完成态 → 不应动
        repository.saveWorkOrder(overdueRecord("mswo_resolved", "resolved",
                Instant.parse("2026-06-18T00:00:00Z")));

        int escalated = service.escalateOverdueWorkOrders(now);

        assertThat(escalated).isEqualTo(1);

        MerchantWorkOrderRecord breached = repository.workOrders.get("mswo_overdue");
        assertThat(breached.slaBreached()).isTrue();
        assertThat(breached.escalatedAt()).isEqualTo(now);
        assertThat(breached.status()).isEqualTo("processing"); // 升级不改 status

        // 未被升级的两单保持原样
        assertThat(repository.workOrders.get("mswo_future").slaBreached()).isFalse();
        assertThat(repository.workOrders.get("mswo_future").escalatedAt()).isNull();
        assertThat(repository.workOrders.get("mswo_resolved").slaBreached()).isFalse();

        // escalated 留痕：from/to 一致、operator 为系统标识
        List<MerchantWorkOrderActionRecord> actions = repository.findActionsByWorkOrderId("mswo_overdue");
        assertThat(actions).hasSize(1);
        MerchantWorkOrderActionRecord action = actions.getFirst();
        assertThat(action.actionType()).isEqualTo("escalated");
        assertThat(action.fromStatus()).isEqualTo("processing");
        assertThat(action.toStatus()).isEqualTo("processing");
        assertThat(action.operatorUserId()).isEqualTo("system");

        assertThat(auditEventRepository.events)
                .extracting(AuditEvent::eventName)
                .contains("merchant_service.work_order.escalated");
    }

    @Test
    void escalateOverdueWorkOrdersIsIdempotentForAlreadyEscalated() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        repository.saveWorkOrder(overdueRecord("mswo_overdue", "processing",
                Instant.parse("2026-06-18T00:00:00Z")));

        assertThat(service.escalateOverdueWorkOrders(now)).isEqualTo(1);
        // 第二次扫描：已升级（escalated_at 非空）应被 findOverdueOpenWorkOrders 过滤掉
        assertThat(service.escalateOverdueWorkOrders(now)).isEqualTo(0);
        assertThat(repository.findActionsByWorkOrderId("mswo_overdue")).hasSize(1);
    }

    // ── P3 并发护栏：定时任务与手动触发并发双扫同一 overdue 工单，只能升级一次 ───────
    //
    // 缺口复现：读已提交隔离下，两个事务可在对方提交前各自读到同一行 escalated_at IS NULL，
    // 若升级走无条件 UPDATE 则会双重升级（escalated_at 被设两次、两条 escalated 留痕、
    // 两次 sla_breached 审计）。修复后升级走条件 UPDATE WHERE escalated_at IS NULL，
    // 受影响行数==1 才算升级，第二个事务行数==0 → 跳过留痕/审计，幂等防双升级。
    @Test
    void concurrentEscalationOfSameWorkOrderEscalatesOnlyOnce() {
        Instant now = Instant.parse("2026-06-19T00:00:00Z");
        // 用一个会“重复返回同一未升级行”的 repo 模拟两个并发事务各自的扫描快照都命中该行。
        ConcurrentSnapshotRepository concurrentRepo = new ConcurrentSnapshotRepository();
        concurrentRepo.saveWorkOrder(overdueRecord("mswo_race", "processing",
                Instant.parse("2026-06-18T00:00:00Z")));
        MerchantServiceService racingService = new MerchantServiceService(
                concurrentRepo,
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                new AuditEventService(auditEventRepository),
                authorizationService
        );

        // 第一次触发（定时）：拿到快照、条件 UPDATE 行数 1 → 升级。
        assertThat(racingService.escalateOverdueWorkOrders(now)).isEqualTo(1);
        // 第二次触发（手动）：扫描快照仍含该行（模拟并发未见对方提交），
        // 但条件 UPDATE 行数 0 → 不再升级、不再留痕、不再审计。
        assertThat(racingService.escalateOverdueWorkOrders(now)).isEqualTo(0);

        MerchantWorkOrderRecord escalated = concurrentRepo.workOrders.get("mswo_race");
        assertThat(escalated.slaBreached()).isTrue();
        assertThat(escalated.escalatedAt()).isEqualTo(now); // 只设一次
        // escalated 留痕只有一条（防双留痕）
        assertThat(concurrentRepo.findActionsByWorkOrderId("mswo_race"))
                .filteredOn(a -> "escalated".equals(a.actionType()))
                .hasSize(1);
        // 升级审计事件只发一次
        assertThat(auditEventRepository.events)
                .filteredOn(e -> "merchant_service.work_order.escalated".equals(e.eventName()))
                .hasSize(1);
    }

    /**
     * 并发快照模拟 repo：findOverdueOpenWorkOrders 不按 escalated_at 过滤，
     * 每次都把库内所有 overdue 行原样返回（模拟两个并发事务的扫描快照都在对方提交前命中同一行），
     * 但 markSlaBreachedIfNotEscalated 保持条件 UPDATE 的原子语义（escalated_at 非空则返回 0）。
     * 用以验证 service 仅依据条件 UPDATE 行数判定升级，而非依赖读端过滤。
     */
    private static final class ConcurrentSnapshotRepository implements MerchantServiceRepository {
        @Override public void setWorkOrderArchiveReason(String id, String reason) { }
        private final Map<String, MerchantWorkOrderRecord> workOrders = new LinkedHashMap<>();
        private final List<MerchantWorkOrderActionRecord> actions = new ArrayList<>();

        @Override
        public List<MerchantWorkOrderRecord> findWorkOrdersByScope(Optional<String> merchantScope, int limit) {
            return List.copyOf(workOrders.values());
        }

        @Override
        public Optional<MerchantWorkOrderRecord> findWorkOrderById(String id) {
            return Optional.ofNullable(workOrders.get(id));
        }

        @Override
        public List<MerchantWorkOrderRecord> findOverdueOpenWorkOrders(Instant now, int limit) {
            // 故意不过滤 escalated_at：模拟并发事务在对方提交前拿到的旧快照仍含该行。
            java.util.Set<String> doneStatuses = java.util.Set.of("resolved", "closed", "cancelled");
            return workOrders.values().stream()
                    .filter(r -> r.dueAt() != null && !r.dueAt().isAfter(now))
                    .filter(r -> !doneStatuses.contains(r.status()))
                    .limit(limit)
                    .toList();
        }

        @Override
        public MerchantWorkOrderRecord saveWorkOrder(MerchantWorkOrderRecord record) {
            workOrders.put(record.id(), record);
            return record;
        }

        @Override
        public int markSlaBreachedIfNotEscalated(String id, Instant now) {
            MerchantWorkOrderRecord current = workOrders.get(id);
            if (current == null || current.escalatedAt() != null) {
                return 0;
            }
            workOrders.put(id, current.markSlaBreached(now));
            return 1;
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

    private MerchantWorkOrderRecord overdueRecord(String id, String status, Instant dueAt) {
        Instant created = Instant.parse("2026-06-14T00:00:00Z");
        return new MerchantWorkOrderRecord(
                id, "mch_1", null, null, "repair", "high", status,
                "Test", "Test description", "reporter", "web",
                "property", "staff_owner", dueAt, null, null, null, List.of(),
                false, null, created, created
        );
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
                null, null, null, null, null, null, List.of(), false, null, now, now
        );
    }

    private static final class InMemoryMerchantServiceRepository implements MerchantServiceRepository {
        @Override public void setWorkOrderArchiveReason(String id, String reason) { }
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
        public List<MerchantWorkOrderRecord> findOverdueOpenWorkOrders(Instant now, int limit) {
            java.util.Set<String> doneStatuses = java.util.Set.of("resolved", "closed", "cancelled");
            return workOrders.values().stream()
                    .filter(r -> r.dueAt() != null && !r.dueAt().isAfter(now))
                    .filter(r -> r.escalatedAt() == null)
                    .filter(r -> !doneStatuses.contains(r.status()))
                    .sorted(java.util.Comparator.comparing(MerchantWorkOrderRecord::dueAt)
                            .thenComparing(MerchantWorkOrderRecord::id))
                    .limit(limit)
                    .toList();
        }

        @Override
        public MerchantWorkOrderRecord saveWorkOrder(MerchantWorkOrderRecord record) {
            workOrders.put(record.id(), record);
            saveCount++;
            return record;
        }

        @Override
        public int markSlaBreachedIfNotEscalated(String id, Instant now) {
            // 模拟 PostgreSQL 条件 UPDATE ... WHERE escalated_at IS NULL 的原子语义：
            // 仅当当前持久行 escalated_at 仍为空才升级并返回 1，否则返回 0（已被并发抢先）。
            MerchantWorkOrderRecord current = workOrders.get(id);
            if (current == null || current.escalatedAt() != null) {
                return 0;
            }
            workOrders.put(id, current.markSlaBreached(now));
            saveCount++;
            return 1;
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
