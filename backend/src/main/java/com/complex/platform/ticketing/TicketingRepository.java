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

    List<EntitlementTemplateRecord> listEntitlementTemplates();

    Optional<EntitlementTemplateRecord> findEntitlementTemplateById(String id);

    TicketTypeEntitlementRecord saveTicketTypeEntitlement(TicketTypeEntitlementRecord entitlement);

    List<TicketTypeEntitlementRecord> listTicketTypeEntitlements(String ticketTypeId);

    IssuedEntitlementRecord saveIssuedEntitlement(IssuedEntitlementRecord entitlement);

    Optional<IssuedEntitlementRecord> findIssuedEntitlementByCode(String entitlementCode);

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
