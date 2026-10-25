package com.complex.platform.facility;

// 文件版本：V0.02

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
import static com.complex.platform.common.Strings.requireText;

/**
 * 楼宇机电设备台账（③楼宇运营 S1，据 M1 第3节 + 速查表第十节重建）。
 * 状态6态、4分类、每台维保周期+到期提醒、告警源(人工/IoT)。
 */
@Service
public class FacilityEquipmentService {

    static final Set<String> TYPES = Set.of("elevator", "hvac", "power", "water_fire");
    static final Set<String> STATUSES = Set.of("normal", "alarm", "fault", "maintenance", "disabled", "retired");
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
    public FacilityEquipmentRecord archive(String id, boolean archive, String reason) {
        authorizationService.requirePlatformScope();
        FacilityEquipmentRecord cur = load(id);
        String r = archive ? requireText(reason, "reason") : null;
        Instant now = Instant.now();
        FacilityEquipmentRecord updated = archive
                ? cur.withArchive(now, archiveActorId(), now)
                : cur.withArchive(null, null, now).withDelete("none", null, null, null, now);
        repository.updateEquipment(updated);
        repository.setEquipmentArchiveReason(id, r);
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
        String reason = requireText(request == null ? null : request.reason(), "reason");
        FacilityEquipmentRecord updated = cur.withDelete("pending", archiveActorId(), now, reason, now);
        repository.updateEquipment(updated);
        auditEventService.recordSuccess("facility.equipment_delete_requested", PERM, "facility_equipment", id,
                null, null, Map.of());
        if (authorizationService.isMasterAuthority(authorizationService.currentActor())) {
            // BSM 第 577 行：持授权 U 盘密钥的最高权限本人发起的删除不需要经过任何审批（占用检查照走）。
            approveDelete(id, new com.complex.platform.facility.dto.FacilityEquipmentDeleteApprovalRequest("approved", null));
            return updated.withDelete("approved", updated.deleteRequestedBy(), now, reason, now);
        }
        return updated;
    }

    @Transactional
    public com.complex.platform.facility.dto.FacilityEquipmentDeleteDecision approveDelete(String id,
            com.complex.platform.facility.dto.FacilityEquipmentDeleteApprovalRequest request) {
        // BSM 第 577 行：设施设备删除一律须由具备最高权限的人审批通过后执行，不设例外。
        if (!authorizationService.isMasterAuthority(authorizationService.currentActor())) {
            throw ApiException.forbidden("MASTER_APPROVAL_REQUIRED", "设施设备删除须由最高权限本人批准");
        }
        authorizationService.requirePermission("facility.equipment.approve");
        FacilityEquipmentRecord cur = load(id);
        if (!"pending".equals(cur.deleteStatus())) {
            throw ApiException.badRequest("FACILITY_EQUIPMENT_DELETE_NOT_PENDING", "该设备没有待审批的删除申请");
        }
        String decision = request == null || request.decision() == null
                ? null : request.decision().trim().toLowerCase(java.util.Locale.ROOT);
        if (!"approved".equals(decision) && !"rejected".equals(decision)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "审批结论必须为通过（approved）或驳回（rejected）");
        }
        // 审批人只能是最高权限本人；最高权限发起的删除直接生效、不经审批，普通申请人无从审批自己的申请（BSM 第 577 行）。
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

    /**
     * 状态变更/告警上报：置 alarm 须带 source，此入口为员工人工上报只收 manual（OBS-13-06，PGM L823「告警来源支持人工上报与远程/物联自动采集两种」，
     * 物联告警须经真实设备接入）；置 alarm/fault 须带优先级；离开 alarm 清告警字段。
     */
    @Transactional
    public FacilityEquipmentRecord changeStatus(String id, FacilityEquipmentStatusRequest request) {
        authorizationService.requirePlatformScope();
        if (request == null || request.status() == null || !STATUSES.contains(request.status())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "status is invalid");
        }
        FacilityEquipmentRecord cur = load(id);
        String target = request.status();
        // HP-F03（BSM L1408「楼宇设备报废由设备所有权主体确认，沿现有审批流程办理」）：报废只经报废申请审批后置；已报废不得改回。
        if ("retired".equals(target)) {
            throw ApiException.conflict("FACILITY_EQUIPMENT_SCRAP_APPROVAL_REQUIRED", "报废须提交报废申请并经审批");
        }
        requireNotRetired(cur);
        String alarmSource = null;
        String alarmNote = null;
        Instant alarmRaisedAt = null;
        if ("alarm".equals(target)) {
            if (!"manual".equals(request.alarmSource())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "告警来源只能是手工上报：此入口为员工人工上报，物联告警须经真实设备接入，不能自行声明");
            }
            alarmSource = request.alarmSource();
            alarmNote = request.alarmNote();
            alarmRaisedAt = Instant.now();
        }
        if (WORK_ORDER_TRIGGER_STATUSES.contains(target)
                && (request.priority() == null || !FacilityWorkOrderService.PRIORITIES.contains(request.priority()))) {
            // HP-F01（2026-10-21 操作员裁决补充第 1 条）：优先级由上报人（或物联网上报）当场选，决定接警响应时限；此入口为员工人工上报。
            throw ApiException.badRequest("VALIDATION_FAILED", "告警或故障上报须选择优先级：紧急（urgent）、高（high）或普通（normal）");
        }
        FacilityEquipmentRecord rec = withStatus(cur, target, alarmSource, alarmNote, alarmRaisedAt,
                cur.lastMaintenanceAt(), cur.nextMaintenanceDue());
        repository.updateEquipment(rec);
        auditEventService.recordSuccess("facility.equipment_status_changed", PERM, "facility_equipment", id,
                null, request.idempotencyKey(), Map.of("status", target));
        if (WORK_ORDER_TRIGGER_STATUSES.contains(target)) {
            eventPublisher.publishEvent(new FacilityEquipmentAlarmedEvent(id, rec.name(),
                    request.alarmSource(), request.alarmNote(), rec.projectId(), rec.spaceId(), rec.equipmentType(),
                    rec.responsibleUserId(), request.priority()));
        }
        return rec;
    }

    /** 维保完成回写：last=now、状态回 normal、按周期重算下次到期。 */
    @Transactional
    public FacilityEquipmentRecord recordMaintenanceDone(String id, String idempotencyKey) {
        authorizationService.requirePlatformScope();
        FacilityEquipmentRecord cur = load(id);
        requireNotRetired(cur);
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
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "设施设备不存在或已被删除"));
    }

    private void requireNotRetired(FacilityEquipmentRecord cur) {
        if ("retired".equals(cur.status())) {
            throw ApiException.conflict("FACILITY_EQUIPMENT_RETIRED", "设备已报废，不能改回在用");
        }
    }

    private void validateType(String type) {
        if (type == null || !TYPES.contains(type)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "equipment_type is invalid");
        }
    }

    private FacilityEquipmentRequest require(FacilityEquipmentRequest r) {
        if (r == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
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
