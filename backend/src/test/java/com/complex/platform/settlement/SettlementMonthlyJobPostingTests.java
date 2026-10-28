package com.complex.platform.settlement;

// 文件版本：V0.05

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
 *   <li><b>超池月 → 同样 confirmed → 自动过账</b>(第 75 条 ENUM L52「结算环节不再设“超池待审批”」「超出部分由娱乐平台公司承担」):
 *       池内额过 ticket_distribution、超出额过 platform_incentive_subsidy;</li>
 *   <li><b>重跑幂等</b>:已 settled 月被 job 状态预检跳过(不重结/不双过),ar_ap 不翻倍。</li>
 * </ul>
 *
 * <p>作业口径固定用 {@code settleMonth(null, month)}=平台默认口径,读<b>全局</b>分配配置。
 * S8 两段化后不再需要切全局方式:给唯一商户配<b>全局第一段固定额规则</b>(V374 rule_type=fixed)
 * 即可造「未越池(固定额 ≤ 池)/超池(固定额 &gt;&gt; 池)」两月;第二段无参与者的余额显式归娱乐平台公司
 * (bill.pool_undistributed_cent 留痕),不影响过账断言。
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

    // 未越池月 = 作业 lastMonth;超池月 = 其上一月;两月均落在默认 6 月回看窗口 [2009-03,2009-08] 内。
    private static final YearMonth CONFIRMED_MONTH = YearMonth.of(2009, 8);
    private static final YearMonth DRAFT_MONTH = YearMonth.of(2009, 7);

    // 唯一商户 + 唯一造数项目(便于清理;null 口径聚合忽略 project_id,仅用于清理定位)。
    private final String suffix = Long.toHexString(System.nanoTime() & 0xFFFFFL);
    private final String mcMerchant = "jc" + suffix; // 未越池:固定额 < 总池 → confirmed
    private final String mdMerchant = "jd" + suffix; // 超池:固定额 >> 总池 → 照常 confirmed,超出部分娱乐平台公司承担
    private final String seedProject = "jobtst_" + suffix;

    @BeforeEach
    void setUp() {
        // FS-021：过账到期日按财务指定付账日推算，缺规则/日历即拒绝——种已批钱路规则与工作日历。
        com.complex.platform.support.InternalPayDayFixtures.seed(jdbcTemplate);
        // 🔴 P1(2026-07-21):【故意不再注入登录主体】——生产调度线程(SettlementMonthlyJob.scheduledSettle)
        //   跑在无主体上下文,过账走 FinanceService.createArApItem(@GuardedAction)靠 currentActor() 必抛 UNAUTHENTICATED。
        //   旧测试在此 setUp 注入 asPlatformAdmin() 掩盖了该生产缺陷(测试绿、生产必挂)。现去掉注入=让测试真实复现调度线程无主体,
        //   由作业【自身装配系统主体 SCHEDULER_ACTOR】(照 FacilityInspectionScheduleJob 范式)使过账在生产真能落账。
        // 第十八批第 2 条:第一段名单与金额随票固化在池快照里——pooledFirstSegment 造一张只覆盖该商户、第一段固定额的池快照票,
        //   即可造「未越池/超池」两态(第二段无参与者,余额显式归平台)。
    }

    @AfterEach
    void cleanup() {
        // 窗口内 null 口径结算单 id 恒为 sb_default_2009-XX(仅本用例产出),先清明细再清单(逐份结果随单删除)。
        jdbcTemplate.update("DELETE FROM settlement_items WHERE bill_id LIKE 'sb_default_2009-%'");
        jdbcTemplate.update("DELETE FROM settlement_bills WHERE id LIKE 'sb_default_2009-%'");
        com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, "DELETE FROM settlement_transactions WHERE project_id = ?", seedProject);
        com.complex.platform.support.PassPoolFixtures.deleteByProject(jdbcTemplate, seedProject);
        // ar_ap_items 为不可变台账(DB 触发器禁 DELETE),source_id=唯一明细 id 不与后续用例冲突,不清。
        TestSecurityActors.clear();
    }

    @Test
    void scheduledSettleAutoPostsConfirmedBillIncludingOverPool() {
        // 未越池月:池 80000;第一段固定额 5000 ≤ 80000 → confirmed。
        seedVerified(mcMerchant, 8, 4, 100000L, CONFIRMED_MONTH, pooledFirstSegment(mcMerchant, 80000L, 5000L));
        // 超池月:池 800;第一段固定额 500000 >> 800 → 同样 confirmed(结算环节不设超池待审批)。
        seedVerified(mdMerchant, 3, 2, 1000L, DRAFT_MONTH, pooledFirstSegment(mdMerchant, 800L, 500000L));

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

        // ---- ② 超池月同样自动过账:池内 800 过 ticket_distribution,超出 499200 过 platform_incentive_subsidy(娱乐平台公司承担)----
        String overPoolBill = "sb_default_" + DRAFT_MONTH; // sb_default_2009-07
        assertThat(billStatus(overPoolBill)).isEqualTo("settled");
        String mdItem = itemId(overPoolBill, mdMerchant);
        assertThat(itemStatus(mdItem)).isEqualTo("posted");
        assertThat(amountBySource(mdItem, "ticket_distribution")).isEqualTo(800L);
        assertThat(amountBySource(mdItem, "platform_incentive_subsidy")).isEqualTo(499_200L);

        // ---- ③ 重跑幂等:已 settled 月被状态预检跳过(不双过),ar_ap 不翻倍 ----
        job.scheduledSettle();
        assertThat(arApCount(mcItem)).isEqualTo(1);
        assertThat(billStatus(confirmedBill)).isEqualTo("settled");
        assertThat(amountBySource(mdItem, "platform_incentive_subsidy")).isEqualTo(499_200L);
    }

    /**
     * 🔴 过账独立闸(main follow-up):postingEnabled=false 时,settleMonth 照常算单→confirmed(内部计算),
     * 但 postBill <b>不执行</b>——单保持 confirmed、明细 draft、无 ar_ap。防"月度作业开关 prod=true 连带自动过账"激活账本首落。
     */
    // 复核 money I7：过账闸关闭时，月结作业不得补跑演出结算（演出结算出账即挂应付，属真过账）。
    @Test
    void scheduledSettleWithPostingDisabledSkipsMediaPerformanceSettlement() {
        MediaPerformanceSettlementService media = org.mockito.Mockito.mock(MediaPerformanceSettlementService.class);
        SettlementMonthlyJob job = new SettlementMonthlyJob(
                org.mockito.Mockito.mock(TicketDistributionSettlementService.class),
                org.mockito.Mockito.mock(JdbcSettlementBillRepository.class),
                org.mockito.Mockito.mock(SettlementPostingService.class),
                Clock.fixed(Instant.parse("2009-09-15T04:00:00Z"), ZoneOffset.UTC), true, false);
        job.mediaSettlement = media;
        job.scheduledSettle();
        org.mockito.Mockito.verify(media, org.mockito.Mockito.never()).settleConfirmedForMonth(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void scheduledSettleWithPostingDisabledDoesNotPostConfirmedBill() {
        // 未越池月 → confirmed(池 80000、第一段固定额 5000 ≤ 80000)。
        seedVerified(mcMerchant, 8, 4, 100000L, CONFIRMED_MONTH, pooledFirstSegment(mcMerchant, 80000L, 5000L));

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

    /** 一张只覆盖该商户的池快照票(池 poolCent),快照第一段名单里该商户按固定额;settleMonth(null,月) 据此给该商户直分。返回父票。 */
    private String pooledFirstSegment(String merchantId, long poolCent, long fixedCent) {
        String ticket = "tk_job_" + merchantId;
        com.complex.platform.support.PassPoolFixtures.snapshot(jdbcTemplate, ticket, seedProject, poolCent,
                com.complex.platform.support.PassPoolFixtures.FACTORY_SCHEME, java.util.List.of(merchantId),
                Map.of(merchantId, fixedCent));
        return ticket;
    }

    /** 直插一条 verification_settled(created_at 落 month 月中=月窗驱动,归商户;confirmed_at 留 NULL 同批1双写)。 */
    private void seedVerified(String merchantId, int verifiedCount, int numPeople, long verifiedCent, YearMonth month,
                              String parentTicket) {
        Instant createdAt = month.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(createdAt);
        String id = "st_jobtst_" + System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    verification_id, settlement_status, entitlement_valid_to, created_at, updated_at, parent_ticket_id)
                VALUES (?, ?, 'verification_settled', ?, 'entry', ?, ?, ?, ?, ?, 0, 'merchant', ?, ?, ?,
                    'draft', ?, ?, ?, ?)
                """,
                id, "TN-" + id, "ent_" + id, verifiedCount,
                Math.max(verifiedCount, 1), numPeople, verifiedCent, verifiedCent,
                merchantId, seedProject, "vf_" + id, ts, ts, ts, parentTicket);
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

    private long amountBySource(String itemId, String sourceType) {
        return jdbcTemplate.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE source_type = ? AND source_id = ?",
                Long.class, sourceType, itemId);
    }
}
