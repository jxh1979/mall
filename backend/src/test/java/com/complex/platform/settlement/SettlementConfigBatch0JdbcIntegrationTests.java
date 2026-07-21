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

    private final List<String> retentionIds = new ArrayList<>();
    private final List<String> attributionIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
    }

    @AfterEach
    void cleanup() {
        TestSecurityActors.clear();
        for (String id : retentionIds) {
            jdbcTemplate.update("DELETE FROM platform_retention_rules WHERE id = ?", id);
        }
        for (String id : attributionIds) {
            jdbcTemplate.update("DELETE FROM merchant_revenue_attribution_rules WHERE id = ?", id);
        }
    }

    /** ① 草案不生效:project 特例 is_draft=TRUE 不被 finder 采用(回落全局默认 2000);转正 FALSE 后才生效为 3333。 */
    @Test
    void draftConfigNotApplied_untilActivated() {
        String pid = "pv326" + Long.toHexString(System.nanoTime() & 0xFFFFFFFL);
        String id = "prr_b0_" + Long.toHexString(System.nanoTime());
        retentionIds.add(id);
        jdbcTemplate.update(
                "INSERT INTO platform_retention_rules (id, project_id, retention_bps, status, is_draft)"
                        + " VALUES (?, ?, 3333, 'active', TRUE)", id, pid);

        // 草案态:不采用 → 回落全局默认种子(2000)
        assertThat(billRepository.findRetentionBps(pid)).isEqualTo(2000);

        // 转正为正式:生效
        jdbcTemplate.update("UPDATE platform_retention_rules SET is_draft = FALSE WHERE id = ?", id);
        assertThat(billRepository.findRetentionBps(pid)).isEqualTo(3333);
    }

    /** ② V326 现存种子一次性转正:迁移后全局默认种子 is_draft=FALSE,读得到种子(零行为变化)。 */
    @Test
    void migrationActivatedExistingSeeds() {
        Boolean retentionSeedDraft = jdbcTemplate.queryForObject(
                "SELECT is_draft FROM platform_retention_rules WHERE project_id IS NULL", Boolean.class);
        assertThat(retentionSeedDraft).isFalse();
        assertThat(billRepository.findRetentionBps(null)).isEqualTo(2000);

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
