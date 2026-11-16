package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.BusinessDayResolver;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AK 段第 221 条 221-R1（真库）：BSM:1069「反结（重开已结账单）、沽清（卖完、临时下架）均需主管审批留痕」；ENUM:111「沽清分两档：
 * 临时沽清（本营业日有效，营业日切换时点自动恢复在售）与长期下架（须主管审批解除后才恢复）；沽清与解除同样须审批留痕，作用范围为本酒吧。」。
 * 临时沽清、长期下架、解除沽清各是一种主管复核动作：首次提交 409 ADMISSION_BLOCKED 建复核单、不生效；主管批准后原样再提交才生效，
 * 批准只放行所批的那一种。请求按后台商品页真实形状（蛇形键 sold_out、sold_out_type、reason）经控制器与真实授权链提交。
 * 复核单与失败留痕走独立事务、不随测试回滚，按本次随机后缀隔离。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarProductSoldOutJdbcIntegrationTests {

    private static final String TEMPORARY = "bar.product_sold_out";
    private static final String DELIST = "bar.product_delist";
    private static final String RELEASE = "bar.product_sold_out_release";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;
    @Autowired private BusinessDayResolver businessDays;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String bar = "bso_bar_" + s;
    private final String table = "bso_table_" + s;
    private RequestPostProcessor clerk;
    private RequestPostProcessor supervisor;

    @BeforeEach
    void seed() {
        String project = "bso_prj_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','沽清综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,'沽清酒吧','bar','active',?,now(),now())", bar, project);
        Instant now = Instant.now();
        bars.saveTable(new BarTableRecord(table, bar, bar, "B-" + s, "吧台位", "bar_counter", 2, 0, 0L, "CNY", "active", now, now));
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        String clerkId = "bso_clerk_" + s;
        String supervisorId = "bso_sup_" + s;
        // 吧台员工：下单、发起沽清与解除；本酒吧主管为其直接上级，办主管复核。
        clerk = staffs.staff(clerkId, "staff", project, bar, "bar.order.create", "bar.product.sold_out");
        supervisor = staffs.staff(supervisorId, "staff", project, bar,
                "governance.supervisor_review.view", "governance.supervisor_review.approve", "governance.supervisor_review.manage");
        jdbc.update("INSERT INTO org_hierarchy (user_id, superior_id) VALUES (?, ?)", clerkId, supervisorId);
    }

    /**
     * 临时沽清：首次提交 409 建复核单、商品仍可下单；主管批准后原样再提交生效——沽清档位临时、到期时间为该酒吧本营业日结束时刻、下单被拒、
     * 成功留痕只有一条 bar.product_sold_out。解除同样先审批：未经批准 409，批准后恢复在售并留痕 bar.product_sold_out_released。
     * 沽清不带档位 400。
     */
    @Test
    void temporarySoldOutAndItsReleaseEachNeedSupervisorApproval() throws Exception {
        String product = product("tmp");
        Map<String, Object> temporary = body(true, "temporary", "今日备货售完");

        soldOut(product, "tmp-1", temporary).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        assertThat(pending(TEMPORARY, product)).hasSize(1);
        assertThat(row(product)).containsEntry("sold_out", false);
        order(product).andExpect(status().isOk());

        approve(pending(TEMPORARY, product).getFirst());
        Instant before = Instant.now();
        soldOut(product, "tmp-2", temporary).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sold_out").value(true))
                .andExpect(jsonPath("$.data.sold_out_type").value("temporary"));
        assertThat(row(product)).containsEntry("sold_out", true).containsEntry("sold_out_type", "temporary")
                .containsEntry("sold_out_reason", "今日备货售完")
                .containsEntry("sold_out_until", Timestamp.from(businessDays.end(null, bar, businessDays.businessDate(null, bar, before))));
        order(product).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("PRODUCT_SOLD_OUT"));
        assertThat(jdbc.queryForList("SELECT event_name || '|' || (metadata->>'sold_out_type') || '|' || (metadata->>'reason')"
                + " FROM audit_events WHERE business_type = 'bar_product' AND business_id = ? AND event_result = 'success'", String.class, product))
                .containsExactly("bar.product_sold_out|temporary|今日备货售完");

        Map<String, Object> release = body(false, null, "补货到店");
        soldOut(product, "rel-1", release).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        assertThat(row(product)).containsEntry("sold_out", true);
        approve(pending(RELEASE, product).getFirst());
        soldOut(product, "rel-2", release).andExpect(status().isOk()).andExpect(jsonPath("$.data.sold_out").value(false));
        assertThat(row(product)).containsEntry("sold_out", false).containsEntry("sold_out_type", null)
                .containsEntry("sold_out_until", null).containsEntry("sold_out_at", null).containsEntry("sold_out_reason", null);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'bar.product_sold_out_released'"
                + " AND business_id = ? AND event_result = 'success'", Integer.class, product)).isEqualTo(1);
        order(product).andExpect(status().isOk());

        soldOut(product, "none-1", body(true, null, "今日备货售完")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(row(product)).containsEntry("sold_out", false);
    }

    /** 批准只放行所批的那一种：拿临时沽清的批准提交长期下架仍 409；长期下架经批准后生效，没有到期时间。 */
    @Test
    void approvalOfTemporarySoldOutDoesNotReleaseDelistAndDelistHasNoExpiry() throws Exception {
        String product = product("dl");
        soldOut(product, "dl-t1", body(true, "temporary", "今日备货售完")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        approve(pending(TEMPORARY, product).getFirst());

        Map<String, Object> delist = body(true, "long_term", "供应商停产");
        soldOut(product, "dl-1", delist).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        assertThat(row(product)).containsEntry("sold_out", false);
        approve(pending(DELIST, product).getFirst());
        soldOut(product, "dl-2", delist).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sold_out").value(true))
                .andExpect(jsonPath("$.data.sold_out_type").value("long_term"));
        assertThat(row(product)).containsEntry("sold_out", true).containsEntry("sold_out_type", "long_term")
                .containsEntry("sold_out_until", null);
        assertThat(jdbc.queryForList("SELECT event_name FROM audit_events WHERE business_type = 'bar_product' AND business_id = ?"
                + " AND event_result = 'success'", String.class, product)).containsExactly("bar.product_delisted");
        order(product).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("PRODUCT_SOLD_OUT"));
    }

    /** 改档按目标档的审批办：临时沽清中的商品提交长期下架，经长期下架审批后档位改为长期下架、到期时间清空。 */
    @Test
    void temporarySoldOutCanBeChangedToLongTermUnderDelistApproval() throws Exception {
        String product = product("chg");
        Map<String, Object> temporary = body(true, "temporary", "今日备货售完");
        soldOut(product, "chg-t1", temporary).andExpect(status().isConflict());
        approve(pending(TEMPORARY, product).getFirst());
        soldOut(product, "chg-t2", temporary).andExpect(status().isOk());
        assertThat(row(product)).containsEntry("sold_out_type", "temporary");

        Map<String, Object> delist = body(true, "long_term", "不再售卖");
        soldOut(product, "chg-d1", delist).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("ADMISSION_BLOCKED"));
        approve(pending(DELIST, product).getFirst());
        soldOut(product, "chg-d2", delist).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sold_out_type").value("long_term"));
        assertThat(row(product)).containsEntry("sold_out", true).containsEntry("sold_out_type", "long_term")
                .containsEntry("sold_out_until", null).containsEntry("sold_out_reason", "不再售卖");
    }

    // ---------- helpers ----------

    private String product(String tag) {
        String id = "bso_prd_" + tag + "_" + s;
        Instant now = Instant.now();
        bars.saveProduct(new BarProductRecord(id, bar, bar, "限量威士忌", "spirit", 8800L, "CNY", null, null, false, "active",
                now, now, null, null, null, false, null, null, null));
        return id;
    }

    /** 后台商品页沽清弹框提交的请求体（幂等键由客户端另补）。 */
    private static Map<String, Object> body(boolean soldOut, String soldOutType, String reason) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sold_out", soldOut);
        if (soldOutType != null) {
            body.put("sold_out_type", soldOutType);
        }
        body.put("reason", reason);
        return body;
    }

    private ResultActions soldOut(String product, String key, Map<String, Object> content) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>(content);
        body.put("idempotency_key", "bso-" + key + "-" + s);
        return call(clerk, "/api/v1/bar/products/" + product + "/sold-out", body);
    }

    /** 吧台位下一单该商品（沽清即 409 PRODUCT_SOLD_OUT）。 */
    private ResultActions order(String product) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bso-order-" + UUID.randomUUID());
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("items", List.of(Map.of("product_id", product, "quantity", 1)));
        return call(clerk, "/api/v1/bar/orders", body);
    }

    private List<String> pending(String action, String product) {
        return jdbc.queryForList("SELECT review_id FROM supervisor_review_decisions WHERE action_type = ? AND object_type = 'bar_product'"
                + " AND object_id = ? AND review_status = 'pending_review'", String.class, action, product);
    }

    /** 直接上级在主管复核页批准。 */
    private void approve(String reviewId) throws Exception {
        mockMvc.perform(post("/api/v1/platform/supervisor-review/" + reviewId + "/chain/regenerate").with(supervisor))
                .andExpect(status().isOk());
        call(supervisor, "/api/v1/platform/supervisor-review/" + reviewId + "/approve", Map.of(
                "idempotency_key", "bso-approve-" + UUID.randomUUID(), "decision", "approved", "comment", "核实属实")).andExpect(status().isOk());
    }

    private ResultActions call(RequestPostProcessor who, String path, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private Map<String, Object> row(String product) {
        return jdbc.queryForMap("SELECT * FROM bar_products WHERE id = ?", product);
    }
}
