package com.complex.platform.bar;

// 文件版本：V0.03

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.ManualBackfillService;
import com.complex.platform.evidence.ManualBackfillTarget;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 第十八批 F 段第 14 条 14-R3 台位会话销售归属补录（BSM 第 1642 行人工补录；第 1021 行「由有销售归属的订台开台的，自动记营销带客并带出该销售人员；
 * 以上都不命中的直接到店开台默认记自来客……来源与销售人员的事后改派须店长复核并留痕，只能在该账单结清前办理」）。
 * 「记录缺失」＝会话与其订台都没有销售；到店＝开台时刻，开卡＝订下卡座的时刻（BSM 第 870 行「开卡人（订下这个卡座的人）」：带订台取订台时刻，
 * 直接到店开台取开台时刻）。拟归属人须有员工档案且在岗，可为别的酒吧的员工（BSM 第 1070 行「销售人员可跨酒吧承接订单」）。
 * 复核通过后会话、本会话未结清账单及其订单、尚未开单的订单改归拟归属人（账单销售岗位档按其员工档案重取），已结清账单与其订单不动；
 * 提成按账单销售计，业绩随归属走（BSM 第 1468 行）。台组订单与关台检查同一集合（{@link BarServiceResponsibilityService#close}）。
 * 第十八批 K 段 95-R2：店长事后改派来源与销售（{@link #reassign}）与补录按同一界线分工——会话与其订台都没有销售时补记销售只走补录，其余改派由店长持
 * bar.table_session.attribution_reassign 本人办理；两者共用本类的台账、结清判定、在岗校验与写入（{@link #applyAttribution}）。
 */
@Component
class BarSessionSalesBackfillTarget implements ManualBackfillTarget {

    static final String TYPE = "bar_session_sales";
    private static final List<String> SETTLED = List.of("settled", "merged", "void");
    // 第十八批 K 段 95-R1：开台客人来源中文名（与前端 fieldLabels.ts KEY_VALUE_LABELS.guest_source 同名）。
    private static final Map<String, String> SOURCE_LABELS = Map.of("platform_own", "平台自有渠道", "marketing", "营销带客", "walk_in", "自来客");
    static final String REASSIGN_PERMISSION = "bar.table_session.attribution_reassign";

    private final BarPayrollSupport p;
    private final BarRepository bars;
    private final BarBillingService billing;
    private final AuditEventService audit;
    // 第十八批 T 段第 157 条 157-R3：已确认坏账的账单改派不重算提成（字段注入免改构造点；单测未注入即不查）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    BarOrderSuspensionService suspensions;

    BarSessionSalesBackfillTarget(BarPayrollSupport p, BarRepository bars, BarBillingService billing, AuditEventService audit) {
        this.p = p;
        this.bars = bars;
        this.billing = billing;
        this.audit = audit;
    }

    @Override public String type() { return TYPE; }
    @Override public String submitPermission() { return "bar.table_session.create"; }
    @Override public String viewPermission() { return "bar.table_session.view"; }
    @Override public String policyActionType() { return "bar.sales.backfill"; }

    @Override
    public void requireScope(String targetId) {
        scoped(targetId, submitPermission());
    }

    @Override
    public void requireView(String targetId) {
        scoped(targetId, viewPermission());
    }

    @Override
    public Resolved resolve(String targetId, String part, ManualBackfillService.Request request) {
        BarTableSessionRecord session = session(targetId);
        BarReservationRecord booking = booking(session);
        if (session.salesStaffId() != null || (booking != null && booking.salesStaffId() != null)) {
            throw ApiException.conflict("SALES_ATTRIBUTION_PRESENT", "该开台已有销售归属，不属记录缺失；改派销售不走补录");
        }
        Ledger ledger = ledger(session);
        if (ledger.settled()) {
            throw ApiException.conflict("BAR_BILL_ALREADY_SETTLED", "账单已结清，不能再补录销售归属");
        }
        String sales = requireOnDuty(request.salesStaffId());
        var scope = p.tableScope(session.tableId());
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("guest_source", session.guestSource());
        original.put("session_sales", null);
        original.put("reservation_id", session.reservationId());
        original.put("reservation_sales", null);
        original.put("orders", ledger.ownOrders().stream().map(o -> row("order_id", o.id(), "sales_staff_id", o.salesStaffId())).toList());
        original.put("bills", ledger.ownBills().stream().map(b -> row("bill_id", b.id(), "sales_staff_id", b.salesStaffId(),
                "sales_role_type", b.salesRoleType(), "settlement_status", b.settlementStatus())).toList());
        Instant openCard = booking == null ? session.openedAt() : booking.createdAt();
        return new Resolved(scope.project(), scope.merchant(), session.openedAt(), session.openedAt(), session.openedAt(), openCard, sales,
                Map.of("sales_staff_id", sales), original, "台位会话 " + session.id() + "（台位 " + session.tableId()
                + (booking == null ? "，直接到店开台" : "，订台 " + booking.id()) + "）销售归属：原无销售（"
                + SOURCE_LABELS.getOrDefault(session.guestSource(), session.guestSource()) + "），拟补为 " + sales);
    }

    @Override
    public void apply(ManualBackfillService.Backfill backfill) {
        p.jdbc.queryForList("SELECT id FROM bar_table_sessions WHERE id=? FOR UPDATE", String.class, backfill.targetId());
        BarTableSessionRecord session = session(backfill.targetId());
        BarReservationRecord booking = booking(session);
        Ledger ledger = ledger(session);
        if (session.salesStaffId() != null || (booking != null && booking.salesStaffId() != null) || ledger.settled()) {
            throw ApiException.conflict("MANUAL_BACKFILL_TARGET_CHANGED", "该开台已有销售归属或账单已结清，补录不再适用");
        }
        String sales = String.valueOf(backfill.proposed().get("sales_staff_id"));
        // 第十八批 K 段 95-R1：补记销售即记营销带客（来源与销售成对写）。
        Changed changed = applyAttribution(session, ledger, "marketing", sales);
        audit.recordSuccess("bar.sales_backfill_applied", "bar.sales_backfill.review", "bar_table_session", session.id(), null, null,
                Map.of("backfill_id", backfill.id(), "sales_staff_id_before", "", "sales_staff_id_after", sales,
                        "sales_role_type", changed.roleType(), "order_ids", changed.orders(), "bill_ids", changed.bills()));
    }

    /** 第十八批 K 段 95-R2：店长改派的范围闸（台位所在综合体与商户，同 {@link #requireScope}），返回改派前的会话。 */
    BarTableSessionRecord requireReassignScope(String sessionId) {
        BarTableSessionRecord session = session(sessionId);
        var scope = p.tableScope(session.tableId());
        p.requireScope(scope.project(), scope.merchant(), REASSIGN_PERMISSION);
        return session;
    }

    /**
     * 第十八批 K 段 95-R2（BSM 第 1021 行「来源与销售人员的事后改派须店长复核并留痕，只能在该账单结清前办理」）：会话须开台中、台组账单未全部结清；
     * 营销带客须带在岗销售（可跨酒吧），其余来源销售置空；会话与其订台原都没有销售时补记销售属记录缺失、走销售归属补录（BSM 第 1642 行）。订台不改。
     */
    BarTableSessionRecord reassign(String sessionId, String guestSource, String salesStaffId) {
        p.jdbc.queryForList("SELECT id FROM bar_table_sessions WHERE id=? FOR UPDATE", String.class, sessionId);
        BarTableSessionRecord session = session(sessionId);
        if (!"open".equals(session.status())) {
            throw ApiException.conflict("BAR_TABLE_SESSION_NOT_ACTIVE", "台位会话不在开台状态");
        }
        String source = requireText(guestSource, "guest_source");
        if (!SOURCE_LABELS.containsKey(source)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "客人来源须为自来客、平台自有渠道或营销带客");
        }
        String sales = "marketing".equals(source) ? requireOnDuty(salesStaffId) : null;
        BarReservationRecord booking = booking(session);
        if (sales != null && session.salesStaffId() == null && (booking == null || booking.salesStaffId() == null)) {
            throw ApiException.conflict("BAR_SALES_BACKFILL_REQUIRED", "原无销售归属的补记销售走销售归属补录");
        }
        Ledger ledger = ledger(session);
        if (ledger.settled()) {
            throw ApiException.conflict("BAR_BILL_ALREADY_SETTLED", "账单已结清，不能再改派来源与销售");
        }
        applyAttribution(session, ledger, source, sales);
        return session(sessionId);
    }

    /**
     * 改归属（补录批准与店长改派共用）：会话来源与销售、本会话未结清账单（销售岗位档按新归属重取）及其订单、尚未开单的订单；已结清账单与其订单不动。
     * 18K 复核：只改本会话这一方客人的订单与账单，并入子会话（含整体转台前旧链）的不动（BSM 第 1079 行「并台：……销售提成归属不变，仍跟各自的客人走」）。
     * 第十八批 K 段 95-R2（BSM 第 1468 行「销售归属改派后，业绩随提成归属一起改」）：未结清账单上已计提的提成随归属改——先结清工资单为其挂的待定，
     * 新销售非空则按新档重取费率、重算金额并置已计提，置空则作废（未结清账单上的作废只可能来自本改派）。
     */
    private Changed applyAttribution(BarTableSessionRecord session, Ledger ledger, String guestSource, String sales) {
        String roleType = billing.resolveSalesRoleType(sales);
        p.jdbc.update("UPDATE bar_table_sessions SET guest_source=?, sales_staff_id=?, session_version=session_version+1, updated_at=now() WHERE id=?",
                guestSource, sales, session.id());
        List<String> orders = ledger.ownOrders().stream().map(BarOrderRecord::id)
                .filter(id -> ledger.bills().stream().noneMatch(b -> b.orderId().equals(id) && SETTLED.contains(b.settlementStatus()))).toList();
        List<BarBillRecord> unsettled = ledger.ownBills().stream().filter(b -> !SETTLED.contains(b.settlementStatus())).toList();
        orders.forEach(id -> p.jdbc.update("UPDATE bar_orders SET sales_staff_id=?, updated_at=now() WHERE id=?", sales, id));
        for (BarBillRecord bill : unsettled) {
            p.jdbc.update("UPDATE bar_bills SET sales_staff_id=?, sales_role_type=?, updated_at=now() WHERE id=?", sales, roleType, bill.id());
            List<Map<String, Object>> commissions = p.jdbc.queryForList(
                    "SELECT id, commission_base_cent FROM bar_commission_calculations WHERE bill_id=?", bill.id());
            // 第十八批 T 段第 157 条 157-R3（BSM:1111「转坏账核销的订单不产生销售提成」）：所属订单已确认坏账的账单提成保持作废、不按新归属重算。
            if (commissions.isEmpty() || suspensions != null && suspensions.badDebtConfirmed(bill)) {
                continue;
            }
            p.jdbc.update("""
                    UPDATE staff_payroll_pending_sources SET status = 'resolved', resolved_at = clock_timestamp()
                     WHERE source_type = 'BAR_COMMISSION_PENDING' AND status = 'pending'
                       AND source_id IN (SELECT id FROM bar_commission_calculations WHERE bill_id = ?)""", bill.id());
            Integer rate = sales == null ? null : billing.requireCommissionRateBp(bill.merchantId(), roleType);
            for (Map<String, Object> commission : commissions) {
                if (rate == null) {
                    p.jdbc.update("UPDATE bar_commission_calculations SET status='void' WHERE id=?", commission.get("id"));
                } else {
                    long base = ((Number) commission.get("commission_base_cent")).longValue();
                    p.jdbc.update("UPDATE bar_commission_calculations SET sales_staff_id=?, sales_role_type=?, commission_rate_bp=?, "
                            + "commission_amount_cent=?, status='calculated' WHERE id=?", sales, roleType, rate, MoneyRounding.bpsOf(base, rate),
                            commission.get("id"));
                }
            }
        }
        return new Changed(roleType, orders, unsettled.stream().map(BarBillRecord::id).toList());
    }

    private record Changed(String roleType, List<String> orders, List<String> bills) {
    }

    private void scoped(String targetId, String permission) {
        var scope = p.tableScope(session(targetId).tableId());
        p.requireScope(scope.project(), scope.merchant(), permission);
    }

    private BarTableSessionRecord session(String id) {
        return bars.findTableSessionById(id)
                .orElseThrow(() -> ApiException.notFound("BAR_TABLE_SESSION_NOT_FOUND", "台位就餐会话不存在"));
    }

    private BarReservationRecord booking(BarTableSessionRecord session) {
        return session.reservationId() == null ? null : bars.findReservationById(session.reservationId()).orElse(null);
    }

    /** 拟归属人：账号正常、档案已批准且在用、在职或试用。不要求本商户（可跨酒吧承接）。 */
    private String requireOnDuty(String salesStaffId) {
        String id = requireText(salesStaffId, "sales_staff_id");
        var profile = p.staff.findProfileByUserId(id);
        var account = p.access.findUserById(id);
        if (profile.isEmpty() || account.isEmpty() || !"active".equals(account.get().status())
                || !"active".equals(profile.get().status()) || !"approved".equals(profile.get().registrationStatus())
                || profile.get().archivedAt() != null || !List.of("employed", "trial").contains(profile.get().employmentStatus())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "销售人员须有员工档案且在岗");
        }
        return id;
    }

    /** 台组（含整体转台前旧链）锁定的订单＋开台以来无会话归属的订单，及其账单；own＝本会话自己的订单＋本会话开台以来本台位上无会话归属的订单。 */
    private Ledger ledger(BarTableSessionRecord session) {
        List<String> family = bars.familySessionIds(session.id(), true);
        List<BarOrderRecord> orders = new ArrayList<>(bars.listOrderIdsBySessionIds(family).stream()
                .map(bars::findOrderById).flatMap(Optional::stream).toList());
        List<BarTableSessionRecord> group = family.stream().map(bars::findTableSessionById).flatMap(Optional::stream).toList();
        group.stream().map(BarTableSessionRecord::openedAt).min(Comparator.naturalOrder()).ifPresent(since ->
                orders.addAll(bars.listOrdersWithoutSession(group.stream().map(BarTableSessionRecord::tableId).distinct().toList(), since)));
        Set<String> own = new HashSet<>(bars.listOrderIdsBySessionIds(List.of(session.id())));
        bars.listOrdersWithoutSession(List.of(session.tableId()), session.openedAt()).forEach(o -> own.add(o.id()));
        return new Ledger(session.status(), orders, bars.listBillsByOrderIds(orders.stream().map(BarOrderRecord::id).toList()), own);
    }

    /** 已结清＝已关台，或台组已有账单且全部结清（含已合并、已作废）、订单都已开单；写入只取 own。 */
    private record Ledger(String status, List<BarOrderRecord> orders, List<BarBillRecord> bills, Set<String> own) {
        List<BarOrderRecord> ownOrders() {
            return orders.stream().filter(o -> own.contains(o.id())).toList();
        }

        List<BarBillRecord> ownBills() {
            return bills.stream().filter(b -> own.contains(b.orderId())).toList();
        }

        boolean settled() {
            return "closed".equals(status) || (!bills.isEmpty() && bills.stream().allMatch(b -> SETTLED.contains(b.settlementStatus()))
                    && orders.stream().allMatch(o -> bills.stream().anyMatch(b -> b.orderId().equals(o.id()))));
        }
    }

    private static Map<String, Object> row(Object... pairs) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            row.put((String) pairs[i], pairs[i + 1]);
        }
        return row;
    }
}
