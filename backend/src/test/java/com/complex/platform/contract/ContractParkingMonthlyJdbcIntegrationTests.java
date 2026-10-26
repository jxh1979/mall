package com.complex.platform.contract;

// 文件版本：V0.02

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import java.time.YearMonth;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * HP-P01 R7 真库、真接口：BSM L1437「商户月租车位按合同约定、按月出账，账单类型为「车位月租」」；ENUM 账单类型「车位月租」；
 * BSM L1446「连接招商租赁与合同履约，商户月租车位按合同出账」。前端经 adminClient.mapKeys 把 rent_terms 嵌套键蛇形化。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class ContractParkingMonthlyJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ContractRepository contracts;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);
    private final String project = "pkm_prj_" + suffix;
    private RequestPostProcessor staff;

    @BeforeEach
    void seed() {
        jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES(?,'complex','车位月租回归','active',now(),now())",
                project);
        String role = "pkm_role_" + suffix;
        jdbc.update("INSERT INTO roles(id,role_name,description,data_scope,status,created_at) VALUES(?,?,'车位月租','project','active',now())",
                role, role);
        for (String code : List.of("finance.ar_ap.manage", "contract.draft.manage")) {
            jdbc.update("INSERT INTO role_permissions(role_id,permission_code) VALUES(?,?)", role, code);
        }
        String user = "pkm_usr_" + suffix;
        jdbc.update("INSERT INTO access_users(id,username,account_type,organization_id,merchant_id,status,created_at) VALUES(?,?,'staff',?,NULL,'active',now())",
                user, user, project);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", user, role);
        AccessActor actor = new AccessActor(user, user, "staff", project, null, "active");
        staff = request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }

    @Test
    void parkingMonthlyBilledEveryPeriodAsParkingMonthly() throws Exception {
        String id = contract("eff", "effective", "{\"baseRentCent\":100000,\"parkingMonthlyCent\":30000}");
        periodBills(id, "2026-11").andExpect(status().isOk());
        periodBills(id, "2026-12").andExpect(status().isOk());
        List<Map<String, Object>> bills = jdbc.queryForList("""
                SELECT b.period, b.amount_cent, a.source_type, a.direction FROM contract_rent_bills b JOIN ar_ap_items a ON a.id=b.ar_ap_item_id
                 WHERE b.contract_id=? AND b.fee_type='parking_monthly' ORDER BY b.period""", id);
        assertThat(bills).extracting(r -> r.get("period")).containsExactly("2026-11", "2026-12");
        assertThat(bills).allSatisfy(r -> {
            assertThat(((Number) r.get("amount_cent")).longValue()).isEqualTo(30000L);
            assertThat(r.get("source_type")).isEqualTo("contract_parking_monthly");
            assertThat(r.get("direction")).isEqualTo("receivable");
        });
    }

    /**
     * 第 7 条（PGM:413「所有基础对象都要带“综合体项目归属”」；PGM:659「所有业务数据默认按项目隔离」）：合同租金、物业费与车位月租
     * 写入应收台账（ar_ap_items）时带合同所属项目，按项目统计的欠费与逾期不漏这些应收。
     */
    @Test
    void periodBillReceivablesCarryContractProject() throws Exception {
        String id = contract("prj", "effective", "{\"baseRentCent\":100000,\"propertyFeeCent\":20000,\"parkingMonthlyCent\":30000}");
        periodBills(id, "2026-11").andExpect(status().isOk());
        List<Map<String, Object>> items = jdbc.queryForList("""
                SELECT b.fee_type, a.project_id FROM contract_rent_bills b JOIN ar_ap_items a ON a.id=b.ar_ap_item_id
                 WHERE b.contract_id=?""", id);
        assertThat(items).isNotEmpty().allSatisfy(r -> assertThat(r.get("project_id")).isEqualTo(project));
        assertThat(items).extracting(r -> r.get("fee_type")).containsExactlyInAnyOrder("rent", "property_fee", "parking_monthly");
    }

    @Test
    void zeroParkingMonthlyProducesNoBill() throws Exception {
        String id = contract("zero", "effective", "{\"baseRentCent\":100000}");
        periodBills(id, "2026-11").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM contract_rent_bills WHERE contract_id=? AND fee_type='parking_monthly'",
                Integer.class, id)).isZero();
    }

    @Test
    void draftWithSnakeCaseParkingMonthlyCentLikeAdminClient() throws Exception {
        String id = contract("dft", "drafting", "{\"rentMode\":\"monthly_fixed\",\"rentBillingMode\":\"fixed_only\",\"baseRentCent\":100000}");
        mockMvc.perform(post("/api/v1/contracts/" + id + "/rent-terms").with(staff).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rent_terms\":{\"rent_mode\":\"monthly_fixed\",\"rent_billing_mode\":\"fixed_only\","
                                + "\"base_rent_cent\":100000,\"parking_monthly_cent\":30000},\"idempotency_key\":\"dft_" + suffix + "\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT (rent_terms->>'parkingMonthlyCent')::bigint FROM contracts WHERE id=?", Long.class, id))
                .isEqualTo(30000L);
        jdbc.update("UPDATE contracts SET lifecycle_status='effective' WHERE id=?", id);
        periodBills(id, "2026-11").andExpect(status().isOk());
        assertThat(jdbc.queryForList("SELECT amount_cent FROM contract_rent_bills WHERE contract_id=? AND fee_type='parking_monthly'",
                Long.class, id)).containsExactly(30000L);
    }

    @Test
    void moveOutMissingBillCheckIncludesParkingMonthly() {
        String id = contract("mv", "effective", "{\"baseRentCent\":100000,\"parkingMonthlyCent\":30000}");
        ContractRecord c = contracts.findContractById(id).orElseThrow();
        assertThat(contracts.listMissingTerminationDocuments(c, YearMonth.from(c.effectiveDate())))
                .contains(YearMonth.from(c.effectiveDate()) + ":parking_monthly");
    }

    private ResultActions periodBills(String id, String period) throws Exception {
        return mockMvc.perform(post("/api/v1/contracts/" + id + "/period-bills").with(staff).contentType(MediaType.APPLICATION_JSON)
                .content("{\"period\":\"" + period + "\",\"idempotency_key\":\"pb_" + UUID.randomUUID() + "\"}"));
    }

    private String contract(String tag, String lifecycle, String rentTerms) {
        String id = "pkm_ct_" + tag + "_" + suffix;
        jdbc.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, project_id, rent_terms, created_at, updated_at)
                VALUES (?,?,?,?,DATE '2026-11-01',DATE '2027-10-31',?,?,?::jsonb,now(),now())""",
                id, "pkm_mch_" + suffix, "pkm_subj_" + suffix, "pkm_subj_" + suffix, lifecycle, project, rentTerms);
        return id;
    }
}
