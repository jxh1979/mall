package com.complex.platform.dividend;
// 文件版本：V0.06

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.dividend.DividendRecords.DividendComputeResultRecord;
import com.complex.platform.dividend.DividendRecords.DividendComputeSkipRecord;
import com.complex.platform.dividend.DividendRecords.DividendPlanRecord;
import com.complex.platform.dividend.DividendRecords.DividendReserveLedgerRecord;
import com.complex.platform.dividend.DividendRecords.DividendReserveReleaseCandidateRecord;
import com.complex.platform.dividend.DividendRecords.DividendReserveReleaseRecord;
import com.complex.platform.dividend.DividendRecords.DividendReserveReleaseRunRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementDetailRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementLineRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementRecord;
import com.complex.platform.dividend.DividendRecords.ExpenseTypeSumRecord;
import com.complex.platform.dividend.dto.DividendActionRequest;
import com.complex.platform.dividend.dto.DividendComputeRequest;
import com.complex.platform.dividend.dto.DividendLegalConfirmationRequest;
import com.complex.platform.dividend.dto.DividendPlanRequest;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.inventory.InventoryCostSummaryRecord;
import com.complex.platform.inventory.InventoryRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

/**
 * 持股分红引擎（2026-07-18 拍板：净利润制 / 真实股东股息红利 20% 代扣 / 季度正式+月度预览）。
 *
 * <p>算式（整数分，恒等式由 V299 DB CHECK 硬保证）：
 * 期间收入（复用 revenue.net 商户行级谓词区间版：酒吧实收−酒吧退款 + 已确认月度分账单的已核销门票结算额（FS-015），计入域可配）
 * − 期间销货成本（V296 sumOutboundCost 同谓词，可配开关）
 * − 期间费用（V297 费用归集台账净额，计入类别可配）
 * = 净利润 → 本次分配 = min(经营口径可分配额, 财务登记的依法可分配金额)（FS-014，正式单未登记不得提交/审批）
 * → 按方案比例劈分（余数归主理人） → 主理人应得 → 代扣股息红利个税（税率可配，默认 20%，
 * 平台分成/风险准备金四舍五入、代扣个税整除截断对齐税局——2026-07-28 用户拍板） → 净发放额。
 *
 * <p>时窗：BusinessDayResolver 方案商户的营业日窗（名下唯一酒吧按该酒吧，否则按其所属综合体；成本汇总同窗）——三来源同窗才能恒等；
 * 费用侧为 V297 的月份桶粒度（台账本身按月归集）。
 *
 * <p>状态机：preview（月预览，只可看不可审批）；official：draft → pending_review → approved（挂应付
 * source_type='dividend_payout'）→ paid；pending_review 可驳回 rejected。重算=旧单 voided + 新单
 * （红字更正范式），已进入审批流（pending_review/approved/paid）不重算。
 *
 * <p>写路径统一走 {@link AuditedOperationExecutor#executeWrite}（同键不同载荷抛 IDEMPOTENCY_CONFLICT）。
 * 审批要求 finance.dividend.manage（V299 仅授 role_platform_admin=最高权限审批现状口径）。
 */
@Service
public class DividendService {

    static final String VIEW_PERM = "finance.dividend.view";
    static final String MANAGE_PERM = "finance.dividend.manage";
    private static final String PLAN_TYPE = "dividend_plan";
    private static final String STMT_TYPE = "dividend_statement";

    static final Set<String> REVENUE_DOMAINS = Set.of("bar", "ticket");
    static final Set<String> EXPENSE_TYPES = Set.of(
            "rent_allocation", "energy", "property_fee", "labor", "amortization", "other");
    /** BSM L1777「分红周期一律取月及月的整数倍粒度（如月、季、半年、年）」。 */
    private static final Set<String> PERIOD_TYPES = DividendPeriod.MONTHS_PER_PERIOD.keySet();
    /** 正式单期间标签与方案周期不符时的跳过说明。 */
    private static final java.util.Map<String, String> OFFICIAL_LABEL_HINTS = java.util.Map.of(
            "monthly", "月度方案的正式单需按月份标签（如 2026-07）出单，跳过",
            "quarterly", "季度方案的正式单需按季度标签（如 2026-Q3）出单，跳过",
            "semiannual", "半年方案的正式单需按半年标签（如 2026-H1）出单，跳过",
            "annual", "年度方案的正式单需按年度标签（如 2026）出单，跳过");
    private static final Set<String> PLAN_STATUSES = Set.of("active", "ended");
    private static final Set<String> KINDS = Set.of("official", "preview");
    private static final Set<String> STATEMENT_STATUSES = Set.of(
            "preview", "draft", "pending_review", "approved", "paid", "rejected", "voided");
    /** 可重算（作废+重出）的状态；已进入审批流的单不重算。 */
    private static final Set<String> RECOMPUTABLE_STATUSES = Set.of("preview", "draft", "rejected");
    /** 月度预览定时作业的系统操作人标识（无请求上下文，落 computed_by/voided_by 便于留痕追溯）。 */
    static final String SYSTEM_OPERATOR = "system_dividend_preview";
    /** V304 留存自动释放定时作业的系统操作人标识（落 released_by 便于留痕追溯）。 */
    static final String SYSTEM_RELEASE_OPERATOR = "system_dividend_reserve_release";
    /** V304 留存释放规则可选值；默认手动=不自动释放（最安全，每笔人工决策）。 */
    static final Set<String> RESERVE_RELEASE_MODES = Set.of("manual", "next_period", "fixed_delay", "annual");
    private static final String DEFAULT_RESERVE_RELEASE_MODE = "manual";
    /** V308 主理人类型：individual 个人（代扣股息红利个税）/ company 公司（不代扣、全额发放）。 */
    static final Set<String> PRINCIPAL_TYPES = Set.of("individual", "company");
    private static final String DEFAULT_PRINCIPAL_TYPE = "individual";
    /** V309 自定义扣除项来源类型 / 比例基数可选值。 */
    static final Set<String> DEDUCTION_SOURCE_TYPES = Set.of("fixed", "ratio");
    static final Set<String> DEDUCTION_RATIO_BASES = Set.of("net_profit", "revenue");
    private static final String DEFAULT_DEDUCTION_BASIS = "net_profit";

    // 默认口径（仅新建方案时的默认值，全部可配）。
    private static final int DEFAULT_PLATFORM_SHARE_BPS = 8000;
    private static final int DEFAULT_TAX_RATE_BPS = 2000;
    /** BSM L1777「分红按所配周期一期一分配，默认周期为半年」。 */
    private static final String DEFAULT_PERIOD_TYPE = "semiannual";
    private static final List<String> DEFAULT_REVENUE_DOMAINS = List.of("bar", "ticket");
    private static final List<String> DEFAULT_EXPENSE_TYPES = List.of(
            "rent_allocation", "energy", "property_fee", "labor", "amortization", "other");

    private final DividendRepository repository;
    private final InventoryRepository inventoryRepository;
    private final BusinessDayResolver businessDayResolver;
    private final AuthorizationService authorizationService;
    private final AuditedOperationExecutor operations;
    private final FinanceService payoutPort;

    // 第十八批第 13 条（BSM 第 1597 行「分红出单与发放」，第 1598 行不可关闭）：业务提交后通知商户。@Lazy 防初始化环；为 null（单测手工构造）＝不发。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    com.complex.platform.merchantbilling.MerchantAmountChangeNotifier merchantAmountNotifier;

    // 第 92 条（BSM:1597 首页未读账单提醒）：商户账号在我的分红读到审批通过后的分红单即记已读。@Lazy 防初始化环；为 null（单测手工构造）＝不记。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    com.complex.platform.merchantportal.MerchantUnreadBillService merchantUnreadBills;

