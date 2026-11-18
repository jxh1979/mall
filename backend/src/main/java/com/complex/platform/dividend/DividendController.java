package com.complex.platform.dividend;

// 文件版本：V0.04

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.dividend.DividendRecords.DividendComputeResultRecord;
import com.complex.platform.dividend.DividendRecords.DividendPeriodHoldRecord;
import com.complex.platform.dividend.DividendRecords.DividendPlanRecord;
import com.complex.platform.dividend.DividendRecords.DividendReserveLedgerRecord;
import com.complex.platform.dividend.DividendRecords.DividendReserveReleaseRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementDetailRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementRecord;
import com.complex.platform.dividend.dto.DividendActionRequest;
import com.complex.platform.dividend.dto.DividendCloseoutRequest;
import com.complex.platform.dividend.dto.DividendComputeRequest;
import com.complex.platform.dividend.dto.DividendHoldRequest;
import com.complex.platform.dividend.dto.DividendPlanRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 持股分红端点（V299）。权限：view=方案/计算单只读（商户 scope 强制），manage=配置/算单/审批/发放。 */
@RestController
@RequestMapping("/api/v1/finance/dividends")
public class DividendController {

    private final DividendService service;
    private final DividendCloseoutService closeouts;

    public DividendController(DividendService service, DividendCloseoutService closeouts) {
        this.service = service;
        this.closeouts = closeouts;
    }

    @GetMapping("/plans")
    public ApiResponse<List<DividendPlanRecord>> listPlans(
            @RequestParam(value = "merchant_id", required = false) String merchantId) {
        return ApiResponse.ok(service.listPlans(merchantId));
    }

    @PostMapping("/plans")
    public ApiResponse<DividendPlanRecord> createPlan(@RequestBody DividendPlanRequest request) {
        return ApiResponse.ok(service.createPlan(request));
    }

    @PutMapping("/plans/{id}")
    public ApiResponse<DividendPlanRecord> updatePlan(
            @PathVariable("id") String planId,
            @RequestBody DividendPlanRequest request) {
        return ApiResponse.ok(service.updatePlan(planId, request));
    }

    @PostMapping("/statements/compute")
    public ApiResponse<DividendComputeResultRecord> compute(@RequestBody DividendComputeRequest request) {
        return ApiResponse.ok(service.compute(request));
    }

