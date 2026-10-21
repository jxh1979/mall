package com.complex.platform.parking;

// 文件版本：V0.01

import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.RiskAlertService;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HP-P01 第三方停车系统接入（BSM L1428「道闸、车牌识别和计费由第三方停车系统执行，平台拿回入出场记录、缴费结果和车位占用；
 * 停车费率、消费抵免规则和月租车位在本平台后台配置并下发给停车系统」；L1444；L1511「第三方可以在身份核验后读取或回写平台数据」）。
 * 回写经开放平台回调（验签、防重放、幂等、适配器字段映射已在 IntegrationService），这里按映射后的标准字段落停车业务；
 * 超长滞留、逃费进统一异常队列（L1438／L1448），缴费回写与平台收款对不上进第三方对账差异（L1445）。
 */
@Service
public class ParkingVendorService {

    static final Set<String> EVENTS = Set.of("vehicle_entered", "vehicle_exited", "payment_reported", "occupancy_reported",
            "anomaly_reported");
    private static final Set<String> KINDS = Set.of("temporary", "monthly", "staff");
    private static final String SYSTEM = "system:parking";

    private final JdbcTemplate jdbc;
    private final ParkingService parking;
    private final RiskAlertService alerts;

    public ParkingVendorService(JdbcTemplate jdbc, ParkingService parking, RiskAlertService alerts) {
        this.jdbc = jdbc;
        this.parking = parking;
        this.alerts = alerts;
    }

    /** 停车系统回写（字段已按适配器映射成标准键）：session_ref／plate_no／temp_code／entered_at／exited_at／fee_cent／paid_cent 等。 */
    @Transactional
    public void ingest(String endpointKey, String provider, String event, Map<String, Object> m) {
        if (!EVENTS.contains(event)) {
            throw ApiException.badRequest("PARKING_EVENT_UNSUPPORTED", "停车系统回写事件不支持：" + event);
        }
        String project = boundProject(endpointKey);
        switch (event) {
            case "vehicle_entered" -> entered(project, m);
            case "vehicle_exited" -> exited(project, provider, m);
            case "payment_reported" -> paymentReported(project, provider, m);
            case "occupancy_reported" -> jdbc.update("""
                    INSERT INTO parking_occupancy_snapshots (project_id, reported_at, total_spaces, occupied_total, occupied_monthly, occupied_staff)
                    VALUES (?,?,?,?,?,?) ON CONFLICT DO NOTHING""", project, ts(instant(m, "reported_at")), number(m, "total_spaces"),
                    number(m, "occupied_total"), orZero(optNumber(m, "occupied_monthly")), orZero(optNumber(m, "occupied_staff")));
            default -> anomaly(project, m);
        }
    }

    /** 下发给停车系统：本项目费率三项、离场宽限、抵免门槛与时长、员工优惠，及有效月租车牌（合同起止日）与在职员工车牌。 */
    public Map<String, Object> config(String endpointKey) {
        String project = boundProject(endpointKey);
        ParkingService.Policy p = parking.requirePolicy(project);
        LocalDate today = LocalDate.now(LocalZone.ID);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("project_id", project);
        out.put("free_minutes", p.freeMinutes());
        out.put("hourly_rate_cent", p.hourlyRateCent());
        out.put("daily_cap_cent", p.dailyCapCent());
        out.put("exit_grace_minutes", p.exitGraceMinutes());
        out.put("credit_threshold_cent", p.creditThresholdCent());
        out.put("credit_minutes", p.creditMinutes());
        out.put("staff_discount_bp", p.staffDiscountBp());
        out.put("monthly_vehicles", jdbc.queryForList("""
                SELECT v.plate_no, c.id AS contract_id, c.effective_date::text AS start_date, c.expiry_date::text AS end_date
                  FROM parking_monthly_vehicles v JOIN contracts c ON c.id = v.contract_id
                 WHERE v.status = 'active' AND c.project_id = ? AND c.lifecycle_status = 'effective'
                   AND c.effective_date <= ? AND (c.expiry_date IS NULL OR c.expiry_date >= ?)
                   AND COALESCE((c.rent_terms->>'parkingMonthlyCent')::bigint, 0) > 0
                 ORDER BY v.plate_no""", project, today, today));
        out.put("staff_vehicles", jdbc.queryForList("""
                SELECT v.plate_no FROM parking_staff_vehicles v JOIN access_users u ON u.id = v.user_id
                 WHERE v.project_id = ? AND v.status = 'active' AND u.status = 'active' ORDER BY v.plate_no""", project));
        return out;
    }

