package com.complex.platform.inventory;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.evidence.EvidenceEventService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批第 24 条 24-R3：盘点差异「谁录的谁不能复核」。PGM:529「申请人与直接受益人不得审批与自己相关的申请，转发不构成例外」；
 * BSM:1196「退酒、报损、盘点差异、存酒作废等需主管复核或先审后行的场景一律以此判定」。真库、真接口（请求体按前端蛇形键）：
 * 同一账号录差异后复核 409、不纠账；另一名复核人批准后纠账。
 * <p>24-R4：ENUM:164「报损/盘点差异/存酒作废须照片+视频双证据+主管复核」；BSM:1183「高价值酒水报损、盘点差异、存酒作废必须拍照和视频留证，
 * 并主管复核」；任务书 18D 裁决补充第 14 条「只高价值的要」——高价值差异缺视频 400，带真实照片与视频可录、他人复核后纠账；低价值不要附件。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class StocktakeVarianceSelfReviewJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private InventoryRepository inventoryRepository;
    @Autowired private EvidenceEventService evidence;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "g24v_prj_" + suffix;
    private final String merchant = "g24v_mch_" + suffix;
    private final String item = "g24v_item_" + suffix;
    private final String warehouse = "g24v_wh_" + suffix;

    @BeforeEach
    void seed() {
        String entity = "g24v_ent_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','盘点复核回归项目','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'门店主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,?,'retail',?,?,'active',now(),now())", merchant, merchant, entity, project);
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\",\"finance\"]'::jsonb,now(),now())", "g24v_ct_" + suffix, merchant, entity, entity);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) VALUES(?,'盘点测试酒水','liquor','瓶',0,'active',?,now(),now())", item, merchant);
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,'bar','吧台仓','active',?)", warehouse, project, merchant);
        jdbc.update("INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,merchant_id,project_id,warehouse_id,updated_at) VALUES(?,?,?,10,0,10,?,?,?,now())",
                "g24v_bal_" + suffix, item, warehouse, merchant, project, warehouse);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recorderCannotReviewOwnVarianceButAnotherReviewerCan() throws Exception {
        arrival(1_000L); // 低价值（10 元 < 出厂阈值 5000 元），本用例只看「录入人不得复核」
        RequestPostProcessor counter = account("cnt", "'inventory.stocktake.create','inventory.stocktake.variance_record','inventory.stocktake.variance_review'");
        RequestPostProcessor reviewer = account("rev", "'inventory.stocktake.variance_review'");
        String stocktake = data(call(counter, "/api/v1/inventory/stocktakes",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"warehouse_id\":\"" + warehouse + "\",\"remark\":\"月末盘点\"}")
                .andExpect(status().isOk())).path("id").asText();
        String variance = data(call(counter, "/api/v1/inventory/stocktakes/" + stocktake + "/variances",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"item_id\":\"" + item + "\",\"counted_qty\":8,\"reason\":\"shortage\"}")
                .andExpect(status().isOk())).path("id").asText();

        review(counter, stocktake, variance)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("INVENTORY_VARIANCE_SELF_REVIEW"));
        assertThat(available()).isEqualTo(10);

        review(reviewer, stocktake, variance)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("approved"));
        assertThat(available()).isEqualTo(8);
    }

    // 24-R4：高价值（最近一次到货价 6000 元 ≥ 出厂阈值 5000 元）差异缺视频 → 400，不落差异。
    @Test
    void highValueVarianceWithoutVideoIsRejected() throws Exception {
        arrival(600_000L);
        RequestPostProcessor counter = account("cnt", "'inventory.stocktake.create','inventory.stocktake.variance_record','evidence.attachment.create'");
        String stocktake = openStocktake(counter);
        String photo = upload("g24v_cnt_" + suffix, "evidence.jpg", "image/jpeg", "photo");

        recordVariance(counter, stocktake, List.of(photo), List.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("HIGH_VALUE_EVIDENCE_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventory_variance WHERE stocktake_id=?", Long.class, stocktake)).isZero();
    }

    // 24-R4：高价值差异带真实上传的照片与视频可录，证据落在差异上；另一名复核人批准后纠账。
    @Test
    void highValueVarianceWithPhotoAndVideoIsRecordedAndAnotherReviewerReconciles() throws Exception {
        arrival(600_000L);
        RequestPostProcessor counter = account("cnt", "'inventory.stocktake.create','inventory.stocktake.variance_record','evidence.attachment.create'");
        RequestPostProcessor reviewer = account("rev", "'inventory.stocktake.variance_review'");
        String stocktake = openStocktake(counter);
        String photo = upload("g24v_cnt_" + suffix, "evidence.jpg", "image/jpeg", "photo");
        String video = upload("g24v_cnt_" + suffix, "evidence.mp4", "video/mp4", "video");

        String variance = data(recordVariance(counter, stocktake, List.of(photo), List.of(video))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.photo_attachment_ids[0]").value(photo))
                .andExpect(jsonPath("$.data.video_attachment_ids[0]").value(video))).path("id").asText();

        review(reviewer, stocktake, variance).andExpect(status().isOk());
        assertThat(available()).isEqualTo(8);
    }

    // 24-R4：低价值（到货价 10 元）差异不要照片视频，照旧可录。
    @Test
    void lowValueVarianceNeedsNoEvidence() throws Exception {
        arrival(1_000L);
        RequestPostProcessor counter = account("cnt", "'inventory.stocktake.create','inventory.stocktake.variance_record'");
        String stocktake = openStocktake(counter);

        recordVariance(counter, stocktake, List.of(), List.of()).andExpect(status().isOk());
    }

    private String openStocktake(RequestPostProcessor who) throws Exception {
        return data(call(who, "/api/v1/inventory/stocktakes",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"warehouse_id\":\"" + warehouse + "\",\"remark\":\"月末盘点\"}")
                .andExpect(status().isOk())).path("id").asText();
    }

    /** 录差异的真实请求形状（蛇形键）：账面 10、实盘 8。 */
    private ResultActions recordVariance(RequestPostProcessor who, String stocktake, List<String> photos, List<String> videos) throws Exception {
        return call(who, "/api/v1/inventory/stocktakes/" + stocktake + "/variances",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"item_id\":\"" + item + "\",\"counted_qty\":8,\"reason\":\"shortage\","
                        + "\"photo_attachment_ids\":" + json.writeValueAsString(photos) + ",\"video_attachment_ids\":" + json.writeValueAsString(videos) + "}");
    }

    /** 采购到货流水（与 PurchaseRequestService.receive 写入的行同形）：高价值判定只看最近一次实际到货价。 */
    private void arrival(long unitCostCent) {
        inventoryRepository.saveStockMovement(new StockMovementRecord("mv_g24v_" + suffix, "inbound", item, null, warehouse, 1,
                unitCostCent, null, "purchase_request", "pr_" + item, null, null, List.of(), "completed",
                Instant.now().minusSeconds(60), merchant), project);
    }

    private String upload(String userId, String name, String contentType, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "staff", project, merchant, "active"), "test", List.of()));
        try {
            return evidence.uploadAttachment(new MockMultipartFile("file", name, contentType, "fake-bytes".getBytes()),
                    "inventory_variance", null, role).id();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private ResultActions review(RequestPostProcessor who, String stocktake, String variance) throws Exception {
        return call(who, "/api/v1/inventory/stocktakes/" + stocktake + "/variances/" + variance + "/review",
                "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"decision\":\"approved\",\"reason\":\"复盘一致\"}");
    }

    private ResultActions call(RequestPostProcessor who, String path, String body) throws Exception {
        return mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private JsonNode data(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString()).path("data");
    }

    private int available() {
        Integer value = jdbc.queryForObject("SELECT available_quantity FROM inventory_stock_balances WHERE item_id=? AND location_id=?", Integer.class, item, warehouse);
        return value == null ? -1 : value;
    }

    private RequestPostProcessor account(String tag, String permissions) {
        String userId = "g24v_" + tag + "_" + suffix;
        String role = "g24v_role_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'盘点复核回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN(" + permissions + ")", role);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())", userId, userId, project, merchant);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchant, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
