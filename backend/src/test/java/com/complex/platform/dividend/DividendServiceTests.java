package com.complex.platform.dividend;

// 文件版本：V0.05

import com.complex.platform.support.InMemoryIdempotencyRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AuthorizationService;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.common.api.PageResponse;
import com.complex.platform.common.idempotency.IdempotencyRecord;
import com.complex.platform.common.idempotency.IdempotencyRepository;
import com.complex.platform.common.idempotency.IdempotencyService;
import com.complex.platform.dividend.DividendRecords.DividendComputeResultRecord;
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
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.FinanceService.DividendArrearsItem;
import com.complex.platform.finance.FinanceService.DividendReserveReleaseRef;
import com.complex.platform.inventory.InventoryCostSummaryRecord;
import com.complex.platform.inventory.InventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 纯内存单测：方案配置校验与唯一性/期间作业算式与快照/状态机（预览不可审批）/审批挂应付/
 * 商户 scope 强制/幂等载荷指纹，不触数据库。三来源取数由内存仓储与 mock 注入。
 * V303 扣除项三开关：亏损结转跨期结转/欠款抵扣削减实发并结算应收/发放保护线留存/默认全关=现状。
 */
class DividendServiceTests {

    private static final String MERCHANT = "merchant_a";

    /** 内存仓储：方案唯一性/计算单状态机/明细行 + 可设的三来源取数值。 */
    private static class InMemoryRepo implements DividendRepository {
        final Map<String, DividendPlanRecord> plans = new LinkedHashMap<>();
        final Map<String, DividendStatementRecord> statements = new LinkedHashMap<>();
        final List<DividendStatementLineRecord> lines = new ArrayList<>();
        final Map<String, Long> barRevenue = new HashMap<>();
        final List<Instant[]> barRevenueWindows = new ArrayList<>();
        final Map<String, Long> barRefund = new HashMap<>();
        final Map<String, List<ExpenseTypeSumRecord>> expenses = new HashMap<>();
        // FS-015：已确认月度分账结算额 / 未确认月份 / 单店门票综合服务费。
        final Map<String, Long> ticketSettled = new HashMap<>();
        List<String> unconfirmedTicketMonths = List.of();
        final Map<String, Long> singleBarServiceFee = new HashMap<>();
        // FS-014：依法核定（商户|期间）；legalDefault=true 时未登记的期间按「核定额足够大」处理（既有用例口径不变）。
        final Map<String, DividendRecords.DividendLegalConfirmationRecord> legal = new HashMap<>();
        boolean legalDefault = true;
        // V309 自定义扣除项按方案存（模拟子表）；读方案时装填回记录（镜像 jdbc mapPlan）。
        final Map<String, List<DividendRecords.DividendCustomDeductionRecord>> customDeductions = new HashMap<>();

        /** 装填自定义扣除项（读路径镜像 jdbc mapPlan：从子表读回附到方案记录）。 */
        private DividendPlanRecord enrich(DividendPlanRecord p) {
            return new DividendPlanRecord(p.id(), p.merchantId(), p.principalUserId(), p.principalName(),
                    p.platformShareBps(), p.taxRateBps(), p.periodType(), p.revenueDomains(),
                    p.includeInventoryCost(), p.expenseTypes(), p.effectiveFrom(), p.status(), p.note(),
                    p.updatedBy(), p.createdAt(), p.updatedAt(), p.lossCarryforwardEnabled(),
                    p.arrearsOffsetEnabled(), p.payoutFloorEnabled(), p.payoutFloorBps(), p.payoutFloorFixedCent(),
                    p.reserveReleaseMode(), p.reserveReleaseDelayPeriods(), p.principalType(),
                    customDeductions.getOrDefault(p.id(), List.of()));
        }

        @Override
        public List<DividendPlanRecord> listPlans(String merchantId) {
            return plans.values().stream()
                    .filter(p -> merchantId == null || p.merchantId().equals(merchantId)).map(this::enrich).toList();
        }

        @Override
        public Optional<DividendPlanRecord> findPlanById(String id) {
            return Optional.ofNullable(plans.get(id)).map(this::enrich);
        }

        @Override
        public Optional<DividendPlanRecord> findActivePlanByMerchant(String merchantId) {
            return plans.values().stream()
                    .filter(p -> p.merchantId().equals(merchantId) && "active".equals(p.status()))
                    .map(this::enrich).findFirst();
        }

        @Override
        public List<DividendPlanRecord> listActivePlans() {
            return plans.values().stream().filter(p -> "active".equals(p.status())).map(this::enrich).toList();
        }

        @Override
        public List<DividendRecords.DividendCustomDeductionRecord> listCustomDeductions(String planId) {
            return customDeductions.getOrDefault(planId, List.of());
        }

        @Override
        public void replaceCustomDeductions(
                String planId, List<DividendRecords.DividendCustomDeductionRecord> items) {
            customDeductions.put(planId, List.copyOf(items));
        }

        @Override
        public boolean insertPlan(DividendPlanRecord plan) {
            if ("active".equals(plan.status()) && findActivePlanByMerchant(plan.merchantId()).isPresent()) {
                return false;
            }
            return plans.putIfAbsent(plan.id(), plan) == null;
        }

        @Override
        public boolean updatePlan(DividendPlanRecord plan) {
            // uq_dividend_plans_active_merchant 内存版：ended 改回 active 时同商户已有另一生效方案 → false。
            if ("active".equals(plan.status()) && plans.values().stream().anyMatch(p ->
                    !p.id().equals(plan.id()) && p.merchantId().equals(plan.merchantId())
                            && "active".equals(p.status()))) {
                return false;
            }
            plans.put(plan.id(), plan);
            return true;
        }

        @Override
        public PageResponse<DividendStatementRecord> pageStatements(
                String merchantId, String kind, String periodLabel, String status, int page, int pageSize) {
            List<DividendStatementRecord> filtered = statements.values().stream()
                    .filter(s -> merchantId == null || s.merchantId().equals(merchantId))
                    .filter(s -> kind == null || s.kind().equals(kind))
                    .filter(s -> periodLabel == null || s.periodLabel().equals(periodLabel))
                    .filter(s -> status == null || s.status().equals(status))
                    .toList();
            return new PageResponse<>(filtered, page, pageSize, filtered.size());
        }

        @Override
        public Optional<DividendStatementRecord> findStatementById(String id) {
            return Optional.ofNullable(statements.get(id));
        }

        @Override
        public Optional<DividendStatementRecord> findActiveStatement(String merchantId, String kind, String periodLabel) {
            return statements.values().stream()
                    .filter(s -> s.merchantId().equals(merchantId) && s.kind().equals(kind)
                            && s.periodLabel().equals(periodLabel) && !"voided".equals(s.status()))
                    .findFirst();
        }

        @Override
        public long priorLossCarryforwardCent(String merchantId, java.time.LocalDate periodStart) {
            // 该商户 periodStart 之前最近一张已结正式单的结转链余额；draft/preview/voided 不计。
            return statements.values().stream()
                    .filter(s -> s.merchantId().equals(merchantId) && "official".equals(s.kind())
                            && ("approved".equals(s.status()) || "paid".equals(s.status()))
                            && s.periodEnd().isBefore(periodStart))
                    .max(java.util.Comparator.comparing(DividendStatementRecord::periodEnd)
                            .thenComparing(DividendStatementRecord::createdAt))
                    .map(DividendStatementRecord::lossCarryforwardAfterCent)
                    .orElse(0L);
        }

        @Override
        public void insertStatement(DividendStatementRecord statement) {
            statements.put(statement.id(), statement);
        }

        @Override
        public void insertLine(DividendStatementLineRecord line) {
            lines.add(line);
        }

        @Override
        public List<DividendStatementLineRecord> listLines(String statementId, String merchantId) {
            return lines.stream()
                    .filter(l -> l.statementId().equals(statementId))
                    .filter(l -> merchantId == null || l.merchantId().equals(merchantId))
                    .toList();
        }

        /** C-1 内存版乐观守卫：仅当前态命中期望集合才变更并返回 1，否则返回 0（与 SQL AND status=... 同语义）。 */
        private int mutateStatus(String id, java.util.Set<String> expectedStatuses,
                java.util.function.UnaryOperator<DividendStatementRecord> mutation) {
            DividendStatementRecord current = statements.get(id);
            if (current == null || !expectedStatuses.contains(current.status())) {
                return 0;
            }
            statements.put(id, mutation.apply(current));
            return 1;
        }

        private static DividendStatementRecord withStatus(DividendStatementRecord s, String status,
                String rejectReason, String arApItemId, String approvedBy, String submittedBy, String paidBy) {
            return new DividendStatementRecord(s.id(), s.merchantId(), s.planId(), s.kind(), s.periodLabel(),
                    s.periodStart(), s.periodEnd(), s.principalUserId(), s.platformShareBps(), s.taxRateBps(),
                    s.revenueDomains(), s.includeInventoryCost(), s.expenseTypes(), s.revenueCent(), s.costCent(),
                    s.expenseCent(), s.netProfitCent(), s.distributableCent(), s.platformShareCent(),
                    s.principalGrossCent(), s.taxCent(), s.principalNetCent(), s.noCostBasisCount(), status,
                    rejectReason != null ? rejectReason : s.rejectReason(),
                    arApItemId != null ? arApItemId : s.arApItemId(),
                    s.computedBy(),
                    submittedBy != null ? submittedBy : s.submittedBy(), s.submittedAt(),
                    approvedBy != null ? approvedBy : s.approvedBy(), s.approvedAt(),
                    paidBy != null ? paidBy : s.paidBy(), s.paidAt(), s.createdAt(), s.updatedAt(),
                    s.lossCarryforwardEnabled(), s.lossCarryforwardInCent(), s.lossCarryforwardAppliedCent(),
                    s.lossCarryforwardAfterCent(), s.arrearsOffsetCent(), s.reserveWithheldCent(), s.payoutCent(),
                    s.principalType(), s.customDeductionCent(), s.legalDistributableCent());
        }

        @Override
        public int markVoided(String id, String actor, Instant now) {
            return mutateStatus(id, java.util.Set.of("preview", "draft", "rejected"),
                    s -> withStatus(s, "voided", null, null, null, null, null));
        }

        @Override
        public int markSubmitted(String id, String actor, Instant now) {
            return mutateStatus(id, java.util.Set.of("draft"),
                    s -> withStatus(s, "pending_review", null, null, null, actor, null));
        }

        @Override
        public int markApproved(String id, String actor, String arApItemId, Instant now) {
            return mutateStatus(id, java.util.Set.of("pending_review"),
                    s -> withStatus(s, "approved", null, arApItemId, actor, null, null));
        }

