package com.complex.platform.finance;

// 文件版本：V0.02

import com.complex.platform.bar.BarRevenueSourceService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.dividend.DividendRepository;
import com.complex.platform.settlement.JdbcSettlementTransactionRepository;
import com.complex.platform.settlement.RevenueStreamAttributionRepository;
import com.complex.platform.settlement.SingleBarServiceFeeService;
import com.complex.platform.settlement.TicketDistributionSettlementService;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第十八批 AR 段第 233 条 233-R4：按主体、按月、按收入类型归集含税收入（系统计算金额与利润的收入基数）。只读、不建表、不单出接口，
 * 由 233-R5 登记申报时现算。依据 ENUM:201「每个主体 × 每类收入 × 每种税……」；BSM:1760「五家主体各自的收入、成本与利润，由平台汇总计算……
 * 平台据此与自身的内部结算数据合并计算」；BSM:1713～1723 收入归属表；第十八批裁决 60「收入＝按收入归属配置归到该主体的各类收入（含税实收）……
 * 酒吧经营主体沿用持股分红的税前口径（收入−销货成本−费用）」。
 *
 * <p>数据已定才算：该月没有已确认或已结清的月度分账单一律 409 TICKET_SETTLEMENT_PENDING（谓词同 JdbcDividendRepository
 * .listUnconfirmedTicketSettlementMonths）；酒吧收入来源未定 409 BAR_REVENUE_PENDING（同分红正式单）。</p>
 *
 * <p>口径分流按主体名下有无酒吧商户或名下有游乐项目的游乐商户（经营主体同 210-R1：生效绑定的运营主体，否则商户档案上的经营主体），不按主体角色
 * （V410 后酒吧公司也是 management_company）。有的按持股分红口径逐店取：酒吧取商品收入减已退成商品分项（catering）、月结定格的服务费扣留
 * （nonstd_service，BSM:1719）、已确认月度分账单本店专属直付（ticket）与其余分账（spv_service）；第十八批 CC 段 401-R1（ENUM:297「引进参股的游乐项目
 * 的参股方为引进该项目的域主管公司」；BSM:753「与实际提供服务的游乐商户结算」；按常理比照第十八批裁决 60 对酒吧经营主体的处理）起，名下有游乐项目的
 * 游乐商户（引进参股的游乐项目经营主体）取游乐域内结算收入（game，同持股分红正式单 revenue_amusement）。其余主体按平台口径取归到本主体的部分：组合通票业态份额（扣出归娱乐平台公司的
 * 门票综合服务费与赠送权益预估成本）、门票综合服务费与赠送权益预估成本收款主体快照、到期未核销票款（BSM:1713）、单一酒吧门票综合服务费、
 * 传媒演出结算（BSM:1721、1145）、场地租金（venue_rent 收入归属）、停车费（BSM:1723）、门店要货货款（supply_goods 收入归属）。跨月撤销的核销与跨月调整的
 * 演出结算在当期冲正（BSM:1760「已封账历史账单不回改，跨月退款和调整在当期生成冲正」）。</p>
 */
@Service
public class EntityIncomeService {

    /** 一项收入：incomeType 同 EntityTaxService.INCOME_LABELS；amountCent 含税、可为负（跨月冲正）；source 为来源说明。 */
    public record IncomeItem(String incomeType, long amountCent, String source) {
    }

    /** 份额行与到期未核销按业态定收入类型：bar、other→ticket（ENUM:32 娱乐平台公司只把归属酒吧域的份额确认为自身收入），amusement→game，media→show。 */
    private static final String FORMAT_TYPE = "CASE %s WHEN 'amusement' THEN 'game' WHEN 'media' THEN 'show' WHEN 'catering' THEN 'catering'"
            + " WHEN 'retail' THEN 'retail' ELSE 'ticket' END";
    private static final String CONFIRMED = "b.status IN ('confirmed', 'settled')";

    private final JdbcTemplate jdbc;
    private final BarRevenueSourceService barSources;
    private final BusinessDayResolver businessDays;
    private final RevenueStreamAttributionRepository attribution;
    private final JdbcSettlementTransactionRepository transactions;
    private final SingleBarServiceFeeService singleBarFees;
    private final TicketDistributionSettlementService ticketSettlement;
    private final DividendRepository dividends;

