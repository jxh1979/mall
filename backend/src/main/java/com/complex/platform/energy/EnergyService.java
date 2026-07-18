package com.complex.platform.energy;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.energy.EnergyRepository.EnergyContractSnapshot;
import com.complex.platform.energy.dto.EnergyMeterRequest;
import com.complex.platform.energy.dto.EnergyReadingRecordRequest;
import com.complex.platform.energy.dto.EnergyReadingReviewRequest;
import com.complex.platform.evidence.AuditEventService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 楼宇运营 S4 能耗抄表。 */
@Service
public class EnergyService {

    private static final Set<String> METER_TYPES = Set.of("electricity", "water", "gas", "heat_cold");
    private static final Set<String> GATHER_OBJECTS = Set.of("public_area", "merchant", "equipment_circuit");
    private static final Set<String> READ_MODES = Set.of("remote", "manual");
    private static final String METER_PERM = "energy.meter.manage";
    private static final String READING_PERM = "energy.reading.manage";
    private static final String SOURCE_TYPE_ENERGY_FEE = "energy_fee";
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal DEFAULT_THRESHOLD_PCT = BigDecimal.valueOf(50);

    private final EnergyRepository repository;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;

    public EnergyService(EnergyRepository repository, AuditEventService auditEventService,
            AuthorizationService authorizationService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
    }

    public PageResponse<EnergyMeterRecord> listMeters() {
        List<EnergyMeterRecord> items = repository.listMeters();
        return new PageResponse<>(items, 1, 500, items.size());
    }

    @Transactional
    public EnergyMeterRecord createMeter(EnergyMeterRequest request) {
        authorizationService.requirePlatformScope();
        EnergyMeterRequest r = requireRequest(request);
        validateMeterType(r.meterType());
        validateGatherObject(r.gatherObject());
        String name = requireText(r.name(), "name");
        String readMode = requireReadMode(r.readMode() == null ? "remote" : r.readMode());
        Instant now = Instant.now();
        String id = r.id() == null || r.id().isBlank() ? "em_" + UUID.randomUUID() : r.id().trim();
        EnergyMeterRecord meter = repository.saveMeter(new EnergyMeterRecord(id, name, r.meterType(),
                trimToNull(r.parentMeterId()), r.gatherObject(), trimToNull(r.spaceId()), trimToNull(r.merchantId()),
                trimToNull(r.projectId()), readMode, r.active() == null || r.active(), now, now));
        auditEventService.recordSuccess("energy.meter_saved", METER_PERM, "energy_meter", meter.id(),
                null, r.idempotencyKey(), Map.of("meter_type", meter.meterType()));
        return meter;
    }

    @Transactional
    public EnergyMeterRecord updateMeter(String id, EnergyMeterRequest request) {
        authorizationService.requirePlatformScope();
        EnergyMeterRecord cur = loadMeter(id);
        EnergyMeterRequest r = requireRequest(request);
        String meterType = r.meterType() == null ? cur.meterType() : r.meterType();
        String gatherObject = r.gatherObject() == null ? cur.gatherObject() : r.gatherObject();
        validateMeterType(meterType);
        validateGatherObject(gatherObject);
        String readMode = requireReadMode(r.readMode() == null ? cur.readMode() : r.readMode());
        EnergyMeterRecord meter = repository.saveMeter(new EnergyMeterRecord(cur.id(),
                r.name() == null || r.name().isBlank() ? cur.name() : r.name().trim(), meterType,
                r.parentMeterId() == null ? cur.parentMeterId() : trimToNull(r.parentMeterId()), gatherObject,
                r.spaceId() == null ? cur.spaceId() : trimToNull(r.spaceId()),
                r.merchantId() == null ? cur.merchantId() : trimToNull(r.merchantId()),
                r.projectId() == null ? cur.projectId() : trimToNull(r.projectId()), readMode,
                r.active() == null ? cur.active() : r.active(), cur.createdAt(), Instant.now()));
        auditEventService.recordSuccess("energy.meter_saved", METER_PERM, "energy_meter", meter.id(),
                null, r.idempotencyKey(), Map.of("meter_type", meter.meterType()));
        return meter;
    }

