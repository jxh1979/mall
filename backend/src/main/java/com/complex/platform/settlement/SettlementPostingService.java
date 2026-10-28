package com.complex.platform.settlement;

// 文件版本：V0.04

import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.ArApItemRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结算重构批2-4：月度分账结算单 <b>过账</b> 到 finance 应付台账（ar_ap_items）。
 *
 * <p>与计算引擎 {@link TicketDistributionSettlementService} 解耦：引擎算+校验+写 confirmed，过账单独触发（月度作业或网关端点）。
 * 本服务不改引擎算法。
 *
 * <p>过账口径（照 bar 域 createPayableForGiftSettlement 范式）：
 * <ul>
 *   <li>仅对 status='confirmed' 的结算单过账（其余状态拒绝并说明）；</li>
 *   <li>遍历 settlement_items（status='draft' 且 final_amount_cent≠0）→ 正额生成
 *       ar_ap_items（direction='payable'、source_type='ticket_distribution'、source_id=明细 id、
 *       counterparty_id=酒吧 merchant、amount=final_amount_cent）；FS-021-R1 负额生成该酒吧的单独追收应收
 *       （direction='receivable'、source_type='ticket_distribution_recovery'、amount=|final|，不与应付轧差）
 *       → 回写 ar_ap_item_id + status='posted'；final=0 天然跳过（不取入待过账集）；</li>
 *   <li>FS-021-R4：每条往来的到期日按该钱路财务指定付账日、异议期与银行工作日推算并固化（internal_payable_schedules），
 *       规则或日历缺失即拒绝过账、账单保持 confirmed；付款时点的项目（异议期按项目、收款方专区可见、换账户审批）取往来对方
 *       所属项目——生产月结出平台单（project_id 为空），明细取商户所属项目、服务费取计费月项目；</li>
 *   <li>全部明细 posted 后 → 结算单 status='settled'、settled_at=now；</li>
 *   <li>幂等：明细 status='posted' 已过即跳过；ar_ap 唯一索引 (direction,source_type,source_id) +
 *       PK=arap_&lt;明细 id&gt; 双保险，重复过账不产生第二条应付。</li>
 * </ul>
 *
 * <p><b>D7 超额 C 口径（甲标签法）</b>：仅当 bill <b>超池</b>（超池额 pool_overflow_cent &gt; 0）时，
 * 对<b>第一段明细</b>（固定金额／固定单价）两段拆——超额 {@code excess_i} 按本单第一段明细金额占比累计额差分分摊超池额（Σexcess＝超池额），
 * 过 {@code platform_incentive_subsidy}（L3 娱乐平台公司承担，独立幂等前缀 {@code settle-pis-} 防与 td 行撞）、
 * 池内额 {@code pool_i = a_i-excess_i} 过 {@code ticket_distribution}（项目公司承担）。
 * 第二段、专属直付 item（exclusive_full）永不拆、全额 ticket_distribution；未超池 bill 完全现状（单条）。
 *
 * <p>第 75 条（ENUM L52「超池的判定与审批一律在事前完成，结算环节不再设“超池待审批”」「超出部分由娱乐平台公司承担」）：
 * 超池单与普通单一样 confirmed 即可过账，不设结算期审批；超出部分照上述两段拆由娱乐平台公司承担。
 *
 * <p>状态流转：结算单 confirmed →（本服务过账）settled；
 * 明细 draft →（本服务过账）posted。
 */