    public EntityIncomeService(JdbcTemplate jdbc, BarRevenueSourceService barSources, BusinessDayResolver businessDays,
            RevenueStreamAttributionRepository attribution, JdbcSettlementTransactionRepository transactions,
            SingleBarServiceFeeService singleBarFees, TicketDistributionSettlementService ticketSettlement, DividendRepository dividends) {
        this.jdbc = jdbc;
        this.barSources = barSources;
        this.businessDays = businessDays;
        this.attribution = attribution;
        this.transactions = transactions;
        this.singleBarFees = singleBarFees;
        this.ticketSettlement = ticketSettlement;
        this.dividends = dividends;
    }

    /** 该主体该月的各项收入（金额为 0 的不列）。 */
    public List<IncomeItem> income(String entityId, YearMonth month) {
        if (!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM settlement_bills b WHERE b.period_start = ? AND " + CONFIRMED + ")",
                Boolean.class, month.atDay(1)))) {
            throw ApiException.conflict("TICKET_SETTLEMENT_PENDING", "票务月度分账（" + month + "）尚未确认，不能计算系统计算金额");
        }
        List<Map<String, Object>> bars = bars(entityId);
        List<IncomeItem> items = new ArrayList<>();
        if (bars.isEmpty()) {
            platform(entityId, month, items);
        } else {
            for (Map<String, Object> bar : bars) {
                String merchantId = (String) bar.get("id");
                String name = (String) bar.get("merchant_name");
                if ("amusement".equals(bar.get("merchant_type"))) {
                    // 401-R1：同持股分红正式单 revenue_amusement（DividendService.computeStatement）取数，营业日落在本月。
                    items.add(new IncomeItem("game", dividends.sumAmusementDomainSettledCent(merchantId, month.atDay(1), month.atEndOfMonth()),
                            name + "：游乐域内结算收入（游乐公司按内部结算价结算，本月核销为正、本月撤销为负）"));
                } else {
                    bar(merchantId, name, month, items);
                }
            }
        }
        return items.stream().filter(item -> item.amountCent() != 0).toList();
    }

    /**
     * 主体名下酒吧商户或名下有游乐项目的游乐商户（id、merchant_name、merchant_type）：经营主体为生效绑定的运营主体，否则商户档案上的经营主体（同 210-R1）；
     * 233-R6 同口径分流。401-R1：游乐商户只收名下有游乐项目的（项目所属商户为空即游乐公司自营，游乐公司为自营项目权益另建的游乐商户档案不算）。
     */
    List<Map<String, Object>> bars(String entityId) {
        return jdbc.queryForList("SELECT m.id, m.merchant_name, m.merchant_type FROM merchants m WHERE (m.merchant_type = 'bar'"
                + " OR (m.merchant_type = 'amusement' AND EXISTS (SELECT 1 FROM amusement_projects p WHERE p.merchant_id = m.id)))"
                + " AND COALESCE((SELECT b.operation_entity_id FROM business_entity_bindings b WHERE b.merchant_id = m.id AND b.status = 'active'"
                + " ORDER BY b.created_at DESC, b.id LIMIT 1), m.business_entity_id) = ? ORDER BY m.id", entityId);
    }

    /** 撤销所在月：各已确认月度分账单所属项目的跨月撤销核销按原月重算之差（原月结算单 id → 差，键见 TicketDistributionSettlementService.uncoveredRecoveryDiffs）。 */
    Map<String, Map<String, Long>> recoveryDiffs(YearMonth month) {
        Map<String, Map<String, Long>> out = new LinkedHashMap<>();
        for (String project : jdbc.queryForList("SELECT DISTINCT b.project_id FROM settlement_bills b WHERE " + CONFIRMED + " AND b.period_start = ?",
                String.class, month.atDay(1))) {
            out.putAll(ticketSettlement.uncoveredRecoveryDiffs(project, month));
        }
        return out;
    }

    /** 酒吧经营主体逐店：同持股分红正式单取数（DividendService.computeStatement、JdbcDividendRepository 非预览口径）。 */
    private void bar(String merchantId, String name, YearMonth month, List<IncomeItem> items) {
        String bar = businessDays.barOfMerchant(merchantId);
        String project = bar == null ? businessDays.projectOfMerchant(merchantId) : null;
        Instant from = businessDays.start(project, bar, month.atDay(1));
        Instant to = businessDays.end(project, bar, month.atEndOfMonth());
        long goods = 0L;
        for (var row : barSources.completedCollections(merchantId, from, to)) {
            if (row.goodsRevenueCent() != null) goods = Math.addExact(goods, row.goodsRevenueCent());
            else if (!"payment_pending".equals(row.status())) throw barPending("酒吧收入来源尚待核实");
        }
        for (var row : barSources.completedGoodsRefunds(merchantId, from, to)) {
            if (row.amountCent() == null) throw barPending("实际退款分项尚待核实");
            goods = Math.subtractExact(goods, row.amountCent());
        }
        long withheld = 0L;
        for (var row : barSources.withheldServiceFees(merchantId, month.atDay(1), month.plusMonths(1).atDay(1))) {
            if (row.amountCent() == null) throw barPending("服务费尚未月结或来源已变化");
            withheld = Math.addExact(withheld, row.amountCent());
        }
        items.add(new IncomeItem("catering", goods, name + "：整单已确认商品收入减已退成商品分项"));
        items.add(new IncomeItem("nonstd_service", withheld, name + "：月结定格的服务费扣留"));
        // 单一酒吧门票直付及其跨月撤销的追收明细（FS-021-R2 exclusive_recovery）同归门票收入，其余（含分池追收与续算）为可分配池分账。
        jdbc.query("SELECT CASE WHEN i.distribution_method IN ('exclusive_full', 'exclusive_recovery') THEN 'ticket' ELSE 'spv_service' END"
                        + " AS income_type,"
                        + " SUM(i.final_amount_cent) AS amount_cent FROM settlement_items i JOIN settlement_bills b ON b.id = i.bill_id"
                        + " WHERE i.merchant_id = ? AND " + CONFIRMED + " AND b.period_start >= ? AND b.period_end <= ? GROUP BY 1 ORDER BY 1",
                rs -> {
                    String type = rs.getString("income_type");
                    items.add(new IncomeItem(type, rs.getLong("amount_cent"),
                            name + ("ticket".equals(type) ? "：已确认月度分账单单一酒吧门票直付额" : "：已确认月度分账单通票可分配池分账")));
                }, merchantId, month.atDay(1), month.atEndOfMonth());
    }

    private void platform(String entityId, YearMonth month, List<IncomeItem> items) {
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();
        // a）已确认月度分账单：份额归属主体计 毛额−门票综合服务费−赠送权益预估成本（V370、V372 两列归娱乐平台公司），两列另按单头收款主体快照计入；
        //   同行并入原月已过账后撤销、在本月追收的域间份额 recovery_net_cent（与成本侧 recovery_internal_settled_cent 同行同单，决策台账 FS-021）。
        jdbc.query("SELECT income_type, src, SUM(amount) AS amount_cent FROM ("
                        + "SELECT " + FORMAT_TYPE.formatted("s.business_format") + " AS income_type, 'share' AS src,"
                        + " s.gross_revenue_cent - s.comp_service_fee_cent - s.gift_cost_cent + s.recovery_net_cent AS amount"
                        + " FROM settlement_bill_format_shares s JOIN settlement_bills b ON b.id = s.bill_id"
                        + " WHERE " + CONFIRMED + " AND b.period_start = ? AND s.beneficiary_kind = 'entity' AND s.beneficiary_entity_id = ?"
                        + " UNION ALL SELECT 'ticket', 'fee', b.comp_service_fee_exclusive_cent"
                        + " + COALESCE((SELECT SUM(s.comp_service_fee_cent) FROM settlement_bill_format_shares s WHERE s.bill_id = b.id), 0)"
                        + " FROM settlement_bills b WHERE " + CONFIRMED + " AND b.period_start = ? AND b.fee_beneficiary_entity_id = ?"
                        + " UNION ALL SELECT 'ticket', 'gift', COALESCE((SELECT SUM(s.gift_cost_cent) FROM settlement_bill_format_shares s"
                        + " WHERE s.bill_id = b.id), 0) FROM settlement_bills b WHERE " + CONFIRMED + " AND b.period_start = ?"
                        + " AND b.gift_beneficiary_entity_id = ?) x GROUP BY income_type, src ORDER BY src, income_type",
                rs -> {
                    String src = rs.getString("src");
                    items.add(new IncomeItem(rs.getString("income_type"), rs.getLong("amount_cent"), "share".equals(src)
                            ? "已确认月度分账单组合通票业态份额（扣出门票综合服务费与赠送权益预估成本，并入域间份额追收）"
                            : "fee".equals(src) ? "已确认月度分账单门票综合服务费" : "已确认月度分账单赠送权益预估成本"));
                }, first, entityId, first, entityId, first, entityId);
        // a2）跨月撤销核销（决策台账 FS-021「单独列追收、按原月重算」；BSM:1760「已封账历史账单不回改，跨月退款和调整在当期生成冲正」）：与 a) 原月计入同一
        //   口径冲减。份额行追收 recovery_net_cent 只记域间份额（已并入 a），其余（酒吧份额、游乐传媒自收票、已扣的销售渠道费与通路费）连同门票综合服务费、
        //   赠送权益预估成本，按月结引擎同一原月两次重算之差在撤销所在月冲减，计原月结算单该业态份额行的归属主体与单头两个收款主体快照。
        recoveryDiffs(month).forEach((bill, diffs) -> jdbc.query(
                "SELECT s.business_format AS k, s.beneficiary_entity_id AS owner, " + FORMAT_TYPE.formatted("s.business_format") + " AS income_type"
                        + " FROM settlement_bill_format_shares s WHERE s.bill_id = ? AND s.beneficiary_kind = 'entity'"
                        + " UNION ALL SELECT 'fee', b.fee_beneficiary_entity_id, 'ticket' FROM settlement_bills b WHERE b.id = ?"
                        + " UNION ALL SELECT 'gift', b.gift_beneficiary_entity_id, 'ticket' FROM settlement_bills b WHERE b.id = ?",
                rs -> {
                    Long amount = diffs.get(rs.getString("k"));
                    if (amount != null && entityId.equals(rs.getString("owner"))) {
                        items.add(new IncomeItem(rs.getString("income_type"), amount, "跨月撤销核销按原月重算之差在当期冲减"));
                    }
                }, bill, bill, bill));
        // 同该月月结的月窗：按唯一启用综合体切营业日（TicketDistributionSettlementService.monthWindow 平台单口径）。
        String dayProject = businessDays.soleProject();
        Timestamp from = Timestamp.from(businessDays.start(dayProject, null, first));
        Timestamp to = Timestamp.from(businessDays.end(dayProject, null, last));
        // b）到期未核销票款：到期作业落行时受益主体已定（BSM:1713），扣到期作业已扣的通路费与销售渠道费（同 aggregateUnverifiedNetByFormat）。
        jdbc.query("SELECT " + FORMAT_TYPE.formatted("COALESCE(m.merchant_type, 'bar')") + " AS income_type,"
                        + " SUM(st.unverified_amount_cent - st.route_fee_cent - st.channel_fee_cent - CASE WHEN st.channel_fee_residual_entity_id"
                        + " = st.beneficiary_entity_id THEN st.channel_fee_residual_cent ELSE 0 END) AS amount_cent"
                        + " FROM settlement_transactions st LEFT JOIN merchants m ON m.id = st.merchant_id"
                        + " WHERE st.transaction_type = 'unverified_expired' AND st.beneficiary_entity_id = ?"
                        + " AND COALESCE(st.business_at, st.created_at) >= ? AND COALESCE(st.business_at, st.created_at) < ? GROUP BY 1 ORDER BY 1",
                rs -> {
                    items.add(new IncomeItem(rs.getString("income_type"), rs.getLong("amount_cent"), "到期未核销票款（扣通路费与销售渠道费）"));
                }, entityId, from, to);
        // c）单一酒吧门票综合服务费：按售出月单独计收（FS-013），按该项目 ticket_service_fee 收入归属定主体。
        long singleBar = 0L;
        for (var total : singleBarFees.monthTotals(first)) {
            if (entityId.equals(attribution.resolveManagementEntity(total.projectId(), "ticket_service_fee"))) {
                singleBar = Math.addExact(singleBar, total.amountCent());
            }
        }
        items.add(new IncomeItem("ticket", singleBar, "单一酒吧门票综合服务费"));
        // d）传媒演出结算：收方一律传媒公司，按应收应付台账出账营业日（应付到期日）取月；原应付留在原月，跨月调整的冲正行与新应付都落调整当日，当期计新额−原额
        //   （BSM:1760「跨月退款和调整在当期生成冲正」；BSM:1145「关联原记录并按既有历史调整规则办理，不覆盖原结果」）。
        items.add(new IncomeItem("show", jdbc.queryForObject("SELECT COALESCE(SUM(amount_cent), 0) FROM ar_ap_items WHERE direction = 'payable'"
                + " AND source_type = 'media_performance_settlement' AND counterparty_id = ? AND due_date BETWEEN ? AND ?",
                Long.class, entityId, first, last), "传媒演出结算"));
        // e）场地租金：合同租金与营业额抽成租金的当月收款减营业额抽成冲正的当月付款，按该项目 venue_rent 收入归属定主体（未配置的不计）。
        items.add(new IncomeItem("rent", owned(project -> entityId.equals(attribution.resolveManagementEntity(project, "venue_rent")),
                "SELECT i.project_id, SUM(CASE WHEN i.direction = 'receivable' THEN p.amount_cent ELSE -p.amount_cent END) AS amount_cent"
                        + " FROM ar_ap_payments p JOIN ar_ap_items i ON i.id = p.ar_ap_item_id"
                        + " WHERE ((i.direction = 'receivable' AND i.source_type IN ('contract_rent', 'turnover_rent'))"
                        + " OR (i.direction = 'payable' AND i.source_type = 'turnover_rent_reversal'))"
                        + " AND p.payment_time >= ? AND p.payment_time < ? GROUP BY i.project_id", from, to), "场地租金当月收款"));
        // f）停车费：缴费时已定格收款主体；车位月租的当月收款与商户承担的停车抵免成本按该项目唯一城市项目公司计（BSM:1723）。
        Predicate<String> projectCompany = project -> transactions.findProjectCompanies(project).equals(List.of(entityId));
        long parking = jdbc.queryForObject("SELECT COALESCE(SUM(amount_cent), 0) FROM parking_payments WHERE status = 'paid'"
                + " AND receiver_entity_id = ? AND paid_at >= ? AND paid_at < ?", Long.class, entityId, from, to);
        parking = Math.addExact(parking, owned(projectCompany, "SELECT i.project_id, SUM(p.amount_cent) AS amount_cent FROM ar_ap_payments p"
                + " JOIN ar_ap_items i ON i.id = p.ar_ap_item_id WHERE i.direction = 'receivable' AND i.source_type = 'contract_parking_monthly'"
                + " AND p.payment_time >= ? AND p.payment_time < ? GROUP BY i.project_id", from, to));
        parking = Math.addExact(parking, owned(projectCompany, "SELECT project_id, SUM(amount_cent) AS amount_cent FROM ar_ap_items"
                + " WHERE direction = 'receivable' AND source_type = 'parking_credit_cost' AND accounting_period = ? GROUP BY project_id", month.toString()));
        items.add(new IncomeItem("parking", parking, "停车费（停车缴费、车位月租收款、商户承担的停车抵免成本）"));
        // g）供货货款：只取门店要货（transfer_type='supply'）应付的当月付款；借调买断同来源、出让方是商户，不计。货款调整的重述行不写来源记录，沿 origin_item_id
        //   红冲链回到签收时那笔根行判来源（BSM:1212「货款调整……不重复收款」）。按该项目 supply_goods 收入归属定主体。
        items.add(new IncomeItem("goods", owned(project -> entityId.equals(attribution.resolveManagementEntity(project, "supply_goods")),
                "WITH RECURSIVE chain (id, root_id) AS (SELECT id, id FROM ar_ap_items WHERE source_type = 'inventory_transfer' AND origin_item_id IS NULL"
                        + " UNION ALL SELECT a.id, c.root_id FROM ar_ap_items a JOIN chain c ON a.origin_item_id = c.id)"
                        + " SELECT i.project_id, SUM(p.amount_cent) AS amount_cent FROM ar_ap_payments p JOIN ar_ap_items i ON i.id = p.ar_ap_item_id"
                        + " JOIN chain c ON c.id = i.id"
                        + " WHERE i.direction = 'payable' AND i.source_type = 'inventory_transfer' AND p.payment_time >= ? AND p.payment_time < ?"
                        + " AND EXISTS (SELECT 1 FROM ar_ap_item_sources src JOIN inventory_transfer_orders o ON o.id = src.source_id"
                        + " WHERE src.ar_ap_item_id = c.root_id AND src.source_type = 'inventory_transfer' AND o.transfer_type = 'supply')"
                        + " GROUP BY i.project_id", from, to), "门店要货货款当月付款"));
    }

    /** 按项目汇总的金额里，项目归本主体的部分（sql 返回 project_id、amount_cent 两列）。 */
    private long owned(Predicate<String> ownsProject, String sql, Object... args) {
        long total = 0L;
        for (Map<String, Object> row : jdbc.queryForList(sql, args)) {
            if (ownsProject.test((String) row.get("project_id"))) {
                total = Math.addExact(total, ((Number) row.get("amount_cent")).longValue());
            }
        }
        return total;
    }

    private static ApiException barPending(String message) {
        return ApiException.conflict("BAR_REVENUE_PENDING", message + "，不能计算系统计算金额");
    }
}
