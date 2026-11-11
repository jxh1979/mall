package com.complex.platform.finance;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AF 段第 198 条 198-R5（财务、分红与结算）：契约删去 PropertyFeeBillRequest 的 operator_note、evidence_attachment_ids
 * （FinanceService.billPropertyFeeInternal 只读 project_id、period、merchant_ids）、物业费管理页（FinancePropertyFeePage）停发写死备注与假附件后，
 * 按停发后的网页形状——idempotency_key、project_id、period、merchant_ids——走真实 HTTP、真实授权链出账。入口未改，本类只锁现行为，不先红。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class PropertyFeeBillWebShape198R5Tests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String digits = String.valueOf(System.nanoTime());

    // 平台财务出账，每平米 800 分 × 12.50 平米：200，逐户出账 10000 分并挂一条待收应收。
    @Test
    void billPropertyFee_webShapeWithoutOperatorNoteAndEvidence_billed() throws Exception {
        String project = "prj_b52ws_" + digits;
        String merchant = "mch_b52ws_" + digits;
        String period = YearMonth.now().toString();
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','物业费形状综合体','active',now(),now())",
                project);
        jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,area_square_meter,status,created_at,updated_at)"
                + " VALUES(?,'物业费形状商户','retail',?,12.50,'active',now(),now())", merchant, project);
        jdbc.update("INSERT INTO property_fee_rates(id,project_id,effective_period,rate_per_square_meter_cent,status,created_at,updated_at)"
                + " VALUES(?,?,NULL,800,'active',now(),now())", "pfr_b52ws_" + digits, project);
        // 出账走 currentMerchantScope（不带商户归属的项目员工 403 MERCHANT_SCOPE_REQUIRED），由平台侧财务操作。
        RequestPostProcessor finance = new StaffRequestFixture(jdbc)
                .staff("usr_b52ws_fin_" + digits, "platform_admin", null, null, "finance.property_fee.manage");

        mockMvc.perform(post("/api/v1/finance/property-fee/bills").with(finance).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotency_key\":\"property-fee-bill-b52ws-" + digits + "\",\"project_id\":\"" + project
                                + "\",\"period\":\"" + period + "\",\"merchant_ids\":[\"" + merchant + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].merchant_id").value(merchant))
                .andExpect(jsonPath("$.data[0].amount_cent").value(10000));

        assertThat(jdbc.queryForMap("""
                SELECT b.amount_cent, a.direction, a.source_type, a.unpaid_amount_cent, a.merchant_id
                  FROM property_fee_bills b JOIN ar_ap_items a ON a.id = b.ar_ap_item_id
                 WHERE b.project_id = ? AND b.period = ? AND b.merchant_id = ?
                """, project, period, merchant))
                .containsEntry("amount_cent", 10000L)
                .containsEntry("direction", "receivable")
                .containsEntry("source_type", "property_fee")
                .containsEntry("unpaid_amount_cent", 10000L)
                .containsEntry("merchant_id", merchant);
    }
}
