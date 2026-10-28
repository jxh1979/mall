package com.complex.platform.merchantportal;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.access.AccessRepository;
import com.complex.platform.access.AccessUser;
import com.complex.platform.access.RoleRecord;
import com.complex.platform.support.InternalPayDayFixtures;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
 * 第十八批 E 段 20-R3 真库＋真实接口回归：商户账号看各经营项目里本商户的数据（财务与员工系列）。
 * 依据：治理地图第 659 行「一个商户在两个或更多项目中有经营的，看到的是每一个项目内他经营的商户。」「商户账号持有这类角色也不能跨项目」；
 * XM-M001-D 答复 3 第 10 条所选选项原文「一个商户账号能看到各项目里本商户的数据；要把财务、员工排班等列表对商户账号改为只按商户过滤」。
 *
 * <p>夹具随测试事务回滚：商户 m 主项目 A、经营项目表里追加 B；商户 n 主项目 B。操作人是归属 A、商户 m 的商户账号，
 * 角色经权限仓储直接建立并分配（普通角色、没有勾选项目）。会话主体的归属、商户、账号类型、状态与账号行一致。
 * 共享库里有别的数据：断言只看本次夹具建的行（编号都以随机后缀结尾）。请求一律按前端的蛇形键。
 * 修复前商户账号只看到 A 的行（列表包含断言即红）；写入口不放开（对 B 的登记收付款、退款查询结果仍 403）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class MerchantAccountCrossProjectReadJdbcIntegrationTests {

    @Autowired
    private AccessRepository accessRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_mcr_a_" + suffix;
    private final String projectB = "prj_mcr_b_" + suffix;
    private final String merchantM = "mch_mcr_m_" + suffix;
    private final String merchantN = "mch_mcr_n_" + suffix;
    private RequestPostProcessor merchantAccount;

    @BeforeEach
    void seed() {
        InternalPayDayFixtures.seed(jdbcTemplate);
        for (String project : List.of(projectA, projectB)) {
            jdbcTemplate.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) "
                    + "VALUES (?, 'complex', ?, 'active', now(), now())", project, project);
        }
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, ?, 'bar', ?, 'active', now(), now())", merchantM, merchantM, projectA);
        jdbcTemplate.update("INSERT INTO merchants (id, merchant_name, merchant_type, project_id, status, created_at, updated_at) "
                + "VALUES (?, ?, 'bar', ?, 'active', now(), now())", merchantN, merchantN, projectB);
        jdbcTemplate.update("INSERT INTO merchant_projects (merchant_id, project_id) VALUES (?, ?)", merchantM, projectB);
        String roleId = "role_mcr_" + suffix;
        accessRepository.createRole(new RoleRecord(roleId, "M20R3 商户账号 " + suffix, null, "project", "active", Instant.now(),
                List.of("finance.ar_ap.view", "finance.ar_ap.export", "finance.payroll.view", "finance.payroll.export",
                        "finance.payment.register", "merchant.portal.settlement.view", "staff.shift.manage",
                        "staff.attendance.view", "staff.profile.view", "staff.payroll.manage"),
                null, false, List.of()));
        merchantAccount = account("acct", projectA, merchantM, roleId);
    }

    // 财务系列：应收应付列表与导出、退款列表与详情、收付款记录、商户专区门票分账只按商户；对 B 行的写入口不放开。
    @Test
    void financeListsAndReadDetailsFollowMerchantAcrossOperatingProjects() throws Exception {
        String itemA = arApItem("a", merchantM, projectA, "mcr_fixture");
        String itemB = arApItem("b", merchantM, projectB, "mcr_fixture");
        String itemN = arApItem("n", merchantN, projectB, "mcr_fixture");
        String employee = employee("pay", projectB, merchantM);
        String payrollB = arApItem("pay", merchantM, projectB, "staff_payroll");
        String statementB = payrollStatement("fin", employee, projectB, merchantM);
        jdbcTemplate.update("UPDATE ar_ap_items SET source_id = ? WHERE id = ?", statementB, payrollB);
        jdbcTemplate.update("UPDATE staff_payroll_statements SET ar_ap_item_id = ? WHERE id = ?", payrollB, statementB);
        String refundA = refund("a", merchantM, projectA);
        String refundB = refund("b", merchantM, projectB);
        String refundN = refund("n", merchantN, projectB);
        String ticketB = ticketSettlement(projectB);

        // 修复前红：只出 A 的行
        assertThat(mine("/api/v1/finance/ar-ap-items?size=200", "$.data.items[*].id"))
                .containsExactlyInAnyOrder(itemA, itemB, payrollB).doesNotContain(itemN);
        assertThat(mine("/api/v1/finance/ar-ap-items/export", "$.data.rows[*].id"))
                .containsExactlyInAnyOrder(itemA, itemB, payrollB);
        assertThat(mine("/api/v1/finance/refunds", "$.data.items[*].id")).containsExactlyInAnyOrder(refundA, refundB)
                .doesNotContain(refundN);
        mockMvc.perform(get("/api/v1/finance/refunds/" + refundB).with(merchantAccount))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.refund.id").value(refundB));
        mockMvc.perform(get("/api/v1/finance/ar-ap-items/" + itemB + "/payments").with(merchantAccount))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/finance/ar-ap-items/" + payrollB + "/payments").with(merchantAccount))
                .andExpect(status().isOk());
        assertThat(mine("/api/v1/merchant-portal/ticket-settlements", "$.data[*].ar_ap_item_id")).containsExactly(ticketB);

        // 写不放开：对 B 行登记收付款、对 B 退款查询结果仍按本人项目拒绝
        denied(postJson("/api/v1/finance/ar-ap-items/" + itemB + "/payments", """
                {"idempotency_key": "%s", "amount_cent": 100, "payment_method": "bank_transfer", "bank_flow_ref": "MCR-%s"}
                """.formatted(key(), suffix)), "CROSS_PROJECT_FORBIDDEN");
        denied(postJson("/api/v1/finance/refunds/" + refundB + "/query", """
                {"idempotency_key": "%s"}
                """.formatted(key())), "CROSS_PROJECT_FORBIDDEN");
    }

    // 员工系列：排班、考勤、请假、派驻记录、薪资单列表只按商户；B 排班的历史与顶班、B 员工的扣罚申诉可读。
    @Test
    void staffListsAndReadDetailsFollowMerchantAcrossOperatingProjects() throws Exception {
        String eA = employee("ea", projectA, merchantM);
        String eB = employee("eb", projectB, merchantM);
        String eN = employee("en", projectB, merchantN);
        staffRows(eA, projectA);
        String shiftB = staffRows(eB, projectB);
        staffRows(eN, projectB);
        String dispatchB = dispatch("b", eB, merchantM, projectB);
        dispatch("n", eN, merchantN, projectB);
        String statementA = payrollStatement("a", eA, projectA, merchantM);
        String statementB = payrollStatement("b", eB, projectB, merchantM);
        payrollStatement("n", eN, projectB, merchantN);

        // 修复前红：只出 A 的员工
        for (String url : List.of("/api/v1/staff/shifts?date=2099-03-17", "/api/v1/staff/attendance?date=2099-03-17",
                "/api/v1/staff/leaves?status=pending")) {
            assertThat(mine(url, "$.data.items[*].user_id")).as(url).containsExactlyInAnyOrder(eA, eB);
        }
        assertThat(mine("/api/v1/staff/dispatches", "$.data.items[*].id")).containsExactly(dispatchB);
        assertThat(mine("/api/v1/staff/payroll?month=2099-03", "$.data.items[*].id"))
                .containsExactlyInAnyOrder(statementA, statementB);
        mockMvc.perform(get("/api/v1/staff/shifts/" + shiftB + "/history").with(merchantAccount)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/staff/shifts/" + shiftB + "/covers").with(merchantAccount)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/staff/payroll/adjustments/appeals?user_id=" + eB).with(merchantAccount))
                .andExpect(status().isOk());
        // 别的商户的员工照旧不可读
        denied(mockMvc.perform(get("/api/v1/staff/payroll/adjustments/appeals?user_id=" + eN).with(merchantAccount)),
                "CROSS_MERCHANT_FORBIDDEN");
    }

    // ── 夹具 ───────────────────────────────────────────────────────────────────────────────

    private String userId(String tag) {
        return "usr_mcr_" + tag + "_" + suffix;
    }

    private static String key() {
        return "mcr-" + UUID.randomUUID();
    }

    /** 建一名账号、挂上给定角色，返回以其身份登录的会话（后台 Web 终端＋生产过滤器放入的 AccessActor 主体，四项与账号行一致）。 */
    private RequestPostProcessor account(String tag, String organizationId, String merchantId, String... roleIds) {
        String userId = userId(tag);
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

    /** 商户员工：账号归属＝登记项目，一行已批准的档案（登记项目、商户按给定，没有所属公司）。返回账号编号。 */
    private String employee(String tag, String projectId, String merchantId) {
        account(tag, projectId, merchantId);
        jdbcTemplate.update("""
                INSERT INTO staff_profiles (id, project_id, merchant_id, user_id, name, registration_status, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'M20R3', 'approved', 'active', now(), now())
                """, "stf_mcr_" + tag + "_" + suffix, projectId, merchantId, userId(tag));
        return userId(tag);
    }

    /** 2099-03-17 的一个班次、一次打卡和一条待批请假，各表自己的 project_id 填 rowProject。返回班次编号。 */
    private String staffRows(String userId, String rowProject) {
        String shiftId = "shf_" + userId;
        jdbcTemplate.update("""
                INSERT INTO staff_shift_schedules (id, user_id, work_date, start_at, end_at, created_by, created_at, updated_at, project_id)
                VALUES (?, ?, DATE '2099-03-17', TIMESTAMPTZ '2099-03-17 09:00:00+08', TIMESTAMPTZ '2099-03-17 17:00:00+08',
                        'usr_mcr', now(), now(), ?)
                """, shiftId, userId, rowProject);
        jdbcTemplate.update("""
                INSERT INTO staff_attendance_records (id, user_id, punch_type, punched_at, method, judgement, created_at, project_id)
                VALUES (?, ?, 'check_in', TIMESTAMPTZ '2099-03-17 12:00:00+08', 'qr', 'normal', now(), ?)
                """, "att_" + userId, userId, rowProject);
        jdbcTemplate.update("""
                INSERT INTO staff_leave_requests (id, user_id, leave_type, start_date, end_date, reason, status, created_at, updated_at, project_id)
                VALUES (?, ?, 'annual', DATE '2099-03-18', DATE '2099-03-18', 'M20R3', 'pending', TIMESTAMPTZ '2099-03-17 00:00:00+00',
                        now(), ?)
                """, "lv_" + userId, userId, rowProject);
        return shiftId;
    }

    private String dispatch(String tag, String userId, String merchantId, String projectId) {
        String id = "dsp_mcr_" + tag + "_" + suffix;
        jdbcTemplate.update("INSERT INTO staff_dispatch_records (id, user_id, company_id, merchant_id, project_id, start_date, status) "
                + "VALUES (?, ?, ?, ?, ?, DATE '2099-03-01', 'active')", id, userId, projectId, merchantId, projectId);
        return id;
    }

    /** 2099-03 已定格的工资单（发薪主体取登记项目）。 */
    private String payrollStatement(String tag, String userId, String projectId, String merchantId) {
        String id = "pst_mcr_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO staff_payroll_statements (id, user_id, month, base_cent, total_cent, status, project_id, payer_company_id, merchant_id)
                VALUES (?, ?, '2099-03', 500000, 500000, 'closed', ?, ?, ?)
                """, id, userId, projectId, projectId, merchantId);
        return id;
    }

    /** 一笔对商户的应收（来源按给定，未收）。 */
    private String arApItem(String tag, String merchantId, String projectId, String sourceType) {
        String id = "arap_mcr_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent,
                    unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at, merchant_id, project_id)
                VALUES (?, ?, 'receivable', ?, ?, ?, 5000, 0, 5000, 'CNY', DATE '2099-03-31', '2099-03', 'pending_payment', now(), now(), ?, ?)
                """, id, id, merchantId, sourceType, id, merchantId, projectId);
        return id;
    }

    private String refund(String tag, String merchantId, String projectId) {
        String id = "rf_mcr_" + tag + "_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO finance_refunds (id, target_id, refund_amount_cent, reason, refund_channel, refund_direction, status, created_at,
                    source_type, merchant_id, project_id, requested_by, original_payment_ref, updated_at)
                VALUES (?, ?, 3000, 'M20R3 原路退回', 'wechat', 'original', 'failed', now(), 'ar_ap_payment', ?, ?, 'usr_mcr', 'orig-mcr', now())
                """, id, "target_" + id, merchantId, projectId);
        return id;
    }

    /** 商户专区「门票分账」：平台向商户 m 的门票分销内部应付，往来对方所属项目 B，已可查看。 */
    private String ticketSettlement(String projectId) {
        String id = "arap_mcr_ticket_" + suffix;
        jdbcTemplate.update("""
                INSERT INTO ar_ap_items (id, item_no, direction, counterparty_id, source_type, source_id, amount_cent, paid_amount_cent,
                    unpaid_amount_cent, currency, due_date, accounting_period, status, created_at, updated_at, merchant_id, project_id)
                VALUES (?, ?, 'payable', ?, 'ticket_distribution', ?, 5000, 0, 5000, 'CNY', DATE '2099-03-31', '2099-03', 'pending_payment',
                        now(), now(), NULL, ?)
                """, id, id, merchantM, id, projectId);
        jdbcTemplate.update("""
                INSERT INTO internal_payable_schedules (ar_ap_item_id, route, project_id, counterparty_id, billing_period_start, visible_at,
                    objection_deadline, designated_pay_day, scheduled_due_date, pay_days_rule_id, route_rule_id, objection_working_days)
                VALUES (?, 'ticket_distribution', ?, ?, DATE '2099-02-01', now() - interval '1 day', DATE '2099-03-20', DATE '2099-03-31',
                        DATE '2099-03-31', 'ipdr_initial_pay_days', 'ipdr_fixture_ticket_distribution', 10)
                """, id, projectId, merchantM);
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

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(merchantAccount).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static void denied(ResultActions result, String detailCode) throws Exception {
        result.andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value(detailCode));
    }
}
