package com.complex.platform.facility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.FacilityEquipmentRequest;
import com.complex.platform.facility.dto.FacilityInspectionCompleteRequest;
import com.complex.platform.facility.dto.FacilityInspectionTemplateRequest;
import com.complex.platform.facility.dto.FacilityStatusRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderAssignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderStatusRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FacilityServiceTests {

    private final InMemoryFacilityRepository repo = new InMemoryFacilityRepository();
    private final AuthorizationService authz = mock(AuthorizationService.class);
    private FacilityService service;

    @BeforeEach
    void setUp() {
        service = new FacilityService(repo, mock(AuditEventService.class), authz);
    }

    @Test
    void createEquipmentGatedByFlagAndValidatesType() {
        // 关标志 → 拒。
        assertThatThrownBy(() -> service.createEquipment(new FacilityEquipmentRequest(
                "中央空调1", "hvac", null, null, null, null, null, 30, "k1")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FACILITY_EQUIPMENT_DISABLED"));

        service.facilityEquipmentEnabled = true;
        // 非法类型 → 拒。
        assertThatThrownBy(() -> service.createEquipment(new FacilityEquipmentRequest(
                "x", "bogus", null, null, null, null, null, null, "k2"))).isInstanceOf(ApiException.class);

        FacilityEquipmentRecord eq = service.createEquipment(new FacilityEquipmentRequest(
                "中央空调1", "hvac", "sp_1", "格力", "GX-1", null, null, 30, "k3"));
        assertThat(eq.status()).isEqualTo("active");
        assertThat(eq.equipmentType()).isEqualTo("hvac");
        assertThat(eq.maintenanceCycleDays()).isEqualTo(30);
        assertThat(service.listEquipment().items()).extracting(FacilityEquipmentRecord::id).contains(eq.id());
    }

    @Test
    void updateEquipmentStatusTransitions() {
        service.facilityEquipmentEnabled = true;
        FacilityEquipmentRecord eq = service.createEquipment(new FacilityEquipmentRequest(
                "电梯1", "elevator", null, null, null, null, null, null, "k1"));
        FacilityEquipmentRecord m = service.updateEquipmentStatus(eq.id(), new FacilityStatusRequest("maintenance", "k2"));
        assertThat(m.status()).isEqualTo("maintenance");
        assertThatThrownBy(() -> service.updateEquipmentStatus(eq.id(), new FacilityStatusRequest("bogus", "k3")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void createWorkOrderGatedComputesSlaAndFlows() {
        // 关标志 → 拒。
        assertThatThrownBy(() -> service.createWorkOrder(new FacilityWorkOrderRequest(
                null, null, "电梯异响", null, "repair", "high", "u1", "k1")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FACILITY_WORK_ORDER_DISABLED"));

        service.facilityWorkOrderEnabled = true;
        // 非法 category → 拒。
        assertThatThrownBy(() -> service.createWorkOrder(new FacilityWorkOrderRequest(
                null, null, "x", null, "bogus", null, null, "k2"))).isInstanceOf(ApiException.class);

        FacilityWorkOrderRecord wo = service.createWorkOrder(new FacilityWorkOrderRequest(
                null, "sp1", "电梯异响", "三号梯异响", "repair", "high", "u1", "k3"));
        assertThat(wo.status()).isEqualTo("open");
        assertThat(wo.priority()).isEqualTo("high");
        // high SLA = 24h（从建单时刻推算）。
        assertThat(wo.slaDueAt()).isEqualTo(wo.createdAt().plus(24, ChronoUnit.HOURS));

        // 默认 priority = medium（72h）。
        FacilityWorkOrderRecord def = service.createWorkOrder(new FacilityWorkOrderRequest(
                null, null, "默认报修", null, "maintenance", null, null, "k4"));
        assertThat(def.priority()).isEqualTo("medium");
        assertThat(def.slaDueAt()).isEqualTo(def.createdAt().plus(72, ChronoUnit.HOURS));

        // 派单 open→assigned。
        FacilityWorkOrderRecord assigned = service.assignWorkOrder(wo.id(), new FacilityWorkOrderAssignRequest("worker1", "k5"));
        assertThat(assigned.status()).isEqualTo("assigned");
        assertThat(assigned.assignee()).isEqualTo("worker1");

        // 流转 assigned→in_progress→resolved（置 resolvedAt）。
        service.updateWorkOrderStatus(wo.id(), new FacilityWorkOrderStatusRequest("in_progress", "k6"));
        FacilityWorkOrderRecord resolved = service.updateWorkOrderStatus(wo.id(), new FacilityWorkOrderStatusRequest("resolved", "k7"));
        assertThat(resolved.status()).isEqualTo("resolved");
        assertThat(resolved.resolvedAt()).isNotNull();

        // 非法迁移 resolved→assigned（不在 transitions 集合）→ 拒。
        assertThatThrownBy(() -> service.updateWorkOrderStatus(wo.id(), new FacilityWorkOrderStatusRequest("assigned", "k8")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FACILITY_WORK_ORDER_INVALID_TRANSITION"));
    }

    @Test
    void createWorkOrderRejectsUnknownEquipment() {
        service.facilityWorkOrderEnabled = true;
        assertThatThrownBy(() -> service.createWorkOrder(new FacilityWorkOrderRequest(
                "feq_nope", null, "挂不存在设备", null, "repair", null, null, "k1")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void createInspectionTemplateGatedByFlag() {
        assertThatThrownBy(() -> service.createInspectionTemplate(new FacilityInspectionTemplateRequest("月检", "hvac", List.of("查滤网"), "k1")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FACILITY_INSPECTION_DISABLED"));
        service.facilityInspectionEnabled = true;
        FacilityInspectionTemplateRecord t = service.createInspectionTemplate(
                new FacilityInspectionTemplateRequest("月检", "hvac", List.of("查滤网", "测温度"), "k2"));
        assertThat(t.items()).containsExactly("查滤网", "测温度");
        assertThat(t.equipmentType()).isEqualTo("hvac");
        assertThat(service.listInspectionTemplates().items()).extracting(FacilityInspectionTemplateRecord::id).contains(t.id());
    }

    @Test
    void generateInspectionTasksDedupsAndMarksOverdue() {
        Instant now = Instant.parse("2026-06-24T00:00:00Z");
        // 直接经 repo 播种：从未维保(due=now,pending) / 已超期(due<now→overdue) / 退役(跳过)。
        repo.saveEquipment(new FacilityEquipmentRecord("feq_a", "A", "hvac", null, null, null, null, null, "active", 30, null, null, null, now, now));
        repo.saveEquipment(new FacilityEquipmentRecord("feq_b", "B", "elevator", null, null, null, null, null, "active", 30, now.minus(40, ChronoUnit.DAYS), null, null, now, now));
        repo.saveEquipment(new FacilityEquipmentRecord("feq_c", "C", "fire", null, null, null, null, null, "retired", 30, null, null, null, now, now));

        assertThat(service.generateDueInspectionTasks(now)).isEqualTo(2); // a + b，c 退役跳过
        assertThat(service.generateDueInspectionTasks(now)).isEqualTo(0); // 同到期点防重

        List<FacilityInspectionTaskRecord> tasks = service.listInspectionTasks().items();
        assertThat(tasks).hasSize(2);
        assertThat(tasks).filteredOn(t -> t.equipmentId().equals("feq_b"))
                .extracting(FacilityInspectionTaskRecord::status).containsExactly("overdue");
        assertThat(tasks).filteredOn(t -> t.equipmentId().equals("feq_a"))
                .extracting(FacilityInspectionTaskRecord::status).containsExactly("pending"); // due=now，不 <now
    }

    @Test
    void completeInspectionTaskWritesRecordAndAdvancesEquipment() {
        Instant now = Instant.parse("2026-06-24T00:00:00Z");
        repo.saveEquipment(new FacilityEquipmentRecord("feq_x", "X", "hvac", null, null, null, null, null, "active", 30, null, null, null, now, now));
        service.generateDueInspectionTasks(now);
        FacilityInspectionTaskRecord task = service.listInspectionTasks().items().get(0);

        // 关标志 → 完成被拒。
        assertThatThrownBy(() -> service.completeInspectionTask(task.id(),
                new FacilityInspectionCompleteRequest("insp", "pass", null, List.of("ok"), "k1")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FACILITY_INSPECTION_DISABLED"));

        service.facilityInspectionEnabled = true;
        FacilityInspectionRecordRecord rec = service.completeInspectionTask(task.id(),
                new FacilityInspectionCompleteRequest("insp", "pass", "正常", List.of("外观", "噪音"), "k2"));
        assertThat(rec.result()).isEqualTo("pass");
        assertThat(rec.checkedItems()).containsExactly("外观", "噪音");
        assertThat(service.listInspectionTasks().items().get(0).status()).isEqualTo("completed");
        assertThat(repo.findEquipmentById("feq_x").orElseThrow().lastMaintenanceAt()).isNotNull(); // 维保时点推进

        // 重复完成 → 拒。
        assertThatThrownBy(() -> service.completeInspectionTask(task.id(),
                new FacilityInspectionCompleteRequest("insp", "pass", null, null, "k3")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FACILITY_INSPECTION_TASK_ALREADY_COMPLETED"));
    }

    private static final class InMemoryFacilityRepository implements FacilityRepository {
        private final Map<String, FacilityEquipmentRecord> m = new LinkedHashMap<>();
        private final Map<String, FacilityWorkOrderRecord> wo = new LinkedHashMap<>();
        private final Map<String, FacilityInspectionTemplateRecord> tpl = new LinkedHashMap<>();
        private final Map<String, FacilityInspectionTaskRecord> task = new LinkedHashMap<>();
        private final Map<String, FacilityInspectionRecordRecord> rec = new LinkedHashMap<>();
        private final Set<String> taskKeys = new HashSet<>();

        @Override
        public FacilityEquipmentRecord saveEquipment(FacilityEquipmentRecord e) {
            m.put(e.id(), e);
            return e;
        }

        @Override
        public List<FacilityEquipmentRecord> listEquipment() {
            return List.copyOf(m.values());
        }

        @Override
        public Optional<FacilityEquipmentRecord> findEquipmentById(String id) {
            return Optional.ofNullable(m.get(id));
        }

        @Override
        public FacilityWorkOrderRecord saveWorkOrder(FacilityWorkOrderRecord w) {
            wo.put(w.id(), w);
            return w;
        }

        @Override
        public List<FacilityWorkOrderRecord> listWorkOrders() {
            return List.copyOf(wo.values());
        }

        @Override
        public Optional<FacilityWorkOrderRecord> findWorkOrderById(String id) {
            return Optional.ofNullable(wo.get(id));
        }

        @Override
        public FacilityInspectionTemplateRecord saveInspectionTemplate(FacilityInspectionTemplateRecord t) {
            tpl.put(t.id(), t);
            return t;
        }

        @Override
        public List<FacilityInspectionTemplateRecord> listInspectionTemplates() {
            return List.copyOf(tpl.values());
        }

        @Override
        public boolean upsertInspectionTask(FacilityInspectionTaskRecord t) {
            if (!taskKeys.add(t.equipmentId() + "|" + t.dueAt())) {
                return false; // 自然键冲突，幂等不重复
            }
            task.put(t.id(), t);
            return true;
        }

        @Override
        public FacilityInspectionTaskRecord saveInspectionTask(FacilityInspectionTaskRecord t) {
            task.put(t.id(), t);
            return t;
        }

        @Override
        public List<FacilityInspectionTaskRecord> listInspectionTasks() {
            return List.copyOf(task.values());
        }

        @Override
        public Optional<FacilityInspectionTaskRecord> findInspectionTaskById(String id) {
            return Optional.ofNullable(task.get(id));
        }

        @Override
        public int markOverdueInspectionTasks(Instant now) {
            int flipped = 0;
            for (FacilityInspectionTaskRecord t : List.copyOf(task.values())) {
                if ("pending".equals(t.status()) && t.dueAt() != null && t.dueAt().isBefore(now)) {
                    task.put(t.id(), new FacilityInspectionTaskRecord(t.id(), t.equipmentId(), t.spaceId(), t.dueAt(),
                            "overdue", t.generatedAt(), t.completedRecordId(), t.completedAt(), t.projectId(),
                            t.merchantId(), t.createdAt(), now));
                    flipped++;
                }
            }
            return flipped;
        }

        @Override
        public FacilityInspectionRecordRecord saveInspectionRecord(FacilityInspectionRecordRecord r) {
            rec.put(r.id(), r);
            return r;
        }
    }
}
