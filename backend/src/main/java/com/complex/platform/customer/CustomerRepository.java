package com.complex.platform.customer;

import com.complex.platform.bar.BarDepositRuleRecord;
import com.complex.platform.bar.BarReservationRecord;
import com.complex.platform.bar.BarTableRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
    Optional<CustomerRecord> findCustomerByPhone(String phone);

    Optional<CustomerRecord> findCustomerById(String customerId);

    CustomerRecord saveCustomer(CustomerRecord customer);

    CustomerSessionRecord saveSession(CustomerSessionRecord session);

    Optional<CustomerSessionRecord> findSessionByToken(String token);

    CustomerConsentRecord saveConsent(CustomerConsentRecord consent);

    Optional<CustomerPointsAccountRecord> findPointsByCustomerId(String customerId);

    Optional<CustomerPointsAccountRecord> findPointsByCustomerIdForUpdate(String customerId);

    CustomerPointsAccountRecord savePoints(CustomerPointsAccountRecord account);

    CustomerPointsLedgerRecord savePointsLedger(CustomerPointsLedgerRecord ledger);

    List<CustomerPointsLedgerRecord> listPointsLedgerByCustomerId(String customerId);

    CustomerRedemptionCatalogItemRecord saveRedemptionCatalogItem(CustomerRedemptionCatalogItemRecord item);

    Optional<CustomerRedemptionCatalogItemRecord> findRedemptionCatalogItemById(String itemId);

    List<CustomerRedemptionCatalogItemRecord> listActiveRedemptionCatalogItems(Instant now);

    CustomerPointsRedemptionRecord savePointsRedemption(CustomerPointsRedemptionRecord redemption);

    List<CustomerPointsRedemptionRecord> listPointsRedemptionsByCustomerId(String customerId);

    /** 积分规则引擎（REQ-12, P0-6）。默认空/抛出，JdbcCustomerRepository 覆盖。 */
    default CustomerPointsRuleRecord savePointsRule(CustomerPointsRuleRecord rule) {
        throw new UnsupportedOperationException("savePointsRule not supported by this repository");
    }

    default List<CustomerPointsRuleRecord> listPointsRules() {
        return List.of();
    }

    default Optional<CustomerPointsRuleRecord> findPointsRuleById(String id) {
        return Optional.empty();
    }

    List<CustomerBarReservationOptionResponse> listBarReservationOptions();

    Optional<BarTableRecord> findActiveBarTableForReservation(String barId, String tableId, String tableType);

    // P0#3 订台定金：读 bar_deposit_rules（V073）按 bar 解析启用规则（scope_type DESC 优先）。
    List<BarDepositRuleRecord> listDepositRules(String barId);

    BarReservationRecord saveCustomerBarReservation(BarReservationRecord reservation);

    // ---- 会员忠诚度 第1部分（V089）：等级配置 / 签到 / 生日规则 / 过期批次 ----

    /** 列出所有积分账户（等级评估/过期清零定时作业逐账户处理用）。 */
    default List<CustomerPointsAccountRecord> listAllPointsAccounts() {
        return List.of();
    }

    default CustomerMemberLevelRecord saveMemberLevel(CustomerMemberLevelRecord level) {
        throw new UnsupportedOperationException("saveMemberLevel not supported by this repository");
    }

    /** 按 project 升序排（level_rank）列出生效等级配置。projectId 可空（综合体级全局）。 */
    default List<CustomerMemberLevelRecord> listMemberLevels(String projectId) {
        return List.of();
    }

    default CustomerCheckInTierRecord saveCheckInTier(CustomerCheckInTierRecord tier) {
        throw new UnsupportedOperationException("saveCheckInTier not supported by this repository");
    }

    default List<CustomerCheckInTierRecord> listCheckInTiers(String projectId) {
        return List.of();
    }

    default CustomerBirthdayRuleRecord saveBirthdayRule(CustomerBirthdayRuleRecord rule) {
        throw new UnsupportedOperationException("saveBirthdayRule not supported by this repository");
    }

    default List<CustomerBirthdayRuleRecord> listActiveBirthdayRules(String projectId) {
        return List.of();
    }

    default CustomerCheckInRecord saveCheckIn(CustomerCheckInRecord checkIn) {
        throw new UnsupportedOperationException("saveCheckIn not supported by this repository");
    }

    default Optional<CustomerCheckInRecord> findCheckIn(String customerId, java.time.LocalDate checkInDate) {
        return Optional.empty();
    }

    default List<CustomerCheckInRecord> listCheckIns(String customerId) {
        return List.of();
    }

    /** 列出该账户已到期、尚有剩余的获取批次（FIFO 过期清零用），按 expire_at 升序。 */
    default List<CustomerPointsLedgerRecord> listExpirableBatches(String customerId, Instant now) {
        return List.of();
    }

    /** 列出该账户尚有剩余、有过期时间的获取批次（FIFO 消耗用，使过期只清未用部分），按时间升序。 */
    default List<CustomerPointsLedgerRecord> listOpenBatches(String customerId) {
        return List.of();
    }

    /** 标记某获取批次为已过期并清零剩余。 */
    default void markBatchExpired(String ledgerId) {
        throw new UnsupportedOperationException("markBatchExpired not supported by this repository");
    }

    /** 扣减获取批次剩余积分（兑换/扣减时 FIFO 消耗，使过期只清未用部分）。 */
    default void consumeBatchRemaining(String ledgerId, long remainingPoints) {
        throw new UnsupportedOperationException("consumeBatchRemaining not supported by this repository");
    }

    // ---- P0 清单#2 积分入账引擎（V100）：按业务单据幂等 + 履约/退款查账 ----

    /** 按业务单据 + 变动类型查已存在的入账流水（幂等：命中即视为已入账，不重复写）。 */
    default Optional<CustomerPointsLedgerRecord> findPointsLedgerBySourceDoc(
            String sourceDocType, String sourceDocId, String changeType) {
        return Optional.empty();
    }

    /** 列出某业务单据下指定变动类型的入账流水（履约转 confirmed / 退款按已 confirmed 比例 deduct 用）。 */
    default List<CustomerPointsLedgerRecord> listPointsLedgerBySourceDoc(
            String sourceDocType, String sourceDocId, String changeType) {
        return List.of();
    }

    /** 把某条 pending 入账流水转为指定状态（履约：pending→confirmed）。 */
    default void updatePointsLedgerStatus(String ledgerId, String status) {
        throw new UnsupportedOperationException("updatePointsLedgerStatus not supported by this repository");
    }

    /**
     * F-TXN-03 按核销张数：把某条 earn 行的已确认份额对齐 confirmedAmount（target），同步 status；
     * remainingPoints 非空时同步可过期余额（=已确认份额，使过期口径与实际入余额一致），为空则不动该列。
     */
    default void updatePointsLedgerConfirmation(String ledgerId, long confirmedAmount, String status, Long remainingPoints) {
        throw new UnsupportedOperationException("updatePointsLedgerConfirmation not supported by this repository");
    }

    // ---- 会员忠诚度 第2部分（V090）：储值 / 付费会员 / 积分转赠 ----

    /** 储值账户（每客每项目唯一），带行锁用于充值/消费一致。 */
    default Optional<CustomerStoredValueAccountRecord> findStoredValueAccountForUpdate(String customerId, String projectId) {
        return Optional.empty();
    }

    default Optional<CustomerStoredValueAccountRecord> findStoredValueAccount(String customerId, String projectId) {
        return Optional.empty();
    }

    // B1d⑤ 卡范围查找：merchantId 非空=商户级卡，为空=综合体通用卡。默认实现：通用卡委派 2 参版（保留既有假实现行为），
    // 商户卡返回空（仅 Jdbc 真仓储承载）。
    default Optional<CustomerStoredValueAccountRecord> findStoredValueAccount(String customerId, String projectId, String merchantId) {
        return merchantId == null ? findStoredValueAccount(customerId, projectId) : Optional.empty();
    }

    default Optional<CustomerStoredValueAccountRecord> findStoredValueAccountForUpdate(String customerId, String projectId, String merchantId) {
        return merchantId == null ? findStoredValueAccountForUpdate(customerId, projectId) : Optional.empty();
    }

    default CustomerStoredValueAccountRecord saveStoredValueAccount(CustomerStoredValueAccountRecord account) {
        throw new UnsupportedOperationException("saveStoredValueAccount not supported by this repository");
    }

    default CustomerStoredValueTransactionRecord saveStoredValueTransaction(CustomerStoredValueTransactionRecord txn) {
        throw new UnsupportedOperationException("saveStoredValueTransaction not supported by this repository");
    }

    // B1d⑤ phase3 商户级卡退款规则（发卡商户自设可否退/比例）。无配置=默认允许全额。
    default Optional<StoredValueMerchantRefundRuleRecord> findStoredValueMerchantRefundRule(String merchantId) {
        return Optional.empty();
    }

    default StoredValueMerchantRefundRuleRecord saveStoredValueMerchantRefundRule(StoredValueMerchantRefundRuleRecord rule) {
        throw new UnsupportedOperationException("saveStoredValueMerchantRefundRule not supported by this repository");
    }

    /** 幂等校验：按 idempotency_key 查已落库的储值流水。 */
    default Optional<CustomerStoredValueTransactionRecord> findStoredValueTransactionByIdempotencyKey(String key) {
        return Optional.empty();
    }

    /** 储值流水是否已引用某支付意图（防同一充值支付意图重复入账）。 */
    default boolean storedValueTransactionExistsForPaymentIntent(String paymentIntentId) {
        return false;
    }

    default List<CustomerStoredValueTransactionRecord> listStoredValueTransactions(String accountId) {
        return List.of();
    }

    // 付费会员档位 / 订阅
    default CustomerMembershipPlanRecord saveMembershipPlan(CustomerMembershipPlanRecord plan) {
        throw new UnsupportedOperationException("saveMembershipPlan not supported by this repository");
    }

    default Optional<CustomerMembershipPlanRecord> findMembershipPlanById(String planId) {
        return Optional.empty();
    }

    default List<CustomerMembershipPlanRecord> listMembershipPlans(String projectId) {
        return List.of();
    }

    default CustomerMembershipRecord saveMembership(CustomerMembershipRecord membership) {
        throw new UnsupportedOperationException("saveMembership not supported by this repository");
    }

    default Optional<CustomerMembershipRecord> findMembershipByIdForUpdate(String membershipId) {
        return Optional.empty();
    }

    default Optional<CustomerMembershipRecord> findMembershipByIdempotencyKey(String key) {
        return Optional.empty();
    }

    default List<CustomerMembershipRecord> listMembershipsByCustomerId(String customerId) {
        return List.of();
    }

    /** 列出已到期但仍 active 的订阅（到期作业用）。 */
    default List<CustomerMembershipRecord> listActiveMembershipsExpiringBefore(Instant now) {
        return List.of();
    }

    // 积分会员间转赠已下线（HTML：积分不支持会员间转赠；2026-06-24 裁决）。相关 repo 方法/记录类/表已随 V172 清理移除。
}
