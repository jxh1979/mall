package com.complex.platform.ticketing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.ticketing.dto.ConfirmTicketOrderPaymentRequest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批第 3 条：票务订单每笔付款成功时按实际支付通路固化通路费。
 *
 * <p>依据：BSM L749「通路费（银行、微信、支付宝等支付通路收取）所有支付都会产生、官方渠道同样产生……每笔分摊须记录所用合同版本」；
 * 操作员长期原话「一个订单可分多个渠道支付，但最终必须完成一次支付才算完成」——每一笔按各自通路固化。
 * 真实 Flyway 库，整类事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@Transactional
class TicketRouteFeeFixationJdbcIntegrationTests {

    @Autowired private TicketOrderService orderService;
    @Autowired private JdbcTemplate jdbc;

    private String k;
    private String project;
    private String routeCfg;

    @BeforeEach
    void seed() {
        k = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        project = "prj_rff_" + k;
        routeCfg = "cfc_rff_" + k;
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, project);
        jdbc.update("""
                INSERT INTO channel_fee_configs (id, project_id, fee_layer, channel, fee_bps, status, is_draft)
                VALUES (?, ?, 'route', 'wechat', 60, 'active', FALSE)""", routeCfg, project);
    }

    /** 官方渠道订单微信付清 10000：固化一行通路费 60 分、记所用配置；付款成功后改费率，已固化行不变。 */
    @Test
    void confirmedPaymentFixesRouteFeeByPaymentChannel() {
        String order = order("app", 10000L);
        String pay = intent(order, "wechat", 10000L);

        orderService.confirmPayment(order, new ConfirmTicketOrderPaymentRequest("rff1_" + k, pay, null));

        assertThat(fixedRows(pay)).as("受理的这一笔固化一行通路费").isEqualTo(1);
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM ticket_payment_route_fees WHERE payment_intent_id = ?", pay);
        assertThat(((Number) row.get("fee_cent")).longValue()).isEqualTo(60L);
        assertThat(row.get("fee_bps")).isEqualTo(60);
        assertThat(row.get("config_id")).as("记所用通路费配置行").isEqualTo(routeCfg);
        assertThat(row.get("payment_channel")).isEqualTo("wechat");
        assertThat(row.get("order_id")).isEqualTo(order);

        jdbc.update("UPDATE channel_fee_configs SET fee_bps = 100 WHERE id = ?", routeCfg);
        orderService.confirmPayment(order, new ConfirmTicketOrderPaymentRequest("rff2_" + k, pay, null));
        assertThat(jdbc.queryForObject("SELECT fee_cent FROM ticket_payment_route_fees WHERE payment_intent_id = ?",
                Long.class, pay)).as("已固化的不随费率改动").isEqualTo(60L);
    }

    /** 一单分两笔：储值先付 4000（储值笔由储值域固化），微信补 6000 → 微信笔按 60bps 固化 36 分。 */
    @Test
    void eachPaymentOfSplitOrderFixedByItsOwnChannel() {
        String order = order("miniapp", 10000L);
        jdbc.update("""
                INSERT INTO ticket_payment_intents (id, business_order_type, business_order_id, amount_cent, currency, channel,
                    status, created_at, updated_at) VALUES (?, 'ticket_order', ?, 4000, 'CNY', 'stored_value', 'succeeded', now(), now())""",
                "svpay_" + k, order);
        String pay = intent(order, "wechat", 6000L);

        orderService.confirmPayment(order, new ConfirmTicketOrderPaymentRequest("rff3_" + k, pay, null));

        assertThat(fixedRows(pay)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT fee_cent FROM ticket_payment_route_fees WHERE payment_intent_id = ?",
                Long.class, pay)).isEqualTo(36L);
        assertThat(jdbc.queryForObject("SELECT pay_status FROM ticket_orders WHERE id = ?", String.class, order)).isEqualTo("paid");
    }

    /**
     * 第十八批第 3 条（BSM L749「每笔分摊须记录所用合同版本」）：第三方渠道（美团）订单付款成功时同时固化销售渠道费所用配置（比例 8%），
     * 之后改费率不回改；官方渠道订单不固化销售渠道费。
     */
    @Test
    void thirdPartyOrderPaymentFixesSalesChannelFeeConfig() {
        String salesCfg = "cfc_rffs_" + k;
        jdbc.update("""
                INSERT INTO channel_fee_configs (id, project_id, fee_layer, channel, fee_bps, status, is_draft)
                VALUES (?, ?, 'sales', 'meituan', 800, 'active', FALSE)""", salesCfg, project);
        String order = order("meituan", 10000L);
        String official = order("app", 10000L);

        orderService.confirmPayment(order, new ConfirmTicketOrderPaymentRequest("rff5_" + k, intent(order, "meituan", 10000L), null));
        orderService.confirmPayment(official, new ConfirmTicketOrderPaymentRequest("rff6_" + k, intent(official, "wechat", 10000L), null));
        jdbc.update("UPDATE channel_fee_configs SET fee_bps = 1500 WHERE id = ?", salesCfg);

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM ticket_order_channel_fees WHERE order_id = ?", order);
        assertThat(row.get("sales_fee_config_id")).as("记付款时所用销售渠道费配置").isEqualTo(salesCfg);
        assertThat(row.get("sales_fee_bps")).as("付款后改费率不回改").isEqualTo(800);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_order_channel_fees WHERE order_id = ? AND sales_fee_config_id IS NOT NULL",
                Integer.class, official)).as("官方渠道不扣销售渠道费").isZero();
    }

    /** 拒收（与待付不符）的到账转原路待退，不固化通路费。 */
    @Test
    void rejectedPaymentIsNotFixed() {
        String order = order("app", 10000L);
        String pay = intent(order, "wechat", 9000L);

        orderService.confirmPayment(order, new ConfirmTicketOrderPaymentRequest("rff4_" + k, pay, null));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM ticket_payment_route_fees WHERE order_id = ?",
                Integer.class, order)).isZero();
    }

    private int fixedRows(String pay) {
        return jdbc.queryForObject("SELECT count(*) FROM ticket_payment_route_fees WHERE payment_intent_id = ?", Integer.class, pay);
    }

    private String order(String salesChannel, long amountCent) {
        String type = "tt_rff_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES (?, '通路费固化测试票种', 'fixed_price', FALSE, 'active', now(), now())""", type);
        String id = "ord_rff_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, currency,
                    pay_status, issue_status, status, created_at, updated_at, project_id)
                VALUES (?, ?, ?, 1, ?, ?, 'CNY', 'unpaid', 'unissued', 'created', now(), now(), ?)""",
                id, "ORD-" + id, type, salesChannel, amountCent, project);
        return id;
    }

    private String intent(String order, String channel, long amountCent) {
        String id = "pi_rff_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO ticket_payment_intents (id, business_order_type, business_order_id, amount_cent, currency, channel,
                    status, created_at, updated_at) VALUES (?, 'ticket_order', ?, ?, 'CNY', ?, 'created', now(), now())""",
                id, order, amountCent, channel);
        return id;
    }
}
