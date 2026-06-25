package com.complex.platform.facility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.FacilityEquipmentRequest;
import com.complex.platform.facility.dto.FacilityStatusRequest;
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

    private static final class InMemoryFacilityRepository implements FacilityRepository {
        private final Map<String, FacilityEquipmentRecord> m = new LinkedHashMap<>();

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
    }
}
