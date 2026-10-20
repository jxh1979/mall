package com.complex.platform.bar;

// 文件版本：V0.01

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.access.staff.StaffProfileRecord;
import com.complex.platform.common.Strings;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.notification.NotificationService;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 第十批 TK-27 待清台超时提醒（BSM L1058「待清台超过后台配置时限（业务参数，按酒吧可配，出厂默认 15 分钟，须同时配置上下限）
 * 未恢复空闲的，系统提醒当班主管」；操作员裁决 2026-10-20 第 6 条：发给本酒吧、此刻有排班、且持有 bar.table.open_close 的员工；
 * 每台位每次待清台只发一次）。同 SavedBottleNotificationJob 范式：逻辑在可直接调用的 {@link #alertOverdue}，@Scheduled 只做薄包装。
 *
 * <p>收件人＝access_users.merchant_id＝该酒吧、当前时刻落在 staff_shift_schedules 排班区间内、且经
 * {@link AuthorizationService#hasEffectivePermission} 核实持 bar.table.open_close 的员工（手机号取员工档案）。
 * 幂等：发送请求号＝台位 id＋进入待清台时刻（updated_at）＋收件人，已发过的不再发；本轮有新发送时记一条审计。
 */
@Component
public class BarPendingClearAlertJob {

    static final String PERMISSION = "bar.table.open_close";
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BarPendingClearAlertJob.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final AuthorizationService authorizationService;
    private final JdbcStaffRepository staffRepository;
    private final NotificationService notificationService;
    private final AuditEventService auditEventService;
    private final boolean scheduledEnabled;

    public BarPendingClearAlertJob(NamedParameterJdbcTemplate jdbc, AuthorizationService authorizationService,
                                   JdbcStaffRepository staffRepository, NotificationService notificationService,
                                   AuditEventService auditEventService,
                                   @Value("${bar.pending-clear-alert.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.jdbc = jdbc;
        this.authorizationService = authorizationService;
        this.staffRepository = staffRepository;
        this.notificationService = notificationService;
        this.auditEventService = auditEventService;
        this.scheduledEnabled = scheduledEnabled;
    }

    /** @Scheduled 薄包装：默认关闭，仅在 bar.pending-clear-alert.scheduled.enabled=true 时注册（默认每分钟一轮）。 */
    @ConditionalOnProperty(name = "bar.pending-clear-alert.scheduled.enabled", havingValue = "true")
    @Scheduled(fixedDelayString = "${bar.pending-clear-alert.scheduled.fixed-delay-ms:60000}")
    public void scheduledAlert() {
        if (scheduledEnabled) {
            alertOverdue(Instant.now());
        }
    }

    /** 对 now 时刻已超时的待清台台位逐台提醒；返回本轮新发送条数。 */
    int alertOverdue(Instant now) {
        record Overdue(String tableId, String barId, String tableCode, String tableName, Instant since, int minutes) {}
        MapSqlParameterSource at = new MapSqlParameterSource().addValue("now", Timestamp.from(now));
        List<Overdue> overdue = jdbc.query("""
                SELECT t.id, t.bar_id, t.table_code, t.table_name, t.pending_clear_since,
                       COALESCE(m.pending_clear_alert_minutes, 15) AS minutes
                  FROM bar_tables t
                  JOIN merchants m ON m.id = t.bar_id
                 WHERE t.status = 'pending_clear'
                   AND t.pending_clear_since < CAST(:now AS timestamptz) - make_interval(mins => COALESCE(m.pending_clear_alert_minutes, 15))
                """, at, (rs, n) -> new Overdue(rs.getString("id"), rs.getString("bar_id"), rs.getString("table_code"),
                rs.getString("table_name"), rs.getTimestamp("pending_clear_since").toInstant(), rs.getInt("minutes")));
        int sentTotal = 0;
        for (Overdue table : overdue) {
            List<AccessActor> onDuty = jdbc.query("""
                    SELECT u.id, u.username, u.account_type, u.organization_id, u.merchant_id, u.status
                      FROM access_users u
                     WHERE u.status = 'active' AND u.merchant_id = :bar
                       AND EXISTS (SELECT 1 FROM staff_shift_schedules s
                                    WHERE s.user_id = u.id AND s.start_at <= :now AND s.end_at > :now)
                     ORDER BY u.id
                    """, new MapSqlParameterSource().addValue("bar", table.barId()).addValue("now", Timestamp.from(now)),
                    (rs, n) -> new AccessActor(rs.getString("id"), rs.getString("username"), rs.getString("account_type"),
                            rs.getString("organization_id"), rs.getString("merchant_id"), rs.getString("status")));
            int sent = 0;
            for (AccessActor actor : onDuty) {
                if (!authorizationService.hasEffectivePermission(actor, PERMISSION)) continue;
                String phone = staffRepository.findProfileByUserId(actor.id()).map(StaffProfileRecord::phone)
                        .map(Strings::trimToNull).orElse(null);
                if (phone == null) continue;
                String requestId = "pclr_" + UUID.nameUUIDFromBytes((table.tableId() + "|" + table.since() + "|" + actor.id())
                        .getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
                Integer exists = jdbc.queryForObject("SELECT count(*) FROM notification_send_log WHERE request_id = :id",
                        Map.of("id", requestId), Integer.class);
                if (exists != null && exists > 0) continue;
                try {
                    notificationService.notify("bar", "pending_clear_overdue", phone, "sms.bar.pending_clear_overdue",
                            Map.of("table_id", table.tableId(), "table_code", String.valueOf(table.tableCode()),
                                    "table_name", String.valueOf(table.tableName()), "pending_clear_since", table.since().toString(),
                                    "alert_minutes", table.minutes()), requestId);
                    sent++;
                } catch (RuntimeException exception) {
                    log.warn("bar pending clear alert failed: table_id={}", table.tableId(), exception);
                }
            }
            if (sent > 0) {
                auditEventService.recordSuccess("bar.table.pending_clear_alerted", PERMISSION, "bar_table", table.tableId(),
                        null, null, Map.of("pending_clear_since", table.since().toString(), "alert_minutes", table.minutes(),
                                "sent", sent));
                sentTotal += sent;
            }
        }
        return sentTotal;
    }
}
