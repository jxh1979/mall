package com.complex.platform.ticketing;

// 文件版本：V0.02

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
 * @param fullPriceCent        D6 该权益满额份额(分,可空)。消费者用与 allocated 同一增量公式算 verifiedFull,折扣增量=max(0,verifiedFull−verified)；
 *                             为空(component_price 全空/direct 出票)→折扣恒 0(零行为变更)。
 * @param projectId            S2：本次核销所属项目公司（综合体）scope。取自核销人 token 派生的项目归属
 *                             （{@code AuthorizationService.currentProjectScopeQuietly()}，与提成费率/高价值阈值同一取数口径），
 *                             平台级操作者=null=平台默认口径。<b>消费者用它同时做两件事</b>：解析综合服务费费率规则的项目维度、
 *                             写入 {@code settlement_transactions.project_id}——两处同一个值,不再各写各的常量,
 *                             项目级费率特例这才有可能命中（原先调用处硬编码 null，项目级规则永远解析不到）。
 * @param parentTicketId       S3：本次核销的权益属于哪一张票（{@code issued_entitlements.parent_ticket_id}）。
 *                             「门票综合服务费一张票只收一道」的判定锚点——消费者据此判断该票是否已收过这道费。
 *                             为空 = 不合规发放数据（V285 起每次出票都建父票实例，新发放恒有）→ 消费者<b>拒绝计提</b>，
 *                             不退化成「按核销笔数各收一道」。
 * @param compensationItemId   FS-012：补偿补发权益的补偿记录 id（非补偿核销为 null）。补偿新增履约由承担主体与提供方另行结算
 *                             （补偿履约记录 + 补偿应付），结算消费者对其不写普通核销结算事实，原票分账不重复计（BSM L800、CG 第 39 条）。
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
        Long settlementPriceCent,
        Long fullPriceCent,
        String projectId,
        String parentTicketId,
        String compensationItemId
) {

    /** 兼容构造（非补偿核销，compensationItemId=null）：既有测试零改动。 */
    public SettlementSettledEvent(String verificationId, Instant verifiedAt, String entitlementId, String orderId,
            String ticketTypeId, String entitlementType, String merchantId, int totalTimes, int usedTimes, int numPeople,
            Long allocatedRevenueCent, Instant validTo, String exclusiveMerchantId, Long settlementPriceCent,
            Long fullPriceCent, String projectId, String parentTicketId) {
        this(verificationId, verifiedAt, entitlementId, orderId, ticketTypeId, entitlementType, merchantId, totalTimes,
                usedTimes, numPeople, allocatedRevenueCent, validTo, exclusiveMerchantId, settlementPriceCent, fullPriceCent,
                projectId, parentTicketId, null);
    }
}
