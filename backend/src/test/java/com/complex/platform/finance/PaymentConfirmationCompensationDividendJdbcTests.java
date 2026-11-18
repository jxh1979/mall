package com.complex.platform.finance;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.common.crypto.PiiCipher;
import com.complex.platform.dividend.DividendRecords;
import com.complex.platform.dividend.DividendRepository;
import com.complex.platform.support.InternalPayDayFixtures;
import com.complex.platform.support.StaffRequestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * 第十八批 AM 段第 241 条 241-R6、241-R3（BSM:1803「审批通过后的发放进入应付款项台账，走付款确认。付款确认按全平台统一的审批模型执行——发起时指定这条
 * 主线的最终审批人……主线与各条平行线都通过才完成付款确认；具体流程按支付类型分别设定。」；BSM:1072「无法替代的，双方确认赔付方案，经门店主管和财务审批后
 * 支付……实物或赔款实际交付后才结案」；BSM:1787「发放走应付款项」；第十八批裁决 55「所有应付都先走付款确认」、65（存酒对客赔付 saved_bottle_compensation，
 * 分红按台账来源 dividend_payout）、66「每类必带财务、法务两线」、68「赔付按单发起」）。存酒对客赔付结案前、分红应付登记付款前须付款确认通过，分红另须
 * 主理人分红应付已付清才能标记发放。发起、审批（主管复核接口）、结案、登记付款、标记发放都按后台真实请求形状（蛇形键）走真实入口与授权链。不开测试事务：
 * 分红通知在业务提交后才发；结案、登记付款带准入闸，前一笔受闸调用失败会把测试事务标为只回滚（与线上每个请求各自一个事务不同）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        "spring.task.scheduling.enabled=false"
})
@AutoConfigureMockMvc
class PaymentConfirmationCompensationDividendJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private DividendRepository dividends;
    @Autowired private PiiCipher cipher;

    private final String s = String.valueOf(System.nanoTime());
    private final String project = "pcc_p_" + s;
    private final String bar = "pcc_bar_" + s;
    private final String merchant = "pcc_dm_" + s;
    private final String plan = "pcc_plan_" + s;
    private final String principal = "usr_pcc_principal_" + s;
    private final String storeId = "usr_pcc_store_" + s;
    private final String payerId = "usr_pcc_pf_" + s;
    private final String mainId = "usr_pcc_s1_" + s;
    private final String finLineId = "usr_pcc_fl_" + s;
    private final String legalLineId = "usr_pcc_ll_" + s;
    private RequestPostProcessor store;
    private RequestPostProcessor supervisor;
    private RequestPostProcessor storeFinance;
    private RequestPostProcessor payer;
    private RequestPostProcessor submitter;
    private RequestPostProcessor approver;
    private RequestPostProcessor main;
    private RequestPostProcessor finLine;
    private RequestPostProcessor legalLine;

    @BeforeEach
    void seed() {
        InternalPayDayFixtures.seed(jdbc);
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','付款确认赔付分红项目','active',now(),now())",
                project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,project_id)"
                + " VALUES(?,'付款确认存酒酒吧','bar','active',now(),now(),?)", bar, project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,contact_name,contact_phone,status,created_at,updated_at)"
                + " VALUES(?,'付款确认分红商户','bar','主理人',?,'active',now(),now())",
                merchant, cipher.encrypt("136" + String.format("%08d", Math.floorMod(s.hashCode(), 100_000_000))));
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        store = staffs.staff(storeId, "staff", project, bar, "bar.saved_bottle.manage");
        supervisor = staffs.staff("usr_pcc_sup_" + s, "staff", project, bar, "inventory.high_value.review");
        storeFinance = staffs.staff("usr_pcc_sf_" + s, "staff", project, bar, "finance.ar_ap.manage");
        payer = staffs.staff(payerId, "platform_admin", null, null, "finance.payment.register", "finance.ar_ap.view");
        submitter = staffs.staff("usr_pcc_ds_" + s, "platform_admin", null, null, "finance.dividend.manage", "finance.dividend.view");
        approver = staffs.staff("usr_pcc_da_" + s, "platform_admin", null, null, "finance.dividend.manage", "finance.dividend.view");
        main = staffs.staff(mainId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        finLine = staffs.staff(finLineId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        legalLine = staffs.staff(legalLineId, "staff", project, null, "governance.supervisor_review.view", "governance.supervisor_review.approve");
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", storeId, mainId);
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", payerId, mainId);
        // 243-R1：平台财务提交关停结算核实，主线沿直接上级到最终审批人。
        jdbc.update("INSERT INTO org_hierarchy(user_id,superior_id) VALUES(?,?)", "usr_pcc_ds_" + s, mainId);
    }

    /**
     * 241-R6：作废存酒登记赔付，经门店主管、财务两道审批后结案 409 PAYMENT_CONFIRMATION_REQUIRED（仍为已批准、未记经营损失）；按单发起付款确认 200
     * （审批类型 finance.payment_confirm.saved_bottle_compensation，金额取赔付金额）；各线通过后本店查询为已通过，结案 200、状态已结案并照记经营损失。
     */
    @Test
    void savedBottleCompensationClosesOnlyAfterPaymentConfirmation() throws Exception {
        String remedy = JsonPath.read(mockMvc.perform(post("/api/v1/inventory/saved-bottles/" + cancelledBottle() + "/remedy").with(store)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "pcc-rm-" + s, "remedy_type", "compensation",
                                "amount_cent", 12_000L, "reason", "原瓶破损无法同品替代"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.customer_delivery_status").value("pending_approval"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        remedyApprove(supervisor, remedy, "supervisor").andExpect(jsonPath("$.data.customer_delivery_status").value("pending_approval"));
        remedyApprove(storeFinance, remedy, "finance").andExpect(jsonPath("$.data.customer_delivery_status").value("approved"));

        settle(remedy, "early").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));
        assertThat(jdbc.queryForMap("SELECT customer_delivery_status, loss_ar_ap_item_id FROM saved_bottle_remedies WHERE id = ?", remedy))
                .containsEntry("customer_delivery_status", "approved").containsEntry("loss_ar_ap_item_id", null);

        String reviewId = reviewOf(mockMvc.perform(post("/api/v1/inventory/saved-bottles/remedies/" + remedy + "/payment-confirmations").with(store)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(confirmation("pcc-rc-" + s, "对客赔付原瓶损失"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payment_type").value("saved_bottle_compensation"))
                .andExpect(jsonPath("$.data.object_type").value("saved_bottle_remedy"))
                .andExpect(jsonPath("$.data.amount_cent").value(12_000)));
        assertThat(jdbc.queryForObject("SELECT action_type FROM supervisor_review_decisions WHERE review_id = ?", String.class, reviewId))
                .isEqualTo("finance.payment_confirm.saved_bottle_compensation");
        approveAll(reviewId, "rm");
        mockMvc.perform(get("/api/v1/finance/payment-confirmations").param("object_type", "saved_bottle_remedy").param("object_id", remedy)
                        .with(store))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_type").value("saved_bottle_compensation"))
                .andExpect(jsonPath("$.data.confirmations[0].status").value("approved"));

        settle(remedy, "done").andExpect(status().isOk()).andExpect(jsonPath("$.data.customer_delivery_status").value("closed"));
        Map<String, Object> closed = jdbc.queryForMap("SELECT loss_cost_cent, loss_ar_ap_item_id FROM saved_bottle_remedies WHERE id = ?", remedy);
        assertThat(((Number) closed.get("loss_cost_cent")).longValue()).isEqualTo(12_000L);
        assertThat(jdbc.queryForObject("SELECT amount_cent FROM ar_ap_items WHERE id = ?", Long.class, closed.get("loss_ar_ap_item_id")))
                .isEqualTo(12_000L);
    }

    /**
     * 241-R3：分红单审批通过得到主理人 dividend_payout 应付；登记付款 409 PAYMENT_CONFIRMATION_REQUIRED；标记发放 409 DIVIDEND_PAYOUT_NOT_PAID 且不发
     * 发放通知。发起付款确认（审批类型 finance.payment_confirm.dividend_payout，受益人为主理人），各线通过并登记付清后标记发放 200 并发通知。
     */
    @Test
    void dividendIsMarkedPaidOnlyAfterConfirmedPayoutIsSettled() throws Exception {
        String statement = statement("2026-07", 1_600L, 14_400L);
        submitAndApprove(statement);
        String payable = jdbc.queryForObject("SELECT ar_ap_item_id FROM dividend_statements WHERE id = ?", String.class, statement);
        assertThat(jdbc.queryForObject("SELECT source_type FROM ar_ap_items WHERE id = ?", String.class, payable)).isEqualTo("dividend_payout");

        pay(payable, "early", 14_400L).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));
        markPaid(approver, statement, "early").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DIVIDEND_PAYOUT_NOT_PAID"));
        assertThat(paidNotices(statement)).isZero();

        String reviewId = reviewOf(mockMvc.perform(post("/api/v1/finance/ar-ap-payment-confirmations").with(payer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "pcc-dc-" + s, "ar_ap_item_ids", List.of(payable),
                                "reason", "七月分红发放", "final_approver_id", mainId, "parallel_lines", lines()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.payment_type").value("dividend_payout")));
        assertThat(jdbc.queryForMap("SELECT action_type, beneficiary_id FROM supervisor_review_decisions WHERE review_id = ?", reviewId))
                .containsEntry("action_type", "finance.payment_confirm.dividend_payout").containsEntry("beneficiary_id", principal);
        approveAll(reviewId, "dv");
        pay(payable, "all", 14_400L).andExpect(status().isOk()).andExpect(jsonPath("$.data.item.status").value("settled"));

        markPaid(approver, statement, "done").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("paid"));
        assertThat(paidNotices(statement)).isEqualTo(1);
    }

    /** 241-R3：实发为 0 的分红单标记发放直接 200；释放后的留存应付 dividend_reserve_released 在付款确认通过之前登记付款 409。 */
    @Test
    void zeroPayoutMarksPaidDirectlyAndReleasedReserveStillNeedsConfirmation() throws Exception {
        String statement = statement("2026-08", 16_000L, 0L);
        submitAndApprove(statement);

        markPaid(approver, statement, "zero").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("paid"));
        mockMvc.perform(post("/api/v1/finance/dividends/statements/" + statement + "/release-reserve").with(approver)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "pcc-rel-" + s, "note", "到期返还"))))
                .andExpect(status().isOk());
        String released = jdbc.queryForObject("SELECT id FROM ar_ap_items WHERE source_type = 'dividend_reserve_released' AND source_id = ?",
                String.class, statement);
        pay(released, "released", 16_000L).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PAYMENT_CONFIRMATION_REQUIRED"));
    }

    /**
     * 第十八批 AQ 段第 243 条 243-R1（BSM:1787「酒吧关闭须完成关停结算，依法核清本次分红、已算未发分红与发放保护线待释放款项」「已确认应付及待释款项的
     * 法律性质和处理由财务、法务核实，不因关店自动核销或一概改为剩余财产分配；相关款项须按核实结果处理并留痕后才能结案。」；PGM:528「需要平行线审批的
     * （如法务、财务），另行指定该条线的最终审批人」；PGM:529「申请人与直接受益人不得审批与自己相关的申请」）：7 月单审批通过挂主理人应付 120 元与留存
     * 40 元，6 月单留草稿；平台财务登记关停结算（关店最后营业日 2026-08-20），列出本次分红（8 月正式单未出）、已算未发分红（6 月草稿、7 月主理人应付）与
     * 保护线待释放（7 月留存）；8 月单未出不能提交核实，出单后逐条填法律性质与处理方式提交，进统一审批（主线＋财务、法务两线，受益人为主理人）；驳回退回可
     * 再提交，全线通过后逐条登记处理才能结案；全程不动分红应付与计算单。按后台真实请求形状（蛇形键）走真实入口与授权链。修复前关停结算接口不存在：登记断言红。
     */
    @Test
    void barCloseoutIsVerifiedByFinanceAndLegalAndClosedOnlyAfterEveryItemIsHandled() throws Exception {
        String july = statement("2026-07", 4_000L, 12_000L);
        submitAndApprove(july);
        String june = statement("2026-06", 0L, 16_000L);
        String payout = jdbc.queryForObject("SELECT ar_ap_item_id FROM dividend_statements WHERE id = ?", String.class, july);
        String reserve = jdbc.queryForObject("SELECT id FROM ar_ap_items WHERE source_type = 'dividend_reserve_withheld' AND source_id = ?",
                String.class, july);
        List<Map<String, Object>> ledger = dividendLedger();

        String closeout = JsonPath.read(registerCloseout("first", merchant).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("open"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.id");
        registerCloseout("again", merchant).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DIVIDEND_CLOSEOUT_EXISTS"));
        String catering = "pcc_cat_" + s;
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at)"
                + " VALUES(?,'付款确认餐饮商户','catering','active',now(),now())", catering);
        registerCloseout("catering", catering).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("关停结算只适用于酒吧"));
        assertThat(closeoutItems(closeout))
                .extracting(item -> item.get("item_type"), item -> item.get("statement_id"), item -> item.get("ar_ap_item_id"),
                        item -> ((Number) item.get("amount_cent")).longValue())
                .containsExactly(tuple("current_period", null, null, 0L), tuple("unpaid_dividend", june, null, 16_000L),
                        tuple("unpaid_dividend", july, payout, 12_000L), tuple("reserve_pending", july, reserve, 4_000L));

        submitCloseout(closeout, "early", filled(closeoutItems(closeout))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DIVIDEND_CLOSEOUT_CURRENT_STATEMENT_MISSING"));
        String august = statement("2026-08", 0L, 16_000L);
        List<Map<String, Object>> items = closeoutItems(closeout);
        assertThat(items.get(0)).containsEntry("item_type", "current_period").containsEntry("statement_id", august)
                .containsEntry("period_label", "2026-08").containsEntry("amount_cent", 16_000);

        List<Map<String, Object>> missing = filled(items);
        missing.get(2).remove("legal_nature");
        submitCloseout(closeout, "missing", missing).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("法律性质")));
        String firstReview = reviewOf(submitCloseout(closeout, "first", filled(items)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review")));
        assertThat(jdbc.queryForMap("SELECT action_type, object_type, object_id, beneficiary_id FROM supervisor_review_decisions WHERE review_id = ?",
                firstReview)).containsEntry("action_type", "finance.dividend.closeout.verify").containsEntry("object_type", "dividend_closeout")
                .containsEntry("object_id", closeout).containsEntry("beneficiary_id", principal);
        assertThat(jdbc.queryForList("SELECT DISTINCT line_code FROM supervisor_review_nodes WHERE review_id = ?", String.class, firstReview))
                .contains("finance_review", "legal_review");
        closeCloseout(closeout, "early").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DIVIDEND_CLOSEOUT_NOT_VERIFIED"));

        decide(main, firstReview, "co-reject", "rejected").andExpect(status().isOk());
        assertThat(jdbc.queryForMap("SELECT status, review_id FROM dividend_closeouts WHERE id = ?", closeout))
                .containsEntry("status", "open").containsEntry("review_id", null);
        String secondReview = reviewOf(submitCloseout(closeout, "second", filled(closeoutItems(closeout))).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_review")));
        approveAll(secondReview, "co");
        items = closeoutItems(closeout);
        assertThat(items).hasSize(4).allSatisfy(item -> assertThat(item).containsEntry("legal_nature", "已确认的股东分红应付"));
        closeCloseout(closeout, "pending").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DIVIDEND_CLOSEOUT_ITEMS_PENDING"));
        for (Map<String, Object> item : items) {
            handleItem(closeout, (String) item.get("id"), "h-" + item.get("id")).andExpect(status().isOk());
        }
        handleItem(closeout, (String) items.get(0).get("id"), "again").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("DIVIDEND_CLOSEOUT_ITEM_HANDLED"));
        closeCloseout(closeout, "done").andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("closed"));

        assertThat(dividendLedger()).isEqualTo(ledger);
        assertThat(jdbc.queryForList("SELECT status FROM dividend_statements WHERE id IN (?, ?, ?) ORDER BY period_label", String.class,
                june, july, august)).containsExactly("draft", "approved", "draft");
    }

    // ---------- helpers ----------

    /** 本酒吧一瓶已作废的存酒（BSM:1072 ②③ 作废后才登记补存或赔付），返回存酒编号。 */
    private String cancelledBottle() {
        String item = "pcc_item_" + s;
        jdbc.update("INSERT INTO inventory_items(id,item_name,item_type,unit,min_quantity,status,merchant_id,created_at,updated_at)"
                + " VALUES(?,'付款确认存酒红酒','liquor','瓶',0,'active',?,now(),now())", item, bar);
        String bottle = "pcc_sb_" + s;
        jdbc.update("""
                INSERT INTO saved_bottles (
                    id, customer_id, bar_project_id, item_id, bottle_name, initial_quantity, remaining_quantity, unit, expire_at,
                    status, evidence_attachment_ids, created_at, updated_at, is_high_value, bottle_label_value,
                    customer_name_or_nickname, registration_photo_refs, merchant_id
                )
                VALUES (?, ?, ?, ?, '付款确认存酒红酒', 1, 1, '瓶', now() + interval '30 days', 'cancelled', '[]'::jsonb, now(), now(), false, '1',
                        '客人甲', '["registration-photo"]'::jsonb, ?)
                """, bottle, "pcc_customer_" + s, bar, item, bar);
        return bottle;
    }

    /** 门店赔付审批的真实请求（role＝supervisor 门店主管、finance 财务）。 */
    private ResultActions remedyApprove(RequestPostProcessor who, String remedy, String role) throws Exception {
        return mockMvc.perform(post("/api/v1/inventory/saved-bottles/remedies/" + remedy + "/approve").with(who)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "pcc-ra-" + role + "-" + s, "role", role, "reason", "同意赔付方案"))))
                .andExpect(status().isOk());
    }

    /** 赔付结案（赔款实际交付）的真实请求。 */
    private ResultActions settle(String remedy, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/inventory/saved-bottles/remedies/" + remedy + "/settle").with(store)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcc-st-" + tag + "-" + s, "evidence_ids", List.of()))));
    }

    /** 分红计算单草稿（净利 10 万：平台 8 万、主理人税前 2 万、税 4000、税后 1.6 万，按给定留存与实发），返回单号。 */
    private String statement(String month, long reserveCent, long payoutCent) {
        if (dividends.findPlanById(plan).isEmpty()) {
            Instant now = Instant.now();
            dividends.insertPlan(new DividendRecords.DividendPlanRecord(plan, merchant, principal, "主理人甲",
                    8000, 2000, "monthly", List.of("bar", "ticket"), true, List.of("labor"),
                    java.time.LocalDate.of(2000, 1, 1), "active", null, "usr_pcc_div_" + s, now, now, false, false, false, 0, 0L));
            jdbc.update("UPDATE dividend_plans SET payout_pay_slot = 1 WHERE id = ?", plan);
        }
        // 派生台账 id 只取「来源类型＋计算单号」前 40 字符：唯一后缀放在月份前，免得留存应付（来源类型 26 字符）跨用例、跨轮撞号沿用旧商户。
        String id = "pcs" + s.substring(s.length() - 9) + "_" + month;
        YearMonth ym = YearMonth.parse(month);
        Instant now = Instant.now();
        dividends.insertStatement(new DividendRecords.DividendStatementRecord(id, merchant, plan, "official", month, ym.atDay(1),
                ym.atEndOfMonth(), principal, 8000, 2000, List.of("bar", "ticket"), true, List.of("labor"),
                100_000L, 0L, 0L, 100_000L, 100_000L, 80_000L, 20_000L, 4_000L, 16_000L,
                0L, "draft", null, null, "usr_pcc_div_" + s, null, null, null, null, null, null, now, now,
                false, 0L, 0L, 0L, 0L, reserveCent, payoutCent, "individual", 0L, 1_000_000_000L));
        return id;
    }

    /** 分红单提交审阅、另一人审批通过的真实请求。 */
    private void submitAndApprove(String statement) throws Exception {
        mockMvc.perform(post("/api/v1/finance/dividends/statements/" + statement + "/submit").with(submitter)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "pcc-sub-" + statement))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/finance/dividends/statements/" + statement + "/approve").with(approver)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "pcc-apv-" + statement))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("approved"));
    }

    /** 分红页「标记发放」的真实请求。 */
    private ResultActions markPaid(RequestPostProcessor who, String statement, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/statements/" + statement + "/mark-paid").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcc-mp-" + tag + "-" + s))));
    }

    /** 台账页「登记核销」的真实请求。 */
    private ResultActions pay(String item, String tag, long amountCent) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/ar-ap-items/" + item + "/payments").with(payer).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcc-pay-" + tag + "-" + s, "amount_cent", amountCent,
                        "payment_method", "bank_transfer", "bank_flow_ref", "FLOW-pcc-" + tag + "-" + s))));
    }

    /** 该分红单的发放通知条数（BSM:1597「分红出单与发放，通知中写明金额、原因与异议截止日期」）。 */
    private int paidNotices(String statement) {
        return jdbc.queryForObject("SELECT count(*) FROM notification_send_log WHERE domain = 'merchant_amount' AND request_id = ?",
                Integer.class, "mamt:dividend_paid:" + statement);
    }

    private List<Map<String, String>> lines() {
        return List.of(Map.of("line_code", "finance_review", "final_approver_id", finLineId),
                Map.of("line_code", "legal_review", "final_approver_id", legalLineId));
    }

    private Map<String, Object> confirmation(String key, String reason) {
        return Map.of("idempotency_key", key, "reason", reason, "final_approver_id", mainId, "parallel_lines", lines());
    }

    private ResultActions approve(RequestPostProcessor who, String reviewId, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + reviewId + "/approve").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcc-apv-" + tag + "-" + s, "decision", "approved",
                        "comment", "已核对付款明细"))));
    }

    /** 主线、财务线、法务线各自的最终审批人逐一通过。 */
    private void approveAll(String reviewId, String tag) throws Exception {
        approve(main, reviewId, tag + "-main").andExpect(status().isOk());
        approve(finLine, reviewId, tag + "-fin").andExpect(status().isOk());
        approve(legalLine, reviewId, tag + "-legal").andExpect(status().isOk());
    }

    // ---------- 243-R1 关停结算 ----------

    /** 持股分红页「关停结算登记」的真实请求（关店最后营业日 2026-08-20）。 */
    private ResultActions registerCloseout(String tag, String merchantId) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/closeouts").with(submitter).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcc-co-" + tag + "-" + s, "merchant_id", merchantId,
                        "closed_business_date", "2026-08-20", "reason", "租约到期退场关店"))));
    }

    /** 关停结算列表里本商户那条登记的款项。 */
    private List<Map<String, Object>> closeoutItems(String closeout) throws Exception {
        String body = mockMvc.perform(get("/api/v1/finance/dividends/closeouts").param("merchant_id", merchant).with(submitter))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(closeout))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.data[0].closeout_items");
    }

    /** 逐条填法律性质与处理方式（照页面提交形状：类别、计算单号、台账号对上列出的那一条）。 */
    private static List<Map<String, Object>> filled(List<Map<String, Object>> items) {
        return items.stream().map(item -> {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("item_type", item.get("item_type"));
            row.put("statement_id", item.get("statement_id"));
            row.put("ar_ap_item_id", item.get("ar_ap_item_id"));
            row.put("legal_nature", "已确认的股东分红应付");
            row.put("handling", "按原分配依据付清");
            return row;
        }).toList();
    }

    /** 「提交财务法务核实」的真实请求（主线最终审批人＋财务、法务两线）。 */
    private ResultActions submitCloseout(String closeout, String tag, List<Map<String, Object>> items) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/closeouts/" + closeout + "/submit").with(submitter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcc-cos-" + tag + "-" + s, "final_approver_id", mainId,
                        "parallel_lines", lines(), "items", items))));
    }

    /** 「登记处理」的真实请求。 */
    private ResultActions handleItem(String closeout, String item, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/closeouts/" + closeout + "/items/" + item + "/handle").with(submitter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcc-coh-" + tag + "-" + s, "handled_note", "已按核实结果付清并留存凭证"))));
    }

    /** 「结案」的真实请求。 */
    private ResultActions closeCloseout(String closeout, String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/dividends/closeouts/" + closeout + "/close").with(submitter)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcc-coc-" + tag + "-" + s))));
    }

    /** 本商户台账行（来源、状态、未付额），核关停结算不动应付。 */
    private List<Map<String, Object>> dividendLedger() {
        return jdbc.queryForList("SELECT id, source_type, status, unpaid_amount_cent FROM ar_ap_items WHERE merchant_id = ? ORDER BY id", merchant);
    }

    private ResultActions decide(RequestPostProcessor who, String reviewId, String tag, String decision) throws Exception {
        return mockMvc.perform(post("/api/v1/platform/supervisor-review/" + reviewId + "/approve").with(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "pcc-apv-" + tag + "-" + s, "decision", decision,
                        "comment", "款项法律性质需补充核实"))));
    }

    private static String reviewOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.data.review_id");
    }
}
