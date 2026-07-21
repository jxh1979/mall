package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * V268 结算重构收尾:综合服务费(§7.2)+套票折扣归属(§7.4)端到端验收。
 *
 * <p>综合服务费落点=月度分账引擎的专属商户直付线(单独门票走此线)。逐家真实断言金额(资金铁律)。
 * <ol>
 *   <li><b>默认 0% = 零行为变更</b>:未配置费率时专属商户仍拿全额,bill.comp_service_fee_cent=0;</li>
 *   <li><b>配置非零费率</b>:商户拿净额(收入-服务费),服务费归项目记入 bill.comp_service_fee_cent;</li>
 *   <li><b>多专属商户各自独立</b>:各家净额独立、费率按商户/票种精确命中,N 家 N 条独立明细(不串户/不撞键);</li>
 *   <li><b>套票折扣归属</b>:默认 platform_absorb;项目特例可覆盖。</li>
 * </ol>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementV268Tests {

    @Autowired
    private TicketDistributionSettlementService settlementService;

    @Autowired
    private JdbcSettlementBillRepository billRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final YearMonth MONTH = YearMonth.of(2012, 5); // 远离今日的确定月
    private final List<String> projectIds = new ArrayList<>();
    private final List<String> feeConfigIds = new ArrayList<>();
    private final List<String> discountConfigIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void cleanup() {
        TestSecurityActors.clear();
        for (String pid : projectIds) {
            jdbcTemplate.update("DELETE FROM settlement_transactions WHERE project_id = ?", pid);
            jdbcTemplate.update(
                    "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                    pid);
            jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", pid);
        }
        for (String id : feeConfigIds) {
            jdbcTemplate.update("DELETE FROM comprehensive_service_fee_configs WHERE id = ?", id);
        }
        for (String id : discountConfigIds) {
            jdbcTemplate.update("DELETE FROM ticket_combo_discount_configs WHERE id = ?", id);
        }
    }

    /** 默认无费率配置 → 专属商户拿全额(零行为变更);bill.comp_service_fee_cent=0。 */
    @Test
    void defaultZeroFee_merchantGetsFullAmount_noBehaviorChange() {
        String pid = newProject();
        seedExclusive(pid, "barA", "ticket", 10, 5, 100000L);

        settlementService.settleMonth(pid, MONTH);

        assertThat(exclusiveAmount(pid, "barA")).isEqualTo(100000L); // 全额,未扣费
        assertThat(num(bill(pid), "comp_service_fee_cent")).isEqualTo(0L);
    }

    /** 配置某商户某票种 10% 服务费 → 商户拿净额 90000,服务费 10000 归项目。 */
    @Test
    void configuredFee_merchantGetsNet_feeToProject() {
        String pid = newProject();
        seedExclusive(pid, "barA", "ticket", 10, 5, 100000L);
        seedFee(pid, "barA", "ticket", 1000); // 10%

        settlementService.settleMonth(pid, MONTH);

        assertThat(exclusiveAmount(pid, "barA")).isEqualTo(90000L); // 100000 - 10%
        assertThat(num(bill(pid), "comp_service_fee_cent")).isEqualTo(10000L);
    }

    /**
     * 多专属商户各自独立扣费(资金铁律:N 家 N 条独立明细、金额互不相撞/不串户)。
     * barA 配 10%(ticket)、barB 无配置(0%)、barC 配 25%(amusement,不同票种)。
     */
    @Test
    void multiExclusiveMerchants_independentFeesAndItems() {
        String pid = newProject();
        seedExclusive(pid, "barA", "ticket", 10, 5, 100000L);
        seedExclusive(pid, "barB", "ticket", 8, 4, 60000L);
        seedExclusive(pid, "barC", "amusement", 6, 3, 40000L);
        seedFee(pid, "barA", "ticket", 1000);      // barA 10%
        seedFee(pid, "barC", "amusement", 2500);   // barC 25%（不同票种范围）

        settlementService.settleMonth(pid, MONTH);

        assertThat(exclusiveAmount(pid, "barA")).isEqualTo(90000L);  // 100000 - 10000
        assertThat(exclusiveAmount(pid, "barB")).isEqualTo(60000L);  // 无配置,全额
        assertThat(exclusiveAmount(pid, "barC")).isEqualTo(30000L);  // 40000 - 10000
        // 项目综合服务费合计 = 10000 + 0 + 10000
        assertThat(num(bill(pid), "comp_service_fee_cent")).isEqualTo(20000L);
        // 恰好 3 条专属明细,各家一条(不串户)
        long exclCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM settlement_items si JOIN settlement_bills sb ON sb.id = si.bill_id"
                        + " WHERE sb.project_id = ? AND si.distribution_method = 'exclusive_full'",
                Long.class, pid);
        assertThat(exclCount).isEqualTo(3L);
    }

    /**
     * 费率按票种范围精确命中:barA 有 ticket 专属(配 10%)+ amusement 专属(未配 → 0%),
     * 两票种净额合并成一条明细 = (ticket 100000×90%) + (amusement 40000×100%) = 90000 + 40000 = 130000。
     * 证明按 entitlement_type 拆聚合命中不同费率是对的。
     */
    @Test
    void feeScopedByEntitlementType_mergedIntoOneMerchantItem() {
        String pid = newProject();
        seedExclusive(pid, "barA", "ticket", 10, 5, 100000L);
        seedExclusive(pid, "barA", "amusement", 4, 2, 40000L);
        seedFee(pid, "barA", "ticket", 1000); // 仅 ticket 10%,amusement 未配=0%

        settlementService.settleMonth(pid, MONTH);

        assertThat(exclusiveAmount(pid, "barA")).isEqualTo(130000L); // 90000 + 40000
        assertThat(num(bill(pid), "comp_service_fee_cent")).isEqualTo(10000L); // 仅 ticket 那笔
    }

    /** 套票折扣归属:默认 platform_absorb;项目特例可覆盖为 merchant_absorb。 */
    @Test
    void comboDiscountConfig_defaultAndProjectOverride() {
        // 全局默认(无项目特例)
        var def = billRepository.findComboDiscountConfig("no_such_project_zzz");
        assertThat(def.discountBearer()).isEqualTo("platform_absorb");

        String pid = newProject();
        String cid = "tcdc_test_" + Long.toHexString(System.nanoTime());
        discountConfigIds.add(cid);
        jdbcTemplate.update(
                "INSERT INTO ticket_combo_discount_configs (id, project_id, discount_bearer, split_platform_bps, is_draft)"
                        + " VALUES (?, ?, 'merchant_absorb', 0, FALSE)", cid, pid);
        var override = billRepository.findComboDiscountConfig(pid);
        assertThat(override.discountBearer()).isEqualTo("merchant_absorb");
    }

    // ---- helpers ----

    private String newProject() {
        String pid = "pv268" + Long.toHexString(System.nanoTime() & 0xFFFFFFFL);
        projectIds.add(pid);
        return pid;
    }

    private void seedFee(String projectId, String merchantId, String entType, int feeBps) {
        String id = "csfc_test_" + Long.toHexString(System.nanoTime());
        feeConfigIds.add(id);
        jdbcTemplate.update(
                "INSERT INTO comprehensive_service_fee_configs (id, project_id, merchant_id, entitlement_type,"
                        + " fee_bps, is_draft) VALUES (?, ?, ?, ?, ?, FALSE)",
                id, projectId, merchantId, entType, feeBps);
    }

    private void seedExclusive(String projectId, String exclusiveMerchantId, String entType,
                              int verifiedCount, int numPeople, long verifiedCent) {
        Instant createdAt = MONTH.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(createdAt);
        String id = "st_v268_" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,"
                        + " entitlement_type, verified_count, total_times, num_people, allocated_revenue_cent,"
                        + " verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,"
                        + " settlement_status, entitlement_valid_to, exclusive_merchant_id, created_at, updated_at)"
                        + " VALUES (?, ?, 'verification_settled', ?, ?, ?, ?, ?, ?, ?, 0, 'merchant', ?, ?,"
                        + " 'draft', ?, ?, ?, ?)",
                id, "TN-" + id, "ent_" + id, entType, verifiedCount,
                Math.max(verifiedCount, 1), numPeople, verifiedCent, verifiedCent,
                exclusiveMerchantId, projectId, ts, exclusiveMerchantId, ts, ts);
    }

    private Map<String, Object> bill(String projectId) {
        return jdbcTemplate.queryForMap("SELECT * FROM settlement_bills WHERE project_id = ?", projectId);
    }

    private long exclusiveAmount(String projectId, String merchantId) {
        return jdbcTemplate.queryForObject(
                "SELECT si.final_amount_cent FROM settlement_items si"
                        + " JOIN settlement_bills sb ON sb.id = si.bill_id"
                        + " WHERE sb.project_id = ? AND si.merchant_id = ? AND si.distribution_method = 'exclusive_full'",
                Long.class, projectId, merchantId);
    }

    private static long num(Map<String, Object> row, String col) {
        return ((Number) row.get(col)).longValue();
    }
}
