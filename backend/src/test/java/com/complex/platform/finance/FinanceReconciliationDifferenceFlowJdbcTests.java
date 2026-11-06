package com.complex.platform.finance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.LocalZone;
import com.complex.platform.support.DailyCloseFixture;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 Y 段第 181 条 181-R1（ENUM:191「差异处理状态 待识别、待处理、待复核、已关闭。……复核环节属业务审批，沿用平台治理地图「全平台审批通用时限」条。
 * 各阶段由财务岗全程负责。」；BSM:1729「对不上的进差异处理，按原因定位、复核后处理」；ENUM:202「复核人须比对原件与录入值后签字；无原件的录入不得进入
 * 对账结论，只能挂为待补凭证。」；PGM:528「任一节点驳回则整单退回起草人」；PGM:529「申请人与直接受益人不得审批与自己相关的申请」）：
 * 财务对账差异四态，关闭复核走统一审批。生成日结、上传原件、登记、识别、提交复核、读复核单、审批、确认日结都按后台真实请求形状（蛇形键）走真实入口与授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FinanceReconciliationDifferenceFlowJdbcTests {

    private static final String CAUSE = "渠道对账单少记一笔团购核销";
    private static final String CONCLUSION = "以平台口径为准，渠道补记后差额消除";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private FinanceRepository financeRepository;

    private final String s = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "frf_pa_" + s;
    private final String projectB = "frf_pb_" + s;
    private final String barA = "frf_ma_" + s;
    private final String barB = "frf_mb_" + s;
    private final String adminId = "usr_frf_admin_" + s;
    private final String approverId = "usr_frf_appr_" + s;
    private final LocalDate day = LocalDate.now(LocalZone.ID).minusDays(3);
    private RequestPostProcessor admin;
    private RequestPostProcessor approver;
    private RequestPostProcessor viewerA;
    private RequestPostProcessor viewerB;
    private RequestPostProcessor financeB;

    @BeforeEach
    void seed() {
        for (String[] pm : new String[][] {{projectA, barA}, {projectB, barB}}) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','对账差异四态项目','active',now(),now())",
                    pm[0]);
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,project_id)"
                    + " VALUES(?,'对账差异四态酒吧','bar','active',now(),now(),?)", pm[1], pm[0]);
        }
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        // 登记人、识别人、提交人：本人项目为 A，差异在 B 项目（复核单按对象项目判可见，不按发起人项目）。
        admin = staffs.staff(adminId, "platform_admin", projectA, null,
                "finance.daily_close.lock", "finance.daily_close.manage", "finance.reconciliation.view",
                "finance.reconciliation.difference.create", "finance.reconciliation.resolve", "evidence.attachment.create",
                "governance.supervisor_review.view", "governance.supervisor_review.approve");
        approver = staffs.staff(approverId, "staff", projectB, null,
                "governance.supervisor_review.view", "governance.supervisor_review.approve", "evidence.timeline.view");
        viewerA = staffs.staff("usr_frf_va_" + s, "staff", projectA, null, "governance.supervisor_review.view");
        viewerB = staffs.staff("usr_frf_vb_" + s, "staff", projectB, null, "governance.supervisor_review.view");
        financeB = staffs.staff("usr_frf_fb_" + s, "staff", projectB, null,
                "finance.reconciliation.view", "finance.reconciliation.resolve");
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", adminId, approverId);
    }

    /**
     * ① 真实流程：人工登记（带原件、高价值）进待识别、负责人＝登记人；识别转待处理；提交复核转待复核（有复核单号）、日结仍拦；
     * 复核单对差异所属项目的普通查看人可见、对别的项目不可见；提交人本人审批被拒；另一财务通过后差异关闭（终审人），日结确认成功。
     */
    @Test
    void differenceGoesThroughFourStatesAndClosesOnlyAfterApproval() throws Exception {
        String close = generate(barB);
        String diff = idOf(register("main", batchOf(barB), paidPayment("p1", barB, projectB, 700_000L), 100_000L, List.of(upload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_identification"))
                .andExpect(jsonPath("$.data.responsible_user_id").value(adminId))
                .andExpect(jsonPath("$.data.stage_entered_at").isNotEmpty()));

        identify(admin, diff, "main").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_handling"))
                .andExpect(jsonPath("$.data.identified_cause").value(CAUSE))
                .andExpect(jsonPath("$.data.responsible_user_id").value(adminId));
        String reviewId = JsonPath.read(submit(admin, diff, "main", List.of(), approverId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andReturn().getResponse().getContentAsString(), "$.data.review_id");
        assertThat(reviewId).isNotBlank();
        confirm(close, "c1").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DAY_CLOSE_DIFFERENCE_PENDING"));

        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + reviewId).with(viewerB))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.object_id").value(diff));
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + reviewId).with(viewerA))
                .andExpect(status().isForbidden());

        approve(admin, reviewId, "self", "approved").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        approve(approver, reviewId, "main", "approved").andExpect(status().isOk());

        Map<String, Object> row = row(diff);
        assertThat(row).containsEntry("status", "closed").containsEntry("closed_by", approverId)
                .containsEntry("close_reason", CONCLUSION);
        confirm(close, "c2").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("confirmed"));
    }

    /** ② 驳回：退回待处理、复核单号置空，审计带驳回意见；可再提交复核。 */
    @Test
    void rejectedReviewReturnsToHandlingAndCanBeResubmitted() throws Exception {
        generate(barB);
        String diff = idOf(register("rej", batchOf(barB), paidPayment("p2", barB, projectB, 100L), 70L, List.of(upload()))
                .andExpect(status().isOk()));
        identify(admin, diff, "rej").andExpect(status().isOk());
        String reviewId = JsonPath.read(submit(admin, diff, "rej1", List.of(), null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.review_id");

        approve(approver, reviewId, "rej", "rejected").andExpect(status().isOk());

        Map<String, Object> row = row(diff);
        assertThat(row.get("status")).isEqualTo("pending_handling");
        assertThat(row.get("review_id")).isNull();
        assertThat(jdbc.queryForObject("SELECT metadata->>'comment' FROM audit_events WHERE event_name = 'finance.reconciliation.difference_review_rejected'"
                + " AND business_id = ?", String.class, diff)).isEqualTo("处理结论与原件不符");
        submit(admin, diff, "rej2", List.of(), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"));
    }

    /** ③ 待补凭证：人工登记不带原件，识别后提交复核也不带 → 409；提交时补上原件 → 待复核，原件并入差异。 */
    @Test
    void manualDifferenceWithoutStatementCannotEnterReview() throws Exception {
        generate(barB);
        String diff = idOf(register("noatt", batchOf(barB), paidPayment("p3", barB, projectB, 100L), 70L, List.of())
                .andExpect(status().isOk()));
        identify(admin, diff, "noatt").andExpect(status().isOk());

        submit(admin, diff, "noatt1", List.of(), null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("RECONCILIATION_EVIDENCE_PENDING"));
        String attachment = upload();
        submit(admin, diff, "noatt2", List.of(attachment), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andExpect(jsonPath("$.data.evidence_attachment_ids[0]").value(attachment));
    }

    /** ④ 越级：待识别直接提交复核 → 409；已关闭再识别 → 409。 */
    @Test
    void stepsOutOfOrderAreRejected() throws Exception {
        generate(barB);
        String diff = idOf(register("order", batchOf(barB), paidPayment("p4", barB, projectB, 100L), 70L, List.of(upload()))
                .andExpect(status().isOk()));

        submit(admin, diff, "early", List.of(), null).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("RECONCILIATION_DIFFERENCE_STATUS_CONFLICT"));
        identify(admin, diff, "order").andExpect(status().isOk());
        String reviewId = JsonPath.read(submit(admin, diff, "order", List.of(), null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.review_id");
        approve(approver, reviewId, "order", "approved").andExpect(status().isOk());

        identify(admin, diff, "late").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("RECONCILIATION_DIFFERENCE_STATUS_CONFLICT"));
    }

    /** ⑤ 范围：项目 B 的财务账号识别项目 A 的差异 → 403。 */
    @Test
    void otherProjectAccountCannotIdentify() throws Exception {
        generate(barA);
        String diff = idOf(register("scope", batchOf(barA), paidPayment("p5", barA, projectA, 100L), 70L, List.of(upload()))
                .andExpect(status().isOk()));

        identify(financeB, diff, "scope").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(row(diff).get("status")).isEqualTo("pending_identification");
    }

    /**
     * ⑥ 平台侧普通财务员工（不属任何商户，负责范围为差异所属综合体）按对账差异页真实请求登记差异、识别、提交复核（PGM:664「跨主体与平台层面的财务工作
     * ——三方对账……由财务中心负责」「单店层面的财务工作——……差异认定……由该酒吧对应的财务 BP 负责」）：写入口只拦商户账号，不只放平台管理员。
     */
    @Test
    void platformStaffFinanceRegistersIdentifiesAndSubmitsReview() throws Exception {
        String staffId = "usr_frf_staff_" + s;
        RequestPostProcessor staffFinance = new StaffRequestFixture(jdbc).staff(staffId, "staff", projectB, null,
                "finance.reconciliation.view", "finance.reconciliation.difference.create", "finance.reconciliation.resolve",
                "evidence.attachment.create");
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", staffId, approverId);
        generate(barB);
        String diff = idOf(register(staffFinance, "staff", batchOf(barB), paidPayment("p6", barB, projectB, 100L), 70L,
                        List.of(upload(staffFinance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_identification"))
                .andExpect(jsonPath("$.data.responsible_user_id").value(staffId)));

        identify(staffFinance, diff, "staff").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_handling"));
        submit(staffFinance, diff, "staff", List.of(), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"));
    }

    /**
     * ⑦ 18Y 复核修复（ENUM:202「复核人须比对原件与录入值后签字」）：平台侧普通财务员工登记、提交复核所附的渠道对账单原件，差异关闭复核的上级
     * （平台侧普通员工，不带商户号、不持对账查看）按审批链页「查看原始凭证」的真实请求回读，得 200。
     */
    @Test
    void reviewSuperiorReadsStatementOriginal() throws Exception {
        String attachment = statementUnderReview("sup");

        mockMvc.perform(get("/api/v1/evidence/attachments/" + attachment + "/file").with(approver))
                .andExpect(status().isOk());
    }

    /** ⑧ 别的项目的员工（持对账查看与证据查看，可触及的项目不含差异所属项目）回读同一原件，得 403。 */
    @Test
    void otherProjectStaffCannotReadStatementOriginal() throws Exception {
        String attachment = statementUnderReview("oth");
        RequestPostProcessor other = new StaffRequestFixture(jdbc).staff("usr_frf_oth_" + s, "staff", projectA, null,
                "finance.reconciliation.view", "evidence.timeline.view");

        mockMvc.perform(get("/api/v1/evidence/attachments/" + attachment + "/file").with(other))
                .andExpect(status().isForbidden());
    }

    // ---------- helpers ----------

    /** 后台日结区块生成该酒吧该营业日日结（随之生成对账批次，180-R1），补齐五类最终标记（143-R2 前提），返回日结 ID。 */
    private String generate(String bar) throws Exception {
        String body = mockMvc.perform(post("/api/v1/finance/daily-closes").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "frf-gen-" + bar,
                                "business_date", day.toString(), "scope_type", "merchant", "scope_id", bar))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.data.id");
        DailyCloseFixture.markAllFinal(financeRepository, id);
        return id;
    }

    /** 对账差异页读批次的真实请求，取该酒吧范围的批次 ID。 */
    private String batchOf(String bar) throws Exception {
        String body = mockMvc.perform(get("/api/v1/finance/reconciliation/batches").with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> ids = JsonPath.read(body, "$.data.items[?(@.scope_id == '" + bar + "')].id");
        assertThat(ids).hasSize(1);
        return ids.get(0);
    }

    /** 对账差异页「登记差异」表单的真实请求（source 固定人工录入）。 */
    private ResultActions register(String tag, String batchId, String businessRef, long actualCent, List<String> attachments) throws Exception {
        return register(admin, tag, batchId, businessRef, actualCent, attachments);
    }

    private ResultActions register(RequestPostProcessor who, String tag, String batchId, String businessRef, long actualCent,
            List<String> attachments) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/reconciliation/differences").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "frf-diff-" + tag + "-" + s, "batch_id", batchId,
                        "difference_type", "amount", "business_ref", businessRef, "source", "manual",
                        "actual_amount_cent", actualCent, "evidence_attachment_ids", attachments))));
    }

    /** 对账差异页选中待识别的行点「识别」的真实请求。 */
    private ResultActions identify(RequestPostProcessor who, String differenceId, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/reconciliation/differences/" + differenceId + "/identify").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "frf-identify-" + tag + "-" + s, "cause_note", CAUSE))));
    }

    /** 对账差异页选中待处理的行点「提交复核」的真实请求（最终审批人可不填）。 */
    private ResultActions submit(RequestPostProcessor who, String differenceId, String tag, List<String> attachments, String finalApprover)
            throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("idempotency_key", "frf-submit-" + tag + "-" + s, "close_reason", CONCLUSION,
                "evidence_attachment_ids", attachments));
        if (finalApprover != null) {
            body.put("final_approver_id", finalApprover);
        }
        return mockMvc.perform(post("/api/v1/finance/reconciliation/differences/" + differenceId + "/close").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 主管复核页审批的真实请求。 */
    private ResultActions approve(RequestPostProcessor who, String reviewId, String tag, String decision) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + reviewId + "/approve").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "frf-approve-" + tag + "-" + s, "decision", decision,
                        "comment", "rejected".equals(decision) ? "处理结论与原件不符" : "已比对原件与录入值"))));
    }

    private ResultActions confirm(String dailyCloseId, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/daily-closes/" + dailyCloseId + "/confirm").with(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "frf-confirm-" + tag + "-" + s))));
    }

    /** 登记表单上传渠道对账单原件（照片），返回附件编号。 */
    private String upload() throws Exception {
        return upload(admin);
    }

    private String upload(RequestPostProcessor who) throws Exception {
        String body = mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "statement.jpg", "image/jpeg", new byte[] {1, 2, 3}))
                        .param("business_type", "finance_reconciliation_difference")
                        .param("attachment_role", "reconciliation_statement").with(who))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.id");
    }

    /** 项目 B 的平台侧普通财务员工（直接上级为 approver）上传原件、登记、识别、提交复核，返回原件附件编号。 */
    private String statementUnderReview(String tag) throws Exception {
        String staffId = "usr_frf_st" + tag + "_" + s;
        RequestPostProcessor staffFinance = new StaffRequestFixture(jdbc).staff(staffId, "staff", projectB, null,
                "finance.reconciliation.view", "finance.reconciliation.difference.create", "finance.reconciliation.resolve",
                "evidence.attachment.create");
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", staffId, approverId);
        generate(barB);
        String attachment = upload(staffFinance);
        String diff = idOf(register(staffFinance, tag, batchOf(barB), paidPayment(tag, barB, projectB, 100L), 70L, List.of(attachment))
                .andExpect(status().isOk()));
        identify(staffFinance, diff, tag).andExpect(status().isOk());
        submit(staffFinance, diff, tag, List.of(), null).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"));
        return attachment;
    }

    /** 前置态：一笔已登记 amountCent 分的收款记录（平台侧系统取数来源），返回收款记录号。 */
    private String paidPayment(String tag, String merchant, String project, long amountCent) {
        String item = "frf_item_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id)
                VALUES(?,?,'receivable',?,'manual',?,?,?,0,'CNY',current_date,'2026-10','settled',now(),now(),?,?)
                """, item, "FRF-" + item, merchant, item, amountCent, amountCent, merchant, project);
        String payment = "frf_pay_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,bank_flow_ref,created_at)
                VALUES(?,?,?,'bank_transfer',now(),'FLOW-'||?,now())
                """, payment, item, amountCent, payment);
        return payment;
    }

    private Map<String, Object> row(String differenceId) {
        return jdbc.queryForMap("SELECT status, review_id, closed_by, close_reason FROM finance_reconciliation_differences WHERE id = ?",
                differenceId);
    }

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.id");
    }
}
