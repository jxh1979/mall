package com.complex.platform.energy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

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

/** 能耗服务回归：异常原始事实、缺失起点阻断、公区不挂账、独表实际费用。 */
// 文件版本：V0.02
class EnergyServiceTests {

    @Test
    void approvedRulesDoNotCreatePublicAreaReceivables() {
        repo.contracts.add(new EnergyContractSnapshot("ct_area", "mch_area", "proj_1",
                "by_area", 0L, bd("100"), 10L));
        repo.saveMeter(meter("public", "electricity", null, "public_area", null, "remote"));
        repo.saveReading(reading("r_public", "public", "2026-06", "100", "0", "100"));
        assertThat(service.settleEnergyFee("2026-06")).isEmpty();
        assertThat(repo.arItems).isEmpty();
    }

    @Test
    void unknownOpeningReadingMustNotBecomeZeroBasedBillableConsumption() {
        repo.saveMeter(meter("new_meter", "electricity", null, "merchant", "mch_1", "remote"));
        var saved = service.recordReading(new EnergyReadingRecordRequest(
                "no_baseline", "new_meter", "2026-06", bd("150"), null, "manual", "usr_reader", Instant.parse("2026-06-15T00:00:00Z"),List.of("photo_new_meter"),"补充核表"));
        assertThat(saved.status()).isEqualTo("abnormal");
        assertThat(saved.prevReadingValue()).isNull();
        assertThat(saved.consumption()).isNull();
    }

