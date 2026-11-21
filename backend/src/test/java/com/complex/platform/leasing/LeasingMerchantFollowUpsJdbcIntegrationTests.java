package com.complex.platform.leasing;

// 文件版本：V0.01

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.complex.platform.support.StaffRequestFixture;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
 * 第十八批 BW 段第 385 条 385-R2（真库、真实入口、真实授权链、蛇形键）。依据：PGM:502「签约转商户自动带入联系人与联系方式、意向业态/面积/位置、
 * 报价租金条款和跟进历史，四类主体默认指向同一公司、后续可拆为不同公司。」。商户档案按来源线索读出招商跟进历史：线索本身的跟进加该线索各意向的跟进，
 * 不复制；读者与商户档案同口径——持 merchant.profile.view、且商户经营项目在该码可触及范围内。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=${SETTLE_TEST_DB_URL:jdbc:postgresql://127.0.0.1:5432/complex_test}",
        "spring.datasource.username=complex",
        "spring.datasource.password=${COMPLEX_DATABASE_PASSWORD:complex_dev_pwd}"
})
@AutoConfigureMockMvc
@Transactional
class LeasingMerchantFollowUpsJdbcIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JdbcLeasingRepository leasingRepository;

    private final String s = UUID.randomUUID().toString().substring(0, 8);
    private final String projectA = "prj_385a_" + s;
    private final String projectB = "prj_385b_" + s;
    private final String subject = "be_385_" + s;
    private final String lead = "lead_385_" + s;
    private final String intentA = "intent_385a_" + s;
    private final String intentB = "intent_385b_" + s;
    private final String leadY = "lead_385y_" + s;
    private final String intentY = "intent_385y_" + s;
    private StaffRequestFixture staffs;
    private String merchantId;

    @BeforeEach
    void seedAndSign() throws Exception {
        for (String p : List.of(projectA, projectB)) {
            jdbc.update("INSERT INTO projects(id,venue_type,name,status,created_at,updated_at) VALUES (?, 'complex', ?, 'active', now(), now())",
                    p, "招商跟进历史回归综合体" + p);
        }
        jdbc.update("INSERT INTO business_entities (id, entity_name, entity_role, status, created_at, updated_at) "
                + "VALUES (?, ?, 'management_company', 'active', now(), now())", subject, "招商跟进历史回归合同主体" + s);
        jdbc.update("INSERT INTO business_entity_projects(business_entity_id,project_id) VALUES (?, ?)", subject, projectA);
        String space = "sp_385_" + s;
        jdbc.update("INSERT INTO spaces (id, project_id, merchant_id, parent_space_id, space_name, space_type, floor_no, area_square_meter, status, "
                + "created_at, updated_at) VALUES (?, ?, NULL, NULL, ?, 'shop', '1F', 66.0, 'active', now(), now())", space, projectA, "招商跟进历史回归铺位");
        // 对照组：同项目另一条线索 Y 不选商户签约、自动建另一家商户，Y 与其意向各带跟进——本商户的跟进历史不得带出。
        String spaceY = "sp_385y_" + s;
        jdbc.update("INSERT INTO spaces (id, project_id, merchant_id, parent_space_id, space_name, space_type, floor_no, area_square_meter, status, "
                + "created_at, updated_at) VALUES (?, ?, NULL, NULL, ?, 'shop', '1F', 66.0, 'active', now(), now())", spaceY, projectA, "招商跟进历史回归另一铺位");
        Instant now = Instant.now();
        String otherLead = "lead_385x_" + s;
        leasingRepository.saveLead(new LeasingLeadRecord(lead, "跟进回归联系人", "13800003333", "referral",
                "retail", space, null, null, "usr_sales", "negotiating", null, null, projectA, now, now));
        leasingRepository.saveLead(new LeasingLeadRecord(otherLead, "无关线索联系人", "13800004444", "referral",
                "retail", null, null, null, "usr_sales", "following_up", null, null, projectA, now, now));
        leasingRepository.saveLead(new LeasingLeadRecord(leadY, "另一商户联系人", "13800005555", "referral",
                "retail", spaceY, null, null, "usr_sales", "negotiating", null, null, projectA, now, now));
        leasingRepository.saveIntent(new LeasingIntentRecord(intentA, lead, "pending_signing", space,
                null, Map.of(), now, "usr_supervisor", projectA, now, now));
        leasingRepository.saveIntent(new LeasingIntentRecord(intentB, lead, "negotiation", null,
                null, Map.of(), now, "usr_supervisor", projectA, now, now));
        leasingRepository.saveIntent(new LeasingIntentRecord(intentY, leadY, "pending_signing", spaceY,
                null, Map.of(), now, "usr_supervisor", projectA, now, now));
        followUp("fu_385_lead_" + s, "lead", lead, "首次电话沟通", now.minusSeconds(300));
        followUp("fu_385_ia_" + s, "intent", intentA, "签约意向报价沟通", now.minusSeconds(200));
        followUp("fu_385_ib_" + s, "intent", intentB, "另一铺位意向沟通", now.minusSeconds(100));
        followUp("fu_385_x_" + s, "lead", otherLead, "无关线索跟进", now.minusSeconds(50));
        followUp("fu_385_y_lead_" + s, "lead", leadY, "另一商户线索跟进", now.minusSeconds(250));
        followUp("fu_385_iy_" + s, "intent", intentY, "另一商户意向跟进", now.minusSeconds(150));

        staffs = new StaffRequestFixture(jdbc);
        RequestPostProcessor sales = staffs.staff("usr_385_sign_" + s, "platform_admin", projectA, null, "leasing.convert");
        merchantId = JsonPath.read(sign(sales, intentA, "385-sign-"), "$.data.merchant_id");
        String otherMerchant = JsonPath.read(sign(sales, intentY, "385-sign-y-"), "$.data.merchant_id");
        assertThat(otherMerchant).isNotEqualTo(merchantId);
    }

    @Test
    void merchantProfileViewerSeesLeadAndIntentFollowUpsOfTheSourceLead() throws Exception {
        RequestPostProcessor viewer = staffs.staff("usr_385_view_" + s, "staff", projectA, null, "merchant.profile.view");
        String body = followUps(viewer).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> items = JsonPath.read(body, "$.data.items");
        assertThat(items).hasSize(3);
        // 按创建时间倒序；另一商户来源线索 Y 及其意向的跟进不带出。
        assertThat(items).extracting(item -> item.get("ref_id")).containsExactly(intentB, intentA, lead);
        assertThat(items).extracting(item -> item.get("content")).doesNotContain("无关线索跟进", "另一商户线索跟进", "另一商户意向跟进");
        assertThat(items.get(0)).containsKeys("ref_type", "ref_id", "content", "created_at");
    }

    @Test
    void leadManagerWithoutMerchantProfileViewIsDenied() throws Exception {
        RequestPostProcessor leadManager = staffs.staff("usr_385_lead_" + s, "staff", projectA, null, "leasing.lead.manage");
        followUps(leadManager).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PERMISSION_DENIED"));
    }

    @Test
    void merchantProfileViewerOfAnotherProjectIsOutOfScope() throws Exception {
        RequestPostProcessor otherProject = staffs.staff("usr_385_other_" + s, "staff", projectB, null, "merchant.profile.view");
        followUps(otherProject).andExpect(status().isForbidden()).andExpect(jsonPath("$.detail_code").value("PROJECT_SCOPE_MISMATCH"));
    }

    private String sign(RequestPostProcessor who, String intentId, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/leasing/intents/" + intentId + "/sign").with(who).contentType(MediaType.APPLICATION_JSON)
                .content("{\"idempotency_key\":\"" + key + s + "\",\"contract_subject_id\":\"" + subject
                        + "\",\"effective_date\":\"2026-12-01\",\"expiry_date\":\"2027-11-30\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private void followUp(String id, String refType, String refId, String content, Instant at) {
        leasingRepository.saveFollowUp(new LeasingFollowUpRecord(id, refType, refId, "phone", content, null, "usr_sales", projectA, at));
    }

    private ResultActions followUps(RequestPostProcessor who) throws Exception {
        return mockMvc.perform(get("/api/v1/leasing/merchants/" + merchantId + "/follow-ups").with(who));
    }
}
