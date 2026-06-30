package com.complex.platform.facility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.FacilityInspectionExceptionReportRequest;
import com.complex.platform.facility.dto.FacilityInspectionRecheckConfirmRequest;
import com.complex.platform.facility.dto.FacilityInspectionRecordSubmitRequest;
import com.complex.platform.facility.dto.FacilityInspectionTaskRequest;
import com.complex.platform.facility.dto.FacilityInspectionTemplateRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Building inspection S3 unit tests: scheduling, state machine, records, abnormal event close loop. */
class FacilityInspectionServiceTests {

    private InMemoryInspectionRepository inspections;
    private List<Object> events;
    private FacilityInspectionService service;

    @BeforeEach
    void setUp() {
        inspections = new InMemoryInspectionRepository();
        events = new ArrayList<>();
        service = new FacilityInspectionService(inspections, mock(AuditEventService.class),
                mock(AuthorizationService.class), events::add);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTemplate_rejectsEnergyAndDefaultsLightMode() {
        assertThatThrownBy(() -> service.createTemplate(templateReq("energy", 30, null, true)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));

        FacilityInspectionTemplateRecord rec = service.createTemplate(templateReq("fire", 7, null, true));

        assertThat(rec.id()).startsWith("fit_");
        assertThat(rec.inspectionType()).isEqualTo("fire");
        assertThat(rec.abnormalMode()).isEqualTo("light");
        assertThat(rec.active()).isTrue();
    }

    @Test
    void generateScheduledTasksCreatesDuePendingTasksAndDedupesByTemplateDate() {
        FacilityInspectionTemplateRecord weekly = service.createTemplate(templateReq("security", 7, "light", true));
        service.createTemplate(templateReq("environment", null, "light", true));
        LocalDate day = LocalDate.parse("2026-06-26");

        assertThat(service.generateScheduledTasks(day)).isEqualTo(1);
        assertThat(service.generateScheduledTasks(day)).isZero();
        assertThat(service.generateScheduledTasks(day.plusDays(6))).isZero();
        assertThat(service.generateScheduledTasks(day.plusDays(7))).isEqualTo(1);

        assertThat(inspections.tasks.values()).extracting(FacilityInspectionTaskRecord::templateId)
                .containsExactly(weekly.id(), weekly.id());
        assertThat(inspections.tasks.values()).extracting(FacilityInspectionTaskRecord::status)
                .containsOnly("pending");
    }

    @Test
    void manualTaskStartAndNormalRecordClosesTaskWithEvidence() {
        FacilityInspectionTaskRecord task = service.createTask(taskReq(null, "environment", "manual", null));
        FacilityInspectionTaskRecord started = service.start(task.id(), action("k_start"));

        FacilityInspectionRecordRecord record = service.submitRecord(started.id(), submit("normal", null, "k_submit"));

        FacilityInspectionTaskRecord closed = inspections.tasks.get(task.id());
        assertThat(record.taskId()).isEqualTo(task.id());
        assertThat(record.photos()).containsExactly("https://cdn.example/inspection.jpg");
        assertThat(record.qrCode()).isEqualTo("qr_1");
        assertThat(record.gpsLat()).isEqualByComparingTo("31.2304");
        assertThat(closed.status()).isEqualTo("closed");
        assertThat(closed.completedAt()).isNotNull();
        assertThat(events).isEmpty();
    }

    @Test
    void outsourcedTaskListDetailAndRecordsAreLimitedToOwnAssigneeAndProject() {
        FacilityInspectionService scoped = outsourcedService("usr_vendor_s", "proj_1");
        inspections.insertTask(task("task_own", "pending", "usr_vendor_s", "proj_1"));
        inspections.insertTask(task("task_other", "pending", "usr_vendor_b", "proj_1"));
        inspections.insertTask(task("task_cross_project", "pending", "usr_vendor_s", "proj_2"));

        PageResponse<FacilityInspectionTaskRecord> page = scoped.listTasks();

        assertThat(page.items()).extracting(FacilityInspectionTaskRecord::id).containsExactly("task_own");
        assertThat(scoped.findTaskById("task_own").id()).isEqualTo("task_own");
        assertThatThrownBy(() -> scoped.findTaskById("task_other"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
        assertThatThrownBy(() -> scoped.findTaskById("task_cross_project"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
        assertThatThrownBy(() -> scoped.listRecords("task_other"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
    }

    @Test
    void outsourcedCanRecordOwnInspectionButCannotRecordOthersOrRecheck() {
        FacilityInspectionService scoped = outsourcedService("usr_vendor_s", "proj_1");
        inspections.insertTask(task("task_own", "pending", "usr_vendor_s", "proj_1"));
        inspections.insertTask(task("task_other", "pending", "usr_vendor_b", "proj_1"));
        inspections.insertTask(task("task_other_started", "in_progress", "usr_vendor_b", "proj_1"));
        inspections.insertTask(task("task_rectifying", "rectifying", "usr_vendor_s", "proj_1"));

        assertThatThrownBy(() -> scoped.start("task_other", action("k_start_other")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
        FacilityInspectionTaskRecord started = scoped.start("task_own", action("k_start_own"));
        FacilityInspectionRecordRecord record = scoped.submitRecord(started.id(), submit("normal", null, "k_submit_own"));

        assertThat(started.status()).isEqualTo("in_progress");
        assertThat(record.taskId()).isEqualTo("task_own");
        assertThat(inspections.tasks.get("task_own").status()).isEqualTo("closed");
        assertThatThrownBy(() -> scoped.submitRecord("task_other_started", submit("normal", null, "k_submit_other")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
        assertThatThrownBy(() -> scoped.recheck("task_rectifying", action("k_recheck")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
    }

    @Test
    void outsourcedCanReportExceptionForOwnTaskAndTriggersWorkOrderPath() {
        FacilityInspectionTemplateRecord template =
                service.createTemplate(templateReq("fire", 1, "to_work_order", true));
        FacilityInspectionService scoped = outsourcedService("usr_vendor_s", "proj_1");
        inspections.insertTask(task("task_own", "pending", "usr_vendor_s", "proj_1", template.id()));
        inspections.insertTask(task("task_other", "pending", "usr_vendor_b", "proj_1", template.id()));

        FacilityInspectionRecordRecord record = scoped.reportException("task_own", exceptionReq("k_exception"));

        assertThat(record.result()).isEqualTo("abnormal");
        assertThat(record.inspectorId()).isEqualTo("usr_vendor_s");
        assertThat(inspections.tasks.get("task_own").status()).isEqualTo("rectifying");
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOfSatisfying(FacilityInspectionAbnormalEvent.class,
                event -> assertThat(event.taskId()).isEqualTo("task_own"));
        assertThatThrownBy(() -> scoped.reportException("task_other", exceptionReq("k_exception_other")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FORBIDDEN"));
    }

    @Test
    void abnormalLightModeRecordsAndClosesWithoutWorkOrderEvent() {
        FacilityInspectionTemplateRecord template = service.createTemplate(templateReq("facility", 3, "light", true));
        FacilityInspectionTaskRecord task = service.createTask(taskReq(template.id(), null, "manual", null));
        service.start(task.id(), action("k_start"));

        service.submitRecord(task.id(), submit("abnormal", "门禁松动", "k_abnormal"));

        FacilityInspectionTaskRecord closed = inspections.tasks.get(task.id());
        assertThat(closed.status()).isEqualTo("closed");
        assertThat(closed.workOrderId()).isNull();
        assertThat(inspections.records.get(0).abnormalNote()).isEqualTo("门禁松动");
        assertThat(events).isEmpty();
    }

    @Test
    void abnormalToWorkOrderPublishesEventCreatesRectificationOrderAndAllowsRecheckClose() {
        FacilityWorkOrderServiceTests.InMemoryWorkOrderRepository workOrders =
                new FacilityWorkOrderServiceTests.InMemoryWorkOrderRepository();
        seedPolicy(workOrders, "rectification", "normal", 240, 4320, true, "supervisor");
        FacilityWorkOrderService workOrderService = new FacilityWorkOrderService(workOrders,
                new FacilityWorkOrderServiceTests.InMemoryFacilityRepository(), mock(AuditEventService.class),
                mock(AuthorizationService.class), inspections);
        service = new FacilityInspectionService(inspections, mock(AuditEventService.class),
                mock(AuthorizationService.class), event -> workOrderService.onInspectionAbnormal((FacilityInspectionAbnormalEvent) event));

        FacilityInspectionTemplateRecord template =
                service.createTemplate(templateReq("fire", 1, "to_work_order", true));
        FacilityInspectionTaskRecord task = service.createTask(taskReq(template.id(), null, "route",
                List.of(Map.of("name", "B1 fire pump room"))));
        service.start(task.id(), action("k_start"));

        service.submitRecord(task.id(), submit("abnormal", "消防泵房渗水", "k_abnormal"));

        FacilityInspectionTaskRecord rectifying = inspections.tasks.get(task.id());
        assertThat(rectifying.status()).isEqualTo("rectifying");
        assertThat(rectifying.workOrderId()).startsWith("fwo_");
        FacilityWorkOrderRecord workOrder = workOrders.store.get(rectifying.workOrderId());
        assertThat(workOrder.workOrderType()).isEqualTo("rectification");
        assertThat(workOrder.source()).isEqualTo("inspection");
        assertThat(workOrder.spaceId()).isEqualTo("sp_1");
        assertThat(workOrder.title()).contains("巡检异常整改");

        FacilityInspectionTaskRecord rechecked = service.recheck(task.id(), action("k_recheck"));
        FacilityInspectionTaskRecord closed = service.supervisorConfirm(task.id(), action("k_confirm"));

        assertThat(rechecked.status()).isEqualTo("rechecked");
        assertThat(closed.status()).isEqualTo("closed");
    }

    @Test
    void rejectsIllegalTransitionsAndMissingAbnormalNote() {
        FacilityInspectionTaskRecord task = service.createTask(taskReq(null, "security", "manual", null));
        assertThatThrownBy(() -> service.submitRecord(task.id(), submit("normal", null, "k_submit")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("INVALID_STATE"));

        service.start(task.id(), action("k_start"));
        assertThatThrownBy(() -> service.submitRecord(task.id(), submit("abnormal", null, "k_bad")))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    private FacilityInspectionTemplateRequest templateReq(
            String type, Integer cycleDays, String abnormalMode, boolean active) {
        return new FacilityInspectionTemplateRequest("楼宇巡检模板", type, cycleDays, abnormalMode,
                List.of(Map.of("item", "door")), "scope", active, "proj_1", "k_tpl_" + type + cycleDays);
    }

    private FacilityInspectionTaskRequest taskReq(
            String templateId, String type, String scheduleMode, List<Map<String, Object>> routePoints) {
        return new FacilityInspectionTaskRequest(templateId, type, "楼宇巡检任务", scheduleMode, routePoints,
                "usr_ops", LocalDate.parse("2026-06-26"), LocalDate.parse("2026-06-26"),
                "proj_1", "sp_1", "k_task_" + scheduleMode);
    }

    private FacilityInspectionRecheckConfirmRequest action(String key) {
        return new FacilityInspectionRecheckConfirmRequest("note", key);
    }

    private FacilityInspectionRecordSubmitRequest submit(String result, String abnormalNote, String key) {
        return new FacilityInspectionRecordSubmitRequest(result, List.of("https://cdn.example/inspection.jpg"),
                "qr_1", new BigDecimal("31.2304"), new BigDecimal("121.4737"),
                List.of(Map.of("item", "door", "result", result)), "note", abnormalNote, "usr_inspector", key);
    }

    private FacilityInspectionExceptionReportRequest exceptionReq(String key) {
        return new FacilityInspectionExceptionReportRequest(List.of("https://cdn.example/inspection.jpg"),
                "qr_1", new BigDecimal("31.2304"), new BigDecimal("121.4737"),
                List.of(Map.of("item", "door", "result", "abnormal")), "note",
                "door is broken", "ignored_inspector", key);
    }

    private FacilityInspectionTaskRecord task(String id, String status, String assigneeId, String projectId) {
        return task(id, status, assigneeId, projectId, null);
    }

    private FacilityInspectionTaskRecord task(
            String id, String status, String assigneeId, String projectId, String templateId) {
        Instant now = Instant.now();
        return new FacilityInspectionTaskRecord(id, templateId, "security", status, "task " + id, "manual",
                List.of(), assigneeId, LocalDate.parse("2026-06-26"), LocalDate.parse("2026-06-26"),
                "in_progress".equals(status) ? now : null, null, null, projectId, "sp_1", now, now);
    }

    private FacilityInspectionService outsourcedService(String userId, String projectId) {
        authenticateOutsourced(userId, projectId);
        return new FacilityInspectionService(inspections, mock(AuditEventService.class),
                new AuthorizationService(null), events::add);
    }

    private void authenticateOutsourced(String userId, String projectId) {
        AccessActor actor = new AccessActor(userId, userId, "outsourced_vendor", projectId, null, "active");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(actor, "token", List.of()));
    }

    private void seedPolicy(FacilityWorkOrderServiceTests.InMemoryWorkOrderRepository workOrders,
            String type, String priority, int response, int handle, boolean requires, String acceptanceBy) {
        Instant now = Instant.now();
        workOrders.savePolicy(new FacilityWorkOrderPolicyRecord("fwop_" + type + "_" + priority,
                type, priority, response, handle, requires, acceptanceBy, true, now, now));
    }

    static class InMemoryInspectionRepository implements FacilityInspectionRepository {
        final Map<String, FacilityInspectionTemplateRecord> templates = new LinkedHashMap<>();
        final Map<String, FacilityInspectionTaskRecord> tasks = new LinkedHashMap<>();
        final List<FacilityInspectionRecordRecord> records = new ArrayList<>();

        @Override
        public List<FacilityInspectionTemplateRecord> listTemplates() {
            return new ArrayList<>(templates.values());
        }

        @Override
        public List<FacilityInspectionTemplateRecord> listSchedulableTemplates() {
            return templates.values().stream()
                    .filter(FacilityInspectionTemplateRecord::active)
                    .filter(t -> t.cycleDays() != null)
                    .toList();
        }

        @Override
        public Optional<FacilityInspectionTemplateRecord> findTemplateById(String id) {
            return Optional.ofNullable(templates.get(id));
        }

        @Override
        public void insertTemplate(FacilityInspectionTemplateRecord record) {
            templates.put(record.id(), record);
        }

        @Override
        public List<FacilityInspectionTaskRecord> listTasks() {
            return new ArrayList<>(tasks.values());
        }

        @Override
        public Optional<FacilityInspectionTaskRecord> findTaskById(String id) {
            return Optional.ofNullable(tasks.get(id));
        }

        @Override
        public Optional<LocalDate> latestScheduledDateForTemplate(String templateId) {
            return tasks.values().stream()
                    .filter(t -> templateId.equals(t.templateId()))
                    .map(FacilityInspectionTaskRecord::scheduledDate)
                    .filter(d -> d != null)
                    .max(Comparator.naturalOrder());
        }

        @Override
        public boolean existsTaskForTemplateDate(String templateId, LocalDate scheduledDate) {
            return tasks.values().stream()
                    .anyMatch(t -> templateId.equals(t.templateId()) && scheduledDate.equals(t.scheduledDate()));
        }

        @Override
        public void insertTask(FacilityInspectionTaskRecord record) {
            tasks.put(record.id(), record);
        }

        @Override
        public void updateTask(FacilityInspectionTaskRecord record) {
            tasks.put(record.id(), record);
        }

        @Override
        public void updateTaskWorkOrderId(String taskId, String workOrderId, Instant updatedAt) {
            FacilityInspectionTaskRecord t = tasks.get(taskId);
            tasks.put(taskId, new FacilityInspectionTaskRecord(t.id(), t.templateId(), t.inspectionType(), t.status(),
                    t.title(), t.scheduleMode(), t.routePoints(), t.assigneeId(), t.scheduledDate(), t.dueDate(),
                    t.startedAt(), t.completedAt(), workOrderId, t.projectId(), t.spaceId(), t.createdAt(), updatedAt));
        }

        @Override
        public void insertRecord(FacilityInspectionRecordRecord record) {
            records.add(record);
        }

        @Override
        public List<FacilityInspectionRecordRecord> listRecords(String taskId) {
            return records.stream().filter(r -> taskId.equals(r.taskId())).toList();
        }
    }
}
