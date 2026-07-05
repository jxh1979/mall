package com.complex.platform.settlement;

import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.ArApItemRequest;
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
 * <p>状态流转：结算单 draft →（引擎校验通过）confirmed →（本服务过账）settled；
 * 明细 draft →（本服务过账）posted。
 */
// ponytail: 过账直接调 FinanceService.createArApItem(public，与 bar 域 createPayableForGiftSettlement 同底层)。
//   不复用 bar 域 BarReceivableLedgerService——该接口 package-private 且签名吃 bar 领域记录，跨域耦合不划算。
@Service
public class SettlementPostingService {

    private static final String STATUS_CONFIRMED = "confirmed";

    private final JdbcSettlementBillRepository repository;
    private final FinanceService financeService;

    public SettlementPostingService(JdbcSettlementBillRepository repository, FinanceService financeService) {
        this.repository = repository;
        this.financeService = financeService;
    }

    /**
     * 过账某结算单：仅 confirmed 单生效；遍历待过账明细挂 finance 应付、回写、全过后置 settled。
     *
     * @param billId 结算单 id
     * @return 本次实际过账（新建或幂等命中）的明细笔数
     */
    @Transactional
    public int postBill(String billId) {
        String status = repository.findBillStatus(billId);
        if (status == null) {
            throw com.complex.platform.common.api.ApiException.notFound(
                    "SETTLEMENT_BILL_NOT_FOUND", "settlement bill not found: " + billId);
        }
        // 仅 confirmed 过账：draft=超总池待特批（特批确认→confirmed 后再过）；settled/reversed=不重复过。
        if (!STATUS_CONFIRMED.equals(status)) {
            throw com.complex.platform.common.api.ApiException.conflict(
                    "SETTLEMENT_BILL_NOT_CONFIRMED",
                    "only confirmed bills can be posted; current status=" + status);
        }

        List<JdbcSettlementBillRepository.PostableItem> items = repository.findPostableItems(billId);
        int posted = 0;
        for (var item : items) {
            String arApItemId = createPayableForTicketDistribution(
                    item.itemId(), item.merchantId(), item.finalAmountCent(), LocalDate.now().plusDays(30));
            // 回写仅当仍 draft（并发下另一过账已置 posted 则 0 行，跳过，不重复）。
            repository.markItemPosted(item.itemId(), arApItemId);
            posted++;
        }

        // 全部待过账明细清零 → 置 settled（幂等：markBillSettled 仅当仍 confirmed 生效）。
        if (!repository.hasUnpostedItems(billId)) {
            repository.markBillSettled(billId);
        }
        return posted;
    }

    /**
     * 通票分账过账：按明细挂平台→酒吧应付（direction='payable'、source_type='ticket_distribution'、
     * source_id/幂等键=settlementItemId）。幂等靠 (direction,source_type,source_id) 唯一 + PK=arap_&lt;itemId&gt;，
     * 重复过账命中已存在项、不重复插。返回 ar_ap_item_id。
     */
    String createPayableForTicketDistribution(
            String settlementItemId, String merchantId, long amountCent, LocalDate dueDate) {
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                "settle-ticket-dist-" + settlementItemId,
                "ticket distribution settlement payable: item " + settlementItemId,
                List.of(),
                "payable",
                merchantId,
                "ticket_distribution",
                settlementItemId,
                amountCent,
                dueDate,
                YearMonth.from(dueDate).toString()
        ));
        return item.id();
    }
}
