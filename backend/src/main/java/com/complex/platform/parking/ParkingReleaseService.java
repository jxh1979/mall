package com.complex.platform.parking;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;
import static com.complex.platform.common.Strings.trimToNull;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.RiskAlertService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HP-P01 人工放行（BSM L1438「识别失败、道闸故障、客人对缴费有争议时，由当班保安人工核对后放行，人工放行必须记录原因、操作人、车牌和时间，
 * 进留痕与异常；免费放行和减免须经物业主管审批」；L1448「人工放行、减免、滞留与逃费记录留痕并进异常队列」）。
 */
@Service
public class ParkingReleaseService {

    static final String RECORD = "parking.release.record";
    static final String ACTION = "parking.release.waive";
    static final String OBJECT = "parking_manual_release";
    private static final Set<String> REASONS = Set.of("recognition_failed", "gate_fault", "fee_dispute");
    private static final Map<String, String> REASON_LABELS = Map.of("recognition_failed", "识别失败", "gate_fault", "道闸故障",
            "fee_dispute", "缴费争议");

    public record Release(String id, String projectId, String sessionId, String plateNo, String releaseReason, String reason,
            String operatorId, String operatorShiftId, Instant releasedAt, String waiveType, Long reductionCent, String reviewId,
            String status, String riskAlertId) {}

    private final JdbcTemplate jdbc;
    private final AuthorizationService auth;
    private final IdempotencyService idempotency;
    private final AuditEventService audit;
    private final ParkingService parking;
    private final ParkingVendorService vendor;
    private final RiskAlertService alerts;
    private final SupervisorReviewService reviews;
    private final JdbcStaffRepository staff;
    private final BusinessDayResolver businessDays;

    public ParkingReleaseService(JdbcTemplate jdbc, AuthorizationService auth, IdempotencyService idempotency,
            AuditEventService audit, ParkingService parking, ParkingVendorService vendor, RiskAlertService alerts,
            SupervisorReviewService reviews, JdbcStaffRepository staff, BusinessDayResolver businessDays) {
        this.jdbc = jdbc;
        this.auth = auth;
        this.idempotency = idempotency;
        this.audit = audit;
        this.parking = parking;
        this.vendor = vendor;
        this.alerts = alerts;
        this.reviews = reviews;
        this.staff = staff;
        this.businessDays = businessDays;
    }

