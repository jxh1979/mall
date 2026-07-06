package com.complex.platform.ticketing;

import java.util.List;
import java.util.Optional;

public interface TicketingRepository {
    TicketTypeRecord saveTicketType(TicketTypeRecord ticketType);

    List<TicketTypeRecord> listTicketTypes();

    Optional<TicketTypeRecord> findTicketTypeById(String id);

    /**
     * P0 修复（V133）：原子扣减票种库存——sold_quantity += quantity，仅当不限量（stock_quantity 为 null）
     * 或扣减后不超上限时成功。返回 true=扣减成功；false=库存不足（超卖被拒）。
     * default 返回 true（不限量）兼容既有测试假实现。
     */
    default boolean atomicDeductTicketStock(String ticketTypeId, int quantity) {
        return true;
    }

    /**
     * P0 修复（V133）：释放票种库存（退款/取消时回补）——sold_quantity = GREATEST(0, sold_quantity - quantity)。
     * default 空实现兼容既有测试假实现。
     */
    default void releaseTicketStock(String ticketTypeId, int quantity) {
    }

    EntitlementTemplateRecord saveEntitlementTemplate(EntitlementTemplateRecord template);

    List<EntitlementTemplateRecord> listEntitlementTemplates(boolean includeArchived);

    /** 物理删除权益模板(审批通过后调用)。 */
    void deleteEntitlementTemplate(String id);

    /** 该模板被票种绑定/已发放权益引用的活跃条数(>0 则禁止硬删)。 */
    long countTemplateReferences(String id);

    Optional<EntitlementTemplateRecord> findEntitlementTemplateById(String id);

    TicketTypeEntitlementRecord saveTicketTypeEntitlement(TicketTypeEntitlementRecord entitlement);

    List<TicketTypeEntitlementRecord> listTicketTypeEntitlements(String ticketTypeId);

    IssuedEntitlementRecord saveIssuedEntitlement(IssuedEntitlementRecord entitlement);

    Optional<IssuedEntitlementRecord> findIssuedEntitlementByCode(String entitlementCode);

    /**
     * 结算重构批3-1(PRD §6.1)：按 bar_products.id 取实物权益的酒吧内部结算价(V152 internal_settlement_price_cent)。
     * 无此商品或内部价为空 → 空(判定层回退 value_amount)。default 返回空兼容既有测试假实现。
     */
    default Optional<Long> findBarProductInternalPrice(String barProductId) {
        return Optional.empty();
    }

    /**
     * 结算重构批3-2(PRD §6.2)：按 venue 营业时间表惰性解析当前场次。
     * 当前营业 → 取/开当日 open 场次并返回 session_id；非营业时段 → 空(调用方报 VENUE_NOT_IN_SESSION)。
     * default 返回空兼容既有测试假实现（无 per_session 权益的测试不受影响）。
     */
    default Optional<String> resolveOpenSessionForVenue(String venueId, java.time.Instant now) {
        return Optional.empty();
    }

    /**
     * 结算重构批3-2：记录该权益在该场次的首次入场(唯一键 entitlement_id+session_id)。
     * 返回 true=本次真插入(本场次首入,应消耗一次)；false=本场次已入过(放行不消耗)。default 恒 true 兼容假实现。
     */
    default boolean recordEntitlementEntry(String entitlementId, String sessionId, java.time.Instant now) {
        return true;
    }

    /** 结算重构批3-2：该权益在该场次是否已有入场记录。default false。 */
    default boolean hasEntitlementEntryInSession(String entitlementId, String sessionId) {
        return false;
    }

    /** 结算重构批3-2：该权益是否曾在别的场次入过场(session_id != 当前) → 过场作废判定。default false。 */
    default boolean hasEntitlementEntryInOtherSession(String entitlementId, String sessionId) {
        return false;
    }

    /**
     * 结算重构批3-3(PRD §6.3)：游乐「当天无限」——记录该权益在该自然日的首次激活
     * (唯一键 entitlement_id+usage_date)。返回 true=本次真插入(当天首刷,调用方应消耗一次)；
     * false=当天已激活/并发抢输(放行不消耗)。default 恒 true 兼容假实现。
     */
    default boolean recordEntitlementDayActivation(String entitlementId, java.time.LocalDate usageDate, java.time.Instant now) {
        return true;
    }

    /** 结算重构批3-3：该权益在该自然日是否已有激活记录(当天已激活→放行不消耗)。default false。 */
    default boolean hasEntitlementDayActivation(String entitlementId, java.time.LocalDate usageDate) {
        return false;
    }

    IssuedEntitlementRecord updateIssuedEntitlement(IssuedEntitlementRecord entitlement);

    List<OfflineEntitlementResponse> listOfflineEntitlements(String merchantId, String venueId, int limit);

    VerificationRecord saveVerification(VerificationRecord verification);

    List<VerificationRecord> listVerifications();

    Optional<VerificationRecord> findVerificationById(String id);

