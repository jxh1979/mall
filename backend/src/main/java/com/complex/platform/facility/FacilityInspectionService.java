package com.complex.platform.facility;

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
import com.complex.platform.governance.PlatformService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.trimToNull;
import static com.complex.platform.common.Strings.requireText;

/** Building inspection service (S3, V167). */
@Service
public class FacilityInspectionService {

    static final Set<String> TYPES = Set.of("security", "fire", "facility", "environment");
    static final Set<String> STATUSES =
            Set.of("pending", "in_progress", "normal", "abnormal", "rectifying", "rechecked", "closed");
    static final Set<String> SCHEDULE_MODES = Set.of("template", "manual", "route");
    static final Set<String> ABNORMAL_MODES = Set.of("light", "to_work_order");
    static final Set<String> RESULTS = Set.of("normal", "abnormal");
    private static final String PERM = "facility.inspection.manage";
    private static final String TEMPLATE_BUSINESS_TYPE = "facility_inspection_template";
    private static final String TASK_BUSINESS_TYPE = "facility_inspection_task";
    private static final String RECORD_BUSINESS_TYPE = "facility_inspection_record";
    private static final String REPORT_EXCEPTION_PERM = "facility.inspection.report_exception_assigned";

    private final FacilityInspectionRepository repository;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final ApplicationEventPublisher eventPublisher;
    private final PlatformService platformService;

    @Autowired
    public FacilityInspectionService(FacilityInspectionRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, ApplicationEventPublisher eventPublisher, PlatformService platformService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.eventPublisher = eventPublisher;
        this.platformService = platformService;
    }

    /** 测试与内部装配用：不带平台服务（新建模板与任务照旧取请求里的项目，见 {@link #landingProject}）。 */
    public FacilityInspectionService(FacilityInspectionRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, ApplicationEventPublisher eventPublisher) {
        this(repository, auditEventService, authorizationService, eventPublisher, null);
    }

    public PageResponse<FacilityInspectionTemplateRecord> listTemplates() {
        AccessActor actor = authorizationService.currentActor();
        List<FacilityInspectionTemplateRecord> items = authorizationService.isOutsourcedVendor(actor)
                ? repository.listTemplatesByProject(authorizationService.requireProjectScope(actor))
                : inReach(repository.listTemplates(), FacilityInspectionTemplateRecord::projectId);
        return new PageResponse<>(items, 1, 500, items.size());
    }

    public PageResponse<FacilityInspectionTaskRecord> listTasks() {
        AccessActor actor = requireScopeActor();
        List<FacilityInspectionTaskRecord> items = authorizationService.isOutsourcedVendor(actor)
                ? repository.listTasksAssignedTo(actor.id(), authorizationService.requireProjectScope(actor))
                : inReach(repository.listTasks(), FacilityInspectionTaskRecord::projectId);
        return new PageResponse<>(items, 1, 500, items.size());
    }

    public FacilityInspectionTaskRecord findTaskById(String id) {
        AccessActor actor = requireScopeActor();
        FacilityInspectionTaskRecord task = loadTask(id);
        enforceAssignedScope(actor, task);
        return task;
    }

    public PageResponse<FacilityInspectionRecordRecord> listRecords(String taskId) {
        AccessActor actor = requireScopeActor();
        enforceAssignedScope(actor, loadTask(taskId));
        List<FacilityInspectionRecordRecord> items = repository.listRecords(taskId);
        return new PageResponse<>(items, 1, 500, items.size());
    }

    @Transactional
    public FacilityInspectionTemplateRecord createTemplate(FacilityInspectionTemplateRequest request) {
        authorizationService.requirePlatformScope();
        FacilityInspectionTemplateRequest r = requireTemplate(request);
        String type = normalizeAllowed(r.inspectionType(), "inspection_type", TYPES);
        String abnormalMode = normalizeAllowed(defaultValue(r.abnormalMode(), "light"), "abnormal_mode", ABNORMAL_MODES);
        Integer cycleDays = validateCycle(r.cycleDays());
        Instant now = Instant.now();
        FacilityInspectionTemplateRecord rec = new FacilityInspectionTemplateRecord(
                "fit_" + UUID.randomUUID(), requireText(r.name(), "name"), type, cycleDays, abnormalMode,
                listOrEmpty(r.checklist()), trimToNull(r.scopeNote()), r.active() == null || r.active(),
                landingProject(r.projectId(), null, null), now, now);
        repository.insertTemplate(rec);
        audit("facility.inspection_template_created", TEMPLATE_BUSINESS_TYPE, rec.id(), r.idempotencyKey(),
                Map.of("type", type, "abnormal_mode", abnormalMode));
        return rec;
    }

