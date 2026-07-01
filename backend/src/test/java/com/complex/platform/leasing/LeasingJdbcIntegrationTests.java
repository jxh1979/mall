package com.complex.platform.leasing;

import static org.assertj.core.api.Assertions.assertThat;

import com.complex.platform.support.MigrationVersions;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 招商获客集成测试（V161 重建）：验迁移建表 + 权限种子 + Flyway 版本 + 仓储往返（含 JSONB）。
 * 注：需独立 rebuild 分支测试库（共享 complex 库仍含旧 V161-170，Flyway 校验会冲突）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex",
        "spring.datasource.username=complex",
        "spring.datasource.password=complex_dev_pwd"
})
@Transactional
class LeasingJdbcIntegrationTests {

    @Autowired
    private JdbcLeasingRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationCreatesLeasingTablesAndSeedsPermissions() {
        assertThat(regclass("leasing_leads")).isEqualTo("leasing_leads");
        assertThat(regclass("leasing_intents")).isEqualTo("leasing_intents");
        assertThat(regclass("leasing_brands")).isEqualTo("leasing_brands");
        assertThat(regclass("leasing_follow_ups")).isEqualTo("leasing_follow_ups");
        assertThat(currentFlywayVersion()).isEqualTo(MigrationVersions.latest());
        assertThat(permissionCodes()).contains("leasing.lead.manage", "leasing.intent.manage",
                "leasing.brand.manage", "leasing.brand.blacklist", "leasing.convert");
        assertThat(platformAdminPermissionCodes()).contains("leasing.lead.manage", "leasing.intent.manage",
                "leasing.brand.manage", "leasing.brand.blacklist", "leasing.convert");
    }

    @Test
    void listLeadsNeedingFollowUpFiltersDueAndExcludesSignedLost() {
        // 3.6 真实库活证据：SQL WHERE 只返回 due（COALESCE(last_follow_up,created)<cutoff）且非 signed/lost。
        Instant cutoff = Instant.parse("2026-06-25T00:00:00Z");
        Instant old = cutoff.minusSeconds(86400);
        Instant fresh = cutoff.plusSeconds(86400);
        repository.saveLead(new LeasingLeadRecord("lead_due", "逾期", null, "referral", null, null, null, null, "u",
                "following_up", null, null, null, old, old));
        repository.saveLead(new LeasingLeadRecord("lead_signed", "已签", null, "referral", null, null, null, null, "u",
                "signed", null, null, null, old, old));
        repository.saveLead(new LeasingLeadRecord("lead_fresh", "刚跟进", null, "referral", null, null, null, null, "u",
                "following_up", null, fresh, null, old, old));
        assertThat(repository.listLeadsNeedingFollowUp(cutoff))
                .extracting(LeasingLeadRecord::id)
                .contains("lead_due").doesNotContain("lead_signed", "lead_fresh");
    }

    @Test
    void repositoryPersistsFunnelRoundTripWithJsonb() {
        Instant now = Instant.parse("2026-06-25T00:00:00Z");
        repository.saveLead(new LeasingLeadRecord("lead_jdbc", "张三", "13800000000", "referral", "bar",
                "sp_jdbc", null, null, "usr_sales", "pending_follow_up", null, null, null, now, now));
        assertThat(repository.findLeadById("lead_jdbc"))
                .isPresent().get()
                .extracting(LeasingLeadRecord::contactName, LeasingLeadRecord::source, LeasingLeadRecord::status)
                .containsExactly("张三", "referral", "pending_follow_up");

        // 意向挂线索（FK）+ rent_terms JSONB 往返。
        repository.saveIntent(new LeasingIntentRecord("intent_jdbc", "lead_jdbc", "proposal_quoted", "sp_jdbc", null,
                Map.of("note", "fixed_rent"), now, "usr_supervisor", null, now, now));
        assertThat(repository.findIntentById("intent_jdbc"))
                .isPresent().get()
                .extracting(LeasingIntentRecord::stage, i -> i.rentTerms().get("note"))
                .containsExactly("proposal_quoted", "fixed_rent");

        // 品牌库 4 类。
        repository.saveBrand(new LeasingBrandRecord("brand_jdbc", "星巴克", "partner", "餐饮",
                null, null, null, null, now, now));
        assertThat(repository.listBrands()).extracting(LeasingBrandRecord::id).contains("brand_jdbc");

        // 跟进流水按 ref 查。
        repository.saveFollowUp(new LeasingFollowUpRecord("follow_jdbc", "lead", "lead_jdbc", "phone",
                "已沟通", null, "usr_sales", null, now));
        assertThat(repository.listFollowUpsByRef("lead", "lead_jdbc"))
                .extracting(LeasingFollowUpRecord::id).contains("follow_jdbc");
    }

    private String regclass(String tableName) {
        return jdbcTemplate.queryForObject("SELECT to_regclass(?)::text", String.class, tableName);
    }

    private String currentFlywayVersion() {
        return jdbcTemplate.queryForObject("""
                SELECT version FROM flyway_schema_history WHERE success = true
                 ORDER BY installed_rank DESC LIMIT 1
                """, String.class);
    }

    private List<String> permissionCodes() {
        return jdbcTemplate.queryForList("SELECT permission_code FROM permissions", String.class);
    }

    private List<String> platformAdminPermissionCodes() {
        return jdbcTemplate.queryForList(
                "SELECT permission_code FROM role_permissions WHERE role_id = 'role_platform_admin'", String.class);
    }
}
