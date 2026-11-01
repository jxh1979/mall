package com.complex.platform.merchantservice;
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderActionRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderAssignRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCloseMethodRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCloseRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderCreateRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderDeleteApprovalRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderDeleteDecision;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderDeleteRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderMerchantConfirmRequest;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderOwnerCandidate;
import com.complex.platform.merchantservice.dto.MerchantWorkOrderPolicyRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant-service")
public class MerchantServiceController {

    private final MerchantServiceService service;
    private final AuthorizationService authorizationService;

    public MerchantServiceController(
            MerchantServiceService service,
            AuthorizationService authorizationService
    ) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/work-orders")
    public ApiResponse<PageResponse<MerchantWorkOrderRecord>> listWorkOrders(
            @RequestParam(value = "include_archived", required = false, defaultValue = "false") boolean includeArchived) {
        authorizationService.requirePermission("merchant.service_work_order.view");
        return ApiResponse.ok(service.listWorkOrders(includeArchived));
    }

    @PostMapping("/work-orders")
    public ApiResponse<MerchantWorkOrderRecord> createWorkOrder(@RequestBody @jakarta.validation.Valid MerchantWorkOrderCreateRequest request) {
        authorizationService.requirePermission("merchant.service_work_order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createWorkOrder(request));
    }

    // 第十八批 L 段第 102 条 102-R1：派单负责人下拉（同派单闸）。
    @GetMapping("/work-orders/{id}/owner-candidates")
    public ApiResponse<List<MerchantWorkOrderOwnerCandidate>> listOwnerCandidates(@PathVariable String id) {
        authorizationService.requirePermission("merchant.service_work_order.assign");
        return ApiResponse.ok(service.listOwnerCandidates(id));
    }

    @PostMapping("/work-orders/{id}/assign")
    public ApiResponse<MerchantWorkOrderRecord> assignWorkOrder(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid MerchantWorkOrderAssignRequest request
    ) {
        authorizationService.requirePermission("merchant.service_work_order.assign");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.assignWorkOrder(id, request));
    }

    @PostMapping("/work-orders/{id}/actions")
    public ApiResponse<MerchantWorkOrderRecord> addAction(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid MerchantWorkOrderActionRequest request
    ) {
        authorizationService.requirePermission("merchant.service_work_order.process");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.addAction(id, request));
    }

    @PostMapping("/work-orders/{id}/close")
    public ApiResponse<MerchantWorkOrderRecord> closeWorkOrder(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid MerchantWorkOrderCloseRequest request
    ) {
        authorizationService.requirePermission("merchant.service_work_order.close");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.closeWorkOrder(id, request));
    }

    // 第十八批 L 段第 102 条 102-R2：商户在商户专区确认已解决即关闭（提单方权限）。
    @PostMapping("/work-orders/{id}/merchant-confirm")
    public ApiResponse<MerchantWorkOrderRecord> confirmByMerchant(
            @PathVariable String id,
            @RequestBody MerchantWorkOrderMerchantConfirmRequest request
    ) {
        authorizationService.requirePermission("merchant.service_work_order.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.confirmByMerchant(id, request));
    }

    // ── 第十八批 I 段 77-R3 处理时限（工单类型×优先级）───────────────────────
    @GetMapping("/work-order-policies")
    public ApiResponse<List<MerchantWorkOrderPolicyRecord>> listPolicies() {
        authorizationService.requirePermission("merchant.service_work_order.assign");
        return ApiResponse.ok(service.listPolicies());
    }

    @PutMapping("/work-order-policies")
    public ApiResponse<MerchantWorkOrderPolicyRecord> savePolicy(@RequestBody MerchantWorkOrderPolicyRequest request) {
        authorizationService.requirePermission("merchant.service_work_order.assign");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.savePolicy(request));
    }

    // ── 第十八批 L 段第 102 条 102-R3 关单方式（报修、其他服务请求；裁决 26）───────────────
    // 读按查看码：后台处理时限小节与商户专区（只在「须商户确认」的单上出确认按钮）都要读。
    @GetMapping("/work-order-close-methods")
    public ApiResponse<List<MerchantWorkOrderCloseMethodRecord>> listCloseMethods() {
        authorizationService.requirePermission("merchant.service_work_order.view");
        return ApiResponse.ok(service.listCloseMethods());
    }

    @PutMapping("/work-order-close-methods")
    public ApiResponse<MerchantWorkOrderCloseMethodRecord> saveCloseMethod(@RequestBody MerchantWorkOrderCloseMethodRequest request) {
        authorizationService.requirePermission("merchant.service_work_order.assign");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.saveCloseMethod(request));
    }

    // ── V264 数据存档与审批后删除 ──────────────────────────────────────────
    @PostMapping("/work-orders/{id}/archive")
    public ApiResponse<MerchantWorkOrderRecord> archiveWorkOrder(@PathVariable String id,
            @org.springframework.web.bind.annotation.RequestBody(required = false) com.complex.platform.merchantservice.dto.MerchantWorkOrderArchiveRequest request) {
        authorizationService.requirePermission("merchant.service_work_order.close");
        return ApiResponse.ok(service.archiveWorkOrder(id, true, request == null ? null : request.reason()));
    }

    @PostMapping("/work-orders/{id}/unarchive")
    public ApiResponse<MerchantWorkOrderRecord> unarchiveWorkOrder(@PathVariable String id) {
        authorizationService.requirePermission("merchant.service_work_order.close");
        return ApiResponse.ok(service.archiveWorkOrder(id, false, null));
    }

    @PostMapping("/work-orders/{id}/delete-request")
    public ApiResponse<MerchantWorkOrderRecord> requestDeleteWorkOrder(
            @PathVariable String id, @RequestBody(required = false) MerchantWorkOrderDeleteRequest request) {
        authorizationService.requirePermission("merchant.service_work_order.close");
        return ApiResponse.ok(service.requestDeleteWorkOrder(id, request));
    }

    @PostMapping("/work-orders/{id}/delete-approve")
    public ApiResponse<MerchantWorkOrderDeleteDecision> approveDeleteWorkOrder(
            @PathVariable String id, @RequestBody(required = false) MerchantWorkOrderDeleteApprovalRequest request) {
        authorizationService.requirePermission("merchant.service_work_order.approve");
        return ApiResponse.ok(service.approveDeleteWorkOrder(id, request));
    }
}
