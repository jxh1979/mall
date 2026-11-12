package com.complex.platform.settlement;

// 文件版本：V0.02

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.ticketing.TicketRewardFacts;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * FS-013 单一酒吧门票综合服务费（BSM L755 / ENUM L57、BSM L804；裁决 Q33/Q33A；操作员裁决 2026-10-19 第 3 条
 * 「售出月，只退款剔除：按客人付款成功（售出）所在月计入；之后只有整张退款成功才从原月剔除重算；到期没用的票照常计费」）。
 * <ol>
 *   <li>出票即逐票锁定（与出票同一事务）：计费基数＝订单客人实付按张均分（「该张票订单上保存的最终选中价」，不取权益分摊、
 *       内部结算价，也不先扣渠道费用），费率或固定额与规则 id、逐票应计费；计费月＝付款成功时刻所属该酒吧营业日的月
 *       （BSM:1682「全系统的钱账统一按「营业日」归集」「酒吧仍按每家酒吧配置」，第十八批 AI 段 213-R1）。
 *       同一项目×酒吧×计费月首张票写入时锁定该月封顶（「该月原锁定合同约定的月度封顶」），之后改配置不影响原月。</li>
 *   <li>月应收总额＝min(Σ未退票逐票费, 锁定封顶)，未设封顶按汇总额；月结过账时单独挂酒吧应收（见
 *       {@link SettlementPostingService#postBill}），不从票款应付里扣（「按总额收付、收支两条线，不得轧差」）。</li>
 *   <li>整张退款成功（与确认拨付同一事务）：回原计费月重算；按台账已有原应收、历次调整与应退核出「已登记总额」，
 *       只登记未被覆盖的差额——先调减原应收未收部分（V344 原行不动＋冲正行＋重述行），超出部分另列应退；
 *       调整记在当前账期（当前时刻所属该酒吧营业日的月，与计费月同一口径）、关联原计费月，不动当前计费月封顶。</li>
 * </ol>
 */
@Service
public class SingleBarServiceFeeService {

    static final String SOURCE_TYPE = "single_bar_service_fee";

    private final JdbcTemplate jdbc;
    private final JdbcSettlementBillRepository bills;
    private final FinanceRepository finance;
    private final InternalPayDayService payDays;
    private final BusinessDayResolver businessDays;

    public SingleBarServiceFeeService(JdbcTemplate jdbc, JdbcSettlementBillRepository bills, FinanceRepository finance,
                                      InternalPayDayService payDays, BusinessDayResolver businessDays) {
        this.jdbc = jdbc;
        this.bills = bills;
        this.finance = finance;
        this.payDays = payDays;
        this.businessDays = businessDays;
    }

    /** 时刻 at 所属该酒吧营业日的月（与 TicketOrderService.saleBusinessDate 同口径）：售出计费月与退款调整账期共用。 */
    YearMonth businessMonth(String projectId, String barId, Instant at) {
        return YearMonth.from(businessDays.merchantBusinessDate(projectId, barId, at));
    }

    /** 未挂应收的计费月（过账用）；项目为该计费月锁定的售票项目。 */
    record UnpostedMonth(String id, String projectId, String barMerchantId) {
    }

    @EventListener
    public void onIssued(TicketRewardFacts.Issued fact) {
        Map<String, Object> order = jdbc.queryForList(
                "SELECT project_id, bar_id, ticket_type_id, amount_cent, paid_at FROM ticket_orders WHERE id=?",
                fact.orderId()).stream().findFirst().orElse(null);
        if (order == null || order.get("bar_id") == null) {
            return; // 只有单一酒吧门票（票种挂酒吧）走本条；通票酒吧部分仍按原不封顶逐票规则。
        }
        List<String> tickets = jdbc.queryForList(
                "SELECT id FROM ticket_instances WHERE order_id=? ORDER BY seq_in_order, id", String.class, fact.orderId());
        if (tickets.isEmpty()) {
            return;
        }
        String project = (String) order.get("project_id");
        String bar = (String) order.get("bar_id");
        LocalDate month = businessMonth(project, bar, ((Timestamp) order.get("paid_at")).toInstant()).atDay(1);
        var cfg = bills.findComprehensiveServiceFee(project, bar, (String) order.get("ticket_type_id"));
        String monthId = "sbfm_" + UUID.nameUUIDFromBytes((project + "|" + bar + "|" + month).getBytes(StandardCharsets.UTF_8));
        jdbc.update("""
                INSERT INTO single_bar_service_fee_months(id, project_id, bar_merchant_id, billing_month, cap_cent)
                VALUES (?, ?, ?, ?, ?) ON CONFLICT (id) DO NOTHING
                """, monthId, project, bar, month, cfg.monthlyCapAmountCent());
        // 与退款按票均分同一算法（整数均分，前 remainder 张各多 1 分），合计恒等于订单实付。
        long amount = ((Number) order.get("amount_cent")).longValue();
        long base = amount / tickets.size();
        long remainder = amount - base * tickets.size();
        for (int i = 0; i < tickets.size(); i++) {
            long ticketBase = base + (i < remainder ? 1 : 0);
            jdbc.update("""
                    INSERT INTO single_bar_service_fee_tickets(parent_ticket_id, order_id, month_id, base_cent, config_id,
                        fee_bps, fixed_amount_cent, fee_cent) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (parent_ticket_id) DO NOTHING
                    """, tickets.get(i), fact.orderId(), monthId, ticketBase, cfg.id(),
                    cfg.fixedAmountCent() == null ? cfg.feeBps() : null, cfg.fixedAmountCent(),
                    ComprehensiveServiceFee.perTicketCent(cfg, ticketBase, 1));
        }
    }

    /** 该票是否已在售出时锁定服务费（是则核销时不再另抽，见 {@link SettlementSettledConsumer}）。 */
    boolean accruedAtSale(String parentTicketId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM single_bar_service_fee_tickets WHERE parent_ticket_id=?)",
                Boolean.class, parentTicketId));
    }

    /**
     * 该账单覆盖的项目×月份尚未挂应收的计费月（行锁，与退款重算串行）。生产月结出的是平台单（project_id 为空），与交易窗口同口径
     * 覆盖全部项目（{@code :project_id IS NULL OR project_id = :project_id}）。
     */
    List<UnpostedMonth> lockUnpostedMonths(String billId) {
        return jdbc.query("""
                SELECT m.id, m.project_id, m.bar_merchant_id FROM single_bar_service_fee_months m
                  JOIN settlement_bills b ON b.id = ?
                 WHERE (b.project_id IS NULL OR m.project_id = b.project_id) AND m.billing_month = b.period_start
                   AND m.ar_ap_item_id IS NULL
                 ORDER BY m.id FOR UPDATE OF m
                """, (rs, i) -> new UnpostedMonth(rs.getString("id"), rs.getString("project_id"), rs.getString("bar_merchant_id")),
                billId);
    }

    void markPosted(String monthId, String arApItemId) {
        jdbc.update("UPDATE single_bar_service_fee_months SET ar_ap_item_id=? WHERE id=?", arApItemId, monthId);
    }

    /** 月应收总额＝min(Σ未退票逐票费, 锁定封顶)；未设封顶按汇总额。 */
    long monthTotal(String monthId) {
        return jdbc.queryForObject("""
                SELECT CASE WHEN m.cap_cent IS NULL THEN s.total ELSE LEAST(s.total, m.cap_cent) END
                  FROM single_bar_service_fee_months m,
                       LATERAL (SELECT COALESCE(SUM(fee_cent), 0) AS total FROM single_bar_service_fee_tickets
                                 WHERE month_id = m.id AND refund_id IS NULL) s
                 WHERE m.id = ?
                """, Long.class, monthId);
    }

    /** FS-015 分红费用口径（BSM L1774 分红基数扣门票综合服务费）：该酒吧计费月落在 [from, to] 的各月应收总额之和。 */
    public long barTotal(String barMerchantId, LocalDate from, LocalDate to) {
        return jdbc.queryForList("SELECT id FROM single_bar_service_fee_months WHERE bar_merchant_id=? AND billing_month BETWEEN ? AND ?",
                String.class, barMerchantId, from, to).stream().mapToLong(this::monthTotal).sum();
    }

    @EventListener
    public void onRefunded(TicketRewardFacts.Refunded fact) {
        List<Map<String, Object>> tickets = jdbc.queryForList("""
                SELECT DISTINCT t.parent_ticket_id, t.month_id FROM ticket_refund_lines l
                  JOIN issued_entitlements e ON e.id = l.entitlement_id
                  JOIN single_bar_service_fee_tickets t ON t.parent_ticket_id = e.parent_ticket_id
                 WHERE l.refund_id = ?
                """, fact.refundId());
        if (tickets.isEmpty()) {
            return;
        }
        // 一笔退款只属一张订单 → 只属一个计费月。
        String monthId = (String) tickets.get(0).get("month_id");
        Map<String, Object> month = jdbc.queryForMap(
                "SELECT project_id, bar_merchant_id, ar_ap_item_id FROM single_bar_service_fee_months WHERE id=? FOR UPDATE", monthId);
        if (Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM single_bar_service_fee_adjustments WHERE refund_id=?)", Boolean.class, fact.refundId()))) {
            return; // 同一退款事实重放不重复登记。
        }
        boolean posted = month.get("ar_ap_item_id") != null;
        long before = posted ? registeredTotal(monthId) : monthTotal(monthId);
        for (Map<String, Object> t : tickets) {
            jdbc.update("UPDATE single_bar_service_fee_tickets SET refund_id=?, refunded_at=now() WHERE parent_ticket_id=? AND refund_id IS NULL",
                    fact.refundId(), t.get("parent_ticket_id"));
        }
        long after = monthTotal(monthId);
        Instant now = Instant.now();
        YearMonth periodMonth = businessMonth((String) month.get("project_id"), (String) month.get("bar_merchant_id"), now);
        String period = periodMonth.toString();
        long cut = 0L;
        long due = 0L;
        String reversalId = null;
        String restatedId = null;
        String dueId = null;
        if (posted && after < before) {
            // 未收部分调整为重算后的待收金额；已多收的差额列为应退，实际退回成功后才结清（登记付款走台账既有规则）。
            ArApItemRecord current = currentReceivable(monthId);
            cut = current == null ? 0L : Math.min(before - after, current.unpaidAmountCent());
            due = before - after - cut;
            if (cut > 0L) {
                reversalId = "arap_sbf_cz_" + fact.refundId();
                finance.appendArApItem(new ArApItemRecord(reversalId, "SBF-CZ-" + fact.refundId(), current.direction(),
                        current.counterpartyId(), SOURCE_TYPE, monthId, -current.amountCent(), -current.paidAmountCent(),
                        -current.unpaidAmountCent(), current.currency(), current.dueDate(), period, "reversed", now, now,
                        current.merchantId(), current.projectId(), current.id(), null));
                long restated = current.amountCent() - cut;
                if (restated > 0L) {
                    long unpaid = current.unpaidAmountCent() - cut;
                    restatedId = "arap_sbf_bl_" + fact.refundId();
                    finance.appendArApItem(new ArApItemRecord(restatedId, "SBF-BL-" + fact.refundId(), current.direction(),
                            current.counterpartyId(), SOURCE_TYPE, monthId, restated, current.paidAmountCent(), unpaid,
                            current.currency(), current.dueDate(), period,
                            unpaid == 0L ? "settled" : current.paidAmountCent() > 0L ? "partial_paid" : "pending_collection",
                            now, now, current.merchantId(), current.projectId(), current.id(), null));
                }
            }
            if (due > 0L) {
                dueId = "arap_sbf_tk_" + fact.refundId();
                // FS-021-R4：应退同走三个付账日——按当前账期推算财务指定付账日并固化（不再填「今天+30 天」）。
                var schedule = payDays.schedule(SOURCE_TYPE, (String) month.get("project_id"),
                        periodMonth.atDay(1), now);
                ArApItemRecord dueItem = new ArApItemRecord(dueId, "SBF-TK-" + fact.refundId(), "payable",
                        (String) month.get("bar_merchant_id"), SOURCE_TYPE, fact.refundId(), due, 0L, due, "CNY",
                        schedule.dueDate(), period, "pending_payment", now, now,
                        null, (String) month.get("project_id"));
                finance.appendArApItem(dueItem);
                payDays.record(dueItem, schedule, (String) month.get("project_id"));
            }
        }
        jdbc.update("""
                INSERT INTO single_bar_service_fee_adjustments(refund_id, month_id, accounting_period, before_cent, after_cent,
                    pending_cut_cent, refund_due_cent, reversal_item_id, restated_item_id, refund_due_item_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, fact.refundId(), monthId, period, before, after, cut, due, reversalId, restatedId, dueId);
    }

    /** 台账已登记的该月总额：原应收（含冲正/重述）净额 − 历次应退。 */
    private long registeredTotal(String monthId) {
        return jdbc.queryForObject("""
                SELECT COALESCE((SELECT SUM(amount_cent) FROM ar_ap_items
                                  WHERE direction='receivable' AND source_type=? AND source_id=?), 0)
                     - COALESCE((SELECT SUM(i.amount_cent) FROM ar_ap_items i
                                   JOIN single_bar_service_fee_adjustments a ON a.refund_due_item_id = i.id
                                  WHERE a.month_id=?), 0)
                """, Long.class, SOURCE_TYPE, monthId, monthId);
    }

    /** 当前有效的原月应收（未被冲正/重述回指的正额行），行锁。 */
    private ArApItemRecord currentReceivable(String monthId) {
        return jdbc.queryForList("""
                SELECT a.id FROM ar_ap_items a
                 WHERE a.direction='receivable' AND a.source_type=? AND a.source_id=? AND a.amount_cent > 0
                   AND NOT EXISTS (SELECT 1 FROM ar_ap_items c WHERE c.origin_item_id = a.id)
                """, String.class, SOURCE_TYPE, monthId).stream().findFirst()
                .flatMap(finance::findArApItemByIdForUpdate).orElse(null);
    }
}
