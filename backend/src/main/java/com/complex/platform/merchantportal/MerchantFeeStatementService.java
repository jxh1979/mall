package com.complex.platform.merchantportal;

// 文件版本：V0.04

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.finance.FinanceService;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 18E 第 21 条 商户专区费用总表与当期综合账单（BSM:1323「商户专区须设一张费用总表，列明平台向本商户收取的全部费用名目、计费基数、承担方与本期金额……
 * 系统须向商户出具一张综合账单，把当期各名目的费用汇总列示，使其一次看清全部应付」；决策台账 DEC-R4-2026-08-04-58）。只读，不新建收费：
 * 金额只取应收应付台账里已出账的应收；有付款时点固化的普通内部结算往来到出账日（visible_at，与门票分账、异议截止日同一时刻；BSM:1597、BSM:1673、DEC-R8-55）才列入。
 * 本期＝计费自然月（BSM:1673「自然月计费」、BSM:1688）：内部应付计划的计费月 → 物业费计费单的期 →
 * 台账账期 → 到期日所在月，一律取冲正链最早那张原行的（冲正行、重述行沿 origin_item_id 一直追到原行），冲正与重述跟原行同月、计入净额；
 * 门票综合服务费付清后退票只挂的应退（payable）按所属计费月（single_bar_service_fee_adjustments → 计费月）从该名目净额扣除（BSM:755）。本商户＝往来对方是本商户（积分发行费往来对方是销售主体，按 merchant_id）；
 * merchant_id 只是账本归属，派驻提成拨付等别人欠的款不算本商户应付。商户范围 fail-closed：无商户归属 403 MERCHANT_SCOPE_REQUIRED。
 */
@Service
public class MerchantFeeStatementService {

    private static final String VIEW = "merchant.billing.view";

    public record FeeItem(String feeName, String billingBasis, String bearer, Long amountCent) {
    }

    public record BillLine(String feeName, long amountCent, long paidAmountCent, long unpaidAmountCent) {
    }

    public record Bill(List<BillLine> lines, long totalAmountCent, long paidAmountCent, long unpaidAmountCent) {
    }

    public record FeeStatement(String month, String merchantId, String merchantName, List<FeeItem> feeItems, Bill bill) {
    }

    /** 第 92 条（BSM:1597 首页未读账单提醒）：某计费自然月综合账单里商户还没看过的台账行笔数。 */
    public record UnreadMonth(String month, long unreadCount) {
    }

    /** 名目、计费基数（只抄 HTML 原句，HTML 没写的留空）、有没有出账路径（没有的本期金额为空，页面显示「本期未出账」）。 */
    private record FeeName(String name, String basis, boolean billed) {
    }

    private static final String FIXED_RENT = "固定租金";
    private static final String PER_SQM_RENT = "每平米租金";
    private static final String TURNOVER_RENT = "营业额抽成租金";

    /** ENUM:256 十二项，加门票综合服务费（BSM:1713、BSM:755）、停车抵免成本（BSM:1445）。 */
    private static final List<FeeName> FEE_NAMES = List.of(
            // BSM:1347「约定租金/账单类型（固定租金、每平米租金、营业额抽成租金、物业费、能源费、押金、违约金）」
            new FeeName(FIXED_RENT, "按合同约定", true),
            new FeeName(PER_SQM_RENT, "按合同约定", true),
            // ENUM:257
            new FeeName(TURNOVER_RENT, "营业额＝该商户当期在本综合体内含税实收成交总额，减去当期已成功的退款金额，减去由该商户以外的出资方承担的赠送与补贴；"
                    + "保底与抽成取高、固定比例、阶梯比例、保底加抽成叠加", true),
            new FeeName("物业费", "物业费按“每平米固定单价 × 商户计租面积”按自然月计费", true), // BSM:1688
            new FeeName("能源费", "商户能源费按各商户独立表计的实际用量与实际费用代收，一律不得加价", true), // ENUM:199
            new FeeName("会员系统服务费", "每份商户合同在“按有效会员消费金额比例”或“固定月费”中二选一", false), // ENUM:85
            new FeeName("积分发行费", "积分发行费 ＝ 转为可用的积分数 × 全国统一积分发行单价", true), // ENUM:75
            new FeeName("押金", "押金金额 ＝ 收取月数 × 该商户签约时的月租金", true), // BSM:1347
            new FeeName("违约金", "按合同约定", true), // BSM:1347「并按合同计违约金」
            new FeeName("工单用料费（商户承担）", "用料必须从指定库房出库并按当前平均进货价计成本", true), // BSM:1380；第十八批 M 段第 117 条
            new FeeName("违规罚款", null, false), // HTML 没写计费基数，不自拟
            new FeeName("车位月租", "商户月租车位按合同约定、按月出账", true), // BSM:1437
            new FeeName("门票综合服务费", "按比例与按固定金额两种逐票计费方式互斥；单一酒吧门票按同一物理项目、同一酒吧的计费月汇总各票应计服务费，"
                    + "再按该月原锁定合同约定的月度封顶确定当月应收总额", true), // BSM:755
            new FeeName("停车抵免成本", "抵免成本按组成有效消费金额比例由各笔原消费归属经营主体承担，按内部结算价进入内部应收应付台账", true)); // BSM:1445

