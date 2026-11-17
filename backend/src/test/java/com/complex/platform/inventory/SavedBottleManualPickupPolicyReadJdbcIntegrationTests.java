package com.complex.platform.inventory;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AP 段第 254 条 254-R5、254-R7（真库、真实 HTTP、蛇形键、真实授权链；存酒行、申请行、策略行 JDBC 直接插入，随测试事务回滚）。
 * 254-R5：BSM:1073「……须同时满足四项：①由店长发起并经另一名主管复核，普通员工不得单独放酒；……③由该客户对应的营销人员确认身份与取酒申请；
 * ④只允许存酒客户本人到场办理，不允许他人代领。……」——存酒页按瓶列出人工例外取酒申请、按状态办理；四步是既有行为，本类只测新增的按瓶列表。
 * 254-R7：BSM:1073「……②申请手机号在本项目内满足后台配置的任一消费历史条件（安全阈值，按项目可配，调整须最高权限审批并留痕……须同时配置上下限）……」；
 * BSM:1072「……客户存酒有效期（业务参数，按项目可配……）……转存次数不得超过后台配置的上限（业务参数，按项目可配……）……顺延该酒吧当前配置的期限
 * （业务参数，按酒吧可配……）……」；PGM:528「……④看不到别的商户和别的项目的数据……」——两项策略可读：商户账号不可读，非平台管理员只看出厂行与可触及项目的行。
 * 修复前两个列表接口不存在（同路径只有 POST、PUT），一律不是 200。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SavedBottleManualPickupPolicyReadJdbcIntegrationTests {

    private static final String BASE = "/api/v1/inventory/saved-bottles";
    private static final String PICKUP_POLICY = "bar.saved_bottle.manual_pickup_policy.manage";
    private static final String RENEWAL_POLICY = "bar.saved_bottle.renewal_policy.manage";
    private static final String REMINDER_POLICY = "bar.saved_bottle.reminder_policy.manage";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PiiCipher cipher;

    private final String s = String.valueOf(System.nanoTime());
    private final String projectA = "prj_b62mp_a_" + s;
    private final String projectB = "prj_b62mp_b_" + s;
    private final String barA = "bar_b62mp_a_" + s;
    private final String barB = "bar_b62mp_b_" + s;
    private final String item = "item_b62mp_" + s;
    private StaffRequestFixture fixture;

    @BeforeEach
    void seed() {
        String entity = "ent_b62mp_" + s;
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'人工例外主体','project_company','active',now(),now())", entity);
        for (String project : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','人工例外综合体','active',now(),now())", project);
            jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, project);
        }
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'人工例外酒吧甲','bar',?,?,'active',now(),now())", barA, entity, projectA);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at)"
                + " VALUES(?,'人工例外酒吧乙','bar',?,?,'active',now(),now())", barB, entity, projectB);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at)"
                + " VALUES(?,'人工例外威士忌','liquor','瓶',0,'active',?,now(),now())", item, barA);
        fixture = new StaffRequestFixture(jdbc);
    }

    // 254-R5：本店店员按瓶读申请：两条按创建时间倒序，首条是刚发起的；响应不带手机号哈希与商户编号。
    @Test
    void manualPickupsOfOneBottleNewestFirstWithoutPhoneHash() throws Exception {
        String bottle = insertBottle();
        String manager = "usr_b62mp_mgr_" + s;
        insertPickup("mpk_b62mp_old_" + s, bottle, "orders_total", "dispensed", manager, Instant.now().minusSeconds(86400));
        insertPickup("mpk_b62mp_new_" + s, bottle, "orders_within_1m", "initiated", manager, Instant.now());
        RequestPostProcessor clerk = fixture.staff("usr_b62mp_clerk_" + s, "staff", projectA, barA, "bar.saved_bottle.manage");

        mockMvc.perform(get(BASE + "/" + bottle + "/manual-pickups").with(clerk))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value("mpk_b62mp_new_" + s))
                .andExpect(jsonPath("$.data.items[0].status").value("initiated"))
                .andExpect(jsonPath("$.data.items[0].history_condition").value("orders_within_1m"))
                .andExpect(jsonPath("$.data.items[0].initiated_by").value(manager))
                .andExpect(jsonPath("$.data.items[0].reason").value("客人手机遗失"))
                .andExpect(jsonPath("$.data.items[0].phone_hash").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].merchant_id").doesNotExist())
                .andExpect(jsonPath("$.data.items[1].status").value("dispensed"));
    }

    // 254-R5：别家商户店员读不到本店存酒的申请；不持存酒管理码的员工拒绝。
    @Test
    void manualPickupsRejectOtherMerchantAndMissingPermission() throws Exception {
        String bottle = insertBottle();
        insertPickup("mpk_b62mp_x_" + s, bottle, "orders_within_3m", "reviewed", "usr_b62mp_mgr_" + s, Instant.now());
        RequestPostProcessor otherClerk = fixture.staff("usr_b62mp_other_" + s, "staff", projectB, barB, "bar.saved_bottle.manage");
        RequestPostProcessor noCode = fixture.staff("usr_b62mp_nocode_" + s, "staff", projectA, barA, "inventory.stock.view");

        mockMvc.perform(get(BASE + "/" + bottle + "/manual-pickups").with(otherClerk))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
        mockMvc.perform(get(BASE + "/" + bottle + "/manual-pickups").with(noCode))
                .andExpect(status().isForbidden());
    }

    // 254-R7：平台管理员读出两项策略的出厂行（BSM:1073 出厂 2/3/5 笔；BSM:1072 转存 2 次、顺延 30 天、有效期 30 天）。
    @Test
    void platformAdminReadsFactoryRowsOfBothPolicies() throws Exception {
        RequestPostProcessor platform = fixture.staff("usr_b62mp_plat_" + s, "platform_admin", "org_platform", null,
                PICKUP_POLICY, RENEWAL_POLICY);

        String pickups = body(get(BASE + "/manual-pickup-policy"), platform);
        assertThat(ints(pickups, "$.data.items[?(@.project_id=='global')].orders_within1m")).containsExactly(2);
        assertThat(ints(pickups, "$.data.items[?(@.project_id=='global')].orders_within3m")).containsExactly(3);
        assertThat(ints(pickups, "$.data.items[?(@.project_id=='global')].orders_total")).containsExactly(5);

        String renewals = body(get(BASE + "/renewal-policy"), platform);
        assertThat(ints(renewals, "$.data.items[?(@.policy_key=='max_renewals' && @.scope_id=='global')].numeric_value")).containsExactly(2);
        assertThat(ints(renewals, "$.data.items[?(@.policy_key=='extension_days' && @.scope_id=='global')].numeric_value")).containsExactly(30);
        assertThat(ints(renewals, "$.data.items[?(@.policy_key=='validity_days' && @.scope_id=='global')].numeric_value")).containsExactly(30);
    }

    // 254-R7：最高权限本人改消费历史条件后列表即见新值；转存参数每保存一次版本加 1；非最高权限改消费历史条件 403。
    @Test
    void savedValuesShowUpInTheListsAndOnlyMasterChangesPickupPolicy() throws Exception {
        RequestPostProcessor platform = fixture.staff("usr_b62mp_plat2_" + s, "platform_admin", "org_platform", null,
                PICKUP_POLICY, RENEWAL_POLICY);
        send(put(BASE + "/manual-pickup-policy"), master(projectA), "{\"idempotency_key\":\"b62mp-pp-" + s + "\",\"project_id\":\"" + projectA
                + "\",\"orders_within1m\":3,\"orders_within3m\":4,\"orders_total\":6,\"min_orders\":1,\"max_orders\":10}")
                .andExpect(status().isOk());
        String pickups = body(get(BASE + "/manual-pickup-policy"), platform);
        String row = "$.data.items[?(@.project_id=='" + projectA + "')]";
        assertThat(ints(pickups, row + ".orders_within1m")).containsExactly(3);
        assertThat(ints(pickups, row + ".orders_total")).containsExactly(6);
        assertThat(ints(pickups, row + ".max_orders")).containsExactly(10);

        String renewalBody = "{\"idempotency_key\":\"%s\",\"policy_key\":\"max_renewals\",\"scope_id\":\"project:" + projectA
                + "\",\"value\":%d,\"min_value\":0,\"max_value\":5}";
        send(put(BASE + "/renewal-policy"), platform, renewalBody.formatted("b62mp-rp1-" + s, 3)).andExpect(status().isOk());
        send(put(BASE + "/renewal-policy"), platform, renewalBody.formatted("b62mp-rp2-" + s, 4)).andExpect(status().isOk());
        String renewals = body(get(BASE + "/renewal-policy"), platform);
        String renewalRow = "$.data.items[?(@.policy_key=='max_renewals' && @.scope_id=='project:" + projectA + "')]";
        assertThat(ints(renewals, renewalRow + ".numeric_value")).containsExactly(4);
        assertThat(ints(renewals, renewalRow + ".version")).containsExactly(2);

        // 失败的写请求放最后（测试事务里会把外层事务标记为只回滚）。
        send(put(BASE + "/manual-pickup-policy"), platform, "{\"idempotency_key\":\"b62mp-pp-np-" + s + "\",\"project_id\":\"" + projectA
                + "\",\"orders_within1m\":2,\"orders_within3m\":3,\"orders_total\":5,\"min_orders\":1,\"max_orders\":10}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("MASTER_APPROVAL_REQUIRED"));
    }

    // 254-R7：商户账号不可读两项策略（项目级参数，PGM:528 ④）。
    @Test
    void merchantAccountCannotReadPolicies() throws Exception {
        RequestPostProcessor merchantStaff = fixture.staff("usr_b62mp_mch_" + s, "staff", projectA, barA, PICKUP_POLICY, RENEWAL_POLICY);
        mockMvc.perform(get(BASE + "/manual-pickup-policy").with(merchantStaff))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"));
        mockMvc.perform(get(BASE + "/renewal-policy").with(merchantStaff))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PLATFORM_SCOPE_REQUIRED"));
    }

    // 254-R7：只到项目甲的项目员工：看得到出厂行、甲的项目行与甲项目酒吧的顺延期限，看不到乙的任何行（PGM:528 ④）。
    @Test
    void projectStaffSeesOnlyReachableProjectRows() throws Exception {
        for (String project : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO saved_bottle_manual_pickup_policy(project_id,orders_within_1m,orders_within_3m,orders_total,min_orders,max_orders)"
                    + " VALUES(?,2,3,5,1,10)", project);
            jdbc.update("INSERT INTO saved_bottle_renewal_policy(policy_key,scope_id,numeric_value,min_value,max_value) VALUES('validity_days',?,45,7,90)",
                    "project:" + project);
        }
        for (String bar : List.of(barA, barB)) {
            jdbc.update("INSERT INTO saved_bottle_renewal_policy(policy_key,scope_id,numeric_value,min_value,max_value) VALUES('extension_days',?,20,7,60)",
                    "bar:" + bar);
        }
        RequestPostProcessor projectStaff = fixture.staff("usr_b62mp_prj_" + s, "staff", projectA, null, PICKUP_POLICY, RENEWAL_POLICY);

        String pickups = body(get(BASE + "/manual-pickup-policy"), projectStaff);
        List<String> pickupProjects = JsonPath.read(pickups, "$.data.items[*].project_id");
        assertThat(pickupProjects).contains("global", projectA).doesNotContain(projectB);

        String renewals = body(get(BASE + "/renewal-policy"), projectStaff);
        List<String> scopes = JsonPath.read(renewals, "$.data.items[*].scope_id");
        assertThat(scopes).contains("global", "project:" + projectA, "bar:" + barA)
                .doesNotContain("project:" + projectB, "bar:" + barB);
    }

    // 第十八批裁决 71「既然是最高权限，不应该什么都能看到吗？」：254-R7 消费历史条件「调整须最高权限本人办理」，最高权限读不到就办不了——不挂项目的最高权限读
    // 消费历史条件、有效期与转存参数、提醒节点三张表都是 200，各项目的行都看得到；不挂项目的普通账号照旧 403 PROJECT_SCOPE_REQUIRED。
    @Test
    void masterWithoutProjectReadsAllThreePoliciesButOrdinaryAccountWithoutProjectCannot() throws Exception {
        for (String project : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO saved_bottle_manual_pickup_policy(project_id,orders_within_1m,orders_within_3m,orders_total,min_orders,max_orders)"
                    + " VALUES(?,2,3,5,1,10)", project);
            jdbc.update("INSERT INTO saved_bottle_renewal_policy(policy_key,scope_id,numeric_value,min_value,max_value) VALUES('validity_days',?,45,7,90)",
                    "project:" + project);
        }
        RequestPostProcessor master = master(null);

        List<String> pickupProjects = JsonPath.read(body(get(BASE + "/manual-pickup-policy"), master), "$.data.items[*].project_id");
        assertThat(pickupProjects).contains("global", projectA, projectB);
        List<String> scopes = JsonPath.read(body(get(BASE + "/renewal-policy"), master), "$.data.items[*].scope_id");
        assertThat(scopes).contains("global", "project:" + projectA, "project:" + projectB);
        List<String> reminderScopes = JsonPath.read(body(get(BASE + "/reminder-policy"), master), "$.data.items[*].scope_id");
        assertThat(reminderScopes).contains("global");

        RequestPostProcessor noProject = fixture.staff("usr_b62mp_noprj_" + s, "staff", null, null,
                PICKUP_POLICY, RENEWAL_POLICY, REMINDER_POLICY);
        for (String path : List.of("/manual-pickup-policy", "/renewal-policy", "/reminder-policy")) {
            mockMvc.perform(get(BASE + path).with(noProject))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_REQUIRED"));
        }
    }

    // ── 夹具 ─────────────────────────────────────────────────────────────────────────

    /** 散客存酒（无会员编号），本店酒吧甲，存酒中、30 天后到期。 */
    private String insertBottle() {
        String id = "sb_b62mp_" + System.nanoTime();
        String phone = "155" + s.substring(s.length() - 8);
        jdbc.update("""
                INSERT INTO saved_bottles (
                    id, customer_id, bar_project_id, item_id, bottle_name, initial_quantity, remaining_quantity, unit, expire_at,
                    status, evidence_attachment_ids, created_at, updated_at, is_high_value, bottle_label_value,
                    customer_name_or_nickname, customer_phone, customer_phone_hash, registration_photo_refs, manual_note, merchant_id
                )
                VALUES (?, NULL, ?, ?, '人工例外威士忌', 2, 2, '瓶', ?, 'stored', '[]'::jsonb, now(), now(), false, 'LBL-B62MP',
                        '例外客人', ?, ?, '["reg_photo_b62mp"]'::jsonb, '吧台登记', ?)
                """, id, barA, item, Timestamp.from(Instant.now().plusSeconds(30L * 86400)),
                cipher.encrypt(phone), "hash_b62mp_" + s, barA);
        return id;
    }

    private void insertPickup(String id, String bottle, String condition, String status, String initiatedBy, Instant createdAt) {
        jdbc.update("INSERT INTO saved_bottle_manual_pickups(id,saved_bottle_id,merchant_id,project_id,phone_hash,reason,history_condition,"
                        + "initiated_by,initiated_at,status,created_at,updated_at) VALUES(?,?,?,?,?,'客人手机遗失',?,?,?,?,?,?)",
                id, bottle, barA, projectA, "hash_b62mp_" + s, condition, initiatedBy, Timestamp.from(createdAt), status,
                Timestamp.from(createdAt), Timestamp.from(createdAt));
    }

    /** 最高权限本人的后台 Web 会话（project 为归属项目，null 为不挂项目）。 */
    private RequestPostProcessor master(String project) {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME, SessionIssuer.MASTER_ACCOUNT_TYPE,
                project, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private ResultActions send(MockHttpServletRequestBuilder request, RequestPostProcessor who, String json) throws Exception {
        return mockMvc.perform(request.with(who).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String body(MockHttpServletRequestBuilder request, RequestPostProcessor who) throws Exception {
        return mockMvc.perform(request.with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static List<Integer> ints(String json, String path) {
        return JsonPath.read(json, path);
    }
}
