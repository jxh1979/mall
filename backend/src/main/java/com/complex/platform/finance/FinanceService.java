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
import com.complex.platform.finance.dto.PropertyFeeBillRequest;
import com.complex.platform.finance.dto.PropertyFeeRateRequest;
import com.complex.platform.finance.dto.DailyCloseRequest;
import com.complex.platform.finance.dto.DepositActionRequest;
import com.complex.platform.finance.dto.InventoryVariancePostRequest;
import com.complex.platform.finance.dto.TurnoverRentBillRequest;
import com.complex.platform.finance.dto.TurnoverRentRuleRequest;
import com.complex.platform.finance.dto.OfflinePaymentConfirmRequest;
import com.complex.platform.finance.dto.PaymentIntentRequest;
import com.complex.platform.finance.dto.CloseReconciliationDifferenceRequest;
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
public class FinanceService implements TurnoverRentPort, PartnerVoucherSettlementPort {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final String CURRENCY_CNY = "CNY";
    private static final Set<String> DIRECTIONS = Set.of("receivable", "payable");
    // P1#13/#14 + R1-LOOP-01：库存差异、综合体计费三源、卡座销售提成入账来源。
    private static final Set<String> SOURCE_TYPES = Set.of(
            "contract", "order", "refund", "manual", "settlement", "bar_bill",
            "employee_commission", "inventory_difference", "turnover_rent", "cam", "deposit",
            "property_fee", "pass_drink_settlement", "ticket_distribution");
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

    // i18n：AR/AP 台账 CSV 数据行的用户可见枚举列（收付方向/来源类型/状态）后端翻中文；未知值原样输出不吞值（ponytail: 就近小映射，非全域枚举表）。
    private static final Map<String, String> AR_AP_DIRECTION_LABELS = Map.of(
            "receivable", "应收", "payable", "应付", "internal_accounting", "内部核算");
    private static final Map<String, String> AR_AP_STATUS_LABELS = Map.of(
            "pending_collection", "待收款", "pending_payment", "待付款",
            "partial_paid", "部分收付", "settled", "已结清");
    private static final Map<String, String> AR_AP_SOURCE_TYPE_LABELS = Map.ofEntries(
            Map.entry("contract", "合同"), Map.entry("order", "订单"), Map.entry("refund", "退款"),
            Map.entry("manual", "手工录入"), Map.entry("settlement", "结算"), Map.entry("bar_bill", "酒吧账单"),
            Map.entry("employee_commission", "员工提成"), Map.entry("inventory_difference", "库存差异"),
            Map.entry("turnover_rent", "营业额抽成租金"), Map.entry("cam", "公共区域维护费"),
            Map.entry("deposit", "保证金"), Map.entry("property_fee", "物业费"),
            Map.entry("pass_drink_settlement", "通票酒水结算"), Map.entry("ticket_distribution", "门票分销"));

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

    // Stage B（审计 A2，适用所有退款）：退款分级授权引擎 + 功能标志（默认关）。用字段注入以免改既有多处构造点；
    // 可为 null（旧构造/单测未注入）；包私有便于单测置位。开标志后超额财务退款须复核（见 requireRefundTierAuthorized）。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    RefundAuthorizationService refundAuthorizationService;
    @org.springframework.beans.factory.annotation.Value("${refund.tiered_authorization.enabled:false}")
    boolean tieredRefundEnabled;

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

    public PageResponse<ArApItemRecord> listArApItems(String keyword, int page, int size) {
        // 服务端真分页:租户 scope(currentMerchantScope)决定 merchant_id 谓词,keyword 跨 单号/方向/状态 服务端筛选。
        return operations.readPageResult(
                AuditedOperation.of(
                        "finance_ar_ap.viewed", "finance_ar_ap.view_failed",
                        "finance.ar_ap.view", "ar_ap_item"),
                () -> {
                    String merchantId = authorizationService.currentMerchantScope().orElse(null);
                    // P1-A 两维并存:merchant scope + project scope 谓词同时下推,平台口径两者皆 null=全量。
                    String projectId = authorizationService.currentProjectScopeQuietly().orElse(null);
                    return repository.listArApItemsPaged(merchantId, projectId, keyword, page, size);
                }
        );
    }

    public ArApLedgerExportRecord exportArApItems() {
        try {
            Optional<String> scope = authorizationService.currentMerchantScope();
            // P1-A：导出与列表同口径,两维(merchant + project)谓词并存,避免仅按 merchant 导出泄露他项目行。
            Optional<String> projectScope = authorizationService.currentProjectScopeQuietly();
            List<ArApItemRecord> items = repository.listArApItemsPaged(
                    scope.orElse(null), projectScope.orElse(null), null, 1, Integer.MAX_VALUE).items();
            ArApLedgerExportRecord export = new ArApLedgerExportRecord(
                    "arap_export_" + UUID.randomUUID().toString().replace("-", ""),
                    "csv",
                    items.size(),
                    List.copyOf(items),
                    arApCsv(items),
                    Instant.now()
            );
            auditEventService.recordSuccess(
                    "finance_ar_ap.exported",
                    "finance.ar_ap.view",
                    "ar_ap_export",
                    export.id(),
                    "req_" + UUID.randomUUID(),
                    null,
                    Map.of("row_count", export.rowCount(), "format", export.format())
            );
            return export;
        } catch (RuntimeException exception) {
            auditEventService.recordFailure(
                    "finance_ar_ap.export_failed",
                    "finance.ar_ap.view",
                    "ar_ap_export",
                    "unknown",
                    "req_" + UUID.randomUUID(),
                    null,
                    exception instanceof ApiException apiException ? apiException.code() : "INTERNAL_ERROR"
            );
            throw exception;
        }
    }