    // F-TXN-04 并发双花修复：把"原 success 核销 → reversed"做成状态 CAS。仅当 status='success' 且 reversed_at IS NULL
    // 才翻转并记 reversed_by/reversed_at，返回受影响行数。并发两路冲正只有一路得 1（CAS 胜出方），另一路得 0 →
    // 调用方据此判定"已被并发冲正"拒绝，杜绝同一核销被重复冲正、used_times 被多次回滚（多次票尤甚）。
    int markVerificationReversed(String id, String reversedBy, java.time.Instant reversedAt);

    /** F-TXN-04 退款审批状态 CAS：仅当退款单仍为 requested 才翻转 approved，返回受影响行数（并发只一路得 1，防双拨付）。 */
    int markTicketRefundApproved(String refundId, String approvedBy, java.time.Instant approvedAt);

    Optional<TerminalSyncTaskRecord> findTerminalSyncTask(String deviceCode, String clientTaskId);

    TerminalSyncTaskRecord saveTerminalSyncTask(TerminalSyncTaskRecord task);

    TicketPriceRuleRecord savePriceRule(TicketPriceRuleRecord rule);

    List<TicketPriceRuleRecord> listPriceRules();

    Optional<TicketPriceRuleRecord> findPriceRuleById(String id);

    TicketDynamicPricingRuleRecord saveDynamicPricingRule(TicketDynamicPricingRuleRecord rule);

    List<TicketDynamicPricingRuleRecord> listDynamicPricingRules();

    Optional<TicketDynamicPricingRuleRecord> findDynamicPricingRuleById(String id);

    TicketOrderRecord saveTicketOrder(TicketOrderRecord order);

    Optional<TicketOrderRecord> findTicketOrderById(String id);

    TicketOrderRecord updateTicketOrder(TicketOrderRecord order);

    List<TicketOrderRecord> listTicketOrders();

    List<TicketOrderRecord> listTicketOrdersByCustomerId(String customerId);

    // 行级隔离（V135）：按下单受理商户过滤订单列表（商户 scope 仅见自身，平台 scope 走 listTicketOrders 看全部）。
    List<TicketOrderRecord> listTicketOrdersByMerchant(String merchantId);

    TicketPaymentIntentRecord saveTicketPaymentIntent(TicketPaymentIntentRecord intent);

    // 支付回写闭环（2a-2）：读取票务支付意图（确认支付成功时按 id 校验归属/金额并回写订单 paid）。
    Optional<TicketPaymentIntentRecord> findTicketPaymentIntentById(String id);

    // 退款冻结（2a-3）：退款审批通过 → 把 active 权益置 refunded（不可再核销）。返回受影响行数（幂等：已 refunded 返回 0）。
    int freezeEntitlementForRefund(String id, java.time.Instant updatedAt);

    // 库存超时释放（2a-5）：原子取消未支付订单（仅 created+unpaid）。返回 1 时调用方才回补库存（防并发重复释放）。
    int atomicCancelUnpaidOrder(String orderId, java.time.Instant now);

    // 库存超时释放（2a-5）：列出超时未支付订单（created+unpaid 且 created_at < cutoff），供定时任务回补库存。
    List<TicketOrderRecord> listExpiredUnpaidOrders(java.time.Instant cutoff);

    /**
     * F-TXN-03 门票生命周期：列出已过期（valid_to &lt; now）、仍 active、且全未核销（used_times=0）的权益——
     * 到期自动退款 / 一张用后其余作废的候选集。default 空兼容既有测试双子。
     */
    default List<IssuedEntitlementRecord> listExpiredUnusedActiveEntitlements(java.time.Instant now) {
        return List.of();
    }

    /**
     * F-TXN-03 一张用后其余作废：把到期未用权益置 forfeited（仅 active 且 used_times=0 才置，幂等）。返回受影响行数。
     * default 返回 0 兼容既有测试双子。
     */
    default int forfeitEntitlement(String id, java.time.Instant now) {
        return 0;
    }

    List<IssuedEntitlementRecord> listIssuedEntitlements();

    List<IssuedEntitlementRecord> listIssuedEntitlementsByOrderId(String orderId);

    Optional<IssuedEntitlementRecord> findIssuedEntitlementById(String id);

    TicketCredentialRecord saveTicketCredential(TicketCredentialRecord credential);

    Optional<TicketCredentialRecord> findTicketCredentialByToken(String credentialToken);

    TicketRefundRecord saveTicketRefund(TicketRefundRecord refund);

    Optional<TicketRefundRecord> findTicketRefundById(String id);

    TicketRefundLineRecord saveTicketRefundLine(TicketRefundLineRecord line);

    List<TicketRefundLineRecord> listTicketRefundLines(String refundId);

    long sumPendingRefundAmountByOrderId(String orderId);

    /** 已审批（approved）退款金额合计——用于判定整单是否累计全额退款（决定库存名额是否回补）。 */
    long sumApprovedRefundAmountByOrderId(String orderId);

    Optional<IssuedEntitlementRecord> atomicUseEntitlement(String id, java.time.Instant updatedAt);

    Optional<IssuedEntitlementRecord> atomicReverseEntitlement(String id, java.time.Instant updatedAt);

    boolean tryLockOrderForIssuance(String orderId);

    String findLatestFlywayVersion();
}
