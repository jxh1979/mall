package com.complex.platform.ticketing;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateArchiveRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateDeleteRequest;
import com.complex.platform.ticketing.dto.EntitlementTemplateDeleteApprovalRequest;
import com.complex.platform.ticketing.dto.StatusChangeRequest;
import com.complex.platform.ticketing.dto.TerminalSyncTasksRequest;
import com.complex.platform.ticketing.dto.TicketIssueRequest;
import com.complex.platform.ticketing.dto.TicketOrderRequest;
import com.complex.platform.ticketing.dto.QrRefreshConfigRequest;
import com.complex.platform.ticketing.dto.TicketPriceRuleRequest;
import com.complex.platform.ticketing.dto.TicketRefundRequest;
import com.complex.platform.ticketing.dto.TicketTypeEntitlementRequest;
import com.complex.platform.ticketing.dto.TicketTypeRequest;
import com.complex.platform.ticketing.dto.VerificationManualOverrideRequest;
import com.complex.platform.ticketing.dto.VerificationRequest;
import com.complex.platform.ticketing.dto.VerificationReverseRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TicketingController {

    private final TicketingService service;
    private final AuthorizationService authorizationService;
    // 批3-6：人脸照存档服务(取照端点授权+解密+审计在服务内)。
    private final TicketFacePhotoService facePhotoService;

    public TicketingController(TicketingService service, AuthorizationService authorizationService,
                              TicketFacePhotoService facePhotoService) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.facePhotoService = facePhotoService;
    }

    @GetMapping("/ticketing/ticket-types")
    public ApiResponse<PageResponse<TicketTypeRecord>> listTicketTypes(
            @RequestParam(name = "includeArchived", required = false, defaultValue = "false") boolean includeArchived) {
        authorizationService.requirePermission("ticket.product.view");
        return ApiResponse.ok(service.listTicketTypes(includeArchived));
    }

    @PostMapping("/ticketing/ticket-types/{id}/archive")
    public ApiResponse<TicketTypeRecord> archiveTicketType(
            @PathVariable("id") String id, @RequestBody @jakarta.validation.Valid TicketCatalogArchiveRequest request) {
        authorizationService.requirePermission("ticket.product.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.archiveTicketType(id, request));
    }

    @PostMapping("/ticketing/ticket-types/{id}/unarchive")
    public ApiResponse<TicketTypeRecord> unarchiveTicketType(
            @PathVariable("id") String id, @RequestBody @jakarta.validation.Valid TicketCatalogArchiveRequest request) {
        authorizationService.requirePermission("ticket.product.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.unarchiveTicketType(id, request));
    }

    @PostMapping("/ticketing/ticket-types/{id}/delete-request")
    public ApiResponse<TicketTypeRecord> requestDeleteTicketType(
            @PathVariable("id") String id, @RequestBody @jakarta.validation.Valid TicketCatalogDeleteRequest request) {
        authorizationService.requirePermission("ticket.product.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.requestDeleteTicketType(id, request));
    }

    @PostMapping("/ticketing/ticket-types/{id}/delete-approve")
    public ApiResponse<TicketTypeDeleteDecision> approveDeleteTicketType(
            @PathVariable("id") String id, @RequestBody @jakarta.validation.Valid TicketCatalogDeleteApprovalRequest request) {
        authorizationService.requirePermission("ticket.type.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveDeleteTicketType(id, request));
    }

    @PostMapping("/ticketing/ticket-types")
    public ApiResponse<TicketTypeRecord> createTicketType(@RequestBody @jakarta.validation.Valid TicketTypeRequest request) {
        authorizationService.requirePermission("ticket.product.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTicketType(request));
    }

    // #172(2026-07-19 拍板)：全场统一动态码刷新间隔（读/改），出示验证类二维码全场一套设置。
    @GetMapping("/ticketing/qr-refresh-config")
    public ApiResponse<QrRefreshConfigResponse> getQrRefreshConfig() {
        authorizationService.requirePermission("ticket.product.view");
        return ApiResponse.ok(service.getQrRefreshConfig());
    }

    @PutMapping("/ticketing/qr-refresh-config")
    public ApiResponse<QrRefreshConfigResponse> updateQrRefreshConfig(
            @RequestBody @jakarta.validation.Valid QrRefreshConfigRequest request) {
        authorizationService.requirePermission("ticket.product.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateQrRefreshConfig(request));
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
    public ApiResponse<PageResponse<TicketPriceRuleRecord>> listTicketPriceRules(
            @RequestParam(name = "includeArchived", required = false, defaultValue = "false") boolean includeArchived) {
        authorizationService.requirePermission("ticket.price_variant.view");
        return ApiResponse.ok(service.listTicketPriceRules(includeArchived));
    }

    @PostMapping("/ticketing/price-rules/{id}/archive")
    public ApiResponse<TicketPriceRuleRecord> archivePriceRule(
            @PathVariable("id") String id, @RequestBody @jakarta.validation.Valid TicketCatalogArchiveRequest request) {
        authorizationService.requirePermission("ticket.price_variant.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.archivePriceRule(id, request));
    }

    @PostMapping("/ticketing/price-rules/{id}/unarchive")
    public ApiResponse<TicketPriceRuleRecord> unarchivePriceRule(
            @PathVariable("id") String id, @RequestBody @jakarta.validation.Valid TicketCatalogArchiveRequest request) {
        authorizationService.requirePermission("ticket.price_variant.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.unarchivePriceRule(id, request));
    }

    @PostMapping("/ticketing/price-rules/{id}/delete-request")
    public ApiResponse<TicketPriceRuleRecord> requestDeletePriceRule(
            @PathVariable("id") String id, @RequestBody @jakarta.validation.Valid TicketCatalogDeleteRequest request) {
        authorizationService.requirePermission("ticket.price_variant.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.requestDeletePriceRule(id, request));
    }

    @PostMapping("/ticketing/price-rules/{id}/delete-approve")
    public ApiResponse<TicketPriceRuleDeleteDecision> approveDeletePriceRule(
            @PathVariable("id") String id, @RequestBody @jakarta.validation.Valid TicketCatalogDeleteApprovalRequest request) {
        authorizationService.requirePermission("ticket.price_rule.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveDeletePriceRule(id, request));
    }

    @PostMapping("/ticketing/distribution-merchant-rates")
    public ApiResponse<TicketDistributionMerchantRateRecord> createDistributionMerchantRate(
            @RequestBody @jakarta.validation.Valid com.complex.platform.ticketing.dto.TicketDistributionMerchantRateRequest request) {
        authorizationService.requirePermission("ticket.price_variant.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createDistributionMerchantRate(request));
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
    public ApiResponse<PageResponse<EntitlementTemplateRecord>> listEntitlementTemplates(
            @RequestParam(name = "includeArchived", required = false, defaultValue = "false") boolean includeArchived) {
        authorizationService.requirePermission("ticket.entitlement.view");
        return ApiResponse.ok(service.listEntitlementTemplates(includeArchived));
    }

    @PostMapping("/ticketing/entitlement-templates/{id}/archive")
    public ApiResponse<EntitlementTemplateRecord> archiveEntitlementTemplate(
            @PathVariable("id") String id, @RequestBody @jakarta.validation.Valid EntitlementTemplateArchiveRequest request) {
        authorizationService.requirePermission("ticket.entitlement.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.archiveEntitlementTemplate(id, request));
    }

    @PostMapping("/ticketing/entitlement-templates/{id}/unarchive")
    public ApiResponse<EntitlementTemplateRecord> unarchiveEntitlementTemplate(
            @PathVariable("id") String id, @RequestBody @jakarta.validation.Valid EntitlementTemplateArchiveRequest request) {
        authorizationService.requirePermission("ticket.entitlement.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.unarchiveEntitlementTemplate(id, request));
    }

    @PostMapping("/ticketing/entitlement-templates/{id}/delete-request")
    public ApiResponse<EntitlementTemplateRecord> requestDeleteEntitlementTemplate(
            @PathVariable("id") String id, @RequestBody @jakarta.validation.Valid EntitlementTemplateDeleteRequest request) {
        authorizationService.requirePermission("ticket.entitlement.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.requestDeleteEntitlementTemplate(id, request));
    }

    @PostMapping("/ticketing/entitlement-templates/{id}/delete-approve")
    public ApiResponse<EntitlementTemplateDeleteDecision> approveDeleteEntitlementTemplate(
            @PathVariable("id") String id, @RequestBody @jakarta.validation.Valid EntitlementTemplateDeleteApprovalRequest request) {
        authorizationService.requirePermission("ticket.entitlement.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveDeleteEntitlementTemplate(id, request));
    }

    @PostMapping("/ticketing/entitlement-templates")
    public ApiResponse<EntitlementTemplateRecord> createEntitlementTemplate(@RequestBody @jakarta.validation.Valid EntitlementTemplateRequest request) {
        authorizationService.requirePermission("ticket.entitlement.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createEntitlementTemplate(request));
    }

    /**
     * V369 票种定价核算（业务 HTML:741）：各业态票面原价合计 × 票种折扣 = 系统算出售价，
     * 与运营填的售价差多少、后台允许差多少、超没超、凑整该填哪个数。建票/改票页据此实时提示。
     */
    @GetMapping("/ticketing/ticket-types/{id}/price-check")
    public ApiResponse<TicketTypePriceCheck> ticketTypePriceCheck(@PathVariable("id") String id) {
        authorizationService.requirePermission("ticket.product.view");
        return ApiResponse.ok(service.ticketTypePriceCheck(id));
    }

    /** V369 改票种售价／折扣：保存当场比对允许误差，超出即拒（不等到发布才报错）。 */
    @PutMapping("/ticketing/ticket-types/{id}/pricing")
    public ApiResponse<TicketTypeRecord> updateTicketTypePricing(
            @PathVariable("id") String id,
            @RequestBody @jakarta.validation.Valid com.complex.platform.ticketing.dto.TicketTypePricingRequest request) {
        authorizationService.requirePermission("ticket.product.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateTicketTypePricing(id, request));
    }

    /** S5 票面赠酒清单：该票种各酒吧分别赠几杯（售票时要写明票面，业务 HTML §通票赠酒按酒吧分配）。 */
    @GetMapping("/ticketing/ticket-types/{id}/gift-coverage")
    public ApiResponse<PageResponse<TicketTypeGiftCoverageRow>> listTicketTypeGiftCoverage(
            @PathVariable("id") String id) {
        authorizationService.requirePermission("ticket.product.view");
        return ApiResponse.ok(service.listGiftCoverage(id));
    }

    // 批10/E：原「改虚拟权益签约价」端点(配置中心批5/PR#274)已下线——内部结算价唯一可编辑处收编为
    // 内部结算 SKU 目录(POST /api/v1/settlement/config/internal-price-item 落草案 → 他人复核转正 → 快照传播)。

    @PostMapping("/ticketing/ticket-types/{id}/entitlements")
    public ApiResponse<TicketTypeEntitlementRecord> bindTicketTypeEntitlement(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid TicketTypeEntitlementRequest request
    ) {
        authorizationService.requirePermission("ticket.product.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.bindTicketTypeEntitlement(id, request));
    }

    // AUD-019：direct 出票端点（POST /ticketing/ticket-types/{id}/issue）已退役——对假订单必 FK 回滚、
    // 对真订单可绕过归属/票种/支付态校验错票占坑。统一走订单出票 POST /ticketing/orders/{order_id}/issue。

    @GetMapping("/ticketing/issued-entitlements")
    public ApiResponse<PageResponse<IssuedEntitlementRecord>> listIssuedEntitlements() {
        authorizationService.requirePermission("ticket.verify.view");
        return ApiResponse.ok(service.listIssuedEntitlements());
    }

    @GetMapping("/ticketing/ticket-instances")
    public ApiResponse<PageResponse<TicketInstanceRecord>> listTicketInstances() {
        authorizationService.requirePermission("ticket.verify.view");
        return ApiResponse.ok(service.listTicketInstances());
    }

    @GetMapping("/ticketing/ticket-transfers")
    public ApiResponse<PageResponse<TicketTransferResponse>> listTicketTransfers() {
        authorizationService.requirePermission("ticket.transfer.read");
        return ApiResponse.ok(service.listTicketTransfers());
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

    @PostMapping("/ticketing/refunds/{refund_id}/reject")
    public ApiResponse<TicketRefundRecord> rejectTicketRefund(
            @PathVariable("refund_id") String refundId,
            @RequestBody @jakarta.validation.Valid StatusChangeRequest request
    ) {
        authorizationService.requirePermission("ticket.refund.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.rejectTicketRefund(refundId, request));
    }

    @PostMapping("/ticketing/refunds/{refund_id}/confirm-payout")
    public ApiResponse<TicketRefundRecord> confirmTicketRefundPayout(
            @PathVariable("refund_id") String refundId,
            @RequestBody @jakarta.validation.Valid StatusChangeRequest request
    ) {
        authorizationService.requirePermission("ticket.refund.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.confirmTicketRefundPayout(refundId, request));
    }

    @GetMapping("/ticketing/refunds")
    public ApiResponse<PageResponse<TicketRefundRecord>> listTicketRefunds() {
        authorizationService.requirePermission("ticket.refund.view");
        return ApiResponse.ok(service.listTicketRefunds());
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

    // 批3-6(用户2026-07-04)：核销页取某权益的人脸照(解密返回 base64)。授权限核销/运营(verification.view)+每次查看写审计,
    //   均在 facePhotoService.getForVerification 内。核销主链不改,照片仅核销页附带展示。
    @GetMapping("/ticketing/issued-entitlements/{entitlement_id}/face-photo")
    public ApiResponse<TicketFacePhotoService.FacePhotoView> getEntitlementFacePhoto(
            @PathVariable("entitlement_id") String entitlementId) {
        return ApiResponse.ok(facePhotoService.getForVerification(entitlementId));
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

    // C1/REQ-05：人工放行（主管）。主管对核销做人工放行决策，留痕 status=manual_overridden。
    //   权限=主管权限码 verification.manual_override；租户 scope 守卫 + 审计留痕在服务层
    //   （TicketVerificationService.manualOverride 内 enforceMerchantScope + @GuardedAction 准入闸 + 落核销记录）。
    @PostMapping("/ticketing/verifications/manual-override")
    public ApiResponse<VerificationResponse> manualOverrideVerification(
            @RequestBody @jakarta.validation.Valid VerificationManualOverrideRequest request
    ) {
        authorizationService.requirePermission("verification.manual_override");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.manualOverrideVerification(
                request.toVerificationRequest(),
                request == null ? null : request.reason()));
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
