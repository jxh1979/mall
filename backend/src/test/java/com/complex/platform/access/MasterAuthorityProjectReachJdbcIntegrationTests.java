package com.complex.platform.access;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BC 段第 363 条 363-R1（真库、真实入口、真实授权链、蛇形键）：第十八批裁决 71「既然是最高权限，不应该什么都能看到吗？」；
 * BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，不需要经过任何审批。」。最高权限（保留超管账号，组织 platform-company、无商户）
 * 经项目闸 {@link AuthorizationService#projectReach}、{@link AuthorizationService#currentProjectScopeQuietly} 按全部项目：
 * ① 读任一项目的会员受限时长 200；② 存酒补偿付款确认列表含项目 B 的确认；③ 项目 B 的财务退款详情 200；
 * ④ 写项目 B 的会员受限时长 200 并留痕；⑤ 合同列表同时含项目 A、B 的合同；⑥ 项目 A 普通员工读 B 仍 403、平台管理员不变。
 * 修复前：①③④ 403 CROSS_PROJECT_FORBIDDEN，②⑤ 为空。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MasterAuthorityProjectReachJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String projectA = "r363prj_a_" + s;
    private final String projectB = "r363prj_b_" + s;

    @BeforeEach
    void seed() {
        for (String project : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())",
                    project, "最高权限项目闸回归综合体" + project);
        }
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    /** ① ④ 会员受限时长：最高权限读写项目 B 200 并留痕；项目 A 普通员工读 B 仍 403；平台管理员不变。 */
    @Test
    void masterReadsAndWritesAnyProjectsRestrictionSetting() throws Exception {
        String url = "/api/v1/customer-restriction-settings/" + projectB;
        mockMvc.perform(get(url).with(master())).andExpect(status().isOk());

        String key = "r363-rs-" + s;
        mockMvc.perform(put(url).with(master()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duration_days\":90,\"min_days\":1,\"max_days\":180,\"idempotency_key\":\"" + key + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.duration_days").value(90));
        mockMvc.perform(get(url).with(master())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value(projectB)).andExpect(jsonPath("$.data.duration_days").value(90));
        assertThat(jdbc.queryForObject("SELECT duration_days FROM customer_restriction_settings WHERE project_id = ?", Integer.class, projectB))
                .isEqualTo(90);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'customer.restriction_setting.configured'"
                + " AND idempotency_key = ?", Integer.class, key)).isEqualTo(1);

        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor staffA = staffs.staff("r363usr_a_" + s, "staff", projectA, null, "customer.account.manage");
        mockMvc.perform(get(url).with(staffA)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        RequestPostProcessor admin = staffs.staff("r363adm_" + s, "platform_admin", null, null,"customer.account.manage");
        mockMvc.perform(get(url).with(admin)).andExpect(status().isOk());
    }

    /** ② 存酒补偿方案的付款确认：最高权限读项目 B 的确认，列表含该条。 */
    @Test
    void masterSeesAnyProjectsPaymentConfirmations() throws Exception {
        String remedy = "r363rem_" + s;
        String confirmation = "r363pc_" + s;
        jdbc.update("INSERT INTO payment_confirmations (id, confirmation_id, payment_type, object_type, object_id, amount_cent, reason, status,"
                + " review_id, project_id, merchant_id, requested_by) VALUES (?, ?, 'saved_bottle_compensation', 'saved_bottle_remedy', ?, 5000,"
                + " '存酒赔付', 'pending_review', ?, ?, ?, ?)",
                "r363pcl_" + s, confirmation, remedy, "r363rv_" + s, projectB, "r363bar_" + s, "r363req_" + s);

        List<String> ids = JsonPath.read(mockMvc.perform(get("/api/v1/finance/payment-confirmations?object_type=saved_bottle_remedy&object_id="
                        + remedy).with(master())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.confirmations[*].confirmation_id");
        assertThat(ids).containsExactly(confirmation);
    }

    /** ③ 财务退款详情：最高权限读项目 B 的退款 200。 */
    @Test
    void masterReadsAnyProjectsRefund() throws Exception {
        String refund = "r363rf_" + s;
        jdbc.update("""
                INSERT INTO finance_refunds (id, target_id, refund_amount_cent, reason, refund_channel, refund_direction, status, created_at,
                    source_type, merchant_id, project_id, requested_by, original_payment_ref, updated_at)
                VALUES (?, ?, 5000, '原路待退', 'wechat', 'original', 'requested', now(), 'stored_value_account', NULL, ?, 'system', ?, now())
                """, refund, "r363tg_" + s, projectB, "r363orig_" + s);

        mockMvc.perform(get("/api/v1/finance/refunds/" + refund).with(master())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refund.id").value(refund));
    }

    /** ⑤ 合同列表：最高权限同时看到项目 A、B 的合同。 */
    @Test
    void masterListsEveryProjectsContracts() throws Exception {
        String contractA = contract("a", projectA);
        String contractB = contract("b", projectB);

        List<String> ids = JsonPath.read(mockMvc.perform(get("/api/v1/contracts?size=500").with(master())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].id");
        assertThat(ids).contains(contractA, contractB);
    }

    private String contract(String tag, String project) {
        String id = "r363ctr_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, project_id, rent_terms, created_at, updated_at)
                VALUES (?, ?, ?, ?, DATE '2098-01-01', DATE '2098-03-31', 'drafting', ?, '{"baseRentCent":100000}'::jsonb,
                    clock_timestamp(), clock_timestamp())""",
                id, "r363mch_" + tag + "_" + s, "r363subj_" + s, "r363subj_" + s, project);
        return id;
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
