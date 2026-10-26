package com.complex.platform.bar;

// 文件版本：V0.03

import static com.complex.platform.bar.BarValidation.requireRequest;
import static com.complex.platform.bar.BarValidation.requireText;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.ArApItemSourceRecord;
import com.complex.platform.finance.FinanceRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 第十六批第 16 条（ENUM L101「账单状态 …… 异常」；ENUM L221「任何单据进入“异常”后，必须由主管在系统内选择“更正后恢复”或“作废并冲正”
 * 两种方式之一处置，填写原因并留痕；恢复后回到进入异常之前的那个状态」；操作员裁决补充第 2 条「客人对账单提出争议时员工登记置异常。
 * 之后都走主管「更正后恢复」或「作废并冲正」」）：
 * <ul>
 *   <li>登记：账单置 abnormal，进入前的 payment_status 记在 abnormal_prior_status；异常期间不收款、不反结（见 BarBillingService）。</li>
 *   <li>更正后恢复：回到进入异常之前的那个状态；已结账单再按已成功退款额重算部分退款／已退款（异常期间办过的退款也算上）。</li>
 *   <li>作废并冲正：账单作废（void），已登记的收款流水按反结同一做法冲正（reversed，不再计入营收与对账）；
 *       同反结，结账营业日已锁定日结的拒绝（DAY_CLOSE_LOCKED，走补录调整流程）。
 *       第十七批（操作员裁决补充第 1 条「全部自动冲回（推荐）：作废时系统自动把已收款项登记为原路待退（走现有退款链），
 *       并冲回已发积分、成长值和提成（工资单不再挂待定）」）同一事务内：已收款项原路待退（{@link BarBillingService#refundCollectedOnVoid}）、
 *       本单商户消费积分与成长值按退款冲回同一路径冲回、销售提成置 void（工资单记下的该单提成待定当即结清；已月结拨付所属主体的追加应收冲回）、
 *       本单低消留款与留款抵用置 reversed；挂账账单挂出的应收未收部分冲正、财务不再追收，已由财务收回的部分并入退款应付原路待退；
 *       作废前已办过退款的只冲正未退部分的收款（各期合计为 0）。</li>
 * </ul>
 * 原因随成功审计留痕（BarService）。
 */
@Service
public class BarBillAnomalyService {

    static final Set<String> RESOLUTIONS = Set.of("restored", "voided_reversed");

    private final JdbcTemplate jdbc;
    private final BarRepository repository;
    private final AuthorizationService authorizationService;
    private final BarBillRefundStatusService refundStatus;
    private final BarBillingService billing;
    private final FinanceRepository finance;

    // 第十八批第 1 条（ENUM:187）：已锁原行的冲正行记该酒吧当前营业日。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    com.complex.platform.common.BusinessDayResolver businessDays = new com.complex.platform.common.BusinessDayResolver();

    public BarBillAnomalyService(JdbcTemplate jdbc, BarRepository repository, AuthorizationService authorizationService,
            BarBillRefundStatusService refundStatus, BarBillingService billing, FinanceRepository finance) {
        this.jdbc = jdbc;
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.refundStatus = refundStatus;
        this.billing = billing;
        this.finance = finance;
    }

    BarBillRecord raise(String billId, BarBillAnomalyRequest request) {
        requireText(requireRequest(request).reason(), "reason");
        BarBillRecord bill = lockedBill(billId);
        if ("abnormal".equals(bill.paymentStatus())) {
            throw ApiException.conflict("BAR_BILL_ALREADY_ABNORMAL", "该账单已在异常中，等待主管处置");
        }
        if ("void".equals(bill.paymentStatus())) {
            throw ApiException.conflict("BAR_BILL_VOIDED", "账单已作废");
        }
        jdbc.update("""
                UPDATE bar_bills SET abnormal_prior_status = payment_status, payment_status = 'abnormal', updated_at = now()
                 WHERE id = ?""", bill.id());
        return repository.findBillById(bill.id()).orElseThrow();
    }

    BarBillRecord resolve(String billId, BarBillAnomalyResolveRequest request) {
        BarBillAnomalyResolveRequest actual = requireRequest(request);
        String resolution = requireText(actual.resolution(), "resolution");
        if (!RESOLUTIONS.contains(resolution)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "处置方式只能是更正后恢复或作废并冲正");
        }
        requireText(actual.reason(), "reason");
        BarBillRecord bill = lockedBill(billId);
        if (!"abnormal".equals(bill.paymentStatus())) {
            throw ApiException.conflict("BAR_BILL_NOT_ABNORMAL", "账单不在异常中，无需处置");
        }
        if ("restored".equals(resolution)) {
            jdbc.update("""
                    UPDATE bar_bills SET payment_status = abnormal_prior_status, abnormal_prior_status = NULL, updated_at = now()
                     WHERE id = ?""", bill.id());
            refundStatus.refresh(bill.id());
        } else {
            billing.requireSettleDayUnlocked(bill);
            long onAccountCollected = jdbc.queryForObject("""
                    SELECT COALESCE(sum(paid_amount_cent), 0) FROM ar_ap_items
                     WHERE direction = 'receivable' AND source_type = 'bar_bill' AND source_id = ? AND origin_item_id IS NULL""",
                    Long.class, bill.id());
            long refunded = billing.refundCollectedOnVoid(bill, actual.reason(), onAccountCollected);
            reverseOnAccountReceivable(bill.id());
            reverseDispatchedCommissionTransfers(bill.id());
            jdbc.update("""
                    UPDATE bar_bills SET payment_status = 'void', settlement_status = 'void', abnormal_prior_status = NULL,
                           updated_at = now()
                     WHERE id = ?""", bill.id());
            reverseUnrefundedCollections(bill.id(), refunded);
            // BSM L1064「以该笔留款抵付的部分不重复收款、不重复确认收入」：本单低消留款含在实收里已随作废退回，不再作台组抵用额；
            // 本单抵用过的台组留款不再算已用、回到台组余额（同退品驳回把留款置 reversed）。
            jdbc.update("""
                    UPDATE bar_bill_adjustments SET status = 'reversed'
                     WHERE bill_id = ? AND status = 'applied' AND adjustment_type IN ('min_spend_retained', 'min_spend_retained_applied')""",
                    bill.id());
            jdbc.update("UPDATE bar_commission_calculations SET status = 'void' WHERE bill_id = ?", bill.id());
            // 工资单不再挂待定：此前工资单记下的该单提成待定当即结清（原单不再显示，之后的工资单也不再按历史待定展开）。
            jdbc.update("""
                    UPDATE staff_payroll_pending_sources SET status = 'resolved', resolved_at = clock_timestamp()
                     WHERE source_type = 'BAR_COMMISSION_PENDING' AND status = 'pending'
                       AND source_id IN (SELECT id FROM bar_commission_calculations WHERE bill_id = ?)""", bill.id());
            billing.reconcileRewards(bill.id());
        }
        return repository.findBillById(bill.id()).orElseThrow();
    }

    /**
     * BSM L1142/L1143（营销、派驻人员促成订单的提成「由该酒吧按月拨付给…所属主体」）：本单提成作废前已随提成月结
     * （StaffService 提成月结，bar_commission_settlements）拨付所属主体的，按作废提成额向所属主体追加一笔应收冲回
     * （同酒吧报酬月结负额单列应收，不轧差、不改原月结）；月结之后才计提的不在原月结内，不冲。提成所属月结按本单开台或下单时点所属
     * 营业日的月份（ENUM:187，同工资单与提成月结）。
     */
    private void reverseDispatchedCommissionTransfers(String billId) {
        String zone = LocalZone.ID.getId();
        Instant now = Instant.now();
        for (Map<String, Object> row : jdbc.queryForList("""
                SELECT c.id, c.commission_amount_cent, s.month, s.merchant_id, s.company_id, s.project_id, s.due_date
                  FROM bar_commission_calculations c
                  JOIN bar_bills b ON b.id = c.bill_id
                  JOIN staff_dispatch_records d ON d.user_id = c.sales_staff_id AND d.merchant_id = COALESCE(b.merchant_id, b.bar_id)
                   AND d.start_date <= (c.created_at AT TIME ZONE ?)::date
                   AND (d.end_date IS NULL OR d.end_date >= (c.created_at AT TIME ZONE ?)::date)
                  JOIN bar_commission_settlements s ON s.month = ?
                   AND s.merchant_id = COALESCE(b.merchant_id, b.bar_id) AND s.company_id = d.company_id AND s.created_at >= c.created_at
                 WHERE c.bill_id = ? AND c.status NOT IN ('void', 'cancelled') AND c.commission_amount_cent > 0
                   AND b.settlement_status = 'settled'""", zone, zone,
                java.time.YearMonth.from(businessDays.barBillBusinessDate(billId)).toString(), billId)) {
            String commission = (String) row.get("id");
            long amount = ((Number) row.get("commission_amount_cent")).longValue();
            String key = UUID.nameUUIDFromBytes(commission.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
            String item = "arap_dcr_" + key;
            finance.appendArApItem(new ArApItemRecord(item, "ARAP-DCR-" + key, "receivable", (String) row.get("company_id"),
                    "dispatch_commission_transfer", commission, amount, 0L, amount, "CNY", ((java.sql.Date) row.get("due_date")).toLocalDate(),
                    (String) row.get("month"), "pending_collection", now, now, (String) row.get("merchant_id"), (String) row.get("project_id")));
            finance.saveArApItemSource(new ArApItemSourceRecord("arap_src_" + item, item, "dispatch_commission_transfer", commission,
                    amount, now));
        }
    }

    /**
     * 第十七批②：挂账账单作废——冲销挂账时挂出的应收（{@link BarReceivableLedgerService#createReceivableForOnAccountBill}），
     * 同合同预排账单作废（JdbcContractRepository.voidUnpaidRentBills）的冲正行写法：追加一行回指原行（origin_item_id）、未收金额取反、
     * 状态 reversed，原行不动；之后原行不能再登记收款（FinanceService.settleArApItem 409 AR_AP_ITEM_REVERSED）、不计欠款，财务不再追收。
     * 原行已锁日结的，冲正行记作废当日（不改已锁日结）。原行已由财务收回的部分不冲正，随作废并入退款应付原路待退（见 resolve）。
     */
    private void reverseOnAccountReceivable(String billId) {
        jdbc.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent,
                        unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at, merchant_id, project_id,
                        origin_item_id)
                SELECT 'arap_void_' || md5(a.id), 'CZ-' || md5(a.id), a.direction, a.counterparty_id, a.source_type, a.source_id,
                       -a.unpaid_amount_cent, 0, -a.unpaid_amount_cent, a.currency,
                       CASE WHEN a.locked_day_close_id IS NULL THEN a.due_date ELSE CAST(? AS DATE) END, a.accounting_period,
                       'reversed', now(), now(), a.merchant_id, a.project_id, a.id
                  FROM ar_ap_items a
                 WHERE a.direction = 'receivable' AND a.source_type = 'bar_bill' AND a.source_id = ? AND a.origin_item_id IS NULL
                   AND a.unpaid_amount_cent > 0 AND NOT EXISTS (SELECT 1 FROM ar_ap_items c WHERE c.origin_item_id = a.id)
                ON CONFLICT (id) DO NOTHING""", java.sql.Date.valueOf(businessDays.businessDate(null,
                jdbc.queryForObject("SELECT bar_id FROM bar_bills WHERE id = ?", String.class, billId), Instant.now())), billId);
    }

    /**
     * 第十七批⑤：作废前已办过的退款已在退款营业日冲减（现有跨期退款口径；那一天可能已锁日结，不改），作废只冲正未退部分的收款，
     * 作废账单在各期合计为 0（日结、分红与经营分析都按收款流水取实收、按退款取冲减，不重复冲减）。已退额按退款原路同序
     * （TK-20-R2「后收的先退」）占在最后几笔收款上：本单收款流水整体置冲正（同反结），被已退额占用的部分按原收款时点、渠道另记一笔
     * 不冲正的留存流水（收款营业日即结账营业日，作废前已校验未锁日结）。
     */
    private void reverseUnrefundedCollections(String billId, long refunded) {
        List<Object[]> kept = new ArrayList<>();
        long left = refunded;
        for (Map<String, Object> s : jdbc.queryForList("""
                SELECT id, settlement_amount_cent FROM bar_bill_settlements
                 WHERE bill_id = ? AND NOT reversed ORDER BY settled_at DESC, id DESC""", billId)) {
            if (left <= 0) {
                break;
            }
            long part = Math.min(left, ((Number) s.get("settlement_amount_cent")).longValue());
            left -= part;
            kept.add(new Object[] {part, s.get("id")});
        }
        repository.markBillSettlementsReversed(billId);
        for (Object[] k : kept) {
            jdbc.update("""
                    INSERT INTO bar_bill_settlements (id, bill_id, settle_channel, cashier_staff_id, pos_transaction_no,
                            settlement_amount_cent, merchant_id, settled_at)
                    SELECT 'settle_kept_' || md5(id), bill_id, settle_channel, cashier_staff_id, pos_transaction_no, ?, merchant_id, settled_at
                      FROM bar_bill_settlements WHERE id = ?""", k);
        }
    }

    private BarBillRecord lockedBill(String billId) {
        BarBillRecord bill = repository.findBillByIdForUpdate(billId)
                .orElseThrow(() -> ApiException.notFound("BAR_BILL_NOT_FOUND", "账单不存在或已被删除"));
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(bill.merchantId())) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN", "无权操作其他商户的数据");
        }
        return bill;
    }
}
