package com.complex.platform.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 结算B2「放弃权益额外收入」交叉表验收：直接对拍真实业务表（seed issued_entitlements + unverified_expired 归项目
 * settlement_transactions），调 {@link AnalyticsService#abandonedEntitlementRevenue} 逐格断言金额/笔数，
 * 覆盖：通票×酒水（多行求和）、单独门票×入场、通票×游乐、空格=0、日期窗外不计、非 unverified_expired 流水不计。
 *
 * <p>非 @Transactional（与 SettlementBatch1P345Tests 同）：service 审计写真提交，@AfterEach 按前缀清理落地行；
 * 用独立临时库 complex_test_b2rep 跑（勿碰共享 complex_test），用远离今天的确定日窗避免真实/并发数据混入。
 * 走真实 Flyway 迁移库；须串行跑（勿与其它 Flyway 集成测试并发）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class AbandonedEntitlementRevenueJdbcIntegrationTests {

    private static final String TICKET_TYPE = "tt_b2rep";
    private static final String TEMPLATE = "et_b2rep";
    private static final String ORDER_ID = "order_b2rep";
    private static final String BAR = "mrc_b2rep_bar";
    // 远离今天的确定单日窗 [D, D+1)，避开并发会话/真实数据落同窗。
    private static final LocalDate DAY = LocalDate.of(2011, 3, 7);

    @Autowired
    private AnalyticsService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.complex.platform.common.BusinessDayResolver businessDayResolver;

    private final List<String> entIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
        TestSecurityActors.clear();
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM settlement_transactions WHERE entitlement_id LIKE 'ent_b2rep_%'");
        jdbcTemplate.update("DELETE FROM issued_entitlements WHERE id LIKE 'ent_b2rep_%'");
        jdbcTemplate.update("DELETE FROM ticket_orders WHERE id = ?", ORDER_ID);
        jdbcTemplate.update("DELETE FROM entitlement_templates WHERE id = ?", TEMPLATE);
        jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", TICKET_TYPE);
        jdbcTemplate.update("DELETE FROM merchants WHERE id = ?", BAR);
        entIds.clear();
    }

    @Test
    void crossTabExactAmountsAndCountsByBucketAndTicketType() {
        seedBase();
        Instant inWin = businessDayResolver.startInstant(DAY).plusSeconds(3600);        // 落 [D, D+1) 内
        Instant outWin = businessDayResolver.startInstant(DAY.plusDays(1)).plusSeconds(3600); // 落 D+1（end 不含，须排除）

        // 通票×酒水：两行求和 → 7500 / 2 笔。第一行故意带 exclusive_merchant_id=BAR（结算专属标记）
        //   但 bar_id 仍为 NULL → 票种应判「通票」，证伪「误用 exclusive_merchant_id 判票种」。
        seedTxn("pd1", null, "gift_drink", 6000, inWin, BAR, "unverified_expired", "project");
        seedTxn("pd2", null, "drink", 1500, inWin, null, "unverified_expired", "project");
        // 单独门票×入场（bar_id=BAR）：3000 / 1 笔
        seedTxn("te1", BAR, "entry", 3000, inWin, null, "unverified_expired", "project");
        // 通票×游乐：2500 / 1 笔
        seedTxn("pa1", null, "amusement", 2500, inWin, null, "unverified_expired", "project");
        // 窗外单独门票×入场 99999：end 不含 → 必须排除
        seedTxn("teOut", BAR, "entry", 99999, outWin, null, "unverified_expired", "project");
        // 非 unverified_expired（核销结算归商户）大额：transaction_type 过滤 → 必须排除
        seedTxn("dec1", null, "entry", 88888, inWin, null, "verification_settled", "merchant");

        AnalyticsRecords.AbandonedEntitlementRevenueResponse resp =
                service.abandonedEntitlementRevenue(DAY, DAY.plusDays(1));

        assertThat(resp.start()).isEqualTo("2011-03-07");
        assertThat(resp.end()).isEqualTo("2011-03-08");
        // 固定五桶行序：酒水/入场/游乐/演出/其他。
        assertThat(resp.rows()).extracting(AnalyticsRecords.AbandonedEntitlementRow::bucket)
                .containsExactly("drink", "entry", "amusement", "show", "other");

        // 通票×酒水（多行求和逐格）
        AnalyticsRecords.AbandonedEntitlementRow drink = rowOf(resp, "drink");
        assertThat(drink.pass().amountCent()).isEqualTo(7500L);
        assertThat(drink.pass().count()).isEqualTo(2L);
        assertThat(drink.ticket().amountCent()).isZero();   // 空格=0
        assertThat(drink.ticket().count()).isZero();
        assertThat(drink.rowTotal().amountCent()).isEqualTo(7500L);
        assertThat(drink.rowTotal().count()).isEqualTo(2L);

        // 单独门票×入场（且证明窗外 99999 未混入）
        AnalyticsRecords.AbandonedEntitlementRow entry = rowOf(resp, "entry");
        assertThat(entry.ticket().amountCent()).isEqualTo(3000L);
        assertThat(entry.ticket().count()).isEqualTo(1L);
        assertThat(entry.pass().amountCent()).isZero();
        assertThat(entry.pass().count()).isZero();
        assertThat(entry.rowTotal().amountCent()).isEqualTo(3000L);

        // 通票×游乐
        AnalyticsRecords.AbandonedEntitlementRow amusement = rowOf(resp, "amusement");
        assertThat(amusement.pass().amountCent()).isEqualTo(2500L);
        assertThat(amusement.pass().count()).isEqualTo(1L);
        assertThat(amusement.ticket().amountCent()).isZero();

        // 空类型整行=0（演出/其他）
        AnalyticsRecords.AbandonedEntitlementRow show = rowOf(resp, "show");
        assertThat(show.rowTotal().amountCent()).isZero();
        assertThat(show.rowTotal().count()).isZero();
        AnalyticsRecords.AbandonedEntitlementRow other = rowOf(resp, "other");
        assertThat(other.rowTotal().amountCent()).isZero();
        assertThat(other.rowTotal().count()).isZero();

        // 列合计 / 总计（88888 核销行、99999 窗外行均不得计入）
        assertThat(resp.passTotal().amountCent()).isEqualTo(10000L);   // 7500 + 2500
        assertThat(resp.passTotal().count()).isEqualTo(3L);
        assertThat(resp.ticketTotal().amountCent()).isEqualTo(3000L);
        assertThat(resp.ticketTotal().count()).isEqualTo(1L);
        assertThat(resp.grandTotal().amountCent()).isEqualTo(13000L);
        assertThat(resp.grandTotal().count()).isEqualTo(4L);
    }

    /**
     * 补充边界(对抗审计 2026-07-21 建议):① start 边界含——business_at == startInstant(start) 的行被计入(左闭);
     * ② 未知 entitlement_type('meal')落 other 桶;③ business_at=NULL 时按 created_at 回退归窗(COALESCE 分支)。
     */
    @Test
    void startBoundaryInclusive_otherBucketHit_businessAtNullFallback() {
        seedBase();
        Instant startEdge = businessDayResolver.startInstant(DAY);          // 恰 start(左闭,须含)
        Instant inWin = startEdge.plusSeconds(3600);
        // ① start 边界含:通票×酒水 500 落 start 那一刻
        seedTxn("edge", null, "drink", 500, startEdge, null, "unverified_expired", "project");
        // ② 未知票种 'meal' → other 桶:单独门票 700
        seedTxn("meal", BAR, "meal", 700, inWin, null, "unverified_expired", "project");
        // ③ business_at=NULL、created_at 落窗 → 按 created_at 回退计入:通票×入场 300
        seedTxnNullBusinessAt("nb", "entry", 300, inWin);

        AnalyticsRecords.AbandonedEntitlementRevenueResponse resp =
                service.abandonedEntitlementRevenue(DAY, DAY.plusDays(1));

        assertThat(rowOf(resp, "drink").pass().amountCent()).isEqualTo(500L);   // ① 边界含
        AnalyticsRecords.AbandonedEntitlementRow other = rowOf(resp, "other");  // ② other 桶命中
        assertThat(other.ticket().amountCent()).isEqualTo(700L);
        assertThat(other.ticket().count()).isEqualTo(1L);
        assertThat(rowOf(resp, "entry").pass().amountCent()).isEqualTo(300L);   // ③ business_at=NULL 回退计入
        assertThat(resp.grandTotal().amountCent()).isEqualTo(1500L);            // 三条都在
        assertThat(resp.grandTotal().count()).isEqualTo(3L);
    }

    private static AnalyticsRecords.AbandonedEntitlementRow rowOf(
            AnalyticsRecords.AbandonedEntitlementRevenueResponse resp, String bucket) {
        return resp.rows().stream().filter(r -> r.bucket().equals(bucket)).findFirst().orElseThrow();
    }

    private void seedBase() {
        jdbcTemplate.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status,
                    created_at, updated_at)
                VALUES (?, 'B2 Type', 'fixed', FALSE, 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """, TICKET_TYPE);
        jdbcTemplate.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times,
                    default_value_cent, status, bar_item_ref, created_at, updated_at)
                VALUES (?, 'B2 Tmpl', 'entry', 1, 0, 'active', NULL, now(), now())
                ON CONFLICT (id) DO NOTHING
                """, TEMPLATE);
        jdbcTemplate.update("""
                INSERT INTO ticket_orders (id, order_no, customer_id, ticket_type_id, quantity, visit_date,
                    sales_channel, amount_cent, currency, pay_status, issue_status, status, created_at, updated_at)
                VALUES (?, ?, NULL, ?, 1, NULL, 'web', 0, 'CNY', 'paid', 'issued', 'created', now(), now())
                ON CONFLICT (id) DO NOTHING
                """, ORDER_ID, "ON-" + ORDER_ID, TICKET_TYPE);
        jdbcTemplate.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)
                VALUES (?, 'B2 Bar', 'bar', 'active', now(), now())
                ON CONFLICT (id) DO NOTHING
                """, BAR);
    }

    /** 直插一条权益 + 对应结算流水。barId=NULL → 通票；非空 → 单独门票（bar_id 判票种）。 */
    private void seedTxn(String suffix, String barId, String entitlementType, long unverifiedCent,
                         Instant businessAt, String exclusiveMerchantId, String transactionType,
                         String beneficiaryType) {
        String ent = "ent_b2rep_" + suffix;
        entIds.add(ent);
        jdbcTemplate.update("""
                INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id,
                    entitlement_code, entitlement_type, merchant_id, venue_id, point_id,
                    total_times, used_times, value_amount_cent, status, valid_from, valid_to,
                    created_at, updated_at, num_people, allocated_revenue_cent, bar_id)
                VALUES (?, ?, ?, ?, ?, ?, NULL, NULL, NULL, 4, 1, 0, 'active', now(), now(),
                    now(), now(), 1, ?, ?)
                """, ent, TICKET_TYPE, ORDER_ID, TEMPLATE, "code_b2rep_" + suffix, entitlementType,
                unverifiedCent, barId);
        java.sql.Timestamp bat = java.sql.Timestamp.from(businessAt);
        // beneficiary_type=merchant 的行给个 merchant_id（BAR）满足语义；project 行 merchant_id 置 NULL。
        String merchantId = "merchant".equals(beneficiaryType) ? BAR : null;
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    settlement_status, entitlement_valid_to, exclusive_merchant_id, business_at,
                    discount_amount_cent, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 1, 4, 1, ?, 0, ?, ?, ?, NULL, 'confirmed', ?, ?, ?, 0, ?, ?)
                """, "st_b2rep_" + suffix, "ST-B2REP-" + suffix, transactionType, ent, entitlementType,
                unverifiedCent, unverifiedCent, beneficiaryType, merchantId, bat, exclusiveMerchantId, bat, bat, bat);
    }

    /** business_at=NULL、created_at=给定时刻 的归项目行(通票),验证窗口 COALESCE(business_at,created_at) 回退分支。 */
    private void seedTxnNullBusinessAt(String suffix, String entitlementType, long unverifiedCent, Instant createdAt) {
        String ent = "ent_b2rep_" + suffix;
        entIds.add(ent);
        jdbcTemplate.update("""
                INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id,
                    entitlement_code, entitlement_type, merchant_id, venue_id, point_id,
                    total_times, used_times, value_amount_cent, status, valid_from, valid_to,
                    created_at, updated_at, num_people, allocated_revenue_cent, bar_id)
                VALUES (?, ?, ?, ?, ?, ?, NULL, NULL, NULL, 4, 1, 0, 'active', now(), now(),
                    now(), now(), 1, ?, NULL)
                """, ent, TICKET_TYPE, ORDER_ID, TEMPLATE, "code_b2rep_" + suffix, entitlementType, unverifiedCent);
        java.sql.Timestamp cat = java.sql.Timestamp.from(createdAt);
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    settlement_status, entitlement_valid_to, exclusive_merchant_id, business_at,
                    discount_amount_cent, created_at, updated_at)
                VALUES (?, ?, 'unverified_expired', ?, ?, 1, 4, 1, ?, 0, ?, 'project', NULL, NULL,
                    'confirmed', ?, NULL, NULL, 0, ?, ?)
                """, "st_b2rep_" + suffix, "ST-B2REP-" + suffix, ent, entitlementType,
                unverifiedCent, unverifiedCent, cat, cat, cat);
    }
}
