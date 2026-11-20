package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.common.LocalZone;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.customer.CustomerSessionRecord;
import com.complex.platform.customer.CustomerStoredValueAccountRecord;
import com.complex.platform.customer.StoredValueService;
import com.complex.platform.settlement.GeneralCardConsumptionSettlementJob;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.time.YearMonth;
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
 * 第十八批 BD 段第 367 条（BSM:1066 酒吧支付方式「储值卡（通用卡先扣本金后扣赠送金且不可配置；……）」；BSM:833「①综合体通用卡：综合体内各商户通用」
 * 「消费时按客人指定的某一张卡扣减，不自动跨卡」；BSM:868「每笔扣款（商户、扣减的本金与赠送金各多少）」；ENUM:86「在商户处消费商品或服务的，按该商户给客人的
 * 成交价格、扣除该笔消费所对应的通路费后与该商户结算」；BSM:704「独立酒吧商户负责自己的经营、人员、设备、单一酒吧门票和店内收入。」；BSM:1704「长沙项目公司
 * 就通用卡余额的实际消费向各业态的经营主体直接付款结算，按三个付账日结清」）：
 * 367-R1 酒吧结账储值按客人指定扣综合体通用卡（账单桌台所在铺位的项目那张），客人逐笔显示扣款酒吧；367-R2 酒吧账单通用卡消费并入月度结算，
 * 挂长沙项目公司对该酒吧商户的应付。
 *
 * <p>真库、真实入口、真实授权链、蛇形键，随测试事务回滚。酒吧商户 M 在综合体 P，账单桌台绑 P 的铺位；客人通用卡先充一批 20000 分本金（通路费率 1%）。
 * 修复前：结账请求的 stored_value_card 被忽略、只找本酒吧商户卡，客人只有通用卡时 404 STORED_VALUE_ACCOUNT_NOT_FOUND；月度入账不取酒吧账单消费。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarGeneralCardSettleJdbcIntegrationTests {

    private static final String BILLS = "/api/v1/bar/bills/";
    private static final String SOURCE = "general_card_consume";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcBarRepository bars;
    @Autowired private CustomerRepository customers;
    @Autowired private StoredValueService storedValueService;
    @Autowired private BusinessDayResolver businessDays;
    @Autowired private GeneralCardConsumptionSettlementJob job;

    private final String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String projectP = "r367prj_p_" + suffix;
    private final String projectQ = "r367prj_q_" + suffix;
    private final String merchant = "r367mer_" + suffix;
    private final String bar = "r367bar_" + suffix;
    private final String space = "r367sp_" + suffix;
    private final String customer = "r367cus_" + suffix;
    private final String token = "cust_token_r367_" + suffix;
    private final AtomicInteger seq = new AtomicInteger();
    private String phone;
    private RequestPostProcessor cashier;

    @BeforeEach
    void seed() {
        for (String project : List.of(projectP, projectQ)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                    project, "通用卡酒吧结账综合体" + project);
        }
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,?,'bar','active',?,now(),now())", merchant, "通用卡回归酒吧" + suffix, projectP);
        jdbc.update("INSERT INTO spaces(id,project_id,space_name,space_type,status,created_at,updated_at)"
                + " VALUES(?,?,'通用卡回归卡座区','unit','active',now(),now())", space, projectP);
        phone = "139" + String.format("%08d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 100_000_000L));
        Instant now = Instant.now();
        customers.saveCustomer(new CustomerRecord(customer, phone, "139****" + phone.substring(7), "active", null, null, now, now));
        customers.saveSession(new CustomerSessionRecord("sess_r367_" + suffix, customer, token, "miniapp", now,
                now.plusSeconds(3600), "active"));
        cashier = cashier();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** ① 客人只有 P 的通用卡：结账指定 general 扣通用卡 10000，消费流水键以 barsv_ 开头（修复前 404 STORED_VALUE_ACCOUNT_NOT_FOUND）。 */
    @Test
    void generalCardPaysBarBillOfItsComplex() throws Exception {
        String card = generalCard(projectP);
        String bill = deliveredBill();

        settle(bill, ",\"stored_value_card\":\"general\"").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_status").value("partial_paid"));

        assertThat(principal(card)).isEqualTo(10_000L);
        assertThat(jdbc.queryForObject("SELECT idempotency_key FROM customer_stored_value_transactions WHERE account_id = ? AND txn_type = 'consume'",
                String.class, card)).startsWith("barsv_");
    }

    /** ② 客人同时有本酒吧商户卡与通用卡：不指定扣哪张时只扣本酒吧商户卡，通用卡不动（BSM:833「不自动跨卡」）。 */
    @Test
    void withoutChoiceStillChargesTheBarMerchantCard() throws Exception {
        String general = generalCard(projectP);
        String merchantCard = "svacc_r367_m_" + suffix;
        Instant now = Instant.now();
        customers.saveStoredValueAccount(new CustomerStoredValueAccountRecord(merchantCard, customer, null, merchant, 20_000L,
                "active", now, now, "merchant_prepaid", 20_000L, 0L));
        String bill = deliveredBill();

        settle(bill, "").andExpect(status().isOk());

        assertThat(principal(merchantCard)).isEqualTo(10_000L);
        assertThat(principal(general)).isEqualTo(20_000L);
    }

    /** ③ 客人只有别的综合体 Q 的通用卡：指定 general 结账 404 STORED_VALUE_ACCOUNT_NOT_FOUND，账单仍待结、卡不动。 */
    @Test
    void generalCardOfAnotherComplexIsNotUsed() throws Exception {
        String other = generalCard(projectQ);
        String bill = deliveredBill();

        settle(bill, ",\"stored_value_card\":\"general\"").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("STORED_VALUE_ACCOUNT_NOT_FOUND"));

        assertThat(jdbc.queryForObject("SELECT payment_status FROM bar_bills WHERE id = ?", String.class, bill)).isEqualTo("unpaid");
        assertThat(principal(other)).isEqualTo(20_000L);
    }

    /** ④ 客人在客人端看通用卡逐笔：酒吧结账那笔扣款显示扣款商户为结账酒吧（BSM:868「每笔扣款（商户……）」）。 */
    @Test
    void customerSeesTheBarOnGeneralCardDebit() throws Exception {
        String card = generalCard(projectP);
        settle(deliveredBill(), ",\"stored_value_card\":\"general\"").andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/customer/stored-value/accounts/" + card + "/transactions")
                        .header("X-Terminal-Type", "miniapp").header("X-Customer-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].txn_type").value("consume"))
                .andExpect(jsonPath("$.data[0].merchant_id").value(merchant))
                .andExpect(jsonPath("$.data[0].merchant_name").value("通用卡回归酒吧" + suffix));
    }

    /**
     * 367-R2：通用卡结账 10000、再退 2000 回原卡（酒吧退款与反结回卡所走的 refundConsumption），付款营业月结束后月度入账挂一笔长沙项目公司对酒吧商户 M 的
     * 应付：10000−100（批次通路费 1%）−(2000−20)＝7920，项目 P；两条流水都记进 general_card_consume_lines；再跑一轮不重复挂账（修复前不取酒吧账单消费，无此应付）。
     */
    @Test
    void barBillGeneralCardConsumptionPostsMonthlyPayableToTheBarMerchant() throws Exception {
        InternalPayDayFixtures.seed(jdbc);
        jdbc.update("""
                INSERT INTO internal_pay_day_rules (id, kind, route, statement_day, pay_slot, effective_from, status, reason,
                    submitted_by, decided_by, decided_at)
                VALUES (?, 'route', ?, 1, 1, DATE '2000-01-01', 'approved', '测试夹具', 'fixture-finance', 'fixture-approver', now())
                """, "ipdr_r367_" + suffix, SOURCE);
        String card = generalCard(projectP);
        settle(deliveredBill(), ",\"stored_value_card\":\"general\"").andExpect(status().isOk());
        String consume = jdbc.queryForObject("SELECT id FROM customer_stored_value_transactions WHERE account_id = ? AND txn_type = 'consume'"
                + " AND idempotency_key LIKE 'barsv\\_%'", String.class, card);
        TestSecurityActors.asPlatformAdmin("usr_r367_rf_" + suffix);
        String reversal;
        try {
            reversal = storedValueService.refundConsumption(consume, 2_000L, "客人退一杯", "r367-rf-" + suffix).id();
        } finally {
            TestSecurityActors.clear();
        }
        Instant runAt = YearMonth.from(businessDays.businessDate(projectP, bar, Instant.now())).plusMonths(2).atDay(2)
                .atTime(10, 0).atZone(LocalZone.ID).toInstant();

        job.run(runAt);

        List<String> items = jdbc.queryForList("SELECT ar_ap_item_id FROM general_card_consume_lines WHERE txn_id = ?", String.class, consume);
        assertThat(items).hasSize(1).doesNotContainNull();
        String item = items.getFirst();
        assertThat(jdbc.queryForObject("SELECT ar_ap_item_id FROM general_card_consume_lines WHERE txn_id = ?", String.class, reversal))
                .isEqualTo(item);
        assertThat(jdbc.queryForMap("SELECT direction, source_type, counterparty_id, project_id, amount_cent FROM ar_ap_items WHERE id = ?", item))
                .isEqualTo(Map.of("direction", "payable", "source_type", SOURCE, "counterparty_id", merchant, "project_id", projectP,
                        "amount_cent", 7_920L));

        job.run(runAt);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ar_ap_items WHERE source_type = ? AND project_id = ?", Integer.class,
                SOURCE, projectP)).isEqualTo(1);
    }

    // ---------- 夹具与请求 ----------

    /** 客人在该综合体的通用卡：充一批 20000 分本金（通路费率 1%）。 */
    private String generalCard(String project) {
        String card = "svacc_r367_g_" + project;
        Instant now = Instant.now();
        customers.saveStoredValueAccount(new CustomerStoredValueAccountRecord(card, customer, project, null, 20_000L, "active", now, now,
                null, 20_000L, 0L));
        String topup = "svtxn_r367_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.update("INSERT INTO customer_stored_value_transactions (id, account_id, customer_id, txn_type, amount_cent, balance_after_cent,"
                + " reason, idempotency_key, created_at, principal_cent, bonus_cent) VALUES (?, ?, ?, 'topup', 20000, 20000, '通用卡充值',"
                + " ?, now() - interval '1 minute', 20000, 0)", topup, card, customer, "r367-topup-" + topup);
        jdbc.update("INSERT INTO stored_value_topup_batches (id, account_id, customer_id, project_id, payment_channel, fee_bps, principal_cent,"
                + " remaining_principal_cent, paid_at) VALUES (?, ?, ?, ?, 'wechat', 100, 20000, 20000, now())", topup, card, customer, project);
        return card;
    }

    /** 该客人点单、出品已全部送达的 30000 待结账单，桌台绑在综合体 P 的铺位。 */
    private String deliveredBill() {
        int n = seq.incrementAndGet();
        String table = "r367tbl_" + suffix + "_" + n;
        String order = "r367ord_" + suffix + "_" + n;
        String product = "r367prd_" + suffix + "_" + n;
        String bill = "r367bill_" + suffix + "_" + n;
        Instant now = Instant.now();
        bars.saveTable(new BarTableRecord(table, bar, merchant, "T-" + suffix + "-" + n, "通用卡回归卡座", "card_seat",
                4, 1, 0L, "CNY", "active", now, now));
        jdbc.update("UPDATE bar_tables SET space_id = ? WHERE id = ?", space, table);
        bars.saveProduct(new BarProductRecord(product, bar, merchant, "通用卡回归酒", "spirit", 30_000L, "CNY", null, null, false,
                "active", now, now, null, null, null, false, null, null, null));
        bars.saveOrder(new BarOrderRecord(order, bar, merchant, table, null, customer, "staff", "created", 30_000L, "CNY", "web",
                null, null, now, now));
        bars.saveOrderItem(new BarOrderItemRecord("r367item_" + suffix + "_" + n, order, product, "通用卡回归酒", 1, 30_000L, 30_000L,
                "delivered", now, now, null, 0L, false, null, null, null, false, false, now.minusSeconds(60)));
        bars.saveBill(new BarBillRecord(bill, "BILL-R367-" + suffix + "-" + n, order, bar, merchant, table, null,
                30_000L, 0L, 0L, 30_000L, 0L, "CNY", "unpaid", "unsettled", null, null, now, now));
        return bill;
    }

    /** 储值支付 10000 分（手机号＋短信验证码），extra 为追加的请求字段。 */
    private ResultActions settle(String bill, String extra) throws Exception {
        int n = seq.incrementAndGet();
        String body = "{\"payment_id\":\"psv_r367_" + suffix + "_" + n + "\",\"payment_channel\":\"stored_value\",\"settlement_amount_cent\":10000,"
                + "\"phone\":\"" + phone + "\",\"sms_code\":\"000000\",\"idempotency_key\":\"ssv_r367_" + suffix + "_" + n + "\"" + extra + "}";
        return mockMvc.perform(post(BILLS + bill + "/settle").with(cashier).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long principal(String card) {
        return jdbc.queryForObject("SELECT principal_cent FROM customer_stored_value_accounts WHERE id = ?", Long.class, card);
    }

    /** 酒吧 M 的收银员（商户账号，归属综合体 P），持结账收款权限。 */
    private RequestPostProcessor cashier() {
        String userId = "r367_cashier_" + suffix;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'merchant_staff', ?, ?, 'active', now())", userId, userId, projectP, merchant);
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                + " VALUES (?, ?, '通用卡酒吧结账回归', 'merchant', 'active', now())", role, "通用卡酒吧结账回归收银" + suffix);
        jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'finance.payment.register')", role);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        AccessActor actor = new AccessActor(userId, userId, "merchant_staff", projectP, merchant, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, "token", List.of())).postProcessRequest(request);
        };
    }
}
