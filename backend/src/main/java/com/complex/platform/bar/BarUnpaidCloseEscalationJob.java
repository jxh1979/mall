package com.complex.platform.bar;

// 文件版本：V0.01

import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.evidence.AuditEventService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 第十八批 T 段第 157 条 157-R5：包厢关台未付清待办超时升级（第十八批裁决 37「超时按通用时限升级」，追问题面「经理在全平台审批通用时限内
 * （涉钱出厂 1 个工作日）没处理就升级给店长或夜班值班经理」；BSM:1109「经理未及时确认时，任务升级给店长或夜班值班经理，在转入该状态前继续阻断」；
 * PGM:529「不得默认通过，也不得默认拒绝」；PGM:429 夜班值班经理由系统根据当日排班自动指定）。同 BarPendingClearAlertJob 范式：逻辑在可直接调用的
 * {@link #escalateOverdue}，@Scheduled 只做薄包装。
 * 到期（due_at≤now）、未升级、账单仍未付清且其订单挂起不在坏账两态、也不是当班经理已提交转坏账正等高价值主管复核（已挂起待付款且
 * bad_debt_review_id 非空，不属于 BSM:1109「经理未及时确认时」；复核驳回清空后照常升级；18T 复核修复②）的待办：给本酒吧持 bar.bill.hang_escalation 的在用员工（店长）与本项目此刻在班的
 * 夜班值班经理（按已发布排班沿顶班关系解析实际当班人）各发一条员工系统通知，置 escalated_at；只升级一次，仍无人处理的保持待办、继续阻断，不默认转坏账。
 */
@Component
public class BarUnpaidCloseEscalationJob {

    private final NamedParameterJdbcTemplate jdbc;
    private final JdbcStaffRepository staff;
    private final BarUnpaidCloseTodoService todos;
    private final AuditEventService audit;
    private final boolean scheduledEnabled;

    public BarUnpaidCloseEscalationJob(NamedParameterJdbcTemplate jdbc, JdbcStaffRepository staff, BarUnpaidCloseTodoService todos,
            AuditEventService audit, @Value("${bar.unpaid-close-escalation.scheduled.enabled:false}") boolean scheduledEnabled) {
        this.jdbc = jdbc;
        this.staff = staff;
        this.todos = todos;
        this.audit = audit;
        this.scheduledEnabled = scheduledEnabled;
    }

    /** @Scheduled 薄包装：默认关闭，仅在 bar.unpaid-close-escalation.scheduled.enabled=true 时执行（默认每分钟一轮）。 */
    @Scheduled(fixedDelayString = "${bar.unpaid-close-escalation.scheduled.fixed-delay-ms:60000}")
    public void scheduledEscalate() {
        if (scheduledEnabled) {
            escalateOverdue(Instant.now());
        }
    }

    /** 对 now 时刻已到期的待办逐张升级；返回本轮新发通知条数。 */
    int escalateOverdue(Instant now) {
        record Due(String billId, String billNo, String barId, String projectId, String tableName, long dueCent, Instant dueAt) {}
        List<Due> due = jdbc.query("""
                SELECT t.bill_id, b.bill_no, t.bar_id, t.project_id, tb.table_name, t.due_at,
                       b.payable_amount_cent + b.tip_amount_cent - b.paid_amount_cent AS due_cent
                  FROM bar_bill_unpaid_close_todos t
                  JOIN bar_bills b ON b.id = t.bill_id
                  LEFT JOIN bar_tables tb ON tb.id = b.table_id
                 WHERE t.escalated_at IS NULL AND t.due_at <= :now
                   AND b.settlement_status NOT IN ('settled', 'merged', 'void')
                   AND NOT EXISTS (SELECT 1 FROM bar_order_suspensions s WHERE (s.order_id = b.order_id OR s.bill_id = b.id)
                                     AND (s.status IN ('bad_debt_processing', 'written_off_closed')
                                          OR (s.status = 'hanging_unpaid' AND s.bad_debt_review_id IS NOT NULL)))
                 ORDER BY t.due_at, t.bill_id""", Map.of("now", Timestamp.from(now)),
                (rs, n) -> new Due(rs.getString("bill_id"), rs.getString("bill_no"), rs.getString("bar_id"), rs.getString("project_id"),
                        rs.getString("table_name"), rs.getLong("due_cent"), rs.getTimestamp("due_at").toInstant()));
        int sentTotal = 0;
        for (Due row : due) {
            Set<String> recipients = new LinkedHashSet<>(todos.barStaffProfiles(row.barId(), BarUnpaidCloseTodoService.ESCALATION, null));
            recipients.addAll(nightDuty(row.projectId(), now));
            String body = "包厢 " + row.tableName() + " 账单 " + row.billNo() + " 未付 " + BarUnpaidCloseTodoService.money(row.dueCent())
                    + "：当班经理在处理期限（" + BarUnpaidCloseTodoService.TIME.format(row.dueAt()) + "）内未确认能否收回，升级由您确认。";
            int sent = 0;
            for (String profile : recipients) {
                if (todos.sendOnce("escalate", row.billId(), profile, "包厢未付清待办已超时：请确认能否收回", body, now)) {
                    sent++;
                }
            }
            jdbc.update("UPDATE bar_bill_unpaid_close_todos SET escalated_at = :now WHERE bill_id = :bill AND escalated_at IS NULL",
                    new MapSqlParameterSource().addValue("now", Timestamp.from(now)).addValue("bill", row.billId()));
            audit.recordSuccess("bar.bill_unpaid_close_escalated", BarUnpaidCloseTodoService.ESCALATION, "bar_bill", row.billId(), null, null,
                    Map.of("due_at", row.dueAt().toString(), "notified", sent));
            sentTotal += sent;
        }
        return sentTotal;
    }

    /** 本项目此刻在班的夜班值班经理（含顶班人）的员工档案号。 */
    private List<String> nightDuty(String projectId, Instant now) {
        if (projectId == null) {
            return List.of();
        }
        return jdbc.queryForList("""
                SELECT id FROM staff_shift_schedules WHERE night_duty_project_id = :project AND start_at <= :now AND end_at > :now""",
                new MapSqlParameterSource().addValue("project", projectId).addValue("now", Timestamp.from(now)), String.class).stream()
                .flatMap(root -> staff.findEffectiveShiftHolders(root, now).stream())
                .map(holder -> todos.profileId(holder.userId())).filter(Objects::nonNull).toList();
    }
}
