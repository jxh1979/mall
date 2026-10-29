package com.complex.platform.parking;

// 文件版本：V0.03

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HP-P01 停车场后台（BSM「停车场」L1425–L1452）：停车业务参数（L1434～L1437，按项目、值与上下限同配、不设出厂值）、
 * 在场与出场记录、月租车牌（L1437「商户月租车位按合同约定」，L1428「月租车位在本平台后台配置并下发给停车系统」）、
 * 员工车辆（L1437「员工车辆由所属主体登记车牌后，按后台配置免费或折扣」）。
 */
@Service
public class ParkingService {

    static final String CONFIG = "parking.config.manage";
    static final String VIEW = "parking.ops.view";
    static final String VEHICLE = "parking.vehicle.manage";
    /** 七项业务参数（各另有 _min/_max 上下限）；staff_discount_bp 按万分比减免，10000＝免费。 */
    static final List<String> PARAMS = List.of("free_minutes", "hourly_rate_cent", "daily_cap_cent", "exit_grace_minutes",
            "credit_threshold_cent", "credit_minutes", "staff_discount_bp");
    private static final Map<String, String> PARAM_LABELS = Map.of("free_minutes", "免费时长（分钟）",
            "hourly_rate_cent", "超出后每小时费率（分）", "daily_cap_cent", "每日封顶（分）", "exit_grace_minutes", "离场宽限时限（分钟）",
            "credit_threshold_cent", "消费抵免门槛（分）", "credit_minutes", "抵免时长（分钟）", "staff_discount_bp", "员工车辆优惠（万分比）");

    /** 本项目生效参数（停车业务计算与下发只读此值，不回落任何默认值）。 */
    public record Policy(String projectId, long freeMinutes, long hourlyRateCent, long dailyCapCent, long exitGraceMinutes,
            long creditThresholdCent, long creditMinutes, long staffDiscountBp) {}

    public record Vehicle(String id, String projectId, String contractId, String userId, String ownerMerchantId,
            String plateNo, String status, String registeredBy, Instant registeredAt, Instant revokedAt) {}

    private final JdbcTemplate jdbc;
    private final AuthorizationService auth;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;

    public ParkingService(JdbcTemplate jdbc, AuthorizationService auth, IdempotencyService idempotency, AuditEventService audit) {
        this.jdbc = jdbc;
        this.auth = auth;
        this.idempotency = idempotency;
        this.audit = audit;
    }

    // ============ 停车业务参数 ============

    public Map<String, Object> getPolicy(String projectId) {
        String project = reachableProject(projectId, VIEW);
        return jdbc.queryForList("SELECT * FROM parking_policies WHERE project_id=?", project).stream().findFirst()
                .map(ParkingService::policyView)
                .orElseThrow(() -> notConfigured(project));
    }

