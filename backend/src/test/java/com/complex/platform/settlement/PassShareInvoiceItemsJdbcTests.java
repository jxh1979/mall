package com.complex.platform.settlement;

// 文件版本：V0.01

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 AZ 段第 290 条 290-R1：组合通票游乐、演出、餐饮、零售四业态的域间份额（含追收与到期未核销归属）进开票清单。依据：BSM:1803「平台内部各主体之间的
 * 往来需要开具发票」；BSM:1750「开票金额≤已收−已退−已开」；BSM:1760「已封账历史账单不回改，跨月退款和调整在当期生成冲正」；BSM:755「一律按总额收付」。
 *
 * <p>夹具随测试事务回滚：一张已确认的月度结算单（门票综合服务费为 0），四行业态份额——游乐域间份额 30000、到期未核销 5000 归游乐公司；传媒 20000 归传媒公司；
 * 餐饮 6000、零售 8000 归城市项目公司；该综合体 table_dining 另配给中央餐饮公司（通票餐饮份额不得被错开到它名下）。all_pass_ticket 取平台默认娱乐平台公司。
 * 持开票清单权限、不挂商户的平台侧财务经真实接口生成、查看（蛇形键）。修复前四行都不出，第一条生成条数断言即红。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class PassShareInvoiceItemsJdbcTests {

    private static final LocalDate PERIOD_START = LocalDate.of(2026, 6, 1);

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private final String s = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    private final String project = "prj_psi_" + s;
    private final String bill = "sb_psi_" + s;
    private final String city = "be_psi_city_" + s;

    @Test
    void passDomainSharesEnterInvoiceListPerFormatWithPlatformAsBuyer() throws Exception {
        jdbc.update("INSERT INTO projects (id, venue_type, name, status, created_at, updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                project, "通票份额开票综合体" + s);
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at)"
                + " VALUES (?, ?, 'management_company', 'active', now(), now())", city, "通票份额城市项目公司" + s);
        jdbc.update("INSERT INTO revenue_stream_attribution (id, project_id, revenue_stream, management_entity_id, beneficiary_kind, status, updated_by,"
                + " updated_at) VALUES (?, ?, 'table_dining', 'be_central_catering', 'entity', 'active', 'tester', now())", "rsa_psi_" + s, project);
        jdbc.update("INSERT INTO settlement_bills (id, project_id, period_start, period_end, comp_service_fee_exclusive_cent, distribution_method, status)"
                + " VALUES (?, ?, ?, ?, 0, 'by_rank_tier', 'confirmed')", bill, project, PERIOD_START, PERIOD_START.withDayOfMonth(30));
        share("amusement", "be_amusement", 30_000, 5_000);
        share("media", "be_media", 20_000, 0);
        share("catering", city, 6_000, 0);
        share("retail", city, 8_000, 0);
        RequestPostProcessor finance = new StaffRequestFixture(jdbc).staff("usr_psi_fin_" + s, "staff", project, null,
                "settlement.invoice.manage", "settlement.invoice.view");

        generate(finance).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(4));

        mockMvc.perform(get("/api/v1/settlement/invoice-items").param("projectId", project).param("periodStart", PERIOD_START.toString())
                        .with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'game')].amount_cent", contains(35_000)))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'game')].subject_entity_id", contains("be_amusement")))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'game')].buyer_name", contains("娱乐平台公司")))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'show')].amount_cent", contains(20_000)))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'show')].subject_entity_id", contains("be_media")))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'catering')].amount_cent", contains(6_000)))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'catering')].subject_entity_id", contains(city)))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'retail')].amount_cent", contains(8_000)))
                .andExpect(jsonPath("$.data.items[?(@.income_type == 'retail')].invoice_item_name", contains("零售商品")));

        generate(finance).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(0));

        // 游乐份额本月另有 5000 追收（原月已过账后撤销）：应开降到 30000，已开 35000，列入已超开提示先红冲。
        jdbc.update("UPDATE settlement_bill_format_shares SET recovery_net_cent = -5000 WHERE bill_id = ? AND business_format = 'amusement'", bill);
        mockMvc.perform(get("/api/v1/settlement/invoice-items/summary").param("projectId", project)
                        .param("periodStart", PERIOD_START.toString()).with(finance))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.over_issued[?(@.income_type == 'game')].over_issued_cent", contains(5_000)));
    }

    private ResultActions generate(RequestPostProcessor who) throws Exception {
        return mockMvc.perform(post("/api/v1/settlement/invoice-items/generate").param("billId", bill).with(who));
    }

    private void share(String format, String entity, long interEntity, long unverified) {
        jdbc.update("INSERT INTO settlement_bill_format_shares (bill_id, business_format, beneficiary_kind, beneficiary_entity_id,"
                + " inter_entity_net_cent, unverified_net_cent) VALUES (?, ?, 'entity', ?, ?, ?)", bill, format, entity, interEntity, unverified);
    }
}
