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
 *   <li><b>默认 0% = 零行为变更</b>:未配置费率时专属商户仍拿全额,bill.comp_service_fee_exclusive_cent=0;</li>
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
    private final java.util.Set<String> ticketTypeIds = new java.util.LinkedHashSet<>();

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void cleanup() {
        TestSecurityActors.clear();
        for (String pid : projectIds) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, "DELETE FROM settlement_transactions WHERE project_id = ?", pid);
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
        // 票种必须在费率规则之后删(fk_csfc_ticket_type)。
        for (String id : ticketTypeIds) {
            jdbcTemplate.update("DELETE FROM ticket_types WHERE id = ?", id);
        }
        ticketTypeIds.clear();
    }

    /** 默认无费率配置 → 专属商户拿全额(零行为变更);bill.comp_service_fee_cent=0。 */
    @Test
    void defaultZeroFee_merchantGetsFullAmount_noBehaviorChange() {
        String pid = newProject();
        seedExclusive(pid, "barA", "ticket", 10, 5, 100000L);

        settle(pid);

        assertThat(exclusiveAmount(pid, "barA")).isEqualTo(100000L); // 全额,未扣费
        assertThat(num(bill(pid), "comp_service_fee_exclusive_cent")).isEqualTo(0L);
    }

    /** 配置某商户某票种 10% 服务费 → 商户拿净额 90000,服务费 10000 归项目。 */
    @Test
    void configuredFee_merchantGetsNet_feeToProject() {
        String pid = newProject();
        seedExclusive(pid, "barA", "ticket", 10, 5, 100000L);
        seedFee(pid, "barA", "ticket", 1000); // 10%

        settle(pid);

        assertThat(exclusiveAmount(pid, "barA")).isEqualTo(90000L); // 100000 - 10%
        assertThat(num(bill(pid), "comp_service_fee_exclusive_cent")).isEqualTo(10000L);
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

        settle(pid);

        assertThat(exclusiveAmount(pid, "barA")).isEqualTo(90000L);  // 100000 - 10000
        assertThat(exclusiveAmount(pid, "barB")).isEqualTo(60000L);  // 无配置,全额
        assertThat(exclusiveAmount(pid, "barC")).isEqualTo(30000L);  // 40000 - 10000
        // 项目综合服务费合计 = 10000 + 0 + 10000
        assertThat(num(bill(pid), "comp_service_fee_exclusive_cent")).isEqualTo(20000L);
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
     * S2:证明费率按【票种】精确命中 —— 原来按权益类型,与「可按（项目 × 商户 × 票种）范围配置」的业务口径不符。
     */
    @Test
    void feeScopedByTicketType_mergedIntoOneMerchantItem() {
        String pid = newProject();
        seedExclusive(pid, "barA", "ticket", 10, 5, 100000L);
        seedExclusive(pid, "barA", "amusement", 4, 2, 40000L);
        seedFee(pid, "barA", "ticket", 1000); // 仅 ticket 10%,amusement 未配=0%

        settle(pid);

        assertThat(exclusiveAmount(pid, "barA")).isEqualTo(130000L); // 90000 + 40000
        assertThat(num(bill(pid), "comp_service_fee_exclusive_cent")).isEqualTo(10000L); // 仅 ticket 那笔
    }

    /**
     * 综合服务费固定额 = 每张票固定额 × 核销张数(用户 2026-07-21 拍板 A 口径,非整包价)。
     * 核销 5 张、固定额 100 分/张 → 服务费 = 100×5 = 500;商户拿净额 100000−500 = 99500。
     */
    @Test
    void fixedAmountFee_chargedPerVerifiedTicket() {
        String pid = newProject();
        seedExclusive(pid, "barA", "ticket", 5, 3, 100000L); // 核销 5 张
        seedFixedFee(pid, "barA", "ticket", 100L);            // 固定额 100 分/张

        settle(pid);

        assertThat(num(bill(pid), "comp_service_fee_exclusive_cent")).isEqualTo(500L); // 100 × 5 张(非整包 100)
        assertThat(exclusiveAmount(pid, "barA")).isEqualTo(99500L);          // 100000 − 500
    }

    /** 固定额上限本笔毛额:固定额×张数 > 收入 → 钳到收入(净额=0,防负)。 */
    @Test
    void fixedAmountFee_cappedAtGross() {
        String pid = newProject();
        seedExclusive(pid, "barA", "ticket", 5, 3, 300L);    // 核销 5 张,收入仅 300
        seedFixedFee(pid, "barA", "ticket", 100L);            // 100×5=500 > 300 → 钳 300

        settle(pid);

        assertThat(num(bill(pid), "comp_service_fee_exclusive_cent")).isEqualTo(300L); // min(500, 300)
        assertThat(exclusiveAmount(pid, "barA")).isEqualTo(0L);              // 净额不为负
    }

    // ── V336 综合服务费月度封顶(可配·默认不封顶·比例与固定额两模式共用) ──────────────

    /**
     * 比例费封顶:barA 配 10%(ticket)+ 封顶 5000。收入 100000 → 未封顶 fee=10000,封顶后 = min(10000, 5000)=5000。
     * 商户拿净额 95000,服务费 5000 归项目。
     */
    @Test
    void ratioFee_clampedByMonthlyCap() {
        String pid = newProject();
        seedExclusive(pid, "barA", "ticket", 10, 5, 100000L);
        seedRatioFeeWithCap(pid, "barA", "ticket", 1000, 5000L); // 10% 封顶 5000

        settle(pid);

        assertThat(num(bill(pid), "comp_service_fee_exclusive_cent")).isEqualTo(5000L);  // min(10000, 5000)
        assertThat(exclusiveAmount(pid, "barA")).isEqualTo(95000L);            // 100000 - 5000
    }

    /**
     * 固定额费封顶:barA 固定额 100/张 × 核销 5 张 = 500,封顶 300 → fee = min(500, 300)=300。
     * 商户拿净额 99700,服务费 300 归项目。
     */
    @Test
    void fixedAmountFee_clampedByMonthlyCap() {
        String pid = newProject();
        seedExclusive(pid, "barA", "ticket", 5, 3, 100000L);   // 核销 5 张
        seedFixedFeeWithCap(pid, "barA", "ticket", 100L, 300L); // 100×5=500,封顶 300

        settle(pid);

        assertThat(num(bill(pid), "comp_service_fee_exclusive_cent")).isEqualTo(300L);  // min(500, 300)
        assertThat(exclusiveAmount(pid, "barA")).isEqualTo(99700L);          // 100000 - 300
    }

    /** 封顶未触顶(cap > fee):10% fee=10000 < 封顶 20000 → 不封,fee 仍 10000(封顶只压不抬)。 */
    @Test
    void cap_notHit_feeUnchanged() {
        String pid = newProject();
        seedExclusive(pid, "barA", "ticket", 10, 5, 100000L);
        seedRatioFeeWithCap(pid, "barA", "ticket", 1000, 20000L); // 10% 封顶 20000(高于 fee)

        settle(pid);

        assertThat(num(bill(pid), "comp_service_fee_exclusive_cent")).isEqualTo(10000L); // 未触顶,原样
        assertThat(exclusiveAmount(pid, "barA")).isEqualTo(90000L);
    }

    /**
     * V336 封顶按「商户×规则」聚合(用户 A 口径核心):同一商户多票种落【同一条商户级规则】(ticket_type_id 留空)时,
     * 封顶压的是该商户在该规则下当月<b>合计</b>费,而非逐票种各压一次(逐票种各压=N×封顶,违背「商户级规则=该商户当月封顶」)。
     * barA 两票种各原始费 10%×100000=10000,合计 20000;商户级封顶 15000 → 压到 15000(非各压后的 20000)。
     */
    @Test
    void capAggregatesPerMerchantRuleAcrossTicketTypes() {
        String pid = newProject();
        seedExclusive(pid, "barA", "ticket", 5, 3, 100000L);
        seedExclusive(pid, "barA", "amusement", 4, 2, 100000L);
        seedRatioFeeWithCap(pid, "barA", null, 1000, 15000L); // 商户级(票种留空)10% + 封顶 15000

        settle(pid);

        assertThat(num(bill(pid), "comp_service_fee_exclusive_cent")).isEqualTo(15000L); // 聚合 20000 → 封 15000(非逐票种各压=20000)
        assertThat(exclusiveAmount(pid, "barA")).isEqualTo(185000L);           // 200000 - 15000
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

    /**
     * P6:测试直插 settlement_transactions 绕过了 {@link SettlementSettledConsumer},故在结算前回填每行实收综合服务费——
     * 镜像消费者口径(按 projectId 解析费率+折扣,每笔算 fee,按「商户×规则」当月增量封顶),使 settleMonth 线性求和 = 旧月末重算。
     * 处理顺序 ORDER BY id 确定,增量封顶查已回填行的月累计。
     */
    private void applyFees(String projectId) {
        var rows = jdbcTemplate.queryForList(
                "SELECT id, exclusive_merchant_id, ticket_type_id, verified_amount_cent, verified_count,"
                        + " discount_amount_cent FROM settlement_transactions"
                        + " WHERE project_id = ? AND transaction_type = 'verification_settled'"
                        + " AND exclusive_merchant_id IS NOT NULL AND comp_service_fee_cent = 0 ORDER BY id",
                projectId);
        Instant ms = MONTH.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant me = MONTH.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        for (var r : rows) {
            String merchant = (String) r.get("exclusive_merchant_id");
            String ticketTypeId = (String) r.get("ticket_type_id");
            long verified = ((Number) r.get("verified_amount_cent")).longValue();
            int count = ((Number) r.get("verified_count")).intValue();
            long discount = ((Number) r.get("discount_amount_cent")).longValue();
            var cfg = billRepository.findComprehensiveServiceFee(projectId, merchant, ticketTypeId);
            long platformBorne = ComprehensiveServiceFee.platformBorneCent(
                    billRepository.findComboDiscountConfig(projectId), discount);
            long fee = ComprehensiveServiceFee.perTicketCent(cfg, verified + platformBorne, count);
            String cfgId = null;
            if (fee > 0L) {
                if (cfg.monthlyCapAmountCent() != null) {
                    long mtd = billRepository.sumMonthToDateServiceFee(merchant, cfg.id(), ms, me);
                    fee = Math.min(fee, Math.max(0L, cfg.monthlyCapAmountCent() - mtd));
                }
                cfgId = cfg.id();
            }
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate,
                    "UPDATE settlement_transactions SET comp_service_fee_cent = ?, comp_service_fee_config_id = ?"
                            + " WHERE id = ?", fee, cfgId, r.get("id"));
        }
    }

    private void settle(String projectId) {
        applyFees(projectId);
        settlementService.settleMonth(projectId, MONTH);
    }


    private String newProject() {
        String pid = "pv268" + Long.toHexString(System.nanoTime() & 0xFFFFFFFL);
        projectIds.add(pid);
        // S4/V370:全局默认费率已按等价保值种成 2000(原池内留存平移)。本类验的是【专属线按商户/票种谈的费率】,
        // 「无配置商户拿全额」语义靠项目级回退 0 兜住(= 给该项目的专属商户显式补 0 费率的口径)。
        String fallbackId = "csfc_fb_" + pid;
        feeConfigIds.add(fallbackId);
        jdbcTemplate.update(
                "INSERT INTO comprehensive_service_fee_configs (id, project_id, merchant_id, ticket_type_id,"
                        + " fee_bps, is_draft) VALUES (?, ?, NULL, NULL, 0, FALSE)", fallbackId, pid);
        return pid;
    }

    private void seedFee(String projectId, String merchantId, String label, int feeBps) {
        String id = "csfc_test_" + Long.toHexString(System.nanoTime());
        feeConfigIds.add(id);
        jdbcTemplate.update(
                "INSERT INTO comprehensive_service_fee_configs (id, project_id, merchant_id, ticket_type_id,"
                        + " fee_bps, is_draft) VALUES (?, ?, ?, ?, ?, FALSE)",
                id, projectId, merchantId, ticketType(label), feeBps);
    }

    /** 固定额综合服务费(fee_bps=0 + fixed_amount_cent,满足 V328 chk_csfc_fee_mode 二选一)。 */
    private void seedFixedFee(String projectId, String merchantId, String label, long fixedAmountCent) {
        String id = "csfc_fx_" + Long.toHexString(System.nanoTime());
        feeConfigIds.add(id);
        jdbcTemplate.update(
                "INSERT INTO comprehensive_service_fee_configs (id, project_id, merchant_id, ticket_type_id,"
                        + " fee_bps, fixed_amount_cent, is_draft) VALUES (?, ?, ?, ?, 0, ?, FALSE)",
                id, projectId, merchantId, ticketType(label), fixedAmountCent);
    }

    /** V336 比例费 + 月度封顶(fee_bps>0 + monthly_cap_amount_cent)。 */
    private void seedRatioFeeWithCap(String projectId, String merchantId, String label, int feeBps, long capCent) {
        String id = "csfc_rc_" + Long.toHexString(System.nanoTime());
        feeConfigIds.add(id);
        jdbcTemplate.update(
                "INSERT INTO comprehensive_service_fee_configs (id, project_id, merchant_id, ticket_type_id,"
                        + " fee_bps, monthly_cap_amount_cent, is_draft) VALUES (?, ?, ?, ?, ?, ?, FALSE)",
                id, projectId, merchantId, ticketType(label), feeBps, capCent);
    }

    /** V336 固定额费 + 月度封顶(fee_bps=0 + fixed_amount_cent + monthly_cap_amount_cent)。 */
    private void seedFixedFeeWithCap(String projectId, String merchantId, String label,
                                     long fixedAmountCent, long capCent) {
        String id = "csfc_fc_" + Long.toHexString(System.nanoTime());
        feeConfigIds.add(id);
        jdbcTemplate.update(
                "INSERT INTO comprehensive_service_fee_configs (id, project_id, merchant_id, ticket_type_id,"
                        + " fee_bps, fixed_amount_cent, monthly_cap_amount_cent, is_draft) VALUES (?, ?, ?, ?, 0, ?, ?, FALSE)",
                id, projectId, merchantId, ticketType(label), fixedAmountCent, capCent);
    }

    private void seedExclusive(String projectId, String exclusiveMerchantId, String label,
                              int verifiedCount, int numPeople, long verifiedCent) {
        Instant createdAt = MONTH.atDay(15).atTime(12, 0).toInstant(ZoneOffset.UTC);
        java.sql.Timestamp ts = java.sql.Timestamp.from(createdAt);
        String id = "st_v268_" + System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO settlement_transactions (id, transaction_no, transaction_type, entitlement_id,"
                        + " entitlement_type, ticket_type_id, verified_count, total_times, num_people, allocated_revenue_cent,"
                        + " verified_amount_cent, unverified_amount_cent, beneficiary_type, merchant_id, project_id,"
                        + " settlement_status, entitlement_valid_to, exclusive_merchant_id, verification_id,"
                        + " created_at, updated_at)"
                        + " VALUES (?, ?, 'verification_settled', ?, ?, ?, ?, ?, ?, ?, ?, 0, 'merchant', ?, ?,"
                        + " 'draft', ?, ?, ?, ?, ?)",
                id, "TN-" + id, "ent_" + id, label, ticketType(label), verifiedCount,
                Math.max(verifiedCount, 1), numPeople, verifiedCent, verifiedCent,
                exclusiveMerchantId, projectId, ts, exclusiveMerchantId, "vf_" + id, ts, ts);
    }

    /**
     * S2:票种维度 —— 按标签惰性建一个真票种(V365 的 fk_csfc_ticket_type 要求费率规则引用真实票种)。
     * 同一标签复用同一 id,故「同票种」的费率规则与核销流水能对上;不同标签=不同票种,用来证费率按票种精确命中。
     */
    private String ticketType(String label) {
        if (label == null) {
            return null; // 票种留空 = 该(项目 × 商户)范围内所有票种通用
        }
        String id = "tt_v268_" + label;
        if (ticketTypeIds.add(id)) {
            jdbcTemplate.update(
                    "INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, stock_required, status,"
                            + " created_at, updated_at) VALUES (?, ?, 'fixed_price', FALSE, 'active', now(), now())"
                            + " ON CONFLICT (id) DO NOTHING",
                    id, "V268测试票种-" + label);
        }
        return id;
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
