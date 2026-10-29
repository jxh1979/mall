package com.complex.platform.inventory;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.evidence.EvidenceEventService;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewRepository;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
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
 * 第十八批 G 段 82-R18a：要货单、借用单记所属项目。第十八批裁决 18 所选「对，按业务发生地（推荐）」：「要货单、借用单建单时记所属项目
 * （在 m 的经营项目里选，只有一个时自动带），之后办理、赔偿应付、逾期工单都按单上的项目。」依据句 PGM:659「一个商户要在别的项目经营的，
 * 由能管理商户档案的人在商户档案里加经营项目，要经过审批才生效。」真库＋真实接口与授权链，请求体按前端蛇形键。
 *
 * <p>夹具随测试事务回滚：商户 m 主项目 A、经营项目 B（merchant_projects）；借出方 m2 只在 A。修复前单上没有项目：办理按 m 的主项目 A 判
 * （挂在 B 的员工办不了 B 门店的单、挂在 A 的反倒能办），赔偿应付记 m 的主项目 A，逾期工单记借出方主项目 A。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class InventoryTransferOrderProjectJdbcIntegrationTests {

    private static final String BASE = "/api/v1/inventory/transfer-orders";
    private static final Instant DUE = Instant.parse("2026-12-31T10:00:00Z");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private InventoryRepository repository;
    @Autowired private InventoryTransferFlowService flow;
    @Autowired private SupervisorReviewService reviews;
    @Autowired private SupervisorReviewRepository reviewRepository;
    @Autowired private EvidenceEventService evidence;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_r18a_a_" + suffix;
    private final String projectB = "prj_r18a_b_" + suffix;
    private final String projectC = "prj_r18a_c_" + suffix;
    private final String merchant = "mch_r18a_" + suffix;
    private final String lender = "mch_r18a_lender_" + suffix;
    private final String supplier = "sup_r18a_" + suffix;
    private final String item = "item_r18a_" + suffix;
    private final String device = "dev_r18a_" + suffix;
    private final String location = "loc_r18a_" + suffix;
    private final String codeA = "R18AA" + suffix;
    private final String codeB = "R18AB" + suffix;
    private RequestPostProcessor admin;
    private RequestPostProcessor storeB;
    private RequestPostProcessor staffA;
    private RequestPostProcessor staffB;
    private RequestPostProcessor lenderStaff;

    @BeforeEach
    void seed() {
        String entity = "ent_r18a_" + suffix;
        for (String project : List.of(projectA, projectB, projectC)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex',?,'active',now(),now())", project, "R18A " + project);
        }
        jdbc.update("INSERT INTO business_entities(id,entity_name,entity_role,status,created_at,updated_at) VALUES(?,'门店主体','project_company','active',now(),now())", entity);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES(?,?)", entity, projectA);
        for (String m : List.of(merchant, lender)) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,business_entity_id,project_id,status,created_at,updated_at) VALUES(?,?,'retail',?,?,'active',now(),now())", m, m, entity, projectA);
            jdbc.update("INSERT INTO contracts(id,merchant_id,contract_subject_id,settlement_subject_id,effective_date,expiry_date,lifecycle_status,allowed_business_scope,created_at,updated_at) VALUES(?,?,?,?,current_date-1,current_date+365,'effective','[\"inventory\",\"finance\"]'::jsonb,now(),now())", "ct_" + m, m, entity, entity);
        }
        jdbc.update("INSERT INTO merchant_projects(merchant_id,project_id) VALUES(?,?)", merchant, projectB);
        jdbc.update("INSERT INTO supply_entities(id,name,entity_type,level,status) VALUES(?,'中央酒水供货','liquor','platform','active')", supplier);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at) VALUES(?,'要货酒水','liquor','瓶',0,'active',?,now(),now())", item, merchant);
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at,non_consumable) VALUES(?,'舞台音响','equipment','台',0,'active',?,now(),now(),TRUE)", device, lender);
        for (String code : List.of(codeA, codeB)) {
            jdbc.update("INSERT INTO inventory_trace_codes(id,trace_code,item_id,merchant_id,owner_type,owner_id) VALUES(?,?,?,?,'merchant',?)", "tc_" + code, code, device, lender, lender);
        }
        repository.atomicIncreaseStockBalance(device, location, 5, "bal_r18a_" + suffix, lender, null, projectA);
        String role = "role_r18a_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'82-R18a','project','active',now())", role, role);
        jdbc.update("""
                INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN(
                'inventory.transfer_order.manage','inventory.transfer_order.view','governance.supervisor_review.view','governance.supervisor_review.approve',
                'finance.ar_ap.manage','evidence.attachment.create','facility.work_order.manage')
                """, role);
        admin = account("admin", "platform_admin", "org", null, role);
        storeB = account("store_b", "staff", projectB, merchant, role);
        staffA = account("staff_a", "staff", projectA, null, role);
        staffB = account("staff_b", "staff", projectB, null, role);
        lenderStaff = account("lender", "staff", projectA, lender, role);
        account("lender_boss", "staff", projectA, lender, role);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("lender"), user("lender_boss"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    // 建单记所选的经营项目（B 门店员工选 B）；只有一个经营项目的商户不带也自动带上。修复前红：单上没有项目
    @Test
    void createRecordsTheChosenOperatingProjectAndBringsTheOnlyOneAutomatically() throws Exception {
        String chosen = id(create(storeB, supplyBody(merchant, projectB)).andExpect(status().isOk()));
        assertThat(orderProject(chosen)).isEqualTo(projectB);

        String single = id(create(admin, supplyBody(lender, null)).andExpect(status().isOk()));
        assertThat(orderProject(single)).isEqualTo(projectA);
    }

    // 所属项目须在接收方商户的经营项目里（C 不是 m 的）；经营项目不止一个时不选 400；建单人够不着的项目 403；都不落单。修复前红：都建成了
    @Test
    void createRejectsAProjectOutsideTheOperatingProjectsAMissingChoiceOrOneBeyondReach() throws Exception {
        create(admin, supplyBody(merchant, projectC)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        create(admin, supplyBody(merchant, null)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        create(staffA, supplyBody(merchant, projectB)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM inventory_transfer_orders WHERE to_owner_id=?", Long.class, merchant)).isZero();
    }

    // 复核：建单的所属项目闸在幂等之前——归属 A、经只勾 B 的角色持码的人建了 B 门店的单后角色去掉 B，同一幂等键重放 403、
    // 不返回第一次结果。修复前红：重放 200
    @Test
    void createProjectGateRunsBeforeIdempotency() throws Exception {
        String ticked = "role_r18a_t_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'82-R18a','project','active',now())", ticked, ticked);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,'inventory.transfer_order.manage')", ticked);
        jdbc.update("INSERT INTO role_viewable_projects(role_id,viewable_id) VALUES(?,?)", ticked, projectB);
        RequestPostProcessor holder = account("holder", "staff", projectA, null, ticked);
        String body = supplyBody(merchant, projectB);

        create(holder, body).andExpect(status().isOk());
        jdbc.update("DELETE FROM role_viewable_projects WHERE role_id=?", ticked);
        create(holder, body).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
    }

    // 办理按单上的项目：B 门店的单，挂在 A 的员工办不了、挂在 B 的员工能办；取消审批等复核单的对象项目取单上的项目。修复前红：A 能办
    @Test
    void handlingFollowsTheOrderProjectInsteadOfTheMerchantsMainProject() throws Exception {
        String id = id(create(storeB, supplyBody(merchant, projectB)).andExpect(status().isOk()));

        call(staffA, id, "submit", "{}").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        call(staffB, id, "submit", "{}").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_supplier_review"));
        assertThat(reviewRepository.reviewObjectProjectIds("inventory_transfer_order", id)).containsExactly(projectB);
    }

    // 逾期工单记单上的项目（工单商户仍是借出方）。修复前红：记借出方主项目 A
    @Test
    void overdueWorkOrderIsRaisedInTheOrderProject() throws Exception {
        lentOut();
        flow.raiseLendingOverdue(DUE.plus(Duration.ofMinutes(1)));

        assertThat(jdbc.queryForMap("SELECT project_id,merchant_id FROM facility_work_orders WHERE merchant_id=? AND source='lending_overdue'", lender))
                .containsEntry("project_id", projectB).containsEntry("merchant_id", lender);
    }

    // 无法归还的赔偿应付（借入方 m 应付借出方）记单上的项目。修复前红：记 m 的主项目 A
    @Test
    void unreturnableCompensationPayableIsBookedInTheOrderProject() throws Exception {
        String id = lentOut();
        String disposition = JsonPath.read(call(storeB, id, "unreturnable", "{\"line_id\":\"" + id + "_l0\",\"quantity\":1,\"trace_codes\":[\"" + codeB
                + "\"],\"reason\":\"演出中摔坏\",\"disposal_note\":\"借出方收回残件报废\",\"evidence_attachment_ids\":[\"" + upload(id) + "\"]}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        call(lenderStaff, id, "dispositions/" + disposition + "/confirm", "{\"operator_note\":\"音响摔坏，同意报废处置\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("pending_supervisor"));
        decide(jdbc.queryForObject("SELECT supervisor_review_id FROM inventory_transfer_dispositions WHERE id=?", String.class, disposition));
        call(admin, id, "dispositions/" + disposition + "/review", "{\"decision\":\"approved\",\"reason\":\"按内部结算价赔偿\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("approved"));

        String payable = jdbc.queryForObject("SELECT payable_item_id FROM inventory_transfer_dispositions WHERE id=?", String.class, disposition);
        assertThat(jdbc.queryForMap("SELECT merchant_id,counterparty_id,project_id FROM ar_ap_items WHERE id=?", payable))
                .containsEntry("merchant_id", merchant).containsEntry("counterparty_id", lender).containsEntry("project_id", projectB);
    }

    // ── 夹具 ──

    private String supplyBody(String to, String project) {
        return """
                {"idempotency_key":"%s","transfer_type":"supply","from_owner_type":"supply_entity","from_owner_id":"%s",
                 "to_owner_type":"merchant","to_owner_id":"%s",%s"lines":[{"item_id":"%s","quantity":2,"unit_price_cent":1000,"trace_codes":[]}]}
                """.formatted(key(), supplier, to, project == null ? "" : "\"project_id\":\"" + project + "\",", item);
    }

    /** m2 借给 m 的两码借用单，记 B：按蛇形键建单 → 借出方交货扫码 → 借入方（B 门店）收货扫码。 */
    private String lentOut() throws Exception {
        String body = """
                {"idempotency_key":"%s","transfer_type":"lending","lending_mode":"borrow","from_owner_type":"merchant","from_owner_id":"%s",
                 "to_owner_type":"merchant","to_owner_id":"%s","from_location_id":"%s","return_due_at":"%s","project_id":"%s",
                 "lines":[{"item_id":"%s","quantity":2,"unit_price_cent":80000,"trace_codes":["%s","%s"]}]}
                """.formatted(key(), lender, merchant, location, DUE, projectB, device, codeA, codeB);
        String id = id(create(admin, body).andExpect(status().isOk()));
        call(lenderStaff, id, "ship", "{\"scanned_trace_codes\":[\"" + codeA + "\",\"" + codeB + "\"]}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("shipped"));
        call(storeB, id, "receive", "{\"scanned_trace_codes\":[\"" + codeA + "\",\"" + codeB + "\"]}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("received"));
        return id;
    }

    private ResultActions create(RequestPostProcessor who, String body) throws Exception {
        return mockMvc.perform(post(BASE).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** 补上幂等键后按蛇形键原样提交。 */
    private ResultActions call(RequestPostProcessor who, String id, String action, String body) throws Exception {
        String withKey = "{\"idempotency_key\":\"" + key() + "\"" + (body.equals("{}") ? "}" : "," + body.substring(1));
        return mockMvc.perform(post(BASE + "/" + id + "/" + action).with(who).contentType(MediaType.APPLICATION_JSON).content(withKey));
    }

    private String orderProject(String id) {
        return jdbc.queryForObject("SELECT project_id FROM inventory_transfer_orders WHERE id=?", String.class, id);
    }

    private String upload(String orderId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(user("store_b"), user("store_b"), "staff", projectB, merchant, "active"), "test", List.of()));
        try {
            return evidence.uploadAttachment(new MockMultipartFile("file", "damage.png", "image/png",
                    Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jCYQAAAAASUVORK5CYII=")),
                    "inventory_transfer_order", orderId, "other").id();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** 借出方主管（确认人的直接上级）批准无法归还的主管复核。 */
    private void decide(String review) {
        String userId = user("lender_boss");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "staff", projectA, lender, "active"), "test", List.of()));
        try {
            reviews.approve(review, new SupervisorReviewApprovalRequest(key(), "核对双方确认的原因与处置", List.of(), null, "approved", "主管裁决"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private RequestPostProcessor account(String tag, String accountType, String organizationId, String merchantId, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,?,?,?,'active',now())",
                userId, userId, accountType, organizationId, merchantId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, organizationId);
        AccessActor actor = new AccessActor(userId, userId, accountType, organizationId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String user(String tag) {
        return "usr_r18a_" + tag + "_" + suffix;
    }

    private static String id(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
