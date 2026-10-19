package com.complex.platform.contract;

// 文件版本：V0.01

import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.contract.dto.ContractActivateRequest;
import com.complex.platform.contract.dto.ContractApprovalFlowRequest;
import com.complex.platform.contract.dto.ContractAttachmentRequest;
import com.complex.platform.contract.dto.ContractChangeRequest;
import com.complex.platform.contract.dto.ContractDraftRentTermsRequest;
import com.complex.platform.contract.dto.ContractDraftRequest;
import com.complex.platform.contract.dto.ContractRentBillRequest;
import com.complex.platform.contract.dto.ContractResolveDisputeRequest;
import com.complex.platform.contract.dto.ContractScopeRequestRequest;
import com.complex.platform.contract.dto.ContractSettleRequest;
import com.complex.platform.contract.dto.ContractSubmitRequest;
import com.complex.platform.contract.dto.DepositMonthsSettingRequest;
import com.complex.platform.contract.dto.DepositRefundWaitSettingRequest;
import com.complex.platform.contract.dto.SpaceExpiryColorRuleRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合同租赁全生命周期 API（V162 重建）：起草 → 提交统一审批（HP-C01：按三类模板开复核单，审批人在
 * /api/v1/platform/supervisor-review/{review_id}/approve 逐节点裁决、/chain 查看节点）→ 生效固化 → 变更 → 退租结算 + 配置。
 */
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
    public ApiResponse<PageResponse<ContractRecord>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "500") int size) {
        authorizationService.requirePermission("contract.draft.manage");
        return ApiResponse.ok(service.listContracts(page, size));
    }

    @PostMapping
    public ApiResponse<ContractRecord> createDraft(@RequestBody @jakarta.validation.Valid ContractDraftRequest request) {
        authorizationService.requirePermission("contract.draft.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createDraft(request));
    }

    @PostMapping("/{contract_id}/attachments")
    public ApiResponse<ContractRecord> addAttachments(
            @PathVariable("contract_id") String contractId, @RequestBody @jakarta.validation.Valid ContractAttachmentRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.addAttachments(contractId, request));
    }

    /**
     * 第四轮裁决第 4 条「合同草稿可编辑补录」：草拟中合同补录/修改租金条款（纯提成录约定押金）；权限码 contract.draft.manage、平台口径与项目范围
     * 在服务层校验（同起草）。
     */
    @PostMapping("/{contract_id}/rent-terms")
    public ApiResponse<ContractRecord> editDraftRentTerms(
            @PathVariable("contract_id") String contractId, @RequestBody @jakarta.validation.Valid ContractDraftRentTermsRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.editDraftRentTerms(contractId, request));
    }

    // ---- 审批（HP-C01：提交即开统一审批复核单；节点裁决走 supervisor-review，不再有合同域专用审批入口） ----
    /** 提交审批：指定主线最终审批人（空=模板默认）与模板要求的各平行线最终审批人。 */
    @PostMapping("/{contract_id}/submit")
    public ApiResponse<ContractRecord> submit(
            @PathVariable("contract_id") String contractId, @RequestBody @jakarta.validation.Valid ContractSubmitRequest request) {
        authorizationService.requirePermission("contract.draft.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.submit(contractId, request));
    }

    // ---- 生效 ----
    @PostMapping("/{contract_id}/activate")
    public ApiResponse<ContractRecord> activate(
            @PathVariable("contract_id") String contractId, @RequestBody @jakarta.validation.Valid ContractActivateRequest request) {
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
            @PathVariable("contract_id") String contractId, @RequestBody @jakarta.validation.Valid ContractChangeRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.raiseChange(contractId, request));
    }

    // ---- 超范围下单经营申请（#30）：拦下后可申请，按「变更」类模板走统一审批，通过则放行 ----
    @PostMapping("/{contract_id}/scope-requests")
    public ApiResponse<ContractChangeRecord> requestScopeGrant(
            @PathVariable("contract_id") String contractId,
            @RequestBody @jakarta.validation.Valid ContractScopeRequestRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.requestScopeGrant(contractId, request));
    }

    // ---- 变更子合同追溯（#31）：列某合同派生的变更子合同 ----
    @GetMapping("/{contract_id}/amendments")
    public ApiResponse<List<ContractRecord>> listAmendments(
            @PathVariable("contract_id") String contractId) {
        return ApiResponse.ok(service.listChildContracts(contractId));
    }

    // ---- 退租结算 ----
    @PostMapping("/{contract_id}/settle")
    public ApiResponse<ContractSettlementRecord> settle(
            @PathVariable("contract_id") String contractId, @RequestBody @jakarta.validation.Valid ContractSettleRequest request) {
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
            @PathVariable("settlement_id") String settlementId, @RequestBody @jakarta.validation.Valid ContractResolveDisputeRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.resolveDispute(settlementId, request));
    }

    // HP-C02（BSM L1352「审批全部通过后，系统按算好的结算清单自动落账，不再单设手动确认入口」）：原 POST /settlements/{id}/complete 手动结清入口已删除，
    // 到期结算由「到期终止」类复核单终审回写自动落账（ContractService.onReviewDecided）。

    /**
     * HP-C02 齐单核对（BSM L1352「退租对应期间的全部相关单据收齐并核对完成后，才允许退租结算最后审批通过；缺单时保持待处理」）：
     * 缺项 → 409 CONTRACT_SETTLEMENT_DOCUMENTS_INCOMPLETE 列明细；齐全 → 结算单记核对人/时间/备注。
     */
    @PostMapping("/settlements/{settlement_id}/documents-check")
    public ApiResponse<ContractSettlementRecord> checkSettlementDocuments(
            @PathVariable("settlement_id") String settlementId, @RequestBody @jakarta.validation.Valid ContractResolveDisputeRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.checkSettlementDocuments(settlementId, request));
    }

    // ---- 租金 AR 挂账 ----
    @PostMapping("/{contract_id}/rent-bills")
    public ApiResponse<ContractRentBillRecord> generateRentBill(
            @PathVariable("contract_id") String contractId, @RequestBody @jakarta.validation.Valid ContractRentBillRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.generateRentBill(contractId, request));
    }

    /** 一期全费目挂账（租金+物业费+能源费）。 */
    @PostMapping("/{contract_id}/period-bills")
    public ApiResponse<List<ContractRentBillRecord>> generatePeriodBills(
            @PathVariable("contract_id") String contractId, @RequestBody @jakarta.validation.Valid ContractRentBillRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.generatePeriodBills(contractId, request));
    }

    // ---- 配置：审批流程模板（V494：新建／到期终止／变更三类，按综合体可配；缺 contract_type 返回三类） ----
    @GetMapping("/approval-flow")
    public ApiResponse<List<ContractApprovalTemplateRecord>> listApprovalFlow(
            @RequestParam(value = "project_id", required = false) String projectId,
            @RequestParam(value = "contract_type", required = false) String contractType) {
        authorizationService.requirePermission("contract.approval.flow.manage");
        return ApiResponse.ok(service.listApprovalFlow(projectId, contractType));
    }

    @PostMapping("/approval-flow")
    public ApiResponse<ContractApprovalTemplateRecord> saveApprovalFlow(
            @RequestBody @jakarta.validation.Valid ContractApprovalFlowRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.saveApprovalFlow(request));
    }

    // ---- 配置：押金收取月数（HP-C03，BSM L1347「全平台统一可配……须同时配置上下限；下限不得低于三个月」；仅平台可改） ----
    @GetMapping("/deposit-months")
    public ApiResponse<DepositMonthsSettingRecord> getDepositMonthsSetting() {
        return ApiResponse.ok(service.getDepositMonthsSetting());
    }

    @PostMapping("/deposit-months")
    public ApiResponse<DepositMonthsSettingRecord> saveDepositMonthsSetting(
            @RequestBody @jakarta.validation.Valid DepositMonthsSettingRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.saveDepositMonthsSetting(request));
    }

    // ---- 配置：押金退还等待期（HP-C06，BSM L1352「业务参数，按项目可配，出厂默认 30 日，须同时配置上下限」） ----
    @GetMapping("/deposit-refund-wait")
    public ApiResponse<DepositRefundWaitSettingRecord> getDepositRefundWaitSetting(
            @RequestParam(value = "project_id", required = false) String projectId) {
        return ApiResponse.ok(service.getDepositRefundWaitSetting(projectId));
    }

    @PostMapping("/deposit-refund-wait")
    public ApiResponse<DepositRefundWaitSettingRecord> saveDepositRefundWaitSetting(
            @RequestBody @jakarta.validation.Valid DepositRefundWaitSettingRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.saveDepositRefundWaitSetting(request));
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
    public ApiResponse<SpaceExpiryColorRuleRecord> saveColorRule(@RequestBody @jakarta.validation.Valid SpaceExpiryColorRuleRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.saveColorRule(request));
    }
}
