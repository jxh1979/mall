package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * S4「合并留存·门票综合服务费进主链」验收(V370)。
 *
 * <p>业务上这一刀回答的是:<b>原「池内 20% 项目留存」与门票综合服务费是同一项、只扣一次</b>
 * (业务 HTML「即娱乐平台公司留存/项目管理费」「不再从可分配池中二次扣娱乐平台公司留存」),
 * 且这道费<b>归娱乐平台公司</b>。本类要证明四件事:
 *
 * <ol>
 *   <li><b>K9 等价保值(上线安全网)</b>:费配 2000bps(= V370 把原留存率平移进综合服务费的等价保值种子)时,
 *       结算单<b>每一列</b>、分账明细<b>每一行</b>与改造前(池内 20% 留存)的手算值<b>逐分一致</b>——
 *       金额一分不变,变的只是名目(留存 → 综合服务费)与收款方(项目公司 → 娱乐平台公司,HTML 要求的正名);</li>
 *   <li><b>负收入月(R-3 带符号)</b>:红冲多于新核销的月份,收入/池/费/分账全部带符号落库,
 *       月结不崩、负数=向对方追回、不静默钳零丢账;同月正冲对全部净零(线性取反);</li>
 *   <li><b>收款主体快照</b>:综合服务费收款方解析自归属配置(V370 全局种子=娱乐平台公司,项目可覆盖),
 *       月结时定格进结算单;</li>
 *   <li><b>K13 合并锁</b>:留存列/留存规则在库里不复存在生效形态——不存在任何被扣两次的路径。</li>
 * </ol>
 *
 * <p>非 @Transactional(settleMonth 内部真提交);@AfterEach 按项目清理;每例独立 projectId。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementMergeRetentionS4Tests {

    private static final YearMonth MONTH = YearMonth.of(2016, 3); // 远离今日的确定月
    private static final Instant AT = MONTH.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);

    @Autowired private TicketDistributionSettlementService settlementService;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<String> projectIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String pid : projectIds) {
            com.complex.platform.support.AppendOnlyTestBypass.update(
                    jdbcTemplate, "DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            jdbcTemplate.update(
                    "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
            jdbcTemplate.update("DELETE FROM revenue_stream_attribution WHERE project_id = ?", pid);
        }
        projectIds.clear();
    }

    // ─────────────────────────── ① K9 等价保值(本刀的核心测试) ───────────────────────────

    /**
     * <b>K9</b>:4 家酒吧、收入 150000,每笔核销按 2000bps(等价保值费率)计提综合服务费 → Σ费 30000。
     *
     * <p>改造前手算(池内留存口径):池 = 150000;留存 = round(150000×20%) = 30000;各酒吧总池 = 120000。
     * K9 的证明对象是这两列(费 = 原留存额、池 = 原各酒吧总池)逐分不变。
     *
     * <p>分账列自 S8 起按「第二段归一化分尽」口径:档位 1600/1200×3(Σ=5200)对 120000 一次分光
     * → 36923/27692×3 + 尾差 1。断言方式 = 结算单整行 {@code SELECT *} 转 Map <b>全列比对</b> +
     * 每条分账明细整行 <b>全列比对</b>,不挑列(挑几列看,恰好没挑的那列错了就漏过去)。
     */
    @Test
    void k9_feeAt2000bps_everyBillColumnAndEveryItemRowMatchesPreRebuildFigures() {
        String pid = newProject();
        seedVerified(pid, "bar1", 20, 8, 60000L, 12000L); // 每笔费 = 金额 × 2000bps(计提时逐笔 HALF_UP)
        seedVerified(pid, "bar2", 15, 6, 40000L, 8000L);
        seedVerified(pid, "bar3", 10, 4, 30000L, 6000L);
        seedVerified(pid, "bar4", 5, 2, 20000L, 4000L);

        String billId = settlementService.settleMonth(pid, MONTH);

        // ---- 结算单全列比对(与改造前手算值逐分一致) ----
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("id", billId);
        expected.put("project_id", pid);
        expected.put("period_start", java.sql.Date.valueOf(MONTH.atDay(1)));
        expected.put("period_end", java.sql.Date.valueOf(MONTH.atEndOfMonth()));
        expected.put("ticket_revenue_cent", 150000L);
        expected.put("channel_fee_cent", 0L);
        expected.put("comp_service_fee_pooled_cent", 30000L);   // = 改造前 retention_cent(K9 的核心列,逐分不变)
        expected.put("available_pool_cent", 120000L);           // = 改造前 spv_pool_cent(K9 的核心列,逐分不变)
        // S8 第二段归一化分尽:档位命中 1600/1200/1200/1200(Σ=5200) → 各家 round(120000×w/5200)
        //   = 36923/27692×3,Σ=119999,尾差 +1 单列。(费/池两列仍是 K9 等价保值的逐分证明;
        //   分账列基线自 S8 起为「分尽」口径,不再留 57600 隐式沉淀。)
        expected.put("distributed_cent", 119999L);
        expected.put("comp_service_fee_exclusive_cent", 0L);
        expected.put("fee_beneficiary_entity_id", "be_ent_platform"); // 收款方正名:归娱乐平台公司(V370 归属种子)
        expected.put("gift_cost_cent", 0L); // S6:未配非零 G 规则(种子 by_ticket_amount+0)恒 0=逐分不变
        expected.put("gift_beneficiary_entity_id", "be_ent_platform"); // V372 归属种子:归娱乐平台公司
        expected.put("distribution_method", "by_rank_tier");
        expected.put("special_approval_ref", null);
        expected.put("special_approval_valid_until", null);
        expected.put("status", "confirmed");
        expected.put("confirmed_at", null);
        expected.put("settled_at", null);
        expected.put("attributed_entity_id", "be_ent_platform"); // V375 all_pass_ticket 全局种子
        expected.put("combo_discount_cent", 0L);
        expected.put("rounding_residual_cent", 1L);
        expected.put("pool_undistributed_cent", 0L);
        expected.put("pool_undistributed_entity_id", null);

        Map<String, Object> actual = new HashMap<>(bill(pid));
        actual.remove("created_at");
        actual.remove("updated_at");
        assertThat(actual)
                .as("K9:费配 2000bps 时结算单每一列与改造前(池内 20% 留存)逐分一致")
                .isEqualTo(expected);

        // ---- 分账明细每行全列比对(S8 归一化分尽口径) ----
        Map<String, Map<String, Object>> items = itemRows(pid);
        assertThat(items).containsOnlyKeys("bar1", "bar2", "bar3", "bar4");
        assertItemRow(items.get("bar1"), billId, "bar1", 20, 8, 1, 36923L); // round(120000×1600/5200)
        assertItemRow(items.get("bar2"), billId, "bar2", 15, 6, 2, 27692L); // round(120000×1200/5200)
        assertItemRow(items.get("bar3"), billId, "bar3", 10, 4, 3, 27692L);
        assertItemRow(items.get("bar4"), billId, "bar4", 5, 2, 4, 27692L);

        // 守恒:收入 = 费 + 池(渠道费 0);Σ分账 + 尾差 = 池(K18,分尽后严格相等)。
        assertThat(30000L + 120000L).isEqualTo(150000L);
        assertThat(36923L + 27692L * 3 + 1L).isEqualTo(120000L);
    }

    // ─────────────────────────── ② 负收入月(R-3 带符号,不崩不丢账) ───────────────────────────

    /**
     * 净红冲月:上月已过账的核销在本月被冲正(冲正额 > 本月新核销)→ 收入/费/池/分账全部为负。
     * 负数 = 向对方追回:池 −64000 → 该酒吧分账 −64000(档位 100%)。月结必须正常落库,不许崩、不许钳零。
     */
    @Test
    void negativeMonthSettlesWithSignedAmountsInsteadOfCrashingOrClampingToZero() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 10000); // 单档 100%,负池整额落到该家
        seedReversal(pid, "bar_neg", 8, 3, 80000L, 16000L); // 冲正:金额/费均取原行之反

        settlementService.settleMonth(pid, MONTH);

        Map<String, Object> b = bill(pid);
        assertThat(num(b, "ticket_revenue_cent")).isEqualTo(-80000L);
        assertThat(num(b, "comp_service_fee_pooled_cent")).as("费带符号追回,不钳零").isEqualTo(-16000L);
        assertThat(num(b, "available_pool_cent")).isEqualTo(-64000L);
        assertThat(num(b, "distributed_cent")).as("负池月分账为负 = 向酒吧追回").isEqualTo(-64000L);
        assertThat(num(b, "rounding_residual_cent")).isZero();
        assertThat(itemRows(pid).get("bar_neg").get("final_amount_cent")).isEqualTo(-64000L);
    }

    /** 线性取反:同月「核销 + 对应冲正」→ 结算单各金额列全部净零(钱一分不留也一分不丢)。 */
    @Test
    void verificationPlusItsReversalNetsEveryAmountColumnToZero() {
        String pid = newProject();
        setRankTier(pid, 1, 9, 10000);
        seedVerified(pid, "bar_net", 5, 2, 50000L, 10000L);
        seedReversal(pid, "bar_net", 5, 2, 50000L, 10000L);

        settlementService.settleMonth(pid, MONTH);

        Map<String, Object> b = bill(pid);
        for (String col : List.of("ticket_revenue_cent", "channel_fee_cent", "comp_service_fee_pooled_cent",
                "available_pool_cent", "distributed_cent", "comp_service_fee_exclusive_cent",
                "combo_discount_cent", "rounding_residual_cent", "pool_undistributed_cent")) {
            assertThat(num(b, col)).as("线性取反后 %s 必须净零", col).isZero();
        }
    }

    // ─────────────────────────── ③ 收款主体快照(归娱乐平台公司,项目可覆盖) ───────────────────────────

    /** 项目级归属覆盖:该项目的综合服务费收款方按项目配置定格,不影响全局默认。 */
    @Test
    void feeBeneficiarySnapshotFollowsProjectOverride() {
        String pid = newProject();
        jdbcTemplate.update("""
                INSERT INTO revenue_stream_attribution
                    (id, project_id, revenue_stream, management_entity_id, beneficiary_kind, status, is_draft,
                     updated_by, updated_at)
                VALUES (?, ?, 'ticket_service_fee', 'be_amusement', 'entity', 'active', FALSE, 'tester', now())
                """, "rsa_s4_" + pid, pid);
        seedVerified(pid, "bar_ov", 1, 1, 1000L, 200L);

        settlementService.settleMonth(pid, MONTH);

        assertThat(bill(pid).get("fee_beneficiary_entity_id"))
                .as("项目级归属覆盖生效,月结时定格进单据")
                .isEqualTo("be_amusement");
    }

    // ─────────────────────────── ④ K13 合并锁:留存不复存在生效形态 ───────────────────────────

    /**
     * 同一项只扣一次的结构性保证:结算单上不再有任何留存列(被扣两次在 schema 上就不可能),
     * platform_retention_rules 整表停用(留库只作历史决策痕迹,无任何生效行)。
     */
    @Test
    void k13_retentionColumnsAndActiveRulesNoLongerExist() {
        List<String> billColumns = jdbcTemplate.queryForList("""
                SELECT column_name FROM information_schema.columns
                 WHERE table_name = 'settlement_bills'
                """, String.class);
        assertThat(billColumns)
                .doesNotContain("retention_bps", "retention_cent", "spv_pool_cent", "giveaway_cost_cent")
                .contains("comp_service_fee_pooled_cent", "comp_service_fee_exclusive_cent",
                        "fee_beneficiary_entity_id");

        Integer activeRetention = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM platform_retention_rules WHERE status = 'active'", Integer.class);
        assertThat(activeRetention).as("留存规则表已整体停用(软删留库,零生效行)").isZero();
    }

    // ─────────────────────────── helpers ───────────────────────────

    private String newProject() {
        String pid = "proj_s4_" + System.nanoTime();
        projectIds.add(pid);
        return pid;
    }

    private void setRankTier(String projectId, int rankFrom, int rankTo, int shareBps) {
        jdbcTemplate.update("""
                INSERT INTO ticket_distribution_rank_tiers (id, project_id, rank_from, rank_to, share_bps,
                    status, is_draft)
                VALUES (?, ?, ?, ?, ?, 'active', FALSE)
                ON CONFLICT DO NOTHING
                """, "trt_s4_" + projectId + "_" + rankFrom, projectId, rankFrom, rankTo, shareBps);
    }

    /** 非专属(进池)核销行,带每笔计提的池链综合服务费。 */
    private void seedVerified(String projectId, String merchantId, int verifiedCount, int numPeople,
                              long verifiedCent, long compServiceFeeCent) {
        insert(projectId, merchantId, "verification_settled",
                verifiedCount, numPeople, verifiedCent, compServiceFeeCent);
    }

    /** 冲正(负向)行:金额/次数/费皆取原行之反。 */
    private void seedReversal(String projectId, String merchantId, int verifiedCount, int numPeople,
                              long verifiedCent, long compServiceFeeCent) {
        insert(projectId, merchantId, "verification_reversed",
                -verifiedCount, -numPeople, -verifiedCent, -compServiceFeeCent);
    }

    private void insert(String projectId, String merchantId, String type, int verifiedCount,
                        int numPeople, long verifiedCent, long compServiceFeeCent) {
        java.sql.Timestamp ts = java.sql.Timestamp.from(AT);
        String id = "st_s4_" + System.nanoTime();
        com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, """
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    verification_id, settlement_status, entitlement_valid_to, business_at, created_at, updated_at,
                    comp_service_fee_cent)
                VALUES (?, ?, ?, ?, 'entry', ?, ?, ?, ?, ?, 0, 'merchant', ?, ?, ?, 'draft', ?, ?, ?, ?, ?)
                """,
                id, "TN-" + id, type, "ent_" + id, verifiedCount,
                Math.max(Math.abs(verifiedCount), 1), numPeople, verifiedCent, verifiedCent,
                merchantId, projectId, "vf_" + id, ts, ts, ts, ts, compServiceFeeCent);
    }

    private Map<String, Object> bill(String projectId) {
        return jdbcTemplate.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ?", projectId);
    }

    private Map<String, Map<String, Object>> itemRows(String projectId) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbcTemplate.queryForList("""
                SELECT si.* FROM settlement_items si JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? ORDER BY si.merchant_id
                """, projectId)) {
            out.put((String) row.get("merchant_id"), row);
        }
        return out;
    }

    /** 分账明细整行全列断言(id/bill/商户/核销数/客数/排名/方式/金额×2/状态/未过账)。 */
    private void assertItemRow(Map<String, Object> row, String billId, String merchantId,
                               int verifiedCount, int guestCount, int rankNo, long amountCent) {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("id", "si_" + billId + "_" + merchantId);
        expected.put("bill_id", billId);
        expected.put("merchant_id", merchantId);
        expected.put("verified_count", verifiedCount);
        expected.put("guest_count", guestCount);
        expected.put("rank_no", rankNo);
        expected.put("distribution_method", "by_rank_tier");
        expected.put("calculated_amount_cent", amountCent);
        expected.put("final_amount_cent", amountCent);
        expected.put("ar_ap_item_id", null);
        expected.put("status", "draft");
        Map<String, Object> actual = new HashMap<>(row);
        actual.remove("created_at");
        actual.remove("updated_at");
        assertThat(actual).as("K9:%s 的分账明细整行逐列一致", merchantId).isEqualTo(expected);
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
