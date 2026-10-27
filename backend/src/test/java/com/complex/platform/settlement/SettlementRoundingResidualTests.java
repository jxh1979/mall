package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.PassPoolFixtures;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * R-7「四舍五入严格落地 + 尾差单列留痕」验收（2026-07-22 用户拍板 D15.1 / 治理地图 HTML
 * 「全局原则 · 金额一律精确到分」）。动资金引擎，TDD + 守恒。
 *
 * <p>三条核心断言：
 * <ol>
 *   <li><b>四舍五入而非整除截断</b>：按参与家数档位行分第二段时各家金额 = round_half_up(第二段余额 × 适用比例)；
 *       旧 {@code pool * shareBps / 10000} 在同一算例上少给（用例给出旧值以证伪）。</li>
 *   <li><b>池链综合服务费线性求和</b>(S4 合并留存后)：池 = 收入 − Σ每笔计提费,
 *       费的四舍五入发生在每笔核销计提时(ComprehensiveServiceFee),月结只做线性求和。</li>
 *   <li><b>尾差单列留痕并归平台</b>：各家舍入后与应分总额的差额落
 *       {@code settlement_bills.rounding_residual_cent}（V343），且守恒
 *       {@code Σ池分账明细 + 尾差 ≡ 应分总额} 逐分成立，不并进任一商户、不静默吞掉。</li>
 * </ol>
 *
 * <p>第十八批第 2 条起按单张票的池快照逐份分账（ENUM L52「档位为动态百分比，不是相对权重、不做归一化」）：分账类用例给父票补池快照，
 * 按参与家数行取比例、当期入场人数排名；原「排名档位归一化分尽」口径随之改写（单一档位酒吧独享整池改为「参与 1 家＝100%」）。
 *
 * <p>非 @Transactional（settleMonth 内部真提交）；@AfterEach 按项目清理落地行；每例独立 projectId。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementRoundingResidualTests {

    @Autowired private TicketDistributionSettlementService service;
    @Autowired private JdbcTemplate jdbcTemplate;

    private static final YearMonth MONTH = YearMonth.of(2014, 7); // 远离今日的确定月
    private final List<String> projectIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String pid : projectIds) {
            // V344 起 settlement_transactions 是 append-only 硬强制表(触发器拒 UPDATE/DELETE),
            //   清场 DELETE 须走测试专用旁路(与 SettlementBatch2EngineTests 同范式)。
            com.complex.platform.support.AppendOnlyTestBypass.update(
                    jdbcTemplate, "DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            jdbcTemplate.update(
                    "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
            PassPoolFixtures.deleteByProject(jdbcTemplate, pid);
            jdbcTemplate.update("DELETE FROM ticket_distribution_configs WHERE project_id = ?", pid);
        }
    }

    // ---- ① 第二段按参与家数行分:四舍五入(旧整除截断少给) ----

    @Test
    void participantRowShareRoundsHalfUpInsteadOfTruncating() {
        String pid = newProject();
        String scheme = PassPoolFixtures.scheme(jdbcTemplate, "tdsv_r7a_" + pid, pid, Map.of(3, new int[] {3333, 3333, 3334}));
        PassPoolFixtures.snapshot(jdbcTemplate, "tk_" + pid, pid, 1000L, scheme, List.of("r1", "r2", "r3"), Map.of());
        // 池 1000 分;r1/r2 精确 333.3 分。旧整除:1000×3333/10000 = 333(截断);新四舍五入:333.3 → 333,r3 333.4 → 333。
        seedVerified(pid, "tk_" + pid, "r1", 3);
        seedVerified(pid, "tk_" + pid, "r2", 2);
        seedVerified(pid, "tk_" + pid, "r3", 1);

        service.settleMonth(pid, MONTH);

        assertThat(num(bill(pid), "available_pool_cent")).isEqualTo(1000L);
        assertThat(itemAmount(pid, "r1")).isEqualTo(333L);
        // 应分总额 = 池 1000;Σ已舍入 = 999 → 尾差 +1 归平台。
        assertThat(num(bill(pid), "rounding_residual_cent")).isEqualTo(1L);
        assertConserved(pid, 1000L);
    }

    @Test
    void participantRowShareCarriesUpWhereTruncationWouldLose() {
        String pid = newProject();
        String scheme = PassPoolFixtures.scheme(jdbcTemplate, "tdsv_r7b_" + pid, pid, Map.of(2, new int[] {1255, 8745}));
        PassPoolFixtures.snapshot(jdbcTemplate, "tk_" + pid, pid, 100L, scheme, List.of("c1", "c2"), Map.of());
        seedVerified(pid, "tk_" + pid, "c1", 3);
        seedVerified(pid, "tk_" + pid, "c2", 2);       // 池 100 分
        // 第 1 名精确 = 100 × 12.55% = 12.55 分。旧整除截断 = 12(少给 1 分);新四舍五入 = 13。
        service.settleMonth(pid, MONTH);

        assertThat(num(bill(pid), "available_pool_cent")).isEqualTo(100L);
        assertThat(100L * 1255L / 10000L).isEqualTo(12L);   // 证伪:旧整除截断给 12
        assertThat(itemAmount(pid, "c1")).isEqualTo(13L);   // 新:四舍五入给 13
        assertThat(itemAmount(pid, "c2")).isEqualTo(87L);   // round(87.45) = 87
        assertThat(num(bill(pid), "rounding_residual_cent")).isZero();
        assertConserved(pid, 100L);
    }

    /** 第二段只有一家参与：取「参与 1 家」行＝第 1 名 100%，独享第二段余额（不再是按相对权重归一化）。 */
    @Test
    void singleParticipantTakesWholeSecondSegmentByTheOneBarRow() {
        String pid = newProject();
        PassPoolFixtures.snapshot(jdbcTemplate, "tk_" + pid, pid, 100L, "solo");
        seedVerified(pid, "tk_" + pid, "solo", 1);
        service.settleMonth(pid, MONTH);

        assertThat(itemAmount(pid, "solo")).isEqualTo(100L);
        assertThat(num(bill(pid), "rounding_residual_cent")).isZero();
        assertThat(num(bill(pid), "pool_undistributed_cent")).isZero();
        assertConserved(pid, 100L);
    }

    // ---- ② 池链综合服务费线性求和(S4 合并留存:原「留存四舍五入」随留存链删除) ----

    @Test
    void pooledServiceFeeReducesPoolByExactPerVerificationSum() {
        String pid = newProject();
        // 每笔核销计提的费在核销时已按 HALF_UP 落库(如 1001 × 20.55% → 206),月结只做线性求和。
        seedVerifiedWithFee(pid, null, "t1", 1, 1001L, 206L);
        service.settleMonth(pid, MONTH);

        Map<String, Object> b = bill(pid);
        assertThat(num(b, "comp_service_fee_pooled_cent")).isEqualTo(206L);
        long barNet = PassPoolFixtures.barNetCent(jdbcTemplate, (String) b.get("id"));
        assertThat(barNet).isEqualTo(795L); // 1001 − 206
        // 拆份额链守恒:收入 = 费 + 酒吧净份额(渠道费 0)
        assertThat(num(b, "comp_service_fee_pooled_cent") + barNet).isEqualTo(num(b, "ticket_revenue_cent"));
        // 无池快照(同不含酒吧入场、未建池快照的票):酒吧净份额按 ENUM L52 末句整笔单列未分配池余额。
        assertThat(num(b, "pool_undistributed_cent")).isEqualTo(barNet);
    }

    // ---- ③ 尾差单列留痕 + 守恒(核心) ----

    @Test
    void roundingResidualIsRecordedSeparatelyAndConserves() {
        String pid = newProject();
        PassPoolFixtures.snapshot(jdbcTemplate, "tk_" + pid, pid, 10000L, "s1", "s2", "s3");
        seedVerified(pid, "tk_" + pid, "s1", 1);
        seedVerified(pid, "tk_" + pid, "s2", 1);
        seedVerified(pid, "tk_" + pid, "s3", 1);      // 当期入场人数相同 → 三家并列,各取 (40%+30%+30%)÷3

        service.settleMonth(pid, MONTH);

        // 各家精确 = 10000 × 33.333…% = 3333.33…→ 舍入各 3333(Σ=9999);应分总额 = 池 10000 → 尾差 +1 单列归平台。
        assertThat(itemAmount(pid, "s1")).isEqualTo(3333L);
        assertThat(num(bill(pid), "rounding_residual_cent")).isEqualTo(1L);
        assertConserved(pid, 10000L);
    }

    @Test
    void residualIsNegativeWhenEveryShareCarriesUp() {
        String pid = newProject();
        PassPoolFixtures.snapshot(jdbcTemplate, "tk_" + pid, pid, 101L, "n1", "n2");
        seedVerified(pid, "tk_" + pid, "n1", 1);
        seedVerified(pid, "tk_" + pid, "n2", 1);      // 两家并列 → 各 (57%+43%)÷2 = 50%;池 101 分

        service.settleMonth(pid, MONTH);

        // 每家精确 50.5 → HALF_UP 各 51(Σ=102);应分总额 = 池 101 → 尾差 −1(平台承担的追回)。
        assertThat(itemAmount(pid, "n1")).isEqualTo(51L);
        assertThat(itemAmount(pid, "n2")).isEqualTo(51L);
        assertThat(num(bill(pid), "rounding_residual_cent")).isEqualTo(-1L);
        assertConserved(pid, 101L);    // 102 + (−1) = 101,尾差单列后总额仍严格相等
    }

    @Test
    void residualIsZeroForFirstSegmentFixedAmountsSoBehaviourIsUnchanged() {
        String pid = newProject();
        // 两家都在快照的第一段名单(每份固定金额),整数绝对额无舍入。
        PassPoolFixtures.snapshot(jdbcTemplate, "tk_" + pid, pid, 10000L, PassPoolFixtures.FACTORY_SCHEME,
                List.of("f1", "f2"), Map.of("f1", 777L, "f2", 333L));
        seedVerified(pid, "tk_" + pid, "f1", 2);
        seedVerified(pid, "tk_" + pid, "f2", 1);

        service.settleMonth(pid, MONTH);

        assertThat(itemAmount(pid, "f1")).isEqualTo(777L);
        assertThat(itemAmount(pid, "f2")).isEqualTo(333L);
        assertThat(num(bill(pid), "rounding_residual_cent")).isZero(); // 固定额无舍入 → 无尾差
        assertConserved(pid, 1110L);   // 应分总额 = 777 + 333
        // 第二段无参与者 → 池余额 10000 − 1110 = 8890 显式归娱乐平台公司留痕(非尾差,分列)。
        assertThat(num(bill(pid), "pool_undistributed_cent")).isEqualTo(8890L);
        assertThat(bill(pid).get("pool_undistributed_entity_id")).isEqualTo("be_ent_platform");
    }

    /**
     * 守恒核心断言:{@code Σ池分账明细 + 尾差 ≡ 应分总额}(应分总额由用例按业务算式独立给出,不复用引擎中间值),
     * 且尾差量级 ≤ 参与家数(越界=分配算法错而非舍入)。
     */
    private void assertConserved(String pid, long expectedDesignedTotalCent) {
        long residual = num(bill(pid), "rounding_residual_cent");
        Long pooled = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(si.calculated_amount_cent), 0) FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.distribution_method <> 'exclusive_full'
                """, Long.class, pid);
        Integer parties = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.distribution_method <> 'exclusive_full'
                """, Integer.class, pid);
        assertThat(Math.abs(residual))
                .as("尾差量级须 ≤ 参与分账家数,越界=分配算法错而非舍入")
                .isLessThanOrEqualTo(Math.max(1L, parties));
        assertThat(pooled + residual)
                .as("Σ池分账(%d) + 尾差(%d) 必须等于应分总额(尾差单列后总额严格相等)", pooled, residual)
                .isEqualTo(expectedDesignedTotalCent);
    }

    // ---- helpers ----

    private String newProject() {
        String pid = "proj_r7_" + System.nanoTime();
        projectIds.add(pid);
        return pid;
    }

    /** 池快照票在某家的一笔入场核销(当期入场人数 numPeople;金额 0:池金额取快照)。 */
    private void seedVerified(String projectId, String ticket, String merchantId, int numPeople) {
        seedVerifiedWithFee(projectId, ticket, merchantId, numPeople, 0L, 0L);
    }

    private void seedVerifiedWithFee(String projectId, String ticket, String merchantId, int numPeople,
                                     long verifiedCent, long compServiceFeeCent) {
        Instant createdAt = MONTH.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(createdAt);
        String id = "st_r7_" + System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    verification_id, settlement_status, entitlement_valid_to, created_at, updated_at,
                    comp_service_fee_cent, parent_ticket_id)
                VALUES (?, ?, 'verification_settled', ?, 'entry', 1, 1, ?, ?, ?, 0, 'merchant', ?, ?, ?,
                    'draft', ?, ?, ?, ?, ?)
                """,
                id, "TN-" + id, "ent_" + id, numPeople, verifiedCent, verifiedCent,
                merchantId, projectId, "vf_" + id, ts, ts, ts, compServiceFeeCent, ticket);
    }

    private Map<String, Object> bill(String projectId) {
        return jdbcTemplate.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ?", projectId);
    }

    private long itemAmount(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.calculated_amount_cent FROM settlement_items si
                  JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ?
                """, Long.class, projectId, merchantId);
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
