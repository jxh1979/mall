package com.complex.platform.customer;

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
}
