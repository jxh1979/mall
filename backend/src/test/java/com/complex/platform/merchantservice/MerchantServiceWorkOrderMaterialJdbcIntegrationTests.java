package com.complex.platform.merchantservice;

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
 * 第十八批 M 段第 117 条 117-R5 真库、真接口回归（请求体一律按前端蛇形键）：BSM:1372「商户报修、客人投诉、设备故障、巡检发现的问题都进统一工单
 * （商户自己在商户专区报的单，按枚举速查表「商户服务工单状态」那条的状态走）」；BSM:1380「用料必须从指定库房出库并按当前平均进货价计成本，同时在工单上
 * 标明费用承担方……商户自有设施、以及因商户责任导致损坏的用料由该商户承担，并自动生成一笔应收进入其费用账单。承担方有争议的，工单挂起转上级判定，
 * 判定结果与依据留痕后才能结案。」；第十八批裁决 29「不加状态，停在原状态」：有争议时单子停在原状态转上级判定，判定前不能转「已处理待关闭」、
 * 不能关闭；判定结果与依据留痕后照常往下走。结案应收的短信在业务提交后才发，本类不包事务（角色起中文名）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class MerchantServiceWorkOrderMaterialJdbcIntegrationTests {

    private static final String BASE = "/api/v1/merchant-service/work-orders/";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PiiCipher cipher;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "m117m_prj_" + suffix;
    private final String warehouse = "m117m_wh_" + suffix;
    private final String item = "m117m_item_" + suffix;
    private final String location = "m117m_loc_" + suffix;
    private final String merchant = "m117m_mch_" + suffix;
    private RequestPostProcessor staff;
    private RequestPostProcessor boss;
    private RequestPostProcessor shop;

    @BeforeEach
    void seed() {
        InternalPayDayFixtures.seed(jdbc);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','商户工单用料回归项目','active',now(),now())", project);
        jdbc.update("""
                INSERT INTO merchants(id,merchant_name,merchant_type,project_id,contact_name,contact_phone,status,created_at,updated_at)
                VALUES(?,'商户工单用料回归商户','retail',?,'主要联系人',?,'active',now(),now())""",
                merchant, project, cipher.encrypt("139" + String.format("%08d", Math.floorMod(suffix.hashCode(), 100_000_000))));
        jdbc.update("INSERT INTO warehouses(id,project_id,warehouse_type,name,status,merchant_id) VALUES(?,?,'main','工程配件库','active',NULL)",
                warehouse, project);
        jdbc.update("INSERT INTO inventory_items (id, item_name, item_type, unit, status, created_at, updated_at) VALUES (?, '水龙头阀芯', 'material', '个', 'active', now(), now())",
                item);
        jdbc.update("""
                INSERT INTO inventory_stock_balances(id,item_id,location_id,quantity,reserved_quantity,available_quantity,merchant_id,project_id,warehouse_id,updated_at)
                VALUES(?,?,?,4,0,4,NULL,?,?,now())""", "m117m_bal_" + suffix, item, location, project, warehouse);
        jdbc.update("""
                INSERT INTO inventory_item_cost_state(item_id,merchant_id,on_hand_quantity,total_cost_cent,avg_unit_cost_cent,has_cost_basis)
                VALUES(?,NULL,4,600,150,TRUE)""", item);
        String staffRole = role("staff", "商户工单用料回归员工", "'merchant.service_work_order.view','merchant.service_work_order.assign',"
                + "'merchant.service_work_order.process','merchant.service_work_order.close','governance.supervisor_review.view',"
                + "'governance.supervisor_review.approve'");
        staff = account("pa", null, staffRole);
        boss = account("boss", null, staffRole);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("pa"), user("boss"));
        // 商户账号另持处理码：验证「商户账号不登记库房出库」由平台口径闸拦，而不是缺码。
        shop = account("shop", merchant, role("shop", "商户工单用料回归商户", "'merchant.service_work_order.view','merchant.service_work_order.create',"
                + "'merchant.service_work_order.process','merchant.billing.view'"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** 登记用料即从所选库位出库、按当前平均进货价计成本、挂商户服务工单；商户账号不能登记（平台口径闸）。 */
    @Test
    void staffIssuesMaterialOnMerchantWorkOrderAtAverageCost() throws Exception {
        String wo = processingOrder("repair");
        String view = body(mockMvc.perform(get(BASE + wo + "/materials").with(staff)).andExpect(status().isOk()));
        List<String> locations = JsonPath.read(view, "$.data.options.stock[*].location_id");
        assertThat(locations).contains(location);

        issue(wo, "i1").andExpect(status().isOk()).andExpect(jsonPath("$.data.total_cost_cent").value(150));
        assertThat(jdbc.queryForList("""
                SELECT movement_type, quantity, total_cost_cent FROM inventory_stock_movements
                 WHERE business_type='merchant_service_work_order' AND business_id=?""", wo)).singleElement().satisfies(row -> {
            assertThat(row.get("movement_type")).isEqualTo("outbound");
            assertThat(((Number) row.get("total_cost_cent")).longValue()).isEqualTo(150L);
        });

        send(shop, wo, "materials", "{\"location_id\":\"" + location + "\",\"item_id\":\"" + item + "\",\"quantity\":1,\"idempotency_key\":\"s1_"
                + suffix + "\"}").andExpect(status().isForbidden());
        assertThat(available()).isEqualTo(3);
    }

    /** 用了库房物料须先标明承担方才能转已处理待关闭；商户承担（就是提单商户）关闭时生成恰一笔应收，进商户综合账单。 */
    @Test
    void merchantBearerPostsOneReceivableOnClose() throws Exception {
        String wo = processingOrder("repair");
        issue(wo, "i2").andExpect(status().isOk());
        act(wo, "resolved").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("WORK_ORDER_MATERIAL_BEARER_REQUIRED"));
        assertThat(orderStatus(wo)).isEqualTo("processing");

        bearer(wo, "merchant", "b2").andExpect(status().isOk()).andExpect(jsonPath("$.data.bearer.merchant_id").value(merchant));
        act(wo, "resolved").andExpect(status().isOk());
        close(wo).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));

        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, counterparty_id, amount_cent, project_id, to_char(due_date,'YYYY-MM') AS month FROM ar_ap_items
                 WHERE source_type='work_order_material' AND source_id=?""", wo);
        assertThat(rows).hasSize(1);
        Map<String, Object> receivable = rows.get(0);
        assertThat(receivable.get("counterparty_id")).isEqualTo(merchant);
        assertThat(((Number) receivable.get("amount_cent")).longValue()).isEqualTo(150L);
        assertThat(receivable.get("project_id")).isEqualTo(project);
        mockMvc.perform(get("/api/v1/merchant-portal/fee-statement").param("month", (String) receivable.get("month")).with(shop))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fee_items[?(@.fee_name == '工单用料费（商户承担）')].amount_cent").value(150));
    }

    /** 物业承担的单关闭不生成应收。 */
    @Test
    void propertyBearerClosesWithoutReceivable() throws Exception {
        String wo = processingOrder("repair");
        issue(wo, "i3").andExpect(status().isOk());
        bearer(wo, "property", "b3").andExpect(status().isOk());
        act(wo, "resolved").andExpect(status().isOk());
        close(wo).andExpect(status().isOk());
        assertThat(receivables(wo)).isZero();
    }

    /**
     * 裁决 29：承担方有争议的，单子停在原状态转提出人的直接上级判定；判定前不能转已处理待关闭、不能改承担方、不能再提；上级按主张批准（意见即
     * 判定依据）后承担方改定，照常往下走，物业承担不生成应收。
     */
    @Test
    void disputeKeepsStatusBlocksResolveUntilSupervisorRules() throws Exception {
        String wo = processingOrder("repair");
        issue(wo, "i4").andExpect(status().isOk());
        bearer(wo, "merchant", "b4").andExpect(status().isOk());
        dispute(wo, "d4").andExpect(status().isOk()).andExpect(jsonPath("$.data.disputes[0].status").value("pending"));
        assertThat(orderStatus(wo)).isEqualTo("processing");
        Map<String, Object> pending = disputeRow(wo);
        String review = (String) pending.get("review_id");
        assertThat(jdbc.queryForList("SELECT approver_id FROM supervisor_review_nodes WHERE review_id=? AND line_code='main'", String.class, review))
                .containsExactly(user("boss"));

        act(wo, "resolved").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("WORK_ORDER_BEARER_DISPUTE_PENDING"));
        assertThat(orderStatus(wo)).isEqualTo("processing");
        bearer(wo, "property", "b5").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("WORK_ORDER_BEARER_DISPUTE_PENDING"));
        dispute(wo, "d5").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("WORK_ORDER_BEARER_DISPUTE_PENDING"));

        decide(review, "approved", "阀芯属楼宇主体给水管道").andExpect(status().isOk());
        Map<String, Object> ruled = disputeRow(wo);
        assertThat(ruled.get("status")).isEqualTo("approved");
        assertThat(ruled.get("ruled_by")).isEqualTo(user("boss"));
        assertThat(ruled.get("ruling_basis")).isEqualTo("阀芯属楼宇主体给水管道");
        assertThat(jdbc.queryForMap("SELECT cost_bearer, merchant_id FROM work_order_cost_bearers WHERE business_type='merchant_service_work_order' AND work_order_id=?", wo))
                .containsEntry("cost_bearer", "property").containsEntry("merchant_id", null);

        act(wo, "resolved").andExpect(status().isOk());
        close(wo).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
        assertThat(receivables(wo)).isZero();
    }

    /** 裁决 29：已处理待关闭时提出的争议，判定前不能关闭；上级驳回（维持商户承担，意见留痕）后照常关闭并生成一笔应收。 */
    @Test
    void disputeOnResolvedBlocksCloseAndRejectionKeepsMerchantBearer() throws Exception {
        String wo = processingOrder("repair");
        issue(wo, "i6").andExpect(status().isOk());
        bearer(wo, "merchant", "b6").andExpect(status().isOk());
        act(wo, "resolved").andExpect(status().isOk());
        dispute(wo, "d6").andExpect(status().isOk());
        close(wo).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("WORK_ORDER_BEARER_DISPUTE_PENDING"));
        assertThat(orderStatus(wo)).isEqualTo("resolved");

        decide((String) disputeRow(wo).get("review_id"), "rejected", "阀芯属商户自有设施").andExpect(status().isOk());
        assertThat(disputeRow(wo)).containsEntry("status", "rejected").containsEntry("ruling_basis", "阀芯属商户自有设施");
        close(wo).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));
        assertThat(receivables(wo)).isEqualTo(1);
    }

    /** 商户提单、员工派给自己、开始处理（后台蛇形键）。 */
    private String processingOrder(String type) throws Exception {
        String body = """
                {"idempotency_key":"%s","work_order_type":"%s","priority":"high","title":"水龙头漏水%s","description":"店内洗手池水龙头漏水",
                 "reported_channel":"merchant_portal"}""".formatted(UUID.randomUUID(), type, suffix);
        String wo = JsonPath.read(body(mockMvc.perform(post("/api/v1/merchant-service/work-orders").with(shop)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())), "$.data.id");
        send(staff, wo, "assign", """
                {"idempotency_key":"%s","operator_note":"派单","owner_department":"property","owner_user_id":"%s"}"""
                .formatted(UUID.randomUUID(), user("pa"))).andExpect(status().isOk());
        act(wo, "processing").andExpect(status().isOk());
        return wo;
    }

    private ResultActions issue(String wo, String key) throws Exception {
        return send(staff, wo, "materials", "{\"location_id\":\"" + location + "\",\"item_id\":\"" + item + "\",\"quantity\":1,\"idempotency_key\":\""
                + key + "_" + suffix + "\"}");
    }

    private ResultActions bearer(String wo, String bearer, String key) throws Exception {
        return send(staff, wo, "cost-bearer", "{\"cost_bearer\":\"" + bearer + "\",\"idempotency_key\":\"" + key + "_" + suffix + "\"}");
    }

    private ResultActions dispute(String wo, String key) throws Exception {
        return send(staff, wo, "bearer-dispute", "{\"claimed_cost_bearer\":\"property\",\"reason\":\"属楼宇主体给水管道\",\"idempotency_key\":\""
                + key + "_" + suffix + "\"}");
    }

    private ResultActions act(String wo, String target) throws Exception {
        return send(staff, wo, "actions", "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"operator_note\":\"已处理\",\"target_status\":\"" + target + "\"}");
    }

    private ResultActions close(String wo) throws Exception {
        return send(staff, wo, "close", "{\"idempotency_key\":\"" + UUID.randomUUID() + "\",\"resolution\":\"已更换阀芯\",\"satisfaction_score\":null}");
    }

    private ResultActions decide(String review, String decision, String comment) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + review + "/approve").with(boss)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"" + decision + "\",\"comment\":\"" + comment + "\",\"operator_note\":\"" + comment
                        + "\",\"idempotency_key\":\"a_" + UUID.randomUUID() + "\"}"));
    }

    private ResultActions send(RequestPostProcessor who, String wo, String action, String body) throws Exception {
        return mockMvc.perform(post(BASE + wo + "/" + action).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Map<String, Object> disputeRow(String wo) {
        return jdbc.queryForMap("""
                SELECT status, review_id, ruled_by, ruling_basis FROM work_order_bearer_disputes
                 WHERE business_type='merchant_service_work_order' AND work_order_id=?""", wo);
    }

    private int available() {
        return jdbc.queryForObject("SELECT available_quantity FROM inventory_stock_balances WHERE item_id=? AND location_id=?", Integer.class, item, location);
    }

    private int receivables(String wo) {
        return jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE source_type='work_order_material' AND source_id=?", Integer.class, wo);
    }

    private String orderStatus(String wo) {
        return jdbc.queryForObject("SELECT status FROM merchant_service_work_orders WHERE id=?", String.class, wo);
    }

    private String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String role(String tag, String name, String permissionCodes) {
        String role = "m117m_" + tag + "_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'商户工单用料回归','project','active',now())",
                role, name + "-" + Long.parseLong(suffix, 16));
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN("
                + permissionCodes + ")", role);
        return role;
    }

    private RequestPostProcessor account(String tag, String merchantId, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,?,'active',now())",
                userId, userId, project, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, registration_status, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, 'approved', 'active', now(), now())", "m117m_stf_" + tag + "_" + suffix, project, merchantId, userId, tag);
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
        return "m117m_" + tag + "_" + suffix;
    }
}
