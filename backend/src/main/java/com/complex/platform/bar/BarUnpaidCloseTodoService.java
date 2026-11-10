package com.complex.platform.bar;

// 文件版本：V0.02

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.staff.EmployeeNoticeRecord;
import com.complex.platform.access.staff.JdbcStaffRepository;
import com.complex.platform.access.staff.StaffProfileRecord;
import com.complex.platform.common.LocalZone;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.governance.review.SupervisorReviewService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 T 段第 157 条 157-R5：包厢关台时没付清——关台照拦（BSM:1058「账单未结清（含挂起未结清）不得关台」，DEC-R8-97-R1-03），
 * 被拦的同一刻给当班经理发「确认能否收回」待办，到期没处理由 {@link BarUnpaidCloseEscalationJob} 升级给店长或夜班值班经理。
 * 依据：第十八批裁决 37 选项原文「包厢关台时没付清就发经理待办，超时按通用时限升级」，追问题面「客人离开（关台）时仍没付清，系统立即给当班经理发
 * 「确认能否收回」待办，经理在全平台审批通用时限内（涉钱出厂 1 个工作日）没处理就升级给店长或夜班值班经理」；BSM:1109「经理未及时确认时，任务升级给
 * 店长或夜班值班经理，在转入该状态前继续阻断」；PGM:529「涉钱审批出厂默认 1 个工作日……不得默认通过，也不得默认拒绝」。
 * 当班经理＝本酒吧（access_users.merchant_id）在用、此刻有排班、持 bar.bill.hang_cancel（PGM:699「经理审批取消」）、有员工档案的员工。
 * 待办对象＝该次关台时所有没付清的账单，不论事先有没有挂起；转坏账仍须先由收银员挂起（BSM:1106），正文对未挂起的注明。
 */
@Service
public class BarUnpaidCloseTodoService {

    static final String MANAGER = "bar.bill.hang_cancel";
    static final String ESCALATION = "bar.bill.hang_escalation";
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(LocalZone.ID);

    private final NamedParameterJdbcTemplate jdbc;
    private final AuthorizationService auth;
    private final JdbcStaffRepository staff;
    private final SupervisorReviewService reviews;
    private final BarOrderSuspensionService suspensions;
    private final AuditEventService audit;

    public BarUnpaidCloseTodoService(NamedParameterJdbcTemplate jdbc, AuthorizationService auth, JdbcStaffRepository staff,
            SupervisorReviewService reviews, BarOrderSuspensionService suspensions, AuditEventService audit) {
        this.jdbc = jdbc;
        this.auth = auth;
        this.staff = staff;
        this.reviews = reviews;
        this.suspensions = suspensions;
        this.audit = audit;
    }

