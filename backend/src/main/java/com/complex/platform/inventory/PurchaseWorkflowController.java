package com.complex.platform.inventory;

// 文件版本：V0.02

import com.complex.platform.common.api.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
public class PurchaseWorkflowController {
    private final PurchaseWorkflowService service;
    private final PurchasePrepaymentService prepayments;
    public PurchaseWorkflowController(PurchaseWorkflowService service,PurchasePrepaymentService prepayments){this.service=service;this.prepayments=prepayments;}
    @GetMapping("/purchase-workbench-options") public ApiResponse<Map<String,Object>> options(){return ApiResponse.ok(service.options());}
    /** 第十八批第 33 条：智能采购建议（只读即时计算，不建单、不下单）。 */
    @GetMapping("/purchase-suggestions") public ApiResponse<List<Map<String,Object>>> suggestions(@RequestParam("project_id") String projectId){return ApiResponse.ok(service.suggestions(projectId));}
    @GetMapping("/purchase-requests/{id}") public ApiResponse<Map<String,Object>> detail(@PathVariable String id){return ApiResponse.ok(service.detail(id));}
    @PostMapping("/supply-contracts/{id}/terms") public ApiResponse<Map<String,Object>> terms(@PathVariable String id,@RequestBody PurchaseWorkflowRequests.Terms request){return ApiResponse.ok(service.terms(id,request));}
    @GetMapping("/supply-contracts/{id}/terms") public ApiResponse<Map<String,Object>> terms(@PathVariable String id){return ApiResponse.ok(service.contractDetail(id));}
    @PostMapping("/purchase-requests/{id}/resolutions") public ApiResponse<Map<String,Object>> resolution(@PathVariable String id,@RequestBody PurchaseWorkflowRequests.Resolution request){return ApiResponse.ok(service.resolveQuantity(id,request));}
    @PostMapping("/purchase-requests/{id}/consignment-sales") public ApiResponse<Map<String,Object>> sale(@PathVariable String id,@RequestBody PurchaseWorkflowRequests.Sale request){return ApiResponse.ok(service.sale(id,request));}
    @PostMapping("/purchase-consignment-sales/{id}/confirm") public ApiResponse<Map<String,Object>> confirm(@PathVariable String id,@RequestBody PurchaseWorkflowRequests.SaleConfirmation request){return ApiResponse.ok(service.confirmSale(id,request));}
    @PostMapping("/purchase-requests/{id}/prepayment-allocations") public ApiResponse<Map<String,Object>> allocation(@PathVariable String id,@RequestBody PurchaseWorkflowRequests.Allocation request){return ApiResponse.ok(service.allocate(id,request));}
    @PostMapping("/purchase-requests/{id}/prepayment-resolutions") public ApiResponse<Map<String,Object>> prepaymentResolution(@PathVariable String id,@RequestBody PurchaseWorkflowRequests.PrepaymentResolution request){return ApiResponse.ok(prepayments.resolve(id,request));}
    @PostMapping("/purchase-prepayment-resolutions/{id}/post") public ApiResponse<Map<String,Object>> postPrepayment(@PathVariable String id,@RequestBody PurchaseWorkflowRequests.PrepaymentPost request){return ApiResponse.ok(prepayments.post(id,request));}
    @PostMapping("/purchase-requests/{id}/returns") public ApiResponse<Map<String,Object>> returns(@PathVariable String id,@RequestBody PurchaseWorkflowRequests.Return request){return ApiResponse.ok(service.proposeReturn(id,request));}
    @PostMapping("/purchase-returns/{id}/finance-review") public ApiResponse<Map<String,Object>> returnFinance(@PathVariable String id,@RequestBody PurchaseWorkflowRequests.ReturnFinanceReview request){return ApiResponse.ok(service.reviewReturnFinance(id,request));}
    @PostMapping("/purchase-returns/{id}/handover") public ApiResponse<Map<String,Object>> handover(@PathVariable String id,@RequestBody PurchaseWorkflowRequests.Handover request){return ApiResponse.ok(service.handover(id,request));}
}
