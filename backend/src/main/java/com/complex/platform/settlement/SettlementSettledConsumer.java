package com.complex.platform.settlement;

import com.complex.platform.ticketing.SettlementSettledEvent;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.stereotype.Component;

/**
 * 结算重构批1：核销结算双写消费者。
 *
 * <p>监听 ticketing 域 {@link SettlementSettledEvent}，向 settlement_transactions 写一条 verification_settled 流水
 * （按 entitlement 幂等 upsert）。观察期用途——旧口径(allocated 快照)与新口径(逐笔结算)按日对账。
 *
 * <p><b>AFTER_COMMIT + REQUIRES_NEW</b>：本消费不属核销主链——核销主事务提交后才触发，且开新事务写入，
 * 故本消费失败不回滚核销（双写观察表容忍最终一致，与 analytics 口径一致）。
 *
 * <p><b>金额口径</b>（整数分，verified+unverified=allocated 恒成立）：
 * verified = round(allocated × usedTimes / totalTimes)；unverified = allocated − verified。
 * allocatedRevenueCent==null（direct 出票无分摊快照）→ 跳过不写（保守，与 analytics 一致）。
 *
 * <p><b>幂等</b>：同一权益再次核销（usedTimes 变）命中部分唯一索引 uq_st_entitlement_verified → DO UPDATE 累积，
 * 不重复插入。
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

        // 餐饮结算(用户2026-07-06 拍板)：专属商户「不分摊」+ 有内部结算价 → 按【内部结算价 × 核销数】全额直付该商户，
        //   不走 allocated 比例分摊(否则纯赠送 allocated=0/折扣价会把内部结算价错算)。§6.4 exclusive_merchant_id
        //   已使月度成账全额归 exclusive_merchant。allocated 保留供旧口径对账(可空则记 0)。
        //   条件 exclusiveMerchantId!=null 现仅餐饮命中(其余新建模板均非专属)，故不改动 bar/游乐现状分摊。
        boolean exclusiveByPrice = event.exclusiveMerchantId() != null && event.settlementPriceCent() != null;

        long allocatedForRow;
        long verified;
        long unverified;
        if (exclusiveByPrice) {
            long price = event.settlementPriceCent();
            verified = Math.max(0L, price * Math.max(0, used));
            // allocated 行值取「结算价 × 总次数」作为该权益应结总额(未核销部分=剩余次数×价);缺 allocated 时不依赖它。
            allocatedForRow = Math.max(verified, price * Math.max(0, total));
            unverified = allocatedForRow - verified;
        } else {
            if (allocated == null) {
                // direct 出票无分摊快照且非专属定价：保守跳过不写（与 analytics 一致，避免凭空造 net）。
                return;
            }
            // total<=0 理论不应发生（发票时至少 1 次），防御性置全额已核销避免除零。
            verified = total <= 0 ? allocated : Math.round((double) allocated * used / total);
            // 夹取到 [0, allocated]，再由差值定 unverified，保证两段和恒等 allocated（无舍入漂移）。
            verified = Math.max(0L, Math.min(allocated, verified));
            allocatedForRow = allocated;
            unverified = allocated - verified;
        }

        repository.upsertVerificationSettled(new SettlementTransactionRecord(
                "st_vs_" + event.entitlementId(),
                "ST-VS-" + event.entitlementId(),
                TRANSACTION_TYPE_VERIFICATION_SETTLED,
                event.entitlementId(),
                event.orderId(),
                event.ticketTypeId(),
                null, // verification_id：批1按权益累积，不绑单次核销记录（NULL）
                event.entitlementType(),
                used,
                total,
                event.numPeople(),
                allocatedForRow,
                verified,
                unverified,
                BENEFICIARY_MERCHANT,
                event.merchantId(),
                null, // project_id：批1暂留 NULL
                STATUS_DRAFT,
                event.validTo(),
                // 批3-4：专属商户随核销定格进 settlement_transactions（空=非专属分池）。
                event.exclusiveMerchantId()
        ));
    }
}
