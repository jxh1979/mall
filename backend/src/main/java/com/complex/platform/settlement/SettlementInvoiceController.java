package com.complex.platform.settlement;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.access.ConsumesSingleUseGrant;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.security.ExportFileService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
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
public class SettlementInvoiceController implements ExportFileService.Source {

    private static final String PERM_VIEW = "settlement.invoice.view";
    private static final String PERM_MANAGE = "settlement.invoice.manage";
    private static final String PERM_EXPORT = "settlement.invoice.export";

    private final SettlementInvoiceService service;
    private final AuthorizationService authorizationService;
    private final ExportFileService exportFiles;

    public SettlementInvoiceController(SettlementInvoiceService service, AuthorizationService authorizationService,
            ExportFileService exportFiles) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.exportFiles = exportFiles;
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

    /**
     * 第 23 条 23-R3 开票清单导出：参数与列表一致、范围同列表、分页取全；导出须单独授权（settlement.invoice.export，不预授，PGM:685、PGM:666），
     * 导一次扣一次；只交出服务端生成的带密码、不可复制不可编辑、带暗纹的 PDF（PGM:687），十四列同开票清单页面。
     */
    @GetMapping("/export")
    @ConsumesSingleUseGrant(PERM_EXPORT)
    public ResponseEntity<byte[]> export(
            @RequestParam(name = "projectId", required = false) String projectId,
            @RequestParam(name = "periodStart", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
            @RequestParam(name = "subjectEntityId", required = false) String subjectEntityId,
            @RequestParam(name = "status", required = false) String status) {
        authorizationService.requirePermission(PERM_EXPORT);
        ExportFileService.Table table = invoiceTable(blankToNull(projectId), periodStart, blankToNull(subjectEntityId), blankToNull(status));
        return exportFiles.create("settlement_invoice_items", table.title(), table.filters(), table.columns(), table.rows()).response();
    }

    @Override
    public String exportType() {
        return "settlement_invoice_items";
    }

    /** 第 23 条 23-R4 可编辑版重取：同查看开票清单（settlement.invoice.view，范围同列表），不再要导出权限。 */
    @Override
    public ExportFileService.Table viewTable(Map<String, Object> filters) {
        authorizationService.requirePermission(PERM_VIEW);
        Object period = filters.get("period_start");
        return invoiceTable((String) filters.get("project_id"), period == null ? null : LocalDate.parse(period.toString()),
                (String) filters.get("subject_entity_id"), (String) filters.get("status"));
    }

    /** 按筛选分页取全开票清单；导出记录 filters 记筛选参数与范围（全平台）。 */
    private ExportFileService.Table invoiceTable(String project, LocalDate periodStart, String subject, String state) {
        List<JdbcSettlementInvoiceRepository.SettlementInvoiceItemView> items = new ArrayList<>();
        for (int page = 1; ; page++) {
            var result = service.list(project, periodStart, subject, state, page, 200);
            items.addAll(result.items());
            if (result.items().isEmpty() || items.size() >= result.total()) {
                break;
            }
        }
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("project_id", project);
        filters.put("period_start", periodStart == null ? null : periodStart.toString());
        filters.put("subject_entity_id", subject);
        filters.put("status", state);
        filters.put("scope", "全平台");
        String month = periodStart == null ? "全部结算月" : periodStart.toString().substring(0, 7);
        return new ExportFileService.Table("开票清单-" + month, filters, EXPORT_COLUMNS,
                items.stream().map(SettlementInvoiceController::exportRow).toList());
    }

    private static final List<String> EXPORT_COLUMNS = List.of("结算月", "开票主体", "开票项目", "税目口径", "收入类型", "开票金额(元)",
            "增值税率", "税额(元)", "文化事业建设费(元)", "凭证要件", "开票状态", "发票号", "来源单号", "购方");

    /** 收入类型的说法同开票清单页面（SettlementInvoicePage.tsx）。 */
    private static String incomeLabel(String incomeType) {
        return switch (incomeType == null ? "" : incomeType) {
            case "ticket" -> "平台通票 / 门票综合服务费";
            case "spv_service" -> "通票可分配池分账";
            case "nonstd_service" -> "非散台服务费";
            case "catering" -> "订台低消 / 二次消费 / 额外酒水";
            case "rent" -> "商业租赁 / 市集摊位";
            case "game" -> "游乐收入";
            case "show" -> "演出票";
            case "giveaway_transfer" -> "通票赠送权益兑付（内部转付）";
            case "marketing" -> "会员积分 / 门票折扣券（营销成本）";
            case "points_issuance" -> "积分发行费";
            default -> incomeType;
        };
    }

    /** 开票状态的说法同开票清单页面。 */
    private static String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "pending" -> "待开票";
            case "issuing" -> "开票中";
            case "issued" -> "已开票";
            case "reversed" -> "已红冲";
            case "voided" -> "已作废";
            case "failed" -> "开票失败";
            default -> status;
        };
    }

    private static List<String> exportRow(JdbcSettlementInvoiceRepository.SettlementInvoiceItemView item) {
        return Arrays.asList(item.periodStart() == null ? "" : item.periodStart().toString(),
                item.subjectName() == null ? "（归属未配置）" : item.subjectName(), item.invoiceItemName(), item.taxCategory(),
                incomeLabel(item.incomeType()), yuan(item.amountCent()), rate(item.vatRateBps()),
                yuan(item.vatAmountCent()), item.culturalFeeApplicable() ? yuan(item.culturalFeeCent()) : "不计", item.evidenceRequired(),
                statusLabel(item.status()), item.invoiceNo(),
                item.billId() != null ? item.billId() : item.pointsBillId(), item.buyerName());
    }

    private static String yuan(long cent) {
        return BigDecimal.valueOf(cent, 2).toPlainString();
    }

    private static String rate(int bps) {
        return BigDecimal.valueOf(bps, 2).toPlainString() + "%";
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
