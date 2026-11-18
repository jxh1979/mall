package com.complex.platform.finance;

// 文件版本：V0.01

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.dividend.DividendRepository;
import com.complex.platform.dividend.DividendService;
import com.complex.platform.inventory.InventoryRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第十八批 AR 段第 233 条 233-R6：按主体汇总所属期的收入、成本与利润。只读、不建表，由「主体收入成本利润」页签与 233-R7 企业所得税系统计算现算。
 * 依据 BSM:1760「娱乐平台公司、游乐公司、传媒公司、中央餐饮公司、城市项目公司五家主体各自的收入、成本与利润，由平台汇总计算；其中涉税的正式核算数据由专业
 * 财税软件回传关键信息，平台据此与自身的内部结算数据合并计算」；第十八批裁决 60「成本＝该主体在平台内作为付款方的内部结算应付（提成拨付、渠道费、赠送权益
 * 兑付等）＋该主体作为发放主体的工资单税前应发；利润＝收入−成本……酒吧经营主体沿用持股分红的税前口径（收入−销货成本−费用）」。
 *
 * <p>收入取 233-R4 各月合计（{@link EntityIncomeService}，数据未定照样 409）；口径分流同 233-R4（名下有酒吧商户的按持股分红口径）。平台主体的成本中
 * 与收入同源的项取同一批已确认或已结清的月度分账单（台账 accounting_period 是付账日所在月，会和收入错开一个月）：池内分配付方取同单酒吧份额行的归属主体、
 * 专属直付按内部结算价应直付额与渠道费（通路费与销售渠道费，BSM:749）计份额行归属主体、赠送权益兑付实付计单头赠送权益收款主体；另取游乐故障补偿履约
 * （承担主体）、提成与报酬拨付冲回（平台主体付款的应收）、传媒演出门票分成（付方）、积分发行费（合同销售方自身成本，BSM:749）与发放的工资单税前应发。
 * 代收后转付的域间份额、到期未核销归属、单店直付及其追收与外部采购不计。酒吧经营主体逐店取销货成本（扣坏账订单出库成本）、费用台账六类与单一酒吧门票综合服务费
 * （同 DividendService.computeStatement），不扣税费。</p>
 */
@Service
public class EntityProfitService {

    /** 收入分项：incomeType 同 EntityTaxService.INCOME_LABELS。 */
    public record RevenueItem(String incomeType, long amountCent) {
    }

    /** 成本分项：costType 平台主体为下列各项，酒吧经营主体同持股分红明细行类型（cost_inventory、expense_*）。 */
    public record CostItem(String costType, long amountCent) {
    }

    /** basis：platform 平台主体口径，dividend 酒吧经营主体持股分红税前口径。金额为 0 的分项不列。 */
    public record Profit(long revenueCent, List<RevenueItem> revenueItems, long costCent, List<CostItem> costItems, long profitCent, String basis) {
    }

    private static final String BILLS = " JOIN settlement_bills b ON b.id = s.bill_id WHERE b.status IN ('confirmed', 'settled')"
            + " AND b.period_start BETWEEN ? AND ?";

    private final JdbcTemplate jdbc;
    private final EntityIncomeService incomes;
    private final BusinessDayResolver businessDays;
    private final InventoryRepository inventory;
    private final DividendRepository dividends;

    public EntityProfitService(JdbcTemplate jdbc, EntityIncomeService incomes, BusinessDayResolver businessDays, InventoryRepository inventory,
            DividendRepository dividends) {
        this.jdbc = jdbc;
        this.incomes = incomes;
        this.businessDays = businessDays;
        this.inventory = inventory;
        this.dividends = dividends;
    }