// ponytail: 过账直接调 FinanceService.createArApItem(public，与 bar 域 createPayableForGiftSettlement 同底层)。
//   不复用 bar 域 BarReceivableLedgerService——该接口 package-private 且签名吃 bar 领域记录，跨域耦合不划算。
@Service
public class SettlementPostingService {

    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String METHOD_EXCLUSIVE_FULL =
            TicketDistributionSettlementService.METHOD_EXCLUSIVE_FULL;
    private static final String SOURCE_TICKET_DISTRIBUTION = "ticket_distribution";
    private static final String SOURCE_PLATFORM_INCENTIVE_SUBSIDY = "platform_incentive_subsidy";
    /** FS-021-R1：负额明细（向商户追回）过账为该商户的单独追收应收。 */
    static final String SOURCE_TICKET_DISTRIBUTION_RECOVERY = "ticket_distribution_recovery";
    /** 第一段两种方式（超池只由第一段合计高于份池产生，越池两段拆只对它们；第二段、专属直付与 R2 追收明细不拆）。 */
    private static final java.util.Set<String> FIRST_SEGMENT_METHODS = java.util.Set.of(
            TicketDistributionSettlementService.METHOD_BY_VENUE_PER_GUEST, TicketDistributionSettlementService.METHOD_BY_FIXED_AMOUNT);

    private final JdbcSettlementBillRepository repository;
    private final FinanceService financeService;
    private final com.complex.platform.evidence.AuditEventService auditEventService;
    private final SingleBarServiceFeeService singleBarServiceFees;
    private final InternalPayDayService payDays;

    public SettlementPostingService(JdbcSettlementBillRepository repository, FinanceService financeService,
                                    com.complex.platform.evidence.AuditEventService auditEventService,
                                    SingleBarServiceFeeService singleBarServiceFees, InternalPayDayService payDays) {
        this.repository = repository;
        this.financeService = financeService;
        this.auditEventService = auditEventService;
        this.singleBarServiceFees = singleBarServiceFees;
        this.payDays = payDays;
    }

