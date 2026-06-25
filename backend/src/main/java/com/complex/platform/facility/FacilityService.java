package com.complex.platform.facility;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.FacilityEquipmentRequest;
import com.complex.platform.facility.dto.FacilityInspectionCompleteRequest;
import com.complex.platform.facility.dto.FacilityInspectionTemplateRequest;
import com.complex.platform.facility.dto.FacilityStatusRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderAssignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderStatusRequest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * ③ 楼宇运营 S1：楼宇机电设备台账（facility_equipment）服务。工单/巡检/能耗的引用地基。
 * 写 requirePlatformScope + 落审计；功能标志 facility.equipment.enabled 默认关（关→写拒，现状不变）。单构造器（无多构造 @Autowired 坑）。
 */
@Service
public class FacilityService {

    private static final Set<String> EQUIPMENT_TYPES = Set.of("hvac", "elevator", "fire", "power", "water", "weak_current", "other");
    private static final Set<String> STATUSES = Set.of("active", "maintenance", "retired");

    private static final Set<String> WO_CATEGORIES = Set.of("repair", "maintenance", "inspection_followup");
    private static final Set<String> WO_PRIORITIES = Set.of("low", "medium", "high", "critical");
    // ponytail: SLA 时长按 priority（小时），实验期默认值，按运营反馈调；常量即旋钮。
    private static final Map<String, Integer> SLA_HOURS = Map.of("critical", 4, "high", 24, "medium", 72, "low", 168);
    // 状态机：派单走 assign（→assigned）；其余流转走 status 端点。closed 终态。
    private static final Map<String, Set<String>> WO_TRANSITIONS = Map.of(
            "open", Set.of("in_progress", "closed"),
            "assigned", Set.of("in_progress", "closed"),
            "in_progress", Set.of("resolved", "closed"),
            "resolved", Set.of("closed", "in_progress"),
            "closed", Set.of());

    private static final Set<String> INSPECTION_RESULTS = Set.of("pass", "fail");

    @Value("${facility.equipment.enabled:false}")
    boolean facilityEquipmentEnabled;

    @Value("${facility.work_order.enabled:false}")
    boolean facilityWorkOrderEnabled;

    @Value("${facility.inspection.enabled:false}")
    boolean facilityInspectionEnabled;

    private final FacilityRepository repository;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public FacilityService(FacilityRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    public PageResponse<FacilityEquipmentRecord> listEquipment() {
        List<FacilityEquipmentRecord> items = repository.listEquipment();
        auditEventService.recordSuccess("facility.equipment.viewed", "facility.equipment.manage", "facility_equipment",
                "all", null, null, Map.of("count", items.size()));
        return new PageResponse<>(items, 1, 500, items.size());
    }

    public FacilityEquipmentRecord createEquipment(FacilityEquipmentRequest request) {
        authorizationService.requirePlatformScope();
        requireEnabled();
        FacilityEquipmentRequest actual = requireRequest(request);
        String type = requireEnum(actual.equipmentType(), EQUIPMENT_TYPES, "equipment_type");
        Integer cycle = actual.maintenanceCycleDays();
        if (cycle != null && cycle <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "maintenance_cycle_days must be positive");
        }
        Instant now = Instant.now();
        FacilityEquipmentRecord saved = repository.saveEquipment(new FacilityEquipmentRecord(
                "feq_" + UUID.randomUUID(), requireText(actual.name(), "name"), type, trimToNull(actual.spaceId()),
                trimToNull(actual.vendor()), trimToNull(actual.model()), actual.installDate(), actual.warrantyUntil(),
                "active", cycle, null, null, null, now, now));
        auditEventService.recordSuccess("facility.equipment.created", "facility.equipment.manage", "facility_equipment",
                saved.id(), null, actual.idempotencyKey(), Map.of("equipment_type", type));
        return saved;
    }

