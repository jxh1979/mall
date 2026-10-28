package com.complex.platform.merchantportal;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.access.staff.PayrollTaxAssessment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 E 段 20-R4 扫尾（canReachProject 写法的同型读）真库＋真实接口回归：商户账号看各经营项目里本商户的数据。
 * 依据：治理地图第 659 行「一个商户在两个或更多项目中有经营的，看到的是每一个项目内他经营的商户。」；
 * XM-M001-D 答复 3 第 10 条所选选项原文「一个商户账号能看到各项目里本商户的数据；要把财务、员工排班等列表对商户账号改为只按商户过滤」。
 *
 * <p>夹具随测试事务回滚：商户 m 主项目 A、经营项目表里追加 B；商户 n 主项目 B。操作人是归属 A、商户 m 的商户账号（普通角色、没有勾选项目）。
 * 断言只看本次夹具建的行（编号都以随机后缀结尾）；请求一律按前端的蛇形键。修复前商户账号对 B 的行 403 或看不到（断言即红）；
 * 写入口不放开（对 B 的凭据上传、税务认定草案、业务交接、员工车辆登记仍按本人项目拒绝）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantAccountCrossProjectSweepReadsJdbcIntegrationTests {

    @Autowired
    private AccessRepository accessRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper json;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_mcs_a_" + suffix;
    private final String projectB = "prj_mcs_b_" + suffix;
    private final String merchantM = "mch_mcs_m_" + suffix;
    private final String merchantN = "mch_mcs_n_" + suffix;
    private final String acct = "usr_mcs_acct_" + suffix;
    private RequestPostProcessor merchantAccount;

    @BeforeEach
    void seed() {
        for (String project : List.of(projectA, projectB)) {
            jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", project, project);
        }
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, ?, 'retail', ?, 'active', now(), now())", merchantM, merchantM, projectA);
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, ?, 'retail', ?, 'active', now(), now())", merchantN, merchantN, projectB);
        jdbcTemplate.update("INSERT INTO merchant_projects (merchant_id, project_id) VALUES (?, ?)", merchantM, projectB);
        String roleId = "role_mcs_" + suffix;
        accessRepository.createRole(new RoleRecord(roleId, "M20R4 扫尾商户账号 " + suffix, null, "project", "active", Instant.now(),
                List.of("staff.payroll.manage", "staff.tax_bracket.manage", "staff.profile.review", "merchant.service_work_order.assign",
                        "finance.payroll.view", "finance.payroll.payment.register", "finance.ar_ap.view", "finance.refund.create",
                        "evidence.timeline.view", "evidence.attachment.create", "parking.vehicle.manage"),
                null, false, List.of()));
        merchantAccount = account(acct, projectA, merchantM, roleId);
    }

    // 税务认定：B 员工的认定列表与认定凭据可看；别的商户的员工照旧 403；对 B 的认定草案与凭据上传不放开。
    @Test
    void payrollTaxAssessmentsFollowMerchantAcrossOperatingProjects() throws Exception {
        String eB = employee("tb", projectB, merchantM);
        String eN = employee("tn", projectB, merchantN);
        String version = taxVersion();
        String taxB = taxAssessment("b", eB, projectB, merchantM, version);
        String taxN = taxAssessment("n", eN, projectB, merchantN, version);

        // 修复前红：B 员工认定列表 403 STAFF_SCOPE_MISMATCH
        assertThat(mine("/api/v1/staff/payroll/tax-assessments?user_id=" + eB + "&month=2099-03", "$.data[*].id")).containsExactly(taxB);
        timeline("payroll_tax_assessment", taxB).andExpect(status().isOk());
        denied(mockMvc.perform(get("/api/v1/staff/payroll/tax-assessments?user_id=" + eN + "&month=2099-03").with(merchantAccount)),
                "STAFF_SCOPE_MISMATCH");
        denied(timeline("payroll_tax_assessment", taxN), "STAFF_SCOPE_MISMATCH");

        // 写不放开
        denied(postJson("/api/v1/staff/payroll/tax-assessments", """
                {"idempotency_key": "%s", "user_id": "%s", "month": "2099-03", "category": "mcs", "tax_date": "2099-03-31",
                 "identity_basis": "已核", "transaction_basis": "服务收入", "professional_basis": "专业认定", "reason": "M20R4 扫尾"}
                """.formatted(key(), eB)), "STAFF_SCOPE_MISMATCH");
        denied(upload("payroll_tax_assessment", taxB), "STAFF_SCOPE_MISMATCH");
    }

    // 薪资方案凭据、工资付款凭据：查看与方案、工资应付查看同口径只按商户；上传不放开。
    @Test
    void payrollEvidenceFollowsMerchantAcrossOperatingProjects() throws Exception {
        String eB = employee("pb", projectB, merchantM);
        String eN = employee("pn", projectB, merchantN);
        String schemeB = scheme("b", eB, projectB, merchantM);
        String schemeN = scheme("n", eN, projectB, merchantN);
        String paymentB = payment("b", eB, projectB, merchantM);
        String paymentN = payment("n", eN, projectB, merchantN);

        // 修复前红：B 的方案凭据 403 PAYROLL_SCOPE_MISMATCH、付款凭据 403 PAYROLL_FINANCE_SCOPE_MISMATCH
        timeline("staff_payroll_scheme", schemeB).andExpect(status().isOk());
        timeline("staff_payroll_payment", paymentB).andExpect(status().isOk());
        denied(timeline("staff_payroll_scheme", schemeN), "PAYROLL_SCOPE_MISMATCH");
        denied(timeline("staff_payroll_payment", paymentN), "PAYROLL_FINANCE_SCOPE_MISMATCH");

        // 写不放开
        denied(upload("staff_payroll_scheme", schemeB), "PAYROLL_SCOPE_MISMATCH");
        denied(upload("staff_payroll_payment", paymentB), "PAYROLL_FINANCE_SCOPE_MISMATCH");
    }

    // 退款凭据：查看与退款详情同口径只按商户；上传不放开。
    @Test
    void refundEvidenceFollowsMerchantAcrossOperatingProjects() throws Exception {
        String refundB = refund("b", merchantM, projectB);
        String refundN = refund("n", merchantN, projectB);

        // 修复前红：403 CROSS_PROJECT_FORBIDDEN
        timeline("finance_refund", refundB).andExpect(status().isOk());
        denied(timeline("finance_refund", refundN), "CROSS_MERCHANT_FORBIDDEN");

        denied(upload("finance_refund", refundB), "CROSS_PROJECT_FORBIDDEN");
    }

    // 业务交接总览：B 员工的总览与其在 B 的待交接事项可看（不可指派）；别的商户的员工照旧 403；交接不放开。
    @Test
    void handoverOverviewFollowsMerchantAcrossOperatingProjects() throws Exception {
        String eB = employee("hb", projectB, merchantM);
        employee("hn", projectB, merchantN);
        String workOrderB = workOrder(eB, merchantM, projectB);

        // 修复前红：403 STAFF_PROFILE_SCOPE_FORBIDDEN
        assertThat(mine("/api/v1/staff/profiles/" + profileId("hb") + "/handover", "$.data.pending[*].business_id"))
                .containsExactly(workOrderB);
        mockMvc.perform(get("/api/v1/staff/profiles/" + profileId("hb") + "/handover").with(merchantAccount))
                .andExpect(jsonPath("$.data.pending[0].assignable").value(false));
        denied(mockMvc.perform(get("/api/v1/staff/profiles/" + profileId("hn") + "/handover").with(merchantAccount)),
                "STAFF_PROFILE_SCOPE_FORBIDDEN");

        denied(postJson("/api/v1/staff/profiles/" + profileId("hb") + "/handover", """
                {"idempotency_key": "%s", "business_type": "merchant_work_order", "business_id": "%s", "target_user_id": "%s",
                 "reason": "M20R4 扫尾"}
                """.formatted(key(), workOrderB, acct)), "STAFF_PROFILE_SCOPE_FORBIDDEN");
    }

    // 商户专区员工车辆：商户账号按本人项目进入（前端传本人项目），看得到各经营项目里本商户的员工车辆；登记仍只到本人项目。
    @Test
    void staffVehiclesFollowMerchantAcrossOperatingProjects() throws Exception {
        String eA = employee("va", projectA, merchantM);
        String eB = employee("vb", projectB, merchantM);
        String eN = employee("vn", projectB, merchantN);
        String vehicleA = vehicle("a", projectA, eA, merchantM);
        String vehicleB = vehicle("b", projectB, eB, merchantM);
        String vehicleN = vehicle("n", projectB, eN, merchantN);

        // 修复前红：只出 A 的车辆
        assertThat(mine("/api/v1/parking/staff-vehicles?project_id=" + projectA, "$.data.items[*].id"))
                .containsExactlyInAnyOrder(vehicleA, vehicleB).doesNotContain(vehicleN);

        denied(postJson("/api/v1/parking/staff-vehicles", """
                {"idempotency_key": "%s", "project_id": "%s", "user_id": "%s", "plate_no": "沪A%s"}
                """.formatted(key(), projectB, eB, suffix.substring(0, 5).toUpperCase())), "CROSS_PROJECT_FORBIDDEN");
    }

    // ── 夹具 ───────────────────────────────────────────────────────────────────────────────

    private String userId(String tag) {
        return "usr_mcs_" + tag + "_" + suffix;
    }

    private String profileId(String tag) {
        return "stf_mcs_" + tag + "_" + suffix;
    }

    private static String key() {
        return "mcs-" + UUID.randomUUID();
    }

    /** 建一名账号、挂上给定角色，返回以其身份登录的会话（后台 Web 终端＋生产过滤器放入的 AccessActor 主体，四项与账号行一致）。 */
    private RequestPostProcessor account(String userId, String organizationId, String merchantId, String... roleIds) {
        accessRepository.createUser(new AccessUser(userId, userId, "staff", organizationId, merchantId, "active", Instant.now()));
        for (String roleId : roleIds) {
            accessRepository.assignUserRole(userId, roleId);
        }
        AccessActor actor = new AccessActor(userId, userId, "staff", organizationId, merchantId, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    /** 商户员工：账号归属＝登记项目，一行已批准的档案（登记项目、商户按给定，所属公司取登记项目）。返回账号编号。 */
    private String employee(String tag, String projectId, String merchantId) {
        account(userId(tag), projectId, merchantId);
        jdbcTemplate.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, company_id, user_id, name, registration_status, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'M20R4 扫尾', 'approved', 'active', now(), now())
                """, profileId(tag), projectId, merchantId, projectId, userId(tag));
        return userId(tag);
    }

    private String taxVersion() {
        String id = "ptv_mcs_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO payroll_tax_versions (id, category, version, effective_from, applicability_basis, calculation_basis, rounding_rule,
                    brackets, reason, status, created_by, created_at, decided_at)
                VALUES (?, ?, 'V0.01', DATE '2099-01-01', '夹具', '夹具', 'truncate_cent_each_step', '[{}]'::jsonb, '夹具', 'approved', ?, now(), now())
                """, id, "mcs_" + suffix, acct);
        return id;
    }

    /** 2099-03 的一份原服务收入税务认定草案（无完整工资试算），项目、商户、发薪主体取员工档案。 */
    private String taxAssessment(String tag, String userId, String projectId, String merchantId, String version) throws Exception {
        String id = "pta_mcs_" + tag + "_" + suffix;
        String calculation = json.writeValueAsString(new PayrollTaxAssessment.Calculation(version, "V0.01", "mcs_" + suffix,
                LocalDate.parse("2099-01-01"), "truncate_cent_each_step", "夹具", null, 0, null, null, null, 0));
        jdbcTemplate.update("""
                INSERT INTO payroll_tax_assessments (id, user_id, month, project_id, merchant_id, payer_company_id, category, tax_date,
                    identity_basis, transaction_basis, professional_basis, income_cent, source_lines, source_fingerprint, tax_version_id,
                    tax_cent, calculation_snapshot, reason, status, created_by, created_at)
                VALUES (?, ?, '2099-03', ?, ?, ?, ?, DATE '2099-03-31', '已核', '服务收入', '专业认定', 0, '[]'::jsonb, 'fp', ?, 0, ?::jsonb,
                        '夹具', 'draft', ?, now())
                """, id, userId, projectId, merchantId, projectId, "mcs_" + suffix, version, calculation, acct);
        return id;
    }

    private String scheme(String tag, String userId, String projectId, String merchantId) {
        String id = "pss_mcs_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO staff_payroll_scheme_versions (id, user_id, project_id, merchant_id, payer_company_id, version, effective_from,
                    position_code, components, performance_rules, worktime_terms, payment_basis, reason, status, created_by)
                VALUES (?, ?, ?, ?, ?, 'V0.01', DATE '2099-03-01', 'waiter', '[]'::jsonb, 'null'::jsonb, '{}'::jsonb, '夹具', '夹具', 'draft', ?)
                """, id, userId, projectId, merchantId, projectId, acct);
        return id;
    }

    /** 2099-03 已定格的工资单、其工资应付与一笔待发起的付款记录（发薪主体取登记项目）。返回付款记录编号。 */
    private String payment(String tag, String userId, String projectId, String merchantId) {
        String statement = "pst_mcs_" + tag + "_" + suffix;
        String item = "arap_mcs_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent,
                    unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at, merchant_id, project_id)
                VALUES (?, ?, 'payable', ?, 'staff_payroll', ?, 5000, 0, 5000, 'CNY', DATE '2099-03-31', '2099-03', 'pending_payment', now(), now(), ?, ?)
                """, item, item, userId, statement, merchantId, projectId);
        jdbcTemplate.update("""
                INSERT INTO staff_payroll_statements (id, user_id, month, base_cent, total_cent, status, project_id, payer_company_id, merchant_id,
                    ar_ap_item_id)
                VALUES (?, ?, '2099-03', 5000, 5000, 'closed', ?, ?, ?, ?)
                """, statement, userId, projectId, projectId, merchantId, item);
        String id = "ppa_mcs_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO finance_payroll_payment_attempts (id, statement_id, ar_ap_item_id, payer_company_id, amount_cent, status, created_by)
                VALUES (?, ?, ?, ?, 5000, 'prepared', ?)
                """, id, statement, item, projectId, acct);
        return id;
    }

    private String refund(String tag, String merchantId, String projectId) {
        String id = "rf_mcs_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO finance_refunds (id, target_id, refund_amount_cent, reason, refund_channel, refund_direction, status, created_at,
                    source_type, merchant_id, project_id, requested_by, original_payment_ref, updated_at)
                VALUES (?, ?, 3000, 'M20R4 扫尾原路退回', 'wechat', 'original', 'failed', now(), 'ar_ap_payment', ?, ?, ?, 'orig-mcs', now())
                """, id, "target_" + id, merchantId, projectId, acct);
        return id;
    }

    /** 员工在 B 名下一张进行中的商户工单（工单铺位在 B、商户按给定）。 */
    private String workOrder(String ownerUserId, String merchantId, String projectId) {
        String space = "spc_mcs_" + suffix;
        jdbcTemplate.update("INSERT INTO spaces (id, project_id, merchant_id, space_name, space_type, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, 'M20R4 扫尾铺位', 'shop', 'active', now(), now())", space, projectId, merchantId);
        String id = "mswo_mcs_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO merchant_service_work_orders (id, merchant_id, space_id, work_order_type, priority, status, title, description,
                    reported_by_user_id, reported_channel, owner_user_id, created_at, updated_at)
                VALUES (?, ?, ?, 'service', 'low', 'assigned', 'M20R4 扫尾', '夹具', ?, 'web', ?, now(), now())
                """, id, merchantId, space, acct, ownerUserId);
        return id;
    }

    private String vehicle(String tag, String projectId, String userId, String merchantId) {
        String id = "psv_mcs_" + tag + "_" + suffix;
        jdbcTemplate.update("INSERT INTO parking_staff_vehicles (id, project_id, user_id, owner_merchant_id, plate_no, registered_by) "
                + "VALUES (?, ?, ?, ?, ?, ?)", id, projectId, userId, merchantId, ("M" + tag + suffix).toUpperCase(), acct);
        return id;
    }

    // ── 请求 ───────────────────────────────────────────────────────────────────────────────

    /** 以商户账号 GET（须 200），取出该 JSON 路径下属于本次夹具的编号（都以随机后缀结尾）；共享库里别的行不看。 */
    private List<String> mine(String url, String path) throws Exception {
        List<String> ids = JsonPath.read(mockMvc.perform(get(url).with(merchantAccount))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), path);
        return ids.stream().filter(id -> id != null && id.endsWith(suffix)).toList();
    }

    private ResultActions timeline(String type, String id) throws Exception {
        return mockMvc.perform(get("/api/v1/evidence/timelines/" + type + "/" + id).with(merchantAccount));
    }

    private ResultActions upload(String type, String id) throws Exception {
        return mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                .file(new MockMultipartFile("file", "mcs.png", "image/png", new byte[]{1}))
                .param("business_type", type).param("business_id", id).with(merchantAccount));
    }

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(merchantAccount).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static void denied(ResultActions result, String detailCode) throws Exception {
        result.andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value(detailCode));
    }
}