    private String arApCsv(List<ArApItemRecord> items) {
        StringBuilder csv = new StringBuilder("单据编号,收付方向,往来对象编号,来源类型,来源编号,金额(分),已收付金额(分),未收付金额(分),币种,到期日,账期,状态\n");
        for (ArApItemRecord item : items) {
            csv.append(csv(item.itemNo())).append(',')
                    .append(csv(arApLabel(AR_AP_DIRECTION_LABELS, item.direction()))).append(',')
                    .append(csv(item.counterpartyId())).append(',')
                    .append(csv(arApLabel(AR_AP_SOURCE_TYPE_LABELS, item.sourceType()))).append(',')
                    .append(csv(item.sourceId())).append(',')
                    .append(item.amountCent()).append(',')
                    .append(item.paidAmountCent()).append(',')
                    .append(item.unpaidAmountCent()).append(',')
                    .append(csv(item.currency())).append(',')
                    .append(csv(item.dueDate() == null ? null : item.dueDate().toString())).append(',')
                    .append(csv(item.accountingPeriod())).append(',')
                    .append(csv(arApLabel(AR_AP_STATUS_LABELS, item.status()))).append('\n');
        }
        return csv.toString();
    }

    /** 枚举列翻中文：命中映射用中文，未知值（含 null）原样返回不吞值。 */
    private static String arApLabel(Map<String, String> labels, String raw) {
        return raw == null ? null : labels.getOrDefault(raw, raw);
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        if (safe.contains(",") || safe.contains("\n") || safe.contains("\"")) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
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
        // F-OPS-09：按调用方 merchant scope 判定——商户 scope 仅看本商户封账，平台口径看全部封账。
        return repository.lockedDailyCloseExistsForDate(
                businessDate, authorizationService.currentMerchantScope().orElse(null));
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
        // F2 内部核算归集：支付意图确认成功（created→succeeded，本方法早返回保证单次）即为收入确认点——
        // 票务/储值/会员（及经票务权益的游乐）收入按 direction=internal_accounting 写内部核算台账，供对账/内部核算下钻。
        // 按裁定 C1，internal_accounting 不并入 revenue.net（net 仍取业务表）。同事务（@Transactional）写入。
        recordInternalAccountingRevenue(intent.id(), intent.businessOrderType(), intent.businessOrderId(), intent.amountCent());
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

    /**
     * F2 内部核算归集：每笔成功支付意图生成一条 direction=internal_accounting 的内部核算台账（平台自营经营收入归集）。
     * 幂等键 = paymentIntentId（每笔支付意图唯一，markPaymentIntentSucceeded 早返回保证单次触发）——
     * 修正此前用 businessOrderType+businessOrderId 派生 id 的缺陷：储值/会员的 businessOrderId=customerId，
     * 同客户多次充值/开通会命中同一 id 被 ON CONFLICT DO UPDATE 覆盖、少计收入。source_type/source_id 仍记业务单维度供追溯。
     * 内部核算=已确认收入，paid=amount、unpaid=0、settled；不并入 revenue.net（C1）；无 merchant scope（平台口径）。
     */
    private void recordInternalAccountingRevenue(String paymentIntentId, String sourceType, String sourceId, long amountCent) {
        String type = trimToNull(sourceType);
        String id = trimToNull(sourceId);
        String intentId = trimToNull(paymentIntentId);
        if (type == null || id == null || intentId == null || amountCent <= 0) {
            return;
        }
        // AUD-059：商户卡充值(stored_value_topup_merchant)不是平台经营收入——商户预收模式钱归商户、平台代收模式为平台预收负债，
        // 二者都不得计入平台内部核算收入。此处跳过；平台代收的消费结算由 recordStoredValueMerchantPayable 逐笔挂应付商户。
        if ("stored_value_topup_merchant".equals(type)) {
            return;
        }
        String key = slug(intentId);
        String itemId = "arap_internal_" + key;
        Instant now = Instant.now();
        // F-OPS-02：内部核算入账须遵守日结锁，禁止向已封账经营日补写收入（与 createArApItem/recordInboundAr 同口径）。
        // F-OPS-09：内部核算=平台口径（merchant_id=null，见上方方法注释），按平台口径判锁（受任一封账约束）。
        java.time.LocalDate businessDate = java.time.LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);
        if (repository.lockedDailyCloseExistsForDate(businessDate, null)) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "business date " + businessDate + " is locked; use the backfill adjustment workflow");
        }
        repository.saveArApItem(new ArApItemRecord(
                itemId,
                "INTACC-" + key,
                "internal_accounting",
                "platform_self",
                type,
                id,
                amountCent,
                amountCent,
                0L,
                CURRENCY_CNY,
                businessDate,
                null,
                "settled",
                now,
                now,
                null
        ));
        repository.saveArApItemSource(new ArApItemSourceRecord(
                "arap_src_internal_" + key, itemId, type, id, amountCent, now));
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
                "finance.reconciliation.view",
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

    /** 正式关闭对账差异（1.6）：幂等（同 key/已关闭返回同结果），落 close_reason+审计。 */
    @Transactional
    public ReconciliationDifferenceRecord closeReconciliationDifference(
            String differenceId, CloseReconciliationDifferenceRequest request) {
        return executeWrite(
                "closeFinanceReconciliationDifference",
                request == null ? null : request.idempotencyKey(),
                request,
                ReconciliationDifferenceRecord.class,
                "finance.reconciliation.difference_closed",
                "finance.reconciliation.difference_close_failed",
                "finance.reconciliation.resolve",
                "finance_reconciliation_difference",
                () -> closeDifference(differenceId, request));
    }

    private ReconciliationDifferenceRecord closeDifference(
            String differenceId, CloseReconciliationDifferenceRequest request) {
        requirePlatformReconciliationAccess();
        CloseReconciliationDifferenceRequest actual = requireRequest(request);
        String closeReason = requireText(actual.closeReason(), "close_reason");
        ReconciliationDifferenceRecord difference = repository
                .findReconciliationDifferenceById(requireText(differenceId, "difference_id"))
                .orElseThrow(() -> ApiException.notFound(
                        "RECONCILIATION_DIFFERENCE_NOT_FOUND", "reconciliation difference not found"));
        // 幂等：已关闭直接返回（executeWrite 幂等键层亦兜底）。
        if ("closed".equals(difference.status())) {
            return difference;
        }
        String closedBy = authorizationService.currentActor().id();
        return repository.closeReconciliationDifference(difference.id(), closeReason, closedBy, Instant.now());
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

    /** 建提成租金规则(填合同条款时):挂合同 + 阶梯超额累进 + 保底(无封顶)。 */
    public TurnoverRentRuleRecord createTurnoverRentRule(TurnoverRentRuleRequest request) {
        return executeWrite(
                "createTurnoverRentRule",
                request == null ? null : request.idempotencyKey(),
                request,
                TurnoverRentRuleRecord.class,
                "finance_turnover_rent_rule.created",
                "finance_turnover_rent_rule.create_failed",
                "finance.turnover_rent.bill",
                "turnover_rent_rule",
                () -> createTurnoverRentRuleInternal(request));
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

    // ── F-OPS-08 物业费固定计费（替代已废止 CAM）─────────────────────────────────

    /** 维护物业费费率（项目级、可按期 upsert）。effectivePeriod 为空=设为该项目默认费率（精确期未命中时回退）。 */
    @com.complex.platform.gate.GuardedAction(capability = "finance.property_fee.manage")
    @Transactional
    public PropertyFeeRateRecord createPropertyFeeRate(PropertyFeeRateRequest request) {
        return executeWrite(
                "createPropertyFeeRate",
                request == null ? null : request.idempotencyKey(),
                request,
                PropertyFeeRateRecord.class,
                "finance_property_fee.rate_set",
                "finance_property_fee.rate_set_failed",
                "finance.property_fee.manage",
                "property_fee_rate",
                () -> createPropertyFeeRateInternal(request));
    }

    /** 列出某项目的物业费费率（含默认与各期）。 */
    public List<PropertyFeeRateRecord> listPropertyFeeRates(String projectId) {
        authorizationService.requirePermission("finance.property_fee.manage");
        return repository.listPropertyFeeRates(requireText(projectId, "project_id"));
    }

    /**
     * 物业费按期计费：对 projectId 下运营提供的 merchantIds，按「每平米固定单价 × 商户面积」逐户生成应收，
     * 不退不补、按 (项目,期,商户) 幂等。面积缺失/已计费的商户跳过。
     */
    @com.complex.platform.gate.GuardedAction(capability = "finance.property_fee.manage", admissionAction = "finance.property_fee_bill", objectType = "property_fee_bill")
    @Transactional
    public List<PropertyFeeBillRecord> billPropertyFee(PropertyFeeBillRequest request) {
        @SuppressWarnings("unchecked")
        List<PropertyFeeBillRecord> result = executeWrite(
                "billPropertyFee",
                request == null ? null : request.idempotencyKey(),
                request,
                (Class<List<PropertyFeeBillRecord>>) (Class<?>) List.class,
                "finance_property_fee.billed",
                "finance_property_fee.bill_failed",
                "finance.property_fee.manage",
                "property_fee_bill",
                () -> billPropertyFeeInternal(request));
        return result;
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

    /**
     * AUD-060 储值退款打款台账：储值退款在渠道未真正兑付（RESERVED/NOT_CONFIGURED/PENDING）时登记一笔
     * status=requested 的退款待办（内部/手工确认打款），复用 finance_refunds 状态机。target_id 承载储值账户 id
     * （该列无外键）。不复用 {@link #createRefund}：其内部强制 target 为 ar_ap_payment 单据，储值退款无 ar_ap 单据。
     * 仅供储值域服务内部调用（调用方已过 customer.stored_value.refund 权限闸并在同一事务/审计留痕），不暴露控制器入口。
     * 幂等：id 由储值退款幂等键派生（sv_ 命名空间与财务退款隔离），saveRefund ON CONFLICT(id) 重试安全。
     */
    public String recordStoredValuePayoutRefund(String accountId, long payoutCent, String reason,
                                                String channel, String idempotencyKey) {
        FinanceRefundRecord saved = repository.saveRefund(new FinanceRefundRecord(
                "refund_sv_" + slug(idempotencyKey),
                requireText(accountId, "account_id"),
                requirePositive(payoutCent, "payout_cent"),
                trimToDefault(reason, "stored value refund payout"),
                trimToDefault(channel, "manual"),
                DEFAULT_REFUND_DIRECTION,
                "requested",
                Instant.now()));
        return saved.id();
    }

    /**
     * AUD-059 平台代收商户卡消费逐笔结算：platform_collect 模式商户卡消费时，生成一条「平台应付商户」台账（逐笔可追溯）。
     * 复用 ar_ap_items（direction=payable、counterparty=merchantId、source_type=stored_value_consume、source_id=消费流水id）；
     * 幂等靠 uq_ar_ap_items_direction_source（direction,source_type,source_id）+ id 派生自消费流水，重放零重复分录；
     * merchant scope=该发卡商户；遵守日结锁（与 recordInternalAccountingRevenue 同口径，按该商户 scope 判锁）。
     * 仅供储值域服务内部调用（调用方已过 customer.stored_value.consume / 结账权限闸并在同一事务/审计留痕），不暴露控制器入口。
     */
    public ArApItemRecord recordStoredValueMerchantPayable(String merchantId, String consumeTxnId,
                                                           long amountCent, String idempotencyKey) {
        String mid = trimToNull(merchantId);
        String txnId = trimToNull(consumeTxnId);
        if (mid == null || txnId == null || amountCent <= 0) {
            return null;
        }
        String key = slug(txnId);
        String itemId = "arap_svpay_" + key;
        Instant now = Instant.now();
        LocalDate businessDate = LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);
        // 遵守日结锁：已封账经营日禁止新增应付，走补录审批（按该商户 scope 判锁，与内部核算/createItem 同口径）。
        if (repository.lockedDailyCloseExistsForDate(businessDate, mid)) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "business date " + businessDate + " is locked; use the backfill adjustment workflow");
        }
        ArApItemRecord saved = repository.saveArApItem(new ArApItemRecord(
                itemId,
                "SVPAY-" + key,
                "payable",
                mid,
                "stored_value_consume",
                txnId,
                amountCent,
                0L,
                amountCent,
                CURRENCY_CNY,
                businessDate,
                null,
                "pending_payment",
                now,
                now,
                mid));
        repository.saveArApItemSource(new ArApItemSourceRecord(
                "arap_src_svpay_" + key, itemId, "stored_value_consume", txnId, amountCent, now));
        return saved;
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
        checkProjectScope(item.projectId());

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
        checkProjectScope(item.projectId());

        Instant now = Instant.now();
        String approver = authorizationService.currentActor().id();

        // F-OPS-07 职责分离（SoD）：补录审批是唯一涉及资金的 finance 审批链（locked backfill：审批通过会写入
        // ar_ap_items 金额变动）。禁止"发起人 = 审批人"自审自批——无论 approve 还是 reject，决策人都不得是该
        // 工单的发起人（requested_by），否则发起人可绕过第二人复核自行放行（或自行撤销）资金调整。
        // 发起人信息可得：requestedBy 在 createAdjustment 时由 currentActor().id() 写入、永不为空。
        if (approver != null && approver.equals(adjustment.requestedBy())) {
            throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN",
                    "the requester of a backfill adjustment cannot decide it; a different approver is required");
        }

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
        // F-OPS-09：用本条目将落账的 merchant scope 判锁，与下方 ArApItemRecord.merchantId 同口径——
        // 商户 scope 仅受本商户封账约束，平台口径受任一封账约束。
        Optional<String> scope = authorizationService.currentMerchantScope();
        // P1-A：落账即写入综合体维度(与 merchantId 同来源=创建方 token 的 scope),历史行由 V243 回填。
        Optional<String> projectScope = authorizationService.currentProjectScopeQuietly();
        // R5 T7 硬拦截：禁止向已锁定经营日新增/回填台账，必须走补录审批。
        if (repository.lockedDailyCloseExistsForDate(dueDate, scope.orElse(null))) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "business date " + dueDate + " is locked; use the backfill adjustment workflow");
        }
        Instant now = Instant.now();
        String slug = slug(actual.idempotencyKey());
        String id = "arap_" + slug;
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
                scope.orElse(null),
                projectScope.orElse(null)
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

        return settleArApItem(
                arApItemId,
                amountCent,
                requireText(actual.paymentMethod(), "payment_method"),
                actual.paymentTime(),
                trimToNull(actual.bankFlowRef()),
                actual.idempotencyKey());
    }

    /**
     * ar_ap 台账结算回写公共逻辑（FOR UPDATE 行锁防并发双扣 + 跨商户校验 + 锁定日拦截 + 登记 payment + 回写 paid/unpaid/status）。
     * 供 registerPayment（在线/后台登记收付款）与 F1 confirmOffline（线下确认收款关联应收）共用，保证回款口径一致。
     */
    private ArApPaymentResponse settleArApItem(
            String arApItemId, long amountCent, String paymentMethod,
            Instant paymentTime, String bankFlowRef, String idempotencyKey) {
        ArApItemRecord current = repository.findArApItemByIdForUpdate(arApItemId)
                .orElseThrow(() -> ApiException.notFound("AR_AP_ITEM_NOT_FOUND", "AR/AP item not found"));
        // 跨商户写入拒绝。
        checkMerchantOwnership(current.merchantId());
        // P1-A 跨项目写入拒绝(两维并存)。
        checkProjectScope(current.projectId());
        // R5 T7 硬拦截：已锁定台账禁止直接登记收付款，必须走补录审批。
        requireNotLocked(current.id());
        if (amountCent > current.unpaidAmountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payment amount exceeds unpaid amount");
        }
        Instant now = Instant.now();
        // 主键纳入 arApItemId，同一幂等键用于不同台账项产生不同 paymentId，避免 ON CONFLICT 覆盖。
        ArApPaymentRecord payment = repository.saveArApPayment(new ArApPaymentRecord(
                "arap_pay_" + slug(arApItemId + "_" + idempotencyKey),
                current.id(),
                amountCent,
                paymentMethod,
                paymentTime == null ? now : paymentTime,
                bankFlowRef,
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
        summarizeDailyClose(dailyCloseId, businessDate, now, scopeType, scopeId);

        // R5 T7：把当日属于本 scope 的 ar_ap_items 打上 locked_day_close_id（merchant 角色仅锁本商户行）。
        Optional<String> merchantScope = authorizationService.currentMerchantScope();
        repository.lockArApItemsForDailyClose(dailyCloseId, businessDate, merchantScope.orElse(null));

        return saved;
    }

    /**
     * 六源汇总聚合（official_ticket / channel_ticket / bar / amusement / supply_cost / thirdparty_reflow），
     * 每源一行写入 finance_daily_close_summaries。聚合公式按聚合草案保守取值，口径未定处见草案 §四"待财务复核"。
     */
    private void summarizeDailyClose(String dailyCloseId, LocalDate businessDate, Instant now,
                                    String scopeType, String scopeId) {
        // 经营日窗口按可配日切（默认 04:00）派生，时间戳类源表统一用 [from, to) 分桶；official_ticket 以 visit_date 为经营日。
        Instant from = businessDayResolver.startInstant(businessDate);
        Instant to = businessDayResolver.endInstant(businessDate);
        for (String source : DAILY_CLOSE_SOURCES) {
            FinanceRepository.DailyCloseSourceTotals totals = switch (source) {
                case "official_ticket" -> repository.aggregateOfficialTicket(businessDate, from, to, scopeType, scopeId);
                case "channel_ticket" -> repository.aggregateChannelTicket(businessDate, from, to, scopeType, scopeId);
                case "bar" -> repository.aggregateBar(businessDate, from, to, scopeType, scopeId);
                case "amusement" -> repository.aggregateAmusement(businessDate, from, to, scopeType, scopeId);
                case "supply_cost" -> repository.aggregateSupplyCost(businessDate, from, to, scopeType, scopeId);
                case "thirdparty_reflow" -> repository.aggregateThirdpartyReflow(businessDate, from, to, scopeType, scopeId);
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
        String channel = PaymentChannelPolicy.requirePaymentIntentChannel(actual.channel());

        // 支付渠道开关（payment.gateway.enabled，后台可配，默认关）。
        //   关 → 不调外部渠道，支付意图标 reserved（占位，由 markPaymentIntentSucceeded 内部确认，不阻塞）。
        //   开 → 路由到 PaymentGatewayPort（当前无真实厂商=占位 NOT_CONFIGURED，留接缝，不崩）。
        // AUD-013：网关请求携带收款商户与定格资金模式（无则为 null=平台收款），真实适配器据此路由收款主体。
        com.complex.platform.finance.gateway.PaymentGatewayResult gw = paymentGateway.initiatePayment(
                new com.complex.platform.finance.gateway.PaymentGatewayRequest(
                        businessOrderType, businessOrderId, amountCent, currency, channel, null,
                        trimToNull(actual.payeeMerchantId()), trimToNull(actual.fundMode())));

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
        // 2d-2 修复（用户裁定：必须关联内部支付意图）：确认线下收款必须对应一个真实支付意图，
        // 且金额一致——堵"凭空对任意 paymentId 字符串伪造已确认线下收款（任意金额）"。
        // 归属为平台财务口径（payment_intents 无 merchant 列），靠 finance.payment.register 权限治理不授商户角色。
        PaymentIntentRecord intent = repository.findPaymentIntentById(bodyPaymentId)
                .orElseThrow(() -> ApiException.notFound("PAYMENT_INTENT_NOT_FOUND", "payment intent not found"));
        long confirmedAmount = requirePositive(actual.amountCent(), "amount_cent");
        if (confirmedAmount != intent.amountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "confirmed amount must equal payment intent amount");
        }
        Instant now = Instant.now();
        String paymentMethod = requireEnum(actual.paymentMethod(), OFFLINE_PAYMENT_METHODS, "payment_method");
        OfflinePaymentConfirmationRecord confirmation = repository.saveOfflinePaymentConfirmation(
                new OfflinePaymentConfirmationRecord(
                        "offline_confirm_" + slug(actual.idempotencyKey()),
                        pathPaymentId,
                        confirmedAmount,
                        paymentMethod,
                        actual.confirmedAt() == null ? now : actual.confirmedAt(),
                        actual.evidenceAttachmentIds() == null ? List.of() : List.copyOf(actual.evidenceAttachmentIds()),
                        "confirmed",
                        now
                ));
        // F1 财务统一台账：线下确认收款若显式关联应收应付台账项，则同事务回写 ar_ap（登记 payment + paid/unpaid/status），
        // 闭合"线下确认与回款台账平行、ar_ap 永不回写"的资金缺口。回写口径与 registerArApPayment 完全一致（共用 settleArApItem）。
        String arApItemId = trimToNull(actual.arApItemId());
        if (arApItemId != null) {
            settleArApItem(arApItemId, confirmedAmount, paymentMethod,
                    actual.confirmedAt(), null, actual.idempotencyKey());
        }
        // AUD-014：柜台确认收款=该笔支付成功——同事务内把支付意图 created→succeeded（CAS，已成功幂等跳过），
        // 否则储值充值等依赖意图状态的入账链永远停留 created/409（生产无其他成功触发器）。
        if ("created".equals(intent.status())) {
            markPaymentIntentSucceeded(bodyPaymentId);
        }
        return confirmation;
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

    /**
     * Stage B 分级授权闸（适用所有退款，功能标志 refund.tiered_authorization.enabled 默认关）：
     * 把退款额按 {@link RefundAuthorizationService#classify} 判档——额度内/规则既定放行，超额（REVIEW_REQUIRED）
     * 因单步退款无独立审批环节而直接拒（REFUND_REVIEW_REQUIRED），须由额度内的授权人办理。
     * 平台管理员旁路（与 {@code requirePermission} 一致）。关闭标志或引擎未注入 → 不拦（维持现状）。
     */
    private void requireRefundTierAuthorized(String domain, long amountCent) {
        if (!tieredRefundEnabled || refundAuthorizationService == null) {
            return; // 关闭标志/引擎未注入 → 不解析操作者、不拦（维持现状，避免对未配置环境产生副作用）。
        }
        var actor = authorizationService.currentActor();
        if (authorizationService.isPlatformAdmin(actor)) {
            return;
        }
        if (refundAuthorizationService.classify(domain, amountCent, actor.id(), null)
                == RefundAuthorizationService.RefundTier.REVIEW_REQUIRED) {
            throw ApiException.forbidden("REFUND_REVIEW_REQUIRED",
                    "refund amount exceeds the self-service authorization limit; an authorized approver must process it");
        }
    }

    private FinanceRefundRecord refund(RefundRequest request) {
        RefundRequest actual = requireRequest(request);
        String targetId = requireText(actual.targetId(), "target_id");
        long refundAmountCent = requirePositive(actual.refundAmountCent(), "refund_amount_cent");
        // P3：现场渠道(cash/pos)退款由服务端强制要求设备上下文，缺 deviceId 直接拒（不靠请求体可空绕过设备门控）。
        requireDeviceContextForOnsiteChannel(actual.refundChannel(), actual.deviceId());
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
        // P1-A 跨项目退款拒绝(两维并存)。
        checkProjectScope(item.projectId());

        // R5 T7 硬拦截：已锁定台账禁止直接退款（影响金额），必须走补录审批。
        requireNotLocked(item.id());

        // P1 fix: prevent refunding more than the original payment (cumulative check)
        long alreadyRefunded = repository.sumRefundedAmountByTargetId(targetId);
        if (alreadyRefunded + refundAmountCent > payment.amountCent()) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "refund amount exceeds refundable balance (payment: " + payment.amountCent()
                            + ", already refunded: " + alreadyRefunded + ")");
        }

        // Stage B 分级授权（功能标志默认关）：超额度财务退款须复核。财务退款为单步台账动作、无独立审批环节，
        // 故超额（REVIEW_REQUIRED）直接拒（REFUND_REVIEW_REQUIRED），由额度内的授权人办理。
        requireRefundTierAuthorized("finance", refundAmountCent);

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
        // 科目方向（用户裁定 C3 对齐 §6D）：
        //   supplier_fault → 向供应商挂应付索赔 payable（真实对外债务）；
        //   overage（盘盈，冲成本）→ payable（账面成本回冲）；
        //   damage/shortage（报损/盘亏）→ internal_accounting 内部核算损耗成本（非对外债权债务，不并入 net）。
        String direction;
        if ("supplier_fault".equals(variance.reason()) || "overage".equals(variance.reason())) {
            direction = "payable";
        } else {
            direction = "internal_accounting";
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

    private TurnoverRentRuleRecord createTurnoverRentRuleInternal(TurnoverRentRuleRequest request) {
        TurnoverRentRuleRequest actual = requireRequest(request);
        String merchantId = requireText(actual.merchantId(), "merchant_id");
        checkMerchantOwnership(merchantId);
        String ruleName = requireText(actual.ruleName(), "rule_name");
        LocalDate effectiveFrom = requireDate(actual.effectiveFrom(), "effective_from");
        LocalDate effectiveTo = actual.effectiveTo();
        if (effectiveTo != null && effectiveTo.isBefore(effectiveFrom)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "effective_to must not be before effective_from");
        }
        List<TurnoverRentRuleRequest.TierInput> tierInputs = actual.tiers();
        if (tierInputs == null || tierInputs.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "at least one tier is required");
        }
        long prevUpper = 0L;
        for (int i = 0; i < tierInputs.size(); i++) {
            TurnoverRentRuleRequest.TierInput t = tierInputs.get(i);
            boolean last = i == tierInputs.size() - 1;
            Integer rate = t.rateBps();
            if (rate == null || rate < 0 || rate > 10000) {
                throw ApiException.badRequest("VALIDATION_FAILED", "tier rate_bps must be within 0..10000");
            }
            if (last) {
                if (t.upperBoundCent() != null) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "the top tier must have no upper bound");
                }
            } else {
                Long upper = t.upperBoundCent();
                if (upper == null || upper <= prevUpper) {
                    throw ApiException.badRequest("VALIDATION_FAILED", "tier upper_bound_cent must be ascending and positive");
                }
                prevUpper = upper;
            }
        }
        Instant now = Instant.now();
        String ruleId = "trr_" + slug(merchantId + "_" + actual.idempotencyKey());
        TurnoverRentRuleRecord rule = repository.saveTurnoverRentRule(new TurnoverRentRuleRecord(
                ruleId,
                actual.contractId(),
                merchantId,
                ruleName,
                0,
                actual.minAmountCent(),
                null,
                effectiveFrom,
                effectiveTo,
                "active",
                now));
        int seq = 1;
        for (TurnoverRentRuleRequest.TierInput t : tierInputs) {
            repository.saveTurnoverRentRuleTier(new TurnoverRentRuleTierRecord(
                    ruleId + "_t" + seq, ruleId, seq, t.upperBoundCent(), t.rateBps()));
            seq++;
        }
        return rule;
    }

    /** 提成端口实现:按合同查其生效提成规则,该商户该自然月回流营业额 × 规则阶梯 → 提成额(分)。合同无生效规则返 0。 */
    @Override
    public long computePeriodTurnoverRent(String merchantId, String contractId, String period) {
        if (contractId == null || contractId.isBlank() || merchantId == null || period == null || !period.matches("\\d{4}-\\d{2}")) {
            return 0L;
        }
        TurnoverRentRuleRecord rule = repository.findActiveTurnoverRentRuleByContract(contractId).orElse(null);
        if (rule == null) {
            return 0L;
        }
        String[] ym = period.split("-");
        LocalDate monthStart = LocalDate.of(Integer.parseInt(ym[0]), Integer.parseInt(ym[1]), 1);
        Instant from = businessDayResolver.startInstant(monthStart);
        Instant to = businessDayResolver.startInstant(monthStart.plusMonths(1));
        long turnover = repository.aggregateMerchantReflowTurnover(merchantId, from, to);
        List<TurnoverRentRuleTierRecord> tiers = repository.listTurnoverRentRuleTiers(rule.id());
        return tiers.isEmpty()
                ? clampRent(turnover, rule.rateBps(), rule.minAmountCent(), rule.maxAmountCent())
                : TurnoverRentMath.tieredRent(turnover, tiers, rule.minAmountCent());
    }

    /** 批3:内部价采购券核销回流 -> 按内部结算价给第三方挂应付(payable, counterparty=partnerId)。 */
    @Override
    public String createPartnerVoucherPayable(String partnerId, long internalPriceCent, String idempotencyKey) {
        ArApItemRecord item = persistDerivedArApItem(
                "payable", partnerId, "partner_voucher", idempotencyKey, internalPriceCent, idempotencyKey, null);
        return item.id();
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

        // 阶梯超额累进优先；无阶梯(旧规则)回退单一费率 clampRent(兼容)。无封顶。
        List<TurnoverRentRuleTierRecord> tiers = repository.listTurnoverRentRuleTiers(rule.id());
        long rent = tiers.isEmpty()
                ? clampRent(turnover, rule.rateBps(), rule.minAmountCent(), rule.maxAmountCent())
                : TurnoverRentMath.tieredRent(turnover, tiers, rule.minAmountCent());
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

    /**
     * 由派生计费/差异流程直接生成一条 ar_ap_items（不经请求体 createItem，但同样遵守锁定经营日硬拦截）。
     * dueDate 取当日（经营日落账），sourceType ∈ SOURCE_TYPES。
     */
    // ponytail: 派生 AR/AP item id="arap_"+slug;物业费出账的 idempotencyKey 含 projectId|period|merchantId 长键→
    // slug 过长会溢出 ar_ap_items.id varchar(64)。仅对超长 slug 哈希封顶(短 slug 不变→其它调用方 id 格式不动)。
    private static String boundedArApSlug(String rawSlug) {
        if (rawSlug.length() <= 48) {
            return rawSlug;
        }
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(rawSlug.getBytes(java.nio.charset.StandardCharsets.UTF_8))).substring(0, 40);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("ar/ap slug hashing unavailable", e);
        }
    }

    private ArApItemRecord persistDerivedArApItem(
            String direction,
            String counterpartyId,
            String sourceType,
            String sourceId,
            long amountCent,
            String idempotencyKey,
            String merchantId) {
        LocalDate dueDate = LocalDate.now();
        // F-OPS-09：派生台账按本条目归属 merchantId 判锁（与下方 ArApItemRecord.merchantId 同口径）；
        // merchantId 为空（平台口径派生）则受任一封账约束。
        if (repository.lockedDailyCloseExistsForDate(dueDate, merchantId)) {
            throw ApiException.conflict("DAY_CLOSE_LOCKED",
                    "business date " + dueDate + " is locked; use the backfill adjustment workflow");
        }
        Instant now = Instant.now();
        String slug = boundedArApSlug(slug(sourceType + "_" + idempotencyKey));
        String id = "arap_" + slug;
        // internal_accounting（内部核算损耗/归集）非对外债权债务：登记即已确认，paid=amount、unpaid=0、settled；
        // receivable/payable 维持待收/待付（unpaid=amount）。
        boolean internalAccounting = "internal_accounting".equals(direction);
        long paidAmountCent = internalAccounting ? amountCent : 0L;
        long unpaidAmountCent = internalAccounting ? 0L : amountCent;
        String status = internalAccounting ? "settled"
                : ("receivable".equals(direction) ? "pending_collection" : "pending_payment");
        ArApItemRecord item = new ArApItemRecord(
                id,
                "ARAP" + now.toEpochMilli() + "-" + slug.substring(0, Math.min(12, slug.length())),
                direction,
                counterpartyId,
                sourceType,
                sourceId,
                amountCent,
                paidAmountCent,
                unpaidAmountCent,
                CURRENCY_CNY,
                dueDate,
                null,
                status,
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

    private PropertyFeeRateRecord createPropertyFeeRateInternal(PropertyFeeRateRequest request) {
        PropertyFeeRateRequest actual = requireRequest(request);
        String projectId = requireText(actual.projectId(), "project_id");
        Long rateBoxed = actual.ratePerSquareMeterCent();
        if (rateBoxed == null || rateBoxed < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", "rate_per_square_meter_cent must be >= 0");
        }
        String effectivePeriod = trimToNull(actual.effectivePeriod());   // null=该项目默认费率
        String status = requireEnum(trimToDefault(actual.status(), "active"),
                Set.of("active", "inactive"), "status");
        Instant now = Instant.now();
        String id = "pfr_" + stableToken("property_fee_rate",
                projectId + "|" + (effectivePeriod == null ? "__default__" : effectivePeriod));
        return repository.savePropertyFeeRate(new PropertyFeeRateRecord(
                id, projectId, effectivePeriod, rateBoxed, status, now, now));
    }

    private List<PropertyFeeBillRecord> billPropertyFeeInternal(PropertyFeeBillRequest request) {
        PropertyFeeBillRequest actual = requireRequest(request);
        String projectId = requireText(actual.projectId(), "project_id");
        String period = requireText(actual.period(), "period");
        // 期格式强校验（yyyy-MM）：错格式期会生成商户费用台账/AR 都对不上的孤儿账期，入口即拒。
        try {
            java.time.YearMonth.parse(period);
        } catch (java.time.format.DateTimeParseException e) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period must be yyyy-MM, e.g. 2026-07");
        }
        List<String> merchantIds = actual.merchantIds();
        if (merchantIds == null || merchantIds.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "merchant_ids is required");
        }
        // 费率按 项目+期 解析一次（精确期优先、回退默认）；无生效费率直接拒绝整批。
        PropertyFeeRateRecord rate = repository.findEffectivePropertyFeeRate(projectId, period)
                .orElseThrow(() -> ApiException.badRequest("PROPERTY_FEE_RATE_NOT_FOUND",
                        "no active property fee rate configured for project " + projectId + " period " + period));
        long rateCent = rate.ratePerSquareMeterCent();
        Instant now = Instant.now();
        List<PropertyFeeBillRecord> result = new java.util.ArrayList<>();
        for (String rawMerchantId : merchantIds) {
            String merchantId = requireText(rawMerchantId, "merchant_id");
            checkMerchantOwnership(merchantId);
            // 不退不补 + 幂等：该 (项目,期,商户) 已计过费则跳过（含跨请求重复计费，不二次挂账）。
            if (repository.propertyFeeBillExists(projectId, period, merchantId)) {
                continue;
            }
            java.math.BigDecimal area = repository.findMerchantAreaSquareMeter(merchantId);
            if (area == null || area.signum() <= 0) {
                // 面积缺失/非正，无法按面积计费——跳过（运营补面积后重跑，已计费的不重复）。
                continue;
            }
            long amountCent = java.math.BigDecimal.valueOf(rateCent)
                    .multiply(area)
                    .setScale(0, java.math.RoundingMode.HALF_UP)
                    .longValueExact();
            if (amountCent <= 0) {
                continue;   // rate=0 等导致 0 额，不挂 0 元应收。
            }
            // ar_ap 幂等键按 (项目,期,商户) 稳定派生（与 bills 行同源），跨请求重复计费 upsert 同一台账行、不重复挂账。
            String naturalKey = projectId + "|" + period + "|" + merchantId;
            ArApItemRecord item = persistDerivedArApItem(
                    "receivable",
                    merchantId,
                    "property_fee",
                    "property_fee_" + naturalKey,
                    amountCent,
                    stableToken("property_fee", naturalKey),
                    merchantId);
            PropertyFeeBillRecord bill = repository.savePropertyFeeBill(new PropertyFeeBillRecord(
                    "pfb_" + stableToken("property_fee_bill", naturalKey),
                    projectId,
                    period,
                    merchantId,
                    area,
                    rateCent,
                    amountCent,
                    item.id(),
                    now));
            result.add(bill);
        }
        return result;
    }

    /** 由稳定自然键派生确定性、抗截断的幂等令牌（名字 UUID v3，跨请求一致；用于物业费 ar_ap/留痕行 id）。 */
    private String stableToken(String namespace, String naturalKey) {
        return java.util.UUID.nameUUIDFromBytes(
                (namespace + ":" + naturalKey).getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .toString().replace("-", "");
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "access to another merchant's resource is not allowed");
        }
    }

    /**
     * P1-A 财务项目级隔离(与 checkMerchantOwnership 同范式,两维并存):
     * 项目 scope 调用方(currentProjectScopeQuietly 非空,即带综合体归属的项目财务员)不得读写他项目台账。
     * 平台/无项目归属调用方返回 empty→不拦(全量);资源 project_id 为 null(历史未回填行)→不拦(不误伤,依赖 merchant 维)。
     */
    private void checkProjectScope(String resourceProjectId) {
        Optional<String> scope = authorizationService.currentProjectScopeQuietly();
        if (scope.isPresent() && resourceProjectId != null && !scope.get().equals(resourceProjectId)) {
            throw ApiException.forbidden("CROSS_PROJECT_FORBIDDEN",
                    "access to another project's finance resource is not allowed");
        }
    }

    /**
     * P3 现场渠道退款设备门控(服务端判定,非请求体可空 deviceId):
     * 现场终端渠道(cash/pos=线下现金/刷卡,天然发生在现场设备)退款必须携带 deviceId 走 MDM 门控;
     * 缺设备上下文=fail-closed 拒绝,堵"现场退款不带 deviceId 即绕过设备门控"。
     * 后台/线上渠道(manual/第三方等)无现场设备语义,不强制。
     */
    private static final Set<String> ONSITE_REFUND_CHANNELS = Set.of("cash", "pos");

    private void requireDeviceContextForOnsiteChannel(String refundChannel, String deviceId) {
        String channel = trimToNull(refundChannel);
        if (channel != null && ONSITE_REFUND_CHANNELS.contains(channel.toLowerCase())
                && trimToNull(deviceId) == null) {
            throw ApiException.forbidden("DEVICE_CONTEXT_REQUIRED",
                    "on-site channel refund requires an operating device context (deviceId)");
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
        return null;
    }
}
