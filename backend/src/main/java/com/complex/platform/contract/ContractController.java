package com.complex.platform.contract;

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.contract.dto.ContractActivateRequest;
import com.complex.platform.contract.dto.ContractApprovalFlowRequest;
import com.complex.platform.contract.dto.ContractApproveRequest;
import com.complex.platform.contract.dto.ContractChangeApproveRequest;
import com.complex.platform.contract.dto.ContractChangeRequest;
import com.complex.platform.contract.dto.ContractDraftRequest;
import com.complex.platform.contract.dto.ContractResolveDisputeRequest;
import com.complex.platform.contract.dto.ContractSettleRequest;
import com.complex.platform.contract.dto.ContractSubmitRequest;
import com.complex.platform.contract.dto.SpaceExpiryColorRuleRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 合同租赁全生命周期 API（V162 重建）：起草 → 提交 → 分组并行审批 → 生效固化 → 变更 → 退租结算 + 配置。 */
@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {

    private final ContractService service;
    private final AuthorizationService authorizationService;

    public ContractController(ContractService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    // ---- 起草 ----
    @GetMapping
    public ApiResponse<PageResponse<ContractRecord>> list() {
        authorizationService.requirePermission("contract.draft.manage");
        return ApiResponse.ok(service.listContracts());
    }

    @PostMapping
    public ApiResponse<ContractRecord> createDraft(@RequestBody ContractDraftRequest request) {
        authorizationService.requirePermission("contract.draft.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createDraft(request));
    }

    // ---- 审批 ----
    @PostMapping("/{contract_id}/submit")
    public ApiResponse<ContractRecord> submit(
            @PathVariable("contract_id") String contractId, @RequestBody ContractSubmitRequest request) {
        authorizationService.requirePermission("contract.draft.manage");
        String key = request == null ? null : request.idempotencyKey();
        requireIdempotencyKey(key);
        return ApiResponse.ok(service.submit(contractId, key));
    }

    /** 审批：权限按 body.approverPermissionCode 在 service 内动态校验（当前阶段并行槽）。 */
    @PostMapping("/{contract_id}/approve")
    public ApiResponse<ContractRecord> approve(
            @PathVariable("contract_id") String contractId, @RequestBody ContractApproveRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approve(contractId, request));
    }

    @GetMapping("/{contract_id}/approvals")
    public ApiResponse<PageResponse<ContractApprovalRecord>> listApprovals(
            @PathVariable("contract_id") String contractId) {
        authorizationService.requirePermission("contract.draft.manage");
        return ApiResponse.ok(service.listApprovals(contractId));
    }

    // ---- 生效 ----
    @PostMapping("/{contract_id}/activate")
    public ApiResponse<ContractRecord> activate(
            @PathVariable("contract_id") String contractId, @RequestBody ContractActivateRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.activate(contractId, request));
    }

    // ---- 变更 ----
    @GetMapping("/{contract_id}/changes")
    public ApiResponse<PageResponse<ContractChangeRecord>> listChanges(
            @PathVariable("contract_id") String contractId) {
        authorizationService.requirePermission("contract.change.manage");
        return ApiResponse.ok(service.listChanges(contractId));
    }

    @PostMapping("/{contract_id}/changes")
    public ApiResponse<ContractChangeRecord> raiseChange(
            @PathVariable("contract_id") String contractId, @RequestBody ContractChangeRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.raiseChange(contractId, request));
    }

    @PostMapping("/changes/{change_id}/approve")
    public ApiResponse<ContractChangeRecord> approveChange(
            @PathVariable("change_id") String changeId, @RequestBody ContractChangeApproveRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveChange(changeId, request));
    }

    // ---- 退租结算 ----
    @PostMapping("/{contract_id}/settle")
    public ApiResponse<ContractSettlementRecord> settle(
            @PathVariable("contract_id") String contractId, @RequestBody ContractSettleRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.settle(contractId, request));
    }

    @GetMapping("/{contract_id}/settlements")
    public ApiResponse<PageResponse<ContractSettlementRecord>> listSettlements(
            @PathVariable("contract_id") String contractId) {
        authorizationService.requirePermission("contract.settlement.manage");
        return ApiResponse.ok(service.listSettlements(contractId));
    }

    @PostMapping("/settlements/{settlement_id}/resolve-dispute")
    public ApiResponse<ContractSettlementRecord> resolveDispute(
            @PathVariable("settlement_id") String settlementId, @RequestBody ContractResolveDisputeRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.resolveDispute(settlementId, request));
    }

    // ---- 配置：审批链 ----
    @GetMapping("/approval-flow")
    public ApiResponse<List<ContractApprovalFlowStepRecord>> listApprovalFlow(
            @RequestParam(value = "project_id", required = false) String projectId) {
        authorizationService.requirePermission("contract.approval.flow.manage");
        return ApiResponse.ok(service.listApprovalFlow(projectId));
    }

    @PostMapping("/approval-flow")
    public ApiResponse<List<ContractApprovalFlowStepRecord>> saveApprovalFlow(
            @RequestBody ContractApprovalFlowRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.saveApprovalFlow(request));
    }

    // ---- 配置：到期着色分类 ----
    @GetMapping("/expiry-color-rules")
    public ApiResponse<List<SpaceExpiryColorRuleRecord>> listColorRules(
            @RequestParam(value = "project_id", required = false) String projectId) {
        authorizationService.requirePermission("contract.space.color.manage");
        return ApiResponse.ok(service.listColorRules(projectId));
    }

    /** 铺位平面图着色数据源（每铺：可租/已租/即将到期 + 命中色）。 */
    @GetMapping("/space-rental-status")
    public ApiResponse<List<SpaceRentalStatusRecord>> spaceRentalStatus(
            @RequestParam(value = "project_id", required = false) String projectId) {
        authorizationService.requirePermission("contract.draft.manage");
        return ApiResponse.ok(service.spaceRentalStatus(projectId));
    }

    @PostMapping("/expiry-color-rules")
    public ApiResponse<SpaceExpiryColorRuleRecord> saveColorRule(@RequestBody SpaceExpiryColorRuleRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.saveColorRule(request));
    }
}