    private void notifyMerchantAmount(String merchantId, String event, long amountCent, String reason, Instant visibleAt, String refKey) {
        if (merchantAmountNotifier != null) {
            merchantAmountNotifier.notifyAfterCommit(merchantId, event, amountCent, reason, null,
                    visibleAt.atZone(com.complex.platform.common.LocalZone.ID).toLocalDate(), null, refKey);
        }
    }

    private static String yuan(long cent) {
        return java.math.BigDecimal.valueOf(cent, 2).toPlainString() + " 元";
    }

    public DividendService(
            DividendRepository repository,
            InventoryRepository inventoryRepository,
            BusinessDayResolver businessDayResolver,
            AuthorizationService authorizationService,
            AuditedOperationExecutor operations,
            FinanceService payoutPort) {
        this.repository = repository;
        this.inventoryRepository = inventoryRepository;
        this.businessDayResolver = businessDayResolver;
        this.authorizationService = authorizationService;
        this.operations = operations;
        this.payoutPort = payoutPort;
    }

    // ── 方案配置 ──────────────────────────────────────────────

    /** 方案列表。商户 scope 账号强制只看自己（覆盖请求参数）。 */
    public List<DividendPlanRecord> listPlans(String merchantId) {
        authorizationService.requirePermission(VIEW_PERM);
        return operations.readSingle(
                AuditedOperation.of("dividend.plans_listed", "dividend.plans_list_failed", VIEW_PERM, PLAN_TYPE),
                () -> repository.listPlans(blankToNull(scopeMerchant(merchantId))),
                result -> "all");
    }

    /** 新建分红方案：一个商户同一时期只允许一个生效中方案；空缺字段按默认口径补齐。 */
    @Transactional
    public DividendPlanRecord createPlan(DividendPlanRequest request) {
        authorizationService.requirePermission(MANAGE_PERM);
        authorizationService.requirePlatformScope();
        requireRequest(request);
        return operations.executeWrite(
                "createDividendPlan",
                request.idempotencyKey(),
                request,
                DividendPlanRecord.class,
                AuditedOperation.of("dividend.plan_created", "dividend.plan_create_failed", MANAGE_PERM, PLAN_TYPE),
                () -> createPlanInternal(request),
                DividendService::businessId,
                java.util.Map.of());
    }

    private DividendPlanRecord createPlanInternal(DividendPlanRequest request) {
        String merchantId = requireText(request.merchantId(), "merchant_id");
        String principalUserId = requireText(request.principalUserId(), "principal_user_id");
        Instant now = Instant.now();
        DividendPlanRecord plan = new DividendPlanRecord(
                "divp_" + stableToken(request.idempotencyKey()),
                merchantId,
                principalUserId,
                blankToNull(request.principalName()),
                requireBps(orDefault(request.platformShareBps(), DEFAULT_PLATFORM_SHARE_BPS), "platform_share_bps"),
                requireBps(orDefault(request.taxRateBps(), DEFAULT_TAX_RATE_BPS), "tax_rate_bps"),
                requireEnum(orDefault(request.periodType(), DEFAULT_PERIOD_TYPE), PERIOD_TYPES, "period_type"),
                requireTokens(orDefault(request.revenueDomains(), DEFAULT_REVENUE_DOMAINS), REVENUE_DOMAINS,
                        "revenue_domains"),
                request.includeInventoryCost() == null || request.includeInventoryCost(),
                requireTokens(orDefault(request.expenseTypes(), DEFAULT_EXPENSE_TYPES), EXPENSE_TYPES,
                        "expense_types"),
                parseDate(request.effectiveFrom(), "effective_from"),
                "active",
                blankToNull(request.note()),
                currentOperator(),
                now,
                now,
                // V303 扣除项三开关（默认全关=不改 V299 口径）+ 保护线参数。
                Boolean.TRUE.equals(request.lossCarryforwardEnabled()),
                Boolean.TRUE.equals(request.arrearsOffsetEnabled()),
                Boolean.TRUE.equals(request.payoutFloorEnabled()),
                requireBps(orDefault(request.payoutFloorBps(), 0), "payout_floor_bps"),
                requireNonNegLong(orDefault(request.payoutFloorFixedCent(), 0L), "payout_floor_fixed_cent"),
                // V304 留存释放规则（默认手动=不自动释放）。
                requireEnum(orDefault(request.reserveReleaseMode(), DEFAULT_RESERVE_RELEASE_MODE),
                        RESERVE_RELEASE_MODES, "reserve_release_mode"),
                requirePositiveInt(orDefault(request.reserveReleaseDelayPeriods(), 1),
                        "reserve_release_delay_periods"),
                // V308 主理人类型（默认个人=代扣个税）；V309 自定义扣除项另表持久化，插入记录先占空。
                requireEnum(orDefault(request.principalType(), DEFAULT_PRINCIPAL_TYPE),
                        PRINCIPAL_TYPES, "principal_type"),
                List.of());
        if (!repository.insertPlan(plan)) {
            throw ApiException.conflict("DIVIDEND_PLAN_EXISTS",
                    "该商户已有生效中的分红方案；请修改现有方案或先停用（status=ended）再新建");
        }
        // V309 自定义扣除项：新建时按请求整表落地（空/缺省=无自定义扣除）。
        repository.replaceCustomDeductions(plan.id(), parseCustomDeductions(plan.id(), request.customDeductions()));
        return repository.findPlanById(plan.id())
                .orElseThrow(() -> ApiException.conflict("DIVIDEND_PLAN_EXISTS", "分红方案创建时发生并发冲突，请重试"));
    }

    /** 修改方案配置（比例/税率/周期/基数构成开关/停用）；改动经审计事件留痕，历史分红单按单内快照追溯。 */
    @Transactional
    public DividendPlanRecord updatePlan(String planId, DividendPlanRequest request) {
        authorizationService.requirePermission(MANAGE_PERM);
        authorizationService.requirePlatformScope();
        requireRequest(request);
        String id = requireText(planId, "plan_id");
        return operations.executeWrite(
                "updateDividendPlan:" + id,
                request.idempotencyKey(),
                request,
                DividendPlanRecord.class,
                AuditedOperation.of("dividend.plan_updated", "dividend.plan_update_failed", MANAGE_PERM, PLAN_TYPE),
                () -> updatePlanInternal(id, request),
                DividendService::businessId,
                java.util.Map.of());
    }

