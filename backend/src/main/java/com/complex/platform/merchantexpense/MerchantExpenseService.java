package com.complex.platform.merchantexpense;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditedOperation;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.merchantexpense.dto.MerchantExpenseEntryRequest;
import com.complex.platform.merchantexpense.dto.MerchantExpenseReversalRequest;
import com.complex.platform.merchantexpense.dto.MerchantExpenseSyncRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.complex.platform.common.Strings.requireText;

/**
 * 商户费用归集台账（持股分红净利润制费用侧）。资金类仅追加：更正走红字冲销新行，不改历史行。
 * 自动挂接（能耗/物业费双路径）与手工录入（人工/摊销/其他）都汇入同一张台账，期间汇总供分红引擎扣减。
 *
 * <p>写路径统一走 {@link AuditedOperationExecutor#executeWrite}（finance 域同款）：幂等键带请求指纹——
 * 同 key 重放返回首次响应，同 key 不同载荷抛 IDEMPOTENCY_CONFLICT（消除“同键不同载荷静默返回第一次行”）；
 * 成功/失败审计事件由执行器统一落，与 OpenAPI x-audit-event-success/failure 一一对应（无幽灵事件）。
 */
@Service
public class MerchantExpenseService {

    static final String VIEW_PERM = "finance.merchant_expense.view";
    static final String MANAGE_PERM = "finance.merchant_expense.manage";
    private static final String BUSINESS_TYPE = "merchant_expense_entry";

    private static final Set<String> EXPENSE_TYPES = Set.of(
            "rent_allocation", "energy", "property_fee", "labor", "amortization", "other");
    private static final Set<String> SOURCES = Set.of(
            "auto_energy", "auto_property", "auto_rent", "auto_payroll", "manual");
    private static final Pattern PERIOD_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final MerchantExpenseRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditedOperationExecutor operations;