    /** 平台已收的停车费（供道闸放行）：按收款时间顺序，after 为上一页最后一笔的 payment_id；credit_minutes＝该笔所用消费抵免时长（未用为 0）。 */
    public List<Map<String, Object>> payments(String endpointKey, String after) {
        String project = boundProject(endpointKey);
        return jdbc.queryForList("""
                SELECT p.id AS payment_id, s.vendor_session_ref AS session_ref, s.plate_no, s.temp_code, p.amount_cent,
                       p.paid_at, p.grace_until, COALESCE(c.credit_minutes, 0) AS credit_minutes
                  FROM parking_payments p JOIN parking_sessions s ON s.id = p.session_id
                  LEFT JOIN parking_credits c ON c.id = p.credit_id
                 WHERE p.project_id = ? AND p.status = 'paid'
                   AND (?::text IS NULL OR (p.paid_at, p.id) > (SELECT q.paid_at, q.id FROM parking_payments q WHERE q.id = ?))
                 ORDER BY p.paid_at, p.id LIMIT 200""", project, after, after).stream().map(ParkingVendorService::iso).toList();
    }

    /**
     * 逃费判定：出场时停车系统应收 − 停车系统自收 − 平台已收 ＞ 0，且该会话没有待批或已批、足以覆盖差额的免费放行／减免，即进异常队列。
     * 人工放行的免费／减免被驳回时再判一次。
     */
    public void checkFeeEvasion(String sessionId) {
        Map<String, Object> s = jdbc.queryForMap("""
                SELECT s.project_id, s.plate_no, s.temp_code, s.status, COALESCE(s.vendor_fee_cent, 0) AS fee,
                       COALESCE(s.vendor_paid_cent, 0) AS vendor_paid,
                       COALESCE((SELECT sum(p.amount_cent) FROM parking_payments p WHERE p.session_id = s.id AND p.status = 'paid'), 0) AS platform_paid
                  FROM parking_sessions s WHERE s.id = ?""", sessionId);
        long unpaid = ((Number) s.get("fee")).longValue() - ((Number) s.get("vendor_paid")).longValue()
                - ((Number) s.get("platform_paid")).longValue();
        if (!"exited".equals(s.get("status")) || unpaid <= 0) {
            return;
        }
        Integer covered = jdbc.queryForObject("""
                SELECT count(*) FROM parking_manual_releases WHERE session_id = ? AND status IN ('pending_approval', 'approved')
                   AND (waive_type = 'free' OR reduction_cent >= ?)""", Integer.class, sessionId, unpaid);
        if (covered != null && covered > 0) {
            return;
        }
        alerts.recordSystemAlert("alert_pk_fe_" + sessionId, "parking", sessionId, "parking.fee_evasion", "high",
                "停车逃费：" + vehicle(s) + " 出场未缴 " + unpaid + " 分", (String) s.get("project_id"), SYSTEM);
    }

