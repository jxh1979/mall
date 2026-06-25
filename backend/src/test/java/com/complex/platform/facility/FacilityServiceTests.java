package com.complex.platform.facility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.FacilityEquipmentRequest;
import com.complex.platform.facility.dto.FacilityStatusRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderAssignRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderRequest;
import com.complex.platform.facility.dto.FacilityWorkOrderStatusRequest;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private static final class InMemoryFacilityRepository implements FacilityRepository {
        private final Map<String, FacilityEquipmentRecord> m = new LinkedHashMap<>();
        private final Map<String, FacilityWorkOrderRecord> wo = new LinkedHashMap<>();

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
    }
}
