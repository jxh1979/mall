package com.complex.platform.finance;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.dto.ArApAdjustmentDecisionRequest;
import com.complex.platform.finance.dto.ArApAdjustmentRequest;
import com.complex.platform.finance.dto.ArApItemRequest;
import com.complex.platform.finance.dto.ArApPaymentRequest;
import com.complex.platform.finance.dto.CamAllocationRequest;
import com.complex.platform.finance.dto.CamTrueUpRequest;
import com.complex.platform.finance.dto.DailyCloseRequest;
import com.complex.platform.finance.dto.DepositActionRequest;
import com.complex.platform.finance.dto.InventoryVariancePostRequest;
import com.complex.platform.finance.dto.TurnoverRentBillRequest;
import com.complex.platform.finance.dto.OfflinePaymentConfirmRequest;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.finance.dto.ReconciliationDifferenceRequest;
import com.complex.platform.finance.dto.RefundRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final String CURRENCY_CNY = "CNY";
    private static final Set<String> DIRECTIONS = Set.of("receivable", "payable");
    // P1#13/#14：库存差异(inventory_difference) + 综合体计费三源(turnover_rent/cam/deposit) 入账来源。
    private static final Set<String> SOURCE_TYPES = Set.of(
            "contract", "order", "refund", "manual", "settlement", "bar_bill",
            "inventory_difference", "turnover_rent", "cam", "deposit");
    // P1#13 库存差异原因（与 V108 chk_inventory_variance_reason 一致）。
    private static final Set<String> INVENTORY_VARIANCE_REASONS = Set.of("damage", "shortage", "overage", "supplier_fault");
    // P1#14 保证金四动作（与 V110 chk_deposit_ledger_action 一致）。
    private static final Set<String> DEPOSIT_ACTIONS = Set.of("collect", "refund", "deduct", "forfeit");
    // P2#④ 退款方向统一维度（与 V118 chk_finance_refunds_direction 一致）。默认 original。
    private static final Set<String> REFUND_DIRECTIONS = Set.of("original", "cash", "third_party", "mixed");
    private static final String DEFAULT_REFUND_DIRECTION = "original";
    private static final int BPS_DENOMINATOR = 10000;
    private static final Set<String> DAILY_CLOSE_SCOPES = Set.of("project", "merchant", "bar", "ticketing", "amusement");
    private static final Set<String> OFFLINE_PAYMENT_METHODS = Set.of("cash", "bank_transfer", "pos", "other");
    // R5 T7：六源（与 finance_daily_close_summaries.chk_fdcs_source 一致）。
    private static final List<String> DAILY_CLOSE_SOURCES = List.of(
            "official_ticket", "channel_ticket", "bar", "amusement", "supply_cost", "thirdparty_reflow");
    private static final Set<String> ADJUSTMENT_DECISIONS = Set.of("approve", "reject");
    // P0#7 / R6-D07：对账差异四类枚举（与 V105 chk_frd_difference_type 一致）。
    private static final Set<String> DIFFERENCE_TYPES = Set.of("amount", "count", "status", "missing");
    // 取数来源：system=平台侧系统取数；manual=第三方侧未接入手工录入进复核（与 V105 chk_frd_source 一致）。
    private static final Set<String> DIFFERENCE_SOURCES = Set.of("system", "manual");

    private final FinanceRepository repository;
    private final AuditedOperationExecutor operations;
    private final AuditEventService auditEventService;
    private final AuthorizationService authorizationService;
    private final com.complex.platform.common.BusinessDayResolver businessDayResolver;
    private final com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway;
    // 跨模块闭环②a：MDM 高风险门控（terminal 模块 public 服务）。依赖方向 finance→terminal 无环
    // （finance 已先引用 terminal.TerminalDeviceRepository 为证；terminal 不引用 finance）。
    // 可为 null=门控未接（旧构造/兼容路径）。
    private final com.complex.platform.terminal.MdmService mdmService;

    @org.springframework.beans.factory.annotation.Autowired
    public FinanceService(
            FinanceRepository repository,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            com.complex.platform.common.BusinessDayResolver businessDayResolver,
            com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway,
            com.complex.platform.terminal.MdmService mdmService
    ) {
        this.repository = repository;
        this.operations = operations;
        this.auditEventService = auditEventService;
        this.authorizationService = authorizationService;
        this.businessDayResolver = businessDayResolver;
        this.paymentGateway = paymentGateway;
        this.mdmService = mdmService;
    }

    /**
     * 向后兼容构造：未提供支付渠道路由 / MDM 门控时，使用"开关关"语义占位路由且门控未接（mdmService=null）。
     * 供既有单测构造（开关默认关，行为与生产默认一致）。
     */
    public FinanceService(
            FinanceRepository repository,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            com.complex.platform.common.BusinessDayResolver businessDayResolver,
            com.complex.platform.finance.gateway.PaymentGatewayRouter paymentGateway
    ) {
        this(repository, operations, auditEventService, authorizationService, businessDayResolver,
                paymentGateway, null);
    }

    /**
     * 向后兼容构造：未提供支付渠道路由时，使用"开关关"语义的占位路由（不调外部渠道）。
     * 供既有单测构造（开关默认关，行为与生产默认一致）。
     */
    public FinanceService(
            FinanceRepository repository,
            AuditedOperationExecutor operations,
            AuditEventService auditEventService,
            AuthorizationService authorizationService,
            com.complex.platform.common.BusinessDayResolver businessDayResolver
    ) {
        this(repository, operations, auditEventService, authorizationService, businessDayResolver,
                com.complex.platform.finance.gateway.PaymentGatewayRouter.alwaysDisabled(), null);
    }

    public PageResponse<ArApItemRecord> listArApItems() {
        return readPage(
                "finance_ar_ap.viewed",
                "finance_ar_ap.view_failed",
                "finance.ar_ap.view",
                "ar_ap_item",
                () -> {
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.isPresent()
                            ? repository.listArApItemsByMerchant(scope.get())
                            : repository.listArApItems();
                }
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.ar_ap.manage", admissionAction = "finance.locked_backfill", objectType = "ar_ap_item")
    @Transactional
    public ArApItemRecord createArApItem(ArApItemRequest request) {
        return executeWrite(
                "createArApItem",
                request == null ? null : request.idempotencyKey(),
                request,
                ArApItemRecord.class,
                "finance_ar_ap.created",
                "finance_ar_ap.create_failed",
                "finance.ar_ap.manage",
                "ar_ap_item",
                () -> createItem(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment.register")
    @Transactional
    public ArApPaymentResponse registerArApPayment(String arApItemId, ArApPaymentRequest request) {
        String itemId = requireText(arApItemId, "ar_ap_item_id");
        return executeWrite(
                "createArApPayment:" + itemId,
                request == null ? null : request.idempotencyKey(),
                request,
                ArApPaymentResponse.class,
                "finance_payment.registered",
                "finance_payment.register_failed",
                "finance.payment.register",
                "ar_ap_payment",
                () -> registerPayment(itemId, request)
        );
    }

    /**
     * R5 T7 协同：判断某经营日是否已被锁定日结纳入（status='locked'）。
     * 供其他域（如 bar 反结）在改动结账后金额前校验，锁定则须走补录审批，不得直接冲减锁定日 net。
     */
    @Transactional(readOnly = true)
    public boolean isBusinessDayLocked(LocalDate businessDate) {
        if (businessDate == null) {
            return false;
        }
        return repository.lockedDailyCloseExistsForDate(businessDate);
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.daily_close.lock")
    @Transactional
    public DailyCloseRecord createDailyClose(DailyCloseRequest request) {
        return executeWrite(
                "createDailyClose",
                request == null ? null : request.idempotencyKey(),
                request,
                DailyCloseRecord.class,
                "finance_daily_close.locked",
                "finance_daily_close.lock_failed",
                "finance.daily_close.lock",
                "finance_daily_close",
                () -> lockDailyClose(request)
        );
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment_intent.create")
    @Transactional
    public PaymentIntentRecord createPaymentIntent(PaymentIntentRequest request) {
        return executeWrite(
                "createPaymentIntent",
                request == null ? null : request.idempotencyKey(),
                request,
                PaymentIntentRecord.class,
                "payment.intent_created",
                "payment.intent_failed",
                "finance.payment_intent.create",
                "finance_payment_intent",
                () -> createIntent(request)
        );
    }

    /** 按 id 查支付意图（储值充值/会员开通：入账前校验支付意图存在且已成功）。 */
    public Optional<PaymentIntentRecord> findPaymentIntent(String paymentIntentId) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return Optional.empty();
        }
        return repository.findPaymentIntentById(paymentIntentId);
    }

    /**
     * 将支付意图标记为成功（占位代替真实支付渠道回调）。储值充值 / 付费会员开通在测试与本地联调期
     * 通过本方法把绑定的支付意图置 succeeded，再由各自服务校验后入账，保证"无支付不入账"。
     * 真实支付渠道接入后由渠道回调驱动此状态。
     */
    @Transactional
    public PaymentIntentRecord markPaymentIntentSucceeded(String paymentIntentId) {
        PaymentIntentRecord intent = repository.findPaymentIntentById(paymentIntentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "payment intent not found"));
        if ("succeeded".equals(intent.status())) {
            return intent;
        }
        if (!"created".equals(intent.status())) {
            throw ApiException.conflict("PAYMENT_INTENT_NOT_PENDING", "payment intent is not pending");
        }
        return repository.savePaymentIntent(new PaymentIntentRecord(
                intent.id(),
                intent.businessOrderType(),
                intent.businessOrderId(),
                intent.amountCent(),
                intent.currency(),
                intent.channel(),
                "succeeded",
                intent.createdAt(),
                Instant.now()));
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.payment.register", admissionAction = "finance.offline_payment_confirm", objectType = "finance_offline_payment_confirmation")
    @Transactional
    public OfflinePaymentConfirmationRecord confirmOfflinePayment(
            String paymentId,
            OfflinePaymentConfirmRequest request
    ) {
        String pathPaymentId = requireText(paymentId, "payment_id");
        return executeWrite(
                "confirmOfflinePayment:" + pathPaymentId,
                request == null ? null : request.idempotencyKey(),
                request,
                OfflinePaymentConfirmationRecord.class,
                "finance_offline_payment.confirmed",
                "finance_offline_payment.confirm_failed",
                "finance.payment.register",
                "finance_offline_payment_confirmation",
                () -> confirmOffline(pathPaymentId, request)
        );
    }

    public PageResponse<ReconciliationBatchRecord> listReconciliationBatches() {
        return readPage(
                "finance.reconciliation_batch.viewed",
                "finance.reconciliation_batch.viewed_failed",
                "finance.reconciliation.view",
                "finance_reconciliation_batch",
                () -> {
                    requirePlatformReconciliationAccess();
                    return repository.listReconciliationBatches();
                }
        );
    }

    public PageResponse<ReconciliationDifferenceRecord> listReconciliationDifferences() {
        return readPage(
                "finance_reconciliation.difference_found",
                "finance_reconciliation.view_failed",
                "openplatform.reconcile.execute",
                "finance_reconciliation_difference",
                () -> {
                    requirePlatformReconciliationAccess();
                    return repository.listReconciliationDifferences();
                }
        );
    }

    /**
     * Reconciliation batches/differences are platform/channel-level finance artifacts with no merchant
     * dimension in the data model. A merchant-scoped caller must not read them — otherwise it would see
     * every merchant's reconciliation data. Platform admins (no merchant scope) may read.
     */
    private void requirePlatformReconciliationAccess() {
        if (authorizationService.currentMerchantScope().isPresent()) {
            throw ApiException.forbidden(
                    "RECONCILIATION_PLATFORM_ONLY",
                    "reconciliation data is platform-scoped and not available to merchant-scoped users");
        }
    }

    /**
     * P0#7 / R6-D07：创建对账差异（强制化）。
     * <ul>
     *   <li>平台侧 expected 由系统取数（按 business_ref 聚合平台权威台账）——不接受请求体手填 expected。</li>
     *   <li>source=system：以系统取数 expected 比对申报 actual，写差额；无系统取数支撑则拒绝。</li>
     *   <li>source=manual：第三方侧未接入，允许录入 actual，但必须有平台侧系统取数支撑（否则纯手填→拒绝），
     *       且强制状态 needs_review（进复核）。</li>
     * </ul>
     */
    @com.complex.platform.gate.GuardedAction(capability = "finance.reconciliation.difference.create")
    @Transactional
    public ReconciliationDifferenceRecord createReconciliationDifference(ReconciliationDifferenceRequest request) {
        return executeWrite(
                "createReconciliationDifference",
                request == null ? null : request.idempotencyKey(),
                request,
                ReconciliationDifferenceRecord.class,
                "finance_reconciliation.difference_created",
                "finance_reconciliation.difference_create_failed",
                "finance.reconciliation.difference.create",
                "finance_reconciliation_difference",
                () -> createDifference(request));
    }

    private ReconciliationDifferenceRecord createDifference(ReconciliationDifferenceRequest request) {
        // 对账差异为平台级数据，商户角色不得写。
        requirePlatformReconciliationAccess();

        ReconciliationDifferenceRequest actual = requireRequest(request);
        String batchId = requireText(actual.batchId(), "batch_id");
        String differenceType = requireEnum(actual.differenceType(), DIFFERENCE_TYPES, "difference_type");
        String businessRef = requireText(actual.businessRef(), "business_ref");
        String source = requireEnum(actual.source(), DIFFERENCE_SOURCES, "source");
        long actualAmountCent = requirePositive(actual.actualAmountCent(), "actual_amount_cent");

        // 校验批次存在（差异必须挂在已建对账批次下）。
        repository.findReconciliationBatchById(batchId)
                .orElseThrow(() -> ApiException.notFound("RECONCILIATION_BATCH_NOT_FOUND", "reconciliation batch not found"));

        // 平台侧 expected 一律系统取数（不接受请求体手填）。0=无系统取数支撑。
        long expectedAmountCent = repository.aggregatePlatformExpectedForBusinessRef(businessRef);
        if (expectedAmountCent <= 0) {
            // 纯手填差异（无平台侧系统取数支撑）一律拒绝，无论 source。
            throw ApiException.badRequest("RECONCILIATION_NO_SYSTEM_EXPECTED",
                    "difference rejected: no platform-side system takings for business_ref " + businessRef
                            + "; manual-only differences are not allowed");
        }

        // source=manual（第三方侧未接入）强制进复核；source=system 仍以系统取数与申报值比对，差额为 0 不应建差异。
        long differenceAmountCent = Math.abs(expectedAmountCent - actualAmountCent);
        String status;
        if ("manual".equals(source)) {
            status = "needs_review";
        } else {
            if (differenceAmountCent == 0) {
                throw ApiException.badRequest("RECONCILIATION_NO_DIFFERENCE",
                        "expected equals actual; no reconciliation difference to record");
            }
            status = "open";
        }

        Instant now = Instant.now();
        ReconciliationDifferenceRecord saved = repository.saveReconciliationDifference(new ReconciliationDifferenceRecord(
                "recon_diff_" + slug(batchId + "_" + actual.idempotencyKey()),
                batchId,
                differenceType,
                businessRef,
                differenceAmountCent,
                expectedAmountCent,
                actualAmountCent,
                source,
                status,
                now));

        // 留痕：executeWrite 已写 finance_reconciliation.difference_created（操作级）。
        // 这里额外写一条带取数明细（expected/actual/source/status）的强制化留痕，便于审计平台侧系统取数来源，
        // 与 summarizeDailyClose 额外发 finance.daily_close_summarized 的做法一致；复用既有 recordSuccess 不改签名。
        auditEventService.recordSuccess(
                "finance_reconciliation.difference_hardened",
                "finance.reconciliation.difference.create",
                "finance_reconciliation_difference",
                saved.id(),
                null,
                actual.idempotencyKey(),
                Map.of(
                        "difference_type", differenceType,
                        "source", source,
                        "expected_amount_cent", expectedAmountCent,
                        "actual_amount_cent", actualAmountCent,
                        "status", status));
        return saved;
    }

    // ── P1#13 库存差异 → AR/AP（R6-D08）─────────────────────────────────────────

    /**
     * 库存差异经复核入账：把 review_status='approved' 的 inventory_variance 行入账成 ar_ap_items。
     * 科目方向（R6-D08）：
     * <ul>
     *   <li>报损 damage / 盘亏 shortage（variance_qty &lt; 0）→ 计损益，方向 receivable（向责任方/内部损益挂应收冲减口径）。</li>
     *   <li>盘盈 overage（variance_qty &gt; 0）→ 冲成本，方向 payable（账面成本回冲）。</li>
     *   <li>供应商责任 supplier_fault → 向供应商挂应付索赔 payable（counterparty=供应商）。</li>
     * </ul>
     * 已入账（posted_ar_ap_item_id 非空）幂等返回原台账项；未复核通过拒绝。
     */
    @com.complex.platform.gate.GuardedAction(capability = "finance.inventory_variance.post", admissionAction = "finance.inventory_variance_post", objectType = "ar_ap_item")
    @Transactional
    public ArApItemRecord postInventoryVariance(InventoryVariancePostRequest request) {
        return executeWrite(
                "postInventoryVariance",
                request == null ? null : request.idempotencyKey(),
                request,
                ArApItemRecord.class,
                "finance_inventory_variance.posted",
                "finance_inventory_variance.post_failed",
                "finance.inventory_variance.post",
                "ar_ap_item",
                () -> postInventoryVarianceInternal(request));
    }

    // ── P1#14 营业额抽成计费 ─────────────────────────────────────────────────────

    /**
     * 营业额抽成计费引擎：按抽成规则 × 回流营业额生成应收 ar_ap_items(source_type='turnover_rent')。
     * rent = clamp(turnover × rateBps / 10000, minAmountCent, maxAmountCent)；turnover 由回流营业额只读取数。
     */
    @com.complex.platform.gate.GuardedAction(capability = "finance.turnover_rent.bill", admissionAction = "finance.turnover_rent_bill", objectType = "ar_ap_item")
    @Transactional
    public ArApItemRecord billTurnoverRent(TurnoverRentBillRequest request) {
        return executeWrite(
                "billTurnoverRent",
                request == null ? null : request.idempotencyKey(),
                request,
                ArApItemRecord.class,
                "finance_turnover_rent.billed",
                "finance_turnover_rent.bill_failed",
                "finance.turnover_rent.bill",
                "ar_ap_item",
                () -> billTurnoverRentInternal(request));
    }

    // ── P1#14 保证金四动作台账 ───────────────────────────────────────────────────

    /** 保证金台账动作：collect 收取 / refund 退还 / deduct 抵扣 / forfeit 没收，维护运行余额。 */
    @com.complex.platform.gate.GuardedAction(capability = "finance.deposit.manage", admissionAction = "finance.deposit_action", objectType = "deposit_ledger")
    @Transactional
    public DepositLedgerRecord recordDepositAction(DepositActionRequest request) {
        return executeWrite(
                "recordDepositAction",
                request == null ? null : request.idempotencyKey(),
                request,
                DepositLedgerRecord.class,
                "finance_deposit.action_recorded",
                "finance_deposit.action_failed",
                "finance.deposit.manage",
                "deposit_ledger",
                () -> recordDepositActionInternal(request));
    }

    // ── P1#14 CAM 权重分摊 ───────────────────────────────────────────────────────

    /** CAM 公共区域维护费按权重分摊：逐商户生成应收 ar_ap_items(source_type='cam')，返回分摊明细。 */
    @com.complex.platform.gate.GuardedAction(capability = "finance.cam.allocate", admissionAction = "finance.cam_allocate", objectType = "cam_allocation")
    @Transactional
    public List<CamAllocationRecord> allocateCam(CamAllocationRequest request) {
        @SuppressWarnings("unchecked")
        List<CamAllocationRecord> result = executeWrite(
                "allocateCam",
                request == null ? null : request.idempotencyKey(),
                request,
                (Class<List<CamAllocationRecord>>) (Class<?>) List.class,
                "finance_cam.allocated",
                "finance_cam.allocate_failed",
                "finance.cam.allocate",
                "cam_allocation",
                () -> allocateCamInternal(request));
        return result;
    }

    // ── P2#④ CAM 年度 true-up（多退少补）─────────────────────────────────────────

    /**
     * CAM 年度 true-up：对某 settlementPeriod 已预收做对账，
     * 逐户「实际应摊 − 已预收」差额 → 少收补应收 / 多收退应付（source_type='cam'），返回批次明细。
     */
    @com.complex.platform.gate.GuardedAction(capability = "finance.cam.trueup", admissionAction = "finance.cam_trueup", objectType = "cam_trueup_run")
    @Transactional
    public CamTrueUpRunRecord runCamTrueUp(CamTrueUpRequest request) {
        return executeWrite(
                "runCamTrueUp",
                request == null ? null : request.idempotencyKey(),
                request,
                CamTrueUpRunRecord.class,
                "finance_cam.trued_up",
                "finance_cam.trueup_failed",
                "finance.cam.trueup",
                "cam_trueup_run",
                () -> runCamTrueUpInternal(request));
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.refund.create", admissionAction = "finance.refund", objectType = "finance_refund")
    @Transactional
    public FinanceRefundRecord createRefund(RefundRequest request) {
        return executeWrite(
                "createFinanceRefund",
                request == null ? null : request.idempotencyKey(),
                request,
                FinanceRefundRecord.class,
                "finance_refund.created",
                "finance_refund.create_failed",
                "finance.refund.create",
                "finance_refund",
                () -> refund(request)
        );
    }

    public PageResponse<DailyCloseSummaryRecord> listDailyCloseSummaries(String dailyCloseId) {
        String id = requireText(dailyCloseId, "daily_close_id");
        return readPage(
                "finance.daily_close_summary.viewed",
                "finance.daily_close_summary.view_failed",
                "finance.daily_close.manage",
                "finance_daily_close_summary",
                () -> {
                    DailyCloseRecord dailyClose = repository.findDailyCloseById(id)
                            .orElseThrow(() -> ApiException.notFound("DAILY_CLOSE_NOT_FOUND", "daily close not found"));
                    // 隔离：商户角色只能看自身 scope 的日结汇总。
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    if (scope.isPresent()
                            && "merchant".equals(dailyClose.scopeType())
                            && !scope.get().equals(dailyClose.scopeId())) {
                        throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                                "access to another merchant's daily close is not allowed");
                    }
                    return repository.listDailyCloseSummaries(id);
                });
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.locked_backfill.request")
    @Transactional
    public ArApAdjustmentRecord requestAdjustment(ArApAdjustmentRequest request) {
        return executeWrite(
                "requestArApAdjustment",
                request == null ? null : request.idempotencyKey(),
                request,
                ArApAdjustmentRecord.class,
                "finance.locked_backfill_requested",
                "finance.locked_backfill_request_failed",
                "finance.locked_backfill.request",
                "ar_ap_adjustment",
                () -> createAdjustment(request));
    }

    @com.complex.platform.gate.GuardedAction(capability = "finance.locked_backfill.approve", admissionAction = "finance.locked_backfill_approve", objectType = "ar_ap_adjustment")
    @Transactional
    public ArApAdjustmentRecord approveAdjustment(String adjustmentId, ArApAdjustmentDecisionRequest request) {
        String id = requireText(adjustmentId, "adjustment_id");
        String decision = requireEnum(request == null ? null : request.decision(), ADJUSTMENT_DECISIONS, "decision");
        boolean approve = "approve".equals(decision);
        return executeWrite(
                "decideArApAdjustment:" + id,
                request == null ? null : request.idempotencyKey(),
                request,
                ArApAdjustmentRecord.class,
                approve ? "finance.locked_backfill_approved" : "finance.locked_backfill_rejected",
                "finance.locked_backfill_decision_failed",
                "finance.locked_backfill.approve",
                "ar_ap_adjustment",
                () -> decideAdjustment(id, approve));
    }

    private ArApAdjustmentRecord createAdjustment(ArApAdjustmentRequest request) {
        ArApAdjustmentRequest actual = requireRequest(request);
        String itemId = requireText(actual.arApItemId(), "ar_ap_item_id");
        long amountCent = requireNonZero(actual.adjustmentAmountCent(), "adjustment_amount_cent");
        String reason = requireText(actual.reason(), "reason");

        ArApItemRecord item = repository.findArApItemById(itemId)
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "AR/AP item not found"));
        checkMerchantOwnership(item.merchantId());

        // 补录只针对已锁定台账（未锁定应直接改账，不走审批）。
        String lockedDayCloseId = repository.findArApItemLockedDayCloseId(itemId)
                .orElseThrow(() -> ApiException.badRequest("AR_AP_ITEM_NOT_LOCKED",
                        "AR/AP item is not locked; modify it directly instead of requesting a backfill"));

        Instant now = Instant.now();
        return repository.saveArApAdjustment(new ArApAdjustmentRecord(
                "arap_adj_" + slug(itemId + "_" + actual.idempotencyKey()),
                itemId,
                lockedDayCloseId,
                amountCent,
                "pending",
                authorizationService.currentActor().id(),
                null,
                reason,
                now,
                null));
    }

    private ArApAdjustmentRecord decideAdjustment(String adjustmentId, boolean approve) {
        ArApAdjustmentRecord adjustment = repository.findArApAdjustmentByIdForUpdate(adjustmentId)
                .orElseThrow(() -> ApiException.notFound("ADJUSTMENT_NOT_FOUND", "adjustment not found"));
        if (!"pending".equals(adjustment.approvalStatus())) {
            throw ApiException.conflict("ADJUSTMENT_ALREADY_DECIDED",
                    "adjustment is already " + adjustment.approvalStatus());
        }

        ArApItemRecord item = repository.findArApItemByIdForUpdate(adjustment.arApItemId())
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "AR/AP item not found"));
        checkMerchantOwnership(item.merchantId());

        Instant now = Instant.now();
        String approver = authorizationService.currentActor().id();

        if (approve) {
            // 审批通过：真正写入金额变动（带符号），同步刷新 paid/unpaid/status，并留痕。
            long newAmountCent = item.amountCent() + adjustment.adjustmentAmountCent();
            if (newAmountCent < 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "adjustment would drive amount negative");
            }
            long unpaidAmountCent = newAmountCent - item.paidAmountCent();
            if (unpaidAmountCent < 0) {
                throw ApiException.badRequest("VALIDATION_FAILED", "adjustment would drive unpaid amount negative");
            }
            String status = unpaidAmountCent == 0
                    ? "settled"
                    : ("receivable".equals(item.direction()) ? "pending_collection" : "pending_payment");
            repository.updateArApItemLedger(new ArApItemRecord(
                    item.id(),
                    item.itemNo(),
                    item.direction(),
                    item.counterpartyId(),
                    item.sourceType(),
                    item.sourceId(),
                    newAmountCent,
                    item.paidAmountCent(),
                    unpaidAmountCent,
                    item.currency(),
                    item.dueDate(),
                    item.accountingPeriod(),
                    status,
                    item.createdAt(),
                    now,
                    item.merchantId()));
        }
        // reject：不改账，只标状态。
        return repository.updateArApAdjustment(new ArApAdjustmentRecord(
                adjustment.id(),
                adjustment.arApItemId(),
                adjustment.lockedDayCloseId(),
                adjustment.adjustmentAmountCent(),
                approve ? "approved" : "rejected",
                adjustment.requestedBy(),
                approver,
                adjustment.reason(),
                adjustment.createdAt(),
                now));
    }

    private void requireNotLocked(String arApItemId) {
        if (repository.findArApItemLockedDayCloseId(arApItemId).isPresent()) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "AR/AP item is locked by a daily close; use the backfill adjustment workflow");
        }
    }

    private long requireNonZero(Long value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        if (value == 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be non-zero");
        }
        return value;
    }

    private ArApItemRecord createItem(ArApItemRequest request) {
        ArApItemRequest actual = requireRequest(request);
        String direction = requireEnum(actual.direction(), DIRECTIONS, "direction");
        String sourceType = optionalEnum(actual.sourceType(), SOURCE_TYPES, "source_type");
        long amountCent = requirePositive(actual.amountCent(), "amount_cent");
        LocalDate dueDate = requireDate(actual.dueDate(), "due_date");
        // R5 T7 硬拦截：禁止向已锁定经营日新增/回填台账，必须走补录审批。
        if (repository.lockedDailyCloseExistsForDate(dueDate)) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "business date " + dueDate + " is locked; use the backfill adjustment workflow");
        }
        Instant now = Instant.now();
        String slug = slug(actual.idempotencyKey());
        String id = "arap_" + slug;
        Optional<String> scope = authorizationService.currentMerchantScope();
        ArApItemRecord item = new ArApItemRecord(
                id,
                "ARAP" + now.toEpochMilli() + "-" + slug.substring(0, Math.min(12, slug.length())),
                direction,
                requireText(actual.counterpartyId(), "counterparty_id"),
                sourceType,
                trimToNull(actual.sourceId()),
                amountCent,
                0L,
                amountCent,
                CURRENCY_CNY,
                dueDate,
                trimToNull(actual.accountingPeriod()),
                "receivable".equals(direction) ? "pending_collection" : "pending_payment",
                now,
                now,
                scope.orElse(null)
        );
        ArApItemRecord saved = repository.saveArApItem(item);
        if (saved.sourceId() != null && saved.sourceType() != null) {
            repository.saveArApItemSource(new ArApItemSourceRecord(
                    "arap_src_" + slug(actual.idempotencyKey()),
                    saved.id(),
                    saved.sourceType(),
                    saved.sourceId(),
                    saved.amountCent(),
                    now
            ));
        }
        return saved;
    }

    private ArApPaymentResponse registerPayment(String arApItemId, ArApPaymentRequest request) {
        ArApPaymentRequest actual = requireRequest(request);
        long amountCent = requirePositive(actual.amountCent(), "amount_cent");
        // 跨模块闭环②a：登记收付款=资金动作高风险，接入 MDM 门控。
        // 现场/工作机收款时请求携带 deviceId → 触发硬门控；后台台账类登记无设备上下文 → 留空安全跳过。
        requireManagedDeviceForFinanceAction(actual.deviceId(), "finance.payment");

        // P0 fix: SELECT FOR UPDATE prevents concurrent reads from both seeing unpaid > 0
        // and both writing a payment, which would silently double-charge the ledger.
        ArApItemRecord current = repository.findArApItemByIdForUpdate(arApItemId)
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "AR/AP item not found"));

        // P1 fix: reject cross-merchant writes
        checkMerchantOwnership(current.merchantId());

        // R5 T7 硬拦截：已锁定台账禁止直接登记收付款，必须走补录审批。
        requireNotLocked(current.id());

        if (amountCent > current.unpaidAmountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payment amount exceeds unpaid amount");
        }
        Instant now = Instant.now();
        // P0 fix: include arApItemId in the slug so the same idempotency key used on two
        // different items produces two different payment IDs, preventing ON CONFLICT overwrite.
        ArApPaymentRecord payment = repository.saveArApPayment(new ArApPaymentRecord(
                "arap_pay_" + slug(arApItemId + "_" + actual.idempotencyKey()),
                current.id(),
                amountCent,
                requireText(actual.paymentMethod(), "payment_method"),
                actual.paymentTime() == null ? now : actual.paymentTime(),
                trimToNull(actual.bankFlowRef()),
                now
        ));
        long paidAmountCent = current.paidAmountCent() + amountCent;
        long unpaidAmountCent = current.amountCent() - paidAmountCent;
        String status = unpaidAmountCent == 0 ? "settled" : "partial_paid";
        ArApItemRecord updated = repository.updateArApItemLedger(new ArApItemRecord(
                current.id(),
                current.itemNo(),
                current.direction(),
                current.counterpartyId(),
                current.sourceType(),
                current.sourceId(),
                current.amountCent(),
                paidAmountCent,
                unpaidAmountCent,
                current.currency(),
                current.dueDate(),
                current.accountingPeriod(),
                status,
                current.createdAt(),
                now,
                current.merchantId()
        ));
        return new ArApPaymentResponse(payment, updated);
    }

    private DailyCloseRecord lockDailyClose(DailyCloseRequest request) {
        DailyCloseRequest actual = requireRequest(request);
        LocalDate businessDate = requireDate(actual.businessDate(), "business_date");
        String scopeType = requireEnum(actual.scopeType(), DAILY_CLOSE_SCOPES, "scope_type");
        String scopeId = requireText(actual.scopeId(), "scope_id");

        // P1 fix: prevent duplicate locks for the same date + scope using different keys
        if (repository.dailyCloseExists(businessDate, scopeType, scopeId)) {
            throw ApiException.conflict("DAILY_CLOSE_EXISTS",
                    "daily close already locked for date " + businessDate + " scope " + scopeType + "/" + scopeId);
        }
        Instant now = Instant.now();
        String dailyCloseId = "daily_close_" + slug(actual.idempotencyKey());
        DailyCloseRecord saved = repository.saveDailyClose(new DailyCloseRecord(
                dailyCloseId,
                businessDate,
                scopeType,
                scopeId,
                actual.forceClose() != null && actual.forceClose(),
                "locked",
                now,
                now
        ));

        // R5 T7：六源汇总写入 finance_daily_close_summaries（口径见 r5-aggregation-metrics-draft §1.2，待财务复核）。
        summarizeDailyClose(dailyCloseId, businessDate, now);

        // R5 T7：把当日属于本 scope 的 ar_ap_items 打上 locked_day_close_id（merchant 角色仅锁本商户行）。
        Optional<String> merchantScope = authorizationService.currentMerchantScope();
        repository.lockArApItemsForDailyClose(dailyCloseId, businessDate, merchantScope.orElse(null));

        return saved;
    }

    /**
     * 六源汇总聚合（official_ticket / channel_ticket / bar / amusement / supply_cost / thirdparty_reflow），
     * 每源一行写入 finance_daily_close_summaries。聚合公式按聚合草案保守取值，口径未定处见草案 §四"待财务复核"。
     */
    private void summarizeDailyClose(String dailyCloseId, LocalDate businessDate, Instant now) {
        // 经营日窗口按可配日切（默认 04:00）派生，时间戳类源表统一用 [from, to) 分桶；official_ticket 以 visit_date 为经营日。
        Instant from = businessDayResolver.startInstant(businessDate);
        Instant to = businessDayResolver.endInstant(businessDate);
        for (String source : DAILY_CLOSE_SOURCES) {
            FinanceRepository.DailyCloseSourceTotals totals = switch (source) {
                case "official_ticket" -> repository.aggregateOfficialTicket(businessDate, from, to);
                case "channel_ticket" -> repository.aggregateChannelTicket(businessDate, from, to);
                case "bar" -> repository.aggregateBar(businessDate, from, to);
                case "amusement" -> repository.aggregateAmusement(businessDate, from, to);
                case "supply_cost" -> repository.aggregateSupplyCost(businessDate, from, to);
                case "thirdparty_reflow" -> repository.aggregateThirdpartyReflow(businessDate, from, to);
                default -> FinanceRepository.DailyCloseSourceTotals.empty();
            };
            repository.saveDailyCloseSummary(new DailyCloseSummaryRecord(
                    "fdcs_" + slug(dailyCloseId + "_" + source),
                    dailyCloseId,
                    source,
                    totals.amountCent(),
                    totals.refundCent(),
                    totals.orderCount(),
                    now
            ));
        }
        auditEventService.recordSuccess(
                "finance.daily_close_summarized",
                "finance.daily_close.manage",
                "finance_daily_close_summary",
                dailyCloseId,
                null,
                null,
                Map.of("source_count", DAILY_CLOSE_SOURCES.size()));
    }

    private PaymentIntentRecord createIntent(PaymentIntentRequest request) {
        PaymentIntentRequest actual = requireRequest(request);
        // 跨模块闭环②a：支付意图创建=支付高风险动作，接入 MDM 门控。
        // 现场/工作机发起时请求携带 deviceId → 触发硬门控；后台/服务端无设备上下文 → 留空安全跳过。
        requireManagedDeviceForFinanceAction(actual.deviceId(), "finance.payment");
        Instant now = Instant.now();
        String businessOrderType = requireText(actual.businessOrderType(), "business_order_type");
        String businessOrderId = requireText(actual.businessOrderId(), "business_order_id");
        long amountCent = requirePositive(actual.amountCent(), "amount_cent");
        String currency = trimToDefault(actual.currency(), CURRENCY_CNY);
        String channel = requireText(actual.channel(), "channel");

        // 支付渠道开关（payment.gateway.enabled，后台可配，默认关）。
        //   关 → 不调外部渠道，支付意图标 reserved（占位，由 markPaymentIntentSucceeded 内部确认，不阻塞）。
        //   开 → 路由到 PaymentGatewayPort（当前无真实厂商=占位 NOT_CONFIGURED，留接缝，不崩）。
        com.complex.platform.finance.gateway.PaymentGatewayResult gw = paymentGateway.initiatePayment(
                com.complex.platform.finance.gateway.PaymentGatewayRequest.of(
                        businessOrderType, businessOrderId, amountCent, channel));

        return repository.savePaymentIntent(new PaymentIntentRecord(
                "pay_intent_" + slug(actual.idempotencyKey()),
                businessOrderType,
                businessOrderId,
                amountCent,
                currency,
                channel,
                "created",
                gw.gatewayProvider(),
                gw.gatewayRef(),
                gw.status().name(),
                now,
                now
        ));
    }

    private OfflinePaymentConfirmationRecord confirmOffline(
            String pathPaymentId,
            OfflinePaymentConfirmRequest request
    ) {
        OfflinePaymentConfirmRequest actual = requireRequest(request);
        String bodyPaymentId = requireText(actual.paymentId(), "payment_id");
        if (!pathPaymentId.equals(bodyPaymentId)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payment_id must match path");
        }
        Instant now = Instant.now();
        return repository.saveOfflinePaymentConfirmation(new OfflinePaymentConfirmationRecord(
                "offline_confirm_" + slug(actual.idempotencyKey()),
                pathPaymentId,
                requirePositive(actual.amountCent(), "amount_cent"),
                requireEnum(actual.paymentMethod(), OFFLINE_PAYMENT_METHODS, "payment_method"),
                actual.confirmedAt() == null ? now : actual.confirmedAt(),
                actual.evidenceAttachmentIds() == null ? List.of() : List.copyOf(actual.evidenceAttachmentIds()),
                "confirmed",
                now
        ));
    }

    /**
     * 跨模块闭环②a：finance 高风险动作（支付意图/退款/高价值兑付）MDM 门控接入点。
     *
     * <p><b>门控语义（device 存在则强制、为空则安全跳过）</b>：相关请求 DTO
     * （{@link RefundRequest}/{@link PaymentIntentRequest}/{@link ArApPaymentRequest}）已携带可空
     * 操作设备标识 {@code deviceId}。按"device_id 存在则门控、不存在则安全跳过"接入（不伪造假值）：
     * <ul>
     *   <li><b>前台现场场景</b>（工作机/现场设备发起退款/收款）请求携带真实 deviceId →
     *       <b>真实启用</b>硬门控：MdmService 开关开 + 设备未纳管/blocked/lost → 硬拒绝；</li>
     *   <li><b>后台台账类场景</b>（服务端/后台财务操作）无设备上下文，deviceId 为 null/空 →
     *       跳过门控，不阻断财务主链（这些操作本就无现场设备，跳过是正确语义而非占位）。</li>
     * </ul>
     * mdmService 为 null（旧兼容构造/未接门控路径）时整体跳过。
     */
    private void requireManagedDeviceForFinanceAction(String deviceId, String actionType) {
        if (mdmService == null) {
            return;
        }
        String id = trimToNull(deviceId);
        if (id == null) {
            // 后台台账类操作无现场设备上下文，安全跳过（不伪造假值）。
            return;
        }
        mdmService.requireManagedForHighRiskAction(id, actionType);
    }

    private FinanceRefundRecord refund(RefundRequest request) {
        RefundRequest actual = requireRequest(request);
        String targetId = requireText(actual.targetId(), "target_id");
        long refundAmountCent = requirePositive(actual.refundAmountCent(), "refund_amount_cent");
        // 跨模块闭环②a：退款=高价值兑付高风险动作，接入 MDM 门控。
        // 现场/工作机退款时请求携带 deviceId → 触发硬门控；后台台账类退款无设备上下文 → 留空安全跳过。
        requireManagedDeviceForFinanceAction(actual.deviceId(), "finance.refund");

        // P1 fix: validate target payment exists.
        // P1 修复（TOCTOU）：对 target payment 行加 FOR UPDATE 行锁后再做累计校验，使并发退款串行化，
        // 避免多笔退款各读旧的 sumRefundedAmount 合计后均通过校验、最终退款合计超过 payment 金额。
        ArApPaymentRecord payment = repository.findArApPaymentByIdForUpdate(targetId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_NOT_FOUND", "payment not found: " + targetId));

        // P1 fix: reject cross-merchant refund attempts
        ArApItemRecord item = repository.findArApItemById(payment.arApItemId())
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "AR/AP item not found"));
        checkMerchantOwnership(item.merchantId());

        // R5 T7 硬拦截：已锁定台账禁止直接退款（影响金额），必须走补录审批。
        requireNotLocked(item.id());

        // P1 fix: prevent refunding more than the original payment (cumulative check)
        long alreadyRefunded = repository.sumRefundedAmountByTargetId(targetId);
        if (alreadyRefunded + refundAmountCent > payment.amountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "refund amount exceeds refundable balance (payment: " + payment.amountCent()
                            + ", already refunded: " + alreadyRefunded + ")");
        }

        // 支付渠道开关（payment.gateway.enabled，后台可配，默认关）决定退款打款路径：
        //   关 → 不调渠道，退款标 requested + 内部/手工打款渠道（reserved，人工确认，不阻塞）。
        //   开 → 路由到 PaymentGatewayPort.refund（当前无真实厂商=占位 NOT_CONFIGURED，留接缝，不崩）。
        com.complex.platform.finance.gateway.PaymentGatewayResult gw = paymentGateway.refund(
                new com.complex.platform.finance.gateway.PaymentGatewayRequest(
                        "finance_refund", targetId, refundAmountCent, CURRENCY_CNY,
                        trimToNull(actual.refundChannel()), null));
        String refundChannel = gw.requiresInternalFallback()
                ? trimToDefault(actual.refundChannel(), "manual")
                : trimToDefault(actual.refundChannel(), gw.gatewayProvider());

        // P2#④ 统一退款方向维度：可空 → 默认 original（原路退回）；非空必须落在枚举内。
        String refundDirection = optionalEnum(actual.refundDirection(), REFUND_DIRECTIONS, "refund_direction");
        if (refundDirection == null) {
            refundDirection = DEFAULT_REFUND_DIRECTION;
        }

        return repository.saveRefund(new FinanceRefundRecord(
                "refund_" + slug(actual.idempotencyKey()),
                targetId,
                refundAmountCent,
                requireText(actual.reason(), "reason"),
                refundChannel,
                refundDirection,
                "requested",
                Instant.now()
        ));
    }

    // ── P1#13/#14 内部实现 ──────────────────────────────────────────────────────

    private ArApItemRecord postInventoryVarianceInternal(InventoryVariancePostRequest request) {
        InventoryVariancePostRequest actual = requireRequest(request);
        String varianceId = requireText(actual.inventoryVarianceId(), "inventory_variance_id");
        String counterpartyId = requireText(actual.counterpartyId(), "counterparty_id");

        InventoryVarianceRecord variance = repository.findInventoryVarianceById(varianceId)
                .orElseThrow(() -> ApiException.notFound("INVENTORY_VARIANCE_NOT_FOUND", "inventory variance not found"));
        checkMerchantOwnership(variance.merchantId());

        // 幂等：已入账直接返回原台账项，不重复生成。
        if (variance.postedArApItemId() != null) {
            return repository.findArApItemById(variance.postedArApItemId())
                    .orElseThrow(() -> ApiException.conflict("INVENTORY_VARIANCE_ALREADY_POSTED",
                            "inventory variance already posted but ledger item missing"));
        }
        // 只有复核通过的差异才能入账。
        if (!"approved".equals(variance.reviewStatus())) {
            throw ApiException.badRequest("INVENTORY_VARIANCE_NOT_APPROVED",
                    "inventory variance must be reviewed (approved) before posting; current=" + variance.reviewStatus());
        }
        if (!INVENTORY_VARIANCE_REASONS.contains(variance.reason())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "inventory variance reason is invalid");
        }
        // R6-D08 科目方向：
        //   supplier_fault → 向供应商挂应付索赔 payable；
        //   overage（盘盈，冲成本）→ payable（账面成本回冲）；
        //   damage/shortage（报损/盘亏，计损益）→ receivable。
        String direction;
        if ("supplier_fault".equals(variance.reason()) || "overage".equals(variance.reason())) {
            direction = "payable";
        } else {
            direction = "receivable";
        }
        long amountCent = Math.abs(variance.varianceAmountCent());
        if (amountCent <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "inventory variance amount must be non-zero to post");
        }

        ArApItemRecord saved = persistDerivedArApItem(
                direction,
                counterpartyId,
                "inventory_difference",
                variance.id(),
                amountCent,
                actual.idempotencyKey(),
                variance.merchantId());
        // P1 修复：条件回填（仅当 posted_ar_ap_item_id IS NULL 才 UPDATE）。并发双开时后到者命中 0 行返回 false，
        // 说明该 variance 已被他人入账，抛冲突，避免重复挂应付台账（前面 persistDerivedArApItem 凭 idempotencyKey 幂等不重复落项）。
        boolean posted = repository.markInventoryVariancePostedIfUnposted(variance.id(), saved.id());
        if (!posted) {
            throw ApiException.conflict("INVENTORY_VARIANCE_ALREADY_POSTED",
                    "inventory variance already posted by a concurrent request");
        }
        return saved;
    }

    private ArApItemRecord billTurnoverRentInternal(TurnoverRentBillRequest request) {
        TurnoverRentBillRequest actual = requireRequest(request);
        String ruleId = requireText(actual.ruleId(), "rule_id");
        LocalDate periodStart = requireDate(actual.periodStart(), "period_start");
        LocalDate periodEnd = requireDate(actual.periodEnd(), "period_end");
        LocalDate dueDate = requireDate(actual.dueDate(), "due_date");
        if (periodEnd.isBefore(periodStart)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period_end must not be before period_start");
        }

        TurnoverRentRuleRecord rule = repository.findTurnoverRentRuleById(ruleId)
                .orElseThrow(() -> ApiException.notFound("TURNOVER_RENT_RULE_NOT_FOUND", "turnover rent rule not found"));
        checkMerchantOwnership(rule.merchantId());
        if (!"active".equals(rule.status())) {
            throw ApiException.badRequest("TURNOVER_RENT_RULE_INACTIVE", "turnover rent rule is not active");
        }

        // 回流营业额只读取数：[periodStart 00:00 经营日起, periodEnd+1 经营日起) 半开窗口。
        Instant from = businessDayResolver.startInstant(periodStart);
        Instant to = businessDayResolver.startInstant(periodEnd.plusDays(1));
        long turnover = repository.aggregateMerchantReflowTurnover(rule.merchantId(), from, to);

        long rent = clampRent(turnover, rule.rateBps(), rule.minAmountCent(), rule.maxAmountCent());
        if (rent <= 0) {
            throw ApiException.badRequest("TURNOVER_RENT_ZERO",
                    "computed turnover rent is zero (turnover=" + turnover + "); nothing to bill");
        }
        // 营业额抽成应收：counterparty=商户。
        return persistDerivedArApItem(
                "receivable",
                rule.merchantId(),
                "turnover_rent",
                rule.id(),
                rent,
                actual.idempotencyKey(),
                rule.merchantId());
    }

    /** rent = clamp(turnover × rateBps / 10000, min, max)。min/max 为 null 时不夹。 */
    private long clampRent(long turnover, int rateBps, Long minAmountCent, Long maxAmountCent) {
        long rent = Math.floorDiv(turnover * (long) rateBps, BPS_DENOMINATOR);
        if (minAmountCent != null && rent < minAmountCent) {
            rent = minAmountCent;
        }
        if (maxAmountCent != null && rent > maxAmountCent) {
            rent = maxAmountCent;
        }
        return rent;
    }

    private DepositLedgerRecord recordDepositActionInternal(DepositActionRequest request) {
        DepositActionRequest actual = requireRequest(request);
        String merchantId = requireText(actual.merchantId(), "merchant_id");
        checkMerchantOwnership(merchantId);
        String action = requireEnum(actual.action(), DEPOSIT_ACTIONS, "action");
        long amountCent = requirePositive(actual.amountCent(), "amount_cent");

        // P1 修复：余额读取改对该商户 deposit_ledger 最新行加 FOR UPDATE 行锁，使并发 collect/deduct 串行化，
        // 在锁内读取权威余额并完成超额校验，避免读旧余额各自 append 导致 balance_after 互覆盖丢更新、超额校验被绕过。
        long balance = repository.currentDepositBalanceForUpdate(merchantId);
        long newBalance;
        if ("collect".equals(action)) {
            newBalance = balance + amountCent;
        } else {
            // refund / deduct / forfeit 均减少余额，不得超额（不可为负）。
            if (amountCent > balance) {
                throw ApiException.badRequest("DEPOSIT_INSUFFICIENT",
                        "deposit action " + action + " amount " + amountCent + " exceeds balance " + balance);
            }
            newBalance = balance - amountCent;
        }

        // deduct/forfeit 联动生成应收（向商户索赔/没收入账）；collect/refund 不生成台账项。
        String arApItemId = null;
        if ("deduct".equals(action) || "forfeit".equals(action)) {
            ArApItemRecord item = persistDerivedArApItem(
                    "receivable",
                    merchantId,
                    "deposit",
                    "deposit_" + action + "_" + slug(actual.idempotencyKey()),
                    amountCent,
                    actual.idempotencyKey(),
                    merchantId);
            arApItemId = item.id();
        }

        Instant now = Instant.now();
        return repository.saveDepositLedger(new DepositLedgerRecord(
                "deposit_" + slug(merchantId + "_" + actual.idempotencyKey()),
                merchantId,
                action,
                amountCent,
                newBalance,
                arApItemId,
                trimToNull(actual.reason()),
                now));
    }

    private List<CamAllocationRecord> allocateCamInternal(CamAllocationRequest request) {
        CamAllocationRequest actual = requireRequest(request);
        String period = requireText(actual.period(), "period");
        long totalAmountCent = requirePositive(actual.totalAmountCent(), "total_amount_cent");
        LocalDate dueDate = requireDate(actual.dueDate(), "due_date");
        List<CamAllocationRequest.CamShare> shares = actual.shares();
        if (shares == null || shares.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "shares is required");
        }
        // 权重合计须为 10000（逐分守恒，避免分摊不足/超额）。
        long totalWeight = 0;
        for (CamAllocationRequest.CamShare share : shares) {
            if (share.weightBps() == null || share.weightBps() < 0 || share.weightBps() > BPS_DENOMINATOR) {
                throw ApiException.badRequest("VALIDATION_FAILED", "weight_bps must be within [0, 10000]");
            }
            requireText(share.merchantId(), "merchant_id");
            totalWeight += share.weightBps();
        }
        if (totalWeight != BPS_DENOMINATOR) {
            throw ApiException.badRequest("CAM_WEIGHTS_NOT_FULL",
                    "share weights must sum to 10000 bps; got " + totalWeight);
        }

        Instant now = Instant.now();
        String batchId = "cam_batch_" + slug(actual.idempotencyKey());
        List<CamAllocationRecord> result = new java.util.ArrayList<>();
        long allocatedSoFar = 0;
        for (int i = 0; i < shares.size(); i++) {
            CamAllocationRequest.CamShare share = shares.get(i);
            long allocated;
            if (i == shares.size() - 1) {
                // 末位归入剩余差额，保证逐分守恒。
                allocated = totalAmountCent - allocatedSoFar;
            } else {
                allocated = Math.floorDiv(totalAmountCent * (long) share.weightBps(), BPS_DENOMINATOR);
            }
            allocatedSoFar += allocated;

            String arApItemId = null;
            if (allocated > 0) {
                ArApItemRecord item = persistDerivedArApItem(
                        "receivable",
                        share.merchantId(),
                        "cam",
                        batchId + "_" + share.merchantId(),
                        allocated,
                        actual.idempotencyKey() + "_" + i,
                        share.merchantId());
                arApItemId = item.id();
            }
            result.add(repository.saveCamAllocation(new CamAllocationRecord(
                    "cam_alloc_" + slug(batchId + "_" + share.merchantId()),
                    batchId,
                    share.merchantId(),
                    period,
                    totalAmountCent,
                    share.weightBps(),
                    allocated,
                    arApItemId,
                    now)));
        }
        return result;
    }

    private CamTrueUpRunRecord runCamTrueUpInternal(CamTrueUpRequest request) {
        CamTrueUpRequest actual = requireRequest(request);
        String settlementPeriod = requireText(actual.settlementPeriod(), "settlement_period");
        long actualTotalAmountCent = requirePositive(actual.actualTotalAmountCent(), "actual_total_amount_cent");
        requireDate(actual.dueDate(), "due_date");
        List<CamTrueUpRequest.TrueUpShare> shares = actual.shares();
        if (shares == null || shares.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "shares is required");
        }
        // 权重合计须为 10000（与 CAM 预收权重口径一致，逐分守恒）。
        long totalWeight = 0;
        for (CamTrueUpRequest.TrueUpShare share : shares) {
            if (share.weightBps() == null || share.weightBps() < 0 || share.weightBps() > BPS_DENOMINATOR) {
                throw ApiException.badRequest("VALIDATION_FAILED", "weight_bps must be within [0, 10000]");
            }
            requireText(share.merchantId(), "merchant_id");
            totalWeight += share.weightBps();
        }
        if (totalWeight != BPS_DENOMINATOR) {
            throw ApiException.badRequest("CAM_WEIGHTS_NOT_FULL",
                    "share weights must sum to 10000 bps; got " + totalWeight);
        }

        Instant now = Instant.now();
        String runId = "cam_trueup_" + slug(settlementPeriod + "_" + actual.idempotencyKey());
        List<CamTrueUpLineRecord> lines = new java.util.ArrayList<>();
        long actualAllocatedSoFar = 0;
        long prepaidTotal = 0;
        for (int i = 0; i < shares.size(); i++) {
            CamTrueUpRequest.TrueUpShare share = shares.get(i);
            checkMerchantOwnership(share.merchantId());

            long actualAllocated;
            if (i == shares.size() - 1) {
                // 末位归差额，保证实际应摊逐分守恒至 actualTotalAmountCent。
                actualAllocated = actualTotalAmountCent - actualAllocatedSoFar;
            } else {
                actualAllocated = Math.floorDiv(actualTotalAmountCent * (long) share.weightBps(), BPS_DENOMINATOR);
            }
            actualAllocatedSoFar += actualAllocated;

            // 该户该归属期已预收合计（来自 cam_allocations，按 settlement_period 归集）。
            long prepaid = repository.sumCamPrepaidByMerchantAndPeriod(share.merchantId(), settlementPeriod);
            prepaidTotal += prepaid;
            long diff = actualAllocated - prepaid;

            String arApItemId = null;
            if (diff != 0) {
                // diff>0 少收 → 补收应收；diff<0 多收 → 退还应付。金额取绝对值，方向区分多退少补。
                String direction = diff > 0 ? "receivable" : "payable";
                ArApItemRecord item = persistDerivedArApItem(
                        direction,
                        share.merchantId(),
                        "cam",
                        "cam_trueup_" + settlementPeriod + "_" + share.merchantId(),
                        Math.abs(diff),
                        actual.idempotencyKey() + "_trueup_" + i,
                        share.merchantId());
                arApItemId = item.id();
            }

            CamTrueUpLineRecord line = repository.saveCamTrueUpLine(new CamTrueUpLineRecord(
                    "cam_trueup_line_" + slug(runId + "_" + share.merchantId()),
                    runId,
                    share.merchantId(),
                    share.weightBps(),
                    actualAllocated,
                    prepaid,
                    diff,
                    arApItemId,
                    now));
            lines.add(line);
        }

        // 标记该归属期既有预收行已被本次 true-up 纳入对账（防重复 true-up）。
        repository.markCamAllocationsTruedUp(settlementPeriod);

        return repository.saveCamTrueUpRun(new CamTrueUpRunRecord(
                runId,
                settlementPeriod,
                actualTotalAmountCent,
                prepaidTotal,
                "completed",
                now,
                lines));
    }

    /**
     * 由派生计费/差异流程直接生成一条 ar_ap_items（不经请求体 createItem，但同样遵守锁定经营日硬拦截）。
     * dueDate 取当日（经营日落账），sourceType ∈ SOURCE_TYPES。
     */
    private ArApItemRecord persistDerivedArApItem(
            String direction,
            String counterpartyId,
            String sourceType,
            String sourceId,
            long amountCent,
            String idempotencyKey,
            String merchantId) {
        LocalDate dueDate = LocalDate.now();
        if (repository.lockedDailyCloseExistsForDate(dueDate)) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "business date " + dueDate + " is locked; use the backfill adjustment workflow");
        }
        Instant now = Instant.now();
        String slug = slug(sourceType + "_" + idempotencyKey);
        String id = "arap_" + slug;
        ArApItemRecord item = new ArApItemRecord(
                id,
                "ARAP" + now.toEpochMilli() + "-" + slug.substring(0, Math.min(12, slug.length())),
                direction,
                counterpartyId,
                sourceType,
                sourceId,
                amountCent,
                0L,
                amountCent,
                CURRENCY_CNY,
                dueDate,
                null,
                "receivable".equals(direction) ? "pending_collection" : "pending_payment",
                now,
                now,
                merchantId);
        ArApItemRecord saved = repository.saveArApItem(item);
        repository.saveArApItemSource(new ArApItemSourceRecord(
                "arap_src_" + slug,
                saved.id(),
                sourceType,
                sourceId,
                amountCent,
                now));
        return saved;
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "access to another merchant's resource is not allowed");
        }
    }

    private <T> PageResponse<T> readPage(
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<List<T>> action
    ) {
        return operations.readPage(
                AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                action
        );
    }

    private <T> T executeWrite(
            String operationId,
            String idempotencyKey,
            Object request,
            Class<T> responseType,
            String successEvent,
            String failureEvent,
            String permissionCode,
            String businessType,
            Supplier<T> action
    ) {
        return operations.executeWrite(
                operationId,
                idempotencyKey,
                request,
                responseType,
                AuditedOperation.of(successEvent, failureEvent, permissionCode, businessType),
                action,
                this::businessId,
                Map.of()
        );
    }

    private <T> T requireRequest(T request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        return request;
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return trimmed;
    }

    private LocalDate requireDate(LocalDate value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        return value;
    }

    private long requirePositive(Long value, String fieldName) {
        if (value == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is required");
        }
        if (value <= 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " must be positive");
        }
        return value;
    }

    private String requireEnum(String value, Set<String> allowed, String fieldName) {
        String trimmed = requireText(value, fieldName);
        if (!allowed.contains(trimmed)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return trimmed;
    }

    private String optionalEnum(String value, Set<String> allowed, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        if (!allowed.contains(trimmed)) {
            throw ApiException.badRequest("VALIDATION_FAILED", fieldName + " is invalid");
        }
        return trimmed;
    }

    private String trimToDefault(String value, String defaultValue) {
        String trimmed = trimToNull(value);
        return trimmed == null ? defaultValue : trimmed;
    }

    private String trimToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String slug(String value) {
        String slug = trimToDefault(value, UUID.randomUUID().toString())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        String fallback = UUID.randomUUID().toString().replace("-", "");
        String resolved = slug.isEmpty() ? fallback : slug;
        return resolved.length() > 40 ? resolved.substring(0, 40) : resolved;
    }

    private String businessId(Object value) {
        if (value instanceof ArApItemRecord record) {
            return record.id();
        }
        if (value instanceof ArApPaymentResponse response) {
            return response.payment().id();
        }
        if (value instanceof DailyCloseRecord record) {
            return record.id();
        }
        if (value instanceof PaymentIntentRecord record) {
            return record.id();
        }
        if (value instanceof OfflinePaymentConfirmationRecord record) {
            return record.id();
        }
        if (value instanceof FinanceRefundRecord record) {
            return record.id();
        }
        if (value instanceof ArApAdjustmentRecord record) {
            return record.id();
        }
        if (value instanceof ReconciliationDifferenceRecord record) {
            return record.id();
        }
        if (value instanceof DepositLedgerRecord record) {
            return record.id();
        }
        if (value instanceof CamAllocationRecord record) {
            return record.id();
        }
        if (value instanceof CamTrueUpRunRecord record) {
            return record.id();
        }
        return null;
    }
}
