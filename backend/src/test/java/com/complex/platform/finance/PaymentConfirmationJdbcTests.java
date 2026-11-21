package com.complex.platform.finance;

// 文件版本：V0.05

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AM 段第 241 条 241-R1（BSM:1803「审批通过后的发放进入应付款项台账，走付款确认。付款确认按全平台统一的审批模型执行——发起时指定这条主线的
 * 最终审批人……财务、法务及相关部门作为平行的相关部门线，各指定该部门的最终审批人……主线与各条平行线都通过才完成付款确认；具体流程按支付类型分别设定。」；
 * 第十八批裁决 55「所有应付都先走付款确认」、65「按台账来源逐个分」、66「每类必带财务、法务两线；其他部门线由发起人按单另加」、68「同收款方、同类型、
 * 同综合体可勾选合发」、67「审批线页设定」（241-R8）、69「付款方是平台主体的应收，登记收款前也须付款确认」（241-R9）；PGM:529「涉钱审批出厂默认 1 个工作日」「申请人与直接受益人不得审批与自己相关的申请」）。组织层级 F→S1（主线最终审批人），
 * 财务线、法务线、业务线各一名最终审批人（起点人为空＝该线只有所指定的最终审批人一个节点）；发起、审批、登记付款、查询都按后台真实请求形状（蛇形键）
 * 走真实入口与授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class PaymentConfirmationJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "pcf_p_" + s;
    private final String otherProject = "pcf_p2_" + s;
    private final String shop = "pcf_m_" + s;
    private final String supplier = "pcf_sup_" + s;
    private final String supplier2 = "pcf_sup2_" + s;
    private final String financeId = "usr_pcf_f_" + s;
    private final String mainId = "usr_pcf_s1_" + s;
    private final String finLineId = "usr_pcf_fl_" + s;
    private final String legalLineId = "usr_pcf_ll_" + s;
    private final String bizLineId = "usr_pcf_bl_" + s;
    private final String responsible = "usr_pcf_r_" + s;
    private RequestPostProcessor finance;
    private RequestPostProcessor main;
    private RequestPostProcessor finLine;
    private RequestPostProcessor legalLine;
    private RequestPostProcessor otherFinance;
    private RequestPostProcessor viewer;
    private RequestPostProcessor otherViewer;

    @BeforeEach
    void seed() {
        for (String p : List.of(project, otherProject)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','付款确认项目','active',now(),now())", p);
        }
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,project_id)"
                + " VALUES(?,'付款确认收款酒吧','bar','active',now(),now(),?)", shop, project);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        finance = staffs.staff(financeId, "staff", project, null, "finance.payment.register", "finance.ar_ap.view",
                "finance.locked_backfill.request", "finance.daily_close.manage", "evidence.attachment.create",
                "governance.supervisor_review.view", "governance.supervisor_review.approve");
        main = staffs.staff(mainId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        finLine = staffs.staff(finLineId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        legalLine = staffs.staff(legalLineId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        staffs.staff(bizLineId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        staffs.staff(responsible, "staff", project, null);
        otherFinance = staffs.staff("usr_pcf_of_" + s, "staff", otherProject, null, "finance.payment.register", "finance.ar_ap.view");
        viewer = staffs.staff("usr_pcf_v_" + s, "staff", project, null, "finance.ar_ap.view", "governance.supervisor_review.view");
        otherViewer = staffs.staff("usr_pcf_ov_" + s, "staff", otherProject, null, "finance.ar_ap.view", "governance.supervisor_review.view");
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", financeId, mainId);
    }

    /** a. 采购入库应付直接登记付款：409 PAYMENT_CONFIRMATION_REQUIRED，台账与付款记录都不变。 */
    @Test
    void payableCannotBePaidWithoutConfirmation() throws Exception {
        String item = payable("a", "purchase_inbound", supplier, 10_000L, 0);

        pay(item, "a", 10_000L).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));

        assertThat(ledger(item)).isEqualTo("10000:0:10000");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ar_ap_payments WHERE ar_ap_item_id = ?", Integer.class, item)).isZero();
    }

    /** b. 缺主线最终审批人 400；缺法务线 400；另带业务线时链上有该线；带齐即待审、动作按来源；同一应付再发一张 409。 */
    @Test
    void requestValidatesLinesAndOpensOneReviewPerConfirmation() throws Exception {
        String item = payable("b", "purchase_inbound", supplier, 10_000L, 0);

        confirm(finance, "b0", List.of(item), null, lines()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        confirm(finance, "b1", List.of(item), mainId, Map.of("finance_review", finLineId)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("APPROVAL_LINE_FINAL_APPROVER_REQUIRED"));
        Map<String, String> withBusiness = new LinkedHashMap<>(lines());
        withBusiness.put("business_review", bizLineId);
        String body = confirm(finance, "b2", List.of(item), mainId, withBusiness).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"))
                .andExpect(jsonPath("$.data.payment_type").value("purchase_inbound"))
                .andExpect(jsonPath("$.data.payment_type_label").value("采购入库"))
                .andExpect(jsonPath("$.data.amount_cent").value(10_000))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String confirmation = JsonPath.read(body, "$.data.confirmation_id");
        String reviewId = JsonPath.read(body, "$.data.review_id");

        assertThat(jdbc.queryForMap("SELECT policy_code, action_type, object_type, object_id, review_status FROM supervisor_review_decisions"
                + " WHERE review_id = ?", reviewId))
                .containsEntry("policy_code", "payment_confirm_purchase_inbound")
                .containsEntry("action_type", "finance.payment_confirm.purchase_inbound")
                .containsEntry("object_type", "payment_confirmation").containsEntry("object_id", confirmation)
                .containsEntry("review_status", "pending_review");
        assertThat(nodes(reviewId)).containsExactlyInAnyOrder("main:" + mainId, "finance_review:" + finLineId, "legal_review:" + legalLineId,
                "business_review:" + bizLineId);
        confirm(finance, "b3", List.of(item), mainId, lines()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_EXISTS"));
    }

    /** c. 发起人审批 403；主线、财务线通过而法务线未审时登记仍 409；全部通过后查询为已通过，可分笔付到结清。 */
    @Test
    void allLinesMustApproveBeforePaymentAndOneConfirmationCoversPartialPayments() throws Exception {
        String item = payable("c", "purchase_inbound", supplier, 10_000L, 0);
        String reviewId = reviewOf(confirm(finance, "c", List.of(item), mainId, lines()).andExpect(status().isOk()));

        approve(finance, reviewId, "c-self", "approved").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("SUPERVISOR_SELF_REVIEW_FORBIDDEN"));
        approve(main, reviewId, "c-1", "approved").andExpect(status().isOk());
        approve(finLine, reviewId, "c-2", "approved").andExpect(status().isOk());
        pay(item, "c-early", 4_000L).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));
        approve(legalLine, reviewId, "c-3", "approved").andExpect(status().isOk());

        listed(finance, item).andExpect(jsonPath("$.data.payment_type").value("purchase_inbound"))
                .andExpect(jsonPath("$.data.payment_type_label").value("采购入库"))
                .andExpect(jsonPath("$.data.required_line_codes[0]").value("finance_review"))
                .andExpect(jsonPath("$.data.required_line_codes[1]").value("legal_review"))
                .andExpect(jsonPath("$.data.confirmations[0].status").value("approved"))
                .andExpect(jsonPath("$.data.confirmations[0].review_id").value(reviewId));
        pay(item, "c-part", 4_000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.item.status").value("partial_paid"));
        pay(item, "c-rest", 6_000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.item.status").value("settled"));
    }

    /** d. 任一线驳回：确认单驳回、登记 409；可以重新发起。 */
    @Test
    void rejectionByAnyLineBlocksPaymentAndAllowsANewRequest() throws Exception {
        String item = payable("d", "purchase_inbound", supplier, 10_000L, 0);
        String reviewId = reviewOf(confirm(finance, "d", List.of(item), mainId, lines()).andExpect(status().isOk()));

        approve(main, reviewId, "d-1", "approved").andExpect(status().isOk());
        approve(legalLine, reviewId, "d-2", "rejected").andExpect(status().isOk());

        listed(finance, item).andExpect(jsonPath("$.data.confirmations[0].status").value("rejected"));
        pay(item, "d", 10_000L).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));
        confirm(finance, "d-again", List.of(item), mainId, lines()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review"));
        listed(finance, item).andExpect(jsonPath("$.data.confirmations.length()").value(2))
                .andExpect(jsonPath("$.data.confirmations[0].status").value("pending_review"));
    }

    /** e. 同一供应商两笔入库应付合发一张：一张复核单、金额为合计，通过后两笔都能登记；不同收款方的两笔一起发起 400。 */
    @Test
    void sameSupplierPayablesMergeIntoOneConfirmation() throws Exception {
        String first = payable("e1", "purchase_inbound", supplier, 3_000L, 0);
        String second = payable("e2", "purchase_inbound", supplier, 5_000L, 0);
        String body = confirm(finance, "e", List.of(second, first), mainId, lines()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amount_cent").value(8_000))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String confirmation = JsonPath.read(body, "$.data.confirmation_id");
        String reviewId = JsonPath.read(body, "$.data.review_id");

        assertThat(jdbc.queryForObject("SELECT count(*) FROM supervisor_review_decisions WHERE object_id = ?", Integer.class, confirmation))
                .isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT object_id || ':' || amount_cent FROM payment_confirmations WHERE confirmation_id = ? ORDER BY object_id",
                String.class, confirmation)).containsExactly(first + ":3000", second + ":5000");
        approveAll(reviewId, "e");
        pay(first, "e1", 3_000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.item.status").value("settled"));
        pay(second, "e2", 5_000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.item.status").value("settled"));

        String a = payable("e3", "purchase_inbound", supplier, 1_000L, 0);
        String b = payable("e4", "purchase_inbound", supplier2, 1_000L, 0);
        confirm(finance, "e-mixed", List.of(a, b), mainId, lines()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM payment_confirmations WHERE object_id IN (?, ?)", Integer.class, a, b)).isZero();
    }

    /** f. 应收登记收款不需要付款确认。 */
    @Test
    void receivableCollectionNeedsNoConfirmation() throws Exception {
        String item = "pcf_item_f_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id)
                VALUES(?,?,'receivable',?,'manual',?,2000,0,2000,'CNY',current_date,'2026-11','pending_collection',now(),now(),NULL,?)
                """, item, "PCF-" + item, shop, item, project);

        pay(item, "f", 2_000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.item.status").value("settled"));
    }

    /** g. 通票分账（异议期已过）发起付款 409；确认通过后发起 200，登记失败后再发起 200，登记到账结清。 */
    @Test
    void internalPayableInitiationNeedsConfirmationAndRepayReusesIt() throws Exception {
        InternalPayDayFixtures.seed(jdbc);
        String item = payable("g", "ticket_distribution", shop, 5_000L, 0);
        LocalDate today = LocalDate.now(com.complex.platform.common.LocalZone.ID);
        jdbc.update("""
                INSERT INTO internal_payable_schedules(ar_ap_item_id,route,project_id,counterparty_id,billing_period_start,visible_at,
                    objection_deadline,designated_pay_day,scheduled_due_date,pay_days_rule_id,route_rule_id,objection_working_days)
                VALUES(?,'ticket_distribution',?,?,?,now() - interval '20 days',?,?,?,'ipdr_initial_pay_days','ipdr_fixture_ticket_distribution',10)
                """, item, project, shop, today.withDayOfMonth(1).minusMonths(1), today.minusDays(1), today, today);

        initiate(item, "g0").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));
        approveAll(reviewOf(confirm(finance, "g", List.of(item), mainId, lines()).andExpect(status().isOk())), "g");
        String attempt = JsonPath.read(initiate(item, "g1").andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        mockMvc.perform(post("/api/v1/finance/internal-payables/" + item + "/payment-attempts/" + attempt + "/failure").with(finance)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "pcf-fail-g-" + s, "failure_reason", "收款账户被冻结",
                                "expected_repay_date", today.toString()))))
                .andExpect(status().isOk());
        initiate(item, "g2").andExpect(status().isOk());
        pay(item, "g", 5_000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.item.status").value("settled"));
    }

    /**
     * h. 到期营业日日结锁定后的应付提交收付款补录（200 待审）；未付款确认时末节点批准 409，补录与复核单仍待审、台账不变；确认通过后同一节点再批 200，台账结清。
     * 不开测试事务：末节点批准失败须整笔回滚（与线上请求同一事务边界）；用例结束把夹具日结改回已确认，不留封账影响别的用例。
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void lockedPaymentBackfillApprovalWaitsForConfirmation() throws Exception {
        String close = "pcf_dc_" + s;
        jdbc.update("INSERT INTO finance_daily_closes(id,business_date,scope_type,scope_id,status,locked_at,created_at)"
                + " VALUES(?,current_date - 3,'project',?,'locked',now(),now())", close, project);
        try {
            String item = payable("h", "manual", supplier, 10_000L, -3);
            jdbc.update("UPDATE ar_ap_items SET locked_day_close_id = ? WHERE id = ?", close, item);
            Map<String, Object> request = new HashMap<>(Map.of("idempotency_key", "pcf-adj-h-" + s, "kind", "payment", "ar_ap_item_id", item,
                    "adjustment_amount_cent", 10_000L, "reason", "银行回单到账后补录", "responsible_staff_id", responsible,
                    "payment_method", "bank_transfer", "evidence_attachment_ids", List.of(upload())));
            String body = mockMvc.perform(post("/api/v1/finance/ar-ap-adjustments").with(finance).contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(request)))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
            String adjustment = JsonPath.read(body, "$.data.id");
            String adjustmentReview = JsonPath.read(body, "$.data.review_id");

            approve(main, adjustmentReview, "h-1", "approved").andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));
            assertThat(jdbc.queryForObject("SELECT approval_status FROM ar_ap_adjustments WHERE id = ?", String.class, adjustment))
                    .isEqualTo("pending");
            assertThat(jdbc.queryForObject("SELECT review_status FROM supervisor_review_decisions WHERE review_id = ?", String.class,
                    adjustmentReview)).isEqualTo("pending_review");
            assertThat(ledger(item)).isEqualTo("10000:0:10000");

            approveAll(reviewOf(confirm(finance, "h", List.of(item), mainId, lines()).andExpect(status().isOk())), "h");
            approve(main, adjustmentReview, "h-2", "approved").andExpect(status().isOk());
            assertThat(jdbc.queryForObject("SELECT approval_status FROM ar_ap_adjustments WHERE id = ?", String.class, adjustment))
                    .isEqualTo("approved");
            assertThat(ledger(item)).isEqualTo("10000:10000:0");
        } finally {
            jdbc.update("UPDATE finance_daily_closes SET status = 'confirmed' WHERE id = ?", close);
        }
    }

    /** i. 押金退还到期前可以发起并通过付款确认，到期前登记仍 409 DEPOSIT_REFUND_NOT_DUE。 */
    @Test
    void depositRefundConfirmedBeforeDueStillWaitsForDueDate() throws Exception {
        String item = payable("i", "deposit_refund", shop, 10_000L, 5);
        approveAll(reviewOf(confirm(finance, "i", List.of(item), mainId, lines()).andExpect(status().isOk())), "i");

        pay(item, "i", 10_000L).andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("DEPOSIT_REFUND_NOT_DUE"));
    }

    /** j. 他项目财务发起 403（同一幂等键重发仍 403，幂等表无该键）；通用入口带工资应付 409 PAYROLL_DEDICATED_WORKFLOW_REQUIRED。 */
    @Test
    void scopeAndPayrollIsolationRunBeforeIdempotency() throws Exception {
        String item = payable("j", "purchase_inbound", supplier, 10_000L, 0);

        confirm(otherFinance, "j", List.of(item), mainId, lines()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        confirm(otherFinance, "j", List.of(item), mainId, lines()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM idempotency_records WHERE idempotency_key = ?", Integer.class, "pcf-req-j-" + s))
                .isZero();

        String wage = payable("j2", "staff_payroll", responsible, 10_000L, 0);
        confirm(finance, "j2", List.of(wage), mainId, lines()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYROLL_DEDICATED_WORKFLOW_REQUIRED"));
    }

    /** k. 他项目查看人查确认单为空；同项目、不在链上的查看人能看到复核单，他项目的看不到。 */
    @Test
    void confirmationsAndReviewsFollowTheObjectProject() throws Exception {
        String item = payable("k", "purchase_inbound", supplier, 10_000L, 0);
        String reviewId = reviewOf(confirm(finance, "k", List.of(item), mainId, lines()).andExpect(status().isOk()));

        listed(viewer, item).andExpect(jsonPath("$.data.confirmations.length()").value(1));
        listed(otherViewer, item).andExpect(jsonPath("$.data.confirmations.length()").value(0));
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + reviewId).with(viewer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.object_type").value("payment_confirmation"));
        mockMvc.perform(get("/api/v1/platform/supervisor-review/" + reviewId).with(otherViewer)).andExpect(status().isForbidden());
    }

    /** l. 审批时限清单含各支付类型的付款确认审批类型（第十八批 AX 段 273-R1 加酒吧借款还款，第十八批 AZ 段 350-R1 加通用卡消费结算），涉钱、出厂 1 个工作日；策略各带财务、法务两线、主线按直接上级链。 */
    @Test
    void fortyPaymentTypesAreMoneyApprovalTypesWithFinanceAndLegalLines() throws Exception {
        List<Map<String, Object>> deadlines = JsonPath.read(mockMvc.perform(get("/api/v1/platform/approval-deadlines").with(main))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data");
        List<Map<String, Object>> payment = deadlines.stream()
                .filter(row -> String.valueOf(row.get("action_type")).startsWith("finance.payment_confirm.")).toList();
        assertThat(payment).extracting(row -> String.valueOf(row.get("action_type")).substring("finance.payment_confirm.".length()))
                .containsExactlyInAnyOrderElementsOf(PaymentConfirmationService.PAYMENT_TYPES.keySet()).containsAll(PAYMENT_TYPES);
        assertThat(payment).allSatisfy(row -> assertThat(row).containsEntry("money", true).containsEntry("factory_working_days", 1));

        List<Map<String, Object>> policies = JsonPath.read(mockMvc.perform(get("/api/v1/platform/supervisor-review-policies").with(main))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.data.items[?(@.policy_code =~ /payment_confirm_.*/)]");
        assertThat(policies).extracting(row -> String.valueOf(row.get("policy_code"))).containsExactlyInAnyOrderElementsOf(
                PaymentConfirmationService.PAYMENT_TYPES.keySet().stream().map(type -> "payment_confirm_" + type).toList());
        assertThat(policies).allSatisfy(row -> assertThat(row)
                .containsEntry("approval_level", "one_level_up").containsEntry("parallel_line_codes", List.of("finance_review", "legal_review")));
    }

    /** 名称陷阱：分红应付（动作名含 dividend）不升最高权限档，酒吧报酬拨付（含 payroll）照常发起；主线都按直接上级链到所指定的最终审批人。 */
    @Test
    void actionNamesContainingDividendOrPayrollKeepTheirPolicy() throws Exception {
        String dividend = payable("m1", "dividend_payout", "usr_pcf_principal_" + s, 10_000L, 0);
        String barPayroll = payable("m2", "bar_payroll_transfer", shop, 10_000L, 0);

        String dividendReview = reviewOf(confirm(finance, "m1", List.of(dividend), mainId, lines()).andExpect(status().isOk()));
        String barReview = reviewOf(confirm(finance, "m2", List.of(barPayroll), mainId, lines()).andExpect(status().isOk()));

        assertThat(jdbc.queryForObject("SELECT policy_code FROM supervisor_review_decisions WHERE review_id = ?", String.class, dividendReview))
                .isEqualTo("payment_confirm_dividend_payout");
        assertThat(jdbc.queryForObject("SELECT policy_code FROM supervisor_review_decisions WHERE review_id = ?", String.class, barReview))
                .isEqualTo("payment_confirm_bar_payroll_transfer");
        assertThat(nodes(dividendReview)).contains("main:" + mainId);
        assertThat(nodes(barReview)).contains("main:" + mainId);
    }

    /**
     * 241-R9（第十八批裁决 69「付款方是平台主体的应收，登记收款前也须付款确认」）：组合通票域间份额负额挂出的应收（付款方是平台主体）登记收款 409，
     * 台账不变；按其来源发起付款确认（支付类型同其应付）、各线通过后登记收款 200 结清。付款方在平台外的房租应收登记收款照旧 200。
     */
    @Test
    void platformPayerReceivableNeedsConfirmationBeforeCollection() throws Exception {
        String share = receivable("r9", "pass_domain_share", shop, 6_000L);

        pay(share, "r9-early", 6_000L).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));
        assertThat(ledger(share)).isEqualTo("6000:0:6000");
        String reviewId = reviewOf(confirm(finance, "r9", List.of(share), mainId, lines()).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_type").value("pass_domain_share")));
        approveAll(reviewId, "r9");
        pay(share, "r9", 6_000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.item.status").value("settled"));

        String rent = receivable("r9-rent", "contract", shop, 2_000L);
        pay(rent, "r9-rent", 2_000L).andExpect(status().isOk()).andExpect(jsonPath("$.data.item.status").value("settled"));
    }

    /**
     * 241-R8（第十八批裁决 67「审批线页设定」、66「每类必带财务、法务两线」）：组织层级与审批线页列出各支付类型（中文名、两线之外的必带线，出厂为空）；
     * 无门禁码 403、只能到本项目的账号改全局流程 403、未知支付类型 404、含停用线 400；平台管理员保存后留痕。
     */
    @Test
    void paymentConfirmationFlowsAreGlobalSettingsOnTheApprovalLinePage() throws Exception {
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor admin = staffs.staff("usr_pcf_adm_" + s, "platform_admin", null, null, "access.role.assign");
        RequestPostProcessor projectManager = staffs.staff("usr_pcf_pm_" + s, "staff", project, null, "access.role.assign");

        List<Map<String, Object>> flows = JsonPath.read(mockMvc.perform(get("/api/v1/access/payment-confirmation-flows").with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data");
        assertThat(flows).extracting(row -> String.valueOf(row.get("payment_type")))
                .containsExactlyElementsOf(PaymentConfirmationService.PAYMENT_TYPES.keySet()).containsSubsequence(PAYMENT_TYPES);
        assertThat(flows).allSatisfy(row -> assertThat(row).containsEntry("line_codes", List.of()));
        assertThat(flows).filteredOn(row -> "purchase_inbound".equals(row.get("payment_type")))
                .extracting(row -> row.get("payment_type_label")).containsExactly("采购入库");
        mockMvc.perform(get("/api/v1/access/payment-confirmation-flows").with(viewer)).andExpect(status().isForbidden());

        flow(finance, "purchase_inbound", "o1", List.of("business_review")).andExpect(status().isForbidden());
        flow(projectManager, "purchase_inbound", "o2", List.of("business_review")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_ORG_FORBIDDEN"));
        flow(admin, "no_such_payment_type", "o3", List.of()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_TYPE_NOT_FOUND"));
        String inactive = "pcf_off_" + s;
        jdbc.update("INSERT INTO approval_lines(line_code,line_name,min_start_rank,entry_user_id,status) VALUES(?,'停用审批线',1,NULL,'inactive')",
                inactive);
        flow(admin, "purchase_inbound", "o4", List.of("business_review", inactive)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("APPROVAL_LINE_NOT_CONFIGURED"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM payment_confirmation_flows WHERE payment_type = 'purchase_inbound'", Integer.class))
                .isZero();

        flow(admin, "purchase_inbound", "o5", List.of("business_review")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_type").value("purchase_inbound"))
                .andExpect(jsonPath("$.data.payment_type_label").value("采购入库"))
                .andExpect(jsonPath("$.data.line_codes", contains("business_review")));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_events WHERE event_name = 'access.payment_confirmation_flow.saved'"
                + " AND event_result = 'success' AND business_id = 'purchase_inbound'", Integer.class)).isEqualTo(1);
    }

    /**
     * 241-R8：某类设定了两线之外的必带线（业务线）后，新发起不带该线 400 APPROVAL_LINE_FINAL_APPROVER_REQUIRED、带上即链上有该线节点，查询回显的必带线为
     * 财务、法务两线加该线；设定前已发起的付款确认链节点不变（链在发起时生成）。
     */
    @Test
    void flowLinesBindNewConfirmationsOnly() throws Exception {
        RequestPostProcessor admin = new StaffRequestFixture(jdbc).staff("usr_pcf_adm2_" + s, "platform_admin", null, null, "access.role.assign");
        String before = payable("n1", "purchase_inbound", supplier, 1_000L, 0);
        String beforeReview = reviewOf(confirm(finance, "n1", List.of(before), mainId, lines()).andExpect(status().isOk()));

        flow(admin, "purchase_inbound", "n", List.of("business_review")).andExpect(status().isOk());

        assertThat(nodes(beforeReview)).containsExactlyInAnyOrder("main:" + mainId, "finance_review:" + finLineId, "legal_review:" + legalLineId);
        String after = payable("n2", "purchase_inbound", supplier, 1_000L, 0);
        confirm(finance, "n2", List.of(after), mainId, lines()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("APPROVAL_LINE_FINAL_APPROVER_REQUIRED"));
        Map<String, String> withBusiness = new LinkedHashMap<>(lines());
        withBusiness.put("business_review", bizLineId);
        String afterReview = reviewOf(confirm(finance, "n3", List.of(after), mainId, withBusiness).andExpect(status().isOk()));
        assertThat(nodes(afterReview)).contains("business_review:" + bizLineId);
        listed(finance, after).andExpect(jsonPath("$.data.required_line_codes", contains("finance_review", "legal_review", "business_review")));
    }

    /**
     * e2. 合并发起须同一方向（第十八批裁决 68「同一收款方、同一支付类型、同一综合体的多笔未结清应付」；裁决 69 题面「同一类往来按金额正负分挂应付或应收」
     * 「记成应收的那一行，登记收款就是登记该平台主体的出款」——应收行的收款方是本方）：同一往来方、同一来源（酒吧报酬月结拨付）的一应付一应收合发 400，
     * 不落付款确认行。
     */
    @Test
    void payableAndReceivableOfTheSameCounterpartyCannotMerge() throws Exception {
        String payable = payable("e5", "bar_payroll_transfer", shop, 5_000L, 0);
        String receivable = receivable("e6", "bar_payroll_transfer", shop, 300L);

        confirm(finance, "e-dir", List.of(payable, receivable), mainId, lines()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("合并发起须为同一收款方、同一支付类型、同一综合体的应付"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM payment_confirmations WHERE object_id IN (?, ?)", Integer.class, payable, receivable))
                .isZero();
    }

    /** e3. 不同支付类型（采购入库与手工应付）、不同综合体的两笔应付合发各 400，不落付款确认行。 */
    @Test
    void mergeRequiresTheSamePaymentTypeAndProject() throws Exception {
        String inbound = payable("e7", "purchase_inbound", supplier, 1_000L, 0);
        String manual = payable("e8", "manual", supplier, 1_000L, 0);
        confirm(finance, "e-type", List.of(inbound, manual), mainId, lines()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("合并发起须为同一收款方、同一支付类型、同一综合体的应付"));

        RequestPostProcessor platformFinance = new StaffRequestFixture(jdbc).staff("usr_pcf_pf_" + s, "platform_admin", null, null,
                "finance.payment.register");
        String here = payable("e9", "purchase_inbound", supplier, 1_000L, 0);
        String there = payable("ea", "purchase_inbound", supplier, 1_000L, 0);
        jdbc.update("UPDATE ar_ap_items SET project_id = ? WHERE id = ?", otherProject, there);
        confirm(platformFinance, "e-project", List.of(here, there), mainId, lines()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("合并发起须为同一收款方、同一支付类型、同一综合体的应付"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM payment_confirmations WHERE object_id IN (?, ?, ?, ?)", Integer.class,
                inbound, manual, here, there)).isZero();
    }

    /** e4. 平台主体付款的应收（241-R9）收款方是本方门店：同一付款方付给不同门店的两笔应收合发 400（裁决 68「同一收款方」），不落付款确认行。 */
    @Test
    void receivablesFromOnePayerToDifferentShopsCannotMerge() throws Exception {
        String shop2 = "pcf_m2_" + s;
        String payer = "pcf_payer_" + s;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,project_id)"
                + " VALUES(?,'付款确认收款酒吧二','bar','active',now(),now(),?)", shop2, project);
        String toShop = receivable("eb", "bar_payroll_transfer", payer, 300L);
        String toShop2 = receivable("ec", "bar_payroll_transfer", payer, 400L);
        jdbc.update("UPDATE ar_ap_items SET merchant_id = ? WHERE id = ?", shop, toShop);
        jdbc.update("UPDATE ar_ap_items SET merchant_id = ? WHERE id = ?", shop2, toShop2);

        confirm(finance, "e-shop", List.of(toShop, toShop2), mainId, lines()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("合并发起须为同一付款方、同一收款方、同一支付类型、同一综合体的应收"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM payment_confirmations WHERE object_id IN (?, ?)", Integer.class, toShop, toShop2))
                .isZero();
    }

    /** e5. 只发起一笔平台主体付款的应收：复核摘要写「付款方 ＜往来方＞」，不写「收款方」（应收行的往来方是出钱的一方）。 */
    @Test
    void receivableConfirmationSummaryNamesThePayer() throws Exception {
        String share = receivable("ed", "pass_domain_share", shop, 6_000L);
        String reviewId = reviewOf(confirm(finance, "ed", List.of(share), mainId, lines()).andExpect(status().isOk()));

        assertThat(jdbc.queryForObject("SELECT reason FROM supervisor_review_decisions WHERE review_id = ?", String.class, reviewId))
                .contains("付款方 " + shop).doesNotContain("收款方");
    }

    // ---------- helpers ----------

    private static final List<String> PAYMENT_TYPES = List.of(
            "dividend_payout", "dividend_reserve_released", "staff_payroll", "deposit_refund", "deposit_adjustment",
            "purchase_inbound", "purchase_consignment", "purchase_prepayment", "ticket_distribution", "platform_incentive_subsidy",
            "single_bar_service_fee", "compensation_fulfillment", "pass_domain_share", "pass_domain_direct_settlement",
            "pass_unverified_attribution", "dispatch_commission_transfer", "bar_payroll_transfer", "inventory_transfer",
            "pass_drink_settlement", "media_performance_settlement", "refund", "bar_deposit_refund", "stored_value_consume",
            "partner_voucher", "reconciliation_difference", "inventory_difference", "turnover_rent_reversal", "manual", "bar_loan",
            "general_card_consume", "amusement_domain_settlement",
            "compensation_fulfillment_recovery", "ticket_distribution_recovery", "saved_bottle_remedy_recovery",
            "finance_refund_ar_ap_payment", "finance_refund_stored_value_account", "finance_refund_ticket_payment_rejection",
            "finance_refund_channel_ticket_rejection", "finance_refund_dealer_order_refund", "finance_refund_bar_deposit_rejection",
            "finance_refund_bar_scan_order_rejection", "ticket_refund", "saved_bottle_compensation");

    /** 本项目一笔未付应付（来源、收款方、金额、到期日相对今天的天数），返回台账号。 */
    private String payable(String tag, String source, String counterparty, long amountCent, int dueOffsetDays) {
        String item = "pcf_item_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id)
                VALUES(?,?,'payable',?,?,?,?,0,?,'CNY',current_date + ?,'2026-11','pending_payment',now(),now(),NULL,?)
                """, item, "PCF-" + item, counterparty, source, item, amountCent, amountCent, dueOffsetDays, project);
        return item;
    }

    /** 本项目一笔未收应收（来源、付款方、金额，今天到期），返回台账号。 */
    private String receivable(String tag, String source, String counterparty, long amountCent) {
        String item = "pcf_item_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id)
                VALUES(?,?,'receivable',?,?,?,?,0,?,'CNY',current_date,'2026-11','pending_collection',now(),now(),NULL,?)
                """, item, "PCF-" + item, counterparty, source, item, amountCent, amountCent, project);
        return item;
    }

    /** 组织层级与审批线页「付款确认流程」保存某支付类型两线之外必带线的真实请求。 */
    private ResultActions flow(RequestPostProcessor who, String paymentType, String tag, List<String> lineCodes) throws Exception {
        return mockMvc.perform(put("/api/v1/access/payment-confirmation-flows/" + paymentType).with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcf-flow-" + tag + "-" + s, "line_codes", lineCodes))));
    }

    private static Map<String, String> lines() {
        return new LinkedHashMap<>(Map.of("finance_review", "", "legal_review", ""));
    }

    /** 台账页勾选款项发起付款确认的真实请求；lines 的值为空串时按本用例的财务线、法务线最终审批人填。 */
    private ResultActions confirm(RequestPostProcessor who, String tag, List<String> items, String finalApprover, Map<String, String> lines)
            throws Exception {
        List<Map<String, String>> parallel = new ArrayList<>();
        lines.forEach((code, approver) -> parallel.add(Map.of("line_code", code, "final_approver_id",
                !approver.isEmpty() ? approver : "finance_review".equals(code) ? finLineId : legalLineId)));
        Map<String, Object> body = new HashMap<>(Map.of("idempotency_key", "pcf-req-" + tag + "-" + s, "ar_ap_item_ids", items,
                "reason", "供应商月结货款", "parallel_lines", parallel));
        if (finalApprover != null) {
            body.put("final_approver_id", finalApprover);
        }
        return mockMvc.perform(post("/api/v1/finance/ar-ap-payment-confirmations").with(who)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private static String reviewOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.review_id");
    }

    private ResultActions listed(RequestPostProcessor who, String item) throws Exception {
        return mockMvc.perform(get("/api/v1/finance/payment-confirmations").param("object_type", "ar_ap_item").param("object_id", item).with(who))
                .andExpect(status().isOk());
    }

    /** 台账页「登记核销」的真实请求。 */
    private ResultActions pay(String item, String tag, long amountCent) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/ar-ap-items/" + item + "/payments").with(finance).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcf-pay-" + tag + "-" + s, "amount_cent", amountCent,
                        "payment_method", "bank_transfer", "bank_flow_ref", "FLOW-" + tag + "-" + s))));
    }

    /** 内部应付面板「登记已发起付款」的真实请求。 */
    private ResultActions initiate(String item, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/internal-payables/" + item + "/payment-attempts").with(finance)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcf-ini-" + tag + "-" + s, "bank_ref", "BANK-" + tag))));
    }

    private ResultActions approve(RequestPostProcessor who, String reviewId, String tag, String decision) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + reviewId + "/approve").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcf-apv-" + tag + "-" + s, "decision", decision,
                        "comment", "已核对付款明细"))));
    }

    /** 主线、财务线、法务线各自的最终审批人逐一通过。 */
    private void approveAll(String reviewId, String tag) throws Exception {
        approve(main, reviewId, tag + "-main", "approved").andExpect(status().isOk());
        approve(finLine, reviewId, tag + "-fin", "approved").andExpect(status().isOk());
        approve(legalLine, reviewId, tag + "-legal", "approved").andExpect(status().isOk());
    }

    /** 补录区块上传原始凭证（银行回单 pdf），返回附件编号。 */
    private String upload() throws Exception {
        return JsonPath.read(mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "bank-receipt.pdf", "application/pdf",
                                "%PDF-1.4 payment confirmation receipt".getBytes(StandardCharsets.UTF_8)))
                        .param("business_type", "finance_locked_backfill").with(finance))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
    }

    /** 审批链节点（线:审批人）。 */
    private List<String> nodes(String reviewId) {
        return jdbc.queryForList("SELECT line_code || ':' || approver_id FROM supervisor_review_nodes WHERE review_id = ?", String.class, reviewId);
    }

    /** 台账总额:已收付:未收付。 */
    private String ledger(String item) {
        return jdbc.queryForObject("SELECT amount_cent || ':' || paid_amount_cent || ':' || unpaid_amount_cent FROM ar_ap_items WHERE id = ?",
                String.class, item);
    }
}