        @Override
        public int markRejected(String id, String actor, String reason, Instant now) {
            return mutateStatus(id, java.util.Set.of("pending_review"),
                    s -> withStatus(s, "rejected", reason, null, null, null, null));
        }

        @Override
        public int markPaid(String id, String actor, Instant now) {
            return mutateStatus(id, java.util.Set.of("approved"),
                    s -> withStatus(s, "paid", null, null, null, null, actor));
        }

        @Override
        public long sumBarRevenueCent(String merchantId, Instant from, Instant to) {
            barRevenueWindows.add(new Instant[] {from, to});
            return barRevenue.getOrDefault(merchantId, 0L);
        }

        @Override
        public long sumBarRefundCent(String merchantId, Instant from, Instant to) {
            return barRefund.getOrDefault(merchantId, 0L);
        }

        @Override
        public long sumTicketSettledCent(String merchantId, java.time.LocalDate start, java.time.LocalDate end) {
            return ticketSettled.getOrDefault(merchantId, 0L);
        }

        @Override
        public List<String> listUnconfirmedTicketSettlementMonths(List<String> months) {
            return unconfirmedTicketMonths.stream().filter(months::contains).toList();
        }

        @Override
        public long sumSingleBarServiceFeeCent(String merchantId, java.time.LocalDate start, java.time.LocalDate end) {
            return singleBarServiceFee.getOrDefault(merchantId, 0L);
        }

        @Override
        public Optional<DividendRecords.DividendLegalConfirmationRecord> findLegalConfirmation(
                String merchantId, String periodLabel) {
            DividendRecords.DividendLegalConfirmationRecord found = legal.get(merchantId + "|" + periodLabel);
            if (found == null && legalDefault) {
                found = new DividendRecords.DividendLegalConfirmationRecord("dlc_default", merchantId, periodLabel,
                        1_000_000_000_000L, "测试默认财税核算", "测试默认分配决议", null, "usr_fin", Instant.EPOCH);
            }
            return Optional.ofNullable(found);
        }

        @Override
        public void upsertLegalConfirmation(DividendRecords.DividendLegalConfirmationRecord c) {
            legal.put(c.merchantId() + "|" + c.periodLabel(), c);
        }

        @Override
        public List<ExpenseTypeSumRecord> sumExpenseByType(
                String merchantId, List<String> months, List<String> expenseTypes) {
            return expenses.getOrDefault(merchantId, List.of()).stream()
                    .filter(e -> expenseTypes.contains(e.expenseType())).toList();
        }

        // ── V304 发放保护线留存·释放 ──────────────────────────
        final Map<String, DividendReserveReleaseRecord> reserveReleases = new LinkedHashMap<>();

        @Override
        public boolean insertReserveRelease(DividendReserveReleaseRecord release) {
            // uq(statement_id) 内存版：同一计算单只允许一条释放行（重复释放 → false）。
            return reserveReleases.putIfAbsent(release.statementId(), release) == null;
        }

        @Override
        public PageResponse<DividendReserveLedgerRecord> pageReserveLedger(
                String merchantId, String releasedFilter, int page, int pageSize) {
            List<DividendReserveLedgerRecord> filtered = statements.values().stream()
                    .filter(s -> "official".equals(s.kind()))
                    .filter(s -> "approved".equals(s.status()) || "paid".equals(s.status()))
                    .filter(s -> s.reserveWithheldCent() > 0)
                    .filter(s -> merchantId == null || s.merchantId().equals(merchantId))
                    .map(s -> {
                        DividendReserveReleaseRecord r = reserveReleases.get(s.id());
                        return new DividendReserveLedgerRecord(s.id(), s.merchantId(), s.principalUserId(),
                                s.periodLabel(), s.periodStart(), s.periodEnd(), s.status(), s.reserveWithheldCent(),
                                r != null, r == null ? null : r.triggerType(), r == null ? null : r.releasedBy(),
                                r == null ? null : r.releasedAt());
                    })
                    .filter(l -> releasedFilter == null
                            || ("released".equals(releasedFilter) && l.released())
                            || ("pending".equals(releasedFilter) && !l.released()))
                    .toList();
            return new PageResponse<>(filtered, page, pageSize, filtered.size());
        }

        @Override
        public List<DividendReserveReleaseCandidateRecord> listReserveReleaseCandidates() {
            return statements.values().stream()
                    .filter(s -> "official".equals(s.kind()))
                    .filter(s -> "approved".equals(s.status()) || "paid".equals(s.status()))
                    .filter(s -> s.reserveWithheldCent() > 0)
                    .filter(s -> !reserveReleases.containsKey(s.id()))
                    .map(s -> {
                        DividendPlanRecord plan = plans.get(s.planId());
                        return new DividendReserveReleaseCandidateRecord(s.id(), s.merchantId(),
                                s.principalUserId(), s.reserveWithheldCent(), s.periodEnd(),
                                plan == null ? "quarterly" : plan.periodType(),
                                plan == null ? "manual" : plan.reserveReleaseMode(),
                                plan == null ? 1 : plan.reserveReleaseDelayPeriods());
                    })
                    .toList();
        }
    }

    /** 内存幂等仓储（MerchantExpenseServiceTests 同款）：载荷指纹与重放语义走真 IdempotencyService。 */

    private InMemoryRepo repository;
    private AuthorizationService authorizationService;
    private InventoryRepository inventoryRepository;
    private FinanceService payoutPort;
    private DividendService service;
    // I-1 职责分离测试用：可切换当前操作人（提交与审批必须不同人）。
    private final String[] actorId = {"usr_admin"};

