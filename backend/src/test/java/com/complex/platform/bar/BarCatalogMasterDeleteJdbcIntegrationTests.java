package com.complex.platform.bar;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 AO 段第 249 条 249-R1、249-R2、249-R9（真库、真实入口、真实授权链）。不开测试事务：最高权限发起删除遇占用时要看到整笔回滚。
 * ① 最高权限本人对别家酒吧已存档的商品申请删除即生效，另落 bar.product.delete_approved（BSM:576「持授权 U 盘密钥的最高权限本人发起的操作，
 *   不需要经过任何审批。」；PGM:528「持授权 U 盘密钥的最高权限可以逐笔开放」）；商品仍被订单明细引用时 409 BAR_PRODUCT_IN_USE，
 *   整笔回滚，商品仍为已存档、未申请删除（BSM:572「只要还有占用就拦住不让删」）。
 * ② 套餐被订单明细引用（套餐下单展开出的明细记 combo_id，V580）时，最高权限审批删除 409 BAR_COMBO_IN_USE、仍待审批；没被引用的照删。
 * ③ 最高权限打开商品、分类、套餐列表看到各家酒吧的目录，酒吧员工照旧只看本店（第十八批裁决 71「看全部酒吧目录」）。
 * 夹具不回滚：角色名用中文、编号后缀纯数字。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
class BarCatalogMasterDeleteJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = Long.toString(ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L));
    private final String project = "r249bar_prj_" + s;
    private final String barA = "r249bar_a_" + s;
    private final String barB = "r249bar_b_" + s;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','酒吧目录删除回归综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,?,'bar','active',?,now(),now())",
                barA, "酒吧目录删除回归甲店" + s, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at) VALUES(?,?,'bar','active',?,now(),now())",
                barB, "酒吧目录删除回归乙店" + s, project);
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    /** 249-R1⑤：最高权限本人对别家酒吧已存档的商品申请删除，当场删除，申请与批准各留一条痕。 */
    @Test
    void masterDeleteRequestOnAnotherBarsArchivedProductTakesEffectAtOnce() throws Exception {
        String id = product(barA, "direct", true);

        send(master(), "/api/v1/bar/catalog/products/" + id + "/delete-request", "{\"reason\":\"配错的商品\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.delete_status").value("approved"));

        assertThat(jdbc.queryForObject("SELECT delete_status FROM bar_products WHERE id = ?", String.class, id)).isEqualTo("approved");
        assertThat(masterEvents(id)).containsExactlyInAnyOrder("bar.product.delete_requested|success", "bar.product.delete_approved|success");

        // 第十八批 AU 段第 320 条 320-R1（BSM:570「删除（走审批·软删除）」；BSM:571「前台各处不再显示」）：已删除的商品勾「显示已存档」也不再列出，
        // 最高权限与甲店员工都一样；只存档、未删除的照旧带出、仍是已存档。修复前红：商品列表不排除已删除行，被标成「已存档」列出。
        String kept = product(barA, "kept", true);
        RequestPostProcessor staffA = staff("archiveda", barA, "酒吧商品查看回归角色", "bar.product.view");
        assertThat(ids(master(), "/api/v1/bar/products?includeArchived=true", "$.data.items[*].id")).contains(kept).doesNotContain(id);
        assertThat(ids(staffA, "/api/v1/bar/products?includeArchived=true", "$.data.items[*].id")).contains(kept).doesNotContain(id);
        assertThat(ids(staffA, "/api/v1/bar/products?includeArchived=true", "$.data.items[?(@.id == '" + kept + "')].archived_at"))
                .hasSize(1).doesNotContainNull();
    }

    /** 249-R1⑤：商品仍被订单明细引用，最高权限发起删除 409，整笔回滚——仍为已存档、未申请删除，成功事件一条也不留。 */
    @Test
    void masterDeleteRequestOnProductStillOrderedIsRejectedAndRolledBack() throws Exception {
        String id = product(barA, "ordered", true);
        orderItem(barA, "ordered", id, null);
        long requestFailed = masterFailures("bar.product.delete_request_failed", "BAR_PRODUCT_IN_USE");
        long approveFailed = masterFailures("bar.product.delete_approve_failed", "BAR_PRODUCT_IN_USE");

        send(master(), "/api/v1/bar/catalog/products/" + id + "/delete-request", "{\"reason\":\"配错的商品\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_PRODUCT_IN_USE"));

        Map<String, Object> row = jdbc.queryForMap("SELECT archived_at, delete_status FROM bar_products WHERE id = ?", id);
        assertThat(row.get("archived_at")).isNotNull();
        assertThat(row.get("delete_status")).isEqualTo("none");
        assertThat(masterEvents(id)).isEmpty();
        assertThat(masterFailures("bar.product.delete_request_failed", "BAR_PRODUCT_IN_USE")).isEqualTo(requestFailed + 1);
        assertThat(masterFailures("bar.product.delete_approve_failed", "BAR_PRODUCT_IN_USE")).isEqualTo(approveFailed + 1);
    }

    /** 249-R2：套餐被订单明细引用（combo_id），最高权限审批删除 409 BAR_COMBO_IN_USE、仍待审批；没被引用的套餐照删。 */
    @Test
    void comboStillOrderedCannotBeDeletedWhileUnorderedComboCan() throws Exception {
        String item = product(barA, "comboitem", false);
        String ordered = combo(barA, "ordered", item, true);
        String unused = combo(barA, "unused", item, true);
        orderItem(barA, "combo", item, ordered);

        send(master(), "/api/v1/bar/catalog/combos/" + ordered + "/delete-approve", "{\"decision\":\"approved\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_COMBO_IN_USE"));
        assertThat(jdbc.queryForObject("SELECT delete_status FROM bar_combos WHERE id = ?", String.class, ordered)).isEqualTo("pending");

        send(master(), "/api/v1/bar/catalog/combos/" + unused + "/delete-approve", "{\"decision\":\"approved\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));
        assertThat(jdbc.queryForObject("SELECT delete_status FROM bar_combos WHERE id = ?", String.class, unused)).isEqualTo("approved");
    }

    /** 249-R9（第十八批裁决 71）：最高权限看得到两家酒吧的商品、分类、套餐；甲店员工只看到甲店的。 */
    @Test
    void masterSeesEveryBarsCatalogWhileBarStaffSeeOnlyTheirOwn() throws Exception {
        String productA = product(barA, "lista", false);
        String productB = product(barB, "listb", false);
        String categoryA = category(barA, "lista");
        String categoryB = category(barB, "listb");
        String comboA = combo(barA, "lista", productA, false);
        String comboB = combo(barB, "listb", productB, false);
        RequestPostProcessor staffA = staff("viewa", barA, "酒吧目录查看回归角色", "bar.product.view", "bar.category.view", "bar.combo.view");

        assertThat(ids(master(), "/api/v1/bar/products?includeArchived=false", "$.data.items[*].id")).contains(productA, productB);
        assertThat(ids(master(), "/api/v1/bar/catalog/categories", "$.data[*].id")).contains(categoryA, categoryB);
        assertThat(ids(master(), "/api/v1/bar/catalog/combos", "$.data[*].id")).contains(comboA, comboB);

        assertThat(ids(staffA, "/api/v1/bar/products?includeArchived=false", "$.data.items[*].id")).contains(productA).doesNotContain(productB);
        assertThat(ids(staffA, "/api/v1/bar/catalog/categories", "$.data[*].id")).contains(categoryA).doesNotContain(categoryB);
        assertThat(ids(staffA, "/api/v1/bar/catalog/combos", "$.data[*].id")).contains(comboA).doesNotContain(comboB);
    }

    // ---------- 夹具 ----------

    private String product(String bar, String tag, boolean archived) {
        String id = "r249bp_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_products (id, bar_id, merchant_id, product_name, product_type, price_cent, status, created_at, updated_at,"
                + " archived_at, archived_by, delete_status) VALUES (?, ?, ?, ?, 'spirit', 5000, 'active', now(), now(),"
                + " CASE WHEN ? THEN now() END, CASE WHEN ? THEN ? END, 'none')",
                id, bar, bar, "回归商品" + tag, archived, archived, "r249bar_mgr_" + s);
        return id;
    }

    private String category(String bar, String tag) {
        String id = "r249bc_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_product_categories (id, bar_id, merchant_id, category_name, sort_order, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, 1, 'active', now(), now())", id, bar, bar, "回归分类" + tag);
        return id;
    }

    /** pending=true：已存档、另一位店员已申请删除、待审批。 */
    private String combo(String bar, String tag, String productId, boolean pending) {
        String id = "r249bk_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_combos (id, bar_id, merchant_id, combo_name, price_cent, status, created_at, updated_at,"
                + " archived_at, archived_by, delete_status, delete_requested_by, delete_requested_at, delete_reason)"
                + " VALUES (?, ?, ?, ?, 9900, 'active', now(), now(), CASE WHEN ? THEN now() END, CASE WHEN ? THEN ? END,"
                + " CASE WHEN ? THEN 'pending' ELSE 'none' END, CASE WHEN ? THEN ? END, CASE WHEN ? THEN now() END, CASE WHEN ? THEN '套餐下线' END)",
                id, bar, bar, "回归套餐" + tag, pending, pending, "r249bar_mgr_" + s, pending, pending, "r249bar_mgr_" + s, pending, pending);
        jdbc.update("INSERT INTO bar_combo_items (id, combo_id, product_id, quantity, created_at) VALUES (?, ?, ?, 1, now())",
                "r249bki_" + tag + "_" + s, id, productId);
        return id;
    }

    /** 一张已下的酒吧订单里的一条明细；comboId 非空＝套餐下单展开出的明细。 */
    private void orderItem(String bar, String tag, String productId, String comboId) {
        String table = "r249tbl_" + tag + "_" + s;
        String order = "r249bo_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '回归卡座', 'card_seat', 'available', now(), now())", table, bar, bar, "K" + tag + s);
        jdbc.update("INSERT INTO bar_orders (id, bar_id, merchant_id, table_id, order_source, status, total_amount_cent, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, 'staff', 'submitted', 5000, now(), now())", order, bar, bar, table);
        jdbc.update("INSERT INTO bar_order_items (id, order_id, product_id, product_name, quantity, unit_price_cent, amount_cent, delivery_status,"
                + " created_at, updated_at, combo_id) VALUES (?, ?, ?, '回归商品', 1, 5000, 5000, 'pending', now(), now(), ?)",
                "r249boi_" + tag + "_" + s, order, productId, comboId);
    }

    // ---------- 断言与请求 ----------

    private ResultActions send(RequestPostProcessor who, String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private List<String> ids(RequestPostProcessor who, String url, String path) throws Exception {
        String body = mockMvc.perform(get(url).with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, path);
    }

    /** 最高权限本人在这个对象上留下的删除类成功／失败事件（失败事件的 business_id 为 all，不在此列）。 */
    private List<String> masterEvents(String businessId) {
        return jdbc.queryForList("SELECT event_name || '|' || event_result FROM audit_events"
                + " WHERE actor_id = ? AND business_id = ? AND event_name LIKE 'bar.%.delete_%'",
                String.class, SessionIssuer.MASTER_USER_ID, businessId);
    }

    private long masterFailures(String event, String errorCode) {
        Long count = jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = ? AND actor_id = ?"
                + " AND metadata->>'error_code' = ?", Long.class, event, SessionIssuer.MASTER_USER_ID, errorCode);
        return count == null ? 0L : count;
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private RequestPostProcessor master() {
        return session(new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active"));
    }

    /** 某家酒吧的员工，经一个中文名角色持有给定权限码。 */
    private RequestPostProcessor staff(String tag, String bar, String roleName, String... codes) {
        String userId = "r249bar_" + tag + "_" + s;
        jdbc.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at) VALUES (?, ?, 'staff', ?, ?, 'active', now())",
                userId, userId, project, bar);
        String role = "role_" + userId;
        jdbc.update("INSERT INTO roles (id, role_name, description, data_scope, status, created_at) VALUES (?, ?, '酒吧目录删除回归', 'project', 'active', now())",
                role, roleName + s);
        for (String code : codes) {
            jdbc.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, ?)", role, code);
        }
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, role);
        return session(new AccessActor(userId, userId, "staff", project, bar, "active"));
    }

    private RequestPostProcessor session(AccessActor actor) {
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
