package com.complex.platform.finance;

// 文件版本：V0.01

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
import java.time.LocalDate;
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
 * 第十八批 Y 段第 180 条 180-R2（BSM:1746「第三方侧接入前可人工录入但须标记并强制复核」；ENUM:202「渠道接入前的人工录入必须同时上传渠道提供的
 * 对账单原件作为凭证……无原件的录入不得进入对账结论，只能挂为待补凭证」；ENUM:189「金额差异、数量差异、状态差异、缺失差异（单边缺失）」；
 * PGM:778「本商户订单回流、券核销、对账差异、结算依据、看板。」；PGM:528 红线④「看不到别的商户和别的项目的数据」）：
 * 对账差异按所属批次范围读与登记，对账单原件落库，四类都可登记。生成日结、上传原件、登记差异、读批次与差异、读指标明细都按后台真实请求形状（蛇形键）
 * 走真实入口与授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FinanceReconciliationDifferenceRegisterJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "frr_pa_" + s;
    private final String projectB = "frr_pb_" + s;
    private final String barA = "frr_ma_" + s;
    private final String barB = "frr_mb_" + s;
    private final LocalDate day = LocalDate.now(LocalZone.ID).minusDays(3);
    private RequestPostProcessor admin;
    private RequestPostProcessor financeA;
    private RequestPostProcessor merchantA;

    @BeforeEach
    void seed() {
        for (String[] pm : new String[][] {{projectA, barA}, {projectB, barB}}) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','对账差异登记项目','active',now(),now())",
                    pm[0]);
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,project_id)"
                    + " VALUES(?,'对账差异登记酒吧','bar','active',now(),now(),?)", pm[1], pm[0]);
        }
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        admin = staffs.staff("usr_frr_admin_" + s, "platform_admin", projectA, null,
                "finance.daily_close.lock", "finance.daily_close.manage", "finance.reconciliation.view",
                "finance.reconciliation.difference.create", "evidence.attachment.create", "analytics.dashboard.view");
        financeA = staffs.staff("usr_frr_fin_" + s, "staff", projectA, null,
                "finance.reconciliation.view", "finance.reconciliation.difference.create");
        merchantA = staffs.staff("usr_frr_mer_" + s, "staff", projectA, barA,
                "finance.reconciliation.view", "finance.reconciliation.difference.create");
    }

    /** ① 人工录入带对账单原件：登记 200、进待识别（181-R1），读差异时该行带回原件附件编号。 */
    @Test
    void manualDifferenceKeepsStatementOriginal() throws Exception {
        String batch = batchOf(barA);
        String attachment = upload();

        register(admin, "main", batch, "amount", paidPayment("p1"), 70, List.of(attachment))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_identification"))
                .andExpect(jsonPath("$.data.evidence_attachment_ids[0]").value(attachment));

        List<String> saved = JsonPath.read(list(admin, "differences"),
                "$.data.items[?(@.batch_id == '" + batch + "')].evidence_attachment_ids[0]");
        assertThat(saved).containsExactly(attachment);
    }

    /** ② 不带原件照收（ENUM:202「只能挂为待补凭证」），原件列为空。 */
    @Test
    void manualDifferenceWithoutStatementIsAccepted() throws Exception {
        register(admin, "noatt", batchOf(barA), "amount", paidPayment("p2"), 70, List.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidence_attachment_ids").isEmpty());
    }

    /** ③ 缺失差异（单边缺失）：第三方侧为 0、平台侧为 0 都可登记，两边都为 0 仍拒；金额差异第三方侧为 0 拒。 */
    @Test
    void missingDifferenceAcceptsOneSideZero() throws Exception {
        String batch = batchOf(barA);
        String noPlatform = "frr_nopay_" + s;

        register(admin, "m-third", batch, "missing", paidPayment("p3"), 0, List.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.amount_cent").value(100));
        register(admin, "m-platform", batch, "missing", noPlatform, 70, List.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.expected_amount_cent").value(0));
        register(admin, "m-both", batch, "missing", noPlatform, 0, List.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("RECONCILIATION_NO_SYSTEM_EXPECTED"));
        register(admin, "a-zero", batch, "amount", paidPayment("p4"), 0, List.of())
                .andExpect(status().isBadRequest());
    }

    /** ④ 两边金额相等：状态差异可登记，金额差异拒 RECONCILIATION_NO_DIFFERENCE。 */
    @Test
    void equalAmountsOnlyRejectedForAmountDifference() throws Exception {
        String batch = batchOf(barA);
        String payment = paidPayment("p5");

        register(admin, "st", batch, "status", payment, 100, List.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.amount_cent").value(0));
        register(admin, "am", batch, "amount", payment, 100, List.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("RECONCILIATION_NO_DIFFERENCE"));
    }

    /** ⑤ 范围：项目 A 财务读不到项目 B 的批次与差异、向项目 B 批次登记 403；商户账号只读到本商户的，登记 403。 */
    @Test
    void readsAndRegistrationFollowBatchScope() throws Exception {
        String batchA = batchOf(barA);
        String batchB = batchOf(barB);
        register(admin, "da", batchA, "amount", paidPayment("pa"), 70, List.of()).andExpect(status().isOk());
        register(admin, "db", batchB, "amount", paidPayment("pb"), 70, List.of()).andExpect(status().isOk());

        List<String> batchesA = JsonPath.read(list(financeA, "batches"), "$.data.items[*].id");
        assertThat(batchesA).contains(batchA).doesNotContain(batchB);
        List<String> differencesA = JsonPath.read(list(financeA, "differences"), "$.data.items[*].batch_id");
        assertThat(differencesA).contains(batchA).doesNotContain(batchB);
        register(financeA, "fb", batchB, "amount", paidPayment("pc"), 70, List.of())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));

        List<String> merchantScopes = JsonPath.read(list(merchantA, "batches"), "$.data.items[*].scope_id");
        assertThat(merchantScopes).isNotEmpty().allMatch(barA::equals);
        List<String> merchantDifferences = JsonPath.read(list(merchantA, "differences"), "$.data.items[*].batch_id");
        assertThat(merchantDifferences).isNotEmpty().allMatch(batchA::equals);
        register(merchantA, "ma", batchA, "amount", paidPayment("pd"), 70, List.of())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("RECONCILIATION_PLATFORM_ONLY"));
    }

    /** ⑥ 原件附件编号不存在拒 400。 */
    @Test
    void unknownStatementAttachmentIsRejected() throws Exception {
        register(admin, "bad", batchOf(barA), "amount", paidPayment("p6"), 70, List.of("att_frr_none_" + s))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("EVIDENCE_ATTACHMENT_NOT_FOUND"));
    }

    /** ⑦ 指标明细（对账差异笔数）行内差异类型、来源为中文。 */
    @Test
    void metricDetailShowsTypeAndSourceInChinese() throws Exception {
        String body = register(admin, "md", batchOf(barA), "amount", paidPayment("p7"), 70, List.of())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.data.id");

        String detail = mockMvc.perform(get("/api/v1/analytics/metric-detail").with(admin)
                        .param("metric", "finance.recon_diff_count").param("size", "200"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> rows = JsonPath.read(detail, "$.data.rows[?(@.id == '" + id + "')]");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).values()).contains("金额差异", "人工录入").doesNotContain("amount", "manual");
    }

    /**
     * 180-R3（ENUM:188「对账口径 按订单、按核销、按合同、按渠道、按日结」；BSM:1746「平台侧金额必须系统自动取数、不许手填」）：按订单口径——
     * 业务单号为已支付票务订单取订单实付，为酒吧账单取已付金额，收款记录照旧取已登记金额。
     */
    @Test
    void orderReferencesTakePlatformSideFromTicketOrderAndBarBill() throws Exception {
        String batch = batchOf(barA);

        register(admin, "to", batch, "amount", ticketOrder("paid", "paid", 8800), 8000, List.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expected_amount_cent").value(8800))
                .andExpect(jsonPath("$.data.amount_cent").value(800));
        register(admin, "bb", batch, "amount", barBill("paid", 6600), 6000, List.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expected_amount_cent").value(6600));
        register(admin, "pay", batch, "amount", paidPayment("p8"), 70, List.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expected_amount_cent").value(100));
    }

    /** 180-R3：未支付票务订单无平台侧系统取数，非缺失差异仍拒 400 RECONCILIATION_NO_SYSTEM_EXPECTED。 */
    @Test
    void unpaidTicketOrderHasNoSystemExpected() throws Exception {
        register(admin, "tu", batchOf(barA), "amount", ticketOrder("unpaid", "unpaid", 8800), 8000, List.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("RECONCILIATION_NO_SYSTEM_EXPECTED"));
    }

    /**
     * 18Y 复核修复（BSM:1746「平台侧金额必须系统自动取数、不许手填」）：业务单号按页面上显示的票务订单号（order_no）、酒吧账单号（bill_no）填，
     * 同样取到平台侧金额。
     */
    @Test
    void orderNoAndBillNoTakePlatformSide() throws Exception {
        String batch = batchOf(barA);

        register(admin, "ton", batch, "amount", "FRR-" + ticketOrder("no", "paid", 8800), 8000, List.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expected_amount_cent").value(8800));
        register(admin, "bbn", batch, "amount", "FRR-" + barBill("no", 6600), 6000, List.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expected_amount_cent").value(6600));
    }

    /** 18Y 复核修复（ENUM:202「上传渠道提供的对账单原件」）：渠道导出的 PDF 原件可直接上传，带上登记成功；带宏的 xlsm 仍拒 400。 */
    @Test
    void pdfStatementOriginalIsAccepted() throws Exception {
        String attachment = upload("statement.pdf", "application/pdf");

        register(admin, "pdf", batchOf(barA), "amount", paidPayment("p9"), 70, List.of(attachment))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.evidence_attachment_ids[0]").value(attachment));
        uploadFile("statement.xlsm", "application/vnd.ms-excel.sheet.macroEnabled.12").andExpect(status().isBadRequest());
    }

    // ---------- helpers ----------

    /** 后台日结区块生成该酒吧该营业日日结（随之生成对账批次，180-R1），返回该批次 ID。 */
    private String batchOf(String bar) throws Exception {
        mockMvc.perform(post("/api/v1/finance/daily-closes").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "frr-gen-" + bar,
                                "business_date", day.toString(), "scope_type", "bar", "scope_id", bar))))
                .andExpect(status().isOk());
        List<String> ids = JsonPath.read(list(admin, "batches"), "$.data.items[?(@.scope_id == '" + bar + "')].id");
        assertThat(ids).hasSize(1);
        return ids.get(0);
    }

    /** 对账差异页「登记差异」表单的真实请求（source 固定人工录入）。 */
    private ResultActions register(RequestPostProcessor who, String tag, String batchId, String type, String businessRef,
            long actualCent, List<String> attachments) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/reconciliation/differences").with(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "frr-diff-" + tag + "-" + s, "batch_id", batchId,
                        "difference_type", type, "business_ref", businessRef, "source", "manual",
                        "actual_amount_cent", actualCent, "evidence_attachment_ids", attachments))));
    }

    /** 对账差异页读批次或差异的真实请求。 */
    private String list(RequestPostProcessor who, String what) throws Exception {
        return mockMvc.perform(get("/api/v1/finance/reconciliation/" + what).with(who))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    /** 登记表单上传渠道对账单原件（照片），返回附件编号。 */
    private String upload() throws Exception {
        return upload("statement.jpg", "image/jpeg");
    }

    private String upload(String fileName, String contentType) throws Exception {
        return JsonPath.read(uploadFile(fileName, contentType).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "$.data.id");
    }

    private ResultActions uploadFile(String fileName, String contentType) throws Exception {
        return mockMvc.perform(multipart("/api/v1/evidence/attachments/upload")
                .file(new MockMultipartFile("file", fileName, contentType, new byte[] {1, 2, 3}))
                .param("business_type", "finance_reconciliation_difference")
                .param("attachment_role", "reconciliation_statement").with(admin));
    }

    /** 前置态：一笔已登记 100 分的收款记录（平台侧系统取数来源），返回收款记录号。 */
    private String paidPayment(String tag) {
        String item = "frr_item_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id)
                VALUES(?,?,'receivable',?,'manual',?,100,100,0,'CNY',current_date,'2026-10','settled',now(),now(),?,?)
                """, item, "FRR-" + item, barA, item, barA, projectA);
        String payment = "frr_pay_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,bank_flow_ref,created_at)
                VALUES(?,?,100,'bank_transfer',now(),'FLOW-'||?,now())
                """, payment, item, payment);
        return payment;
    }

    /** 前置态：一张票务订单（订单金额即实付），返回票务订单号。 */
    private String ticketOrder(String tag, String payStatus, long amountCent) {
        String type = "frr_tt_" + tag + "_" + s;
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)"
                + " VALUES (?, '对账差异门票', 'fixed', 'active', now(), now())", type);
        String order = "frr_to_" + tag + "_" + s;
        jdbc.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, pay_status, issue_status, status,"
                + " created_at, updated_at) VALUES (?, ?, ?, 1, 'web', ?, ?, ?, 'created', now(), now())",
                order, "FRR-" + order, type, amountCent, payStatus, "paid".equals(payStatus) ? "issued" : "not_issued");
        return order;
    }

    /** 前置态：本酒吧一张已收款结清的账单，返回酒吧账单号。 */
    private String barBill(String tag, long paidCent) {
        String table = "frr_table_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity,"
                + " allowed_pass_free_guest_count, min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '对账差异卡座', 'card_seat', 6, 0, 0, 'CNY', 'active', now(), now())",
                table, barA, barA, "FRR-" + tag + "-" + s);
        String order = "frr_bo_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,created_at,updated_at)"
                + " VALUES(?,?,?,?,'staff','created',?,'CNY',now(),now())", order, barA, barA, table, paidCent);
        String bill = "frr_bill_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,"
                + "deposit_deduct_amount_cent,payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,"
                + "created_at,updated_at) VALUES(?,?,?,?,?,?,?,0,0,?,?,'CNY','paid','settled',0,now(),now())",
                bill, "FRR-" + bill, order, barA, barA, table, paidCent, paidCent, paidCent);
        return bill;
    }
}
