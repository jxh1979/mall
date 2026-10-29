package com.complex.platform.ticketing;

// 文件版本：V0.02

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ROOT-03 clients-R10 未核销票到期提醒。BSM 第 1596 行「未核销的票在到期前通过官方 App 或小程序弹出的到期提醒（业务参数，按项目可配，出厂默认
 * 到期前二十四小时，须同时配置上下限）」；决策台账 DEC-R4-2026-08-04-84「未核销票到期前二十四小时经 App／小程序弹通知，并列入不可关闭的通知清单」。
 * 参数范式同押金退还等待期（V499）：项目无自有行即用 '__global__' 出厂行（24 小时、上下限未配），首次配置必须同时填写上下限。
 * 应用关闭时的系统推送需推送凭证，未接入；客户端启动与回前台拉取本接口弹出。
 */
@Service
public class TicketExpiryReminderService {

    static final String VIEW_PERMISSION = "ticket.product.view";
    static final String MANAGE_PERMISSION = "ticket.product.manage";
    private static final String GLOBAL = "__global__";

    /** projectId＝生效行所属（项目自有行或 '__global__' 出厂行）。 */
    public record Policy(String projectId, int remindHours, Integer minHours, Integer maxHours) {
    }

    public record PolicyRequest(String idempotencyKey, Integer remindHours, Integer minHours, Integer maxHours, String reason) {
    }

    /** ticketId＝我的票（权益）编号。 */
    public record Reminder(String ticketId, String parentTicketId, String ticketName, String orderId, Instant validTo) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final TicketingRepository repository;
    private final AuthorizationService authorizationService;
    private final IdempotencyService idempotencyService;
    private final AuditEventService auditEventService;

    public TicketExpiryReminderService(NamedParameterJdbcTemplate jdbc, TicketingRepository repository,
            AuthorizationService authorizationService, IdempotencyService idempotencyService, AuditEventService auditEventService) {
        this.jdbc = jdbc;
        this.repository = repository;
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
            String project = requireProject(projectId, MANAGE_PERMISSION);
            Integer hours = request == null ? null : request.remindHours();
            Integer min = request == null ? null : request.minHours();
            Integer max = request == null ? null : request.maxHours();
            if (hours == null || min == null || max == null || min < 1 || hours < min || hours > max) {
                throw ApiException.badRequest("VALIDATION_FAILED", "到期提醒提前小时数须同时填写参数值及上下限，下限不小于 1，参数值须在上下限内");
            }
            return idempotencyService.execute("saveTicketExpiryReminderPolicy:" + project, key, request, () -> {
                Policy before = policy(project);
                jdbc.update("""
                        INSERT INTO ticket_expiry_reminder_policies (project_id, remind_hours, min_hours, max_hours, updated_by, updated_at)
                        VALUES (:pid, :hours, :min, :max, :actor, now())
                        ON CONFLICT (project_id) DO UPDATE SET remind_hours = EXCLUDED.remind_hours, min_hours = EXCLUDED.min_hours,
                            max_hours = EXCLUDED.max_hours, updated_by = EXCLUDED.updated_by, updated_at = now()
                        """, new MapSqlParameterSource().addValue("pid", project).addValue("hours", hours).addValue("min", min)
                        .addValue("max", max).addValue("actor", authorizationService.currentActor().id()));
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("before_remind_hours", before.remindHours());
                detail.put("before_min_hours", before.minHours() == null ? "" : before.minHours());
                detail.put("before_max_hours", before.maxHours() == null ? "" : before.maxHours());
                detail.put("remind_hours", hours);
                detail.put("min_hours", min);
                detail.put("max_hours", max);
                detail.put("reason", request.reason() == null ? "" : request.reason());
                auditEventService.recordSuccess("ticket.expiry_reminder_policy_updated", MANAGE_PERMISSION, "ticket_expiry_reminder_policy",
                        project, null, key, detail);
                return new Policy(project, hours, min, max);
            }, Policy.class);
        } catch (RuntimeException failure) {
            auditEventService.recordFailure("ticket.expiry_reminder_policy_update_failed", MANAGE_PERMISSION, "ticket_expiry_reminder_policy",
                    projectId, null, key, failure instanceof ApiException api ? api.code() : "INTERNAL_ERROR");
            throw failure;
        }
    }

    /** 本人持有、未核销、到期时刻落在 [现在, 现在＋该票所属项目的提前小时数] 内的票（客人会话由控制器校验）。 */
    public List<Reminder> remindersFor(String customerId) {
        Instant now = Instant.now();
        Map<String, Integer> hoursByProject = new HashMap<>();
        List<Reminder> out = new ArrayList<>();
        for (TicketSlotHolderRecord holder : repository.listSlotHoldersByHolder(customerId)) {
            for (IssuedEntitlementRecord e : repository.listIssuedEntitlementsByParentSlot(holder.parentTicketId(), holder.slotIndex())) {
                if (e.usedTimes() > 0 || !"active".equals(e.status()) || e.validTo() == null || !e.validTo().isAfter(now)) {
                    continue;
                }
                String project = repository.findTicketOrderById(e.orderId()).map(TicketOrderRecord::projectId).orElse(null);
                int hours = hoursByProject.computeIfAbsent(project, p -> policy(p).remindHours());
                if (!e.validTo().isAfter(now.plus(hours, ChronoUnit.HOURS))) {
                    out.add(new Reminder(e.id(), e.parentTicketId(),
                            repository.findTicketTypeById(e.ticketTypeId()).map(TicketTypeRecord::ticketName).orElse(null),
                            e.orderId(), e.validTo()));
                }
            }
        }
        return out;
    }

    private Policy policy(String projectId) {
        return jdbc.query("""
                SELECT project_id, remind_hours, min_hours, max_hours FROM ticket_expiry_reminder_policies
                 WHERE project_id = :pid OR project_id = '__global__'
                 ORDER BY (project_id = '__global__') LIMIT 1
                """, new MapSqlParameterSource("pid", projectId == null ? GLOBAL : projectId),
                (rs, n) -> new Policy(rs.getString("project_id"), rs.getInt("remind_hours"),
                        rs.getObject("min_hours", Integer.class), rs.getObject("max_hours", Integer.class)))
                .stream().findFirst()
                .orElseThrow(() -> ApiException.conflict("POLICY_NOT_CONFIGURED", "到期提醒参数未配置出厂行"));
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
