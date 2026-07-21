package com.complex.platform.ticketing;

import java.time.Instant;

/**
 * 结算重构批1 事件 {@code settlement.settled}：核销成功后发布，用于向 settlement_transactions 双写核销结算流水。
 *
 * <p>异步发布（{@code @TransactionalEventListener(AFTER_COMMIT)}）：本事件不属于核销主链，
 * 核销主流程照常提交后才触发下游写入；下游消费失败不回滚核销（双写观察表，容忍最终一致）。
 * 与 {@link PointsConfirmRequestedEvent}（同步入账，属主链）不同——积分是业务闭环，本事件仅记账观察。
 *
 * <p>跨模块解耦：settlement 域依赖 ticketing，ticketing 不反向注入 settlement，故事件 record 置于 ticketing，
 * 由 settlement 域 {@code SettlementSettledConsumer} 监听（与 customer/bar 消费 ticketing 事件同范式）。
 *
 * @param verificationId       本次核销记录 id（AUD-012 不可变增量结算的幂等键=transaction_no ST-VS-&lt;verificationId&gt;）
 * @param verifiedAt           本次核销业务发生时刻（AUD-012 结算按此落月窗，取代首插 created_at 的月份漂移）
 * @param entitlementId        被核销的 issued_entitlement id（溯源）
 * @param orderId              出票订单 id（可空：direct 出票无订单）
 * @param ticketTypeId         票种 id
 * @param entitlementType      权益类型（bar/amusement/ticket/gift_drink...）
 * @param merchantId           商户 id（beneficiary_type=merchant 时的受益方）
 * @param totalTimes           权益总次数
 * @param usedTimes            当前已核销次数（本次核销后的最新值）
 * @param numPeople            核销人数
 * @param allocatedRevenueCent 权益总分配收入快照（可空：direct 出票为 null → 消费者跳过不写）
 * @param validTo              权益有效期截止（过期判定基线，写入 entitlement_valid_to）
 * @param exclusiveMerchantId  结算重构批3-4(PRD §6.4)：专属某商户「不分摊」，非空=该权益核销收入全额归该商户（不进池）；空=非专属分池
 * @param settlementPriceCent  本次核销结算价(分,可空)——resolveSettlementPrice 结果。餐饮等专属权益按此价×核销数直付商户(内部结算价口径)；
 *                             为空或非专属时消费者仍按 allocated 分摊(现状,与旧口径对账)。
 */
public record SettlementSettledEvent(
        String verificationId,
        java.time.Instant verifiedAt,
        String entitlementId,
        String orderId,
        String ticketTypeId,
        String entitlementType,
        String merchantId,
        int totalTimes,
        int usedTimes,
        int numPeople,
        Long allocatedRevenueCent,
        Instant validTo,
        String exclusiveMerchantId,
        Long settlementPriceCent
) {
}
