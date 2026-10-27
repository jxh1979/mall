package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.AppendOnlyTestBypass;
import com.complex.platform.ticketing.SettlementSettledEvent;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
 * 第十八批第 2 条：酒吧通票逐份分账（settlement-rebuild-prd §5.1 U-1～U-8 与 R8-H2 ①～⑤）。
 *
 * <p>依据：ENUM L52「档位为动态百分比，不是相对权重、不做归一化：一张票的第二段余额，由该票覆盖的酒吧中参加第二段的那几家，
 * 按其在第二段参与者内部的排名分配……按本张票的第二段参与家数查「参与家数档位表」……按各酒吧当期核销入场人数由多到少排定」
 * 「当期核销入场人数相同的酒吧为并列名次……各家一律取这几个名次的百分比之和的算术平均值」「第二段参与家数决定取哪一行，与是否核销无关……
 * 核销才拿」「该酒吧参加第一段的，按其第一段的固定金额或固定单价一次减除」「三者即使同归一个主体，也必须分三栏分别列示」
 * 「结算时遇到本张票的第二段参与家数尚未配置对应行的，该张票的第二段分配暂挂……补配生效后，因缺该行而暂挂的票按该行续算结算」；
 * BSM L778/L780/L800 每人每次入场一份完整票权益；BSM L807 已检票未核销按场次终结制进入分账减除。
 *
 * <p>池快照由夹具直接写入（售票时固化本身见 PassPoolSnapshotJdbcIntegrationTests），核销走真实结算消费者，月结走真实引擎。
 * 名次只看当期入场人数：夹具另造金额为 0 的入场核销调出各家名次，不影响金额。非 @Transactional（消费者 REQUIRES_NEW），@AfterEach 清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementPassPerTicketEngineTests {

    private static final YearMonth MONTH = YearMonth.of(2015, 3); // 远离今日的确定月
    private static final Instant AT = MONTH.atDay(12).atTime(21, 0).toInstant(ZoneOffset.UTC);
    private static final String FACTORY = "tdsv_factory";

    @Autowired private SettlementSettledConsumer settled;
    @Autowired private TicketDistributionSettlementService settlement;
    @Autowired private SettlementExpiryJob expiryJob;
    @Autowired private JdbcTemplate jdbc;

    private final String key = Long.toHexString(System.nanoTime());
    private final String project = "prj_ppe_" + key;
    private final String ticketType = "tt_ppe_" + key;
    private final String template = "et_ppe_" + key;
    private final List<String> merchants = new ArrayList<>();
    private int seq;

    @BeforeEach
    void setUp() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, project);
        jdbc.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES (?, '逐份分账测试通票', 'fixed_price', FALSE, 'published', now(), now())""", ticketType);
        jdbc.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,
                    created_at, updated_at) VALUES (?, '入场', 'entry', 1, 0, 'active', now(), now())""", template);
    }

    @AfterEach
    void cleanup() {
        com.complex.platform.support.TestSecurityActors.clear();
        AppendOnlyTestBypass.update(jdbc, "DELETE FROM settlement_transactions WHERE project_id = ?", project);
        AppendOnlyTestBypass.updateOnTable(jdbc, "pass_gift_cost_variance_ledger", "trg_append_only_ud",
                "DELETE FROM pass_gift_cost_variance_ledger WHERE project_id = ?", project);
        for (String table : List.of("settlement_items", "settlement_bill_format_shares")) {
            jdbc.update("DELETE FROM " + table + " WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)", project);
        }
        jdbc.update("DELETE FROM settlement_bills WHERE project_id = ?", project);
        jdbc.update("DELETE FROM pass_pool_snapshots WHERE project_id = ?", project);
        jdbc.update("DELETE FROM ticket_distribution_scheme_versions WHERE project_id = ?", project);
        jdbc.update("DELETE FROM channel_fee_configs WHERE project_id = ?", project);
        jdbc.update("DELETE FROM issued_entitlements WHERE ticket_type_id = ?", ticketType);
        jdbc.update("DELETE FROM ticket_instances WHERE ticket_type_id = ?", ticketType);
        jdbc.update("DELETE FROM ticket_orders WHERE ticket_type_id = ?", ticketType);
        jdbc.update("DELETE FROM entitlement_templates WHERE id = ?", template);
        jdbc.update("DELETE FROM ticket_types WHERE id = ?", ticketType);
        for (String m : merchants) {
            jdbc.update("DELETE FROM merchants WHERE id = ?", m);
        }
        jdbc.update("DELETE FROM projects WHERE id = ?", project);
    }

    /** U-1 三家全核销：取「参与 3 家」行 40/30/30，按当期入场人数排名，分完不归一化。 */
    @Test
    void allCoveredBarsVerifiedTakeParticipantRowShares() {
        String a = bar("a");
        String b = bar("b");
        String c = bar("c");
        legacyEntries(a, 2);
        legacyEntries(b, 1);
        String t = ticket("t1");
        snapshot(t, 10_000L, 1, FACTORY, List.of(a, b, c), Map.of());
        use(t, 1, a);
        use(t, 1, b);
        use(t, 1, c);

        settlement.settleMonth(project, MONTH);

        assertThat(item(a, "by_rank_tier")).isEqualTo(4_000L);
        assertThat(item(b, "by_rank_tier")).isEqualTo(3_000L);
        assertThat(item(c, "by_rank_tier")).isEqualTo(3_000L);
        Map<String, Object> bill = bill();
        assertThat(num(bill, "unverified_deduction_cent")).isZero();
        assertThat(num(bill, "available_pool_cent")).isEqualTo(10_000L);
        Map<String, Map<String, Object>> units = units(t);
        assertThat(num(units.get("1|1|" + a), "rank_no")).isEqualTo(1L);
        assertThat(((java.math.BigDecimal) units.get("1|1|" + a).get("pct"))).isEqualByComparingTo("40");
        assertThat(((java.math.BigDecimal) units.get("1|1|" + c).get("pct"))).isEqualByComparingTo("30");
    }

    /** U-2 第一段酒吧未核销：按其固定金额一次减除，不再按第二段档位二次减除；余额由第二段两家按「参与 2 家」行分。 */
    @Test
    void firstSegmentBarNotVerifiedIsDeductedOnceByItsFixedAmount() {
        String a = bar("a");
        String b = bar("b");
        String c = bar("c");
        legacyEntries(b, 1);
        String v = version(Instant.parse("2014-01-01T00:00:00Z"), 1, 2, 3);
        String t = ticket("t1");
        snapshot(t, 10_000L, 1, v, List.of(a, b, c), Map.of(a, 2_000L));
        use(t, 1, b);
        use(t, 1, c);

        settlement.settleMonth(project, MONTH);

        assertThat(item(b, "by_rank_tier")).as("第二段余额 8000 × 57%").isEqualTo(4_560L);
        assertThat(item(c, "by_rank_tier")).as("第二段余额 8000 × 43%").isEqualTo(3_440L);
        assertThat(itemTotal(a)).as("未核销的第一段酒吧分得 0").isZero();
        assertThat(num(bill(), "unverified_deduction_cent")).as("只按第一段固定金额减除一次").isEqualTo(2_000L);
        Map<String, Map<String, Object>> units = units(t);
        assertThat(units.keySet().stream().filter(k -> k.endsWith("|" + a))).hasSize(1);
        assertThat(num(units.get("1|1|" + a), "segment")).isEqualTo(1L);
        assertThat(units.get("1|1|" + a).get("verified")).isEqualTo(false);
    }

    /** U-3 第二段酒吧未核销：按其在 3 家行里的比例减除，减除额不流向其余两家（核销才拿）。 */
    @Test
    void secondSegmentBarNotVerifiedIsDeductedNotRedistributed() {
        String a = bar("a");
        String b = bar("b");
        String c = bar("c");
        legacyEntries(a, 2);
        legacyEntries(b, 1);
        String t = ticket("t1");
        snapshot(t, 10_000L, 1, FACTORY, List.of(a, b, c), Map.of());
        use(t, 1, a);
        use(t, 1, b);

        settlement.settleMonth(project, MONTH);

        assertThat(item(a, "by_rank_tier")).isEqualTo(4_000L);
        assertThat(item(b, "by_rank_tier")).isEqualTo(3_000L);
        Map<String, Object> bill = bill();
        assertThat(num(bill, "unverified_deduction_cent")).isEqualTo(3_000L);
        assertThat(num(bill, "pool_undistributed_cent")).isZero();
        assertThat(item(a, "by_rank_tier") + item(b, "by_rank_tier")).as("其余两家合计＝P−减除额−未分配−尾差")
                .isEqualTo(10_000L - num(bill, "unverified_deduction_cent") - num(bill, "pool_undistributed_cent")
                        - num(bill, "rounding_residual_cent"));
    }

    /** U-4 已检票但覆盖酒吧一家都没核销：全池落「通票未核销减除额」栏，未分配池余额为 0。 */
    @Test
    void noBarVerifiedPutsWholePoolIntoDeductionNotUndistributed() {
        String a = bar("a");
        String b = bar("b");
        String park = merchant("park", "amusement");
        String t = ticket("t1");
        snapshot(t, 10_000L, 1, FACTORY, List.of(a, b), Map.of());
        use(t, 1, park, "amusement_entry");

        settlement.settleMonth(project, MONTH);

        Map<String, Object> bill = bill();
        assertThat(num(bill, "unverified_deduction_cent")).isEqualTo(10_000L);
        assertThat(num(bill, "pool_undistributed_cent")).isZero();
        assertThat(itemTotal(a) + itemTotal(b)).isZero();
    }

    /** U-6 5 家覆盖、1 家走第一段 → 取「参与 4 家」行（31/23/23/23），合计 100%，按第二段余额分。 */
    @Test
    void participantRowFollowsSecondSegmentCountNotCoveredCount() {
        String a = bar("a");
        String b = bar("b");
        String c = bar("c");
        String d = bar("d");
        String e = bar("e");
        legacyEntries(a, 3);
        legacyEntries(b, 2);
        legacyEntries(c, 1);
        String v = version(Instant.parse("2014-01-01T00:00:00Z"), 4, 5);
        String t = ticket("t1");
        snapshot(t, 10_000L, 1, v, List.of(a, b, c, d, e), Map.of(e, 1_000L));
        for (String m : List.of(a, b, c, d, e)) {
            use(t, 1, m);
        }

        settlement.settleMonth(project, MONTH);

        assertThat(item(e, "by_fixed_amount")).isEqualTo(1_000L);
        assertThat(item(a, "by_rank_tier")).as("9000 × 31%").isEqualTo(2_790L);
        assertThat(item(b, "by_rank_tier")).as("9000 × 23%").isEqualTo(2_070L);
        assertThat(item(c, "by_rank_tier")).isEqualTo(2_070L);
        assertThat(item(d, "by_rank_tier")).isEqualTo(2_070L);
        assertThat(num(bill(), "rounding_residual_cent")).isZero();
    }

    /** U-6 并列均分：参与 5 家、两家并列第 4 → 各 (20%+15%)÷2＝17.5%，全行合计仍 100%。 */
    @Test
    void tiedBarsShareTheAverageOfTheRanksTheyOccupy() {
        String a = bar("a");
        String b = bar("b");
        String c = bar("c");
        String d = bar("d");
        String e = bar("e");
        legacyEntries(a, 4);
        legacyEntries(b, 3);
        legacyEntries(c, 2);
        String t = ticket("t1");
        snapshot(t, 10_000L, 1, FACTORY, List.of(a, b, c, d, e), Map.of());
        for (String m : List.of(a, b, c, d, e)) {
            use(t, 1, m);
        }

        settlement.settleMonth(project, MONTH);

        assertThat(item(a, "by_rank_tier")).isEqualTo(2_500L);
        assertThat(item(b, "by_rank_tier")).isEqualTo(2_000L);
        assertThat(item(c, "by_rank_tier")).isEqualTo(2_000L);
        assertThat(item(d, "by_rank_tier")).as("并列第 4：(20%+15%)÷2").isEqualTo(1_750L);
        assertThat(item(e, "by_rank_tier")).isEqualTo(1_750L);
        assertThat(((java.math.BigDecimal) units(t).get("1|1|" + d).get("pct"))).isEqualByComparingTo("17.5");
        assertThat(num(units(t).get("1|1|" + e), "rank_no")).as("并列各家共同占用第 4、5 名").isEqualTo(4L);
    }

    /** U-6 三家全并列：池 100.00 元各 33.33 元，1 分尾差单列 rounding_residual_cent，不塞给任何一家。 */
    @Test
    void threeWayTieLeavesOneCentInRoundingResidual() {
        String a = bar("a");
        String b = bar("b");
        String c = bar("c");
        String t = ticket("t1");
        snapshot(t, 10_000L, 1, FACTORY, List.of(a, b, c), Map.of());
        use(t, 1, a);
        use(t, 1, b);
        use(t, 1, c);

        settlement.settleMonth(project, MONTH);

        assertThat(item(a, "by_rank_tier")).isEqualTo(3_333L);
        assertThat(item(b, "by_rank_tier")).isEqualTo(3_333L);
        assertThat(item(c, "by_rank_tier")).isEqualTo(3_333L);
        assertThat(num(bill(), "rounding_residual_cent")).isEqualTo(1L);
    }

    /** U-7/U-8 减除额、未分配池余额、尾差三栏分列；减除额按域路由（酒吧域＝通票收入流归属主体，即娱乐平台公司）。 */
    @Test
    void deductionUndistributedAndResidualStayInThreeColumns() {
        String a = bar("a");
        String b = bar("b");
        String c = bar("c");
        String d = bar("d");
        String e = bar("e");
        String v = version(Instant.parse("2014-01-01T00:00:00Z"), 1, 2, 3);
        String t1 = ticket("t1");
        snapshot(t1, 10_000L, 1, v, List.of(a, b), Map.of(a, 3_000L, b, 3_000L));
        use(t1, 1, a);
        String t2 = ticket("t2");
        snapshot(t2, 10_000L, 1, FACTORY, List.of(c, d, e), Map.of());
        use(t2, 1, c);
        use(t2, 1, d);
        use(t2, 1, e);

        settlement.settleMonth(project, MONTH);

        Map<String, Object> bill = bill();
        assertThat(num(bill, "unverified_deduction_cent")).as("b 第一段未核销").isEqualTo(3_000L);
        assertThat(num(bill, "pool_undistributed_cent")).as("t1 无第二段参与者：余额 4000").isEqualTo(4_000L);
        assertThat(num(bill, "rounding_residual_cent")).as("t2 三家并列 1 分尾差").isEqualTo(1L);
        String platform = jdbc.queryForObject("""
                SELECT management_entity_id FROM revenue_stream_attribution
                 WHERE revenue_stream = 'all_pass_ticket' AND project_id IS NULL AND status = 'active'""", String.class);
        assertThat(bill.get("unverified_deduction_entity_id")).isEqualTo(platform);
        assertThat(bill.get("pool_undistributed_entity_id")).isEqualTo(platform);
    }

    /**
     * 18B 复核后裁决第 11 条「未分配池余额栏只放无第二段参与者的余额」（ENUM L52「该票从无第二段参与者时……余额单列为未分配池余额」）：
     * 单一酒吧门票（订单挂酒吧，不建池快照）的已核销票款按专属直付结付给订单酒吧（ENUM L32），不并进可分配池与未分配池余额。
     */
    @Test
    void undistributedColumnHoldsOnlyNoSecondSegmentRemainder() {
        String a = bar("a");
        String b = bar("b");
        String v = version(Instant.parse("2014-01-01T00:00:00Z"), 1, 2);
        String t = ticket("t1");
        snapshot(t, 10_000L, 1, v, List.of(a, b), Map.of(a, 3_000L, b, 3_000L));
        use(t, 1, a);
        use(t, 1, b);
        Timestamp sold = Timestamp.from(AT.minusSeconds(86_400L));
        jdbc.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, currency, pay_status,
                    issue_status, status, created_at, updated_at, project_id, paid_at, bar_id)
                VALUES (?, ?, ?, 1, 'app', 50000, 'CNY', 'paid', 'issued', 'created', ?, ?, ?, ?, ?)""",
                "o_ppe_nosnap_" + key, "o_ppe_nosnap_" + key, ticketType, sold, sold, project, sold, a);
        settled.onSettlementSettled(new SettlementSettledEvent("v_ppe_nosnap_" + key, AT, "ent_ppe_nosnap_" + key,
                "o_ppe_nosnap_" + key, ticketType, "entry", a, 1, 1, 1, 50_000L, AT.plusSeconds(86_400L), null, null, null,
                project, "tk_ppe_nosnap_" + key));

        settlement.settleMonth(project, MONTH);

        Map<String, Object> bill = bill();
        assertThat(num(bill, "pool_undistributed_cent")).as("只放 t1 无第二段参与者的余额 10000 − 6000").isEqualTo(4_000L);
        assertThat(num(bill, "available_pool_cent")).as("可分配池只含有池快照的份").isEqualTo(10_000L);
        // 夹具未走出票事实（无 FS-013 售出锁定），该笔综合服务费在核销行上计提，专属直付净额＝已核销额 − 该笔费。
        long fee = jdbc.queryForObject("SELECT comp_service_fee_cent FROM settlement_transactions WHERE verification_id = ?",
                Long.class, "v_ppe_nosnap_" + key);
        assertThat(item(a, "exclusive_full")).as("单一酒吧门票（无池快照）已核销 50000 结付给订单酒吧（ENUM L32）")
                .isEqualTo(50_000L - fee);
    }

    /** 名次按当期核销入场人数，赠酒等其他权益的核销不计名次。 */
    @Test
    void rankFollowsEntryPeopleNotGiftDrinkVerifications() {
        String a = bar("a");
        String b = bar("b");
        legacyEntries(a, 5, "gift_drink");
        legacyEntries(b, 1);
        String t = ticket("t1");
        snapshot(t, 10_000L, 1, FACTORY, List.of(a, b), Map.of());
        use(t, 1, a);
        use(t, 1, b);

        settlement.settleMonth(project, MONTH);

        assertThat(item(b, "by_rank_tier")).as("b 入场 2 人排第 1：57%").isEqualTo(5_700L);
        assertThat(item(a, "by_rank_tier")).as("a 入场 1 人排第 2：43%").isEqualTo(4_300L);
    }

    /** 双人票：每个人份各是一份完整票权益，各按份结算；第 2 人未进 b 吧，其份额减除。 */
    @Test
    void twoPersonTicketSettlesEachPersonAsOwnUnit() {
        String a = bar("a");
        String b = bar("b");
        String t = ticket("t1");
        snapshot(t, 10_000L, 2, FACTORY, List.of(a, b), Map.of());
        use(t, 1, a);
        use(t, 1, b);
        use(t, 2, a);

        settlement.settleMonth(project, MONTH);

        assertThat(item(a, "by_rank_tier")).as("两份各 5000 × 57%").isEqualTo(5_700L);
        assertThat(item(b, "by_rank_tier")).as("只拿第 1 人那份 5000 × 43%").isEqualTo(2_150L);
        assertThat(num(bill(), "unverified_deduction_cent")).isEqualTo(2_150L);
        assertThat(units(t)).containsKeys("1|1|" + a, "1|1|" + b, "2|1|" + a, "2|1|" + b);
    }

    /**
     * 缺该家数档位行：第二段暂挂、不出结果、不插值、不转未分配；补配行通告期未满时仍暂挂；
     * 生效后的月结按补配行与原月名次续算，另列续算明细，原月结算单不变。
     */
    @Test
    void missingParticipantRowSuspendsAndResumesAfterSupplementTakesEffect() {
        String a = bar("a");
        String b = bar("b");
        String c = bar("c");
        legacyEntries(a, 2);
        legacyEntries(b, 1);
        String v = version(Instant.parse("2014-01-01T00:00:00Z"), 1, 2);
        String t = ticket("t1");
        snapshot(t, 9_000L, 1, v, List.of(a, b, c), Map.of());
        use(t, 1, a);
        use(t, 1, b);
        use(t, 1, c);

        settlement.settleMonth(project, MONTH);

        Map<String, Object> march = bill();
        assertThat(num(march, "pool_suspended_cent")).isEqualTo(9_000L);
        assertThat(num(march, "pool_undistributed_cent")).as("不得转记未分配池余额").isZero();
        assertThat(itemTotal(a) + itemTotal(b) + itemTotal(c)).isZero();
        assertThat(units(t).values()).extracting(r -> r.get("status")).containsOnly("suspended");

        // 补配「参与 3 家」行：5 月才生效 → 4 月月结仍暂挂。
        version(Instant.parse("2015-05-05T00:00:00Z"), 3);
        settlement.settleMonth(project, MONTH.plusMonths(1));
        assertThat(item(MONTH.plusMonths(1), a, "pooled_resume")).isZero();

        settlement.settleMonth(project, MONTH.plusMonths(2));
        assertThat(item(MONTH.plusMonths(2), a, "pooled_resume")).as("按补配行与原月名次续算：9000 × 40%").isEqualTo(3_600L);
        assertThat(item(MONTH.plusMonths(2), b, "pooled_resume")).isEqualTo(2_700L);
        assertThat(item(MONTH.plusMonths(2), c, "pooled_resume")).isEqualTo(2_700L);
        assertThat(num(bill(), "pool_suspended_cent")).as("原月结算单不变").isEqualTo(9_000L);
    }

    /** BSM L807：已检票、权益未在该场次核销的酒吧份额已在池内按减除额处理，到期作业不再按场次终结未核销另记一遍。 */
    @Test
    void pooledBarEntitlementOfCheckedInSlotIsNotExpiredAgain() {
        String a = bar("a");
        String b = bar("b");
        String t = ticket("t1");
        String order = "o_" + t;
        Timestamp sold = Timestamp.from(AT.minusSeconds(86_400L));
        jdbc.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, currency, pay_status,
                    issue_status, status, created_at, updated_at, project_id, paid_at)
                VALUES (?, ?, ?, 1, 'app', 10000, 'CNY', 'paid', 'issued', 'created', ?, ?, ?, ?)""",
                order, order, ticketType, sold, sold, project, sold);
        jdbc.update("""
                INSERT INTO ticket_instances (id, ticket_no, order_id, ticket_type_id, seq_in_order, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 1, 'active', ?, ?)""", t, t, order, ticketType, sold, sold);
        snapshot(t, 10_000L, 1, FACTORY, List.of(a, b), Map.of());
        entitlement(t, 1, a, "entry", 1, 5_000L);
        String unused = entitlement(t, 1, b, "entry", 0, 5_000L);

        expiryJob.expireUnverified(Instant.now());

        assertThat(jdbc.queryForObject("""
                SELECT COALESCE(SUM(unverified_amount_cent), 0) FROM settlement_transactions
                 WHERE entitlement_id = ? AND transaction_type = 'unverified_expired'""", Long.class, unused))
                .as("该份额已在池内按通票未核销减除额处理，不再按场次终结未核销另记").isZero();
    }

    /**
     * 多次通票按营业日对齐各份（ENUM L52「一张票的第二段余额，由该票覆盖的酒吧中参加第二段的那几家……核销才拿」；ENUM L48「已完成检票环节
     * （刷码核销成功）、但权益未在该场次内核销的，按场次终结制作为未核销份额进入分账减除」）：2 次票覆盖 a、b，3 月第 1 次只去 a、
     * 4 月第 2 次只去 b——第 1 份 a 拿、b 减除，第 2 份 b 拿、a 减除；到期作业按有入场的营业日数算已开启 2 次，不再把第 2 次记成从未检票。
     */
    @Test
    void multiVisitPassAlignsEachVisitByBusinessDayAcrossMonths() {
        String a = bar("a");
        String b = bar("b");
        String t = ticket("t1");
        snapshot(t, 10_000L, 1, 2, 0L, "app", FACTORY, List.of(a, b), Map.of());
        instance(t, 1);
        String entryA = entitlement(t, 1, a, "entry", 2, 1, 5_000L);
        String entryB = entitlement(t, 1, b, "entry", 2, 1, 5_000L);
        settle(t, entryA, a, AT, 2, 1);
        settle(t, entryB, b, MONTH.plusMonths(1).atDay(12).atTime(21, 0).toInstant(ZoneOffset.UTC), 2, 1);

        settlement.settleMonth(project, MONTH);
        assertThat(item(b, "by_rank_tier")).as("b 第 1 次没去，3 月不拿").isZero();
        assertThat(item(a, "by_rank_tier")).as("第 1 份只有 a 入场：5000 × 57%").isEqualTo(2_850L);
        assertThat(num(bill(), "unverified_deduction_cent")).as("第 1 份 b 未核销：5000 × 43%").isEqualTo(2_150L);

        settlement.settleMonth(project, MONTH.plusMonths(1));
        assertThat(item(MONTH.plusMonths(1), b, "by_rank_tier")).as("第 2 份只有 b 入场：5000 × 57%").isEqualTo(2_850L);
        assertThat(item(MONTH.plusMonths(1), a, "by_rank_tier")).isZero();

        expiryJob.expireUnverified(Instant.now());
        assertThat(jdbc.queryForObject("""
                SELECT COALESCE(SUM(unverified_amount_cent), 0) FROM settlement_transactions
                 WHERE entitlement_id IN (?, ?) AND transaction_type = 'unverified_expired'""", Long.class, entryA, entryB))
                .as("两次入场都已开启，各份已在池内结算，到期不再按从未检票另记").isZero();
    }

    /**
     * 固定金额销售渠道费只扣一次且扣足（ENUM L48「仅扣实际发生的通路费，经第三方渠道售出的再扣实际发生的销售渠道费……各项均只扣一次」）：
     * 双人通票全为酒吧份额，快照里酒吧那部分销售渠道费 1000；第 1 人入场、第 2 人始终未到——第 2 人那份的 500 随到期未核销额扣。
     */
    @Test
    void fixedSalesFeeShareOfUnopenedPersonIsChargedAtExpiry() {
        String a = bar("a");
        String t = ticket("t1");
        jdbc.update("""
                INSERT INTO channel_fee_configs (id, project_id, channel, fee_bps, fixed_amount_cent, status, is_draft)
                VALUES (?, ?, 'meituan', 0, 1000, 'active', FALSE)""", "cfc_ppe_" + key, project);
        snapshot(t, 9_000L, 2, 1, 1_000L, "meituan", FACTORY, List.of(a), Map.of());
        instance(t, 2);
        String first = entitlement(t, 1, a, "entry", 1, 1, 5_000L);
        String second = entitlement(t, 2, a, "entry", 1, 0, 5_000L);
        settle(t, first, a, AT, 1, 1);

        expiryJob.expireUnverified(Instant.now());

        assertThat(jdbc.queryForObject("""
                SELECT COALESCE(SUM(channel_fee_cent + channel_fee_residual_cent), 0) FROM settlement_transactions
                 WHERE entitlement_id = ? AND transaction_type = 'unverified_expired'""", Long.class, second))
                .as("第 2 人那一份的快照销售渠道费 1000 ÷ 2").isEqualTo(500L);
    }

    // ─────────────────────────── 夹具 ───────────────────────────

    private String bar(String label) {
        return merchant(label, "bar");
    }

    private String merchant(String label, String format) {
        String id = "m_ppe_" + label + "_" + key;
        merchants.add(id);
        jdbc.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'active', now(), now())""", id, id, format, project);
        return id;
    }

    private String ticket(String label) {
        return "tk_ppe_" + label + "_" + key;
    }

    /** 售票时固化的池快照（夹具：毛额即池，各扣项为 0）与快照里的第一段名单。 */
    private void snapshot(String ticket, long pool, int slots, String version, List<String> covered,
                          Map<String, Long> firstSegmentFixed) {
        snapshot(ticket, pool, slots, 1, 0L, "app", version, covered, firstSegmentFixed);
    }

    /** 同上，另给每人入场次数、快照里酒吧那部分销售渠道费与订单销售渠道（酒吧毛额＝池＋销售渠道费）。 */
    private void snapshot(String ticket, long pool, int slots, int visits, long salesFee, String channel, String version,
                          List<String> covered, Map<String, Long> firstSegmentFixed) {
        Timestamp sold = Timestamp.from(AT.minusSeconds(86_400L));
        jdbc.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, currency, pay_status,
                    issue_status, status, created_at, updated_at, project_id, paid_at)
                VALUES (?, ?, ?, 1, ?, ?, 'CNY', 'paid', 'issued', 'created', ?, ?, ?, ?)
                ON CONFLICT (id) DO NOTHING""", "o_" + ticket, "o_" + ticket, ticketType, channel, pool + salesFee, sold, sold,
                project, sold);
        jdbc.update("""
                INSERT INTO pass_pool_snapshots (ticket_instance_id, order_id, project_id, ticket_type_id, slots, visits,
                    bar_gross_cent, route_fee_cent, sales_channel_fee_cent, gift_cost_cent, service_fee_cent, pool_cent,
                    scheme_version_id, covered_bar_ids)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, 0, 0, ?, ?, CAST(? AS text[]))""",
                ticket, "o_" + ticket, project, ticketType, slots, visits, pool + salesFee, salesFee, pool, version,
                "{" + String.join(",", covered) + "}");
        firstSegmentFixed.forEach((merchant, amount) -> jdbc.update("""
                INSERT INTO pass_pool_snapshot_first_segment (ticket_instance_id, merchant_id, rule_type, amount_cent)
                VALUES (?, ?, 'fixed', ?)""", ticket, merchant, amount));
    }

    /** 本项目一版已通告的分账方案，档位行照出厂表抄所列家数行。 */
    private String version(Instant effectiveAt, int... participantCounts) {
        String id = "tdsv_ppe_" + key + "_" + (++seq);
        jdbc.update("""
                INSERT INTO ticket_distribution_scheme_versions (id, project_id, status, notified_at, effective_at)
                VALUES (?, ?, 'notified', ?, ?)""", id, project, Timestamp.from(effectiveAt.minusSeconds(86_400L * 31)),
                Timestamp.from(effectiveAt));
        for (int n : participantCounts) {
            jdbc.update("""
                    INSERT INTO ticket_distribution_participant_tiers (version_id, participant_count, rank_no, share_bps)
                    SELECT ?, participant_count, rank_no, share_bps FROM ticket_distribution_participant_tiers
                     WHERE version_id = ? AND participant_count = ?""", id, FACTORY, n);
        }
        return id;
    }

    private String entitlement(String ticket, int slot, String merchant, String type, int used, long allocated) {
        return entitlement(ticket, slot, merchant, type, 1, used, allocated);
    }

    private String entitlement(String ticket, int slot, String merchant, String type, int total, int used, long allocated) {
        String id = "ent_ppe_" + (++seq) + "_" + key;
        Timestamp from = Timestamp.from(AT.minusSeconds(86_400L * 2));
        jdbc.update("""
                INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id, entitlement_code,
                    entitlement_type, merchant_id, total_times, used_times, status, valid_from, valid_to, created_at, updated_at,
                    allocated_revenue_cent, parent_ticket_id, slot_index)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'active', ?, ?, now(), now(), ?, ?, ?)""",
                id, ticketType, "o_" + ticket, template, id, type, merchant, total, used, from,
                Timestamp.from(AT.plusSeconds(86_400L)), allocated, ticket, slot);
        return id;
    }

    /** 父票实例（到期作业只扫父票仍有效的权益）。 */
    private void instance(String ticket, int people) {
        Timestamp sold = Timestamp.from(AT.minusSeconds(86_400L));
        jdbc.update("""
                INSERT INTO ticket_instances (id, ticket_no, order_id, ticket_type_id, seq_in_order, num_people, status, created_at,
                    updated_at) VALUES (?, ?, ?, ?, 1, ?, 'active', ?, ?)""", ticket, ticket, "o_" + ticket, ticketType, people,
                sold, sold);
    }

    /** 某人份用自己那条入场权益在 at 核销第 used 次（金额 0：引擎从快照取钱）。 */
    private void settle(String ticket, String entitlement, String merchant, Instant at, int total, int used) {
        settled.onSettlementSettled(new SettlementSettledEvent("v_ppe_" + (++seq) + "_" + key, at, entitlement, "o_" + ticket,
                ticketType, "entry", merchant, total, used, 1, 0L, at.plusSeconds(86_400L), null, null, null, project, ticket));
    }

    /** 池快照票某人份在某家的一次核销（金额 0：引擎从快照取钱，不看核销额）。 */
    private void use(String ticket, int slot, String merchant) {
        use(ticket, slot, merchant, "entry");
    }

    private void use(String ticket, int slot, String merchant, String type) {
        String ent = entitlement(ticket, slot, merchant, type, 1, 0L);
        settled.onSettlementSettled(new SettlementSettledEvent("v_ppe_" + (++seq) + "_" + key, AT, ent, "o_" + ticket,
                ticketType, type, merchant, 1, 1, 1, 0L, AT.plusSeconds(86_400L), null, null, null, project, ticket));
    }

    /** 金额为 0 的当期核销（无池快照），只用来调出各家当期入场人数名次。 */
    private void legacyEntries(String merchant, int n) {
        legacyEntries(merchant, n, "entry");
    }

    private void legacyEntries(String merchant, int n, String type) {
        for (int i = 0; i < n; i++) {
            int k = ++seq;
            settled.onSettlementSettled(new SettlementSettledEvent("v_ppe_lg_" + k + "_" + key, AT.minusSeconds(3_600L),
                    "ent_ppe_lg_" + k + "_" + key, "o_ppe_lg_" + key, ticketType, type, merchant, 1, 1, 1, 0L,
                    AT.plusSeconds(86_400L), null, null, null, project, null));
        }
    }

    private Map<String, Object> bill() {
        return jdbc.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ? AND period_start = ?",
                project, java.sql.Date.valueOf(MONTH.atDay(1)));
    }

    private long item(String merchant, String method) {
        return item(MONTH, merchant, method);
    }

    private long item(YearMonth month, String merchant, String method) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(i.final_amount_cent), 0) FROM settlement_items i JOIN settlement_bills b ON b.id = i.bill_id
                 WHERE b.project_id = ? AND b.period_start = ? AND i.merchant_id = ? AND i.distribution_method = ?""",
                Long.class, project, java.sql.Date.valueOf(month.atDay(1)), merchant, method);
    }

    private long itemTotal(String merchant) {
        return jdbc.queryForObject("""
                SELECT COALESCE(SUM(i.final_amount_cent), 0) FROM settlement_items i JOIN settlement_bills b ON b.id = i.bill_id
                 WHERE b.project_id = ? AND b.period_start = ? AND i.merchant_id = ?""",
                Long.class, project, java.sql.Date.valueOf(MONTH.atDay(1)), merchant);
    }

    /** 本月单该票逐份结果：键＝人份|第几次入场|酒吧。 */
    private Map<String, Map<String, Object>> units(String ticket) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbc.queryForList("""
                SELECT r.* FROM pass_pool_unit_results r JOIN settlement_bills b ON b.id = r.bill_id
                 WHERE b.project_id = ? AND b.period_start = ? AND r.ticket_instance_id = ?
                 ORDER BY r.slot_index, r.visit_no, r.merchant_id""", project, java.sql.Date.valueOf(MONTH.atDay(1)), ticket)) {
            out.put(row.get("slot_index") + "|" + row.get("visit_no") + "|" + row.get("merchant_id"), row);
        }
        return out;
    }

    private static long num(Map<String, Object> row, String column) {
        return ((Number) row.get(column)).longValue();
    }
}