    private DividendPlanRecord updatePlanInternal(String planId, DividendPlanRequest request) {
        DividendPlanRecord existing = repository.findPlanById(planId)
                .orElseThrow(() -> ApiException.notFound("DIVIDEND_PLAN_NOT_FOUND",
                        "分红方案不存在：" + planId));
        if (request.merchantId() != null && !request.merchantId().isBlank()
                && !existing.merchantId().equals(request.merchantId().trim())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "分红方案的所属商户不能变更");
        }
        DividendPlanRecord updated = new DividendPlanRecord(
                existing.id(),
                existing.merchantId(),
                request.principalUserId() == null || request.principalUserId().isBlank()
                        ? existing.principalUserId() : request.principalUserId().trim(),
                request.principalName() == null ? existing.principalName() : blankToNull(request.principalName()),
                requireBps(orDefault(request.platformShareBps(), existing.platformShareBps()), "platform_share_bps"),
                requireBps(orDefault(request.taxRateBps(), existing.taxRateBps()), "tax_rate_bps"),
                requireEnum(orDefault(request.periodType(), existing.periodType()), PERIOD_TYPES, "period_type"),
                requireTokens(orDefault(request.revenueDomains(), existing.revenueDomains()), REVENUE_DOMAINS,
                        "revenue_domains"),
                request.includeInventoryCost() == null ? existing.includeInventoryCost()
                        : request.includeInventoryCost(),
                requireTokens(orDefault(request.expenseTypes(), existing.expenseTypes()), EXPENSE_TYPES,
                        "expense_types"),
                request.effectiveFrom() == null ? existing.effectiveFrom()
                        : parseDate(request.effectiveFrom(), "effective_from"),
                requireEnum(orDefault(request.status(), existing.status()), PLAN_STATUSES, "status"),
                request.note() == null ? existing.note() : blankToNull(request.note()),
                currentOperator(),
                existing.createdAt(),
                Instant.now(),
                request.lossCarryforwardEnabled() == null
                        ? existing.lossCarryforwardEnabled() : request.lossCarryforwardEnabled(),
                request.arrearsOffsetEnabled() == null
                        ? existing.arrearsOffsetEnabled() : request.arrearsOffsetEnabled(),
                request.payoutFloorEnabled() == null
                        ? existing.payoutFloorEnabled() : request.payoutFloorEnabled(),
                requireBps(orDefault(request.payoutFloorBps(), existing.payoutFloorBps()), "payout_floor_bps"),
                requireNonNegLong(orDefault(request.payoutFloorFixedCent(), existing.payoutFloorFixedCent()),
                        "payout_floor_fixed_cent"),
                // V304 留存释放规则（可空=不改现值）。
                requireEnum(orDefault(request.reserveReleaseMode(), existing.reserveReleaseMode()),
                        RESERVE_RELEASE_MODES, "reserve_release_mode"),
                request.reserveReleaseDelayPeriods() == null ? existing.reserveReleaseDelayPeriods()
                        : requirePositiveInt(request.reserveReleaseDelayPeriods(), "reserve_release_delay_periods"),
                // V308 主理人类型（可空=不改现值）；V309 自定义扣除项另表持久化，更新记录先占空。
                requireEnum(orDefault(request.principalType(), existing.principalType()),
                        PRINCIPAL_TYPES, "principal_type"),
                List.of());
        if (!repository.updatePlan(updated)) {
            // ended 改回 active 撞同商户唯一生效索引（uq_dividend_plans_active_merchant）→ 业务 409 非 500。
            throw ApiException.conflict("DIVIDEND_PLAN_EXISTS",
                    "该商户已有另一生效中的分红方案，无法再启用本方案；请先停用另一方案");
        }
        // V309 自定义扣除项：请求提供 customDeductions 才整表替换（null=不改现值，历史配置保留）。
        if (request.customDeductions() != null) {
            repository.replaceCustomDeductions(planId, parseCustomDeductions(planId, request.customDeductions()));
        }
        return repository.findPlanById(planId)
                .orElseThrow(() -> ApiException.notFound("DIVIDEND_PLAN_NOT_FOUND", "分红方案已被并发修改，请刷新后重试"));
    }

    /** V309 请求项 → 校验 + 落地记录（sort_order=输入序）。fixed 用固定额；ratio 用比例+基数。 */
    private List<DividendRecords.DividendCustomDeductionRecord> parseCustomDeductions(
            String planId, List<DividendPlanRequest.CustomDeductionItem> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<DividendRecords.DividendCustomDeductionRecord> parsed = new ArrayList<>();
        int order = 0;
        for (DividendPlanRequest.CustomDeductionItem item : items) {
            if (item == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "自定义扣减项不能为空");
            }
            String name = requireText(item.name(), "custom_deduction.name");
            String sourceType = requireEnum(orDefault(item.sourceType(), "fixed"),
                    DEDUCTION_SOURCE_TYPES, "custom_deduction.source_type");
            long amountCent = requireNonNegLong(orDefault(item.amountCent(), 0L), "custom_deduction.amount_cent");
            int ratioBps = requireBps(orDefault(item.ratioBps(), 0), "custom_deduction.ratio_bps");
            String ratioBasis = requireEnum(orDefault(item.ratioBasis(), DEFAULT_DEDUCTION_BASIS),
                    DEDUCTION_RATIO_BASES, "custom_deduction.ratio_basis");
            boolean enabled = item.enabled() == null || item.enabled();
            parsed.add(new DividendRecords.DividendCustomDeductionRecord(
                    "divd_" + UUID.randomUUID().toString().replace("-", ""),
                    planId, name, sourceType, amountCent, ratioBps, ratioBasis, enabled, order++));
        }
        return parsed;
    }

    // ── 期间作业 ──────────────────────────────────────────────

    /** 期间分红作业（手动触发，幂等可重跑）：对每个生效中方案出计算单，算式逐项落明细行。 */
    @Transactional
    public DividendComputeResultRecord compute(DividendComputeRequest request) {
        authorizationService.requirePermission(MANAGE_PERM);
        authorizationService.requirePlatformScope();
        requireRequest(request);
        return operations.executeWrite(
                "computeDividendStatements",
                request.idempotencyKey(),
                request,
                DividendComputeResultRecord.class,
                AuditedOperation.of("dividend.computed", "dividend.compute_failed", MANAGE_PERM, STMT_TYPE),
                () -> computeInternal(request),
                DividendService::businessId,
                java.util.Map.of());
    }

    private DividendComputeResultRecord computeInternal(DividendComputeRequest request) {
        String kind = requireEnum(requireText(request.kind(), "kind"), KINDS, "kind");
        DividendPeriod period = DividendPeriod.parse(request.periodLabel());
        if ("preview".equals(kind) && !DividendPeriod.isMonthLabel(period.label())) {
            throw ApiException.badRequest("VALIDATION_FAILED",
                    "preview 只支持月度期间标签（如 2026-07）");
        }
        List<DividendPlanRecord> plans;
        if (request.merchantId() != null && !request.merchantId().isBlank()) {
            plans = List.of(repository.findActivePlanByMerchant(request.merchantId().trim())
                    .orElseThrow(() -> ApiException.notFound("DIVIDEND_PLAN_NOT_FOUND",
                            "该商户没有生效中的分红方案: " + request.merchantId())));
        } else {
            plans = repository.listActivePlans();
        }
        return runCompute(plans, kind, period, currentOperator());
    }

    /**
     * 月度预览定时作业入口（{@link DividendMonthlyPreviewJob} 调用）：无请求上下文、系统操作人，
     * 对每个生效中方案出上月 preview 单（季度、半年、年度方案出，月度方案跳过）。刷新式幂等——每次跑作废旧
     * 未作废预览单再重出（runCompute 内 RECOMPUTABLE 含 preview）。preview 不可审批，维持 V299 拍板。
     * 无鉴权闸：定时线程无 SecurityContext，本方法只对内部作业开放（无 Controller 绑定）。
     */
    @Transactional
    public DividendComputeResultRecord runScheduledMonthlyPreview(java.time.YearMonth month) {
        DividendPeriod period = DividendPeriod.parse(month.toString());
        return runCompute(repository.listActivePlans(), "preview", period, SYSTEM_OPERATOR);
    }

    /** 期间作业核心循环（请求路径与定时预览共用；operator 显式传入以脱离鉴权上下文）。 */
    private DividendComputeResultRecord runCompute(
            List<DividendPlanRecord> plans, String kind, DividendPeriod period, String operator) {
        List<DividendStatementRecord> computed = new ArrayList<>();
        List<DividendComputeSkipRecord> skipped = new ArrayList<>();
        Instant now = Instant.now();
        for (DividendPlanRecord plan : plans) {
            String mismatch = periodMismatchReason(kind, period, plan);
            if (mismatch != null) {
                skipped.add(new DividendComputeSkipRecord(plan.merchantId(), mismatch));
                continue;
            }
            Optional<DividendStatementRecord> existing =
                    repository.findActiveStatement(plan.merchantId(), kind, period.label());
            if (existing.isPresent()) {
                if (!RECOMPUTABLE_STATUSES.contains(existing.get().status())) {
                    skipped.add(new DividendComputeSkipRecord(plan.merchantId(),
                            "已有计算单进入审批流（" + existing.get().status() + "），不重算"));
                    continue;
                }
                // C-1：作废 UPDATE 带期望前态守卫（preview/draft/rejected）；0 行=并发下已进入审批流，跳过不重算。
                if (repository.markVoided(existing.get().id(), operator, now) == 0) {
                    skipped.add(new DividendComputeSkipRecord(plan.merchantId(),
                            "计算单状态在重算期间被并发变更（已进入审批流），本次跳过"));
                    continue;
                }
            }
            computed.add(computeStatement(plan, kind, period, operator, now));
        }
        return new DividendComputeResultRecord(period.label(), kind, computed, skipped);
    }

    /**
     * kind=official 时期间标签必须匹配方案周期类型（BSM L1777「分红按所配周期一期一分配」）；
     * kind=preview（每月只读预览单）对周期长于一个月的方案出，月度方案的正式单即当月，不另出预览。
     */
    private static String periodMismatchReason(String kind, DividendPeriod period, DividendPlanRecord plan) {
        if ("preview".equals(kind)) {
            return "monthly".equals(plan.periodType()) ? "月度正式方案无需月度预览，跳过" : null;
        }
        return plan.periodType().equals(DividendPeriod.periodTypeOf(period.label()))
                ? null : OFFICIAL_LABEL_HINTS.get(plan.periodType());
    }

    private DividendStatementRecord computeStatement(
            DividendPlanRecord plan, String kind, DividendPeriod period, String operator, Instant now) {
        String merchantId = plan.merchantId();
        // 三来源同窗（第十八批第 1 条，ENUM:187 按每家酒吧可配）：方案商户名下唯一酒吧按该酒吧营业日，否则按其所属综合体。
        String bar = businessDayResolver.barOfMerchant(merchantId);
        String dayProject = bar == null ? businessDayResolver.projectOfMerchant(merchantId) : null;
        Instant from = businessDayResolver.start(dayProject, bar, period.start());
        Instant to = businessDayResolver.end(dayProject, bar, period.end());
        String window = "[" + period.start() + "," + period.end().plusDays(1) + ") 经营日窗";

        String statementId = "divs_" + UUID.randomUUID().toString().replace("-", "");
        List<DividendStatementLineRecord> lines = new ArrayList<>();
        int[] seq = {0};
        long revenue = 0L;

        if (plan.revenueDomains().contains("bar")) {
            long barGross = repository.sumBarRevenueCent(merchantId, from, to);
            long barRefund = repository.sumBarRefundCent(merchantId, from, to);
            revenue += barGross - barRefund;
            addLine(lines, statementId, merchantId, seq, "revenue_bar", barGross,
                    "酒吧整单已确认商品收入及原经营日月结服务费扣留" + window, now);
            addLine(lines, statementId, merchantId, seq, "revenue_bar_refund", -barRefund,
                    "酒吧已实际退成的商品分项" + window, now);
        }
        long ticketServiceFee = 0L;
        if (plan.revenueDomains().contains("ticket")) {
            // FS-015（BSM L755「确认已核销部分的门票收入」、L1689「未核销不产生提供方结算」）：取已确认月度分账单的
            //   已核销结算额，与结算域同一事实源；退票只发生在完全未核销的票上，从未计入结算额，不再另扣。
            List<String> pending = repository.listUnconfirmedTicketSettlementMonths(period.months());
            if (!pending.isEmpty() && "official".equals(kind)) {
                throw ApiException.conflict("TICKET_SETTLEMENT_PENDING",
                        "票务月度分账（" + String.join("、", pending) + "）尚未确认，不能确认分红");
            }
            long ticketSettled = repository.sumTicketSettledCent(merchantId, period.start(), period.end());
            revenue += ticketSettled;
            addLine(lines, statementId, merchantId, seq, "revenue_ticket", ticketSettled,
                    "已核销门票结算额（已确认月度分账单：单一酒吧门票为已核销票款直付额，通票为核销入场后的池分配额；"
                            + "到期未核销款与通票未核销减除额不计；月份 " + String.join("、", period.months()) + "）"
                            + (pending.isEmpty() ? "" : "；分账未确认月份 " + String.join("、", pending) + " 暂不计"), now);
            // FS-013 后单一酒吧门票综合服务费按售出月单独计收、不再从直付额内扣，按 BSM L1774 作为费用扣减。
            ticketServiceFee = repository.sumSingleBarServiceFeeCent(merchantId, period.start(), period.end());
        }

        long cost = 0L;
        long noCostBasisCount = 0L;
        if (plan.includeInventoryCost()) {
            InventoryCostSummaryRecord costSummary = inventoryRepository.sumOutboundCost(
                    merchantId, null, period.start().toString(), period.end().toString(), from, to);
            cost = costSummary.outboundTotalCostCent();
            noCostBasisCount = costSummary.noCostBasisCount();
            addLine(lines, statementId, merchantId, seq, "cost_inventory", -cost,
                    "销货成本（V296 移动加权出库成本汇总）" + window
                            + (noCostBasisCount > 0
                                    ? "；无成本基准出库 " + noCostBasisCount + " 笔，成本合计或有低估" : ""),
                    now);
        }

        long expense = 0L;
        for (ExpenseTypeSumRecord typeSum : repository.sumExpenseByType(
                merchantId, period.months(), plan.expenseTypes())) {
            expense += typeSum.totalCent();
            addLine(lines, statementId, merchantId, seq, "expense_" + typeSum.expenseType(), -typeSum.totalCent(),
                    "期间费用（V297 费用归集台账净额，月份桶 " + String.join("、", period.months()) + "）", now);
        }

        if (ticketServiceFee > 0) {
            expense += ticketServiceFee;
            addLine(lines, statementId, merchantId, seq, "expense_ticket_service_fee", -ticketServiceFee,
                    "单一酒吧门票综合服务费（计费月＝售出月，整张退款成功剔除后按月封顶；计费月 "
                            + String.join("、", period.months()) + "）", now);
        }

        long netProfit = revenue - cost - expense;

        // V303 扣除项三开关（默认全关时逐分等于 V299 现状）。
        // 亏损结转累计恒结转：无论本期开关都读往期累计（关闭期不扣但不清零，用户 2026-07-19 拍板）。
        long carryIn = repository.priorLossCarryforwardCent(merchantId, period.start());
        List<FinanceService.DividendArrearsItem> arrearsItems = plan.arrearsOffsetEnabled()
                ? payoutPort.listOutstandingReceivables(merchantId) : List.of();
        long arrearsOutstanding = arrearsItems.stream()
                .mapToLong(FinanceService.DividendArrearsItem::unpaidCent).sum();

        // V308 主理人类型分支：公司股东不代扣个税（生效税率强制 0，全额发放）；个人股东按方案税率代扣。
        boolean isCompany = "company".equals(plan.principalType());
        int effectiveTaxBps = isCompany ? 0 : plan.taxRateBps();

        // V309 自定义扣除项：取启用项（仓储已按 sort_order 排序），逐项解析原始扣除额（fixed 固定额 / ratio 比例）。
        List<DividendRecords.DividendCustomDeductionRecord> customItems = plan.customDeductions().stream()
                .filter(DividendRecords.DividendCustomDeductionRecord::enabled).toList();
        long[] customRaws = new long[customItems.size()];
        for (int i = 0; i < customItems.size(); i++) {
            customRaws[i] = resolveDeductionRaw(customItems.get(i), netProfit, revenue);
        }

        // FS-014：正式单读财务登记的依法可分配金额（预览单不读），本次分配与经营口径取小。
        DividendRecords.DividendLegalConfirmationRecord legal = "official".equals(kind)
                ? repository.findLegalConfirmation(merchantId, period.label()).orElse(null) : null;
        Long legalCap = legal == null ? null : legal.legalDistributableCent();

        DividendMath.Distribution dist = DividendMath.compute(
                netProfit, plan.platformShareBps(), effectiveTaxBps,
                plan.lossCarryforwardEnabled(), carryIn,
                plan.arrearsOffsetEnabled(), arrearsOutstanding,
                customRaws,
                plan.payoutFloorEnabled(), plan.payoutFloorBps(), plan.payoutFloorFixedCent(), legalCap);

        addLine(lines, statementId, merchantId, seq, "net_profit", netProfit,
                "净利润 = 收入 − 销货成本 − 费用（亏损期可分配额为 0，不倒扣主理人）", now);
        if (dist.carryInCent() > 0 || dist.carryAfterCent() > 0) {
            // 累计恒结转留痕：开启期弥补（扣减可分配额）、关闭期仅结转不扣（累计保留）。
            String note = plan.lossCarryforwardEnabled()
                    ? "本期弥补 " + dist.carryAppliedCent() + " 分（扣减可分配额）"
                    : "本期未启用抵扣（累计保留、不扣减）";
            addLine(lines, statementId, merchantId, seq, "loss_carryforward", -dist.carryAppliedCent(),
                    "亏损结转：期初未弥补亏损累计 " + carryIn + " 分，" + note
                            + "，结转至下期 " + dist.carryAfterCent() + " 分", now);
        }
        if (legal != null) {
            long operating = Math.max(netProfit - dist.carryAppliedCent(), 0L);
            addLine(lines, statementId, merchantId, seq, "legal_cap", legal.legalDistributableCent(),
                    "依法可分配金额（财务核定）：财税核算依据「" + legal.taxAccountingRef() + "」，分配决议「"
                            + legal.resolutionRef() + "」" + (legal.note() == null ? "" : "，说明：" + legal.note())
                            + "；经营口径可分配额 " + operating + " 分，本次分配取二者较小 " + dist.distributableCent()
                            + " 分（已在核定额中扣除的事项不重复扣除；被压下部分留在公司，以后不单独补分）", now);
        }
        addLine(lines, statementId, merchantId, seq, "platform_share", dist.platformShareCent(),
                "平台分成（" + plan.platformShareBps() + " 万分比，四舍五入到分）", now);
        addLine(lines, statementId, merchantId, seq, "principal_gross", dist.principalGrossCent(),
                "主理人应得（可分配额 − 平台分成，余数归主理人）", now);
        addLine(lines, statementId, merchantId, seq, "tax_withheld", -dist.taxCent(),
                isCompany
                        ? "公司股东不代扣个税，按约定全额发放（代扣 0 分）"
                        : "代扣股息红利个税（个人真实股东口径，税率 " + effectiveTaxBps
                                + " 万分比，整除截断·对齐税局申报口径）", now);
        addLine(lines, statementId, merchantId, seq, "principal_net", dist.principalNetCent(),
                "税后应得 = 主理人应得 − 代扣税", now);
        if (plan.arrearsOffsetEnabled() && dist.arrearsOffsetCent() > 0) {
            // 逐项留痕：先欠先抵（arrearsItems 已按 created_at 升序），单据审批时按此冻结额逐项结算。
            long remaining = dist.arrearsOffsetCent();
            for (FinanceService.DividendArrearsItem item : arrearsItems) {
                if (remaining <= 0) {
                    break;
                }
                long applied = Math.min(remaining, item.unpaidCent());
                remaining -= applied;
                addLine(lines, statementId, merchantId, seq, "arrears_offset", -applied,
                        "欠款抵扣：应收 " + item.itemNo() + "（未清 " + item.unpaidCent() + " 分）抵扣 " + applied + " 分", now);
            }
        }
        // V309 自定义扣除项逐项留痕：与 DividendMath 同源 clampDeductions，对 (税后应得 − 欠款抵扣) 余额封顶扣减。
        if (!customItems.isEmpty()) {
            long[] customApplied = DividendMath.clampDeductions(
                    dist.principalNetCent() - dist.arrearsOffsetCent(), customRaws);
            for (int i = 0; i < customItems.size(); i++) {
                DividendRecords.DividendCustomDeductionRecord item = customItems.get(i);
                if (customApplied[i] <= 0 && customRaws[i] <= 0) {
                    continue;   // 原始额与实扣额皆 0（如比例基数亏损）：不落零额行，减噪。
                }
                addLine(lines, statementId, merchantId, seq, "custom_deduction", -customApplied[i],
                        "自定义扣除「" + item.name() + "」（" + deductionSourceLabel(item)
                                + "，原始 " + customRaws[i] + " 分，封顶实扣 " + customApplied[i] + " 分）", now);
            }
        }
        if (plan.payoutFloorEnabled() && dist.reserveWithheldCent() > 0) {
            addLine(lines, statementId, merchantId, seq, "reserve_withheld", -dist.reserveWithheldCent(),
                    "发放保护线留存风险准备金 " + dist.reserveWithheldCent() + " 分（比例 " + plan.payoutFloorBps()
                            + " 万分比 / 固定额 " + plan.payoutFloorFixedCent() + " 分，取大并钳到可发额），本期不发放", now);
        }
        addLine(lines, statementId, merchantId, seq, "payout", dist.payoutCent(),
                "实发额 = 税后应得 − 欠款抵扣 − 自定义扣除 − 留存；审批通过后按此额挂应付发放", now);

        // V308 快照生效税率（公司=0）与主理人类型；V309 快照自定义扣除合计（逐项留痕见明细行）。
        DividendStatementRecord statement = new DividendStatementRecord(
                statementId, merchantId, plan.id(), kind, period.label(), period.start(), period.end(),
                plan.principalUserId(), plan.platformShareBps(), effectiveTaxBps, plan.revenueDomains(),
                plan.includeInventoryCost(), plan.expenseTypes(),
                revenue, cost, expense, netProfit, dist.distributableCent(), dist.platformShareCent(),
                dist.principalGrossCent(), dist.taxCent(), dist.principalNetCent(), noCostBasisCount,
                "preview".equals(kind) ? "preview" : "draft",
                null, null, operator, null, null, null, null, null, null, now, now,
                plan.lossCarryforwardEnabled(), dist.carryInCent(), dist.carryAppliedCent(),
                dist.carryAfterCent(), dist.arrearsOffsetCent(), dist.reserveWithheldCent(), dist.payoutCent(),
                plan.principalType(), dist.customDeductionCent(), legalCap);
        repository.insertStatement(statement);
        for (DividendStatementLineRecord line : lines) {
            repository.insertLine(line);
        }
        return statement;
    }

    private static void addLine(List<DividendStatementLineRecord> lines, String statementId, String merchantId,
            int[] seq, String lineType, long amountCent, String detail, Instant now) {
        seq[0]++;
        lines.add(new DividendStatementLineRecord(
                statementId + "_" + seq[0], statementId, merchantId, seq[0], lineType, amountCent, detail, now));
    }

    /**
     * V309 自定义扣除项原始扣除额（分，未封顶）：fixed 取固定额；ratio 取<b>四舍五入</b>(基数（净利润/营收）× 比例)，
     * 基数按 max(figure,0) 取（亏损/负基数不倒扣）。封顶（不透支税后余额）由 {@link DividendMath#clampDeductions} 处理。
     *
     * <p>R-7（2026-07-28 裁决口径「分红改、个税不改」——自定义扣除在分红域、非代扣税，落在「改」的一侧）：
     * 旧 {@code basis * ratioBps / 10000} 整除截断使扣除方恒少扣、差额白留主理人且不留痕，改
     * {@link MoneyRounding#bpsOf} HALF_UP。<b>不影响守恒</b>：本值只决定「扣多少」的大小，
     * 链条结构不变（{@code afterCustom := afterArrears − custom} 仍取补数），
     * 故 {@code 平台分成 + 代扣个税 + 欠款抵扣 + 自定义扣除 + 风险准备金 + 主理人实得 ≡ 可分配额} 依旧逐分成立。
     */
    private static long resolveDeductionRaw(
            DividendRecords.DividendCustomDeductionRecord item, long netProfit, long revenue) {
        if ("fixed".equals(item.sourceType())) {
            return Math.max(item.amountCent(), 0L);
        }
        long basis = "revenue".equals(item.ratioBasis()) ? revenue : netProfit;
        return MoneyRounding.bpsOf(Math.max(basis, 0L), item.ratioBps());
    }

    /** 自定义扣除项来源的中文口径说明（供算式明细行 detail 留痕）。 */
    private static String deductionSourceLabel(DividendRecords.DividendCustomDeductionRecord item) {
        if ("fixed".equals(item.sourceType())) {
            return "固定额 " + item.amountCent() + " 分";
        }
        String basis = "revenue".equals(item.ratioBasis()) ? "营收" : "净利润";
        return "比例 " + item.ratioBps() + " 万分比 × " + basis + "（四舍五入到分）";
    }

    // ── 查询 ──────────────────────────────────────────────────

    /** 计算单分页。商户 scope 账号强制只看自己（商户专区主理人视图同入口）。 */
    public PageResponse<DividendStatementRecord> listStatements(
            String merchantId, String kind, String periodLabel, String status, int page, int pageSize) {
        authorizationService.requirePermission(VIEW_PERM);
        return operations.readPageResult(
                AuditedOperation.of("dividend.statements_listed", "dividend.statements_list_failed",
                        VIEW_PERM, STMT_TYPE),
                () -> {
                    if (kind != null && !kind.isBlank() && !KINDS.contains(kind)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "无法识别的类型：" + kind);
                    }
                    if (status != null && !status.isBlank() && !STATEMENT_STATUSES.contains(status)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "无法识别的状态：" + status);
                    }
                    int safePage = Math.max(page, 1);
                    int safeSize = Math.min(Math.max(pageSize, 1), 200);
                    PageResponse<DividendStatementRecord> pageResult = repository.pageStatements(
                            blankToNull(scopeMerchant(merchantId)), blankToNull(kind),
                            blankToNull(periodLabel), blankToNull(status), safePage, safeSize);
                    // S-3：商户 scope（主理人视图）不外发内部操作员 id 与应付款项 id（平台侧全量）。
                    Optional<String> scoped = authorizationService.currentMerchantScope();
                    if (scoped.isEmpty()) {
                        return pageResult;
                    }
                    if (merchantUnreadBills != null) {
                        merchantUnreadBills.markDividendsRead(scoped.get(), pageResult.items().stream()
                                .filter(s -> "approved".equals(s.status()) || "paid".equals(s.status()))
                                .map(DividendStatementRecord::id).toList());
                    }
                    return new PageResponse<>(
                            pageResult.items().stream().map(DividendStatementRecord::forMerchantView).toList(),
                            pageResult.page(), pageResult.pageSize(), pageResult.total());
                });
    }

    /** 计算单详情（头 + 算式明细行）。商户 scope 账号只能看本商户的单。 */
    public DividendStatementDetailRecord getStatement(String statementId) {
        authorizationService.requirePermission(VIEW_PERM);
        return operations.readSingle(
                AuditedOperation.of("dividend.statement_viewed", "dividend.statement_view_failed",
                        VIEW_PERM, STMT_TYPE),
                () -> {
                    DividendStatementRecord statement = requireStatement(statementId);
                    checkMerchantOwnership(statement.merchantId());
                    String scoped = authorizationService.currentMerchantScope().orElse(null);
                    // S-3：商户 scope（主理人视图）不外发内部操作员 id 与应付款项 id（平台侧全量）。
                    return new DividendStatementDetailRecord(
                            scoped == null ? statement : statement.forMerchantView(),
                            repository.listLines(statement.id(), scoped));
                },
                result -> statementId);
    }

    // ── 状态机 ────────────────────────────────────────────────

    /** 提交审阅：draft → pending_review。 */
    @Transactional
    public DividendStatementRecord submit(String statementId, DividendActionRequest request) {
        return transition(statementId, request, "submitDividendStatement",
                AuditedOperation.of("dividend.submitted", "dividend.submit_failed", MANAGE_PERM, STMT_TYPE),
                (statement, operator, now) -> {
                    requireStatus(statement, "draft", "只有草稿单可以提交审阅");
                    requireLegalAmount(statement);
                    requireTransition(repository.markSubmitted(statement.id(), operator, now), "提交审阅");
                });
    }

    /**
     * 审批通过：pending_review → approved；净发放额 > 0 时给主理人挂应付
     * （source_type='dividend_payout'，source_id=计算单 id，部分唯一索引防一单重挂）。
     * C-1：先挂应付、后带守卫置 approved——守卫 0 行（并发被驳回等）抛冲突，事务整体回滚，应付不落地。
     * I-1：职责分离——提交人不得审批自己提交的单（SELF_APPROVAL_FORBIDDEN，仓内补录审批同范式）。
     */
    @Transactional
    public DividendStatementRecord approve(String statementId, DividendActionRequest request) {
        return transition(statementId, request, "approveDividendStatement",
                AuditedOperation.of("dividend.approved", "dividend.approve_failed", MANAGE_PERM, STMT_TYPE),
                (statement, operator, now) -> {
                    requireStatus(statement, "pending_review", "只有待审批单可以审批通过");
                    if (operator != null && operator.equals(statement.submittedBy())) {
                        throw ApiException.forbidden("SELF_APPROVAL_FORBIDDEN",
                                "分红计算单的提交人不能审批自己提交的单；请由另一位审批人处理");
                    }
                    // V303 欠款抵扣：先按冻结抵扣额对该商户未清应收逐项结算（先欠先抵，dividend_offset 收款）。
                    // 实结 != 冻结额 = 抵扣期间应收被并发清偿——@Transactional 回滚，要求作废重算（快照冻结口径）。
                    if (statement.arrearsOffsetCent() > 0) {
                        long settled = payoutPort.applyDividendOffsetToReceivables(
                                statement.merchantId(), statement.arrearsOffsetCent(), statement.id());
                        if (settled != statement.arrearsOffsetCent()) {
                            throw ApiException.conflict("DIVIDEND_ARREARS_CHANGED",
                                    "欠款抵扣期间该商户未清应收发生变化（当前可抵 " + settled + " 分，单据冻结 "
                                            + statement.arrearsOffsetCent() + " 分）；请作废重算后再审批");
                        }
                    }
                    // 实发额（税后应得 − 欠款抵扣 − 留存）> 0 才给主理人挂应付（立即可付）。
                    String arApItemId = null;
                    if (statement.payoutCent() > 0) {
                        arApItemId = payoutPort.createDividendPayoutPayable(
                                statement.principalUserId(), statement.merchantId(),
                                statement.payoutCent(), statement.id());
                    }
                    // V303 发放保护线留存 → 记为欠主理人的待释放款（非平台白拿；释放为后续 R）。
                    if (statement.reserveWithheldCent() > 0) {
                        payoutPort.createDividendReserveWithholdingPayable(
                                statement.principalUserId(), statement.merchantId(),
                                statement.reserveWithheldCent(), statement.id());
                    }
                    requireTransition(repository.markApproved(statement.id(), operator, arApItemId, now), "审批通过");
                    // 审批通过这一步才挂应付、抵欠款（草稿与月度预览会重算作废，不发）。
                    notifyMerchantAmount(statement.merchantId(), "dividend_issued", statement.payoutCent(),
                            statement.periodLabel() + " 分红出单：税后应得 " + yuan(statement.principalNetCent()) + "，欠款抵扣 "
                                    + yuan(statement.arrearsOffsetCent()) + "，留存 " + yuan(statement.reserveWithheldCent()),
                            now, statement.id());
                });
    }

    /** 驳回：pending_review → rejected，理由必填；驳回后可重算（旧单作废+新单）。 */
    @Transactional
    public DividendStatementRecord reject(String statementId, DividendActionRequest request) {
        return transition(statementId, request, "rejectDividendStatement",
                AuditedOperation.of("dividend.rejected", "dividend.reject_failed", MANAGE_PERM, STMT_TYPE),
                (statement, operator, now) -> {
                    String reason = requireText(request.note(), "note");
                    requireStatus(statement, "pending_review", "只有待审批单可以驳回");
                    requireTransition(repository.markRejected(statement.id(), operator, reason, now), "驳回");
                });
    }

    /** 标记发放：approved → paid（实际打款走应收应付台账既有付款确认流程，本动作登记发放完成）。 */
    @Transactional
    public DividendStatementRecord markPaid(String statementId, DividendActionRequest request) {
        return transition(statementId, request, "markDividendStatementPaid",
                AuditedOperation.of("dividend.paid_marked", "dividend.mark_paid_failed", MANAGE_PERM, STMT_TYPE),
                (statement, operator, now) -> {
                    requireStatus(statement, "approved", "只有审批通过的单可以标记发放");
                    requireTransition(repository.markPaid(statement.id(), operator, now), "标记发放");
                    // 与出单同一张分红单的异议期（自审批通过之日次日起算）。
                    notifyMerchantAmount(statement.merchantId(), "dividend_paid", statement.payoutCent(),
                            statement.periodLabel() + " 分红发放", statement.approvedAt(), statement.id());
                });
    }

    /**
     * FS-014 财务登记依法可分配金额（BSM L1785「由财务依据专业财税核算结果及公司有效分配决议，确认依法可分配金额，
     * 本次分配不得超出该金额」）：仅正式草稿/已驳回单；登记后按 min(经营口径, 核定额) 重算（旧单作废 + 新草稿）。
     */
    @Transactional
    public DividendStatementRecord confirmLegal(String statementId, DividendLegalConfirmationRequest request) {
        authorizationService.requirePermission(MANAGE_PERM);
        authorizationService.requirePlatformScope();
        requireRequest(request);
        String id = requireText(statementId, "statement_id");
        return operations.executeWrite(
                "confirmDividendLegalAmount:" + id,
                request.idempotencyKey(),
                request,
                DividendStatementRecord.class,
                AuditedOperation.of("dividend.legal_confirmed", "dividend.legal_confirm_failed", MANAGE_PERM, STMT_TYPE),
                () -> confirmLegalInternal(id, request),
                DividendService::businessId,
                java.util.Map.of());
    }

    private DividendStatementRecord confirmLegalInternal(String id, DividendLegalConfirmationRequest request) {
        DividendStatementRecord statement = requireStatement(id);
        if ("preview".equals(statement.kind())) {
            throw ApiException.conflict("DIVIDEND_PREVIEW_NOT_APPROVABLE", "月度预览单只可查看，不登记依法可分配金额");
        }
        if (!"draft".equals(statement.status()) && !"rejected".equals(statement.status())) {
            throw ApiException.conflict("DIVIDEND_STATE_INVALID",
                    "只有草稿或已驳回的正式单可以登记依法可分配金额（当前状态：" + statement.status() + "）");
        }
        if (request.legalDistributableCent() == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "legal_distributable_cent is required");
        }
        String operator = currentOperator();
        repository.upsertLegalConfirmation(new DividendRecords.DividendLegalConfirmationRecord(
                "dlc_" + UUID.randomUUID().toString().replace("-", ""), statement.merchantId(), statement.periodLabel(),
                requireNonNegLong(request.legalDistributableCent(), "legal_distributable_cent"),
                requireText(request.taxAccountingRef(), "tax_accounting_ref"),
                requireText(request.resolutionRef(), "resolution_ref"),
                blankToNull(request.note()), operator, Instant.now()));
        DividendPlanRecord plan = repository.findPlanById(statement.planId())
                .orElseThrow(() -> ApiException.notFound("DIVIDEND_PLAN_NOT_FOUND", "分红方案不存在：" + statement.planId()));
        DividendComputeResultRecord result = runCompute(
                List.of(plan), "official", DividendPeriod.parse(statement.periodLabel()), operator);
        if (result.computed().isEmpty()) {
            throw ApiException.conflict("DIVIDEND_STATE_INVALID", result.skipped().get(0).reason());
        }
        return result.computed().get(0);
    }

    /** FS-014 正式分红应付款形成前须已登记依法可分配金额（BSM L1785；PRD §6）。 */
    private static void requireLegalAmount(DividendStatementRecord statement) {
        if (statement.legalDistributableCent() == null) {
            throw ApiException.conflict("DIVIDEND_LEGAL_AMOUNT_REQUIRED", "须先由财务登记依法可分配金额及分配决议");
        }
    }

    /** C-1：状态 UPDATE 守卫 0 行=并发下状态已被变更，抛业务冲突（409），事务回滚（不产生半截副作用）。 */
    private static void requireTransition(int updatedRows, String action) {
        if (updatedRows == 0) {
            throw ApiException.conflict("DIVIDEND_STATE_INVALID",
                    action + "失败：计算单状态已被并发变更，请刷新后重试");
        }
    }

    @FunctionalInterface
    private interface Transition {
        void apply(DividendStatementRecord statement, String operator, Instant now);
    }

    private DividendStatementRecord transition(
            String statementId, DividendActionRequest request, String operationId,
            AuditedOperation operation, Transition transition) {
        authorizationService.requirePermission(MANAGE_PERM);
        authorizationService.requirePlatformScope();
        requireRequest(request);
        String id = requireText(statementId, "statement_id");
        return operations.executeWrite(
                operationId + ":" + id,
                request.idempotencyKey(),
                request,
                DividendStatementRecord.class,
                operation,
                () -> {
                    DividendStatementRecord statement = requireStatement(id);
                    if ("preview".equals(statement.kind())) {
                        throw ApiException.conflict("DIVIDEND_PREVIEW_NOT_APPROVABLE",
                                "月度预览单只可查看，不可提交/审批/发放");
                    }
                    transition.apply(statement, currentOperator(), Instant.now());
                    return requireStatement(id);
                },
                DividendService::businessId,
                java.util.Map.of());
    }

    private DividendStatementRecord requireStatement(String id) {
        return repository.findStatementById(id)
                .orElseThrow(() -> ApiException.notFound("DIVIDEND_STATEMENT_NOT_FOUND",
                        "分红结算单不存在：" + id));
    }

    private static void requireStatus(DividendStatementRecord statement, String expected, String message) {
        if (!expected.equals(statement.status())) {
            throw ApiException.conflict("DIVIDEND_STATE_INVALID",
                    message + "（当前状态：" + statement.status() + "）");
        }
    }

    // ── V304 发放保护线留存·释放 ──────────────────────────────

    /**
     * 待释放/已释放留存台账分页（后台管理 + 商户专区共用）。商户 scope 账号强制只看自己。
     * releasedFilter：'pending'=仅待释放 / 'released'=仅已释放 / 空=全部。
     */
    public PageResponse<DividendReserveLedgerRecord> listReserveLedger(
            String merchantId, String releasedFilter, int page, int pageSize) {
        authorizationService.requirePermission(VIEW_PERM);
        return operations.readPageResult(
                AuditedOperation.of("dividend.reserve_ledger_listed", "dividend.reserve_ledger_list_failed",
                        VIEW_PERM, STMT_TYPE),
                () -> {
                    String filter = blankToNull(releasedFilter);
                    if (filter != null && !"pending".equals(filter) && !"released".equals(filter)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "无法识别的发放状态筛选：" + filter);
                    }
                    int safePage = Math.max(page, 1);
                    int safeSize = Math.min(Math.max(pageSize, 1), 200);
                    PageResponse<DividendReserveLedgerRecord> result = repository.pageReserveLedger(
                            blankToNull(scopeMerchant(merchantId)), filter, safePage, safeSize);
                    if (authorizationService.currentMerchantScope().isEmpty()) {
                        return result;
                    }
                    // 商户（主理人）视图脱敏：不外发内部操作员 id。
                    return new PageResponse<>(
                            result.items().stream().map(DividendReserveLedgerRecord::forMerchantView).toList(),
                            result.page(), result.pageSize(), result.total());
                });
    }

    /**
     * 手动释放某计算单的发放保护线留存：把待释放留存应付 re-tag 为已释放（金额=原留存额，全额一次），
     * 使主理人能真正拿到这笔钱。要求 finance.dividend.manage + 平台 scope；幂等（同一留存不重复释放）。
     */
    @Transactional
    public DividendReserveReleaseRecord releaseReserve(String statementId, DividendActionRequest request) {
        authorizationService.requirePermission(MANAGE_PERM);
        authorizationService.requirePlatformScope();
        requireRequest(request);
        String id = requireText(statementId, "statement_id");
        return operations.executeWrite(
                "releaseDividendReserve:" + id,
                request.idempotencyKey(),
                request,
                DividendReserveReleaseRecord.class,
                AuditedOperation.of("dividend.reserve_released", "dividend.reserve_release_failed",
                        MANAGE_PERM, STMT_TYPE),
                () -> releaseInternal(id, "manual", currentOperator(), Instant.now(), blankToNull(request.note())),
                DividendService::businessId,
                java.util.Map.of());
    }

    /**
     * 留存自动释放定时作业入口（{@link DividendReserveReleaseJob} 调用）：无请求上下文、系统操作人。
     * 遍历所有待释放留存候选（approved/paid + 有留存 + 未释放），按方案释放规则判到期（{@link
     * DividendReserveReleaseMath#isDue}）后释放。幂等：候选已排除已释放单 + re-tag/uq 双守卫，同期重跑不重复释放。
     * 无鉴权闸：定时线程无 SecurityContext，本方法只对内部作业开放（无 Controller 绑定）。
     */
    @Transactional
    public DividendReserveReleaseRunRecord runScheduledReserveRelease(LocalDate asOf) {
        List<DividendReserveReleaseRecord> released = new ArrayList<>();
        List<DividendComputeSkipRecord> skipped = new ArrayList<>();
        Instant now = Instant.now();
        for (DividendReserveReleaseCandidateRecord c : repository.listReserveReleaseCandidates()) {
            if (!DividendReserveReleaseMath.isDue(c.reserveReleaseMode(), c.periodEnd(),
                    c.reserveReleaseDelayPeriods(), c.periodType(), asOf)) {
                continue;   // 未到期（含 manual 手动模式恒不自动释放）：不释放。
            }
            try {
                released.add(releaseInternal(
                        c.statementId(), "scheduled", SYSTEM_RELEASE_OPERATOR, now, "定时到期自动释放"));
            } catch (ApiException e) {
                // 幂等兜底：并发下被手动释放等 → 记跳过，不中断整批。
                skipped.add(new DividendComputeSkipRecord(c.merchantId(), c.statementId() + "：" + e.getMessage()));
            }
        }
        return new DividendReserveReleaseRunRecord(released, skipped);
    }

    /** 释放核心：状态/留存校验 → finance re-tag 待释放→已释放（全额）→ 落释放台账（幂等）。 */
    private DividendReserveReleaseRecord releaseInternal(
            String statementId, String trigger, String operator, Instant now, String note) {
        DividendStatementRecord stmt = requireStatement(statementId);
        if (!"approved".equals(stmt.status()) && !"paid".equals(stmt.status())) {
            throw ApiException.conflict("DIVIDEND_STATE_INVALID",
                    "只有已审批/已发放的计算单才有留存可释放（当前状态：" + stmt.status() + "）");
        }
        if (stmt.reserveWithheldCent() <= 0) {
            throw ApiException.conflict("DIVIDEND_RESERVE_NONE", "该计算单没有发放保护线留存，无需释放");
        }
        // FS-014 BSM L1786「释放时核验原合法分配依据及本次支付依据」：本次支付依据随实际付款登记（凭证附件、银行流水）。
        if (stmt.legalDistributableCent() == null) {
            throw ApiException.conflict("DIVIDEND_LEGAL_BASIS_MISSING", "原分红单未登记依法可分配金额及分配决议，不能释放留存");
        }
        // finance 侧 re-tag 待释放留存应付 → 已释放（金额不变，全额一次）；已释放/无待释放应付 → empty。
        Optional<FinanceService.DividendReserveReleaseRef> ref =
                payoutPort.releaseDividendReserveWithholding(statementId);
        if (ref.isEmpty()) {
            throw ApiException.conflict("DIVIDEND_RESERVE_ALREADY_RELEASED",
                    "该计算单的留存已释放，不可重复释放");
        }
        // 释放额 = 留存额全额（不多不少，非平台白拿）；ratioBps=FULL 预留未来分期释放。
        long releasedCent = DividendReserveReleaseMath.releasableCent(
                ref.get().reserveCent(), DividendReserveReleaseMath.FULL_RATIO_BPS);
        DividendReserveReleaseRecord release = new DividendReserveReleaseRecord(
                "divrr_" + UUID.randomUUID().toString().replace("-", ""),
                statementId, stmt.merchantId(), stmt.principalUserId(), releasedCent,
                ref.get().arApItemId(), trigger, operator, now, note);
        if (!repository.insertReserveRelease(release)) {
            // uq(statement_id) 冲突（并发兜底）：与 re-tag 守卫双保险；@Transactional 回滚。
            throw ApiException.conflict("DIVIDEND_RESERVE_ALREADY_RELEASED",
                    "该计算单的留存已释放，不可重复释放");
        }
        // 留存部分没有单独的标记发放动作，释放即其发放点。
        notifyMerchantAmount(stmt.merchantId(), "dividend_paid", releasedCent, stmt.periodLabel() + " 分红留存释放发放", now,
                statementId + ":reserve");
        return release;
    }

    // ── 辅助 ──────────────────────────────────────────────────

    private String scopeMerchant(String requested) {
        return authorizationService.currentMerchantScope().orElse(requested);
    }

    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "无权操作其他商户的数据");
        }
    }

    private String currentOperator() {
        return authorizationService.currentActor().id();
    }

    private static String businessId(Object saved) {
        if (saved instanceof DividendPlanRecord plan) {
            return plan.id();
        }
        if (saved instanceof DividendStatementRecord statement) {
            return statement.id();
        }
        if (saved instanceof DividendComputeResultRecord result) {
            return result.kind() + ":" + result.periodLabel();
        }
        if (saved instanceof DividendReserveReleaseRecord release) {
            return release.id();
        }
        return "all";
    }

    private static void requireRequest(Object request) {
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static <T> T orDefault(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int requireBps(int value, String field) {
        if (value < 0 || value > DividendMath.BPS_DENOMINATOR) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须在 0 到 10000 之间");
        }
        return value;
    }

    private static long requireNonNegLong(long value, String field) {
        if (value < 0) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须大于等于 0");
        }
        return value;
    }

    private static int requirePositiveInt(int value, String field) {
        if (value < 1) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须大于等于 1");
        }
        return value;
    }

    private static String requireEnum(String value, Set<String> allowed, String field) {
        String trimmed = requireText(value, field);
        if (!allowed.contains(trimmed)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "无法识别的 " + field + "：" + trimmed);
        }
        return trimmed;
    }

    private static List<String> requireTokens(List<String> values, Set<String> allowed, String field) {
        if (values == null || values.isEmpty()) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 不能为空");
        }
        LinkedHashSet<String> distinct = new LinkedHashSet<>();
        for (String value : values) {
            String token = requireText(value, field);
            if (!allowed.contains(token)) {
                throw ApiException.badRequest("VALIDATION_FAILED", "无法识别的 " + field + " 取值：" + token);
            }
            distinct.add(token);
        }
        return List.copyOf(distinct);
    }

    private static LocalDate parseDate(String value, String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " 必须为日期格式（yyyy-MM-dd）");
        }
    }

    private static String stableToken(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
    }
}
