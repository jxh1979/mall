package com.complex.platform.facility;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.evidence.EvidenceEventService;
import com.complex.platform.governance.review.SupervisorReviewApprovalRequest;
import com.complex.platform.governance.review.SupervisorReviewService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * HP-F03 真库、真接口回归（请求体一律按前端蛇形键）：
 * BSM L1408「楼宇设备报废由设备所有权主体确认，沿现有审批流程办理，记录批准人、处置方式及实际处置凭证；残值或损失由财务记录并关联原设备。
 * 标记报废不等于实物处置或款项完成」；ENUM L266；PGM L821；批准 13｜Q19「报废状态、实际处置、款项分别记，标报废不等于都完成」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FacilityEquipmentScrapJdbcIntegrationTests {

    private static final String EQUIPMENT_BASE = "/api/v1/facility/equipment/";
    private static final String SCRAP_BASE = "/api/v1/facility/equipment-scraps";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SupervisorReviewService reviews;
    @Autowired private EvidenceEventService evidence;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "hpf03_prj_" + suffix;
    private final String equipment = "hpf03_feq_" + suffix;
    private RequestPostProcessor manager;
    private RequestPostProcessor technician;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','报废回归项目','active',now(),now())", project);
        String full = "hpf03_role_" + suffix;
        String facilityOnly = "hpf03_role_tech_" + suffix;
        role(full, "'facility.equipment.manage','governance.supervisor_review.view','governance.supervisor_review.approve',"
                + "'finance.ar_ap.manage','finance.payment.register','evidence.attachment.create','evidence.timeline.view'");
        role(facilityOnly, "'facility.equipment.manage','evidence.attachment.create'");
        manager = account("mgr", full);
        account("boss", full);
        technician = account("tech", facilityOnly);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", user("mgr"), user("boss"));
        jdbc.update("""
                INSERT INTO facility_equipment (id, name, equipment_type, space_id, responsible_user_id, status, project_id)
                VALUES (?, '3号冷却塔', 'hvac', 'sp_roof', ?, 'normal', ?)""", equipment, user("tech"), project);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /** R1：报废申请记处置方式、待审批，设备状态不动；所有权主体（发起人上级）批准后才置报废并记批准人。 */
    @Test
    void scrapNeedsOwnerApprovalBeforeRetiredAndRecordsApproverAndMethod() throws Exception {
        String scrapId = idOf(requestScrap()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approval_status").value("pending_approval"))
                .andExpect(jsonPath("$.data.disposal_method").value("整机交回收商")));
        assertThat(equipmentStatus()).isEqualTo("normal");

        decide(scrapId, "approved");

        assertThat(equipmentStatus()).isEqualTo("retired");
        Map<String, Object> row = jdbc.queryForMap("SELECT status, approved_by, approved_at, disposal_method FROM facility_equipment_scraps WHERE id=?", scrapId);
        assertThat(row.get("status")).isEqualTo("approved");
        assertThat(row.get("approved_by")).isEqualTo(user("boss"));
        assertThat(row.get("approved_at")).isNotNull();
        assertThat(row.get("disposal_method")).isEqualTo("整机交回收商");
        Map<String, Object> listed = listed(scrapId);
        assertThat(listed.get("disposal_status")).isEqualTo("pending");
        assertThat(listed.get("money_status")).isEqualTo("pending_finance");
        assertThat(listed.get("completed")).isEqualTo(false);
    }

    /** R1：状态变更不能直接置报废。 */
    @Test
    void directStatusChangeToRetiredRequiresScrapApproval() throws Exception {
        send(manager, EQUIPMENT_BASE + equipment + "/status", "{\"status\":\"retired\",\"idempotency_key\":\"st_" + suffix + "\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("FACILITY_EQUIPMENT_SCRAP_APPROVAL_REQUIRED"));
        assertThat(equipmentStatus()).isEqualTo("normal");
    }

    /** R1：在办期间不能重复申请；驳回后设备照常在用、可重新申请。 */
    @Test
    void rejectedScrapKeepsEquipmentInServiceAndAllowsNewRequest() throws Exception {
        String scrapId = idOf(requestScrap().andExpect(status().isOk()));
        requestScrap().andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("FACILITY_EQUIPMENT_SCRAP_PENDING"));

        decide(scrapId, "rejected");

        assertThat(equipmentStatus()).isEqualTo("normal");
        assertThat(jdbc.queryForObject("SELECT status FROM facility_equipment_scraps WHERE id=?", String.class, scrapId)).isEqualTo("rejected");
        requestScrap().andExpect(status().isOk()).andExpect(jsonPath("$.data.approval_status").value("pending_approval"));
    }

    /** R1：已报废设备不能经状态变更或维保完成回到在用。 */
    @Test
    void retiredEquipmentCannotReturnToServiceByStatusOrMaintenanceDone() throws Exception {
        decide(idOf(requestScrap().andExpect(status().isOk())), "approved");

        send(manager, EQUIPMENT_BASE + equipment + "/status", "{\"status\":\"normal\",\"idempotency_key\":\"st2_" + suffix + "\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("FACILITY_EQUIPMENT_RETIRED"));
        mockMvc.perform(post(EQUIPMENT_BASE + equipment + "/maintenance-done?idempotency_key=md_" + suffix).with(manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("FACILITY_EQUIPMENT_RETIRED"));
        assertThat(equipmentStatus()).isEqualTo("retired");
    }

    /** R2：实物处置须批准后、附已上传且绑定本报废单的凭证单独登记；登记后款项仍待财务，不算全部收尾。 */
    @Test
    void disposalNeedsBoundUploadedEvidenceAndStaysSeparateFromMoney() throws Exception {
        String scrapId = idOf(requestScrap().andExpect(status().isOk()));
        disposal(scrapId, "[]").andExpect(status().isConflict());
        decide(scrapId, "approved");

        disposal(scrapId, "[]").andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("EVIDENCE_REQUIRED"));
        // 绑定在另一张报废单上的凭证不能拿来登记本单的实际处置。
        jdbc.update("INSERT INTO facility_equipment (id, name, equipment_type, status, project_id) VALUES (?, '4号冷却塔', 'hvac', 'normal', ?)",
                equipment + "_b", project);
        String otherScrap = idOf(send(manager, EQUIPMENT_BASE + equipment + "_b/scrap-requests",
                "{\"reason\":\"锈蚀\",\"disposal_method\":\"拆解\",\"idempotency_key\":\"rqb_" + suffix + "\"}").andExpect(status().isOk()));
        String foreign = upload("facility_equipment_scrap", otherScrap);
        disposal(scrapId, "[\"" + foreign + "\"]").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("EVIDENCE_BINDING_MISMATCH"));
        String proof = upload("facility_equipment_scrap", scrapId);
        disposal(scrapId, "[\"" + proof + "\"]").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.disposal_status").value("completed"));

        Map<String, Object> listed = listed(scrapId);
        assertThat(listed.get("disposal_status")).isEqualTo("completed");
        assertThat(listed.get("money_status")).isEqualTo("pending_finance");
        assertThat(listed.get("completed")).isEqualTo(false);
        assertThat(equipmentStatus()).isEqualTo("retired");
    }

    /** R3：残值登记为关联本报废单的应收，收足前款项未结清；实物也已处置时收足才全部收尾。 */
    @Test
    void residualPostsReceivableLinkedToScrapAndMoneySettlesOnlyAfterCollection() throws Exception {
        String scrapId = idOf(requestScrap().andExpect(status().isOk()));
        decide(scrapId, "approved");
        disposal(scrapId, "[\"" + upload("facility_equipment_scrap", scrapId) + "\"]").andExpect(status().isOk());

        finance(manager, scrapId, "{\"residual_amount_cent\":30000,\"residual_counterparty_id\":\"recycler_x\",\"loss_amount_cent\":0,\"idempotency_key\":\"fin_" + suffix + "\"}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.money_status").value("residual_pending_collection"));
        Map<String, Object> item = jdbc.queryForMap("""
                SELECT id, direction, counterparty_id, source_id, unpaid_amount_cent FROM ar_ap_items WHERE source_type='facility_equipment_scrap' AND source_id=?""", scrapId);
        assertThat(item.get("direction")).isEqualTo("receivable");
        assertThat(item.get("counterparty_id")).isEqualTo("recycler_x");
        assertThat(((Number) item.get("unpaid_amount_cent")).longValue()).isEqualTo(30000L);
        assertThat(listed(scrapId).get("completed")).isEqualTo(false);

        send(manager, "/api/v1/finance/ar-ap-items/" + item.get("id") + "/payments",
                "{\"amount_cent\":30000,\"payment_method\":\"bank_transfer\",\"idempotency_key\":\"pay_" + suffix + "\"}")
                .andExpect(status().isOk());

        Map<String, Object> listed = listed(scrapId);
        assertThat(listed.get("money_status")).isEqualTo("settled");
        assertThat(listed.get("completed")).isEqualTo(true);
    }

    /** R3：损失登记为关联本报废单的内部核算（登记即确认），款项结清；实物未处置仍不算全部收尾。 */
    @Test
    void lossPostsInternalAccountingAndCountsAsSettled() throws Exception {
        String scrapId = idOf(requestScrap().andExpect(status().isOk()));
        decide(scrapId, "approved");

        finance(manager, scrapId, "{\"residual_amount_cent\":0,\"loss_amount_cent\":50000,\"idempotency_key\":\"fin_" + suffix + "\"}")
                .andExpect(status().isOk());

        Map<String, Object> item = jdbc.queryForMap("""
                SELECT direction, status, source_id FROM ar_ap_items WHERE source_type='facility_equipment_scrap_loss' AND source_id=?""", scrapId);
        assertThat(item.get("direction")).isEqualTo("internal_accounting");
        assertThat(item.get("status")).isEqualTo("settled");
        Map<String, Object> listed = listed(scrapId);
        assertThat(listed.get("money_status")).isEqualTo("settled");
        assertThat(listed.get("disposal_status")).isEqualTo("pending");
        assertThat(listed.get("completed")).isEqualTo(false);
    }

    /** R3：财务登记须财务权限、须已批准、每单一次；有残值须写回收方。 */
    @Test
    void financeRecordNeedsFinancePermissionApprovedScrapAndOnlyOnce() throws Exception {
        String scrapId = idOf(requestScrap().andExpect(status().isOk()));
        String body = "{\"residual_amount_cent\":0,\"loss_amount_cent\":1000,\"idempotency_key\":\"%s\"}";
        finance(manager, scrapId, body.formatted("f0_" + suffix)).andExpect(status().isConflict());
        decide(scrapId, "approved");

        finance(technician, scrapId, body.formatted("f1_" + suffix)).andExpect(status().isForbidden());
        finance(manager, scrapId, "{\"residual_amount_cent\":100,\"loss_amount_cent\":0,\"idempotency_key\":\"f2_" + suffix + "\"}")
                .andExpect(status().isBadRequest());
        finance(manager, scrapId, body.formatted("f3_" + suffix)).andExpect(status().isOk());
        finance(manager, scrapId, body.formatted("f4_" + suffix)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("FACILITY_EQUIPMENT_SCRAP_FINANCE_RECORDED"));
    }

    private ResultActions requestScrap() throws Exception {
        return send(manager, EQUIPMENT_BASE + equipment + "/scrap-requests",
                "{\"reason\":\"压缩机烧毁\",\"disposal_method\":\"整机交回收商\",\"idempotency_key\":\"rq_" + UUID.randomUUID() + "\"}");
    }

    private ResultActions disposal(String scrapId, String evidenceIds) throws Exception {
        return send(manager, SCRAP_BASE + "/" + scrapId + "/disposal", "{\"evidence_attachment_ids\":" + evidenceIds
                + ",\"disposal_note\":\"回收商已拉走\",\"idempotency_key\":\"dp_" + UUID.randomUUID() + "\"}");
    }

    private ResultActions finance(RequestPostProcessor who, String scrapId, String body) throws Exception {
        return send(who, SCRAP_BASE + "/" + scrapId + "/finance", body);
    }

    private ResultActions send(RequestPostProcessor who, String path, String body) throws Exception {
        return mockMvc.perform(post(path).with(who).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private Map<String, Object> listed(String scrapId) throws Exception {
        String body = mockMvc.perform(get(SCRAP_BASE).with(manager)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> rows = JsonPath.read(body, "$.data.items[?(@.id == '" + scrapId + "')]");
        assertThat(rows).hasSize(1);
        return rows.get(0);
    }

    private String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    private String equipmentStatus() {
        return jdbc.queryForObject("SELECT status FROM facility_equipment WHERE id=?", String.class, equipment);
    }

    private void decide(String scrapId, String decision) {
        String review = jdbc.queryForObject("SELECT review_id FROM facility_equipment_scraps WHERE id=?", String.class, scrapId);
        act(user("boss"));
        try {
            reviews.approve(review, new SupervisorReviewApprovalRequest("dec_" + UUID.randomUUID(), "核对报废原因与处置方式",
                    List.of(), null, decision, "所有权主体审批"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String upload(String businessType, String businessId) {
        act(user("mgr"));
        try {
            return evidence.uploadAttachment(new MockMultipartFile("file", "disposal.png", "image/png",
                    Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jCYQAAAAASUVORK5CYII=")),
                    businessType, businessId, "other").id();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void act(String userId) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(userId, userId, "staff", project, null, "active"), "test", List.of()));
    }

    private void role(String role, String permissions) {
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'报废回归','project','active',now())", role, role);
        jdbc.update("INSERT INTO role_permissions(role_id,permission_code) SELECT ?,permission_code FROM permissions WHERE permission_code IN(" + permissions + ")", role);
    }

    private RequestPostProcessor account(String tag, String role) {
        String userId = user(tag);
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,NULL,'active',now())",
                userId, userId, project);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, role);
        jdbc.update("INSERT INTO staff_level_assignments(user_id,staff_level_id,organization_id) VALUES(?,'lvl_m3_upper',?)", userId, project);
        AccessActor actor = new AccessActor(userId, userId, "staff", project, null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    private String user(String tag) {
        return "hpf03_" + tag + "_" + suffix;
    }
}
