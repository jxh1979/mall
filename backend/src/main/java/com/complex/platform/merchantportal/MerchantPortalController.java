package com.complex.platform.merchantportal;
// 文件版本：V0.05

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
    private final MerchantZoneNoticeService notices;
    private final MerchantFeeStatementService feeStatements;
    private final MerchantUnreadBillService unreadBills;

    public MerchantPortalController(MerchantPortalService service, MerchantZoneNoticeService notices,
            MerchantFeeStatementService feeStatements, MerchantUnreadBillService unreadBills) {
        this.service = service;
        this.notices = notices;
        this.feeStatements = feeStatements;
        this.unreadBills = unreadBills;
    }

    /** 第 92 条（BSM:1597「商户专区首页保留未读账单提醒，该提醒不因异议期届满而消失。」）：本商户未读账单，只读。 */
    @GetMapping("/unread-bills")
    public ApiResponse<MerchantUnreadBillService.UnreadBills> unreadBills() {
        return ApiResponse.ok(unreadBills.unread());
    }

    /** 18E 第 21 条（BSM:1323）：本商户费用总表与当期（计费自然月）综合账单，只读；month 缺省取当月。 */
    @GetMapping("/fee-statement")
    public ApiResponse<MerchantFeeStatementService.FeeStatement> feeStatement(
            @RequestParam(name = "month", required = false) String month) {
        return ApiResponse.ok(feeStatements.statement(month));
    }

    /** 18B 复核后操作员裁决第 10 条「商户专区显示通告」：本商户的通告，专区可查看即送达。 */
    @GetMapping("/notices")
    public ApiResponse<java.util.List<MerchantZoneNoticeService.Notice>> listMyNotices() {
        return ApiResponse.ok(notices.listMine());
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

    /** HP-C04（2026-10-18 裁决第 6 条）：主管裁决后商户「不接受裁决」→ 结算单进「有争议，诉讼/仲裁」挂起。 */
    @PostMapping("/settlements/{settlement_id}/reject-resolution")
    public ApiResponse<ContractSettlementRecord> rejectSettlementResolution(
            @PathVariable("settlement_id") String settlementId,
            @RequestBody @jakarta.validation.Valid MerchantSettlementDisputeRequest request
    ) {
        ApiRequests.requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.rejectSettlementResolution(settlementId, request));
    }
}
