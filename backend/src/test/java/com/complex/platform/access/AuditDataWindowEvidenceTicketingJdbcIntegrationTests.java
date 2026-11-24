package com.complex.platform.access;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.SoftAssertions;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 CM 段第 372 条第 2 段 372-S2-R1～R3（PGM:682「外部审计/监管只能查看划定的范围与时间段」；PGM:683「审计人员查看或导出敏感信息
 * （……完整操作记录导出）需先经审批并全程留痕。」；第十八批裁决 77「各清单、明细、导出只看得到这段时间发生的记录；没有发生日期的资料照常显示」）：
 * 操作留痕查询与导出、证据事件清单与证据时间线、票务订单、已发放权益、核销记录、门票退款台账、通票安全事件清单按外审数据时段承载。
 * 真库＋真实接口＋真实角色授权链，夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AuditDataWindowEvidenceTicketingJdbcIntegrationTests {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private static final String BEFORE = "2025-12-31 23:30:00+08";
    private static final String INSIDE = "2026-06-01 10:00:00+08";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = String.valueOf(System.nanoTime());
    private final String p1 = "adw2_p1_" + s;
    private final String p2 = "adw2_p2_" + s;
    private final String m1 = "adw2_m1_" + s;
    private final String auditor = "usr_adw2_auditor_" + s;
    private StaffRequestFixture staffs;
    private RequestPostProcessor auditorRequest;

    @BeforeEach
    void setUp() throws Exception {
        for (String project : List.of(p1, p2)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','外审时段综合体','active',now(),now())",
                    project);
        }
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, '外审时段商户', 'amusement', ?, 'active', now(), now())", m1, p1);
        staffs = new StaffRequestFixture(jdbc);
        auditorRequest = staffs.staff(auditor, "outsourced_vendor", p1, null,
                "audit.operation_log.view", "audit.operation_log.export", "evidence.event.view", "evidence.timeline.view",
                "finance.ar_ap.view", "ticket.order.read", "ticket.verify.view", "verification.view", "ticket.refund.view",
                "ticket.product.view");
        mockMvc.perform(put("/api/v1/access/users/" + auditor + "/audit-data-window").with(master())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"adw2-" + UUID.randomUUID() + "\",\"data_from\":\"2026-01-01\",\"data_to\":\"2026-12-31\"}"))
                .andExpect(status().isOk());
    }

    // ---------- 372-S2-R1 操作留痕查询与导出 ----------

    @Test
    void operationEventsListOnlyInsideWindowWhileUnwindowedStaffSeesAll() throws Exception {
        String biz = "adw2_biz_" + s;
        auditEvent("adw_probe", biz, BEFORE);
        String in1 = auditEvent("adw_probe", biz, "2026-01-15 10:00:00+08");
        String in2 = auditEvent("adw_probe", biz, "2026-12-31 23:59:00+08");
        auditEvent("adw_probe", biz, "2027-01-01 00:00:00+08");
        List<String> ids = ids(auditorRequest, "/api/v1/audit/operation-events?business_id=" + biz, "$.data[*].id");
        assertThat(ids).containsExactlyInAnyOrder(in1, in2);
        RequestPostProcessor projectStaff = staffs.staff("usr_adw2_logstaff_" + s, "staff", p1, null, "audit.operation_log.view");
        assertThat(ids(projectStaff, "/api/v1/audit/operation-events?business_id=" + biz, "$.data[*].id")).hasSize(4);
    }

    @Test
    void operationEventsExportRecordsWindowAndCountsOnlyEventsInsideIt() throws Exception {
        String biz = "adw2_exp_" + s;
        auditEvent("adw_probe", biz, BEFORE);
        auditEvent("adw_probe", biz, "2026-01-15 10:00:00+08");
        auditEvent("adw_probe", biz, "2026-12-31 23:59:00+08");
        auditEvent("adw_probe", biz, "2027-01-01 00:00:00+08");
        String device = "WBADV" + s.substring(s.length() - 10);
        jdbc.update("INSERT INTO browser_devices (device_no, first_user_id) VALUES (?, ?)", device, auditor);
        mockMvc.perform(get("/api/v1/audit/operation-events/export").param("business_id", biz).with(auditorRequest)
                        .cookie(new Cookie("complex_device_no", device)))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT filters->>'data_window' FROM export_files WHERE exporter_id = ? AND export_type = 'audit_operation_events'",
                String.class, auditor)).isEqualTo("2026-01-01～2026-12-31");
        assertThat(jdbc.queryForObject("SELECT metadata->>'row_count' FROM audit_events WHERE event_name = 'audit.operation_log.exported' AND actor_id = ?",
                String.class, auditor)).isEqualTo("2");
    }

    // ---------- 372-S2-R2 证据事件清单与证据时间线 ----------

    @Test
    void evidenceEventsListByOccurredAtWithTotalOnSameBasis() throws Exception {
        String a = evidenceEvent("adw2_eva_" + s, BEFORE, "2026-01-01 00:10:00+08", null);
        String b = evidenceEvent("adw2_evb_" + s, INSIDE, INSIDE, null);
        String body = mockMvc.perform(get("/api/v1/evidence/events?page=1&size=200").with(auditorRequest))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(body, "$.data.items[*].id");
        assertThat(ids).containsExactly(b).doesNotContain(a);
        assertThat(((Number) JsonPath.read(body, "$.data.total")).longValue()).isEqualTo(1L);
        RequestPostProcessor projectStaff = staffs.staff("usr_adw2_evstaff_" + s, "staff", p1, null, "evidence.event.view");
        String all = mockMvc.perform(get("/api/v1/evidence/events?page=1&size=200").with(projectStaff))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(((Number) JsonPath.read(all, "$.data.total")).longValue()).isEqualTo(2L);
    }

    @Test
    void evidenceTimelineKeepsAuditEventsAndReviewsInsideWindowAndListsAttachmentsWithTheirObject() throws Exception {
        String refund = "rf_adw2_" + s;
        jdbc.update("""
                INSERT INTO finance_refunds (id, target_id, refund_amount_cent, reason, refund_channel, refund_direction, status, created_at,
                    source_type, merchant_id, project_id, requested_by, original_payment_ref, updated_at)
                VALUES (?, ?, 3000, '外审时段退款', 'wechat', 'original', 'failed', CAST(? AS timestamptz), 'ar_ap_payment', ?, ?, 'usr_adw', 'orig-adw', now())
                """, refund, "target_" + refund, INSIDE, m1, p1);
        String outAudit = auditEvent("finance_refund", refund, BEFORE);
        String inAudit = auditEvent("finance_refund", refund, INSIDE);
        String payload = "{\"business_type\":\"finance_refund\",\"business_id\":\"" + refund + "\"}";
        String e1 = evidenceEvent("adw2_e1_" + s, "2026-12-31 23:00:00+08", "2027-01-01 01:00:00+08", payload);
        String e2 = evidenceEvent("adw2_e2_" + s, BEFORE, BEFORE, payload);
        String att1 = attachment("adw2_att1_" + s, refund, BEFORE);
        String att2 = attachment("adw2_att2_" + s, refund, INSIDE);
        String outReview = review("adw2_rvo_" + s, refund, BEFORE);
        String inReview = review("adw2_rvi_" + s, refund, INSIDE);
        String url = "/api/v1/evidence/timelines/finance_refund/" + refund;
        assertThat(ids(auditorRequest, url, "$.data[*].reference_id"))
                .contains(inAudit, e1, att1, att2, inReview)
                .doesNotContain(outAudit, e2, outReview);
        RequestPostProcessor projectStaff = staffs.staff("usr_adw2_tlstaff_" + s, "staff", p1, null,
                "finance.ar_ap.view", "evidence.timeline.view");
        assertThat(ids(projectStaff, url, "$.data[*].reference_id"))
                .contains(outAudit, inAudit, e1, e2, att1, att2, outReview, inReview);
    }

    // ---------- 372-S2-R3 票务订单、权益、核销、退款、安全事件 ----------

    @Test
    void ticketOrdersEntitlementsVerificationsRefundsAndSafetyEventsFollowWindow() throws Exception {
        String type = "tt_adw2_" + s;
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)"
                + " VALUES (?, '外审时段门票', 'fixed', 'active', CAST('2025-06-01 10:00:00+08' AS timestamptz), now())", type);
        String outOrder = order("out", type, BEFORE);
        String inOrder = order("in", type, INSIDE);
        String template = "tpl_adw2_" + s;
        jdbc.update("INSERT INTO entitlement_templates (id, name, entitlement_type, default_total_times, default_value_cent, status, created_at, updated_at)"
                + " VALUES (?, '外审时段入场', 'entry', 1, 0, 'active', now(), now())", template);
        String outEnt = entitlement("out", type, inOrder, template, BEFORE);
        String inEnt = entitlement("in", type, inOrder, template, INSIDE);
        jdbc.update("INSERT INTO terminal_devices (id, device_id, device_code, terminal_type, binding_scope, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, 'kiosk', 'venue', 'bound', now(), now())", "dev_adw2_" + s, "dev_adw2_" + s, "dev_adw2_" + s);
        String outVer = verification("out", inEnt, BEFORE);
        String inVer = verification("in", inEnt, INSIDE);
        String outRefund = ticketRefund("out", inOrder, BEFORE);
        String inRefund = ticketRefund("in", inOrder, INSIDE);
        String entity = "be_adw2_" + s;
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, '外审时段游乐公司', 'project_company', 'active', now(), now())", entity);
        String parent = "ti_adw2_" + s;
        jdbc.update("INSERT INTO ticket_instances (id, ticket_no, order_id, ticket_type_id, seq_in_order, num_people, total_times,"
                + " status, created_at, updated_at) VALUES (?, ?, ?, ?, 1, 2, 1, 'active', now(), now())", parent, "TN-" + parent, inOrder, type);
        String outSafety = safetyEvent("out", inOrder, parent, 1, entity, BEFORE);
        String inSafety = safetyEvent("in", inOrder, parent, 2, entity, INSIDE);

        // 五个清单逐个判（软断言：一次跑出每个清单各自的红绿）
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(ids(auditorRequest, "/api/v1/ticketing/orders", "$.data.items[*].id")).contains(inOrder).doesNotContain(outOrder);
        softly.assertThat(ids(auditorRequest, "/api/v1/ticketing/issued-entitlements", "$.data.items[*].id")).contains(inEnt).doesNotContain(outEnt);
        softly.assertThat(ids(auditorRequest, "/api/v1/ticketing/verifications", "$.data.items[*].id")).contains(inVer).doesNotContain(outVer);
        softly.assertThat(ids(auditorRequest, "/api/v1/ticketing/refunds", "$.data.items[*].id")).contains(inRefund).doesNotContain(outRefund);
        softly.assertThat(ids(auditorRequest, "/api/v1/ticketing/safety-events", "$.data.items[*].id")).contains(inSafety).doesNotContain(outSafety);
        softly.assertAll();

        // 不带时段的项目员工与外部人员账号照旧看到两头
        RequestPostProcessor projectStaff = staffs.staff("usr_adw2_tkstaff_" + s, "staff", p1, null,
                "ticket.order.read", "ticket.verify.view", "verification.view", "ticket.refund.view");
        RequestPostProcessor unwindowedVendor = staffs.staff("usr_adw2_tkvendor_" + s, "outsourced_vendor", p1, null,
                "ticket.order.read", "ticket.verify.view", "verification.view", "ticket.refund.view");
        for (RequestPostProcessor who : List.of(projectStaff, unwindowedVendor)) {
            assertThat(ids(who, "/api/v1/ticketing/orders", "$.data.items[*].id")).contains(inOrder, outOrder);
            assertThat(ids(who, "/api/v1/ticketing/issued-entitlements", "$.data.items[*].id")).contains(inEnt, outEnt);
            assertThat(ids(who, "/api/v1/ticketing/verifications", "$.data.items[*].id")).contains(inVer, outVer);
            assertThat(ids(who, "/api/v1/ticketing/refunds", "$.data.items[*].id")).contains(inRefund, outRefund);
            assertThat(ids(who, "/api/v1/ticketing/safety-events", "$.data.items[*].id")).contains(inSafety, outSafety);
        }

        // 没有发生日期的票务规则与留痕字典照常显示
        assertThat(ids(auditorRequest, "/api/v1/ticketing/ticket-types", "$.data.items[*].id")).contains(type);
        mockMvc.perform(get("/api/v1/audit/labels").with(auditorRequest)).andExpect(status().isOk());
    }

    // ---------- helpers ----------

    private List<String> ids(RequestPostProcessor who, String url, String path) throws Exception {
        return JsonPath.read(mockMvc.perform(get(url).with(who)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), path);
    }

    private String auditEvent(String businessType, String businessId, String occurredAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO audit_events (id, occurred_at, event_name, event_result, actor_type, actor_id, business_type, business_id, merchant_id)"
                + " VALUES (?, CAST(? AS timestamptz), 'adw.probe', 'success', 'staff', 'usr_adw2_actor', ?, ?, ?)",
                id, occurredAt, businessType, businessId, m1);
        return id.toString();
    }

    private String evidenceEvent(String id, String occurredAt, String receivedAt, String payload) {
        jdbc.update("INSERT INTO evidence_events (id, device_code, event_type, summary, payload, occurred_at, received_at, merchant_id)"
                + " VALUES (?, 'DEV-ADW2', 'terminal.login', '外审时段探针', COALESCE(CAST(? AS jsonb), '{}'::jsonb), CAST(? AS timestamptz), CAST(? AS timestamptz), ?)",
                id, payload, occurredAt, receivedAt, m1);
        return id;
    }

    private String attachment(String id, String refund, String updatedAt) {
        jdbc.update("INSERT INTO evidence_attachments (id, file_name, content_type, file_size, business_type, business_id, attachment_role,"
                + " object_key, upload_url, status, created_at, updated_at) VALUES (?, 'a.png', 'image/png', 1, 'finance_refund', ?, 'other',"
                + " 'evidence-files/adw2.png', '/adw2', 'uploaded', CAST(? AS timestamptz), CAST(? AS timestamptz))", id, refund, updatedAt, updatedAt);
        return id;
    }

    private String review(String id, String refund, String createdAt) {
        jdbc.update("INSERT INTO evidence_reviews (id, business_type, business_id, risk_level, decision, comment, evidence_attachment_ids, status,"
                + " created_at, updated_at) VALUES (?, 'finance_refund', ?, 'medium', 'approved', '外审时段复核', '[]'::jsonb, 'created',"
                + " CAST(? AS timestamptz), CAST(? AS timestamptz))", id, refund, createdAt, createdAt);
        return id;
    }

    private String order(String tag, String type, String createdAt) {
        String id = "to_adw2_" + tag + "_" + s;
        jdbc.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, pay_status, issue_status, status,"
                + " created_at, updated_at, merchant_id, bar_id, project_id) VALUES (?, ?, ?, 1, 'web', 8800, 'paid', 'issued', 'created',"
                + " CAST(? AS timestamptz), now(), NULL, NULL, ?)", id, "ADW2-" + id, type, createdAt, p1);
        return id;
    }

    private String entitlement(String tag, String type, String order, String template, String createdAt) {
        String id = "ie_adw2_" + tag + "_" + s;
        jdbc.update("INSERT INTO issued_entitlements (id, ticket_type_id, order_id, entitlement_template_id, entitlement_code, entitlement_type,"
                + " total_times, used_times, status, valid_from, valid_to, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, 'entry', 1, 0, 'active', CAST(? AS timestamptz), CAST('2027-12-31 00:00:00+08' AS timestamptz),"
                + " CAST(? AS timestamptz), now())", id, type, order, template, "CE-" + id, createdAt, createdAt);
        return id;
    }

    private String verification(String tag, String entitlement, String createdAt) {
        String id = "v_adw2_" + tag + "_" + s;
        jdbc.update("INSERT INTO ticket_verifications (id, verification_no, entitlement_id, entitlement_code, credential_type,"
                + " credential_payload, device_id, point_id, status, source_type, created_at, verified_people)"
                + " VALUES (?, ?, ?, ?, 'qr', 'x', ?, 'gate', 'success', 'admin', CAST(? AS timestamptz), 1)",
                id, "VN-" + id, entitlement, "CE-" + entitlement, "dev_adw2_" + s, createdAt);
        return id;
    }

    private String ticketRefund(String tag, String order, String createdAt) {
        String id = "trf_adw2_" + tag + "_" + s;
        jdbc.update("INSERT INTO ticket_refunds (id, target_id, refund_amount_cent, reason, refund_channel, status, requested_by, created_at, updated_at)"
                + " VALUES (?, ?, 1000, '外审时段退款', 'original', 'approved', 'usr_adw2', CAST(? AS timestamptz), now())", id, order, createdAt);
        return id;
    }

    private String safetyEvent(String tag, String order, String parent, int slot, String entity, String requestedAt) {
        String id = "tse_adw2_" + tag + "_" + s;
        jdbc.update("INSERT INTO ticket_safety_events (id, project_id, order_id, parent_ticket_id, slot_index, reason, situation,"
                + " responsible_entity_id, outcome, status, requested_by, requested_at)"
                + " VALUES (?, ?, ?, ?, ?, '外审时段安全例外', '场内突发情况', ?, 'refund', 'pending', 'usr_adw2', CAST(? AS timestamptz))",
                id, p1, order, parent, slot, entity, requestedAt);
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
