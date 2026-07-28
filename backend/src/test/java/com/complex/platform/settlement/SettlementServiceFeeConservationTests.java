package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.ticketing.SettlementSettledEvent;
import com.complex.platform.ticketing.VerificationReversedEvent;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * P6(2026-07-21)综合服务费<b>跨月红冲守恒</b>验收——过账激活前必修的核心证明。
 *
 * <p>直调真消费者({@link SettlementSettledConsumer}/{@link SettlementReversedConsumer})走生产路径:
 * 核销时按每笔+增量封顶算<b>实收 fee</b> 落库,冲正时严格取<b>原行之反</b>。核心断言=红冲行 comp_service_fee_cent
 * 精确等于原核销行实收之反,而非旧月末对聚合 gross 重算的 min(fixed×count,gross)/min(raw,cap)。
 *
 * <p><b>为何触顶/封顶用例能区分新旧</b>:旧月末重算对负 gross 红冲行套 min → 退的 fee ≠ 原月实收,
 * 触顶时项目倒退、net 可能<0 撞 chk_si_amount。新每笔实收落库 + 取反 → f(-x)=-f(x) 天然守恒。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementServiceFeeConservationTests {

    @Autowired private SettlementSettledConsumer settledConsumer;
    @Autowired private SettlementReversedConsumer reversedConsumer;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<String> merchants = new ArrayList<>();
    private final List<String> configIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (String m : merchants) {
            com.complex.platform.support.AppendOnlyTestBypass.update(jdbcTemplate, "DELETE FROM settlement_transactions WHERE exclusive_merchant_id = ?", m);
        }
        for (String c : configIds) {
            jdbcTemplate.update("DELETE FROM comprehensive_service_fee_configs WHERE id = ?", c);
        }
    }

    /**
     * 触顶固定额跨月红冲:固定额 800/张 > 单笔毛额 500 → 触顶实收 500。
     * 旧月末重算红冲=min(800×-1,-500)=-800 ≠ -500 → 项目倒退 300(不守恒)。
     * 新:红冲行取原行之反=-500,核销+红冲=0(精确守恒)。
     */
    @Test
    void crossMonthReversalOfClampedFixedFee_exactlyClawsBackStoredFee() {
        String merchant = uniqueMerchant();
        String vid = "vclamp_" + Long.toHexString(System.nanoTime());
        Instant m = Instant.parse("2010-08-15T10:00:00Z");
        seedFee(merchant, 0, 800L, null); // 全局商户级固定额 800/张(票种留空=该商户所有票种通用),无封顶

        // ① 核销(专属·毛额 500)→ 真消费者算触顶实收 fee = min(800,500) = 500。
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                vid, m, "ent_" + vid, "ord_x", "tt_x", "entry", merchant, 1, 1, 1, 500L,
                m.plusSeconds(3600), merchant, null, null, null));
        assertThat(feeOf("ST-VS-" + vid)).isEqualTo(500L); // 触顶实收 500(非 800)

        // ② 原月标记已过账 → 红冲跨月落当月(now)。
        seedSettledBill(YearMonth.from(m.atZone(ZoneOffset.UTC)));
        reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                vid, "rev_" + vid, "ent_" + vid, "ord_x", merchant, 1));

        // ③ 核心:红冲行实收 fee = 原行之反 = -500(不是旧 min 重算的 -800)。
        long fVs = feeOf("ST-VS-" + vid);
        long fVr = feeOf("ST-VR-rev_" + vid);
        assertThat(fVr).isEqualTo(-500L);          // 精确取反(守恒核心)
        assertThat(fVs + fVr).isZero();            // 全生命周期 fee 守恒:+500 + (-500) = 0
    }

    /**
     * 月度封顶跨月红冲:固定额 300/张 + 封顶 400。两笔各毛额 1000 → 增量封顶:v1 实收 300、v2 实收 min(300,400-300)=100,
     * 当月合计 400(触顶)。旧月末重算对红冲聚合套 min 会退错;新:各红冲取各自原行之反(-300/-100),两两精确对冲。
     */
    @Test
    void crossMonthReversalUnderMonthlyCap_eachClawsBackItsOwnStoredFee() {
        String merchant = uniqueMerchant();
        String v1 = "vcap1_" + Long.toHexString(System.nanoTime());
        String v2 = "vcap2_" + Long.toHexString(System.nanoTime());
        Instant m = Instant.parse("2010-09-15T10:00:00Z");
        seedFee(merchant, 0, 300L, 400L); // 固定额 300/张 + 封顶 400(票种留空=该商户所有票种通用)

        // ① 两笔核销(同月同商户同规则)→ 增量封顶:v1=300、v2=min(300,剩余100)=100。
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                v1, m, "ent_" + v1, "ord_x", "tt_x", "entry", merchant, 1, 1, 1, 1000L,
                m.plusSeconds(3600), merchant, null, null, null));
        settledConsumer.onSettlementSettled(new SettlementSettledEvent(
                v2, m.plusSeconds(60), "ent_" + v2, "ord_x", "tt_x", "entry", merchant, 1, 1, 1, 1000L,
                m.plusSeconds(3660), merchant, null, null, null));
        assertThat(feeOf("ST-VS-" + v1)).isEqualTo(300L);
        assertThat(feeOf("ST-VS-" + v2)).isEqualTo(100L); // 增量封顶:剩余额度 100
        assertThat(feeOf("ST-VS-" + v1) + feeOf("ST-VS-" + v2)).isEqualTo(400L); // 当月合计=封顶

        // ② 原月已过账 → 两笔各自跨月红冲。
        seedSettledBill(YearMonth.from(m.atZone(ZoneOffset.UTC)));
        reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                v1, "rev_" + v1, "ent_" + v1, "ord_x", merchant, 1));
        reversedConsumer.onVerificationReversed(new VerificationReversedEvent(
                v2, "rev_" + v2, "ent_" + v2, "ord_x", merchant, 1));

        // ③ 各红冲取各自原行之反(-300/-100),封顶下仍精确对冲,全生命周期 fee 守恒 = 0。
        assertThat(feeOf("ST-VR-rev_" + v1)).isEqualTo(-300L);
        assertThat(feeOf("ST-VR-rev_" + v2)).isEqualTo(-100L);
        long total = feeOf("ST-VS-" + v1) + feeOf("ST-VS-" + v2)
                + feeOf("ST-VR-rev_" + v1) + feeOf("ST-VR-rev_" + v2);
        assertThat(total).isZero(); // 封顶跨月红冲守恒
    }

    // ---- helpers ----

    private String uniqueMerchant() {
        String m = "barcons_" + Long.toHexString(System.nanoTime());
        merchants.add(m);
        return m;
    }

    /**
     * 全局(project_id NULL)商户级综合服务费费率(消费者按 project=null 平台口径解析,与生产 settleMonth(null) 一致)。
     * S2:票种维度留空 = 该商户所有票种通用 —— 本测试证的是跨月红冲守恒,与费率作用域怎么切无关。
     */
    private void seedFee(String merchant, int feeBps, Long fixedAmountCent, Long capCent) {
        String id = "csfc_cons_" + Long.toHexString(System.nanoTime());
        configIds.add(id);
        jdbcTemplate.update(
                "INSERT INTO comprehensive_service_fee_configs (id, project_id, merchant_id, ticket_type_id,"
                        + " fee_bps, fixed_amount_cent, monthly_cap_amount_cent, status, is_draft)"
                        + " VALUES (?, NULL, ?, NULL, ?, ?, ?, 'active', FALSE)",
                id, merchant, feeBps, fixedAmountCent, capCent);
    }

    /** 平台口径(project NULL)某月已过账账单 → 令冲正消费者判「原月已过账」把红冲落当月(now)。 */
    private void seedSettledBill(YearMonth month) {
        jdbcTemplate.update(
                "INSERT INTO settlement_bills (id, project_id, period_start, period_end, status, created_at, updated_at)"
                        + " VALUES (?, NULL, ?, ?, 'settled', now(), now())"
                        + " ON CONFLICT (COALESCE(project_id, '__DEFAULT__'), period_start, period_end) DO UPDATE SET status = 'settled'",
                "sb_cons_" + month, java.sql.Date.valueOf(month.atDay(1)), java.sql.Date.valueOf(month.atEndOfMonth()));
    }

    private long feeOf(String transactionNo) {
        return jdbcTemplate.queryForObject(
                "SELECT comp_service_fee_cent FROM settlement_transactions WHERE transaction_no = ?",
                Long.class, transactionNo);
    }
}
