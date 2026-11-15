package com.complex.platform.bar;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * 第十八批 AK 段第 221 条 221-R2（真库）：ENUM:111「临时沽清（本营业日有效，营业日切换时点自动恢复在售）」。临时沽清到期的商品由自动恢复作业
 * 恢复在售并留痕 bar.product_sold_out_auto_restored（不经审批）；未到期的临时沽清与长期下架不动；再跑一轮不重复留痕；恢复后可下单
 * （下单按后台订单页真实请求形状）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class BarSoldOutRestoreJobJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private BarRepository bars;
    @Autowired private BarSoldOutRestoreJob job;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String bar = "bsr_bar_" + s;
    private final String table = "bsr_table_" + s;
    private RequestPostProcessor clerk;

    @BeforeEach
    void seed() {
        String project = "bsr_prj_" + s;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','沽清恢复综合体','active',now(),now())", project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,'沽清恢复酒吧','bar','active',?,now(),now())", bar, project);
        Instant now = Instant.now();
        bars.saveTable(new BarTableRecord(table, bar, bar, "B-" + s, "吧台位", "bar_counter", 2, 0, 0L, "CNY", "active", now, now));
        clerk = new StaffRequestFixture(jdbc).staff("bsr_clerk_" + s, "staff", project, bar, "bar.order.create");
    }

    @Test
    void expiredTemporarySoldOutIsRestoredOnceWhileOthersStay() throws Exception {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant expiredUntil = now.minus(1, ChronoUnit.MINUTES);
        String expired = product("expired", "temporary", expiredUntil);
        String notYet = product("notyet", "temporary", now.plus(3, ChronoUnit.HOURS));
        String delisted = product("delisted", "long_term", null);
        order(expired).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("PRODUCT_SOLD_OUT"));

        job.restoreExpired(now);

        assertThat(row(expired)).containsEntry("sold_out", false).containsEntry("sold_out_at", null)
                .containsEntry("sold_out_reason", null).containsEntry("sold_out_type", null).containsEntry("sold_out_until", null);
        assertThat(jdbc.queryForList("SELECT metadata->>'product_id' || '|' || (metadata->>'sold_out_until') FROM audit_events"
                + " WHERE event_name = 'bar.product_sold_out_auto_restored' AND business_type = 'bar_product' AND business_id = ?",
                String.class, expired)).containsExactly(expired + "|" + expiredUntil);
        assertThat(row(notYet)).containsEntry("sold_out", true).containsEntry("sold_out_type", "temporary");
        assertThat(row(delisted)).containsEntry("sold_out", true).containsEntry("sold_out_type", "long_term");

        job.restoreExpired(now.plus(1, ChronoUnit.MINUTES));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'bar.product_sold_out_auto_restored'"
                + " AND business_id IN (?, ?, ?)", Integer.class, expired, notYet, delisted)).isEqualTo(1);
        assertThat(row(notYet)).containsEntry("sold_out", true);
        assertThat(row(delisted)).containsEntry("sold_out", true);

        order(expired).andExpect(status().isOk());
    }

    // ---------- helpers ----------

    private String product(String tag, String soldOutType, Instant until) {
        String id = "bsr_prd_" + tag + "_" + s;
        Instant at = Instant.now().minus(2, ChronoUnit.HOURS);
        bars.saveProduct(new BarProductRecord(id, bar, bar, "限量威士忌", "spirit", 8800L, "CNY", null, null, false, "active",
                at, at, null, null, null, false, null, null, null).withSoldOut(soldOutType, at, "今日备货售完", until, at));
        return id;
    }

    /** 后台订单页「下单」：吧台位下一单该商品。 */
    private ResultActions order(String product) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("idempotency_key", "bsr-order-" + UUID.randomUUID());
        body.put("bar_id", bar);
        body.put("table_id", table);
        body.put("items", List.of(Map.of("product_id", product, "quantity", 1)));
        return mockMvc.perform(post("/api/v1/bar/orders").with(clerk).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private Map<String, Object> row(String product) {
        return jdbc.queryForMap("SELECT * FROM bar_products WHERE id = ?", product);
    }
}
