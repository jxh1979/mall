package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.ticketing.SettlementSettledEvent;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * S2「门票综合服务费:换维 + 覆盖面 + 项目真值」验收(V365)。
 *
 * <p>业务上这一刀回答三件事(口径出自 business-system-map §「门票综合服务费」):
 * <ol>
 *   <li><b>所有门票项目都要收一道</b> —— 原来只有「专属某商户」的核销才抽,通票本身一分不抽;</li>
 *   <li><b>费率按票种谈</b> —— 原来按权益类型(bar/amusement)配,一张通票里两类权益被按两套费率各抽一道,
 *       业务人员在结算单上回答不了「这张票收了多少服务费」;</li>
 *   <li><b>项目级费率能生效</b> —— 原来解析费率时项目维度写死 null,后台配了项目特例也永远命中不了。</li>
 * </ol>
 *
 * <p><b>最核心的一条断言是「逐分不变」</b>:出厂费率仍是 0,所以上面三件事全部落地之后,
 * 结算单每一列、每一条分账明细必须和改造前<b>一分不差</b>。财务对账看到的数字不会因为本刀而动;
 * 真正开始收钱要等费率被人在后台配上(而且进池那部分进主链扣减是 S4,本刀只落事实不动引擎公式)。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementServiceFeeDimensionS2Tests {

    @Autowired private SettlementSettledConsumer settledConsumer;
    @Autowired private TicketDistributionSettlementService settlementService;
    @Autowired private JdbcSettlementBillRepository billRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final YearMonth MONTH = YearMonth.of(2013, 4); // 远离今日的确定月
    private static final Instant AT = MONTH.atDay(12).atTime(10, 0).toInstant(ZoneOffset.UTC);

    private final List<String> projectIds = new ArrayList<>();
    private final List<String> feeConfigIds = new ArrayList<>();
    private final Set<String> ticketTypeIds = new LinkedHashSet<>();
    private final List<String> looseVerificationIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void cleanup() {
        com.complex.platform.support.TestSecurityActors.clear();
        for (String vid : looseVerificationIds) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate,
                    "DELETE FROM settlement_transactions WHERE verification_id = ?", vid);
        }
        for (String pid : projectIds) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate,
                    "DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            jdbcTemplate.update(
                    "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM ticket_distribution_rank_tiers WHERE project_id = ?", pid);
        }
        for (String id : feeConfigIds) {
            jdbcTemplate.update("DELETE FROM comprehensive_service_fee_configs WHERE id = ?", id);
        }
        for (String id : ticketTypeIds) { // 票种在费率规则之后删(fk_csfc_ticket_type)
            jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", id);
        }
        projectIds.clear();
        feeConfigIds.clear();
        ticketTypeIds.clear();
        looseVerificationIds.clear();
    }

    // ─────────────────────────── ① 逐分不变(本刀的地基) ───────────────────────────

    /**
     * <b>K-S2</b>:出厂费率 0 时,「非专属也计提 + 按票种解析 + 项目传真值」全部生效之后,
     * 结算单每一列、每条分账明细与改造前<b>逐分一致</b>。
     *
     * <p>对照组构造法:改造前进池核销压根不过费率解析这条路,落库的 {@code comp_service_fee_cent} 恒为 0。
     * 所以把同样的核销事实按「费恒 0」直接落库,就是改造前那张账单;实验组走真消费者(会解析费率、会给进池的行
     * 也算一遍费)。两边整行 {@code SELECT *} 转 Map 全列比对,只排除账单 id / 项目 id / 时间戳这类天然不同的列。
     */
    @Test
    void everyBillColumnIsPennyIdenticalWhenFeeRateIsStillZero() {
        String after = newProject();   // 实验组:走真消费者(S2 行为)
        String before = newProject();  // 对照组:改造前形态(进池不计提、费恒 0)

        for (String pid : List.of(after, before)) {
            setRankTier(pid, 1, 50, 10000);
        }
        // 三笔进池核销 + 一笔专属核销,两组数据完全一样。
        settleViaConsumer(after, "s2a", "barA", null, 60000L);
        settleViaConsumer(after, "s2b", "barB", null, 40000L);
        settleViaConsumer(after, "s2c", "barC", null, 15000L);
        settleViaConsumer(after, "s2d", "barD", "barD", 25000L);
        seedPreS2Row(before, "s2a", "barA", null, 60000L);
        seedPreS2Row(before, "s2b", "barB", null, 40000L);
        seedPreS2Row(before, "s2c", "barC", null, 15000L);
        seedPreS2Row(before, "s2d", "barD", "barD", 25000L);

        settlementService.settleMonth(after, MONTH);
        settlementService.settleMonth(before, MONTH);

        // 出厂费率 0 → 每一笔(含进池那三笔)实收服务费都是 0。
        assertThat(feeSum(after)).as("费率为 0 时不许凭空抽出一分钱").isZero();

        Map<String, Object> a = new HashMap<>(bill(after));
        Map<String, Object> b = new HashMap<>(bill(before));
        for (String col : List.of("id", "project_id", "created_at", "updated_at")) {
            a.remove(col);
            b.remove(col);
        }
        assertThat(a)
                .as("费率仍为 0 时,结算单每一列必须与改造前逐分一致")
                .isEqualTo(b);
        assertThat(itemAmounts(after))
                .as("各商户分账明细必须逐条逐分一致")
                .isEqualTo(itemAmounts(before));
        assertThat(itemAmounts(after)).isNotEmpty();
    }

    // ─────────────────────────── ② 覆盖面:所有门票都收 ───────────────────────────

    /**
     * 覆盖面:配上费率后,<b>非专属(进池)</b>的核销也开始计提综合服务费 —— 这正是
     * 「所有门票项目都要收一道综合服务费」;改造前这类核销一分不抽。
     *
     * <p>同时守住本刀的边界:进池那部分只落成事实,<b>还不进引擎</b>(账单上的综合服务费仍只统计专属那条线,
     * 进主链是 S4),所以账单数字此刻不动。
     */
    @Test
    void pooledVerificationNowAccruesFeeButEngineStillUntouched() {
        String pid = newProject();
        setRankTier(pid, 1, 50, 10000);
        seedFee(pid, null, null, 1000); // 该项目全票种 10%

        settleViaConsumer(pid, "cov1", "barA", null, 100000L);   // 进池
        settleViaConsumer(pid, "cov2", "barB", "barB", 50000L);  // 专属

        assertThat(feeOf("cov1")).as("进池核销也要计提(所有门票都收一道)").isEqualTo(10000L);
        assertThat(feeOf("cov2")).isEqualTo(5000L);

        settlementService.settleMonth(pid, MONTH);
        assertThat(num(bill(pid), "comp_service_fee_cent"))
                .as("本刀不动引擎数值公式:账单上的综合服务费仍只含专属那条线(进池部分进主链是 S4)")
                .isEqualTo(5000L);
    }

    // ─────────────────────────── ③ 换维:按票种,不是按权益类型 ───────────────────────────

    /** 费率配在票种 A 上 → 票种 A 的核销抽、票种 B 的核销不抽(即便权益类型相同、商户相同)。 */
    @Test
    void feeRateIsScopedByTicketTypeNotEntitlementType() {
        String pid = newProject();
        String ttA = ticketType("a");
        String ttB = ticketType("b");
        seedFee(pid, null, ttA, 2000); // 只对票种 A 收 20%

        settleViaConsumer(pid, "dimA", "barA", null, 10000L, ttA, "bar");
        settleViaConsumer(pid, "dimB", "barA", null, 10000L, ttB, "bar"); // 同权益类型、同商户,换个票种

        assertThat(feeOf("dimA")).isEqualTo(2000L);
        assertThat(feeOf("dimB")).as("没给这个票种配费率就不许收").isZero();
    }

    // ─────────────────────────── ④ 项目维度传真值 ───────────────────────────

    /**
     * 项目级费率特例真的能命中(改造前解析时项目维度写死 null,后台配了也永远不生效),
     * 且这笔核销落库的项目归属 = 解析费率时用的那一个,不再各写各的。
     */
    @Test
    void projectScopedRateIsHitAndLandsOnTheLedgerRow() {
        String pid = newProject();
        seedFee(pid, null, null, 3000); // 只对该项目配 30%

        settleViaConsumer(pid, "prj1", "barA", null, 10000L);
        assertThat(feeOf("prj1")).as("项目级费率特例必须命中").isEqualTo(3000L);
        assertThat(projectIdOf("prj1"))
                .as("落库的项目归属必须与解析费率用的是同一个值")
                .isEqualTo(pid);

        // 平台口径(无项目 scope)的同一笔核销不吃这条项目特例,回落全局默认 0。
        settleLoose("prj2", "barA", null, 10000L);
        assertThat(feeOf("prj2")).isZero();
        assertThat(projectIdOf("prj2")).isNull();
    }

    // ─────────────────────────── ⑤ 进池那部分的月度封顶也要管住 ───────────────────────────

    /**
     * 覆盖面扩大后进池核销也计提,其「专属商户」为空;封顶的月累计若只按专属商户配对,
     * 进池那部分就永远累计不到、封顶形同虚设(月内可无限收)。此处证明进池同样被封顶管住。
     */
    @Test
    void monthlyCapAlsoAppliesToPooledAccrual() {
        String pid = newProject();
        seedFeeWithCap(pid, null, null, 1000, 1200L); // 10%,当月封顶 1200

        settleViaConsumer(pid, "cap1", "barA", null, 10000L); // 1000
        settleViaConsumer(pid, "cap2", "barB", null, 10000L); // 剩余额度 200
        settleViaConsumer(pid, "cap3", "barC", null, 10000L); // 已触顶 → 0

        assertThat(feeOf("cap1")).isEqualTo(1000L);
        assertThat(feeOf("cap2")).isEqualTo(200L);
        assertThat(feeOf("cap3")).isZero();
        assertThat(feeOf("cap1") + feeOf("cap2") + feeOf("cap3")).isEqualTo(1200L);
    }

    // ─────────────────────────── helpers ───────────────────────────

    private String newProject() {
        String pid = "proj_s2_" + System.nanoTime();
        projectIds.add(pid);
        return pid;
    }

    /** 惰性建票种(V365 的 fk_csfc_ticket_type 要求费率规则引用真实票种)。 */
    private String ticketType(String label) {
        String id = "tt_s2_" + label + "_" + (System.nanoTime() % 1_000_000L);
        ticketTypeIds.add(id);
        jdbcTemplate.update(
                "INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status,"
                        + " created_at, updated_at) VALUES (?, ?, 'fixed_price', FALSE, 'active', now(), now())"
                        + " ON CONFLICT (id) DO NOTHING",
                id, "S2测试票种-" + label);
        return id;
    }

    private void seedFee(String projectId, String merchantId, String ticketTypeId, int feeBps) {
        insertFee(projectId, merchantId, ticketTypeId, feeBps, null);
    }

    private void seedFeeWithCap(String projectId, String merchantId, String ticketTypeId, int feeBps, long capCent) {
        insertFee(projectId, merchantId, ticketTypeId, feeBps, capCent);
    }

    private void insertFee(String projectId, String merchantId, String ticketTypeId, int feeBps, Long capCent) {
        String id = "csfc_s2_" + Long.toHexString(System.nanoTime());
        feeConfigIds.add(id);
        jdbcTemplate.update(
                "INSERT INTO comprehensive_service_fee_configs (id, project_id, merchant_id, ticket_type_id,"
                        + " fee_bps, monthly_cap_amount_cent, status, is_draft)"
                        + " VALUES (?, ?, ?, ?, ?, ?, 'active', FALSE)",
                id, projectId, merchantId, ticketTypeId, feeBps, capCent);
    }

    /** 走真消费者(生产路径)落一笔核销结算事实。 */
    private void settleViaConsumer(String projectId, String tag, String merchantId,
                                   String exclusiveMerchantId, long allocatedCent) {
        settleViaConsumer(projectId, tag, merchantId, exclusiveMerchantId, allocatedCent, "tt_s2_default", "entry");
    }

    private void settleViaConsumer(String projectId, String tag, String merchantId, String exclusiveMerchantId,
                                   long allocatedCent, String ticketTypeId, String entitlementType) {
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                tag, AT, "ent_" + tag, "ord_" + tag, ticketTypeId, entitlementType, merchantId,
                1, 1, 1, allocatedCent, AT.plusSeconds(86400), exclusiveMerchantId, null, null, projectId));
    }

    /** 平台口径(无项目 scope)的一笔核销;不落任何项目,清场按 verification_id 走。 */
    private void settleLoose(String tag, String merchantId, String exclusiveMerchantId, long allocatedCent) {
        looseVerificationIds.add(tag);
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                tag, AT, "ent_" + tag, "ord_" + tag, "tt_s2_default", "entry", merchantId,
                1, 1, 1, allocatedCent, AT.plusSeconds(86400), exclusiveMerchantId, null, null, null));
    }

    /**
     * 改造前的核销结算事实形态:进池核销压根不过费率解析,{@code comp_service_fee_cent} 恒 0。
     * 专属核销在费率 0 时算出来也是 0,故两类都按费 0 落库 = 改造前那张账单的输入。
     */
    private void seedPreS2Row(String projectId, String tag, String merchantId,
                              String exclusiveMerchantId, long allocatedCent) {
        java.sql.Timestamp ts = java.sql.Timestamp.from(AT);
        String id = "st_s2pre_" + tag + "_" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,"
                        + " order_id, ticket_type_id, entitlement_type, verified_count, total_times, num_people,"
                        + " allocated_revenue_cent, verified_amount_cent, unverified_amount_cent, beneficiary_type,"
                        + " merchant_id, project_id, exclusive_merchant_id, verification_id, settlement_status,"
                        + " entitlement_valid_to, business_at, discount_amount_cent, comp_service_fee_cent,"
                        + " created_at, updated_at)"
                        + " VALUES (?, ?, 'verification_settled', ?, ?, 'tt_s2_default', 'entry', 1, 1, 1,"
                        + " ?, ?, 0, 'merchant', ?, ?, ?, ?, 'draft', ?, ?, 0, 0, ?, ?)",
                id, "TN-" + id, "ent_" + tag, "ord_" + tag,
                allocatedCent, allocatedCent, merchantId, projectId, exclusiveMerchantId, "vf_" + id,
                java.sql.Timestamp.from(AT.plusSeconds(86400)), ts, ts, ts);
    }

    private void setRankTier(String projectId, int rankFrom, int rankTo, int shareBps) {
        jdbcTemplate.update(
                "INSERT INTO ticket_distribution_rank_tiers (id, project_id, rank_from, rank_to, share_bps,"
                        + " status, is_draft) VALUES (?, ?, ?, ?, ?, 'active', FALSE)",
                "trt_s2_" + Long.toHexString(System.nanoTime()), projectId, rankFrom, rankTo, shareBps);
    }

    private long feeOf(String verificationId) {
        return jdbcTemplate.queryForObject(
                "SELECT comp_service_fee_cent FROM settlement_transactions WHERE transaction_no = ?",
                Long.class, "ST-VS-" + verificationId);
    }

    private String projectIdOf(String verificationId) {
        return jdbcTemplate.queryForObject(
                "SELECT project_id FROM settlement_transactions WHERE transaction_no = ?",
                String.class, "ST-VS-" + verificationId);
    }

    private long feeSum(String projectId) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(comp_service_fee_cent), 0) FROM settlement_transactions WHERE project_id = ?",
                Long.class, projectId);
    }

    private Map<String, Object> bill(String projectId) {
        return jdbcTemplate.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ?", projectId);
    }

    private Map<String, Long> itemAmounts(String projectId) {
        Map<String, Long> out = new HashMap<>();
        jdbcTemplate.query(
                "SELECT si.merchant_id, si.distribution_method, si.calculated_amount_cent, si.final_amount_cent"
                        + " FROM settlement_items si JOIN settlement_bills sb ON sb.id = si.bill_id"
                        + " WHERE sb.project_id = ?",
                rs -> {
                    out.put(rs.getString("merchant_id") + "|" + rs.getString("distribution_method") + "|calc",
                            rs.getLong("calculated_amount_cent"));
                    out.put(rs.getString("merchant_id") + "|" + rs.getString("distribution_method") + "|final",
                            rs.getLong("final_amount_cent"));
                }, projectId);
        return out;
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
