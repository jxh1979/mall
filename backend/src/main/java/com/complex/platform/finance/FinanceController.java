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
import com.complex.platform.finance.dto.CamAllocationRequest;
import com.complex.platform.finance.dto.CamTrueUpRequest;
import com.complex.platform.finance.dto.DailyCloseRequest;
import com.complex.platform.finance.dto.DepositActionRequest;
import com.complex.platform.finance.dto.InventoryVariancePostRequest;
import com.complex.platform.finance.dto.TurnoverRentBillRequest;
import java.util.List;
import com.complex.platform.finance.dto.OfflinePaymentConfirmRequest;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.finance.dto.PropertyFeeBillRequest;
import com.complex.platform.finance.dto.PropertyFeeRateRequest;
import com.complex.platform.finance.dto.ReconciliationDifferenceRequest;
import com.complex.platform.finance.dto.RefundRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/ar-ap-items/export")
    public ApiResponse<ArApLedgerExportRecord> exportArApItems() {
        authorizationService.requirePermission("finance.ar_ap.view");
        return ApiResponse.ok(service.exportArApItems());
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
    public ApiResponse<PaymentIntentRecord> createPaymentIntent(@RequestBody @jakarta.validation.Valid PaymentIntentRequest request) {
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

    @PostMapping("/reconciliation/differences")
    public ApiResponse<ReconciliationDifferenceRecord> createReconciliationDifference(
            @RequestBody ReconciliationDifferenceRequest request
    ) {
        authorizationService.requirePermission("finance.reconciliation.difference.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createReconciliationDifference(request));
    }

    @PostMapping("/refunds")
    public ApiResponse<FinanceRefundRecord> createRefund(@RequestBody @jakarta.validation.Valid RefundRequest request) {
        authorizationService.requirePermission("finance.refund.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createRefund(request));
    }

    // ── P1#13 库存差异 → AR/AP ──────────────────────────────────────────────────

    @PostMapping("/inventory-variances/post")
    public ApiResponse<ArApItemRecord> postInventoryVariance(@RequestBody InventoryVariancePostRequest request) {
        authorizationService.requirePermission("finance.inventory_variance.post");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.postInventoryVariance(request));
    }

    // ── P1#14 综合体计费：营业额抽成 / 保证金 / CAM ─────────────────────────────

    @PostMapping("/turnover-rent/bills")
    public ApiResponse<ArApItemRecord> billTurnoverRent(@RequestBody TurnoverRentBillRequest request) {
        authorizationService.requirePermission("finance.turnover_rent.bill");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.billTurnoverRent(request));
    }

    @PostMapping("/deposits/actions")
    public ApiResponse<DepositLedgerRecord> recordDepositAction(@RequestBody DepositActionRequest request) {
        authorizationService.requirePermission("finance.deposit.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.recordDepositAction(request));
    }

    // F-OPS-08 已废止：CAM 权重分摊 + 年度 true-up 由物业费每平米固定计费（不退不补）取代。
    // 两端点保留仅为对旧客户端返回明确弃用错误码（CAM_ALLOCATION_ABOLISHED / CAM_TRUEUP_ABOLISHED）；
    // 改用下方 /property-fee/* 端点。
    @PostMapping("/cam/allocations")
    public ApiResponse<List<CamAllocationRecord>> allocateCam(@RequestBody CamAllocationRequest request) {
        authorizationService.requirePermission("finance.cam.allocate");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.allocateCam(request));
    }

    @PostMapping("/cam/trueups")
    public ApiResponse<CamTrueUpRunRecord> runCamTrueUp(@RequestBody CamTrueUpRequest request) {
        authorizationService.requirePermission("finance.cam.trueup");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.runCamTrueUp(request));
    }

    // ── F-OPS-08 物业费固定计费（每平米固定单价 × 商户面积，按期不退不补）─────────────

    @PostMapping("/property-fee/rates")
    public ApiResponse<PropertyFeeRateRecord> createPropertyFeeRate(@RequestBody PropertyFeeRateRequest request) {
        authorizationService.requirePermission("finance.property_fee.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createPropertyFeeRate(request));
    }

    @GetMapping("/property-fee/rates")
    public ApiResponse<List<PropertyFeeRateRecord>> listPropertyFeeRates(@RequestParam("project_id") String projectId) {
        authorizationService.requirePermission("finance.property_fee.manage");
        return ApiResponse.ok(service.listPropertyFeeRates(projectId));
    }

    @PostMapping("/property-fee/bills")
    public ApiResponse<List<PropertyFeeBillRecord>> billPropertyFee(@RequestBody PropertyFeeBillRequest request) {
        authorizationService.requirePermission("finance.property_fee.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.billPropertyFee(request));
    }
}
