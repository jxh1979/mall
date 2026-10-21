package com.complex.platform.customer;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.complex.platform.access.AccessActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十一批 TK-04-R1：商户业态积分获取率、商户消费成长值规则、商户消费等待期的版本化配置。
 * 依据：BSM L822「可配置“每消费多少元产生一积分”（业务参数，按商户业态可配，须同时配置上下限）」「积分规则与成长值规则分别配置」；
 * BSM L823「该等待期为业务参数……全平台统一配置，出厂默认 24 小时，须同时配置上下限」；DEC-R8-93「由该业态对应域主管公司审批」；
 * 2026-10-21 操作员裁决补充第 4 条「按商户业态分别配置……配置时值与上下限同配」；会员与等级 PRD §4（全国平台管理成长值基础规则与审批）。
 */
@SpringBootTest(properties = {"spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/complex_test", "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"})
@Transactional
class MerchantRewardConfigJdbcIntegrationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired TicketRewardConfigService configs;
    @Autowired ObjectMapper json;
    private final String key = UUID.randomUUID().toString().replace("-", "");

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void merchantPointsRuleResolvedByBusinessFormatAndVersion() throws Exception {
        Instant t0 = Instant.parse("2037-05-01T00:00:00Z").plus(Math.floorMod(key.hashCode(), 400_000), ChronoUnit.MINUTES);
        String v1 = insert("merchant_points", "catering", 1_000L, t0, "V0.01");
        String v2 = insert("merchant_points", "catering", 500L, t0.plus(1, ChronoUnit.DAYS), "V0.02");
        assertThat(configs.effectiveOptional("merchant_points", null, null, "catering", t0.plus(1, ChronoUnit.HOURS))).get()
                .satisfies(c -> {
                    assertThat(c.id()).isEqualTo(v1);
                    assertThat(c.businessFormat()).isEqualTo("catering");
                    assertThat(c.values().fixedValue()).isEqualTo(1_000L);
                });
        assertThat(configs.effectiveOptional("merchant_points", null, null, "catering", t0.plus(25, ChronoUnit.HOURS))).get()
                .extracting(TicketRewardConfig::id).isEqualTo(v2);
        assertThat(configs.effectiveOptional("merchant_points", null, null, "media", t0.plus(25, ChronoUnit.HOURS))).isEmpty();
        // 成长值规则独立，不由积分规则回落。
        assertThat(configs.effectiveOptional("merchant_growth", null, null, "catering", t0.plus(25, ChronoUnit.HOURS))).isEmpty();
    }

    @Test
    void merchantRuleSubmissionRequiresFormatBoundsAndFormatDomainApprover() {
        String company = entity("management_company");
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,status,created_at,updated_at) VALUES(?,'规则测试传媒','media',?,'active',now(),now())",
                "m_" + key, company);
        String requester = actor(company, List.of("ticket.reward.config.manage", "ticket.reward.config.view"));
        String approver = actor(company, List.of("ticket.reward.config.manage", "ticket.reward.config.view", "ticket.reward.rule.approve", "governance.supervisor_review.approve"));
        String outsider = actor(entity("management_company"), List.of("ticket.reward.rule.approve"));
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", requester, approver);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id,assigned_by,assigned_at) VALUES(?,'lvl_m1_lower',?,'fixture-hr-approved',now())", approver, company);
        authenticate(requester, company);
        Instant future = Instant.now().plusSeconds(7200).truncatedTo(ChronoUnit.MICROS);

        assertThatThrownBy(() -> configs.submit(request("merchant_points", null, rate(1_000L, 1L), future, approver)))
                .hasMessageContaining("business_format");
        assertThatThrownBy(() -> configs.submit(request("merchant_points", "media", new TicketRewardConfig.Values(1_000L, null, null,
                null, null, null, null, null, null, null, null, null, null, List.of()), future, approver)))
                .hasMessageContaining("上下限");
        assertThatThrownBy(() -> configs.submit(request("merchant_points", "media", rate(1_000L, 0L), future, approver)))
                .hasMessageContaining("大于 0");
        assertThatThrownBy(() -> configs.submit(request("merchant_points", "media", rate(1_000L, 1L), future, outsider)))
                .hasMessageContaining("域主管公司");

        TicketRewardConfig pending = configs.submit(request("merchant_points", "media", rate(1_000L, 1L), future, approver));
        assertThat(pending.status()).isEqualTo("pending_review");
        assertThat(pending.businessFormat()).isEqualTo("media");
        assertThat(pending.version()).isEqualTo("V0.01");
        // 成长值规则由全国平台审批：业态域主管公司的审批人不可作最终审批人。
        assertThatThrownBy(() -> configs.submit(request("merchant_growth", "media", rate(100L, 1L), future, approver)))
                .hasMessageContaining("顶层平台");
    }

    @Test
    void merchantWaitingHoursOutOfBoundsRejected() {
        String company = entity("platform");
        String requester = actor(company, List.of("ticket.reward.config.manage", "ticket.reward.config.view"));
        authenticate(requester, company);
        Instant future = Instant.now().plusSeconds(7200);
        assertThatThrownBy(() -> configs.submit(request("national", null, national(company, 800, 1, 720), future, requester)))
                .hasMessageContaining("上下限");
        assertThatThrownBy(() -> configs.submit(request("national", null, national(company, 24, null, null), future, requester)))
                .hasMessageContaining("上下限");
    }

    private TicketRewardConfig.Request request(String kind, String format, TicketRewardConfig.Values values, Instant at, String approver) {
        return new TicketRewardConfig.Request("mrc-" + UUID.randomUUID(), kind, null, null, values, at, "业态规则调整", approver, List.of(), format);
    }

    private static TicketRewardConfig.Values rate(long cents, Long min) {
        return new TicketRewardConfig.Values(cents, min, 100_000L, null, null, null, null, null, null, null, null, null, null, List.of());
    }

    private static TicketRewardConfig.Values national(String receiver, Integer hours, Integer min, Integer max) {
        return new TicketRewardConfig.Values(null, null, null, 24, 1, 720, new BigDecimal("0.01"), new BigDecimal("0.001"), new BigDecimal("100"),
                receiver, null, null, null, List.of(), hours, min, max);
    }

    private String insert(String kind, String format, long cents, Instant effective, String version) throws Exception {
        String id = "trc_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.update("INSERT INTO ticket_reward_config_versions(id,kind,business_format,version,payload,effective_at,published_at,status,created_by,created_at,approved_at) VALUES(?,?,?,?,?::jsonb,?,?,'approved','test-reviewer',?,?)",
                id, kind, format, version, json.writeValueAsString(rate(cents, 1L)), Timestamp.from(effective), Timestamp.from(effective.minusSeconds(60)),
                Timestamp.from(effective.minusSeconds(120)), Timestamp.from(effective.minusSeconds(60)));
        return id;
    }

    private String entity(String role) {
        String id = "be_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'规则测试主体',?,'active',now(),now())", id, role);
        return id;
    }

    private String actor(String company, List<String> permissions) {
        String user = "u_" + UUID.randomUUID().toString().replace("-", "");
        String role = "r_" + user;
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,status,created_at) VALUES(?,?,'platform_admin',?,'active',now())", user, user, company);
        jdbc.update("INSERT INTO roles(id,role_name,created_at) VALUES(?,?,now())", role, role);
        for (String permission : permissions) jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, permission);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        return user;
    }

    private void authenticate(String user, String company) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(user, user, "platform_admin", company, null, "active"), "token", List.of()));
    }
}
