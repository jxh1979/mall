package com.complex.platform.finance;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AM 段第 241 条 241-R4、241-R5（BSM:1803「付款确认按全平台统一的审批模型执行——发起时指定这条主线的最终审批人……财务、法务及相关部门作为
 * 平行的相关部门线……主线与各条平行线都通过才完成付款确认；具体流程按支付类型分别设定。」；第十八批裁决 55「所有应付都先走付款确认……退款、工资、货款各类
 * 都要设流程」、65（工资 staff_payroll，财务退款待办按来源 finance_refund_＋来源）、66「每类必带财务、法务两线」、68「工资、退款、赔付按单发起」；
 * PGM:529「申请人与直接受益人不得审批与自己相关的申请」）。工资在准备银行付款前、财务退款在执行与重试前须有已通过的付款确认，未确认的退款不派发网关。
 * 发起、审批（主管复核接口）、出款都按后台真实请求形状（蛇形键）走真实入口与授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class PaymentConfirmationPayrollRefundJdbcTests {

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jCYQAAAAASUVORK5CYII=");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private FinancePayrollService payroll;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "pcp_p_" + s;
    private final String otherProject = "pcp_p2_" + s;
    private final String financeId = "usr_pcp_f_" + s;
    private final String verifierId = "usr_pcp_v_" + s;
    private final String employeeId = "usr_pcp_e_" + s;
    private final String mainId = "usr_pcp_s1_" + s;
    private final String finLineId = "usr_pcp_fl_" + s;
    private final String legalLineId = "usr_pcp_ll_" + s;
    private RequestPostProcessor finance;
    private RequestPostProcessor verifier;
    private RequestPostProcessor employee;
    private RequestPostProcessor main;
    private RequestPostProcessor finLine;
    private RequestPostProcessor legalLine;
    private RequestPostProcessor registerOnly;
    private RequestPostProcessor otherPayrollViewer;
    private RequestPostProcessor platformFinance;

    @BeforeEach
    void seed() {
        for (String p : List.of(project, otherProject)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','付款确认出款项目','active',now(),now())", p);
        }
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        finance = staffs.staff(financeId, "staff", project, null, FinancePayrollService.PAY, FinancePayrollService.VIEW,
                "finance.refund.create", "finance.payment.register", "finance.ar_ap.view", "evidence.attachment.create",
                "governance.supervisor_review.view", "governance.supervisor_review.approve");
        verifier = staffs.staff(verifierId, "staff", project, null, "finance.refund.create", "finance.payment.register", "finance.ar_ap.view",
                "evidence.attachment.create");
        employee = staffs.staff(employeeId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        main = staffs.staff(mainId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        finLine = staffs.staff(finLineId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        legalLine = staffs.staff(legalLineId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        registerOnly = staffs.staff("usr_pcp_ro_" + s, "staff", project, null, "finance.payment.register", "finance.ar_ap.view");
        otherPayrollViewer = staffs.staff("usr_pcp_ov_" + s, "staff", otherProject, null, FinancePayrollService.VIEW);
        // 本类非原收款来源用例沿用平台财务上传凭证；项目财务无商户编号时按退款所属项目办理（第十八批 AV 段 316-R1）。
        platformFinance = staffs.staff("usr_pcp_pf_" + s, "platform_admin", null, null, "finance.refund.create", "finance.ar_ap.view",
                "evidence.attachment.create");
        for (String user : List.of(financeId, verifierId, "usr_pcp_pf_" + s)) {
            jdbc.update("INSERT INTO refund_authority_limits(id,subject_type,subject_id,refund_domain,limit_amount_cent,project_id,status)"
                    + " VALUES(?,'staff',?,'finance',1000000,NULL,'active')", "ral_" + user, user);
        }
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 241-R4：工资审批、定格得到工资应付后，准备银行付款 409；只持通用登记付款权限的账号走工资入口发起 403；员工在发起人上级链上时，把他指定为主线
     * 最终审批人 400；逐人发起 200（审批类型 finance.payment_confirm.staff_payroll、受益人为该员工），员工本人审批 403；各线通过后部分金额准备、
     * 登记银行发起与失败、失败后重新准备、成功入账直至结清都照常；他项目的工资查看人查不到这张确认。
     */
    @Test
    void payrollBankPaymentWaitsForPerEmployeeConfirmation() throws Exception {
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", financeId, employeeId);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", employeeId, mainId);
        String wage = wage();
        String payable = jdbc.queryForObject("SELECT ar_ap_item_id FROM staff_payroll_statements WHERE id = ?", String.class, wage);

        prepare(wage, "w0", 480_000L).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));
        payrollConfirm(registerOnly, wage, "w-ro", mainId).andExpect(status().isForbidden());
        payrollConfirm(finance, wage, "w-emp", employeeId).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("FINAL_APPROVER_NOT_IN_CHAIN"));
        String reviewId = reviewOf(payrollConfirm(finance, wage, "w", mainId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_type").value("staff_payroll"))
                .andExpect(jsonPath("$.data.object_type").value("ar_ap_item"))
                .andExpect(jsonPath("$.data.amount_cent").value(480_000)));

        assertThat(jdbc.queryForMap("SELECT action_type, beneficiary_id FROM supervisor_review_decisions WHERE review_id = ?", reviewId))
                .containsEntry("action_type", "finance.payment_confirm.staff_payroll").containsEntry("beneficiary_id", employeeId);
        approve(employee, reviewId, "w-emp", "approved").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        approveAll(reviewId, "w");
        listed(finance, payable).andExpect(jsonPath("$.data.confirmations[0].status").value("approved"));
        listed(otherPayrollViewer, payable).andExpect(jsonPath("$.data.confirmations.length()").value(0));

        String first = attempt(prepare(wage, "w1", 200_000L).andExpect(status().isOk()));
        bank(first, "w1", "failed");
        String second = attempt(prepare(wage, "w2", 200_000L).andExpect(status().isOk()));
        bank(second, "w2", "succeeded");
        assertThat(ledger(payable)).isEqualTo("480000:200000:280000");
        String third = attempt(prepare(wage, "w3", 280_000L).andExpect(status().isOk()));
        bank(third, "w3", "succeeded");
        assertThat(ledger(payable)).isEqualTo("480000:480000:0");
    }

    /**
     * 241-R1 用例 k（工资部分）：经 241-R4 工资入口为员工工资应付发起的付款确认，查询按工资可见应付过滤——能触及工资单项目与发薪主体的工资查看人
     * 看得到；持 finance.payroll.view 但只触及工资单项目、触及不到发薪主体的账号看不到；只持 finance.ar_ap.view 的账号查工资应付的确认 403。
     */
    @Test
    void payrollConfirmationIsListedOnlyToPayrollViewersReachingThePayer() throws Exception {
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", financeId, mainId);
        jdbc.update("UPDATE roles SET view_all_projects = true WHERE id = ?", "role_" + financeId);
        // 发薪主体为另一个组织：财务可触及全部项目，下面的工资查看人只到工资单项目。
        String wage = wage(otherProject);
        String payable = jdbc.queryForObject("SELECT ar_ap_item_id FROM staff_payroll_statements WHERE id = ?", String.class, wage);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor projectPayrollViewer = staffs.staff("usr_pcp_pv_" + s, "staff", project, null, FinancePayrollService.VIEW);
        RequestPostProcessor ledgerViewer = staffs.staff("usr_pcp_lv_" + s, "staff", project, null, "finance.ar_ap.view");

        payrollConfirm(finance, wage, "k", mainId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_type").value("staff_payroll"));

        listed(finance, payable).andExpect(jsonPath("$.data.confirmations.length()").value(1))
                .andExpect(jsonPath("$.data.confirmations[0].status").value("pending_review"));
        listed(projectPayrollViewer, payable).andExpect(jsonPath("$.data.confirmations.length()").value(0));
        mockMvc.perform(get("/api/v1/finance/payment-confirmations").param("object_type", "ar_ap_item").param("object_id", payable)
                .with(ledgerViewer)).andExpect(status().isForbidden());
    }

    /**
     * 241-R5 (a)：原收款退款建单 200（待退），不派发网关（只有一条待派发的提交尝试，没有查询结果行）；执行与重试都 409；发起付款确认 200（审批类型
     * finance.payment_confirm.finance_refund_ar_ap_payment）；各线通过后登记执行 200、另一人核实成功 200。不开测试事务：执行与重试带准入闸，
     * 前一笔受闸调用失败会把测试事务标为只回滚，后一笔的准入求值即按 fail-closed 拦下（与线上每个请求各自一个事务不同）。
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void financeRefundIsNeitherDispatchedNorExecutedBeforeConfirmation() throws Exception {
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", financeId, mainId);
        String payment = paidReceivable("r1");
        String refund = JsonPath.read(mockMvc.perform(post("/api/v1/finance/refunds").with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "pcp-rf-r1-" + s, "target_id", payment,
                                "refund_amount_cent", 6_000L, "reason", "客人申请退款", "refund_direction", "original"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("requested"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");

        assertThat(jdbc.queryForList("SELECT operation || ':' || coalesce(dispatch_state, '') FROM finance_refund_attempts WHERE refund_id = ?",
                String.class, refund)).containsExactly("submit:ready");
        execute(finance, refund, "r1-early").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));
        mockMvc.perform(post("/api/v1/finance/refunds/" + refund + "/retry").with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "pcp-retry-r1-" + s, "operator_note", "重试原路退款"))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));

        String reviewId = reviewOf(refundConfirm(finance, refund, "r1").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_type").value("finance_refund_ar_ap_payment"))
                .andExpect(jsonPath("$.data.amount_cent").value(6_000)));
        assertThat(jdbc.queryForObject("SELECT action_type FROM supervisor_review_decisions WHERE review_id = ?", String.class, reviewId))
                .isEqualTo("finance.payment_confirm.finance_refund_ar_ap_payment");
        // 复核摘要的原路渠道写中文名（PGM:287 界面全中文，枚举取值须为中文）。
        assertThat(jdbc.queryForObject("SELECT reason FROM supervisor_review_decisions WHERE review_id = ?", String.class, reviewId))
                .contains("原路渠道 微信").doesNotContain("wechat");
        approveAll(reviewId, "r1");

        execute(finance, refund, "r1").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("processing"));
        mockMvc.perform(post("/api/v1/finance/refunds/" + refund + "/confirm").with(verifier).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "pcp-cf-r1-" + s, "operator_note", "核对银行回单",
                                "evidence_attachment_ids", List.of(upload(verifier, "finance_refund", refund)), "status", "success",
                                "amount_cent", 6_000L, "payment_reference", "RF-" + refund, "payment_operator_id", financeId))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("success"));
    }

    /**
     * 241-R5 (a)：通用卡储值本金待退、票务拒收待退（含超时未付足取消后已收各笔）、渠道回流拒收待退同样执行前 409，按各自来源发起确认、各线通过后执行 200。
     * 复核摘要的原路渠道写中文名（PGM:287）；渠道回流拒收的原收款渠道是第三方销售渠道码（V382 闭集，如 meituan）。
     * 不开测试事务，理由同上。
     */
    @ParameterizedTest
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @ValueSource(strings = {"stored_value_account", "ticket_payment_rejection", "channel_ticket_rejection"})
    void pendingRefundOfEachSourceNeedsItsOwnConfirmation(String source) throws Exception {
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", "usr_pcp_pf_" + s, mainId);
        String refund = "pcp_rf_" + source + "_" + s;
        String channel = "channel_ticket_rejection".equals(source) ? "meituan" : "wechat";
        jdbc.update("""
                INSERT INTO finance_refunds (id, target_id, refund_amount_cent, reason, refund_channel, refund_direction, status, created_at,
                    source_type, merchant_id, project_id, requested_by, original_payment_ref, updated_at)
                VALUES (?, ?, 5000, '原路待退', ?, 'original', 'requested', now(), ?, NULL, ?, 'system', ?, now())
                """, refund, "target_" + refund, channel, source, project, "orig_" + refund);

        execute(platformFinance, refund, source + "-early").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));
        String reviewId = reviewOf(refundConfirm(platformFinance, refund, source).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_type").value("finance_refund_" + source)));
        assertThat(jdbc.queryForObject("SELECT reason FROM supervisor_review_decisions WHERE review_id = ?", String.class, reviewId))
                .contains("原路渠道 " + ("meituan".equals(channel) ? "美团" : "微信")).doesNotContain(channel);
        approveAll(reviewId, source);
        execute(platformFinance, refund, source).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("processing"));
    }

    // ---------- helpers ----------

    /** 已批准方案、已批准计薪依据的定格工资（48 万分，员工为 employeeId，发薪主体即本项目），经定格事务生成工资应付，返回工资单号。 */
    private String wage() {
        return wage(project);
    }

    /** 同上，发薪主体为 payer。 */
    private String wage(String payer) {
        String scheme = "pcp_scheme_" + s;
        String facts = "pcp_facts_" + s;
        String calendar = "pcp_cal_" + s;
        String wage = "pcp_wage_" + s;
        jdbc.update("""
                INSERT INTO staff_profiles(id,project_id,user_id,name,company_id,registration_status,status,employment_status,created_at,updated_at)
                VALUES(?,?,?,'工资员工',?,'approved','active','employed',now(),now())
                """, "pcp_profile_" + s, project, employeeId, project);
        jdbc.update("""
                INSERT INTO staff_payroll_scheme_versions(id,user_id,project_id,payer_company_id,version,effective_from,position_code,
                 components,worktime_terms,payment_basis,reason,status,created_by,approved_at)
                VALUES(?,?,?,?,'V0.01','2026-01-01','test_position','[]','{}','已核工资支付日期','测试已批准来源','approved',?,now())
                """, scheme, employeeId, project, payer, financeId);
        jdbc.update("""
                INSERT INTO staff_payroll_calendar_versions(id,project_id,version,start_date,end_date,days,source_basis,reason,status,created_by,approved_at)
                VALUES(?,?,'V0.01','2026-01-01','2026-12-31','[]','测试日历','已批准来源','approved',?,now())
                """, calendar, project, financeId);
        jdbc.update("""
                INSERT INTO staff_payroll_period_facts(id,user_id,month,project_id,payer_company_id,scheme_version_id,calendar_version_id,
                 component_facts,cycle_start,cycle_end,cycle_standard_minutes,agreed_due_date,reason,status,created_by,approved_at)
                VALUES(?,?,'2026-01',?,?,?,?,'[]','2026-01-01','2026-01-31',0,'2026-02-05','测试已批准期间','approved',?,now())
                """, facts, employeeId, project, payer, scheme, calendar, financeId);
        jdbc.update("""
                INSERT INTO staff_payroll_statements(id,user_id,month,base_cent,performance_cent,penalty_cent,total_cent,status,
                  project_id,payer_company_id,scheme_version_id,facts_id,calculation_snapshot,payment_due_date,payment_due_basis,statement_kind)
                VALUES(?,?,'2026-01',500000,80000,100000,480000,'closed',?,?,?,?,?::jsonb,'2026-02-05',?::jsonb,'monthly')
                """, wage, employeeId, project, payer, scheme, facts, "{\"gross_cent\":480000}", "{\"approved_facts\":\"" + facts + "\"}");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AccessActor(financeId, financeId, "staff", project, null, "active"), "test", List.of()));
        payroll.ensurePayrollPayable(wage);
        SecurityContextHolder.clearContext();
        return wage;
    }

    /** 工资银行付款面板「准备付款」的真实请求。 */
    private ResultActions prepare(String wage, String tag, long amountCent) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/payroll/" + wage + "/payment-attempts").with(finance).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcp-prep-" + tag + "-" + s, "amount_cent", amountCent))));
    }

    /** 登记银行实际发起，再登记银行结果（成功带银行成功流水号）；两步各上传绑定本笔的银行凭证。 */
    private void bank(String attempt, String tag, String result) throws Exception {
        Instant requestedAt = Instant.now().minusSeconds(120);
        mockMvc.perform(post("/api/v1/finance/payroll/payment-attempts/" + attempt + "/submit").with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "pcp-sub-" + tag + "-" + s, "bank_request_ref", "REQ-" + tag + "-" + s,
                                "requested_at", requestedAt.toString(), "recipient_user_id", employeeId,
                                "evidence_attachment_ids", List.of(upload(finance, "staff_payroll_payment", attempt))))))
                .andExpect(status().isOk());
        Map<String, Object> body = new HashMap<>(Map.of("idempotency_key", "pcp-res-" + tag + "-" + s, "status", result,
                "result_at", requestedAt.plusSeconds(60).toString(), "reason", "已核银行实际结果",
                "evidence_attachment_ids", List.of(upload(finance, "staff_payroll_payment", attempt))));
        if ("succeeded".equals(result)) {
            body.put("bank_flow_ref", "FLOW-" + tag + "-" + s);
        }
        mockMvc.perform(post("/api/v1/finance/payroll/payment-attempts/" + attempt + "/result").with(finance).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(result));
    }

    private static String attempt(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    /** 工资付款面板逐人发起付款确认的真实请求：主线最终审批人与财务线、法务线各自的最终审批人。 */
    private ResultActions payrollConfirm(RequestPostProcessor who, String wage, String tag, String finalApprover) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/payroll/" + wage + "/payment-confirmations").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(confirmation("pcp-pc-" + tag, "一月工资发放", finalApprover))));
    }

    /** 财务退款面板发起付款确认的真实请求。 */
    private ResultActions refundConfirm(RequestPostProcessor who, String refund, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/refunds/" + refund + "/payment-confirmations").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(confirmation("pcp-rc-" + tag, "原路退回客人", mainId))));
    }

    private Map<String, Object> confirmation(String key, String reason, String finalApprover) {
        List<Map<String, String>> lines = new ArrayList<>();
        lines.add(Map.of("line_code", "finance_review", "final_approver_id", finLineId));
        lines.add(Map.of("line_code", "legal_review", "final_approver_id", legalLineId));
        return Map.of("idempotency_key", key + "-" + s, "reason", reason, "final_approver_id", finalApprover, "parallel_lines", lines);
    }

    /** 财务退款面板「登记原路执行」的真实请求（执行流水号为 RF-退款单号，核实时引用）。 */
    private ResultActions execute(RequestPostProcessor who, String refund, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/refunds/" + refund + "/execution").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcp-ex-" + tag + "-" + s, "operator_note", "已沿原路退回",
                        "evidence_attachment_ids", List.of(upload(who, "finance_refund", refund)),
                        "payment_reference", "RF-" + refund))));
    }

    private ResultActions approve(RequestPostProcessor who, String reviewId, String tag, String decision) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + reviewId + "/approve").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcp-apv-" + tag + "-" + s, "decision", decision,
                        "comment", "已核对付款明细"))));
    }

    /** 主线、财务线、法务线各自的最终审批人逐一通过。 */
    private void approveAll(String reviewId, String tag) throws Exception {
        approve(main, reviewId, tag + "-main", "approved").andExpect(status().isOk());
        approve(finLine, reviewId, tag + "-fin", "approved").andExpect(status().isOk());
        approve(legalLine, reviewId, tag + "-legal", "approved").andExpect(status().isOk());
    }

    private static String reviewOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.review_id");
    }

    private ResultActions listed(RequestPostProcessor who, String item) throws Exception {
        return mockMvc.perform(get("/api/v1/finance/payment-confirmations").param("object_type", "ar_ap_item").param("object_id", item).with(who))
                .andExpect(status().isOk());
    }

    /** 上传绑定本笔业务的凭证图片，返回附件编号。 */
    private String upload(RequestPostProcessor who, String businessType, String businessId) throws Exception {
        return JsonPath.read(mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "proof.png", "image/png", PNG))
                        .param("business_type", businessType).param("business_id", businessId).with(who))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
    }

    /** 本项目已收款 100 元的应收及其微信收款记录（有原收款流水），返回收款记录编号。 */
    private String paidReceivable(String tag) {
        String item = "pcp_item_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id)
                VALUES(?,?,'receivable','pcp_customer',?,?,10000,10000,0,'CNY',current_date,'2026-11','settled',now(),now(),NULL,?)
                """, item, "PCP-" + item, "manual", item, project);
        String payment = "pcp_pay_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,bank_flow_ref,created_at)
                VALUES(?,?,10000,'wechat',now(),?,now())
                """, payment, item, "FLOW-" + payment);
        return payment;
    }

    /** 台账总额:已收付:未收付。 */
    private String ledger(String item) {
        return jdbc.queryForObject("SELECT amount_cent || ':' || paid_amount_cent || ':' || unpaid_amount_cent FROM ar_ap_items WHERE id = ?",
                String.class, item);
    }
}