    @BeforeEach
    void setUp() {
        actorId[0] = "usr_admin";
        repository = new InMemoryRepo();
        authorizationService = mock(AuthorizationService.class);
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.empty());
        when(authorizationService.currentActor()).thenAnswer(invocation ->
                new AccessActor(actorId[0], "platform_admin_user", "platform_admin", "org_1", null, "active"));
        inventoryRepository = mock(InventoryRepository.class);
        when(inventoryRepository.sumOutboundCost(anyString(), any(), anyString(), anyString(), any(), any()))
                .thenReturn(new InventoryCostSummaryRecord(MERCHANT, "2026-07-01", "2026-09-30", 0L, 0L, 0L));
        payoutPort = mock(FinanceService.class);
        when(payoutPort.createDividendPayoutPayable(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn("arap_div_1");
        // V303 默认：无未清应收；欠款抵扣默认足额结算（返回请求的抵扣额）。
        when(payoutPort.listOutstandingReceivables(anyString())).thenReturn(List.of());
        when(payoutPort.applyDividendOffsetToReceivables(anyString(), anyLong(), anyString()))
                .thenAnswer(inv -> (long) inv.getArgument(1));
        when(payoutPort.createDividendReserveWithholdingPayable(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn("arap_reserve_1");
        // V304 释放：finance re-tag 待释放留存应付（幂等）——默认返回该计算单留存额的释放引用；
        // 留存为 0 或计算单不存在 → empty（真 finance 侧无待释放应付时的 empty 语义）。
        when(payoutPort.releaseDividendReserveWithholding(anyString())).thenAnswer(inv -> {
            String sid = inv.getArgument(0);
            return repository.findStatementById(sid)
                    .filter(s -> s.reserveWithheldCent() > 0)
                    .map(s -> Optional.of(new DividendReserveReleaseRef(
                            "arap_reserve_" + sid, s.reserveWithheldCent(), s.principalUserId(), s.merchantId())))
                    .orElse(Optional.empty());
        });
        AuditedOperationExecutor operations = new AuditedOperationExecutor(
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                mock(AuditEventService.class));
        service = new DividendService(repository, inventoryRepository, new BusinessDayResolver(),
                authorizationService, operations, payoutPort);
    }

    /** 季度方案（多数用例按 2026-Q3 出单）；默认周期见 {@link #createPlanDefaultsToSemiannualPeriod}。 */
    private DividendPlanRecord createDefaultPlan() {
        return service.createPlan(new DividendPlanRequest("plan-key-1", MERCHANT, "usr_principal", "主理人甲",
                null, null, "quarterly", null, null, null, null, null, null, null, null, null, null, null));
    }

    private DividendStatementRecord computeOfficialQuarter() {
        DividendComputeResultRecord result = service.compute(
                new DividendComputeRequest("compute-key-1", "2026-Q3", "official", null));
        assertThat(result.computed()).hasSize(1);
        return result.computed().get(0);
    }

    @Test
    void createPlanFillsDefaultsAndEnforcesSingleActivePerMerchant() {
        DividendPlanRecord plan = createDefaultPlan();
        // 默认口径：80/20、股息红利 20%、收入域 bar+ticket、扣成本、费用全类别、三扣除全关（周期默认见下一用例）。
        assertThat(plan.platformShareBps()).isEqualTo(8000);
        assertThat(plan.taxRateBps()).isEqualTo(2000);
        assertThat(plan.revenueDomains()).containsExactly("bar", "ticket");
        assertThat(plan.includeInventoryCost()).isTrue();
        assertThat(plan.expenseTypes()).containsExactly(
                "rent_allocation", "energy", "property_fee", "labor", "amortization", "other");
        assertThat(plan.status()).isEqualTo("active");
        assertThat(plan.lossCarryforwardEnabled()).isFalse();
        assertThat(plan.arrearsOffsetEnabled()).isFalse();
        assertThat(plan.payoutFloorEnabled()).isFalse();
        assertThat(plan.payoutFloorBps()).isZero();
        assertThat(plan.payoutFloorFixedCent()).isZero();
        // 同商户第二个生效方案被拒（一个商户同一时期只允许一个生效中方案）。
        assertThatThrownBy(() -> service.createPlan(new DividendPlanRequest("plan-key-2", MERCHANT,
                "usr_other", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_PLAN_EXISTS"));
    }

    /** BSM L1777「分红按所配周期一期一分配，默认周期为半年」。 */
    @Test
    void createPlanDefaultsToSemiannualPeriod() {
        DividendPlanRecord plan = service.createPlan(new DividendPlanRequest("plan-default-period", "merchant_semi",
                "usr_p_semi", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        assertThat(plan.periodType()).isEqualTo("semiannual");
    }

    /**
     * BSM L1777「分红周期一律取月及月的整数倍粒度（如月、季、半年、年）……分红按所配周期一期一分配……每月另出一张只读的预览单」：
     * 半年方案按 2026-H1/H2、年度方案按 2026 出正式单，标签与方案周期不符的跳过；两者都出月度预览。
     */
    @Test
    void semiannualAndAnnualPlansComputeByMatchingLabelsAndGetMonthlyPreview() {
        service.createPlan(new DividendPlanRequest("plan-semi", "merchant_semi", "usr_p_semi", null,
                null, null, "semiannual", null, null, null, null, null, null, null, null, null, null, null));
        service.createPlan(new DividendPlanRequest("plan-annual", "merchant_annual", "usr_p_annual", null,
                null, null, "annual", null, null, null, null, null, null, null, null, null, null, null));

        DividendComputeResultRecord half = service.compute(
                new DividendComputeRequest("semi-h2", "2026-H2", "official", null));
        assertThat(half.computed()).singleElement().satisfies(s -> {
            assertThat(s.merchantId()).isEqualTo("merchant_semi");
            assertThat(s.periodStart()).isEqualTo(java.time.LocalDate.parse("2026-07-01"));
            assertThat(s.periodEnd()).isEqualTo(java.time.LocalDate.parse("2026-12-31"));
        });
        assertThat(half.skipped()).singleElement().satisfies(s -> assertThat(s.reason()).contains("年度"));

        DividendComputeResultRecord year = service.compute(
                new DividendComputeRequest("annual-2026", "2026", "official", null));
        assertThat(year.computed()).singleElement().satisfies(s -> {
            assertThat(s.merchantId()).isEqualTo("merchant_annual");
            assertThat(s.periodStart()).isEqualTo(java.time.LocalDate.parse("2026-01-01"));
            assertThat(s.periodEnd()).isEqualTo(java.time.LocalDate.parse("2026-12-31"));
        });
        assertThat(year.skipped()).singleElement().satisfies(s -> assertThat(s.reason()).contains("半年"));

        assertThat(service.compute(new DividendComputeRequest("preview-semi", "2026-07", "preview", null)).computed())
                .hasSize(2);
    }

    @Test
    void createPlanValidatesBpsAndTokens() {
        assertThatThrownBy(() -> service.createPlan(new DividendPlanRequest("bad-bps", MERCHANT,
                "usr_p", null, 10001, null, null, null, null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.createPlan(new DividendPlanRequest("bad-domain", MERCHANT,
                "usr_p", null, null, null, null, List.of("bar", "karaoke"), null, null, null, null, null,
                null, null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VALIDATION_FAILED"));
        // V303 保护线固定额负值被拒。
        assertThatThrownBy(() -> service.createPlan(new DividendPlanRequest("bad-floor", MERCHANT,
                "usr_p", null, null, null, null, null, null, null, null, null, null, null, null, true, 0, -1L)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void updatePlanChangesRatioButNotMerchant() {
        DividendPlanRecord plan = createDefaultPlan();
        DividendPlanRecord updated = service.updatePlan(plan.id(), new DividendPlanRequest("upd-key-1", null,
                null, null, 7000, 0, null, null, null, null, null, null, "比例调整为 70/30，免税试点",
                null, null, null, null, null));
        assertThat(updated.platformShareBps()).isEqualTo(7000);
        assertThat(updated.taxRateBps()).isZero();
        assertThat(updated.principalUserId()).isEqualTo("usr_principal");
        assertThatThrownBy(() -> service.updatePlan(plan.id(), new DividendPlanRequest("upd-key-2",
                "merchant_b", null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VALIDATION_FAILED"));
    }

    /**
     * 第十八批第 1 条（ENUM:187「按每个项目、每家酒吧可配」）：方案商户是已批准 12:00 起算的酒吧时，酒吧收入与销货成本按该酒吧营业日窗取数——
     * 2026-Q3 窗为 [北京 07-01 12:00, 北京 10-01 12:00)，不按出厂 10:00。解析器桩替身该酒吧的已批准版本。
     */
    @Test
    void barMerchantPeriodWindowFollowsTheBarsBusinessDay() {
        BusinessDayResolver noonBar = new BusinessDayResolver() {
            @Override
            public String barOfMerchant(String merchantId) {
                return MERCHANT.equals(merchantId) ? merchantId : null;
            }

            @Override
            public Instant start(String projectId, String barId, java.time.LocalDate date) {
                return barId == null ? super.start(projectId, null, date)
                        : date.atTime(12, 0).atZone(com.complex.platform.common.LocalZone.ID).toInstant();
            }

            @Override
            public Instant end(String projectId, String barId, java.time.LocalDate date) {
                return barId == null ? super.end(projectId, null, date)
                        : date.plusDays(1).atTime(12, 0).atZone(com.complex.platform.common.LocalZone.ID).toInstant();
            }
        };
        service = new DividendService(repository, inventoryRepository, noonBar, authorizationService,
                new AuditedOperationExecutor(new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                        mock(AuditEventService.class)), payoutPort);
        createDefaultPlan();
        computeOfficialQuarter();
        Instant from = Instant.parse("2026-07-01T04:00:00Z");
        Instant to = Instant.parse("2026-10-01T04:00:00Z");
        assertThat(repository.barRevenueWindows).anySatisfy(window -> assertThat(window).containsExactly(from, to));
        verify(inventoryRepository).sumOutboundCost(org.mockito.ArgumentMatchers.eq(MERCHANT), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("2026-07-01"), org.mockito.ArgumentMatchers.eq("2026-09-30"),
                org.mockito.ArgumentMatchers.eq(from), org.mockito.ArgumentMatchers.eq(to));
    }

    @Test
    void computeProducesIdentityConsistentStatementWithSnapshotAndLines() {
        createDefaultPlan();
        repository.barRevenue.put(MERCHANT, 800_000L);
        repository.barRefund.put(MERCHANT, 50_000L);
        repository.ticketSettled.put(MERCHANT, 270_000L);
        // 2026-07-18 拍板：租金计入费用台账（auto_rent 自动挂接 rent_allocation），默认方案默认扣减。
        repository.expenses.put(MERCHANT, List.of(
                new ExpenseTypeSumRecord("labor", 200_000L),
                new ExpenseTypeSumRecord("energy", 20_000L),
                new ExpenseTypeSumRecord("property_fee", 30_000L),
                new ExpenseTypeSumRecord("rent_allocation", 100_000L)));
        when(inventoryRepository.sumOutboundCost(anyString(), any(), anyString(), anyString(), any(), any()))
                .thenReturn(new InventoryCostSummaryRecord(MERCHANT, "2026-07-01", "2026-09-30", 150_000L, 12L, 2L));

        DividendStatementRecord statement = computeOfficialQuarter();
        // 收入 = (800000−50000)+(300000−30000)=1020000；
        // 净利 = 收入−成本−能耗−物业−人工−租金 = 1020000−150000−(200000+20000+30000+100000) = 520000。
        assertThat(statement.revenueCent()).isEqualTo(1_020_000L);
        assertThat(statement.costCent()).isEqualTo(150_000L);
        assertThat(statement.expenseCent()).isEqualTo(350_000L);
        assertThat(statement.netProfitCent()).isEqualTo(520_000L);
        assertThat(statement.platformShareCent()).isEqualTo(416_000L);
        assertThat(statement.principalGrossCent()).isEqualTo(104_000L);
        assertThat(statement.taxCent()).isEqualTo(20_800L);
        assertThat(statement.principalNetCent()).isEqualTo(83_200L);
        // V303 默认全关：实发 = 税后应得，三扣除全 0。
        assertThat(statement.payoutCent()).isEqualTo(83_200L);
        assertThat(statement.lossCarryforwardEnabled()).isFalse();
        assertThat(statement.arrearsOffsetCent()).isZero();
        assertThat(statement.reserveWithheldCent()).isZero();
        // 租金以独立算式行留痕。
        assertThat(repository.listLines(statement.id(), null))
                .anyMatch(l -> l.lineType().equals("expense_rent_allocation") && l.amountCent() == -100_000L);
        assertThat(statement.noCostBasisCount()).isEqualTo(2L);
        assertThat(statement.status()).isEqualTo("draft");
        assertThat(statement.periodStart()).isEqualTo(java.time.LocalDate.parse("2026-07-01"));
        assertThat(statement.periodEnd()).isEqualTo(java.time.LocalDate.parse("2026-09-30"));
        // 配置快照冻结在单上。
        assertThat(statement.platformShareBps()).isEqualTo(8000);
        assertThat(statement.taxRateBps()).isEqualTo(2000);
        // 明细行：来源行合计 = 净利润（算式留痕数字恒等）。三扣除全关时不额外落扣除行。
        List<DividendStatementLineRecord> lines = repository.listLines(statement.id(), null);
        long sourceSum = lines.stream()
                .filter(l -> !List.of("net_profit", "loss_carryforward", "legal_cap", "platform_share",
                        "principal_gross", "tax_withheld", "principal_net", "arrears_offset", "reserve_withheld",
                        "payout").contains(l.lineType()))
                .mapToLong(DividendStatementLineRecord::amountCent).sum();
        assertThat(sourceSum).isEqualTo(statement.netProfitCent());
        assertThat(lines).noneMatch(l -> l.lineType().equals("loss_carryforward"));
        assertThat(lines).noneMatch(l -> l.lineType().equals("arrears_offset"));
        assertThat(lines).noneMatch(l -> l.lineType().equals("reserve_withheld"));
        // 实发行始终落痕。
        assertThat(lines).anyMatch(l -> l.lineType().equals("payout") && l.amountCent() == 83_200L);
        assertThat(lines).anyMatch(l -> l.lineType().equals("cost_inventory") && l.detail().contains("无成本基准"));
    }

    // ── V507 FS-014 依法可分配金额前置（BSM L1785、L1786；PRD §6；裁决 2026-10-19 第 1 条「取小、不补」）──

    private static DividendLegalConfirmationRequest legalRequest(String key, long cent) {
        return new DividendLegalConfirmationRequest(key, cent, "财税核算报告 TA-2026Q3-001", "股东会分配决议 R-2026-10-08", null);
    }

    @Test
    void submitWithoutLegalConfirmationIsRejected() {
        repository.legalDefault = false;
        createDefaultPlan();
        repository.barRevenue.put(MERCHANT, 1_000_000L);
        DividendStatementRecord draft = computeOfficialQuarter();
        assertThat(draft.legalDistributableCent()).isNull();
        assertThatThrownBy(() -> service.submit(draft.id(), new DividendActionRequest("s-nolegal", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_LEGAL_AMOUNT_REQUIRED"));
        assertThat(repository.findStatementById(draft.id()).orElseThrow().status()).isEqualTo("draft");
    }

    @Test
    void legalConfirmationCapsDistributionAndSupersedesDraft() {
        repository.legalDefault = false;
        createDefaultPlan();
        repository.barRevenue.put(MERCHANT, 1_000_000L);
        DividendStatementRecord draft = computeOfficialQuarter();
        DividendStatementRecord capped = service.confirmLegal(draft.id(), legalRequest("legal-1", 900_000L));
        // 红字更正范式：旧草稿作废、按核定额取小出新草稿（经营 100 万、核定 90 万 → 本次分配 90 万）。
        assertThat(capped.id()).isNotEqualTo(draft.id());
        assertThat(repository.findStatementById(draft.id()).orElseThrow().status()).isEqualTo("voided");
        assertThat(capped.status()).isEqualTo("draft");
        assertThat(capped.netProfitCent()).isEqualTo(1_000_000L);
        assertThat(capped.legalDistributableCent()).isEqualTo(900_000L);
        assertThat(capped.distributableCent()).isEqualTo(900_000L);
        assertThat(capped.platformShareCent()).isEqualTo(720_000L);
        assertThat(capped.payoutCent()).isEqualTo(144_000L);
        assertThat(repository.listLines(capped.id(), null)).anyMatch(l -> l.lineType().equals("legal_cap")
                && l.amountCent() == 900_000L && l.detail().contains("TA-2026Q3-001") && l.detail().contains("R-2026-10-08"));
        service.submit(capped.id(), new DividendActionRequest("s-legal", null));
        actorId[0] = "usr_approver";
        service.approve(capped.id(), new DividendActionRequest("a-legal", null));
        verify(payoutPort).createDividendPayoutPayable("usr_principal", MERCHANT, 144_000L, capped.id());
    }

    @Test
    void legalConfirmationOnlyOnOfficialDraftWithBasis() {
        repository.legalDefault = false;
        createDefaultPlan();
        DividendStatementRecord preview = service.compute(
                new DividendComputeRequest("legal-prev", "2026-07", "preview", null)).computed().get(0);
        assertThatThrownBy(() -> service.confirmLegal(preview.id(), legalRequest("legal-p", 1L)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_PREVIEW_NOT_APPROVABLE"));
        DividendStatementRecord draft = computeOfficialQuarter();
        assertThatThrownBy(() -> service.confirmLegal(draft.id(), new DividendLegalConfirmationRequest(
                "legal-nores", 1L, "财税核算报告", " ", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.confirmLegal(draft.id(), legalRequest("legal-neg", -1L)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VALIDATION_FAILED"));
        repository.markSubmitted(draft.id(), "usr_admin", Instant.now());
        assertThatThrownBy(() -> service.confirmLegal(draft.id(), legalRequest("legal-pr", 1L)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_STATE_INVALID"));
        doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "platform scope required"))
                .when(authorizationService).requirePlatformScope();
        assertThatThrownBy(() -> service.confirmLegal(draft.id(), legalRequest("legal-scope", 1L)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
    }

    @Test
    void releaseRequiresOriginalLegalBasis() {
        // 本批前已审批、有保护线留存但无核定的旧单：手动与定时释放都核原合法分配依据，缺失不释放。
        repository.legalDefault = false;
        service.createPlan(new DividendPlanRequest("plan-old-rel", MERCHANT, "usr_principal", null,
                8000, 2000, "quarterly", List.of("bar"), false, List.of("labor"), null, null, null,
                null, null, true, 1000, null, "next_period", null));
        repository.barRevenue.put(MERCHANT, 1_000_000L);
        DividendStatementRecord old = service.compute(
                new DividendComputeRequest("old-rel-c", "2026-Q1", "official", MERCHANT)).computed().get(0);
        repository.markSubmitted(old.id(), "usr_admin", Instant.now());
        repository.markApproved(old.id(), "usr_approver", "arap_old", Instant.now());
        assertThatThrownBy(() -> service.releaseReserve(old.id(), new DividendActionRequest("old-rel-k", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_LEGAL_BASIS_MISSING"));
        DividendReserveReleaseRunRecord run = service.runScheduledReserveRelease(java.time.LocalDate.parse("2026-04-15"));
        assertThat(run.released()).isEmpty();
        assertThat(run.skipped()).anyMatch(k -> k.reason().contains(old.id()));
        verify(payoutPort, never()).releaseDividendReserveWithholding(anyString());
    }

    // ── FS-015 票务收入取已确认月度分账结算额（BSM L755、L1689）──

    @Test
    void unconfirmedTicketSettlementBlocksOfficialButPreviewCountsConfirmedOnly() {
        createDefaultPlan();
        repository.ticketSettled.put(MERCHANT, 200_000L);
        repository.unconfirmedTicketMonths = List.of("2026-09");
        assertThatThrownBy(this::computeOfficialQuarter)
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("TICKET_SETTLEMENT_PENDING"))
                .hasMessageContaining("2026-09");
        // 月度预览（1 日 02:00 早于月结 03:00）不拦：只计已确认月份，并写明未确认月份。
        DividendStatementRecord preview = service.compute(
                new DividendComputeRequest("prev-unconfirmed", "2026-09", "preview", null)).computed().get(0);
        assertThat(preview.revenueCent()).isEqualTo(200_000L);
        assertThat(repository.listLines(preview.id(), null)).anyMatch(l -> l.lineType().equals("revenue_ticket")
                && l.detail().contains("2026-09"));
    }

    @Test
    void computeRespectsBaseCompositionSwitches() {
        // 只计酒吧收入、不扣成本、只计人工费用。
        service.createPlan(new DividendPlanRequest("plan-key-cfg", MERCHANT, "usr_principal", null,
                5000, 1000, "quarterly", List.of("bar"), false, List.of("labor"), null, null, null,
                null, null, null, null, null));
        repository.barRevenue.put(MERCHANT, 500_000L);
        repository.ticketSettled.put(MERCHANT, 999_999L);
        repository.expenses.put(MERCHANT, List.of(
                new ExpenseTypeSumRecord("labor", 100_000L),
                new ExpenseTypeSumRecord("energy", 999_999L)));
        DividendStatementRecord statement = computeOfficialQuarter();
        assertThat(statement.revenueCent()).isEqualTo(500_000L);
        assertThat(statement.costCent()).isZero();
        assertThat(statement.expenseCent()).isEqualTo(100_000L);
        assertThat(statement.netProfitCent()).isEqualTo(400_000L);
        assertThat(statement.platformShareCent()).isEqualTo(200_000L);
        verify(inventoryRepository, never()).sumOutboundCost(anyString(), any(), anyString(), anyString(),
                any(), any());
    }

    @Test
    void computeIsIdempotentAndSupersedesDraftButNotPipeline() {
        createDefaultPlan();
        DividendStatementRecord first = computeOfficialQuarter();
        // 重算（新幂等键）：旧 draft 作废 + 新单（红字更正范式）。
        DividendComputeResultRecord recompute = service.compute(
                new DividendComputeRequest("compute-key-2", "2026-Q3", "official", null));
        assertThat(recompute.computed()).hasSize(1);
        assertThat(repository.findStatementById(first.id()).orElseThrow().status()).isEqualTo("voided");
        // 提交进入审批流后不再重算，跳过并给出原因。
        DividendStatementRecord second = recompute.computed().get(0);
        service.submit(second.id(), new DividendActionRequest("submit-key-1", null));
        DividendComputeResultRecord third = service.compute(
                new DividendComputeRequest("compute-key-3", "2026-Q3", "official", null));
        assertThat(third.computed()).isEmpty();
        assertThat(third.skipped()).hasSize(1);
        assertThat(third.skipped().get(0).reason()).contains("审批流");
    }

    @Test
    void previewOnlyForQuarterlyPlansAndNotApprovable() {
        createDefaultPlan();
        DividendComputeResultRecord preview = service.compute(
                new DividendComputeRequest("preview-key-1", "2026-07", "preview", null));
        assertThat(preview.computed()).hasSize(1);
        DividendStatementRecord statement = preview.computed().get(0);
        assertThat(statement.status()).isEqualTo("preview");
        // 预览单只可看：提交/审批/发放全部拒绝。
        assertThatThrownBy(() -> service.submit(statement.id(), new DividendActionRequest("k1", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_PREVIEW_NOT_APPROVABLE"));
        assertThatThrownBy(() -> service.approve(statement.id(), new DividendActionRequest("k2", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_PREVIEW_NOT_APPROVABLE"));
        // 预览用季度标签直接拒绝。
        assertThatThrownBy(() -> service.compute(
                new DividendComputeRequest("preview-key-2", "2026-Q3", "preview", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void officialPeriodLabelMustMatchPlanPeriodType() {
        createDefaultPlan();
        // 季度方案 + 月份标签的正式单 → 跳过并说明原因。
        DividendComputeResultRecord result = service.compute(
                new DividendComputeRequest("compute-month-key", "2026-07", "official", null));
        assertThat(result.computed()).isEmpty();
        assertThat(result.skipped()).hasSize(1);
        assertThat(result.skipped().get(0).reason()).contains("季度");
    }

    @Test
    void stateMachineSubmitApproveCreatesPayableThenPaid() {
        createDefaultPlan();
        repository.barRevenue.put(MERCHANT, 1_000_000L);
        DividendStatementRecord statement = computeOfficialQuarter();
        // draft 直接审批 → 状态非法。
        assertThatThrownBy(() -> service.approve(statement.id(), new DividendActionRequest("a0", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_STATE_INVALID"));
        service.submit(statement.id(), new DividendActionRequest("s1", null));
        // I-1 职责分离：审批人须与提交人不同。
        actorId[0] = "usr_approver";
        DividendStatementRecord approved = service.approve(statement.id(), new DividendActionRequest("a1", null));
        assertThat(approved.status()).isEqualTo("approved");
        assertThat(approved.arApItemId()).isEqualTo("arap_div_1");
        // 净发放额 = 1000000×20%−20%税 = 160000，按此额挂应付给主理人（默认全关：实发=税后应得）。
        verify(payoutPort).createDividendPayoutPayable("usr_principal", MERCHANT, 160_000L, statement.id());
        DividendStatementRecord paid = service.markPaid(statement.id(), new DividendActionRequest("p1", null));
        assertThat(paid.status()).isEqualTo("paid");
        // 已发放不可再驳回。
        assertThatThrownBy(() -> service.reject(statement.id(), new DividendActionRequest("r1", "驳回理由")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_STATE_INVALID"));
    }

    @Test
    void approveWithZeroNetSkipsPayable() {
        createDefaultPlan();
        // 无任何收入 → 净利 0 → 净发放 0 → 不挂应付。
        DividendStatementRecord statement = computeOfficialQuarter();
        service.submit(statement.id(), new DividendActionRequest("s1", null));
        actorId[0] = "usr_approver";
        DividendStatementRecord approved = service.approve(statement.id(), new DividendActionRequest("a1", null));
        assertThat(approved.status()).isEqualTo("approved");
        assertThat(approved.arApItemId()).isNull();
        verify(payoutPort, never()).createDividendPayoutPayable(anyString(), anyString(), anyLong(), anyString());
    }

    @Test
    void selfApprovalForbidden() {
        // I-1：提交人不能审批自己提交的单（SELF_APPROVAL_FORBIDDEN，补录审批同范式）；换人后放行。
        createDefaultPlan();
        DividendStatementRecord statement = computeOfficialQuarter();
        service.submit(statement.id(), new DividendActionRequest("s1", null));
        assertThatThrownBy(() -> service.approve(statement.id(), new DividendActionRequest("a1", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("SELF_APPROVAL_FORBIDDEN"));
        assertThat(repository.findStatementById(statement.id()).orElseThrow().status())
                .isEqualTo("pending_review");
        actorId[0] = "usr_approver";
        assertThat(service.approve(statement.id(), new DividendActionRequest("a2", null)).status())
                .isEqualTo("approved");
    }

    @Test
    void concurrentGuardMissYieldsBusinessConflictNot500() {
        // C-1：状态守卫 miss（UPDATE 影响 0 行，模拟 READ_COMMITTED 并发下已被他人驳回）→ 业务 409 非 500。
        InMemoryRepo guarded = new InMemoryRepo() {
            @Override
            public int markApproved(String id, String actor, String arApItemId, Instant now) {
                return 0; // 模拟并发窗口：读到 pending_review，UPDATE 时已被驳回。
            }
        };
        AuditedOperationExecutor operations = new AuditedOperationExecutor(
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                mock(AuditEventService.class));
        DividendService guardedService = new DividendService(guarded, inventoryRepository,
                new BusinessDayResolver(), authorizationService, operations, payoutPort);
        repository = guarded;
        service = guardedService;
        createDefaultPlan();
        DividendStatementRecord statement = computeOfficialQuarter();
        service.submit(statement.id(), new DividendActionRequest("s1", null));
        actorId[0] = "usr_approver";
        assertThatThrownBy(() -> service.approve(statement.id(), new DividendActionRequest("a1", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_STATE_INVALID"));
    }

    @Test
    void reactivatingPlanConflictsWithAnotherActivePlan() {
        // S-2：停用后另建生效方案，再把旧方案改回 active → 撞唯一生效索引，业务 409（DIVIDEND_PLAN_EXISTS）。
        DividendPlanRecord first = createDefaultPlan();
        service.updatePlan(first.id(), new DividendPlanRequest("end-key", null, null, null, null, null,
                null, null, null, null, null, "ended", null, null, null, null, null, null));
        service.createPlan(new DividendPlanRequest("plan-key-2", MERCHANT, "usr_principal_2", null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null));
        assertThatThrownBy(() -> service.updatePlan(first.id(), new DividendPlanRequest("react-key", null,
                null, null, null, null, null, null, null, null, null, "active", null,
                null, null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_PLAN_EXISTS"));
    }

    @Test
    void rejectRequiresReasonAndAllowsRecompute() {
        createDefaultPlan();
        DividendStatementRecord statement = computeOfficialQuarter();
        service.submit(statement.id(), new DividendActionRequest("s1", null));
        assertThatThrownBy(() -> service.reject(statement.id(), new DividendActionRequest("r0", "  ")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VALIDATION_FAILED"));
        DividendStatementRecord rejected = service.reject(statement.id(), new DividendActionRequest("r1", "费用未挂接完整"));
        assertThat(rejected.status()).isEqualTo("rejected");
        assertThat(rejected.rejectReason()).isEqualTo("费用未挂接完整");
        // 驳回后可重算：旧单作废 + 新 draft。
        DividendComputeResultRecord recompute = service.compute(
                new DividendComputeRequest("compute-key-2", "2026-Q3", "official", null));
        assertThat(recompute.computed()).hasSize(1);
        assertThat(repository.findStatementById(statement.id()).orElseThrow().status()).isEqualTo("voided");
    }

    @Test
    void merchantScopeForcedOnReadsAndForbiddenOnWrites() {
        createDefaultPlan();
        DividendStatementRecord statement = computeOfficialQuarter();
        // 商户 scope 账号：列表被强制只看自己（请求他商户参数被覆盖）。
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of("merchant_b"));
        PageResponse<DividendStatementRecord> page =
                service.listStatements(MERCHANT, null, null, null, 1, 50);
        assertThat(page.items()).isEmpty();
        // 详情跨商户 → 拒绝。
        assertThatThrownBy(() -> service.getStatement(statement.id()))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("CROSS_MERCHANT_FORBIDDEN"));
        // 写路径要求平台 scope（requirePlatformScope 抛出即拒绝）。
        doThrow(ApiException.forbidden("PLATFORM_SCOPE_REQUIRED", "platform scope required"))
                .when(authorizationService).requirePlatformScope();
        assertThatThrownBy(() -> service.compute(
                new DividendComputeRequest("compute-key-x", "2026-Q3", "official", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("PLATFORM_SCOPE_REQUIRED"));
        // 本商户详情可看（主理人视图），但 S-3 脱敏：内部操作员 id 与应付款项 id 不外发。
        when(authorizationService.currentMerchantScope()).thenReturn(Optional.of(MERCHANT));
        DividendStatementDetailRecord detail = service.getStatement(statement.id());
        assertThat(detail.statement().id()).isEqualTo(statement.id());
        assertThat(detail.lines()).isNotEmpty();
        assertThat(detail.statement().computedBy()).isNull();
        assertThat(detail.statement().submittedBy()).isNull();
        assertThat(detail.statement().approvedBy()).isNull();
        assertThat(detail.statement().arApItemId()).isNull();
        // 金额与状态照常可见。
        assertThat(detail.statement().principalNetCent()).isEqualTo(statement.principalNetCent());
        assertThat(detail.statement().payoutCent()).isEqualTo(statement.payoutCent());
        PageResponse<DividendStatementRecord> ownPage = service.listStatements(null, null, null, null, 1, 50);
        assertThat(ownPage.items()).isNotEmpty();
        assertThat(ownPage.items().get(0).computedBy()).isNull();
    }

    @Test
    void idempotencyReplayReturnsFirstResponseAndConflictsOnDifferentPayload() {
        createDefaultPlan();
        DividendComputeResultRecord first = service.compute(
                new DividendComputeRequest("same-key", "2026-Q3", "official", null));
        // 同键同载荷重放：返回首次响应（不作废重算）。
        DividendComputeResultRecord replay = service.compute(
                new DividendComputeRequest("same-key", "2026-Q3", "official", null));
        assertThat(replay.computed().get(0).id()).isEqualTo(first.computed().get(0).id());
        assertThat(repository.findStatementById(first.computed().get(0).id()).orElseThrow().status())
                .isEqualTo("draft");
        // 同键不同载荷 → IDEMPOTENCY_CONFLICT。
        assertThatThrownBy(() -> service.compute(
                new DividendComputeRequest("same-key", "2026-Q4", "official", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void monthEndBusinessDayFinalPreviewRefreshesQuarterlyAndSkipsMonthlyPlans() {
        // 159-R6 月末营业日五类标齐事件：季度方案出 preview 单（系统操作人，不可审批），月度方案不出。
        createDefaultPlan(); // 季度方案（MERCHANT）
        service.createPlan(new DividendPlanRequest("plan-monthly", "merchant_monthly", "usr_p2", null,
                null, null, "monthly", null, null, null, null, null, null, null, null, null, null, null));
        repository.barRevenue.put(MERCHANT, 1_000_000L);

        service.onMonthEndBusinessDayFinal(new FinanceService.MonthEndBusinessDayFinal(MERCHANT, null, YearMonth.of(2026, 7)));
        DividendStatementRecord preview = repository.findActiveStatement(MERCHANT, "preview", "2026-07").orElseThrow();
        assertThat(preview.status()).isEqualTo("preview");
        assertThat(preview.computedBy()).isEqualTo("system_dividend_preview");

        // 刷新式重跑：旧 preview 作废 + 新 preview（取后者，反映台账最新状态）。
        service.onMonthEndBusinessDayFinal(new FinanceService.MonthEndBusinessDayFinal(MERCHANT, null, YearMonth.of(2026, 7)));
        assertThat(repository.findActiveStatement(MERCHANT, "preview", "2026-07").orElseThrow().id()).isNotEqualTo(preview.id());
        assertThat(repository.findStatementById(preview.id()).orElseThrow().status()).isEqualTo("voided");

        // 月度方案不出月度预览。
        service.onMonthEndBusinessDayFinal(new FinanceService.MonthEndBusinessDayFinal("merchant_monthly", null, YearMonth.of(2026, 7)));
        assertThat(repository.statements.values()).noneMatch(s -> "merchant_monthly".equals(s.merchantId()));
    }

    // ── V303 扣除项三开关 ──────────────────────────────────────────

    /** 亏损结转：季度亏损期审批后，往期未弥补亏损结转至下季度并在盈利期抵减可分配额（跨期结转）。 */
    @Test
    void lossCarryforwardOffsetsPriorLossAcrossPeriods() {
        service.createPlan(new DividendPlanRequest("lcf-plan", MERCHANT, "usr_principal", null,
                8000, 2000, "quarterly", List.of("bar"), false, List.of("labor"), null, null, null,
                true, null, null, null, null)); // 开亏损结转，费用置空
        // Q1 亏损 300000：只有酒吧退款无收入。
        repository.barRefund.put(MERCHANT, 300_000L);
        DividendStatementRecord q1 = service.compute(
                new DividendComputeRequest("lcf-q1", "2026-Q1", "official", null)).computed().get(0);
        assertThat(q1.netProfitCent()).isEqualTo(-300_000L);
        assertThat(q1.distributableCent()).isZero();
        assertThat(q1.lossCarryforwardAfterCent()).isEqualTo(300_000L);
        // 审批 Q1（实发 0），结转生效。
        service.submit(q1.id(), new DividendActionRequest("lcf-s1", null));
        actorId[0] = "usr_approver";
        service.approve(q1.id(), new DividendActionRequest("lcf-a1", null));

        // Q2 盈利 500000：可分配 = 500000 − 期初累计 300000 = 200000。
        actorId[0] = "usr_admin";
        repository.barRefund.put(MERCHANT, 0L);
        repository.barRevenue.put(MERCHANT, 500_000L);
        DividendStatementRecord q2 = service.compute(
                new DividendComputeRequest("lcf-q2", "2026-Q2", "official", null)).computed().get(0);
        assertThat(q2.lossCarryforwardInCent()).isEqualTo(300_000L);
        assertThat(q2.lossCarryforwardAppliedCent()).isEqualTo(300_000L);
        assertThat(q2.lossCarryforwardAfterCent()).isZero();
        assertThat(q2.distributableCent()).isEqualTo(200_000L);
        // 结转留痕行。
        assertThat(repository.listLines(q2.id(), null))
                .anyMatch(l -> l.lineType().equals("loss_carryforward") && l.amountCent() == -300_000L);
    }

    /** 欠款抵扣：税后应得先抵扣未清应收，实发=余额，审批时按冻结额结算应收；逐项留痕。 */
    @Test
    void arrearsOffsetReducesPayoutAndSettlesReceivablesAtApprove() {
        service.createPlan(new DividendPlanRequest("arr-plan", MERCHANT, "usr_principal", null,
                8000, 2000, "quarterly", List.of("bar"), false, List.of("labor"), null, null, null,
                null, true, null, null, null)); // 开欠款抵扣
        repository.barRevenue.put(MERCHANT, 1_000_000L); // 税后应得 160000
        // 未清应收两笔合计 100000。
        when(payoutPort.listOutstandingReceivables(MERCHANT)).thenReturn(List.of(
                new DividendArrearsItem("arap_r1", "AR-001", 60_000L),
                new DividendArrearsItem("arap_r2", "AR-002", 40_000L)));
        DividendStatementRecord s = service.compute(
                new DividendComputeRequest("arr-c1", "2026-Q3", "official", null)).computed().get(0);
        assertThat(s.principalNetCent()).isEqualTo(160_000L);
        assertThat(s.arrearsOffsetCent()).isEqualTo(100_000L);
        assertThat(s.payoutCent()).isEqualTo(60_000L);
        // 逐项留痕：两笔应收各一行。
        assertThat(repository.listLines(s.id(), null))
                .filteredOn(l -> l.lineType().equals("arrears_offset"))
                .hasSize(2);
        // 审批：先结算应收（冻结 100000），再按实发 60000 挂应付。
        service.submit(s.id(), new DividendActionRequest("arr-s1", null));
        actorId[0] = "usr_approver";
        service.approve(s.id(), new DividendActionRequest("arr-a1", null));
        verify(payoutPort).applyDividendOffsetToReceivables(MERCHANT, 100_000L, s.id());
        verify(payoutPort).createDividendPayoutPayable("usr_principal", MERCHANT, 60_000L, s.id());
    }

    /** 欠款抵扣：审批时应收被并发清偿（实结 < 冻结额）→ DIVIDEND_ARREARS_CHANGED，回滚要求重算。 */
    @Test
    void arrearsOffsetConflictsWhenReceivablesChangedAtApprove() {
        service.createPlan(new DividendPlanRequest("arr-plan2", MERCHANT, "usr_principal", null,
                8000, 2000, "quarterly", List.of("bar"), false, List.of("labor"), null, null, null,
                null, true, null, null, null));
        repository.barRevenue.put(MERCHANT, 1_000_000L);
        when(payoutPort.listOutstandingReceivables(MERCHANT)).thenReturn(List.of(
                new DividendArrearsItem("arap_r1", "AR-001", 100_000L)));
        DividendStatementRecord s = service.compute(
                new DividendComputeRequest("arr2-c1", "2026-Q3", "official", null)).computed().get(0);
        assertThat(s.arrearsOffsetCent()).isEqualTo(100_000L);
        // 抵扣期间应收被清偿：只结算得 40000 < 冻结 100000。
        when(payoutPort.applyDividendOffsetToReceivables(anyString(), anyLong(), anyString())).thenReturn(40_000L);
        service.submit(s.id(), new DividendActionRequest("arr2-s1", null));
        actorId[0] = "usr_approver";
        assertThatThrownBy(() -> service.approve(s.id(), new DividendActionRequest("arr2-a1", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_ARREARS_CHANGED"));
        // 未挂应付（事务回滚语义：不产生半截发放）。
        verify(payoutPort, never()).createDividendPayoutPayable(anyString(), anyString(), anyLong(), anyString());
    }

    /** 发放保护线：按比例留存风险准备金，实发=税后应得−留存，留痕并挂应付按实发额。 */
    @Test
    void payoutFloorWithholdsReserveAndPaysNetOfReserve() {
        service.createPlan(new DividendPlanRequest("floor-plan", MERCHANT, "usr_principal", null,
                8000, 2000, "quarterly", List.of("bar"), false, List.of("labor"), null, null, null,
                null, null, true, 1000, null)); // 开保护线，留 10%
        repository.barRevenue.put(MERCHANT, 1_000_000L); // 税后应得 160000
        DividendStatementRecord s = service.compute(
                new DividendComputeRequest("floor-c1", "2026-Q3", "official", null)).computed().get(0);
        assertThat(s.principalNetCent()).isEqualTo(160_000L);
        assertThat(s.reserveWithheldCent()).isEqualTo(16_000L);
        assertThat(s.payoutCent()).isEqualTo(144_000L);
        assertThat(repository.listLines(s.id(), null))
                .anyMatch(l -> l.lineType().equals("reserve_withheld") && l.amountCent() == -16_000L);
        service.submit(s.id(), new DividendActionRequest("floor-s1", null));
        actorId[0] = "usr_approver";
        service.approve(s.id(), new DividendActionRequest("floor-a1", null));
        verify(payoutPort).createDividendPayoutPayable("usr_principal", MERCHANT, 144_000L, s.id());
        // 留存不白扣：记为欠主理人的待释放应付（金额=留存额），释放为后续 R。
        verify(payoutPort).createDividendReserveWithholdingPayable("usr_principal", MERCHANT, 16_000L, s.id());
    }

    /**
     * 亏损结转累计恒结转（用户 2026-07-19 拍板反例）：开→亏→关一期→再开。
     * 关闭期不扣减但累计不清零；再开时 carryIn=保留的累计（非 0），正常抵减可分配额。
     */
    @Test
    void lossCarryforwardAccumulatesWhilePausedThenResumes() {
        // 季度方案：只计酒吧收入、不扣成本、无费用；亏损结转开。
        DividendPlanRecord plan = service.createPlan(new DividendPlanRequest("lcf-pause", MERCHANT,
                "usr_principal", null, 8000, 2000, "quarterly", List.of("bar"), false, List.of("labor"),
                null, null, null, true, null, null, null, null));
        // Q1（开）亏损 100000：仅酒吧退款无收入 → 净利 −100000，结转累计 100000。
        repository.barRefund.put(MERCHANT, 100_000L);
        DividendStatementRecord q1 = service.compute(
                new DividendComputeRequest("lcf-p-q1", "2026-Q1", "official", null)).computed().get(0);
        assertThat(q1.netProfitCent()).isEqualTo(-100_000L);
        assertThat(q1.lossCarryforwardAfterCent()).isEqualTo(100_000L);
        service.submit(q1.id(), new DividendActionRequest("lcf-p-s1", null));
        actorId[0] = "usr_approver";
        service.approve(q1.id(), new DividendActionRequest("lcf-p-a1", null));

        // 关闭亏损结转（暂停本期抵扣）。
        actorId[0] = "usr_admin";
        service.updatePlan(plan.id(), new DividendPlanRequest("lcf-p-off", null, null, null, null, null,
                null, null, null, null, null, null, null, false, null, null, null, null));
        // Q2（关）无收支：本期不扣、累计不清零 → carryIn=100000、applied=0、after=100000。
        repository.barRefund.put(MERCHANT, 0L);
        DividendStatementRecord q2 = service.compute(
                new DividendComputeRequest("lcf-p-q2", "2026-Q2", "official", null)).computed().get(0);
        assertThat(q2.lossCarryforwardInCent()).isEqualTo(100_000L);
        assertThat(q2.lossCarryforwardAppliedCent()).isZero();
        assertThat(q2.lossCarryforwardAfterCent()).isEqualTo(100_000L);
        assertThat(q2.distributableCent()).isZero();
        service.submit(q2.id(), new DividendActionRequest("lcf-p-s2", null));
        actorId[0] = "usr_approver";
        service.approve(q2.id(), new DividendActionRequest("lcf-p-a2", null));

        // 再开亏损结转。
        actorId[0] = "usr_admin";
        service.updatePlan(plan.id(), new DividendPlanRequest("lcf-p-on", null, null, null, null, null,
                null, null, null, null, null, null, null, true, null, null, null, null));
        // Q3（再开）盈利 500000：carryIn=保留的 100000（非 0！）→ 弥补 100000、可分配 400000。
        repository.barRevenue.put(MERCHANT, 500_000L);
        DividendStatementRecord q3 = service.compute(
                new DividendComputeRequest("lcf-p-q3", "2026-Q3", "official", null)).computed().get(0);
        assertThat(q3.lossCarryforwardInCent()).isEqualTo(100_000L);
        assertThat(q3.lossCarryforwardAppliedCent()).isEqualTo(100_000L);
        assertThat(q3.lossCarryforwardAfterCent()).isZero();
        assertThat(q3.distributableCent()).isEqualTo(400_000L);
    }

    /** 三扣除组合：亏损结转 + 欠款抵扣 + 保护线同单生效，快照与留痕逐项一致，实发按最终额发放。 */
    @Test
    void allThreeDeductionsComposeOnOneStatement() {
        service.createPlan(new DividendPlanRequest("combo-plan", MERCHANT, "usr_principal", null,
                8000, 2000, "quarterly", List.of("bar"), false, List.of("labor"), null, null, null,
                true, true, true, 1000, null));
        // 先造 Q1 亏损 200000 并审批，形成期初累计。
        repository.barRefund.put(MERCHANT, 200_000L);
        DividendStatementRecord q1 = service.compute(
                new DividendComputeRequest("combo-q1", "2026-Q1", "official", null)).computed().get(0);
        service.submit(q1.id(), new DividendActionRequest("combo-s0", null));
        actorId[0] = "usr_approver";
        service.approve(q1.id(), new DividendActionRequest("combo-a0", null));
        // Q2 盈利 1000000，期初累计 200000 → 可分配 800000；平台 80% → 应得毛 160000，税 20% → 税后 128000。
        actorId[0] = "usr_admin";
        repository.barRefund.put(MERCHANT, 0L);
        repository.barRevenue.put(MERCHANT, 1_000_000L);
        when(payoutPort.listOutstandingReceivables(MERCHANT)).thenReturn(List.of(
                new DividendArrearsItem("arap_r1", "AR-001", 28_000L)));
        DividendStatementRecord q2 = service.compute(
                new DividendComputeRequest("combo-q2", "2026-Q2", "official", null)).computed().get(0);
        assertThat(q2.distributableCent()).isEqualTo(800_000L);
        assertThat(q2.principalNetCent()).isEqualTo(128_000L);
        assertThat(q2.arrearsOffsetCent()).isEqualTo(28_000L);
        // 抵扣后 100000，留 10% = 10000 → 实发 90000。
        assertThat(q2.reserveWithheldCent()).isEqualTo(10_000L);
        assertThat(q2.payoutCent()).isEqualTo(90_000L);
        assertThat(q2.payoutCent())
                .isEqualTo(q2.principalNetCent() - q2.arrearsOffsetCent() - q2.reserveWithheldCent());
    }

    // ── V304 发放保护线留存·释放 ──────────────────────────────

    /** 造一张带留存（保护线留 10%）的已审批正式单；releaseMode 指定释放规则。 */
    private DividendStatementRecord approvedReserveStatement(
            String merchant, String planKey, String releaseMode, String period, String key) {
        service.createPlan(new DividendPlanRequest(planKey, merchant, "usr_principal_" + merchant, null,
                8000, 2000, "quarterly", List.of("bar"), false, List.of("labor"), null, null, null,
                null, null, true, 1000, null, releaseMode, null));
        repository.barRevenue.put(merchant, 1_000_000L); // 税后应得 160000，留 10% = 16000
        DividendStatementRecord s = service.compute(
                new DividendComputeRequest(key, period, "official", merchant)).computed().get(0);
        service.submit(s.id(), new DividendActionRequest(key + "-s", null));
        actorId[0] = "usr_approver";
        service.approve(s.id(), new DividendActionRequest(key + "-a", null));
        actorId[0] = "usr_admin";
        return repository.findStatementById(s.id()).orElseThrow();
    }

    /** 手动释放：re-tag 待释放留存为已释放，金额=原留存额、留痕（触发方式/操作人）；不改原单快照（关账协同）。 */
    @Test
    void manualReleaseRecordsLedgerAndKeepsStatementSnapshot() {
        DividendStatementRecord s = approvedReserveStatement(MERCHANT, "rel-plan", "manual", "2026-Q3", "rel-c1");
        assertThat(s.reserveWithheldCent()).isEqualTo(16_000L);
        DividendReserveReleaseRecord rel = service.releaseReserve(
                s.id(), new DividendActionRequest("rel-k1", "季度到期返还"));
        assertThat(rel.reserveCent()).isEqualTo(16_000L);        // 金额=原留存额（不多不少）
        assertThat(rel.triggerType()).isEqualTo("manual");
        assertThat(rel.releasedBy()).isEqualTo("usr_admin");     // 留痕操作人
        assertThat(rel.statementId()).isEqualTo(s.id());
        verify(payoutPort).releaseDividendReserveWithholding(s.id());
        // 关账/快照协同：释放是新动作，原已审批计算单的留存快照不变（不改冻结单）。
        assertThat(repository.findStatementById(s.id()).orElseThrow().reserveWithheldCent()).isEqualTo(16_000L);
    }

    /** 幂等：同一留存不重复释放（第二次不同键 → uq 拦下 ALREADY_RELEASED）。 */
    @Test
    void releaseIsIdempotentNoDoubleRelease() {
        DividendStatementRecord s = approvedReserveStatement(MERCHANT, "rel-plan2", "manual", "2026-Q3", "rel2-c1");
        service.releaseReserve(s.id(), new DividendActionRequest("rel2-k1", null));
        assertThatThrownBy(() -> service.releaseReserve(s.id(), new DividendActionRequest("rel2-k2", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_RESERVE_ALREADY_RELEASED"));
    }

    /** finance 侧报告无待释放应付（已翻/被释放）→ ALREADY_RELEASED（释放守卫路径）。 */
    @Test
    void releaseConflictsWhenFinanceHasNoWithheldPayable() {
        DividendStatementRecord s = approvedReserveStatement(MERCHANT, "rel-plan3", "manual", "2026-Q3", "rel3-c1");
        when(payoutPort.releaseDividendReserveWithholding(s.id())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.releaseReserve(s.id(), new DividendActionRequest("rel3-k1", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_RESERVE_ALREADY_RELEASED"));
    }

    /** 状态/留存校验：未审批单不可释放；无留存单无需释放。 */
    @Test
    void releaseRequiresApprovedOrPaidWithReserve() {
        // draft 单不可释放。
        service.createPlan(new DividendPlanRequest("rel-plan4", MERCHANT, "usr_principal", null,
                8000, 2000, "quarterly", List.of("bar"), false, List.of("labor"), null, null, null,
                null, null, true, 1000, null, "manual", null));
        repository.barRevenue.put(MERCHANT, 1_000_000L);
        DividendStatementRecord draft = service.compute(
                new DividendComputeRequest("rel4-c1", "2026-Q3", "official", MERCHANT)).computed().get(0);
        assertThatThrownBy(() -> service.releaseReserve(draft.id(), new DividendActionRequest("rel4-k1", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_STATE_INVALID"));
        // 无留存（未开保护线）的已审批单 → DIVIDEND_RESERVE_NONE。
        service.createPlan(new DividendPlanRequest("rel-plan5", "merchant_nr", "usr_p", null,
                8000, 2000, "quarterly", List.of("bar"), false, List.of("labor"), null, null, null,
                null, null, false, null, null, "manual", null));
        repository.barRevenue.put("merchant_nr", 1_000_000L);
        DividendStatementRecord noReserve = service.compute(
                new DividendComputeRequest("rel5-c1", "2026-Q3", "official", "merchant_nr")).computed().get(0);
        service.submit(noReserve.id(), new DividendActionRequest("rel5-s", null));
        actorId[0] = "usr_approver";
        service.approve(noReserve.id(), new DividendActionRequest("rel5-a", null));
        actorId[0] = "usr_admin";
        assertThat(noReserve.reserveWithheldCent()).isZero();
        assertThatThrownBy(() -> service.releaseReserve(noReserve.id(), new DividendActionRequest("rel5-k1", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_RESERVE_NONE"));
    }

    /** 自动作业：到期候选（next_period）释放、手动模式不自动释放、系统操作人留痕；重跑不重复释放。 */
    @Test
    void scheduledReleaseReleasesDueSkipsManualAndIsIdempotent() {
        DividendStatementRecord due = approvedReserveStatement("m_np", "p-np", "next_period", "2026-Q1", "np");
        DividendStatementRecord manual = approvedReserveStatement("m_man", "p-man", "manual", "2026-Q1", "man");
        // Q1 期末 2026-03-31；asOf 2026-04-15：next_period 到期、manual 恒不自动。
        DividendReserveReleaseRunRecord run =
                service.runScheduledReserveRelease(java.time.LocalDate.parse("2026-04-15"));
        assertThat(run.released()).extracting(DividendReserveReleaseRecord::statementId)
                .contains(due.id()).doesNotContain(manual.id());
        assertThat(run.released()).allSatisfy(r -> {
            assertThat(r.triggerType()).isEqualTo("scheduled");
            assertThat(r.releasedBy()).isEqualTo("system_dividend_reserve_release"); // 系统操作人留痕
        });
        // 幂等重跑：已释放候选被排除，不再释放。
        DividendReserveReleaseRunRecord rerun =
                service.runScheduledReserveRelease(java.time.LocalDate.parse("2026-04-16"));
        assertThat(rerun.released()).noneMatch(r -> r.statementId().equals(due.id()));
    }

    /** 待释放/已释放台账：释放前在 pending、释放后在 released（含触发方式）。 */
    @Test
    void reserveLedgerReflectsPendingThenReleased() {
        DividendStatementRecord s = approvedReserveStatement(MERCHANT, "p-led", "manual", "2026-Q3", "led");
        assertThat(service.listReserveLedger(null, "pending", 1, 50).items())
                .anyMatch(l -> l.statementId().equals(s.id()) && !l.released());
        service.releaseReserve(s.id(), new DividendActionRequest("led-rel", null));
        assertThat(service.listReserveLedger(null, "released", 1, 50).items())
                .anyMatch(l -> l.statementId().equals(s.id()) && l.released() && "manual".equals(l.triggerType()));
        assertThat(service.listReserveLedger(null, "pending", 1, 50).items())
                .noneMatch(l -> l.statementId().equals(s.id()));
    }

    // ── V308 主理人类型（个人/公司）代扣个税分支（#28）──────────────────────

    /** #28 公司股东不代扣个税：生效税率快照 0、tax=0、全额发放；类型快照与算式行体现。 */
    @Test
    void companyPrincipalWithholdsNoTaxAndPaysGross() {
        service.createPlan(new DividendPlanRequest("co-key", MERCHANT, "usr_co", "公司主理人甲",
                null, null, "quarterly", null, null, null, null, null, null,
                null, null, null, null, null, null, null, "company", null));
        repository.barRevenue.put(MERCHANT, 1_000_000L);
        DividendStatementRecord s = computeOfficialQuarter();
        // 净利 100 万 → 平台 80% = 80 万；主理人毛额 20 万；公司不代扣 → 税 0、税后=毛额、实发=毛额。
        assertThat(s.principalType()).isEqualTo("company");
        assertThat(s.taxRateBps()).isZero();                 // 生效税率快照为 0（公司分支）
        assertThat(s.principalGrossCent()).isEqualTo(200_000L);
        assertThat(s.taxCent()).isZero();
        assertThat(s.principalNetCent()).isEqualTo(200_000L);
        assertThat(s.payoutCent()).isEqualTo(200_000L);
        assertThat(repository.listLines(s.id(), null))
                .anyMatch(l -> l.lineType().equals("tax_withheld") && l.amountCent() == 0L
                        && l.detail().contains("公司股东"));
    }

    /** #28 个人股东按方案税率代扣（对照公司分支，验证类型分流）。 */
    @Test
    void individualPrincipalWithholdsDividendTax() {
        createDefaultPlan();                                  // 默认 individual + 股息红利 20%
        repository.barRevenue.put(MERCHANT, 1_000_000L);
        DividendStatementRecord s = computeOfficialQuarter();
        assertThat(s.principalType()).isEqualTo("individual");
        assertThat(s.taxRateBps()).isEqualTo(2000);
        assertThat(s.principalGrossCent()).isEqualTo(200_000L);
        assertThat(s.taxCent()).isEqualTo(40_000L);          // 20 万 × 20%
        assertThat(s.principalNetCent()).isEqualTo(160_000L);
        assertThat(s.payoutCent()).isEqualTo(160_000L);
    }

    // ── V309 自定义扣除项（#35）──────────────────────────────────────────

    /** #35 自定义扣除项：固定额 + 比例逐项封顶扣减实发，合计快照 + 逐项算式行 + 发放额恒等；停用项不算。 */
    @Test
    void customDeductionsReducePayoutWithSnapshotAndLines() {
        service.createPlan(new DividendPlanRequest("cd-key", MERCHANT, "usr_principal", "主理人甲",
                null, null, "quarterly", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                List.of(
                        new DividendPlanRequest.CustomDeductionItem("固定管理费", "fixed", 30_000L, null, null, true),
                        new DividendPlanRequest.CustomDeductionItem("营收提成", "ratio", null, 1000, "net_profit", true),
                        new DividendPlanRequest.CustomDeductionItem("停用项", "fixed", 99_999L, null, null, false))));
        repository.barRevenue.put(MERCHANT, 1_000_000L);
        DividendStatementRecord s = computeOfficialQuarter();
        // 净利 100 万 → 主理人毛额 20 万 → 税 4 万 → 税后 16 万；固定扣 3 万 + 比例(净利 10%)=10 万，合计 13 万；实发 3 万。
        assertThat(s.principalNetCent()).isEqualTo(160_000L);
        assertThat(s.customDeductionCent()).isEqualTo(130_000L);
        assertThat(s.payoutCent()).isEqualTo(30_000L);
        // 发放额恒等：payout = principal_net − arrears − custom − reserve。
        assertThat(s.payoutCent()).isEqualTo(
                s.principalNetCent() - s.arrearsOffsetCent() - s.customDeductionCent() - s.reserveWithheldCent());
        List<DividendStatementLineRecord> lines = repository.listLines(s.id(), null);
        assertThat(lines).filteredOn(l -> l.lineType().equals("custom_deduction"))
                .extracting(DividendStatementLineRecord::amountCent)
                .containsExactlyInAnyOrder(-30_000L, -100_000L);
        assertThat(lines).noneMatch(l -> l.lineType().equals("custom_deduction") && l.detail().contains("停用项"));
    }

    /**
     * R-7（2026-07-28 裁决口径）：<b>自定义扣除项按比例算的原始额改四舍五入</b>（旧整除截断，
     * 扣除方恒少扣、差额白留主理人且不留痕）。本例取恰好 .5 的边界证明进位。
     *
     * <p>净利 1000 分、平台 0%、税 0% → 税后应得 1000（隔离出扣除这一步单独验）；
     * 比例 12.55% × 净利 1000 = <b>125.5 分</b> → 旧整除截断 125，新四舍五入 <b>126</b>；实发 874（旧 875）。
     */
    @Test
    void customDeductionRatioRoundsHalfUpAtExactHalfBoundary() {
        service.createPlan(new DividendPlanRequest("cd-halfup", MERCHANT, "usr_principal", null,
                0, 0, "quarterly", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                List.of(new DividendPlanRequest.CustomDeductionItem(
                        "比例扣除", "ratio", null, 1255, "net_profit", true))));
        repository.barRevenue.put(MERCHANT, 1000L);
        DividendStatementRecord s = computeOfficialQuarter();

        assertThat(s.netProfitCent()).isEqualTo(1000L);
        assertThat(s.principalNetCent()).isEqualTo(1000L);          // 平台 0%、税 0%
        assertThat(1000L * 1255L / 10000L).isEqualTo(125L);          // 证伪:旧整除截断给 125
        assertThat(s.customDeductionCent()).isEqualTo(126L);         // 新:四舍五入给 126
        assertThat(s.payoutCent()).isEqualTo(874L);
        assertConserved(s);
    }

    /** R-7：比例基数取「营收」的分支同样四舍五入，且确实按营收（非净利）取基数。 */
    @Test
    void customDeductionRatioOnRevenueBasisAlsoRoundsHalfUp() {
        service.createPlan(new DividendPlanRequest("cd-halfup-rev", MERCHANT, "usr_principal", null,
                0, 0, "quarterly", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                List.of(new DividendPlanRequest.CustomDeductionItem(
                        "营收比例扣除", "ratio", null, 1255, "revenue", true))));
        repository.barRevenue.put(MERCHANT, 3000L);
        repository.expenses.put(MERCHANT, List.of(new ExpenseTypeSumRecord("labor", 2000L)));
        DividendStatementRecord s = computeOfficialQuarter();

        assertThat(s.revenueCent()).isEqualTo(3000L);
        assertThat(s.netProfitCent()).isEqualTo(1000L);              // 营收 3000 − 费用 2000
        // 基数=营收 3000 × 12.55% = 376.5 分 → 四舍五入 377（旧整除截断 376）；
        // 若误按净利取基数会是 125.5→126，本断言同时锁住「基数确实是营收」。
        assertThat(3000L * 1255L / 10000L).isEqualTo(376L);          // 证伪:旧整除截断给 376
        assertThat(s.customDeductionCent()).isEqualTo(377L);
        assertThat(s.payoutCent()).isEqualTo(623L);                  // 1000 − 377
        assertConserved(s);
    }

    /** R-7：非 .5 边界（除得尽）时新旧同值——改口径对既有配置零行为变更。 */
    @Test
    void customDeductionRatioUnchangedWhenDivisionIsExact() {
        service.createPlan(new DividendPlanRequest("cd-exact", MERCHANT, "usr_principal", null,
                0, 0, "quarterly", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                List.of(new DividendPlanRequest.CustomDeductionItem(
                        "整除比例", "ratio", null, 1000, "net_profit", true))));
        repository.barRevenue.put(MERCHANT, 1000L);
        DividendStatementRecord s = computeOfficialQuarter();

        assertThat(1000L * 1000L / 10000L).isEqualTo(100L);          // 旧
        assertThat(s.customDeductionCent()).isEqualTo(100L);          // 新:同值
        assertThat(s.payoutCent()).isEqualTo(900L);
        assertConserved(s);
    }

    /**
     * R-7 守恒（自定义扣除改口径后仍必须成立）：
     * {@code 平台分成 + 代扣个税 + 欠款抵扣 + 自定义扣除 + 风险准备金 + 主理人实得 ≡ 可分配额}，逐分严格相等。
     *
     * <p>成立原因：自定义扣除只改「扣多少」的<b>大小</b>，不改链条结构——
     * {@code afterCustom := afterArrears − custom} 仍是补数，故扣除额用什么取整口径都不影响总额恒等。
     */
    @Test
    void customDeductionHalfUpKeepsFullChainConservation() {
        service.createPlan(new DividendPlanRequest("cd-conserve", MERCHANT, "usr_principal", null,
                8000, 1255, "quarterly", null, null, null, null, null, null,
                null, null, true, 1255, 0L, null, null, null,
                List.of(
                        new DividendPlanRequest.CustomDeductionItem("比例扣除", "ratio", null, 1255, "net_profit", true),
                        new DividendPlanRequest.CustomDeductionItem("固定扣除", "fixed", 7L, null, null, true))));
        repository.barRevenue.put(MERCHANT, 12_345L);
        DividendStatementRecord s = computeOfficialQuarter();
        assertConserved(s);
        // 各方非负 + V299 两条 DB CHECK 恒等式（平台分成/自定义扣除改 HALF_UP 后仍成立）
        assertThat(s.platformShareCent() + s.principalGrossCent()).isEqualTo(s.distributableCent());
        assertThat(s.principalNetCent()).isEqualTo(s.principalGrossCent() - s.taxCent());
        assertThat(s.customDeductionCent()).isGreaterThanOrEqualTo(0L);
        assertThat(s.payoutCent()).isGreaterThanOrEqualTo(0L);
    }

    /** 全链守恒断言：各方相加 ≡ 可分配额（尾差恒 0，补数式拆分不产生尾差）。 */
    private static void assertConserved(DividendStatementRecord s) {
        long sum = s.platformShareCent() + s.taxCent() + s.arrearsOffsetCent()
                + s.customDeductionCent() + s.reserveWithheldCent() + s.payoutCent();
        assertThat(sum)
                .as("平台分成 + 代扣个税 + 欠款抵扣 + 自定义扣除 + 风险准备金 + 主理人实得 必须 ≡ 可分配额")
                .isEqualTo(s.distributableCent());
    }

    /** #35 自定义扣除封顶不透支：合计超过税后余额时逐项封顶，实发不为负。 */
    @Test
    void customDeductionsClampToAvailableAndNeverNegativePayout() {
        service.createPlan(new DividendPlanRequest("cd-clamp", MERCHANT, "usr_principal", null,
                null, null, "quarterly", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                List.of(new DividendPlanRequest.CustomDeductionItem(
                        "超额扣除", "fixed", 999_999_999L, null, null, true))));
        repository.barRevenue.put(MERCHANT, 1_000_000L);
        DividendStatementRecord s = computeOfficialQuarter();
        // 税后 16 万；固定扣远超 → 封顶实扣 16 万，实发 0（非负）。
        assertThat(s.customDeductionCent()).isEqualTo(160_000L);
        assertThat(s.payoutCent()).isZero();
    }

    /** #35 更新方案：提供 customDeductions 整表替换（空=清空）；不提供（null）保留现值。 */
    @Test
    void updatePlanReplacesCustomDeductionsButNullPreserves() {
        DividendPlanRecord plan = service.createPlan(new DividendPlanRequest("u-cd", MERCHANT, "usr_principal", null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                List.of(new DividendPlanRequest.CustomDeductionItem("原扣除", "fixed", 10_000L, null, null, true))));
        assertThat(plan.customDeductions()).hasSize(1);
        // 不提供 customDeductions（null）→ 保留现值。
        DividendPlanRecord unchanged = service.updatePlan(plan.id(), new DividendPlanRequest("u-cd-2", null,
                null, null, 7000, null, null, null, null, null, null, null, "只改比例",
                null, null, null, null, null, null, null, null, null));
        assertThat(unchanged.customDeductions()).hasSize(1);
        // 提供空列表 → 清空。
        DividendPlanRecord cleared = service.updatePlan(plan.id(), new DividendPlanRequest("u-cd-3", null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, List.of()));
        assertThat(cleared.customDeductions()).isEmpty();
    }

    /** #35 自定义扣除项来源类型非法被拒（配置校验）。 */
    @Test
    void customDeductionValidatesSourceType() {
        assertThatThrownBy(() -> service.createPlan(new DividendPlanRequest("bad-src", MERCHANT, "usr_p", null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                List.of(new DividendPlanRequest.CustomDeductionItem("坏来源", "percent", null, 100, null, true)))))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VALIDATION_FAILED"));
    }
}
