package com.complex.platform.ticketing;

import com.complex.platform.common.api.PageResponse;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TerminalSyncTasksRequest;
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import com.complex.platform.ticketing.dto.TicketPriceRuleRequest;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
import com.complex.platform.ticketing.dto.TicketTypeEntitlementRequest;
import com.complex.platform.ticketing.dto.TicketTypeRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.complex.platform.ticketing.dto.VerificationReverseRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketingService {

    private final TicketingRepository repository;
    private final TicketCatalogService catalogService;
    private final TicketPricingService pricingService;
    private final TicketOrderService orderService;
    private final TicketRefundService refundService;
    private final TicketVerificationService verificationService;
    private final TicketCredentialService credentialService;
    private final TicketOfflinePackageService offlinePackageService;
    private final TicketingOperationExecutor operations;

    public TicketingService(
            TicketingRepository repository,
            TicketCatalogService catalogService,
            TicketPricingService pricingService,
            TicketOrderService orderService,
            TicketRefundService refundService,
            TicketVerificationService verificationService,
            TicketCredentialService credentialService,
            TicketOfflinePackageService offlinePackageService,
            TicketingOperationExecutor operations
    ) {
        this.repository = repository;
        this.catalogService = catalogService;
        this.pricingService = pricingService;
        this.orderService = orderService;
        this.refundService = refundService;
        this.verificationService = verificationService;
        this.credentialService = credentialService;
        this.offlinePackageService = offlinePackageService;
        this.operations = operations;
    }

    public PageResponse<TicketTypeRecord> listTicketTypes() {
        return operations.readPage(
                TicketingReadOperation.LIST_TICKET_TYPES,
                repository::listTicketTypes
        );
    }

    public PageResponse<TicketPriceRuleRecord> listTicketPriceRules() {
        return operations.readPage(
                TicketingReadOperation.LIST_TICKET_PRICE_RULES,
                repository::listPriceRules
        );
    }

    public PageResponse<TicketDynamicPricingRuleRecord> listDynamicPricingRules() {
        return operations.readPage(
                TicketingReadOperation.LIST_DYNAMIC_PRICING_RULES,
                repository::listDynamicPricingRules
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.dynamic_pricing.manage")
    @Transactional
    public TicketDynamicPricingRuleRecord createDynamicPricingRule(TicketDynamicPricingRuleRequest request) {
        return operations.executeWrite(
                "createTicketDynamicPricingRule",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketDynamicPricingRuleRecord.class,
                TicketingWriteOperation.CREATE_DYNAMIC_PRICING_RULE,
                () -> pricingService.createDynamicPricingRule(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.dynamic_pricing.publish", admissionAction = "ticketing.dynamic_pricing_publish", objectType = "ticket_dynamic_pricing_rule")
    @Transactional
    public TicketDynamicPricingRuleRecord publishDynamicPricingRule(String ruleId, StatusChangeRequest request) {
        String id = operations.requireText(ruleId, "dynamic_pricing_rule_id");
        return operations.executeWrite(
                "publishTicketDynamicPricingRule:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketDynamicPricingRuleRecord.class,
                TicketingWriteOperation.PUBLISH_DYNAMIC_PRICING_RULE,
                () -> pricingService.publishDynamicPricingRule(id, request)
        );
    }

    @Transactional
    public TicketDynamicPricingSimulationResponse simulateDynamicPricing(TicketDynamicPricingSimulationRequest request) {
        return operations.executeWrite(
                "simulateTicketDynamicPricing",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketDynamicPricingSimulationResponse.class,
                TicketingWriteOperation.SIMULATE_DYNAMIC_PRICING,
                () -> pricingService.simulateDynamicPricing(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.product.manage")
    @Transactional
    public TicketTypeRecord createTicketType(TicketTypeRequest request) {
        return operations.executeWrite(
                "createTicketType",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeRecord.class,
                TicketingWriteOperation.CREATE_TICKET_TYPE,
                () -> catalogService.createTicketType(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.price_variant.manage")
    @Transactional
    public TicketPriceRuleRecord createTicketPriceRule(TicketPriceRuleRequest request) {
        return operations.executeWrite(
                "createTicketPriceRule",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketPriceRuleRecord.class,
                TicketingWriteOperation.CREATE_TICKET_PRICE_RULE,
                () -> catalogService.createPriceRule(request)
        );
    }

    public PageResponse<EntitlementTemplateRecord> listEntitlementTemplates() {
        return operations.readPage(
                TicketingReadOperation.LIST_ENTITLEMENT_TEMPLATES,
                repository::listEntitlementTemplates
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.entitlement.manage")
    @Transactional
    public EntitlementTemplateRecord createEntitlementTemplate(EntitlementTemplateRequest request) {
        return operations.executeWrite(
                "createEntitlementTemplate",
                request == null ? null : request.idempotencyKey(),
                request,
                EntitlementTemplateRecord.class,
                TicketingWriteOperation.CREATE_ENTITLEMENT_TEMPLATE,
                () -> catalogService.createEntitlementTemplate(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.product.manage")
    @Transactional
    public TicketTypeEntitlementRecord bindTicketTypeEntitlement(String ticketTypeId, TicketTypeEntitlementRequest request) {
        String id = operations.requireText(ticketTypeId, "ticket_type_id");
        return operations.executeWrite(
                "bindTicketTypeEntitlement:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeEntitlementRecord.class,
                TicketingWriteOperation.BIND_TICKET_TYPE_ENTITLEMENT,
                () -> catalogService.bindTicketTypeEntitlement(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.order.create")
    @Transactional
    public IssuedTicketResponse issueTicketTypeDirectly(String ticketTypeId, TicketIssueRequest request) {
        String id = operations.requireText(ticketTypeId, "ticket_type_id");
        return operations.executeWrite(
                "issueTicketTypeDirectly:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                IssuedTicketResponse.class,
                TicketingWriteOperation.ISSUE_TICKET_TYPE_DIRECTLY,
                () -> orderService.issueEntitlements(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.order.create")
    @Transactional
    public TicketOrderRecord createTicketOrder(TicketOrderRequest request) {
        return operations.executeWrite(
                "createTicketOrder",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketOrderRecord.class,
                TicketingWriteOperation.CREATE_TICKET_ORDER,
                () -> orderService.createOrder(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment_intent.create")
    @Transactional
    public TicketPaymentIntentRecord createTicketOrderPayIntent(String orderId, PaymentIntentRequest request) {
        String id = operations.requireText(orderId, "order_id");
        return operations.executeWrite(
                "createTicketOrderPayIntent:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketPaymentIntentRecord.class,
                TicketingWriteOperation.CREATE_TICKET_ORDER_PAY_INTENT,
                () -> orderService.createPaymentIntent(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.ticket.issue")
    @Transactional
    public IssuedTicketResponse issueTicketOrder(String orderId, StatusChangeRequest request) {
        String id = operations.requireText(orderId, "order_id");
        return operations.executeWrite(
                "issueTicketOrder:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                IssuedTicketResponse.class,
                TicketingWriteOperation.ISSUE_TICKET_ORDER,
                () -> orderService.issueOrder(id, request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.product.manage")
    @Transactional
    public TicketTypeRecord publishTicketType(String ticketTypeId, StatusChangeRequest request) {
        String id = operations.requireText(ticketTypeId, "ticket_type_id");
        return operations.executeWrite(
                "publishTicketType:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketTypeRecord.class,
                TicketingWriteOperation.PUBLISH_TICKET_TYPE,
                () -> catalogService.publishTicketType(id, request)
        );
    }

    public PageResponse<IssuedEntitlementRecord> listIssuedEntitlements() {
        return operations.readPage(
                TicketingReadOperation.LIST_ISSUED_ENTITLEMENTS,
                repository::listIssuedEntitlements
        );
    }

    public PageResponse<TicketOrderRecord> listTicketOrders() {
        return operations.readPage(
                TicketingReadOperation.LIST_TICKET_ORDERS,
                repository::listTicketOrders
        );
    }

    public List<TicketOrderRecord> listTicketOrdersByCustomerId(String customerId) {
        return repository.listTicketOrdersByCustomerId(operations.requireText(customerId, "customer_id"));
    }

    @Transactional
    public TicketCredentialResponse getTicketCredential(String ticketId) {
        return credentialService.getTicketCredential(ticketId);
    }

    @Transactional
    public TicketCredentialResponse getCustomerTicketCredential(String ticketId, String customerId) {
        return credentialService.getCustomerTicketCredential(ticketId, customerId);
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.refund.request")
    @Transactional
    public TicketRefundRecord requestTicketRefund(TicketRefundRequest request) {
        return operations.executeWrite(
                "requestTicketRefund",
                request == null ? null : request.idempotencyKey(),
                request,
                TicketRefundRecord.class,
                TicketingWriteOperation.REQUEST_TICKET_REFUND,
                () -> refundService.requestRefund(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "ticket.refund.approve", admissionAction = "ticketing.refund_approve", objectType = "ticket_refund")
    @Transactional
    public TicketRefundRecord approveTicketRefund(String refundId, StatusChangeRequest request) {
        String id = operations.requireText(refundId, "refund_id");
        return operations.executeWrite(
                "approveTicketRefund:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                TicketRefundRecord.class,
                TicketingWriteOperation.APPROVE_TICKET_REFUND,
                () -> refundService.approveRefund(id, request)
        );
    }

    public PageResponse<TicketRefundLineRecord> listTicketRefundLines(String refundId) {
        String id = operations.requireText(refundId, "refund_id");
        return operations.readPage(
                TicketingReadOperation.LIST_TICKET_REFUND_LINES,
                () -> repository.listTicketRefundLines(id)
        );
    }

    public OfflinePackageResponse getOfflineEntitlements(String merchantId, String venueId, int limit) {
        return offlinePackageService.getOfflineEntitlements(merchantId, venueId, limit);
    }

    @Transactional
    public VerificationPreviewResponse previewVerification(VerificationRequest request) {
        return operations.executeWrite(
                "previewTicketVerification",
                request == null ? null : request.idempotencyKey(),
                request,
                VerificationPreviewResponse.class,
                TicketingWriteOperation.PREVIEW_VERIFICATION,
                () -> verificationService.previewVerification(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "verification.execute")
    @Transactional
    public VerificationResponse createVerification(VerificationRequest request) {
        return operations.executeWrite(
                "createTicketVerification",
                request == null ? null : request.idempotencyKey(),
                request,
                VerificationResponse.class,
                TicketingWriteOperation.CREATE_VERIFICATION,
                () -> verificationService.createVerification(request)
        );
    }

    /** C1/REQ-05：人工放行（主管），留痕 status=manual_overridden。权限在端点层按主管权限码控制。 */
    @com.complex.platform.gate.GuardedAction(admissionAction = "ticketing.special_release", objectType = "verification")
    public VerificationResponse manualOverrideVerification(VerificationRequest request, String reason) {
        return verificationService.manualOverride(request, reason);
    }

    /** R5 T5：核销冲正。幂等键必填、reason 必填、权限 ticket.verify.reverse；成功事件 ticket.verification_reversed。 */
    @com.complex.platform.gate.GuardedAction(capability = "ticket.verify.reverse", admissionAction = "ticketing.verification_reverse", objectType = "verification")
    @Transactional
    public VerificationResponse reverseVerification(String verificationId, VerificationReverseRequest request, String reversedBy) {
        return operations.executeWrite(
                "reverseTicketVerification",
                request == null ? null : request.idempotencyKey(),
                request,
                VerificationResponse.class,
                TicketingWriteOperation.REVERSE_VERIFICATION,
                () -> verificationService.reverseVerification(
                        verificationId,
                        request == null ? null : request.reason(),
                        reversedBy
                )
        );
    }

    public PageResponse<VerificationRecord> listVerifications() {
        return operations.readPage(
                TicketingReadOperation.LIST_VERIFICATIONS,
                repository::listVerifications
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "terminal.device.bind")
    @Transactional
    public SyncTasksResponse syncTerminalTasks(TerminalSyncTasksRequest request) {
        return operations.executeWrite(
                "syncTerminalTasks",
                request == null ? null : request.idempotencyKey(),
                request,
                SyncTasksResponse.class,
                TicketingWriteOperation.SYNC_TERMINAL_TASKS,
                () -> verificationService.syncTerminalTasks(request)
        );
    }
}
