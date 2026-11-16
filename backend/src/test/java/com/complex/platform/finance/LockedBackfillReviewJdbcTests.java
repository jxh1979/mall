package com.complex.platform.finance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.governance.review.SupervisorReviewService;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
 * 第十八批 AL 段第 245 条 245-R1（ENUM:190「日结锁定后的补录须上级审批」；PGM:528「发起单据时只需指定该事项的最终审批人，系统按组织层级自动补全从发起人到
 * 该最终审批人之间的全部直接上级节点，逐级审批；……任一节点驳回则整单退回起草人。」；PGM:529「涉钱审批出厂默认 1 个工作日……系统自动提醒并升级到该节点的
 * 上一级，不得默认通过，也不得默认拒绝」）：日结锁定后补录（金额调整、收付款补录、退款补录）提交即进统一审批，沿申请人直接上级链到所指定的最终审批人逐级
 * 审批，全部通过才落账，任一节点驳回不改账；按涉钱 1 个工作日到期，超时升级到该节点上一级；旧审批接口与旧审批权限码已退。组织层级 F→S1→S2→S3，F 为无商户号
 * 的项目财务；上传凭证、申请、审批、清单都按后台真实请求形状（蛇形键）走真实入口与授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class LockedBackfillReviewJdbcTests {

    private static final String APPROVE_RETIRED = "finance.locked_backfill.approve";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private SupervisorReviewService reviews;

    private final String s = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "lbr_p_" + s;
    private final String shop = "lbr_m_" + s;
    private final String financeId = "usr_lbr_f_" + s;
    private final String s1 = "usr_lbr_s1_" + s;
    private final String s2 = "usr_lbr_s2_" + s;
    private final String s3 = "usr_lbr_s3_" + s;
    private final String responsible = "usr_lbr_r_" + s;
    private final String close = "lbr_dc_" + s;
    private RequestPostProcessor finance;
    private RequestPostProcessor approver1;
    private RequestPostProcessor approver2;
    private RequestPostProcessor approver3;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','锁账补录审批项目','active',now(),now())",
                project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,project_id)"
                + " VALUES(?,'锁账补录审批餐饮','catering','active',now(),now(),?)", shop, project);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        finance = staffs.staff(financeId, "staff", project, null, "finance.locked_backfill.request", "finance.daily_close.manage",
                "evidence.attachment.create", "governance.supervisor_review.view", "governance.supervisor_review.approve");
        approver1 = staffs.staff(s1, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        approver2 = staffs.staff(s2, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        approver3 = staffs.staff(s3, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        staffs.staff(responsible, "staff", project, null);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", financeId, s1);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", s1, s2);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", s2, s3);
        jdbc.update("INSERT INTO finance_daily_closes(id,business_date,scope_type,scope_id,status,locked_at,created_at)"
                + " VALUES(?,current_date - 3,'project',?,'locked',now(),now())", close, project);
    }

    /**
     * ①③⑤ 收付款补录指定 S3 为最终审批人：复核单动作 finance.locked_backfill.review、对象 ar_ap_adjustment、待复核，主线依次 S1、S2、S3，到期＝发起时刻
     * 加 1 个工作日；S1、S2 通过后补录仍待审、不落收款；S3 终审通过才登记收款（金额与收付方式同申请），批准人记 S3。
     */
    @Test
    void paymentBackfillWalksTheChainToTheNamedFinalApproverBeforeSettling() throws Exception {
        String item = lockedItem("pay");
        Instant before = Instant.now().truncatedTo(ChronoUnit.MICROS);
        String body = request(item, "pay", "payment", 2_000L, s3).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approval_status").value("pending"))
                .andReturn().getResponse().getContentAsString();
        Instant after = Instant.now();
        String adjustment = JsonPath.read(body, "$.data.id");
        String reviewId = JsonPath.read(body, "$.data.review_id");
        assertThat(reviewId).startsWith("review_");
        assertThat(jdbc.queryForMap("SELECT action_type, object_type, object_id, review_status FROM supervisor_review_decisions WHERE review_id = ?",
                reviewId)).containsEntry("action_type", "finance.locked_backfill.review").containsEntry("object_type", "ar_ap_adjustment")
                .containsEntry("object_id", adjustment).containsEntry("review_status", "pending_review");
        assertThat(chain(reviewId)).containsExactly(s1, s2, s3);
        assertThat(dueAt(reviewId)).isBetween(reviews.addWorkingDays(before, 1), reviews.addWorkingDays(after, 1));

        approve(approver1, reviewId, "pay-1", "approved").andExpect(status().isOk());
        approve(approver2, reviewId, "pay-2", "approved").andExpect(status().isOk());
        assertThat(decided(adjustment)).isEqualTo("pending:");
        assertThat(backfillPayments(item)).isEmpty();

        approve(approver3, reviewId, "pay-3", "approved").andExpect(status().isOk());
        assertThat(decided(adjustment)).isEqualTo("approved:" + s3);
        assertThat(backfillPayments(item)).containsExactly("2000:bank_transfer");
        assertThat(ledger(item)).isEqualTo("10000:3000:7000");
    }

    /** ⑤ 金额调整不指定最终审批人（默认直接上级 S1）：S1 通过后红冲原行、追加重述行（原额＋补录额），批准人记 S1。 */
    @Test
    void amountBackfillRestatesTheItemAfterTheDirectSuperiorApproves() throws Exception {
        String item = lockedItem("amt");
        String body = request(item, "amt", "amount", 500L, null).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String adjustment = JsonPath.read(body, "$.data.id");
        String reviewId = JsonPath.read(body, "$.data.review_id");
        assertThat(chain(reviewId)).containsExactly(s1);

        approve(approver1, reviewId, "amt-1", "approved").andExpect(status().isOk());

        assertThat(decided(adjustment)).isEqualTo("approved:" + s1);
        assertThat(jdbc.queryForList("SELECT id || ':' || amount_cent FROM ar_ap_items WHERE origin_item_id = ? ORDER BY id", String.class, item))
                .containsExactly("arap_res_" + adjustment + ":10500", "arap_rev_" + adjustment + ":-10000");
    }

    /** ⑤ 退款补录：终审人在退款额度内，通过后按该台账原收款建原路退款待办，由终审人发起；台账不动。 */
    @Test
    void refundBackfillCreatesOriginalPathRefundTodoAfterApproval() throws Exception {
        String item = lockedItem("ref");
        jdbc.update("INSERT INTO refund_authority_limits (id, subject_type, subject_id, refund_domain, limit_amount_cent, project_id, status)"
                + " VALUES (?, 'staff', ?, 'finance', 1000000, NULL, 'active')", "ral_lbr_" + s, s1);
        String body = request(item, "ref", "refund", 600L, null).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String adjustment = JsonPath.read(body, "$.data.id");

        approve(approver1, JsonPath.read(body, "$.data.review_id"), "ref-1", "approved").andExpect(status().isOk());

        assertThat(decided(adjustment)).isEqualTo("approved:" + s1);
        assertThat(jdbc.queryForList("SELECT refund_amount_cent || ':' || refund_channel || ':' || status || ':' || requested_by FROM finance_refunds"
                + " WHERE target_id = ?", String.class, "lbr_pay_ref_" + s)).containsExactly("600:bank_transfer:requested:" + s1);
        assertThat(ledger(item)).isEqualTo("10000:1000:9000");
    }

    /** ⑦ 任一节点驳回：补录驳回、记驳回人，不落收款、不红冲。 */
    @Test
    void rejectionByAnyNodeLeavesTheLedgerUntouched() throws Exception {
        String item = lockedItem("rej");
        String body = request(item, "rej", "amount", 500L, s3).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String adjustment = JsonPath.read(body, "$.data.id");
        String reviewId = JsonPath.read(body, "$.data.review_id");

        approve(approver1, reviewId, "rej-1", "approved").andExpect(status().isOk());
        approve(approver2, reviewId, "rej-2", "rejected").andExpect(status().isOk());

        assertThat(decided(adjustment)).isEqualTo("rejected:" + s2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_items WHERE origin_item_id = ?", Integer.class, item)).isZero();
        assertThat(ledger(item)).isEqualTo("10000:1000:9000");
    }

    /** ④ 到期未处理：超时扫描把第一节点由 S1 升级为 S1 的上级 S2，整单仍待复核、补录仍待审（不默认通过）。 */
    @Test
    void overdueFirstNodeEscalatesToItsSuperiorWithoutDeciding() throws Exception {
        String item = lockedItem("esc");
        String body = request(item, "esc", "amount", 500L, s3).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String adjustment = JsonPath.read(body, "$.data.id");
        String reviewId = JsonPath.read(body, "$.data.review_id");
        // 超时扫描按到期时刻排序、一次最多 200 张：夹具单排到最前（同 ApprovalDeadlineJdbcIntegrationTests 写法）。
        jdbc.update("UPDATE supervisor_review_decisions SET due_at = TIMESTAMPTZ '2000-01-01 00:00:00+00' WHERE review_id = ?", reviewId);

        reviews.escalateOverdue(Instant.now());

        assertThat(chain(reviewId).get(0)).isEqualTo(s2);
        assertThat(jdbc.queryForObject("SELECT review_status FROM supervisor_review_decisions WHERE review_id = ?", String.class, reviewId))
                .isEqualTo("pending_review");
        assertThat(decided(adjustment)).isEqualTo("pending:");
    }

    /** ⑥ 申请人本人审批 403 SUPERVISOR_SELF_REVIEW_FORBIDDEN，补录仍待审。 */
    @Test
    void requesterCannotApproveOwnBackfill() throws Exception {
        String item = lockedItem("self");
        String body = request(item, "self", "amount", 500L, null).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        approve(finance, JsonPath.read(body, "$.data.review_id"), "self-1", "approved").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        assertThat(decided(JsonPath.read(body, "$.data.id"))).isEqualTo("pending:");
    }

    /** ⑧ 旧审批接口已删：POST …/ar-ap-adjustments/{id}/approve 404，补录仍待审。 */
    @Test
    void oldApproveEndpointIsGone() throws Exception {
        String item = lockedItem("old");
        String adjustment = JsonPath.read(request(item, "old", "amount", 500L, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(post("/api/v1/finance/ar-ap-adjustments/" + adjustment + "/approve").with(approver1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "lbr-old-" + s, "decision", "approve"))))
                .andExpect(status().isNotFound());
        assertThat(decided(adjustment)).isEqualTo("pending:");
    }

    /** ② 审批时限清单有「日结锁定后补录」（涉钱、出厂 1 个工作日），没有已停用的「锁账后补录审批」准入行。 */
    @Test
    void approvalDeadlinesListLockedBackfillReviewAsMoney() throws Exception {
        List<Map<String, Object>> data = JsonPath.read(mockMvc.perform(get("/api/v1/platform/approval-deadlines").with(approver1))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data");
        Map<String, Map<String, Object>> rows = new LinkedHashMap<>();
        data.forEach(row -> rows.put((String) row.get("action_type"), row));

        assertThat(rows.get("finance.locked_backfill.review")).isNotNull()
                .containsEntry("money", true).containsEntry("factory_working_days", 1);
        assertThat(rows).doesNotContainKey("finance.locked_backfill_approve");
    }

    /**
     * ⑨ 旧审批权限码退役、授予来源清空，审批入口的超合同范围准入策略停用。
     * 三张授予表只核 V628 迁移完成前就已存在的来源（迁移负责清的范围）：别的测试夹具在测试期按 LIKE 'finance.%' 新建角色授权时
     * 会连同已退役码一起授入，那些行不是迁移遗留，按 CI 跑序先跑就会误报。
     */
    @Test
    void retiredApprovePermissionKeepsNoGrantSources() {
        assertThat(jdbc.queryForObject("SELECT status FROM permissions WHERE permission_code = ?", String.class, APPROVE_RETIRED))
                .isEqualTo("retired");
        String migrated = "(SELECT installed_on FROM flyway_schema_history WHERE version = '628')";
        assertThat(jdbc.queryForObject("SELECT count(*) FROM role_permissions rp JOIN roles r ON r.id = rp.role_id"
                + " WHERE rp.permission_code = ? AND r.created_at <= " + migrated, Integer.class, APPROVE_RETIRED))
                .as("role_permissions").isZero();
        for (String table : List.of("permission_template_items", "user_permission_overrides")) {
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE permission_code = ? AND created_at <= " + migrated,
                    Integer.class, APPROVE_RETIRED)).as(table).isZero();
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM temporary_permission_grants WHERE permission_code = ? AND status = 'active'",
                Integer.class, APPROVE_RETIRED)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM terminal_capability_switches WHERE capability_code = ?", Integer.class,
                APPROVE_RETIRED)).isZero();
        assertThat(jdbc.queryForObject("SELECT enabled FROM supervisor_review_policies WHERE policy_code = 'finance_locked_approve_over_scope'",
                Boolean.class)).isFalse();
    }

    // ---------- helpers ----------

    /** 本项目商户一笔锁在本项目日结上的应收（10000 分，已按银行转账收 1000 分），返回台账号；收款记录号为 lbr_pay_{tag}_{s}。 */
    private String lockedItem(String tag) {
        String item = "lbr_item_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id,locked_day_close_id)
                VALUES(?,?,'receivable',?,'manual',?,10000,1000,9000,'CNY',current_date - 3,'2026-11','partial_paid',now(),now(),?,?,?)
                """, item, "LBR-" + item, shop, item, shop, project, close);
        jdbc.update("""
                INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,bank_flow_ref,created_at)
                VALUES(?,?,1000,'bank_transfer',now() - interval '4 days','FLOW-'||?,now() - interval '4 days')
                """, "lbr_pay_" + tag + "_" + s, item, item);
        return item;
    }

    /** 日结区块「本日结锁定的台账」行提交补录的真实请求（先上传原始凭证，带责任人；finalApprover 为空即不填）。 */
    private ResultActions request(String item, String tag, String kind, long amountCent, String finalApprover) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("idempotency_key", "lbr-adj-" + tag + "-" + s, "kind", kind, "ar_ap_item_id", item,
                "adjustment_amount_cent", amountCent, "reason", "银行回单到账后补录", "responsible_staff_id", responsible,
                "evidence_attachment_ids", List.of(upload())));
        if ("payment".equals(kind)) {
            body.put("payment_method", "bank_transfer");
        }
        if (finalApprover != null) {
            body.put("final_approver_id", finalApprover);
        }
        return mockMvc.perform(post("/api/v1/finance/ar-ap-adjustments").with(finance)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    /** 补录区块上传原始凭证（银行回单 pdf），返回附件编号。 */
    private String upload() throws Exception {
        return JsonPath.read(mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "bank-receipt.pdf", "application/pdf",
                                "%PDF-1.4 locked backfill receipt".getBytes(StandardCharsets.UTF_8)))
                        .param("business_type", "finance_locked_backfill").with(finance))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
    }

    /** 主管复核页（或审批链页）审批的真实请求。 */
    private ResultActions approve(RequestPostProcessor who, String reviewId, String tag, String decision) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + reviewId + "/approve").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "lbr-apv-" + tag + "-" + s, "decision", decision,
                        "comment", "已核对银行回单"))));
    }

    /** 主线节点审批人（按顺序）。 */
    private List<String> chain(String reviewId) {
        return jdbc.queryForList("SELECT approver_id FROM supervisor_review_nodes WHERE review_id = ? AND line_code = 'main' ORDER BY seq",
                String.class, reviewId);
    }

    private Instant dueAt(String reviewId) {
        return jdbc.queryForObject("SELECT due_at FROM supervisor_review_decisions WHERE review_id = ?", Timestamp.class, reviewId).toInstant();
    }

    /** 补录状态:批准人（未决为空）。 */
    private String decided(String adjustment) {
        return jdbc.queryForObject("SELECT approval_status || ':' || coalesce(approved_by, '') FROM ar_ap_adjustments WHERE id = ?",
                String.class, adjustment);
    }

    /** 该台账经补录登记的收款（金额:收付方式）。 */
    private List<String> backfillPayments(String item) {
        return jdbc.queryForList("SELECT amount_cent || ':' || payment_method FROM ar_ap_payments WHERE ar_ap_item_id = ? AND id LIKE 'arap_pay_%'",
                String.class, item);
    }

    /** 台账总额:已收付:未收付。 */
    private String ledger(String item) {
        return jdbc.queryForObject("SELECT amount_cent || ':' || paid_amount_cent || ':' || unpaid_amount_cent FROM ar_ap_items WHERE id = ?",
                String.class, item);
    }
}
