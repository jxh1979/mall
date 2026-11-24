package com.complex.platform.evidence;

// 文件版本：V0.03

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.auth.SessionIssuer;
import com.complex.platform.customer.CustomerRecord;
import com.complex.platform.customer.CustomerRepository;
import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 BA 段第 324 条 324-R1、324-R2（PGM:899「谁发起、谁销售、谁服务、谁第一个处理异常、为什么、谁审批、结果是什么，都要能查到。」；PGM:682
 * 「内部稽核可查看本综合体全部数据」；第十八批裁决 71 最高权限什么都能看）：操作留痕查询与审计事件页的证据事件列表，不挂商户的非平台管理员按入口权限码可触及的
 * 项目看该项目内商户与人员所做的操作，最高权限看全部，没有项目归属的 403 PROJECT_SCOPE_REQUIRED。真库＋真实接口＋真实角色授权链，夹具随测试事务回滚。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class AuditOperationEventsScopeJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customers;

    private final String s = String.valueOf(System.nanoTime());
    private final String p1 = "aud_p1_" + s;
    private final String p2 = "aud_p2_" + s;
    private final String m1 = "aud_m1_" + s;
    private final String m2 = "aud_m2_" + s;
    private final String p1Staff = "usr_aud_p1_" + s;
    private final String m1User = "usr_aud_m1_" + s;
    private final String p2Staff = "usr_aud_p2_" + s;
    private final String eventName = "b73.audit_scope_probe_" + s;
    private RequestPostProcessor auditor;
    private RequestPostProcessor auditorWithoutProject;

    @BeforeEach
    void setUp() {
        for (String project : List.of(p1, p2)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','留痕范围综合体','active',now(),now())",
                    project);
        }
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, '留痕范围商户甲', 'amusement', ?, 'active', now(), now())", m1, p1);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, '留痕范围商户乙', 'amusement', ?, 'active', now(), now())", m2, p2);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        auditor = staffs.staff("usr_aud_auditor_" + s, "staff", p1, null, "audit.operation_log.view", "evidence.event.view");
        auditorWithoutProject = staffs.staff("usr_aud_noprj_" + s, "staff", null, null, "audit.operation_log.view", "evidence.event.view");
        staffs.staff(p1Staff, "staff", p1, null, "ops.production_readiness.view");
        staffs.staff(m1User, "staff", p1, m1, "ops.production_readiness.view");
        staffs.staff(p2Staff, "staff", p2, null, "ops.production_readiness.view");
        auditEvent(p1Staff, null);
        auditEvent(m1User, m1);
        auditEvent(p2Staff, null);
    }

    @Test
    void auditorWithoutMerchantSeesOperationEventsOfOwnProjectAndMasterSeesAll() throws Exception {
        assertThat(actorIds(auditor)).containsExactlyInAnyOrder(p1Staff, m1User);
        assertThat(actorIds(master())).containsExactlyInAnyOrder(p1Staff, m1User, p2Staff);
        mockMvc.perform(get("/api/v1/audit/operation-events").param("event_name", eventName).with(auditorWithoutProject))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_REQUIRED"));
    }

    @Test
    void auditorWithoutMerchantSeesEvidenceEventsOfOwnProjectAndMasterSeesAll() throws Exception {
        String e1 = evidenceEvent(m1);
        String e2 = evidenceEvent(m2);
        String own = mockMvc.perform(get("/api/v1/evidence/events").param("page", "1").param("size", "20").with(auditor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> ownIds = JsonPath.read(own, "$.data.items[*].id");
        assertThat(ownIds).containsExactly(e1);
        assertThat(((Number) JsonPath.read(own, "$.data.total")).longValue()).isEqualTo(1L);
        String all = mockMvc.perform(get("/api/v1/evidence/events").param("page", "1").param("size", "200").with(master()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> allIds = JsonPath.read(all, "$.data.items[*].id");
        assertThat(allIds).contains(e1, e2);
    }

    /**
     * 18BA 收尾复核修正（PGM:659「一个商户在两个或更多项目中有经营的，看到的是每一个项目内他经营的商户」；PGM:682「内部稽核可查看本综合体全部数据」）：
     * 商户所属项目＝主项目加已批准的经营项目（merchant_projects，与 listMerchantOperatingProjectIds 同一口径）。主项目在乙、经批准也在甲经营的商户，
     * 其证据事件与商户级账号的操作留痕对甲的稽核可见；只提了申请、尚未批准的不可见。
     */
    @Test
    void auditorSeesEventsOfMerchantApprovedToOperateInOwnProject() throws Exception {
        String m3 = "aud_m3_" + s;
        String m4 = "aud_m4_" + s;
        String m3User = "usr_aud_m3_" + s;
        String m4User = "usr_aud_m4_" + s;
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, '留痕范围商户丙', 'amusement', ?, 'active', now(), now())", m3, p2);
        jdbc.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, '留痕范围商户丁', 'amusement', ?, 'active', now(), now())", m4, p2);
        jdbc.update("INSERT INTO merchant_projects (merchant_id, project_id) VALUES (?, ?)", m3, p1);
        jdbc.update("INSERT INTO merchant_project_requests (id, merchant_id, project_id, reason, requested_by, review_id, status) "
                + "VALUES (?, ?, ?, '到甲项目经营', ?, ?, 'pending_approval')", "mpr_aud_" + s, m4, p1, m4User, "rv_aud_" + s);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        staffs.staff(m3User, "staff", p2, m3, "ops.production_readiness.view");
        staffs.staff(m4User, "staff", p2, m4, "ops.production_readiness.view");
        auditEvent(m3User, m3);
        auditEvent(m4User, m4);
        String e3 = evidenceEvent(m3);
        evidenceEvent(m4);

        assertThat(actorIds(auditor)).containsExactlyInAnyOrder(p1Staff, m1User, m3User);
        String own = mockMvc.perform(get("/api/v1/evidence/events").param("page", "1").param("size", "20").with(auditor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> ownIds = JsonPath.read(own, "$.data.items[*].id");
        assertThat(ownIds).containsExactly(e3);
        assertThat(((Number) JsonPath.read(own, "$.data.total")).longValue()).isEqualTo(1L);
    }

    /**
     * 第十八批 BT 段第 370 条 370-R1（PGM:682「内部稽核可查看本综合体全部数据」；PGM:413「当前先按一个综合体落地」）：只有一个启用中的综合体时，
     * 不带商户、操作人也不属于任何项目的留痕（客人、无登录主体的系统作业、system_scheduler、平台管理员）与不带商户的证据事件，本综合体稽核一并可见；
     * 已停用项目的员工所做操作仍归该项目、不并入。
     */
    @Test
    void auditorOfSoleActiveComplexAlsoSeesEventsBelongingToNoProject() throws Exception {
        jdbc.update("UPDATE projects SET status = 'suspended' WHERE status = 'active' AND id <> ?", p1);
        String customer = "cus_aud_" + s;
        String platformAdmin = "usr_aud_padm_" + s;
        new StaffRequestFixture(jdbc).staff(platformAdmin, "platform_admin", "platform-company", null, "ops.production_readiness.view");
        eventsBelongingToNoProject(customer, platformAdmin);
        String noMerchant = evidenceEventWithoutMerchant();

        assertThat(actorIds(auditor)).containsExactlyInAnyOrder(p1Staff, m1User, customer, "system_scheduler", platformAdmin, null);
        String own = mockMvc.perform(get("/api/v1/evidence/events").param("page", "1").param("size", "20").with(auditor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> ownIds = JsonPath.read(own, "$.data.items[*].id");
        assertThat(ownIds).contains(noMerchant);
        assertThat(((Number) JsonPath.read(own, "$.data.total")).longValue()).isGreaterThanOrEqualTo(1L);
    }

    /** 第 370 条 370-R1：两个综合体都启用时，归不到任何项目的留痕与不带商户的证据事件不对只能触及其中一个的稽核出现。 */
    @Test
    void auditorOfOneOfTwoActiveComplexesDoesNotSeeEventsBelongingToNoProject() throws Exception {
        String customer = "cus_aud_" + s;
        String platformAdmin = "usr_aud_padm_" + s;
        new StaffRequestFixture(jdbc).staff(platformAdmin, "platform_admin", "platform-company", null, "ops.production_readiness.view");
        eventsBelongingToNoProject(customer, platformAdmin);
        String noMerchant = evidenceEventWithoutMerchant();

        assertThat(actorIds(auditor)).containsExactlyInAnyOrder(p1Staff, m1User);
        String own = mockMvc.perform(get("/api/v1/evidence/events").param("page", "1").param("size", "20").with(auditor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> ownIds = JsonPath.read(own, "$.data.items[*].id");
        assertThat(ownIds).doesNotContain(noMerchant);
    }

    /**
     * 第十八批裁决 79（BSM:833「平台各岗位（含最高权限）一律看不到商户级卡的卡号、持卡人、余额与充值、退款流水」）：平台管理员与最高权限查操作留痕，
     * 以商户级卡为对象（customer_stored_value）或以商户级卡充值意向为对象（payment_intent）的储值事件都不列，通用卡的照列；商户账号照旧看本商户的（修复前平台侧都列）。
     * 18CL 收尾复核：商户员工在冻结、解冻、线上与柜台退卡写口失败的留痕以客人编号为对象，平台侧同样不列（修复前列出、可认出持卡人）；
     * 商户员工的成功留痕以账户为对象（如酒吧结账扣通用卡），通用卡的照列。
     */
    @Test
    void platformSideDoesNotSeeMerchantCardStoredValueEvents() throws Exception {
        String customer = "cus_aud_" + s;
        String phone = "139" + s.substring(s.length() - 8);
        customers.saveCustomer(new CustomerRecord(customer, phone, "139****" + phone.substring(7), "active", null, null, Instant.now(), Instant.now()));
        String generalCard = "svacc_aud_g_" + s;
        String merchantCard = "svacc_aud_m_" + s;
        jdbc.update("INSERT INTO customer_stored_value_accounts (id, customer_id, project_id, merchant_id, balance_cent, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, NULL, 0, 'active', now(), now())", generalCard, customer, p1);
        jdbc.update("INSERT INTO customer_stored_value_accounts (id, customer_id, project_id, merchant_id, balance_cent, status, created_at, updated_at) "
                + "VALUES (?, ?, NULL, ?, 0, 'active', now(), now())", merchantCard, customer, m1);
        String generalIntent = "pi_aud_g_" + s;
        String merchantIntent = "pi_aud_m_" + s;
        jdbc.update("INSERT INTO stored_value_topup_intents (payment_intent_id, customer_id, project_id, merchant_id, created_at) VALUES (?, ?, ?, NULL, now())",
                generalIntent, customer, p1);
        jdbc.update("INSERT INTO stored_value_topup_intents (payment_intent_id, customer_id, project_id, merchant_id, created_at) VALUES (?, ?, NULL, ?, now())",
                merchantIntent, customer, m1);
        storedValueEvent("customer_stored_value", generalCard, null, "success");
        storedValueEvent("customer_stored_value", merchantCard, m1, "success");
        storedValueEvent("payment_intent", generalIntent, null, "success");
        storedValueEvent("payment_intent", merchantIntent, m1, "success");
        String generalAtBar = "svacc_aud_gb_" + s;
        jdbc.update("INSERT INTO customer_stored_value_accounts (id, customer_id, project_id, merchant_id, balance_cent, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, NULL, 0, 'active', now(), now())", generalAtBar, customer, p2);
        storedValueEvent("customer_stored_value", customer, m1, "failure");
        storedValueEvent("customer_stored_value", generalAtBar, m1, "success");
        RequestPostProcessor admin = new StaffRequestFixture(jdbc).staff("usr_aud_pf_" + s, "platform_admin", null, null, "audit.operation_log.view");
        RequestPostProcessor shop = new StaffRequestFixture(jdbc).staff("usr_aud_sh_" + s, "staff", p1, m1, "audit.operation_log.view");

        for (RequestPostProcessor platformSide : List.of(admin, master())) {
            assertThat(businessIds(platformSide)).contains(generalCard, generalIntent, generalAtBar).doesNotContain(merchantCard, merchantIntent, customer);
        }
        assertThat(businessIds(shop)).containsExactlyInAnyOrder(merchantCard, merchantIntent, customer, generalAtBar);
    }

    // ---------- helpers ----------

    private List<String> businessIds(RequestPostProcessor who) throws Exception {
        String body = mockMvc.perform(get("/api/v1/audit/operation-events").param("event_name", eventName + ".stored_value").with(who))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data[*].business_id");
    }

    private void storedValueEvent(String businessType, String businessId, String merchantId, String result) {
        jdbc.update("INSERT INTO audit_events (id, occurred_at, event_name, event_result, actor_type, actor_id, business_type, business_id, merchant_id) "
                + "VALUES (?, now(), ?, ?, 'customer', 'cus_aud', ?, ?, ?)", UUID.randomUUID(), eventName + ".stored_value", result, businessType, businessId,
                merchantId);
    }

    private List<String> actorIds(RequestPostProcessor who) throws Exception {
        String body = mockMvc.perform(get("/api/v1/audit/operation-events").param("event_name", eventName).with(who))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data[*].actor_id");
    }

    private void auditEvent(String actorId, String merchantId) {
        auditEvent("staff", actorId, merchantId);
    }

    private void auditEvent(String actorType, String actorId, String merchantId) {
        jdbc.update("INSERT INTO audit_events (id, occurred_at, event_name, event_result, actor_type, actor_id, merchant_id) "
                + "VALUES (?, now(), ?, 'success', ?, ?, ?)", UUID.randomUUID(), eventName, actorType, actorId, merchantId);
    }

    /** 第 370 条：都不带商户——客人（合成客人号）、无登录主体的系统作业（操作人空、类型落默认 staff）、system_scheduler、平台管理员。 */
    private void eventsBelongingToNoProject(String customer, String platformAdmin) {
        auditEvent("customer", customer, null);
        auditEvent("staff", null, null);
        auditEvent("platform_admin", "system_scheduler", null);
        auditEvent("platform_admin", platformAdmin, null);
    }

    /** 平台管理员登记的、不带商户的证据事件；收到时间排在最前。 */
    private String evidenceEventWithoutMerchant() {
        String id = "evt_aud_nomerchant_" + s;
        jdbc.update("INSERT INTO evidence_events (id, device_code, event_type, summary, occurred_at, received_at, merchant_id) "
                + "VALUES (?, 'DEV-AUD', 'terminal.login', '留痕范围探针', now(), now() + interval '1 day', NULL)", id);
        return id;
    }

    /** 收到时间排在最前（其余用例留在库里的事件不挤出第一页）。 */
    private String evidenceEvent(String merchantId) {
        String id = "evt_aud_" + merchantId;
        jdbc.update("INSERT INTO evidence_events (id, device_code, event_type, summary, occurred_at, received_at, merchant_id) "
                + "VALUES (?, 'DEV-AUD', 'terminal.login', '留痕范围探针', now(), now() + interval '1 day', ?)", id, merchantId);
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