    public FacilityEquipmentRecord updateEquipmentStatus(String equipmentId, FacilityStatusRequest request) {
        authorizationService.requirePlatformScope();
        requireEnabled();
        String id = requireText(equipmentId, "equipment_id");
        FacilityStatusRequest actual = requireRequest(request);
        String status = requireEnum(actual.status(), STATUSES, "status");
        FacilityEquipmentRecord ex = repository.findEquipmentById(id)
                .orElseThrow(() -> ApiException.notFound("FACILITY_EQUIPMENT_NOT_FOUND", "equipment not found"));
        FacilityEquipmentRecord saved = repository.saveEquipment(new FacilityEquipmentRecord(
                ex.id(), ex.name(), ex.equipmentType(), ex.spaceId(), ex.vendor(), ex.model(), ex.installDate(),
                ex.warrantyUntil(), status, ex.maintenanceCycleDays(), ex.lastMaintenanceAt(), ex.projectId(),
                ex.merchantId(), ex.createdAt(), Instant.now()));
        auditEventService.recordSuccess("facility.equipment.status_changed", "facility.equipment.manage",
                "facility_equipment", id, null, actual.idempotencyKey(), Map.of("status", status));
        return saved;
    }

    public PageResponse<FacilityWorkOrderRecord> listWorkOrders() {
        List<FacilityWorkOrderRecord> items = repository.listWorkOrders();
        auditEventService.recordSuccess("facility.work_order.viewed", "facility.work_order.manage", "facility_work_orders",
                "all", null, null, Map.of("count", items.size()));
        return new PageResponse<>(items, 1, 500, items.size());
    }