    public MerchantExpenseService(
            MerchantExpenseRepository repository,
            AuthorizationService authorizationService,
            AuditedOperationExecutor operations) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.operations = operations;
    }

    /** 台账分页查询。商户 scope 账号强制只看自己（覆盖请求参数）。 */
    public PageResponse<MerchantExpenseEntryRecord> listEntries(
            String merchantId, String period, String expenseType, String source, int page, int pageSize) {
        authorizationService.requirePermission(VIEW_PERM);
        return operations.readPageResult(
                AuditedOperation.of("merchant_expense.listed", "merchant_expense.list_failed", VIEW_PERM, BUSINESS_TYPE),
                () -> {
                    String scopedMerchant = scopeMerchant(merchantId);
                    if (period != null && !period.isBlank()) {
                        requirePeriod(period);
                    }
                    if (expenseType != null && !expenseType.isBlank() && !EXPENSE_TYPES.contains(expenseType)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "无法识别的费用类型：" + expenseType);
                    }
                    if (source != null && !source.isBlank() && !SOURCES.contains(source)) {
                        throw ApiException.badRequest("VALIDATION_FAILED", "无法识别的费用来源：" + source);
                    }
                    int safePage = Math.max(page, 1);
                    int safeSize = Math.min(Math.max(pageSize, 1), 200);
                    return repository.listEntries(blankToNull(scopedMerchant), blankToNull(period),
                            blankToNull(expenseType), blankToNull(source), safePage, safeSize);
                });
    }

    /**
     * 期间关账锁清单：某月被 approved/paid 正式分红单覆盖（关账）的商户 id 列表。
     * 前端据此对已关账商户禁用「录入/冲销」按钮并给锁定提示（后端 requirePeriodOpen 是硬闸）。
     */
    public List<String> listLockedMerchants(String period) {
        authorizationService.requirePermission(VIEW_PERM);
        return operations.readSingle(
                AuditedOperation.of("merchant_expense.period_locks_viewed",
                        "merchant_expense.period_locks_view_failed", VIEW_PERM, BUSINESS_TYPE),
                () -> {
                    List<String> locked = repository.listLockedMerchants(requirePeriod(period));
                    // x-data-scope: merchant——商户 scope 账号只回自己商户的关账状态（0 或 1 条），不外泄他商户。
                    Optional<String> scope = authorizationService.currentMerchantScope();
                    return scope.map(own -> locked.stream().filter(own::equals).toList()).orElse(locked);
                },
                result -> "period:" + period);
    }

    /** 期间汇总：按商户 + 月份返回分类型费用合计（分红引擎费用侧取数点）。 */
    public List<MerchantExpenseSummaryRecord> summarize(String period, String merchantId) {
        authorizationService.requirePermission(VIEW_PERM);
        return operations.readSingle(
                AuditedOperation.of(
                        "merchant_expense.summary_viewed", "merchant_expense.summary_view_failed", VIEW_PERM, BUSINESS_TYPE),
                () -> repository.summarize(requirePeriod(period), blankToNull(scopeMerchant(merchantId))),
                result -> "period:" + period);
    }

    /**
     * 手工录入（人工/摊销/其他等）：理由必填；幂等键带请求指纹（同 key 同载荷重放返回同一行，
     * 同 key 不同载荷抛 IDEMPOTENCY_CONFLICT）。商户 scope 账号只能记自己商户，传他商户 id 拒绝。
     */
    @Transactional
    public MerchantExpenseEntryRecord createManual(MerchantExpenseEntryRequest request) {
        authorizationService.requirePermission(MANAGE_PERM);
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return operations.executeWrite(
                "createMerchantExpenseEntry",
                request.idempotencyKey(),
                request,
                MerchantExpenseEntryRecord.class,
                AuditedOperation.of(
                        "merchant_expense.manual_created", "merchant_expense.manual_create_failed", MANAGE_PERM, BUSINESS_TYPE),
                () -> createManualInternal(request),
                MerchantExpenseService::businessId,
                java.util.Map.of());
    }

    private MerchantExpenseEntryRecord createManualInternal(MerchantExpenseEntryRequest request) {
        // I-2 商户 scope 守卫（MerchantBillingService.createBillOnce 同款）：请求体显式携带 merchant_id 的写路径，
        // 商户账号传他商户 id 一律拒绝（不静默改写）；平台账号（scope 为空）可代任意商户录入。
        String merchantId = requireText(request.merchantId(), "merchant_id");
        checkMerchantOwnership(merchantId);
        String period = requirePeriod(request.period());
        requirePeriodOpen(merchantId, period);
        String expenseType = requireText(request.expenseType(), "expense_type");
        if (!EXPENSE_TYPES.contains(expenseType)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "无法识别的费用类型：" + expenseType);
        }
        if (request.amountCent() == null || request.amountCent() == 0L) {
            throw ApiException.badRequest("VALIDATION_FAILED", "金额必须为非零整数（单位：分）");
        }
        String note = requireText(request.note(), "note");

        String id = "mex_m_" + stableToken(request.idempotencyKey());
        MerchantExpenseEntryRecord entry = new MerchantExpenseEntryRecord(
                id, merchantId, period, expenseType, request.amountCent(), "manual", null,
                note, currentOperator(), Instant.now());
        repository.insertEntry(entry);
        return repository.findById(id)
                .orElseThrow(() -> ApiException.conflict("EXPENSE_ENTRY_CONFLICT", "费用登记发生并发冲突，请重试"));
    }

    /**
     * 红字冲销：生成原行金额取负的新行（source=manual、source_ref=原行 id），不改历史行。
     * 每行仅允许一次全额冲销（冲销行 id 由原行 id 派生，天然幂等）；冲销行本身不可再被冲销。
     * 幂等以「操作 × 原行」为界（operationId 含 entryId）：同 key 重放同一原行返回首次响应，
     * 同 key 不同载荷（note 不同等）抛 IDEMPOTENCY_CONFLICT。商户 scope 账号不得冲销他商户行。
     */
    @Transactional
    public MerchantExpenseEntryRecord reverse(String entryId, MerchantExpenseReversalRequest request) {
        authorizationService.requirePermission(MANAGE_PERM);
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        String id = requireText(entryId, "entry_id");
        return operations.executeWrite(
                "reverseMerchantExpenseEntry:" + id,
                request.idempotencyKey(),
                request,
                MerchantExpenseEntryRecord.class,
                AuditedOperation.of(
                        "merchant_expense.reversed", "merchant_expense.reverse_failed", MANAGE_PERM, BUSINESS_TYPE),
                () -> reverseInternal(id, request),
                MerchantExpenseService::businessId,
                java.util.Map.of());
    }

    private MerchantExpenseEntryRecord reverseInternal(String entryId, MerchantExpenseReversalRequest request) {
        String note = requireText(request.note(), "note");
        MerchantExpenseEntryRecord original = repository.findById(entryId)
                .orElseThrow(() -> ApiException.notFound("EXPENSE_ENTRY_NOT_FOUND", "费用记录不存在：" + entryId));
        // I-2 归属校验（FinanceService.checkMerchantOwnership 同款）：商户 scope 账号不得冲销他商户的台账行。
        checkMerchantOwnership(original.merchantId());
        // 关账锁：已被 approved/paid 正式分红单覆盖的月份禁止冲销（否则已发放分红的基数被改）。
        requirePeriodOpen(original.merchantId(), original.period());
        if (original.isReversal()) {
            throw ApiException.conflict("EXPENSE_REVERSAL_NOT_ALLOWED", "冲销行不可再被冲销；如需更正请冲销原始行后重新录入");
        }

        String reversalId = "mex_r_" + original.id();
        MerchantExpenseEntryRecord reversal = new MerchantExpenseEntryRecord(
                reversalId, original.merchantId(), original.period(), original.expenseType(),
                -original.amountCent(), "manual", original.id(), note, currentOperator(), Instant.now());
        repository.insertEntry(reversal);
        return repository.findById(reversalId)
                .orElseThrow(() -> ApiException.conflict("EXPENSE_ENTRY_CONFLICT", "费用冲正发生并发冲突，请重试"));
    }

    /**
     * 自动挂接：把 period 已出账的能耗费应收（ar_ap_items source_type='energy_fee'）、物业费账单
     * （property_fee_bills）、合同计费物业费（ar_ap_items source_type='contract_property_fee'）与租金
     * （ar_ap_items source_type ∈ contract_rent/turnover_rent，2026-07-18 拍板计入分红基数）汇入台账。
     * 幂等可重跑：同商户同月同来源单据不重复计入（id 派生 + 部分唯一索引）。
     * 响应附金额漂移报告（上游改账后台账净额与上游不一致的清单；处置 SOP=红字冲销旧行+手工录入修正额）。
     * 上游出账本身是手动触发（能耗结算/物业费出账），故此处用手动触发端点而非 @Scheduled 空跑。
     * 第十八批 AC 段第 210 条 210-R1（BSM:1801「连接工资单月结，拿到这期的人工成本」）：另挂工资单月结人工成本与派驻、营销人员提成拨付
     * （来源 auto_payroll，见 {@link MerchantExpenseRepository#attachPayrollLabor}）。
     */
    @Transactional
    public MerchantExpenseSyncResultRecord syncAuto(MerchantExpenseSyncRequest request) {
        authorizationService.requirePermission(MANAGE_PERM);
        authorizationService.requirePlatformScope();
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "请求内容不能为空");
        }
        return operations.executeWrite(
                "syncMerchantExpenseAuto",
                request.idempotencyKey(),
                request,
                MerchantExpenseSyncResultRecord.class,
                AuditedOperation.of(
                        "merchant_expense.auto_synced", "merchant_expense.auto_sync_failed", MANAGE_PERM, BUSINESS_TYPE),
                () -> {
                    String period = requirePeriod(request.period());
                    Instant now = Instant.now();
                    String operator = currentOperator();
                    // 费用侧冻结锁：attach* 的 SQL 已对 approved/paid 正式分红单覆盖的 (商户,月份) NOT EXISTS 跳过；
                    // lockedMerchants 回该期<b>全部关账商户</b>（非仅本次跳过的——见 MerchantExpenseSyncResultRecord），
                    // 让操作员一眼知道哪些商户该期已关账、费用台账被冻结。
                    int energyInserted = repository.attachEnergyFees(period, operator, now);
                    int propertyInserted = repository.attachPropertyFees(period, operator, now);
                    int contractPropertyInserted = repository.attachContractPropertyFees(period, operator, now);
                    int rentInserted = repository.attachContractRent(period, operator, now);
                    int payrollInserted = repository.attachPayrollLabor(period, operator, now);
                    return new MerchantExpenseSyncResultRecord(period, energyInserted, propertyInserted,
                            contractPropertyInserted, rentInserted, payrollInserted, repository.listAutoDrift(period),
                            repository.listLockedMerchants(period));
                },
                MerchantExpenseService::businessId,
                java.util.Map.of());
    }

    /** 商户 scope 账号强制只看/只记自己商户；平台账号透传请求参数。 */
    private String scopeMerchant(String requested) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return scope.orElse(requested);
    }

    /** 商户 scope 账号访问的台账行必须归属自己商户（FinanceService 同款守卫）。 */
    private void checkMerchantOwnership(String resourceMerchantId) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        if (scope.isPresent() && !scope.get().equals(resourceMerchantId)) {
            throw ApiException.forbidden("CROSS_MERCHANT_FORBIDDEN",
                    "无权操作其他商户的数据");
        }
    }

    /**
     * 分红期间「费用侧冻结」锁：该商户该月被 approved/paid 正式分红单覆盖时，拒绝录入/冲销费用（响亮 409
     * DIVIDEND_PERIOD_LOCKED，进 x-failure-codes）；否则已发放分红的费用基数被追改而静默漂移。
     * 锁的是费用台账 merchant_expense_entries——营收/成本源头按设计不锁；已发单金额靠 V299 计算单快照冻结兜底。
     * 解锁=作废该分红单（判定式不含 voided，天然解锁）。
     *
     * <p>ponytail: 读判定 → 插入之间有一个有界 TOCTOU 窗口（窗口内某分红单恰好被审批通过）；此处不加
     * FOR SHARE 跨表行锁（费用写是热路径，跨 dividend_statements 加锁引入锁竞争得不偿失）。该窗口已被 V299
     * 计算单快照冻结兜底自愈：已 approved 单的金额是 compute 时的快照，窗口内漏进的这行费用不会回改它；下次
     * compute 会纳入并重出新单（重新审批）。要更强一致再上 FOR SHARE / 应用级期间锁。
     */
    private void requirePeriodOpen(String merchantId, String period) {
        if (repository.isPeriodLocked(merchantId, period)) {
            throw ApiException.conflict("DIVIDEND_PERIOD_LOCKED",
                    "该商户 " + period + " 月已完成分红核算（关账），费用台账已冻结、不可再录入/冲销；如需更正请先作废对应分红计算单解锁");
        }
    }

    private String currentOperator() {
        return authorizationService.currentActor().id();
    }

    private static String businessId(Object saved) {
        if (saved instanceof MerchantExpenseEntryRecord entry) {
            return entry.id();
        }
        if (saved instanceof MerchantExpenseSyncResultRecord sync) {
            return sync.period();
        }
        return "all";
    }

    private static String requirePeriod(String period) {
        String p = requireText(period, "period");
        if (!PERIOD_PATTERN.matcher(p).matches()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "账期格式不正确，应为 yyyy-MM（如 2026-07）");
        }
        return p;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String stableToken(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
    }
}
