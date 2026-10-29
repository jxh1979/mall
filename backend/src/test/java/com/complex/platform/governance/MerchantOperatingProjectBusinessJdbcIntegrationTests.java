package com.complex.platform.governance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 G 段第 82 条 82b-R8 真库＋真实接口＋真实授权链回归，请求体一律按前端蛇形键。
 * 依据：治理地图第 659 行「一个商户要在别的项目经营的，由能管理商户档案的人在商户档案里加经营项目，要经过审批才生效。」；
 * XM-M001-D 答复 3 第 10 条所选「同一个商户，跨项目经营」；裁决 fresh-M474「只有商户的主项目或已批准的经营项目里……」。
 *
 * <p>夹具随测试事务回滚：项目 A、B；商户 m（主项目 A，传媒）已批准加经营项目 B（merchant_projects 只在审批通过后写入），
 * 商户 n（主项目 A，传媒）只有一张待审批的加 B 申请；B 项目普通员工（角色只到本人项目）。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantOperatingProjectBusinessJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_r8_a_" + suffix;
    private final String projectB = "prj_r8_b_" + suffix;
    private final String m = "mch_r8_m_" + suffix;
    private final String n = "mch_r8_n_" + suffix;
    private RequestPostProcessor staffB;

    @BeforeEach
    void seed() {
        for (String projectId : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", projectId, "R8 " + projectId);
        }
        for (String merchantId : List.of(m, n)) {
            jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, status, created_at, updated_at, project_id) "
                    + "VALUES (?, ?, 'media', 'active', now(), now(), ?)", merchantId, "R8 " + merchantId, projectA);
        }
        jdbc.update("INSERT INTO merchant_projects (merchant_id, project_id) VALUES (?, ?)", m, projectB);
        jdbc.update("INSERT INTO merchant_project_requests (id, merchant_id, project_id, reason, requested_by, review_id) "
                + "VALUES (?, ?, ?, '在 B 开店经营', 'usr_r8_x', ?)", "mpr_r8_" + suffix, n, projectB, "rv_r8_" + suffix);
        staffB = account("sb", projectB, null);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** ①④ 演出场次：B 员工给已批准在 B 经营的 m 建 B 的场次 200；只有待审批申请的 n 照旧 400；m 的商户账号（主项目 A）对 B 建场次 403。 */
    @Test
    void mediaShowAcceptsMerchantOperatingInTheShowProject() throws Exception {
        send(staffB, "/api/v1/settlement/media-shows", show(m, "ms1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value(projectB))
                .andExpect(jsonPath("$.data.media_merchant_id").value(m));
        send(staffB, "/api/v1/settlement/media-shows", show(n, "ms2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("MEDIA_SHOW_MERCHANT_PROJECT_MISMATCH"));
        send(account("ma", projectA, m), "/api/v1/settlement/media-shows", show(m, "ms3"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_performance_shows WHERE project_id=?", Integer.class, projectB))
                .isEqualTo(1);
    }

    /** ② 供货合同（商户自采）：B 员工给已批准在 B 经营的 m 建 B 的合同 200；n 照旧 409「商户不属于该综合体」。 */
    @Test
    void merchantSelfPurchaseContractAcceptsMerchantOperatingInTheContractProject() throws Exception {
        String supplierM = supplier(m), supplierN = supplier(n);
        send(staffB, "/api/v1/inventory/supply-contracts", contract(supplierM, m, item(m), "sc1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.project_id").value(projectB))
                .andExpect(jsonPath("$.data.merchant_id").value(m));
        send(staffB, "/api/v1/inventory/supply-contracts", contract(supplierN, n, item(n), "sc2"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PURCHASE_WORKFLOW_INVALID"))
                .andExpect(jsonPath("$.message").value("商户不属于该综合体"));
    }

    /** ③ 采购工作台目录：B 员工的商户选项含 m（带主项目与经营项目），不含 n。 */
    @Test
    void purchaseWorkbenchOptionsListMerchantsOperatingInVisibleProjects() throws Exception {
        String body = mockMvc.perform(get("/api/v1/inventory/purchase-workbench-options").with(staffB))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<List<String>>read(body, "$.data.merchants[*].id")).contains(m).doesNotContain(n);
        assertThat(JsonPath.<List<String>>read(body, "$.data.merchants[?(@.id=='" + m + "')].operating_project_ids[*]"))
                .containsExactlyInAnyOrder(projectA, projectB);
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────────────────

    private String show(String media, String key) {
        return "{\"project_id\":\"" + projectB + "\",\"show_kind\":\"self_hosted\",\"media_merchant_id\":\"" + media
                + "\",\"show_date\":\"2026-11-20\",\"idempotency_key\":\"" + key + "-" + suffix + "\"}";
    }

    private String contract(String supplier, String merchant, String item, String key) {
        LocalDate today = LocalDate.now();
        return "{\"idempotency_key\":\"" + key + "-" + suffix + "\",\"supplier_id\":\"" + supplier + "\",\"project_id\":\"" + projectB
                + "\",\"merchant_id\":\"" + merchant + "\",\"contract_mode\":\"merchant_self_purchase\",\"supply_scope\":\"饮品\","
                + "\"price_terms\":\"见执行条款\",\"valid_from\":\"" + today.minusDays(1) + "\",\"valid_to\":\"" + today.plusYears(1)
                + "\",\"summary\":\"商户自采合同\",\"purchasable_scope\":[\"" + item + "\"]}";
    }

    private String supplier(String merchant) {
        String id = "sup_r8_" + merchant;
        jdbc.update("INSERT INTO inventory_suppliers(id,supplier_name,supplier_type,status,merchant_id,created_at,updated_at) "
                + "VALUES(?,?,'general','active',?,now(),now())", id, id, merchant);
        return id;
    }

    private String item(String merchant) {
        String id = "itm_r8_" + merchant;
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) "
                + "VALUES(?,?,'consumable','瓶',0,'active',?,now(),now())", id, id, merchant);
        return id;
    }

    private ResultActions send(RequestPostProcessor who, String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 员工主体：角色只到本人项目，持演出合同、供货合同与采购查看权限码。 */
    private RequestPostProcessor account(String tag, String project, String merchantId) {
        String userId = "usr_r8_" + tag + "_" + suffix, role = "role_r8_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'经营项目业务回归','project','active',now())",
                role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + "'media.performance.view','media.performance.contract.manage','inventory.supply_contract.manage','inventory.purchase_request.view')", role);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
