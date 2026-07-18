package com.complex.platform.merchantexpense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.merchantexpense.dto.MerchantExpenseEntryRequest;
import com.complex.platform.merchantexpense.dto.MerchantExpenseReversalRequest;
import com.complex.platform.merchantexpense.dto.MerchantExpenseSyncRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** 纯内存单测：校验/幂等/冲销规则/商户 scope 强制，不触数据库。 */
class MerchantExpenseServiceTests {

    /** 内存仓储：只实现单测所需路径（自动挂接 SQL 属 JDBC 集成测试范畴，此处返回 0）。 */
    private static class InMemoryRepo implements MerchantExpenseRepository {
        final Map<String, MerchantExpenseEntryRecord> rows = new LinkedHashMap<>();

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
    }

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
        service = new MerchantExpenseService(repository, authorizationService, mock(AuditEventService.class));
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
                .isInstanceOf(ApiException.class).hasMessageContaining("period");
        assertThatThrownBy(() -> service.createManual(manualRequest("k3", 0L, "七月人工")))
                .isInstanceOf(ApiException.class).hasMessageContaining("amount_cent");
        assertThatThrownBy(() -> service.createManual(
                new MerchantExpenseEntryRequest("k4", "merchant-1", "2026-07", "unknown_type", 100_00L, "七月人工")))
                .isInstanceOf(ApiException.class).hasMessageContaining("expense_type");
        assertThat(repository.rows).isEmpty();
    }

    @Test
    void manualEntryIsIdempotentBySameKey() {
        MerchantExpenseEntryRecord first = service.createManual(manualRequest("same-key", 500_00L, "七月人工"));
        MerchantExpenseEntryRecord replay = service.createManual(manualRequest("same-key", 500_00L, "七月人工"));
        assertThat(replay.id()).isEqualTo(first.id());
        assertThat(repository.rows).hasSize(1);
        assertThat(first.source()).isEqualTo("manual");
        assertThat(first.sourceRef()).isNull();
        assertThat(first.operatorUserId()).isEqualTo("usr_admin");
    }

    @Test
    void reversalNegatesOriginalOnceAndRejectsReversingAReversal() {
        MerchantExpenseEntryRecord original = service.createManual(manualRequest("k-orig", 300_00L, "七月摊销"));

        MerchantExpenseEntryRecord reversal = service.reverse(original.id(),
                new MerchantExpenseReversalRequest("k-rev", "录错金额，红字冲销"));
        assertThat(reversal.amountCent()).isEqualTo(-300_00L);
        assertThat(reversal.sourceRef()).isEqualTo(original.id());
        assertThat(reversal.isReversal()).isTrue();

        // 重复冲销同一原行：幂等返回同一冲销行，不产生第二行。
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
    void listRejectsUnknownFilterValues() {
        assertThatThrownBy(() -> service.listEntries(null, null, "bad-type", null, 1, 50))
                .isInstanceOf(ApiException.class).hasMessageContaining("expense_type");
        assertThatThrownBy(() -> service.listEntries(null, null, null, "bad-source", 1, 50))
                .isInstanceOf(ApiException.class).hasMessageContaining("source");
        assertThatThrownBy(() -> service.listEntries(null, "2026/07", null, null, 1, 50))
                .isInstanceOf(ApiException.class).hasMessageContaining("period");
    }

    @Test
    void syncAutoRequiresIdempotencyKeyAndPeriod() {
        assertThatThrownBy(() -> service.syncAuto(new MerchantExpenseSyncRequest(null, "2026-07")))
                .isInstanceOf(ApiException.class).hasMessageContaining("idempotency_key");
        assertThatThrownBy(() -> service.syncAuto(new MerchantExpenseSyncRequest("k", "202607")))
                .isInstanceOf(ApiException.class).hasMessageContaining("period");
        MerchantExpenseSyncResultRecord result = service.syncAuto(new MerchantExpenseSyncRequest("k", "2026-07"));
        assertThat(result.energyInserted()).isZero();
        assertThat(result.propertyInserted()).isZero();
    }
}