    private void entered(String project, Map<String, Object> m) {
        String plate = m.get("plate_no") == null ? null : ParkingService.plate(String.valueOf(m.get("plate_no")));
        String temp = text(m, "temp_code", false);
        if (plate == null && temp == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "入场记录须带车牌或临时码");
        }
        String kind = m.get("vehicle_kind") == null ? "temporary" : String.valueOf(m.get("vehicle_kind"));
        if (!KINDS.contains(kind)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "车辆类型不支持：" + kind);
        }
        jdbc.update("""
                INSERT INTO parking_sessions (id, project_id, vendor_session_ref, plate_no, temp_code, vehicle_kind, entered_at)
                VALUES (?,?,?,?,?,?,?) ON CONFLICT (project_id, vendor_session_ref) DO NOTHING""",
                "ps_" + UUID.randomUUID(), project, text(m, "session_ref", true), plate, temp, kind, ts(instant(m, "entered_at")));
    }

    private void exited(String project, String provider, Map<String, Object> m) {
        String sessionId = session(project, text(m, "session_ref", true), true);
        int updated = jdbc.update("""
                UPDATE parking_sessions SET status = 'exited', exited_at = ?, vendor_fee_cent = ?, vendor_paid_cent = ?, updated_at = now()
                 WHERE id = ? AND status = 'in_lot'""", ts(instant(m, "exited_at")), optNumber(m, "fee_cent"),
                optNumber(m, "paid_cent"), sessionId);
        if (updated == 0) {
            return;
        }
        checkFeeEvasion(sessionId);
        // 平台已收、停车系统到出场仍没回报的，进对账差异；抵到 0 元的已缴记录没有资金往来，不进停车系统对账。
        jdbc.queryForList("""
                SELECT p.id, p.amount_cent FROM parking_payments p WHERE p.session_id = ? AND p.status = 'paid' AND p.amount_cent > 0
                   AND NOT EXISTS (SELECT 1 FROM parking_vendor_payments v WHERE v.project_id = p.project_id AND v.platform_payment_id = p.id)
                """, sessionId).forEach(p -> difference(project, provider, (String) p.get("id"),
                ((Number) p.get("amount_cent")).longValue(), 0));
    }

    private void paymentReported(String project, String provider, Map<String, Object> m) {
        String ref = text(m, "payment_ref", true);
        long amount = number(m, "amount_cent");
        String sessionRef = text(m, "session_ref", false);
        String platformPayment = text(m, "platform_payment_id", false);
        int inserted = jdbc.update("""
                INSERT INTO parking_vendor_payments (id, project_id, vendor_payment_ref, session_id, amount_cent, channel, paid_at, platform_payment_id)
                VALUES (?,?,?,?,?,?,?,?) ON CONFLICT (project_id, vendor_payment_ref) DO NOTHING""",
                "pvp_" + UUID.randomUUID(), project, ref, sessionRef == null ? null : session(project, sessionRef, false), amount,
                text(m, "channel", false), ts(instant(m, "paid_at")), platformPayment);
        if (inserted == 0) {
            return;
        }
        Long platformAmount = platformPayment == null ? null : jdbc.query(
                "SELECT amount_cent FROM parking_payments WHERE id = ? AND project_id = ? AND status = 'paid'",
                (rs, n) -> rs.getLong(1), platformPayment, project).stream().findFirst().orElse(null);
        if (platformAmount == null || platformAmount != amount) {
            difference(project, provider, ref, platformAmount == null ? 0 : platformAmount, amount);
        }
    }

    private void anomaly(String project, Map<String, Object> m) {
        String kind = text(m, "kind", true);
        if (!Set.of("overstay", "fee_evasion").contains(kind)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "异常类型不支持：" + kind);
        }
        String sessionId = session(project, text(m, "session_ref", true), true);
        Map<String, Object> s = jdbc.queryForMap("SELECT plate_no, temp_code FROM parking_sessions WHERE id = ?", sessionId);
        boolean overstay = "overstay".equals(kind);
        alerts.recordSystemAlert((overstay ? "alert_pk_os_" : "alert_pk_fe_") + sessionId, "parking", sessionId, "parking." + kind,
                overstay ? "suspicious" : "high", (overstay ? "停车超长滞留：" : "停车逃费：") + vehicle(s) + "（停车系统回传）",
                project, SYSTEM);
    }

    /** 停车缴费对不上：按项目、日期归入第三方对账批次（business_type=parking）。 */
    private void difference(String project, String provider, String externalId, long internal, long vendor) {
        LocalDate day = LocalDate.now(LocalZone.ID);
        String batch = "recon_parking_" + project + "_" + day;
        long diff = Math.abs(internal - vendor);
        jdbc.update("""
                INSERT INTO integration_reconciliation_batches (id, provider_code, business_type, period_start, period_end, status, created_at, updated_at)
                VALUES (?,?,'parking',?,?,'difference_found',now(),now()) ON CONFLICT (id) DO NOTHING""", batch, provider, day, day);
        jdbc.update("""
                INSERT INTO integration_reconciliation_differences (id, batch_id, provider_code, business_type, external_business_id,
                    difference_type, internal_amount_cent, provider_amount_cent, difference_amount_cent, status, source_domain,
                    provider_payload_json, created_at, updated_at)
                VALUES (?,?,?,'parking',?,'parking_payment_mismatch',?,?,?,'difference_found','thirdparty',?::jsonb,now(),now())""",
                "recon_diff_pk_" + UUID.randomUUID(), batch, provider, externalId, internal, vendor, diff,
                "{\"project_id\":\"" + project.replace("\"", "") + "\"}");
        jdbc.update("""
                UPDATE integration_reconciliation_batches SET difference_count = difference_count + 1,
                       total_difference_cent = total_difference_cent + ?, updated_at = now() WHERE id = ?""", diff, batch);
    }

    private String boundProject(String endpointKey) {
        return jdbc.query("SELECT project_id FROM parking_vendor_bindings WHERE endpoint_key = ?", (rs, n) -> rs.getString(1), endpointKey)
                .stream().findFirst()
                .orElseThrow(() -> ApiException.forbidden("PARKING_ENDPOINT_NOT_BOUND", "该开放平台接口未绑定停车场项目"));
    }

    private String session(String project, String ref, boolean required) {
        String id = jdbc.query("SELECT id FROM parking_sessions WHERE project_id = ? AND vendor_session_ref = ?",
                (rs, n) -> rs.getString(1), project, ref).stream().findFirst().orElse(null);
        if (id == null && required) {
            throw ApiException.conflict("PARKING_SESSION_NOT_FOUND", "停车记录不存在：" + ref);
        }
        return id;
    }

    private static String vehicle(Map<String, Object> s) {
        return s.get("plate_no") != null ? "车牌 " + s.get("plate_no") : "临时码 " + s.get("temp_code");
    }

    private static String text(Map<String, Object> m, String key, boolean required) {
        Object v = m.get(key);
        String s = v == null ? null : String.valueOf(v).trim();
        if ((s == null || s.isEmpty()) && required) {
            throw ApiException.badRequest("VALIDATION_FAILED", key + " is required");
        }
        return s == null || s.isEmpty() ? null : s;
    }

    private static long number(Map<String, Object> m, String key) {
        Long v = optNumber(m, key);
        if (v == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", key + " is required");
        }
        return v;
    }

    private static Long optNumber(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) {
            return null;
        }
        try {
            long n = v instanceof Number num ? num.longValue() : Long.parseLong(String.valueOf(v).trim());
            if (n < 0) {
                throw new NumberFormatException();
            }
            return n;
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("VALIDATION_FAILED", key + " is invalid");
        }
    }

    private static long orZero(Long v) {
        return v == null ? 0 : v;
    }

    private static Instant instant(Map<String, Object> m, String key) {
        try {
            return OffsetDateTime.parse(text(m, key, true)).toInstant();
        } catch (java.time.format.DateTimeParseException e) {
            throw ApiException.badRequest("VALIDATION_FAILED", key + " is invalid");
        }
    }

    private static Timestamp ts(Instant instant) {
        return Timestamp.from(instant);
    }

    static Map<String, Object> iso(Map<String, Object> row) {
        Map<String, Object> out = new LinkedHashMap<>(row);
        out.replaceAll((k, v) -> v instanceof Timestamp t ? t.toInstant().toString() : v);
        return out;
    }
}
