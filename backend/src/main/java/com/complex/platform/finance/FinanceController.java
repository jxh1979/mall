package com.complex.platform.finance;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.finance.dto.ArApAdjustmentDecisionRequest;
import com.complex.platform.finance.dto.ArApAdjustmentRequest;
import com.complex.platform.finance.dto.ArApItemRequest;
import com.complex.platform.finance.dto.ArApPaymentRequest;
import com.complex.platform.finance.dto.DailyCloseRequest;
import com.complex.platform.finance.dto.OfflinePaymentConfirmRequest;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.finance.dto.RefundRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController {

    private final FinanceService service;
    private final AuthorizationService authorizationService;

    public FinanceController(FinanceService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/ar-ap-items")
    public ApiResponse<PageResponse<ArApItemRecord>> listArApItems() {
        authorizationService.requirePermission("finance.ar_ap.view");
        return ApiResponse.ok(service.listArApItems());
    }

    @PostMapping("/ar-ap-items")
    public ApiResponse<ArApItemRecord> createArApItem(@RequestBody ArApItemRequest request) {
        authorizationService.requirePermission("finance.ar_ap.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createArApItem(request));
    }

    @PostMapping("/ar-ap-items/{id}/payments")
    public ApiResponse<ArApPaymentResponse> registerArApPayment(
            @PathVariable String id,
            @RequestBody ArApPaymentRequest request
    ) {
        authorizationService.requirePermission("finance.payment.register");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.registerArApPayment(id, request));
    }

    @PostMapping("/daily-closes")
    public ApiResponse<DailyCloseRecord> createDailyClose(@RequestBody DailyCloseRequest request) {
        authorizationService.requirePermission("finance.daily_close.lock");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createDailyClose(request));
    }

    @GetMapping("/daily-closes/{id}/summaries")
    public ApiResponse<PageResponse<DailyCloseSummaryRecord>> listDailyCloseSummaries(@PathVariable String id) {
        authorizationService.requirePermission("finance.daily_close.manage");
        return ApiResponse.ok(service.listDailyCloseSummaries(id));
    }

    @PostMapping("/ar-ap-adjustments")
    public ApiResponse<ArApAdjustmentRecord> requestArApAdjustment(@RequestBody ArApAdjustmentRequest request) {
        authorizationService.requirePermission("finance.locked_backfill.request");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.requestAdjustment(request));
    }

    @PostMapping("/ar-ap-adjustments/{id}/approve")
    public ApiResponse<ArApAdjustmentRecord> approveArApAdjustment(
            @PathVariable String id,
            @RequestBody ArApAdjustmentDecisionRequest request
    ) {
        authorizationService.requirePermission("finance.locked_backfill.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveAdjustment(id, request));
    }

    @PostMapping("/payment-intents")
    public ApiResponse<PaymentIntentRecord> createPaymentIntent(@RequestBody PaymentIntentRequest request) {
        authorizationService.requirePermission("finance.payment_intent.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createPaymentIntent(request));
    }

    @PostMapping("/payments/{payment_id}/confirm-offline")
    public ApiResponse<OfflinePaymentConfirmationRecord> confirmOfflinePayment(
            @PathVariable("payment_id") String paymentId,
            @RequestBody OfflinePaymentConfirmRequest request
    ) {
        authorizationService.requirePermission("finance.payment.register");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.confirmOfflinePayment(paymentId, request));
    }

    @GetMapping("/reconciliation/batches")
    public ApiResponse<PageResponse<ReconciliationBatchRecord>> listReconciliationBatches() {
        authorizationService.requirePermission("finance.reconciliation.view");
        return ApiResponse.ok(service.listReconciliationBatches());
    }

    @GetMapping("/reconciliation/differences")
    public ApiResponse<PageResponse<ReconciliationDifferenceRecord>> listReconciliationDifferences() {
        authorizationService.requirePermission("openplatform.reconcile.execute");
        return ApiResponse.ok(service.listReconciliationDifferences());
    }

    @PostMapping("/refunds")
    public ApiResponse<FinanceRefundRecord> createRefund(@RequestBody RefundRequest request) {
        authorizationService.requirePermission("finance.refund.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createRefund(request));
    }
}