    /**
     * 过账某结算单：仅 confirmed 单生效；遍历待过账明细挂 finance 应付、回写、全过后置 settled。
     * D7：超池 bill 对池明细走超额 C 口径两段拆（池内→ticket_distribution、超额→platform_incentive_subsidy）。
     *
     * @param billId 结算单 id
     * @return 本次实际过账（新建或幂等命中）的明细笔数
     */
    @Transactional
    public int postBill(String billId) {
        JdbcSettlementBillRepository.OverPoolContext ctx = repository.findOverPoolContext(billId);
        if (ctx == null) {
            throw com.complex.platform.common.api.ApiException.notFound(
                    "SETTLEMENT_BILL_NOT_FOUND", "结算账单不存在：" + billId);
        }
        // 仅 confirmed 过账：settled/reversed=不重复过。
        if (!STATUS_CONFIRMED.equals(ctx.status())) {
            throw com.complex.platform.common.api.ApiException.conflict(
                    "SETTLEMENT_BILL_NOT_CONFIRMED",
                    "只有已确认的账单才能过账；当前状态：" + ctx.status());
        }

        boolean overPool = ctx.overPool();
        // 第十八批第 2 条(ENUM L52「娱乐平台公司确需让第一段合计高于池金额的……超出部分由娱乐平台公司承担」):超池额只由某几份第一段
        //   合计高于该份池产生;补贴合计恰为超池额,按本单第一段明细金额占比累计额差分拆到各家(可分配池里的未核销减除、未分配、暂挂、
        //   尾差与第二段分账都不在拆分基数里)。第一段明细全未核销时补贴无可拆。
        java.util.List<JdbcSettlementBillRepository.PostableItem> items = repository.findPostableItems(billId);
        long firstSegmentTotal = items.stream()
                .filter(i -> i.finalAmountCent() > 0 && FIRST_SEGMENT_METHODS.contains(i.distributionMethod()))
                .mapToLong(JdbcSettlementBillRepository.PostableItem::finalAmountCent).sum();
        long subsidyTotal = Math.min(ctx.pooledDistributedCent() - ctx.availablePoolCent(), firstSegmentTotal);
        long firstSegmentSoFar = 0L;

        // FS-021-R4（BSM L1673/L1683；DEC-R8-56/57/58）：每条钱路按财务指定付账日、异议期与银行工作日推算到期日并固化；
        //   规则或日历缺失即拒绝过账（账单保持 confirmed），不再填「今天+30 天」。同一账单同一钱路只推算一次。
        java.time.Instant now = java.time.Instant.now();
        java.util.Map<String, InternalPayDayService.Schedule> schedules = new java.util.HashMap<>();
        java.util.function.BiFunction<String, String, InternalPayDayService.Schedule> schedule = (route, project) ->
                schedules.computeIfAbsent(route + "|" + project, k -> payDays.schedule(route, project, ctx.periodStart(), now));

        int posted = 0;
        for (var item : items) {
            String primaryArApId;
            long a = item.finalAmountCent();
            if (a < 0) {
                // FS-021-R1（BSM L755「一律按总额收付、收支两条线，不得轧差」）：负额＝向该商户追回，单独挂追收应收，不从任何应付里扣。
                primaryArApId = post("receivable", item.itemId(), item.merchantId(), -a,
                        SOURCE_TICKET_DISTRIBUTION_RECOVERY, "settle-tdr-", ctx.projectId(), item.payeeProjectId(), schedule);
            } else if (overPool && FIRST_SEGMENT_METHODS.contains(item.distributionMethod())) {
                // 超额 C 口径两段拆：pool_i(项目公司承担) + excess_i(L3 平台承担)，pool_i+excess_i=a_i 精确；Σexcess_i＝补贴合计。
                //   MoneyRounding 全程 BigDecimal，a×补贴合计不在 long 上溢出(对抗复核 risk1)。
                long before = firstSegmentSoFar;
                firstSegmentSoFar += a;
                long excessI = MoneyRounding.prorate(subsidyTotal, firstSegmentSoFar, firstSegmentTotal)
                        - MoneyRounding.prorate(subsidyTotal, before, firstSegmentTotal);
                long poolI = a - excessI;
                String tdId = poolI > 0
                        ? createPayable(item.itemId(), item.merchantId(), poolI, SOURCE_TICKET_DISTRIBUTION, "settle-td-",
                                ctx.projectId(), item.payeeProjectId(), schedule)
                        : null;
                String pisId = excessI > 0
                        ? createPayable(item.itemId(), item.merchantId(), excessI, SOURCE_PLATFORM_INCENTIVE_SUBSIDY,
                                "settle-pis-", ctx.projectId(), item.payeeProjectId(), schedule)
                        : null;
                // 明细一列 ar_ap_item_id 回写以池内行为主键；availablePool=0 全超额时回写补贴行（tdId=null）。
                primaryArApId = tdId != null ? tdId : pisId;
            } else {
                // 未超池 or 专属直付 or 追收明细中的正额：单条 ticket_distribution 全额。
                primaryArApId = createPayable(item.itemId(), item.merchantId(), a, SOURCE_TICKET_DISTRIBUTION, "settle-td-",
                        ctx.projectId(), item.payeeProjectId(), schedule);
            }
            // 回写仅当仍 draft（并发下另一过账已置 posted 则 0 行，跳过，不重复）。
            repository.markItemPosted(item.itemId(), primaryArApId);
            posted++;
        }

        // FS-013（BSM L755「单店门票综合服务费按前述计费月汇总后的应收总额另行收取，同样走三个付账日……平台内部各主体之间的
        //   应收应付，一律按总额收付、收支两条线，不得轧差」）：本单所属项目×月份的单店服务费计费月，按 min(Σ未退票逐票费, 锁定封顶)
        //   各挂一笔酒吧应收，与上面的票款应付分别收付；0 元不挂。台账与付款时点的项目取计费月锁定的售票项目（平台单不丢项目）。
        for (var month : singleBarServiceFees.lockUnpostedMonths(billId)) {
            long total = singleBarServiceFees.monthTotal(month.id());
            if (total > 0L) {
                String receivable = post("receivable", month.id(), month.barMerchantId(), total,
                        SingleBarServiceFeeService.SOURCE_TYPE, "settle-sbf-", month.projectId(), month.projectId(), schedule);
                singleBarServiceFees.markPosted(month.id(), receivable);
            }
        }

        // 全部待过账明细清零 → 置 settled（幂等：markBillSettled 仅当仍 confirmed 生效）。
        if (!repository.hasUnpostedItems(billId)) {
            repository.markBillSettled(billId);
        }
        return posted;
    }

