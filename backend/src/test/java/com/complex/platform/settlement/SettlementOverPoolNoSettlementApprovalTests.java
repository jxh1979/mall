package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.complex.platform.access.AccessActor;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第 75 条 75-R2：结算环节不再设「超池待审批」，超池单照常确认、超出部分由娱乐平台公司承担。
 *
 * <p>依据：ENUM L52「超池的判定与审批一律在事前完成，结算环节不再设“超池待审批”」「超出部分由娱乐平台公司承担」；BSM L752 同义。
 * 原「超池 draft → 平台负责人特批 → confirmed → 过账」整条链路（D7/S9）下线：超池单与普通单一样 confirmed，开过账即过；
 * 超出额照 18B 的两段拆过 platform_incentive_subsidy（娱乐平台公司承担，平台域、project_id 为空），池内额过 ticket_distribution。
 *
 * <p>造超池（按单张票的池快照逐份分账）：一张池快照票（池 800，第一段名单 m1 固定 600、m2 固定 400，Q=1000&gt;800）。
 * 非 @Transactional（settleMonth/postBill 各自真提交）；@AfterEach 按项目清理；远离今日的确定月 2011-04。
 * ar_ap_items 为不可变台账（触发器禁 DELETE），source_id＝唯一明细 id 不与后续用例冲突，不清。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class SettlementOverPoolNoSettlementApprovalTests {

    @Autowired private TicketDistributionSettlementService settlementService;
    @Autowired private SettlementPostingService postingService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;

    private static final YearMonth MONTH = YearMonth.of(2011, 4);
    private final List<String> projectIds = new ArrayList<>();
    private final String suffix = Long.toHexString(System.nanoTime());
    private final String poster = "opna_poster_" + suffix;

    @BeforeEach
    void setUp() {
        // FS-021：过账到期日按财务指定付账日推算，缺规则/日历即拒绝——种已批钱路规则与工作日历。
        com.complex.platform.support.InternalPayDayFixtures.seed(jdbcTemplate);
        // 过账走 FinanceService.createArApItem(@GuardedAction)——需登录主体过鉴权/审计。
        TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void cleanup() {
        for (String pid : projectIds) {
            jdbcTemplate.update(
                    "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)", pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate,
                    "DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            com.complex.platform.support.PassPoolFixtures.deleteByProject(jdbcTemplate, pid);
            jdbcTemplate.update("DELETE FROM ticket_distribution_configs WHERE project_id = ?", pid);
        }
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id = ?", poster);
        jdbcTemplate.update("DELETE FROM access_users WHERE id = ?", poster);
        jdbcTemplate.update("DELETE FROM role_permissions WHERE role_id = ?", "role_" + poster);
        jdbcTemplate.update("DELETE FROM roles WHERE id = ?", "role_" + poster);
        TestSecurityActors.clear();
    }

    /** 第一段合计高于份池的月结单：照常 confirmed，超出额单列 pool_overflow_cent。 */
    @Test
    void overPoolBillIsConfirmedNotDraft() {
        String pid = overPoolProject();

        String bill = settlementService.settleMonth(pid, MONTH);

        assertThat(billStatus(bill)).as("结算环节不再设超池待审批").isEqualTo("confirmed");
        assertThat(num(bill(pid), "available_pool_cent")).isEqualTo(800L);
        assertThat(num(bill(pid), "pool_overflow_cent")).isEqualTo(200L);
    }

    /**
     * 超池单不经任何审批直接过账（按真实请求经网关 POST /api/v1/settlement/bills/{id}/post，平台域＋settlement.bill.post）：
     * 补贴合计＝超池额、记在娱乐平台公司（platform_incentive_subsidy，project_id 为空），池内额过 ticket_distribution。
     */
    @Test
    void overPoolBillPostsWithoutAnyApprovalAndPlatformBearsExcess() throws Exception {
        String pid = overPoolProject();
        String bill = settlementService.settleMonth(pid, MONTH);

        int status = mockMvc.perform(post("/api/v1/settlement/bills/" + bill + "/post").with(poster()))
                .andReturn().getResponse().getStatus();

        assertThat(status).as("超池单不需要任何审批即可过账").isEqualTo(200);
        assertPayable(poolItem(pid, "m1"), "platform_incentive_subsidy", "m1", 120L);
        assertPayable(poolItem(pid, "m2"), "platform_incentive_subsidy", "m2", 80L);
        assertPayable(poolItem(pid, "m1"), "ticket_distribution", "m1", 480L);
        assertPayable(poolItem(pid, "m2"), "ticket_distribution", "m2", 320L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount_cent), 0) FROM ar_ap_items
                 WHERE source_type = 'platform_incentive_subsidy' AND project_id IS NULL AND source_id IN (?, ?)""",
                Long.class, poolItem(pid, "m1"), poolItem(pid, "m2"))).as("补贴合计＝超池额，娱乐平台公司承担").isEqualTo(200L);
        assertThat(billStatus(bill)).isEqualTo("settled");
    }

    /** 同月迟到核销后重结：金额随之重算（不再有特批冻结），仍 confirmed。 */
    @Test
    void resettleRecomputesOverPoolBill() {
        String pid = overPoolProject();
        String bill = settlementService.settleMonth(pid, MONTH);
        assertThat(num(bill(pid), "available_pool_cent")).isEqualTo(800L);

        seedNoSecondSegmentTicket(pid, "m1", 80L); // 迟到的另一张票：池 80 单列未分配
        String again = settlementService.settleMonth(pid, MONTH);

        assertThat(again).isEqualTo(bill);
        assertThat(num(bill(pid), "available_pool_cent")).as("重算了，不冻结").isEqualTo(880L);
        assertThat(billStatus(bill)).isEqualTo("confirmed");
    }

    /** 原超池特批端点下线：按旧前端请求形状（蛇形体）提交不再映射。 */
    @Test
    void specialApprovalEndpointIsGone() throws Exception {
        String pid = overPoolProject();
        String bill = settlementService.settleMonth(pid, MONTH);

        int status = mockMvc.perform(post("/api/v1/settlement/bills/" + bill + "/special-approval").with(poster())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"special_approval_ref\":\"SA-GONE\"}"))
                .andReturn().getResponse().getStatus();

        assertThat(status).isIn(404, 405);
    }

    @Test
    void overPoolPostBillSplitsPoolItemsAndKeepsExclusiveWhole() {
        String pid = overPoolProject();
        // 额外一家专属直付 barX：全额 5000（不占池、永不拆）。
        seedExclusive(pid, "barX", 10, 6, 5000L);
        String bill = settlementService.settleMonth(pid, MONTH);

        int posted = postingService.postBill(bill);
        assertThat(posted).isEqualTo(3); // m1 + m2 + barX 三条明细

        // m1 a=600 → pool=480、excess=120；m2 a=400 → pool=320、excess=80；Σpool=可分配池 800，Σexcess=超池 200。
        assertPayable(poolItem(pid, "m1"), "ticket_distribution", "m1", 480L);
        assertPayable(poolItem(pid, "m1"), "platform_incentive_subsidy", "m1", 120L);
        assertPayable(poolItem(pid, "m2"), "ticket_distribution", "m2", 320L);
        assertPayable(poolItem(pid, "m2"), "platform_incentive_subsidy", "m2", 80L);
        // 专属直付 barX：单条 ticket_distribution 全额 5000、无补贴行。
        String exItem = exclusiveItem(pid, "barX");
        assertPayable(exItem, "ticket_distribution", "barX", 5000L);
        assertThat(payableCount(exItem, "platform_incentive_subsidy")).isZero();
        assertThat(billStatus(bill)).isEqualTo("settled");
    }

    /**
     * 补贴合计恰为超池额（ENUM L52「超出部分由娱乐平台公司承担」）：同单另有一张未覆盖第二段酒吧的池快照票（池 80 单列未分配）
     * 使可分配池为 880，超池额仍是 200——补贴按第一段金额 600／400 拆为 120／80。
     */
    @Test
    void overPoolSubsidyEqualsOverflowWhenPoolAlsoHoldsUndistributed() {
        String pid = overPoolProject();
        seedNoSecondSegmentTicket(pid, "m3", 80L);
        String bill = settlementService.settleMonth(pid, MONTH);
        assertThat(num(bill(pid), "available_pool_cent")).isEqualTo(880L);
        assertThat(num(bill(pid), "pool_overflow_cent")).isEqualTo(200L);

        postingService.postBill(bill);

        assertPayable(poolItem(pid, "m1"), "platform_incentive_subsidy", "m1", 120L);
        assertPayable(poolItem(pid, "m2"), "platform_incentive_subsidy", "m2", 80L);
        assertPayable(poolItem(pid, "m1"), "ticket_distribution", "m1", 480L);
        assertPayable(poolItem(pid, "m2"), "ticket_distribution", "m2", 320L);
    }

    @Test
    void nonOverPoolPostBillSingleTicketDistributionNoSubsidy() {
        String pid = pooledProject(800L, 300L, 200L); // 300+200=500 ≤ 800
        String bill = settlementService.settleMonth(pid, MONTH);

        assertThat(postingService.postBill(bill)).isEqualTo(2);

        assertPayable(poolItem(pid, "m1"), "ticket_distribution", "m1", 300L);
        assertThat(payableCount(poolItem(pid, "m1"), "platform_incentive_subsidy")).isZero();
        assertPayable(poolItem(pid, "m2"), "ticket_distribution", "m2", 200L);
        assertThat(payableCount(poolItem(pid, "m2"), "platform_incentive_subsidy")).isZero();
        assertThat(billStatus(bill)).isEqualTo("settled");
    }

    @Test
    void postBillIsIdempotentAcrossBothPrefixes() {
        String pid = overPoolProject();
        String bill = settlementService.settleMonth(pid, MONTH);
        postingService.postBill(bill); // 首过：每家 td + pis 各一条

        String m1Item = poolItem(pid, "m1");
        String m2Item = poolItem(pid, "m2");
        // 白盒复演：把单打回 confirmed、明细打回 draft（不清 ar_ap）→ 再过账须命中幂等、不双挂。
        jdbcTemplate.update("UPDATE settlement_bills SET status = 'confirmed', settled_at = NULL WHERE id = ?", bill);
        jdbcTemplate.update("UPDATE settlement_items SET status = 'draft' WHERE bill_id = ?", bill);

        postingService.postBill(bill);

        assertThat(payableCount(m1Item, "ticket_distribution")).isEqualTo(1);
        assertThat(payableCount(m1Item, "platform_incentive_subsidy")).isEqualTo(1);
        assertThat(payableCount(m2Item, "ticket_distribution")).isEqualTo(1);
        assertThat(payableCount(m2Item, "platform_incentive_subsidy")).isEqualTo(1);
    }

    /** 溢出安全：a×可分配池 超 long 上限时 BigInteger 中间乘保 pool+excess=a 且 pool≥0。 */
    @Test
    void overflowSafeSplitPreservesConservation() {
        String pid = pooledProject(8_000_000_000L, 5_000_000_000L, 5_000_000_000L);
        String bill = settlementService.settleMonth(pid, MONTH);
        postingService.postBill(bill);

        long a = 5_000_000_000L;
        long poolI = java.math.BigInteger.valueOf(a).multiply(java.math.BigInteger.valueOf(8_000_000_000L))
                .divide(java.math.BigInteger.valueOf(10_000_000_000L)).longValueExact(); // 4e9
        String m1Item = poolItem(pid, "m1");
        assertPayable(m1Item, "ticket_distribution", "m1", poolI);
        assertPayable(m1Item, "platform_incentive_subsidy", "m1", a - poolI);
        assertThat(poolI).isGreaterThan(0L);
    }

    /** 补贴应付 project_id 必为 NULL——落平台主体域，不进任一项目公司聚合。 */
    @Test
    void subsidyPayableHasNullProjectIdOutOfProjectDomain() {
        String pid = overPoolProject();
        String bill = settlementService.settleMonth(pid, MONTH);
        postingService.postBill(bill);

        Object projectIdCol = jdbcTemplate.queryForMap(
                "SELECT project_id FROM ar_ap_items WHERE source_type = 'platform_incentive_subsidy' AND source_id = ?",
                poolItem(pid, "m1")).get("project_id");
        assertThat(projectIdCol).isNull();
    }

    // ─── 造数据 ───────────────────────────────────────────────────────────────

    /** 有过账权限（settlement.bill.post）、不属任何商户的平台账号，按后台网页请求形状。 */
    private RequestPostProcessor poster() {
        String role = "role_" + poster;
        if (jdbcTemplate.queryForObject("SELECT count(*) FROM access_users WHERE id = ?", Integer.class, poster) == 0) {
            jdbcTemplate.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at)"
                    + " VALUES (?, ?, '过账', 'project', 'active', now())", role, role);
            jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'settlement.bill.post')", role);
            jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, status, created_at)"
                    + " VALUES (?, ?, 'platform_admin', 'org', 'active', now())", poster, poster);
            jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", poster, role);
        }
        AccessActor actor = new AccessActor(poster, poster, "platform_admin", "org", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 越池项目：池快照 800，第一段名单 m1 固定 600 / m2 固定 400 → Q=1000 > 可分配池 800。 */
    private String overPoolProject() {
        return pooledProject(800L, 600L, 400L);
    }

    /** 一张池快照票(池 poolCent),第一段名单 m1/m2 各按固定金额,两家都核销入场。 */
    private String pooledProject(long poolCent, long m1Fixed, long m2Fixed) {
        String pid = "proj_opna_" + System.nanoTime();
        projectIds.add(pid);
        jdbcTemplate.update("""
                INSERT INTO ticket_distribution_configs (id, project_id, method_type, status, is_draft)
                VALUES (?, ?, 'by_rank_tier', 'active', FALSE)
                """, "tdc_opna_" + pid, pid);
        String ticket = "tk_opna_" + pid;
        com.complex.platform.support.PassPoolFixtures.snapshot(jdbcTemplate, ticket, pid, poolCent,
                com.complex.platform.support.PassPoolFixtures.FACTORY_SCHEME, List.of("m1", "m2"),
                Map.of("m1", m1Fixed, "m2", m2Fixed));
        insert(pid, "m1", "entry", 1, 3, 0L, null, 0L, ticket);
        insert(pid, "m2", "entry", 1, 2, 0L, null, 0L, ticket);
        return pid;
    }

    /** 一张未覆盖任何第二段酒吧的池快照票（池 poolCent），在 merchantId 入场一次：该份余额单列未分配池余额（ENUM L52）。 */
    private void seedNoSecondSegmentTicket(String projectId, String merchantId, long poolCent) {
        String ticket = "tk_opnau_" + System.nanoTime();
        com.complex.platform.support.PassPoolFixtures.snapshot(jdbcTemplate, ticket, projectId, poolCent);
        insert(projectId, merchantId, "entry", 1, 1, 0L, null, 0L, ticket);
    }

    /** 专属核销行（exclusive_merchant_id=该商户;该商户专属费率 0 → 费恒 0）。 */
    private void seedExclusive(String projectId, String exclusiveMerchantId, int verifiedCount, int numPeople, long verifiedCent) {
        insert(projectId, exclusiveMerchantId, "bar_drink", verifiedCount, numPeople, verifiedCent, exclusiveMerchantId, 0L, null);
    }

    private void insert(String projectId, String merchantId, String entType, int verifiedCount, int numPeople,
                        long verifiedCent, String exclusiveMerchantId, long compServiceFeeCent, String parentTicket) {
        Instant businessAt = MONTH.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(businessAt);
        String id = "st_opna_" + System.nanoTime();
        jdbcTemplate.update("""
                INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,
                    entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,
                    verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,
                    exclusive_merchant_id, verification_id, settlement_status, entitlement_valid_to, created_at, updated_at,
                    business_at, discount_amount_cent, comp_service_fee_cent, parent_ticket_id)
                VALUES (?, ?, 'verification_settled', ?, ?, ?, ?, ?, ?, ?, 0, 'merchant', ?, ?, ?, ?, 'draft', ?, ?, ?, ?, 0, ?, ?)
                """,
                id, "TN-" + id, "ent_" + id, entType, verifiedCount,
                Math.max(verifiedCount, 1), numPeople, verifiedCent, verifiedCent,
                merchantId, projectId, exclusiveMerchantId, "vf_" + id, ts, ts, ts, ts, compServiceFeeCent, parentTicket);
    }

    // ─── 断言/读取 helpers ─────────────────────────────────────────────────────

    private String billStatus(String billId) {
        return jdbcTemplate.queryForObject("SELECT status FROM settlement_bills WHERE id = ?", String.class, billId);
    }

    private Map<String, Object> bill(String projectId) {
        return jdbcTemplate.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ?", projectId);
    }

    private String poolItem(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.id FROM settlement_items si JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ? AND si.distribution_method <> 'exclusive_full'
                """, String.class, projectId, merchantId);
    }

    private String exclusiveItem(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject("""
                SELECT si.id FROM settlement_items si JOIN settlement_bills sb ON sb.id = si.bill_id
                 WHERE sb.project_id = ? AND si.merchant_id = ? AND si.distribution_method = 'exclusive_full'
                """, String.class, projectId, merchantId);
    }

    /** 断言某明细某 source_type 恰有一条应付：direction=payable、counterparty=merchant、amount=expected。 */
    private void assertPayable(String itemId, String sourceType, String merchantId, long expectedCent) {
        Map<String, Object> arap = jdbcTemplate.queryForMap(
                "SELECT * FROM ar_ap_items WHERE source_type = ? AND source_id = ?", sourceType, itemId);
        assertThat(arap.get("direction")).isEqualTo("payable");
        assertThat(arap.get("counterparty_id")).isEqualTo(merchantId);
        assertThat(((Number) arap.get("amount_cent")).longValue()).isEqualTo(expectedCent);
        assertThat(payableCount(itemId, sourceType)).isEqualTo(1);
    }

    private int payableCount(String itemId, String sourceType) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM ar_ap_items WHERE source_type = ? AND source_id = ?", Integer.class, sourceType, itemId);
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
