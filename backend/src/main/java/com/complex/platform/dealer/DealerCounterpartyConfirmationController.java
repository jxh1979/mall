package com.complex.platform.dealer;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.dealer.dto.DealerCounterpartyConfirmationRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 第十八批第 31 条 经销商停用后经办人确认留证（BSM:1247；内部后台）。 */
@RestController
@RequestMapping("/api/v1/dealers")
public class DealerCounterpartyConfirmationController {

    private final DealerCounterpartyConfirmationService service;

    public DealerCounterpartyConfirmationController(DealerCounterpartyConfirmationService service) {
        this.service = service;
    }

    @GetMapping("/orders/{id}/counterparty-confirmations")
    public ApiResponse<PageResponse<DealerCounterpartyConfirmationService.Confirmation>> list(@PathVariable String id) {
        return ApiResponse.ok(service.list(id));
    }

    @PostMapping("/orders/{id}/counterparty-confirmations")
    public ApiResponse<DealerCounterpartyConfirmationService.Confirmation> record(@PathVariable String id, @RequestBody DealerCounterpartyConfirmationRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.record(id, request));
    }
}
