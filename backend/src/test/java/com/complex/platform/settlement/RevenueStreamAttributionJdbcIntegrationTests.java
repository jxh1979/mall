package com.complex.platform.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.TestSecurityActors;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 六业态收入归属(terminology-canonical §三)落库 + 接结算 验收(V318/V319)。
 *
 * <ol>
 *   <li>解析优先级:项目特例 > 平台默认 > 未配置(null);</li>
 *   <li>通票 settleMonth 把归属主体标注进 settlement_bills.attributed_entity_id;</li>
 *   <li>business_entities.parent_entity_id 层级落库可读回(L3→L2 父链)。</li>
 * </ol>
 * 走真实 Flyway 迁移库(complex_test);须主会话串行跑(勿与其它 Flyway 集成测试并发)。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
class RevenueStreamAttributionJdbcIntegrationTests {

    private static final String PROJECT = "proj_rsa_it";
    private static final String ENT_DEFAULT = "be_rsa_ent_default";   // 平台默认娱乐平台公司
    private static final String ENT_PROJECT = "be_rsa_ent_project";   // 项目特例娱乐平台公司
    private static final YearMonth MONTH = YearMonth.of(2013, 5);

    @Autowired
    private RevenueStreamAttributionRepository attributionRepository;

    @Autowired
    private TicketDistributionSettlementService settlementService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        TestSecurityActors.asPlatformAdmin();
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
        TestSecurityActors.clear();
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM revenue_stream_attribution WHERE id LIKE 'rsa_it_%'");
        // S8/V375:全局 all_pass_ticket 归属是种子行——本类对「平台默认」的 upsert 会按唯一键原地改写该行
        //   (id 不变),DELETE 按 id 清不掉 → 显式还原指回娱乐平台公司,不让改写泄漏给后续测试类。
        jdbcTemplate.update("UPDATE revenue_stream_attribution"
                + " SET management_entity_id = 'be_ent_platform', beneficiary_kind = 'entity'"
                + " WHERE revenue_stream = 'all_pass_ticket' AND project_id IS NULL"
                + "   AND status = 'active' AND is_draft = FALSE");
        jdbcTemplate.update(
                "DELETE FROM settlement_items WHERE bill_id IN (SELECT id FROM settlement_bills WHERE project_id = ?)",
                PROJECT);
        jdbcTemplate.update("DELETE FROM settlement_bills WHERE project_id = ?", PROJECT);
        jdbcTemplate.update("DELETE FROM business_entities WHERE id IN (?, ?)", ENT_DEFAULT, ENT_PROJECT);
    }

    @Test
    void resolvePrefersProjectOverrideThenDefaultThenNull() {
        // 项目未配置 → 回落 V375 全局种子(通票收入 → 娱乐平台公司),S8 起不再是 null
        assertThat(attributionRepository.resolveManagementEntity(PROJECT, "all_pass_ticket"))
                .isEqualTo("be_ent_platform");

        // 平台默认
        attributionRepository.upsert(new RevenueStreamAttributionRecord(
                "rsa_it_default", null, "all_pass_ticket", ENT_DEFAULT, "active", "tester", Instant.now()));
        assertThat(attributionRepository.resolveManagementEntity(PROJECT, "all_pass_ticket")).isEqualTo(ENT_DEFAULT);
        assertThat(attributionRepository.resolveManagementEntity(null, "all_pass_ticket")).isEqualTo(ENT_DEFAULT);

        // 项目特例覆盖默认
        attributionRepository.upsert(new RevenueStreamAttributionRecord(
                "rsa_it_project", PROJECT, "all_pass_ticket", ENT_PROJECT, "active", "tester", Instant.now()));
        assertThat(attributionRepository.resolveManagementEntity(PROJECT, "all_pass_ticket")).isEqualTo(ENT_PROJECT);
        // 别的项目仍走默认
        assertThat(attributionRepository.resolveManagementEntity("proj_other", "all_pass_ticket"))
                .isEqualTo(ENT_DEFAULT);

        // 未配置的业态流仍 null
        assertThat(attributionRepository.resolveManagementEntity(PROJECT, "venue_rent")).isNull();

        // upsert 覆盖同键(项目特例改指向默认实体)
        attributionRepository.upsert(new RevenueStreamAttributionRecord(
                "rsa_it_project2", PROJECT, "all_pass_ticket", ENT_DEFAULT, "active", "tester", Instant.now()));
        assertThat(attributionRepository.resolveManagementEntity(PROJECT, "all_pass_ticket")).isEqualTo(ENT_DEFAULT);

        List<RevenueStreamAttributionRecord> all = attributionRepository.list();
        assertThat(all).extracting(RevenueStreamAttributionRecord::revenueStream).contains("all_pass_ticket");
    }

    @Test
    void settleMonthStampsAttributedEntityOnBill() {
        attributionRepository.upsert(new RevenueStreamAttributionRecord(
                "rsa_it_project", PROJECT, "all_pass_ticket", ENT_PROJECT, "active", "tester", Instant.now()));

        // 空项目也会写出一张(零额)结算单;断言归属主体被标注。
        settlementService.settleMonth(PROJECT, MONTH);

        String attributed = jdbcTemplate.queryForObject(
                "SELECT attributed_entity_id FROM settlement_bills WHERE project_id = ?", String.class, PROJECT);
        assertThat(attributed).isEqualTo(ENT_PROJECT);
    }

    @Test
    void businessEntityParentLayerPersists() {
        Instant now = Instant.now();
        // L2 项目公司(父=某平台),L3 管理公司(父=该项目公司)
        jdbcTemplate.update("""
                INSERT INTO business_entities (id, entity_name, entity_role, parent_entity_id, status, created_at, updated_at)
                VALUES (?, ?, 'project_company', NULL, 'active', ?, ?)
                """, ENT_DEFAULT, "项目公司X", java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));
        jdbcTemplate.update("""
                INSERT INTO business_entities (id, entity_name, entity_role, parent_entity_id, status, created_at, updated_at)
                VALUES (?, ?, 'management_company', ?, 'active', ?, ?)
                """, ENT_PROJECT, "娱乐平台公司X", ENT_DEFAULT, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now));

        String parent = jdbcTemplate.queryForObject(
                "SELECT parent_entity_id FROM business_entities WHERE id = ?", String.class, ENT_PROJECT);
        assertThat(parent).isEqualTo(ENT_DEFAULT);
    }
}
