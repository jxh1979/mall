package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.common.api.ApiException;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * D7 超池特批出口 + 超额 C 口径（甲标签法）验收（新建独立测试类，不动 Batch2/Batch34/ComboDiscount/MonthlyJob 用例）。
 *
 * <p>三件套：① 特批出口——超池 draft 单填 special_approval_ref → confirmed（非超池单拒 BILL_NOT_OVERPOOL_DRAFT）；
 * ② 防覆盖——特批后 settleMonth 同月重跑，ref 仍在、status 仍 confirmed（upsertBill COALESCE/CASE）；
 * ③ 超额 C 口径——超池 bill 过账时对池明细两段拆（池内→ticket_distribution、超额→platform_incentive_subsidy），
 *    专属直付不拆，未超池零行为变更，两前缀各自幂等。
 *
 * <p>造超池（S8 两段化）：项目级方式=by_rank_tier(唯一合法值,仅指第二段) + 商户级<b>第一段固定额规则</b>
 * (V374 rule_type=fixed,更隔离,不碰全局默认)。可分配池=800、两家第一段 Q=600+400=1000&gt;800 → overPool
 * (Q&gt;P 不削、第二段为 0,走既有特批路径)。S4:每笔核销带 20% 池链综合服务费(等价保值口径),池 = 收入 − 费。
 *
 * <p><b>S9 短期激励期限（V357）</b>四件套：④ 期限后台可配（项目配 2 月 → 到期日 = 特批日 + 2 月）；
 * ⑤ 到期回收·过账闸——过期特批的超池单过账 409 {@code SPECIAL_APPROVAL_EXPIRED} 且零应付落地；
 * ⑥ 到期回收·冻结解除——过期后月结重跑不再冻结、仍超池即打回 draft，须重新特批（新有效期重新起算）；
 * ⑦ 逐分不变判据——<b>无特批单时期限配 1 月 / 36 月，结算单各金额列逐分一致、生效期留空、过账行为不变</b>。
 *
 * <p>非 @Transactional（settleMonth/postBill 内部各自 @Transactional 真提交）；@AfterEach 按项目清理；
 * 远离今日的确定月 2011-04；每用例独立 projectId 隔离。ar_ap_items 为不可变台账（触发器禁 DELETE），
 * source_id=唯一明细 id 不与后续用例冲突，不清。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementOverPoolSpecialApprovalTests {

    @Autowired private TicketDistributionSettlementService settlementService;
    @Autowired private SettlementPostingService postingService;
    @Autowired private JdbcSettlementBillRepository billRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final YearMonth MONTH = YearMonth.of(2011, 4);
    private final List<String> projectIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 过账走 FinanceService.createArApItem(@GuardedAction)——需登录主体过鉴权/审计（同批2-4 过账测试口径）。
        TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void cleanup() {
        for (String pid : projectIds) {
            jdbcTemplate.update(
                    "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, "DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM ticket_distribution_configs WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM ticket_distribution_merchant_rates WHERE project_id = ?", pid);
        }
        TestSecurityActors.clear();
    }

    // ─── ① 特批出口 ──────────────────────────────────────────────────────────

    @Test
    void overPoolDraftSpecialApprovalFillsRefAndConfirms() {
        String pid = overPoolProject(); // 可分配池=800、池分配 600+400=1000 → overPool draft
        String bill = settlementService.settleMonth(pid, MONTH);

        // 越池 → draft、ref 空、可分配池=800（前置）。
        assertThat(billStatus(bill)).isEqualTo("draft");
        assertThat(specialRef(bill)).isNull();
        assertThat(num(bill(pid), "available_pool_cent")).isEqualTo(800L);

        var returned = postingService.specialApprove(bill, "SA-2026-04-001");

        assertThat(returned.specialApprovalRef()).isEqualTo("SA-2026-04-001");
        // S9：特批必带到期日（出厂默认 3 个月）——R-2「生效期强制字段」。
        assertThat(returned.validUntil()).isEqualTo(LocalDate.now().plusMonths(3));
        assertThat(billStatus(bill)).isEqualTo("confirmed");
        assertThat(specialRef(bill)).isEqualTo("SA-2026-04-001");
    }

    @Test
    void nonOverPoolBillSpecialApprovalRejected() {
        String pid = nonOverPoolProject(); // 池分配 300+200=500 ≤ 800 → 未越池 → confirmed
        String bill = settlementService.settleMonth(pid, MONTH);
        assertThat(billStatus(bill)).isEqualTo("confirmed"); // 未越池自动 confirmed

        assertThatThrownBy(() -> postingService.specialApprove(bill, "SA-x"))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("BILL_NOT_OVERPOOL_DRAFT"));

        // 拒绝后不改单：仍 confirmed、ref 仍空。
        assertThat(billStatus(bill)).isEqualTo("confirmed");
        assertThat(specialRef(bill)).isNull();
    }

    /** 理由为空拒：特批引用是审计留痕的凭据，null/空串/纯空白一律 400，且不动单。 */
    @Test
    void blankSpecialApprovalRefRejected() {
        String pid = overPoolProject();
        String bill = settlementService.settleMonth(pid, MONTH);

        for (String blank : new String[]{null, "", "   "}) {
            assertThatThrownBy(() -> postingService.specialApprove(bill, blank))
                    .isInstanceOf(ApiException.class)
                    .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("SPECIAL_APPROVAL_REF_REQUIRED"));
        }
        assertThat(billStatus(bill)).isEqualTo("draft"); // 拒绝后不动单：仍 draft 待特批
        assertThat(specialRef(bill)).isNull();
    }

    /** 后台入口读投影：listBills 带特批三列——超池 draft 单看得出超了多少，特批后看得到 ref + 有效期。 */
    @Test
    void listBillsExposesOverPoolAndSpecialApprovalColumns() {
        String pid = overPoolProject();
        String bill = settlementService.settleMonth(pid, MONTH);

        var summary = billSummary(pid, bill);
        assertThat(summary.status()).isEqualTo("draft");
        assertThat(summary.availablePoolCent()).isEqualTo(800L);
        assertThat(summary.pooledDistributedCent()).isEqualTo(1000L); // 超池 200 分,前端据此显示「超了多少」
        assertThat(summary.specialApprovalRef()).isNull();
        assertThat(summary.specialApprovalValidUntil()).isNull();

        postingService.specialApprove(bill, "SA-UI-1");

        summary = billSummary(pid, bill);
        assertThat(summary.status()).isEqualTo("confirmed");
        assertThat(summary.specialApprovalRef()).isEqualTo("SA-UI-1");
        assertThat(summary.specialApprovalValidUntil()).isEqualTo(LocalDate.now().plusMonths(3));
    }

    private JdbcSettlementBillRepository.BillSummary billSummary(String pid, String billId) {
        return billRepository.listBills(pid, null, 24).stream()
                .filter(b -> b.id().equals(billId)).findFirst().orElseThrow();
    }

    // ─── ② 防覆盖：特批后 settleMonth 重跑不打回 ──────────────────────────────

    @Test
    void specialApprovalSurvivesResettleSameMonth() {
        String pid = overPoolProject();
        String bill = settlementService.settleMonth(pid, MONTH);
        postingService.specialApprove(bill, "SA-KEEP");
        assertThat(billStatus(bill)).isEqualTo("confirmed");

        // 🔴 同月重跑 settleMonth：upsertBill 的 ON CONFLICT 若直接 EXCLUDED 覆盖，会把 ref 清 null、status 打回 draft。
        settlementService.settleMonth(pid, MONTH);

        assertThat(specialRef(bill)).isEqualTo("SA-KEEP"); // ref 仍在（COALESCE 保留）
        assertThat(billStatus(bill)).isEqualTo("confirmed"); // status 仍 confirmed（CASE 不打回）
    }

    // ─── ③ 超额 C 口径：过账两段拆 ────────────────────────────────────────────

    @Test
    void overPoolPostBillSplitsPoolItemsAndKeepsExclusiveWhole() {
        String pid = overPoolProject();
        // 额外一家专属直付 barX：全额 5000（不占池、永不拆）。
        seedExclusive(pid, "barX", 10, 6, 5000L);
        String bill = settlementService.settleMonth(pid, MONTH);
        postingService.specialApprove(bill, "SA-SPLIT");

        int posted = postingService.postBill(bill);
        assertThat(posted).isEqualTo(3); // m1 + m2 + barX 三条明细

        long availablePool = 800L;
        long poolDistributed = 1000L;

        // m1 a=600 → pool=600*800/1000=480、excess=120。
        String m1Item = poolItem(pid, "m1");
        long m1Pool = 600L * availablePool / poolDistributed;
        long m1Excess = 600L - m1Pool;
        assertThat(m1Pool).isEqualTo(480L);
        assertThat(m1Excess).isEqualTo(120L);
        assertPayable(m1Item, "ticket_distribution", "m1", m1Pool);
        assertPayable(m1Item, "platform_incentive_subsidy", "m1", m1Excess);
        assertThat(m1Pool + m1Excess).isEqualTo(600L); // 不变式 pool+excess=a

        // m2 a=400 → pool=320、excess=80。
        String m2Item = poolItem(pid, "m2");
        long m2Pool = 400L * availablePool / poolDistributed;
        long m2Excess = 400L - m2Pool;
        assertThat(m2Pool).isEqualTo(320L);
        assertThat(m2Excess).isEqualTo(80L);
        assertPayable(m2Item, "ticket_distribution", "m2", m2Pool);
        assertPayable(m2Item, "platform_incentive_subsidy", "m2", m2Excess);
        assertThat(m2Pool + m2Excess).isEqualTo(400L);

        // Σpool ≤ 可分配池；Σexcess = poolDistributed − Σpool。
        long sumPool = m1Pool + m2Pool;
        long sumExcess = m1Excess + m2Excess;
        assertThat(sumPool).isEqualTo(800L);
        assertThat(sumPool).isLessThanOrEqualTo(availablePool);
        assertThat(sumExcess).isEqualTo(poolDistributed - sumPool);
        assertThat(sumExcess).isEqualTo(200L);

        // 专属直付 barX：单条 ticket_distribution 全额 5000、无补贴行。
        String exItem = exclusiveItem(pid, "barX");
        assertPayable(exItem, "ticket_distribution", "barX", 5000L);
        assertThat(payableCount(exItem, "platform_incentive_subsidy")).isZero();

        // 全过 → 单 settled。
        assertThat(billStatus(bill)).isEqualTo("settled");
    }

    @Test
    void nonOverPoolPostBillSingleTicketDistributionNoSubsidy() {
        String pid = nonOverPoolProject(); // 300+200=500 ≤ 800 → 未越池 confirmed
        String bill = settlementService.settleMonth(pid, MONTH);

        int posted = postingService.postBill(bill);
        assertThat(posted).isEqualTo(2);

        // 每家单条 ticket_distribution = 其固定额；无 platform_incentive_subsidy 行（零行为变更）。
        String m1Item = poolItem(pid, "m1");
        assertPayable(m1Item, "ticket_distribution", "m1", 300L);
        assertThat(payableCount(m1Item, "platform_incentive_subsidy")).isZero();
        String m2Item = poolItem(pid, "m2");
        assertPayable(m2Item, "ticket_distribution", "m2", 200L);
        assertThat(payableCount(m2Item, "platform_incentive_subsidy")).isZero();

        assertThat(billStatus(bill)).isEqualTo("settled");
    }

    @Test
    void postBillIsIdempotentAcrossBothPrefixes() {
        String pid = overPoolProject();
        String bill = settlementService.settleMonth(pid, MONTH);
        postingService.specialApprove(bill, "SA-IDEM");
        postingService.postBill(bill); // 首过：每家 td + pis 各一条

        String m1Item = poolItem(pid, "m1");
        String m2Item = poolItem(pid, "m2");
        // 白盒复演：把单打回 confirmed、明细打回 draft（不清 ar_ap）→ 再过账须命中幂等、不双挂。
        jdbcTemplate.update("UPDATE settlement_bills SET status = 'confirmed', settled_at = NULL WHERE id = ?", bill);
        jdbcTemplate.update("UPDATE settlement_items SET status = 'draft' WHERE bill_id = ?", bill);

        postingService.postBill(bill);

        // 两前缀各自幂等：td 仍 1、pis 仍 1（每家），无重复挂账。
        assertThat(payableCount(m1Item, "ticket_distribution")).isEqualTo(1);
        assertThat(payableCount(m1Item, "platform_incentive_subsidy")).isEqualTo(1);
        assertThat(payableCount(m2Item, "ticket_distribution")).isEqualTo(1);
        assertThat(payableCount(m2Item, "platform_incentive_subsidy")).isEqualTo(1);
    }

    // ─── 对抗复核回归锁：溢出安全 / 特批冻结 / 补贴出项目域 ───────────────────────

    /** Fix1 溢出(risk1):a×可分配池 超 long 上限时 BigInteger 中间乘保 pool+excess=a 且 pool≥0(long 直乘会反转成负应付)。 */
    @Test
    void overflowSafeSplitPreservesConservation() {
        String pid = hugeOverPoolProject(); // 固定额 5e9×2、营收 1e10 → 可分配池 8e9、poolDistributed 1e10（超池）
        String bill = settlementService.settleMonth(pid, MONTH);
        postingService.specialApprove(bill, "SA-HUGE");
        postingService.postBill(bill);

        long a = 5_000_000_000L, availablePool = 8_000_000_000L, poolDistributed = 10_000_000_000L;
        long poolI = java.math.BigInteger.valueOf(a).multiply(java.math.BigInteger.valueOf(availablePool))
                .divide(java.math.BigInteger.valueOf(poolDistributed)).longValueExact(); // 4e9
        long excessI = a - poolI; // 1e9
        // 佐证溢出：a×池=4e19>Long.MAX(9.22e18) → long 直乘回绕(此处成正 3.1e18≠真积)，
        // 老口径 poolI=(a*池)/poolDistributed 会算成 ≈3.1e8（偏小、补贴虚高）→ 与 BigInteger 真 poolI 不等。
        long oldWrappedPoolI = (a * availablePool) / poolDistributed;
        assertThat(oldWrappedPoolI).isNotEqualTo(poolI);
        String m1Item = poolItem(pid, "m1");
        assertPayable(m1Item, "ticket_distribution", "m1", poolI);
        assertPayable(m1Item, "platform_incentive_subsidy", "m1", excessI);
        assertThat(poolI).isGreaterThan(0L);
        assertThat(poolI + excessI).isEqualTo(a);
    }

    /** Fix2 冻结(risk2):特批后又来核销再 settleMonth，须返回原单、明细总额冻结不漂移、ref 仍在。 */
    @Test
    void specialApprovedBillFrozenAgainstNewTransactions() {
        String pid = overPoolProject();
        String bill = settlementService.settleMonth(pid, MONTH);
        postingService.specialApprove(bill, "SA-FREEZE");
        long itemSumBefore = itemFinalSum(bill);

        // 特批锁定的是「该次超池特定金额」；此后新增核销普通重跑会把明细抬高、与已批总额脱节 → 必须冻结。
        seedPooled(pid, "m1", 3, 1, 300L);
        String again = settlementService.settleMonth(pid, MONTH);

        assertThat(again).isEqualTo(bill);                         // 返回原单 id、未新建
        assertThat(itemFinalSum(bill)).isEqualTo(itemSumBefore);   // 明细总额冻结不变
        assertThat(specialRef(bill)).isEqualTo("SA-FREEZE");
        assertThat(billStatus(bill)).isEqualTo("confirmed");
    }

    /** Fix3 红线b(risk3):补贴应付 project_id 必为 NULL——落平台主体域,不进任一项目公司聚合。 */
    @Test
    void subsidyPayableHasNullProjectIdOutOfProjectDomain() {
        String pid = overPoolProject();
        String bill = settlementService.settleMonth(pid, MONTH);
        postingService.specialApprove(bill, "SA-PID");
        postingService.postBill(bill);

        String m1Item = poolItem(pid, "m1");
        Object projectIdCol = jdbcTemplate.queryForMap(
                "SELECT project_id FROM ar_ap_items WHERE source_type = 'platform_incentive_subsidy' AND source_id = ?",
                m1Item).get("project_id");
        assertThat(projectIdCol).isNull();
    }

    // ─── S9 短期激励期限：生效期 + 到期回收 + 无特批单零变更 ─────────────────────

    /** 期限后台可配：项目配 2 个月 → 到期日 = 特批当日 + 2 个月（不是写死的 3）。 */
    @Test
    void specialApprovalValidUntilFollowsConfiguredTerm() {
        String pid = overPoolProject();
        setProjectSpecialApprovalTermMonths(pid, 2);
        String bill = settlementService.settleMonth(pid, MONTH);

        var result = postingService.specialApprove(bill, "SA-TERM-2");

        assertThat(result.validUntil()).isEqualTo(LocalDate.now().plusMonths(2));
        assertThat(validUntil(bill)).isEqualTo(LocalDate.now().plusMonths(2));
    }

    /**
     * V368（用户 2026-07-28 纠正「不是指校验大于等于一个月，也可以三天」）：
     * 期限配 3 天 → 到期日 = 特批当日 + 3 天。三天的短期激励活动必须真的批得出三天，
     * 而不是被「至少一个月」的下限顶成一个月。
     */
    @Test
    void specialApprovalTermCanBeConfiguredInDays() {
        String pid = overPoolProject();
        setProjectSpecialApprovalTerm(pid, 3, "day");
        String bill = settlementService.settleMonth(pid, MONTH);

        var result = postingService.specialApprove(bill, "SA-TERM-3D");

        assertThat(result.validUntil()).isEqualTo(LocalDate.now().plusDays(3));
        assertThat(validUntil(bill)).isEqualTo(LocalDate.now().plusDays(3));
    }

    /** 默认没动：不配任何项目特例 → 仍是出厂默认 3 个月（业务口径「期限可配，默认不超过 3 个月」）。 */
    @Test
    void specialApprovalTermDefaultsToThreeMonths() {
        String pid = overPoolProject();
        String bill = settlementService.settleMonth(pid, MONTH);

        var result = postingService.specialApprove(bill, "SA-TERM-DEFAULT");

        assertThat(result.validUntil()).isEqualTo(LocalDate.now().plusMonths(3));
        assertThat(validUntil(bill)).isEqualTo(LocalDate.now().plusMonths(3));
    }

    /** 到期回收①：特批过期后超池单不可过账（fail-closed），且一分钱没落台账。 */
    @Test
    void expiredSpecialApprovalBlocksOverPoolPosting() {
        String pid = overPoolProject();
        String bill = settlementService.settleMonth(pid, MONTH);
        postingService.specialApprove(bill, "SA-EXPIRE");
        expireApproval(bill); // 时间快进到到期日之后

        assertThatThrownBy(() -> postingService.postBill(bill))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).code()).isEqualTo("SPECIAL_APPROVAL_EXPIRED"));

        // 拒绝即止：明细仍 draft、无任何应付落地（超池部分不会溜进台账）。
        String m1Item = poolItem(pid, "m1");
        assertThat(payableCount(m1Item, "ticket_distribution")).isZero();
        assertThat(payableCount(m1Item, "platform_incentive_subsidy")).isZero();
        assertThat(billStatus(bill)).isEqualTo("confirmed");
    }

    /** 到期回收②：过期特批不再冻结月结重跑 → 单被打回 draft 待重新特批；重新特批即拿到新有效期。 */
    @Test
    void expiredSpecialApprovalNoLongerFreezesResettleAndRequiresReapproval() {
        String pid = overPoolProject();
        String bill = settlementService.settleMonth(pid, MONTH);
        postingService.specialApprove(bill, "SA-OLD");
        expireApproval(bill);

        // 特批有效期内重跑会被冻结（见 specialApprovedBillFrozenAgainstNewTransactions）；过期后照常重算。
        seedPooled(pid, "m1", 1, 1, 100L); // 收入 1100 → 池 1100 − 池链服务费 220 = 880 < 固定额分配 1000（仍超池）
        String again = settlementService.settleMonth(pid, MONTH);

        assertThat(again).isEqualTo(bill);
        assertThat(num(bill(pid), "available_pool_cent")).isEqualTo(880L); // 800→880：确实重算了，不是被冻结
        assertThat(billStatus(bill)).isEqualTo("draft"); // 仍超池 → 打回待特批 = 回收后不再允许超池
        assertThat(specialRef(bill)).isEqualTo("SA-OLD"); // 过期引用留痕不清空

        // 回收后要继续，只能由平台负责人按当期金额重新特批 → 新有效期从今天重新起算。
        var renewed = postingService.specialApprove(bill, "SA-RENEW");
        assertThat(renewed.validUntil()).isEqualTo(LocalDate.now().plusMonths(3));
        assertThat(billStatus(bill)).isEqualTo("confirmed");
    }

    /** 逐分不变判据：无特批单时，期限配成多少都不改结算单任何金额列，也不改过账行为。 */
    @Test
    void withoutSpecialApprovalTermConfigChangesNothing() {
        String pidShort = nonOverPoolProject();
        setProjectSpecialApprovalTermMonths(pidShort, 1);
        String billShort = settlementService.settleMonth(pidShort, MONTH);

        String pidLong = nonOverPoolProject();
        setProjectSpecialApprovalTermMonths(pidLong, 36);
        String billLong = settlementService.settleMonth(pidLong, MONTH);

        // ① 金额列逐分一致（期限只管特批出口，不进任何金额公式）。
        for (String col : List.of("ticket_revenue_cent", "channel_fee_cent",
                "available_pool_cent", "distributed_cent",
                "comp_service_fee_pooled_cent", "comp_service_fee_exclusive_cent",
                "combo_discount_cent", "rounding_residual_cent", "pool_undistributed_cent")) {
            assertThat(num(bill(pidShort), col)).as(col).isEqualTo(num(bill(pidLong), col));
        }
        // ② 无特批 → 无生效期（列留空，CHECK 只在有特批引用时要求非空）。
        assertThat(specialRef(billShort)).isNull();
        assertThat(validUntil(billShort)).isNull();
        // ③ 过账照常（未超池永不进到期判定分支）。
        assertThat(postingService.postBill(billShort)).isEqualTo(2);
        assertThat(postingService.postBill(billLong)).isEqualTo(2);
        assertThat(billStatus(billShort)).isEqualTo("settled");
        assertThat(billStatus(billLong)).isEqualTo("settled");
    }

    // ─── 造数据 ───────────────────────────────────────────────────────────────

    /** 溢出用超池项目：固定额 5e9×2、营收 5e9×2=1e10、池链服务费 2e9 → 可分配池=8e9、poolDistributed=1e10（a×池 越 long 上限）。 */
    private String hugeOverPoolProject() {
        String pid = newProject();
        setProjectFixedMethod(pid);
        setMerchantFixed(pid, "m1", 5_000_000_000L);
        setMerchantFixed(pid, "m2", 5_000_000_000L);
        seedPooled(pid, "m1", 6, 3, 5_000_000_000L);
        seedPooled(pid, "m2", 4, 2, 5_000_000_000L);
        return pid;
    }

    private long itemFinalSum(String billId) {
        Long s = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(final_amount_cent), 0) FROM settlement_items WHERE bill_id = ?", Long.class, billId);
        return s == null ? 0L : s;
    }

    /** 越池项目：第一段固定规则 m1 固定 600 / m2 固定 400，核销收入 1000、池链服务费 200 → 可分配池=800 < Q=1000。 */
    private String overPoolProject() {
        String pid = newProject();
        setProjectFixedMethod(pid);
        setMerchantFixed(pid, "m1", 600L);
        setMerchantFixed(pid, "m2", 400L);
        seedPooled(pid, "m1", 6, 3, 600L);
        seedPooled(pid, "m2", 4, 2, 400L);
        return pid;
    }

    /** 未越池项目：第一段固定规则 m1 固定 300 / m2 固定 200 → Q=500 ≤ 可分配池=800(余额 300 显式归平台留痕)。 */
    private String nonOverPoolProject() {
        String pid = newProject();
        setProjectFixedMethod(pid);
        setMerchantFixed(pid, "m1", 300L);
        setMerchantFixed(pid, "m2", 200L);
        seedPooled(pid, "m1", 6, 3, 600L);
        seedPooled(pid, "m2", 4, 2, 400L);
        return pid;
    }

    private String newProject() {
        String pid = "proj_d7_" + System.nanoTime();
        projectIds.add(pid);
        return pid;
    }

    /** S8:项目级配置行只承载第二段方式(by_rank_tier,唯一合法值)+ 特批有效期;超池由商户级固定规则造。 */
    private void setProjectFixedMethod(String projectId) {
        jdbcTemplate.update("""
                INSERT INTO ticket_distribution_configs (id, project_id, method_type, status, is_draft)
                VALUES (?, ?, 'by_rank_tier', 'active', FALSE)
                """, "tdc_d7_" + projectId, projectId);
    }

    /** S8/V374:商户级第一段固定额规则(rule_type=fixed)。 */
    private void setMerchantFixed(String projectId, String merchantId, long fixedCent) {
        jdbcTemplate.update("""
                INSERT INTO ticket_distribution_merchant_rates (id, project_id, merchant_id,
                    rule_type, per_guest_cent, fixed_amount_cent, status, is_draft)
                VALUES (?, ?, ?, 'fixed', NULL, ?, 'active', FALSE)
                """, "tdmr_d7_" + projectId + "_" + merchantId, projectId, merchantId, fixedCent);
    }

    /** 非专属（进池）核销行。S4:每笔带 20% 池链综合服务费(等价保值口径)。 */
    private void seedPooled(String projectId, String merchantId, int verifiedCount, int numPeople, long verifiedCent) {
        insert(projectId, merchantId, "entry", verifiedCount, numPeople, verifiedCent, null, verifiedCent / 5);
    }

    /** 专属核销行（exclusive_merchant_id=该商户;该商户专属费率 0 → 费恒 0）。 */
    private void seedExclusive(String projectId, String exclusiveMerchantId, int verifiedCount, int numPeople,
                               long verifiedCent) {
        insert(projectId, exclusiveMerchantId, "bar_drink", verifiedCount, numPeople, verifiedCent,
                exclusiveMerchantId, 0L);
    }

    private void insert(String projectId, String merchantId, String entType, int verifiedCount, int numPeople,
                        long verifiedCent, String exclusiveMerchantId, long compServiceFeeCent) {
        Instant businessAt = MONTH.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(businessAt);
        String id = "st_d7_" + System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    exclusive_merchant_id, verification_id, settlement_status, entitlement_valid_to, created_at, updated_at,
                    business_at, discount_amount_cent, comp_service_fee_cent)
                VALUES (?, ?, 'verification_settled', ?, ?, ?, ?, ?, ?, ?, 0, 'merchant', ?, ?, ?, ?, 'draft', ?, ?, ?, ?, 0, ?)
                """,
                id, "TN-" + id, "ent_" + id, entType, verifiedCount,
                Math.max(verifiedCount, 1), numPeople, verifiedCent, verifiedCent,
                merchantId, projectId, exclusiveMerchantId, "vf_" + id, ts, ts, ts, ts, compServiceFeeCent);
    }

    // ─── 断言/读取 helpers ─────────────────────────────────────────────────────

    private String billStatus(String billId) {
        return jdbcTemplate.queryForObject("SELECT status FROM settlement_bills WHERE id = ?", String.class, billId);
    }

    private String specialRef(String billId) {
        return jdbcTemplate.queryForObject(
                "SELECT special_approval_ref FROM settlement_bills WHERE id = ?", String.class, billId);
    }

    private LocalDate validUntil(String billId) {
        return jdbcTemplate.queryForObject(
                "SELECT special_approval_valid_until FROM settlement_bills WHERE id = ?", LocalDate.class, billId);
    }

    /** 时间快进：把到期日推到昨天（不动系统时钟，只改这张单的有效期，等价于「已到期」）。 */
    private void expireApproval(String billId) {
        jdbcTemplate.update(
                "UPDATE settlement_bills SET special_approval_valid_until = CURRENT_DATE - 1 WHERE id = ?", billId);
    }

    /** 项目级超池特批有效期（月）；不配则回落全局默认 3 个月。 */
    private void setProjectSpecialApprovalTermMonths(String projectId, int months) {
        setProjectSpecialApprovalTerm(projectId, months, "month");
    }

    /** V368：项目级超池特批有效期 = 数值 + 单位（day/month）。 */
    private void setProjectSpecialApprovalTerm(String projectId, int value, String unit) {
        jdbcTemplate.update(
                "UPDATE ticket_distribution_configs"
                        + " SET special_approval_term_value = ?, special_approval_term_unit = ?"
                        + " WHERE project_id = ?",
                value, unit, projectId);
    }

    private Map<String, Object> bill(String projectId) {
        return jdbcTemplate.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ?", projectId);
    }

    private String poolItem(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.id FROM settlement_items si JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ? AND si.distribution_method <> 'exclusive_full'
                """, String.class, projectId, merchantId);
    }

    private String exclusiveItem(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.id FROM settlement_items si JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ? AND si.distribution_method = 'exclusive_full'
                """, String.class, projectId, merchantId);
    }

    /** 断言某明细某 source_type 恰有一条应付：direction=payable、counterparty=merchant、amount=expected。 */
    private void assertPayable(String itemId, String sourceType, String merchantId, long expectedCent) {
        Map<String, Object> arap = jdbcTemplate.queryForMap(
                "SELECT * FROM ar_ap_items WHERE source_type = ? AND source_id = ?", sourceType, itemId);
        assertThat(arap.get("direction")).isEqualTo("payable");
        assertThat(arap.get("counterparty_id")).isEqualTo(merchantId);
        assertThat(((Number) arap.get("amount_cent")).longValue()).isEqualTo(expectedCent);
        assertThat(payableCount(itemId, sourceType)).isEqualTo(1);
    }

    private int payableCount(String itemId, String sourceType) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ar_ap_items WHERE source_type = ? AND source_id = ?",
                Integer.class, sourceType, itemId);
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
