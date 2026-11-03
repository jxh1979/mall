package com.complex.platform.settlement;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台侧结算审阅(2026-07-05 审计断层②):只读查看通票核销产生的结算流水。
 * 权限 finance.settlement.view(V245);读取不含写。
 *
 * <p>批1 补过账触发缺口②:{@code POST /bills/{id}/post} 网关端点——把 confirmed 结算单的各酒吧分账
 * 挂 finance 应付台账(委托 {@link SettlementPostingService#postBill},幂等·@Transactional·仅 confirmed 单)。
 * 供人工过账;常规单由 {@link SettlementMonthlyJob} 自动过账。第 75 条起结算环节不设超池审批(ENUM L52),超池单同样 confirmed 即可过账。
 * 🔴 落码≠上线:本端点是运营激活手段之一,生产首次真过账须另行拍板,非部署默认动作。
 */
@RestController
@RequestMapping("/api/v1/settlement")
public class SettlementController {

    private final SettlementQueryService service;
    private final SettlementPostingService postingService;
    private final AuthorizationService authorizationService;
    private final com.complex.platform.evidence.AuditedOperationExecutor operations;

    public SettlementController(SettlementQueryService service, SettlementPostingService postingService,
                                AuthorizationService authorizationService, com.complex.platform.evidence.AuditedOperationExecutor operations) {
        this.service = service;
        this.postingService = postingService;
        this.authorizationService = authorizationService;
        this.operations = operations;
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
     * 第十八批 N 段第 124 条 124-R4（PGM L966「所有有过程的业务动作都必须留痕」、PGM L738）：人工过账成功落 settlement.bill.posted
     * （business_id 结算单 id，记本次过账笔数），任一步失败（权限、范围、未确认单）落 settlement.bill.post_failed；月结作业自动过账不经此入口。
     */
    @PostMapping("/bills/{id}/post")
    public ApiResponse<Integer> postBill(@PathVariable("id") String billId) {
        return ApiResponse.ok(operations.recordingFailure("settlement.bill.post_failed", "settlement.bill.post", "settlement_bill", billId,
                null, () -> {
                    authorizationService.requirePermission("settlement.bill.post");
                    authorizationService.requirePlatformScope();
                    int posted = postingService.postBill(billId);
                    operations.recordDetail("settlement.bill.posted", "settlement.bill.post", "settlement_bill", billId,
                            java.util.Map.of("posted_count", posted));
                    return posted;
                }));
    }
}