    public PageResponse<EnergyMeterReadingRecord> listReadings(String period) {
        String p = period == null || period.isBlank() ? null : requirePeriod(period);
        List<EnergyMeterReadingRecord> items = repository.listReadings(p);
        return new PageResponse<>(items, 1, 500, items.size());
    }

    @Transactional
    public EnergyMeterReadingRecord recordReading(EnergyReadingRecordRequest request) {
        authorizationService.requirePlatformScope();
        EnergyReadingRecordRequest r = requireRequest(request);
        String period = requirePeriod(r.period());
        EnergyMeterRecord meter = loadMeter(requireText(r.meterId(), "meter_id"));
        if (repository.findReadingByMeterAndPeriod(meter.id(), period).isPresent()) {
            throw ApiException.conflict("ENERGY_READING_ALREADY_RECORDED", "meter already has reading for " + period);
        }
        BigDecimal readingValue = requireDecimal(r.readingValue(), "reading_value");
        EnergyMeterReadingRecord latestBefore = repository.findLatestReadingBefore(meter.id(), period).orElse(null);
        BigDecimal prevReadingValue = latestBefore == null ? BigDecimal.ZERO : latestBefore.readingValue();
        BigDecimal consumption = readingValue.subtract(prevReadingValue);
        Abnormal abnormal = classify(meter, readingValue, prevReadingValue, consumption,
                latestBefore == null ? BigDecimal.ZERO : latestBefore.consumption(), latestBefore != null);
        String readMode = "remote".equals(meter.readMode())
                ? "remote"
                : requireReadMode(r.readMode() == null ? "manual" : r.readMode());
        Instant now = Instant.now();
        EnergyMeterReadingRecord rec = repository.saveReading(new EnergyMeterReadingRecord(
                "emr_" + UUID.randomUUID(), meter.id(), period, readingValue, prevReadingValue, consumption,
                abnormal.reason() == null ? "recorded" : "abnormal", abnormal.reason(), readMode,
                trimToNull(r.readerId()), r.readAt() == null ? now : r.readAt(), null, null, now, now));
        auditEventService.recordSuccess("energy.reading_recorded", READING_PERM, "energy_meter_reading", rec.id(),
                null, r.idempotencyKey(), Map.of("period", period, "status", rec.status()));
        return rec;
    }

