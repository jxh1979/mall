package com.complex.platform.contract;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.access.AccessActor;
import com.complex.platform.analytics.AnalyticsRecords;
import com.complex.platform.analytics.AnalyticsRepository;
import com.complex.platform.common.LocalZone;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/**
 * 第十八批 CE 段 403-R1 租赁状态看板「已签待交」与按面积出租率的数据源（ENUM:259「看板对已签合同但尚未交付的铺位另加「已签待交」标注
 * 并关联合同与交付依据，不新增第七种铺位租赁状态，不混作空置或已交付」；BSM:1829「铺位出租率＝已租铺位面积 ÷ 本项目拥有的铺位总面积」
 * 「看板单列「已签待交」标注」；PGM:540「已签约未交付的铺位在看板单独标注「已签待交」，不另增铺位租赁状态」；
 * PGM:502「合同审批全部通过、生效后转为已签约」；BSM:1347「交付日（即起租日）」）。
 *
 * <p>走真实 Flyway 迁移库与真实 HTTP（蛇形键、平台管理员网页请求），{@code @Transactional} 每用例回滚。
 * 夹具：sp_a 商铺 100㎡ 生效在租；sp_b 摊位 40㎡ 绑待生效合同；sp_c 商铺 60㎡ 绑已生效但未到生效日的合同；
 * sp_d 商铺 500㎡ 人工不可租；sp_e 商铺 20㎡ 有招商意向并绑待生效合同；sp_f 商铺 70㎡ 绑生效日为当天的已生效合同；
 * sp_g 商铺 30㎡ 绑审批中（pending_confirmation）合同。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class SpaceRentalBoardSignedPendingJdbcIntegrationTests {

    @Autowired private AnalyticsRepository analyticsRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;

    private String suffix;
    private String projectId;
    private LocalDate today;
    private AnalyticsRecords.OccupancyAreas baseline;
    private String spA;
    private String spB;
    private String spC;
    private String spD;
    private String spE;
    private String spF;
    private String spG;
    private String ctB;
    private String ctC;
    private String ctE;

    @BeforeEach
    void seed() {
        suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        projectId = "prj_ce403_" + suffix;
        today = LocalDate.now(LocalZone.ID);
        baseline = analyticsRepository.occupancyAreas(today);
        spA = space("sp_a", "shop", "100");
        contract("ct_a", spA, "effective", today.minusDays(30), today.plusDays(200));
        spB = space("sp_b", "booth", "40");
        ctB = contract("ct_b", spB, "pending_effective", today.plusDays(10), today.plusDays(375));
        spC = space("sp_c", "shop", "60");
        ctC = contract("ct_c", spC, "effective", today.plusDays(15), today.plusDays(380));
        spD = space("sp_d", "shop", "500");
        jdbcTemplate.update("UPDATE spaces SET status = 'unavailable' WHERE id = ?", spD);
        spE = space("sp_e", "shop", "20");
        intent(spE);
        ctE = contract("ct_e", spE, "pending_effective", today.plusDays(20), today.plusDays(385));
        spF = space("sp_f", "shop", "70");
        contract("ct_f", spF, "effective", today, today.plusDays(300));
        spG = space("sp_g", "shop", "30");
        contract("ct_g", spG, "pending_confirmation", today.plusDays(30), today.plusDays(395));
    }

    /** 看板接口每行带项目、空间类型、面积；已签待交只是标注：待生效、已生效未到生效日都标，不改六态（未到生效日不算已租）；生效日当天已起租只算已租不标，审批中未签约不标（PGM:502）。 */
    @Test
    void boardRowsCarryProjectTypeAreaAndSignedPendingMark() throws Exception {
        mockMvc.perform(get("/api/v1/contracts/space-rental-status").param("project_id", projectId).with(platformAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath(row(spA, "rental_status")).value("let"))
                .andExpect(jsonPath(row(spA, "area_sqm")).value(100.0))
                .andExpect(jsonPath(row(spA, "space_type")).value("shop"))
                .andExpect(jsonPath(row(spA, "project_id")).value(projectId))
                .andExpect(jsonPath(row(spA, "signed_pending_contract_id")).value(contains(nullValue())))
                .andExpect(jsonPath(row(spB, "rental_status")).value("vacant"))
                .andExpect(jsonPath(row(spB, "space_type")).value("booth"))
                .andExpect(jsonPath(row(spB, "signed_pending_contract_id")).value(ctB))
                .andExpect(jsonPath(row(spB, "signed_pending_effective_date")).value(today.plusDays(10).toString()))
                .andExpect(jsonPath(row(spC, "rental_status")).value("vacant"))
                .andExpect(jsonPath(row(spC, "signed_pending_contract_id")).value(ctC))
                .andExpect(jsonPath(row(spC, "signed_pending_effective_date")).value(today.plusDays(15).toString()))
                .andExpect(jsonPath(row(spD, "rental_status")).value("unavailable"))
                .andExpect(jsonPath(row(spD, "area_sqm")).value(500.0))
                .andExpect(jsonPath(row(spE, "rental_status")).value("negotiating"))
                .andExpect(jsonPath(row(spE, "signed_pending_contract_id")).value(ctE))
                .andExpect(jsonPath(row(spF, "rental_status")).value("let"))
                .andExpect(jsonPath(row(spF, "signed_pending_contract_id")).value(contains(nullValue())))
                .andExpect(jsonPath(row(spG, "rental_status")).value("vacant"))
                .andExpect(jsonPath(row(spG, "signed_pending_contract_id")).value(contains(nullValue())));
    }

    /** 经营分析已签待交面积与看板同口径：待生效 40＋20、已生效未到生效日 60 共 120；已租计在租的 100 与生效日当天起租的 70 共 170；审批中的 30 两边都不计。 */
    @Test
    void occupancySignedPendingAreaSharesBoardDefinition() {
        AnalyticsRecords.OccupancyAreas after = analyticsRepository.occupancyAreas(today);
        assertThat(after.signedPendingArea().subtract(baseline.signedPendingArea())).isEqualByComparingTo("120");
        assertThat(after.leasedArea().subtract(baseline.leasedArea())).isEqualByComparingTo("170");
    }

    // ---------- helpers ----------

    private static String row(String spaceId, String key) {
        return "$.data[?(@.space_id == '" + spaceId + "')]." + key;
    }

    private String space(String key, String type, String area) {
        String id = key + "_ce403_" + suffix;
        jdbcTemplate.update("INSERT INTO spaces (id, space_name, space_type, floor_no, area_square_meter, polygon_points, status, "
                + "project_id, created_at, updated_at) VALUES (?, ?, ?, '1F', ?, '[]'::jsonb, 'active', ?, now(), now())",
                id, "已签待交测试铺位" + key, type, new BigDecimal(area), projectId);
        return id;
    }

    private String contract(String key, String spaceId, String lifecycle, LocalDate effective, LocalDate expiry) {
        String id = key + "_ce403_" + suffix;
        jdbcTemplate.update("INSERT INTO contracts (id, merchant_id, contract_subject_id, settlement_subject_id, effective_date, "
                + "expiry_date, lifecycle_status, project_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, now(), now())",
                id, "mch_ce403_" + suffix, "subj_ce403_" + suffix, "subj_ce403_" + suffix, effective, expiry, lifecycle, projectId);
        jdbcTemplate.update("INSERT INTO contract_spaces (contract_id, space_id) VALUES (?, ?)", id, spaceId);
        return id;
    }

    private void intent(String spaceId) {
        String leadId = "lead_ce403_" + spaceId;
        jdbcTemplate.update("INSERT INTO leasing_leads (id, contact_name, source, status, intended_space_id, project_id, created_at, updated_at) "
                + "VALUES (?, '已签待交测试线索', 'outbound', 'negotiating', ?, ?, now(), now())", leadId, spaceId, projectId);
        jdbcTemplate.update("INSERT INTO leasing_intents (id, lead_id, stage, intended_space_id, project_id, created_at, updated_at) "
                + "VALUES (?, ?, 'negotiation', ?, ?, now(), now())", "intent_ce403_" + spaceId, leadId, spaceId, projectId);
    }

    /** 平台管理员（持 contract.draft.manage），后台网页真实请求形状（X-Terminal-Type: web）。 */
    private RequestPostProcessor platformAdmin() {
        String userId = "usr_ce403_" + suffix;
        String roleId = "role_ce403_" + suffix;
        jdbcTemplate.update("INSERT INTO access_users (id, username, account_type, organization_id, merchant_id, status, created_at)"
                + " VALUES (?, ?, 'platform_admin', 'org_1', NULL, 'active', now())", userId, "u_ce403_" + suffix);
        jdbcTemplate.update("INSERT INTO roles (id, role_name, created_at) VALUES (?, ?, now())", roleId,
                "租赁状态看板测试角色" + System.nanoTime());
        jdbcTemplate.update("INSERT INTO role_permissions (role_id, permission_code) VALUES (?, 'contract.draft.manage')", roleId);
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)", userId, roleId);
        AccessActor actor = new AccessActor(userId, "u_ce403_" + suffix, "platform_admin", "org_1", null, "active");
        return request -> {
            request.addHeader("X-Terminal-Type", "web");
            return authentication(new UsernamePasswordAuthenticationToken(actor, null,
                    List.of(new SimpleGrantedAuthority("ROLE_P0_STAFF")))).postProcessRequest(request);
        };
    }
}
