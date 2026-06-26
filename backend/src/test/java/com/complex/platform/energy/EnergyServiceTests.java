package com.complex.platform.energy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.energy.EnergyRepository.EnergyContractSnapshot;
import com.complex.platform.energy.dto.EnergyReadingRecordRequest;
import com.complex.platform.evidence.AuditEventService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 能耗抄表 S4 服务测试：异常识别、总分表抹差、公区能源费分摊与 AR 幂等。 */
class EnergyServiceTests {

    private final InMemoryEnergyRepository repo = new InMemoryEnergyRepository();
    private final EnergyService service =
            new EnergyService(repo, mock(AuditEventService.class), mock(AuthorizationService.class));

    @Test
    void recordReading_flagsSurgeAndKeepsRemoteReadMode() {
        repo.saveMeter(meter("mtr_e", "electricity", null, "merchant", "mch_1", "remote"));
        repo.saveReading(reading("old", "mtr_e", "2026-05", "100", "0", "100"));

        EnergyMeterReadingRecord rec = service.recordReading(new EnergyReadingRecordRequest(
                "k_surge", "mtr_e", "2026-06", bd("260"), null, "manual", "usr_reader", null));

        assertThat(rec.status()).isEqualTo("abnormal");
        assertThat(rec.abnormalReason()).isEqualTo("surge");
        assertThat(rec.prevReadingValue()).isEqualByComparingTo("100");
        assertThat(rec.consumption()).isEqualByComparingTo("160");
        assertThat(rec.readMode()).isEqualTo("remote");
    }

    @Test
    void recordReading_flagsDropAgainstPreviousConsumption() {
        repo.saveMeter(meter("mtr_w", "water", null, "merchant", "mch_1", "manual"));
        repo.saveReading(reading("old", "mtr_w", "2026-05", "100", "0", "100"));

        EnergyMeterReadingRecord rec = service.recordReading(new EnergyReadingRecordRequest(
                "k_drop", "mtr_w", "2026-06", bd("140"), null, "manual", "usr_reader", null));

        assertThat(rec.status()).isEqualTo("abnormal");
        assertThat(rec.abnormalReason()).isEqualTo("drop");
        assertThat(rec.consumption()).isEqualByComparingTo("40");
    }

    @Test
    void recordReading_flagsZeroReadingOrConsumption() {
        repo.saveMeter(meter("mtr_g", "gas", null, "merchant", "mch_1", "manual"));
        repo.saveReading(reading("old", "mtr_g", "2026-05", "100", "0", "100"));

        EnergyMeterReadingRecord rec = service.recordReading(new EnergyReadingRecordRequest(
                "k_zero", "mtr_g", "2026-06", bd("100"), null, "manual", "usr_reader", null));

        assertThat(rec.status()).isEqualTo("abnormal");
        assertThat(rec.abnormalReason()).isEqualTo("zero");
        assertThat(rec.consumption()).isEqualByComparingTo("0");
    }

    @Test
    void recordReading_flagsReverseWithoutRejectingIt() {
        repo.saveMeter(meter("mtr_h", "heat_cold", null, "merchant", "mch_1", "manual"));
        repo.saveReading(reading("old", "mtr_h", "2026-05", "100", "0", "100"));

        EnergyMeterReadingRecord rec = service.recordReading(new EnergyReadingRecordRequest(
                "k_reverse", "mtr_h", "2026-06", bd("90"), null, "manual", "usr_reader", null));

        assertThat(rec.status()).isEqualTo("abnormal");
        assertThat(rec.abnormalReason()).isEqualTo("reverse");
        assertThat(rec.prevReadingValue()).isEqualByComparingTo("100");
        assertThat(rec.consumption()).isEqualByComparingTo("-10");
        assertThat(repo.findReadingById(rec.id())).isPresent();
    }

    @Test
    void recordReading_firstPeriodUsesZeroPreviousAndRecordsNormally() {
        repo.saveMeter(meter("mtr_first", "electricity", null, "merchant", "mch_1", "remote"));

        EnergyMeterReadingRecord rec = service.recordReading(new EnergyReadingRecordRequest(
                "k_first", "mtr_first", "2026-06", bd("88"), null, "manual", "usr_reader", null));

        assertThat(rec.status()).isEqualTo("recorded");
        assertThat(rec.prevReadingValue()).isEqualByComparingTo("0");
        assertThat(rec.consumption()).isEqualByComparingTo("88");
    }

