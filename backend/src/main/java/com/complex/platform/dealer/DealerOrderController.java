package com.complex.platform.dealer;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.dealer.dto.DealerCancelRequest;
import com.complex.platform.dealer.dto.DealerContractTermsRequest;
import com.complex.platform.dealer.dto.DealerIdempotentRequest;
import com.complex.platform.dealer.dto.DealerOrderDecisionRequest;
import com.complex.platform.dealer.dto.DealerOrderDraftRequest;
import com.complex.platform.dealer.dto.DealerOverpaymentUseRequest;
import com.complex.platform.dealer.dto.DealerPaymentRequest;
import com.complex.platform.dealer.dto.DealerPaymentVerifyRequest;
import com.complex.platform.dealer.dto.DealerRefundApprovalRequest;
import com.complex.platform.dealer.dto.DealerRefundExecuteRequest;
import com.complex.platform.dealer.dto.DealerShipRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CG-11b 经销订单、付款与履约（内部后台：审核、到账核验、出库、取消、退款、超收）。 */
@RestController
@RequestMapping("/api/v1/dealers")
public class DealerOrderController {

    private final DealerOrderService service;

    public DealerOrderController(DealerOrderService service) {
        this.service = service;
    }

    @PostMapping("/contracts/{id}/terms")
    public ApiResponse<DealerContractRecord> updateContractTerms(@PathVariable String id, @RequestBody DealerContractTermsRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.updateContractTerms(id, request));
    }

    @GetMapping("/{id}/orders")
    public ApiResponse<PageResponse<DealerOrderRecord>> listOrders(@PathVariable String id) {
        return ApiResponse.ok(service.listOrders(id));
    }

    @PostMapping("/{id}/orders")
    public ApiResponse<DealerOrderRecord> createOrderDraft(@PathVariable String id, @RequestBody DealerOrderDraftRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createOrderDraft(id, request));
    }

    @GetMapping("/{id}/overpayments")
    public ApiResponse<PageResponse<DealerOrderOverpaymentRecord>> listOverpayments(@PathVariable String id) {
        return ApiResponse.ok(service.listOverpayments(id));
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<DealerOrderRecord> getOrder(@PathVariable String id) {
        return ApiResponse.ok(service.getOrder(id));
    }

    @GetMapping("/orders/{id}/lines")
    public ApiResponse<PageResponse<DealerOrderLineRecord>> listLines(@PathVariable String id) {
        return ApiResponse.ok(service.listLines(id));
    }

    @PostMapping("/orders/{id}/submit")
    public ApiResponse<DealerOrderRecord> submitOrder(@PathVariable String id, @RequestBody DealerIdempotentRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submitOrder(id, request.idempotencyKey()));
    }

    @PostMapping("/orders/{id}/review")
    public ApiResponse<DealerOrderRecord> reviewOrder(@PathVariable String id, @RequestBody DealerOrderDecisionRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reviewOrder(id, request));
    }

    @GetMapping("/orders/{id}/payments")
    public ApiResponse<PageResponse<DealerOrderPaymentRecord>> listPayments(@PathVariable String id) {
        return ApiResponse.ok(service.listPayments(id));
    }

    @PostMapping("/orders/{id}/payments")
    public ApiResponse<DealerOrderPaymentRecord> registerPayment(@PathVariable String id, @RequestBody DealerPaymentRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.registerPayment(id, request));
    }

    @PostMapping("/payments/{id}/verify")
    public ApiResponse<DealerOrderPaymentRecord> verifyPayment(@PathVariable String id, @RequestBody DealerPaymentVerifyRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.verifyPayment(id, request));
    }

    @PostMapping("/orders/{id}/ship")
    public ApiResponse<DealerOrderRecord> shipOrder(@PathVariable String id, @RequestBody DealerShipRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.shipOrder(id, request));
    }

    @PostMapping("/orders/{id}/sign")
    public ApiResponse<DealerOrderRecord> signOrder(@PathVariable String id, @RequestBody DealerIdempotentRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.signOrder(id, request.idempotencyKey()));
    }

    @PostMapping("/orders/{id}/cancel-request")
    public ApiResponse<DealerOrderRecord> requestCancel(@PathVariable String id, @RequestBody DealerCancelRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.requestCancel(id, request));
    }

    @PostMapping("/orders/{id}/cancel-decision")
    public ApiResponse<DealerOrderRecord> decideCancel(@PathVariable String id, @RequestBody DealerOrderDecisionRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.decideCancel(id, request));
    }

    @GetMapping("/orders/{id}/refunds")
    public ApiResponse<PageResponse<DealerOrderRefundRecord>> listRefunds(@PathVariable String id) {
        return ApiResponse.ok(service.listRefunds(id));
    }

    @PostMapping("/refunds/{id}/approval")
    public ApiResponse<DealerOrderRefundRecord> openRefundApproval(@PathVariable String id, @RequestBody DealerRefundApprovalRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.openRefundApproval(id, request));
    }

    @PostMapping("/refunds/{id}/execute")
    public ApiResponse<DealerOrderRefundRecord> executeRefund(@PathVariable String id, @RequestBody DealerRefundExecuteRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.executeRefund(id, request));
    }

    @PostMapping("/overpayments/{id}/use")
    public ApiResponse<DealerOrderOverpaymentRecord> confirmOverpaymentUse(@PathVariable String id, @RequestBody DealerOverpaymentUseRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.confirmOverpaymentUse(id, request));
    }
}
