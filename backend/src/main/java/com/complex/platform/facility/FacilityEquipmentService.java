package com.complex.platform.facility;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.FacilityEquipmentRequest;
import com.complex.platform.facility.dto.FacilityEquipmentStatusRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 楼宇机电设备台账（③楼宇运营 S1，据 M1 第3节 + 速查表第十节重建）。
 * 状态6态、4分类、每台维保周期+到期提醒、告警源(人工/IoT)。
 */
@Service
public class FacilityEquipmentService {

    static final Set<String> TYPES = Set.of("elevator", "hvac", "power", "water_fire");
    static final Set<String> STATUSES = Set.of("normal", "alarm", "fault", "maintenance", "disabled", "retired");
    static final Set<String> ALARM_SOURCES = Set.of("manual", "iot");
    private static final Set<String> WORK_ORDER_TRIGGER_STATUSES = Set.of("alarm", "fault");
    private static final String PERM = "facility.equipment.manage";

    private final FacilityRepository repository;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final ApplicationEventPublisher eventPublisher;

    public FacilityEquipmentService(FacilityRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.eventPublisher = eventPublisher;
    }

    public PageResponse<FacilityEquipmentRecord> listEquipment() {
        return listEquipment(false);
    }

    /** V264：includeArchived=false（默认）排除已存档设备（外包商始终只看未存档本项目件）；true 供后台管理存档件。 */
    public PageResponse<FacilityEquipmentRecord> listEquipment(boolean includeArchived) {
        AccessActor actor = authorizationService.currentActor();
        List<FacilityEquipmentRecord> items;
        if (authorizationService.isOutsourcedVendor(actor)) {
            String project = authorizationService.requireProjectScope(actor);
            items = repository.listEquipment(includeArchived).stream()
                    .filter(e -> java.util.Objects.equals(project, e.projectId()))
                    .toList();
        } else {
            items = repository.listEquipment(includeArchived);
        }
        return new PageResponse<>(items, 1, 500, items.size());
    }

    // ================= V264 设施设备：存档与审批后删除 =================

    /** 当前操作者 id（存档/删除留痕 + 自审校验）；无鉴权上下文（测试/内部）回退 null。 */
    private String archiveActorId() {
        try {
            return authorizationService.currentActor().id();
        } catch (RuntimeException unauthenticated) {
            return null;
        }
    }

    @Transactional
    public FacilityEquipmentRecord archive(String id, boolean archive) {
        authorizationService.requirePlatformScope();
        FacilityEquipmentRecord cur = load(id);
        Instant now = Instant.now();
        FacilityEquipmentRecord updated = archive
                ? cur.withArchive(now, archiveActorId(), now)
                : cur.withArchive(null, null, now).withDelete("none", null, null, null, now);
        repository.updateEquipment(updated);
        auditEventService.recordSuccess(archive ? "facility.equipment_archived" : "facility.equipment_unarchived",
                PERM, "facility_equipment", id, null, null, Map.of("archived", archive));
        return updated;
    }

    @Transactional
    public FacilityEquipmentRecord requestDelete(String id,
            com.complex.platform.facility.dto.FacilityEquipmentDeleteRequest request) {
        authorizationService.requirePlatformScope();
        FacilityEquipmentRecord cur = load(id);
        if (cur.archivedAt() == null) {
            throw ApiException.badRequest("FACILITY_EQUIPMENT_NOT_ARCHIVED", "设备必须先存档才能申请删除");
        }
        if ("pending".equals(cur.deleteStatus())) {
            throw ApiException.conflict("FACILITY_EQUIPMENT_DELETE_ALREADY_PENDING", "该设备已有待审批的删除申请");
        }
        Instant now = Instant.now();
        String reason = request == null || request.reason() == null || request.reason().isBlank()
                ? null : request.reason().trim();
        FacilityEquipmentRecord updated = cur.withDelete("pending", archiveActorId(), now, reason, now);
        repository.updateEquipment(updated);
        auditEventService.recordSuccess("facility.equipment_delete_requested", PERM, "facility_equipment", id,
                null, null, Map.of());
        return updated;
    }

