package com.complex.platform.facility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.facility.dto.FacilityEquipmentRequest;
import com.complex.platform.facility.dto.FacilityEquipmentStatusRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 楼宇设备台账 unit 测试（S1）：验 4分类/6态枚举校验、告警源必填、离开告警清字段、
 * 维保完成回写+按周期重算到期、到期清单过滤。无 DB。
 */
class FacilityEquipmentServiceTests {

    private final InMemoryFacilityRepository repo = new InMemoryFacilityRepository();
    private final FacilityEquipmentService service =
            new FacilityEquipmentService(repo, mock(AuditEventService.class), mock(AuthorizationService.class));

    private FacilityEquipmentRequest req(String type, Integer cycle, LocalDate install) {
        return new FacilityEquipmentRequest(null, "电梯1号", type, "sp_1", "三菱", "M-100", "三菱电梯",
                "usr_eng", cycle, install, null, "proj_1", null, "k_" + type + cycle);
    }

    @Test
    void create_rejectsInvalidType() {
        assertThatThrownBy(() -> service.create(req("weak_current", 90, LocalDate.parse("2026-01-01"))))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void create_setsNormalStatusAndComputesNextDueFromInstallPlusCycle() {
        FacilityEquipmentRecord rec = service.create(req("elevator", 90, LocalDate.parse("2026-01-01")));
        assertThat(rec.id()).startsWith("feq_");
        assertThat(rec.status()).isEqualTo("normal");
        assertThat(rec.nextMaintenanceDue()).isEqualTo(LocalDate.parse("2026-04-01"));
        assertThat(repo.store).containsKey(rec.id());
    }

    @Test
    void changeStatus_alarmRequiresSourceThenSetsAlarmFields() {
        FacilityEquipmentRecord rec = service.create(req("hvac", null, null));
        assertThatThrownBy(() -> service.changeStatus(rec.id(),
                new FacilityEquipmentStatusRequest("alarm", null, "温度异常", null)))
                .isInstanceOf(ApiException.class);
        FacilityEquipmentRecord alarmed = service.changeStatus(rec.id(),
                new FacilityEquipmentStatusRequest("alarm", "iot", "温度异常", null));
        assertThat(alarmed.status()).isEqualTo("alarm");
        assertThat(alarmed.alarmSource()).isEqualTo("iot");
        assertThat(alarmed.alarmRaisedAt()).isNotNull();
    }

    @Test
    void changeStatus_leavingAlarmClearsAlarmFields() {
        FacilityEquipmentRecord rec = service.create(req("power", null, null));
        service.changeStatus(rec.id(), new FacilityEquipmentStatusRequest("alarm", "manual", "跳闸", null));
        FacilityEquipmentRecord cleared = service.changeStatus(rec.id(),
                new FacilityEquipmentStatusRequest("normal", null, null, null));
        assertThat(cleared.status()).isEqualTo("normal");
        assertThat(cleared.alarmSource()).isNull();
        assertThat(cleared.alarmRaisedAt()).isNull();
    }

    @Test
    void changeStatus_rejectsUnknownStatus() {
        FacilityEquipmentRecord rec = service.create(req("water_fire", null, null));
        assertThatThrownBy(() -> service.changeStatus(rec.id(),
                new FacilityEquipmentStatusRequest("broken", null, null, null)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void maintenanceDone_resetsToNormalAndRecomputesDueFromNow() {
        FacilityEquipmentRecord rec = service.create(req("elevator", 30, LocalDate.parse("2025-01-01")));
        service.changeStatus(rec.id(), new FacilityEquipmentStatusRequest("fault", null, null, null));
        FacilityEquipmentRecord done = service.recordMaintenanceDone(rec.id(), "k_done");
        assertThat(done.status()).isEqualTo("normal");
        assertThat(done.lastMaintenanceAt()).isNotNull();
        assertThat(done.nextMaintenanceDue()).isEqualTo(LocalDate.now().plusDays(30));
    }

    @Test
    void maintenanceDue_filtersOverdueAndExcludesRetired() {
        FacilityEquipmentRecord due = service.create(req("elevator", 10, LocalDate.now().minusDays(40)));
        FacilityEquipmentRecord notDue = service.create(req("hvac", 10, LocalDate.now().plusDays(40)));
        service.changeStatus(due.id(), new FacilityEquipmentStatusRequest("retired", null, null, null));
        // due 已报废 -> 不应出现；notDue 未到期 -> 不应出现
        assertThat(service.listMaintenanceDue().items()).extracting(FacilityEquipmentRecord::id)
                .doesNotContain(due.id(), notDue.id());
    }

    /** 内存仓。 */
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
        public void insertEquipment(FacilityEquipmentRecord r) {
            store.put(r.id(), r);
        }

        @Override
        public void updateEquipment(FacilityEquipmentRecord r) {
            store.put(r.id(), r);
        }

        @Override
        public List<FacilityEquipmentRecord> listMaintenanceDue(LocalDate asOf) {
            List<FacilityEquipmentRecord> out = new ArrayList<>();
            for (FacilityEquipmentRecord r : store.values()) {
                if (r.nextMaintenanceDue() != null && !r.nextMaintenanceDue().isAfter(asOf)
                        && !"retired".equals(r.status()) && !"disabled".equals(r.status())) {
                    out.add(r);
                }
            }
            return out;
        }
    }
}
