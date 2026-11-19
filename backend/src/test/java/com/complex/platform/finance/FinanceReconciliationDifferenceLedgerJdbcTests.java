package com.complex.platform.finance;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.LocalZone;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
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
 * 第十八批 AL 段第 223 条 223-R1（BSM:1751「高价值差异复核通过才入账、同一差异只入账一次」；ENUM:191「应收应付台账「高价值差异复核通过才入账」按本判定
 * 执行。」；BSM:1746「资金以银行实际到账为准」）：对账差异提交复核时勾选「认定结果入应收应付台账」，复核通过后按实际到账与平台记录之差向原往来方记
 * 一笔应收或应付，同一差异只入一次。第 224 条 224-R1（ENUM:173「财务差异按本表「差异处理状态」条处理」；第十八批裁决 15）：已核验的经销到账记录
 * 同样可入账。生成日结、上传原件、登记、识别、提交复核、读复核单、审批、读差异都按后台真实请求形状（蛇形键）走真实入口与授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FinanceReconciliationDifferenceLedgerJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "frl_p_" + s;
    private final String bar = "frl_m_" + s;
    private final String adminId = "usr_frl_admin_" + s;
    private final String approverId = "usr_frl_appr_" + s;
    private final LocalDate day = LocalDate.now(LocalZone.ID).minusDays(3);
    private RequestPostProcessor admin;
    private RequestPostProcessor approver;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','对账差异入账项目','active',now(),now())",
                project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,project_id)"
                + " VALUES(?,'对账差异入账酒吧','bar','active',now(),now(),?)", bar, project);
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff(adminId, "platform_admin", project, null,
                "finance.daily_close.lock", "finance.reconciliation.view", "finance.reconciliation.difference.create",
                "finance.reconciliation.resolve", "evidence.attachment.create", "governance.supervisor_review.view",
                "finance.locked_backfill.request");
        approver = staffs.staff(approverId, "staff", project, null,
                "governance.supervisor_review.view", "governance.supervisor_review.approve", "evidence.timeline.view");
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", adminId, approverId);
    }

    /**
     * ① 原台账为商户应收、实际到账少于平台记录：勾选入账提交复核，复核单原因写明入账方向与金额；上级通过后恰入一行应收（往来方、商户、项目同原台账，
     * 金额＝差额，不填冲正原行），差异列表带回入账台账编号；同一幂等键重放审批请求 200，台账仍一行（同一差异只入账一次）。
     */
    @Test
    void shortfallOnReceivablePostsReceivableOnce() throws Exception {
        String payment = payment("r1", "receivable", 10_000L);
        String diff = handled("r1", payment, 9_000L);

        String reviewId = JsonPath.read(submit(diff, "r1", true).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ledger_entry").value(true))
                .andReturn().getResponse().getContentAsString(), "$.data.review_id");
        String reason = JsonPath.read(mockMvc.perform(get("/api/v1/platform/supervisor-review/" + reviewId).with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.reason");
        assertThat(reason).contains("认定后入应收应付台账：向往来方 " + bar + " 记应收 10.00 元（原台账 FRL-frl_item_r1_" + s + "）");
        approve(reviewId, "r1").andExpect(status().isOk());

        List<Map<String, Object>> posted = posted(diff);
        assertThat(posted).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("direction", "receivable").containsEntry("counterparty_id", bar).containsEntry("merchant_id", bar)
                .containsEntry("project_id", project).containsEntry("amount_cent", 1_000L).containsEntry("unpaid_amount_cent", 1_000L)
                .containsEntry("origin_item_id", null));
        List<String> listed = JsonPath.read(mockMvc.perform(get("/api/v1/finance/reconciliation/differences").with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "$.data.items[?(@.id == '" + diff + "')].posted_ar_ap_item_id");
        assertThat(listed).containsExactly((String) posted.get(0).get("id"));

        approve(reviewId, "r1").andExpect(status().isOk());
        assertThat(posted(diff)).hasSize(1);
    }

    /** ② 原台账为应收、实际到账多于平台记录：记应付（反方向）。 */
    @Test
    void overpaymentOnReceivablePostsPayable() throws Exception {
        String diff = handled("r2", payment("r2", "receivable", 10_000L), 11_000L);

        approve(reviewOf(submit(diff, "r2", true)), "r2").andExpect(status().isOk());

        assertThat(posted(diff)).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("direction", "payable").containsEntry("amount_cent", 1_000L));
    }

    /** ③ 原台账为应付、实际到账少于平台记录：记应付（同方向）。 */
    @Test
    void shortfallOnPayablePostsPayable() throws Exception {
        String diff = handled("r3", payment("r3", "payable", 10_000L), 9_000L);

        approve(reviewOf(submit(diff, "r3", true)), "r3").andExpect(status().isOk());

        assertThat(posted(diff)).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("direction", "payable").containsEntry("amount_cent", 1_000L));
    }

    /** ④ 不勾选入账：差异 ledger_entry=false，复核通过后不入台账。 */
    @Test
    void withoutLedgerEntryNothingIsPosted() throws Exception {
        String diff = handled("r4", payment("r4", "receivable", 10_000L), 9_000L);

        String reviewId = reviewOf(submit(diff, "r4", null).andExpect(jsonPath("$.data.ledger_entry").value(false)));
        approve(reviewId, "r4").andExpect(status().isOk());

        assertThat(posted(diff)).isEmpty();
    }

    /** ⑤ 业务单号为已支付票务订单（往来方是渠道与客人，没有原往来台账）：勾选入账提交 400 VALIDATION_FAILED，差异仍为待处理。 */
    @Test
    void ticketOrderDifferenceCannotEnterLedger() throws Exception {
        String diff = handled("r5", ticketOrder("r5", 8_800L), 8_000L);

        submit(diff, "r5", true).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("VALIDATION_FAILED"));
        assertThat(jdbc.queryForObject("SELECT status FROM finance_reconciliation_differences WHERE id = ?", String.class, diff))
                .isEqualTo("pending_handling");
    }

    /** ⑦（随 224-R1）已核验的经销到账：复核通过后记应收，往来方为经销商、项目为经销商项目、商户为空。 */
    @Test
    void verifiedDealerPaymentPostsReceivableToDealer() throws Exception {
        String dealer = "frl_dl_" + s;
        String diff = handled("r7", dealerPayment(dealer, 50_000L), 49_000L, "project", project);

        approve(reviewOf(submit(diff, "r7", true)), "r7").andExpect(status().isOk());

        assertThat(posted(diff)).singleElement().satisfies(row -> assertThat(row)
                .containsEntry("direction", "receivable").containsEntry("counterparty_id", dealer).containsEntry("project_id", project)
                .containsEntry("merchant_id", null).containsEntry("amount_cent", 1_000L));
    }

    /**
     * ⑧ 18AL 收尾复核修正（ENUM:190「日结锁定后的补录须上级审批」；BSM:1318「查看与平台口径不一致的对账差异。」）：入账行所在营业日的日结锁定后，对该入账行
     * 申请锁账金额补录（−4.00 元）并经上级在主管复核页通过，追加冲正行与重述行（两行沿用来源与差异号）；此后差异列表仍 200、入账台账编号仍为原入账行，
     * 复核人按附件回读原件（按差异号取差异）仍 200。日结锁定与台账打锁照 LockedBackfillReviewJdbcTests 直接落库（该营业日未闭店，真实锁定过不了日结闸）。
     */
    @Test
    void amountBackfillOnPostedItemKeepsDifferenceReadable() throws Exception {
        String diff = handled("r8", payment("r8", "receivable", 10_000L), 9_000L);
        approve(reviewOf(submit(diff, "r8", true)), "r8").andExpect(status().isOk());
        String item = (String) posted(diff).get(0).get("id");
        String close = "frl_dc_" + s;
        jdbc.update("INSERT INTO finance_daily_closes(id,business_date,scope_type,scope_id,status,locked_at,created_at)"
                + " SELECT ?, due_date, 'merchant', ?, 'locked', now(), now() FROM ar_ap_items WHERE id = ?", close, bar, item);
        jdbc.update("UPDATE ar_ap_items SET locked_day_close_id = ? WHERE id = ?", close, item);
        String responsible = "usr_frl_resp_" + s;
        new StaffRequestFixture(jdbc).staff(responsible, "staff", project, null);

        String adjustment = mockMvc.perform(post("/api/v1/finance/ar-ap-adjustments").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "frl-adj-r8-" + s, "kind", "amount", "ar_ap_item_id", item,
                                "adjustment_amount_cent", -400, "reason", "核实商户实际只差 6 元", "responsible_staff_id", responsible,
                                "evidence_attachment_ids", List.of(backfillEvidence())))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        approve(JsonPath.read(adjustment, "$.data.review_id"), "r8-adj").andExpect(status().isOk());
        assertThat(jdbc.queryForList("SELECT amount_cent FROM ar_ap_items WHERE origin_item_id = ? ORDER BY id", Long.class, item))
                .containsExactly(600L, -1_000L);

        List<String> listed = JsonPath.read(mockMvc.perform(get("/api/v1/finance/reconciliation/differences").with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "$.data.items[?(@.id == '" + diff + "')].posted_ar_ap_item_id");
        assertThat(listed).containsExactly(item);
        String statement = jdbc.queryForObject("SELECT evidence_attachment_ids->>0 FROM finance_reconciliation_differences WHERE id = ?",
                String.class, diff);
        mockMvc.perform(get("/api/v1/evidence/attachments/" + statement + "/file").with(approver)).andExpect(status().isOk());
    }

    // ---------- helpers ----------

    /** 生成该酒吧营业日日结（随之生成对账批次），以人工录入登记差异（带原件）并识别，返回处于待处理的差异号。 */
    private String handled(String tag, String businessRef, long actualCent) throws Exception {
        return handled(tag, businessRef, actualCent, "merchant", bar);
    }

    /** 第十八批 AV 段 305-R1：同上，指定日结范围（经销到账是项目级，登记在综合体批次下）。 */
    private String handled(String tag, String businessRef, long actualCent, String scopeType, String scopeId) throws Exception {
        mockMvc.perform(post("/api/v1/finance/daily-closes").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "frl-gen-" + tag + "-" + s,
                                "business_date", day.toString(), "scope_type", scopeType, "scope_id", scopeId))))
                .andExpect(status().isOk());
        List<String> batches = JsonPath.read(mockMvc.perform(get("/api/v1/finance/reconciliation/batches").with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.items[?(@.scope_id == '" + scopeId + "')].id");
        assertThat(batches).hasSize(1);
        String diff = JsonPath.read(mockMvc.perform(post("/api/v1/finance/reconciliation/differences").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "frl-diff-" + tag + "-" + s, "batch_id", batches.get(0),
                                "difference_type", "amount", "business_ref", businessRef, "source", "manual",
                                "actual_amount_cent", actualCent, "evidence_attachment_ids", List.of(upload())))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
        mockMvc.perform(post("/api/v1/finance/reconciliation/differences/" + diff + "/identify").with(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "frl-identify-" + tag + "-" + s,
                                "cause_note", "银行到账与平台收款记录不符"))))
                .andExpect(status().isOk());
        return diff;
    }

    /** 对账差异页选中待处理的行点「提交复核」的真实请求；ledgerEntry 为空即不勾选（不带该键）。 */
    private ResultActions submit(String differenceId, String tag, Boolean ledgerEntry) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("idempotency_key", "frl-submit-" + tag + "-" + s,
                "close_reason", "以银行实际到账为准", "evidence_attachment_ids", List.of()));
        if (ledgerEntry != null) {
            body.put("ledger_entry", ledgerEntry);
        }
        return mockMvc.perform(post("/api/v1/finance/reconciliation/differences/" + differenceId + "/close").with(admin)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private static String reviewOf(ResultActions submitted) throws Exception {
        return JsonPath.read(submitted.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.review_id");
    }

    /** 主管复核页上级审批通过的真实请求（同一 tag 即同一幂等键）。 */
    private ResultActions approve(String reviewId, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + reviewId + "/approve").with(approver)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "frl-approve-" + tag + "-" + s, "decision", "approved",
                        "comment", "已比对原件与银行到账"))));
    }

    /** 该差异入账的应收应付台账行。 */
    private List<Map<String, Object>> posted(String differenceId) {
        return jdbc.queryForList("SELECT id, direction, counterparty_id, merchant_id, project_id, amount_cent, unpaid_amount_cent, origin_item_id"
                + " FROM ar_ap_items WHERE source_type = 'reconciliation_difference' AND source_id = ?", differenceId);
    }

    /** 登记表单上传渠道对账单原件（照片），返回附件编号。 */
    private String upload() throws Exception {
        return JsonPath.read(mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "statement.jpg", "image/jpeg", new byte[] {1, 2, 3}))
                        .param("business_type", "finance_reconciliation_difference")
                        .param("attachment_role", "reconciliation_statement").with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
    }

    /** 日结区块补录上传原始凭证（银行回单 pdf），返回附件编号。 */
    private String backfillEvidence() throws Exception {
        return JsonPath.read(mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                        .file(new MockMultipartFile("file", "bank-receipt.pdf", "application/pdf",
                                "%PDF-1.4 locked backfill receipt".getBytes(StandardCharsets.UTF_8)))
                        .param("business_type", "finance_locked_backfill").with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.data.id");
    }

    /** 前置态：本酒吧一条已收付清的往来台账（应收或应付）与一笔等额收付款记录，返回收付款记录号。 */
    private String payment(String tag, String direction, long amountCent) {
        String item = "frl_item_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id)
                VALUES(?,?,?,?,'manual',?,?,?,0,'CNY',current_date,'2026-10','settled',now(),now(),?,?)
                """, item, "FRL-" + item, direction, bar, item, amountCent, amountCent, bar, project);
        String payment = "frl_pay_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,bank_flow_ref,created_at)
                VALUES(?,?,?,'bank_transfer',now(),'FLOW-'||?,now())
                """, payment, item, amountCent, payment);
        return payment;
    }

    /** 前置态：本酒吧受理的一张已支付票务订单（305-R1 起写受理商户与售票项目，归本酒吧日结），返回票务订单号。 */
    private String ticketOrder(String tag, long amountCent) {
        String type = "frl_tt_" + tag + "_" + s;
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)"
                + " VALUES (?, '对账差异入账门票', 'fixed', 'active', now(), now())", type);
        String order = "frl_to_" + tag + "_" + s;
        jdbc.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, pay_status, issue_status, status,"
                + " created_at, updated_at, merchant_id, project_id) VALUES (?, ?, ?, 1, 'web', ?, 'paid', 'issued', 'created', now(), now(), ?, ?)",
                order, "FRL-" + order, type, amountCent, bar, project);
        return order;
    }

    /**
     * 前置态：本项目一家经销商的订单（应收台账照 DealerOrderService 审核建台账：往来方为经销商、项目为经销商项目、商户为空）与一笔已核验的全额到账，
     * 返回到账记录号。
     */
    private String dealerPayment(String dealer, long amountCent) {
        jdbc.update("INSERT INTO dealers(id, project_id, dealer_name, status) VALUES (?, ?, '对账差异入账经销商', 'approved')", dealer, project);
        String entity = "frl_se_" + s;
        jdbc.update("INSERT INTO business_entities(id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, '对账差异入账销售主体', 'project_company', 'active', now(), now())", entity);
        String warehouse = "frl_wh_" + s;
        jdbc.update("INSERT INTO warehouses(id, project_id, warehouse_type, name, status) VALUES (?, ?, 'main', '对账差异入账中心仓', 'active')",
                warehouse, project);
        String contract = "frl_dc_" + s;
        jdbc.update("INSERT INTO dealer_contracts(id, dealer_id, contract_no, sales_entity_id, central_warehouse_id, valid_from, valid_to)"
                + " VALUES (?, ?, ?, ?, ?, current_date, current_date + 365)", contract, dealer, "FRL-" + contract, entity, warehouse);
        String order = "frl_do_" + s;
        String item = "frl_item_do_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,status,created_at,updated_at,project_id)
                VALUES(?,?,'receivable',?,'dealer_order',?,?,?,0,'CNY',current_date,'settled',now(),now(),?)
                """, item, "FRL-" + item, dealer, order, amountCent, amountCent, project);
        jdbc.update("INSERT INTO dealer_orders(id, order_no, dealer_id, dealer_contract_id, sales_entity_id, central_warehouse_id, status,"
                + " total_amount_cent, paid_amount_cent, ar_ap_item_id) VALUES (?, ?, ?, ?, ?, ?, 'pending_shipment', ?, ?, ?)",
                order, "FRL-" + order, dealer, contract, entity, warehouse, amountCent, amountCent, item);
        String payment = "dpay_frl_" + s;
        jdbc.update("INSERT INTO dealer_order_payments(id, order_id, method, amount_cent, received_at, status, applied_cent)"
                + " VALUES (?, ?, 'bank_transfer', ?, now(), 'verified', ?)", payment, order, amountCent, amountCent);
        return payment;
    }
}
