package com.complex.platform.dividend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.complex.platform.dividend.DividendRecords.DividendStatementDetailRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementLineRecord;
import com.complex.platform.dividend.DividendRecords.DividendStatementRecord;
import com.complex.platform.dividend.DividendRecords.ExpenseTypeSumRecord;
import com.complex.platform.dividend.dto.DividendActionRequest;
import com.complex.platform.dividend.dto.DividendComputeRequest;
import com.complex.platform.dividend.dto.DividendPlanRequest;
import com.complex.platform.evidence.AuditEventService;
import com.complex.platform.evidence.AuditedOperationExecutor;
import com.complex.platform.finance.DividendPayoutPort;
import com.complex.platform.inventory.InventoryCostSummaryRecord;
import com.complex.platform.inventory.InventoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
 */
class DividendServiceTests {

    private static final String MERCHANT = "merchant_a";

    /** 内存仓储：方案唯一性/计算单状态机/明细行 + 可设的三来源取数值。 */
    private static class InMemoryRepo implements DividendRepository {
        final Map<String, DividendPlanRecord> plans = new LinkedHashMap<>();
        final Map<String, DividendStatementRecord> statements = new LinkedHashMap<>();
        final List<DividendStatementLineRecord> lines = new ArrayList<>();
        final Map<String, Long> barRevenue = new HashMap<>();
        final Map<String, Long> barRefund = new HashMap<>();
        final Map<String, Long> ticketAllocated = new HashMap<>();
        final Map<String, Long> ticketRefund = new HashMap<>();
        final Map<String, List<ExpenseTypeSumRecord>> expenses = new HashMap<>();

        @Override
        public List<DividendPlanRecord> listPlans(String merchantId) {
            return plans.values().stream()
                    .filter(p -> merchantId == null || p.merchantId().equals(merchantId)).toList();
        }

        @Override
        public Optional<DividendPlanRecord> findPlanById(String id) {
            return Optional.ofNullable(plans.get(id));
        }

        @Override
        public Optional<DividendPlanRecord> findActivePlanByMerchant(String merchantId) {
            return plans.values().stream()
                    .filter(p -> p.merchantId().equals(merchantId) && "active".equals(p.status())).findFirst();
        }

        @Override
        public List<DividendPlanRecord> listActivePlans() {
            return plans.values().stream().filter(p -> "active".equals(p.status())).toList();
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
                    paidBy != null ? paidBy : s.paidBy(), s.paidAt(), s.createdAt(), s.updatedAt());
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
            return barRevenue.getOrDefault(merchantId, 0L);
        }

        @Override
        public long sumBarRefundCent(String merchantId, Instant from, Instant to) {
            return barRefund.getOrDefault(merchantId, 0L);
        }

        @Override
        public long sumTicketAllocatedCent(String merchantId, Instant from, Instant to) {
            return ticketAllocated.getOrDefault(merchantId, 0L);
        }

        @Override
        public long sumTicketRefundCent(String merchantId, Instant from, Instant to) {
            return ticketRefund.getOrDefault(merchantId, 0L);
        }

