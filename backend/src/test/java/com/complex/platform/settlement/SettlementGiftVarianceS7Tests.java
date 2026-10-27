package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.ticketing.SettlementSettledEvent;
import com.complex.platform.ticketing.VerificationReversedEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * S7「赠付差额调账留痕 + 兑付成本按业态分记」验收(V377)。
 *
 * <p>业务口径(business-system-map :962):「对商户的兑付由娱乐平台公司承担,不进入酒吧可分配池,
 * 也不计入酒吧的销售提成;<b>成本按提供方的商户业态(酒吧/餐厅/零售)分开记账</b>;
 * <b>预扣与核销实付的差额留在娱乐平台公司,按可配规则调账留痕</b>。」;ENUM L52「该笔预扣归娱乐平台公司,
 * 预扣数与日后实付数的差额也留在娱乐平台公司」;18B 复核后裁决第 11 条「对，下线两个选项（推荐）」——处理规则只剩
 * platform_absorb。本类八组断言:
 *
 * <ol>
 *   <li><b>逐分不变基线</b>:差额规则未触发/配置为零 → 不落任何台账行,结算单整行 SELECT * 全列
 *       与 S6 K9 同一组手算数逐分一致;</li>
 *   <li><b>platform_absorb</b>:差额留娱乐平台公司仅留痕——台账一行(预扣/实付/差额/规则/主体快照),
 *       可分配池不动;<b>K12</b> Σ差额 = ΣG − ΣA 逐分;</li>
 *   <li><b>预扣关零仍有兑付</b>:差额为负(平台没预扣却真付了钱),归到解析的规则行,财务看得见;</li>
 *   <li><b>下线的两个规则写不进库</b>:规则表与差额台账的处理规则词表只剩 platform_absorb;</li>
 *   <li><b>冲正取反净零</b>:核销+冲正、挂账+对冲同月配对 → G/A 双净零 → 不落行;</li>
 *   <li><b>幂等重算冲正取反</b>:结论有变时追加「旧净额全额取反」冲正行 + 新陈述行,历史不灭、
 *       净额=新真相;结论没变重跑零膨胀;</li>
 *   <li><b>K14 按业态分记守恒</b>:Σ各业态份额行 gift_actual_paid_cent ≡ 台账实付合计,
 *       兑付只有实付、没有核销收入的业态也有自己的行;</li>
 *   <li><b>append-only 硬强制</b>:台账 UPDATE/DELETE 被 DB 触发器当场拒绝。</li>
 * </ol>
 *
 * <p>非 @Transactional(settleMonth/消费者内部真提交);@AfterEach 按项目/来源清理(旁路触发器)。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementGiftVarianceS7Tests {

    private static final YearMonth MONTH = YearMonth.of(2015, 3); // 远离今日/其它测试类的确定月
    private static final Instant AT = MONTH.atDay(10).atTime(20, 0).toInstant(ZoneOffset.UTC);

    @Autowired private SettlementSettledConsumer settledConsumer;
    @Autowired private SettlementReversedConsumer reversedConsumer;
    @Autowired private TicketDistributionSettlementService settlementService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<String> projectIds = new ArrayList<>();
    private final List<String> merchants = new ArrayList<>();
    private final List<String> giftConfigIds = new ArrayList<>();
    private final List<String> arApSourceIds = new ArrayList<>();
    private final List<String> merchantRows = new ArrayList<>();

    @BeforeEach
    void setUp() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void cleanup() {
        com.complex.platform.support.TestSecurityActors.clear();
        for (String sid : arApSourceIds) {
            com.complex.platform.support.AppendOnlyTestBypass.updateOnTable(jdbcTemplate,
                    "ar_ap_items", "trg_append_only_d",
                    "DELETE FROM ar_ap_items WHERE source_type = 'pass_drink_settlement' AND source_id = ?", sid);
        }
        for (String m : merchants) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate,
                    "DELETE FROM settlement_transactions WHERE merchant_id = ?", m);
        }
        for (String pid : projectIds) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate,
                    "DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            com.complex.platform.support.AppendOnlyTestBypass.updateOnTable(jdbcTemplate,
                    "pass_gift_cost_variance_ledger", "trg_append_only_ud",
                    "DELETE FROM pass_gift_cost_variance_ledger WHERE project_id = ?", pid);
            jdbcTemplate.update(
                    "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update(
                    "DELETE FROM settlement_bill_format_shares WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM ticket_distribution_rank_tiers WHERE project_id = ?", pid);
        }
        for (String id : giftConfigIds) {
            jdbcTemplate.update("DELETE FROM pass_gift_cost_configs WHERE id = ?", id);
        }
        for (String id : merchantRows) {
            jdbcTemplate.update("DELETE FROM merchants WHERE id = ?", id);
        }
        projectIds.clear();
        merchants.clear();
        giftConfigIds.clear();
        arApSourceIds.clear();
        merchantRows.clear();
    }

    // ───────────────── ① 逐分不变基线:规则未触发/配置为零 → 零台账行,结算单全列不变 ─────────────────

    /**
     * 不配任何项目级 G 规则(命中 V373 种子 by_ticket_amount+0)、无任何兑付挂账:
     * 复算 S6 K9 同一组数(4 家酒吧、收入 150000、费 2000bps)——结算单<b>每一列</b>逐分一致,
     * 差额台账<b>零行</b>,业态份额行兑付实付列恒 0。
     */
    @Test
    void baseline_zeroConfigNoPayout_noLedgerRowsEveryBillColumnUnchanged() {
        String pid = newProject();
        seedVerified(pid, "bar1", 20, 8, 60000L, 12000L);
        seedVerified(pid, "bar2", 15, 6, 40000L, 8000L);
        seedVerified(pid, "bar3", 10, 4, 30000L, 6000L);
        seedVerified(pid, "bar4", 5, 2, 20000L, 4000L);

        String billId = settlementService.settleMonth(pid, MONTH);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("id", billId);
        expected.put("project_id", pid);
        expected.put("period_start", java.sql.Date.valueOf(MONTH.atDay(1)));
        expected.put("period_end", java.sql.Date.valueOf(MONTH.atEndOfMonth()));
        expected.put("ticket_revenue_cent", 150000L);
        expected.put("channel_fee_cent", 0L);
        expected.put("route_fee_cent", 0L);           // 第十八批第 3 条 V555 通路费列（无付款固化行 → 0）
        expected.put("comp_service_fee_pooled_cent", 30000L);
        expected.put("available_pool_cent", 120000L);   // 无池快照:酒吧净份额按 ENUM L52 末句整笔单列未分配池余额
        expected.put("distributed_cent", 0L);
        expected.put("comp_service_fee_exclusive_cent", 0L);
        expected.put("fee_beneficiary_entity_id", "be_ent_platform");
        expected.put("gift_cost_cent", 0L);
        expected.put("gift_beneficiary_entity_id", "be_ent_platform");
        expected.put("distribution_method", "by_rank_tier");
        expected.put("special_approval_ref", null);
        expected.put("special_approval_valid_until", null);
        expected.put("status", "confirmed");
        expected.put("confirmed_at", null);
        expected.put("settled_at", null);
        expected.put("attributed_entity_id", "be_ent_platform");
        expected.put("combo_discount_cent", 0L);
        expected.put("rounding_residual_cent", 0L);
        expected.put("rounding_residual_entity_id", null);
        expected.put("pool_undistributed_cent", 120000L);   // 只放无第二段参与者的余额(18B 复核后裁决第 11 条)
        expected.put("pool_undistributed_entity_id", "be_ent_platform");
        expected.put("unverified_deduction_cent", 0L);      // V556 三栏分列与暂挂、超池列
        expected.put("unverified_deduction_entity_id", null);
        expected.put("pool_suspended_cent", 0L);
        expected.put("pool_overflow_cent", 0L);

        Map<String, Object> actual = new HashMap<>(bill(pid));
        actual.remove("created_at");
        actual.remove("updated_at");
        assertThat(actual)
                .as("基线:差额规则未触发时结算单每一列与 S6 K9 逐分一致")
                .isEqualTo(expected);
        assertThat(ledgerRows(billId)).as("无预扣无兑付无结转 → 差额台账零行").isEmpty();
        assertThat(((Number) formatShare(billId, "bar").get("gift_actual_paid_cent")).longValue())
                .as("业态份额行兑付实付列恒 0").isZero();
    }

    // ───────────────── ② platform_absorb:留痕不动池 + K12 ─────────────────

    /** G 5900、A 4000 → 台账一行(5900/4000/1900/platform_absorb/主体快照);池 = R−C−G−F 不含差额。 */
    @Test
    void platformAbsorb_recordsVarianceRowAndLeavesPoolUntouched() {
        String pid = newProject();
        String merchant = merchant();
        String cfg = seedGiftConfig(pid, "by_ticket_amount", 5900L, "platform_absorb");
        setRankTier(pid, 1, 9, 10000);
        String ticket = "tkt_s7ab_" + Long.toHexString(System.nanoTime());
        String v1 = "v7ab1_" + ticket;
        String v2 = "v7ab2_" + ticket;

        verifyPooled(pid, merchant, ticket, v1, 1, AT);
        verifyPooled(pid, merchant, ticket, v2, 2, AT); // 同票第 2 笔:不再计提 G,承载兑付挂账
        seedPayable(v2, merchant, 4000L);

        String billId = settlementService.settleMonth(pid, MONTH);

        List<Map<String, Object>> rows = ledgerRows(billId);
        assertThat(rows).hasSize(1);
        Map<String, Object> row = rows.get(0);
        assertThat(row.get("config_id")).isEqualTo(cfg);
        assertThat(num(row, "accrued_cost_cent")).as("预扣 G").isEqualTo(5900L);
        assertThat(num(row, "actual_paid_cent")).as("实付 A").isEqualTo(4000L);
        assertThat(num(row, "variance_cent")).as("差额 V = G − A").isEqualTo(1900L);
        assertThat(row.get("variance_handling")).isEqualTo("platform_absorb");
        assertThat(row.get("beneficiary_entity_id")).as("差额留娱乐平台公司(主体快照)").isEqualTo("be_ent_platform");

        // K12:Σ台账差额 = Σ预扣 − Σ实付,逐分(行内恒等式另有 DB CHECK chk_pgcv_identity 兜底)。
        assertThat(ledgerSum(billId, "variance_cent"))
                .isEqualTo(ledgerSum(billId, "accrued_cost_cent") - ledgerSum(billId, "actual_paid_cent"));

        // absorb 不动份额:酒吧净份额 = R(2×10000) − G 5900 − F(2×2000) = 10100,差额 1900 不回补、不进未分配池余额;
        //   夹具无池快照,未分配池余额只是该净份额本身(ENUM L52 末句)。
        assertThat(((Number) formatShare(billId, "bar").get("net_revenue_cent")).longValue()).isEqualTo(10100L);
        assertThat(num(bill(pid), "pool_undistributed_cent")).isEqualTo(10100L);
    }

    // ───────────────── ③ 预扣关零仍有兑付:差额为负照样留痕 ─────────────────

    /** 命中全局种子(整额 0,无计提行):兑付 800 → 台账行 accrued 0/paid 800/variance −800,归种子规则。 */
    @Test
    void zeroPredeductWithRealPayout_negativeVarianceStillRecorded() {
        String pid = newProject();
        String merchant = merchant();
        String ticket = "tkt_s7ng_" + Long.toHexString(System.nanoTime());
        String v1 = "v7ng1_" + ticket;

        verifyPooled(pid, merchant, ticket, v1, 1, AT);
        seedPayable(v1, merchant, 800L);

        String billId = settlementService.settleMonth(pid, MONTH);

        List<Map<String, Object>> rows = ledgerRows(billId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("config_id")).as("归到解析的规则(全局种子)").isEqualTo("pgc_default");
        assertThat(num(rows.get(0), "accrued_cost_cent")).isZero();
        assertThat(num(rows.get(0), "actual_paid_cent")).isEqualTo(800L);
        assertThat(num(rows.get(0), "variance_cent")).as("平台没预扣却真付了钱 → 差额为负").isEqualTo(-800L);
    }

    // ───────────────── ④ 结转下期 / 回补当期池已下线:写不进库 ─────────────────

    /**
     * 18B 复核后裁决第 11 条「对，下线两个选项（推荐）」(ENUM L52「预扣数与日后实付数的差额也留在娱乐平台公司」):
     * 规则表 chk_pgc_variance 与差额台账 chk_pgcv_handling 收窄为 platform_absorb,carry_forward / true_up_pool 一律拒收。
     */
    @Test
    void offlineVarianceRulesAreRejectedByBothTables() {
        String pid = newProject();
        for (String offline : List.of("carry_forward", "true_up_pool")) {
            assertThatThrownBy(() -> seedGiftConfig(pid, "by_ticket_amount", 5900L, offline))
                    .as("规则表不收 " + offline).hasMessageContaining("chk_pgc_variance");
            assertThatThrownBy(() -> jdbcTemplate.update("""
                    INSERT INTO pass_gift_cost_variance_ledger (id, bill_id, project_id, period_start, period_end, config_id,
                        variance_handling, accrued_cost_cent, actual_paid_cent, variance_cent)
                    VALUES (?, 'sb_s7_offline', ?, ?, ?, 'pgc_default', ?, 0, 0, 0)""",
                    "pgv_s7_off_" + offline + "_" + Long.toHexString(System.nanoTime()), pid,
                    java.sql.Date.valueOf(MONTH.atDay(1)), java.sql.Date.valueOf(MONTH.atEndOfMonth()), offline))
                    .as("差额台账不收 " + offline).hasMessageContaining("chk_pgcv_handling");
        }
    }

    // ───────────────── ⑤ 冲正取反净零:核销+冲正、挂账+对冲同月配对 → 不落行 ─────────────────

    /** 核销计提 G 5900 + 挂应付 4000;随后冲正核销(G 取反)+ 对冲应收 4000 → 双净零,台账零行。 */
    @Test
    void reversalPairsNetToZero_noLedgerRows() {
        String pid = newProject();
        String merchant = merchant();
        seedGiftConfig(pid, "by_ticket_amount", 5900L, "platform_absorb");
        setRankTier(pid, 1, 9, 10000);
        String ticket = "tkt_s7rv_" + Long.toHexString(System.nanoTime());
        String v1 = "v7rv1_" + ticket;

        verifyPooled(pid, merchant, ticket, v1, 1, AT);
        seedPayable(v1, merchant, 4000L);
        reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                v1, "rev_" + v1, "ent_" + ticket, "ord_x", merchant, 1));
        seedReceivableOffset(v1, merchant, 4000L);

        String billId = settlementService.settleMonth(pid, MONTH);

        assertThat(ledgerRows(billId)).as("G 净 0 且 A 净 0 → 不出行(零膨胀)").isEmpty();
    }

    // ───────────────── ⑥ 幂等重算:冲正取反 + 重述,历史不灭 ─────────────────

    /** 首结 1 行;补一笔兑付后重结 → 追加冲正行(全额取反,回指被冲行)+ 新陈述行,净额=新真相;再重跑零追加。 */
    @Test
    void rerunAppendsReversalAndRestatement_historyPreserved() {
        String pid = newProject();
        String merchant = merchant();
        seedGiftConfig(pid, "by_ticket_amount", 5900L, "platform_absorb");
        setRankTier(pid, 1, 9, 10000);
        String ticket = "tkt_s7rr_" + Long.toHexString(System.nanoTime());
        String v1 = "v7rr1_" + ticket;
        String v2 = "v7rr2_" + ticket;

        verifyPooled(pid, merchant, ticket, v1, 1, AT);
        seedPayable(v1, merchant, 4000L);
        String billId = settlementService.settleMonth(pid, MONTH);
        assertThat(ledgerRows(billId)).hasSize(1);
        String statement1 = (String) ledgerRows(billId).get(0).get("id");

        verifyPooled(pid, merchant, ticket, v2, 2, AT);
        seedPayable(v2, merchant, 700L);
        settlementService.settleMonth(pid, MONTH);

        List<Map<String, Object>> rows = ledgerRows(billId);
        assertThat(rows).as("陈述 + 冲正 + 重述 = 3 行,历史结论不灭").hasSize(3);
        Map<String, Object> reversal = rows.stream()
                .filter(r -> r.get("reversal_of") != null).findFirst().orElseThrow();
        assertThat(reversal.get("reversal_of")).as("冲正行回指被冲陈述行").isEqualTo(statement1);
        assertThat(num(reversal, "accrued_cost_cent")).isEqualTo(-5900L);
        assertThat(num(reversal, "actual_paid_cent")).isEqualTo(-4000L);
        assertThat(num(reversal, "variance_cent")).as("冲正行仍满足 V = G − A").isEqualTo(-1900L);
        assertThat(ledgerSum(billId, "accrued_cost_cent")).as("净额 = 新真相 G").isEqualTo(5900L);
        assertThat(ledgerSum(billId, "actual_paid_cent")).as("净额 = 新真相 A").isEqualTo(4700L);
        assertThat(ledgerSum(billId, "variance_cent")).isEqualTo(1200L);

        settlementService.settleMonth(pid, MONTH);
        assertThat(ledgerRows(billId)).as("结论没变重跑零追加").hasSize(3);
    }

    // ───────────────── ⑦ K14:兑付成本按提供方业态分记,不重不漏 ─────────────────

    /** 兑付给酒吧商户 800、餐饮商户 1200:各业态份额行各记各的,Σ各业态实付 ≡ 台账实付合计 2000。 */
    @Test
    void k14_actualPaidSplitsByProviderFormatAndSumsToLedger() {
        String pid = newProject();
        String barMerchant = seedMerchantRow("bar");
        String cateringMerchant = seedMerchantRow("catering");
        merchants.add(barMerchant);
        setRankTier(pid, 1, 9, 10000);
        String t1 = "tkt_s7fm1_" + Long.toHexString(System.nanoTime());
        String t2 = "tkt_s7fm2_" + Long.toHexString(System.nanoTime());
        String v1 = "v7fm1_" + t1;
        String v2 = "v7fm2_" + t2;

        verifyPooled(pid, barMerchant, t1, v1, 1, AT);
        verifyPooled(pid, barMerchant, t2, v2, 1, AT);
        seedPayable(v1, barMerchant, 800L);
        seedPayable(v2, cateringMerchant, 1200L);

        String billId = settlementService.settleMonth(pid, MONTH);

        assertThat(((Number) formatShare(billId, "bar").get("gift_actual_paid_cent")).longValue())
                .as("酒吧业态实付").isEqualTo(800L);
        Map<String, Object> catering = formatShare(billId, "catering");
        assertThat(((Number) catering.get("gift_actual_paid_cent")).longValue())
                .as("餐饮业态实付(本月无进池收入也有自己的行)").isEqualTo(1200L);
        assertThat(((Number) catering.get("net_revenue_cent")).longValue())
                .as("兑付纯留痕,不进净份额").isZero();

        Long formatSum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(gift_actual_paid_cent), 0) FROM settlement_bill_format_shares WHERE bill_id = ?",
                Long.class, billId);
        assertThat(formatSum).as("K14:Σ各业态实付 ≡ 台账实付合计").isEqualTo(ledgerSum(billId, "actual_paid_cent"))
                .isEqualTo(2000L);
    }

    // ───────────────── ⑧ append-only 硬强制 ─────────────────

    /** 台账行 UPDATE/DELETE 被 V344 触发器族当场拒绝(COMPLEX_APPEND_ONLY_VIOLATION)。 */
    @Test
    void ledgerRejectsUpdateAndDelete() {
        String pid = newProject();
        String merchant = merchant();
        seedGiftConfig(pid, "by_ticket_amount", 5900L, "platform_absorb");
        setRankTier(pid, 1, 9, 10000);
        String ticket = "tkt_s7ao_" + Long.toHexString(System.nanoTime());
        verifyPooled(pid, merchant, ticket, "v7ao1_" + ticket, 1, AT);
        String billId = settlementService.settleMonth(pid, MONTH);
        assertThat(ledgerRows(billId)).hasSize(1);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE pass_gift_cost_variance_ledger SET variance_cent = 0 WHERE bill_id = ?", billId))
                .hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM pass_gift_cost_variance_ledger WHERE bill_id = ?", billId))
                .hasMessageContaining("COMPLEX_APPEND_ONLY_VIOLATION");
    }

    // ─────────────────────────── helpers ───────────────────────────

    private String newProject() {
        String pid = "proj_s7_" + System.nanoTime();
        projectIds.add(pid);
        return pid;
    }

    private String merchant() {
        String m = "bars7_" + Long.toHexString(System.nanoTime());
        merchants.add(m);
        return m;
    }

    /** 建商户主数据行(业态五值之一),供兑付按提供方业态归堆。 */
    private String seedMerchantRow(String merchantType) {
        String id = "m_s7_" + merchantType + "_" + Long.toHexString(System.nanoTime());
        merchantRows.add(id);
        jdbcTemplate.update(
                "INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)"
                        + " VALUES (?, ?, ?, 'active', now(), now())",
                id, "S7测试商户-" + merchantType, merchantType);
        return id;
    }

    /** 项目级 G 预扣规则(该项目所有票种通用),带差额处理规则,直接生效。 */
    private String seedGiftConfig(String projectId, String mode, Long ticketGiftAmountCent, String varianceHandling) {
        String id = "pgc_s7_" + Long.toHexString(System.nanoTime());
        giftConfigIds.add(id);
        jdbcTemplate.update(
                "INSERT INTO pass_gift_cost_configs (id, project_id, ticket_type_id, prededuct_mode,"
                        + " ticket_gift_amount_cent, variance_handling, status, is_draft, updated_by)"
                        + " VALUES (?, ?, NULL, ?, ?, ?, 'active', FALSE, 'tester')",
                id, projectId, mode, ticketGiftAmountCent, varianceHandling);
        return id;
    }

    /** 进池核销(消费者真路径:计提 G/费、落结算事实行),business_at 可指定月份。 */
    private void verifyPooled(String projectId, String merchant, String ticket, String verificationId,
                              int used, Instant at) {
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                verificationId, at.plusSeconds(used), "ent_" + ticket, "ord_x", "tt_s7", "entry", merchant,
                5, used, 1, 50000L, at.plusSeconds(86400), null, null, null, projectId, ticket));
    }

    /** 兑付挂账:平台按内部结算价向提供方商户挂应付(source=pass_drink_settlement,幂等键=核销号)。 */
    private void seedPayable(String verificationId, String providerMerchantId, long amountCent) {
        insertArAp("payable", verificationId, providerMerchantId, amountCent);
    }

    /** 兑付冲正:对冲应收(同 source_id、direction 不同 → 与原应付净额归零)。 */
    private void seedReceivableOffset(String verificationId, String providerMerchantId, long amountCent) {
        insertArAp("receivable", verificationId, providerMerchantId, amountCent);
    }

    private void insertArAp(String direction, String verificationId, String counterpartyId, long amountCent) {
        arApSourceIds.add(verificationId);
        String id = "arap_s7_" + System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id,
                    amount_cent, paid_amount_cent, unpaid_amount_cent, currency, due_date, accounting_period,
                    status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'pass_drink_settlement', ?, ?, 0, ?, 'CNY', CURRENT_DATE + 30, ?,
                    'open', now(), now())
                """,
                id, "NO-" + id, direction, counterpartyId, verificationId, amountCent, amountCent,
                YearMonth.now().toString());
    }

    /** 直接落账本形态的进池核销行(消费者产物,G=0,复算 S6 K9 基线用)。 */
    private void seedVerified(String projectId, String merchantId, int verifiedCount, int numPeople,
                              long verifiedCent, long compServiceFeeCent) {
        java.sql.Timestamp ts = java.sql.Timestamp.from(AT);
        String id = "st_s7_" + System.nanoTime();
        com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, """
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    verification_id, settlement_status, entitlement_valid_to, business_at, created_at, updated_at,
                    comp_service_fee_cent, gift_cost_cent)
                VALUES (?, ?, 'verification_settled', ?, 'entry', ?, ?, ?, ?, ?, 0, 'merchant', ?, ?, ?, 'draft',
                    ?, ?, ?, ?, ?, 0)
                """,
                id, "TN-" + id, "ent_" + id, verifiedCount,
                Math.max(Math.abs(verifiedCount), 1), numPeople, verifiedCent, verifiedCent,
                merchantId, projectId, "vf_" + id, ts, ts, ts, ts, compServiceFeeCent);
    }

    private void setRankTier(String projectId, int rankFrom, int rankTo, int shareBps) {
        jdbcTemplate.update(
                "INSERT INTO ticket_distribution_rank_tiers (id, project_id, rank_from, rank_to, share_bps,"
                        + " status, is_draft) VALUES (?, ?, ?, ?, ?, 'active', FALSE)",
                "trt_s7_" + Long.toHexString(System.nanoTime()), projectId, rankFrom, rankTo, shareBps);
    }

    private List<Map<String, Object>> ledgerRows(String billId) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM pass_gift_cost_variance_ledger WHERE bill_id = ? ORDER BY id", billId);
    }

    private long ledgerSum(String billId, String column) {
        Long sum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(" + column + "), 0) FROM pass_gift_cost_variance_ledger WHERE bill_id = ?",
                Long.class, billId);
        return sum == null ? 0L : sum;
    }

    private Map<String, Object> bill(String projectId) {
        return jdbcTemplate.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ?", projectId);
    }

    private Map<String, Object> formatShare(String billId, String format) {
        return jdbcTemplate.queryForMap(
                "SELECT * FROM settlement_bill_format_shares WHERE bill_id = ? AND business_format = ?",
                billId, format);
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
