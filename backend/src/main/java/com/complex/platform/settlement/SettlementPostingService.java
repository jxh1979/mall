package com.complex.platform.settlement;

import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.ArApItemRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 结算重构批2-4：月度分账结算单 <b>过账</b> 到 finance 应付台账（ar_ap_items）。
 *
 * <p>与计算引擎 {@link TicketDistributionSettlementService} 解耦：引擎算+校验+写 draft/confirmed，
 * 过账单独触发（便于超总池 draft 单人工特批调整后再过）。本服务不改引擎算法。
 *
 * <p>过账口径（照 bar 域 createPayableForGiftSettlement 范式）：
 * <ul>
 *   <li>仅对 status='confirmed' 的结算单过账（draft=超总池待特批，拒绝并说明）；</li>
 *   <li>遍历 settlement_items（status='draft' 且 final_amount_cent&gt;0）→ 生成
 *       ar_ap_items（direction='payable'、source_type='ticket_distribution'、source_id=明细 id、
 *       counterparty_id=酒吧 merchant、amount=final_amount_cent）→ 回写 ar_ap_item_id + status='posted'；
 *       final=0 天然跳过（不取入待过账集）；</li>
 *   <li>全部明细 posted 后 → 结算单 status='settled'、settled_at=now；</li>
 *   <li>幂等：明细 status='posted' 已过即跳过；ar_ap 唯一索引 (direction,source_type,source_id) +
 *       PK=arap_&lt;明细 id&gt; 双保险，重复过账不产生第二条应付。</li>
 * </ul>
 *
 * <p><b>D7 超额 C 口径（甲标签法）</b>：仅当 bill <b>超池</b>（池分账合计 poolDistributed &gt; spvPool）时，
 * 对<b>池明细</b>（method≠exclusive_full）两段拆——池内额 {@code pool_i = a_i*spvPool/poolDistributed}
 * 过 {@code ticket_distribution}（项目公司承担）、超额 {@code excess_i = a_i-pool_i} 过
 * {@code platform_incentive_subsidy}（L3 娱乐运营平台承担，独立幂等前缀 {@code settle-pis-} 防与 td 行撞）。
 * 专属直付 item（exclusive_full）永不拆、全额 ticket_distribution；未超池 bill 完全现状（单条）。
 *
 * <p>状态流转：结算单 draft →（引擎校验通过 or 超池特批）confirmed →（本服务过账）settled；
 * 明细 draft →（本服务过账）posted。
 */
