package com.complex.platform.settlement;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 批 4b 开票清单(ADM-STL-002)。系统<b>只生成清单交财务</b>,不接税控盘、不自建开票引擎、不生成发票号
 * (业务 HTML:「开票由税务／电子发票第三方平台对接完成……只做状态对接与留痕」)。
 */
@RestController
@RequestMapping("/api/v1/settlement/invoice-items")
public class SettlementInvoiceController {

    private static final String PERM_VIEW = "settlement.invoice.view";
    private static final String PERM_MANAGE = "settlement.invoice.manage";

    private final SettlementInvoiceService service;
    private final AuthorizationService authorizationService;

    public SettlementInvoiceController(SettlementInvoiceService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    /** 开票清单分页列表(可按项目/结算月/开票主体/开票状态筛)。 */
    @GetMapping
    public ApiResponse<PageResponse<JdbcSettlementInvoiceRepository.SettlementInvoiceItemView>> list(
            @RequestParam(name = "projectId", required = false) String projectId,
            @RequestParam(name = "periodStart", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(name = "subjectEntityId", required = false) String subjectEntityId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        authorizationService.requirePermission(PERM_VIEW);
        return ApiResponse.ok(service.list(blankToNull(projectId), periodStart, blankToNull(subjectEntityId),
                blankToNull(status), page, size));
    }

    /** 按开票主体分组汇总(含税率税额)+ 开票映射规则 + 已超开告警。 */
    @GetMapping("/summary")
    public ApiResponse<SettlementInvoiceService.InvoiceSummaryView> summary(
            @RequestParam(name = "projectId", required = false) String projectId,
            @RequestParam(name = "periodStart", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart) {
        authorizationService.requirePermission(PERM_VIEW);
        return ApiResponse.ok(service.summarize(blankToNull(projectId), periodStart));
    }

    /** 按结算单生成/补齐开票清单(幂等;可开额 = 应开 − 已开,≤0 不生成)。返回本次新增条目数。 */
    @PostMapping("/generate")
    public ApiResponse<Integer> generate(@RequestParam(name = "billId") String billId) {
        return ApiResponse.ok(service.generateForBill(billId, requireManageActor()));
    }

    /** 流转开票状态(登记第三方开票结果 / 红冲 / 作废);本系统不发起开票。 */
    @PostMapping("/{id}/status")
    public ApiResponse<Void> changeStatus(@PathVariable("id") String itemId,
            @RequestBody StatusChangeRequest request) {
        String actorId = requireManageActor();
        service.changeStatus(itemId, request == null ? null : request.status(),
                request == null ? null : request.invoiceNo(), request == null ? null : request.remark(), actorId);
        return ApiResponse.ok(null);
    }

    private String requireManageActor() {
        authorizationService.requirePermission(PERM_MANAGE);
        authorizationService.requirePlatformScope();
        return authorizationService.currentActor().id();
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** 状态流转请求体:目标状态 + 第三方发票号(可空) + 备注(红冲/作废理由)。 */
    public record StatusChangeRequest(String status, String invoiceNo, String remark) {
    }
}
