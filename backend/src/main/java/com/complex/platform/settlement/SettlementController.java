package com.complex.platform.settlement;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台侧结算审阅(2026-07-05 审计断层②):只读查看通票核销产生的结算流水。
 * 权限 finance.settlement.view(V245);不含确认/入账等写操作(月度成账仍由 Job 负责)。
 */
@RestController
@RequestMapping("/api/v1/settlement")
public class SettlementController {

    private final SettlementQueryService service;
    private final AuthorizationService authorizationService;

    public SettlementController(SettlementQueryService service, AuthorizationService authorizationService) {
        this.service = service;
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
}
