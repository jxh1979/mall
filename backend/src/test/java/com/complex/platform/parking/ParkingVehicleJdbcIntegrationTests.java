package com.complex.platform.parking;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
 * HP-P01 R7／R8 真库、真接口（请求体按前端蛇形键）：BSM L1437「商户月租车位按合同约定、按月出账……员工车辆由所属主体登记车牌后，
 * 按后台配置免费或折扣」；L1428「月租车位在本平台后台配置并下发给停车系统」。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}",
        ParkingFixture.SECRET_PROPERTY
})
@AutoConfigureMockMvc
@Transactional
class ParkingVehicleJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private ParkingFixture f;
    private String merchant;
    private RequestPostProcessor merchantAdmin;
    private RequestPostProcessor platformStaff;

    @BeforeEach
    void seed() {
        f = new ParkingFixture(jdbc, "veh");
        f.policy();
        f.vendor(true);
        merchant = "pk_mch_" + f.suffix;
        String role = f.role("veh", "parking.vehicle.manage");
        merchantAdmin = f.account("madmin", role, merchant);
        platformStaff = f.account("ops", role, null);
        f.account("mstaff", null, merchant);
        f.account("other", null, "pk_mch_other_" + f.suffix);
    }

    @Test
    void merchantRegistersOwnStaffPlate() throws Exception {
        registerStaff(merchantAdmin, f.user("mstaff"), "粤A 88888").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plate_no").value("粤A88888"))
                .andExpect(jsonPath("$.data.owner_merchant_id").value(merchant))
                .andExpect(jsonPath("$.data.status").value("active"));
        mockMvc.perform(f.openGet("/parking/config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.staff_vehicles[0].plate_no").value("粤A88888"));
    }

    @Test
    void merchantCannotRegisterOtherMerchantStaff() throws Exception {
        registerStaff(merchantAdmin, f.user("other"), "粤A77777").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail_code").value("PARKING_STAFF_NOT_OWNED"));
        // 项目员工也不能替商户登记。
        registerStaff(platformStaff, f.user("mstaff"), "粤A77777").andExpect(status().isForbidden());
    }

    @Test
    void duplicateActivePlateConflict() throws Exception {
        registerStaff(merchantAdmin, f.user("mstaff"), "粤A66666").andExpect(status().isOk());
        registerStaff(merchantAdmin, f.user("mstaff"), "粤A66666").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PARKING_PLATE_ALREADY_REGISTERED"));
    }

    @Test
    void inactiveStaffPlateNotInConfigPull() throws Exception {
        registerStaff(merchantAdmin, f.user("mstaff"), "粤A55555").andExpect(status().isOk());
        jdbc.update("UPDATE access_users SET status='disabled' WHERE id=?", f.user("mstaff"));
        mockMvc.perform(f.openGet("/parking/config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.staff_vehicles").isEmpty());
    }

    @Test
    void configPullCarriesStaffDiscountBp() throws Exception {
        mockMvc.perform(f.openGet("/parking/config")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.staff_discount_bp").value(5000));
    }

    @Test
    void registerShapedLikeAdminClient() throws Exception {
        mockMvc.perform(post("/api/v1/parking/staff-vehicles").with(merchantAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\":\"" + f.user("mstaff") + "\",\"plate_no\":\"粤A44444\",\"project_id\":\"" + f.project
                                + "\",\"idempotency_key\":\"admin-web-1-" + f.suffix + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user_id").value(f.user("mstaff")));
        String id = jdbc.queryForObject("SELECT id FROM parking_staff_vehicles WHERE plate_no='粤A44444' AND project_id=?", String.class, f.project);
        mockMvc.perform(post("/api/v1/parking/staff-vehicles/" + id + "/revoke").with(merchantAdmin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"rv_" + f.suffix + "\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("revoked"));
    }

    @Test
    void monthlyPlatesOnlyFromEffectiveContractsInConfigPull() throws Exception {
        String effective = contract("eff", "effective", "{\"parkingMonthlyCent\":30000}");
        String draft = contract("dft", "drafting", "{\"parkingMonthlyCent\":30000}");
        String noParking = contract("nop", "effective", "{}");
        registerMonthly(effective, "粤B11111").andExpect(status().isOk()).andExpect(jsonPath("$.data.contract_id").value(effective));
        registerMonthly(draft, "粤B22222").andExpect(status().isOk());
        registerMonthly(noParking, "粤B33333").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail_code").value("PARKING_MONTHLY_NOT_CONTRACTED"));
        String body = mockMvc.perform(f.openGet("/parking/config")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> plates = JsonPath.read(body, "$.data.monthly_vehicles[*].plate_no");
        assertThat(plates).containsExactly("粤B11111");
        List<String> contracts = JsonPath.read(body, "$.data.monthly_vehicles[*].contract_id");
        assertThat(contracts).containsExactly(effective);
    }

    /**
     * BSM L1428「月租车位在本平台后台配置并下发给停车系统」：月租车牌只由平台后台登记、撤销和查看。商户账号即使持 parking.vehicle.manage
     * （用于登记本商户员工车辆），也不能给别家合同挂月租车牌、撤销或列出全项目月租车牌。
     */
    @Test
    void merchantCannotManageMonthlyVehicles() throws Exception {
        String other = "pk_ct_oth_" + f.suffix;
        jdbc.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, project_id, rent_terms, created_at, updated_at)
                VALUES (?,?,?,?,current_date-1,current_date+365,'effective',?,'{"parkingMonthlyCent":30000}'::jsonb,now(),now())""",
                other, "pk_mch_other_" + f.suffix, "pk_subj_" + f.suffix, "pk_subj_" + f.suffix, f.project);
        mockMvc.perform(post("/api/v1/parking/contracts/" + other + "/vehicles").with(merchantAdmin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plate_no\":\"粤B99999\",\"idempotency_key\":\"mvm_" + f.suffix + "\"}"))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM parking_monthly_vehicles WHERE contract_id=?", Integer.class, other)).isZero();
        registerMonthly(other, "粤B88888").andExpect(status().isOk());
        String id = jdbc.queryForObject("SELECT id FROM parking_monthly_vehicles WHERE contract_id=?", String.class, other);
        mockMvc.perform(post("/api/v1/parking/monthly-vehicles/" + id + "/revoke").with(merchantAdmin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"mvr_" + f.suffix + "\"}")).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT status FROM parking_monthly_vehicles WHERE id=?", String.class, id)).isEqualTo("active");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/parking/monthly-vehicles")
                .param("project_id", f.project).with(merchantAdmin)).andExpect(status().isForbidden());
    }

    private ResultActions registerStaff(RequestPostProcessor who, String userId, String plate) throws Exception {
        return mockMvc.perform(post("/api/v1/parking/staff-vehicles").with(who).contentType(MediaType.APPLICATION_JSON)
                .content("{\"user_id\":\"" + userId + "\",\"plate_no\":\"" + plate + "\",\"project_id\":\"" + f.project
                        + "\",\"idempotency_key\":\"sv_" + java.util.UUID.randomUUID() + "\"}"));
    }

    private ResultActions registerMonthly(String contractId, String plate) throws Exception {
        return mockMvc.perform(post("/api/v1/parking/contracts/" + contractId + "/vehicles").with(platformStaff)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"plate_no\":\"" + plate + "\",\"idempotency_key\":\"mv_" + java.util.UUID.randomUUID() + "\"}"));
    }

    private String contract(String tag, String lifecycle, String rentTerms) {
        String id = "pk_ct_" + tag + "_" + f.suffix;
        jdbc.update("""
                INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, expiry_date,
                    lifecycle_status, project_id, rent_terms, created_at, updated_at)
                VALUES (?,?,?,?,current_date-1,current_date+365,?,?,?::jsonb,now(),now())""",
                id, merchant, "pk_subj_" + f.suffix, "pk_subj_" + f.suffix, lifecycle, f.project, rentTerms);
        return id;
    }
}
