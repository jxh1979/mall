package com.complex.platform.bar;

// 文件版本：V0.02

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十四批 OBS-13-03-R3 最晚到店时间前提醒。BSM 第 1596 行「最晚到店时间前提醒（提前量业务参数，按项目可配，出厂默认 30 分钟，
 * 须同时配置上下限）」。参数范式同未核销票到期提醒（{@link com.complex.platform.ticketing.TicketExpiryReminderService}）：项目无自有行即用
 * '__global__' 出厂行（30 分钟、上下限未配），配置时值与上下限同填；项目按订台 bar_id 取 merchants.project_id。
 * 提醒只发仍占着台位的有效订台（未付定金、未占台的自助订台不提醒），每笔订台只发一次，由订台超时作业每轮先调一次。
 */
@Service
public class BarArrivalReminderService {

    static final String VIEW_PERMISSION = "bar.reservation.view";
    static final String MANAGE_PERMISSION = "bar.reservation.manage";
    private static final String GLOBAL = "__global__";
    private static final Logger log = LoggerFactory.getLogger(BarArrivalReminderService.class);

    /** projectId＝生效行所属（项目自有行或 '__global__' 出厂行）。 */
    public record Policy(String projectId, int remindMinutes, Integer minMinutes, Integer maxMinutes) {
    }

    public record PolicyRequest(String idempotencyKey, Integer remindMinutes, Integer minMinutes, Integer maxMinutes, String reason) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final BarRepository repository;
    private final BarReservationNotifier notifier;
    private final AuthorizationService authorizationService;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    public BarArrivalReminderService(NamedParameterJdbcTemplate jdbc, BarRepository repository, BarReservationNotifier notifier,
            AuthorizationService authorizationService, IdempotencyService idempotencyService, AuditEventService auditEventService) {
        this.jdbc = jdbc;
        this.repository = repository;
        this.notifier = notifier;
        this.authorizationService = authorizationService;
        this.idempotencyService = idempotencyService;
        this.auditEventService = auditEventService;
    }

    public Policy getPolicy(String projectId) {
        authorizationService.requirePermission(VIEW_PERMISSION);
        return policy(requireProject(projectId, VIEW_PERMISSION));
    }

    @Transactional
    public Policy savePolicy(String projectId, PolicyRequest request) {
        String key = request == null ? null : request.idempotencyKey();
        try {
            authorizationService.requirePermission(MANAGE_PERMISSION);
            if (authorizationService.currentMerchantIdQuietly().isPresent()) {
                throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "到店前提醒是综合体项目参数，商户账号不能修改");
            }
            String project = requireProject(projectId, MANAGE_PERMISSION);
            Integer minutes = request == null ? null : request.remindMinutes();
            Integer min = request == null ? null : request.minMinutes();
            Integer max = request == null ? null : request.maxMinutes();
            if (minutes == null || min == null || max == null || min < 1 || minutes < min || minutes > max) {
                throw ApiException.badRequest("VALIDATION_FAILED", "到店前提醒分钟数须同时填写参数值及上下限，下限不小于 1，参数值须在上下限内");
            }
            return idempotencyService.execute("saveBarArrivalReminderPolicy:" + project, key, request, () -> {
                Policy before = policy(project);
                jdbc.update("""
                        INSERT INTO bar_arrival_reminder_policies (project_id, remind_minutes, min_minutes, max_minutes, updated_by, updated_at)
                        VALUES (:pid, :minutes, :min, :max, :actor, now())
                        ON CONFLICT (project_id) DO UPDATE SET remind_minutes = EXCLUDED.remind_minutes, min_minutes = EXCLUDED.min_minutes,
                            max_minutes = EXCLUDED.max_minutes, updated_by = EXCLUDED.updated_by, updated_at = now()
                        """, new MapSqlParameterSource().addValue("pid", project).addValue("minutes", minutes).addValue("min", min)
                        .addValue("max", max).addValue("actor", authorizationService.currentActor().id()));
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("before_remind_minutes", before.remindMinutes());
                detail.put("before_min_minutes", before.minMinutes() == null ? "" : before.minMinutes());
                detail.put("before_max_minutes", before.maxMinutes() == null ? "" : before.maxMinutes());
                detail.put("remind_minutes", minutes);
                detail.put("min_minutes", min);
                detail.put("max_minutes", max);
                detail.put("reason", request.reason() == null ? "" : request.reason());
                auditEventService.recordSuccess("bar.arrival_reminder_policy_updated", MANAGE_PERMISSION, "bar_arrival_reminder_policy",
                        project, null, key, detail);
                return new Policy(project, minutes, min, max);
            }, Policy.class);
        } catch (RuntimeException failure) {
            auditEventService.recordFailure("bar.arrival_reminder_policy_update_failed", MANAGE_PERMISSION, "bar_arrival_reminder_policy",
                    projectId, null, key, failure instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw failure;
        }
    }

