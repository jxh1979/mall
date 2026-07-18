package com.complex.platform.merchantexpense;

import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.ApiRequests;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.merchantexpense.dto.MerchantExpenseEntryRequest;
import com.complex.platform.merchantexpense.dto.MerchantExpenseReversalRequest;
import com.complex.platform.merchantexpense.dto.MerchantExpenseSyncRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商户费用归集台账（持股分红净利润制费用侧）。资金类仅追加：更正走红字冲销新行，不改历史行。
 * 自动挂接（能耗/物业费）与手工录入（人工/摊销/其他）都汇入同一张台账，期间汇总供分红引擎扣减。
 */
@Service
public class MerchantExpenseService {

    static final String VIEW_PERM = "finance.merchant_expense.view";
    static final String MANAGE_PERM = "finance.merchant_expense.manage";

    private static final Set<String> EXPENSE_TYPES = Set.of(
            "rent_allocation", "energy", "property_fee", "labor", "amortization", "other");
    private static final Set<String> SOURCES = Set.of("auto_energy", "auto_property", "manual");
    private static final Pattern PERIOD_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private final MerchantExpenseRepository repository;
    private final AuthorizationService authorizationService;
    private final AuditEventService auditEventService;

    public MerchantExpenseService(
            MerchantExpenseRepository repository,
            AuthorizationService authorizationService,
            AuditEventService auditEventService) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.auditEventService = auditEventService;
    }

    /** 台账分页查询。商户 scope 账号强制只看自己（覆盖请求参数）。 */
    public PageResponse<MerchantExpenseEntryRecord> listEntries(
            String merchantId, String period, String expenseType, String source, int page, int pageSize) {
        authorizationService.requirePermission(VIEW_PERM);
        String scopedMerchant = scopeMerchant(merchantId);
        if (period != null && !period.isBlank()) {
            requirePeriod(period);
        }
        if (expenseType != null && !expenseType.isBlank() && !EXPENSE_TYPES.contains(expenseType)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "unknown expense_type: " + expenseType);
        }
        if (source != null && !source.isBlank() && !SOURCES.contains(source)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "unknown source: " + source);
        }
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), 200);
        return repository.listEntries(blankToNull(scopedMerchant), blankToNull(period),
                blankToNull(expenseType), blankToNull(source), safePage, safeSize);
    }

    /** 期间汇总：按商户 + 月份返回分类型费用合计（分红引擎费用侧取数点）。 */
    public List<MerchantExpenseSummaryRecord> summarize(String period, String merchantId) {
        authorizationService.requirePermission(VIEW_PERM);
        return repository.summarize(requirePeriod(period), blankToNull(scopeMerchant(merchantId)));
    }

    /** 手工录入（人工/摊销/其他等）：理由必填；幂等（同 idempotency_key 重放返回同一行）。 */
    @Transactional
    public MerchantExpenseEntryRecord createManual(MerchantExpenseEntryRequest request) {
        authorizationService.requirePermission(MANAGE_PERM);
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        ApiRequests.requireIdempotencyKey(request.idempotencyKey());
        String merchantId = requireText(request.merchantId(), "merchant_id");
        String period = requirePeriod(request.period());
        String expenseType = requireText(request.expenseType(), "expense_type");
        if (!EXPENSE_TYPES.contains(expenseType)) {
            throw ApiException.badRequest("VALIDATION_FAILED", "unknown expense_type: " + expenseType);
        }
        if (request.amountCent() == null || request.amountCent() == 0L) {
            throw ApiException.badRequest("VALIDATION_FAILED", "amount_cent must be a non-zero integer (cent)");
        }
        String note = requireText(request.note(), "note");

        String id = "mex_m_" + stableToken(request.idempotencyKey());
        MerchantExpenseEntryRecord entry = new MerchantExpenseEntryRecord(
                id, merchantId, period, expenseType, request.amountCent(), "manual", null,
                note, currentOperator(), Instant.now());
        boolean inserted = repository.insertEntry(entry);
        MerchantExpenseEntryRecord saved = repository.findById(id)
                .orElseThrow(() -> ApiException.conflict("EXPENSE_ENTRY_CONFLICT", "expense entry insert lost race"));
        if (inserted) {
            auditEventService.recordSuccess("merchant_expense.manual_created", MANAGE_PERM,
                    "merchant_expense_entry", id, null, request.idempotencyKey(),
                    Map.of("merchant_id", merchantId, "period", period,
                            "expense_type", expenseType, "amount_cent", request.amountCent(), "note", note));
        }
        return saved;
    }

    /**
     * 红字冲销：生成原行金额取负的新行（source=manual、source_ref=原行 id），不改历史行。
     * 每行仅允许一次全额冲销（冲销行 id 由原行 id 派生，天然幂等）；冲销行本身不可再被冲销。
     */
    @Transactional
    public MerchantExpenseEntryRecord reverse(String entryId, MerchantExpenseReversalRequest request) {
        authorizationService.requirePermission(MANAGE_PERM);
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        ApiRequests.requireIdempotencyKey(request.idempotencyKey());
        String note = requireText(request.note(), "note");
        MerchantExpenseEntryRecord original = repository.findById(requireText(entryId, "entry_id"))
                .orElseThrow(() -> ApiException.notFound("EXPENSE_ENTRY_NOT_FOUND", "expense entry not found: " + entryId));
        if (original.isReversal()) {
            throw ApiException.conflict("EXPENSE_REVERSAL_NOT_ALLOWED", "冲销行不可再被冲销；如需更正请冲销原始行后重新录入");
        }

        String reversalId = "mex_r_" + original.id();
        MerchantExpenseEntryRecord reversal = new MerchantExpenseEntryRecord(
                reversalId, original.merchantId(), original.period(), original.expenseType(),
                -original.amountCent(), "manual", original.id(), note, currentOperator(), Instant.now());
        boolean inserted = repository.insertEntry(reversal);
        MerchantExpenseEntryRecord saved = repository.findById(reversalId)
                .orElseThrow(() -> ApiException.conflict("EXPENSE_ENTRY_CONFLICT", "expense reversal insert lost race"));
        if (inserted) {
            auditEventService.recordSuccess("merchant_expense.reversed", MANAGE_PERM,
                    "merchant_expense_entry", reversalId, null, request.idempotencyKey(),
                    Map.of("original_entry_id", original.id(), "merchant_id", original.merchantId(),
                            "period", original.period(), "amount_cent", -original.amountCent(), "note", note));
        }
        return saved;
    }

    /**
     * 自动挂接：把 period 已出账的能耗费应收（ar_ap_items source_type='energy_fee'）与物业费账单
     * （property_fee_bills）汇入台账。幂等可重跑：同商户同月同来源单据不重复计入（id 派生 + 部分唯一索引）。
     * 上游出账本身是手动触发（能耗结算/物业费出账），故此处用手动触发端点而非 @Scheduled 空跑。
     */
    @Transactional
    public MerchantExpenseSyncResultRecord syncAuto(MerchantExpenseSyncRequest request) {
        authorizationService.requirePermission(MANAGE_PERM);
        authorizationService.requirePlatformScope();
        if (request == null) {
            throw ApiException.badRequest("VALIDATION_FAILED", "request body is required");
        }
        ApiRequests.requireIdempotencyKey(request.idempotencyKey());
        String period = requirePeriod(request.period());
        Instant now = Instant.now();
        String operator = currentOperator();
        int energyInserted = repository.attachEnergyFees(period, operator, now);
        int propertyInserted = repository.attachPropertyFees(period, operator, now);
        auditEventService.recordSuccess("merchant_expense.auto_synced", MANAGE_PERM,
                "merchant_expense_entry", period, null, request.idempotencyKey(),
                Map.of("period", period, "energy_inserted", energyInserted, "property_inserted", propertyInserted));
        return new MerchantExpenseSyncResultRecord(period, energyInserted, propertyInserted);
    }

    /** 商户 scope 账号强制只看/只记自己商户；平台账号透传请求参数。 */
    private String scopeMerchant(String requested) {
        Optional<String> scope = authorizationService.currentMerchantScope();
        return scope.orElse(requested);
    }

    private String currentOperator() {
        return authorizationService.currentActor().id();
    }

    private static String requirePeriod(String period) {
        String p = requireText(period, "period");
        if (!PERIOD_PATTERN.matcher(p).matches()) {
            throw ApiException.badRequest("VALIDATION_FAILED", "period must be yyyy-MM, e.g. 2026-07");
        }
        return p;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("VALIDATION_FAILED", field + " is required");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String stableToken(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
    }
}
