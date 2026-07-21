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
     * 过账网关(②):把某 confirmed 结算单的各酒吧分账挂 finance 应付台账。仅 confirmed 单(draft/settled 由 postBill 拒);
     * 幂等(重复过账不重复挂);返回本次过账明细笔数。高敏权限 settlement.bill.post + 平台域。
     */
    @PostMapping("/bills/{id}/post")
    public ApiResponse<Integer> postBill(@PathVariable("id") String billId) {
        authorizationService.requirePermission("settlement.bill.post");
        authorizationService.requirePlatformScope();
        return ApiResponse.ok(postingService.postBill(billId));
    }
}
