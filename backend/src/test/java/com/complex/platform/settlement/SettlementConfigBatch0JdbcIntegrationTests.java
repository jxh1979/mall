package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.analytics.AnalyticsRepository;
import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 结算配置中心 · 新立项批0 验收(V326)。
 *
 * <ol>
 *   <li><b>「草案不生效」读取强制</b>:project 特例配 is_draft=TRUE → finder 不采用(回落全局默认);
 *       转正为 FALSE 后才生效。堵住既有「草案即生效」漏洞。</li>
 *   <li><b>V326 现存种子一次性转正</b>:迁移后全局默认种子 is_draft=FALSE(零行为变化)。</li>
 *   <li><b>归因规则草案复核转正 + 前态守卫</b>:activateAttributionRuleDraft 翻 is_draft 返回 1;
 *       仅草案可转正,已正式再转正返回 0。</li>
 * </ol>
 *
 * <p>数据源 URL 经 {@code SETTLE_TEST_DB_URL} 可覆盖:自测时指向一次性临时库(不对共享库 complex_test 跑 Flyway,
 * 与并发会话协调);默认 complex_test 供集成流水线统一跑。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class SettlementConfigBatch0JdbcIntegrationTests {

    @Autowired
    private JdbcSettlementBillRepository billRepository;

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> feeConfigIds = new ArrayList<>();
    private final List<String> attributionIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void cleanup() {
        TestSecurityActors.clear();
        for (String id : feeConfigIds) {
            jdbcTemplate.update("DELETE FROM comprehensive_service_fee_configs WHERE id = ?", id);
        }
        for (String id : attributionIds) {
            jdbcTemplate.update("DELETE FROM merchant_revenue_attribution_rules WHERE id = ?", id);
        }
    }

    /** ① 草案不生效(S4 起以综合服务费为样本,原留存表已停用):project 特例 is_draft=TRUE 不被 finder 采用
     *  (回落全局默认 2000 = V370 等价保值种子);转正 FALSE 后才生效为 3333。 */
    @Test
    void draftConfigNotApplied_untilActivated() {
        String pid = "pv326" + Long.toHexString(System.nanoTime() & 0xFFFFFFFL);
        String id = "csfc_b0_" + Long.toHexString(System.nanoTime());
        feeConfigIds.add(id);
        jdbcTemplate.update(
                "INSERT INTO comprehensive_service_fee_configs (id, project_id, fee_bps, status, is_draft)"
                        + " VALUES (?, ?, 3333, 'active', TRUE)", id, pid);

        // 草案态:不采用 → 回落全局默认种子(2000,V370 由留存率平移)
        assertThat(billRepository.findComprehensiveServiceFee(pid, null, null).feeBps()).isEqualTo(2000);

        // 转正为正式:生效
        jdbcTemplate.update("UPDATE comprehensive_service_fee_configs SET is_draft = FALSE WHERE id = ?", id);
        assertThat(billRepository.findComprehensiveServiceFee(pid, null, null).feeBps()).isEqualTo(3333);
    }

    /** ② 种子形态(V326 转正 + V370 合并留存):综合服务费全局默认行 is_draft=FALSE 且费率=2000
     *  (等价保值:原池内 20% 留存平移为费率);platform_retention_rules 已整体停用(无 active 行)。 */
    @Test
    void migrationActivatedExistingSeeds() {
        Boolean feeSeedDraft = jdbcTemplate.queryForObject(
                "SELECT is_draft FROM comprehensive_service_fee_configs"
                        + " WHERE project_id IS NULL AND merchant_id IS NULL AND ticket_type_id IS NULL"
                        + " AND status = 'active' AND is_draft = FALSE", Boolean.class);
        assertThat(feeSeedDraft).isFalse();
        assertThat(billRepository.findComprehensiveServiceFee(null, null, null).feeBps()).isEqualTo(2000);

        // S4/V370:留存与综合服务费为同一项、只扣一次 → 留存表软删停用,不许再有生效行。
        Integer activeRetention = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM platform_retention_rules WHERE status = 'active'", Integer.class);
        assertThat(activeRetention).isZero();

        Boolean giveawaySeedDraft = jdbcTemplate.queryForObject(
                "SELECT is_draft FROM giveaway_cost_configs WHERE project_id IS NULL", Boolean.class);
        assertThat(giveawaySeedDraft).isFalse();
    }

    /** ③ 归因规则草案复核转正 + 前态守卫:草案→转正返回 1、is_draft 翻 FALSE;已正式再转正返回 0。 */
    @Test
    void attributionDraftActivation_withPrestateGuard() {
        String id = "mrar_b0_" + Long.toHexString(System.nanoTime());
        String mid = "fakeM_b0_" + Long.toHexString(System.nanoTime());
        attributionIds.add(id);
        // 用 merchant 域规则(唯一 merchant_id)避开 default 域 uq_mrar_default_source(thirdparty 已被种子占用)冲突;
        // 转正/前态守卫与作用域无关,验证目的不变。
        jdbcTemplate.update(
                "INSERT INTO merchant_revenue_attribution_rules (id, rule_scope, merchant_id, revenue_source,"
                        + " attribution_mode, share_bps, refund_attribution, status, is_draft)"
                        + " VALUES (?, 'merchant', ?, 'bar', 'share_bps', 5000, 'net_deduct', 'active', TRUE)", id, mid);
        assertThat(draftFlag(id)).isTrue();

        int activated = analyticsRepository.activateAttributionRuleDraft(id, "tester", Instant.now());
        assertThat(activated).isEqualTo(1);
        assertThat(draftFlag(id)).isFalse();

        // 前态守卫:已是正式口径,重复转正返回 0(WHERE is_draft=TRUE 不命中)
        int again = analyticsRepository.activateAttributionRuleDraft(id, "tester", Instant.now());
        assertThat(again).isEqualTo(0);
    }

    private boolean draftFlag(String id) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT is_draft FROM merchant_revenue_attribution_rules WHERE id = ?", Boolean.class, id));
    }
}