    @Transactional
    public FacilityInspectionTaskRecord createTask(FacilityInspectionTaskRequest request) {
        authorizationService.requirePlatformScope();
        FacilityInspectionTaskRequest r = requireTask(request);
        FacilityInspectionTaskRecord rec = newTask(r, Instant.now());
        repository.insertTask(rec);
        audit("facility.inspection_task_created", TASK_BUSINESS_TYPE, rec.id(), r.idempotencyKey(),
                Map.of("status", rec.status(), "schedule_mode", rec.scheduleMode()));
        return rec;
    }

    @Transactional
    public FacilityInspectionTaskRecord start(String id, FacilityInspectionRecheckConfirmRequest request) {
        AccessActor actor = requireScopeActor();
        FacilityInspectionRecheckConfirmRequest r = requireAction(request);
        FacilityInspectionTaskRecord cur = loadTask(id);
        enforceAssignedScope(actor, cur);
        requireStatus(cur, "pending", "只有待巡检的任务才能开始");
        Instant now = Instant.now();
        FacilityInspectionTaskRecord started = withTaskState(cur, "in_progress", now, cur.completedAt(), now);
        repository.updateTask(started);
        audit("facility.inspection_task_started", TASK_BUSINESS_TYPE, started.id(), r.idempotencyKey(),
                metadata(started.status(), r.note()));
        return started;
    }

    @Transactional
    public FacilityInspectionRecordRecord submitRecord(String taskId, FacilityInspectionRecordSubmitRequest request) {
        AccessActor actor = requireScopeActor();
        FacilityInspectionRecordSubmitRequest r = requireRecord(request);
        FacilityInspectionTaskRecord cur = loadTask(taskId);
        enforceAssignedScope(actor, cur);
        requireStatus(cur, "in_progress", "只有已开始的巡检任务才能提交");
        String result = normalizeAllowed(r.result(), "result", RESULTS);
        String abnormalNote = trimToNull(r.abnormalNote());
        if ("abnormal".equals(result) && abnormalNote == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "abnormal_note is required");
        }
        validateGps(r.gpsLat(), r.gpsLng());

        Instant now = Instant.now();
        FacilityInspectionRecordRecord rec = new FacilityInspectionRecordRecord(
                "firec_" + UUID.randomUUID(), cur.id(), result, stringsOrEmpty(r.photos()), trimToNull(r.qrCode()),
                r.gpsLat(), r.gpsLng(), listOrEmpty(r.checklistResult()), trimToNull(r.note()), abnormalNote,
                requireText(r.inspectorId(), "inspector_id"), now, now);
        repository.insertRecord(rec);

