package com.complex.platform.inventory;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.evidence.EvidenceEventService;
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
 * FS-018 R1 真库、真接口回归（请求体按前端真实形状、蛇形键）：typed stock-movements 的 damage/loss/waste 不再直接扣库存，
 * 统一进入既有高价值判定与照片+视频双证据主管复核流程，批准后才影响库存；非高价值照旧即时调整并补流水。
 * BSM L1183「高价值酒水报损、盘点差异、存酒作废必须拍照和视频留证，并主管复核」；
 * BSM L1196「阈值未配置时一律按高价值处理、拦下走审批，不得静默放行。报损、盘点差异、存酒作废必须照片+视频双证据+主管复核」；
 * BSM L2016「已拍板：高价值酒水报损、盘点差异、存酒作废必须主管复核，并拍照/视频留证」；ENUM L164 同。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class TypedDamageHighValueJdbcIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private EvidenceEventService evidence;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "fs018_prj_" + suffix;
    private final String merchant = "fs018_mch_" + suffix;
    private final String item = "fs018_item_" + suffix;
    private final String location = "fs018_loc_" + suffix;
    private final String userId = "fs018_staff_" + suffix;
    private RequestPostProcessor staff;

    @BeforeEach
    void seed() {
        String entity = "fs018_ent_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','报损复核回归项目','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'门店主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,?,'retail',?,?,'active',now(),now())", merchant, merchant, entity, project);
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\",\"finance\"]'::jsonb,now(),now())", "fs018_ct_" + suffix, merchant, entity, entity);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) VALUES(?,'报损测试酒水','liquor','瓶',0,'active',?,now(),now())", item, merchant);
        jdbc.update("INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,merchant_id,project_id,updated_at) VALUES(?,?,?,10,0,10,?,?,now())",
                "fs018_bal_" + suffix, item, location, merchant, project);
        String role = "fs018_role_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'报损复核回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN('inventory.stock.move','inventory.stock.adjust','evidence.attachment.create')", role);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())", userId, userId, project, merchant);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        AccessActor actor = actor();
        staff = request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // 高价值（最近一次到货价 ≥ 出厂阈值 5000 元）+ 照片与视频双证据 → 返回 pending_review 调整单、建 pending 复核单、库存不动。
    @Test
    void highValueTypedDamageWithBothEvidencesIsHeldForSupervisorReviewAndDoesNotTouchStock() throws Exception {
        arrival(600_000L);
        String photo = upload("evidence.jpg", "image/jpeg", "photo");
        String video = upload("evidence.mp4", "video/mp4", "video");

        report("damage", body("碰落破损", List.of(photo), List.of(video)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"));

        assertThat(available()).isEqualTo(10);
        assertThat(jdbc.queryForList("SELECT subject_type,review_status FROM inventory_high_value_reviews WHERE merchant_id=?", merchant))
                .singleElement().satisfies(r -> assertThat(r)
                        .containsEntry("subject_type", "damage_report").containsEntry("review_status", "pending"));
        assertThat(jdbc.queryForList("SELECT adjustment_type,status,reason FROM inventory_stock_adjustments WHERE item_id=?", item))
                .singleElement().satisfies(a -> assertThat(a)
                        .containsEntry("adjustment_type", "damage").containsEntry("status", "pending_review").containsEntry("reason", "碰落破损"));
    }

    // 高价值缺视频证据 → 400 HIGH_VALUE_EVIDENCE_REQUIRED，库存不动、不建复核单。
    @Test
    void highValueTypedDamageWithoutVideoIsRejectedAndStockUnchanged() throws Exception {
        arrival(600_000L);
        String photo = upload("evidence.jpg", "image/jpeg", "photo");

        report("damage", body("碰落破损", List.of(photo), List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("HIGH_VALUE_EVIDENCE_REQUIRED"));

        assertThat(available()).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventory_high_value_reviews WHERE merchant_id=?", Long.class, merchant)).isZero();
    }

    // 非高价值（到货价 10 元 < 阈值）→ 照旧即时调整：completed、库存扣减、守恒账本补一条 damage 流水。
    @Test
    void lowValueTypedDamageAdjustsImmediatelyWithMovementTrail() throws Exception {
        arrival(1_000L);

        report("damage", body("过期报废", List.of(), List.of()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("completed"));

        assertThat(available()).isEqualTo(7);
        assertThat(jdbc.queryForList("SELECT movement_type,business_type,quantity FROM inventory_stock_movements WHERE item_id=? AND movement_type='damage'", item))
                .singleElement().satisfies(m -> assertThat(m)
                        .containsEntry("business_type", "inventory_stock_adjustment").containsEntry("quantity", 3));
    }

    // 第十八批第 24 条 24-R2（ENUM:164「报损/盘点差异/存酒作废须照片+视频双证据+主管复核」；BSM:1658 主管复核强制项）：
    // 后台高价值复核页的列表带复核对象摘要（物品名、数量），由另一名门店主管按前端蛇形键批准后才扣库存。
    @Test
    void reviewListCarriesSubjectSummaryAndAnotherSupervisorApprovesToDeduct() throws Exception {
        arrival(600_000L);
        String photo = upload("evidence.jpg", "image/jpeg", "photo");
        String video = upload("evidence.mp4", "video/mp4", "video");
        report("damage", body("碰落破损", List.of(photo), List.of(video))).andExpect(status().isOk());
        RequestPostProcessor reviewer = account("rev", "'inventory.high_value.review'");
        String review = jdbc.queryForObject("SELECT id FROM inventory_high_value_reviews WHERE merchant_id=?", String.class, merchant);

        mockMvc.perform(get("/api/v1/inventory/high-value-reviews").with(reviewer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(review))
                .andExpect(jsonPath("$.data.items[0].subject_summary.item_name").value("报损测试酒水"))
                .andExpect(jsonPath("$.data.items[0].subject_summary.quantity").value(3))
                .andExpect(jsonPath("$.data.items[0].subject_summary.adjustment_type").value("damage"));

        decide(reviewer, review, "approved")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("approved"));
        assertThat(available()).isEqualTo(7);
    }

    // 第十八批第 24 条 24-R3（PGM:529「申请人与直接受益人不得审批与自己相关的申请，转发不构成例外」）：
    // 报损发起人即使同时持主管复核码，也不能批准自己的高价值报损，库存不动。
    @Test
    void applicantCannotApproveOwnHighValueDamage() throws Exception {
        arrival(600_000L);
        String photo = upload("evidence.jpg", "image/jpeg", "photo");
        String video = upload("evidence.mp4", "video/mp4", "video");
        report("damage", body("碰落破损", List.of(photo), List.of(video))).andExpect(status().isOk());
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'inventory.high_value.review')", "fs018_role_" + suffix);
        String review = jdbc.queryForObject("SELECT id FROM inventory_high_value_reviews WHERE merchant_id=?", String.class, merchant);

        decide(staff, review, "approved")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("HIGH_VALUE_REVIEW_SELF_APPROVAL"));
        assertThat(available()).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT review_status FROM inventory_high_value_reviews WHERE id=?", String.class, review))
                .isEqualTo("pending");
    }

    /** 后台高价值复核页「批准」「驳回」的真实请求形状（蛇形键）。 */
    private ResultActions decide(RequestPostProcessor who, String review, String decision) throws Exception {
        return mockMvc.perform(post("/api/v1/inventory/high-value-reviews/" + review + "/review").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"decision\":\"" + decision + "\",\"reason\":\"核对无误\"}"));
    }

    /** 同商户另一员工账号（门店主管等）。 */
    private RequestPostProcessor account(String tag, String permissions) {
        String other = "fs018_" + tag + "_" + suffix;
        String role = "fs018_role_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'报损复核回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN(" + permissions + ")", role);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())", other, other, project, merchant);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", other, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", other, project);
        AccessActor actor = new AccessActor(other, other, "staff", project, merchant, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private ResultActions report(String type, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/inventory/stock-movements/" + type).with(staff)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 前端真实请求形状（蛇形键）。 */
    private String body(String note, List<String> photos, List<String> videos) {
        return "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"operator_note\":\"" + note + "\","
                + "\"from_location_id\":\"" + location + "\",\"items\":[{\"item_id\":\"" + item + "\",\"quantity\":3}],"
                + "\"photo_attachment_ids\":" + json(photos) + ",\"video_attachment_ids\":" + json(videos) + "}";
    }

    /** 采购到货流水（与 PurchaseRequestService.receive 写入的行同形）：高价值判定只看最近一次实际到货价。 */
    private void arrival(long unitCostCent) {
        inventoryRepository.saveStockMovement(new StockMovementRecord("mv_fs018_" + suffix, "inbound", item, null, location, 1,
                unitCostCent, null, "purchase_request", "pr_" + item, null, null, List.of(), "completed",
                Instant.now().minusSeconds(60), merchant), project);
    }

    private String upload(String name, String contentType, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor(), "test", List.of()));
        try {
            return evidence.uploadAttachment(new MockMultipartFile("file", name, contentType, "fake-bytes".getBytes()),
                    "inventory_stock_adjustment", null, role).id();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private AccessActor actor() {
        return new AccessActor(userId, userId, "staff", project, merchant, "active");
    }

    private int available() {
        Integer value = jdbc.queryForObject("SELECT available_quantity FROM inventory_stock_balances WHERE item_id=? AND location_id=?", Integer.class, item, location);
        return value == null ? -1 : value;
    }

    private static String json(List<String> values) {
        return values.isEmpty() ? "[]" : "[\"" + String.join("\",\"", values) + "\"]";
    }
}
