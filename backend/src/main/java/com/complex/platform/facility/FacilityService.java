package com.complex.platform.facility;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.FacilityEquipmentRequest;
import com.complex.platform.facility.dto.FacilityStatusRequest;
import java.time.Instant;
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

    @Value("${facility.equipment.enabled:false}")
    boolean facilityEquipmentEnabled;

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
