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
}
