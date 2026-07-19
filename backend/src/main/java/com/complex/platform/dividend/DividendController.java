package com.complex.platform.dividend;

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.dividend.DividendRecords.DividendComputeResultRecord;
import com.complex.platform.dividend.DividendRecords.DividendPlanRecord;
import com.complex.platform.dividend.DividendRecords.DividendReserveLedgerRecord;
import com.complex.platform.dividend.DividendRecords.DividendReserveReleaseRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementDetailRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementRecord;
import com.complex.platform.dividend.dto.DividendActionRequest;
import com.complex.platform.dividend.dto.DividendComputeRequest;
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

    public DividendController(DividendService service) {
        this.service = service;
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
}
