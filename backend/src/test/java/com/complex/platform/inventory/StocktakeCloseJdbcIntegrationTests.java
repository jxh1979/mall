package com.complex.platform.inventory;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
 * 第十八批 BK 段修复项 F01：建盘点单后该库房入库被冻结（409 INVENTORY_WAREHOUSE_STOCKTAKE_LOCKED「仓库正在盘点中，盘点结束前库存变动已冻结」），
 * 此前没有任何结束盘点的入口，库房永久冻结。BSM:1164「记录入库、出库、调拨、盘点、报损、报溢和库存调整。」——入库出库是日常动作。
 * 真库、真接口（请求体按前端蛇形键、真实授权链）：建单→入库被拒→有待复核差异时结束被拒→复核后结束→入库放行；
 * 结束盘点核建单权限与商户归属，同号重放返回原结果，已结束的不能再结束、也不能再登记差异。夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class StocktakeCloseJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private InventoryRepository inventoryRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "f01c_prj_" + suffix;
    private final String merchant = "f01c_mch_" + suffix;
    private final String otherMerchant = "f01c_mch2_" + suffix;
    private final String item = "f01c_item_" + suffix;
    private final String warehouse = "f01c_wh_" + suffix;

    @BeforeEach
    void seed() {
        String entity = "f01c_ent_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','结束盘点回归项目','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'门店主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
        for (String id : List.of(merchant, otherMerchant)) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,?,'retail',?,?,'active',now(),now())", id, id, entity, project);
            jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\",\"finance\"]'::jsonb,now(),now())", "ct_" + id, id, entity, entity);
        }
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) VALUES(?,'结束盘点测试酒水','liquor','瓶',0,'active',?,now(),now())", item, merchant);
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,'bar','吧台仓','active',?)", warehouse, project, merchant);
        jdbc.update("INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,merchant_id,project_id,warehouse_id,updated_at) VALUES(?,?,?,10,0,10,?,?,?,now())",
                "f01c_bal_" + suffix, item, warehouse, merchant, project, warehouse);
        // 低价值（最近到货价 10 元 < 出厂阈值 5000 元）：本用例登记差异不带照片视频。
        inventoryRepository.saveStockMovement(new StockMovementRecord("f01c_mv_" + suffix, "inbound", item, null, warehouse, 1,
                1_000L, null, "purchase_request", "pr_" + item, null, null, List.of(), "completed",
                Instant.now().minusSeconds(60), merchant), project);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void closingStocktakeUnfreezesWarehouseOnceEveryVarianceIsReviewed() throws Exception {
        RequestPostProcessor keeper = account("kp", merchant,
                "'inventory.stocktake.create','inventory.stocktake.variance_record','inventory.stock.move'");
        RequestPostProcessor reviewer = account("rev", merchant, "'inventory.stocktake.variance_review'");

        String stocktake = openStocktake(keeper);
        // 建单即冻结：该库房入库 409。
        stockIn(keeper).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INVENTORY_WAREHOUSE_STOCKTAKE_LOCKED"));

        String variance = data(call(keeper, "/api/v1/inventory/stocktakes/" + stocktake + "/variances",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"item_id\":\"" + item + "\",\"counted_qty\":8,\"reason\":\"shortage\"}")
                .andExpect(status().isOk())).path("id").asText();

        // 还有待复核的差异：不许结束，盘点单仍在盘点中。
        close(keeper, stocktake, UUID.randomUUID().toString()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("STOCKTAKE_HAS_PENDING_VARIANCE"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("待复核")));
        assertThat(statusOf(stocktake)).isEqualTo("counting");

        call(reviewer, "/api/v1/inventory/stocktakes/" + stocktake + "/variances/" + variance + "/review",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"decision\":\"approved\",\"reason\":\"复盘一致\"}")
                .andExpect(status().isOk());
        assertThat(available()).isEqualTo(8);

        // 复核完再结束：置已关闭并留痕。
        close(keeper, stocktake, UUID.randomUUID().toString()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(stocktake))
                .andExpect(jsonPath("$.data.status").value("closed"));
        assertThat(statusOf(stocktake)).isEqualTo("closed");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='inventory.stocktake.closed' AND business_type='stocktake' AND business_id=?",
                Long.class, stocktake)).isEqualTo(1L);

        // 结束后该库房入库放行。
        stockIn(keeper).andExpect(status().isOk());
        assertThat(available()).isEqualTo(9);
        // 已关闭的盘点单不能再登记差异。
        call(keeper, "/api/v1/inventory/stocktakes/" + stocktake + "/variances",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"item_id\":\"" + item + "\",\"counted_qty\":7,\"reason\":\"shortage\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("STOCKTAKE_NOT_OPEN"));
    }

    @Test
    void closeNeedsCreatePermissionAndOwnMerchantAndReplaysBySameKey() throws Exception {
        RequestPostProcessor keeper = account("kp", merchant, "'inventory.stocktake.create'");
        RequestPostProcessor viewer = account("vw", merchant, "'inventory.stocktake.view'");
        RequestPostProcessor outsider = account("out", otherMerchant, "'inventory.stocktake.create'");
        String stocktake = openStocktake(keeper);

        // 没有建单权限的不能结束。
        close(viewer, stocktake, UUID.randomUUID().toString()).andExpect(status().isForbidden());
        // 别的商户的账号不能结束本商户的盘点单。
        close(outsider, stocktake, UUID.randomUUID().toString()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        assertThat(statusOf(stocktake)).isEqualTo("counting");

        String key = UUID.randomUUID().toString();
        close(keeper, stocktake, key).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
        // 同号重放返回原结果，不重复留痕。
        close(keeper, stocktake, key).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name='inventory.stocktake.closed' AND business_id=?",
                Long.class, stocktake)).isEqualTo(1L);
        // 已结束的盘点单换号再结束 409。
        close(keeper, stocktake, UUID.randomUUID().toString()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("STOCKTAKE_NOT_OPEN"));
        // 缺请求编号 400。
        call(keeper, "/api/v1/inventory/stocktakes/" + stocktake + "/close", "{}").andExpect(status().isBadRequest());
    }

    private String openStocktake(RequestPostProcessor who) throws Exception {
        return data(call(who, "/api/v1/inventory/stocktakes",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"warehouse_id\":\"" + warehouse + "\",\"remark\":\"月末盘点\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("counting"))).path("id").asText();
    }

    private ResultActions close(RequestPostProcessor who, String stocktake, String key) throws Exception {
        return call(who, "/api/v1/inventory/stocktakes/" + stocktake + "/close", "{\"idempotency_key\":\"" + key + "\"}");
    }

    /** 通用入库的真实请求形状（蛇形键）：期初来源入 1 瓶。 */
    private ResultActions stockIn(RequestPostProcessor who) throws Exception {
        return call(who, "/api/v1/inventory/stock-in",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"source_type\":\"initial\",\"warehouse_id\":\"" + warehouse
                        + "\",\"items\":[{\"item_id\":\"" + item + "\",\"quantity\":1}]}");
    }

    private ResultActions call(RequestPostProcessor who, String path, String body) throws Exception {
        return mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
    }

    private String statusOf(String stocktake) {
        return jdbc.queryForObject("SELECT status FROM stocktake WHERE id=?", String.class, stocktake);
    }

    private int available() {
        Integer value = jdbc.queryForObject("SELECT available_quantity FROM inventory_stock_balances WHERE item_id=? AND location_id=?", Integer.class, item, warehouse);
        return value == null ? -1 : value;
    }

    private RequestPostProcessor account(String tag, String merchantId, String permissions) {
        String userId = "f01c_" + tag + "_" + suffix;
        String role = "f01c_role_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'结束盘点回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN(" + permissions + ")", role);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())", userId, userId, project, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