    public Profit profit(String entityId, YearMonth start, YearMonth end) {
        Map<String, Long> revenue = new TreeMap<>();
        List<String> months = new ArrayList<>();
        for (YearMonth m = start; !m.isAfter(end); m = m.plusMonths(1)) {
            months.add(m.toString());
            for (var item : incomes.income(entityId, m)) revenue.merge(item.incomeType(), item.amountCent(), Math::addExact);
        }
        Map<String, Long> cost = new LinkedHashMap<>();
        List<Map<String, Object>> bars = incomes.bars(entityId);
        if (bars.isEmpty()) {
            platformCost(entityId, start, end, cost);
        } else {
            for (Map<String, Object> bar : bars) barCost((String) bar.get("id"), start.atDay(1), end.atEndOfMonth(), months, cost);
        }
        List<RevenueItem> revenueItems = revenue.entrySet().stream().filter(e -> e.getValue() != 0)
                .map(e -> new RevenueItem(e.getKey(), e.getValue())).toList();
        List<CostItem> costItems = cost.entrySet().stream().filter(e -> e.getValue() != 0).map(e -> new CostItem(e.getKey(), e.getValue())).toList();
        long revenueCent = revenueItems.stream().mapToLong(RevenueItem::amountCent).reduce(0L, Math::addExact);
        long costCent = costItems.stream().mapToLong(CostItem::amountCent).reduce(0L, Math::addExact);
        return new Profit(revenueCent, revenueItems, costCent, costItems, Math.subtractExact(revenueCent, costCent), bars.isEmpty() ? "platform" : "dividend");
    }

