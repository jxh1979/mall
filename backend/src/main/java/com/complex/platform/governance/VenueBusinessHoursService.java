package com.complex.platform.governance;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.dto.VenueBusinessHoursRequest;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * 场地营业时间配置（D1：V226 建表后一直只有 DB 表、没有配置界面，运营只能改库）。
 *
 * <p><b>为什么这页非有不可</b>：入场资格权益（按场次核销）和当天无限权益都靠这张表划场次——
 * 开业开场、歇业关场、过了场次的票自动作废。表里没配某个场地，那个场地当天所有此类核销直接被拒
 * （VENUE_NOT_IN_SESSION）；配错一小时，客人就在门口进不来。这不是能靠运维改库维持的东西。
 *
 * <p><b>保存即生效</b>：本页不做草稿/发布两态。营业时间改了要立刻影响今晚的场次判定，
 * 多一道发布只会让「改了没生效」变成新的事故来源。库内 is_draft 列在此复用为「停用」：
 * 停用=该日不营业、不参与场次判定，配置行保留可一键恢复（对齐删除=软删除口径，不物理删配置）。
 */
@Service
public class VenueBusinessHoursService {

    private final VenueBusinessHoursRepository repository;
    private final AuditEventService auditEventService;

    VenueBusinessHoursService(VenueBusinessHoursRepository repository, AuditEventService auditEventService) {
        this.repository = repository;
        this.auditEventService = auditEventService;
    }

    public List<VenueBusinessHoursRecord> list() {
        List<VenueBusinessHoursRecord> rows = repository.listAll();
        auditEventService.recordSuccess(
                "venue_business_hours.viewed", "governance.space.view", "venue_business_hours", "all",
                null, null, Map.of("count", rows.size()));
        return rows;
    }

    public VenueBusinessHoursRecord save(VenueBusinessHoursRequest request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        String venueId = trimToNull(request.venueId());
        if (venueId == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "venue_id is required");
        }
        Integer dayOfWeek = request.dayOfWeek();
        if (dayOfWeek == null || dayOfWeek < 0 || dayOfWeek > 6) {
            throw ApiException.badRequest("VALIDATION_FAILED", "day_of_week must be 0(Sunday)..6(Saturday)");
        }
        LocalTime openTime = parseTime(request.openTime(), "open_time");
        LocalTime closeTime = parseTime(request.closeTime(), "close_time");
        // 开歇同刻 = 零长营业窗口 = 这一天谁都进不来，但页面上看着像配好了。宁可拒，不留这种静默坑。
        if (openTime.equals(closeTime)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "open_time must differ from close_time");
        }
        boolean disabled = Boolean.TRUE.equals(request.disabled());
        Instant now = Instant.now();
        VenueBusinessHoursRecord saved = repository.upsert(
                "vbh_" + UUID.randomUUID().toString().replace("-", ""),
                venueId, dayOfWeek, openTime, closeTime, disabled, request.effectiveFrom(), now);
        auditEventService.recordSuccess(
                "venue_business_hours.updated", "governance.space.manage", "venue_business_hours", saved.id(),
                null, null, Map.of(
                        "venue_id", venueId,
                        "day_of_week", dayOfWeek,
                        "open_time", saved.openTime(),
                        "close_time", saved.closeTime(),
                        "disabled", disabled));
        return saved;
    }

    private LocalTime parseTime(String raw, String field) {
        String value = trimToNull(raw);
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required (HH:mm)");
        }
        try {
            return LocalTime.parse(value.length() == 5 ? value : value.substring(0, 5));
        } catch (DateTimeParseException | IndexOutOfBoundsException exception) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " must be HH:mm");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
