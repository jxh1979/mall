package com.complex.platform.finance;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** FS-021-R5：普通内部结算应付的执行状态（财务端）与收款方视图（商户专区「门票分账」、更换收款账户审批）。 */
@RestController
public class InternalPayableController {

    private final InternalPayableExecutionService service;

    public InternalPayableController(InternalPayableExecutionService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/finance/internal-payables/{ar_ap_item_id}")
    public ApiResponse<InternalPayableExecutionService.Execution> getExecution(@PathVariable("ar_ap_item_id") String id) {
        return ApiResponse.ok(service.execution(id));
    }

    @PostMapping("/api/v1/finance/internal-payables/{ar_ap_item_id}/payment-attempts")
    public ApiResponse<InternalPayableExecutionService.Attempt> initiate(@PathVariable("ar_ap_item_id") String id,
            @RequestBody InternalPayableExecutionService.InitiateRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.initiate(id, request));
    }

    @PostMapping("/api/v1/finance/internal-payables/{ar_ap_item_id}/payment-attempts/{attempt_id}/failure")
    public ApiResponse<InternalPayableExecutionService.Attempt> fail(@PathVariable("ar_ap_item_id") String id,
            @PathVariable("attempt_id") String attemptId, @RequestBody InternalPayableExecutionService.FailureRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.fail(id, attemptId, request));
    }

    @PostMapping("/api/v1/finance/internal-payables/{ar_ap_item_id}/funds-insufficient")
    public ApiResponse<InternalPayableExecutionService.Execution> fundsInsufficient(@PathVariable("ar_ap_item_id") String id,
            @RequestBody InternalPayableExecutionService.FundsInsufficientRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.fundsInsufficient(id, request));
    }

    @GetMapping("/api/v1/merchant-portal/ticket-settlements")
    public ApiResponse<List<InternalPayableExecutionService.Execution>> listMyTicketSettlements() {
        return ApiResponse.ok(service.merchantTicketSettlements());
    }

    @PostMapping("/api/v1/merchant-portal/ticket-settlements/{ar_ap_item_id}/account-change")
    public ApiResponse<Map<String, Object>> approveAccountChange(@PathVariable("ar_ap_item_id") String id,
            @RequestBody InternalPayableExecutionService.AccountChangeRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveAccountChange(id, request));
    }
}