    @Test
    void recordReading_reverseUsesStoredPreviousReadingEvenWhenCallerSuppliesLowerPrevious() {
        repo.saveMeter(meter("mtr_reverse_auto", "electricity", null, "merchant", "mch_1", "remote"));
        repo.saveReading(reading("old", "mtr_reverse_auto", "2026-05", "100", "0", "100"));

        EnergyMeterReadingRecord rec = service.recordReading(new EnergyReadingRecordRequest(
                "k_reverse_auto", "mtr_reverse_auto", "2026-06", bd("90"), bd("0"), "manual", "usr_reader", null));

        assertThat(rec.status()).isEqualTo("abnormal");
        assertThat(rec.abnormalReason()).isEqualTo("reverse");
        assertThat(rec.prevReadingValue()).isEqualByComparingTo("100");
        assertThat(rec.consumption()).isEqualByComparingTo("-10");
    }

    @Test
    void meterDifferenceComparesParentConsumptionWithChildrenSum() {
        repo.saveMeter(meter("main", "electricity", null, "public_area", null, "remote"));
        repo.saveMeter(meter("sub_1", "electricity", "main", "merchant", "mch_1", "remote"));
        repo.saveMeter(meter("sub_2", "electricity", "main", "merchant", "mch_2", "remote"));
        repo.saveReading(reading("r_main", "main", "2026-06", "300", "0", "300"));
        repo.saveReading(reading("r_sub1", "sub_1", "2026-06", "120", "0", "120"));
        repo.saveReading(reading("r_sub2", "sub_2", "2026-06", "150", "0", "150"));

        EnergyMeterDifferenceRecord diff = service.meterDifference("main", "2026-06");

        assertThat(diff.parentConsumption()).isEqualByComparingTo("300");
        assertThat(diff.childConsumption()).isEqualByComparingTo("270");
        assertThat(diff.difference()).isEqualByComparingTo("30");
    }

    @Test
    void settleEnergyFeeAllocatesByContractAreaAndUsageThenSkipsDuplicates() {
        repo.contracts.add(new EnergyContractSnapshot("ct_fixed", "mch_fixed", "proj_1",
                "by_contract", 1000L, bd("10"), 10L));
        repo.contracts.add(new EnergyContractSnapshot("ct_area_1", "mch_area_1", "proj_1",
                "by_area", 0L, bd("100"), 10L));
        repo.contracts.add(new EnergyContractSnapshot("ct_area_2", "mch_area_2", "proj_1",
                "by_area", 0L, bd("300"), 10L));
        repo.contracts.add(new EnergyContractSnapshot("ct_usage_1", "mch_usage_1", "proj_1",
                "by_usage", 0L, bd("10"), 10L));
        repo.contracts.add(new EnergyContractSnapshot("ct_usage_2", "mch_usage_2", "proj_1",
                "by_usage", 0L, bd("10"), 10L));
        repo.contracts.add(new EnergyContractSnapshot("ct_none", "mch_none", "proj_1",
                "none", 9999L, bd("10"), 10L));
        repo.saveMeter(meter("public", "electricity", null, "public_area", null, "remote"));
        repo.saveMeter(meter("usage_1", "electricity", null, "merchant", "mch_usage_1", "remote"));
        repo.saveMeter(meter("usage_2", "electricity", null, "merchant", "mch_usage_2", "remote"));
        repo.saveReading(reading("r_public", "public", "2026-06", "100", "0", "100"));
        repo.saveReading(reading("r_usage_1", "usage_1", "2026-06", "20", "0", "20"));
        repo.saveReading(reading("r_usage_2", "usage_2", "2026-06", "80", "0", "80"));

        List<EnergyFeeSettlementRecord> first = service.settleEnergyFee("2026-06");
        List<EnergyFeeSettlementRecord> second = service.settleEnergyFee("2026-06");

        assertThat(first).extracting(EnergyFeeSettlementRecord::merchantId)
                .containsExactlyInAnyOrder("mch_fixed", "mch_area_1", "mch_area_2", "mch_usage_1", "mch_usage_2");
        assertThat(amountByMerchant(first)).containsEntry("mch_fixed", 1000L)
                .containsEntry("mch_area_1", 250L)
                .containsEntry("mch_area_2", 750L)
                .containsEntry("mch_usage_1", 200L)
                .containsEntry("mch_usage_2", 800L);
        assertThat(second).isEmpty();
        assertThat(repo.arItems).hasSize(5);
        assertThat(repo.arItems.values()).allMatch(ar -> "energy_fee".equals(ar.sourceType()))
                .allMatch(ar -> "receivable".equals(ar.direction()))
                .allMatch(ar -> "pending_collection".equals(ar.status()));
    }

    @Test
    void settleEnergyFee_noEffectiveContractsReturnsEmptyList() {
        assertThat(service.settleEnergyFee("2026-06")).isEmpty();
        assertThat(repo.arItems).isEmpty();
    }

    private static Map<String, Long> amountByMerchant(List<EnergyFeeSettlementRecord> records) {
        Map<String, Long> out = new LinkedHashMap<>();
        for (EnergyFeeSettlementRecord r : records) {
            out.put(r.merchantId(), r.amountCent());
        }
        return out;
    }

