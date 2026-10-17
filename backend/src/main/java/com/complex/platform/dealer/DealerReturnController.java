package com.complex.platform.dealer;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.dealer.dto.DealerIdempotentRequest;
import com.complex.platform.dealer.dto.DealerInvoiceIssueRequest;
import com.complex.platform.dealer.dto.DealerInvoiceRequest;
import com.complex.platform.dealer.dto.DealerReturnDecisionRequest;
import com.complex.platform.dealer.dto.DealerReturnFreightConfirmRequest;
import com.complex.platform.dealer.dto.DealerReturnInspectionRequest;
import com.complex.platform.dealer.dto.DealerReturnRefundResultRequest;
import com.complex.platform.dealer.dto.DealerReturnRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CG-11c 经销退货、退款、红冲与开票（内部后台：审批、收货验收、红冲重开、退款执行）。 */
@RestController
@RequestMapping("/api/v1/dealers")
public class DealerReturnController {

    private final DealerReturnService service;

    public DealerReturnController(DealerReturnService service) {
        this.service = service;
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

    @GetMapping("/orders/{id}/invoices")
    public ApiResponse<PageResponse<DealerInvoiceRecord>> listInvoices(@PathVariable String id) {
        return ApiResponse.ok(service.listInvoices(id));
    }

    @PostMapping("/orders/{id}/invoices")
    public ApiResponse<DealerInvoiceRecord> requestInvoice(@PathVariable String id, @RequestBody DealerInvoiceRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.requestInvoice(id, request));
    }

    @GetMapping("/returns/{id}")
    public ApiResponse<DealerReturnRecord> getReturn(@PathVariable String id) {
        return ApiResponse.ok(service.getReturn(id));
    }

    @GetMapping("/returns/{id}/lines")
    public ApiResponse<PageResponse<DealerReturnLineRecord>> listReturnLines(@PathVariable String id) {
        return ApiResponse.ok(service.listReturnLines(id));
    }

    @PostMapping("/returns/{id}/decision")
    public ApiResponse<DealerReturnRecord> decideReturn(@PathVariable String id, @RequestBody DealerReturnDecisionRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.decideReturn(id, request));
    }

    @PostMapping("/returns/{id}/receive")
    public ApiResponse<DealerReturnRecord> receiveReturn(@PathVariable String id, @RequestBody DealerIdempotentRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.receiveReturn(id, request.idempotencyKey()));
    }

    @PostMapping("/returns/{id}/inspection")
    public ApiResponse<DealerReturnRecord> inspectReturn(@PathVariable String id, @RequestBody DealerReturnInspectionRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.inspectReturn(id, request));
    }

    @PostMapping("/returns/{id}/invoice-reversal")
    public ApiResponse<DealerReturnRecord> reverseInvoice(@PathVariable String id, @RequestBody DealerInvoiceIssueRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.reverseInvoice(id, request));
    }

    @PostMapping("/returns/{id}/freight-confirmation")
    public ApiResponse<DealerReturnRecord> confirmReturnFreight(@PathVariable String id, @RequestBody DealerReturnFreightConfirmRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.confirmReturnFreight(id, request));
    }

    @PostMapping("/returns/{id}/refund-result")
    public ApiResponse<DealerReturnRecord> recordRefundResult(@PathVariable String id, @RequestBody DealerReturnRefundResultRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.recordRefundResult(id, request));
    }

    @PostMapping("/invoices/{id}/issue")
    public ApiResponse<DealerInvoiceRecord> issueInvoice(@PathVariable String id, @RequestBody DealerInvoiceIssueRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.issueInvoice(id, request));
    }
}
