package com.complex.platform.settlement;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台侧结算审阅(2026-07-05 审计断层②):只读查看通票核销产生的结算流水。
 * 权限 finance.settlement.view(V245);读取不含写。
 *
 * <p>批1 补过账触发缺口②:{@code POST /bills/{id}/post} 网关端点——把 confirmed 结算单的各酒吧分账
 * 挂 finance 应付台账(委托 {@link SettlementPostingService#postBill},幂等·@Transactional·仅 confirmed 单)。
 * 供超池特批 confirmed 后人工过账;常规单由 {@link SettlementMonthlyJob} 自动过账。
 * 🔴 落码≠上线:本端点是运营激活手段之一,生产首次真过账须另行拍板,非部署默认动作。
 */
@RestController
@RequestMapping("/api/v1/settlement")
public class SettlementController {

    private final SettlementQueryService service;
    private final SettlementPostingService postingService;
    private final AuthorizationService authorizationService;

    public SettlementController(SettlementQueryService service, SettlementPostingService postingService,
                                AuthorizationService authorizationService) {
        this.service = service;
        this.postingService = postingService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/transactions")
    public ApiResponse<PageResponse<JdbcSettlementTransactionRepository.SettlementTransactionView>> listSettlements(
            @RequestParam(name = "projectId", required = false) String projectId,
            @RequestParam(name = "merchantId", required = false) String merchantId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        authorizationService.requirePermission("finance.settlement.view");
        return ApiResponse.ok(service.listSettlements(projectId, merchantId, page, size));
    }

    /**
     * S4 财务对账:月度结算单头列表(含门票综合服务费两支与收款主体)。只读;权限同结算流水审阅。
     * month 形如 2026-06(留空 = 近 24 单)。
     */
    @GetMapping("/bills")
    public ApiResponse<java.util.List<JdbcSettlementBillRepository.BillSummary>> listBills(
            @RequestParam(name = "projectId", required = false) String projectId,
            @RequestParam(name = "month", required = false) String month) {
        authorizationService.requirePermission("finance.settlement.view");
        return ApiResponse.ok(service.listBills(projectId, month));
    }

    /**
     * 过账网关(②):把某 confirmed 结算单的各酒吧分账挂 finance 应付台账。仅 confirmed 单(draft/settled 由 postBill 拒);
     * 幂等(重复过账不重复挂);返回本次过账明细笔数。高敏权限 settlement.bill.post + 平台域。
     */
    @PostMapping("/bills/{id}/post")
    public ApiResponse<Integer> postBill(@PathVariable("id") String billId) {
        authorizationService.requirePermission("settlement.bill.post");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(postingService.postBill(billId));
    }

    /**
     * D7 超池特批出口:把某<b>超池 draft</b> 结算单填特批引用并置 confirmed(可再走 postBill 过账)。
     * 仅超池 draft 单可特批(非超池 draft/已 confirmed/settled 由服务层 409 BILL_NOT_OVERPOOL_DRAFT 拒)。
     *
     * <p>v7 审计 R-2(V357):改用<b>专属权限点</b> settlement.bill.special_approve(高敏·仅授平台负责人)
     * + 平台域——与常规过账 settlement.bill.post 分权:过账可下放财务运营,超池特批只能平台负责人本人批。
     * 返回特批引用 + 本次特批的到期日(S9 短期激励期限,到期自动失效)。
     */
    @PostMapping("/bills/{id}/special-approval")
    public ApiResponse<SettlementPostingService.SpecialApprovalResult> specialApprove(
            @PathVariable("id") String billId, @RequestBody SpecialApprovalRequest request) {
        authorizationService.requirePermission("settlement.bill.special_approve");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(postingService.specialApprove(billId,
                request == null ? null : request.specialApprovalRef()));
    }

    /** 特批请求体：平台负责人特批短期激励的凭据引用。 */
    public record SpecialApprovalRequest(String specialApprovalRef) {
    }
}