    private static EnergyMeterRecord meter(
            String id, String type, String parentId, String gatherObject, String merchantId, String readMode) {
        Instant now = Instant.now();
        return new EnergyMeterRecord(id, id, type, parentId, gatherObject,
                null, merchantId, "proj_1", readMode, true, now, now);
    }

    private static EnergyMeterReadingRecord reading(
            String id, String meterId, String period, String reading, String prev, String consumption) {
        Instant now = Instant.now();
        return new EnergyMeterReadingRecord(id, meterId, period, bd(reading), bd(prev), bd(consumption),
                "recorded", null, "remote", "usr_seed", now, null, null, now, now);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    static class InMemoryEnergyRepository implements EnergyRepository {
        final Map<String, EnergyMeterRecord> meters = new LinkedHashMap<>();
        final Map<String, EnergyMeterReadingRecord> readings = new LinkedHashMap<>();
        final Map<String, EnergyAnomalyThresholdRecord> thresholds = new LinkedHashMap<>();
        final List<EnergyContractSnapshot> contracts = new ArrayList<>();
        final Map<String, EnergyFeeSettlementRecord> arItems = new LinkedHashMap<>();

        InMemoryEnergyRepository() {
            Instant now = Instant.now();
            for (String type : List.of("electricity", "water", "gas", "heat_cold")) {
                thresholds.put(type, new EnergyAnomalyThresholdRecord(
                        "thr_" + type, type, bd("50"), bd("50"), true, now, now));
            }
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
        public EnergyMeterRecord saveMeter(EnergyMeterRecord meter) {
            meters.put(meter.id(), meter);
            return meter;
        }

        @Override
        public List<EnergyMeterReadingRecord> listReadings(String period) {
            return readings.values().stream().filter(r -> period == null || period.equals(r.period())).toList();
        }

        @Override
        public Optional<EnergyMeterReadingRecord> findReadingById(String id) {
            return Optional.ofNullable(readings.get(id));
        }

        @Override
        public Optional<EnergyMeterReadingRecord> findReadingByMeterAndPeriod(String meterId, String period) {
            return readings.values().stream()
                    .filter(r -> r.meterId().equals(meterId) && r.period().equals(period)).findFirst();
        }

        @Override
        public Optional<EnergyMeterReadingRecord> findLatestReadingBefore(String meterId, String period) {
            return readings.values().stream()
                    .filter(r -> r.meterId().equals(meterId) && r.period().compareTo(period) < 0)
                    .max((a, b) -> a.period().compareTo(b.period()));
        }

        @Override
        public EnergyMeterReadingRecord saveReading(EnergyMeterReadingRecord reading) {
            readings.put(reading.id(), reading);
            return reading;
        }

        @Override
        public Optional<EnergyAnomalyThresholdRecord> findThresholdByMeterType(String meterType) {
            return Optional.ofNullable(thresholds.get(meterType));
        }

        @Override
        public List<EnergyMeterReadingRecord> listChildReadings(String parentMeterId, String period) {
            return readings.values().stream()
                    .filter(r -> period.equals(r.period()))
                    .filter(r -> parentMeterId.equals(meters.get(r.meterId()).parentMeterId()))
                    .toList();
        }

        @Override
        public List<EnergyContractSnapshot> listEffectiveContracts(String period) {
            return List.copyOf(contracts);
        }

        @Override
        public BigDecimal sumConsumptionByGatherObject(String gatherObject, String period, String projectId) {
            return readings.values().stream()
                    .filter(r -> period.equals(r.period()))
                    .filter(r -> "recorded".equals(r.status()) || "reviewed".equals(r.status()))
                    .filter(r -> gatherObject.equals(meters.get(r.meterId()).gatherObject()))
                    .filter(r -> projectId == null || projectId.equals(meters.get(r.meterId()).projectId()))
                    .map(EnergyMeterReadingRecord::consumption)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        @Override
        public BigDecimal sumMerchantConsumption(String merchantId, String period, String projectId) {
            return readings.values().stream()
                    .filter(r -> period.equals(r.period()))
                    .filter(r -> "recorded".equals(r.status()) || "reviewed".equals(r.status()))
                    .map(r -> Map.entry(r, meters.get(r.meterId())))
                    .filter(e -> "merchant".equals(e.getValue().gatherObject()))
                    .filter(e -> merchantId.equals(e.getValue().merchantId()))
                    .filter(e -> projectId == null || projectId.equals(e.getValue().projectId()))
                    .map(e -> e.getKey().consumption())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        @Override
        public boolean energyFeeArExists(String merchantId, String period) {
            return arItems.containsKey(EnergyService.energyFeeSourceId(merchantId, period));
        }

        @Override
        public EnergyFeeSettlementRecord insertEnergyFeeAr(EnergyFeeSettlementRecord record) {
            arItems.putIfAbsent(record.sourceId(), record);
            return arItems.get(record.sourceId());
        }
    }
}
