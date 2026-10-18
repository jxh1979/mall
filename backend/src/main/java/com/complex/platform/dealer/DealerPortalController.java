package com.complex.platform.dealer;

// 文件版本：V0.04

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.dealer.dto.DealerCancelRequest;
import com.complex.platform.dealer.dto.DealerIdempotentRequest;
import com.complex.platform.dealer.dto.DealerInquiryRequest;
import com.complex.platform.dealer.dto.DealerOrderDraftRequest;
import com.complex.platform.dealer.dto.DealerPaymentRequest;
import com.complex.platform.dealer.dto.DealerPortalProfileRequest;
import com.complex.platform.dealer.dto.DealerReturnRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CG-11d 独立经销商 Web 门户接口（受控第十一端 dealer_portal；与 /api/v1/dealers 内部后台接口隔离）。 */
@RestController
@RequestMapping("/api/v1/dealer-portal")
public class DealerPortalController {

    private final DealerPortalService service;

    public DealerPortalController(DealerPortalService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public ApiResponse<DealerRecord> me() {
        return ApiResponse.ok(service.me());
    }

    @PostMapping("/me/submission")
    public ApiResponse<DealerRecord> submitProfile(@RequestBody DealerPortalProfileRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submitProfile(request));
    }

    @GetMapping("/contracts")
    public ApiResponse<PageResponse<DealerContractRecord>> listContracts() {
        return ApiResponse.ok(service.listContracts());
    }

    @GetMapping("/contracts/{id}/external-stock")
    public ApiResponse<PageResponse<DealerExternalStockRecord>> externalStock(@PathVariable String id) {
        return ApiResponse.ok(service.externalStock(id));
    }

    @GetMapping("/inquiries")
    public ApiResponse<PageResponse<DealerInquiryRecord>> listInquiries() {
        return ApiResponse.ok(service.listInquiries());
    }

    @PostMapping("/inquiries")
    public ApiResponse<DealerInquiryRecord> createInquiry(@RequestBody DealerInquiryRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createInquiry(request));
    }

    @GetMapping("/orders")
    public ApiResponse<PageResponse<DealerOrderRecord>> listOrders() {
        return ApiResponse.ok(service.listOrders());
    }

    @PostMapping("/orders")
    public ApiResponse<DealerOrderRecord> createOrder(@RequestBody DealerOrderDraftRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createOrder(request));
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

    @GetMapping("/orders/{id}/payments")
    public ApiResponse<PageResponse<DealerOrderPaymentRecord>> listPayments(@PathVariable String id) {
        return ApiResponse.ok(service.listPayments(id));
    }

    @PostMapping("/orders/{id}/payments")
    public ApiResponse<DealerOrderPaymentRecord> registerPayment(@PathVariable String id, @RequestBody DealerPaymentRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.registerPayment(id, request));
    }

    @PostMapping("/orders/{id}/sign")
    public ApiResponse<DealerOrderRecord> signOrder(@PathVariable String id, @RequestBody com.complex.platform.dealer.dto.DealerDeliveryReceiptRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.signOrder(id, request));
    }

    @GetMapping("/orders/{id}/delivery")
    public ApiResponse<DealerDeliveryRecord> delivery(@PathVariable String id) { return ApiResponse.ok(service.delivery(id)); }

    @PostMapping(value = "/orders/{id}/delivery-evidence", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<com.complex.platform.evidence.EvidenceAttachmentRecord> uploadDeliveryEvidence(@PathVariable String id,
            @org.springframework.web.bind.annotation.RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        return ApiResponse.ok(service.uploadDeliveryEvidence(id, file));
    }

    @GetMapping("/orders/{id}/delivery-evidence/{attachmentId}")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> readDeliveryEvidence(@PathVariable String id, @PathVariable String attachmentId) {
        return service.readDeliveryEvidence(id, attachmentId);
    }

    @GetMapping("/orders/{id}/returns")
    public ApiResponse<PageResponse<DealerReturnRecord>> listReturns(@PathVariable String id) {
        return ApiResponse.ok(service.listReturns(id));
    }

    @PostMapping("/orders/{id}/returns")
    public ApiResponse<DealerReturnRecord> requestReturn(@PathVariable String id, @RequestBody DealerReturnRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.requestReturn(id, request));
    }
    @GetMapping("/orders/{id}/return-sources")
    public ApiResponse<java.util.List<JdbcDealerReturnRepository.ReturnSource>> returnSources(@PathVariable String id) {
        return ApiResponse.ok(service.returnSources(id));
    }
    @GetMapping("/orders/{id}/return-financials")
    public ApiResponse<java.util.List<DealerReturnFinancialRecord>> returnFinancials(@PathVariable String id) {
        return ApiResponse.ok(service.returnFinancials(id));
    }

    /** ENUM:179 / 裁决 69：门户查看发票（只读）。 */
    @GetMapping("/orders/{id}/invoices")
    public ApiResponse<PageResponse<DealerInvoiceRecord>> listInvoices(@PathVariable String id) {
        return ApiResponse.ok(service.listInvoices(id));
    }

    /** ENUM:179 / 裁决 69：门户取消申请（只是申请，审批在内部后台）。 */
    @PostMapping("/orders/{id}/cancel-request")
    public ApiResponse<DealerOrderRecord> requestCancel(@PathVariable String id, @RequestBody DealerCancelRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.requestCancel(id, request));
    }

}
