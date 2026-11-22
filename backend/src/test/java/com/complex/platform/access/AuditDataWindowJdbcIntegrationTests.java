package com.complex.platform.access;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BX 段第 372 条 372-R1～R4（PGM:682「外部审计/监管只能查看划定的范围与时间段」；第十八批裁决 77「开外审权限必须划定数据起止日期，
 * 各清单、明细、导出只看得到这段时间发生的记录；没有发生日期的资料照常显示」）：外审数据时段的划定与查看、开外审权限前须先划定、
 * 财务域各入口按时段承载。真库＋真实接口＋真实角色授权链，夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AuditDataWindowJdbcIntegrationTests {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String p1 = "adw_p1_" + s;
    private final String p2 = "adw_p2_" + s;
    private final String m1 = "adw_m1_" + s;
    private final String auditor = "usr_adw_auditor_" + s;
    private StaffRequestFixture staffs;
    private RequestPostProcessor auditorRequest;

    @BeforeEach
    void setUp() {
        for (String project : List.of(p1, p2)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','外审时段综合体','active',now(),now())",
                    project);
        }
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, '外审时段商户', 'amusement', ?, 'active', now(), now())", m1, p1);
        staffs = new StaffRequestFixture(jdbc);
        auditorRequest = staffs.staff(auditor, "outsourced_vendor", p1, null,
                "finance.ar_ap.view", "finance.reconciliation.view", "finance.ar_ap.export", "refund.authority.view");
    }

    // ---------- 372-R1 划定与查看 ----------

    @Test
    void masterSetsAndChangesWindowWithBeforeValuesInAuditAndListsIt() throws Exception {
        setWindow(master(), auditor, "2026-01-01", "2026-12-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data_from").value("2026-01-01"))
                .andExpect(jsonPath("$.data.data_to").value("2026-12-31"));
        String key = "adw-" + UUID.randomUUID();
        mockMvc.perform(put("/api/v1/access/users/" + auditor + "/audit-data-window").with(master())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"" + key + "\",\"data_from\":\"2026-03-01\",\"data_to\":\"2026-12-31\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data_from").value("2026-03-01"));
        assertThat(jdbc.queryForObject("SELECT data_from::text FROM audit_data_windows WHERE user_id = ?", String.class, auditor))
                .isEqualTo("2026-03-01");
        assertThat(jdbc.queryForObject("SELECT metadata->>'data_from_before' FROM audit_events"
                + " WHERE event_name = 'access.audit_data_window.set' AND idempotency_key = ?", String.class, key)).isEqualTo("2026-01-01");
        String list = mockMvc.perform(get("/api/v1/access/audit-data-windows").with(master()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> users = JsonPath.read(list, "$.data.items[*].user_id");
        assertThat(users).contains(auditor);
    }

    @Test
    void rejectsReversedRangeStaffTargetAndOtherProjectTargetWithoutWritingRows() throws Exception {
        setWindow(master(), auditor, "2026-12-31", "2026-01-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        String staffTarget = "usr_adw_staff_" + s;
        staffs.staff(staffTarget, "staff", p1, null, "finance.ar_ap.view");
        setWindow(master(), staffTarget, "2026-01-01", "2026-12-31")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("AUDIT_DATA_WINDOW_TARGET_INVALID"));
        String p2Vendor = "usr_adw_p2vendor_" + s;
        staffs.staff(p2Vendor, "outsourced_vendor", p2, null);
        RequestPostProcessor p1Manager = staffs.staff("usr_adw_p1mgr_" + s, "staff", p1, null, "access.user.manage");
        setWindow(p1Manager, p2Vendor, "2026-01-01", "2026-12-31")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_data_windows WHERE user_id IN (?, ?, ?)", Long.class,
                auditor, staffTarget, p2Vendor)).isZero();
    }

    @Test
    void listForProjectStaffShowsOnlyReachableAccounts() throws Exception {
        // 372-R1 plan ③「list() 范围同 AccessService.listAccessUsers」：只持 access.user.view 的项目人事看不到别的项目外审账号的时段
        windowAuditor();
        String p2Vendor = "usr_adw_p2listed_" + s;
        staffs.staff(p2Vendor, "outsourced_vendor", p2, null);
        setWindow(master(), p2Vendor, "2026-01-01", "2026-12-31").andExpect(status().isOk());
        RequestPostProcessor p1Viewer = staffs.staff("usr_adw_p1viewer_" + s, "staff", p1, null, "access.user.view");
        List<String> users = JsonPath.read(mockMvc.perform(get("/api/v1/access/audit-data-windows").with(p1Viewer))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.items[*].user_id");
        assertThat(users).contains(auditor).doesNotContain(p2Vendor);
    }

    // ---------- 372-R2 开外审权限前须先划定 ----------

    @Test
    void externalAuditTemplateAndPositionRequireWindowFirst() throws Exception {
        String vendor = "usr_adw_tplvendor_" + s;
        staffs.staff(vendor, "outsourced_vendor", p1, null);
        applyAuditTemplate("tpl_auditor_external", vendor)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("AUDIT_DATA_WINDOW_REQUIRED"));
        // 外部审计模板的直接复制件（只带一个日常查看码）同样先判须划定；划定后套用成功、有该账号的套用行
        String copy = "tpl_adw_copy_" + s;
        jdbc.update("INSERT INTO permission_templates (id, template_name, template_code, template_type, source_template_id, owner_scope_type,"
                + " usage_mode, single_use, status, created_at, updated_at) VALUES (?, ?, ?, 'base', 'tpl_auditor_external', 'platform',"
                + " 'merge', FALSE, 'active', now(), now())", copy, "外审模板复制件" + s, copy);
        jdbc.update("INSERT INTO permission_template_items (template_id, permission_code, effect, data_scope, sensitive)"
                + " VALUES (?, 'finance.ar_ap.view', 'grant', 'project', FALSE)", copy);
        applyAuditTemplate(copy, vendor)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("AUDIT_DATA_WINDOW_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM permission_template_applications WHERE target_id = ?", Long.class, vendor)).isZero();
        setWindow(master(), vendor, "2026-01-01", "2026-12-31").andExpect(status().isOk());
        applyAuditTemplate(copy, vendor).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM permission_template_applications WHERE target_id = ?", Long.class, vendor)).isPositive();
        // 外部审计模板本身划定后过了本闸（其后被既有的须审批码闸 GRANT_REQUIRES_APPROVAL 拦，与本条无关）
        assertThat(applyAuditTemplate("tpl_auditor_external", vendor).andReturn().getResponse().getContentAsString())
                .doesNotContain("AUDIT_DATA_WINDOW_REQUIRED");

        String positioned = "usr_adw_posvendor_" + s;
        staffs.staff(positioned, "outsourced_vendor", p1, null);
        assignAuditPosition(positioned)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("AUDIT_DATA_WINDOW_REQUIRED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM user_position_assignments WHERE user_id = ?", Long.class, positioned)).isZero();
        setWindow(master(), positioned, "2026-01-01", "2026-12-31").andExpect(status().isOk());
        assignAuditPosition(positioned).andExpect(status().isOk());
    }

    @Test
    void temporaryGrantAndGrantRequestForMarkedAuditorRequireWindowButPlainVendorDoesNot() throws Exception {
        String marked = "usr_adw_marked_" + s;
        staffs.staff(marked, "outsourced_vendor", p1, null);
        jdbc.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, 'role_auditor_external')", marked);
        temporaryGrant(marked)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("AUDIT_DATA_WINDOW_REQUIRED"));
        mockMvc.perform(post("/api/v1/access/grant-requests").with(master())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"adw-" + UUID.randomUUID() + "\",\"request_type\":\"temporary_permission\","
                                + "\"target_user_id\":\"" + marked + "\",\"permission_codes\":[\"finance.ar_ap.view\"],"
                                + "\"expires_at\":\"2099-01-01T00:00:00Z\",\"reason\":\"外审进场\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("AUDIT_DATA_WINDOW_REQUIRED"));
        String plain = "usr_adw_plain_" + s;
        staffs.staff(plain, "outsourced_vendor", p1, null);
        assertThat(temporaryGrant(plain).andReturn().getResponse().getContentAsString()).doesNotContain("AUDIT_DATA_WINDOW_REQUIRED");
    }

    @Test
    void temporaryGrantForExternalAuditOrgMemberRequiresWindow() throws Exception {
        // 设计 notes ①⑪「归属外部审计类机构即算外审标记」：没装外部审计角色、只绑了外部审计类机构的外部人员账号同样先判须划定
        String org = "po_adw_" + s;
        jdbc.update("INSERT INTO external_partner_orgs (id, project_id, org_name, org_category, created_at, updated_at)"
                + " VALUES (?, ?, '外审时段审计机构', 'external_audit', now(), now())", org, p1);
        String member = "usr_adw_orgmember_" + s;
        staffs.staff(member, "outsourced_vendor", p1, null);
        jdbc.update("UPDATE access_users SET partner_org_id = ? WHERE id = ?", org, member);
        temporaryGrant(member)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("AUDIT_DATA_WINDOW_REQUIRED"));
    }

    // ---------- 372-R3、R4 财务域按时段承载 ----------

    @Test
    void arApListKeepsOnlyItemsCreatedInsideWindowWhileUnwindowedAccountsSeeAll() throws Exception {
        windowAuditor();
        String keyword = "ADWKW" + s;
        arApItem(keyword + "A", "2025-12-31 23:30:00+08");
        String in1 = arApItem(keyword + "B", "2026-06-01 10:00:00+08");
        String in2 = arApItem(keyword + "C", "2026-12-31 23:59:00+08");
        arApItem(keyword + "D", "2027-01-01 00:00:00+08");
        String body = mockMvc.perform(get("/api/v1/finance/ar-ap-items").param("keyword", keyword).with(auditorRequest))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(body, "$.data.items[*].id");
        assertThat(ids).containsExactlyInAnyOrder(in1, in2);
        assertThat(((Number) JsonPath.read(body, "$.data.total")).longValue()).isEqualTo(2L);
        RequestPostProcessor projectStaff = staffs.staff("usr_adw_p1staff_" + s, "staff", p1, null, "finance.ar_ap.view");
        RequestPostProcessor unwindowedVendor = staffs.staff("usr_adw_novendor_" + s, "outsourced_vendor", p1, null, "finance.ar_ap.view");
        for (RequestPostProcessor who : List.of(projectStaff, unwindowedVendor)) {
            mockMvc.perform(get("/api/v1/finance/ar-ap-items").param("keyword", keyword).with(who))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(4));
        }
    }

    @Test
    void refundsListDetailAndQueryFollowWindow() throws Exception {
        windowAuditor();
        String out = refund("out", "2025-12-31 10:00:00+08", "ar_ap_payment");
        String in = refund("in", "2026-06-01 10:00:00+08", "ar_ap_payment");
        String body = mockMvc.perform(get("/api/v1/finance/refunds").with(auditorRequest))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(body, "$.data.items[*].id");
        assertThat(ids).contains(in).doesNotContain(out);
        mockMvc.perform(get("/api/v1/finance/refunds/" + out).with(auditorRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("OUT_OF_AUDIT_DATA_WINDOW"));
        mockMvc.perform(get("/api/v1/finance/refunds/" + in).with(auditorRequest)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/finance/refunds/" + out + "/query").with(auditorRequest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"adw-" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("OUT_OF_AUDIT_DATA_WINDOW"));
    }

    @Test
    void dealerReturnRefundDetailFollowsWindow() throws Exception {
        // 372-R4 plan ⑧「两处返回各加一行」：经销退货退款（dealer_order_refund）明细在时段外同样 403
        windowAuditor();
        String out = refund("dealer_out", "2025-12-31 10:00:00+08", "dealer_order_refund");
        mockMvc.perform(get("/api/v1/finance/refunds/" + out).with(auditorRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("OUT_OF_AUDIT_DATA_WINDOW"));
    }

    @Test
    void reconciliationBatchesAndDifferencesFollowBusinessDate() throws Exception {
        windowAuditor();
        String outBatch = batch("out", "2025-12-31");
        String inBatch = batch("in", "2026-01-01");
        String outDiff = difference(outBatch);
        String inDiff = difference(inBatch);
        List<String> batches = JsonPath.read(mockMvc.perform(get("/api/v1/finance/reconciliation/batches").with(auditorRequest))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.items[*].id");
        assertThat(batches).contains(inBatch).doesNotContain(outBatch);
        List<String> differences = JsonPath.read(mockMvc.perform(get("/api/v1/finance/reconciliation/differences").with(auditorRequest))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.items[*].id");
        assertThat(differences).contains(inDiff).doesNotContain(outDiff);
    }

    @Test
    void paymentsInternalPayableAndConfirmationsFollowWindow() throws Exception {
        windowAuditor();
        String item = arApItem("ADWPAY" + s, "2026-06-01 10:00:00+08");
        String outItem = arApItem("ADWOUT" + s, "2025-06-01 10:00:00+08");
        String inPayment = payment(item, "2026-07-01 10:00:00+08");
        String outPayment = payment(item, "2027-01-02 10:00:00+08");
        List<String> payments = JsonPath.read(mockMvc.perform(get("/api/v1/finance/ar-ap-items/" + item + "/payments").with(auditorRequest))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.items[*].id");
        assertThat(payments).contains(inPayment).doesNotContain(outPayment);
        mockMvc.perform(get("/api/v1/finance/internal-payables/" + outItem).with(auditorRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("OUT_OF_AUDIT_DATA_WINDOW"));
        String inConfirmation = confirmation(item, "approved", "2026-06-02 10:00:00+08");
        String outConfirmation = confirmation(item, "rejected", "2025-12-31 10:00:00+08");
        List<String> confirmations = JsonPath.read(mockMvc.perform(get("/api/v1/finance/payment-confirmations")
                        .param("object_type", "ar_ap_item").param("object_id", item).with(auditorRequest))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.confirmations[*].confirmation_id");
        assertThat(confirmations).contains(inConfirmation).doesNotContain(outConfirmation);
    }

    @Test
    void exportRecordsWindowAndCountsOnlyItemsInsideIt() throws Exception {
        windowAuditor();
        arApItem("ADWEXA" + s, "2025-12-31 23:30:00+08");
        arApItem("ADWEXB" + s, "2026-06-01 10:00:00+08");
        String device = "WBADW" + s.substring(s.length() - 10);
        jdbc.update("INSERT INTO browser_devices (device_no, first_user_id) VALUES (?, ?)", device, auditor);
        mockMvc.perform(get("/api/v1/finance/ar-ap-items/export").with(auditorRequest).cookie(new Cookie("complex_device_no", device)))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT filters->>'data_window' FROM export_files WHERE exporter_id = ?", String.class, auditor))
                .isEqualTo("2026-01-01～2026-12-31");
        assertThat(jdbc.queryForObject("SELECT metadata->>'row_count' FROM audit_events WHERE event_name = 'finance_ar_ap.exported' AND actor_id = ?",
                String.class, auditor)).isEqualTo("1");
    }

    @Test
    void refundRulesWithoutOccurrenceDateStayVisible() throws Exception {
        windowAuditor();
        mockMvc.perform(get("/api/v1/finance/refund-authority/rules").param("project_id", p1).with(auditorRequest))
                .andExpect(status().isOk());
    }

    // ---------- helpers ----------

    private void windowAuditor() throws Exception {
        setWindow(master(), auditor, "2026-01-01", "2026-12-31").andExpect(status().isOk());
    }

    private ResultActions setWindow(RequestPostProcessor who, String userId, String from, String to) throws Exception {
        return mockMvc.perform(put("/api/v1/access/users/" + userId + "/audit-data-window").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"adw-" + UUID.randomUUID() + "\",\"data_from\":\"" + from + "\",\"data_to\":\"" + to + "\"}"));
    }

    private ResultActions applyAuditTemplate(String templateId, String userId) throws Exception {
        return mockMvc.perform(post("/api/v1/access/permission-templates/" + templateId + "/apply").with(master())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"adw-" + UUID.randomUUID() + "\",\"target_user_ids\":[\"" + userId + "\"]}"));
    }

    private ResultActions assignAuditPosition(String userId) throws Exception {
        return mockMvc.perform(put("/api/v1/access/users/" + userId + "/position").with(master())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"adw-" + UUID.randomUUID() + "\",\"position_code\":\"auditor_external\","
                        + "\"staff_level_id\":\"lvl_m1_lower\"}"));
    }

    private ResultActions temporaryGrant(String userId) throws Exception {
        return mockMvc.perform(post("/api/v1/access/temporary-grants").with(master())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"adw-" + UUID.randomUUID() + "\",\"target_user_id\":\"" + userId + "\","
                        + "\"permission_codes\":[\"finance.ar_ap.view\"],\"valid_until\":\"2099-01-01T00:00:00Z\",\"reason\":\"外审进场\"}"));
    }

    private String arApItem(String itemNo, String createdAt) {
        String id = "arap_" + itemNo;
        jdbc.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent,
                    unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at, merchant_id, project_id)
                VALUES (?, ?, 'payable', ?, 'manual', ?, 5000, 0, 5000, 'CNY', DATE '2027-03-31', '2026-06', 'pending_payment',
                        CAST(? AS timestamptz), now(), ?, ?)
                """, id, itemNo, m1, id, createdAt, m1, p1);
        return id;
    }

    private String refund(String tag, String createdAt, String sourceType) {
        String id = "rf_adw_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO finance_refunds (id, target_id, refund_amount_cent, reason, refund_channel, refund_direction, status, created_at,
                    source_type, merchant_id, project_id, requested_by, original_payment_ref, updated_at)
                VALUES (?, ?, 3000, '外审时段退款', 'wechat', 'original', 'failed', CAST(? AS timestamptz), ?, ?, ?, 'usr_adw', 'orig-adw', now())
                """, id, "target_" + id, createdAt, sourceType, m1, p1);
        return id;
    }

    private String batch(String tag, String businessDate) {
        String id = "frb_adw_" + tag + "_" + s;
        jdbc.update("INSERT INTO finance_reconciliation_batches (id, batch_no, scope_type, scope_id, business_date, status, created_at)"
                + " VALUES (?, ?, 'project', ?, CAST(? AS date), 'open', now())", id, "ADW-" + tag + "-" + s, p1, businessDate);
        return id;
    }

    private String difference(String batchId) {
        String id = "frd_" + batchId;
        jdbc.update("INSERT INTO finance_reconciliation_differences (id, batch_id, difference_type, business_ref, amount_cent,"
                + " expected_amount_cent, actual_amount_cent, source, status, created_at)"
                + " VALUES (?, ?, 'amount', ?, 100, 0, 100, 'system', 'pending_identification', now())", id, batchId, id);
        return id;
    }

    private String payment(String itemId, String paymentTime) {
        String id = "pay_adw_" + UUID.randomUUID();
        jdbc.update("INSERT INTO ar_ap_payments (id, ar_ap_item_id, amount_cent, payment_method, payment_time, bank_flow_ref, created_at)"
                + " VALUES (?, ?, 100, 'bank_transfer', CAST(? AS timestamptz), ?, now())", id, itemId, paymentTime, "FLOW-" + id);
        return id;
    }

    private String confirmation(String itemId, String status, String requestedAt) {
        String id = "pc_adw_" + UUID.randomUUID();
        jdbc.update("INSERT INTO payment_confirmations (id, confirmation_id, payment_type, object_type, object_id, amount_cent, reason, status,"
                        + " review_id, project_id, requested_by, requested_at) VALUES (?, ?, 'manual', 'ar_ap_item', ?, 100, '外审时段付款确认', ?, ?, ?,"
                        + " 'usr_adw', CAST(? AS timestamptz))",
                "pcl_" + id, id, itemId, status, "rv_" + id, p1, requestedAt);
        return id;
    }

    /** 最高权限本人（保留超管账号，组织 platform-company、无商户），经后台 Web 终端。 */
    private RequestPostProcessor master() {
        AccessActor actor = new AccessActor(SessionIssuer.MASTER_USER_ID, SessionIssuer.MASTER_USERNAME,
                SessionIssuer.MASTER_ACCOUNT_TYPE, "platform-company", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
