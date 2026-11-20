package com.complex.platform.facility;

// 文件版本：V0.03

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
    @Autowired private com.complex.platform.finance.FinanceService financeService;

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
        mockMvc.perform(post(EQUIPMENT_BASE + equipment + "/maintenance-done?idempotency_key=md_" + suffix
                        + "&maintenance_type=fault_repair").with(manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("FACILITY_EQUIPMENT_RETIRED"));
        assertThat(equipmentStatus()).isEqualTo("retired");
    }

    /**
     * 第十六批第 36 条（ENUM L267「设备维保类型 定期保养、例行巡检、故障维修、安全检测、部件更换」；BSM L1408「维保分……」）：
     * 维保完成按前端形状带 maintenance_type，不带或不在五类内拒绝；成功后台账记最近维保类型、审计载荷留痕，之后的状态变更不抹掉。
     */
    @Test
    void maintenanceDoneRequiresOneOfFiveTypesAndRecordsIt() throws Exception {
        String path = EQUIPMENT_BASE + equipment + "/maintenance-done?idempotency_key=";
        mockMvc.perform(post(path + "mt0_" + suffix).with(manager).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        mockMvc.perform(post(path + "mt1_" + suffix + "&maintenance_type=overhaul").with(manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT last_maintenance_at FROM facility_equipment WHERE id=?", Object.class, equipment)).isNull();

        mockMvc.perform(post(path + "mt2_" + suffix + "&maintenance_type=part_replacement").with(manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("normal"))
                .andExpect(jsonPath("$.data.last_maintenance_type").value("part_replacement"));
        assertThat(jdbc.queryForObject("SELECT last_maintenance_type FROM facility_equipment WHERE id=?", String.class, equipment))
                .isEqualTo("part_replacement");
        assertThat(jdbc.queryForObject("""
                SELECT metadata->>'maintenance_type' FROM audit_events
                 WHERE event_name='facility.equipment_maintained' AND business_id=?""", String.class, equipment))
                .isEqualTo("part_replacement");

        send(manager, EQUIPMENT_BASE + equipment + "/status", "{\"status\":\"disabled\",\"idempotency_key\":\"mt3_" + suffix + "\"}")
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT last_maintenance_type FROM facility_equipment WHERE id=?", String.class, equipment))
                .isEqualTo("part_replacement");
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

    /**
     * 第十八批 BH 段第 261 条 261-B14-R5（BSM:1408「残值或损失由财务记录并关联原设备」；PGM:303「不会因为重试就重复扣款、重复入账」）：
     * 两张报废单先后做财务登记，请求编号按后台真实格式（「admin-web-」＋13 位毫秒＋「-」＋8 位随机）。两张单的残值应收、损失核算各落各的台账行，
     * 金额与来源单各是自己的。修复前派生号由「来源类型＋请求编号」截到 40 个字符，损失一行只剩来源类型加「admin_web」，后登记的把先登记的覆盖。
     */
    @Test
    void twoScrapsKeepTheirOwnResidualAndLossLedgerRows() throws Exception {
        String first = idOf(requestScrap().andExpect(status().isOk()));
        decide(first, "approved");
        String equipment2 = "hpf03_feq2_" + suffix;
        jdbc.update("""
                INSERT INTO facility_equipment (id, name, equipment_type, space_id, responsible_user_id, status, project_id)
                VALUES (?, '4号冷却塔', 'hvac', 'sp_roof', ?, 'normal', ?)""", equipment2, user("tech"), project);
        String second = idOf(send(manager, EQUIPMENT_BASE + equipment2 + "/scrap-requests",
                "{\"reason\":\"风机断轴\",\"disposal_method\":\"整机交回收商\",\"idempotency_key\":\"rq_" + UUID.randomUUID() + "\"}")
                .andExpect(status().isOk()));
        decide(second, "approved");

        finance(manager, first, "{\"residual_amount_cent\":20000,\"residual_counterparty_id\":\"recycler_x\",\"loss_amount_cent\":12000,"
                + "\"idempotency_key\":\"admin-web-1794000000123-" + suffix + "\"}").andExpect(status().isOk());
        finance(manager, second, "{\"residual_amount_cent\":30000,\"residual_counterparty_id\":\"recycler_y\",\"loss_amount_cent\":34000,"
                + "\"idempotency_key\":\"admin-web-1794050000456-" + suffix + "\"}").andExpect(status().isOk());

        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, source_type || '/' || source_id || '/' || amount_cent AS line FROM ar_ap_items
                 WHERE source_type IN ('facility_equipment_scrap','facility_equipment_scrap_loss') AND source_id IN (?, ?)""", first, second);
        assertThat(rows).extracting(row -> row.get("line")).containsExactlyInAnyOrder(
                "facility_equipment_scrap/" + first + "/20000", "facility_equipment_scrap_loss/" + first + "/12000",
                "facility_equipment_scrap/" + second + "/30000", "facility_equipment_scrap_loss/" + second + "/34000");
        List<String> linked = jdbc.queryForList("""
                SELECT x FROM facility_equipment_scraps, LATERAL (VALUES (residual_ar_ap_item_id), (loss_ar_ap_item_id)) AS v(x)
                 WHERE id IN (?, ?)""", String.class, first, second);
        assertThat(linked).doesNotContainNull().doesNotHaveDuplicates().hasSize(4)
                .containsExactlyInAnyOrderElementsOf(rows.stream().map(row -> (String) row.get("id")).toList());
    }

    /** 261-B14-R5（BSM:1072「该娱乐平台公司可经审批先垫付并向原门店追收」）：两条存酒赔付单各挂追收（键为后台请求编号加「:recovery」），各落各的应收行。 */
    @Test
    void twoSavedBottleRemedyRecoveriesKeepTheirOwnLedgerRows() {
        String bar = "mch_" + UUID.randomUUID();
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,project_id,created_at,updated_at)"
                + " VALUES(?,?,'bar','active',?,now(),now())", bar, "垫付追收回归酒吧" + suffix, project);
        financeService.postSavedBottleRemedyRecovery("sbr_a_" + suffix, bar, 12000L, "admin-web-1794000000123-" + suffix + ":recovery");
        financeService.postSavedBottleRemedyRecovery("sbr_b_" + suffix, bar, 34000L, "admin-web-1794050000456-" + suffix + ":recovery");
        assertThat(jdbc.queryForList("SELECT source_id || '/' || amount_cent FROM ar_ap_items"
                + " WHERE source_type = 'saved_bottle_remedy_recovery' AND counterparty_id = ?", String.class, bar))
                .containsExactlyInAnyOrder("sbr_a_" + suffix + "/12000", "sbr_b_" + suffix + "/34000");
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
