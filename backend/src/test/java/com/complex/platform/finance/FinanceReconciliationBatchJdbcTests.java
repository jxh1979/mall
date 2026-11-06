package com.complex.platform.finance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 Y 段第 180 条 180-R1（BSM:1729「三方对账按日进行」；BSM:1682「按营业日生成日结批次。」；ENUM:188「按日结」）：
 * 生成日结时同时生成该营业日该范围的财务对账批次，差异登记可挂到该批次下。生成日结、查批次、登记差异都按后台真实请求形状（蛇形键）走真实入口与授权链。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class FinanceReconciliationBatchJdbcTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;

    private final String s = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "frb_p_" + s;
    private final String merchant = "frb_m_" + s;
    private final LocalDate day = LocalDate.now(LocalZone.ID).minusDays(3);
    private RequestPostProcessor admin;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','对账批次项目','active',now(),now())",
                project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,status,created_at,updated_at,project_id)"
                + " VALUES(?,'对账批次酒吧','bar','active',now(),now(),?)", merchant, project);
        admin = new StaffRequestFixture(jdbc).staff("usr_frb_admin_" + s, "platform_admin", project, null,
                "finance.daily_close.lock", "finance.daily_close.manage", "finance.reconciliation.view",
                "finance.reconciliation.difference.create");
    }

    /** ① 生成日结后，对账批次列表有一行：营业日、范围取该日结的。 */
    @Test
    void generatingDailyCloseCreatesReconciliationBatchForSameDayAndScope() throws Exception {
        generate("main").andExpect(status().isOk());

        List<Map<String, Object>> rows = batchesOfMerchant();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("business_date", day.toString()).containsEntry("scope_type", "bar")
                .containsEntry("scope_id", merchant);
    }

    /** ② 同日同范围再生成 409 DAILY_CLOSE_EXISTS，批次仍只有一行。 */
    @Test
    void duplicateDailyCloseDoesNotCreateSecondBatch() throws Exception {
        generate("main").andExpect(status().isOk());
        generate("again").andExpect(status().isConflict()).andExpect(jsonPath("$.detail_code").value("DAILY_CLOSE_EXISTS"));

        assertThat(batchesOfMerchant()).hasSize(1);
    }

    /** ③ 拿随日结生成的批次登记差异 200（不再 404 RECONCILIATION_BATCH_NOT_FOUND）。 */
    @Test
    void differenceCanBeRegisteredOnGeneratedBatch() throws Exception {
        generate("main").andExpect(status().isOk());
        List<Map<String, Object>> rows = batchesOfMerchant();
        assertThat(rows).hasSize(1);
        String batchId = (String) rows.get(0).get("id");

        mockMvc.perform(post("/api/v1/finance/reconciliation/differences").with(admin).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("idempotency_key", "frb-diff-" + s, "batch_id", batchId,
                                "difference_type", "amount", "business_ref", paidPayment(), "source", "manual",
                                "actual_amount_cent", 70))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batch_id").value(batchId))
                .andExpect(jsonPath("$.data.expected_amount_cent").value(100));
    }

    // ---------- helpers ----------

    /** 后台日结区块生成本酒吧该营业日日结的真实请求形状。 */
    private ResultActions generate(String tag) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/daily-closes").with(admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("idempotency_key", "frb-gen-" + tag + "-" + s,
                        "business_date", day.toString(), "scope_type", "bar", "scope_id", merchant))));
    }

    /** 对账差异页读批次的真实请求，取本酒吧范围的行。 */
    private List<Map<String, Object>> batchesOfMerchant() throws Exception {
        String body = mockMvc.perform(get("/api/v1/finance/reconciliation/batches").with(admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.items[?(@.scope_id == '" + merchant + "')]");
    }

    /** 前置态：一笔已登记 100 分的收款记录（平台侧系统取数来源），返回收款记录号。 */
    private String paidPayment() {
        String item = "frb_item_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_items(id,item_no,direction,counterparty_id,source_type,source_id,amount_cent,paid_amount_cent,
                    unpaid_amount_cent,currency,due_date,accounting_period,status,created_at,updated_at,merchant_id,project_id)
                VALUES(?,?,'receivable',?,'manual',?,100,100,0,'CNY',current_date,'2026-10','settled',now(),now(),?,?)
                """, item, "FRB-" + item, merchant, item, merchant, project);
        String payment = "frb_pay_" + s;
        jdbc.update("""
                INSERT INTO ar_ap_payments(id,ar_ap_item_id,amount_cent,payment_method,payment_time,bank_flow_ref,created_at)
                VALUES(?,?,100,'bank_transfer',now(),'FLOW-'||?,now())
                """, payment, item, payment);
        return payment;
    }
}