    @Transactional
    public com.complex.platform.facility.dto.FacilityEquipmentDeleteDecision approveDelete(String id,
            com.complex.platform.facility.dto.FacilityEquipmentDeleteApprovalRequest request) {
        authorizationService.requirePermission("facility.equipment.approve");
        FacilityEquipmentRecord cur = load(id);
        if (!"pending".equals(cur.deleteStatus())) {
            throw ApiException.badRequest("FACILITY_EQUIPMENT_DELETE_NOT_PENDING", "该设备没有待审批的删除申请");
        }
        String decision = request == null || request.decision() == null
                ? null : request.decision().trim().toLowerCase(java.util.Locale.ROOT);
        if (!"approved".equals(decision) && !"rejected".equals(decision)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "decision must be approved or rejected");
        }
        String actor = archiveActorId();
        if (actor != null && actor.equals(cur.deleteRequestedBy())) {
            throw ApiException.forbidden("FACILITY_EQUIPMENT_SELF_APPROVE_FORBIDDEN", "删除申请发起人不能审批自己的申请");
        }
        if ("rejected".equals(decision)) {
            repository.updateEquipment(cur.withDelete("none", null, null, null, Instant.now()));
            auditEventService.recordSuccess("facility.equipment_delete_rejected", "facility.equipment.approve",
                    "facility_equipment", id, null, null, Map.of());
            return new com.complex.platform.facility.dto.FacilityEquipmentDeleteDecision(id, "rejected", false);
        }
        long refs = repository.countEquipmentReferences(id);
        if (refs > 0) {
            throw ApiException.conflict("FACILITY_EQUIPMENT_IN_USE",
                    "设备仍被 " + refs + " 条维保工单引用，不可删除");
        }
        repository.deleteEquipment(id);
        auditEventService.recordSuccess("facility.equipment_delete_approved", "facility.equipment.approve",
                "facility_equipment", id, null, null, Map.of());
        return new com.complex.platform.facility.dto.FacilityEquipmentDeleteDecision(id, "approved", true);
    }

    public PageResponse<FacilityEquipmentRecord> listMaintenanceDue() {
        AccessActor actor = authorizationService.currentActor();
        LocalDate today = LocalDate.now();
        List<FacilityEquipmentRecord> items = authorizationService.isOutsourcedVendor(actor)
                ? repository.listMaintenanceDueByProject(today, authorizationService.requireProjectScope(actor))
                : repository.listMaintenanceDue(today);
        return new PageResponse<>(items, 1, 500, items.size());
    }

    @Transactional
    public FacilityEquipmentRecord create(FacilityEquipmentRequest request) {
        authorizationService.requirePlatformScope();
        FacilityEquipmentRequest r = require(request);
        validateType(r.equipmentType());
        if (r.name() == null || r.name().isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "name is required");
        }
        Instant now = Instant.now();
        String id = r.id() == null || r.id().isBlank() ? "feq_" + UUID.randomUUID() : r.id();
        LocalDate nextDue = computeNextDue(r.installDate(), null, r.maintenanceCycleDays());
        FacilityEquipmentRecord rec = new FacilityEquipmentRecord(id, r.name().trim(), r.equipmentType(),
                r.spaceId(), r.brand(), r.model(), r.vendor(), r.responsibleUserId(), "normal",
                r.maintenanceCycleDays(), null, nextDue, r.installDate(), r.warrantyUntil(),
                null, null, null, r.projectId(), r.merchantId(), now, now);
        repository.insertEquipment(rec);
        auditEventService.recordSuccess("facility.equipment_created", PERM, "facility_equipment", id,
                null, r.idempotencyKey(), Map.of("type", r.equipmentType()));
        return rec;
    }

    @Transactional
    public FacilityEquipmentRecord update(String id, FacilityEquipmentRequest request) {
        authorizationService.requirePlatformScope();
        FacilityEquipmentRequest r = require(request);
        FacilityEquipmentRecord cur = load(id);
        validateType(r.equipmentType());
        LocalDate nextDue = computeNextDue(r.installDate(), cur.lastMaintenanceAt(), r.maintenanceCycleDays());
        FacilityEquipmentRecord rec = new FacilityEquipmentRecord(id,
                r.name() == null || r.name().isBlank() ? cur.name() : r.name().trim(), r.equipmentType(),
                r.spaceId(), r.brand(), r.model(), r.vendor(), r.responsibleUserId(), cur.status(),
                r.maintenanceCycleDays(), cur.lastMaintenanceAt(), nextDue, r.installDate(), r.warrantyUntil(),
                cur.alarmSource(), cur.alarmNote(), cur.alarmRaisedAt(), r.projectId(), r.merchantId(),
                cur.createdAt(), Instant.now());
        repository.updateEquipment(rec);
        auditEventService.recordSuccess("facility.equipment_updated", PERM, "facility_equipment", id,
                null, r.idempotencyKey(), Map.of());
        return rec;
    }

    /** 状态变更/告警上报：置 alarm 须带 source(manual|iot)；离开 alarm 清告警字段。 */
    @Transactional
    public FacilityEquipmentRecord changeStatus(String id, FacilityEquipmentStatusRequest request) {
        authorizationService.requirePlatformScope();
        if (request == null || request.status() == null || !STATUSES.contains(request.status())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "status is invalid");
        }
        FacilityEquipmentRecord cur = load(id);
        String target = request.status();
        String alarmSource = null;
        String alarmNote = null;
        Instant alarmRaisedAt = null;
        if ("alarm".equals(target)) {
            if (request.alarmSource() == null || !ALARM_SOURCES.contains(request.alarmSource())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "alarm_source must be manual or iot");
            }
            alarmSource = request.alarmSource();
            alarmNote = request.alarmNote();
            alarmRaisedAt = Instant.now();
        }
        FacilityEquipmentRecord rec = withStatus(cur, target, alarmSource, alarmNote, alarmRaisedAt,
                cur.lastMaintenanceAt(), cur.nextMaintenanceDue());
        repository.updateEquipment(rec);
        auditEventService.recordSuccess("facility.equipment_status_changed", PERM, "facility_equipment", id,
                null, request.idempotencyKey(), Map.of("status", target));
        if (WORK_ORDER_TRIGGER_STATUSES.contains(target)) {
            eventPublisher.publishEvent(new FacilityEquipmentAlarmedEvent(id, rec.name(),
                    request.alarmSource(), request.alarmNote()));
        }
        return rec;
    }

    /** 维保完成回写：last=now、状态回 normal、按周期重算下次到期。 */
    @Transactional
    public FacilityEquipmentRecord recordMaintenanceDone(String id, String idempotencyKey) {
        authorizationService.requirePlatformScope();
        FacilityEquipmentRecord cur = load(id);
        Instant now = Instant.now();
        LocalDate nextDue = computeNextDue(cur.installDate(), now, cur.maintenanceCycleDays());
        FacilityEquipmentRecord rec = withStatus(cur, "normal", null, null, null, now, nextDue);
        repository.updateEquipment(rec);
        auditEventService.recordSuccess("facility.equipment_maintained", PERM, "facility_equipment", id,
                null, idempotencyKey, Map.of());
        return rec;
    }

    private FacilityEquipmentRecord load(String id) {
        return repository.findEquipmentById(id)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "facility equipment not found"));
    }

    private void validateType(String type) {
        if (type == null || !TYPES.contains(type)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "equipment_type is invalid");
        }
    }

    private FacilityEquipmentRequest require(FacilityEquipmentRequest r) {
        if (r == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return r;
    }

    private LocalDate computeNextDue(LocalDate installDate, Instant lastMaintenance, Integer cycleDays) {
        if (cycleDays == null || cycleDays <= 0) {
            return null;
        }
        LocalDate base = lastMaintenance != null ? lastMaintenance.atZone(ZoneOffset.UTC).toLocalDate() : installDate;
        return base == null ? null : base.plusDays(cycleDays);
    }

    private FacilityEquipmentRecord withStatus(FacilityEquipmentRecord c, String status, String alarmSource,
            String alarmNote, Instant alarmRaisedAt, Instant lastMaintenanceAt, LocalDate nextDue) {
        return new FacilityEquipmentRecord(c.id(), c.name(), c.equipmentType(), c.spaceId(), c.brand(), c.model(),
                c.vendor(), c.responsibleUserId(), status, c.maintenanceCycleDays(), lastMaintenanceAt, nextDue,
                c.installDate(), c.warrantyUntil(), alarmSource, alarmNote, alarmRaisedAt, c.projectId(),
                c.merchantId(), c.createdAt(), Instant.now());
    }
}
