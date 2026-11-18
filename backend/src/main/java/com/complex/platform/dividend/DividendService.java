package com.complex.platform.dividend;
// 文件版本：V0.12

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.money.MoneyRounding;
import com.complex.platform.dividend.DividendRecords.DividendComputeResultRecord;
import com.complex.platform.dividend.DividendRecords.DividendComputeSkipRecord;
import com.complex.platform.dividend.DividendRecords.DividendPeriodHoldRecord;
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
import com.complex.platform.dividend.dto.DividendHoldRequest;
import com.complex.platform.dividend.dto.DividendLegalConfirmationRequest;
import com.complex.platform.dividend.dto.DividendPlanRequest;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.EntityTaxService;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.governance.review.SupervisorReviewDecisionRecord;
import com.complex.platform.governance.review.SupervisorReviewEvaluateRequest;
import com.complex.platform.inventory.InventoryCostSummaryRecord;
import com.complex.platform.inventory.InventoryRepository;
import com.complex.platform.settlement.InternalPayDayService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import static com.complex.platform.common.Strings.requireText;

/**
 * 持股分红引擎（2026-07-18 拍板：净利润制 / 真实股东股息红利 20% 代扣 / 季度正式+月度预览）。
 *
 * <p>算式（整数分，恒等式由 V299 DB CHECK 硬保证）：
 * 期间收入（复用 revenue.net 商户行级谓词区间版：酒吧实收−酒吧退款 + 已确认月度分账单的已核销门票结算额（FS-015），计入域可配）
 * − 期间销货成本（V296 sumOutboundCost 同谓词，可配开关；经理已确认坏账的酒吧订单出库成本另列冲回不计，第十八批裁决 38）
 * − 期间费用（V297 费用归集台账净额，计入类别可配）
 * = 净利润 → 本次分配 = min(经营口径可分配额, 财务登记的依法可分配金额)（FS-014，正式单未登记不得提交/审批）
 * → 按持股比例劈分（平台、各其他股东与主理人各自四舍五入，尾差单列） → 主理人应得 → 代扣股息红利个税（税率可配，默认 20%，
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
 *
 * <p>234-R1 方案版本化（BSM:521）：dividend_plans 每行是一个版本，新版本经最高权限审批后自生效起始日起用（最高权限本人提交直接生效）；
 * 每期出单取该期期末日已生效的版本，计算单 plan_id 固化所用版本。
 *
 * <p>234-R2 其他股东（BSM:1775「娱乐平台公司、主理人和其他股东按每家酒吧工商登记的真实持股比例分配」）：名单挂在方案版本上；平台、各其他股东与主理人
 * 各自四舍五入、尾差单列由该酒吧公司承担（BSM:1757）；个人股东按方案税率代扣、公司股东不代扣（BSM:1776）；审批通过后每位其他股东另挂一笔分红应付，
 * 全部付清才能标记发放（BSM:1787、BSM:1803）。
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
    /** 234-R1 分红方案调整的审批动作（统一审批策略表 top_authority；动作名以 finance. 开头按涉钱出厂 1 个工作日）。 */
    static final String PLAN_CHANGE_ACTION = "finance.dividend.plan.change";
    /** 234-R1 已批准版本：active＝自生效起始日起照此出单，ended＝自生效起始日起停用。 */
    private static final Set<String> APPROVED_PLAN_STATUSES = Set.of("active", "ended");
    private static final java.util.Map<String, String> PERIOD_TYPE_LABELS = java.util.Map.of(
            "monthly", "月度", "quarterly", "季度", "semiannual", "半年", "annual", "年度");
    /** 244-R1 分红周期上下限中文名（按月数，与周期同一粒度）。 */
    private static final java.util.Map<Integer, String> PERIOD_BOUND_LABELS = java.util.Map.of(1, "月", 3, "季", 6, "半年", 12, "年");
    private static final java.util.Map<String, String> RELEASE_MODE_LABELS = java.util.Map.of(
            "manual", "手动释放", "next_period", "下期返还", "fixed_delay", "固定延后", "annual", "年度末清算");
    /** 234-R1 审批摘要逐项（改前 → 改后）。 */
    private static final List<java.util.Map.Entry<String, java.util.function.Function<DividendPlanRecord, String>>> SUMMARY_FIELDS = List.of(
            java.util.Map.entry("平台比例", plan -> percent(plan.platformShareBps())),
            java.util.Map.entry("其他股东", DividendService::shareholdersText),
            java.util.Map.entry("主理人账户", DividendPlanRecord::principalUserId),
            java.util.Map.entry("主理人类型", plan -> "company".equals(plan.principalType()) ? "公司股东" : "个人股东"),
            java.util.Map.entry("代扣税率", plan -> percent(plan.taxRateBps())),
            java.util.Map.entry("分红周期", plan -> PERIOD_TYPE_LABELS.get(plan.periodType()) + "（上下限 "
                    + PERIOD_BOUND_LABELS.get(plan.periodMinMonths()) + "～" + PERIOD_BOUND_LABELS.get(plan.periodMaxMonths()) + "）"),
            java.util.Map.entry("发放付账日", plan -> "第 " + plan.payoutPaySlot() + " 个付账日"),
            java.util.Map.entry("亏损结转", plan -> plan.lossCarryforwardEnabled() ? "开" : "关"),
            java.util.Map.entry("欠款抵扣", plan -> plan.arrearsOffsetEnabled() ? "开" : "关"),
            java.util.Map.entry("发放保护线", plan -> plan.payoutFloorEnabled()
                    ? "开（留存 " + percent(plan.payoutFloorBps()) + " 或 " + yuan(plan.payoutFloorFixedCent()) + " 取大，"
                            + RELEASE_MODE_LABELS.get(plan.reserveReleaseMode()) + "）" : "关"),
            java.util.Map.entry("本版本自生效起始日起", plan -> plan.stopRequested() || "ended".equals(plan.status())
                    ? "停用（不再出单）" : "继续出单"));
    private static final Set<String> KINDS = Set.of("official", "preview");
    private static final Set<String> STATEMENT_STATUSES = Set.of(
            "preview", "draft", "pending_review", "approved", "paid", "rejected", "voided");
    /** 207-R1 PGM:287「状态……报错……必须全部为中文」：跳过原因与报错里的计算单状态中文名，与前端 DIVIDEND_STATUS_LABELS 同名。 */
    private static final java.util.Map<String, String> STATEMENT_STATUS_LABELS = java.util.Map.of(
            "preview", "预览（只读）", "draft", "草稿", "pending_review", "待审批", "approved", "已审批",
            "paid", "已发放", "rejected", "已驳回", "voided", "已作废");
    /** 可重算（作废+重出）的状态；已进入审批流的单不重算。 */
    private static final Set<String> RECOMPUTABLE_STATUSES = Set.of("preview", "draft", "rejected");
    /** 211-R1 整期暂不分红／暂缓分红登记类别中文名（第十八批裁决 43）。 */
    private static final java.util.Map<String, String> HOLD_LABELS = java.util.Map.of("skip", "暂不分红", "defer", "暂缓分红");
    private static final String HOLD_OBJECT = "dividend_period_hold";
    /** 月度预览自动生成（159-R6 月末营业日五类标齐后）的系统操作人标识（无请求上下文，落 computed_by/voided_by 便于留痕追溯）。 */
    static final String SYSTEM_OPERATOR = "system_dividend_preview";
    /** V304 留存自动释放定时作业的系统操作人标识（落 released_by 便于留痕追溯）。 */
    static final String SYSTEM_RELEASE_OPERATOR = "system_dividend_reserve_release";
    /** V304 留存释放规则可选值；默认手动=不自动释放（最安全，每笔人工决策）。 */
    static final Set<String> RESERVE_RELEASE_MODES = Set.of("manual", "next_period", "fixed_delay", "annual");
    private static final String DEFAULT_RESERVE_RELEASE_MODE = "manual";
    /** V308 主理人类型：individual 个人（代扣股息红利个税）/ company 公司（不代扣、全额发放）。 */
    static final Set<String> PRINCIPAL_TYPES = Set.of("individual", "company");
    /** 234-R2 其他股东类型中文名（与主理人类型同取值：个人股东代扣、公司股东不代扣）。 */
    private static final java.util.Map<String, String> SHAREHOLDER_TYPE_LABELS = java.util.Map.of("individual", "个人股东", "company", "公司股东");
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
    private final InternalPayDayService payDays;

    // 第十八批第 13 条（BSM 第 1597 行「分红出单与发放」，第 1598 行不可关闭）：业务提交后通知商户。@Lazy 防初始化环；为 null（单测手工构造）＝不发。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    com.complex.platform.merchantbilling.MerchantAmountChangeNotifier merchantAmountNotifier;

    // 第 92 条（BSM:1597 首页未读账单提醒）：商户账号在我的分红读到审批通过后的分红单即记已读。@Lazy 防初始化环；为 null（单测手工构造）＝不记。
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    com.complex.platform.merchantportal.MerchantUnreadBillService merchantUnreadBills;

    // 234-R1（PGM:666「持股分红的方案配置……由最高权限审批」）：方案新版本进统一审批。@Lazy 防初始化环；手工构造的单测走非最高权限提交时须赋桩。
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    com.complex.platform.governance.review.SupervisorReviewService supervisorReviews;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    com.complex.platform.governance.review.SupervisorReviewRepository reviewRepository;

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
            FinanceService payoutPort,
            InternalPayDayService payDays) {
        this.repository = repository;
        this.inventoryRepository = inventoryRepository;
        this.businessDayResolver = businessDayResolver;
        this.authorizationService = authorizationService;
        this.operations = operations;
        this.payoutPort = payoutPort;
        this.payDays = payDays;
    }

    // ── 方案配置 ──────────────────────────────────────────────

    /** 方案列表（全部版本，新建在前）。商户 scope 账号强制只看自己（覆盖请求参数）。 */
    public List<DividendPlanRecord> listPlans(String merchantId) {
        authorizationService.requirePermission(VIEW_PERM);
        return operations.readSingle(
                AuditedOperation.of("dividend.plans_listed", "dividend.plans_list_failed", VIEW_PERM, PLAN_TYPE),
                () -> withVersionStates(repository.listPlans(blankToNull(scopeMerchant(merchantId)))),
                result -> "all");
    }

    /**
     * 234-R1：按各商户当前营业日给每个版本填版本状态——pending 待审批、rejected 已驳回、expired 审批时已过生效日、scheduled 已批准待生效、
     * in_force 生效起始日不晚于今天的已批准版本里最新的一个（status 为 ended 即已停用）、superseded 已被新版本取代。
     */
    private List<DividendPlanRecord> withVersionStates(List<DividendPlanRecord> plans) {
        Instant now = Instant.now();
        java.util.Map<String, LocalDate> today = new java.util.HashMap<>();
        java.util.Map<String, LocalDate> inForce = new java.util.HashMap<>();
        for (DividendPlanRecord plan : plans) {
            LocalDate date = today.computeIfAbsent(plan.merchantId(), m -> businessDayResolver.merchantBusinessDate(null, m, now));
            if (APPROVED_PLAN_STATUSES.contains(plan.status()) && !plan.effectiveFrom().isAfter(date)) {
                inForce.merge(plan.merchantId(), plan.effectiveFrom(), (a, b) -> a.isAfter(b) ? a : b);
            }
        }
        return plans.stream().map(plan -> plan.withVersionState(switch (plan.status()) {
            case "pending_review" -> "pending";
            case "rejected", "expired" -> plan.status();
            default -> plan.effectiveFrom().isAfter(today.get(plan.merchantId())) ? "scheduled"
                    : plan.effectiveFrom().equals(inForce.get(plan.merchantId())) ? "in_force" : "superseded";
        })).toList();
    }

    /** 新建分红方案：提交该商户的第一个版本（已有已批准或待审批版本的商户 409）；空缺字段按默认口径补齐。 */
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
                () -> submitVersion(null, request),
                DividendService::businessId,
                java.util.Map.of());
    }

    /** 调整方案（比例/税率/周期/主理人/扣除项/停用）：在已批准版本上提交新版本；历史分红单按单内所记版本追溯。 */
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
                () -> submitVersion(id, request),
                DividendService::businessId,
                java.util.Map.of());
    }

    /**
     * 第十八批 AQ 段第 234 条 234-R1（BSM:521「每次改动生成新版本，必须填写生效起始日期，并先存为草案、经高敏权限“转正”审批通过后按指定日期生效，
     * 不即改即生效」；BSM:1775「必须以获批登记版本生效」；PGM:666「持股分红的方案配置……由最高权限审批，提交人不能审批自己的单据」；PGM:899「为什么……
     * 都要能查到」；BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批」）：新建与修改都落一个新版本行，取值＝改前版本（新建取默认口径）
     * 各字段叠加请求里非空的字段，自定义扣除项没给就照改前版本复制。生效起始日须晚于该商户当前营业日，理由必填。最高权限本人提交直接落已批准版本；
     * 其他人提交落待审批版本并进最高权限审批（结果见 {@link #onPlanChangeDecided}）。
     */
    private DividendPlanRecord submitVersion(String basePlanId, DividendPlanRequest request) {
        DividendPlanRecord base = null;
        if (basePlanId != null) {
            base = repository.findPlanById(basePlanId)
                    .orElseThrow(() -> ApiException.notFound("DIVIDEND_PLAN_NOT_FOUND", "分红方案不存在：" + basePlanId));
            if (request.merchantId() != null && !request.merchantId().isBlank()
                    && !base.merchantId().equals(request.merchantId().trim())) {
                throw ApiException.badRequest("VALIDATION_FAILED", "分红方案的所属商户不能变更");
            }
        }
        requireFullDeductions(request);
        String merchantId = base != null ? base.merchantId() : requireText(request.merchantId(), "merchant_id");
        DividendPlanRecord prev = base != null ? base : new DividendPlanRecord(null, merchantId, null, null,
                DEFAULT_PLATFORM_SHARE_BPS, DEFAULT_TAX_RATE_BPS, DEFAULT_PERIOD_TYPE, DEFAULT_REVENUE_DOMAINS, true,
                DEFAULT_EXPENSE_TYPES, null, "active", null, null, null, null, false, false, false, 0, 0L,
                DEFAULT_RESERVE_RELEASE_MODE, 1, DEFAULT_PRINCIPAL_TYPE, List.of(), null);
        String id = "divp_" + stableToken(request.idempotencyKey());
        String principalUserId = requireText(orDefault(request.principalUserId(), prev.principalUserId()), "principal_user_id");
        int platformShareBps = requireBps(orDefault(request.platformShareBps(), prev.platformShareBps()), "platform_share_bps");
        int taxRateBps = requireBps(orDefault(request.taxRateBps(), prev.taxRateBps()), "tax_rate_bps");
        String periodType = requireEnum(orDefault(request.periodType(), prev.periodType()), PERIOD_TYPES, "period_type");
        List<String> revenueDomains = requireTokens(orDefault(request.revenueDomains(), prev.revenueDomains()), REVENUE_DOMAINS,
                "revenue_domains");
        List<String> expenseTypes = requireTokens(orDefault(request.expenseTypes(), prev.expenseTypes()), EXPENSE_TYPES,
                "expense_types");
        int payoutFloorBps = requireBps(orDefault(request.payoutFloorBps(), prev.payoutFloorBps()), "payout_floor_bps");
        long payoutFloorFixedCent = requireNonNegLong(orDefault(request.payoutFloorFixedCent(), prev.payoutFloorFixedCent()),
                "payout_floor_fixed_cent");
        String reserveReleaseMode = requireEnum(orDefault(request.reserveReleaseMode(), prev.reserveReleaseMode()),
                RESERVE_RELEASE_MODES, "reserve_release_mode");
        int reserveReleaseDelayPeriods = requirePositiveInt(
                orDefault(request.reserveReleaseDelayPeriods(), prev.reserveReleaseDelayPeriods()), "reserve_release_delay_periods");
        String principalType = requireEnum(orDefault(request.principalType(), prev.principalType()), PRINCIPAL_TYPES, "principal_type");
        Integer payoutPaySlot = requirePaySlot(orDefault(request.payoutPaySlot(), prev.payoutPaySlot()));
        Integer periodMinMonths = orDefault(request.periodMinMonths(), prev.periodMinMonths());
        Integer periodMaxMonths = orDefault(request.periodMaxMonths(), prev.periodMaxMonths());
        requirePeriodBounds(periodType, periodMinMonths, periodMaxMonths);
        String status = requireEnum(orDefault(request.status(), prev.status()), PLAN_STATUSES, "status");
        // V309 自定义扣除项：请求给了按请求落；没给照改前版本逐项复制到新版本名下（主键重新取）。
        List<DividendRecords.DividendCustomDeductionRecord> deductions = request.customDeductions() != null
                ? parseCustomDeductions(id, request.customDeductions())
                : prev.customDeductions().stream().map(item -> new DividendRecords.DividendCustomDeductionRecord(
                        "divd_" + UUID.randomUUID().toString().replace("-", ""), id, item.name(), item.sourceType(),
                        item.amountCent(), item.ratioBps(), item.ratioBasis(), item.enabled(), item.sortOrder())).toList();
        // 234-R2 其他股东：请求给了按请求整份落新版本；没给照改前版本逐项复制（主键重新取）。平台与其他股东合计不超过 100%，主理人为余下部分。
        List<DividendRecords.DividendShareholderRecord> shareholders = request.otherShareholders() != null
                ? parseShareholders(id, request.otherShareholders())
                : prev.otherShareholders().stream().map(item -> new DividendRecords.DividendShareholderRecord(
                        "dps_" + UUID.randomUUID().toString().replace("-", ""), id, item.shareholderName(), item.shareholderType(),
                        item.shareBps(), item.sortOrder())).toList();
        if (platformShareBps + shareholders.stream().mapToInt(DividendRecords.DividendShareholderRecord::shareBps).sum()
                > DividendMath.BPS_DENOMINATOR) {
            throw ApiException.badRequest("VALIDATION_FAILED", "平台与其他股东持股比例合计不能超过 100%（主理人比例为余下部分）");
        }
        LocalDate effectiveFrom = parseDate(requireText(request.effectiveFrom(), "effective_from"), "effective_from");
        Instant now = Instant.now();
        if (!effectiveFrom.isAfter(businessDayResolver.merchantBusinessDate(null, merchantId, now))) {
            throw ApiException.badRequest("VALIDATION_FAILED", "生效起始日期须晚于今天（按营业日）");
        }
        String reason = requireText(request.reason(), "reason");

        List<DividendPlanRecord> versions = repository.listPlans(merchantId);
        if (base == null && versions.stream().anyMatch(plan ->
                APPROVED_PLAN_STATUSES.contains(plan.status()) || "pending_review".equals(plan.status()))) {
            throw ApiException.conflict("DIVIDEND_PLAN_EXISTS", "该商户已有分红方案，请在现有方案上提交新版本");
        }
        if (base != null && !APPROVED_PLAN_STATUSES.contains(base.status())) {
            throw ApiException.conflict("DIVIDEND_PLAN_NOT_EDITABLE", "只能在已批准的方案版本上提交新版本");
        }
        if (versions.stream().anyMatch(plan -> "pending_review".equals(plan.status()))) {
            throw pendingExists();
        }
        if (versions.stream().anyMatch(plan -> APPROVED_PLAN_STATUSES.contains(plan.status()) && effectiveFrom.equals(plan.effectiveFrom()))) {
            throw versionConflict();
        }

        boolean master = authorizationService.isMasterAuthority(authorizationService.currentActor());
        String operator = currentOperator();
        DividendPlanRecord plan = new DividendPlanRecord(id, merchantId, principalUserId,
                request.principalName() == null ? prev.principalName() : blankToNull(request.principalName()),
                platformShareBps, taxRateBps, periodType, revenueDomains,
                request.includeInventoryCost() == null ? prev.includeInventoryCost() : request.includeInventoryCost(),
                expenseTypes, effectiveFrom, master ? status : "pending_review",
                request.note() == null ? prev.note() : blankToNull(request.note()), operator, now, now,
                orDefault(request.lossCarryforwardEnabled(), prev.lossCarryforwardEnabled()),
                orDefault(request.arrearsOffsetEnabled(), prev.arrearsOffsetEnabled()),
                orDefault(request.payoutFloorEnabled(), prev.payoutFloorEnabled()),
                payoutFloorBps, payoutFloorFixedCent, reserveReleaseMode, reserveReleaseDelayPeriods, principalType,
                deductions, payoutPaySlot, "ended".equals(status), reason, null,
                master ? operator : null, master ? now : null, null, periodMinMonths, periodMaxMonths, shareholders);
        if (!repository.insertPlan(plan)) {
            throw base == null ? ApiException.conflict("DIVIDEND_PLAN_EXISTS", "该商户已有分红方案，请在现有方案上提交新版本")
                    : pendingExists();
        }
        repository.replaceCustomDeductions(id, deductions);
        if (!master) {
            String summary = planChangeSummary(base, plan);
            SupervisorReviewDecisionRecord decision = supervisorReviews.evaluate(new SupervisorReviewEvaluateRequest(
                    request.idempotencyKey() + ":review", summary, List.of(), PLAN_CHANGE_ACTION, PLAN_TYPE, id, 0L, summary,
                    null, List.of()));
            repository.setPlanReviewId(id, decision.reviewId());
        }
        return repository.findPlanById(id)
                .orElseThrow(() -> ApiException.notFound("DIVIDEND_PLAN_NOT_FOUND", "分红方案已被并发修改，请刷新后重试"));
    }

    /**
     * 234-R1 审批结果（照 BarBusinessDayService.decided）：先核复核表持久化终态与对象一致，只改复核单号相符的待审批版本。驳回落 rejected；通过时生效起始日
     * 已不晚于该商户当前营业日的落 expired（BSM:521「审批通过后按指定日期生效」，须重新提交）；同商户同一生效起始日已有已批准版本 409 整笔回滚；其余按是否
     * 申请停用落 ended 或 active。提交人不能审批自己的单由统一审批的自审闸负责（PGM:666）。
     */
    @EventListener
    @Transactional
    public void onPlanChangeDecided(SupervisorReviewDecisionRecord decision) {
        if (!PLAN_TYPE.equals(decision.objectType()) || !PLAN_CHANGE_ACTION.equals(decision.actionType())
                || !List.of("approved", "rejected").contains(decision.reviewStatus())) {
            return;
        }
        SupervisorReviewDecisionRecord persisted = reviewRepository.findSupervisorReviewById(decision.reviewId()).orElse(null);
        if (persisted == null || !Objects.equals(persisted.objectId(), decision.objectId())
                || !Objects.equals(persisted.reviewStatus(), decision.reviewStatus())) {
            return;
        }
        DividendPlanRecord plan = repository.findPlanById(decision.objectId()).orElse(null);
        if (plan == null || !"pending_review".equals(plan.status()) || !Objects.equals(plan.reviewId(), decision.reviewId())) {
            return;
        }
        Instant now = Instant.now();
        String status = "rejected";
        if ("approved".equals(decision.reviewStatus())) {
            if (!plan.effectiveFrom().isAfter(businessDayResolver.merchantBusinessDate(null, plan.merchantId(), now))) {
                status = "expired";
            } else if (repository.listPlans(plan.merchantId()).stream().anyMatch(other ->
                    APPROVED_PLAN_STATUSES.contains(other.status()) && plan.effectiveFrom().equals(other.effectiveFrom()))) {
                throw versionConflict();
            } else {
                status = plan.stopRequested() ? "ended" : "active";
            }
        }
        repository.decidePlan(plan.id(), decision.reviewId(), status, currentOperator(), now);
    }

    private static ApiException pendingExists() {
        return ApiException.conflict("DIVIDEND_PLAN_PENDING_EXISTS", "该商户已有待审批的方案版本，请等审批结束后再提交");
    }

    private static ApiException versionConflict() {
        return ApiException.conflict("DIVIDEND_PLAN_VERSION_CONFLICT", "该商户这一生效起始日已有已批准的方案版本，请换一个日期");
    }

    /** 234-R1 审批摘要（PGM:899）：理由、商户、生效起始日与改前到改后的变化，审批人在复核单里看。 */
    private static String planChangeSummary(DividendPlanRecord before, DividendPlanRecord after) {
        StringBuilder text = new StringBuilder("调整理由：" + after.reason() + "\n商户：" + after.merchantId()
                + "\n生效起始日：" + after.effectiveFrom());
        for (java.util.Map.Entry<String, java.util.function.Function<DividendPlanRecord, String>> field : SUMMARY_FIELDS) {
            text.append('\n').append(field.getKey()).append('：')
                    .append(before == null ? "（新建）" : field.getValue().apply(before)).append(" → ").append(field.getValue().apply(after));
        }
        return text.toString();
    }

    private static String percent(int bps) {
        return java.math.BigDecimal.valueOf(bps, 2).stripTrailingZeros().toPlainString() + "%";
    }

    /**
     * 控制席并入（BSM:1774「分红基数按「净利润」算：一个周期里的经营收入（按含税实收口径），减掉这期间的全部成本、费用与税费之后的余额」）：
     * 方案不能关掉销货成本或去掉任一类费用；不填按全量（新建）或现值（修改）。
     */
    private static void requireFullDeductions(DividendPlanRequest request) {
        if (Boolean.FALSE.equals(request.includeInventoryCost())) {
            throw ApiException.badRequest("VALIDATION_FAILED", "分红净利润须减掉期间全部成本、费用与税费，不能关闭扣减销货成本");
        }
        if (request.expenseTypes() != null
                && !Set.copyOf(requireTokens(request.expenseTypes(), EXPENSE_TYPES, "expense_types")).equals(EXPENSE_TYPES)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "分红净利润须减掉期间全部成本、费用与税费，计入的费用类别须为全部类别，不能去掉任一类费用");
        }
    }

    /** 211-R2 发放付账日（BSM:1787「落在三个付账日之一，具体哪一个由该公司在分红周期配置中指定」）：全平台统一每月三个付账日中的第几个，必填 1～3。 */
    private static Integer requirePaySlot(Integer slot) {
        if (slot == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payout_pay_slot is required");
        }
        if (slot < 1 || slot > 3) {
            throw ApiException.badRequest("VALIDATION_FAILED", "payout_pay_slot 须为第一、第二、第三个付账日之一（1～3）");
        }
        return slot;
    }

    /**
     * 第十八批 AQ 段第 244 条 244-R1（ENUM:299「分红周期可配置（业务参数，按经营主体可配，酒吧商户作为独立经营主体同样适用，须同时配置上下限）」；
     * BSM:521「超出该区间的配置一律拒绝生效，未填写上下限的参数不得上线」）：上下限按月数、与周期同一粒度（月、季、半年、年＝1、3、6、12），
     * 两个都要填，下限不大于上限，分红周期须落在上下限之内。不预置出厂数值（DEC-R3-2026-08-04-30「具体数值留到落地时逐个定」）。
     */
    private static void requirePeriodBounds(String periodType, Integer min, Integer max) {
        if (min == null || max == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "分红周期须同时填写上下限（月、季、半年、年）");
        }
        if (!PERIOD_BOUND_LABELS.containsKey(min) || !PERIOD_BOUND_LABELS.containsKey(max) || min > max) {
            throw ApiException.badRequest("VALIDATION_FAILED", "分红周期上下限须取月、季、半年、年之一，且下限不大于上限");
        }
        int months = DividendPeriod.MONTHS_PER_PERIOD.get(periodType);
        if (months < min || months > max) {
            throw ApiException.badRequest("VALIDATION_FAILED", "分红周期须在所配上下限之内");
        }
    }

    /** 234-R2 其他股东请求项 → 校验 + 落地记录（sort_order＝输入序）：名称必填，类型个人股东或公司股东，持股比例 1～10000 万分比。 */
    private static List<DividendRecords.DividendShareholderRecord> parseShareholders(
            String planId, List<DividendPlanRequest.OtherShareholderItem> items) {
        List<DividendRecords.DividendShareholderRecord> parsed = new ArrayList<>();
        for (DividendPlanRequest.OtherShareholderItem item : items) {
            if (item == null) {
                throw ApiException.badRequest("VALIDATION_FAILED", "其他股东不能为空");
            }
            String name = requireText(item.shareholderName(), "other_shareholders.shareholder_name");
            String type = requireEnum(item.shareholderType(), PRINCIPAL_TYPES, "other_shareholders.shareholder_type");
            if (item.shareBps() == null || item.shareBps() < 1 || item.shareBps() > DividendMath.BPS_DENOMINATOR) {
                throw ApiException.badRequest("VALIDATION_FAILED", "other_shareholders.share_bps 必须在 1 到 10000 之间");
            }
            parsed.add(new DividendRecords.DividendShareholderRecord(
                    "dps_" + UUID.randomUUID().toString().replace("-", ""), planId, name, type, item.shareBps(), parsed.size()));
        }
        return parsed;
    }

    /** 234-R2 其他股东名单中文摘要（审批摘要改前、改后两份名单）。 */
    private static String shareholdersText(DividendPlanRecord plan) {
        return plan.otherShareholders().isEmpty() ? "无" : plan.otherShareholders().stream()
                .map(item -> item.shareholderName() + "（" + SHAREHOLDER_TYPE_LABELS.get(item.shareholderType()) + " "
                        + percent(item.shareBps()) + "）")
                .collect(java.util.stream.Collectors.joining("、"));
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
                    "月度预览只支持月份期间标签（如 2026-07）");
        }
        // 234-R1（BSM:521「已发生的业务一律按当时生效的版本计算」）：每期出单取该期期末日已生效的版本。243-R1（18AQ 控制席判定）：有关停结算登记的商户
        // 取 min（期末日，关店最后营业日）已生效的版本——关店后的期间不再分红，与关停结算按关店最后营业日取周期同口径。
        java.util.Map<String, LocalDate> closed = repository.closeoutClosedDates();
        List<DividendPlanRecord> plans;
        if (request.merchantId() != null && !request.merchantId().isBlank()) {
            String merchantId = request.merchantId().trim();
            LocalDate asOf = planDate(period, closed.get(merchantId));
            plans = List.of(repository.findPlanInForce(merchantId, asOf)
                    .orElseThrow(() -> ApiException.notFound("DIVIDEND_PLAN_NOT_FOUND",
                            "该商户在 " + asOf + " 没有已生效的分红方案")));
        } else {
            plans = java.util.stream.Stream.concat(
                            repository.listPlansInForce(period.end()).stream().filter(plan -> !closed.containsKey(plan.merchantId())),
                            closed.entrySet().stream().flatMap(entry ->
                                    repository.findPlanInForce(entry.getKey(), planDate(period, entry.getValue())).stream()))
                    .sorted(java.util.Comparator.comparing(DividendPlanRecord::merchantId)).toList();
        }
        return runCompute(plans, kind, period, currentOperator());
    }

    /** 243-R1：出单取方案版本的日期——期末日，有关停结算登记且关店最后营业日更早的取关店最后营业日。 */
    private static LocalDate planDate(DividendPeriod period, LocalDate closedDay) {
        return closedDay != null && closedDay.isBefore(period.end()) ? closedDay : period.end();
    }

    /**
     * 第十八批 V 段第 159 条 159-R6（BSM:1777「该预览单在上月最后一个营业日的五类数据……全部标记为最终之后自动生成，不设固定生成时点——数据未全部
     * 标记最终之前不得生成预览，标记完成即自动生成。」；DEC-R8-45；第十八批裁决 35「酒吧逐店逐日查；游乐等按综合体逐日查」）：取代每月固定时点的预览作业。
     * 某单位当月最后一个营业日五类标齐的标记提交后，对该单位的生效方案出该月 preview 单——酒吧取该店的方案，综合体取其下非酒吧商户的方案；
     * 系统操作人、刷新式幂等（runCompute 内 RECOMPUTABLE 含 preview），月度方案照旧跳过，preview 不可审批（V299）。提交后另开事务执行，
     * 失败只记日志、不影响已提交的标记（财务可手动出预览，同闸）；无请求上下文，不设鉴权闸，无 Controller 绑定。
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMonthEndBusinessDayFinal(FinanceService.MonthEndBusinessDayFinal event) {
        LocalDate monthEnd = event.month().atEndOfMonth();
        List<DividendPlanRecord> plans = event.barId() != null
                ? repository.findPlanInForce(event.barId(), monthEnd).stream().toList()
                : repository.listPlansInForce(monthEnd).stream().filter(plan -> businessDayResolver.barOfMerchant(plan.merchantId()) == null
                        && event.projectId().equals(businessDayResolver.projectOfMerchant(plan.merchantId()))).toList();
        if (!plans.isEmpty()) {
            runCompute(plans, "preview", DividendPeriod.parse(event.month().toString()), SYSTEM_OPERATOR);
        }
    }

    /** 期间作业核心循环（请求路径与月末五类标齐预览共用；operator 显式传入以脱离鉴权上下文）。 */
    private DividendComputeResultRecord runCompute(
            List<DividendPlanRecord> plans, String kind, DividendPeriod period, String operator) {
        List<DividendStatementRecord> computed = new ArrayList<>();
        List<DividendComputeSkipRecord> skipped = new ArrayList<>();
        Instant now = Instant.now();
        java.util.Map<String, LocalDate> closed = repository.closeoutClosedDates();
        for (DividendPlanRecord plan : plans) {
            // 243-R1（18AQ 控制席判定「关店后的期间不再分红」）：整期在关店最后营业日之后的不出单。
            LocalDate closedDay = closed.get(plan.merchantId());
            if (closedDay != null && closedDay.isBefore(period.start())) {
                skipped.add(new DividendComputeSkipRecord(plan.merchantId(),
                        "该酒吧已于 " + closedDay + " 关店（已登记关停结算），关店后的期间不再分红"));
                continue;
            }
            String mismatch = periodMismatchReason(kind, period, plan);
            if (mismatch != null) {
                skipped.add(new DividendComputeSkipRecord(plan.merchantId(), mismatch));
                continue;
            }
            // 211-R1 第十八批裁决 43「「暂不分红」该期不出正式单、利润不补分；「暂缓分红」等协商恢复后由财务登记恢复，再照常出单；月度预览照常」：
            // 只判正式单，有未恢复登记的不出单并写明原因与协商结果（该期草稿已在登记时作废）。
            Optional<DividendPeriodHoldRecord> hold = "official".equals(kind)
                    ? repository.findOpenHold(plan.merchantId(), period.label()) : Optional.empty();
            if (hold.isPresent()) {
                skipped.add(new DividendComputeSkipRecord(plan.merchantId(), "本期经协商" + HOLD_LABELS.get(hold.get().holdType())
                        + "，不出正式单（原因：" + hold.get().reason() + "；协商结果：" + hold.get().negotiationResult() + "）"));
                continue;
            }
            // 第十八批裁决 42「登记齐了才能出单：正式单出单前本期每个月都要有已生效的税费登记，缺的不出单并写跳过原因」：只判正式单（月度预览照扣
            // 已生效税费、不判），看各月是否被已生效登记的所属期覆盖（扣税取数另看所属期末月）；在作废旧单之前判，已有草稿不作废。
            List<String> untaxed = "official".equals(kind)
                    ? repository.listMonthsWithoutApprovedTax(plan.merchantId(), period.months()) : List.of();
            if (!untaxed.isEmpty()) {
                skipped.add(new DividendComputeSkipRecord(plan.merchantId(), "本期 " + String.join("、", untaxed)
                        + " 尚无已生效的税费申报登记，请先登记并审批通过后再出正式单"));
                continue;
            }
            String notFinal = nonFinalReason(kind, period, plan, closedDay);
            if (notFinal != null) {
                skipped.add(new DividendComputeSkipRecord(plan.merchantId(), notFinal));
                continue;
            }
            Optional<DividendStatementRecord> existing =
                    repository.findActiveStatement(plan.merchantId(), kind, period.label());
            if (existing.isPresent()) {
                if (!RECOMPUTABLE_STATUSES.contains(existing.get().status())) {
                    skipped.add(new DividendComputeSkipRecord(plan.merchantId(),
                            "已有计算单进入审批流（" + STATEMENT_STATUS_LABELS.get(existing.get().status()) + "），不重算"));
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

    /**
     * 第十八批 V 段第 159 条 159-R5（ENUM:190「非最终快照的数据一律不得进入结算、分红、正式提成计算和对账关账」；BSM:1774「分红基数按「净利润」算：
     * 一个周期里的经营收入（按含税实收口径），减掉这期间的全部成本、费用与税费之后的余额」）：正式单要求方案商户检查单位（酒吧按该店，其他商户按
     * 所属综合体，建档前营业日不计）期间内每个营业日五类数据都已标记为最终；未齐返回跳过原因（列营业日与缺的类别），在作废旧单之前判，旧草稿不作废。
     * 159-R6（BSM:1777「数据未全部标记最终之前不得生成预览」；DEC-R8-45「未全部标记最终之前不得生成」）：preview 只判该月最后一个营业日的五类，
     * 手动出预览与月末自动生成同闸。
     * 第十八批 AQ 段第 243 条 243-R1（ENUM:190；BSM:1787「酒吧关闭须完成关停结算，依法核清本次分红」）：有关停结算登记的酒吧，正式单只查期初至
     * min（期末日，关店最后营业日），停用、存档后照查；关店后的营业日不计。
     */
    private String nonFinalReason(String kind, DividendPeriod period, DividendPlanRecord plan, LocalDate closedDay) {
        boolean preview = "preview".equals(kind);
        List<String> lines = !preview && closedDay != null
                ? payoutPort.nonFinalDaysOfBar(plan.merchantId(), period.start(), planDate(period, closedDay),
                        FinanceService.FINAL_MARK_CATEGORIES.keySet())
                : payoutPort.nonFinalDays(plan.merchantId(), null, preview ? period.end() : period.start(), period.end(),
                        FinanceService.FINAL_MARK_CATEGORIES.keySet());
        if (lines.isEmpty()) {
            return null;
        }
        return (preview ? "该月最后一个营业日（" + period.end() + "）的五类数据尚未全部标记为最终，不得生成预览："
                : "以下营业日数据尚未标记为最终，不能出正式分红单：") + FinanceService.describeNonFinalDays(lines);
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
            // 第十八批 V 段第 159 条 159-R10（BSM:1777「预览只能看，不参与审批、不发放」「标记完成即自动生成」）：预览时尚未月结定格的服务费扣留数暂不计、
            // 不拦；正式单照旧要求月结定格（BSM:1076「月结定格的扣留部分，扣留金额计入该商户营业额、抽成基数与分红净利润基数」）。
            boolean preview = "preview".equals(kind);
            long barGross = repository.sumBarRevenueCent(merchantId, from, to, preview);
            long barRefund = repository.sumBarRefundCent(merchantId, from, to);
            revenue += barGross - barRefund;
            addLine(lines, statementId, merchantId, seq, "revenue_bar", barGross,
                    "酒吧整单已确认商品收入及原经营日月结服务费扣留" + window
                            + (preview ? "；预览：尚未月结定格的服务费扣留数暂不计" : ""), now);
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
            noCostBasisCount = costSummary.noCostBasisCount();
            addLine(lines, statementId, merchantId, seq, "cost_inventory", -costSummary.outboundTotalCostCent(),
                    "销货成本（V296 移动加权出库成本汇总）" + window
                            + (noCostBasisCount > 0
                                    ? "；无成本基准出库 " + noCostBasisCount + " 笔，成本合计或有低估" : ""),
                    now);
            // 第十八批裁决 38「收入与出库成本都不进分红净利润」（BSM:1111）：经理已确认坏账的酒吧订单出库成本另列一行冲回。
            long badDebtCost = inventoryRepository.sumBadDebtBarOrderOutboundCost(merchantId, from, to);
            if (badDebtCost > 0) {
                addLine(lines, statementId, merchantId, seq, "cost_inventory", badDebtCost,
                        "坏账订单出库成本不计入（第十八批裁决 38「收入与出库成本都不进分红净利润」）" + window, now);
            }
            cost = costSummary.outboundTotalCostCent() - badDebtCost;
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

        // 210-R3（BSM:1774「……以及该期间的各项税费（含增值税、所得税及其他应由该主体承担的税费）」；BSM:1801「连接税务处理，拿到这期的各项税费，
        // 算出净利润」；第十八批裁决 42「分红扣本期内已生效的申报金额合计；按季申报的税整季计入季末那个月」）：方案商户经营主体已生效、所属期末月落在
        // 本期内的实际申报金额逐税种扣减，不设方案开关（BSM:1774「全部」）。
        long periodTax = 0L;
        for (DividendRecords.PeriodTaxSumRecord taxSum : repository.sumEntityTaxByType(merchantId, period.months())) {
            periodTax += taxSum.totalCent();
            addLine(lines, statementId, merchantId, seq, "tax_period", -taxSum.totalCent(),
                    EntityTaxService.TAX_LABELS.get(taxSum.taxType()) + "实际申报金额（已生效登记，所属期 " + taxSum.periods() + "）", now);
        }

        long netProfit = revenue - cost - expense - periodTax;

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

        // 234-R2（BSM:1775、BSM:1776）：其他股东按名单比例各自四舍五入；个人股东按方案税率代扣（不随主理人类型变），公司股东不代扣。
        List<DividendRecords.DividendShareholderRecord> shareholders = plan.otherShareholders();
        int[] otherBps = shareholders.stream().mapToInt(DividendRecords.DividendShareholderRecord::shareBps).toArray();
        boolean[] otherCompany = new boolean[shareholders.size()];
        for (int i = 0; i < otherCompany.length; i++) {
            otherCompany[i] = "company".equals(shareholders.get(i).shareholderType());
        }

        DividendMath.Distribution dist = DividendMath.compute(
                netProfit, plan.platformShareBps(), effectiveTaxBps,
                plan.lossCarryforwardEnabled(), carryIn,
                plan.arrearsOffsetEnabled(), arrearsOutstanding,
                customRaws,
                plan.payoutFloorEnabled(), plan.payoutFloorBps(), plan.payoutFloorFixedCent(), legalCap,
                otherBps, otherCompany, plan.taxRateBps());

        addLine(lines, statementId, merchantId, seq, "net_profit", netProfit,
                "净利润 = 收入 − 销货成本 − 费用 − 税费（亏损期可分配额为 0，不倒扣主理人）", now);
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
        for (int i = 0; i < shareholders.size(); i++) {
            DividendRecords.DividendShareholderRecord holder = shareholders.get(i);
            long gross = dist.otherGrossCents().get(i);
            long tax = dist.otherTaxCents().get(i);
            addLine(lines, statementId, merchantId, seq, "shareholder_gross", gross,
                    "其他股东「" + holder.shareholderName() + "」（" + SHAREHOLDER_TYPE_LABELS.get(holder.shareholderType()) + "，持股 "
                            + holder.shareBps() + " 万分比）应得，四舍五入到分", now);
            addLine(lines, statementId, merchantId, seq, "shareholder_tax_withheld", -tax,
                    otherCompany[i]
                            ? "公司股东不代扣个税，按约定全额发放（代扣 0 分）"
                            : "代扣股息红利个税（个人股东，税率 " + plan.taxRateBps() + " 万分比，整除截断·对齐税局申报口径）", now);
            addLine(lines, statementId, merchantId, seq, "shareholder_payout", gross - tax,
                    "其他股东「" + holder.shareholderName() + "」实发 = 应得 − 代扣；审批通过后按此额挂应付", now);
        }
        if (dist.roundingResidualCent() != 0) {
            addLine(lines, statementId, merchantId, seq, "rounding_residual", dist.roundingResidualCent(),
                    "多方按持股比例分配的舍入尾差，由本酒吧公司承担，不发给任何一方", now);
        }
        addLine(lines, statementId, merchantId, seq, "principal_gross", dist.principalGrossCent(),
                "主理人应得（持股 " + (DividendMath.BPS_DENOMINATOR - plan.platformShareBps() - java.util.Arrays.stream(otherBps).sum())
                        + " 万分比，四舍五入到分）", now);
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
                plan.principalType(), dist.customDeductionCent(), legalCap, periodTax,
                dist.otherGrossCents().stream().mapToLong(Long::longValue).sum(), dist.roundingResidualCent());
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
                                statement.payoutCent(), statement.id(), payoutDueDate(statement, now));
                    }
                    // V303 发放保护线留存 → 记为欠主理人的待释放款（非平台白拿；释放为后续 R）。
                    if (statement.reserveWithheldCent() > 0) {
                        payoutPort.createDividendReserveWithholdingPayable(
                                statement.principalUserId(), statement.merchantId(),
                                statement.reserveWithheldCent(), statement.id());
                    }
                    // 234-R2（BSM:1787「分红由该酒吧公司自己从其对公账户支付给主理人与其他股东……发放走应付款项」）：每位其他股东实发 > 0 另挂一笔
                    // dividend_payout 应付，应付对方与来源都是该股东实发行（不要求平台账户，按单查得到是谁），到期日同为方案发放付账日；
                    // 登记付款同样过付款确认（第十八批裁决 55、65）。
                    for (DividendStatementLineRecord line : shareholderPayouts(statement)) {
                        payoutPort.createShareholderDividendPayable(line.id(), statement.merchantId(), line.amountCent(),
                                payoutDueDate(statement, now));
                    }
                    requireTransition(repository.markApproved(statement.id(), operator, arApItemId, now), "审批通过");
                    // 审批通过这一步才挂应付、抵欠款（草稿与月度预览会重算作废，不发）。
                    notifyMerchantAmount(statement.merchantId(), "dividend_issued", statement.payoutCent(),
                            statement.periodLabel() + " 分红出单：税后应得 " + yuan(statement.principalNetCent()) + "，欠款抵扣 "
                                    + yuan(statement.arrearsOffsetCent()) + "，留存 " + yuan(statement.reserveWithheldCent()),
                            now, statement.id());
                });
    }

    /** 234-R2 本单金额大于 0 的其他股东实发行（各挂一笔分红应付，来源＝行 id）。 */
    private List<DividendStatementLineRecord> shareholderPayouts(DividendStatementRecord statement) {
        return repository.listLines(statement.id(), null).stream()
                .filter(line -> "shareholder_payout".equals(line.lineType()) && line.amountCent() > 0).toList();
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
                    // 第十八批 AM 段第 241 条 241-R3（BSM:1787「发放走应付款项」；BSM:1803「审批通过后的发放进入应付款项台账，走付款确认」）：实发额大于 0 时，
                    // 主理人分红应付须已按付款确认付清才能标记发放。
                    if (statement.payoutCent() > 0 && payoutPort.arApUnpaidCent(statement.arApItemId()) > 0) {
                        throw ApiException.conflict("DIVIDEND_PAYOUT_NOT_PAID", "主理人分红应付尚未付清（须先完成付款确认并登记付款），不能标记发放");
                    }
                    // 234-R2：其他股东分红应付也须都已按付款确认付清。
                    List<String> shareholderSources = shareholderPayouts(statement).stream().map(DividendStatementLineRecord::id).toList();
                    if (!shareholderSources.isEmpty() && payoutPort.unpaidDividendPayoutCent(shareholderSources) > 0) {
                        throw ApiException.conflict("DIVIDEND_PAYOUT_NOT_PAID", "其他股东分红应付尚未付清（须先完成付款确认并登记付款），不能标记发放");
                    }
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
                    "只有草稿或已驳回的正式单可以登记依法可分配金额（当前状态：" + STATEMENT_STATUS_LABELS.get(statement.status()) + "）");
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
                    message + "（当前状态：" + STATEMENT_STATUS_LABELS.get(statement.status()) + "）");
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
                    "只有已审批/已发放的计算单才有留存可释放（当前状态：" + STATEMENT_STATUS_LABELS.get(stmt.status()) + "）");
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
                payoutPort.releaseDividendReserveWithholding(statementId, payoutDueDate(stmt, now));
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

    // ── 211-R1 整期暂不分红／暂缓分红登记 ──────────────────────

    /** 登记列表（平台财务持股分红页用；商户账号拒）。 */
    public List<DividendPeriodHoldRecord> listHolds() {
        authorizationService.requirePermission(VIEW_PERM);
        authorizationService.requirePlatformScope();
        return operations.readSingle(
                AuditedOperation.of("dividend.holds_listed", "dividend.holds_list_failed", VIEW_PERM, HOLD_OBJECT),
                repository::listHolds,
                result -> "all");
    }

    /**
     * 211-R1 登记暂不分红或暂缓分红（BSM:1777「若因特殊情况需要整期暂不分红或暂缓分红的，须由主理人与娱乐平台公司协商一致后执行，并记录原因与协商结果」；
     * 第十八批裁决 43「平台财务在持股分红页按商户按期登记（暂不分红或暂缓分红，原因、协商结果必填，不另走审批）；该期正式单已提交审批或已发放的不能再登记，
     * 草稿随登记作废」）：期间标签须与该商户生效方案的周期一致；同一商户同一期间只能有一条未恢复的登记。
     */
    @Transactional
    public DividendPeriodHoldRecord recordHold(DividendHoldRequest request) {
        authorizationService.requirePermission(MANAGE_PERM);
        authorizationService.requirePlatformScope();
        requireRequest(request);
        return operations.executeWrite(
                "recordDividendHold",
                request.idempotencyKey(),
                request,
                DividendPeriodHoldRecord.class,
                AuditedOperation.of("dividend.hold_recorded", "dividend.hold_record_failed", MANAGE_PERM, HOLD_OBJECT),
                () -> recordHoldInternal(request),
                DividendService::businessId,
                java.util.Map.of());
    }

    private DividendPeriodHoldRecord recordHoldInternal(DividendHoldRequest request) {
        String merchantId = requireText(request.merchantId(), "merchant_id");
        String holdType = requireEnum(request.holdType(), HOLD_LABELS.keySet(), "hold_type");
        String reason = requireText(request.reason(), "reason");
        String negotiationResult = requireText(request.negotiationResult(), "negotiation_result");
        DividendPeriod period = DividendPeriod.parse(request.periodLabel());
        DividendPlanRecord plan = repository.findPlanInForce(merchantId, period.end())
                .orElseThrow(() -> ApiException.notFound("DIVIDEND_PLAN_NOT_FOUND", "该商户在 " + period.end() + " 没有已生效的分红方案"));
        String mismatch = periodMismatchReason("official", period, plan);
        if (mismatch != null) {
            throw ApiException.badRequest("VALIDATION_FAILED", mismatch.replace("，跳过", ""));
        }
        Optional<DividendStatementRecord> official = repository.findActiveStatement(merchantId, "official", period.label());
        if (official.isPresent() && !RECOMPUTABLE_STATUSES.contains(official.get().status())) {
            throw ApiException.conflict("DIVIDEND_STATE_INVALID", "该期正式单已提交审批或已发放（当前状态："
                    + STATEMENT_STATUS_LABELS.get(official.get().status()) + "），不能再登记暂不分红或暂缓分红");
        }
        String operator = currentOperator();
        Instant now = Instant.now();
        DividendPeriodHoldRecord hold = new DividendPeriodHoldRecord("divh_" + UUID.randomUUID().toString().replace("-", ""),
                merchantId, period.label(), holdType, reason, negotiationResult, operator, now, null, null, null);
        if (!repository.insertHold(hold)) {
            throw ApiException.conflict("DIVIDEND_HOLD_EXISTS", "该商户本期已有未恢复的暂不分红或暂缓分红登记");
        }
        // 草稿随登记作废（已驳回的正式单同为可重算态，一并作废）；0 行＝并发下已提交审批，整笔回滚。
        if (official.isPresent()) {
            requireTransition(repository.markVoided(official.get().id(), operator, now), "作废该期正式草稿");
        }
        return hold;
    }

    /**
     * 211-R1 恢复暂缓分红（第十八批裁决 43「「暂缓分红」等协商恢复后由财务登记恢复，再照常出单」）：只对未恢复的暂缓分红，恢复的协商结果必填；
     * 暂不分红不恢复（「该期不出正式单、利润不补分」）。
     */
    @Transactional
    public DividendPeriodHoldRecord liftHold(String holdId, DividendHoldRequest.Lift request) {
        authorizationService.requirePermission(MANAGE_PERM);
        authorizationService.requirePlatformScope();
        requireRequest(request);
        String id = requireText(holdId, "hold_id");
        return operations.executeWrite(
                "liftDividendHold:" + id,
                request.idempotencyKey(),
                request,
                DividendPeriodHoldRecord.class,
                AuditedOperation.of("dividend.hold_lifted", "dividend.hold_lift_failed", MANAGE_PERM, HOLD_OBJECT),
                () -> {
                    String liftResult = requireText(request.liftResult(), "lift_result");
                    DividendPeriodHoldRecord hold = repository.findHoldById(id)
                            .orElseThrow(() -> ApiException.notFound("DIVIDEND_HOLD_NOT_FOUND", "暂不分红或暂缓分红登记不存在：" + id));
                    if (!"defer".equals(hold.holdType())) {
                        throw ApiException.conflict("DIVIDEND_HOLD_NOT_LIFTABLE", "暂不分红的期间不恢复：该期不出正式单、利润不补分");
                    }
                    if (repository.liftHold(id, currentOperator(), liftResult, Instant.now()) == 0) {
                        throw ApiException.conflict("DIVIDEND_HOLD_NOT_LIFTABLE", "该暂缓分红登记已恢复，不能重复恢复");
                    }
                    return repository.findHoldById(id).orElseThrow();
                },
                DividendService::businessId,
                java.util.Map.of());
    }

    // ── 辅助 ──────────────────────────────────────────────────

    /**
     * 211-R2（BSM:1787「发放走应付款项，落在三个付账日之一，具体哪一个由该公司在分红周期配置中指定」；DEC-R3-2026-08-04-05；第十八批裁决 44
     * 「审批通过后给主理人的应付和留存释放的应付都落在当天或之后第一个该付账日」）：按计算单所属方案的发放付账日，取 at 所在营业日当天或之后第一个该付账日
     * （遇非银行工作日顺延，BSM:1683）。
     */
    private LocalDate payoutDueDate(DividendStatementRecord statement, Instant at) {
        Integer slot = repository.findPlanById(statement.planId()).map(DividendPlanRecord::payoutPaySlot).orElse(null);
        if (slot == null) {
            throw ApiException.conflict("DIVIDEND_PAY_SLOT_MISSING", "请先在分红方案中指定发放付账日");
        }
        return payDays.payDayOnOrAfter(slot, businessDayResolver.merchantBusinessDate(null, statement.merchantId(), at));
    }

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
        if (saved instanceof DividendPeriodHoldRecord hold) {
            return hold.id();
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
