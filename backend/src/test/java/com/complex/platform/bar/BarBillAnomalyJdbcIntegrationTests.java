package com.complex.platform.bar;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.LocalZone;
import com.complex.platform.common.api.ApiException;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerStoredValueAccountRecord;
import com.complex.platform.customer.TicketRewardConfig;
import com.complex.platform.finance.FinanceRepository;
import com.complex.platform.finance.FinanceService;
import com.complex.platform.finance.dto.DailyCloseRequest;
import com.complex.platform.support.TestSecurityActors;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十六批第 16 条真库、真接口回归（请求体按前端 adminClient.mapKeys 后的蛇形键）：ENUM L101「账单状态 待结、部分支付、已结、部分退款、
 * 已退款、异常」；ENUM L221「任何单据进入“异常”后，必须由主管在系统内选择“更正后恢复”或“作废并冲正”两种方式之一处置，填写原因并留痕；
 * 恢复后回到进入异常之前的那个状态」；操作员裁决补充第 2 条「客人对账单提出争议时员工登记置异常。之后都走主管「更正后恢复」或「作废并冲正」」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarBillAnomalyJdbcIntegrationTests {

    private static final String BILLS = "/api/v1/bar/bills/";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcBarRepository bars;
    @Autowired private FinanceService finance;
    @Autowired private BusinessDayResolver businessDays;
    @Autowired private CustomerRepository customers;
    @Autowired private ObjectMapper json;
    @Autowired private BarBillingService billing;
    @Autowired private FinanceRepository financeRepository;
    @Autowired private BarRevenueSourceService revenue;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String merchant = "bba_mer_" + suffix;
    private final AtomicInteger seq = new AtomicInteger();
    private RequestPostProcessor cashier;
    private RequestPostProcessor supervisor;
    private String phone;
    private final String project = "bba_prj_" + suffix;

    @BeforeEach
    void seed() {
        // 员工账号归属综合体（第十七批作废并冲正登记退款应付按操作者所在综合体落账）。
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','账单异常综合体','active',now(),now())",
                project);
        cashier = staff("cashier", "bar.bill.adjust", "finance.payment.register", "bar.bill.refund", "bar.table_commission.calculate");
        supervisor = staff("sup", "bar.bill.anomaly.resolve");
        // 酒吧退款分级授权闸（AUD-007）：收银员本域自助额度。
        jdbc.update("INSERT INTO refund_authority_limits (id, subject_type, subject_id, refund_domain, limit_amount_cent)"
                + " VALUES (?, 'staff', ?, 'bar', 100000000)", "ral_bba_" + suffix, "bba_cashier_" + suffix);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 待结账单：员工登记争议即置异常，异常期间不能收款、没有处置权限的不能处置；主管「更正后恢复」回到待结，之后照常收款。 */
    @Test
    void disputedUnpaidBillStaysAbnormalUntilSupervisorRestores() throws Exception {
        String bill = bill("unpaid", "unsettled", 0L);
        call(cashier, bill + "/anomaly", "{\"reason\":\"客人称未点第二瓶香槟\",\"idempotency_key\":\"ra_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("abnormal"));
        assertThat(jdbc.queryForObject("SELECT abnormal_prior_status FROM bar_bills WHERE id=?", String.class, bill)).isEqualTo("unpaid");
        call(cashier, bill + "/settle", "{\"payment_id\":\"pay1_%s\",\"payment_channel\":\"aggregate_pay\",\"idempotency_key\":\"st1_%s\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_ABNORMAL"));
        call(cashier, bill + "/anomaly/resolve", "{\"resolution\":\"restored\",\"reason\":\"核实已点\",\"idempotency_key\":\"rs0_%s\"}")
                .andExpect(status().isForbidden());

        call(supervisor, bill + "/anomaly/resolve",
                "{\"resolution\":\"restored\",\"reason\":\"监控核实两瓶均已送达，按原账单收款\",\"idempotency_key\":\"rs_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("unpaid"));
        assertThat(jdbc.queryForObject("SELECT abnormal_prior_status FROM bar_bills WHERE id=?", String.class, bill)).isNull();
        call(cashier, bill + "/settle", "{\"payment_id\":\"pay2_%s\",\"payment_channel\":\"aggregate_pay\",\"idempotency_key\":\"st2_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("paid"));
        assertThat(successes("bar.bill_anomaly_raised", bill)).isEqualTo(1);
        assertThat(successes("bar.bill_anomaly_resolved", bill)).isEqualTo(1);
    }

    /** 已结账单：争议登记后主管「作废并冲正」——账单作废，已登记的收款流水按反结同一做法冲正；作废后不能再收款，也不能再登记争议。 */
    /**
     * 第十七批复核：BSM L1064「以该笔留款抵付的部分不重复收款、不重复确认收入」——实收（含本单退款时留在台位账上的低消留款）随作废整笔原路待退，
     * 本单留款不再作台组抵用额、本单抵用过的台组留款回到台组余额；裁决「冲回……提成（工资单不再挂待定）」——作废后不能再计提成。
     */
    @Test
    void disputedSettledBillVoidedAndCollectionsReversed() throws Exception {
        String bill = bill("paid", "settled", 30000L);
        jdbc.update("UPDATE bar_bills SET sales_staff_id='bba_sales', sales_role_type='sales' WHERE id=?", bill);
        bars.saveBillSettlement(new BarBillSettlementRecord("settle_bba_" + suffix, bill, "pos", null, "POS-" + suffix,
                30000L, merchant, Instant.now()));
        for (String type : List.of("min_spend_retained", "min_spend_retained_applied")) {
            bars.saveBillAdjustment(new BarBillAdjustmentRecord("bba_adj_" + type + "_" + suffix, bill, type,
                    "min_spend_retained".equals(type) ? 10000L : 4000L, 30000L, 30000L, type, "bba_cashier_" + suffix, false, "applied",
                    "bba_" + type + "_" + suffix, List.of(), null, "{}", Instant.now(), null));
        }
        call(cashier, bill + "/anomaly", "{\"reason\":\"客人称整单记错卡座\",\"idempotency_key\":\"rb_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("abnormal"));
        call(supervisor, bill + "/anomaly/resolve",
                "{\"resolution\":\"voided_reversed\",\"reason\":\"整单录错卡座，作废后在正确卡座重开\",\"idempotency_key\":\"vb_%s\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_status").value("void"))
                .andExpect(jsonPath("$.data.settlement_status").value("void"));
        assertThat(jdbc.queryForObject("SELECT reversed FROM bar_bill_settlements WHERE bill_id=?", Boolean.class, bill)).isTrue();
        call(cashier, bill + "/settle", "{\"payment_id\":\"pay3_%s\",\"payment_channel\":\"aggregate_pay\",\"idempotency_key\":\"st3_%s\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_VOIDED"));
        call(cashier, bill + "/anomaly", "{\"reason\":\"再登记\",\"idempotency_key\":\"rc_%s\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_VOIDED"));
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE direction='payable' AND source_type='refund' AND source_id=?",
                Long.class, bill)).isEqualTo(30000L);
        call(cashier, bill + "/commissions/calculate", "{\"bill_id\":\"" + bill + "\",\"idempotency_key\":\"cm_%s\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_VOIDED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM bar_commission_calculations WHERE bill_id=?", Integer.class, bill)).isZero();
        assertThat(jdbc.queryForList("SELECT DISTINCT status FROM bar_bill_adjustments WHERE bill_id=?"
                + " AND adjustment_type IN ('min_spend_retained','min_spend_retained_applied')", String.class, bill)).containsExactly("reversed");
    }

    /** 重复登记被拒并留失败痕；同一幂等键重放返回第一次结果、成功只记一次；处置方式只认两种；部分支付账单恢复后回到部分支付。 */
    @Test
    void repeatedRegistrationRejectedReplayIdempotentAndRestoreReturnsPriorState() throws Exception {
        String bill = bill("partial_paid", "partial_settled", 10000L);
        String raise = "{\"reason\":\"客人对低消补差有异议\",\"idempotency_key\":\"rp_%s\"}";
        call(cashier, bill + "/anomaly", raise).andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("abnormal"));
        call(cashier, bill + "/anomaly", raise).andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("abnormal"));
        call(cashier, bill + "/anomaly", "{\"reason\":\"再次登记\",\"idempotency_key\":\"rp2_%s\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_ALREADY_ABNORMAL"));
        assertThat(successes("bar.bill_anomaly_raised", bill)).isEqualTo(1);
        assertThat(failures("bar.bill_anomaly_raise_failed", "rp2_" + suffix)).isEqualTo(1);

        call(supervisor, bill + "/anomaly/resolve", "{\"resolution\":\"ignored\",\"reason\":\"不处理\",\"idempotency_key\":\"rx_%s\"}")
                .andExpect(status().isBadRequest());
        call(supervisor, bill + "/anomaly/resolve", "{\"resolution\":\"restored\",\"reason\":\"补差口径已向客人解释\",\"idempotency_key\":\"rr_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("partial_paid"))
                .andExpect(jsonPath("$.data.paid_amount_cent").value(10000));
    }

    /** ENUM L221「异常单据未处置完毕，所属日结不得确认锁定」：结账落在该营业日的账单处于异常时日结锁定被拒；主管处置后可锁。 */
    @Test
    void lockDailyCloseRefusedWhileBarBillAbnormal() throws Exception {
        LocalDate day = LocalDate.parse("2014-06-12");
        Instant at = businessDays.startInstant(day).plusSeconds(3600);
        String bill = settledBillAt(at);
        call(cashier, bill + "/anomaly", "{\"reason\":\"客人称多收一瓶\",\"idempotency_key\":\"rd_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("abnormal"));

        TestSecurityActors.asPlatformAdmin();
        assertThatThrownBy(() -> finance.createDailyClose(dailyClose(day)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DAY_CLOSE_ANOMALY_PENDING"));
        SecurityContextHolder.clearContext();
        call(supervisor, bill + "/anomaly/resolve", "{\"resolution\":\"restored\",\"reason\":\"核实无误\",\"idempotency_key\":\"rdr_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("paid"));
        TestSecurityActors.asPlatformAdmin();
        assertThat(finance.createDailyClose(dailyClose(day)).status()).isEqualTo("locked");
    }

    /** 与反结同一做法：结账营业日已锁定日结的，「作废并冲正」被拒（须走补录调整流程），账单仍在异常，「更正后恢复」不受影响。 */
    @Test
    void voidReverseRefusedAfterSettleDayLocked() throws Exception {
        LocalDate day = LocalDate.parse("2014-06-13");
        String bill = settledBillAt(Instant.parse("2014-06-13T12:00:00Z"));
        TestSecurityActors.asPlatformAdmin();
        assertThat(finance.createDailyClose(dailyClose(day)).status()).isEqualTo("locked");
        SecurityContextHolder.clearContext();

        call(cashier, bill + "/anomaly", "{\"reason\":\"次日客人来电称记错卡座\",\"idempotency_key\":\"rl_%s\"}")
                .andExpect(status().isOk());
        call(supervisor, bill + "/anomaly/resolve", "{\"resolution\":\"voided_reversed\",\"reason\":\"作废重开\",\"idempotency_key\":\"rlv_%s\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_LOCKED"));
        assertThat(jdbc.queryForObject("SELECT payment_status FROM bar_bills WHERE id=?", String.class, bill)).isEqualTo("abnormal");
        assertThat(jdbc.queryForObject("SELECT reversed FROM bar_bill_settlements WHERE bill_id=?", Boolean.class, bill)).isFalse();
        call(supervisor, bill + "/anomaly/resolve", "{\"resolution\":\"restored\",\"reason\":\"走补录调整\",\"idempotency_key\":\"rlr_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("paid"));
    }

    /**
     * 第十七批（操作员裁决补充第 1 条「全部自动冲回（推荐）：作废时系统自动把已收款项登记为原路待退（走现有退款链），并冲回已发积分、
     * 成长值和提成（工资单不再挂待定）」）：先储值 5000 后现金 25000 结清、结账后已办现金退款 10000（应付尚未结清）的账单作废并冲正——
     * 实收减已退的 20000 按收款倒序原路待退：后收现金剩余 15000 挂退款应付待财务结清、储值 5000 当场回原卡，连同先前退款合计等于已收；
     * 本单商户消费积分与成长值不等先前退款退出即冲回；销售提成作废，派驻人员提成已随月结拨付所属主体的（BSM L1143「由该酒吧按月拨付给人员所属主体」）
     * 向所属主体追加应收冲回；同一处置重放不重复退，换键再处置被拒。
     */
    @Test
    void voidReversedRefundsCollectionsAlongOriginalRoutesAndReversesRewardsAndCommission() throws Exception {
        String member = memberWithCard();
        String bill = deliveredBill(member);
        call(cashier, bill + "/settle", "{\"payment_id\":\"psv_%s\",\"payment_channel\":\"stored_value\",\"settlement_amount_cent\":5000,"
                + "\"phone\":\"" + phone + "\",\"sms_code\":\"000000\",\"idempotency_key\":\"ssv_%s\"}").andExpect(status().isOk());
        call(cashier, bill + "/settle", "{\"payment_id\":\"pcs_%s\",\"payment_channel\":\"cash\",\"idempotency_key\":\"scs_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("paid"));
        assertThat(cardBalance()).isEqualTo(3_000L);
        String reward = jdbc.queryForObject("SELECT id FROM merchant_consumption_rewards WHERE source_type='bar_bill' AND source_id=?",
                String.class, bill);
        String commission = "bba_cm_" + suffix;
        jdbc.update("INSERT INTO bar_commission_calculations(id,bill_id,sales_staff_id,sales_role_type,commission_base_cent,commission_rate_bp,"
                + "commission_amount_cent,status,created_at) VALUES(?,?,'bba_sales','sales',30000,1500,4500,'calculated',now())", commission, bill);
        // 销售为派驻人员，本月提成已月结拨付所属主体（月结在作废之前）。
        String company = "bba_co_" + suffix;
        String zone = LocalZone.ID.getId();
        jdbc.update("INSERT INTO staff_dispatch_records(id,user_id,company_id,merchant_id,project_id,start_date,status)"
                + " VALUES(?,'bba_sales',?,?,?,(now() AT TIME ZONE ?)::date,'active')", "bba_dsp_" + suffix, company, merchant, project, zone);
        jdbc.update("INSERT INTO bar_commission_settlements(id,month,merchant_id,company_id,project_id,amount_cent,calculation_count,staff_count,"
                + "due_date,created_at) VALUES(?,to_char(now() AT TIME ZONE ?,'YYYY-MM'),?,?,?,4500,1,1,(now() AT TIME ZONE ?)::date + 10,now())",
                "bcs_bba_" + suffix, zone, merchant, company, project, zone);
        // 结账后先办一笔现金退款 10000（接口退款要过主管复核准入闸，这里直接走退款服务）：后收的现金先退，挂退款应付、财务尚未结清，奖励按规则继续待确认。
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(
                "bba_cashier_" + suffix, "bba_cashier_" + suffix, "merchant_staff", project, merchant, "active"), "token", List.of()));
        billing.applyBillAdjustment(bill, new BarBillAdjustmentRequest("rf_" + suffix, null, List.of(), "refund", 10_000L, "客人退一瓶", null));
        SecurityContextHolder.clearContext();
        assertThat(cardBalance()).isEqualTo(3_000L);
        assertThat(jdbc.queryForObject("SELECT status FROM merchant_consumption_rewards WHERE id=?", String.class, reward)).isNotEqualTo("cancelled");

        call(cashier, bill + "/anomaly", "{\"reason\":\"客人称整单不是本桌消费\",\"idempotency_key\":\"rv_%s\"}").andExpect(status().isOk());
        String resolve = "{\"resolution\":\"voided_reversed\",\"reason\":\"核实整单记错，作废并退款\",\"idempotency_key\":\"vv_%s\"}";
        call(supervisor, bill + "/anomaly/resolve", resolve).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_status").value("void"));
        call(supervisor, bill + "/anomaly/resolve", resolve).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_status").value("void"));
        call(supervisor, bill + "/anomaly/resolve", resolve.replace("vv_", "vv2_"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("BAR_BILL_NOT_ABNORMAL"));

        // ① 原路待退：先前退款应付 10000，作废登记后收现金剩余的 15000，先收的储值 5000 当场回原卡，三项合计等于已收 30000。
        assertThat(jdbc.queryForObject("SELECT x.amount_cent FROM bar_bill_adjustments a JOIN ar_ap_items x ON x.id=a.ar_ap_item_id"
                + " WHERE a.bill_id=? AND a.adjustment_type='refund'", Long.class, bill)).isEqualTo(10_000L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE direction='payable' AND source_type='refund' AND source_id=?",
                Integer.class, bill)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE direction='payable' AND source_type='refund' AND source_id=?",
                Long.class, bill)).isEqualTo(15_000L);
        assertThat(cardBalance()).isEqualTo(8_000L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customer_stored_value_transactions WHERE account_id=? AND txn_type='consume_reversal'",
                Integer.class, "svacc_bba_" + suffix)).isEqualTo(1);
        // ② 本单待确认积分取消、成长值冲回（成长值流水净额归零）。
        Map<String, Object> after = jdbc.queryForMap(
                "SELECT status, points, reversed_points, growth, reversed_growth FROM merchant_consumption_rewards WHERE id=?", reward);
        assertThat(((Number) after.get("growth")).longValue()).isEqualTo(300L);
        assertThat(after.get("status")).isEqualTo("cancelled");
        assertThat(after.get("reversed_points")).isEqualTo(after.get("points"));
        assertThat(after.get("reversed_growth")).isEqualTo(after.get("growth"));
        assertThat(jdbc.queryForObject("SELECT sum(growth_delta) FROM customer_growth_ledger WHERE merchant_reward_id=?", Long.class, reward))
                .isZero();
        // ③ 销售提成作废（工资核算按 void 排除，不再挂「酒吧账单尚未确认结算」）。
        assertThat(jdbc.queryForObject("SELECT status FROM bar_commission_calculations WHERE id=?", String.class, commission))
                .isEqualTo("void");
        // ④ 已月结拨付所属主体的派驻提成：向所属主体追加一笔应收冲回，原月结记录不改。
        assertThat(jdbc.queryForList("SELECT direction, counterparty_id, amount_cent, status FROM ar_ap_items"
                + " WHERE source_type='dispatch_commission_transfer' AND source_id=?", commission))
                .containsExactly(Map.of("direction", "receivable", "counterparty_id", company, "amount_cent", 4_500L,
                        "status", "pending_collection"));
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM bar_commission_settlements WHERE id=?", Long.class, "bcs_bba_" + suffix))
                .isEqualTo(4_500L);
    }

    /** 本商户会员及其本商户储值卡（余额 8000）；商户入册，配全国参数与酒吧业态积分（每 10 元 1 分）、成长值（每 1 元 1 点）规则。 */
    private String memberWithCard() throws Exception {
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,'作废冲回酒吧','bar','active',?,now(),now())", merchant, project);
        Instant effective = Instant.now().minusSeconds(5);
        rewardConfig("merchant_points", "bar", rate(1_000L), effective, "V0.01");
        rewardConfig("merchant_growth", "bar", rate(100L), effective, "V0.01");
        String entity = "be_bba_" + suffix;
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at)"
                + " VALUES(?,'积分结算中心','platform','active',now(),now())", entity);
        rewardConfig("national", null, new TicketRewardConfig.Values(null, null, null, 24, 1, 720, new BigDecimal("0.01"),
                new BigDecimal("0.001"), new BigDecimal("100"), entity, null, null, null, List.of(), 24, 1, 720), effective,
                "V" + Math.floorMod(suffix.hashCode(), 900_000) + ".07");
        String member = "bba_cus_" + suffix;
        phone = "139" + String.format("%08d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000L));
        Instant now = Instant.now();
        customers.saveCustomer(new CustomerRecord(member, phone, "139****" + phone.substring(7), "active", null, null, now, now));
        customers.saveStoredValueAccount(new CustomerStoredValueAccountRecord("svacc_bba_" + suffix, member, null, merchant, 8_000L,
                "active", now, now, "merchant_prepaid", 8_000L, 0L));
        return member;
    }

    private static TicketRewardConfig.Values rate(long cents) {
        return new TicketRewardConfig.Values(cents, 1L, 1_000_000L, null, null, null, null, null, null, null, null, null, null, List.of());
    }

    private void rewardConfig(String kind, String format, TicketRewardConfig.Values values, Instant effective, String version)
            throws Exception {
        jdbc.update("INSERT INTO ticket_reward_config_versions(id,kind,business_format,version,payload,effective_at,published_at,status,"
                + "created_by,created_at,approved_at) VALUES(?,?,?,?,?::jsonb,?,?,'approved','test-reviewer',?,?)",
                "trc_" + UUID.randomUUID().toString().replace("-", ""), kind, format, version, json.writeValueAsString(values),
                Timestamp.from(effective), Timestamp.from(effective.minusSeconds(60)), Timestamp.from(effective.minusSeconds(120)),
                Timestamp.from(effective.minusSeconds(60)));
    }

    /** 下单客人为会员、出品已全部送达的 30000 待结账单。 */
    private String deliveredBill(String member) {
        int n = seq.incrementAndGet();
        String bar = "bba_bar_" + suffix;
        String table = "bba_tbl_" + suffix + "_" + n;
        String order = "bba_ord_" + suffix + "_" + n;
        String product = "bba_prd_" + suffix + "_" + n;
        String bill = "bba_bill_" + suffix + "_" + n;
        Instant now = Instant.now();
        bars.saveTable(new BarTableRecord(table, bar, merchant, "T-" + suffix + "-" + n, "冲回回归卡座", "card_seat",
                4, 1, 0L, "CNY", "active", now, now));
        bars.saveProduct(new BarProductRecord(product, bar, merchant, "冲回回归酒", "spirit", 30_000L, "CNY", null, null, false,
                "active", now, now, null, null, null, false, null, null, null));
        bars.saveOrder(new BarOrderRecord(order, bar, merchant, table, null, member, "staff", "created", 30_000L, "CNY", "web",
                null, null, now, now));
        bars.saveOrderItem(new BarOrderItemRecord("bba_item_" + suffix + "_" + n, order, product, "冲回回归酒", 1, 30_000L, 30_000L,
                "delivered", now, now, null, 0L, false, null, null, null, false, false, now.minusSeconds(60)));
        bars.saveBill(new BarBillRecord(bill, "BILL-BBA-" + suffix + "-" + n, order, bar, merchant, table, null,
                30_000L, 0L, 0L, 30_000L, 0L, "CNY", "unpaid", "unsettled", null, null, now, now));
        return bill;
    }

    private long cardBalance() {
        return jdbc.queryForObject("SELECT balance_cent FROM customer_stored_value_accounts WHERE id=?", Long.class, "svacc_bba_" + suffix);
    }

    /**
     * 第十七批②（裁决「作废时系统自动把已收款项登记为原路待退……」与「冲正」）：挂账账单（已挂应收、财务待追收）作废并冲正——冲销挂账时挂出的应收
     * （同合同预排账单作废的冲正行写法：回指原行、金额取反、状态 reversed），原行不再计欠款、财务不再追收；没有实收的不挂退款应付。
     */
    @Test
    void voidReversedOnAccountBillReversesItsReceivable() throws Exception {
        String bill = bill("unpaid", "unsettled", 0L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(
                "bba_cashier_" + suffix, "bba_cashier_" + suffix, "merchant_staff", project, merchant, "active"), "token", List.of()));
        billing.applyBillAdjustment(bill, new BarBillAdjustmentRequest("oa_" + suffix, null, List.of(), "on_account", 0L, "熟客挂账月结", null));
        SecurityContextHolder.clearContext();
        String receivable = jdbc.queryForObject("SELECT id FROM ar_ap_items WHERE direction='receivable' AND source_type='bar_bill' AND source_id=?",
                String.class, bill);
        assertThat(finance.listOutstandingReceivables(merchant)).extracting(FinanceService.DividendArrearsItem::arApItemId).contains(receivable);

        call(cashier, bill + "/anomaly", "{\"reason\":\"客人称挂账金额不对\",\"idempotency_key\":\"ro_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("abnormal"));
        call(supervisor, bill + "/anomaly/resolve",
                "{\"resolution\":\"voided_reversed\",\"reason\":\"核实整单录错，作废并冲正\",\"idempotency_key\":\"rov_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("void"));

        assertThat(jdbc.queryForList("SELECT amount_cent, paid_amount_cent, unpaid_amount_cent, status FROM ar_ap_items WHERE origin_item_id=?",
                receivable)).containsExactly(Map.of("amount_cent", -30_000L, "paid_amount_cent", 0L, "unpaid_amount_cent", -30_000L,
                "status", "reversed"));
        assertThat(finance.listOutstandingReceivables(merchant)).extracting(FinanceService.DividendArrearsItem::arApItemId)
                .doesNotContain(receivable);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE direction='payable' AND source_type='refund' AND source_id=?",
                Integer.class, bill)).isZero();
    }

    /**
     * 第十七批⑤：作废前已办过退款的账单——已退的 10000 已在退款营业日冲减（跨期退款口径），作废只冲正未退部分的收款、只把未退的 20000 原路待退；
     * 日结、分红与经营分析不重复冲减：收款营业日留存已退部分、退款营业日（已锁日结）不改，作废账单在各期合计为 0。
     */
    @Test
    void voidAfterPartialRefundReversesOnlyUnrefundedCollectionsSoPeriodsNetToZero() throws Exception {
        LocalDate settleDay = LocalDate.parse("2012-02-14");
        LocalDate refundDay = LocalDate.parse("2012-02-15");
        Instant refundedAt = Instant.parse("2012-02-15T12:00:00Z");
        String bill = settledBillAt(Instant.parse("2012-02-14T12:00:00Z"));
        String space = "bba_sp_" + suffix;
        jdbc.update("INSERT INTO spaces(id,project_id,space_name,space_type,status,created_at,updated_at)"
                + " VALUES(?,?,'争议回归卡座区','unit','active',now(),now())", space, project);
        jdbc.update("UPDATE bar_tables SET space_id=? WHERE id=(SELECT table_id FROM bar_bills WHERE id=?)", space, bill);
        String refund = "bba_rf_" + suffix;
        String payable = "bba_ap_" + suffix;
        jdbc.update("INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,unpaid_amount_cent,due_date,"
                + "status,created_at,updated_at) VALUES(?,?,'payable',?,'refund',?,10000,10000,'2012-02-15','open',now(),now())",
                payable, payable, merchant, refund);
        bars.saveBillAdjustment(new BarBillAdjustmentRecord(refund, bill, "refund", 10_000L, 30_000L, 20_000L, "客人退一瓶",
                "bba_cashier_" + suffix, true, "applied", "rk_" + suffix, List.of(), payable, "{}", refundedAt, refundedAt));
        jdbc.update("INSERT INTO bar_payroll_refund_lines(id,adjustment_id,collection_bill_id,source_bill_id,component_type,amount_cent,ar_ap_item_id)"
                + " VALUES(?,?,?,?,'goods',10000,?)", "bprf_bba_" + suffix, refund, bill, bill, payable);
        jdbc.update("UPDATE bar_bills SET payable_amount_cent=20000 WHERE id=?", bill);
        jdbc.update("INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,created_at)"
                + " VALUES(?,?,10000,'bank_transfer',?,now())", "bba_pay_" + suffix, payable, Timestamp.from(refundedAt));
        TestSecurityActors.asPlatformAdmin();
        assertThat(finance.createDailyClose(dailyClose(refundDay)).status()).isEqualTo("locked");
        SecurityContextHolder.clearContext();
        assertThat(net(settleDay)).isEqualTo(30_000L);
        assertThat(net(refundDay)).isEqualTo(-10_000L);

        call(cashier, bill + "/anomaly", "{\"reason\":\"客人称整单记错卡座\",\"idempotency_key\":\"rp_%s\"}")
                .andExpect(status().isOk());
        call(supervisor, bill + "/anomaly/resolve",
                "{\"resolution\":\"voided_reversed\",\"reason\":\"核实整单记错，作废并冲正\",\"idempotency_key\":\"rpv_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("void"));

        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE direction='payable' AND source_type='refund' AND source_id=?",
                Long.class, bill)).isEqualTo(20_000L);
        // 日结：退款营业日已锁、不改；收款营业日只冲正未退的 20000，两期合计为 0。
        assertThat(net(refundDay)).isEqualTo(-10_000L);
        assertThat(net(settleDay) + net(refundDay)).isZero();
        // 分红与经营分析（同一收入来源）：收款营业日的商品收入与退款营业日的商品退款相抵。
        assertThat(revenue.completedCollections(merchant, businessDays.startInstant(settleDay), businessDays.endInstant(settleDay)))
                .filteredOn(c -> bill.equals(c.billId())).singleElement().satisfies(c -> {
                    assertThat(c.status()).isEqualTo("confirmed");
                    assertThat(c.goodsRevenueCent()).isEqualTo(10_000L);
                });
        assertThat(revenue.completedGoodsRefunds(merchant, businessDays.startInstant(refundDay), businessDays.endInstant(refundDay)))
                .filteredOn(r -> bill.equals(r.billId())).singleElement().satisfies(r -> assertThat(r.amountCent()).isEqualTo(10_000L));
    }

    /**
     * 第十七批复核⑤（第十六批 TK-04-b「退回储值卡的部分即时成功」）：先现金 25000 后储值 5000 结清，作废前先退 3000（全额回卡、不挂应付），
     * 再退 4000（2000 回卡、2000 挂应付并由财务付清）——作废后留存的已退 7000 商品收入，与两笔已退成的商品退款相抵为 0。
     */
    @Test
    void voidAfterStoredValueRefundsNetsGoodsRevenueToZero() throws Exception {
        String member = memberWithCard();
        String bill = deliveredBill(member);
        String space = "bba_sp_" + suffix;
        jdbc.update("INSERT INTO spaces(id,project_id,space_name,space_type,status,created_at,updated_at)"
                + " VALUES(?,?,'储值退款回归卡座区','unit','active',now(),now())", space, project);
        jdbc.update("UPDATE bar_tables SET space_id=? WHERE id=(SELECT table_id FROM bar_bills WHERE id=?)", space, bill);
        call(cashier, bill + "/settle", "{\"payment_id\":\"pcs_%s\",\"payment_channel\":\"cash\",\"settlement_amount_cent\":25000,"
                + "\"idempotency_key\":\"scs_%s\"}").andExpect(status().isOk());
        call(cashier, bill + "/settle", "{\"payment_id\":\"psv_%s\",\"payment_channel\":\"stored_value\",\"settlement_amount_cent\":5000,"
                + "\"phone\":\"" + phone + "\",\"sms_code\":\"000000\",\"idempotency_key\":\"ssv_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("paid"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(
                "bba_cashier_" + suffix, "bba_cashier_" + suffix, "merchant_staff", project, merchant, "active"), "token", List.of()));
        billing.applyBillAdjustment(bill, new BarBillAdjustmentRequest("sv1_" + suffix, null, List.of(), "refund", 3_000L, "客人退一杯", null));
        BarBillAdjustmentRecord mixed = billing.applyBillAdjustment(bill,
                new BarBillAdjustmentRequest("sv2_" + suffix, null, List.of(), "refund", 4_000L, "客人再退一杯", null));
        SecurityContextHolder.clearContext();
        assertThat(cardBalance()).isEqualTo(8_000L);
        jdbc.update("INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,created_at)"
                + " VALUES(?,?,2000,'cash',now(),now())", "bba_svpay_" + suffix, mixed.arApItemId());

        call(cashier, bill + "/anomaly", "{\"reason\":\"客人称整单不是本桌消费\",\"idempotency_key\":\"rsv_%s\"}").andExpect(status().isOk());
        call(supervisor, bill + "/anomaly/resolve",
                "{\"resolution\":\"voided_reversed\",\"reason\":\"核实整单记错，作废并冲正\",\"idempotency_key\":\"vsv_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("void"));

        Instant from = Instant.now().minusSeconds(86_400);
        Instant to = Instant.now().plusSeconds(60);
        assertThat(revenue.completedCollections(merchant, from, to)).filteredOn(c -> bill.equals(c.billId()))
                .extracting(BarRevenueSourceService.CollectionRevenue::goodsRevenueCent).containsExactly(7_000L);
        assertThat(revenue.completedGoodsRefunds(merchant, from, to)).filteredOn(r -> bill.equals(r.billId()))
                .extracting(BarRevenueSourceService.GoodsRefund::amountCent).containsExactlyInAnyOrder(3_000L, 4_000L);
    }

    /**
     * 第十七批复核②（裁决「作废时系统自动把已收款项登记为原路待退（走现有退款链）」）：挂账 30000、财务已登记收回 12000 后作废并冲正——
     * 已收的 12000 挂退款应付由财务按原收款方式结清，未收的 18000 冲正。
     */
    @Test
    void voidReversedOnAccountBillRefundsCollectedPartAndReversesRest() throws Exception {
        String bill = bill("unpaid", "unsettled", 0L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new AccessActor(
                "bba_cashier_" + suffix, "bba_cashier_" + suffix, "merchant_staff", project, merchant, "active"), "token", List.of()));
        billing.applyBillAdjustment(bill, new BarBillAdjustmentRequest("oa_" + suffix, null, List.of(), "on_account", 0L, "熟客挂账月结", null));
        SecurityContextHolder.clearContext();
        String receivable = jdbc.queryForObject("SELECT id FROM ar_ap_items WHERE direction='receivable' AND source_type='bar_bill' AND source_id=?",
                String.class, bill);
        mockMvc.perform(post("/api/v1/finance/ar-ap-items/" + receivable + "/payments").with(cashier).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount_cent\":12000,\"payment_method\":\"bank_transfer\",\"idempotency_key\":\"fp_" + suffix + "\"}"))
                .andExpect(status().isOk());

        call(cashier, bill + "/anomaly", "{\"reason\":\"客人称挂账金额不对\",\"idempotency_key\":\"rop_%s\"}").andExpect(status().isOk());
        call(supervisor, bill + "/anomaly/resolve",
                "{\"resolution\":\"voided_reversed\",\"reason\":\"核实整单录错，作废并冲正\",\"idempotency_key\":\"ropv_%s\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_status").value("void"));

        assertThat(jdbc.queryForList("SELECT amount_cent FROM ar_ap_items WHERE direction='payable' AND source_type='refund' AND source_id=?",
                Long.class, bill)).containsExactly(12_000L);
        assertThat(jdbc.queryForList("SELECT amount_cent, status FROM ar_ap_items WHERE origin_item_id=?", receivable))
                .containsExactly(Map.of("amount_cent", -18_000L, "status", "reversed"));
    }

    /** 商户口径日结酒吧源：收款减退款。 */
    private long net(LocalDate day) {
        FinanceRepository.DailyCloseSourceTotals totals = financeRepository.aggregateBar(day, businessDays.startInstant(day),
                businessDays.endInstant(day), "merchant", merchant);
        return totals.amountCent() - totals.refundCent();
    }

    private String settledBillAt(Instant at) {
        String bill = bill("paid", "settled", 30000L);
        jdbc.update("UPDATE bar_bills SET created_at=? WHERE id=?", Timestamp.from(at), bill);
        bars.saveBillSettlement(new BarBillSettlementRecord("settle_bba_" + bill, bill, "pos", null, "POS-" + bill,
                30000L, merchant, at));
        return bill;
    }

    private DailyCloseRequest dailyClose(LocalDate day) {
        return new DailyCloseRequest("bba-close-" + suffix + "-" + seq.incrementAndGet(), "日结", List.of(), day, "merchant",
                merchant, false);
    }

    private ResultActions call(RequestPostProcessor who, String path, String body) throws Exception {
        return mockMvc.perform(post(BILLS + path).with(who).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("%s", suffix)));
    }

    private String bill(String paymentStatus, String settlementStatus, long paidCent) {
        int n = seq.incrementAndGet();
        String bar = "bba_bar_" + suffix;
        String table = "bba_tbl_" + suffix + "_" + n;
        String order = "bba_ord_" + suffix + "_" + n;
        String bill = "bba_bill_" + suffix + "_" + n;
        Instant now = Instant.now();
        bars.saveTable(new BarTableRecord(table, bar, merchant, "T-" + suffix + "-" + n, "争议回归卡座", "card_seat",
                4, 1, 0L, "CNY", "active", now, now));
        bars.saveOrder(new BarOrderRecord(order, bar, merchant, table, null, null, "staff", "billed", 30000L, "CNY", "web",
                null, null, now, now));
        bars.saveBill(new BarBillRecord(bill, "BILL-BBA-" + suffix + "-" + n, order, bar, merchant, table, null,
                30000L, 0L, 0L, 30000L, paidCent, "CNY", paymentStatus, settlementStatus, null, null, now, now));
        return bill;
    }

    private RequestPostProcessor staff(String tag, String... permissions) {
        String userId = "bba_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'merchant_staff', ?, ?, 'active', now())", userId, userId, project, merchant);
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                + " VALUES (?, ?, '账单异常回归', 'merchant', 'active', now())", role, role);
        for (String code : permissions) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        AccessActor actor = new AccessActor(userId, userId, "merchant_staff", project, merchant, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }

    private int successes(String event, String businessId) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name=? AND business_id=?", Integer.class,
                event, businessId);
    }

    private int failures(String event, String idempotencyKey) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name=? AND idempotency_key=?", Integer.class,
                event, idempotencyKey);
    }
}