    /**
     * 包厢关台被未结清账单拦下时：每张账单登记一条待办（已有不重登、期限不改；期限＝审批通用时限 bar.bill.unpaid_close（第十八批 AD 段 204-R1 后台可改，出厂涉钱 1 个工作日），无当班经理也照样起算），给当班经理各发一条
     * 员工系统通知（同一批账单每人只发一次）。独立事务：随后的 409 让关台回滚，待办不丢。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyAtClose(String sessionId, BarTableRecord table, List<BarBillRecord> bills) {
        Instant now = Instant.now();
        String project = jdbc.queryForList("SELECT s.project_id FROM bar_tables t JOIN spaces s ON s.id = t.space_id WHERE t.id = :table",
                Map.of("table", table.id()), String.class).stream().filter(Objects::nonNull).findFirst().orElse(null);
        int registered = 0;
        for (BarBillRecord bill : bills) {
            registered += jdbc.update("""
                    INSERT INTO bar_bill_unpaid_close_todos (bill_id, bar_id, merchant_id, project_id, session_id, notified_at, due_at)
                    VALUES (:bill, :bar, :merchant, :project, :session, :now, :due) ON CONFLICT (bill_id) DO NOTHING""",
                    new MapSqlParameterSource().addValue("bill", bill.id()).addValue("bar", bill.barId())
                            .addValue("merchant", bill.merchantId()).addValue("project", project).addValue("session", sessionId)
                            .addValue("now", Timestamp.from(now)).addValue("due", Timestamp.from(reviews.deadlineFrom(SupervisorReviewService.BAR_UNPAID_CLOSE_ACTION, now))));
        }
        List<String> ids = bills.stream().map(BarBillRecord::id).sorted().toList();
        Instant deadline = jdbc.queryForObject("SELECT min(due_at) FROM bar_bill_unpaid_close_todos WHERE bill_id IN (:ids)",
                Map.of("ids", ids), Timestamp.class).toInstant();
        List<String> lines = new ArrayList<>();
        for (BarBillRecord bill : bills) {
            String line = "账单 " + bill.billNo() + " 未付 " + money(bill.payableAmountCent() + bill.tipAmountCent() - bill.paidAmountCent());
            Map<String, Object> hanging = jdbc.queryForList("""
                    SELECT reason, responsible_user_id FROM bar_order_suspensions
                     WHERE (order_id = :order OR bill_id = :bill) AND status IN ('hanging_unpaid', 'cancel_pending_approval') LIMIT 1""",
                    new MapSqlParameterSource().addValue("order", bill.orderId()).addValue("bill", bill.id())).stream().findFirst().orElse(null);
            lines.add(hanging == null ? line + "，未挂起：须先由收银员挂起登记原因"
                    : line + "，已挂起（原因：" + hanging.get("reason") + "；责任人：" + responsible((String) hanging.get("responsible_user_id")) + "）");
        }
        String body = "包厢 " + table.tableName() + " 客人离开时以下账单未付清，请确认能否收回：" + String.join("；", lines)
                + "。处理期限：" + TIME.format(deadline) + "。";
        String key = String.join(",", ids);
        int sent = 0;
        for (String profile : barStaffProfiles(table.barId(), MANAGER, now)) {
            if (sendOnce("unpaid_close", key, profile, "包厢客人离开未付清：请确认能否收回", body, now)) {
                sent++;
            }
        }
        if (registered > 0 || sent > 0) {
            audit.recordSuccess("bar.bill_unpaid_close_notified", MANAGER, "bar_table_session", sessionId, null, null,
                    Map.of("bill_no", bills.stream().map(BarBillRecord::billNo).collect(Collectors.joining("、")),
                            "due_at", deadline.toString(), "notified", sent));
        }
    }

    /**
     * 157-R5 ⑥ 转坏账鉴权（在幂等之前，代替原控制器的 bar.bill.hang_cancel 闸）：持 bar.bill.hang_cancel 照旧（本商户）；本单账单（或挂起改指的主单）的
     * 关台未付清待办已升级的，持 bar.bill.hang_escalation 的本商户员工（店长）或本项目此刻在班的夜班值班经理（按已发布排班与顶班关系）也可办理；
     * 都不满足按缺 bar.bill.hang_cancel 拒绝（403 PERMISSION_DENIED）。
     */
    void requireBadDebtAuthority(String orderId) {
        if (!auth.currentActorHasPermission(MANAGER)) {
            List<String> escalated = jdbc.queryForList("""
                    SELECT COALESCE(project_id, '') FROM bar_bill_unpaid_close_todos
                     WHERE escalated_at IS NOT NULL
                       AND bill_id IN (SELECT id FROM bar_bills WHERE order_id = :order
                                       UNION SELECT bill_id FROM bar_order_suspensions WHERE order_id = :order)""",
                    Map.of("order", orderId), String.class);
            if (!escalated.isEmpty()) {
                if (auth.currentActorHasPermission(ESCALATION)) {
                    auth.requirePermission(ESCALATION);
                    suspensions.requireOrderScope(orderId);
                    return;
                }
                String project = escalated.getFirst();
                if (!project.isEmpty() && staff.findCurrentNightDuty(auth.currentActor().id(), project, Instant.now()).isPresent()) {
                    return;
                }
            }
        }
        auth.requirePermission(MANAGER);
        suspensions.requireOrderScope(orderId);
    }

    /** 本酒吧在用、持该码、有员工档案的员工档案号；onShiftAt 非空时只取此刻有排班的（SQL 同 BarPendingClearAlertJob）。 */
    List<String> barStaffProfiles(String barId, String permission, Instant onShiftAt) {
        String shift = onShiftAt == null ? ""
                : " AND EXISTS (SELECT 1 FROM staff_shift_schedules s WHERE s.user_id = u.id AND s.start_at <= :at AND s.end_at > :at)";
        List<AccessActor> users = jdbc.query("""
                SELECT u.id, u.username, u.account_type, u.organization_id, u.merchant_id, u.status FROM access_users u
                 WHERE u.status = 'active' AND u.merchant_id = :bar""" + shift + " ORDER BY u.id",
                new MapSqlParameterSource().addValue("bar", barId).addValue("at", onShiftAt == null ? null : Timestamp.from(onShiftAt)),
                (rs, n) -> new AccessActor(rs.getString("id"), rs.getString("username"), rs.getString("account_type"),
                        rs.getString("organization_id"), rs.getString("merchant_id"), rs.getString("status")));
        return users.stream().filter(actor -> auth.hasEffectivePermission(actor, permission))
                .map(actor -> profileId(actor.id())).filter(Objects::nonNull).toList();
    }

    String profileId(String userId) {
        return staff.findProfileByUserId(userId).map(StaffProfileRecord::id).orElse(null);
    }

    /** 发一条员工系统通知作待办；通知号由类别＋key＋员工档案派生，已发过即跳过（不覆盖：覆盖会把员工已读的通知改回未读）。 */
    boolean sendOnce(String kind, String key, String profileId, String title, String body, Instant now) {
        String id = "notice_" + UUID.nameUUIDFromBytes(("bar_bill_unpaid_close:" + kind + ":" + key + ":" + profileId)
                .getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
        if (staff.findNoticeById(id).isPresent()) {
            return false;
        }
        staff.saveNotice(new EmployeeNoticeRecord(id, profileId, "system_notice", title, body, false, "pending", null, "system", now, now));
        return true;
    }

    static String money(long cent) {
        return BigDecimal.valueOf(cent, 2).toPlainString() + " 元";
    }

    private String responsible(String userId) {
        return userId == null ? "门店" : staff.findProfileByUserId(userId).map(StaffProfileRecord::name).orElse(userId);
    }
}
