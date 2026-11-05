package com.complex.platform.bar;

// 文件版本：V0.04

import com.complex.platform.finance.ArApItemRecord;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.ArApItemRequest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class FinanceBarReceivableLedgerService implements BarReceivableLedgerService {

    private final FinanceService financeService;
    // TK-20-R2：退款按收款相反顺序逐笔原路退回——储值收款部分回原卡（StoredValueService.refundConsumption），其余渠道仍挂应付。
    private final BarRepository barRepository;
    private final com.complex.platform.customer.CustomerRepository customerRepository;
    private final com.complex.platform.customer.StoredValueService storedValueService;

    /** 旧构造（单测/内存双子）：无结算流水与储值依赖，退款全部挂应付（既有行为）。 */
    FinanceBarReceivableLedgerService(FinanceService financeService) {
        this(financeService, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    FinanceBarReceivableLedgerService(FinanceService financeService, BarRepository barRepository,
                                      com.complex.platform.customer.CustomerRepository customerRepository,
                                      // @Lazy 断环：ledger → StoredValueService → CustomerService → BarOrderService → ledger
                                      @org.springframework.context.annotation.Lazy
                                      com.complex.platform.customer.StoredValueService storedValueService) {
        this.financeService = financeService;
        this.barRepository = barRepository;
        this.customerRepository = customerRepository;
        this.storedValueService = storedValueService;
    }

    // 第十八批第 1 条：挂账应收、退款与赠饮应付的落账日按该酒吧的营业日（ENUM:187），不按服务器时区自然日。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.complex.platform.common.BusinessDayResolver businessDays = new com.complex.platform.common.BusinessDayResolver();

    private LocalDate today(String barId) {
        return businessDays.businessDate(null, barId, java.time.Instant.now());
    }

    @Override
    public boolean isBusinessDayLocked(LocalDate businessDate) {
        return financeService.isBusinessDayLocked(businessDate);
    }

    @Override
    public void postShiftOverageIncome(String shiftId, String barId, long amountCent) {
        financeService.postBarShiftOverageIncome(shiftId, barId, amountCent);
    }

    @Override
    public String createReceivableForOnAccountBill(
            BarBillRecord bill,
            BarOrderRecord order,
            String idempotencyKey,
            String operatorNote,
            List<String> evidenceAttachmentIds
    ) {
        LocalDate today = today(bill.barId());
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                "bar-on-account-" + bill.id(),
                "bar bill on account receivable: " + bill.id(),
                List.of(),
                "receivable",
                counterpartyId(bill, order),
                "bar_bill",
                bill.id(),
                bill.payableAmountCent(),
                today.plusDays(30),
                YearMonth.from(today).toString()
        ));
        return item.id();
    }

    @Override
    public String createRefundPayable(
            BarBillRecord bill,
            BarOrderRecord order,
            String adjustmentId,
            long refundAmountCent,
            String idempotencyKey,
            String reason,
            List<String> evidenceAttachmentIds
    ) {
        LocalDate today = today(bill.barId());
        // TK-20-R2（ENUM「退款资金流向」/ BSM「退款资金流向」：「混合支付的退款按收款相反顺序逐笔原路退回（后收的先退，单笔退足再退上一笔）；
        // 储值部分回冲仍按先赠送金后本金的既有顺序」；CG-06a 第 12 条「酒吧消费获准退款均回原卡」）：
        // 按该账单结算流水倒序分配本次退款额，已退额（之前 applied 的 refund 减 refund_reversal）先从末笔起占满；
        // settle_channel=stored_value 的部分回冲原消费流水（barsv_<结算幂等键>），其余渠道合并为一条应付台账项。
        long alreadyRefunded = barRepository == null ? 0L : barRepository.listBillAdjustments(bill.id()).stream()
                .filter(a -> "applied".equals(a.status()))
                .mapToLong(a -> "refund".equals(a.adjustmentType()) ? a.amountCent()
                        : "refund_reversal".equals(a.adjustmentType()) ? -a.amountCent() : 0L)
                .sum();
        List<BarBillSettlementRecord> settlements = barRepository == null || storedValueService == null
                ? new java.util.ArrayList<>() : new java.util.ArrayList<>(barRepository.listBillSettlements(bill.id()));
        java.util.Collections.reverse(settlements);
        long skip = Math.max(0L, alreadyRefunded);
        long left = refundAmountCent;
        long otherChannelsCent = 0L;
        int seq = 0;
        for (BarBillSettlementRecord settlement : settlements) {
            if (left <= 0) {
                break;
            }
            long capacity = settlement.settlementAmountCent();
            long skipped = Math.min(capacity, skip);
            skip -= skipped;
            capacity -= skipped;
            if (capacity <= 0) {
                continue;
            }
            long part = Math.min(capacity, left);
            left -= part;
            if ("stored_value".equals(settlement.settleChannel())) {
                String consumeKey = "barsv_" + settlement.id().substring("settle_".length());
                var consume = customerRepository.findStoredValueTransactionByIdempotencyKey(consumeKey)
                        .orElseThrow(() -> com.complex.platform.common.api.ApiException.conflict(
                                "STORED_VALUE_CONSUME_NOT_FOUND", "找不到该结算对应的储值消费流水"));
                storedValueService.refundConsumption(consume.id(), part, "酒吧账单退款 " + adjustmentId,
                        "barsvrev_" + adjustmentId + "_" + (seq++));
            } else {
                otherChannelsCent += part;
            }
        }
        // 结算流水之外收进的款（第十七批：挂账应收由财务登记收回的，作废时原路待退）没有可回的卡，同其余渠道挂应付由财务按原收款方式结清；
        // 旧构造（单测 / 内存双子，无结算流水与储值依赖）因此退款全部挂应付（既有行为）。
        otherChannelsCent += left;
        if (otherChannelsCent <= 0) {
            return null;
        }
        // RC1①：退款过账为 direction='payable'、source_type='refund' 的应付台账项（"应退/已退"）。
        // source_id 取退款调整记录 id（唯一），与 V008 唯一索引 (direction, source_type, source_id) 对齐，幂等。
        // 退款过账与退款主流程（V079 减项）同事务执行（applyBillAdjustment @Transactional / createArApItem @Transactional
        // 复用同一外层事务）：过账失败将回滚退款，保证财务台账与账单减项强一致，不出现"退了钱但台账无记录"。
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                "bar-refund-" + adjustmentId,
                "bar bill refund payable: " + bill.id() + " (" + reason + ")",
                evidenceAttachmentIds == null ? List.of() : List.copyOf(evidenceAttachmentIds),
                "payable",
                counterpartyId(bill, order),
                "refund",
                adjustmentId,
                otherChannelsCent,
                today,
                YearMonth.from(today).toString()
        ));
        return item.id();
    }

    @Override
    public long reverseStoredValueRefund(BarBillRecord bill, BarOrderRecord order, String originalAdjustmentId,
                                         String reversalIdempotencyKey, String reason) {
        // TK-20-R2 退品驳回对称回滚：原退款调整回冲到卡上的金额（barsvrev_<调整id>_<序>）按发卡商户卡重新扣回；
        // 卡余额不足 / 冻结即按既有消费守卫拒绝，驳回整笔回滚（fail-closed）。
        if (customerRepository == null || storedValueService == null) {
            return 0L;
        }
        // 复核 money I3：逐笔撤回原退款回冲（barsvrev_<调整id>_<序>）——写回指原消费的负向 consume_reversal，
        // 原消费的已回冲累计随之净额扣减，之后同一账单再退款可再回冲。
        long credited = 0L;
        for (int seq = 0; ; seq++) {
            var reversal = customerRepository.findStoredValueTransactionByIdempotencyKey(
                    "barsvrev_" + originalAdjustmentId + "_" + seq).orElse(null);
            if (reversal == null) {
                break;
            }
            storedValueService.undoConsumptionRefund(reversal.id(), "退品驳回：扣回已回冲储值 " + originalAdjustmentId,
                    "barsvund_" + BarValidation.slug(reversalIdempotencyKey) + "_" + seq);
            credited += reversal.amountCent();
        }
        return credited;
    }

    @Override
    public String reverseRefundPayable(
            BarBillRecord bill,
            BarOrderRecord order,
            String reversalAdjustmentId,
            long refundAmountCent,
            String idempotencyKey,
            String reason,
            List<String> evidenceAttachmentIds
    ) {
        LocalDate today = today(bill.barId());
        // 延期项④：驳回冲销过账为 direction='receivable'、source_type='refund' 的对冲应收项。
        // source_id 取驳回冲销调整记录 id（与原退款应付项 source_id 不同），direction 亦不同，
        // 与 V008 唯一索引 (direction, source_type, source_id) 对齐，冲销项与原退款项净额相抵。
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                "bar-refund-reversal-" + reversalAdjustmentId,
                "bar bill refund reversal (void rejected): " + bill.id() + " (" + reason + ")",
                evidenceAttachmentIds == null ? List.of() : List.copyOf(evidenceAttachmentIds),
                "receivable",
                counterpartyId(bill, order),
                "refund",
                reversalAdjustmentId,
                refundAmountCent,
                today,
                YearMonth.from(today).toString()
        ));
        return item.id();
    }

    @Override
    public String createDepositRefundPayable(BarReservationRecord reservation) {
        LocalDate today = today(reservation.barId());
        // 第十批 TK-24-R3（BSM L1062「达标时，各笔定金分别原路退回」「退款处理中的记录继续关联原付款，只有实际退款成功才记为已退」）：
        // 定金达标退回过账为 direction='payable'、source_type='bar_deposit_refund'、source_id=订台 id 的应付退款项
        // （V008 唯一索引天然幂等）；原付款渠道与意向随说明留痕供「原路」实付，实付由既有 finance 收付款登记完成。
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                "bar-deposit-refund-" + reservation.id(),
                "bar reservation deposit refund on min-spend threshold: " + reservation.id()
                        + " (channel " + reservation.depositChannel() + ", intent " + reservation.depositIntentId() + ")",
                List.of(),
                "payable",
                reservation.customerId() != null && !reservation.customerId().isBlank()
                        ? reservation.customerId() : reservation.id(),
                "bar_deposit_refund",
                reservation.id(),
                reservation.depositAmountCent(),
                today,
                YearMonth.from(today).toString()
        ));
        return item.id();
    }

    @Override
    public boolean hasDepositRefundPayable(String reservationId) {
        return barRepository != null && barRepository.depositRefundPayableExists(reservationId);
    }

    @Override
    public String createPayableForGiftSettlement(String verificationId, String merchantId, long internalPriceCent) {
        LocalDate today = today(businessDays.barOfMerchant(merchantId));
        // 通票免费酒水：核销发放 → 平台按内部结算价向酒吧(merchant)挂应付。源/幂等键 = verificationId（一次核销至多一笔）。
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                "bar-gift-settle-" + verificationId,
                "platform-to-bar pass drink settlement: verification " + verificationId,
                List.of(),
                "payable",
                merchantId,
                "pass_drink_settlement",
                verificationId,
                internalPriceCent,
                today.plusDays(30),
                YearMonth.from(today).toString()
        ));
        return item.id();
    }

    @Override
    public String reverseGiftSettlementPayable(String verificationId, String merchantId, long internalPriceCent) {
        // S6 根因守卫：冲销一笔「从未挂出」的应付必须 no-op——原核销因商户让利型(cost_bearer=merchant_concession)、
        //   自营吧等被跳过挂应付时,这里若照冲会凭空造一笔反向应收(商户平白欠平台一笔钱)。
        //   查的是「当时到底挂没挂」的事实,不查当下配置 → 承担方中途被改也不会错冲。
        if (!financeService.arApItemExistsBySource("payable", "pass_drink_settlement", verificationId)) {
            return null;
        }
        LocalDate today = today(businessDays.barOfMerchant(merchantId));
        // 赠饮冲正 → 对冲应收（同 source_id=verificationId、direction 不同 → 与原应付 V008 唯一索引不冲突且净额归零）。
        ArApItemRecord item = financeService.createArApItem(new ArApItemRequest(
                "bar-gift-settle-reverse-" + verificationId,
                "reverse platform-to-bar pass drink settlement: verification " + verificationId,
                List.of(),
                "receivable",
                merchantId,
                "pass_drink_settlement",
                verificationId,
                internalPriceCent,
                today.plusDays(30),
                YearMonth.from(today).toString()
        ));
        return item.id();
    }

    private String counterpartyId(BarBillRecord bill, BarOrderRecord order) {
        if (order.customerId() != null && !order.customerId().isBlank()) {
            return order.customerId();
        }
        if (bill.reservationId() != null && !bill.reservationId().isBlank()) {
            return bill.reservationId();
        }
        return bill.tableId();
    }
}
