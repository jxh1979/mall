package com.complex.platform.settlement;

// 文件版本：V0.04

import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.evidence.AuditEventService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 批 4b 开票清单(结算重构 PRD 附录 B.1 / 附录 D / 财务底稿 23_开票清单模板)。
 *
 * <p><b>边界</b>:系统只生成开票清单交财务,<b>不对接税控盘、不自建开票引擎、不生成发票号</b>
 * (业务 HTML:「开票由税务／电子发票第三方平台对接完成,本系统不自建开票引擎,只做状态对接与留痕」)。
 *
 * <p><b>清单怎么来</b>:一张 confirmed/settled 的月度结算单 →
 * <ul>
 *   <li>① 门票综合服务费(= 娱乐平台公司留存,同一项只扣一次)→ 收入类型 {@code ticket},
 *       开票主体经 {@code revenue_stream_attribution(ticket_service_fee)} 解析 = 娱乐平台公司,
 *       发票项目「综合体验服务费」;</li>
 *   <li>② 每条可分配池分账 → 收入类型 {@code spv_service},开票主体 = 该酒吧商户自身的结算主体,
 *       发票项目「运营服务费」。</li>
 * </ul>
 * 赠送权益兑付(内部成本转付)与积分/折扣券(营销成本)在 {@code invoice_subject_rules} 里
 * {@code invoiceable=false},<b>永不进对外清单</b>——底稿 23 第 11/12 行口径。
 *
 * <p><b>金额铁律</b>(业务 HTML 逐字):「开票金额 ≤ 已收 − 已退 − 已开;退款后先红冲再重开」。
 * 实现:本次可开额 = <b>应开额</b>(结算单当期净额,月结重跑已扣退款/冲正)− <b>已开额</b>
 * (同来源 待开票/开票中/已开票 三态合计)。≤0 即不生成,故:
 * <ul>
 *   <li>重复生成天然幂等(第二次可开额=0);</li>
 *   <li>红冲后额度释放,再生成即「重开」(演出结算台账行由 TicketPointsBillingJob 定时补开,同积分月账单);</li>
 *   <li>退款致应开额下降到已开额之下时不再生成,并在汇总里以 {@code overIssuedCent} 报「已超开」,提示先红冲。</li>
 * </ul>
 * DB 侧另有 {@code uq_sii_source_open} 局部唯一索引挡并发重复生成。
 */
@Service
public class SettlementInvoiceService {

    /** 门票综合服务费 → 娱乐平台公司(底稿 23 第 1 行)。 */
    static final String INCOME_TICKET = "ticket";
    /** 可分配池分账 → 各酒吧商户运营服务费(底稿 23 第 2 行)。 */
    static final String INCOME_SPV_SERVICE = "spv_service";
    /** FS-022-R1 积分发行费(ENUM L84「平台应收的积分发行费各自全额挂账、各自开票」)。 */
    static final String INCOME_POINTS_ISSUANCE = "points_issuance";
    /** 第十八批 AZ 段 290-R2：传媒演出结算(V362 isr_show 文化服务费)。 */
    static final String INCOME_SHOW = "show";
    /** 第十八批 AZ 段 290-R1：组合通票业态份额 → 收入类型(游乐、演出、餐饮、零售;酒吧份额走 ② 可分配池,「其他」不开)。 */
    private static final Map<String, String> FORMAT_INCOME_TYPES = Map.of(
            "amusement", "game", "media", INCOME_SHOW, "catering", "catering", "retail", "retail");

    private static final String STATUS_PENDING = "pending";
    private static final Set<String> BILL_INVOICEABLE_STATUSES = Set.of("confirmed", "settled");
    private static final int MAX_SIZE = 200;

