package com.complex.platform.inventory;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
 * OBS-13-02 真库回归（接口请求体按前端真实形状、蛇形键）。
 * R1：BSM L1072「存酒到期后，系统自动结束客户存酒权益，该瓶酒先转为所在酒吧的自有库存资产；转为库存时不确认收入」、L1194 库存流水
 * 「回库（……客人存酒到期转为酒吧自有库存）」、L863 注销放弃「按存酒到期规则转为所在酒吧的自有库存资产」、L1169「按标签显示数值记录剩余量」；
 * 操作员裁决补充（第十四批）第 3 条：酒吧退出后交平台保管的存酒到期，转为接收保管的娱乐平台公司的库存。
 * R2：BSM L1072「转库存之后的报废、赠送、内部使用等处置必须走审批并保留操作记录」；第十四批第 4 条：门店主管与财务审批
 * （同「退出前无法交还的赔付方案」那样），批准后才扣库存，报废沿用既有报损证据要求。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SavedBottleStockDisposalJdbcIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private SavedBottleNotificationJob notificationJob;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "o1302_prj_" + suffix;
    private final String merchant = "o1302_mch_" + suffix;
    private final String item = "o1302_item_" + suffix;
    private final String savedWarehouse = "o1302_wsb_" + suffix;
    private final String barWarehouse = "o1302_wbar_" + suffix;
    private final Instant now = Instant.now();
    private RequestPostProcessor staff;
    private RequestPostProcessor supervisor;
    private RequestPostProcessor finance;
    private RequestPostProcessor both;

    @BeforeEach
    void seed() {
        String entity = "o1302_ent_" + suffix;
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'mall','存酒转库存回归项目','active',now(),now())", project);
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'门店主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,?,'retail',?,?,'active',now(),now())", merchant, merchant, entity, project);
        jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\",\"finance\"]'::jsonb,now(),now())", "o1302_ct_" + suffix, merchant, entity, entity);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) VALUES(?,'存酒测试红酒','liquor','瓶',0,'active',?,now(),now())", item, merchant);
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,'bar','吧台仓','active',?)", barWarehouse, project, merchant);
        staff = account("staff", "'inventory.stock.move','inventory.stock.adjust','evidence.attachment.create'");
        supervisor = account("sup", "'inventory.high_value.review'");
        finance = account("fin", "'finance.ar_ap.manage'");
        both = account("both", "'inventory.high_value.review','finance.ar_ap.manage'");
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // R1：到期瓶按标签剩余量以「回库」流水转入所在酒吧的存酒库房；重跑不重复；不产生任何应收应付（不确认收入）。
    @Test
    void expiredBottleBecomesBarSavedBottleWarehouseStockOnceWithoutRevenue() {
        savedBottleWarehouse(savedWarehouse, merchant);
        String bottle = storedBottle("exp", 3, now.minus(Duration.ofDays(1)), null);

        notificationJob.runOnce(now);
        notificationJob.runOnce(now.plusSeconds(60));

        assertThat(jdbc.queryForObject("SELECT status FROM saved_bottles WHERE id=?", String.class, bottle)).isEqualTo("expired");
        assertThat(toStockMovements(bottle)).singleElement().satisfies(m -> assertThat(m)
                .containsEntry("movement_type", "return")
                .containsEntry("source_location_id", null)
                .containsEntry("target_location_id", savedWarehouse)
                .containsEntry("quantity", 3)
                .containsEntry("merchant_id", merchant)
                .containsEntry("project_id", project));
        assertThat(balance(savedWarehouse)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT warehouse_id FROM inventory_stock_balances WHERE item_id=? AND location_id=?",
                String.class, item, savedWarehouse)).isEqualTo(savedWarehouse);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_id=?", Long.class, bottle)).isZero();
        assertThat(audits("bar.saved_bottle.returned_to_stock", bottle)).isEqualTo(1);
    }

    // R1：酒吧没有在用存酒库房 → 不入库、留失败审计、瓶仍 expired；补建库房后下一次作业自动补记。
    @Test
    void missingSavedBottleWarehouseLeavesFailureAuditAndPostsAfterWarehouseIsCreated() {
        String bottle = storedBottle("miss", 2, now.minus(Duration.ofDays(1)), null);

        notificationJob.runOnce(now);

        assertThat(toStockMovements(bottle)).isEmpty();
        assertThat(audits("bar.saved_bottle.return_to_stock_failed", bottle)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM saved_bottles WHERE id=?", String.class, bottle)).isEqualTo("expired");

        savedBottleWarehouse(savedWarehouse, merchant);
        notificationJob.runOnce(now.plusSeconds(60));

        assertThat(toStockMovements(bottle)).hasSize(1);
        assertThat(balance(savedWarehouse)).isEqualTo(2);
    }

    // R1 + BSM L863：注销时放弃的存酒置 expired 后，下一次作业按同一规则转入所在酒吧库存。
    @Test
    void bottleAbandonedOnDeregistrationIsPostedToStockByNextRun() {
        savedBottleWarehouse(savedWarehouse, merchant);
        String customer = "o1302_cus_" + suffix;
        String bottle = storedBottle("abd", 1, now.plus(Duration.ofDays(20)), null, customer);

        assertThat(inventoryService.abandonSavedBottlesOnDeregistration(customer, null, now)).isEqualTo(1);
        notificationJob.runOnce(now);

        assertThat(toStockMovements(bottle)).singleElement().satisfies(m -> assertThat(m).containsEntry("quantity", 1));
        assertThat(balance(savedWarehouse)).isEqualTo(1);
    }

    // 第十四批第 3 条：交娱乐平台公司保管的存酒到期 → 转入该公司在本项目的存酒库房（平台库房无商户归属），不进原酒吧库房。
    @Test
    void platformCustodyBottleBecomesPlatformCompanyStock() {
        savedBottleWarehouse(savedWarehouse, merchant);
        String platformWarehouse = "o1302_wpf_" + suffix;
        savedBottleWarehouse(platformWarehouse, null);
        String bottle = storedBottle("cus", 2, now.minus(Duration.ofDays(1)), "o1302_pent_" + suffix);

        notificationJob.runOnce(now);

        assertThat(toStockMovements(bottle)).singleElement().satisfies(m -> assertThat(m)
                .containsEntry("target_location_id", platformWarehouse)
                .containsEntry("merchant_id", null));
        assertThat(balance(platformWarehouse)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT merchant_id FROM inventory_stock_balances WHERE item_id=? AND location_id=?",
                String.class, item, platformWarehouse)).isNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventory_stock_balances WHERE item_id=? AND location_id=?",
                Long.class, item, savedWarehouse)).isZero();
    }

    // R2：存酒库房的出库（赠送、内部使用）先审批：门店主管批后仍不扣，财务也批后才扣减并留出库流水。
    @Test
    void outboundFromSavedBottleWarehouseWaitsForSupervisorAndFinanceBeforeTouchingStock() throws Exception {
        seedSavedBottleStock(2);

        mockMvc.perform(post("/api/v1/inventory/stock-movements").with(staff).contentType(MediaType.APPLICATION_JSON)
                        .content(outboundBody(savedWarehouse, "赠送给常客")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"));

        assertThat(balance(savedWarehouse)).isEqualTo(2);
        Map<String, Object> adjustment = jdbc.queryForMap(
                "SELECT id,adjustment_type,status,reason FROM inventory_stock_adjustments WHERE item_id=? AND location_id=?", item, savedWarehouse);
        assertThat(adjustment).containsEntry("adjustment_type", "outbound").containsEntry("status", "pending_review")
                .containsEntry("reason", "赠送给常客");
        String review = reviewId();
        assertThat(jdbc.queryForObject("SELECT subject_type FROM inventory_high_value_reviews WHERE id=?", String.class, review))
                .isEqualTo("saved_bottle_stock_disposal");

        decide(supervisor, review, "approved", "supervisor").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("pending"));
        assertThat(balance(savedWarehouse)).isEqualTo(2);

        decide(finance, review, "approved", "finance").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("approved"));
        assertThat(balance(savedWarehouse)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT movement_type,quantity FROM inventory_stock_movements WHERE item_id=? AND source_location_id=? AND business_id=?",
                item, savedWarehouse, adjustment.get("id"))).singleElement().satisfies(m -> assertThat(m)
                .containsEntry("movement_type", "outbound").containsEntry("quantity", 1));
        assertThat(jdbc.queryForObject("SELECT status FROM inventory_stock_adjustments WHERE id=?", String.class, adjustment.get("id")))
                .isEqualTo("completed");
    }

    // R2：任一方驳回 → 不扣库存，调整单置驳回。
    @Test
    void rejectedSavedBottleStockDisposalKeepsStock() throws Exception {
        seedSavedBottleStock(2);
        mockMvc.perform(post("/api/v1/inventory/stock-movements").with(staff).contentType(MediaType.APPLICATION_JSON)
                .content(outboundBody(savedWarehouse, "店内自用"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"));

        decide(finance, reviewId(), "rejected", "finance").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.review_status").value("rejected"));

        assertThat(balance(savedWarehouse)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT status FROM inventory_stock_adjustments WHERE item_id=? AND location_id=?",
                String.class, item, savedWarehouse)).isEqualTo("rejected");
    }

    // R2（同赔付方案审批）：门店主管与财务两道审批不得由同一人完成。
    @Test
    void supervisorAndFinanceApprovalsMustComeFromDifferentPeople() throws Exception {
        seedSavedBottleStock(2);
        mockMvc.perform(post("/api/v1/inventory/stock-movements").with(staff).contentType(MediaType.APPLICATION_JSON)
                .content(outboundBody(savedWarehouse, "赠送"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"));
        String review = reviewId();

        decide(both, review, "approved", "supervisor").andExpect(status().isOk());
        decide(both, review, "approved", "finance").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("HIGH_VALUE_REVIEW_SAME_APPROVER"));

        assertThat(balance(savedWarehouse)).isEqualTo(2);
    }

    // R2：存酒库房的报废（报损族）低价值也进审批，不再即时扣减；证据沿用既有报损口径（低价值不强制照片+视频）。
    @Test
    void lowValueDamageInSavedBottleWarehouseIsHeldForApproval() throws Exception {
        seedSavedBottleStock(2);
        arrival(1_000L);

        mockMvc.perform(post("/api/v1/inventory/stock-movements/damage").with(staff).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"operator_note\":\"瓶身破裂报废\","
                                + "\"from_location_id\":\"" + savedWarehouse + "\",\"items\":[{\"item_id\":\"" + item + "\",\"quantity\":1}],"
                                + "\"photo_attachment_ids\":[],\"video_attachment_ids\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"));

        assertThat(balance(savedWarehouse)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT subject_type FROM inventory_high_value_reviews WHERE id=?", String.class, reviewId()))
                .isEqualTo("saved_bottle_stock_disposal");
    }

    // R2 边界：普通吧台仓的出库照旧即时扣减。
    @Test
    void ordinaryWarehouseOutboundStaysImmediate() throws Exception {
        jdbc.update("INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,merchant_id,project_id,warehouse_id,updated_at) VALUES(?,?,?,5,0,5,?,?,?,now())",
                "o1302_bal_" + suffix, item, barWarehouse, merchant, project, barWarehouse);

        mockMvc.perform(post("/api/v1/inventory/stock-movements").with(staff).contentType(MediaType.APPLICATION_JSON)
                        .content(outboundBody(barWarehouse, "吧台出库")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("completed"));

        assertThat(balance(barWarehouse)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventory_high_value_reviews WHERE merchant_id=?", Long.class, merchant)).isZero();
    }

    // 审批角色只用于存酒库存处置：持财务码不能替代主管复核码去裁决普通高价值复核。
    @Test
    void financeRoleCannotDecideOrdinaryHighValueReview() throws Exception {
        String adjustment = "o1302_adj_" + suffix;
        String review = "o1302_hvr_" + suffix;
        jdbc.update("INSERT INTO inventory_stock_adjustments(id,item_id,location_id,adjustment_type,quantity,reason,status,created_at,merchant_id) VALUES(?,?,?,'damage',1,'碰落','pending_review',now(),?)",
                adjustment, item, barWarehouse, merchant);
        jdbc.update("INSERT INTO inventory_high_value_reviews(id,subject_type,subject_id,photo_attachment_ids,video_attachment_ids,review_status,created_at,merchant_id) VALUES(?,'damage_report',?,'[\"p\"]'::jsonb,'[\"v\"]'::jsonb,'pending',now(),?)",
                review, adjustment, merchant);

        decide(finance, review, "rejected", "finance").andExpect(status().isForbidden());

        assertThat(jdbc.queryForObject("SELECT review_status FROM inventory_high_value_reviews WHERE id=?", String.class, review))
                .isEqualTo("pending");
    }

    // 读路径按商户范围：有商户范围的门店主管列复核单只看到本商户的（存酒库房处置单与审批人账号不对别的商户可见）。
    @Test
    void highValueReviewListShowsOnlyOwnMerchantReviews() throws Exception {
        String own = "o1302_hvr_own_" + suffix;
        String other = "o1302_hvr_oth_" + suffix;
        for (String[] row : new String[][] {{own, merchant}, {other, "o1302_mch_other_" + suffix}}) {
            jdbc.update("INSERT INTO inventory_high_value_reviews(id,subject_type,subject_id,photo_attachment_ids,video_attachment_ids,review_status,created_at,merchant_id,created_by) "
                    + "VALUES(?,'saved_bottle_stock_disposal',?,'[]'::jsonb,'[]'::jsonb,'pending',now(),?,'o1302_creator')", row[0], "adj_" + row[0], row[1]);
        }

        String body = mockMvc.perform(get("/api/v1/inventory/high-value-reviews").with(supervisor))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(body).contains(own).doesNotContain(other);
    }

    /** 存酒库房里已有到期转入的存货（余额行同 R1 回库写入的形状）。 */
    private void seedSavedBottleStock(int quantity) {
        savedBottleWarehouse(savedWarehouse, merchant);
        jdbc.update("INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,merchant_id,project_id,warehouse_id,updated_at) VALUES(?,?,?,?,0,?,?,?,?,now())",
                "o1302_sbal_" + suffix, item, savedWarehouse, quantity, quantity, merchant, project, savedWarehouse);
    }

    private ResultActions decide(RequestPostProcessor who, String review, String decision, String role) throws Exception {
        return mockMvc.perform(post("/api/v1/inventory/high-value-reviews/" + review + "/review").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"decision\":\"" + decision
                        + "\",\"role\":\"" + role + "\",\"reason\":\"核对无误\"}"));
    }

    /** 后台库存台账「登记出库」的真实请求形状（InventoryLedgerPage.submitMovement，蛇形键）。 */
    private String outboundBody(String from, String note) {
        return "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"operator_note\":\"" + note + "\","
                + "\"evidence_attachment_ids\":[],\"from_location_id\":\"" + from + "\",\"to_location_id\":\"\","
                + "\"items\":[{\"item_id\":\"" + item + "\",\"quantity\":1,\"unit\":\"瓶\"}],\"trace_code\":\"\"}";
    }

    private String reviewId() {
        return jdbc.queryForObject("SELECT id FROM inventory_high_value_reviews WHERE merchant_id=? ORDER BY created_at DESC LIMIT 1",
                String.class, merchant);
    }

    private void savedBottleWarehouse(String id, String owner) {
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,'saved_bottle','存酒库房','active',?)",
                id, project, owner);
    }

    private String storedBottle(String tag, int remaining, Instant expireAt, String custodian) {
        return storedBottle(tag, remaining, expireAt, custodian, null);
    }

    private String storedBottle(String tag, int remaining, Instant expireAt, String custodian, String customer) {
        String id = "o1302_sb_" + tag + "_" + suffix;
        jdbc.update("""
                INSERT INTO saved_bottles (
                    id, customer_id, bar_project_id, item_id, bottle_name, initial_quantity, remaining_quantity, unit, expire_at,
                    status, evidence_attachment_ids, created_at, updated_at, is_high_value, bottle_label_value,
                    customer_name_or_nickname, registration_photo_refs, merchant_id, custodian_entity_id
                )
                VALUES (?, ?, ?, ?, '存酒测试红酒', 5, ?, '瓶', ?, 'stored', '[]'::jsonb, now(), now(), false, '5',
                        '客人甲', '["registration-photo"]'::jsonb, ?, ?)
                """, id, customer, merchant, item, remaining, java.sql.Timestamp.from(expireAt), merchant, custodian);
        return id;
    }

    /** 采购到货流水（高价值判定只看最近一次实际到货价）。 */
    private void arrival(long unitCostCent) {
        inventoryRepository.saveStockMovement(new StockMovementRecord("mv_o1302_" + suffix, "inbound", item, null, barWarehouse, 1,
                unitCostCent, null, "purchase_request", "pr_" + item, null, null, List.of(), "completed",
                Instant.now().minusSeconds(60), merchant), project);
    }

    private List<Map<String, Object>> toStockMovements(String bottle) {
        return jdbc.queryForList("""
                SELECT movement_type, source_location_id, target_location_id, quantity, merchant_id, project_id
                  FROM inventory_stock_movements WHERE business_type='saved_bottle_to_stock' AND business_id=?
                """, bottle);
    }

    private int balance(String location) {
        List<Integer> values = jdbc.queryForList("SELECT available_quantity FROM inventory_stock_balances WHERE item_id=? AND location_id=?",
                Integer.class, item, location);
        return values.isEmpty() ? 0 : values.getFirst();
    }

    private long audits(String event, String bottle) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name=? AND business_id=?", Long.class, event, bottle);
    }

    private RequestPostProcessor account(String tag, String permissions) {
        String userId = "o1302_" + tag + "_" + suffix;
        String role = "o1302_role_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'存酒转库存回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN(" + permissions + ")", role);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchant);
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
