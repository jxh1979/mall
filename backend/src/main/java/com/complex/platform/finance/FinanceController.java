package com.complex.platform.finance;

// 文件版本：V0.07
import static com.complex.platform.common.api.ApiRequests.requireIdempotencyKey;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiResponse;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.finance.dto.ArApAdjustmentDecisionRequest;
import com.complex.platform.finance.dto.ArApAdjustmentRequest;
import com.complex.platform.finance.dto.ArApItemRequest;
import com.complex.platform.finance.dto.ArApPaymentRequest;
import com.complex.platform.finance.dto.DailyCloseActionRequest;
import com.complex.platform.finance.dto.DailyCloseRequest;
import com.complex.platform.finance.dto.DepositActionRequest;
import com.complex.platform.finance.dto.InventoryVariancePostRequest;
import com.complex.platform.finance.dto.TurnoverRentBillRequest;
import com.complex.platform.finance.dto.TurnoverRentRuleRequest;
import java.util.List;
import com.complex.platform.finance.dto.OfflinePaymentConfirmRequest;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.finance.dto.PropertyFeeBillRequest;
import com.complex.platform.finance.dto.PropertyFeeRateRequest;
import com.complex.platform.finance.dto.CloseReconciliationDifferenceRequest;
import com.complex.platform.finance.dto.IdentifyReconciliationDifferenceRequest;
import com.complex.platform.finance.dto.ReconciliationDifferenceRequest;
import com.complex.platform.finance.dto.ReconciliationStageLimitRequest;
import com.complex.platform.finance.dto.RefundRequest;
import com.complex.platform.finance.dto.RefundOperationRequest;
import com.complex.platform.finance.dto.RefundExecutionRequest;
import com.complex.platform.finance.dto.RefundResultConfirmRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finance")
public class FinanceController implements com.complex.platform.security.ExportFileService.Source {

    private final FinanceService service;
    private final AuthorizationService authorizationService;
    // AUD-014 柜台确认储值充值即入账：确认+意图 CAS+入账同事务，见 confirmOfflinePayment 路由。
    private final com.complex.platform.customer.StoredValueService storedValueService;
    // 第十八批 Y 段第 181 条 181-R1：对账差异识别与提交关闭复核（四态）。
    private final FinanceReconciliationDifferenceService reconciliationDifferences;

    public FinanceController(FinanceService service, AuthorizationService authorizationService,
                             com.complex.platform.customer.StoredValueService storedValueService,
                             FinanceReconciliationDifferenceService reconciliationDifferences) {
        this.service = service;
        this.authorizationService = authorizationService;
        this.storedValueService = storedValueService;
        this.reconciliationDifferences = reconciliationDifferences;
    }

    @GetMapping("/ar-ap-items")
    public ApiResponse<PageResponse<ArApItemRecord>> listArApItems(
            @RequestParam(name = "keyword", required = false) String keyword,
            // 第十八批 R 段 143-R4：可选只列被该日结锁定的台账。
            @RequestParam(name = "locked_day_close_id", required = false) String lockedDayCloseId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        authorizationService.requirePermission("finance.ar_ap.view");
        return ApiResponse.ok(service.listArApItems(keyword, lockedDayCloseId, page, size));
    }

    @GetMapping("/ar-ap-items/export")
    @com.complex.platform.access.ConsumesSingleUseGrant({"finance.ar_ap.export", "finance.payroll.export"}) // M723：导出导一次扣一次（操作员 2026-10-03）
    public org.springframework.http.ResponseEntity<byte[]> exportArApItems() {
        // 治理口径：资金流水属敏感信息，导出与查看分权——审计角色只直授 view，export 须走临时授权审批（V196）。
        authorizationService.requirePermission("finance.ar_ap.export");
        // 第 23 条 23-R3：只交出服务端生成的带密码、带暗纹的 PDF（PGM:687）。
        return service.exportArApItems().response();
    }

    @Override
    public String exportType() {
        return "finance_ar_ap_ledger";
    }

    /** 第 23 条 23-R4 可编辑版重取：同查看往来台账的权限与范围（finance.ar_ap.view），不再要导出权限。 */
    @Override
    public com.complex.platform.security.ExportFileService.Table viewTable(java.util.Map<String, Object> filters) {
        return service.arApViewTable();
    }

