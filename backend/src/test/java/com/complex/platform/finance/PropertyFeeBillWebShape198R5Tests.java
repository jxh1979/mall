package com.complex.platform.finance;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import java.time.YearMonth;
import java.util.List;
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
        // 平台侧财务出账（不挂商户的项目财务按项目范围放行，见第十八批 AZ 段 349-R1 用例）。
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

    /**
     * 第十八批 AZ 段 349-R1（PGM:664「商户费用账单出账……由该酒吧对应的财务 BP 负责」；PGM:420 城市项目公司「只负责资产、场地、物业、租赁、
     * 直接管理的第三方商户」；BSM:1758「生成的物业费账单在应收应付台账按「来源＝物业费」查询」）：不挂商户的项目财务给本项目出账 200，
     * 应收挂该项目、本人台账列得出；给别的项目出账 403 CROSS_PROJECT_FORBIDDEN；商户账号给他商户出账仍 403 CROSS_MERCHANT_FORBIDDEN。
     * 修复前项目财务出账 403 MERCHANT_SCOPE_REQUIRED，第一条 200 断言即红。
     */
    @Test
    void projectFinanceWithoutMerchantBillsOwnProjectAndReceivableCarriesProject() throws Exception {
        String project = "prj_b72pf_" + digits;
        String other = "prj_b72pfo_" + digits;
        String merchant = "mch_b72pf_" + digits;
        String neighbour = "mch_b72pfn_" + digits;
        String otherMerchant = "mch_b72pfo_" + digits;
        String period = YearMonth.now().toString();
        for (String p : List.of(project, other)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','物业费范围综合体','active',now(),now())",
                    p);
            jdbc.update("INSERT INTO property_fee_rates(id,project_id,effective_period,rate_per_square_meter_cent,status,created_at,updated_at)"
                    + " VALUES(?,?,NULL,800,'active',now(),now())", "pfr_" + p, p);
        }
        for (String[] m : List.of(new String[] {merchant, project}, new String[] {neighbour, project}, new String[] {otherMerchant, other})) {
            jdbc.update("INSERT INTO merchants(id,merchant_name,merchant_type,project_id,area_square_meter,status,created_at,updated_at)"
                    + " VALUES(?,'物业费范围商户','retail',?,12.50,'active',now(),now())", m[0], m[1]);
        }
        StaffRequestFixture staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor projectFinance = staffs.staff("usr_b72pf_fin_" + digits, "staff", project, null,
                "finance.property_fee.manage", "finance.ar_ap.view");

        bill(projectFinance, "own", project, period, merchant)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].amount_cent").value(10000));
        String item = jdbc.queryForObject("SELECT ar_ap_item_id FROM property_fee_bills WHERE project_id = ? AND period = ? AND merchant_id = ?",
                String.class, project, period, merchant);
        assertThat(jdbc.queryForObject("SELECT project_id FROM ar_ap_items WHERE id = ?", String.class, item)).isEqualTo(project);
        mockMvc.perform(get("/api/v1/finance/ar-ap-items").param("keyword", "property_fee").param("size", "200").with(projectFinance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].id", hasItem(item)));

        bill(projectFinance, "other", other, period, otherMerchant)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_PROJECT_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM property_fee_bills WHERE project_id = ?", Integer.class, other)).isZero();

        RequestPostProcessor merchantAccount = staffs.staff("usr_b72pf_mch_" + digits, "staff", project, merchant,
                "finance.property_fee.manage");
        bill(merchantAccount, "cross", project, period, neighbour)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("CROSS_MERCHANT_FORBIDDEN"));
    }

    /** 物业费管理页出账的网页形状（蛇形键）。 */
    private ResultActions bill(RequestPostProcessor who, String tag, String project, String period, String merchant) throws Exception {
        return mockMvc.perform(post("/api/v1/finance/property-fee/bills").with(who).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"property-fee-bill-b72pf-" + tag + "-" + digits + "\",\"project_id\":\"" + project
                        + "\",\"period\":\"" + period + "\",\"merchant_ids\":[\"" + merchant + "\"]}"));
    }
}