    /** 仍占着台位、最晚到店时间落在 (现在, 现在＋该订台项目的提前分钟数] 内、尚未提醒过的订台各发一次提醒。@return 本轮发出的笔数。 */
    public int remindApproaching(Instant now) {
        int sent = 0;
        for (String reservationId : jdbc.queryForList("""
                SELECT r.id FROM bar_reservations r
                  LEFT JOIN merchants m ON m.id = r.bar_id
                 CROSS JOIN LATERAL (SELECT p.remind_minutes FROM bar_arrival_reminder_policies p
                                      WHERE p.project_id = m.project_id OR p.project_id = '__global__'
                                      ORDER BY (p.project_id = '__global__') LIMIT 1) p
                 WHERE r.status = 'reserved' AND r.hold_status = 'held' AND r.delete_status <> 'approved'
                   AND r.latest_arrival_at > :now
                   AND r.latest_arrival_at <= CAST(:now AS TIMESTAMPTZ) + make_interval(mins => p.remind_minutes)
                   AND NOT EXISTS (SELECT 1 FROM notification_send_log l WHERE l.request_id = 'barres:arrival_reminder:' || r.id)
                 ORDER BY r.latest_arrival_at, r.id
                """, Map.of("now", Timestamp.from(now)), String.class)) {
            try {
                BarReservationRecord reservation = repository.findReservationById(reservationId).orElse(null);
                if (reservation != null) {
                    notifier.notify(reservation, "arrival_reminder");
                    sent++;
                }
            } catch (RuntimeException failure) {
                log.warn("bar arrival reminder {} not sent this round: {}", reservationId, failure.getClass().getSimpleName());
            }
        }
        return sent;
    }

    private Policy policy(String projectId) {
        return jdbc.query("""
                SELECT project_id, remind_minutes, min_minutes, max_minutes FROM bar_arrival_reminder_policies
                 WHERE project_id = :pid OR project_id = '__global__'
                 ORDER BY (project_id = '__global__') LIMIT 1
                """, new MapSqlParameterSource("pid", projectId == null ? GLOBAL : projectId),
                (rs, n) -> new Policy(rs.getString("project_id"), rs.getInt("remind_minutes"),
                        rs.getObject("min_minutes", Integer.class), rs.getObject("max_minutes", Integer.class)))
                .stream().findFirst()
                .orElseThrow(() -> ApiException.conflict("POLICY_NOT_CONFIGURED", "到店前提醒参数未配置出厂行"));
    }

    /** 项目须在入口权限码可触及的范围内（含可跨项目角色勾选的项目），否则 403 CROSS_PROJECT_FORBIDDEN（第十八批 G 段 82b-R4）。 */
    private String requireProject(String projectId, String permission) {
        String project = projectId == null || projectId.isBlank() ? null : projectId.trim();
        if (project == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "必须指定综合体项目");
        }
        return authorizationService.requireReachableProject(project, permission, AuthorizationService.PROJECT_OUT_OF_REACH);
    }
}