    @Transactional
    public EnergyMeterReadingRecord reviewReading(String readingId, EnergyReadingReviewRequest request) {
        authorizationService.requirePlatformScope();
        EnergyReadingReviewRequest r = requireRequest(request);
        EnergyMeterReadingRecord cur = repository.findReadingById(requireText(readingId, "reading_id"))
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "energy reading not found"));
        Instant now = Instant.now();
        EnergyMeterReadingRecord rec = repository.saveReading(new EnergyMeterReadingRecord(cur.id(), cur.meterId(),
                cur.period(), cur.readingValue(), cur.prevReadingValue(), cur.consumption(), "reviewed",
                cur.abnormalReason(), cur.readMode(), cur.readerId(), cur.readAt(),
                requireText(r.reviewedBy(), "reviewed_by"), now, cur.createdAt(), now));
        auditEventService.recordSuccess("energy.reading_reviewed", READING_PERM, "energy_meter_reading", rec.id(),
                null, r.idempotencyKey(), Map.of("period", rec.period()));
        return rec;
    }

    public EnergyMeterDifferenceRecord meterDifference(String parentMeterId, String period) {
        String p = requirePeriod(period);
        EnergyMeterRecord meter = loadMeter(requireText(parentMeterId, "parent_meter_id"));
        BigDecimal parent = repository.findReadingByMeterAndPeriod(meter.id(), p)
                .map(EnergyMeterReadingRecord::consumption).orElse(BigDecimal.ZERO);
        BigDecimal children = repository.listChildReadings(meter.id(), p).stream()
                .map(EnergyMeterReadingRecord::consumption).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EnergyMeterDifferenceRecord(meter.id(), p, parent, children, parent.subtract(children));
    }

    public List<EnergyFeeSettlementRecord> settleEnergyFee(String period) {
        return settleEnergyFee(period, null);
    }

    @Transactional
    public List<EnergyFeeSettlementRecord> settleEnergyFee(String period, String idempotencyKey) {
        authorizationService.requirePlatformScope();
        String p = requirePeriod(period);
        List<EnergyContractSnapshot> contracts = repository.listEffectiveContracts(p).stream()
                .filter(c -> c.merchantId() != null && !"none".equals(c.energyFeeBasis()))
                .toList();
        Map<String, BigDecimal> areaTotals = new HashMap<>();
        Map<String, BigDecimal> usageTotals = new HashMap<>();
        Map<String, BigDecimal> merchantUsage = new HashMap<>();
        for (EnergyContractSnapshot c : contracts) {
            String key = projectKey(c.projectId());
            if ("by_area".equals(c.energyFeeBasis()) && positive(c.areaSquareMeter())) {
                areaTotals.merge(key, c.areaSquareMeter(), BigDecimal::add);
            }
            if ("by_usage".equals(c.energyFeeBasis())) {
                BigDecimal usage = repository.sumMerchantConsumption(c.merchantId(), p, c.projectId());
                merchantUsage.put(c.contractId(), usage);
                if (positive(usage)) {
                    usageTotals.merge(key, usage, BigDecimal::add);
                }
            }
        }

        Map<String, BigDecimal> publicConsumptionByProject = new LinkedHashMap<>();
        List<EnergyFeeSettlementRecord> out = new ArrayList<>();
        for (EnergyContractSnapshot c : contracts) {
            if (repository.energyFeeArExists(c.merchantId(), c.contractId(), p)) {
                continue;
            }
            long amountCent = amountCent(c, p, areaTotals, usageTotals, merchantUsage, publicConsumptionByProject);
            if (amountCent <= 0) {
                continue;
            }
            String sourceId = energyFeeSourceId(c.merchantId(), c.contractId(), p);
            String token = stableToken(sourceId);
            Instant now = Instant.now();
            EnergyFeeSettlementRecord rec = repository.insertEnergyFeeAr(new EnergyFeeSettlementRecord(
                    "efs_" + token.substring(0, 28), c.contractId(), c.merchantId(), c.projectId(), p,
                    c.energyFeeBasis(), amountCent, "arap_energy_" + token.substring(0, 24),
                    SOURCE_TYPE_ENERGY_FEE, sourceId, "receivable", "pending_collection", now));
            out.add(rec);
        }
        auditEventService.recordSuccess("energy.fee_settled", READING_PERM, "energy_fee", p,
                null, idempotencyKey, Map.of("period", p, "count", out.size()));
        return out;
    }

    private long amountCent(EnergyContractSnapshot c, String period, Map<String, BigDecimal> areaTotals,
            Map<String, BigDecimal> usageTotals, Map<String, BigDecimal> merchantUsage,
            Map<String, BigDecimal> publicConsumptionByProject) {
        if ("by_contract".equals(c.energyFeeBasis())) {
            return c.energyFeeCent() == null ? 0L : c.energyFeeCent();
        }
        BigDecimal publicConsumption = publicConsumptionByProject.computeIfAbsent(projectKey(c.projectId()),
                ignored -> repository.sumConsumptionByGatherObject("public_area", period, c.projectId()));
        long publicFeeCent = publicFeeCent(c, publicConsumption);
        if (publicFeeCent <= 0) {
            return 0L;
        }
        if ("by_area".equals(c.energyFeeBasis())) {
            BigDecimal totalArea = areaTotals.get(projectKey(c.projectId()));
            if (!positive(c.areaSquareMeter()) || !positive(totalArea)) {
                return 0L;
            }
            return BigDecimal.valueOf(publicFeeCent).multiply(c.areaSquareMeter())
                    .divide(totalArea, 0, RoundingMode.HALF_UP).longValue();
        }
        if ("by_usage".equals(c.energyFeeBasis())) {
            BigDecimal usage = merchantUsage.getOrDefault(c.contractId(), BigDecimal.ZERO);
            BigDecimal totalUsage = usageTotals.get(projectKey(c.projectId()));
            if (!positive(usage) || !positive(totalUsage)) {
                return 0L;
            }
            return BigDecimal.valueOf(publicFeeCent).multiply(usage)
                    .divide(totalUsage, 0, RoundingMode.HALF_UP).longValue();
        }
        return 0L;
    }

    private long publicFeeCent(EnergyContractSnapshot c, BigDecimal publicConsumption) {
        if (!positive(publicConsumption)) {
            return 0L;
        }
        long unitPriceCent = c.energyUnitPriceCent() == null ? 1L : c.energyUnitPriceCent();
        if (unitPriceCent <= 0) {
            return 0L;
        }
        return publicConsumption.multiply(BigDecimal.valueOf(unitPriceCent))
                .setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private Abnormal classify(EnergyMeterRecord meter, BigDecimal readingValue, BigDecimal prevReadingValue,
            BigDecimal consumption, BigDecimal previousConsumption, boolean hasPrior) {
        if (readingValue.compareTo(prevReadingValue) < 0) {
            return new Abnormal("reverse");
        }
        if (readingValue.signum() == 0 || consumption.signum() == 0) {
            return new Abnormal("zero");
        }
        if (previousConsumption.signum() <= 0) {
            // ponytail: 0 基线无法算环比%。有上一期且其用量=0、本期>0 = 可疑跳变 → 进复核(surge)；
            // 首期(无上一期)不误报。本期 consumption>0 已被上面 zero 档保证。
            return new Abnormal(hasPrior ? "surge" : null);
        }
        EnergyAnomalyThresholdRecord threshold = repository.findThresholdByMeterType(meter.meterType())
                .filter(EnergyAnomalyThresholdRecord::active)
                .orElse(new EnergyAnomalyThresholdRecord("default", meter.meterType(),
                        DEFAULT_THRESHOLD_PCT, DEFAULT_THRESHOLD_PCT, true, null, null));
        if (consumption.compareTo(previousConsumption) > 0
                && pct(consumption.subtract(previousConsumption), previousConsumption)
                .compareTo(threshold.surgePct()) > 0) {
            return new Abnormal("surge");
        }
        if (consumption.compareTo(previousConsumption) < 0
                && pct(previousConsumption.subtract(consumption), previousConsumption)
                .compareTo(threshold.dropPct()) > 0) {
            return new Abnormal("drop");
        }
        return new Abnormal(null);
    }

    private BigDecimal pct(BigDecimal delta, BigDecimal base) {
        return delta.multiply(ONE_HUNDRED).divide(base, 6, RoundingMode.HALF_UP);
    }

    private EnergyMeterRecord loadMeter(String id) {
        return repository.findMeterById(id)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "energy meter not found"));
    }

    private void validateMeterType(String type) {
        if (type == null || !METER_TYPES.contains(type)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "meter_type is invalid");
        }
    }

    private void validateGatherObject(String gatherObject) {
        if (gatherObject == null || !GATHER_OBJECTS.contains(gatherObject)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "gather_object is invalid");
        }
    }

    private String requireReadMode(String readMode) {
        if (readMode == null || !READ_MODES.contains(readMode)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "read_mode is invalid");
        }
        return readMode;
    }

    private String requirePeriod(String period) {
        String p = requireText(period, "period");
        if (!p.matches("\\d{4}-\\d{2}")) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period must be YYYY-MM");
        }
        return p;
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return value.trim();
    }

    private BigDecimal requireDecimal(BigDecimal value, String field) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return value;
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private static String projectKey(String projectId) {
        return projectId == null ? "__default__" : projectId;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static String energyFeeSourceId(String merchantId, String contractId, String period) {
        // AUD-005：去重键加合同维度,一商户多合同同期各自独立结算,避免按 merchant+period 去重时第二份合同静默漏计。
        return SOURCE_TYPE_ENERGY_FEE + "_" + stableToken(merchantId + "|" + contractId + "|" + period);
    }

    private static String stableToken(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
    }

    private record Abnormal(String reason) {
    }
}
