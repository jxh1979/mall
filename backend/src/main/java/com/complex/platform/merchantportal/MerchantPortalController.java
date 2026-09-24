package com.complex.platform.merchantportal;
// 文件版本：V0.01

import com.complex.platform.common.api.ApiRequests;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.contract.ContractSettlementRecord;
import com.complex.platform.merchantportal.dto.MerchantSettlementDisputeRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商户专区一期(3.18 方案A)商户自助端点:/api/v1/merchant-portal/*。
 * 账单/工单直接复用既有域端点(已按 merchant scope 硬闸);此处仅承载
 * 既有端点权限为平台管理权限、需要商户只读口径的合同与结算视图。
 */
@RestController
@RequestMapping("/api/v1/merchant-portal")
public class MerchantPortalController {

    private final MerchantPortalService service;

    public MerchantPortalController(MerchantPortalService service) {
        this.service = service;
    }

    @GetMapping("/contracts")
    public ApiResponse<PageResponse<MerchantContractView>> listMyContracts(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "500") int size) {
        return ApiResponse.ok(service.listMyContracts(page, size));
    }

    @GetMapping("/settlements")
    public ApiResponse<PageResponse<ContractSettlementRecord>> listMySettlements(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "500") int size) {
        return ApiResponse.ok(service.listMySettlements(page, size));
    }

    @PostMapping("/settlements/{settlement_id}/dispute")
    public ApiResponse<ContractSettlementRecord> submitSettlementDispute(
            @PathVariable("settlement_id") String settlementId,
            @RequestBody @jakarta.validation.Valid MerchantSettlementDisputeRequest request
    ) {
        // M020-004:幂等键校验走共享助手(VALIDATION_FAILED),与全仓其它控制器一致。
        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submitSettlementDispute(settlementId, request));
    }
}