    @Test
    void abnormalReadingCannotBeConfirmedByArbitraryNamedReviewer() {
        repo.saveMeter(meter("mtr_bad", "electricity", null, "merchant", "mch_1", "remote"));
        Instant now = Instant.now();
        repo.saveReading(new EnergyMeterReadingRecord("bad", "mtr_bad", "2026-06", bd("90"),
                bd("100"), bd("-10"), "abnormal", "reverse", "remote", "usr_reader", now,
                null, null, now, now));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.reviewReading("bad",
                new com.complex.platform.energy.dto.EnergyReadingReviewRequest("review_bad", "any_user")))
                .isInstanceOf(com.complex.platform.common.api.ApiException.class);
        assertThat(repo.findReadingById("bad").orElseThrow().status()).isEqualTo("abnormal");
    }


    private final InMemoryEnergyRepository repo = new InMemoryEnergyRepository();
    private final EnergyWorkflowRepository workflow=mock(EnergyWorkflowRepository.class);
    private final AuthorizationService authorization=mock(AuthorizationService.class);
    private final com.complex.platform.common.idempotency.IdempotencyService idempotency=mock(com.complex.platform.common.idempotency.IdempotencyService.class);
    private final com.complex.platform.evidence.EvidenceEventRepository evidence=mock(com.complex.platform.evidence.EvidenceEventRepository.class);
    private final com.complex.platform.evidence.EvidenceAttachmentValidator files=mock(com.complex.platform.evidence.EvidenceAttachmentValidator.class);
    private final com.complex.platform.governance.review.SupervisorReviewService reviews=mock(com.complex.platform.governance.review.SupervisorReviewService.class);
    private final EnergyService service=new EnergyService(repo,workflow,mock(AuditEventService.class),authorization,idempotency,evidence,files,reviews);
    @org.junit.jupiter.api.BeforeEach void configure() {
        when(authorization.currentActor()).thenReturn(new com.complex.platform.access.AccessActor("usr_reader","读数员","staff","proj_1",null,"active"));
        when(authorization.canReachProject(any(),anyString(),any())).thenReturn(true);
        when(idempotency.execute(anyString(),anyString(),any(),any(java.util.function.Supplier.class),any(Class.class))).thenAnswer(call->((java.util.function.Supplier<?>)call.getArgument(3)).get());
        when(workflow.effectiveValue(any())).thenAnswer(call->((EnergyMeterReadingRecord)call.getArgument(0)).readingValue());
        when(workflow.effectiveConsumption(any())).thenAnswer(call->((EnergyMeterReadingRecord)call.getArgument(0)).consumption());
        when(evidence.findAttachmentById(startsWith("photo_"))).thenAnswer(call->{String id=call.getArgument(0);return Optional.of(new com.complex.platform.evidence.EvidenceAttachmentRecord(id,"表底.png","image/png",1L,"energy_meter",id.substring(6),"reading","valid","valid","uploaded",null,Instant.now(),Instant.now(),null));});
    }

    @Test
    void recordReading_flagsSurgeAndRecordsManualEvidence() {
        repo.saveMeter(meter("mtr_e", "electricity", null, "merchant", "mch_1", "remote"));
        repo.saveReading(reading("old", "mtr_e", "2026-05", "100", "0", "100"));

        EnergyMeterReadingRecord rec = service.recordReading(new EnergyReadingRecordRequest(
                "k_surge", "mtr_e", "2026-06", bd("260"), null, "manual", "usr_reader", Instant.parse("2026-06-15T00:00:00Z"),List.of("photo_mtr_e"),"补充核表"));

        assertThat(rec.status()).isEqualTo("abnormal");
        assertThat(rec.abnormalReason()).isEqualTo("surge");
        assertThat(rec.prevReadingValue()).isEqualByComparingTo("100");
        assertThat(rec.consumption()).isEqualByComparingTo("160");
        assertThat(rec.readMode()).isEqualTo("manual");
    }

    @Test
    void recordReading_flagsSurgeWhenPreviousConsumptionWasZeroBaseline() {
        repo.saveMeter(meter("mtr_zb", "electricity", null, "merchant", "mch_1", "remote"));
        // 上一期 consumption=0(表没走)，本期跳到大于 0 → 0 基线无法算环比%，按可疑跳变进复核(surge)
        repo.saveReading(reading("zb_prev", "mtr_zb", "2026-05", "100", "100", "0"));

        EnergyMeterReadingRecord rec = service.recordReading(new EnergyReadingRecordRequest(
                "k_zb", "mtr_zb", "2026-06", bd("4200"), null, "manual", "usr_reader", Instant.parse("2026-06-15T00:00:00Z"),List.of("photo_mtr_zb"),"补充核表"));

        assertThat(rec.status()).isEqualTo("abnormal");
        assertThat(rec.abnormalReason()).isEqualTo("surge");
        assertThat(rec.consumption()).isEqualByComparingTo("4100");
    }

    @Test
    void recordReading_flagsDropAgainstPreviousConsumption() {
        repo.saveMeter(meter("mtr_w", "water", null, "merchant", "mch_1", "manual"));
        repo.saveReading(reading("old", "mtr_w", "2026-05", "100", "0", "100"));

        EnergyMeterReadingRecord rec = service.recordReading(new EnergyReadingRecordRequest(
                "k_drop", "mtr_w", "2026-06", bd("140"), null, "manual", "usr_reader", Instant.parse("2026-06-15T00:00:00Z"),List.of("photo_mtr_w"),"补充核表"));

        assertThat(rec.status()).isEqualTo("abnormal");
        assertThat(rec.abnormalReason()).isEqualTo("drop");
        assertThat(rec.consumption()).isEqualByComparingTo("40");
    }

    @Test
    void recordReading_flagsZeroReadingOrConsumption() {
        repo.saveMeter(meter("mtr_g", "gas", null, "merchant", "mch_1", "manual"));
        repo.saveReading(reading("old", "mtr_g", "2026-05", "100", "0", "100"));

        EnergyMeterReadingRecord rec = service.recordReading(new EnergyReadingRecordRequest(
                "k_zero", "mtr_g", "2026-06", bd("100"), null, "manual", "usr_reader", Instant.parse("2026-06-15T00:00:00Z"),List.of("photo_mtr_g"),"补充核表"));

        assertThat(rec.status()).isEqualTo("abnormal");
        assertThat(rec.abnormalReason()).isEqualTo("zero");
        assertThat(rec.consumption()).isEqualByComparingTo("0");
    }

    @Test
    void recordReading_flagsReverseWithoutRejectingIt() {
        repo.saveMeter(meter("mtr_h", "heat_cold", null, "merchant", "mch_1", "manual"));
        repo.saveReading(reading("old", "mtr_h", "2026-05", "100", "0", "100"));

        EnergyMeterReadingRecord rec = service.recordReading(new EnergyReadingRecordRequest(
                "k_reverse", "mtr_h", "2026-06", bd("90"), null, "manual", "usr_reader", Instant.parse("2026-06-15T00:00:00Z"),List.of("photo_mtr_h"),"补充核表"));

        assertThat(rec.status()).isEqualTo("abnormal");
        assertThat(rec.abnormalReason()).isEqualTo("reverse");
        assertThat(rec.prevReadingValue()).isEqualByComparingTo("100");
        assertThat(rec.consumption()).isEqualByComparingTo("-10");
        assertThat(repo.findReadingById(rec.id())).isPresent();
    }

    @Test
    void recordReading_firstPeriodWithoutOpeningIsKeptForVerification() {
        repo.saveMeter(meter("mtr_first", "electricity", null, "merchant", "mch_1", "remote"));

        EnergyMeterReadingRecord rec = service.recordReading(new EnergyReadingRecordRequest(
                "k_first", "mtr_first", "2026-06", bd("88"), null, "manual", "usr_reader", Instant.parse("2026-06-15T00:00:00Z"),List.of("photo_mtr_first"),"补充核表"));

        assertThat(rec.status()).isEqualTo("abnormal");
        assertThat(rec.prevReadingValue()).isNull();
        assertThat(rec.consumption()).isNull();
    }

    @Test
    void recordReading_reverseUsesStoredPreviousReadingEvenWhenCallerSuppliesLowerPrevious() {
        repo.saveMeter(meter("mtr_reverse_auto", "electricity", null, "merchant", "mch_1", "remote"));
        repo.saveReading(reading("old", "mtr_reverse_auto", "2026-05", "100", "0", "100"));

        EnergyMeterReadingRecord rec = service.recordReading(new EnergyReadingRecordRequest(
                "k_reverse_auto", "mtr_reverse_auto", "2026-06", bd("90"), bd("0"), "manual", "usr_reader", Instant.parse("2026-06-15T00:00:00Z"),List.of("photo_mtr_reverse_auto"),"补充核表"));

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
        Instant now = java.time.YearMonth.parse(period).atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        return new EnergyMeterReadingRecord(id, meterId, period, bd(reading), bd(prev), bd(consumption),
                "reviewed", null, "remote", "usr_seed", now, null, null, now, now);
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
        public boolean energyFeeArExists(String merchantId, String contractId, String period) {
            return arItems.containsKey(EnergyService.energyFeeSourceId(merchantId, contractId, period));
        }

        @Override
        public EnergyFeeSettlementRecord insertEnergyFeeAr(EnergyFeeSettlementRecord record) {
            arItems.putIfAbsent(record.sourceId(), record);
            return arItems.get(record.sourceId());
        }
    }
}
