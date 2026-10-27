package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.ticketing.SettlementSettledEvent;
import com.complex.platform.ticketing.VerificationReversedEvent;
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
 * 第十八批第 3 条：通票分账渠道费分两层——通路费与销售渠道费。
 *
 * <p>依据：BSM L749「通路费（银行、微信、支付宝等支付通路收取）所有支付都会产生、官方渠道同样产生；销售渠道费（第三方售票平台佣金）
 * 只有经第三方渠道售出才产生，在通路费之上再扣一层。两者一律在拆分给各域之前从整单扣除且只扣一次，由各域按分配份额自然共担……
 * 官方渠道售出的票不扣销售渠道费，但仍扣通路费」；ENUM L52 第一步。
 *
 * <p>付款时按支付通路固化的通路费由夹具直接写 ticket_payment_route_fees（固化本身见 TicketRouteFeeFixationJdbcIntegrationTests），
 * 核销走真实结算消费者，月结走真实引擎。非 @Transactional（消费者 REQUIRES_NEW、月结真提交），@AfterEach 按项目清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementRouteFeeTwoLayerTests {

    private static final YearMonth MONTH = YearMonth.of(2014, 7); // 远离今日的确定月
    private static final Instant AT = MONTH.atDay(12).atTime(21, 0).toInstant(ZoneOffset.UTC);

    @Autowired private SettlementSettledConsumer settledConsumer;
    @Autowired private SettlementReversedConsumer reversedConsumer;
    @Autowired private TicketDistributionSettlementService settlementService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RouteFeeShares routeFeeShares;

    private final List<String> projectIds = new ArrayList<>();
    private final List<String> merchantIds = new ArrayList<>();
    private final List<String> orderIds = new ArrayList<>();
    private final List<String> ticketTypeIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void cleanup() {
        com.complex.platform.support.TestSecurityActors.clear();
        for (String pid : projectIds) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbc,
                    "DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            com.complex.platform.support.AppendOnlyTestBypass.updateOnTable(jdbc,
                    "pass_gift_cost_variance_ledger", "trg_append_only_ud",
                    "DELETE FROM pass_gift_cost_variance_ledger WHERE project_id = ?", pid);
            for (String table : List.of("settlement_items", "settlement_bill_format_shares")) {
                jdbc.update("DELETE FROM " + table + " WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)", pid);
            }
            jdbc.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
            jdbc.update("DELETE FROM ticket_distribution_rank_tiers WHERE project_id = ?", pid);
            jdbc.update("DELETE FROM channel_fee_configs WHERE project_id = ?", pid);
        }
        for (String oid : orderIds) {
            jdbc.update("DELETE FROM ticket_payment_route_fees WHERE order_id = ?", oid);
            jdbc.update("DELETE FROM ticket_order_channel_fees WHERE order_id = ?", oid);
            com.complex.platform.support.AppendOnlyTestBypass.updateOnTable(jdbc, "ticket_payment_intents", "trg_append_only_d",
                    "DELETE FROM ticket_payment_intents WHERE business_order_id = ?", oid);
            jdbc.update("DELETE FROM ticket_orders WHERE id = ?", oid);
        }
        for (String tid : ticketTypeIds) {
            jdbc.update("DELETE FROM ticket_types WHERE id = ?", tid);
        }
        for (String mid : merchantIds) {
            jdbc.update("DELETE FROM merchants WHERE id = ?", mid);
        }
        for (String pid : projectIds) {
            jdbc.update("DELETE FROM projects WHERE id = ?", pid);
        }
    }

    /** 官方渠道（app）微信付清 10000：通路费 60 照扣、通用销售渠道费 500bps 不扣；通路费在拆份额前扣，落酒吧那一份。 */
    @Test
    void officialChannelDeductsRouteFeeButNoSalesFee() {
        String pid = project();
        salesFee(pid, null, 500);
        String bar = merchant("bar");
        String order = order(pid, "app", 10000L);
        paid(order, pid, "wechat", 10000L, 60L);

        verify(pid, order, bar, "v1", 10000L, "tk1");
        String billId = settlementService.settleMonth(pid, MONTH);

        Map<String, Object> b = bill(pid);
        assertThat(num(b, "channel_fee_cent")).as("官方渠道不扣销售渠道费（通用行也不落到官方渠道）").isZero();
        assertThat(num(b, "route_fee_cent")).as("官方渠道仍扣通路费").isEqualTo(60L);
        Map<String, Object> barShare = shares(billId).get("bar");
        assertThat(num(barShare, "route_fee_cent")).isEqualTo(60L);
        assertThat(num(barShare, "net_revenue_cent")).as("净份额＝毛额−销售渠道费−通路费−G−综合服务费")
                .isEqualTo(10000L - 60L - num(barShare, "gift_cost_cent") - num(barShare, "comp_service_fee_cent"));
    }

    /** 第三方渠道（meituan）：通路费 30 之上再扣一层销售渠道费 1000bps＝1000，两层分列。 */
    @Test
    void thirdPartyChannelDeductsSalesFeeOnTopOfRouteFee() {
        String pid = project();
        salesFee(pid, "meituan", 1000);
        String bar = merchant("bar");
        String order = order(pid, "meituan", 10000L);
        paid(order, pid, "meituan", 10000L, 30L);

        verify(pid, order, bar, "v1", 10000L, "tk1");
        String billId = settlementService.settleMonth(pid, MONTH);

        Map<String, Object> b = bill(pid);
        assertThat(num(b, "route_fee_cent")).isEqualTo(30L);
        assertThat(num(b, "channel_fee_cent")).isEqualTo(1000L);
        Map<String, Object> barShare = shares(billId).get("bar");
        assertThat(num(barShare, "route_fee_cent")).isEqualTo(30L);
        assertThat(num(barShare, "channel_fee_cent")).isEqualTo(1000L);
    }

    /** 跨域组合通票酒吧 6000／游乐 4000：整单通路费 100 按份额 60／40 落两业态，Σ＝整单。 */
    @Test
    void crossDomainRouteFeeSharedByFormatShares() {
        String pid = project();
        String bar = merchant("bar");
        String amusement = merchant("amusement");
        String order = order(pid, "miniapp", 10000L);
        paid(order, pid, "alipay", 10000L, 100L);

        verify(pid, order, bar, "v1", 6000L, "tk1");
        verify(pid, order, amusement, "v2", 4000L, "tk1");
        String billId = settlementService.settleMonth(pid, MONTH);

        Map<String, Map<String, Object>> shares = shares(billId);
        assertThat(num(shares.get("bar"), "route_fee_cent")).isEqualTo(60L);
        assertThat(num(shares.get("amusement"), "route_fee_cent")).isEqualTo(40L);
        assertThat(num(bill(pid), "route_fee_cent")).isEqualTo(100L);
        assertThat(num(shares.get("amusement"), "net_revenue_cent")).isEqualTo(4000L - 40L);
    }

    /**
     * BSM L749「两者一律在拆分给各域之前从整单扣除且只扣一次……每笔分摊须记录所用合同版本」：付款时按美团 10% 固化销售渠道费配置，
     * 之后该渠道改配 20%，核销当月月结仍按售出时的 10% 扣。
     */
    @Test
    void salesChannelFeeStaysAtTheRateFixedAtPayment() {
        String pid = project();
        salesFee(pid, "meituan", 1000);
        String bar = merchant("bar");
        String order = order(pid, "meituan", 10000L);
        paid(order, pid, "meituan", 10000L, 0L);
        jdbc.update("UPDATE channel_fee_configs SET fee_bps = 2000 WHERE project_id = ? AND channel = 'meituan'", pid);

        verify(pid, order, bar, "v1", 10000L, "tk1");
        settlementService.settleMonth(pid, MONTH);

        assertThat(num(bill(pid), "channel_fee_cent")).as("按售出时固化的 10%").isEqualTo(1000L);
        assertThat(jdbc.queryForObject("SELECT sales_fee_bps FROM ticket_order_channel_fees WHERE order_id = ?", Integer.class, order))
                .isEqualTo(1000);
    }

    /** 冲正取原行之反：同月核销又冲正，通路费净 0，守恒自检不抛。 */
    @Test
    void reversalNegatesRouteFee() {
        String pid = project();
        String bar = merchant("bar");
        String order = order(pid, "app", 10000L);
        paid(order, pid, "wechat", 10000L, 60L);
        verify(pid, order, bar, "v1", 10000L, "tk1");

        reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                "v1_" + pid, "rv1_" + pid, "ent_v1_" + pid, order, bar, 1));
        assertThat(jdbc.queryForObject("SELECT route_fee_cent FROM settlement_transactions WHERE transaction_no = ?",
                Long.class, "ST-VR-rv1_" + pid)).isEqualTo(-60L);

        settlementService.settleMonth(pid, MONTH);
        assertThat(num(bill(pid), "route_fee_cent")).isZero();
    }

    /**
     * BSM L749「两者一律在拆分给各域之前从整单扣除且只扣一次」、BSM L1757「多方分配产生的非涉税尾差单独留痕」：一单实付 1000、通路费 5，
     * 三笔核销 333／333／334——逐笔各自四舍五入会扣成 2＋2＋2＝6；按本单累计额差分摊，三笔合计恰为固化的 5。
     */
    @Test
    void routeFeeSharesOfOneOrderAddUpToTheFixedFee() {
        String pid = project();
        String amusement = merchant("amusement");
        String order = order(pid, "app", 1000L);
        paid(order, pid, "wechat", 1000L, 5L);

        verify(pid, order, amusement, "v1", 333L, "tk1");
        verify(pid, order, amusement, "v2", 333L, "tk1");
        verify(pid, order, amusement, "v3", 334L, "tk1");

        assertThat(jdbc.queryForObject("""
                SELECT SUM(route_fee_cent) FROM settlement_transactions
                 WHERE order_id = ? AND transaction_type = 'verification_settled'""", Long.class, order))
                .as("各笔分摊合计＝本单固化的通路费").isEqualTo(5L);
        settlementService.settleMonth(pid, MONTH);
        assertThat(num(bill(pid), "route_fee_cent")).isEqualTo(5L);
    }

    // ─────────────────────────── 夹具 ───────────────────────────

    private String project() {
        String pid = "prj_rf_" + Long.toHexString(System.nanoTime());
        projectIds.add(pid);
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                pid, pid);
        jdbc.update("""
                INSERT INTO ticket_distribution_rank_tiers (id, project_id, rank_from, rank_to, share_bps, status, is_draft)
                VALUES (?, ?, 1, 9, 10000, 'active', FALSE)""", "tdrt_" + pid, pid);
        return pid;
    }

    private String merchant(String format) {
        String id = "m_rf_" + format + "_" + Long.toHexString(System.nanoTime());
        merchantIds.add(id);
        jdbc.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at)
                VALUES (?, ?, ?, 'active', now(), now())""", id, id, format);
        return id;
    }

    private void salesFee(String pid, String channel, int bps) {
        jdbc.update("""
                INSERT INTO channel_fee_configs (id, project_id, channel, fee_bps, status, is_draft)
                VALUES (?, ?, ?, ?, 'active', FALSE)""", "cfc_rf_" + Long.toHexString(System.nanoTime()), pid, channel, bps);
    }

    private String order(String pid, String salesChannel, long amountCent) {
        String type = "tt_rf_" + Long.toHexString(System.nanoTime());
        ticketTypeIds.add(type);
        jdbc.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES (?, '两层渠道费测试票种', 'fixed_price', FALSE, 'active', now(), now())""", type);
        String id = "ord_rf_" + Long.toHexString(System.nanoTime());
        orderIds.add(id);
        jdbc.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, currency,
                    pay_status, issue_status, status, created_at, updated_at, project_id)
                VALUES (?, ?, ?, 1, ?, ?, 'CNY', 'paid', 'issued', 'created', now(), now(), ?)""",
                id, "ORD-" + id, type, salesChannel, amountCent, pid);
        return id;
    }

    /** 一笔已成功付款及其付款时按支付通路固化的通路费；本单销售渠道费所用配置同时固化（同真实付款链）。 */
    private void paid(String order, String pid, String paymentChannel, long amountCent, long routeFeeCent) {
        String pay = "pi_" + order;
        jdbc.update("""
                INSERT INTO ticket_payment_intents (id, business_order_type, business_order_id, amount_cent, currency, channel,
                    status, created_at, updated_at) VALUES (?, 'ticket_order', ?, ?, 'CNY', ?, 'succeeded', now(), now())""",
                pay, order, amountCent, paymentChannel);
        jdbc.update("""
                INSERT INTO ticket_payment_route_fees (payment_intent_id, order_id, project_id, payment_channel, amount_cent, fee_cent)
                VALUES (?, ?, ?, ?, ?, ?)""", pay, order, pid, paymentChannel, amountCent, routeFeeCent);
        routeFeeShares.fixSalesChannelFee(order);
    }

    private void verify(String pid, String order, String merchant, String v, long allocatedCent, String ticket) {
        String type = jdbc.queryForObject("SELECT ticket_type_id FROM ticket_orders WHERE id = ?", String.class, order);
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                v + "_" + pid, AT, "ent_" + v + "_" + pid, order, type, "entry", merchant,
                1, 1, 1, allocatedCent, AT.plusSeconds(86_400), null, null, null, pid, ticket + "_" + pid));
    }

    private Map<String, Object> bill(String pid) {
        return jdbc.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ?", pid);
    }

    private Map<String, Map<String, Object>> shares(String billId) {
        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (Map<String, Object> row : jdbc.queryForList(
                "SELECT * FROM settlement_bill_format_shares WHERE bill_id = ?", billId)) {
            out.put((String) row.get("business_format"), row);
        }
        return out;
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
