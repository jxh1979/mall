package com.complex.platform.bar;

// 文件版本：V0.01

import static com.complex.platform.common.Strings.requireText;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.ManualBackfillService;
import com.complex.platform.evidence.ManualBackfillTarget;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 第十八批 F 段第 14 条 14-R3 台位会话销售归属补录（BSM 第 1642 行人工补录；第 1021 行「由有销售归属的订台开台的，自动记营销带客并带出该销售人员；
 * 以上都不命中的直接到店开台默认记自来客……来源与销售人员的事后改派须店长复核并留痕，只能在该账单结清前办理」）。
 * 「记录缺失」＝会话与其订台都没有销售；到店＝开台时刻，开卡＝订下卡座的时刻（BSM 第 870 行「开卡人（订下这个卡座的人）」：带订台取订台时刻，
 * 直接到店开台取开台时刻）。拟归属人须有员工档案且在岗，可为别的酒吧的员工（BSM 第 1070 行「销售人员可跨酒吧承接订单」）。
 * 复核通过后会话、台组未结清账单及其订单、尚未开单的订单改归拟归属人（账单销售岗位档按其员工档案重取），已结清账单与其订单不动；
 * 提成按账单销售计，业绩随归属走（BSM 第 1468 行）。台组订单与关台检查同一集合（{@link BarServiceResponsibilityService#close}）。
 */
@Component
class BarSessionSalesBackfillTarget implements ManualBackfillTarget {

    static final String TYPE = "bar_session_sales";
    private static final List<String> SETTLED = List.of("settled", "merged", "void");

    private final BarPayrollSupport p;
    private final BarRepository bars;
    private final BarBillingService billing;
    private final AuditEventService audit;

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
        original.put("session_sales", null);
        original.put("reservation_id", session.reservationId());
        original.put("reservation_sales", null);
        original.put("orders", ledger.orders().stream().map(o -> row("order_id", o.id(), "sales_staff_id", o.salesStaffId())).toList());
        original.put("bills", ledger.bills().stream().map(b -> row("bill_id", b.id(), "sales_staff_id", b.salesStaffId(),
                "sales_role_type", b.salesRoleType(), "settlement_status", b.settlementStatus())).toList());
        Instant openCard = booking == null ? session.openedAt() : booking.createdAt();
        return new Resolved(scope.project(), scope.merchant(), session.openedAt(), session.openedAt(), session.openedAt(), openCard, sales,
                Map.of("sales_staff_id", sales), original, "台位会话 " + session.id() + "（台位 " + session.tableId()
                + (booking == null ? "，直接到店开台" : "，订台 " + booking.id()) + "）销售归属：原无（自来客），拟补为 " + sales);
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
        String roleType = billing.resolveSalesRoleType(sales);
        p.jdbc.update("UPDATE bar_table_sessions SET sales_staff_id=?, session_version=session_version+1, updated_at=now() WHERE id=?",
                sales, session.id());
        List<String> orders = ledger.orders().stream().map(BarOrderRecord::id)
                .filter(id -> ledger.bills().stream().noneMatch(b -> b.orderId().equals(id) && SETTLED.contains(b.settlementStatus()))).toList();
        List<String> bills = ledger.bills().stream().filter(b -> !SETTLED.contains(b.settlementStatus())).map(BarBillRecord::id).toList();
        orders.forEach(id -> p.jdbc.update("UPDATE bar_orders SET sales_staff_id=?, updated_at=now() WHERE id=?", sales, id));
        bills.forEach(id -> p.jdbc.update("UPDATE bar_bills SET sales_staff_id=?, sales_role_type=?, updated_at=now() WHERE id=?",
                sales, roleType, id));
        audit.recordSuccess("bar.sales_backfill_applied", "bar.sales_backfill.review", "bar_table_session", session.id(), null, null,
                Map.of("backfill_id", backfill.id(), "sales_staff_id_before", "", "sales_staff_id_after", sales,
                        "sales_role_type", roleType, "order_ids", orders, "bill_ids", bills));
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

    /** 台组（含整体转台前旧链）锁定的订单＋开台以来无会话归属的订单，及其账单。 */
    private Ledger ledger(BarTableSessionRecord session) {
        List<String> family = bars.familySessionIds(session.id(), true);
        List<BarOrderRecord> orders = new ArrayList<>(bars.listOrderIdsBySessionIds(family).stream()
                .map(bars::findOrderById).flatMap(Optional::stream).toList());
        List<BarTableSessionRecord> group = family.stream().map(bars::findTableSessionById).flatMap(Optional::stream).toList();
        group.stream().map(BarTableSessionRecord::openedAt).min(Comparator.naturalOrder()).ifPresent(since ->
                orders.addAll(bars.listOrdersWithoutSession(group.stream().map(BarTableSessionRecord::tableId).distinct().toList(), since)));
        return new Ledger(session.status(), orders, bars.listBillsByOrderIds(orders.stream().map(BarOrderRecord::id).toList()));
    }

    /** 已结清＝已关台，或已有账单且全部结清（含已合并、已作废）、订单都已开单。 */
    private record Ledger(String status, List<BarOrderRecord> orders, List<BarBillRecord> bills) {
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