    /**
     * 通票分账过账挂平台→酒吧应付（direction='payable'、source_id=settlementItemId）。
     * 幂等键={@link #idempotencyKey(String, String)}（前缀 + 明细 id 短哈希），靠
     * (direction,source_type,source_id) 唯一 + PK=arap_&lt;短哈希 slug&gt; 双保险，重复过账不重复插。
     * 返回 ar_ap_item_id。
     *
     * <p>D7 两 source_type 各用<b>独立前缀</b>（{@code settle-td-} / {@code settle-pis-}）→ 同一明细 id
     * 的池内行与补贴行哈希 slug 互不相撞，各自独立幂等；同 source_type 重复过账哈希相同 → 幂等命中不重复插。
     */
    private String createPayable(String settlementItemId, String merchantId, long amountCent, String sourceType,
                                 String idempotencyPrefix, String billProjectId, String payeeProjectId,
                                 java.util.function.BiFunction<String, String, InternalPayDayService.Schedule> schedule) {
        // 🔴 P2:显式项目归属——platform_incentive_subsidy 恒平台域 NULL(不进项目公司聚合)、ticket_distribution 取 bill.projectId;
        //   不继承过账人 token 项目域(防项目域管理员经网关过账错打项目)。
        String explicitProjectId = SOURCE_PLATFORM_INCENTIVE_SUBSIDY.equals(sourceType) ? null : billProjectId;
        return post("payable", settlementItemId, merchantId, amountCent, sourceType, idempotencyPrefix, explicitProjectId,
                payeeProjectId, schedule);
    }

    /** 挂一笔普通内部结算往来并按往来对方所属项目固化其付款时点（到期日＝该钱路推算出的有效付账日）。 */
    private String post(String direction, String sourceId, String counterpartyId, long amountCent, String sourceType,
                        String idempotencyPrefix, String projectId, String payeeProjectId,
                        java.util.function.BiFunction<String, String, InternalPayDayService.Schedule> schedule) {
        InternalPayDayService.Schedule s = schedule.apply(sourceType, payeeProjectId);
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                idempotencyKey(idempotencyPrefix, sourceId),
                sourceType + " " + direction + ": " + sourceId,
                List.of(),
                direction,
                counterpartyId,
                sourceType,
                sourceId,
                amountCent,
                s.dueDate(),
                YearMonth.from(s.dueDate()).toString()
        ), projectId);
        payDays.record(item, s, payeeProjectId);
        return item.id();
    }

    /**
     * 过账幂等键：{@code <prefix><明细 id 的 SHA-256 前 20 hex>}。
     *
     * <p>直接用 {@code "settle-ticket-dist-"+明细id} 会被 {@link FinanceService#createArApItem}
     * 内部 {@code slug()} 截断 40 字符——标准明细 id {@code si_sb_<projectId>_<YearMonth>_<merchant>}
     * 的商户判别位落在 40 字符之后，同一结算单多家酒吧的应付 PK({@code arap_<slug40>}) 相撞、被
     * ON CONFLICT DO UPDATE 覆盖成 1 条 → <b>资金丢账</b>。改用定长短哈希后：
     * ① 天然 &lt;40 字符不受截断；② 明细 id 一一对应、各家键互不相撞 → N 家落 N 条独立应付；
     * ③ 输入相同（同一明细同一前缀重复过账）哈希相同 → 幂等不重复插。前缀区分 td/pis 两口径。
     */
    private static String idempotencyKey(String prefix, String settlementItemId) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(settlementItemId.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(40);
            for (int i = 0; i < 10; i++) { // 10 字节 = 20 hex
                hex.append(Character.forDigit((digest[i] >> 4) & 0xF, 16));
                hex.append(Character.forDigit(digest[i] & 0xF, 16));
            }
            return prefix + hex; // settle-td-(10)/settle-pis-(11) + 20 = 30/31 字符，< 40 不截断
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e); // JRE 必带，理论不达
        }
    }
}
