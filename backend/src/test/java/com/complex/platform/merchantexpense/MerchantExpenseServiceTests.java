package com.complex.platform.merchantexpense;

import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.merchantexpense.dto.MerchantExpenseEntryRequest;
import com.complex.platform.merchantexpense.dto.MerchantExpenseReversalRequest;
import com.complex.platform.merchantexpense.dto.MerchantExpenseSyncRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 纯内存单测：校验/幂等（含载荷指纹冲突）/冲销规则/商户 scope 强制（读+写），不触数据库。 */
class MerchantExpenseServiceTests {

    /** 内存仓储：只实现单测所需路径（自动挂接/漂移 SQL 属 JDBC 集成测试范畴，此处返回空）。 */
    private static class InMemoryRepo implements MerchantExpenseRepository {
        final Map<String, MerchantExpenseEntryRecord> rows = new LinkedHashMap<>();
        // 关账锁：键 = merchantId|period（关账判定 SQL 走 dividend_statements，此处以内存集合模拟）。
        final java.util.Set<String> lockedPeriods = new java.util.HashSet<>();

        void lock(String merchantId, String period) {
            lockedPeriods.add(merchantId + "|" + period);
        }

        @Override
        public boolean isPeriodLocked(String merchantId, String period) {
            return lockedPeriods.contains(merchantId + "|" + period);
        }

        @Override
        public List<String> listLockedMerchants(String period) {
            return lockedPeriods.stream()
                    .filter(key -> key.endsWith("|" + period))
                    .map(key -> key.substring(0, key.length() - period.length() - 1))
                    .distinct().sorted().toList();
        }

        @Override
        public PageResponse<MerchantExpenseEntryRecord> listEntries(
                String merchantId, String period, String expenseType, String source, int page, int pageSize) {
            List<MerchantExpenseEntryRecord> filtered = rows.values().stream()
                    .filter(r -> merchantId == null || r.merchantId().equals(merchantId))
                    .filter(r -> period == null || r.period().equals(period))
                    .filter(r -> expenseType == null || r.expenseType().equals(expenseType))
                    .filter(r -> source == null || r.source().equals(source))
                    .toList();
            return new PageResponse<>(filtered, page, pageSize, filtered.size());
        }

        @Override
        public List<MerchantExpenseSummaryRecord> summarize(String period, String merchantId) {
            Map<String, Long> totals = new LinkedHashMap<>();
            for (MerchantExpenseEntryRecord r : rows.values()) {
                if (!r.period().equals(period)) continue;
                if (merchantId != null && !r.merchantId().equals(merchantId)) continue;
                totals.merge(r.merchantId() + "|" + r.expenseType(), r.amountCent(), Long::sum);
            }
            List<MerchantExpenseSummaryRecord> out = new ArrayList<>();
            totals.forEach((key, total) -> {
                String[] parts = key.split("\\|");
                out.add(new MerchantExpenseSummaryRecord(parts[0], period, parts[1], total));
            });
            out.sort(Comparator.comparing(MerchantExpenseSummaryRecord::merchantId)
                    .thenComparing(MerchantExpenseSummaryRecord::expenseType));
            return out;
        }

        @Override
        public Optional<MerchantExpenseEntryRecord> findById(String id) {
            return Optional.ofNullable(rows.get(id));
        }

        @Override
        public boolean insertEntry(MerchantExpenseEntryRecord entry) {
            return rows.putIfAbsent(entry.id(), entry) == null;
        }

        @Override
        public int attachEnergyFees(String period, String operatorUserId, Instant now) {
            return 0;
        }

        @Override
        public int attachPropertyFees(String period, String operatorUserId, Instant now) {
            return 0;
        }

        @Override
        public int attachContractPropertyFees(String period, String operatorUserId, Instant now) {
            return 0;
        }

        @Override
        public int attachContractRent(String period, String operatorUserId, Instant now) {
            return 0;
        }

        @Override
        public List<MerchantExpenseDriftRecord> listAutoDrift(String period) {
            return List.of();
        }
    }

    /** 内存幂等仓储（RiskAlertServiceTests 同款）：载荷指纹与重放语义走真 IdempotencyService。 */