    @PostMapping("/ar-ap-items")
    public ApiResponse<ArApItemRecord> createArApItem(@RequestBody @jakarta.validation.Valid ArApItemRequest request) {
        authorizationService.requirePermission("finance.ar_ap.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createArApItem(request));
    }

    @PostMapping("/ar-ap-items/{id}/payments")
    public ApiResponse<ArApPaymentResponse> registerArApPayment(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid ArApPaymentRequest request
    ) {
        authorizationService.requirePermission("finance.payment.register");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.registerArApPayment(id, request));
    }

    @PostMapping("/daily-closes")
    public ApiResponse<DailyCloseRecord> createDailyClose(@RequestBody @jakarta.validation.Valid DailyCloseRequest request) {
        authorizationService.requirePermission("finance.daily_close.lock");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createDailyClose(request));
    }

    /** 第十八批 R 段第 143 条 143-R1（ENUM:190「待生成、待确认、有差异、已确认、已锁定、已归档」）：确认日结，与生成、锁定同一个码。 */
    @PostMapping("/daily-closes/{id}/confirm")
    public ApiResponse<DailyCloseRecord> confirmDailyClose(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid DailyCloseActionRequest request
    ) {
        authorizationService.requirePermission("finance.daily_close.lock");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.confirmDailyClose(id, request));
    }

    /** 143-R1：锁定已确认的日结（BSM:2023「日结锁定后不能直接补录影响金额，必须上级审批通过后补录。」）。 */
    @PostMapping("/daily-closes/{id}/lock")
    public ApiResponse<DailyCloseRecord> lockDailyClose(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid DailyCloseActionRequest request
    ) {
        authorizationService.requirePermission("finance.daily_close.lock");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.lockDailyClose(id, request));
    }

    /** 143-R2 五类最终标记（ENUM:190「由各域主管公司的财务岗在该营业日闭店之后完成核对并逐类标记」）：与生成、确认、锁定同一个码。 */
    @PostMapping("/daily-closes/{id}/final-marks")
    public ApiResponse<DailyCloseRecord> markDailyCloseFinal(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid DailyCloseActionRequest request
    ) {
        authorizationService.requirePermission("finance.daily_close.lock");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.markDailyCloseFinal(id, request));
    }

    /**
     * 143-R5 归档已锁定的日结（第十八批裁决 33「锁定后，由该店持日结锁定权限的财务 BP 点「归档」；归档后只读可查，锁定后补录照常走上级审批。」）：
     * 与生成、确认、锁定同一个码。
     */
    @PostMapping("/daily-closes/{id}/archive")
    public ApiResponse<DailyCloseRecord> archiveDailyClose(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid DailyCloseActionRequest request
    ) {
        authorizationService.requirePermission("finance.daily_close.lock");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.archiveDailyClose(id, request));
    }

    @GetMapping("/daily-closes/{id}/summaries")
    public ApiResponse<PageResponse<DailyCloseSummaryRecord>> listDailyCloseSummaries(@PathVariable String id) {
        authorizationService.requirePermission("finance.daily_close.manage");
        return ApiResponse.ok(service.listDailyCloseSummaries(id));
    }

    /**
     * 第十八批 R 段第 143 条 143-R4（ENUM:190「……同时在原营业日的日结锁定表单上关联显示该笔补录的金额与情况，两边互相可查。」）：
     * 按日结号查锁定后补录，与汇总查询同一个码。
     */
    @GetMapping("/daily-closes/{id}/adjustments")
    public ApiResponse<PageResponse<ArApAdjustmentRecord>> listDailyCloseAdjustments(@PathVariable String id) {
        authorizationService.requirePermission("finance.daily_close.manage");
        return ApiResponse.ok(service.listDailyCloseAdjustments(id));
    }

    /** 第十八批 P 段第 133 条 133-R2（BSM:1848「财务看退款、差异、日结和结算依据。」）：日结批次列表，与汇总查询同一个码。 */
    @GetMapping("/daily-closes")
    public ApiResponse<PageResponse<DailyCloseRecord>> listDailyCloses(
            // 第十八批 R 段 143-R1：可选按营业日与范围过滤，查不到即「待生成」。
            @RequestParam(name = "business_date", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate businessDate,
            @RequestParam(name = "scope_type", required = false) String scopeType,
            @RequestParam(name = "scope_id", required = false) String scopeId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        authorizationService.requirePermission("finance.daily_close.manage");
        return ApiResponse.ok(service.listDailyCloses(businessDate, scopeType, scopeId, page, size));
    }

    /**
     * 第十八批 Z 段第 188 条 188-R1（BSM:1803「封账由财务部门执行，条件是账目已算清楚、经确认无误、四流一致。」；第十八批裁决 41「财务中心按综合体＋月份
     * 在平台办理」）：按综合体＋月份封账。
     */
    @PostMapping("/period-closes")
    public ApiResponse<PeriodCloseRecord> closePeriod(
            @RequestBody @jakarta.validation.Valid com.complex.platform.finance.dto.PeriodCloseRequest request) {
        authorizationService.requirePermission("finance.period_close.execute");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.closePeriod(request));
    }

    /** 188-R1：已封账月份列表，与封账同一个码；可按综合体过滤。 */
    @GetMapping("/period-closes")
    public ApiResponse<PageResponse<PeriodCloseRecord>> listPeriodCloses(
            @RequestParam(name = "project_id", required = false) String projectId) {
        authorizationService.requirePermission("finance.period_close.execute");
        return ApiResponse.ok(service.listPeriodCloses(projectId));
    }

    @PostMapping("/ar-ap-adjustments")
    public ApiResponse<ArApAdjustmentRecord> requestArApAdjustment(@RequestBody @jakarta.validation.Valid ArApAdjustmentRequest request) {
        authorizationService.requirePermission("finance.locked_backfill.request");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.requestAdjustment(request));
    }

    @PostMapping("/ar-ap-adjustments/{id}/approve")
    public ApiResponse<ArApAdjustmentRecord> approveArApAdjustment(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid ArApAdjustmentDecisionRequest request
    ) {
        authorizationService.requirePermission("finance.locked_backfill.approve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.approveAdjustment(id, request));
    }

    @PostMapping("/payment-intents")
    public ApiResponse<PaymentIntentRecord> createPaymentIntent(@RequestBody @jakarta.validation.Valid PaymentIntentRequest request) {
        authorizationService.requirePermission("finance.payment_intent.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createPaymentIntent(request));
    }

    @PostMapping("/payments/{payment_id}/confirm-offline")
    public ApiResponse<OfflinePaymentConfirmationRecord> confirmOfflinePayment(
            @PathVariable("payment_id") String paymentId,
            @RequestBody @jakarta.validation.Valid OfflinePaymentConfirmRequest request
    ) {
        authorizationService.requirePermission("finance.payment.register");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        // AUD-014：储值充值意图走「确认+意图 CAS+入账」同事务链（柜台确认成功=余额到账）；其余保持原确认路径。
        boolean storedValueTopup = service.findPaymentIntent(paymentId)
                .map(intent -> intent.businessOrderType() != null
                        && intent.businessOrderType().startsWith("stored_value_topup"))
                .orElse(false);
        if (storedValueTopup) {
            return ApiResponse.ok(storedValueService.confirmOfflineTopupAndSettle(paymentId, request));
        }
        return ApiResponse.ok(service.confirmOfflinePayment(paymentId, request));
    }

    @GetMapping("/reconciliation/batches")
    public ApiResponse<PageResponse<ReconciliationBatchRecord>> listReconciliationBatches() {
        authorizationService.requirePermission("finance.reconciliation.view");
        return ApiResponse.ok(service.listReconciliationBatches());
    }

    @GetMapping("/reconciliation/differences")
    public ApiResponse<PageResponse<ReconciliationDifferenceRecord>> listReconciliationDifferences() {
        // 权限口径对齐(2026-07-18 复核裁决): 对账差异读路径=finance.reconciliation.view(与 batches/审计口径一致);
        // openplatform.reconcile.execute 保留给对账执行/补偿等写动作。
        authorizationService.requirePermission("finance.reconciliation.view");
        return ApiResponse.ok(service.listReconciliationDifferences());
    }

    @PostMapping("/reconciliation/differences")
    public ApiResponse<ReconciliationDifferenceRecord> createReconciliationDifference(
            @RequestBody @jakarta.validation.Valid ReconciliationDifferenceRequest request
    ) {
        authorizationService.requirePermission("finance.reconciliation.difference.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createReconciliationDifference(request));
    }

    @PostMapping("/reconciliation/differences/{difference_id}/close")
    public ApiResponse<ReconciliationDifferenceRecord> closeReconciliationDifference(
            @org.springframework.web.bind.annotation.PathVariable("difference_id") String differenceId,
            @RequestBody @jakarta.validation.Valid CloseReconciliationDifferenceRequest request
    ) {
        authorizationService.requirePermission("finance.reconciliation.resolve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        // 181-R1：语义改为提交关闭复核（待处理 → 待复核），复核通过才关闭。
        return ApiResponse.ok(reconciliationDifferences.submitReview(differenceId, request));
    }

    /** 第十八批 Y 段第 181 条 181-R1（BSM:1729「按原因定位、复核后处理」）：识别对账差异（待识别 → 待处理）。 */
    @PostMapping("/reconciliation/differences/{difference_id}/identify")
    public ApiResponse<ReconciliationDifferenceRecord> identifyReconciliationDifference(
            @org.springframework.web.bind.annotation.PathVariable("difference_id") String differenceId,
            @RequestBody @jakarta.validation.Valid IdentifyReconciliationDifferenceRequest request
    ) {
        authorizationService.requirePermission("finance.reconciliation.resolve");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(reconciliationDifferences.identify(differenceId, request));
    }

    /**
     * 第十八批 Y 段第 181 条 181-R2（ENUM:191「待识别与待处理两个阶段各自的停留时限为业务参数，按项目可配，出厂默认 1 个工作日，须同时配置上下限」）：
     * 查项目两阶段停留时限，未配置的按出厂默认 1 个工作日、上下限为空；权限与项目范围在服务层校验。
     */
    @GetMapping("/reconciliation/stage-limits")
    public ApiResponse<List<FinanceRepository.ReconciliationStageLimit>> listReconciliationStageLimits(
            @RequestParam(name = "project_id", required = false) String projectId) {
        return ApiResponse.ok(reconciliationDifferences.stageLimits(projectId));
    }

    /** 181-R2：配置项目某一阶段的停留时限与上下限（三值同填）；权限与项目范围在服务层校验。 */
    @PutMapping("/reconciliation/stage-limits/{project_id}")
    public ApiResponse<FinanceRepository.ReconciliationStageLimit> saveReconciliationStageLimit(
            @PathVariable("project_id") String projectId, @RequestBody ReconciliationStageLimitRequest request) {
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(reconciliationDifferences.saveStageLimit(projectId, request));
    }

    @PostMapping("/refunds")
    public ApiResponse<FinanceRefundRecord> createRefund(@RequestBody @jakarta.validation.Valid RefundRequest request) {
        authorizationService.requirePermission("finance.refund.create");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createRefund(request));
    }

    @GetMapping("/ar-ap-items/{id}/payments")
    public ApiResponse<PageResponse<ArApPaymentRecord>> listArApPayments(@PathVariable("id") String id) {
        authorizationService.requirePermission("finance.ar_ap.view");
        return ApiResponse.ok(service.listArApPayments(id));
    }

    @GetMapping("/refunds")
    public ApiResponse<PageResponse<FinanceRefundRecord>> listFinanceRefunds() {
        authorizationService.requirePermission("finance.ar_ap.view");
        return ApiResponse.ok(service.listFinanceRefunds());
    }

    @GetMapping("/refunds/{refund_id}")
    public ApiResponse<FinanceRefundDetailRecord> getFinanceRefund(@PathVariable("refund_id") String id) {
        authorizationService.requirePermission("finance.ar_ap.view");
        return ApiResponse.ok(service.getFinanceRefund(id));
    }

    @PostMapping("/refunds/{refund_id}/query")
    public ApiResponse<FinanceRefundRecord> queryFinanceRefund(@PathVariable("refund_id") String id,
            @RequestBody @jakarta.validation.Valid RefundOperationRequest request) {
        authorizationService.requirePermission("finance.ar_ap.view");
        requireIdempotencyKey(request.idempotencyKey());
        return ApiResponse.ok(service.queryFinanceRefund(id, request));
    }

    @PostMapping("/refunds/{refund_id}/retry")
    public ApiResponse<FinanceRefundRecord> retryFinanceRefund(@PathVariable("refund_id") String id,
            @RequestBody @jakarta.validation.Valid RefundOperationRequest request) {
        authorizationService.requirePermission("finance.refund.create");
        requireIdempotencyKey(request.idempotencyKey());
        return ApiResponse.ok(service.retryFinanceRefund(id, request));
    }

    @PostMapping("/refunds/{refund_id}/execution")
    public ApiResponse<FinanceRefundRecord> recordFinanceRefundExecution(@PathVariable("refund_id") String id,
            @RequestBody @jakarta.validation.Valid RefundExecutionRequest request) {
        authorizationService.requirePermission("finance.refund.create");
        requireIdempotencyKey(request.idempotencyKey());
        return ApiResponse.ok(service.recordFinanceRefundExecution(id, request));
    }

    @PostMapping("/refunds/{refund_id}/confirm")
    public ApiResponse<FinanceRefundRecord> confirmFinanceRefundResult(@PathVariable("refund_id") String id,
            @RequestBody @jakarta.validation.Valid RefundResultConfirmRequest request) {
        authorizationService.requirePermission("finance.refund.create");
        authorizationService.requirePermission("finance.payment.register");
        requireIdempotencyKey(request.idempotencyKey());
        return ApiResponse.ok(service.confirmFinanceRefundResult(id, request));
    }

    // ── P1#13 库存差异 → AR/AP ──────────────────────────────────────────────────

    @PostMapping("/inventory-variances/post")
    public ApiResponse<ArApItemRecord> postInventoryVariance(@RequestBody @jakarta.validation.Valid InventoryVariancePostRequest request) {
        authorizationService.requirePermission("finance.inventory_variance.post");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.postInventoryVariance(request));
    }

    // ── P1#14 综合体计费：营业额抽成 / 保证金 / CAM ─────────────────────────────

    @PostMapping("/turnover-rent/bills")
    public ApiResponse<ArApItemRecord> billTurnoverRent(@RequestBody @jakarta.validation.Valid TurnoverRentBillRequest request) {
        authorizationService.requirePermission("finance.turnover_rent.bill");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.billTurnoverRent(request));
    }

    @PostMapping("/turnover-rent/rules")
    public ApiResponse<TurnoverRentRuleRecord> createTurnoverRentRule(@RequestBody @jakarta.validation.Valid TurnoverRentRuleRequest request) {
        authorizationService.requirePermission("finance.turnover_rent.bill");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createTurnoverRentRule(request));
    }

    @PostMapping("/deposits/actions")
    public ApiResponse<DepositLedgerRecord> recordDepositAction(@RequestBody @jakarta.validation.Valid DepositActionRequest request) {
        authorizationService.requirePermission("finance.deposit.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.recordDepositAction(request));
    }

    // ── F-OPS-08 物业费固定计费（每平米固定单价 × 商户面积，按期不退不补）─────────────

    @PostMapping("/property-fee/rates")
    public ApiResponse<PropertyFeeRateRecord> createPropertyFeeRate(@RequestBody @jakarta.validation.Valid PropertyFeeRateRequest request) {
        authorizationService.requirePermission("finance.property_fee.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.createPropertyFeeRate(request));
    }

    @GetMapping("/property-fee/rates")
    public ApiResponse<List<PropertyFeeRateRecord>> listPropertyFeeRates(@RequestParam("project_id") String projectId) {
        authorizationService.requirePermission("finance.property_fee.manage");
        return ApiResponse.ok(service.listPropertyFeeRates(projectId));
    }

    @PostMapping("/property-fee/bills")
    public ApiResponse<List<PropertyFeeBillRecord>> billPropertyFee(@RequestBody @jakarta.validation.Valid PropertyFeeBillRequest request) {
        authorizationService.requirePermission("finance.property_fee.manage");
        requireIdempotencyKey(request == null ? null : request.idempotencyKey());
        return ApiResponse.ok(service.billPropertyFee(request));
    }
}
