package com.complex.platform.finance;

// 文件版本：V0.03

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
    private final String barA2 = "frr_ma2_" + s;
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
        // 第十八批 AV 段 305-R1：项目 A 的第二家酒吧。
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,project_id)"
                + " VALUES(?,'对账差异登记酒吧二','bar','active',now(),now(),?)", barA2, projectA);
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
        register(admin, "db", batchB, "amount", paidPayment("pb", barB, projectB), 70, List.of()).andExpect(status().isOk());

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

    /**
     * 第十八批 AL 段第 224 条 224-R1（ENUM:173「交付差异与款项分别跟踪，财务差异按本表「差异处理状态」条处理」；ENUM:202「通过既有三方对账及财务差异流程
     * 核实真实收款事实和凭证」；第十八批裁决 15「财务核验到账后，这条到账核验记录……就当收款凭证」）：业务单号为已核验的经销到账记录取到账金额；
     * 待核验的到账记录无平台侧系统取数，仍拒 400 RECONCILIATION_NO_SYSTEM_EXPECTED。第十八批 AV 段 305-R1：经销到账归经销商项目，登记在该综合体批次下。
     */
    @Test
    void dealerVerifiedPaymentTakesPlatformSide() throws Exception {
        String batch = batchOf("project", projectA);

        register(admin, "dv", batch, "amount", dealerPayment("v", "verified", 50000), 49800, List.of())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expected_amount_cent").value(50000))
                .andExpect(jsonPath("$.data.amount_cent").value(200));
        register(admin, "dp", batch, "amount", dealerPayment("p", "pending_verification", 30000), 29800, List.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("RECONCILIATION_NO_SYSTEM_EXPECTED"));
    }

    /**
     * 第十八批 AV 段第 308 条 308-R1（ENUM:191「差异处理状态待识别、待处理、待复核、已关闭」；BSM:1751「同一差异只入账一次」）：同一批次以不同幂等键
     * 登记的多笔差异各得一个差异号、各自落库；同一幂等键原样重发返回同一差异。
     */
    @Test
    void differencesInSameBatchGetDistinctIds() throws Exception {
        String batch = batchOf(barA);
        String first = paidPayment("s1");
        String second = paidPayment("s2");

        String firstId = idOf(register(admin, "same-1", batch, "amount", first, 70, List.of()).andExpect(status().isOk()));
        String secondId = idOf(register(admin, "same-2", batch, "amount", second, 60, List.of()).andExpect(status().isOk()));

        assertThat(secondId).isNotEqualTo(firstId);
        List<String> refs = JsonPath.read(list(admin, "differences"), "$.data.items[?(@.batch_id == '" + batch + "')].business_ref");
        assertThat(refs).containsExactlyInAnyOrder(first, second);
        assertThat(idOf(register(admin, "same-1", batch, "amount", first, 70, List.of()).andExpect(status().isOk()))).isEqualTo(firstId);
    }

    /**
     * 第十八批 AV 段第 305 条 305-R1（PGM:528「④看不到别的商户和别的项目的数据」；PGM:664「单店层面的财务工作——……差异认定……由该酒吧对应的财务 BP
     * 负责」；ENUM:191「差异未关闭期间，高价值差异所涉营业日的日结不得确认锁定」）：业务单号在平台侧查得到的须属所选批次的日结范围——酒吧批次认该酒吧的单
     * （含单店票），综合体批次认该综合体里酒吧以外的单；不属的拒 400 且不回显平台侧金额；平台侧查不到的照旧登记。
     */
    @Test
    void businessRefMustBelongToBatchScope() throws Exception {
        String barBatch = batchOf(barA);
        String complexBatch = batchOf("project", projectA);
        String complexTicket = ticketOrder("cx", "paid", 8800, null, null, projectA);
        String singleBarTicket = ticketOrder("sb", "paid", 8800, null, barA, projectA);

        register(financeA, "o-pay", barBatch, "amount", paidPayment("ob", barB, projectB), 70, List.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("RECONCILIATION_BUSINESS_REF_OUT_OF_BATCH"))
                .andExpect(jsonPath("$.data.expected_amount_cent").doesNotExist());
        register(admin, "o-bill", barBatch, "amount", barBill("o2", 6600, barA2), 6000, List.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("RECONCILIATION_BUSINESS_REF_OUT_OF_BATCH"));
        register(admin, "i-pay", barBatch, "amount", paidPayment("ib"), 70, List.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.expected_amount_cent").value(100));

        register(admin, "i-cx", complexBatch, "amount", complexTicket, 8000, List.of()).andExpect(status().isOk());
        register(admin, "i-dl", complexBatch, "amount", dealerPayment("ix", "verified", 50000), 49800, List.of())
                .andExpect(status().isOk());
        register(admin, "o-cxbill", complexBatch, "amount", barBill("o3", 6600), 6000, List.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("RECONCILIATION_BUSINESS_REF_OUT_OF_BATCH"));
        register(admin, "o-cx", barBatch, "amount", complexTicket, 8000, List.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("RECONCILIATION_BUSINESS_REF_OUT_OF_BATCH"));

        register(admin, "i-sb", barBatch, "amount", singleBarTicket, 8000, List.of()).andExpect(status().isOk());
        register(admin, "o-sb", complexBatch, "amount", singleBarTicket, 8000, List.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail_code").value("RECONCILIATION_BUSINESS_REF_OUT_OF_BATCH"));

        register(admin, "i-miss", barBatch, "missing", "frr_none_" + s, 70, List.of())
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.expected_amount_cent").value(0));
    }

    // ---------- helpers ----------

    /** 后台日结区块生成该酒吧该营业日日结（随之生成对账批次，180-R1），返回该批次 ID。 */
    private String batchOf(String bar) throws Exception {
        return batchOf("bar", bar);
    }

    /** 305-R1：同上，可生成综合体（project）范围的日结与批次。 */
    private String batchOf(String scopeType, String scopeId) throws Exception {
        mockMvc.perform(post("/api/v1/finance/daily-closes").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "frr-gen-" + scopeId,
                                "business_date", day.toString(), "scope_type", scopeType, "scope_id", scopeId))))
                .andExpect(status().isOk());
        List<String> ids = JsonPath.read(list(admin, "batches"), "$.data.items[?(@.scope_id == '" + scopeId + "')].id");
        assertThat(ids).hasSize(1);
        return ids.get(0);
    }

    private static String idOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.id");
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

    /** 前置态：酒吧 A 一笔已登记 100 分的收款记录（平台侧系统取数来源），返回收款记录号。 */
    private String paidPayment(String tag) {
        return paidPayment(tag, barA, projectA);
    }

    /** 305-R1：同上，收款记录所属台账挂指定商户与项目。 */
    private String paidPayment(String tag, String merchant, String project) {
        String item = "frr_item_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id)
                VALUES(?,?,'receivable',?,'manual',?,100,100,0,'CNY',current_date,'2026-10','settled',now(),now(),?,?)
                """, item, "FRR-" + item, merchant, item, merchant, project);
        String payment = "frr_pay_" + tag + "_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,bank_flow_ref,created_at)
                VALUES(?,?,100,'bank_transfer',now(),'FLOW-'||?,now())
                """, payment, item, payment);
        return payment;
    }

    /** 前置态：酒吧 A 受理的一张票务订单（订单金额即实付；305-R1 起写受理商户与售票项目，归酒吧 A 日结），返回票务订单号。 */
    private String ticketOrder(String tag, String payStatus, long amountCent) {
        return ticketOrder(tag, payStatus, amountCent, barA, null, projectA);
    }

    /** 305-R1：同上，指定受理商户、绑定酒吧（V288，单店票）与售票项目（V463）。 */
    private String ticketOrder(String tag, String payStatus, long amountCent, String merchant, String bar, String project) {
        String type = "frr_tt_" + tag + "_" + s;
        jdbc.update("INSERT INTO ticket_types (id, ticket_name, base_calendar_price_mode, status, created_at, updated_at)"
                + " VALUES (?, '对账差异门票', 'fixed', 'active', now(), now())", type);
        String order = "frr_to_" + tag + "_" + s;
        jdbc.update("INSERT INTO ticket_orders (id, order_no, ticket_type_id, quantity, sales_channel, amount_cent, pay_status, issue_status, status,"
                + " created_at, updated_at, merchant_id, bar_id, project_id) VALUES (?, ?, ?, 1, 'web', ?, ?, ?, 'created', now(), now(), ?, ?, ?)",
                order, "FRR-" + order, type, amountCent, payStatus, "paid".equals(payStatus) ? "issued" : "not_issued", merchant, bar, project);
        return order;
    }

    /** 前置态：酒吧 A 一张已收款结清的账单，返回酒吧账单号。 */
    private String barBill(String tag, long paidCent) {
        return barBill(tag, paidCent, barA);
    }

    /** 305-R1：同上，指定酒吧。 */
    private String barBill(String tag, long paidCent, String bar) {
        String table = "frr_table_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_tables (id, bar_id, merchant_id, table_code, table_name, table_type, capacity,"
                + " allowed_pass_free_guest_count, min_consumption_cent, currency, status, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, '对账差异卡座', 'card_seat', 6, 0, 0, 'CNY', 'active', now(), now())",
                table, bar, bar, "FRR-" + tag + "-" + s);
        String order = "frr_bo_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_orders(id,bar_id,merchant_id,table_id,order_source,status,total_amount_cent,currency,created_at,updated_at)"
                + " VALUES(?,?,?,?,'staff','created',?,'CNY',now(),now())", order, bar, bar, table, paidCent);
        String bill = "frr_bill_" + tag + "_" + s;
        jdbc.update("INSERT INTO bar_bills(id,bill_no,order_id,bar_id,merchant_id,table_id,subtotal_amount_cent,discount_amount_cent,"
                + "deposit_deduct_amount_cent,payable_amount_cent,paid_amount_cent,currency,payment_status,settlement_status,tip_amount_cent,"
                + "created_at,updated_at) VALUES(?,?,?,?,?,?,?,0,0,?,?,'CNY','paid','settled',0,now(),now())",
                bill, "FRR-" + bill, order, bar, bar, table, paidCent, paidCent, paidCent);
        return bill;
    }

    /** 前置态：项目 A 一家经销商的一张订单与一笔到账记录（已核验或待核验），返回到账记录号（dpay_ 前缀，同 DealerOrderService 登记到账）。 */
    private String dealerPayment(String tag, String paymentStatus, long amountCent) {
        String dealer = "frr_dl_" + tag + "_" + s;
        jdbc.update("INSERT INTO dealers(id, project_id, dealer_name, status) VALUES (?, ?, '对账差异经销商', 'approved')", dealer, projectA);
        String entity = "frr_se_" + tag + "_" + s;
        jdbc.update("INSERT INTO business_entities(id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, '对账差异经销销售主体', 'project_company', 'active', now(), now())", entity);
        String warehouse = "frr_wh_" + tag + "_" + s;
        jdbc.update("INSERT INTO warehouses(id, project_id, warehouse_type, name, status) VALUES (?, ?, 'main', '对账差异经销中心仓', 'active')",
                warehouse, projectA);
        String contract = "frr_dc_" + tag + "_" + s;
        jdbc.update("INSERT INTO dealer_contracts(id, dealer_id, contract_no, sales_entity_id, central_warehouse_id, valid_from, valid_to)"
                + " VALUES (?, ?, ?, ?, ?, current_date, current_date + 365)", contract, dealer, "FRR-" + contract, entity, warehouse);
        long applied = "verified".equals(paymentStatus) ? amountCent : 0L;
        String order = "frr_do_" + tag + "_" + s;
        jdbc.update("INSERT INTO dealer_orders(id, order_no, dealer_id, dealer_contract_id, sales_entity_id, central_warehouse_id, status,"
                + " total_amount_cent, paid_amount_cent) VALUES (?, ?, ?, ?, ?, ?, 'payment_verifying', ?, ?)",
                order, "FRR-" + order, dealer, contract, entity, warehouse, amountCent, applied);
        String payment = "dpay_frr_" + tag + "_" + s;
        jdbc.update("INSERT INTO dealer_order_payments(id, order_id, method, amount_cent, received_at, status, applied_cent)"
                + " VALUES (?, ?, 'bank_transfer', ?, now(), ?, ?)", payment, order, amountCent, paymentStatus, applied);
        return payment;
    }
}
