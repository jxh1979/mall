package com.complex.platform.energy;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.energy.dto.EnergyMeterReadingRequest;
import com.complex.platform.energy.dto.EnergyMeterRequest;
import com.complex.platform.evidence.AuditEventService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * ③ 楼宇运营 S4：能耗表台账 + 抄表录入。系统据上期表底算本期用量（consumption=本期-上期）。
 * 写 requirePlatformScope + 落审计；功能标志 facility.energy.enabled 默认关。单构造器（无多构造坑）。
 * 能耗费起账（用量×单价→AR）留 S4b/二期。
 */
@Service
public class EnergyService {

    private static final Set<String> METER_TYPES = Set.of("electricity", "water", "gas");

    @Value("${facility.energy.enabled:false}")
    boolean facilityEnergyEnabled;

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
        auditEventService.recordSuccess("facility.energy.meter_viewed", "facility.energy.manage", "energy_meters",
                "all", null, null, Map.of("count", items.size()));
        return new PageResponse<>(items, 1, 500, items.size());
    }

    public EnergyMeterRecord createMeter(EnergyMeterRequest request) {
        authorizationService.requirePlatformScope();
        requireEnabled();
        EnergyMeterRequest actual = requireRequest(request);
        String type = requireEnum(actual.meterType(), METER_TYPES, "meter_type");
        Instant now = Instant.now();
        EnergyMeterRecord saved = repository.saveMeter(new EnergyMeterRecord(
                "emtr_" + UUID.randomUUID(), requireText(actual.name(), "name"), type, trimToNull(actual.spaceId()),
                actual.unit() == null || actual.unit().isBlank() ? "kWh" : actual.unit().trim(), "active",
                null, trimToNull(actual.merchantId()), now, now));
        auditEventService.recordSuccess("facility.energy.meter_created", "facility.energy.manage", "energy_meters",
                saved.id(), null, actual.idempotencyKey(), Map.of("meter_type", type));
        return saved;
    }

    public PageResponse<EnergyMeterReadingRecord> listReadings() {
        List<EnergyMeterReadingRecord> items = repository.listReadings();
        auditEventService.recordSuccess("facility.energy.reading_viewed", "facility.energy.manage", "energy_meter_readings",
                "all", null, null, Map.of("count", items.size()));
        return new PageResponse<>(items, 1, 500, items.size());
    }

    public EnergyMeterReadingRecord recordReading(String meterId, EnergyMeterReadingRequest request) {
        authorizationService.requirePlatformScope();
        requireEnabled();
        String id = requireText(meterId, "meter_id");
        EnergyMeterReadingRequest actual = requireRequest(request);
        String period = requireText(actual.period(), "period");
        BigDecimal value = actual.readingValue();
        if (value == null || value.signum() < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "reading_value must be >= 0");
        }
        EnergyMeterRecord meter = repository.findMeterById(id)
                .orElseThrow(() -> ApiException.notFound("ENERGY_METER_NOT_FOUND", "energy meter not found"));
        if (repository.findReading(id, period).isPresent()) {
            throw ApiException.conflict("ENERGY_READING_DUPLICATE_PERIOD", "reading for this period already recorded");
        }
        BigDecimal consumption = null;
        Optional<BigDecimal> prev = repository.findLatestReadingValueBefore(id, period);
        if (prev.isPresent()) {
            if (value.compareTo(prev.get()) < 0) {
                throw ApiException.badRequest("ENERGY_READING_NOT_MONOTONIC",
                        "reading_value must be >= previous period reading");
            }
            consumption = value.subtract(prev.get());
        }
        Instant now = Instant.now();
        EnergyMeterReadingRecord saved = repository.saveReading(new EnergyMeterReadingRecord(
                "erdg_" + UUID.randomUUID(), id, period, value, consumption, trimToNull(actual.recordedBy()),
                meter.projectId(), meter.merchantId(), now));
        auditEventService.recordSuccess("facility.energy.reading_recorded", "facility.energy.manage",
                "energy_meter_readings", saved.id(), null, actual.idempotencyKey(),
                Map.of("period", period, "consumption", String.valueOf(consumption)));
        return saved;
    }

    private void requireEnabled() {
        if (!facilityEnergyEnabled) {
            throw ApiException.conflict("FACILITY_ENERGY_DISABLED", "facility energy module is disabled");
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