    /** 台账来源 → 名目；contract_rent 按所属合同收租方式归名（见 rentFeeName）。合同域已不出 contract_energy_fee（ContractService），不映射。 */
    private static final Map<String, String> SOURCE_FEE_NAMES = Map.ofEntries(
            Map.entry("turnover_rent", TURNOVER_RENT),
            Map.entry("contract_property_fee", "物业费"), Map.entry("property_fee", "物业费"),
            Map.entry("energy_fee", "能源费"),
            Map.entry("ticket_points_issuance", "积分发行费"),
            Map.entry("deposit", "押金"), Map.entry("deposit_adjustment", "押金"),
            Map.entry("termination_penalty", "违约金"),
            Map.entry("contract_parking_monthly", "车位月租"),
            Map.entry("single_bar_service_fee", "门票综合服务费"),
            Map.entry("parking_credit_cost", "停车抵免成本"),
            Map.entry("work_order_material", "工单用料费（商户承担）"));

    /**
     * 当期综合账单所列本商户台账行（一行一笔，带计费自然月 bill_month）；参数：本商户、本商户。
     * 第 92 条：综合账单汇总与账单已读、未读都按这同一组行取（BSM:1597 首页未读账单提醒）。
     * 第 101 条：有付款时点固化的普通内部结算往来到出账日（visible_at，与门票分账、异议截止日同一时刻）才列入；冲正、重述行随原行。
     */
    private static final String BILL_ROWS = """
            WITH RECURSIVE chain(item_id, root_id, next_id) AS (
                SELECT a.id, a.id, a.origin_item_id FROM ar_ap_items a
                 WHERE a.counterparty_id = ? OR (a.source_type = 'ticket_points_issuance' AND a.merchant_id = ?)
                UNION ALL
                SELECT chain.item_id, p.id, p.origin_item_id FROM chain JOIN ar_ap_items p ON p.id = chain.next_id
            )
            SELECT a.id, a.source_type, c.rent_terms ->> 'rentBillingMode' AS billing_mode, c.rent_terms ->> 'rentMode' AS rent_mode,
                   s.sign * a.amount_cent AS amount, s.sign * a.paid_amount_cent AS paid, s.sign * a.unpaid_amount_cent AS unpaid,
                   COALESCE(to_char(sbm.billing_month, 'YYYY-MM'), to_char(ips.billing_period_start, 'YYYY-MM'), pfb.period,
                            o.accounting_period, to_char(o.due_date, 'YYYY-MM')) AS bill_month
              FROM ar_ap_items a
              JOIN chain ON chain.item_id = a.id AND chain.next_id IS NULL
              JOIN ar_ap_items o ON o.id = chain.root_id
              CROSS JOIN LATERAL (SELECT CASE WHEN a.direction = 'payable' THEN -1 ELSE 1 END AS sign) s
              LEFT JOIN single_bar_service_fee_adjustments sba ON sba.refund_due_item_id = a.id
              LEFT JOIN single_bar_service_fee_months sbm ON sbm.id = sba.month_id
              LEFT JOIN internal_payable_schedules ips ON ips.ar_ap_item_id = o.id
              LEFT JOIN property_fee_bills pfb ON pfb.ar_ap_item_id = o.id
              LEFT JOIN contract_rent_bills crb ON a.source_type = 'contract_rent' AND crb.ar_ap_item_id = o.id
              LEFT JOIN contracts c ON c.id = crb.contract_id
             WHERE (a.direction = 'receivable' OR sba.refund_due_item_id IS NOT NULL)
               AND (ips.ar_ap_item_id IS NULL OR ips.visible_at <= now())
            """;

    private final JdbcTemplate jdbc;
    private final AuthorizationService auth;
    private final AuditEventService audit;

    public MerchantFeeStatementService(JdbcTemplate jdbc, AuthorizationService auth, AuditEventService audit) {
        this.jdbc = jdbc;
        this.auth = auth;
        this.audit = audit;
    }

