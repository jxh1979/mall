package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BL 段第 380 条 380-R1（BSM:717「酒吧要知道桌台、卡座、吧台属于哪个空间和商户。」）：建台位必带所属空间（所属商户为该酒吧且启用中），
 * 与台位同一事务写入；该酒吧可选空间清单。第 389 条 389-R1、389-R2（BSM:1017「桌台、商品、分类、套餐等都归属到具体的酒吧商户」）：
 * 平台管理员建的台位、商品、分类、套餐归所选酒吧，酒吧员工在其上开台点单走得通。空间由平台管理员经新建空间接口建，停用经修改空间接口。
 * 真实 HTTP、蛇形键、真实授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarTableCreateWithSpaceJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "prj_b84ts_" + s;
    private final String barA = "bar_b84ts_a_" + s;
    private final String barB = "bar_b84ts_b_" + s;
    private final String spaceType = "b84ts_" + s;
    private int seq;
    private RequestPostProcessor admin;
    private RequestPostProcessor manager;
    private String seat;
    private String disabledSeat;
    private String otherBarSeat;

    @BeforeEach
    void seed() throws Exception {
        String company = "ent_b84ts_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','台位空间综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'经营主体','management_company','active',now(),now())", company);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", company, project);
        for (String bar : List.of(barA, barB)) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,contract_entity_id,settlement_entity_id,project_id,status,created_at,updated_at)"
                    + " VALUES(?,?,'bar',?,?,?,?,'active',now(),now())", bar, bar.equals(barA) ? "甲酒吧" : "乙酒吧", company, company, company, project);
        }
        jdbc.update("INSERT INTO space_type_dictionary(id,type_code,type_name,sort_order,status,created_at,updated_at) VALUES(?,?,?,100,'active',now(),now())",
                "sptype_" + spaceType, spaceType, "回归卡座区" + s);
        jdbc.update("""
                INSERT INTO bar_payroll_rules(id, rule_kind, project_id, merchant_id, effective_from, version, rule_json, reason, status,
                    platform_company_id, created_by)
                VALUES (?, 'charge', ?, NULL, ?, 'V0.01', ?::jsonb, '服务费 0', 'approved', ?, 'usr_b84ts')
                """, "bpr_b84ts_" + s, project, java.sql.Date.valueOf(LocalDate.now().minusDays(1)),
                json.writeValueAsString(new BarPayrollRequests.RuleValues(0, 0, 2000, null, null, null, List.of())), company);
        StaffRequestFixture accounts = new StaffRequestFixture(jdbc);
        admin = accounts.staff("usr_b84ts_admin_" + s, "platform_admin", null, null, "governance.space.manage", "bar.table.manage",
                "bar.product.manage", "bar.category.manage", "bar.combo.manage");
        manager = accounts.staff("usr_b84ts_mgr_" + s, "staff", project, barA, "bar.table.manage", "bar.table.view",
                "bar.table_session.create", "bar.order.create", "bar.payroll.view", "bar.product.view");
        seat = space(barA, "卡座-01", "active");
        disabledSeat = space(barA, "卡座-停用", "unavailable");
        otherBarSeat = space(barB, "乙卡座", "active");
    }

    // 380-R1 ①：挂酒吧甲的经理带甲名下启用空间建卡座：200，台位挂上该空间；随后开台、点单 200，服务来源记空间所在综合体，服务责任可查。
    // 修复前建台位不写空间（库里 space_id 为空，断言失败）。
    @Test
    void managerCreatesCardSeatOnOwnSpaceThenOpensAndOrders() throws Exception {
        String table = read(createTable(manager, seat).andExpect(status().isOk()), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT space_id FROM bar_tables WHERE id = ?", String.class, table)).isEqualTo(seat);

        String snack = "product_b84ts_" + s;
        Instant now = Instant.now();
        bars.saveProduct(new BarProductRecord(snack, barA, barA, "果盘", "snack", 300L, "CNY", null, null, false, "active",
                now, now, null, null, null, false, null, null, null));
        String session = read(openSession(manager, table).andExpect(status().isOk()), "$.data.id");
        String order = read(call(manager, post("/api/v1/bar/orders"), order(table, List.of(Map.of("product_id", snack, "quantity", 1))))
                .andExpect(status().isOk()), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT project_id FROM bar_order_service_sources WHERE order_id = ?", String.class, order))
                .isEqualTo(project);
        mockMvc.perform(get("/api/v1/bar/table-sessions/" + session + "/service-responsibilities").with(manager))
                .andExpect(status().isOk());
    }

    // 380-R1 ②：不带所属空间：400 VALIDATION_FAILED，台位不落。修复前 200。
    @Test
    void createTableWithoutSpaceIsRejected() throws Exception {
        createTable(manager, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(tablesOfBarA()).isZero();
    }

    // 380-R1 ③：别家酒吧的空间、本店停用的空间：各 409 BAR_TABLE_SPACE_NOT_OWNED，台位不落。修复前 200。
    @Test
    void createTableOnOtherBarOrDisabledSpaceIsRejected() throws Exception {
        createTable(manager, otherBarSeat)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_SPACE_NOT_OWNED"));
        createTable(manager, disabledSeat)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_SPACE_NOT_OWNED"));
        assertThat(tablesOfBarA()).isZero();
    }

    // 380-R1 ④⑤：可选空间清单只列甲名下启用的「卡座-01」（键 id、space_name），平台管理员带同一 bar_id 结果相同；
    // 该空间经修改空间接口改为不可用后清单为空，再带它建台位 409。修复前清单接口不存在（状态断言失败）。
    @Test
    void spaceOptionsListOwnActiveSpacesAndDropDisabledOnes() throws Exception {
        for (RequestPostProcessor who : List.of(manager, admin)) {
            options(who).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items.length()").value(1))
                    .andExpect(jsonPath("$.data.items[0].id").value(seat))
                    .andExpect(jsonPath("$.data.items[0].space_name").value("卡座-01"));
        }
        call(admin, put("/api/v1/platform/spaces/" + seat), "{\"idempotency_key\":\"b84ts-off-" + s + "\",\"space_name\":\"卡座-01\","
                + "\"space_type\":\"" + spaceType + "\",\"merchant_id\":\"" + barA + "\",\"status\":\"unavailable\"}")
                .andExpect(status().isOk());
        options(manager).andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(0));
        createTable(manager, seat)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("BAR_TABLE_SPACE_NOT_OWNED"));
    }

    // 389-R1、389-R2：平台管理员在后台为酒吧甲建台位、商品、分类、套餐，各自所属商户为甲；甲的员工在台位清单与商品清单里看得到，
    // 在该台位开台后点这件商品与这份套餐 200。修复前平台管理员建的台位所属商户为空（断言失败）。
    @Test
    void platformAdminConfiguresBarThenBarStaffOpensAndOrders() throws Exception {
        String table = read(createTable(admin, seat).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchant_id").value(barA)), "$.data.id");
        assertThat(ids(mockMvc.perform(get("/api/v1/bar/tables").with(manager)).andExpect(status().isOk()))).contains(table);

        Map<String, Object> product = new LinkedHashMap<>();
        product.put("idempotency_key", "b84ts-p-" + s);
        product.put("bar_id", barA);
        product.put("product_name", "果盘");
        product.put("category", "snack");
        product.put("price_cent", 300);
        String snack = read(call(admin, post("/api/v1/bar/products"), product).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchant_id").value(barA)), "$.data.id");
        String category = read(call(admin, post("/api/v1/bar/catalog/categories"), Map.of("bar_id", barA, "category_name", "小食"))
                .andExpect(status().isOk()), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT merchant_id FROM bar_product_categories WHERE id = ?", String.class, category)).isEqualTo(barA);
        Map<String, Object> combo = new LinkedHashMap<>();
        combo.put("bar_id", barA);
        combo.put("combo_name", "果盘套餐");
        combo.put("price_cent", 500);
        combo.put("items", List.of(Map.of("product_id", snack, "quantity", 1)));
        String comboId = read(call(admin, post("/api/v1/bar/catalog/combos"), combo).andExpect(status().isOk()), "$.data.id");
        assertThat(jdbc.queryForObject("SELECT merchant_id FROM bar_combos WHERE id = ?", String.class, comboId)).isEqualTo(barA);
        assertThat(ids(mockMvc.perform(get("/api/v1/bar/products").with(manager)).andExpect(status().isOk()))).contains(snack);

        read(openSession(manager, table).andExpect(status().isOk()), "$.data.id");
        call(manager, post("/api/v1/bar/orders"), order(table, List.of(Map.of("product_id", snack, "quantity", 1),
                Map.of("combo_id", comboId, "quantity", 1))))
                .andExpect(status().isOk());
    }

    /** 平台管理员经新建空间接口建一行，返回编号。 */
    private String space(String merchant, String name, String spaceStatus) throws Exception {
        String body = "{\"idempotency_key\":\"b84ts-sp-" + (++seq) + "-" + s + "\",\"project_id\":\"" + project + "\",\"space_name\":\"" + name
                + "\",\"space_type\":\"" + spaceType + "\",\"merchant_id\":\"" + merchant + "\",\"status\":\"" + spaceStatus + "\"}";
        return read(call(admin, post("/api/v1/platform/spaces"), body).andExpect(status().isOk()), "$.data.id");
    }

    private ResultActions createTable(RequestPostProcessor who, String spaceId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "b84ts-t-" + (++seq) + "-" + s);
        body.put("bar_id", barA);
        body.put("table_name", "卡座一号");
        body.put("table_type", "card_seat");
        body.put("capacity", 4);
        body.put("minimum_spend_cent", 0);
        if (spaceId != null) {
            body.put("space_id", spaceId);
        }
        return call(who, post("/api/v1/bar/tables"), body);
    }

    private ResultActions openSession(RequestPostProcessor who, String table) throws Exception {
        return call(who, post("/api/v1/bar/table-sessions"), Map.of("idempotency_key", "b84ts-o-" + (++seq) + "-" + s, "bar_id", barA,
                "table_id", table, "guest_count", 2));
    }

    private Map<String, Object> order(String table, List<Map<String, Object>> items) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "b84ts-order-" + (++seq) + "-" + s);
        body.put("bar_id", barA);
        body.put("table_id", table);
        body.put("items", items);
        return body;
    }

    private ResultActions options(RequestPostProcessor who) throws Exception {
        return mockMvc.perform(get("/api/v1/bar/tables/space-options").param("bar_id", barA).with(who));
    }

    private int tablesOfBarA() {
        return jdbc.queryForObject("SELECT count(*) FROM bar_tables WHERE bar_id = ?", Integer.class, barA);
    }

    private ResultActions call(RequestPostProcessor who, MockHttpServletRequestBuilder request, Object body) throws Exception {
        String content = body instanceof String text ? text : json.writeValueAsString(body);
        return mockMvc.perform(request.with(who).contentType(MediaType.APPLICATION_JSON).content(content));
    }

    private static String read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), path);
    }

    private static List<String> ids(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.items[*].id");
    }
}
