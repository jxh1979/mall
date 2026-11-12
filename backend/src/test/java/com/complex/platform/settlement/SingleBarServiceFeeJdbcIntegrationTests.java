package com.complex.platform.settlement;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.LocalZone;
import com.complex.platform.support.CustomerTicketPaymentFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.complex.platform.support.TicketSalesContractTestFixtures;
import com.complex.platform.support.TestSecurityActors;
import com.complex.platform.ticketing.TicketRewardFacts;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * FS-013 单一酒吧门票综合服务费真库回归（BSM L755 / ENUM L57 / BSM L804；裁决 Q33/Q33A；操作员裁决 2026-10-19 第 3 条
 * 「售出月，只退款剔除：按客人付款成功（售出）所在月计入；之后只有整张退款成功才从原月剔除重算；到期没用的票照常计费」）。
 * 退款成功一律走前端真实请求 POST /api/v1/ticketing/refunds/{refund_id}/confirm-payout（蛇形键 idempotency_key / reason）。
 * 金额按 Q33 例子：A、B 两张各应计 70 元、月封顶 100 元。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SingleBarServiceFeeJdbcIntegrationTests {

    private static final YearMonth SOLD = YearMonth.of(2013, 9); // 远离今日的确定售出月

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ApplicationEventPublisher events;
    @Autowired private TicketDistributionSettlementService settlement;
    @Autowired private SettlementPostingService posting;
    @Autowired private SingleBarServiceFeeService fees;
    @Autowired private ObjectMapper json;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "fs013_prj_" + suffix;
    private final String bar = "fs013_bar_" + suffix;
    private final String type = "fs013_tt_" + suffix;
    private final String template = "fs013_et_" + suffix;
    private final String feeConfig = "fs013_csfc_" + suffix;
    private int seq;
    private RequestPostProcessor finance;

    @BeforeEach
    void seed() {
        // FS-021：过账到期日按财务指定付账日推算，缺规则/日历即拒绝——种已批钱路规则与工作日历。
        com.complex.platform.support.InternalPayDayFixtures.seed(jdbc);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','单店服务费回归项目','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,status,created_at,updated_at) VALUES(?,'单店服务费酒吧','bar',?,'active',now(),now())", bar, project);
        jdbc.update("INSERT INTO ticket_types(id,ticket_name,base_calendar_price_mode,bar_id,refundable,status,created_at,updated_at) VALUES(?,'单店门票','fixed',?,TRUE,'active',now(),now())", type, bar);
        jdbc.update("INSERT INTO entitlement_templates(id,name,entitlement_type,default_total_times,default_value_cent,status,created_at,updated_at) VALUES(?,'单店入场','bar_entry',1,0,'active',now(),now())", template);
        // 按比例 10%，月封顶 100 元。
        jdbc.update("INSERT INTO comprehensive_service_fee_configs(id,project_id,merchant_id,ticket_type_id,fee_bps,monthly_cap_amount_cent,status,is_draft) VALUES(?,?,?,?,1000,10000,'active',FALSE)",
                feeConfig, project, bar, type);
        String role = "fs013_role_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'单店服务费回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN('ticket.refund.approve','finance.ar_ap.manage')", role);
        String user = "fs013_fin_" + suffix;
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'platform_admin','org','active',now())", user, user);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        AccessActor actor = new AccessActor(user, user, "platform_admin", "org", null, "active");
        finance = request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
        TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // R1+R2：出票逐票锁定（计费基数＝订单客人实付按张均分，不取权益分摊/内部结算价）；月应收＝min(Σ逐票费, 锁定封顶)；
    //   月结过账另挂酒吧应收，酒吧票款应付按毛额全额、不扣服务费（不轧差）。
    @Test
    void saleLocksPerTicketFeeOnPaidPriceAndMonthIsPostedAsSeparateReceivable() {
        List<String> tickets = sell(2, 140_000L);
        assertThat(jdbc.queryForList("SELECT base_cent, fee_cent FROM single_bar_service_fee_tickets WHERE parent_ticket_id IN (?,?) ORDER BY parent_ticket_id",
                tickets.get(0), tickets.get(1)))
                .extracting(r -> List.of(num(r, "base_cent"), num(r, "fee_cent")))
                .containsExactly(List.of(70_000L, 7_000L), List.of(70_000L, 7_000L));
        seedExclusiveVerification(70_000L);

        postMonth();

        List<Map<String, Object>> receivables = items("receivable", "single_bar_service_fee");
        assertThat(receivables).hasSize(1);
        assertThat(num(receivables.get(0), "amount_cent")).as("两张各 70 元、封顶 100 元 → 当月应收 100 元").isEqualTo(10_000L);
        assertThat(receivables.get(0).get("counterparty_id")).isEqualTo(bar);
        assertThat(receivables.get(0).get("project_id")).isEqualTo(project);
        assertThat(items("payable", "ticket_distribution")).extracting(r -> num(r, "amount_cent"))
                .as("酒吧票款应付按毛额，不扣服务费").containsExactly(70_000L);
    }

    // 生产月结只出平台单（SettlementMonthlyJob → settleMonth(null, 月份)，project_id 为空）：计费月照样挂出酒吧应收（BSM L755
    //   「按前述计费月汇总后的应收总额另行收取，同样走三个付账日」）；应收与各笔付款时点按计费月 / 酒吧所属项目归属，不丢项目。
    @Test
    void platformMonthlyBillPostsMonthReceivableUnderBarProject() {
        sell(2, 140_000L);
        seedExclusiveVerification(70_000L);

        TestSecurityActors.asPlatformAdmin();
        posting.postBill(settlement.settleMonth(null, SOLD));

        List<Map<String, Object>> receivables = items("receivable", "single_bar_service_fee");
        assertThat(receivables).extracting(r -> num(r, "amount_cent")).as("平台单下计费月照样挂出应收").containsExactly(10_000L);
        assertThat(receivables.get(0).get("project_id")).isEqualTo(project);
        assertThat(jdbc.queryForList("""
                SELECT s.project_id FROM internal_payable_schedules s JOIN ar_ap_items a ON a.id = s.ar_ap_item_id
                 WHERE a.counterparty_id = ? ORDER BY a.source_type
                """, String.class, bar)).as("服务费应收与票款应付的付款时点都归酒吧所属项目").containsExactly(project, project);
    }

    // R1：该月封顶在首张票写入时锁定，之后改配置不影响原月。
    @Test
    void monthCapLockedAtFirstSaleSurvivesLaterConfigChange() {
        sell(1, 70_000L);
        jdbc.update("UPDATE comprehensive_service_fee_configs SET monthly_cap_amount_cent=5000 WHERE id=?", feeConfig);
        sell(1, 70_000L);

        postMonth();

        assertThat(items("receivable", "single_bar_service_fee")).extracting(r -> num(r, "amount_cent")).containsExactly(10_000L);
    }

    // R3（Q33/Q33A）：已收 100 元后退 A → 只新增应退 30；再退 B → 重算总应退 100、已有 30 在途 → 只新增 70；原应收与收款不动；
    //   跨月退款的调整记在当前账期并关联原计费月；同一退款事实重放不再登记。
    @Test
    void refundOfCollectedMonthRegistersOnlyUncoveredRefundDue() throws Exception {
        List<String> tickets = sell(2, 140_000L);
        postMonth();
        List<Map<String, Object>> posted = items("receivable", "single_bar_service_fee");
        assertThat(posted).hasSize(1);
        String ar = posted.get(0).get("id").toString();
        jdbc.update("UPDATE ar_ap_items SET paid_amount_cent=amount_cent, unpaid_amount_cent=0, status='settled' WHERE id=?", ar);

        String refundA = refundOk(tickets.get(0));
        assertThat(items("payable", "single_bar_service_fee")).extracting(r -> num(r, "amount_cent")).containsExactly(3_000L);
        String refundB = refundOk(tickets.get(1));
        assertThat(items("payable", "single_bar_service_fee")).extracting(r -> num(r, "amount_cent"))
                .as("已有应退 30 在途，只新增 70，不出现 130").containsExactlyInAnyOrder(3_000L, 7_000L);
        assertThat(items("payable", "single_bar_service_fee")).extracting(r -> r.get("source_id"))
                .containsExactlyInAnyOrder(refundA, refundB);

        Map<String, Object> original = jdbc.queryForMap("SELECT amount_cent, paid_amount_cent, status FROM ar_ap_items WHERE id=?", ar);
        assertThat(List.of(num(original, "amount_cent"), num(original, "paid_amount_cent"))).containsExactly(10_000L, 10_000L);
        assertThat(items("receivable", "single_bar_service_fee")).as("原应收与收款不覆盖").hasSize(1);
        Map<String, Object> adjustment = jdbc.queryForMap(
                "SELECT a.accounting_period, m.billing_month FROM single_bar_service_fee_adjustments a JOIN single_bar_service_fee_months m ON m.id=a.month_id WHERE a.refund_id=?", refundA);
        assertThat(adjustment.get("accounting_period")).as("213-R1：当前账期按确认时刻所属该酒吧营业日定月")
                .isEqualTo(fees.businessMonth(project, bar, Instant.now()).toString());
        assertThat(adjustment.get("billing_month").toString()).isEqualTo(SOLD.atDay(1).toString());

        events.publishEvent(new TicketRewardFacts.Refunded(refundB));
        assertThat(items("payable", "single_bar_service_fee")).hasSize(2);
    }

    // R3：原月应收未收时退 A → 待收由 100 调为 70（原行不动＋冲正行＋重述行），不列应退。
    @Test
    void refundOfUncollectedMonthOnlyReducesPending() throws Exception {
        List<String> tickets = sell(2, 140_000L);
        postMonth();

        refundOk(tickets.get(0));

        List<Map<String, Object>> rows = items("receivable", "single_bar_service_fee");
        assertThat(rows).extracting(r -> num(r, "amount_cent")).containsExactlyInAnyOrder(10_000L, -10_000L, 7_000L);
        Map<String, Object> restated = rows.stream().filter(r -> num(r, "amount_cent") == 7_000L).findFirst().orElseThrow();
        assertThat(num(restated, "unpaid_amount_cent")).isEqualTo(7_000L);
        assertThat(restated.get("origin_item_id")).isNotNull();
        assertThat(restated.get("accounting_period")).isEqualTo(fees.businessMonth(project, bar, Instant.now()).toString());
        assertThat(items("payable", "single_bar_service_fee")).isEmpty();
    }

    // R3：月结过账前已整张退款的票不进原月应收总额。
    @Test
    void ticketRefundedBeforePostingIsLeftOutOfMonthTotal() throws Exception {
        List<String> tickets = sell(2, 140_000L);
        refundOk(tickets.get(0));

        postMonth();

        assertThat(items("receivable", "single_bar_service_fee")).extracting(r -> num(r, "amount_cent")).containsExactly(7_000L);
        assertThat(items("payable", "single_bar_service_fee")).isEmpty();
    }

    // 第十八批 AI 段 213-R1（BSM:1682「全系统的钱账统一按「营业日」归集」「酒吧仍按每家酒吧配置」；决策台账 FS-013「按客人付款成功（售出）所在月计入」）：
    //   计费月＝付款成功时刻所属该酒吧营业日的月。出厂营业日 10:00 起算：北京 7 月 1 日 09:30 付款属 6 月 30 日营业日 → 计入 6 月；10:30 → 7 月。
    @Test
    void billingMonthFollowsBarBusinessDayOfPaymentOnFirstMorning() throws Exception {
        RequestPostProcessor seller = publishForSale();
        String early = sellOverHttp(seller, Instant.parse("2026-07-01T01:30:00Z"));
        String late = sellOverHttp(seller, Instant.parse("2026-07-01T02:30:00Z"));

        assertThat(billingMonth(early)).as("北京 7-01 09:30 付款属 6-30 营业日，计入 6 月").isEqualTo("2026-06-01");
        assertThat(billingMonth(late)).as("北京 7-01 10:30 付款属 7-01 营业日").isEqualTo("2026-07-01");
    }

    // 213-R1：该酒吧已批准营业日起点 12:00 时按该酒吧的版本定月：北京 7 月 1 日 11:00 付款仍属 6 月 30 日营业日。
    @Test
    void billingMonthUsesTheBarsApprovedBusinessDayStart() throws Exception {
        Instant effective = Instant.parse("2026-01-01T00:00:00Z");
        jdbc.update("""
                INSERT INTO bar_business_day_configs(id,bar_id,project_id,version,cutoff_hour,min_cutoff_hour,max_cutoff_hour,
                  close_hour,min_close_hour,max_close_hour,effective_at,status,reason,created_by,created_at,decided_by,decided_at)
                VALUES(?,?,?,'V0.01',12,0,23,7,0,23,?,'approved','单店服务费营业日回归','fs013_requester',?,'usr_master',?)
                """, "bbd_fs013_" + suffix, bar, project, Timestamp.from(effective),
                Timestamp.from(effective.minusSeconds(7200)), Timestamp.from(effective.minusSeconds(3600)));
        RequestPostProcessor seller = publishForSale();

        String order = sellOverHttp(seller, Instant.parse("2026-07-01T03:00:00Z"));

        assertThat(billingMonth(order)).as("酒吧 12:00 起算：北京 7-01 11:00 属 6-30 营业日").isEqualTo("2026-06-01");
    }

    // 213-R1：退款调整的当前账期与售出计费月共用同一营业日定月（BSM:755「在当前账期生成关联原计费月的调整」）。
    @Test
    void businessMonthOfFirstMorningBeforeDayStartIsThePreviousMonth() {
        assertThat(fees.businessMonth(project, bar, Instant.parse("2026-07-01T01:00:00Z"))).isEqualTo(YearMonth.of(2026, 6));
        assertThat(fees.businessMonth(project, bar, Instant.parse("2026-07-01T02:00:00Z"))).isEqualTo(YearMonth.of(2026, 7));
    }

    // ───────── helpers ─────────

    /** 213-R1 真实售票链：单店票种已发布（每张 8000 分、一次入场）且有已批准销售合同；平台员工按后台网页下单、登记现金、出票。 */
    private RequestPostProcessor publishForSale() {
        jdbc.update("UPDATE ticket_types SET base_calendar_price_mode='flat', weekday_price_cent=8000, weekend_price_cent=8000,"
                + " holiday_price_cent=8000, stock_required=FALSE, status='published', validity_days=1 WHERE id=?", type);
        String entry = "fs013_entry_" + suffix;
        jdbc.update("INSERT INTO entitlement_templates(id,name,entitlement_type,default_total_times,default_value_cent,status,merchant_id,"
                + "entitlement_kind,consumption_window,created_at,updated_at) VALUES(?,'单店入场','entry',1,8000,'active',?,'virtual','per_use',now(),now())",
                entry, bar);
        jdbc.update("INSERT INTO ticket_type_entitlements(id,ticket_type_id,entitlement_template_id,quantity,total_times,num_people,"
                + "value_amount_cent,component_price_cent,created_at,updated_at) VALUES(?,?,?,1,1,1,8000,8000,now(),now())",
                "fs013_bind_" + suffix, type, entry);
        TicketSalesContractTestFixtures.approvedPublished(jdbc, json, type, project, bar);
        return new StaffRequestFixture(jdbc).staff("fs013_seller_" + suffix, "staff", project, null,
                "ticket.order.create", "ticket.order.read", "finance.payment_intent.create", "finance.payment.register", "ticket.ticket.issue");
    }

    /** 按后台网页请求体（蛇形键）下单、登记现金收款、确认收款后出票；出票前把付款成功时刻固定到指定时点（服务端取确认收款时刻）。 */
    private String sellOverHttp(RequestPostProcessor seller, Instant paidAt) throws Exception {
        String key = "fs013-bd-" + suffix + "-" + (++seq);
        ResultActions created = mockMvc.perform(post("/api/v1/ticketing/orders").with(seller).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotency_key": "%s-order", "ticket_type_id": "%s", "quantity": 1, "sales_channel": "admin", "bar_id": "%s"}
                                """.formatted(key, type, bar)))
                .andExpect(status().isOk());
        String order = CustomerTicketPaymentFixture.read(created, "$.data.id");
        String amount = CustomerTicketPaymentFixture.read(created, "$.data.amount_cent");
        String intent = CustomerTicketPaymentFixture.read(mockMvc.perform(post("/api/v1/ticketing/orders/" + order + "/pay-intents")
                        .with(seller).contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "%s-intent", "business_order_type": "ticket_order", "business_order_id": "%s",
                                 "amount_cent": %s, "currency": "CNY", "channel": "cash"}
                                """.formatted(key, order, amount)))
                .andExpect(status().isOk()), "$.data.id");
        mockMvc.perform(post("/api/v1/ticketing/orders/" + order + "/confirm-payment").with(seller)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "%s-confirm", "payment_intent_id": "%s"}
                                """.formatted(key, intent)))
                .andExpect(status().isOk());
        jdbc.update("UPDATE ticket_orders SET paid_at=? WHERE id=?", Timestamp.from(paidAt), order);
        mockMvc.perform(post("/api/v1/ticketing/orders/" + order + "/issue").with(seller)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"idempotency_key": "%s-issue", "reason": "客人在前台补票"}
                                """.formatted(key)))
                .andExpect(status().isOk());
        return order;
    }

    private String billingMonth(String order) {
        return jdbc.queryForObject("""
                SELECT DISTINCT m.billing_month::text FROM single_bar_service_fee_months m
                  JOIN single_bar_service_fee_tickets t ON t.month_id = m.id WHERE t.order_id = ?
                """, String.class, order);
    }

    /** 售出并出票：订单付款成功在售出月，出票事实发布后由结算域逐票锁定。权益分摊额刻意给 30000（≠ 客人实付分摊）。 */
    private List<String> sell(int quantity, long amountCent) {
        String order = "fs013_o_" + suffix + "_" + (++seq);
        Timestamp paidAt = Timestamp.from(SOLD.atDay(10).atTime(12, 0).atZone(LocalZone.ID).toInstant());
        jdbc.update("INSERT INTO ticket_orders(id,order_no,ticket_type_id,quantity,sales_channel,amount_cent,pay_status,issue_status,status,created_at,updated_at,project_id,bar_id,paid_at) VALUES(?,?,?,?,'miniapp',?,'paid','issued','created',?,?,?,?,?)",
                order, order, type, quantity, amountCent, paidAt, paidAt, project, bar, paidAt);
        List<String> tickets = new ArrayList<>();
        for (int i = 1; i <= quantity; i++) {
            String ticket = order + "_t" + i;
            jdbc.update("INSERT INTO ticket_instances(id,ticket_no,order_id,ticket_type_id,seq_in_order,status,created_at,updated_at,bar_id) VALUES(?,?,?,?,?,'active',?,?,?)",
                    ticket, ticket, order, type, i, paidAt, paidAt, bar);
            jdbc.update("INSERT INTO issued_entitlements(id,ticket_type_id,order_id,entitlement_template_id,entitlement_code,entitlement_type,total_times,used_times,status,valid_from,valid_to,created_at,updated_at,allocated_revenue_cent,parent_ticket_id,slot_index,bar_id) VALUES(?,?,?,?,?,'bar_entry',1,0,'active',?,?,?,?,30000,?,1,?)",
                    ticket + "_e", type, order, template, ticket + "_e", paidAt, Timestamp.from(paidAt.toInstant().plusSeconds(86_400L * 30)), paidAt, paidAt, ticket, bar);
            tickets.add(ticket);
        }
        events.publishEvent(new TicketRewardFacts.Issued(order));
        return tickets;
    }

    /** 整张退款已批准、权益已冻结（退款审批链结果），财务实际打款后按前端请求确认拨付 → 退款成功。 */
    private String refundOk(String ticket) throws Exception {
        String order = jdbc.queryForObject("SELECT order_id FROM ticket_instances WHERE id=?", String.class, ticket);
        String refund = "fs013_r_" + suffix + "_" + (++seq);
        jdbc.update("INSERT INTO ticket_refunds(id,target_id,refund_amount_cent,reason,refund_channel,status,requested_by,created_at,updated_at) VALUES(?,?,70000,'客人使用前申请退票','original','approved','fs013-requester',now(),now())", refund, order);
        jdbc.update("INSERT INTO ticket_refund_lines(id,refund_id,order_id,entitlement_id,entitlement_type,line_type,refund_amount_cent,remaining_times_before,used_times_before,status,created_at,updated_at) VALUES(?,?,?,?,'bar_entry','entitlement',70000,1,0,'approved',now(),now())",
                refund + "_l", refund, order, ticket + "_e");
        jdbc.update("UPDATE issued_entitlements SET status='refunded' WHERE id=?", ticket + "_e");
        jdbc.update("UPDATE ticket_instances SET status='refunded' WHERE id=?", ticket);
        mockMvc.perform(post("/api/v1/ticketing/refunds/" + refund + "/confirm-payout").with(finance)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"fs013-payout-" + refund + "\",\"reason\":\"已原路退回客人\"}"))
                .andExpect(status().isOk());
        return refund;
    }

    /** 该月一笔专属（单店）核销事实：售出时已锁定服务费，核销行不再带服务费。 */
    private void seedExclusiveVerification(long verifiedCent) {
        Timestamp at = Timestamp.from(SOLD.atDay(12).atTime(10, 0).atZone(LocalZone.ID).toInstant());
        String id = "st_fs013_" + suffix + "_" + (++seq);
        jdbc.update("INSERT INTO settlement_transactions(id,transaction_no,transaction_type,entitlement_id,order_id,ticket_type_id,entitlement_type,verified_count,total_times,num_people,"
                        + "allocated_revenue_cent,verified_amount_cent,unverified_amount_cent,beneficiary_type,merchant_id,project_id,exclusive_merchant_id,verification_id,settlement_status,"
                        + "entitlement_valid_to,business_at,discount_amount_cent,comp_service_fee_cent,created_at,updated_at)"
                        + " VALUES(?,?,'verification_settled','ent_fs013','ord_fs013',?,'bar_entry',1,1,1,?,?,0,'merchant',?,?,?,?,'draft',?,?,0,0,?,?)",
                id, "TN-" + id, type, verifiedCent, verifiedCent, bar, project, bar, "vf_" + id, at, at, at, at);
    }

    private void postMonth() {
        TestSecurityActors.asPlatformAdmin(); // MockMvc 请求结束会清掉线程上的登录态，月结过账前重新以平台管理员操作。
        String billId = settlement.settleMonth(project, SOLD);
        posting.postBill(billId);
    }

    private List<Map<String, Object>> items(String direction, String sourceType) {
        return jdbc.queryForList("""
                SELECT * FROM ar_ap_items WHERE direction=? AND source_type=? AND counterparty_id=?
                 ORDER BY created_at, (origin_item_id IS NOT NULL), amount_cent DESC
                """, direction, sourceType, bar);
    }

    private static long num(Map<String, Object> row, String column) {
        return ((Number) row.get(column)).longValue();
    }
}
