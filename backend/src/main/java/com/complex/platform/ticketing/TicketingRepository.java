package com.complex.platform.ticketing;

import java.util.List;
import java.util.Optional;

public interface TicketingRepository {
    TicketTypeRecord saveTicketType(TicketTypeRecord ticketType);

    List<TicketTypeRecord> listTicketTypes();

    Optional<TicketTypeRecord> findTicketTypeById(String id);

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

    Optional<TerminalSyncTaskRecord> findTerminalSyncTask(String deviceCode, String clientTaskId);

    TerminalSyncTaskRecord saveTerminalSyncTask(TerminalSyncTaskRecord task);

    TicketPriceRuleRecord savePriceRule(TicketPriceRuleRecord rule);

    List<TicketPriceRuleRecord> listPriceRules();

    Optional<TicketPriceRuleRecord> findPriceRuleById(String id);

    TicketOrderRecord saveTicketOrder(TicketOrderRecord order);

    Optional<TicketOrderRecord> findTicketOrderById(String id);

    TicketOrderRecord updateTicketOrder(TicketOrderRecord order);

    List<TicketOrderRecord> listTicketOrdersByCustomerId(String customerId);

    TicketPaymentIntentRecord saveTicketPaymentIntent(TicketPaymentIntentRecord intent);

    List<IssuedEntitlementRecord> listIssuedEntitlements();

    List<IssuedEntitlementRecord> listIssuedEntitlementsByOrderId(String orderId);

    Optional<IssuedEntitlementRecord> findIssuedEntitlementById(String id);

    TicketCredentialRecord saveTicketCredential(TicketCredentialRecord credential);

    Optional<TicketCredentialRecord> findTicketCredentialByToken(String credentialToken);

    TicketRefundRecord saveTicketRefund(TicketRefundRecord refund);

    Optional<TicketRefundRecord> findTicketRefundById(String id);

    TicketRefundLineRecord saveTicketRefundLine(TicketRefundLineRecord line);

    List<TicketRefundLineRecord> listTicketRefundLines(String refundId);

    String findLatestFlywayVersion();
}
