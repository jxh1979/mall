package com.complex.platform.energy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.energy.dto.EnergyMeterReadingRequest;
import com.complex.platform.energy.dto.EnergyMeterRequest;
import com.complex.platform.evidence.AuditEventService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnergyServiceTests {

    private final InMemoryEnergyRepository repo = new InMemoryEnergyRepository();
    private final AuthorizationService authz = mock(AuthorizationService.class);
    private EnergyService service;

    @BeforeEach
    void setUp() {
        service = new EnergyService(repo, mock(AuditEventService.class), authz);
    }

    @Test
    void createMeterGatedAndValidatesType() {
        assertThatThrownBy(() -> service.createMeter(new EnergyMeterRequest("电表1", "electricity", null, null, null, "k1")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("FACILITY_ENERGY_DISABLED"));
        service.facilityEnergyEnabled = true;
        assertThatThrownBy(() -> service.createMeter(new EnergyMeterRequest("x", "bogus", null, null, null, "k2")))
                .isInstanceOf(ApiException.class);
        EnergyMeterRecord m = service.createMeter(new EnergyMeterRequest("电表1", "electricity", "sp1", null, null, "k3"));
        assertThat(m.meterType()).isEqualTo("electricity");
        assertThat(m.unit()).isEqualTo("kWh"); // 默认单位
        assertThat(m.status()).isEqualTo("active");
    }

    @Test
    void recordReadingComputesConsumptionAndGuards() {
        service.facilityEnergyEnabled = true;
        EnergyMeterRecord m = service.createMeter(new EnergyMeterRequest("电表1", "electricity", null, null, null, "k1"));

        // 首读：无上期 → consumption 空。
        EnergyMeterReadingRecord r1 = service.recordReading(m.id(),
                new EnergyMeterReadingRequest("2026-05", new BigDecimal("100.0"), "u1", "k2"));
        assertThat(r1.consumption()).isNull();

        // 次读：用量=本期-上期。
        EnergyMeterReadingRecord r2 = service.recordReading(m.id(),
                new EnergyMeterReadingRequest("2026-06", new BigDecimal("150.5"), "u1", "k3"));
        assertThat(r2.consumption()).isEqualByComparingTo("50.5");

        // 同期次重复 → 拒。
        assertThatThrownBy(() -> service.recordReading(m.id(),
                new EnergyMeterReadingRequest("2026-06", new BigDecimal("160.0"), "u1", "k4")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ENERGY_READING_DUPLICATE_PERIOD"));

        // 表底回退 → 拒。
        assertThatThrownBy(() -> service.recordReading(m.id(),
                new EnergyMeterReadingRequest("2026-07", new BigDecimal("120.0"), "u1", "k5")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ENERGY_READING_NOT_MONOTONIC"));

        // 负读数 → 拒。
        assertThatThrownBy(() -> service.recordReading(m.id(),
                new EnergyMeterReadingRequest("2026-08", new BigDecimal("-5"), "u1", "k6")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("VALIDATION_FAILED"));

        // 未知表 → 拒。
        assertThatThrownBy(() -> service.recordReading("emtr_nope",
                new EnergyMeterReadingRequest("2026-06", new BigDecimal("10"), "u1", "k7")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("ENERGY_METER_NOT_FOUND"));

        assertThat(service.listReadings().items()).hasSize(2);
    }

    private static final class InMemoryEnergyRepository implements EnergyRepository {
        private final Map<String, EnergyMeterRecord> meters = new LinkedHashMap<>();
        private final List<EnergyMeterReadingRecord> readings = new ArrayList<>();

        @Override
        public EnergyMeterRecord saveMeter(EnergyMeterRecord m) {
            meters.put(m.id(), m);
            return m;
        }

        @Override
        public List<EnergyMeterRecord> listMeters() {
            return List.copyOf(meters.values());
        }

        @Override
        public Optional<EnergyMeterRecord> findMeterById(String id) {
            return Optional.ofNullable(meters.get(id));
        }

        @Override
        public EnergyMeterReadingRecord saveReading(EnergyMeterReadingRecord r) {
            readings.add(r);
            return r;
        }

        @Override
        public List<EnergyMeterReadingRecord> listReadings() {
            return List.copyOf(readings);
        }

        @Override
        public Optional<EnergyMeterReadingRecord> findReading(String meterId, String period) {
            return readings.stream().filter(r -> r.meterId().equals(meterId) && r.period().equals(period)).findFirst();
        }

        @Override
        public Optional<BigDecimal> findLatestReadingValueBefore(String meterId, String period) {
            return readings.stream()
                    .filter(r -> r.meterId().equals(meterId) && r.period().compareTo(period) < 0)
                    .max(Comparator.comparing(EnergyMeterReadingRecord::period))
                    .map(EnergyMeterReadingRecord::readingValue);
        }
    }
}
