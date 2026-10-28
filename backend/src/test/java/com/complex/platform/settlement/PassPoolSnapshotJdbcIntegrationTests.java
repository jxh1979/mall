package com.complex.platform.settlement;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.AppendOnlyTestBypass;
import com.complex.platform.ticketing.SettlementSettledEvent;
import com.complex.platform.ticketing.TicketOrderService;
import com.complex.platform.ticketing.TicketRewardFacts;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 第十八批第 2 条：酒吧通票可分配池售票时逐张固化。
 *
 * <p>依据：ENUM L52「第一步从整单扣一次通路费，经第三方渠道售出的再从整单扣一次销售渠道费……第二步按各业态价格占比拆出各域份额……
 * 第三步仅对酒吧份额继续扣减——先扣赠送权益预估成本……再扣门票综合服务费……第四步，酒吧份额扣完上述两项后的剩余，即为酒吧通票可分配池，
 * 池金额在售票时随订单固化」「变更只对通告生效后新售出的票适用，已售出的票一律按售票时固化的第一段名单与固定额结算，不追溯」；
 * BSM L755「酒吧通票和跨域组合通票中的酒吧部分仍按原不封顶逐票规则处理」。
 *
 * <p>出票事实（{@link TicketRewardFacts.Issued}）与出票同一事务发布；夹具直接落订单、父票、权益与付款时固化的通路费后发布该事实。
 * 核销走真实结算消费者（REQUIRES_NEW），故本类非 @Transactional，@AfterEach 按项目清理。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class PassPoolSnapshotJdbcIntegrationTests {

    private static final Instant SOLD = Instant.parse("2016-05-10T04:00:00Z");

    @Autowired private ApplicationEventPublisher events;
    @Autowired private SettlementSettledConsumer settled;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RouteFeeShares routeFeeShares;
    @Autowired private TicketOrderService orderService;
    @Autowired private TicketDistributionSettlementService settlement;
    @Autowired private com.complex.platform.common.BusinessDayResolver businessDays;

    private final String key = Long.toHexString(System.nanoTime());
    private final String project = "prj_pps_" + key;
    private final String type = "tt_pps_" + key;
    private final List<String> merchants = new ArrayList<>();
    private final List<String> orders = new ArrayList<>();
    private final List<String> templates = new ArrayList<>();
    private String barA;
    private String barB;
    private String park;
    private int seq;

    @BeforeEach
    void setUp() {
        com.complex.platform.support.TestSecurityActors.asPlatformAdmin();
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, project);
        jdbc.update("""
                INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status, created_at, updated_at)
                VALUES (?, '池快照测试通票', 'fixed_price', FALSE, 'published', now(), now())""", type);
        barA = merchant("a", "bar");
        barB = merchant("b", "bar");
        park = merchant("park", "amusement");
        // 本项目本票种：赠送权益预估成本按票计 5 元，门票综合服务费 10%。
        jdbc.update("""
                INSERT INTO pass_gift_cost_configs (id, project_id, ticket_type_id, prededuct_mode, ticket_gift_amount_cent,
                    monthly_cap_amount_cent, status, is_draft, updated_by)
                VALUES (?, ?, ?, 'by_ticket_amount', 500, NULL, 'active', FALSE, 'tester')""", "pgc_pps_" + key, project, type);
        jdbc.update("""
                INSERT INTO comprehensive_service_fee_configs (id, project_id, merchant_id, ticket_type_id, fee_bps,
                    monthly_cap_amount_cent, status, is_draft)
                VALUES (?, ?, NULL, ?, 1000, NULL, 'active', FALSE)""", "csfc_pps_" + key, project, type);
    }

    @AfterEach
    void cleanup() {
        com.complex.platform.support.TestSecurityActors.clear();
        AppendOnlyTestBypass.update(jdbc, "DELETE FROM settlement_transactions WHERE project_id = ?", project);
        AppendOnlyTestBypass.updateOnTable(jdbc, "pass_gift_cost_variance_ledger", "trg_append_only_ud",
                "DELETE FROM pass_gift_cost_variance_ledger WHERE project_id = ?", project);
        for (String table : List.of("settlement_items", "settlement_bill_format_shares", "pass_pool_unit_results")) {
            jdbc.update("DELETE FROM " + table + " WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)", project);
        }
        jdbc.update("DELETE FROM settlement_bills WHERE project_id = ?", project);
        jdbc.update("DELETE FROM single_bar_service_fee_tickets WHERE month_id IN (SELECT id FROM single_bar_service_fee_months WHERE project_id = ?)",
                project);
        jdbc.update("DELETE FROM single_bar_service_fee_months WHERE project_id = ?", project);
        jdbc.update("DELETE FROM ticket_type_entitlements WHERE ticket_type_id = ?", type);
        jdbc.update("DELETE FROM pass_pool_snapshots WHERE project_id = ?", project);
        jdbc.update("DELETE FROM pass_incentive_applications WHERE project_id = ?", project);
        jdbc.update("DELETE FROM ticket_distribution_scheme_versions WHERE project_id = ?", project);
        for (String order : orders) {
            AppendOnlyTestBypass.updateOnTable(jdbc, "ticket_refund_lines", "trg_append_only_d",
                    "DELETE FROM ticket_refund_lines WHERE order_id = ?", order);
            AppendOnlyTestBypass.updateOnTable(jdbc, "ticket_refunds", "trg_append_only_d",
                    "DELETE FROM ticket_refunds WHERE target_id = ?", order);
            jdbc.update("DELETE FROM ticket_payment_route_fees WHERE order_id = ?", order);
            jdbc.update("DELETE FROM ticket_order_channel_fees WHERE order_id = ?", order);
            AppendOnlyTestBypass.updateOnTable(jdbc, "ticket_payment_intents", "trg_append_only_d",
                    "DELETE FROM ticket_payment_intents WHERE business_order_id = ?", order);
        }
        jdbc.update("DELETE FROM issued_entitlements WHERE ticket_type_id = ?", type);
        jdbc.update("DELETE FROM ticket_instances WHERE ticket_type_id = ?", type);
        jdbc.update("DELETE FROM ticket_orders WHERE ticket_type_id = ?", type);
        for (String t : templates) {
            jdbc.update("DELETE FROM entitlement_templates WHERE id = ?", t);
        }
        jdbc.update("DELETE FROM channel_fee_configs WHERE project_id = ?", project);
        jdbc.update("DELETE FROM pass_gift_cost_configs WHERE project_id = ?", project);
        jdbc.update("DELETE FROM comprehensive_service_fee_configs WHERE project_id = ?", project);
        jdbc.update("DELETE FROM ticket_types WHERE id = ?", type);
        for (String m : merchants) {
            jdbc.update("DELETE FROM merchants WHERE id = ?", m);
        }
        jdbc.update("DELETE FROM projects WHERE id = ?", project);
    }

    /**
     * 官方渠道（app）跨域组合通票 10000：酒吧 a 5000、b 3000、游乐 2000；微信付清、通路费 60。
     * 池＝酒吧份额 8000 − 通路费 48（按份额）− 销售渠道费 0（官方渠道）− 赠送权益预估成本 500 − 综合服务费 800（10%，不封顶）＝6652。
     */
    @Test
    void officialChannelPoolIsBarShareLessRouteFeeGiftCostAndServiceFee() {
        String order = order("app", SOLD);
        String ticket = ticketWithBarsAndPark(order);

        events.publishEvent(new TicketRewardFacts.Issued(order));

        assertThat(count(ticket)).as("售票即逐张固化池快照").isEqualTo(1);
        Map<String, Object> s = jdbc.queryForMap("SELECT * FROM pass_pool_snapshots WHERE ticket_instance_id = ?", ticket);
        assertThat(num(s, "bar_gross_cent")).isEqualTo(8_000L);
        assertThat(num(s, "route_fee_cent")).isEqualTo(48L);
        assertThat(num(s, "sales_channel_fee_cent")).isZero();
        assertThat(num(s, "gift_cost_cent")).isEqualTo(500L);
        assertThat(num(s, "service_fee_cent")).isEqualTo(800L);
        assertThat(num(s, "pool_cent")).isEqualTo(6_652L);
        assertThat(s.get("scheme_version_id")).isEqualTo("tdsv_factory");
        assertThat(covered(ticket)).containsExactlyInAnyOrder(barA, barB);
    }

    /** 第三方渠道（meituan，销售渠道费 10%）：在通路费之上再按酒吧份额扣一层销售渠道费 800。 */
    @Test
    void thirdPartyChannelAlsoDeductsSalesChannelFee() {
        jdbc.update("""
                INSERT INTO channel_fee_configs (id, project_id, channel, fee_bps, status, is_draft, fee_layer)
                VALUES (?, ?, 'meituan', 1000, 'active', FALSE, 'sales')""", "cfc_pps_" + key, project);
        String order = order("meituan", SOLD);
        String ticket = ticketWithBarsAndPark(order);

        events.publishEvent(new TicketRewardFacts.Issued(order));

        assertThat(count(ticket)).isEqualTo(1);
        Map<String, Object> s = jdbc.queryForMap("SELECT * FROM pass_pool_snapshots WHERE ticket_instance_id = ?", ticket);
        assertThat(num(s, "sales_channel_fee_cent")).isEqualTo(800L);
        assertThat(num(s, "pool_cent")).isEqualTo(8_000L - 48L - 800L - 500L - 800L);
    }

    /** 售票时刻生效的方案版本与其第一段名单随票固化；之后生效的新版只管之后售出的票。 */
    @Test
    void schemeVersionAndFirstSegmentAreFrozenAtSale() {
        String v1 = version(Instant.parse("2016-01-01T00:00:00Z"), barA, "fixed", 1_200L);
        String v2 = version(Instant.parse("2016-06-01T00:00:00Z"), barB, "per_guest", 900L);
        String early = order("app", SOLD);
        String t1 = ticketWithBarsAndPark(early);
        events.publishEvent(new TicketRewardFacts.Issued(early));
        String late = order("app", Instant.parse("2016-06-15T04:00:00Z"));
        String t2 = ticketWithBarsAndPark(late);
        events.publishEvent(new TicketRewardFacts.Issued(late));

        assertThat(count(t1)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT scheme_version_id FROM pass_pool_snapshots WHERE ticket_instance_id = ?",
                String.class, t1)).isEqualTo(v1);
        assertThat(jdbc.queryForList("SELECT merchant_id || ':' || rule_type || ':' || amount_cent FROM pass_pool_snapshot_first_segment"
                + " WHERE ticket_instance_id = ?", String.class, t1)).containsExactly(barA + ":fixed:1200");
        assertThat(jdbc.queryForObject("SELECT scheme_version_id FROM pass_pool_snapshots WHERE ticket_instance_id = ?",
                String.class, t2)).isEqualTo(v2);
        assertThat(jdbc.queryForList("SELECT merchant_id || ':' || rule_type || ':' || amount_cent FROM pass_pool_snapshot_first_segment"
                + " WHERE ticket_instance_id = ?", String.class, t2)).containsExactly(barB + ":per_guest:900");
    }

    /**
     * 18C 核对后操作员裁决补充第 12 条「整版回到此前方案（推荐）：到期那天起新卖的票改按此前在用的方案（本项目上一版，没有就用全平台版）；
     * 已卖的票照售票时固化的结果结算，不追溯」：本项目版 v2 凭短期经营激励转正、激励到期日 2016-06-01——到期日前售出的票
     * （含 6 月 1 日 10:00 前、营业日仍属 5 月 31 日的）按 v2 固化，到期营业日起售出的票改按此前在用的本项目上一版 v1；
     * 到期后才跑的 5 月月结，到期前售出的票仍按售票时固化的 v2 第一段结算。走出票事实链。
     */
    @Test
    void incentiveVersionStopsTakingNewSalesFromItsExpiryBusinessDay() {
        String v1 = version(Instant.parse("2016-01-01T00:00:00Z"), barA, "fixed", 1_200L);
        String v2 = version(Instant.parse("2016-04-01T02:00:00Z"), barB, "per_guest", 900L);
        approvedIncentive(v2, LocalDate.of(2016, 6, 1));
        String early = order("app", SOLD);
        String beforeCut = order("app", Instant.parse("2016-06-01T01:00:00Z"));
        String afterCut = order("app", Instant.parse("2016-06-01T02:30:00Z"));
        String t1 = ticketWithBarsAndPark(early);
        String t2 = ticketWithBarsAndPark(beforeCut);
        String t3 = ticketWithBarsAndPark(afterCut);
        for (String order : List.of(early, beforeCut, afterCut)) {
            events.publishEvent(new TicketRewardFacts.Issued(order));
        }

        assertThat(schemeOf(t1)).as("到期日前售出").isEqualTo(v2);
        assertThat(schemeOf(t2)).as("6 月 1 日 09:00 售出，营业日仍属 5 月 31 日").isEqualTo(v2);
        assertThat(schemeOf(t3)).as("到期营业日起售出，改按此前在用的本项目上一版").isEqualTo(v1);
        assertThat(jdbc.queryForList("SELECT merchant_id || ':' || rule_type || ':' || amount_cent FROM pass_pool_snapshot_first_segment"
                + " WHERE ticket_instance_id = ?", String.class, t3)).containsExactly(barA + ":fixed:1200");

        verify(early, t1, barB, "entry", 3_000L);
        String bill = settlement.settleMonth(project, YearMonth.of(2016, 5));

        assertThat(jdbc.queryForList("""
                SELECT merchant_id || ':' || amount_cent || ':' || scheme_version_id FROM pass_pool_unit_results
                 WHERE bill_id = ? AND ticket_instance_id = ? AND segment = 1""", String.class, bill, t1))
                .as("到期后月结，已售票照售票时固化的 v2 第一段结算，不追溯").containsExactly(barB + ":900:" + v2);
    }

    /** 单一酒吧门票（订单挂酒吧）按 FS-013 售出月逐票计费，不进通票可分配池。 */
    @Test
    void singleBarTicketGetsNoPoolSnapshot() {
        String order = order("app", SOLD);
        jdbc.update("UPDATE ticket_orders SET bar_id = ? WHERE id = ?", barA, order);
        String ticket = ticketWithBarsAndPark(order);

        events.publishEvent(new TicketRewardFacts.Issued(order));

        assertThat(count(ticket)).isZero();
    }

    /**
     * 单一酒吧门票按真实出票链（{@link TicketOrderService#issuePaidOrderAutomatically}）出票：后台通用权益模板不填商户与专属商户，
     * 出票时权益商户回退为订单酒吧、专属商户为空。核销后月结，已核销票款按专属直付结付给订单上的那家酒吧（ENUM L32「单一酒吧门票……
     * 票款由娱乐平台公司统一收款后结付给该酒吧商户」），不进可分配池与未分配池余额；综合服务费售出时已另行锁定（FS-013），不从票款里扣。
     */
    @Test
    void singleBarTicketVerifiedRevenueIsPaidDirectToOrderBar() {
        jdbc.update("UPDATE ticket_types SET bar_id = ? WHERE id = ?", barA, type);
        String template = "et_pps_" + (++seq) + "_" + key;
        templates.add(template);
        jdbc.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,
                    created_at, updated_at) VALUES (?, '单店入场', 'entry', 1, 0, 'active', now(), now())""", template);
        jdbc.update("""
                INSERT INTO ticket_type_entitlements (id, ticket_type_id, entitlement_template_id, quantity, total_times, num_people,
                    value_amount_cent, component_price_cent, created_at, updated_at)
                VALUES (?, ?, ?, 1, 1, 1, 0, 10000, now(), now())""", "tte_" + template, type, template);
        String order = order("app", SOLD);
        jdbc.update("UPDATE ticket_orders SET bar_id = ?, issue_status = 'not_issued' WHERE id = ?", barA, order);

        orderService.issuePaidOrderAutomatically(order);
        Map<String, Object> ent = jdbc.queryForMap("SELECT * FROM issued_entitlements WHERE order_id = ?", order);
        assertThat(ent.get("merchant_id")).as("通用模板：权益商户回退为订单酒吧").isEqualTo(barA);
        assertThat(ent.get("exclusive_merchant_id")).as("通用模板不填专属商户").isNull();
        settled.onSettlementSettled(new SettlementSettledEvent("v_pps_" + (++seq) + "_" + key, SOLD.plusSeconds(86_400L),
                (String) ent.get("id"), order, type, "entry", barA, 1, 1, 1, num(ent, "allocated_revenue_cent"),
                ((Timestamp) ent.get("valid_to")).toInstant(), null, null, null, project, (String) ent.get("parent_ticket_id")));

        String bill = settlement.settleMonth(project, YearMonth.of(2016, 5));

        assertThat(jdbc.queryForList("SELECT final_amount_cent FROM settlement_items WHERE bill_id = ? AND merchant_id = ?"
                        + " AND distribution_method = 'exclusive_full'", Long.class, bill, barA))
                .as("订单酒吧拿到已核销票款 10000").containsExactly(10_000L);
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM settlement_bills WHERE id = ?", bill);
        assertThat(num(row, "available_pool_cent")).as("不进可分配池").isZero();
        assertThat(num(row, "pool_undistributed_cent")).as("不进未分配池余额").isZero();
    }

    /**
     * 跨域组合票只送一杯酒吧赠饮、不含任何酒吧入场（订单不挂酒吧），按真实出票链出票：该票未覆盖任何第二段酒吧，不建池快照。
     * 核销后月结，该票酒吧业态净额按 ENUM L52「该票从无第二段参与者时（……或该票未覆盖任何符合条件的第二段酒吧），余额单列为未分配池余额
     * 并归娱乐平台公司」进未分配池余额（18B 复核后裁决第 11 条「未分配池余额栏只放无第二段参与者的余额」），各栏守恒。
     * 酒吧赠饮 2000：通路费 60 × 2000 ÷ 10000 ＝ 12、赠送权益预估成本 500（该票首笔进池核销）、综合服务费 10% ＝ 200 → 净额 1288。
     */
    @Test
    void comboTicketWithBarGiftButNoBarEntryPutsBarNetIntoUndistributedPool() {
        bind("gift_drink", barA, 2_000L);
        bind("amusement_entry", park, 8_000L);
        String order = order("app", SOLD);
        jdbc.update("UPDATE ticket_orders SET issue_status = 'not_issued' WHERE id = ?", order);

        orderService.issuePaidOrderAutomatically(order);
        String ticket = jdbc.queryForObject("SELECT id FROM ticket_instances WHERE order_id = ?", String.class, order);
        assertThat(count(ticket)).as("不含酒吧入场：不建池快照").isZero();
        for (String kind : List.of("gift_drink", "amusement_entry")) { // 赠饮先核销：G 随该票首笔进池核销落在酒吧业态
            Map<String, Object> ent = jdbc.queryForMap(
                    "SELECT * FROM issued_entitlements WHERE order_id = ? AND entitlement_type = ?", order, kind);
            settled.onSettlementSettled(new SettlementSettledEvent("v_pps_" + (++seq) + "_" + key, SOLD.plusSeconds(86_400L + seq),
                    (String) ent.get("id"), order, type, kind, (String) ent.get("merchant_id"), 1, 1, 1,
                    num(ent, "allocated_revenue_cent"), ((Timestamp) ent.get("valid_to")).toInstant(), null, null, null,
                    project, ticket));
        }

        String bill = settlement.settleMonth(project, YearMonth.of(2016, 5));

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM settlement_bills WHERE id = ?", bill);
        Map<String, Object> bar = jdbc.queryForMap(
                "SELECT * FROM settlement_bill_format_shares WHERE bill_id = ? AND business_format = 'bar'", bill);
        assertThat(num(bar, "net_revenue_cent")).as("酒吧业态净额 2000 − 12 − 500 − 200").isEqualTo(1_288L);
        assertThat(num(row, "pool_undistributed_cent")).as("酒吧业态净额单列未分配池余额").isEqualTo(1_288L);
        assertThat(row.get("pool_undistributed_entity_id")).as("归娱乐平台公司").isEqualTo("be_ent_platform");
        assertThat(num(row, "available_pool_cent")).as("可分配池＝酒吧业态净额").isEqualTo(1_288L);
        assertThat(num(row, "available_pool_cent") + num(row, "pool_overflow_cent"))
                .as("池 + 超池 ＝ 实分 + 减除 + 未分配 + 暂挂 + 尾差")
                .isEqualTo(num(row, "distributed_cent") + num(row, "unverified_deduction_cent")
                        + num(row, "pool_undistributed_cent") + num(row, "pool_suspended_cent") + num(row, "rounding_residual_cent"));
        long nets = jdbc.queryForObject(
                "SELECT SUM(net_revenue_cent) FROM settlement_bill_format_shares WHERE bill_id = ?", Long.class, bill);
        assertThat(nets + num(row, "channel_fee_cent") + num(row, "route_fee_cent") + num(row, "gift_cost_cent")
                + num(row, "comp_service_fee_pooled_cent"))
                .as("收入 ＝ Σ各业态净额 + 销售渠道费 + 通路费 + 赠送权益预估成本 + 综合服务费")
                .isEqualTo(num(row, "ticket_revenue_cent"))
                .isEqualTo(10_000L);
    }

    /** 有池快照的票：核销时不再逐笔计提综合服务费与赠送权益预估成本，酒吧核销也不再分摊通路费（快照里已扣，防双扣）；游乐核销照常分摊通路费。 */
    @Test
    void pooledTicketVerificationsDoNotAccrueServiceFeeGiftCostOrBarRouteFeeAgain() {
        String order = order("app", SOLD);
        String ticket = ticketWithBarsAndPark(order);
        events.publishEvent(new TicketRewardFacts.Issued(order));

        verify(order, ticket, barA, "entry", 5_000L);
        verify(order, ticket, park, "amusement_entry", 2_000L);

        Map<String, Object> bar = fact(order, barA);
        assertThat(num(bar, "comp_service_fee_cent")).isZero();
        assertThat(num(bar, "gift_cost_cent")).isZero();
        assertThat(num(bar, "route_fee_cent")).isZero();
        Map<String, Object> amusement = fact(order, park);
        assertThat(num(amusement, "gift_cost_cent")).isZero();
        assertThat(num(amusement, "route_fee_cent")).as("游乐份额照常按核销额分摊通路费：60 × 2000 ÷ 10000").isEqualTo(12L);
    }

    /** 整张退款成功：该票池快照作废。 */
    @Test
    void fullRefundVoidsThePoolSnapshot() {
        String order = order("app", SOLD);
        String ticket = ticketWithBarsAndPark(order);
        events.publishEvent(new TicketRewardFacts.Issued(order));
        String refund = "rf_pps_" + key;
        jdbc.update("""
                INSERT INTO ticket_refunds (id, target_id, refund_amount_cent, reason, refund_channel, status, requested_by,
                    created_at, updated_at) VALUES (?, ?, 10000, '客人使用前申请退票', 'original', 'success', 'tester', now(), now())""",
                refund, order);
        for (Map<String, Object> ent : jdbc.queryForList(
                "SELECT id, allocated_revenue_cent FROM issued_entitlements WHERE parent_ticket_id = ?", ticket)) {
            jdbc.update("""
                    INSERT INTO ticket_refund_lines (id, refund_id, order_id, entitlement_id, entitlement_type, line_type,
                        refund_amount_cent, remaining_times_before, used_times_before, status, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'entry', 'entitlement', ?, 1, 0, 'approved', now(), now())""",
                    refund + "_" + ent.get("id"), refund, order, ent.get("id"), ent.get("allocated_revenue_cent"));
        }
        jdbc.update("UPDATE ticket_instances SET status = 'refunded' WHERE id = ?", ticket);

        events.publishEvent(new TicketRewardFacts.Refunded(refund));

        assertThat(jdbc.queryForList("SELECT status FROM pass_pool_snapshots WHERE ticket_instance_id = ?", String.class, ticket))
                .containsExactly("void");
    }

    // ─────────────────────────── 夹具 ───────────────────────────

    private String merchant(String label, String format) {
        String id = "m_pps_" + label + "_" + key;
        merchants.add(id);
        jdbc.update("""
                INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'active', now(), now())""", id, id, format, project);
        return id;
    }

    /** 已付清的订单（实付 10000）与付款时按微信通路固化的通路费 60；本单销售渠道费所用配置同时固化（同真实付款链）。 */
    private String order(String salesChannel, Instant paidAt) {
        String id = "o_pps_" + (++seq) + "_" + key;
        orders.add(id);
        Timestamp at = Timestamp.from(paidAt);
        jdbc.update("""
                INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, currency, pay_status,
                    issue_status, status, created_at, updated_at, project_id, paid_at)
                VALUES (?, ?, ?, 1, ?, 10000, 'CNY', 'paid', 'issued', 'created', ?, ?, ?, ?)""",
                id, id, type, salesChannel, at, at, project, at);
        jdbc.update("""
                INSERT INTO ticket_payment_intents (id, business_order_type, business_order_id, amount_cent, currency, channel,
                    status, created_at, updated_at) VALUES (?, 'ticket_order', ?, 10000, 'CNY', 'wechat', 'succeeded', ?, ?)""",
                "pi_" + id, id, at, at);
        jdbc.update("""
                INSERT INTO ticket_payment_route_fees (payment_intent_id, order_id, project_id, payment_channel, amount_cent, fee_cent)
                VALUES (?, ?, ?, 'wechat', 10000, 60)""", "pi_" + id, id, project);
        routeFeeShares.fixSalesChannelFee(id);
        return id;
    }

    /** 一张单人单次父票：酒吧 a 入场 5000、酒吧 b 入场 3000、游乐入场 2000（出票时按各业态价格占比拆好的份额）。 */
    private String ticketWithBarsAndPark(String order) {
        String ticket = "tk_" + order;
        Timestamp at = jdbc.queryForObject("SELECT paid_at FROM ticket_orders WHERE id = ?", Timestamp.class, order);
        jdbc.update("""
                INSERT INTO ticket_instances (id, ticket_no, order_id, ticket_type_id, seq_in_order, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 1, 'active', ?, ?)""", ticket, ticket, order, type, at, at);
        entitlement(order, ticket, barA, "entry", 5_000L);
        entitlement(order, ticket, barB, "entry", 3_000L);
        entitlement(order, ticket, park, "amusement_entry", 2_000L);
        return ticket;
    }

    private void entitlement(String order, String ticket, String merchant, String entitlementType, long allocated) {
        String template = "et_pps_" + (++seq) + "_" + key;
        templates.add(template);
        jdbc.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,
                    merchant_id, created_at, updated_at) VALUES (?, '入场', ?, 1, 0, 'active', ?, now(), now())""",
                template, entitlementType, merchant);
        Timestamp at = jdbc.queryForObject("SELECT paid_at FROM ticket_orders WHERE id = ?", Timestamp.class, order);
        String id = "ent_" + ticket + "_" + merchant;
        jdbc.update("""
                INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id, entitlement_code,
                    entitlement_type, merchant_id, total_times, used_times, status, valid_from, valid_to, created_at, updated_at,
                    allocated_revenue_cent, parent_ticket_id, slot_index)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1, 0, 'active', ?, ?, now(), now(), ?, ?, 1)""",
                id, type, order, template, id, entitlementType, merchant, at,
                Timestamp.from(at.toInstant().plusSeconds(86_400L * 30)), allocated, ticket);
    }

    /** 本票种绑一条权益模板（模板归属商户、票面原价，出票时按票面原价占比拆份额）。 */
    private void bind(String entitlementType, String merchant, long componentPrice) {
        String template = "et_pps_" + (++seq) + "_" + key;
        templates.add(template);
        jdbc.update("""
                INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status,
                    merchant_id, created_at, updated_at) VALUES (?, ?, ?, 1, 0, 'active', ?, now(), now())""",
                template, entitlementType, entitlementType, merchant);
        jdbc.update("""
                INSERT INTO ticket_type_entitlements (id, ticket_type_id, entitlement_template_id, quantity, total_times, num_people,
                    value_amount_cent, component_price_cent, created_at, updated_at)
                VALUES (?, ?, ?, 1, 1, 1, 0, ?, now(), now())""", "tte_" + template, type, template, componentPrice);
    }

    /** 一份已发布的本项目分账方案版本（含出厂 1～3 家行）与其第一段名单一行。 */
    private String version(Instant effectiveAt, String firstSegmentBar, String ruleType, long amount) {
        String id = "tdsv_pps_" + (++seq) + "_" + key;
        jdbc.update("""
                INSERT INTO ticket_distribution_scheme_versions (id, project_id, status, notified_at, effective_at)
                VALUES (?, ?, 'notified', ?, ?)""", id, project, Timestamp.from(effectiveAt.minusSeconds(86_400L * 31)),
                Timestamp.from(effectiveAt));
        jdbc.update("""
                INSERT INTO ticket_distribution_participant_tiers (version_id, participant_count, rank_no, share_bps)
                SELECT ?, participant_count, rank_no, share_bps FROM ticket_distribution_participant_tiers
                 WHERE version_id = 'tdsv_factory' AND participant_count <= 3""", id);
        jdbc.update("""
                INSERT INTO ticket_distribution_first_segment_rows (version_id, merchant_id, rule_type, per_guest_cent, fixed_amount_cent)
                VALUES (?, ?, ?, ?, ?)""", id, firstSegmentBar, ruleType,
                "per_guest".equals(ruleType) ? amount : null, "fixed".equals(ruleType) ? amount : null);
        return id;
    }

    /** 该版凭短期经营激励转正：一笔已批准的首次激励，到期日＝expiry 那一营业日的起点。 */
    private void approvedIncentive(String version, LocalDate expiry) {
        jdbc.update("""
                INSERT INTO pass_incentive_applications (id, scheme_version_id, project_id, kind, status, reason, term_value, term_unit,
                    trial_result, max_excess_cent, submitted_by, decided_by, decided_at, valid_from, valid_until)
                SELECT ?, v.id, v.project_id, 'initial', 'approved', '开业季保底', 2, 'month', '[]'::jsonb, 100, 'tester_a', 'tester_b',
                       now(), v.effective_at, ?
                  FROM ticket_distribution_scheme_versions v WHERE v.id = ?""",
                "pia_" + version, Timestamp.from(businessDays.start(project, null, expiry)), version);
    }

    private String schemeOf(String ticket) {
        return jdbc.queryForObject("SELECT scheme_version_id FROM pass_pool_snapshots WHERE ticket_instance_id = ?", String.class, ticket);
    }

    private void verify(String order, String ticket, String merchant, String entitlementType, long allocated) {
        settled.onSettlementSettled(new SettlementSettledEvent("v_pps_" + (++seq) + "_" + key, SOLD.plusSeconds(86_400L),
                "ent_" + ticket + "_" + merchant, order, type, entitlementType, merchant, 1, 1, 1, allocated,
                SOLD.plusSeconds(86_400L * 30), null, null, null, project, ticket));
    }

    private Map<String, Object> fact(String order, String merchant) {
        return jdbc.queryForMap("""
                SELECT * FROM settlement_transactions WHERE order_id = ? AND merchant_id = ? AND transaction_type = 'verification_settled'""",
                order, merchant);
    }

    private int count(String ticket) {
        return jdbc.queryForObject("SELECT count(*) FROM pass_pool_snapshots WHERE ticket_instance_id = ?", Integer.class, ticket);
    }

    private List<String> covered(String ticket) {
        return jdbc.query("SELECT covered_bar_ids FROM pass_pool_snapshots WHERE ticket_instance_id = ?",
                (rs, i) -> Arrays.asList((String[]) rs.getArray(1).getArray()), ticket).getFirst();
    }

    private static long num(Map<String, Object> row, String column) {
        return ((Number) row.get(column)).longValue();
    }
}
