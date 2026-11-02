package com.complex.platform.facility;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.support.InternalPayDayFixtures;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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

/**
 * 第十八批 M 段第 117 条 117-R1、117-R2 真库、真接口回归（请求体一律按前端蛇形键）：BSM:1380「用料必须从指定库房出库并按当前平均进货价计成本，
 * 同时在工单上标明费用承担方：公区设施与楼宇主体设备的用料由物业承担；商户自有设施、以及因商户责任导致损坏的用料由该商户承担，并自动生成一笔应收
 * 进入其费用账单。承担方有争议的，工单挂起转上级判定，判定结果与依据留痕后才能结案。」；BSM:1597 商户金额变化短信写明金额、原因与异议截止日期。
 * 结案应收的短信在业务提交后才发，本类不包事务（角色起中文名）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class FacilityWorkOrderMaterialJdbcIntegrationTests {

    private static final String BASE = "/api/v1/facility/work-orders/";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PiiCipher cipher;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "m117_prj_" + suffix;
    private final String warehouse = "m117_wh_" + suffix;
    private final String item = "m117_item_" + suffix;
    private final String location = "m117_loc_" + suffix;
    private final String merchant = "m117_mch_" + suffix;
    private RequestPostProcessor manager;
    private RequestPostProcessor boss;
    private RequestPostProcessor shop;

    @BeforeEach
    void seed() {
        InternalPayDayFixtures.seed(jdbc);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','工单用料回归项目','active',now(),now())", project);
        jdbc.update("""
                INSERT INTO merchants(id,merchant_name,merchant_type,project_id,contact_name,contact_phone,status,created_at,updated_at)
                VALUES(?,'用料回归商户','retail',?,'主要联系人',?,'active',now(),now())""",
                merchant, project, cipher.encrypt("138" + String.format("%08d", Math.floorMod(suffix.hashCode(), 100_000_000))));
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,'main','工程配件库','active',NULL)",
                warehouse, project);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, created_at, updated_at) VALUES (?, '门锁锁芯', 'material', '个', 'active', now(), now())",
                item);
        jdbc.update("""
                INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,merchant_id,project_id,warehouse_id,updated_at)
                VALUES(?,?,?,2,0,2,NULL,?,?,now())""", "m117_bal_" + suffix, item, location, project, warehouse);
        jdbc.update("""
                INSERT INTO inventory_item_cost_state(item_id,merchant_id,on_hand_quantity,total_cost_cent,avg_unit_cost_cent,has_cost_basis)
                VALUES(?,NULL,2,300,150,TRUE)""", item);
        String role = "m117_role_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'工单用料回归','project','active',now())",
                role, "工单用料回归-" + Long.parseLong(suffix, 16));
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + "'facility.work_order.manage','governance.supervisor_review.view','governance.supervisor_review.approve')", role);
        manager = account("mgr", null, role);
        boss = account("boss", null, role);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("mgr"), user("boss"));
        String shopRole = "m117_shop_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'工单用料回归','project','active',now())",
                shopRole, "工单用料回归商户-" + Long.parseLong(suffix, 16));
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'merchant.billing.view')", shopRole);
        shop = account("shop", merchant, shopRole);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** R1：用料面板列本项目平台库房余额行与本项目经营商户；登记用料即从所选库位出库、按当前平均进货价计成本、挂工单；同键重放不再扣。 */
    @Test
    void issueMaterialOutboundsAtAverageCostAndLinksWorkOrder() throws Exception {
        String wo = order("processing");
        String view = body(mockMvc.perform(get(BASE + wo + "/materials").with(manager)).andExpect(status().isOk()));
        List<Map<String, Object>> stock = JsonPath.read(view, "$.data.options.stock[?(@.location_id == '" + location + "')]");
        assertThat(stock).singleElement().satisfies(row -> {
            assertThat(row.get("warehouse_id")).isEqualTo(warehouse);
            assertThat(row.get("item_id")).isEqualTo(item);
            assertThat(((Number) row.get("available_quantity")).intValue()).isEqualTo(2);
        });
        List<String> merchants = JsonPath.read(view, "$.data.options.merchants[*].merchant_id");
        assertThat(merchants).contains(merchant);

        String issue = "{\"location_id\":\"" + location + "\",\"item_id\":\"" + item + "\",\"quantity\":1,\"idempotency_key\":\"k1_" + suffix + "\"}";
        send(manager, wo, "materials", issue).andExpect(status().isOk());
        send(manager, wo, "materials", issue).andExpect(status().isOk());

        assertThat(jdbc.queryForList("""
                SELECT movement_type, source_location_id, quantity, total_cost_cent FROM inventory_stock_movements
                 WHERE business_type='facility_work_order' AND business_id=?""", wo)).singleElement().satisfies(row -> {
            assertThat(row.get("movement_type")).isEqualTo("outbound");
            assertThat(row.get("source_location_id")).isEqualTo(location);
            assertThat(((Number) row.get("total_cost_cent")).longValue()).isEqualTo(150L);
        });
        assertThat(available()).isEqualTo(1);
        mockMvc.perform(get(BASE + wo + "/materials").with(manager)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lines[0].warehouse_id").value(warehouse))
                .andExpect(jsonPath("$.data.lines[0].item_name").value("门锁锁芯"))
                .andExpect(jsonPath("$.data.lines[0].unit_cost_cent").value(150))
                .andExpect(jsonPath("$.data.total_cost_cent").value(150));
    }

    /** R1：库存不足 409 不扣；只有处理中的工单才能登记用料。 */
    @Test
    void issueRejectsShortStockAndOrderNotProcessing() throws Exception {
        String wo = order("processing");
        send(manager, wo, "materials", "{\"location_id\":\"" + location + "\",\"item_id\":\"" + item + "\",\"quantity\":5,\"idempotency_key\":\"k5_" + suffix + "\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("STOCK_INSUFFICIENT"));
        assertThat(available()).isEqualTo(2);

        String assigned = order("assigned");
        send(manager, assigned, "materials", "{\"location_id\":\"" + location + "\",\"item_id\":\"" + item + "\",\"quantity\":1,\"idempotency_key\":\"ka_" + suffix + "\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("INVALID_STATE"));
        assertThat(available()).isEqualTo(2);
    }

    /**
     * R1：用了库房物料须先标明费用承担方才能完工；商户承担的结案生成恰一笔商户应收（工单用料费），回写承担方，短信带异议截止日，
     * 商户综合账单「工单用料费（商户承担）」列出该笔。
     */
    @Test
    void merchantBearerPostsOneReceivableOnCloseWithSmsAndStatement() throws Exception {
        String wo = order("processing");
        issue(wo, "m1");
        complete(wo).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("WORK_ORDER_MATERIAL_BEARER_REQUIRED"));
        assertThat(orderStatus(wo)).isEqualTo("processing");

        bearer(wo, "{\"cost_bearer\":\"merchant\",\"merchant_id\":\"" + merchant + "\",\"idempotency_key\":\"b1_" + suffix + "\"}")
                .andExpect(status().isOk());
        complete(wo).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));

        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, direction, counterparty_id, amount_cent, project_id, to_char(due_date,'YYYY-MM') AS month FROM ar_ap_items
                 WHERE source_type='work_order_material' AND source_id=?""", wo);
        assertThat(rows).hasSize(1);
        Map<String, Object> receivable = rows.get(0);
        assertThat(receivable.get("direction")).isEqualTo("receivable");
        assertThat(receivable.get("counterparty_id")).isEqualTo(merchant);
        assertThat(((Number) receivable.get("amount_cent")).longValue()).isEqualTo(150L);
        assertThat(receivable.get("project_id")).isEqualTo(project);
        assertThat(jdbc.queryForObject("SELECT ar_ap_item_id FROM work_order_cost_bearers WHERE business_type='facility_work_order' AND work_order_id=?",
                String.class, wo)).isEqualTo(receivable.get("id"));
        assertThat(jdbc.queryForObject("SELECT params->>'objection_deadline' FROM notification_send_log WHERE request_id=?",
                String.class, "mamt:work_order_material:" + receivable.get("id"))).isNotBlank();

        mockMvc.perform(get("/api/v1/merchant-portal/fee-statement").param("month", (String) receivable.get("month")).with(shop))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fee_items[?(@.fee_name == '工单用料费（商户承担）')].amount_cent").value(150));
    }

    /** R1：物业承担的结案不生成任何台账行；商户承担不带商户 400；没登记用料的工单不标承担方照常完工结案。 */
    @Test
    void propertyBearerAndNoMaterialsCloseWithoutReceivable() throws Exception {
        String wo = order("processing");
        issue(wo, "p1");
        bearer(wo, "{\"cost_bearer\":\"merchant\",\"idempotency_key\":\"b0_" + suffix + "\"}")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        bearer(wo, "{\"cost_bearer\":\"property\",\"idempotency_key\":\"b2_" + suffix + "\"}").andExpect(status().isOk());
        complete(wo).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
        assertThat(receivables(wo)).isZero();

        String plain = order("processing");
        complete(plain).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
        assertThat(receivables(plain)).isZero();
    }

    /**
     * R2：承担方有争议的，工单挂起转提出人的直接上级判定；判定前不能恢复、不能改承担方、不能再提；上级按主张批准（意见即判定依据）后
     * 承担方改定、工单仍挂起，由物业人员恢复后照常完工，物业承担不生成应收。
     */
    @Test
    void disputeSuspendsUntilSupervisorRulesAndApprovalSwitchesBearer() throws Exception {
        String wo = merchantBorneOrder();
        dispute(wo, "d1").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("suspended"));
        Map<String, Object> pending = disputeRow(wo);
        assertThat(pending.get("status")).isEqualTo("pending");
        String review = (String) pending.get("review_id");
        assertThat(review).isNotBlank();
        assertThat(jdbc.queryForList("SELECT approver_id FROM supervisor_review_nodes WHERE review_id=? AND line_code='main'", String.class, review))
                .containsExactly(user("boss"));

        send(manager, wo, "resume", "{\"idempotency_key\":\"r0_" + suffix + "\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("WORK_ORDER_BEARER_DISPUTE_PENDING"));
        bearer(wo, "{\"cost_bearer\":\"property\",\"idempotency_key\":\"b3_" + suffix + "\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("WORK_ORDER_BEARER_DISPUTE_PENDING"));
        dispute(wo, "d2").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("WORK_ORDER_BEARER_DISPUTE_PENDING"));

        decide(review, "approved", "经现场核实属楼宇主体设备").andExpect(status().isOk());
        Map<String, Object> ruled = disputeRow(wo);
        assertThat(ruled.get("status")).isEqualTo("approved");
        assertThat(ruled.get("ruled_by")).isEqualTo(user("boss"));
        assertThat(ruled.get("ruling_basis")).isEqualTo("经现场核实属楼宇主体设备");
        assertThat(ruled.get("ruled_at")).isNotNull();
        assertThat(jdbc.queryForMap("SELECT cost_bearer, merchant_id FROM work_order_cost_bearers WHERE business_type='facility_work_order' AND work_order_id=?", wo))
                .containsEntry("cost_bearer", "property").containsEntry("merchant_id", null);
        assertThat(orderStatus(wo)).isEqualTo("suspended");

        send(manager, wo, "resume", "{\"idempotency_key\":\"r1_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("processing"));
        complete(wo).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
        assertThat(receivables(wo)).isZero();
    }

    /** R2：上级驳回（维持原承担方，意见留痕），恢复、结案后照常生成一笔商户应收。 */
    @Test
    void rejectedDisputeKeepsMerchantBearerAndPostsReceivable() throws Exception {
        String wo = merchantBorneOrder();
        dispute(wo, "d3").andExpect(status().isOk());
        String review = (String) disputeRow(wo).get("review_id");

        decide(review, "rejected", "门锁属商户自有设施").andExpect(status().isOk());
        Map<String, Object> ruled = disputeRow(wo);
        assertThat(ruled.get("status")).isEqualTo("rejected");
        assertThat(ruled.get("ruling_basis")).isEqualTo("门锁属商户自有设施");
        assertThat(jdbc.queryForObject("SELECT cost_bearer FROM work_order_cost_bearers WHERE business_type='facility_work_order' AND work_order_id=?",
                String.class, wo)).isEqualTo("merchant");

        send(manager, wo, "resume", "{\"idempotency_key\":\"r2_" + suffix + "\"}").andExpect(status().isOk());
        complete(wo).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
        assertThat(receivables(wo)).isEqualTo(1);
    }

    /** R2：上级判定须填意见（判定依据），不填 400，争议仍待判定、工单仍挂起；未标承担方就提争议 409。 */
    @Test
    void rulingNeedsCommentAndDisputeNeedsMarkedBearer() throws Exception {
        String wo = merchantBorneOrder();
        dispute(wo, "d4").andExpect(status().isOk());
        String review = (String) disputeRow(wo).get("review_id");
        decide(review, "approved", null).andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(disputeRow(wo).get("status")).isEqualTo("pending");
        assertThat(orderStatus(wo)).isEqualTo("suspended");

        String unmarked = order("processing");
        issue(unmarked, "u1");
        dispute(unmarked, "d5").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("WORK_ORDER_MATERIAL_BEARER_REQUIRED"));
        assertThat(orderStatus(unmarked)).isEqualTo("processing");
    }

    private String merchantBorneOrder() throws Exception {
        String wo = order("processing");
        issue(wo, "i_" + wo);
        bearer(wo, "{\"cost_bearer\":\"merchant\",\"merchant_id\":\"" + merchant + "\",\"idempotency_key\":\"bm_" + wo + "\"}")
                .andExpect(status().isOk());
        return wo;
    }

    private void issue(String wo, String key) throws Exception {
        send(manager, wo, "materials", "{\"location_id\":\"" + location + "\",\"item_id\":\"" + item + "\",\"quantity\":1,\"idempotency_key\":\""
                + key + "_" + suffix + "\"}").andExpect(status().isOk());
    }

    private ResultActions bearer(String wo, String body) throws Exception {
        return send(manager, wo, "cost-bearer", body);
    }

    private ResultActions dispute(String wo, String key) throws Exception {
        return send(manager, wo, "bearer-dispute", "{\"claimed_cost_bearer\":\"property\",\"reason\":\"属楼宇主体设备\",\"idempotency_key\":\""
                + key + "_" + suffix + "\"}");
    }

    private ResultActions complete(String wo) throws Exception {
        return send(manager, wo, "complete", "{\"resolution\":\"已更换锁芯\",\"idempotency_key\":\"c_" + UUID.randomUUID() + "\"}");
    }

    private ResultActions decide(String review, String decision, String comment) throws Exception {
        String note = comment == null ? "" : ",\"comment\":\"" + comment + "\",\"operator_note\":\"" + comment + "\"";
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(boss)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"" + decision + "\"" + note + ",\"idempotency_key\":\"a_" + UUID.randomUUID() + "\"}"));
    }

    private ResultActions send(RequestPostProcessor who, String wo, String action, String body) throws Exception {
        return mockMvc.perform(post(BASE + wo + "/" + action).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String order(String status) {
        String id = "m117_wo_" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO facility_work_orders (id, code, work_order_type, priority, status, title, source, reporter_id, reporter_type,
                    assignee_id, response_due_at, handle_due_at, assigned_at, responded_at, project_id, merchant_id)
                VALUES (?, ?, 'other', 'normal', ?, '更换商户门锁', 'platform', ?, 'staff', ?, now() + interval '4 hours',
                    now() + interval '2 days', now(), now(), ?, ?)""",
                id, "FWO-" + id.substring(id.length() - 8).toUpperCase(), status, user("mgr"), user("mgr"), project, merchant);
        return id;
    }

    private Map<String, Object> disputeRow(String wo) {
        return jdbc.queryForMap("SELECT status, review_id, ruled_by, ruled_at, ruling_basis FROM work_order_bearer_disputes WHERE business_type='facility_work_order' AND work_order_id=?", wo);
    }

    private int available() {
        return jdbc.queryForObject("SELECT available_quantity FROM inventory_stock_balances WHERE item_id=? AND location_id=?", Integer.class, item, location);
    }

    private int receivables(String wo) {
        return jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type='work_order_material' AND source_id=?", Integer.class, wo);
    }

    private String orderStatus(String wo) {
        return jdbc.queryForObject("SELECT status FROM facility_work_orders WHERE id=?", String.class, wo);
    }

    private String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private RequestPostProcessor account(String tag, String merchantId, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        if (merchantId == null) {
            jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        }
        AccessActor actor = new AccessActor(userId, userId, "staff", project, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String user(String tag) {
        return "m117_" + tag + "_" + suffix;
    }
}