        @Override
        public List<ExpenseTypeSumRecord> sumExpenseByType(
                String merchantId, List<String> months, List<String> expenseTypes) {
            return expenses.getOrDefault(merchantId, List.of()).stream()
                    .filter(e -> expenseTypes.contains(e.expenseType())).toList();
        }
    }

    /** 内存幂等仓储（MerchantExpenseServiceTests 同款）：载荷指纹与重放语义走真 IdempotencyService。 */
    private static final class InMemoryIdempotencyRepository implements IdempotencyRepository {
        private final Map<String, IdempotencyRecord> records = new LinkedHashMap<>();

        @Override
        public Optional<IdempotencyRecord> findByOperationIdAndIdempotencyKey(String operationId, String idempotencyKey) {
            return Optional.ofNullable(records.get(operationId + ":" + idempotencyKey));
        }

        @Override
        public boolean tryReserve(IdempotencyRecord record) {
            String key = record.operationId() + ":" + record.idempotencyKey();
            if (records.containsKey(key)) {
                return false;
            }
            records.put(key, record);
            return true;
        }

        @Override
        public IdempotencyRecord complete(IdempotencyRecord record) {
            records.put(record.operationId() + ":" + record.idempotencyKey(), record);
            return record;
        }
    }

    private InMemoryRepo repository;
    private AuthorizationService authorizationService;
    private InventoryRepository inventoryRepository;
    private DividendPayoutPort payoutPort;
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
        payoutPort = mock(DividendPayoutPort.class);
        when(payoutPort.createDividendPayoutPayable(anyString(), anyString(), org.mockito.ArgumentMatchers.anyLong(),
                anyString())).thenReturn("arap_div_1");
        AuditedOperationExecutor operations = new AuditedOperationExecutor(
                new IdempotencyService(new InMemoryIdempotencyRepository(), new ObjectMapper()),
                mock(AuditEventService.class));
        service = new DividendService(repository, inventoryRepository, new BusinessDayResolver(4),
                authorizationService, operations, payoutPort);
    }

    private DividendPlanRecord createDefaultPlan() {
        return service.createPlan(new DividendPlanRequest("plan-key-1", MERCHANT, "usr_principal", "主理人甲",
                null, null, null, null, null, null, null, null, null));
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
        // 默认口径：80/20、股息红利 20%、季度、收入域 bar+ticket、扣成本、费用全类别。
        assertThat(plan.platformShareBps()).isEqualTo(8000);
        assertThat(plan.taxRateBps()).isEqualTo(2000);
        assertThat(plan.periodType()).isEqualTo("quarterly");
        assertThat(plan.revenueDomains()).containsExactly("bar", "ticket");
        assertThat(plan.includeInventoryCost()).isTrue();
        assertThat(plan.expenseTypes()).containsExactly(
                "rent_allocation", "energy", "property_fee", "labor", "amortization", "other");
        assertThat(plan.status()).isEqualTo("active");
        // 同商户第二个生效方案被拒（一个商户同一时期只允许一个生效中方案）。
        assertThatThrownBy(() -> service.createPlan(new DividendPlanRequest("plan-key-2", MERCHANT,
                "usr_other", null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("DIVIDEND_PLAN_EXISTS"));
    }

    @Test
    void createPlanValidatesBpsAndTokens() {
        assertThatThrownBy(() -> service.createPlan(new DividendPlanRequest("bad-bps", MERCHANT,
                "usr_p", null, 10001, null, null, null, null, null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VALIDATION_FAILED"));
        assertThatThrownBy(() -> service.createPlan(new DividendPlanRequest("bad-domain", MERCHANT,
                "usr_p", null, null, null, null, List.of("bar", "karaoke"), null, null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void updatePlanChangesRatioButNotMerchant() {
        DividendPlanRecord plan = createDefaultPlan();
        DividendPlanRecord updated = service.updatePlan(plan.id(), new DividendPlanRequest("upd-key-1", null,
                null, null, 7000, 0, null, null, null, null, null, null, "比例调整为 70/30，免税试点"));
        assertThat(updated.platformShareBps()).isEqualTo(7000);
        assertThat(updated.taxRateBps()).isZero();
        assertThat(updated.principalUserId()).isEqualTo("usr_principal");
        assertThatThrownBy(() -> service.updatePlan(plan.id(), new DividendPlanRequest("upd-key-2",
                "merchant_b", null, null, null, null, null, null, null, null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("VALIDATION_FAILED"));
    }

    @Test
    void computeProducesIdentityConsistentStatementWithSnapshotAndLines() {
        createDefaultPlan();
        repository.barRevenue.put(MERCHANT, 800_000L);
        repository.barRefund.put(MERCHANT, 50_000L);
        repository.ticketAllocated.put(MERCHANT, 300_000L);
        repository.ticketRefund.put(MERCHANT, 30_000L);
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
        // 明细行：来源行合计 = 净利润（算式留痕数字恒等）。
        List<DividendStatementLineRecord> lines = repository.listLines(statement.id(), null);
        long sourceSum = lines.stream()
                .filter(l -> !List.of("net_profit", "platform_share", "principal_gross", "tax_withheld",
                        "principal_net").contains(l.lineType()))
                .mapToLong(DividendStatementLineRecord::amountCent).sum();
        assertThat(sourceSum).isEqualTo(statement.netProfitCent());
        assertThat(lines).anyMatch(l -> l.lineType().equals("cost_inventory") && l.detail().contains("无成本基准"));
    }

    @Test
    void computeRespectsBaseCompositionSwitches() {
        // 只计酒吧收入、不扣成本、只计人工费用。
        service.createPlan(new DividendPlanRequest("plan-key-cfg", MERCHANT, "usr_principal", null,
                5000, 1000, "quarterly", List.of("bar"), false, List.of("labor"), null, null, null));
        repository.barRevenue.put(MERCHANT, 500_000L);
        repository.ticketAllocated.put(MERCHANT, 999_999L);
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
        // 净发放额 = 1000000×20%−20%税 = 160000，按此额挂应付给主理人。
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
        verify(payoutPort, never()).createDividendPayoutPayable(anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyLong(), anyString());
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
                new BusinessDayResolver(4), authorizationService, operations, payoutPort);
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
                null, null, null, null, null, "ended", null));
        service.createPlan(new DividendPlanRequest("plan-key-2", MERCHANT, "usr_principal_2", null,
                null, null, null, null, null, null, null, null, null));
        assertThatThrownBy(() -> service.updatePlan(first.id(), new DividendPlanRequest("react-key", null,
                null, null, null, null, null, null, null, null, null, "active", null)))
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
}
