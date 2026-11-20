package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.bar.BarBillRecord;
import com.complex.platform.bar.BarBillSettlementRecord;
import com.complex.platform.bar.BarOrderRecord;
import com.complex.platform.bar.BarRepository;
import com.complex.platform.bar.BarTableRecord;
import com.complex.platform.common.LocalZone;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
 * 第十八批 BD 段第 364 条 364-R1（BSM:833「运营端提供储值台账，可按客人、商户、期间查询逐笔明细与余额构成。」「商户的员工只能管理本商户发行的储值卡。」；
 * ENUM:86「每笔充值拆为本金与赠送金两段分别记账」；PGM:659「所有业务数据默认按项目隔离」「在所分配的权限内可以查看、处理该角色可查看项目的数据」）：
 * 储值台账查询 queryStoredValueLedger。真库、真实入口、真实授权链、蛇形键，随测试事务回滚。
 *
 * <p>夹具：项目 A、B；酒吧商户 M 在 A、商户 N 在 B；客人有 A 通用卡、B 通用卡与 M 的商户卡（项目 A）。A 通用卡：40 天前充值本金 10000 赠送 1000，
 * 近 3 分钟内依次消费本金 3000、回冲本金 1000、在酒吧 M 结账扣本金 500（barsv_ 键对应 M 的结算行）。修复前接口不存在（404）。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class StoredValueLedgerJdbcIntegrationTests {

    private static final String LEDGER = "/api/v1/loyalty/stored-value/ledger/query";
    private static final String VIEW = "customer.stored_value.view";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private AccessRepository accessRepository;
    @Autowired private BarRepository bars;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String projectA = "r364prj_a_" + s;
    private final String projectB = "r364prj_b_" + s;
    private final String merchantM = "r364mch_m_" + s;
    private final String merchantN = "r364mch_n_" + s;
    private final String customer = "r364cus_" + s;
    private String phone;
    private StaffRequestFixture staffs;
    private String generalA;
    private String generalB;
    private String cardM;
    private String topupA;
    private String consumeA;
    private String reversalA;
    private String barConsumeA;
    private String topupB;
    private String topupM;

    @BeforeEach
    void seed() {
        for (String project : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                    project, "储值台账回归综合体" + project);
        }
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,?,'bar','active',?,now(),now())",
                merchantM, "储值台账回归酒吧" + s, projectA);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,?,'bar','active',?,now(),now())",
                merchantN, "储值台账回归别家酒吧" + s, projectB);
        phone = "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        Instant now = Instant.now();
        customerRepository.saveCustomer(new CustomerRecord(customer, phone, "139****" + phone.substring(7), "active", null, null, now, now));
        generalA = card(projectA, null, 8_500L, 7_500L, 1_000L);
        generalB = card(projectB, null, 5_000L, 5_000L, 0L);
        cardM = card(projectA, merchantM, 2_000L, 2_000L, 0L);

        topupA = txn(generalA, "topup", 10_000L, 1_000L, 11_000L, now.minus(40, ChronoUnit.DAYS), null, null);
        consumeA = txn(generalA, "consume", -3_000L, 0L, 8_000L, now.minusSeconds(180), null, null);
        reversalA = txn(generalA, "consume_reversal", 1_000L, 0L, 9_000L, now.minusSeconds(120), consumeA, null);
        String bill = barBill(now);
        bars.saveBillSettlement(new BarBillSettlementRecord("settle_r364_" + s, bill, "stored_value", null, null, 500L, merchantM, now));
        barConsumeA = txn(generalA, "consume", -500L, 0L, 8_500L, now.minusSeconds(60), null, "barsv_r364_" + s);
        topupB = txn(generalB, "topup", 5_000L, 0L, 5_000L, now.minusSeconds(300), null, null);
        topupM = txn(cardM, "topup", 2_000L, 0L, 2_000L, now.minusSeconds(300), null, null);
        staffs = new StaffRequestFixture(jdbc);
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    /** ① 平台管理员按手机号与期间查：A 通用卡各行倒序，变动后本金、赠送金按卡的全部流水累计（期间前的充值也算），只回掩码手机号。 */
    @Test
    void platformAdminSeesRunningPrincipalAndBonus() throws Exception {
        String json = query(admin(), "{\"phone\":\"" + phone + "\",\"date_from\":\"" + yesterday() + "\",\"date_to\":\"" + today() + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.page_size").value(20))
                .andExpect(jsonPath(row(consumeA, "principal_after_cent"), contains(7_000)))
                .andExpect(jsonPath(row(consumeA, "bonus_after_cent"), contains(1_000)))
                .andExpect(jsonPath(row(consumeA, "balance_after_cent"), contains(8_000)))
                .andExpect(jsonPath(row(consumeA, "card_type"), contains("general")))
                .andExpect(jsonPath(row(consumeA, "masked_phone"), contains("139****" + phone.substring(7))))
                .andExpect(jsonPath(row(reversalA, "principal_after_cent"), contains(8_000)))
                .andExpect(jsonPath(row(reversalA, "original_txn_id"), contains(consumeA)))
                .andExpect(jsonPath(row(barConsumeA, "principal_after_cent"), contains(7_500)))
                .andExpect(jsonPath(row(barConsumeA, "consume_merchant_id"), contains(merchantM)))
                .andExpect(jsonPath(row(barConsumeA, "consume_merchant_name"), contains("储值台账回归酒吧" + s)))
                .andExpect(jsonPath(row(topupM, "card_type"), contains("merchant")))
                .andExpect(jsonPath(row(topupM, "issuer_merchant_name"), contains("储值台账回归酒吧" + s)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> ids = JsonPath.read(json, "$.data.items[*].txn_id");
        assertThat(ids).startsWith(barConsumeA, reversalA, consumeA).doesNotContain(topupA);
        assertThat(json).doesNotContain(phone);
    }

    /** ② 按商户 M 查：只含 M 发的卡的流水与在 M 扣款的那笔通用卡消费，不含 B 卡与不在 M 扣的流水。 */
    @Test
    void merchantFilterKeepsIssuedCardAndConsumptionAtMerchant() throws Exception {
        assertThat(ids(admin(), "{\"phone\":\"" + phone + "\",\"merchant_id\":\"" + merchantM + "\"}"))
                .containsExactlyInAnyOrder(topupM, barConsumeA);
    }

    /** ③ 期间外的流水不出；不带期间时全部出（含 40 天前的充值）。 */
    @Test
    void periodFilterExcludesOutside() throws Exception {
        assertThat(ids(admin(), "{\"phone\":\"" + phone + "\",\"date_from\":\"" + yesterday() + "\"}")).doesNotContain(topupA)
                .contains(consumeA);
        assertThat(ids(admin(), "{\"phone\":\"" + phone + "\"}"))
                .containsExactlyInAnyOrder(topupA, consumeA, reversalA, barConsumeA, topupB, topupM);
    }

    /** ④ 范围：本人项目 A 的普通账号不见 B 卡；持勾选 B 的可跨项目角色的账号见 B 卡；商户 M 账号只见 M 的卡；商户 N 账号见不到（N 没发卡）。 */
    @Test
    void scopeFollowsAccount() throws Exception {
        String body = "{\"phone\":\"" + phone + "\"}";
        assertThat(ids(staffs.staff("r364usr_pa_" + s, "staff", projectA, null, VIEW), body))
                .containsExactlyInAnyOrder(topupA, consumeA, reversalA, barConsumeA, topupM);
        assertThat(ids(crossProjectToB(), body)).containsExactly(topupB);
        assertThat(ids(staffs.staff("r364usr_m_" + s, "merchant_staff", projectA, merchantM, VIEW), body)).containsExactly(topupM);
        assertThat(ids(staffs.staff("r364usr_n_" + s, "merchant_staff", projectB, merchantN, VIEW), body)).isEmpty();
    }

    /** ⑤ 不持查看码 403 PERMISSION_DENIED；每页 0 条 400 VALIDATION_FAILED。 */
    @Test
    void permissionAndPagingAreChecked() throws Exception {
        query(staffs.staff("r364usr_no_" + s, "staff", projectA, null, "customer.account.manage"), "{}")
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
        query(admin(), "{\"size\":0}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
    }

    // ---------- 夹具与请求 ----------

    private String card(String projectId, String merchantId, long balance, long principal, long bonus) {
        String id = "svacc_" + UUID.randomUUID().toString().replace("-", "");
        customerRepository.saveStoredValueAccount(new CustomerStoredValueAccountRecord(id, customer, projectId, merchantId, balance, "active",
                Instant.now(), Instant.now(), merchantId == null ? null : "merchant_prepaid", principal, bonus));
        return id;
    }

    private String txn(String account, String type, long principal, long bonus, long balanceAfter, Instant at, String original, String key) {
        String id = "svtxn_r364_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.update("INSERT INTO customer_stored_value_transactions (id, account_id, customer_id, txn_type, amount_cent, balance_after_cent,"
                + " reason, idempotency_key, created_at, principal_cent, bonus_cent, original_txn_id) VALUES (?, ?, ?, ?, ?, ?, '储值台账回归', ?, ?, ?, ?, ?)",
                id, account, customer, type, principal + bonus, balanceAfter, key == null ? "r364-" + id : key, Timestamp.from(at),
                principal, bonus, original);
        return id;
    }

    /** 酒吧 M 的一张账单（结算行外键到账单）。 */
    private String barBill(Instant now) {
        String bar = "r364bar_" + s;
        String table = "r364tbl_" + s;
        String order = "r364ord_" + s;
        String bill = "r364bill_" + s;
        bars.saveTable(new BarTableRecord(table, bar, merchantM, "T-" + s, "储值台账回归卡座", "card_seat", 4, 1, 0L, "CNY", "active", now, now));
        bars.saveOrder(new BarOrderRecord(order, bar, merchantM, table, null, customer, "staff", "billed", 500L, "CNY", "web",
                null, null, now, now));
        bars.saveBill(new BarBillRecord(bill, "BILL-R364-" + s, order, bar, merchantM, table, null,
                500L, 0L, 0L, 500L, 500L, "CNY", "paid", "settled", null, null, now, now));
        return bill;
    }

    private static String today() {
        return LocalDate.now(LocalZone.ID).toString();
    }

    private static String yesterday() {
        return LocalDate.now(LocalZone.ID).minusDays(1).toString();
    }

    private static String row(String txnId, String field) {
        return "$.data.items[?(@.txn_id == '" + txnId + "')]." + field;
    }

    private RequestPostProcessor admin() {
        return staffs.staff("r364usr_admin_" + s, "platform_admin", null, null, VIEW);
    }

    /** 组织为项目 A、只经勾选了 B 的可跨项目角色持查看码的账号（PGM:659「逐个勾选的只能查看被选中的项目」）。 */
    private RequestPostProcessor crossProjectToB() {
        String role = "role_r364_" + s;
        accessRepository.createRole(new RoleRecord(role, "储值台账回归跨项目角色" + s, null, "project", "active", Instant.now(),
                List.of(VIEW), null, false, List.of(projectB)));
        String userId = "usr_r364_x_" + s;
        accessRepository.createUser(new AccessUser(userId, userId, "staff", projectA, null, "active", Instant.now()));
        accessRepository.assignUserRole(userId, role);
        AccessActor actor = new AccessActor(userId, userId, "staff", projectA, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private ResultActions query(RequestPostProcessor who, String body) throws Exception {
        return mockMvc.perform(post(LEDGER).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private List<String> ids(RequestPostProcessor who, String body) throws Exception {
        return JsonPath.read(query(who, body).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].txn_id");
    }
}