        String finalStatus = applySubmitTransition(cur, rec, now);
        audit("facility.inspection_record_submitted", RECORD_BUSINESS_TYPE, rec.id(), r.idempotencyKey(),
                Map.of("task_id", cur.id(), "result", result, "task_status", finalStatus));
        return rec;
    }

    @Transactional
    public FacilityInspectionRecordRecord reportException(String taskId, FacilityInspectionExceptionReportRequest request) {
        AccessActor actor = requireScopeActor();
        FacilityInspectionExceptionReportRequest r = requireException(request);
        FacilityInspectionTaskRecord cur = loadTask(taskId);
        enforceAssignedScope(actor, cur);
        if (!Set.of("pending", "in_progress").contains(cur.status())) {
            throw ApiException.badRequest("INVALID_STATE", "只有待巡检或巡检中的任务才能上报异常");
        }
        validateGps(r.gpsLat(), r.gpsLng());
        String abnormalNote = requireText(r.abnormalNote(), "abnormal_note");
        Instant now = Instant.now();
        FacilityInspectionTaskRecord started = cur;
        if ("pending".equals(cur.status())) {
            started = withTaskState(cur, "in_progress", now, cur.completedAt(), now);
            repository.updateTask(started);
        }
        FacilityInspectionRecordRecord rec = new FacilityInspectionRecordRecord(
                "firec_" + UUID.randomUUID(), started.id(), "abnormal", stringsOrEmpty(r.photos()),
                trimToNull(r.qrCode()), r.gpsLat(), r.gpsLng(), listOrEmpty(r.checklistResult()),
                trimToNull(r.note()), abnormalNote, actor.id(), now, now);
        repository.insertRecord(rec);

        String finalStatus = applySubmitTransition(started, rec, now);
        audit("facility.inspection_exception_reported", REPORT_EXCEPTION_PERM, RECORD_BUSINESS_TYPE,
                rec.id(), r.idempotencyKey(), Map.of("task_id", started.id(), "task_status", finalStatus));
        return rec;
    }

    @Transactional
    public FacilityInspectionTaskRecord recheck(String id, FacilityInspectionRecheckConfirmRequest request) {
        authorizationService.requirePlatformScope();
        FacilityInspectionRecheckConfirmRequest r = requireAction(request);
        FacilityInspectionTaskRecord cur = loadTask(id);
        requireReach(cur.projectId());
        requireStatus(cur, "rectifying", "只有整改中的巡检任务才能复检");
        Instant now = Instant.now();
        FacilityInspectionTaskRecord rechecked = withTaskState(cur, "rechecked", cur.startedAt(), cur.completedAt(), now);
        repository.updateTask(rechecked);
        audit("facility.inspection_task_rechecked", TASK_BUSINESS_TYPE, rechecked.id(), r.idempotencyKey(),
                metadata(rechecked.status(), r.note()));
        return rechecked;
    }

    @Transactional
    public FacilityInspectionTaskRecord supervisorConfirm(String id, FacilityInspectionRecheckConfirmRequest request) {
        authorizationService.requirePlatformScope();
        FacilityInspectionRecheckConfirmRequest r = requireAction(request);
        FacilityInspectionTaskRecord cur = loadTask(id);
        requireReach(cur.projectId());
        requireStatus(cur, "rechecked", "只有已复检的巡检任务才能确认");
        Instant now = Instant.now();
        FacilityInspectionTaskRecord closed = withTaskState(cur, "closed", cur.startedAt(), now, now);
        repository.updateTask(closed);
        audit("facility.inspection_task_closed", TASK_BUSINESS_TYPE, closed.id(), r.idempotencyKey(),
                metadata(closed.status(), r.note()));
        return closed;
    }

    @Transactional
    public int generateScheduledTasks(LocalDate asOf) {
        authorizationService.requirePlatformScope();
        LocalDate scheduledDate = asOf == null ? LocalDate.now() : asOf;
        int created = 0;
        List<FacilityInspectionTemplateRecord> templates = repository.listSchedulableTemplates();
        // F3（审计三补三）：模板排程统计一次取全（原实现逐模板 2 次查询 = 2N）。
        var scheduleStats = repository.templateScheduleStats(templates, scheduledDate);
        for (FacilityInspectionTemplateRecord template : templates) {
            var stat = scheduleStats.get(template.id());
            if (template.cycleDays() == null || (stat != null && stat.hasOnDate())) {
                continue;
            }
            if (stat != null && stat.latest() != null
                    && stat.latest().plusDays(template.cycleDays()).isAfter(scheduledDate)) {
                continue;
            }
            Instant now = Instant.now();
            FacilityInspectionTaskRecord task = new FacilityInspectionTaskRecord(
                    "fitask_" + UUID.randomUUID(), template.id(), template.inspectionType(), "pending",
                    template.name(), "template", List.of(), null, scheduledDate, scheduledDate,
                    null, null, null, template.projectId(), null, now, now);
            repository.insertTask(task);
            audit("facility.inspection_task_scheduled", TASK_BUSINESS_TYPE, task.id(), null,
                    Map.of("template_id", template.id(), "scheduled_date", scheduledDate.toString()));
            created++;
        }
        return created;
    }

    private FacilityInspectionTaskRecord newTask(FacilityInspectionTaskRequest r, Instant now) {
        String templateId = trimToNull(r.templateId());
        FacilityInspectionTemplateRecord template = templateId == null ? null : loadTemplate(templateId);
        String type = template == null
                ? normalizeAllowed(r.inspectionType(), "inspection_type", TYPES)
                : template.inspectionType();
        String scheduleMode = normalizeAllowed(defaultValue(r.scheduleMode(), "manual"), "schedule_mode", SCHEDULE_MODES);
        if ("template".equals(scheduleMode) && template == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "模板排程必须指定模板（template_id）");
        }
        List<Map<String, Object>> routePoints = listOrEmpty(r.routePoints());
        if ("route".equals(scheduleMode) && routePoints.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "路线排程必须填写巡检点位（route_points）");
        }
        if (r.scheduledDate() != null && r.dueDate() != null && r.dueDate().isBefore(r.scheduledDate())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "截止日期不能早于计划日期");
        }
        LocalDate dueDate = r.dueDate() == null ? r.scheduledDate() : r.dueDate();
        String title = trimToNull(r.title());
        if (title == null && template != null) {
            title = template.name();
        }
        return new FacilityInspectionTaskRecord(
                "fitask_" + UUID.randomUUID(), templateId, type, "pending", requireText(title, "title"),
                scheduleMode, routePoints, trimToNull(r.assigneeId()), r.scheduledDate(), dueDate, null, null,
                null, landingProject(r.projectId(), template == null ? null : template.projectId(), r.spaceId()),
                trimToNull(r.spaceId()), now, now);
    }

    private String applySubmitTransition(FacilityInspectionTaskRecord cur, FacilityInspectionRecordRecord rec, Instant now) {
        if ("normal".equals(rec.result())) {
            FacilityInspectionTaskRecord normal = withTaskState(cur, "normal", cur.startedAt(), now, now);
            repository.updateTask(normal);
            FacilityInspectionTaskRecord closed = withTaskState(normal, "closed", normal.startedAt(), now, now);
            repository.updateTask(closed);
            return closed.status();
        }

        FacilityInspectionTaskRecord abnormal = withTaskState(cur, "abnormal", cur.startedAt(), null, now);
        repository.updateTask(abnormal);
        if ("to_work_order".equals(abnormalModeFor(cur))) {
            FacilityInspectionTaskRecord rectifying = withTaskState(abnormal, "rectifying", abnormal.startedAt(), null, now);
            repository.updateTask(rectifying);
            eventPublisher.publishEvent(new FacilityInspectionAbnormalEvent(
                    rectifying.id(), rectifying.title(), rec.abnormalNote(), rectifying.projectId(),
                    rectifying.spaceId(), rec.inspectorId()));
            return rectifying.status();
        }
        FacilityInspectionTaskRecord closed = withTaskState(abnormal, "closed", abnormal.startedAt(), now, now);
        repository.updateTask(closed);
        return closed.status();
    }

    private String abnormalModeFor(FacilityInspectionTaskRecord task) {
        String templateId = trimToNull(task.templateId());
        if (templateId == null) {
            return "light";
        }
        return repository.findTemplateById(templateId)
                .map(FacilityInspectionTemplateRecord::abnormalMode)
                .orElse("light");
    }

    private FacilityInspectionTaskRecord withTaskState(
            FacilityInspectionTaskRecord cur, String status, Instant startedAt, Instant completedAt, Instant updatedAt) {
        return new FacilityInspectionTaskRecord(cur.id(), cur.templateId(), cur.inspectionType(), status,
                cur.title(), cur.scheduleMode(), cur.routePoints(), cur.assigneeId(), cur.scheduledDate(),
                cur.dueDate(), startedAt, completedAt, cur.workOrderId(), cur.projectId(), cur.spaceId(),
                cur.createdAt(), updatedAt);
    }

    private FacilityInspectionTemplateRecord loadTemplate(String id) {
        return repository.findTemplateById(id)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "巡检模板不存在或已被删除"));
    }

    private FacilityInspectionTaskRecord loadTask(String id) {
        return repository.findTaskById(id)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "巡检任务不存在或已被删除"));
    }

    private AccessActor requireScopeActor() {
        AccessActor actor = authorizationService.currentActor();
        if (!authorizationService.isOutsourcedVendor(actor)) {
            authorizationService.requirePlatformScope();
        }
        return actor;
    }

    private void enforceAssignedScope(AccessActor actor, FacilityInspectionTaskRecord task) {
        authorizationService.enforceOutsourcedAssigneeScope(actor, task.assigneeId(), task.projectId());
        if (!authorizationService.isOutsourcedVendor(actor)) {
            requireReach(task.projectId());
        }
    }

    /**
     * 第十八批 G 段 82b-R1（治理地图第 659 行「在所分配的权限内可以查看、处理该角色可查看项目的数据」「逐个勾选的只能查看被选中的项目」；
     * 裁决 M737-iter7「行本身没有项目的不出」）：巡检任务项目须在 facility.inspection.manage 可触及范围内（平台管理员不限），
     * 项目为空的只给平台管理员；否则 403 CROSS_PROJECT_FORBIDDEN。外包维保账号照旧只按派给本人、本项目判。
     */
    private void requireReach(String projectId) {
        if (!authorizationService.canReachProject(authorizationService.currentActor(), projectId, PERM)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", "该记录不在本权限可触及的项目范围内");
        }
    }

    /** 82b-R1：列表只留可触及项目内的模板或任务（同上口径）。 */
    private <T> List<T> inReach(List<T> items, java.util.function.Function<T, String> projectOf) {
        AuthorizationService.ProjectReach reach = authorizationService.projectReach(authorizationService.currentActor(), PERM);
        return items.stream().filter(item -> reach.covers(projectOf.apply(item))).toList();
    }

    /**
     * 第十八批 G 段 82b-R2（治理地图第 659 行「角色能查看哪些项目，持有这类角色的员工就能在哪些项目新建记录」；第 413 行「所有基础对象都要带
     * “综合体项目归属”」）：新建巡检模板落地＝请求 project_id → 本人项目；新建巡检任务落地＝请求 project_id → 模板的项目 → 空间所在项目 →
     * 本人项目。须是项目且在 facility.inspection.manage 可触及范围内，否则 403 CROSS_PROJECT_FORBIDDEN；平台管理员不带任何锚点时照旧落空。
     * 定期生成的任务照旧随模板（{@link #generateScheduledTasks}）。不带平台服务的测试装配照旧取请求里的项目。
     */
    private String landingProject(String requested, String templateProject, String spaceId) {
        String landing = trimToNull(requested);
        if (platformService == null) {
            return landing;
        }
        if (landing == null) {
            landing = templateProject;
        }
        if (landing == null) {
            landing = platformService.spaceProjectId(trimToNull(spaceId));
        }
        AccessActor actor = authorizationService.currentActor();
        if (landing == null) {
            if (authorizationService.isPlatformAdmin(actor)) {
                return null;
            }
            landing = actor.organizationId();
        }
        if (!platformService.projectExists(landing) || !authorizationService.canReachProject(actor, landing, PERM)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN", AuthorizationService.PROJECT_OUT_OF_REACH);
        }
        return landing;
    }

    private FacilityInspectionTemplateRequest requireTemplate(FacilityInspectionTemplateRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private FacilityInspectionTaskRequest requireTask(FacilityInspectionTaskRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private FacilityInspectionRecordSubmitRequest requireRecord(FacilityInspectionRecordSubmitRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private FacilityInspectionExceptionReportRequest requireException(FacilityInspectionExceptionReportRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private FacilityInspectionRecheckConfirmRequest requireAction(FacilityInspectionRecheckConfirmRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return request;
    }

    private void requireStatus(FacilityInspectionTaskRecord cur, String expected, String message) {
        if (!expected.equals(cur.status())) {
            throw ApiException.badRequest("INVALID_STATE", message);
        }
    }

    private Integer validateCycle(Integer cycleDays) {
        if (cycleDays != null && cycleDays <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "周期天数必须大于 0");
        }
        return cycleDays;
    }

    private void validateGps(BigDecimal lat, BigDecimal lng) {
        if (lat != null && (lat.compareTo(BigDecimal.valueOf(-90)) < 0 || lat.compareTo(BigDecimal.valueOf(90)) > 0)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "gps_lat is invalid");
        }
        if (lng != null && (lng.compareTo(BigDecimal.valueOf(-180)) < 0 || lng.compareTo(BigDecimal.valueOf(180)) > 0)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "gps_lng is invalid");
        }
    }

    private String normalizeAllowed(String value, String field, Set<String> allowed) {
        String normalized = requireText(value, field).toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is invalid");
        }
        return normalized;
    }

    private String defaultValue(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }

    private List<Map<String, Object>> listOrEmpty(List<Map<String, Object>> value) {
        return value == null ? List.of() : value;
    }

    private List<String> stringsOrEmpty(List<String> value) {
        return value == null ? List.of() : value;
    }

    private Map<String, Object> metadata(String status, String note) {
        String trimmed = trimToNull(note);
        return trimmed == null ? Map.of("status", status) : Map.of("status", status, "note", trimmed);
    }

    private void audit(String event, String businessType, String id, String idempotencyKey, Map<String, Object> metadata) {
        audit(event, PERM, businessType, id, idempotencyKey, metadata);
    }

    private void audit(
            String event, String permission, String businessType, String id, String idempotencyKey,
            Map<String, Object> metadata) {
        auditEventService.recordSuccess(event, permission, businessType, id, null, idempotencyKey, metadata);
    }
}