    private InMemoryRepo repository;
    private AuthorizationService authorizationService;
    private MerchantExpenseService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepo();
        authorizationService = mock(AuthorizationService.class);
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        when(authorizationService.currentActor()).thenReturn(
                new AccessActor("usr_admin", "platform_admin_user", "platform_admin", "org_1", null, "active"));
        AuditedOperationExecutor operations = new AuditedOperationExecutor(
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                mock(AuditEventService.class));
        service = new MerchantExpenseService(repository, authorizationService, operations);
    }

    private static MerchantExpenseEntryRequest manualRequest(String key, long amountCent, String note) {
        return new MerchantExpenseEntryRequest(key, "merchant-1", "2026-07", "labor", amountCent, note);
    }

    @Test
    void manualEntryRequiresNoteAndValidPeriodAndNonZeroAmount() {
        assertThatThrownBy(() -> service.createManual(manualRequest("k1", 100_00L, " ")))
                .isInstanceOf(ApiException.class).hasMessageContaining("note");
        assertThatThrownBy(() -> service.createManual(
                new MerchantExpenseEntryRequest("k2", "merchant-1", "2026-13", "labor", 100_00L, "七月人工")))
                .isInstanceOf(ApiException.class).hasMessageContaining("账期");
        assertThatThrownBy(() -> service.createManual(manualRequest("k3", 0L, "七月人工")))
                .isInstanceOf(ApiException.class).hasMessageContaining("金额");
        assertThatThrownBy(() -> service.createManual(
                new MerchantExpenseEntryRequest("k4", "merchant-1", "2026-07", "unknown_type", 100_00L, "七月人工")))
                .isInstanceOf(ApiException.class).hasMessageContaining("费用类型");
        assertThat(repository.rows).isEmpty();
    }

    @Test
    void manualEntryIsIdempotentBySameKeyAndPayload() {
        MerchantExpenseEntryRecord first = service.createManual(manualRequest("same-key", 500_00L, "七月人工"));
        MerchantExpenseEntryRecord replay = service.createManual(manualRequest("same-key", 500_00L, "七月人工"));
        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(repository.rows).hasSize(1);
        assertThat(first.source()).isEqualTo("manual");
        assertThat(first.sourceRef()).isNull();
        assertThat(first.operatorUserId()).isEqualTo("usr_admin");
    }

    @Test
    void manualEntrySameKeyDifferentPayloadIsRejected() {
        // I-1：同幂等键不同载荷（金额/商户/月份/备注任一不同）必须拒绝，不得静默返回第一次的行（分红基数会错）。
        service.createManual(manualRequest("dup-key", 500_00L, "七月人工"));
        assertThatThrownBy(() -> service.createManual(manualRequest("dup-key", 999_00L, "七月人工")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
        assertThatThrownBy(() -> service.createManual(
                new MerchantExpenseEntryRequest("dup-key", "merchant-2", "2026-07", "labor", 500_00L, "七月人工")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
        assertThat(repository.rows).hasSize(1);
    }

    @Test
    void reversalSameKeyDifferentNoteIsRejected() {
        // I-1（reverse 侧）：同键重放载荷必须逐字一致，note 不同也拒。
        MerchantExpenseEntryRecord original = service.createManual(manualRequest("k-orig-dup", 300_00L, "七月摊销"));
        service.reverse(original.id(), new MerchantExpenseReversalRequest("rev-key", "录错金额"));
        assertThatThrownBy(() -> service.reverse(original.id(),
                new MerchantExpenseReversalRequest("rev-key", "换了个理由")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void reversalNegatesOriginalOnceAndRejectsReversingAReversal() {
        MerchantExpenseEntryRecord original = service.createManual(manualRequest("k-orig", 300_00L, "七月摊销"));

        MerchantExpenseEntryRecord reversal = service.reverse(original.id(),
                new MerchantExpenseReversalRequest("k-rev", "录错金额，红字冲销"));
        assertThat(reversal.amountCent()).isEqualTo(-300_00L);
        assertThat(reversal.sourceRef()).isEqualTo(original.id());
        assertThat(reversal.isReversal()).isTrue();

        // 换新幂等键重复冲销同一原行：冲销行 id 由原行派生，幂等返回同一冲销行，不产生第二行。
        MerchantExpenseEntryRecord replay = service.reverse(original.id(),
                new MerchantExpenseReversalRequest("k-rev-2", "重复请求"));
        assertThat(replay.id()).isEqualTo(reversal.id());
        assertThat(repository.rows).hasSize(2);

        // 冲销行不可再被冲销。
        assertThatThrownBy(() -> service.reverse(reversal.id(),
                new MerchantExpenseReversalRequest("k-rev-3", "再冲一次")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("EXPENSE_REVERSAL_NOT_ALLOWED"));

        // 冲销后该商户该月该类型合计归零（分红费用基数口径）。
        assertThat(service.summarize("2026-07", "merchant-1"))
                .anySatisfy(row -> {
                    assertThat(row.expenseType()).isEqualTo("labor");
                    assertThat(row.totalCent()).isZero();
                });
    }

    @Test
    void reversalRequiresNoteAndExistingEntry() {
        assertThatThrownBy(() -> service.reverse("missing-id",
                new MerchantExpenseReversalRequest("k", "冲销理由")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("EXPENSE_ENTRY_NOT_FOUND"));
        MerchantExpenseEntryRecord original = service.createManual(manualRequest("k-orig2", 100_00L, "其他杂费"));
        assertThatThrownBy(() -> service.reverse(original.id(),
                new MerchantExpenseReversalRequest("k-rev", "  ")))
                .isInstanceOf(ApiException.class).hasMessageContaining("note");
    }

    @Test
    void merchantScopedActorIsForcedToOwnMerchant() {
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant-scope"));
        repository.rows.put("mex_a", new MerchantExpenseEntryRecord(
                "mex_a", "merchant-scope", "2026-07", "energy", 800_00L, "auto_energy", "arap_1", null, null, Instant.now()));
        repository.rows.put("mex_b", new MerchantExpenseEntryRecord(
                "mex_b", "merchant-other", "2026-07", "energy", 900_00L, "auto_energy", "arap_2", null, null, Instant.now()));

        PageResponse<MerchantExpenseEntryRecord> page = service.listEntries("merchant-other", null, null, null, 1, 50);
        assertThat(page.items()).extracting(MerchantExpenseEntryRecord::merchantId).containsOnly("merchant-scope");

        List<MerchantExpenseSummaryRecord> summary = service.summarize("2026-07", "merchant-other");
        assertThat(summary).extracting(MerchantExpenseSummaryRecord::merchantId).containsOnly("merchant-scope");
    }

    @Test
    void merchantScopedActorManualEntryForOtherMerchantIsRejected() {
        // I-2（写路径①，MerchantBillingService.createBillOnce 同款口径）：商户 scope 账号
        // 手工录入他商户费用 → 拒绝（CROSS_MERCHANT_FORBIDDEN），不静默改写归属。
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant-scope"));
        assertThatThrownBy(() -> service.createManual(
                new MerchantExpenseEntryRequest("scope-key", "merchant-other", "2026-07", "labor", 100_00L, "越权录入")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(repository.rows).isEmpty();
        // 记自己商户照常可录。
        MerchantExpenseEntryRecord own = service.createManual(
                new MerchantExpenseEntryRequest("scope-key-own", "merchant-scope", "2026-07", "labor", 100_00L, "自家人工"));
        assertThat(own.merchantId()).isEqualTo("merchant-scope");
    }

    @Test
    void platformActorCanCreateManualEntryForAnyMerchant() {
        // I-2 正例：平台账号（无商户 scope）可代任意商户录入。
        MerchantExpenseEntryRecord saved = service.createManual(
                new MerchantExpenseEntryRequest("platform-key", "merchant-any", "2026-07", "labor", 100_00L, "代录人工"));
        assertThat(saved.merchantId()).isEqualTo("merchant-any");
    }

    @Test
    void merchantScopedActorCannotReverseOtherMerchantEntry() {
        // I-2（写路径②）：商户 scope 账号不得冲销他商户的台账行（CROSS_MERCHANT_FORBIDDEN）。
        MerchantExpenseEntryRecord other = service.createManual(
                new MerchantExpenseEntryRequest("other-key", "merchant-other", "2026-07", "labor", 100_00L, "他商户人工"));
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant-scope"));
        assertThatThrownBy(() -> service.reverse(other.id(),
                new MerchantExpenseReversalRequest("scope-rev", "越权冲销")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(repository.rows).hasSize(1);
    }

    @Test
    void listRejectsUnknownFilterValues() {
        assertThatThrownBy(() -> service.listEntries(null, null, "bad-type", null, 1, 50))
                .isInstanceOf(ApiException.class).hasMessageContaining("费用类型");
        assertThatThrownBy(() -> service.listEntries(null, null, null, "bad-source", 1, 50))
                .isInstanceOf(ApiException.class).hasMessageContaining("source");
        assertThatThrownBy(() -> service.listEntries(null, "2026/07", null, null, 1, 50))
                .isInstanceOf(ApiException.class).hasMessageContaining("账期");
    }

    @Test
    void syncAutoRequiresIdempotencyKeyAndPeriod() {
        assertThatThrownBy(() -> service.syncAuto(new MerchantExpenseSyncRequest(null, "2026-07")))
                .isInstanceOf(ApiException.class).hasMessageContaining("idempotency_key");
        assertThatThrownBy(() -> service.syncAuto(new MerchantExpenseSyncRequest("k", "202607")))
                .isInstanceOf(ApiException.class).hasMessageContaining("账期");
        MerchantExpenseSyncResultRecord result = service.syncAuto(new MerchantExpenseSyncRequest("k2", "2026-07"));
        assertThat(result.energyInserted()).isZero();
        assertThat(result.propertyInserted()).isZero();
        assertThat(result.contractPropertyInserted()).isZero();
        assertThat(result.drift()).isEmpty();
        assertThat(result.lockedMerchants()).isEmpty();
    }

    // ── 分红期间关账锁 ────────────────────────────────────────

    @Test
    void manualEntryOnLockedPeriodIsRejected() {
        // 该商户该月已被 approved/paid 正式分红单覆盖（关账）→ 手工录入拒绝，不落库。
        repository.lock("merchant-1", "2026-07");
        assertThatThrownBy(() -> service.createManual(manualRequest("lk1", 100_00L, "关账后补录")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_PERIOD_LOCKED"));
        assertThat(repository.rows).isEmpty();
        // 未关账的其他月照常可录。
        MerchantExpenseEntryRecord open = service.createManual(
                new MerchantExpenseEntryRequest("lk1b", "merchant-1", "2026-08", "labor", 100_00L, "八月人工"));
        assertThat(open.period()).isEqualTo("2026-08");
    }

    @Test
    void reverseOnLockedPeriodIsRejected() {
        // 先在未关账时录入，随后该月关账 → 冲销被拒（已发放分红的基数不可改）。
        MerchantExpenseEntryRecord entry = service.createManual(manualRequest("lk2", 300_00L, "七月人工"));
        repository.lock("merchant-1", "2026-07");
        assertThatThrownBy(() -> service.reverse(entry.id(),
                new MerchantExpenseReversalRequest("lk2-rev", "关账后想冲销")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_PERIOD_LOCKED"));
        assertThat(repository.rows).hasSize(1);
    }

    @Test
    void syncAutoListsLockedMerchantsAsSkipped() {
        // 该月两个商户关账 → 自动挂接把它们列入 lockedMerchants 跳过清单（SQL 侧 NOT EXISTS 已跳过挂接）。
        repository.lock("merchant-x", "2026-07");
        repository.lock("merchant-y", "2026-07");
        MerchantExpenseSyncResultRecord result = service.syncAuto(new MerchantExpenseSyncRequest("lk3", "2026-07"));
        assertThat(result.lockedMerchants()).containsExactly("merchant-x", "merchant-y");
        // 其他月无关账 → 清单空。
        MerchantExpenseSyncResultRecord august = service.syncAuto(new MerchantExpenseSyncRequest("lk3b", "2026-08"));
        assertThat(august.lockedMerchants()).isEmpty();
    }
}
