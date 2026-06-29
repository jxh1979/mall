package com.complex.platform.facility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.FacilityWorkOrderActionRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderAssignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** 楼宇维保工单 S2 unit 测试：SLA建单、流转、告警转单、超时、完工回写设备。 */
class FacilityWorkOrderServiceTests {

    private InMemoryWorkOrderRepository workOrders;
    private InMemoryFacilityRepository facility;
    private FacilityWorkOrderService service;

    @BeforeEach
    void setUp() {
        workOrders = new InMemoryWorkOrderRepository();
        facility = new InMemoryFacilityRepository();
        seedPolicy("repair", "high", 60, 720, true, "supervisor");
        seedPolicy("cleaning", "normal", 120, 480, false, "handler");
        seedPolicy("complaint", "normal", 120, 1440, false, "reporter");
        service = new FacilityWorkOrderService(workOrders, facility, mock(AuditEventService.class),
                mock(AuthorizationService.class));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_setsSlaCodeAndRejectsMerchantSelfRestrictedTypes() {
        assertThatThrownBy(() -> service.create(req("rectification", "normal", "merchant_self", null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));

        FacilityWorkOrderRecord rec = service.create(req("repair", "high", "merchant_self", null));

        assertThat(rec.id()).startsWith("fwo_");
        assertThat(rec.code()).startsWith("FWO-");
        assertThat(rec.status()).isEqualTo("pending_assign");
        assertThat(rec.responseDueAt()).isEqualTo(rec.createdAt().plusSeconds(60L * 60));
        assertThat(rec.handleDueAt()).isEqualTo(rec.createdAt().plusSeconds(720L * 60));
        assertThat(workOrders.store).containsKey(rec.id());
    }

    @Test
    void readMethodsRequirePlatformScopeSoMerchantCannotReadAllPlatformWorkOrders() {
        // B1d 跨租户读隔离：list/listOverdue/findById 也必须过平台域闸，
        // 否则持 facility.work_order.manage 的商户角色可读全平台工单。
        AuthorizationService authz = mock(AuthorizationService.class);
        org.mockito.Mockito.doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "merchant-scoped"))
                .when(authz).requirePlatformScope();
        FacilityWorkOrderService scoped = new FacilityWorkOrderService(
                workOrders, facility, mock(AuditEventService.class), authz);

        assertThatThrownBy(scoped::list)
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
        assertThatThrownBy(scoped::listOverdue)
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
        assertThatThrownBy(() -> scoped.findById("fwo_any"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
    }

    @Test
    void outsourcedListAndDetailAreLimitedToOwnAssigneeAndProject() {
        FacilityWorkOrderService scoped = outsourcedService("usr_vendor_a", "proj_1");
        workOrders.insertWorkOrder(rawAssigned("wo_a", "assigned", "usr_vendor_a", "proj_1"));
        workOrders.insertWorkOrder(rawAssigned("wo_b", "assigned", "usr_vendor_b", "proj_1"));
        workOrders.insertWorkOrder(rawAssigned("wo_cross_project", "assigned", "usr_vendor_a", "proj_2"));

        PageResponse<FacilityWorkOrderRecord> page = scoped.list();

        assertThat(page.items()).extracting(FacilityWorkOrderRecord::id).containsExactly("wo_a");
        assertThat(scoped.findById("wo_a").id()).isEqualTo("wo_a");
        assertThatThrownBy(() -> scoped.findById("wo_b"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
        assertThatThrownBy(() -> scoped.findById("wo_cross_project"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
    }

    @Test
    void outsourcedCanProcessOwnWorkOrderButCannotProcessOrAcceptOthers() {
        FacilityWorkOrderService scoped = outsourcedService("usr_vendor_a", "proj_1");
        workOrders.insertWorkOrder(rawAssigned("wo_own", "assigned", "usr_vendor_a", "proj_1"));
        workOrders.insertWorkOrder(rawAssigned("wo_other", "assigned", "usr_vendor_b", "proj_1"));
        workOrders.insertWorkOrder(rawAssigned("wo_accept_own", "pending_acceptance", "usr_vendor_a", "proj_1"));
        workOrders.insertWorkOrder(rawAssigned("wo_accept_other", "pending_acceptance", "usr_vendor_b", "proj_1"));

        assertThatThrownBy(() -> scoped.start("wo_other", action(null, null, "start", "k_start_other")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
        FacilityWorkOrderRecord processing = scoped.start("wo_own", action(null, null, "start", "k_start_own"));
        FacilityWorkOrderRecord completed = scoped.complete(processing.id(), action("fixed", null, "done", "k_done_own"));

        assertThat(processing.status()).isEqualTo("processing");
        assertThat(completed.status()).isEqualTo("pending_acceptance");
        assertThatThrownBy(() -> scoped.accept("wo_accept_other", action(null, 5, "ok", "k_accept_other")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
        assertThat(scoped.accept("wo_accept_own", action(null, 5, "ok", "k_accept_own")).status())
                .isEqualTo("closed");
    }

    @Test
    void supervisorAcceptanceFlow_closesAndResetsEquipmentToNormal() {
        facility.store.put("feq_1", equipment("feq_1", "alarm"));
        FacilityWorkOrderRecord rec = service.create(req("repair", "high", "platform", "feq_1"));

        FacilityWorkOrderRecord assigned = service.assign(rec.id(), new FacilityWorkOrderAssignRequest("usr_ops", "k_assign"));
        FacilityWorkOrderRecord processing = service.start(assigned.id(), action(null, null, "start", "k_start"));
        FacilityWorkOrderRecord completed = service.complete(processing.id(), action("已修复", null, null, "k_done"));

        assertThat(completed.status()).isEqualTo("pending_acceptance");
        assertThat(facility.store.get("feq_1").status()).isEqualTo("alarm");

        FacilityWorkOrderRecord closed = service.accept(completed.id(), action(null, 5, "ok", "k_accept"));

        assertThat(closed.status()).isEqualTo("closed");
        assertThat(closed.verifiedAt()).isNotNull();
        assertThat(closed.verifierId()).isEqualTo("system");
        assertThat(closed.satisfaction()).isEqualTo(5);
        assertThat(facility.store.get("feq_1").status()).isEqualTo("normal");
        assertThat(facility.store.get("feq_1").alarmSource()).isNull();
    }

    @Test
    void complete_routesByPolicyHandlerAndReporter() {
        FacilityWorkOrderRecord cleaning = start(service.create(req("cleaning", "normal", "platform", null)));
        assertThat(service.complete(cleaning.id(), action("已清洁", null, null, "k_clean_done")).status())
                .isEqualTo("closed");

        FacilityWorkOrderRecord complaint = start(service.create(req("complaint", "normal", "platform", null)));
        FacilityWorkOrderRecord pendingReview =
                service.complete(complaint.id(), action("已解释并处理", null, null, "k_complaint_done"));
        assertThat(pendingReview.status()).isEqualTo("pending_review");
        assertThat(service.review(pendingReview.id(), action(null, 4, null, "k_review")).status())
                .isEqualTo("closed");
    }

    @Test
    void rejectsIllegalTransitionsAndBadSatisfaction() {
        FacilityWorkOrderRecord rec = service.create(req("repair", "high", "platform", null));

        assertThatThrownBy(() -> service.start(rec.id(), action(null, null, null, "k_start")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("INVALID_STATE"));

        FacilityWorkOrderRecord completed = service.complete(start(rec).id(), action("已修复", null, null, "k_done"));
        assertThatThrownBy(() -> service.accept(completed.id(), action(null, 6, null, "k_accept_bad")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void suspendAndResumeMovesBackToProcessing() {
        FacilityWorkOrderRecord processing = start(service.create(req("repair", "high", "platform", null)));

        FacilityWorkOrderRecord suspended = service.suspend(processing.id(), action(null, null, "等配件", "k_suspend"));
        FacilityWorkOrderRecord resumed = service.resume(suspended.id(), action(null, null, "配件到货", "k_resume"));

        assertThat(suspended.status()).isEqualTo("suspended");
        assertThat(resumed.status()).isEqualTo("processing");
    }

    @Test
    void markOverdueOnlyMarksActiveOrdersPastHandleDue() {
        Instant now = Instant.now();
        FacilityWorkOrderRecord overdue = raw("wo_overdue", "assigned", now.minusSeconds(60), false);
        FacilityWorkOrderRecord closed = raw("wo_closed", "closed", now.minusSeconds(60), false);
        FacilityWorkOrderRecord alreadyMarked = raw("wo_marked", "processing", now.minusSeconds(60), true);
        workOrders.insertWorkOrder(overdue);
        workOrders.insertWorkOrder(closed);
        workOrders.insertWorkOrder(alreadyMarked);

        assertThat(service.markOverdue("k_overdue")).isEqualTo(1);

        assertThat(workOrders.store.get("wo_overdue").overtime()).isTrue();
        assertThat(workOrders.store.get("wo_closed").overtime()).isFalse();
        assertThat(workOrders.store.get("wo_marked").overtime()).isTrue();
    }

    @Test
    void equipmentAlarmEventCreatesHighRepairOrder() {
        service.onEquipmentAlarmed(new FacilityEquipmentAlarmedEvent("feq_9", "冷机9号", "iot", "水温异常"));

        FacilityWorkOrderRecord rec = workOrders.store.values().iterator().next();
        assertThat(rec.workOrderType()).isEqualTo("repair");
        assertThat(rec.priority()).isEqualTo("high");
        assertThat(rec.source()).isEqualTo("equipment_alarm");
        assertThat(rec.equipmentId()).isEqualTo("feq_9");
        assertThat(rec.title()).contains("冷机9号");
        assertThat(rec.description()).isEqualTo("水温异常");
    }

    private FacilityWorkOrderRecord start(FacilityWorkOrderRecord rec) {
        FacilityWorkOrderRecord assigned = service.assign(rec.id(), new FacilityWorkOrderAssignRequest("usr_ops", "k_a" + rec.id()));
        return service.start(assigned.id(), action(null, null, null, "k_s" + rec.id()));
    }

    private FacilityWorkOrderRequest req(String type, String priority, String source, String equipmentId) {
        return new FacilityWorkOrderRequest(type, priority, type + " title", "desc", source, "sp_1",
                equipmentId, "usr_reporter", "staff", "proj_1", "mch_1", "k_" + type + priority + source);
    }

    private FacilityWorkOrderActionRequest action(String resolution, Integer satisfaction, String note, String key) {
        return new FacilityWorkOrderActionRequest(resolution, satisfaction, note, key);
    }

    private void seedPolicy(String type, String priority, int response, int handle, boolean requires, String acceptanceBy) {
        Instant now = Instant.now();
        workOrders.savePolicy(new FacilityWorkOrderPolicyRecord("fwop_" + type + "_" + priority,
                type, priority, response, handle, requires, acceptanceBy, true, now, now));
    }

    private FacilityEquipmentRecord equipment(String id, String status) {
        return new FacilityEquipmentRecord(id, "设备" + id, "hvac", "sp_1", null, null, null, "usr_eng",
                status, 90, null, LocalDate.now().plusDays(90), LocalDate.now(), null,
                "iot", "告警", Instant.now(), "proj_1", null, Instant.now(), Instant.now());
    }

    private FacilityWorkOrderRecord raw(String id, String status, Instant handleDueAt, boolean overtime) {
        Instant now = Instant.now();
        return new FacilityWorkOrderRecord(id, "FWO-" + id, "repair", "high", status, "title", "desc",
                "platform", null, null, null, null, null, now, handleDueAt, null, null, null, null, null, null,
                overtime, null, null, null, now, now);
    }

    private FacilityWorkOrderRecord rawAssigned(String id, String status, String assigneeId, String projectId) {
        Instant now = Instant.now();
        return new FacilityWorkOrderRecord(id, "FWO-" + id, "repair", "high", status, "title", "desc",
                "platform", "sp_1", null, "usr_reporter", "staff", assigneeId, now.plusSeconds(3600),
                now.plusSeconds(7200), now, null, null, null, null, null, false, null, projectId, "mch_1", now, now);
    }

    private FacilityWorkOrderService outsourcedService(String userId, String projectId) {
        authenticateOutsourced(userId, projectId);
        return new FacilityWorkOrderService(workOrders, facility, mock(AuditEventService.class),
                new AuthorizationService(null));
    }

    private void authenticateOutsourced(String userId, String projectId) {
        AccessActor actor = new AccessActor(userId, userId, "outsourced_vendor", projectId, null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    static class InMemoryWorkOrderRepository implements FacilityWorkOrderRepository {
        final Map<String, FacilityWorkOrderRecord> store = new LinkedHashMap<>();
        final Map<String, FacilityWorkOrderPolicyRecord> policies = new LinkedHashMap<>();

        @Override
        public Optional<FacilityWorkOrderPolicyRecord> findPolicy(String workOrderType, String priority) {
            FacilityWorkOrderPolicyRecord policy = policies.get(workOrderType + ":" + priority);
            return policy == null || !policy.active() ? Optional.empty() : Optional.of(policy);
        }

        @Override
        public FacilityWorkOrderPolicyRecord savePolicy(FacilityWorkOrderPolicyRecord record) {
            policies.put(record.workOrderType() + ":" + record.priority(), record);
            return record;
        }

        @Override
        public void insertWorkOrder(FacilityWorkOrderRecord record) {
            store.put(record.id(), record);
        }

        @Override
        public void updateWorkOrder(FacilityWorkOrderRecord record) {
            store.put(record.id(), record);
        }

        @Override
        public Optional<FacilityWorkOrderRecord> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<FacilityWorkOrderRecord> list() {
            return new ArrayList<>(store.values());
        }

        @Override
        public List<FacilityWorkOrderRecord> listOverdue(Instant now) {
            return store.values().stream()
                    .filter(r -> r.handleDueAt() != null && r.handleDueAt().isBefore(now))
                    .filter(r -> !r.overtime())
                    .filter(r -> FacilityWorkOrderService.ACTIVE_STATUSES.contains(r.status()))
                    .sorted(Comparator.comparing(FacilityWorkOrderRecord::handleDueAt))
                    .toList();
        }
    }

    static class InMemoryFacilityRepository implements FacilityRepository {
        final Map<String, FacilityEquipmentRecord> store = new LinkedHashMap<>();

        @Override
        public List<FacilityEquipmentRecord> listEquipment() {
            return new ArrayList<>(store.values());
        }

        @Override
        public Optional<FacilityEquipmentRecord> findEquipmentById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public void insertEquipment(FacilityEquipmentRecord record) {
            store.put(record.id(), record);
        }

        @Override
        public void updateEquipment(FacilityEquipmentRecord record) {
            store.put(record.id(), record);
        }

        @Override
        public List<FacilityEquipmentRecord> listMaintenanceDue(LocalDate asOf) {
            return List.of();
        }
    }
}
