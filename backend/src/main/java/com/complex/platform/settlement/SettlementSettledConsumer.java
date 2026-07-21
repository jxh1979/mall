package com.complex.platform.settlement;

import com.complex.platform.ticketing.SettlementSettledEvent;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.stereotype.Component;

/**
 * 结算状态机重构(AUD-012/017)：核销结算消费者——每次核销写一条<b>不可变增量</b> verification_settled 事实。
 *
 * <p>监听 ticketing 域 {@link SettlementSettledEvent}，向 settlement_transactions append 一行，
 * key=transaction_no {@code ST-VS-<verificationId>}（幂等：同一核销重放 ON CONFLICT DO NOTHING，永不 UPDATE）。
 * account 按 {@code business_at}=核销业务时刻聚合增量，跨月核销各归其月，不再用累计值覆盖首插行（AUD-012 根因）。
 *
 * <p><b>AFTER_COMMIT + REQUIRES_NEW</b>：本消费不属核销主链——核销主事务提交后才触发，且开新事务写入，
 * 故本消费失败不回滚核销（append-only 账本容忍最终一致；缺失由重建任务按核销权威事实补建，AUD-017）。
 *
 * <p><b>金额口径</b>（整数分，本行只记本次增量，unverified 恒 0；未核销到期归项目是独立事实）：
 * verified = f(usedTimes) − f(usedTimes−1)（{@link SettlementAmounts}，逐笔 telescope 回累计，无漂移）。
 * allocatedRevenueCent==null 且非专属定价（direct 出票无分摊快照）→ 跳过不写（与 analytics 一致）。
 */
@Component
public class SettlementSettledConsumer {

    private static final String TRANSACTION_TYPE_VERIFICATION_SETTLED = "verification_settled";
    private static final String BENEFICIARY_MERCHANT = "merchant";
    private static final String STATUS_DRAFT = "draft";

    private final JdbcSettlementTransactionRepository repository;

    public SettlementSettledConsumer(JdbcSettlementTransactionRepository repository) {
        this.repository = repository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSettlementSettled(SettlementSettledEvent event) {
        int total = event.totalTimes();
        int used = event.usedTimes();
        Long allocated = event.allocatedRevenueCent();
        boolean exclusiveByPrice = SettlementAmounts.exclusiveByPrice(
                event.exclusiveMerchantId(), event.settlementPriceCent());

        if (!exclusiveByPrice && allocated == null) {
            // direct 出票无分摊快照且非专属定价：保守跳过不写（与 analytics 一致，避免凭空造 net）。
            return;
        }
        // 本次核销的不可变增量已核销金额（第 used 次核销的增量 = f(used)−f(used−1)，恒 >=0）。
        long verified = SettlementAmounts.incrementalVerified(
                allocated, total, used, exclusiveByPrice, event.settlementPriceCent());

        repository.insertImmutableFact(new SettlementTransactionRecord(
                "st_vs_" + event.verificationId(),
                "ST-VS-" + event.verificationId(),
                TRANSACTION_TYPE_VERIFICATION_SETTLED,
                event.entitlementId(),
                event.orderId(),
                event.ticketTypeId(),
                event.verificationId(), // AUD-012：绑本次核销记录（不可变增量行的溯源+幂等锚点）
                event.entitlementType(),
                1,                       // verified_count：本行=本次核销 1 次（累计由多行相加）
                total,
                event.numPeople(),
                verified,                // allocated_revenue_cent：增量行等于本次已核销额（无未核销余量）
                verified,
                0L,                      // unverified_amount_cent：增量行恒 0（未核销到期归项目是独立事实）
                BENEFICIARY_MERCHANT,
                event.merchantId(),
                null, // project_id：暂留 NULL（平台默认口径）
                STATUS_DRAFT,
                event.validTo(),
                event.exclusiveMerchantId(),
                event.verifiedAt()       // business_at：核销业务时刻（account 月窗聚合基准）
        ));
    }
}