    public FacilityWorkOrderRecord createWorkOrder(FacilityWorkOrderRequest request) {
        authorizationService.requirePlatformScope();
        requireWorkOrderEnabled();
        FacilityWorkOrderRequest actual = requireRequest(request);
        String category = requireEnum(actual.category(), WO_CATEGORIES, "category");
        String priority = actual.priority() == null || actual.priority().isBlank()
                ? "medium" : requireEnum(actual.priority(), WO_PRIORITIES, "priority");
        String equipmentId = trimToNull(actual.equipmentId());
        if (equipmentId != null && repository.findEquipmentById(equipmentId).isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "equipment_id not found");
        }
        Instant now = Instant.now();
        Instant slaDueAt = now.plus(SLA_HOURS.get(priority), ChronoUnit.HOURS);
        FacilityWorkOrderRecord saved = repository.saveWorkOrder(new FacilityWorkOrderRecord(
                "fwo_" + UUID.randomUUID(), equipmentId, trimToNull(actual.spaceId()), requireText(actual.title(), "title"),
                trimToNull(actual.description()), category, priority, "open", null, trimToNull(actual.reportedBy()),
                slaDueAt, null, null, null, now, now));
        auditEventService.recordSuccess("facility.work_order.created", "facility.work_order.manage", "facility_work_orders",
                saved.id(), null, actual.idempotencyKey(), Map.of("category", category, "priority", priority));
        return saved;
    }

    public FacilityWorkOrderRecord assignWorkOrder(String workOrderId, FacilityWorkOrderAssignRequest request) {
        authorizationService.requirePlatformScope();
        requireWorkOrderEnabled();
        String id = requireText(workOrderId, "work_order_id");
        FacilityWorkOrderAssignRequest actual = requireRequest(request);
        String assignee = requireText(actual.assignee(), "assignee");
        FacilityWorkOrderRecord ex = requireWorkOrder(id);
        if (!"open".equals(ex.status()) && !"assigned".equals(ex.status())) {
            throw ApiException.conflict("FACILITY_WORK_ORDER_NOT_ASSIGNABLE",
                    "work order cannot be assigned in status " + ex.status());
        }
        FacilityWorkOrderRecord saved = repository.saveWorkOrder(withStatus(ex, "assigned", assignee, ex.resolvedAt()));
        auditEventService.recordSuccess("facility.work_order.assigned", "facility.work_order.manage", "facility_work_orders",
                id, null, actual.idempotencyKey(), Map.of("assignee", assignee));
        return saved;
    }

    public FacilityWorkOrderRecord updateWorkOrderStatus(String workOrderId, FacilityWorkOrderStatusRequest request) {
        authorizationService.requirePlatformScope();
        requireWorkOrderEnabled();
        String id = requireText(workOrderId, "work_order_id");
        FacilityWorkOrderStatusRequest actual = requireRequest(request);
        String target = requireText(actual.status(), "status");
        FacilityWorkOrderRecord ex = requireWorkOrder(id);
        Set<String> allowed = WO_TRANSITIONS.getOrDefault(ex.status(), Set.of());
        if (!allowed.contains(target)) {
            throw ApiException.conflict("FACILITY_WORK_ORDER_INVALID_TRANSITION",
                    "cannot transition work order from " + ex.status() + " to " + target);
        }
        Instant resolvedAt = "resolved".equals(target) ? Instant.now() : ex.resolvedAt();
        FacilityWorkOrderRecord saved = repository.saveWorkOrder(withStatus(ex, target, ex.assignee(), resolvedAt));
        auditEventService.recordSuccess("facility.work_order.status_changed", "facility.work_order.manage",
                "facility_work_orders", id, null, actual.idempotencyKey(), Map.of("status", target));
        return saved;
    }

    private FacilityWorkOrderRecord requireWorkOrder(String id) {
        return repository.findWorkOrderById(id)
                .orElseThrow(() -> ApiException.notFound("FACILITY_WORK_ORDER_NOT_FOUND", "work order not found"));
    }

    private FacilityWorkOrderRecord withStatus(FacilityWorkOrderRecord ex, String status, String assignee, Instant resolvedAt) {
        return new FacilityWorkOrderRecord(ex.id(), ex.equipmentId(), ex.spaceId(), ex.title(), ex.description(),
                ex.category(), ex.priority(), status, assignee, ex.reportedBy(), ex.slaDueAt(), resolvedAt,
                ex.projectId(), ex.merchantId(), ex.createdAt(), Instant.now());
    }

    private void requireWorkOrderEnabled() {
        if (!facilityWorkOrderEnabled) {
            throw ApiException.conflict("FACILITY_WORK_ORDER_DISABLED", "facility work order module is disabled");
        }
    }

    public PageResponse<FacilityInspectionTemplateRecord> listInspectionTemplates() {
        List<FacilityInspectionTemplateRecord> items = repository.listInspectionTemplates();
        auditEventService.recordSuccess("facility.inspection.template_viewed", "facility.inspection.manage",
                "facility_inspection_templates", "all", null, null, Map.of("count", items.size()));
        return new PageResponse<>(items, 1, 500, items.size());
    }

    public FacilityInspectionTemplateRecord createInspectionTemplate(FacilityInspectionTemplateRequest request) {
        authorizationService.requirePlatformScope();
        requireInspectionEnabled();
        FacilityInspectionTemplateRequest actual = requireRequest(request);
        String name = requireText(actual.name(), "name");
        String type = actual.equipmentType() == null || actual.equipmentType().isBlank()
                ? null : requireEnum(actual.equipmentType(), EQUIPMENT_TYPES, "equipment_type");
        Instant now = Instant.now();
        FacilityInspectionTemplateRecord saved = repository.saveInspectionTemplate(new FacilityInspectionTemplateRecord(
                "fitpl_" + UUID.randomUUID(), name, type, actual.items() == null ? List.of() : actual.items(),
                "active", null, null, now, now));
        auditEventService.recordSuccess("facility.inspection.template_created", "facility.inspection.manage",
                "facility_inspection_templates", saved.id(), null, actual.idempotencyKey(), Map.of("name", name));
        return saved;
    }

    public PageResponse<FacilityInspectionTaskRecord> listInspectionTasks() {
        List<FacilityInspectionTaskRecord> items = repository.listInspectionTasks();
        auditEventService.recordSuccess("facility.inspection.task_viewed", "facility.inspection.manage",
                "facility_inspection_tasks", "all", null, null, Map.of("count", items.size()));
        return new PageResponse<>(items, 1, 500, items.size());
    }

    /**
     * 巡检到期生成（Job + 手动端点共用）：对每台 active 且设维保周期的设备，按 last_maintenance_at + 周期推 due_at，
     * 自然键防重 upsert；末尾把 pending 且超期的翻 overdue。返回新增任务数。
     */
    public int generateDueInspectionTasks(Instant now) {
        int upserted = 0;
        for (FacilityEquipmentRecord eq : repository.listEquipment()) {
            if (!"active".equals(eq.status()) || eq.maintenanceCycleDays() == null) {
                continue;
            }
            Instant dueAt = eq.lastMaintenanceAt() != null
                    ? eq.lastMaintenanceAt().plus(Duration.ofDays(eq.maintenanceCycleDays()))
                    : now; // 从未维保 → 立即到期
            boolean inserted = repository.upsertInspectionTask(new FacilityInspectionTaskRecord(
                    "fitask_" + UUID.randomUUID(), eq.id(), eq.spaceId(), dueAt, "pending", now,
                    null, null, eq.projectId(), eq.merchantId(), now, now));
            if (inserted) {
                upserted++;
            }
        }
        repository.markOverdueInspectionTasks(now);
        return upserted;
    }

    public int generateDueInspectionTasksManually() {
        authorizationService.requirePlatformScope();
        requireInspectionEnabled();
        int n = generateDueInspectionTasks(Instant.now());
        auditEventService.recordSuccess("facility.inspection.tasks_generated", "facility.inspection.manage",
                "facility_inspection_tasks", "all", null, null, Map.of("upserted", n));
        return n;
    }

    public FacilityInspectionRecordRecord completeInspectionTask(String taskId, FacilityInspectionCompleteRequest request) {
        authorizationService.requirePlatformScope();
        requireInspectionEnabled();
        String id = requireText(taskId, "task_id");
        FacilityInspectionCompleteRequest actual = requireRequest(request);
        String result = actual.result() == null || actual.result().isBlank()
                ? "pass" : requireEnum(actual.result(), INSPECTION_RESULTS, "result");
        FacilityInspectionTaskRecord task = repository.findInspectionTaskById(id)
                .orElseThrow(() -> ApiException.notFound("FACILITY_INSPECTION_TASK_NOT_FOUND", "inspection task not found"));
        if ("completed".equals(task.status())) {
            throw ApiException.conflict("FACILITY_INSPECTION_TASK_ALREADY_COMPLETED", "inspection task already completed");
        }
        Instant now = Instant.now();
        FacilityInspectionRecordRecord record = repository.saveInspectionRecord(new FacilityInspectionRecordRecord(
                "firec_" + UUID.randomUUID(), task.id(), task.equipmentId(), trimToNull(actual.inspector()), result,
                trimToNull(actual.findings()), actual.checkedItems() == null ? List.of() : actual.checkedItems(),
                task.projectId(), task.merchantId(), now));
        repository.saveInspectionTask(new FacilityInspectionTaskRecord(
                task.id(), task.equipmentId(), task.spaceId(), task.dueAt(), "completed", task.generatedAt(),
                record.id(), now, task.projectId(), task.merchantId(), task.createdAt(), now));
        // 巡检完成即推进设备维保时点，下一周期 due_at 顺延（闭环）。
        repository.findEquipmentById(task.equipmentId()).ifPresent(eq -> repository.saveEquipment(new FacilityEquipmentRecord(
                eq.id(), eq.name(), eq.equipmentType(), eq.spaceId(), eq.vendor(), eq.model(), eq.installDate(),
                eq.warrantyUntil(), eq.status(), eq.maintenanceCycleDays(), now, eq.projectId(), eq.merchantId(),
                eq.createdAt(), now)));
        auditEventService.recordSuccess("facility.inspection.task_completed", "facility.inspection.manage",
                "facility_inspection_tasks", id, null, actual.idempotencyKey(), Map.of("result", result));
        return record;
    }

    private void requireInspectionEnabled() {
        if (!facilityInspectionEnabled) {
            throw ApiException.conflict("FACILITY_INSPECTION_DISABLED", "facility inspection module is disabled");
        }
    }

    private void requireEnabled() {
        if (!facilityEquipmentEnabled) {
            throw ApiException.conflict("FACILITY_EQUIPMENT_DISABLED", "facility equipment registry is disabled");
        }
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String field) {
        String t = value == null ? "" : value.trim();
        if (t.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return t;
    }

    private String requireEnum(String value, Set<String> allowed, String field) {
        String t = requireText(value, field);
        if (!allowed.contains(t)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is invalid");
        }
        return t;
    }

    private String trimToNull(String value) {
        String t = value == null ? "" : value.trim();
        return t.isEmpty() ? null : t;
    }
}
