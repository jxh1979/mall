package com.complex.platform.finance;

// 文件版本：V0.02

import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 税费申报登记端点（第十八批 AC 段第 210 条 210-R2）。view＝列表；manage＝登记并提交财税法审核与最高确认。
 * 第十八批 AR 段第 233 条：已生效申报的实际缴纳登记（233-R1）与差异调整单（233-R3），同走 manage 与财税法审核加最高确认；
 * 233-R2 税率版本：view 列全部版本，manage 提交新版本（财税法审核加最高确认后按生效日起用）。
 * 233-R6 主体收入、成本与利润：view 按经营主体与起止月汇总（第十八批裁决 60）。
 */
@RestController
@RequestMapping("/api/v1/finance/entity-taxes")
public class EntityTaxController {

    private final EntityTaxService service;

    public EntityTaxController(EntityTaxService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PageResponse<EntityTaxDeclaration>> list(
            @RequestParam(value = "business_entity_id", required = false) String businessEntityId,
            @RequestParam(value = "month", required = false) String month,
            @RequestParam(value = "tax_type", required = false) String taxType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "page_size", defaultValue = "50") int pageSize) {
        return ApiResponse.ok(service.list(businessEntityId, month, taxType, status, page, pageSize));
    }

    @PostMapping
    public ApiResponse<EntityTaxDeclaration> declare(@RequestBody EntityTaxDeclaration.Request request) {
        return ApiResponse.ok(service.declare(request));
    }

    @GetMapping("/rates")
    public ApiResponse<List<EntityTaxDeclaration.Rate>> listRates() {
        return ApiResponse.ok(service.listRates());
    }

    @PostMapping("/rates")
    public ApiResponse<EntityTaxDeclaration.Rate> submitRate(@RequestBody EntityTaxDeclaration.RateRequest request) {
        return ApiResponse.ok(service.submitRate(request));
    }

    @GetMapping("/profits")
    public ApiResponse<EntityProfitService.Profit> profit(
            @RequestParam(value = "business_entity_id", required = false) String businessEntityId,
            @RequestParam(value = "start_month", required = false) String startMonth,
            @RequestParam(value = "end_month", required = false) String endMonth) {
        return ApiResponse.ok(service.profit(businessEntityId, startMonth, endMonth));
    }

    @PostMapping("/{id}/payments")
    public ApiResponse<EntityTaxDeclaration.Payment> registerPayment(@PathVariable String id,
            @RequestBody EntityTaxDeclaration.PaymentRequest request) {
        return ApiResponse.ok(service.registerPayment(id, request));
    }

    @PostMapping("/{id}/adjustments")
    public ApiResponse<EntityTaxDeclaration.Adjustment> registerPeriodDifference(@PathVariable String id,
            @RequestBody EntityTaxDeclaration.AdjustmentRequest request) {
        return ApiResponse.ok(service.registerPeriodDifference(id, request));
    }
}