    @GetMapping("/statements")
    public ApiResponse<PageResponse<DividendStatementRecord>> listStatements(
            @RequestParam(value = "merchant_id", required = false) String merchantId,
            @RequestParam(value = "kind", required = false) String kind,
            @RequestParam(value = "period_label", required = false) String periodLabel,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "50") int pageSize) {
        return ApiResponse.ok(service.listStatements(merchantId, kind, periodLabel, status, page, pageSize));
    }

    @GetMapping("/statements/{id}")
    public ApiResponse<DividendStatementDetailRecord> getStatement(@PathVariable("id") String statementId) {
        return ApiResponse.ok(service.getStatement(statementId));
    }

    /** FS-014 财务登记依法可分配金额（finance.dividend.manage；仅正式草稿/已驳回单，按核定额取小重算）。 */
    @PostMapping("/statements/{id}/legal-confirmation")
    public ApiResponse<DividendStatementRecord> confirmLegal(
            @PathVariable("id") String statementId,
            @RequestBody com.complex.platform.dividend.dto.DividendLegalConfirmationRequest request) {
        return ApiResponse.ok(service.confirmLegal(statementId, request));
    }

    @PostMapping("/statements/{id}/submit")
    public ApiResponse<DividendStatementRecord> submit(
            @PathVariable("id") String statementId, @RequestBody DividendActionRequest request) {
        return ApiResponse.ok(service.submit(statementId, request));
    }

    @PostMapping("/statements/{id}/approve")
    public ApiResponse<DividendStatementRecord> approve(
            @PathVariable("id") String statementId, @RequestBody DividendActionRequest request) {
        return ApiResponse.ok(service.approve(statementId, request));
    }

    @PostMapping("/statements/{id}/reject")
    public ApiResponse<DividendStatementRecord> reject(
            @PathVariable("id") String statementId, @RequestBody DividendActionRequest request) {
        return ApiResponse.ok(service.reject(statementId, request));
    }

    @PostMapping("/statements/{id}/mark-paid")
    public ApiResponse<DividendStatementRecord> markPaid(
            @PathVariable("id") String statementId, @RequestBody DividendActionRequest request) {
        return ApiResponse.ok(service.markPaid(statementId, request));
    }

    // ── V304 发放保护线留存·释放 ──────────────────────────────

    /** 待释放/已释放留存台账（商户 scope 强制）；released=pending 仅待释放 / released 仅已释放 / 空=全部。 */
    @GetMapping("/reserve-releases")
    public ApiResponse<PageResponse<DividendReserveLedgerRecord>> listReserveLedger(
            @RequestParam(value = "merchant_id", required = false) String merchantId,
            @RequestParam(value = "released", required = false) String released,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "50") int pageSize) {
        return ApiResponse.ok(service.listReserveLedger(merchantId, released, page, pageSize));
    }

    /** 手动释放某计算单的发放保护线留存（finance.dividend.manage）。 */
    @PostMapping("/statements/{id}/release-reserve")
    public ApiResponse<DividendReserveReleaseRecord> releaseReserve(
            @PathVariable("id") String statementId, @RequestBody DividendActionRequest request) {
        return ApiResponse.ok(service.releaseReserve(statementId, request));
    }

    // ── 211-R1 整期暂不分红／暂缓分红登记（第十八批裁决 43）──────────

    /** 登记列表（finance.dividend.view，平台口径）。 */
    @GetMapping("/holds")
    public ApiResponse<List<DividendPeriodHoldRecord>> listHolds() {
        return ApiResponse.ok(service.listHolds());
    }

    /** 登记暂不分红或暂缓分红（finance.dividend.manage；该期正式草稿随登记作废）。 */
    @PostMapping("/holds")
    public ApiResponse<DividendPeriodHoldRecord> recordHold(@RequestBody DividendHoldRequest request) {
        return ApiResponse.ok(service.recordHold(request));
    }

    /** 恢复暂缓分红（finance.dividend.manage；只对未恢复的暂缓分红）。 */
    @PostMapping("/holds/{hold_id}/lift")
    public ApiResponse<DividendPeriodHoldRecord> liftHold(
            @PathVariable("hold_id") String holdId, @RequestBody DividendHoldRequest.Lift request) {
        return ApiResponse.ok(service.liftHold(holdId, request));
    }

    // ── 第十八批 AQ 段第 243 条 243-R1 酒吧关停结算（BSM:1787）──────────

    /** 关停结算列表（finance.dividend.view，平台口径）；merchant_id 非空只看该商户。待提交核实的款项现查，其余取提交核实时的快照。 */
    @GetMapping("/closeouts")
    public ApiResponse<List<DividendCloseoutService.Closeout>> listCloseouts(
            @RequestParam(value = "merchant_id", required = false) String merchantId) {
        return ApiResponse.ok(closeouts.list(merchantId));
    }

    /** 登记关停结算（finance.dividend.manage；只适用于酒吧，一家酒吧一条；不改商户状态、不动应付）。 */
    @PostMapping("/closeouts")
    public ApiResponse<DividendCloseoutService.Closeout> registerCloseout(@RequestBody DividendCloseoutRequest request) {
        return ApiResponse.ok(closeouts.register(request));
    }

    /** 提交财务、法务核实（finance.dividend.manage；本次分红出单后，逐条填法律性质与处理方式，进统一审批）。 */
    @PostMapping("/closeouts/{id}/submit")
    public ApiResponse<DividendCloseoutService.Closeout> submitCloseout(
            @PathVariable("id") String closeoutId, @RequestBody DividendCloseoutRequest.Submit request) {
        return ApiResponse.ok(closeouts.submit(closeoutId, request));
    }

    /** 按核实结果登记一条款项的处理（finance.dividend.manage；核实通过后，每条一次）。 */
    @PostMapping("/closeouts/{id}/items/{item_id}/handle")
    public ApiResponse<DividendCloseoutService.Closeout> handleCloseoutItem(
            @PathVariable("id") String closeoutId, @PathVariable("item_id") String itemId,
            @RequestBody DividendCloseoutRequest.Handle request) {
        return ApiResponse.ok(closeouts.handle(closeoutId, itemId, request));
    }

    /** 结案（finance.dividend.manage；核实通过且每条款项都已登记处理）。 */
    @PostMapping("/closeouts/{id}/close")
    public ApiResponse<DividendCloseoutService.Closeout> closeCloseout(
            @PathVariable("id") String closeoutId, @RequestBody DividendActionRequest request) {
        return ApiResponse.ok(closeouts.close(closeoutId, request));
    }
}