    /**
     * 当班保安登记：原因、车牌必填，操作人取登录人、时间取服务器时刻，须此刻当班（排班或顶班）；同事务进统一异常队列。
     * 免费放行／减免进统一审批（最终审批人为物业主管，须经角色持有 parking.release.approve），待批期间不判逃费。
     */
    @Transactional
    public Release record(ParkingRequests.ManualRelease r) {
        String key = r == null ? null : r.idempotencyKey();
        try {
            if (r == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "请填写人工放行登记");
            }
            String project = parking.reachableProject(requireText(r.projectId(), "project_id"), RECORD);
            return idempotency.execute("parkingManualRelease:" + project, key, r, () -> {
                String plate = ParkingService.plate(r.plateNo());
                String releaseReason = requireText(r.releaseReason(), "release_reason");
                if (!REASONS.contains(releaseReason)) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "release_reason is invalid");
                }
                String reason = requireText(r.reason(), "reason");
                String waive = trimToNull(r.waiveType()) == null ? "none" : r.waiveType().trim();
                if (!Set.of("none", "free", "reduction").contains(waive)) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "waive_type is invalid");
                }
                Long reduction = "reduction".equals(waive) ? r.reductionCent() : null;
                if ("reduction".equals(waive) && (reduction == null || reduction <= 0)) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "reduction_cent is invalid");
                }
                AccessActor actor = auth.currentActor();
                Instant now = Instant.now();
                String shift = onDutyShift(actor.id(), now).orElseThrow(() ->
                        ApiException.conflict("PARKING_GUARD_NOT_ON_DUTY", "人工放行须由当班保安办理"));
                String session = session(project, trimToNull(r.parkingSessionId()), plate, now);
                String id = "pmr_" + UUID.randomUUID();
                String summary = "停车人工放行：车牌 " + plate + "，原因 " + REASON_LABELS.get(releaseReason) + "（" + reason + "），操作人 "
                        + actor.id() + "，时间 " + now + switch (waive) {
                            case "free" -> "，申请免费放行";
                            case "reduction" -> "，申请减免 " + reduction + " 分";
                            default -> "";
                        };
                String review = null;
                String status = "released";
                if (!"none".equals(waive)) {
                    review = reviews.evaluate(new SupervisorReviewEvaluateRequest(key + ":review", reason, List.of(), ACTION, OBJECT,
                            id, reduction, summary, requireText(r.finalApproverId(), "final_approver_id"), List.of())).reviewId();
                    status = "pending_approval";
                }
                String alert = alerts.recordSystemAlert("alert_" + id, "parking", id, "parking.manual_release", "normal", summary,
                        project, actor.id()).alertId();
                jdbc.update("""
                        INSERT INTO parking_manual_releases (id, project_id, session_id, plate_no, release_reason, reason, operator_id,
                            operator_shift_id, released_at, waive_type, reduction_cent, review_id, status, risk_alert_id)
                        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)""", id, project, session, plate, releaseReason, reason, actor.id(), shift,
                        Timestamp.from(now), waive, reduction, review, status, alert);
                audit.recordSuccess("parking.manual_release_recorded", RECORD, OBJECT, id, null, key,
                        Map.of("plate_no", plate, "release_reason", releaseReason, "waive_type", waive, "risk_alert_id", alert));
                return find(id);
            }, Release.class);
        } catch (RuntimeException error) {
            try {
                audit.recordFailure("parking.manual_release_record_failed", RECORD, OBJECT, r == null || r.plateNo() == null
                        ? "unknown" : r.plateNo(), null, key, error instanceof ApiException api ? api.code() : error.getClass().getSimpleName());
            } catch (RuntimeException auditError) {
                error.addSuppressed(auditError);
            }
            throw error;
        }
    }

    /** 审批结论回写：批准／驳回均留痕；驳回后该次出场仍欠费的，补进逃费异常。 */
    @EventListener
    @Transactional
    public void decided(SupervisorReviewDecisionRecord d) {
        if (!ACTION.equals(d.actionType()) || !OBJECT.equals(d.objectType())
                || !List.of("approved", "rejected").contains(d.reviewStatus())) {
            return;
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, session_id FROM parking_manual_releases WHERE review_id=? AND status='pending_approval' FOR UPDATE", d.reviewId());
        if (rows.isEmpty()) {
            return;
        }
        String id = (String) rows.getFirst().get("id");
        jdbc.update("UPDATE parking_manual_releases SET status=? WHERE id=?", d.reviewStatus(), id);
        audit.recordSuccess("parking.manual_release_" + d.reviewStatus(), "parking.release.approve", OBJECT, id, null, null,
                Map.of("review_id", d.reviewId()));
        if ("rejected".equals(d.reviewStatus()) && rows.getFirst().get("session_id") != null) {
            vendor.checkFeeEvasion((String) rows.getFirst().get("session_id"));
        }
    }

    public PageResponse<Release> list(String projectId) {
        String project = parking.reachableProject(projectId, RECORD);
        List<Release> rows = jdbc.query("SELECT * FROM parking_manual_releases WHERE project_id=? ORDER BY released_at DESC, id LIMIT 500",
                this::map, project);
        return new PageResponse<>(rows, 1, rows.size(), rows.size());
    }

    /** 此刻实际当班：本人排班（含顶班生成的班次）此刻在进行，且这一段没有再被别人顶走。返回当班班次。 */
    private Optional<String> onDutyShift(String userId, Instant now) {
        Timestamp at = Timestamp.from(now);
        for (String shift : jdbc.queryForList("SELECT id FROM staff_shift_schedules WHERE user_id=? AND start_at<=? AND end_at>?",
                String.class, userId, at, at)) {
            for (JdbcStaffRepository.ShiftHolder holder : staff.findEffectiveShiftHolders(shift, now)) {
                if (userId.equals(holder.userId())) {
                    return Optional.of(holder.shiftId());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 关联停车记录：指定的须属本项目；未指定按车牌取此刻在场的那一条，不在场则取本营业日内最近离场的那一条
     * （保安先在停车系统开闸、出场回写先到再来登记）；都没有则不关联。
     */
    private String session(String project, String requested, String plate, Instant now) {
        if (requested != null) {
            return jdbc.query("SELECT id FROM parking_sessions WHERE id=? AND project_id=?", (rs, n) -> rs.getString(1), requested, project)
                    .stream().findFirst().orElseThrow(() -> ApiException.notFound("PARKING_SESSION_NOT_FOUND", "停车记录不存在"));
        }
        return jdbc.query("""
                SELECT id FROM parking_sessions WHERE project_id=? AND plate_no=?
                   AND (status='in_lot' OR (status='exited' AND exited_at>=? AND exited_at<=?))
                 ORDER BY status='in_lot' DESC, COALESCE(exited_at, entered_at) DESC LIMIT 1""",
                (rs, n) -> rs.getString(1), project, plate, Timestamp.from(businessDays.startInstant(businessDays.businessDate(now))),
                Timestamp.from(now)).stream().findFirst().orElse(null);
    }

    private Release find(String id) {
        return jdbc.query("SELECT * FROM parking_manual_releases WHERE id=?", this::map, id).getFirst();
    }

    private Release map(java.sql.ResultSet rs, int n) throws java.sql.SQLException {
        Long reduction = rs.getObject("reduction_cent") == null ? null : rs.getLong("reduction_cent");
        return new Release(rs.getString("id"), rs.getString("project_id"), rs.getString("session_id"), rs.getString("plate_no"),
                rs.getString("release_reason"), rs.getString("reason"), rs.getString("operator_id"), rs.getString("operator_shift_id"),
                rs.getTimestamp("released_at").toInstant(), rs.getString("waive_type"), reduction,
                rs.getString("review_id"), rs.getString("status"), rs.getString("risk_alert_id"));
    }
}