    /** 平台主体按第十八批裁决 60 原文：作为付款方的内部结算应付＋作为发放主体的工资单税前应发。 */
    private void platformCost(String entityId, YearMonth start, YearMonth end, Map<String, Long> cost) {
        LocalDate first = start.atDay(1);
        LocalDate lastBill = end.atDay(1);
        LocalDate last = end.atEndOfMonth();
        String from = start.toString();
        String to = end.toString();
        // ①工资单税前应发：小费与服务费应发属代收代付不计（同 210-R1 JdbcMerchantExpenseRepository 工资单月结人工成本）。
        cost.put("payroll", sum("SELECT COALESCE(SUM(total_cent + tax_cent - tip_cent - service_fee_cent), 0) FROM staff_payroll_statements"
                + " WHERE payer_company_id = ? AND month BETWEEN ? AND ?", entityId, from, to));
        // ②通票分账池内分配（含超池补贴、分池追收与续算）：付方取同单酒吧份额行的归属主体（月结定格；ENUM:32「票款由娱乐平台公司统一收款」）。
        //   单店直付 exclusive_full 及其追收 exclusive_recovery 不计（BSM:1713「娱乐平台公司不取得已核销门票的收入」；组合通票专属直付在③按份额行计）。
        cost.put("pool_distribution", sum("SELECT COALESCE(SUM(i.final_amount_cent), 0) FROM settlement_items i"
                + " JOIN settlement_bill_format_shares s ON s.bill_id = i.bill_id AND s.business_format = 'bar'" + BILLS
                + " AND i.distribution_method NOT IN ('exclusive_full', 'exclusive_recovery') AND s.beneficiary_kind = 'entity' AND s.beneficiary_entity_id = ?",
                first, lastBill, entityId));
        // ③专属直付按内部结算价应直付额（V606 列注释「为负由归属主体承担」）与④渠道费（通路费与销售渠道费，BSM:749、DEC-R3-2026-08-04-08）。
        jdbc.query("SELECT COALESCE(SUM(s.internal_settled_cent + s.recovery_internal_settled_cent), 0) AS exclusive,"
                        + " COALESCE(SUM(s.channel_fee_cent + s.route_fee_cent), 0) AS channel FROM settlement_bill_format_shares s" + BILLS
                        + " AND s.beneficiary_kind = 'entity' AND s.beneficiary_entity_id = ?",
                rs -> {
                    cost.put("exclusive_direct_settlement", rs.getLong("exclusive"));
                    cost.put("channel_fee", rs.getLong("channel"));
                }, first, lastBill, entityId);
        //   跨月撤销核销：撤销所在月按同一原月两次重算之差冲减渠道费，计原月结算单该业态份额行的归属主体（同收入 a2 一批重算，收支同冲；决策台账 FS-021
        //   「单独列追收、按原月重算」，BSM:1760「已封账历史账单不回改，跨月退款和调整在当期生成冲正」）。
        for (YearMonth m = start; !m.isAfter(end); m = m.plusMonths(1)) {
            incomes.recoveryDiffs(m).forEach((bill, diffs) -> jdbc.query("SELECT business_format FROM settlement_bill_format_shares WHERE bill_id = ?"
                            + " AND beneficiary_kind = 'entity' AND beneficiary_entity_id = ?",
                    rs -> {
                        cost.merge("channel_fee", diffs.getOrDefault("channel:" + rs.getString("business_format"), 0L), Math::addExact);
                    }, bill, entityId));
        }
        // ⑤赠送权益兑付实付（V377，计单头赠送权益收款主体）与游乐故障补偿履约（应付减撤销追收，计承担主体）。
        cost.put("gift_redemption", sum("SELECT COALESCE(SUM(s.gift_actual_paid_cent), 0) FROM settlement_bill_format_shares s" + BILLS
                + " AND b.gift_beneficiary_entity_id = ?", first, lastBill, entityId));
        cost.put("compensation_fulfillment", sum("SELECT COALESCE(SUM(CASE WHEN i.direction = 'payable' THEN i.amount_cent ELSE -i.amount_cent END), 0)"
                + " FROM ar_ap_items i JOIN amusement_compensation_fulfillments f"
                + " ON (i.direction = 'payable' AND i.source_type = 'compensation_fulfillment' AND i.source_id = f.id)"
                + " OR (i.direction = 'receivable' AND i.source_type = 'compensation_fulfillment_recovery' AND i.source_id = f.reversal_item_id)"
                + " WHERE f.bearer_company_id = ? AND i.accounting_period BETWEEN ? AND ?", entityId, from, to));
        // ⑥提成与报酬拨付冲回：平台主体付款的应收（往来方＝本主体，按工资月）。
        cost.put("commission_transfer_reversal", sum("SELECT COALESCE(SUM(amount_cent), 0) FROM ar_ap_items WHERE direction = 'receivable'"
                + " AND source_type IN ('dispatch_commission_transfer', 'bar_payroll_transfer') AND counterparty_id = ? AND accounting_period BETWEEN ? AND ?",
                entityId, from, to));
        // ⑦传媒演出门票分成：付方为本主体（all_pass_ticket 归属），同收入 d 按应收应付台账出账营业日（应付到期日）取月：原应付留在原月，跨月调整的冲正行
        //   （来源回指原结算行）与新应付都落调整当日（BSM:1760「跨月退款和调整在当期生成冲正」）。
        cost.put("media_ticket_share", sum("SELECT COALESCE(SUM(i.amount_cent), 0) FROM ar_ap_items i JOIN media_performance_settlements s ON s.id = i.source_id"
                + " WHERE i.direction = 'payable' AND i.source_type = 'media_performance_settlement' AND s.fee_kind = 'ticket_share'"
                + " AND s.payer_entity_id = ? AND i.due_date BETWEEN ? AND ?", entityId, first, last));
        // ⑧积分发行费：本主体是合同销售方的计费月应收（BSM:749「作为合同销售方自身成本单独列支」）。
        cost.put("points_issuance", sum("SELECT COALESCE(SUM(amount_cent), 0) FROM ar_ap_items WHERE direction = 'receivable'"
                + " AND source_type = 'ticket_points_issuance' AND counterparty_id = ? AND accounting_period BETWEEN ? AND ?", entityId, from, to));
    }

    /** 酒吧经营主体逐店：同持股分红正式单的销货成本与费用（DividendService.computeStatement），营业日窗同 233-R4。 */
    private void barCost(String merchantId, LocalDate first, LocalDate last, List<String> months, Map<String, Long> cost) {
        String bar = businessDays.barOfMerchant(merchantId);
        String project = bar == null ? businessDays.projectOfMerchant(merchantId) : null;
        Instant from = businessDays.start(project, bar, first);
        Instant to = businessDays.end(project, bar, last);
        long goods = Math.subtractExact(inventory.sumOutboundCost(merchantId, null, first.toString(), last.toString(), from, to).outboundTotalCostCent(),
                inventory.sumBadDebtBarOrderOutboundCost(merchantId, from, to));
        cost.merge("cost_inventory", goods, Math::addExact);
        for (var expense : dividends.sumExpenseByType(merchantId, months, List.copyOf(DividendService.EXPENSE_TYPES))) {
            cost.merge("expense_" + expense.expenseType(), expense.totalCent(), Math::addExact);
        }
        cost.merge("expense_ticket_service_fee", dividends.sumSingleBarServiceFeeCent(merchantId, first, last), Math::addExact);
    }

    private long sum(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }
}
