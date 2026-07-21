package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.TestSecurityActors;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 结算配置中心批1(通票分账):月度作业 ① 自动过账触发缺口验收(补 postBill 生产零调用)。
 *
 * <p>覆盖 {@link SettlementMonthlyJob#scheduledSettle} 新增的「settleMonth 后按 bill 状态自动过账」逻辑:
 * <ul>
 *   <li><b>未越池月 → confirmed → 自动过账</b>:结算单落 settled、各酒吧分账挂 ar_ap_items
 *       (direction=payable、source_type='ticket_distribution')、明细回填 status=posted;</li>
 *   <li><b>超池月 → draft(待特批)→ 不过账</b>:结算单保持 draft、明细保持 draft、<b>无 ar_ap</b>
 *       (job 的 {@code if status==confirmed} 门天然拦住,超池 draft 单只能走网关端点特批后人工过);</li>
 *   <li><b>重跑幂等</b>:已 settled 月被 job 状态预检跳过(不重结/不双过),ar_ap 不翻倍;draft 月重算仍 draft、仍不过。</li>
 * </ul>
 *
 * <p>作业口径固定用 {@code settleMonth(null, month)}=平台默认口径,读<b>全局</b>分配配置。默认全局方式
 * by_rank_tier 的档位份额和 ≤100% 天然不越池、造不出 draft,故本类 @BeforeEach 临时把<b>全局默认</b>方式
 * 切 by_fixed_amount(唯一全局行,UPDATE+@AfterEach 还原;Surefire 顺序跑无并发,还原保证零泄漏),
 * 再配唯一商户全局固定额造「未越池/超池」两月。全局配置改动仅本方法窗口内、结束即还原。
 *
 * <p>月窗选 2009 年远离其它用例(2013-05)与今日,避免 null 口径聚合(跨项目)误吸其它用例遗留核销。
 * 非 @Transactional(settleMonth/postBill 内部各自 @Transactional 真提交);@AfterEach 清窗口内落地行 + 还原全局方式。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementMonthlyJobPostingTests {

    @Autowired
    private TicketDistributionSettlementService settlementService;

    @Autowired
    private SettlementPostingService postingService;

    @Autowired
    private JdbcSettlementBillRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 未越池(confirmed)月 = 作业 lastMonth;超池(draft)月 = 其上一月;两月均落在默认 6 月回看窗口 [2009-03,2009-08] 内。
    private static final YearMonth CONFIRMED_MONTH = YearMonth.of(2009, 8);
    private static final YearMonth DRAFT_MONTH = YearMonth.of(2009, 7);

    // 唯一商户 + 唯一造数项目(便于清理;null 口径聚合忽略 project_id,仅用于清理定位)。
    private final String suffix = Long.toHexString(System.nanoTime() & 0xFFFFFL);
    private final String mcMerchant = "jc" + suffix; // 未越池:固定额 < 总池 → confirmed
    private final String mdMerchant = "jd" + suffix; // 超池:固定额 >> 总池 → draft
    private final String seedProject = "jobtst_" + suffix;

    private String originalGlobalMethod;

    @BeforeEach
    void setUp() {
        // 过账走 FinanceService.createArApItem(@GuardedAction)——需登录主体过鉴权/审计(同批2-4 过账测试口径)。
        TestSecurityActors.asPlatformAdmin();
        // 临时把全局默认分配方式切 by_fixed_amount(唯一全局行),使 settleMonth(null,月) 能按固定额造 未越池/超池 两态。
        originalGlobalMethod = jdbcTemplate.queryForObject(
                "SELECT method_type FROM ticket_distribution_configs"
                        + " WHERE project_id IS NULL AND is_draft = FALSE AND status = 'active'",
                String.class);
        jdbcTemplate.update("UPDATE ticket_distribution_configs SET method_type = 'by_fixed_amount'"
                + " WHERE project_id IS NULL AND is_draft = FALSE AND status = 'active'");
    }

    @AfterEach
    void cleanup() {
        // 还原全局默认方式(顺序跑,还原后下一用例恢复 by_rank_tier 默认;guard null 防 setUp 半途失败)。
        if (originalGlobalMethod != null) {
            jdbcTemplate.update("UPDATE ticket_distribution_configs SET method_type = ?"
                    + " WHERE project_id IS NULL AND is_draft = FALSE AND status = 'active'", originalGlobalMethod);
        }
        jdbcTemplate.update("DELETE FROM ticket_distribution_merchant_rates WHERE merchant_id IN (?, ?)",
                mcMerchant, mdMerchant);
        // 窗口内 null 口径结算单 id 恒为 sb_default_2009-XX(仅本用例产出),先清明细再清单。
        jdbcTemplate.update("DELETE FROM settlement_items WHERE bill_id LIKE 'sb_default_2009-%'");
        jdbcTemplate.update("DELETE FROM settlement_bills WHERE id LIKE 'sb_default_2009-%'");
        jdbcTemplate.update("DELETE FROM settlement_transactions WHERE project_id = ?", seedProject);
        // ar_ap_items 为不可变台账(DB 触发器禁 DELETE),source_id=唯一明细 id 不与后续用例冲突,不清。
        TestSecurityActors.clear();
    }

    @Test
    void scheduledSettleAutoPostsConfirmedBillAndSkipsDraftOverPool() {
        // 未越池月:收入 100000 → 池 100000、留存 20% → 总池 80000;固定额 5000 ≤ 80000 → confirmed。
        seedVerified(mcMerchant, 8, 4, 100000L, CONFIRMED_MONTH);
        setGlobalMerchantFixed(mcMerchant, 5000L);
        // 超池月:收入 1000 → 总池 800;固定额 500000 >> 800 → draft(待特批)。
        seedVerified(mdMerchant, 3, 2, 1000L, DRAFT_MONTH);
        setGlobalMerchantFixed(mdMerchant, 500000L);

        // 固定 Clock:now=2009-09 → lastMonth=2009-08=CONFIRMED_MONTH;默认回看 6 月覆盖 DRAFT_MONTH。
        SettlementMonthlyJob job = new SettlementMonthlyJob(settlementService, repository, postingService,
                Clock.fixed(CONFIRMED_MONTH.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                        ZoneOffset.UTC));

        job.scheduledSettle();

        // ---- ① 未越池月 confirmed → 自动过账:单 settled、明细 posted、ar_ap 落地(payable/ticket_distribution/金额/对手方)----
        String confirmedBill = "sb_default_" + CONFIRMED_MONTH; // sb_default_2009-08
        assertThat(billStatus(confirmedBill)).isEqualTo("settled");
        String mcItem = itemId(confirmedBill, mcMerchant);
        assertThat(itemStatus(mcItem)).isEqualTo("posted");
        Map<String, Object> arap = arApBySource(mcItem);
        assertThat(arap.get("direction")).isEqualTo("payable");
        assertThat(arap.get("source_type")).isEqualTo("ticket_distribution");
        assertThat(arap.get("counterparty_id")).isEqualTo(mcMerchant);
        assertThat(((Number) arap.get("amount_cent")).longValue()).isEqualTo(5000L);
        assertThat(arApCount(mcItem)).isEqualTo(1);

        // ---- ② 超池月 draft → 不过账:单 draft、明细 draft、无 ar_ap ----
        String draftBill = "sb_default_" + DRAFT_MONTH; // sb_default_2009-07
        assertThat(billStatus(draftBill)).isEqualTo("draft");
        String mdItem = itemId(draftBill, mdMerchant);
        assertThat(itemStatus(mdItem)).isEqualTo("draft");
        assertThat(arApCount(mdItem)).isZero();

        // ---- ③ 重跑幂等:已 settled 月被状态预检跳过(不双过),ar_ap 仍 1;draft 月重算仍 draft、仍无 ar_ap ----
        job.scheduledSettle();
        assertThat(arApCount(mcItem)).isEqualTo(1);
        assertThat(billStatus(confirmedBill)).isEqualTo("settled");
        assertThat(billStatus(draftBill)).isEqualTo("draft");
        assertThat(arApCount(mdItem)).isZero();
    }

    /**
     * 🔴 过账独立闸(main follow-up):postingEnabled=false 时,settleMonth 照常算单→confirmed(内部计算),
     * 但 postBill <b>不执行</b>——单保持 confirmed、明细 draft、无 ar_ap。防"月度作业开关 prod=true 连带自动过账"激活账本首落。
     */
    @Test
    void scheduledSettleWithPostingDisabledDoesNotPostConfirmedBill() {
        // 未越池月 → confirmed(总池 80000、固定额 5000 ≤ 80000)。
        seedVerified(mcMerchant, 8, 4, 100000L, CONFIRMED_MONTH);
        setGlobalMerchantFixed(mcMerchant, 5000L);

        // 作业开(scheduledEnabled=true)、过账关(postingEnabled=false)。
        SettlementMonthlyJob job = new SettlementMonthlyJob(settlementService, repository, postingService,
                Clock.fixed(CONFIRMED_MONTH.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                        ZoneOffset.UTC),
                true, false);

        job.scheduledSettle();

        // settleMonth 算出 confirmed 单(内部计算),但过账关 → 未落账本:单仍 confirmed、明细仍 draft、无 ar_ap。
        String confirmedBill = "sb_default_" + CONFIRMED_MONTH;
        assertThat(billStatus(confirmedBill)).isEqualTo("confirmed");
        String mcItem = itemId(confirmedBill, mcMerchant);
        assertThat(itemStatus(mcItem)).isEqualTo("draft");
        assertThat(arApCount(mcItem)).isZero();
    }

    // ---- helpers ----

    /** 全局(project_id NULL)商户固定额费率:by_fixed_amount 方式下 settleMonth(null,月) 据此给该商户直分。 */
    private void setGlobalMerchantFixed(String merchantId, long fixedCent) {
        jdbcTemplate.update("""
                INSERT INTO ticket_distribution_merchant_rates (id, project_id, merchant_id,
                    per_guest_cent, fixed_amount_cent, status, is_draft)
                VALUES (?, NULL, ?, NULL, ?, 'active', FALSE)
                """, "tdmr_g_" + merchantId, merchantId, fixedCent);
    }

    /** 直插一条 verification_settled(created_at 落 month 月中=月窗驱动,归商户;confirmed_at 留 NULL 同批1双写)。 */
    private void seedVerified(String merchantId, int verifiedCount, int numPeople, long verifiedCent, YearMonth month) {
        Instant createdAt = month.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(createdAt);
        String id = "st_jobtst_" + System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    settlement_status, entitlement_valid_to, created_at, updated_at)
                VALUES (?, ?, 'verification_settled', ?, 'entry', ?, ?, ?, ?, ?, 0, 'merchant', ?, ?,
                    'draft', ?, ?, ?)
                """,
                id, "TN-" + id, "ent_" + id, verifiedCount,
                Math.max(verifiedCount, 1), numPeople, verifiedCent, verifiedCent,
                merchantId, seedProject, ts, ts, ts);
    }

    private String billStatus(String billId) {
        return jdbcTemplate.queryForObject("SELECT status FROM settlement_bills WHERE id = ?", String.class, billId);
    }

    private String itemId(String billId, String merchantId) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM settlement_items WHERE bill_id = ? AND merchant_id = ?",
                String.class, billId, merchantId);
    }

    private String itemStatus(String itemId) {
        return jdbcTemplate.queryForObject("SELECT status FROM settlement_items WHERE id = ?", String.class, itemId);
    }

    private Map<String, Object> arApBySource(String itemId) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM ar_ap_items WHERE source_type = 'ticket_distribution' AND source_id = ?", itemId);
    }

    private int arApCount(String itemId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ar_ap_items WHERE source_type = 'ticket_distribution' AND source_id = ?",
                Integer.class, itemId);
    }
}