// ponytail: 过账直接调 FinanceService.createArApItem(public，与 bar 域 createPayableForGiftSettlement 同底层)。
//   不复用 bar 域 BarReceivableLedgerService——该接口 package-private 且签名吃 bar 领域记录，跨域耦合不划算。
@Service
public class SettlementPostingService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String METHOD_EXCLUSIVE_FULL =
            TicketDistributionSettlementService.METHOD_EXCLUSIVE_FULL;
    private static final String SOURCE_TICKET_DISTRIBUTION = "ticket_distribution";
    private static final String SOURCE_PLATFORM_INCENTIVE_SUBSIDY = "platform_incentive_subsidy";

    private final JdbcSettlementBillRepository repository;
    private final FinanceService financeService;
    private final com.complex.platform.evidence.AuditEventService auditEventService;

    public SettlementPostingService(JdbcSettlementBillRepository repository, FinanceService financeService,
                                    com.complex.platform.evidence.AuditEventService auditEventService) {
        this.repository = repository;
        this.financeService = financeService;
        this.auditEventService = auditEventService;
    }

    /**
     * D7 超池特批出口：把某<b>超池 draft</b> 结算单填 special_approval_ref 并置 confirmed（可再走 postBill 过账）。
     * 仅超池 draft 单可特批；非超池 draft 或已 confirmed/settled/reversed → 409 {@code BILL_NOT_OVERPOOL_DRAFT}。
     *
     * @param billId             结算单 id
     * @param specialApprovalRef 特批引用（平台负责人特批短期激励凭据），非空
     * @return 落库的特批引用（回显）
     */
    @Transactional
    public String specialApprove(String billId, String specialApprovalRef) {
        String ref = specialApprovalRef == null ? null : specialApprovalRef.trim();
        if (ref == null || ref.isEmpty()) {
            throw com.complex.platform.common.api.ApiException.badRequest(
                    "SPECIAL_APPROVAL_REF_REQUIRED", "special_approval_ref is required");
        }
        JdbcSettlementBillRepository.OverPoolContext ctx = repository.findOverPoolContext(billId);
        if (ctx == null) {
            throw com.complex.platform.common.api.ApiException.notFound(
                    "SETTLEMENT_BILL_NOT_FOUND", "settlement bill not found: " + billId);
        }
        // 特批出口只对「超池 draft」开：非超池的 draft 不需特批、任何已推进态(confirmed/settled/reversed)不可再特批。
        if (!STATUS_DRAFT.equals(ctx.status()) || !ctx.overPool()) {
            throw com.complex.platform.common.api.ApiException.conflict(
                    "BILL_NOT_OVERPOOL_DRAFT",
                    "only over-pool draft bills can be special-approved; status=" + ctx.status()
                            + ", overPool=" + ctx.overPool());
        }
        int rows = repository.markBillSpecialApproved(billId, ref);
        if (rows == 0) {
            // 并发下状态已被改（另一次特批/过账）→ 与上同义，再抛一次防竞态。
            throw com.complex.platform.common.api.ApiException.conflict(
                    "BILL_NOT_OVERPOOL_DRAFT", "bill is no longer a draft: " + billId);
        }
        // 治理留痕(对抗复核补):超池特批是资金级审批,记 billId/ref/超池额供追溯——谁批的短期激励超额、批了多大缺口。
        auditEventService.recordSuccess(
                "settlement.bill.special_approved", "settlement.bill.post", "settlement_bill", billId,
                "req_" + java.util.UUID.randomUUID(), null,
                java.util.Map.of("special_approval_ref", ref,
                        "spv_pool_cent", ctx.spvPoolCent(),
                        "pooled_distributed_cent", ctx.pooledDistributedCent()));
        return ref;
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
                    "SETTLEMENT_BILL_NOT_FOUND", "settlement bill not found: " + billId);
        }
        // 仅 confirmed 过账：draft=超总池待特批（特批确认→confirmed 后再过）；settled/reversed=不重复过。
        if (!STATUS_CONFIRMED.equals(ctx.status())) {
            throw com.complex.platform.common.api.ApiException.conflict(
                    "SETTLEMENT_BILL_NOT_CONFIRMED",
                    "only confirmed bills can be posted; current status=" + ctx.status());
        }

        boolean overPool = ctx.overPool();
        long spvPool = ctx.spvPoolCent();
        long poolDistributed = ctx.pooledDistributedCent(); // overPool ⇒ poolDistributed > spvPool ≥ 0 ⇒ >0，除法安全

        List<JdbcSettlementBillRepository.PostableItem> items = repository.findPostableItems(billId);
        LocalDate dueDate = LocalDate.now().plusDays(30);
        int posted = 0;
        for (var item : items) {
            String primaryArApId;
            if (overPool && !METHOD_EXCLUSIVE_FULL.equals(item.distributionMethod())) {
                // 超额 C 口径两段拆：pool_i(项目公司承担) + excess_i(L3 平台承担)，pool_i+excess_i=a_i 精确。
                long a = item.finalAmountCent();
                // 溢出护栏(对抗复核 risk1):a×spvPool 可超 Long.MAX(大场馆高营业月单笔×池),long 直乘会反转
                //   → poolI 负、excessI 灌爆。用 BigInteger 中间乘;结果 ⌊a·spvPool/poolDistributed⌋ ≤ a 恒在 long 内(longValueExact 兜底)。
                long poolI = java.math.BigInteger.valueOf(a)
                        .multiply(java.math.BigInteger.valueOf(spvPool))
                        .divide(java.math.BigInteger.valueOf(poolDistributed))
                        .longValueExact();
                long excessI = a - poolI;
                String tdId = poolI > 0
                        ? createPayable(item.itemId(), item.merchantId(), poolI, dueDate,
                                SOURCE_TICKET_DISTRIBUTION, "settle-td-")
                        : null;
                String pisId = excessI > 0
                        ? createPayable(item.itemId(), item.merchantId(), excessI, dueDate,
                                SOURCE_PLATFORM_INCENTIVE_SUBSIDY, "settle-pis-")
                        : null;
                // 明细一列 ar_ap_item_id 回写以池内行为主键；spvPool=0 全超额时回写补贴行（tdId=null）。
                primaryArApId = tdId != null ? tdId : pisId;
            } else {
                // 未超池 or 专属直付：完全现状，单条 ticket_distribution 全额。
                primaryArApId = createPayable(item.itemId(), item.merchantId(), item.finalAmountCent(), dueDate,
                        SOURCE_TICKET_DISTRIBUTION, "settle-td-");
            }
            // 回写仅当仍 draft（并发下另一过账已置 posted 则 0 行，跳过，不重复）。
            repository.markItemPosted(item.itemId(), primaryArApId);
            posted++;
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
    String createPayable(String settlementItemId, String merchantId, long amountCent, LocalDate dueDate,
                         String sourceType, String idempotencyPrefix) {
        String note = SOURCE_PLATFORM_INCENTIVE_SUBSIDY.equals(sourceType)
                ? "platform incentive subsidy (over-pool special approval): item " + settlementItemId
                : "ticket distribution settlement payable: item " + settlementItemId;
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                idempotencyKey(idempotencyPrefix, settlementItemId),
                note,
                List.of(),
                "payable",
                merchantId,
                sourceType,
                settlementItemId,
                amountCent,
                dueDate,
                YearMonth.from(dueDate).toString()
        ));
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