    /**
     * 七项参数及各自上下限须一次给齐（缺任一 400 PARKING_POLICY_BOUNDS_REQUIRED），且 0 ≤ 下限 ≤ 值 ≤ 上限、员工优惠上限不超过 10000
     * （否则 400 PARKING_POLICY_OUT_OF_BOUNDS）；只作用于该项目。
     */
    @Transactional
    public Map<String, Object> savePolicy(String projectId, Map<String, Object> request) {
        Map<String, Object> body = request == null ? Map.of() : request;
        String key = body.get("idempotency_key") instanceof String s ? s : null;
        try {
            String project = reachableProject(projectId, CONFIG);
            Map<String, Long> values = new LinkedHashMap<>();
            for (String param : PARAMS) {
                for (String column : List.of(param, param + "_min", param + "_max")) {
                    Object raw = body.get(column);
                    if (!(raw instanceof Number n) || n.doubleValue() != Math.rint(n.doubleValue())) {
                        throw ApiException.badRequest("PARKING_POLICY_BOUNDS_REQUIRED", "停车业务参数须同时填写值及上下限："
                                + PARAM_LABELS.get(param) + (column.endsWith("_min") ? "下限" : column.endsWith("_max") ? "上限" : ""));
                    }
                    values.put(column, n.longValue());
                }
                long value = values.get(param);
                long min = values.get(param + "_min");
                long max = values.get(param + "_max");
                if (min < 0 || value < min || value > max || ("staff_discount_bp".equals(param) && max > 10000)) {
                    throw ApiException.badRequest("PARKING_POLICY_OUT_OF_BOUNDS", "停车业务参数须满足 0 ≤ 下限 ≤ 值 ≤ 上限（员工优惠上限不超过 10000）：" + PARAM_LABELS.get(param));
                }
            }
            if (jdbc.queryForObject("SELECT count(*) FROM projects WHERE id=?", Integer.class, project) == 0) {
                throw ApiException.notFound("NOT_FOUND", "综合体项目不存在");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> saved = idempotency.execute("parkingPolicySave:" + project, key, body, () -> {
                String columns = String.join(",", values.keySet());
                String updates = String.join(",", values.keySet().stream().map(c -> c + "=EXCLUDED." + c).toList());
                Object[] args = new Object[values.size() + 2];
                args[0] = project;
                int i = 1;
                for (Long v : values.values()) {
                    args[i++] = v;
                }
                args[i] = actor().id();
                jdbc.update("INSERT INTO parking_policies (project_id," + columns + ",updated_by,updated_at) VALUES (?"
                        + ",?".repeat(values.size()) + ",?,now()) ON CONFLICT (project_id) DO UPDATE SET " + updates
                        + ",updated_by=EXCLUDED.updated_by,updated_at=now()", args);
                Map<String, Object> view = policyView(jdbc.queryForMap("SELECT * FROM parking_policies WHERE project_id=?", project));
                audit.recordSuccess("parking.policy_saved", CONFIG, "parking_policy", project, null, key, new LinkedHashMap<>(values));
                return view;
            }, Map.class);
            return saved;
        } catch (RuntimeException error) {
            recordFailure("parking.policy_save_failed", CONFIG, "parking_policy", projectId, key, error);
            throw error;
        }
    }

    /** 停车业务取本项目参数：未配置一律 409 PARKING_POLICY_NOT_CONFIGURED，不回落默认值（不设出厂值，第十八批裁决第 7 条）。 */
    public Policy requirePolicy(String projectId) {
        return jdbc.query("SELECT * FROM parking_policies WHERE project_id=?", (rs, n) -> new Policy(rs.getString("project_id"),
                        rs.getLong("free_minutes"), rs.getLong("hourly_rate_cent"), rs.getLong("daily_cap_cent"),
                        rs.getLong("exit_grace_minutes"), rs.getLong("credit_threshold_cent"), rs.getLong("credit_minutes"),
                        rs.getLong("staff_discount_bp")), projectId)
                .stream().findFirst().orElseThrow(() -> notConfigured(projectId));
    }

    // ============ 在场与记录 ============

    /** 在场车辆与出场记录（含平台已收停车费）及最近一次车位占用回传。 */
    public Map<String, Object> overview(String projectId, String status) {
        String project = reachableProject(projectId, VIEW);
        String filter = trimToNull(status);
        List<Map<String, Object>> sessions = jdbc.queryForList("""
                SELECT s.id, s.vendor_session_ref, s.plate_no, s.temp_code, s.vehicle_kind, s.entered_at, s.exited_at,
                       s.vendor_fee_cent, s.vendor_paid_cent, s.status,
                       COALESCE((SELECT sum(p.amount_cent) FROM parking_payments p WHERE p.session_id=s.id AND p.status='paid'), 0)
                           AS platform_paid_cent
                  FROM parking_sessions s
                 WHERE s.project_id=? AND (?::text IS NULL OR s.status=?)
                 ORDER BY s.entered_at DESC, s.id LIMIT 500
                """, project, filter, filter);
        Map<String, Object> occupancy = jdbc.queryForList("""
                SELECT reported_at, total_spaces, occupied_total, occupied_monthly, occupied_staff
                  FROM parking_occupancy_snapshots WHERE project_id=? ORDER BY reported_at DESC LIMIT 1
                """, project).stream().findFirst().orElse(null);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("project_id", project);
        out.put("occupancy", occupancy);
        out.put("sessions", sessions);
        return out;
    }

    // ============ 月租车牌 ============

    /** 月租车牌只在平台后台配置与查看（BSM L1428）；商户、外包账号即使持 parking.vehicle.manage（登记本商户员工车辆用）也 403。 */
    public PageResponse<Vehicle> listMonthlyVehicles(String projectId) {
        auth.requirePlatformScope();
        String project = reachableProject(projectId, VEHICLE);
        List<Vehicle> rows = jdbc.query("""
                SELECT v.*, c.project_id, NULL AS user_id, NULL AS owner_merchant_id FROM parking_monthly_vehicles v
                  JOIN contracts c ON c.id=v.contract_id WHERE c.project_id=? ORDER BY v.registered_at DESC, v.id
                """, this::vehicle, project);
        return new PageResponse<>(rows, 1, rows.size(), rows.size());
    }

    /** 只能为约定了车位月租（rent_terms.parkingMonthlyCent＞0）的合同登记月租车牌；同合同同车牌只有一条有效。 */
    @Transactional
    public Vehicle registerMonthlyVehicle(String contractId, ParkingRequests.MonthlyVehicle r) {
        String key = r == null ? null : r.idempotencyKey();
        try {
            auth.requirePlatformScope();
            Map<String, Object> contract = jdbc.queryForList("""
                    SELECT project_id, COALESCE((rent_terms->>'parkingMonthlyCent')::bigint, 0) AS parking_monthly_cent
                      FROM contracts WHERE id=?""", requireText(contractId, "contract_id")).stream().findFirst()
                    .orElseThrow(() -> ApiException.notFound("CONTRACT_NOT_FOUND", "合同不存在"));
            reachableProject((String) contract.get("project_id"), VEHICLE);
            if (r == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请填写车牌");
            }
            return idempotency.execute("parkingMonthlyVehicle:" + contractId, key, r, () -> {
                if (((Number) contract.get("parking_monthly_cent")).longValue() <= 0) {
                    throw ApiException.conflict("PARKING_MONTHLY_NOT_CONTRACTED", "该合同未约定车位月租");
                }
                String plate = plate(r.plateNo());
                if (jdbc.queryForObject("SELECT count(*) FROM parking_monthly_vehicles WHERE contract_id=? AND plate_no=? AND status='active'",
                        Integer.class, contractId, plate) > 0) {
                    throw plateTaken();
                }
                String id = "pmv_" + UUID.randomUUID();
                jdbc.update("INSERT INTO parking_monthly_vehicles (id, contract_id, plate_no, registered_by) VALUES (?,?,?,?)",
                        id, contractId, plate, actor().id());
                audit.recordSuccess("parking.monthly_vehicle_registered", VEHICLE, "parking_monthly_vehicle", id, null, key,
                        Map.of("contract_id", contractId, "plate_no", plate));
                return monthly(id);
            }, Vehicle.class);
        } catch (RuntimeException error) {
            recordFailure("parking.monthly_vehicle_register_failed", VEHICLE, "parking_monthly_vehicle", contractId, key, error);
            throw error;
        }
    }

    @Transactional
    public Vehicle revokeMonthlyVehicle(String id, ParkingRequests.Revoke r) {
        String key = r == null ? null : r.idempotencyKey();
        try {
            auth.requirePlatformScope();
            Vehicle v = monthly(requireText(id, "id"));
            reachableProject(v.projectId(), VEHICLE);
            return idempotency.execute("parkingMonthlyVehicleRevoke:" + id, key, r, () -> {
                jdbc.update("UPDATE parking_monthly_vehicles SET status='revoked', revoked_by=?, revoked_at=now() WHERE id=? AND status='active'",
                        actor().id(), id);
                audit.recordSuccess("parking.monthly_vehicle_revoked", VEHICLE, "parking_monthly_vehicle", id, null, key, Map.of());
                return monthly(id);
            }, Vehicle.class);
        } catch (RuntimeException error) {
            recordFailure("parking.monthly_vehicle_revoke_failed", VEHICLE, "parking_monthly_vehicle", id, key, error);
            throw error;
        }
    }

    // ============ 员工车辆 ============

    /**
     * 商户账号只看本商户员工车辆。第十八批 E 段 20-R4 扫尾（治理地图第 659 行「一个商户在两个或更多项目中有经营的，看到的是每一个项目内他经营的商户」）：
     * 商户账号（非平台管理员）只按商户，看得到各经营项目里本商户的员工车辆，不按 project_id 限项目；登记、撤销仍只到可触及项目。
     */
    public PageResponse<Vehicle> listStaffVehicles(String projectId) {
        String merchant = auth.currentMerchantIdQuietly().orElse(null);
        String project = null;
        if (merchant == null) {
            project = reachableProject(projectId, VEHICLE);
        } else {
            auth.requirePermission(VEHICLE);
        }
        List<Vehicle> rows = jdbc.query("""
                SELECT v.*, NULL AS contract_id FROM parking_staff_vehicles v
                 WHERE (?::text IS NULL OR v.project_id=?) AND (?::text IS NULL OR v.owner_merchant_id=?) ORDER BY v.registered_at DESC, v.id
                """, this::vehicle, project, project, merchant, merchant);
        return new PageResponse<>(rows, 1, rows.size(), rows.size());
    }

    /**
     * 所属主体登记：商户账号只能登记本商户员工；非商户账号只能登记按 parking.vehicle.manage 可触及的（含可跨项目角色勾选的项目，
     * 第十八批 G 段 82b-R5）、不属任何商户的员工，平台管理员不受限。
     * 员工账号须在职（active）；同项目同车牌只有一条有效。
     */
    @Transactional
    public Vehicle registerStaffVehicle(ParkingRequests.StaffVehicle r) {
        String key = r == null ? null : r.idempotencyKey();
        try {
            if (r == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请填写员工与车牌");
            }
            String project = reachableProject(requireText(r.projectId(), "project_id"), VEHICLE);
            Map<String, Object> staff = jdbc.queryForList("SELECT id, username, account_type, organization_id, merchant_id, status FROM access_users WHERE id=?",
                    requireText(r.userId(), "user_id")).stream().findFirst()
                    .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "员工账号不存在"));
            AccessActor actor = actor();
            String merchant = auth.currentMerchantIdQuietly().orElse(null);
            String staffMerchant = (String) staff.get("merchant_id");
            boolean owned = merchant != null ? merchant.equals(staffMerchant)
                    : auth.currentProjectScopeQuietly().isEmpty()
                            || (staffMerchant == null && auth.canReachUser(actor, new AccessUser((String) staff.get("id"),
                                    (String) staff.get("username"), (String) staff.get("account_type"),
                                    (String) staff.get("organization_id"), null, (String) staff.get("status"), null), VEHICLE));
            if (!owned) {
                throw ApiException.forbidden("PARKING_STAFF_NOT_OWNED", "只能登记本主体的员工车辆");
            }
            if (!"active".equals(staff.get("status"))) {
                throw ApiException.conflict("PARKING_STAFF_INACTIVE", "员工账号不在职，不能登记车辆");
            }
            return idempotency.execute("parkingStaffVehicle:" + project, key, r, () -> {
                String plate = plate(r.plateNo());
                if (jdbc.queryForObject("SELECT count(*) FROM parking_staff_vehicles WHERE project_id=? AND plate_no=? AND status='active'",
                        Integer.class, project, plate) > 0) {
                    throw plateTaken();
                }
                String id = "psv_" + UUID.randomUUID();
                jdbc.update("""
                        INSERT INTO parking_staff_vehicles (id, project_id, user_id, owner_merchant_id, plate_no, registered_by)
                        VALUES (?,?,?,?,?,?)""", id, project, r.userId().trim(), staffMerchant, plate, actor.id());
                audit.recordSuccess("parking.staff_vehicle_registered", VEHICLE, "parking_staff_vehicle", id, null, key,
                        Map.of("user_id", r.userId().trim(), "plate_no", plate));
                return staffVehicle(id);
            }, Vehicle.class);
        } catch (RuntimeException error) {
            recordFailure("parking.staff_vehicle_register_failed", VEHICLE, "parking_staff_vehicle",
                    r == null ? null : r.userId(), key, error);
            throw error;
        }
    }

    @Transactional
    public Vehicle revokeStaffVehicle(String id, ParkingRequests.Revoke r) {
        String key = r == null ? null : r.idempotencyKey();
        try {
            Vehicle v = staffVehicle(requireText(id, "id"));
            reachableProject(v.projectId(), VEHICLE);
            String merchant = auth.currentMerchantIdQuietly().orElse(null);
            if (merchant != null && !merchant.equals(v.ownerMerchantId())) {
                throw ApiException.forbidden("PARKING_STAFF_NOT_OWNED", "只能撤销本主体的员工车辆");
            }
            return idempotency.execute("parkingStaffVehicleRevoke:" + id, key, r, () -> {
                jdbc.update("UPDATE parking_staff_vehicles SET status='revoked', revoked_by=?, revoked_at=now() WHERE id=? AND status='active'",
                        actor().id(), id);
                audit.recordSuccess("parking.staff_vehicle_revoked", VEHICLE, "parking_staff_vehicle", id, null, key, Map.of());
                return staffVehicle(id);
            }, Vehicle.class);
        } catch (RuntimeException error) {
            recordFailure("parking.staff_vehicle_revoke_failed", VEHICLE, "parking_staff_vehicle", id, key, error);
            throw error;
        }
    }

    // ============ 内部 ============

    /** 项目：未指定取本人项目（平台管理员须指定）；须在该权限的可达范围内（见 {@link AuthorizationService#requireReachableProject}）。 */
    public String reachableProject(String requested, String permission) {
        return auth.requireReachableProject(requested, permission, "停车场不在授权项目范围内");
    }

    static String plate(String raw) {
        String plate = requireText(raw, "plate_no").replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (plate.length() > 16) {
            throw ApiException.badRequest("VALIDATION_FAILED", "车牌号过长");
        }
        return plate;
    }

    private Vehicle monthly(String id) {
        return jdbc.query("""
                SELECT v.*, c.project_id, NULL AS user_id, NULL AS owner_merchant_id FROM parking_monthly_vehicles v
                  JOIN contracts c ON c.id=v.contract_id WHERE v.id=?""", this::vehicle, id).stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("PARKING_VEHICLE_NOT_FOUND", "车辆登记不存在"));
    }

    private Vehicle staffVehicle(String id) {
        return jdbc.query("SELECT v.*, NULL AS contract_id FROM parking_staff_vehicles v WHERE v.id=?", this::vehicle, id)
                .stream().findFirst().orElseThrow(() -> ApiException.notFound("PARKING_VEHICLE_NOT_FOUND", "车辆登记不存在"));
    }

    private Vehicle vehicle(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        Timestamp revoked = rs.getTimestamp("revoked_at");
        return new Vehicle(rs.getString("id"), rs.getString("project_id"), rs.getString("contract_id"), rs.getString("user_id"),
                rs.getString("owner_merchant_id"), rs.getString("plate_no"), rs.getString("status"), rs.getString("registered_by"),
                rs.getTimestamp("registered_at").toInstant(), revoked == null ? null : revoked.toInstant());
    }

    private static Map<String, Object> policyView(Map<String, Object> row) {
        Map<String, Object> out = new LinkedHashMap<>(row);
        if (out.get("updated_at") instanceof Timestamp t) {
            out.put("updated_at", t.toInstant().toString());
        }
        return out;
    }

    private static ApiException notConfigured(String projectId) {
        return ApiException.conflict("PARKING_POLICY_NOT_CONFIGURED", "该项目尚未配置停车业务参数：" + projectId);
    }

    private static ApiException plateTaken() {
        return ApiException.conflict("PARKING_PLATE_ALREADY_REGISTERED", "该车牌已登记");
    }

    private AccessActor actor() {
        return auth.currentActor();
    }

    private void recordFailure(String event, String permission, String type, String id, String key, RuntimeException error) {
        try {
            audit.recordFailure(event, permission, type, id == null ? "unknown" : id, null, key,
                    error instanceof ApiException api ? api.code() : error.getClass().getSimpleName());
        } catch (RuntimeException auditError) {
            error.addSuppressed(auditError);
        }
    }
}
