package com.complex.platform.ticketing;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TicketingController {

    private final TicketingService service;
    private final AuthorizationService authorizationService;

    public TicketingController(TicketingService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/ticketing/ticket-types")
    public ApiResponse<PageResponse<TicketTypeRecord>> listTicketTypes() {
        authorizationService.requirePermission("ticket.product.view");
        return ApiResponse.ok(service.listTicketTypes());
    }

    @PostMapping("/ticketing/ticket-types")
    public ApiResponse<TicketTypeRecord> createTicketType(@RequestBody @jakarta.validation.Valid TicketTypeRequest request) {
        authorizationService.requirePermission("ticket.product.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTicketType(request));
    }

    @PostMapping("/ticketing/ticket-types/{id}/publish")
    public ApiResponse<TicketTypeRecord> publishTicketType(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid StatusChangeRequest request
    ) {
        authorizationService.requirePermission("ticket.product.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.publishTicketType(id, request));
    }

    @GetMapping("/ticketing/price-rules")
    public ApiResponse<PageResponse<TicketPriceRuleRecord>> listTicketPriceRules() {
        authorizationService.requirePermission("ticket.price_variant.view");
        return ApiResponse.ok(service.listTicketPriceRules());
    }

    @PostMapping("/ticketing/price-rules")
    public ApiResponse<TicketPriceRuleRecord> createTicketPriceRule(@RequestBody @jakarta.validation.Valid TicketPriceRuleRequest request) {
        authorizationService.requirePermission("ticket.price_variant.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTicketPriceRule(request));
    }


    @GetMapping("/ticketing/dynamic-pricing-rules")
    public ApiResponse<PageResponse<TicketDynamicPricingRuleRecord>> listDynamicPricingRules() {
        authorizationService.requirePermission("ticket.dynamic_pricing.view");
        return ApiResponse.ok(service.listDynamicPricingRules());
    }

    @PostMapping("/ticketing/dynamic-pricing-rules")
    public ApiResponse<TicketDynamicPricingRuleRecord> createDynamicPricingRule(
            @RequestBody @jakarta.validation.Valid TicketDynamicPricingRuleRequest request
    ) {
        authorizationService.requirePermission("ticket.dynamic_pricing.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createDynamicPricingRule(request));
    }

    @PostMapping("/ticketing/dynamic-pricing-rules/{id}/publish")
    public ApiResponse<TicketDynamicPricingRuleRecord> publishDynamicPricingRule(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid StatusChangeRequest request
    ) {
        authorizationService.requirePermission("ticket.dynamic_pricing.publish");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.publishDynamicPricingRule(id, request));
    }

    @PostMapping("/ticketing/dynamic-pricing/simulations")
    public ApiResponse<TicketDynamicPricingSimulationResponse> simulateDynamicPricing(
            @RequestBody @jakarta.validation.Valid TicketDynamicPricingSimulationRequest request
    ) {
        authorizationService.requirePermission("ticket.dynamic_pricing.simulate");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.simulateDynamicPricing(request));
    }

    @GetMapping("/ticketing/entitlement-templates")
    public ApiResponse<PageResponse<EntitlementTemplateRecord>> listEntitlementTemplates() {
        authorizationService.requirePermission("ticket.entitlement.view");
        return ApiResponse.ok(service.listEntitlementTemplates());
    }

    @PostMapping("/ticketing/entitlement-templates")
    public ApiResponse<EntitlementTemplateRecord> createEntitlementTemplate(@RequestBody @jakarta.validation.Valid EntitlementTemplateRequest request) {
        authorizationService.requirePermission("ticket.entitlement.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createEntitlementTemplate(request));
    }

    @PostMapping("/ticketing/ticket-types/{id}/entitlements")
    public ApiResponse<TicketTypeEntitlementRecord> bindTicketTypeEntitlement(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid TicketTypeEntitlementRequest request
    ) {
        authorizationService.requirePermission("ticket.product.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.bindTicketTypeEntitlement(id, request));
    }

    @PostMapping("/ticketing/ticket-types/{id}/issue")
    public ApiResponse<IssuedTicketResponse> issueTicketTypeDirectly(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid TicketIssueRequest request
    ) {
        authorizationService.requirePermission("ticket.order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.issueTicketTypeDirectly(id, request));
    }

    @GetMapping("/ticketing/issued-entitlements")
    public ApiResponse<PageResponse<IssuedEntitlementRecord>> listIssuedEntitlements() {
        authorizationService.requirePermission("ticket.verify.view");
        return ApiResponse.ok(service.listIssuedEntitlements());
    }

    @GetMapping("/ticketing/orders")
    public ApiResponse<PageResponse<TicketOrderRecord>> listTicketOrders() {
        authorizationService.requirePermission("ticket.order.read");
        return ApiResponse.ok(service.listTicketOrders());
    }

    @PostMapping("/ticketing/orders")
    public ApiResponse<TicketOrderRecord> createTicketOrder(@RequestBody @jakarta.validation.Valid TicketOrderRequest request) {
        authorizationService.requirePermission("ticket.order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTicketOrder(request));
    }

    @PostMapping("/ticketing/orders/{order_id}/pay-intents")
    public ApiResponse<TicketPaymentIntentRecord> createTicketOrderPayIntent(
            @PathVariable("order_id") String orderId,
            @RequestBody @jakarta.validation.Valid PaymentIntentRequest request
    ) {
        authorizationService.requirePermission("finance.payment_intent.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTicketOrderPayIntent(orderId, request));
    }

    @PostMapping("/ticketing/orders/{order_id}/confirm-payment")
    public ApiResponse<TicketOrderRecord> confirmTicketOrderPayment(
            @PathVariable("order_id") String orderId,
            @RequestBody @jakarta.validation.Valid ConfirmTicketOrderPaymentRequest request
    ) {
        authorizationService.requirePermission("finance.payment.register");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.confirmTicketOrderPayment(orderId, request));
    }

    @PostMapping("/ticketing/orders/{order_id}/cancel")
    public ApiResponse<TicketOrderRecord> cancelTicketOrder(
            @PathVariable("order_id") String orderId,
            @RequestBody @jakarta.validation.Valid StatusChangeRequest request
    ) {
        authorizationService.requirePermission("ticket.order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.cancelTicketOrder(orderId, request));
    }

    @PostMapping("/ticketing/orders/{order_id}/issue")
    public ApiResponse<IssuedTicketResponse> issueTicketOrder(
            @PathVariable("order_id") String orderId,
            @RequestBody @jakarta.validation.Valid StatusChangeRequest request
    ) {
        authorizationService.requirePermission("ticket.ticket.issue");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.issueTicketOrder(orderId, request));
    }

    @GetMapping("/ticketing/tickets/{ticket_id}/credential")
    public ApiResponse<TicketCredentialResponse> getTicketCredential(@PathVariable("ticket_id") String ticketId) {
        authorizationService.requirePermission("ticket.credential.view");
        return ApiResponse.ok(service.getTicketCredential(ticketId));
    }

    @PostMapping("/ticketing/refunds")
    public ApiResponse<TicketRefundRecord> requestTicketRefund(@RequestBody @jakarta.validation.Valid TicketRefundRequest request) {
        authorizationService.requirePermission("ticket.refund.request");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.requestTicketRefund(request));
    }

    @PostMapping("/ticketing/refunds/{refund_id}/approve")
    public ApiResponse<TicketRefundRecord> approveTicketRefund(
            @PathVariable("refund_id") String refundId,
            @RequestBody @jakarta.validation.Valid StatusChangeRequest request
    ) {
        authorizationService.requirePermission("ticket.refund.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveTicketRefund(refundId, request));
    }

    @GetMapping("/ticketing/refunds/{refund_id}/lines")
    public ApiResponse<PageResponse<TicketRefundLineRecord>> listTicketRefundLines(
            @PathVariable("refund_id") String refundId
    ) {
        authorizationService.requirePermission("ticket.refund.view");
        return ApiResponse.ok(service.listTicketRefundLines(refundId));
    }

    @GetMapping("/terminals/offline-entitlements")
    public ApiResponse<OfflinePackageResponse> offlineEntitlements(
            @RequestParam(name = "merchant_id", required = false) String merchantId,
            @RequestParam(name = "venue_id", required = false) String venueId,
            @RequestParam(name = "merchantId", required = false) String camelMerchantId,
            @RequestParam(name = "venueId", required = false) String camelVenueId,
            @RequestParam(name = "limit", defaultValue = "200") int limit
    ) {
        authorizationService.requirePermission("terminal.device.bind");
        return ApiResponse.ok(service.getOfflineEntitlements(
                firstNonBlank(merchantId, camelMerchantId),
                firstNonBlank(venueId, camelVenueId),
                limit
        ));
    }

    @PostMapping("/ticketing/verifications/preview")
    public ApiResponse<VerificationPreviewResponse> previewVerification(@RequestBody @jakarta.validation.Valid VerificationRequest request) {
        authorizationService.requirePermission("verification.execute");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.previewVerification(request));
    }

    @PostMapping("/ticketing/verifications")
    public ApiResponse<VerificationResponse> createVerification(@RequestBody @jakarta.validation.Valid VerificationRequest request) {
        authorizationService.requirePermission("verification.execute");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createVerification(request));
    }

    @GetMapping("/ticketing/verifications")
    public ApiResponse<PageResponse<VerificationRecord>> listVerifications() {
        authorizationService.requirePermission("verification.view");
        return ApiResponse.ok(service.listVerifications());
    }

    @PostMapping("/ticketing/verifications/{id}/reverse")
    public ApiResponse<VerificationResponse> reverseVerification(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid VerificationReverseRequest request
    ) {
        authorizationService.requirePermission("ticket.verify.reverse");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reverseVerification(id, request, resolveReversedBy()));
    }

    @PostMapping("/terminals/sync-tasks")
    public ApiResponse<SyncTasksResponse> syncTerminalTasks(@RequestBody @jakarta.validation.Valid TerminalSyncTasksRequest request) {
        authorizationService.requirePermission("terminal.device.bind");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.syncTerminalTasks(request));
    }

    private String resolveReversedBy() {
        try {
            return authorizationService.currentActor().id();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second == null || second.isBlank() ? null : second;
    }
}