    /**
     * 开票状态流转(业务 HTML 六态)。已红冲/已作废为终态;开票失败可修正后重回待开票。
     * 只有【已开票】能红冲——未开出去的票没什么可冲,直接作废。
     */
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "pending", Set.of("issuing", "issued", "failed", "voided"),
            "issuing", Set.of("issued", "failed", "voided"),
            "issued", Set.of("reversed"),
            "failed", Set.of("pending", "voided"),
            "reversed", Set.of(),
            "voided", Set.of());

    private final JdbcSettlementInvoiceRepository repository;
    private final RevenueStreamAttributionRepository attributionRepository;
    private final AuditEventService auditEventService;

    public SettlementInvoiceService(JdbcSettlementInvoiceRepository repository,
            RevenueStreamAttributionRepository attributionRepository, AuditEventService auditEventService) {
        this.repository = repository;
        this.attributionRepository = attributionRepository;
        this.auditEventService = auditEventService;
    }

    // ------------------------------------------------------------------
    // 生成
    // ------------------------------------------------------------------

    /** 按结算单生成(或补齐)开票清单条目;幂等。返回本次新增条目数。 */
    @Transactional
    public int generateForBill(String billId, String actorId) {
        var source = repository.findBillSource(billId);
        if (source == null) {
            throw ApiException.notFound("SETTLEMENT_BILL_NOT_FOUND", "结算单不存在:" + billId);
        }
        if (!BILL_INVOICEABLE_STATUSES.contains(source.status())) {
            throw ApiException.conflict("SETTLEMENT_BILL_NOT_CONFIRMED",
                    "仅已确认/已结算的结算单可生成开票清单,当前状态:" + source.status());
        }

        int created = 0;
        // ① 单头级:门票综合服务费 → 娱乐平台公司
        created += generateLine(source, null, INCOME_TICKET, source.compServiceFeeCent(), null, null, null, actorId);
        // ② 明细级:各酒吧分账 → 各酒吧商户
        for (var share : repository.listMerchantShares(billId)) {
            created += generateLine(source, share.settlementItemId(), INCOME_SPV_SERVICE, share.finalAmountCent(),
                    share.entityId(), share.merchantName(), null, actorId);
        }
        // ③ 第十八批 AZ 段 290-R1(BSM:1803「平台内部各主体之间的往来需要开具发票」;BSM:755「一律按总额收付」):组合通票游乐、演出、餐饮、零售
        //   各业态域间份额(含追收与到期未核销归属,即月结过账挂往来的同三列之和)按业态出单头级行,开票主体＝该业态份额归属主体,
        //   购方＝娱乐平台公司(all_pass_ticket 归属,与传媒演出结算门票加价付方同一取法)。代付是垫付款,不开票。
        var formatShares = repository.listFormatShares(billId);
        String platform = formatShares.isEmpty() ? null : attributionRepository.resolveManagementEntity(source.projectId(),
                TicketDistributionSettlementService.REVENUE_STREAM_ALL_PASS_TICKET);
        for (var share : formatShares) {
            created += generateLine(source, null, FORMAT_INCOME_TYPES.get(share.businessFormat()), share.dueAmountCent(),
                    share.beneficiaryEntityId(), null, platform, actorId);
        }

        auditEventService.recordSuccess("settlement.invoice.generated", "settlement.invoice.manage",
                "settlement_bill", billId, "req_" + java.util.UUID.randomUUID(), null,
                Map.of("created_item_count", created, "period_start", String.valueOf(source.periodStart())));
        return created;
    }

    /**
     * 生成一行清单。{@code fallbackEntityId} 非空时按该主体开票(商户侧收款方、业态份额归属主体),
     * 否则按规则里的收入流查归属主体。{@code buyerEntityId} 为购方(业态份额行为娱乐平台公司),其余为 null。
     */
    private int generateLine(JdbcSettlementInvoiceRepository.InvoiceSource source, String settlementItemId,
            String incomeType, long dueAmountCent, String fallbackEntityId, String fallbackName, String buyerEntityId,
            String actorId) {
        var rule = repository.findRule(source.projectId(), incomeType);
        if (rule == null || !rule.invoiceable()) {
            return 0;   // 未配规则,或规则明示「内部转付/营销成本不开票」
        }
        // 可开票金额 = 已收 − 已退 − 已开(前两项即结算单当期净额,月结重跑已扣冲正)
        long invoiceable = dueAmountCent - repository.sumOpenAmountCent(source.billId(), incomeType, settlementItemId);
        if (invoiceable <= 0) {
            return 0;
        }

        // 主体解析:给了 fallbackEntityId 就按它开(第十八批 AZ 段 290-R1:通票餐饮份额不得按 isr_catering 的 table_dining 收入流错开到订台餐饮主体名下);
        // 没给才按规则收入流查 V355 归属层,配了 entity 用配的主体,配了 per_merchant 或没配即 null(页面标「归属未配置」提示去配)。
        String subjectEntityId = fallbackEntityId;
        if (subjectEntityId == null && rule.revenueStream() != null) {
            var attribution = attributionRepository.resolveAttribution(source.projectId(), rule.revenueStream());
            if (attribution != null && "entity".equals(attribution.beneficiaryKind())) {
                subjectEntityId = attribution.managementEntityId();
            }
        }
        String subjectName = repository.findEntityName(subjectEntityId);
        if (subjectName == null) {
            subjectName = fallbackName;
        }

        Tax tax = tax(subjectEntityId, incomeType, invoiceable, source.periodEnd());
        repository.insert(new SettlementInvoiceItemRecord(
                buildId(source.billId(), incomeType, settlementItemId),
                source.billId(), settlementItemId, source.projectId(),
                source.periodStart(), source.periodEnd(), incomeType,
                subjectEntityId, subjectName, rule.invoiceItemName(), rule.taxCategory(), rule.evidenceRequired(),
                invoiceable, tax.vatRateBps(), tax.vatCent(),
                tax.culturalApplicable(), tax.culturalRateBps(), tax.culturalCent(),
                STATUS_PENDING, null, actorId, null, buyerEntityId, null, null));
        return 1;
    }

    /**
     * 第十八批 AZ 段 290-R2(BSM:1803;BSM:1144「酒吧门票上的演出加价部分，随酒吧门票由娱乐平台公司售票收款后分给传媒公司」;BSM:1145;
     * BSM:1750「开票金额≤已收−已退−已开」):传媒演出结算应付(门票加价分配、固定演出费、酒水提成)出账即进开票清单——文化服务费(show 规则),
     * 开票主体＝收方传媒公司;购方＝付方(门票加价为娱乐平台公司;固定演出费、酒水提成为该酒吧及其收款主体,同 listMerchantShares 取法)。
     * 应开额＝该台账行金额加回指它的冲正行金额(调整后原行为 0,列入已超开提示先红冲),可开额＝应开额−该行已开额,≤0 不生成。
     * 期间＝演出日所在月。返回本次新增条目数。
     */
    @Transactional
    public int generateForMediaSettlement(String arApItemId, String projectId, LocalDate showDate, String payeeEntityId,
            String payerEntityId, String payerMerchantId, String actorId) {
        var rule = repository.findRule(projectId, INCOME_SHOW);
        if (rule == null || !rule.invoiceable()) {
            return 0;
        }
        long invoiceable = repository.arApDueAmountCent(arApItemId) - repository.sumOpenArApAmountCent(arApItemId);
        if (invoiceable <= 0) {
            return 0;
        }
        LocalDate start = showDate.withDayOfMonth(1);
        LocalDate end = showDate.withDayOfMonth(showDate.lengthOfMonth());
        Tax tax = tax(payeeEntityId, INCOME_SHOW, invoiceable, end);
        repository.insert(new SettlementInvoiceItemRecord(
                "sii_mps_" + java.util.UUID.randomUUID().toString().replace("-", ""), null, null, projectId, start, end,
                INCOME_SHOW, payeeEntityId, repository.findEntityName(payeeEntityId),
                rule.invoiceItemName(), rule.taxCategory(), rule.evidenceRequired(),
                invoiceable, tax.vatRateBps(), tax.vatCent(),
                tax.culturalApplicable(), tax.culturalRateBps(), tax.culturalCent(),
                STATUS_PENDING, null, actorId, null,
                payerEntityId != null ? payerEntityId : repository.findMerchantEntityId(payerMerchantId), payerMerchantId, arApItemId));
        auditEventService.recordSuccess("settlement.invoice.generated", "settlement.invoice.manage",
                "ar_ap_item", arApItemId, "req_" + java.util.UUID.randomUUID(), null,
                Map.of("created_item_count", 1, "amount_cent", invoiceable, "period_start", String.valueOf(start)));
        return 1;
    }

    /**
     * 18AZ 收尾复核(BSM:1750「退款后先红冲再重开」;BSM:1803):应开额高于已开额的演出结算台账行(开票行红冲或作废后额度释放;
     * 定时作业逐条调 {@link #generateForMediaSettlement} 补开)。
     */
    public List<JdbcSettlementInvoiceRepository.MediaInvoiceSource> mediaSettlementsToInvoice() {
        return repository.listMediaSettlementsToInvoice();
    }

    /** 已收额高于已开额的积分月账单(定时作业逐张调 {@link #generateForPointsBill})。 */
    public List<String> pointsBillsToInvoice() {
        return repository.listPointsBillsToInvoice();
    }

    /**
     * FS-022-R1:积分发行费月账单接入统一开票清单。ENUM L84「商户应收的兑换补偿与平台应收的积分发行费各自全额挂账、
     * 各自开票……不得轧差」;BSM L1750「开票金额≤已收−已退−已开」;BSM L1803「平台内部各主体之间的往来需要开具发票」。
     * 开票主体=账单收款方(全国积分结算中心/顶层平台公司),购方=合同销售方;本次可开额=该账单已收−已开(只取本账单金额,
     * 不与兑换补偿相抵);≤0 不生成,故重跑幂等、每次收款只补开新收部分。账单行加锁,同账单并发生成串行。返回本次新增条目数。
     */
    @Transactional
    public int generateForPointsBill(String pointsBillId, String actorId) {
        var source = repository.lockPointsBillSource(pointsBillId);
        if (source == null) {
            return 0;   // 尚未形成正式应收,没有已收额可开
        }
        var rule = repository.findRule(source.projectId(), INCOME_POINTS_ISSUANCE);
        if (rule == null || !rule.invoiceable()) {
            return 0;
        }
        long invoiceable = source.paidAmountCent() - repository.sumOpenPointsAmountCent(pointsBillId);
        if (invoiceable <= 0) {
            return 0;
        }
        LocalDate monthEnd = source.billingMonth().withDayOfMonth(source.billingMonth().lengthOfMonth());
        Tax tax = tax(source.receiverEntityId(), INCOME_POINTS_ISSUANCE, invoiceable, monthEnd);
        repository.insert(new SettlementInvoiceItemRecord(
                // 积分账单号本身已较长,条目 id 不再拼来源,免超 64 位;来源由 points_bill_id 列承载。
                "sii_pts_" + java.util.UUID.randomUUID().toString().replace("-", ""), null, null, source.projectId(),
                source.billingMonth(), monthEnd,
                INCOME_POINTS_ISSUANCE, source.receiverEntityId(), repository.findEntityName(source.receiverEntityId()),
                rule.invoiceItemName(), rule.taxCategory(), rule.evidenceRequired(),
                invoiceable, tax.vatRateBps(), tax.vatCent(),
                tax.culturalApplicable(), tax.culturalRateBps(), tax.culturalCent(),
                STATUS_PENDING, null, actorId, pointsBillId, source.sellerEntityId(), source.sellerMerchantId(), null));
        auditEventService.recordSuccess("settlement.invoice.generated", "settlement.invoice.manage",
                "ticket_points_bill", pointsBillId, "req_" + java.util.UUID.randomUUID(), null,
                Map.of("created_item_count", 1, "amount_cent", invoiceable,
                        "billing_month", String.valueOf(source.billingMonth())));
        return 1;
    }

    /**
     * 税额:含税额 × 率 ÷ (10000 + 率),四舍五入到分(全局金额口径 D15.1);文化事业建设费计费依据 = 取得的全部含税价款
     * (财税〔2016〕25 号),最终认定权留财务,费率后台可配。未配即 0。第十八批 AR 段第 233 条 233-R2:税率按开票主体、按业务日期(开票期间末日)
     * 当时生效的版本取(ENUM:201「每个主体 × 每类收入 × 每种税」;BSM:521「已发生的业务一律按当时生效的版本计算」)。
     */
    private Tax tax(String subjectEntityId, String incomeType, long amountCent, LocalDate date) {
        var vat = repository.findTaxRate(subjectEntityId, incomeType, "vat", date);
        int vatRateBps = vat == null ? 0 : vat.rateBps();
        long vatCent = MoneyRounding.prorate(amountCent, vatRateBps, MoneyRounding.BPS_DENOMINATOR + vatRateBps);
        var cultural = repository.findTaxRate(subjectEntityId, incomeType, "cultural_fee", date);
        int culturalRate = cultural == null ? 0 : cultural.rateBps();
        boolean culturalApplicable = culturalRate > 0;
        return new Tax(vatRateBps, vatCent, culturalApplicable, culturalRate,
                culturalApplicable ? MoneyRounding.bpsOf(amountCent, culturalRate) : 0L);
    }

    private record Tax(int vatRateBps, long vatCent, boolean culturalApplicable, int culturalRateBps, long culturalCent) {
    }

    /**
     * 条目 id:同来源可能因红冲后重开而生成多条,故带一个短随机后缀。
     * 唯一性由 {@code uq_sii_source_open}(占额三态内唯一)保证,不靠 id 本身。
     */
    private static String buildId(String billId, String incomeType, String settlementItemId) {
        String source = settlementItemId != null ? settlementItemId : billId;
        return "sii_" + incomeType + "_" + source + "_" + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    // ------------------------------------------------------------------
    // 状态流转(第三方开票结果回填 / 红冲 / 作废)
    // ------------------------------------------------------------------

    /** 流转开票状态。本系统不发起开票,只登记第三方结果与红冲/作废留痕。 */
    @Transactional
    public void changeStatus(String itemId, String targetStatus, String invoiceNo, String remark, String actorId) {
        String current = repository.findStatus(itemId);
        if (current == null) {
            throw ApiException.notFound("INVOICE_ITEM_NOT_FOUND", "开票清单条目不存在:" + itemId);
        }
        Set<String> allowed = ALLOWED_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw ApiException.conflict("INVOICE_STATUS_TRANSITION_FORBIDDEN",
                    "开票状态不可从「" + zh(current) + "」流转到「" + zh(targetStatus) + "」");
        }
        repository.updateStatus(itemId, targetStatus, invoiceNo, remark, actorId);
        auditEventService.recordSuccess("settlement.invoice.status_changed", "settlement.invoice.manage",
                "settlement_invoice_item", itemId, "req_" + java.util.UUID.randomUUID(), null,
                Map.of("from", current, "to", targetStatus, "invoice_no", invoiceNo == null ? "" : invoiceNo));
    }

    private static String zh(String status) {
        return switch (status) {
            case "pending" -> "待开票";
            case "issuing" -> "开票中";
            case "issued" -> "已开票";
            case "reversed" -> "已红冲";
            case "voided" -> "已作废";
            case "failed" -> "开票失败";
            default -> status;
        };
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    public PageResponse<JdbcSettlementInvoiceRepository.SettlementInvoiceItemView> list(String projectId,
            LocalDate periodStart, String subjectEntityId, String status, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(MAX_SIZE, Math.max(1, size));
        long total = repository.count(projectId, periodStart, subjectEntityId, status);
        var items = repository.list(projectId, periodStart, subjectEntityId, status, safeSize,
                (safePage - 1) * safeSize);
        return new PageResponse<>(items, safePage, safeSize, total);
    }

    /** 按开票主体分组汇总 + 开票映射规则(财务据此按主体开票,并看清每项税率/凭证要件从哪来)。 */
    public InvoiceSummaryView summarize(String projectId, LocalDate periodStart) {
        return new InvoiceSummaryView(repository.summarizeBySubject(projectId, periodStart),
                repository.listRules(projectId), overIssued(projectId, periodStart));
    }

    /**
     * 已超开明细:某来源的已开额 > 结算单当期应开额(通常是结算单重跑因退款下调所致)。
     * 业务 HTML 要求「退款后先红冲再重开」,这里把该红冲的条目挑出来给财务。
     */
    private List<OverIssuedLine> overIssued(String projectId, LocalDate periodStart) {
        var lines = new ArrayList<OverIssuedLine>();
        for (var summary : repository.listOverIssued(projectId, periodStart)) {
            lines.add(new OverIssuedLine(summary.billId(), summary.incomeType(), summary.settlementItemId(),
                    summary.subjectName(), summary.dueAmountCent(), summary.openAmountCent(),
                    summary.openAmountCent() - summary.dueAmountCent()));
        }
        return lines;
    }

    /** 汇总视图:主体分组 + 规则表 + 已超开告警。 */
    public record InvoiceSummaryView(List<JdbcSettlementInvoiceRepository.InvoiceSubjectSummary> subjects,
            List<JdbcSettlementInvoiceRepository.InvoiceSubjectRule> rules, List<OverIssuedLine> overIssued) {
    }

    /** 已超开一行:应开 vs 已开 与超出额(须先红冲再重开)。 */
    public record OverIssuedLine(String billId, String incomeType, String settlementItemId, String subjectName,
            long dueAmountCent, long openAmountCent, long overIssuedCent) {
    }
}