    public FeeStatement statement(String month) {
        try {
            auth.requirePermission(VIEW);
            String merchantId = auth.currentMerchantScope()
                    .orElseThrow(() -> ApiException.forbidden("MERCHANT_SCOPE_REQUIRED", "此操作必须在商户范围内进行"));
            String period = parseMonth(month);
            FeeStatement result = build(merchantId, period);
            // 第 92 条（BSM:1597）：商户看过该月综合账单即记所列台账行已读（按商户记，首次已读时间不改）。
            jdbc.update("INSERT INTO merchant_bill_reads (merchant_id, bill_kind, bill_id) SELECT DISTINCT ?, 'ar_ap_item', id FROM ("
                    + BILL_ROWS + ") r WHERE bill_month = ? ON CONFLICT DO NOTHING", merchantId, merchantId, merchantId, period);
            audit.recordSuccess("merchant_portal.fee_statement.viewed", VIEW, "merchant_portal_fee_statement", merchantId,
                    null, null, Map.of("month", period));
            return result;
        } catch (RuntimeException exception) {
            audit.recordFailure("merchant_portal.fee_statement.view_failed", VIEW, "merchant_portal_fee_statement", null, null, null,
                    exception instanceof ApiException api ? api.code() : exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private static String parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now(LocalZone.ID).toString();
        }
        try {
            return YearMonth.parse(month.trim()).toString();
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("VALIDATION_FAILED", "月份格式不正确，应为 YYYY-MM");
        }
    }

    private FeeStatement build(String merchantId, String period) {
        String merchantName = jdbc.query("SELECT merchant_name FROM merchants WHERE id = ?",
                rs -> rs.next() ? rs.getString(1) : null, merchantId);
        // 名目 → [金额, 已付, 未付]；目录内名目在前（按目录顺序），目录外来源按中文名排在后。
        Map<String, long[]> known = new LinkedHashMap<>();
        Map<String, long[]> extra = new TreeMap<>();
        jdbc.query("SELECT source_type, billing_mode, rent_mode, SUM(amount) AS amount, SUM(paid) AS paid, SUM(unpaid) AS unpaid FROM ("
                + BILL_ROWS + ") r WHERE bill_month = ? GROUP BY 1, 2, 3", rs -> {
            String source = rs.getString("source_type");
            String name = "contract_rent".equals(source)
                    ? rentFeeName(rs.getString("billing_mode"), rs.getString("rent_mode"))
                    : SOURCE_FEE_NAMES.get(source);
            long[] sum = name != null
                    ? known.computeIfAbsent(name, k -> new long[3])
                    : extra.computeIfAbsent(FinanceService.sourceTypeLabel(source), k -> new long[3]);
            sum[0] += rs.getLong("amount");
            sum[1] += rs.getLong("paid");
            sum[2] += rs.getLong("unpaid");
        }, merchantId, merchantId, period);

        List<FeeItem> feeItems = new ArrayList<>();
        List<BillLine> lines = new ArrayList<>();
        for (FeeName fee : FEE_NAMES) {
            long[] sum = known.get(fee.name());
            feeItems.add(new FeeItem(fee.name(), fee.basis(), merchantName,
                    fee.billed() ? Long.valueOf(sum == null ? 0L : sum[0]) : null));
            if (sum != null) {
                lines.add(new BillLine(fee.name(), sum[0], sum[1], sum[2]));
            }
        }
        extra.forEach((name, sum) -> lines.add(new BillLine(name, sum[0], sum[1], sum[2])));
        long total = lines.stream().mapToLong(BillLine::amountCent).sum();
        long paid = lines.stream().mapToLong(BillLine::paidAmountCent).sum();
        long unpaid = lines.stream().mapToLong(BillLine::unpaidAmountCent).sum();
        return new FeeStatement(period, merchantId, merchantName, feeItems, new Bill(lines, total, paid, unpaid));
    }

    /** 第 92 条（BSM:1597「首页保留未读账单提醒，该提醒不因异议期届满而消失」）：综合账单里本商户还没看过的台账行，按计费自然月计笔数，新月在前。 */
    public List<UnreadMonth> unreadMonths(String merchantId) {
        return jdbc.query("SELECT bill_month, count(DISTINCT id) AS unread_count FROM (" + BILL_ROWS + ") r"
                        + " WHERE NOT EXISTS (SELECT 1 FROM merchant_bill_reads b WHERE b.merchant_id = ? AND b.bill_kind = 'ar_ap_item'"
                        + " AND b.bill_id = r.id) GROUP BY bill_month ORDER BY bill_month DESC",
                (rs, i) -> new UnreadMonth(rs.getString("bill_month"), rs.getLong("unread_count")), merchantId, merchantId, merchantId);
    }

    /** ENUM:257 把「保底与抽成取高」「保底加抽成叠加」列为营业额抽成模式：凡用到提成的收租方式归营业额抽成租金，账单金额照原值、不拆分。 */
    private static String rentFeeName(String billingMode, String rentMode) {
        if (billingMode == null || "fixed_only".equals(billingMode)) {
            return "per_sqm".equals(rentMode) ? PER_SQM_RENT : FIXED_RENT;
        }
        return TURNOVER_RENT;
    }
}
