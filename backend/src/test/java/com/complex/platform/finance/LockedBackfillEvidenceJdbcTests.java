package com.complex.platform.finance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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
 * 第十八批 AL 段第 253 条（BSM:1642「记录缺失时允许人工补录，但必须写原因和责任人。……补录必须附原始记录凭证（微信截图、通话录音等）并经审核……涉及金额、
 * 销售归属、提成或责任认定的补录，须由被影响方之外的主管复核。」；第十八批裁决 56「三种锁账补录提交时必须上传原始凭证（银行回单、转账截图、异议处理结论等）
 * 并写责任人；「业务发生后 3 天内」不套。」）：253-R1 三种锁账补录不附凭证、凭证不存在都拒，凭证随补录落库并随复核单给审批人，无商户号的项目财务能上传、
 * 审批链上的审批人能回读，链外又不可触及该日结的不能读；该业务类型只收真实文件。253-R2 三种锁账补录不写责任人、责任人账号不存在都拒，责任人登记为复核单
 * 直接受益人——是申请人直接上级时链上跳过、本人复核 403。上传、申请、审批、回读都按后台真实请求形状（蛇形键）走真实入口与授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class LockedBackfillEvidenceJdbcTests {

    private static final List<String> KINDS = List.of("amount", "payment", "refund");

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "lbe_p_" + s;
    private final String otherProject = "lbe_q_" + s;
    private final String shop = "lbe_m_" + s;
    private final String financeId = "usr_lbe_f_" + s;
    private final String s1 = "usr_lbe_s1_" + s;
    private final String s2 = "usr_lbe_s2_" + s;
    private final String responsible = "usr_lbe_r_" + s;
    private final String close = "lbe_dc_" + s;
    private final String item = "lbe_item_" + s;
    private RequestPostProcessor finance;
    private RequestPostProcessor approver1;
    private RequestPostProcessor outsider;

    @BeforeEach
    void seed() {
        for (String p : List.of(project, otherProject)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','锁账补录凭证项目','active',now(),now())", p);
        }
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,project_id)"
                + " VALUES(?,'锁账补录凭证餐饮','catering','active',now(),now(),?)", shop, project);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        finance = staffs.staff(financeId, "staff", project, null, "finance.locked_backfill.request", "finance.daily_close.manage",
                "evidence.attachment.create");
        approver1 = staffs.staff(s1, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve",
                "evidence.timeline.view");
        staffs.staff(s2, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        outsider = staffs.staff("usr_lbe_o_" + s, "staff", otherProject, null, "evidence.timeline.view", "finance.daily_close.manage");
        staffs.staff(responsible, "staff", project, null);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", financeId, s1);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", s1, s2);
        jdbc.update("INSERT INTO finance_daily_closes(id,business_date,scope_type,scope_id,status,locked_at,created_at)"
                + " VALUES(?,current_date - 3,'project',?,'locked',now(),now())", close, project);
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id,locked_day_close_id)
                VALUES(?,?,'receivable',?,'manual',?,10000,1000,9000,'CNY',current_date - 3,'2026-11','partial_paid',now(),now(),?,?,?)
                """, item, "LBE-" + item, shop, item, shop, project, close);
        jdbc.update("""
                INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,bank_flow_ref,created_at)
                VALUES(?,?,1000,'bank_transfer',now() - interval '4 days','FLOW-'||?,now() - interval '4 days')
                """, "lbe_pay_" + s, item, item);
    }

    /**
     * 253-R1 ①：无商户号的项目财务上传银行回单 pdf，补录带上该附件编号 200；按日结查补录带回凭证与责任人，复核单凭证相同、直接受益人为责任人；
     * 审批链第一节点 S1（无商户号）可回读原始凭证，他项目、不在链上的无商户号账号 403。
     */
    @Test
    void uploadedEvidenceIsKeptOnTheBackfillAndReadableAlongTheChain() throws Exception {
        String evidence = upload();
        String body = request("ok", "payment", List.of(evidence), responsible).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidence_attachment_ids[0]").value(evidence))
                .andExpect(jsonPath("$.data.responsible_staff_id").value(responsible))
                .andReturn().getResponse().getContentAsString();
        String adjustment = JsonPath.read(body, "$.data.id");
        String reviewId = JsonPath.read(body, "$.data.review_id");

        String listed = mockMvc.perform(get("/api/v1/finance/daily-closes/" + close + "/adjustments").with(finance))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<List<String>> evidenceListed = JsonPath.read(listed, "$.data.items[?(@.id == '" + adjustment + "')].evidence_attachment_ids");
        assertThat(evidenceListed).containsExactly(List.of(evidence));
        List<String> responsibleListed = JsonPath.read(listed, "$.data.items[?(@.id == '" + adjustment + "')].responsible_staff_id");
        assertThat(responsibleListed).containsExactly(responsible);
        Map<String, Object> review = jdbc.queryForMap(
                "SELECT evidence_attachment_ids::text AS evidence, beneficiary_id FROM supervisor_review_decisions WHERE review_id = ?", reviewId);
        assertThat(JsonPath.<List<String>>read((String) review.get("evidence"), "$")).containsExactly(evidence);
        assertThat(review).containsEntry("beneficiary_id", responsible);

        mockMvc.perform(get("/api/v1/evidence/attachments/" + evidence + "/file").with(approver1)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/evidence/attachments/" + evidence + "/file").with(outsider)).andExpect(status().isForbidden());
    }

    /** 253-R1 ②：三种补录不附凭证各 400 MANUAL_BACKFILL_EVIDENCE_REQUIRED，不建补录、不建复核单。 */
    @Test
    void everyKindWithoutEvidenceIsRejected() throws Exception {
        for (String kind : KINDS) {
            request("none-" + kind, kind, List.of(), responsible).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail_code").value("MANUAL_BACKFILL_EVIDENCE_REQUIRED"));
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_adjustments WHERE ar_ap_item_id = ?", Integer.class, item)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM supervisor_review_decisions WHERE initiated_by = ?", Integer.class, financeId))
                .isZero();
    }

    /** 253-R1 ③：带不存在的附件编号 400 EVIDENCE_ATTACHMENT_NOT_FOUND。 */
    @Test
    void unknownEvidenceIsRejected() throws Exception {
        request("ghost", "amount", List.of("att_lbe_none_" + s), responsible).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("EVIDENCE_ATTACHMENT_NOT_FOUND"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_adjustments WHERE ar_ap_item_id = ?", Integer.class, item)).isZero();
    }

    /** 253-R2 ①：三种补录不写责任人各 400 VALIDATION_FAILED；责任人账号不存在 400「责任人账号不存在」；都不建补录。 */
    @Test
    void everyKindNeedsAnExistingResponsibleAccount() throws Exception {
        String evidence = upload();
        for (String kind : KINDS) {
            request("nobody-" + kind, kind, List.of(evidence), null).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        }
        request("ghost-staff", "amount", List.of(evidence), "usr_lbe_none_" + s).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("责任人账号不存在"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_adjustments WHERE ar_ap_item_id = ?", Integer.class, item)).isZero();
    }

    /** 253-R2 ②：责任人是申请人直接上级 S1 时，主线不含 S1、第一节点为 S1 的上级 S2；S1 复核 403 SUPERVISOR_SELF_REVIEW_FORBIDDEN。 */
    @Test
    void responsibleSuperiorIsSkippedAndCannotReview() throws Exception {
        String body = request("s1", "amount", List.of(upload()), s1).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String reviewId = JsonPath.read(body, "$.data.review_id");

        assertThat(jdbc.queryForList("SELECT approver_id FROM supervisor_review_nodes WHERE review_id = ? AND line_code = 'main' ORDER BY seq",
                String.class, reviewId)).containsExactly(s2);
        mockMvc.perform(post("/api/v1/platform/supervisor-review/" + reviewId + "/approve").with(approver1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "lbe-apv-" + s, "decision", "approved", "comment", "自己相关"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
    }

    /** 253-R1 ④：锁账补录凭证只收真实文件，登记上传意向 400 REAL_UPLOAD_REQUIRED。 */
    @Test
    void uploadIntentIsRejectedForLockedBackfillEvidence() throws Exception {
        mockMvc.perform(post("/api/v1/evidence/attachments/upload-intents").with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "lbe-intent-" + s, "file_name", "bank-receipt.pdf",
                                "content_type", "application/pdf", "file_size", 1024, "business_type", "finance_locked_backfill"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("REAL_UPLOAD_REQUIRED"));
    }

    // ---------- helpers ----------

    /** 日结区块「本日结锁定的台账」行提交补录的真实请求；责任人为空即不带该键。 */
    private ResultActions request(String tag, String kind, List<String> evidence, String responsibleStaff) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("idempotency_key", "lbe-adj-" + tag + "-" + s, "kind", kind, "ar_ap_item_id", item,
                "adjustment_amount_cent", "amount".equals(kind) ? 500L : 300L, "reason", "银行回单到账后补录", "evidence_attachment_ids", evidence));
        if ("payment".equals(kind)) {
            body.put("payment_method", "bank_transfer");
        }
        if (responsibleStaff != null) {
            body.put("responsible_staff_id", responsibleStaff);
        }
        return mockMvc.perform(post("/api/v1/finance/ar-ap-adjustments").with(finance)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 无商户号的项目财务在补录区块上传原始凭证（银行回单 pdf），返回附件编号。 */
    private String upload() throws Exception {
        return JsonPath.read(mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "bank-receipt.pdf", "application/pdf",
                                "%PDF-1.4 locked backfill receipt".getBytes(StandardCharsets.UTF_8)))
                        .param("business_type", "finance_locked_backfill").with(finance))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
    }
}
